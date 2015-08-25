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

package org.apache.activemq.artemis.jdbc.store;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.utils.ExecutorFactory;

public class ActiveMQJDBCStorageManager //extends JournalJDBCStorageManager
{
   private String jdbcUrl;

   private Properties jdbcConnectionProperties;

   private java.sql.Connection dbConnection;

   private Driver dbDriver;

   private ActiveMQJDBCJournal journal;

   public ActiveMQJDBCStorageManager(Configuration config, ExecutorFactory executorFactory) throws SQLException
   {

      //super(config, executorFactory);
      loadConfig(config);

      // TODO Load params from config
      List<Driver> drivers = Collections.list(DriverManager.getDrivers());
      if (drivers.size() == 1)
      {
         dbDriver = drivers.get(0);
         dbConnection = dbDriver.connect(jdbcUrl, jdbcConnectionProperties);
      }
      else
      {
         String error = drivers.isEmpty() ? "No DB driver found on class path" : "Too many DB drivers on class path";
         throw new RuntimeException(error);
      }
      journal = new ActiveMQJDBCJournal(dbConnection, executorFactory);
   }

   private void loadConfig(Configuration config)
   {
      jdbcUrl = "jdbc:derby:derbyDB;create=true";
      jdbcConnectionProperties = new Properties();
   }

   public ActiveMQJDBCJournal getJournal()
   {
      return journal;
   }
}
