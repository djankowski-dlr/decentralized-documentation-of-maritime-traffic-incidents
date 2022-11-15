package de.dlr.ddomtia;

import java.util.Set;

import de.dlr.ddomtia.identity.TSAEntry;

public record Message(String identity, int iteration, int maxIteration, Set<TSAEntry> entries) {
}
