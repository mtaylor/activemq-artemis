package org.apache.activemq.artemis.core.protocol.mqtt;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.core.remoting.FailureListener;

public class MQTTFailureListener implements FailureListener
{
   private MQTTConnectionManager connectionManager;

   public MQTTFailureListener(MQTTConnectionManager connectionManager)
   {
      this.connectionManager = connectionManager;
   }
   @Override
   public void connectionFailed(ActiveMQException exception, boolean failedOver)
   {
      connectionManager.disconnect();
   }

   @Override
   public void connectionFailed(ActiveMQException exception, boolean failedOver, String scaleDownTargetNodeID)
   {
      connectionManager.disconnect();
   }
}
