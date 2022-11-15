package de.dlr.ddomtia.controller;

import de.dlr.ddomtia.event.StartEvent;
import de.dlr.ddomtia.service.NegotiationService;
import de.dlr.ddomtia.util.RunningState;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/dlr")

public final class EventStarterController {
	private final NegotiationService negotiationService;
	private final ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	public EventStarterController(NegotiationService negotiationService, ApplicationEventPublisher applicationEventPublisher) {
		this.negotiationService = negotiationService;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@GetMapping(value = "/start")
	public ResponseEntity<Void> start() {
		if (this.negotiationService.getRunningState() == RunningState.NOT_RUNNING) {
			this.negotiationService.setRunningState(RunningState.RUNNING);
			this.applicationEventPublisher.publishEvent(new StartEvent(this));
		}
		return ResponseEntity.ok().build();
	}
}
