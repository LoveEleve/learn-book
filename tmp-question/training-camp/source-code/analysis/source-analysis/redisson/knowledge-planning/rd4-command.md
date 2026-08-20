# RD-4 命令执行流水线 — 知识规划 (knowledge-planning)

> 项目: Redisson 4.6.2-SNAPSHOT | 🔴 A / 3 篇 (+harness) | command/ 12 文件 4027 行
> 基线: REDISSON-PLAN RD-4 — 前置: **RD-3 Codec (显式传参) + RD-1 (连接池借还) + r28 (RESP)** — 展开 汇聚点→重试→超时→协议适配→Lua→分批
> 双链: 前置 [[rd3-codec]] [[rd1-connection]] | 复用 [[r28-networking]] (RESP) | 对照 [[s75-boot-redis]] (Lettuce 连接) [[r2-events]] (事件循环) | 引出 [[rd2-rlock]] [[rd5-rmap]] [[rd6-localcachedmap]]

---

## §0.8

- 🔴 A，3篇 — 汇聚点(**async(readOnly, NodeSource, codec, command, params, ignoreRedirect, noRetry) CommandAsyncService:690-731: resp3 适配 + SORT_RO 降级 + Client-Side Caching 失效钩子 + new RedisExecutor**) → 执行管线(**RedisExecutor.execute:132-230: 借连接→sendCommand→响应→releaseConnection, CompletableFuture 链**) → 重试协议(**scheduleRetryTimeout L278-375: 连接/写失败可重试递归 execute, 响应超时不重试, blocking 命令特殊路径**) → 超时三定时器(**connectionTimeout 池满 / writeTimeout EventLoop 堵 / responseTimeout 无响应; 文案带排障建议 L241-275**) → RESP2/3 适配(**ServiceManager RESP3MAPPING L689-705: Stream/ZSet 读族 _V2 版**) → 能力降级(**SORT_RO/EVALSHA_RO: static AtomicBoolean 乐观探测→ERR unknown command 置 false 永久降级**) → Lua 执行(**evalAsync L578-659: EVALSHA 缓存 + NOSCRIPT 自愈 loadScript 到对节点 + EVALSHA_RO 降级; calcSHA sha1**) → 缓存失效钩子(**!readOnly + hasCachingInstances → evictClientSideCaching L717-727**) → 批处理(**CommandBatchService extends CommandAsyncService: 按 NodeSource 分组 + skipResult 快路径 + 失败 tryFailure 全灭; batch=🔥 pipeline 非 transaction**) → NodeSource(**slot/addr/Redirect(MOVED/ASK)/entry 重定向载体**)
- 设计模式: [模式: 汇聚适配+有界重试+能力探测+脚本缓存自愈+分组批处理]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| CommandAsyncService.java:690-731 | 汇聚点 | resp3+SORT_RO+缓存钩子+RedisExecutor | High |
| RedisExecutor.java:122-230 | 执行管线 | 借→发→响→还 CompletableFuture 链 | High |
| RedisExecutor.java:278-375 | 重试协议 | 递归 execute; 分类型重试边界 | High |
| RedisExecutor.java:232-276 | 超时三定时 | 池满/写堵/无响应 + 排障文案 | High |
| ServiceManager.java:686-705 | RESP 适配 | RESP3MAPPING _V2 | High |
| CommandAsyncService.java:688,694-714,542 | 能力降级 | SORT_RO/EVALSHA_RO 悲观降级 | High |
| CommandAsyncService.java:578-659,498-504 | Lua 执行 | EVALSHA+NOSCRIPT 自愈+loadScript | High |
| CommandAsyncService.java:717-727 | 缓存失效钩子 | evictClientSideCaching | High |
| CommandBatchService.java:53,273-327 | 批处理 | 分组+skipResult+全灭 | High |

---

## 02-04 聚合+分类+聚类 (3篇+harness)

**3篇理由**: 8 闭环 → 篇1 单命令执行 (q1/q2/q3), 篇2 协议适配+能力 (q4/q5), 篇3 Lua+批量+钩子 (q6/q7/q8)。harness 验证异步执行链+重试边界。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | async 汇聚 + RedisExecutor | 🔴 | **为什么🔴**: 命令执行中枢 |
| P1-2 | 重试协议边界 | 🔴 | **为什么🔴**: 可靠性核心 |
| P1-3 | Lua EVALSHA 自愈 | 🔴 | **为什么🔴**: 脚本面 |
| P1-4 | 批处理分组 | 🔴 | **为什么🔴**: 性能优化 |
| P2-1 | 超时三定时器 | 🟡 | 运营面 |
| P2-2 | resp3 适配 | 🟡 | 协议面 |
| P2-3 | 能力探测降级 | 🟡 | 兼容面 |
| P2-4 | 缓存失效钩子 | 🟡 | 一致性面 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **执行与重试** (q1/q2/q3) | 🔴 | 中枢 |
| B | **协议与能力** (q4/q5) | 🟡 | 兼容 |
| C | **Lua 与批** (q6/q7/q8) | 🔴 | 高效 |

---

## 05 闭环结论摘要 (Pass 2 内化)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 汇聚点 | async() 三适配 (resp3/SORT_RO/缓存钩子) → new RedisExecutor 借→发→响→还 | CommandAsyncService:690-731 |
| q2 | 重试协议 | 定时器驱动递归; 连接/写失败可重试, 响应超时不重试; noRetry 防续期放大 | RedisExecutor:278-375 |
| q3 | 超时三定时 | 池满/写堵/无响应 三阶段, 文案带排障建议 | RedisExecutor:232-276 |
| q4 | RESP 适配 | RESP3MAPPING Stream/ZSet 读族 _V2 版 | ServiceManager:686-705 |
| q5 | 能力降级 | SORT_RO/EVALSHA_RO atomic 乐观探测→失败永久降级 | CommandAsyncService:688,694-714 |
| q6 | 缓存失效钩子 | 写命令成功 → evictClientSideCaching (就近失效) | CommandAsyncService:717-727 |
| q7 | Lua 自愈 | EVALSHA 缓存 + NOSCRIPT 自动 loadScript 到对节点 | CommandAsyncService:578-659 |
| q8 | 批处理 | 按 NodeSource 分组 + skipResult 快路径; batch=pipeline 非事务 | CommandBatchService:53,273-327 |

→ 引出 RD-2: 锁的 tryLock Lua 走 EVALSHA 自愈链路 — [[rd2-rlock]] ; RD-6 缓存失效钩子 — [[rd6-localcachedmap]]