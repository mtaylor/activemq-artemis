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
package org.apache.activemq.artemis.jdbc.store.journal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.activemq.artemis.core.journal.RecordInfo;
import org.apache.activemq.artemis.core.journal.impl.SimpleWaitIOCallback;
import org.apache.activemq.artemis.journal.ActiveMQJournalLogger;

public class JDBCWriter {

   // Sync Delay in ms
   public static final int SYNC_DELAY = 5;

   private static final AtomicInteger SYNC_RUN = new AtomicInteger(0);

   private Connection connection;

   private List<JDBCJournalRecord> records = new ArrayList<JDBCJournalRecord>();

   private final ReadWriteLock journalLock = new ReentrantReadWriteLock();

   private Map<Long, TransactionHolder> transactions;

   private boolean started = false;

   private final String tableName;

   private PreparedStatement insertJournalRecords;

   private PreparedStatement deleteJournalRecords;

   private PreparedStatement deleteJournalTxRecords;

   private List<Long> inFlightTx;

   private List<Long> inFlightRecords;

   private Timer syncTimer;

   public JDBCWriter(Connection connection, Map<Long, TransactionHolder> transactions, String tableName) {
      this.connection = connection;
      this.transactions = transactions;
      this.tableName = tableName;
      this.inFlightRecords = new ArrayList<>();
      this.inFlightTx = new ArrayList<>();
   }

   public synchronized boolean isOwner(JDBCJournalRecord record) {
      return (inFlightTx.contains(record.getTxId()) || inFlightRecords.contains(record.getId()));
   }

   public boolean isStarted() {
      return started;
   }

   public void start() throws SQLException {
      started = true;

      insertJournalRecords = connection.prepareStatement(JDBCJournalRecord.insertRecordsSQL(tableName));
      deleteJournalRecords = connection.prepareStatement(JDBCJournalRecord.deleteRecordsSQL(tableName));
      deleteJournalTxRecords = connection.prepareStatement(JDBCJournalRecord.deleteJournalTxRecordsSQL(tableName));

      String timerThread = "Timer JDBC Journal(" + tableName + ")";
      syncTimer = new Timer(timerThread, true);
      syncTimer.schedule(new JDBCJournalSync(this), SYNC_DELAY * 2, SYNC_DELAY);
   }

   public synchronized void appendRecord(JDBCJournalRecord record) throws Exception {

      SimpleWaitIOCallback callback = null;
      if (record.isSync() && record.getIoCompletion() == null) {
         callback = new SimpleWaitIOCallback();
         record.setIoCompletion(callback);
      }

      try {
         journalLock.writeLock().lock();
         if (record.isTransactional() || record.getRecordType() == JDBCJournalRecord.PREPARE_RECORD) {
            addTxRecord(record);
         }
         records.add(record);
      }
      finally {
         journalLock.writeLock().unlock();
      }

      if (callback != null) callback.waitCompletion();
   }

   private void addTxRecord(JDBCJournalRecord record) {
      TransactionHolder txHolder = transactions.get(record.getTxId());
      if (txHolder == null) {
         txHolder = new TransactionHolder(record.getTxId());
         transactions.put(record.getTxId(), txHolder);
         System.out.println("Added Tx: " + record.getTxId());
      }

      // We actually only need the record ID in this instance.
      if (record.isTransactional()) {
         RecordInfo info = new RecordInfo(record.getId(), record.getRecordType(), new byte[0], record.isUpdate(), record.getCompactCount());
         if (record.getRecordType() == JDBCJournalRecord.DELETE_RECORD_TX) {
            txHolder.recordsToDelete.add(info);
         }
         else {
            txHolder.recordInfos.add(info);
         }
      }
      else {
         txHolder.prepared = true;
      }
   }

   public void stop() {
      journalLock.writeLock().lock();
      sync();
      journalLock.writeLock().unlock();
      started = false;
   }

   public int sync() {
      SYNC_RUN.addAndGet(1);

      if (!started)
         return 0;

      List<JDBCJournalRecord> recordRef = records;
      records = new ArrayList<JDBCJournalRecord>();

      // We keep a list of deleted records and committed tx (used for cleaning up old transaction data).
      List<Long> deletedRecords = new ArrayList<>();
      List<Long> committedTransactions = new ArrayList<>();

      TransactionHolder holder;

      boolean success = false;
      try {
         for (JDBCJournalRecord record : recordRef) {
            record.storeLineUp();

            switch (record.getRecordType()) {
               case JDBCJournalRecord.DELETE_RECORD:
                  // Standard SQL Delete Record, Non transactional delete
                  deletedRecords.add(record.getId());
                  //record.writeRecord(insertJournalRecords);
                  record.writeDeleteRecord(deleteJournalRecords);
                  break;
               case JDBCJournalRecord.ROLLBACK_RECORD:
                  // Roll back we remove all records associated with this TX ID.  This query is always performed last.
                  deleteJournalTxRecords.setLong(1, record.getTxId());
                  deleteJournalTxRecords.addBatch();
                  break;
               case JDBCJournalRecord.COMMIT_RECORD:
                  // We perform all the deletes and add the commit record in the same Database TX
                  System.out.println("Looking for Tx: " + record.getTxId());
                  holder = transactions.get(record.getTxId());
                  for (RecordInfo info : holder.recordsToDelete) {
                     deletedRecords.add(record.getId());
                     deleteJournalRecords.setLong(1, info.id);
                     deleteJournalRecords.addBatch();
                  }
                  record.writeRecord(insertJournalRecords);
                  committedTransactions.add(record.getTxId());
                  break;
               default:
                  // Default we add a new record to the DB
                  record.writeRecord(insertJournalRecords);
                  break;
            }
         }
      }
      catch (SQLException e) {
         executeCallbacks(recordRef, success);
         return 0;
      }

      try {
         connection.setAutoCommit(false);

         insertJournalRecords.executeBatch();

         if (SYNC_RUN.intValue() > 1000) {
            deleteJournalRecords.executeBatch();
            deleteJournalTxRecords.executeBatch();
            SYNC_RUN.set(0);
         }

         connection.commit();

         cleanupTxRecords(deletedRecords, committedTransactions);
         success = true;
      }
      catch (SQLException e) {
         performRollback(connection, recordRef);
      }

      executeCallbacks(recordRef, success);
      return recordRef.size();
   }

   /* We store Transaction reference in memory (once all records associated with a Tranascation are Deleted,
   we remove the Tx Records (i.e. PREPARE, COMMIT). */
   private void cleanupTxRecords(List<Long> deletedRecords, List<Long> committedTx) throws SQLException {

      List<RecordInfo> iterableCopy;
      List<TransactionHolder> iterableCopyTx = new ArrayList<>();
      iterableCopyTx.addAll(transactions.values());

      for (Long txId : committedTx) {
         transactions.get(txId).committed = true;
      }

      // TODO (mtaylor) perhaps we could store a reverse mapping of IDs to prevent this O(n) loop
      for (TransactionHolder h : iterableCopyTx) {

         iterableCopy = new ArrayList<>();
         iterableCopy.addAll(h.recordInfos);

         for (RecordInfo info : iterableCopy) {
            if (deletedRecords.contains(info.id)) {
               h.recordInfos.remove(info);
            }
         }

         if (h.recordInfos.isEmpty() && h.committed) {
            deleteJournalTxRecords.setLong(1, h.transactionID);
            deleteJournalTxRecords.addBatch();
            transactions.remove(h.transactionID);
            System.out.println("Removed: " + h.transactionID);
         }
      }
   }

   private void performRollback(Connection connection, List<JDBCJournalRecord> records) {
      try {
         connection.rollback();
         for (JDBCJournalRecord record : records) {
            if (record.isTransactional() || record.getRecordType() == JDBCJournalRecord.PREPARE_RECORD) {
               removeTxRecord(record);
            }
         }

         List<TransactionHolder> txHolders = new ArrayList<>();
         txHolders.addAll(transactions.values());

         // On rollback we must update the tx map to remove all the tx entries
         for (TransactionHolder txH : txHolders) {
            if (!txH.prepared && txH.recordInfos.isEmpty() && txH.recordsToDelete.isEmpty()) {
               transactions.remove(txH.transactionID);
            }
         }
      }
      catch (Exception sqlE) {
         ActiveMQJournalLogger.LOGGER.error("Error performing rollback", sqlE);
      }
   }

   // TODO Use an executor.
   private void executeCallbacks(final List<JDBCJournalRecord> records, final boolean result) {
      Runnable r = new Runnable() {
         @Override
         public void run() {
            for (JDBCJournalRecord record : records) {
               record.complete(result);
            }
         }
      };
      Thread t = new Thread(r);
      t.start();
   }

   private void removeTxRecord(JDBCJournalRecord record) {
      TransactionHolder txHolder = transactions.get(record.getTxId());

      // We actually only need the record ID in this instance.
      if (record.isTransactional()) {
         RecordInfo info = new RecordInfo(record.getTxId(), record.getRecordType(), new byte[0], record.isUpdate(), record.getCompactCount());
         if (record.getRecordType() == JDBCJournalRecord.DELETE_RECORD_TX) {
            txHolder.recordsToDelete.remove(info);
         }
         else {
            txHolder.recordInfos.remove(info);
         }
      }
      else {
         txHolder.prepared = false;
      }
   }

   public void setTransactions(Map<Long, TransactionHolder> transactions) {
      this.transactions = transactions;
   }
}
