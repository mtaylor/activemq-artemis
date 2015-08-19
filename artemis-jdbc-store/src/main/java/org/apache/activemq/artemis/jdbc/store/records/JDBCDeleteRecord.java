package org.apache.activemq.artemis.jdbc.store.records;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.jdbc.store.records.JDBCRecord;

public class JDBCDeleteRecord extends JDBCRecord
{
   private long txId;

   protected static String SQL = "DELETE FROM JOURNAL WHERE id=? OR txId=?";

   public JDBCDeleteRecord(long id, boolean sync, IOCompletion ioCompletion)
   {
      super(id, JDBCRecord.DELETE_RECORD, sync, ioCompletion);
      txId = -2;
   }

   public JDBCDeleteRecord(long id, long txId, boolean sync, IOCompletion completion)
   {
      super(id, JDBCRecord.DELETE_RECORD, sync, completion);
      this.txId = txId;
   }
   @Override
   public void addToStatement(PreparedStatement statement) throws SQLException
   {
      statement.setLong(1, id);
      statement.setLong(2, txId);
      statement.addBatch();
   }

   @Override
   public boolean getInsert()
   {
      return false;
   }
}
