package org.apache.activemq.artemis.core.protocol.mqtt;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.impl.ServerMessageImpl;
import org.apache.activemq.artemis.core.server.impl.ServerSessionImpl;
import org.apache.activemq.artemis.utils.ConcurrentHashSet;
import org.apache.activemq.artemis.utils.UUIDGenerator;

import java.util.Set;
import java.util.UUID;

public class MQTTConnectionManager
{
   private MQTTSession session;

   //TODO We should read in a list of existing client IDs from stored Sessions.
   public static Set<String> CONNECTED_CLIENTS = new ConcurrentHashSet<String>();

   private MQTTLogger logger = MQTTLogger.LOGGER;

   public MQTTConnectionManager(MQTTSession session)
   {
      this.session = session;
      MQTTFailureListener failureListener = new MQTTFailureListener(this);
      session.getConnection().addFailureListener(failureListener);
   }

   private ActiveMQServerLogger log = ActiveMQServerLogger.LOGGER;

   void connect(String cId, String username, String password, boolean will, String willMessage, String willTopic,
                boolean willRetain, int willQosLevel, boolean cleanSession, int keepAlive) throws Exception
   {
      String clientId = validateClientId(cId, cleanSession);
      if (clientId == null)
      {
         session.getProtocolHandler().sendConnack(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);
         session.getProtocolHandler().disconnect();
         return;
      }

      session.setSessionState(getSessionState(clientId, cleanSession));

      ServerSessionImpl serverSession = createServerSession(username, password);
      serverSession.start();

      session.setServerSession(serverSession);

      if (will)
      {
         ServerMessageImpl w = createWillMessage(willMessage, willTopic, willQosLevel, willRetain);
         session.getSessionState().setWillMessage(w);
      }

      session.getConnection().setConnected(true);
      session.start();
      session.getProtocolHandler().sendConnack(MqttConnectReturnCode.CONNECTION_ACCEPTED);
   }

   ServerSessionImpl createServerSession(String username, String password) throws Exception
   {
      String id = UUIDGenerator.getInstance().generateStringUUID();
      ActiveMQServer server =  session.getConnection().getServer();

      ServerSession serverSession = server.createSession(id,
            username,
            password,
            ActiveMQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE,
            session.getConnection(),
            MQTTUtil.SESSION_AUTO_COMMIT_SENDS,
            MQTTUtil.SESSION_AUTO_COMMIT_ACKS,
            MQTTUtil.SESSION_PREACKNOWLEDGE,
            MQTTUtil.SESSION_XA,
            null,
            session.getSessionCallback(),
            null, // Session factory
            MQTTUtil.SESSION_AUTO_CREATE_QUEUE);
      return (ServerSessionImpl) serverSession;
   }

   void disconnect()
   {
      try
      {
         if (session != null && session.getSessionState() != null)
         {
            String clientId = session.getSessionState().getClientId();
            if (clientId != null) CONNECTED_CLIENTS.remove(clientId);

            if (session.getState().isWill())
            {
               session.getConnectionManager().sendWill();
            }
         }
         session.stop();
         session.getConnection().disconnect(false);
         session.getConnection().destroy();
      }
      catch (Exception e)
      {
         log.error("Error during disconnect");
         e.printStackTrace();
         // TODO Better Error Handling
      }
   }

   private void sendWill() throws Exception
   {
      session.getServerSession().send(session.getSessionState().getWillMessage(), true);
      // todo(mtaylor) Do we need to reset will message on send?
      session.getSessionState().deleteWillMessage();
   }

   private MQTTSessionState getSessionState(String clientId, boolean cleanSession) throws InterruptedException
   {
      synchronized (MQTTSession.SESSIONS)
      {
         /* [MQTT-3.1.2-6] If CleanSession is set to 1, the Client and Server MUST discard any previous Session and
          * start a new one  This Session lasts as long as the Network Connection. State data associated with this Session
          * MUST NOT be reused in any subsequent Session */
         if (cleanSession)
         {
            MQTTSession.SESSIONS.remove(clientId);
            return new MQTTSessionState(clientId);
         }
         else
         {
            /* [MQTT-3.1.2-4] Attach an existing session if one exists (if cleanSession flag is false) otherwise create
            a new one. */
            MQTTSessionState state = MQTTSession.SESSIONS.get(clientId);
            if (state != null)
            {
               // wait until any existing session is stopped.
               while(state.getAttached())
               {
                  Thread.sleep(1000);
               }

               return  state;
            }
            else
            {
               state = new MQTTSessionState(clientId);
               MQTTSession.SESSIONS.put(clientId, state);
               return state;
            }
         }
      }
   }

   private String validateClientId(String clientId, boolean cleanSession)
   {
      if (clientId == null || clientId == "")
      {
         // [MQTT-3.1.3-7] [MQTT-3.1.3-6] If client does not specify a client ID and clean session is set to 1 create it.
         if (cleanSession)
         {
            clientId = UUID.randomUUID().toString();
         }
         else
         {
            // [MQTT-3.1.3-8] Return ID rejected and disconnect if clean session = false and client id is null
            return null;
         }
      }
      // If the client ID is not unique (i.e. it has already registered) then do not accept it.
      else if(!CONNECTED_CLIENTS.add(clientId))
      {
         // [MQTT-3.1.3-9] Return ID Rejected if server rejects the client ID
         return null;
      }
      return clientId;
   }

   private ServerMessageImpl createWillMessage(String message, String topic, int qos, boolean retain)
   {
      long id = session.getConnection().getServer().getStorageManager().generateID();
      ServerMessageImpl serverMessage =  new ServerMessageImpl(id, MQTTUtil.DEFAULT_SERVER_MESSAGE_BUFFER_SIZE);
      serverMessage.getBodyBuffer().writeString(message);
      serverMessage.setAddress(new SimpleString(topic));
      serverMessage.putIntProperty(new SimpleString(MQTTUtil.MQTT_QOS_LEVEL_KEY), qos);
      serverMessage.putBooleanProperty(new SimpleString(MQTTUtil.MQTT_WILL_RETAIN_KEY), retain);
      return serverMessage;
   }

}
