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

package org.apache.activemq.artemis.jdbc.store.store.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.journal.JournalLoadInformation;
import org.apache.activemq.artemis.core.persistence.GroupingInfo;
import org.apache.activemq.artemis.core.persistence.QueueBindingInfo;
import org.apache.activemq.artemis.core.persistence.config.PersistedAddressSetting;
import org.apache.activemq.artemis.core.persistence.config.PersistedRoles;
import org.apache.activemq.artemis.core.postoffice.Binding;
import org.apache.activemq.artemis.core.server.group.impl.GroupBinding;
import org.apache.activemq.artemis.jdbc.store.records.JDBCRecord;
import org.apache.activemq.artemis.jdbc.store.store.BindingsStore;
import org.apache.activemq.artemis.jdbc.store.store.impl.JDBCEncoding.JDBCAddressSettingsEncoding;

public class JDBCBindingsStore implements BindingsStore
{
   public static final String CREATE_ADDRESS_SETTINGS = "CREATE TABLE ADDRESS_SETTINGS (id INT)";

   public static final String INSERT_ADDRESS_SETTINGS = "INSERT INTO ADDRESS_SETTINGS (id) VALUES (?)";

   public static final String DELETE_ADDRESS_SETTINGS = "DELETE FROM ADDRESS_SETTINGS WHERE id=?";

   private Connection connection;

   private ConcurrentHashMap<SimpleString, PersistedAddressSetting> persistedAddressSettings = new ConcurrentHashMap<SimpleString, PersistedAddressSetting>();

   private PreparedStatement insertAddressSettingsStmt;

   private PreparedStatement deleteAddressSettings;

   private boolean started = false;

   private List<JDBCAddressSettingsEncoding> insertAddressSettings = Collections.synchronizedList(new ArrayList<JDBCAddressSettingsEncoding>());

   public JDBCBindingsStore(Connection connection)
   {
      this.connection = connection;
   }

   public void sync()
   {
      public synchronized void sync() throws SQLException
      {
         synchronized (insertAddressSettingsStmt)
         {
            for (JDBCAddressSettingsEncoding e : insertAddressSettingsStmt)
            {
               e.encode(insertAddressSettingsStmt);
            }
         }
         List<JDBCRecord> localRecords;

         synchronized (insertRecordsLock)
         {
            localRecords = records;
            records = new ArrayList<JDBCRecord>();
         }

         for (JDBCRecord record : localRecords)
         {
            record.addToStatement(record.getInsert() ? insertJournalRecords : deleteJournalRecords);
            record.storeLineUp();
         }

         try
         {
            connection.setAutoCommit(false);
            insertJournalRecords.executeBatch();
            deleteJournalRecords.executeBatch();
            connection.commit();

            executeCallbacks(localRecords, true);
         }
         catch (SQLException e)
         {
            connection.rollback();
            executeCallbacks(localRecords, false);
         }
      }
   }

   @Override
   public void start() throws SQLException
   {
      Statement statement = connection.createStatement();
      statement.executeUpdate(JDBCAddressSettingsEncoding.CREATE_TABLE_SQL);

      insertAddressSettingsStmt = connection.prepareStatement(JDBCAddressSettingsEncoding.INSERT_RECORDS_SQL);
      deleteAddressSettings = connection.prepareStatement(JDBCAddressSettingsEncoding.DELETE_RECORDS_SQL);

      started = true;
   }

   @Override
   public void stop() throws Exception
   {
      connection.close();
      started = false;
   }

   @Override
   public boolean isStarted()
   {
      return started;
   }

   @Override
   public void commitBindings(long txID) throws Exception
   {

   }

   @Override
   public void rollbackBindings(long txID) throws Exception
   {

   }

   @Override
   public void storeAddressSetting(PersistedAddressSetting addressSetting) throws Exception
   {

   }

   @Override
   public void storeSecurityRoles(PersistedRoles persistedRoles) throws Exception
   {
   }

   @Override
   public void storeID(long journalID, long id) throws Exception
   {

   }

   @Override
   public void deleteID(long journalD) throws Exception
   {

   }

   @Override
   public void deleteAddressSetting(SimpleString addressMatch) throws Exception
   {
      try
      {
         //read lock
         PersistedAddressSetting deletedSetting = persistedAddressSettings.remove(addressMatch);
         // DELETE FROM ADDRESS_SETTINGS
      }
      catch(SQLException e)
      {

      }
      finally
      {
         //read unlock
      }

      // DELETE FROM ADDRESS_SETTINGS WHERE ID =
   }

   @Override
   public void deleteSecurityRoles(SimpleString addressMatch) throws Exception
   {

   }

   @Override
   public void addGrouping(GroupBinding groupBinding) throws Exception
   {

   }

   @Override
   public void deleteGrouping(long tx, GroupBinding groupBinding) throws Exception
   {

   }

   @Override
   public void addQueueBinding(long tx, Binding binding) throws Exception
   {

   }

   @Override
   public void deleteQueueBinding(long tx, long queueBindingID)
   {

   }

   @Override
   public JournalLoadInformation load(List<QueueBindingInfo> queueBindingInfos, List<GroupingInfo> groupingInfos) throws Exception
   {
      return null;
   }
}
