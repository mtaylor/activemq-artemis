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
//import java.lang.reflect.Method;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.ByteBufAllocator;
//import io.netty.handler.codec.mqtt.MqttConnAckMessage;
//import io.netty.handler.codec.mqtt.MqttConnectMessage;
//import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
//import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
//import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
//import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
//import org.apache.activemq.artemis.core.buffers.impl.ChannelBufferWrapper;
//import org.apache.activemq.artemis.core.protocol.mqtt.MQTTConnection;
//import org.apache.activemq.artemis.core.protocol.mqtt.codec.MQTTDecoder;
//import org.apache.activemq.artemis.core.protocol.mqtt.codec.MQTTEncoder;
//import org.apache.activemq.artemis.core.protocol.mqtt.MQTTProtocolHandler;
//import org.apache.activemq.artemis.core.protocol.mqtt.MQTTSession;
//import org.apache.activemq.artemis.core.protocol.mqtt.MQTTUtil;
//import org.apache.activemq.artemis.core.protocol.mqtt.netty.extensions.ActiveMQByteBufAllocator;
//import org.apache.activemq.artemis.core.server.impl.ServerMessageImpl;
//import org.apache.activemq.artemis.core.protocol.mqtt.test.mock.MockMQTTConnection;
//import org.apache.activemq.artemis.core.protocol.mqtt.test.mock.MockMQTTSession;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//
//public class MQTTProtocolHandlerTest
//{
//   /**
//    * [MQTT-3.1.0-2] The Server MUST process a second CONNECT Packet sent from a Client as a protocol violation and
//    * disconnect the Client
//    */
//
//   MQTTProtocolHandler protocolHandler;
//
//   MockMQTTConnection connection;
//
//   MockMQTTSession session;
//
//   @Before
//   public void setUp() throws Exception
//   {
//      connection = Utils.createFakeConnection();
//      session = Utils.createFakeSession();
//      session.attach(connection);
//      protocolHandler = new MQTTProtocolHandler(connection);
//   }
//
//   /**
//    * [MQTT-3.1.2-3] The Server MUST validate that the reserved flag in the CONNECT Control Packet is set to zero and
//    * disconnect the Client if it is not zero
//    */
//   @Test
//   public void ensureDisconnectIfConnectReservedFlagIsSet()
//   {
//      Assert.fail("Unimplemented"); // Requires support from Netty Codec to Inspect this flag.
//   }
//
//   /**
//    * [MQTT-3.1.4-4] The Server MUST acknowledge the CONNECT Packet with a CONNACK Packet containing a zero return code
//    */
//   @Test
//   public void testValidConnectReturnConnAck() throws Exception
//   {
//      // Set up mock connections
//      MockMQTTConnection connection = Utils.createFakeConnection();
//
//      // Send 2 connect messages
//      MqttConnectMessage connect = Utils.createMqttConnect();
//      protocolHandler.handleConnect(connect);
//
//      // Check Connack Recieved with zero return code..
//      MQTTDecoder decoder = new MQTTDecoder();
//      MqttConnAckMessage connAck = (MqttConnAckMessage) decoder.decode(connection.getTransportOutBuffer()).get(0);
//      Assert.assertTrue(connAck.variableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED);
//   }
//
//   /**
//    * MQTT-3.1.4-3] The Server MUST perform the processing of CleanSession that is described in section 3.1.2.4
//    */
//   @Test
//   public void testCleanSessionProcessing()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.1.4-5] If the Server rejects the CONNECT, it MUST NOT process any data sent by the Client after the
//    * CONNECT Packet
//    */
//   @Test
//   public void testServerDoesNotProcessSubsequentDataAfterInvalidConnect() throws Exception
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.1.0-2] The Server MUST process a second CONNECT Packet sent from a Client as a protocol violation and
//    * disconnect the Client
//    */
//   @Test
//   public void ensureDisconnectWhenClientConnectIsSentMoreThanOnce() throws Exception
//   {
//      // Set up mock connections
//      MockMQTTConnection connection = Utils.createFakeConnection();
//
//      // Send 2 connect messages
//      MqttConnectMessage connect = Utils.createMqttConnect();
//      protocolHandler.handleConnect(connect);
//      protocolHandler.handleConnect(connect);
//      Assert.assertTrue(connection.getDestroyed());
//   }
//
//   /**
//    * [MQTT-3.1.3-2] Each Client connecting to the Server has a unique ClientId. The ClientId MUST be used by Clients
//    * and by Servers to identify state that they hold relating to this MQTT Session between the Client and the Server
//    * <p/>
//    * [MQTT-3.1.3-9] If the Server rejects the ClientId it MUST respond to the CONNECT Packet with a CONNACK return
//    * code 0x02 (Identifier rejected) and then close the Network Connection.
//    * <p/>
//    * [MQTT-3.1.4-2] If the ClientId represents a Client already connected to the Server then the Server MUST disconnect
//    * the existing Client
//    */
//   @Test
//   public void ensureDisconnectOnDuplicateClientId() throws Exception
//   {
//      // Set up mock connections
//      MockMQTTConnection connection1 = Utils.createFakeConnection();
//      MockMQTTConnection connection2 = Utils.createFakeConnection();
//
//      // Send same connect (same client ID) message on two separate connections.
//      MqttConnectMessage connect = Utils.createMqttConnect();
//
//      protocolHandler.handleConnect(connect);
//      protocolHandler.handleConnect(connect);
//
//      // Check Connection Refused was sent to client 2.
//      MQTTDecoder decoder = new MQTTDecoder();
//      MqttConnAckMessage connAck = (MqttConnAckMessage) decoder.decode(connection2.getTransportOutBuffer()).get(0);
//      Assert.assertTrue(connAck.variableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
//
//      // Connection 2 should be disconnected as a client with this ID is already registered.
//      Assert.assertFalse(connection1.getDestroyed());
//      Assert.assertTrue(connection2.getDestroyed());
//   }
//
//   /**
//    * [MQTT-3.1.3-5]  The Server MUST allow ClientIds which are between 1 and 23 UTF-8 encoded bytes in length, and that
//    * contain only the characters "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
//    */
//   @Test
//   public void ensureClientIDFormatIsSupported() throws Exception
//   {
//      // Test char set and boundaries
//      List<String> inputList = Arrays.asList("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
//                                             "0",
//                                             "00000000000000000000000",
//                                             "123143214234");
//      for (String clientId : inputList)
//      {
//         MockMQTTConnection connection = Utils.createFakeConnection();
//         MqttConnectMessage connect = Utils.createMqttConnect(clientId);
//         protocolHandler.handleConnect(connection, connect);
//         Assert.assertFalse(connection.getDestroyed());
//      }
//   }
//
//   /**
//    * [MQTT-3.1.3-6] A Server MAY allow a Client to supply a ClientId that has a length of zero bytes, however if it
//    * does so the Server MUST treat this as a special case and assign a unique ClientId to that Client. It MUST then
//    * process the CONNECT packet as if the Client had provided that unique ClientId
//    */
//   @Test
//   public void ensureServerCreatesClientIdWhenNoneIsSpecifiedInConnect() throws Exception
//   {
//      // Access ClientID Set
//      Field field = MQTTProtocolHandler.class.getDeclaredField("CONNECTED_CLIENTS");
//      field.setAccessible(true);
//      Set<String> CONNECTED_CLIENTS = (Set<String>) field.get(protocolHandler);
//      int size = CONNECTED_CLIENTS.size();
//
//      // Set up mock connections
//      MockMQTTConnection connection = Utils.createFakeConnection();
//
//      // Send connect with no Client Id
//      MqttConnectMessage connect = Utils.createMqttConnect(null);
//      protocolHandler.handleConnect(connection, connect);
//      Assert.assertFalse(connection.getDestroyed());
//      Assert.assertTrue(CONNECTED_CLIENTS.size() == size + 1);
//   }
//
//   /**
//    * [MQTT-3.1.3-7] If the Client supplies a zero-byte ClientId, the Client MUST also set CleanSession to 1
//    */
//   @Test
//   public void ensureDisconnectIfCleanSessionIsSetTo0WhenNoClientIdIsSupplied() throws Exception
//   {
//      // Access ClientID Set
//      Field field = MQTTProtocolHandler.class.getField("CONNECTED_CLIENTS");
//      field.setAccessible(true);
//      Set<String> CONNECTED_CLIENTS = (Set<String>) field.get(protocolHandler);
//      int size = CONNECTED_CLIENTS.size();
//
//      // Set up mock connections
//      MockMQTTConnection connection = Utils.createFakeConnection();
//
//      // Send connect with no Client Id and Clean Session = false
//      MqttConnectVariableHeader varHeader = new MqttConnectVariableHeader(null, 0, false, false, false, 0, false, false, 0);
//      MqttConnectMessage connect = Utils.createMqttConnect(null, varHeader);
//      protocolHandler.handleConnect(connection, connect);
//      Assert.assertTrue(connection.getDestroyed());
//      Assert.assertTrue(CONNECTED_CLIENTS.size() == size);
//
//      MQTTDecoder decoder = new MQTTDecoder();
//      MqttConnAckMessage connAck = (MqttConnAckMessage) decoder.decode(connection.getTransportOutBuffer()).get(0);
//      Assert.assertTrue(connAck.variableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
//   }
//
//   /**
//    * [MQTT-3.1.4-1]  The Server MUST validate that the CONNECT Packet conforms to section 3.1 and close the Network
//    * Connection without sending a CONNACK if it does not conform
//    * <p/>
//    * We rely on the Netty Codec to throw an appropriate exception on a malformed message.  For this test we will
//    * simply set an invalid protocol version on the connect message to force the netty mqtt codec to fail. We must do
//    * this on an already encoded byte stream as the encoder would fail to encode a message if it is malformed before
//    * we encode it.
//    */
//   @Test
//   public void ensureServerClosesConnectionWithOutSendingConnackOnInvalidConnectMessage() throws Exception
//   {
//      MQTTEncoder encoder = new MQTTEncoder();
//
//      MockMQTTConnection connection = Utils.createFakeConnection();
//      // Decode the message and send to the ProtocolHandler
//      MqttConnectMessage invalidConnect = Utils.createMqttConnect();
//      ActiveMQBuffer in = ActiveMQBuffers.dynamicBuffer(512);
//
//      // Encode a valid connect message
//      ByteBufAllocator bufAllocator = new ActiveMQByteBufAllocator(in);
//      ByteBuf encodedBuffer = encoder.encode(invalidConnect, bufAllocator);
//
//      // Create a new buffer and copy everything up to the protocol version byte.
//      ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
//      out.writeBytes(encodedBuffer, encodedBuffer.readerIndex(), 8);
//
//      // Move the reader index of the encoded buffer the same amount
//      encodedBuffer.readBytes(9);
//
//      // Set the protocol version to 0 (invalid)
//      out.writeByte(0);
//
//      // Copy the rest of the message on to the buffer
//      out.writeBytes(encodedBuffer);
//
//      protocolHandler.handleBuffer(connection, new ChannelBufferWrapper(out));
//      Assert.assertTrue(connection.getDestroyed());
//
//      // Ensure that server did not send a Connack.
//      Assert.assertTrue(connection.getTransportOutBuffer().readableBytes() == 0);
//   }
//
//   /**
//    * [MQTT-3.2.0-1] The first packet sent from the Server to the Client MUST be a CONNACK Packet
//    * <p/>
//    * Check the server replies with Successful ConnAck after Connect is received.
//    */
//   @Test
//   public void ensureServerSendsConnAckAfterConnect() throws Exception
//   {
//      // Set up mock connections
//      MockMQTTConnection connection = Utils.createFakeConnection();
//
//      // Send connect
//      MqttConnectMessage connect = Utils.createMqttConnect();
//      protocolHandler.handleConnect(connection, connect);
//
//      MQTTDecoder decoder = new MQTTDecoder();
//      MqttConnAckMessage connAck = (MqttConnAckMessage) decoder.decode(connection.getTransportOutBuffer()).get(0);
//      Assert.assertTrue(connAck.variableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED);
//   }
//
//   /**
//    * [MQTT-3.1.2-4] If CleanSession is set to 0, the Server MUST resume communications with the Client based on state
//    * from the current Session (as identified by the Client identifier). If there is no Session associated with the
//    * Client identifier the Server MUST create a new Session. The Client and Server MUST store the Session after the
//    * Client and Server are disconnected
//    */
//   @Test
//   public void ensureSessionIsReattachedWhenCleanSessionIsFalse() throws Exception
//   {
//      Method method = MQTTProtocolHandler.class.getDeclaredMethod("disconnect", MQTTConnection.class);
//      method.setAccessible(true);
//      // Set up mock connections
//      MockMQTTConnection connection = Utils.createFakeConnection();
//
//      // Send connect
//      MqttConnectMessage connect = Utils.createMqttConnect();
//      protocolHandler.handleConnect(connection, connect);
//      MQTTSession session = connection.getSession();
//
//      // Disconnect
//      method.invoke(protocolHandler, connection);
//
//      // Create new Connection
//      connection = Utils.createFakeConnection();
//      protocolHandler.handleConnect(connection, connect);
//
//      Assert.assertTrue(connection.getSession() == session);
//   }
//
//   /**
//    * [MQTT-3.1.2-8] If the Will Flag is set to 1 this indicates that, if the Connect request is accepted, a Will
//    * Message MUST be stored on the Server and associated with the Network Connection. The Will Message MUST be
//    * published when the Network Connection is subsequently closed unless the Will Message has been deleted by the
//    * Server on receipt of a DISCONNECT Packet.
//    */
//   @Test
//   public void testWillMessageIsSentOnDisconnect() throws Exception
//   {
//      // Check message is stored on the server.
//      // Set up mock connections
//      MockMQTTConnection connection = Utils.createFakeConnection();
//
//      // Send 2 connect messages
//      MqttConnectMessage connect = Utils.createMqttConnect();
//      protocolHandler.handleConnect(connection, connect);
//
//      Field field = MQTTSession.class.getDeclaredField("willMessage");
//      field.setAccessible(true);
//      ServerMessageImpl willMessage = (ServerMessageImpl) field.get(connection.getSession());
//      Assert.assertTrue(willMessage.getIntProperty(MQTTUtil.MQTT_QOS_LEVEL_KEY) == connect.variableHeader().willQos());
//      Assert.assertTrue(willMessage.getBooleanProperty(MQTTUtil.MQTT_WILL_RETAIN_KEY) == connect.variableHeader().isWillRetain());
//      Assert.assertEquals(connect.payload().willTopic(), willMessage.getAddress().toString());
//      Assert.assertEquals(connect.payload().willMessage(), willMessage.getBodyBuffer().readString());
//
//      // Check message is sent on disconnect.
//      field = MQTTProtocolHandler.class.getDeclaredField("session");
//      field.setAccessible(true);
//      field.set(protocolHandler, session);
//
//      Method method = MQTTProtocolHandler.class.getDeclaredMethod("disconnect", MQTTConnection.class);
//      method.setAccessible(true);
//      method.invoke(protocolHandler, connection);
//      Assert.assertTrue(session.getWillMessageSent());
//   }
//
//   /**
//    * [MQTT-3.1.2-10] The Will Message MUST be removed from the stored Session state in the Server once it has been
//    * published or the Server has received a DISCONNECT packet from the Client .
//    */
//   @Test
//   public void ensureWillMessageIsremovedOnDisconnect() throws Exception
//   {
//      // Check message is stored on the server.
//      // Set up mock connections
//      MockMQTTConnection connection = Utils.createFakeConnection();
//
//      // Check message is sent on disconnect.
//      Field field = MQTTProtocolHandler.class.getDeclaredField("session");
//      field.setAccessible(true);
//      field.set(protocolHandler, session);
//
//      ServerMessageImpl willMessage = new ServerMessageImpl(1, 256);
//      Field fieldWillMessage = MQTTSession.class.getDeclaredField("willMessage");
//      fieldWillMessage.setAccessible(true);
//      fieldWillMessage.set(session, willMessage);
//
//      Field fieldIsWill = MQTTSession.class.getDeclaredField("isWill");
//      fieldIsWill.setAccessible(true);
//      fieldIsWill.set(session, true);
//
//      protocolHandler.handleDisconnect(connection, null);
//      Assert.assertEquals(null, fieldWillMessage.get(session));
//      Assert.assertFalse((boolean) fieldIsWill.get(session));
//
//      // TODO Add test for public
//   }
//
//
//   /**
//    * [MQTT-3.1.2-12] If the Will Flag is set to 0, a Will Message MUST NOT be published when this Network Connection
//    * ends
//    */
//   @Test
//   public void ensureWillMessageIsNotPublishedOnConnectionEnd() throws Exception
//   {
//      // Check message is stored on the server.
//      // Set up mock connections
//      MockMQTTConnection connection = Utils.createFakeConnection();
//
//      // Send 2 connect messages
//      MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader("MQTT", 4, false, false, false, 0, false, false, 0);
//      MqttConnectMessage connect = Utils.createMqttConnect(UUID.randomUUID().toString(), variableHeader);
//      protocolHandler.handleConnect(connection, connect);
//
//      // Check message is sent on disconnect.
//      Field field = MQTTProtocolHandler.class.getDeclaredField("session");
//      field.setAccessible(true);
//      field.set(protocolHandler, session);
//
//      Method method = MQTTProtocolHandler.class.getDeclaredMethod("disconnect", MQTTConnection.class);
//      method.setAccessible(true);
//      method.invoke(protocolHandler, connection);
//      Assert.assertFalse(session.getWillMessageSent());
//   }
//
//   /**
//    * [MQTT-3.1.2.7] Retained messages do not form part of the Session state in the Server, they MUST NOT be deleted
//    * when the Session ends
//    */
//   @Test
//   public void ensureRetainedMessagesAreNotdeletedWhenSessionEnds()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.1.2-16] If Will Retain is set to 0, the Server MUST handleMessage the Will Message as a non-retained message .
//    */
//   @Test
//   public void ensureWillMessageIsPublishedAsNonRetainMessageOnWillRetainFalse()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.1.2-17] If Will Retain is set to 1, the Server MUST handleMessage the Will Message as a retained message .
//    */
//   @Test
//   public void ensureWillMessageIsPublishedAsRetainMessageOnWillRetainTrue()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * If the Keep Alive value is non-zero and the Server does not receive a Control Packet from the Client within one
//    * and a half times the Keep Alive time period, it MUST disconnect the Network Connection to the Client as if the
//    * network had failed [MQTT-3.1.2-24].
//    */
//   @Test
//   public void ensureDisconnectWhenClientKeepAliveExpires()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.2.2-1] If the Server accepts a connection with CleanSession set to 1, the Server MUST set Session Present
//    * to 0 in the CONNACK packet in addition to setting a zero return code in the CONNACK packet
//    */
//   @Test
//   public void testServerSetsCleanSessionTrueInConnack()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.2.2-2] If the Server accepts a connection with CleanSession set to 0, the value set in Session Present
//    * depends on whether the Server already has stored Session state for the supplied client ID. If the Server has
//    * stored Session state, it MUST set Session Present to 1 in the CONNACK packet
//    */
//   public void testServerSetsSessionPresentIfSessionAlreadyExists()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.2.2-3] If the Server does not have stored Session state, it MUST set Session Present to 0 in the CONNACK
//    * packet. This is in addition to setting a zero return code in the CONNACK packet
//    */
//   @Test
//   public void testServerSetsSessionFalseIfNoSessionExists()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.2.2-4] If a server sends a CONNACK packet containing a non-zero return code it MUST set Session Present to 0
//    */
//   public void testFailedConnAckHasSessionPresentFalse()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.2.2-5] If a server sends a CONNACK packet containing a non-zero return code it MUST then close the
//    * Network Connection.
//    */
//   public void testServerClosesConnectionOnFailedConnect()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.2.2-6] If none of the return codes listed in Table 3.1 – Connect Return code values are deemed applicable,
//    * then the Server MUST close the Network Connection without sending a CONNACK
//    */
//   public void testServerClosesConnectionWithNoConnackIfNotApplicable()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /* ######### PUBLISH ########## */
//
//   /**
//    * [MQTT-3.3.1.-1] The DUP flag MUST be set to 1 by the Client or Server when it attempts to re-deliver a PUBLISH
//    * Packet.
//    */
//   @Test
//   public void testDupFlagIsSetTo1ForRedeliveries()
//   {
//      // Checked by hand
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.3.1-2] The DUP flag MUST be set to 0 for all QoS 0 messages.
//    */
//   @Test
//   public void testDupFlagIsSetTo0ForAllQoS0Messages()
//   {
//      // Checked by hand
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-3.3.1-3] The value of the DUP flag from an incoming PUBLISH packet is not propagated when the PUBLISH
//    * Packet is sent to subscribers by the Server. The DUP flag in the outgoing PUBLISH packet is set independently to
//    * the incoming PUBLISH packet, its value MUST be determined solely by whether the outgoing PUBLISH packet is a
//    * retransmission .
//    */
//   public void testRedeliveryOnPublishPacketIsNotPropogated()
//   {
//      // checked by hand
//      Assert.fail("Unimplemented");
//   }
//
//
//   /**
//    * [MQTT-2.3.1-6] A PUBACK, PUBREC or PUBREL Packet MUST contain the same Packet Identifier as the PUBLISH Packet
//    * that was originally sent .
//    */
//   @Test
//   public void ensureResponsesToPublishContainTheSamePacketIDAsPublish()
//   {
//      Assert.fail("Unimplemented");
//   }
//
//   /**
//    * [MQTT-2.3.1-7] Similarly (to [MQTT-2.3.1-6]) SUBACK and UNSUBACK MUST contain the Packet Identifier that was
//    * used in the corresponding SUBSCRIBE and UNSUBSCRIBE Packet respectively .
//    */
//   @Test
//   public void ensureResponsesToSubscribeContainTheSamePacketIDAsPublish()
//   {
//      Assert.fail("Unimplemented");
//   }
//}
