package de.dlr.ddomtia.service;

import java.time.Duration;
import java.util.Objects;

import com.google.gson.GsonBuilder;

import de.dlr.ddomtia.Message;
import de.dlr.ddomtia.identity.IdentityRegister;
import de.dlr.ddomtia.util.gson.MessageDeserializer;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.Disposable;

@Log4j2
@Service
public final class P2PService {
	private final WebClient webClient;
	private final IdentityRegister identityRegister;
	private Disposable disposable;

	@Autowired
	public P2PService(IdentityRegister identityRegister, @Value("${p2p.endpoint}") String p2pEndpoint) {
		this.webClient = WebClient.create(p2pEndpoint);
		this.identityRegister = identityRegister;
	}

	public void createTCPClient() {
		this.disposable = this.webClient.get().uri("/subscribe").retrieve().bodyToFlux(String.class).delayElements(Duration.ofMillis(10)).subscribe(s -> {
			final Message msg = new GsonBuilder().registerTypeAdapter(Message.class, new MessageDeserializer()).create().fromJson(s, Message.class);
			this.identityRegister.createOrUpdateIdentityByMessage(msg);
		});
	}

	public void dispose() {
		this.disposable.dispose();
	}

	public void waitForMessages(int iteration, int participants) {
		while (this.identityRegister.getIdentities().size() != participants || this.identityRegister.getIdentities()
				.stream().map(i -> i.getTSAsOfIteration(iteration)).anyMatch(t -> Objects.isNull(t) || t.isEmpty())) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				log.error("Interrupted", e);
			}
		}
		log.debug("Finished waiting for Iteration {}.", iteration);
	}
}
