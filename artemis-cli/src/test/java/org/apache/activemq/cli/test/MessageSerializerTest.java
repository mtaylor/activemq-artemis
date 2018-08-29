/*
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
package org.apache.activemq.cli.test;

import javax.jms.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//import com.sun.org.apache.xalan.internal.xsltc.dom.SimpleResultTreeImpl;
//import junit.framework.TestListener;
//import junit.framework.TestListener;
import org.apache.activemq.artemis.cli.Artemis;
import org.apache.activemq.artemis.cli.commands.Run;
import org.apache.activemq.artemis.jlibaio.LibaioContext;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.utils.RandomUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test to validate that the CLI doesn't throw improper exceptions when invoked.
 */
public class MessageSerializerTest extends CliTestBase {

   private Connection connection;

   @Before
   @Override
   public void setup() throws Exception {
      setupAuth();
      super.setup();
      startServer();
      ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
      connection = cf.createConnection("admin", "admin");
   }

   @After
   @Override
   public void tearDown() throws Exception {
      try {
         connection.close();
      } finally {
         stopServer();
         super.tearDown();
      }
   }

   private void setupAuth() throws Exception {
      setupAuth(temporaryFolder.getRoot());
   }

   private void setupAuth(File folder) throws Exception {
      System.setProperty("java.security.auth.login.config", folder.getAbsolutePath() + "/etc/login.config");
   }

   private void startServer() throws Exception {
      File rootDirectory = new File(temporaryFolder.getRoot(), "broker");
      setupAuth(rootDirectory);
      Run.setEmbedded(true);
      Artemis.main("create", rootDirectory.getAbsolutePath(), "--silent", "--no-fsync", "--no-autotune", "--no-web", "--require-login");
      System.setProperty("artemis.instance", rootDirectory.getAbsolutePath());
      Artemis.internalExecute("run");
   }

   private void stopServer() throws Exception {
      Artemis.internalExecute("stop");
      assertTrue(Run.latchRunning.await(5, TimeUnit.SECONDS));
      assertEquals(0, LibaioContext.getTotalMaxIO());
   }

   private File createMessageFile() throws IOException {
      return temporaryFolder.newFile("messages.xml");
   }

   @Test
   public void testTextMessageImportExport() throws Exception {
      String address = "test";
      int noMessages = 10;
      File file = createMessageFile();

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      connection.start();

      List<Message> sent = new ArrayList<>(noMessages);
      for (int i = 0; i < noMessages; i++) {
         sent.add(session.createTextMessage(RandomUtil.randomString()));
      }

      sendMessages(session, address, sent);
      exportMessages(address, noMessages, file);

      // Ensure there's nothing left to consume
      MessageConsumer consumer = session.createConsumer(getDestination(address));
      assertNull(consumer.receive(1000));
      consumer.close();

      importMessages(address, file);

      List<Message> received = consumeMessages(session, address, noMessages, false);
      for (int i = 0; i < noMessages; i++) {
         assertEquals(((TextMessage) sent.get(i)).getText(), ((TextMessage) received.get(i)).getText());
      }
   }

   @Test
   public void testObjectMessageImportExport() throws Exception {
      String address = "test";
      int noMessages = 10;
      File file = createMessageFile();

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      connection.start();

      // Send initial messages.
      List<Message> sent = new ArrayList<>(noMessages);
      for (int i = 0; i < noMessages; i++) {
         sent.add(session.createObjectMessage(UUID.randomUUID()));
      }

      sendMessages(session, address, sent);
      exportMessages(address, noMessages, file);

      // Ensure there's nothing left to consume
      MessageConsumer consumer = session.createConsumer(getDestination(address));
      assertNull(consumer.receive(1000));
      consumer.close();

      importMessages(address, file);
      List<Message> received = consumeMessages(session, address, noMessages, false);
      for (int i = 0; i < noMessages; i++) {
         assertEquals(((ObjectMessage) sent.get(i)).getObject(), ((ObjectMessage) received.get(i)).getObject());
      }
   }

   @Test
   public void testMapMessageImportExport() throws Exception {
      String address = "test";
      int noMessages = 10;
      String key = "testKey";
      File file = createMessageFile();

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      connection.start();

      List<Message> sent = new ArrayList<>(noMessages);
      for (int i = 0; i < noMessages; i++) {
         MapMessage m = session.createMapMessage();
         m.setString(key, RandomUtil.randomString());
         sent.add(m);
      }

      sendMessages(session, address, sent);
      exportMessages(address, noMessages, file);

      // Ensure there's nothing left to consume
      MessageConsumer consumer = session.createConsumer(getDestination(address));
      assertNull(consumer.receive(1000));
      consumer.close();

      importMessages(address, file);
      List<Message> received = consumeMessages(session, address, noMessages, false);
      for (int i = 0; i < noMessages; i++) {
         assertEquals(((MapMessage) sent.get(i)).getString(key), ((MapMessage) received.get(i)).getString(key));
      }
   }

   private void sendMessages(Session session, String address, List<Message> messages) throws Exception {
      MessageProducer producer = session.createProducer(getDestination(address));
      for (Message m : messages) {
         producer.send(m);
      }
   }

   private void sendMessages(Session session, Destination destination, List<Message> messages) throws Exception {
      MessageProducer producer = session.createProducer(destination);
      for (Message m : messages) {
         producer.send(m);
      }
   }

   private List<Message> consumeMessages(Session session, String address, int noMessages, boolean fqqn) throws Exception {
      Destination destination = fqqn ? session.createQueue(address) : getDestination(address);
      MessageConsumer consumer = session.createConsumer(destination);

      List<Message> messages = new ArrayList<>();
      for (int i = 0; i < noMessages; i++) {
         Message m = consumer.receive(1000);
         assertNotNull(m);
         messages.add(m);
      }
      return messages;
   }

   private void exportMessages(String address, int noMessages, File output) throws Exception {
      Artemis.main("consumer",
                   "--user", "admin",
                   "--password", "admin",
                   "--destination", address,
                   "--message-count", "" + noMessages,
                   "--data", output.getAbsolutePath());
   }

   private void importMessages(String address, File input) throws Exception {
      Artemis.main("producer",
                   "--user", "admin",
                   "--password", "admin",
                   "--destination", address,
                   "--data", input.getAbsolutePath());
   }

   private void createQueue(String routingTypeOption, String address, String queueName) throws Exception {
      Artemis.main("queue", "create",
                   "--user", "admin",
                   "--password", "admin",
                   "--address", address,
                   "--name", queueName,
                   routingTypeOption,
                   "--durable",
                   "--preserve-on-no-consumers",
                   "--auto-create-address");
   }

   private void createBothTypeAddress(String address) throws Exception {
      Artemis.main("address", "create",
                   "--user", "admin",
                   "--password", "admin",
                   "--name", address,
                   "--anycast", "--multicast");
   }

   @Test
   public void testSendDirectToQueue() throws Exception {

      String address = "test";
      String queue1Name = "queue1";
      String queue2Name = "queue2";

      createQueue("--multicast", address, queue1Name);
      createQueue("--multicast", address, queue2Name);

      try (ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616"); Connection connection = cf.createConnection("admin", "admin");) {

         // send messages to queue
         Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         connection.start();

         Destination queue1 = session.createQueue(address + "::" + queue1Name);
         Destination queue2 = session.createQueue(address + "::" + queue2Name);

         MessageConsumer consumer1 = session.createConsumer(queue1);
         MessageConsumer consumer2 = session.createConsumer(queue2);

         Artemis.main("producer",
                      "--user", "admin",
                      "--password", "admin",
                      "--destination", "fqqn://" + address + "::" + queue1Name,
                      "--message-count", "5");

         assertNull(consumer2.receive(1000));
         assertNotNull(consumer1.receive(1000));
      }
   }

   @Test
   public void exportFromFQQN() throws Exception {
      String addr = "address";
      String queue = "queue";
      String fqqn = addr + "::" + queue;
      String destination = "fqqn://" + fqqn;

      File file = createMessageFile();
      int noMessages = 10;

      createQueue("--multicast", addr, queue);

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      connection.start();

      Topic topic = session.createTopic(addr);

      List<Message> messages = new ArrayList<>(noMessages);
      for (int i = 0; i < noMessages; i++) {
         messages.add(session.createTextMessage(RandomUtil.randomString()));
      }

      sendMessages(session, topic, messages);

      exportMessages(destination, noMessages, file);
      importMessages(destination, file);

      List<Message> recieved = consumeMessages(session, fqqn, noMessages, true);
      for (int i = 0; i < noMessages; i++) {
         assertEquals(((TextMessage) messages.get(i)).getText(), ((TextMessage) recieved.get(i)).getText());
      }
   }

   @Test
   public void testAnycastToMulticastTopic() throws Exception {
      String mAddress = "testMulticast";
      String aAddress = "testAnycast";
      String queueM1Name = "queueM1";
      String queueM2Name = "queueM2";

      File file = createMessageFile();
      int noMessages = 10;

      createQueue("--multicast", mAddress, queueM1Name);
      createQueue("--multicast", mAddress, queueM2Name);

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      connection.start();

      List<Message> messages = new ArrayList<>(noMessages);
      for (int i = 0; i < noMessages; i++) {
         messages.add(session.createTextMessage(RandomUtil.randomString()));
      }

      sendMessages(session, aAddress, messages);

      exportMessages(aAddress, noMessages, file);
      importMessages("topic://" + mAddress, file);

      List<Message> received;

      received = consumeMessages(session, queueM1Name, noMessages, false);
      for (int i = 0; i < noMessages; i++) {
         assertEquals(((TextMessage) messages.get(i)).getText(), ((TextMessage) received.get(i)).getText());
      }

      received = consumeMessages(session, queueM2Name, noMessages, false);
      for (int i = 0; i < noMessages; i++) {
         assertEquals(((TextMessage) messages.get(i)).getText(), ((TextMessage) received.get(i)).getText());
      }
   }

   @Test
   public void testAnycastToMulticastFQQN() throws Exception {
      String mAddress = "testMulticast";
      String aAddress = "testAnycast";
      String queueM1Name = "queueM1";
      String queueM2Name = "queueM2";
      String fqqnMulticast1 = mAddress + "::" + queueM1Name;
      String fqqnMulticast2 = mAddress + "::" + queueM2Name;

      File file = createMessageFile();
      int noMessages = 10;

      createQueue("--multicast", mAddress, queueM1Name);
      createQueue("--multicast", mAddress, queueM2Name);

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      connection.start();

      List<Message> messages = new ArrayList<>(noMessages);
      for (int i = 0; i < noMessages; i++) {
         messages.add(session.createTextMessage(RandomUtil.randomString()));
      }

      sendMessages(session, aAddress, messages);

      exportMessages(aAddress, noMessages, file);
      importMessages("fqqn://" + fqqnMulticast1, file);

      List<Message> received = consumeMessages(session, fqqnMulticast1, noMessages, true);
      for (int i = 0; i < noMessages; i++) {
         assertEquals(((TextMessage) messages.get(i)).getText(), ((TextMessage) received.get(i)).getText());
      }
      MessageConsumer consumer = session.createConsumer(getDestination(fqqnMulticast2));
      assertNull(consumer.receive(1000));
   }

   @Test
   public void testMulticastTopicToAnycastQueueBothAddress() throws Exception {
      String address = "testBoth";
//      String qName = "Name";

      File file = createMessageFile();
      int noMessages = 10;


      connection.setClientID("durable-client");
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      createBothTypeAddress(address);
      Topic topic = session.createTopic(address);
//      MessageProducer messageProducer = session.createProducer(topic);
//      MessageConsumer consumer = session.createConsumer(topic);
      TopicSubscriber subscriber = session.createDurableSubscriber(topic, "subscriber-1");

//      consumer.
//      TestListener l1 = new TestListener(noMessages);
//      consumer.setMessageListener(l1);
      connection.start();
      subscriber.close();
//      session.un
//      Topic topic = session.createTopic(address);
//      consumer.close();


//      createQueue("--multicast", address, qName);

//      Thread export = new Thread(() -> {
//         try {
//            exportMessages("topic://" + address, noMessages, file);
//         } catch (Exception e) {
//            e.printStackTrace();
//         }
//      });

//      export.start();

      List<Message> messages = new ArrayList<>(noMessages);
      for (int i = 0; i < noMessages; i++) {
         messages.add(session.createTextMessage(RandomUtil.randomString()));
      }

//       wait for the listener, TODO: replace sleep to waiting loop
//      TimeUnit.SECONDS.sleep(3);


//      consumer.receiveNoWait();

      sendMessages(session, topic, messages);
//      exportMessages("topic://" + address, noMessages, file);
      System.out.println("HERE");
      System.out.println(topic.toString());
//      System.out.println(subscriber.getTopic().getTopicName());
      System.out.println("-----");
//      System.out.println(subscriber.receive());
      exportMessages("fqqn://testBoth::durable-client.subscription-1", noMessages, file);
//      export.join();

      importMessages(address, file);

      List<Message> received = consumeMessages(session, address, noMessages, false);
      for (int i = 0; i < noMessages; i++) {
         assertEquals(((TextMessage) messages.get(i)).getText(), ((TextMessage) received.get(i)).getText());
      }
   }

   @Test
   public void testAnycastQueueToMulticastTopicBothAddress() throws Exception {
      String address = "testBoth";

      File file = createMessageFile();
      int noMessages = 10;

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      connection.start();

      createBothTypeAddress(address);

      List<Message> messages = new ArrayList<>(noMessages);
      for (int i = 0; i < noMessages; i++) {
         messages.add(session.createTextMessage(RandomUtil.randomString()));
      }

      sendMessages(session, getDestination(address), messages);

      exportMessages(address, noMessages, file);

      Thread receive = new Thread(() -> {
         try {
            MessageConsumer consumer = session.createConsumer(getTopicDestination(address));
            assertNotNull(consumer.receive(3000));
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
      receive.start();

      // wait for the listener, TODO: replace sleep to waiting loop
      TimeUnit.SECONDS.sleep(1);

      importMessages("topic://" + address, file);
      receive.join();
   }

   //read individual lines from byteStream
   private ArrayList<String> getOutputLines(TestActionContext context, boolean errorOutput) throws IOException {
      byte[] bytes;

      if (errorOutput) {
         bytes = context.getStdErrBytes();
      } else {
         bytes = context.getStdoutBytes();
      }
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
      ArrayList<String> lines = new ArrayList<>();

      String currentLine = bufferedReader.readLine();
      while (currentLine != null) {
         lines.add(currentLine);
         currentLine = bufferedReader.readLine();
      }

      return lines;
   }

   private void sendMessages(Session session, String queueName, int messageCount) throws JMSException {
      MessageProducer producer = session.createProducer(getDestination(queueName));

      TextMessage message = session.createTextMessage(getTestMessageBody());

      for (int i = 0; i < messageCount; i++) {
         producer.send(message);
      }
   }

   private String getTestMessageBody() {
      return "Sample Message";
   }

   private Destination getDestination(String queueName) {
      return ActiveMQDestination.createDestination("queue://" + queueName, ActiveMQDestination.TYPE.QUEUE);
   }

   private Destination getTopicDestination(String queueName) {
      return ActiveMQDestination.createDestination("topic://" + queueName, ActiveMQDestination.TYPE.TOPIC);
   }

   static class Wibble2 implements Serializable {

      private static final long serialVersionUID = -5146179676719808756L;

      String s;
   }

   static class TestListener implements MessageListener {

      boolean failed;

      int count;

      int num;

      TestListener(final int num) {
         this.num = num;
      }

      @Override
      public synchronized void onMessage(final Message m) {
         ObjectMessage om = (ObjectMessage) m;

         try {
            Wibble2 w = (Wibble2) om.getObject();
         } catch (Exception e) {
            failed = true;
         }

         count++;

         if (count == num) {
            notify();
         }
      }

      synchronized void waitForMessages() throws Exception {
         while (count < num) {
            this.wait();
         }
      }
   }
}
