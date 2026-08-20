# RD-5 RMap 分布式映射 — 知识规划 (knowledge-planning)

> 项目: Redisson 4.6.2-SNAPSHOT | 🟡 B / 2-3 篇 (无 harness) | RedissonMap(1967)+RedissonMapCache+EvictionScheduler(8)
> 基线: REDISSON-PLAN RD-5 — 前置: **RD-4 (命令) + RD-3 (codec) + RD-1 (连接)** — 展开 写三路→读通→TTL 五结构→自适应清理
> 双链: 前置 [[rd4-command]] [[rd3-codec]] | 复用 [[r21-db]] (键空间) | 对照 [[r22-expire]] (过期) [[m7-cache]] (MyBatis 缓存) | 引出 [[rd6-localcachedmap]] [[rd7-spring]]

---

## §0.8

- 🟡 B，2篇 — 写三路(**mapWriterFuture: WRITE_BEHIND 缓冲 vs WRITE_THROUGH 同步/异步; condition 门控 Redis 成功才写外部 RedissonMap:659-698**) → WriteBehind(**50/1000ms 批量合并+Retryable 重试 MapOptions:62-63**) → MapLoader 读通(**Redis miss→load(K) 外部源回填**) → RMapCache 五结构(**主 hash + TTL/idle/LRU zset + options hash; 读时惰性检查 zscore 比较+idle 刷新+LRU 更新 RedissonMapCache:129-176**) → EvictionTask 自适应(**sizeHistory 三态调 delay: 递减×1.5/持续大/4/清零×1.5; 5s~2h EvictionTask:71-113**) → MapCacheEvictionTask Lua(**zrangebyscore(0,now)→hdel unpack 4999 批次**) → 双引擎(**RedissonMapCache vs RedissonMapCacheNative 4.x Native 面**)
- 设计模式: [模式: 双写编排+读通缓存+五结构索引+自适应清理]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| RedissonMap.java:659-698 | 写三路 | BEHIND/THROUGH 双写+condition | High |
| MapOptions.java:62-63,101 | WriteBehind | 50/1000ms 批量+重试 | High |
| api/map/MapLoader.java:28-36 | 读通 | load(K) miss 触发 | High |
| RedissonMapCache.java:129-176 | 五结构 | TTL/idle/LRU zset 协同 | High |
| EvictionTask.java:71-113 | 自适应 | sizeHistory 三态调频 | High |
| MapCacheEvictionTask.java:77-111 | 清理 Lua | zrangebyscore→hdel | High |

---

## 02-04 聚合+分类+聚类 (2篇)

**2篇理由**: 6 闭环 → 篇1 读写三路 (q1/q2/q3), 篇2 缓存与清理 (q4/q5/q6)。无 harness (🟡 B)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 写三路 (through/behind) | 🔴 | **为什么🔴**: 双写核心 |
| P1-2 | RMapCache 五结构 | 🔴 | **为什么🔴**: 缓存机制 |
| P2-1 | MapLoader 读通 | 🟡 | 读面 |
| P2-2 | WriteBehind 缓冲 | 🟡 | 性能面 |
| P2-3 | EvictionTask 自适应 | 🟡 | 运维面 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **读写双写** (q1/q2/q3) | 🔴 | 核心 |
| B | **缓存与清理** (q4/q5/q6) | 🟡 | 扩展 |

---

## 05 闭环结论摘要 (Pass 2 内化)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 写三路 | mapWriterFuture: BEHIND 缓冲 / THROUGH 同步或异步; condition 门控 | RedissonMap:659-698 |
| q2 | WriteBehind | 50 条或 1000ms 批量落库合并重复 key; Retryable 重试 | MapOptions:62-63 |
| q3 | MapLoader | Redis miss → load(K) 外部源回填 | MapLoader.java:28-36 |
| q4 | 五结构 | 主 hash+TTL/idle/LRU zset+options; 读时惰性检查+刷新 | RedissonMapCache:129-176 |
| q5 | 自适应清理 | sizeHistory 三态调 delay (5s~2h) | EvictionTask:71-113 |
| q6 | 清理 Lua | zrangebyscore(0,now)→hdel unpack 4999 批次 | MapCacheEvictionTask:77-111 |

→ 引出 RD-6: 本地缓存怎么基于 RMap 载体 + 订阅失效 — [[rd6-localcachedmap]]