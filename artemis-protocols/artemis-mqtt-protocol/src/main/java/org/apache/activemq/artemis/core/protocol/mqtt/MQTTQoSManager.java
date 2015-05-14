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

   private ActiveMQServerLogger log = ActiveMQServerLogger.LOGGER;

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
      // Allows reconnects to use the same message id.

      String consumerAddress = consumer.getQueue().getAddress().toString();

      log.info("Address: " + consumerAddress);

      Integer mqttId = session.getSessionState().getMessageRefFromAddress(consumerAddress, message.getMessageID());

      if (mqttId == null)
      {
         mqttId = (int) session.getServer().getStorageManager().generateID();
      }


      if (isExactlyOnceManagementConsumer(consumer))
      {
         sendPubRelMessage(message);
      }
      else
      {
         int qos = decideQoS(message, consumer);
         if (qos == 0)
         {
            log.info("Send: " + mqttId);
            session.getServerSession().acknowledge(consumer.getID(), message.getMessageID());
         }
         else
         {
            MQTTMessageInfo messageInfo = new MQTTMessageInfo(mqttId, message.getMessageID(), consumer.getID(), consumerAddress);
            if (mqttId == 35)
            {
               Thread.sleep(10000);
            }
            session.getSessionState().storeMessageRef(mqttId, messageInfo);

            log.info(this.toString());
            log.info("X Send: " + mqttId + " QoS: " + qos);
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
         log.info("S2: " + messageInfo);
         session.getSessionState().storeMessageRef(messageId, messageInfo);
         session.getProtocolHandler().sendPubRel(messageId);
      }
   }

   protected void handleMessage(int messageId, String topic, int qos, ByteBuf payload, boolean retain) throws Exception
   {
      log.info("Received Message: " + messageId);
      synchronized (sendLock)
      {
         ServerMessage serverMessage = MQTTUtil.createServerMessage(session.getConnection(), topic, retain, qos, payload);

         if (qos == MqttQoS.EXACTLY_ONCE.value())
         {
            handleExactlyOnceProtocolMessage(serverMessage, messageId);
         }
         else if (qos == MqttQoS.AT_LEAST_ONCE.value())
         {
            handleAtLeastOnceProtocolMessage(serverMessage, messageId);
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

   private void handleAtLeastOnceProtocolMessage(ServerMessage message, int messageId) throws Exception
   {
      session.getServerSession().send(message, true);
      session.getProtocolHandler().sendPubAck(messageId);
   }

   public void handleExactlyOnceProtocolMessage(ServerMessage message, int messageId) throws Exception
   {
      if (!session.getSessionState().messageRefExists(messageId))
      {
         session.getServerSession().send(message, true);
         session.getSessionState().storeMessageRef(messageId, new MQTTMessageInfo(messageId, message.getMessageID(), 0L,message.getAddress().toString()));
      }
      session.getProtocolHandler().sendPubRec(messageId);
   }

   protected void handlePubRec(int messageId) throws Exception
   {
      log.info("PubRec: " + messageId);
      MQTTMessageInfo messageRef = session.getSessionState().getMessageInfo(messageId);
      if (messageRef != null)
      {
         ServerMessage pubRel = createPubRelMessage(messageId);
         session.getServerSession().send(pubRel, true);
         session.getServerSession().acknowledge(messageRef.getConsumerId(), messageRef.getServerMessageId());
      }
   }

   public void handlePubComp(int messageId) throws Exception
   {
      try
      {
         log.info("PubComp: " + messageId);
         MQTTMessageInfo messageInfo = session.getSessionState().getMessageInfo(messageId);
         if (messageInfo != null)
         {
            session.getServerSession().acknowledge(managementConsumer.getID(), messageInfo.getServerMessageId());
         }
         else
         {
            log.warn("PubComp received but no message to Ack");
         }
      }
      catch(Exception e)
      {
         log.warn("Failed to ack pub comp for message id: " + messageId);
      }
   }

   protected void handlePubRel(int messageId)
   {
      session.getProtocolHandler().sendPubComp(messageId);
      // We don't check to see if a PubRel existed for this message.  We assume it did and so send PubComp.
      //session.getSessionState().removeMessageRef(messageId);
   }


   protected void handlePubAck(int messageId) throws Exception
   {
      log.info("PubAck: " + messageId);
      MQTTMessageInfo messageInfo = session.getSessionState().getMessageInfo(messageId);
      if (messageInfo != null)
      {
         log.info("L: " + messageInfo);
         session.getServerSession().acknowledge(messageInfo.getConsumerId(), messageInfo.getServerMessageId());
      }
      else
      {
         log.warn("PubAck received but no message to Ack");
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