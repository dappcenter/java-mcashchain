package io.midasprotocol.common.overlay.message;

import io.midasprotocol.common.overlay.server.Channel;
import io.midasprotocol.core.exception.P2pException;
import io.midasprotocol.core.net.message.MessageTypes;
import io.midasprotocol.core.net.message.TronMessageFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class MessageCodec extends ByteToMessageDecoder {

	private Channel channel;
	private P2pMessageFactory p2pMessageFactory = new P2pMessageFactory();
	private TronMessageFactory tronMessageFactory = new TronMessageFactory();

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
		throws Exception {
		int length = buffer.readableBytes();
		byte[] encoded = new byte[length];
		buffer.readBytes(encoded);
		try {
			Message msg = createMessage(encoded);
			channel.getNodeStatistics().tcpFlow.add(length);
			out.add(msg);
		} catch (Exception e) {
			channel.processException(e);
		}
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	private Message createMessage(byte[] encoded) throws Exception {
		byte type = encoded[0];
		if (MessageTypes.inP2pRange(type)) {
			return p2pMessageFactory.create(encoded);
		}
		if (MessageTypes.inTronRange(type)) {
			return tronMessageFactory.create(encoded);
		}
		throw new P2pException(P2pException.TypeEnum.NO_SUCH_MESSAGE, "type=" + encoded[0]);
	}

}