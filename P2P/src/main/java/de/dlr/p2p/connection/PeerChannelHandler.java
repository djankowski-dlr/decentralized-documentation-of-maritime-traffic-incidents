package de.dlr.p2p.connection;

import de.dlr.p2p.config.P2PConfig;
import de.dlr.p2p.connection.message.Handshake;
import de.dlr.p2p.connection.message.Message;
import de.dlr.p2p.peer.Peer;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Sharable
public final class PeerChannelHandler extends SimpleChannelInboundHandler<Message> {
	private static final String SESSION_ATTRIBUTE_KEY = "session";
	private final P2PConfig config;
	private final Peer peer;

	public PeerChannelHandler(P2PConfig config, Peer peer) {
		this.config = config;
		this.peer = peer;
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) {
		log.debug("Channel active {}.", ctx.channel().remoteAddress());
		final Connection connection = new Connection(ctx);
		PeerChannelHandler.getSessionAttribute(ctx).set(connection);
		ctx.writeAndFlush(new Handshake(this.config.getPeerName(), this.config.getPort()));
	}

	@Override
	public void channelInactive(final ChannelHandlerContext ctx) {
		log.debug("Channel inactive {}.", ctx.channel().remoteAddress());
		final Connection connection = PeerChannelHandler.getSessionAttribute(ctx).get();
		this.peer.handleConnectionClosed(connection);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
		log.debug("Message {} received from {}.", msg.getClass(), ctx.channel().remoteAddress());
		final Connection connection = getSessionAttribute(ctx).get();
		msg.handle(this.peer, connection);
	}

	@Override
	public void channelReadComplete(final ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
		log.error("Channel failure " + ctx.channel().remoteAddress(), cause);
		ctx.close();
		this.peer.handleConnectionClosed(getSessionAttribute(ctx).get());
	}

	public static synchronized Attribute<Connection> getSessionAttribute(ChannelHandlerContext ctx) {
		return ctx.channel().attr(AttributeKey.valueOf(SESSION_ATTRIBUTE_KEY));
	}

	@Override
	public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
		if (evt instanceof final IdleStateEvent idleStateEvent) {
			if (idleStateEvent.state() == IdleState.READER_IDLE) {
				log.warn("Channel idle {}", ctx.channel().remoteAddress());
				ctx.close();
			}
		}
	}

}
