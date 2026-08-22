大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 初识消费者组**」，了解了 Kafka 消费者初始化后消费者组是如何初始化的，以及如何加入消费组的，另外剖析了「**Kafka 消费者组四大请求**」处理流程，今天我们开启消费端源码的征程，这是第五篇来深度聊聊「**Kafka 消费者组状态机流程**」，看看消费者组内部都有那几个状态，是如何进行流转的。

![](https://article-images.zsxq.com/Ftw7zCpDKdYyzGI1I7NXUt6s_wIH)

## **01 总体概述**

我们知道 Kafka 是一款「**高吞吐量**」、「**低延迟**」、「**高并发**」、「**高可扩展性**」的消息队列产品， 那么如果某个 Topic 拥有数百万到数千万的数据量， 仅仅依靠 Consumer 进程消费， 消费速度可想而知， 所以需要一个扩展性较好的机制来保障消费进度， 这个时候 Consumer Group 应运而生， **Consumer Group 是 Kafka 提供的可扩展且具有容错性的消费者机制****。**

通过原理篇 [【原理分析系列第五篇】图解 Kafka Consumer 架构设计](https://articles.zsxq.com/id_xqh2l0kkhc7a.html) 中，我们了解到既然是管理「**消费者**」，对于 Consumer Group 来说，可能随时会出现「**消费者启动加入**」或者「**消费者宕机退出**」情况，此时就会发生 「**消费者组重平衡 Rebalance**」，暂停消费直至完成平衡后重新开始消费。

当 「**消费者组重平衡 Rebalance**」一旦发生，必定会涉及到 Consumer Group 的「**状态流转**」，此时 Kafka 为我们设计了一套完整的「**状态机机制**」，来帮助 Broker [GroupCoordinator](http://groupcoordinator%20/) 完成整个重平衡流程。

了解整个状态流转过程可以帮助我们深入理解 Consumer Group 的设计原理，接下来我们就来深入了解其实现细节。

##   
**02 初探消费者组状态机**

在 Kafka 中设计了一套「**消费者组状态机**」（State Machine），总共包括 5 种状态：

1.  Empty 状态表示当前组内无成员， 但是可能存在 Consumer Group 已提交的位移数据，且未过期，这种状态只能响应 JoinGroup 请求。
2.  Dead 状态表示组内已经没有任何成员的状态，组内的元数据已经被 Broker Coordinator 移除，这种状态响应各种请求都是一个Response：[UNKNOWN\_MEMBER\_ID](http://unknown_member_id/)。
3.  PreparingRebalance 状态表示准备开始新的 Rebalance, 等待组内所有成员重新加入组内。
4.  CompletingRebalance 状态表示组内成员都已经加入成功，正在等待分配方案，旧版本中叫“AwaitingSync”。
5.  Stable 状态表示 Rebalance 已经平衡完成， 组内 Consumer 可以开始消费了。

5种状态流转图如下：

![](https://article-images.zsxq.com/Fqq4IxapG4V4bdlMLg8hIubDpmNN)

这里简单说明下当消费者组启动时的状态流转过程：最开始为 Empty 状态，当重平衡开启后，它会被设置为PreparingRebalance 状态等待成员加入，当加入之后变更到 CompletingRebalance 状态等待分配方案，最后分配完成后变更为 Stable 状态完成重平衡。

当有新消费者加入或已有消费者退出时，消费者组的状态会从 Stable 直接跳到 PreparingRebalance 状态，此时所有现存消费者必须重新申请加入组。

最后当所有消费者都退出消费者组后，消费者组状态会继续变更为 Empty 状态。

## **03 消费者组状态机流转源码剖析**

了解了「**消费者组状态机**」的 5 种状态的含义以及整体流转流程图之后，我们来看看其源码是如何实现的。

通过源码追踪可以发现，「**消费者组状态机**」是在 GroupMetadata.scala 类中定义的，所以它属于消费者组元数据中很重要的一部分，如下图：

  
![](https://article-images.zsxq.com/FgU2_hRtWJGIMo_K6TB1Gj2rZ835)

## **3.1 五种状态定义**

GroupState 类定义了「**消费者组状态机**」中的 5 种状态，其实现对象 Stable 的代码如下：

/\*\*

\* Group is preparing to rebalance

\*

\* action: respond to heartbeats with REBALANCE\_IN\_PROGRESS

\* respond to sync group with REBALANCE\_IN\_PROGRESS

\* remove member on leave group request

\* park join group requests from new or existing members until all expected members have joined

\* allow offset commits from previous generation

\* allow offset fetch requests

\* transition: some members have joined by the timeout => CompletingRebalance

\* all members have left the group => Empty

\* group is removed by partition emigration => Dead

\*/

private\[group\] case object PreparingRebalance extends GroupState {

// 合法前置状态，可以从 Stable、CompletingRebalance、Empty 状态进行转换

val validPreviousStates: Set\[GroupState\] = Set(Stable, CompletingRebalance, Empty)

}

/\*\*

\* Group is awaiting state assignment from the leader

\*

\* action: respond to heartbeats with REBALANCE\_IN\_PROGRESS

\* respond to offset commits with REBALANCE\_IN\_PROGRESS

\* park sync group requests from followers until transition to Stable

\* allow offset fetch requests

\* transition: sync group with state assignment received from leader => Stable

\* join group from new member or existing member with updated metadata => PreparingRebalance

\* leave group from existing member => PreparingRebalance

\* member failure detected => PreparingRebalance

\* group is removed by partition emigration => Dead

\*/

private\[group\] case object CompletingRebalance extends GroupState {

// 合法前置状态，只可以从 PreparingRebalance 状态进行转换

val validPreviousStates: Set\[GroupState\] = Set(PreparingRebalance)

}

/\*\*

\* Group is stable

\*

\* action: respond to member heartbeats normally

\* respond to sync group from any member with current assignment

\* respond to join group from followers with matching metadata with current group metadata

\* allow offset commits from member of current generation

\* allow offset fetch requests

\* transition: member failure detected via heartbeat => PreparingRebalance

\* leave group from existing member => PreparingRebalance

\* leader join-group received => PreparingRebalance

\* follower join-group with new metadata => PreparingRebalance

\* group is removed by partition emigration => Dead

\*/

private\[group\] case object Stable extends GroupState {

// 合法前置状态，只可以从 CompletingRebalance 状态进行转换

val validPreviousStates: Set\[GroupState\] = Set(CompletingRebalance)

}

/\*\*

\* Group has no more members and its metadata is being removed

\*

\* action: respond to join group with UNKNOWN\_MEMBER\_ID

\* respond to sync group with UNKNOWN\_MEMBER\_ID

\* respond to heartbeat with UNKNOWN\_MEMBER\_ID

\* respond to leave group with UNKNOWN\_MEMBER\_ID

\* respond to offset commit with UNKNOWN\_MEMBER\_ID

\* allow offset fetch requests

\* transition: Dead is a final state before group metadata is cleaned up, so there are no transitions

\*/

private\[group\] case object Dead extends GroupState {

// 合法前置状态，可以从所有状态进行转换

val validPreviousStates: Set\[GroupState\] = Set(Stable, PreparingRebalance, CompletingRebalance, Empty, Dead)

}

/\*\*

\* Group has no more members, but lingers until all offsets have expired. This state

\* also represents groups which use Kafka only for offset commits and have no members.

\*

\* action: respond normally to join group from new members

\* respond to sync group with UNKNOWN\_MEMBER\_ID

\* respond to heartbeat with UNKNOWN\_MEMBER\_ID

\* respond to leave group with UNKNOWN\_MEMBER\_ID

\* respond to offset commit with UNKNOWN\_MEMBER\_ID

\* allow offset fetch requests

\* transition: last offsets removed in periodic expiration task => Dead

\* join group from a new member => PreparingRebalance

\* group is removed by partition emigration => Dead

\* group is removed by expiration => Dead

\*/

private\[group\] case object Empty extends GroupState {

// 合法前置状态，只可以从 PreparingRebalance 状态进行转换

val validPreviousStates: Set\[GroupState\] = Set(PreparingRebalance)

}

这里你需要掌握的是，消费者组从初始化到正常工作，它的状态流转路径是 Empty -> PreparingRebalance -> CompletingRebalance -> Stable，如果当前消费者组无成员且元数据信息被删除的话会变更为 Dead。

##   
**3.2 消费者组状态管理**

消费者组状态是很重要的元数据，对于管理状态的方法很简单就两种，「**设置或者更新状态**」设置和查询，都比较简单如下：

// GroupMetadata.scala 类方法

// 设置/更新状态

def transitionTo(groupState: GroupState): Unit = {

// 确保是合法的状态转换

assertValidTransition(groupState)

// 设置状态到给定状态

state = groupState

// 更新状态变更时间戳

currentStateTimestamp = Some(time.milliseconds()

该方法的作用就是**将消费者组的状态变更给指定状态**。在变更前，需要确保这次变更必须是合法的状态转换。它是依赖每个 [GroupState](http://groupstate/) 实现类定义的 [validPreviousStates](http://validpreviousstates/) 集合来完成的。只有在这个集合中的状态，才是合法的前置状态，最终才能转换到当前状态。

同时，该方法还会**更新状态变更的时间戳**。在 Kafka 内部有个定期清除过期的消费者组位移数据的定时任务就是依靠这个时间戳字段来判断是否过期的。

// 查询当前状态

def currentState \= state

// 判断消费者组状态是指定状态

def is(groupState: GroupState) = state == groupState

// 判断消费者组状态不是指定状态

def not(groupState: GroupState) = state != groupState

其中 is 方法，被大量用在上层调用中，执行各类消费者组管理任务的前置状态校验工作。

// 消费者组能否 Rebalance 的条件是当前状态是 PreparingRebalance 状态包含的合法前置状态

def canRebalance \= PreparingRebalance.validPreviousStates.contains(state)

该方法被用来判断消费者组是否能够开启 Rebalance 操作，其判断依据是**当前状态是否在 PreparingRebalance 状态的合法前置状态中，只有这几个状态才能开启 Rebalance**。

##   
**3.3 消费者组状态流转**

本篇只是简单的带大家梳理一下，后面在剖析 「**协调器**」以及「**消费者重分配**」的时候再详细剖析。

##   
**3.3.1 消费者组服务端状态定义**

其消费者组初始状态会在「**组元数据类 GroupMetadata**」中进行初始化，如下：

@nonthreadsafe

private\[group\] class GroupMetadata(val groupId: String, initialState: GroupState, time: Time) extends Logging {

// 初始消费者组状态

private var state: GroupState = initialState

// 记录状态最近一次变更的时间戳

var currentStateTimestamp: Option\[Long\] = Some(time.milliseconds())

// 查询当前消费者组状态

def currentState: GroupState = state

....

}

## **3.3.2 消费者组客户端状态定义**

而在消费端 [AbstractCoordinator](http://abstractcoordinator/) 中会有 4 种状态跟服务端 [GroupCoodinator](http://groupcoodinator%20/) 互相对应，如下：

public abstract class AbstractCoordinator implements Closeable {

// 心跳线程

public static final String HEARTBEAT\_THREAD\_PREFIX \= "kafka-coordinator-heartbeat-thread";

public static final int JOIN\_GROUP\_TIMEOUT\_LAPSE \= 5000;

// 消费端成员状态

protected enum MemberState {

UNJOINED, // the client is not part of a group

PREPARING\_REBALANCE, // the client has sent the join group request, but have not received response

COMPLETING\_REBALANCE, // the client has received join group response, but have not received assignment

STABLE; // the client has joined and is sending heartbeats

public boolean hasNotJoinedGroup() {

return equals(UNJOINED) || equals(PREPARING\_REBALANCE);

}

}

}

可以看到 [MemberState](http://memberstate/) 是一个枚举，列举了消费者和服务端 [GroupCoordinator](http://groupcoordinator%20/) 交互的 4 种状态。

1.  UNJOINED：表示消费者还没加入到服务端 [GroupCoordinator](http://groupcoordinator/) 中。
2.  PREPARING\_REBALANCE：表示消费者发出了加入 [GroupCoordinator](http://groupcoordinator/) 的请求，但是还没收到响应。
3.  COMPLETING\_REBALANCE：表示消费者收到了加入 [GroupCoordinator](http://groupcoordinator/) 的响应。
4.  STABLE：表示消费者发出了加入 [GroupCoordinator](http://groupcoordinator/) 中，并正在向 [GroupCoordinator](http://groupcoordinator/) 发送心跳。

对于服务端 [GroupCoordinator](http://groupcoordinator/) 中 [GroupState](http://groupstate/) 的变更，其实基本都是由消费端来触发的，一个「**消费者组**」在最初初始化的过程其实就是该「**消费者组**」第一个 Consumer 客户端初始化的过程，这个过程在上两篇都已经剖析过了，这里只挑关于状态变更相关的源码来梳理。

## **3.3.3 发送加入消费者组请求 JOIN\_GROUP**

首先在「**消费者组**」初始化时，会先发送「**JOIN\_GROUP**」请求给 [GroupCoordinator](http://groupcoordinator/) 要求加入消费者组会调用 [joinGroupIfNeeded()](http://joingroupifneeded\(\)/)，该方法会在 while 循环中不断进行加入消费者组的尝试，直至成功。

boolean joinGroupIfNeeded(final Timer timer) {

// 如果需要重新加入或正在等待重新加入，则继续循环

while (rejoinNeededOrPending()) {

....

// 1、准备请求，准备 JOIN\_GROUP 类型请求，存储到 unsent 队列中，调用 AbstractCoordinator#initiateJoinGroup() 方法生成加入消费者组的 JoinGroup 异步请求，并通过轮询等待响应

final RequestFuture<ByteBuffer> future = initiateJoinGroup();

// 2、调用 ConsumerNetworkClinet#poll() 方法发送请求，轮询处理待发送队列中的所有可发送的请求，这部分在上一节中有分析，不再赘述

client.poll(future, timer);

....

}

return true;

}

// 初始化加入消费者组，并发送 JOIN\_GROUP 请求给 GroupCoordinator

private synchronized RequestFuture<ByteBuffer> initiateJoinGroup() {

if (joinFuture == null) {

// 1、消费者客户端设置为 PREPARING\_REBALANCE 准备重平衡

state = MemberState.PREPARING\_REBALANCE;

// 如果之前的重平衡失败，可能会连续触发重平衡，这种情况下不更新开始时间。

if (lastRebalanceStartMs == -1L)

lastRebalanceStartMs = time.milliseconds();

// 2、发送 JOIN\_GROUP 请求

joinFuture = sendJoinGroupRequest();

joinFuture.addListener(new RequestFutureListener<ByteBuffer>() {

@Override

public void onSuccess(ByteBuffer value) {

// joinFuture 成功完成时不做任何操作，所有的处理逻辑已经在 SyncGroupResponseHandler 中了

}

@Override

public void onFailure(RuntimeException e) {

// 请求完成后处理失败的情况

// 如果在唤醒后完成了加入消费者组，则忽略异常，重新加入群组

synchronized (AbstractCoordinator.this) {

sensors.failedRebalanceSensor.record();

}

}

});

}

return joinFuture;

}

// 当准备工作做好后，就可以开始发送「JoinGroup 」请求了

RequestFuture<ByteBuffer> sendJoinGroupRequest() {

// 如果 coordinator 不可用，则返回 RequestFuture，其状态为未知协调器

if (coordinatorUnknown())

return RequestFuture.coordinatorNotAvailable();

// 向协调器发送 JoinGroup 请求

log.info("(Re-)joining group");

JoinGroupRequest.Builder requestBuilder \= new JoinGroupRequest.Builder(

new JoinGroupRequestData()

.setGroupId(rebalanceConfig.groupId)

.setSessionTimeoutMs(this.rebalanceConfig.sessionTimeoutMs)

.setMemberId(this.generation.memberId)

.setGroupInstanceId(this.rebalanceConfig.groupInstanceId.orElse(null))

.setProtocolType(protocolType())

.setProtocols(metadata())

.setRebalanceTimeoutMs(this.rebalanceConfig.rebalanceTimeoutMs)

);

log.debug("Sending JoinGroup ({}) to coordinator {}", requestBuilder, this.coordinator);

// 由于重平衡超时时间是协调器可能会阻塞的最长时间，因此我们使用重平衡超时时间来覆盖请求超时时间。我们额外加了 5 秒用于解决可能出现的小延迟。

int joinGroupTimeoutMs \= Math.max(client.defaultRequestTimeoutMs(),

rebalanceConfig.rebalanceTimeoutMs + JOIN\_GROUP\_TIMEOUT\_LAPSE);

return client.send(coordinator, requestBuilder, joinGroupTimeoutMs)

.compose(new JoinGroupResponseHandler(generation));

}

## **3.3.4 KafkaApis.handle() 处理 JoinGroup**

  
![](https://article-images.zsxq.com/FokxHzcNue70eLgMDFjqoFPdEzHj)

可以看到，处理 [JoinGroupRequest](http://joingrouprequest/) 请求的方法是 [handleJoinGroupRequest()](http://%20handlejoingrouprequest\(\)/)。它的主要逻辑是调用[GroupCoordinator#handleJoinGroup()](http://groupcoordinator/#handleJoinGroup\(\)) 方法来处理消费者组成员发送过来的加入组请求，这里简单来看下，后面会详细剖析。

## **3.3.5 GroupCoordinator.handleJoinGroup()**

![](https://article-images.zsxq.com/FgDhnjD2xYz19lEBB30I7YzcHq7F)

首先获取消费者组的信息，如果消费者组不存在时，此时会创建消费者组，并设置状态为 [Empty](http://empty/)，如下：

![](https://article-images.zsxq.com/Ft-1zsWY_3LGAE9BRHuS5BRZb2e-)

如果消费者组存在或者创建完成后，开始接受正在加入消费者组成员方法，源码如下：

![](https://article-images.zsxq.com/FsfsU7QySVXVjWJfRHqwo4ydgCw8)

如果当前消费者组成员 ID 为空，执行加入消费组操作方法，源码如下：

![](https://article-images.zsxq.com/FsJtMup-hFnrRNv_rBvJ68K5Y-GI)

如果当前消费者组成员 ID 不为空，执行加入消费组操作方法，源码如下：

![](https://article-images.zsxq.com/Fo3M5N1or9KVYDm1ov0KzaUDVRdr)

![](https://article-images.zsxq.com/Fonalf0fjUwrltkeiJ4Mzv8TQhSF)

![](https://article-images.zsxq.com/FvXrgNtyb9ejG0aFoS3QkypLamET)

当「**JOIN\_GROUP**」请求在 [GroupCoordinator](http://groupcoordinator/) 中添加组成功会将消费者组状态变更为 [PreparingRebalance](http://preparingrebalance/) 准备进行重分配，当操作完成后会返回响应到消费者端，此时消费者端会判断自己是否当选为「**LeaderConsumer**」，如果当选后就会发送 「**SYNC\_GROUP**」请求。

##   
**3.3.6 发送分区分配方案请求 SYNC\_GROUP**

这里只有「**LeaderConsumer**」才有资格去发送「**SYNC\_GROUP**」请求。

private RequestFuture<ByteBuffer> onJoinLeader(JoinGroupResponse joinResponse) {

try {

// 1、调用 ConsumerCoordinator#performAssignment() 进行分区分配

Map<String, ByteBuffer> groupAssignment = performAssignment(joinResponse.data().leader(), joinResponse.data().protocolName(),

joinResponse.data().members());

List<SyncGroupRequestData.SyncGroupRequestAssignment> groupAssignmentList = new ArrayList<>();

for (Map.Entry<String, ByteBuffer> assignment : groupAssignment.entrySet()) {

// 生成SyncGroup 异步请求，调用 AbstractCoordinator#sendSyncGroupRequest() 方法将分配方案同步给协调器

groupAssignmentList.add(new SyncGroupRequestData.SyncGroupRequestAssignment()

.setMemberId(assignment.getKey())

.setAssignment(Utils.toArray(assignment.getValue()))

);

}

// ApiKeys.SYNC\_GROUP 类型请求，给服务端发送制定完成的分区分配策略

SyncGroupRequest.Builder requestBuilder \=

new SyncGroupRequest.Builder(

new SyncGroupRequestData()

.setGroupId(rebalanceConfig.groupId)

.setMemberId(generation.memberId)

.setProtocolType(protocolType())

.setProtocolName(generation.protocolName)

.setGroupInstanceId(this.rebalanceConfig.groupInstanceId.orElse(null))

.setGenerationId(generation.generationId)

.setAssignments(groupAssignmentList)

);

// 2、准备请求 存储到 unsent 队列

return sendSyncGroupRequest(requestBuilder);

} catch (RuntimeException e) {

return RequestFuture.failure(e);

}

}

## **3.3.7 KafkaApis.handle() 处理 SyncGroup**

  
![](https://article-images.zsxq.com/FvDScqLbhFc_gdpIO1JT9xjZSHO3)

可以看到，处理 [SyncGroupRequest](http://syncgrouprequest/) 请求的方法是 [](http://handlejoingrouprequest\(\)/)[handleSyncGroupRequest()](http://handlesyncgrouprequest\(\)/)。它的主要逻辑是调用[GroupCoordinator#handleSyncGroup()](http://groupcoordinator/#handleSyncGroup\(\)) 方法来处理分区分配方案请求，这里简单来看下，后面会详细剖析。

![](https://article-images.zsxq.com/FmJ2k4atQEI09zZRl0evqCUgHAmg)

执行组同步任务操作，源码如下：

![](https://article-images.zsxq.com/FnD4PqvcKHCqSKf1qav5arZV800B)

从上面源码可以看到，当分配完成后，整个消费者组的状态变更为 [Stable](http://stable/)，如果没有分配失败，需要开启新一轮的 Rebalance，此时整个消费者组的状态变更为 [prepareRebalance](http://preparerebalance/)。

## **3.3.8 状态流转总结**

从上面这些请求操作可以看出对「**消费者组状态机**」中 5 种状态的流转，这里通过一张图来梳理下其状态流转，如下：

  
![](https://article-images.zsxq.com/FkWKMiIWlVMawQWRA-R9XGYDEFqr)

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、通过「**场景驱动**」的方式从消费者调用出发，抛出消费者组状态机的 5 种状态以及消费者组状态是如何流转的?

2、带你剖析了「**消费者组状态机**」的整个流转流程，从「**发送 JOIN\_GROUP 请求**」、「**发送 SYNC\_GROUP 请求**」两个维度进行剖析和梳理。

3、最后通过一张图来梳理其状态流转。

下篇我们来深度剖析「**消费者组组管理全流程剖析**」，大家期待，我们下期见。