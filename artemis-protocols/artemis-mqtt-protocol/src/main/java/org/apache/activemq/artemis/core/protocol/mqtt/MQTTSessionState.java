/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.core.protocol.mqtt;

import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import org.apache.activemq.artemis.core.server.impl.ServerMessageImpl;
import org.apache.activemq.artemis.utils.ConcurrentHashSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MQTTSessionState
{
   private String clientId;

   private ServerMessageImpl willMessage;

   private ConcurrentHashMap<String, MqttTopicSubscription> subscriptions;

   // Used to store Packet ID of Publish QoS1 and QoS2 message.  See spec: 4.3.3 QoS 2: Exactly once delivery.  Method B.
   private Map<Integer, MQTTMessageInfo> messageRefStore;

   private Map<String, Map<Long, Integer>> addressMessageMap;

   private boolean durable;

   private boolean attached = false;

   private ActiveMQServerLogger log = ActiveMQServerLogger.LOGGER;

   public MQTTSessionState(String clientId)
   {
      this.clientId = clientId;

      subscriptions = new ConcurrentHashMap<>();
      messageRefStore = new ConcurrentHashMap<>();
      addressMessageMap = new ConcurrentHashMap<>();

      durable = false;
   }

   boolean getAttached()
   {
      return attached;
   }

   void setAttached(boolean attached)
   {
      this.attached = attached;
   }

   public boolean isWill()
   {
      return willMessage != null;
   }

   public ServerMessageImpl getWillMessage()
   {
      return willMessage;
   }

   public void setWillMessage(ServerMessageImpl willMessage)
   {
      this.willMessage = willMessage;
   }

   public void deleteWillMessage()
   {
      willMessage = null;
   }

   public Collection<MqttTopicSubscription> getSubscriptions()
   {
      return subscriptions.values();
   }

   boolean addSubscription(MqttTopicSubscription subscription)
   {
      synchronized (subscriptions)
      {
         addressMessageMap.putIfAbsent(MQTTUtil.convertMQTTAddressFilterToCore(subscription.topicName()), new ConcurrentHashMap<Long, Integer>());

         MqttTopicSubscription existingSubscription = subscriptions.get(subscription.topicName());
         if (existingSubscription != null)
         {
            if (subscription.qualityOfService().value() > existingSubscription.qualityOfService().value())
            {
               subscriptions.put(subscription.topicName(), subscription);
               return true;
            }
         }
         else
         {
            subscriptions.put(subscription.topicName(), subscription);
            return true;
         }
      }
      return false;
   }

   void removeSubscription(String address)
   {
      subscriptions.remove(address);
      addressMessageMap.remove(address);
   }

   MqttTopicSubscription getSubscription(String address)
   {
      return subscriptions.get(address);
   }

   public boolean isDurable()
   {
      return durable;
   }

   public void setDurable(boolean durable)
   {
      this.durable = durable;
   }

   public String getClientId()
   {
      return clientId;
   }

   public void setClientId(String clientId)
   {
      this.clientId = clientId;
   }


   void storeMessageRef(Integer mqttId, MQTTMessageInfo messageInfo)
   {
      log.info("Storing MQTT: " + mqttId + " for " + messageInfo.getAddress() + " " + messageInfo.getServerMessageId() + " Consumer: " + messageInfo.getConsumerId());
      messageRefStore.put(mqttId, messageInfo);


      Map<Long, Integer> addressMap = addressMessageMap.get(messageInfo.getAddress());
      if (addressMap != null)
      {
         addressMap.put(messageInfo.getServerMessageId(), mqttId);
      }
   }

   void removeMessageRef(Integer mqttId)
   {
      MQTTMessageInfo info = messageRefStore.remove(mqttId);
      if (info != null)
      {
         Map<Long, Integer> addressMap = addressMessageMap.get(info.getAddress());
         if (addressMap != null)
         {
            addressMap.remove(info.getServerMessageId());
         }
      }
   }

   boolean messageRefExists(Integer mqttId)
   {
      return messageRefStore.containsKey(mqttId);
   }

   MQTTMessageInfo getMessageInfo(Integer mqttId)
   {
      return messageRefStore.get(mqttId);
   }

   Integer getMessageRefFromAddress(String address, Long serverMessageId)
   {
      Map<Long, Integer> addressMap = addressMessageMap.get(address);
      if (addressMap != null)
      {
         return addressMap.get(serverMessageId);
      }
      return null;
   }
}
