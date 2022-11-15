package de.dlr.p2p.connection.ping;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.connection.message.Message;
import de.dlr.p2p.peer.Peer;

public final class CancelPing implements Message {

	private static final long serialVersionUID = -8650899535821394626L;

	private final String peerName;

	public CancelPing(String peerName) {
		this.peerName = peerName;
	}

	@Override
	public void handle(Peer peer, Connection connection) {
		peer.cancelPings(connection, this.peerName);
	}

	@Override
	public String toString() {
		return "RemovePings{" +
				"peerName='" + this.peerName + '\'' +
				'}';
	}
}
