package de.dlr.ddomtia.dto;


import java.util.Arrays;
import java.util.Objects;

public record TSADocument(String timestamp, byte[] hashTSQ, byte[] hashTSR, String position) {

	@Override
	public String toString() {
		return "TSADocument{" + "timestamp='" + this.timestamp + '\'' + ", hashTSQ=" + Arrays.toString(this.hashTSQ) + ", hashTSR=" + Arrays.toString(this.hashTSR) + ", position='" + this.position + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final TSADocument that = (TSADocument) o;

		if (!Objects.equals(this.timestamp, that.timestamp)) {
			return false;
		}
		if (!Arrays.equals(this.hashTSQ, that.hashTSQ)) {
			return false;
		}
		if (!Arrays.equals(this.hashTSR, that.hashTSR)) {
			return false;
		}
		return this.position.equals(that.position);
	}

	@Override
	public int hashCode() {
		int result = this.timestamp != null ? this.timestamp.hashCode() : 0;
		result = 31 * result + Arrays.hashCode(this.hashTSQ);
		result = 31 * result + Arrays.hashCode(this.hashTSR);
		result = 31 * result + (this.position != null ? this.position.hashCode() : 0);
		return result;
	}
}
