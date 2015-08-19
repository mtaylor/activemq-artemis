package org.apache.activemq.artemis.jdbc.store.records;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ActiveMQBuffers;
import org.apache.activemq.artemis.core.journal.EncodingSupport;
import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.core.journal.impl.JournalImpl;
import org.apache.activemq.artemis.core.journal.impl.dataformat.JournalCompleteRecordTX;
import org.apache.activemq.artemis.utils.ActiveMQBufferInputStream;

public class JDBCInsertRecord extends JDBCRecord
{
   private InputStream record;

   private long txId = -1;

   public static String SQL = "INSERT INTO JOURNAL " +
      "(id,recordType,record,txId) " +
      "VALUES (?,?,?,?)";

   public JDBCInsertRecord(long id, byte recordType, boolean sync, IOCompletion ioCompletion)
   {
      super (id, recordType, sync, ioCompletion);
   }

   public JDBCInsertRecord(long id, byte recordType, boolean sync, IOCompletion ioCompletion, boolean storeLineUp)
   {
      super (id, recordType, sync, ioCompletion, storeLineUp);
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

   @Override
   public void addToStatement(PreparedStatement statement) throws SQLException
   {
      statement.setLong(1, id);
      statement.setByte(2, recordType);
      statement.setBinaryStream(3, record);
      statement.setLong(4, txId);
      statement.addBatch();
   }

   @Override
   public boolean getInsert()
   {
      return true;
   }
}