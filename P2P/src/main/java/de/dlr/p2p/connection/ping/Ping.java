package de.dlr.p2p.connection.ping;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.connection.message.Message;
import de.dlr.p2p.peer.Peer;

import lombok.Getter;
import lombok.Setter;

@Getter
public final class Ping implements Message {

	private static final long serialVersionUID = -4362142418375530711L;
	private final String peerName;
	private final int ttl;
	private final int hops;
	private final long pingTimeoutDurationInMillis;
	@Setter
	private transient long pingStartTimestamp;

	public Ping(String peerName, int ttl, int hops, long pingTimeoutDurationInMillis) {
		this.peerName = peerName;
		this.ttl = ttl;
		this.hops = hops;
		this.pingTimeoutDurationInMillis = pingTimeoutDurationInMillis;
	}

	public Ping next() {
		return this.ttl > 1 ? new Ping(this.peerName, this.ttl - 1, this.hops + 1, this.pingTimeoutDurationInMillis) : null;
	}

	@Override
	public void handle(Peer peer, Connection connection) {
		peer.handlePing(connection, this);
	}

	@Override
	public String toString() {
		return "Ping{" +
				"peerName=" + this.peerName +
				", ttl=" + this.ttl +
				", hops=" + this.hops +
				", pingStartTimestamp=" + this.pingStartTimestamp +
				'}';
	}
}
