大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端之控制器元数据管理**」，通过剖析元数据引出了「**ControllerContext**」类以及都有哪些元数据组成，另外重点剖析了部分「**元数据**」的定义及实现，从今天开始，我们来深度剖析 Kafka「**Controller**」的底层源码实现，这是 Controller 系列第四篇，我们接着来深度聊聊「**Kafka 服务端控制器 Controller 选举机制实现原理**」。

##   
**01 总体概述**

在 [【服务端 Broker 源码分析系列第十七篇】图解 Kafka 源码之 Broker 启动集群如何感知](https://articles.zsxq.com/id_j9ibrxqafawv.html) 这篇中，我们深度剖析了「**Controller**」的启动过程，在启动时会进行控制器选举。

在一个 Kafka 集群中，某段时间内只能有一台 Broker 被选举为 Controller。随着时间的推移，可能会有不同的 Broker 陆续被选举为 Controller，但是在**某一时刻，只有一个 Broker 能成为 Controller**。

那究竟选择哪个 Broker 成为 Controller 呢？当前，Controller 的选举过程依赖 ZooKeeper 完成。在 ZooKeeper 定义了 **/Controller 临时节点**，来完成 Controller 的选举，即第一个成功抢注 Zookeeper 中 **/Controller 临时节点**的 Broker 将成为控制器。

Controller 选举正是基于「**/Controller 临时节点**」的这个特性，一旦 Broker 与 ZooKeeper 的会话终止，该临时节点就会消失。每个 Broker 都会监听「**/Controller 临时节点**」随时准备选举为 Controller 角色，如下图：

  
![](https://article-images.zsxq.com/Fggk_E2vLfu4kSLNNpLWtVKfWc1Q)

上图中，集群上所有的 Broker 都在实时监听 ZooKeeper 上的 「**/Controller 临时节点**」。这里的「**监听**」有以下两个情况：

1.  **监听该节点是否存在**：如果发现该节点不存在，Broker 会立即「**创建**」/controller 节点。创建成功的 Broker 当选为新一轮的 Controller。
2.  **监听该节点是否发生变更**：同样，一旦发现该节点的内容发生了变化，Broker 也会立即启动新一轮的 Controller 选举。

那么接下来我们就来深度剖析下 「**Controller**」的选举流程以及选举时机。

## **02 Controller 选举流程**

接下来我们重点来剖析下 Controller 的选举。整个选举过程分为两个步骤： 「**触发选举**」、 「**开始选举**」。

##   
**2.1 触发选举流程**

这里有三种场景：

1.  集群刚启动时。
2.  Broker 检测 /controller 节点消失时。
3.  Broker 检测 /controller 节点发生变更时。

这三种场景最后都得执行「**选举 Controller**」的操作，我们分别来深度剖析下其流程：

##   
**场景一：集群刚启动时**

集群首次启动时，Controller 还未被选举出来。所以在 Broker 启动时，即 Controller 启动时，首先会将 [Startup](http://startup/) 事件写入到事件队列中，然后启动对应的事件处理线程和 ControllerChangeHandler 监听器，最后依赖事件处理线程进行 Controller 的选举。

在 [【服务端 Broker 源码分析系列第十七篇】图解 Kafka 源码之 Broker 启动集群如何感知](https://articles.zsxq.com/id_j9ibrxqafawv.html) 这篇中，我们已经简单剖析过。

这里再来讲解下，当 Broker 启动时，它会调用 startup 方法启动 ControllerEventThread 线程。**对于每个 Broker 都需要做这些事情，并不是只有 Controller 所在的 Broker 才需要做这些事情**。

def startup() = {

// 1、注册 ZooKeeper 状态变更监听器，当 zk 重新初始化时会调用，它是用于监听 Zookeeper 会话过期的

zkClient.registerStateChangeHandler(new StateChangeHandler {

override val name: String = StateChangeHandlers.ControllerHandler

// zk 初始化之后调用

override def afterInitializingSession(): Unit = {

//设置 RegisterBrokerAndReelect 事件，重新选举 Controller 的 Leader

eventManager.put(RegisterBrokerAndReelect)

}

// zk 初始化之前调用

override def beforeInitializingSession(): Unit = {

val queuedEvent \= eventManager.clearAndPut(Expire)

// Block initialization of the new session until the expiration event is being handled,

// which ensures that all pending events have been processed before creating the new session

//等待controller中的事件处理完

queuedEvent.awaitProcessing()

}

})

// 2、写入 Startup 事件到事件管理器，eventManager 会调用 KafkaController#process() 处理 Startup 事件， Startup 事件中会进行 controller 的选举等初始化处理

eventManager.put(Startup)

// 3、启动 ControllerEventThread 线程，开始处理事件队列中的 ControllerEvent

eventManager.start()

}

首先会注册 ZooKeeper 状态变更监听器，用于监听 Broker 与 ZooKeeper 之间的会话是否过期。接着写入 Startup 事件到事件队列，然后启动 ControllerEventThread 线程，开始处理事件队列中的 Startup 事件。

接着，我们来看下 [KafkaController#process](http://kafkacontroller/#process) 方法是如何处理 Startup 事件的，源码如下：

override def process(event: ControllerEvent): Unit = {

try {

event match {

....

case Startup \=\>

// 处理 Startup 事件

processStartup()

}

} catch {

....

} finally {

....

}

}

private def processStartup(): Unit = {

// 1、注册【/controller】节点变更监听器并检查节点是否存在

zkClient.registerZNodeChangeHandlerAndCheckExistence(controllerChangeHandler)

// 2、执行选举操作

elect()

}

从上面这段源码得出，主要用来处理向 **/controller**「**注册 ControllerChangeHandler 监听器**」、接着进行「**选举控制器**」操作。

总体来说，集群启动时，Broker 通过向事件队列写入 [Startup](http://startup/) 事件的方式，来触发 Controller 的选举。

  
![](https://article-images.zsxq.com/FugVyMoSqqJwLbPli-iTTy2M4pLB)

## **场景二：Broker 检测 /controller 节点消失时**

当 Broker 检测到 /controller 节点消失时，意味着此时整个集群中已经没有了 Controller。因此当检测到 /controller 节点消失的 Broker，都会立即调用 elect 方法执行选举逻辑。

简单来说，选举就是依赖 Zookeeper 来完成 Controller 的选举，对于 3.x 版本的会通过 Raft 算法来选举 Controller，后面抽空补充。

class ControllerChangeHandler(eventManager: ControllerEventManager) extends ZNodeChangeHandler {

override val path: String = ControllerZNode.path

....

//当节点被删除，表明当前无 Leader节点，处理 Reelect 事件，进行重新选举

override def handleDeletion(): Unit = eventManager.put(Reelect)

....

}

override def process(event: ControllerEvent): Unit = {

try {

event match {

....

// ControllerChange事件

case ControllerChange \=\>

processControllerChange()

// Reelect事件

case Reelect \=\>

processReelect()

....

}

} catch {

....

} finally {

....

}

}

// 如果是 ControllerChange 事件，仅执行卸任逻辑即可

private def processControllerChange(): Unit = {

maybeResign()

}

// 如果是 Reelect 事件，还需要执行 elect 方法参与新一轮的选举

private def processReelect(): Unit = {

maybeResign()

elect()

}

  
![](https://article-images.zsxq.com/FkI8H2qllHofEVLZcEH2EQeEopLt)

## **场景三：Broker 检测 /controller 节点发生变更时**

Broker 检测到 /controller 节点数据发生变化，通常可以说明 Controller已经发生「**易主**」了，分为两种情况：

1.  如果 Broker 之前是 Controller，那么该 Broker 需要首先执行卸任操作，然后再尝试选举。
2.  如果 Broker 之前不是 Controller，那么该 Broker 直接去选举新 Controller。

在 maybeResign 方法中已经解释了这两种情况，可能需要执行卸任操作也可能不需要，源码如下：

private def maybeResign(): Unit = {

// 1、判断该 Broker 之前是否是 Controller

val wasActiveBeforeChange \= isActive

// 2、注册 ControllerChangeHandler 监听器

zkClient.registerZNodeChangeHandlerAndCheckExistence(controllerChangeHandler)

// 3、获取当前集群 Controller 所在的 Broker Id，如果没有 Controller 则返回-1

activeControllerId = zkClient.getControllerId.getOrElse(-1)

// 4、如果该 Broker 之前是 Controller 但现在不是了，则执行卸任操作

if (wasActiveBeforeChange && !isActive) { // 卸任操作

onControllerResignation()

}

}

这里的第一步**非常关键，它是决定是否需要执行卸任操作的重要依据**，步骤如下：

1.  判断该 Broker 之前是否是 Controller。
2.  注册 ControllerChangeHandler 监听器。
3.  获取当前集群 Controller 所在的Broker Id，如果没有 Controller 则返回 -1。
4.  如果该 Broker 之前是 Controller 但现在不是了，则执行卸任操作。

从上面步骤中可以得出，如果 Broker 之前不是 Controller，就没必要执行卸任操作； 如果 Broker 之前是 Controller，但是现在不是了，则会执行卸任操作。

接下来看下卸任操作的实现逻辑：

private def onControllerResignation(): Unit = {

debug("Resigning")

// de-register listeners

// 1、取消 ZooKeeper 监听器的注册

zkClient.unregisterZNodeChildChangeHandler(isrChangeNotificationHandler.path)

zkClient.unregisterZNodeChangeHandler(partitionReassignmentHandler.path)

zkClient.unregisterZNodeChangeHandler(preferredReplicaElectionHandler.path)

zkClient.unregisterZNodeChildChangeHandler(logDirEventNotificationHandler.path)

unregisterBrokerModificationsHandler(brokerModificationsHandlers.keySet)

// shutdown leader rebalance scheduler

// 2、关闭 Kafka 线程调度器，其实就是取消定期的 Leader 重选举

kafkaScheduler.shutdown()

// 3、将统计字段全部清零

offlinePartitionCount = 0

preferredReplicaImbalanceCount = 0

globalTopicCount = 0

globalPartitionCount = 0

topicsToDeleteCount = 0

replicasToDeleteCount = 0

ineligibleTopicsToDeleteCount = 0

ineligibleReplicasToDeleteCount = 0

// stop token expiry check scheduler

// 4、关闭 Token 过期检查调度器

if (tokenCleanScheduler.isStarted)

tokenCleanScheduler.shutdown()

// de-register partition ISR listener for on-going partition reassignment task

// 5、取消分区重分配监听器的注册

unregisterPartitionReassignmentIsrChangeHandlers()

// shutdown partition state machine

// 6、关闭分区状态机

partitionStateMachine.shutdown()

// 7、取消主题变更监听器的注册

zkClient.unregisterZNodeChildChangeHandler(topicChangeHandler.path)

// 8、取消分区变更监听器的注册

unregisterPartitionModificationsHandlers(partitionModificationsHandlers.keys.toSeq)

// 9、取消主题删除监听器的注册

zkClient.unregisterZNodeChildChangeHandler(topicDeletionHandler.path)

// shutdown replica state machine

// 9、关闭副本状态机

replicaStateMachine.shutdown()

// 10、取消 Broker 变更监听器的注册

zkClient.unregisterZNodeChildChangeHandler(brokerChangeHandler.path)

// 11、关闭 Controller 通道管理器

controllerChannelManager.shutdown()

// 12、清空集群元数据

controllerContext.resetContext()

info("Resigned")

}

该方法主要用来**清空统计字段，取消 Zookeeper 监听器、关闭各种状态机和管理器等**。

![](https://article-images.zsxq.com/FlItvKZCjGGqcksMqprxkYajLC12)

至此，带你深度剖析了上面三种场景，接下来我们来剖析下选举过程。

## **2.2 开始选举 Controller 流程**

上面三种场景都会调用 elect() 方法来执行控制器选举操作，源码如下：

private def elect(): Unit = {

//1、获取当前 Controller 所在 Broker 的序号，如果 Controller 不存在，返回 -1

activeControllerId = zkClient.getControllerId.getOrElse(-1)

// 2、如果当前 Controller 已经选出来了，直接返回即可

if (activeControllerId != -1) {

debug(s"Broker $activeControllerId has been elected as the controller, so stopping the election process.")

return

}

try {

// 3、注册Controller相关信息，主要是创建 /controller 节点

val (epoch, epochZkVersion) = zkClient.registerControllerAndIncrementControllerEpoch(config.brokerId)

controllerContext.epoch = epoch

controllerContext.epochZkVersion = epochZkVersion

activeControllerId \= config.brokerId

info(s"${config.brokerId} successfully elected as the controller. Epoch incremented to ${controllerContext.epoch} " +

s"and epoch zk version is now ${controllerContext.epochZkVersion}")

// 4、执行当选 Controller 的后续逻辑

onControllerFailover()

} catch {

case e: ControllerMovedException =>

// 执行卸任操作

maybeResign()

if (activeControllerId != -1)

debug(s"Broker $activeControllerId was elected as controller instead of broker ${config.brokerId}", e)

else

warn("A controller has been elected but just resigned, this will result in another round of election", e)

case t: Throwable =>

error(s"Error while electing or becoming controller on broker ${config.brokerId}. " +

s"Trigger controller movement immediately", t)

triggerControllerMove()

}

}

该方法首先检查 Controller 是否已经选出来了，**集群中所有的 Broker 都会执行这些操作**，**所以很有可能某些 Broker 在执行该方法时 Controller 已经被选举出来了**。如果 Controller 已经选出来了直接返回即可。

相反如果 Controller 还未被选举出来，那么就会尝试创建 /controller 节点去抢注 Controller。一旦抢注成功，就调用 onControllerFailover 方法，执行选举成功后的动作。这些动作包括注册各类 ZooKeeper 监听器、删除日志路径变更和 ISR 副本变更通知事件、启动 Controller 通道管理器，以及启动副本状态机和分区状态机，在上一篇中已经剖析过了，你可以点击 [【服务端 Broker 源码分析系列第十七篇】图解 Kafka 源码之 Broker 启动集群如何感知](https://articles.zsxq.com/id_j9ibrxqafawv.html) 进行查看。

如果抢注失败了，代码会抛出 ControllerMovedException 异常。这通常表明 Controller 已经被其他 Broker 抢先占据了，此时调用 maybeResign 方法去执行卸任操作。

  
![](https://article-images.zsxq.com/ljn1-UA64wfWOVhNRqPH8WhhZW1i)

##   
**03 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头通过剖析上上篇关于「**Controller**」启动过程，引出了「**Controller 选举**」时机。

2、从「**触发选举**」三种场景、「**开始选举**」两个大步骤带你深度剖析了 「**Controller 选举**」的流程以及源码实现。

下篇我们来深度剖析「**控制器 Controller 如何管理请求发送**」，大家期待，我们下期见。