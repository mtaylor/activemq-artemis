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
import org.apache.activemq.artemis.core.config.QueuePolicyConfiguration;
import org.apache.activemq.artemis.utils.DataConstants;

public abstract class QueuePolicyConfigurationImpl implements QueuePolicyConfiguration {

   public static QueuePolicyConfiguration decodePolicy(ActiveMQBuffer buffer) {
      // For backwards compatability.
      if (buffer.readableBytes() > 0) {
         buffer.markWriterIndex();
         QueuePolicyConfiguration qpc;

         byte qpOrdinal = buffer.readByte();
         switch(QueuePolicyConfiguration.TYPE.getType(qpOrdinal)) {
            case DEFAULT: qpc = new DefaultQueuePolicyConfiguration(); break;
            case LVQ: qpc = new LVQPolicyConfiguration(); break;
            case RING: qpc = new RingQueuePolicyConfiguration(); break;
            default: qpc = new DefaultQueuePolicyConfiguration(); break;
         }
         buffer.resetWriterIndex();
         qpc.decode(buffer);
         return qpc;
      }
      return new DefaultQueuePolicyConfiguration();
   }


   @Override
   public void encode(ActiveMQBuffer buffer) {
      buffer.writeByte(getType().getType());
   }

   @Override
   public void decode(ActiveMQBuffer buffer) {
      buffer.readByte();
   }

   @Override
   public int getEncodeSize() {
      return DataConstants.SIZE_BYTE;
   }

   @Override
   public String toString() {
      return this.getClass().getName();
   }
}
