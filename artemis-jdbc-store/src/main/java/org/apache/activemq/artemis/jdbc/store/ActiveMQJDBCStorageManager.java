package org.apache.activemq.artemis.jdbc.store;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.utils.ExecutorFactory;

public class ActiveMQJDBCStorageManager //extends JournalStorageManager
{
   private String jdbcUrl;

   private Properties jdbcConnectionProperties;

   private java.sql.Connection dbConnection;

   private Driver dbDriver;

   private ActiveMQJDBCJournal journal;

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
      }
      else
      {
         String error = drivers.isEmpty() ? "No DB driver found on class path" : "Too many DB drivers on class path";
         throw new RuntimeException(error);
      }
      journal = new ActiveMQJDBCJournal(dbConnection);
   }

   private void loadConfig(Configuration config)
   {
      jdbcUrl = "jdbc:derby:derbyDB;create=true";
      jdbcConnectionProperties = new Properties();
   }

   public ActiveMQJDBCJournal getJournal()
   {
      return journal;
   }
}
