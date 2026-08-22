# Kafka-40. 分区到底怎么分给成员——Range、RoundRobin、Sticky、Uniform 分配算法主链

> 场景：Kafka-6/22 讲了 ConsumerGroup 两个协议，但分区分配器（Assignor）本身只是一笔带过。本篇正面讲 Kafka 的几种分区分配算法：Range、RoundRobin、Sticky、CooperativeSticky（Classic 协议下）和 UniformAssignor（Modern 协议下）。覆盖分配策略、粘性（sticky）与共分区（co-partitioning）的边界。

## 第一层：Classic 与 Modern 的 Assignor 位置不同

**Classic 协议下**：分配由 leader 成员在客户端调用 `AbstractPartitionAssignor.assign()` 执行，coordinator 只负责转发。**Modern 协议（KIP-848）下**：分配由 Coordinator 侧的 `ServerPartitionAssignor`（如 `UniformAssignor`）执行，成员只接收结果。

## 第二层：RangeAssignor

`RangeAssignor` 的核心不是“把所有分区拉平”，而是**按 topic 各自独立切片**。如果是同质订阅、成员 A 和 B 都订阅同一个 topic 且该 topic 有 10 个分区，那么它会把这个 topic 的分区按成员顺序切成两段，A 大致拿前半段、B 拿后半段；如果 topic 很多，这个过程会对每个 topic 分别重复。它的优点是局部简单，但缺点也来自这里：**每个 topic 都独立切片时，跨 topic 看总负载可能不均。**

## 第三层：RoundRobinAssignor

`RoundRobinAssignor` 把所有 topic 的分区列表拉平，按成员字典序轮询分配。解决了 Range 的不均匀问题，但**不保证粘性**：每次 rebalance 后成员的分配可能完全变化。

## 第四层：StickyAssignor

`StickyAssignor` 在保持分区分配尽量稳定（粘性）的同时，尽量让负载均衡。它记录上次分配结果，在本次 rebalance 时尽量保留已有的分配，只调整必须变化的部分。

## 第五层：CooperativeStickyAssignor

`CooperativeStickyAssignor` 是 Sticky 的增量版本，专门用于 Cooperative Rebalance：它允许部分成员先完成分配，不需要全组 stop-the-world。

## 第六层：UniformAssignor（Modern 协议）

`UniformAssignor` 是 KIP-848 下的服务端 assignor，运行在 coordinator 上。它不会只用一套算法硬算所有组，而是先判断订阅是 homogeneous（所有成员订阅相同 topic 集）还是 heterogeneous（不同成员订阅不同 topic），再分别走 `UniformHomogeneousAssignmentBuilder` 或 `UniformHeterogeneousAssignmentBuilder` 这两条路径；共同目标都是尽量均匀，但两类输入的处理约束并不相同。

## 收网

```text
Classic leader 分配
  → Range / RoundRobin / Sticky / CooperativeSticky

Modern coordinator 分配
  → UniformAssignor（homogeneous / heterogeneous）
```

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“分配一定在客户端完成。”** Modern 协议下由服务端 `UniformAssignor` 执行。
2. **“RoundRobin 一定均衡。”** 只是比 Range 好，仍可能不均。
3. **“Sticky 一定最均衡。”** 以粘性和均衡折中。
4. **“CooperativeSticky 只有集群分配用。”** 主要用于增量 rebalance。
5. **“Range 只按 topic 均分一定均匀。”** 成员数不能整除分区数时，少数成员会多分。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractPartitionAssignor.java:44`：Classic assignor 基类。
- `clients/src/main/java/org/apache/kafka/clients/consumer/CooperativeStickyAssignor.java:37`：CooperativeSticky 与 Sticky 的关系。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/assignor/UniformAssignor.java:53`：Modern 服务端 assignor。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/assignor/UniformHomogeneousAssignmentBuilder.java:36`：homogeneous 路径。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/assignor/UniformHeterogeneousAssignmentBuilder.java:38`：heterogeneous 路径。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/assignor/RangeAssignor.java:143`：服务端 Range 的 homogeneous 路径。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/assignor/RangeAssignor.java:185`：服务端 Range 的 heterogeneous 路径。
