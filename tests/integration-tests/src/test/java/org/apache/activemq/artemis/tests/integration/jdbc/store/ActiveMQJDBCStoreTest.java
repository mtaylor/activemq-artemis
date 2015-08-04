package org.apache.activemq.artemis.tests.integration.jdbc.store;

import org.apache.activemq.artemis.jdbc.store.ActiveMQJDBCStorageManager;
import org.junit.Test;

public class ActiveMQJDBCStoreTest
{
   @Test
   public void testJDBCStoreSetup() throws Exception
   {
      ActiveMQJDBCStorageManager activeMQJDBCStorageManager = new ActiveMQJDBCStorageManager(null, null);
   }
}
