package org.apache.activemq.artemis.jdbc.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

public class ActiveMQJDBCJournal implements Journal
{
   private static String CREATE_TABLE_SQL = "CREATE TABLE JOURNAL " +
                                            "(id INT, recordType SMALLINT, record BLOB, txId BIGINT)";

   private static String INSERT_RECORD_SQL = "INSERT INTO JOURNAL " +
                                             "(id,recordType,record,txId) " +
                                             "VALUES (?,?,?,?)";

   private static String DELETE_RECORD_SQL = "DELETE FROM JOURNAL WHERE id = ?";

   private Connection connection;

   private List<JDBCRecord> records;

   private PreparedStatement insertJournalRecords;

   private PreparedStatement deleteJournalRecords;

   private Object insertRecordsLock = new Object();

   private Object deleteRecordsLock = new Object();

   public ActiveMQJDBCJournal(Connection connection) throws SQLException
   {
      this.connection = connection;
      setup();

      records = new ArrayList<JDBCRecord>();

      insertJournalRecords = connection.prepareStatement(INSERT_RECORD_SQL);
      deleteJournalRecords = connection.prepareStatement(DELETE_RECORD_SQL);
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
      Statement statement = connection.createStatement();
      statement.executeUpdate("DROP TABLE JOURNAL");
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
