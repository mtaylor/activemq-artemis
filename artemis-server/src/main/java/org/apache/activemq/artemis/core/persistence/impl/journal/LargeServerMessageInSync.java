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
package org.apache.activemq.artemis.core.persistence.impl.journal;

import java.nio.ByteBuffer;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.io.SequentialFile;
import org.apache.activemq.artemis.core.persistence.StorageManager;
import org.apache.activemq.artemis.core.persistence.StorageManager.LargeMessageExtension;
import org.apache.activemq.artemis.core.replication.ReplicatedLargeMessage;
import org.apache.activemq.artemis.core.server.ActiveMQServerLogger;
import org.apache.activemq.artemis.core.server.LargeServerMessage;
import org.jboss.logging.Logger;

public final class LargeServerMessageInSync implements ReplicatedLargeMessage {

   private final Logger logger = Logger.getLogger(LargeServerMessageInSync.class);
   private final boolean isTrace = logger.isTraceEnabled();

   private final LargeServerMessage mainLM;
   private final StorageManager storageManager;
   private SequentialFile appendFile;
   private boolean syncDone;
   private boolean deleted;

   /**
    * @param storageManager
    */
   public LargeServerMessageInSync(StorageManager storageManager) {
      mainLM = storageManager.createLargeMessage();
      this.storageManager = storageManager;
   }

   public synchronized void joinSyncedData(ByteBuffer buffer) throws Exception {
      if (deleted) {
         if (isTrace) {
            logger.trace("joinSyncedData was ignored on " + mainLM + " as a previous delete was called");
         }
         return;
      }
      SequentialFile mainSeqFile = mainLM.getFile();
      if (!mainSeqFile.isOpen()) {
         mainSeqFile.open();
      }

      if (isTrace) {
         logger.trace("joinSyncedData on " + mainLM + ", currentSize on mainMessage=" + mainSeqFile.size() + " ");
      }


      try {
         mainSeqFile.position(mainSeqFile.size());
         if (appendFile != null) {
            if (!appendFile.isOpen()) {
               appendFile.open();
            }
            appendFile.position(0);

            for (;;) {
               buffer.rewind();
               int bytesRead = appendFile.read(buffer);

               if (isTrace) {
                  logger.trace("appending " + bytesRead + " bytes on mainSeqFile");
               }

               if (bytesRead > 0) {
                  mainSeqFile.writeDirect(buffer, false);
               }

               if (bytesRead < buffer.capacity()) {
                  logger.trace("Interrupting reading as the whole thing was sent on " + mainLM);
                  break;
               }
            }
            deleteAppendFile();
         }
         else {
            if (isTrace) {
               logger.trace("joinSyncedData, appendFile is null, ignoring joinSyncedData on " + mainLM);
            }
         }
      }
      catch (Throwable e) {
         logger.warn("Error while sincing data on largeMessageInSync::" + mainLM);
      }


      if (isTrace) {
         logger.trace("joinedSyncData on " + mainLM + " finished with " + mainSeqFile.size());
      }



      syncDone = true;
   }

   public SequentialFile getSyncFile() throws ActiveMQException {
      return mainLM.getFile();
   }

   @Override
   public Message setDurable(boolean durable) {
      mainLM.setDurable(durable);
      return mainLM;
   }

   @Override
   public synchronized Message setMessageID(long id) {
      mainLM.setMessageID(id);
      return mainLM;
   }

   @Override
   public synchronized void releaseResources() {
      mainLM.releaseResources();
      if (appendFile != null && appendFile.isOpen()) {
         try {
            appendFile.close();
         }
         catch (Exception e) {
            ActiveMQServerLogger.LOGGER.largeMessageErrorReleasingResources(e);
         }
      }
   }

   @Override
   public synchronized void deleteFile() throws Exception {
      deleted = true;
      try {
         mainLM.deleteFile();
      }
      finally {
         deleteAppendFile();
      }
   }

   /**
    * @throws Exception
    */
   private void deleteAppendFile() throws Exception {
      if (appendFile != null) {
         if (appendFile.isOpen())
            appendFile.close();
         appendFile.delete();
      }
   }

   @Override
   public synchronized void addBytes(byte[] bytes) throws Exception {
      if (syncDone) {
         // this should never happen
         logger.warn("AddBytes was called on a complete LargeServerMessageinSync during replication, something is wrong with your codebase (perhaps you have mixed up jars on  your system) Original LargeMessage:" + mainLM);
         // If this happened we would at least complete the operation just a recovery mechanism
         mainLM.addBytes(bytes);
         return;
      }

      if (isTrace) {
         logger.trace("addBytes(bytes.length=" + bytes.length + ") on message=" + mainLM);
      }

      if (appendFile == null) {
         appendFile = storageManager.createFileForLargeMessage(mainLM.getMessageID(), LargeMessageExtension.SYNC);
      }

      if (!appendFile.isOpen()) {
         appendFile.open();
      }
      storageManager.addBytesToLargeMessage(appendFile, mainLM.getMessageID(), bytes);
   }

}
