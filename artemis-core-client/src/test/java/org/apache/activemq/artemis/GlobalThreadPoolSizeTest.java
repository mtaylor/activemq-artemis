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
package org.apache.activemq.artemis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.core.client.impl.ServerLocatorImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GlobalThreadPoolSizeTest {

   private static final int threadPoolMaxSize = 1000;

   private static final int threadPoolCoreSize = 100;

   private static final int scheduledThreadPoolSize = 10;

   @BeforeClass
   public static void setup() {
      System.setProperty(ActiveMQClient.THREAD_POOL_CORE_SIZE_PROPERTY_KEY, "" + threadPoolCoreSize);
      System.setProperty(ActiveMQClient.THREAD_POOL_MAX_SIZE_PROPERTY_KEY, "" + threadPoolMaxSize);
      System.setProperty(ActiveMQClient.SCHEDULED_THREAD_POOL_SIZE_PROPERTY_KEY, "" + scheduledThreadPoolSize);
   }

   @Test
   public void testThreadPoolSettings() throws Exception {
      testSystemPropertiesThreadPoolSettings(threadPoolCoreSize, threadPoolMaxSize, scheduledThreadPoolSize);

      int testCoreSize = 99;
      int testMaxSize = 999;
      int testScheduleSize = 9;

      ActiveMQClient.globalThreadCorePoolSize = testCoreSize;
      ActiveMQClient.globalThreadMaxPoolSize = testMaxSize;
      ActiveMQClient.globalScheduledThreadPoolSize = testScheduleSize;

      clean();

      testSystemPropertiesThreadPoolSettings(testCoreSize, testMaxSize, testScheduleSize);
   }

   private void testSystemPropertiesThreadPoolSettings(int expectedCore, int expectedMax, int expectedScheduled) throws Exception {
      ServerLocatorImpl serverLocator = new ServerLocatorImpl(false);
      serverLocator.isUseGlobalPools();

      Method setThreadPools = ServerLocatorImpl.class.getDeclaredMethod("setThreadPools");
      setThreadPools.setAccessible(true);
      setThreadPools.invoke(serverLocator);

      Field threadPoolField = ServerLocatorImpl.class.getDeclaredField("threadPool");
      Field scheduledThreadPoolField = ServerLocatorImpl.class.getDeclaredField("scheduledThreadPool");

      threadPoolField.setAccessible(true);
      scheduledThreadPoolField.setAccessible(true);

      ThreadPoolExecutor threadPool = (ThreadPoolExecutor) threadPoolField.get(serverLocator);
      ScheduledThreadPoolExecutor scheduledThreadPool = (ScheduledThreadPoolExecutor) scheduledThreadPoolField.get(serverLocator);

      assertEquals(expectedCore, threadPool.getCorePoolSize());
      assertEquals(expectedMax, threadPool.getMaximumPoolSize());
      assertEquals(expectedScheduled, scheduledThreadPool.getCorePoolSize());
   }

   private void clean() throws Exception {
      Field globalThreadPoolField = ServerLocatorImpl.class.getDeclaredField("globalThreadPool");
      globalThreadPoolField.setAccessible(true);
      globalThreadPoolField.set(null, null);

      Field globalScheduledThreadPoolField = ServerLocatorImpl.class.getDeclaredField("globalScheduledThreadPool");
      globalScheduledThreadPoolField.setAccessible(true);
      globalScheduledThreadPoolField.set(null, null);
   }

   @AfterClass
   public static void cleanupEnv() {
      System.clearProperty(ActiveMQClient.THREAD_POOL_CORE_SIZE_PROPERTY_KEY);
      System.clearProperty(ActiveMQClient.THREAD_POOL_MAX_SIZE_PROPERTY_KEY);
      System.clearProperty(ActiveMQClient.SCHEDULED_THREAD_POOL_SIZE_PROPERTY_KEY);
   }
}
