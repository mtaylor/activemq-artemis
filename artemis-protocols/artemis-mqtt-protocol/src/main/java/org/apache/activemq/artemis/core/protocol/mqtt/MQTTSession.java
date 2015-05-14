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

import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.apache.activemq.artemis.core.server.*;
import org.apache.activemq.artemis.core.server.impl.ServerSessionImpl;
import org.apache.activemq.artemis.spi.core.protocol.SessionCallback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MQTTSession
{
   static final Map<String, MQTTSessionState> SESSIONS = new ConcurrentHashMap<>();

   private MQTTConnection connection;

   private MQTTProtocolHandler protocolHandler;

   private MQTTSubscriptionManager subscriptionManager;

   private MQTTSessionCallback sessionCallback;

   private ServerSessionImpl serverSession;

   private MQTTQoSManager mqttQoSManager;

   private MQTTConnectionManager mqttConnectionManager;

   private MQTTRetainMessageManager retainMessageManager;

   protected MQTTSessionState state;

   private boolean stopped = false;

   public MQTTSession(MQTTConnection connection) throws Exception
   {
      this.connection = connection;

      protocolHandler = new MQTTProtocolHandler(this);
      mqttConnectionManager = new MQTTConnectionManager(this);
      mqttQoSManager = new MQTTQoSManager(this);
      sessionCallback = new MQTTSessionCallback(this);
      subscriptionManager = new MQTTSubscriptionManager(this);
      retainMessageManager = new MQTTRetainMessageManager(this);
   }

   // Called after the client has Connected.
   void start() throws Exception
   {
      mqttQoSManager.start();
      subscriptionManager.start();
      stopped = false;
   }

   // TODO ensure resources are cleaned up for GC.
   void stop() throws Exception
   {
      if (!stopped)
      {
         // TODO this should pass in clean session.
         subscriptionManager.stop(false);
         mqttQoSManager.stop(false);

         if (serverSession != null)
         {
            serverSession.stop();
            serverSession.close(false);
         }

         state.setAttached(false);
      }
      stopped = true;
   }

   boolean getStopped()
   {
      return stopped;
   }

   MQTTConnection getConnection()
   {
      return connection;
   }

   MQTTQoSManager getMqttQoSManager()
   {
      return mqttQoSManager;
   }

   MQTTSessionState getState()
   {
      return state;
   }

   MQTTConnectionManager getConnectionManager()
   {
      return mqttConnectionManager;
   }

   MQTTSessionState getSessionState()
   {
      return state;
   }

   ServerSessionImpl getServerSession()
   {
      return serverSession;
   }

   ActiveMQServer getServer()
   {
      return connection.getServer();
   }

   MQTTSubscriptionManager getSubscriptionManager()
   {
      return subscriptionManager;
   }

   MQTTProtocolHandler getProtocolHandler()
   {
      return protocolHandler;
   }

   SessionCallback getSessionCallback()
   {
      return sessionCallback;
   }

   void setServerSession(ServerSessionImpl serverSession)
   {
      this.serverSession = serverSession;
   }

   void setSessionState(MQTTSessionState state)
   {
      this.state = state;
      state.setAttached(true);
   }

   MQTTRetainMessageManager getRetainMessageManager()
   {
      return retainMessageManager;
   }
   //   private boolean isDuplicate(ServerMessage message)
//   {
//      boolean result = true;
//      byte[] messageId = ByteBuffer.allocate(8).putLong(message.getMessageID()).array();
//      synchronized (duplicateIDCache)
//      {
//         try
//         {
//            if (!duplicateIDCache.contains(messageId))
//            {
//               duplicateIDCache.addToCache(messageId, null);
//               result = false;
//            }
//         }
//         catch(Exception e)
//         {
//            log.error("Unable to store ID in duplicate ID cache: " + e.getMessage());
//         }
//      }
//      return result;
//   }
}
