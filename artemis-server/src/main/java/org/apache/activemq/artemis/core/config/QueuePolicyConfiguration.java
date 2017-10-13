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
package org.apache.activemq.artemis.core.config;

import java.io.Serializable;

import org.apache.activemq.artemis.core.journal.EncodingSupport;
import org.apache.activemq.artemis.core.server.queue.policy.Policy;

public interface QueuePolicyConfiguration extends Serializable, EncodingSupport {

   enum TYPE {
      DEFAULT, LVQ, RING;

      public static TYPE getType(byte type) {
         switch (type) {
            case 0:
               return TYPE.DEFAULT;
            case 1:
               return TYPE.LVQ;
            case 2:
               return TYPE.RING;
            default:
               return null;
         }
      }

      public byte getType() {
         switch (this) {
            case DEFAULT:
               return 0;
            case LVQ:
               return 1;
            case RING:
               return 2;
            default:
               return -1;
         }
      }
   }

   TYPE getType();

   Policy getInstance();
}
