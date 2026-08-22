大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了「**Kafka 消费者组重平衡机制流程**」，了解了消费者组重平衡的「**发生条件**」、「**发生场景**」、「**源码流程分析**」等等。今天我们开启消费端源码的征程，这是第十一篇来深度聊聊「**\_\_consumer\_offsets 探秘**」，看看 Kafka 消费者存储位移 「**\_\_consumer\_offsets**」 的存储结构是怎样的。

![](https://article-images.zsxq.com/Fr0HQWZq0MSWEMN8m-7TwBsFkb9z)

## **01 总体概述**

在原理篇 [【原理分析系列第五篇】图解 Kafka Consumer 架构设计](https://articles.zsxq.com/id_xqh2l0kkhc7a.html) 中，我们知道 Consumer 消费完数据后需要进行「**位移提交**」， 那么提交的位移数据究竟存储在哪里， 又是以何种方式进行存储的呢？

我们知道 Kafka 旧版本（0.8版本之前）是重度依赖 Zookeeper 来实现各种各样的协调管理，当然旧版本的 Consumer Group 是把位移保存在 ZooKeeper 中，减少 Broker 端状态存储开销，鉴于 Zookeeper 的存储架构设计来说， 它「**不适合写更新**」，而 Consumer Group 的位移提交又是高频写操作，这样会拖慢 ZooKeeper 集群的性能， 于是在新版 Kafka 中， 社区重新设计了 Consumer Group 的位移管理方式，采用了将位移保存在 Kafka 内部（这主要是因为 Kafka Topic 天然支持高频写且持久化），这就是所谓大名鼎鼎的 「**\_\_consumer\_offsets**」。

「**\_\_consumer\_offsets**」用来保存 Kafka Consumer 提交的位移信息，另外它是由 Kafka 自动创建的，和普通的 Topic 相同，它的消息格式也是 Kafka 自己定义的，我们无法进行修改。这里我们很好奇它的消息格式究竟是怎么样的，接下来就让我们来一起分析并揭开它的神秘面纱吧。

**本篇内容来自星球嘉宾-彦祖的文章，下面是正文**。

## **02 存储结构**

在之前的文章中, 我们有讲解过消息是如何从 Producer 存储到 Broker 中的, 也分析了消息的结构, 如果不清楚的话,可以看看 [图解Kafka的RecordBatch结构](https://mp.weixin.qq.com/s?__biz=Mzg4ODY1NTcxNg==&mid=2247493850&idx=1&sn=bcfffcb5094046cf94ad08cc7a6e6f34&chksm=cff572d1f882fbc77280528eaf3cad51a337e158fcc50ad2467717795286a5434540fb1977f3&scene=21#wechat_redirect)

![](https://article-images.zsxq.com/Fp15xqRtH5i96DkRq5NlUgjNMK-D)

**那么作为消费者，消费了某条消息之后又是如何记录我已经消费了这条消息呢？如果消费者重启了，那么又是如何获知我上次消费了哪个消息呢？**

既然要解决上述的问题, 那么我们肯定要把我们已经消费的消息给记录下来, 不可能把整个 Record 给记录下来吧,那也太大了, 但是我们知道消息存储在具体的分区的时候都是有自己的 Offset 的, 并且是有序的 offset, 所以我们只需要记录下当前消费到了哪个 Offset 就行了。

**这个 Offset 保存在哪里呢？**

在 0.9 版本之前，这个信息是记录在 zookeeper 内的。但是因为频繁的对 zookeeper 进行读写，压力太大集群不稳，所以在 0.9 之后的版本，offset 保存在「**\_\_consumer\_offsets**」这个内部的 Topic 内了。

**那么这个是怎么存储的呢？**

因为本质上还是把消费的 offset 当做一条普通的消息发送到内部的 Topic「**\_\_consumer\_offsets**」中, 所以存储数据的时候, 也就是存到 Record 消息结构的 Key-Value 中。

根据版本的迭代，存储的 Key-Value 的 Schema 也有一些略微不一样, 接下来我会把每个版本的 Schema 都讲解一下。

值得一提的是, 这个 「**\_\_consumer\_offsets**」 保存的并不仅仅是 offset 的信息，实际上它分为两种类型的数据。

1.  一种是 GROUP\_METADATA：这种类型的数据，存放的是消费组相关的元数据信息
2.  一种是 OFFSET\_COMMIT：这种类型的数据，存放的是就是上面说的Offset消费偏移量数据

![](https://article-images.zsxq.com/Fta_kuWBg0NiNZvVb5ao46JwubqD)

接下来我们看看他们的数据结构。

我们下面要说的存储结构说的是 Record 数据结构里面的 Key-Value。

![](https://article-images.zsxq.com/FnHaFRFqGeVlCJx4WGhAKw00L_Ik)

## **2.1 GROUP\_METADATA 消费者组元信息**

GROUP\_METADATA 存储的是消费组的元信息, 比如 groupId 、protocol\_type、protocol、leader、members 等等。

比如说有很多的消费组  ， 每个消费组有很多消费者客户端member，每个member的 [client.id](http://client.id/)、[clien.host](http://clien.host/)等等信息。

这个消息的Key  Value数据如下

## **2.1.1 GROUP\_METADATA\_KEY**

Key的结构

![](https://article-images.zsxq.com/FvIsz0ozefd0m0YR8z7j_PlWINDw)

GroupMetadataKey结构

这里的 GroupMetadataKey 的最开始的 2 个字节存储的是 version 数据。

1.  version=2 表示的是这个消息类型就是 GROUP\_METADATA 类型
2.  version=0或1 表示的是 OFFSET\_COMMIT 类型。

除了 version，后面的数据才是 Schema, 这个Schema只有一个字段就是 group，这里的key就是 group.id 。

> 注意: 在数据结构上 version 并不是属于Schema内的。

## **2.1.2 GROUP\_METADATA\_VALUE**

GroupMetaDataValue 的数据结构变更的有点多，有好几次升级，版本信息存放在 version 里面,并且占用 2 个字节。

> 特别注意的是: version 跟其他的并不属于同一个 Schema 里面。

### **2.1.2.1 Version 0 -- 基础版本，也是所有版本的共有属性**

这个是最原始的版本，后续的版本都是在这个基础上做加法。

Kafka版本：kafka version  < 0.10.1 Schema：

![](https://article-images.zsxq.com/FsByNunFMnz0gSSf98y-8PcgWo0z)

这里是每个结构的共有属性， 后面每个版本讲解的时候只会讲差异

![](https://article-images.zsxq.com/Fu_-Okf2VDly99IiNN3LQOpjXA5x)

Member结构

![](https://article-images.zsxq.com/FuLhih4bbWh3Q_VktseQdxHWiThl)

### **SyncGroupRequest 请求参数**

assignment 属性的结构看下图虚线部分的 assignment：

private List<TopicPartition> partitions;

private ByteBuffer userData;

![](https://article-images.zsxq.com/FlAAmnDnJYnnta7IjOGhXSuDJ3fu)

### **JoinGroupRequest 请求参数**

subscription 属性的结构看下图红线框起来的部分 metadata：

![](https://article-images.zsxq.com/FlTqtEjJaRTLVbZuOYAhv9sv8jdx)

## **2.1.2.2 Version 1**

Kafka版本：0.10.1 < kafka version < 2.1 Schema：

![](https://article-images.zsxq.com/Fk0HJmo2NDdQGXHxMYjmZi31LZW-)

相比于version = 0 的版本，在 member 里面新增了一个字段 rebalance\_timeout。

rebalance\_timeout 重平衡的超时时间，由配置 [max.poll.interval.ms](http://max.poll.interval.ms/) 控制，默认值 300000（5 分钟），如果消费者两次 poll 的时间超过了此值，那就认为此消费者能力不足，将此消费者的 commit 标记为失败，并将此消费者从group移除，触发一次reblance,将该消费者消费的分区分配给其他人。

## **2.1.2.2 Version 2**

Kafka版本：2.1 < kafka version <2.3 Schema：

![](https://article-images.zsxq.com/Fr8swK_OpkFRC2LucD3ZEMNXvFYe)

跟V1的版本区别是 新增了一个字段 current\_state\_timestamp。

current\_state\_timestamp 当前 Group 状态流转的时间,从上一个状态流转到下一个状态的这个时间点。

## **2.1.2.3 Version3**

Kafka版本：2.3 < kafka version Schema：

![](https://article-images.zsxq.com/Fq0w1z-Na6XRpFLjZuNcj3A1i1CQ)

跟 V2 的版本区别是 member 里面新增了一个字段：

![](https://article-images.zsxq.com/FqfWQXwVhAAcwJaxKLBqiSoFktlz)

## **2.2 OFFSET\_COMMIT 消费偏移量信息**

这种类型的数据就是跟存储的每个消费组每个分区消费的 offset 的数据。

## **2.2.1 OFFSET\_COMMIT\_KEY**

![](https://article-images.zsxq.com/Fplb-vBoeif-VOCu91IwzXklLdir)

1.  version=0或1 表示的是OFFSET\_COMMIT类型。
2.  version=2 表示的是上面提过的GROUP\_METADATA类型

![](https://article-images.zsxq.com/Fj1koB_o70fvWfjGMc4oeBPCX-eH)

因为可能会有很多个不同的消费组消费同一个分区, 并且他们直接是相互独立的, 所以为了区别他们,在存储消费组offset的时候, key的值实际上是 由[group.id+topic+partition](http://group.id+topic+partition/) 组成的。

并且一个分区在同一个消费组内只会被其中一个消费者消费, 这样就不会出现多个消费者重复消费的问题了。

## **2.2.2 OFFSET\_COMMIT\_VALUE**

Value 是存储的消费组的消费偏移量 Offset，这个 Offset 是针对每个 [group.id+topic+partition](http://group.id+topic+partition/) 维度的。

除了 Offset 信息，还存了其他的一些信息，请看下图 Value 结构的变更历史。

![](https://article-images.zsxq.com/FvxUMqOO4Tr2fyelukhzMI3N_SKC)

我们主要看 Version=3 的一个版本。

![](https://article-images.zsxq.com/FsDFcGIiFH-OtKUYYa-zOJ2bsU0v)

## **03 写入时机**

## **3.1 Group 元信息写入时机**

元信息的写入源码入口在 [GroupMetadataManager#storeGroup](http://groupmetadatamanager/#storeGroup)

/\*\*

\* 公众号：石臻臻的杂货铺

\* Vx: shiyanzu001

\* 领取20万字《Kafka运维实战宝典》

\*\*/

def storeGroup(group: GroupMetadata,

                 groupAssignment: Map\[String, Array\[Byte\]\],

                 responseCallback: Errors => Unit): Unit = {

//读取分区数据,获取消息的结构版本             

    getMagic(partitionFor(group.groupId)) match {

case Some(magicValue) =>

// We always use CREATE\_TIME, like the producer. The conversion to LOG\_APPEND\_TIME (if necessary) happens automatically.

val timestampType \= TimestampType.CREATE\_TIME

val timestamp \= time.milliseconds()

// 获取Group元信息的Key结构

val key \= GroupMetadataManager.groupMetadataKey(group.groupId)

// 获取Group元信息的Value结构

val value \= GroupMetadataManager.groupMetadataValue(group, groupAssignment, interBrokerProtocolVersion)

//构造消息体

val records \= {

val buffer \= ByteBuffer.allocate(AbstractRecords.estimateSizeInBytes(magicValue, compressionType,

            Seq(new SimpleRecord(timestamp, key, value)).asJava))

val builder \= MemoryRecords.builder(buffer, magicValue, compressionType, timestampType, 0L)

          builder.append(timestamp, key, value)

          builder.build()

        }

//构造TopicPartition，消息将会发往这个分区

val groupMetadataPartition \= new TopicPartition(Topic.GROUP\_METADATA\_TOPIC\_NAME, partitionFor(group.groupId))

val groupMetadataRecords \= Map(groupMetadataPartition -> records)

val generationId \= group.generationId

//当日志写入成功之和调用这个回调函数，并将 创建的group插入缓存

        def putCacheCallback(responseStatus: Map\[TopicPartition, PartitionResponse\]): Unit = {

// 省略很多代码..

          }

          responseCallback(responseError)

        }

//将数据写入到Log中

        appendForGroup(group, groupMetadataRecords, putCacheCallback)

case None \=\>

        responseCallback(Errors.NOT\_COORDINATOR)

        None

    }

}

这段代码很简单, 就是构造一下 GroupMetadata 的消息体并写入 \_\_consumer\_offset 对应的分区中。我们主要还是要来了解一下在什么情况下才会写入。我们看调用链发现有三个地方调用了它。

## **3.1.1 onCompleteJoin**

当所有的Member都成功Join之后调用，这个的执行条件是: 当 Group 当前是 Empty 的状态，一个 Member 也没有，那么这个时候写入一个 Group 元信息**。**

[GroupCoordinator#onCompleteJoin](http://groupcoordinator/#onCompleteJoin)

![](https://article-images.zsxq.com/FqR18bABtcv2XLFP8lAZtEtJy2VE)

## **3.1.2 doSyncGroup**

当 Leader Member 接收到 SyncGroup 请求。一般 SyncGroup 请求都是在 JoinGroup 完成了之后，所有的Member 都会向组协调器发送 SyncGroup。但是 Leader Member 会带上计算好的新的分区分配方案给协调器

所以协调器当发现是 Leader Member 给自己发送 SyncGroup 请求之后,他会把相应的元数据给写入到\_consumer\_offset中。(因为Group的元信息有变更了)。

[GroupCoordinator#doSyncGroup](http://groupcoordinator/#doSyncGroup)

![](https://article-images.zsxq.com/Fmd6HTWjjb19PAJCiJJjY0VqdYiS)

上面的一个入参 assignments 列表, 是 SyncGroup 请求带进来的,具体的结构请看下图右边虚线框。

![](https://article-images.zsxq.com/FlAAmnDnJYnnta7IjOGhXSuDJ3fu)

上图虚线内的单个assignment的数据结构是：

private List<TopicPartition> partitions;

private ByteBuffer userData;

想了解 SyncGroupRequest 请看：KafkaConsumer SyncGroupRequest 详解

### **3.1.3 updateStaticMemberAndRebalance 更新静态成员**

在静态成员 JoinGroup 的时候, 会走单独的静态成员更新流程。

[GroupCoordinator#updateStaticMemberAndRebalance](http://groupcoordinator/#updateStaticMemberAndRebalance)，判断 group 的下一代的 selectedProtocol (所有memmber 都支持并排序最靠前)是否会改变。

1.  如果有变化，应该触发 rebalance，让 group 的 assignment 和 selectProtocol 一致
2.  如果没有，则简单地存储 group 来持久化更新的静态成员**。**

就是这里的 2 的情况,去更新一下静态成员的相关信息，主要就是更新 memberId，其他的都没有什么好变更的。分配方案也不会有改变。

### **3.1.4 GROUP\_METADATA\_VALUE\_SCHEMA 版本选择**

[GroupMetadataManager#groupMetadataValue](http://groupmetadatamanager/#groupMetadataValue)

def groupMetadataValue(groupMetadata: GroupMetadata,

assignment: Map\[String, Array\[Byte\]\],

apiVersion: ApiVersion): Array\[Byte\] = {

val version \=

if (apiVersion < KAFKA\_0\_10\_1\_IV0) 0.toShort

else if (apiVersion < KAFKA\_2\_1\_IV0) 1.toShort

else if (apiVersion < KAFKA\_2\_3\_IV0) 2.toShort

else 3.toShort

MessageUtil.toVersionPrefixedBytes(version, new GroupMetadataValue()

.setProtocolType(groupMetadata.protocolType.getOrElse(""))

.setGeneration(groupMetadata.generationId)

.setProtocol(groupMetadata.protocolName.orNull)

.setLeader(groupMetadata.leaderOrNull)

.setCurrentStateTimestamp(groupMetadata.currentStateTimestampOrDefault)

.setMembers(groupMetadata.allMemberMetadata.map { memberMetadata =>

new GroupMetadataValue.MemberMetadata()

.setMemberId(memberMetadata.memberId)

.setClientId(memberMetadata.clientId)

.setClientHost(memberMetadata.clientHost)

.setSessionTimeout(memberMetadata.sessionTimeoutMs)

.setRebalanceTimeout(memberMetadata.rebalanceTimeoutMs)

.setGroupInstanceId(memberMetadata.groupInstanceId.orNull)

// The group is non-empty, so the current protocol must be defined

.setSubscription(groupMetadata.protocolName.map(memberMetadata.metadata)

.getOrElse(throw new IllegalStateException("Attempted to write non-empty group metadata with no defined protocol.")))

.setAssignment(assignment.getOrElse(memberMetadata.memberId,

throw new IllegalStateException(s"Attempted to write member ${memberMetadata.memberId} of group ${groupMetadata.groupId} with no assignment.")))

}.asJava))

}

原文连接：[https://mp.weixin.qq.com/s/mxTjj3wTXxdOKwEIP3j-7Q](https://mp.weixin.qq.com/s/mxTjj3wTXxdOKwEIP3j-7Q)