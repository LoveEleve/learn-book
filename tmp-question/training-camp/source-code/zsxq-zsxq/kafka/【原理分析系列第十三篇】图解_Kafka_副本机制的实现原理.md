大家好，我是 **华仔**, 又跟大家见面了。

##   
**01 总体概述**

所谓的副本机制，也称为备份机制，通常是指分布式系统在多台网络互联的机器上保存有相同的数据拷贝。

比如 MySQL 的主从复制、Redis 的主从复制、ES 的副本机制等等，对于一个高可用系统来说，副本机制是必备的功能。

对于 Kafka 系统来说，它提供了数据冗余来实现「**高可用性**」和「**消息高持久性**」。「**副本机制**」是 Kafka 服务端架构设计的核心功能，它是 Kafka 确保系统「**高可用性**」和「**消息高持久性**」的重要基石。

## **02 Kafka 副本机制**

在讲解「**副本机制**」之前，我们先来看看副本的定义是什么？

## **2.1 副本定义**

对于 Kafka 来说，「**副本**」就是一个只能用来追加写消息的提交日志。根据 Kafka 副本机制的定义，同一个分区下的所有副本保存有相同的消息集，这些副本会被分散保存在不同的 Broker 上，从而能够应对部分 Broker 宕机带来的数据不可用。

  
保证集群中的某个节点发生故障时，该节点上的 Partition 数据不丢失，且 Kafka 仍然能够继续工作，提高了系统可用性和数据持久性。

**同一个分区下的所有副本保存相同的消息数据，这些副本分散保存在不同的 Broker 上，保证了 Broker 的整体可用性。**

如下图所示：一个由 3 台 Broker 组成的 Kafka 集群上的副本分布情况。从这张图中，我们可以看到，主题 1 分区 1 的 3 个副本分散在 3 台 Broker 上，其他主题分区的副本也都散落在不同的 Broker 上，从而实现数据冗余。

![](https://article-images.zsxq.com/Fs36s5mbt2Ahq8EDHWr8HglKZjiP)

## **2.2 副本角色**

从第一小节图中可以看出分区下能够配置多个副本，且要保证这些副本内容一致性，那么你很自然想到一个问题：**我们到底该如何保证副本中所有数据一致性呢？**

对 Kafka 来说，当 Producer 发送消息到某个 Topic 后，消息是如何同步到对应的所有副本中的呢？

对于这个问题，Kafka 的做法是采用**基于领导者副本机制（Leader-based）。**我来简单解释一下这张图里面的内容。

![](https://article-images.zsxq.com/FnrHSacKSmqIsx2hZBTd0jazQQTD)

1.  在 Kafka 中，副本分成两类：领导者副本（Leader Replica）和追随者副本（Follower Replica）。每个分区在创建时都要选举一个副本，称为 Leader 副本，其余的副本自动称为 Follower 副本。
2.  Kafka 的副本机制比其他分布式系统要更严格一些。在 Kafka 中， Follower 副本是不对外提供服务的。即任何一个 Follower 副本都不能响应消费者和生产者的读写请求。所有的读写请求都必须发往 Leader 副本所在的 Broker，由该 Broker 负责处理。 Follower 副本不处理客户端请求，它唯一的任务就是从 Leader 副本**异步拉取**消息，并写入到自己的提交日志中，从而实现与 Leader 副本的同步。
3.  当 Leader 副本所在的 Broker 宕机时，Kafka 依托于 ZooKeeper 提供的监控功能能够实时感知到，并立即开启新一轮的 Leader 选举，从 Follower 副本中选一个作为新的 Leader 。老 Leader 副本重启回来后，只能作为 Follower 副本加入到集群中。

对于客户端来说，Kafka 的 Follower 副本没有任何作用，它不能像 MySQL 那样帮助主节点「**分担读压力**」。

既然如此，Kafka 为什么要这样设计呢？其实这种副本机制有以下几个方面的好处。

1.  **方便实现读写一致**：因为只在leader副本上进行读写操作，所以生产者写入什么消息，消费者就能读到什么消息，消费者不会从follower上进行读取操作，避免了主从同步过程中的延迟问题。
2.  **方便实现单调读**：在进行多次消费时，不会存在某条消息一会存在，一会消失的情况。如果 2 个Follower 副本 F1 和 F2，它们异步地拉取 Leader 副本数据。倘若 F1 拉取了 Leader 的最新消息而 F2 还未及时拉取，那么，此时如果有一个消费者先从 F1 读取消息之后又从 F2 拉取消息。
3.  **不提供读写分离功能**：是因为 kakfa 本身的设计是通过分区来进行分担集群读写的压力，从架构层面可以很好的进行水平扩展，提供读写分离是在一定程序软件架构设计的不足，才需要进行读写分离，来分担集群的读写压力；更多的是一种弥补方法。

## **2.3 重要概念区分**

**![](https://article-images.zsxq.com/Fh7LRX_EkiLNzGiPEpACSk6NbMc8)**

1.  **AR 副本集合:** 分区 Partition 中的所有 Replica 组成 AR 副本集合。
2.  **ISR 副本集合:** 所有与 Leader 副本能保持一定程度同步的 Replica 组成 ISR 副本集合， 其中也包括 Leader 副本。
3.  **OSR 副本集合:** 与 Leader 副本同步滞后过多的 Replica 组成 OSR 副本集合。

  
这里我们重点来分析下 **ISR 副本集合。**

## **2.4 ISR 副本集合**

上面强调过，Follower 副本不提供服务，只是定期地异步拉取 Leader 副本中的数据。既然是异步的，就一定会存在不能与 Leader 实时同步的情况出现。

Kafka 为了解决这个问题， 引入了「 **In-sync Replicas**」机制，即 ISR 副本集合。要求 ISR 副本集合中的 Follower 副本都是与 Leader 同步的副本。

**那么，到底什么样的副本能够进入到 ISR 副本集合中呢？**

首先要明确的，Leader 副本天然就在 ISR 副本集合中。也就是说，ISR 不只是有 Follower 副本集合，它必然包括 Leader 副本。另外，能够进入到 ISR 副本集合的 Follower 副本要满足一定的条件。

![](https://article-images.zsxq.com/FmctlUY2LbhKHDlCbNrL1QHqzJ9k)

图中有 3 个副本：1 个 Leader 副本和 2 个 Follower 副本。Leader 副本当前写入了 6 条消息，Follower1 副本同步了其中的 4 条消息，而 Follower2 副本只同步了其中的 3 条消息。那么，对于这 2 个 Follower 副本，你觉得哪个 Follower 副本与 Leader 不同步？

事实上，这2个 Follower 副本都有可能与 Leader 副本同步，但也可能不与 Leader 副本同步，这个完全依赖于 **Broker 端参数 [replica.lag.time.max.ms](http://replica.lag.time.max.ms/) 参数值。**

这个参数是指 **Follower 副本能够落后 Leader 副本的最长时间间隔，当前默认值是 10 秒，从 2.5 版本开始，默认值从 10 秒增加到 30 秒**。即只要一个 Follower 副本落后Leader 副本的时间不连续超过 30 秒，Kafka 就认为该 Follower 副本与 Leader 是同步的，即使 Follower 副本中保存的消息明显少于 Leader 副本中的消息。

此时如果这个副本同步过程的速度持续慢于 Leader 副本的消息写入速度的时候，那么在 [replica.lag.time.max.ms](http://replica.lag.time.max.ms/) 时间后，该 Follower 副本就会被认为与 Leader 副本是不同步的，因此 Kafka 会自动收缩，将其踢出 ISR 副本集合中。后续如果该副本追上了 Leader 副本的进度的话，那么它是能够重新被加回 ISR副本集合的。

在默认情况下，当 Leader 副本发生故障时，只有在 ISR 副本集合中的 Follower 副本才有资格被选举为新Leader，而 OSR 中副本集合的副本是没有机会的（可以通过**[unclean.leader.election.enable](http://unclean.leader.election.enable/)** 进行配置执行脏选举）。

Kafka 的 ISR 副本集合管理最终都会反馈到Zookeeper节点上。具体位置为：**F/brokers/topics/\[topic\]/partition/\[partition\]/state。**目前有以下两个地方会对这个 Zookeeper 的节点进行维护：

1.  **Controller 维护**：Kafka 集群中的其中一个 Broker 会被选举为 Controller，主要负责 Partition 管理和副本状态管理，也会执行类似于重分配 Partition 之类的管理任务。在符合某些特定条件下，Controller 下的 LeaderSelector 会选举新的Leader，ISR 和新的 LeaderEpoch 及 ControllerEpoch 写入 Zookeeper 的相关节点中。同时发起 LeaderAndIsrRequest通知所有的 Replicas。
2.  **Leader 维护**：Leader 有单独的线程定期检测 ISR 中 follower 是否脱离 ISR, 如果发现 ISR 变化，则会将新的 ISR 的信息返回到 Zookeeper 的相关节点中。

**总结：ISR 副本集合是一个动态调整的集合。**

## **03 Kafka UnClean 领导者选举**

既然 ISR 是可以动态调整的，那么自然就可以出现这样的情形：ISR 为空。因为 Leader 副本天然就在 ISR 中，如果 ISR 为空了，就说明 Leader 副本也「**挂掉**」了，Kafka 需要重新选举一个新的 Leader。可是 ISR 是空，此时该怎么选举新 Leader 呢？

**Kafka 把所有不在 ISR 副本集合中的存货副本都称为非同步副本。**通常来说，非同步副本落后 Leader 太多，因此，如果选择这些副本作为新 Leader，就可能出现数据的丢失。毕竟，这些副本中保存的消息远远落后于老 Leader 中的消息。

在 Kafka 中，选举这种副本的过程称为 Unclean 领导者选举。**Broker 端参数** **[unclean.leader.election.enable](http://unclean.leader.election.enable/)** **控制是否允许 Unclean 领导者选举。**

**开启 Unclean 领导者选举**可能会造成数据丢失，但好处是，它使得分区 Leader 副本一直存在，不至于停止对外提供服务，因此提升了高可用性。反之，**禁止 Unclean 领导者选举**的好处在于维护了数据的一致性，避免了消息丢失，但牺牲了高可用性。

如果你听说过 CAP 理论的话，你一定知道，一个分布式系统通常只能同时满足一致性（Consistency）、可用性（Availability）、分区容错性（Partition tolerance）中的两个。显然，在这个问题上，Kafka 赋予你选择 C 或 A 的权利。

你可以根据你的实际业务场景决定是否**开启 Unclean 领导者选举**。不过，我强烈建议你**不要**开启它，毕竟我们还可以通过其他的方式来提升高可用性。

如果只是为了这点儿高可用性的改善，牺牲了数据一致性，那就非常不值当了。

## **04 副本提交**

1.  **同步复制**： 只有所有的 Follower 把数据拿过去后才 Commit，一致性好，可用性不高。
2.  **异步复制**： 只要 Leader拿到数据立即 commit，等 Follower 慢慢去复制，可用性高，立即返回，一致性差一些。
3.  **提交 commit**：是指 Leader 告诉客户端，这条数据写成功了。kafka 尽量保证 commit 后立即 Leader 挂掉，其他 Flower都有该条数据。

**Kafka 不是完全同步，也不是完全异步，是一种 ISR 机制：**

1.  Leader 会维护一个与其基本保持同步的 Replica 列表，该列表称为 ISR(in-sync Replica)，每个 Partition 都会有一个 ISR，而且是由 Leader 动态维护。
2.  如果一个 Follower 比一个 Leader 落后太多，或者超过一定时间未发起数据复制请求，则 Leader 将其从 ISR 中移除
3.  当 ISR 中所有 Replica 都向 Leader 发送 ACK 时，Leader 才 commit。

**既然所有 Replica 都向 Leader 发送 Ack 时，Leader 才会 Commit，那么 Follower 怎么会落后 Leader 太多？**

当 Producer 往 kafka中发送数据，不仅可以一次发送一条数据，而是在生产者底层会有队列缓存起来，批量发送，对应 Broker 来说，就会收到很多数据(假设1000)。

1.  这时候 Leader 发现自己有 1000 条数据。
2.  Follower 只有500 条数据，落后了 500 条数据，就把它从 ISR 中移除出去。
3.  这时候发现其他的 Follower 与它的差距都很小，就会等待。
4.  如果因为内存等原因，差距很大，就把它从ISR中移除出去。

## **4.1 commit 策略配置**

**Server 配置**

rerplica.lag.time.max.ms=10000

\# 如果leader发现flower超过10秒没有向它发起fech请求，那么leader考虑这个flower是不是程序出了点问题

\# 或者资源紧张调度不过来，它太慢了，不希望它拖慢后面的进度，就把它从ISR中移除。

**Topic 配置**

\# 需要保证ISR中至少有多少个replica

min.insync.replicas=1

**Producer 配置**

\# 0:相当于异步的，不需要 Leader 给予回复，Producer 立即返回，发送就是成功,那么发送消息网络超时或broker crash(1. Partition的 Leader 还没有 commit 消息 2. Leader 与 Follower 数据不同步)，既有可能丢失也可能会重发

\# 1：当 Leader 接收到消息之后发送 ack，丢会重发，丢的概率很小

\# -1：当所有的 Follower 都同步消息成功后发送 ack. 丢失消息可能性比较低

request.required.asks=0