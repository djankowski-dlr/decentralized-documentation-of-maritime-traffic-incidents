package de.dlr.p2p.connection;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.concurrent.EventExecutorGroup;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class PeerChannelInitializer extends ChannelInitializer<SocketChannel> {
	private final ObjectEncoder encoder;
	private final EventExecutorGroup peerChannelHandlerExecutorGroup;
	private final PeerChannelHandler peerChannelHandler;

	public PeerChannelInitializer(ObjectEncoder encoder, EventExecutorGroup peerChannelHandlerExecutorGroup, PeerChannelHandler peerChannelHandler) {
		this.encoder = encoder;
		this.peerChannelHandlerExecutorGroup = peerChannelHandlerExecutorGroup;
		this.peerChannelHandler = peerChannelHandler;
	}

	@Override
	protected void initChannel(SocketChannel socketChannel) {
		final ChannelPipeline pipeline = socketChannel.pipeline();
		pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
		pipeline.addLast(this.encoder);
		pipeline.addLast(this.peerChannelHandlerExecutorGroup, this.peerChannelHandler);
	}
}
