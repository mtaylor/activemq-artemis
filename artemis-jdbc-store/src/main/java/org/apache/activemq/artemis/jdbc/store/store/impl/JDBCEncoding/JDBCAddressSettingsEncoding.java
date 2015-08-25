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

package org.apache.activemq.artemis.jdbc.store.store.impl.JDBCEncoding;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.core.settings.impl.SlowConsumerPolicy;

public class JDBCAddressSettingsEncoding<T> implements JDBCEncodingSupport
{
   public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS ADDRESS_SETTINGS (id INT, " +
      "addressFullMessagePolicy VARCHAR," +
      "maxSizeBytes BIGINT, " +
      "pageSizeBytes BIGINT, " +
      "pageCacheMaxSize INT, " +
      "dropMessagesWhenFull BOOLEAN, " +
      "maxDeliveryAttempts INT, " +
      "messageCounterHistoryDayLimit INT, " +
      "redeliveryDelay BIGINT, " +
      "redeliveryMultiplier DOUBLE, " +
      "maxRedeliveryDelay BIGINT, " +
      "deadLetterAddress VARCHAR, " +
      "expiryAddress VARCHAR, " +
      "expiryDelay BIGINT, " +
      "lastValueQueue BOOLEAN, " +
      "redistributionDelay BIGINT, " +
      "sendToDLAOnNoRoute BOOLEAN, " +
      "slowConsumerThreshold BIGINT, " +
      "slowConsumerCheckPeriod BIGINT, " +
      "slowConsumerPolicy VARCHAR, " +
      "autoCreateJmsQueues BOOLEAN, " +
      "autoCreateJmsQueues BOOLEAN)";

   public static final String INSERT_RECORDS_SQL = "INSERT INTO ADDRESS_SETTINGS (id, addressFullMessagePolicy, maxSizeBytes, " +
      "pageSizeBytes, pageCacheMaxSize, dropMessagesWhenFull, maxDeliveryAttempts, messageCounterHistoryDayLimit, " +
      "redeliveryDelay, redeliveryMultiplier, maxRedeliveryDelay, deadLetterAddress, expiryAddress, expiretyDelay," +
      "lastValueQUeue, redistributionDelay, sendToDLAOnNoRoute, slowConsumerThreshold, slowConsumerCheckPeriod," +
      "slowConsumerPolicy, autoCreateJmsQueues, autoDeleteJmsQueues) " +
      "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


   public static final String DELETE_RECORDS_SQL = "DELETE FROM ADDRESS_SETTINGS WHERE id=?";

   private AddressSettings addressSettings;

   public JDBCAddressSettingsEncoding(AddressSettings addressSettings)
   {
      this.addressSettings = addressSettings;
   }

   @Override
   public void encode(PreparedStatement statement) throws SQLException
   {
      // generate id.
      statement.setLong(1, 1);
      statement.setString(2, addressSettings.getAddressFullMessagePolicy().toString());
      statement.setLong(3, addressSettings.getMaxSizeBytes());
      statement.setLong(4, addressSettings.getPageSizeBytes());
      statement.setInt(5, addressSettings.getPageCacheMaxSize());
      statement.setBoolean(6, addressSettings.isDropMessagesWhenFull());
      statement.setInt(7, addressSettings.getMaxDeliveryAttempts());
      statement.setInt(8, addressSettings.getMessageCounterHistoryDayLimit());
      statement.setLong(9, addressSettings.getRedeliveryDelay());
      statement.setDouble(10, addressSettings.getRedeliveryMultiplier());
      statement.setLong(11, addressSettings.getMaxRedeliveryDelay());
      statement.setString(12, addressSettings.getDeadLetterAddress().toString());
      statement.setString(13, addressSettings.getExpiryAddress().toString());
      statement.setLong(14, addressSettings.getExpiryDelay());
      statement.setBoolean(15, addressSettings.isLastValueQueue());
      statement.setLong(16, addressSettings.getRedistributionDelay());
      statement.setBoolean(17, addressSettings.isSendToDLAOnNoRoute());
      statement.setLong(18, addressSettings.getSlowConsumerThreshold());
      statement.setLong(19, addressSettings.getSlowConsumerCheckPeriod());
      statement.setString(20, addressSettings.getSlowConsumerPolicy().toString());
      statement.setBoolean(21, addressSettings.isAutoCreateJmsQueues());
      statement.setBoolean(22, addressSettings.isAutoDeleteJmsQueues());
   }

   @Override
   public AddressSettings decode(ResultSet resultSet) throws SQLException
   {
      AddressSettings settings = new AddressSettings();

      settings.setAddressFullMessagePolicy(AddressFullMessagePolicy.valueOf(resultSet.getString(2)));
      settings.setMaxSizeBytes(resultSet.getLong(3));
      settings.setPageSizeBytes(resultSet.getLong(4));
      settings.setPageCacheMaxSize(resultSet.getInt(5));
      settings.setDropMessageWhenFull(resultSet.getBoolean(6));
      settings.setMaxDeliveryAttempts(resultSet.getInt(7));
      settings.setMessageCounterHistoryDayLimit(resultSet.getInt(8));
      settings.setRedeliveryDelay(resultSet.getLong(9));
      settings.setRedeliveryMultiplier(resultSet.getDouble(10));
      settings.setMaxRedeliveryDelay(resultSet.getLong(11));
      settings.setDeadLetterAddress(new SimpleString(resultSet.getString(12)));
      settings.setExpiryAddress(new SimpleString(resultSet.getString(13)));
      settings.setExpiryDelay(resultSet.getLong(14));
      settings.setLastValueQueue(resultSet.getBoolean(15));
      settings.setRedistributionDelay(resultSet.getLong(16));
      settings.setSendToDLAOnNoRoute(resultSet.getBoolean(17));
      settings.setSlowConsumerThreshold(resultSet.getLong(18));
      settings.setSlowConsumerCheckPeriod(resultSet.getLong(19));
      settings.setSlowConsumerPolicy(SlowConsumerPolicy.valueOf(resultSet.getString(20)));
      settings.setAutoCreateJmsQueues(resultSet.getBoolean(21));
      settings.setAutoDeleteJmsQueues(resultSet.getBoolean(22));

      resultSet.next();

      return settings;
   }
}