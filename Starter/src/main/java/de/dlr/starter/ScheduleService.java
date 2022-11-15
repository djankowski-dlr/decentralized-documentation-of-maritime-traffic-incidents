package de.dlr.starter;

import java.util.List;

import lombok.extern.log4j.Log4j2;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.util.HttpConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public final class ScheduleService {

	private final AsyncHttpClient asyncHttpClient;
	private final String portAddress;
	private final String vtsAddress;
	private final String vesselAddress;

	@Autowired
	public ScheduleService(AsyncHttpClient asyncHttpClient,
						   @Value("${address.port}") String portAddress,
						   @Value("${address.vts}") String vtsAddress,
						   @Value("${address.vessel}") String vesselAddress) {

		this.asyncHttpClient = asyncHttpClient;
		this.portAddress = portAddress;
		this.vtsAddress = vtsAddress;
		this.vesselAddress = vesselAddress;
	}

	@Scheduled(fixedDelay = 3500)
	public void run() {
		final List<String> addressList = List.of(this.portAddress, this.vtsAddress, this.vesselAddress);
		addressList.forEach(address -> {
			final Request getRequest = new RequestBuilder(HttpConstants.Methods.GET).setUrl(address).build();
			this.asyncHttpClient.executeRequest(getRequest);
		});
	}
}
