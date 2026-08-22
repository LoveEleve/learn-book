大家好，我是 **华仔**, 又跟大家见面了。

上篇主要带大家深度剖析了 「**Kafka 服务端****事务消息提交处理流程**」，了解了 Kafka 中「**事务消息**」的是如何处理事务提交的，从今天开始，我们接着来深度剖析 Kafka「**Broker**」的底层源码实现，我们接着来深度聊聊「**Kafka 服务端 KRaft 模块初探**」，看看 Kafka Kraft 算法是什么，内部的 Leader 选举、日志复制的流程是怎么样的。

![](https://article-images.zsxq.com/FgpsNdnwcKAV7EnWsJ-u2GBlmcEp)

## **01 总体概述**

从 Kafka 2.8 版本开始，Kafka 尝试移除 Zookeeper，并提供了 KRaft 模块来代替 Zookeeper，从而实现不需要 Zookeeper 的情况下单独部署 Kafka 集群。

这里基于 Kafka 3.0 版本来分析 KRaft 模块的设计与实现。

接下来会剖析 「**KRaft 模块部署**」、「**KRaft 中的 Raft 算法**」等。

## **02 为什么要移除 Zookeeper**

主要有两方面的的考虑：

1.  使用 Zookeeper 模式来部署、管理 Kafka 相对比较复杂。Kafka 使用 Zookeeper 来存储 「**Broker**」、「**主题**」、「**分区**」等元数据，并使用 Zookeeper 来协助 Kafka 集群完成 Broker Leader 选举等操作。虽然这样简化了 Kafka 的管理，但是也让 Kafka 的部署和运维更加复杂了。当使用 Zookeeper 模式时，需要同时部署和管理 Kafka、Zookeeper 两套分布式系统，综合运维成本、要求较高。
2.  Zookeeper 本身的一些问题会对 Kafka 造成影响。比如当出现大量分区时，Zookeeper 可能会导致性能问题，然后 Kafka 团队无法解决和优化 Zookeeper 的这些痛点问题。

引入 KRaft 模块后，优点如下：

1.  **部署更简单**：只需要部署 Kafka 集群，使 Kafka 更加简单、轻量级，此时只需要维护 Kafka 集群即可。
2.  **监控更便捷**：此时 Kafka 的元数据已经交由 KRaft 模块来管理了，所以从 Kafka 获取监控信息更加可控便捷。
3.  **性能更强大**：使用了高性能的 Raft 算法来实现 KRaft 模块，并针对 Kafka 使用场景进行了性能优化。

## **03 部署 KRaft**

下面我们来看下如何通过 KRaft 模块来部署 Kafka 集群，这里使用 3 台机器/ 3 个容器来部署 3 个 Kafka 节点，版本为 Kafka 3.0.0。

## **3.1 修改配置文件**

在 Kafka 中 KRaft 模块的配置文件路径为：[config/kraft/broker.properties](http://config/kraft/broker.properties)，该配置文件只能用在单个 Kafka 节点，如果要部署 Kafka 集群，则需要修改配置文件。

process.roles=broker,controller

node.id=1

listeners=PLAINTEXT://192.168.56.1:9092,CONTROLLER://192.168.56.1:9092

advertised.listeners=PLAINTEXT://192.168.56.1:9092

inter.broker.listener.name=PLAINTEXT

controller.listener.name=CONTROLLER

controller.quorum.voters=1@192.168.56.1:9093,2@192.168.56.2:9093,3@192.168.56.3:9093

3 台节点都需要做配置。

下面来解释下这些配置参数的含义：

1.  process.roles：该参数指定了该节点的角色，如下：
2.  broker：表示这台机器仅仅作为一个 Broker 节点，负责为客户端提供服务。
3.  controller：作为 Raft Quorum 的 Controller 节点，负责完成 Kafka 集群元数据管理、 Raft 集群选主等。
4.  broker，controller：拥有上面两者都包含的功能。
5.  listeners：配置 broker 角色和 Contoller 角色对应服务绑定的网络地址和端口。
6.  node.id：当前节点id，一个集群中不同节点的 node.id 不能一样。
7.  controller.quorum.voters：集群中所有的 Controller 节点，配置格式为 [<nodeId>@<ip>:<port>](http://nodeId@ip:port)。

## **3.2 生成 ClusterId 以及 meta 文件**

1.  使用 [Kafka-storage.sh](http://kafka-storage.sh/) 来生成 [ClusterId](http://clusterid/)，命令如下：

./bin/kafka-storage.sh random-uuid

zXBF9RACGSg6cQdPqpRF-G

1.  使用第一步生成的 ClusterId 在集群的三台机器上生成对应的 meta 文件，命令如下：

./bin/kafka-storage.sh format -t zXBF9RACGSg6cQdPqpRF-G -c ./config/kraft/server.properties

Formatting /tmp/kraft-combined-logs

至此，可以看到在 Kafka 的 Log 目录 [/tmp/kraft-combined-logs](http://tmp/kraft-combined-logs) 下生成了 meta.properties，如下：

cluster.id=zXBF9RACGSg6cQdPqpRF-G

version=1

node.id=1

> 这里我们也可以自己创建 Kafka 的 Log 目录下创建该 meta 文件，写入上面内容。

## **3.3 启动 Kafka 服务**

我们就可以启动服务了，跟普通启动一样，使用 kafka-server-start.sh 脚本来启动 Kafka 节点。

./bin/kafka-server-start.sh ./config/kraft/server.properties

至此，Kafka 集群就部署成功了。

## **04 Raft 算法初探**

Raft 算法使现在非常流行的分布式一致性算法，主要用来保证分布式系统中数据的一致性。首先 Raft 算法会选举 一个「**Leader 节点**」，其余的节点称为「**Follower** **节点**」。由 「**Leader 节点**」来负责处理所有写请求，「**Leader 节点**」会将自己收到的写请求广播给集群内的其他「**Follower 节点**」，从而保证集群内数据一致。

通过 「**Leader 节点**」，Raft 算法将一致性问题简化为以下 3 个问题来拆解。

1.  **Leader 选举**：集群刚启动或者当前 Leader 节点下线时，需要重新选举一个新的 Leader 节点。
2.  **日志复制**：Leader 节点将自己收到的写请求记录到日志中，并将日志广播给其他节点，从而保证集群数据一致性。
3.  **数据安全**：如果某个日志已经被集群提交，那么该日志不能被修改或者覆盖。Raft 算法通过日志存储所有的修改操作，保证了已提交日志的安全性，保证这些日志对应的修改操作不会被篡改。

接下来我们详细剖析下 Raft 算法的实现原理。

Raft 算法里面定义了 Term 任期的概念，它会将时间分为一个个 Term，可以认为是逻辑上的时间。每一任期开始时都需要选举 Leader 节点，该节点将在该任期内一直完成 Leader 节点的工作。如果 Leader 节点故障下线，则需要开启一个新的任期，并选举新的 Leader 节点。

> 这里的任期跟前面我们学过的 LeaderEpoch 机制中的 Epoch 概念非常相似的。

在 Raft 算法中，每个节点都需要维护以下属性：

1.  **currentTerm**：服务端当前任期，从 0 开始单调递增。
2.  **votedFor**：当前任期获得该节点选票的 candidate 节点的 id，如果当前节点没有投给任何 candidate 节点，则为空，用来 Leader 节点选举。
3.  **log\[\]**：本地日志集，每个条目包括日志索引、操作内容，Leader 节点收到该日志时的任期。
4.  **commitIndex**：已提交的最后一条日志条目的索引，从 0 开始单调递增。
5.  **lastApplied**：已执行真正的修改操作的最后一条日志条目索引，从 0 开始单调递增。

## **4.1 Leader 选举**

Raft 算法中的每个节点都可以有 3 种状态：「**Leader**」、「**Follower**」、「**Candidate**」。正常情况下，集群种存在一个「**Leader 节点**」和 0 到多个 「**Follower 节点**」。

「**Leader 节点**」会定时向所有的「**Follower 节点**」发送「**心跳请求**」用来维护 Leader 地位或同步数据。如果某个 「**Follower 节点**」超过指定选举时间没有收到 「**Leader 节点**」的心跳，那么就认为 Leader 节点已下线，并转化为「**Candidate 节点**」，然后发起选举流程。

集群刚启动时，每个节点都是「**Follower 节点**」，直到某个节点转化为「**Candidate 节点**」，发起选举流程，如下：

1.  增加自己的 Term，更新 [currentTerm = currentTerm + 1](http://currentterm%20=%20currentterm%20+%201/)。
2.  给自己投票。
3.  向集群其他节点发送投票请求，要求它们给自己投票。

投票请求内容如下：

1.  **term**：发送节点的当前任期号。
2.  **candidateId**：发送节点 Id。
3.  **lastLogIndex**：发送节点最后一条日志条目的索引值。
4.  **lastLogTerm**：发送节点最后一条日志条目的任期号。

当其他节点收到该投票请求报文后，流程如下：

1.  如果 [request.term](http://request.term%20/) 「**请求数据**」< [receiver.currentTerm](http://receiver.currentterm/)「**接收节点的数据**」，则拒绝投票。
2.  如果 [request.lastLogTerm](http://request.lastlogterm/) < [receiver](http://receiver.currentterm/)[.lastLogTerm](http://.clastlogterm/)「**接收节点最后一条日志条目的任期号**」或者 [request.lastLog](http://request.lastlogterm/)[Index](http://index/) < [receiver](http://receiver.currentterm/)[.lastLog](http://.clastlogterm/)[Index](http://index/)「**接收节点最后一条日志条目的索引值**」，则拒绝投票。
3.  如果 [request.term](http://request.term/) 「**请求数据**」== [receiver.currentTerm](http://receiver.currentterm/)「**接收节点的数据**」，且接收节点 votedFor 属性不为空，则表示该任期内接收节点已经给其他节点投过票了，则拒绝投票。
4.  此时就可以给请求节点进行投票，更新 [receiver.currentTerm](http://receiver.currentterm/) = [equest.term](http://request.term/)，重置选举超时时间，并返回投票响应。

当出现以下情况时，「**Candidate 节点**」会转化为其他状态：

1.  当超过半数节点给自己投票，也包括自己的投票，那么此时当前节点当选为「**Leader 节点**」。由于内部使用了 「**Quorum 机制**」，因此 Raft 算法保证如果某次选举成功，那么只能选举出一个唯一的 「**Leader 节点**」。
2.  当收到其他节点的心跳请求报文，并且任期不小于当前任期，则表示其他节点已成为「**Leader 节点**」，当前节点转化为 「**Follower 节点**」。
3.  等待一段时间，直到超过选举超时时间仍没有选举或者收到其他「**Leader 节点**」的心跳请求报文，则开始新一轮选举。

下面通过图解来剖析 Raft 算法的选举流程。

此时，「 **N1 节点**」将最新的日志复制（lastLogIndex = 12）给「 **N2 节点**」后就故障下线了，其中 「 **N1**」为 **Leader 节点**。之后，「 **N5 节点**」开始发起选举流程，如下图：

![](https://article-images.zsxq.com/lj5lGFFbw2koMJbuhlMzvEny6aF1)

当 「 **N5 节点**」发起投票其他节点回应之后，「 **N5 节点**」获得 3 票（加上自己的一票），即满足大多数节点，最终成为了 「 **Leader 节点**」，此时 「 **N5 节点**」需要定时发送心跳请求报文来维持自己的 Leader 角色。

![](https://article-images.zsxq.com/lgZW5j173R37KD7VkU3IIxIhHtPG)

如果在一个任期内同时有多个节点发起选举时，则该任期的选票可能会被多个节点瓜分，导致没有一个节点可以成为「**Leader 节点**」的，最终都投票失败。

![](https://article-images.zsxq.com/lscK5vIKzB-go7_-5WV32fuWE65d)

![](https://article-images.zsxq.com/liOHlvvBgVbBsQcHch3ZiKByPIoH)

同图上可以看出，「 **N2 节点**」、「 **N5 节点**」同时发起选举流程，最终「 **N2 节点**」、「 **N5 节点**」都只收到两个投票（包括自己的投票），最终本轮投票没有选举出「**Leader 节点**」。

为了避免这种情况发生，Raft 算法要求每个节点都在一个固定的时间「**比如 150~300 ms**」内选择一个随机时间作为「**选举超时时间**」。

当「**Leader 节点**」故障下线后，最先超时的节点会先发起选举流程，此时它通常会获取多数选票。然后该节点赢得选举并在其他节点选举超时之前发送心跳请求报文，从而成为「**Leader 节点**」。

## **4.2 日志复制**

当「**Leader 节点**」被选举出来后，就开始为对外提供服务了。它将每个写入请求记录为一条新的日志条目并追加到本地日志集中，然后通过心跳请求报文发送给集群中的其他 「**Follower 节点**」，让它们复制这条日志条目。

当集群中超过半数节点，包括 「**Leader 节点**」在内都存储了该日志后，此时「**Leader 节点**」在收到其他节点存储日志条目成功的确认信息后就认为该日志已经安全了，就会将它提交到「**状态机**」并执行真正的修改操作，最后把操作结果返回客户端。

「**Leader 节点**」会保证所有的「**Follower 节点**」都存储了全部的日志条目，如果中途有「**网络丢包**」、「**Follower 节点故障**」等情况，「**Leader 节点**」会重复发送日志条目。

接下看，我们来看下心跳请求报文，它是由 「**Leader 节点**」发送的，负责维持 「**Leader 节点**」地位或者发送日志条目的，内容如下：

1.  **term**：Leader 节点的任期。
2.  **leaderId**：Leader 节点的 Id，用来 Follower 节点通知客户端进行重定向。
3.  **prevLogIndex**：新日志条目前一条日志条目的索引。
4.  **prevLogTerm**：新日志条目前一条日志条目的任期。
5.  **entries\[\]**：需要 Follower 节点报错的新日日志条目。
6.  **prevLogTerm**：Leader 节点中已提交的最后一条日志条目的索引。

「**Follower 节点**」收到心跳请求报文后，流程如下：

1.  如果 [request.term](http://request.term/) 「**请求数据**」< [local.currentTerm](http://receiver.currentterm/)「**本地节点的当前数据**」，则该请求为无效请求，拒绝处理。
2.  执行日志一致性检查操作，如果不通过，则拒绝接收请求中的新日志。
3.  将请求中的新日志条目添加到本地日志集中并返回结果。如果本地已经存在的日志条目和请求中的新日志条目发生冲突（比如：索引值相同但是任期号不同），则删除本地日志集中该索引以及后续所有日志条目，再将请求中的新日志条目添加到本地日志集中。

### **4.2.1 日志一致性检测**

由于是分布式系统，或多或少会出现网络不稳定或者节点运行缓慢的情况，此时「**Follower 节点**」的日志可能会与 「**Leader 节点**」的不一致，如下图：

![](https://article-images.zsxq.com/lkqSnL4uKkFpw326kXEfaLQC7joa)

对于日志不一致的 「**Follower 节点**」，「**Leader 节点**」需要将自己的日志复制给它们。「**Leader 节点**」会给每个 「**Follower 节点**」记录一个「**nextIndex**」，代表「**Follower 节点**」在该索引位置的日志与 「**Leader 节点**」不一致，并将该索引位置的日志作为下一次心跳请求报文中发生的日志条目。

那么如何来确定这个「**nextIndex**」呢？当一个节点刚成为 Leader 节点时，它会将所有的 「**Follower 节点**」的 「**nextIndex**」值初始化为自己最小的日志索引 + 1。

在发送心跳请求报文时，「**Leader 节点**」会给每个「**Follower 节点**」发送其 「**nextIndex**」索引的日志条目，并将 「**nextIndex**」 索引的前一条日志的索引「**prevLogIndex**」和任期「**prevLogTerm**」等信息存放在请求报文中。

如果「**Follower 节点**」在它的日志集中找不到包含相同的 「**prevLogIndex**」、「**prevLogTerm**」日志条目，就会拒绝心跳请求报文中的日志条目，此时 「**Leader 节点**」会将该 「**Follower 节点**」的「**nextIndex**」-1 并重新发送心跳请求报文。直到找到合适的 「**nextIndex**」索引为止。

综上，通过日志一致性检测，Raft 算法可以保证如下：

1.  如果在不同节点中，两个日志条目具有相同的索引和任期号时，则它们的内容一定相同。
2.  如果在不同节点中，两个日志条目具有相同的索引和任期号时，则它们之前的所有内容也一定相同。

### **4.2.2 日志覆盖**

由于各种原因，「**Leader 节点**」会发生变更，此时可能会导致某些 「**Follower 节点**」中出现冲突的日志条目，这些日志条目需要被覆盖掉，如下图所示：

![](https://article-images.zsxq.com/lkDLrqc2sagCoeC71Lz6r4DGt-IG)

> 这里的日志覆盖类似主从同步过程中的日志截断操作

### **4.2.3 日志提交**

Raft 算法通过「**CommitIndex**」索引维护「**状态机**」已提交的日志索引。当「**Leader 节点**」收到多数截断存储某个日志的成功响应后，将提交日志给「**Leader 节点**」，流程如下：

1.  「**Leader 节点**」修改「**CommitIndex**」索引。
2.  「**Leader 节点**」执行真正的修改操作并修改 「**LastApplied**」索引。
3.  「**Leader 节点**」通过心跳请求报文将最小的 「**CommitIndex**」发送给其他「**Follower**」节点。

当「**Follower**」收到新的「**CommitIndex**」索引后，流程如下：

1.  如果 [request.leaderCommit](http://request.leadercommit/) > [receiver.comitIndex](http://receiver.comitindex/)，则将 [request.leaderCommit](http://request.leadercommit/) 和新日志条目索引中较小值赋值给 [receiver.comitIndex](http://receiver.comitindex/)。
2.  如果接收节点中 [commitIndex](http://commitindex/) > [lastApplied](http://lastapplied/)，则将 [lastApplied +1](http://lastapplied%20+1/)，并执行 [lastApplied](http://lastapplied/) 位置的日志操作，直到 [lastApplied](http://lastapplied/) == [commitIndex](http://commitindex/)。

## **4.3 日志安全性**

Raft 算法保证已提交的日志条目不会被覆盖或者篡改，通过以下几点来保证：

### **4.3.1 投票限制**

在投票选举中，如果某个节点不包含所有「**已提交**」的日志，则不能被选举为 「**Leader 节点**」。此限制保证了最小提交的日志条目不会在新的「**Leader 节点**」中被覆盖。

之前说过「**日志被提交**」的前提是奥满足它被多数节点所接收，所以如果一个节点拥有了比多数节点更加新的日志条目，则表示它必然包含了所有已提交的日志。

此时 Raft 算法会采用一种简单的处理方式来避免最新提交的日志条目在新的「**Leader 节点**」中被覆盖，就是在投票期间，只有当「**请求投票节点**」的日志条目至少和「**接收节点**」一样新时，「**接收节点**」给「**请求节点**」投票，否则拒绝为该节点进行投票。

Raft 算法通过比较两个节点的最后一条日志条目的索引值和任期号来判断哪个节点的日志更加新。

1.  如果两个节点最后的日志条目的任期不同，那么任期大的日志更加新。
2.  如果两个节点日志条目的任期号相同，那么索引值大的日志更加新。

### **4.3.2 前一任期的日志处理**

新上任的「**Leader 节点**」不能直接抛弃前一任期中「**未提交**」的日志，所以要继续处理这些日志。但是需要特殊处理：即使前一任期的日志被多数节点接收，也不能提交该日志，因为此时不安全，所以要等到「**当前任期**」的日志被大多数节点接收后才能提交日志。

下面通过两张图来说明：

![](https://article-images.zsxq.com/llAexYfPXz1vsz969c2yt02eCQh2)

![](https://article-images.zsxq.com/lkIycmT9vl6H9RX40n7TvGhYEwH-)

综上可以看到，对于前一任期的日志，即使被复制到多数节点，也可能被覆盖，所以并不安全，不能提交到「**状态机**」。只有当前任期的日志被复制到多数节点后，才可以被安全提交。

## **05 总结**

这里，我们一起来总结一下这篇文章的重点。

1、文章开头引出了「**Kraft**」算法是什么，内部的 Leader 选举、日志复制的流程是怎么样的。

2、接着带大家剖析了「**为什么要移除 Zookeeper**」。

3、接着带大家「**部署了 Kraft 模式的 Kafka 集群**」。

4、接着带大家剖析了「**Kraft 算法实现原理**」，从「**Leader 选举**」、「**日志复制**」、「**日志安全性**」三个方面进行了深度解剖。

下篇我们来深度剖析「**Kraft 请求处理流程**」，大家期待，我们下期见。