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
package org.apache.activemq.artemis.api.core.management;

/**
 * An AddressControl is used to manage an address.
 */
public interface AliasControl {

   /**
    * Returns the Address of this Alias.
    */
   @Attribute(desc = "The address of this Alias")
   String getFromAddress();

   /**
    * Returns the Address that this Alias is mapped to.
    */
   @Attribute(desc = "The actual address that this alias is mapped to")
   String getToAddress();

   /**
    * Returns the roles (name and permissions) associated with the underlying address of this Alias
    */
   @Attribute(desc = "roles (name and permissions) associated with this alias")
   Object[] getRoles() throws Exception;

   /**
    * Returns the roles  (name and permissions) associated with the underlying address of this Alias
    * using JSON serialization.
    * <br>
    * Java objects can be recreated from JSON serialization using {@link RoleInfo#from(String)}.
    */
   @Attribute(desc = "roles  (name and permissions) associated with this alias using JSON serialization")
   String getRolesAsJSON() throws Exception;
}
