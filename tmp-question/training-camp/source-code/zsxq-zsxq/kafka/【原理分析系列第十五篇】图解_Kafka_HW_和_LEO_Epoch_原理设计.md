大家好，我是 **华仔**, 又跟大家见面了。

今天我们来讲解下 **Kafka HW 和 LEO Epoch 原理设计**，下面是正文。

## **01 HW 和 LEO 、LEO Epoch 概念**

在 Kafka 中，有几个与偏移量（offset）相关的重要概念，包括高水位（High Watermark，HW）、LEO（Log End Offset）和 LEO Epoch。它们在消息的可靠性和消费者的位置追踪方面发挥着关键作用。

1.  高水位（High Watermark，HW）：每个分区中已提交消息的最高偏移量，表示消费者可安全读取的位置。当消息被成功写入到所有的 ISR（In-Sync Replica，同步副本）中，并且已经被提交，该消息的偏移量就会被包含在高水位中。消费者只能消费高于高水位的消息，因为高水位以下的消息可能会因为某种原因（如副本故障）而丢失。
2.  LEO（Log End Offset）：每个分区中消息日志（log）的最后一个偏移量，表示当前最新的消息位置。即 LEO 是消息日志上最后一个已写入的消息的偏移量加一。生产者在发送消息时，会将消息写入到 LEO 位置之后的位置。消费者在消费消息时，可以获取到 LEO 的值，以确定他们是否已经消费到了最新的消息。
3.  LEO Epoch：一个与 LEO 相关的元数据属性，用于记录日志段的特定时间点的 LEO 值。Kafka 使用 LEO Epoch 来管理消息日志的截断和删除。当某个时间点之前的消息日志已经过期或不再被需要时，Kafka 可以根据 LEO Epoch 进行相应的日志段截断和删除操作。

「**高水位**」、「**LEO**」、「**LEO Epoch**」在 Kafka 中的作用如下：

1.  高水位用于确保只有可靠的消息被消费者读取。消费者只能消费高于高水位的消息，以确保消费者不会读取到尚未被持久化或复制到所有副本的消息。
2.  LEO 提供了一个参考点，用于判断消费者是否已经消费到了最新的消息。消费者可以通过比较自己的偏移量与 LEO 的值，来确定是否需要继续拉取新的消息。
3.  LEO Epoch 用于管理和维护消息日志的有效性。基于 LEO Epoch，Kafka 可以管理消息日志的截断和删除，以及维护消息的持久性和可用性。

总之，「**高水位**」、「**LEO**」、「**LEO Epoch**」在 Kafka 中扮演着重要的角色，用于确保消息的一致性和可靠性，以及判断消费者的消费进度和维护消息日志的有效性。

## **02 HW 和 LEO 机制**

首先这里有两个 Broker，也就是两台服务器，然后它们的分区中分别存储了两个 p0 的副本，一个是 Leader，一个是 Follower, 此时生产者开始往 Leader Partition 发送数据，数据最终写到磁盘上的。然后 Follower 会从 Leader那里去同步数据，Follower上的数据也会写到磁盘上。**可是 Follower 是先从 Leader 那去同步然后再写入磁盘的，所以它磁盘上面的数据肯定会比 Leader 的那块少一些**。

![](https://article-images.zsxq.com/FvY99vcBbWhSTvRujzi2xdvmosOc)

接下来我们来了解下日志复制中的一些重要偏移 (offset) 概念：

1.  起始位移base offset：副本中所含第一条消息的 offset。
2.  高水位high watermark：副本最新一条己提交消息的 offset。
3.  日志末端位移log end offset：副本中下一条待写入消息的 offset。

在 Kafka 中高水位的作用主要有2个:

1.  用来标识分区下的哪些消息是可以被消费者消费的。
2.  协助 Kafka 完成副本数据同步。

而LEO一个重要作用就是用来更新HW:

1.  如果 Follower 和 Leader 的 LEO 数据。
2.  同步了, 那么 HW 就可以更新了。
3.  HW 之前的消息数据对消费者是可见的。
4.  属于 commited 状态,  HW 之后的消息数据对消费者是不可见的。

![](https://article-images.zsxq.com/FkwNSu6KDTjTZ9YTzylSnTxG4t4j)

如上图所示: 每个副本会同时维护 HW 与 LEO 值：

1.  Leader 保证只有 HW 及其之前的消息，才对消费者是可见的。
2.  Follower 宕机后重启时会对其日志截断，只保留 HW 及其之前的日志消息（新版本有改动）。

对于日志末端位移, 即 Log End Offset (LEO)。它表示副本写入下一条消息的位移值。注意: 数字 12 所在的方框是虚线，说明这个副本当前只有 12 条消息，位移值是从 0 到 11，下一条新消息的位移是 12。显然，介于高水位和 LEO 之间的消息就属于未提交消息。即同一个副本对象，其高水位值不会大于 LEO 值。

高水位和 LEO 是副本对象的两个重要属性。Kafka 使用 Leader 副本的高水位来定义所在分区的高水位。即分区的高水位就是其 Leader 副本的高水位。

## **HW 和 LEO 更新机制**

从上面讲解我们知道了每个副本对象都保存了一组HW值和 LEO 值，但实际上，在 Leader 副本所在的 Broker 上，还保存了其他 Follower 副本的 LEO 值。看下图所示:

![](https://article-images.zsxq.com/FiOlPeuIi1PNgCYuY04NvsN0O1uk)

如上图所示，我们可以看到，Broker 0 上保存了某分区的 Leader 副本和所有 Follower 副本的 LEO 值，而 Broker 1 上仅仅保存了该分区的某个 Follower 副本。**Kafka 副本机制在运行过程中，会更新 Broker 1 上的 Follower 副本的高水位和 LEO 值，同时也会更新 Broker 0 上 Leader 副本的高水位和 LEO 以及所有 Follower 副本的 LEO，但是它不会更新所有 Follower 的高水位值，也就是我在图中标记为粉红色的部分**。

Kafka之所以要在 Broker 0 上保存这些所有 Follower 副本, 就是为了**帮助 Leader 副本确定其高水位，也就是分区高水位。**

接下来,我们从Leader 副本 和 Follower 副本两个维度，来总结一下高水位和 LEO 的更新机制。

首先 Follower 在从 Leader 同步数据的同时会带上自己的 LEO 的值，可是在**实际情况中有可能 p0 的副本可能会是多个**。它们也从 Leader Partition 同步数据并带上自己的LEO。**Leader Parition 就会记录这些 Follower 同步过来的 LEO，然后取最小的 LEO 值作为 HW 值****。**

之所以要这么做是为了保证如果 Leader Partition 宕机时，集群会从其它的 Follower Partition 里面选举出一个新的 Leader Partition。这时候无论选举了哪一个节点作为 Leader，都能保证存在当前待消费的数据,保证数据的安全性。

那么此时 Follower 自身 HW 的值如何确定，即 **Follower 获取数据时也会带上 Leader Partition 的 HW 的值，然后和自身的 LEO 值中取一个较小的值作为自己的 HW 值**。

![](https://article-images.zsxq.com/Fl-4x--93LT1GWaRLYdDt4XMx2je)

## **03 LeaderEpoch 机制**

从上一小节看数据同步似乎很完美，依托于 HW 和 LEO ，Kafka 既完美实现了消息的对外可见性，又实现了异步的副本同步机制。但是从上一节的讲解分析中，我们知道，**Follower 副本的 HW 更新需要一轮额外的拉取请求才能实现**。如果把上一小节图中例子扩展到更多个 Follower 副本，也许需要更多轮拉取请求。也就是说，Leader 副本 HW 更新和 Follower 副本 HW 更新在时间上是存在错配的。**这种错配是很多 “数据丢失”或者 “数据不一致”问题的根源**。因此社区在 0.11 版本正式引入了 **Leader Epoch** 概念，来规避因HW 更新错配导致的各种不一致问题。

所谓 Leader Epoch，我们大致可以认为是 Leader 版本。它由两部分数据组成。

Epoch: 一个单调递增的版本号。每当副本 Leader 权发生变更时，都会增加该版本号。小版本号的 Leader 被认为是过期 Leader，不能再行使 Leader 权力。

起始位移（Start Offset）: Leader 副本在该 Epoch 值上写入的首条消息的位移。

Kafka Broker 会在**内存中为每个分区都缓存 Leader Epoch 数据**，同时它还会定期地将这些信息**持久化到一个 checkpoint 文件**中。当 Leader Partition 写入消息到磁盘时，Broker 会尝试更新这部分缓存。如果该 Leader 是首次写入消息，那么 Broker 会向缓存中增加一个 Leader Epoch 条目，否则就不做更新。这样，每次有 Leader 变更时，新的 Leader 副本会查询这部分缓存，取出对应的 Leader Epoch 的起始位移，以避免数据丢失和不一致的情况。接下来,我们先看一个无 Leader Epoch 机制造成数据丢失的场景图, 如下:

![](https://article-images.zsxq.com/Ft6MM2Pue3z28vGiYwWPOfkh4wR8)  

正如上图描述的，单纯依靠 HW 是怎么造成数据丢失的。一开始时，Partition A 和Partition  B 都处于正常状态，A 是 Leader Partition 。现在我们假设 Leader Partition  和 Follower Partition 都写入了这4条消息，而且 Leader Partition 的 HW 已经更新了，但 Follower Partition 的 HW 还未更新(这种情况是可能能会发生的)。上面说过，Follower 的 HW 更新与 Leader 的 HW 是存在时间错配的。**如果此时 Partition B 所在的 Broker 宕机，当它重启回来后， Partition B 会执行日志截断操作，将 LEO 值调整为之前的 HW 值，也就是 3。**即位移值为 3 的那条消息被副本 B 从磁盘中删除，此时副本 B 的底层磁盘文件中只保存有 3 条消息，即位移值为 \[0,1,2\] 对应的消息。

当执行完截断操作后，Partition  B 开始从 Partition  A 拉取消息，执行正常的消息同步。如果就在这个时候，Partition  A 所在的 Broker 宕机了，那么 Kafka 就很无奈，只能让Partition B 成为新的 Leader，此时，当 Partition A 回来后，需要执行相同的日志截断操作，即将 HW 调整为与 Partition B 相同的值，也就是 3。这样操作之后，位移值为 3 的那条消息就从这两个 Partition 中被永远地抹掉了。这就是这张图要展示的数据丢失场景。

严格来说，这个场景发生的前提必须是**Broker 端参数** **[min.insync.replicas](http://min.insync.replicas/)** **设置为 1**。此时一旦消息被写入到 Leader 副本的磁盘，就会被认为是“commited状态”，但因存在**时间错配**问题导致 Follower 的 HW 更新是有滞后的。如果在这个短暂的滞后时间内，接连发生 Broker 宕机，那么这类数据的丢失就是无法避免的。

接下来, 我们来看下如何利用 Leader Epoch 机制来规避这种数据丢失。如下图所示:

![](https://article-images.zsxq.com/FtcTnht0p3Qa8eKSJ5mvxBe1tTEq)