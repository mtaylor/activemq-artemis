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

package org.apache.activemq.artemis.jdbc.store.records;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
import org.apache.activemq.artemis.core.journal.EncodingSupport;
import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.utils.ActiveMQBufferInputStream;

public class JDBCInsertRecord extends JDBCRecord
{
   private InputStream record;

   private byte[] data;

   private long txId = -1;

   public static String SQL = "INSERT INTO JOURNAL " +
      "(id,recordType,record,txId) " +
      "VALUES (?,?,?,?)";

   public static JDBCInsertRecord load(ResultSet r) throws SQLException
   {
      JDBCInsertRecord record = new JDBCInsertRecord(r.getLong(1),
                                                     r.getByte(2),
                                                     r.getBytes(3),
                                                     r.getLong(4));
      r.next();

      return record;
   }

   public JDBCInsertRecord(long id, byte recordType, boolean sync, IOCompletion ioCompletion)
   {
      super (id, recordType, sync, ioCompletion);
   }

   public JDBCInsertRecord(long id, byte recordType, byte[] record, long txId)
   {
      this(id, recordType, false, null);
      this.data = record;
      this.txId = txId;
   }

   public JDBCInsertRecord(long id, byte recordType, boolean sync, IOCompletion ioCompletion, boolean storeLineUp)
   {
      super (id, recordType, sync, ioCompletion, storeLineUp);
   }

   public JDBCInsertRecord(long id, byte recordType, byte[] record, boolean sync, IOCompletion ioCompletion)
   {
      this(id, recordType, sync, ioCompletion);
      this.record = new ByteArrayInputStream(record);
   }

   public JDBCInsertRecord(long id, byte recordType, EncodingSupport record, boolean sync, IOCompletion ioCompletion)
   {
      this(id, recordType, sync, ioCompletion);

      ActiveMQBuffer buffer = ActiveMQBuffers.fixedBuffer(record.getEncodeSize());
      record.encode(buffer);
      this.record = new ActiveMQBufferInputStream(buffer);
   }

   public JDBCInsertRecord(long id, byte recordType, byte[] record, long txId, boolean sync, IOCompletion ioCompletion)
   {
      this(id, recordType, record, sync, ioCompletion);
      this.txId = txId;
   }

   public JDBCInsertRecord(long id, byte recordType, EncodingSupport record, long txId, boolean sync, IOCompletion ioCompletion)
   {
      this(id, recordType, record, sync, ioCompletion);
      this.txId = txId;
   }

   @Override
   public void addToStatement(PreparedStatement statement) throws SQLException
   {
      statement.setLong(1, id);
      statement.setByte(2, recordType);
      statement.setBinaryStream(3, record);
      statement.setLong(4, txId);
      statement.addBatch();
   }

   @Override
   public boolean getInsert()
   {
      return true;
   }

   public byte[] getRecordData()
   {
      return data;
   }

   public long getTxId()
   {
      return txId;
   }
}