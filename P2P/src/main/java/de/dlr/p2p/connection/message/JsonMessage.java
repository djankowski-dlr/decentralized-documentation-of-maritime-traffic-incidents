package de.dlr.p2p.connection.message;

import java.io.Serial;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.peer.Peer;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public record JsonMessage(@Getter String jsonData) implements Message {
	@Serial
	private static final long serialVersionUID = 4774946063037173943L;

	@Override
	public void handle(Peer peer, Connection connection) {
		log.debug("Read from {} msg, say:'{}'.", connection.getPeerName(), this.jsonData());
	}
}
