///**
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements. See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License. You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.activemq.artemis.core.protocol.mqtt.test;
//
//import java.lang.reflect.Field;
//import java.util.UUID;
//
//import io.netty.handler.codec.mqtt.MqttConnectMessage;
//import io.netty.handler.codec.mqtt.MqttConnectPayload;
//import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
//import io.netty.handler.codec.mqtt.MqttFixedHeader;
//import io.netty.handler.codec.mqtt.MqttMessageType;
//import io.netty.handler.codec.mqtt.MqttQoS;
//import org.apache.activemq.artemis.core.protocol.mqtt.MQTTConnection;
//import org.apache.activemq.artemis.core.protocol.mqtt.test.mock.MockActiveMQServer;
//import org.apache.activemq.artemis.core.protocol.mqtt.test.mock.MockMQTTConnection;
//import org.apache.activemq.artemis.core.protocol.mqtt.test.mock.MockMQTTSession;
//
//public class Utils
//{
//   public static MockMQTTConnection createFakeConnection() throws Exception
//   {
//      MockMQTTConnection connection = new MockMQTTConnection();
//      Field field = MQTTConnection.class.getDeclaredField("server");
//      field.setAccessible(true);
//      field.set(connection, new MockActiveMQServer());
//      return connection;
//   }
//
//   public static MockMQTTSession createFakeSession()
//   {
//      return new MockMQTTSession();
//   }
//
//   public static MqttConnectMessage createMqttConnect()
//   {
//      return createMqttConnect(UUID.randomUUID().toString());
//   }
//
//   public static MqttConnectMessage createMqttConnect(String clientId)
//   {
//      MqttConnectVariableHeader varHeader = new MqttConnectVariableHeader("MQTT", 4, false, false, true, 0, true, true, 0);
//      return createMqttConnect(clientId, varHeader);
//   }
//
//   public static MqttConnectMessage createMqttConnect(String clientId, MqttConnectVariableHeader varHeader)
//   {
//      MqttFixedHeader header = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
//      MqttConnectPayload payload = new MqttConnectPayload(clientId, "willTopic", "willMessage", null, null);
//      return new MqttConnectMessage(header, varHeader, payload);
//   }
//
//   public static MqttConnectMessage createMqttConnect(String clientId, String username, String password)
//   {
//      MqttFixedHeader header = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
//      MqttConnectVariableHeader varHeader = new MqttConnectVariableHeader("MQTT", 4, false, false, false, 0, false, true, 0);
//      MqttConnectPayload payload = new MqttConnectPayload(clientId, username, password, null, null);
//      return new MqttConnectMessage(header, varHeader, payload);
//   }
//}
