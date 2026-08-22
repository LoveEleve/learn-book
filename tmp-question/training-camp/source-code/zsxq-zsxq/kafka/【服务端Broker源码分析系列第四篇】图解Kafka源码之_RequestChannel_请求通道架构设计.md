大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**SocketServer 组件之 Processor 线程架构设计与源码实现**」，今天我们就来深度聊聊「**RequestChannel 请求通道架构设计与源码实现**」，看看 Kafka 服务端是如何接收Processor 线程类处理的请求对象，同时接收响应的。

![](https://article-images.zsxq.com/FgXP_SZr1AmJ0AcfWyIw4e6Uz5MG)

## **01 总体概述**

最近这几篇文章都是基于下面这张图来展开的，这是 Kafka 服务端超高并发的网络通信架构，如下图所示：

![](https://article-images.zsxq.com/lojiZJNqUko4zFwqFJ8m4NNg9614)

在上一篇中，我们从整体上带你分析了「**Processor 线程类**」的架构设计，它主要用来**真正创建网络连接以及分发网络读写请求，最后关闭连接资源，**今天这篇我们的主角是「**RequestChannel**」，主要带大家分析「**RequestChannel**」请求通道的源码实现。

为了方便大家理解，所有的源码只保留骨干。

##   
**02 RequestChannel 类**

在 Kafka 的网络通信层中，RequestChannel 为 「**Processor 网络线程池**」与 「**KafkaRequestHandler 业务线程池**」之间的数据交换提供了一个「**数据缓冲区**」，是通信过程中 Request 和 Response 缓存的地方。因此它在**网络通信中起到了一个数据缓冲队列的作用**。

「**Processor 线程**」将读取到的请求添加至 「**RequestChannel**」请求通道的全局请求队列「**RequestQueue**」中，「**KafkaRequestHandler 业务线程**」从请求队列「**RequestQueue**」中获取并处理，处理完以后将 Response 添加至「**RequestChannel**」请求通道的响应队列 「**ResponseQueue**」中，并通过 「**responseListeners**」唤醒对应的 Processor 线程，最后 Processor 线程从响应队列中取出后发送至客户端，下面我来带你深度剖析下。

RequestChannel 类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/RequestChannel.sca](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/RequestChannel.scala)[la](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/SocketServer.scala)

这里我先来看看 RequestChannel 这个类的重要字段和参数。

##   
**2.1 参数与重要字段**

class RequestChannel(val queueSize: Int, // 请求队列的大小

val metricNamePrefix : String, // 度量指标的名称前缀

time: Time) extends KafkaMetricsGroup {

import RequestChannel.\_

// 管理请求通道度量指标的对象

val metrics \= new RequestChannel.Metrics

// 请求队列 queueSize为队列大小，默认500

private val requestQueue \= new ArrayBlockingQueue\[BaseRequest\](queueSize)

// RequestChannel 下的 Processor 线程池。默认 3 个线程，通过 num.network.threads 配置。

private val processors \= new ConcurrentHashMap\[Int, Processor\]()

.....

我们来看看 「**RequestChannel 请求通道**」初始化时都做了哪些事情：

1.  **requestQueue**: 即请求队列，它是个 ArrayBlockingQueue 阻塞队列，其元素是 Request 对象，队列长度大小为500。其作用是 Processor 线程类接收到 Request 请求后会把请求放入该队列里。下一篇要讲的 KafkaRequestHandler 线程类会消费队列中的元素进行真正的业务处理。

对应 core/src/main/scala/kafka/server/KafkaConfig.scala 中的

val QueuedMaxRequests \= 500

.define(QueuedMaxRequestsProp, INT, Defaults.QueuedMaxRequests, atLeast(1), HIGH, QueuedMaxRequestsDoc)。

1.  **processors**: RequestChannel 请求通道下的 Processor 线程池，它是个 ConcurrentHashMap 类型集合，默认3 个线程，Kafka 允许你动态地修改此参数值。Broker 启动时指定 num.network.threads 为 3，之后你可以通过 kafka-configs 命令进行修改。

介绍完类参数和重要字段后，我们来看看「**RequestChannel 请求通道类**」的重要方法，下面带你挨个深度剖析。

首先，来看看「**RequestChannel 请求通道类**」是如何将 Processor 对象添加到 「**Processor 线程池**」中的，对应的方法是 addProcessor()。

## **2.2 addProcessor()**

def addProcessor(processor: Processor): Unit = {

// 1、添加 Processor 到 Processor 线程池

if (processors.putIfAbsent(processor.id, processor) != null)

warn(s"Unexpected processor with processorId ${processor.id}")

// 2、为给定 Processor 对象创建对应的监控指标

newGauge(responseQueueSizeMetricName, () => processor.responseQueueSize,

Map(ProcessorMetricTag -> processor.id.toString))

}

该方法非常简单，主要做了 2 件事情：

1.  首先使用了 ConcurrentHashMap 集合的 putIfAbsent 方法向「**Processor 线程池**」中添加一个 Processor。putIfAbsent方法会尝试将指定的 Processor.id 和 processor组成的键值对添加到Map中。**如果该键已经存在，则方法会返回已存在的值，如果不存在，则会将该键值对添加到 Map 中，并且返回 null**。通过返回值是否为null，可以判断 Processor 是否被成功添加到「**Processor 线程池**」中。
2.  在 Processor 被添加到「**Processor 线程池**」中后，使用 newGauge 方法来监控该 Processor 的 responseQueueSize 度量值。responseQueueSize 表示该 Processor 中请求处理的响应队列大小，是 Processor 的一个重要指标。对于每个Processor 都会生成一个度量值，并将其与一个标签 ProcessorMetricTag (处理器标签) 以及该 [processor.id](http://processor.id/) 进行关联，这样就可以在后续进行网络连接监控时，方便的根据 [processor.id](http://processor.id/) 或标签进行过滤和统计操作。

接下来，我们来看看「**RequestChannel 请求通道类**」是如何将 Processor 对象从 「**Processor 线程池**」中移除的，对应的方法是 removeProcessor()。

## **2.3 removeProcessor()**

def removeProcessor(processorId: Int): Unit = {

// 1、将 Processor 从 Processor 线程池中进行移除

processors.remove(processorId)

// 2、移除给定 Processor 对象的监控指标

removeMetric(responseQueueSizeMetricName, Map(ProcessorMetricTag -> processorId.toString))

}

该方法也非常简单，主要做了 2 件事情：

1.  将 processorId 的处理器从「**Processor 线程池**」中移除。
2.  删除与 processor 相关的度量值 metrics。删除后该 processor 就无法接受和处理请求，以便在后续的网络监控过程中避免出现垃圾数据。

接下来，我们来看看「**RequestChannel 请求通道类**」关于 Request 相关的方法。

## **2.4 sendRequest()**

/\*\* Send a request to be handled, potentially blocking until there is room in the queue for the request \*/

def sendRequest(request: RequestChannel.Request): Unit = {

// 将 request 请求添加到 requestQueue 队列中

requestQueue.put(request)

}

该方法很重要，主要用来**暂存请求的，即** Processor 线程接收到请求后，会调用该方法把请求 request 添加到「**RequestQueue**」阻塞队列里。等待 「**KafkaRequestHandler 业务线程**」来获取请求并处理。

## **2.5 receiveRequest()**

/\*\* Get the next request or block until specified time has elapsed \*/

def receiveRequest(timeout: Long): RequestChannel.BaseRequest =

// requestQueue.poll 方法会阻塞等待一段指定的时间，如果在等待指定时间内都没有请求，则返回 null。因此，poll 方法可以用于等待一定的时间内有请求到来，如果时间到了还没有请求到来，则继续执行后续操作。这个方法实现了超时功能。

requestQueue.poll(timeout, TimeUnit.MILLISECONDS)

/\*\* Get the next request or block until there is one \*/

def receiveRequest(): RequestChannel.BaseRequest =

// requestQueue.take 方法会一直阻塞等待下一个请求，即使等待很久也不会返回 null。这个方法会一直等到有请求到来为止，并且立即返回请求。这个方法不会实现超时等待的功能。

requestQueue.take()

这 2 个方法也非常简单，主要用来等待「**KafkaRequestHandler 业务线程**」调用此方法从请求队列 「**RequestQueue**」取出一个 request 对象，再根据 request 对象调用底层对应的 api。

不过这 2 个方法，实现上有一些不同：

1.  receiveRequest(timeout: Long) 带一个超时参数 timeout，用于**在等待指定时间后获取下一个请求**。如果在指定时间内没有请求 request 到来，则会返回 null。因此 [requestQueue.poll](http://requestqueue.poll/) 方法可以用于等待一定的时间内有请求 request 到来，如果时间到了还没有请求 request 到来，则继续执行后续操作。**该方法实现了超时功能**。
2.  receiveRequest() 没有超时参数，会一直阻塞等待下一个请求队列中的请求。当有请求到来时阻塞会解除并立即返回这个请求 request，将其从「**RequestQueue**」中移除。

最后，我们来看看发送响应到客户端的方法。

## **2.6 sendResponse()**

/\*\* Send a response back to the socket server to be sent over the network \*/

def sendResponse(response: RequestChannel.Response): Unit = {

// 1、如果执行了跟踪（trace）操作，将会打印出消息发送的详情

if (isTraceEnabled) {

// 获取请求头

val requestHeader \= response.request.header

// 匹配 response 结果类型，根据不同类型进行处理

val message \= response match {

// 发送消息成功，将消息大小、客户端 ID 和 API 名称等信息打印出来

case sendResponse: SendResponse =>

s"Sending ${requestHeader.apiKey} response to client ${requestHeader.clientId} of ${sendResponse.responseSend.size} bytes."

// 不需要发送响应消息，因此不做任何处理。

case \_: NoOpResponse =>

s"Not sending ${requestHeader.apiKey} response to client ${requestHeader.clientId} as it's not required."

// API 执行错误，关闭客户端连接

case \_: CloseConnectionResponse =>

s"Closing connection for client ${requestHeader.clientId} due to error during ${requestHeader.apiKey}."

// 开始 API 请求限流时，不需要更新指标（metrics）

case \_: StartThrottlingResponse =>

s"Notifying channel throttling has started for client ${requestHeader.clientId} for ${requestHeader.apiKey}"

// 结束 API 请求限流时，不需要更新指标（metrics）

case \_: EndThrottlingResponse =>

s"Notifying channel throttling has ended for client ${requestHeader.clientId} for ${requestHeader.apiKey}"

}

// 记录 trace 日志

trace(message)

}

// 2、匹配 response 结果类型，根据不同类型进行处理

response match {

// We should only send one of the following per request

// 如果是发送响应 | 无响应 | 关闭连接响应

case \_: SendResponse | \_: NoOpResponse | \_: CloseConnectionResponse =>

val request \= response.request

val timeNanos \= time.nanoseconds()

// 计算响应完成时间

request.responseCompleteTimeNanos = timeNanos

if (request.apiLocalCompleteTimeNanos == -1L)

// 计算 apiLocal 完成时间

request.apiLocalCompleteTimeNanos = timeNanos

// For a given request, these may happen in addition to one in the previous section, skip updating the metrics

case \_: StartThrottlingResponse | \_: EndThrottlingResponse => ()

}

// 3、找出 response 对应的 Processor 线程，即 request 当初是由哪个 Processor 线程处理，请求和响应对应的是同一个 Processor。

val processor \= processors.get(response.processor)

// The processor may be null if it was shutdown. In this case, the connections

// are closed, so the response is dropped.

if (processor != null) {

// 4、将 response 对象添加到对应 Processor 线程的 response 队列中

processor.enqueueResponse(response)

}

}

// Processor 线程类的方法，就是添加响应到 ResponseQueue 队列中，并唤醒正在等待响应的处理线程。

private\[network\] def enqueueResponse(response: RequestChannel.Response): Unit = {

responseQueue.put(response)

wakeup()

}

该方法是最复杂的，主要用来等待「**KafkaRequestHandler 业务线程**」处理完 request 对象后，会封装 response, 然后调用此方法把 response 添加到对应的 Processor 线程的 response 队列中完成响应，主要做了以下 4 件事情：

1.  如果执行了跟踪（trace）操作，将会打印出消息发送的详情 。
2.  匹配 response 结果类型，根据不同类型进行计算完成时间。
3.  这是**重要步骤**：找出 response 对应的 Processor 线程，这个 Processor 线程就是接收对应 request 对象的，即 request 当初是由哪个 Processor 线程处理，请求和响应对应的是同一个 Processor，所以最后返回客户端还是由这个 Processor 来处理。
4.  将 response 对象添加到对应 Processor 线程的 response 队列中 。

至此，整个「**RequestChannel 请求通道类**」的方法就带大家剖析完毕了，相对都比较简单，最后通过一张图来说明下整个流程：

![](https://article-images.zsxq.com/lgO4TPo1N7ozPCHDwuU7eoEeNWkY)

## **03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过剖析 Kafka 网络通信架构设计，引出 RequetChannel 请求通道。

2、带你深度剖析了 RequetChannel 请求通道的源码实现，它主要为 「**Processor 网络线程池**」与 「**KafkaRequestHandler 业务线程池**」之间的数据交换提供了一个「**数据缓冲区**」，是通信过程中 Request 和 Response 缓存的地方。

下篇我们来深度剖析「**SocketServer 组件类之 KafkaRequestHandler 业务线程池架构设计**」，大家期待，我们下期见。