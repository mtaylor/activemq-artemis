/**
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

package org.apache.activemq.artemis.core.protocol.mqtt.test.mock;

import javax.management.MBeanServer;
import javax.transaction.xa.Xid;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.Pair;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.DivertConfiguration;
import org.apache.activemq.artemis.core.journal.IOAsyncTask;
import org.apache.activemq.artemis.core.journal.Journal;
import org.apache.activemq.artemis.core.journal.JournalLoadInformation;
import org.apache.activemq.artemis.core.journal.SequentialFile;
import org.apache.activemq.artemis.core.management.impl.ActiveMQServerControlImpl;
import org.apache.activemq.artemis.core.message.impl.MessageInternal;
import org.apache.activemq.artemis.core.paging.PageTransactionInfo;
import org.apache.activemq.artemis.core.paging.PagedMessage;
import org.apache.activemq.artemis.core.paging.PagingManager;
import org.apache.activemq.artemis.core.paging.PagingStore;
import org.apache.activemq.artemis.core.paging.cursor.PagePosition;
import org.apache.activemq.artemis.core.persistence.GroupingInfo;
import org.apache.activemq.artemis.core.persistence.OperationContext;
import org.apache.activemq.artemis.core.persistence.QueueBindingInfo;
import org.apache.activemq.artemis.core.persistence.StorageManager;
import org.apache.activemq.artemis.core.persistence.config.PersistedAddressSetting;
import org.apache.activemq.artemis.core.persistence.config.PersistedRoles;
import org.apache.activemq.artemis.core.persistence.impl.PageCountPending;
import org.apache.activemq.artemis.core.postoffice.Binding;
import org.apache.activemq.artemis.core.postoffice.PostOffice;
import org.apache.activemq.artemis.core.remoting.server.RemotingService;
import org.apache.activemq.artemis.core.replication.ReplicationManager;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.security.SecurityStore;
import org.apache.activemq.artemis.core.server.ActivateCallback;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.LargeServerMessage;
import org.apache.activemq.artemis.core.server.MessageReference;
import org.apache.activemq.artemis.core.server.NodeManager;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.QueueCreator;
import org.apache.activemq.artemis.core.server.QueueFactory;
import org.apache.activemq.artemis.core.server.RouteContextList;
import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.ServerSessionFactory;
import org.apache.activemq.artemis.core.server.cluster.ClusterManager;
import org.apache.activemq.artemis.core.server.cluster.ha.HAPolicy;
import org.apache.activemq.artemis.core.server.group.GroupingHandler;
import org.apache.activemq.artemis.core.server.group.impl.GroupBinding;
import org.apache.activemq.artemis.core.server.impl.Activation;
import org.apache.activemq.artemis.core.server.impl.ConnectorsService;
import org.apache.activemq.artemis.core.server.impl.JournalLoader;
import org.apache.activemq.artemis.core.server.management.ManagementService;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.core.transaction.ResourceManager;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.core.version.Version;
import org.apache.activemq.artemis.spi.core.protocol.ProtocolManagerFactory;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.protocol.SessionCallback;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.activemq.artemis.utils.ExecutorFactory;

public class MockActiveMQServer implements ActiveMQServer
{
   @Override
   public void setIdentity(String identity)
   {

   }

   @Override
   public String getIdentity()
   {
      return null;
   }

   @Override
   public String describe()
   {
      return null;
   }

   @Override
   public void addActivationParam(String key, Object val)
   {

   }

   @Override
   public Configuration getConfiguration()
   {
      return null;
   }

   @Override
   public RemotingService getRemotingService()
   {
      return null;
   }

   @Override
   public StorageManager getStorageManager()
   {
      return new MockStorageManager();
   }

   @Override
   public PagingManager getPagingManager()
   {
      return null;
   }

   @Override
   public ManagementService getManagementService()
   {
      return null;
   }

   @Override
   public ActiveMQSecurityManager getSecurityManager()
   {
      return null;
   }

   @Override
   public Version getVersion()
   {
      return null;
   }

   @Override
   public NodeManager getNodeManager()
   {
      return null;
   }

   @Override
   public ActiveMQServerControlImpl getActiveMQServerControl()
   {
      return null;
   }

   @Override
   public void registerActivateCallback(ActivateCallback callback)
   {

   }

   @Override
   public void unregisterActivateCallback(ActivateCallback callback)
   {

   }

   @Override
   public ServerSession createSession(String name, String username, String password, int minLargeMessageSize, RemotingConnection remotingConnection, boolean autoCommitSends, boolean autoCommitAcks, boolean preAcknowledge, boolean xa, String defaultAddress, SessionCallback callback, ServerSessionFactory sessionFactory, boolean autoCreateQueues) throws Exception
   {
      return null;
   }

   @Override
   public SecurityStore getSecurityStore()
   {
      return null;
   }

   @Override
   public void removeSession(String name) throws Exception
   {

   }

   @Override
   public Set<ServerSession> getSessions()
   {
      return null;
   }

   @Override
   public HierarchicalRepository<Set<Role>> getSecurityRepository()
   {
      return null;
   }

   @Override
   public HierarchicalRepository<AddressSettings> getAddressSettingsRepository()
   {
      return null;
   }

   @Override
   public int getConnectionCount()
   {
      return 0;
   }

   @Override
   public PostOffice getPostOffice()
   {
      return null;
   }

   @Override
   public QueueFactory getQueueFactory()
   {
      return null;
   }

   @Override
   public ResourceManager getResourceManager()
   {
      return null;
   }

   @Override
   public List<ServerSession> getSessions(String connectionID)
   {
      return null;
   }

   @Override
   public ServerSession lookupSession(String metakey, String metavalue)
   {
      return null;
   }

   @Override
   public ClusterManager getClusterManager()
   {
      return null;
   }

   @Override
   public SimpleString getNodeID()
   {
      return null;
   }

   @Override
   public boolean isActive()
   {
      return false;
   }

   @Override
   public void setJMSQueueCreator(QueueCreator queueCreator)
   {

   }

   @Override
   public QueueCreator getJMSQueueCreator()
   {
      return null;
   }

   @Override
   public boolean waitForActivation(long timeout, TimeUnit unit) throws InterruptedException
   {
      return false;
   }

   @Override
   public void createSharedQueue(SimpleString address, SimpleString name, SimpleString filterString, SimpleString user, boolean durable) throws Exception
   {

   }

   @Override
   public Queue createQueue(SimpleString address, SimpleString queueName, SimpleString filter, boolean durable, boolean temporary) throws Exception
   {
      return null;
   }

   @Override
   public Queue createQueue(SimpleString address, SimpleString queueName, SimpleString filter, SimpleString user, boolean durable, boolean temporary) throws Exception
   {
      return null;
   }

   @Override
   public Queue createQueue(SimpleString address, SimpleString queueName, SimpleString filter, SimpleString user, boolean durable, boolean temporary, boolean autoCreated) throws Exception
   {
      return null;
   }

   @Override
   public Queue deployQueue(SimpleString address, SimpleString queueName, SimpleString filterString, boolean durable, boolean temporary) throws Exception
   {
      return null;
   }

   @Override
   public Queue locateQueue(SimpleString queueName)
   {
      return null;
   }

   @Override
   public void destroyQueue(SimpleString queueName) throws Exception
   {

   }

   @Override
   public void destroyQueue(SimpleString queueName, ServerSession session) throws Exception
   {

   }

   @Override
   public void destroyQueue(SimpleString queueName, ServerSession session, boolean checkConsumerCount) throws Exception
   {

   }

   @Override
   public void destroyQueue(SimpleString queueName, ServerSession session, boolean checkConsumerCount, boolean removeConsumers) throws Exception
   {

   }

   @Override
   public String destroyConnectionWithSessionMetadata(String metaKey, String metaValue) throws Exception
   {
      return null;
   }

   @Override
   public ScheduledExecutorService getScheduledPool()
   {
      return null;
   }

   @Override
   public ExecutorFactory getExecutorFactory()
   {
      return null;
   }

   @Override
   public void setGroupingHandler(GroupingHandler groupingHandler)
   {

   }

   @Override
   public GroupingHandler getGroupingHandler()
   {
      return null;
   }

   @Override
   public ReplicationManager getReplicationManager()
   {
      return null;
   }

   @Override
   public void deployDivert(DivertConfiguration config) throws Exception
   {

   }

   @Override
   public void destroyDivert(SimpleString name) throws Exception
   {

   }

   @Override
   public ConnectorsService getConnectorsService()
   {
      return null;
   }

   @Override
   public void deployBridge(BridgeConfiguration config) throws Exception
   {

   }

   @Override
   public void destroyBridge(String name) throws Exception
   {

   }

   @Override
   public ServerSession getSessionByID(String sessionID)
   {
      return null;
   }

   @Override
   public void threadDump(String reason)
   {

   }

   @Override
   public boolean isAddressBound(String address) throws Exception
   {
      return false;
   }

   @Override
   public void stop(boolean failoverOnServerShutdown) throws Exception
   {

   }

   @Override
   public void addProtocolManagerFactory(ProtocolManagerFactory factory)
   {

   }

   @Override
   public void removeProtocolManagerFactory(ProtocolManagerFactory factory)
   {

   }

   @Override
   public ActiveMQServer createBackupServer(Configuration configuration)
   {
      return null;
   }

   @Override
   public void addScaledDownNode(SimpleString scaledDownNodeId)
   {

   }

   @Override
   public boolean hasScaledDown(SimpleString scaledDownNodeId)
   {
      return false;
   }

   @Override
   public Activation getActivation()
   {
      return null;
   }

   @Override
   public HAPolicy getHAPolicy()
   {
      return null;
   }

   @Override
   public void setHAPolicy(HAPolicy haPolicy)
   {

   }

   @Override
   public void setMBeanServer(MBeanServer mBeanServer)
   {

   }

   @Override
   public void start() throws Exception
   {

   }

   @Override
   public void stop() throws Exception
   {

   }

   @Override
   public boolean isStarted()
   {
      return false;
   }

   private class MockStorageManager implements StorageManager
   {
      private long ids = 0;

      @Override
      public OperationContext getContext()
      {
         return null;
      }

      @Override
      public void lineUpContext()
      {

      }

      @Override
      public OperationContext newContext(Executor executor)
      {
         return null;
      }

      @Override
      public OperationContext newSingleThreadContext()
      {
         return null;
      }

      @Override
      public void setContext(OperationContext context)
      {

      }

      @Override
      public void stop(boolean ioCriticalError) throws Exception
      {

      }

      @Override
      public void pageClosed(SimpleString storeName, int pageNumber)
      {

      }

      @Override
      public void pageDeleted(SimpleString storeName, int pageNumber)
      {

      }

      @Override
      public void pageWrite(PagedMessage message, int pageNumber)
      {

      }

      @Override
      public void afterCompleteOperations(IOAsyncTask run)
      {

      }

      @Override
      public boolean waitOnOperations(long timeout) throws Exception
      {
         return false;
      }

      @Override
      public void waitOnOperations() throws Exception
      {

      }

      @Override
      public void beforePageRead() throws Exception
      {

      }

      @Override
      public void afterPageRead() throws Exception
      {

      }

      @Override
      public ByteBuffer allocateDirectBuffer(int size)
      {
         return null;
      }

      @Override
      public void freeDirectBuffer(ByteBuffer buffer)
      {

      }

      @Override
      public void clearContext()
      {

      }

      @Override
      public void confirmPendingLargeMessageTX(Transaction transaction, long messageID, long recordID) throws Exception
      {

      }

      @Override
      public void confirmPendingLargeMessage(long recordID) throws Exception
      {

      }

      @Override
      public void storeMessage(ServerMessage message) throws Exception
      {

      }

      @Override
      public void storeReference(long queueID, long messageID, boolean last) throws Exception
      {

      }

      @Override
      public void deleteMessage(long messageID) throws Exception
      {

      }

      @Override
      public void storeAcknowledge(long queueID, long messageID) throws Exception
      {

      }

      @Override
      public void storeCursorAcknowledge(long queueID, PagePosition position) throws Exception
      {

      }

      @Override
      public void updateDeliveryCount(MessageReference ref) throws Exception
      {

      }

      @Override
      public void updateScheduledDeliveryTime(MessageReference ref) throws Exception
      {

      }

      @Override
      public void storeDuplicateID(SimpleString address, byte[] duplID, long recordID) throws Exception
      {

      }

      @Override
      public void deleteDuplicateID(long recordID) throws Exception
      {

      }

      @Override
      public void storeMessageTransactional(long txID, ServerMessage message) throws Exception
      {

      }

      @Override
      public void storeReferenceTransactional(long txID, long queueID, long messageID) throws Exception
      {

      }

      @Override
      public void storeAcknowledgeTransactional(long txID, long queueID, long messageID) throws Exception
      {

      }

      @Override
      public void storeCursorAcknowledgeTransactional(long txID, long queueID, PagePosition position) throws Exception
      {

      }

      @Override
      public void deleteCursorAcknowledgeTransactional(long txID, long ackID) throws Exception
      {

      }

      @Override
      public void deleteCursorAcknowledge(long ackID) throws Exception
      {

      }

      @Override
      public void storePageCompleteTransactional(long txID, long queueID, PagePosition position) throws Exception
      {

      }

      @Override
      public void deletePageComplete(long ackID) throws Exception
      {

      }

      @Override
      public void updateScheduledDeliveryTimeTransactional(long txID, MessageReference ref) throws Exception
      {

      }

      @Override
      public void storeDuplicateIDTransactional(long txID, SimpleString address, byte[] duplID, long recordID) throws Exception
      {

      }

      @Override
      public void updateDuplicateIDTransactional(long txID, SimpleString address, byte[] duplID, long recordID) throws Exception
      {

      }

      @Override
      public void deleteDuplicateIDTransactional(long txID, long recordID) throws Exception
      {

      }

      @Override
      public LargeServerMessage createLargeMessage()
      {
         return null;
      }

      @Override
      public LargeServerMessage createLargeMessage(long id, MessageInternal message) throws Exception
      {
         return null;
      }

      @Override
      public SequentialFile createFileForLargeMessage(long messageID, LargeMessageExtension extension)
      {
         return null;
      }

      @Override
      public void prepare(long txID, Xid xid) throws Exception
      {

      }

      @Override
      public void commit(long txID) throws Exception
      {

      }

      @Override
      public void commit(long txID, boolean lineUpContext) throws Exception
      {

      }

      @Override
      public void rollback(long txID) throws Exception
      {

      }

      @Override
      public void rollbackBindings(long txID) throws Exception
      {

      }

      @Override
      public void commitBindings(long txID) throws Exception
      {

      }

      @Override
      public void storePageTransaction(long txID, PageTransactionInfo pageTransaction) throws Exception
      {

      }

      @Override
      public void updatePageTransaction(long txID, PageTransactionInfo pageTransaction, int depage) throws Exception
      {

      }

      @Override
      public void updatePageTransaction(PageTransactionInfo pageTransaction, int depage) throws Exception
      {

      }

      @Override
      public void deletePageTransactional(long recordID) throws Exception
      {

      }

      @Override
      public JournalLoadInformation loadMessageJournal(PostOffice postOffice, PagingManager pagingManager, ResourceManager resourceManager, Map<Long, QueueBindingInfo> queueInfos, Map<SimpleString, List<Pair<byte[], Long>>> duplicateIDMap, Set<Pair<Long, Long>> pendingLargeMessages, List<PageCountPending> pendingNonTXPageCounter, JournalLoader journalLoader) throws Exception
      {
         return null;
      }

      @Override
      public long storeHeuristicCompletion(Xid xid, boolean isCommit) throws Exception
      {
         return 0;
      }

      @Override
      public void deleteHeuristicCompletion(long id) throws Exception
      {

      }

      @Override
      public void addQueueBinding(long tx, Binding binding) throws Exception
      {

      }

      @Override
      public void deleteQueueBinding(long tx, long queueBindingID) throws Exception
      {

      }

      @Override
      public JournalLoadInformation loadBindingJournal(List<QueueBindingInfo> queueBindingInfos, List<GroupingInfo> groupingInfos) throws Exception
      {
         return null;
      }

      @Override
      public void addGrouping(GroupBinding groupBinding) throws Exception
      {

      }

      @Override
      public void deleteGrouping(long tx, GroupBinding groupBinding) throws Exception
      {

      }

      @Override
      public void storeAddressSetting(PersistedAddressSetting addressSetting) throws Exception
      {

      }

      @Override
      public void deleteAddressSetting(SimpleString addressMatch) throws Exception
      {

      }

      @Override
      public List<PersistedAddressSetting> recoverAddressSettings() throws Exception
      {
         return null;
      }

      @Override
      public void storeSecurityRoles(PersistedRoles persistedRoles) throws Exception
      {

      }

      @Override
      public void deleteSecurityRoles(SimpleString addressMatch) throws Exception
      {

      }

      @Override
      public List<PersistedRoles> recoverPersistedRoles() throws Exception
      {
         return null;
      }

      @Override
      public long storePageCounter(long txID, long queueID, long value) throws Exception
      {
         return 0;
      }

      @Override
      public long storePendingCounter(long queueID, long pageID, int inc) throws Exception
      {
         return 0;
      }

      @Override
      public void deleteIncrementRecord(long txID, long recordID) throws Exception
      {

      }

      @Override
      public void deletePageCounter(long txID, long recordID) throws Exception
      {

      }

      @Override
      public void deletePendingPageCounter(long txID, long recordID) throws Exception
      {

      }

      @Override
      public long storePageCounterInc(long txID, long queueID, int add) throws Exception
      {
         return 0;
      }

      @Override
      public long storePageCounterInc(long queueID, int add) throws Exception
      {
         return 0;
      }

      @Override
      public Journal getBindingsJournal()
      {
         return null;
      }

      @Override
      public Journal getMessageJournal()
      {
         return null;
      }

      @Override
      public void startReplication(ReplicationManager replicationManager, PagingManager pagingManager, String nodeID, boolean autoFailBack) throws Exception
      {

      }

      @Override
      public boolean addToPage(PagingStore store, ServerMessage msg, Transaction tx, RouteContextList listCtx) throws Exception
      {
         return false;
      }

      @Override
      public void stopReplication()
      {

      }

      @Override
      public void addBytesToLargeMessage(SequentialFile appendFile, long messageID, byte[] bytes) throws Exception
      {

      }

      @Override
      public void storeID(long journalID, long id) throws Exception
      {

      }

      @Override
      public void deleteID(long journalD) throws Exception
      {

      }

      @Override
      public void readLock()
      {

      }

      @Override
      public void readUnLock()
      {

      }

      @Override
      public void persistIdGenerator()
      {

      }

      @Override
      public void start() throws Exception
      {

      }

      @Override
      public void stop() throws Exception
      {

      }

      @Override
      public boolean isStarted()
      {
         return false;
      }

      @Override
      public long generateID()
      {
         return ids++;
      }

      @Override
      public long getCurrentID()
      {
         return 0;
      }
   }
}
