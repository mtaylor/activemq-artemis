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
package org.apache.activemq.artemis.jdbc.store.journal;

import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class JDBCSyncTask extends TimerTask
{
   private final JDBCJournalImpl journal;

   private long delay;

   private Timer timer;

   private AtomicInteger noRecords = new AtomicInteger(0);

   private int maxRecordCount;

   private AtomicBoolean syncInProgress = new AtomicBoolean(false);

   public JDBCSyncTask(JDBCJournalImpl journal, long delay, int maxRecordCount)
   {
      this.delay = delay;
      this.journal = journal;
   }

   @Override
   public void run()
   {

   }
}
