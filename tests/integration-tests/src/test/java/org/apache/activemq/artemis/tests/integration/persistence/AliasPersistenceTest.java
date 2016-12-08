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
package org.apache.activemq.artemis.tests.integration.persistence;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.impl.Alias;
import org.apache.activemq.artemis.tests.util.ActiveMQTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AliasPersistenceTest extends ActiveMQTestBase {

   private ActiveMQServer server;

   @Before
   public void setup() throws Exception {
      server = createServer(createBasicConfig());
      server.start();
      waitForServerToStart(server);
   }

   @After
   public void tearDown() throws Exception {
      server.stop(false);
   }

   @Test
   public void testAliasSurvivesRestart() throws Exception {
      Alias alias = new Alias(SimpleString.toSimpleString("fromAddress"), SimpleString.toSimpleString("toAddress"));
      server.addAlias(alias);
      assertEquals(alias, server.getAlias(alias.getFromAddress()));

      server.stop();
      server.start();
      waitForServerToStart(server);
      assertEquals(alias, server.getAlias(alias.getFromAddress()));
   }

   @Test
   public void testAliasDeletesSurvivesRestart() throws Exception {
      Alias alias = new Alias(SimpleString.toSimpleString("fromAddress"), SimpleString.toSimpleString("toAddress"));
      server.addAlias(alias);
      assertEquals(alias, server.getAlias(alias.getFromAddress()));
      server.removeAlias(alias.getFromAddress());
      assertNull(server.getAlias(alias.getFromAddress()));

      server.stop();
      server.start();
      waitForServerToStart(server);
      assertNull(server.getAlias(alias.getFromAddress()));
   }
}
