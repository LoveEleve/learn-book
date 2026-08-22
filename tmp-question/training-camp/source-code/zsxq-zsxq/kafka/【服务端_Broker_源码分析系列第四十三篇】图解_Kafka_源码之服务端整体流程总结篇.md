大家好，我是 **华仔**, 又跟大家见面了。

上篇，我们深度剖析了「**kafka Raft 模块关于快照管理与数据清理流程**」， 今天主要来总结下「**kafka 服务端整体流程总结**」，从宏观的角度来梳理下服务端的各个模块是如何串联起来的。

##   
**01 总体概述**

通过「**场景驱动**」的方式，前面[四十二篇](http://xn--4kqp8i9sdku7c/)文章，我们已经从消息批次的累积到最后构造 Request 请求并通过网络组件 NetworkClient 将消息发送出去后，Kafka Broker 服务端整个处理过程的源码进行了详细的剖析。

今天我们就来总结下这整个过程。

1.  Broker 服务端启动核心流程。
2.  Broker 集群如何选举控制器。
3.  Broker 集群如何选举 Leader。
4.  Broker 服务端处理请求核心流程。
5.  Broker 服务端整个日志存储系统核心流程。
6.  Broker 服务端处理 Topic 请求核心流程。
7.  Broker 服务端副本之间同步数据核心流程。
8.  Broker 服务端处理数据过程中状态机实现。
9.  Broker 服务端处理数据过程中延迟机制实现。
10.  事务消息处理核心流程。
11.  基于 3.x 版本的 KRaft 模块下的请求处理流程。

## **02 Broker 服务端启动核心流程**

详情请点击 [【服务端 Broker 源码分析系列第十六篇】图解 Kafka 源码之服务端启动流程（ZK、Raft 两种启动模式）](https://articles.zsxq.com/id_mj6vi10s49c1.html)

关于「**kafka Zookeeper 模式**」启动比较简单，启动后的网络通信架构图如下：

  
![](https://article-images.zsxq.com/FuhT3UGGqSYL1lqiYeT1WyJjmYcl)

关于「**kafka Raft 模式**」启动后的网络通信架构图如下，可以看到 [KafkaRaftServer](http://kafkaraftserver%20/) 将节点分为「**Broker 节点**」和 「**Controller 节点**」，它们都会提供独立的网络服务。其中 「**Broker 节点**」提供 Kafka 数据服务，比如读写消息等，「**Controller 节点**」则提供管理 Kafka 的服务，比如创建主题等：

![](https://article-images.zsxq.com/Fnr6V4A8CezKlzuVOoDsZAkLhEjK)

## **03 Broker 集群如何选举控制器**

当 Broker 启动之后就需要进行选举控制器。

关于「**kafka Zookeeper 模式**」 下主要是借助 Zookeeper 来实现的，即抢先在 ZK 下注册 [/Controller](http://controller/) 临时节点，如下图：

![](https://article-images.zsxq.com/FnFSe0T1i616nSRwgkQiGdU_Ykg5)

而对于「**kafka Raft 模式**」 下，移除了对 Zookeeper 的依赖，它是通过配置文件中的属性 [process.roles](http://process.roles/) 指定节点角色，如果指定了 Controller 角色时才被创建，它用来处理元数据类请求，包括 topic 创建删除等，由配置 [controller.listener.names](http://controller.listener.names/) 指定供外部请求的端口等信息，内部会启动 **ControllerServer** 的 socketServer，提供 Controller 节点的网络服务。

一个节点可被直接指定为 Controller 节点，Controller 不再需要和 zk 通信管理集群元数据。 整个 Kafka 集群中 Controller 节点可以存在多个，共同组成 Controller 集群，负责处理集群元数据。

> Kafka 服务端网络通信的架构目前大致经历过 3 个阶段的演进：
> 
> 1、早期的设计是任意节点都可能成为 Controller，同时集群中每个节点都需要暴露端口给 Controller 进行连接方便控制，所以节点使用同一个端口监听处理所有网络请求。
> 
> 2、当到了 2.2 版本开始将请求分流，Kafka 节点分别用数据面 DataPlane 和控制面 ControlPlane 来对应处理数据类请求 和 控制类请求(来自集群内部Controller的控制请求)，二者区分出不同的端口(控制类请求的处理端口由 control.plane.listener.name 配置)，且同时存在于同一个 KafkaServer 中。如感兴趣可以参考 Kafka 社区记录KAFKA-4453 add request prioritization。
> 
> 3、到了 2.8 版本，Kafka 推出了剔除 zk 的 KRaft 模式。在该模式下 KafkaRaftServer 分别抽象出对应Controller 角色的 ControllerServer 和对应 Broker 角色的 BrokerServer，消息生产之类的请求只会被 BrokerServer 处理，元数据类请求则由 ControllerServer 处理，一个节点可同时充当两种角色。在 KRaft 模式下，Kaka 节点已经不支持控制类请求， control.plane.listener.name 配置在 3.0 版本后的 KRaft 模式下将导致异常。

关于「**kafka Controller 模块源码**」：

[【服务端 Broker 源码分析系列第十七篇】图解 Kafka 源码之 Broker 启动集群如何感知](https://articles.zsxq.com/id_j9ibrxqafawv.html)

[【服务端 Broker 源码分析系列第十八篇】图解 Kafka 源码之控制器 Controller 元数据管理](https://articles.zsxq.com/id_z6wkq8gxjdgk.html)

[【服务端 Broker 源码分析系列第十九篇】图解 Kafka 源码之控制器 Controller 选举机制实现原理](https://articles.zsxq.com/id_rs4qxbgf2okt.html)

[【服务端 Broker 源码分析系列第二十篇】图解 Kafka 源码之控制器 Controller 如何管理请求发送的](https://articles.zsxq.com/id_nep2gwb20mh6.html)

[【服务端 Broker 源码分析系列第二十篇】图解 Kafka 源码之控制器 Controller 如何管理请求发送的](https://articles.zsxq.com/id_nep2gwb20mh6.html)

[【服务端 Broker 源码分析系列第二十一篇】图解 Kafka 源码之控制器 Controller 如何处理事件的](https://articles.zsxq.com/id_uvfk47q4sa03.html)

[【服务端 Broker 源码分析系列第三十一篇】图解 Kafka 源码之 Broker 异步更新元数据缓存实现原理](https://articles.zsxq.com/id_pa94he7udtww.html)

## **04 Broker 集群如何选举 Leader**

当集群选举完 Controller 节点后，就会选举分区 Leader 节点，对于 Kafka 来说，只有 Leader 节点才能对外提供读写请求。

Kafka 目前提供 4 种 Leader 选举策略，分别是「**分区下线后的 Leader 选举**」、「**分区执行副本重分配的 Leader 选举**」、「**分区执行 Preferred 副本的 Leader 选举**」、「**Broker 下线时的分区 Leader 选举**」。

这 4 类选举策略在选择 Leader 这件事情上有着类似的逻辑，那就是，它们几乎都是「**选择当前副本有序集合中的**」、「**首个处于 ISR 集合中的存活副本作为新的 Leader 节点**」。

关于「**kafka Zookeeper 模式**」下选举流程：

[【服务端 Broker 源码分析系列第二十五篇】图解 Kafka 源码之分区状态机机制以及 Leader 选举实现原理](https://articles.zsxq.com/id_jwlx4lrd7okd.html)

关于「**kafka Raft 模式**」下选举流程：

正常的集群选主流程，如下图所示：

![](https://article-images.zsxq.com/lnOogKrzjq2PRho6Cn-5yp7cB4H2)

如果集群中有「**多个投票节点同时启动**」，并且都是「**初次启动**」，那么很可能这些节点都会切换到「**Candidate**」状态。此时它们就算收到了其他「**候选者**」的 Vote 投票请求，也不会为其他候选者投票，选举就陷入了失败的僵局。对于这种情况， Kafka 引入了「**回退机制**」进行处理，大致流程如下图所示：

![](https://article-images.zsxq.com/lkTW_7VIYMa5-erQHTJkGghHkEHT)

回退机制的核心在于使用 [controller.quorum.election.backoff.max.ms](http://controller.quorum.election.backoff.max.ms/) 配置设置一个随机的回退超时时间，[KafkaRaftClient#pollCandidate()](http://kafkaraftclient/#pollCandidate\(\)) 方法会检查「**候选者**」节点是否处于「**回退状态**」，回退状态的候选者将不再发送 Vote 请求。

一旦回退的超时时间到达，最早退出「**回退状态**」的「**候选者**」节点将重新发起 [VoteRequest](http://voterequest/) 投票请求，此时投票请求中携带的集群 epoch 增加了一个版本，收到请求的其他候选者会因为版本落后而回退到 [UnattachedState](http://unattachedstate/) 状态，此时可以顺利地投「**赞成票**」，选举僵局解除。

[【服务端 Broker 源码分析系列第三十七篇】图解 Kafka 源码之 KRaft Leader 选举机制流程](https://articles.zsxq.com/id_ox3kxhfx7li2.html)

## **05 Broker 服务端处理请求核心流程**

当 Broker 集群选举出 「**kafka Controller**」、「**kafka Leader 副本节点**」后，就会对外处理请求，关于网络请求模块的架构图如下：

  
![](https://article-images.zsxq.com/lgWHCod6TVaWcSh3HgoUpV6okBlc)

整个网络层请求处理流程总结可以点击这篇，[【服务端Broker源码分析系列第七篇】图解Kafka源码之网络层请求处理全流程总结](https://articles.zsxq.com/id_xtza6mo3tkfb.html) ，内部包含 7 篇各个组件的源码剖析。

[【服务端 Broker 源码分析系列启蒙篇】图解 Java NIO 多路复用实现原理](https://articles.zsxq.com/id_nmsosqvfwpwl.html)

[【服务端 Broker 源码分析系列第一篇】图解 Kafka 源码之 Reactor 网络模型架构设计](https://articles.zsxq.com/id_te1vv3pi7reo.html)

[【服务端 Broker 源码分析系列第二篇】图解Kafka源码SocketServer组件之Acceptor线程架构设计](https://articles.zsxq.com/id_raazwmww8fo1.html)

[【服务端Broker源码分析系列第三篇】图解Kafka源码SocketServer组件之Porcessor线程架构设计](https://articles.zsxq.com/id_bh7qwrhpw9mb.html)

[【服务端Broker源码分析系列第四篇】图解Kafka源码之 RequestChannel 请求通道架构设计](https://articles.zsxq.com/id_atxidhwk1phj.html)

[【服务端Broker源码分析系列第五篇】图解Kafka源码之 KafkaRequestHandler I/O 线程池剖析](https://articles.zsxq.com/id_ohjd0i8l3w57.html)

[【服务端Broker源码分析系列第六篇】图解Kafka源码之 KafkaApis 详解](https://articles.zsxq.com/id_k4o72eh3pai3.html)

网络通信层是共用的，不需要 「**kafka Raft 模式**」还是 「**kafka Zookeeper 模式**」。

## **06 Broker 服务端整个日志存储系统核心流程**

我们知道在 Producer 端，会维护一个 [< ConcurrentMap > batches](http://%20concurrentmap%20%20batches/) 的变量以消息批次为数据单元存储消息数据，然后会根据 [topic-partition](http://topic-partition/) 的 Leader 信息，将 Leader 在同一台 Broker 机器上的 batch 放在一个 request 中，发送到 Broker 端，这样可以节省很多网络开销，提高发送效率。

当生产者发送批次消息到 Broker 端时，会交由 「**日志组件**」来处理并存储的。

![](https://article-images.zsxq.com/FnDPoaG_9_LVjOlLMYsxj-t1zvVp)

 从上图可以看出来，Kafka 是基于「**主题**」+ 「**分区**」 + 「**副本**」+「**分段**」 + 「**索引**」的结构：

这里我们主要来说下，「**分段**」 + 「**索引**」。

在 Kafka 中，**为了避免日志文件过大导致数据定位效率低下**，「**分区**」将通过「**日志分段**」的方式将 「**Log**」 切分为多个 「**LogSegment 日志段**」，相当于一个巨型文件被平均分割为一些相对较小的文件，这样也便于消息的查找、维护和清理。这样在做历史数据清理的时候，直接删除旧的「**LogSegment 日志段**」文件就可以了。

「**LogSegment 日志段**」是一个逻辑上的概念，一个「**LogSegment 日志段**」包含磁盘上 3 个二进制格式的物理文件，一个「**日志文件 .log**」，一个「**位置索引文件 .index**」，一个 「**时间索引文件 .timeindex**」，其中日志文件用于记录消息，索引文件中保存了消息的索引。

随着消息的不断写入，日志段文件的数据量大小到达指定阈值时，就会创建新的日志文件和索引文件继续写入后续的消息和索引信息。

日志文件的文件名的命名规则是 [\[baseOffset\].log](http://%5BbaseOffset%5D.log)，「**baseOffset**」是日志文件中第一条消息的offset，默认为 [00000000000000000000](http://00000000000000000000%20/) 。

例如：Topic 为 message3，有 2 个分区，1 个副本，磁盘上对应的日志目录如下图所示：

![](https://article-images.zsxq.com/Fn9jVfs93t2J5-mke52k0c5ZeTE6)

![](https://article-images.zsxq.com/FjoICmaX479SzgHwU6okodtGIZZ3)

![](https://article-images.zsxq.com/FlZr9P5Pl8yb0h_b7TJFBf5faUNG)

另外为了提高日志查询消息的效率，每个日志文件都会对应一个「**位置索引文件**」，它建立「**消息偏移量**」到「**物理位置**」的映射关系，这样更方便的定位消息所在物理文件的位置。

「**位置索引文件**」使用 「**稀疏哈希索引方式**」来维护数据，每次间隔特定大小「**1G**」的消息则记录一个「**消息索引**」，其内容为键值对 「**<消息批次最后一条消息偏移量，物理地址>**」的映射关系，如下图所示：

![](https://article-images.zsxq.com/FsmRpfy6l37q-eKJEGYnHAfttpIm)

关于存储的日志组件如下图：

![](https://article-images.zsxq.com/Fn14DkayjaPFfu12fifk4lnQ39k0)

关于「**kafka 日志模块源码**」：

  
[【服务端 Broker 源码分析系列第八篇】图解 Kafka 源码之日志存储机制介绍以及核心对象管理梳理](https://articles.zsxq.com/id_rqum80f450hc.html)

[【服务端 Broker 源码分析系列第九篇】图解 Kafka 源码之 ReplicaManager 日志读写流程](https://articles.zsxq.com/id_5dq174gc0wji.html)

[【服务端 Broker 源码分析系列第十篇】图解 Kafka 源码之 LogManager 磁盘文件管理组件](https://articles.zsxq.com/id_fjotrswcfval.html)

[【服务端 Broker 源码分析系列第十一篇】图解 Kafka 源码之日志段 LogSegment 架构设计](https://articles.zsxq.com/id_t4f0eut7oe94.html)

[【服务端 Broker 源码分析系列第十二篇】图解 Kafka 源码之日志 Log 架构设计](https://articles.zsxq.com/id_ph1gx4aq04sq.html)

[【服务端 Broker 源码分析系列第十三篇】图解 Kafka 源码之日志 Log 对象操作](https://articles.zsxq.com/id_n9jj4wsw958w.html)

[【服务端 Broker 源码分析系列第十四篇】图解 Kafka 源码之稀疏索引架构设计](https://articles.zsxq.com/id_lw2exlc8nd7m.html)

[【服务端 Broker 源码分析系列第十五篇】图解 Kafka 源码之 Log 日志管理操作](https://articles.zsxq.com/id_5p3wdbasv08k.html)

## **07 Broker 服务端处理 Topic 请求核心流程**

关于 Topic 请求处理也区别「**kafka Zookeeper 模式**」、「**kafka Raft 模式**」。

对于「**kafka Zookeeper 模式**」下创建 Topic 的流程如下，删除 Topic 流程类似：

  
![](https://article-images.zsxq.com/lhrhKqgTaoD6bdiHMsdk0XPRd-nJ)

[【服务端 Broker 源码分析系列第二十二篇】图解 Kafka 源码之 Topic 创建请求处理流程](https://articles.zsxq.com/id_2q1nwfkg3fcb.html)

[【服务端 Broker 源码分析系列第二十三篇】图解 Kafka 源码之 Topic 删除请求处理流程](https://articles.zsxq.com/id_hwjqupg24t2f.html)

对于「**kafka Raft 模式**」下创建 Topic 的流程是通过事件机制来处理的：

  
![](https://article-images.zsxq.com/FtqXhlyAaYB20-LyHU4Qnvolzdo2)

整个处理流程时序图如下：

  
![](https://article-images.zsxq.com/FsXWLgKPv0sRYlw10X4KK3yLcH4w)

  
[【服务端 Broker 源码分析系列第三十八篇】图解 Kafka 源码之 KRaft 数据处理机制](https://articles.zsxq.com/id_xx081g35rpve.html)

## **08 Broker 服务端副本之间同步数据核心流程**

当集群 Leader 节点将数据写入本地日志文件之后，Follower 节点就会启动线程去拉取日志数据并写入本地日志。

关于底层「**拉取线程**」处理是统一的，主要分为三部分：即日志截断（truncate）+日志获取（buildFetch）+日志处理（processPartitionData），Follower 副本利用 [ReplicaFetcherThread](http://replicafetcherthread%20/) 线程实时地从 Leader 副本拉取消息并写入到本地日志，从而实现了与 Leader 副本之间的同步，如下图：

![](https://article-images.zsxq.com/llOQ2qlQGWkOsL7Wcyda9yJpa7lc)

而对于「**Kafka Raft 模式**」下还是通过事件来处理的。这里还是以「**创建 Topic 场景流程**」为例，从消息数据分区副本主从同步的场景来分析这个过程。当创建 Topic 后，整个消息变更发布与消费的完整流程如下图所示：

  
![](https://article-images.zsxq.com/ljc8ycOr0fBNLCJkdowrSVAdYilt)

接着来看下消息主从同步的流程，如下图：

![](https://article-images.zsxq.com/lhnyHswQ3m8VfFkfRR1aZ-nrrgGu)

![](https://article-images.zsxq.com/lqN-RdaT0ciizfBM5bofUBBywXqI)

整个「**Kafka Raft 模式**」处理时序图如下：

![](https://article-images.zsxq.com/Fm0dhcoMmv7Gt8p8ApKGxJNNrbDU)

关于「**kafka 副本之间同步数据源码**」：

  
[【服务端 Broker 源码分析系列第二十六篇】图解 Kafka 源码之副本同步实现原理（上）](https://articles.zsxq.com/id_j8h0gmrk5i6d.html)

[【服务端 Broker 源码分析系列第二十七篇】图解 Kafka 源码之副本同步实现原理（下）](https://articles.zsxq.com/id_64qw2onjkgpx.html)

[【服务端 Broker 源码分析系列第二十八篇】图解 Kafka 源码之 Leader 副本更新 ISR、HW 流程](https://articles.zsxq.com/id_vls7p1b8nh4a.html)

[【服务端 Broker 源码分析系列第二十九篇】图解 Kafka 源码之 ISR 、LeaderEpoch 机制实现原理](https://articles.zsxq.com/id_fvkc347dkp9u.html)

[【服务端 Broker 源码分析系列第四十篇】图解 Kafka 源码之 KRaft 消息数据主从同步机制](https://articles.zsxq.com/id_3o7516uxtd4b.html)

## **09 Broker 服务端处理数据过程中状态机实现**

在 Kafka 服务端中，关于状态机主要分为两种：「**分区状态机**」、「**副本状态机**」。

  
[PartitionStateMachine](http://partitionstatemachine%20/) 是 Kafka Controller 端定义的分区状态机，负责定义、维护和

管理合法的分区状态转换。每个 Broker 启动时都会实例化一个分区状态机对象，但只有 Controller 所在的 Broker

才会启动它。

这里我通过一张图来帮你总结下完整的状态转换，如下：

![](https://article-images.zsxq.com/Fhslgy7E8GNqAJ6z-7ggZBJM4vXQ)

[ReplicaStateMachine](http://replicastatemachine%20/) 是 Kafka Broker 端源码中控制副本状态流转的实现类。每个 Broker 启动时都会创建 ReplicaStateMachine 实例，但只有 Controller 组件所在的 Broker 才会启动它。

这里我通过一张图来帮你总结下完整的状态转换，如下：

![](https://article-images.zsxq.com/Fswn-7fVPtmiZqRBZMvK65KvUbIS)

它们分别管理着 Kafka 集群中所有副本和分区的状态转换。

关于「**kafka 状态机源码**」：

  
[【服务端 Broker 源码分析系列第二十四篇】图解 Kafka 源码之副本状态机机制实现原理](https://articles.zsxq.com/id_5rvr83cgqqvk.html)

[【服务端 Broker 源码分析系列第二十五篇】图解 Kafka 源码之分区状态机机制以及 Leader 选举实现原理](https://articles.zsxq.com/id_jwlx4lrd7okd.html)

## **10 Broker 服务端处理数据过程中延迟机制实现**

在源码学习过程中，我们或多或少都会遇到以下这样的处理逻辑：

// 触发对应的延迟操作

tryCompleteDelayedRequests()

延迟请求（Delayed Operation）：它是指因未满足条件而暂时无法被处理的 Kafka 请求。

这里举个例子，假如配置了 acks=all 的生产者发送的请求可能一时无法完成，因为 **Kafka 必须确保 ISR 副本集合中的所有副本都要成功响应这次写入请求才是真正完成**。在通常情况下，这些请求无法被立即处理。只有满足了条件或发生了超时，Kafka 才会把该请求标记为完成状态。这就是所谓的延迟请求。

另外 Kafka 中还存在其他的延迟操作：

1.  当消费者重平衡时，协调者需要等待消费组中的消费组都加入消费组后再进行分配。
2.  消费者要求 Broker 返回指定大小的消息内容，如果当前读取的消息内容不满足要求，则需要等待消息。

对于 Kafka 来说，它解决延迟请求是通过「**时间轮**」算法，关于该算法的实现关系图如下：

  
![](https://article-images.zsxq.com/FgBb3g-8AhJcIZwgzn-DpMm0GoKu)

关于「**多层时间轮**」示意图如下：

![](https://article-images.zsxq.com/FuLuCpHPFXCKK9zjvy_EF6ZwE-I2)

1.  第一层时间轮：tickMs＝ 1ms 、wheelSize=20 、interval=20ms。
2.  第二层时间轮的 tickMs 为第一层时间轮的 interval，即 20ms，每一层时间轮的 wheelSize 是固定的，都是 20，那么第二层时间轮的总体时间跨度就是 400ms。
3.  依次类推，第三层的时间轮的 interval 为 400ms，那么总体时间跨度就是 8000ms。

第N层时间轮走了一圈，等于 N+1 层时间轮走一格。即高一层时间轮的时间跨度等于当前时间轮的整体跨度。

在任务插入时，如果第一层时间轮不满足条件，就尝试插入到高一层的时间轮，以此类推。

随着时间推进，也会有一个时间轮降级的操作，原本延时较长的任务会从高一层时间轮重新提交到时间轮中，然后会被放在合适的低层次的时间轮当中等待处理；

关于「**kafka 时间轮源码**」：

[【服务端 Broker 源码分析系列第三十篇】图解 Kafka 源码之延迟机制、 时间轮实现原理](https://articles.zsxq.com/id_e178oe6nbzqw.html)

## **11 事务消息处理核心流程**

对于「**事务机制** 」来说，就如同关系型数据库类似，保证操作要么执行成功、要么都不执行。

它可以提供以下保证：

1.  支持多个操作的「**原子性**」执行：要么执行成功、要么不执行，且这些操作支持「**跨主题**」、「**跨分区**」。
2.  「**精确一次**」的语义保证，它可以保证事务中的操作只会执行一次。![](https://article-images.zsxq.com/FpZxhp18Z3Jwje_tRf3mmwtU_Efs)
3.  执行消费者中不同的隔离级别：「**UNCOMMITTED**」隔离级别可以读取未提交的事务消息；而「**COMMITTED**」隔离级别只能读取已提交的事务消息。

其初始化流程如下：

![](https://article-images.zsxq.com/Fi8Shrf_1vDNErTdgkDpP3XIURJ8)

而事务消息的处理也是需要生产者来发送，协调者来完成的。

关于「**kafka 事务消息源码**」：

[【服务端 Broker 源码分析系列第三十二篇】图解 Kafka 源码之事务设计以及初始化流程](https://articles.zsxq.com/id_7rg312ke2fl7.html)

[【服务端 Broker 源码分析系列第三十三篇】图解 Kafka 源码之事务消息发送处理流程](https://articles.zsxq.com/id_dj25ipks0726.html)

[【服务端 Broker 源码分析系列第三十四篇】图解 Kafka 源码之事务消息提交处理流程](https://articles.zsxq.com/id_9nsuvsc9wx8n.html)

## **12 基于 3.x 版本的 KRaft 模块请求处理流程**

最后来聊聊从 Kafka 2.8 版本开始提出了 KRaft 模式，在该模式下存在「**Controller**」、「**Broker**」两种角色，这里我们将「**Controller 角色**」叫做 「**Controller 节点**」，而 「**Broker 角色**」叫做 「**Broker 节点**」。

对于 「**Broker 节点**」来说，跟 Zookeeper 模式下的 Broker 基本一致，而 「**Controller 节点**」主要实现以下功能：

1.  Controller 节点之间使用 Raft 算法来实现一个强一致性的分布式存储系统，负责存储 Kafka 中的元数据，**这类似于 Zookeeper 的作用**，这里将 Controller 节点组成的集群成为 Raft 集群。
2.  负责管理 Kafka 集群中的主题、Broker 等。比如需要协同完成创建主题的工作、监控 Broker 节点并进行故障转移等待，**这类似于 KafkaController 节点 + Zookeeper 的作用**。

关于「**kafka KRaft 模块源码**」：

[【服务端 Broker 源码分析系列第三十五篇】图解 Kafka 源码之 KRaft 模块初探实现原理](https://articles.zsxq.com/id_7fp9s8us807i.html)

[【服务端 Broker 源码分析系列第三十六篇】图解 Kafka 源码之 KRaft 请求处理流程](https://articles.zsxq.com/id_eal5tbquenj6.html)

[【服务端 Broker 源码分析系列第三十八篇】图解 Kafka 源码之 KRaft 数据处理机制](https://articles.zsxq.com/id_xx081g35rpve.html)

[【服务端 Broker 源码分析系列第三十九篇】图解 Kafka 源码之 KRaft 元数据主从同步机制](https://articles.zsxq.com/id_lgowqtnbcrbw.html)

[【服务端 Broker 源码分析系列第四十一篇】图解 Kafka 源码之 KRaft 节点监控与故障转移流程](https://articles.zsxq.com/id_k7p341wdzb5a.html)

[【服务端 Broker 源码分析系列第四十二篇】图解 Kafka 源码之 KRaft 数据清理过程](https://articles.zsxq.com/id_zoliu3qksewp.html)