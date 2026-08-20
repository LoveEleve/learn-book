# RD-4 篇1 — exec: 单命令执行与重试边界

> 前置: [[rd3-codec]] (codec 显式传参) + [[rd1-connection]] (连接池借还) | 复用: [[r28-networking]] (RESP 协议) | 对照: [[r2-events]] (Netty 事件循环) | 引出: [[RD-4-篇2]] (协议适配) + [[rd2-rlock]] (续期 noRetry 消费)
> 🔴 A | 3 KP | [模式: 汇聚适配 + 有界重试 + 超时分层]
> Pass 2 闭环: q1(异步执行管线) q2(重试协议) q3(超时三定时器)

**读者处境**: 你执行 `rBucket.get("key")` — 背后那条命令经过了什么?为什么说"响应超时绝不重试"而"连接失败要重试"?RedisExecutor 928 行里藏着一个三层超时和递归重试的协议。这篇拆命令从 API 到网络发送的最小单元: async() 汇聚点做什么适配, RedisExecutor 的执行链怎么串, 以及重试/超时的边界为什么这样定。

### 概念依赖链
q1(执行管线) ← q2(重试协议) ← q3(超时三定时) — 先讲"一条命令怎么走", 再讲"失败怎么重试", 最后讲"三个超时各自把守什么"。

### 核心悬念
"一条 get 命令背后：适配三步、执行五阶段、三个超时定时器、一个递归重试——它们怎么不打架？"

### 叙事顺序
1. 问题引入: rBucket.get 背后的那条命令
2. async 汇聚点 (q1) — resp3 适配 + SORT_RO 降级探测 + 缓存失效钩子 + RedisExecutor
3. 执行管线 (q1) — 借连接→sendCommand→响应→归还 的 CompletableFuture 链
4. 重试协议 (q2) — 定时器驱动递归; 连接/写失败可重试, 响应超时不重试
5. 超时三定时 (q3) — 连接池满/写堵/无响应 + 排障文案
6. 收束: "有界重试 + 类型化失败分路" — 每次失败都知道该不该再来一次

### 1. async 汇聚点 — 三重装配

场景: 为什么所有命令都过一个 async()?
源码路径:
- `CommandAsyncService.async(readOnlyMode, NodeSource, codec, command, params, ignoreRedirect, noRetry)` (CommandAsyncService.java:690-731):
  - L692: `resp3(command)` — RESP2/3 命令版本适配 (ServiceManager.java:689 映射表)
  - L694-714: **SORT_RO 降级探测** — readOnly+SORT+RO 支持 → SORT_RO; `ERR unknown command` → static AtomicBoolean=false + 递归降级 SORT (CommandAsyncService.java:705-708)
  - L717-727: **Client-Side Caching 失效钩子** — 写命令成功 → evictClientSideCaching (RD-6)
  - L728-731: `new RedisExecutor<>(...).execute()` — 移交单命令执行核心
- 数据流: 结构 API → async (适配) → RedisExecutor (执行) → CompletableFuture 回调
关键设计 (q1): 汇聚点把"命令语义" (结构 API) 与"执行机制" (RedisExecutor) 解耦, 中途拦下三类横切: 协议适配/能力降级/缓存失效。[模式: 汇聚适配]
数据流: getMap.get → async → resp3 → SORT_RO? → evict 钩子 → executor.execute。

### 2. 执行管线 — 借、发、响、还

场景: execute() 里那几步怎么串起来?
源码路径:
- `RedisExecutor.execute()` (RedisExecutor.java:122-230):
  - L128-130 addFuture / L132 shutdown 防呆
  - L139 `codec = getCodec(codec)` — 解析明确的 codec
  - L142 `getConnection(attemptPromise)` — 从连接池借 (RD-1)
  - L143-167 mainPromiseListener: 取消处理 (blocking 命令 forceFastReconnectAsync L162)
  - L177 `retryInterval = retryStrategy.calcDelay(attempt)` — jitter
  - L179-181 scheduleRetryTimeout + scheduleConnectionTimeout (双定时先挂入)
  - L183-213 connectionFuture 完成 → sendCommand → scheduleWriteTimeout → writeFuture listener
  - L215-224 attemptPromise 完成 → releaseConnection (归还) + checkAttemptPromise
关键设计 (q1): 三个 CompletableFuture (connection/attempt/write) 编织执行链, 任何阶段失败都能对号入座地失败/重试。[模式: 异步编织]
数据流: borrow → send → (writeFuture) → response → release。

### 3. 重试协议 — 递归 execute 与分型失败

场景: 什么失败该重试, 什么不该?
源码路径:
- `scheduleRetryTimeout` (RedisExecutor.java:278-375): 
  - L279: retryInterval==0 || attempts==0 → 不重试
  - L288-303 **blocking 命令**: 连接未得 → attempt++ 重排; 到顶超时
  - L305-340 (RedisExecutor.java): 连接失败/写未完成 → attempt==attempts? 超时 : **attempt++ 递归 execute** (RedisExecutor.java:359-371)
  - L342-349: 取消/关闭 → 快速失败
- 三失败类型:
  - **连接失败** → 重试 (递归新轮次)
  - **写失败** (checkWriteFuture L387-407) → WriteRedisConnectionException → 重试
  - **响应超时** (scheduleResponseTimeout) → 不重试 (服务端已执行, 重试=重复操作)
- **noRetry** (evalWriteNoRetryAsync L489-492): 锁续期等**幂等**操作禁重试 — 防止放大 (RD-2)
关键设计 (q2): 重试 = **定时器驱动的有界递归** (最多 attempts 次), 失败类型决定重试与否 (写可重试/响应不可, 防重复副作用)。[模式: 分型重试]
数据流: 连接失败 → CAS attempt → 新 execute; 响应超时 → 直接 fail。

### 4. 线程模型 — 回调在哪个线程跑

场景: 我的 thenAccept / whenComplete 在哪里执行?
源码路径 (completeness Q3 回填):
- 连接/写/响应 Future 由 **Netty EventLoop** 驱动完成 (RedisExecutor network 回调)
- 用户回调 `mainPromise.thenAccept(thenAccept(...))` (CommandAsyncService:723) 默认在**完成该 promise 的线程**执行 — 即 EventLoop
- 无自定义 executor: 异步 API 的回调都挂 EventLoop, **不要在里面做阻塞操作** (写超时文案反复警告 "blocking invocations in async/reactive/rx listeners")
- 同步 API (`get()/join()`) 由调用方线程阻塞等待 future — `CommandAsyncService.get(RFuture)` (CommandAsyncService.java:181-229): get()/getInterrupted() 双入口, 内部 RFuture.toCompletableFuture().get() 阻塞, 异常包装
关键设计: 回调线程 = 完成方线程 (EventLoop 为主); 连锁: 异步回调阻塞 → EventLoop 卡 → 写超时 → 级联。

### 5. 超时三定时 — 池满、写堵、无响应

场景: 一次超时, 到底卡哪了?
源码路径:
- **connection 超时** (scheduleConnectionTimeout L232-252): `"Unable to acquire connection! Increase connection pool size or timeout... after attempt of attempts"` — 池满
- **write 超时** (scheduleWriteTimeout L254-276): `"Command still hasn't been written into connection! Check CPU usage... no blocking invocations in async/reactive/rx listeners... Try to increase nettyThreads setting. Netty pending tasks: N"` — EventLoop 堵
- **response 超时** (RedisExecutor.java:406 调度, 注释 "starts to countdown when Redis command was successfully sent"): 服务端无响应
- countPendingTasks (RedisExecutor.java:409-418): 遍历所有 EventLoop 累计 pendingTasks — 量化拥堵
关键设计 (q3): 三定时器 = 三阶段, 文案=排障手册 (池满→加池, 写堵→查回调/网卡, 无响应→查服务端)。[模式: 分层超时]
数据流: getConnection 超时 → connectFuture cancel; send 超时 → writeFuture cancel; 响应超时 → promise fail。

### 负面空间 — 单命令刻意不做的事

- **不做无限重试**: attempts 有界 (默认 4), 防雪崩
- **不做响应重试**: 命令可能在服务端已执行, 不保证幂等
- **不做命令切节点重放**: 连接失败重试可能换连接但同一逻辑节点 (Cluster 才跨节点)
- **不做同步阻塞**: 全程异步 CompletableFuture
- **不做命令级超时覆盖**: 三定时共用 responseTimeout (3s)

→ 引出: RESP2/3 命令面怎么适配?能力探测怎么降级?→ [[RD-4-篇2]]
→ 衔接: 锁的续期为什么 noRetry?→ [[rd2-rlock]]