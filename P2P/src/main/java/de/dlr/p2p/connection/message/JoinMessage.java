package de.dlr.p2p.connection.message;

import java.io.Serial;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.peer.Peer;

import lombok.extern.log4j.Log4j2;

@Log4j2
public record JoinMessage(String peerName, String host, int port) implements Message {
	@Serial
	private static final long serialVersionUID = 1332409481651160075L;

	@Override
	public void handle(Peer peer, Connection connection) {
		log.debug("Peer {} has send a new Join Message {}", connection.getPeerName(), this);
		peer.handleJoinMessage(this);
	}

	@Override
	public String toString() {
		return "JoinMessage{" + "peerName='" + this.peerName + '\'' + ", host='" + this.host + '\'' + ", port=" + this.port + '}';
	}
}
