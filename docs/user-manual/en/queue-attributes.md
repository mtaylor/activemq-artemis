# Address and Queue Attributes and Settings

Address and queue attributes can be set in one of two ways. Either by configuring
them using the configuration file or by using the core API. This chapter
will explain how to configure each attribute and what effect the
attribute has.

## Address and Queue Attributes


## Using the API

Addresses and Queues can also be created using the core API or the management API.

For the core API, queues can be created via the
`org.apache.activemq.artemis.api.core.client.ClientSession` interface. There are
multiple `createQueue` methods that support setting all of the
previously mentioned attributes. There is one extra attribute that can
be set via this API which is `temporary`. setting this to true means
that the queue will be deleted once the session is disconnected.

Take a look at [Management](management.md) for a description of the management API for creating
queues.

## Configuring Addresses and Queues via Address Settings

There are some attributes that are defined against an address wildcard
rather than a specific address/queue. Here an example of an `address-setting`
entry that would be found in the `broker.xml` file.

    <address-settings>
       <address-setting match="jms.queue.exampleQueue">
          <dead-letter-address>jms.queue.deadLetterQueue</dead-letter-address>
          <max-delivery-attempts>3</max-delivery-attempts>
          <redelivery-delay>5000</redelivery-delay>
          <expiry-address>jms.queue.expiryQueue</expiry-address>
          <last-value-queue>true</last-value-queue>
          <max-size-bytes>100000</max-size-bytes>
          <page-size-bytes>20000</page-size-bytes>
          <redistribution-delay>0</redistribution-delay>
          <send-to-dla-on-no-route>true</send-to-dla-on-no-route>
          <address-full-policy>PAGE</address-full-policy>
          <slow-consumer-threshold>-1</slow-consumer-threshold>
          <slow-consumer-policy>NOTIFY</slow-consumer-policy>
          <slow-consumer-check-period>5</slow-consumer-check-period>
       </address-setting>
    </address-settings>

The idea with address settings, is you can provide a block of settings
which will be applied against any addresses that match the string in the
`match` attribute. In the above example the settings would only be
applied to any addresses which exactly match the address
`jms.queue.exampleQueue`, but you can also use wildcards to apply sets
of configuration against many addresses. The wildcard syntax used is
described [here](#wildcard-syntax).

For example, if you used the `match` string `jms.queue.#` the settings
would be applied to all addresses which start with `jms.queue.` which
would be all JMS queues.

The meaning of the specific settings are explained fully throughout the
user manual, however here is a brief description with a link to the
appropriate chapter if available.

`max-delivery-attempts` defines how many time a cancelled message can be
redelivered before sending to the `dead-letter-address`. A full
explanation can be found [here](#undelivered-messages.configuring).

`redelivery-delay` defines how long to wait before attempting redelivery
of a cancelled message. see [here](#undelivered-messages.delay).

`expiry-address` defines where to send a message that has expired. see
[here](#message-expiry.configuring).

`expiry-delay` defines the expiration time that will be used for
messages which are using the default expiration time (i.e. 0). For
example, if `expiry-delay` is set to "10" and a message which is using
the default expiration time (i.e. 0) arrives then its expiration time of
"0" will be changed to "10." However, if a message which is using an
expiration time of "20" arrives then its expiration time will remain
unchanged. Setting `expiry-delay` to "-1" will disable this feature. The
default is "-1".

`last-value-queue` defines whether a queue only uses last values or not.
see [here](#last-value-queues).

`max-size-bytes` and `page-size-bytes` are used to set paging on an
address. This is explained [here](#paging).

`redistribution-delay` defines how long to wait when the last consumer
is closed on a queue before redistributing any messages. see
[here](#clusters).

`send-to-dla-on-no-route`. If a message is sent to an address, but the
server does not route it to any queues, for example, there might be no
queues bound to that address, or none of the queues have filters that
match, then normally that message would be discarded. However if this
parameter is set to true for that address, if the message is not routed
to any queues it will instead be sent to the dead letter address (DLA)
for that address, if it exists.

`address-full-policy`. This attribute can have one of the following
values: PAGE, DROP, FAIL or BLOCK and determines what happens when an
address where `max-size-bytes` is specified becomes full. The default
value is PAGE. If the value is PAGE then further messages will be paged
to disk. If the value is DROP then further messages will be silently
dropped. If the value is FAIL then further messages will be dropped and
an exception will be thrown on the client-side. If the value is BLOCK
then client message producers will block when they try and send further
messages. See the following chapters for more info [Flow Control](flow-control.md), [Paging](paging.md).

`slow-consumer-threshold`. The minimum rate of message consumption
allowed before a consumer is considered "slow." Measured in
messages-per-second. Default is -1 (i.e. disabled); any other valid
value must be greater than 0.

`slow-consumer-policy`. What should happen when a slow consumer is
detected. `KILL` will kill the consumer's connection (which will
obviously impact any other client threads using that same connection).
`NOTIFY` will send a CONSUMER\_SLOW management notification which an
application could receive and take action with. See [slow consumers](slow-consumers.md) for more details
on this notification.

`slow-consumer-check-period`. How often to check for slow consumers on a
particular queue. Measured in seconds. Default is 5. See [slow consumers](slow-consumers.md)
for more information about slow consumer detection.

`auto-create-jms-queues`. Whether or not the broker should automatically
create a JMS queue when a JMS message is sent to a queue whose name fits
the address `match` (remember, a JMS queue is just a core queue which has
the same address and queue name) or a JMS consumer tries to connect to a
queue whose name fits the address `match`. Queues which are auto-created
are durable, non-temporary, and non-transient. Default is `true`. This is
_DEPRECATED_.  See `auto-create-queues`.

`auto-delete-jms-queues`. Whether or not the broker should automatically
delete auto-created JMS queues when they have both 0 consumers and 0 messages.
Default is `true`. This is _DEPRECATED_.  See `auto-delete-queues`.

`auto-create-jms-topics`. Whether or not the broker should automatically
create a JMS topic when a JMS message is sent to a topic whose name fits
the address `match` (remember, a JMS topic is just a core address which has 
one or more core queues mapped to it) or a JMS consumer tries to subscribe
to a topic whose name fits the address `match`. Default is `true`. This is
_DEPRECATED_.  See `auto-create-addresses`.

`auto-delete-jms-topics`. Whether or not the broker should automatically
delete auto-created JMS topics once the last subscription on the topic has
been closed. Default is `true`. This is _DEPRECATED_.  See `auto-delete-addresses`.

`auto-create-queues`. Whether or not the broker should automatically
create a queue when a message is sent or a consumer tries to connect to a
queue whose name fits the address `match`. Queues which are auto-created
are durable, non-temporary, and non-transient. Default is `true`.

`auto-delete-queues`. Whether or not the broker should automatically
delete auto-created queues when they have both 0 consumers and 0 messages.
Default is `true`.

`auto-create-addresses`. Whether or not the broker should automatically
create an address when a message is sent to or a consumer tries to consume
from a queue which is mapped to an address whose name fits the address `match`.
Default is `true`.

`auto-delete-addresses`. Whether or not the broker should automatically
delete auto-created addresses once the address no longer has any queues.
Default is `true`.
