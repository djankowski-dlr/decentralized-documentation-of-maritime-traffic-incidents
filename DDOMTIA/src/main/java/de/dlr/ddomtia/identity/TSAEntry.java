package de.dlr.ddomtia.identity;

import java.io.Serial;
import java.io.Serializable;

public record TSAEntry(String name, String address) implements Serializable {
	@Serial
	private static final long serialVersionUID = -15462867688723L;

	@Override
	public String toString() {
		return "TSAEntry{" + "name='" + this.name + '\'' + ", address='" + this.address + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final TSAEntry tsaEntry = (TSAEntry) o;
		return this.name.equals(tsaEntry.name()) && this.address.equals(tsaEntry.address);
	}
}
