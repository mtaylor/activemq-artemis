/**
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
package org.apache.activemq.artemis.core.protocol.mqtt;

import io.netty.channel.ChannelPipeline;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyServerConnection;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.management.Notification;
import org.apache.activemq.artemis.core.server.management.NotificationListener;
import org.apache.activemq.artemis.spi.core.protocol.ConnectionEntry;
import org.apache.activemq.artemis.spi.core.protocol.MessageConverter;
import org.apache.activemq.artemis.spi.core.protocol.ProtocolManager;
import org.apache.activemq.artemis.spi.core.protocol.ProtocolManagerFactory;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.remoting.Acceptor;
import org.apache.activemq.artemis.spi.core.remoting.Connection;

import java.util.List;

/**
 * MQTTProtocolManager
 */
class MQTTProtocolManager implements ProtocolManager, NotificationListener
{
   private ActiveMQServer server;

   public MQTTProtocolManager(ActiveMQServer server)
   {
      this.server = server;
   }

   @Override
   public void onNotification(Notification notification)
   {
      System.out.print("Notification Received");
   }


   @Override
   public ProtocolManagerFactory getFactory()
   {
      return null;
   }

   @Override
   public void updateInterceptors(List incomingInterceptors, List outgoingInterceptors)
   {

   }

   @Override
   public ConnectionEntry createConnectionEntry(Acceptor acceptorUsed, Connection connection)
   {
      try
      {
         MQTTConnection conn = new MQTTConnection(connection, server);
         return new ConnectionEntry(conn, null, System.currentTimeMillis(), 1 * 60 * 1000);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         return null;
      }
   }

   @Override
   public void removeHandler(String name)
   {
      System.out.print("Handler Removed");
   }

   @Override
   public void handleBuffer(RemotingConnection connection, ActiveMQBuffer buffer)
   {
      connection.bufferReceived(connection.getID(), buffer);
   }

   @Override
   public void addChannelHandlers(ChannelPipeline pipeline)
   {
   }

   @Override
   public boolean isProtocol(byte[] array)
   {
      return array[4] == 77 && // M
             array[5] == 81 && // Q
             array[6] == 84 && // T
             array[7] == 84;   // T
   }

   @Override
   public MessageConverter getConverter()
   {
      return null;
   }

   @Override
   public void handshake(NettyServerConnection connection, ActiveMQBuffer buffer)
   {
   }
}
