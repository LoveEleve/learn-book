# 缓存策略 — 商品价格为什么改了，页面仍然显示旧值

> Cluster B: 15 KPs | 依赖: 03-distributed-theory-architecture、分布式理论 01 一致性模型 | 读者基线: Redis、MySQL、HTTP/CDN、缓存基础
> 读者处境: 03 篇说明一个架构可以同时包含多种一致性；本篇把问题落到商品详情：CDN、本地缓存、Redis 和 MySQL 多层副本如何更新、失效和修复
> 打开新视角: 缓存不是“加速开关”，而是**把数据复制到多个层，并把一致性、过期、热点和故障修复责任引入系统**

---

### 概念依赖链

```
03 CAP/一致性 + 01 通信/RPC → 本篇: 多级缓存/Redis/一致性/缓存三害
  ├─ §1 CDN/本地/Redis/DB 多层缓存
  ├─ §2 Redis 数据结构/持久化/高可用
  ├─ §3 Cache Aside/Read Through/Write Behind
  ├─ §4 binlog/消息/TTL/对账一致性
  └─ §5 雪崩/穿透/击穿/热点保护
先讲: 缓存层 → Redis → 更新模式 → 一致性修复 → 故障防御
后续依赖: 05-message-driven(消息削峰与事件驱动)
```

### 叙事顺序

1. 问题引入——MySQL 价格已经更新，页面却仍显示旧价格；数据到底经过了几层缓存？
2. 多级缓存——CDN、代理、本地、Redis、数据库
3. Redis——数据结构、持久化与高可用
4. 缓存更新——Cache Aside/Through/Behind
5. binlog/消息/TTL/对账——最终一致修复
6. 雪崩/穿透/击穿/热点——缓存故障防御
7. 收束

### 1. 多级缓存 — 一次读请求可能经过五个副本

场景提示: 用户刷新商品详情，CDN、Nginx、本地 Caffeine、Redis、MySQL 哪一层返回了旧值？ [写作时展开]

关键设计: 缓存层越多，读延迟越低的机会越大，但一致性和失效传播越复杂：

```[pseudocode]
request
  → CDN/edge?
  → reverse proxy cache?
  → application local cache?
  → Redis/distributed cache?
  → MySQL authoritative data

update:
  DB updated
  → invalidate/refresh Redis
  → invalidate local cache/CDN
  → each layer has its own TTL/propagation delay
```

Why: 为什么不能像 CPU cache 一样在所有业务缓存上实现简单全局一致协议？——**缓存层跨进程、跨机器、跨供应商和跨网络，失效消息本身也可能延迟/丢失/重复**；工程上通常选择明确的最终一致窗口、TTL、版本号和对账，而不是为每层维持昂贵的全局序。 [分布式理论: 缓存一致性属于操作级 SLA，不能只写“最终一致”而不写窗口和修复]

比喻锚点: 多级缓存像多地书店都存了一本商品目录；总部改价后，要通知每家书店换页，任何一家延迟都会让顾客看到旧价格。 [写作时展开]

### 2. Redis — 数据结构、持久化和高可用是三种不同能力

场景提示: 为什么 Redis 不只是“一个更快的 Map”，它还能承担队列、集合、限流和分布式缓存？ [写作时展开]

关键设计: Redis 的价值来自数据结构、内存模型和高可用/持久化组合：

```[pseudocode]
数据结构:
  String/Hash/List/Set/ZSet
  HyperLogLog/Bitmap/Geo 等

持久化:
  RDB snapshot
  AOF command log
  混合/配置策略

高可用:
  replication
  Sentinel failover
  Cluster sharding

使用前:
  明确数据丢失窗口、故障切换、淘汰和内存上限
```

Why: 为什么 Redis“快”不能简单归因于单线程？——**内存数据结构、事件循环、网络 I/O、命令复杂度、持久化 fork、慢命令和单线程热点都会影响结果**；高可用也会带来复制延迟、故障切换窗口和数据丢失语义。Redis Sentinel/Cluster/Codis 的拓扑和一致性模型不是同一件事。 [系统性能: Redis 延迟要结合命令复杂度、事件循环、内存、网络和持久化观测]

比喻锚点: Redis 像前台高速货架，取货快但容量有限；RDB/AOF 是库存快照/流水，Sentinel/Cluster 是备用店和分仓。 [写作时展开]

### 3. Cache Aside、Read/Write Through、Write Behind — 谁负责一致性

场景提示: 更新商品价格时，先写数据库还是先删缓存？不同缓存模式把责任交给谁？ [写作时展开]

关键设计: 三种模式的关键差异是“数据库读写由谁触发、缓存如何承诺一致”：

```[pseudocode]
Cache Aside:
  read miss → 应用读 DB → 写 cache
  update → 应用写 DB → invalidate cache

Read/Write Through:
  应用调用 cache
  → cache 层负责回源/写 DB

Write Behind:
  应用先写 cache
  → cache 异步批量写 DB
  → 性能高, 崩溃/丢失/延迟风险更高
```

Why: 为什么延时双删不是严格一致方案？——**它依赖“延时足够覆盖并发读写窗口”的经验假设，慢请求、长事务、GC、网络抖动都可能超出估计**；先写 DB 再删 cache 通常更容易收敛，但并发读仍可能短暂写入旧值。版本号、消息失效和对账比单纯 sleep 更可验证。 [分布式理论: 02 网络模型/09 可靠消息的重试与幂等边界在缓存更新中同样存在]

比喻锚点: Cache Aside 像应用自己管理书店库存；Through 像仓库管理员统一补货；Write Behind 像先在前台记订单、晚些时候再入总账，速度快但断电风险更大。 [写作时展开]

### 4. binlog、消息、TTL 与对账 — 把缓存最终修回来

场景提示: 更新数据库后，应用进程在删除 Redis 前宕机，怎样避免缓存永久脏掉？ [写作时展开]

关键设计: 用数据库变更日志作为权威事件源，再通过消息和周期对账形成多层防线：

```[pseudocode]
DB transaction commit
  → binlog
  → CDC/Canal parser
  → cache invalidation topic
  → consumer DEL/refresh key

失败防线:
  message retry/idempotency
  Redis TTL
  periodic reconciliation(DB vs cache)
  version/timestamp check

最终一致窗口:
  DB commit → event delivery → cache consumer
  + retry/backoff + TTL/repair delay
```

Why: 为什么 binlog 异步失效仍不能承诺 100% 强一致？——**CDC、MQ、消费者和 Redis 都可能失败/重复/延迟，且 CDN/本地缓存还可能在更上层保留旧值**；可靠系统不是假装没有窗口，而是把窗口、重试、TTL、对账和告警明确化。 [系统性能/可观测性: 事件延迟、消费者积压和对账差异要纳入指标]

比喻锚点: binlog 是总部出库流水，消息是通知各门店换价签；通知失败时 TTL 是自动过期，对账是夜间盘点。 [写作时展开]

### 5. 雪崩、穿透、击穿与热点 — 四类缓存故障不是一回事

场景提示: Redis 有 100GB 容量，为什么仍可能被一个热点 key、一次批量过期或恶意不存在 ID 打垮？ [写作时展开]

关键设计: 不同故障要用不同防线：

```[pseudocode]
雪崩:
  大量 key 同时过期/缓存集群故障
  → TTL jitter/预热/多级缓存/限流/降级

穿透:
  查询大量不存在 key
  → 空值短 TTL/Bloom filter/参数校验

击穿:
  一个热点 key 过期
  → singleflight/互斥重建/逻辑过期

热点:
  单 key/分片被极高频访问
  → 本地缓存/热点副本/分片散列/限流
```

Why: 为什么不能用“加 Redis 节点”解决所有缓存问题？——**雪崩是时间集中，穿透是无效请求，击穿是单 key 重建竞争，热点是访问分布偏斜**；扩容只可能改善容量，不自动解决回源风暴、重建并发和恶意参数。防御方案也会引入旧值、内存、锁和一致性代价。 [分布式架构: 缓存防护必须和限流、熔断、降级、消息/数据库容量一起设计]

### 6. 收束

商品价格更新链路：

```[pseudocode]
write DB
  → commit/binlog
  → invalidate Redis/local/CDN
  → read miss 回源 DB
  → 重建 cache
  → TTL/对账修复残留差异
```

**Aha Moment**: "引入缓存就等于复制数据；**每复制一层，就新增一条失效、延迟、故障和修复路径**。缓存的正确设计不是追求绝对强一致，而是让不一致窗口、用户语义、兜底和修复都可测量。"
**回答读者三问**: ①缓存为什么显示旧值=多层副本失效传播不同步；②延时双删为什么不可靠=依赖时间窗口；③四类缓存故障怎么区分=雪崩/穿透/击穿/热点的触发机制不同。

---

### 核心悬念

**"缓存扛住了读流量，但秒杀写请求瞬间超过数据库容量；消息队列如何削峰、解耦和保证最终一致？"**

→ 引出 05-message-driven — 消息驱动、削峰填谷与事件驱动架构。