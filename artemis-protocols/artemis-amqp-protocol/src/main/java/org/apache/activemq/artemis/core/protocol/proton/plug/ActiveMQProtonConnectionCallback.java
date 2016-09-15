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
package org.apache.activemq.artemis.core.protocol.proton.plug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.core.buffers.impl.ChannelBufferWrapper;
import org.apache.activemq.artemis.core.protocol.proton.ActiveMQProtonRemotingConnection;
import org.apache.activemq.artemis.core.protocol.proton.ProtonProtocolManager;
import org.apache.activemq.artemis.core.protocol.proton.sasl.ActiveMQPlainSASL;
import org.apache.activemq.artemis.core.remoting.CloseListener;
import org.apache.activemq.artemis.core.remoting.FailureListener;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.spi.core.protocol.SessionCallback;
import org.apache.activemq.artemis.spi.core.remoting.Connection;
import org.apache.activemq.artemis.utils.ReusableLatch;
import org.apache.activemq.artemis.utils.UUIDGenerator;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.jboss.logging.Logger;
import org.proton.plug.AMQPConnectionCallback;
import org.proton.plug.AMQPConnectionContext;
import org.proton.plug.AMQPSessionCallback;
import org.proton.plug.SASLResult;
import org.proton.plug.ServerSASL;
import org.proton.plug.handler.ExtCapability;
import org.proton.plug.sasl.AnonymousServerSASL;
import org.proton.plug.sasl.PlainSASLResult;

import static org.proton.plug.AmqpSupport.CONTAINER_ID;
import static org.proton.plug.AmqpSupport.INVALID_FIELD;
import static org.proton.plug.context.AbstractConnectionContext.CONNECTION_OPEN_FAILED;

public class ActiveMQProtonConnectionCallback implements AMQPConnectionCallback, FailureListener, CloseListener {
   private static final List<String> connectedContainers = Collections.synchronizedList(new ArrayList());

   private static final Logger log = Logger.getLogger(ActiveMQProtonConnectionCallback.class);

   private final ProtonProtocolManager manager;

   private final Connection connection;

   protected ActiveMQProtonRemotingConnection protonConnectionDelegate;

   protected AMQPConnectionContext amqpConnection;

   private final ReusableLatch latch = new ReusableLatch(0);

   private final Executor closeExecutor;

   private String remoteContainerId;

   private boolean connected;

   private final Object connectionLock = new Object();

   public ActiveMQProtonConnectionCallback(ProtonProtocolManager manager,
                                           Connection connection,
                                           Executor closeExecutor) {
      this.manager = manager;
      this.connection = connection;
      this.closeExecutor = closeExecutor;
   }

   @Override
   public ServerSASL[] getSASLMechnisms() {

      ServerSASL[] result;

      if (isSupportsAnonymous()) {
         result = new ServerSASL[]{new ActiveMQPlainSASL(manager.getServer().getSecurityStore()), new AnonymousServerSASL()};
      }
      else {
         result = new ServerSASL[]{new ActiveMQPlainSASL(manager.getServer().getSecurityStore())};
      }

      return result;
   }

   public boolean isSupportsAnonymous() {
      boolean supportsAnonymous = false;
      try {
         manager.getServer().getSecurityStore().authenticate(null, null, null);
         supportsAnonymous = true;
      }
      catch (Exception e) {
         // authentication failed so no anonymous support
      }
      return supportsAnonymous;
   }

   @Override
   public void close() {
      synchronized (connectionLock) {
         try {
            if (remoteContainerId != null && connected) {
               connectedContainers.remove(remoteContainerId);
               connected = false;
            }
         }
         catch (Exception e) {
            log.error("error closing internal session", e);
         }
      }
      connection.close();
      amqpConnection.close();
   }

   public Executor getExeuctor() {
      if (protonConnectionDelegate != null) {
         return protonConnectionDelegate.getExecutor();
      }
      else {
         return null;
      }
   }

   @Override
   public void setConnection(AMQPConnectionContext connection) {
      this.amqpConnection = connection;
   }

   @Override
   public AMQPConnectionContext getConnection() {
      return amqpConnection;
   }

   public ActiveMQProtonRemotingConnection getProtonConnectionDelegate() {
      return protonConnectionDelegate;
   }

   public void setProtonConnectionDelegate(ActiveMQProtonRemotingConnection protonConnectionDelegate) {

      this.protonConnectionDelegate = protonConnectionDelegate;
   }

   @Override
   public void onTransport(ByteBuf byteBuf, AMQPConnectionContext amqpConnection) {
      final int size = byteBuf.writerIndex();

      latch.countUp();
      connection.write(new ChannelBufferWrapper(byteBuf, true), false, false, new ChannelFutureListener() {
         @Override
         public void operationComplete(ChannelFuture future) throws Exception {
            latch.countDown();
         }
      });

      if (amqpConnection.isSyncOnFlush()) {
         try {
            latch.await(5, TimeUnit.SECONDS);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }

      amqpConnection.outputDone(size);
   }

   @Override
   public AMQPSessionCallback createSessionCallback(AMQPConnectionContext connection) {
      return new ProtonSessionIntegrationCallback(this, manager, connection, this.connection, closeExecutor);
   }

   @Override
   public void sendSASLSupported() {
      connection.write(ActiveMQBuffers.wrappedBuffer(new byte[]{'A', 'M', 'Q', 'P', 3, 1, 0, 0}));
   }

   @Override
   public boolean validateConnection(org.apache.qpid.proton.engine.Connection connection, SASLResult saslResult) {
      synchronized (connectionLock) {
         remoteContainerId = connection.getRemoteContainer();
         if (ExtCapability.needUniqueConnection(connection)) {
            if (connectedContainers.contains(remoteContainerId)) {
               //https://issues.apache.org/jira/browse/ARTEMIS-728
               Map<Symbol, Object> connProp = new HashMap<>();
               connProp.put(CONNECTION_OPEN_FAILED, "true");
               connection.setProperties(connProp);
               connection.getCondition().setCondition(AmqpError.INVALID_FIELD);
               Map<Symbol, Symbol> info = new HashMap<>();
               info.put(INVALID_FIELD, CONTAINER_ID);
               connection.getCondition().setInfo(info);
               return false;
            }
         }
         connectedContainers.add(remoteContainerId);
         connected = true;
         return true;
      }
   }

   @Override
   public void connectionClosed() {
      close();
   }

   @Override
   public void connectionFailed(ActiveMQException exception, boolean failedOver) {
      close();
   }

   @Override
   public void connectionFailed(ActiveMQException exception, boolean failedOver, String scaleDownTargetNodeID) {
      close();
   }
}
