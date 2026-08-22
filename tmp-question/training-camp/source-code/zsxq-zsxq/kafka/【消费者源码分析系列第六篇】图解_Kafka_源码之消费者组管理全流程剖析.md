大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 消费者组状态机流程**」，看看消费者组内部都有那几个状态，是如何进行流转的。今天我们开启消费端源码的征程，这是第六篇来深度聊聊「**消费者组管理全流程剖析**」，看看消费者组内部是如何对「**组状态**」、「**组成员**」、「**位移**」、「**分区分配策略**」进行管理的。

![](https://article-images.zsxq.com/Fg_thcH_BjHwOIuLMBx07NwYj0Wj)

## **01 总体概述**

对 Kafka 消费者组来说，我们在前面几篇中了解到，在 Consumer 初始化时会启动 [ConsumerCoordinator](http://consumercoordinator/) 负责跟服务端 GroupCoordinator 进行交互，比如发送「**FIND\_COORDINATOR**」「**HEARTBEAT**」、「**JOIN\_GROUP**」、「**SYNC\_GROUP**」等请求。

那么消费者组管理「**组状态**」、「**组成员**」、「**位移**」、「**分区分配策略**」就显得尤为重要了，它们都属于消费者组元数据的一部分，接下来我们就来重点看看这几个维度的管理操作。

关于 「**组状态**」的我们上一篇已经剖析过了，这里就不赘述了，点击 [【消费者源码分析系列第五篇】图解 Kafka 源码之消费者组状态机流程](https://articles.zsxq.com/id_tpg7iob24t7w.html)

## **02 消费者组成员管理**

首先在 [GroupMetadata](http://groupmetadata/) 中会定义成员元数据信息属性，如下：

![](https://article-images.zsxq.com/FhpoqCc-frqUuacwF76a9Cku6bBc)

可以看出该字段是一个 HashMap，其中 Key 是成员的 member ID，Value 是 MemberMetadata 类型，该类型保存了成员的元数据信息。

在其伴生对象 [Object](http://object%20%20groupmetadata/) [GroupMetadata](http://object%20%20groupmetadata/) 中会进行加载消费者组的元数据信息，在其过程中会对 [GroupMetadata](http://object%20%20groupmetadata/) 对象添加成员操作。

  
![](https://article-images.zsxq.com/FsUxWPeYHLhcMZqf4oUZUx7H6V6Y)

接下来我们先来看看如何添加组成员操作。

## **2.1 添加组成员**

在上面 [loadGroup](http://loadgroup/) 方法中会实例化 [GroupMetadata](http://groupmetadata/) 类并调用其 [add](http://add%20/) 方法来添加组成员，对于「**消费者组重分配**」来说，添加组成员是最重要的一步，只有加入组之后才能开启「**消费者组重分配**」， 源码如下：

def add(member: MemberMetadata, callback: JoinCallback = null): Unit = {

// 先遍历判断静态组成员中是否包含已有成员，如果是直接抛异常

member.groupInstanceId.foreach { instanceId =>

if (staticMembers.contains(instanceId))

throw new IllegalStateException(s"Static member with groupInstanceId=$instanceId " +

s"cannot be added to group $groupId since it is already a member")

staticMembers.put(instanceId, member.memberId)

}

// 如果是要添加的第一个消费者组成员

if (members.isEmpty)

// 把该成员的 procotolType 设置为该消费者组的 protocolType

this.protocolType = Some(member.protocolType)

// 确保成员元数据中的 protoclType 和组 protocolType 相同

assert(this.protocolType.orNull == member.protocolType)

// 确保该成员选定的分区分配策略与组选定的分区分配策略相匹配

assert(supportsProtocols(member.protocolType, MemberMetadata.plainProtocolSet(member.supportedProtocols)))

// 如果此时还未选出消费者组的 Leader 成员

if (leaderId.isEmpty)

// 把该成员选为 Leader 成员

leaderId = Some(member.memberId)

// 将该成员添加进 members

members.put(member.memberId, member)

// 递增分区分配策略支持票数

incSupportedProtocols(member)

// 设置成员加入消费者组后的回调方法

member.awaitingJoinCallback = callback

// 递增已加入消费者组的成员数

if (member.isAwaitingJoin)

numMembersAwaitingJoin += 1

// 添加成功后将其从 pendingMembers 待加入成员列表中进行移除

pendingMembers.remove(member.memberId)

}

// 递增分区分配策略支持票数

private def incSupportedProtocols(member: MemberMetadata): Unit = {

member.supportedProtocols.foreach { case (protocol, \_) => supportedProtocols(protocol) += 1 }

}

核心步骤如下：

1.  先遍历判断静态组成员中是否包含已有成员，如果是直接抛异常。
2.  如果不包含的话，则判断要添加的成员是该消费者组的第一个成员，则把该成员的 procotolType 设置为该消费者组的 protocolType。对于普通的消费者而言，其 protocolType 就是 “consumer”。
3.  接着进行两次检测，如果未通过直接抛异常：
4.  确保成员元数据中的 protoclType 和组 protocolType 相同。
5.  确保该成员选定的分区分配策略与组选定的分区分配策略相匹配。
6.  如果此时还未选出消费者组的 Leader 成员，则将该成员选为 Leader 成员，这里的 Leader 成员负责为所有组成员制定分区分配策略方案。
7.  将该成员添加进 members。
8.  递增分区分配策略支持票数。
9.  设置成员加入消费者组后的回调方法。
10.  递增已加入消费者组的成员数。
11.  添加成功后将其从 [pendingMembers](http://pendingmembers/) 待加入成员列表中进行移除。

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/lhXNg5iaYwiXwYk3LZmfxc_j0kfR)

## **2.2 移除组成员**

有添加成员操作就会有移除成员操作，如下：

def remove(memberId: String): Unit = {

// 从 members 中移除给定成员

members.remove(memberId).foreach { member =>

// 递减分区分配策略支持票数

decSupportedProtocols(member)

// 递减已加入消费者组的成员数

if (member.isAwaitingJoin)

numMembersAwaitingJoin -= 1

// 依次从静态组成员中进行移除

member.groupInstanceId.foreach(staticMembers.remove)

}

// 如果该成员是Leader，选择剩下成员列表中的第一个作为新的Leader成员

if (isLeader(memberId))

leaderId = members.keys.headOption

// 最后将其从 pendingMembers 待加入成员列表以及 pendingSyncMembers 正在等待同步的成员列表中移除

pendingMembers.remove(memberId)

pendingSyncMembers.remove(memberId)

}

// 递减分区分配策略支持票数

private def decSupportedProtocols(member: MemberMetadata): Unit = {

member.supportedProtocols.foreach { case (protocol, \_) => supportedProtocols(protocol) -= 1 }

}

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/ljRkvHUowOIFaC24lvqTEFL1r8nY)

## **2.3 查找组成员相关**

查找方法都比较简单，这里汇总简单讲解下，如下：

// 判断当前消费者组是否包含该指定成员

def has(memberId: String): Boolean = members.contains(memberId)

// 获取当前消费者组该指定成员

def get(memberId: String): MemberMetadata = members(memberId)

// 统计当前消费者组总成员数

def size: Int = members.size

// 判断 LeaderId 成员中是否包含该指定成员

def isLeader(memberId: String): Boolean = leaderId.contains(memberId)

// 检查指定的成员ID是否是待加入的成员

def isPendingMember(memberId: String): Boolean = pendingMembers.contains(memberId)

// 检查是否已经从所有成员接收到同步

def hasReceivedSyncFromAllMembers: Boolean = {

pendingSyncMembers.isEmpty

}

// 返回所有待同步的成员集合。

def allPendingSyncMembers: Set\[String\] = {

pendingSyncMembers.toSet

}

// 清除待同步成员列表

def clearPendingSyncMembers(): Unit = {

pendingSyncMembers.clear()

}。

// 检查给定的组实例ID是否是静态成员。

def hasStaticMember(groupInstanceId: String): Boolean = {

staticMembers.contains(groupInstanceId)

}

// 获取给定组实例ID的当前静态成员ID

def currentStaticMemberId(groupInstanceId: String): Option\[String\] = {

staticMembers.get(groupInstanceId)

}

// 获取尚未重新加入消费者组的成员的元数据信息。

def notYetRejoinedMembers: Map\[String, MemberMetadata\] = members.filter(!\_.\_2.isAwaitingJoin).toMap

// 检查是否所有成员已加入消费者组。

def hasAllMembersJoined: Boolean = members.size == numMembersAwaitingJoin && pendingMembers.isEmpty

// 返回消费者组中的所有成员的集合

def allMembers: collection.Set\[String\] = members.keySet

// 返回消费者组中所有静态成员的集合

def allStaticMembers: collection.Set\[String\] = staticMembers.keySet

// 返回消费者组中所有动态成员的集合

private\[group\] def allDynamicMembers: Set\[String\] = {

val dynamicMemberSet \= new mutable.HashSet\[String\]

allMembers.foreach(memberId => dynamicMemberSet.add(memberId))

staticMembers.values.foreach(memberId => dynamicMemberSet.remove(memberId))

dynamicMemberSet.toSet

}

// 获取待加入的成员数量

def numPending: Int = pendingMembers.size

// 获取正在等待加入的成员数量

def numAwaiting: Int = numMembersAwaitingJoin

// 返回消费者组中所有成员的元数据信息的列表

def allMemberMetadata: List\[MemberMetadata\] = members.values.toList

// 获取消费者组中所有成员的最大重新平衡超时时间

def rebalanceTimeoutMs: Int = members.values.foldLeft(0) { (timeout, member) =>

timeout.max(member.rebalanceTimeoutMs)

}

// 检查指定的消费者组实例ID的静态成员是否被拒绝访问

def isStaticMemberFenced(

groupInstanceId: String,

memberId: String

): Boolean = {

currentStaticMemberId(groupInstanceId).exists(\_ != memberId)

}

## **02 消费者组位移管理**

除了「**消费者组状态**」和「**消费者组成员**」管理之外，还有一个非常重要的元数据管理，那就是对「**消费者组位移提交**」进行管理。

在元数据类属性字段中，有这样的定义：

// 保存消费者组订阅分区的提交位移值

private val offsets \= new mutable.HashMap\[TopicPartition, CommitRecordMetadataAndOffset\]

可以看出它也是 HashMap 类型，其中 Key 是成员的 [TopicPartition](http://topicpartition/)，它表示主题分区，Value 是 [CommitRecordMetadataAndOffset](http://commitrecordmetadataandoffset/) 类型，该类型封装了位移提交消息的位移值。

众所周知，消费者消费完消息之后都需要提交位移的，即消费者组需要向 [Coordinator](http://coordinator/) 提交已消费消息的进度，最终保存到 Kafka 内部位移主题：\_\_consumer\_offsets，在 Kafka 中使用它来定位消费者组要消费的下一条消息。

这里的 [CommitRecordMetadataAndOffset](http://commitrecordmetadataandoffset/) 类主要是用来标识位移提交消息的地方，如下：

// appendedBatchOffset：其内部保存的是位移主题消息自己的位移值

// offsetAndMetadata：其内部保存的是位移提交消息中保存的消费者组的位移值。

case class CommitRecordMetadataAndOffset(appendedBatchOffset: Option\[Long\], offsetAndMetadata: OffsetAndMetadata) {

def olderThan(that: CommitRecordMetadataAndOffset): Boolean = appendedBatchOffset.get < that.appendedBatchOffset.get

}

##   
**3.1 位移初始化**

// 初始化偏移量和待处理的事务性偏移量提交

def initializeOffsets(offsets: collection.Map\[TopicPartition, CommitRecordMetadataAndOffset\],

pendingTxnOffsets: Map\[Long, mutable.Map\[TopicPartition, CommitRecordMetadataAndOffset\]\]): Unit = {

// 将传入的偏移量集合累计到 offsets 变量中

this.offsets ++= offsets

// 将传入的待处理事务性偏移量提交集合添加到 pendingTransactionalOffsetCommits 变量中

this.pendingTransactionalOffsetCommits ++= pendingTxnOffsets

}

初始化很简单，其目的是为了在 [GroupMetadata](http://groupmetadata/) 对象中初始化消费者的偏移量和待处理的事务性偏移量提交。当消费者组的协调器启动时会创建一个异步任务来定期读取 [\_\_consumer\_offsets](http://__consumer_offsets/) 中对应消费者组的提交位移数据，把它们加载到 offsets 中，使得 [GroupMetadata](http://groupmetadata/) 能够跟踪和管理这些偏移量数据，并确保在提交偏移量时能够正确处理它们。

##   
**3.3 提交普通位移**

// 在提交位移消息被成功写入后调用

def onOffsetCommitAppend(topicPartition: TopicPartition, offsetWithCommitRecordMetadata: CommitRecordMetadataAndOffset): Unit = {

// 如果 pendingOffsetCommits 中包含该分区的偏移量提交记录

if (pendingOffsetCommits.contains(topicPartition)) {

// 如果 offsetWithCommitRecordMetadata.appendedBatchOffset 为空，抛出异常。因为没有提供日志中记录的元数据，无法完成偏移量提交的写入。

if (offsetWithCommitRecordMetadata.appendedBatchOffset.isEmpty)

throw new IllegalStateException("Cannot complete offset commit write without providing the metadata of the record in the log.")

// 如果 offsets 不包含该分区位移提交数据，或者 offsets 中该分区对应的提交位移消息在位移主题中的位移值小于待写入的位移值

if (!offsets.contains(topicPartition) || offsets(topicPartition).olderThan(offsetWithCommitRecordMetadata))

// 将该分区对应的提交位移消息添加到 offsets 中

offsets.put(topicPartition, offsetWithCommitRecordMetadata)

}

// 成功处理偏移量的提交记录后，更新和管理偏移量的状态，并确保偏移量的正确提交和删除

pendingOffsetCommits.get(topicPartition) match {

// 如果相同，表示偏移量已成功提交，从 pendingOffsetCommits 中删除该分区的偏移量提交记录。

case Some(stagedOffset) if offsetWithCommitRecordMetadata.offsetAndMetadata == stagedOffset =>

pendingOffsetCommits.remove(topicPartition)

case \_ \=\>

// 如果不相同，则保留 pendingOffsetCommits 中的该分区的偏移量提交记录，并且如果该分区的主题已被删除，则它的条目将由 removeOffsets 方法从缓存中删除。

}

}

这里通过一张图来梳理其流程：

![](https://article-images.zsxq.com/FpNl05nBXyTlUO-yfbNruaNmgLcl)

## **3.4 提交事务位移**

说完普通位移提交，我们来剖析下事务位移提交，源码如下：

// 正在进行中、还没有完成的事务提交

def completePendingTxnOffsetCommit(producerId: Long, isCommit: Boolean): Unit = {

// 获取指定producerId的挂起事务性偏移量提交，并将其从pendingTransactionalOffsetCommits中移除

val pendingOffsetsOpt \= pendingTransactionalOffsetCommits.remove(producerId)

// 完成事务性偏移量提交

if (isCommit) {

// 检查每个挂起的偏移量提交记录

pendingOffsetsOpt.foreach { pendingOffsets =>

pendingOffsets.forKeyValue { (topicPartition, commitRecordMetadataAndOffset) =>

// 如果commitRecordMetadataAndOffset.appendedBatchOffset为空，抛出异常。因为偏移量提交记录本身尚未添加到日志中，无法完成事务性偏移量的提交。

if (commitRecordMetadataAndOffset.appendedBatchOffset.isEmpty)

throw new IllegalStateException(s"Trying to complete a transactional offset commit for producerId $producerId " + s"and groupId $groupId even though the offset commit record itself hasn't been appended to the log.")

// 如果 offsets 不包含该分区位移提交数据，或者 offsets 中该分区对应的提交位移消息在位移主题中的位移值小于待写入的位移值

val currentOffsetOpt \= offsets.get(topicPartition)

if (currentOffsetOpt.forall(\_.olderThan(commitRecordMetadataAndOffset))) {

trace(s"TxnOffsetCommit for producer $producerId and group $groupId with offset $commitRecordMetadataAndOffset " + "committed and loaded into the cache.")

// 将该分区对应的事务提交位移消息添加到 offsets 中

offsets.put(topicPartition, commitRecordMetadataAndOffset)

} else {

trace(s"TxnOffsetCommit for producer $producerId and group $groupId with offset $commitRecordMetadataAndOffset " + s"committed, but not loaded since its offset is older than current offset $currentOffsetOpt.")

}

}

}

} else {

// 如果 isCommit 为 false，表示事务性偏移量提交被中止，记录相应的日志。

trace(s"TxnOffsetCommit for producer $producerId and group $groupId with offsets $pendingOffsetsOpt aborted")

}

}

该方法主要是在完成事务性偏移量提交后，「**更新**」和「**管理**」偏移量的状态，并确保偏移量的「**正确提交**」和「**记录事务状态**」，其执行流程基本同普通位移提交，这里就不多赘述了。

##   
**3.5 移除位移**

既然有「**提交位移**」就一定会有「**移除位移**」，所以在 offsets 中订阅分区的已消费位移值也是能够被移除的。

在 kafka 中默认的消息留存时间为 7 天，如果当前时间与已提交位移消息时间戳的差值，超过了 Broker 端参数offsets.retention.minutes 值，Kafka 就会将这条记录从 offsets 字段中移除。

对应的方法就是 [removeExpiredOffsets()](http://removeexpiredoffsets\(\)/)，该方法相对比较长，内嵌了 2 个方法，如下：

def removeExpiredOffsets(currentTimestamp: Long, offsetRetentionMs: Long): Map\[TopicPartition, OffsetAndMetadata\] = {

// 专门用于获取订阅分区过期的位移值

def getExpiredOffsets(

baseTimestamp: CommitRecordMetadataAndOffset => Long, // 计算时间戳并返回

subscribedTopics: Set\[String\] = Set.empty // 订阅主题集合，默认是空

): Map\[TopicPartition, OffsetAndMetadata\] = {

// 遍历 offsets 中的所有分区，过滤出同时满足以下 3 个条件的所有分区

// 1、分区所属主题不在订阅主题列表之内

// 2、该主题分区已经完成位移提交

// 3、该主题分区在位移主题中对应消息的存在时间超过了阈值

offsets.filter {

case (topicPartition, commitRecordMetadataAndOffset) =>

// 分区所属主题不在订阅主题列表之内，对于正在消费的主题是不能执行过期位移移除的

!subscribedTopics.contains(topicPartition.topic()) &&

// 该主题分区已经完成位移提交，对于提交中状态的分区不能移除

!pendingOffsetCommits.contains(topicPartition) && {

// 该主题分区在位移主题中对应消息的存在时间超过了阈值，对于新版 Kafka 来说，判断是否过期主要基于消费者组状态来处理。如果是 Empty 状态，是否过期是根据当前时间与消费者组变为 Empty 状态时间的差值，是否超过 Broker 端参数 offsets.retention.minutes 值；如果不是 Empty 状态，是否过期是当前时间与提交位移消息中的时间戳差值是否超过了 offsets.retention.minutes 值。如果超过了，就认为已过期，对应的位移值需要被移除；如果没有超过，就不需要移除了。

commitRecordMetadataAndOffset.offsetAndMetadata.expireTimestamp match {

case None \=\>

// current version with no per partition retention

currentTimestamp - baseTimestamp(commitRecordMetadataAndOffset) >= offsetRetentionMs

case Some(expireTimestamp) =>

// older versions with explicit expire\_timestamp field => old expiration semantics is used

currentTimestamp >= expireTimestamp

}

}

}.map {

// 为满足以上 3 个条件的分区提取出 commitRecordMetadataAndOffset 中的位移值

case (topicPartition, commitRecordOffsetAndMetadata) =>

(topicPartition, commitRecordOffsetAndMetadata.offsetAndMetadata)

}.toMap

}

// 内部调用 getExpiredOffsets 方法获取主题分区的过期位移

val expiredOffsets: Map\[TopicPartition, OffsetAndMetadata\] = protocolType match {

// 如果消费者组状态是 Empty，就传入消费者组变更为 Empty 状态的时间，如果没有该时间则使用提交位移消息本身的写入时间戳来获取过期位移

case Some(\_) if is(Empty) =>

// no consumer exists in the group =>

// - if current state timestamp exists and retention period has passed since group became Empty,

// expire all offsets with no pending offset commit;

// - if there is no current state timestamp (old group metadata schema) and retention period has passed

// since the last commit timestamp, expire the offset

getExpiredOffsets(

commitRecordMetadataAndOffset => currentStateTimestamp

.getOrElse(commitRecordMetadataAndOffset.offsetAndMetadata.commitTimestamp)

)

// 如果是普通的消费者组类型且订阅主题信息已知，就传入提交位移消息本身的写入时间戳和订阅主题集合一起来获取过期位移

case Some(ConsumerProtocol.PROTOCOL\_TYPE) if subscribedTopics.isDefined =>

// consumers exist in the group =>

// - if the group is aware of the subscribed topics and retention period had passed since the

// the last commit timestamp, expire the offset. offset with pending offset commit are not

// expired

getExpiredOffsets(

\_.offsetAndMetadata.commitTimestamp,

subscribedTopics.get

)

// 如果 protocolType 为 None，表示该消费者组是一个 Standalone 消费者，还是要传入提交位移消息本身的写入时间戳来获取过期位移

case None \=\>

// protocolType is None => standalone (simple) consumer, that uses Kafka for offset storage only

// expire offsets with no pending offset commit that retention period has passed since their last commit

getExpiredOffsets(\_.offsetAndMetadata.commitTimestamp)

// 如果消费者组的状态不符合上面的这几个 Case，此时不需要被移除

case \_ \=\>

Map()

}

if (expiredOffsets.nonEmpty)

debug(s"Expired offsets from group '$groupId': ${expiredOffsets.keySet}")

// 如果 expiredOffsets 不为空，则直接将过期位移对应的主题分区从 offsets 中移除

offsets --= expiredOffsets.keySet

// 返回主题分区对应的过期位移

expiredOffsets

}

![](https://article-images.zsxq.com/lt0uGRaferB8RF6P0PfSUMWQT_hk)

## **04 分区分配策略管理**

最后，我们剖析下消费者组「**分区分配策略**」的管理，即前面提到过的 [supportedProtocols](http://supportedprotocols/) 字段的管理。

首先，[supportedProtocols](http://supportedprotocols/) 是「**分区分配策略的支持票数**」，在前面剖析 「**消费者组成员管理**」中 「**添加组成员**」、「**移除组成员**」方法中会进行相应的更新。

当消费者组每次 [Rebalance](http://rebalance/) 的时候，需要重新确认当本次 [Rebalance](http://rebalance/) 完成结束后要使用哪个分区分配策略来同步给每个消费者，所以就需要有方法来统计这些票数，把票数最多的那个策略作为新的策略。

既然要进行统计分区分配，那么大概就会有两个步骤：「**候选分区分配策略集合**」、「**决定最终分区分配策略集合**」，我们来剖析下。

## **4.1 候选分区分配策略集合**

首先来看 [candidateProtocols](http://candidateprotocols/) 方法，其作用就是**找出组内所有成员都支持的分区分配策略集合**，源码如下：

private def candidateProtocols: Set\[String\] = {

// 获取消费者组内成员数

val numMembers \= members.size

// 该消费者组中所有成员都支持该分区分配策略，并返回它们的名称

supportedProtocols.filter(\_.\_2 == numMembers).keys.toSet

}

该方法首先会获取组内的总成员数，然后找出 [supportedProtocols](http://supportedprotocols/) 中该消费者组中所有成员都支持该分区分配策略，并返回它们的名称。

##   
**4.2 决定最终分区分配策略集合**

def selectProtocol: String = {

// 如果没有任何成员，直接抛异常

if (members.isEmpty)

throw new IllegalStateException("Cannot select protocol for empty group")

// 获取所有成员都支持的策略集合

val candidates \= candidateProtocols

// 让每个成员投票，票数最多的那个策略当选

val (protocol, \_) = allMemberMetadata

.map(\_.vote(candidates))

.groupBy(identity)

.maxBy { case (\_, votes) => votes.size }

protocol

}

这里你是否很疑惑，**这里的 vote 是如何实现的呢？**

这里我举例说明下，帮助你理解，你可以点击这里进行查看：[【原理分析系列第十四篇】图解 Kafka 消费者分区分配策略](https://articles.zsxq.com/id_esdb8l8rwxb7.html)

> 不过这里需要注意的是：成员支持列表中的策略是有顺序的，成员会倾向于选择靠前的策略。

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过「**场景驱动**」的方式从消费者组拉取数据过程中的 4 大请求中出发，抛出消费者组管理「**组状态**」、「**组成员**」、「**位移**」、「**分区分配策略**」操作。

2、带你分别剖析了「**消费者组管理**」的整个流程，从「**组成员**」、「**位移**」、「**分区分配策略**」四个维度进行剖析和梳理。

下篇我们来深度剖析「**消费者组元数据设计原理剖析**」，大家期待，我们下期见。