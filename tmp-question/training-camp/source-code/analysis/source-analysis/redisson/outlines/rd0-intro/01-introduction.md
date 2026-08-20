# RD-0 Redisson 导论 — 是什么 & 为什么 & 与 Spring Boot 的关系 (大纲)

> 前置: [[r28-networking]] [[r21-db]] (并行 Redis 服务端) | 复用: [[s75-boot-redis]] [[s77-boot-cache]] [[r1-object]] | 对照: [[s19-cacheable]] [[h13-datasource]] [[m7-cache]] | 引出: [[rd1-connection]] [[rd2-rlock]] [[rd3-codec]] [[rd7-spring]]
> 🔴 A* 导读域 (非机制: 不拆不 harness 无闭环) | 1 篇 | 来源: README + CHANGELOG + api/ 全景 + redisson-spring 模块
> 定位: 全书 Redisson 卷的开场 — 回答"Redisson 是什么" + "在 Spring Boot 项目里扮演什么" — 为后续 8 个机制域搭认知坐标系

**读者处境**: 你已经用过 spring-boot-starter-data-redis 连过 Redis；你在微服务里写过 `RedisTemplate`、`@Cacheable`、`redisTemplate.setIfAbsent` 抢过锁，但总有种"手动拼命令"的笨拙感。面试官问"讲讲你怎么用 Redis 做分布式锁"，你说"setnx + 过期时间"。这篇就是来颠覆这个答案的：有一个库把这些都做成了"本地数据结构一样"的 API，你 new 完就能用。

### 1. Redisson 是什么 — 一句话定位

场景: 一个 HTTP 响应里, "用户积分 +5", 你希望它在 Redis 里是原子的、掉线自动释放、发给谁都能抢到。
源码路径 (全景面, 非单文件):
- README 首行: "Redisson: Valkey & Redis Java Client **and Real-Time Data Platform**" — 客户端 + 实时数据平台双身份
- 定位三角: (a) **Redis/Valkey 客户端** (对比 Lettuce/Jedis) (b) **数据结构本地化**: `RLock` 用起来像 `java.util.concurrent.locks.Lock`, `RMap` 像 `Map`, 语义移植到分布式 (c) **服务/扩展**: 分布式信号量/队列/执行器/MapReduce/可靠发布订阅
- 与服务端的边界: 服务端只认命令 (阶段3.5 Redis [[r28-networking]] RESP), Redisson 把"一组命令的正确组合"封装成本地 API — 这就是它与"裸客户端"的本质分水岭
关键设计: **API 镜像语义** — 面试记忆点 "在 Java 里写分布式数据结构, 和使用本地集合的体验一致性"。数据流: `你的代码 → Redisson 本地对象 → Lua/命令批 → Redis 服务端`。

### 2. 和"传统客户端"的区别 — 为什么需要它

场景: 用 RedisTemplate 实现分布式锁需要几步? 原子性谁来保证?
源码路径:
- 对比点 1 (命令级 vs 对象级): Lettuce/Jedis 暴露 `set()/get()/eval()` 命令; Redisson 暴露 `RMap.put(k,v)` 对象 API — 内部是 `HSET`/`EVAL` (RD-4 展开)
- 对比点 2 (连接): Lettuce 单连接/连接池; Redisson Netty 连接池 + 读写分离 + 故障切换 (RD-1 展开, 本导读一句话点到)
- 对比点 3 (**原子操作内建**): `RLock.tryLock` = Lua 可重入+看门狗续期 (RD-2); `RAtomicLong` = INCRBY 封装 — 无需自己拼 MULTI/EVAL
- 对比点 4 (本地缓存): `RLocalCachedMap` 本地 Caffeine/内存 + Redis 亡者一致性 — 面试高频 (RD-6)
关键设计: **"把不可靠的分布式操作, 收敛成可靠的本地体验"** — 可靠性 (重试/续期/失效) 封装在对象内部, 业务代码只关心语义。

### 3. 生态全景 — 800+ 接口的组织方式

场景: 文档里 800 多个接口, 从哪看起?
源码路径:
- api/ 801 文件 = 四套镜像: **Xxx (同步) + XxxAsync (CompletableFuture) + XxxReactive (Reactor) + XxxRx (RxJava3)** — 同步 149 个接口, 家族 ×4
- 家族清单 (同步面示例): 集合 (RMap/RSortedMap/RSet/RList/RQueue/RDeque/RBlockingQueue/RMultimap)、对象 (RBucket/RBitSet/RAtomicLong/RAtomicDouble/RBloomFilter/RScoredSortedSet)、服务 (RLock/RCountDownLatch/RSemaphore/RRateLimiter/RTopic/RStream/RTimer)、搜索 (RSearch 红斧) — api/ 目录实测
- 门面入口: `RedissonClient` (20+ getXxx 方法) 返回各家族对象
- 四模镜像的动机: 同一数据结构服务同步/响应式两条技术栈 (reactive/rx 包已排除, 此处只讲存在性)
关键设计: **接口分层 = 家族 × 编程模型** — 学习路径: 同步面打底 (RD-3~8), 异步面随 RD-4 扫一眼。

### 4. 部署拓扑 — 5+ 模式与 2 个新词 (Valkey/Proxy)

场景: 连接串怎么写? 生产高可用用哪种?
源码路径:
- 5 种官方配置模式: Single/MasterSlave/Sentinel/Cluster/Replicated (config/ 12 类实测; ConnectionManager.create 5 分支 RD-1 展开)
- 两个 4.x 新词: **Valkey 兼容** (Redis 社区 fork, 7.2.5+, Config:115 ValkeyCapability) / **Proxy/Multi-Cluster/Multi-Sentinel** (README 声称 8 模式, 需 Pro 版 — 开源代码仅 5 分支, README 超前, 大纲标注此差异)
- 配置模型: Config 空构造 (默认值族) + copy 构造 (Immutability 语义, RD-1 展开)
关键设计: **单配置模型选择拓扑** — 换模式 = 改一段 config, 代码零改动。

### 5. 版本演进 — 4.0.0 是分水岭

场景: 你用的 3.x 还是 4.x? 4.x 多了什么?
源码路径 (CHANGELOG 时空):
- 版本线: 3.37.0 (2024-10) → … → 3.52.0 (2025-09) → **4.0.0 (2025-12-16)** → 4.6.1 (2026-06)
- 4.0.0 断裂点: 全功能 **Reliable Pub/Sub** (ack/重放/死信/分组) / **Spring Boot 4.0 + Spring Data Redis 4.0** 集成 / Jackson 变可选 / SnakeYAML 配置 / NameMapper/NatMapper 迁 config 包 / 砍 JSON 旧配置 & Spring XML
- 4.x API 面: `e.expire(Duration, names)` 批量过期 / `getClusteredMapCacheNative` 原生结构族 (Native = 直接 Redis 原生命令, 非 Lua) — 双实现路径 (Lua vs Native) 是 4.x 重要演进
- 4.6.x 可靠性史: 连接池 AsyncSemaphore 竞态修复 / 锁续期锁家族扩展 (Fenced/Spin)
关键设计: **4.x = 云原生 + 双引擎 (Lua/Native) + Spring 4.0 对齐**; 大纲标注"3.x vs 4.x 迁移关注点"。

### 6. 与 Spring Boot 的关系 — 集成四模块取代图 (用户重点)

场景: 你的 Spring Boot 项目里 `redisTemplate` 是谁实现的? 换成 Redisson 要改几行?
源码路径 (redisson-spring/ 四模块实证):
- **模块 1 — starter (自动装配)**: `RedissonAutoConfigurationV4` (`@AutoConfiguration(before=DataRedisAutoConfiguration)` + `@ConditionalOnClass(Redisson+RedisOperations+DataRedisAutoConfiguration)`, V4:31-33) — **用 `RedissonConnectionFactory` 替换 Boot 默认的 Lettuce/Jedis** → 你的 `RedisTemplate`/`StringRedisTemplate` 无感切换到 Redisson 连接; `RedissonProperties` 映射 `spring.redis.*`
- **模块 2 — spring-data (连接适配)**: `RedissonConnectionFactory` (redisson-spring-data/redisson-spring-data-16 等) 实现 `RedisConnectionFactory` — 让 Spring Data Redis 数据访问层 (RedisTemplate) 完整跑在 Redisson 上
- **模块 3 — spring-cache (缓存管理)**: `RedissonSpringCacheManager` 实现 Spring `CacheManager` — 替代 Boot `CacheAutoConfiguration` 按 CacheType (SIMPLE/CAFFEINE/REDIS) 装配的默认管理器 (对照 [[s77-boot-cache]] 的 CacheType 选择); `RedissonCache` + `CacheConfig` (TTL/maxIdleTime)
- **模块 4 — spring-transaction (事务)**: `RedissonTransactionManager` 实现 Spring `PlatformTransactionManager` — 让 `@Transactional` 管理 Redis 事务 (对照 s29-tx 链)
- **取代关系一句话**: "Redisson 不是要替代 Spring Data Redis, 而是替换它底下的**连接工厂/缓存管理器/事务管理器** — 让 Spring 生态『用 Redis 的方式』无缝换成『用 Redisson』"
关键设计: **集成 = 适配器模式 (每个模块实现对应 Spring SPI)** — 改依赖 + 配置, 业务零重构; Boot 4.0 (V4) 用 `@AutoConfiguration(before=…)` 保证覆盖默认。数据流: `@Cacheable → CacheManager(Redisson) → RMap(HSET/EVAL) → RedissonConnectionFactory → Netty → Redis`。

### 负面空间 — Redisson 刻意不做的事

- **不替代 Redis 服务端**: 它不解决服务端主从一致性, 只讲客户端怎么连/怎么用 (生产一致性在 R-9/R-14/R-15)
- **不做进程内缓存兜底**: RLocalCachedMap 本地缓存是"尽力一致" (RD-6 展开弱一致边界), 不是强一致缓存
- **不强行对齐 JDK 语义**: RBlockingQueue 无界 Redis list 等价, 不搞容量限制; 分布式数据结构有网络固有语义差异
- **不保证 MAG 任意性**: 部分高级型只能 5 种模式中的子集 (如 RClustered 系列仅 Cluster)

→ 引出: 机制域地图 — 「用什么 → 看 RD-3~8; 怎么连 → RD-1; 怎么执行 → RD-4; 锁 → RD-2; 缓存 → RD-6; 塞给 Spring → RD-7」
→ 双链: 前置 [[r1-object]] [[r28-networking]] [[r21-db]] | 复用 [[s75-boot-redis]] [[s77-boot-cache]] | 对照 [[s19-cacheable]] [[h13-datasource]] [[m7-cache]] | 引出 [[rd1-connection]] [[rd2-rlock]] [[rd7-spring]]