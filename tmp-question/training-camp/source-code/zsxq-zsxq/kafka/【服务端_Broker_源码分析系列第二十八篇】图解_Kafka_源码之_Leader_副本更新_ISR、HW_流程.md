大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端 副本同步实现原理**」的下半部分，了解了 Follower 副本是如何从 Leader 副本进行拉取消息的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 Leader 副本更新 ISR、HW 流程**」，看看「**Leader 副本**」是如何处理「**Follower 副本**」的 Fetch 请求的。

![](https://article-images.zsxq.com/FqD3IAU_2SBRl8vIgIb2ZSp4Km2o)

## **01 总体概述**

当 「**KafkaController**」启动之后，就会选举分区 Leader 副本，剩余的就会成为 Follower 副本。其中 Leader 副本负责消息收发，而 Follower 副本只负责拉取 Leader 副本上的消息内容，并保存到自己的 Log 日志中。当 Leader 副本挂了的时候，会从 ISR副本集合列表中选举出一个充当 Leader 副本，继续支持消息的收发功能。

这是一种的典型的主从同步机制，是分布式系统中常用的保证数据安全的机制。

那么 **Kafka Leader 副本是如何处理 Follower 副本发送过来的 Fetch 请求的呢？**

带着这个问题，我们开启今天的话题，继续来聊聊「**副本处理**」的实现机制。

##   
**02 与消费者 Fetch 请求区别**

在 [【服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html) 这篇中，我带大家剖析了消费者 Fetch 请求的处理流程。

这里介绍下「**Leader 副本**」处理「**Follower 副本**」的 Fetch 请求的处理流程的区别如下：

1.  「**Follower 副本**」可以读取的位置为 Log#logEndOffset，即「**Follower 副本**」可以读取 「**Leader 副本**」上最新的数据，而普通消费者则只能读取高水位前的消息，这是最重要的一点区别。
2.  更新 ISR 副本集合。
3.  更新分区高水位。

本文接下来会剖析下面两点。

## **03 更新 ISR 副本集合**

在 Kafka 中，「**ISR 副本集合**」是一个非常重要的机制， Kafka 为了能够支持「**故障转移**」，设计了 「**ISR 副本集合**」。

在该集合中的「**Follower 副本**」都是与「**Leader 副本**」保持同步的，这个同步并不一定是保持数据一致，而是不落后太多。

> 多个 Follower 副本向 Leader 副本拉取消息的过程中，每个 Follower 副本根据自身状况拉取的进度都不太一样，有的 Follower 副本的消息同步会逐渐落后，此时应该把这个 Follower 副本踢出 ISR 副本集合列表。但是，过段时间这个 Follower 副本同步消息又赶上来了，满足进入 ISR 副本集合列表的标准（replica.lag.time.max.ms 默认是 30 s）。

每个分区的「**ISR 副本集合**」由「**Leader 副本**」来进行管理的，「**Leader 副本**」的 Partition#isrState 中存储了 ISR 的信息。

当「**Follower 副本**」拉取 「**Leader 副本**」数据后，「**Leader 副本**」会检查是否有新的副本可以放入 「**ISR 副本集合**」，如果存在的话，则开始扩充「**ISR 副本集合**」。

另外 KafkaController 也会将分区 ISR 信息存储在 Zookeeper 中（zk 节点路径为 [/brokers/topics/{topic}/partition/{partition}/state](http://brokers/topics/%7Btopic%7D/partition/%7Bpartition%7D/state)），从而避免由于 「**Leader 副本**」下线导致 「**ISR**」信息丢失。

前面说过，如果 [ReplicaManager#fetchMessages](http://replicamanager/#fetchMessages) 方法拉取消息数据的是「**Follower 副本**」，则调用下图中的方法来更新分区高水位、ISR 集合。

  
![](https://article-images.zsxq.com/FsqTuJ6qqC2_2U7W6r_IL9WYpGUY)

## **3.1 updateFollowerFetchState**

private def updateFollowerFetchState(followerId: Int,

readResults: Seq\[(TopicPartition, LogReadResult)\]): Seq\[(TopicPartition, LogReadResult)\] = {

// 遍历每个读取结果

readResults.map {

case (topicPartition, readResult) =>

val updatedReadResult \= if (readResult.error != Errors.NONE) {

// 如果读取结果出错，则跳过更新 follower 的 fetch 状态

debug(s"Skipping update of fetch state for follower $followerId since the " +

s"log read returned error ${readResult.error}")

readResult

} else {

// 判断此副本是否在在线副本集合里。

onlinePartition(topicPartition) match {

case Some(partition) =>

// 如果在在线副本集合里就更新 Follower 副本拉取状态。

if (partition.updateFollowerFetchState(followerId,

followerFetchOffsetMetadata = readResult.info.fetchOffsetMetadata,

followerStartOffset = readResult.followerLogStartOffset,

followerFetchTimeMs = readResult.fetchTimeMs,

leaderEndOffset = readResult.leaderLogEndOffset)) {

readResult

} else {

// 如果更新失败，则记录日志，并返回空的fetch信息

warn(s"Leader $localBrokerId failed to record follower $followerId's position " +

s"${readResult.info.fetchOffsetMetadata.messageOffset}, and last sent HW since the replica " +

s"is not recognized to be one of the assigned replicas ${partition.assignmentState.replicas.mkString(",")} " +

s"for partition $topicPartition. Empty records will be returned for this partition.")

// 返回一个空的fetch信息

readResult.withEmptyFetchInfo

}

case None \=\>

// 如果分区不存在，则记录日志，并返回原始的读取结果

warn(s"While recording the replica LEO, the partition $topicPartition hasn't been created.")

readResult

}

}

topicPartition -> updatedReadResult

}

}

该方法主要用来**更新 Follower 副本的 fetch 状态信息的**, 步骤如下:

1.  首先遍历每个读取结果，然后根据读取结果的情况进行处理。
2.  如果读取结果中存在错误，那么会跳过更新 follower 的 fetch 状态，并打印相应的调试信息。
3.  如果读取结果正常，会判断分区是否在线，如果分区在线，就调用 partition#updateFollowerFetchState 方法来更新 follower 的 fetch 状态。
4.  如果更新成功，则返回原始的读取结果。
5.  如果更新失败，会打印警告日志，并返回一个空的 fetch 信息。
6.  如果分区不存在，会打印警告日志，并返回原始的读取结果。
7.  最后返回包含更新后的分区和读取结果的序列。

这里通过一张图来梳理下其执行流程：

![](https://article-images.zsxq.com/Fv7IOV03utXgOwBLUkabVxMaw8wV)

接着我们来剖析下 [Partition#updateFollowerFetchState](http://partition/#updateFollowerFetchState) 方法。

## **3.2 Partition#updateFollowerFetchState**

这是「**Partition**」组件类的相关方法，「**Partition**」组件是 topic 在某个 broker 上一个副本的抽象。**每个 Parititon 对象都会维护一个 Replica 对象，而 Replica 对象中又会维护 Log 对象，也就是日志数据目录的抽象**。

「**Partition**」类源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/cluster/Partition.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/cluster/Partition.scala)

// 从远端副本集合中匹配当前副本

def getReplica(replicaId: Int): Option\[Replica\] = Option(remoteReplicasMap.get(replicaId))

/\*\*

\* Update the follower's state in the leader based on the last fetch request. See

\* \[\[Replica.updateFetchState()\]\] for details.

\* @return true if the follower's fetch state was updated, false if the followerId is not recognized

\*/

def updateFollowerFetchState(followerId: Int,

followerFetchOffsetMetadata: LogOffsetMetadata,

followerStartOffset: Long,

followerFetchTimeMs: Long,

leaderEndOffset: Long): Boolean = {

// 1、从 Partition#remoteReplicasMap 中获取副本对应的 Replica 实例。

getReplica(followerId) match {

case Some(followerReplica) =>

// No need to calculate low watermark if there is no delayed DeleteRecordsRequest

val oldLeaderLW \= if (delayedOperations.numDelayedDelete > 0) lowWatermarkIfLeader else -1L

val prevFollowerEndOffset \= followerReplica.logEndOffset

// 2、更新 Replica 实例，包括 logStartOffset、logEndOffsetMetadata 等属性。由于更新了 Follower 的 logEndOffset 属性，所以分区的高水位也可能需要更新。

followerReplica.updateFetchState(

followerFetchOffsetMetadata,

followerStartOffset,

followerFetchTimeMs,

leaderEndOffset)

// 3、oldLeaderLW 、newLeaderLW 这两个变量是更新分区数据前后的低水位。

// 这里的低水位是指：该分区所有副本中同步最落后的副本的 logStartOffset 偏移量。

val newLeaderLW \= if (delayedOperations.numDelayedDelete > 0) lowWatermarkIfLeader else -1L

// check if the LW of the partition has incremented

// since the replica's logStartOffset may have incremented

val leaderLWIncremented \= newLeaderLW > oldLeaderLW

// Check if this in-sync replica needs to be added to the ISR.

// 4、扩容 ISR

maybeExpandIsr(followerReplica, followerFetchTimeMs)

// check if the HW of the partition can now be incremented

// since the replica may already be in the ISR and its LEO has just incremented

// 5、更新分区高水位

val leaderHWIncremented \= if (prevFollowerEndOffset != followerReplica.logEndOffset) {

// the leader log may be updated by ReplicaAlterLogDirsThread so the following method must be in lock of

// leaderIsrUpdateLock to prevent adding new hw to invalid log.

inReadLock(leaderIsrUpdateLock) {

leaderLogIfLocal.exists(leaderLog => maybeIncrementLeaderHW(leaderLog, followerFetchTimeMs))

}

} else {

false

}

// some delayed operations may be unblocked after HW or LW changed

// 6、如果分区的低水位或者高水位发生了变化，则触发该分区生产者的延迟操作的正常结束行为。如果生产者设置了 acks = -1，则写入消息时，会创建一个延迟操作，该延迟操作需要等待 ISR 中所有副本同步数据后再返回成功响应给生产者。如果高水位发生变化则表示 ISR 中 Follower 副本同步了新数据，此时触发对应的生产者延迟操作的正常结束行为。

if (leaderLWIncremented || leaderHWIncremented)

tryCompleteDelayedRequests()

debug(s"Recorded replica $followerId log end offset (LEO) position " +

s"${followerFetchOffsetMetadata.messageOffset} and log start offset $followerStartOffset.")

true

case None \=\>

false

}

}

该方法主要用来**更新 follower 的 fetch 状态信息**, 步骤如下：

1.  从 Partition#remoteReplicasMap 中获取副本对应的 Replica 实例。
2.  更新 followerReplica 副本，包括 logStartOffset、logEndOffsetMetadata 等属性。由于更新了 Follower 的 logEndOffset 属性，所以分区的高水位也可能需要更新。
3.  oldLeaderLW 、newLeaderLW 这两个变量是更新分区数据前后的低水位。这里的低水位是指：该分区所有副本中同步最落后的副本的 logStartOffset 偏移量。
4.  尝试扩容 ISR 副本集合。
5.  更新分区高水位。
6.  如果分区的低水位或者高水位发生了变化，则触发该分区生产者的延迟操作的正常结束行为。如果生产者设置了 acks = -1，则写入消息时，会创建一个延迟操作，该延迟操作需要等待 ISR 中所有副本同步数据后再返回成功响应给生产者。如果高水位发生变化则表示 ISR 中 Follower 副本同步了新数据，此时触发对应的生产者延迟操作的正常结束行为。
7.  最后返回更新成功，如果副本不存在，则返回 false。

这里通过一张图来梳理下其执行流程：

![](https://article-images.zsxq.com/Fo5JhQ_UxeM9NznhO9Y0x2RUryZD)

在整个流程中，最主要的是 「**第二步**」、「**第四步**」、「**第五步**」，接下来我们挨个剖析下。

##   
**3.3 更新 followerReplica 副本的 fetch 状态**

def updateFetchState(followerFetchOffsetMetadata: LogOffsetMetadata,

followerStartOffset: Long,

followerFetchTimeMs: Long,

leaderEndOffset: Long): Unit = {

// 如果 follower 的消息偏移量大于等于 leader 的最新偏移量

if (followerFetchOffsetMetadata.messageOffset >= leaderEndOffset)

// 更新最后追上的时间

\_lastCaughtUpTimeMs = math.max(\_lastCaughtUpTimeMs, followerFetchTimeMs)

// 如果 follower 的消息偏移量大于等于上一次 fetch 时的 leader 的最新偏移量

else if (followerFetchOffsetMetadata.messageOffset >= lastFetchLeaderLogEndOffset)

// 更新最后追上的时间

\_lastCaughtUpTimeMs = math.max(\_lastCaughtUpTimeMs, lastFetchTimeMs)

// 更新日志的起始偏移量和结束偏移量

\_logStartOffset = followerStartOffset

\_logEndOffsetMetadata \= followerFetchOffsetMetadata

lastFetchLeaderLogEndOffset \= leaderEndOffset

// 更新最后 fetch 的时间

lastFetchTimeMs = followerFetchTimeMs

}

该方法主要用来**更新 followerReplica 副本的 fetch 状态**。它接收 follower 的 fetch 偏移量元数据、follower 的起始偏移量、follower 的 fetch 时间以及 Leader 的最新偏移量作为参数，步骤如下：

1.  首先根据 follower 的 fetch 偏移量与 Leader 的最新偏移量进行比较。
2.  如果 follower 的消息偏移量大于等于 Leader 的最新偏移量，说明该 follower 已经追上了 Leader 的进度，此时会更新最后追上的时间。
3.  接着如果 follower 的消息偏移量大于等于上一次 fetch 时的 Leader 的最新偏移量，表示该 follower 在上一次 fetch 时已经追上了 Leader 的进度，同样会更新最后追上的时间。
4.  然后更新日志的起始偏移量、结束偏移量以及最后 fetch 的时间。

## **3.4 ISR 扩缩容机制**

在 [【服务端 Broker 源码分析系列第二十六篇】图解 Kafka 源码之副本同步实现原理（上）](https://articles.zsxq.com/id_j8h0gmrk5i6d.html) 这篇中，我们剖析了从流程上剖析了扩缩容机制，这里我们综合总结下。

Kafka在启动的时候,会启动一个「**副本管理器 ReplicaManager**」，它会启动以下几个定时任务：

1.  ISR 过期定时任务 [isr-expiration](http://isr-expiration/)，每隔 **replica.lag.time.max.ms/2** 毫秒就执行一次。
2.  ISR变更的传播定时任务 [isr-change-propagation](http://isr-change-propagation/)，每隔 **2500** 毫秒就执行一次。

**replica.lag.time.max.ms** : 如果一个 follower 在这个时间内没有发送 fetch 请求，Leader 节点将从 ISR 副本集合中移除这个 follower。从 2.5 版本开始 ,默认值从 10 秒增加到 30 秒。

接下来我们分析一下这两个定时任务的作用。

## **3.4.1 ISR 收缩 isr-expiration**

它是每隔 **replica.lag.time.max.ms/2** 毫秒执行一次。

  
![](https://article-images.zsxq.com/FuiCPU1l6fpEORu5atmN6Lev_7Zh)

如果「**Follower 副本**」上次发送的 Fetch 请求距今时间大于 [Partition#](http://partition/#replicaLagTimeMaxMs)[replicaLagTimeMaxMs](http://partition/#replicaLagTimeMaxMs) 属性。「**ISR 副本集合**」中的副本落后太多，则会将该「**Follower 副本**」从 「**ISR 副本集合**」中移除，从而收缩 ISR 。

private def maybeShrinkIsr(): Unit = {

trace("Evaluating ISR list of partitions to see which replicas can be removed from the ISR")

// 尝试收缩ISR, 先遍历所有的分区,找出本台 Broker 上所有在线的分区进行遍历,检查是否需要收缩

allPartitions.keys.foreach { topicPartition =>

onlinePartition(topicPartition).foreach(\_.maybeShrinkIsr())

}

}

// 尝试传播 ISR 变更信息

def maybeShrinkIsr(): Unit = {

// 判断是否需要执行 ISR 收缩

val needsIsrUpdate \= !isrState.isInflight && inReadLock(leaderIsrUpdateLock) {

// 判断条件：当前分区是 Leader&&（follower 副本 LEO != Leader 副本 LEO && ( (currentTimeMs - followerReplica.lastCaughtUpTimeMs) > replica.lag.time.max.ms)）

needsShrinkIsr()

}

val leaderHWIncremented \= needsIsrUpdate && inWriteLock(leaderIsrUpdateLock) {

// 如果是 Leader 副本

leaderLogIfLocal.exists { leaderLog =>

// 获取不同步的副本 Id 列表

val outOfSyncReplicaIds \= getOutOfSyncReplicas(replicaLagTimeMaxMs)

// 如果存在不同步的副本 Id 列表

if (outOfSyncReplicaIds.nonEmpty) {

// 获取不同步的的副本日志

val outOfSyncReplicaLog \= outOfSyncReplicaIds.map { replicaId =>

s"(brokerId: $replicaId, endOffset: ${getReplicaOrException(replicaId).logEndOffset})"

}.mkString(" ")

// 计算收缩之后的 ISR 列表:把它们从当前 ISR 中剔除出去，然后计算得出最新的 ISR 列表

val newIsrLog \= (isrState.isr -- outOfSyncReplicaIds).mkString(",")

info(s"Shrinking ISR from ${isrState.isr.mkString(",")} to $newIsrLog. " +

s"Leader: (highWatermark: ${leaderLog.highWatermark}, endOffset: ${leaderLog.logEndOffset}). " +

s"Out of sync replicas: $outOfSyncReplicaLog.")

// 更新 ZooKeeper 中分区的 ISR 数据以及 Broker 的元数据缓存中的 ISR 数据

shrinkIsr(outOfSyncReplicaIds)

// 尝试更新 Leader 副本的高水位值

maybeIncrementLeaderHW(leaderLog)

} else {

// 如果没有不同步的副本 Id 列表，什么都不做

false

}

}

}

// 如果 Leader 副本的高水位值已经抬高了

if (leaderHWIncremented)

// 尝试解锁一下延迟请求

tryCompleteDelayedRequests()

}

private def needsShrinkIsr(): Boolean = {

// 获取与 Leader 不同步的副本

leaderLogIfLocal.exists { \_ => getOutOfSyncReplicas(replicaLagTimeMaxMs).nonEmpty }

}

def getOutOfSyncReplicas(maxLagMs: Long): Set\[Int\] = {

val current \= isrState

if (!current.isInflight) {

val candidateReplicaIds \= current.isr - localBrokerId

val currentTimeMs \= time.milliseconds()

val leaderEndOffset \= localLogOrException.logEndOffset

// 调用 isFollowerOutOfSync 来判断是否没跟上

candidateReplicaIds.filter(replicaId => isFollowerOutOfSync(replicaId, leaderEndOffset, currentTimeMs, maxLagMs))

} else {

Set.empty

}

}

private def isFollowerOutOfSync(replicaId: Int,

leaderEndOffset: Long,

currentTimeMs: Long,

maxLagMs: Long): Boolean = {

getReplica(replicaId).fold(true) { followerReplica =>

// 判断条件：当前分区是 Leader&&（follower 副本 LEO != Leader 副本 LEO && ( (currentTimeMs - followerReplica.lastCaughtUpTimeMs) > replica.lag.time.max.ms)）

followerReplica.logEndOffset != leaderEndOffset &&

(currentTimeMs - followerReplica.lastCaughtUpTimeMs) > maxLagMs

}

}

核心步骤如下：

1.  找到所有需要收缩的副本 OSR，其判断条件：
2.  当前分区必须是 Leader。
3.  follower 副本 LEO != Leader 副本 LEO (如果相等的话就跟 Leader保持最高同步了,也没必要收缩了)。
4.  follower 副本中, 当前时间 - 上一次去 Leader 副本获取数据的时间戳 > **replica.lag.time.max.ms（从 2.5 版本开始默认为 30000 ms）**。
5.  计算新的 newISR = 当前ISR - 步骤1中获取到的 OSR 。
6.  将 newISR 组装一下成 newLeaderData 对象(内部包括 Leader 和 epoch 信息)，然后将信息写入到 Zookeeper 持久节点  **/brokers/topics/{topic 名称}/partitions/{分区号}/state** 中。
7.  如果写入成功,则更新一下这两个对象内存，**isrChangeSet** 对象保存着 ISR 变更记录；**lastIsrChangeMs**记录这最新一次 ISR 变更的时间戳。这两个两个对象会在 ISR 传播时用到。
8.  还会更新一下2个对象内存：**inSyncReplicaIds = newISR， zkVersion = newVersion**。
9.  接着尝试增加HW(高水位)， 该方法可能会在 ①.ISR变更 ②.任何副本的LEO更改 这两种情况下触发调用。当然这里触发时因为 **ISR 发生变更了**。如果HW有更新，则返回 true,否则返回 false。
10.  如果更新 HW 成功，则触发一下待处理的延迟操作。这里包含一些 fetch、produce、deleteRecords 等延迟请求。

## **3.4.2 ISR 扩容**

ISR 缩容是有一个定时任务定时检查，而 **ISR 扩容** 可不一样,它是在 Follower 副本向 Leader 副本发起「**Fetch 请求**」时会尝试检查是否需要重新加入到 ISR 副本集合中。

private def maybeExpandIsr(followerReplica: Replica, followerFetchTimeMs: Long): Unit = {

// 检查一下是否满足扩容的条件

val needsIsrUpdate \= canAddReplicaToIsr(followerReplica.brokerId) && inReadLock(leaderIsrUpdateLock) {

needsExpandIsr(followerReplica)

}

if (needsIsrUpdate) {

inWriteLock(leaderIsrUpdateLock) {

// 再检查一遍是否需要扩容,双重检查。

if (needsExpandIsr(followerReplica)) {

expandIsr(followerReplica.brokerId)

}

}

}

}

  
如果 [Partition#maybeExpandIsr](http://partition/#maybeExpandIsr) 方法检查当前发起 「**Fetch 请求**」的 Follower 副本是否满足加入 ISR 的条件, 发现某个 follower 副本满足以下条件，则会将该副本添加到「**ISR 副本集合**」中，条件如下(与运算)：

1.  当前该副本不在 ISR 副本集合中。
2.  该副本 logEndOffset 大于或者等于分区高水位，即 Follower 副本 LEO >=HW(高水位) && Follower 副本 LEO >= 当前Leader 的 LogStartOffset。

如果满足条件,则开始执行「**ISR 扩容**」的流程，这里的流程跟上面「**ISR 缩容**」差不多。

1.  将 newISR 组装一下成 newLeaderData 对象(内部包括 Leader 和 epoch 信息)，然后将信息写入到 Zookeeper 持久节点  **/brokers/topics/{topic 名称}/partitions/{分区号}/state** 中。
2.  如果写入成功,则更新一下这两个对象内存，**isrChangeSet** 对象保存着 ISR 变更记录；**lastIsrChangeMs**记录这最新一次 ISR 变更的时间戳。这两个两个对象会在 ISR 传播时用到。
3.  还会更新一下2个对象内存：**inSyncReplicaIds = newISR， zkVersion = newVersion**。

该方法还会更新 [Parititon#isrState](http://parititon/#isrState) 属性，并调用 [AlterIsrManager#submit](http://alterisrmanager/#submit) 方法给 KafkaController 发送 AlterIsr 请求，当 KafkaController 收到该请求后，会修改 KafkaController 元数据，并更新 Zookeeper 中该分区元数据的 ISR 信息。

> 在 Kafka 2.7 版本之前，当 ISR 变更后 Broker 将直接修改 Zookeeper 数据，Kafka 2.7 及后续版本中，当 ISR 变更后由 KafkaController 修改 Zookeeper 数据。

## **3.4.3 ISR 传播**

它是每隔 **2500** 毫秒就执行一次。

上面我们剖析的「**ISR 扩容**」以及「**ISR 缩容**」都是只修改 ISR 和 内存信息，并没有通知到每个 Broker 节点。

只修改zk中的 **/brokers/topics/{topic 名称}/partitions/{分区号}/state** 节点，并不会通知集群 ISR 已经变更了，因为正常情况下, Broker 是没有去监听每一个 State 节点的。

因为在整个集群中，state 节点太多了，每个节点都去监听的话成本有点高。 **除了在分区副本重分配时**会去监听迁移的state节点，其他情况都没有监听。

**那么我们是如何通知其他 Broker 节点 ISR 变更了呢？**很简单，就是定时任务定时去传播 ISR 的变更。

## **04 更新高水位**

最后来聊聊更新高水位的逻辑。

在 [Log#highWatermarkMetadata](http://log/#highWatermarkMetadata) 属性中存储了分区的高水位， 接下来我们来剖析 「**Leader 副本**」如何更新分区的高水位的。

Leader 副本在以下场景中可能会更新高水位：

1.  当 Broker 成为 Leader 副本后。
2.  当 Follower 副本拉取数据后。
3.  当 ISR 副本集合收缩后。
4.  当写入新的消息后。

在上面 [Partition#updateFollowerFetchState](http://partition/#updateFollowerFetchState) 方法的「**第四步**」中，Leader 副本会调用 [maybeIncrementLeaderHW](http://maybeincrementleaderhw/) 方法来更新分区的高水位。

private def maybeIncrementLeaderHW(leaderLog: Log, curTime: Long = time.milliseconds): Boolean = {

// maybeIncrementLeaderHW is in the hot path, the following code is written to

// avoid unnecessary collection generation

// 1、初始新的高水位线为 Leader 副本的 LEO

var newHighWatermark \= leaderLog.logEndOffsetMetadata

// 遍历所有的远端副本

remoteReplicasMap.values.foreach { replica =>

// Note here we are using the "maximal", see explanation above

// follower 副本在 ISR 副本集合中，follower 副本上次发送的 Fetch 请求距今时间小于 replicaLagTimeMaxMs 属性值（因为这些 follower 副本后续会添加到 ISR 副本集合中，所以需要考虑这些 follower 副本），则将新高水位线更新为该副本的 LEO。

if (replica.logEndOffsetMetadata.messageOffset < newHighWatermark.messageOffset &&

(curTime - replica.lastCaughtUpTimeMs <= replicaLagTimeMaxMs || isrState.maximalIsr.contains(replica.brokerId))) {

// follower 副本的最小 LEO 作为高水位值

newHighWatermark = replica.logEndOffsetMetadata

}

}

// 2、尝试通过新的高水位线更新 Leader 副本的高水位线

leaderLog.maybeIncrementHighWatermark(newHighWatermark) match {

case Some(oldHighWatermark) =>

debug(s"High watermark updated from $oldHighWatermark to $newHighWatermark")

// 更新成功

true

case None \=\>

// 如果更新失败，打印日志以及当前的所有 LEO 信息

def logEndOffsetString: ((Int, LogOffsetMetadata)) => String = {

case (brokerId, logEndOffsetMetadata) => s"replica $brokerId: $logEndOffsetMetadata"

}

if (isTraceEnabled) {

val replicaInfo \= remoteReplicas.map(replica => (replica.brokerId, replica.logEndOffsetMetadata)).toSet

val localLogInfo \= (localBrokerId, localLogOrException.logEndOffsetMetadata)

trace(s"Skipping update high watermark since new hw $newHighWatermark is not larger than old value. " +

s"All current LEOs are ${(replicaInfo + localLogInfo).map(logEndOffsetString)}")

}

// 更新失败

false

}

}

该方法主要用来**更新 Leader 副本的高水位值**。步骤如下：

1.  初始新的高水位线为 Leader 副本的 LEO。
2.  遍历所有的远端副本，该远程副本的 logEndOffset 小于当前的新高水位线且满足以下条件之一的副本，则取其中最小的 logEndOffset 作为分区高水位：
3.  follower 副本在 ISR 副本集合中。
4.  follower 副本上次发送的 Fetch 请求距今时间小于 replicaLagTimeMaxMs 属性值（因为这些 follower 副本后续会添加到 ISR 副本集合中，所以需要考虑这些 follower 副本）。
5.  接着调用 leaderLog.maybeIncrementHighWatermark 尝试更新 Leader 副本的高水位，更新成功返回 true，否则返回 false。

> 这里需要注意的是： 高水位值只能增加，不能减少，如果第2步中获取的 HW 比之前的 HW 小，则不更新。

这里通过一张图来梳理下其执行流程：

![](https://article-images.zsxq.com/Fmk9QlEbKznLzzpeyO2qrO0Jz4Zy)

## **4.1 log#maybeIncrementHighWatermark**

这是 Log 类的方法，主要用来更新 Leader 副本的高水位的。

def maybeIncrementHighWatermark(newHighWatermark: LogOffsetMetadata): Option\[LogOffsetMetadata\] = {

// 如果新高水位线的消息位移大于当前 logEndOffset，则抛出非法参数异常，因为这意味着新高水位线超出了当前的 logEndOffset

if (newHighWatermark.messageOffset > logEndOffset)

throw new IllegalArgumentException(s"High watermark $newHighWatermark update exceeds current " + s"log end offset $logEndOffsetMetadata")

lock.synchronized {

val oldHighWatermark \= fetchHighWatermarkMetadata

// Ensure that the high watermark increases monotonically. We also update the high watermark when the new

// offset metadata is on a newer segment, which occurs whenever the log is rolled to a new segment.

// 确保高水位线单调递增。

// 当新的 offset metadata 处于一个新的日志段时，也就意味着日志发生了滚动，此时我们也会更新高水位线。

if (oldHighWatermark.messageOffset < newHighWatermark.messageOffset ||

(oldHighWatermark.messageOffset == newHighWatermark.messageOffset && oldHighWatermark.onOlderSegment(newHighWatermark))) {

// 更新高水位线

updateHighWatermarkMetadata(newHighWatermark)

// 返回旧的高水位线元数据

Some(oldHighWatermark)

} else {

None

}

}

}

该方法主要用来**检查是否应该将 Leader 副本的高水位线更新为给定的新高水位线（newHighWatermark）**。步骤如下：

1.  首先检查新高水位线的消息位移是否超出当前的 logEndOffset，如果是，则抛出异常，因为这意味着新高水位线超出了当前的日志尾偏移量（logEndOffset）。
2.  接下来获取当前高水位线的元数据信息。然后，它检查新高水位线是否大于当前高水位线或者位于较新的日志段。如果是，则更新高水位线为新高水位线，然后返回旧的高水位线元数据。否则，不执行任何操作，返回 None。

> 请注意，该方法执行在一个锁（lock）内，因此在执行更新操作时，只有一个线程能够获取锁进行更新。因为需要保证高水位线的单调递增，同时避免并发更新导致不一致或错误。

这里通过一张图来梳理下其执行流程：

![](https://article-images.zsxq.com/FuU_ei1tG83JJMe1YSvmqWwiPiA_)

## **4.2 log#updateHighWatermarkMetadata**

private def updateHighWatermarkMetadata(newHighWatermark: LogOffsetMetadata): Unit = {

// 检查新高水位线的消息位移是否为非负数，如果不是，则抛出非法参数异常

if (newHighWatermark.messageOffset < 0)

throw new IllegalArgumentException("High watermark offset should be non-negative")

lock synchronized {

// 检查新高水位线是否小于当前高水位线，如果是，则打印警告，因为这不满足单调递增的

if (newHighWatermark.messageOffset < highWatermarkMetadata.messageOffset) {

warn(s"Non-monotonic update of high watermark from $highWatermarkMetadata to $newHighWatermark")

}

// 更新高水位线的元数据信息

highWatermarkMetadata = newHighWatermark

// 更新 ProducerState 中的高水位线信息

producerStateManager.onHighWatermarkUpdated(newHighWatermark.messageOffset)

// 尝试增加第一个不稳定偏移（first unstable offset）

maybeIncrementFirstUnstableOffset()

}

trace(s"Setting high watermark $newHighWatermark")

}

该方法主要用来**更新高水位线的元数据信息**。步骤如下：

1.  首先检查新高水位线的消息位移是否是非负数，如果不是，则抛出异常。
2.  接下来进入同步代码块（lock synchronized），然后检查新高水位线是否小于当前高水位线。如果是，则打印警告，因为这意味着更新不是单调递增的。
3.  然后更新高水位线的元数据信息，将其设置为新的高水位线。此外，函数还通过 producerStateManager 更新 ProducerState 中的高水位线信息，并尝试增加第一个不稳定偏移（first unstable offset）。最后，函数记录新的高水位线信息，并打印跟踪日志。

> 请注意，该方法执行也在锁（lock）内，这是为了保证在同时进行的更新操作中，只有一个线程能够更新高水位线。

##   
**05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从「**KafkaController**」启动会选举「**Leader 副本**」，Follower 副本会同步消息，那么 **Leader 副本是如何处理 Follower 副本发送过来的 Fetch 请求的呢？**

2、接着带大家深度剖析了「**Follower 副本发送 Fetch 请求跟消费者发送 Fetch 请求的区别**」。

3、接着带大家深度剖析了「**Leader 副本更新 ISR 副本集合**」的处理流程，让 Leader 副本是如何管理 ISR 副本集合的。

4、最后带大家深度剖析了「**Leader 副本更新高水位**」的处理流程，让你了解 Leader 副本高水位是如何更新的。

下篇我们来深度剖析「**ISR、 LeaderEpoch 机制实现原理**」，大家期待，我们下期见。