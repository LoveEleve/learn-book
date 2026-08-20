# Pass 1 探索笔记: RD-5 RMap 分布式映射

> 方案 B (🟡) | 源码: `/data/workspace/source-code/code/spring/redisson` (4.6.2-SNAPSHOT)
> 域规模: RedissonMap(1967)+RedissonMapCache+MapOptions+MapLoader/Writer+EvictionScheduler(8 文件) → 2-3 篇

## Pass 0 上下文吸收

- 核心: RedissonMap(1967 行) extends RedissonExpirable + WriteBehindService + MapOptions
- MapLoader (读通) / MapWriter (写通/写后) 接口: load(K)/write(Map)/delete(Collection)
- RMapCache extends RedissonMap (L66): 每条目 TTL/maxIdle; **双 zset 存分数** (KEYS[2] TTL / KEYS[3] idle) + 惰性读检查 + 后台清理
- EvictionScheduler (eviction/8 文件): 每集合一个 EvictionTask; 六子类 (MapCache/Multimap/JCache/TimeSeries/SetCache/ScoredSet)
- MapOptions: writeBehindBatchSize=50 / writeBehindDelay=1000 (写后缓冲)

## 继承树/调用图

```
RedissonMap (1967) extends RedissonExpirable implements RMap
  ├── MapOptions (writer/loader/writerAsync/loaderAsync + writeBehind*)
  ├── WriteBehindService → MapWriteBehindTask (写后异步)
  ├── mapWriterFuture (L659) — 写操作包装: 直接写 vs writeBehind 缓冲 vs 写通
  ├── RedissonMapCache extends RedissonMap (L66) — TTL/maxIdle
  │    ├── 双 zset 索引 (TTL + idle) 存分数
  │    ├── 惰性读检查 (L142-153 zscore 比较)
  │    └── MapCacheEvictionTask 后台清理
  └── RedissonMapCacheNative — Native 引擎 (4.x 双实现)

MapLoader<K,V> (map/MapLoader)          load(K) 读通
MapWriter<K,V> (map/MapWriter)          write(Map)/delete(Collection) 写通/写后
RetryableMapWriter                      MapWriter 重试包装
WriteBehindService                      写后缓冲任务调度

EvictionScheduler (eviction/)
  └── EvictionTask (TimerTask)          自适应延迟: sizeHistory 连续递减→delay*1.5; 连续大→delay/4; 连续0→delay*1.5; 界 5s~2h
       ├── MapCacheEvictionTask          zrangebyscore(TTL zset) → hdel 主 hash
       ├── MultimapEvictionTask / JCacheEvictionTask / TimeSeriesEvictionTask / SetCacheEvictionTask / ScoredSetEvictionTask
```

## 基本元素分解

1. **RedissonMap 骨架** — extends RedissonExpirable + RMap 接口; 命令操作走 evalWriteAsync (RD-4)
2. **MapOptions 配置** — writer/loader/writerAsync/loaderAsync + writeBehindBatchSize=50/Delay=1000 (MapOptions:36-62)
3. **MapWriter 写通/写后** — write(Map)/delete(Collection); RetryableMapWriter 重试; WriteBehindService 异步缓冲
4. **MapLoader 读通** — load(K) 缓存未命中 → 从数据源加载
5. **mapWriterFuture 包装** (L659) — 写操作统一走"直接写/写后/写通"三路
6. **RMapCache TTL 双 zset** (L142-153) — TTL zset + idle zset 存分数, 读时 zscore 比较
7. **EvictionTask 自适应** (L71-113) — sizeHistory 三态调 delay (5s~2h)
8. **MapCacheEvictionTask Lua** (L77-111) — zrangebyscore(0,now) → hdel(unpack ≤4999)

## 标记问题 (6 个)

1. **Q1 mapWriterFuture 三路**: 写操作怎么在"直接写/写后/写通"间选择?writeBehindTask 的缓冲语义?
2. **Q2 MapWriter 写通 vs 写后**: writeBehindBatchSize=50/writeBehindDelay=1000 的缓冲协议?失败重试 (Retryable)?
3. **Q3 MapLoader 读通**: load(K) 在什么时机触发?miss 才 load?并发 miss 会重复 load 吗 (loadAll)?
4. **Q4 RMapCache 双 zset**: TTL 和 idle 两个 zset 怎么协同?读时惰性检查逻辑 (expireDate/expireIdle min)?
5. **Q5 EvictionTask 自适应**: sizeHistory 三态调 delay 的完整逻辑?5s~2h 怎么来的?
6. **Q6 MapCacheEvictionTask Lua**: zrangebyscore 批量取过期 + hdel 清理的细节?unpack 上限 4999?

## 已读测试 (2 个)

- RedissonMapCacheTest (顶层): TTL/maxIdle 验证
- RedissonMapTest: 基础操作 (未深入)

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 8 项有源码位置
- [x] 6 个标记问题有源码位置
- [x] 已读测试 (RedissonMapCacheTest)
- [x] 方案 B: 无 harness/时空溯源 (符合 🟡 B)