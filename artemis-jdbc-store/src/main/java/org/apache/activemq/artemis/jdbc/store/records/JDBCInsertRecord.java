package org.apache.activemq.artemis.jdbc.store.records;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
import org.apache.activemq.artemis.core.journal.EncodingSupport;
import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.core.journal.impl.dataformat.JournalCompleteRecordTX;
import org.apache.activemq.artemis.utils.ActiveMQBufferInputStream;

public class JDBCInsertRecord extends JDBCRecord
{
   private InputStream record;

   private Long txId;

   private JournalCompleteRecordTX.TX_RECORD_TYPE txRecordType;

   private InputStream txData;

   protected static String INSERT_RECORD_SQL = "INSERT INTO JOURNAL " +
      "(id,recordType,record,txId,txRecordType,transactionData) " +
      "VALUES (?,?,?,?,?,?,?,?)";

   private JDBCInsertRecord(long id, byte recordType, boolean sync, IOCompletion ioCompletion)
   {
      super (id, recordType, sync, ioCompletion);
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

   public JDBCInsertRecord(long txId, JournalCompleteRecordTX.TX_RECORD_TYPE txRecordType, boolean sync, IOCompletion ioCompletion)
   {
      super(-1, sync, ioCompletion);
      this.txId = txId;
      this.txRecordType = txRecordType;
   }

   public JDBCInsertRecord(long txId, JournalCompleteRecordTX.TX_RECORD_TYPE txRecordType, EncodingSupport transactionData, boolean sync, IOCompletion ioCompletion)
   {
      this(txId, txRecordType, sync, ioCompletion);

      ActiveMQBuffer buffer = ActiveMQBuffers.fixedBuffer(transactionData.getEncodeSize());
      transactionData.encode(buffer);
      this.txData = new ActiveMQBufferInputStream(buffer);
   }

   public JDBCInsertRecord(long txId, JournalCompleteRecordTX.TX_RECORD_TYPE txRecordType, byte[] transactionData, boolean sync, IOCompletion ioCompletion)
   {
      this(txId, txRecordType, sync, ioCompletion);
      this.txData = new ByteArrayInputStream(transactionData);
   }

   @Override
   public void addToStatement(PreparedStatement statement) throws SQLException
   {
      statement.setLong(1, id);
      statement.setByte(2, recordType);
      statement.setBinaryStream(3, record);

      statement.setLong(4, txId);
      statement.setInt(5, txRecordType == null ? -1 : txRecordType.ordinal());
      statement.setBinaryStream(6, txData == null ? new ByteArrayInputStream(new byte[0]) : txData);
      statement.addBatch();
   }

   @Override
   public boolean getInsert()
   {
      return true;
   }
}