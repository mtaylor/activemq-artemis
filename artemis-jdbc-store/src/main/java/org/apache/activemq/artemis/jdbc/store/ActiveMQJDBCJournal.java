package org.apache.activemq.artemis.jdbc.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.activemq.artemis.core.journal.EncodingSupport;
import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.core.journal.Journal;
import org.apache.activemq.artemis.core.journal.JournalLoadInformation;
import org.apache.activemq.artemis.core.journal.LoaderCallback;
import org.apache.activemq.artemis.core.journal.PreparedTransactionInfo;
import org.apache.activemq.artemis.core.journal.RecordInfo;
import org.apache.activemq.artemis.core.journal.SequentialFileFactory;
import org.apache.activemq.artemis.core.journal.TransactionFailureCallback;
import org.apache.activemq.artemis.core.journal.impl.JournalFile;

public class ActiveMQJDBCJournal implements Journal
{
   private static String INSERT_RECORD_SQL = "INSERT INTO JOURNAL " +
                                             "(id,recordType,record,add,txId,txRecordType,transactionData,noRecords) " +
                                             "VALUES (?,?,?,?,?,?,?,?)";
   private Connection connection;

   private List<JDBCRecord> records;

   private PreparedStatement insertJournalRecords;

   private Object recordsLock = new Object();

   public ActiveMQJDBCJournal(Connection connection) throws SQLException
   {
      this.connection = connection;
      connection.setAutoCommit(false);

      records = new ArrayList<JDBCRecord>();
      insertJournalRecords = connection.prepareStatement(INSERT_RECORD_SQL);
   }

   private synchronized void sync() throws SQLException
   {
      List<JDBCRecord> localRecords;

      synchronized (recordsLock)
      {
         localRecords = records;
         records = new ArrayList<JDBCRecord>();
      }

      for (JDBCRecord record : localRecords)
      {
         record.insertRecord(insertJournalRecords);
      }

      boolean result = insertJournalRecords.execute();
      executeCallbacks(localRecords, result);
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

   private void appendRecord(JDBCRecord record) throws SQLException
   {
      synchronized (recordsLock)
      {
         records.add(record);
      }
   }

   @Override
   public void appendAddRecord(long id, byte recordType, byte[] record, boolean sync) throws Exception
   {
      JDBCRecord jdbcRecord = JDBCRecord.createAppendAddRecord(id, recordType, record, null);
      appendRecord(jdbcRecord);
   }

   @Override
   public void appendAddRecord(long id, byte recordType, EncodingSupport record, boolean sync) throws Exception
   {
      appendAddRecord(id, recordType, record, sync, null);
   }

   @Override
   public void appendAddRecord(long id, byte recordType, EncodingSupport record, boolean sync, IOCompletion completionCallback) throws Exception
   {
      JDBCRecord jdbcRecord = JDBCRecord.createAppendAddRecord(id, recordType, new byte[0], completionCallback);
      appendRecord(jdbcRecord);
   }

   @Override
   public void appendUpdateRecord(long id, byte recordType, byte[] record, boolean sync) throws Exception
   {

   }

   @Override
   public void appendUpdateRecord(long id, byte recordType, EncodingSupport record, boolean sync) throws Exception
   {

   }

   @Override
   public void appendUpdateRecord(long id, byte recordType, EncodingSupport record, boolean sync, IOCompletion completionCallback) throws Exception
   {

   }

   @Override
   public void appendDeleteRecord(long id, boolean sync) throws Exception
   {

   }

   @Override
   public void appendDeleteRecord(long id, boolean sync, IOCompletion completionCallback) throws Exception
   {

   }

   @Override
   public void appendAddRecordTransactional(long txID, long id, byte recordType, byte[] record) throws Exception
   {

   }

   @Override
   public void appendAddRecordTransactional(long txID, long id, byte recordType, EncodingSupport record) throws Exception
   {

   }

   @Override
   public void appendUpdateRecordTransactional(long txID, long id, byte recordType, byte[] record) throws Exception
   {

   }

   @Override
   public void appendUpdateRecordTransactional(long txID, long id, byte recordType, EncodingSupport record) throws Exception
   {

   }

   @Override
   public void appendDeleteRecordTransactional(long txID, long id, byte[] record) throws Exception
   {

   }

   @Override
   public void appendDeleteRecordTransactional(long txID, long id, EncodingSupport record) throws Exception
   {

   }

   @Override
   public void appendDeleteRecordTransactional(long txID, long id) throws Exception
   {

   }

   @Override
   public void appendCommitRecord(long txID, boolean sync) throws Exception
   {

   }

   @Override
   public void appendCommitRecord(long txID, boolean sync, IOCompletion callback) throws Exception
   {

   }

   @Override
   public void appendCommitRecord(long txID, boolean sync, IOCompletion callback, boolean lineUpContext) throws Exception
   {

   }

   @Override
   public void appendPrepareRecord(long txID, EncodingSupport transactionData, boolean sync) throws Exception
   {

   }

   @Override
   public void appendPrepareRecord(long txID, EncodingSupport transactionData, boolean sync, IOCompletion callback) throws Exception
   {

   }

   @Override
   public void appendPrepareRecord(long txID, byte[] transactionData, boolean sync) throws Exception
   {

   }

   @Override
   public void appendRollbackRecord(long txID, boolean sync) throws Exception
   {

   }

   @Override
   public void appendRollbackRecord(long txID, boolean sync, IOCompletion callback) throws Exception
   {

   }

   @Override
   public JournalLoadInformation load(LoaderCallback reloadManager) throws Exception
   {
      return null;
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
   public int getNumberOfRecords()
   {
      return 0;
   }

   @Override
   public int getUserVersion()
   {
      return 0;
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
