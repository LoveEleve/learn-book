# K-12 FetchSession 篇 1/2 — 只发变更: 会话模型与增量协议

> 前置: [[K-4-isr-03]] (两阶段拉取) [[K-4-isr-04]] (读路径) | 复用: — | 对照: [[r28-networking]] (增量协议对照) | 引出: [[K-12-fetchsession-02]]
> 🟡 B | 来源: FetchSession.scala:236,271-292 + FetchMetadata.java:31-65 + FetchSessionHandler.java:60-77,559-598
> 定位: K-12 卷开篇 — 回答"fetch 请求怎么只发变更?"

**读者处境**: 面试官问 "消费者有 1000 个分区, 每次 poll 都发全部分区元数据?" 你答 "有会话缓存" — 但再问 "会话怎么建的? epoch 怎么推进? 增量三元组是什么?" 你答不上来。这篇是 FetchSession 协议的完整答案。

### 1. 问题引入 — 1000 分区的 fetch 请求

场景: 消费者订阅 1000 分区, 每次 poll 请求都带全部分区元数据 (topic+partition+fetchOffset) — 请求越来越大, 大部分没变。怎么办?
- KIP-219 方案: 会话缓存 — 首请求建会话, 后续只发变更
- 本篇问题: 会话模型 (Q1) / Epoch (Q2) / 客户端 Handler (Q3)

### 2. 服务端会话 — 缓存分区参数

场景: 服务端记住什么?
- FetchSession (FetchSession.scala:236): id + epoch + partitionMap (缓存各分区 fetch 参数)
- update 增量三元组 (FetchSession.scala:L271-292): added (新增) / updated (更新参数) / removed (遗忘) — 只对变更分区响应
- 为什么不压缩全量? — **不做全量压缩, 而做增量**: 增量 O(变更) vs 压缩 O(分区), 变更少时增量更优 (设计权衡)
- 响应生成: 全量/增量/失效三形态 (FetchSession.scala:L337,356,386)

### 3. Epoch — 会话的版本号

场景: 怎么知道会话新旧?
- 常量 (FetchMetadata.java:31-43): INVALID_SESSION_ID=0 / INITIAL_EPOCH=0 / FINAL_EPOCH=-1
- 生命周期: 创建时 nextEpoch(INITIAL) (FetchSession.scala:685) → 每响应递增 (FetchSessionHandler.java:596) → FINAL 终态 (FetchMetadata.java:L64-65)
- 与 K-4 分区 leader epoch 的区别: 两套独立体系 — 面试易混点

### 4. 客户端 Handler — 双路径切换

场景: 客户端怎么决定下次发什么?
- 状态: nextMetadata (FetchSessionHandler.java:77)
- full 响应: 新会话 → newIncremental (FetchSessionHandler.java:L569-573); INVALID → INITIAL 重建 (FetchSessionHandler.java:L562-565)
- incremental 响应: 继续 → nextIncremental (FetchSessionHandler.java:L590-596); 服务端关闭 → INITIAL (FetchSessionHandler.java:L578-584)
- KIP-219 节流: 空 full 响应 = 节流信号 (FetchSessionHandler.java:L542-553)

### 核心悬念
"为什么 fetch 会话能省这么多?" — 1000 分区每次全量: 每次请求 1000 条元数据; 会话化后: 首请求建会话 (1000 条), 之后每次只发变更分区 (通常几条) — 请求体从 O(分区数) 降到 O(变更数), 这是 Kafka 高分区数消费者可扩展性的关键。

### 概念依赖链
Q1 会话模型 → Q2 Epoch → Q3 Handler → (02 篇: 失效/淘汰/衔接)

### 源码锚点清单
- FetchSession.scala:236 (class FetchSession) / 237-242 (字段) / 268-270 (getFetchOffset) / 271-292 (update 增量三元组) / 337,356,386 (响应三形态) / 685 (创建时 epoch)
- FetchMetadata.java:31 (INVALID_SESSION_ID=0) / 37 (INITIAL_EPOCH=0) / 43 (FINAL_EPOCH=-1) / 64-65 (nextEpoch 终态)
- FetchSessionHandler.java:60 (类) / 76-77 (sessionId/nextMetadata) / 542-553 (KIP-219 节流) / 559-598 (handleResponse 双路径) / 562-565 (INVALID→INITIAL) / 569-573 (新会话) / 578-584 (关闭→INITIAL) / 590-596 (nextIncremental)
