/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.integration.mqtt.imported;

import org.apache.activemq.artemis.core.protocol.mqtt.MQTTLogger;
import org.apache.activemq.artemis.tests.unit.core.server.group.impl.SystemPropertyOverrideTest;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class PahoMQTTTest extends MQTTTestSupport
{

   private long time;

   private MQTTLogger log = MQTTLogger.LOGGER;

   @Test(timeout = 300000)
   public void testSendAndReceiveMQTT() throws Exception
   {
      MqttClient client = new MqttClient("tcp://localhost:" + 1883, "id", new MemoryPersistence());
      client.setProtocolVersion(MqttProtocolVersion.V3_1_1);

      client.connect();
      client.subscribe("test", 1);

      PahoCallback callback = new PahoCallback();
      client.setCallback(callback);


      int messagesSent = 0;

      time = System.currentTimeMillis();
      for (int i = 0; i < 100; i++)
      {
         client.publish("test", "1".getBytes(), 1, false);
         messagesSent++;
      }

      System.out.println("Sent");
      Thread.sleep(1000);
      client.disconnect();
      client.close();

      assertEquals(messagesSent, callback.getCount());
   }

   private class PahoCallback implements MqttCallback
   {
      private int count = 0;

      public int getCount()
      {
         return count;
      }

      @Override
      public synchronized void connectionLost(Throwable cause)
      {

      }

      @Override
      public synchronized void messageArrived(String topic, MqttMessage message) throws Exception
      {
         count++;
         if (count == 100)
         {
            System.out.println("TIME++++++ " + (System.currentTimeMillis() - time));
         }
      }

      @Override
      public synchronized void deliveryComplete(IMqttDeliveryToken token)
      {

      }
   }
}