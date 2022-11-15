package de.dlr.p2p.connection.ping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import de.dlr.p2p.connection.Connection;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import static java.util.Collections.unmodifiableList;

@Log4j2
public final class PingContext {
	@Getter
	private final Ping ping;
	@Getter
	private final Connection connection;
	private final Map<String, Pong> pongs;
	private final List<CompletableFuture<Collection<String>>> futures;
	public PingContext(Ping ping, Connection connection) {
		this.ping = ping;
		this.pongs = new HashMap<>();
		this.connection = connection;
		this.futures = new ArrayList<>();
	}

	public String getPeerName() {
		return this.ping.getPeerName();
	}
	public Collection<Pong> getPongs() {
		return Collections.unmodifiableCollection(this.pongs.values());
	}

	public void handlePong(final String thisServerName, final Pong pong) {
		final String pongServerName = pong.getPeerName();
		if (this.pongs.containsKey(pongServerName)) {
			log.debug("{} from {} is already handled for {}", pong, pongServerName, this.ping.getPeerName());
			return;
		}

		this.pongs.put(pongServerName, pong);

		log.debug("Handling {} from {} for {}. Pong #: {}", pong, pongServerName, this.ping.getPeerName(), this.pongs.size());

		if (!thisServerName.equals(this.ping.getPeerName())) {
			if (this.connection != null) {
				final Pong next = pong.next(thisServerName);
				if (next != null) {
					log.debug("Forwarding {} to {} for initiator {}", pong, this.connection.getPeerName(), this.ping.getPeerName());
					this.connection.send(next);
				} else {
					log.error("Invalid {} received from {} for {}", pong, pongServerName, this.ping.getPeerName());
				}
			} else {
				log.error("No connection is found in ping context for {} from {} for {}", pong, pongServerName, this.ping.getPeerName());
			}
		}
	}

	public void addFuture(CompletableFuture<Collection<String>> future) {
		this.futures.add(future);
	}

	public boolean hasTimeout() {
		return this.ping.getPingStartTimestamp() + this.ping.getPingTimeoutDurationInMillis() <= System.currentTimeMillis();
	}

	public List<CompletableFuture<Collection<String>>> getFutures() {
		return unmodifiableList(this.futures);
	}

	public boolean removePong(final String serverName) {
		return this.pongs.remove(serverName) != null;
	}

	@Override
	public String toString() {
		return "PingContext{" +
				"pongs=" + this.pongs +
				", connection=" + this.connection +
				", ping=" + this.ping +
				'}';
	}
}
