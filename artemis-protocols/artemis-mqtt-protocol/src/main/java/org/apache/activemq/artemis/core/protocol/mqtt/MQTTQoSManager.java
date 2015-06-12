package org.apache.activemq.artemis.core.protocol.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.*;
import org.apache.activemq.artemis.core.server.impl.ServerMessageImpl;

/**
 * Handles MQTT Exactly Once (QoS level 2) Protocol.
 */
public class MQTTQoSManager
{
   private static final String MANAGEMENT_QUEUE_PREFIX = "$sys.mqtt.queue.qos2.";

   private SimpleString managementAddress;

   private ServerConsumer managementConsumer;

   private MQTTSession session;

   private MQTTLogger log = MQTTLogger.LOGGER;

   private Object sendLock = new Object();

   public MQTTQoSManager(MQTTSession session)
   {
      this.session = session;
   }

   void start() throws Exception
   {
      createManagementAddress();
      createManagementQueue();
      createManagementConsumer();
   }

   public void stop(boolean clean) throws Exception
   {
      if (managementConsumer != null)
      {
         managementConsumer.close(false);
         if (clean)
         {
            // delete manangement queues
         }
      }
   }

   private void createManagementConsumer() throws Exception
   {
      long consumerId = session.getServer().getStorageManager().generateID();
      managementConsumer = session.getServerSession().createConsumer(consumerId, managementAddress, null, false, false, -1);
      managementConsumer.setStarted(true);
   }

   private void createManagementAddress()
   {
      String clientId = session.getSessionState().getClientId();
      managementAddress = new SimpleString(MANAGEMENT_QUEUE_PREFIX + clientId);
   }

   private void createManagementQueue() throws Exception
   {
      if (session.getServer().locateQueue(managementAddress) == null)
      {
         session.getServerSession().createQueue(managementAddress, managementAddress, null, false, true);
      }
   }

   protected boolean isExactlyOnceManagementConsumer(ServerConsumer consumer)
   {
      return consumer == managementConsumer;
   }

   protected synchronized void sendMessage(ServerMessage message, ServerConsumer consumer, int deliveryCount) throws Exception
   {
      if (isExactlyOnceManagementConsumer(consumer))
      {
         sendPubRelMessage(message);
         return;
      }

      /* Since MQTT Subscriptions can over lap; a client may receive the same message twice.  When this happens the client
      returns a PubRec or PubAck with ID.  But we need to know which consumer to ack, since we only have the ID to go on we
      are not able to decide which consumer to ack.  Instead we send MQTT messages with different IDs and store a reference
      to original ID and consumer in the Session state.  This way we can look up the consumer Id and the message Id from
      the PubAck or PubRec message id.
       */

      // Allows reconnects to use the same message id but different consumer ids (we ID the consumer by address).

      int qos = decideQoS(message, consumer);

      if (qos == 0)
      {
         sendServerMessage((int) message.getMessageID(), message, consumer, deliveryCount, qos);
         session.getServerSession().acknowledge(consumer.getID(), message.getMessageID());
      }
      else
      {
         String consumerAddress = consumer.getQueue().getAddress().toString();
         Integer mqttId = session.getSessionState().getMessageRefFromAddress(consumerAddress, message.getMessageID());

         if (mqttId == null)
         {
            mqttId = (int) session.getServer().getStorageManager().generateID();
            MQTTMessageInfo messageInfo = new MQTTMessageInfo(mqttId, message.getMessageID(), consumer.getID(), consumerAddress);
            session.getSessionState().storeMessageRef(mqttId, messageInfo, true);
         }
         sendServerMessage(mqttId, message, consumer, deliveryCount, qos);
      }


   }

   protected void sendPubRelMessage(ServerMessage message)
   {
      if (message.getIntProperty(MQTTUtil.MQTT_MESSAGE_TYPE_KEY) == MqttMessageType.PUBREL.value())
      {
         int messageId = message.getIntProperty(MQTTUtil.MQTT_MESSAGE_ID_KEY);
         MQTTMessageInfo messageInfo = new MQTTMessageInfo(messageId, message.getMessageID(), managementConsumer.getID(), message.getAddress().toString());
         session.getSessionState().storeMessageRef(messageId, messageInfo, false);
         session.getProtocolHandler().sendPubRel(messageId);
      }
   }

   protected void handleMessage(int messageId, String topic, int qos, ByteBuf payload, boolean retain) throws Exception
   {
      synchronized (sendLock)
      {
         ServerMessage serverMessage = MQTTUtil.createServerMessage(session.getConnection(), topic, retain, qos, payload);

         if (qos == MqttQoS.EXACTLY_ONCE.value())
         {
            handlePubQoS2(serverMessage, messageId);
         }
         else if (qos == MqttQoS.AT_LEAST_ONCE.value())
         {
            handlePubQoS1(serverMessage, messageId);
         }
         else
         {
            session.getServerSession().send(serverMessage, true);
         }

         if (retain)
         {
            boolean emptyPayload = payload.unwrap() instanceof EmptyByteBuf;
            session.getRetainMessageManager().handleRetainedMessage(serverMessage, topic, emptyPayload);
         }
      }
   }

   private void handlePubQoS1(ServerMessage message, int messageId) throws Exception
   {
      if (!session.getSessionState().getPub().contains(messageId))
      {
         session.getServerSession().send(message, true);
      }
      session.getProtocolHandler().sendPubAck(messageId);
      session.getSessionState().getPub().add(messageId);
   }

   public void handlePubQoS2(ServerMessage message, int messageId) throws Exception
   {
      if (!session.getSessionState().getPub().contains(messageId))
      {
         session.getServerSession().send(message, true);
         //session.getSessionState().storeMessageRef(messageId, new MQTTMessageInfo(messageId, message.getMessageID(), 0L,message.getAddress().toString()), false);
      }
      session.getProtocolHandler().sendPubRec(messageId);
      session.getSessionState().getPub().add(messageId);
   }

   protected void handlePubRec(int messageId) throws Exception
   {
      MQTTMessageInfo messageRef = session.getSessionState().getMessageInfo(messageId);
      if (messageRef != null)
      {
         //ServerMessage pubRel = createPubRelMessage(messageId);
         //session.getServerSession().send(pubRel, true);
         session.getServerSession().acknowledge(messageRef.getConsumerId(), messageRef.getServerMessageId());
         session.getProtocolHandler().sendPubRel(messageId);
      }
   }

   public void handlePubComp(int messageId) throws Exception
   {
      try
      {
         MQTTMessageInfo messageInfo = session.getSessionState().getMessageInfo(messageId);
         if (messageInfo != null)
         {
            session.getServerSession().acknowledge(managementConsumer.getID(), messageInfo.getServerMessageId());
         }
         else
         {
            log.warn("No message to Ack -> PubComp(" + messageId + ")");
         }
      }
      catch(Exception e)
      {
         log.error("Failed to ack pub comp for message id: " + messageId);
      }
   }

   protected void handlePubRel(int messageId)
   {
      session.getProtocolHandler().sendPubComp(messageId);
      // We don't check to see if a PubRel existed for this message.  We assume it did and so send PubComp.
      session.getSessionState().removeMessageRef(messageId);
   }


   protected void handlePubAck(int messageId) throws Exception
   {
      MQTTMessageInfo messageInfo = session.getSessionState().getMessageInfo(messageId);
      if (messageInfo != null)
      {
         session.getServerSession().acknowledge(messageInfo.getConsumerId(), messageInfo.getServerMessageId());
         session.getSessionState().removeMessageRef(messageId);
      }
      else
      {
         log.warn("No message to Ack -> PubAck(" + messageId + ")");
      }
   }

   protected void sendServerMessage(int messageId, ServerMessage message, ServerConsumer consumer, int deliveryCount, int qos)
   {
      ByteBuf payload = message.getBodyBuffer().copy(message.getBodyBuffer().readerIndex(),
            message.getBodyBuffer().readableBytes()).byteBuf();

      String address = MQTTUtil.convertCoreAddressFilterToMQTT(message.getAddress().toString()).toString();
      session.getProtocolHandler().send(messageId, address, qos, payload, deliveryCount);
   }

   private ServerMessage createPubRelMessage(int messageId)
   {
      long id = session.getServer().getStorageManager().generateID();
      ServerMessage message = new ServerMessageImpl(id, MQTTUtil.DEFAULT_SERVER_MESSAGE_BUFFER_SIZE);
      message.putIntProperty(new SimpleString(MQTTUtil.MQTT_MESSAGE_ID_KEY), messageId);
      message.putIntProperty(new SimpleString(MQTTUtil.MQTT_QOS_LEVEL_KEY), 1);
      message.putIntProperty(new SimpleString(MQTTUtil.MQTT_MESSAGE_TYPE_KEY), MqttMessageType.PUBREL.value());
      message.setAddress(managementAddress);
      return message;
   }

   private int decideQoS(ServerMessage message, ServerConsumer consumer)
   {
      int subscriptionQoS = session.getSubscriptionManager().getConsumerQoSLevels().get(consumer.getID());
      int qos = message.getIntProperty(MQTTUtil.MQTT_QOS_LEVEL_KEY);

      return subscriptionQoS < qos ? subscriptionQoS : qos;
   }
}