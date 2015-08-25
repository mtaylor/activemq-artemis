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

public abstract class JDBCRecord
{
   // Record types taken from Journal Impl
   public static final byte ADD_RECORD = 11;
   public static final byte UPDATE_RECORD = 12;
   public static final byte ADD_RECORD_TX = 13;
   public static final byte UPDATE_RECORD_TX = 14;
   public static final byte DELETE_RECORD_TX = 15;
   public static final byte DELETE_RECORD = 16;
   public static final byte PREPARE_RECORD = 17;
   public static final byte COMMIT_RECORD = 18;
   public static final byte ROLLBACK_RECORD = 19;

   protected IOCompletion ioCompletion;

   protected Long id;

   protected boolean sync;

   protected byte recordType;

   protected boolean storeLineUp;

   public JDBCRecord(long id, byte recordType, boolean sync, IOCompletion ioCompletion)
   {
      this.id = id;
      this.ioCompletion = ioCompletion;
      this.sync = sync;
   }

   public JDBCRecord(long id, byte recordType, boolean sync, IOCompletion ioCompletion, boolean storeLineUp)
   {
      this.id = id;
      this.ioCompletion = ioCompletion;
      this.sync = sync;
      this.storeLineUp = storeLineUp;
   }

   public void complete()
   {
      if (ioCompletion != null)
      {
         ioCompletion.done();
      }
   }

   public void error()
   {
      if (ioCompletion != null)
      {
         ioCompletion.done();
      }
   }

   public void storeLineUp()
   {
      if (storeLineUp && ioCompletion != null)
      {
         ioCompletion.storeLineUp();
      }
   }

   public abstract void addToStatement(PreparedStatement statement) throws SQLException;

   /**
    * Returns whether or not this record is a delete or insert.
    */
   public abstract boolean getInsert();

   public byte getRecordType()
   {
      return recordType;
   }
   
   public boolean getStoreLineUp()
   {
      return storeLineUp;
   }

   public Long getId()
   {
      return id;
   }
}
