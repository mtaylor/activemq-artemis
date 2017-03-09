# Addresses, Queues, and Topics

Apache ActiveMQ Artemis has a unique addressing model that is both powerful and flexible and that offers great performance. The addressing model comprises three main concepts: addresses, queues and routing types.

An address represents a messaging endpoint. Within the configuration, a typical address is given a unique name, 0 or more queues, and a routing type.

A queue is associated with an address. There can be multiple queues per address. Once an incoming message is matched to an address, the message will be sent on to one or more of its queues, depending on the routing type configured. Queues can be configured to be automatically created and deleted.

A routing type determines how messages are sent to the queues associated with an address. A Apache ActiveMQ Artemis address can be configured with two different routing types.

Table 1. Routing Types

----------------------------------------------------------------------------------------------------
| If you want your messages routed to…​	                                  | Use this routing type …​ |
| ----------------------------------------------------------------------- | :---------------------: |
| A single queue within the matching address, in a point-to-point manner. | Anycast                 |
| Every queue within the matching address, in a publish-subscribe manner. | Multicast               |

multicast

Note
It is possible to define more than one routing type per address, but this typically results in an anti-pattern and is therefore not recommended.

If an address does use both routing types, however, and the client does not show a preference for either one, the broker typically defaults to the anycast routing type. The one exception is when the client uses the MQTT protocol. In that case, the default routing type is multicast.

Configuring an Address for Point-to-Point Messaging

Point-to-point messaging is a common scenario in which a message sent by a producer has only one consumer. AMQP and JMS message producers and consumers can make use of point-to-point messaging queues, for example. Define an anycast routing type for an address so that its queues receive messages in a point-to-point manner.

When a message is received on an address using anycast, Apache ActiveMQ Artemis locates the queue associated with the address and routes the message to it. When consumers request to consume from the address, the broker locates the relevant queue and associates this queue with the appropriate consumers. If multiple consumers are connected to the same queue, messages are distributed amongst each consumer equally, providing the consumers are equally able to handle them.

Publish-Subscribe
Figure 1. Point-to-Point
Configuring an Address to Use the Anycast Routing Type
Open the file <broker-instance>/etc/broker.xml for editing.

(Optional) Add an address configuration element and its associated queue if they do not exist already.

<configuration ...>
  <core ...>
    ...
    <address name="address.foo">
        <queue name="q1"/>
    </address>
  </core>
</configuration>
Wrap an anycast configuration element around the queue element in the address.

<configuration ...>
  <core ...>
    ...
    <address name="address.foo">
      <anycast>
        <queue name="q1"/>
      </anycast>
    </address>
  </core>
</configuration>
Configuring an Address for Publish-Subscribe Messaging

In a publish-subscribe scenario, messages are sent to every consumer subscribed to an address. JMS topics and MQTT subscriptions are two examples of publish-subscribe messaging. An example of a publish-subscribe Assign a multicast routing type for an address so that its queues receive messages in a pubish-subscribe manner.

When a message is received on an address with a multicast routing type, Apache ActiveMQ Artemis will route a copy of the message (in reality only a message reference to reduce the overhead of copying) to each queue.

Publish-Subscribe
Figure 2. Publish-Subscribe
Configuring an Address to Use the Multicast Routing Type
Open the file <broker-instance>/etc/broker.xml for editing.

(Optional) Add an address configuration element if one does not exist already.

<configuration ...>
  <core ...>
    ...
    <address name="topic.foo">
    </address>
  </core>
</configuration>
Add and empty multicast configuration element to the address

<configuration ...>
  <core ...>
    ...
    <address name="topic.foo">
      <multicast/>
    </address>
  </core>
</configuration>
(Optional) Add one more queue elements to the address and wrap the multicast element around them. This step is typically not needed since the broker will automatically create a queue for each subscription requested by a client.

<configuration ...>
  <core ...>
    ...
    <address name="topic.foo">
      <multicast>
        <queue name="client123.topic.foo"/>
        <queue name="client456.topic.foo"/>
      </multicast>
    </address>
  </core>
</configration>
Configuring a Point-to-Point Address with Two Queues

It is actually possible to define more than one queue on an address with an anycast routing type. When messages are received on such an address, they are firstly distributed evenly across all the defined queues. Using Fully Qualified Queue Names described later, clients are able to select the queue that they’d like to subscribe to. Should more than one consumer connect direct to a single queue, Apache ActiveMQ Artemis will take care of distributing messages between them, as in the example above.

Point-to-Point with Two Queues
Figure 3. Point-to-Point with Two Queues
Note
This is how Apache ActiveMQ Artemis handles load balancing of queues across multiple nodes in a cluster.
Configuring a Point-to-Point Address with Two Queues
Open the file <broker-instance>/etc/broker.xml for editing.

(Optional) Add an address configuration element and its associated queues if they do not exist already.

<configuration ...>
  <core ...>
    ...
    <address name="address.foo">
        <queue name="q1"/>
    </address>
  </core>
</configuration>
Wrap an anycast configuration element around the queue elements in the address.

<configuration ...>
  <core ...>
    ...
    <address name="address.foo">
      <anycast>
        <queue name="q1"/>
        <queue name="q2"/>
      </anycast>
    </address>
  </core>
</configuration>
Configuring an Address to Use Point-to-Point and Publish-Subscribe

It is possible to define an address with both point-to-point and publish-subscribe semantics enabled. While not typically recommend, this can be useful when you want, for example, a JMS Queue say orders and a JMS Topic named orders. The different routing types make the addresses appear to be distinct.

Using an example of JMS Clients, the messages sent by a JMS queue producer will be routed using the anycast routing type. Messages sent by a JMS topic producer will use the multicast routing type. In addition when a JMS topic consumer attaches it will be attached to it’s own subscription queue. JMS queue consumer will be attached to the anycast queue.

Point-to-Point and Publish-Subscribe
Figure 4. [Point-to-Point and Publish-Subscribe
Note
The behavior in this scenario is dependent on the protocol being used. For JMS there is a clear distinction between topic and queue producers and consumers, which make the logic straight forward. Other protocols like AMQP do not make this distinction. A message being sent via AMQP will be routed by both anycast and multicast and consumers will default to anycast. For more information, please check the behavior of each protocol in the sections on protocols.
The XML snippet below is an example of what the configuration for an address using both anycast and multicast would look like in <broker-instance>/etc/broker.xml. routing types. Note that subscription queues are typically created on demand, so there is no need to list specific queue elements inside the multicast routing type.

<configuration ...>
  <core ...>
    ...
    <address name="foo.orders">
      <anycast>
        <queue name="orders"/>
      </anycast>
      <multicast/>
    </address>
  </core>
</configuration>
Configuring Subscription Queues

In most cases it’s not necessary to pre-create subscription queues. The relevant protocol manager will take care of creating the subscription queues when the client initially requests to subscribe to an address. See Protocol Mangers and Adresses for more information. For durable subscriptions, the generated queue name is usually a concatenation of the client id and the address.

Configuring a Durable Subscription Queue

When an queue is configured as a durable subscription, the broker saves messages for any inactive subscribers and delivers them to the subscribers when they reconnect. Clients are therefore guaranteed to receive each message delivered to the queue after subscribing to it.

Configuring a Queue as a Durable Subscription
Open the file <broker-instance>/etc/broker.xml for editing.

(Optional) Add address and queue configuration elements if they do not exist already.

<configuration ...>
  <core ...>
    ...
    <address name="durable.foo">
        <queue name="q1"/>
    </address>
  </core>
</configuration>
(Optional) Wrap an anycast configuration element around the queue if it does not exist already.

<configuration ...>
  <core ...>
    ...
    <address name="durable.foo">
      <anycast>
        <queue name="q1" />
      </anycast>
    </address>
  </core>
</configuration>
Add the durable configuration element to the queue and assign it a value of true.

<configuration ...>
  <core ...>
    ...
    <address name="durable.foo">
      <anycast>
        <queue name="q1">
          <durable>true</durable>
        </queue>
      </anycast>
    </address>
  </core>
</configuration>
Configuring a Non-Shared Durable Subscription

The broker can be configured to prevent more than one consumer from connecting to a queue at any one time. The subscriptions to queues configured this way are therefore "non-shared".

Configuring a Queue as a Durable Subscription
Open the file <broker-instance>/etc/broker.xml for editing.

(Optional) Add address and queue configuration elements if they do not exist already.

<configuration ...>
  <core ...>
    ...
    <address name="non.shared.durable.foo">
        <queue name="orders1"/>
        <queue name="orders2"/>
    </address>
  </core>
</configuration>
(Optional) Add the multicast routing type if does not exist.

<configuration ...>
  <core ...>
    ...
    <address name="non.shared.durable.foo">
      <multicast>
        <queue name="orders1"/>
        <queue name="orders2"/>
      </multicast>
    </address>
  </core>
</configuration>
Add the durable configuration element to each queue.

<configuration ...>
  <core ...>
    ...
    <address name="non.shared.durable.foo">
      <multicast>
        <queue name="orders1">
          <durable>true</durable>
        </queue>
        <queue name="orders2">
          <durable>true</durable>
        </queue>
      </mutlicast>
    </address>
  </core>
</configuration>
Add the maxConsumers attribute to each queue element and assign it a value of 1.

<configuration ...>
  <core ...>
    ...
    <address name="non.shared.durable.foo">
      <multicast>
        <queue name="orders1" maxConsumers="1">
          <durable>true</durable>
        </queue>
        <queue name="orders2" maxConsumers="1">
          <durable>true</durable>
        </queue>
      </mutlicast>
    </address>
  </core>
</configuration>
Creating and Deleting Queues Automatically

You can configure {ProductName} to automatically create addresses and then delete them when they are no longer in use. This saves you from having to preconfigure each address before a client can connect to it. Automatic creation and deletion is configured on a per address basis and is controlled by three configuration elements:

auto-create-addresses
When set to true, the broker will create the address requested by the client if it does not exist already. The default is true.

auto-delete-addresses
When set to true, the broker will be delete the address once all of it’s queues have been deleted. The default is true

default-address-routing-type
The routing type to use if the client does not specify one. Possible values are MULTICAST and ANYCAST. See earlier in this chapter for more information about routing types. The default value is MULTICAST.

Configuring an Address to be Automatically Created
Edit the file <broker-instance>/etc/broker.xml and add the auto-create-addresses element to the address-setting you want the broker to automatically create.

(Optional) Add the address-setting if it does not exits. Use the match parameter and the The Apache ActiveMQ Artemis Wildcard Syntax to match more than one specific address.

Set auto-create-addresses to true

(Optional) Assign MULTICAST or ANYCAST as the default routing type for the address.

The example below configures an address-setting to be automatically created by the broker. The default routing type to be used if not specified by the client is MULTICAST. Note that wildcard syntax is used. Any address starting with /news/politics/ will be automatically created by the broker.

<configuration ...>
  <core ...>
    ...
    <address-settings>
       <address-setting match="/news/politics/#">
          <auto-create-addresses>true</auto-create-addresses>
          <default-address-routing-type>MULTICAST</default-address-routing-type>
       </address-setting>
    </address-settings>
    ...
  </core>
</configuration>
Configuring and Address to be Automatically Deleted
Edit the file <broker-instance>/etc/broker.xml and add the auto-delete-addresses element to the address-setting you want the broker to automatically create.

(Optional) Add the address-setting if it does not exits. Use the match parameter and the The Apache ActiveMQ Artemis Wildcard Syntax to match more than one specific address.

Set auto-delete-addresses to true

The example below configures an address-setting to be automatically deleted by the broker. Note that wildcard syntax is used. Any address request by the client that starts with /news/politics/ is configured to be automatically deleted by the broker.

<configuration ...>
  <core ...>
    ...
    <address-settings>
       <address-setting match="/news/politics/#">
          <auto-create-addresses>true</auto-create-addresses>
          <default-address-routing-type>MULTICAST</default-address-routing-type>
       </address-setting>
    </address-settings>
    ...
  </core>
</configuration>
Using a Fully Qualified Queue Name

Internally the broker maps a client’s request for an address to specific queues. The broker decides on behalf of the client which queues to send messages to or from which queue to receive messages. However, more advanced use cases might require that the client specify a queue directly. In these situations the client and use a fully qualified queue name, by specifying both the address name and the queue name, separated by a ::.

Specifying a Fully Qualified Queue Name
In this example, the address foo is configured with two queues q1, q2 as shown in the configuration below.

<configuration ...>
  <core ...>
    ...
    <addresses>
       <address name="foo">
          <anycast>
             <queue name="q1" />
             <queue name="q2" />
          </anycast>
       </address>
    </addresses>
  </core>
</configuration>
In the client code, use both the address name and the queue name when requesting a connection from the broker. Remember to use two colons, ::, to separate the names, as in the example Java code below.

String FQQN = "foo::q1";
Queue q1 session.createQueue(FQQN);
MessageConsumer consumer = session.createConsumer(q1);
Configuring Sharded Queues

A common pattern for processing of messages across a queue where only partial ordering is required is to use queue sharding. In Artemis this can be achieved by creating an anycast address that acts as a single logical queue, but which is backed by many underlying physical queues.

Configuring a Sharded Queue
Open <broker-instance>/etc/broker.xml and add an address with the desired name. In the example below the address named sharded is added to the configuration.

<configuration ...>
  <core ...>
    ...
    <addresses>
       <address name="sharded"></address>
    </addresses>
  </core>
</configuration>
Add a the anycast routing type and include the desired number of sharded queues. In the example below, the queues q1, q2, and q3 are added as anycast destinations.

<configuration ...>
  <core ...>
    ...
    <addresses>
       <address name="sharded">
          <anycast>
             <queue name="q1" />
             <queue name="q2" />
             <queue name="q3" />
          </anycast>
       </address>
    </addresses>
</core>
</configuration>
Using the configuration above, messages sent to foo are distributed equally across q1, q2 and q3. Clients are able to connect directly to a specific physical queue when using a fully qualified queue name and will receive messages sent to that specific queue only.

In order to tie particular messages to a particular queue, clients can specify a message group for each message. A message group will be associated with a specific queue and therefore all messages sent to any one group will always get sent to the same queue.

Limiting the Number of Consumers Connected to a Queue

It’s possible to limit the number of consumers connected to for a particular queue by using the max-consumers attribute. Create an exclusive consumer by setting max-consumers flag can be set to 1. The default value is -1, which is sets an unlimited number of consumers.

Limiting the Number of Consumers for a Queue
Open <broker-instance>/etc/broker.xml and add the max-consumers attribute to the desired queue. In the example below, only 20 consumers can connect to the queue q3 at the same time.

<configuration ...>
  <core ...>
    ...
    <addresses>
       <address name="foo">
          <anycast>
             <queue name="q3" max-consumers="20"/>
          </anycast>
       </address>
    </addresses>
  </core>
</configuration>
(Optional) Create an exclusive consumer by setting max-consumers to 1, as in the example below.

<configuration ...>
  <core ...>
    ...
    <address name="foo">
      <anycast>
        <queue name="q3" max-consumers="1"/>
      </anycast>
    </address>
  </core>
</configuration>
(Optional) Have an unlimited number of consumers by setting max-consumers to -1, as in the example below.

<configuration ...>
  <core ...>
    ...
    <address name="foo">
      <anycast>
         <queue name="q3" max-consumers="-1"/>
      </anycast>
    </address>
  </core>
</configuration>
Configuring a Prefix to Connect to a Specific Routing Type

Normally, if a message is received by an address that uses both anycast and multicast, one of the anycast queues will receiving the message and all of the multicast queues. However, clients can specify a special prefix when connecting to an address to specify whether to connect using anycast or multicast. The prefixes are custom values that are designated using the anycastPrefix and multicastPrefix parameters within the URL of an acceptor.

Configuring an Anycast Prefix
In <broker-instance>/etc/broker.xml, add the anycastPrefix to the URL of the desired acceptor. In the example below, the acceptor is configured to use anycast:// for the anycastPrefix. Client code can specify anycast://foo/ if the client needs to send a message to only one of the anycast queues.

<configuration ...>
  <core ...>
    ...
      <acceptors>
         <!-- Acceptor for every supported protocol -->
         <acceptor name="artemis">tcp://0.0.0.0:61616?protocols=AMQP&anycastPrefix=anycast://</acceptor>
      </acceptors>
    ...
  </core>
</configuration>
Configuring a Multicast Prefix
In <broker-instance>/etc/broker.xml, add the anycastPrefix to the URL of the desired acceptor. In the example below, the acceptor is configured to use multicast:// for the multicastPrefix. Client code can specify multicast://foo/ if the client needs the message sent to only the multicast queues of the address.

<configuration ...>
  <core ...>
    ...
      <acceptors>
         <!-- Acceptor for every supported protocol -->
         <acceptor name="artemis">tcp://0.0.0.0:61616?protocols=AMQP&multicastPrefix=multicast://</acceptor>
      </acceptors>
    ...
  </core>
</configuration>
Protocol Managers and Addresses

A protocol manager maps protocol specific concepts down to the Apache ActiveMQ Artemis core model of addresses, queues and routing types. For example, when a client sends a MQTT subscription packet with the addresses /house/room1/lights and /house/room2/lights, the MQTT protocol manager understands that the two addresses require multicast semantics. The protocol manager will therefore first look to ensure that multicast is enabled for both addresses. If not, it will attempt to dynamically create them. If successful, the protocol manger then creates special subscription queues for each subscription requested by the client.

Each protocol behaves slightly differently. The table below describes what typically happens when subscribe frames to various types of queue are requested.

Table 2. Protocol Manager Actions
If the queue is of this type…​	The typical action for a protocol manager is to…​
Durable Subscription Queue

Look for the appropriate address and ensures that multicast semantics is enabled. It then creates a special subscription queue with the client ID and the address as it’s name and multicast as it’s routing type.

The special name allows the protocol manager to quickly identify the required client subscription queues should the client disconnect and reconnect at a later date.

When the client unsubscribes the queue is deleted.

Temporary Subscription Queue

Look for the appropriate address and ensures that multicast semantics is enabled. It then creates a queue with a random (read UUID) name under this address with multicast routing type.

When the client disconnects the queue is deleted.

Point-to-Point Queue

Look for the appropriate address and ensures that anycast routing type is enabled. If it is it will aim to locate a queue with the same name as the address. If it does not exist, it will look for the first queue available. It this does not exist then it will auto create the queue (providing auto create is enabled). The queue consumer is bound to this queue.

If the queue is auto created, it will be auto deleted once there are no consumers and no messages in it.