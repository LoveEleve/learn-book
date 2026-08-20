# M-7 缓存体系 — CacheBuilder 装饰链 + CacheKey + 事务性二级缓存

> 前置: [[M-1-configuration]] (cache 别名) | 复用: [[M-2-executor]] (CachingExecutor 消费/一级缓存) | 对照: [[R-1-redis]] (本地 vs 分布式) | 引出: [[M-3-mapper-proxy]]
> 🟡 Working | 6 KP | [模式: 装饰器+Builder+值语义键+事务性工作集]
> Pass 2 闭环: q1(装饰链组装) q2(构造契约) q3(CacheKey 算法) q4(LruCache 双 map) q5(事务性缓存) q6(BlockingCache) q7(接口自持同步) q8(一级 vs 二级)

**读者处境**: `<cache eviction="LRU" size="1024"/>` 一行配置二级缓存就绪 — 它经 CacheBuilder 变成几层装饰?为何默认 readWrite=true 存的是序列化副本?为何 commit 前其他会话看不到新写入?为何并发同 key 未命中时只有一个线程查库?这篇拆二级缓存的组装链、键值算法与事务化写入。

### 1. 接口与基础 — Cache 7 方法契约 + PerpetualCache 非同步

场景: Cache 接口为什么有 getReadWriteLock 却没人调用?PerpetualCache 线程安全吗?
源码路径:
- `Cache.java:60-76` — 7 方法: getId/putObject/getObject/removeObject/clear/getSize/**getReadWriteLock(default null — 3.2.6 起核心不再调用 L91-97, 同步由缓存自持)**(q7); removeObject 注释说明其仅用于回滚时让 BlockingCache 释放锁(L60-74)
- `PerpetualCache.java:24-35` — **裸 HashMap 非同步**(同步靠 SynchronizedCache/BlockingCache 装饰); equals/hashCode 按 id(M-1 别名体系一致, 无 id 抛 CacheException)
- `CacheKeyTest.java:34-121` — 值语义/顺序敏感/二进制数组/NULL 键不可更新/序列化测试地图
关键设计: 接口收敛(q7): 核心不强制锁协议, 每个缓存自管同步 — 装饰器体系因此可自由组合; PerpetualCache 只是 HashMap 的 Cache 化, 一切能力(淘汰/过期/引用/锁/统计)都是装饰。[模式: 最小接口+自持同步]
数据流: putObject(key, value) → cache.put; getObject → cache.get — 无锁; 线程安全由外层装饰保证。

### 2. 组装 — CacheBuilder 装饰链 (默认/标准链/契约/属性注入)

场景: `<cache/>` 空配置会得到什么?自定义缓存实现还会被包装饰器吗?`<property>` 怎么生效?
源码路径:
- `CacheBuilder.java:92-106` — build(): 默认 `PerpetualCache`; **无装饰器时默认加 LruCache**(setDefaultImplementations L98-106)
- `CacheBuilder.java:110-127` — setStandardDecorators 固定顺序(q1): size 注入→`ScheduledCache`(clearInterval)→`SerializedCache`(readWrite)→`LoggingCache`→`SynchronizedCache`→`BlockingCache`(blocking) — **Logging 在 Synchronized 内**(命中统计计入加锁访问)
- `CacheBuilder.java:95-97` — **装饰器循环仅限 PerpetualCache**(issue#352: 自定义实现只包 LoggingCache)
- `CacheBuilder.java:199-218` — 构造契约(q2): 基础缓存必须 `(String id)` 构造/装饰器必须 `(Cache)` 构造 — 反射按签名分支实例化, 违反抛 CacheException
- `CacheBuilder.java:137-177` — setCacheProperties: `MetaObject.hasSetter`+8 种类型反射转换(String/int/long/short/byte/float/boolean/double)+`InitializingObject.initialize`(L171-177)
- `XMLMapperBuilder.java:169-172` — cacheElement: type 默认 PERPETUAL/eviction 默认 LRU; **readWrite=!readOnly(默认 true → SerializedCache 默认应用)**(q1); blocking 默认 false
关键设计: 两层装饰(q1): 用户 `<cache type= eviction=>` 声明的装饰器 + 标准装饰器固定链; readWrite 默认 true 意味着默认缓存存序列化副本(防引用共享脏读 — 同一对象被多会话直接引用会互相污染); 属性注入走 MetaObject 反射, 未识别的 property 静默忽略。[模式: Builder+装饰器链+反射注入]
数据流: `<cache/>` → CacheBuilder(id).implementation(Perpetual).addDecorator(Lru) → build: base=Perpetual → Lru(用户装饰器循环) → setStandardDecorators(size→Scheduled?→Serialized?→Logging→Synchronized→Blocking?) → builderAssistant.useNewCache 注册进 Configuration.caches。

### 3. 键 — CacheKey 算法与值语义

场景: 为什么 CacheKey 的 equals 先比 hashcode 再比 updateList?null 参数怎么进键?为什么说它顺序敏感?
源码路径:
- `CacheKey.java:40-59` — 字段: multiplier=37/hashcode 初始 17/checksum/count/updateList
- `CacheKey.java:71-79` — update(q3): `ArrayUtil.hashCode(null→1)` → count++/`checksum += base`/`base *= count` → `hashcode = 37*hashcode + base` — **累加式哈希, 每次 update 整体改变**
- `CacheKey.java:91-95` — equals: hashcode/checksum/count 快速失败 → updateList 逐项 `ArrayUtil.equals`(顺序敏感 — 参数顺序不同键不同, 测试 shouldTestCacheKeysNotEqualDueToOrder)
- `CacheKey.java:33-44` — **NULL_CACHE_KEY 单例**: update 抛 "Not allowed to update a null cache key instance"(测试 throwExceptionWhenTryingToUpdateNullCacheKey)
- `CacheKeyTest.java:34-121` — 空键与 null 键相等(shouldDemonstrateEmptyAndNullKeysAreEqual)/二进制数组/序列化/克隆(updateList 副本)
关键设计: 累加式哈希(q3): 每个参数 update 都乘以 count 并累加 — 顺序与值都影响最终 hashcode, 碰撞概率低; checksum 是廉价预过滤(不等则直接不等, 免逐项); NULL 键单例防误改(缓存未命中场景的占位)。[模式: 值语义复合键]
数据流: createCacheKey(M-2 六段) → update×N → equals(快速失败+逐项) → HashMap 定位 → getObject/putObject。

### 4. 淘汰策略 — LruCache 双 map 同步 + Fifo/Scheduled

场景: LruCache 为什么用 LinkedHashMap 自建而非现成 LRU 库?淘汰时底层数据怎么同步删?为什么 getObject 也要 touch?
源码路径:
- `LruCache.java:31-93` — **keyMap=LinkedHashMap(size, .75F, accessOrder=true)**(触摸序 L50); removeEldestEntry 超容量记 `eldestKey`(L54-58); `cycleKeyList`: keyMap.put 触发淘汰 → **eldestKey 从 delegate 同步 removeObject**(L88-93); getObject 先 `keyMap.get(key)` touch 更新访问序(L72); 默认 size=1024(L36)
- `FifoCache.java:31-36` — Deque keyList 先进先出淘汰(putObject 时 cycleKeyList)
- `ScheduledCache.java:28-38` — 默认 1h clearInterval+lastClear 时间戳, 每次操作前 clearWhenStale 检查
关键设计: 双 map 同步(q4): keyMap 只管淘汰顺序(访问序), delegate 管数据 — put 后经 removeEldestEntry 拿到 eldestKey 再删 delegate, 两处一致; getObject 也 touch(否则只 put 更新序, 读不更新 — LRU 语义错误); 三策略可互换(eviction 属性), 全部是"顺序记录器+delegate 同步删除"模式。[模式: 代理+LRU/FIFO/定时]
数据流: putObject → delegate.put + cycleKeyList(keyMap.put→超容量记 eldest) → delegate.removeObject(eldest); getObject → keyMap.get(touch) → delegate.get; Scheduled: 操作前 clearWhenStale(超时则 delegate.clear)。

### 5. 事务性二级缓存 — TransactionalCache 三暂存 + BlockingCache 击穿防护

场景: 为什么 commit 前其他会话看不到新写入的二级缓存?回滚时 BlockingCache 的锁怎么释放?并发同 key 未命中会发生什么?
源码路径:
- `TransactionalCacheManager.java:44-57` — per-Cache TransactionalCache 包装(`computeIfAbsent`); commit/rollback 全量遍历所有事务缓存
- `TransactionalCache.java:43-51` — 三暂存: `clearOnCommit`/`entriesToAddOnCommit`/`entriesMissedInCache`
- `TransactionalCache.java:65-81` — getObject 三态(L65-75): 命中返回/未命中记 miss(L68-70, issue#116)/**clearOnCommit 时返回 null**(L73-75, issue#146 — 本事务 clear 后读不到旧缓存强制查库); putObject 只入 pending(L78-80); **commit: clearOnCommit→delegate.clear→flushPendingEntries→reset**(L85-95); rollback: `unlockMissedEntries`→reset; clear() 只置 clearOnCommit 标志(L89-90)
- `BlockingCache.java:41-72` — `ConcurrentHashMap<key, CountDownLatch>`; getObject `acquireLock`(未命中等待 L68-73)/putObject `releaseLock`(L59-63)/removeObject 释放(回滚路径)
关键设计: 事务化写入(q5): 本事务内的写先入 pending, commit 才 flush 到共享缓存 — 回滚的写不污染全局; **releaseLock 也随 commit 延迟**: BlockingCache.putObject 的 finally releaseLock(L59-63) 只在 flush 阶段才被触发, 回滚走 unlockMissedEntries→removeObject 释放(L78-81 "despite its name, called only to release locks"); BlockingCache 是缓存击穿防护(q6): 同 key 并发未命中只有第一个查库, 其余等待其 put 后取现成值。[模式: 事务性工作集+锁]
数据流: query → tcm.getObject(BlockingCache acquireLock+未命中记 miss) → delegate.query → tcm.putObject(pending, 无锁释放) → commit → TransactionalCache.commit → flushPendingEntries → BlockingCache.putObject(finally releaseLock) → reset; rollback → unlockMissedEntries → BlockingCache.removeObject(releaseLock) → reset。

### 6. 一级 vs 二级对照 + 负面空间

场景: 两级缓存谁先查?生命周期?清了二级一级会跟着清吗?
源码路径:
- 一级: `BaseExecutor.localCache`(PerpetualCache, M-2 L58/68) — 会话级, CacheKey 直接存取, update/commit/rollback/STATEMENT scope 清空
- 二级: CachingExecutor(M-2 §6)+CacheBuilder 装饰链+tcm — 命名空间级(ms.getCache), 跨会话, 事务提交才生效
- `XMLMapperBuilder.java:111-118` — cache-ref 跨 namespace 引用(M-1 incomplete 关联)
关键设计: 两级分工(q8): 一级防会话内重复查询(写操作即失效); 二级跨会话共享(namespace 粒度, cache-ref 可共享); 查询顺序 二级→一级→库, 写入路径 库→一级→pending→commit 二级。[模式: 分级缓存]
数据流: selectList → CachingExecutor(二级 tcm) → BaseExecutor(一级 localCache) → 库 → 一级 put → tcm pending → commit 后二级可见。
负面空间: **不做分布式**(纯本地 JVM, 多实例数据不一致, 需 Redis 等外部缓存, 对照 R-1); **不做多级淘汰联动**(一级清空不影响二级, 各自独立生命周期); **不做缓存预热**(首查才填充); **不做写穿透**(更新只清缓存不主动回写, 靠下次查询重建)。

→ 引出: 二级缓存 namespace 归属与 MapperRegistry 注册对齐; Redis 阶段将对照本地 vs 分布式缓存 → [[M-3-mapper-proxy]] [[R-1-redis]]
