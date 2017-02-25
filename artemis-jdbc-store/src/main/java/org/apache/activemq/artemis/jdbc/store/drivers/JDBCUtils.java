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
package org.apache.activemq.artemis.jdbc.store.drivers;

import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;

import org.apache.activemq.artemis.jdbc.store.drivers.derby.DerbySQLProvider;
import org.apache.activemq.artemis.jdbc.store.drivers.mysql.MySQLSQLProvider;
import org.apache.activemq.artemis.jdbc.store.drivers.postgres.PostgresSQLProvider;
import org.apache.activemq.artemis.jdbc.store.sql.GenericSQLProvider;
import org.apache.activemq.artemis.jdbc.store.sql.SQLProvider;
import org.apache.activemq.artemis.utils.UUIDGenerator;
import org.jboss.logging.Logger;

public class JDBCUtils {

   private static final Logger logger = Logger.getLogger(JDBCUtils.class);

   public static SQLProvider.Factory getSQLProviderFactory(String url) {
      SQLProvider.Factory factory;
      if (url.contains("derby")) {
         logger.tracef("getSQLProvider Returning Derby SQL provider for url::%s", url);
         factory = new DerbySQLProvider.Factory();
      } else if (url.contains("postgres")) {
         logger.tracef("getSQLProvider Returning postgres SQL provider for url::%s", url);
         factory = new PostgresSQLProvider.Factory();
      } else if (url.contains("mysql")) {
         logger.tracef("getSQLProvider Returning mysql SQL provider for url::%s", url);
         factory = new MySQLSQLProvider.Factory();
      } else {
         logger.tracef("getSQLProvider Returning generic SQL provider for url::%s", url);
         factory = new GenericSQLProvider.Factory();
      }
      return factory;
   }

   public static SQLProvider getSQLProvider(String driverClass, String tableName) {
      SQLProvider.Factory factory;
      if (driverClass.contains("derby")) {
         logger.tracef("getSQLProvider Returning Derby SQL provider for driver::%s, tableName::%s", driverClass, tableName);
         factory = new DerbySQLProvider.Factory();
      } else if (driverClass.contains("postgres")) {
         logger.tracef("getSQLProvider Returning postgres SQL provider for driver::%s, tableName::%s", driverClass, tableName);
         factory = new PostgresSQLProvider.Factory();
      } else if (driverClass.contains("mysql")) {
         logger.tracef("getSQLProvider Returning mysql SQL provider for driver::%s, tableName::%s", driverClass, tableName);
         factory = new MySQLSQLProvider.Factory();
      } else {
         logger.tracef("getSQLProvider Returning generic SQL provider for driver::%s, tableName::%s", driverClass, tableName);
         factory = new GenericSQLProvider.Factory();
      }
      return factory.create(tableName);
   }

   /**
    * Append to {@code errorMessage} a detailed description of the provided {@link SQLException}.<br/>
    * The information appended are:
    * <ul>
    * <li>SQL STATEMENTS</li>
    * <li>SQL EXCEPTIONS details ({@link SQLException#getSQLState},
    * {@link SQLException#getErrorCode} and {@link SQLException#getMessage}) of the linked list ({@link SQLException#getNextException}) of exceptions</li>
    * </ul>
    *
    * @param errorMessage  the target where append the exceptions details
    * @param exception     the SQL exception (or warning)
    * @param sqlStatements the SQL statements related to the {@code exception}
    * @return {@code errorMessage}
    */
   public static StringBuilder appendSQLExceptionDetails(StringBuilder errorMessage,
                                                         SQLException exception,
                                                         CharSequence sqlStatements) {
      errorMessage.append("\nSQL STATEMENTS: \n").append(sqlStatements);
      return appendSQLExceptionDetails(errorMessage, exception);
   }

   /**
    * Append to {@code errorMessage} a detailed description of the provided {@link SQLException}.<br/>
    * The information appended are:
    * <ul>
    * <li>SQL EXCEPTIONS details ({@link SQLException#getSQLState},
    * {@link SQLException#getErrorCode} and {@link SQLException#getMessage}) of the linked list ({@link SQLException#getNextException}) of exceptions</li>
    * </ul>
    *
    * @param errorMessage the target where append the exceptions details
    * @param exception    the SQL exception (or warning)
    * @return {@code errorMessage}
    */
   public static StringBuilder appendSQLExceptionDetails(StringBuilder errorMessage, SQLException exception) {
      errorMessage.append("\nSQL EXCEPTIONS: ");
      SQLException nextEx = exception;
      int level = 0;
      do {
         errorMessage.append('\n');
         for (int i = 0; i < level; i++) {
            errorMessage.append(' ');
         }
         formatSqlException(errorMessage, nextEx);
         nextEx = exception.getNextException();
         level++;
      } while (nextEx != null);
      return errorMessage;
   }

   private static StringBuilder formatSqlException(StringBuilder errorMessage, SQLException exception) {
      final String sqlState = exception.getSQLState();
      final int errorCode = exception.getErrorCode();
      final String message = exception.getMessage();
      return errorMessage.append("SQLState: ").append(sqlState).append(" ErrorCode: ").append(errorCode).append(" Message: ").append(message);
   }

   /**
    * Generates a globally unique random table name.  UUID is encoded as base 64 Mime with "-" and "_" encoded as
    * "AA" and "BB" respectively.  This keeps the charset to [A-Z][a-z][0-9] and max length 30 for compatibility with
    * various DB vendors.
    *
    * @return GUID for DB table name
    */
   public static String getRandomDatabaseTableName() {
      byte[] uuid = UUIDGenerator.getInstance().generateUUID().asBytes();
      String base64uuid = Base64.getMimeEncoder().encodeToString(uuid);
      base64uuid = base64uuid.replace("/", "AA");
      base64uuid = base64uuid.replace("=", "BB");
      base64uuid = base64uuid.replace("+", "CC");
      /* Since we're using AA,BB,CC as the replacement for unsupported chars, there's a small chance that the uuid could
      exceed 30 chars, if this happens we rebuild.  It's an expensive operation but only happens once on a single
      address when initial page happens */
      // FIXME This could be avoided by understanding the supported char set and table length of each database.
      if (base64uuid.length() > 30) {
         base64uuid = getRandomDatabaseTableName();
      }
      return base64uuid;
   }
}
