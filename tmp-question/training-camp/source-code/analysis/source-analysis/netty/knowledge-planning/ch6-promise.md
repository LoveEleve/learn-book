# Ch6 Netty Promise/Future — 知识规划

> 来源: 22 源文件 | ~3000 行 | common/src/main/java/io/netty/util/concurrent/
> 基线: Ch5 EventLoop 回答了 "什么时候读写" — Ch6 回答 "异步结果怎么传递"

---

## 01 提取 — 逐源映射

### 01.1 核心接口 (2 文件 — 17 KPs)

#### Future.java + Promise.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Future.java:isSuccess/isDone/isCancellable/isCancelled | **三态判定**: isSuccess/isDone(cancel+failure都是done)/isCancellable | High |
| Future.java:cause() | **cause(): 失败→throwable, cancel→CancellationException, 否则→null** — 三态映射 | High |
| Future.java:addListener/removeListener | **Listener 注册/移除**: GenericFutureListener 回调链基础 | High |
| Future.java:sync/await | **阻塞等待: sync 抛异常(失败时), await 静默等待** | High |
| Future.java:getNow | **getNow(): done→result, else→null** — 非阻塞读取, 不保证 happens-before | High |
| Promise.java:setSuccess/setFailure | **set: 强制写入, 已 done 抛 IllegalStateException** | High |
| Promise.java:trySuccess/tryFailure | **try: CAS 尝试写入, 已 done 返回 false** | High |
| Promise.java:setUncancellable | **setUncancellable: 标记为不可取消, 在 run() 之前调用** | High |

### 01.2 DefaultPromise (1 文件 — 25 KPs)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| DefaultPromise.java:result 字段 | **5 态编码**: null(未完成)/SUCCESS/UNCANCELLABLE/CauseHolder(失败)/值(成功) | High |
| DefaultPromise.java:setSuccess0/setFailure0 | **CAS 原子状态转换**: RESULT_UPDATER.compareAndSet — 只第一个成功 | High |
| DefaultPromise.java:addListener0 | **渐进升级**: null→1 listener→2 listeners→DefaultFutureListeners 数组 | High |
| DefaultPromise.java:MAX_LISTENER_STACK_DEPTH=8 | **递归保护**: 超过 8 层嵌套通知时退化为 safeExecute+async | High |
| DefaultPromise.java:notifyListeners | **早监听: register→check done→已完成→立即通知(在 addListener 线程)** | High |
| DefaultPromise.java:checkNotifyWaiters | **sync/await 实现: Object.wait/notifyAll + checkDeadLock** | High |
| DefaultPromise.java:checkDeadLock | **死锁检测: inEventLoop 且已注册的 Channel 上 await → 抛 BlockingOperationException** | High |
| DefaultPromise.java:cause() | **懒创建 CancellationException**: 缓存预填堆栈帧, 避免 fillInStackTrace | High |
| DefaultPromise.java:progressiveListeners | **progressiveSize 独立计数**: 区分普通+进度 listener, notify 时分发 | High |

### 01.3 监听器系统 (2 文件 — 12 KPs)

#### GenericFutureListener.java + DefaultFutureListeners.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| GenericFutureListener.java:operationComplete | **单方法接口: extends EventListener — 完成时回调** | High |
| DefaultFutureListeners.java:listeners 数组 | **数组存储: add O(1)尾部追加, remove O(n)逐个找+arraycopy** | High |
| DefaultFutureListeners.java:progressiveSize | **进度监听器独立计数: 仅 GenericProgressiveFutureListener 计数** | High |
| DefaultFutureListeners.java:notifyListeners | **safeExecute: try-catch 包裹每个 listener, 异常不中断后续通知** | High |

### 01.4 不可变 Future (3 文件 — 15 KPs)

#### CompleteFuture.java + SucceededFuture.java + FailedFuture.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| CompleteFuture.java:addListener | **立即通知: 已完成 Future 的 addListener 在注册线程同步回调** | High |
| CompleteFuture.java:removeListener | **NOOP: 已完成 Future 不需要维护 listener 列表** | High |
| CompleteFuture.java:await/sync | **立即返回: 已完成 Future 不需要等待** | High |
| SucceededFuture.java:isSuccess=true, cause=null | **成功 Future: sync 返回 this, getNow 返回 result** | High |
| FailedFuture.java:isSuccess=false, cause 非 null | **失败 Future: sync 抛异常, getNow 返回 null** | High |

### 01.5 组合器 (4 文件 — 25 KPs)

#### PromiseCombiner + PromiseAggregator + PromiseNotifier + UnaryPromiseNotifier

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| PromiseCombiner.java:三阶段 | **add→finish→aggregate: expectedCount 累计→lock aggregate→notify** | High |
| PromiseCombiner.java:operationComplete0 | **只记第一个 cause: 不收集全部, 多失败下 winner 是 undefined** | High |
| PromiseCombiner.java:checkInEventLoop | **显式线程绑定: EventExecutor.inEventLoop 强制检查** | High |
| PromiseAggregator.java:failPending | **级联失败: 任一失败→遍历 pendingPromises 全部 setFailure** | High |
| PromiseNotifier.java:cascade | **双向取消传播: promise 取消→future.cancel+future 完成→promise 通知** | High |
| PromiseNotifier.java:防御性复制 | **promises.clone(): 构造时复制数组防止外部修改** | Medium |

### 01.6 Scheduled + Task (2 文件 — 17 KPs)

#### ScheduledFutureTask.java + PromiseTask.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ScheduledFutureTask.java:period>0 | **fixed-rate: deadlineNanos += period — 不受执行时间影响, 追赶式** | High |
| ScheduledFutureTask.java:period<0 | **fixed-delay: deadlineNanos = nanoTime() - period — 从执行完成算** | High |
| ScheduledFutureTask.java:compareTo | **PriorityQueue 排序: deadlineNanos+id(同期限时用 id 打破同值** | High |
| PromiseTask.java:Runnable+Promise | **桥接并发: 同时实现 RunnableFuture 和 DefaultPromise** | High |
| PromiseTask.java:哨兵模式 | **COMPLETED/CANCELLED/FAILED Runnable 替换原始 task — 防重复执行** | High |
| PromiseTask.java:公开写禁用 | **setSuccess/trySuccess 全部 throw — 只有内部执行路径可写** | High |

### 01.7 Channel 层 (3 文件 — 20 KPs)

#### ChannelFuture.java + ChannelPromise.java + DefaultChannelPromise.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ChannelFuture.java:channel() | **Channel 关联: channel() 绑定操作 Channel, I/O Future 的上下文** | High |
| ChannelPromise.java:FlushCheckpoint | **FlushCheckpoint 集成: DefaultChannelPromise 参与 Channel flush 协调** | High |
| DefaultChannelPromise.java:executor() | **executor 后退: null→channel().eventLoop(), 通知在正确 event loop** | High |
| DefaultChannelPromise.java:checkDeadLock | **条件死锁检查: 仅 channel().isRegistered() 时检查 await 死锁** | High |

### 01.8 Progressive (3 文件 — 12 KPs)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ProgressivePromise.java:setProgress/tryProgress | **进度写入: total=-1 未知总量, progress 是累计值非增量** | High |
| GenericProgressiveFutureListener.java | **进度回调: operationProgressed(F, progress, total), 独立于完成回调** | High |
| DefaultProgressivePromise.java:total 标准化 | **total<0→-1: 已知限制只检查 progress>=0, 未知限制跳过<=total 检查** | High |

---

## 01 聚合

N=22, P1=≥5, P2=3-4, P3=1-2

### P1 — 全系统共识

| Knowledge Point | 出现文件 |
|----------------|---------|
| **Future.isSuccess/isDone/cause/addListener** | Future + Promise + DefaultPromise + Complete + Succeeded + Failed + Channel + Progressive |
| **Promise.setSuccess/trySuccess/setFailure/tryFailure** | Promise + DefaultPromise + DefaultChannelPromise + DefaultProgressivePromise + PromiseTask |
| **DefaultPromise.result 5 态编码** | DefaultPromise + 全子类继承 |
| **addListener 渐进升级 + notifyListeners** | DefaultPromise + CompleteFuture + DefaultFutureListeners + Channel |
| **sync/await + checkDeadLock** | Future + DefaultPromise + CompleteFuture + DefaultChannelPromise |
| **cause() 懒 CancellationException** | DefaultPromise + CompleteFuture + SucceededFuture + FailedFuture |

### P2 — 局部重要

| Knowledge Point | 出现文件 |
|----------------|---------|
| **PromiseCombiner add→finish→aggregate 三阶段** | PromiseCombiner |
| **PromiseNotifier.cascade 双向取消** | PromiseNotifier + UnaryPromiseNotifier |
| **ScheduledFutureTask fixed-rate vs fixed-delay** | ScheduledFutureTask |
| **PromiseTask 哨兵防重复执行** | PromiseTask |
| **FlushCheckpoint 集成** | DefaultChannelPromise |

### P3 — 独立

| Knowledge Point | 出现文件 |
|----------------|---------|
| **progressiveSize 独立计数** | DefaultFutureListeners |
| **MAX_LISTENER_STACK_DEPTH=8** | DefaultPromise |
| **PromiseAggregator failPending 级联失败** | PromiseAggregator |
| **ChannelFuture.checkDeadLock 条件检查** | DefaultChannelPromise |

---

## 02 深度分类

### 🔴 Deep (承载核心设计决策)

| KP | 为什么🔴 |
|----|---------|
| **result 字段 5 态编码** | 单字段编码 5 种状态 — Netty 最巧妙的位域设计, 避免多 volatile 字段的同步成本 |
| **addListener0 渐进升级 (null→1→2→array)** | 空间换时间 — 99% 场景只有 1-2 个 listener, 无需预分配数组 |
| **MAX_LISTENER_STACK_DEPTH 递归保护 + safeExecute** | 8 层通知后异步化 — 防止 addListener 在 notify 中注册新 listener 导致 StackOverflow |
| **checkDeadLock: inEventLoop+已注册→BlockingOperationException** | 死锁防护 — EventLoop 线程 await 自己会永久阻塞 |

### 🟡 Working (有设计决策, 非核心)

| KP | 说明 |
|----|------|
| **PromiseCombiner 只记第一个 cause** | 简化聚合 — 不收集全部 causes |
| **ScheduledFutureTask period>0/<0 双算法** | fixed-rate 追赶 vs fixed-delay 精确间隔 |
| **PromiseTask 哨兵防重复** | 周期重调度时防重复执行 |
| **cause() 懒创建共享堆栈** | 内存优化 — CancellationException 不调用 fillInStackTrace |
| **PromiseNotifier.cascade 双向取消** | 取消链传播 |
| **ChannelPromise FlushCheckpoint** | flush 协调增量通知 |

### 🟢 Surface (机制性了解即可)

| KP | 放在哪 |
|----|-------|
| **CompleteFuture 立即通知** | 和不可变 Future 一起 |
| **PromiseAggregator failPending** | 和 PromiseCombiner 对比 |
| **UnaryPromiseNotifier** | 和 PromiseNotifier.cascade 对比 |
| **SucceededFuture/FailedFuture sync 行为** | 和 CompleteFuture 一起 |

---

## 03 聚类

### Cluster A: 状态模型与 Listener 通知链 (14 KPs) — 零前置

1. result 字段 5 态编码 (null/SUCCESS/UNCANCELLABLE/CauseHolder/value)
2. setSuccess0/setFailure0 CAS 原子转换
3. trySuccess/tryFailure vs set 语义差异
4. isSuccess/isDone/cause() 三态查询
5. addListener0 渐进升级 (null→1→2→array)
6. notifyListeners: 早监听(注册时若 done 立即通知)
7. MAX_LISTENER_STACK_DEPTH=8 + safeExecute
8. progressiveSize 独立计数
9. removeListener O(n) arraycopy
10. sync/await + checkDeadLock
11. checkNotifyWaiters: Object.wait/notifyAll
12. cause() 懒创建 CancellationException
13. addListener 优于 await 最佳实践
14. isVoid() 短路 Future

### Cluster B: 不可变 Future + 组合器 (10 KPs) — 依赖 A

1. CompleteFuture: addListener 立即通知, removeListener NOOP
2. SucceededFuture/FailedFuture: sync 抛异常 vs 返回
3. PromiseCombiner 三阶段: add→finish→aggregate
4. PromiseCombiner 只记第一个 cause
5. PromiseCombiner.checkInEventLoop 线程绑定
6. PromiseAggregator failPending 级联失败
7. PromiseNotifier.cascade 双向取消传播
8. PromiseNotifier 防御性复制 promises
9. PromiseNotifier 取消死循环防护(双取消检测)
10. PromiseTask 哨兵防重复 + Runnable 桥接

### Cluster C: Channel 层 + Scheduled + Progressive (10 KPs) — 依赖 A

1. ChannelFuture.channel() 关联
2. DefaultChannelPromise.executor() 后退到 channel.eventLoop
3. DefaultChannelPromise.checkDeadLock 条件检查(仅注册后)
4. FlushCheckpoint 进度追踪
5. ChannelPromise.setSuccess() 无参便捷
6. ChannelPromise.unvoid() 解包
7. ScheduledFutureTask fixed-rate(period>0) vs fixed-delay(period<0)
8. ScheduledFutureTask PriorityQueue compareTo(deadline+id)
9. ProgressivePromise.setProgress/tryProgress + total 标准化
10. GenericProgressiveFutureListener operationProgressed 回调

### 教学顺序: A → B → C
