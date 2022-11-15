package de.dlr.p2p.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import de.dlr.p2p.config.P2PConfig;
import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.connection.PeerChannelHandler;
import de.dlr.p2p.connection.PeerChannelInitializer;
import de.dlr.p2p.peer.Peer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ObjectEncoder;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public final class ConnectionService {
	private final EventLoopGroup networkEventLoopGroup;
	private final EventLoopGroup peerEventLoopGroup;
	@Getter
	private final Map<String, Connection> connections;
	private final ObjectEncoder encoder;
	private final P2PConfig config;

	@Autowired
	public ConnectionService(EventLoopGroup networkEventLoopGroup, EventLoopGroup peerEventLoopGroup,
							 ObjectEncoder encoder, P2PConfig config) {

		this.networkEventLoopGroup = networkEventLoopGroup;
		this.peerEventLoopGroup = peerEventLoopGroup;
		this.encoder = encoder;
		this.connections = Collections.synchronizedMap(new HashMap<>());
		this.config = config;
	}

	public void addConnection(Connection connection) {
		final String peerName = connection.getPeerName();
		final Connection previousConnection = this.connections.put(peerName, connection);
		log.debug("Connection to {} is added.", peerName);
		if (previousConnection != null) {
			previousConnection.close();
			log.warn("Already existing connection to {} is closed.", peerName);
		}
	}

	public boolean removeConnection(Connection connection) {
		final Connection removeConnection = this.connections.remove(connection.getPeerName());
		final boolean removed = removeConnection != null;
		if (removed) {
			log.debug("{} is removed from connections!", connection.getPeerName());
		} else {
			log.warn("Connection to {} is not removed since not found in connections!", connection.getPeerName());
		}
		return removed;
	}

	public void connectTo(final Peer peer, final String host, final int port, final CompletableFuture<Void> futureToNotify) {
		final PeerChannelHandler handler = new PeerChannelHandler(this.config, peer);
		final PeerChannelInitializer initializer = new PeerChannelInitializer(this.encoder, this.peerEventLoopGroup, handler);
		final Bootstrap clientBootstrap = new Bootstrap();
		clientBootstrap.group(this.networkEventLoopGroup).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).handler(initializer);

		final ChannelFuture connectFuture = clientBootstrap.connect(host, port);
		if (futureToNotify != null) {
			connectFuture.addListener((ChannelFuture future) -> {
				if (future.isSuccess()) {
					futureToNotify.complete(null);
					log.debug("Successfully connect to {}:{}", host, port);
				} else {
					futureToNotify.completeExceptionally(future.cause());
					log.error("Could not connect to " + host + ":" + port, future.cause());
				}
			});
		}
	}

	public int getNumberOfConnections() {
		return this.connections.size();
	}

	public boolean isConnectedTo(String peerName) {
		return this.connections.containsKey(peerName);
	}

	public Connection getConnection(String peerName) {
		return this.connections.get(peerName);
	}

	public Collection<Connection> getConnectionsValues() {
		return Collections.unmodifiableCollection(this.connections.values());
	}
}
