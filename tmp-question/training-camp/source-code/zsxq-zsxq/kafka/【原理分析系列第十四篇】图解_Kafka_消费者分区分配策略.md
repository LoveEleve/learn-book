大家好，我是 **华仔**, 又跟大家见面了。

## **01 总体概述**

我们知道 Kafka 是一款高吞吐量，低延迟，高并发, 高可扩展性的消息队列产品， 那么如果某个 Topic 拥有数百万到数千万的数据量， 仅仅依靠 Consumer 进程消费， 消费速度可想而知， 所以需要一个扩展性较好的机制来保障消费进度。

此时 Consumer Group 应运而生， **Consumer Group 是 Kafka 提供的可扩展且具有容错性的消费者机制**。其中一个 Consumer Group 中有多个 Consumer，一个 Topic 也有多个 Partition，所以必然会涉及到 Partition 的分配问题： **确定哪个 Partition 由哪个 Consumer 来消费的问题**。

这就需要 Kafka 消费者支持分区分配策略。

![](https://article-images.zsxq.com/FuAc975rZIpdQdqMjQahTEpPqWSO)

## **02 分区分配策略**

## **2.1 分区分配策略要做的事情**

简单的说，消费者分区分配策略要做的就两件事情：

1.  同一个消费组中，给不同消费者分配能够消费的分区数。
2.  同一个消费组中, 一个分区只会被一个消费者消费。

## **2.2 分区分配策略如何选择**

### **2.2.1 分区分配策略配置**

先来看下如何配置分区分配策略，每个消费组客户端都可以配置一个 partition.assignment.strategy 属性且支持配置多个分配策略，如下：

partition.assignment.strategy=org.apache.kafka.clients.consumer.RoundRobinAssignor

2.8 版本之前其默认策略是 [org.apache.kafka.clients.consumer.RoundRobinAssignor](http://org.apache.kafka.clients.consumer.roundrobinassignor/) 轮询分配。

### **2.2.2 如何选择合适的分区分配策略**

上面说每个客户端成员都可以配置多个支持的分配策略,  那么消费者协调器 **GroupCoordinator** 会使用哪个分配策略去分配这些资源呢？

答案肯定是需要消费组下面的所有成员都使用同一种分配策略来进行分配。所以 **GroupCoordinator** 就会面临如何选择分配策略的问题。

选择的规则如下：

1.  选择所有消费者组成员都支持的分区分配策略。
2.  在第一步的基础上，优先选择每个 partition.assignment.strategy 配置靠前的策略。

这里举例说下，比较形象些：

![](https://article-images.zsxq.com/Fnv0n2H2R2QK1a3c-qzYClnOg1tX)

## **2.3 深度剖析分区分配策略**

目前Kafka已经实现了几种具体的策略：

1.  RangeAssignor（2.8版本中的默认策略）。
2.  RoundRobinAssignor。
3.  StickyAssignor。
4.  CooperativeStickyAssignor。

从 Kafka 2.8 版本开始，默认的分区分配策略是 [org.apache.kafka.clients.consumer.RangeAssignor](http://org.apache.kafka.clients.consumer.rangeassignor/)，也称为范围分配器。在之前的版本中默认的分区分配策略是 [org.apache.kafka.clients.consumer.RoundRobinAssignor](http://org.apache.kafka.clients.consumer.roundrobinassignor/)，也称为轮询分配器。

RangeAssignor 策略旨在优化消费者组在扩展性和负载均衡方面的性能。它通过将每个主题中的分区范围动态分配给消费者来实现负载平衡。具体而言，它会根据消费者组中的消费者数量和主题分区的范围来分配分区。这样，每个消费者将获得一些连续的分区范围，以确保消费者之间的负载均衡和消费者组的扩展性。

其整体的类关系图如下：

![](https://article-images.zsxq.com/FjkKoBiuDNSEHJ2sBFpnhA1eUqQU)

接下来我们挨个来深度剖析一下。

## **2.3.1 RangeAssignor**

RangeAssignor 是 Kafka 默认的分区分配算法，它是按照**单个 Topic 维度进行分配的**，也就是说逐个 Topic 进行分配，它只负责将每一个 Topic的分区尽可能均衡的分配给消费者。

partition.assignment.strategy=org.apache.kafka.clients.consumer.RangeAssignor

其规则如下：

1.  对于每个 Topic，首先对 Partition 按照分区ID进行排序。
2.  然后对订阅这个 Topic 的 Consumer Group 的 Consumer 再按照字母进行排序
3.  之后尽量均衡的按照范围区段将分区分配给 Consumer，即用分区数除以消费者个数得出每个 Consumer 应该分配的分区个数N（不能整除时向上取整），然后依次给每个Consumer一次分配N个分区（最后一个可能不足N个）。

> 注意：此时可能会造成先分配分区的 Consumer 进程的任务过重（分区数无法被消费者数量整除）。

举例说明：

比如现在组内有 2 个 Consumer C0 和 C1，订阅了 2 个 Topic tA 和 tB，每个 Topic 有 3 个分区，即：tAp0, tAp1, tAp2, tBp0, tBp1, tBp2。

假设消费者排序后顺序为C0、C1，先开始分配 Topic tA，3 个分区/2 个 Consumer等于 1.5，向上取整为 2，即每个 Consumer 分配 2 个分区，于是 tA 的分配结果为：

1.  C0：\[tAp0, tAp1\]
2.  C1：\[tAp2\]

然后再分配 Topic tB，同理其分配结果如下：

1.  C0：\[tBp0, tBp1\]
2.  C1：\[tBp2\]​

所以最终合并后的分配结果为：

1.  C0：\[tAp0, tAp1, tBp0, tBp1\]
2.  C1：\[tAp2, tBp2\]

**分区分配常见分析如下图所示（同一个消费者组下的多个 Consumer）：**

![](https://article-images.zsxq.com/FqoQ8hq2juVWfmKwvkEvYrXUBsrn)

**同理新增消费者分配如下：**

![](https://article-images.zsxq.com/Fo__WS1UsW1TxgvDrK1x5uEKPt89)

## **RangeAssignor 弊端**

RangeAssignor 针对单个 Topic 的情况下显得比较均衡， 但是假如 Topic 很多的话，消费者成员排序靠前的可能会比排序靠后的负载多很多。

  
![](https://article-images.zsxq.com/FteWweK2arBc3Rmw7bkfFkYgTDow)

## **2.3.2 RoundRobinAssignor**

RoundRobinAssignor 策略俗称轮询策略，它和 RangeAssignor 的不同之处在于它是**将所有 Topic 的分区放在一起进行轮询分配的**，即将 Consumer Group 内订阅的所有 Topic 的 Partition 及所有 Consumer 进行排序后按照顺序尽量均衡的一个一个进行分配。

partition.assignment.strategy=org.apache.kafka.clients.consumer.RoundRobinAssignor

其规则如下：

1.  先将 Consumer 按 member.id 进行排序，将所有分区按数值排序。
2.  然后将分区以轮询的方式依次（每次一次）分配给各个 Consumer。

> 注意：如果 Consumer Group 内，每个 Consumer 订阅都订阅了相同的Topic，那么分配结果是均衡的。如果订阅 Topic 是不同的，那么分配结果是不保证“尽量均衡”的，因为某些 Consumer 可能不参与一些 Topic 的分配。

举例说明：

**例子1**：**当组内每个 Consumer 订阅的 Topic 相同情况时**。假设现在组内有 2 个 Consumer C0 和 C1，订阅了 2 个 Topic tA 和 tB，每个 Topic 有 3 个分区，即：tAp0, tAp1, tAp2, tBp0, tBp1, tBp2。将这 6 个分区以 round-robin 的方式分配给 C0 和 C1，分配结果为：

1.  C0：\[tAp0, tAp2, tBp1\]
2.  C1：\[tAp1, tBp0, tBp2\]

![](https://article-images.zsxq.com/Fk7YKiWmJwGSXh5oUd3W6sQwJ-WA)

**例子2**：**当组内每个 Consumer 订阅的 Topic 不同情况时**。假设有 3 个 Consumer C0、C1、C2；有 3 个 Topic tA、tB、tC，3 个 Topic 的分区数分别为 1、2、3。即所有分区为：tAp0, tBp0, tBp1, tCp0, tCp1, tCp2。其中 C0订阅了 tA，C1订阅了tA、tB，C2订阅了tA、tB、tC，则分配结果为：

1.  C0：\[tAp0\]
2.  C1：\[tBp0\]
3.  C2：\[tBp1, tCp0, tCp1, tCp2\]

![](https://article-images.zsxq.com/Fjoi1boHYa6dJsntq_z_owiNjW09)

## **2.3.3 StickyAssignor**

上面介绍的两种分区分配方式,多多少少都会有一些分配上的偏差,  而且每次重新分配的时候都是把所有的都重新来计算并分配一遍, 那么每次分配的结果都会偏差很多, 如果我们在计算的时候能够考虑上一次的分配情况,来尽量的减少分配的变动，也不失为一种优化方案。

StickyAssignor 策略俗称粘性分区策略，它是 Kafka Java 客户端提供的分配策略中最复杂的一种。

partition.assignment.strategy=org.apache.kafka.clients.consumer.StickyAssignor

该算在分配时有 2 个重要的目标如下：

1.  Topic Partition 的分配要尽量均衡。
2.  当 Rebalance (重分配) 发生时，在保证1的情况下，尽量与上一次分配结果保持一致。

> 注意：当两个目标发生冲突的时候，优先保证第一个目标，这样可以使分配更加均匀，其中第一个目标是3种分配策略都尽量去尝试完成的， 而第二个目标才是该算法的精髓所在。

下面我们举例来聊聊 RoundRobinAssignor 跟 StickyAssignor 的区别：

**例子1**：**当组内每个 Consumer 订阅的 Topic 相同情况时，RoundRobinAssignor 跟 StickAssginor 分配是一致的**：假设有 3 个 Consumer C0、C1、C2；有 2个 Topic tA、tB，每个 Topic 有 3 个分区，即所有分区为：tAp0, tAp1, tAp2, tBp0, tBp1, tBp2。现在所有 Consumer 都订阅了 2 个主题，则分配结果如下：

1.  C0：\[tAp0, tBp0\]
2.  C1：\[tAp1, tBp1\]
3.  C2：\[tAp2, tBp2\]

![](https://article-images.zsxq.com/FtRKPrpOIoW0U6nHlpc08HuxQX67)

这个结果和前面 RoundRobinAssignor 的分配结果是一样的。**当上述情况发生 Rebalance 情况后，可能分配会不太一样，假如这时候 C1 发生故障下线:** 需要重新分配，如果是 RoundRobinAssignor，重新分配后的结果如下：

1.  C0：\[tAp0, tAp2, tBp1\]
2.  C2：\[tAp1, tBp0, tBp2\]

![](https://article-images.zsxq.com/FjldEHl3VZ_9tVMdUugUmb8-2Jsk)

但如果使用 StickyAssignor 的话，重新分配后的结果如下：

1.  C0：\[tAp0, tBp0, tBp1\]
2.  C2：\[tAp1, tAp2, tBp2\]

![](https://article-images.zsxq.com/FvcbkBlwDKwqUYp3_V1RJ-9NxT4S)

**从上面 Rebalance 后的结果可以看出，RoundRobinAssignor 完全重新分配了一遍，而 StickyAssignor 则在院校基础上达到了均匀分配的状态。**

**例子2**：**当组内每个 Consumer 订阅的 Topic 不同情况时，RoundRobinAssignor 跟 StickAssginor 分配是一致的**：假设有 3 个 Consumer C0、C1、C2；有 2个 Topic tA、tB，每个 Topic 有 3 个分区，即所有分区为：tAp0, tAp1, tAp2, tBp0, tBp1, tBp2。现在C0、C1 订阅 tA，tB，C2 订阅 tA，tB，tC，如果是 RoundRobinAssignor，重新分配后的结果如下：

1.  C0：\[tAp0\]
2.  C1：\[tBp0\]
3.  C1：\[tBp1, tCp0, tCp1, tCp2\]

![](https://article-images.zsxq.com/FvNrPQ6BeJhPkTu_BhsdYyd5GgvJ)

如果是 StickyAssignor，重新分配后的结果如下：

1.  C0：\[tAp0\]
2.  C1：\[tBp0,tBp1\]
3.  C1：\[tCp0, tCp1, tCp2\]

![](https://article-images.zsxq.com/Fk_hzONaYxHCpGiEdCn8lOR2ZTCj)

**当上述情况发生 Rebalance 情况后，可能分配会不太一样，假如此时 C1 发生故障下线：**如果是 RoundRobinAssignor，重新分配后的结果如下：

1.  C0：\[tAp0,tBp0\]
2.  C2：\[tBp1,tCp0, tCp1, tCp2\]

![](https://article-images.zsxq.com/FquqpoMfccRGIBAaZszz0somOSBw)

如果是 StickyAssignor，重新分配后的结果如下：

1.  C0：\[tAp0,tBp0,tBp1\]
2.  C2：\[tCp0,tCp1,tCp2\]

![](https://article-images.zsxq.com/FnC2cVS4TEaLLzUl4zT4M8_2174H)

**从上面 Rebalance 后的结果可以看出，RoundRobinAssignor 重新分配之后造成了严重的分配倾斜，因此在生产环境上如果想要减少重分配带来的开销，可以采用 StickyAssignor 分区分配策略。**

## **2.3.4 CooperativeStickyAssignor**

上一小节剖析的 StickyAssignor 粘性分区策略，其主要作用是保证消费者客户端在重平衡之后能够维持原本的分配方案。但是 StickyAssignor 还是属于 [RebalanceProtocol.EAGER](http://rebalanceprotocol.eager/)  协议, 重平衡的时候需要每个客户端都要先放弃当前所持有的资源。

为了解决这个问题, Kafka 就有了 **CooperativeStickyAssignor** 分配策略，它使用的是[RebalanceProtocol.COOPERATIVE](http://rebalanceprotocol.cooperative/)，渐进式的重平衡。

这里你可以理解为 **CooperativeStickyAssignor** 的分配策略跟 **StickyAssignor** 的策略差不多。

## **2.3.5 简述重平衡协议**

前 4 小节，我们讲的是分区策略, 但是分区策略本质上又分为两大类：

1.  RebalanceProtocol.EAGER
2.  RebalanceProtocol.COOPERATIVE   协作重平衡，kafka 2.4 版本出的功能。

这两个区别是 **EAGER** 重新平衡协议要求消费者在参与重新平衡事件之前始终撤销其拥有的所有分区。所以它允许完全改组分配。而 **COOPERATIVE** 协议允许消费者在参与再平衡事件之前保留其当前拥有的分区，分配者不应该立即重新分配任何拥有的分区，而是可以指示消费者需要撤销分区，以便可以在下一次重新平衡事件中将被撤销的分区重新分配给其他消费者。

**COOPERATIVE 协议将一次全局重平衡，改成每次小规模重平衡，直到最终收敛平衡的过程。**

COOPERATIVE 有效的改进来在此之前 EAGER 协议重平衡而触发的 stop-the-world(STW) 问题。

我们上面讲的分配策略3种策略都是 [RebalanceProtocol.EAGER](http://rebalanceprotocol.eager/)  协议：

1.  RangeAssignor 范围分区分配策略
2.  RoundRobinAssignor 轮询分区策略
3.  StickyAssignor 粘性分区策略

而 **CooperativeStickyAssignor** 分配策略是使用的 [RebalanceProtocol.COOPERATIVE](http://rebalanceprotocol.cooperative/) 协议。

## **03 总结**

就通用场景来说，进行分区分配的时候，一方面我们比较关注**分配的均衡性**。另一方面也会比较关注当发生Consumer Group Rebalance 的时候，能否最大限度的保持原有的分配。

这里从这两个角度来看：

1.  **关于均衡**：RangeAssignor 和 RoundRobinAssignor 的分配是否均衡，主要取决于组内 Consumer 订阅的主题情况以及每个主题的分区个数。而 StickyAssignor 和 CooperativeAssignor 则不太依赖这个条件，相当于在任何情况下，都能实现一个相对均衡的分配方案。
2.  **当发生 Rebelance 时最大限度保持原有分配方案**：RangeAssignor 和 RoundRobinAssignor 算法自身其实完全没有考虑这点，要实现这个功能点，需要结合 static-membership 特性来实现，即指定 [group.instance.id](http://group.instance.id/)，这样相当于确定了Consumer 的顺序，只要组内 Consumer 不变、订阅信息不变，就能有一个稳定的分配结果。而 StickyAssignor 和CooperativeAssignor 则考虑了这点，但有一个注意点就是对于 StickyAssignor，虽然会尽量保留原有的分配方案，但因为使用的是 Eager Rebalance 协议，所以在 Rebalance 的时候还是会回收所有分区，而 CooperativeAssignor 使用的是Cooperative Rebalance，所以只会回收有变化的分区。

因此，新的系统（Kafka 2.4及之后版本）建议你使用 **CooperativeStickyAssignor 分配策略**。当然，我们也可以实现自己的 PartitionAssignor。