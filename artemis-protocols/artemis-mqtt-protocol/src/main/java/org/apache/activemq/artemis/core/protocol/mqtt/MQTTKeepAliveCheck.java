package org.apache.activemq.artemis.core.protocol.mqtt;

public class MQTTKeepAliveCheck implements Runnable
{
   private MQTTProtocolHandler handler;

   private long keepAliveInterval;

   public MQTTKeepAliveCheck(MQTTProtocolHandler handler, int keepAliveInterval)
   {
      this.handler = handler;

      // Spec says check for 1.5 * time interval.  This also converts seconds to millis.
      this.keepAliveInterval = keepAliveInterval * 1500;
   }

   @Override
   public void run()
   {
      if (System.currentTimeMillis() >= handler.getLastMessageReceived() + keepAliveInterval)
      {
         handler.handleDisconnect(null);
      }
   }
}
