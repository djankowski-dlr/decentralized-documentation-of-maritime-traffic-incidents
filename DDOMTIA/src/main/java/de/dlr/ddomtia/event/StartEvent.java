package de.dlr.ddomtia.event;

import org.springframework.context.ApplicationEvent;

public final class StartEvent extends ApplicationEvent {

	public StartEvent(Object source) {
		super(source);
	}
}
