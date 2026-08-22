大家好，我是 **华仔**, 又跟大家见面了。

上一篇中，主要带大家深度剖析了 「**RequestChannel 请求通道架构设计与源码实现**」，今天我们就来深度聊聊「**KafkaRequestHandler I/O 线程池架构设计与源码实现**」，看看 Kafka 服务端是如何对网络请求进行业务处理并返回响应数据的。

![](https://article-images.zsxq.com/Fhb0fSyl8ayXH-IB5S_n3hzd4Sus)

## **01 总体概述**

最近这几篇文章都是基于下面这张图来展开的，这是 Kafka 服务端超高并发的网络通信架构，如下图所示：

![](https://article-images.zsxq.com/lojiZJNqUko4zFwqFJ8m4NNg9614)

在上一篇中，我们从整体上带你分析了「**RequestChannel**」请求通道的架构设计和源码实现，它主要用来为 「**Processor 网络线程池**」与 「**KafkaRequestHandler 业务线程池**」之间的数据交换提供「**数据缓冲区**」的，今天这篇我们的主角是「**KafkaRequestHandler**」和 「**KafkaRequestHandlerPool**」，带大家分析「**KafkaRequestHandler 业务线程**」和 「**KafkaRequestHandlerPool 业务线程池**」的架构设计和源码实现。

为了方便大家理解，所有的源码只保留骨干。

## **02 KafkaRequestHandler 类**

在 Kafka 的网络通信层中，「**Acceptor 线程**」主要负责建立网络连接，「**Processor 网路线程池**」主要负责网络读写，「**RequestChannel**」主要负责为 「**Processor 网络线程池**」与 「**KafkaRequestHandler 业务线程池**」之间的数据交换提供「**数据缓冲区**」，那么 「**Acceptor 线程**」、「**Processor 网路线程池**」 还有「**RequestChannel**」等都不做请求处理， 它们只是请求和响应的「**搬运工**」，真正负责业务读写请求处理的是「**KafkaRequestHandler**」和 「**KafkaRequestHandlerPool**」。

KafkaRequestHandler 类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaRequestHandler.sca](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/KafkaRequestHandler.scala)[la](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/network/SocketServer.scala)

这里我先来看看 KafkaRequestHandler 这个类的重要字段和参数。

## **2.1 参数与重要字段**

// I/O 业务线程类

class KafkaRequestHandler(id: Int, // IO 线程序号

brokerId: Int, // Broker 对应序号

val aggregateIdleMeter: Meter,

val totalHandlerThreads: AtomicInteger,// I/O 线程池的大小

val requestChannel: RequestChannel, // 请求处理通道

apis: ApiRequestHandler, // KafkaApis 类，用来实现真正请求处理逻辑

time: Time) extends Runnable with Logging {

this.logIdent = "\[Kafka Request Handler " + id + " on Broker " + brokerId + "\], "

// 关闭线程操作完成

private val shutdownComplete \= new CountDownLatch(1)

// 线程是否已关闭

@volatile private var stopped \= false

从上面类定义得出，「**KafkaRequestHandler**」是一个 Runnable 对象，所以你可以把它当作是个线程。每个实例都有以下 7 个重要属性字段：

1.  **id**: 即请求处理线程的序号，用来标识是线程池中的哪个线程。
2.  **brokerId**: 即 Broker 对应序号，用来标识这是哪个 Broker 上的请求处理线程。
3.  **totalHandlerThreads**: 即 I/O 线程池的大小，默认为 8 个， 通过 num.io.threads 来配置。
4.  **requestChannel**: 即 SocketServer 中的请求通道，通过上一篇的剖析，我们知道请求 request 是保存在「**RequestChannel 请求通道**」中的「**RequestQueue**」队列中的，那么 Kafka 在构造 KafkaRequestHandler 时，必须关联 SocketServer 组件中的 RequestChannel，即要让 I/O 线程能够找到请求被保存的地方。
5.  **apis**: 即 KafkaApis 类，如果说「**KafkaRequestHandler**」 是真正处理请求的，那么「**KafkaApis**」类就是真正执行请求处理逻辑的地方。
6.  **stopped**: 即 KafkaRequestHandler 线程关闭状态，默认为 false。
7.  **shutdownComplete**: 即 KafkaRequestHandler 线程关闭完成，使用 **CountDownLatch** 来实现线程同步。这个属性被用于等待 handler 线程关闭，当 handler 线程关闭时，会通过 **shutdownComplete** 来通知其他线程。具体来说，使用 **CountDownLatch(1)** 来初始化 shutdownComplete，**表示只有一个主线程等待 handler 线程**。当 handler 线程正常关闭时，会调用 **CountDown()** 函数来通知等待线程，**awaitShutdown()** 函数中就是在等待 **shutdownComplete** 的状态为已完成，如下图所示：

![](https://article-images.zsxq.com/FsXfQeuEyN0zr-34m33bV0qQY86B)

介绍完类参数和重要字段后，我们来看看「**KafkaRequestHandler 线程类**」的重要方法，总共 4 个方法，下面带你挨个深度剖析。

首先，来看看「**KafkaRequestHandler 线程类**」是如何处理「**RequestChannel 请求通道**」中的「**RequestQueue**」队列中的 request 的，对应的方法是 run()。

## **2.2 run()**

def run(): Unit = {

// 1、只要该线程尚未关闭就一直循环运行处理逻辑

while (!stopped) {

// 计算开始时间

val startSelectTime \= time.nanoseconds

// 2、每隔300ms从请求队列中获取下一个待处理的请求

val req \= requestChannel.receiveRequest(300)

// 计算结束时间

val endTime \= time.nanoseconds

// 3、统计线程空闲时间

val idleTime \= endTime - startSelectTime

// 4、更新线程空闲百分比指标

aggregateIdleMeter.mark(idleTime / totalHandlerThreads.get)

// 5、匹配 request 类型，分情况处理

req match {

// 关闭线程请求

case RequestChannel.ShutdownRequest =>

debug(s"Kafka request handler $id on broker $brokerId received shut down command")

// 关闭线程

shutdownComplete.countDown()

return

// 普通请求

case request: RequestChannel.Request =>

try {

// 更新请求移出队列的时间戳

request.requestDequeueTimeNanos = endTime

trace(s"Kafka request handler $id on broker $brokerId handling request $request")

// 调用 KafkaApis.handle 方法对 Request对象执行相应处理逻辑，apis 类实现了处理请求的逻辑，同时负责将响应写回对应的 RequestChannel.responseQueue 中，并唤醒 Processor 处理，这个下篇再进行剖析。

apis.handle(request)

} catch {

// 如果出现严重错误，立即关闭线程

case e: FatalExitError =>

shutdownComplete.countDown()

Exit.exit(e.statusCode)

// 如果是普通异常，记录错误日志

case e: Throwable => error("Exception when handling request", e)

} finally {

// 释放请求对象占用的内存缓冲区资源

request.releaseBuffer()

}

case null \=\> // continue

}

}

// 6、如果 stopped = true，即关闭状态，则关闭线程

shutdownComplete.countDown()

}

该方法相对比较简单，这里重点分析下步骤：

1.  只要该线程尚未关闭「**stopped = false**」，就一直循环运行处理逻辑。
2.  每隔300ms从请求队列中获取下一个待处理的请求。
3.  统计线程空闲时间。
4.  更新线程空闲百分比指标，如果本轮循环没取到，则结束循环，进入下一轮。
5.  **接下来是重点，**根据取到的 request 类型进行判断：
6.  如果请求类型是 ShutdownRequest 请求，则表示**该 Broker 发起了关闭操作，**就调用 shutdownComplete.countDown()关闭 KafkaRequestHandler 线程。
7.  如果请求类型是普通 Request 请求，先更新请求移出队列的时间戳，然后调用 KafkaApis.handler 方法执行实际请求处理逻辑。
8.  如果出现严重错误，立即关闭线程；如果是普通异常，记录错误日志；如果正常处理完成后，释放请求对象占用的内存缓冲区资源，然后进行下一轮循环。
9.  如果 stopped = true，即关闭状态，则关闭线程。

就这样会一直周而复始的执行上面的逻辑，下面通过一张图更形象的说明。

![](https://article-images.zsxq.com/lpLWx6SSv2SokB0Vqacs1E2UltGP)

接下来，我们来看看另外 3 个比较简单的方法。

## **2.3 stop()**

def stop(): Unit = {

// stopped 定义是通过「volatile」来修饰的，目的就是在多线程的场景下，当其发生变化的时候其他的线程都是可见的。

// 这里将 stopped 赋值为 true，表示要关闭 KafkaRequestHandler 线程。

stopped = true

}

该方法超级简单，就是将 「**stopped = true**」，表示要关闭 KafkaRequestHandler 线程，但它定义是 「**volatail**」，目的就是在多线程的场景下，当其发生变化的时候其他的线程都是可见的。

最后，我们来看看 2 个关闭方法，主要是在「**KafkaRequestHandlerPool**」中被调用。

## **2.4 initiateShutdown()**

// 向 requestChannel 发送关闭请求

def initiateShutdown(): Unit = requestChannel.sendShutdownRequest()

// core/src/main/scala/kafka/network/RequestChannel.scala

// 将 ShutdownRequest 请求添加到 requestQueue 请求队列中，即发送一个关闭请求

def sendShutdownRequest(): Unit = requestQueue.put(ShutdownRequest)

该方法超级简单，主要用来**向 requestChannel 发送关闭请求**，但是**这里有个串联关系，要结合上面刚刚的 run()，**我带你来分析下：

1.  首先如果 Broker 关闭线程操作的话，会调用「**KafkaRequestHandlerPool**」的 **shutdown()** 方法，这里只是简单提一下，让你了解，下一小节会剖析。再切回来，然后就会调用 **initiateShutdown()** 方法来关闭请求，而该方法内部是调用 「**RequestChannel**」的 **sendShutdownRequest()** 方法，它会将 ShutdownRequest 请求添加到「**requestQueue**」请求队列中，即发送一个关闭请求。
2.  一旦从「**requestQueue**」请求队列中获取到 ShutdownRequest，则 run() **会感知到**，就会调用 shutdownComplete.countDown() 正式关闭掉 KafkaRequestHandler 线程。

## **2.5 awaitShutdown()**

// 等待线程完成关闭

def awaitShutdown(): Unit = shutdownComplete.await()

该方法也超级简单，主要用来**等待线程完成关闭**，一旦 run() 方法执行了 shutdownComplete.countDown()，将解除等待，从而完成整个线程的关闭请求。

至此，「**KafkaRequestHandler**」类的方法就带你剖析完了，接下来我们来重点看看「**KafkaRequestHandlerPool**」类的重要参数和重要方法。

##   
**03 KafkaRequestHandlerPool 类**

从「**KafkaRequestHandlerPool**」名字来看，它主要是用来管理线程池的相关操作的，先来看看它的重要字段和参数，其源码跟「**KafkaRequestHandler**」在一起，这里就不列了，直接看上面的源码地址。

## **3.1 参数与重要字段**

/\*\*

\* 用于管理 Kafka 请求处理线程池。构造函数接受了多个参数，包括当前 brokerId、请求通道、KafkaApis类、时间对象、I/O 线程池大小、请求处理程序空闲度量指标。

\*/

class KafkaRequestHandlerPool(val brokerId: Int, // 所属 Broker的序号

val requestChannel: RequestChannel, // RequestChannel 请求通道

val apis: ApiRequestHandler, // KafkaApis类，实际请求处理逻辑类

time: Time, // 时间

numThreads: Int, // I/O 线程池初始线程数量

requestHandlerAvgIdleMetricName: String, // 请求处理程序空闲度量指标

logAndThreadNamePrefix : String) extends Logging with KafkaMetricsGroup {

// 定义 I/O 线程池大小，默认 8 个线程，支持动态变更。

private val threadPoolSize: AtomicInteger = new AtomicInteger(numThreads)

....

// I/O 线程池

val runnables \= new mutable.ArrayBuffer\[KafkaRequestHandler\](numThreads)

// 创建 numThreads 个 KafkaRequestHandler 线程，KafkaRequestHandler 线程就是负责处理 I/O 请求的线程。

for (i <- 0 until numThreads) {

createHandler(i)

}

「**KafkaRequestHandlerPool**」的重要属性字段：

1.  **brokerId**: 即 Broker 对应序号。
2.  **requestChannel**: 即 SocketServer 中的请求通道，通过上一篇的剖析，我们知道请求 request 是保存在「**RequestChannel 请求通道**」中的「**RequestQueue**」队列中的，那么 Kafka 在构造 KafkaRequestHandler 时，必须关联 SocketServer 组件中的 RequestChannel，即要让 I/O 线程能够找到请求被保存的地方。
3.  这里的「**RequestChannel 请求通道**」中的 「**RequestQueue**」为所有 I/O 线程「**KafkaRequestHandler**」共享。
4.  **apis**: 即 KafkaApis 类，真正执行请求处理逻辑的地方。
5.  **numThreads**: I/O 线程池中的初始线程数量。 Broker 端参数 [num.io.threads](http://num.io.threads/) 的值。目前 Kafka 支持动态修改 I/O 线程池的大小，因此这里的 numThreads 是初始线程数，调整后的 I/O 线程池的实际大小可以和 numThreads 不一样。
6.  这里解释一下 **numThreads** 和实际线程池中线程数的关系。刚刚说过 I/O 线程池的大小是可以修改的，在 [kafkaConfig.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/kafkaServer.scala) 中的 startup 方法以及 [kafkaConfig.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/server/kafkaConfig.scala) 中的定义，后面会单独篇章讲解，你会看到以下代码：

// 数据面 I/O 线程池初始化 大小为 config.numIoThreads，即参数 nums.io.threads

dataPlaneRequestHandlerPool = new KafkaRequestHandlerPool(config.brokerId, socketServer.dataPlaneRequestChannel, dataPlaneRequestProcessor, time,config.numIoThreads, s"${SocketServer.DataPlaneMetricPrefix}RequestHandlerAvgIdlePercent", SocketServer.DataPlaneThreadPrefix)

// 在 kafkaConfig.scala 中定义

object Defaults {

....

val NumIoThreads \= 8

....

}

object KafkaConfig {

....

val NumIoThreadsProp \= "num.io.threads"

....

val NumIoThreadsDoc \= "The number of threads that the server uses for processing requests, which may include disk I/O"

....

private val configDef \= {

....

.define(NumIoThreadsProp, INT, Defaults.NumIoThreads, atLeast(1), HIGH, NumIoThreadsDoc)

....

}

}

class KafkaConfig(val props: java.util.Map\[\_, \_\], doLog: Boolean, dynamicConfigOverride: Option\[DynamicBrokerConfig\])

extends AbstractConfig(KafkaConfig.configDef, props, doLog) {

....

def numIoThreads \= getInt(KafkaConfig.NumIoThreadsProp)

....

}

// 控制面 I/O 线程池初始化 大小为1，硬编码

controlPlaneRequestHandlerPool = new KafkaRequestHandlerPool(config.brokerId, socketServer.controlPlaneRequestChannelOpt.get, controlPlaneRequestProcessor, time,

1, s"${SocketServer.ControlPlaneMetricPrefix}RequestHandlerAvgIdlePercent", SocketServer.ControlPlaneThreadPrefix)

1.  从这些代码可以得出， 数据面的 KafkaRequestHandlerPool 线程池的初始数量，就是 Broker 端的参数 [nums.io.threads](http://nums.io.threads/)，即这里的 config.numIoThreads 值，而控制面的 KafkaRequestHandlerPool 线程池的数量，则硬编码为 1。因此 **Broker 端参数 num.io.threads 控制的是 Broker 启动时 KafkaRequestHandlerPool 线程池的数量，如果你想提升 Broker 端请求处理能力的话，可以尝试增加该参数值**。
2.  **threadPoolSize**: 标识线程池当前的大小，本质上是用 AtomicInteger 包了一层 numThreads。
3.  **分析到这里会有个小疑问，不知道大家有没有，**既然参数 numThreads 已经传值了，为什么这里还要新申请一个变量来记录呢？**这是因为 kafka 支持动态修改 KafkaRequestHandlerPool 线程池的数量，参数传入就固定不变了，所以需要单独创建一个可以支持更新操作的线程池数量的变量，**而使用 AtomicInteger 主要是为了保证多线程下访问的线程安全性，利用它本身的原子操作，能够有效确保并发访问，还能提供必要的内存可见性。
4.  **runnables**: 初始化I/O 线程池，后面会循环创建里面的线程。这里使用的是 scala 中的数组对象类来实现。

介绍完类参数和重要字段后，我们来看看「**KafkaRequestHandler 线程类**」的重要方法，总共 3 个方法，下面带你挨个深度剖析。

首先，来看看「**KafkaRequestHandlerPool 线程池类**」是如何创建「**KafkaRequestHandler 线程**」的，对应的方法是 createHandler()。

##   
**3.2 createHandler()**

// 初始化时会创建 numThreads 个 KafkaRequestHandler 线程，KafkaRequestHandler 线程就是负责处理 I/O 请求的线程。

// 使用了 0 until numThreads 这个区间来生成线程 ID，因此线程 ID 的范围是 0 到 numThreads - 1。代码中，如果 numThreads 等于 8，那么线程 ID 将从 0 到 7。

for (i <- 0 until numThreads) {

// 创建对应序号的 I/O 线程

createHandler(i)

}

// 创建指定 id 序号的 I/O 线程并启动

def createHandler(id: Int): Unit = synchronized {

// 1、创建 KafkaRequestHandler 实例并加入到 runnables I/O 线程池中

runnables += new KafkaRequestHandler(id, brokerId, aggregateIdleMeter, threadPoolSize, requestChannel, apis, time)

// 2、启动 KafkaRequestHandler 线程

KafkaThread.daemon(logAndThreadNamePrefix + "-kafka-request-handler-" + id, runnables(id)).start()

}

该方法也比较简单，主要用来**创建并启动**「**KafkaRequestHandler 线程**」，做了以下 2 件事情：

1.  创建 KafkaRequestHandler 实例并加入到 runnables I/O 线程池数组中。
2.  启动 KafkaRequestHandler 线程，其中线程的命名规范：logAndThreadNamePrefix + "-kafka-request-handler-" + id，其中，logAndThreadNamePrefix 为 "data-plane" | "control-plane"。
3.  数据面线程的名称为：data-plane-kafka-request-handler-0~7。
4.  控制面线程的名称为：data-plane-kafka-request-handler-0。

接下来，我们来看看「**KafkaRequestHandlerPool 线程池类**」是如何重置线程池大小的，对应的方法是 resizeThreadPool()。

## **3.3 resizeThreadPool()**

def resizeThreadPool(newSize: Int): Unit = synchronized {

// 1、获取当前线程池的大小 currentSize。

val currentSize \= threadPoolSize.get

// 打印信息表示将线程池大小重置为 newSize。

info(s"Resizing request handler thread pool size from $currentSize to $newSize")

// 2、如果 newSize 大于 currentSize，则循环创建从 currentSize 到 newSize-1 号的所有 I/O 线程。

if (newSize > currentSize) {

for (i <- currentSize until newSize) {

// 循环创建所有 I/O 线程

createHandler(i)

}

// 3、如果 newSize 小于 currentSize，则循环从 currentSize-1 到 newSize 号线程将最后一个 runnables 对象弹出，然后使用 stop() 方法停止该线程并从 runnables 集合中删除。

} else if (newSize < currentSize) {

for (i <- 1 to (currentSize - newSize)) {

// 循环停止线程

runnables.remove(currentSize - i).stop()

}

}

// 4、最后将线程池大小设置为新值。

threadPoolSize.set(newSize)

}

该方法也比较简单，主要用来**重置线程池的大小**，做了以下 4 件事情：

1.  首先获取当前线程池的大小 currentSize。
2.  如果 newSize 大于 currentSize，则循环创建从 currentSize 到 newSize-1 号的所有 I/O 线程，即**调用 createHandler 方法将线程数补齐到目标值 newSize**。
3.  如果 newSize 小于 currentSize，则循环从 currentSize-1 到 newSize 号线程将最后一个 runnables 对象弹出，然后使用 stop() 方法停止该线程并从 runnables 集合中删除，即**将多余的线程从线程池中移除并停止它们**。
4.  最后将线程池 threadPoolSize 大小设置为新值。

最后我们来看看关于「**KafkaRequestHandlerPool 线程池类**」是如何关闭线程的，对应的方法是 shutdown()。

## **3.4 shutdown()**

/\*\*

\* 循环关闭线程

\* synchronized 给方法加锁，确保在多线程环境下只有一个线程执行该方法

\*/

def shutdown(): Unit = synchronized {

// 1、记录关闭线程日志

info("shutting down")

// 2、确定需要关闭的所有 handler，然后遍历所有 handler。

for (handler <- runnables)

// 告知它们需要开始关闭。

handler.initiateShutdown()

// 3、再次遍历所有的 handler

for (handler <- runnables)

// 等待它们关闭完成。

handler.awaitShutdown()

// 4、记录关闭线程完成日志

info("shut down completely")

}

该方法也比较简单，主要用来**关闭线程的**，使用 synchronized 给方法加锁，确保在多线程环境下只有一个线程执行该方法，做了以下 4 件事情：

1.  记录关闭线程日志。
2.  确定需要关闭的所有 handler，然后遍历所有 handler，调用 handler.initiateShutdown() **告知它们需要开始关闭**。
3.  再次遍历所有的 handler，调用 handler.awaitShutdown() **等待它们关闭完成**，run方法一旦调用 countDown() 方法，这里将解除等待状态。
4.  记录关闭线程完成日志。

至此，「**KafkaRequestHandlerPool**」类的重要参数和重要方法就带你剖析完了，最后通过一张图来说明下：

  
![](https://article-images.zsxq.com/Fmg_6EKo6pwGnSEPF7orcAdWWlm1)

##   
**04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过剖析 Kafka 网络通信架构设计，引出 KafkaRequestHandler 线程。

2、带你深度剖析了 「**KafkaRequestHandler**」线程的源码实现以及「**KafkaRequestHandlerPool**」线程池的源码实现。

下篇我们来深度剖析「**SocketServer 组件类之 KafkaApis 详解**」，大家期待，我们下期见。