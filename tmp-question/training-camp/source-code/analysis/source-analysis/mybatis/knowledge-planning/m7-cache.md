# M-7 缓存体系 — CacheBuilder 装饰链 + CacheKey + 事务性二级缓存

> 项目: MyBatis | 🟡 Working / 1 篇 | Cache(101)+CacheKey(138)+CacheBuilder(218)+PerpetualCache(91)+装饰器 10 种+TransactionalCacheManager(59)
> 基线: M-PLAN M-7 — 前置: **M-1 (cache 别名/CacheBuilder 消费) + M-2 (CachingExecutor 消费/一级缓存对照)** — 展开 Cache 接口→CacheBuilder 组装→CacheKey→事务性缓存

---

## §0.8

- 🟡 Working，1篇 — 接口(**Cache.java: 7 方法[getId/putObject/getObject/removeObject/clear/getSize/getReadWriteLock default null — 3.2.6 起核心不再调用锁, 同步由缓存自持]**) → 基础实现(**PerpetualCache: 裸 HashMap 非同步[同步靠装饰器], equals/hashCode 按 id**) → 组装(**CacheBuilder.build L92-127: 默认 PerpetualCache+LruCache[setDefaultImplementations L98-106]; **装饰器循环仅限 PerpetualCache**(issue#352 自定义实现只包 LoggingCache); setStandardDecorators L110-127 顺序: size 注入→ScheduledCache(clearInterval)→SerializedCache(readWrite 默认 true)→LoggingCache→SynchronizedCache→BlockingCache(blocking); 构造契约: 基础=String id/装饰器=Cache 构造 L199-218; setCacheProperties 反射类型转换注入 L137-177**) → 键(**CacheKey L40-138: 37 乘子+17 初始 hashcode+checksum+count+updateList; update L71-79[null→1, baseHashCode*=count, hashcode=37*hashcode+base]; equals 快速失败 L91-95[hashcode/checksum/count→updateList 逐项 ArrayUtil]; NULL_CACHE_KEY 单例不可更新 L33-44**) → 淘汰策略(**LruCache: LinkedHashMap(accessOrder=true) keyMap 触摸序+removeEldestEntry 记 eldestKey+cycleKeyList 同步淘汰 delegate, 默认 1024 L31-93; FifoCache: Deque keyList 先进先出; ScheduledCache: 默认 1h clearInterval+clearWhenStale**) → 事务性二级缓存(**TransactionalCacheManager: per-Cache TransactionalCache 包装[computeIfAbsent]+commit/rollback 全量 L44-57; TransactionalCache 三暂存[clearOnCommit/entriesToAddOnCommit/entriesMissedInCache L43-51]: getObject 未命中记 miss[供 BlockingCache unlock L65-73], putObject 只入 pending L79-81, commit: clearOnCommit→delegate.clear→flushPendingEntries→reset L85-95, rollback: unlockMissedEntries→reset; BlockingCache: ConcurrentHashMap<key,CountDownLatch> acquireLock/releaseLock L41-72**) → 消费(**CachingExecutor.tcm 调用[导航 M-2 §6]; XMLMapperBuilder.cacheElement: readWrite=!readOnly 默认 true L169 → SerializedCache 默认应用**)
- 设计模式: [模式: 装饰器+Builder+值语义键+事务性工作集]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Cache.java:28-90 | 接口 | **7 方法契约**: getId/putObject/getObject/removeObject/clear/getSize/getReadWriteLock(default null — 3.2.6 起核心不再调用, 同步由缓存自持) | High |
| PerpetualCache.java:24-35 | 基础 | 裸 HashMap 非同步; equals/hashCode 按 id(与 StrictMap/别名体系一致) | High |
| CacheBuilder.java:92-106 | 默认 | build(): 默认 PerpetualCache; 无装饰器时默认加 LruCache | High |
| CacheBuilder.java:110-127 | 标准链 | setStandardDecorators 顺序: Scheduled→Serialized(readWrite)→Logging→Synchronized→Blocking — **Logging 在 Synchronized 内**(命中统计不受锁污染?) | High |
| CacheBuilder.java:95-97,199-218 | 契约 | 装饰器循环仅限 PerpetualCache(issue#352 自定义实现只包 Logging); 构造契约: 基础 String id/装饰器 Cache | High |
| CacheBuilder.java:137-177 | 属性 | setCacheProperties: MetaObject hasSetter+8 种类型反射转换+InitializingObject.initialize | High |
| CacheKey.java:71-79 | update | hashcode=37*hashcode+base; base=ArrayUtil.hashCode(null→1); base*=count; checksum+=base | High |
| CacheKey.java:91-95 | equals | hashcode/checksum/count 快速失败 → updateList 逐项 ArrayUtil.equals(顺序敏感) | High |
| CacheKey.java:33-44 | NULL 键 | NULL_CACHE_KEY 单例, update 抛 "Not allowed" — 防误用 | High |
| LruCache.java:31-93 | LRU | LinkedHashMap(accessOrder=true) 触摸序+removeEldestEntry 记 eldestKey+cycleKeyList 同步淘汰 delegate(默认 1024) | High |
| TransactionalCacheManager.java:44-57 | 管理器 | per-Cache TransactionalCache 包装; commit/rollback 全量遍历 | High |
| TransactionalCache.java:43-51,85-95 | 事务缓存 | 三暂存: clearOnCommit/entriesToAddOnCommit/entriesMissedInCache; commit=clear→flush→reset; rollback=unlock→reset | High |
| BlockingCache.java:41-72 | 阻塞 | ConcurrentHashMap<key,CountDownLatch>; getObject acquireLock(未命中等待)/putObject releaseLock | High |
| XMLMapperBuilder.java:169-172 | 默认值 | cacheElement: readWrite=!readOnly(默认 true→SerializedCache 默认应用); blocking 默认 false | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 缓存体系是单装饰链 — 1篇 (~45行) 按"接口/基础→CacheBuilder 组装→CacheKey→淘汰策略→事务性二级缓存→消费"展开; CachingExecutor 消费已在 M-2 展开(导航), 一级缓存对照 M-2(导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | CacheBuilder 装饰链组装 (默认/标准链/契约) | 🔴 | **为什么🔴**: 二级缓存核心 |
| P1-2 | CacheKey 算法与值语义 | 🔴 | **为什么🔴**: 缓存正确性 |
| P1-3 | 事务性二级缓存 (TransactionalCache 三暂存) | 🔴 | **为什么🔴**: 事务隔离 |
| P2-1 | 淘汰策略 (Lru/Fifo/Scheduled) | 🟡 | **为什么🟡**: 容量管理 |
| P2-2 | 装饰器族 (Serialized/Soft/Weak/Blocking/Logging/Synchronized) | 🟡 | **为什么🟡**: 能力组合 |
| P3-1 | 与 CachingExecutor/一级缓存对照 (导航 M-2) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **组装与键** | 🔴 | 二级缓存骨架 |
| B | **事务性** | 🔴 | 正确性 |
| C | **策略与装饰器** | 🟡 | 能力面 |
| D | **消费与边界** | 🟢 | 衔接 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 装饰链组装 | CacheBuilder.build: 默认 PerpetualCache+LruCache → 用户装饰器循环(仅 PerpetualCache, issue#352) → setStandardDecorators 固定顺序: Scheduled→Serialized→Logging→Synchronized→Blocking — **readWrite 默认 true → SerializedCache 默认应用** | CacheBuilder.java:92-127; XMLMapperBuilder.java:169 |
| q2 | 构造契约 | 基础缓存必须 `(String id)` 构造, 装饰器必须 `(Cache)` 构造 — 反射按签名分支实例化, 违反抛 CacheException | CacheBuilder.java:199-218 |
| q3 | CacheKey 算法 | update: null→1 的 ArrayUtil.hashCode → count++/checksum+=base/base*=count → hashcode=37*hashcode+base — 累加式哈希, 顺序敏感; equals 三步快速失败+updateList 逐项值比较 | CacheKey.java:71-95; 测试 CacheKeyTest |
| q4 | LruCache 双 map | keyMap=LinkedHashMap(accessOrder=true) 触摸序; removeEldestEntry 超容量记 eldestKey; putObject 后 cycleKeyList 把 eldestKey 从 delegate 同步淘汰 — 访问序同步 | LruCache.java:31-93 |
| q5 | 事务性缓存 | TransactionalCache 三暂存: 读未命中记 entriesMissedInCache(供 BlockingCache unlock); 写只入 entriesToAddOnCommit; commit 才 flush 到 delegate(clearOnCommit 时先 clear); rollback 只 unlock 丢弃 | TransactionalCache.java:43-95; TransactionalCacheManager.java:44-57 |
| q6 | BlockingCache 语义 | 未命中 acquireLock(CountDownLatch 等待), 他线程 put 后 releaseLock — 缓存击穿防护: 单线程查库其余等待 | BlockingCache.java:41-72 |
| q7 | 接口自持同步 | 3.2.6 起核心不再调用 getReadWriteLock — 同步由各缓存自持(PerpetualCache 裸 HashMap 非同步, 依赖 SynchronizedCache/BlockingCache 装饰) | Cache.java:77-90; PerpetualCache.java:24-35 |
| q8 | 一级 vs 二级 | 一级: BaseExecutor 内嵌 PerpetualCache+CacheKey 直接存取(M-2 已述, 会话级); 二级: CacheBuilder 装饰链+CachingExecutor.tcm 事务化(命名空间级, 跨会话) | BaseExecutor.java:58(M-2); CachingExecutor.java:96-140(M-2) |

→ 引出 M-3: Mapper 代理 — 二级缓存的 namespace 归属与 MapperRegistry 的 knownMappers 注册对齐; M-7 与 Redis 3.5 对照(本地 vs 分布式缓存)。
