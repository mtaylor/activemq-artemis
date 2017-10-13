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
package org.apache.activemq.artemis.core.server.queue.policy.impl;

import org.apache.activemq.artemis.core.filter.Filter;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.impl.QueueImpl;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.utils.collections.LinkedListIterator;

public class Ring extends Default {

   private long maxSizeBytes;

   private long maxNoMessages;

   public Ring(long maxSizeBytes, long maxNoMessages) {
      this.maxSizeBytes = maxSizeBytes;
      this.maxNoMessages = maxNoMessages;
   }

   private boolean isFull(int nextMessageSize) {
      return (queue.getMessageCount() + 1 >= maxSizeBytes) || (queue.sizeBytes() + nextMessageSize >= maxSizeBytes);
   }

   @Override
   public MessageReference beforeAddTail(final MessageReference ref, boolean direct) throws Exception {
      int memoryEstimate = ref.getMessageMemoryEstimate();
      try (LinkedListIterator<MessageReference> iter = queue.iterator()) {
         while (isFull(memoryEstimate) && iter.hasNext()) {
            queue.sendToDeadLetterAddress(null, iter.next());
         }
         queue.referenceHandled();
      }
      return ref;
   }

   @Override
   public MessageReference beforeAddHead(MessageReference ref, boolean scheduling) throws Exception {
      int memoryEstimate = ref.getMessageMemoryEstimate();
      if (isFull(memoryEstimate)) {
         queue.referenceHandled();
         try {
            queue.acknowledge(ref);
         } catch (Exception e) {
            ActiveMQServerLogger.LOGGER.errorAckingOldReference(e);
         }
         return null;
      }
      return ref;
   }

   @Override
   public boolean beforeRefRemoved(MessageReference ref) {
      return true;
   }

}
