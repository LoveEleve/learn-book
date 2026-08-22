大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了网络层请求处理的最后一篇 「**KafkaApis 的源码实现**」，今天我们就来深度剖析下「**Kafka 网络层请求处理全流程总结**」，看看 Kafka 服务端是如何一步步处理请求并返回响应的，以及了解 Kafka 服务端是如何支持百万并发网络连接的。

![](https://article-images.zsxq.com/FnbGb4Z7SrFGxJ4fzl7BaeXfBU0C)

## **01 总体概述**

最近这几篇文章都是基于下面这张图来展开的，这是 Kafka 服务端超高并发的网络通信架构，如下图所示：

![](https://article-images.zsxq.com/lojiZJNqUko4zFwqFJ8m4NNg9614)

在上一篇中，我们从整体上带你分析了「**KafkaApis**」源码实现，它主要用来真正处理请求的工具类，也是 Kafka 服务端网络层请求处理的最后一篇，今天这篇我们就来总结下。

为了方便大家理解，所有的源码只保留骨干。

## **02 流程总结**

通过我们的超高并发网络通信架构图，可以得出总共有 16 步，这里根据「**场景驱动**」的方式简化下步骤来串联总结下。

## **2.1 客户端发送请求到 Acceptor 线程**

在 [图解Kafka源码SocketServer组件之Acceptor线程架构设计](https://articles.zsxq.com/id_raazwmww8fo1.html) 这篇中，我们得知 「**Acceptor 线程**」会实时接收来自外部的发送请求。一旦接收到了之后就会创建对应的 SocketChannel 通道。

![](https://article-images.zsxq.com/FiAKZsnNJ0LL-VQoNs2KriC5JFCy)

def run(): Unit = {

// 1、向 nioSelector 注册 serverChannel 监听 serverChannel 上的 OP\_ACCEPT 事件等待客户端的请求

serverChannel.register(nioSelector, SelectionKey.OP\_ACCEPT)

// 2、启动 Acceptor 线程完成，并唤醒阻塞的线程。

startupComplete()

try {

// 3、记录当前使用的 Processor 序号，从0开始，最大值是 num.network.threads - 1

var currentProcessorIndex \= 0

while (isRunning) {

try {

// 4、每隔 500 毫秒读取底层通道上准备就绪 I/O 事件的数量

val ready \= nioSelector.select(500)

// 如果有就绪 I/O 事件

if (ready > 0) {

// 5、获取所有监听到的 SelectionKey 集合

val keys \= nioSelector.selectedKeys()

val iter \= keys.iterator()

// 遍历 SelectionKey 集合 事件

while (iter.hasNext && isRunning) {

try {

val key \= iter.next

iter.remove()

// 6、这里只监听 OP\_ACCEPT 事件。

if (key.isAcceptable) {

// 调用 accept 方法处理一个新的连接请求，返回新建的 SocketChannel，然后轮询处理 socketChannel，即接收此连接并分配对应的 Processor 线程

accept(key).foreach { socketChannel =>

// 获取 processors 线程池的锁，保证多线程并发访问的线程安全

var retriesLeft \= synchronized(processors.length)

var processor: Processor = null

do {

retriesLeft -= 1

// 7、轮询选择哪个 Processor 线程进行处理

processor = synchronized {

currentProcessorIndex = currentProcessorIndex % processors.length

// 从 Processor 线程池中取出对应的 processor 线程

processors(currentProcessorIndex)

}

// 8、取出后更新 Processor 线程序号。

currentProcessorIndex += 1

// 9、将新连接的 SocketChannel 添加到 Processor 线程等待处理连接队列，等待 Processor 线程后续处理

// 这里会分配给不同的 Processor 线程，保证请求处理的负载均衡。

} while (!assignNewConnection(socketChannel, processor, retriesLeft == 0))

}

} else

// 这里只注册了 OP\_ACCEPT 事件，如果是其他事件直接抛出异常。

throw new IllegalStateException("Unrecognized key state for acceptor thread.")

} catch {....}

}

}

}

catch {....}

}

} finally {....}

}

上面是「**Acceptor 线程**」的分发逻辑的方法，可以看到「**Acceptor 线程**」注册「**OP\_READ 事件**」到 「**Selector**」上，然后调用「**accept()**」方法创建对应的「**SocketChannel**」，然后将该 Channel 实例传给 「**assignNewConnection()**」方法，等待「**Processor 线程**」将该 Socket 连接请求放入到它维护的待处理连接队列「**NewConnections**」中。

下一步「**Processor 线程**」的「**run()**」方法会不断地从该「**NewConnections**」队列中取出这些 Socket 连接请求，然后**真正的创建对应的 Socket 连接**。

在上面代码中的「**assignNewConnection()**」方法的主要作用是：将这个新建的 SocketChannel 对象存入 「**Processor 线程**」的「**NewConnections**」队列中。然后「**Processor 线程**」会不断轮询该「**NewConnections**」队列中的待处理 SocketChannel，并向这些 SocketChannel 注册基于 Java NIO 的 「**Selector**」，用于真正的请求获取和响应发送 I/O 事件操作。

综上，「**Acceptor 线程**」仅仅是将客户端的连接请求创建对应的 SocketChannel，然后传递给「**Processor 线程**」 而已。

## **2.3 Processor 线程处理请求，添加到 RequestQueue 请求队列**

在 [图解Kafka源码SocketServer组件之Porcessor线程架构设计](https://articles.zsxq.com/id_bh7qwrhpw9mb.html) 这篇中，我们得知 「**Processor 线程**」会处理 「**Acceptor 线程**」发送过来的连接请求。

  
![](https://article-images.zsxq.com/FkQrc2XbFhkP1Bq87acpVhRlBlHx)

一旦 「**Processor 线程**」 调用该类的「**ConfigureNewConnections**」方法成功地向 「**SocketChannel**」注册了「**Selector**」，那么客户端「**Clients 端或者 其他 Broker 端**」发送过来的请求就能通过该 「**SocketChannel**」被获取到，对应方法是「**processCompletedReceives**」方法。.

private def processCompletedReceives(): Unit = {

// 1、从 Selector 中提取已接收到的所有请求数据，然后遍历这些 Request 对象

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

....

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

....

// 7、核心操作：将 Request 添加到 requestChannel 的 requestQueue 队列中等待处理。

requestChannel.sendRequest(req)

// 8、调用 selector.mute 把 OP\_READ 事件移除。

selector.mute(connectionId)

....

}

}

case None \=\>

. ....

}

} catch {

// note that even though we got an exception, we can assume that receive.source is valid.

// Issues with constructing a valid receive object were handled earlier

case e: Throwable =>

....

}

}

// 9、最后清空 Selector 中已接收的 Request。

selector.clearCompletedReceives()

}

该方法会将「**Selector**」获取到的所有 「**Receive**」对象转换成对应的「**Request**」对象，然后将这些「**Request**」对象添加到请求队列「**RequestQueue**」中，对应架构图中的第 3 ~ 9 步骤。

综上，「**Processor 线程**」处理连接请求，就是从底层 I/O 获取到发送的连接数据，然后将其转换成「**Request**」对象实例，并最终添加到请求队列「**RequestQueue**」的过程。

## **2.3 I/O 业务线程处理请求**

在 [图解Kafka源码之 KafkaRequestHandler I/O 线程池剖析](https://articles.zsxq.com/id_ohjd0i8l3w57.html) 这篇中，我们得知 「**KafkaRequestHandler**」会定时从「**RequestChannel 请求通道**」的「**RequestQueue 队列**」中获取 Request 请求对象然后调用 「**KafkApis**」的 「**handle()**」执行真正的业务逻辑处理。

  
跟 「**KafkaRequestHandler**」相比， 上面所说的「**Acceptor 线程**」、「**Processor 线程**」还有 「**RequestChannel**」等都不做请求处理， 它们只是请求和响应的「**搬运工**」。

接下来，该「**KafkaRequestHandler**」的「**run()**」方法上场了。

def run(): Unit = {

// 1、只要该线程尚未关闭就一直循环运行处理逻辑

while (!stopped) {

....

// 2、每隔300ms从请求队列中获取下一个待处理的请求

val req \= requestChannel.receiveRequest(300)

....

// 3、匹配 request 类型，分情况处理

req match {

// 关闭线程请求

case RequestChannel.ShutdownRequest =>

....

// 普通请求

case request: RequestChannel.Request =>

try {

....

// 调用 KafkaApis.handle 方法对 Request对象执行相应处理逻辑

apis.handle(request)

} catch {

....

} finally {

// 释放请求对象占用的内存缓冲区资源

request.releaseBuffer()

}

case null => // continue

}

}

// 4、如果 stopped = true，即关闭状态，则关闭线程

shutdownComplete.countDown()

}

##   
**2.4 I/O 业务线程将 Response 放入到 ResponseQueue 队列**

其实这一步的工作是由「**KafkApis**」类完成。对应 [图解Kafka源码之 KafkaApis 详解](https://articles.zsxq.com/id_k4o72eh3pai3.html)，当然也是由「**KafkaRequestHandler**」线程来完成的。对应「**KafkApis**」类中的「**SendResponse**」方法，将对「**Request**」的处理结果 「**Response**」发送出去，实际上底层是调用了 「**RequestChannel 请求通道**」的 「**SendResponse**」方法。

def sendResponse(response: RequestChannel.Response): Unit = {

....

// 找出 response 对应的 Processor 线程，即 request 当初是由哪个 Processor 线程处理，请求和响应对应的是同一个 Processor。

val processor \= processors.get(response.processor)

if (processor != null) {

// 将 response 对象添加到对应 Processor 线程的 response 队列中

processor.enqueueResponse(response)

}

}

// Processor 线程类的方法，就是添加响应到 ResponseQueue 队列中，并唤醒正在等待响应的处理线程。

private\[network\] def enqueueResponse(response: RequestChannel.Response): Unit = {

responseQueue.put(response)

wakeup()

}

## **2.5 Processor 线程返还 Response 给客户端**

最后一步，「**Processor 线程**」取出 「**ResponseQueue**」队列中的「**Response**」并返还给 「**Request**」客户端。对应的方法是「**Processor 线程**」的「**processNewResponses**」方法。

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

....

}

} catch {

....

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

从这段代码得出，其中最核心的逻辑是「**SendResponse**」方法来执行「**Response**」发送。其底层使用 「**Selector**」实现真正的发送逻辑。

实际上，在这步骤执行完成后，「**Processor 线程**」通常还会尝试执行「**Response**」的回调逻辑，对应的方法是即 「**Processor 线程**」类的「**processCompletedSends**」方法。不过**并不是所有的**「**Request**」或者「**Response**」 **都会执行回调逻辑**。事实上，只有很少的 「**Response**」才会执行回调逻辑。比如「**FETCH**」请求在发送 Response 之后，就要求更新下 Broker 端与消息格式转换操作相关的统计指标等。

至此，一个请求被 Kafka 服务端网络层完整处理流程就带大家剖析完了，请大家对照篇首的架构图，再根据之前几篇的深度源码剖析文章进行研究后，我相信大家都可以真正掌握 Kafka 服务端网络层处理请求的全流程。

## **03 Kafka 服务端网络通信架构支撑百万并发奥秘**

通过篇首的服务端网络通信架构图中，其实整个服务端网络架构分为 **4** 个层次。

1.  由 Acceptor 线程构成的负责创建和客户端的连接。
2.  由 Processor 线程构成的负责的网络事件处理。
3.  由 RequestChannel 构成的负责构建请求、响应的缓冲层。
4.  由 KafkaRequestHandler 以及 KafkaApis 构成的负责真正的业务处理。

这里，我们来思考一个问题：**Kafka 为什么要把**「**Acceptor 线程**」、「**Processor 线程**」分开呢？

我们来尝试分析下，**如果不分开，对于 Kafka 这种为大数据而生的流处理的消息系统，****如果有海量网络读写请求过来后，势必会造成大量线程的阻塞，进而造成服务端对 OP\_ACCEPT 事件的响应不及时，最终导致连接失败**。与此同时，**如果服务端刚启动就来了很多连接，大量的线程都会去建立新的连接，那么网络读写事件的处理就会慢很多，也会造成读写事件超时等问题**。

「**Acceptor 线程**」、「**Processor 线程**」分层设计的目的就是**要让**「**连接创建**」与 「**网络读写事件处理**」分开，同时支持我们动态配置「**Processor 线程**」的数量，对应的配置参数为 num.network.threads， 默认为 3。这样做不会**被极端场景影响到整体响应时间，同时也符合 Reactor 设计模式**。

![](https://article-images.zsxq.com/FufQfDz3XSjMn478yD44WIusVQYJ)

接下来，我们来分析下，「**Processor 线程**」处理完请求后并没有**直接交给**「**KafkaRequestHandler**」业务线程处理，而是先放到「**RequestChannel 请求通道**」的「**RequestQueue**」请求队列里，这样做的好处是**避免在高并发场景下**「**KafkaRequestHandler**」**业务线程工作过于饱和而造成处理超时**。

接下来，「**KafkaRequestHandlerPool**」线程池会消费「**RequestChannel 请求通道**」的「**RequestQueue**」请求队列里的请求，然后通过调用「**KafkApis**」的「**handle()**」方法实现真正的业务逻辑处理。这样做的好处是**既可以实现**「**网络读写请求处理**」**与**「**调用底层工具组件**」**的解耦**，**还可以让我们可以根据实际请求随时调整**「**KafkaRequestHandlerPool**」**线程池的线程数，进而提升调用底层组件的能力**，对应的配置参数为 num.io.threads， 默认为 8。

![](https://article-images.zsxq.com/Fmg_6EKo6pwGnSEPF7orcAdWWlm1)

最后，「**KafkApis**」类会把响应「**Response**」放入对应的「**Processor 线程**」里的响应队列 「**ResponseQueue**」里，而不是直接让「**Processor 线程**」把响应「**Response**」发送给客户端，这样做的好处是**实现**「**业务线程**」**与**「**网络处理线程**」**的解耦**，**避免了在高并发场景下业务线程工作过于饱和而造成延迟问题**。

其实，Kafka 底层的很多设计都非常地经典，这样一款经受顶尖大厂的磨练出来的工业级软件，很值得我们深入学习与研究。

再次，致敬 Kafka 社区给我们带来的这样一款「**高并发**」、「**高性能**」、「**高可靠**」软件。