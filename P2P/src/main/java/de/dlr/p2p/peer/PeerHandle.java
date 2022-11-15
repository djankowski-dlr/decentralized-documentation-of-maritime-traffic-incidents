package de.dlr.p2p.peer;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.SettableFuture;

import de.dlr.p2p.config.P2PConfig;
import de.dlr.p2p.connection.Connection;
import de.dlr.p2p.connection.PeerChannelHandler;
import de.dlr.p2p.connection.PeerChannelInitializer;
import de.dlr.p2p.connection.message.BroadcastMessage;
import de.dlr.p2p.connection.message.JsonMessage;
import de.dlr.p2p.service.ConnectionService;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Log4j2
@Component
public final class PeerHandle {
	private final P2PConfig config;
	private final int portToBind;
	private final EventLoopGroup acceptorEventLoopGroup;
	private final EventLoopGroup networkEventLoopGroup;
	private final EventLoopGroup peerEventLoopGroup;
	private Future keepAliveFuture;
	private Future timeoutPingsFuture;
	private final ObjectEncoder encoder;
	private final Peer peer;
	private final Random random;

	@Autowired
	public PeerHandle(P2PConfig config,
					  @Value("${p2p.port}") int portToBind,
					  EventLoopGroup acceptorEventLoopGroup,
					  EventLoopGroup networkEventLoopGroup,
					  EventLoopGroup peerEventLoopGroup,
					  ObjectEncoder objectEncoder,
					  Peer peer) {

		this.config = config;
		this.portToBind = portToBind;
		this.acceptorEventLoopGroup = acceptorEventLoopGroup;
		this.networkEventLoopGroup = networkEventLoopGroup;
		this.peerEventLoopGroup = peerEventLoopGroup;
		this.peer = peer;
		this.encoder = objectEncoder;
		this.random = new Random();
	}

	@PostConstruct
	public void init() {
		try {
			this.start();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void start() throws InterruptedException {
		final PeerChannelHandler peerChannelHandler = new PeerChannelHandler(this.config, this.peer);
		final PeerChannelInitializer peerChannelInitializer = new PeerChannelInitializer(this.encoder, this.peerEventLoopGroup, peerChannelHandler);
		final ServerBootstrap peerBootstrap = new ServerBootstrap();
		peerBootstrap.group(this.acceptorEventLoopGroup, this.networkEventLoopGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.SO_BACKLOG, 100)
				.handler(new LoggingHandler(LogLevel.DEBUG))
				.childHandler(peerChannelInitializer);

		final ChannelFuture bindFuture = peerBootstrap.bind(this.portToBind).sync();

		if (bindFuture.isSuccess()) {
			log.info("{} Successfully bind to {}", this.config.getPeerName(), this.portToBind);
			final Channel serverChannel = bindFuture.channel();

			final SettableFuture<Void> setServerChannelFuture = SettableFuture.create();
			this.peerEventLoopGroup.execute(() -> {
				try {
					this.peer.setBindChannel(serverChannel);
					setServerChannelFuture.set(null);
				} catch (Exception e) {
					setServerChannelFuture.setException(e);
				}
			});

			try {
				setServerChannelFuture.get(10, TimeUnit.SECONDS);
			} catch (Exception e) {
				log.error("Couldn't set bind channel to server " + this.config.getPeerName(), e);
				System.exit(-1);
			}

			final int initialDelay = this.random.nextInt(P2PConfig.DEFAULT_KEEP_ALIVE_SECONDS);

			this.keepAliveFuture = this.peerEventLoopGroup.scheduleAtFixedRate(this.peer::keepAlivePing, initialDelay, P2PConfig.DEFAULT_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS);

//			this.timeoutPingsFuture = this.peerEventLoopGroup.scheduleAtFixedRate(this.peer::timeoutPings, 0, 5, TimeUnit.MILLISECONDS);

		} else {
			log.error(this.config.getPeerName() + " could not bind to " + this.portToBind, bindFuture.cause());
			System.exit(-1);
		}

	}
	public void leave() {
		final CompletableFuture<Void> future = new CompletableFuture<>();
		this.peerEventLoopGroup.execute(() -> this.peer.leave(future));
		if (this.keepAliveFuture != null && this.timeoutPingsFuture != null) {
			this.keepAliveFuture.cancel(false);
			this.timeoutPingsFuture.cancel(false);
			this.keepAliveFuture = null;
			this.timeoutPingsFuture = null;
		}
	}

	public void connect(final String host, final int port) {
		final CompletableFuture<Void> connectToHostFuture = new CompletableFuture<>();
		this.peerEventLoopGroup.execute(() -> this.peer.connectTo(host, port, connectToHostFuture));
	}

	public void send(String peerName, String message){
		final JsonMessage hello = new JsonMessage(message);
		this.peerEventLoopGroup.execute(()-> this.peer.sendMsg(peerName, hello));
		log.debug("{} Successfully send message {}", this.config.getPeerName(), message);
	}

	public void disconnect(final String peerName) {
		this.peerEventLoopGroup.execute(() -> this.peer.disconnect(peerName));
	}

	public void broadcast(String message) {
		final BroadcastMessage broadcastMessage = new BroadcastMessage(message);
		this.peerEventLoopGroup.execute(()-> this.peer.broadcastMsg(broadcastMessage));
		log.debug("{} Successfully broadcast message.", this.config.getPeerName());
	}

	public Collection<Connection> connections() {
		return this.peer.connections();
	}
}
