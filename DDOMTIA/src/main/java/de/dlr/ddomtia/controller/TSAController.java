package de.dlr.ddomtia.controller;

import java.io.IOException;

import de.dlr.ddomtia.service.TSAService;

import lombok.extern.log4j.Log4j2;

import org.springframework.core.io.InputStreamSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping(value = "/dlr/tsa")
public final class TSAController {
	private final TSAService tsaService;

	public TSAController(TSAService tsaService) {
		this.tsaService = tsaService;
	}

	@PostMapping(value = "/document")
	public ResponseEntity<String> processTSARequest(@RequestParam("file") InputStreamSource file, @RequestParam("name") String name) throws IOException {
		return this.tsaService.document(file, name);
	}

	@PostMapping(value = "/validate")
	public ResponseEntity<String> validate(@RequestParam("tsq") InputStreamSource tsq, @RequestParam("tsr") InputStreamSource tsr) {
		return this.tsaService.validate(tsq, tsr);
	}
}
