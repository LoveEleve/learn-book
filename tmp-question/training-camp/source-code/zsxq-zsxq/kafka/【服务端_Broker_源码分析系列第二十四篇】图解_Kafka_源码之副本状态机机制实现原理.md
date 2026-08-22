大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端 Topic 删除请求处理流程**」，了解了 Topic 是如何被删除出来的，从今天开始，我们来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 副本状态机机制实现原理**」。

![](https://article-images.zsxq.com/FgozYn_zve4cRvSNzr0KsKPyB0bC)

##   
**01 总体概述**

在前面剖析 「**KafkaController**」以及 「**Topic 创建、删除流程**」中或多或少都提及了「**副本状态机 ReplicaStateMachine**」、「**分区状态机 PartitionStateMachine**」。

对于状态机来说，主要应用于事件处理中，并且事件会有多种状态。当事件状态发生变化时，会触发对于的事件处理动作。Kafka 控制器启动「**状态机**」时有以下两个特点：

1.  「**分区状态机**」和「**副本状态机**」需要分别获取集群的所有分区和所有副本，而初始化 ControllerContext 会从控制器中读取集群的所有分区与副本，所以初始化 ControllerContext 后，才能启动「**状态机**」。
2.  分区中包含了多个副本，只有集群中所有副本的状态都初始化完毕，才可以初始分区的状态，所以控制器会先启动「**副本状态机**」，然后才启动「**分区状态机**」。

所以现在你应该大概知道它们是用来管理 Kafka 中分区和副本状态转换的，那么你是否知道在「**副本**」、「**分区**」内部都有哪些状态吗？它们都是如何转换的？

带着这些问题，我们开启对这两个组件的源码剖析，今天先来看下「**副本状态机 ReplicaStateMachine**」的实现机制。

## **02 ReplicaStateMachine**

![](https://article-images.zsxq.com/FuinpqXRiS9YH35f7gbq2hT3FYMz)

从图中可以看出，这两个状态机组件都是在 Controller 包下。

先来看下副本状态机的源码结构总览。

## **2.1 源码结构总览**

「**ReplicaStateMachine****.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/controller/ReplicaStateMachine.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/controller/ReplicaStateMachine.scala)

![](https://article-images.zsxq.com/FkfZEme5fA06kFzYsc93CorqGNEN)

如上图其源码主要可以分为 3 部分，如下：

1.  **ReplicaStateMachine**：它是副本状态机抽象类，定义了一些常用方法如 startup、shutdown、initializeReplicaState，以及状态机最重要的处理逻辑方法 handleStateChanges。
2.  **ZkReplicaStateMachine**：它是副本状态机具体实现类，内部重写了 handleStateChanges 方法，主要实现了副本状态之间的状态转换。目前版本中，ZkReplicaStateMachine 是唯一的 ReplicaStateMachine 子类。
3.  **ReplicaState**：副本状态集合，在 Kafka 目前共定义了 7 种副本状态，「**NewReplica**」、「**OnlineReplica**」、「**OfflineReplica**」、「**ReplicaDeletionStarted**」、「**ReplicaDeletionSuccessful**」、「**ReplicaDeletionIneligible**」、「**NoExistentReplica**」。

## **2.2 ReplicaStateMachine 抽象类**

先来看下抽象类，都比较简单，源码如下：

// 一个抽象类，用于管理副本的状态机

abstract class ReplicaStateMachine(controllerContext: ControllerContext) extends Logging {

/\*\*

\* Invoked on successful controller election.

\* 它是在成功选举为控制器后调用

\*/

def startup(): Unit = {

info("Initializing replica state")

// 初始化副本的状态

initializeReplicaState()

info("Triggering online replica state changes")

// 获取在线和离线副本列表

val (onlineReplicas, offlineReplicas) = controllerContext.onlineAndOfflineReplicas

// 处理在线副本的状态变化

handleStateChanges(onlineReplicas.toSeq, OnlineReplica)

info("Triggering offline replica state changes")

// 处理离线副本的状态变化

handleStateChanges(offlineReplicas.toSeq, OfflineReplica)

debug(s"Started replica state machine with initial state -> ${controllerContext.replicaStates}")

}

/\*\*

\* Invoked on controller shutdown.

\* 在控制器关闭时调用

\*/

def shutdown(): Unit = {

info("Stopped replica state machine")

}

/\*\*

\* Invoked on startup of the replica's state machine to set the initial state for replicas of all existing partitions in zookeeper

\* 在启动副本状态机时调用，用于设置所有现有分区副本的初始状态

\*/

private def initializeReplicaState(): Unit = {

// 遍历所有分区

controllerContext.allPartitions.foreach { partition =>

val replicas \= controllerContext.partitionReplicaAssignment(partition)

replicas.foreach { replicaId =>

val partitionAndReplica \= PartitionAndReplica(partition, replicaId)

if (controllerContext.isReplicaOnline(replicaId, partition)) {

// 如果副本在线，则将其状态设置为OnlineReplica

controllerContext.putReplicaState(partitionAndReplica, OnlineReplica)

} else {

// 如果副本所在的broker已宕机，则将其状态设置为 ReplicaDeletionIneligible，用于删除 topic

controllerContext.putReplicaState(partitionAndReplica, ReplicaDeletionIneligible)

}

}

}

}

// 状态机最重要的处理逻辑方法 子类会实现它

def handleStateChanges(replicas: Seq\[PartitionAndReplica\], targetState: ReplicaState): Unit

}

可以看到 ReplicaStateMachine 类只需要接收一个 ControllerContext 对象。在前面讲解过 ControllerContext 封装了 Controller 端保存的所有集群元数据信息。

它定义了在 「**Controller**」选举成功时调用的 startup()，当「**Controller**」关闭后调用的 shutdown()，以及在启动副本状态机时调用的初始化方法等。

接下来我们来看下其子类，也就是其实现类，在目前版本中，ZkReplicaStateMachine 是唯一的 ReplicaStateMachine 子类。

## **2.3 ZkReplicaStateMachine 实现类之初始化**

class ZkReplicaStateMachine(config: KafkaConfig,

stateChangeLogger: StateChangeLogger,

controllerContext: ControllerContext,

zkClient: KafkaZkClient,

controllerBrokerRequestBatch: ControllerBrokerRequestBatch)

extends ReplicaStateMachine(controllerContext) with Logging {

....

}

ZKReplicaStateMachine 类的属性相对多一些。如果你想要构造一个 **Zk****ReplicaStateMachine** 实例，大致需要以下几个：

1.  ControllerContext 实例：封装了 Controller 端保存的所有集群元数据信息。
2.  KafkaZkClient对象实例：负责与 ZooKeeper 进行交互。
3.  ControllerBrokerRequestBatch 实例：用于给集群 Broker 发送控制类请求，即 LeaderAndIsrRequest、StopReplicaRequest和UpdateMetadataRequest。

接着我们来看下「**副本状态机**」是在何时进行初始化的。

不知道在前面讲解 「**KafkaController**」时注意到，在其对象构建时，会创建一个 **ZkReplicaStateMachine** 实例

，源码如下：

![](https://article-images.zsxq.com/Fs9bEzs4rF6zTaRaKGP28hdiIFUR)

看到这里你是否会有疑惑：「**如果当一个 Broker 还未被选举成为 Controller 时，它也会构建 kafkaController 对象以及初始化** **Zk****ReplicaStateMachine 实例吗？**」

答案：是的，在所有 Broker 启动时，都会创建 KafkaController 实例以及初始化 ZKReplicaStateMachine 实例的，但是**这不代表每个 Broker 都会启动副本状态机**。为什么这么说呢？

实际上，只有被选举成为 Controller 所在的 Broker 上， 「**副本状态机**」才会被启动，而具体的启动代码位于KafkaController#onControllerFailover 方法，源码如下：

private def onControllerFailover(): Unit = {

......

replicaStateMachine.startup() // 启动副本状态机

partitionStateMachine.startup() // 启动分区状态机

......

}

当 Broker 被成功选举成为 Controller 后，onControllerFailover 方法会被调用，进而启动该 Broker 早已创建好的「**副本状态机**」和「**分区状态机**」。

了解这些后，接着我们来聊聊 「**副本状态**」以及 「**状态转换流程**」。

##   
**2.4 副本状态以及状态转换流程**

当「**副本状态机**」启动后，它就要行使「**处理副本状态的转换**」权力了。

不过，在学习如何处理状态转换之前，我们必须要弄明白一个问题就是：**当前都有哪些状态，它们都代表什么?** 源码中的 ReplicaState 定义了 7 种副本状态，如下：

![](https://article-images.zsxq.com/FkLct1BCkqO_TFaXQNoOAo6nruTG)

1.  NewReplica：副本被刚创建后状态。
2.  OnlineReplica：副本正常提供服务时状态。
3.  OfflineReplica：副本服务下线时状态。
4.  ReplicaDeletionStarted：副本被删除时状态。
5.  ReplicaDeletionSuccessful：副本被成功删除后状态。
6.  ReplicaDeletionIneligible：开启副本删除但副本暂时无法被删除时状态。
7.  NonExistentReplica：副本从副本状态机被移除前所处的状态。

先来看下 「**ReplicaState**」接口的定义：

// ReplicaState接口

sealed trait ReplicaState {

// 定义状态序号

  def state: Byte

// 定义合法的前置状态

  def validPreviousStates: Set\[ReplicaState\]

}

这 7 种副本状态都继承了该接口，并实现了对象定义了「**该副本状态的序号**」、「**该副本状态的合法前置转换状态**」。

case object NewReplica extends ReplicaState {

val state: Byte = 1

val validPreviousStates: Set\[ReplicaState\] = Set(NonExistentReplica)

}

case object OnlineReplica extends ReplicaState {

val state: Byte = 2

val validPreviousStates: Set\[ReplicaState\] = Set(NewReplica, OnlineReplica, OfflineReplica, ReplicaDeletionIneligible)

}

case object OfflineReplica extends ReplicaState {

val state: Byte = 3

val validPreviousStates: Set\[ReplicaState\] = Set(NewReplica, OnlineReplica, OfflineReplica, ReplicaDeletionIneligible)

}

case object ReplicaDeletionStarted extends ReplicaState {

val state: Byte = 4

val validPreviousStates: Set\[ReplicaState\] = Set(OfflineReplica)

}

case object ReplicaDeletionSuccessful extends ReplicaState {

val state: Byte = 5

val validPreviousStates: Set\[ReplicaState\] = Set(ReplicaDeletionStarted)

}

case object ReplicaDeletionIneligible extends ReplicaState {

val state: Byte = 6

val validPreviousStates: Set\[ReplicaState\] = Set(OfflineReplica, ReplicaDeletionStarted)

}

case object NonExistentReplica extends ReplicaState {

val state: Byte = 7

val validPreviousStates: Set\[ReplicaState\] = Set(ReplicaDeletionSuccessful)

}

从上面源码中可以看到，为了确保状态机的正常运行，其状态转换都是有前置条件的。而 [validPreviousStates](http://validpreviousstates/) 属性就是用来存储前置条件的，它是一个集合类型，里面包含了每个副本状态的「**合法前置转换状态**」，它是需要你重点了解和掌握的，对于集合不存在的副本状态来转换到该副本状态时，它就是「**非法的状态转换**」。

这里拿「**OfflineReplica**」为例，可以看到里面包含了 NewReplica、OnlineReplica、OfflineReplica、ReplicaDeletionIneligible 这 4 种状态，换句话说，**Kafka 只允许副本从刚刚这 4 种状态转换到** 「**OfflineReplica**」**状态**，**如果想从 ReplicaDeletionStarted 状态转换到** 「**OfflineReplica**」**状态时，就是非法的状态转换**。

其他状态也一样，这里我就不一一介绍了，你可以对照着上面源码自己研究下。

这里我通过一张图来帮你总结下完整的状态转换，如下：

  
![](https://article-images.zsxq.com/Fp42gpJxAa2LZ_HSgbItPSsqXHJG)

根据上面状态转换图，这里再给大家梳理下各个状态的流转过程：

1.  当副本首次被创建后，它会被置于 「**NewReplica**」状态。经过初始化当副本能够对外提供服务之后，状态机会将其变更为 「**OnlineReplica**」并一直以该状态持续工作。
2.  当副本所在的 Broker 关闭或者是其他原因不能正常工作了，副本就需要从 「**OnlineReplica**」变更为「**OfflineReplica**」，表示副本已处于离线状态。
3.  一旦触发了删除 Topic 这样的操作，此时状态机会将副本状态变更为「**ReplicaDeletionStarted**」 ，表示副本删除任务已然开启。
4.  假如删除 Topic 成功，则变更为 「**ReplicaDeletionSuccessful**」状态。
5.  假如删除失败（比如所在 Broker 处于下线状态），那就变更为 「**ReplicaDeletionIneligible**」状态，方便后续重试。
6.  当副本被删除后，其状态会变更为「**NonExistentReplica**」 状态，即从副本状态机将移除该副本数据。

以上就是一个基本的状态管理流程。

接下来，我们来看下具体实现类里面是如何实现这些状态流转的。

## **2.5 ZkReplicaStateMachine 实现类方法介绍**

该类总共定义了 8 个方法，其中 1 个为 public ，剩下 7 个都是 private，这个 public 方法是副本状态机最重要的逻辑 [handleStateChanges](http://handlestatechanges%20/) 方法。而那 7 个方法全部都是用来辅助 public 方法的。

这里先简单的介绍下其他 7 个辅助方法都是做什么的，只有清楚了这些方法的用途，你才能更好地理解handleStateChanges 的实现逻辑，根据源码从上到下介绍。

1.  doHandleStateChanges：它是执行状态变更和转换操作的具体方法。接下来会详细剖析其源码。
2.  removeReplicasFromIsr：调用 doRemoveReplicasFromIsr 方法，其实现了将给定的副本对象从给定分区 ISR 副本列表中移除的功能。
3.  doRemoveReplicasFromIsr：把给定的副本对象从给定分区 ISR 副本列表中移除。
4.  getTopicPartitionStatesFromZk：从 ZooKeeper 中获取指定分区的状态信息，其中包括每个分区的 **Leader 副本**、**ISR 集合**等数据。
5.  logSuccessfulTransition：仅仅记录一次成功的状态转换操作日志。
6.  logInvalidTransition：记录错误之用，其记录一次非法的状态转换。
7.  logFailedStateChange：仅仅记录一条错误日志，表示执行了一次无效的状态变更。

接下来我们分别来看下其重要方法的具体实现。

## **2.6 handleStateChanges()**

override def handleStateChanges(replicas: Seq\[PartitionAndReplica\], targetState: ReplicaState): Unit = {

if (replicas.nonEmpty) {

try {

// 清空 Controller 待发送请求集合

controllerBrokerRequestBatch.newBatch()

// 将所有副本对象按照 Broker 进行分组，然后依次执行状态转换操作

replicas.groupBy(\_.replica).forKeyValue { (replicaId, replicas) =>

doHandleStateChanges(replicaId, replicas, targetState)

}

// 发送对应的 Controller 请求给 Broker

controllerBrokerRequestBatch.sendRequestsToBrokers(controllerContext.epoch)

} catch {

// 如果 Controller 已经易主，则记录错误日志然后抛出异常

case e: ControllerMovedException =>

error(s"Controller moved to another broker when moving some replicas to $targetState state", e)

throw e

case e: Throwable => error(s"Error while moving some replicas to $targetState state", e)

}

}

}

该方法的主要作用是**处理状态变更的，是对外提供状态转换操作的入口**。

其接收两个参数：

1.  replicas：是一组副本对象，每个副本对象都封装了各自所属的主题、分区以及副本所在的 Broker ID 数据。
2.  targetState：是对这些副本对象要转换成的目标状态。

整体逻辑可以分为两步：

1.  将所有副本对象 replicas 按照 Broker ID 进行分组，然后依次执行状态转换操作，即循环调用 [doHandleStateChanges](http://dohandlestatechanges%20/) 方法执行真正的副本状态转换。
2.  给集群中的相应 Broker 批量发送请求。

接下来，重点剖析 [doHandleStateChanges](http://dohandlestatechanges/) 方法。

## **2.7 doHandleStateChanges()**

这里先从整体来看下该方法的源码，它主要是用来执行具体状态转换操作的，方法看着很长，其实都是不同的代码分支，如下：

![](https://article-images.zsxq.com/FvYFTbTzGkIJoNv3d70rk4hmgpvW)

这里先通过一张图来梳理下其流程，再接着挨个剖析其分支源码。

![](https://article-images.zsxq.com/FniLAgmKHaea57_qqZ9j5Db6wdW8)

步骤如下：

1.  会先尝试从 Controller 端元数据缓存中获取这些给定副本对象的当前状态，如果没有保存某个副本对象的状态，则将其设置为 [NonExistentReplica](http://nonexistentreplica%20/) 状态。
2.  根据不同 ReplicaState 中定义的合法前置状态集合以及传入的目标状态 targetState，对这些给定的副本对象集合进行校验合法性，并划分成两部分。
3.  能够被合法转换的副本对象集合。
4.  执行非法状态转换的副本对象集合，然后对其中每个副本对象记录一条错误日志。
5.  对能够执行合法转换的副本对象集合进行匹配并进入到不同的代码分支。在上面已经剖析了总共定义了 7 类状态，所以这里的分支也总共有 7 个。

接下来，我们挨个剖析了每个分支的源码实现。

## **2.7.1 目标状态变更为 NewReplica**

case NewReplica \=\>

// 遍历所有能够执行转换的副本对象

validReplicas.foreach { replica =>

// 获取该副本对象的分区对象，即 <主题名，分区号> 数据

val partition \= replica.topicPartition

// 从 Controller 元数据中获取副本对象的当前状态

val currentState \= controllerContext.replicaState(replica)

// 尝试从 Controller 元数据缓存中获取该分区当前信息, 包括 Leader、ISR 副本列表等信息

controllerContext.partitionLeadershipInfo(partition) match {

// 如果成功拿到分区数据信息

case Some(leaderIsrAndControllerEpoch) =>

// 如果该副本是 Leader 副本

if (leaderIsrAndControllerEpoch.leaderAndIsr.leader == replicaId) {

val exception \= new StateChangeFailedException(s"Replica $replicaId for partition $partition cannot be moved to NewReplica state as it is being requested to become leader")

// 记录错误日志，因为 Leader 副本不能被设置成 NewReplica 状态。

logFailedStateChange(replica, currentState, OfflineReplica, exception)

} else {

// 如果该副本不是 Leader 副本，则给该副本所在的 Broker 发送 LeaderAndIsrRequest 请求同步该分区的数据, 之后再给集群当前所有 Broker 发送 UpdateMetadataRequest 通知它们该分区数据发生变更。

controllerBrokerRequestBatch.addLeaderAndIsrRequestForBrokers(Seq(replicaId),

replica.topicPartition,

leaderIsrAndControllerEpoch,

controllerContext.partitionFullReplicaAssignment(replica.topicPartition),

isNew = true)

if (traceEnabled)

logSuccessfulTransition(stateLogger, replicaId, partition, currentState, NewReplica)

// 接着更新元数据缓存中该副本对象的当前状态为 NewReplica

controllerContext.putReplicaState(replica, NewReplica)

}

case None \=\>

// 如果没有相应数据

if (traceEnabled)

logSuccessfulTransition(stateLogger, replicaId, partition, currentState, NewReplica)

// 仅仅更新元数据缓存中该副本对象的当前状态为 NewReplica

controllerContext.putReplicaState(replica, NewReplica)

}

}

下面通过一张图来梳理其流程：

![](https://article-images.zsxq.com/FoZfVkRsJ3MMi9d07oO8MuKF-U0W)

## **2.7.2 目标状态变更为 OnlineReplica**

这是副本对象正常工作时的状态。我们来看下要变更到该状态，源码都做了哪些事情：

case OnlineReplica \=\>

// 遍历所有能够执行转换的副本对象

validReplicas.foreach { replica =>

// 获取该副本对象的分区对象，即 <主题名，分区号> 数据

val partition \= replica.topicPartition

// 从 Controller 元数据中获取副本对象的当前状态

val currentState \= controllerContext.replicaState(replica)

currentState match {

// 如果当前状态是 NewReplica

case NewReplica \=\>

// 从元数据缓存中拿到分区副本列表

val assignment \= controllerContext.partitionFullReplicaAssignment(partition)

// 如果副本列表不包含当前副本，则为异常情况

if (!assignment.replicas.contains(replicaId)) {

error(s"Adding replica ($replicaId) that is not part of the assignment $assignment")

// 将该副本加入到副本列表中，并更新元数据缓存中该分区的副本列表

val newAssignment \= assignment.copy(replicas = assignment.replicas :+ replicaId)

controllerContext.updatePartitionFullReplicaAssignment(partition, newAssignment)

}

// 如果当前状态是其他状态

case \_ \=\>

// 尝试获取该分区当前信息数据

controllerContext.partitionLeadershipInfo(partition) match {

// 如果存在分区信息则向该副本对象所在 Broker 发送请求同步该分区数据

case Some(leaderIsrAndControllerEpoch) =>

controllerBrokerRequestBatch.addLeaderAndIsrRequestForBrokers(Seq(replicaId),

replica.topicPartition,

leaderIsrAndControllerEpoch,

controllerContext.partitionFullReplicaAssignment(partition), isNew = false)

case None \=\>

}

}

if (traceEnabled)

logSuccessfulTransition(stateLogger, replicaId, partition, currentState, OnlineReplica)

// 将该副本对象设置成 OnlineReplica 状态

controllerContext.putReplicaState(replica, OnlineReplica)

}

下面通过一张图来梳理其流程：

![](https://article-images.zsxq.com/ljmKUx2Q_Y3hXyqztXZnAbNQn3VA)

## **2.7.3 目标状态变更为 OfflineReplica**

case OfflineReplica \=\>

// 遍历所有能够执行转换的副本对象

validReplicas.foreach { replica =>

// 向副本所在 Broker 发送 StopReplicaRequest 请求，停止对应副本。

// StopReplicaRequest 请求被发送出去之后，这些 Broker 上对应的副本就停止工作了。

controllerBrokerRequestBatch.addStopReplicaRequestForBrokers(Seq(replicaId), replica.topicPartition, deletePartition = false)

}

// 将副本对象集合划分成有 Leader 信息的副本集合和无 Leader 信息的副本集合

// 有无 Leader 信息并不仅仅包含 Leader，还有 ISR 和 controllerEpoch 等数据

val (replicasWithLeadershipInfo, replicasWithoutLeadershipInfo) = validReplicas.partition { replica =>

controllerContext.partitionLeadershipInfo(replica.topicPartition).isDefined

}

// 对于有 Leader 信息的副本集合来说，从它们对应的所有分区中移除该副本对象并更新 ZooKeeper节点

val updatedLeaderIsrAndControllerEpochs \= removeReplicasFromIsr(replicaId, replicasWithLeadershipInfo.map(\_.topicPartition))

// 遍历每个更新过的分区信息

updatedLeaderIsrAndControllerEpochs.forKeyValue { (partition, leaderIsrAndControllerEpoch) =>

stateLogger.info(s"Partition $partition state changed to $leaderIsrAndControllerEpoch after removing replica $replicaId from the ISR as part of transition to $OfflineReplica")

// 如果分区对应主题并未被删除

if (!controllerContext.isTopicQueuedUpForDeletion(partition.topic)) {

// 获取该分区除给定副本之外的其他副本所在的 Broker

val recipients \= controllerContext.partitionReplicaAssignment(partition).filterNot(\_ == replicaId)

// 向这些 Broker 发送 LeaderAndIsrRequest 请求，去更新停止副本操作之后的分区信息

controllerBrokerRequestBatch.addLeaderAndIsrRequestForBrokers(recipients,

partition,

leaderIsrAndControllerEpoch,

controllerContext.partitionFullReplicaAssignment(partition), isNew = false)

}

// 获取分区的副本列表

val replica \= PartitionAndReplica(partition, replicaId)

// 从 Controller 元数据中获取副本对象的当前状态

val currentState \= controllerContext.replicaState(replica)

if (traceEnabled)

logSuccessfulTransition(stateLogger, replicaId, partition, currentState, OfflineReplica)

// 设置该分区给定副本的状态为 OfflineReplica

controllerContext.putReplicaState(replica, OfflineReplica)

}

// 接着遍历无 Leader 信息的所有副本对象

replicasWithoutLeadershipInfo.foreach { replica =>

// 从 Controller 元数据中获取无 Leader 副本对象的当前状态

val currentState \= controllerContext.replicaState(replica)

if (traceEnabled)

logSuccessfulTransition(stateLogger, replicaId, replica.topicPartition, currentState, OfflineReplica)

// 向集群所有 Broker 发送请求，更新对应分区的元数据

// 对无 Leader 来说，因为我们没有执行任何 Leader 选举操作，所以给这些副本所在的 Broker 发送的就不是 LeaderAndIsrRequest 请求了，而是 UpdateMetadataRequest 请求，去告知它们更新对应分区的元数据。

controllerBrokerRequestBatch.addUpdateMetadataRequestForBrokers(controllerContext.

liveOrShuttingDownBrokerIds.toSeq, Set(replica.topicPartition))

// 设置该分区给定副本的状态为 OfflineReplica

controllerContext.putReplicaState(replica, OfflineReplica)

}

下面依然通过一张图来梳理其流程：

![](https://article-images.zsxq.com/lvfZTLk2tAdvXLAEmijegNB6GeDP)

简单用一句话概括就是：停止对应副本 + 更新远端 Broker 元数据的操作。

## **2.7.4 目标状态变更为 ReplicaDeletionStarted**

case ReplicaDeletionStarted \=\>

// 遍历所有能够执行转换的副本对象

validReplicas.foreach { replica =>

// 从 Controller 元数据中获取副本对象的当前状态

val currentState \= controllerContext.replicaState(replica)

if (traceEnabled)

logSuccessfulTransition(stateLogger, replicaId, replica.topicPartition, currentState, ReplicaDeletionStarted)

// 设置该分区给定副本的状态为 ReplicaDeletionStarted

controllerContext.putReplicaState(replica, ReplicaDeletionStarted)

// 向副本所在 Broker 发送 StopReplicaRequest 请求，停止对应副本。

controllerBrokerRequestBatch.addStopReplicaRequestForBrokers(Seq(replicaId), replica.topicPartition, deletePartition = true)

}

该方法比较简单，步骤如下：

1.  遍历所有能够执行转换的副本对象。
2.  从 Controller 元数据中获取副本对象的当前状态。
3.  设置该分区给定副本的状态为 ReplicaDeletionStarted。
4.  向副本所在 Broker 发送 StopReplicaRequest 请求，停止对应副本。

## **2.7.5 目标状态变更为 ReplicaDeletionIneligible**

case ReplicaDeletionIneligible \=\>

// 遍历所有能够执行转换的副本对象

validReplicas.foreach { replica =>

// 从 Controller 元数据中获取副本对象的当前状态

val currentState \= controllerContext.replicaState(replica)

if (traceEnabled)

logSuccessfulTransition(stateLogger, replicaId, replica.topicPartition, currentState, ReplicaDeletionIneligible)

// 因为暂时无法删除，所以直接设置该分区给定副本的状态为 ReplicaDeletionIneligible

controllerContext.putReplicaState(replica, ReplicaDeletionIneligible)

}

该方法比较简单，步骤如下：

1.  遍历所有能够执行转换的副本对象。
2.  从 Controller 元数据中获取副本对象的当前状态。
3.  因为暂时无法删除，所以直接设置该分区给定副本的状态为 ReplicaDeletionIneligible。

## **2.7.6 目标状态变更为 ReplicaDeletionSuccessful**

case ReplicaDeletionSuccessful \=\>

// 遍历所有能够执行转换的副本对象

validReplicas.foreach { replica =>

// 从 Controller 元数据中获取副本对象的当前状态

val currentState \= controllerContext.replicaState(replica)

if (traceEnabled)

logSuccessfulTransition(stateLogger, replicaId, replica.topicPartition, currentState, ReplicaDeletionSuccessful)

// 因为删除成功了，所以直接设置该分区给定副本的状态为 ReplicaDeletionSuccessful

controllerContext.putReplicaState(replica, ReplicaDeletionSuccessful)

}

该方法比较简单，步骤如下：

1.  遍历所有能够执行转换的副本对象。
2.  从 Controller 元数据中获取副本对象的当前状态。
3.  因为删除成功了，所以直接设置该分区给定副本的状态为 ReplicaDeletionSuccessful。

## **2.7.7 目标状态变更为 NonExistentReplica**

case NonExistentReplica \=\>

// 遍历所有能够执行转换的副本对象

    validReplicas.foreach { replica =>

// 从 Controller 元数据中获取副本对象的当前状态

val currentState \= controllerContext.replicaState(replica)

// 从分区的完整副本分配中移除当前副本。

val newAssignedReplicas \= controllerContext

            .partitionFullReplicaAssignment(replica.topicPartition)

            .removeReplica(replica.replica)

// 更新分区的完整副本分配。

          controllerContext.updatePartitionFullReplicaAssignment(replica.topicPartition, newAssignedReplicas)

if (traceEnabled)

            logSuccessfulTransition(stateLogger, replicaId, replica.topicPartition, currentState, NonExistentReplica)

// 从副本状态机将移除该副本数据

          controllerContext.removeReplicaState(replica)

   }

该方法比较简单，步骤如下：

1.  遍历所有能够执行转换的副本对象。
2.  从 Controller 元数据中获取副本对象的当前状态。
3.  从分区的完整副本分配中移除当前副本。
4.  更新分区的完整副本分配。
5.  最后从副本状态机将移除该副本数据。

## **2.8 removeReplicaFromIsr**

// 从 ISR 副本列表中移除副本

private def removeReplicasFromIsr(

replicaId: Int,

partitions: Seq\[TopicPartition\]

): Map\[TopicPartition, LeaderIsrAndControllerEpoch\] = {

// 用于存储处理后的结果，初始值为空的Map。

var results \= Map.empty\[TopicPartition, LeaderIsrAndControllerEpoch\]

// 初始化剩余的分区列表

var remaining \= partitions

while (remaining.nonEmpty) {

// 调用 doRemoveReplicasFromIsr()方法执行副本从 ISR 列表中移除的操作，并将返回的结果进行分类。

val (finishedRemoval, removalsToRetry) = doRemoveReplicasFromIsr(replicaId, remaining)

// 更新剩余的分区列表为需要重试的分区列表

remaining = removalsToRetry

// 遍历每个已经完成移除的分区

finishedRemoval.foreach {

case (partition, Left(e)) =>

// 如果移除失败，将错误 e 记录到日志。

val replica \= PartitionAndReplica(partition, replicaId)

val currentState \= controllerContext.replicaState(replica)

logFailedStateChange(replica, currentState, OfflineReplica, e)

case (partition, Right(leaderIsrAndEpoch)) =>

// 如果移除成功，将分区和对应的 LeaderIsrAndControllerEpoch 添加到 results 中。

results += partition -> leaderIsrAndEpoch

}

}

results

}

该方法比较简单，它是在副本状态变更为 OfflineReplica 时调用的，步骤如下：

1.  初始化剩余的分区列表。
2.  遍历这些分区列表，依次调用 doRemoveReplicasFromIsr() 方法执行副本从 ISR 列表中移除的操作，并将返回的结果进行分类。
3.  如果存在重试的分区列表，则更新剩余的分区列表为需要重试的分区列表。
4.  遍历每个已经完成移除的分区。
5.  如果移除失败，将错误 e 记录到日志。
6.  如果移除成功，将分区和对应的 LeaderIsrAndControllerEpoch 添加到 results 中。
7.  最后返回结果。

## **2.9 doRemoveReplicaFromIsr**

// 从 ISR 集合中移除指定副本并更新 Zookeeper 中的 Leader 和 isr信息

private def doRemoveReplicasFromIsr(

replicaId: Int,

partitions: Seq\[TopicPartition\]

): (Map\[TopicPartition, Either\[Exception, LeaderIsrAndControllerEpoch\]\], Seq\[TopicPartition\]) = {

// 1、从 Zookeeper 中获取待更新的 TopicPartition 的 Leader 和 Isr 信息，以及不存在 Leader 和 Isr信息的 TopicPartition 集合

val (leaderAndIsrs, partitionsWithNoLeaderAndIsrInZk) = getTopicPartitionStatesFromZk(partitions)

// 2、将 Leader 和 Isr 信息中包含 replicaId 的 TopicPartition 分为包含和不包含该副本的两个集合

val (leaderAndIsrsWithReplica, leaderAndIsrsWithoutReplica) = leaderAndIsrs.partition { case (\_, result) =>

result.map { leaderAndIsr =>

leaderAndIsr.isr.contains(replicaId)

}.getOrElse(false)

}

// 3、调整 Leader 和 Isr 信息

val adjustedLeaderAndIsrs: Map\[TopicPartition, LeaderAndIsr\] = leaderAndIsrsWithReplica.flatMap {

case (partition, result) =>

result.toOption.map { leaderAndIsr =>

// 计算新 Leader，当前要离线的副本 == Leader, 那么这个新Leader == -1；否则新 Leader 还是等于原 Leader。

val newLeader \= if (replicaId == leaderAndIsr.leader) LeaderAndIsr.NoLeader else leaderAndIsr.leader

// 当 Isr 副本集合的数量只剩下 1 个的时候,那么 ISR 等于原 ISR,否则新 ISR =（原ISR - 当前的被离线的副本）

val adjustedIsr \= if (leaderAndIsr.isr.size == 1) leaderAndIsr.isr else leaderAndIsr.isr.filter(\_ != replicaId)

partition -> leaderAndIsr.newLeaderAndIsr(newLeader, adjustedIsr)

}

}

// 4、将调整后的 Leader 和 Isr 信息更新到 Zookeeper 中

val UpdateLeaderAndIsrResult(finishedPartitions, updatesToRetry) = zkClient.updateLeaderAndIsr(adjustedLeaderAndIsrs, controllerContext.epoch, controllerContext.epochZkVersion)

// 5、处理没有 Leader和 Isr 信息的 TopicPartition，如果该 TopicPartition 不在待删除队列中，则返回一个异常信息

val exceptionsForPartitionsWithNoLeaderAndIsrInZk: Map\[TopicPartition, Either\[Exception, LeaderIsrAndControllerEpoch\]\] =

partitionsWithNoLeaderAndIsrInZk.iterator.flatMap { partition =>

if (!controllerContext.isTopicQueuedUpForDeletion(partition.topic)) {

val exception \= new StateChangeFailedException(

s"Failed to change state of replica $replicaId for partition $partition since the leader and isr " +

"path in zookeeper is empty"

)

Option(partition -> Left(exception))

} else None

}.toMap

// 6、处理已完成的 TopicPartition，将其对应的 LeaderIsrAndControllerEpoch 信息更新到 Controller 元数据缓存中

val leaderIsrAndControllerEpochs: Map\[TopicPartition, Either\[Exception, LeaderIsrAndControllerEpoch\]\] =

(leaderAndIsrsWithoutReplica ++ finishedPartitions).map { case (partition, result) =>

(partition, result.map { leaderAndIsr =>

val leaderIsrAndControllerEpoch \= LeaderIsrAndControllerEpoch(leaderAndIsr, controllerContext.epoch)

controllerContext.putPartitionLeadershipInfo(partition, leaderIsrAndControllerEpoch)

leaderIsrAndControllerEpoch

})

}

// 7、返回更新后的信息

(leaderIsrAndControllerEpochs ++ exceptionsForPartitionsWithNoLeaderAndIsrInZk, updatesToRetry)

}

该方法主要是**从 ISR 集合中移除指定副本并更新 Zookeeper 中的 Leader 和 ISR 信息**，步骤如下：

1.  从 Zookeeper 中获取待更新的 TopicPartition 的 Leader 和 Isr 信息，以及不存在 Leader 和 Isr信息的 TopicPartition 集合。
2.  将 Leader 和 Isr 信息中包含 replicaId 的 TopicPartition 分为包含和不包含该副本的两个集合。
3.  调整 Leader 和 Isr 信息。
4.  计算 newLeader，当前要离线的副本 == Leader, 那么这个新Leader == -1；否则新 Leader 还是等于原 Leader。
5.  计算 adjustedIsr， 当 Isr 副本集合的数量只剩下 1 个的时候,那么 ISR 等于原 ISR,否则新 ISR =（原ISR - 当前的被离线的副本）。
6.  将调整后的 Leader 和 Isr 信息更新到 Zookeeper 中。
7.  处理没有 Leader和 Isr 信息的 TopicPartition，如果该 TopicPartition 不在待删除队列中，则返回一个异常信息。
8.  处理已完成的 TopicPartition，将其对应的 LeaderIsrAndControllerEpoch 信息更新到 Controller 元数据缓存中。
9.  返回更新后的信息。

不知你是否会有这样的疑问： **什么时候 Leader 会设置为 -1 呢？**

这里我给大家总结一下：

分区状态变更时候会触发 Leader 选举,选举成功会去 zk 修改节点 [brokers/topics/{Topic}/partitions/{分区}/state](http://brokers/topics/%7BTopic%7D/partitions/%7B%E5%88%86%E5%8C%BA%7D/state)值。而分区状态变更一般会伴随着副本状态的变更，副本状态的变更也会触发去修改 zk 节点[brokers/topics/{Topic}/partitions/{分区}/state](http://brokers/topics/%7BTopic%7D/partitions/%7B%E5%88%86%E5%8C%BA%7D/state) 值。

一般情况下如果 Leader 选举的时候选举出来了新的 LeaderAndISR，那么副本变更的时候修改 zk 的值基本不变。

但是如果 Leader 选举的时候没有选举 LeaderAndIsr，那么在副本状态变更时就会将 zk 节点的 Leader 设置为 -1。

设置的逻辑正如上面源码提过的：

1.  计算 newLeader，当前要离线的副本 == Leader, 那么这个新Leader == -1；否则新 Leader 还是等于原 Leader。
2.  计算 adjustedIsr， 当 Isr 副本集合的数量只剩下 1 个的时候,那么 ISR 等于原 ISR,否则新 ISR =（原ISR - 当前的被离线的副本）。

## **2.10 getTopicPartitionStatesFromZk()**

// 从 ZooKeeper 获取指定分区的主题和分区的状态

private def getTopicPartitionStatesFromZk(

    partitions: Seq\[TopicPartition\]

  ): (Map\[TopicPartition, Either\[Exception, LeaderAndIsr\]\], Seq\[TopicPartition\]) = {

// 尝试从 ZooKeeper 获取分区的状态数据

val getDataResponses \= try {

      zkClient.getTopicPartitionStatesRaw(partitions)

    } catch {

case e: Exception =>

// 如果发生异常，返回所有分区的异常状态，并且没有分区数据

return (partitions.iterator.map(\_ -> Left(e)).toMap, Seq.empty)

    }

// 保存没有在 ZooKeeper 中找到 Leader 和 ISR 的分区

val partitionsWithNoLeaderAndIsrInZk \= mutable.Buffer.empty\[TopicPartition\]

// 保存每个分区的状态结果

val result \= mutable.Map.empty\[TopicPartition, Either\[Exception, LeaderAndIsr\]\]

// 遍历从 ZooKeeper 中获取分区的状态数据结果

    getDataResponses.foreach\[Unit\] { getDataResponse =>

val partition \= getDataResponse.ctx.get.asInstanceOf\[TopicPartition\]

// 如果获取成功

if (getDataResponse.resultCode == Code.OK) {

// 解码获取的状态数据

        TopicPartitionStateZNode.decode(getDataResponse.data, getDataResponse.stat) match {

case None \=\>

// 如果无法解码，说明该分区在 ZooKeeper 中没有 Leader 和 ISR 数据

            partitionsWithNoLeaderAndIsrInZk += partition

case Some(leaderIsrAndControllerEpoch) =>

if (leaderIsrAndControllerEpoch.controllerEpoch > controllerContext.epoch) {

// 如果获取到的 ControllerEpoch 大于当前 ControllerEpoch，则说明该分区的领导者和 ISR 数据可能被其他控制器写入，这可能意味着当前控制器发生了软故障，另一个具有更高 ControllerEpoch 的控制器被选举为新的控制器，所以中止该控制器的状态更改

val exception \= new StateChangeFailedException(

"Leader and isr path written by another controller. This probably " +

                s"means the current controller with epoch ${controllerContext.epoch} went through a soft failure and " +

                s"another controller was elected with epoch ${leaderIsrAndControllerEpoch.controllerEpoch}. Aborting " +

"state change by this controller"

              )

              result += (partition -> Left(exception))

            } else {

// 否则，将获取到的 Leader 和 ISR 数据保存到结果中

              result += (partition -> Right(leaderIsrAndControllerEpoch.leaderAndIsr))

            }

        }

      } else if (getDataResponse.resultCode == Code.NONODE) {

// 如果结果是 NONODE，说明该分区在 ZooKeeper 中不存在

        partitionsWithNoLeaderAndIsrInZk += partition

      } else {

/ 其他情况下，将分区的状态设置为异常状态，并保存到结果中

        result += (partition -> Left(getDataResponse.resultException.get))

      }

    }

// 返回结果

    (result.toMap, partitionsWithNoLeaderAndIsrInZk)

  }

该方法主要是**从 ISR 集合中移除指定副本并更新 Zookeeper 中的 Leader 和 ISR 信息**，步骤如下：

1.  尝试从 ZooKeeper 获取分区的状态数据。如果发生异常，返回所有分区的异常状态，并且没有分区数据。
2.  遍历从 ZooKeeper 中获取分区的状态数据结果。
3.  如果获取成功，则解码获取的状态数据。
4.  如果无法解码，说明该分区在 ZooKeeper 中没有 Leader 和 ISR 数据。
5.  如果获取到的 ControllerEpoch 大于当前 ControllerEpoch，则说明该分区的领导者和 ISR 数据可能被其他控制器写入，这可能意味着当前控制器发生了软故障，另一个具有更高 ControllerEpoch 的控制器被选举为新的控制器，所以中止该控制器的状态更改。
6.  否则，将获取到的 Leader 和 ISR 数据保存到结果中。
7.  如果结果是 NONODE，说明该分区在 ZooKeeper 中不存在。
8.  其他情况下，将分区的状态设置为异常状态，并保存到结果中。
9.  返回结果。

## **03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从「**KafkaController**」以及 「**Topic 创建、删除流程**」引出 「**副本状态机 ReplicaStateMachine**」概念。

2、接着带大家深度剖析了 「**副本状态机 ReplicaStateMachine**」源码总览。ReplicaStateMachine 是 Kafka Broker 端源码中控制副本状态流转的实现类。每个 Broker 启动时都会创建 ReplicaStateMachine 实例，但只有 Controller 组件所在的 Broker 才会启动它。

3、接着带大家深度剖析了**副本状态的定义以及状态转换流程**。当前，Kafka定义了7类副本状态。同时，它还规定了每类状态合法的前置状态。

4、最后挨个剖析了 「**ZkReplicaStateMachine**」的重要方法以及内部的**状态转换源码流程**。

下篇我们来深度剖析「**分区状态机机制实现原理**」，大家期待，我们下期见。