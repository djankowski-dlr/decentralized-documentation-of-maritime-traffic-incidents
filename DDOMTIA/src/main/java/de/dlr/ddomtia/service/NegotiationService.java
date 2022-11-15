package de.dlr.ddomtia.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.math.Quantiles;
import com.google.common.primitives.SignedBytes;
import com.google.gson.JsonArray;

import de.dlr.ddomtia.Message;
import de.dlr.ddomtia.event.StartEvent;
import de.dlr.ddomtia.event.TSAEvent;
import de.dlr.ddomtia.identity.Identity;
import de.dlr.ddomtia.identity.IdentityRegister;
import de.dlr.ddomtia.identity.TSAEntry;
import de.dlr.ddomtia.util.RunningState;
import de.dlr.ddomtia.util.Util;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public final class NegotiationService implements ApplicationListener<StartEvent> {
	private static final String LIST_ENDPOINT = "/list";
	private static final String BROADCAST_ENDPOINT = "/broadcast";
	private RunningState runningState;
	private final RestTemplate restTemplate;
	private final IdentityRegister identityRegister;
	private final String p2pEndpoint;
	private final ConnectionService connectionService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final P2PService p2PService;
	private final List<Long> negotiationTimes;
	private final List<Long> communicationTimes;


	@Autowired
	public NegotiationService(RestTemplate restTemplate, IdentityRegister identityRegister,
							  @Value("${p2p.endpoint}") String p2pEndpoint,
							  ApplicationEventPublisher applicationEventPublisher,
							  ConnectionService connectionService, P2PService p2PService) {

		this.restTemplate = restTemplate;
		this.identityRegister = identityRegister;
		this.applicationEventPublisher = applicationEventPublisher;
		this.connectionService = connectionService;
		this.p2pEndpoint = p2pEndpoint;
		this.runningState = RunningState.NOT_RUNNING;
		this.p2PService = p2PService;
		this.negotiationTimes = new ArrayList<>(1000);
		this.communicationTimes = new ArrayList<>(1000);
	}

	public void start(int part) throws InterruptedException {
		final long startCommunicationTime = System.currentTimeMillis();
		this.connectionService.leaveFromP2P();
		this.p2PService.createTCPClient();
		if (!this.isConnectedToP2PNetwork()) {
			this.connectionService.connectToP2P();
		}
		while (!this.hasNParticipants(2)) {
			Thread.sleep(10);
		}
		this.communicationTimes.add(System.currentTimeMillis() - startCommunicationTime);

		final double avgCommunication = this.communicationTimes.stream().mapToDouble(Long::doubleValue).average().getAsDouble();
		double medianCommunication = Quantiles.median().compute(this.communicationTimes.stream().mapToDouble(Long::doubleValue).toArray());

		log.info("Communication Time -> avg: {}, median: {}, Count: {}", avgCommunication, medianCommunication, this.communicationTimes.size());
		final long startNegotiationTime = System.currentTimeMillis();

		final Identity self = this.identityRegister.getOwnIdentity();

		final String list = Util.sendHTTPRequest(this.restTemplate, this.p2pEndpoint + NegotiationService.LIST_ENDPOINT, "", HttpMethod.GET);

		final JsonArray jsonList = Util.gson.fromJson(list, JsonArray.class);

		final int participants = part != 0 ? part : jsonList.size();

		log.debug("{} participants are entering the negotiations.", participants);
		log.debug("Start enter iteration phase.");

		if (participants == 1) {
			this.identityRegister.reset();
			return;
		}

		int maxIteration = 0;
		for (int j = 0; j <= maxIteration; j++) {
			log.debug("Enter iteration {}", j);

			final Set<TSAEntry> tsaEntries = Identity.determineTSASet(self, Math.min(j, self.getMaxIteration()));

			if (self.getMaxIteration() >= j) {
				final Message message = new Message(self.getIdentityName(), j, self.getMaxIteration(), tsaEntries);
				final String req = Util.gson.toJson(message, Message.class);
				Util.sendHTTPRequest(this.restTemplate, this.p2pEndpoint + NegotiationService.BROADCAST_ENDPOINT, req, HttpMethod.POST);
			}

			log.debug("Start waiting | Iteration: {}", j);

			this.p2PService.waitForMessages(j, participants);

			maxIteration = Math.max(this.identityRegister.getIdentities().stream().mapToInt(Identity::getMaxIteration).max().orElseThrow(), self.getMaxIteration());

			final Set<TSAEntry> tsaEntrySet = Identity.findTSAs(this.identityRegister.getIdentities(), j);

			if (!tsaEntrySet.isEmpty()) {
				log.debug("At least one TSA was found for all participants. TSAs: {}", tsaEntrySet.stream().map(TSAEntry::name).collect(Collectors.joining(", ")));
				this.runningState = RunningState.NOT_RUNNING;
				this.applicationEventPublisher.publishEvent(new TSAEvent(this, tsaEntries.iterator().next()));
				this.connectionService.leaveFromP2P();
				this.identityRegister.reset();
				this.p2PService.dispose();
				log.debug("Process finished.");
				this.negotiationTimes.add(System.currentTimeMillis() - startNegotiationTime);
				final double meanNegotiation = this.negotiationTimes.stream().mapToDouble(Long::doubleValue).average().getAsDouble();
				final double medianNegotiation = Quantiles.median().compute(this.negotiationTimes.stream().mapToDouble(Long::doubleValue).toArray());
				log.info("Negotiation Time -> avg: {}, median: {}, Count: {}", meanNegotiation, medianNegotiation, this.negotiationTimes.size());
				return;
			}
		}

		log.debug("No matching TSAs found");

		final Optional<Set<TSAEntry>> tsaEntrySetOptional = this.reducedTSADetermination(Sets.newHashSet(this.identityRegister.getIdentities()), this.identityRegister.getOwnIdentity());
		tsaEntrySetOptional.ifPresent(tsaEntries -> {
			this.applicationEventPublisher.publishEvent(new TSAEvent(this, tsaEntries.iterator().next()));
			log.info("TSA Entries: {}", tsaEntries.stream().map(TSAEntry::name).collect(Collectors.joining(", ")));
		});

		this.runningState = RunningState.NOT_RUNNING;
		this.connectionService.leaveFromP2P();
		this.identityRegister.reset();
		this.p2PService.dispose();
		log.debug("Process finished.");
	}

	public Optional<Set<TSAEntry>> reducedTSADetermination(Set<Identity> identities, Identity self) {
		final int maxIteration = identities.stream().mapToInt(Identity::getMaxIteration).max().orElseThrow();
		log.info("Start reduced TSA determination.");
		final Set<Set<Identity>> powerSet = Sets.powerSet(Sets.newHashSet(identities));
		final List<ReducedTSATriple> iterationList = new ArrayList<>();
		for (int i = identities.size(); i > 1; i--) {
			log.info("Enter TSA determination with {} participants.", i);
			final Set<Set<Identity>> targetSet = Util.getPowerSetOfIteration(powerSet, i);

			for (int j = 0; j <= maxIteration; j++) {

				for (final Set<Identity> identitySet : targetSet) {
					final Set<TSAEntry> intersection = Identity.findTSAs(identitySet, j);

					if (!intersection.isEmpty()) {
						iterationList.add(new ReducedTSATriple(identitySet, intersection));
					}
				}
				if (!iterationList.isEmpty()) {
					Collections.sort(iterationList);
					break;
				}
			}

			if (iterationList.isEmpty()) {
				continue;
			}
			log.info("At least one subset of Identities share al least one TSA.");
			final ReducedTSATriple reducedTSATriple = iterationList.get(0);

			if (reducedTSATriple.identities.contains(self)) {
				log.info("A TSA was found for the user.");
				this.identityRegister.reset();
				return Optional.of(reducedTSATriple.tsaEntries);
			} else {
				final Set<Identity> identitySet = Sets.difference(identities, reducedTSATriple.identities);
				return this.reducedTSADetermination(identitySet, self);
			}
		}
		log.info("No TSA could be found, use fallback TSA.");
		this.identityRegister.reset();
		return Optional.empty();
	}

	@Override
	public void onApplicationEvent(@NonNull StartEvent event) {
		try {
			this.start(3);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}

	private boolean isConnectedToP2PNetwork() {
		final String list = Util.sendHTTPRequest(this.restTemplate, this.p2pEndpoint + NegotiationService.LIST_ENDPOINT, "", HttpMethod.GET);
		final JsonArray jsonList = Util.gson.fromJson(list, JsonArray.class);
		return !jsonList.isEmpty();
	}

	private boolean hasNParticipants(int n) {
		final String list = Util.sendHTTPRequest(this.restTemplate, this.p2pEndpoint + NegotiationService.LIST_ENDPOINT, "", HttpMethod.GET);
		final JsonArray jsonList = Util.gson.fromJson(list, JsonArray.class);
		return jsonList.size() == n;
	}

	static class ReducedTSATriple implements Comparable<ReducedTSATriple> {
		private final Set<Identity> identities;
		private final Set<TSAEntry> tsaEntries;
		private final byte[] serializedHash;

		public ReducedTSATriple(@NonNull Set<Identity> identities, @NonNull Set<TSAEntry> tsaEntries) {
			this.identities = identities;
			this.tsaEntries = tsaEntries;
			this.serializedHash = Hashing.sha256().hashBytes(Objects.requireNonNull(SerializationUtils.serialize(Set.copyOf(this.identities)))).asBytes();
		}

		@Override
		public int compareTo(@NonNull ReducedTSATriple reducedTSATriple) {
			return SignedBytes.lexicographicalComparator().compare(this.serializedHash, reducedTSATriple.serializedHash);
		}

		@Override
		public String toString() {
			return this.identities.stream().map(Identity::getIdentityName).collect(Collectors.joining(", "));
		}
	}

	public RunningState getRunningState() {
		return this.runningState;
	}

	public void setRunningState(RunningState runningState) {
		this.runningState = runningState;
	}
}
