/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.tests.performance.storage;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.InitialContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.artemis.core.config.ConnectorServiceConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.junit.Assert;
import org.junit.Test;

public class PersistMultiThreadTest extends ActiveMQTestBase {

   ConnectionFactory cf;

   Destination destination;

   @Test
   public void testMultipleWrites() throws Exception {

      ActiveMQServer server = createServer(true);
      server.getConfiguration().setJournalFileSize(10 * 1024 * 1024);
      server.getConfiguration().setJournalMinFiles(2);
      server.getConfiguration().setJournalType(JournalType.ASYNCIO);

      // TODO Setup Acceptors

      server.start();

      int NUMBER_OF_THREADS = 50;
      int NUMBER_OF_MESSAGES = 5000;

      MyThread[] threads = new MyThread[NUMBER_OF_THREADS];

      final CountDownLatch alignFlag = new CountDownLatch(NUMBER_OF_THREADS);
      final CountDownLatch startFlag = new CountDownLatch(1);
      final CountDownLatch finishFlag = new CountDownLatch(NUMBER_OF_THREADS);
      final CountDownLatch messageConsumed = new CountDownLatch(NUMBER_OF_MESSAGES * NUMBER_OF_THREADS);

      InitialContext initialContext = new InitialContext();

      cf = (ConnectionFactory) initialContext.lookup("ConnectionFactory");

      destination = (Destination) initialContext.lookup("queue/performanceQueue");

      Connection connection = cf.createConnection();

      Session consumerSession = connection.createSession(true, Session.SESSION_TRANSACTED);

      consumerSession.setMessageListener(new MyMessageConsumer(messageConsumed));

      for (int i = 0; i < threads.length; i++) {
         threads[i] = new MyThread("writer::" + i, NUMBER_OF_MESSAGES, alignFlag, startFlag, finishFlag);
      }

      for (MyThread t : threads) {
         t.start();
      }

      alignFlag.await();

      long startTime = System.currentTimeMillis();
      startFlag.countDown();

      // I'm using a countDown to avoid measuring time spent on thread context from join.
      // i.e. i want to measure as soon as the loops are done
      finishFlag.await();
      messageConsumed.await();

      long endtime = System.currentTimeMillis();

      for (MyThread t : threads) {
         t.join();
         Assert.assertEquals(0, t.errors.get());
      }

      System.out.println("Time:: " + (endtime - startTime));
   }

   class MyMessageConsumer implements MessageListener {

      private final CountDownLatch messageConsumed;

      public MyMessageConsumer(CountDownLatch messageConsumed) {
         this.messageConsumed = messageConsumed;
      }
      @Override
      public void onMessage(Message message) {
         messageConsumed.countDown();
      }
   }

   class MyThread extends Thread
   {
      final int numberOfMessages;
      final AtomicInteger errors = new AtomicInteger(0);

      final CountDownLatch align;
      final CountDownLatch start;
      final CountDownLatch finish;

      MyThread(String name, int numberOfMessages, CountDownLatch align, CountDownLatch start, CountDownLatch finish)
      {
         super(name);
         this.numberOfMessages = numberOfMessages;
         this.align = align;
         this.start = start;
         this.finish = finish;
      }

      public void run() {
         try {

            Connection connection = cf.createConnection();

            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            MessageProducer producer = session.createProducer(destination);

            align.countDown();
            start.await();

            for (int i = 0; i < numberOfMessages; i++) {
               producer.send(session.createTextMessage("Test Message"));
               session.commit();
            }
         }
         catch (Exception e) {
            e.printStackTrace();
            errors.incrementAndGet();
         }
         finally {
            finish.countDown();
         }
      }
   }
}
