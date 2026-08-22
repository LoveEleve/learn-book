大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端之控制器 Controller 如何管理请求发送的**」，了解了 Controller 「**发送请求类型**」、「**发送请求线程实现**」、「**管理 Broker 链接以及写入请求到阻塞队列**」，从今天开始，我们来深度剖析 Kafka「**Controller**」的底层源码实现，这是 Controller 系列第六篇，我们接着来深度聊聊「**Kafka 服务端控制器 Controller 如何处理事件的**」。

  
![](https://article-images.zsxq.com/FohsMncco4JejzydNGnBSFLZzzcz)

##   
**01 总体概述**

在 [【服务端 Broker 源码分析系列第十七篇】图解 Kafka 源码之 Broker 启动集群如何感知](https://articles.zsxq.com/id_j9ibrxqafawv.html) 这篇中，里面涉及到很多事件相关的管理器。

另外如果你了解 Kafka 历史版本变更的话，在 0.11.0.0 版本之前，Controller 组件的源码非常复杂，集群元数据信息同时会被多个线程并发访问，源码里有大量的 Monitor 锁、Lock 锁等安全机制，你根本不知道，变动了这个线程访问的数据，会不会影响到其他线程。

所以自 0.11.0.0 版本开始，社区陆续对 Controller 代码结构进行了改造。非常重要的改动就是将**多线程并发访问的方式改成了单线程的事件队列方式来处理**，最终大大简化了 Controller 端的代码结构。

这部分源码非常重要，**它能够帮助你掌握 Controller 端处理各类事件原理，这将极大地提升你在实际场景中处理 Controller 各类问题的能力**。

本文我们就来深度剖析下 **Controller 是如何处理事件的**。

##   
**02 控制器事件处理总览**

我们从宏观的角度来看下 **Controller 事件队列处理模型以及基础组件构成**，如下图所示：

![](https://article-images.zsxq.com/lrHfSicBzqiMwh2Mt0_t2U0FDt-7)

从图中得出，Controller 端有多个线程向事件队列写入不同种类的事件，比如：ZooKeeper 端注册的 Watcher 线程、各种监听器、Kafka 定时任务线程等等。而在事件队列的另一端，只有一个名为 ControllerEventThread 的线程专门负责处理队列中的事件。**这就是所谓的 Controller 单线程事件队列模型**。

通过通读该部分源码得出，参与实现该模型的源码类总共有以下 4 个：

1.  **ControllerEventProcessor**：它是 Controller 端的事件处理器接口。
2.  **ControllerEvent**：它是 Controller 事件，也就是事件队列中被处理的对象。
3.  **ControllerEventManager**：它是事件处理器，用于创建和管理 ControllerEventThread 线程。
4.  **ControllerEventThread**：它是专属的事件处理线程，它的作用是处理不同种类的 ControllEvent。这个类是 ControllerEventManager 类内部定义的线程类。

以上源码类完整地构建出了单线程事件队列模型。下面我们将一个一个地剖析它们的源码，你要重点掌握事件队列的实现以及专属线程是如何访问事件队列的。

##   
**03 ControllerEventProcessor**

该接口位于 controller 包下的 ControllerEventManager.scala 文件中。它定义了一个支持普通处理和抢占处理 Controller 事件的接口，本质上是一个 trait 类型。

「**ControllerEventManager.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerEventManager.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerEventManager.scala)

源码如下所示：

trait ControllerEventProcessor {

// 接收一个 Controller 事件，并进行处理

def process(event: ControllerEvent): Unit

// 接收一个 Controller 事件，并抢占队列之前的事件进行优先处理。

def preempt(event: ControllerEvent): Unit

}

目前，在 Kafka 源码中，KafkaController 类是 Controller 组件的功能实现类，它也是 ControllerEventProcessor 接口的唯一实现类。

该接口非常简单，只有两个方法，分别是 process 和 preempt。

1.  **process**：它是实现 Controller 事件处理的方法。
2.  **preempt**：它是实现某些高优先级事件的抢占处理的方法，对于 Kafka 来说，目前只有两类事件:「**ShutdownEventThread**」、「**Expire**」需要抢占式处理。

## **04 ControllerEvent**

它就是 Controller 事件，在源码中对应的就是 ControllerEvent 接口。该接口定义在 KafkaController.scala 文件中，本质上是一个 trait 类型。

「**KafkaController.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/KafkaController.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/KafkaController.scala)

源码如下：

sealed trait ControllerEvent {

def state: ControllerState

// preempt() is not executed by \`ControllerEventThread\` but by the main thread.

def preempt(): Unit

}

从上源码得出，**每个 ControllerEvent 都定义了这样一个状态**。Controller 在处理具体的事件时，都会对状态进行相应的变更。

这个状态是由源码文件 ControllerState.scala 中的抽象类 ControllerState 定义的。

「**ControllerState.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerState.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/ControllerState.scala)

源码如下：

sealed abstract class ControllerState {

// 表示 Controller 状态的序号，从 0 开始

def value: Byte

// 用于构造 Controller 状态速率的监控指标名称的

def rateAndTimeMetricName: Option\[String\] =

if (hasRateAndTimeMetric) Some(s"${toString}RateAndTimeMs") else None

protected def hasRateAndTimeMetric: Boolean = true

}

从上源码得出:

1.  每个 ControllerState 都定义了一个 value 值，表示 Controller 状态的序号，从 0 开始。
2.  rateAndTimeMetricName 方法是用于构造 Controller 状态速率的监控指标名称的。

## **05 ControllerEventManager**

在 Kafka 中，Controller 事件处理器代码位于 controller 包下的 ControllerEventManager.scala 文件下。我用一张图来展示下这个文件的结构：

  
![](https://article-images.zsxq.com/Fj1ZQxPEXO46G82S4hZx9SjsY-JS)

如图所示，该文件主要由 4 个部分组成:

1.  **Object ControllerEventManager**：内部保存线程名称、监控指标名称等。
2.  **ControllerEventProcessor**：上面讲解的事件处理器接口。
3.  **QueuedEvent**：它表示事件队列上的事件对象。
4.  **ControllerEventManager**：它是 ControllerEventManager 的伴生类，主要用于创建和管理事件处理线程和事件队列，尤其定义了重要的 ControllerEventThread 线程类。

## **5.1 object ControllerEventManager**

作为 Controller 唯一的事件处理线程，我们要时刻关注这个线程的运行状态。这个线程的名字是由 Object ControllerEventManager 中 ControllerEventThreadName 变量定义的，源码如下：

object ControllerEventManager {

// 线程名称

val ControllerEventThreadName \= "controller-event-thread"

// 监控指标

val EventQueueTimeMetricName \= "EventQueueTimeMs"

val EventQueueSizeMetricName \= "EventQueueSize"

}

它非常简单，就是定义了三个公共变量，一个线程名称，两个监控指标名称。

## **5.2 QueuedEvent**

来看下其定义，源码如下：

class QueuedEvent(val event: ControllerEvent, // ControllerEvent类，表示 Controller 事件

val enqueueTimeMs: Long) { // 表示 Controller 事件被放入到事件队列的时间戳

// 标识事件是否开始被处理

val processingStarted \= new CountDownLatch(1)

// 标识事件是否被处理过

val spent \= new AtomicBoolean(false)

// 处理事件

def process(processor: ControllerEventProcessor): Unit = {

if (spent.getAndSet(true))

return

processingStarted.countDown()

processor.process(event)

}

// 抢占式处理事件

def preempt(processor: ControllerEventProcessor): Unit = {

if (spent.getAndSet(true))

return

processor.preempt(event)

}

// 阻塞等待事件被处理完成

def awaitProcessing(): Unit = {

processingStarted.await()

}

override def toString: String = {

s"QueuedEvent(event=$event, enqueueTimeMs=$enqueueTimeMs)"

}

}

源码比较简单，从上得出，**每个 QueuedEvent 对象实例都包含 ControllerEvent 事件**。

另外定义了三个方法：

1.  **process**：它是实现 Controller 事件处理的方法。
2.  **preempt**：它是实现某些高优先级事件的抢占处理的方法。
3.  **awaitProcessing**：它是等待事件处理的方法。

在 QueuedEvent 对象中，我们再一次看到了「**CountDownLatch**」。Kafka源码非常喜欢用 「**CountDownLatch**」来做各种条件控制，比如用于检测线程是否成功启动、成功关闭等等。

QueuedEvent 对象使用 「**CountDownLatch**」的唯一目的就是判断事件是否被处理过了，在这里就是**确保 Expire 事件在建立 Zookeeper 会话前被处理**。其他场景下，会使用「**spent**」来标识该事件是否已经被处理过了，如果已经被处理过了，当再次调用process方法时就会直接返回。

## **5.3 class ControllerEventManager**

上面剖析了 QueuedEvent 对象之后，接下来我们再来深度剖析下 **ControllerEventManager 类**。

先看下类定义，源码如下：

// 用于管理控制器事件

class ControllerEventManager(controllerId: Int, // 控制器的 ID

processor: ControllerEventProcessor, // 用于处理控制器事件的 ControllerEventProcessor 实例

time: Time,

rateAndTimeMetrics: Map\[ControllerState, KafkaTimer\], // 以控制器状态和 Kafka 计时器为键值对的映射，用于度量控制器事件的速率和时间

eventQueueTimeTimeoutMs: Long = 300000) // 事件队列等待超时的时间，默认为 300000 毫秒（5分钟）

extends KafkaMetricsGroup {

import ControllerEventManager.\_

// 一个私有的 volatile 变量，用于存储控制器的当前状态。通过 @volatile 标记，表示该变量可能在多个线程之间进行并发访问，且每次访问都从共享内存中读取最新值

@volatile private var \_state: ControllerState = ControllerState.Idle

// 一个重入锁，用于保证在多线程环境下对队列的操作互斥进行

private val putLock = new ReentrantLock()

// 一个基于链表的阻塞队列，用于存储待处理的事件

private val queue = new LinkedBlockingQueue\[QueuedEvent\]

// 一个 ControllerEventThread 实例，用于处理事件队列中的事件

private\[controller\] var thread = new ControllerEventThread(ControllerEventThreadName)

// EventQueueSizeMetricName 是度量事件队列大小的指标名称。

private val eventQueueTimeHist = newHistogram(EventQueueTimeMetricName)

// EventQueueTimeMetricName 是事件队列等待时间的指标名称

// newGauge(EventQueueSizeMetricName, () => queue.size) 是一个用于创建新度量指标的方法，用于度量事件队列的当前大小。该方法通过闭包将 queue.size 作为度量值，返回一个 Gauge 实例，用于度量事件队列大小。

newGauge(EventQueueSizeMetricName, () => queue.size)

上面定义了一个控制器事件管理器，包含了「**事件队列**」、「**线程**」、「**状态变量**」、「**度量指标**」等组件，用于处理和度量控制器事件的流程。

## **5.4 ControllerEventThread**

最后我们再来深度剖析了代表消费者的 **ControllerEventThread 类**。

先看下类定义，源码如下：

class ControllerEventThread(name: String) extends ShutdownableThread(name = name, isInterruptible = false) {

logIdent = s"\[ControllerEventThread controllerId=$controllerId\] "

这个类就是一个普通的线程类，继承了 **ShutdownableThread** 基类，而 **ShutdownableThread 类是 Kafka 为很多线程类定义的公共父类**。该父类是 Java Thread 类的子类，其线程逻辑方法 run 的主要源码如下：

def doWork(): Unit

override def run(): Unit = {

isStarted = true

info("Starting")

try {

while (isRunning)

doWork()

} catch {

case e: FatalExitError =>

shutdownInitiated.countDown()

shutdownComplete.countDown()

info("Stopped")

Exit.exit(e.statusCode())

case e: Throwable =>

if (isRunning)

error("Error due to", e)

} finally {

shutdownComplete.countDown()

}

info("Stopped")

}

可见，这个父类会循环地执行 doWork 方法的逻辑，而该方法的具体实现则交由子类来完成。

接着我们来剖析下 **ControllerEventThread** 类的 doWork 方法是如何实现的，源码如下：

override def doWork(): Unit = {

// 1、从事件队列中获取待处理的Controller事件，否则等待

val dequeued \= pollFromEventQueue()

dequeued.event match {

// 2、如果是关闭线程事件，则表示控制器线程正在关闭，忽略该事件。关闭线程由外部来执行

case ShutdownEventThread \=\> // The shutting down of the thread has been initiated at this point. Ignore this event.

// 3、如果是普通的控制器事件，则执行以下步骤

case controllerEvent \=\>

// 更新控制器的状态为该事件所对应的状态。

\_state = controllerEvent.state

// 计算事件在队列中等待的时间，更新对应事件在队列中保存的时间

eventQueueTimeHist.update(time.milliseconds() - dequeued.enqueueTimeMs)

// 尝试执行该事件所对应的处理逻辑，如果执行出现异常，则捕获异常并打印错误日志

try {

def process(): Unit = dequeued.process(processor)

// 处理事件，同时计算处理速率

rateAndTimeMetrics.get(state) match {

case Some(timer) => timer.time { process() }

case None \=\> process()

}

} catch {

case e: Throwable => error(s"Uncaught error processing event $controllerEvent", e)

}

// 最后更新控制器状态为 Idle

\_state = ControllerState.Idle

}

}

这里需要特别注意的是，在执行控制器事件所对应的处理逻辑时，如果该事件所对应的状态在rateAndTimeMetrics 中有对应的计时器，则先通过计时器对处理逻辑进行计时，再执行处理逻辑。如果没有对应的计时器，则直接执行处理逻辑。

我们看下第一步的方法实现，源码如下：

// 从事件队列中获取待处理的Controller事件

private def pollFromEventQueue(): QueuedEvent = {

// 获取事件队列中事件的数量，记录在count变量中

val count \= eventQueueTimeHist.count()

// 如果事件数量不为 0，则执行以下步骤：

if (count != 0) {

// 使用指定的超时时间从队列中获取一个事件，记录在 event 变量中。

val event \= queue.poll(eventQueueTimeTimeoutMs, TimeUnit.MILLISECONDS)

// 如果获取到的事件为 null，则清除事件队列时延的统计信息，并且从队列中取出一个事件

if (event == null) {

eventQueueTimeHist.clear()

queue.take()

} else {

// 如果获取到的事件非 null，则直接返回获取的事件

event

}

} else {

// 如果事件数量为 0，则直接从队列中取出一个事件

queue.take()

}

}

这里需要注意，这里用的是**take方法**，它表示如果事件队列中没有 QueuedEvent，那么 ControllerEventThread 线程将一直处于阻塞状态，直到事件队列上插入了新的待处理事件。

另外 eventQueueTimeHist 和 queue 都是事件队列，其中 eventQueueTimeHist 用于记录事件在队列中等待的时延信息，而 queue 则用于存储排队等待处理的事件。而 eventQueueTimeTimeoutMs 则指定了从队列中获取排队事件的最大等待时间，超过该时间则会返回 null。

我们用一张图来说明其执行流程：

![](https://article-images.zsxq.com/FjVkv3Aj5XoxkN-gvsGmZ1UnYH-2)

接着我们来看下 process 处理事件方法具体实现，它底层调用的是 ControllerEventProcessor 的 process 方法，源码如下：

def process(processor: ControllerEventProcessor): Unit = {

// 若已经被处理过，直接返回

if (spent.getAndSet(true))

return

processingStarted.countDown()

// 调用 ControllerEventProcessor 的 process 方法处理事件

processor.process(event)

}

这里，你**可能好奇，对于每个 ControllerEventProcessor 的 process 方法在哪里实现的？**实际上，它们都被封装在 [KafkaController.scala](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/KafkaController.scala) [](https://github.com/apache/kafka/blob/2.7.0/core/src/main/scala/kafka/controller/KafkaController.scala)文件中。

> 记住一点：KafkaController 类是目前源码中 ControllerEventProcessor 接口的唯一实现类。

  
![](https://article-images.zsxq.com/Fnk-V1kCckaaycE3h7hTs-pc-JRD)

我们来看下真正处理事件的 process 方法，这里省略部分源码如下：

override def process(event: ControllerEvent): Unit = {

try {

// 依次匹配 ControllerEvent 事件

event match {

case event: MockEvent =>

// Used only in test cases

event.process()

case ShutdownEventThread \=\>

error("Received a ShutdownEventThread event. This type of event is supposed to be handle by ControllerEventThread")

....

case Startup =\>

processStartup()

}

} catch {

// 如果 Controller 换成了别的 Broker

case e: ControllerMovedException =>

info(s"Controller moved to another broker when processing $event.", e)

// 执行 Controller 卸任逻辑

maybeResign()

case e: Throwable =>

error(s"Error processing event $event", e)

} finally {

updateMetrics()

}

}

该方法接收一个 ControllerEvent 实例，接着会判断它是哪类 Controller 事件，并调用相应的处理方法。

**剖析完了读队列事件之后，我们再来看下写队列事件是如何处理的**。

写队列事件也分两种：「**普通事件**」、「**高优先级的抢占事件**」。

## **5.5 put**

先来看下「**普通事件**」的处理方法，源码如下：

def put(event: ControllerEvent): QueuedEvent = inLock(putLock) {

// 构建QueuedEvent实例

val queuedEvent \= new QueuedEvent(event, time.milliseconds())

// 写入到事件队列

queue.put(queuedEvent)

// 返回新建QueuedEvent实例

queuedEvent

}

该方法主要是把**指定 ControllerEvent 事件写入到事件队列中**。

## **5.6 clearAndPut**

接着来看下「**高优先级的抢占式事件**」的处理方法，源码如下：

def clearAndPut(event: ControllerEvent): QueuedEvent = inLock(putLock){

// 优先处理抢占式事件,创建一个 ArrayList 集合 preemptedEvents，用于存储被抢占的事件

val preemptedEvents \= new ArrayList\[QueuedEvent\]()

// 将队列中的所有事件都取出，并存储到 preemptedEvents 集合中

queue.drainTo(preemptedEvents)

// 针对 preemptedEvents 集合中的每个事件，调用 preempt 方法，执行被抢占的处理逻辑

preemptedEvents.forEach(\_.preempt(processor))

// 将新的事件 event 放入队列中

put(event)

}

// \_.preempt(processor)

def preempt(processor: ControllerEventProcessor): Unit = {

// 若已经被处理过，直接返回

if (spent.getAndSet(true))

return

// 调用 ControllerEventProcessor 的 preempt 方法处理抢占式事件

processor.preempt(event)

}

这里需要注意的是，使用了 **putLock 进行加锁目的是为了确保对队列操作的原子性**。在清空队列和写入新事件的过程中，其他线程则无法写入新的事件或读取队列中的事件，以确保操作的完整性。

另外，这里的 processor 是一个函数，用于执行事件的处理逻辑。同时，这里使用了 inLock 方法，它是一种加锁的快捷方式，用于简化代码编写。

## **5.7 其他方法**

最后我们再来看几个其他方法，都比较简单。

// 对外提供了 state 方法，用于获取控制器的状态。

def state: ControllerState = \_state

// start方法启动了控制器线程。

def start(): Unit = thread.start()

// 用于关闭控制器线程

def close(): Unit = {

try {

// 停止控制器线程的消息消费，即将消息处理线程停掉

thread.initiateShutdown()

// 将 ShutdownEventThread 写入到阻塞事件队列中，表示控制器线程已经停掉，需要停止事件的消费

clearAndPut(ShutdownEventThread)

// 等待控制器线程的停止操作完成

thread.awaitShutdown()

} finally {

// 删除控制器的度量信息，以便等待垃圾回收

removeMetric(EventQueueTimeMetricName)

removeMetric(EventQueueSizeMetricName)

}

}

需要注意的是，这里的 thread 表示控制器的主要线程对象，也是事件处理线程。而 ShutdownEventThread 是一个 shutdown 事件，用来表示线程已经停止，是一个特殊的预定义事件。clearAndPut 方法用于清空当前队列并将shutdown 事件写入队列的操作。removeMetric 方法用于删除度量信息。

## **06 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头通过对比新旧 Kafka 版本关于「**Controller 事件处理模型**」，引出了 Kafka 单线程的事件处理模型，即 ControllerEventManager 通过构建 ControllerEvent、ControllerState 和对应的 ControllerEventThread 线程，并且结合事件队列，共同实现事件处理。

2、接着带大家深度剖析了 「**ControllerEventProcessor**」的实现逻辑。

4、接着带大家深度剖析了 「**ControllerEvent**」的实现逻辑，它是用来定义 Controller 能够处理的各类事件名称。

5、接着带大家深度剖析了 「**ControllerEventManager**」的实现逻辑，它是 Controller 定义的事件管理器，专门定义和维护专属线程以及对应的事件队列。

6、最后带大家深度剖析了「**ControllerEventThread**」的实现流程，它是事件管理器创建的事件处理线程。该线程排他性地读取事件队列并处理队列中的所有事件。

下篇我们来深度剖析「**Topic 创建、删除请求处理流程**」，大家期待，我们下期见。