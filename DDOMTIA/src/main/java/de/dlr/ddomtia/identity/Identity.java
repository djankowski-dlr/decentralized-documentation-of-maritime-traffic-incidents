package de.dlr.ddomtia.identity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.Getter;

@Getter
public final class Identity implements Serializable {
	@Serial
	private static final long serialVersionUID = -5000741622340654511L;
	private final String identityName;
	private final transient Register registerList;
	private final Map<Integer, Set<TSAEntry>> tsaIterationMap;
	private final int maxIteration;

	public Identity(String identityName, Register registerList, Map<Integer, Set<TSAEntry>> tsaIterationMap, int maxIteration) {
		this.identityName = identityName;
		this.registerList = registerList;
		this.tsaIterationMap = tsaIterationMap;
		this.maxIteration = maxIteration;
	}

	public Identity(String identityName, Map<Integer, Set<TSAEntry>> tsaIterationMap, int maxIteration) {
		this.identityName = identityName;
		this.tsaIterationMap = tsaIterationMap;
		this.registerList = null;
		this.maxIteration = maxIteration;
	}

	public static Identity createPlainIdentity(String identity, int maxIteration) {
		final Extended extended = new Extended(new HashSet<>(), new HashSet<>());
		final Register register = new Register(new HashSet<>(), extended);
		return new Identity(identity, register, new HashMap<>(), maxIteration);
	}

	public static Set<TSAEntry> determineTSASet(Identity identity, int iteration) {
		if (iteration == 0) {
			return new HashSet<>(identity.getRegisterList().basis());
		}
		if (iteration > 0) {
			final Set<TSAEntry> result = new HashSet<>(identity.getRegisterList().basis());
			result.addAll(identity.getRegisterList().extended().tsaEntries());
			final Set<Identity> identitySet = identity.getRegisterList().extended().identities();
			identitySet.forEach(ident -> result.addAll(determineTSASet(ident, iteration - 1)));
			return result;
		} else {
			throw new RuntimeException("Iteration can not be smaller then 0");
		}
	}

	public static Set<TSAEntry> findTSAs(Collection<Identity> identities, int iteration) {
		final Identity ident = identities.stream().findAny().orElse(null);
		if (ident == null) {
			return Collections.emptySet();
		}
		final Set<TSAEntry> resultSet = new HashSet<>(ident.getTSAsOfIteration(iteration));
		for (final Identity identity : identities) {
			resultSet.retainAll(identity.getTSAsOfIteration(iteration));
		}
		return resultSet;
	}

	public Set<TSAEntry> getTSAsOfIteration(int iteration) {
		return this.tsaIterationMap.get(Math.min(iteration, this.maxIteration));
	}

	public void addToIterationMap(int iteration, Set<TSAEntry> tsaEntries) {
		if (this.tsaIterationMap.containsKey(iteration)) {
			this.tsaIterationMap.get(iteration).addAll(tsaEntries);
			return;
		}
		this.tsaIterationMap.put(iteration, tsaEntries);
	}

	public void addToIterationMap() {
		for (int i = 0; i <= this.maxIteration; i++) {
			if (this.tsaIterationMap.containsKey(i)) {
				this.tsaIterationMap.get(i).addAll(Identity.determineTSASet(this, i));
				continue;
			}
			this.tsaIterationMap.put(i, Identity.determineTSASet(this, i));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final Identity identity1 = (Identity) o;

		return this.identityName.equals(identity1.identityName);
	}

	@Override
	public int hashCode() {
		return this.identityName.hashCode();
	}

}
