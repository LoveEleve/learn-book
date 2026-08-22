大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端** **消息数据主从同步机制**」，以「**创建 Topic 场景**」为例来深度剖析 Kafka Kraft 模式下 「**主从之间是如何同步消息数据的**」的三个维度，让你更好的理解其处理全流程，今天我们接着来深度聊聊「**Kafka 服务端 KRaft 节点监控与故障转移流程**」，看看 Kafka Kraft 模式下「**节点故障如何转移的**」。

![](https://article-images.zsxq.com/Fv5ie0cv4hCqQtEYrtx-7hGb-vhP)

## **01 总体概述**

本篇我们来剖析一下「**Kafka KRaft 模块**」的重要功能：对 Broker 节点进行监控，在 Broker 节点故障下线后，执行故障转移操作。

当 Broker 节点启动后，会在 KRaft 模块中进行注册，并定时发送心跳给「**Leader 节点**」。如果某个 Broker 节点超时没有发送心跳，则「**Leader 节点**」会判定该 Broker 节点已经故障下线了，此时就会执行「**故障转移操作**」，即重新选举新的「**Leader 节点**」的操作。

这套机制有以下两种关键请求，是本文要重点进行剖析的：

1.  节点注册请求 [BrokerRegistration](http://brokerregistration/)：当 Broker 节点启动时会向集群 Leader 节点「**发送注册请求**」，集群 Leader 节点处理请求并将节点状态维护在本地列表中，请求完成后 Broker 节点会定时向集群 「**Leader 节点**」上报心跳。
2.  节点心跳请求 [BrokerHeartbeat](http://brokerheartbeat/)：当 Leader 节点「**收到节点心跳**」时将检查本地注册列表中是否有节点超时，如有超时则需要进行下线处理，将分布在失败节点上的分区 Leader 副本重新进行选主，同时生成元数据的变动记录，关于元数据变动传播生效的机制可以点击 [【服务端 Broker 源码分析系列第三十九篇】图解 Kafka 源码之 KRaft 元数据主从同步机制](https://articles.zsxq.com/id_lgowqtnbcrbw.html) 。

整个流程如下图所示：

![](https://article-images.zsxq.com/liAidzxRSRhA5Z4tcuwwg9BWYIHi)

本文涉及的源码：

「**BrokerServer**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerServer.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerServer.scala)

「**BrokerLifecycleManager**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerLifecycleManager.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/BrokerServer.scala)

「**ControllerApis**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerApis.scala](https://github.com/apache/kafka/blob/3.0.0/core/src/main/scala/kafka/server/ControllerApis.scala)

「**QuorumController**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/QuorumController.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/image/MetadataDelta.java)

「**ClusterControlManager**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/ClusterControlManager.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/image/MetadataDelta.java)

「**BrokerHeartbeatManager**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/BrokerHeartbeatManager.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/BrokerHeartbeatManager.java)

「**BrokerControlState**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/BrokerControlState.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/BrokerControlState.java)

「**ReplicationControlManager**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java)

「**PartitionChangeBuilder**」类源码在 Kafka 源码包的 metadata 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java](https://github.com/apache/kafka/blob/3.0.0/metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java)

## **02 节点注册**

## **2.1 发起节点注册请求**

## **2.1.1 BrokerServer#startup()**

当配置了 [process.roles](http://process.roles/) 属性的 Broker 节点会在启动的时候创建 [BrokerServer](http://brokerserver%20/) [](http://brokerserver%20/)对象，并执行 [BrokerServer.scala#startup](http://brokerserver.scala/#startup)() 方法进行基本的初始化工作，其中和本篇相关的部分是调用 [BrokerLifecycleManager.scala#start()](http://brokerlifecyclemanager.scala/#start\(\)) 方法启动 Broker 生命周期管理器。

def startup(): Unit = {

if (!maybeChangeStatus(SHUTDOWN, STARTING)) return

try {

info("Starting broker")

....

// 启动 Broker 生命周期管理器

lifecycleManager.start(() => metadataListener.highestMetadataOffset(),

BrokerToControllerChannelManager(controllerNodeProvider, time, metrics, config,

"heartbeat", threadNamePrefix, config.brokerSessionTimeoutMs.toLong),

metaProps.clusterId, networkListeners, supportedFeatures)

// Register a listener with the Raft layer to receive metadata event notifications

raftManager.register(metadataListener)

....

// Block until we've caught up with the latest metadata from the controller quorum.

lifecycleManager.initialCatchUpFuture.get()

// Apply the metadata log changes that we've accumulated.

metadataPublisher = new BrokerMetadataPublisher(config, metadataCache,

logManager, replicaManager, groupCoordinator, transactionCoordinator,

clientQuotaMetadataManager, featureCache, dynamicConfigHandlers.toMap)

// Tell the metadata listener to start publishing its output, and wait for the first

// publish operation to complete. This first operation will initialize logManager,

// replicaManager, groupCoordinator, and txnCoordinator. The log manager may perform

// a potentially lengthy recovery-from-unclean-shutdown operation here, if required.

metadataListener.startPublishing(metadataPublisher).get()

// Log static broker configurations.

new KafkaConfig(config.originals(), true)

// Enable inbound TCP connections.

socketServer.startProcessingRequests(authorizerFutures)

// We're now ready to unfence the broker. This also allows this broker to transition

// from RECOVERY state to RUNNING state, once the controller unfences the broker.

lifecycleManager.setReadyToUnfence()

// 最后将状态从 STARTING 转变为 STARTED

maybeChangeStatus(STARTING, STARTED)

} catch {

case e: Throwable =>

maybeChangeStatus(STARTING, STARTED)

fatal("Fatal error during broker startup. Prepare to shutdown", e)

shutdown()

throw e

}

}

这里关键是调用 [lifecycleManager.setReadyToUnfence()](http://lifecyclemanager.setreadytounfence\(\)/) 方法来发送注册请求给 KRaft 集群中的 「**Leader 节点**」。

## **2.1.2 BrokerLifecycleManager#startup()**

/\*\*

\* Start the BrokerLifecycleManager.

\* @param highestMetadataOffsetProvider Provides the current highest metadata offset.

\* @param channelManager The brokerToControllerChannelManager to use.

\* @param clusterId The cluster ID.

\*/

def start(highestMetadataOffsetProvider: () => Long,

channelManager: BrokerToControllerChannelManager,

clusterId: String,

advertisedListeners: ListenerCollection,

supportedFeatures: util.Map\[String, VersionRange\]): Unit = {

// 创建 StartupEvent 事件对象，并将该事件投递到异步队列 KafkaEventQueue 中

eventQueue.append(new StartupEvent(highestMetadataOffsetProvider,

channelManager, clusterId, advertisedListeners, supportedFeatures))

}

该方法比较简单，关键就是创建 [StartupEvent](http://startupevent%20/) [](http://startupevent%20/)事件对象，并将该事件投递到异步队列 [KafkaEventQueue](http://kafkaeventqueue%20/) [](http://kafkaeventqueue%20/)中，对于事件队列，这篇 [【服务端 Broker 源码分析系列第三十八篇】图解 Kafka 源码之 KRaft 数据处理机制](https://articles.zsxq.com/id_xx081g35rpve.html) 详细分析了其运行原理，这里不再赘述，你只需要知道「**事件被消费处理**」时会执行 [StartupEvent#run()](http://startupevent/#run\(\)) 方法。

## **2.1.3 StartupEvent#run()**

// BrokerLifecycleManager.scala 子类

private class StartupEvent(highestMetadataOffsetProvider: () => Long,

channelManager: BrokerToControllerChannelManager,

clusterId: String,

advertisedListeners: ListenerCollection,

supportedFeatures: util.Map\[String, VersionRange\]) extends EventQueue.Event {

override def run(): Unit = {

\_highestMetadataOffsetProvider = highestMetadataOffsetProvider

\_channelManager \= channelManager

// 启动通道管理器

\_channelManager.start()

// 状态为启动中

\_state = BrokerState.STARTING

\_clusterId \= clusterId

\_advertisedListeners \= advertisedListeners.duplicate()

\_supportedFeatures = new util.HashMap\[String, VersionRange\](supportedFeatures)

// 向异步队列 KafkaEventQueue 中投递延时事件

eventQueue.scheduleDeferred("initialRegistrationTimeout",

new DeadlineFunction(time.nanoseconds() + initialTimeoutNs),

new RegistrationTimeoutEvent())

// 发送注册请求

sendBrokerRegistration()

info(s"Incarnation ${incarnationId} of broker ${nodeId} in cluster ${clusterId} " +

"is now STARTING.")

}

}

该方法比较简单，核心步骤：

1.  向异步队列 [KafkaEventQueue](http://kafkaeventqueue/) 中投递一个延时任务。
2.  发送注册请求。

## **2.1.4 StartupEvent#sendBrokerRegistration()**

接着来看发送注册请求的方法，源码如下：

private def sendBrokerRegistration(): Unit = {

val features \= new BrokerRegistrationRequestData.FeatureCollection()

\_supportedFeatures.asScala.foreach {

case (name, range) => features.add(new BrokerRegistrationRequestData.Feature().

setName(name).

setMinSupportedVersion(range.min()).

setMaxSupportedVersion(range.max()))

}

// 组装 BrokerRegistration 请求对象

val data \= new BrokerRegistrationRequestData().

setBrokerId(nodeId).

setClusterId(\_clusterId).

setFeatures(features).

setIncarnationId(incarnationId).

setListeners(\_advertisedListeners).

setRack(rack.orNull)

if (isTraceEnabled) {

trace(s"Sending broker registration ${data}")

}

// 调用 BrokerToControllerChannelManager.scala#sendRequest() 方法向集群 Leader 发起网络请求，并设置请求响应的处理器为 BrokerRegistrationResponseHandler。

\_channelManager.sendRequest(new BrokerRegistrationRequest.Builder(data),

new BrokerRegistrationResponseHandler())

}

核心步骤如下：

1.  组装 [BrokerRegistration](http://brokerregistration/) 请求对象。
2.  调用 [BrokerToControllerChannelManager.scala#sendRequest](http://brokertocontrollerchannelmanager.scala/#sendRequest)() 方法向集群 Leader 发起网络请求，并设置请求响应的处理器为 [BrokerRegistrationResponseHandler](http://brokerregistrationresponsehandler/)。

## **2.1.5 BrokerRegistrationResponseHandler#onComplete()**

接着当集群「**Leader 节点**」处理完 [BrokerRegistration](http://brokerregistration%20/) 请求，将响应送达发起请求的节点时，将触发请求发起节点的 [BrokerRegistrationResponseHandler#onComplete()](http://brokerregistrationresponsehandler/#onComplete\(\)) 方法对响应数据进行处理。

private class BrokerRegistrationResponseHandler extends ControllerRequestCompletionHandler {

override def onComplete(response: ClientResponse): Unit = {

if (response.authenticationException() != null) {

error(s"Unable to register broker ${nodeId} because of an authentication exception.",

response.authenticationException());

scheduleNextCommunicationAfterFailure()

} else if (response.versionMismatch() != null) {

error(s"Unable to register broker ${nodeId} because of an API version problem.",

response.versionMismatch());

scheduleNextCommunicationAfterFailure()

} else if (response.responseBody() == null) {

warn(s"Unable to register broker ${nodeId}.")

scheduleNextCommunicationAfterFailure()

} else if (!response.responseBody().isInstanceOf\[BrokerRegistrationResponse\]) {

error(s"Unable to register broker ${nodeId} because the controller returned an " +

"invalid response type.")

scheduleNextCommunicationAfterFailure()

} else {

val message \= response.responseBody().asInstanceOf\[BrokerRegistrationResponse\]

val errorCode \= Errors.forCode(message.data().errorCode())

if (errorCode == Errors.NONE) {

failedAttempts = 0

\_brokerEpoch = message.data().brokerEpoch()

registered = true

initialRegistrationSucceeded \= true

info(s"Successfully registered broker ${nodeId} with broker epoch ${\_brokerEpoch}")

// 投递延时事件 CommunicationEvent

scheduleNextCommunicationImmediately() // Immediately send a heartbeat

} else {

info(s"Unable to register broker ${nodeId} because the controller returned " +

s"error ${errorCode}")

scheduleNextCommunicationAfterFailure()

}

}

}

override def onTimeout(): Unit = {

info(s"Unable to register the broker because the RPC got timed out before it could be sent.")

scheduleNextCommunicationAfterFailure()

}

}

private def scheduleNextCommunicationAfterFailure(): Unit = {

// 重试超时时间

val delayMs \= resendExponentialBackoff.backoff(failedAttempts)

// 重试次数 + 1

failedAttempts = failedAttempts + 1

// 下一次定时任务

scheduleNextCommunication(NANOSECONDS.convert(delayMs, MILLISECONDS))

}

该方法比较简单，可以看到无论 Broker 节点是否成功注册到「**Leader 节点**」，核心就是定时发起下一次的请求。

## **2.2 处理节点注册请求**

当 Kafka 集群的「**Leader 节点**」收到 [BrokerRegistration](http://brokerregistration%20/) 请求，经过底层网络组件的协议解析后会将其分发到 [ControllerApis.scala#handleBrokerRegistration()](http://controllerapis.scala/#handleBrokerRegistration\(\)) 方法进行处理，源码如下：

![](https://article-images.zsxq.com/FlWB_Xqjnsb_rbIByfgjgWlZobp5)

def handleBrokerRegistration(request: RequestChannel.Request): Unit = {

val registrationRequest \= request.body\[BrokerRegistrationRequest\]

authHelper.authorizeClusterOperation(request, CLUSTER\_ACTION)

// 调用 QuorumController.java#registerBroker() 方法完成节点注册的业务处理。

controller.registerBroker(registrationRequest.data).handle\[Unit\] { (reply, e) =>

// 回调函数

def createResponseCallback(requestThrottleMs: Int,

reply: BrokerRegistrationReply,

e: Throwable): BrokerRegistrationResponse = {

if (e != null) {

new BrokerRegistrationResponse(new BrokerRegistrationResponseData().

setThrottleTimeMs(requestThrottleMs).

setErrorCode(Errors.forException(e).code))

} else {

new BrokerRegistrationResponse(new BrokerRegistrationResponseData().

setThrottleTimeMs(requestThrottleMs).

setErrorCode(NONE.code).

setBrokerEpoch(reply.epoch))

}

}

requestHelper.sendResponseMaybeThrottle(request,

requestThrottleMs => createResponseCallback(requestThrottleMs, reply, e))

}

}

该方法核心步骤就是调用 [QuorumController.java#registerBroker()](http://quorumcontroller.java/#registerBroker\(\)) 方法完成节点注册的业务处理。

## **2.2.1 QuorumController#registerBroker()**

可以看出该方法的处理完全按照 [【服务端 Broker 源码分析系列第三十八篇】图解 Kafka 源码之 KRaft 数据处理机制](https://articles.zsxq.com/id_xx081g35rpve.html) 中提到的异步处理框架，核心业务被封装在 [ControllerWriteEvent](http://controllerwriteevent%20/) [](http://controllerwriteevent%20/)事件中。

@Override

public CompletableFuture<BrokerRegistrationReply>

registerBroker(BrokerRegistrationRequestData request) {

return appendWriteEvent("registerBroker", () -> {

// 1、调用 ClusterControlManager.java#registerBroker() 方法将发起请求节点添加到本地列表中，并维护其状态。

ControllerResult<BrokerRegistrationReply> result = clusterControl.

registerBroker(request, writeOffset + 1, featureControl.

finalizedFeatures(Long.MAX\_VALUE));

// 2、调用 QuorumController.java#rescheduleMaybeFenceStaleBrokers() 方法检查本地列表中是否有超时的节点，超时节点将被置为 FENCED 状态，并进行分区副本 Leader 的重新选举

rescheduleMaybeFenceStaleBrokers();

return result;

});

}

核心步骤如下：

1.  调用 [ClusterControlManager.java#registerBroker](http://clustercontrolmanager.java/#registerBroker)() 方法将发起请求节点添加到本地列表中，并维护其状态。
2.  调用 [QuorumController.java#rescheduleMaybeFenceStaleBrokers](http://quorumcontroller.java/#rescheduleMaybeFenceStaleBrokers)() 方法检查本地列表中是否有超时的节点，超时节点将被置为 FENCED 状态，并进行分区副本 Leader 的重新选举。

## **2.2.2 ClusterControlManager#registerBroker()**

/\*\*

\* Process an incoming broker registration request.

\*/

public ControllerResult<BrokerRegistrationReply> registerBroker(

BrokerRegistrationRequestData request,

long brokerEpoch,

FeatureMapAndEpoch finalizedFeatures) {

if (heartbeatManager == null) {

throw new RuntimeException("ClusterControlManager is not active.");

}

// 获取注册的 Broker ID，并查找该 ID 是否已经在 brokerRegistrations 中存在

int brokerId \= request.brokerId();

// 1、首先根据请求携带的 brokerId 查找本地列表中的 BrokerRegistration 节点注册信息，如果这个节点已经注册了，则通过调用 BrokerHeartbeatManager.java#hasValidSession() 方法检查该节点是否超时未上报心跳

BrokerRegistration existing \= brokerRegistrations.get(brokerId);

// 如果这个节点已经注册了

if (existing != null) {

// 通过调用 BrokerHeartbeatManager.java#hasValidSession() 方法检查该节点是否超时未上报心跳

if (heartbeatManager.hasValidSession(brokerId)) {

// 如果未超时，则判断 incarnationId 是否相等，如果不相等则抛出异常

if (!existing.incarnationId().equals(request.incarnationId())) {

throw new DuplicateBrokerRegistrationException("Another broker is " +

"registered with that broker id.");

}

} else {

// 如果未超时，则判断 incarnationId 是否相等

if (!existing.incarnationId().equals(request.incarnationId())) {

// 如果不相等，则移除旧的会话

heartbeatManager.remove(brokerId);

existing = null;

}

}

}

// 2、生成节点注册元数据记录 RegisterBrokerRecord 对象，这个记录对象将在 ControllerWriteEvent 事件处理完成后在异步处理框架中写入到元数据 topic( \_\_cluster\_metadata)，后续会重放到内存当中

RegisterBrokerRecord record \= new RegisterBrokerRecord().setBrokerId(brokerId).

setIncarnationId(request.incarnationId()).

setBrokerEpoch(brokerEpoch).

setRack(request.rack());

// 依次添加 Broker 监听器的 endpoint 信息

for (BrokerRegistrationRequestData.Listener listener : request.listeners()) {

record.endPoints().add(new BrokerEndpoint().

setHost(listener.host()).

setName(listener.name()).

setPort(listener.port()).

setSecurityProtocol(listener.securityProtocol()));

}

// 依次添加 Broker 支持的 feature 信息

for (BrokerRegistrationRequestData.Feature feature : request.features()) {

Optional<VersionRange> finalized = finalizedFeatures.map().get(feature.name());

if (finalized.isPresent()) {

if (!finalized.get().contains(new VersionRange(feature.minSupportedVersion(),

feature.maxSupportedVersion()))) {

throw new UnsupportedVersionException("Unable to register because " +

"the broker has an unsupported version of " + feature.name());

}

}

record.features().add(new BrokerFeature().

setName(feature.name()).

setMinSupportedVersion(feature.minSupportedVersion()).

setMaxSupportedVersion(feature.maxSupportedVersion()));

}

// 3、调用 BrokerHeartbeatManager.java#touch() 方法维护节点状态 BrokerHeartbeatState

if (existing == null) {

heartbeatManager.touch(brokerId, true, -1);

} else {

heartbeatManager.touch(brokerId, existing.fenced(), -1);

}

// 将 Broker 注册记录添加到待持久化的列表中

List<ApiMessageAndVersion> records = new ArrayList<>();

records.add(new ApiMessageAndVersion(record,

REGISTER\_BROKER\_RECORD.highestSupportedVersion()));

// 返回注册结果和 BrokerEpoch 信息

return ControllerResult.of(records, new BrokerRegistrationReply(brokerEpoch));

}

核心步骤如下：

1.  首先根据请求携带的 brokerId 查找本地列表中的 [BrokerRegistration](http://brokerregistration%20/) 节点注册信息，如果这个节点已经注册了，则通过调用 [BrokerHeartbeatManager.java#hasValidSession](http://brokerheartbeatmanager.java/#hasValidSession)() 方法检查该节点是否超时未上报心跳。
2.  生成节点注册元数据记录 [RegisterBrokerRecord](http://registerbrokerrecord%20/) 对象，这个记录对象将在 [ControllerWriteEvent](http://controllerwriteevent%20/) 事件处理完成后在异步处理框架中写入到元数据 [topic( \_\_cluster\_metadata)](http://topic\(%20__cluster_metadata\)/)，后续会重放到内存当中 。
3.  调用 [BrokerHeartbeatManager.java#touch](http://brokerheartbeatmanager.java/#touch)() 方法维护节点状态 [BrokerHeartbeatState](http://brokerheartbeatstate/)。
4.  将 Broker 注册记录添加到待持久化的列表中。
5.  返回注册结果和 [BrokerEpoch](http://brokerepoch%20/) 信息。

简单来说就两点：

1.  生成 [RegisterBrokerRecord](http://registerbrokerrecord/)，存储该 Broker 节点的注册记录。
2.  生成 [BrokerHeartbeatState](http://brokerheartbeatstate%20/) 实例并添加到 [BrokerHeartbeatManager#brokers](http://brokerheartbeatmanager/#brokers%20) 中。

## **03 心跳请求**

在 Leader 节点中， [BrokerHeartbeatManager](http://brokerheartbeatmanager/#brokers) 类用来负责管理 Broker 节点信息，包含以下属性：

![](https://article-images.zsxq.com/FviBcGjA0crspp5m2d_FmGO_ERnd)

另外， [BrokerHeartbeatState](http://%20brokerheartbeatstate/) 存在以下状态：

![](https://article-images.zsxq.com/FiBU0bNEbza2Dfout7cMSewJS10J)

> 这里需要注意的是：BrokerControlState 中没有定义 state 变量，而是通过 BrokerControlState 实例是否存在于 ClusterControlManager#unfenced 列表中来判断该 BrokerControlState 实例当前处于 FENCED 状态或者 UNFENCED 状态。

## **3.1 发起心跳请求**

## **3.1.1 BrokerLifecycleManager**

**#scheduleNextCommunicationImmediately()**

在 **2.1.5** 节中，节点注册成功则触发 [BrokerLifecycleManager.scala#scheduleNextCommunicationImmediately()](http://brokerlifecyclemanager.scala/#scheduleNextCommunicationImmediately\(\)) 执行。

![](https://article-images.zsxq.com/FqFvFqL8zg-SbBEhKf35vk8ZjN0x)

private def scheduleNextCommunication(intervalNs: Long): Unit = {

trace(s"Scheduling next communication at ${MILLISECONDS.convert(intervalNs, NANOSECONDS)} " +

"ms from now.")

// 下一次 deadline 时间

val deadlineNs \= time.nanoseconds() + intervalNs

// 向异步队列 KafkaEventQueue 中投递延时事件 CommunicationEvent

eventQueue.scheduleDeferred("communication",

new DeadlineFunction(deadlineNs),

// 这个事件被消费时将触发 CommunicationEvent#run() 执行

new CommunicationEvent())

}

## **3.1.2 CommunicationEvent#run()**

// BrokerLifecycleManager.scala 子类

private class CommunicationEvent extends EventQueue.Event {

override def run(): Unit = {

if (registered) {

// 如果注册过则发送心跳

sendBrokerHeartbeat()

} else {

// 如果未注册则注册请求

sendBrokerRegistration()

}

}

}

该方法会对当前节点的注册状态进行判断，进而决定是否发送心跳，这也就是**其上游触发点不需要关系节点是否注册成功的原因**。

## **3.1.3 BrokerLifecycleManager#sendBrokerHeartbeat()**

可以看到发送 [BrokerHeartbeat](http://brokerheartbeat%20/) 请求的处理流程其实和 [BrokerRegistration](http://brokerregistration%20/) 请求流程类似。

private def sendBrokerHeartbeat(): Unit = {

val metadataOffset \= \_highestMetadataOffsetProvider()

// 组装 BrokerHeartbeat 请求对象

val data \= new BrokerHeartbeatRequestData().

setBrokerEpoch(\_brokerEpoch).

setBrokerId(nodeId).

setCurrentMetadataOffset(metadataOffset).

setWantFence(!readyToUnfence).

setWantShutDown(\_state == BrokerState.PENDING\_CONTROLLED\_SHUTDOWN)

if (isTraceEnabled) {

trace(s"Sending broker heartbeat ${data}")

}

// 调用 BrokerToControllerChannelManager.scala#sendRequest() 方法向集群 Leader 发起网络请求，并设置请求响应的处理器为 BrokerHeartbeatResponseHandler。

\_channelManager.sendRequest(new BrokerHeartbeatRequest.Builder(data),

new BrokerHeartbeatResponseHandler())

}

## **3.1.4 BrokerHeartbeatResponseHanlder#onComplete()**

[BrokerHeartbeatResponseHandler#onComplete()](http://brokerheartbeatresponsehandler/#onComplete\(\)) 方法负责处理 Leader 节点返回 [BrokerHeartbeat](http://brokerheartbeat/) 请求的心跳响应，它会添加一个延迟任务，定时发送下一次心跳请求，以维持会话正常。

private class BrokerHeartbeatResponseHandler extends ControllerRequestCompletionHandler {

override def onComplete(response: ClientResponse): Unit = {

if (response.authenticationException() != null) {

error(s"Unable to send broker heartbeat for ${nodeId} because of an " +

"authentication exception.", response.authenticationException());

scheduleNextCommunicationAfterFailure()

} else if (response.versionMismatch() != null) {

error(s"Unable to send broker heartbeat for ${nodeId} because of an API " +

"version problem.", response.versionMismatch());

scheduleNextCommunicationAfterFailure()

} else if (response.responseBody() == null) {

warn(s"Unable to send broker heartbeat for ${nodeId}. Retrying.")

scheduleNextCommunicationAfterFailure()

} else if (!response.responseBody().isInstanceOf\[BrokerHeartbeatResponse\]) {

error(s"Unable to send broker heartbeat for ${nodeId} because the controller " +

"returned an invalid response type.")

scheduleNextCommunicationAfterFailure()

} else {

val message \= response.responseBody().asInstanceOf\[BrokerHeartbeatResponse\]

val errorCode \= Errors.forCode(message.data().errorCode())

if (errorCode == Errors.NONE) {

failedAttempts = 0

\_state match {

case BrokerState.STARTING =>

if (message.data().isCaughtUp()) {

info(s"The broker has caught up. Transitioning from STARTING to RECOVERY.")

\_state = BrokerState.RECOVERY

initialCatchUpFuture.complete(null)

} else {

debug(s"The broker is STARTING. Still waiting to catch up with cluster metadata.")

}

// Schedule the heartbeat after only 10 ms so that in the case where

// there is no recovery work to be done, we start up a bit quicker.

scheduleNextCommunication(NANOSECONDS.convert(10, MILLISECONDS))

case BrokerState.RECOVERY =>

if (!message.data().isFenced()) {

info(s"The broker has been unfenced. Transitioning from RECOVERY to RUNNING.")

\_state = BrokerState.RUNNING

} else {

info(s"The broker is in RECOVERY.")

}

scheduleNextCommunicationAfterSuccess()

case BrokerState.RUNNING =>

debug(s"The broker is RUNNING. Processing heartbeat response.")

scheduleNextCommunicationAfterSuccess()

case BrokerState.PENDING\_CONTROLLED\_SHUTDOWN =>

if (!message.data().shouldShutDown()) {

info(s"The broker is in PENDING\_CONTROLLED\_SHUTDOWN state, still waiting " +

"for the active controller.")

if (!gotControlledShutdownResponse) {

// If this is the first pending controlled shutdown response we got,

// schedule our next heartbeat a little bit sooner than we usually would.

// In the case where controlled shutdown completes quickly, this will

// speed things up a little bit.

scheduleNextCommunication(NANOSECONDS.convert(50, MILLISECONDS))

} else {

scheduleNextCommunicationAfterSuccess()

}

} else {

info(s"The controlled has asked us to exit controlled shutdown.")

beginShutdown()

}

gotControlledShutdownResponse = true

case BrokerState.SHUTTING\_DOWN =>

info(s"The broker is SHUTTING\_DOWN. Ignoring heartbeat response.")

case \_ \=\>

error(s"Unexpected broker state ${\_state}")

scheduleNextCommunicationAfterSuccess()

}

} else {

warn(s"Broker ${nodeId} sent a heartbeat request but received error ${errorCode}.")

scheduleNextCommunicationAfterFailure()

}

}

}

override def onTimeout(): Unit = {

info("Unable to send a heartbeat because the RPC got timed out before it could be sent.")

scheduleNextCommunicationAfterFailure()

}

}

## **3.2 处理心跳请求**

![](https://article-images.zsxq.com/Fogq6mV3_vT6Am4QMYPdIdDYVijA)

首先 [ControllerApis.scala#handleBrokerHeartBeatRequest()](http://controllerapis.scala/#handleBrokerRegistration\(\)) 方法负责 [BrokerHeartbeat](http://brokerheartbeat%20/) 请求的处理，源码如下:

def handleBrokerHeartBeatRequest(request: RequestChannel.Request): Unit = {

val heartbeatRequest \= request.body\[BrokerHeartbeatRequest\]

authHelper.authorizeClusterOperation(request, CLUSTER\_ACTION)

// 调用 QuorumController.java#processBrokerHeartbeat() 方法完成节点心跳的业务处理。

controller.processBrokerHeartbeat(heartbeatRequest.data).handle\[Unit\] { (reply, e) =>

// 回调函数

def createResponseCallback(requestThrottleMs: Int,

reply: BrokerHeartbeatReply,

e: Throwable): BrokerHeartbeatResponse = {

if (e != null) {

new BrokerHeartbeatResponse(new BrokerHeartbeatResponseData().

setThrottleTimeMs(requestThrottleMs).

setErrorCode(Errors.forException(e).code))

} else {

new BrokerHeartbeatResponse(new BrokerHeartbeatResponseData().

setThrottleTimeMs(requestThrottleMs).

setErrorCode(NONE.code).

setIsCaughtUp(reply.isCaughtUp).

setIsFenced(reply.isFenced).

setShouldShutDown(reply.shouldShutDown))

}

}

requestHelper.sendResponseMaybeThrottle(request,

requestThrottleMs => createResponseCallback(requestThrottleMs, reply, e))

}

}

可以看到和前面处理节点注册的流程高度一致，此处将触发 [QuorumController.java#processBrokerHeartbeat()](http://%20quorumcontroller.java/#processBrokerHeartbeat\(\)) 方法执行。

## **3.2.1 QuorumController#processBrokerHeartbeat()**

@Override

public CompletableFuture<BrokerHeartbeatReply>

processBrokerHeartbeat(BrokerHeartbeatRequestData request) {

return appendWriteEvent("processBrokerHeartbeat",

new ControllerWriteOperation<BrokerHeartbeatReply>() {

private final int brokerId \= request.brokerId();

private boolean inControlledShutdown \= false;

@Override

public ControllerResult<BrokerHeartbeatReply> generateRecordsAndResult() {

// 调用 ReplicationControlManager#processBrokerHeartbeat() 方法依据请求数据更新节点信息

ControllerResult<BrokerHeartbeatReply> result = replicationControl.

processBrokerHeartbeat(request, lastCommittedOffset);

inControlledShutdown = result.response().inControlledShutdown();

// 调用 QuorumController.java#rescheduleMaybeFenceStaleBrokers() 方法检查本地列表中是否有超时的节点，超时节点将被置为 FENCED 状态，并需要进行分区副本 Leader 的重新选举

rescheduleMaybeFenceStaleBrokers();

return result;

}

@Override

public void processBatchEndOffset(long offset) {

if (inControlledShutdown) {

clusterControl.heartbeatManager().

updateControlledShutdownOffset(brokerId, offset);

}

}

});

}

核心步骤如下：

1.  调用 [ReplicationControlManager#processBrokerHeartbeat()](http://replicationcontrolmanager/#processBrokerHeartbeat\(\)) 方法依据请求数据更新节点信息。
2.  调用 [QuorumController.java#rescheduleMaybeFenceStaleBrokers](http://quorumcontroller.java/#rescheduleMaybeFenceStaleBrokers)() 方法检查本地列表中是否有超时的节点，超时节点将被置为 [FENCED](http://fenced/) 状态，并需要进行分区副本 Leader 的重新选举。

## **3.2.2 ReplicationControlManager#processBrokerHeartbeat()**

ControllerResult<BrokerHeartbeatReply> processBrokerHeartbeat(

BrokerHeartbeatRequestData request, long lastCommittedOffset) {

// 获取 BrokerId 和 Epoch 信息

int brokerId \= request.brokerId();

long brokerEpoch \= request.brokerEpoch();

// 校验 BrokerId 是否注册过并且注册 Epoch 是否等于 BrokerEpoch，否则抛异常

clusterControl.checkBrokerEpoch(brokerId, brokerEpoch);

BrokerHeartbeatManager heartbeatManager \= clusterControl.heartbeatManager();

// 调用 BrokerHeartbeatManager.java#calculateNextBrokerState() 方法计算报告心跳的节点接下来的状态

BrokerControlStates states \= heartbeatManager.calculateNextBrokerState(brokerId,

request, lastCommittedOffset, () -> brokersToIsrs.hasLeaderships(brokerId));

List<ApiMessageAndVersion> records = new ArrayList<>();

// 如果节点当前状态和接下来的状态不一致，则说明节点即将发生状态变化，需要进行对应的处理，生成元数据变动记录，后续将其封装到事件处理结果对象

if (states.current() != states.next()) {

switch (states.next()) {

case FENCED:

handleBrokerFenced(brokerId, records);

break;

case UNFENCED:

handleBrokerUnfenced(brokerId, brokerEpoch, records);

break;

case CONTROLLED\_SHUTDOWN:

generateLeaderAndIsrUpdates("enterControlledShutdown\[" + brokerId + "\]",

brokerId, NO\_LEADER, records, brokersToIsrs.partitionsWithBrokerInIsr(brokerId));

break;

case SHUTDOWN\_NOW:

handleBrokerFenced(brokerId, records);

break;

}

}

// 更新维护节点状态到本地列表

heartbeatManager.touch(brokerId,

states.next().fenced(),

request.currentMetadataOffset());

boolean isCaughtUp \= request.currentMetadataOffset() >= lastCommittedOffset;

BrokerHeartbeatReply reply \= new BrokerHeartbeatReply(isCaughtUp,

states.next().fenced(),

states.next().inControlledShutdown(),

states.next().shouldShutDown());

return ControllerResult.of(records, reply);

}

核心步骤如下：

1.  获取 BrokerId 和 Epoch 信息并校验 BrokerId 是否注册过并且注册 Epoch 是否等于 BrokerEpoch，否则抛异常。
2.  调用 [BrokerHeartbeatManager.java#calculateNextBrokerState](http://brokerheartbeatmanager.java/#calculateNextBrokerState)() 方法计算报告心跳的节点接下来的状态。
3.  如果节点当前状态和接下来的状态不一致，则说明节点即将发生状态变化，需要进行对应的处理，生成元数据变动记录，后续将其封装到事件处理结果对象。
4.  更新维护节点状态到本地列表。

## **3.2.3 ReplicationControlManager#processBrokerHeartbeat()**

回到 **3.2.1** 步骤第2步，[QuorumController.java#rescheduleMaybeFenceStaleBrokers()](http://quorumcontroller.java/#rescheduleMaybeFenceStaleBrokers\(\)) 方法的核心步骤就是延时执行 [ReplicationControl.java#maybeFenceOneStaleBroker()](http://replicationcontrol.java/#maybeFenceOneStaleBroker\(\)%20) 方法检测心跳超时节点，并递归调用自身以便处理多个节点下线的情况。

private void rescheduleMaybeFenceStaleBrokers() {

long nextCheckTimeNs \= clusterControl.heartbeatManager().nextCheckTimeNs();

if (nextCheckTimeNs == Long.MAX\_VALUE) {

cancelMaybeFenceReplicas();

return;

}

scheduleDeferredWriteEvent(MAYBE\_FENCE\_REPLICAS, nextCheckTimeNs, () -> {

// 延时执行 ReplicationControl.java#maybeFenceOneStaleBroker() 方法检测心跳超时节点

ControllerResult<Void> result = replicationControl.maybeFenceOneStaleBroker();

// This following call ensures that if there are multiple brokers that

// are currently stale, then fencing for them is scheduled immediately

// 递归调用自身以便处理多个节点下线的情况

rescheduleMaybeFenceStaleBrokers();

return result;

});

}

## **3.2.4 ReplicationControlManager#maybeFenceOneStaleBroker()**

ControllerResult<Void> maybeFenceOneStaleBroker() {

List<ApiMessageAndVersion> records = new ArrayList<>();

BrokerHeartbeatManager heartbeatManager \= clusterControl.heartbeatManager();

// 首先调用 BrokerHeartbeatManager.java#findOneStaleBroker() 检测是否存在心跳超时的节点

heartbeatManager.findOneStaleBroker().ifPresent(brokerId -> {

// Even though multiple brokers can go stale at a time, we will process

// fencing one at a time so that the effect of fencing each broker is visible

// to the system prior to processing the next one

log.info("Fencing broker {} because its session has timed out.", brokerId);

// 生成对应的 Record 数据，这些数据存储了对应的故障转移操作，比如：变更分区 ISR，选举新的 Leader 副本等。如果存在则触发失败转移机制，取列表中超时时间最长的一个节点出来调用 ReplicationControlManager.java#handleBrokerFenced() 进行下线处理

handleBrokerFenced(brokerId, records);

// 将该 Broker 节点从 BrokerHeartbeatManager#unfenced 中进行移除

heartbeatManager.fence(brokerId);

});

return ControllerResult.of(records, null);

}

核心步骤如下：

1.  首先调用 [BrokerHeartbeatManager.java#findOneStaleBroker](http://brokerheartbeatmanager.java/#findOneStaleBroker)() 检测是否存在心跳超时的节点。
2.  生成对应的 Record 数据，这些数据存储了对应的故障转移操作，比如：变更分区 ISR，选举新的 Leader 副本等。如果存在则触发失败转移机制，取列表中超时时间最长的一个节点出来调用 [ReplicationControlManager.java#handleBrokerFenced](http://replicationcontrolmanager.java/#handleBrokerFenced)() 进行下线处理。
3.  将该 Broker 节点从 [BrokerHeartbeatManager#unfenced](http://brokerheartbeatmanager/#unfenced) 中进行移除。
4.  返回上面生成的的 Records，最后这些数据存储到「**\_\_cluster\_metadata**」中。当超过半数 Controller 节点都同步了这些数据后，Broker 节点将执行这些数据存储的故障转移操作。

## **3.2.5 BrokerHeartbeatManager#findOneStaleBroker()**

Optional<Integer> findOneStaleBroker() {

BrokerHeartbeatStateIterator iterator \= unfenced.iterator();

if (iterator.hasNext()) {

BrokerHeartbeatState broker \= iterator.next();

// The unfenced list is sorted on last contact time from each

// broker. If the first broker is not stale, then none is.

if (!hasValidSession(broker)) {

return Optional.of(broker.id);

}

}

return Optional.empty();

}

private boolean hasValidSession(BrokerHeartbeatState broker) {

if (broker.fenced()) {

return false;

} else {

return broker.lastContactNs + sessionTimeoutNs >= time.nanoseconds();

}

}

 该方法比较简单，主要就是从头遍历 unfenced 节点列表，通过 [BrokerHeartbeatManager.java#hasValidSession](http://brokerheartbeatmanager.java/#hasValidSession)() 方法判断节点是否超时。

##   
**3.2.6 ReplicationControlManager#handleBrokerFenced()**

该方法用来进行失败转移处理的，核心步骤就是调用 [ReplicationControlManager.java#generateLeaderAndIsrUpdates](http://replicationcontrolmanager.java/#generateLeaderAndIsrUpdates)() 方法进行处理。

void handleBrokerFenced(int brokerId, List<ApiMessageAndVersion> records) {

BrokerRegistration brokerRegistration \= clusterControl.brokerRegistrations().get(brokerId);

if (brokerRegistration == null) {

throw new RuntimeException("Can't find broker registration for broker " + brokerId);

}

generateLeaderAndIsrUpdates("handleBrokerFenced", brokerId, NO\_LEADER, records,

brokersToIsrs.partitionsWithBrokerInIsr(brokerId));

records.add(new ApiMessageAndVersion(new FenceBrokerRecord().

setId(brokerId).setEpoch(brokerRegistration.epoch()),

FENCE\_BROKER\_RECORD.highestSupportedVersion()));

}

## **3.2.7 ReplicationControlManager#generateLeaderAndIsrUpdates()**

void generateLeaderAndIsrUpdates(String context,

int brokerToRemove,

int brokerToAdd,

List<ApiMessageAndVersion> records,

Iterator<TopicIdPartition> iterator) {

// 原大小

int oldSize \= records.size();

// If the caller passed a valid broker ID for brokerToAdd, rather than passing

// NO\_LEADER, that node will be considered an acceptable leader even if it is

// currently fenced. This is useful when handling unfencing. The reason is that

// while we're generating the records to handle unfencing, the ClusterControlManager

// still shows the node as fenced.

//

// Similarly, if the caller passed a valid broker ID for brokerToRemove, rather

// than passing NO\_LEADER, that node will never be considered an acceptable leader.

// This is useful when handling a newly fenced node. We also exclude brokerToRemove

// from the target ISR, but we need to exclude it here too, to handle the case

// where there is an unclean leader election which chooses a leader from outside

// the ISR.

Function<Integer, Boolean> isAcceptableLeader =

r -> (r != brokerToRemove) && (r == brokerToAdd || clusterControl.unfenced(r));

while (iterator.hasNext()) {

TopicIdPartition topicIdPart \= iterator.next();

// 获取 topic

TopicControlInfo topic \= topics.get(topicIdPart.topicId());

if (topic == null) {

throw new RuntimeException("Topic ID " + topicIdPart.topicId() +

" existed in isrMembers, but not in the topics map.");

}

// 获取 partition

PartitionRegistration partition \= topic.parts.get(topicIdPart.partitionId());

if (partition == null) {

throw new RuntimeException("Partition " + topicIdPart +

" existed in isrMembers, but not in the partitions map.");

}

// 构建元数据变更记录

PartitionChangeBuilder builder \= new PartitionChangeBuilder(partition,

topicIdPart.topicId(),

topicIdPart.partitionId(),

isAcceptableLeader,

() -> configurationControl.uncleanLeaderElectionEnabledForTopic(topic.name));

// Note: if brokerToRemove was passed as NO\_LEADER, this is a no-op (the new

// target ISR will be the same as the old one).

builder.setTargetIsr(Replicas.toList(

Replicas.copyWithout(partition.isr, brokerToRemove)));

builder.build().ifPresent(records::add);

}

if (records.size() != oldSize) {

if (log.isDebugEnabled()) {

StringBuilder bld \= new StringBuilder();

String prefix \= "";

for (ListIterator<ApiMessageAndVersion> iter = records.listIterator(oldSize);

iter.hasNext(); ) {

ApiMessageAndVersion apiMessageAndVersion \= iter.next();

PartitionChangeRecord record \= (PartitionChangeRecord) apiMessageAndVersion.message();

bld.append(prefix).append(topics.get(record.topicId()).name).append("-").

append(record.partitionId());

prefix = ", ";

}

log.debug("{}: changing partition(s): {}", context, bld.toString());

} else if (log.isInfoEnabled()) {

log.info("{}: changing {} partition(s)", context, records.size() - oldSize);

}

}

}

该方法主要负责为副本 Leader 分布在失败节点上的分区重新选举 Leader 副本，核心步骤为 [PartitionChangeBuilder.java#build()](http://partitionchangebuilder.java/#build\(\)) 方法调用。

## **3.2.8 PartitionChangeBuilder#build()**

public Optional<ApiMessageAndVersion> build() {

PartitionChangeRecord record \= new PartitionChangeRecord().

setTopicId(topicId).

setPartitionId(partitionId);

completeReassignmentIfNeeded();

// 调用 PartitionChangeBuilder.java#shouldTryElection() 方法判断当前分区副本 Leader 是否在 ISR 列表中，不在则需要重新选举副本 Leader

if (shouldTryElection()) {

// 调用 PartitionChangeBuilder.java#tryElection() 方法执行重新选举分区副本 Leader 的操作

tryElection(record);

}

triggerLeaderEpochBumpIfNeeded(record);

if (!targetIsr.isEmpty() && !targetIsr.equals(Replicas.toList(partition.isr))) {

record.setIsr(targetIsr);

}

if (!targetReplicas.isEmpty() && !targetReplicas.equals(Replicas.toList(partition.replicas))) {

record.setReplicas(targetReplicas);

}

if (!targetRemoving.equals(Replicas.toList(partition.removingReplicas))) {

record.setRemovingReplicas(targetRemoving);

}

if (!targetAdding.equals(Replicas.toList(partition.addingReplicas))) {

record.setAddingReplicas(targetAdding);

}

if (changeRecordIsNoOp(record)) {

return Optional.empty();

} else {

// 生成 PARTITION\_CHANGE\_RECORD 元数据变动记录，记录分区信息的变化。该元数据变动在集群节点上重放时会检测 Leader 变动，分区 Leader 每变动一次版本号自增 1。分区版本号主要和 Kafka 的异常恢复机制有关

return Optional.of(new ApiMessageAndVersion(record,

PARTITION\_CHANGE\_RECORD.highestSupportedVersion()));

}

}

核心步骤如下：

1.  调用 [PartitionChangeBuilder.java#shouldTryElection](http://partitionchangebuilder.java/#shouldTryElection)() 方法判断当前分区副本 Leader 是否在 ISR 列表中，不在则需要重新选举副本 Leader。
2.  调用 [PartitionChangeBuilder.java#tryElection](http://partitionchangebuilder.java/#tryElection)() 方法执行重新选举分区副本 Leader 的操作。
3.  生成 PARTITION\_CHANGE\_RECORD 元数据变动记录，记录分区信息的变化。该元数据变动在集群节点上重放时会检测 Leader 变动，分区 Leader 每变动一次版本号自增 1。分区版本号主要和 Kafka 的异常恢复机制有关。

## **3.2.9 PartitionChangeBuilder#tryElection()**

该方法用来实际完成分区副本 Leader 的选举，源码如下：

private void tryElection(PartitionChangeRecord record) {

// 新建 BestLeader 对象，在它的构造方法中完成 Leader 选举

BestLeader bestLeader \= new BestLeader();

// 判断新选举的 Leader 是否和分区的当前 Leader 相同，不相同才需要将其更新到元数据变动记录中传播出去。

if (bestLeader.node != partition.leader) {

record.setLeader(bestLeader.node);

if (bestLeader.unclean) {

// If the election was unclean, we have to forcibly set the ISR to just the

// new leader. This can result in data loss!

record.setIsr(Collections.singletonList(bestLeader.node));

}

}

}

核心步骤如下：

1.  新建 BestLeader 对象，在它的构造方法中完成 Leader 选举。
2.  判断新选举的 Leader 是否和分区的当前 Leader 相同，不相同才需要将其更新到元数据变动记录中传播出去。

## **3.2.10 BestLeader 类**

// PartitionChangeBuilder 的子类

class BestLeader {

final int node;

final boolean unclean;

BestLeader() {

// 从 ISR 列表中选取第一个合适的节点（UNFENCED 状态），默认模式。

for (int replica : targetReplicas) {

if (targetIsr.contains(replica) && isAcceptableLeader.apply(replica)) {

this.node = replica;

this.unclean = false;

return;

}

}

// 从所有节点中选取第一个合适的节点（UNFENCED 状态），以上模式选举失败才进入，默认关闭

if (uncleanElectionOk.get()) {

for (int replica : targetReplicas) {

if (isAcceptableLeader.apply(replica)) {

this.node = replica;

this.unclean = true;

return;

}

}

}

this.node = NO\_LEADER;

this.unclean = false;

}

}

BestLeader 选举比较简单，可以看到其实就是遍历本地分区的副本列表选取合适的副本即可，不过这里分为以下两种模式：

1.  从 ISR 列表中选取第一个合适的节点（UNFENCED 状态），默认模式。
2.  从所有节点中选取第一个合适的节点（UNFENCED 状态），以上模式选举失败才进入，默认关闭。

至此，「**分区副本失败故障转移**」的源码流程就剖析完了，最后我们来总结图解下。

## **04 节点监控与故障转移总结**

可以看到本篇是从两个大的方向来剖析 「**KRaft 模式下节点监控与故障转移流程**」的，下面通过一张时序图来梳理下：

  
![](https://article-images.zsxq.com/FtvPSdS52QAwfv919z4H163OcZv3)

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头抛出「**Kafka KRaft 模块**」的重要功能：对 Broker 节点进行监控，在 Broker 节点故障下线后，执行故障转移操作。

2、接着带大家从两个维度「**节点注册**」、「**心跳请求**」深度剖析了是如何对 Broker 节点进行监控的以及当 Broker 节点故障下线后是如何进行故障转移的源码流程。

3、最后通过一张时序流程图带大家梳理了整个处理流程，希望让你更好的理解。

下篇我们来深度剖析「**Kraft 数据清理过程**」，大家期待，我们下期见。