大家好，我是 **华仔**, 又跟大家见面了。

今天我们来讲解下 **Kafka Rebalance 流程分析以及调优**，下面是正文。

![](https://article-images.zsxq.com/Fj7ZVi1JKr-gygzFt6w4xT3NhGgW)

## **01 总体概述**

对于 Kafka Consumer 来说，最棘手的事情就是遇到 Rebalance 问题， Rebalance 其实就是让一个 Consumer Group 下所有的 Consumer 成员就如何消费订阅主题的所有分区达成共识的过程。

在 Rebalance 过程中，所有 Consumer 实例共同参与，在协调者组件的帮助下，完成订阅主题分区的分配。但是，在整个过程中，所有实例都不能消费任何消息。

## **02 消费组重分配机制**

接下来，我们来聊聊 Consumer Group 中 Rebalance (重分配) 机制，对于 Consumer Group 来说，可能随时都会有 Consumer 加入或退出，那么 Consumer 列表的变化必定会引起 Partition 的重新分配。我们将这个分配过程叫做 Consumer Rebalance，但是这个分配过程需要借助 Broker 端的 Coordinator 协调者组件，在 Coordinator 的帮助下完成整个消费者组的分区重分配。

## **2.1 Rebalance 触发与通知**

**Rebalance 的触发条件有三种：**

1.  当 Consumer Group 组成员数量发生变化(主动加入或者主动离组，故障下线等)。
2.  当订阅主题数量发生变化。
3.  当订阅主题的分区数发生变化。

**Rebalance 时如何通知其他 consumer 实例呢？**

Rebalance 的通知机制就是靠 Consumer 端的心跳线程，它会定期发送心跳请求到 Broker 端的 Coordinator,当协调者决定开启 Rebalance 后，它会将“[REBALANCE\_IN\_PROGRESS](http://rebalance_in_progress/)”封装进心跳请求的响应中发送给 Consumer ,当 Consumer 发现心跳响应中包含了“[REBALANCE\_IN\_PROGRESS](http://rebalance_in_progress/)”，就知道 Rebalance 开始了。

## **2.2 协议（Protocol）说明**

其实 Rebalance 本质上也是一组协议。Consumer Group 与 Coordinator 共同使用它来完成 Consumer Group 的 Rebalance。下面我看看这5种协议都是什么，完成了什么功能：

1.  Heartbeat请求：Consumer 需要定期给 Coordinator 发送心跳来证明自己还活着。
2.  LeaveGroup请求：主动告诉 Coordinator 要离开 Consumer Group。
3.  SyncGroup请求：Group Leader Consumer 把分配方案告诉组内所有成员。
4.  JoinGroup请求：成员请求加入组。
5.  DescribeGroup请求：显示组的所有信息，包括成员信息，协议名称，分配方案，订阅信息等。通常该请求是给管理员使用。

Coordinator 在 Rebalance 的时候主要用到了前面4种请求。

## **2.3 Consumer Group 状态机**

Rebalance 一旦发生，必定会涉及到 Consumer Group 的状态流转，此时 Kafka 为我们设计了一套完整的状态机机制，来帮助 Broker Coordinator 完成整个重平衡流程。了解整个状态流转过程可以帮助我们深入理解 Consumer Group 的设计原理。

**5 种状态，定义分别如下：**

1.  Empty 状态表示当前组内无成员， 但是可能存在 Consumer Group 已提交的位移数据，且未过期，这种状态只能响应 JoinGroup 请求。
2.  Dead 状态表示组内已经没有任何成员的状态，组内的元数据已经被 Broker Coordinator 移除，这种状态响应各种请求都是一个Response：UNKNOWN\_MEMBER\_ID。
3.  PreparingRebalance 状态表示准备开始新的 Rebalance, 等待组内所有成员重新加入组内。
4.  CompletingRebalance 状态表示组内成员都已经加入成功，正在等待分配方案，旧版本中叫“AwaitingSync”。
5.  Stable 状态表示 Rebalance 已经完成， 组内 Consumer 可以开始消费了。

5种状态流转图如下：

![](https://article-images.zsxq.com/Fqq4IxapG4V4bdlMLg8hIubDpmNN)

## **2.3 Rebalance 能否避免**

在讲解 Rebalance 之前，我们先来了解下什么是 「**协调者**」，它在 Kafka 中叫 Coordinator，它专门为 Consumer Group 服务，负责为 Group 执行 Rebalance 以及提供位移管理和组成员管理等。

简单来说，当 Consumer 提交位移时，就是向「**Coordinator**」所在的「**Broker**」提交位移。同样地，当 Consumer 启动时，也是向 「**Coordinator**」所在的「**Broker**」发送各种请求，然后由「**Coordinator**」负责执行「**消费者组注册**」、「**消费者组成员管理记录**」等元数据管理操作。

所有 Broker 在启动时，都会创建和启动相应的「**Coordinator**」组件。即**所有 Broker 都有各自的 Coordinator 组件**。那么，Consumer Group 如何确定为它服务的 Coordinator 在哪台 Broker 上呢？其实就是存储在 Kafka 内部位移主题 \_\_consumer\_offsets 身上。

Kafka 为某个 Consumer Group 确定 Coordinator 所在的 Broker 的算法有 2 个步骤：

1.  确定由位移主题的哪个分区来保存该 Group 数据：[partitionId=Math.abs(groupId.hashCode() % offsetsTopicPartitionCount](http://partitionId=Math.abs\(groupId.hashCode\(\)%20%%20offsetsTopicPartitionCount\))[)](http://\)/)。
2.  找出该分区 Leader 副本所在的 Broker，该 Broker 即为对应的 Coordinator。

举例说明：

Kafka 会计算该 Consumer Group 的 [group.id](http://group.id/) 参数的哈希值。假如此时你有个 Group 的 [group.id](http://group.id/) 设置成了“test-group”，那么它的 hashCode 值就应该是 627841412。另外 Kafka 会计算 \_\_consumer\_offsets 的分区数，默认通常为 50 个分区，之后将刚才那个哈希值对分区数进行取模加求绝对值计算，即 abs(627841412 % 50) = 12。此时，我们就知道了位移主题的分区 12 负责保存这个 Group 的数据。我们只需要找出位移主题分区 12 的 Leader 副本在哪个 Broker 上就可以了。这个 Broker，就是我们要找的 Coordinator。

这里我们来了解下 Rebalance 的弊端：

1.  **Rebalance 影响 Consumer 端 TPS**：这个之前也反复提到了，这里就不再具体讲了。总之就是，在 Rebalance 期间，Consumer 会停下手头的事情，什么也干不了。
2.  **Rebalance 过程很慢**：如果你的 Group 下成员很多，就一定会有这样的痛点。假如你的 Group 下有几百个 Consumer 实例，Rebalance 一次大概要1~几个小时。在那种场景下，Consumer Group 的 Rebalance 已经完全失控了。

关于第二点，Kafka 社区目前也是无解，我们只能尽量去避免 Rebalance 发生。在笔者的印象中，大部分的 Rebalance 都是不应该的，下面我来看下如何去避免发生。

要避免 Rebalance，还是要从 Rebalance 发生的时机入手，开头说过，Rebalance 发生的时机有三个。而对于 「**当订阅主题数量发生变化**」、「**当订阅主题分区数发生变化**」这两个通常都是运维主动操作的，是我们不可以避免的。

接下来我们看下第一种「**当组成员发生变化**」是如何避免的，我们生产环境碰到的 Rebalance 大部分都是由这种情况引起的。

这里我们主要关注「**组成员减少**」，在某些情况下，Consumer 实例被 Coordinator 错误地认为「**挂掉**」从而被「**踢出**」Group。如果是这个原因导致的 Rebalance，我们就得管了。

那么「**Coordinator**」会在什么情况下认为某个 Consumer 实例已挂要退组呢？接下来我们详细说明下。

当 「**Cosumer Group**」 完成 Rebalance 之后，每个 Consumer 实例都会定期地向「**Coordinator**」发送心跳请求，代表它还存活着。

如果某个 Consumer 实例不能及时地发送这些心跳请求，「**Coordinator**」就会认为该 Consumer 已经挂了，就会将其从「**Cosumer Group**」中移除，然后开启新一轮 Rebalance。在 Consumer 端有个参数叫 [session.timeout.ms](http://session.timeout.ms/)，该参数的默认值是 10 秒，即如果 「**Coordinator**」在 10 秒之内没有收到 「**Cosumer Group**」下某 Consumer 实例的心跳，它就会认为这个 Consumer 实例已经挂了。可以这么说，[session.timout.ms](http://session.timout.ms/) 决定了 Consumer 存活性的时间间隔。

除了这个参数，Consumer 端还为我们提供了一个允许你控制发送心跳请求频率的参数，就是 [heartbeat.interval.ms](http://heartbeat.interval.ms/)。这个值设置得越小，Consumer 实例发送心跳请求的频率就越高。频繁地发送心跳请求会额外消耗带宽资源，但好处是**能够更加快速地知道当前是否要开启 Rebalance**。

前面说过，「**Coordinator**」通知各个 Consumer 实例开启 Rebalance 的方法，就是将 [REBALANCE\_NEEDED](http://rebalance_needed%20/) 标识封装进心跳请求的响应体中。

除了以上两个参数，Consumer 端还有一个参数 [max.poll.interval.ms](http://max.poll.interval.ms/)，用来控制 Consumer 端实际消费能力对 Rebalance 的影响。它限定了 Consumer 端两次调用 poll 拉取方法的最大时间间隔。**其默认值 5 分钟**，**如果你的 Consumer 在 5 分钟内无法消费完 poll 方法返回的消息，那么 Consumer 会主动发起 “离组”请求，Coordinator 也会开启新一轮 Rebalance**。

了解了上面这些参数，我们来确定下哪些 Rebalance 是没必要的。这里主要有两类：

## **2.3.1 未能及时发送心跳时引发**

此时你需要了解如何设置 [session.timeout.ms](http://session.timeout.ms/) 和 [heartbeat.interval.ms](http://heartbeat.interval.ms/) 值。这里我给出一些推荐值，你可以直接用。

1.  设置 [session.timeout.ms](http://session.timeout.ms/) = 6s，主要是为了让 「**Coordinator**」**能够更快地定位已经挂掉地 Consumer**。
2.  设置 [heartbeat.interval.ms](http://heartbeat.interval.ms/) = 2s。
3.  要保证 Consumer 实例在「**挂掉**」之前，能够至少发送 3 轮的心跳请求，即 [session.timeout.ms](http://session.timeout.ms/) >= 3 \* [heartbeat.interval.ms](http://heartbeat.interval.ms/)。

## **2.3.2 Consumer 消费时间过长引发**

在你的业务场景中，Consumer 消费数据时需要将处理结果写入到 ElasticSearch 中。假如此时 ElasticSearch 不稳定就会导致 Consumer 消费时间增加。此时，[max.poll.interval.ms](http://max.poll.interval.ms%20/) 参数值的设置就显得尤为重要了。**如果你要避免非预期的 Rebalance，此时最好将该值设置大一些，也就是说要设置的比你下游最大处理时间稍长一些。**总之，你要为你的业务处理逻辑留下充足的时间。这样，Consumer 就不会因为处理这些消息的时间太长而引发 Rebalance 了。

如果你已经按照上面的参数推荐值恰当地设置好还是出现了「**Rebalance**」，此时你就需要去排查下 「**Consumer 端 Full GC 情况**」，判断是否出现了频繁地 Full GC 导致长时间停顿，从而引发了 「**Rebalance**」。主要在实际场景中，我见过太多因为 GC 设置不合理导致程序频发 Full GC 而引发的非预期 Rebalance 了。

接下来，我们来剖析了 「**Rebalance**」流程。

## **03 Rebalance 流程分析**

接下来我们看看 Rebalance 的流程，通过上面5种状态可以看出，Rebalance 主要分为两个步骤：

加入组(对应JoinGroup请求)。

等待 Leader Consumer 分配方案(SyncGroup 请求)。

1.  JoinGroup 请求: 组内所有成员向 Coordinator 发送 JoinGroup 请求，请求加入组，顺带会上报自己订阅的 Topic，这样 Coordinator 就能收集到所有成员的 JoinGroup 请求和订阅 Topic 信息，Coordinator 就会从这些成员中选择一个担任这个Consumer Group 的 Leader(一般情况下，第一个发送请求的 Consumer 会成为 Leader)，**这里说的 Leader 是指具体的某一个 consumer，它的任务就是收集所有成员的订阅 Toic 信息，然后制定具体的消费分区分配方案。** 待选出 Leader 后，Coordinator 会把 Consumer Group 的订阅 Topic 信息封装进 JoinGroup 请求的 Response 中，然后发给 Leader ，然后由 Leader 统一做出分配方案后，进入到下一步，如下图：

![](https://article-images.zsxq.com/Fg2yHzOQh1ITWRfFcT-ORFzdptUu)

2)、SyncGroup 请求: Leader 开始分配消费方案，**即哪个 Consumer 负责消费哪些 Topic 的 哪些 Parition****。** 一旦完成分配，Leader 会将这个分配方案封装进 SyncGroup 请求中发给 Coordinator ，其他成员也会发 SyncGroup 请求，只是内容为空，待 Coordinator 接收到分配方案之后会把方案封装进 SyncGroup 的 Response 中发给组内各成员, 这样各自就知道应该消费哪些 Partition 了，如下图：

![](https://article-images.zsxq.com/Fhgzc_efDx-vPqwDE2bq2u6obprA)

## **3.1 Rebalance 场景分析**

刚刚详细的聊了关于 Rebalance 的状态流转与流程分析，接下来我们通过时序图来重点分析几个场景来加深对 Rebalance 的理解。

**场景一：新成员（c1****）加入组**

![](https://article-images.zsxq.com/FveBoFv60zwHpaJgtic5ukxVk0Sf)

**场景二：成员（c2****）主动离组**

![](https://article-images.zsxq.com/FhCkR9hg4EA3H80E0tcPzyVMuhxR)

**场景三：成员（c2****）超时被踢出组**

![](https://article-images.zsxq.com/FjH91uyFGXBeGPmpOTOYBSYMPyNn)

**场景三：成员（c2****）提交位移数据**

![](https://article-images.zsxq.com/FjUZwCjH4nHxSGGMgqYfjm4TLma3)

## **04 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头引出了 「**Rebalance**」的概念。

2、带你挨个剖析了「**消费者重分配机制**」的各个方面，主要包括「**Rebalance 触发与通知**」、「**协议说明**」、「**Consumer Group 状态机**」、「**Rebalance 能否避免**」。

3、接着带你深度剖析了「**Rebalance 流程**」进行了 4 种场景分析。