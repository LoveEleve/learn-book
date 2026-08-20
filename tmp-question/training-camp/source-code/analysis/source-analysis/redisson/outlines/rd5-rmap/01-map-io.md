# RD-5 篇1 — map-io: 读写三路与外部数据源

> 前置: [[rd4-command]] (命令执行) + [[rd3-codec]] (mapValue codec) | 复用: [[r21-db]] (键空间) | 对照: [[h13-datasource]] (数据源) | 引出: [[RD-5-篇2]] (缓存清理) + [[rd6-localcachedmap]]
> 🟡 B | 3 KP | [模式: 双写编排 + 写后缓冲 + 读通回填]
> Pass 2 闭环: q1(写三路) q2(WriteBehind) q3(MapLoader)

**读者处境**: RMap 像分布式 Map — 但你配了 MapWriter/MapLoader 后, put 一次数据发生了两次写 (Redis + 数据库)。写后和写通怎么选?读没命中时数据从哪来?这篇拆 RMap 的数据同步面: 写三路 (写后/写通同步/写通异步), 写后批量缓冲, 读通 load 回填。

### 概念依赖链
q1(写三路) ← q2(WriteBehind) ← q3(MapLoader) — 先讲写操作的三路编排, 再讲缓冲模式细节, 最后讲读侧对称。

### 核心悬念
"一次 put 背后 Redis 和数据库怎么保持同步？写后缓冲为什么能合并 50 次写？"

### 叙事顺序
1. 问题引入: RMap 不只是分布式 Map — 还能双写数据库
2. 写三路 (q1) — mapWriterFuture: BEHIND/THROUGH 同步/异步
3. WriteBehind (q2) — 50/1000ms 批量合并 + 重试
4. MapLoader (q3) — 读通回填
5. 收束: "写对称" — 写通/写后 vs 读通

### 1. 写三路 — Redis 主存储 + 影子同步

场景: put 之后, 外部数据源怎么同步?
源码路径:
- `mapWriterFuture` (RedissonMap.java:659-698):
  - **WRITE_BEHIND** (RedissonMap.java:664-670): Redis 成功后 `writeBehindTask.addTask(task)` — 缓冲异步 (Q2)
  - **WRITE_THROUGH 同步** (RedissonMap.java:673-684): `supplyAsync { Add→writer.write(map); delete→writer.delete(keys) }` 在 ServiceManager executor 同步
  - **WRITE_THROUGH 异步** (RedissonMap.java:686-692): `writerAsync.write/delete`
  - condition 门控 (RedissonMap.java:666/674): Redis 成功才触发 writer
- WriteMode: WRITE_THROUGH (默认) / WRITE_BEHIND
- MapWriterTask.Add/delete 分派 (RedissonMap.java:677-681)
关键设计 (q1): 双写 = Redis 命令 + MapWriter 影子; condition 确保 Redis 成功才同步外部。[模式: 双写门控]
数据流: put → Redis 命令成功 → condition → BEHIND 缓冲 / THROUGH 写外部。

### 2. WriteBehind — 写后批量合并

场景: 写后模式怎么省写次数?
源码路径:
- MapOptions (MapOptions.java:62-63): `writeBehindBatchSize=50` / `writeBehindDelay=1000`
- RedissonMap 构造 (RedissonMap.java:77-88): writer 存在 → writeBehindService.start → MapWriteBehindTask
- mapWriterFuture BEHIND (RedissonMap.java:667): `addTask(task)` — 每次写进缓冲
- 合并: 同 key 多次写 → 最后一次 (批处理核心收益)
- 触发: 缓冲满 50 或 1000ms → 批量 writer.write
- RetryableMapWriter (MapOptions.java:101): 失败重试包装
关键设计 (q2): 写后 = 缓冲合并 + 定时批量; 延迟窗口内外部滞后 (最终一致)。[模式: 写后缓冲]
数据流: 50 次 put → 缓冲 → 满 50/1000ms → 一次批量 write 外部。

### 3. MapLoader — 读通回填

场景: 读未命中时数据从哪来?
源码路径:
- MapLoader (api/map/MapLoader.java:28): `V load(K key)` (L36) — Redis miss → 外部源加载
- loadAll: 批量预热 (启动缓存)
- 对称: MapWriter (写通/后, Redis→外部) vs MapLoader (读通, 外部→Redis 回填)
- 边界: 并发 miss 无去重 (调用方保障幂等)
关键设计 (q3): 读通 = miss 时从数据源 load + 回填缓存。[模式: 读通回填]
数据流: get miss → load(K) → 回填 Redis → 返回。

### 4. 命令面 — RMap 就是 Redis hash

场景: put/get 背后是什么命令?
源码路径 (REDISSON-PLAN "Map 命令面" 回填):
- get → `HGET` (RedissonMap.java:1229); put → `HSET` (L1446); putIfAbsent → `HSETNX` (L1038)
- putAll → `HMSET` (RedissonMap.java:705); containsKey → `HEXISTS` (RedissonMap.java:472); remove → `HDEL` (RedissonMap.java:1521)
- entrySet → `HGETALL` (RedissonMap.java:949) / HGETALL_ENTRY (RedissonMap.java:931); 遍历 → `HSCAN` (RedissonMap.java:939,1572-1576)
- 计数/浮点 → `HINCRBYFLOAT` (RedissonMap.java:1604)
- 编码: `encodeMapKey/encodeMapValue` — mapKey/mapValue 分开编 (RD-3 CompositeCodec)
关键设计 (补充): RMap = Redis hash 的对象化封装 — 每个方法映射到 HSET/HGET/HMSET 族; 命令面 + 双写 (本篇) + 缓存 (篇2) 三层。[模式: hash 命令面]
数据流: map.get(k) → HGET → decodeMapValue → V。

### 5. 负面空间 — 读写双写刻意不做的事

- **不做并发 miss 去重**: 多个线程 miss 同时 load (调用方幂等)
- **不做写通事务**: Redis + 外部 DB 无原子事务 (最终一致)
- **不做 writeBehind 顺序保证**: 缓冲内并发写顺序不定
- **不自动清理外部数据**: MapWriter.delete 需显式调
- **写失败默认不重试**: RetryableMapWriter 重试需显式配 `writerRetryAttempts` (默认 0, MapOptions:64), 间隔 100ms (MapOptions:66); 双写无事务, 失败靠重试 + 人工补偿 (completeness Q15/Q16)

→ 引出: TTL/idle 怎么用五结构存?清理怎么自适应?→ [[RD-5-篇2]]