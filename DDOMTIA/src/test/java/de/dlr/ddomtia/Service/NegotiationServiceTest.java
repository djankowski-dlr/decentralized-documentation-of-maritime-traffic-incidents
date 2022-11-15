package de.dlr.ddomtia.Service;

import java.util.*;

import com.google.common.collect.Sets;

import de.dlr.ddomtia.identity.Identity;
import de.dlr.ddomtia.identity.IdentityRegister;
import de.dlr.ddomtia.identity.TSAEntry;
import de.dlr.ddomtia.service.NegotiationService;
import de.dlr.ddomtia.util.RunningState;
import de.dlr.ddomtia.util.Util;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.*;

class NegotiationServiceTest {
	private final NegotiationService negotiationService;
	private final TSAEntry tsaEntry1;
	private final TSAEntry tsaEntry2;
	private final TSAEntry tsaEntry3;
	private final TSAEntry tsaEntry4;
	private final TSAEntry tsaEntry5;

	public NegotiationServiceTest() {
		this.negotiationService = new NegotiationService(null, new IdentityRegister("IdentityRegister.json"), "", null,null, null);
		this.tsaEntry1 = new TSAEntry("TSA1", "https://tsa1.de");
		this.tsaEntry2 = new TSAEntry("TSA2", "https://tsa2.de");
		this.tsaEntry3 = new TSAEntry("TSA3", "https://tsa3.de");
		this.tsaEntry4 = new TSAEntry("TSA4", "https://tsa4.de");
		this.tsaEntry5 = new TSAEntry("TSA5", "https://tsa5.de");
	}

	@Test
	void reducedTSADetermination() {
		Identity identity1 = new Identity("identity1", Map.of(0, Set.of(this.tsaEntry1), 1, Set.of(this.tsaEntry2, this.tsaEntry1), 2, Set.of(this.tsaEntry3, this.tsaEntry2, this.tsaEntry1)), 2);
		Identity identity2 = new Identity("identity2", Map.of(0, Set.of(this.tsaEntry1), 1, Set.of(this.tsaEntry2, this.tsaEntry1), 2, Set.of(this.tsaEntry3, this.tsaEntry2, this.tsaEntry1)), 2);
		Identity identity3 = new Identity("identity3", Map.of(0, Set.of(this.tsaEntry1), 1, Set.of(this.tsaEntry2, this.tsaEntry1), 2, Set.of(this.tsaEntry3, this.tsaEntry2, this.tsaEntry1)), 2);
		Identity identity4 = new Identity("identity4", Map.of(0, Set.of(this.tsaEntry1), 1, Set.of(this.tsaEntry2, this.tsaEntry1), 2, Set.of(this.tsaEntry3, this.tsaEntry2, this.tsaEntry1)), 2);
		Identity identity5 = new Identity("identity5", Map.of(0, Set.of(this.tsaEntry4)), 0);

		Set<Identity> identities = Set.of(identity1, identity2, identity3, identity4, identity5);
		Optional<Set<TSAEntry>> resultSetOptional;
		resultSetOptional = this.negotiationService.reducedTSADetermination(identities, identity1);
		assertEquals(Set.of(this.tsaEntry1), resultSetOptional.get());

		resultSetOptional = this.negotiationService.reducedTSADetermination(identities, identity5);
		assertFalse(resultSetOptional.isPresent());

		identity1 = new Identity("identity1", Map.of(0, Set.of(this.tsaEntry1), 1, Set.of(this.tsaEntry2, this.tsaEntry1), 2, Set.of(this.tsaEntry3, this.tsaEntry2, this.tsaEntry1)), 2);
		identity2 = new Identity("identity2", Map.of(0, Set.of(this.tsaEntry1), 1, Set.of(this.tsaEntry2, this.tsaEntry1), 2, Set.of(this.tsaEntry3, this.tsaEntry2, this.tsaEntry1)), 2);
		identity3 = new Identity("identity3", Map.of(0, Set.of(this.tsaEntry3)), 0);
		identity4 = new Identity("identity4", Map.of(0, Set.of(this.tsaEntry4), 1, Set.of(this.tsaEntry5, this.tsaEntry4)), 1);
		identity5 = new Identity("identity5", Map.of(0, Set.of(this.tsaEntry4), 1, Set.of(this.tsaEntry5, this.tsaEntry4)), 1);

		identities = Set.of(identity1, identity2, identity3, identity4, identity5);
		resultSetOptional = this.negotiationService.reducedTSADetermination(identities, identity3);
		assertEquals(Optional.of(Set.of(this.tsaEntry3)), resultSetOptional);

		resultSetOptional = this.negotiationService.reducedTSADetermination(identities, identity5);
		assertEquals(Optional.of(Set.of(this.tsaEntry4)), resultSetOptional);

		identity1 = new Identity("identity1", Map.of(0, Set.of(this.tsaEntry2), 1, Set.of(this.tsaEntry2, this.tsaEntry1)), 1);
		identity2 = new Identity("identity2", Map.of(0, Set.of(this.tsaEntry2), 1, Set.of(this.tsaEntry2, this.tsaEntry1)), 1);
		identity3 = new Identity("identity3", Map.of(0, Set.of(this.tsaEntry2), 1, Set.of(this.tsaEntry5, this.tsaEntry2)), 1);
		identity4 = new Identity("identity4", Map.of(0, Set.of(this.tsaEntry4), 1, Set.of(this.tsaEntry4, this.tsaEntry5)), 1);
		identity5 = new Identity("identity5", Map.of(0, Set.of(this.tsaEntry4), 1, Set.of(this.tsaEntry4, this.tsaEntry5)), 1);

		identities = Set.of(identity1, identity2, identity3, identity4, identity5);
		resultSetOptional = this.negotiationService.reducedTSADetermination(identities, identity3);
		final Optional<Set<TSAEntry>> resultSetOptional1 = this.negotiationService.reducedTSADetermination(identities, identity1);
		final Optional<Set<TSAEntry>> resultSetOptional5 = this.negotiationService.reducedTSADetermination(identities, identity5);
		final List<Set<TSAEntry>> res = List.of(resultSetOptional1.get(), resultSetOptional5.get());

		assertEquals(Set.of(this.tsaEntry2), resultSetOptional.get());
		assertTrue(Set.copyOf(Sets.intersection(Set.of(this.tsaEntry4, this.tsaEntry5), resultSetOptional.get())).isEmpty());
	}
}