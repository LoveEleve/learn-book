大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端 副本状态机机制实现原理**」，了解了副本状态都有哪些以及副本状态之间是如何转换的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 分区状态机机制以及 Leader 选举实现原理**」。

  
![](https://article-images.zsxq.com/FrUdJg5f5W_wT-FGtmvHdtnYHqOB)

## **01 总体概述**

在前面剖析 「**KafkaController**」以及 「**Topic 创建、删除流程**」中或多或少都提及了「**副本状态机 ReplicaStateMachine**」、「**分区状态机 PartitionStateMachine**」，所以现在你应该大概知道它们是用来管理 Kafka 中分区和副本状态转换的，那么你是否知道在「**副本**」、「**分区**」内部都有哪些状态吗？它们都是如何转换的？

带着这些问题，我们开启对这两个组件的源码剖析，今天先来看下「**分区状态机 PartitionStateMachine**」的实现机制。

##   
**02 PartitionStateMachine**

![](https://article-images.zsxq.com/Frh0eg-_aQT2ZQg-bnyTH50wesKb)

从图中可以看出，这两个状态机组件都是在 Controller 包下。

先来看下分区状态机的源码结构总览。

## **2.1 源码结构总览**

「**PartitionStateMachine****.scala**」源码在 Kafka 源码包的 core 包下，具体的 github 源码位置如下：

[https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/controller/PartitionStateMachine.scala](https://github.com/apache/kafka/blob/2.8.0/core/src/main/scala/kafka/controller/PartitionStateMachine.scala)

![](https://article-images.zsxq.com/FvZwwqxfLaUp5dy8YD5p7h1Fri2z)

如上图其源码主要可以分为 5 部分，如下：

1.  **PartitionStateMachine**：它是分区状态机抽象类。定义了 startup、shutdown 、initializePartitionState 等这样的公共方法，同时也给出了处理分区状态转换入口方法 handleStateChanges。
2.  **ZkPartitionStateMachine**：它是分区状态机具体实现类，同时也是 PartitionStateMachine 唯一的子类。它实现了分区状态机的主体逻辑功能。ZkPartitionStateMachine 重写了父类的 handleStateChanges 方法，并配以私有的doHandleStateChanges 方法，共同实现分区状态转换的操作。
3.  **PartitionState**：分区状态集合，在 Kafka 目前共定义了 4 种分区状态，分别是「**NewPartition**」、「**OnlinePartition**」、「**OfflinePartition**」、「**NonExistentPartition**」。
4.  **PartitionLeaderElectionStrategy 接口及其实现对象**：定义 4 类分区 Leader 选举策略，它们是发生 Leader 选举的 4 种场景。
5.  **PartitionLeaderElectionAlgorithms**：分区 Leader 选举的算法实现。

## **2.2 PartitionStateMachine 抽象类**

先来看下抽象类，都比较简单，源码如下：

// 一个抽象类，用于管理分区的状态机

abstract class PartitionStateMachine(controllerContext: ControllerContext) extends Logging {

/\*\*

\* Invoked on successful controller election.

\* 它也是在成功选举为控制器后调用

\*/

def startup(): Unit = {

info("Initializing partition state")

// 初始化分区的状态

initializePartitionState()

info("Triggering online partition state changes")

// 触发在线分区状态的改变

triggerOnlinePartitionStateChange()

debug(s"Started partition state machine with initial state -> ${controllerContext.partitionStates}")

}

/\*\*

\* Invoked on controller shutdown.

\* 在控制器关闭时调用

\*/

def shutdown(): Unit = {

info("Stopped partition state machine")

}

/\*\*

\* This API invokes the OnlinePartition state change on all partitions in either the NewPartition or OfflinePartition

\* state. This is called on a successful controller election and on broker changes

\* 在成功 controller 选举和 broker 变更时调用

\*/

def triggerOnlinePartitionStateChange(): Unit = {

// 从 Controller 元数据缓存中获取处于 NewPartition 或 OfflinePartition 状态的分区集合

val partitions \= controllerContext.partitionsInStates(Set(OfflinePartition, NewPartition))

// 对处于 NewPartition 或 OfflinePartition 状态的分区触发 OnlinePartition 状态的改变。

triggerOnlineStateChangeForPartitions(partitions)

}

def triggerOnlinePartitionStateChange(topic: String): Unit = {

// 从 Controller 元数据缓存中获取指定主题中处于 NewPartition 或 OfflinePartition 状态的分区集合

val partitions \= controllerContext.partitionsInStates(topic, Set(OfflinePartition, NewPartition))

// 对指定主题中处于 NewPartition 或 OfflinePartition 状态的分区触发 OnlinePartition 状态的改变。

triggerOnlineStateChangeForPartitions(partitions)

}

private def triggerOnlineStateChangeForPartitions(partitions: collection.Set\[TopicPartition\]): Unit = {

// try to move all partitions in NewPartition or OfflinePartition state to OnlinePartition state except partitions

// that belong to topics to be deleted

// 尝试将处于 NewPartition 或 OfflinePartition 状态的分区移动到 OnlinePartition 状态，除了属于要删除的主题的分区。

val partitionsToTrigger \= partitions.filter { partition =>

!controllerContext.isTopicQueuedUpForDeletion(partition.topic)

}.toSeq

handleStateChanges(partitionsToTrigger, OnlinePartition, Some(OfflinePartitionLeaderElectionStrategy(false)))

// TODO: If handleStateChanges catches an exception, it is not enough to bail out and log an error.

// It is important to trigger leader election for those partitions.

}

/\*\*

\* Invoked on startup of the partition's state machine to set the initial state for all existing partitions in zookeeper

\* 在分区状态机启动时调用，为所有现有的分区设置初始状态。

\*/

private def initializePartitionState(): Unit = {

// 遍历所有主题下的分区数据

for (topicPartition <- controllerContext.allPartitions) {

// check if leader and isr path exists for partition. If not, then it is in NEW state

// 检查分区的 leader 和 isr 路径是否存在。如果存在，则分区处于 Online 状态

controllerContext.partitionLeadershipInfo(topicPartition) match {

case Some(currentLeaderIsrAndEpoch) =>

// else, check if the leader for partition is alive. If yes, it is in Online state, else it is in Offline state

// 检查分区的 Leader 副本是否在线，如果在线则分区处于 Online 状态

if (controllerContext.isReplicaOnline(currentLeaderIsrAndEpoch.leaderAndIsr.leader, topicPartition))

// leader is alive

controllerContext.putPartitionState(topicPartition, OnlinePartition)

else

// 否则处于 Offline 状态。

controllerContext.putPartitionState(topicPartition, OfflinePartition)

case None \=\>

// 如果在 zookeeper 中找不到分区的信息，则分区处于 NewPartition 状态。

controllerContext.putPartitionState(topicPartition, NewPartition)

}

}

}

def handleStateChanges(

partitions: Seq\[TopicPartition\],

targetState: PartitionState

): Map\[TopicPartition, Either\[Throwable, LeaderAndIsr\]\] = {

// 处理分区状态的改变，将指定的分区改变为目标状态。

handleStateChanges(partitions, targetState, None)

}

// 处理分区状态的改变，并根据指定的策略进行重新选举 leader。

def handleStateChanges(

partitions: Seq\[TopicPartition\],

targetState: PartitionState,

leaderElectionStrategy: Option\[PartitionLeaderElectionStrategy\]

): Map\[TopicPartition, Either\[Throwable, LeaderAndIsr\]\]

}

可以看到 PartitionStateMachine 类同样也只需要接收一个 ControllerContext 对象。在前面讲解过 ControllerContext 封装了 Controller 端保存的所有集群元数据信息。

它定义了在 「**Controller**」选举成功时调用的 startup()，当「**Controller**」关闭后调用的 shutdown()，以及在启动分区状态机时调用的初始化方法等。

接下来我们来看下其子类，也就是其实现类，在目前版本中，ZkPartitionStateMachine 是唯一的 PartitionStateMachine 子类。

## **2.3 ZkPartitionStateMachine 实现类之初始化**

class ZkPartitionStateMachine(config: KafkaConfig,

stateChangeLogger: StateChangeLogger,

controllerContext: ControllerContext,

zkClient: KafkaZkClient,

controllerBrokerRequestBatch: ControllerBrokerRequestBatch)

extends PartitionStateMachine(controllerContext) {

....

}

ZkPartitionStateMachine 类的属性相对多一些。如果你想要构造一个 **ZkPartitionStateMachine** 实例，大致需要以下几个：

1.  ControllerContext 实例：封装了 Controller 端保存的所有集群元数据信息。
2.  KafkaZkClient对象实例：负责与 ZooKeeper 进行交互。
3.  ControllerBrokerRequestBatch 实例：用于给集群 Broker 发送控制类请求，即 LeaderAndIsrRequest、StopReplicaRequest 和 UpdateMetadataRequest。

接着我们来看下「**分区状态机**」是在何时进行初始化的。

不知道在前面讲解 「**KafkaController**」时注意到，在其对象构建时，会创建一个 **ZkPartitionStateMachine** 实例，源码如下：

![](https://article-images.zsxq.com/FhlwMDwGgqsT2YlXGSQJsLd1aPnQ)

看到这里你是否会有疑惑：「**如果当一个 Broker 还未被选举成为 Controller 时，它也会构建 kafkaController 对象以及初始化 ZkPartitionStateMachine 实例吗？**」

答案：是的，在所有 Broker 启动时，都会创建 KafkaController 实例以及初始化 ZKPartitionStateMachine 实例的，但是**这不代表每个 Broker 都会启动分区状态机**。为什么这么说呢？

实际上，只有被选举成为 Controller 所在的 Broker 上， 「**分区状态机**」才会被启动，而具体的启动代码位于KafkaController#onControllerFailover 方法，源码如下：

private def onControllerFailover(): Unit = {

......

replicaStateMachine.startup() // 启动副本状态机

partitionStateMachine.startup() // 启动分区状态机

......

}

当 Broker 被成功选举成为 Controller 后，onControllerFailover 方法会被调用，进而启动该 Broker 早已创建好的「**副本状态机**」和「**分区状态机**」。

> 这里需要注意一点：如果 Controller 易主到其他 Broker，旧 Controller 所在的 Broker 要调用这些状态机的shutdown 方法关闭它们，新 Controller 所在的 Broker 调用状态机的 startup 方法来启动它们。

了解这些后，接着我们来聊聊 「**分区状态**」以及 「**状态转换流程**」。

##   
**2.4 分区状态以及状态转换流程**

当「**分区状态机**」启动后，它就要行使「**处理分区状态的转换**」权力了。

ZkPartitionStateMachine 是管理分区状态转换的，不过，在学习如何处理状态转换之前，我们必须要弄明白一个问题就是：**当前都有哪些状态，它们都代表什么?** 和 ReplicaState 类似，PartitionState 定义了分区的状态以及流转规则。源码中的 PartitionState 定义了 4 种副本状态，如下：

![](https://article-images.zsxq.com/FviTBigWJzG0DnP_Z8D0f2bwRE0G)

1.  NewPartition：分区被刚创建后状态，表示它是一个全新的分区对象。处于这个状态的分区不能选举 Leader。
2.  OnlinePartition：分区正常提供服务时状态。
3.  OfflinePartition：分区下线时状态。
4.  NonExistentPartition：分区从分区状态机移除后所处的状态。

先来看下 「**PartitionState**」接口的定义：

// ReplicaState接口

sealed trait PartitionState {

// 定义状态序号

def state: Byte

// 定义合法的前置状态

def validPreviousStates: Set\[PartitionState\]

}

这 4 种分区状态都继承了该接口，并实现了对象定义了「**该分区状态的序号**」、「**该分区状态的合法前置转换状态**」。

case object NewPartition extends PartitionState {

val state: Byte = 0

val validPreviousStates: Set\[PartitionState\] = Set(NonExistentPartition)

}

case object OnlinePartition extends PartitionState {

val state: Byte = 1

val validPreviousStates: Set\[PartitionState\] = Set(NewPartition, OnlinePartition, OfflinePartition)

}

case object OfflinePartition extends PartitionState {

val state: Byte = 2

val validPreviousStates: Set\[PartitionState\] = Set(NewPartition, OnlinePartition, OfflinePartition)

}

case object NonExistentPartition extends PartitionState {

val state: Byte = 3

val validPreviousStates: Set\[PartitionState\] = Set(OfflinePartition)

}

从上面源码中可以看到，validPreviousStates 属性是一个集合类型，里面包含了每个分区状态的「**合法前置转换状态**」，它是需要你重点了解和掌握的，对于集合不存在的分区状态来转换到该分区状态时，它就是「**非法的状态转换**」。

  
对于OnlinePartition而言，它的合法前置状态集包括NewPartition、OnlinePartition和OfflinePartition。在Kafka中，从合法状态集以外的状态向目标状态进行转换，将被视为非法操作。

这里拿「**OfflinePartition**」为例，可以看到里面包含了 NewPartition、OnlinePartition、OfflinePartition 这 3 种状态，换句话说，**Kafka 只允许分区从刚刚这 3 种状态转换到** 「**OfflinePartition**」**状态**，**如果想从 NonExistentPartition 状态转换到** 「**OfflinePartition**」**状态时，就是非法的状态转换**。

其他状态也一样，这里我就不一一介绍了，你可以对照着上面源码自己研究下。

这里我通过一张图来帮你总结下完整的状态转换，如下：

  
![](https://article-images.zsxq.com/FopoJyfIvnR-rbXzo1QRRTWksPFx)

根据上面状态转换图，这里再给大家梳理下各个状态的流转过程：

1.  当分区首次被创建后，它会被置于 「**NewPartition**」状态。当 Broker 启动或者分区初始化且能够对外提供服务之后，状态机会将其变更为 「**OnlinePartition**」并一直以该状态持续工作。
2.  当分区所在的 Broker 关闭下线或者主题被删除了，分区就需要从「**OnlinePartition**」变更为「**OfflinePartition**」，表示分区已处于离线状态。
3.  当分区选举 Leader 成功后，该分区的状态将从「**OfflinePartition**」变更为「**OnlinePartition**」 ，表示分区上线。
4.  当主题被成功删除后，其状态会变更为「**NonExistentPartition**」 状态，即从分区状态机将移除该分区数据。

以上就是一个基本的状态管理流程。

接下来，我们来看下分区选举 Leader 的哪些事情，接着在剖析具体实现类里面是如何实现这些状态流转的。

## **2.5 分区选举 Leader 场景及算法实现**

对于分区来说，每个分区都必须选举出 Leader 才能正常对外提供服务，所以对于分区来说，**Leader 副本时非常重要的角色**。

既然这么重要，我们就必须要**了解 Leader 选举策略流程是怎样的，又是如何选举的以及源码是如何实现的呢？**带着这些问题，我们重点来剖析下选举策略以及具体的实现。

## **2.5.1 分区选举 Leader 接口**

既然要进行分区 Leader 的选举，肯定是需要策略的，那么 Kafka 内部定义了哪几种选举策略呢？或者说会在什么请情况下去执行 Leader 选举呢？

其实这是 「**PartitionLeaderElectionStrategy**」接口要实现的事情，源码如下：

// 分区 Leader 选举策略接口

sealed trait PartitionLeaderElectionStrategy

// 离线分区 Leader 选举策略

final case class OfflinePartitionLeaderElectionStrategy(allowUnclean: Boolean) extends PartitionLeaderElectionStrategy

// 分区副本重分配 Leader 选举策略

final case object ReassignPartitionLeaderElectionStrategy extends PartitionLeaderElectionStrategy

// 分区 Preferred 副本 Leader 选举策略

final case object PreferredReplicaPartitionLeaderElectionStrategy extends PartitionLeaderElectionStrategy

// Broker Controlled 关闭时 Leader 选举策略

final case object ControlledShutdownPartitionLeaderElectionStrategy extends PartitionLeaderElectionStrategy

当前，Kafka 给分区 Leader 选举策略有以下 4 类场景：

1.  OfflinePartitionLeaderElectionStrategy： Leader 副本下线而引发的分区 Leader 选举。
2.  ReassignPartitionLeaderElectionStrategy：执行分区副本重分配操作而引发的分区 Leader 选举。
3.  PreferredReplicaPartitionLeaderElectionStrategy：执行 Preferred 副本 Leader 选举而引发的分区 Leader 选举。
4.  ControlledShutdownPartitionLeaderElectionStrategy：正常关闭 Broker 而引发的分区 Leader 选举。

## **2.5.2 分区选举 Leader 策略实现**

针对上面剖析的这 4 类场景，分区状态机为此定义了 4 个对应方法，分别负责为每种场景下选举 Leader 副本如下：

![](https://article-images.zsxq.com/FtxFvtCEVHkf8PVqXw1VGSyxV5x5)

接下来，我们挨个剖析下，先来看最复杂的 offlinePartitionLeaderElection 方法。

##   
**offlinePartitionLeaderElection()**

它是 Leader 副本下线而引发的分区 Leader 选举策略算法，源码如下：

def offlinePartitionLeaderElection(assignment: Seq\[Int\], isr: Seq\[Int\], liveReplicas: Set\[Int\], uncleanLeaderElectionEnabled: Boolean, controllerContext: ControllerContext): Option\[Int\] = {

// 从当前分区副本列表中查找首个处于存活状态的 ISR 副本

assignment.find(id => liveReplicas.contains(id) && isr.contains(id)).orElse {

// 如果找不到满足条件的副本，查看是否允许 Unclean Leader 选举，即 Broker 端参数unclean.leader.election.enable 是否等于 true

if (uncleanLeaderElectionEnabled) {

// 策略很简单，选择当前 ISR 副本列表中的第一个存活副本作为 Leader

val leaderOpt \= assignment.find(liveReplicas.contains)

if (leaderOpt.isDefined)

controllerContext.stats.uncleanLeaderElectionRate.mark()

leaderOpt

} else {

// 如果不允许 Unclean Leader 选举，则返回 None 表示无法选举 Leader

None

}

}

}

我们看到该方法总共接收 5 个参数，这里重点介绍下：

1.  **assignment**：该参数是分区的副本集合列表。它有个专属的名称，叫 Assigned Replicas，简称 AR。当我们创建主题之后，使用 kafka-topics 脚本查看主题时，就可以看到名为 Replicas 这一列数据。它显示的是主题下每个分区的 AR 信息。assignments 参数类型是 Seq\[Int\]。**这充分说明了 AR 是有顺序的，而且不一定和 ISR 副本列表的顺序是相同的**。
2.  **isr**: 它也有个专属的名称，叫  In-Sync Replicas，简称 ISR。内部保存了分区所有与Leader 副本保持同步的副本列表。当然 Leader 副本也在 ISR 副本集合中。另外它的参数类型也是 Seq\[Int\]，**所以 ISR 自身也是有顺序的**。![](https://article-images.zsxq.com/FvqoqzCNbY5YgGCcFlUYGOfG-nL_)
3.  **liveReplicas**: 它内部保存了该分区下所有处于存活状态的副本。那么**怎么判断副本是否存活呢？答案就是根据 Controller 元数据缓存中的数据来判断**。你可以认为所有在运行中的 Broker 上的副本都是存活的。
4.  **uncleanLeaderElectionEnabled**: 在默认配置下，只要不是由 AdminClient 发起的 Leader 选举，这个参数的值一般是 false，即 Kafka 不允许执行 Unclean Leader 选举。所谓的 Unclean Leader 选举（脏选举），是指在 ISR 列表为空的情况下，Kafka 选择一个非 ISR 副本作为新的 Leader。由于存在丢失数据的风险，所以Kafka 已经把Broker端参数 [unclean.leader.election.enable](http://unclean.leader.election.enable/) 的默认值设置为 false 的方式，禁止 Unclean Leader 选举了。

在 Kafka 2.4.0 版本社区正式支持在 AdminClient 端为给定分区来选举 Leader，如果 Leader 选举是由AdminClient 端触发的，那就默认开启 Unclean Leader 选举，即 [](http://%20unclean.leader.election.enable%20=%20true/)[unclean.leader.election.enable](http://%20unclean.leader.election.enable%20=%20true/) [= true](http://%20unclean.leader.election.enable%20=%20true/)。

所以在该方法中，你可以认为 [uncleanLeaderElectionEnabled = false](http://uncleanleaderelectionenabled%20=%20false/)。

下面通过一张图来梳理其流程：

![](https://article-images.zsxq.com/Fs-75eDjoqzmXhvatSwdPRVrOrw1)

步骤如下：

1.  首先会顺序查找 AR 列表，并把第一个同时满足以下两个条件的副本作为新的 Leader 并返回：
2.  该副本是存活状态，即副本所在的 Broker 依然在运行中。
3.  该副本在 ISR 副本集合列表中。
4.  如果无法找到这样的副本，会再检查是否开启了 Unclean Leader 选举
5.  如果开启了，则只要满足上面第一个条件即可，即选择当前 ISR 副本列表中的第一个存活副本作为新 Leader。
6.  如果未开启，则本次 Leader 选举失败，没有新 Leader 被选出。

## **reassignPartitionLeaderElection()**

它是执行分区副本重分配操作而引发的分区 Leader 选举策略算法，源码如下：

def reassignPartitionLeaderElection(reassignment: Seq\[Int\], isr: Seq\[Int\], liveReplicas: Set\[Int\]): Option\[Int\] = {

// 在重新分配的分区列表中查找首个处于存活状态的 ISR 副本

reassignment.find(id => liveReplicas.contains(id) && isr.contains(id))

}

## **preferredReplicaPartitionLeaderElection()**

它是执行 Preferred 副本 Leader 选举而引发的分区 Leader 选举策略算法，源码如下：

def preferredReplicaPartitionLeaderElection(assignment: Seq\[Int\], isr: Seq\[Int\], liveReplicas: Set\[Int\]): Option\[Int\] = {

// 在分区分配列表中查找首个处于存活状态的 ISR 副本

assignment.headOption.filter(id => liveReplicas.contains(id) && isr.contains(id))

}

## **controlledShutdownPartitionLeaderElection()**

它是正常关闭 Broker 而引发的分区 Leader 选举策略算法，源码如下：

def controlledShutdownPartitionLeaderElection(assignment: Seq\[Int\], isr: Seq\[Int\], liveReplicas: Set\[Int\], shuttingDownBrokers: Set\[Int\]): Option\[Int\] = {

// 在分区分配列表中查找首个处于存活状态的 ISR 副本且不在关闭中的副本

assignment.find(id => liveReplicas.contains(id) && isr.contains(id) && !shuttingDownBrokers.contains(id))

}

可以看到，它们的逻辑几乎是相同的，大概的原理都是从 AR，或给定的副本列表中寻找存活状态的 ISR 副本。

所以到这里，你应该已经了解了**怎 Kafka 选举分区 Leader 的策略就是：找出 AR 副本列表（或给定副本列表）中首个处于存活状态，且在 ISR 副本列表的副本，并将其作为新的 Leader**。

## **2.6 ZkPartitionStateMachine 实现类方法介绍**

该类总共定义了 10 个方法，其中 1 个为 public ，剩下 9 个都是 private，这个 public 方法是分区状态机最重要的逻辑 [handleStateChanges](http://handlestatechanges%20/) 方法。而那 9 个方法全部都是用来辅助 public 方法的。

这里先简单的介绍下其他 9 个辅助方法都是做什么的，只有清楚了这些方法的用途，你才能更好地理解[handleStateChanges](http://handlestatechanges/) 的实现逻辑，根据源码从上到下介绍。

1.  partitionState：它是从 Controller 元数据缓存中获取分区状态的方法。
2.  doHandleStateChanges：它是执行分区状态变更和转换操作的具体方法。接下来会详细剖析其源码。
3.  initializeLeaderAndIsrForPartitions：它是初始化分区 Leader 和 ISR 副本列表到 ZK 中的方法。
4.  electLeaderForPartitions：它是为分区选举 Leader 副本的方法。
5.  doElectLeaderForPartitions：它是批量为分区选举 Leader 副本的方法。
6.  collectUncleanLeaderElectionState：它是搜集脏选举的状态的方法。
7.  logInvalidTransition：记录错误之用，其记录一次非法的状态转换。
8.  logFailedStateChange：它直接调用另外一个logFailedStateChange来记录异常。
9.  logFailedStateChange：它是直接抛异常错误的方法。

接下来我们分别来看下其重要方法的具体实现。

## **2.7 handleStateChanges()**

override def handleStateChanges(

partitions: Seq\[TopicPartition\],

targetState: PartitionState,

partitionLeaderElectionStrategyOpt: Option\[PartitionLeaderElectionStrategy\]

): Map\[TopicPartition, Either\[Throwable, LeaderAndIsr\]\] = {

if (partitions.nonEmpty) {

try {

// 清空 Controller 待发送请求集合，准备本次请求发送

controllerBrokerRequestBatch.newBatch()

// 调用 doHandleStateChanges 方法执行真正的状态变更逻辑

val result \= doHandleStateChanges(

partitions,

targetState,

partitionLeaderElectionStrategyOpt

)

// Controller 给相关 Broker 发送请求通知状态变化

controllerBrokerRequestBatch.sendRequestsToBrokers(controllerContext.epoch)

// 返回状态变更处理结果

result

} catch {

// 如果 Controller 已经易主，则记录错误日志，然后重新抛出异常

// 上层代码会捕获该异常并执行maybeResign方法执行卸任逻辑

case e: ControllerMovedException =>

error(s"Controller moved to another broker when moving some partitions to $targetState state", e)

throw e

case e: Throwable =>

// 如果是其他异常，记录错误日志，封装错误返回

error(s"Error while moving some partitions to $targetState state", e)

partitions.iterator.map(\_ -> Left(e)).toMap

}

} else {

// 如果 partitions 为空，直接返回空

Map.empty

}

}

该方法的主要作用是**处理状态变更的，是对外提供状态转换操作的入口**。

其接收两个参数：

1.  partition：是待执行状态变更的目标分区列表。
2.  targetState：是对这些分区对象要转换成的目标状态。
3.  leaderElectionStrategy：它是一个可选项，如果传入了，就表示要执行 Leader 选举。

整体逻辑可以分为两步：

1.  调用 doHandleStateChanges 方法执行分区状态转换。
2.  Controller给相关Broker发送请求，告知它们这些分区的状态变更。至于哪些Broker属于相关Broker，以及给Broker发送哪些请求，在上一步已经确定了。

接下来，重点剖析 [doHandleStateChanges](http://dohandlestatechanges/) 方法。

## **2.8 doHandleStateChanges()**

这里先从整体来看下该方法的源码，它主要是用来执行具体状态转换操作的，如下：

private def doHandleStateChanges(

partitions: Seq\[TopicPartition\],

targetState: PartitionState,

partitionLeaderElectionStrategyOpt: Option\[PartitionLeaderElectionStrategy\]

): Map\[TopicPartition, Either\[Throwable, LeaderAndIsr\]\] = {

val stateChangeLog \= stateChangeLogger.withControllerEpoch(controllerContext.epoch)

val traceEnabled \= stateChangeLog.isTraceEnabled

// 初始化新分区的状态为 NonExistentPartition

partitions.foreach(partition => controllerContext.putPartitionStateIfNotExists(partition, NonExistentPartition))

// 找出要执行非法状态转换的分区，记录错误日志

val (validPartitions, invalidPartitions) = controllerContext.checkValidPartitionStateChange(partitions, targetState)

invalidPartitions.foreach(partition => logInvalidTransition(partition, targetState))

// 根据 targetState 进入到不同的 case 分支

targetState match {

case NewPartition \=\>

....

case OnlinePartition \=\>

....

case OfflinePartition | NonExistentPartition =>

....

}

}

步骤如下：

1.  首先会做状态初始化的工作，具体来说就是，不在元数据缓存中的所有分区的状态，会被初始化为NonExistentPartition。
2.  找出要执行非法状态转换的分区，记录错误日志。
3.  最后根据 targetState 进入到不同的 case 分支。

接下来，我们挨个剖析了每个分支的源码实现。

##   
**2.8.1 目标状态变更为 New****Partition**

case NewPartition \=\>

// 遍历所有能够执行转换的分区对象

validPartitions.foreach { partition =>

stateChangeLog.info(s"Changed partition $partition state from ${partitionState(partition)} to $targetState with " +

s"assigned replicas ${controllerContext.partitionReplicaAssignment(partition).mkString(",")}")

// 将该分区对象设置成 NewPartition 状态

controllerContext.putPartitionState(partition, NewPartition)

}

Map.empty

## **2.8.2 目标状态变更为 OfflinePartition | NonExistentPartition**

case OfflinePartition | NonExistentPartition =>

// 遍历所有能够执行转换的分区对象

validPartitions.foreach { partition =>

if (traceEnabled)

stateChangeLog.trace(s"Changed partition $partition state from ${partitionState(partition)} to $targetState")

// 将该分区对象设置成目标状态

controllerContext.putPartitionState(partition, targetState)

}

Map.empty

## **2.8.3 目标状态变更为 OnlinePartition**

case OnlinePartition \=\>

// 获取未初始化分区列表，也就是NewPartition状态下的所有分区

val uninitializedPartitions \= validPartitions.filter(partition => partitionState(partition) == NewPartition) // 获取具备 Leader 选举资格的分区列表，只能为 OnlinePartition 和 OfflinePartition 状态的分区选举 Leader

val partitionsToElectLeader \= validPartitions.filter(partition => partitionState(partition) == OfflinePartition || partitionState(partition) == OnlinePartition)

// 初始化 NewPartition 状态分区，在 ZK 中写入 Leader 和 ISR 数据

if (uninitializedPartitions.nonEmpty) {

val successfulInitializations \= initializeLeaderAndIsrForPartitions(uninitializedPartitions)

successfulInitializations.foreach { partition =>

stateChangeLog.info(s"Changed partition $partition from ${partitionState(partition)} to $targetState with state " + s"${controllerContext.partitionLeadershipInfo(partition).get.leaderAndIsr}")

// 循环将该分区对象设置成 OnlinePartition 状态

controllerContext.putPartitionState(partition, OnlinePartition)

}

}

// 为具备 Leader 选举资格的分区选举 Leader

if (partitionsToElectLeader.nonEmpty) {

// 调用 electLeaderForPartitions 进行选举并返回选举结果

val electionResults \= electLeaderForPartitions(

partitionsToElectLeader,

partitionLeaderElectionStrategyOpt.getOrElse(

throw new IllegalArgumentException("Election strategy is a required field when the target state is OnlinePartition")

)

)

// 如果选举结果不为空，则遍历结果

electionResults.foreach {

case (partition, Right(leaderAndIsr)) =>

stateChangeLog.info(

s"Changed partition $partition from ${partitionState(partition)} to $targetState with state $leaderAndIsr"

)

// 将成功选举为 Leader 后的分区设置成 OnlinePartition 状态

controllerContext.putPartitionState(partition, OnlinePartition)

case (\_, Left(\_)) => // Ignore; no need to update partition state on election error

// 如果选举失败，忽略

}

// 返回 Leader 选举结果

electionResults

} else {

Map.empty

}

步骤如下：

1.  获取未初始化分区列表，也就是NewPartition状态下的所有分区。
2.  获取具备 Leader 选举资格的分区列表，只能为 OnlinePartition 和 OfflinePartition 状态的分区选举 Leader 。
3.  初始化 NewPartition 状态分区，在 ZK 中写入 Leader 和 ISR 数据。
4.  循环将该分区对象设置成 OnlinePartition 状态。
5.  调用 electLeaderForPartitions 为具备 Leader 选举资格的分区进行选举 Leader并返回选举结果。
6.  如果选举结果不为空，则遍历结果将成功选举为 Leader 后的分区设置成 OnlinePartition 状态。
7.  返回 Leader 选举结果。

接下来重点剖析下「**第二步**」 和 「**第五步**」 。

## **2.9 initializeLeaderAndIsrForPartitions()**

private def initializeLeaderAndIsrForPartitions(partitions: Seq\[TopicPartition\]): Seq\[TopicPartition\] = {

val successfulInitializations \= mutable.Buffer.empty\[TopicPartition\]

// 获取每个分区的副本列表

val replicasPerPartition \= partitions.map(partition => partition -> controllerContext.partitionReplicaAssignment(partition))

// 获取每个分区的所有存活副本

val liveReplicasPerPartition \= replicasPerPartition.map { case (partition, replicas) =>

val liveReplicasForPartition \= replicas.filter(replica => controllerContext.isReplicaOnline(replica, partition))

partition -> liveReplicasForPartition

}

// 按照有无存活副本对分区进行分组。

// 分为两组：有存活副本的分区、无任何存活副本的分区

val (partitionsWithoutLiveReplicas, partitionsWithLiveReplicas) = liveReplicasPerPartition.partition { case (\_, liveReplicas) => liveReplicas.isEmpty }

....

// 为"有存活副本的分区" 选择 Leader 和 ISR

val leaderIsrAndControllerEpochs \= partitionsWithLiveReplicas.map { case (partition, liveReplicas) =>

// Leader 选择依据：存活副本列表的首个副本被认定为Leader

val leaderAndIsr \= LeaderAndIsr(liveReplicas.head, liveReplicas.toList)

// ISR 选择依据：存活副本列表被认定为ISR

val leaderIsrAndControllerEpoch \= LeaderIsrAndControllerEpoch(leaderAndIsr, controllerContext.epoch)

partition -> leaderIsrAndControllerEpoch

}.toMap

val createResponses \= try {

// 将 leader 和 ISR 持久化到 Zookeeper

zkClient.createTopicPartitionStatesRaw(leaderIsrAndControllerEpochs, controllerContext.epochZkVersion)

} catch {

case e: ControllerMovedException =>

error("Controller moved to another broker when trying to create the topic partition state znode", e)

throw e

case e: Exception =>

// 如果持久化过程中出现异常，打印日志，并将有在线副本的分区添加到失败集合

partitionsWithLiveReplicas.foreach { case (partition, \_) => logFailedStateChange(partition, partitionState(partition), NewPartition, e) }

Seq.empty

}

// 处理持久化的结果，并添加请求到 batch 中

createResponses.foreach { createResponse =>

val code \= createResponse.resultCode

val partition \= createResponse.ctx.get.asInstanceOf\[TopicPartition\]

val leaderIsrAndControllerEpoch \= leaderIsrAndControllerEpochs(partition)

if (code == Code.OK) {

controllerContext.putPartitionLeadershipInfo(partition, leaderIsrAndControllerEpoch)

controllerBrokerRequestBatch.addLeaderAndIsrRequestForBrokers(leaderIsrAndControllerEpoch.leaderAndIsr.isr,

partition, leaderIsrAndControllerEpoch, controllerContext.partitionFullReplicaAssignment(partition), isNew = true)

successfulInitializations += partition

} else {

logFailedStateChange(partition, NewPartition, OnlinePartition, code)

}

}

// 返回初始化成功的分区集合

successfulInitializations

}

简单来说，它的主要功能就是在 ZooKeeper 中，创建并写入分区节点数据。节点的位置是 [/brokers/topics/<topic>/partitions/<partition>](http://brokers/topics/topic/partitions/partition)，每个节点都要包含分区的 Leader 和 ISR 等数据。而 **第Leader 和 ISR 的选择规则是：选择存活副本列表的第一个副本为 Leader 副本。选择存活副本列表作为 ISR 副本列表**。

接着我们来剖析最重要的分区选举 Leader 的源码实现逻辑。

## **2.10 electLeaderForParititions()**

在上面「**第五步**」中会为具备 Leader 选举资格的分区选举 Leader，该方法会不断尝试为多个分区选举 Leader，直到所有分区都成功选出 Leader，源码如下：

private def electLeaderForPartitions(

partitions: Seq\[TopicPartition\],

partitionLeaderElectionStrategy: PartitionLeaderElectionStrategy

): Map\[TopicPartition, Either\[Throwable, LeaderAndIsr\]\] = {

// 初始化剩余的未选举成功的分区列表

var remaining \= partitions

val finishedElections \= mutable.Map.empty\[TopicPartition, Either\[Throwable, LeaderAndIsr\]\]

while (remaining.nonEmpty) {

// 调用 doElectLeaderForPartitions()方法尝试为多个分区选举 Leader，并将返回的结果进行分类。

val (finished, updatesToRetry) = doElectLeaderForPartitions(remaining, partitionLeaderElectionStrategy)

// 更新剩余的分区列表为需要重试选举的分区列表

remaining = updatesToRetry

// 遍历每个已经完成选举的分区

finished.foreach {

// 如果选举失败，将错误 e 记录到日志。

case (partition, Left(e)) =>

logFailedStateChange(partition, partitionState(partition), OnlinePartition, e)

case (\_, Right(\_)) => // Ignore; success so no need to log failed state change

}

// 累计成功选举的分区

finishedElections ++= finished

if (remaining.nonEmpty)

logger.info(s"Retrying leader election with strategy $partitionLeaderElectionStrategy for partitions $remaining")

}

// 返回选举结果

finishedElections.toMap

}

该方法比较简单，它是在分区状态变更为 OnlinePartition 时调用的，步骤如下：

1.  初始化剩余的未选举成功的分区列表。
2.  遍历这些分区列表，依次调用doElectLeaderForPartitions()方法尝试为多个分区选举 Leader，并将返回的结果进行分类。
3.  更新剩余的分区列表为需要重试选举的分区列表。
4.  遍历每个已经完成选举的分区。
5.  如果选举失败，将错误 e 记录到日志。
6.  如果选举成功，则忽略。
7.  累计成功选举的分区。
8.  返回选举结果。

选举 Leader 的核心位于 doElectLeaderForPartitions 方法中，接下来重点剖析。

## **2.11 doElectLeaderForParititions()**

该方法比较长，接下来我会通过三步来拆解剖析，这里先通过一张图来梳理下其流程。

![](https://article-images.zsxq.com/lkyxanDEleXzQGUAbuuNr7q-NwK7)

### **2.11.1 从 ZK 中获取给定分区的 Leader 和 ISR 信息，将结果追加到 validLeaderAndIsrs 中**

val getDataResponses \= try {

// 批量获取 ZooKeeper 中给定分区的 znode 节点数据

zkClient.getTopicPartitionStatesRaw(partitions)

} catch {

case e: Exception =>

return (partitions.iterator.map(\_ -> Left(e)).toMap, Seq.empty)

}

// 构建两个容器，分别保存选举失败分区列表和可选举 Leader 分区列表

val failedElections \= mutable.Map.empty\[TopicPartition, Either\[Exception, LeaderAndIsr\]\]

val validLeaderAndIsrs \= mutable.Buffer.empty\[(TopicPartition, LeaderAndIsr)\]

// 遍历每个分区的 znode 节点数据

getDataResponses.foreach { getDataResponse =>

val partition \= getDataResponse.ctx.get.asInstanceOf\[TopicPartition\]

val currState \= partitionState(partition)

// 如果成功拿到 znode 节点数据

if (getDataResponse.resultCode == Code.OK) {

// 解码分区状态节点数据进行匹配

TopicPartitionStateZNode.decode(getDataResponse.data, getDataResponse.stat) match {

// 节点数据中含 Leader 和 ISR 信息

case Some(leaderIsrAndControllerEpoch) =>

// 如果节点数据的 Controller Epoch 值大于当前 Controller Epoch 值，则表示该分区已经被一个更新的 Controller 选举过 Leader 了，此时必须终止本次Leader选举。

if (leaderIsrAndControllerEpoch.controllerEpoch > controllerContext.epoch) {

val failMsg \= s"Aborted leader election for partition $partition since the LeaderAndIsr path was " +

s"already written by another controller. This probably means that the current controller $controllerId went through " +

s"a soft failure and another controller was elected with epoch ${leaderIsrAndControllerEpoch.controllerEpoch}."

// 将该分区加入到选举失败分区列表

failedElections.put(partition, Left(new StateChangeFailedException(failMsg)))

} else {

// 将 Leader 和 ISR 信息按照分区分组加入到可选举 Leader 分区列表

validLeaderAndIsrs += partition -> leaderIsrAndControllerEpoch.leaderAndIsr

}

// 如果节点数据不含 Leader 和 ISR 信息

case None \=\>

val exception \= new StateChangeFailedException(s"LeaderAndIsr information doesn't exist for partition $partition in $currState state")

// 将该分区加入到选举失败分区列表

failedElections.put(partition, Left(exception))

}

// 如果没有拿到 znode 节点数据，则将该分区加入到选举失败分区列表

} else if (getDataResponse.resultCode == Code.NONODE) {

val exception \= new StateChangeFailedException(s"LeaderAndIsr information doesn't exist for partition $partition in $currState state")

failedElections.put(partition, Left(exception))

} else {

failedElections.put(partition, Left(getDataResponse.resultException.get))

}

}

// 判断 validLeaderAndIsrs 容器中是否包含可选举 Leader 的分区。如果一个满足选举 Leader 的分区都没有，方法直接返回

if (validLeaderAndIsrs.isEmpty) {

return (failedElections.toMap, Seq.empty)

}

### **2.11.2 开始选举 Leader**

// 开始选举 Leader，并根据选举策略将分区进行分类：有 Leader、无 Leader

val (partitionsWithoutLeaders, partitionsWithLeaders) = partitionLeaderElectionStrategy match {

// 匹配

case OfflinePartitionLeaderElectionStrategy(allowUnclean) =>

val partitionsWithUncleanLeaderElectionState \= collectUncleanLeaderElectionState(

validLeaderAndIsrs,

allowUnclean

)

// 为 OffinePartition 分区选举 Leader

leaderForOffline(controllerContext, partitionsWithUncleanLeaderElectionState).partition(\_.leaderAndIsr.isEmpty)

case ReassignPartitionLeaderElectionStrategy \=\>

// 为副本重分配的分区选举 Leader

leaderForReassign(controllerContext, validLeaderAndIsrs).partition(\_.leaderAndIsr.isEmpty)

case PreferredReplicaPartitionLeaderElectionStrategy \=\>

// 为分区执行 Preferred 副本 Leader 选举

leaderForPreferredReplica(controllerContext, validLeaderAndIsrs).partition(\_.leaderAndIsr.isEmpty)

case ControlledShutdownPartitionLeaderElectionStrategy \=\>

// 为 Broker 正常关闭而受影响的分区选举 Leader

leaderForControlledShutdown(controllerContext, validLeaderAndIsrs).partition(\_.leaderAndIsr.isEmpty)

}

这一步是根据给定的PartitionLeaderElectionStrategy，调用PartitionLeaderElectionAlgorithms的不同方法执行Leader选举，同时，区分出成功选举 Leader 和未选出 Leader 的分区。

选择Leader的规则：就是选择副本集合中首个存活且处于 ISR 中的副本作为新 Leader。

这里面涉及的方法我们会在后面篇章单独剖析，这里就简单了解它的作用即可。

### **2.11.3 更新 ZK 节点数据、元数据缓存数据**

partitionsWithoutLeaders.foreach { electionResult =>

// 将所有选举失败的分区全部加入到Leader选举失败分区列表

val partition \= electionResult.topicPartition

val failMsg \= s"Failed to elect leader for partition $partition under strategy $partitionLeaderElectionStrategy"

failedElections.put(partition, Left(new StateChangeFailedException(failMsg)))

}

// AR 列表

val recipientsPerPartition \= partitionsWithLeaders.map(result => result.topicPartition -> result.liveReplicas).toMap

// ISR 列表

val adjustedLeaderAndIsrs \= partitionsWithLeaders.map(result => result.topicPartition -> result.leaderAndIsr.get).toMap

// 使用新选举的 Leader 和 ISR 信息更新 ZooKeeper 上分区的 znode 节点数据

val UpdateLeaderAndIsrResult(finishedUpdates, updatesToRetry) = zkClient.updateLeaderAndIsr(

adjustedLeaderAndIsrs, controllerContext.epoch, controllerContext.epochZkVersion)

finishedUpdates.forKeyValue { (partition, result) =>

result.foreach { leaderAndIsr =>

val replicaAssignment \= controllerContext.partitionFullReplicaAssignment(partition)

// 构建 LeaderAndIsr 请求，并将该请求加入到 Controller 待发送请求集合

val leaderIsrAndControllerEpoch \= LeaderIsrAndControllerEpoch(leaderAndIsr, controllerContext.epoch)

controllerContext.putPartitionLeadershipInfo(partition, leaderIsrAndControllerEpoch)

// 给这些分区所在的 Broker 发送 LeaderAndIsrRequest 请求同步该分区的数据

controllerBrokerRequestBatch.addLeaderAndIsrRequestForBrokers(

recipientsPerPartition(partition), partition,

leaderIsrAndControllerEpoch, replicaAssignment, isNew = false)

}

}

// 返回选举结果，包括成功选举并更新 ZooKeeper 节点的分区、选举失败分区以及ZooKeeper节点更新失败的分区

(finishedUpdates ++ failedElections, updatesToRetry)

这一步首先，将上一步中所有选举失败的分区全部加入到Leader选举失败分区列表 。然后，使用新选举的 Leader 和 ISR 信息，更新 ZooKeeper 上分区的 Znode 节点数据。对于 ZooKeeper Znode节点数据更新成功的那些分区，会封装对应的 Leader 和 ISR 信息，构建 LeaderAndIsr 请求，并将该请求加入到 Controller 待发送请求集合，给这些分区所在的 Broker 发送 LeaderAndIsrRequest 请求同步该分区的数据，等待后续统一发送。最后返回选举结果，包括成功选举并更新 ZooKeeper 节点的分区列表、选举失败分区列表，以及 ZooKeeper节点更新失败的分区列表。

## **03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头从「**KafkaController**」以及 「**Topic 创建、删除流程**」引出 「**分区状态机 PartitionStateMachine**」概念。

2、接着带大家深度剖析了 「**分区状态机 PartitionStateMachine**」源码总览。PartitionStateMachine 是 Kafka Broker 端源码中控制分区状态流转的实现类。每个 Broker 启动时都会创建 PartitionStateMachine 实例，但只有 Controller 组件所在的 Broker 才会启动它。

3、接着带大家深度剖析了**分区状态的定义以及状态转换流程**。当前，Kafka定义了4类分区状态。同时，它还规定了每类状态合法的前置状态。

4、接着带大家深度剖析了**分区选举 Leader 的 4 大场景以及对应算法实现**。

4、最后挨个剖析了 「**ZkPartitionStateMachine**」的重要方法以及内部的**状态转换源码流程**。

下篇我们来深度剖析「**副本同步实现原理**」，大家期待，我们下期见。