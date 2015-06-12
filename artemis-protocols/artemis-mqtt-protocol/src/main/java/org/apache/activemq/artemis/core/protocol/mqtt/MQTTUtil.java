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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.*;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.core.buffers.impl.ChannelBufferWrapper;
import org.apache.activemq.artemis.core.postoffice.DuplicateIDCache;
import org.apache.activemq.artemis.core.postoffice.impl.DuplicateIDCacheImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.impl.ServerMessageImpl;
import org.apache.activemq.artemis.core.server.impl.ServerSessionImpl;
import org.apache.activemq.artemis.utils.UUIDGenerator;

/**
 * A Utility Class for creating Server Side objects and converting MQTT concepts to/from Artemis.
 *
 * @author mtaylor
 */

public class MQTTUtil
{
   public static final int DEFAULT_SERVER_MESSAGE_BUFFER_SIZE = 512;

   public static final int DUPLICATE_ID_CACHE_SIZE = 1000;

   public static final boolean DUPLICATE_ID_PERSISTED = false;

   public static final String MQTT_ADDRESS_PREFIX = "$sys.mqtt.";

   public static final String MQTT_RETAIN_ADDRESS_PREFIX = "$sys.mqtt.retain.";

   public static final String MQTT_QOS_LEVEL_KEY = "mqtt.qos.level";

   public static final String MQTT_WILL_RETAIN_KEY = "mqtt.will.retain";

   public static final String MQTT_MESSAGE_ID_KEY = "mqtt.message.id";

   public static final String MQTT_MESSAGE_TYPE_KEY = "mqtt.message.type";

   public static final String MQTT_MESSAGE_RETAIN_KEY = "mqtt.message.retain";

   public static final boolean SESSION_AUTO_COMMIT_SENDS = true;

   public static final boolean SESSION_AUTO_COMMIT_ACKS = false;

   public static final boolean SESSION_PREACKNOWLEDGE = false;

   public static final boolean SESSION_XA = false;

   public static final boolean SESSION_AUTO_CREATE_QUEUE = false;

   // Default Keep Alive Frequency Seconds.
   public static final int DEFAULT_KEEP_ALIVE_FREQUENCY = 15;

   public static String convertMQTTAddressFilterToCore(String filter)
   {
      return MQTT_ADDRESS_PREFIX + swapMQTTAndCoreWildCards(filter);
   }

   public static String convertCoreAddressFilterToMQTT(String filter)
   {
      if (filter.startsWith(MQTT_RETAIN_ADDRESS_PREFIX.toString()))
      {
         filter = filter.substring(MQTT_RETAIN_ADDRESS_PREFIX.length(), filter.length());
      }
      else if (filter.startsWith(MQTT_ADDRESS_PREFIX.toString()))
      {
         filter = filter.substring(MQTT_ADDRESS_PREFIX.length(), filter.length());
      }
      return swapMQTTAndCoreWildCards(filter);
   }

   public static String convertMQTTAddressFilterToCoreRetain(String filter)
   {
      return MQTT_RETAIN_ADDRESS_PREFIX + swapMQTTAndCoreWildCards(filter);
   }

   public static String swapMQTTAndCoreWildCards(String filter)
   {
      char[] topicFilter = filter.toCharArray();
      for (int i=0; i<topicFilter.length; i++)
      {
         switch (topicFilter[i])
         {
            case '/': topicFilter[i] = '.'; break;
            case '.': topicFilter[i] = '/'; break;
            case '*': topicFilter[i] = '+'; break;
            case '+': topicFilter[i] = '*'; break;
            default: break;
         }
      }
      return String.valueOf(topicFilter);
   }

   public static DuplicateIDCache createDuplicateIdCache(MQTTConnection connection, String clientId)
   {
      return new DuplicateIDCacheImpl(new SimpleString(clientId), DUPLICATE_ID_CACHE_SIZE,
            connection.getServer().getStorageManager(), DUPLICATE_ID_PERSISTED);
   }

   public static ServerMessage createServerMessage(MQTTConnection connection, ByteBuf payload)
   {
      long id = connection.getServer().getStorageManager().generateID();
      ServerMessage message = new ServerMessageImpl(id, DEFAULT_SERVER_MESSAGE_BUFFER_SIZE);

      // FIXME does this involve a copy?
      message.getBodyBuffer().writeBytes(new ChannelBufferWrapper(payload), payload.readableBytes());
      return message;
   }

   public static ServerMessage createServerMessage(MQTTConnection connection, String topic, boolean retain, int qos, ByteBuf payload)
   {
      String coreAddress = convertMQTTAddressFilterToCore(topic);

      ServerMessage serverMessage = createServerMessage(connection, payload);
      serverMessage.setAddress(new SimpleString(coreAddress));
      serverMessage.putBooleanProperty(new SimpleString(MQTT_MESSAGE_RETAIN_KEY), retain);
      serverMessage.putIntProperty(new SimpleString(MQTT_QOS_LEVEL_KEY), qos);
      return serverMessage;
   }

   public static void logMessage(MQTTLogger logger, MqttMessage message, boolean inbound)
   {
      StringBuilder log = inbound ? new StringBuilder("Received ") : new StringBuilder("Sent ");
      log.append(message.fixedHeader().messageType());

      if (message.variableHeader() instanceof MqttPublishVariableHeader)
      {
         log.append("(" + ((MqttPublishVariableHeader) message.variableHeader()).messageId() + ") " + message.fixedHeader().qosLevel());
      }
      else if (message.variableHeader() instanceof MqttMessageIdVariableHeader)
      {
         log.append("(" + ((MqttMessageIdVariableHeader) message.variableHeader()).messageId() + ")");
      }

      if (message.fixedHeader().messageType() == MqttMessageType.SUBSCRIBE)
      {
         log.append(" " + message.fixedHeader().qosLevel());
      }

      logger.debug(log.toString());
   }
}
