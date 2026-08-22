# Kafka-26 重写规划

> 题目：Controller 真正开始工作之前发生了什么——QuorumController 与元数据日志主链
> 状态：K-8 KRaft 域第 2 篇，按“QuorumController 与元数据日志”展开
> 目标：解释 QuorumController 如何把业务请求变成元数据日志记录：`appendWriteEvent` 的事件队列、`ControllerWriteEvent` 的 deferred 完成、active controller 才能写、commit 后才完成 Future 的语义；以及 `handleCommit` / `handleLoadSnapshot` / `handleLeaderChange` 如何在 active 与 standby 模式下分别推进状态。

## 1. 读者困惑

- QuorumController 收到 createTopic / alterPartition 请求后，先做什么？
- 为什么必须先排队成 `ControllerWriteEvent`，不能直接改内存？
- active controller 和 standby controller 在 commit 时分别做什么？
- 请求成功返回的时机是什么——写入本地日志就算，还是要等 commit？
- leader 变化时，QuorumController 如何切 active / standby 身份？
- snapshot 加载时为什么 active controller 不允许走 loadSnapshot 逻辑？

## 2. 一句话顿悟

**QuorumController 把所有元数据写请求先排成 `ControllerWriteEvent`，由单线程事件队列执行；只有 active controller 才能生成 records 并写入元数据日志，真正的请求完成要等到 commit 到 `lastStableOffset` 之后，`deferredEventQueue` 才会唤醒等待中的 Future。standby controller 不做写决策，只在 `handleCommit` / `handleLoadSnapshot` 中 replay 记录追上状态。**

## 3. 五要素卡片

### 读者问题

客户端发出 `createTopic` 请求到 active controller 后，为什么不能立刻返回，而要等元数据日志 commit 才算成功？

### 入口

- `QuorumController.appendWriteEvent`：把请求包装成 `ControllerWriteEvent` 入队
- `ControllerWriteEvent`：生成 `ControllerResult`、等待 commit 后完成 Future
- `deferredEventQueue`：按 offset 等待 commit
- `offsetControl`：跟踪 nextWriteOffset / lastCommittedOffset / lastStableOffset
- `QuorumMetaLogListener.handleCommit`：收到 commit batch 后推进状态
- `QuorumMetaLogListener.handleLoadSnapshot`：standby controller 加载快照
- `QuorumMetaLogListener.handleLeaderChange`：切 active / standby 身份

### 状态核心

- `curClaimEpoch`：当前 active controller 所在的 Raft epoch
- `isActiveController()`：active/standby 判断
- `ControllerWriteEvent.resultAndOffset`：本次写操作对应的提交 offset
- `lastCommittedOffset` / `lastStableOffset`：commit 进度
- `deferredEventQueue.completeUpTo(lastStableOffset)`：批量完成等待中的请求

### 失败路径

- 非 active controller 收到写请求 → 必须拒绝，否则脑裂
- active controller 先改内存后写日志 → 崩溃时状态不可恢复
- commit 前就返回成功 → 新 leader 可能看不到这条记录
- standby controller 跟 active 一样重放 write path → 重复执行副作用

### 连接点

- 前文 `Kafka-8`：KRaft 选主与 commit 总览，本篇补上 QuorumController 自己的执行链。
- 前文 `Kafka-24/25`：Controller 状态变更最终都要经过 appendWriteEvent 写入日志。
- 后文：K-8 第 3 篇（MetadataImage / Loader / Publisher）接上 commit 后 broker 如何消费元数据。

## 4. 总图

```text
客户端请求（如 createTopic / alterPartition）
  → appendWriteEvent
    → ControllerWriteEvent 入队
      → 单线程执行 op.generateRecordsAndResult()
        → 生成 ControllerResult(records, response)
          → append 到元数据日志
            → deferredEventQueue 记住 offset
              → commit 到 lastStableOffset
                → handleCommit
                  → deferredEventQueue.completeUpTo(lastStableOffset)
                    → Future 完成，客户端收到成功
```

## 5. 关键边界

- 本篇不重复 Raft 投票/leader 选举（K-8 第 1 篇），只讲 controller 自己如何消费元数据日志。
- 不把 `appendWriteEvent` 写成“直接写日志”：它先入事件队列，再由 ControllerWriteEvent 决定写入。
- 不把 active 与 standby 的 commit 路径写成一样：active 只推进 committed/stable offset，standby 真正 replay 记录。
- 不展开所有 Controller API，只抓 createTopic / alterPartition 这类写请求共性。

## 6. 失败方案推演

1. **所有写请求直接改内存**：崩溃后新 leader 看不到这次变更。
2. **standby 也执行写路径**：脑裂，重复变更。
3. **append 本地日志就返回**：commit 前 leader 崩溃，请求对外成功但集群无记录。
4. **不区分 committed 和 stable offset**：事务性元数据无法安全完成。

## 7. 误解清单

- “appendWriteEvent 就是直接写日志。”：它先入事件队列。
- “active controller commit 时也会 replay 记录。”：active 只推进 offset，不重复 replay。
- “请求写进本地日志就算成功。”：要等 commit 到 stable offset。
- “standby controller 什么都不做。”：它通过 commit/snapshot replay 追状态。
- “leader 变化只影响 Raft 层。”：也会切换 QuorumController 的 active 身份。

## 8. 证据清单

- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:931`：appendWriteEvent。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:743`：ControllerWriteEvent。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:956`：handleCommit。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1010`：handleLoadSnapshot。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1056`：handleLeaderChange。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1104`：isActiveController。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1335`：deferredEventQueue。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1340`：offsetControl。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 QuorumController 自己的执行链，不展开 broker 侧 MetadataLoader（K-8 第 3 篇）。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"createTopic 为何不能立刻返回"开场。
2. 否定直接改内存、standby 也写、append 就成功三种方案。
3. 解释 appendWriteEvent → ControllerWriteEvent 入队。
4. 解释 active controller 生成 records 与等待 commit。
5. 解释 handleCommit 在 active/standby 上的区别。
6. 解释 handleLoadSnapshot / handleLeaderChange 的作用。
7. 收网：controller 请求先变成日志，commit 后才算成功。