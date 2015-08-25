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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.activemq.artemis.core.io.SequentialFileFactory;
import org.apache.activemq.artemis.core.journal.EncodingSupport;
import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.core.journal.Journal;
import org.apache.activemq.artemis.core.journal.JournalLoadInformation;
import org.apache.activemq.artemis.core.journal.LoaderCallback;
import org.apache.activemq.artemis.core.journal.PreparedTransactionInfo;
import org.apache.activemq.artemis.core.journal.RecordInfo;
import org.apache.activemq.artemis.core.journal.TransactionFailureCallback;
import org.apache.activemq.artemis.core.journal.impl.JournalFile;
import org.apache.activemq.artemis.jdbc.store.records.JDBCDeleteRecord;
import org.apache.activemq.artemis.jdbc.store.records.JDBCInsertRecord;
import org.apache.activemq.artemis.jdbc.store.records.JDBCRecord;
import org.apache.activemq.artemis.utils.ExecutorFactory;

public class ActiveMQJDBCJournal implements Journal
{
   private static final String CREATE_TABLE_SQL = "CREATE TABLE JOURNAL " +
                                            "(id INT, recordType SMALLINT, record BLOB, txId BIGINT)";

   private static final String INSERT_RECORD_SQL = "INSERT INTO JOURNAL " +
                                             "(id,recordType,record,txId) " +
                                             "VALUES (?,?,?,?)";

   private static final String DELETE_RECORD_SQL = "DELETE FROM JOURNAL WHERE id = ?";

   private static final String SELECT_RECORD_SQL = "SELECT id, recordType, record, txId FROM JOURNAL";

   private static String COUNT_RECORD_SQL  = "SELECT COUNT(*) FROM JOURNAL";

   private static final  String DESTROY_JOURNAL_SQL  = "DROP TABLE JOURNAL";

   private static int USER_VERSION = 1;

   private Connection connection;

   private List<JDBCRecord> records;

   private PreparedStatement insertJournalRecords;

   private PreparedStatement deleteJournalRecords;

   private PreparedStatement selectJournalRecords;

   private PreparedStatement countJournalRecords;

   private final Object insertRecordsLock = new Object();

   private final Object deleteRecordsLock = new Object();

   private ScheduledSync scheduledSync;

   private ScheduledExecutorService executorService;

   public ActiveMQJDBCJournal(Connection connection, ExecutorFactory executorFactory) throws SQLException
   {
      this.connection = connection;
      setup();

      records = new ArrayList<JDBCRecord>();

      insertJournalRecords = connection.prepareStatement(INSERT_RECORD_SQL);
      deleteJournalRecords = connection.prepareStatement(DELETE_RECORD_SQL);
      selectJournalRecords = connection.prepareStatement(SELECT_RECORD_SQL);
      countJournalRecords = connection.prepareStatement(COUNT_RECORD_SQL);

      scheduledSync = new ScheduledSync(this);

   }

   public void setup() throws SQLException
   {
      // If JOURNAL table doesn't exist then create it
      ResultSet rs = connection.getMetaData().getTables(null, null, "JOURNAL", null);
      if (!rs.next())
      {
         Statement statement = connection.createStatement();
         statement.executeUpdate(CREATE_TABLE_SQL);
      }
   }

   public void destroy() throws SQLException
   {
      connection.setAutoCommit(false);
      Statement statement = connection.createStatement();
      statement.executeUpdate(DESTROY_JOURNAL_SQL);
      connection.commit();
   }

   public synchronized void sync() throws SQLException
   {
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

   // TODO run in separate thread, use an executor.
   private void executeCallbacks(List<JDBCRecord> records, boolean result)
   {
      if (result)
      {
         for (JDBCRecord r : records)
         {
            r.complete();
         }
      }
      else
      {
         for (JDBCRecord r : records)
         {
            r.error();
         }
      }
   }

   private void insertRecord(JDBCInsertRecord record) throws SQLException
   {
      synchronized (insertRecordsLock)
      {
         records.add(record);
      }
   }

   private void deleteRecord(JDBCDeleteRecord record)
   {
      synchronized (deleteRecordsLock)
      {
         records.add(record);
      }
   }

   @Override
   public void appendAddRecord(long id, byte recordType, byte[] record, boolean sync) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, sync, null);
      insertRecord(r);
   }

   @Override
   public void appendAddRecord(long id, byte recordType, EncodingSupport record, boolean sync) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, sync, null);
      insertRecord(r);
   }

   @Override
   public void appendAddRecord(long id, byte recordType, EncodingSupport record, boolean sync, IOCompletion completionCallback) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, sync, completionCallback);
      insertRecord(r);
   }

   @Override
   public void appendUpdateRecord(long id, byte recordType, byte[] record, boolean sync) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, sync, null);
      insertRecord(r);
   }

   @Override
   public void appendUpdateRecord(long id, byte recordType, EncodingSupport record, boolean sync) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, sync, null);
      insertRecord(r);
   }

   @Override
   public void appendUpdateRecord(long id, byte recordType, EncodingSupport record, boolean sync, IOCompletion completionCallback) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, sync, completionCallback);
      insertRecord(r);
   }

   @Override
   public void appendDeleteRecord(long id, boolean sync) throws Exception
   {
      JDBCDeleteRecord r = new JDBCDeleteRecord(id, sync, null);
      deleteRecord(r);
   }

   @Override
   public void appendDeleteRecord(long id, boolean sync, IOCompletion completionCallback) throws Exception
   {
      JDBCDeleteRecord r = new JDBCDeleteRecord(id, sync, completionCallback);
      deleteRecord(r);
   }

   @Override
   public void appendAddRecordTransactional(long txID, long id, byte recordType, byte[] record) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, txID, true, null);
      insertRecord(r);
   }

   @Override
   public void appendAddRecordTransactional(long txID, long id, byte recordType, EncodingSupport record) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, txID, true, null);
      insertRecord(r);
   }

   @Override
   public void appendUpdateRecordTransactional(long txID, long id, byte recordType, byte[] record) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, txID, true, null);
      insertRecord(r);
   }

   @Override
   public void appendUpdateRecordTransactional(long txID, long id, byte recordType, EncodingSupport record) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(id, recordType, record, txID, true, null);
      insertRecord(r);
   }

   @Override
   public void appendDeleteRecordTransactional(long txID, long id, byte[] record) throws Exception
   {
      JDBCDeleteRecord r = new JDBCDeleteRecord(id, txID, true, null);
      deleteRecord(r);
   }

   @Override
   public void appendDeleteRecordTransactional(long txID, long id, EncodingSupport record) throws Exception
   {
      JDBCDeleteRecord r = new JDBCDeleteRecord(id, txID, true, null);
      deleteRecord(r);
   }

   @Override
   public void appendDeleteRecordTransactional(long txID, long id) throws Exception
   {
      JDBCDeleteRecord r = new JDBCDeleteRecord(id, txID, true, null);
      deleteRecord(r);
   }

   @Override
   public void appendCommitRecord(long txID, boolean sync) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(txID, JDBCRecord.COMMIT_RECORD, sync, null);
      insertRecord(r);
   }

   @Override
   public void appendCommitRecord(long txID, boolean sync, IOCompletion callback) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(txID, JDBCRecord.COMMIT_RECORD, sync, callback);
      insertRecord(r);
   }

   @Override
   public void appendCommitRecord(long txID, boolean sync, IOCompletion callback, boolean lineUpContext) throws Exception
   {
      //FIXME what does line up context mean here?
      JDBCInsertRecord r = new JDBCInsertRecord(txID, JDBCRecord.COMMIT_RECORD, sync, callback, lineUpContext);
      insertRecord(r);
   }

   @Override
   public void appendPrepareRecord(long txID, EncodingSupport transactionData, boolean sync) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(txID, JDBCRecord.PREPARE_RECORD, transactionData, sync, null);
      insertRecord(r);
   }

   @Override
   public void appendPrepareRecord(long txID, EncodingSupport transactionData, boolean sync, IOCompletion callback) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(txID, JDBCRecord.PREPARE_RECORD, transactionData, sync, callback);
      insertRecord(r);
   }

   @Override
   public void appendPrepareRecord(long txID, byte[] transactionData, boolean sync) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(txID, JDBCRecord.PREPARE_RECORD, transactionData, sync, null);
      insertRecord(r);
   }

   @Override
   public void appendRollbackRecord(long txID, boolean sync) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(txID, JDBCRecord.ROLLBACK_RECORD, sync, null);
      insertRecord(r);
   }

   @Override
   public void appendRollbackRecord(long txID, boolean sync, IOCompletion callback) throws Exception
   {
      JDBCInsertRecord r = new JDBCInsertRecord(txID, JDBCRecord.ROLLBACK_RECORD, sync, callback);
      insertRecord(r);
   }

   @Override
   public JournalLoadInformation load(LoaderCallback reloadManager) throws Exception
   {
      JDBCInsertRecord record = null;
      int count = 0;

      try (ResultSet records = selectJournalRecords.executeQuery())
      {
         while (records.next())
         {
            record = JDBCInsertRecord.load(records);
            switch (record.getRecordType())
            {
               case JDBCRecord.ADD_RECORD:
                  reloadManager.addRecord(new RecordInfo(record.getId(),
                                                         record.getRecordType(),
                                                         record.getRecordData(),
                                                         false,
                                                         (short) 0));
               case JDBCRecord.UPDATE_RECORD:
                  reloadManager.updateRecord(new RecordInfo(record.getId(),
                                                            record.getRecordType(),
                                                            record.getRecordData(),
                                                            true,
                                                            (short) 0));

               default:
                  PreparedTransactionInfo p = new PreparedTransactionInfo(record.getTxId(), record.getRecordData());
                  reloadManager.addPreparedTransaction(p);
            }
            count++;
         }
      }
      return new JournalLoadInformation(count, count > 0 ? record.getId() : 0);
   }

   @Override
   public JournalLoadInformation loadInternalOnly() throws Exception
   {
      return null;
   }

   @Override
   public JournalLoadInformation loadSyncOnly(JournalState state) throws Exception
   {
      return null;
   }

   @Override
   public void lineUpContext(IOCompletion callback)
   {
      callback.storeLineUp();
   }

   @Override
   public JournalLoadInformation load(List<RecordInfo> committedRecords, List<PreparedTransactionInfo> preparedTransactions, TransactionFailureCallback transactionFailure) throws Exception
   {
      return null;
   }

   @Override
   public int getAlignment() throws Exception
   {
      return 0;
   }

   @Override
   public int getNumberOfRecords() throws SQLException
   {
      int count = 0;
      try (ResultSet rs = countJournalRecords.executeQuery())
      {
         rs.next();
         count = rs.getInt(1);
      }
      return count;
   }

   @Override
   public int getUserVersion()
   {
      return USER_VERSION;
   }

   @Override
   public void perfBlast(int pages)
   {

   }

   @Override
   public void runDirectJournalBlast() throws Exception
   {

   }

   @Override
   public Map<Long, JournalFile> createFilesForBackupSync(long[] fileIds) throws Exception
   {
      return null;
   }

   @Override
   public void synchronizationLock()
   {

   }

   @Override
   public void synchronizationUnlock()
   {

   }

   @Override
   public void forceMoveNextFile() throws Exception
   {

   }

   @Override
   public JournalFile[] getDataFiles()
   {
      return new JournalFile[0];
   }

   @Override
   public SequentialFileFactory getFileFactory()
   {
      return null;
   }

   @Override
   public int getFileSize()
   {
      return 0;
   }

   @Override
   public void scheduleCompactAndBlock(int timeout) throws Exception
   {

   }

   @Override
   public void replicationSyncPreserveOldFiles()
   {

   }

   @Override
   public void replicationSyncFinished()
   {

   }

   @Override
   public void start() throws Exception
   {

   }

   @Override
   public void stop() throws Exception
   {

   }

   @Override
   public boolean isStarted()
   {
      return false;
   }

}
