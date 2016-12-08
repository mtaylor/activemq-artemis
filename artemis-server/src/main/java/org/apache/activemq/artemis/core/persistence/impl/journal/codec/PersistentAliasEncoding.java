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
package org.apache.activemq.artemis.core.persistence.impl.journal.codec;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.journal.EncodingSupport;
import org.apache.activemq.artemis.core.persistence.AliasBindingInfo;
import org.apache.activemq.artemis.core.server.impl.Alias;

public class PersistentAliasEncoding implements EncodingSupport, AliasBindingInfo {

   public Alias alias;

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("PersistentAliasEncoding [id=" + alias.getId());
      sb.append(", fromAddress=" + alias.getFromAddress());
      sb.append(", toAddress=" + alias.getToAddress());
      sb.append("]");
      return sb.toString();
   }

   public PersistentAliasEncoding() {
   }

   public PersistentAliasEncoding(Alias alias) {
      this.alias = alias;
   }

   @Override
   public long getId() {
      return alias.getId();
   }

   @Override
   public void setId(long id) {
      alias.setId(id);
   }

   @Override
   public SimpleString getFromAddress() {
      return alias.getFromAddress();
   }

   @Override
   public SimpleString getToAddress() {
      return alias.getToAddress();
   }

   @Override
   public void decode(final ActiveMQBuffer buffer) {
      SimpleString fromAddress = buffer.readSimpleString();
      SimpleString toAddress = buffer.readSimpleString();
      alias = new Alias(fromAddress, toAddress);
   }

   @Override
   public void encode(final ActiveMQBuffer buffer) {
      buffer.writeSimpleString(alias.getFromAddress());
      buffer.writeSimpleString(alias.getToAddress());
   }

   @Override
   public int getEncodeSize() {
      return SimpleString.sizeofString(alias.getFromAddress()) + SimpleString.sizeofString(alias.getToAddress());
   }
}
