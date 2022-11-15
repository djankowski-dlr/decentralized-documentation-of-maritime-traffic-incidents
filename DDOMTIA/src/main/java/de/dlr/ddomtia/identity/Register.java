package de.dlr.ddomtia.identity;

import java.util.Set;

public record Register(Set<TSAEntry> basis, Extended extended) {
}
