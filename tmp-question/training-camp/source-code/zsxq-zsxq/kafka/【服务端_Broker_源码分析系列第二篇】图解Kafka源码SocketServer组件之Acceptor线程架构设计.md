大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**Kafka Reactor 网络模型架构设计**」以及「**SocketServer 组件整体设计和源码实现**」，今天我们就来深度聊聊「**SocketServer 组件之 Acceptor 线程架构设计与源码实现**」，看看 Kafka 服务端是如何实现超高并发的网络连接的。

![](https://article-images.zsxq.com/FtPaqyAhR9P9jcAgnGPJ2q7WzRUS)

## **01 总体概述**

最近这几篇文章都是基于下面这张图来展开的，这是 Kafka 服务端超高并发的网络通信架构，如下图所示：

![](https://article-images.zsxq.com/lojiZJNqUko4zFwqFJ8m4NNg9614)

在上一篇中，我们从整体上带你分析了 SocketServer 组件类的组成以及数据面、控制面下的「**Acceptor 线程**」、「**Processor 线程**」、「**RequestChannel 队列**」的创建与启动流程。

在经典的 Reactor 网络模型中有个 「**Dispatcher**」角色，主要是用来接收外部请求，并分发给后面的实际处理线程，在 Kafka 中，这个「**Dispatcher**」就是「**Acceptor 线程**」。

通过上图我们可以得到，「**Acceptor 线程**」类主要是**负责接收外部请求并与外部建立网络连接。**今天这篇我们的主角是「**Acceptor 线程**」，带大家分析「**Acceptor 线程**」的源码实现。

为了方便大家理解，所有的源码只保留骨干。

##   
**02 Acceptor 线程类**

![](https://article-images.zsxq.com/FjDPliqAjgA_EEcQIgJBTpo4XIyG)

我们先来拆解下上面那张 Kafka 超高并发网络架构图，这里只保留「**Acceptor 线程**」部分，从图中，我们可以得出，「**Acceptor 线程类**」会单独构建一个 「**ServerSocketChannel**」和 一个「**Selector**」 处理外部请求数据，并且会构建一个线程来监听 「**OP\_ACCEPT 事件**」。

Acceptor 线程类是 SocketServer 组件类的一部分，源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/SocketServer.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/SocketServer.scala)

这里我先来看看 Acceptor 这个类的重要字段和参数。

##   
**2.1 参数与重要字段**

/\*\*

\* Thread that accepts and configures new connections. There is one of these per endpoint.

\*/

private\[kafka\] class Acceptor(val endPoint: EndPoint, // Kafka Broker 连接信息

val sendBufferSize: Int, //出站网络I/O底层缓冲区大小，默认100k

val recvBufferSize: Int, //入站网络I/O底层缓冲区大小,默认100k

brokerId: Int, // BrokerID

connectionQuotas: ConnectionQuotas, // 连接配额

metricPrefix: String) extends AbstractServerThread(connectionQuotas) with KafkaMetricsGroup {

// 创建底层的 NIO Seletor 对象，负责执行底层实际 I/O 事件，比如：监听连接创建请求、读写请求等，注意这里不是 KSelector 对象。

private val nioSelector \= NSelector.open()

// Broker 端创建对应的 ServerSocketChannel 实例, 在后面会将该 Channel 往上面的 Seletor 进行注册

val serverChannel \= openServerSocket(endPoint.host, endPoint.port)

// 创建该 Acceptor 线程对应的 Processor 线程池。

private val processors \= new ArrayBuffer\[Processor\]()

private val processorsStarted \= new AtomicBoolean

private val blockedPercentMeter \= newMeter(s"${metricPrefix}AcceptorBlockedPercent",

"blocked time", TimeUnit.NANOSECONDS, Map(ListenerMetricTag -> endPoint.listenerName.value))

/\*\*

\* Create a server socket to listen for connections on.

\*/

private def openServerSocket(host: String, port: Int): ServerSocketChannel = {

val socketAddress \=

if (host == null || host.trim.isEmpty)

new InetSocketAddress(port)

else

new InetSocketAddress(host, port)

// 打开一个 serverChannel 实例

val serverChannel \= ServerSocketChannel.open()

// 设置非阻塞

serverChannel.configureBlocking(false)

if (recvBufferSize != Selectable.USE\_DEFAULT\_BUFFER\_SIZE)

serverChannel.socket().setReceiveBufferSize(recvBufferSize)

try {

// 绑定监听地址

serverChannel.socket.bind(socketAddress)

info(s"Awaiting socket connections on ${socketAddress.getHostString}:${serverChannel.socket.getLocalPort}.")

} catch {

case e: SocketException =>

throw new KafkaException(s"Socket server failed to bind to ${socketAddress.getHostString}:$port: ${e.getMessage}.", e)

}

// 返回 serverChannel

serverChannel

}

从上面代码可以看出，「**Acceptor 线程类**」继承了抽象类 「**AbstractServerThread**」，AbstractServerThread抽象类封装了一些为线程服务的公共方法和参数，例如：线程启动、停止操作要做的额外工作等。

该类定义的参数有以下几个：

1.  **endPoint**：表示 Kafka Broker 的连接信息，比如：PLAINTEXT://localhost:9092。Aceeptor 线程需要用到 endpoint 中的主机名和端口来创建 ServerSocketChannel 实例。
2.  **sendBufferSize**：表示出站网络I/O底层缓冲区大小，该值默认是 Broker 端参数 [socket.send.buffer.bytes](http://socket.send.buffer.bytes/) 值，默认 100 KB。
3.  **recvBufferSize**：表示入站网络I/O底层缓冲区大小，该值默认是 Broker 端参数 [socket.receive.buffer.bytes](http://socket.send.buffer.bytes/) 值，默认 100 KB。
4.  **brokerId**：表示所在的 Broker id。
5.  **connectionQuotas**：连接配额，为了保证已成功连接可以正常工作，要通过连接配额控制连接的数量。

该类重要的字段有以下几个：

1.  **nioSeletor**：它是 Acceptor 线程类的 Java nio selector 对象，它是后续所有网络通信组件实现 Java NIO 机制的基础，一个 Acceptor 线程类都有一个 selector 对象。
2.  **serverChannel**：它是 Broker 端创建对应的 Java nio ServerSocketChannel 类对象实例，在后面会将该 Channel 往上面的 Seletor 进行注册。
3.  **processors**：它是网络 Processor 线程池，Acceptor 线程在初始化时，需要创建对应的网络 Processor 线程池，即一个 Acceptor 类对象都有一个 Processor 线程池，当 Acceptor 类建立好一个网络连接后，会从 Processor 线程池里取出一个 Processor 线程类处理这个网络连接上的网络操作，因此，Processor 线程是在 Acceptor 线程中管理和维护的。

介绍完类参数和重要字段后，我们来看看 「**Acceptor 线程类**」的重要方法。

## **2.2 accept()**

先来看看建立连接的方法，它主要用来**接收请求并建立连接**，**然后返回对应的 SocketChannel**，即连接。

/\*\*

\* Accept a new connection

\*/

private def accept(key: SelectionKey): Option\[SocketChannel\] = {

// 创建 serverSocketChannel 实例

val serverSocketChannel \= key.channel().asInstanceOf\[ServerSocketChannel\]

// 1、建立客户端与服务端的连接

val socketChannel \= serverSocketChannel.accept()

try {

// 2、连接配额统计

connectionQuotas.inc(endPoint.listenerName, socketChannel.socket.getInetAddress, blockedPercentMeter)

// 3、配置相关属性，设置非阻塞、设置 TCP\_NODELAY、设置 keepAlive、设置 sendBufferSize 大小。

socketChannel.configureBlocking(false)

socketChannel.socket().setTcpNoDelay(true)

socketChannel.socket().setKeepAlive(true)

if (sendBufferSize != Selectable.USE\_DEFAULT\_BUFFER\_SIZE)

socketChannel.socket().setSendBufferSize(sendBufferSize)

// 4、返回 socketChannel

Some(socketChannel)

} catch {

case e: TooManyConnectionsException =>

info(s"Rejected connection from ${e.ip}, address already has the configured maximum of ${e.count} connections.")

close(endPoint.listenerName, socketChannel)

None

}

}

该方法相对比较简单，大概**做了以下 4 件事情**：

1.  从 SelectionKey 类对象参数 key 中获取 serverSocketChannel，然后通过 serverSocketChannel 来建立客户端与服务端的连接 socketChannel。
2.  连接数加一，更新连接配额统计。
3.  配置相关属性，设置非阻塞、设置 TCP\_NODELAY、设置 keepAlive、设置 sendBufferSize 大小。
4.  返回 socketChannel。

接下来，我们来深度剖析下 「**Acceptor 线程类**」的核心方法。

## **2.3 run()**

/\*\*

\* Accept loop that checks for new connection attempts

\*/

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

// Assign the channel to the next processor (using round-robin) to which the

// channel can be added without blocking. If newConnections queue is full on

// all processors, block until the last one is able to accept a connection.

// 获取 processors 线程池的锁，保证多线程并发访问的线程安全

var retriesLeft \= synchronized(processors.length)

var processor: Processor = null

do {

retriesLeft -= 1

// 7、轮询选择哪个 Processor 线程进行处理

processor = synchronized {

// adjust the index (if necessary) and retrieve the processor atomically for

// correct behaviour in case the number of processors is reduced dynamically

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

} catch {

case e: Throwable => error("Error while accepting connection", e)

}

}

}

}

catch {

// We catch all the throwables to prevent the acceptor thread from exiting on exceptions due

// to a select operation on a specific channel or a bad request. We don't want

// the broker to stop responding to requests from other clients in these scenarios.

case e: ControlThrowable => throw e

case e: Throwable => error("Error occurred", e)

}

}

} finally {

// 开始执行各种资源关闭逻辑

debug("Closing server socket and selector.")

// 关闭 serverChannel 连接

CoreUtils.swallow(serverChannel.close(), this, Level.ERROR)

// 关闭 serverChannel 连接对应的 Selector

CoreUtils.swallow(nioSelector.close(), this, Level.ERROR)

// 关闭 Acceptor 线程完成，并唤醒对应的执行关闭被阻塞的线程

shutdownComplete()

}

}

// Processor 线程与对方建立连接

private def assignNewConnection(socketChannel: SocketChannel, processor: Processor, mayBlock: Boolean): Boolean = {

if (processor.accept(socketChannel, mayBlock, blockedPercentMeter)) {

debug(s"Accepted connection from ${socketChannel.socket.getRemoteSocketAddress} on" +

s" ${socketChannel.socket.getLocalSocketAddress} and assigned it to processor ${processor.id}," +

s" sendBufferSize \[actual|requested\]: \[${socketChannel.socket.getSendBufferSize}|$sendBufferSize\]" +

s" recvBufferSize \[actual|requested\]: \[${socketChannel.socket.getReceiveBufferSize}|$recvBufferSize\]")

true

} else

false

}

/\*\*

\* 该类主要用于管理 Kafka 中的服务器线程，包含线程的启动和关闭。线程的启动和关闭都受计数器的控制， \* 可以保证线程的正常启动和关闭。同时，该类还包含一个连接配额对象，用于管理连接的配额，可以有效地控

\* 制连接的数量，避免资源浪费。

\*/

private\[kafka\] abstract class AbstractServerThread(connectionQuotas: ConnectionQuotas) extends Runnable with Logging {

private val startupLatch \= new CountDownLatch(1)

// \`shutdown()\` is invoked before \`startupComplete\` and \`shutdownComplete\` if an exception is thrown in the constructor

// (e.g. if the address is already in use). We want \`shutdown\` to proceed in such cases, so we first assign an open

// latch and then replace it in \`startupComplete()\`.

@volatile private var shutdownLatch \= new CountDownLatch(0)

/\*\*

\* Record that the thread startup is complete

\* 记录线程启动完成。将 open 状态的关闭计数器 shutdownLatch 替换成一个 closed 状态的计数器，并且减少启动计数器 startupLatch 的计数器。

\*/

protected def startupComplete(): Unit = {

// Replace the open latch with a closed one

shutdownLatch = new CountDownLatch(1)

startupLatch.countDown()

}

/\*\*

\* Record that the thread shutdown is complete

\* 记录线程关闭完成。减少关闭计数器 shutdownLatch 的计数器。

\*/

protected def shutdownComplete(): Unit = shutdownLatch.countDown()

}

它是处理 Reactor 模式中分发逻辑的主要实现方法，相对比较复杂些，这里我给大家梳理下运行时步骤：

1.  向 nioSelector 注册 serverChannel，并监听 serverChannel 上的 OP\_ACCEPT 事件，等待客户端的请求。
2.  启动 Acceptor 线程完成，并唤醒阻塞的线程。
3.  记录当前使用的 Processor 序号，从0开始，最大值是 num.network.threads - 1。
4.  进入while(true)轮询代码，调用 Selector 的 select() 方法，监听是否有网络连接事件。每隔 500 毫秒轮询获取一次网络就绪 I/O 事件。收到监听事件后把 key 拿出来，然后判断是否是 Accept 事件，只有 Accept 事件才会处理。
5.  获取所有监听到的注册的key。
6.  判断是否是 Accept 事件，如果是 Accept 事件就调用 accept() 方法，建立连接并开始轮询处理 socketChannel。
7.  从 Processors 线程池中轮询选择出一个 Processor 线程。这样能够把任务平均分配到 Processors 线程池内 Processor 线程里。这样能确保某个 Processor 线程的负载均衡。
8.  取出后将当前 Processor 索引加一。这样下次分配的 Processor 线程就是 Processors 线程池中下一个 Processor 线程。
9.  将新连接的 SocketChannel 分配给不同的 Processor 线程，然后建立连接，保证请求处理的负载均衡。

![](https://article-images.zsxq.com/FuPNmbc-BSxSlDSfORhrpcbiPB4e)

通过上面的源码剖析，我们可以得出，在「**Acceptor 线程**」中会维护和管理 「**Processor 线程**」，最后我们来看看关于「**Acceptor 线程**」中 Processor 的方法，相对都比较简单，我们来看下。

##   
**2.4 addProcessors()**

private\[network\] def addProcessors(newProcessors: Buffer\[Processor\], processorThreadPrefix: String): Unit = synchronized {

// 添加一组新的 Processor 线程

processors ++= newProcessors

// 如果 Processor 线程池已启动

if (processorsStarted.get)

// 启动新的 Processor 线程

startProcessors(newProcessors, processorThreadPrefix)

}

## **2.5 startProcessors()**

private def startProcessors(processors: Seq\[Processor\], processorThreadPrefix: String): Unit = synchronized {

// 轮询依次创建并启动 Processor 线程

processors.foreach { processor =>

// 线程命名规范: processor 线程前缀-kafka-network-thread-brokerId-监听器名称-安全协议-Processor序号

// 这里举例，如果序号为 0 的 Broker 设置 PLAINTEXT://192.168.56.1:9092 作为连接信息，那么3个Processor 线程名称分别为：

// data-plane-kafka-network-thread-0-ListenerName(PLAINTEXT)-PLAINTEXT-0

// data-plane-kafka-network-thread-0-ListenerName(PLAINTEXT)-PLAINTEXT-1

// data-plane-kafka-network-thread-0-ListenerName(PLAINTEXT)-PLAINTEXT-2

KafkaThread.nonDaemon(

s"${processorThreadPrefix}-kafka-network-thread-$brokerId-${endPoint.listenerName}-${endPoint.securityProtocol}-${processor.id}",

processor

).start()

}

}

private\[network\] def startProcessors(processorThreadPrefix: String): Unit = synchronized {

// 如果 Processor 线程池未启动

if (!processorsStarted.getAndSet(true)) {

// 启动给定的 Processor 线程

startProcessors(processors, processorThreadPrefix)

}

}

添加和启动线程流程如下图所示：

![](https://article-images.zsxq.com/FvE-9aU34-dq8fhq3F8Wl99EpQhP)

##   
**2.6 removeProcessors()**

private\[network\] def removeProcessors(removeCount: Int, requestChannel: RequestChannel): Unit = synchronized {

// Shutdown \`removeCount\` processors. Remove them from the processor list first so that no more

// connections are assigned. Shutdown the removed processors, closing the selector and its connections.

// The processors are then removed from \`requestChannel\` and any pending responses to these processors are dropped.

// 获取 Processor 线程池中最新的 removeCount 个线程

val toRemove \= processors.takeRight(removeCount)

// 移除这最新的 removeCount 个线程

processors.remove(processors.size - removeCount, removeCount)

// 关闭这些线程

toRemove.foreach(\_.initiateShutdown())

toRemove.foreach(\_.awaitShutdown())

// 从 RequestChannel 队列中移除这些 Processor 线程

toRemove.foreach(processor => requestChannel.removeProcessor(processor.id))

}

有了这 3 个方法后， 对于 「**Acceptor 线程**」来说，就具备了基本的管理 「**Processor 线程池**」的能力了。

## **03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过剖析 Kafka 网络通信架构设计，引出 Acceptor 线程，即经典 Reactor 模型中的 「**Dispatcher**」角色。

2、带你深度剖析了 SocketServer 组件类之一 Acceptor 线程的源码实现，它主要使用 Java NIO 的 Selector + SocketChannel 的方式轮询处理就绪的 I/O 事件，这里只监听 「**OP\_ACCEPT 事件**」，一旦收到外部连接请求，Acceptor 线程就会指定一个 Processor 线程来处理，让它创建真正的网络连接。

下篇我们来深度剖析「**SocketServer 组件类之 Processor 线程架构设计**」，大家期待，我们下期见。