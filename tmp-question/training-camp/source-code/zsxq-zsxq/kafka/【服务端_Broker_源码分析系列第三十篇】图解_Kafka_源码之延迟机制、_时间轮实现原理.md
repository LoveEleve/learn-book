大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端 LeaderEpoch 机制实现原理**」，了解了 LeaderEpoch 是如何解决之前基于 HW 机制导致数据丢失问题的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端延迟机制、时间轮实现原理**」，看看 Kafka 是如何实现「**时间轮算法**」、以及如何利用解决「**延迟请求**」的。

![](https://article-images.zsxq.com/Fj4Ch6myuatuyqsfWd5uD0qoD7-q)

## **01 总体概述**

在前面的章节中，我们或多或少都会遇到以下这样的处理逻辑：

// 触发对应的延迟操作

tryCompleteDelayedRequests()

延迟请求（Delayed Operation）：它是指因未满足条件而暂时无法被处理的 Kafka 请求。

这里举个例子，假如配置了 acks=all 的生产者发送的请求可能一时无法完成，因为 **Kafka 必须确保 ISR 副本集合中的所有副本都要成功响应这次写入请求才是真正完成**。在通常情况下，这些请求无法被立即处理。只有满足了条件或发生了超时，Kafka 才会把该请求标记为完成状态。这就是所谓的延迟请求。

另外 Kafka 中还存在其他的延迟操作：

1.  当消费者重平衡时，协调者需要等待消费组中的消费组都加入消费组后再进行分配。
2.  消费者要求 Broker 返回指定大小的消息内容，如果当前读取的消息内容不满足要求，则需要等待消息。

那么 Kafka 究竟使用何种算法来实现延迟请求的，对于 Kafka 这种超高并发、超高性能的消息中间件来说，它的功能实现都需要时刻满足三高要求，否则可能会带来性能问题。

带着这个问题开启我们今天的话题。

##   
**02 延迟请求解决方案**

业界主流系统在解决延迟请求时大部分的解决方案就是使用「**时间轮**」算法。

时间轮的应用范围非常广。很多操作系统的定时任务调度「**Crontab**」以及通信框架「**Netty**」等都利用了「**时间轮**」的思想。几乎所有的时间任务调度系统都是基于时间轮算法的。Kafka 也是基于「**时间轮**」算法来管理延迟请求，这样的源码实现简洁精炼，而且和业务逻辑完全解耦，你可以从 0 到 1 地照搬到你自己的项目工程中。

今天，我们的重点是弄明白请求被延迟处理的机制 ——「**时间轮**」算法。

## **03 时间轮算法简介**

假如让你来设计 kafka 中的延迟请求，你该怎么做呢？

你可能会直接使用 Java 自带的 DelayQueue 类来实现延时队列，实际上， Kafka 第一版本的延迟请求就是使用 DelayQueue 来做的。

但是它有一个弊端：**它插入和删除队列元素的时间复杂度是 O(logN)**。对于 Kafka 这种很容易就积攒几十万个延迟请求的场景来说，它的数据结构的瓶颈就在性能上面。当然还有其他弊端，比如，它在清除已过期的延迟请求方面不够高效，可能会出现内存溢出的情形。

后来 Kafka 改造了延迟请求的实现机制，采用了基于时间轮的方案。时间轮是一种充分利用线程资源进行批量化任务调度的调度模型算法，能够高效地管理各种延迟任务。

「**时间轮**」算法有两种实现方式：「**简单时间轮**」、「**分层时间轮**」。两者各有利弊，也都有各自的使用场景。

Kafka 目前采用的是「**分层时间轮**」，这是我们今天学习的重点内容。

关于原理部分，可以看这篇：[【原理分析系列第十七篇】图解 Kafka 时间轮算法实现原理](https://articles.zsxq.com/id_fnwudzvq4ozx.html)

## **04 Kafka 时间轮算法源码实现**

在 Kafka 中，具体是怎么使用「**分层时间轮**」算法来实现延迟请求队列呢？

  
![](https://article-images.zsxq.com/Fiq8unCD4sb1NqUNaGNBqE1fq8kl)

图中的时间轮共有两个层级，分别是 Level 0 和 Level 1。每个时间轮有 20 个 Bucket，每个 Bucket 下是一个双向循环链表，用来保存延迟请求。

在Kafka源码中，时间轮对应 [utils.timer](http://utils.timer/) 包下的 [TimingWheel](http://timingwheel%20/) 类，每个 Bucket 下的链表对应 TimerTaskList 类，链表元素对应 TimerTaskEntry 类，而每个链表元素里面保存的延迟任务对应 TimerTask。

在这些类中，[TimerTaskEntry](http://timertaskentry/) 与 [TimerTask](http://timertask/) 是1对1的关系，而 [TimerTaskList](http://timertasklist%20/) 下包含多个 [TimerTaskEntry](http://timertaskentry/)，[TimingWheel](http://timingwheel/) 又包含多个 [TimerTaskList](http://timertasklist/)。

[Timer](http://timer%20/) 接口和 [SystemTimer](http://systemtimer/) 实现类用来实现时间轮 Bucket 的管理以及时钟向前推进功能。它是实现延迟请求后续被自动处理的基础。

这些类对应关系图如下：

![](https://article-images.zsxq.com/Fg6vGfrFavRZnDGKaTbjZ6g5dzxq)

接下来，我们挨个来剖析下这些类来**解释下延迟请求是如何被这套分层时间轮来管理的**。根据调用关系，我采用自上而下的方法来进行剖析。

源码位置如下：

  
![](https://article-images.zsxq.com/Fr5d2LGV-Ze8mKnLwFi9m7BC_HO-)

## **4.1 TimingWheel**

我们先来看下 TimingWheel类，**它是用来统一管理其下的所有 Bucket 以及定时任务**。它使用时间轮算法，监控延迟任务是否到期。

「**TimingWheel**」类源码在 Kafka 源码包的 utils 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/utils/timer/TimingWheel.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/utils/timer/TimingWheel.scala)

## **4.1.1 TimingWheel 类定义**

其方法定义如下：

@nonthreadsafe

private\[timer\] class TimingWheel(

tickMs: Long,

wheelSize: Int,

startMs: Long,

taskCounter: AtomicInteger,

queue: DelayQueue\[TimerTaskList\]) {

private\[this\] val interval \= tickMs \* wheelSize

private\[this\] val buckets \= Array.tabulate\[TimerTaskList\](wheelSize) { \_ => new TimerTaskList(taskCounter) }

private\[this\] var currentTime \= startMs - (startMs % tickMs) // rounding down to multiple of tickMs

@volatile private\[this\] var overflowWheel: TimingWheel = null

....

}

如上，每个 TimingWheel 对象都定义了上面这些字段。它们都非常重要，每个字段都是「**分层时间轮**」的重要属性，如下：

1.  **tickMs**：它表示一个槽位的时长，即向前推进一格的时间。在 Kafka 中，第 1 层时间轮的 tickMs 被固定为 1 毫秒，即向前推进一格 Bucket 的时长是 1 毫秒。
2.  **wheelSize**：它表示每一层时间轮上的槽位 Bucket 数量。Kafka 默认值为 20，所以第 1 层的 Bucket 数量是 20。
3.  **startMs**：它表示时间轮对象被创建时的起始时间戳。
4.  **taskCounter**：它表示这一层时间轮上的总定时任务数。
5.  **queue**：它表示将所有 Bucket 按照过期时间排序的 TimerTaskList 延迟队列。随着时间不断向前推进，Kafka 需要依靠它获取那些已过期的Bucket，并清除它们。
6.  **interval**：它表示这层时间轮的时间跨度，等于 tickMs \* wheelSize。以第 1 层为例，interval 就是 20 毫秒。由于下一层时间轮的 tickMs 就是上一层的总时长，第 2 层的 tickMs 就是 20 毫秒，总时长是 400 毫秒，以此类推。
7.  **buckets**：它表示时间轮下的所有槽位 Bucket 对象，即所有 TimerTaskList 对象实例。
8.  **currentTime**：它表示当前时间戳，即时间轮指针指向的时间，在源码中将它设置成小于当前时间的最大 tickMs 的整数倍。举例假设 tickMs 是 20 毫秒，当前时间戳是 123 毫秒，那么，currentTime 会被调整为 120 毫秒。
9.  **overflowWheel**：Kafka 是按需创建上层时间轮的。当有新的定时任务到达时，会尝试将其放入第 1 层时间轮。如果第 1 层的 interval 无法容纳定时任务的超时时间，就新创建并配置好第 2 层时间轮，并再次尝试放入，如果依然无法容纳，那么，就再创建和配置第 3 层时间轮，以此类推，直到找到适合容纳该定时任务的第 N 层时间轮。

从上面定义字段可以得出，**每层时间轮的长度都是倍增的，所以并不需要创建太多层的时间轮就可以容纳绝大部分的延迟请求**。

接下来我们剖析下该类下的重要方法。

## **4.1.2 addOverflowWheel()**

在定义中讲到 overflowWheel 的创建是按需的，每当需要一个新的上层时间轮时，都会调用该方法进行创建，我们来剖析下：

private\[this\] def addOverflowWheel(): Unit = {

synchronized {

// 只有之前没有创建上层时间轮方法才会继续

if (overflowWheel == null) {

// 创建新的 TimingWheel 实例

overflowWheel = new TimingWheel(

tickMs = interval, // tickMs 等于下层时间轮总时长

wheelSize = wheelSize, // 每层的轮子数都是相同的

startMs = currentTime,

taskCounter = taskCounter,

queue

)

}

}

}

该方法比较简单，就是**创建一个新的 TimingWheel 对象实例**，**即创建上层时间轮**。所用的槽位时长 tickMs 等于下层时间轮总时长，而每层的轮子数都是相同的。创建完成之后将新创建的实例赋值给 overflowWheel 字段。

## **4.1.3 add()**

接下来我们来看下 add 方法。它的调用链路是：[DelayedOperationPurgatory#tryCompleteElseWatch](http://delayedoperationpurgatory/#tryCompleteElseWatch) 方法调用 [SystemTimer#add](http://systemtimer/#add) 将 TimerTask 转化为 TimerTaskEntry，再调用 [TimingWheel#add](http://timingwheel/#add) 方法将定时任务添加到时间轮中。

def add(timerTaskEntry: TimerTaskEntry): Boolean = {

// 首先获取定时任务的过期时间戳

val expiration \= timerTaskEntry.expirationMs

// 如果该任务已然被取消了，直接返回

if (timerTaskEntry.cancelled) {

// Cancelled

false

// 如果该任务超时时间已过期，直接返回

} else if (expiration < currentTime + tickMs) {

// Already expired

false

// 如果该任务超时时间在本层时间轮覆盖时间范围内

} else if (expiration < currentTime + interval) {

// Put in its own bucket

// 计算 bucket 的虚拟 id

val virtualId \= expiration / tickMs

// 计算要被放入到哪个 Bucket 中

val bucket \= buckets((virtualId % wheelSize.toLong).toInt)

// 将 timerTaskEntry 添加到 Bucket 中

bucket.add(timerTaskEntry)

// Set the bucket expiration time

// 如果 Bucket 的到期时间没有设置，则设置到期时间

if (bucket.setExpiration(virtualId \* tickMs)) {

// 如果该时间变更过，说明 Bucket 是新建或被重用，将其加回到 DelayQueue

queue.offer(bucket)

}

true

} else {

// 如果定时任务的过期时间无法被涵盖在本层时间轮中，交由上层时间轮处理

// Out of the interval. Put it into the parent timer

// 如果 overflowWheel 为空，则按需创建上层时间轮

if (overflowWheel == null) addOverflowWheel()

// 将 timerTaskEntry 加入到上层时间轮中

overflowWheel.add(timerTaskEntry)

}

}

该方法主要用来处理**添加操作的**，将一个 TimerTaskEntry 添加到时间轮中，步骤如下：

1.  首先获取定时任务的过期时间戳。所谓过期时间戳就是该定时任务过期时的时间点。
2.  判断该定时任务是否已被取消。如果已经被取消，则无需加入到时间轮中，直接返回。
3.  如果没有被取消，接着判断该定时任务是否已经过期。如果过期了，也不用加入到时间轮中，直接返回。
4.  如果没有过期，接着判断该定时任务的过期时间是否能够被涵盖本层时间轮的时间范围内。如果可以，则进行计算：
5.  首先计算目标 Bucket 位置，即这个定时任务需要被保存在哪个 TimerTaskList 中。举例说明如何计算目标 Bucket。首先第 1层的时间轮有 20 个Bucket，每个 tickMs 是 1 毫秒。那么第 2 层时间轮的 tickMs 就是 20 毫秒，总时长是 400 毫秒。第 2层第 1 个 Bucket 的时间范围应该是 \[20，40)，第 2 个Bucket的时间范围是 \[40，60），依次类推。假如现在有个延迟请求的超时时间戳是 128，它就应该被插入到第 6 个Bucket 中。此时将该定时任务添加到这个 Bucket 下，同时更新这个Bucket 的过期时间戳。在这个例子中，第 6 号 Bucket 的起始时间就应该是小于 128 的最大的 20 的倍数，即 120。
6.  如果该 Bucket 是首次插入定时任务，那么要将它加入到 DelayQueue 中，方便 Kafka 轻松地获取那些已过期Bucket，并删除它们。
7.  如果定时任务的过期时间无法被涵盖在本层时间轮中，此时按需创建上一层时间戳，然后将 timerTaskEntry 加入到上层时间轮中。

这里通过一张图来梳理其执行流程：

![](https://article-images.zsxq.com/lsv_9j_16L1Ibszoi2wkH60UpWUX)

## **4.1.4 advanceClock()**

当定时任务被添加到时间轮后， **Kafka 究竟如何执行它呢？**

在 [DelayedOperationPurgatory#](http://delayedoperationpurgatory/#tryCompleteElseWatch)[expirationReaper](http://expirationreaper/) 中定义了一个线程，该线程会不断的调用 [DelayedOperationPurgatory#](http://delayedoperationpurgatory/#tryCompleteElseWatch)[advanceClock](http://advanceclock/) 方法进行时间轮推进，接着调用 [SystemTimer#advanceClock](http://systemtimer/#advanceClock) 方法，最终调用 [TimingWheel#advanceClock](http://timingwheel/#advanceClock) 方法。

那么接下来我们来看下 advanceClock 方法。从名字上看，它是用来推进时间轮的时钟，使其与指定的时间 timeMs 进行同步。

def advanceClock(timeMs: Long): Unit = {

// 判断时间 timeMs 是否大于等于当前时间加上时间间隔 tickMs，即要超过当前 Bucket 的时间范围

if (timeMs >= currentTime + tickMs) {

// 更新当前时间 currentTime 到下一个 Bucket 的起始时点

currentTime = timeMs - (timeMs % tickMs)

// Try to advance the clock of the overflow wheel if present

// 同时尝试为上一层时间轮做向前推进操作

if (overflowWheel != null) overflowWheel.advanceClock(currentTime)

}

}

该方法主要用来处理**推进时间轮的时钟，使其与指定时间 timeMs 进行同步，其目的就是为了移动时间轮中的指针以处理已经到期的 TimerTaskEntry**，步骤如下：

1.  判断时间 timeMs 是否大于等于当前时间加上时间间隔 tickMs，即要超过当前 Bucket 的时间范围。
2.  如果是则更新当前时间 currentTime 到下一个 Bucket 的起始时点。
3.  同时尝试为上一层时间轮做向前推进操作。

这里总结下：

1.  按任务到期时间，将任务添加到对应的槽位 Bucket 中。
2.  将任务的槽位 Bucket 放入到一个延迟队列 DelayQueue 中。
3.  从 DelayQueue 中取出已过期的槽位 Bucket，执行里面的任务，并修改时间指针 currentTime。

接着我们来剖析下一个类，该链表类 TimerTaskList 闪亮登场了。

## **4.2 TimerTaskList**

「**TimerTaskList**」类源码在 Kafka 源码包的 utils 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/utils/timer/TimerTaskList.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/utils/timer/TimerTaskList.scala)

## **4.2.1 TimerTaskList 类定义**

**它是时间轮 Bucket 下的延迟请求双向循环链表，提供了 O(1) 的时间复杂度的请求插入和删除功能**。

private\[timer\] class TimerTaskList(taskCounter: AtomicInteger) extends Delayed {

// TimerTaskList forms a doubly linked cyclic list using a dummy root entry

// root.next points to the head

// root.prev points to the tail

private\[this\] val root \= new TimerTaskEntry(null, -1)

root.next = root

root.prev = root

private\[this\] val expiration \= new AtomicLong(-1L)

....

}

该类是用来**实现双向循环链表的**，字段如下：

1.  **root**：它表示根节点。
2.  **taskCounter**：它表示当前这个链表中的总定时任务数。
3.  **expiration**：它表示这个链表所在 Bucket 的过期时间戳。

![](https://article-images.zsxq.com/FvCxhvYnXqzmAwdjOB-8VNP4yBh_)

我们先看 expiration 的 Getter 和 Setter 方法。

## **4.2.2 setExpiration、getExpiration**

// Setter方法

def setExpiration(expirationMs: Long): Boolean = {

expiration.getAndSet(expirationMs) != expirationMs

}

// Getter方法

def getExpiration: Long = expiration.get

可以看到 Setter 方法中，使用了 **AtomicLong 的 CAS 方法 getAndSet 原子性地设置了过期时间戳**，之后将新过期时间戳和旧值进行比较，然后返回结果。

看到这里你是否会有疑问：**为什么要比较新旧值是否不同呢？**

其实主要问题是 Kafka 底层使用一个 DelayQueue 队列来统一管理所有地 Bucket，即「**TimerTaskList**」对象，随着时钟不断向前推进，原有 Bucket 会不断地过期最后失效。当这些 Bucket 失效后，还会重用这些 Bucket。

**重用地方式就是重新设置 Bucket 的过期时间，并把它加回到 DelayQueue 中**。对比的目的就是用来判断这个Bucket 是否要被重新插入到 DelayQueue 中。

## **4.2.3 add()**

// 将定时任务插入到链表

def add(timerTaskEntry: TimerTaskEntry): Unit = {

var done \= false

while (!done) {

// 检查 timerTaskEntry 是否已经在其他列表中，如果是，则先将其从其他列表中移除。

// 将该操作放在同步块之外是为了避免死锁

// 在 timerTaskEntry.list 变为 null 之前，我们可能会重试

timerTaskEntry.remove()

synchronized {

// 在同步块的作用域内，使用 timerTaskEntry 锁定 timerTaskEntry 对象。

timerTaskEntry.synchronized {

// 如果在循环中检测到 timerTaskEntry 的 list 不为 null，则通过重新执行循环来重试添加操作，直到 timerTaskEntry 的 list 变为 null。

if (timerTaskEntry.list == null) {

val tail \= root.prev

timerTaskEntry.next = root

timerTaskEntry.prev = tail

timerTaskEntry.list = this

// 把 timerTaskEntry 添加到链表末尾

tail.next = timerTaskEntry

root.prev = timerTaskEntry

// 增加任务计数器的值。

taskCounter.incrementAndGet()

// 将标志变量 done 设置为 true，表示添加操作完成。

done = true

}

}

}

}

}

## **4.2.4 remove()**

// 从列表中移除一个 TimerTaskEntry

def remove(timerTaskEntry: TimerTaskEntry): Unit = {

synchronized {

timerTaskEntry.synchronized {

if (timerTaskEntry.list eq this) {

// 从列表中移除 timerTaskEntry

timerTaskEntry.next.prev = timerTaskEntry.prev

timerTaskEntry.prev.next = timerTaskEntry.next

timerTaskEntry.next = null

timerTaskEntry.prev = null

timerTaskEntry.list = null

taskCounter.decrementAndGet()

}

}

}

}

上面 2 个方法，主要用来将给定定时任务插入到链表、从链表中移除定时任务的逻辑，比较简单，自行学习即可。

## **4.2.5 flush()**

def flush(f: TimerTaskEntry => Unit): Unit = {

synchronized {

// 找到链表第一个元素

var head \= root.next

// 开始遍历链表

while (head ne root) {

// 移除遍历到的链表元素

remove(head)

// 执行传入参数f的逻辑

f(head)

head = root.next

}

// 清空过期时间设置

expiration.set(-1L)

}

}

该方法主要用来**对列表中所有的 TimerTaskEntry 执行操作 f，并从列表移除它们**，步骤如下：

1.  首先在同步块的作用域内，找到链表第一个元素。
2.  开始遍历链表，判断的逻辑： head 不等于 root，一直重复以下操作：
3.  从列表中移除 head，即调用 remove(head) 方法从列表中移除 head。
4.  执行操作 f。通常情况下，操作 f 是取消 TimerTaskEntry，即调用 TimerTaskEntry 的 cancel 方法。
5.  取下一个 head，将 head 的下一个元素作为新的 head。
6.  重复以上步骤直到 head 等于 root，即列表为空
7.  将 expiration（过期时间）设置为-1，重置 TimerWheel 的计时器。

##   
**4.2.6 foreach()**

// 对列表中的每个任务执行提供的函数 f

def foreach(f: (TimerTask)\=>Unit): Unit = {

synchronized {

var entry \= root.next

while (entry ne root) {

val nextEntry \= entry.next

// 如果 entry 没有被取消，就对其对应的 timerTask 执行函数 f

if (!entry.cancelled) f(entry.timerTask)

entry = nextEntry

}

}

}

该方法主要用来**对列表中的每个任务执行提供的函数 f**，步骤如下：

1.  首先，在同步块的作用域内，获取列表的头指针 entry，即 root 的下一个元素。
2.  开始遍历链表，判断的逻辑：在 entry 不等于 root 的情况下，重复以下操作：
3.  获取 entry 的下一个元素作为 nextEntry。
4.  如果 entry 没有被取消（cancelled 属性为 false），则执行函数 f，传入 entry 对应的 timerTask 作为参数。
5.  将 nextEntry 赋值给 entry，即将下一个元素作为新的 entry。
6.  重复以上步骤直到 entry 等于 root，即遍历完整个列表。

接着我们来剖析下一个类，该链表类 TimerTaskEntry 闪亮登场了。

## **4.3 TimerTaskEntry**

「**TimerTaskEntry**」是「**TimerTaskList**」的一个子类。

**它是 Bucket 下延迟请求链表的元素，内部封装了 TimerTask 对象和定时任务的过期时间戳信息**。

private\[timer\] class TimerTaskEntry(

val timerTask: TimerTask, // 与该 TimerTaskEntry 相关联的 TimerTask 定时任务。

val expirationMs: Long // TimerTaskEntry 的过期时间。

) extends Ordered\[TimerTaskEntry\] {

@volatile

// 当前 TimerTaskEntry 所在的 TimerTaskList

var list: TimerTaskList = null

// 下一个 TimerTaskEntry

var next: TimerTaskEntry = null

// 上一个 TimerTaskEntry

var prev: TimerTaskEntry = null

// 如果 timerTask 不为 null，关联给定的定时任务

if (timerTask != null) timerTask.setTimerTaskEntry(this)

....

}

该类定义了 2 个字段：

1.  **TimerTask**： 与该 TimerTaskEntry 相关联的 TimerTask 定时任务。
2.  **expirationMs**：TimerTaskEntry 的过期时间。
3.  这里举例说明：假如此时有个 [PRODUCE](http://produce/) 请求在当前时间 3 点被发送到 Broker，超时时间为 15 秒，那么该请求必须在 3 点 15 秒之前完成，否则超时。这里的 3 点 15 秒就是字段 **expirationMs**。
4.  **list**：Bucket 链表实例，该字段是 volatile 型的，因为 Kafka 的延迟请求可能会被其他线程从一个链表移到另一个链表中，**为了保证必要的内存可见性**，声明为 volatile 型的。
5.  **next**：下一个 TimerTaskEntry。
6.  **prev**：上一个 TimerTaskEntry。

## **4.3.1 cancelled()**

// 关联定时任务是否已经被取消了

def cancelled: Boolean = {

// 返回 timerTask.getTimerTaskEntry 是否等于当前 TimerTaskEntry，用于检查 TimerTask 是否已被取消。

timerTask.getTimerTaskEntry != this

}

## **4.3.2 remove()**

// 将 TimerTask 自身从双向链表中移除掉。

def remove(): Unit = {

var currentList \= list

while (currentList != null) {

// 从所在的 TimerTaskList 中移除当前 TimerTaskEntry。

currentList.remove(this)

// 使用循环重试直到列表变为 null。

currentList = list

}

}

该方法主要用来**对将 TimerTask 从双向链表中移除掉**。

那么这里有个问题：**怎么才算真正移除掉呢？**判断的逻辑是 TimerTaskEntry 的 list 是否为空，一旦为空，双向链表就被删除了，从而达到删除的效果。

由于该方法可能会被其他线程同时调用，因此使用了 while 循环的方式来确保 TimerTaskEntry 的 list 确实被置空了。这样 Kafka 才能安全地认为此链表元素被成功移除了。

## **4.3.3 compare()**

// 实现 Ordered trait 的 compare 方法，用于比较两个 TimerTaskEntry 的过期时间，以便进行排序。

override def compare(that: TimerTaskEntry): Int = {

java.lang.Long.compare(expirationMs, that.expirationMs)

}

接着我们来剖析下一个类，该链表类 TimerTask 闪亮登场了。

## **4.4 TimerTask**

「**TimerTask**」类源码在 Kafka 源码包的 utils 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/utils/timer/TimerTask.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/utils/timer/TimerTask.scala)

该类源码比较简单，很容易理解，源码如下：

trait TimerTask extends Runnable {

// request.timeout.ms 参数值

val delayMs: Long // timestamp in millisecond

// 每个 TimerTask 实例关联一个 TimerTaskEntry，每个定时任务需要知道它在哪个 Bucket 链表下的哪个链表元素上

private\[this\] var timerTaskEntry: TimerTaskEntry = null

// 取消定时任务

def cancel(): Unit = {

synchronized {

// timerTaskEntry 不为空，则把定时任务从链表上移除。

if (timerTaskEntry != null) timerTaskEntry.remove()

// 将关联的 timerTaskEntry 置空

timerTaskEntry = null

}

}

// 关联timerTaskEntry，由锁保护起来，以保证线程安全性

private\[timer\] def setTimerTaskEntry(entry: TimerTaskEntry): Unit = {

synchronized {

// 判断定时任务是否已经绑定了其他的 timerTaskEntry，如果是的话，就必须先取消绑定

if (timerTaskEntry != null && timerTaskEntry != entry)

timerTaskEntry.remove()

// 给 timerTaskEntry 字段赋值

timerTaskEntry = entry

}

}

// 获取关联的 timerTaskEntry 实例

private\[timer\] def getTimerTaskEntry: TimerTaskEntry = timerTaskEntry

}

TimerTask 类是一个 Scala 接口 Trait。每个 TimerTask 都有一个 delayMs 字段，表示这个定时任务的超时时间，客户端参数 [request.timeout.ms](http://request.timeout.ms/) 的值。另外还绑定了一个 [timerTaskEntry](http://timertaskentry/) 字段，每个定时任务都要知道，它存放在哪个 Bucket 链表下的哪个链表元素上。

总之，**它是 Kafka 中延迟请求的定时任务，是一个 Runnable 类，Kafka 使用一个单独线程异步添加延迟请到时间轮中**。

下面通过一张图来总结下这些类的关系：

  
![](https://article-images.zsxq.com/Fj9MKyH7ml8l767dxMxfDNa6W7AE)

基于上面这种设计，每个延迟请求需要根据自己的超时时间，来决定它要被保存于哪一层时间轮上。我们假设在 t = 0 时创建了第 1 层的时间轮，那么该层第 1 个 Bucket 保存的延迟请求就是介于 \[0，1）之间，第 2 个 Bucket 保存的是介于 \[1，2) 之间的请求。现在，如果有两个延迟请求，超时时刻分别在 18.5 毫秒和 123 毫秒，那么，第 1 个请求就应该被保存在第 1 层的第 19 个Bucket（序号从1开始）中，而第 2 个请求，则应该被保存在第 2 层时间轮的第 6 个 Bucket 中。

剖析完上面「**分层时间轮**」算法后，你是否感觉到了这个时间轮跟 Kafka 内部的「**主题**」、「**分区**」、「**副本**」并没有关联是吧。

实际上，延迟处理请求是 Kafka 服务端非常重要功能之一。此时你可能会问：**到底 Kafka 是如何创建和维护这个分层时间轮的呢？**

这是接下来的我们要剖析的重点。

##   
**05 延迟机制**

这里回答下上面的问题：

**首先 Timer 接口定义了管理延迟操作的相关方法，SystemTimer 实现了延迟操作的关键逻辑**。

另外的延迟请求类 DelayedOperation 调用「**分层时间轮**」上的各类操作，都是通过 SystemTimer 类完成的。

「**Timer**」类源码在 Kafka 源码包的 utils 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/utils/timer/Timer.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/utils/timer/Timer.scala)

## **5.1 Timer 接口**

trait Timer {

/\*\*

\* 将给定的定时任务插入到时间轮上，等待后续延迟执行

\*/

def add(timerTask: TimerTask): Unit

/\*\*

\* 向前推进时钟，执行已达过期时间的延迟任务

\*/

def advanceClock(timeoutMs: Long): Boolean

/\*\*

\* 获取时间轮上总的定时任务数

\*/

def size: Int

/\*\*

\* 关闭定时器

\*/

def shutdown(): Unit

}

这里总共定义了 4 个方法，最重要的两个方法是「**add**」和「**advanceClock**」，它们是**完成延迟请求处理的关键**。接下来，我会结合Timer 实现类 SystemTimer 的源码，重点来剖析这两个方法。

##   
**5.2 SystemTime 实现类**

SystemTimer 类是 Timer 接口的实现类。它是一个定时器类，内部封装了「**分层时间轮**」对象，为 Purgatory 提供延迟请求管理功能。

Purgatory 是指保存延迟请求的缓冲区。即它内部保存的是因不满足条件而无法完成，但又没有超时的请求。

## **5.2.1 SystemTime 定义**

@threadsafe

class SystemTimer(executorName: String,

tickMs: Long = 1,

wheelSize: Int = 20,

startMs: Long = Time.SYSTEM.hiResClockMs) extends Timer {

// 单线程的线程池用于异步执行定时任务

private\[this\] val taskExecutor \= Executors.newFixedThreadPool(1,

(runnable: Runnable) => KafkaThread.nonDaemon("executor-" + executorName, runnable))

// 延迟队列保存所有 Bucket，即所有 TimerTaskList 对象

private\[this\] val delayQueue \= new DelayQueue\[TimerTaskList\]()

// 总定时任务数

private\[this\] val taskCounter \= new AtomicInteger(0)

// 时间轮对象

private\[this\] val timingWheel \= new TimingWheel(

tickMs = tickMs,

wheelSize = wheelSize,

startMs = startMs,

taskCounter = taskCounter,

delayQueue

)

// 线程安全的读写锁

private\[this\] val readWriteLock \= new ReentrantReadWriteLock()

private\[this\] val readLock \= readWriteLock.readLock()

private\[this\] val writeLock \= readWriteLock.writeLock()

....

}

该类是**线程安全的类**，属性如下：

1.  **tickMs**：它表示一个槽位的时长，即向前推进一格的时间。在 Kafka 中，第 1 层时间轮的 tickMs 被固定为 1 毫秒，即向前推进一格 Bucket 的时长是 1 毫秒。
2.  **wheelSize**：它表示每一层时间轮上的 Bucket 数量。第 1 层的 Bucket 数量是 20。
3.  **executorName**：Kafka 中存在不同的 Purgatory，比如专门处理生产者延迟请求的 Produce 缓冲区、处理消费者延迟请求的 Fetch 缓冲区等。这里的 Produce 和 Fetch 就是 executorName。
4.  **startMs**：它表示 SystemTimer 定时器启动时间，单位是毫秒。
5.  **delayQueue**： 它保存了该定时器下管理的所有 Bucket 对象。正是 DelayQueue，所以只有在 Bucket 过期后，才能从该队列中获取到。而该类的 [advanceClock](http://advanceclock/) 方法正是依靠了这个特性向前驱动时钟。
6.  **timingWheel**：它是实现分层时间轮的实现类，上面已经剖析过，SystemTimer 类依靠它来操作分层时间轮。
7.  **taskExecutor**：它是单线程的线程池，用于异步执行提交的定时任务逻辑。

剖析完这些字段外，我们来看下该类的重要方法，看看它到底是如何实现延迟操作的。

## **5.2.2 add()**

def add(timerTask: TimerTask): Unit = {

// 首先获取读锁，在没有线程持有写锁的前提下，多个线程能够同时向时间轮添加定时任务

readLock.lock()

try {

// 调用 addTimerTaskEntry 执行插入逻辑

addTimerTaskEntry(new TimerTaskEntry(timerTask, timerTask.delayMs + Time.SYSTEM.hiResClockMs))

} finally {

// 释放读锁

readLock.unlock()

}

}

该方法主要用来**将给定的定时任务插入到时间轮中进行管理的**, 步骤如下：

1.  首先获取读锁，在没有线程持有写锁的前提下，多个线程能够同时向时间轮添加定时任务。
2.  调用 [addTimerTaskEntry](http://addtimertaskentry/) 将给定的 TimerTaskEntry 插入到时间轮中。
3.  如果失败，则释放读锁。

## **5.2.3 addTimerTaskEntry()**

private def addTimerTaskEntry(timerTaskEntry: TimerTaskEntry): Unit = {

// 首先判断是否添加成功

if (!timingWheel.add(timerTaskEntry)) {

// 如果添加失败，则判断定时任务是否已取消，如果没有则说明定时任务已过期否则添加肯定成功的

if (!timerTaskEntry.cancelled)

// 此时将定时任务添加到处理延迟任务的线程池中

taskExecutor.submit(timerTaskEntry.timerTask)

}

}

在上面剖析 [TimingWheel#add](http://timingwheel/#add) 方法时，它会在定时任务「**已取消**」或「**已过期**」时返回 false，否则会将定时任务添加到时间轮并返回 true。

所以这里需要根据定时任务的状态来判断执行逻辑：

1.  如果该任务既未取消也未过期，那么将其添加到时间轮。
2.  如果添加失败则判断该任务是否已取消，如果已取消则直接返回。
3.  如果该任务已经过期，则提交到相应的延迟处理线程池，等待后续执行。

## **5.2.4 advanceClock()**

def advanceClock(timeoutMs: Long): Boolean = {

// 从延迟队列 delayQueue 中取出下一个已过期的 Bucket

var bucket \= delayQueue.poll(timeoutMs, TimeUnit.MILLISECONDS)

// 如果不为空

if (bucket != null) {

// 获取写锁，一旦有线程持有写锁，其他任何线程执行 add 或 advanceClock 方法时会阻塞

writeLock.lock()

try {

// 如果 bucket 不为空，则一直重复执行

while (bucket != null) {

// 推动时间轮向前推进到 Bucket 的过期时间点，这里只需要修改 TimingWheel#currentTime 即可。

timingWheel.advanceClock(bucket.getExpiration)

// 将该 Bucket 下的所有定时任务重写回到时间轮

bucket.flush(addTimerTaskEntry)

// 读取下一个 Bucket 对象

bucket = delayQueue.poll()

}

} finally {

// 释放写锁

writeLock.unlock()

}

true

} else {

false

}

}

这里通过一张图来梳理下其执行流程：

![](https://article-images.zsxq.com/Fp9AOkKuiZzObjZ51QEQkBKqQI8o)

该方法主要用来**遍历 delayQueue 中的所有 Bucket，并将时间轮的时钟一次推进到它们的过期时间点并让它们过期，最后再将这些 Bucket 的所有定时任务全部重新插入回时间轮中**。

这里的重点操作是下面这两步：

timingWheel.advanceClock(bucket.getExpiration)

bucket.flush(addTimerTaskEntry)

第一步用来推动时间轮指针前进，这里只需要修改 TimingWheel#currentTime。

第二步会遍历 bucket 下的所有任务，执行以下操作：

1.  调用 timingWheel#add 方法尝试将任务「**降层**」添加时间轮中。
2.  如果添加失败，则说明任务已经到期，此时调用 SystemTimer#taskExecutor 执行该到期定时任务。

假设当前时间为 T，添加一个到期时间为 45 ms 的任务，在一开始时，该任务会放入到「**第二层**」时间轮 Wheel\[2\]，bucketId 槽的到期时间为 T + 40，当实际实际到达 T + 40 后，SystemTimer 将该 Bucket 从延迟队列中取出，再次调用 [timingWheel#add](http://timingwheel/#add), 此时会将任务放入到「**第一层**」时间轮 Wheel\[5\]，从而实现了时间轮的「**降层**」。

> 注意这里将修改时间轮 currentTime，可以认为将指针移动到指定位置， Kafka 做了一个很巧妙的设计，只有当某个 Bucket 过期时，才会将该指针移动到对应位置，即推进时间轮。

到这里，**你是否会有疑惑：如果时间轮长时间没有过期的 bucket，那么时间轮指针长时间不推动，此时指针当前时间将落后于实际实际，会有问题吗？**

这里举例说明下：

假设当前时间为 T，时间轮中的 currentTime 也是 T，此时过了 10 ms后，时间轮指针一直没有进行推动，实际时间已经为 T + 10 了，那么此时**落后了10 ms**。

现在添加一个到期时间为 15 ms 的任务，即任务到期时间为 T + 25 ms，本来该任务应该被添加到「**第一层**」时间轮 Wheel\[15\]中 「**实际 currentTime = T + 10，bucket 位置 = T + 25 - 10 = 15**」，由于时间轮 currentTime落后了 10 ms，该任务被添加到 「**第二层**」时间轮 Wheel\[1\] 中。该 bucket 槽过期时间为 T + 20 ms，那么在过去10 ms 后 ，实际时间到达 T + 20 ms时，该槽位过期了，此时调用 [timingWheel.advanceClock()](http://timingwheel.advanceclock\(bucket.getexpiration\)/) 将时间轮 currentTime 改为 T + 20 ms，并将任务重新添加到「**第一层**」 Wheel\[5\] 中。

因此，当该任务最终在 T + 25 ms 时间执行时，并不会出现错误。

可以得出，虽然时间轮 **currentTime** 可能小于实际时间，导致任务被放到更高层的时间轮中，但该任务会被降层，时间轮 **currentTime** 也会被改正，最终保证任务在正确的时间被执行。**这真是一个很巧妙的设计**。

总的来说，SystemTimer 类实现了 Timer 接口的相关方法，**它封装了底层的分层时间轮，并为上层调用放提供了便捷的方法来进行操作时间轮**。

剖析到这里，你是否会接着问，**它的上层调用方是谁呢？**

这是接下来重点剖析的类，即 [DelayedOperationPurgatory](http://delayedoperationpurgatory/) 类。它就是我们存储 Purgatory 的地方。

在剖析该类之前，我们先来剖析下另外一个重要类：[DelayedOperation](http://delayedoperation/)。

## **5.3 DelayedOperation 类**

「**DelayedOperation**」类源码在 Kafka 源码包的 utils 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/DelayedOperation.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/DelayedOperation.scala)

## **5.3.1 DelayedOperation 类定义**

abstract class DelayedOperation(override val delayMs: Long,

lockOpt: Option\[Lock\] = None)

extends TimerTask with Logging {

// 标识该延迟操作是否已经完成

private val completed \= new AtomicBoolean(false)

// 防止多个线程同时检查操作是否可完成时发生锁竞争导致操作最终超时

private\[server\] val lock: Lock = lockOpt.getOrElse(new ReentrantLock)

....

}

它是一个抽象类，在构造函数中需要传入一个超时时间，这个超时时间通常是**客户端发出请求的超时时间**，即客户端参数 [request.timeout.ms](http://request.timeout.ms/) 值。可以看到该类继承了 [TimerTask](http://timertask/) 接口，作为一个延迟操作类，它自动继承了 TimerTask#cancel 方法，支持延迟操作的取消。

接下来，我们挨个剖析下其重要方法。

## **5.3.2 forceComplete()**

def forceComplete(): Boolean = {

// 使用 compareAndSet 方法比较并设置 completed 的值。如果 completed 的值尚未被设置为 true，则将其设置为 true，并继续执行以下操作。否则，直接返回 false。

if (completed.compareAndSet(false, true)) {

// 在成功将 completed 的值设置为 true 后，执行以下操作：

// 1、取消超时计时器。调用 cancel() 方法取消任务的超时计时器。

// 2、执行完成时的操作。调用 onComplete() 方法执行任务完成时的操作。

// 返回 true，表示任务成功标记为完成状态。

cancel()

onComplete()

true

} else {

// 如果 completed 的值在调用 compareAndSet 方法之前已经是 true，则直接返回 false，表示任务无法强制标记为完成状态。

false

}

}

该方法主要用来**强制完成延迟操作**，不管它是否满足完成条件。每当操作满足完成条件或已经过期了，就需要调用该方法完成该操作。

## **5.3.4 safeTryCompleteOrElse()**

// 以安全的方式尝试完成任务，否则执行提供的函数

private\[server\] def safeTryCompleteOrElse(f: => Unit): Boolean = inLock(lock) {

if (tryComplete()) true

else {

f

// 最后一次完成检查

tryComplete()

}

}

该方法主要用来**在进行任务完成的过程中保证线程安全，并确保任务能够正确的完成**，步骤如下：

1.  首先，在锁（lock）的保护下执行以下操作：
2.  尝试完成任务。调用 tryComplete() 方法尝试完成任务，如果成功完成任务，则返回 true。
3.  如果无法完成任务，则执行提供的函数 f。这个函数会在任务无法完成时执行，以执行一些特定的逻辑。
4.  在执行了函数 f 之后，再次进行一次完成检查。调用 tryComplete() 方法对任务进行最后一次完成检查。
5.  如果最后一次完成检查成功，表示任务已经完成并返回 true。否则，表示任务仍未完成，并返回 false。

## **5.3.5 run()**

// 重写 run 方法，用于执行定时任务

override def run(): Unit = {

// 强制标记任务为完成状态

if (forceComplete())

// 在任务超时时执行的操作

onExpiration()

}

该方法重写了 run() 方法，用于执行定时任务。

1.  首先调用 forceComplete() 方法，尝试强制标记任务为完成状态。如果成功地标记任务为完成状态，即任务的 completed 标志被设置为 true，则继续执行以下操作。否则，直接返回。
2.  在任务被成功标记为完成状态后，调用 onExpiration() 方法，在任务超时时执行的操作。该方法用于处理任务超时时需要执行的逻辑。

> 注意，只有当任务强制标记为完成状态成功时，才会执行 onExpiration() 方法。这可以确保仅当定时任务超时并完成时，才会执行超时时的操作。

## **5.3.6 其他方法**

// 检查延迟操作是否已经完成来决定后续如何处理该操作。比如如果操作已经完成了，那么通常需要取消该操作。

def isCompleted: Boolean = completed.get()

// 强制完成之后执行的过期逻辑回调方法。只有真正完成操作的那个线程才有资格调用这个方法。

def onExpiration(): Unit

// 完成延迟操作所需的处理逻辑。该方法只会在 forceComplete 方法中被调用。

def onComplete(): Unit

// 尝试完成延迟操作的顶层方法，内部会调用forceComplete方法

def tryComplete(): Boolean

// 线程安全版本的tryComplete方法

private\[server\] def safeTryComplete(): Boolean = inLock(lock)(tryComplete())

至此，[DelayedOperation](http://delayedoperation%20/) 类就剖析完了。

这里你可以认为它是一个延迟操作，通常有两个操作结果：

1.  **正常结束**：在到期时间到达前，延迟操作的结束条件已经满足，操作可以正常结束。比如在 DelayProduce 中，当 ISR 副本集合都同步了写入消息后，可以正常结束该操作，请成功响应返回给生产者。
2.  **到期结束**：直到到期时间到达后，延迟操作的结束条件仍未满足，操作只能到期结束。比如在 DelayProduce 中，当到达最大延迟时间后，ISR 副本集合还没有全部同步写入消息，便返回异常响应给生产者。

接下来我们来结合子类看看具体延迟请求类是如何实现 tryComplete 方法。

## **5.4 DelayedProduce 类**

「**DelayedProduce**」类源码在 Kafka 源码包的 utils 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/DelayedProduce.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/server/DelayedProduce.scala)

它是用来处理生产者延迟请求的，比如 [acks=all](http://acks=all/) 的 [PRODUCE](http://produce/) 请求很容易就成为延迟请求，因为它必须等待所有的「**ISR 副本集合**」全部同步消息之后才能真正完成。

## **5.4.1 DelayedProduce 类定义**

class DelayedProduce(delayMs: Long,

produceMetadata: ProduceMetadata,

replicaManager: ReplicaManager,

responseCallback: Map\[TopicPartition, PartitionResponse\] => Unit,

lockOpt: Option\[Lock\] = None)

extends DelayedOperation(delayMs, lockOpt) {

// 遍历了 produceMetadata.produceStatus 对象中的每个键值对，并对每个状态进行处理。

produceMetadata.produceStatus.forKeyValue { (topicPartition, status) =>

// 对于每个状态，首先检查其 responseStatus.error 字段是否为 Errors.NONE。如果错误是 Errors.NONE，表示没有发生错误，则执行以下操作：

if (status.responseStatus.error == Errors.NONE) {

// 将 status.acksPending 设置为 true，表示等待确认。

status.acksPending = true

// 将 status.responseStatus.error 设置为 Errors.REQUEST\_TIMED\_OUT，表示请求超时。

status.responseStatus.error = Errors.REQUEST\_TIMED\_OUT

} else {

// 如果错误不是 Errors.NONE，表示发生了错误，则执行以下操作：

// 将 status.acksPending 设置为 false，表示不需要等待确认。

status.acksPending = false

}

trace(s"Initial partition status for $topicPartition is $status")

}

....

}

## **5.4.2 tryComplete()**

/\*\*

\* The delayed produce operation can be completed if every partition

\* it produces to is satisfied by one of the following:

\*

\* Case A: This broker is no longer the leader: set an error in response

\* Case B: This broker is the leader:

\* B.1 - If there was a local error thrown while checking if at least requiredAcks

\* replicas have caught up to this operation: set an error in response

\* B.2 - Otherwise, set the response with no error.

\*/

// 尝试完成任务

override def tryComplete(): Boolean = {

// 为每个分区检查是否仍有待确认的消息

produceMetadata.produceStatus.forKeyValue { (topicPartition, status) =>

trace(s"Checking produce satisfaction for $topicPartition, current status $status")

// 跳过已满足的分区

if (status.acksPending) {

val (hasEnough, error) = replicaManager.getPartitionOrError(topicPartition) match {

case Left(err) =>

// Case A : 对于无可用分区的情况，将 hasEnough 设置为 false，错误设置为对应错误

(false, err)

case Right(partition) =>

// Case B : 验证分区是否满足 ACK，如果满足，则将 hasEnough 设置为 true，否则为 false

partition.checkEnoughReplicasReachOffset(status.requiredOffset)

}

// 处理 Case B.1 和 B.2 的情况，并标记为不再需要确认

if (error != Errors.NONE || hasEnough) {

status.acksPending = false

status.responseStatus.error = error

}

}

}

// 检查每个分区是否已满足 Case A 和 Case B 中的至少一种情况

if (!produceMetadata.produceStatus.values.exists(\_.acksPending))

forceComplete()

else

false

}

该方法主要用来**尝试完成任务**，步骤如下：

1.  首先，遍历 produceMetadata.produceStatus 中的每个分区状态，对每个状态进行检查，并尝试标记为完成状态。
2.  对于每个状态，首先检查 acksPending 标志是否为 true，该标志用于指示状态中是否有待确认的消息。如果该标志为 true，则对状态进行进一步处理。
3.  然后，调用 replicaManager.getPartitionOrError() 方法获取对应分区的副本管理器。如果出现错误，则将 hasEnough 设置为 false，并将 error 设置为对应的错误。
4.  如果没有错误，则调用 partition.checkEnoughReplicasReachOffset() 方法检查分区的副本是否达到了指定的偏移量。如果达到了，则将 hasEnough 设置为 true 否则为 false。
5.  最后，在满足 Case A 和 Case B 中至少一种情况的前提下，将 acksPending 标志设置为 false，表示不再需要等待确认，并将错误设置为 error。
6.  接下来，检查每个分区是否已满足 Case A 和 Case B 中的至少一种情况。如果已满足，则尝试强制标记任务为完成状态，并返回 true。否则，返回 false。

> 通过遍历每个分区状态并更新 acksPending 标志，该方法可确保任务被标记为完成状态，并对状态进行必要的更新来保持数据的一致性。

## **5.4.3 onExpiration()**

// 在任务超时时执行的操作

override def onExpiration(): Unit = {

// 遍历 produceMetadata.produceStatus，查找仍有待确认消息的分区

produceMetadata.produceStatus.forKeyValue { (topicPartition, status) =>

if (status.acksPending) {

// 输出调试日志，显示哪些分区的请求已过期

debug(s"Expiring produce request for partition $topicPartition with status $status")

// 记录延迟产生指标，这些指标用于度量消息发送的延迟。

DelayedProduceMetrics.recordExpiration(topicPartition)

}

}

}

该方法主要**在任务超时时执行的操作，用来处理产生请求超时的情况**，步骤如下：

1.  遍历 produceMetadata.produceStatus 对象中的每个键值对，并查找仍有待确认消息的分区。
2.  对于每个带有未确认消息的分区，首先在调试日志中记录超时的分区，以便进行排查和调试。
3.  然后，记录延迟生产指标，这些指标用于度量消息发送的延迟。

> 通过记录延迟产生的指标，可以了解消息发送的延迟情况，并进一步分析和优化发送流程，从而提高整个系统的性能和稳定性。

## **5.4.4 onComplete()**

// 任务完成时执行的操作

override def onComplete(): Unit = {

// 从 produceMetadata.produceStatus 中提取 responseStatus

val responseStatus \= produceMetadata.produceStatus.map { case (k, status) => k -> status.responseStatus }

// 调用 responseCallback 方法，将 responseStatus 作为参数传递给回调函数

responseCallback(responseStatus)

}

该方法主要**在任务完成时执行的操作**，步骤如下：

1.  从 produceMetadata.produceStatus 对象中提取 responseStatus，其中 responseStatus 是一个键值对集合，包含了每个分区的响应状态。
2.  然后，调用 responseCallback 方法，将 responseStatus 作为参数传递给回调函数。

> 通过将响应状态传递给回调函数，可以根据实际需求进行后续的处理逻辑，例如发送响应给客户端、记录日志等。这样可以实现对任务完成的灵活处理，并根据需要进行相应的操作。

最后我们再来剖析下 [DelayedOperationPurgatory](http://delayedoperationpurgatory/) 类。

## **5.5 DelayedOprationPurgatory 类**

该类是实现 Purgatory 的地方。从源码结构上看，它是一个 Scala 伴生对象。也就是说，源码文件同时定义了[Object DelayedOperationPurgatory](http://object%20delayedoperationpurgatory/) 和 [Class DelayedOperationPurgatory](http://class%20delayedoperationpurgatory/)。

Object 中定义了 apply 工厂方法和 Shards 的字段，该字段是 DelayedOperationPurgatory 用来监控列表的数组长度信息的。

这里重点剖析下 [Class DelayedOperationPurgatory](http://class%20delayedoperationpurgatory/) 的源码。

[DelayedOperationPurgatory](http://delayedoperationpurgatory/) 类是一个泛型类，它的参数类型是 DelayedOperation 的具体子类。通常情况下，每一类延迟请求都对应于一个 DelayedOperationPurgatory 实例，这些实例一般都保存在上层的管理器中。

> 比如，与消费者组相关的心跳请求、加入组请求的Purgatory实例，就保存在GroupCoordinator组件中，而与生产者相关的 PRODUCE 请求的 Purgatory 实例，被保存在分区对象或副本状态机中。

## **5.5.1 DelayedOprationPurgatory 类定义**

final class DelayedOperationPurgatory\[T <: DelayedOperation\](

purgatoryName: String,

timeoutTimer: Timer,

brokerId: Int = 0,

purgeInterval: Int = 1000,

reaperEnabled: Boolean = true,

timerEnabled: Boolean = true)

extends Logging with KafkaMetricsGroup {

字段如下：

1.  **purgatoryName**：它表示 purgatory 名称。
2.  **timeoutTimer**：它表示 SystemTimer 实例。
3.  **brokerId**：它表示 Broker 序号。
4.  **purgeInterval**：它用来控制删除线程移除 Bucket 中的过期延迟请求的频率，在绝大部分情况下，都是 1 秒一次。当然，对于生产者、消费者以及删除消息的 AdminClient 而言，Kafka 分别定义了专属的参数允许你调整这个频率。比如，生产者参数 [producer.purgatory.purge.interval.requests](http://producer.purgatory.purge.interval.requests/) 。
5.  **preaperEnabled**：是否启动删除线程，这里默认启动。
6.  **timerEnabled**：是否启用分层时间轮，这里默认启动。

另外在该类中定义了两个内置类，我们分别来剖析下，待会再来剖析 [DelayedOperationPurgatory](http://delayedoperationpurgatory/) 类的方法。

## **5.5.2 Watchers 类**

Watchers 是**基于 key 的延迟请求的监控链表辅助类**，源码如下：

private class Watchers(val key: Any) {

private\[this\] val operations \= new ConcurrentLinkedQueue\[T\]()

// 计算当前正在监控的操作数量。这是一个 O(n) 的操作，如果可能，应使用 isEmpty() 方法来检查是否为空。

def countWatched: Int = operations.size

// 检查监视列表是否为空

def isEmpty: Boolean = operations.isEmpty

// 添加要监控的元素

def watch(t: T): Unit = {

operations.add(t)

}

// 遍历监控列表，并尝试完成一些已监控的元素

def tryCompleteWatched(): Int = {

var completed \= 0

val iter \= operations.iterator()

while (iter.hasNext) {

val curr \= iter.next()

if (curr.isCompleted) {

// 其他线程已经完成了这个操作，只需将其移除

iter.remove()

} else if (curr.safeTryComplete()) {

iter.remove()

completed += 1

}

}

// 如果监控列表为空，则从全局列表中移除该键

if (operations.isEmpty)

removeKeyIfEmpty(key, this)

completed

}

// 取消监控列表中的所有元素

def cancel(): List\[T\] = {

val iter \= operations.iterator()

val cancelled \= new ListBuffer\[T\]()

while (iter.hasNext) {

val curr \= iter.next()

curr.cancel()

iter.remove()

cancelled += curr

}

cancelled.toList

}

// 遍历监控列表，并清除已被其他线程完成的元素

def purgeCompleted(): Int = {

var purged \= 0

val iter \= operations.iterator()

while (iter.hasNext) {

val curr \= iter.next()

if (curr.isCompleted) {

iter.remove()

purged += 1

}

}

// 如果监控列表为空，则从全局列表中移除该键

if (operations.isEmpty)

removeKeyIfEmpty(key, this)

purged

}

}

每个 Watchers 实例都定义了一个延迟请求链表，而 Key 可以是任何类型，比如「**消费者组的字符串类型**」、「**主题分区的 TopicPartitionOperationKey 类型**」。这里你只需要了解，Watchers 是一个通用的延迟请求链表。Kafka 利用它来**监控保存在其中的延迟请求的可完成状态**。

既然 Watchers 主要的数据结构是链表，它的所有方法本质上就是一个链表操作，如下：

1.  countWatched：计算当前正在监控的操作数量。
2.  isEmpty：检查监控列表是否为空。
3.  watch：添加要监控的延迟请求。
4.  tryCompleteWatched：遍历监控列表，并尝试完成其中的延迟请求。对于已经完成的延迟请求，将其移除并返回已完成的延迟请求数量。
5.  cancel：取消监控列表中的延迟请求。对于每个延迟请求，先取消它，然后将其从监控列表中移除，并将其添加到一个列表中，最后返回该列表。
6.  purgeCompleted：遍历监控列表，并清除已被其他线程完成的请求。对于已经完成的延迟请求，将其从监控列表中移除，并返回已清除的请求数量。

##   
**5.5.3 WatcherList 类**

WatcherList 非常短小精悍，源码如下：

private class WatcherList {

// 定义一组按照 Key 分组的 Watchers 对象

val watchersByKey \= new Pool\[Any, Watchers\](Some((key: Any) => new Watchers(key)))

val watchersLock \= new ReentrantLock()

// 返回所有 Watchers 对象

def allWatchers \= {

watchersByKey.values

}

}

WatcherList 最重要的字段是 **watchersByKey**。它是一个 Pool 池，本质上是一个 ConcurrentHashMap。watchersByKey 的 Key 可以是任何类型，而 Value 就是 Key 对应类型的一组 Watchers 对象。

##   
**5.5.4 tryCompleteElseWatch()**

def tryCompleteElseWatch(operation: T, watchKeys: Seq\[Any\]): Boolean = {

assert(watchKeys.nonEmpty, "The watch key list can't be empty")

// The cost of tryComplete() is typically proportional to the number of keys. Calling tryComplete() for each key is

// going to be expensive if there are many keys. Instead, we do the check in the following way through safeTryCompleteOrElse().

// If the operation is not completed, we just add the operation to all keys. Then we call tryComplete() again. At

// this time, if the operation is still not completed, we are guaranteed that it won't miss any future triggering

// event since the operation is already on the watcher list for all keys.

//

// ==============\[story about lock\]==============

// Through safeTryCompleteOrElse(), we hold the operation's lock while adding the operation to watch list and doing

// the tryComplete() check. This is to avoid a potential deadlock between the callers to tryCompleteElseWatch() and

// checkAndComplete(). For example, the following deadlock can happen if the lock is only held for the final tryComplete()

// 1) thread\_a holds readlock of stateLock from TransactionStateManager

// 2) thread\_a is executing tryCompleteElseWatch()

// 3) thread\_a adds op to watch list

// 4) thread\_b requires writelock of stateLock from TransactionStateManager (blocked by thread\_a)

// 5) thread\_c calls checkAndComplete() and holds lock of op

// 6) thread\_c is waiting readlock of stateLock to complete op (blocked by thread\_b)

// 7) thread\_a is waiting lock of op to call the final tryComplete() (blocked by thread\_c)

//

// Note that even with the current approach, deadlocks could still be introduced. For example,

// 1) thread\_a calls tryCompleteElseWatch() and gets lock of op

// 2) thread\_a adds op to watch list

// 3) thread\_a calls op#tryComplete and tries to require lock\_b

// 4) thread\_b holds lock\_b and calls checkAndComplete()

// 5) thread\_b sees op from watch list

// 6) thread\_b needs lock of op

// To avoid the above scenario, we recommend DelayedOperationPurgatory.checkAndComplete() be called without holding

// any exclusive lock. Since DelayedOperationPurgatory.checkAndComplete() completes delayed operations asynchronously,

// holding a exclusive lock to make the call is often unnecessary.

// 尝试完成操作，如果操作已经完成，则返回 true

if (operation.safeTryCompleteOrElse {

// 对于每个 watchKey，将操作添加到对应的监控列表中

watchKeys.foreach(key => watchForOperation(key, operation))

// 如果有 watchKey，则增加估计的总操作数

if (watchKeys.nonEmpty) estimatedTotalOperations.incrementAndGet()

}) return true

// 如果依然不能完成此请求，将其加入到过期队列

if (!operation.isCompleted) {

// 如果启用了定时器，则将操作添加到超时定时器中

if (timerEnabled)

timeoutTimer.add(operation)

if (operation.isCompleted) {

// 如果操作在添加到定时器之前已经完成，则取消定时器任务

operation.cancel()

}

}

false

}

该方法**先尝试完成请求，如果无法完成，则将其加入到 WatcherList 中进行监控**。步骤如下：

1.  判断 watchKeys 列表是否为空，如果为空则抛出断言异常。
2.  使用 operation 的 safeTryCompleteOrElse 方法尝试完成操作。如果操作已经成功完成，则执行以下操作：
3.  对于 watchKeys 列表中的每个 key，调用 watchForOperation 方法将操作添加到对应的监控列表中。
4.  如果 watchKeys 列表不为空，则通过 estimatedTotalOperations AtomicInteger 增加估计的总操作数。
5.  如果操作未能在 tryCompleteElseWatch 步骤中完成，即被添加到了监控列表中：
6.  如果定时器启用了，将 DelayedOperation 添加到该类 timeoutTimer 属性指向的 SystemTimer 定时器中，并指定操作的到期时间，任务到期后，时间轮会调用 DelayedOperation#run 方法执行到期结束的逻辑。
7.  检查操作是否已经在添加到定时器之前完成，如果已经完成，则取消定时器任务。
8.  返回 false 表示操作未能立即完成。

总的来看，你要掌握这个方法要做的两个事情：

1.  先尝试完成延迟请求。
2.  如果不行，就加入到 WatcherList，等待后面再试。

那么，**在哪里进行重试的呢？**是在哪里进行重试的呢？这就是接下来要剖析的方法了。

## **5.5.5 checkAndComplete()**

def checkAndComplete(key: Any): Int = {

// 获取给定 Key 的 WatcherList

val wl \= watcherList(key)

// 获取 WatcherList 中 Key 对应的 Watchers 对象实例

val watchers \= inLock(wl.watchersLock) { wl.watchersByKey.get(key) }

// 尝试完成满足完成条件的延迟请求并返回成功完成的请求数

val numCompleted \= if (watchers == null)

0

else

watchers.tryCompleteWatched()

debug(s"Request key $key unblocked $numCompleted $purgatoryName operations")

numCompleted

}

该方法会**先检查给定 Key 所在的 WatcherList 中的延迟请求是否满足完成条件**，步骤如下：

1.  根据给定Key，获取对应的WatcherList对象，以及它下面保存的Watchers对象实例。
2.  然后尝试完成满足完成条件的延迟请求。
3.  返回成功完成的请求数。

这里的重点是：**调用 Watchers#tryCompleteWatched 方法，去尝试完成哪些已满足完成条件的延迟请求**。

##   
**5.5.6 cancelForKey()**

def cancelForKey(key: Any): List\[T\] = {

// 根据 key 获取对应的监控列表 wl。

val wl \= watcherList(key)

// 使用 wl 的 watchersLock 进行同步操作，确保线程安全

inLock(wl.watchersLock) {

// 从 wl 的 watchersByKey 中移除 key 并获取与之关联的监控操作列表 watchers

val watchers \= wl.watchersByKey.remove(key)

// 如果监控操作列表 watchers 不为空，即存在与 key 关联的监控操作

if (watchers != null)

// 取消所有监控操作

watchers.cancel()

else

Nil

}

}

该方法**先用来取消特定 key 关联的监控操作**，步骤如下：

1.  根据 key 获取对应的监控列表 wl。
2.  使用 wl 的 watchersLock 进行同步操作，确保线程安全。
3.  从 wl 的 watchersByKey 中移除 key 并获取与之关联的监控操作列表 watchers。
4.  如果监控操作列表 watchers 不为空，即存在与 key 关联的监控操作：
5.  调用 watchers 的 cancel 方法，取消所有监控操作。
6.  如果监控操作列表 watchers 为空，表示 key 没有关联的监控操作，则返回一个空列表。

## **5.5.7 cancelForKey()**

private def watchForOperation(key: Any, operation: T): Unit = {

val wl \= watchList(key) // 获取 key 对应的监控列表 wl

inLock(wl.watchersLock) { // 使用 wl 的 watchersLock 进行同步操作

val watcher \= wl.watchersByKey.getAndMaybePut(key) // 获取 key 对应的监控器 watcher，如果不存在则创建一个新的监控器并加入监控列表

watcher.watch(operation) // 将操作添加到监控器中进行监控

}

}

该方法**对给定 Key 对应的操作进行监控**，步骤如下：

1.  通过调用 watchList(key) 方法获取给定 key 对应的监控列表 wl。
2.  通过 wl.synchornized(wl.watchersLock){...} 同步块来保证线程安全。
3.  获取 key 对应的监控器 watcher，并将其加入监控列表 wl 中。其中，getAndMaybePut 函数是一个原子操作，如果 key 对应的监控器不存在，则创建一个并加入 wl 的 watchersByKey 中。
4.  将操作 operation 添加到监控器 watcher 中进行监听监控。即将操作 operation 添加到和 key 关联的监控操作列表中。

## **5.5.8 removeKeyIfEmpty()**

private def removeKeyIfEmpty(key: Any, watchers: Watchers): Unit = {

val wl \= watcherList(key) // 获取 key 对应的监控列表 wl

inLock(wl.watchersLock) { // 使用 wl 的 watchersLock 进行同步操作// 如果当前 key 不再与要移除的监控器相关联，则跳过if (wl.watchersByKey.get(key) != watchers)

returnif (watchers != null && watchers.isEmpty) {

// 如果监控器不为空且已经没有任何监控操作了

wl.watchersByKey.remove(key) // 从 wl 的 watchersByKey 中移除 key

}

}

}

该方法**用来在监控列表为空时，将 key 进行移除**，步骤如下：

1.  通过调用 watcherList(key) 方法获取给定 key 对应的监控列表 wl。
2.  使用 wl 的 watchersLock 进行同步操作，确保线程安全。
3.  检查当前 key 是否仍与要移除的监控器 watchers 相关联。如果不相关，则直接返回。
4.  如果监控器 watchers 不为 null 且已经没有任何监控操作，则执行以下操作：
5.  从 wl 的 watchersByKey 中移除 key，即移除和 key 相关联的监控器。

## **5.5.9 advanceClock()**

def advanceClock(timeoutMs: Long): Unit = {

timeoutTimer.advanceClock(timeoutMs) // 推进超时定时器的时钟到 timeoutMs

// 如果已完成但仍在监控中的操作数（estimatedTotalOperations - numDelayed）大于清理阈值（purgeInterval）if (estimatedTotalOperations.get - numDelayed > purgeInterval) {

// 将 estimatedTotalOperations 重新设置为待处理操作（numDelayed）的数量，因为接下来要清理监控列表

estimatedTotalOperations.getAndSet(numDelayed)

debug("Begin purging watch lists")

// 对所有监控列表进行遍历，并清理已经完成的操作val purged = watcherLists.foldLeft(0) {

case (sum, watcherList) => sum + watcherList.allWatchers.map(\_.purgeCompleted()).sum

}

debug("Purged %d elements from watch lists.".format(purged))

}

}

该方法**用来推进超时定时器的时钟并进行监控列表的清理**，步骤如下：

1.  调用 timeoutTimer 的 advanceClock(timeoutMs) 方法，将超时定时器的时钟推进到 timeoutMs。
2.  如果已完成但仍在监控中的操作数（estimatedTotalOperations - numDelayed）大于清理阈值（purgeInterval），则执行以下操作：
3.  将 estimatedTotalOperations 重新设置为待处理操作（numDelayed）的数量，因为接下来要清理监控列表。
4.  遍历所有监控列表，并清理已经完成的操作。其中，map(\_.purgeCompleted()) 用于遍历监控器列表中所有监控器并清理其中已经完成的操作。
5.  返回清理操作的数量 purged。

## **5.5.10 过期操作清理线程()**

// 创建一个 ExpiredOperationReaper 实例作为清理线程，并定义线程名private val expirationReaper = new ExpiredOperationReaper()

private class ExpiredOperationReaper extends ShutdownableThread(

"ExpirationReaper-%d-%s".format(brokerId, purgatoryName), false) {

// 重写 ShutdownableThread 中的 doWork() 方法，定义清理线程的具体工作任务override def doWork(): Unit = {

advanceClock(200L) // 定期推进时钟并进行监控器列表的清理

}

}

这里定义了一个 ExpiredOperationReaper 线程类，主要用于定期进行监控器列表的清理工作。具体步骤为：在 doWork() 方法中调用 advanceClock(timeoutMs) 方法推进时钟并进行监控器列表的清理，定期执行该线程中的清理任务。

至此，[DelayedOperationPurgatory](http://delayedoperationpurgatory/) 类就剖析完了，在之前剖析 ISR 副本同步流程中，当 ISR 副本同步后， 「**Leader 副本**」会调用 [ReplicaManager#completeDelayedFetchOrProduceRequests](http://replicamanager/#completeDelayedFetchOrProduceRequests) 的 [delayedProducePurgatory.checkAndComplete](http://delayedproducepurgatory.checkandcomplete/) 方法正常结束对应的 DelayedProduce 操作。

![](https://article-images.zsxq.com/FpCG6N8lmV5xMD5gyHmD2GBhWbH8)

##   
**06 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从生产者设置「**ack=all**」配置，**Kafka 必须确保 ISR 副本集合中的所有副本都要成功响应这次写入请求才是真正完成**引出了「**延迟机制**」。

2、接着带大家剖析了「**延迟请求的业界通用实现方案--时间轮**」。

3、接着带大家深度剖析了「**时间轮算法**」的实现方式：「**简单时间轮**」、「**分层时间轮**」。

3、接着带大家深度剖析了 Kafka 「**分层时间轮**」算法，Kafka 正是利用这套「**分层时间轮**」算法实现了对于延迟请求的处理。在源码层级上，Kafka 定义了 4 个类来构建整套「**分层时间轮**」。

1.  TimerTask 类：Kafka 延时请求。它是一个 Runnable 类，Kafka 使用一个单独线程异步添加延时请求到时间轮。
2.  TimerTaskEntry 类：时间轮 Bucket 下延时请求链表的元素类型，封装了 TimerTask 对象和定时任务的过期时间戳信息。
3.  TimerTaskList 类：时间轮 Bucket 下的延时请求双向循环链表，提供 O(1) 时间复杂度的请求插入和删除。
4.  TimingWheel 类：时间轮类型，统一管理下辖的所有 Bucket 以及定时任务。

4、最后带大家深度剖析了「**分层时间轮**」的上层组件，包括 Timer 接口及其实现类 SystemTimer、DelayedOperation 类、DelayedProdude 类以及 DelayedOperationPurgatory 类。DelayedOperation调用SystemTimer类，DelayedOperationPurgatory管理DelayedOperation。它们共同实现了 Broker 端对于延迟请求的处理：**如果能立即完成的请求马上完成，否则就放入到 Purgatory 的缓冲区中**。后续，DelayedOperationPurgatory 类的方法会自动地处理这些延迟请求。

最后来一个关系调用图：

  
![](https://article-images.zsxq.com/FgBb3g-8AhJcIZwgzn-DpMm0GoKu)

下篇我们来深度剖析「**Broker 异步更新元数据实现原理**」，大家期待，我们下期见。