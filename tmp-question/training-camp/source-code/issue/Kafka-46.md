# Kafka-46. 消息为什么会“丢”——从 acks、maximalIsr、HW、LSO 到 marker 的排查路径

> 场景：最常见、也最容易被误判的问题之一，就是“Kafka 丢消息了”。但在源码语义里，这句话常常不精确：有时是 producer 并没真正拿到可靠成功；有时是消息已经 append 了，但还没到 HW；有时是事务没完成，`read_committed` 看不到；有时甚至不是丢，而是幂等/重试/隔离级别造成的可见性差异。本篇不做运维清单，而是把“消息像丢了一样”的几类现象，沿源码边界串成一条能落地的排查主链。

## 先把真正的困惑摆出来：producer 都返回成功了，为什么 consumer 还可能看不到

如果只站在应用层，很容易把“send 成功”理解成“消息已经稳定落盘、所有副本确认、任何消费者都可见”。

但 Kafka 里这至少拆成几层不同语义：

- producer 的成功语义，取决于 `acks`；
- broker 的副本确认，取决于 enough-replicas 判定；
- 普通 consumer 的可见性，通常会被 HW 这条稳定边界强烈约束，但真正返回还要叠加 fetch offset、leader epoch、bytes 条件等 fetch 语义；
- `read_committed` consumer 的可见性，还要再受 LSO 与事务 marker 约束。

*关键设计（斜体）：* *Kafka 里的“消息丢失”排查不能从一句“有没有写进去”结束，而必须沿四层边界往下看：producer 成功语义、leader 侧 enough-replicas 判定、log 可见边界（HW / LSO）、事务与消费隔离语义。只要其中任意一层没有满足，应用就可能主观感知为“消息丢了”。*[模式: 写入确认边界 + 副本稳定边界 + 日志可见边界 + 事务隔离边界]

## 第一层：先问 producer 到底拿到的是什么“成功”

排查第一步不是去看 consumer，而是先问：**producer 这次到底按什么语义判成功？**

- `acks=0`：几乎没有 broker 侧确认保障，应用层最容易把“发送出去了”误当“已经可靠写入”；
- `acks=1`：leader 本地 append 完即可返回，副本是否跟上还未成为成功条件；
- `acks=all`：leader 进入 `DelayedProduce`，等 `checkEnoughReplicasReachOffset` 判定 enough replicas 后再返回。

所以一上来就要把“producer 说成功”翻译成具体源码语义，而不是把三种 `acks` 混成一个成功概念。

## 第二层：`acks=all` 也不是魔法，它真正等的是 enough-replicas

如果是 `acks=all`，消息写到 leader 后不会立刻返回，而是进入：

```text
DelayedProduce.tryComplete()
  → partition.checkEnoughReplicasReachOffset(requiredOffset)
```

这里最关键的边界是：leader 看的不是一句抽象的“所有 ISR 都确认”，而是 `checkEnoughReplicasReachOffset(...)` 这套具体判定。

这套判定里真正重要的成员视图是 `partitionState.maximalIsr`。所以排查“明明 acks=all 还像丢了”时，要看的是：

- producer 返回前 enough-replicas 有没有真的成立；
- 期间 `maximalIsr` / `minISR` 是否触发了错误分支；
- leader 是否其实返回了成功前提没满足的异常，只是上层被吞掉或误读了。

## 第三层：消息“写进 leader”不等于“普通 consumer 一定马上读到”

即使 producer 已成功，consumer 仍然可能暂时看不到，这里就要转入日志可见边界。

对普通 consumer 来说，消息是否可见不能被偷换成“只看 HW 就行”。更准确地说：消息可能已经 append 到 leader logEndOffset，但 fetch 结果通常仍会受到 HW 这条稳定边界的强烈约束；与此同时，真正返回给 consumer 还要叠加 fetch offset、leader epoch / diverging epoch、`minBytes/maxBytes` 等 fetch 语义。

所以第二步排查常常是先把这三个边界分开，再回到具体 fetch 条件：

- `logEndOffset`：leader 物理上已经写到哪；
- `highWatermark`：当前已稳定到可安全读取的边界；
- `lastStableOffset`：如果有事务，`read_committed` 最多能读到哪。

只要把这三者混了，就会把“暂不可见”误判成“消息丢了”。

## 第四层：`read_committed` 看不到，很多时候不是丢，是 LSO 没过

如果 consumer 开了 `read_committed`，问题会再收紧一层：哪怕消息已经在 leader log 里，也不代表此时可见。

原因是 `read_committed` 不看普通 HW 边界，而是看 LSO（LastStableOffset）以及事务完成状态。

这意味着一条事务消息要真正被 `read_committed` 看见，至少要跨过几类相互关联的事务边界：

1. 事务消息成功 append；
2. coordinator 决定 COMMIT/ABORT；
3. marker 被发送到相关分区；
4. broker 侧 `ProducerStateManager.completeTxn(...)` 与 `UnifiedLog` 的事务完成/LSO 计算逻辑收敛；
5. fetch 时再结合 abort 事务索引过滤。

这里不要把它误读成“marker 扇出 → completeTxn → LSO”这样一条可以机械背诵的单线程流水线；更准确的理解是：**这些步骤共同决定 `read_committed` 的最终可见边界。**

所以有些“消息不见了”的根本原因，不是 broker 丢了，而是**事务相关边界还没收敛到可见状态**。

## 第五层：幂等/epoch 问题会制造另一种“像丢了一样”的错觉

有一类现象也常被误判成丢消息：生产侧重试后，应用发现“我以为发了两次，但只看到一次”或“后面的批次报错后前面的像没了”。

这时要看 `ProducerAppendInfo`：

- 旧 epoch 会触发 `InvalidProducerEpochException`；
- sequence 异常会触发 `OutOfOrderSequenceException`。

这里的本质不是 broker 把消息弄丢，而是 broker 在帮你**拒绝重复/乱序写入**。应用如果只盯“为什么没再写进去”，就会把正确的幂等保护感知成“丢失”。

## 第六层：把“真丢失”和“暂不可见”分开，排查路径才不会乱

从源码边界往下拆，常见现象其实应该被分成四类：

### 1) producer 从未拿到可靠成功
- 多见于 `acks=0/1`；
- 或者 `acks=all` 其实没有 enough replicas 成功。

### 2) broker 已写入，但普通 consumer 暂时还不可见
- 多见于 HW 尚未推进；
- 这不是“丢”，而是复制稳定边界还没过。

### 3) broker 已写入，但 `read_committed` 还不可见
- 多见于事务尚未 complete、marker 未收敛、LSO 未推进；
- 这不是“丢”，而是隔离级别在起作用。

### 4) broker 拒绝了写入重复/乱序
- 多见于幂等 epoch/sequence 问题；
- 这也不是“丢”，而是 broker 在防止错误写入。

一旦先把问题归到这四类，后面的日志与指标排查才有方向。

## 收网：Kafka 里“像丢消息”往往是边界没分清

把整篇压成一句话：Kafka 的“消息丢失”经常不是单点故障，而是对不同边界的混淆——producer 侧要先确认 `acks` 成功语义；`acks=all` 实际落到 `DelayedProduce` + `checkEnoughReplicasReachOffset`，关键成员视图是 `maximalIsr`；普通 consumer 的可见性要看 HW，`read_committed` 则要再看 LSO、marker 与 abort 索引；幂等 epoch/sequence 异常还会制造“像丢了一样”的假象。真正的排查路径必须沿“producer 成功 → enough replicas → HW/LSO → transaction visibility”逐层缩小。

```text
应用说“丢消息了”
  → 先看 producer 成功语义（acks）
    → acks=all ? 看 DelayedProduce / enough replicas / maximalIsr
      → 再看 leader LEO 与 HW
        → read_committed ? 再看 LSO / marker / abort 索引
          → 若报 epoch/sequence 异常，再转幂等重试路径
```

**本篇的一句话困惑**：Kafka 里为什么会出现“producer 成功了，但 consumer 像没收到”的现象，到底该怎么沿源码路径排查？

**本篇的一句话顿悟**：Kafka 的“像丢消息”常常不是消息真的消失，而是 producer 成功语义、副本稳定边界、HW/LSO 可见性、事务完成与幂等保护这几层边界里某一层没有满足；排查必须逐层拆开，而不能把“没读到”直接等同于“broker 丢了”。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“producer send 成功 = 任意 consumer 立刻可见。”** 成功语义、HW、LSO 是不同层。
2. **“`acks=all` 就绝不会出现像丢消息的现象。”** 还要看 enough-replicas 的真实判定与后续可见性边界。
3. **“HW 和 LSO 是一回事。”** HW 更偏普通稳定可读边界，LSO 还叠加事务稳定边界。
4. **“`read_committed` 看不到就是 broker 丢了。”** 很多时候只是事务/marker/LSO 还没收敛。
5. **“幂等报错导致没再写进去，就是 broker 吃消息。”** 常常是 broker 正在拒绝重复或乱序写入。

### 关键证据清单

- `core/src/main/scala/kafka/server/DelayedProduce.scala:89`：`DelayedProduce.tryComplete()`。
- `core/src/main/scala/kafka/cluster/Partition.scala:1089`：`checkEnoughReplicasReachOffset(...)`。
- `core/src/main/scala/kafka/cluster/Partition.scala:1093`：读取 `partitionState.maximalIsr`。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:107`：`InvalidProducerEpochException`。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerAppendInfo.java:116`：`OutOfOrderSequenceException`。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java:646`：LSO 计算入口。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/ProducerStateManager.java:545`：`completeTxn(...)`。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionCoordinator.scala:67`：创建 `TransactionMarkerChannelManager`。
- `core/src/main/scala/kafka/server/DelayedFetch.scala:88`：fetch 根据 isolation 选择可读 end offset。
- `core/src/main/scala/kafka/server/KafkaApis.scala:639`：fetch 响应设置 `lastStableOffset`。

### 版本与实现边界

- 本文以 Kafka `v4.x` 为基线。
- 本篇是源码 + 运维桥接篇：给的是排查主链，不展开监控平台、报警系统、外部重试框架。
- 不把“没读到”直接等同于“broker 物理丢失消息”。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-29`（maximalIsr）、`Kafka-33/35/38`（幂等/事务/可靠性总串联）。
- 后续桥接：下一篇可补 `Kafka-47` 容量评估，把吞吐、批量、segment、fetch、quota 串成估算路径。