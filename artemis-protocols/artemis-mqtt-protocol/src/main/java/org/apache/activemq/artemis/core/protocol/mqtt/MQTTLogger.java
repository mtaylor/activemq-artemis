package org.apache.activemq.artemis.core.protocol.mqtt;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Logger Code 83
 *
 * each message id must be 6 digits long starting with 10, the 3rd digit donates the level so
 *
 * INF0  1
 * WARN  2
 * DEBUG 3
 * ERROR 4
 * TRACE 5
 * FATAL 6
 *
 * so an INFO message would be 101000 to 101999
 */

@MessageLogger(projectCode = "AMQ")
public interface MQTTLogger extends BasicLogger
{
   MQTTLogger LOGGER = Logger.getMessageLogger(MQTTLogger.class, MQTTLogger.class.getPackage().getName());
}
