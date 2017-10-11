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

import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.queue.policy.Policy;

public class Default implements Policy {

   protected Queue queue;

   public Default() {
   }

   @Override
   public void setQueue(Queue queue) {
      this.queue = queue;
   }

   @Override
   public MessageReference beforeAddTail(MessageReference ref, boolean direct) {
      return ref;
   }

   @Override
   public MessageReference beforeAddHead(MessageReference ref, boolean scheduling) {
      return ref;
   }

   @Override
   public boolean beforeRefRemoved(MessageReference ref) {
      return true;
   }
}
