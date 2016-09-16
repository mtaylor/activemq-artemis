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
package org.apache.activemq.artemis.core.server.impl;

import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.junit.Before;
import org.junit.Test;

public class ActiveMQServerImplTest extends ActiveMQTestBase {

   private ActiveMQServerImpl server;

   @Before
   public void setup() throws Exception {
      server = (ActiveMQServerImpl) createServer(false);
   }

   @Test
   public void testUniqueClientIDFailsWithExistingClients() {
      String clientId = "clientId";

      server.addClientConnection(clientId, false);
      server.addClientConnection(clientId, false);
      assertFalse(server.addClientConnection(clientId, true));

      server.removeClientConnection(clientId, false);
      assertFalse(server.addClientConnection(clientId, true));

      server.removeClientConnection(clientId, false);
      assertTrue(server.addClientConnection(clientId, true));
   }

   @Test
   public void testUniqueClientIDFailsWithExistingUniqueClients() {
      String clientId = "clientId";

      server.addClientConnection(clientId, true);
      assertFalse(server.addClientConnection(clientId, true));
   }

   @Test
   public void testNewClientConnectionFailsWhenUniqueConnectionPresent() {
      String clientId = "clientId";

      server.addClientConnection(clientId, true);
      assertFalse(server.addClientConnection(clientId, false));

      server.removeClientConnection(clientId, true);
      assertTrue(server.addClientConnection(clientId, false));
   }
}
