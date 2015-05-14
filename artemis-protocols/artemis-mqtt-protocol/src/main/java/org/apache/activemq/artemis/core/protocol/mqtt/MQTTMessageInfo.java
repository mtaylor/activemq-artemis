package org.apache.activemq.artemis.core.protocol.mqtt;

/**
 * MQTT Acks only hold message ID information.  From this we must infer the internal message ID and consumer.
 */
public class MQTTMessageInfo
{
   private long serverMessageId;

   private long consumerId;

   private long mqttId;

   private String address;

   public MQTTMessageInfo(long mqttId, long serverMessageId, long consumerId, String address)
   {
      this.mqttId = mqttId;
      this.serverMessageId = serverMessageId;
      this.consumerId = consumerId;
      this.address = address;
   }

   public long getServerMessageId()
   {
      return serverMessageId;
   }

   public long getMqttId()
   {
      return mqttId;
   }

   public long getConsumerId()
   {
      return consumerId;
   }

   public String getAddress()
   {
      return address;
   }

   public String toString()
   {
      return ("MqttId: " + mqttId + " ServerMessageId: " + serverMessageId + " ConsumerId: " + consumerId + " addr: " + address);
   }
}
