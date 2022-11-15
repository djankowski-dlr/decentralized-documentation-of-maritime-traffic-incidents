package de.dlr.ddomtia.event;

import de.dlr.ddomtia.identity.TSAEntry;

import lombok.Getter;

import org.springframework.context.ApplicationEvent;

public final class TSAEvent extends ApplicationEvent {
	@Getter
	private final TSAEntry tsaEntry;

	public TSAEvent(Object source, TSAEntry tsaEntry) {
		super(source);
		this.tsaEntry = tsaEntry;
	}
}
