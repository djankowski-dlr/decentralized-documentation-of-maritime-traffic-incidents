package de.dlr.starter.configuration;

import lombok.extern.log4j.Log4j2;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;


@Log4j2
@Configuration
@ComponentScan(basePackages = {"de.dlr"})
@EnableScheduling

public class Config {

	@Bean
	public AsyncHttpClient httpClient() {
		final DefaultAsyncHttpClientConfig.Builder clientBuilder = Dsl.config().setConnectTimeout(3000);
		return Dsl.asyncHttpClient(clientBuilder);
	}
}
