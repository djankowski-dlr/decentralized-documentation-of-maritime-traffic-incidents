package de.dlr.ddomtia.controller;

import de.dlr.ddomtia.identity.IdentityRegister;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/dlr")
public final class IdentityController {
	private final IdentityRegister identityRegister;

	@Autowired
	public IdentityController(IdentityRegister identityRegister) {
		this.identityRegister = identityRegister;
	}

	@PostMapping(value = "/identity")
	public ResponseEntity<String> tsa(@RequestParam("identity") String identityString) {
		this.identityRegister.reloadAndStoreIdentity(identityString);
		return ResponseEntity.ok("Own identity has been updated.");
	}

}
