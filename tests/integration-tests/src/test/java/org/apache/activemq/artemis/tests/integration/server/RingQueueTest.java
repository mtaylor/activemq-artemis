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
package org.apache.activemq.artemis.tests.integration.server;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.queue.policy.LVQPolicyConfiguration;
import org.apache.activemq.artemis.core.config.queue.policy.RingQueuePolicyConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.apache.activemq.artemis.tests.util.Wait;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RingQueueTest extends ActiveMQTestBase {

   private ActiveMQServer server;

   private ClientSession clientSession;

   private ClientSession clientSessionTxReceives;

   private ClientSession clientSessionTxSends;

   private final SimpleString address = new SimpleString("RingQueueTestAddress");

   private final SimpleString qName1 = new SimpleString("RinqQueueTestQ1");

   @Test
   public void testSendReceiveNotFull() throws Exception {
      ClientProducer producer = clientSession.createProducer(address);
      ClientConsumer consumer = clientSession.createConsumer(qName1);

      ClientMessage m1 = createTextMessage(clientSession, "m1");
      ClientMessage m2 = createTextMessage(clientSession, "m2");

      producer.send(m1);
      producer.send(m2);

      clientSession.start();
      ClientMessage m = consumer.receive(1000);
      Assert.assertNotNull(m);
      m.acknowledge();
      Assert.assertEquals(m.getBodyBuffer().readString(), "m1");

      m = consumer.receive(1000);
      Assert.assertNotNull(m);
      m.acknowledge();
      Assert.assertEquals(m.getBodyBuffer().readString(), "m2");
   }

   @Test
   public void testRingFullOneInOneOut() throws Exception {
      //TODO
   }

   @Test
   public void testFirstMessageReceivedButAckedAfter() throws Exception {
   }

   @Test
   public void testFirstMessageReceivedAndCancelled() throws Exception {
   }

   @Test
   public void testManyMessagesReceivedAndCancelled() throws Exception {
   }

   @Test
   public void testSimpleInTx() throws Exception {
   }

   @Test
   public void testMultipleMessagesInTx() throws Exception {
   }

   @Test
   public void testMultipleMessagesInTxRollback() throws Exception {
   }

   @Test
   public void testSingleTXRollback() throws Exception {
   }

   @Test
   public void testMultipleMessagesInTxSend() throws Exception {
   }

   @Test
   public void testMultipleMessagesPersistedCorrectly() throws Exception {
   }

   @Test
   public void testMultipleMessagesPersistedCorrectlyInTx() throws Exception {
   }

   @Test
   public void testMultipleAcksPersistedCorrectly() throws Exception {
   }

   @Test
   public void testRemoveMessageThroughManagement() throws Exception {
   }

   @Test
   public void testScheduledMessages() throws Exception {
   }

   @Test
   public void testMultipleAcksPersistedCorrectly2() throws Exception {
   }

   @Test
   public void testMultipleAcksPersistedCorrectlyInTx() throws Exception {
   }

   @Test
   public void testLargeMessage() throws Exception {
   }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();

      server = addServer(ActiveMQServers.newActiveMQServer(getServerConfiguration(address), true));
      // start the server
      server.start();

      // then we create a client as normalServer
      ServerLocator locator = createNettyNonHALocator().setBlockOnAcknowledge(true).setAckBatchSize(0);

      ClientSessionFactory sf = createSessionFactory(locator);
      clientSession = addClientSession(sf.createSession(false, true, true));
      clientSessionTxReceives = addClientSession(sf.createSession(false, true, false));
      clientSessionTxSends = addClientSession(sf.createSession(false, false, true));
      clientSession.createQueue(address, qName1, null, true);
   }

   protected Configuration getServerConfiguration(SimpleString ringQueueAddress) throws Exception {
      Configuration configuration = createDefaultNettyConfig();

      AddressSettings addressSettings = new AddressSettings();
      RingQueuePolicyConfiguration ringQueuePolicyConfiguration = new RingQueuePolicyConfiguration();
      ringQueuePolicyConfiguration.setMaxSizeBytes(1024);
      addressSettings.setQueuePolicyConfiguration(ringQueuePolicyConfiguration);

      configuration.getAddressesSettings().put(ringQueueAddress.toString(), addressSettings);
      return configuration;
   }

}
