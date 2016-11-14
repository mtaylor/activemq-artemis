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
package org.apache.activemq.artemis.tests.integration.cluster.distribution;

import org.apache.activemq.artemis.core.protocol.mqtt.MQTTProtocolManagerFactory;
import org.apache.activemq.artemis.core.server.cluster.impl.MessageLoadBalancingType;
import org.apache.activemq.artemis.tests.integration.mqtt.imported.FuseMQTTClientProvider;
import org.junit.Before;
import org.junit.Test;

public class MQTTWildcardClusterTest extends ClusterTestBase {

   private String host1 = "tcp://localhost:1883";

   private String host2 = "tcp://localhost:1884";

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      setupServers();
      startServers();
   }

   protected void setupServers() throws Exception {
      setupServer(0, isFileStorage(), isNetty());
      setupServer(1, isFileStorage(), isNetty());

      setupClusterConnection("cluster0", "mqtt", MessageLoadBalancingType.STRICT, 1, isNetty(), 0, 1);
      setupClusterConnection("cluster1", "mqtt", MessageLoadBalancingType.STRICT, 1, isNetty(), 1, 0);

      servers[0].addProtocolManagerFactory(new MQTTProtocolManagerFactory());
      servers[0].getConfiguration().addAcceptorConfiguration("mqtt", "tcp://localhost:1883?protocols=MQTT");

      servers[1].addProtocolManagerFactory(new MQTTProtocolManagerFactory());
      servers[1].getConfiguration().addAcceptorConfiguration("mqtt", "tcp://localhost:1884?protocols=MQTT");
   }

   protected void startServers() throws Exception {
      startServers(0, 1);
   }

   protected void stopServers() throws Exception {
      stopServers(0, 1);
   }

   protected boolean isNetty() {
      return true;
   }

   @Test
   public void testMQTTWildcardSubscriber() throws Exception {
      String payload = "TestMessage";

      FuseMQTTClientProvider client1 = new FuseMQTTClientProvider();
      client1.connect(host1);


      FuseMQTTClientProvider client2 = new FuseMQTTClientProvider();
      client2.connect(host2);

      client1.subscribe("mqtt/#", 1);
      Thread.sleep(2000);

      client2.publish("mqtt/1", payload.getBytes(), 1);

      byte[] m = client1.receive(1000);
      assertNotNull(m);
      assertEquals(new String(m), payload);
   }
}
