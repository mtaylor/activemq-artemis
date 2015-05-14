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

//package org.apache.activemq.artemis.tests.integration.mqtt;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.UUID;
//
//import junit.framework.Assert;
//
//import io.netty.handler.codec.mqtt.MqttConnectMessage;
//import io.netty.handler.codec.mqtt.MqttConnectPayload;
//import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
//import io.netty.handler.codec.mqtt.MqttFixedHeader;
//import io.netty.handler.codec.mqtt.MqttMessage;
//import io.netty.handler.codec.mqtt.MqttMessageType;
//import io.netty.handler.codec.mqtt.MqttQoS;
//import org.apache.activemq.artemis.api.core.TransportConfiguration;
//import org.apache.activemq.artemis.core.protocol.ProtocolHandler;
//import org.apache.activemq.artemis.core.protocol.mqtt.MQTTConnection;
//import org.apache.activemq.artemis.core.protocol.mqtt.MQTTProtocolHandler;
//import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
//import org.apache.activemq.artemis.core.server.ActiveMQServer;
//import org.apache.activemq.artemis.tests.util.ServiceTestBase;
//import org.fusesource.mqtt.client.BlockingConnection;
//import org.fusesource.mqtt.client.MQTT;
//import org.fusesource.mqtt.client.Message;
//import org.fusesource.mqtt.client.QoS;
//import org.fusesource.mqtt.client.Topic;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
///**
// * @author mtaylor
// */
//
//public class MQTTTest extends ServiceTestBase
//{
//   private ActiveMQServer server;
//
//   private BlockingConnection mqttConnection;
//
//   @Before
//   public void setup() throws Exception
//   {
//      super.setUp();
//      server = createServer(true, true);
//      HashMap<String, Object> params = new HashMap<String, Object>();
//      params.put(TransportConstants.PORT_PROP_NAME, "1883");
//      params.put(TransportConstants.PROTOCOLS_PROP_NAME, "MQTT");
//      TransportConfiguration transportConfiguration = new TransportConfiguration(NETTY_ACCEPTOR_FACTORY, params);
//
//      server.getConfiguration().getAcceptorConfigurations().add(transportConfiguration);
//      server.start();
//      waitForServer(server);
//
//      MQTT mqtt = new MQTT();
//      mqtt.setVersion("3.1.1");
//      mqtt.setHost("localhost", 1883);
//      mqtt.setClientId(UUID.randomUUID().toString());
//      mqttConnection = mqtt.blockingConnection();
//   }
//
//   @After
//   public void tearDown() throws Exception
//   {
//      mqttConnection.kill();
//      server.stop();
//   }
//
//   @Test
//   public void testConnect() throws Exception
//   {
//      mqttConnection.connect();
//      assertTrue(mqttConnection.isConnected());
//   }
//
//   @Test
//   public void testPublish() throws Exception
//   {
//      mqttConnection.connect();
//      Topic[] topics = {new Topic("mqtt.foo.bar", QoS.AT_LEAST_ONCE)};
//      mqttConnection.subscribe(topics);
//      String payload = "Test Message Payload";
//
//      mqttConnection.handleMessage("mqtt.foo.bar", payload.getBytes(), QoS.AT_LEAST_ONCE, false);
//      Message message = mqttConnection.receive();
//      assertArrayEquals(payload.getBytes(), message.getPayload());
//      Thread.sleep(4000);
//   }
//
//   @Test
//   /**
//    * [MQTT-1.5.3-1] The character data in a UTF-8 encoded string MUST be well-formed UTF-8 as defined by the Unicode
//    * specification [Unicode] and restated in RFC 3629 [RFC3629]. In particular this data MUST NOT include encodings of
//    * code points between U+D800 and U+DFFF. If a Server or Client receives a Control Packet containing ill-formed UTF-8
//    * it MUST close the Network Connection.
//    */
//   public void testDisconnectOnInvalidChars()
//   {
//      //TODO
//   }
//
//   @Test
//   /**
//   * [MQTT-1.5.3-2]  A UTF-8 encoded string MUST NOT include an encoding of the null character U+0000. If a receiver
//   * (Server or Client) receives a Control Packet containing U+0000 it MUST close the Network Connection.
//   */
//   public void testDisconnectOnNullChar()
//   {
//      //TODO
//   }
//
//   /**
//   * [MQTT-1.5.3-3] A UTF-8 encoded sequence 0xEF 0xBB 0xBF is always to be interpreted to mean U+FEFF ("ZERO WIDTH NO
//   * BREAK SPACE") wherever it appears in a string and MUST NOT be skipped over or stripped off by a packet receiver.
//   */
//   public void testZeroWidthNoBreakSpace()
//   {
//      //TODO
//   }
//
//   /**
//    * [MQTT-2.2.2-1] Where a flag bit is marked as “Reserved” in Table 2.2 - Flag Bits, it is reserved for future use
//    * and MUST be set to the value listed in that table.
//    */
//   public void testReservedFlagBitsAreProperlySet()
//   {
//      // TODO
//   }
//
//   /**
//    * [MQTT-2.2.2-2] If invalid flags are received, the receiver MUST close the Network Connection.
//    */
//   public void testDisconnectOnInvalidFlag()
//   {
//      // TODO
//   }
//
//   /** [MQTT-2.3.1-1] SUBSCRIBE, UNSUBSCRIBE, and PUBLISH (in cases where QoS > 0) Control Packets MUST contain a
//   * non-zero 16-bit Packet Identifier.
//   */
//   public void testPacketIdentifierIsProperlySet()
//   {
//      // TODO
//   }
//
//   /** [MQTT-2.3.1-2] Each time a Client sends a new packet of one of these types (SUBSCRIBE, UNSUBSCRIBE, and PUBLISH)
//    * it MUST assign it a currently unused Packet Identifier.
//    */
//    public void testClientAssignUnusedPacketIdentifier()
//    {
//       // TODO
//    }
//}
