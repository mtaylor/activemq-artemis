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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.jdbc.store.records.JDBCRecord;

public class JDBCDeleteRecord extends JDBCRecord
{
   private long txId;

   protected static String SQL = "DELETE FROM JOURNAL WHERE id=? OR txId=?";

   public JDBCDeleteRecord(long id, boolean sync, IOCompletion ioCompletion)
   {
      super(id, JDBCRecord.DELETE_RECORD, sync, ioCompletion);
      txId = -2;
   }

   public JDBCDeleteRecord(long id, long txId, boolean sync, IOCompletion completion)
   {
      super(id, JDBCRecord.DELETE_RECORD, sync, completion);
      this.txId = txId;
   }
   @Override
   public void addToStatement(PreparedStatement statement) throws SQLException
   {
      statement.setLong(1, id);
      statement.setLong(2, txId);
      statement.addBatch();
   }

   @Override
   public boolean getInsert()
   {
      return false;
   }
}
