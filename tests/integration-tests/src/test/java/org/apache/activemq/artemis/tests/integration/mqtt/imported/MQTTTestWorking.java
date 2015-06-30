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

import java.lang.reflect.Field;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.core.protocol.mqtt.MQTTConnectionManager;
import org.apache.activemq.artemis.core.protocol.mqtt.MQTTSession;
import org.apache.activemq.artemis.tests.integration.client.ConcurrentCreateDeleteProduceTest;
import org.apache.activemq.artemis.tests.integration.mqtt.imported.util.Wait;
import org.apache.activemq.artemis.tests.unit.core.server.group.impl.SystemPropertyOverrideTest;
import org.apache.activemq.artemis.utils.ConcurrentHashSet;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.mqtt.client.Tracer;
import org.fusesource.mqtt.codec.MQTTFrame;
import org.fusesource.mqtt.codec.PUBLISH;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.hawtbuf.UTF8Buffer.utf8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MQTTTestWorking extends MQTTTestSupport
{
   private static final Logger LOG = LoggerFactory.getLogger(MQTTTest.class);

   private static final int NUM_MESSAGES = 250;

   @Before
   public void setUp() throws Exception
   {
      Field sessions = MQTTSession.class.getDeclaredField("SESSIONS");
      sessions.setAccessible(true);
      sessions.set(null, new ConcurrentHashMap<>());

      Field connectedClients = MQTTConnectionManager.class.getDeclaredField("CONNECTED_CLIENTS");
      connectedClients.setAccessible(true);
      connectedClients.set(null, new ConcurrentHashSet<>());
      super.setUp();

   }
   @Test(timeout = 60 * 1000)
   public void testSendAndReceiveMQTT() throws Exception
   {
      final MQTTClientProvider subscriptionProvider = getMQTTClientProvider();
      initializeConnection(subscriptionProvider);

      subscriptionProvider.subscribe("foo/bah", AT_MOST_ONCE);

      final CountDownLatch latch = new CountDownLatch(NUM_MESSAGES);

      Thread thread = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            for (int i = 0; i < NUM_MESSAGES; i++)
            {
               try
               {
                  byte[] payload = subscriptionProvider.receive(10000);
                  assertNotNull("Should get a message", payload);
                  latch.countDown();
               }
               catch (Exception e)
               {
                  e.printStackTrace();
                  break;
               }
            }
         }
      });
      thread.start();

      final MQTTClientProvider publishProvider = getMQTTClientProvider();
      initializeConnection(publishProvider);

      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         String payload = "Message " + i;
         publishProvider.publish("foo/bah", payload.getBytes(), AT_LEAST_ONCE);
      }

      latch.await(10, TimeUnit.SECONDS);
      assertEquals(0, latch.getCount());
      subscriptionProvider.disconnect();
      publishProvider.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testUnsubscribeMQTT() throws Exception
   {
      final MQTTClientProvider subscriptionProvider = getMQTTClientProvider();
      initializeConnection(subscriptionProvider);

      String topic = "foo/bah";

      subscriptionProvider.subscribe(topic, AT_MOST_ONCE);

      final CountDownLatch latch = new CountDownLatch(NUM_MESSAGES / 2);

      Thread thread = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            for (int i = 0; i < NUM_MESSAGES; i++)
            {
               try
               {
                  byte[] payload = subscriptionProvider.receive(10000);
                  assertNotNull("Should get a message", payload);
                  latch.countDown();
               }
               catch (Exception e)
               {
                  e.printStackTrace();
                  break;
               }

            }
         }
      });
      thread.start();

      final MQTTClientProvider publishProvider = getMQTTClientProvider();
      initializeConnection(publishProvider);

      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         String payload = "Message " + i;
         if (i == NUM_MESSAGES / 2)
         {
            subscriptionProvider.unsubscribe(topic);
         }
         publishProvider.publish(topic, payload.getBytes(), AT_LEAST_ONCE);
      }

      latch.await(20, TimeUnit.SECONDS);
      assertEquals(0, latch.getCount());
      subscriptionProvider.disconnect();
      publishProvider.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testSendAtMostOnceReceiveExactlyOnce() throws Exception
   {
      /**
       * Although subscribing with EXACTLY ONCE, the message gets published
       * with AT_MOST_ONCE - in MQTT the QoS is always determined by the
       * message as published - not the wish of the subscriber
       */
      final MQTTClientProvider provider = getMQTTClientProvider();
      initializeConnection(provider);
      provider.subscribe("foo", EXACTLY_ONCE);
      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         String payload = "Test Message: " + i;
         provider.publish("foo", payload.getBytes(), AT_MOST_ONCE);
         byte[] message = provider.receive(5000);
         assertNotNull("Should get a message", message);
         assertEquals(payload, new String(message));
      }
      provider.disconnect();
   }

   @Test(timeout = 2 * 60 * 1000)
   public void testSendAtLeastOnceReceiveExactlyOnce() throws Exception
   {
      final MQTTClientProvider provider = getMQTTClientProvider();
      initializeConnection(provider);
      provider.subscribe("foo", EXACTLY_ONCE);
      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         String payload = "Test Message: " + i;
         System.out.println("Test Sending: " + i);
         provider.publish("foo", payload.getBytes(), AT_LEAST_ONCE);
         System.out.println("TEST Sent " + i);
         byte[] message = provider.receive(5000);
         System.out.println("TEST Received " + i);
         assertNotNull("Should get a message", message);
         assertEquals(payload, new String(message));
      }
      provider.disconnect();
   }

   @Test(timeout = 2 * 60 * 1000)
   public void testSendAtLeastOnceReceiveAtMostOnce() throws Exception
   {
      final MQTTClientProvider provider = getMQTTClientProvider();
      initializeConnection(provider);
      provider.subscribe("foo", AT_MOST_ONCE);
      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         String payload = "Test Message: " + i;
         provider.publish("foo", payload.getBytes(), AT_LEAST_ONCE);
         byte[] message = provider.receive(5000);
         assertNotNull("Should get a message", message);
         assertEquals(payload, new String(message));
      }
      provider.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testSendAndReceiveAtMostOnce() throws Exception
   {
      final MQTTClientProvider provider = getMQTTClientProvider();
      initializeConnection(provider);
      provider.subscribe("foo", AT_MOST_ONCE);
      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         String payload = "Test Message: " + i;
         provider.publish("foo", payload.getBytes(), AT_MOST_ONCE);
         byte[] message = provider.receive(5000);
         assertNotNull("Should get a message", message);
         assertEquals(payload, new String(message));
      }
      provider.disconnect();
   }

   @Test(timeout = 2 * 60 * 1000)
   public void testSendAndReceiveAtLeastOnce() throws Exception
   {
      final MQTTClientProvider provider = getMQTTClientProvider();
      provider.setKeepAlive(40000);
      initializeConnection(provider);
      provider.subscribe("foo", AT_LEAST_ONCE);


      for (int j=1; j<10; j++)
      {
         long time = System.currentTimeMillis();
         for (int i = 0; i < 40000; i++)
         {
            String payload = "Test Message: " + i;
            provider.publish("foo", payload.getBytes(), AT_LEAST_ONCE);
            byte[] message = provider.receive(5000);

            //Thread.sleep(2000);
//            assertNotNull("Should get a message", message);
//            assertEquals(payload, new String(message));
         }
         System.out.println("Time: " + (System.currentTimeMillis() - time));
      }
      provider.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testSendAndReceiveExactlyOnce() throws Exception
   {
      final MQTTClientProvider publisher = getMQTTClientProvider();
      initializeConnection(publisher);

      final MQTTClientProvider subscriber = getMQTTClientProvider();
      initializeConnection(subscriber);

      subscriber.subscribe("foo", EXACTLY_ONCE);
      for (int i = 0; i < NUM_MESSAGES; i++)
      {
         String payload = "Test Message: " + i;
         publisher.publish("foo", payload.getBytes(), EXACTLY_ONCE);
         byte[] message = subscriber.receive(5000);
         assertNotNull("Should get a message + [" + i + "]", message);
         assertEquals(payload, new String(message));
      }
      subscriber.disconnect();
      publisher.disconnect();
   }


   @Test(timeout = 60 * 1000)
   public void testSendAndReceiveLargeMessages() throws Exception
   {
      // FIXME We do not support messages sizes > 1024 (the default netty buffer size)
      // Should be 1024 * 32
      byte[] payload = new byte[512 * 1];
      for (int i = 0; i < payload.length; i++)
      {
         payload[i] = '2';
      }
      final MQTTClientProvider publisher = getMQTTClientProvider();
      initializeConnection(publisher);

      final MQTTClientProvider subscriber = getMQTTClientProvider();
      initializeConnection(subscriber);

      subscriber.subscribe("foo", AT_LEAST_ONCE);
      for (int i = 0; i < 10; i++)
      {
         publisher.publish("foo", payload, AT_LEAST_ONCE);
         byte[] message = subscriber.receive(5000);
         assertNotNull("Should get a message", message);

         assertArrayEquals(payload, message);
      }
      subscriber.disconnect();
      publisher.disconnect();
   }

   @Test(timeout = 30 * 1000)
   public void testValidZeroLengthClientId() throws Exception
   {
      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("");
      mqtt.setCleanSession(true);

      BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();
      connection.disconnect();
   }

   @Test(timeout = 2 * 60 * 1000)
   public void testMQTTWildcard() throws Exception
   {
      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("");
      mqtt.setCleanSession(true);

      BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();

      Topic[] topics = {new Topic(utf8("a/#"), QoS.values()[AT_MOST_ONCE])};
      connection.subscribe(topics);
      String payload = "Test Message";
      String publishedTopic = "a/b/1.2.3*4";
      connection.publish(publishedTopic, payload.getBytes(), QoS.values()[AT_MOST_ONCE], false);

      Message msg = connection.receive(1, TimeUnit.SECONDS);
      assertEquals("Topic changed", publishedTopic, msg.getTopic());
   }

   @Test(timeout = 60 * 1000)
   public void testMQTT311Connection() throws Exception
   {
      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("foo");
      mqtt.setVersion("3.1.1");
      final BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();
      connection.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testPingOnMQTT() throws Exception
   {
      stopBroker();
      protocolConfig = "maxInactivityDuration=-1";
      startBroker();

      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("test-mqtt");
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

   @Test(timeout = 30 * 10000)
   public void testSubscribeMultipleTopics() throws Exception
   {

      byte[] payload = new byte[128];
      for (int i = 0; i < payload.length; i++)
      {
         payload[i] = '2';
      }

      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("MQTT-Client");
      mqtt.setCleanSession(false);

      final BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();

      Topic[] topics = {new Topic("Topic/A", QoS.EXACTLY_ONCE), new Topic("Topic/B", QoS.EXACTLY_ONCE)};
      Topic[] wildcardTopic = {new Topic("Topic/#", QoS.AT_LEAST_ONCE)};
      connection.subscribe(wildcardTopic);

      for (Topic topic : topics)
      {
         connection.publish(topic.name().toString(), payload, QoS.AT_LEAST_ONCE, false);
      }

      int received = 0;
      for (int i = 0; i < topics.length; ++i)
      {
         Message message = connection.receive();
         assertNotNull(message);
         received++;
         payload = message.getPayload();
         String messageContent = new String(payload);
         LOG.info("Received message from topic: " + message.getTopic() + " Message content: " + messageContent);
         message.ack();
      }

      assertEquals("Should have received " + topics.length + " messages", topics.length, received);
   }

   @Test(timeout = 60 * 1000)
   public void testSendAndReceiveRetainedMessages() throws Exception
   {
      final MQTTClientProvider publisher = getMQTTClientProvider();
      initializeConnection(publisher);

      final MQTTClientProvider subscriber = getMQTTClientProvider();
      initializeConnection(subscriber);

      String RETAINED = "retained";
      publisher.publish("foo", RETAINED.getBytes(), AT_LEAST_ONCE, true);

      List<String> messages = new ArrayList<String>();
      for (int i = 0; i < 10; i++)
      {
         messages.add("TEST MESSAGE:" + i);
      }

      subscriber.subscribe("foo", AT_LEAST_ONCE);

      for (int i = 0; i < 10; i++)
      {
         publisher.publish("foo", messages.get(i).getBytes(), AT_LEAST_ONCE);
      }
      byte[] msg = subscriber.receive(5000);
      assertNotNull(msg);
      assertEquals(RETAINED, new String(msg));

      for (int i = 0; i < 10; i++)
      {
         msg = subscriber.receive(5000);
         assertNotNull(msg);
         assertEquals(messages.get(i), new String(msg));
      }

      subscriber.disconnect();
      publisher.publish("foo", "retained2".getBytes(), AT_LEAST_ONCE, true);
      initializeConnection(subscriber);
      subscriber.subscribe("foo", AT_LEAST_ONCE);
      msg = subscriber.receive(2000);
      assertEquals("retained2", new String(msg));
      subscriber.disconnect();
      Thread.sleep(100);
      publisher.disconnect();
   }

   @Test(timeout = 2 * 60 * 1000)
   public void testMQTTPathPatterns() throws Exception
   {
      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("");
      mqtt.setCleanSession(true);

      BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();

      final String RETAINED = "RETAINED";
      String[] topics = {"TopicA", "/TopicA", "/", "TopicA/", "//"};
      for (String topic : topics)
      {
         // test retained message
         connection.publish(topic, (RETAINED + topic).getBytes(), QoS.AT_LEAST_ONCE, true);

         connection.subscribe(new Topic[]{new Topic(topic, QoS.AT_LEAST_ONCE)});
         Message msg = connection.receive(5, TimeUnit.SECONDS);
         assertNotNull("No message for " + topic, msg);
         assertEquals(RETAINED + topic, new String(msg.getPayload()));
         msg.ack();
         Thread.sleep(100);

         // test non-retained message
         connection.publish(topic, topic.getBytes(), QoS.AT_LEAST_ONCE, false);
         msg = connection.receive(1000, TimeUnit.MILLISECONDS);
         assertNotNull(msg);
         assertEquals(topic, new String(msg.getPayload()));
         msg.ack();
         Thread.sleep(100);

         connection.unsubscribe(new String[]{topic});
      }
      connection.disconnect();

      // test wildcard patterns with above topics
      String[] wildcards = {"#", "+", "+/#", "/+", "+/", "+/+", "+/+/", "+/+/+"};
      for (String wildcard : wildcards)
      {
         final Pattern pattern = Pattern.compile(wildcard.replaceAll("/?#", "(/?.*)*").replaceAll("\\+", "[^/]*"));

         connection = mqtt.blockingConnection();
         connection.connect();
         final byte[] qos = connection.subscribe(new Topic[]{new Topic(wildcard, QoS.AT_LEAST_ONCE)});
         assertNotEquals("Subscribe failed " + wildcard, (byte) 0x80, qos[0]);

         // test retained messages
         Message msg = connection.receive(5, TimeUnit.SECONDS);
         do
         {
            assertNotNull("RETAINED null " + wildcard, msg);
            assertTrue("RETAINED prefix " + wildcard, new String(msg.getPayload()).startsWith(RETAINED));
            assertTrue("RETAINED matching " + wildcard + " " + msg.getTopic(), pattern.matcher(msg.getTopic()).matches());
            msg.ack();
            Thread.sleep(100);
            msg = connection.receive(500, TimeUnit.MILLISECONDS);
         } while (msg != null);

         // test non-retained message
         for (String topic : topics)
         {
            connection.publish(topic, topic.getBytes(), QoS.AT_LEAST_ONCE, false);
            Thread.sleep(100);
         }
         msg = connection.receive(1000, TimeUnit.MILLISECONDS);
         do
         {
            assertNotNull("Non-retained Null " + wildcard, msg);
            assertTrue("Non-retained matching " + wildcard + " " + msg.getTopic(), pattern.matcher(msg.getTopic()).matches());
            msg.ack();
            Thread.sleep(100);
            msg = connection.receive(500, TimeUnit.MILLISECONDS);
         } while (msg != null);

         connection.unsubscribe(new String[]{wildcard});
         connection.disconnect();
      }
   }

   @Test(timeout = 60 * 1000)
   public void testMQTTRetainQoS() throws Exception
   {
      String[] topics = {"AT_MOST_ONCE", "AT_LEAST_ONCE", "EXACTLY_ONCE"};
      for (int i = 0; i < topics.length; i++)
      {
         final String topic = topics[i];

         MQTT mqtt = createMQTTConnection();
         mqtt.setClientId("foo");
         mqtt.setKeepAlive((short) 2);

         final int[] actualQoS = {-1};
         mqtt.setTracer(new Tracer()
         {
            @Override
            public void onReceive(MQTTFrame frame)
            {
               // validate the QoS
               if (frame.messageType() == PUBLISH.TYPE)
               {
                  actualQoS[0] = frame.qos().ordinal();
               }
            }
         });

         final BlockingConnection connection = mqtt.blockingConnection();
         connection.connect();
         connection.publish(topic, topic.getBytes(), QoS.EXACTLY_ONCE, true);
         connection.subscribe(new Topic[]{new Topic(topic, QoS.valueOf(topic))});

         final Message msg = connection.receive(5000, TimeUnit.MILLISECONDS);
         assertNotNull(msg);
         assertEquals(topic, new String(msg.getPayload()));
         int waitCount = 0;
         while (actualQoS[0] == -1 && waitCount < 10)
         {
            Thread.sleep(1000);
            waitCount++;
         }
         assertEquals(i, actualQoS[0]);
         msg.ack();
         Thread.sleep(100);

         connection.unsubscribe(new String[]{topic});
         connection.disconnect();
      }
   }

   @Test(timeout = 120 * 1000)
   public void testRetainedMessage() throws Exception
   {
      MQTT mqtt = createMQTTConnection();
      mqtt.setKeepAlive((short) 60);

      final String RETAIN = "RETAIN";
      final String TOPICA = "TopicA";

      final String[] clientIds = {null, "foo", "durable"};
      for (String clientId : clientIds)
      {
         LOG.info("Testing now with Client ID: {}", clientId);

         mqtt.setClientId(clientId);
         mqtt.setCleanSession(!"durable".equals(clientId));

         BlockingConnection connection = mqtt.blockingConnection();
         connection.connect();

         // set retained message and check
         connection.publish(TOPICA, RETAIN.getBytes(), QoS.EXACTLY_ONCE, true);
         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
         Message msg = connection.receive(5000, TimeUnit.MILLISECONDS);
         assertNotNull("No retained message for " + clientId, msg);
         assertEquals(RETAIN, new String(msg.getPayload()));
         msg.ack();
         //Thread.sleep(100);
         assertNull(connection.receive(100, TimeUnit.MILLISECONDS));

         // test duplicate subscription
         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
         msg = connection.receive(15000, TimeUnit.MILLISECONDS);
         assertNotNull("No retained message on duplicate subscription for " + clientId, msg);
         assertEquals(RETAIN, new String(msg.getPayload()));
         msg.ack();
         //Thread.sleep(100);
         assertNull(connection.receive(100, TimeUnit.MILLISECONDS));
         try
         {
            connection.unsubscribe(new String[]{TOPICA});
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }

         // clear retained message and check that we don't receive it
         connection.publish(TOPICA, "".getBytes(), QoS.AT_MOST_ONCE, true);
         Thread.sleep(2000);
         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
         msg = connection.receive(500, TimeUnit.MILLISECONDS);
         assertNull("Retained message not cleared for " + clientId, msg);
         connection.unsubscribe(new String[]{TOPICA});

         // set retained message again and check
         connection.publish(TOPICA, RETAIN.getBytes(), QoS.EXACTLY_ONCE, true);
         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
         msg = connection.receive(5000, TimeUnit.MILLISECONDS);
         assertNotNull("No reset retained message for " + clientId, msg);
         assertEquals(RETAIN, new String(msg.getPayload()));
         msg.ack();
         Thread.sleep(100);
         assertNull(connection.receive(500, TimeUnit.MILLISECONDS));

         // re-connect and check
         connection.disconnect();
         connection = mqtt.blockingConnection();
         connection.connect();
         connection.subscribe(new Topic[]{new Topic(TOPICA, QoS.AT_LEAST_ONCE)});
         msg = connection.receive(5000, TimeUnit.MILLISECONDS);
         assertNotNull("No reset retained message for " + clientId, msg);
         assertEquals(RETAIN, new String(msg.getPayload()));
         msg.ack();
         Thread.sleep(100);
         assertNull(connection.receive(500, TimeUnit.MILLISECONDS));

         connection.unsubscribe(new String[]{TOPICA});
         connection.disconnect();
      }
   }

   @Test(timeout = 60 * 1000)
   public void testReuseConnection() throws Exception
   {
      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("Test-Client");

      {
         BlockingConnection connection = mqtt.blockingConnection();
         connection.connect();
         connection.disconnect();
         Thread.sleep(1000);
      }
      {
         BlockingConnection connection = mqtt.blockingConnection();
         connection.connect();
         connection.disconnect();
         Thread.sleep(1000);
      }
   }

   @Test(timeout = 30 * 1000)
   public void testDefaultKeepAliveWhenClientSpecifiesZero() throws Exception
   {
      stopBroker();
      protocolConfig = "transport.defaultKeepAlive=2000";
      startBroker();

      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("foo");
      mqtt.setKeepAlive((short) 0);
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
   }

   @Test(timeout = 60 * 1000)
   public void testUniqueMessageIds() throws Exception
   {
      MQTT mqtt = createMQTTConnection();
      mqtt.setClientId("foo");
      mqtt.setKeepAlive((short) 200);
      mqtt.setCleanSession(true);

      final List<PUBLISH> publishList = new ArrayList<PUBLISH>();
      mqtt.setTracer(new Tracer()
      {
         @Override
         public void onReceive(MQTTFrame frame)
         {
            LOG.info("Client received:\n" + frame);
            if (frame.messageType() == PUBLISH.TYPE)
            {
               PUBLISH publish = new PUBLISH();
               try
               {
                  publish.decode(frame);
               }
               catch (ProtocolException e)
               {
                  fail("Error decoding handleMessage " + e.getMessage());
               }
               publishList.add(publish);
            }
         }

         @Override
         public void onSend(MQTTFrame frame)
         {
            LOG.info("Client sent:\n" + frame);
         }
      });

      final BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();

      // create overlapping subscriptions with different QoSs
      QoS[] qoss = {QoS.AT_MOST_ONCE, QoS.AT_LEAST_ONCE, QoS.EXACTLY_ONCE};
      final String TOPIC = "TopicA/";

      // handleMessage retained message
      connection.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, true);

      String[] subs = {TOPIC, "TopicA/#", "TopicA/+"};
      for (int i = 0; i < qoss.length; i++)
      {
         connection.subscribe(new Topic[]{new Topic(subs[i], qoss[i])});
      }

      // handleMessage non-retained message
      connection.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);
      int received = 0;

      Message msg = connection.receive(5000, TimeUnit.MILLISECONDS);
      do
      {
         assertNotNull(msg);
         assertEquals(TOPIC, new String(msg.getPayload()));
         Thread.sleep(100);
         msg.ack();
         int waitCount = 0;
         while (publishList.size() <= received && waitCount < 10)
         {
            Thread.sleep(1000);
            waitCount++;
         }
         msg = connection.receive(5000, TimeUnit.MILLISECONDS);
      } while (msg != null && received++ < subs.length * 2);
      assertEquals("Unexpected number of messages", subs.length * 2, received + 1);

      // make sure we received distinct ids for QoS != AT_MOST_ONCE, and 0 for
      // AT_MOST_ONCE
      for (int i = 0; i < publishList.size(); i++)
      {
         for (int j = i + 1; j < publishList.size(); j++)
         {
            final PUBLISH publish1 = publishList.get(i);
            final PUBLISH publish2 = publishList.get(j);
            boolean qos0 = false;
            if (publish1.qos() == QoS.AT_MOST_ONCE)
            {
               qos0 = true;
               assertEquals(0, publish1.messageId());
            }
            if (publish2.qos() == QoS.AT_MOST_ONCE)
            {
               qos0 = true;
               assertEquals(0, publish2.messageId());
            }
            if (!qos0)
            {
               assertNotEquals(publish1.messageId(), publish2.messageId());
            }
         }
      }

      connection.unsubscribe(subs);
      connection.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testResendMessageId() throws Exception
   {
      final MQTT mqtt = createMQTTConnection("resend", false);
      mqtt.setKeepAlive((short) 5);

      final List<PUBLISH> publishList = new ArrayList<PUBLISH>();
      mqtt.setTracer(new Tracer()
      {
         @Override
         public void onReceive(MQTTFrame frame)
         {
            LOG.info("Client received:\n" + frame);
            if (frame.messageType() == PUBLISH.TYPE)
            {
               PUBLISH publish = new PUBLISH();
               try
               {
                  publish.decode(frame);
               }
               catch (ProtocolException e)
               {
                  fail("Error decoding handleMessage " + e.getMessage());
               }
               publishList.add(publish);
            }
         }

         @Override
         public void onSend(MQTTFrame frame)
         {
            LOG.info("Client sent:\n" + frame);
         }
      });

      BlockingConnection connection = mqtt.blockingConnection();
      connection.connect();
      final String TOPIC = "TopicA/";
      final String[] topics = new String[]{TOPIC, "TopicA/+"};
      connection.subscribe(new Topic[]{new Topic(topics[0], QoS.AT_LEAST_ONCE), new Topic(topics[1], QoS.EXACTLY_ONCE)});

      // handleMessage non-retained message
      connection.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);

      Wait.waitFor(new Wait.Condition()
      {
         @Override
         public boolean isSatisified() throws Exception
         {
            return publishList.size() == 2;
         }
      }, 5000);
      assertEquals(2, publishList.size());

      connection.disconnect();

      connection = mqtt.blockingConnection();
      connection.connect();

      Wait.waitFor(new Wait.Condition()
      {
         @Override
         public boolean isSatisified() throws Exception
         {
            return publishList.size() == 4;
         }
      }, 30000);
      assertEquals(4, publishList.size());

      // make sure we received duplicate message ids
      assertTrue(publishList.get(0).messageId() == publishList.get(2).messageId() || publishList.get(0).messageId() == publishList.get(3).messageId());
      assertTrue(publishList.get(1).messageId() == publishList.get(3).messageId() || publishList.get(1).messageId() == publishList.get(2).messageId());
      assertTrue(publishList.get(2).dup() && publishList.get(3).dup());

      connection.unsubscribe(topics);
      connection.disconnect();
   }

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
         Thread.sleep(200);
         //FIXME REMOVE THIS SEE OTHERS
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
         Thread.sleep(200);
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
         Thread.sleep(200);
      }

      // these should not be received
      assertNull(connectionSub.receive(5, TimeUnit.SECONDS));

      connectionSub.disconnect();
      connectionPub.disconnect();
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
      connection.disconnect();

      mqtt = createMQTTConnection();
      mqtt.setClientId(clientId);
      mqtt.setKeepAlive((short) 2);
      connection = mqtt.blockingConnection();
      connection.connect();

      connection.subscribe(new Topic[]{new Topic(DOLLAR_TOPIC, QoS.EXACTLY_ONCE)});
      connection.publish(DOLLAR_TOPIC, DOLLAR_TOPIC.getBytes(), QoS.EXACTLY_ONCE, true);

      Message message = connection.receive(10, TimeUnit.SECONDS);
      assertNotNull(message);
      message.ack();
      assertEquals("Message body", DOLLAR_TOPIC, new String(message.getPayload()));

      connection.disconnect();
   }

   @Test(timeout = 60 * 1000)
   public void testCleanSession() throws Exception
   {
      final String CLIENTID = "cleansession";
      final MQTT mqttNotClean = createMQTTConnection(CLIENTID, false);
      BlockingConnection notClean = mqttNotClean.blockingConnection();
      final String TOPIC = "TopicA";
      notClean.connect();
      notClean.subscribe(new Topic[]{new Topic(TOPIC, QoS.EXACTLY_ONCE)});
      notClean.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);
      notClean.disconnect();

      // MUST receive message from previous not clean session
      notClean = mqttNotClean.blockingConnection();
      notClean.connect();
      Message msg = notClean.receive(2000, TimeUnit.MILLISECONDS);
      assertNotNull(msg);
      assertEquals(TOPIC, new String(msg.getPayload()));
      msg.ack();
      Thread.sleep(3000);
      notClean.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);

      notClean.disconnect();

      // MUST NOT receive message from previous not clean session
      final MQTT mqttClean = createMQTTConnection(CLIENTID, true);
      final BlockingConnection clean = mqttClean.blockingConnection();
      clean.connect();
      msg = clean.receive(2000, TimeUnit.MILLISECONDS);
      assertNull(msg);
      clean.subscribe(new Topic[]{new Topic(TOPIC, QoS.EXACTLY_ONCE)});
      clean.publish(TOPIC, TOPIC.getBytes(), QoS.EXACTLY_ONCE, false);
      clean.disconnect();

      // MUST NOT receive message from previous clean session
      notClean = mqttNotClean.blockingConnection();
      notClean.connect();
      msg = notClean.receive(1000, TimeUnit.MILLISECONDS);
      assertNull(msg);
      notClean.disconnect();
   }

   @Test(timeout = 120 * 1000)
   public void testClientConnectionFailure() throws Exception
   {
      MQTT mqtt = createMQTTConnection("reconnect", false);
      mqtt.setKeepAlive((short) 2);
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

      Thread.sleep(5000);
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
}
