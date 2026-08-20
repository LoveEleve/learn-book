# Redisson — 知识网络化规划 (RD-1~RD-8, 09 域重审后 v2)

> **日期**: 2026-08-13 | 依据: issue/Redisson源码学习范围规划.md (7 域) + issue/源码分析执行计划.md 阶段3.6 (RD-1~7) + **09 对既有规划保持怀疑 全量域重审** (顶层包扫描+数字穷举+依赖方向+拓扑重排)
> **源码**: `/data/workspace/source-code/code/spring/redisson` (**4.6.2-SNAPSHOT**, CHANGELOG 2026-06-18 发 4.6.1; core 2804 源文件, 22 顶层包)
> **定位**: 阶段3.6 — 数据存储 (Redis 客户端之王). 核心 = **Netty 异步连接池 + RESP 命令执行器 + 数据结构本地化封装**. 与阶段3.5 (Redis 服务端) 互为镜像: 服务端讲"怎么存", Redisson 讲"怎么调"
> **与 Spring Boot 关系 (v2 补充)**: Redisson 是**嵌入式客户端库, 非独立中间件** — 它自身不认识 Spring; 是 `redisson-spring-boot-starter` 通过自动装配把它接入 Spring 容器. 集成四模块: **starter**(自动装配, `@AutoConfiguration(before=DataRedisAutoConfiguration)` 用 RedissonConnectionFactory 替代默认 Lettuce/Jedis)、**spring-data**(RedisConnectionFactory 适配, 让 Spring Data Redis 无缝跑在 Redisson 上)、**spring-cache**(CacheManager, 替换 ConcurrentMap/Lettuce 缓存)、**spring-transaction**(RedissonTransactionManager 事务管理器). 与阶段2 Spring Boot B-10 (Redis 自动配置)/B-12 (缓存自动配置) 形成生态闭环对照
> **知识网络**: 本文含 前置/复用/引出 双链; 与 Redis 33 域 + Spring Boot (B-10/B-12) + Spring Cache + Netty 互联
> **并行**: 与阶段3.5 Redis (另一 AI) 并行执行 — Redisson 规划基于 09 独立审计, 不依赖 Redis 域完成

---

## 〇、09 怀疑审计表 (Redisson, 2026-08-13) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| **域清单 7 个** | 顶层包扫描 (22 包, 2804 文件) | **cache/ 27 文件 (RLocalCachedMap 本地缓存失效机制 RLCSC) 未独立成域** — 现规划仅在 R-5 一句话带过; 1474 行主类 + 27 失效类 = Redisson 定义特征 (分布式一致性面, 面试高频) | **修正: 7 → 8 域** (新增 RD-6 本地缓存) |
| "7 种内置 Codec" (含 FST/Marshalling) | ls codec/ (36 文件) | **FSTCodec/MarshallingCodec 已删除** (版本演进); 现行: Kryo5/JsonJackson/Jackson3/Serialization/SnappyCodecV2/LZ4CodecV2/**ZStdCodec/ProtobufCodec/CompositeCodec** + Jackson3 家族 (Avro/Cbor/Ion/Smile/MsgPack/TypedJson/Jackson3) | **修正: 明确现行清单** (RD-3) |
| "JsonJacksonCodec (默认)" | Config copy 构造器 | **Config.java:165 `oldConf.setCodec(new Kryo5Codec())`** — 默认 = **Kryo5Codec** | **修正: 默认 Kryo5** ✅ |
| 关键类行数 (10 项) | wc -l 穷举 | Redisson 1532/Config 1351/ServiceManager 804/CommandAsyncService 1256/ElementsSubscribeService 126/DNSMonitor 285/RedissonLock 600/RedissonBaseLock 290/RedissonMap 1967 全命中; **LockTask 112 ≠ 113** | **9/10 接受** ⚠️ LockTask -1 |
| 包文件数 (7 项) | find 穷举 | api 801/client 217/connection 39/command 12/codec 36/renewal 8/eviction 8 全命中 | **接受** ✅ |
| 全局配置 6 项 | grep Config | lockWatchdogTimeout=30s (L81)/lockWatchdogBatchSize=100 (L83)/fairLockWaitTimeout=5min (L85)/checkLockSyncedSlaves=true (L87)/useScriptCache=true (L95)/keepPubSubOrder=true (L93) | **接受** ✅ |
| "Watchdog 30s→10s" | grep 续期周期 | **RenewalTask.java:68 `internalLockLeaseTime / 3`** = 10s | **接受** ✅ |
| 三续期器 (Lock/ReadLock/FastMultilock) | ls renewal/ | 8 文件: LockTask/ReadLockTask/FastMultilockTask/LockEntry/ReadLockEntry/FastMultilockEntry/RenewalTask/LockRenewalScheduler; LockRenewalScheduler CAS 单例引用 | **接受** ✅ |
| 5 种服务器模式→5 种 Manager | grep ConnectionManager.create | 5 分支 if-else: MasterSlave/Single/Sentinel/Cluster/Replicated → 5 实现类 | **接受** ✅ |
| "tryLock → Lua (HINCRBY+EXPIRE) → 订阅 channel → Semaphore 阻塞" | grep RedissonLock | Lua 内联 L216-222 (exists/hexists/hincrby/pexpire/pttl); channel `redisson_lock__channel` **L71**; tryLock: tryAcquire→subscribe→`RedissonLockEntry.getLatch()` = **Semaphore(0)**, LockPubSub onMessage release | **接受** ✅ (channel 在 RedissonLock 非 LockPubSub) |
| "DNSMonitor → changeMaster/changeSlave" | grep DNSMonitor | DNSMonitor:152 → masterSlaveEntry.changeMaster; MasterSlaveConnectionManager:649 changeMaster | **接受** ✅ |
| 依赖方向/顺序: R-1→R-4→R-2→R-3→R-5→R-6 | 拓扑重跑 | **Codec 是叶子依赖** (RedisClient/CommandAsyncExecutor/全部结构消费) — 原排第 4 位反拓扑 (同 Redis 教训: 编码层先行) | **修正: RD-3 提前至第 1** |
| R-1/R-4 边界 | 文本比对 | 原 R-1 描述含"CommandAsyncService 读写分离+重试" 与 R-4 重叠 | **边界重画**: R-1=初始化/连接/订阅/DNS/全局态; R-4=命令流水线 (异步模型/重试/批量/Lua) |
| 4.x 新特性覆盖 | 顶层扫描 | **未覆盖**: Protocol RESP2/RESP3 可配 (Config:113) + ValkeyCapability (Valkey 兼容, Config:115)/FencedLock/SpinLock/FasterMultiLock/NonReentrantFairLock (锁族扩展)/DelayStrategy jitter 重试 (BaseConfig:67 EqualJitterDelay 1-2s) | 并入 RD-1/RD-2/RD-4 一句话 |
| retryAttempts/retryDelay 无数字 | grep BaseConfig | **retryAttempts=4** (BaseConfig:62) + retryDelay=**EqualJitterDelay(1000ms,2000ms)** (L67) — 4.x 新 DelayStrategy | 补数字 (RD-4) |
| "responseTimeout 重试三件套" | 全局 grep | **responseTimeout 已移出全局配置** (仅 BatchOptions:72/TransactionOptions:29 残留); 4.x 等价物 = **timeout=3000ms** (BaseConfig:58 "Redis server response timeout") | **修正: 三件套 = retryAttempts(4)+retryDelay(jitter 1-2s)+timeout(3000ms)** |
| ServiceManager 含 NameMapper/CommandMapper | grep ServiceManager 全字段 (L122-156) | **NameMapper/CommandMapper 在 Config:117-119 非 ServiceManager**; 实测 ServiceManager 字段: ConnectionEventsHub/HashedWheelTimer(138)/IdleConnectionWatcher(140)/ElementsSubscribeService(144)/NatMapper(146)/QueueTransferService(154)/LockRenewalScheduler(156, **register() 注入** L766)/MapResolver(liveobject) | **修正: 组件清单错位** |
| 初始化链描述 | Redisson 构造器 | **Redisson.java:66-86 实证**: configCopy→ConnectionManager.create→createCommandExecutor→**EvictionScheduler+WriteBehindService 创建**→**register(new LockRenewalScheduler)**; **lazyInitialization 默认 false** (Config:111, 默认立即 connect) | 补强 RD-1 (规划原文未含) |
| RD-4 执行核心 | ls command/ | **RedisExecutor (928 行) 是单命令执行核心** (attempts/retryStrategy=DelayStrategy/timeout 管理/连接获取/响应处理, RedisExecutor.java:78-91) — 旧规划仅提 Executor/Service 接口 | 补强 RD-4 |
| "RLCSC 失效策略" | grep RLCSC 全源码 | **RLCSC 源码零命中 = 外部术语** (面试圈缩写); 实际机制: **SyncStrategy 枚举 NONE/INVALIDATE/UPDATE** (api/LocalCachedMapOptions.java:64-79) + ReconnectionStrategy + evictionPolicy | **修正: RD-6 术语用 SyncStrategy, RLCSC 标注外部术语** |
| 锁族清单 | ls root 包 | 9 锁类实证: RedissonLock/FairLock/**FencedLock/SpinLock/NonReentrantLock/NonReentrantFairLock**/MultiLock/RedLock/**FasterMultiLock** | 具体化 (RD-2) |
| RD-7 子模块 | find redisson-spring | 4 子模块 = boot-starter/cache/data/**transaction** — **RedissonTransactionManager 未提** (Spring 事务管理器, 6 文件) | 补强 RD-7 边界 |

**覆盖率报告**: 既有规划 7 域 → 重审后 **8 域** (+1: 本地缓存失效机制 — cache/ 27 文件 + 1474 行主类, 定义特征级)。与 Redis 18→33 不同, Redisson 客户端域清单错误率较低 (12.5%), 因 800+ API 是门面非机制, 淘汰面大 (reactive/rx/liveobject/executor/remote/mapreduce/transaction/jcache 均无定义特征)。**二次 REVIEW (2026-08-13): 再抓 3 修正 + 5 补强** (术语编造 RLCSC/responseTimeout 过时/组件错位/RedisExecutor 漏项等 — 全部实证)。

---

## 一、入口点与主线

`Redisson.create(config) (Redisson.java:119) → new Redisson(config): configCopy→ConnectionManager.create (5 分支选实现, L77/L89) → createCommandExecutor → EvictionScheduler+WriteBehindService → register(LockRenewalScheduler)` — ServiceManager 中央工厂 (HashedWheelTimer/IdleConnectionWatcher/ConnectionEventsHub/ElementsSubscribeService/QueueTransferService/NatMapper, NameMapper/CommandMapper 在 Config) — 数据流: `结构方法 → CommandAsyncExecutor (evalWrite/readAsync) → RedisExecutor (928 行: 重试 4 次 jitter 1-2s/timeout 3000ms/连接选择) → RedisClient/RedisConnection (RESP2/3, Netty) → Redis 服务端 → addListener 异步回调` — 旁路: PubSub 订阅通道 (LockPubSub/ElementsSubscribeService) + DNSMonitor 故障切换 (changeMaster) + Watchdog 续期调度 (leaseTime/3) + lazyInitialization 默认立即连接。

## 二、入口展开追踪 (00 §2)

| 候选 | 源码位置 | 设计决策测试 | 结论 |
|---|---|---|---|
| `Redisson.create/ServiceManager` | Redisson.java:77,119; ServiceManager.java (804) | 初始化链路+中央工厂 — 定义特征 | **RD-1** |
| `ConnectionManager.create + 5 实现` | ConnectionManager.java:89; connection/ 39 文件 | 连接池/主从/故障切换 — 定义特征 | **RD-1** |
| `RedissonLock/RLock` | RedissonLock.java (600) + renewal/ (8) | 分布式锁+Watchdog — 定义特征 (最常用) | **RD-2** |
| `Codec 接口+实现` | codec/ (36) | 数据存取编解码 — 编码层定义特征 | **RD-3** |
| `CommandAsyncExecutor/Service` | command/ (12) | 异步执行/重试/Lua — 定义特征 | **RD-4** |
| `RedissonMap` | RedissonMap.java (1967) | 分布式映射+Loader/Writer — 高频结构 | **RD-5** |
| `RedissonLocalCachedMap + cache/` | RedissonLocalCachedMap.java (1474) + cache/ (27) | 本地缓存失效机制 (RLCSC) — **定义特征, 重审新增** | **RD-6** |
| `RedissonSpringCacheManager/ConnectionFactory` | redisson-spring-cache/redisson-spring-data | Spring 集成 — 生态面 | **RD-7** |
| `RedissonBucket/AtomicLong/Semaphore` | redisson root 实现 | 基础命令封装 — 薄 | **RD-8** |

## 三、域清单 (9 域: 1 导论 + 5🔴 + 3🟡, 09 重审修正 v2 + v2.1 补 RD-0)

| # | 域 | 文件 (行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| RD-0 | **导论: Redisson 是什么 + Spring Boot 集成地图** | README+CHANGELOG+api/ 门面 (无机制) | **Redisson 全景**: 与普通客户端 (Lettuce/Jedis) 的本质区别 (数据结构本地化封装)/8 种部署模式声称/RESP2-3+Valkey/生态全景 (数据+服务)/演进史 ()/ **Spring Boot 四集成模块地图** (starter/data/cache/transaction 各自取代 Boot 的哪个默认件) | 🔴 A* (非机制域, 特殊处理: 不拆不 harness, 1 篇导读) |
| RD-3 | **Codec 序列化** | codec/ (36) | Codec 接口/**默认 Kryo5**/现行实现清单 (Kryo5/JsonJackson/Jackson3/Serialization/SnappyV2/LZ4V2/ZStd/Protobuf/Composite + Jackson3 族)/CompositeCodec | 🔴 A |
| RD-1 | **主类+连接管理** | Redisson (1532)+Config (1351)+ServiceManager (804)+connection/ (39) | Config 5 模式→ConnectionManager.create 5 实现/**Redisson 构造器初始化链 (EvictionScheduler+WriteBehindService+register LockRenewalScheduler)**/ServiceManager 中央工厂 (HashedWheelTimer/IdleConnectionWatcher/NatMapper 等)/**RESP2/3+Valkey 兼容**/DNSMonitor 故障切换/ElementsSubscribeService 订阅/全局配置 (**lazyInitialization 默认 false**) | 🔴 A |
| RD-4 | **命令执行流水线** | CommandAsyncExecutor+CommandAsyncService (1256)+**RedisExecutor (928)**+client/ (217) | 异步模型 RFuture/读写分离/**RedisExecutor 单命令核心 (attempts=4+DelayStrategy jitter 1-2s+timeout 3000ms)**/CommandBatchService 批量 (BatchOptions.responseTimeout)/eval 脚本 (useScriptCache SHA)/RESP 解析 | 🔴 A |
| RD-2 | **RLock+Watchdog** | RedissonLock (600)+RedissonBaseLock (290)+renewal/ (8)+LockPubSub | tryLock 四步 (Lua 可重入→订阅→Semaphore 等待→续期)/Watchdog (leaseTime/3=10s 批量 AsyncChunkProcessor)/三续期器/**9 锁族 (Fair/Fenced/Spin/NonReentrant/NonReentrantFair/Multi/Red/Faster)** | 🔴 A |
| RD-5 | **RMap 分布式映射** | RedissonMap (1967)+MapLoader/Writer+EvictionScheduler | Map 命令面/MapWriter 写后/MapLoader 读通/RMapCache 单 entry TTL/EvictionScheduler 清理 | 🟡 B |
| RD-6 | **RLocalCachedMap 本地缓存** (新增) | RedissonLocalCachedMap (1474)+cache/ (27) | NearCache 本地读/**SyncStrategy NONE/INVALIDATE/UPDATE (api/LocalCachedMapOptions.java:64-79; "RLCSC"=外部面试术语 源码无此名)** + ReconnectionStrategy + evictionPolicy 消息链/cache 包 27 类/LocalCacheView | 🔴 A |
| RD-7 | **Spring Boot 集成矩阵** (v2 升级) | redisson-spring/ (4 子模块) + starter (5 文件) | **四模块取代关系**: starter 用 RedissonConnectionFactory 替代 Lettuce/Jedis (**`@AutoConfiguration(before=DataRedisAutoConfiguration)`**, V4:31-33)/spring-data 适配 RedisTemplate/spring-cache 替代 CacheAutoConfiguration 的 CacheManager (B-12 对照)/spring-transaction (RedissonTransactionManager, B-16 tx 对照) | 🟡 B (升级考虑: 用户明确重点) |
| RD-8 | **基础数据结构** | RedissonBucket/AtomicLong/Semaphore/CountDownLatch/BitSet | 基本命令 Java 封装模式/RedissonClient 门面 | 🟡 B |

## 四、已排除 (00 §3 — 防"存在=域")

| 类/包 | 文件数 | 原因 |
|---|---|---|
| api/ | 801 | 全部数据结构接口门面 — 理解架构后按需查阅, 非机制 |
| client/ 协议细节 | 217 | RESP 编解码底层 — 并入 RD-4 讲调用面 |
| reactive/ + rx/ | 34+32 | 响应式门面 (RReactive/Rx) — 独立话题, RedissonClient.reactive() 一句话带过 |
| transaction/ | 58 | 客户端 MULTI/EXEC 包装 — Spring 面试低频; 服务端事务见 Redis R-16 对照 |
| liveobject/ | 25 | 对象映射 (ORM for Redis) — 生产不常用 |
| executor/ | 22 | 分布式执行器 — 面试低频 |
| remote/ | 17 | RPC 服务调用 — 面试低频 |
| jcache/ | 13 | JSR107 标准适配 |
| mapreduce/ | 12 | 分布式 MapReduce — 业务不用 |
| redisnode/ iterator/ eviction/ | 7+7+8 | eviction 并入 RD-5; redisnode/iterator 薄工具 |
| cluster/ | 3 | Cluster 拓扑并入 RD-1 |
| redisson-hibernate/tomcat/mybatis/helidon/micronaut/quarkus | — | 框架特定集成 — RD-7 一句话 |

## 五、知识网络图 (Obsidian 双链 — REVIEW3: 全部改为真实存在域, 双向引用, 含承接/复用/对照/引出)

> **REVIEW3 修正 (2026-08-13)**: v2.1 原知识网络违反 06-跨域引用 §2"不得引用未分析域" (R-26/R-11 等未产出) + 编号错位 (B-10/B-12 非实际产出目录) + 缺反向承接 (s75 等阶段2 → 阶段3 的待承接点) + 漏引已存在域 (HikariCP/MyBatis)。以下为**核验 redis/spring/other outlines 实际目录后**的真实双向引用表。

### 双向引用总表 (源头域 / 本域 / 反向)

| 源头域 (已存在, 产出目录) | RD 侧 | 关系 (06 §2.5 复用≠省略 — 内核引用 + 本层视角展开) |
|---|---|---|
| `r1-object` (redis 编码/共享对象) | RD-3 Codec | **对照**: 服务端 robj 编码 (16B+共享池) vs 客户端 Codec (序列化器族) — 内核对照, 各自展开 |
| `r2-events` (redis 事件循环) | RD-4 | **对照**: ae epoll 单线程 vs Netty EventLoopGroup |
| `r3-dict`/`r4-sds`/`r5-quicklist`/`r6-zset`/`r7-intset`/`r19-listpack` | RD-3 | **背景**: 服务端数据结构编码 — 客户端只管命令, 不感知编码 (反向: 这些编码决定服务端内存, 客户段 Codec 只做网络字节流) |
| `r20-server` (服务端骨架) | RD-1 | **对照**: 服务端 serverCron/命令表 vs 客户端 Redisson 构造器/服务工厂 |
| `r21-db` (键空间) | RD-5 RMap | **对照**: db.c 键空间 dict 遍历 vs RMap 客户端视图 — 同一 redisDb 的 Java 镜像 |
| `r22-expire` | RD-5/6 | **对照**: 服务端 expire 惰性+主动 vs 客户端 TTL 语义 (RMapCache/RExpirable) |
| `r23-evict` (maxmemory 淘汰) | RD-6 | **对照**: 服务端 LRU/LFU 淘汰 vs 客户端本地缓存 evictionPolicy |
| `r28-networking` (RESP 协议) | RD-4/RD-1 | **核心复用**: RESP2/3 协议解析 — 服务端收发面在 r28, 客户端命令面在 RD-4; **反向待承接**: r28 大纲如写"命令发起在客户端"则本域回引 |
| `r33-zmalloc` | RD-1 | **对照**: 服务端内存分配统计 (used_memory) vs 客户端连接池内存面 (薄) |
| `s75-boot-redis` (Redis 自动装配) | RD-7/RD-1 | **核心承接** (反向链接最重要): s75 大纲明写"连接/协议深入在阶段3" — **RD-7 必须回引**: "s75 讲了 Boot 怎么装配 RedisTemplate (Lettuce/Jedis 条件选择); RD-7 展开 Redisson 怎么替换连接工厂 (before=DataRedisAutoConfiguration)" — 这是真正的双向链 |
| `s77-boot-cache` (CacheAutoConfiguration) | RD-7 | **承接**: s77 讲 CacheType 选择 CacheManager; RD-7 展开 RedissonSpringCacheManager 作为 REDIS CacheManager 替代 — "s77 装配的 CacheManager 可以是 Redisson 的" |
| `s19-cacheable` (Framework @Cacheable 原理) | RD-7 | **复用**: @Cacheable 的 execute() 模板方法 (s19) — RD-7 的 RedissonCache 实现其 Cache 接口; 引用 s19 §拦截器 |
| `s29-tx-chain` ~ `s33-tx-sync` (Spring 事务链) | RD-7 | **对照**: PlatformTransactionManager 抽象 (s29) — RedissonTransactionManager 是其实现; 内核引用 s29 |
| `s74-boot-datasource` | RD-7 | **对照**: 数据源自动装配 (HikariCP 替换) vs Redis starter (Redisson 替换) — 同构: "Boot 连接工厂默认件可被第三方替换" 模式 |
| `s53-jdbc-datasource` | RD-1 | **对照**: JDBC 连接抽象 vs RedisClient 连接抽象 |
| `h13-datasource` (HikariCP) | RD-1 | **对照**: Hikari 池生命周期 vs Redisson 连接管理器生命周期 |
| `h02-concurrentbag` (Hikari 无锁容器) | RD-1 | **对照**: Hikari ConcurrentBag (无锁+线程亲和) vs ConnectionsHolder (双队列+AsyncSemaphore) — 同类问题两种解法, 面试对比点 |
| `m7-cache` (MyBatis 缓存装饰链) | RD-6 | **对照**: MyBatis LruCache/BlockingCache 装饰链 vs RLocalCachedMap 失效策略 — 本地缓存一致性两个方向 (m7 单一 JVM / RD-6 多 JVM) |

### 反向引用承接点 (源头域 → 本域的"待承接"声明)

| 源头域声明 | 本域承接 (RD 大纲必须回应) |
|---|---|
| `s75-boot-redis`: "连接/协议深入在阶段3" | **RD-1 篇1/RD-7**: 承接 — "阶段2 s75 说连接深入在阶段3; 阶段3.5 Redis 讲了服务端 (r28); 本域 (Redisson) 讲客户端连接管理" |
| `s77-boot-cache`: CacheType 多实现 | **RD-7**: 承接 — CacheManager 的可插拔性在 Redisson 得到第三种实现 |
| `m7-cache`: "对照: R-1-redis (本地 vs 分布式)" | **RD-6**: 承接 — m7 引用的 redis 本地/分布式对照, 在 Redisson RLocalCachedMap 得到完整展开 |

### 本域内部双链 (RD 内)

**RD-0** (导论, 已配) → 引出全部 | **RD-3** (codec) → 前置给 RD-1/4/5 (消费) | **RD-1** (连接) → 前置给 RD-2/4/5/6/7 (Hub) | **RD-4** (命令) → 前置给 RD-2/5/6/8 | **RD-2** (锁) → 前置给 RD-7 (Spring 可注 RLock) | **RD-5** (RMap) → 前置给 RD-6/7 | **RD-6** (本地缓存) → 依赖 RD-5+RD-1 订阅 | **RD-7** (Spring) → 消费全部 | **RD-8** (基础结构) → 消费 RD-4

📌 每篇大纲 header 必须含 (06 §6):
```
前置: [[r28-networking]] ...
复用: [[s19-cacheable]] [[m7-cache]] ...
对照: [[s75-boot-redis]] [[h02-concurrentbag]] ...
引出: [[RD-2-rlock]] ...
```
> ⚠️ 引用规则: **只引已存在目录** (上表左列); 未完成域 (Redis R-14/15/16 等) 不得引用 — 06 §2"不该做"。若并行 AI 后续产出新域, 再补链 (06 §3 新发现回流)。

## 六、执行顺序 (拓扑: 叶子先, 09 重排 v2 + v2.1 补 RD-0)

**RD-0 → RD-3 → RD-1 → RD-4 → RD-2 → RD-5 → RD-6 → RD-7 → RD-8**

> 拓扑理由: **RD-0 导读最先** (全景+与 Spring Boot 关系地图 — 读者认知起点, 非机制依赖); Codec 是编码层叶子 (命令层/结构层全消费) — 排第 1 (同 Redis 教训: 编码层先行, 原计划第 4 位反拓扑); RD-1 连接中枢 (ServiceManager 创建 CommandAsyncExecutor + 订阅器 + 续期器 — RD-2/4 依赖其产物); RD-4 命令流水线; RD-2 锁 (消费命令+Lua+订阅); RD-5 RMap (消费命令); RD-6 本地缓存 (依赖 RMap 载体 + 订阅失效通道); RD-7 Spring 集成 (依赖 RMap + 全部机制消费者, Boot B-10/B-12 已讲完可对照); RD-8 基础结构薄消费殿后。
> **与原计划差异**: RD-0 新增最前; RD-3 4→1 位 (编码层先行); RD-6 新增插入 RD-5 后; RD-7 从"Cache+Data"升级为"Spring Boot 集成矩阵 (四模块取代关系)"; 原 R-7 基础结构末位保留。

## 七、深度分类复核 (00 §3.5)

- **RD-0 导论 + 5🔴 / 3🟡** (v2.1 修正, 原 4🔴/3🟡 → 补导论)
- 🔴 = 定义特征 (编码层/连接层/命令层/锁/本地缓存)
- 🟡 = 支撑与集成 (RMap/Spring 集成/基础结构)
- RD-0 = 导读域 (非机制 — 特殊处理: 1 篇不拆, 无 harness, 无闭环; 定位读者认知起点)
- 巨型域: RD-1 (Redisson+Config+ServiceManager+connection 39 文件) — 按"大域拆 2-5 篇"标准拆: 01-create-初始化 / 02-connection-连接池 / 03-pubsub-dns

## 八、与原始执行计划的差异汇总 (v2)

| 原始 | 重审后 | 理由 |
|:--:|:--:|---|
| 7 域 | **8 域** (+1) | cache/ 27 文件本地缓存失效机制 (RLocalCachedMap 1474 行) — 定义特征级, 原仅 R-5 一句话 |
| 默认 Codec JsonJackson | **Kryo5** | Config.java:165 copy ctor 实证 |
| "7 种 Codec 含 FST/Marshalling" | **现行清单** (FST/Marshalling 已删, +ZStd/Protobuf/Composite/Jackson3 族) | codec/ 36 文件 ls 实证 |
| RD-3 第 4 位 | **第 1 位** | 编码层叶子依赖 — 拓扑修正 |
| R-1/R-4 边界重叠 | 重画 | R-1=连接面, R-4=命令面 |
| 锁族仅 RedLock | 补 4.x 扩展 (Fenced/Spin/Faster/NonReentrant) | 顶层扫描实证 |
| 重试无数字 | retryAttempts=4 + EqualJitter 1-2s | BaseConfig:62,67 |
| 4.x 新特性 (RESP2/3+Valkey) | 并入 RD-1 | Config:113-115 |
| (REVIEW2) "RLCSC" 术语 | SyncStrategy NONE/INVALIDATE/UPDATE; RLCSC=外部术语 | api/LocalCachedMapOptions.java:64-79 |
| (REVIEW2) responseTimeout | timeout=3000ms (4.x) | BaseConfig:58 |
| (REVIEW2) ServiceManager 组件 | NameMapper/CommandMapper 在 Config; +HashedWheelTimer/IdleConnectionWatcher/NatMapper/register 注入 | ServiceManager.java:122-156,766 |
| (REVIEW2) RD-4 漏 RedisExecutor | 补 928 行单命令核心 | RedisExecutor.java:78-91 |
| (REVIEW2) 初始化链/lazy 默认 | EvictionScheduler+WriteBehindService+register; lazyInitialization=false | Redisson.java:66-86; Config:111 |
| (REVIEW2) RD-7 漏 spring-transaction | 补 RedissonTransactionManager | redisson-spring-transaction/ (6 文件) |
| (v2.1) 无导论/无 Spring 关系 | **新增 RD-0 导论** (Redisson 是什么+ 与普通客户端区别 + 演进史) + **RD-7 升级为 Spring Boot 集成矩阵** (四模块取代关系: starter 用 RedissonConnectionFactory 替代 Lettuce, `@AutoConfiguration(before=DataRedisAutoConfiguration)` V4:31-33) | 用户明确指令; B-10/B-12 交叉实证 |
| (REVIEW3) 引用未分析域 | 删 R-26/R-11 等; 只引真实存在目录 (核验 redis/outlines 现有 r1-r7/r19-r23/r28/r33) | 06 §2"不得引用未分析域" |
| (REVIEW3) 编号错位 B-10/B-12 | 改引 **s75-boot-redis / s77-boot-cache / s19-cacheable** (实际产出目录) | spring/outlines 核验 |
| (REVIEW3) 缺反向承接 | 建"待承接点"表: s75 "连接/协议深入在阶段3" → RD-1/RD-7 回引; m7 引 R-1-redis → RD-6 承接 | 双向引用完整性 |
| (REVIEW3) 漏引已存在域 | 补 HikariCP h02/h13 (连接池对照) + MyBatis m7-cache (缓存对照) | 阶段3 已完成域核验 |
| (REVIEW3) 双链格式缺失 | 每篇 header 强制 前置/复用/对照/引出 四行 (06 §6) | m7-cache header 为规范范例 |

**覆盖率报告 (00 §第九步)**: 方法论域发现 9 域 (1 导论 + 8 机制) = 执行计划 7 域 + 重审新增 1 域 + 导论 1. 差距分析: 客户端仓库 800+ API 门面屏蔽机制, 真实机制域 (连接/命令/锁/编码/缓存) 已全覆盖; 域清单错误率 12.5% (Redis 83% 对比 — 客户端仓库薄面广, 域发现更易收敛)。

**知识网络覆盖率 (06 §6)**: 双向引用表 17 条目 = Redis 9 域 + Spring 6 域 + 其他 2 域 (HikariCP/MyBatis), 全部对照真实产出目录建链; 反向承接点 3 处 (s75/s77/m7) 要求 RD 大纲显式回应; 未完成域 (Redis R-14/15/16/R-30+ 等) 明确标注禁引。

## 九、完成检查单 (00 §8)

- [x] 顶层包扫描 (22 包 2804 文件) ↔ 域清单覆盖矩阵
- [x] 全部数字断言穷举验证 (10 类 40+ 项, LockTask 112 修正)
- [x] 依赖方向方法适配记录 (Java 有模块 import — 用 import 统计 + 调用方向)
- [x] 拓扑重排完成 (RD-3 编码层先行), 与规划差异逐条记录理由
- [x] 新增域 (RD-6) 全过 00 §3 + §3.5 测试
- [x] 怀疑审计表已写入 (§〇)
- [x] **二次 REVIEW (深审) 完成: 3 修正 (RLCSC 术语/responseTimeout/timeout=3000/ServiceManager 错位) + 5 补强 (RedisExecutor/初始化链/lazy 默认/锁族 9 类/spring-transaction) — 全部实证后写入 §〇/§一/§三/§八**
- [x] **三次 REVIEW (跨域引用专项) 完成: 引用未分析域删除/编号错位修正/反向承接表建全/漏引已存在域补齐/双链四行格式标准化 — §五重写为双向引用表 + §八记录 6 项**
- [x] 偏差待同步 HANDOFF-STAGE3 + HANDOVER.md
