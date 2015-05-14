///**
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements. See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License. You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.activemq.artemis.core.protocol.mqtt.test.mock;
//
//import java.util.List;
//
//import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
//import org.apache.activemq.artemis.api.core.ActiveMQException;
//import org.apache.activemq.artemis.core.protocol.mqtt.MQTTConnection;
//import org.apache.activemq.artemis.core.remoting.CloseListener;
//import org.apache.activemq.artemis.core.remoting.FailureListener;
//import org.apache.activemq.artemis.spi.core.remoting.Connection;
//
//public class MockMQTTConnection extends MQTTConnection
//{
//   private boolean destroyed;
//
//   private MockNettyConnection transportConnection;
//
//   public MockMQTTConnection()
//   {
//      super(null, null);
//      this.destroyed = false;
//      this.transportConnection = new MockNettyConnection();
//   }
//
//   public ActiveMQBuffer getTransportOutBuffer()
//   {
//      return transportConnection.getOutBuffer();
//   }
//
//   public boolean getDestroyed()
//   {
//      return destroyed;
//   }
//
//   @Override
//   public Object getID()
//   {
//      return null;
//   }
//
//   @Override
//   public long getCreationTime()
//   {
//      return 0;
//   }
//
//   @Override
//   public String getRemoteAddress()
//   {
//      return null;
//   }
//
//   @Override
//   public void addFailureListener(FailureListener listener)
//   {
//
//   }
//
//   @Override
//   public boolean removeFailureListener(FailureListener listener)
//   {
//      return false;
//   }
//
//   @Override
//   public void addCloseListener(CloseListener listener)
//   {
//
//   }
//
//   @Override
//   public boolean removeCloseListener(CloseListener listener)
//   {
//      return false;
//   }
//
//   @Override
//   public List<CloseListener> removeCloseListeners()
//   {
//      return null;
//   }
//
//   @Override
//   public void setCloseListeners(List<CloseListener> listeners)
//   {
//
//   }
//
//   @Override
//   public List<FailureListener> getFailureListeners()
//   {
//      return null;
//   }
//
//   @Override
//   public List<FailureListener> removeFailureListeners()
//   {
//      return null;
//   }
//
//   @Override
//   public void setFailureListeners(List<FailureListener> listeners)
//   {
//
//   }
//
//   @Override
//   public ActiveMQBuffer createTransportBuffer(int size)
//   {
//      return null;
//   }
//
//   @Override
//   public void fail(ActiveMQException me)
//   {
//
//   }
//
//   @Override
//   public void fail(ActiveMQException me, String scaleDownTargetNodeID)
//   {
//
//   }
//
//   @Override
//   public void destroy()
//   {
//      destroyed = true;
//   }
//
//   @Override
//   public Connection getTransportConnection()
//   {
//      return transportConnection;
//   }
//
//   @Override
//   public boolean isClient()
//   {
//      return false;
//   }
//
//   @Override
//   public boolean isDestroyed()
//   {
//      return false;
//   }
//
//   @Override
//   public void disconnect(boolean criticalError)
//   {
//
//   }
//
//   @Override
//   public void disconnect(String scaleDownNodeID, boolean criticalError)
//   {
//
//   }
//
//   @Override
//   public boolean checkDataReceived()
//   {
//      return false;
//   }
//
//   @Override
//   public void flush()
//   {
//
//   }
//
//   @Override
//   public void bufferReceived(Object connectionID, ActiveMQBuffer buffer)
//   {
//
//   }
//}
