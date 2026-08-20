# RD-5 篇2 — map-cache: RMapCache 五结构与自适应清理

> 前置: [[RD-5-篇1]] (RMap 基础) | 复用: [[r22-expire]] (服务端过期) | 对照: [[m7-cache]] (MyBatis 缓存装饰链) | 引出: [[rd6-localcachedmap]] (本地缓存)
> 🟡 B | 3 KP | [模式: 五结构索引 + 惰性检查 + 自适应清理]
> Pass 2 闭环: q4(五结构) q5(自适应) q6(清理 Lua)

**读者处境**: RMapCache 让你给每条 entry 设 TTL — 但 Redis 的 hash 没有 TTL。Redisson 怎么实现"每条目过期"?读一次要检查多少次?后台多久清一次, 怎么决定频率?这篇拆 RMapCache 的五个结构 (主 hash + TTL/idle/LRU zset + options), 惰性检查协议, 以及自适应的后台清理。

### 概念依赖链
q4(五结构) ← q5(自适应) ← q6(清理 Lua) — 先讲数据怎么组织 (5 结构), 再讲后台怎么调频, 最后讲清理的具体 Lua。

### 核心悬念
"Redis hash 没有条目 TTL, RMapCache 怎么做到的？后台清理多久跑一次、怎么自适应？"

### 叙事顺序
1. 问题引入: 给 hash 的每个字段单独设过期
2. 五结构 (q4) — 主 hash + TTL/idle/LRU zset + options hash
3. 惰性检查 (q4) — 读时 zscore 比较 + idle 刷新 + LRU 更新
4. 自适应清理 (q5) — sizeHistory 三态调 delay
5. 清理 Lua (q6) — zrangebyscore → hdel
6. 收束: "索引化过期" — 缓存语义的 Redis 实现

### 1. 五结构 — 主 hash 之外的四个影子

场景: 条目 TTL 存哪?
源码路径:
- `getWithLease` Lua (RedissonMapCache.java:129-179):
  - 主 hash hget (RedissonMapCache.java:129); `struct.unpack('dLc0', value)` 解 TTL 头 (RedissonMapCache.java:141)
  - **TTL zset** (KEYS[2]): zscore 存过期时间 (RedissonMapCache.java:143-146)
  - **idle zset** (KEYS[3]): zscore 存 idle 截止 (RedissonMapCache.java:148-155)
  - **LRU zset** (KEYS[4]): maxSize 时 zadd 访问时间 / zincrby 频率 (RedissonMapCache.java:167-175)
  - **options hash** (KEYS[5]): maxSize/mode (LRU/LFU) 配置 (RedissonMapCache.java:167-170)
- 五结构 (RedissonMapCache.java:177-178): name + timeoutSet + idleSet + lastAccessSet + optionsName
关键设计 (q4): 条目级 TTL = 分数存入旁路 zset, 主 hash 保持普通字段。[模式: 旁路索引]
数据流: put(k, v, ttl) → 主 hash + TTL zset 同写。

### 2. 惰性检查 — 读时判定过期

场景: 读一次检查几遍?
源码路径:
- 惰性 Lua (RedissonMapCache.java:142-166):
  - TTL: zscore(KEYS[2]) → expireDate (RedissonMapCache.java:143-146)
  - idle: zscore(KEYS[3]) → expireIdle; **t>0 → zadd(KEYS[3], t+now) 刷新 idle 窗口** (RedissonMapCache.java:147-155)
  - **expireDate = min(TTL, idle)** (RedissonMapCache.java:153) — 取更早
  - 过期判定 (RedissonMapCache.java:156): `expireDate <= now → 返回不存在`
  - LRU/LFU 更新 (RedissonMapCache.java:167-175): maxSize 时访问更新
关键设计 (q4): 惰性 = 读路径内联过期判定 + 动态刷新 idle 窗口; TTL/idle 取早。[模式: 读时惰性检查]
数据流: get → hget → TTL zscore → idle zscore(min) → 过期? 不存在 : 刷新返回。

### 3. 自适应清理 — 清理频率智能浮动

场景: 后台多久清一次?
源码路径:
- EvictionTask.run (EvictionTask.java:71-113):
  - L81-83 错误也 schedule (续排)
  - **三态调 delay** (EvictionTask.java:88-105):
    - 连续递减 (清理越来越少) → `delay*1.5` 放慢 (EvictionTask.java:93-95)
    - 持续大 (≥keysLimit) → `delay/4` 加快 (EvictionTask.java:100-102)
    - 清零 → `delay*1.5` 放慢 (EvictionTask.java:103-105)
  - 界: 5s ~ 2h (EvictionScheduler 注释)
- 每集合一个 task (tasks map L36) 独立自适应
关键设计 (q5): 清理频率 = 反馈控制: 用清理量历史自动调频 (闲置省资源/堆积勤清理)。[模式: 自适应反馈]
数据流: 清理量 size → sizeHistory → 三态 → delay → schedule。

### 4. 清理 Lua — 过期索引批量删

场景: 物理删除怎么做?
源码路径:
- MapCacheEvictionTask (MapCacheEvictionTask.java:77-111):
  - L77 `zrangebyscore(KEYS[2], 0, now, limit 0, batch)` — TTL zset 取过期 (score≤now)
  - L90-93 **五路清理**: `zrem(KEYS[5]/KEYS[3]/KEYS[2], unpack(keys, i, min(i+4999, n)))` 清三个辅助 zset + `hdel(KEYS[1], ...)` 清主 hash — **每批 ≤4999, 索引和主存一起删**
  - L95-111 idle zset (KEYS[3]) 同样清理
- 返回清理数 → EvictionTask 自适应 (Q5)
关键设计 (q6): 物理清理 = 过期索引驱动: zrangebyscore 取过期 → **zrem 三辅助 zset + hdel 主 hash 五路删除** (4999 批次)。[模式: 索引驱动清理]
数据流: 定时到 → zrangebyscore(0,now) → 五路删 (zrem×3 + hdel) → 返回清理数 → 调 delay。

### 负面空间 — 缓存与清理刻意不做的事

- **不做条目级精确推送**: 过期靠惰性检查+后台轮询, 无即时事件 (对照 RLocalCachedMap 订阅)
- **不做 TTL 反向更新**: TTL 固定, 无"访问自动续 TTL" (idle 才是)
- **不做多级缓存**: RMapCache 直接存 Redis, 无本地层 (那是 RD-6)
- **清理不保证实时**: 后台任务延迟 5s~2h 不等
- **不处理 maxSize 精确淘汰**: LRU/LFU zset 按访问更新, 淘汰在清理周期
- **zset 随过期条目膨胀**: TTL/idle zset 随条目增长, 靠清理周期回收 (completeness Q35)
- **惰性检查有固定开销**: 每读多 2 次 zscore (TTL+idle) + 可能 1 次 zadd (idle 刷新) + 1 次 zadd (LRU) — maxSize 模式 (completeness Q32)

→ 引出: 本地缓存怎么读本地 + 订阅失效?→ [[rd6-localcachedmap]]