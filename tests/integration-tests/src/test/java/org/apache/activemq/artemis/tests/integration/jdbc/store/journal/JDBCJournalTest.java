package org.apache.activemq.artemis.tests.integration.jdbc.store.journal;

import org.apache.activemq.artemis.jdbc.store.ActiveMQJDBCJournal;
import org.apache.activemq.artemis.jdbc.store.ActiveMQJDBCStorageManager;
import org.apache.activemq.artemis.jdbc.store.records.JDBCRecord;
import org.junit.After;
import org.junit.Test;

public class JDBCJournalTest
{
   private ActiveMQJDBCJournal journal;

   private ActiveMQJDBCStorageManager storageManager;

   @Test
   public void testAppendRecords() throws Exception
   {
      ActiveMQJDBCStorageManager storageManager = new ActiveMQJDBCStorageManager(null, null);
      journal = storageManager.getJournal();

      journal.appendAddRecord(1, JDBCRecord.ADD_RECORD, new byte[0], true);
      journal.sync();

   }

   @After
   public void cleanDB()
   {

   }
}
