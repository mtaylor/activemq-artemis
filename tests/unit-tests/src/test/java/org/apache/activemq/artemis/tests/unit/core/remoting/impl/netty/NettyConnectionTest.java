/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.unit.core.remoting.impl.netty;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnection;
import org.apache.activemq.artemis.core.server.ActiveMQComponent;
import org.apache.activemq.artemis.spi.core.remoting.Connection;
import org.apache.activemq.artemis.spi.core.remoting.ConnectionLifeCycleListener;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.junit.Assert;
import org.junit.Test;

public class NettyConnectionTest extends ActiveMQTestBase {

   private static final Map<String, Object> emptyMap = Collections.emptyMap();

   @Test
   public void testGetID() throws Exception {
      Channel channel = createChannel();
      NettyConnection conn = new NettyConnection(emptyMap, channel, new MyListener(), false, false);

      Assert.assertEquals(channel.hashCode(), conn.getID());
   }

   @Test
   public void testWrite() throws Exception {
      ActiveMQBuffer buff = ActiveMQBuffers.wrappedBuffer(ByteBuffer.allocate(128));
      EmbeddedChannel channel = createChannel();

      Assert.assertEquals(0, channel.outboundMessages().size());

      NettyConnection conn = new NettyConnection(emptyMap, channel, new MyListener(), false, false);
      conn.write(buff);
      channel.runPendingTasks();
      Assert.assertEquals(1, channel.outboundMessages().size());
   }

   @Test
   public void testCreateBuffer() throws Exception {
      EmbeddedChannel channel = createChannel();
      NettyConnection conn = new NettyConnection(emptyMap, channel, new MyListener(), false, false);

      final int size = 1234;

      ActiveMQBuffer buff = conn.createTransportBuffer(size);
      buff.writeByte((byte) 0x00); // Netty buffer does lazy initialization.
      Assert.assertEquals(size, buff.capacity());

   }

   private static EmbeddedChannel createChannel() {
      return new EmbeddedChannel(new ChannelInboundHandlerAdapter());
   }

   class MyListener implements ConnectionLifeCycleListener {

      @Override
      public void connectionCreated(final ActiveMQComponent component,
                                    final Connection connection,
                                    final String protocol) {

      }

      @Override
      public void connectionDestroyed(final Object connectionID) {

      }

      @Override
      public void connectionException(final Object connectionID, final ActiveMQException me) {

      }

      @Override
      public void connectionReadyForWrites(Object connectionID, boolean ready) {
      }

   }

//   @Test
//   public void testOrderedWritesWhileBatching() throws Exception {
//      final int msgSize = Long.BYTES;
//      final ActiveMQBuffer firstMessage = ActiveMQBuffers.wrappedBuffer(ByteBuffer.allocateDirect(msgSize));
//      firstMessage.writeLong(1L);
//      final ActiveMQBuffer secondMessage = ActiveMQBuffers.wrappedBuffer(ByteBuffer.allocateDirect(msgSize));
//      secondMessage.writeLong(2L);
//      final EmbeddedChannel channel = createChannel();
//      Assert.assertTrue(channel.config().getWriteBufferHighWaterMark() >= (msgSize * 2));
//
//      final NettyConnection connection = new NettyConnection(emptyMap, channel, new MyListener(), true, false);
//
//      connection.write(firstMessage, false, true);
//      connection.getChannel().eventLoop().submit(() -> {
//         connection.write(secondMessage, false, true);
//      });
//      channel.flushOutbound();
//      final ByteBuf expectedFirst = channel.readOutbound();
//      Assert.assertEquals(1L, expectedFirst.readLong());
//      final ByteBuf expectedSecond = channel.readOutbound();
//      Assert.assertEquals(2L, expectedSecond.readLong());
//   }

   @Test
   public void testShouldFlushOverWriteBatchSize() throws Exception {
      final EmbeddedChannel channel = createChannel();
      final NettyConnection connection = new NettyConnection(emptyMap, channel, new MyListener(), true, false);

      final ActiveMQBuffer firstMessage = ActiveMQBuffers.wrappedBuffer(ByteBuffer.allocateDirect(connection.writeBatchSize() - 1));
      firstMessage.setByte(0, (byte) 1);
      firstMessage.writerIndex(firstMessage.capacity());
      final ActiveMQBuffer secondMessage = ActiveMQBuffers.wrappedBuffer(ByteBuffer.allocateDirect(1));
      secondMessage.setByte(0, (byte) 2);
      secondMessage.writerIndex(secondMessage.capacity());

      Assert.assertTrue(channel.isWritable());
      connection.write(firstMessage, false, true);
      Assert.assertTrue(channel.isWritable());
      Assert.assertEquals(0, channel.outboundMessages().size());
      connection.write(secondMessage, false, true);
      Assert.assertTrue(channel.isWritable());
      Assert.assertEquals(2, channel.outboundMessages().size());
   }
}
