# Kafka-10 重写规划

> 题目：为什么 Kafka 不立刻返回，而要把请求挂起来——Purgatory、DelayedProduce、DelayedFetch 与 TimingWheel 主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka 的“等待条件满足再返回”是如何统一实现的：为什么 `acks=all` 的 produce、`fetch.min.bytes` / `maxWaitMs` 的 fetch 不能简单睡眠等待；`DelayedOperation` / `DelayedOperationPurgatory` 如何把条件等待和超时剥离；`DelayedProduce`、`DelayedFetch` 又如何把副本确认和最小字节数这两类条件接到同一套机制上。

## 1. 读者困惑

- `acks=all` 的 produce 为什么不能在请求线程里 while 循环等 ISR 追上？
- `fetch.min.bytes` / `maxWaitMs` 为什么不是简单 sleep 一会儿再读？
- Kafka 怎么同时支持“条件一满足就立刻唤醒”和“条件一直不满足也要超时返回”？
- 一个请求可能依赖多个分区/多个 key，Kafka 怎么把它挂到正确的触发点上？
- 为什么 Kafka 要用 TimingWheel，而不是一个普通的优先队列定时器？

## 2. 一句话顿悟

**Kafka 把“等条件满足再返回”抽成了统一的延迟操作模型：请求线程先把 Produce/Fetch 包装成 `DelayedOperation`，尝试立即完成；如果条件还不够，就把它按 key 挂到 `DelayedOperationPurgatory` 的 watcher 列表，同时放进 TimingWheel 管理超时。之后只要相关 key 上的状态发生变化，就用 `checkAndComplete` 触发重试；要是一直等不到条件，就由定时器到期强制完成。**

## 3. 五要素卡片

### 读者问题

为什么 `acks=all` 与 `fetch.min.bytes` 这两类看似完全不同的等待需求，在 Kafka 里能用同一套机制解决？

### 入口

- `DelayedOperation`：统一的“可重试 + 可超时 + 只完成一次”抽象
- `DelayedOperationPurgatory`：按 key 挂请求，按事件/超时唤醒
- `DelayedProduce`：等待 enough replicas / leader 状态变化
- `DelayedFetch`：等待 minBytes / epoch 分叉 / leader 变化
- `TimingWheel` / `SystemTimer`：超时管理
- `checkAndComplete` / `tryCompleteElseWatch`：事件触发与超时触发的接合点

### 状态核心

- operation 是否已 completed
- watcher lists：按 `DelayedOperationKey` 建立的触发列表
- timeout queue：由 timer/timing wheel 管理的超时任务
- `tryComplete()` / `forceComplete()` / `onComplete()` / `onExpiration()`

### 失败路径

- 请求线程同步阻塞等待 → 网络线程 / IO 线程被卡死
- 只靠 sleep 超时轮询 → 条件提前满足也不能及时返回
- 只有事件触发、没有超时 → 永远等不到的请求泄漏
- 一个请求只挂一个 key → 多分区场景漏唤醒

### 连接点

- 前文 `Kafka-7`/`Kafka-9`：produce 是否满足 `acks=all` 依赖 ISR/HW；fetch 是否满足返回依赖 leader log / diverging epoch。
- 前文 `Kafka-2`：Producer 感知到的“等副本确认”在 broker 侧落成 DelayedProduce。
- 前文 `Kafka-4`：Consumer 感知到的 long poll 在 broker 侧落成 DelayedFetch。

## 4. 总图

```text
请求到达 broker
  → 构造 DelayedOperation 子类（produce/fetch）
    → tryCompleteElseWatch
      → 条件已满足：直接 forceComplete → onComplete
      → 条件未满足：按 key 挂进 watcher list + 加入 TimingWheel

后续两种出路
  1. 相关 key 状态变化
     → checkAndComplete(key)
       → 再次 tryComplete
         → 满足则 onComplete
  2. 超时到期
     → timer 触发 run()
       → forceComplete + onExpiration
```

## 5. 关键边界

- 本篇只讲“等待条件满足”的统一机制，不回头展开 ISR/HW 细节（Kafka-9）或网络线程模型（后续可单独讲）。
- 不把 Purgatory 理解成“请求队列”：它是“条件等待 + 超时管理”的中间层。
- 不把 watcher list 与 timer 混成一个结构：前者响应事件，后者负责超时。
- 不把 `DelayedOperation` 视为普通 future：它是带有幂等完成语义的定时任务抽象。

## 6. 失败方案推演

1. **请求线程 while 循环等条件**：把处理线程阻塞住，吞吐崩掉。
2. **纯 sleep/poll 重试**：条件提前满足也要等到下一轮轮询，延迟抖动大。
3. **只在事件变化时唤醒、没有 timeout**：永远等不到的请求泄漏。
4. **每个请求只监听一个分区 key**：多分区 produce/fetch 的任一关键分区变化都可能漏掉。 

## 7. 误解清单

- “Purgatory 就是一个延迟队列。”：它同时有 watcher lists 与 timeout timer 两套触发机制。
- “条件满足时一定靠定时器醒来。”：多数请求是被 `checkAndComplete` 事件驱动提前唤醒的。
- “`acks=all` 与 long poll 是两套完全不同实现。”：它们只是 `DelayedOperation` 的两个子类。
- “操作一旦进了 purgatory 就只能等超时。”：任何相关 key 上的事件都能提前触发再检查。
- “TimingWheel 只是为了省内存。”：核心收益是大量定时任务下的低插入/删除成本。

## 8. 证据清单

- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperation.java:24`：DelayedOperation 抽象与完成语义。
- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperation.java:60`：`forceComplete()` 只完成一次。
- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperationPurgatory.java:122`：`tryCompleteElseWatch()`。
- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperationPurgatory.java:184`：`checkAndComplete()`。
- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperationPurgatory.java:409`：超时 reaper。
- `server-common/src/main/java/org/apache/kafka/server/util/timer/TimingWheel.java:22`：分层时间轮设计与复杂度。
- `core/src/main/scala/kafka/server/DelayedProduce.scala:57`：DelayedProduce。
- `core/src/main/scala/kafka/server/DelayedProduce.scala:89`：produce 满足条件检查。
- `core/src/main/scala/kafka/server/DelayedFetch.scala:50`：DelayedFetch。
- `core/src/main/scala/kafka/server/DelayedFetch.scala:77`：fetch 满足条件检查。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 purgatory 框架、DelayedProduce/DelayedFetch、timer/watcher 双触发；不再深入 ISR/HW 本体。
- 目标正文：7000~11000 字；核心拆解层覆盖 delayed operation 抽象、tryCompleteElseWatch、事件唤醒、时间轮超时、produce/fetch 两类实例。

## 10. 本轮重写主线

1. 从“为什么不能直接等一会儿再返回”开场。
2. 否定 while 等待、sleep/poll 两种直觉方案。
3. 解释 `DelayedOperation` 的 once-only 完成语义。
4. 解释 `tryCompleteElseWatch` 如何把“先试一次、再挂表、最后再试一次”串起来。
5. 解释 `checkAndComplete(key)` 与 watcher list 的事件唤醒。
6. 解释 `TimingWheel` 的 timeout 路径。
7. 解释 `DelayedProduce` 与 `DelayedFetch` 如何共用这套机制。
8. 收网：Kafka 不是在等时间，而是在等条件；时间只负责兜底。