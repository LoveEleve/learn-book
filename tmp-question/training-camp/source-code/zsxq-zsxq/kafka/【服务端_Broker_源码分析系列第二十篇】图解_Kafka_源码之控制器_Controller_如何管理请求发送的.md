大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端之控制器 Controller 选举机制实现原理**」，通过两大步骤「**触发选举**」、 「**开始选举**」，三大场景「**集群刚启动时**」、 「**Broker 检测 /Controller 节点消失时**」、「**Broker 检测 /Controller 节点发生变更时**」来剖析「**Controller**」选举全过程，从今天开始，我们来深度剖析 Kafka「**Controller**」的底层源码实现，这是 Controller 系列第五篇，我们接着来深度聊聊「**Kafka 服务端控制器 Controller 如何管理请求发送的**」。

  
![](https://article-images.zsxq.com/FndYh1i_epuKW6Y86iS52k1Mx0Kd)

##   
**01 总体概述**

在上篇中，我们深度剖析了「**Controller**」的选举全过程以及选举时机。当「**Controller**」所在的 Broker 被选举出来后，其他的 Broker 就会继续监听「**/Controller 节点**」, 那么 Broker 之间的通讯是如何处理的，是 「**Controller**」直接向其他 Broker 发送请求吗？它们之前都会发送哪些请求呢？

带着这些问题，我们开启今天的话题。

在一个 Kafka 集群中，某段时间内只能有一台 Broker 被选举为「**Controller**」。

当某台 Broker 成为「**Controller**」后，此时就会行驶它作为控制器的重要权利了，主要职能包括：

1.  **集群成员管理**：对集群成员数量管理以及成员信息管理。
2.  **Broker 状态管理**：Controller 会跟踪集群中所有 Broker 的在线状态，并在 Broker 宕机或者恢复时更新集群的状态。
3.  **分区状态管理**：当新的 Topic 被创建，或者已有的 Topic 被删除时，Controller 会负责管理这些变化，并更新集群的状态。
4.  **分区 Leader 副本选举**：当一台 Broker 节点宕机时，并且宕机的机器上包含分区 Leader 副本时，Controller 会负责对其上的所有Partition 进行新的领导者选举。
5.  **副本状态管理**：Controller 负责管理 Partition 的 ISR 列表，当 Follower 副本无法及时跟随 Leader 副本时，Controller 会将其从 ISR 列表中移除。
6.  **分区重平衡**：当添加或删除 Broker 节点时，Controller 会负责对 Partition 的分布进行重平衡，以确保数据的均匀分布。
7.  **存储集群元数据**：Controller 保存了集群中最全的元数据信息，并通过发送请求同步到其他 Broker。

「**Controller**」会行驶这么多重要的权利，所以第一个问题的答案肯定也是「**Controller**」会向集群中的其他 Broker （当然也包括自己所在的 Broker）发送网络请求的。而发送请求的目的**就是让其他 Broker 执行某些相应的操作**。

## **02 Controller 发送请求类型**

上面确定了「**Controller**」会向集群中的其他 Broker 发送网络请求，那么它会发送哪些请求呢？

记得在之前文章中讲过，服务端的网络请求分两种：「**控制类请求**」、「**数据类请求**」，这里首先你要明白的是「**Controller**」发送的这些请求都是「**控制类请求**」，大致有以下三类：

1.  **LeaderAndIsrRequest**：该请求最重要的功能是告诉「**Broker 相关主题各个分区的 Leader 副本位于哪台 Broker 上**」、「**ISR 副本集合中副本都分布在哪些 Broker 上**」。这里你试着想一下，如果请求中的 Leader 副本都发生变更了，那么之前客户端发往旧的 Leader 上的 **PRODUCE** 请求是不是都全部失效了呢？**所以个人任务它是非常重要的控制类请求**。
2.  **StopReplicaRequest**：该请求最重要的功能是告诉「**指定 Broker 停止它上面的副本对象**」、「**删除副本底层的日志数据**」。它主要发生在「**分区副本迁移场景**」、「**删除主题场景**」，在这两个场景下，都要涉及停止 Broker 上的副本操作。
3.  **UpdateMetadataRequest**：该请求最重要的功能是「**更新 Broker 上的元数据缓存**」。集群上的所有元数据变更，都首先发生在 Controller 端，然后再经由这个请求发送给集群上的所有 Broker。所以我们需要关注某些监控指标，防止 Controller 端请求积压造成元数据更新滞后问题。

![](https://article-images.zsxq.com/Fju_4t80fSVkBRd4G_FVVuajdfsi)

通过对源码的了解，上面定义的这三类请求都封装在 Clients 包中，其控制器抽象类为 AbstractControlRequest。

「**AbstractControlRequest.java**」类源码为主，其在 Kafka 源码包的 clients 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/requests/AbstractControlRequest.java](https://github.com/apache/kafka/blob/2.7.0/clients/src/main/java/org/apache/kafka/common/requests/AbstractControlRequest.java)

![](https://article-images.zsxq.com/FkjgOThrwF5mCsAKj_DwCGaXhZo5)

如上图可以看出，抽象类包含 3 个字段：

1.  **controllerId**：该字段表示 Controller 所在的 Broker ID。
2.  **controllerEpoch**：该字段表示 Controller Epoch 版本信息。
3.  **brokerEpoch**：该字段表示 Broker Epoch 版本信息。

后面两个字段用来保证集群一致性的，主要用来防止 「**Zombie Controller**」、「**Zombie Broker**」。

而在抽象类 AbstractRequest 中，会处理各类请求的入口，上面剖析的三类请求也在其中，源码如下：

/\*\*

\* Factory method for getting a request object based on ApiKey ID and a version

\*/

public static AbstractRequest parseRequest(ApiKeys apiKey, short apiVersion, Struct struct) {

switch (apiKey) {

....

case STOP\_REPLICA:

return new StopReplicaRequest(struct, apiVersion);

case UPDATE\_METADATA:

return new UpdateMetadataRequest(struct, apiVersion);

case LEADER\_AND\_ISR:

return new LeaderAndIsrRequest(struct, apiVersion);

....

default:

throw new AssertionError(String.format("ApiKey %s is not currently handled in \`parseRequest\`, the " + "code should be updated to do so.", apiKey));

}

}

相关的三类请求，源码比较简单，这里就不剖析了，后面会单独一篇文章来剖析 Kafka 请求和响应数据结构，如下：

![](https://article-images.zsxq.com/Fo8s-JqeV4nKn9zC2X8D_vJ3qnfH)

![](https://article-images.zsxq.com/Fofhk3OD3ozKIfv3OJsVq1onJeFi)

![](https://article-images.zsxq.com/FmyXcG7cKwXxPtfxWMlNn8karI_Z)

## **03 Controller 发送请求线程**

接下来，我们来剖析下「**Controller**」怎么发送请求的。

在 Kafka 源码中，非常喜欢使用 「**生产者**」、「**消费者**」模式，该模式的好处就是解耦生产者、消费者逻辑的。

比如前面所学的 「**SocketServer**」组件，其就是在内部定义了一个「**线程共享**」的「**请求队列**」：其中 Processor 线程可以理解为「**生产者**」，而 KafkaRequestHandler 线程池可以理解为 「**消费者**」。

那么这里 「**Controller**」同样使用了该模式：这里内部依然定义了「**线程安全**」的「**阻塞队列**」，其中控制器事件处理线程「**ControllerEventThread**」负责向该队列写入待发送的请求，由另外一个线程来负责执行真正的请求发送，该线程为「**RequestSendThread**」，如下图所示：

  
![](https://article-images.zsxq.com/FrL4iPIKYIUOE2Q7Uhh1vrrUJ_1e)

从上图得出，Controller 会为集群中的每个 Broker 都创建一个对应的 「**RequestSendThread**」线程。它会持续地从阻塞队列中获取待发送的请求。

那么，Controller 往阻塞队列上写入什么数据呢？它是由源码中的 QueueItem 类定义的，其源码在

「**controllerChannelManager****.scala**」类源码为主，其在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerChannelManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerChannelManager.scala)

// ControllerChannelManager 类

case class QueueItem(apiKey: ApiKeys, request: AbstractControlRequest.Builder\[\_ <: AbstractControlRequest\],callback: AbstractResponse => Unit, enqueueTimeMs: Long)

每个 QueueItem 的核心字段都是 AbstractControlRequest.Builder 对象，这里需要注意的是这里的“<:”符号，它在 Scala 中表示「**上边界**」，也就是说字段 request 必须是 AbstractControlRequest 的子类，即上面我们剖析了三类请求「**LeaderAndIsrRequest**」、「**StopReplicaRequest**」、「**UpdateMetadataRequest**」。

接下来我们来看下 「**RequestSendThread**」线程类定义，源码如下：

//

class RequestSendThread(val controllerId: Int, // Controller 所在 Broker Id

val controllerContext: ControllerContext, // Controller 元数据信息

val queue: BlockingQueue\[QueueItem\], // 请求阻塞队列

val networkClient: NetworkClient, // 用于执行发送的网络请求类

val brokerNode: Node, // 目标 Broker 节点

val config: KafkaConfig ,// Kafka配置信息

val time: Time,

val requestRateAndQueueTimeMetrics: Timer,

val stateChangeLogger: StateChangeLogger,

name: String)

extends ShutdownableThread(name = name) {

....

该类最重要的方法是 doWork()，即执行线程逻辑的方法，源码如下：

override def doWork(): Unit = {

def backoff(): Unit = pause(100, TimeUnit.MILLISECONDS)

// 以阻塞方式从阻塞队列中获取请求

val QueueItem(apiKey, requestBuilder, callback, enqueueTimeMs) = queue.take()

// 更新监控指标

requestRateAndQueueTimeMetrics.update(time.milliseconds() - enqueueTimeMs, TimeUnit.MILLISECONDS)

var clientResponse: ClientResponse = null

try {

var isSendSuccessful \= false

// 正在运行中 && 未发送成功 则一直执行

while (isRunning && !isSendSuccessful) {

try {

// 目标 Broker 连接是否准备就绪

if (!brokerReady()) {

// 标记发送失败

isSendSuccessful = false

// 等待重试

backoff()

}

else {

val clientRequest \= networkClient.newClientRequest(brokerNode.idString, requestBuilder, time.milliseconds(), true)

// 发送请求，等待接收 Response

clientResponse = NetworkClientUtils.sendAndReceive(networkClient, clientRequest, time)

// 标记发送成功

isSendSuccessful = true

}

} catch {

case e: Throwable => // if the send was not successful, reconnect to broker and resend the message

warn(s"Controller $controllerId epoch ${controllerContext.epoch} fails to send request $requestBuilder " + s"to broker $brokerNode. Reconnecting to broker.", e)

// 如果出现异常，关闭与对应 Broker 的连接

networkClient.close(brokerNode.idString)

// 设置发送成功标识为 false

isSendSuccessful = false

// 等待重试

backoff()

}

}

// 如果接收到了 Response

if (clientResponse != null) {

val requestHeader \= clientResponse.requestHeader

val api \= requestHeader.apiKey

// 请求类型必须是 LeaderAndIsrRequest、StopReplicaRequest或UpdateMetadataRequest 中的一种，否则抛异常

if (api != ApiKeys.LEADER\_AND\_ISR && api != ApiKeys.STOP\_REPLICA && api != ApiKeys.UPDATE\_METADATA)

throw new KafkaException(s"Unexpected apiKey received: $apiKey")

val response \= clientResponse.responseBody

stateChangeLogger.withControllerEpoch(controllerContext.epoch).trace(s"Received response " + s"${response.toString(requestHeader.apiVersion)} for request $api with correlation id " + s"${requestHeader.correlationId} sent to broker $brokerNode")

if (callback != null) {

// 处理回调

callback(response)

}

}

} catch {

case e: Throwable =>

error(s"Controller $controllerId fails to send a request to broker $brokerNode", e)

// If there is any socket error (eg, socket timeout), the connection is no longer usable and needs to be recreated.

networkClient.close(brokerNode.idString)

}

}

这里用一张图来说明下其执行流程：

![](https://article-images.zsxq.com/FuPYPgNzeSP7o1eMQQewI-t0JuiJ)

doWork 方法主要作用是**从阻塞队列中取出待发送的请求**，**然后把它发送出去等待 Response 响应**。**在等待 Response 过程中**，**线程将一直处于阻塞状态**。**当接收到 Response 后**，**调用 Callback 执行请求处理完成后的回调逻辑**。

> 这里需要注意的是，RequestSendThread 线程对请求发送的处理方式是调用 sendAndReceive 方法在发送完请求之后，会原地进入阻塞状态，等待 Response 返回。只有接收到 Response，并执行完回调逻辑之后，该线程才能从阻塞队列中取出下一个待发送请求进行处理。

最后我们来看下 「**RequestSendThread**」线程类的父类 「**controllerChannelManager**」。

##   
**04 ControllerChannelManager**

在 [【服务端 Broker 源码分析系列第十七篇】图解 Kafka 源码之 Broker 启动集群如何感知](https://articles.zsxq.com/id_j9ibrxqafawv.html) 这篇中，我们简单的剖析了该类的部分源码，这里我们再来看下。

「**controllerChannelManager**」类和 「**RequestSendThread**」线程类是合作共赢的关系。个人认为，它有两大类任务：

1.  管理 Controller 与集群 Broker 之间的连接，并为每个 Broker 创建 RequestSendThread 线程。
2.  将要发送的请求放入到指定 Broker 的阻塞队列中，等待该 Broker 的 RequestSendThread 线程取出并进行处理。

该类最重要的数据结构是 brokerStateInfo，几乎所有的方法都是对该数据结构进行管理，源码如下：

protected val brokerStateInfo \= new HashMap\[Int, ControllerBrokerStateInfo\]

这是一个 HashMap 类型，Key 是 Integer 类型，其实就是集群中 Broker 的 ID 信息，而 Value 是一个 ControllerBrokerStateInfo。

case class ControllerBrokerStateInfo(networkClient: NetworkClient,

brokerNode: Node,

messageQueue: BlockingQueue\[QueueItem\],

requestSendThread: RequestSendThread,

queueSizeGauge: Gauge\[Int\],

requestRateAndTimeMetrics: Timer,

reconfigurableChannelBuilder: Option\[Reconfigurable\])

它有三个非常关键的字段：

1.  **brokerNode**：它表示目标 Broker 节点，里面封装了目标 Broker 的连接信息，比如主机名、端口号等。
2.  **messageQueue**：请求阻塞队列。Controller 会给每个目标 Broker 都创建了这样的消息阻塞队列。
3.  **requestSendThread**：Controller 会使用该线程从阻塞队列中取出待发送请求发送给目标 Broker。

请你记住这三个字段，它们是实现「**Controller**」发送请求的关键。为什么这么说呢？

这里你可以设想下，如果「**Controller**」想发送请求给其他 Broker，肯定需要解决以下三个问题：

1.  发给谁：由 **brokerNode** 来决定。
2.  发什么： **messageQueue** 里面保存了待发送请求。
3.  怎么发：依赖 **requestSendThread** 线程进行发送。

最后，这里再总结下 「**controllerChannelManager**」类的方法，如下：

1.  startup() ：Controller 组件在启动时，会调用 ControllerChannelManager 的 startup 方法。该方法会从元数据信息中找到集群的 Broker 列表，然后依次为它们调用 addBroker 方法，把它们加到 brokerStateInfo 变量中，最后再依次启动 brokerStateInfo 中的 RequestSendThread 线程。
2.  shutdown()：关闭所有 RequestSendThread 线程，并清空必要的资源。
3.  sendRequest()：发送请求，即把请求对象写入到请求队列。
4.  addBroker()：添加目标 Broker 到 brokerStateInfo 数据结构中，并创建必要的配套资源，如请求队列、RequestSendThread 线程对象等。最后，RequestSendThread 启动线程。
5.  removeBroker()：从 brokerStateInfo 移除目标 Broker 的相关配套资源。
6.  startRequestSendThread()：启动专属的 RequestSendThread 线程。

接下来，我们分别来看下这里的方法。

## **4.1 startup()**

// 在控制器创建时启动各个 Broker 上的请求发送线程。

def startup() = {

// 当前已经运行或正在关闭中的 Broker 列表，并对每个 Broker 调用 addNewBroker 方法

controllerContext.liveOrShuttingDownBrokers.foreach(addNewBroker)

brokerLock synchronized {

// 遍历 brokerStateInfo 映射表中每个 Broker 状态信息，调用 startRequestSendThread 方法以启动该 Broker 的请求发送线程。

brokerStateInfo.foreach(brokerState => startRequestSendThread(brokerState.\_1))

}

}

// 启动指定 Broker 的请求发送线程

protected def startRequestSendThread(brokerId: Int): Unit = {

val requestThread \= brokerStateInfo(brokerId).requestSendThread

// 首先获取 Broker 状态信息中的请求发送线程实例 requestThread。如果该线程状态为 NEW，则启动该线程。

if (requestThread.getState == Thread.State.NEW)

requestThread.start()

}

该方法主要用来**启动各个 Broker 的请求发送线程的**，比较简单自行研究下即可。

## **4.2 shutdown()**

def shutdown() = {

brokerLock synchronized {

// 遍历 brokerStateInfo 映射表中每个 Broker 状态信息，调用 removeExistingBroker 方法删除该 Broker 的相关配套资源。

brokerStateInfo.values.toList.foreach(removeExistingBroker)

}

该方法主要用来**删除各个 Broker 的配套资源的**，比较简单自行研究下即可。

## **4.3 sendRequest()**

def sendRequest(brokerId: Int, request: AbstractControlRequest.Builder\[\_ <: AbstractControlRequest\],callback: AbstractResponse => Unit = null): Unit = {

brokerLock synchronized {

// 获取 Broker 状态信息

val stateInfoOpt \= brokerStateInfo.get(brokerId)

stateInfoOpt match {

case Some(stateInfo) =>

// 如果匹配则写入请求到阻塞队列

stateInfo.messageQueue.put(QueueItem(request.apiKey, request, callback, time.milliseconds()))

case None \=\>

warn(s"Not sending request $request to broker $brokerId, since it is offline.")

}

}

}

该方法主要用来**将请求写入指定 Broker 的阻塞队列中，以在下一轮周期中向该 Broker 发送请求，并根据响应调用回调函数处理**，比较简单自行研究下即可。

## **4.4 addBroker()**

每当集群中扩容了新的 Broker 时，Controller 就会调用该方法为新 Broker 增加新的 RequestSendThread 线程。

def addBroker(broker: Broker): Unit = {

// be careful here. Maybe the startup() API has already started the request send thread

brokerLock synchronized {

// 如果该 Broker 是新 Broker 的话

if (!brokerStateInfo.contains(broker.id)) {

// 添加新的 Broker，即将新 Broker 加入到 Controller 管理，并创建对应的 RequestSendThread 线程

addNewBroker(broker)

// 启动请求发送线程

startRequestSendThread(broker.id)

}

}

}

可以看到，整个代码段被 brokerLock 保护起来了。这是因为 brokerStateInfo 仅仅是一个 HashMap 对象，是非线程安全的，所以任何访问该变量的地方，都需要锁的保护。

步骤如下：

1.  判断目标 Broker Id 是否已经保存在 brokerStateInfo 中。如果已存在，就说明这个 Broker 之前已经添加过了，没必要再次添加了。
2.  否则会对目前的 Broker 执行以下两个操作：
3.  把该 Broker 节点添加到 brokerStateInfo 中。
4.  启动与该 Broker 对应的 RequestSendThread 线程。

先来看第一个操作：addNewBroker()。

## **4.4 addNewBroker()**

private def addNewBroker(broker: Broker): Unit = {

// 创建用于存储消息的阻塞队列

val messageQueue \= new LinkedBlockingQueue\[QueueItem\]

// 打印调试信息

debug(s"Controller ${config.brokerId} trying to connect to broker ${broker.id}")

// 获取控制器与 Broker 之间的网络监听器名称和安全协议

val controllerToBrokerListenerName \= config.controlPlaneListenerName.getOrElse(config.interBrokerListenerName)

val controllerToBrokerSecurityProtocol \= config.controlPlaneSecurityProtocol.getOrElse(config.interBrokerSecurityProtocol)

// 构建与 Broker 的连接

val brokerNode \= broker.node(controllerToBrokerListenerName)

val logContext \= new LogContext(s"\[Controller id=${config.brokerId}, targetBrokerId=${brokerNode.idString}\] ")

// 构建网络客户端和可重配置的通道构建器， NetworkClient类是Kafka clients工程封装的顶层网络客户端API，内部提供了丰富的方法实现网络层 I/O 数据传输

val (networkClient, reconfigurableChannelBuilder) = {

// 创建通道构建器

val channelBuilder \= ChannelBuilders.clientChannelBuilder(

controllerToBrokerSecurityProtocol,

JaasContext.Type.SERVER,

config,

controllerToBrokerListenerName,

config.saslMechanismInterBrokerProtocol,

time,

config.saslInterBrokerHandshakeRequestEnable,

logContext

)

// 如果通道构建器支持重新配置，则将其添加到 Controller 的可重配置列表中

val reconfigurableChannelBuilder \= channelBuilder match {

case reconfigurable: Reconfigurable =>

config.addReconfigurable(reconfigurable)

Some(reconfigurable)

case \_ \=\> None

}

// 创建网络选择器和网络客户端用于网络传输

val selector \= new Selector(

NetworkReceive.UNLIMITED,

Selector.NO\_IDLE\_TIMEOUT\_MS,

metrics,

time,

"controller-channel",

Map("broker-id" -> brokerNode.idString).asJava,

false,

channelBuilder,

logContext

)

val networkClient \= new NetworkClient(

selector,

new ManualMetadataUpdater(Seq(brokerNode).asJava),

config.brokerId.toString,

1,

0,

0,

Selectable.USE\_DEFAULT\_BUFFER\_SIZE,

Selectable.USE\_DEFAULT\_BUFFER\_SIZE,

config.requestTimeoutMs,

config.connectionSetupTimeoutMs,

config.connectionSetupTimeoutMaxMs,

ClientDnsLookup.USE\_ALL\_DNS\_IPS,

time,

false,

new ApiVersions,

logContext

)

(networkClient, reconfigurableChannelBuilder)

}

// 构建用于发送请求的线程名称

val threadName \= threadNamePrefix match {

case None \=\> s"Controller-${config.brokerId}-to-broker-${broker.id}-send-thread"

case Some(name) => s"$name:Controller-${config.brokerId}-to-broker-${broker.id}-send-thread"

}

// 构造请求处理速率监控指标

val requestRateAndQueueTimeMetrics \= newTimer(

RequestRateAndQueueTimeMetricName, TimeUnit.MILLISECONDS, TimeUnit.SECONDS, brokerMetricTags(broker.id)

)

// 构建请求发送线程，并设置为非守护线程

val requestThread \= new RequestSendThread(

config.brokerId,

controllerContext,

messageQueue,

networkClient,

brokerNode,

config,

time,

requestRateAndQueueTimeMetrics,

stateChangeLogger,

threadName

)

requestThread.setDaemon(false)

// 创建队列大小指标

val queueSizeGauge \= newGauge(QueueSizeMetricName, () => messageQueue.size, brokerMetricTags(broker.id))

// 创建该 Broker 专属的 ControllerBrokerStateInfo 实例,将 Broker 的相关信息存储到 brokerStateInfo 中统一管理

brokerStateInfo.put(broker.id, ControllerBrokerStateInfo(networkClient, brokerNode, messageQueue,requestThread, queueSizeGauge, requestRateAndQueueTimeMetrics, reconfigurableChannelBuilder))

}

该方法的关键在于，**要为目标 Broker 创建一系列配套资源**，比如，NetworkClient 用于网络 I/O 操作、messageQueue 用于阻塞队列、requestThread 用于发送请求等等。

接下来第二个操作：启动发送请求线程。

##   
**4.5 startRequestSendThread()**

protected def startRequestSendThread(brokerId: Int): Unit = {

// 获取指定 brokerid 的请求发送线程

val requestThread \= brokerStateInfo(brokerId).requestSendThread

// 如果请求发送线程的状态为刚创建的，则启动线程

if (requestThread.getState == Thread.State.NEW)

// 启动发送请求线程。

requestThread.start()

}

该方法比较简单，步骤如下：

1.  它首先根据给定的 Broker Id 信息，从 brokerStateInfo 中找出对应的 ControllerBrokerStateInfo 对象有了这个对象，也就有了为该目标 Broker 服务的所有配套资源。
2.  从 ControllerBrokerStateInfo 中拿出 RequestSendThread 对象，启动发送请求线程。

## **4.6 removeBroker()**

/\*\*

\* 删除 Broker

\*/

def removeBroker(brokerId: Int): Unit = {

// 对 Broker 进行同步操作

brokerLock synchronized {

// 删除已存在的 Broker

removeExistingBroker(brokerStateInfo(brokerId))

}

}

/\*\*

\* 删除已存在的 Broker

\*/

private def removeExistingBroker(brokerState: ControllerBrokerStateInfo): Unit = {

try {

// 在关闭 NetworkClient 之前关闭 RequestSendThread 以避免在 KAFKA-4959 中描述的非线程安全类的并发使用。

// ShutdownableThread.shutdown()方法中的 shutdownLatch.await() 调用充当同步屏障，它将NetworkClient 从 RequestSendThread 移交给了 ZkEventThread。

// 如果 Broker 状态具有可重配置通道构建器，则从配置中删除该构建器

brokerState.reconfigurableChannelBuilder.foreach(config.removeReconfigurable)

// 关闭请求发送线程

brokerState.requestSendThread.shutdown()

// 关闭NetworkClient

brokerState.networkClient.close()

// 清空消息队列

brokerState.messageQueue.clear()

// 删除指定 Broker 的队列大小指标和请求速率与队列时间指标

removeMetric(QueueSizeMetricName, brokerMetricTags(brokerState.brokerNode.id))

removeMetric(RequestRateAndQueueTimeMetricName, brokerMetricTags(brokerState.brokerNode.id))

// 从 Broker 状态信息中移除该 Broker

brokerStateInfo.remove(brokerState.brokerNode.id)

} catch {

case e: Throwable => error("Error while removing broker by the controller", e)

}

}

该方法相对比较简单，就是**删除目标 Broker 启动时创建一系列配套资源**。

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头重点剖析了「**Controller**」向 Broker 发送请求机制的实现原理以及发送请求类型总结，并对相关抽象类源码进行剖析。

2、接着深度剖析了「**请求发送线程**」的源码实现，Controller 主要通过 「**controllerChannelManager**」类来实现与其他 Broker 之间的请求发送。「**controllerChannelManager**」类中定义的 「**RequestSendThread**」是主要的线程实现类，用于实际发送请求给集群 Broker。

3、接着深度剖析了「**controllerChannelManager**」类中的「**请求阻塞队列**」+「**请求发送线程**」，里面的几个方法都是围绕这两个重要属性进行操作和管理的。

下篇我们来深度剖析「**控制器 Controller 如何处理事件的**」，大家期待，我们下期见。