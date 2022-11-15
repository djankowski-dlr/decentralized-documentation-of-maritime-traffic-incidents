package de.dlr.p2p.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dlr.p2p.config.P2PConfig;
import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.peer.Peer;
import de.dlr.p2p.peer.PeerHandle;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@Log4j2
@RestController
public final class P2PController {
	private final PeerHandle peerHandle;
	private final Peer peer;

	@Autowired
	public P2PController(PeerHandle peerHandle, Peer peer) {
		this.peerHandle = peerHandle;
		this.peer = peer;

	}

	@GetMapping("/connect")
	public boolean connect(@RequestParam("host") String host, @RequestParam("port") int port) {
		this.peerHandle.connect(host, port);
		log.debug("connect -> {}:{}", host, port);
		return true;
	}

	@PostMapping("/broadcast")
	public boolean broadcast(@RequestBody String msg) {
		this.peerHandle.broadcast(msg);
		return true;
	}

	@GetMapping("/send")
	public boolean send(@RequestParam("peer") String peer, @RequestParam("msg") String msg) {
		this.peerHandle.send(peer, msg);
		log.debug("send to {} ,say: '{}'", peer, msg);
		return true;
	}

	@GetMapping("/leave")
	public boolean leave() {
		this.peerHandle.leave();
		log.debug("p2p network leave");
		return true;
	}

	@GetMapping("/disconnect")
	public boolean disconnect(@RequestParam("peer") String peer) {
		this.peerHandle.disconnect(peer);
		log.debug("disconnect peer:{}", peer);
		return true;
	}

	@GetMapping("/disconnectALL")
	public boolean disconnectAll() {
		this.peerHandle.connections().forEach(connection -> this.peerHandle.disconnect(connection.getPeerName()));
		return true;
	}

	@GetMapping("/list")
	public List<P2PConfig> list() {
		final Collection<Connection> connections = this.peerHandle.connections();
		final List<P2PConfig> list = new ArrayList<>();
		for (final Connection connection : connections) {
			list.add(new P2PConfig(connection.getPeerName(), connection.getPort()));
		}
		return list;
	}

	@GetMapping("/subscribe")
	public Flux<String> subscribe() {
		return this.peer.getFlux();

	}
}
