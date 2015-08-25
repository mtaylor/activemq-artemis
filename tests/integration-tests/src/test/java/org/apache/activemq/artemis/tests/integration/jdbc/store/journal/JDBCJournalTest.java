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

package org.apache.activemq.artemis.tests.integration.jdbc.store.journal;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;

import org.apache.activemq.artemis.jdbc.store.ActiveMQJDBCJournal;
import org.apache.activemq.artemis.jdbc.store.ActiveMQJDBCStorageManager;
import org.apache.activemq.artemis.jdbc.store.records.JDBCRecord;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JDBCJournalTest
{
   private ActiveMQJDBCJournal journal;

   private ActiveMQJDBCStorageManager storageManager;

   @Test
   public void testInsertRecords() throws Exception
   {
      storageManager = new ActiveMQJDBCStorageManager(null, null);
      journal = storageManager.getJournal();

      int noRecords = 10;
      for (int i=0; i<noRecords; i ++)
      {
         journal.appendAddRecord(1, JDBCRecord.ADD_RECORD, new byte[0], true);
      }

      assertEquals(0, journal.getNumberOfRecords());
      journal.sync();
      assertEquals(noRecords, journal.getNumberOfRecords());
   }

//   @Test
//   public void testLoadJournal()
//   {
////      final CountDownLatch addRecords = new CountDownLatch(50);
////      final CountDownLatch updateRecords = new CountDownLatch(50);
////      final CountDownLatch txRecords = new CountDownLatch(50);
//   }

   @After
   public void tearDown() throws SQLException
   {
      journal.destroy();
   }
}
