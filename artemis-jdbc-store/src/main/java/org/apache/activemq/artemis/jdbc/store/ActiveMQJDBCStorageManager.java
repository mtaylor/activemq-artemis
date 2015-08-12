package org.apache.activemq.artemis.jdbc.store;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.persistence.impl.journal.JournalStorageManager;
import org.apache.activemq.artemis.utils.ExecutorFactory;

public class ActiveMQJDBCStorageManager extends JournalStorageManager
{


   private String jdbcUrl;

   private Properties jdbcConnectionProperties;

   private java.sql.Connection dbConnection;

   private Driver dbDriver;

   public ActiveMQJDBCStorageManager(Configuration config, ExecutorFactory executorFactory) throws SQLException
   {
      //super(config, executorFactory);
      loadConfig(config);

      // TODO Load params from config
      List<Driver> drivers = Collections.list(DriverManager.getDrivers());
      if (drivers.size() == 1)
      {
         dbDriver = drivers.get(0);
         dbConnection = dbDriver.connect(jdbcUrl, jdbcConnectionProperties);
         setupDatabase();
      }
      else
      {
         String error = drivers.isEmpty() ? "No DB driver found on class path" : "Too many DB drivers on class path";
         throw new RuntimeException(error);
      }
   }

   private void loadConfig(Configuration config)
   {
      jdbcUrl = "jdbc:derby:derbyDB;create=true";
      jdbcConnectionProperties = new Properties();
   }

   private void setupDatabase() throws SQLException
   {
      ResultSet rs = dbConnection.getMetaData().getTables(null, null, "JOURNAL", null);
      if (!rs.next())
      {
         PreparedStatement createJournalTable = dbConnection.prepareStatement(CREATE_JOURNAL_TABLE);
         createJournalTable.execute();
      }
   }
}
