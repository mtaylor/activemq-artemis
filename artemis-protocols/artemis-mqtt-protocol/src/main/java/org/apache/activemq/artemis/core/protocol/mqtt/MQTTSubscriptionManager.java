/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.activemq.artemis.core.protocol.mqtt;

import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MQTTSubscriptionManager
{
   private static final boolean DURABLE_QUEUES = true;

   private MQTTSession session;

   private ConcurrentHashMap<Long, Integer> consumerQoSLevels;

   private ConcurrentHashMap<String, ServerConsumer> consumers;

   private MQTTLogger log = MQTTLogger.LOGGER;

   public MQTTSubscriptionManager(MQTTSession session)
   {
      this.session = session;

      consumers = new ConcurrentHashMap<>();
      consumerQoSLevels = new ConcurrentHashMap<>();
   }

   void start() throws Exception
   {
      for (MqttTopicSubscription subscription : session.getSessionState().getSubscriptions())
      {
         SimpleString q = createQueueForSubscription(subscription.topicName(), subscription.qualityOfService().value());
         createConsumerForSubscriptionQueue(q, subscription.topicName(), subscription.qualityOfService().value());
      }
   }

   void stop(boolean clean) throws Exception
   {
      for (ServerConsumer consumer : consumers.values())
      {
         consumer.setStarted(false);
         consumer.disconnect();
         consumer.getQueue().removeConsumer(consumer);
         consumer.close(false);
      }

      if (clean)
      {
         // TODO delete queues
      }
   }

   /**
    * Creates a Queue if it doesn't already exist, based on a topic and address.  Returning the queue name.
    */
   private SimpleString createQueueForSubscription(String topic, int qos) throws Exception
   {
      String address = MQTTUtil.convertMQTTAddressFilterToCore(topic);
      SimpleString queue = getQueueNameForTopic(address);

      Queue q = session.getServer().locateQueue(queue);
      if (q == null)
      {
         session.getServerSession().createQueue(new SimpleString(address), queue, null, false, DURABLE_QUEUES && qos >= 0);
      }
      return queue;
   }

   /**
    * Creates a new consumer for the queue associated with a subscription
    */
   private void createConsumerForSubscriptionQueue(SimpleString queue, String topic, int qos) throws Exception
   {
      long cid = session.getServer().getStorageManager().generateID();

      ServerConsumer consumer = session.getServerSession().createConsumer(cid, queue, null, false, true, -1);
      consumer.setStarted(true);

      consumers.put(topic, consumer);
      consumerQoSLevels.put(cid, qos);
   }


   private void addSubscription(MqttTopicSubscription subscription) throws Exception
   {
      MqttTopicSubscription s = session.getSessionState().getSubscription(subscription.topicName());

      int qos = subscription.qualityOfService().value();
      String topic = subscription.topicName();

      session.getSessionState().addSubscription(subscription);

      SimpleString q = createQueueForSubscription(topic, qos);
      createConsumerForSubscriptionQueue(q, topic, qos);
      session.getRetainMessageManager().addRetainedMessagesToQueue(q, topic);
   }

   protected void removeSubscriptions(List<String> topics) throws Exception
   {
      for (String topic : topics)
      {
         removeSubscription(topic);
      }
   }

   private void removeSubscription(String address) throws Exception
   {
      ServerConsumer consumer = consumers.get(address);
      String internalAddress = MQTTUtil.convertMQTTAddressFilterToCore(address);
      SimpleString internalQueueName = getQueueNameForTopic(internalAddress);

      synchronized (this)
      {
         Queue queue = session.getConnection().getServer().locateQueue(internalQueueName);
         // FIXME we should be deleting the queue.
         queue.deleteQueue(true);
      }
      session.getSessionState().removeSubscription(address);
      consumers.remove(address);
      consumerQoSLevels.remove(consumer.getID());
      // FIXME we can't delete the queue or remove the consumer until all acks have been received.
      // TODO look for a callback "onQueueEmtpy".
      // TODO Is there any more cleanup required here?
   }

   private SimpleString getQueueNameForTopic(String topic)
   {
      return new SimpleString(session.getSessionState().getClientId() + "." + topic);
   }

   /**
    * As per MQTT Spec.  Subscribes this client to a number of MQTT topics.
    *
    * @param subscriptions
    * @return An array of integers representing the list of accepted QoS for each topic.
    *
    * @throws Exception
    */
   protected int[] addSubscriptions(List<MqttTopicSubscription> subscriptions) throws Exception
   {
      int[] qos = new int[subscriptions.size()];

      for (int i = 0; i < subscriptions.size(); i++)
      {
         addSubscription(subscriptions.get(i));
         // FIXME this accepts all QoS.  This may not be possible under certain configurations of the server.
         qos[i] = subscriptions.get(i).qualityOfService().value();
      }
      return qos;
   }

   protected Map<Long, Integer> getConsumerQoSLevels()
   {
      return consumerQoSLevels;
   }

   public ServerConsumer getConsumerForAddress(String address)
   {
      return consumers.get(address);
   }
}
