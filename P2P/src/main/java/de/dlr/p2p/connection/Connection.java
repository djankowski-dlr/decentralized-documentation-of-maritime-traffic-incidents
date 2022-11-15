package de.dlr.p2p.connection;

import java.net.InetSocketAddress;

import de.dlr.p2p.connection.message.Message;

import io.netty.channel.ChannelHandlerContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public final class Connection {
	private final InetSocketAddress remoteAddress;
	private ChannelHandlerContext ctx;
	@Setter
	private String peerName;
	private final String host;
	@Setter
	private int port;

	public Connection(ChannelHandlerContext ctx) {
		this.remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
		this.ctx = ctx;
		this.host = this.remoteAddress.getAddress().getHostAddress();
	}

	public void send(Message msg) {
		if (this.ctx != null) {
			this.ctx.writeAndFlush(msg);
		} else {
			log.error("Can not send message {} to {}", msg.getClass(), toString());
		}
	}

	public void close() {
		log.debug("Closing session of {}", this.toString());
		if (this.ctx != null) {
			this.ctx.close();
			this.ctx = null;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final Connection that = (Connection) o;

		return this.peerName.equals(that.peerName);
	}

	@Override
	public int hashCode() {
		return this.peerName != null ? this.peerName.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "Connection{" + "remoteAddress=" + this.remoteAddress + ", ctx=" + this.ctx + ", peerName='" + this.peerName + '\'' + '}';
	}
}
