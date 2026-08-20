# Redisson 源码分析 — 交接文档 v3 超详细版 (阶段3.6, 全 9 域交付)

> **日期**: 2026-08-13 | 阶段3.6 Redisson (4.6.2-SNAPSHOT, core 2804 文件, 22 顶层包)
> **⚠️ 总入口**: 阶段3 总交接见 `../HANDOFF-STAGE3.md` (M/MP/Redis/Redisson 全状态) — 本文为 Redisson **分域唯一入口** (自包含, 无需回溯历史会话)。
> **规划权威**: `REDISSON-PLAN.md` (9 域 v2.1: 1 导论 RD-0 + 5🔴 + 3🟡, 含三次 REVIEW 记录 — 09 审计/六层深审/跨域引用专项/收官全域检查)。
> **方法论权威**: `talk-method/source-code-analysis/methodology/zh/` (01-09; **07 五维度全量审查必读** — 每域收官轮用)。
> **并行**: 与阶段3.5 Redis (另一 AI) 并行 — Redis 已完成 15+/33 域, 产出目录 `redis/outlines/rXX-*` 为引用目标。
> **给新 AI**: 阅读顺序 §零~§一 → §二~§十 (各域机制速查) → §十一 (REVIEW 教训与铁律) → §十二 (知识网络) → §十三 (ES 交接指引) → 开工。**阶段3.6 已 100% 完成 — 本文是归档交接, 直接进入阶段3.7 ES**。

---

## §零 当前状态速查 (2026-08-13, **全 9 域完成**)

| 域 | 目录 | 类型 | 状态 | 大纲 | harness | 问 |
|:--:|---|:--:|:--:|:--:|:--:|:--:|
| RD-0 导论 | outlines/rd0-intro | 🔴 A* (导读) | ✅ | 1 篇 | — | — |
| RD-1 主类+连接管理 | outlines/rd1-connection | 🔴 A | ✅ | 3 篇 | 20/20 | 50 |
| RD-3 Codec | outlines/rd3-codec | 🔴 A | ✅ | 3 篇 | 9/9 | 50 |
| RD-4 命令流水线 | outlines/rd4-command | 🔴 A | ✅ | 3 篇 | 9/9 | 50 |
| RD-2 RLock+Watchdog | outlines/rd2-rlock | 🔴 A | ✅ | 3 篇 | 15/15 | 50 |
| RD-5 RMap | outlines/rd5-rmap | 🟡 B | ✅ | 2 篇 | — | 50 |
| RD-6 本地缓存 | outlines/rd6-localcachedmap | 🔴 A | ✅ | 3 篇 | 12/12 | 50 |
| RD-7 Spring 集成矩阵 | outlines/rd7-spring | 🟡 B | ✅ | 3 篇 | — | 50 |
| RD-8 基础数据结构 | outlines/rd8-basic | 🟡 B | ✅ | 2 篇 | — | 50 |

**执行序全部完成**: RD-0 → RD-3 → RD-1 → RD-4 → RD-2 → RD-5 → RD-6 → RD-7 → RD-8

**📊 项目总量**: 23 篇大纲 / 63 个闭环笔记 / 5 harness (65/65 PASS) / 400+ completeness 问 / 9 份 review-notes / 1 份 knowledge-planning 每域 / 全域 122 文件。

> ⚠️ **执行序注记**: 实际 RD-1 先于 RD-3 完成 (RD-3 是编码层叶子, RD-1 是连接层枢纽); 严格拓扑序下 RD-3 应先, 但 RD-1 的源码依赖 (Codec 被命令层消费) 不影响其分析独立性。已按规划序全交付。

---

## §一 方法论铁律 (本阶段实战验证, 新 AI 必读)

1. **09 怀疑审计每域必走**: 数字/行号穷举 grep (RD-1 抓 7 处: ConnectionManager 报错行号/acquire 无超时/connectingThread volatile; RD-2 锁族 9→14 修正; RD-6 LOAD"全量"→"增量"修正 — 全是实测纠错)
2. **行号必须 awk/sed 验证**: 大纲引用全经数值核对, 不靠记忆 (ConnectionManager 104-106/107-108 分扯; RedissonMap 清理五路 zrem×3+hdel 教训)
3. **跨域引用只引真实存在域** (06 §2/§6): 引用前 `[ -d ]` 核验目标目录; 并行 AI 侧 Redis 域在快速产出 — **每域收官轮必重扫 redis/outlines 补新链** (r28/r29-pubsub 是中途补链的实例)
4. **双链四行格式**: 每篇 header 必须 `前置/复用/对照/引出` (06 §6); 复用≠省略 (06 §2.5) — 内核引用 + 本层视角展开 + 五件事检查; **对照声明必须在正文出现同词 + 至少一句摘要** (r22/s29 对照零展开被抓过)
5. **逆向承接**: 阶段2 已产出的 s75/s77 等大纲有"深入在阶段3"声明, RD 大纲必须显式回应 (承接点表)
6. **harness 实证**: 🔴 域强制, 机制理解用极简复现验证; 5 个 harness 65/65 — 每个都抓到过自身实现缺陷 (同步模拟 Lua 原子/断言方向)
7. **禁止引未分析域**: Redis 未产出域不得引用; 若并行 AI 补产出 → 06 §3 新发现回流补链
8. **编号漂移警惕**: MyBatis m7 引用 `R-1-redis` vs 实际 `r1-object` — 引用一律用真实目录名; B-10/B-12 (规划编号) vs s75/s77 (产出目录) 教训
9. **裸行号八连复发铁律**: RD-1/3/4/2/5/6/7/8 每域 REVIEW 都抓出裸行号 (`(Lxxx)` 缺文件名) — **根治方案**: 写作规范改为"每个行号独立 `(File.java:line)` 格式, 禁止括号内简写", 域完成后必跑 `grep -rnE '\(L[0-9]+' *.md | grep -v java:` 全量扫描 (注意 `(Lxxx, File.java)` 是合法格式)
10. **六层深审必真找问题**: 每域深审记录 4-8 项 (行号偏差/编造/语义/覆盖), 零发现=不合格

---

## §二 RD-0 导论 (✅ 已交付, 导读域无闭环)

**文件**: `outlines/rd0-intro/01-introduction.md`

**机制速查**:
| 知识点 | 锚点 |
|:--|:--|
| 一句话定位 | README "Valkey & Redis Java Client and Real-Time Data Platform" — 客户端+实时数据平台双身份 |
| vs 传统客户端 | 对象级 API (内部 Lua/命令批) vs 命令级; 可靠性内建 (续期/失效/重试封装在对象内) |
| 生态全景 | api/ 149 同步接口 ×4 编程模型 (Sync/Async/Reactive/Rx) |
| 部署拓扑 | 5 模式 + Valkey 兼容 (7.2.5+) + Pro 声称 8 模式 (README 超前, 开源仅 5 分支) |
| 版本演进 | 4.0.0 (2025-12-16) 断裂点: Reliable Pub/Sub + Spring Boot 4 + Jackson 可选 + Native 引擎; FST/Marshalling 删除 |
| **Spring Boot 四模块** | starter (替换 ConnectionFactory) / data (适配 RedisTemplate) / cache (替换 CacheManager) / transaction (事务管理器) |

**负面空间**: 不替代服务端 / 本地缓存弱一致 / 不强对齐 JDK 语义 / 部分类型仅特定模式。
**双链**: 前置 [[r1-object]] [[r28-networking]] [[r21-db]] | 复用 [[s75-boot-redis]] [[s77-boot-cache]] | 对照 [[s19-cacheable]] [[h13-datasource]] [[m7-cache]] | 引出 [[rd1-connection]] [[rd2-rlock]] [[rd7-spring]]

---

## §三 RD-1 主类+连接管理 (✅ 🔴 A, 3 篇 + harness 20/20)

**目录**: `outlines/rd1-connection/` (01-create / 02-connection / 03-pubsub-dns + 8 闭环 + 50 问 + review-notes)
**核心文件**: Redisson(1532) + Config(1351) + ServiceManager(804) + connection/(39)

**核心机制速查**:
| 机制 | 源码锚点 |
|:--|:--|
| 五步初始化链 | Redisson.java:66-86 (copy→Manager.create→createCommandExecutor→EvictionScheduler+WriteBehindService→register LockRenewalScheduler) |
| 模式工厂 5 分支 | ConnectionManager.java:89-111, ConfigSupport.java:844-859 (优先级 MasterSlave>Single>Sentinel>Cluster>Replicated) |
| ServiceManager 中央工厂 | ServiceManager.java:122-156 (EventLoopGroup/HashedWheelTimer/IdleConnectionWatcher/ElementsSubscribeService/NatMapper/QueueTransferService), register:766 服务注册表 |
| lazyConnect 单飞锁 | MasterSlaveConnectionManager.java:190-227 (CAS latch + volatile connectingThread 防自死锁 + isCompletedExceptionally 失败重试) |
| connect 重试循环 | MasterSlaveConnectionManager.java:229-262 (retryAttempts+1=5 次, 配置错/中断不重试, EqualJitter 1-2s) |
| doConnect 有界等待 | L286-331 (connectTimeout*max(1,minIdle) 防 latch 卡死; 失败 internalShutdown 清理) |
| detectCluster | L264-284 (EVAL 双 key 脚本 → CROSSSLOT → setClusterDetected 自动识别集群) |
| 读写分离路由 | MasterSlaveEntry.java:569-604 (ReadMode 三态, 写固定 master 池; SLAVE 降级 master) |
| AsyncSemaphore 池容量 | ConnectionsHolder.java:44-59,141,224,263 (permit 精确配对; 4.6.1 tryRun 竞态史; counter==poolMax 不变式) |
| DNS 故障切换 | DNSMonitor.java:53-283 (自循环+多轮确认防抖+成功才提交); MasterSlaveEntry.java:500-545 changeMaster (旧 master 降级 slave+失败回滚) |

**harness 验证**: MiniConnectionManager 20/20 (模式工厂 5 分支 / lazyConnect 单飞并发只连一次 / 重入不死锁 / 失败重试 / permit 超容量回滚)
**REVIEW 教训**: ConnectionManager.java:104-106 (throw) vs 107-108 (connect) 精确行号分扯; acquireConnection 无池层超时 (CompletableFuture 返回)

---

## §四 RD-3 Codec 序列化 (✅ 🔴 A, 3 篇 + harness 9/9)

**目录**: `outlines/rd3-codec/` (01-byte-protocol / 02-default-composite / 03-compression-type + 8 闭环 + 50 问)
**核心文件**: client/codec (9 接口+原型) + org.redisson.codec (36 实现) — **双包结构**

**核心机制速查**:
| 机制 | 源码锚点 |
|:--|:--|
| 双包边界 | Codec 接口在 client/codec/Codec.java:30 (协议面) vs 实现在 org.redisson.codec (实现面); StringCodec implements JsonCodec 跨包证明分层非隔离 |
| 接口契约 | Codec.java:37-79 (4 组 E/D: value/mapKey/mapValue + getClassLoader; 双构造器要求默认+ClassLoader) |
| 原型零开销 | StringCodec.java:34-53 (INSTANCE 单例; writeCharSequence/toString 直走 ByteBuf 零分配) |
| 默认 Kryo5 三池化 | Config.java:165 (copy ctor); Kryo5Codec.java:97-236 (Pool<Kryo> 1024+Input 512+Output 512; write/readClassAndObject 类型内置; 失败 out.release L228) |
| **默认根因** | 4.0.0 "Jackson library is now optional" → 默认不能依赖 Jackson → Kryo5 |
| CompositeCodec 三委托 | org.redisson.codec/CompositeCodec.java:30-45 (mapKey/mapValue/value; 2 参 value=null 合法 — Stream 只玩 map 语义 RedissonReliableTopic:82) |
| 压缩装饰器 | LZ4CodecV2.java:44-96 (decompressionSize 头+解压流+innerCodec.decode; 默认 inner=Kryo5; LZ4 两版并存 lz4-java vs commons-compress) |
| 类型保真三档 | JsonJacksonCodec.java:105 (readValue Object.class 丢类型) / TypedJsonJacksonCodec.java:40-75 (TypeReference 指定) / Kryo5 (ClassAndObject 内置) |
| 线程安全三策 | Kryo 池化 (L123) / ObjectMapper 单例共享 (JsonJackson:57) / Protobuf RuntimeSchema 缓存 (L189) |
| 命令衔接 | CommandAsyncService.java:471 (codec 显式传参); CommandDecoder.java:565-575 (selectDecoder 从 CommandData 取 codec; null 兜底 StringCodec) |

**harness 验证**: MiniCodec 9/9 (原型往返/复合委托/value=null 契约 NPE/丢类型 vs 类型内置/单例并发安全)
**REVIEW 教训**: Codec 接口不在 org.redisson.codec 是 Pass 1 才发现的 (09 审计未辨明双包); CompositeCodec value=null 非 bug 是 Stream 场景契约

---

## §五 RD-4 命令流水线 (✅ 🔴 A, 3 篇 + harness 9/9)

**目录**: `outlines/rd4-command/` (01-exec-retry / 02-protocol-capability / 03-lua-batch + 8 闭环 + 50 问)
**核心文件**: command/ 12 文件 4027 行 (CommandAsyncService 1256 + RedisExecutor 928 + CommandBatchService 828)

**核心机制速查**:
| 机制 | 源码锚点 |
|:--|:--|
| async 汇聚点 | CommandAsyncService.java:690-731 (resp3 适配 + SORT_RO 降级探测 + Client-Side Caching 失效钩子 + new RedisExecutor) |
| 执行管线 | RedisExecutor.java:122-230 (借连接→sendCommand→响应→releaseConnection 的 CompletableFuture 链; getCodec/getConnection/三定时器) |
| 重试协议 | RedisExecutor.java:278-375 (定时器驱动递归 execute; 连接/写失败可重试, 响应超时不重试防重复副作用; blocking 命令特殊路径) |
| noRetry 语义 | CommandAsyncService.java:489-492 evalWriteNoRetryAsync (续期幂等禁重试防放大 — RD-2 消费) |
| 超时三定时 | RedisExecutor.java:232-276 (connectionTimeout 池满 / writeTimeout EventLoop 堵含 countPendingTasks / responseTimeout 无响应; 文案带排障建议) |
| RESP3 适配 | ServiceManager.java:686,689-705 (RESP3MAPPING: Stream/ZSet 读族 _V2 版) |
| 能力探测降级 | CommandAsyncService.java:688,694-714 (SORT_RO static AtomicBoolean 乐观探测→ERR unknown command 永久降级); 542,592-595 (EVALSHA_RO 同构) |
| Lua EVALSHA 自愈 | CommandAsyncService.java:578-659 (map→calcSHA→EVALSHA; NOSCRIPT→loadScript 到对节点重发; EVALSHA_RO 降级; useScriptCache 默认 true) |
| 缓存失效钩子 | CommandAsyncService.java:717-727 (写命令+hasCachingInstances→evictClientSideCaching — RD-6 衔接) |
| 批处理 | CommandBatchService.java:53,273-327 (按 NodeSource 分组+skipResult 快路径+失败 tryFailure 全灭; batch=pipeline 非事务) |

**harness 验证**: MiniCommandExecutor 9/9 (正常执行/连接重试 2 次/有界耗尽 4 次/响应超时不重试/noRetry 单次)
**REVIEW 教训**: 线程模型 (回调在 EventLoop) 是 completeness 强制回填; r16-transaction 引用未产出域被抓 → 改 r20-server

---

## §六 RD-2 RLock+Watchdog (✅ 🔴 A, 3 篇 + harness 15/15)

**目录**: `outlines/rd2-rlock/` (01-lock-wait / 02-watchdog / 03-lock-family + 8 闭环 + 50 问)
**核心文件**: RedissonLock(600) + RedissonBaseLock(290) + renewal/(8) + LockPubSub + **锁族 14 类**

**核心机制速查**:
| 机制 | 源码锚点 |
|:--|:--|
| tryLock 四步 | RedissonLock.java:227-300 (首试→订阅→循环"试-等-试"; 时间会计扣减保证 ≤waitTime; latch.tryAcquire(min(ttl,time))) |
| 加锁 Lua | RedissonLock.java:214-224 (exists/hexists→hincrby+1+pexpire; else pttl — 可重入哈希锁, hash 字段=uuid:threadId) |
| 解锁 Lua | RedissonLock.java:348-360 (hexists owner 校验→hincrby-1→counter>0 续期 / =0 删+publish); forceUnlock L336-346 (无条件 del+publish) |
| Watchdog 开关 | RenewalTask.java:40-70,97-185 (CAS 单例 LockTask→tryRun/schedule→lease/3=10s 固定心跳→run→execute→schedule 续排→锁尽 stop) |
| 批量续期 Lua | LockTask.java:37-104 (AsyncChunkProcessor chunk=100; 单 Lua 多键 hexists→pexpire; ContainsDecoder 过滤失效→cancel) |
| LockEntry 记账 | renewal/LockEntry.java:29-61 (多线程持有者表; getFirstThreadId 代表者; hasNoThreads→停) |
| 订阅唤醒 | LockPubSub.java:41-52 (Semaphore(0) + redisson_lock__channel; UNLOCK_MESSAGE=0→release(1) / READ=1→release(queueLength)) |
| **三续期器** | LockRenewalScheduler.java:30-33 (LockTask 普通 / ReadLockTask 读锁 keyPrefix L33,66-73 / FastMultilockTask 字段集 L32,78) |
| **14 锁族矩阵** | 等待(订阅/队列/自旋)×重入(可/不可)×组合(单/联/多数/读写)×防御(fencing); Fair zset 排队 L133,223 / Spin backOff L85-96 / RedLock majority / RW 双 key / Faster group |

**harness 验证**: MiniRLock 15/15 (可重入计数 2/owner 校验/重入递减/forceUnlock/tryRun 防重入/锁尽停跳/并发互斥)
**REVIEW 教训**: 锁族 9→14 (ReadWriteLock 家族); 三续期器只讲 LockTask 被抓 (R5 覆盖缺口); r22/s29 对照零展开补全

---

## §七 RD-5 RMap (✅ 🟡 B, 2 篇)

**目录**: `outlines/rd5-rmap/` (01-map-io / 02-map-cache + 6 闭环 + 50 问)
**核心文件**: RedissonMap(1967) + RedissonMapCache + MapOptions + EvictionScheduler(8)

**核心机制速查**:
| 机制 | 源码锚点 |
|:--|:--|
| 写三路 | RedissonMap.java:659-698 mapWriterFuture (WRITE_BEHIND 缓冲 L664-670 / WRITE_THROUGH 同步 supplyAsync L673-684 / 异步 writerAsync L686-692; condition 门控) |
| WriteBehind | MapOptions.java:62-63 (writeBehindBatchSize=50/Delay=1000 批量合并; RetryableMapWriter 重试 L101 默认 0 次) |
| MapLoader 读通 | api/map/MapLoader.java:28-36 (Redis miss→load(K) 外部源回填; 并发 miss 无去重) |
| 命令面 | RedissonMap.java:472-1604 (get→HGET/put→HSET/putIfAbsent→HSETNX/putAll→HMSET/remove→HDEL/HSCAN 遍历) |
| RMapCache 五结构 | RedissonMapCache.java:129-179 (主 hash+TTL/idle/LRU zset+options hash; 惰性检查 zscore 比较+idle 刷新 zadd t+now+LRU 更新) |
| 自适应清理 | EvictionTask.java:71-113 (sizeHistory 三态调 delay: 递减×1.5/持续大/4/清零×1.5; 5s~2h; 错误也续排) |
| 清理 Lua | MapCacheEvictionTask.java:77-111 (zrangebyscore(0,now)→**zrem×3 辅助 zset+hdel 主 hash 五路**; unpack ≤4999) |

**REVIEW 教训**: 清理五路 (zrem+hdel) 初稿只写 hdel 被抓; "Map 命令面" 覆盖缺口 → 新增命令面节; 裸行号第五次复发

---

## §八 RD-6 RLocalCachedMap 本地缓存 (✅ 🔴 A, 3 篇 + harness 12/12)

**目录**: `outlines/rd6-localcachedmap/` (01-local-read / 02-invalidate / 03-reconnection-ops + 7 闭环 + 50 问)
**核心文件**: RedissonLocalCachedMap(1474) + cache/(27) + LocalCacheListener

**核心机制速查**:
| 机制 | 源码锚点 |
|:--|:--|
| 双层级 | RedissonLocalCachedMap.java:44 (extends RedissonMap — RMap 远程载体 + LocalCacheView 本地层) |
| 读路径 | RedissonLocalCachedMap.java:285-317 (本地 hit 零网络; miss 按 StoreMode: LOCALCACHE loader 回填 L294-306 / LOCALCACHE_REDIS 查 Redis L309-315; storeCacheMiss 防穿透) |
| StoreMode | LocalCachedMapOptions.java:121-131 (LOCALCACHE 只本地 / LOCALCACHE_REDIS 双存) |
| SyncStrategy | LocalCachedMapOptions.java:64-79 (**默认 INVALIDATE** L281); INVALIDATE 广播 hash (接收者清+回源) vs UPDATE 广播全值 (接收者直更) |
| excludedId | LocalCacheListener.java:263,278,297 (广播带 instanceId 排除发送者防循环) |
| **重连策略** | LocalCachedMapOptions.java:42-60 (**默认 NONE** L267); CLEAR 全清 / **LOAD 增量补漏** (LocalCacheListener.java:476-511 更新日志 zset valueRange lastInvalidate..+inf 只失效断线期 key; 断线>10min 全清 cacheUpdateLogTime RedissonLocalCachedMap:55) |
| 消息族 | LocalCacheListener.java:220-274 (Disable→清+DisableAck L221-237 / Enable / Clear→全清 L261-274; DisabledKey 持久化) |
| CacheProvider | LocalCacheView.java:312-350 (CAFFEINE Caffeine builder TTL/idle/size L317-326 / REDISSON LRU/LFU/Soft/Weak/None 按 EvictionPolicy L338-350) |
| 双一致性 | RedissonLocalCachedMap.java:361-401 (本实例写路径 cachePut + 跨实例订阅广播; RD-4 evictClientSideCaching 命令级) |

**harness 验证**: MiniLocalCachedMap 12/12 (读路径零回源/INVALIDATE 清空回源/UPDATE 零回源/excludedId 防循环/并发最终一致)
**REVIEW 教训**: **LOAD"全量重载"→"增量补漏"重大修正** (更新日志 zset 只失效断线期 key); 默认值实证 (SyncStrategy INVALIDATE/Reconnection NONE); 10 分钟断线窗口

---

## §九 RD-7 Spring 集成矩阵 (✅ 🟡 B, 3 篇)

**目录**: `outlines/rd7-spring/` (01-starter / 02-cache / 03-data-tx + 6 闭环 + 50 问)
**核心文件**: redisson-spring/ 四模块 (starter 5 + cache 11 + data 18 版本子模块 + transaction 6)

**核心机制速查**:
| 机制 | 源码锚点 |
|:--|:--|
| starter 取代链 | RedissonAutoConfigurationV4.java:60,78-97,114-116 (RedissonClient destroyMethod=shutdown → RedissonConnectionFactory → RedisTemplate/StringRedisTemplate; @ConditionalOnMissingBean 用户优先) |
| 时序 | RedissonAutoConfigurationV4.java:31-33 (@AutoConfiguration(before=DataRedisAutoConfiguration) 抢在 Boot 默认前) |
| Cache 包装 | redisson-spring-cache/RedissonCache.java:41-159 (implements Cache 包装 RMapCache: get→map.get L84-89; put→fastPut(ttl) L123-137; evict→fastRemove; NullValue/null 语义) |
| getCache 双路 | RedissonSpringCacheManager.java:216-282 (config.ttl>0→createMapCache RMapCache L259 else createMap RMap L238; configMap 按名; configLocation YAML) |
| data 适配 | redisson-spring-data-26/RedissonConnectionFactory.java:49-126 (implements RedisConnectionFactory+Reactive; getConnection L118); RedissonConnection.java:178-663 (get/set/setNX/setEx 操作集; execute 反射分发 L178-201) |
| Transaction 模板 | RedissonTransactionManager.java:37-105 (extends AbstractPlatformTransactionManager; doBegin L73/doCommit L92/doRollback L102 三模板) |
| 版本矩阵 | redisson-spring-data-{16..41} 18 子模块 (data-26→SD Redis 2.6.10 / data-30→3.0.12 / data-40→4.0.5; 编译期隔离) |

**REVIEW 教训**: 版本对应实证 (2.x/3.x/4.x 分界); execute 反射失败处理补全; 裸行号第七次复发 (01 几乎无锚点)

---

## §十 RD-8 基础数据结构 (✅ 🟡 B, 2 篇)

**目录**: `outlines/rd8-basic/` (01-basic-command / 02-unified-wait + 6 闭环 + 50 问)
**核心文件**: RedissonBucket + RedissonAtomicLong + RedissonSemaphore + RedissonCountDownLatch + RedissonBitSet

**核心机制速查**:
| 机制 | 源码锚点 |
|:--|:--|
| 命令封装模式 | RedissonBucket.java:100-147 (get→GET L141-147; getAndSet→GETSET L107; getAndExpire→GETEX L117-137 原子获取+过期); RedissonAtomicLong.java:85-134 (addAndGet→INCRBY L91; DECR L134) |
| 原子性 | INCRBY/DECR 服务端单线程原子; compareAndSet→Lua EVAL_LONG_SAFE (读-比-写) |
| **统一等待模型** | RedissonSemaphore.java:66-98 (tryAcquire→subscribe→循环 tryAcquire?return:latch.acquire→退订 — 与 RLock.tryLock 完全同构); SemaphorePubSub.java:27-42 extends PublishSubscribe<**RedissonLockEntry**> (复用 RLock entry 强证据) |
| RSemaphore | RedissonSemaphore.java:267-269,445-446 (release Lua decrby; onMessage latch.release(min(acquired,msg)); 非公平) |
| RCountDownLatch 门闩 | CountDownLatchPubSub.java:40-56 (ZERO→运行 listeners+latch.open L50 / NEW→latch.close L53); RedissonCountDownLatch.java:253-259 (countDown Lua: decr→v<=0 del→v==0 发布 ZERO — **归零删 key**) |
| RBitSet | RedissonBitSet.java:47-94 (BITFIELD_LONG 任意位宽 1-64bit 有/无符号+原子自增; SETBIT/GETBIT 超集) |
| 接口独立性 | api/RSemaphore.java:29 (extends RExpirable 非 JDK Semaphore); api/RCountDownLatch.java:29 (extends RObject) |

**REVIEW 教训**: countDown 归零删 key 语义补齐; 接口非 JDK 实现修正; 收官轮全域一致性检查 (122 文件 9 域 + 全域裸行号零残留 + 知识网络成环)

---

## §十一 REVIEW 教训与铁律汇总 (本阶段最痛的点)

### 1. 裸行号八连复发 (最顽固)

- RD-1/3/4/2/5/6/7/8 **每域 REVIEW 都抓出裸行号** `(Lxxx)` 缺文件名
- 根因: "方法内简写 + 文件名前置" 的写作习惯 vs 07 标准"锚点必须含文件名"
- 根治: 写作规范改为"每个行号独立 `(File.java:line)` 格式"; 域完成必跑 `grep -rnE '\(L[0-9]+' *.md | grep -v java:`
- 注意 `(Lxxx, File.java)` (文件名在括号内) 是合法格式

### 2. 跨域引用违规 (三次被抓)

- REDISSON-PLAN §五 原引用 R-26/R-11 (未产出) + B-10/B-12 (编号非目录) → 全改真实目录
- RD-4 引 r16-transaction (未产出) → 改 r20-server
- 对策: 引用前 `[ -d ]` 核验; 每域收官轮重扫 redis/outlines 补新链 (r28/r29-pubsub 中途补链实例)

### 3. 重大机制修正 (三次)

- **锁族 9→14**: REDISSON-PLAN 低估, 实测含 ReadWriteLock 家族
- **LOAD "全量重载"→"增量补漏"**: 初稿理解错, 更新日志 zset 只失效断线期 key
- **默认 Codec**: JsonJackson (旧规划) → Kryo5 (Config:165 实证, 4.0.0 Jackson optional 根因)

### 4. 覆盖缺口 (每次 REVIEW 必查)

- RD-4 线程模型节 (completeness 强制)
- RD-2 三续期器 (只讲 LockTask)
- RD-5 Map 命令面 (只讲双写未讲命令映射)
- 对策: REDISSON-PLAN 规划主题 vs 大纲词面逐条核对

### 5. 对照零展开 (06 §2.5)

- r22/s29/m7 等 header 声明对照但正文零摘要 → 补"同词+一句摘要"

---

## §十二 知识网络图 (全域双链)

### 全域图谱 (Obsidian 双链, 全部真实存在)

```
RD-0 (导论) ──引出──→ RD-3 Codec / RD-1 连接 / RD-4 命令 / RD-2 锁 / RD-5 RMap / RD-6 本地缓存 / RD-7 Spring / RD-8 基础
RD-3 (codec) ──前置──→ RD-1/4/5/8  (编码层叶子)
RD-1 (连接)  ──前置──→ RD-2/4/5/6/7 (Hub)
RD-4 (命令)  ──前置──→ RD-2/5/6/8
RD-2 (锁)    ──前置──→ RD-7 (可注 RLock)
RD-5 (RMap)  ──前置──→ RD-6/7
RD-6 (本地缓存) ──依赖──→ RD-5 (载体) + RD-1 (订阅) + RD-4 (钩子)
RD-7 (Spring) ──消费──→ 全部
RD-8 (基础)  ──引回──→ RD-0 (闭环)
```

### 跨仓库引用 (真实目录, 已核验)

| 来源 | 关系 |
|:--|:--|
| redis/outlines/r1-object | 对照 RD-3 (服务端编码 vs 客户端 codec) |
| redis/outlines/r28-networking / r29-pubsub | 对照 RD-4/RD-6 (RESP/PubSub 双面) |
| redis/outlines/r20-server / r21-db / r22-expire / r26-list | 对照 RD-1/5/6/2 |
| redis/outlines/r24-string / r11-bitmap | 复用 RD-8 |
| spring/outlines/s75-boot-redis / s77-boot-cache | 承接 (s75 "连接深入在阶段3") |
| spring/outlines/s19-cacheable / s29-tx-chain / s74-boot-datasource | 对照 RD-7 |
| hikaricp/outlines/h13-datasource / h02-concurrentbag | 对照 RD-1 (连接池架构) |
| mybatis/outlines/m7-cache | 对照 RD-6 (缓存一致性) |

---

## §十三 阶段3.7 ES 交接指引 (下一步)

**⚠️ 阶段3.6 Redisson 已 100% 完成归档 — 新 AI 直接进入阶段3.7 Elasticsearch**

### 1. 现状
- **ES (3.7)**: 11 域规划 (E-1~E-11), ⏳ 未开始
- **执行计划**: issue/源码分析执行计划.md §3.7 (E-1~E-11)
- **范围规划**: issue/Elasticsearch源码学习范围规划.md (11 域)
- **源码**: `/data/workspace/source-code/code/spring/elasticsearch` (ES 服务端, 非 Java client)

### 2. 方法论接力 (已验证 9 域)
- 按 09 怀疑审计 → Pass 0-3 → 六层深审 → harness (🔴) → completeness → 跨域双链 → 深审 → HANDOFF
- **注意**: 全程用本文 §一 的 10 条铁律, 尤其裸行号根治方案 + 跨域引用核验
- **空间超限**: 本文是归档交接 — ES 开工前新建 `es/HANDOFF-ES.md` 从 §零 开始

### 3. 与 Redisson 的衔接点
- RD-0 导论提到 Redisson 的客户端连接面 → ES 服务端连接面对照
- Redis (阶段3.5) 33 域已 15+ 完成 → ES 的存储引擎 (Lucene) 与 Redis 内存引擎对照
- 交叉引用可用已存在的 redisson/outlines/rd*-* + redis/outlines/r*-* 目录 (真实存在)

### 4. 开工步骤
1. 读 issue/源码分析执行计划.md §3.7 + Elasticsearch源码学习范围规划.md
2. 09 怀疑审计 (域清单/数字/依赖方向/顺序)
3. 写 ES-PLAN.md (对齐 REDISSON-PLAN.md 格式: §〇 审计表 + 域清单 + 知识网络)
4. 逐域 Pass 0-3 (方案选择块 + harness + completeness + 深审)
5. 每域收官更新 HANDOFF-ES.md + HANDOFF-STAGE3 §零

---

## §十四 完成检查单

- [x] 阶段3.6 Redisson 全 9 域交付 (RD-0~RD-8)
- [x] 23 篇大纲 + 63 闭环 + 5 harness (65/65) + 400+ 问
- [x] 每域六层深审 + 收官 07 五维度 REVIEW
- [x] 全域一致性: 122 文件 / 裸行号零残留 / 知识网络成环
- [x] REDISSON-PLAN v2.1 (三次 REVIEW 记录) + 各域 knowledge-planning
- [x] HANDOFF-STAGE3 §零 同步 (Redisson 9/9 ✅)
- [x] 本文 v3 归档 (自包含, 含全部机制速查 + 教训 + ES 指引)

**🎉 阶段3.6 Redisson 收官 — 与阶段3.5 Redis 形成服务端/客户端完整闭环, 交接阶段3.7 ES。**
