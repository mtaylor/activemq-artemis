package org.apache.activemq.artemis.core.protocol.mqtt;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import org.apache.activemq.artemis.core.server.ServerConsumer;
import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.spi.core.protocol.SessionCallback;
import org.apache.activemq.artemis.spi.core.remoting.ReadyListener;

public class MQTTSessionCallback implements SessionCallback
{
   private MQTTSession session;

   private ActiveMQServerLogger log = ActiveMQServerLogger.LOGGER;

   public MQTTSessionCallback(MQTTSession session) throws Exception
   {
      this.session = session;
   }

   @Override
   public int sendMessage(ServerMessage message, ServerConsumer consumer, int deliveryCount)
   {
      try
      {
         session.getMqttQoSManager().sendMessage(message, consumer, deliveryCount);
      }
      catch (Exception e)
      {
         log.warn("Unable to send message: " + message.getMessageID() + " Cause: " + e.getMessage());
      }
      return 1;
   }

   @Override
   public int sendLargeMessageContinuation(ServerConsumer consumerID, byte[] body, boolean continues, boolean requiresResponse)
   {
      log.warn("Sending LARGE MESSAGE");
      return 1;
   }

   @Override
   public void addReadyListener(ReadyListener listener)
   {
      session.getConnection().getTransportConnection().addReadyListener(listener);
   }

   @Override
   public void removeReadyListener(ReadyListener listener)
   {
      session.getConnection().getTransportConnection().removeReadyListener(listener);
   }

   @Override
   public int sendLargeMessage(ServerMessage message, ServerConsumer consumer, long bodySize, int deliveryCount)
   {
      return sendMessage(message, consumer, deliveryCount);
   }

   @Override
   public void disconnect(ServerConsumer consumer, String queueName)
   {
      try
      {
         consumer.removeItself();
      }
      catch (Exception e)
      {
         log.error(e.getMessage());
      }
   }

   @Override
   public boolean hasCredits(ServerConsumer consumerID)
   {
      return true;
   }

   @Override
   public void sendProducerCreditsMessage(int credits, SimpleString address)
   {
   }

   @Override
   public void sendProducerCreditsFailMessage(int credits, SimpleString address)
   {
   }

   @Override
   public void closed()
   {
   }

}
