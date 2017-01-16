/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.jdbc.store.file;

import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.Unpooled;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.core.buffers.impl.ChannelBufferWrapper;
import org.apache.activemq.artemis.core.io.DummyCallback;
import org.apache.activemq.artemis.jdbc.store.sql.Oracle12CSQLProvider;
import org.apache.activemq.artemis.utils.ActiveMQThreadFactory;
import org.apache.activemq.artemis.utils.ActiveMQThreadPoolExecutor;

public class Oracle12CSequentialSequentialFileDriver extends JDBCSequentialFileFactoryDriver<Oracle12CSQLProvider> {

   private PreparedStatement writeToLargeMessage;

   Oracle12CSequentialSequentialFileDriver() {
      super();
   }

   Oracle12CSequentialSequentialFileDriver(DataSource dataSource, Oracle12CSQLProvider provider) {
      super(dataSource, provider);
   }

   public Oracle12CSequentialSequentialFileDriver(Oracle12CSQLProvider sqlProvider,
                                                  String jdbcConnectionUrl,
                                                  String jdbcDriverClass) {
      super(sqlProvider, jdbcConnectionUrl, jdbcDriverClass);
   }

   @Override
   protected void prepareStatements() throws SQLException {
      super.prepareStatements();
      writeToLargeMessage = connection.prepareStatement(sqlProvider.getWritetoLargeMessage());
   }

   @Override
   public synchronized void createFile(JDBCSequentialFile file) throws SQLException {
      try {
         connection.setAutoCommit(false);

         createFile.setString(1, file.getFileName());
         createFile.setString(2, file.getExtension());
         createFile.executeUpdate();

         try (ResultSet keys = createFile.getGeneratedKeys()) {
            keys.next();
            file.setId(keys.getInt(1));
         }
         connection.commit();
      } catch (SQLException e) {
         connection.rollback();
         throw e;
      }
   }

   @Override
   public synchronized int writeToFile(JDBCSequentialFile file, byte[] data) throws SQLException {

      connection.setAutoCommit(false);
      writeToLargeMessage.setInt(1, file.getId());
      int written = 0;
      try (ResultSet rs = writeToLargeMessage.executeQuery()) {
         if (rs.next()) {
            Blob blob = rs.getBlob("DATA");
            written = blob.setBytes(blob.length() + 1, data);
         }
         connection.commit();
      } catch (SQLException e) {
         connection.rollback();
         throw e;
      }
      return written;
   }

   public static void main(String[] args) {
      try {
         //step1 load the driver class
         Class.forName("oracle.jdbc.driver.OracleDriver");
         Oracle12CSQLProvider sqlProvider = new Oracle12CSQLProvider("andys_file");
         Oracle12CSequentialSequentialFileDriver sequentialFileDriver = new Oracle12CSequentialSequentialFileDriver(sqlProvider, "jdbc:oracle:thin:dballo16/dballo16@db-12.rhev-ci-vms.eng.rdu2.redhat.com:1521:orcl", "oracle.jdbc.driver.OracleDriver");
         ActiveMQThreadFactory factory = new ActiveMQThreadFactory("test", false, Oracle12CSequentialSequentialFileDriver.class.getClassLoader());
         ActiveMQThreadPoolExecutor executor = new ActiveMQThreadPoolExecutor(0, 10, 60000, TimeUnit.MILLISECONDS, factory);
         Object lock = new Object();
         JDBCSequentialFileFactory fileFactory = new JDBCSequentialFileFactory("jdbc:oracle:thin:dballo16/dballo16@db-12.rhev-ci-vms.eng.rdu2.redhat.com:1521:orcl", "oracle.jdbc.driver.OracleDriver", sqlProvider, executor);
         JDBCSequentialFile file = new JDBCSequentialFile(fileFactory, "foo.bar", executor, sequentialFileDriver, lock);
         sequentialFileDriver.start();

         file.open();
         ActiveMQBuffer buffer = new ChannelBufferWrapper(Unpooled.buffer());
         buffer.writeString("helloooo ");
         file.internalWrite(buffer, new DummyCallback());

         buffer = new ChannelBufferWrapper(Unpooled.buffer());
         buffer.writeString("from Andy");
         file.internalWrite(buffer, new DummyCallback());

         ByteBuffer allocate = ByteBuffer.allocate((int) file.size());
         int read = file.read(allocate);

         buffer = new ChannelBufferWrapper(Unpooled.copiedBuffer(allocate.array()));
         System.out.println(buffer.readString());
         System.out.println(buffer.readString());

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
