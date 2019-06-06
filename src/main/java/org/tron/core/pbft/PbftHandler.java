package org.tron.core.pbft;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.server.Channel;
import org.tron.common.overlay.server.MessageQueue;
import org.tron.core.exception.P2pException;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.pbft.message.PbftBaseMessage;

@Component
@Scope("prototype")
public class PbftHandler extends SimpleChannelInboundHandler<PbftBaseMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue;

  private static final Striped<Lock> striped = Striped.lazyWeakLock(1024);

  private static final Cache<String, Boolean> msgCache = CacheBuilder.newBuilder()
      .initialCapacity(3000).maximumSize(10000).expireAfterWrite(10, TimeUnit.MINUTES).build();

  @Autowired
  private PbftManager pbftManager;

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, PbftBaseMessage msg) throws Exception {
    String key = buildKey(msg);
    Lock lock = striped.get(key);
    try {
      lock.lock();
      if (msgCache.getIfPresent(key) != null) {
        return;
      }
      if (!msg.validateSignature() || !pbftManager.checkIsWitnessMsg(msg)) {
        throw new P2pException(P2pException.TypeEnum.BAD_MESSAGE, msg.toString());
      }
      msgCache.put(key, true);
      pbftManager.forwardMessage(msg);
      msgQueue.receivedMessage(msg);
      pbftManager.doAction(msg);
    } finally {
      lock.unlock();
    }

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

  private String buildKey(PbftBaseMessage msg) {
    return msg.getKey() + msg.getPbftMessage().getRawData()
        .getPbftMsgType().toString();
  }

}