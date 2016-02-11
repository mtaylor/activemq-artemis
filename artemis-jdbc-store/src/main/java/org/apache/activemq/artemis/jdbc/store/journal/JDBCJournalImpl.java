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
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import org.apache.activemq.artemis.jdbc.store.JDBCUtils;
import org.apache.activemq.artemis.journal.ActiveMQJournalLogger;

public class JDBCJournalImpl implements Journal {


   public static final int NO_WRITERS = 5;
   
   private static int USER_VERSION = 1;

   private final String tableName;

   private final String jdbcDriverClass;

   private Connection connection;

   private boolean started;

   private String jdbcUrl;

   private Driver dbDriver;

   private final ReadWriteLock journalLock = new ReentrantReadWriteLock();

   // Track Tx Records
   private Map<Long, TransactionHolder> transactions = new ConcurrentHashMap<>();

   // Sequence ID for journal records
   private AtomicLong seq = new AtomicLong(0);

   private PreparedStatement selectJournalRecords;

   private PreparedStatement countJournalRecords;

   private List<JDBCWriter> writers;

   private AtomicInteger nextWriter = new AtomicInteger(0);

   public JDBCJournalImpl(String jdbcUrl, String tableName, String jdbcDriverClass) {
      this.tableName = tableName;
      this.jdbcUrl = jdbcUrl;
      this.jdbcDriverClass = jdbcDriverClass;

      writers = new ArrayList<>();
   }

   @Override
   public void start() throws Exception {
      dbDriver = JDBCUtils.getDriver(jdbcDriverClass);

      try {
         connection = dbDriver.connect(jdbcUrl, new Properties());
         JDBCUtils.createTableIfNotExists(connection, tableName, JDBCJournalRecord.createTableSQL(tableName));

         JDBCWriter writer;
         for (int i = 0; i < NO_WRITERS; i++) {
            writer = new JDBCWriter(dbDriver.connect(jdbcUrl, new Properties()), transactions, tableName);
            writers.add(writer);
            writer.start();
         }

         selectJournalRecords = connection.prepareStatement(JDBCJournalRecord.selectRecordsSQL(tableName));
         countJournalRecords = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
      }
      catch (SQLException e) {
         ActiveMQJournalLogger.LOGGER.error("Unable to connect to database using URL: " + jdbcUrl);
         throw new RuntimeException("Error connecting to database", e);
      }

      started = true;
   }

   @Override
   public void stop() throws Exception {
      stop(true);
   }

   public synchronized void stop(boolean shutdownConnection) throws Exception {
      if (started) {
         journalLock.writeLock().lock();

         for (JDBCWriter writer : writers) {
            writer.stop();
         }

         if (shutdownConnection) {
            connection.close();
         }

         started = false;
         journalLock.writeLock().unlock();
      }
   }

   public synchronized void destroy() throws Exception {
      connection.setAutoCommit(false);
      Statement statement = connection.createStatement();
      statement.executeUpdate("DROP TABLE " + tableName);
      statement.close();
      connection.commit();
      stop();
   }

   private synchronized void appendRecord(JDBCJournalRecord record) throws Exception {
      getWriter(record).appendRecord(record);
   }

   // Simple round robin
   private synchronized JDBCWriter getWriter(JDBCJournalRecord record) {
      for (JDBCWriter writer : writers) {
         if (writer.isOwner(record)) return writer;
      }

      nextWriter.addAndGet(1);
      if (nextWriter.intValue() > (NO_WRITERS - 1)) {
         nextWriter.set(0);
      }
      return writers.get(nextWriter.getAndAdd(1));
   }

   @Override
   public void appendAddRecord(long id, byte recordType, byte[] record, boolean sync) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.ADD_RECORD, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setSync(sync);
      appendRecord(r);
   }

   @Override
   public void appendAddRecord(long id, byte recordType, EncodingSupport record, boolean sync) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.ADD_RECORD, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setSync(sync);
      appendRecord(r);
   }

   @Override
   public void appendAddRecord(long id,
                               byte recordType,
                               EncodingSupport record,
                               boolean sync,
                               IOCompletion completionCallback) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.ADD_RECORD, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setSync(sync);
      r.setIoCompletion(completionCallback);
      appendRecord(r);
   }

   @Override
   public void appendUpdateRecord(long id, byte recordType, byte[] record, boolean sync) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.UPDATE_RECORD, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setSync(sync);
      appendRecord(r);
   }

   @Override
   public void appendUpdateRecord(long id, byte recordType, EncodingSupport record, boolean sync) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.UPDATE_RECORD, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setSync(sync);
      appendRecord(r);
   }

   @Override
   public void appendUpdateRecord(long id,
                                  byte recordType,
                                  EncodingSupport record,
                                  boolean sync,
                                  IOCompletion completionCallback) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.ADD_RECORD, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setSync(sync);
      r.setIoCompletion(completionCallback);
      appendRecord(r);
   }

   @Override
   public void appendDeleteRecord(long id, boolean sync) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.DELETE_RECORD, seq.incrementAndGet());
      r.setSync(sync);
      appendRecord(r);
   }

   @Override
   public void appendDeleteRecord(long id, boolean sync, IOCompletion completionCallback) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.DELETE_RECORD, seq.incrementAndGet());
      r.setSync(sync);
      r.setIoCompletion(completionCallback);
      appendRecord(r);
   }

   @Override
   public void appendAddRecordTransactional(long txID, long id, byte recordType, byte[] record) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.ADD_RECORD_TX, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setTxId(txID);
      appendRecord(r);
   }

   @Override
   public void appendAddRecordTransactional(long txID,
                                            long id,
                                            byte recordType,
                                            EncodingSupport record) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.ADD_RECORD_TX, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setTxId(txID);
      appendRecord(r);
   }

   @Override
   public void appendUpdateRecordTransactional(long txID, long id, byte recordType, byte[] record) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.UPDATE_RECORD_TX, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setTxId(txID);
      appendRecord(r);
   }

   @Override
   public void appendUpdateRecordTransactional(long txID,
                                               long id,
                                               byte recordType,
                                               EncodingSupport record) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.UPDATE_RECORD_TX, seq.incrementAndGet());
      r.setUserRecordType(recordType);
      r.setRecord(record);
      r.setTxId(txID);
      appendRecord(r);
   }

   @Override
   public void appendDeleteRecordTransactional(long txID, long id, byte[] record) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.DELETE_RECORD_TX, seq.incrementAndGet());
      r.setRecord(record);
      r.setTxId(txID);
      appendRecord(r);
   }

   @Override
   public void appendDeleteRecordTransactional(long txID, long id, EncodingSupport record) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.DELETE_RECORD_TX, seq.incrementAndGet());
      r.setRecord(record);
      r.setTxId(txID);
      appendRecord(r);
   }

   @Override
   public void appendDeleteRecordTransactional(long txID, long id) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(id, JDBCJournalRecord.DELETE_RECORD_TX, seq.incrementAndGet());
      r.setTxId(txID);
      appendRecord(r);
   }

   @Override
   public void appendCommitRecord(long txID, boolean sync) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(-1, JDBCJournalRecord.COMMIT_RECORD, seq.incrementAndGet());
      r.setTxId(txID);
      appendRecord(r);
   }

   @Override
   public void appendCommitRecord(long txID, boolean sync, IOCompletion callback) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(-1, JDBCJournalRecord.COMMIT_RECORD, seq.incrementAndGet());
      r.setTxId(txID);
      r.setIoCompletion(callback);
      appendRecord(r);
   }

   @Override
   public void appendCommitRecord(long txID,
                                  boolean sync,
                                  IOCompletion callback,
                                  boolean lineUpContext) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(-1, JDBCJournalRecord.COMMIT_RECORD, seq.incrementAndGet());
      r.setTxId(txID);
      r.setStoreLineUp(lineUpContext);
      r.setIoCompletion(callback);
      appendRecord(r);
   }

   @Override
   public void appendPrepareRecord(long txID, EncodingSupport transactionData, boolean sync) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(-1, JDBCJournalRecord.PREPARE_RECORD, seq.incrementAndGet());
      r.setTxId(txID);
      r.setTxData(transactionData);
      r.setSync(sync);
      appendRecord(r);
   }

   @Override
   public void appendPrepareRecord(long txID,
                                   EncodingSupport transactionData,
                                   boolean sync,
                                   IOCompletion callback) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(0, JDBCJournalRecord.PREPARE_RECORD, seq.incrementAndGet());
      r.setTxId(txID);
      r.setTxData(transactionData);
      r.setTxData(transactionData);
      r.setSync(sync);
      r.setIoCompletion(callback);
      appendRecord(r);
   }

   @Override
   public void appendPrepareRecord(long txID, byte[] transactionData, boolean sync) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(0, JDBCJournalRecord.PREPARE_RECORD, seq.incrementAndGet());
      r.setTxId(txID);
      r.setTxData(transactionData);
      r.setSync(sync);
      appendRecord(r);
   }

   @Override
   public void appendRollbackRecord(long txID, boolean sync) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(0, JDBCJournalRecord.ROLLBACK_RECORD, seq.incrementAndGet());
      r.setTxId(txID);
      r.setSync(sync);
      appendRecord(r);
   }

   @Override
   public void appendRollbackRecord(long txID, boolean sync, IOCompletion callback) throws Exception {
      JDBCJournalRecord r = new JDBCJournalRecord(0, JDBCJournalRecord.ROLLBACK_RECORD, seq.incrementAndGet());
      r.setTxId(txID);
      r.setSync(sync);
      r.setIoCompletion(callback);
      appendRecord(r);
   }

   @Override
   public synchronized JournalLoadInformation load(LoaderCallback reloadManager) throws Exception {
      JournalLoadInformation jli = new JournalLoadInformation();
      JDBCJournalReaderCallback jrc = new JDBCJournalReaderCallback(reloadManager);
      JDBCJournalRecord r;

      try (ResultSet rs = selectJournalRecords.executeQuery()) {
         int noRecords = 0;
         while (rs.next()) {
            r = JDBCJournalRecord.readRecord(rs);
            switch (r.getRecordType()) {
               case JDBCJournalRecord.ADD_RECORD:
                  jrc.onReadAddRecord(r.toRecordInfo());
                  break;
               case JDBCJournalRecord.UPDATE_RECORD:
                  jrc.onReadUpdateRecord(r.toRecordInfo());
                  break;
               case JDBCJournalRecord.DELETE_RECORD:
                  jrc.onReadDeleteRecord(r.getId());
                  break;
               case JDBCJournalRecord.ADD_RECORD_TX:
                  jrc.onReadAddRecordTX(r.getTxId(), r.toRecordInfo());
                  break;
               case JDBCJournalRecord.UPDATE_RECORD_TX:
                  jrc.onReadUpdateRecordTX(r.getTxId(), r.toRecordInfo());
                  break;
               case JDBCJournalRecord.DELETE_RECORD_TX:
                  jrc.onReadDeleteRecordTX(r.getTxId(), r.toRecordInfo());
                  break;
               case JDBCJournalRecord.PREPARE_RECORD:
                  jrc.onReadPrepareRecord(r.getTxId(), r.getTxDataAsByteArray(), r.getTxCheckNoRecords());
                  break;
               case JDBCJournalRecord.COMMIT_RECORD:
                  jrc.onReadCommitRecord(r.getTxId(), r.getTxCheckNoRecords());
                  break;
               case JDBCJournalRecord.ROLLBACK_RECORD:
                  jrc.onReadRollbackRecord(r.getTxId());
                  break;
               default:
                  throw new Exception("Error Reading Journal, Unknown Record Type: " + r.getRecordType());
            }
            noRecords++;
            if (r.getSeq() > seq.longValue()) {
               seq.set(r.getSeq());
            }
         }
         jrc.checkPreparedTx();

         jli.setMaxID(((JDBCJournalLoaderCallback) reloadManager).getMaxId());
         jli.setNumberOfRecords(noRecords);
         transactions = jrc.getTransactions();
         System.out.println("Loaded");

         for (JDBCWriter writer : writers) {
            writer.setTransactions(transactions);
         }
      }
      return jli;
   }

   @Override
   public JournalLoadInformation loadInternalOnly() throws Exception {
      return null;
   }

   @Override
   public JournalLoadInformation loadSyncOnly(JournalState state) throws Exception {
      return null;
   }

   @Override
   public void lineUpContext(IOCompletion callback) {
      callback.storeLineUp();
   }

   @Override
   public JournalLoadInformation load(List<RecordInfo> committedRecords,
                                      List<PreparedTransactionInfo> preparedTransactions,
                                      TransactionFailureCallback transactionFailure) throws Exception {
      return load(committedRecords, preparedTransactions, transactionFailure, true);
   }

   public synchronized JournalLoadInformation load(final List<RecordInfo> committedRecords,
                                                   final List<PreparedTransactionInfo> preparedTransactions,
                                                   final TransactionFailureCallback failureCallback,
                                                   final boolean fixBadTX) throws Exception {
      JDBCJournalLoaderCallback lc = new JDBCJournalLoaderCallback(committedRecords, preparedTransactions, failureCallback, fixBadTX);
      return load(lc);
   }

   @Override
   public int getAlignment() throws Exception {
      return 0;
   }

   @Override
   public int getNumberOfRecords() {
      int count = 0;
      try (ResultSet rs = countJournalRecords.executeQuery()) {
         rs.next();
         count = rs.getInt(1);
      }
      catch (SQLException e) {
         return -1;
      }
      return count;
   }

   @Override
   public int getUserVersion() {
      return USER_VERSION;
   }

   @Override
   public void perfBlast(int pages) {
   }

   @Override
   public void runDirectJournalBlast() throws Exception {
   }

   @Override
   public Map<Long, JournalFile> createFilesForBackupSync(long[] fileIds) throws Exception {
      return null;
   }

   public final void synchronizationLock() {
      journalLock.writeLock().lock();
   }

   public final void synchronizationUnlock() {
      journalLock.writeLock().unlock();
   }

   @Override
   public void forceMoveNextFile() throws Exception {
   }

   @Override
   public JournalFile[] getDataFiles() {
      return new JournalFile[0];
   }

   @Override
   public SequentialFileFactory getFileFactory() {
      return null;
   }

   @Override
   public int getFileSize() {
      return 0;
   }

   @Override
   public void scheduleCompactAndBlock(int timeout) throws Exception {

   }

   @Override
   public void replicationSyncPreserveOldFiles() {

   }

   @Override
   public void replicationSyncFinished() {

   }

   @Override
   public boolean isStarted() {
      return started;
   }

}
