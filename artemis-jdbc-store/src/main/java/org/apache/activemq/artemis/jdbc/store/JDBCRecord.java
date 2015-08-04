package org.apache.activemq.artemis.jdbc.store;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.activemq.artemis.core.journal.EncodingSupport;
import org.apache.activemq.artemis.core.journal.IOCompletion;

public class JDBCRecord
{
   private IOCompletion ioCompletion;

   private long id;

   private byte recordType;

   private byte[] record;

   private boolean add;

   private long txId;

   private int txRecordType;

   private InputStream transactionData;

   private int noRecords;

   private JDBCRecord(long id, byte recordType, byte[] record, boolean add, long txId,
                     int txRecordType, InputStream transactionData, int noRecords, IOCompletion ioCompletion)
   {
      this.id = id;
      this.recordType = recordType;
      this.record = record;
      this.add = add;
      this.txId = txId;
      this.txRecordType = txRecordType;
      this.transactionData = transactionData;
      this.noRecords = noRecords;
      this.ioCompletion = ioCompletion;
   }

   protected static JDBCRecord createAppendAddRecord(long id, byte recordType, byte[] record, IOCompletion ioCompletion)
   {
      return new JDBCRecord(id, recordType, record, false, -1, -1, null, 1, ioCompletion);
   }

   void complete()
   {
      if (ioCompletion != null)
      {
         ioCompletion.done();
      }
   }

   void error()
   {
      if (ioCompletion != null)
      {
         ioCompletion.done();
      }
   }

   void insertRecord(PreparedStatement statement) throws SQLException
   {
      statement.setLong(1, id);
      statement.setByte(2, recordType);
      statement.setBoolean(3, add);
      statement.setLong(4, txId);
      statement.setInt(5, txRecordType);
      statement.setBinaryStream(6, transactionData);
      statement.setInt(7, noRecords);
      statement.addBatch();
   }
}
