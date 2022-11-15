package de.dlr.ddomtia.identity;

import java.util.Set;

public record Extended(Set<TSAEntry> tsaEntries, Set<Identity> identities) {
}
