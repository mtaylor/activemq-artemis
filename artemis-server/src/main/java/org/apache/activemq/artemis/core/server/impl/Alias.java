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
package org.apache.activemq.artemis.core.server.impl;

import org.apache.activemq.artemis.api.core.SimpleString;

public final class Alias {

   private long id;

   private final SimpleString fromAddress;

   private final SimpleString toAddress;

   public Alias(SimpleString  fromAddress, SimpleString toAddress) {
      this.fromAddress = fromAddress;
      this.toAddress = toAddress;
   }
   public SimpleString getToAddress() {
      return toAddress;
   }

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public SimpleString getFromAddress() {
      return fromAddress;
   }

   @Override
   public boolean equals(final Object other) {
      if (this == other) {
         return true;
      }
      if (!(other instanceof Alias))
         return false;

      Alias aother = (Alias) other;

      if (!fromAddress.equals(aother.getFromAddress())) {
         return false;
      }

      if (!toAddress.equals(aother.getToAddress())) {
         return false;
      }
      return true;
   }

   @Override
   public String toString() {
      StringBuffer buff = new StringBuffer();
      buff.append("Alias [id=" + id);
      buff.append(", fromAddress=" + fromAddress);
      buff.append(", toAddress=" + toAddress);
      buff.append("]");
      return buff.toString();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * ((fromAddress == null) ? 0 : fromAddress.hashCode());
      result = prime * ((toAddress == null) ? 0 : toAddress.hashCode());
      return result;
   }

}
