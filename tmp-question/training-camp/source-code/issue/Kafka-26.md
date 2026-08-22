# Kafka-26. Controller 真正开始工作之前发生了什么——QuorumController 与元数据日志主链

> 场景：Kafka-8 讲过 KRaft 怎么通过 Raft 选出 metadata leader，但那时我们只停在“谁是 active controller”。本篇继续往前走：**active controller 得到资格后，真正是怎么处理写请求的？** 为什么 createTopic/alterPartition 不能立刻改内存、立刻返回？答案在 `QuorumController` 自己的事件队列和元数据日志主链里。

## 先把真正的困惑摆出来：为什么 createTopic 不能一收到就算成功

想象客户端发来 `createTopic` 或 `alterPartition` 请求。active controller 明明已经知道自己是 leader，为什么不直接：

```text
收到请求
  → 改内存里的 topicsImage / partition state
    → 立刻返回成功
```

这么做看似省事，但有两个致命问题：

1. **崩溃恢复**：如果内存改完、还没写进元数据日志 controller 就挂了，新 leader 根本看不到这次变更；
2. **脑裂风险**：如果 standby controller 也能这么干，两个 controller 同时改内存会得到两套不同状态。

所以 QuorumController 的第一原则是：**任何写请求都必须先变成日志记录，再以 commit 为成功边界。**

*关键设计（斜体）：* *QuorumController 把所有元数据写请求先排成 `ControllerWriteEvent`，由单线程事件队列执行；只有 active controller 才能生成 records 并写入元数据日志，真正的请求完成要等到 commit 推进到 `lastStableOffset`，`deferredEventQueue` 才会唤醒对应 Future。standby controller 不做写决策，只通过 commit / snapshot replay 追上状态。*[模式: 单线程事件队列 + 先日志后成功 + active/standby 分流]

## 第一层：appendWriteEvent——所有写请求先排队，不直接落盘

无论是 `createTopic`、`alterPartition` 还是其它 controller 变更，入口都是 `appendWriteEvent()`。

它做的事其实很少：

```text
appendWriteEvent(name, deadline, op)
  → new ControllerWriteEvent(name, op, flags)
    → queue.append(...) 或 appendWithDeadline(...)
      → 返回 event.future()
```

注意这里还没有生成任何元数据记录，也还没有写日志。它只做了一件事：**把请求变成一个等待执行的 event，丢进 controller 的单线程事件队列。**

这意味着 controller 的所有写入——无论业务上看起来多么不同——最终都统一成一种调度模型：先进队列，再由事件线程串行执行。这样才能保证没有两个写请求并发地改同一份元数据。

## 第二层：ControllerWriteEvent——先跑业务逻辑，得到 records 与返回值

真正处理写请求的是 `ControllerWriteEvent.run()`。它在执行时会先检查：当前 controller epoch 还是不是 active。

- 如果已经失去 active 身份，直接失败，不允许继续；
- 如果仍是 active，调用 `op.generateRecordsAndResult()` 得到一个 `ControllerResult<T>`。

`ControllerResult` 里有两样东西：

- **records**：要追加到元数据日志的记录；
- **response/result**：最终要返回给客户端的业务结果。

```text
ControllerWriteEvent.run()
  → still active?
    → yes: generateRecordsAndResult()
      → ControllerResult(records, response)
        → append to metadata log
```

这一步很关键：业务逻辑和日志记录在同一个事件里生成，天然绑定。不存在“先改内存、后想想要写什么记录”的两步。生成 records 的那一刻，就是对外可见状态的唯一候选事实。

## 第三层：写进 leader 本地日志还不够，要等 commit

即便 `ControllerWriteEvent` 成功把 records append 到 metadata log，也还**不能立刻**给客户端回成功。

因为 append 只是写进了 leader 本地日志，它还没有被多数派 controller quorum 确认。Leader 可能在 commit 之前就崩溃，那这条记录就不应该成为集群事实。

QuorumController 用 `deferredEventQueue` 记住每个待完成的 event 对应的 offset。只有当 commit 推进到 `lastStableOffset`，这条 event 才真正完成。

```text
append 成功
  → resultAndOffset = (offset, response)
    → deferredEventQueue.add(offset, event)
      → 等待 commit
```

这就是为什么 Kafka-8 里说“append 本地日志 ≠ committed”。QuorumController 的写请求成功边界不是 leader append，而是 quorum commit。

## 第四层：handleCommit 在 active 与 standby 上走不同路径

`QuorumMetaLogListener.handleCommit()` 收到一批已提交的 records 后，会先问：**当前自己是不是 active controller？**

### active controller

如果自己是 active，说明这些 records 在最初处理请求时就已经执行过业务逻辑了。所以这里**不需要 replay 记录**。它只需要：

- 推进 committed/stable offset；
- 用 `deferredEventQueue.completeUpTo(offsetControl.lastStableOffset())` 唤醒已经安全 commit 的 event。

```text
active handleCommit
  → offsetControl.handleCommitBatch(batch)
    → deferredEventQueue.completeUpTo(lastStableOffset)
      → event.future complete
```

### standby controller

如果自己是 standby，情况完全不同：这些 records 不是自己生成的，自己内存里还没有对应状态。所以它必须逐条 replay：

```text
standby handleCommit
  → 遍历 batch.records()
    → replay(message, offset)
      → 重建本地 controller 状态
```

这就是 active 与 standby 的根本分工：**active 负责生成记录，standby 负责重放记录。** 两者都消费 commit，但行为不同。

## 第五层：为什么真正的成功边界是 lastStableOffset

`deferredEventQueue.completeUpTo(offsetControl.lastStableOffset())` 而不是 `lastCommittedOffset()`，这不是随便选的。

`lastStableOffset` 代表：到这个 offset 为止的元数据记录已经足够稳定，适合对外完成依赖它们的请求。它比“已经被写入 leader 本地 log”更强，比“仅仅被 commit 到某个过渡点”更安全。

对客户端来说，这条设计意味着：

```text
createTopic 请求返回成功
  ≠ active controller 刚生成了记录
  = 对应的 metadata records 已经 commit 并达到 stable 边界
```

这保证了未来无论 controller 如何切换，新的 active controller 都必须承认这次请求已经成功。

## 第六层：handleLeaderChange——controller 身份切换与 failover

Raft 选主变化时，`handleLeaderChange()` 会更新 `curClaimEpoch`。这里有三种情况：

1. **我们原来是 active，现在还是 active**：更新 epoch，继续处理；
2. **我们原来是 active，但新 leader 不是自己**：放弃 active 身份，`offsetControl.deactivate()`，并把 `deferredEventQueue` 里的所有等待请求 fail 掉；
3. **我们原来是 standby，现在成为新 leader**：激活 `offsetControl`，进入 active controller 身份。

```text
handleLeaderChange(newLeader)
  → 当前是否 leader?
    → 是：curClaimEpoch = new epoch，继续 active
    → 否：deactivate + failAll pending events
```

第二种情况最重要：如果旧 active 在 commit 之前失去领导权，那些还没到 stable offset 的请求必须失败，否则客户端会看到“旧 leader 说成功，新 leader 不承认”的矛盾状态。

## 第七层：handleLoadSnapshot——standby 的快速追赶路径

除了逐条 replay commit，standby controller 还有另一条追赶路径：`handleLoadSnapshot()`。

当从快照加载时：

- 如果自己还是 active controller，直接报错——active 不应该通过 snapshot 覆盖自己正在工作的状态；
- 如果自己是 standby，就从 snapshot 中读取 batches，逐条 replay，更新本地元数据状态；
- `offsetControl.beginLoadSnapshot(...)` / `endLoadSnapshot(...)` 用于对齐恢复过程中的 offset 状态。

这条路径让 standby 不需要从 offset 0 逐条追日志，而是可以从 snapshot 基线开始，快速追上最新状态。

## 收网：Controller 请求先变成日志，commit 后才算成功

把整篇压成一句话：QuorumController 收到任何元数据写请求后，都先通过 `appendWriteEvent` 把它变成 `ControllerWriteEvent` 排进单线程队列；只有 active controller 才能生成 records 并 append 到元数据日志，event 的 Future 被放进 `deferredEventQueue` 等待 commit；`handleCommit` 在 active 上只推进 offset 并完成 future，在 standby 上真正 replay 记录；`handleLeaderChange` 则负责切 active / standby 身份并在 failover 时清理未完成事件。

```text
请求
  → appendWriteEvent
    → ControllerWriteEvent
      → generateRecordsAndResult
        → append 到 metadata log
          → deferredEventQueue 等待 commit
            → handleCommit（active: 完成 future / standby: replay）
              → lastStableOffset → 请求真正成功
```

到这里，主线只发生了六件事。

第一，所有写请求先入 `ControllerWriteEvent` 队列。

第二，只有 active controller 才能生成 records。

第三，append 本地日志不等于对外成功。

第四，真正的成功边界是 `lastStableOffset`。

第五，active 与 standby 在 `handleCommit` 上走不同路径。

第六，leader 变化时 `handleLeaderChange` 负责身份切换与未完成事件清理。

**本篇的一句话困惑**：Controller 拿到 active 身份后，为什么还不能立刻改内存并返回成功？

**本篇的一句话顿悟**：因为 controller 的每次写入都必须先变成 metadata log records，再等到 commit 达到 stable 边界，才由 `deferredEventQueue` 完成请求；active 负责生成，standby 负责重放。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“appendWriteEvent 就是直接写日志。”** 它先入事件队列。
2. **“active controller commit 时也要 replay。”** active 只推进 offset 并完成等待中的 Future。
3. **“append 到 leader 本地日志就算成功。”** 要等 commit 达到 stable 边界。
4. **“standby controller 什么都不做。”** 它持续 replay commit / snapshot 追状态。
5. **“leader 变化只影响 Raft 层。”** 它还会切换 QuorumController 的 active 身份并 fail 未完成请求。

### 关键证据清单

- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:931`：appendWriteEvent。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:743`：ControllerWriteEvent。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:956`：handleCommit。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1010`：handleLoadSnapshot。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1056`：handleLeaderChange。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1104`：isActiveController。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1335`：deferredEventQueue。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1340`：offsetControl。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 QuorumController 自己的执行链，不展开 broker 侧 MetadataLoader（K-8 第 3 篇）。
- 不把 active 与 standby 的 commit 路径混成同一个流程。
- 不展开所有 Controller API，只抓写请求共性。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-8`（KRaft 选主与 MetadataImage 总览）、`Kafka-24/25`（Controller 如何做状态变更）。
- 后续桥接：下一篇可进入 K-8 域第 3 篇（MetadataLoader / MetadataImage / Publisher），或继续 K-9 ISR 域子专题。