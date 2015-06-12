package org.apache.activemq.artemis.tests.integration.mqtt.imported;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import javax.net.ssl.SSLContext;

/**
 * Created by mtaylor on 12/06/15.
 */
public class PahoMQTTClientProvider implements  MQTTClientProvider
{
   private MqttClient mqtt;

   @Override
   public void connect(String host) throws Exception
   {
      mqtt = new MqttClient("tcp://" + host + ":1883", "clientId", new MemoryPersistence());
      mqtt.setProtocolVersion(MqttProtocolVersion.V3_1_1);
      mqtt.connect();
   }

   @Override
   public void disconnect() throws Exception
   {
      mqtt.disconnect();
   }

   @Override
   public void publish(String topic, byte[] payload, int qos, boolean retained) throws Exception
   {
      mqtt.publish(topic, payload, qos, retained);
   }

   @Override
   public void publish(String topic, byte[] payload, int qos) throws Exception
   {
      mqtt.publish(topic, payload, qos, false);
   }

   @Override
   public void subscribe(String topic, int qos) throws Exception
   {
      mqtt.subscribe(topic, qos);
   }

   @Override
   public void unsubscribe(String topic) throws Exception
   {
      mqtt.unsubscribe(topic);
   }

   @Override
   public byte[] receive(int timeout) throws Exception
   {
      return new byte[0];
   }

   @Override
   public void setSslContext(SSLContext sslContext)
   {

   }

   @Override
   public void setWillMessage(String string)
   {

   }

   @Override
   public void setWillTopic(String topic)
   {

   }

   @Override
   public void setClientId(String clientId)
   {

   }

   @Override
   public void kill() throws Exception
   {

   }

   @Override
   public void setKeepAlive(int keepAlive) throws Exception
   {

   }

   private class PahoOnMessageCallback implements MqttCallback
   {

      private int receiveTimeOut;

      @Override
      public synchronized void connectionLost(Throwable cause)
      {

      }

      @Override
      public synchronized void messageArrived(String topic, MqttMessage message) throws Exception
      {
      }

      @Override
      public synchronized void deliveryComplete(IMqttDeliveryToken token)
      {

      }
   }
}
