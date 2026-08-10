# Ch6 Promise/Future 全视角验证

> 从多角色视角提问, 验证每问都能在 outline 中找到答案

---

## 开发者视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | result 字段怎么用一个 volatile 字段表示 5 种状态？SUCCESS 和 UNCANCELLABLE 是怎么表示的？ | 3.1 §3 |
| 2 | trySuccess 和 setSuccess 的区别是什么？什么场景该用 try？ | 3.1 §4 |
| 3 | addListener 的 "渐进升级" 是什么？为什么用 null→1→2→array 而不是直接创建数组？ | 3.1 §5 |
| 4 | MAX_LISTENER_STACK_DEPTH=8 — listener 递归通知为什么会导致 StackOverflow？ | 3.1 §6 |
| 5 | sync/await 的 checkDeadLock 什么条件下抛 BlockingOperationException？EventLoop 线程为什么不能 await？ | 3.1 §8 |
| 6 | cause() 为什么懒创建 CancellationException？不创建会怎样？ | 3.1 §9 |
| 7 | PromiseCombiner 只记第一个 cause — 多 Future 同时失败, 丢掉的 cause 在哪？ | 3.2 §3 |
| 8 | PromiseNotifier.cascade 的双向取消 — 什么场景会死循环？怎么防止？ | 3.2 §5 |

## 性能工程师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | DefaultFutureListeners 用数组存储 — remove O(n) arraycopy 在频繁 re-register 场景下性能如何？ | 3.1 §7 |
| 10 | safeExecute 包裹每个 listener — 如果 listener 抛异常, 后续 listener 还会被通知吗？ | 3.1 §6 |
| 11 | addListener 注册后若 Future 已完成 — 直接同步回调 vs 进 EventLoop 任务队列, 哪个更好？ | 3.1 §6 vs 3.2 §1 |

## SRE/运维视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 12 | 生产日志看到 BlockingOperationException — 是哪层代码出错了？怎么修？ | 3.1 §8 |
| 13 | isVoid() Future 禁用 addListener — 如果想给 VoidPromise 加 listener 怎么办？ | 3.3 §2 |

## 架构师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 14 | DefaultPromise.result 单字段 5 态 vs JDK CompletableFuture 多字段 — 设计取舍是什么？ | 3.1 §3 |
| 15 | PromiseTask 公开写全禁用 — 为什么不让外部手动 setSuccess？这和 DefaultPromise 有什么不同？ | 3.2 §6 |
| 16 | FlushCheckpoint 为什么不在 Promise 层而要在 ChannelPromise 层？和 Channel flush 机制有什么关系？ | 3.3 §3 |

## 学生/新人视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 17 | Netty Future 和 JDK Future 有什么区别？为什么不直接用 java.util.concurrent.Future？ | 3.1 §1 |
| 18 | addListener 和 await 都可以等结果 — 什么时候用哪个？ | 3.1 §8,10 |

---

## 覆盖统计

| 角色 | 问题数 | 覆盖 |
|------|:--:|:--:|
| 开发者 | 8 | 3.1§3-6,8-9, 3.2§3,5 |
| 性能工程师 | 3 | 3.1§6-7, 3.2§1 |
| SRE/运维 | 2 | 3.1§8, 3.3§2 |
| 架构师 | 3 | 3.1§3, 3.2§6, 3.3§3 |
| 学生/新人 | 2 | 3.1§1,8 |
| **合计** | **18** | **100%** |

## 缺口

无缺口 — 18 问全部能在 3 篇 outline 中找到答案。
