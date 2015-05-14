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

package org.apache.activemq.artemis.tests.integration.mqtt.imported;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.tests.integration.mqtt.imported.util.Wait;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.mqtt.client.Tracer;
import org.fusesource.mqtt.codec.MQTTFrame;
import org.fusesource.mqtt.codec.PUBLISH;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.hawtbuf.UTF8Buffer.utf8;

public class MQTTTest extends MQTTTestSupport
{
   private static final Logger LOG = LoggerFactory.getLogger(MQTTTest.class);

   private static final int NUM_MESSAGES = 250;

//   @Ignore
//   @Test(timeout = 120 * 1000)
//   public void testRetainedMessageOnVirtualTopics() throws Exception
//   {
//      MQTT mqtt = createMQTTConnection();
//      mqtt.setKeepAlive((short) 60);
//
//      final String RETAIN = "RETAIN";
//      final String TOPICA = "VirtualTopic/TopicA";
//
//      final String[] clientIds = {null, "foo", "durable"};
//      for (String clientId : clientIds)
//      {
//         LOG.info("Testing now with Client ID: {}", clientId);
//
//         mqtt.setClientId(clientId);
//         mqtt.setCleanSession(!"durable".equals(clientId));
//
//         BlockingConnection connection = mqtt.blockingConnection();
//         connection.connect();
//
//         // set retained message and check
//         connection.publish(TOPICA, RETAIN.getBytes(), QoS.EXACTLY_ONCE, true);
//         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
//         Message msg = connection.receive(5000, TimeUnit.MILLISECONDS);
//         assertNotNull("No retained message for " + clientId, msg);
//         assertEquals(RETAIN, new String(msg.getPayload()));
//         msg.ack();
//         assertNull(connection.receive(500, TimeUnit.MILLISECONDS));
//
//         // test duplicate subscription
//         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
//         msg = connection.receive(15000, TimeUnit.MILLISECONDS);
//         assertNotNull("No retained message on duplicate subscription for " + clientId, msg);
//         assertEquals(RETAIN, new String(msg.getPayload()));
//         msg.ack();
//         assertNull(connection.receive(500, TimeUnit.MILLISECONDS));
//         connection.unsubscribe(new String[]{TOPICA});
//
//         // clear retained message and check that we don't receive it
//         connection.publish(TOPICA, "".getBytes(), QoS.AT_MOST_ONCE, true);
//         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
//         msg = connection.receive(500, TimeUnit.MILLISECONDS);
//         assertNull("Retained message not cleared for " + clientId, msg);
//         connection.unsubscribe(new String[]{TOPICA});
//
//         // set retained message again and check
//         connection.publish(TOPICA, RETAIN.getBytes(), QoS.EXACTLY_ONCE, true);
//         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
//         msg = connection.receive(5000, TimeUnit.MILLISECONDS);
//         assertNotNull("No reset retained message for " + clientId, msg);
//         assertEquals(RETAIN, new String(msg.getPayload()));
//         msg.ack();
//         assertNull(connection.receive(500, TimeUnit.MILLISECONDS));
//
//         // re-connect and check
//         connection.disconnect();
//         connection = mqtt.blockingConnection();
//         connection.connect();
//         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
//         msg = connection.receive(5000, TimeUnit.MILLISECONDS);
//         assertNotNull("No reset retained message for " + clientId, msg);
//         assertEquals(RETAIN, new String(msg.getPayload()));
//         msg.ack();
//         assertNull(connection.receive(500, TimeUnit.MILLISECONDS));
//
//         LOG.info("Test now unsubscribing from: {} for the last time", TOPICA);
//         connection.unsubscribe(new String[]{TOPICA});
//         connection.disconnect();
//      }
//   }



//   @Test(timeout = 90 * 1000)
//   public void testPacketIdGeneratorNonCleanSession() throws Exception
//   {
//      final MQTT mqtt = createMQTTConnection("nonclean-packetid", false);
//      mqtt.setKeepAlive((short) 15);
//
//      final Map<Short, PUBLISH> publishMap = new ConcurrentHashMap<Short, PUBLISH>();
//      mqtt.setTracer(new Tracer()
//      {
//         @Override
//         public void onReceive(MQTTFrame frame)
//         {
//            LOG.info("Client received:\n" + frame);
//            if (frame.messageType() == PUBLISH.TYPE)
//            {
//               PUBLISH publish = new PUBLISH();
//               try
//               {
//                  publish.decode(frame);
//                  LOG.info("PUBLISH " + publish);
//               }
//               catch (ProtocolException e)
//               {
//                  fail("Error decoding handleMessage " + e.getMessage());
//               }
//               if (publishMap.get(publish.messageId()) != null)
//               {
//                  assertTrue(publish.dup());
//               }
//               publishMap.put(publish.messageId(), publish);
//            }
//         }
//
//         @Override
//         public void onSend(MQTTFrame frame)
//         {
//            LOG.info("Client sent:\n" + frame);
//         }
//      });
//
//      BlockingConnection connection = mqtt.blockingConnection();
//      connection.connect();
//      final String TOPIC = "TopicA/";
//      connection.subscribe(new Topic[]{new Topic(TOPIC, QoS.EXACTLY_ONCE)});
//
//      // handleMessage non-retained messages
//      final int TOTAL_MESSAGES = 10;
//      for (int i = 0; i < TOTAL_MESSAGES; i++)
//      {
//         connection.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);
//      }
//
//      // receive half the messages in this session
//      for (int i = 0; i < TOTAL_MESSAGES / 2; i++)
//      {
//         final Message msg = connection.receive(1000, TimeUnit.MILLISECONDS);
//         assertNotNull(msg);
//         assertEquals(TOPIC, new String(msg.getPayload()));
//         msg.ack();
//      }
//
//      connection.disconnect();
//      // resume session
//      connection = mqtt.blockingConnection();
//      connection.connect();
//      // receive rest of the messages
//      Message msg = null;
//      do
//      {
//         msg = connection.receive(1000, TimeUnit.MILLISECONDS);
//         if (msg != null)
//         {
//            assertEquals(TOPIC, new String(msg.getPayload()));
//            msg.ack();
//         }
//      } while (msg != null);
//
//      // make sure we received all message ids
//      for (short id = 1; id <= TOTAL_MESSAGES; id++)
//      {
//         assertNotNull("No message for id " + id, publishMap.get(id));
//      }
//
//      connection.unsubscribe(new String[]{TOPIC});
//      connection.disconnect();
//   }

//   @Test(timeout = 90 * 1000)
//   public void testPacketIdGeneratorCleanSession() throws Exception
//   {
//      final String[] cleanClientIds = new String[]{"", "clean-packetid", null};
//      final Map<Short, PUBLISH> publishMap = new ConcurrentHashMap<Short, PUBLISH>();
//      MQTT[] mqtts = new MQTT[cleanClientIds.length];
//      for (int i = 0; i < cleanClientIds.length; i++)
//      {
//         mqtts[i] = createMQTTConnection("", true);
//         mqtts[i].setKeepAlive((short) 15);
//
//         mqtts[i].setTracer(new Tracer()
//         {
//            @Override
//            public void onReceive(MQTTFrame frame)
//            {
//               LOG.info("Client received:\n" + frame);
//               if (frame.messageType() == PUBLISH.TYPE)
//               {
//                  PUBLISH publish = new PUBLISH();
//                  try
//                  {
//                     publish.decode(frame);
//                     LOG.info("PUBLISH " + publish);
//                  }
//                  catch (ProtocolException e)
//                  {
//                     fail("Error decoding handleMessage " + e.getMessage());
//                  }
//                  if (publishMap.get(publish.messageId()) != null)
//                  {
//                     assertTrue(publish.dup());
//                  }
//                  publishMap.put(publish.messageId(), publish);
//               }
//            }
//
//            @Override
//            public void onSend(MQTTFrame frame)
//            {
//               LOG.info("Client sent:\n" + frame);
//            }
//         });
//      }
//
//      final Random random = new Random();
//      for (short i = 0; i < 10; i++)
//      {
//         BlockingConnection connection = mqtts[random.nextInt(cleanClientIds.length)].blockingConnection();
//         connection.connect();
//         final String TOPIC = "TopicA/";
//         connection.subscribe(new Topic[]{new Topic(TOPIC, QoS.EXACTLY_ONCE)});
//
//         // handleMessage non-retained message
//         connection.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);
//         Message msg = connection.receive(1000, TimeUnit.MILLISECONDS);
//         assertNotNull(msg);
//         assertEquals(TOPIC, new String(msg.getPayload()));
//         msg.ack();
//
//         assertEquals(1, publishMap.size());
//         final short id = (short) (i + 1);
//         assertNotNull("No message for id " + id, publishMap.get(id));
//         publishMap.clear();
//
//         connection.disconnect();
//      }
//
//   }

   @Test(timeout = 60 * 1000)
   public void testClientConnectionFailure() throws Exception
   {
      MQTT mqtt = createMQTTConnection("reconnect", false);
      final BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();
      Wait.waitFor(new Wait.Condition()
      {
         @Override
         public boolean isSatisified() throws Exception
         {
            return connection.isConnected();
         }
      });

      final String TOPIC = "TopicA";
      final byte[] qos = connection.subscribe(new Topic[]{new Topic(TOPIC, QoS.EXACTLY_ONCE)});
      assertEquals(QoS.EXACTLY_ONCE.ordinal(), qos[0]);
      connection.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);
      // kill transport
      connection.kill();

      final BlockingConnection newConnection = mqtt.blockingConnection();
      newConnection.connect();
      Wait.waitFor(new Wait.Condition()
      {
         @Override
         public boolean isSatisified() throws Exception
         {
            return newConnection.isConnected();
         }
      });

      assertEquals(QoS.EXACTLY_ONCE.ordinal(), qos[0]);
      Message msg = newConnection.receive(1000, TimeUnit.MILLISECONDS);
      assertNotNull(msg);
      assertEquals(TOPIC, new String(msg.getPayload()));
      msg.ack();
      newConnection.disconnect();
   }

//   @Test(timeout = 60 * 1000)
//   public void testCleanSession() throws Exception
//   {
//      final String CLIENTID = "cleansession";
//      final MQTT mqttNotClean = createMQTTConnection(CLIENTID, false);
//      BlockingConnection notClean = mqttNotClean.blockingConnection();
//      final String TOPIC = "TopicA";
//      notClean.connect();
//      notClean.subscribe(new Topic[]{new Topic(TOPIC, QoS.EXACTLY_ONCE)});
//      notClean.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);
//      notClean.disconnect();
//
//      // MUST receive message from previous not clean session
//      notClean = mqttNotClean.blockingConnection();
//      notClean.connect();
//      Message msg = notClean.receive(10000, TimeUnit.MILLISECONDS);
//      assertNotNull(msg);
//      assertEquals(TOPIC, new String(msg.getPayload()));
//      msg.ack();
//      notClean.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);
//      notClean.disconnect();
//
//      // MUST NOT receive message from previous not clean session
//      final MQTT mqttClean = createMQTTConnection(CLIENTID, true);
//      final BlockingConnection clean = mqttClean.blockingConnection();
//      clean.connect();
//      msg = clean.receive(10000, TimeUnit.MILLISECONDS);
//      assertNull(msg);
//      clean.subscribe(new Topic[]{new Topic(TOPIC, QoS.EXACTLY_ONCE)});
//      clean.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);
//      clean.disconnect();
//
//      // MUST NOT receive message from previous clean session
//      notClean = mqttNotClean.blockingConnection();
//      notClean.connect();
//      msg = notClean.receive(1000, TimeUnit.MILLISECONDS);
//      assertNull(msg);
//      notClean.disconnect();
//   }

   @Test(timeout = 60 * 1000)
   public void testPingKeepsInactivityMonitorAlive() throws Exception
   {
      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("foo");
      mqtt.setKeepAlive((short) 2);
      final BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();

      assertTrue("KeepAlive didn't work properly", Wait.waitFor(new Wait.Condition()
      {

         @Override
         public boolean isSatisified() throws Exception
         {
            return connection.isConnected();
         }
      }));

      connection.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testTurnOffInactivityMonitor() throws Exception
   {
      stopBroker();
      protocolConfig = "transport.useInactivityMonitor=false";
      startBroker();

      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("foo3");
      mqtt.setKeepAlive((short) 2);
      final BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();

      assertTrue("KeepAlive didn't work properly", Wait.waitFor(new Wait.Condition()
      {

         @Override
         public boolean isSatisified() throws Exception
         {
            return connection.isConnected();
         }
      }));

      connection.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testPublishDollarTopics() throws Exception
   {
      MQTT mqtt = createMQTTConnection();
      final String clientId = "publishDollar";
      mqtt.setClientId(clientId);
      mqtt.setKeepAlive((short) 2);
      BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();

      final String DOLLAR_TOPIC = "$TopicA";
      connection.subscribe(new Topic[]{new Topic(DOLLAR_TOPIC, QoS.EXACTLY_ONCE)});
      connection.publish(DOLLAR_TOPIC, DOLLAR_TOPIC.getBytes(), QoS.EXACTLY_ONCE, true);

      Message message = connection.receive(10, TimeUnit.SECONDS);
      assertNull("Publish enabled for $ Topics by default", message);
      connection.disconnect();

      stopBroker();
      protocolConfig = "transport.publishDollarTopics=true";
      startBroker();

      mqtt = createMQTTConnection();
      mqtt.setClientId(clientId);
      mqtt.setKeepAlive((short) 2);
      connection = mqtt.blockingConnection();
      connection.connect();

      connection.subscribe(new Topic[]{new Topic(DOLLAR_TOPIC, QoS.EXACTLY_ONCE)});
      connection.publish(DOLLAR_TOPIC, DOLLAR_TOPIC.getBytes(), QoS.EXACTLY_ONCE, true);

      message = connection.receive(10, TimeUnit.SECONDS);
      assertNotNull(message);
      message.ack();
      assertEquals("Message body", DOLLAR_TOPIC, new String(message.getPayload()));

      connection.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testDuplicateClientId() throws Exception
   {
      // test link stealing enabled by default
      final String clientId = "duplicateClient";
      MQTT mqtt = createMQTTConnection(clientId, false);
      mqtt.setVersion("3.1.1");
      mqtt.setKeepAlive((short) 2);
      final BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();
      final String TOPICA = "TopicA";
      connection.publish(TOPICA, TOPICA.getBytes(), QoS.EXACTLY_ONCE, true);

      MQTT mqtt1 = createMQTTConnection(clientId, false);
      mqtt1.setVersion("3.1.1");
      mqtt1.setKeepAlive((short) 2);
      final BlockingConnection connection1 = mqtt1.blockingConnection();
      connection1.connect();

      assertTrue("Duplicate client disconnected", Wait.waitFor(new Wait.Condition()
      {
         @Override
         public boolean isSatisified() throws Exception
         {
            return connection1.isConnected();
         }
      }));

      assertTrue("Old client still connected", Wait.waitFor(new Wait.Condition()
      {
         @Override
         public boolean isSatisified() throws Exception
         {
            return !connection.isConnected();
         }
      }));

      connection1.publish(TOPICA, TOPICA.getBytes(), QoS.EXACTLY_ONCE, true);
      connection1.disconnect();

      // disable link stealing
      stopBroker();
      protocolConfig = "allowLinkStealing=false";
      startBroker();

      mqtt = createMQTTConnection(clientId, false);
      mqtt.setKeepAlive((short) 2);
      final BlockingConnection connection2 = mqtt.blockingConnection();
      connection2.connect();
      connection2.publish(TOPICA, TOPICA.getBytes(), QoS.EXACTLY_ONCE, true);

      mqtt1 = createMQTTConnection(clientId, false);
      mqtt1.setKeepAlive((short) 2);
      final BlockingConnection connection3 = mqtt1.blockingConnection();
      try
      {
         connection3.connect();
         fail("Duplicate client connected");
      }
      catch (Exception e)
      {
         // ignore
      }

      assertTrue("Old client disconnected", connection2.isConnected());
      connection2.publish(TOPICA, TOPICA.getBytes(), QoS.EXACTLY_ONCE, true);
      connection2.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testRepeatedLinkStealing() throws Exception
   {
      final String clientId = "duplicateClient";
      final AtomicReference<BlockingConnection> oldConnection = new AtomicReference<BlockingConnection>();
      final String TOPICA = "TopicA";

      for (int i = 1; i <= 10; ++i)
      {

         LOG.info("Creating MQTT Connection {}", i);

         MQTT mqtt = createMQTTConnection(clientId, false);
         mqtt.setVersion("3.1.1");
         mqtt.setKeepAlive((short) 2);
         final BlockingConnection connection = mqtt.blockingConnection();
         connection.connect();
         connection.publish(TOPICA, TOPICA.getBytes(), QoS.EXACTLY_ONCE, true);

         assertTrue("Client connect failed for attempt: " + i, Wait.waitFor(new Wait.Condition()
         {
            @Override
            public boolean isSatisified() throws Exception
            {
               return connection.isConnected();
            }
         }));

         if (oldConnection.get() != null)
         {
            assertTrue("Old client still connected", Wait.waitFor(new Wait.Condition()
            {
               @Override
               public boolean isSatisified() throws Exception
               {
                  return !oldConnection.get().isConnected();
               }
            }));
         }

         oldConnection.set(connection);
      }

      oldConnection.get().publish(TOPICA, TOPICA.getBytes(), QoS.EXACTLY_ONCE, true);
      oldConnection.get().disconnect();
   }

//   @Test(timeout = 60 * 1000)
//   public void testReceiveMessageSentWhileOffline() throws Exception
//   {
//      final byte[] payload = new byte[1024 * 32];
//      for (int i = 0; i < payload.length; i++)
//      {
//         payload[i] = '2';
//      }
//
//      int numberOfRuns = 100;
//      int messagesPerRun = 2;
//
//      final MQTT mqttPub = createMQTTConnection("MQTT-Pub-Client", true);
//      final MQTT mqttSub = createMQTTConnection("MQTT-Sub-Client", false);
//
//      final BlockingConnection connectionPub = mqttPub.blockingConnection();
//      connectionPub.connect();
//
//      BlockingConnection connectionSub = mqttSub.blockingConnection();
//      connectionSub.connect();
//
//      Topic[] topics = {new Topic("TopicA", QoS.EXACTLY_ONCE)};
//      connectionSub.subscribe(topics);
//
//      for (int i = 0; i < messagesPerRun; ++i)
//      {
//         connectionPub.publish(topics[0].name().toString(), payload, QoS.AT_LEAST_ONCE, false);
//      }
//
//      int received = 0;
//      for (int i = 0; i < messagesPerRun; ++i)
//      {
//         Message message = connectionSub.receive(5, TimeUnit.SECONDS);
//         assertNotNull(message);
//         received++;
//         assertTrue(Arrays.equals(payload, message.getPayload()));
//         message.ack();
//      }
//      connectionSub.disconnect();
//
//      for (int j = 0; j < numberOfRuns; j++)
//      {
//
//         for (int i = 0; i < messagesPerRun; ++i)
//         {
//            connectionPub.publish(topics[0].name().toString(), payload, QoS.AT_LEAST_ONCE, false);
//         }
//
//         connectionSub = mqttSub.blockingConnection();
//         connectionSub.connect();
//         connectionSub.subscribe(topics);
//
//         for (int i = 0; i < messagesPerRun; ++i)
//         {
//            Message message = connectionSub.receive(5, TimeUnit.SECONDS);
//            assertNotNull(message);
//            received++;
//            assertTrue(Arrays.equals(payload, message.getPayload()));
//            message.ack();
//         }
//         connectionSub.disconnect();
//      }
//      assertEquals("Should have received " + (messagesPerRun * (numberOfRuns + 1)) + " messages", (messagesPerRun * (numberOfRuns + 1)), received);
//   }

//   @Test(timeout = 60 * 1000)
//   public void testReceiveMessageSentWhileOfflineAndBrokerRestart() throws Exception
//   {
//      stopBroker();
//      this.persistent = true;
//      startBroker();
//
//      final byte[] payload = new byte[1024 * 32];
//      for (int i = 0; i < payload.length; i++)
//      {
//         payload[i] = '2';
//      }
//
//      int messagesPerRun = 10;
//
//      Topic[] topics = {new Topic("TopicA", QoS.EXACTLY_ONCE)};
//
//      {
//         // Establish a durable subscription.
//         MQTT mqttSub = createMQTTConnection("MQTT-Sub-Client", false);
//         BlockingConnection connectionSub = mqttSub.blockingConnection();
//         connectionSub.connect();
//         connectionSub.subscribe(topics);
//         connectionSub.disconnect();
//      }
//
//      MQTT mqttPubLoop = createMQTTConnection("MQTT-Pub-Client", true);
//      BlockingConnection connectionPub = mqttPubLoop.blockingConnection();
//      connectionPub.connect();
//
//      for (int i = 0; i < messagesPerRun; ++i)
//      {
//         connectionPub.publish(topics[0].name().toString(), payload, QoS.AT_LEAST_ONCE, false);
//      }
//
//      connectionPub.disconnect();
//
//      stopBroker();
//      startBroker();
//
//      MQTT mqttSub = createMQTTConnection("MQTT-Sub-Client", false);
//      BlockingConnection connectionSub = mqttSub.blockingConnection();
//      connectionSub.connect();
//      connectionSub.subscribe(topics);
//
//      for (int i = 0; i < messagesPerRun; ++i)
//      {
//         Message message = connectionSub.receive(5, TimeUnit.SECONDS);
//         assertNotNull(message);
//         assertTrue(Arrays.equals(payload, message.getPayload()));
//         message.ack();
//      }
//      connectionSub.disconnect();
//   }

   @Test(timeout = 60 * 1000)
   public void testNoMessageReceivedAfterUnsubscribeMQTT() throws Exception
   {
      Topic[] topics = {new Topic("TopicA", QoS.EXACTLY_ONCE)};

      MQTT mqttPub = createMQTTConnection("MQTTPub-Client", true);
      mqttPub.setVersion("3.1.1");

      MQTT mqttSub = createMQTTConnection("MQTTSub-Client", false);
      mqttSub.setVersion("3.1.1");

      BlockingConnection connectionPub = mqttPub.blockingConnection();
      connectionPub.connect();

      BlockingConnection connectionSub = mqttSub.blockingConnection();
      connectionSub.connect();
      connectionSub.subscribe(topics);
      connectionSub.disconnect();

      for (int i = 0; i < 5; i++)
      {
         String payload = "Message " + i;
         connectionPub.publish(topics[0].name().toString(), payload.getBytes(), QoS.EXACTLY_ONCE, false);
         //FIXME REMOVE THIS SEE OTHERS
         Thread.sleep(50);
      }

      connectionSub = mqttSub.blockingConnection();
      connectionSub.connect();

      int received = 0;
      for (int i = 0; i < 5; ++i)
      {
         Message message = connectionSub.receive(5, TimeUnit.SECONDS);
         assertNotNull("Missing message " + i, message);
         LOG.info("Message is " + new String(message.getPayload()));
         received++;
         message.ack();
      }
      assertEquals(5, received);

      // unsubscribe from topic
      connectionSub.unsubscribe(new String[]{"TopicA"});

      // send more messages
      for (int i = 0; i < 5; i++)
      {
         String payload = "Message " + i;
         connectionPub.publish(topics[0].name().toString(), payload.getBytes(), QoS.EXACTLY_ONCE, false);
      }

      // these should not be received
      assertNull(connectionSub.receive(5, TimeUnit.SECONDS));

      connectionSub.disconnect();
      connectionPub.disconnect();
   }

}
