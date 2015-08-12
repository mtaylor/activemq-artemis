package org.apache.activemq.artemis.jdbc.store.records;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.activemq.artemis.core.journal.IOCompletion;
import org.apache.activemq.artemis.utils.DataConstants;

public abstract class JDBCRecord
{
   // Record types taken from Journal Impl
   public static final byte ADD_RECORD = 11;
   public static final byte UPDATE_RECORD = 12;
   public static final byte ADD_RECORD_TX = 13;
   public static final byte UPDATE_RECORD_TX = 14;
   public static final byte DELETE_RECORD_TX = 15;
   public static final byte DELETE_RECORD = 16;
   public static final byte PREPARE_RECORD = 17;
   public static final byte COMMIT_RECORD = 18;
   public static final byte ROLLBACK_RECORD = 19;

   protected IOCompletion ioCompletion;

   protected Long id;

   protected boolean sync;

   protected byte recordType;

   public JDBCRecord(long id, byte recordType, boolean sync, IOCompletion ioCompletion)
   {
      this.id = id;
      this.ioCompletion = ioCompletion;
      this.sync = sync;
   }

   public void complete()
   {
      if (ioCompletion != null)
      {
         ioCompletion.done();
      }
   }

   public void error()
   {
      if (ioCompletion != null)
      {
         ioCompletion.done();
      }
   }

   public void storeLineUp()
   {
      if (ioCompletion != null)
      {
         ioCompletion.storeLineUp();
      }
   }

   public abstract void addToStatement(PreparedStatement statement) throws SQLException;

   /**
    * Returns whether or not this record is a delete or insert.
    */
   public abstract boolean getInsert();
}
