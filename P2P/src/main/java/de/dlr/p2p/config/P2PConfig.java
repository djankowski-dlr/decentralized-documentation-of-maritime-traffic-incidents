package de.dlr.p2p.config;

import java.util.concurrent.Executors;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.serialization.ObjectEncoder;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Getter
@EnableAsync
@Configuration
@ComponentScan(basePackages = {"de.dlr.p2p"})
public class P2PConfig {
	public static final int DEFAULT_MIN_NUMBER_OF_ACTIVE_CONNECTIONS = 5;
	public static final int DEFAULT_MAX_READ_IDLE_SECONDS = 120;
	public static final int DEFAULT_KEEP_ALIVE_SECONDS = 15;
	public static final int DEFAULT_PING_TIMEOUT_SECONDS = 5;
	public static final int DEFAULT_AUTO_DISCOVERY_PING_FREQUENCY = 10;
	public static final int DEFAULT_PING_TTL = 7;
	private final String peerName;
	private final int port;

	public P2PConfig(@Value("${p2p.name}") String peerName, @Value("${p2p.port}") int port) {
		this.peerName = peerName;
		this.port = port;
	}

	@Bean(name = "acceptorEventLoopGroup")
	public EventLoopGroup acceptorEventLoopGroup() {
		return new NioEventLoopGroup(1);
	}

	@Bean(name = "networkEventLoopGroup")
	public EventLoopGroup networkEventLoopGroup() {
		return new NioEventLoopGroup(5);
	}

	@Bean(name = "peerEventLoopGroup")
	public EventLoopGroup peerEventLoopGroup() {
		return new NioEventLoopGroup(1);
	}

	@Bean()
	public ObjectEncoder objectEncoder() {
		return new ObjectEncoder();
	}

	@Bean
	protected ConcurrentTaskExecutor getTaskExecutor() {
		return new ConcurrentTaskExecutor(Executors.newFixedThreadPool(2));
	}

	@Bean
	protected WebMvcConfigurer webMvcConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
				configurer.setTaskExecutor(getTaskExecutor());
			}
		};
	}
}
