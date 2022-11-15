package de.dlr.p2p.connection.message;

import java.io.Serial;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.peer.Peer;

import lombok.extern.log4j.Log4j2;

@Log4j2
public record KeepAlive() implements Message {
	@Serial
	private static final long serialVersionUID = -4998803925489492616L;

	@Override
	public void handle(Peer peer, Connection connection) {
		log.debug("Keep alive ping received from {}", connection);
	}
}
