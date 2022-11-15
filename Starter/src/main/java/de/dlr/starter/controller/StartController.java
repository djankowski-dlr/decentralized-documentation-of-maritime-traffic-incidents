package de.dlr.starter.controller;

import java.util.List;

import lombok.SneakyThrows;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.util.HttpConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class StartController {
	private final AsyncHttpClient asyncHttpClient;
	private final String portAddress;
	private final String vtsAddress;
	private final String vesselAddress;

	public StartController(AsyncHttpClient asyncHttpClient,
						   @Value("${address.port}") String portAddress,
						   @Value("${address.vts}") String vtsAddress,
						   @Value("${address.vessel}") String vesselAddress) {

		this.asyncHttpClient = asyncHttpClient;
		this.portAddress = portAddress;
		this.vtsAddress = vtsAddress;
		this.vesselAddress = vesselAddress;
	}

	@SneakyThrows
	@GetMapping(value = "/dlr/start")
	public ResponseEntity<String> processTSARequest() {
		final List<String> addressList = List.of(this.portAddress, this.vtsAddress, this.vesselAddress);
		addressList.forEach(address -> {
			final Request getRequest = new RequestBuilder(HttpConstants.Methods.GET).setUrl(address).build();
			this.asyncHttpClient.executeRequest(getRequest);
		});
		return ResponseEntity.ok("");
	}

}
