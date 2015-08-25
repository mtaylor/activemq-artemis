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

package org.apache.activemq.artemis.jdbc.store.store;

import java.util.List;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.journal.JournalLoadInformation;
import org.apache.activemq.artemis.core.persistence.GroupingInfo;
import org.apache.activemq.artemis.core.persistence.QueueBindingInfo;
import org.apache.activemq.artemis.core.persistence.config.PersistedAddressSetting;
import org.apache.activemq.artemis.core.persistence.config.PersistedRoles;
import org.apache.activemq.artemis.core.postoffice.Binding;
import org.apache.activemq.artemis.core.server.ActiveMQComponent;
import org.apache.activemq.artemis.core.server.group.impl.GroupBinding;

public interface BindingsStore extends ActiveMQComponent
{
   void commitBindings(final long txID) throws Exception;

   void rollbackBindings(final long txID) throws Exception;

   void storeAddressSetting(PersistedAddressSetting addressSetting) throws Exception;

   void storeSecurityRoles(PersistedRoles persistedRoles) throws Exception;

   void storeID(final long journalID, final long id) throws Exception;

   void deleteID(long journalD) throws Exception;

   void deleteAddressSetting(SimpleString addressMatch) throws Exception;

   void deleteSecurityRoles(SimpleString addressMatch) throws Exception;

   void addGrouping(final GroupBinding groupBinding) throws Exception;

   void deleteGrouping(long tx, final GroupBinding groupBinding) throws Exception;

   void addQueueBinding(final long tx, final Binding binding) throws Exception;

   void deleteQueueBinding(long tx, final long queueBindingID);

   JournalLoadInformation load(List<QueueBindingInfo> queueBindingInfos, List<GroupingInfo> groupingInfos) throws Exception;
}
