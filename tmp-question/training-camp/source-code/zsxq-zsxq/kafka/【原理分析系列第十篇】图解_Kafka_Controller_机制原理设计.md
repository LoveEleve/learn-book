大家好，我是 **华仔**, 又跟大家见面了。

从今天开始我们讲解 **Kafka Controller 机制原理设计**的。

## **01 总体概述**

Kafka Controller 是 Kafka 集群中的一个特殊节点，它负责管理集群的元数据和协调集群的各种操作。

其机制原理设计如下：

1.  **选举机制**：Kafka Controller 采用 ZooKeeper/Quorum 的选举机制来选举集群中的 Controller 节点。
2.  **元数据管理**：Kafka Controller 负责管理集群的元数据，包括 Topic、Partition、Broker 等信息。
3.  **副本管理**：Kafka Controller 还负责管理集群中的副本，包括副本的分配、迁移、删除等操作。
4.  **Leader 选举**：Kafka Controller 还负责管理 Partition 的 Leader，包括 Leader 的选举、切换等操作。
5.  **集群扩容**：Kafka Controller 还负责管理集群的扩容，包括添加新的 Broker 节点、删除已有的 Broker 节点等操作。

接下来我们挨个详细剖析下其原理设计。

## **02 选举机制**

对于 Kafka 3.x 之前的版本来说， Kafka 集群中的 Controller 节点是通过 **Zookeeper 的选举机制**来选举的。当一个节点成为 Controller 后，它会在 **Zookeeper 中创建一个临时节点，表示它是当前的 Controller**。如果当前的 Controller 节点宕机或失去连接，ZooKeeper 会重新选举一个新的 Controller 节点。

例如，假设当前的 Controller 节点是节点 A，它在 ZooKeeper 中创建了一个临时节点 /controller，表示它是当前的 Controller。当节点 A 宕机或失去连接时，ZooKeeper 会重新选举一个新的 Controller 节点，假设选举出来的节点是节点 B，它会在 ZooKeeper 中创建一个临时节点 /controller，表示它是当前的 Controller。

如下图所示：

![](https://article-images.zsxq.com/FgXZ8b7-LFZGkQULqaxk0weCO5c-)

而在 Kafka 3.x 版本中，Controller 的选举机制不再依赖于 ZooKeeper，而是采用了一种**新的选举机制**，称为 **Controller Quorum**。

Controller Quorum 是一种基于 Raft 算法的分布式一致性协议，它可以保证在任何时候只有一个 Controller 节点处于活动状态，从而保证集群的稳定性和可用性。

其实现机制如下：

1.  Controller Quorum 由多个 Controller 节点组成，每个节点都有一个唯一的 ID。
2.  当一个节点启动时，它会向其他节点发送 JoinGroup 请求，请求加入 Controller Quorum。
3.  如果当前 Controller Quorum 中的节点数小于设定的最小节点数，那么该节点会被选举为活动节点，并成为 Controller。
4.  如果当前 Controller Quorum 中的节点数已经达到设定的最小节点数，那么该节点会等待其他节点的加入请求。
5.  当一个节点加入 Controller Quorum 后，它会与其他节点进行心跳检测，以确保其他节点的状态正常。
6.  如果一个节点宕机或失去连接，其他节点会重新选举一个新的活动节点，并将元数据信息同步到所有的 Broker 节点。
7.  当一个节点退出 Controller Quorum 时，其他节点会重新选举一个新的活动节点，并将元数据信息同步到所有的 Broker 节点。

总之，Controller Quorum 是一种基于 Raft 算法的分布式一致性协议，它可以保证在任何时候只有一个 Controller 节点处于活动状态，从而保证集群的稳定性和可用性。

与 ZooKeeper 相比，Controller Quorum 具有更高的性能和可靠性，可以更好地适应大规模集群的需求。

![](https://article-images.zsxq.com/Fqy8TAqFHVa54_Zp81f1ngnvV7ph)

## **03 元数据管理**

Kafka Controller **负责管理集群中的元数据**，包括 「**Topic**」、「**Partition**」、「**Broker**」等信息。它会定期从 ZooKeeper 中获取最新的元数据信息，并将其缓存在本地内存中。当有新的 Topic 或 Partition 被创建或删除时，Controller 会更新元数据信息，并将其同步到所有的 Broker 节点。

例如，假设当前集群中有一个 Topic，它有两个 Partition，分别分配在 Broker 1 和 Broker 2 上。当有新的 Partition 被创建时，Controller 会更新元数据信息，并将其同步到所有的 Broker 节点，使得所有的 Broker 节点都能够知道新的 Partition 信息。

我们从下面几个方面分别剖析。

## **03.1 元数据信息的存储**

对于 kafka 3.x 之前版本来说，Kafka Controller 将元数据信息**存储在 Zookeeper 中**，包括 Topic、Partition、Broker 等信息。每个节点都可以从 ZooKeeper 中获取最新的元数据信息，并将其缓存在本地内存中，以便快速访问。

而对于 Kafka 3.x 版本来说，将元数据信息存储在 Kafka 集群中的**一个特殊 Topic 中，称为 \_clustermetadata**。每个节点都可以从该 Topic 中获取最新的元数据信息，并将其缓存在本地内存中，以便快速访问。

## **03.2 元数据信息的更新**

当有新的 Topic 或 Partition 被创建或删除时，Kafka Controller 会更新元数据信息，并将其同步到所有的 Broker 节点。当一个 Broker 节点宕机或失去连接时，Controller 会重新分配该节点上的副本到其他节点上，并更新元数据信息。

## **03.3 元数据信息的同步**

对于 kafka 3.x 之前版本来说，Kafka Controller 会定期**从 Zookeeper 中获取最新的元数据信息，并将其同步到所有的 Broker 节点**。当一个 Broker 节点加入集群时，Controller 会将元数据信息同步到该节点。当一个 Broker 节点宕机或失去连接时，Controller 会将其从集群的 Broker 列表中删除，并将元数据信息同步到其他节点。

而对于 Kafka 3.x 版本来说，Kafka Controller 会定期**从 \_clustermetadata Topic 中获取最新的元数据信息，并将其同步到所有的 Broker 节点**。

## **03.4 元数据信息的缓存**

Kafka Controller 会将元数据信息缓存在本地内存中，以便快速访问。当元数据信息发生变化时，Controller 会更新本地缓存，并将其同步到所有的 Broker 节点。

## **03.5 元数据信息的访问**

Kafka Controller 允许其他节点通过 API 访问元数据信息，以便进行各种操作。例如，可以通过 API 获取 Topic 的列表、Partition 的列表、Broker 的列表等信息。

## **03.6 详解\_clustermetadata**

Kafka \_clustermetadata 结构体主要由以下几个部分组成：

1.  **brokers**：表示 Kafka 集群中所有的 broker，包括它们的 ID、主机名、端口号等信息。
2.  **topics**：表示 Kafka 集群中所有的 topic，包括它们的名称、分区数、副本数等信息。
3.  **partitions**：表示 Kafka 集群中所有的 partition，包括它们所属的 topic、分区 ID、副本分配情况等信息。
4.  **replicas**：表示 Kafka 集群中所有的 replica，包括它们所属的 partition、副本 ID、所在的 broker 等信息。

在 Kafka 中，\_clustermetadata 结构体是由 Kafka Controller 负责维护和更新的。 \_clustermetadata 结构体的实现确实依赖于 ZooKeeper，在 Kafka 集群中，ZooKeeper 负责存储和管理 Kafka 的元数据信息，包括 broker、topic、partition、replica 等信息。 KafkaController 会定期从 ZooKeeper 中获取集群的元数据信息，并将其转换为 \_clustermetadata 结构体。

具体实现过程如下：

1.  在 Kafka Controller 中定义 \_clustermetadata 结构体，并提供相应的 getter 和 setter 方法。
2.  在 Kafka Controller 中实现定时从 ZooKeeper 中获取元数据信息的逻辑，并将其转换为 \_clustermetadata 结构体。
3.  在 Kafka Controller 中实现处理 broker 上下线、topic 创建、partition 副本分配等事件的逻辑，并更新 \_clustermetadata 结构体。
4.  在 Kafka Controller 中提供查询 \_clustermetadata 结构体的方法，供其他组件使用。

通过以上实现，Kafka 能够实时维护和更新集群的元数据信息，并提供查询接口供其他组件使用，从而保证了 Kafka 集群的稳定和可靠性。

这样做的好处是，ZooKeeper 可以提供高可用性和数据一致性，从而保证 Kafka 集群的稳定和可靠性。同时，Kafka 也提供了其他的元数据管理方式，比如使用 Kafka 自身的元数据管理工具，但这种方式相对于使用 ZooKeeper 来说，实现起来更加复杂，而且需要额外的配置和维护工作。因此，大多数 Kafka 集群仍然使用 ZooKeeper 来管理元数据信息。

## **03.7 小结**

总之，对于 Kafka 3.x 版本之前，Controller 元数据管理的设计原理主要是将元数据信息存储在 ZooKeeper 中，而 Kafka 3.x 版本在元数据管理方面有一些差异，主要是将元数据信息存储在一个特殊的 Topic 中，并通过定期同步、缓存、更新等方式来保证元数据信息的一致性和可靠性。

这种设计可以有效地管理集群的元数据信息，保证集群的高可用性和稳定性。

## **04 副本管理**

Kafka Controller **负责管理集群中的副本，包括副本的分配、迁移、删除等操作**。当一个 Broker 节点宕机或失去连接时，Controller 会重新分配该节点上的副本到其他节点上。当一个 Topic 的副本数发生变化时，Controller 会根据副本分配策略重新分配副本，并将其同步到所有的 Broker 节点。

例如，假设当前集群中有一个 Topic，它有两个 Partition，每个 Partition 都有三个副本，分别分配在 Broker 1、Broker 2 和 Broker 3 上。当 Broker 1 宕机时，Controller 会重新分配该节点上的副本到其他节点上，例如将 Broker 1 上的副本分配到 Broker 4 和 Broker 5 上。

我们从下面几个方面分别剖析。

## **04.1 副本分配**

Kafka Controller 负责管理 Kafka 集群中的副本分配。当一个新的 topic 被创建时，Kafka Controller 会根据配置的副本因子和分区数，计算出每个分区应该分配到哪些 broker 上，并将分配结果保存到 ZooKeeper 中。

**负责当一个 Broker 上线或下线时， Kafka Controller 会重新计算副本分配，并将新的分配结果保存到 Zookeeper 中。**

在 ZooKeeper 中，副本分配结果被存储在 **/brokers/topics/\[topicname\]/partitions/\[partitionid\]/state** 节点下，其中 topicname 表示 topic 的名称，partitionid 表示 partition 的 ID。

在 state 节点中，副本分配结果被表示为一个 JSON 格式的字符串，包括以下几个字段：

1.  **version**：表示副本分配结果的版本号，每次副本分配都会增加版本号。
2.  **leader**：表示 partition 的主副本所在的 broker ID。
3.  **isr**：表示 partition 的 ISR（In-Sync Replica）列表，即与主副本保持同步的副本列表。
4.  **replicas**：表示 partition 的所有副本列表，包括主副本和备份副本。

对 Kafka 3.x 版本来说，副本分配结果的存储方式和 2.x 版本基本相同，**但是在 3.x 版本中，副本分配结构的版本号被改成 long 类型，以支持更大的版本号范围**。

此外，Kafka 3.x 版本还引入了一些新的副本管理功能，比如支持动态调整 ISR 列表、支持副本重平衡等，这些功能都需要在副本分配结果的存储和管理上进行相应的改进和优化。

## **04.2 副本迁移**

当一个 broker 下线时，Kafka Controller 会检查该 broker 上的副本是否有备份副本可以接替。如果没有备份副本可以接替，Kafka Controller 会尝试将该 broker 上的副本迁移到其他 broker 上。在副本迁移的过程中，Kafka Controller 会先将副本从原来的 broker 上删除，然后再将副本复制到新的 broker 上。副本迁移的过程中，Kafka Controller 会监控副本的状态，并在副本迁移完成后更新副本的元数据信息。

Kafka controller 副本迁移是将某个分区的 leader 副本从当前的副本列表中迁移到新的副本列表中，以提高分区的可用性和性能。下面以一个具体的例子来说明 Kafka controller 副本迁移过程。

**假设有一个 Kafka 集群，其中有一个分区 P1，副本列表为 \[R1、R2、R3\]，其中 R1 是当前的 Leader 副本。由于 R1 所在的机器出现故障，导致 R1 无法正常工作，此时 Kafka Controller 会触发副本迁移的过程，具体步骤如下：**

1.  Kafka controller 会将 R1 从 ISR（In-Sync Replicas）列表中移除，并将新的 ISR 列表更新到 ZooKeeper 中。同时，Kafka controller 会将 P1 的 leader 副本切换到 ISR 列表中的下一个副本，即 R2。
2.  Kafka controller 会将 R1 从副本列表中移除，并将新的副本列表更新到 ZooKeeper 中。同时，Kafka controller 会将 R1 所在的机器标记为不可用，以避免将来再次将 R1 分配到该机器上。
3.  Kafka controller 会将 P1 的副本列表扩容，以提高分区的可用性和性能。具体来说，Kafka controller 会将一个新的副本 R4 分配到一个可用的机器上，并将 R4 添加到副本列表中。同时，Kafka controller 会将 R4 加入 ISR 列表，并将新的 ISR 列表更新到 ZooKeeper 中。
4.  Kafka controller 会将 P1 的 leader 副本切换回到 ISR 列表中的第一个副本，即 R2。同时，Kafka controller 会将 R2 加入 ISR 列表，并将新的 ISR 列表更新到 ZooKeeper 中。
5.  Kafka controller 会将 R1 的数据复制到 R4 上，并将 R4 加入 ISR 列表。同时，Kafka controller 会将 R4 的 LEO（Log End Offset）和 HW（High Watermark）更新为与 R1 相同的值，以保证数据的一致性。
6.  Kafka controller 会将 R1 所在的机器标记为可用，以便将来再次将副本分配到该机器上。

通过以上步骤，Kafka controller 完成了副本迁移的过程，将 P1 的 leader 副本从 R1 迁移到了 R2，并将新的副本列表和 ISR 列表更新到 ZooKeeper 中。这样，即使某个副本出现故障，也可以通过副本迁移的方式来提高分区的可用性和性能。

## **04.3 副本选举**

当一个 broker 下线时，Kafka Controller 会检查该 broker 上的副本是否有备份副本可以接替。如果有多个备份副本可以接替，Kafka Controller 会根据副本的状态和位置信息，选择一个最合适的备份副本作为新的主副本。副本选举的过程中，Kafka Controller 会监控副本的状态，并在选举完成后更新副本的元数据信息。

Kafka controller 副本选举是在某个分区的 leader 副本出现故障或者网络异常时，从 ISR（In-Sync Replicas）列表中选举一个新的 leader 副本，以保证分区的可用性和性能。下面也以一个具体的例子来说明 Kafka controller 副本选举的过程。

**假设有一个 Kafka 集群，其中有一个分区 P1，副本列表为 \[R1、R2、R3****\]，其中 R1 是当前的 Leader 副本。由于 R1 所在的集群出现故障，导致 R1 无法正常工作，此时 Kafka Controller 会触发副本选举的过程，具体的步骤如下：**

1.  Kafka controller 会从 ISR 列表中选举一个新的 leader 副本。具体来说，Kafka controller 会选择 ISR 列表中 LEO（Log End Offset）最大的副本作为新的 leader 副本。假设 ISR 列表为 \[R2, R3\]，则 Kafka controller 会选择 LEO 最大的副本 R3 作为新的 leader 副本。
2.  Kafka controller 会将 P1 的 leader 副本切换到新的 leader 副本 R3。同时，Kafka controller 会将新的 leader 副本 R3 加入 ISR 列表，并将新的 ISR 列表更新到 ZooKeeper 中。
3.  Kafka controller 会将 R1 从 ISR 列表中移除，并将新的 ISR 列表更新到 ZooKeeper 中。同时，Kafka controller 会将 R1 所在的机器标记为不可用，以避免将来再次将 R1 分配到该机器上。
4.  Kafka controller 会将 R1 的数据复制到新的 leader 副本 R3 上，并将 R3 的 LEO（Log End Offset）和 HW（High Watermark）更新为与 R1 相同的值，以保证数据的一致性。

通过以上步骤，Kafka controller 完成了副本选举的过程，将 P1 的 leader 副本从 R1 切换到了新的 leader 副本 R3，并将新的 ISR 列表更新到 ZooKeeper 中。这样即使某个副本出现故障，也可以通过副本选举的方式来保证分区的可用性和性能。

## **04.4 副本状态监控**

Kafka Controller 会定期检查副本的状态，并将状态信息保存到 ZooKeeper 中。如果一个副本的状态发生了变化，Kafka Controller 会根据变化的类型和副本的位置信息，决定是否需要进行副本迁移或副本选举。

Kafka controller 副本状态监控就是定期检查每个副本的状态，包括副本的 LEO（Log End Offset）、HW（High Watermark）、ISR（In-Sync Replicas）列表等，以保证副本的可用性和一致性。下面以一个具体的例子来说明 Kafka controller 副本状态监控的过程。

**假设有一个 Kafka 集群，其中有一个分区 P1，副本列表为 \[R1、R2、R3****\]，其中 R1 是当前的 Leader 副本。 Kafka Controller 会定期检查每个副本的状态，具体的步骤如下：**

1.  Kafka controller 会检查每个副本的 LEO（Log End Offset）和 HW（High Watermark），以判断副本是否已经同步了最新消息。如果某个副本的 LEO 和 HW 落后于 ISR 列表中的其他副本，则 Kafka controller 会将该副本从 ISR 列表中移除，并将新的 ISR 列表更新到 ZooKeeper 中。
2.  Kafka controller 会检查每个副本的 ISR 列表，以判断副本是否已经同步了最新消息。如果某个副本的 ISR 列表与副本列表不一致，则 Kafka controller 会将该副本的 ISR 列表更新为副本列表中已经同步了最新消息的副本，并将新的 ISR 列表更新到 ZooKeeper 中。
3.  Kafka controller 会检查每个副本的状态，包括副本所在的机器是否可用、副本的数据是否完整、副本的同步速度是否正常等。如果某个副本的状态异常，则 Kafka controller 会将该副本从 ISR 列表中移除，并将新的 ISR 列表更新到 ZooKeeper 中。

通过以上步骤，Kafka controller 可以定期检查每个副本的状态，以保证副本的可用性和一致性。这样，即使某个副本出现故障或者网络异常，也可以通过副本状态监控的方式来保证分区的可用性和性能。

## **04.5 小结**

综上，Kafka Controller 能够实现副本的自动管理和维护，从而保证 Kafka 集群的高可用性和数据一致性。同时，Kafka Controller 还提供了一些 API 接口，供用户查询和管理副本的状态和位置信息。

## **05 Leader 选举**

Kafka Controller 负责管理 Partition 的 Leader，包括 「**Leader 选举**」、「**Leader 切换**」等操作。当某个分区的 Leader 副本出现故障或者网络异常时，从 ISR（In-Sync Replicas）列表中选举一个新的 Leader 副本，以保证分区的可用性和性能。具体的选举过程如下：

1.  Kafka controller 定期检查每个分区的 ISR 列表，以判断当前的 Leader 副本是否可用。如果当前的 Leader 副本不可用，则 Kafka controller 会触发新的 Leader 选举过程。
2.  Kafka controller 会从 ISR 列表中选举一个新的 Leader 副本。具体来说，Kafka controller 会选择 ISR 列表中 LEO（Log End Offset）最大的副本作为新的 Leader 副本。如果有多个副本的 LEO 相同，则 Kafka controller 会选择其中 HW（High Watermark）最大的副本作为新的 Leader 副本。
3.  Kafka controller 会将分区的 Leader 副本切换到新的 Leader 副本，并将新的 Leader 副本加入 ISR 列表。同时，Kafka controller 会将旧的 Leader 副本从 ISR 列表中移除，并将新的 ISR 列表更新到 ZooKeeper 中。
4.  Kafka controller 会将新的 leader 副本的 LEO 和 HW 更新为与旧的 leader 副本相同的值，以保证数据的一致性。

下面举一个例子来说明 Kafka controller Leader 选举的过程。

**假设有一个 Kafka 集群，其中有一个分区 P1，副本列表为 \[R1、R2、R3****\]，其中 R1 是当前的 Leader 副本。 由于 R1 所在的集群出现故障，导致 R1 无法正常工作，此时 Kafka Controller 会触发新的 Leader 选举的过程，具体的步骤如下：**

1.  Kafka controller 定期检查每个分区的 ISR 列表，发现 R1 不可用，触发新的 leader 选举过程。
2.  Kafka controller 从 ISR 列表中选举一个新的 leader 副本。假设 ISR 列表为 \[R2, R3\]，其中 R2 的 LEO 为 100，HW 为 90，R3 的 LEO 为 110，HW 为 100，则 Kafka controller 会选择 R3 作为新的 leader 副本。
3.  Kafka controller 将 P1 的 leader 副本切换到新的 leader 副本 R3，并将 R3 加入 ISR 列表。同时，Kafka controller 将 R1 从 ISR 列表中移除，并将新的 ISR 列表更新到 ZooKeeper 中。
4.  Kafka controller 将新的 leader 副本 R3 的 LEO 和 HW 更新为与旧的 leader 副本 R1 相同的值，即 LEO 为 100，HW 为 90。

通过以上步骤，Kafka controller 完成了新的 leader 选举过程，将 P1 的 Leader 副本从 R1 切换到了新的 leader 副本 R3，并将新的 ISR 列表更新到 ZooKeeper 中。这样，即使某个副本出现故障，也可以通过新的 Leader 选举过程来保证分区的可用性和性能。

## **06 集群扩容**

Kafka Controller 负责管理集群的扩容，包括「**添加新的 Broker 节点**」、「**删除已有的 Broker 节点**」等操作。当一个新的 Broker 节点加入集群时，Controller 会将其加入到集群的 Broker 列表中，并将元数据信息同步到该节点。当一个 Broker 节点宕机或失去连接时，Controller 会将其从集群的 Broker 列表中删除，并重新分配该节点上的副本到其他节点上。

在新的机器上安装 Kafka，并配置 Kafka controller 的相关参数，包括 ZooKeeper 的地址、Kafka controller 的 ID 等。将新的 Kafka controller 加入到 Kafka 集群中。具体来说，需要将新的 Kafka controller 的 ID 添加到 ZooKeeper 的 /controller/id 节点中，并将新的 Kafka controller 的地址添加到 Kafka 集群的 broker 列表中。

等待新的 Kafka controller 加入到 Kafka 集群中，并与其他 Kafka controller 建立连接。Kafka controller 之间会通过 ZooKeeper 进行协调，以确保集群中只有一个 Kafka controller 在运行。

一旦新的 Kafka controller 加入到 Kafka 集群中，并与其他 Kafka controller 建立连接，就可以开始将请求分配给新的 Kafka controller。具体来说，Kafka 集群会将一部分分区的管理权交给新的 Kafka controller，以减轻原有 Kafka controller 的负担。

逐步将更多的分区的管理权交给新的 Kafka controller，直到新的 Kafka controller 能够处理所有的请求。同时，原有 Kafka controller 的管理权也会逐步减少，以避免新的 Kafka controller 过度负载。

例如，假设当前集群中有三个 Broker 节点，分别是 Broker 1、Broker 2 和 Broker 3。当需要扩容集群时，可以添加一个新的 Broker 节点 Broker 4，Controller 会将其加入到集群的 Broker 列表中，并将元数据信息同步到该节点。当 Broker 1 宕机时，Controller 会将其从集群的 Broker 列表中删除，并重新分配该节点上的副本到其他节点上。