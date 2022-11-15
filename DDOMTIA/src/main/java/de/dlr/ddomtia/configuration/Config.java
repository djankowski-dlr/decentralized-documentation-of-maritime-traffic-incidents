package de.dlr.ddomtia.configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.dlr.ddomtia.Message;

import lombok.extern.log4j.Log4j2;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Configuration
@ComponentScan(basePackages = {"de.dlr.ddomtia"})
public class Config {

	public Config() {
		log.info("Config has been loaded.");
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplateBuilder().setConnectTimeout(Duration.ofMillis(5000)).setReadTimeout(Duration.ofMillis(5000)).build();
	}
}
