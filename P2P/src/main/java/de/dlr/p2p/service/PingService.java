package de.dlr.p2p.service;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import de.dlr.p2p.config.P2PConfig;
import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.connection.message.KeepAlive;
import de.dlr.p2p.connection.ping.CancelPing;
import de.dlr.p2p.connection.ping.CancelPong;
import de.dlr.p2p.connection.ping.Ping;
import de.dlr.p2p.connection.ping.PingContext;
import de.dlr.p2p.connection.ping.Pong;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public final class PingService {
	private final ConnectionService connectionService;
	private final P2PConfig config;
	private final Map<String, PingContext> currentPings;
	private long autoPingCount;

	@Autowired
	public PingService(ConnectionService connectionService, P2PConfig config) {
		this.currentPings = new HashMap<>();
		this.connectionService = connectionService;
		this.config = config;
		this.autoPingCount = 0;
	}

	public void keepAlive(boolean discoveryPingEnabled) {
		if (!this.currentPings.containsKey(this.config.getPeerName())) {
			if (incrementAutoPingCountAndCheckIfFullPing() && discoveryPingEnabled) {
				if (!this.currentPings.containsKey(this.config.getPeerName())) {
					discoveryPing();
				} else {
					log.debug("Periodic discovery ping skipped since there exists a ping already");
				}
			} else {
				keepAlive();
			}
		}
	}

	public void ping(CompletableFuture<Collection<String>> future) {
		PingContext pingContext = this.currentPings.get(this.config.getPeerName());
		if (pingContext == null) {
			pingContext = discoveryPing();
		} else {
			log.debug("Attaching to the already existing ping context");
		}

		if (future != null) {
			pingContext.addFuture(future);
		}
	}

	public void handlePing(InetSocketAddress bindAddress, Connection connection, Ping ping) {
		final String pingPeerName = ping.getPeerName();
		if (this.currentPings.containsKey(pingPeerName)) {
			log.debug("Skipping ping of {} since it is already handled.", pingPeerName);
			return;
		}

		if (pingPeerName.equals(connection.getPeerName())) {
			log.debug("Handling {} of initiator {} with ttl={}", ping, pingPeerName, ping.getTtl());
		} else {
			log.debug("Handling {} of initiator {} and forwarder {} with ttl={} and hops={}", ping, pingPeerName, connection.getPeerName(), ping.getTtl(), ping.getHops());
		}

		ping.setPingStartTimestamp(System.currentTimeMillis());
		this.currentPings.put(pingPeerName, new PingContext(ping, connection));

		final Pong pong = new Pong(pingPeerName, this.config.getPeerName(), this.config.getPeerName(),
				bindAddress.getAddress().getHostAddress(), bindAddress.getPort(), ping.getHops() + 1, 0);
		connection.send(pong);

		final Ping next = ping.next();
		if (next != null) {
			for (final Connection neighbour : this.connectionService.getConnectionsValues()) {
				if (!neighbour.equals(connection) && !neighbour.getPeerName().equals(ping.getPeerName())) {
					log.debug("Forwarding {} to {} for initiator {}", next, neighbour.getPeerName(), ping.getPeerName());
					neighbour.send(next);
				}
			}
		}
	}

	public void propagatePingsToNewConnection(Connection connection) {
		for (final PingContext pingContext : this.currentPings.values()) {
			if (!pingContext.getPeerName().equals(connection.getPeerName())) {
				final Ping next = pingContext.getPing().next();
				if (next != null) {
					connection.send(next);
					log.debug("{} sent to new connection {}", next, connection.getPeerName());
				}
			}
		}
	}

	public void handlePong(final Pong pong) {
		if (pong.getPeerName().equals(this.config.getPeerName())) {
			log.warn("Received {} from itself", pong);
			return;
		}
		final String pingPeerName = pong.getPingPeerName();
		final PingContext pingContext = this.currentPings.get(pingPeerName);
		if (pingContext == null) {
			log.warn("No ping context is found for {} from {} for initiator {}", pong, pong.getPeerName(), pingPeerName);
			return;
		}

		pingContext.handlePong(this.config.getPeerName(), pong);
	}

	public void cancelPongs(String disconnectedPeerName) {
		final Iterator<Map.Entry<String, PingContext>> pingIt = this.currentPings.entrySet().iterator();
		final CancelPong cancelPongs = new CancelPong(disconnectedPeerName);
		while (pingIt.hasNext()) {
			final Map.Entry<String, PingContext> pingEntry = pingIt.next();
			final String pingPeerName = pingEntry.getKey();
			final PingContext pingContext = pingEntry.getValue();

			// Remove Pong messages of disconnected peer in ongoing ping operations
			if (pingContext.removePong(disconnectedPeerName)) {
				final Connection pingOwnerConnection = pingContext.getConnection();
				if (pingOwnerConnection != null) {
					log.debug("Removed pong of {} in ping of {}. Forwarding {} to {}", disconnectedPeerName, pingPeerName,
							cancelPongs, pingOwnerConnection.getPeerName());
					pingOwnerConnection.send(cancelPongs);
				} else {
					log.debug("Removed pong of {} in ping of {}", disconnectedPeerName, pingPeerName);
				}
			}
			boolean rePing = false;
			for (final Pong pong : new ArrayList<>(pingContext.getPongs())) {
				if (pong.getSenderPeerName().equals(disconnectedPeerName)) {
					pingContext.removePong(pong.getPeerName());
					rePing = true;
					log.debug("Removed Pong of {} in ping of {} since it was sent by {}", pong.getPeerName(), pingPeerName,
							disconnectedPeerName);
					final Connection connection = pingContext.getConnection();
					if (connection != null) {
						final CancelPong msg = new CancelPong(pong.getPeerName());
						connection.send(msg);
						log.debug("Forwarded {} to {} for ping of {}", msg, connection.getPeerName(), pingPeerName);
					}
				}
			}
			if (rePing) {
				final Ping next = pingContext.getPing().next();
				if (next != null) {
					log.debug("Will re-send {}", next);
					for (final Connection connection : this.connectionService.getConnectionsValues()) {
						if (!(connection.getPeerName().equals(pingPeerName) || connection.getPeerName()
								.equals(disconnectedPeerName) || !connection.equals(pingContext.getConnection()))) {
							connection.send(next);
							log.debug("{} re-sent to {} because {} left", next, connection.getPeerName(), disconnectedPeerName);
						}
					}
				}
			}
		}
	}

	public void cancelPings(Connection connection, final String disconnectedPeerName) {
		final Iterator<Map.Entry<String, PingContext>> pingIt = this.currentPings.entrySet().iterator();
		while (pingIt.hasNext()) {
			final Map.Entry<String, PingContext> pingEntry = pingIt.next();
			final String pingPeerName = pingEntry.getKey();
			final PingContext pingContext = pingEntry.getValue();
			final Connection pingOwnerConnection = pingContext.getConnection();
			boolean shouldRemove = pingPeerName.equals(disconnectedPeerName) && connection.equals(pingOwnerConnection);
			if (!shouldRemove) {
				shouldRemove = pingOwnerConnection != null && pingOwnerConnection.getPeerName().equals(disconnectedPeerName);
			}
			if (shouldRemove) {
				log.debug("Removing ping of {} since it is disconnected", pingPeerName);
				pingIt.remove();
				final CancelPing cancelPings = new CancelPing(disconnectedPeerName);
				for (final Pong pong : pingContext.getPongs()) {
					final Connection c = this.connectionService.getConnection(pong.getPeerName());
					if (c != null) {
						c.send(cancelPings);
						log.debug("{} sent to {}", cancelPings, pong.getPeerName());
					} else {
						log.warn("{} not sent to {} since there is no connection", cancelPings, pong.getPeerName());
					}
				}
			}
		}
	}

	public void cancelOwnPing() {
		final PingContext pingContext = this.currentPings.get(this.config.getPeerName());
		if (pingContext != null) {
			log.debug("Cancelling own ping");
			for (final CompletableFuture<Collection<String>> future : pingContext.getFutures()) {
				future.cancel(true);
			}
		}
	}

	public Collection<Pong> timeoutPings() {
		Collection<Pong> pongs = new ArrayList<>();
		final Iterator<Map.Entry<String, PingContext>> pingIt = this.currentPings.entrySet().iterator();
		while (pingIt.hasNext()) {
			final Map.Entry<String, PingContext> pingEntry = pingIt.next();
			final String pingPeerName = pingEntry.getKey();
			final PingContext pingContext = pingEntry.getValue();
			if (pingContext.hasTimeout()) {
				pingIt.remove();
				if (this.config.getPeerName().equals(pingPeerName)) {
					pongs = pingContext.getPongs();
					final Set<String> peers = new HashSet<>();
					for (final Pong pong : pongs) {
						peers.add(pong.getPeerName());
					}
					peers.add(this.config.getPeerName());
					log.debug("Ping for {} has timed out. Notifying futures with # peers: {}", pingContext.getPeerName(), peers.size());
					for (final CompletableFuture<Collection<String>> future : pingContext.getFutures()) {
						future.complete(peers);
					}
				} else {
					log.debug("Ping for {} has timed out.", pingContext.getPeerName());
				}
			}
		}
		return pongs;
	}

	private PingContext discoveryPing() {
		final int ttl = P2PConfig.DEFAULT_PING_TTL;
		log.debug("Doing a full ping with ttl={}", ttl);
		final Ping ping = new Ping(this.config.getPeerName(), ttl, 0, TimeUnit.SECONDS.toMillis(P2PConfig.DEFAULT_PING_TIMEOUT_SECONDS));
		final PingContext pingContext = new PingContext(ping, null);
		this.currentPings.put(this.config.getPeerName(), pingContext);
		ping.setPingStartTimestamp(System.currentTimeMillis());
		for (final Connection connection : this.connectionService.getConnectionsValues()) {
			connection.send(ping);
		}
		return pingContext;
	}

	private boolean incrementAutoPingCountAndCheckIfFullPing() {
		++this.autoPingCount;
		return this.autoPingCount % P2PConfig.DEFAULT_AUTO_DISCOVERY_PING_FREQUENCY == 0;
	}

	private void keepAlive() {
		log.debug("Doing a keep-alive ping");
		final KeepAlive keepAlive = new KeepAlive();
		for (final Connection connection : this.connectionService.getConnectionsValues()) {
			connection.send(keepAlive);
		}
	}
}
