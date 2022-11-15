package de.dlr.ddomtia.identity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.dlr.ddomtia.Message;
import de.dlr.ddomtia.util.Util;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Getter
@Component
public final class IdentityRegister {
	private final List<Identity> identities;
	private Identity ownIdentity;
	private final String path;

	public IdentityRegister(@Value("${identity.file.path}") String path) {
		final Identity identity = Util.loadDataSources(path, Identity.class);
		identity.addToIterationMap();
		this.identities = new ArrayList<>();
		this.identities.add(identity);
		this.ownIdentity = identity;
		this.path = path;
		if (identity.getTsaIterationMap().isEmpty()) {
			log.warn("Trust relationships should be established.");
			System.exit(5);
		}
		log.info("Identity information has been loaded from {}", path);
	}

	public synchronized void createOrUpdateIdentityByMessage(Message msg) {
		final Identity identity = this.identities.stream().filter(i -> i.getIdentityName().equals(msg.identity()))
				.findFirst().orElse(Identity.createPlainIdentity(msg.identity(), msg.maxIteration()));
		if (!this.identities.contains(identity)) {
			this.identities.add(identity);
		}
		identity.addToIterationMap(msg.iteration(), msg.entries());
	}

	public void reloadAndStoreIdentity(String identityJsonString) {
		final Identity identity = Util.gson.fromJson(identityJsonString, Identity.class);
		this.identities.remove(this.ownIdentity);
		this.identities.add(identity);
		this.ownIdentity = identity;
		try {
			Util.writeToFile(this.path, identityJsonString);
			log.debug("Overwriting file was successful");
		} catch (IOException e) {
			log.error("cannot override identity file.", e);
		}
		if (identity.getTsaIterationMap().isEmpty()) {
			System.exit(5);
		}
	}

	public void reset() {
		this.identities.clear();
		this.ownIdentity = Util.loadDataSources(this.path, Identity.class);
		this.ownIdentity.addToIterationMap();
		this.identities.add(this.ownIdentity);
	}


}
