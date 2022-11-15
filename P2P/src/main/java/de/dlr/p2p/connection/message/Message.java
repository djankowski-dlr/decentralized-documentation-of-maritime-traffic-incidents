package de.dlr.p2p.connection.message;

import java.io.Serializable;

import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.peer.Peer;

public interface Message extends Serializable {

	void handle(Peer peer, Connection connection);
}
