package de.dlr.p2p.connection.ping;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.connection.message.Message;
import de.dlr.p2p.peer.Peer;

public final class CancelPong implements Message {

	private static final long serialVersionUID = 5147827390577329607L;

	private final String peerName;

	public CancelPong(String peerName) {
		this.peerName = peerName;
	}

	@Override
	public void handle(Peer peer, Connection connection) {
		peer.cancelPongs(this.peerName);
	}

	@Override
	public String toString() {
		return "RemovePongs{" +
				"peerName='" + this.peerName + '\'' +
				'}';
	}

}
