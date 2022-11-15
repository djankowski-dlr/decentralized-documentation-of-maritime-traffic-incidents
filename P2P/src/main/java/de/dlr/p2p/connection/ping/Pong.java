package de.dlr.p2p.connection.ping;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.connection.message.Message;
import de.dlr.p2p.peer.Peer;

import lombok.Getter;

@Getter
public final class Pong implements Message {
	private static final long serialVersionUID = 2748377163219868853L;

	private final String pingPeerName;
	private final String senderPeerName;
	private final String peerName;
	private final String serverHost;
	private final int serverPort;
	private final int ttl;
	private final int hops;

	public Pong(String pingPeerName, String senderPeerName, String peerName, String serverHost, int serverPort, int ttl, int hops) {
		this.pingPeerName = pingPeerName;
		this.senderPeerName = senderPeerName;
		this.peerName = peerName;
		this.serverHost = serverHost;
		this.serverPort = serverPort;
		this.ttl = ttl;
		this.hops = hops;
	}


	public Pong next(final String thisPeerName) {
		return this.ttl > 1 ? new Pong(this.pingPeerName, thisPeerName, this.peerName, this.serverHost, this.serverPort, this.ttl - 1, this.hops + 1) : null;
	}

	@Override
	public void handle(Peer peer, Connection connection) {
		peer.handlePong(connection, this);
	}

	@Override
	public String toString() {
		return "Pong{" +
				"pingPeerName='" + this.pingPeerName + '\'' +
				", senderPeerName='" + this.senderPeerName + '\'' +
				", peerName='" + this.peerName + '\'' +
				", serverHost='" + this.serverHost + '\'' +
				", serverPort=" + this.serverPort +
				", ttl=" + this.ttl +
				", hops=" + this.hops +
				'}';
	}
}
