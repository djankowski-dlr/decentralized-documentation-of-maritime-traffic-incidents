package de.dlr.p2p.peer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import de.dlr.p2p.config.P2PConfig;
import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.connection.message.BroadcastMessage;
import de.dlr.p2p.connection.message.JoinMessage;
import de.dlr.p2p.connection.message.Message;
import de.dlr.p2p.connection.ping.CancelPong;
import de.dlr.p2p.connection.ping.Ping;
import de.dlr.p2p.connection.ping.Pong;
import de.dlr.p2p.service.ConnectionService;
import de.dlr.p2p.service.PingService;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Log4j2
@Component
public final class Peer {
	private final P2PConfig config;
	private final ConnectionService connectionService;
	private final PingService pingService;
	private Channel bindChannel;
	private boolean running = true;
	@Getter
	private final Sinks.Many<String> sink;
	@Getter
	private final Flux<String> flux;
	private final EventLoopGroup peerEventLoopGroup;

	@Autowired
	public Peer(P2PConfig config, ConnectionService connectionService, PingService pingService,
				EventLoopGroup peerEventLoopGroup) {
		this.config = config;
		this.connectionService = connectionService;
		this.pingService = pingService;
		this.sink = Sinks.many().multicast().directBestEffort();
		this.flux = this.sink.asFlux();
		this.peerEventLoopGroup = peerEventLoopGroup;
	}

	public synchronized void handleConnectionOpened(Connection connection, int port) {
		connection.setPort(port);
		if (!this.running) {
			log.warn("New connection of {} ignored since not running", connection.getPeerName());
			return;
		}

		if (connection.getPeerName().equals(this.config.getPeerName())) {
			log.error("Can not connect to itself. Closing new connection.");
			connection.close();
			return;
		}
		if (this.connectionService.isConnectedTo(connection.getPeerName())) {
			return;
		}

		this.connectionService.addConnection(connection);
		for (final Connection c : this.connectionService.getConnectionsValues()) {
			if (c.equals(connection)) {
				continue;
			}
			c.send(new JoinMessage(connection.getPeerName(), connection.getHost(), connection.getPort()));
		}

	}

	public synchronized void handleConnectionClosed(Connection connection) {
		if (connection == null) {
			return;
		}

		final String connectionPeerName = connection.getPeerName();
		if (connectionPeerName == null || connectionPeerName.equals(this.config.getPeerName())) {
			return;
		}

		if (this.connectionService.removeConnection(connection)) {
			this.cancelPings(connection, connectionPeerName);
			this.cancelPongs(connectionPeerName);
		}
	}

	public void cancelPings(Connection connection, String removedPeerName) {
		if (this.running) {
			this.pingService.cancelPings(connection, removedPeerName);
		} else {
			log.warn("Pings of {} can't be cancelled since not running", removedPeerName);
		}
	}

	public void cancelPongs(final String removedPeerName) {
		if (!this.running) {
			log.warn("Pongs of {} not cancelled since not running", removedPeerName);
			return;
		}

		this.pingService.cancelPongs(removedPeerName);
	}

	public synchronized void handlePing(Connection connection, Ping ping) {
		if (this.running) {
			this.pingService.handlePing((InetSocketAddress) this.bindChannel.localAddress(), connection, ping);
		} else {
			log.warn("Ping of {} is ignored since not running", connection.getPeerName());
		}
	}

	public synchronized void handlePong(Connection connection, Pong pong) {
		if (this.running) {
			this.pingService.handlePong(pong);
		} else {
			log.warn("Pong of {} is ignored since not running", connection.getPeerName());
		}
	}

	public synchronized void keepAlivePing() {
		if (!this.running) {
			log.warn("Periodic ping ignored since not running");
			return;
		}

		final int numberOfConnections = this.connectionService.getNumberOfConnections();
		if (numberOfConnections > 0) {
			final boolean discoveryPingEnabled = numberOfConnections < P2PConfig.DEFAULT_MIN_NUMBER_OF_ACTIVE_CONNECTIONS;
			this.pingService.keepAlive(discoveryPingEnabled);
		} else {
			log.debug("No auto ping since there is no connection");
		}
	}

	public synchronized void timeoutPings() {
		if (!this.running) {
			log.warn("Timeout pings ignored since not running");
			return;
		}

		final Collection<Pong> pongs = this.pingService.timeoutPings();
		final int availableConnectionSlots = P2PConfig.DEFAULT_MIN_NUMBER_OF_ACTIVE_CONNECTIONS - this.connectionService.getNumberOfConnections();

		if (availableConnectionSlots > 0) {
			final List<Pong> notConnectedPeers = new ArrayList<>();
			for (final Pong pong : pongs) {
				if (!this.config.getPeerName().equals(pong.getPeerName()) && !this.connectionService.isConnectedTo(pong.getPeerName())) {
					notConnectedPeers.add(pong);
				}
			}

			Collections.shuffle(notConnectedPeers);
			for (int i = 0, j = Math.min(availableConnectionSlots, notConnectedPeers.size()); i < j; i++) {
				final Pong peerToConnect = notConnectedPeers.get(i);
				final String host = peerToConnect.getServerHost();
				final int port = peerToConnect.getServerPort();
				log.debug("Auto-connecting to {} via {}:{}", peerToConnect.getPeerName(), host, port);
				this.connectTo(host, port, null);
			}
		}
	}

	public synchronized void broadcastMsg(Message message) {
		final Collection<Connection> connections = this.connectionService.getConnectionsValues();
		log.debug("Connections: " + connections.size());
		for (final Connection connection : connections) {
			log.debug("Send Message to Peer {}", connection.getPeerName());
			connection.send(message);
		}
	}

	public synchronized void sendMsg(String peerName, Message message) {
		final Connection connection = this.connectionService.getConnection(peerName);
		if (connection != null) {
			connection.send(message);
		} else {
			log.warn("This peer {} is not connected to {}", this.config.getPeerName(), peerName);
		}
	}

	public synchronized void handleJoinMessage(Message message) {
		final JoinMessage msg = (JoinMessage) message;
		final boolean isConnected = this.connectionService.isConnectedTo(msg.peerName());
		if (!isConnected && !msg.peerName().equals(this.config.getPeerName())) {
			final CompletableFuture<Void> connectToHostFuture = new CompletableFuture<>();
			this.connectTo(msg.host(), msg.port(), connectToHostFuture);
			log.debug("Join-Message -> Established connection to Peer {} at {}:{}.", msg.peerName(), msg.host(), msg.port());
		}
	}

	public synchronized void handleBroadcastMessage(Message message, String peerName) {
		final BroadcastMessage broadcastMessage = (BroadcastMessage) message;
		log.debug("Received Message from {} with content.", peerName);
		this.sink.tryEmitNext(broadcastMessage.jsonData().replaceAll("[\\n\t ]", "") + "\r\n");
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void disconnect(final String peerName) {
		if (!this.running) {
			log.warn("Not disconnected from {} since not running", peerName);
			return;
		}
		final Connection connection = this.connectionService.getConnection(peerName);
		if (connection != null) {
			log.debug("Disconnecting this peer {} from {}", this.config.getPeerName(), peerName);
			connection.close();
		}
	}

	public synchronized void setBindChannel(final Channel bindChannel) {
		this.bindChannel = bindChannel;
	}

	public void ping(final CompletableFuture<Collection<String>> futureToNotify) {
		if (!this.running) {
			futureToNotify.completeExceptionally(new RuntimeException("Disconnected!"));
			return;
		}
		this.pingService.ping(futureToNotify);
	}

	public synchronized void leave(final CompletableFuture<Void> futureToNotify) {
		if (!this.running) {
			log.warn("{} already shut down!", this.config.getPeerName());
			futureToNotify.complete(null);
			return;
		}

		this.bindChannel.closeFuture().addListener(future -> {
			if (future.isSuccess()) {
				futureToNotify.complete(null);
			} else {
				futureToNotify.completeExceptionally(future.cause());
			}
		});

		this.pingService.cancelOwnPing();
		this.pingService.cancelPongs(this.config.getPeerName());
		final CancelPong cancelPongs = new CancelPong(this.config.getPeerName());
		for (final Connection connection : this.connectionService.getConnectionsValues()) {
			connection.send(cancelPongs);
			connection.close();
		}
		this.bindChannel.close();
		this.running = false;
	}

	public synchronized void connectTo(final String host, final int port, final CompletableFuture<Void> futureToNotify) {
		if (this.running) {
			this.connectionService.connectTo(this, host, port, futureToNotify);
		} else {
			futureToNotify.completeExceptionally(new RuntimeException("Server is not running"));
		}
	}

	public synchronized Collection<Connection> connections() {
		return this.connectionService.getConnectionsValues();
	}
}
