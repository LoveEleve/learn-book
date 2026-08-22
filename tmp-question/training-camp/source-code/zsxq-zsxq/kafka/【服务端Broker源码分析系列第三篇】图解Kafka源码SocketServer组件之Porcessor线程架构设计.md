大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**SocketServer 组件之 Acceptor 线程架构设计与源码实现**」，今天我们就来深度聊聊「**SocketServer 组件之 Processor 线程架构设计与源码实现**」，看看 Kafka 服务端是如何处理超高并发的网络连接的。

![](https://article-images.zsxq.com/FtpuaW5P5PgVcgOc_OIp2I7KzC0X)

## **01 总体概述**

最近这几篇文章都是基于下面这张图来展开的，这是 Kafka 服务端超高并发的网络通信架构，如下图所示：

![](https://article-images.zsxq.com/lojiZJNqUko4zFwqFJ8m4NNg9614)

在上一篇中，我们从整体上带你分析了「**Acceptor 线程类**」的架构设计，接收连接、Processor 线程池管理、线程启动、连接分发等源码实现。

通过上图我们可以得到，我们知道 Acceptor 只是做了请求入口连接处理的，那么， **真正创建网络连接以及分发网络请求**是由 「**Processor** **线程**」来完成的。今天这篇我们的主角是主要「**Processor** **线程**」，带大家分析「**Processor** **线程**」的源码实现。

为了方便大家理解，所有的源码只保留骨干。

##   
**02 Processor 线程类**

![](https://article-images.zsxq.com/lgO4TPo1N7ozPCHDwuU7eoEeNWkY)

这里我们也先来拆解下上面那张 Kafka 超高并发网络架构图，这里只保留「**Processor 线程**」部分，从图中，我们可以得出，「**Processor 线程类**」会单独构建一个 「**NewConnections 队列**」用来存放 SocketChannel 和 一个「**inflightResponses 队列**」 用来存放临时 Response 请求数据，还有一个 「**ResponseQueue 队列**」用来存放响应数据。

Processor 线程类也是 SocketServer 组件类的一部分，源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/SocketServer.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/SocketServer.scala)

这里我先来看看 Processor 这个线程类的重要字段和参数。

##   
**2.1 参数与重要字段**

private\[kafka\] object Processor {

val IdlePercentMetricName \= "IdlePercent"

val NetworkProcessorMetricTag \= "networkProcessor"

val ListenerMetricTag \= "listener"

// 硬编码长度

val ConnectionQueueSize \= 20

}

/\*\*

\* Thread that processes all requests from a single connection. There are N of these running in parallel

\* each of which has its own selector

\*/

private\[kafka\] class Processor(val id: Int,

time: Time,

maxRequestSize: Int,

requestChannel: RequestChannel,

connectionQuotas: ConnectionQuotas,

connectionsMaxIdleMs: Long,

failedAuthenticationDelayMs: Int,

listenerName: ListenerName,

securityProtocol: SecurityProtocol,

config: KafkaConfig,

metrics: Metrics,

credentialProvider: CredentialProvider,

memoryPool: MemoryPool,

logContext: LogContext,

connectionQueueSize: Int = ConnectionQueueSize) extends AbstractServerThread(connectionQuotas) with KafkaMetricsGroup {

....

// 创建连接队列

private val newConnections \= new ArrayBlockingQueue\[SocketChannel\](connectionQueueSize)

// 创建临时 Response 队列

private val inflightResponses \= mutable.Map\[String, RequestChannel.Response\]()

// 创建Response 队列

private val responseQueue \= new LinkedBlockingDeque\[RequestChannel.Response\]()

....

// 创建监听网络事件的 KSelector 对象

private val selector \= createSelector(

ChannelBuilders.serverChannelBuilder(listenerName,

listenerName == config.interBrokerListenerName,

securityProtocol,

config,

credentialProvider.credentialCache,

credentialProvider.tokenCache,

time,

logContext))

// Visible to override for testing

protected\[network\] def createSelector(channelBuilder: ChannelBuilder): KSelector = {

channelBuilder match {

case reconfigurable: Reconfigurable => config.addReconfigurable(reconfigurable)

case \_ \=\>

}

new KSelector(

maxRequestSize,

connectionsMaxIdleMs,

failedAuthenticationDelayMs,

metrics,

time,

"socket-server",

metricTags,

false,

true,

channelBuilder,

memoryPool,

logContext)

}

我们来看看 「**Processor 线程**」初始化时都做了哪些事情，从上面代码可以得出，每个 「**Processor 线程**」在创建时都会创建下面 3 个队列。

1.  **newConnections**: 它是用来存放要创建的新连接信息，类型为 ArrayBlockingQueue\[SocketChannel\] 的队列，保存的是 SocketChannel 对象，这是一个**默认长度为20的队列，从上面代码可以看出是硬编码的，你无法改变它的长度。**「**Acceptor 线程**」创建 SocketChannel 后会把 SocketChannel 放到这个队列中，在后面创建连接时会从该队列中取出 SocketChannel，然后注册新的连接。
2.  **inflightResponses**: 它是一个临时的 Response 队列，类型为 mutable.Map\[String, RequestChannel.Response\] 的集合。里面保存是每个连接对应的返回给客户端途中的响应数据。**对于有些 Response 回调逻辑要在 Response 被发送回客户端之后才能执行**，因此需要暂存在一个临时队列里面。这就是 inflightResponses 存在的意义。
3.  **responseQueue** 它是 Response 队列，类型为 LinkedBlockingDeque\[RequestChannel.Response\] 的队列，即 **对每个 Processor 线程都会维护自己的 Response 队列，**它里面保存着要返回给客户端的所有 Response 响应对象。

另外还会生成 Kselector 对象用来**监听网络事件，KSelector 线程类是 Kafka 基于 Java NIO 的 Selector 封装的类，**如果忘记了，可以点开[【生产者源码分析系列第四篇】图解 Kafka 网络层实现机制之Selector 多路复用器](https://articles.zsxq.com/id_r3td0dogfg0v.html) 学习。

介绍完类参数和重要字段后，我们来看看「**Processor 线程类**」的重要方法，相对比较多，它是真正处理网络读写的类，下面带你挨个深度剖析。

## **2.2 accept()**

这里也先来看看建立连接的方法，它是由 Acceptor 线程来调用的，主要用来**把新建的 SocketChannel 添加到 newConnections 队列，等待 Processor 线程进行处理**。

/\*\*

\* Queue up a new connection for reading

\*/

def accept(socketChannel: SocketChannel, // 新传入的 SocketChannel 对象。

mayBlock: Boolean, // 是否可以阻塞在等待新连接上。

acceptorIdlePercentMeter: com.yammer.metrics.core.Meter // 当等待新连接时，记录 Acceptor 空闲时间的 Meter 对象。

): Boolean = {

val accepted \= {

// 1、首先尝试将传入的 socketChannel 添加到 newConnections 队列中。如果队列没满添加成功，直接返回 true。

if (newConnections.offer(socketChannel))

true

// 2、如果队列已满且可以阻塞，将 SocketChannel 添加到 newConnections 队列中，并且在等待新连接时需要记录 Acceptor 空闲时间，则记录开始等待的时间，然后一直等待，直到有空余的位置或者线程被中断。最后使用 Meter 计算执行等待的时间，并返回 true。

else if (mayBlock) {

val startNs \= time.nanoseconds

newConnections.put(socketChannel)

acceptorIdlePercentMeter.mark(time.nanoseconds() - startNs)

true

} else

// 3、如果队列已满且不能阻塞，则直接返回 false。

false

}

// 4、如果连接添加成功，则唤醒 Selector，使其返回从 newConnections 队列中接受的准备就绪的连接。

if (accepted)

wakeup()

accepted

}

这个方法其实很简单，主要做了下面 3 件事情：

1.  首先尝试将传入的 socketChannel 添加到 newConnections 队列中。如果队列没满添加成功，直接返回 true。
2.  如果队列已满且可以阻塞，将 SocketChannel 添加到 newConnections 队列中，并且在等待新连接时需要记录 Acceptor 空闲时间，则记录开始等待的时间，然后一直等待，直到有空余的位置或者线程被中断。最后使用 Meter 计算执行等待的时间，并返回 true。
3.  如果队列已满且不能阻塞，则直接返回 false。
4.  如果连接添加成功，则唤醒 Selector，使其返回从 newConnections 队列中接受的准备就绪的连接。

这里总结一下：就是把 socketChannel 放入「**Processor 线程类**」的 「**newConnections 队列**」里。也就是说 Processor 线程类并没有直接处理 socketChannel，而是 「**Acceptor 线程类**」把 socketChannel 对象先添加到newConnections 队列里，等待 Processor 线程进行处理。

接下来，我们来看看 「**Processor 线程类**」的启动方法，即 run()。

## **2.3 run()**

override def run(): Unit = {

// 1、等待 Processor 线程启动完成

startupComplete()

try {

// 当正在运行中

while (isRunning) {

try {

// 2、创建新连接

// setup any new connections that have been queued up

configureNewConnections()

// 3、发送 response 响应给客户端,并把要发送的 response 放入 inflightResponses 临时队列里。

// register any new responses for writing

processNewResponses()

// 4、执行NIO poll()方法，获取对应 SocketChannel 上准备就绪的 I/O 操作，真正的发送response 到客户端。

poll()

// 5、处理成功接收到的请求。将接收到的 Request 放入 RequestQueue 队列。

processCompletedReceives()

// 6、处理成功发送的响应。对临时 Response 队列中的 Response 执行回调逻辑。

processCompletedSends()

// 7、获取发送失败而导致断开的连接，然后处理这些连接。

processDisconnected()

// 8、关闭超过配额限制部分的连接。

closeExcessConnections()

} catch {

// We catch all the throwables here to prevent the processor thread from exiting. We do this because

// letting a processor exit might cause a bigger impact on the broker. This behavior might need to be

// reviewed if we see an exception that needs the entire broker to stop. Usually the exceptions thrown would

// be either associated with a specific socket channel or a bad request. These exceptions are caught and

// processed by the individual methods above which close the failing channel and continue processing other

// channels. So this catch block should only ever see ControlThrowables.

case e: Throwable => processException("Processor got uncaught exception.", e)

}

}

} finally {

// 关闭底层资源

debug(s"Closing selector - processor $id")

CoreUtils.swallow(closeAll(), this, Level.ERROR)

shutdownComplete()

}

}

通过上面代码可以看出，run() 方法逻辑拆分的相当好，各个子方法各司其职边界非常清楚，主要做了以下 8 件事情：

1.  等待 Processor 线程启动完成。
2.  调用 configureNewConnections() 负责处理新连接请求。将 SocketChannel 注册到 selector 上, 然后 Selector 监听SocketChannel 上的 OP\_READ 事件。
3.  调用 processNewResponses() 从响应队列里拿出一个response。发送 response 响应给客户端,并把要发送的 response 放入 inflightResponses 临时队列里。。
4.  调用 NIO poll() 获取对应 SocketChannel 上准备就绪的 I/O 操作，它会真正的发送 response 到客户端。。
5.  调用 processCompletedReceives() 处理成功接收到的请求。将接收到的 Request 放入 RequestQueue 队列。
6.  调用 processCompletedSends() 处理成功发送的响应。对临时 Response 队列中的每个 Response 执行回调逻辑。
7.  调用 processDisconnected() 获取发送失败而导致断开的连接，然后处理这些连接。
8.  调用 closeExcessConnections() 关闭超过配额限制部分的连接。

流程图如下：

![](https://article-images.zsxq.com/llDChdCQgo8N5Sb3pDXdhUn5rLS4)

下面我们会根据 run() 方法的中的方法调用顺序，挨个剖析里面的方法，先来看看处理新连接请求，对应的是 configureNewConnections 方法。

## **2.4 configureNewConnections****()**

/\*\*

\* Register any new connections that have been queued up. The number of connections processed

\* in each iteration is limited to ensure that traffic and connection close notifications of

\* existing channels are handled promptly.

\*/

private def configureNewConnections(): Unit = {

// 1、当前已配置的连接数计数器

var connectionsProcessed \= 0

// 2、迭代的条件是要处理的连接必须小于connectionQueueSize=20个，且连接队列不能为空

while (connectionsProcessed < connectionQueueSize && !newConnections.isEmpty) {

// 3、从连接队列中取出一个 SocketChannel

val channel \= newConnections.poll()

try {

debug(s"Processor $id listening to new connection from ${channel.socket.getRemoteSocketAddress}")

// 4、将取出的 SocketChannel 注册到指定的 Selector 上，并监听 OP\_READ 事件，其底层是调用 Java NIO 的 SocketChannel.register(selector, SelectionKey.OP\_READ) 来实现

selector.register(connectionId(channel.socket), channel)

// 5、更新连接数计数器

connectionsProcessed += 1

} catch {

// We explicitly catch all exceptions and close the socket to avoid a socket leak.

case e: Throwable =>

val remoteAddress \= channel.socket.getRemoteSocketAddress

// need to close the channel here to avoid a socket leak.

close(listenerName, channel)

processException(s"Processor $id closed connection from $remoteAddress", e)

}

}

}

该方法作用是**调用 Selector.register 来注册 SocketChannal，且每个 Processor 线程都会维护一个 Selector 实例，**主要做了以下 5 件事：

1.  首先先初始化当前已配置的连接数计数器 connectionsProcessed。
2.  然后进入一个 while 循环体，必须满足以下 2 个条件才能进入循环：
3.  connectionsProcessed < connectionQueueSize：其中 connectionsProcessed 是循环内要处理的连接数计数器，connectionQueueSize 是连接队列的长度，默认值是20「**硬编码**」。也就是说，该方法被调用一次，只能处理 20 个连接。目的就是要保证已存在的连接通信通畅。
4.  !newConnections.isEmpty: 连接队列 newConnections 不为空，也就是说如果没有新的连接就不会进入 while 循环。
5.  当满足条件进入 while 循环后，从连接队列 newConnections 中取出一个 SocketChannel。
6.  将取出的 SocketChannel 注册到 Processor 线程类指定的 Selector 上，并监听 OP\_READ 事件。
7.  最后更新计数器，加1下次迭代判断使用。

接下来，我们来看看 「**Processor 线程类**」中是如何给客户端发送响应的，对应的方法是 processNewResponses()。

##   
**2.5 processNewResponses****()**

private def processNewResponses(): Unit = {

// 声明一个 response 对象

var currentResponse: RequestChannel.Response = null

// 从响应队列 ResponseQueue 中取出 response

while ({currentResponse = dequeueResponse(); currentResponse != null}) {

// 获取 channel\_id

val channelId \= currentResponse.request.context.connectionId

try {

// 匹配 response 类型，根据不同类型进行相应处理

currentResponse match {

// 1、无需发送响应给客户端，比如 ack = 0

case response: NoOpResponse =>

// 更新请求相关的指标。

updateRequestMetrics(response)

....

// 无需响应客户端，所以需要读取更多的请求，先尝试解除通道的静音状态

handleChannelMuteEvent(channelId, ChannelMuteEvent.RESPONSE\_SENT)

// 再次绑定 OP\_READ 事件

tryUnmuteChannel(channelId)

case response: SendResponse =>

// 2、真正的发送 Response 响应请求，并将其放入临时 inflightResponse 队列用来发送响应成功或响应失败后执行回调逻辑。

sendResponse(response, response.responseSend)

case response: CloseConnectionResponse =>

// 3、需要主动关闭连接的请求，更新请求相关的指标并主动关闭连接。

updateRequestMetrics(response)

trace("Closing socket connection actively according to the response code.")

close(channelId)

case \_: StartThrottlingResponse =>

// 4、开始限流的请求，记录事件并处理相关逻辑。

handleChannelMuteEvent(channelId, ChannelMuteEvent.THROTTLE\_STARTED)

case \_: EndThrottlingResponse =>

// 5、结束限流的请求，记录事件并处理相关逻辑。

handleChannelMuteEvent(channelId, ChannelMuteEvent.THROTTLE\_ENDED)

// 并尝试解除通道的静音状态。

tryUnmuteChannel(channelId)

case \_ \=\>

// 5、对于未知类型的请求，抛出异常。

throw new IllegalArgumentException(s"Unknown response type: ${currentResponse.getClass}")

}

} catch {

case e: Throwable =>

processChannelException(channelId, s"Exception while processing response for $channelId", e)

}

}

}

private def dequeueResponse(): RequestChannel.Response = {

// 从 responseQueue 取出一个 response

val response \= responseQueue.poll()

if (response != null)

response.request.responseDequeueTimeNanos = Time.SYSTEM.nanoseconds

response

}

该方法作用是**负责将 Response 响应请求发送到 Request 发送方，服务端通过不断地从响应队列中取出请求，并根据请求类型进行相应地处理，**主要做了以下 2 件事：

1.  声明一个 response 变量用来保存要发送的 response 响应请求对象，从响应队列「**ResponseQueue**」里取出一个response 进行赋值，这里的响应队列是个阻塞类型「**LinkedBlockingDeque**」队列，如果没有响应对象就一直会阻塞。
2.  匹配 response 类型，根据不同类型进行相应处理。
3.  对于无需响应的请求（NoOpResponse）：这种情况一般就是生产环境设置了 ack = 0，表示无需服务端的响应。更新请求相关的指标，然后对应的 SocketChannel 要继续处理读事件，调用方法 tryUnmuteChannel(channelId) 尝试解除通道的静音状态并再次绑定OP\_READ事件。
4.  对于需要发送响应的请求（SendResponse）：调用 sendResponse() 真正的发送 Response 响应请求，并将其放入临时 inflightResponse 队列用来发送响应成功或响应失败后执行回调逻辑。
5.  对于需要主动关闭连接的请求（CloseConnectionResponse）：更新请求相关的指标，并主动关闭连接。
6.  对于开始限流的请求（StartThrottlingResponse）：记录事件并处理相关逻辑。
7.  对于结束限流的请求（EndThrottlingResponse）：记录事件，并尝试解除通道的静音状态。
8.  对于未知类型的请求，抛出异常。

这里我们继续来看看发送响应请求准备工作，对应的方法是 SendResponse()。

## **2.6 sendResponse****()**

// 发送响应请求

protected\[network\] def sendResponse(response: RequestChannel.Response, responseSend: Send): Unit = {

// 1、获取 Response 对应的 connectionId（即客户端连接 ID）。

val connectionId \= response.request.context.connectionId

....

// 2、判断存储连接集合中是否存在该 kafkaChannel 连接，如果不存在，则记录一条“尝试发送 Response 消息的连接ID不存在”的 WARN 级别的日志，并使用 request 对应的累加器更新其请求指标(metric)的值。

if (channel(connectionId).isEmpty) {

warn(s"Attempting to send response via channel for which there is no open connection, connection id $connectionId")

response.request.updateRequestMetrics(0L, response)

}

// 3、判断该连接是否打开或正在关闭状态，如果是，则通过 NIO Selector 将 Response 响应请求发送给客户端，并将 response 响应请求放入到 inflightResponses 临时响应队列中。

if (openOrClosingChannel(connectionId).isDefined) {

selector.send(responseSend)

inflightResponses += (connectionId -> response)

}

}

该方法作用是**完成发送响应请求的准备工作，并将 Response 响应请求放入到 inflightResponse 临时响应队列中，**主要做了以下 3 件事：

1.  获取 Response 对应的 connectionId（即客户端连接 ID）。
2.  判断存储连接集合中是否存在该 kafkaChannel 连接，如果不存在，则记录一条"尝试发送 Response 消息的连接ID不存在"的 WARN 级别的日志，并使用 request 对应的累加器更新其请求指标 (metric) 的值。
3.  判断该连接是否打开或正在关闭状态，如果是的话，则通过 NIO Selector 将 Response 响应请求发送给客户端，并将 connectionId -> response 响应请求放入到 inflightResponses 临时响应队列中。
4.  inflightResponses 队列里的响应请求**是指要发送但还没确定发送成功过的响应，目的就是 inflightResponses 队列里的 Response 有回调方法，会根据响应发送的结果进行回调逻辑处理**。

到这里，发送响应的准备工作就已经完成了，接下来我们来看看响应是如何发送到客户端的，对应方法是 poll()。

## **2.7 poll****()**

// 真正发送响应请求到客户端

private def poll(): Unit = {

// 计算轮询超时时间。如果没有新的连接需要建立， pollTimeout 设置为 300ms，否则为 0。

val pollTimeout \= if (newConnections.isEmpty) 300 else 0

// 底层调用 Java NIO 的 Selector 的 select 方法去执行那些准备就绪的 I/O 操作。

try selector.poll(pollTimeout)

catch {

case e @ (\_: IllegalStateException | \_: IOException) =>

// 如果在轮询过程中 Selector 抛出异常，记录异常日志，但不处理异常。这里的异常一般是状态非法或发生 I/O 错误等情况引起的。

error(s"Processor $id poll failed", e)

}

}

严格来说，poll() 方法才是**会真正发送 Response 响应请求到客户端，其核心代码就一行，**就是调用 [selector.poll](http://selector.poll/)()方法获取对应 SocketChannel 上准备就绪的 I/O 操作并执行对应操作。

当执行 poll() 方法真正发送响应后，接下来我们来看看如何处理成功接收到的请求的，对应的方法是 processCompletedReceives()。

## **2.8 processCompletedReceives****()**

private def processCompletedReceives(): Unit = {

// 1、遍历所有已接收的 Request 对象。

selector.completedReceives.forEach { receive =>

try {

// 2、根据接收到的数据源(receive.source)，找到对应的 Channel 对象，保证对应连接通道已经建立了，并获取对应的 channelId。

openOrClosingChannel(receive.source) match {

case Some(channel) =>

// 获取请求头

val header \= parseRequestHeader(receive.payload)

// 3、如果接收到的请求数据为 SASL\_HANDSHAKE 握手请求，且 channel 需要重新进行身份认证，则记录日志并进行后续处理。

if (header.apiKey == ApiKeys.SASL\_HANDSHAKE && channel.maybeBeginServerReauthentication(receive,

() => time.nanoseconds()))

trace(s"Begin re-authentication: $channel")

else {

val nowNanos \= time.nanoseconds()

// 4、如果 channel 中的身份认证会话已过期，则关闭该 channel 的连接，并更新统计信息。

if (channel.serverAuthenticationSessionExpired(nowNanos)) {

// be sure to decrease connection count and drop any in-flight responses

debug(s"Disconnecting expired channel: $channel : $header")

close(channel.id)

expiredConnectionsKilledCount.record(null, 1, 0)

} else {

// 获取连接id

val connectionId \= receive.source

// 5、根据请求数据创建 RequestContext 对象，进一步构造 Request 对象。

val context \= new RequestContext(header, connectionId, channel.socketAddress,

channel.principal, listenerName, securityProtocol,

channel.channelMetadataRegistry.clientInformation)

// 6、真正构建 request 对象

val req \= new RequestChannel.Request(processor = id, context = context,

startTimeNanos = nowNanos, memoryPool, receive.payload, requestChannel.metrics)

// KIP-511: ApiVersionsRequest is intercepted here to catch the client software name and version. It is done here to avoid wiring things up to the api layer.

// 如果接收到的请求数据为 API\_VERSIONS 请求，则从中提取客户端软件名称和版本，并更新 channel 的客户端信息注册表。

if (header.apiKey == ApiKeys.API\_VERSIONS) {

val apiVersionsRequest \= req.body\[ApiVersionsRequest\]

if (apiVersionsRequest.isValid) {

channel.channelMetadataRegistry.registerClientInformation(new ClientInformation(

apiVersionsRequest.data.clientSoftwareName,

apiVersionsRequest.data.clientSoftwareVersion))

}

}

// 7、核心操作：将 Request 添加到 requestChannel 的 requestQueue 队列中等待处理。

requestChannel.sendRequest(req)

// 8、调用 selector.mute 把 OP\_READ 事件移除。

selector.mute(connectionId)

handleChannelMuteEvent(connectionId, ChannelMuteEvent.REQUEST\_RECEIVED)

}

}

case None \=\>

// This should never happen since completed receives are processed immediately after \`poll()\`

throw new IllegalStateException(s"Channel ${receive.source} removed from selector before processing completed receive")

}

} catch {

// note that even though we got an exception, we can assume that receive.source is valid.

// Issues with constructing a valid receive object were handled earlier

case e: Throwable =>

processChannelException(receive.source, s"Exception while processing request from ${receive.source}", e)

}

}

// 9、最后清空 Selector 中已接收的 Request。

selector.clearCompletedReceives()

}

/\*\* Send a request to be handled, potentially blocking until there is room in the queue for the request \*/

// RequestChannel 类方法，下一篇会讲解

def sendRequest(request: RequestChannel.Request): Unit = {

requestQueue.put(request)

}

该方法相对比较复杂，作用是 **Processor 线程从底层 Socket 通道中不断读取已经接收到的请求 Request，然后构建 Request 对象，并将其添加到 RequestChannel 的 RequestQueue 队列中，**主要做了以下 9 件事：

1.  遍历 SocketChannel 已经接收成功的 Request 对象。已经接收成功的 Request 会保存在 **KSelector 的 competedReceives 集合内**，因此这里只要遍历该集合就能够获得已经发送成功的 Request 对象。
2.  根据接收到的数据源 ([receive.source](http://receive.source/))，找到对应的 Channel 对象，保证对应连接通道已经建立了，并获取对应的 channelId。
3.  如果接收到的请求数据为 SASL\_HANDSHAKE 握手请求，且 channel 需要重新进行身份认证，则记录日志并进行后续处理。
4.  如果 channel 中的身份认证会话已过期，则关闭该 channel 的连接，并更新统计信息。
5.  根据请求数据创建 RequestContext 对象，进一步构造 Request 对象。
6.  **真正构建 Request 对象**，如果接收到的请求数据为 API\_VERSIONS 请求，则从中提取客户端软件名称和版本，并更新 channel 的客户端信息注册表。
7.  **核心操作**：Processor 线程接收到 Request 对象后调用 requestChannel.sendRequest(req) 方法把 Request 对象放入 requestChannel 里的 requestQueue 队列里等待业务线程池 Handler 处理。
8.  调用 [selector.mute](http://selector.mute/) 把连接上的 OP\_READ 事件移除，**此时已经把这个 Channel 上的 OP\_READ 事件处理完成了并缓存了 Request 请求，这时移除连接上的 OP\_READ 事件就不会有新请求读进来，待业务线程池多个 Handler 线程去消费，为了保证顺序性，就需要多处注册、取消 OP\_READ 事件和 OP\_READ 事件，这样就可以保证同一时间只有一个请求被处理，直到完成后再处理下一个**。
9.  最后清空 Selector 中已接收的 Request。

完成接收请求 Request 对象添加到 RequestQueue 队列后，接下来我们来看看如何处理响应 Response 的，对应的方法是 processCompletedSends()。

## **2.9 processCompletedSends****()**

private def processCompletedSends(): Unit = {

// 1、遍历底层 SocketChannel 已经发送的 Response。

selector.completedSends.forEach { send =>

try {

// 2、取出对应 inflightResponses 中的 Response。

val response \= inflightResponses.remove(send.destination).getOrElse {

throw new IllegalStateException(s"Send for ${send.destination} completed, but not in \`inflightResponses\`")

}

// 更新统计指标

updateRequestMetrics(response)

// Invoke send completion callback

// 3、调用发送完成的回调方法

response.onComplete.foreach(onComplete => onComplete(send))

handleChannelMuteEvent(send.destination, ChannelMuteEvent.RESPONSE\_SENT)

// 4、重新注册 OP\_READ，这样又可以监听客户端请求了，即一个 channel 处理完了才会继续监听。

tryUnmuteChannel(send.destination)

} catch {

case e: Throwable => processChannelException(send.destination,

s"Exception while processing completed send to ${send.destination}", e)

}

}

// 5、最后清空 Selector 中已发送的 Request。

selector.clearCompletedSends()

}

该方法比较简单，作用是**负责处理 Response 回调逻辑，**主要做了 5 件事情：

1.  遍历 SocketChannel 已经发送成功的 Response 对象。已经发送成功的 Response 会保存在 **KSelector 的 competedSends 集合内**。因此这里只要遍历这个集合就能够获得已经发送成功的 Response。
2.  取出对应 inflightResponses 中的 Response，已经发送成功的响应就不应该在 inflightResponses 里。
3.  **调用发送完成的回调方法**。
4.  重新注册 OP\_READ，这样又可以监听客户端请求了，即一个 channel 处理完了才会继续监听。
5.  最后清空 Selector 中已发送的 Request。

最后在来看 2 个方法，即收尾工作。

## **2.10 processDisconnected****()**

private def processDisconnected(): Unit = {

// 1、遍历底层 SocketChannel 的已经断开的连接

selector.disconnected.keySet.forEach { connectionId =>

try {

// 2、尝试获取断开连接的远端主机名信息

val remoteHost \= ConnectionId.fromString(connectionId).getOrElse {

throw new IllegalStateException(s"connectionId has unexpected format: $connectionId")

}.remoteHost

// 3、将该连接从 inflightResponses 中移除，同时更新一些请求监控指标

inflightResponses.remove(connectionId).foreach(updateRequestMetrics)

// 4、更新连接配额（connectionQuotas），确保即使连接断开，每个客户端都不会占用太多的资源

connectionQuotas.dec(listenerName, InetAddress.getByName(remoteHost))

} catch {

case e: Throwable => processException(s"Exception while processing disconnection of $connectionId", e)

}

}

}

该方法比较简单，作用是**确保 Kafka 的连接器能够正确地处理客户端与服务端的连接断开，并在必要时对连接配额进行更新**。

## **2.11 closeExcessConnections****()**

// 控制连接数量并保持网络连接的稳定性。

private def closeExcessConnections(): Unit = {

// 如果连接配额超限了

if (connectionQuotas.maxConnectionsExceeded(listenerName)) {

// 找出优先级最低的那个连接

val channel \= selector.lowestPriorityChannel()

if (channel != null)

// 关闭该连接

close(channel.id)

}

}

/\*\*

\* 用来关闭指定连接

\*/

private def close(connectionId: String): Unit = {

openOrClosingChannel(connectionId).foreach { channel =>

debug(s"Closing selector connection $connectionId")

// 获取连接的地址

val address \= channel.socketAddress

if (address != null)

更新连接配额（connectionQuotas）

connectionQuotas.dec(listenerName, address)

// 将连接从 selector 中移除

selector.close(connectionId)

// 如果该连接有待处理的响应，将从 inflightResponses 移除它们并更新请求的度量信息。

inflightResponses.remove(connectionId).foreach(response => updateRequestMetrics(response))

}

}

该方法比较简单，作用是**控制连接数量并保持网络连接的稳定性，**这里所谓的「**优先关闭**」是指在众多 TCP 连接中找出**先最近未被使用的连接**。也就是说在最近一段时间内，没有任何 Request 由这个连接被发送到 Processor 线程。

至此，整个 「**Processor** **线程类**」的方法就带大家剖析完毕了。

## **03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过剖析 Kafka 网络通信架构设计，引出 Processor 线程。

2、带你深度剖析了 SocketServer 组件类之一 Processor 线程的源码实现，它主要用来真正处理网络读写请求，最后关闭连接资源。

下篇我们来深度剖析「**SocketServer 组件类之 RequestChannel 线程架构设计**」，大家期待，我们下期见。