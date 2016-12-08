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
package org.apache.activemq.artemis.core.management.impl;

import javax.json.JsonArrayBuilder;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import java.util.Set;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.management.AliasControl;
import org.apache.activemq.artemis.core.persistence.StorageManager;
import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.utils.JsonLoader;

public class AliasControlImpl extends AbstractControl implements AliasControl {

   private SimpleString fromAddress;

   private SimpleString toAddress;

   private HierarchicalRepository<Set<Role>> securityRepository;

   public AliasControlImpl(final SimpleString fromAddress,
                           final SimpleString toAddress,
                           final StorageManager storageManager,
                           final HierarchicalRepository<Set<Role>> securityRepository) throws Exception {
      super(AliasControl.class, storageManager);
      this.fromAddress = fromAddress;
      this.toAddress = toAddress;
      this.securityRepository = securityRepository;
   }

   /**
    * Returns the Address of this Alias.
    */
   @Override
   public String getFromAddress() {
      return fromAddress.toString();
   }

   /**
    * Returns the Address that this Alias is mapped to.
    */
   @Override
   public String getToAddress() {
      return toAddress.toString();
   }

   @Override
   public Object[] getRoles() throws Exception {
      clearIO();
      try {
         Set<Role> roles = securityRepository.getMatch(toAddress.toString());

         Object[] objRoles = new Object[roles.size()];

         int i = 0;
         for (Role role : roles) {
            objRoles[i++] = new Object[]{role.getName(), CheckType.SEND.hasRole(role), CheckType.CONSUME.hasRole(role), CheckType.CREATE_DURABLE_QUEUE.hasRole(role), CheckType.DELETE_DURABLE_QUEUE.hasRole(role), CheckType.CREATE_NON_DURABLE_QUEUE.hasRole(role), CheckType.DELETE_NON_DURABLE_QUEUE.hasRole(role), CheckType.MANAGE.hasRole(role)};
         }
         return objRoles;
      } finally {
         blockOnIO();
      }
   }

   @Override
   public String getRolesAsJSON() throws Exception {
      clearIO();
      try {
         JsonArrayBuilder json = JsonLoader.createArrayBuilder();
         Set<Role> roles = securityRepository.getMatch(toAddress.toString());

         for (Role role : roles) {
            json.add(role.toJson());
         }
         return json.build().toString();
      } finally {
         blockOnIO();
      }
   }

   @Override
   protected MBeanOperationInfo[] fillMBeanOperationInfo() {
      return MBeanInfoHelper.getMBeanOperationsInfo(AliasControl.class);
   }

   @Override
   protected MBeanAttributeInfo[] fillMBeanAttributeInfo() {
      return MBeanInfoHelper.getMBeanAttributesInfo(AliasControl.class);
   }
}
