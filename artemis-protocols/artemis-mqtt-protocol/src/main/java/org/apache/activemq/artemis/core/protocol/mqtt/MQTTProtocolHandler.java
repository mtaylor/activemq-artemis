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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.core.buffers.impl.ChannelBufferWrapper;
import org.apache.activemq.artemis.core.protocol.mqtt.codec.MQTTDecoder;
import org.apache.activemq.artemis.core.protocol.mqtt.codec.MQTTEncoder;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnection;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;

public class MQTTProtocolHandler
{
   // Max length of MQTT message in bytes.
   public final int MAX_MESSAGE_LENGTH = 268435455;

   //TODO We should read in a list of existing client IDs from stored Sessions.
   private static final ConcurrentHashMap<String, MQTTSession> sessions = new ConcurrentHashMap<String, MQTTSession>();

   private MQTTEncoder encoder;

   private MQTTDecoder decoder;

   private MQTTConnection connection;

   private MQTTSession session;

   private ActiveMQServerLogger log = ActiveMQServerLogger.LOGGER;;

   public MQTTProtocolHandler(MQTTSession session)
   {
      this.connection = session.getConnection();
      this.session = session;

      decoder = new MQTTDecoder(MAX_MESSAGE_LENGTH);
      encoder = new MQTTEncoder();
   }

   public void handleBuffer(ActiveMQBuffer buffer)
   {
      try
      {
         List<Object> messages = decoder.decode(buffer);
         for (Object obj : messages)
         {
            MqttMessage message = (MqttMessage) obj;

            // Disconnect if Netty codec failed to decode the stream.
            if (message.decoderResult().isFailure())
            {
               // FIXME We currently do not support messages > netty buffer size.
               log.warn("Unable to decode message.  Disconnecting: " + message.decoderResult().toString());
               return;
            }

            log.warn("Message Type " + message.fixedHeader().messageType());
            switch (message.fixedHeader().messageType())
            {
               case CONNECT:
                  handleConnect((MqttConnectMessage) message);
                  break;
               case CONNACK:
                  handleConnack((MqttConnAckMessage) message);
                  break;
               case PUBLISH:
                  handlePublish((MqttPublishMessage) message);
                  break;
               case PUBACK:
                  handlePuback((MqttPubAckMessage) message);
                  break;
               case PUBREC:
                  handlePubrec(message);
                  break;
               case PUBREL:
                  handlePubrel(message);
                  break;
               case PUBCOMP:
                  handlePubcomp(message);
                  break;
               case SUBSCRIBE:
                  handleSubscribe((MqttSubscribeMessage) message);
                  break;
               case SUBACK:
                  handleSuback((MqttSubAckMessage) message);
                  break;
               case UNSUBSCRIBE:
                  handleUnsubscribe((MqttUnsubscribeMessage) message);
                  break;
               case UNSUBACK:
                  handleUnsuback((MqttUnsubAckMessage) message);
                  break;
               case PINGREQ:
                  handlePingreq(message);
                  break;
               case PINGRESP:
                  handlePingresp(message);
                  break;
               case DISCONNECT:
                  handleDisconnect(message);
                  break;
               default:
                  disconnect();
            }
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
         log.warn("Error processing Control Packet, Disconnecting Client" + e.getMessage());
         disconnect();
      }
   }

   /**
    * Called during connection.
    *
    * @param connect
    */

   public void handleConnect(MqttConnectMessage connect) throws Exception
   {
      String clientId = connect.payload().clientIdentifier();

      session.getConnectionManager().connect(clientId,
            connect.payload().userName(),
            connect.payload().password(),
            connect.variableHeader().isWillFlag(),
            connect.payload().willMessage(),
            connect.payload().willTopic(),
            connect.variableHeader().isWillRetain(),
            connect.variableHeader().willQos(),
            connect.variableHeader().isCleanSession());
   }

   void disconnect()
   {
      session.getConnectionManager().disconnect();
   }

   void sendConnack(MqttConnectReturnCode returnCode)
   {
      MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK,
                                                        false,
                                                        MqttQoS.AT_MOST_ONCE,
                                                        false,
                                                        0);
      MqttConnAckVariableHeader varHeader = new MqttConnAckVariableHeader(returnCode);
      MqttConnAckMessage message = new MqttConnAckMessage(fixedHeader, varHeader);
      write(message);
   }

   /**
    * The server does not instantiate connections therefore any CONNACK received over a connection is an invalid
    * control message.
    * @param message
    */
   public void handleConnack(MqttConnAckMessage message)
   {
      log.debug("Received invalid CONNACK from client: " + session.getSessionState().getClientId());
      log.debug("Disconnecting client: " + session.getSessionState().getClientId());
      disconnect();
   }

   public synchronized void handlePublish(MqttPublishMessage message) throws Exception
   {
      session.getMqttQoSManager().handleMessage(message.variableHeader().messageId(),
            message.variableHeader().topicName(),
            message.fixedHeader().qosLevel().value(),
            message.payload(),
            message.fixedHeader().isRetain());
   }

   void sendPubAck(int messageId)
   {
      sendPublishProtocolControlMessage(messageId, MqttMessageType.PUBACK);
   }

   protected void sendPubRel(int messageId)
   {
      sendPublishProtocolControlMessage(messageId, MqttMessageType.PUBREL);
   }

   protected void sendPubRec(int messageId)
   {
      sendPublishProtocolControlMessage(messageId, MqttMessageType.PUBREC);
   }

   protected void sendPubComp(int messageId)
   {
      sendPublishProtocolControlMessage(messageId, MqttMessageType.PUBCOMP);
   }

   private void sendPublishProtocolControlMessage(int messageId, MqttMessageType messageType)
   {
      MqttFixedHeader fixedHeader = new MqttFixedHeader(messageType,
                                                        false,
                                                        MqttQoS.AT_LEAST_ONCE, // Spec requires 01 in header.
                                                        false,
                                                        0);
      MqttPubAckMessage rel = new MqttPubAckMessage(fixedHeader, MqttMessageIdVariableHeader.from(messageId));
      write(rel);
   }

   public void handlePuback(MqttPubAckMessage message) throws Exception
   {
      session.getMqttQoSManager().handlePubAck(message.variableHeader().messageId());
   }

   public void handlePubrec(MqttMessage message) throws Exception
   {
      int messageId =  ((MqttMessageIdVariableHeader) message.variableHeader()).messageId();
      session.getMqttQoSManager().handlePubRec(messageId);
   }

   public void handlePubrel(MqttMessage message)
   {
      int messageId = ((MqttMessageIdVariableHeader) message.variableHeader()).messageId();
      session.getMqttQoSManager().handlePubRel(messageId);
   }

   public void handlePubcomp( MqttMessage message) throws Exception
   {
      int messageId = ((MqttMessageIdVariableHeader) message.variableHeader()).messageId();
      session.getMqttQoSManager().handlePubComp(messageId);
   }

   public void handleSubscribe( MqttSubscribeMessage message) throws Exception
   {
      //TODO Error handling
      MQTTSubscriptionManager subscriptionManager = session.getSubscriptionManager();
      int[] qos = subscriptionManager.addSubscriptions(message.payload().topicSubscriptions());

      MqttFixedHeader header = new MqttFixedHeader(MqttMessageType.SUBACK,
                                                   false,
                                                   MqttQoS.AT_MOST_ONCE,
                                                   false,
                                                   0);
      MqttSubAckMessage ack = new MqttSubAckMessage(header,
                                                    message.variableHeader(),
                                                    new MqttSubAckPayload(qos));
      write(ack);
   }

   public void handleSuback(MqttSubAckMessage message)
   {
      disconnect();
   }

   public void handleUnsubscribe(MqttUnsubscribeMessage message) throws Exception
   {
      session.getSubscriptionManager().removeSubscriptions(message.payload().topics());
      MqttFixedHeader header = new MqttFixedHeader(MqttMessageType.UNSUBACK,
                                                   false,
                                                   MqttQoS.AT_MOST_ONCE,
                                                   false,
                                                   0);
      MqttUnsubAckMessage m = new MqttUnsubAckMessage(header, message.variableHeader());
      write(m);
   }

   public void handleUnsuback(MqttUnsubAckMessage message)
   {
      disconnect();
   }

   public void handlePingreq(MqttMessage message)
   {
      write(new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGRESP,
                                                            false,
                                                            MqttQoS.AT_MOST_ONCE,
                                                            false,
                                                            0)));
   }

   public void handlePingresp(MqttMessage message)
   {
      disconnect();
   }

   public void handleDisconnect(MqttMessage message)
   {
      session.getState().deleteWillMessage();
      disconnect();
   }

   private void write(MqttMessage message)
   {
      write(((NettyConnection) connection.getTransportConnection()), message);
      if (!(message instanceof MqttConnAckMessage))
      {
         log.warn("Sent to:" + session.getSessionState().getClientId() + "message: " + message);
      }
   }

   private void write(NettyConnection connection, MqttMessage message)
   {
      try
      {
         ByteBufAllocator bufAllocator = connection.getChannel().alloc();
         ChannelBufferWrapper channelBufferWrapper = new ChannelBufferWrapper(encoder.encode(message, bufAllocator));
         synchronized (this)
         {
            connection.write(channelBufferWrapper);
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   protected int send(int messageId, String topicName, int qosLevel,  ByteBuf payload, int deliveryCount)
   {
      boolean redelivery = qosLevel == 0 ? false : (deliveryCount > 0);
      MqttFixedHeader header = new MqttFixedHeader(MqttMessageType.PUBLISH,
                                                   redelivery,
                                                   MqttQoS.valueOf(qosLevel),
                                                   false,
                                                   0);
      MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topicName, messageId);
      MqttMessage publish = new MqttPublishMessage(header, varHeader, payload);

      write(publish);

      //TODO Handle errors
      return 1;
   }
}
