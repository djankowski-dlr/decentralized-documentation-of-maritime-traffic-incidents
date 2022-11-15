package de.dlr.ddomtia.service;

import de.dlr.ddomtia.util.Util;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;

@Log4j2
@Service
public final class ConnectionService {
	private static final String CONNECT_ENDPOINT = "/connect";
	private static final String DISCONNECT_ALL_ENDPOINT = "/disconnectALL";
	private final String host;
	private final int port;
	private final int p2pPort;
	private final RestOperations restTemplate;
	private final String p2pEndpoint;

	@Autowired
	public ConnectionService(RestOperations restOperations, @Value("${connection.host}") String host,
							 @Value("${connection.port}") int port, @Value("${p2p.port}") int p2pPort,
							 @Value("${p2p.endpoint}") String p2pEndpoint) {
		this.host = host;
		this.port = port;
		this.p2pPort = p2pPort;
		this.restTemplate = restOperations;
		this.p2pEndpoint = p2pEndpoint;
	}

	public void connectToP2P() {
		if (this.p2pPort == this.port) {
			return;
		}

		final String url = this.p2pEndpoint + ConnectionService.CONNECT_ENDPOINT + "?host=" + this.host + "&port=" + this.port;
		Util.sendHTTPRequest(this.restTemplate, url, "", HttpMethod.GET);
	}

	public void leaveFromP2P() {
		Util.sendHTTPRequest(this.restTemplate, this.p2pEndpoint + DISCONNECT_ALL_ENDPOINT, "", HttpMethod.GET);
		log.debug("Left P2P Network");
	}
}
