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
package org.apache.activemq.artemis.core.config.queue.policy;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.core.server.queue.policy.Policy;
import org.apache.activemq.artemis.core.server.queue.policy.impl.Ring;
import org.apache.activemq.artemis.utils.DataConstants;

public class RingQueuePolicyConfiguration extends QueuePolicyConfigurationImpl {

   private long maxSizeBytes = Long.MAX_VALUE;

   private long maxMessages = Long.MAX_VALUE;

   @Override
   public TYPE getType() {
      return null;
   }

   @Override
   public Policy getInstance() {
      return new Ring(maxSizeBytes, maxMessages);
   }

   public long getMaxMessages() {
      return maxMessages;
   }

   public void setMaxMessages(long maxMessages) {
      this.maxMessages = maxMessages;
   }

   public long getMaxSizeBytes() {
      return maxSizeBytes;
   }

   public void setMaxSizeBytes(long maxSizeBytes) {
      this.maxSizeBytes = maxSizeBytes;
   }

   @Override
   public int getEncodeSize() {
      return DataConstants.SIZE_BYTE +
         DataConstants.SIZE_LONG +
         DataConstants.SIZE_LONG;
   }

   @Override
   public void encode(ActiveMQBuffer buffer) {
      buffer.writeByte(getType().getType());
      buffer.writeLong(maxSizeBytes);
      buffer.writeLong(maxMessages);
   }

   @Override
   public void decode(ActiveMQBuffer buffer) {
      buffer.readByte();
      maxSizeBytes = buffer.readLong();
      maxMessages = buffer.readLong();
   }
}
