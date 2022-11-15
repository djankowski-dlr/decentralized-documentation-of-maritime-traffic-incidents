package de.dlr.p2p.connection.message;

import java.io.Serial;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.peer.Peer;

import lombok.extern.log4j.Log4j2;

@Log4j2
public record BroadcastMessage(String jsonData) implements Message {
	@Serial
	private static final long serialVersionUID = 6777332849366330679L;

	@Override
	public void handle(Peer peer, Connection connection) {
		peer.handleBroadcastMessage(this, connection.getPeerName());
		log.debug("Read from {}.", connection.getPeerName());
	}

	@Override
	public String toString() {
		return "BroadcastMessage{" + "jsonData='" + this.jsonData + '\'' + '}';
	}
}
