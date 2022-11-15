package de.dlr.p2p.connection.message;

import java.io.Serial;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.peer.Peer;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public record Handshake(String peerName, int port) implements Message {
	@Serial
	private static final long serialVersionUID = 213352944600339280L;

	@Override
	public void handle(Peer peer, Connection connection) {
		final String name = connection.getPeerName();
		if (name == null) {
			connection.setPeerName(this.peerName);
			peer.handleConnectionOpened(connection, this.port);
			return;
		}
		if (!name.equals(this.peerName)) {
			log.warn("Mismatching peer name received from connection! Existing: {} Received: {}", name, this.peerName);
		}
	}
}
