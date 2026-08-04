# Redisson 源码学习范围规划

> **版本**: main 分支（latest）
> **仓库**: `/data/workspace/source-code/code/spring/redisson/`
> **规模**: core 1570 个源文件，12 个子模块，总计 2804 文件
> **日期**: 2026-08-03

---

## 一、仓库概况

Redisson 是 Java 生态中功能最全面的 Redis 客户端，提供分布式锁（RLock）、分布式集合（RMap/RQueue/RSet）、分布式信号量/闭锁、订阅发布、分布式执行器、MapReduce 等高级分布式数据结构。**核心设计**：通过 Netty 异步连接池 + Redis 协议命令执行器，将每个 Redis 数据类型封装为 Java 对象，提供本地化的编程体验。

800+ 个 API 接口定义了所有 Redis 操作，127 个根级实现类提供具体的数据结构实现。

**子模块清单**（12 个，全部审计）：

| 子模块 | 文件数 | 职责 | 状态 |
|---|---|---|---|
| `redisson/` | 1570 | 核心：Client 连接(217) + API 接口(801) + 实现(127) + Codec(36) + Connection(39) + Command(12) | ✅ 已探索 |
| `redisson-all/` | — | 聚合打包模块 | 淘汰 |
| `redisson-spring/` | 若干 | Spring Cache + Spring Data Redis 集成 | 🟡 |
| `redisson-helidon/` `redisson-micronaut/` `redisson-quarkus/` | — | 其他框架集成 | 淘汰 |
| `redisson-hibernate/` `redisson-mybatis/` `redisson-tomcat/` | — | ORM/容器集成 | 淘汰 |
| `docs/` | — | 文档 | 淘汰 |

**core 包结构**：

| 包 | 文件数 | 职责 | 关键类 |
|---|---|---|---|
| `api/` | 801 | 全部 Redis 数据结构接口 | RLock/RMap/RQueue/RSet/RBucket/RSemaphore/RCountDownLatch 等 |
| `redisson/` root | 127 | 接口实现 + 主类 | Redisson(1532行), RedissonLock(600行), RedissonMap(1967行), RedissonBucket |
| `client/` | 217 | Redis 协议客户端（RESP2/3）| RedisClient, RedisConnection, 各种命令编解码 |
| `connection/` | 39 | 连接池管理 | ConnectionManager, MasterSlaveConnectionManager, ClientConnectionsEntry |
| `command/` | 12 | 命令异步执行器 | CommandAsyncExecutor, CommandAsyncService |
| `codec/` | 36 | 序列化编解码 | JsonJacksonCodec, Kryo5Codec, FSTCodec, MarshallingCodec, SnappyCodecV2 |
| `renewal/` | 8 | 锁续期（Watchdog）| LockRenewalScheduler, RedissonBaseLock.scheduleExpirationRenewal |
| `eviction/` | 若干 | Map 过期策略 | EvictionScheduler, JCache eviction |

---

## 二、知识域规划

### 🔴 核心域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| R-1 | **Redisson 主类与连接管理** | Redisson(1532行), Config(1351行), ServiceManager(804行), CommandAsyncService(1256行), ElementsSubscribeService(126行), DNSMonitor(285行) | **初始化**：`Config`（5 种服务器模式 + 全局配置）→ `ConnectionManager.create()` 选择对应实现（5 种）→ `ServiceManager` 中央工厂（EventLoopGroup/Timer/LockRenewalScheduler/ElementsSubscribeService/QueueTransferService/ConnectionEventsHub/IdleConnectionWatcher/NameMapper/CommandMapper/NatMapper）→ `CommandAsyncExecutor`；**命令执行**：`CommandAsyncService` 读写分离——`readAsync()` 路由到 slave（支持 `executeAllAsync` 全部 slave 执行）、`writeAsync()` 路由到 master——带 `retryAttempts`/`retryDelay`/`responseTimeout` 重试；**PubSub 通信**：`ElementsSubscribeService` 管理跨实例订阅——RLock 释放通知（`redisson_lock__channel`）、RMap/RQueue 数据变更通知——自动重连订阅恢复；**DNS 监控**：`DNSMonitor` 定期解析 master+slave DNS→变更时 `changeMaster`/`changeSlave` 切换；**全局配置**：`lockWatchdogTimeout`(30s)、`lockWatchdogBatchSize`(100)、`fairLockWaitTimeout`(5min)、`checkLockSyncedSlaves`(检查锁同步)、`keepPubSubOrder`(保序)、`useScriptCache`(SHA缓存Lua)、`NettyHook`(Netty扩展钩子)
| R-2 | **RLock 分布式锁 + 批量 Watchdog** | RedissonLock(600行), RedissonBaseLock(290行), LockRenewalScheduler, LockTask(113行) | **加锁流程**：`tryLock()` → Lua 脚本原子操作（`SET key NX PX ttl` / `HINCRBY` 可重入计数 + `PEXPIRE`）→ 订阅 `redisson_lock__channel` 释放通知 → `Semaphore` 阻塞等待；**批量 Watchdog**：`scheduleExpirationRenewal(threadId)` → `LockRenewalScheduler.renewLock()` → `LockTask` 使用 `AsyncChunkProcessor` **批量续期**多把锁 → Lua 脚本 `HEXISTS` 检查锁存在 + `PEXPIRE` 续期（`lockWatchdogTimeout=30s` / `lockWatchdogBatchSize` 控制批量大小）→ 锁不存在时自动从续期列表移除；**三种续期器**：`LockTask`（普通锁）、`ReadLockTask`（读写锁）、`FastMultilockTask`（红锁/联锁）；**红锁（RedLock）**：多个独立 Redis 实例上获取锁，多数实例成功才算加锁成功——应对 Redis 节点故障的脑裂问题 |
| R-3 | **Codec 序列化体系** | Codec 接口, JsonJacksonCodec(默认), Kryo5Codec, FSTCodec, MarshallingCodec, SnappyCodecV2 | **核心抽象**：`Codec` 接口定义 `Encoder`（序列化）和 `Decoder`（反序列化）→ 所有数据结构通过 Codec 存取数据；7 种内置实现：`JsonJacksonCodec`（默认，Jackson JSON）、`Kryo5Codec`（高性能二进制）、`FSTCodec`（Java 序列化替代）、`MarshallingCodec`（JBoss Marshalling）、`SnappyCodecV2`（压缩编码）、`LZ4CodecV2`（LZ4 压缩）、`SerializationCodec`（JDK 自带）；**注册方式**：`config.setCodec(new JsonJacksonCodec())` 全局设置，或 `getMap("key", codec)` 单个结构指定 |
| R-4 | **命令执行流水线** | CommandAsyncExecutor, CommandAsyncService, RedisClient, RedisConnection | **异步执行模型**：`CommandAsyncExecutor` 通过 Netty 连接池向 Redis 发送 RESP 协议命令 → `RFuture<V>` 返回异步结果（`CompletionStage` 兼容）→ 同步变体通过 `get()`/`await()` 阻塞等待；**Lua 脚本执行**：`evalWriteAsync()`/`evalReadAsync()` → Lua 脚本字符串 + KEYS[] + ARGV[] 参数 → Netty writeAndFlush → 解析 RESP 响应；**命令重试**：`CommandAsyncService` 包含连接断开重试、主从切换重试逻辑 |

### 🟡 扩展域（3 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| R-5 | **RMap 分布式映射** | RedissonMap(1967行), RMap 接口, MapWriter/MapLoader | **本地缓存**：`LocalCachedMapOptions` 支持 `NearCache` 模式——读取时缓存到本地 `ConcurrentHashMap`，写操作 invalidate 本地缓存；**写后/读通**：`MapWriter`/`MapLoader` 接口支持同步/异步回写数据库；**过期策略**：`EvictionScheduler` 定期清理过期 entries；**RMapCache** 支持单个 entry 的 TTL |
| R-6 | **Spring Cache 集成** | SpringCacheManager, SpringRedisConnection, SpringDataRedis | **@Cacheable 集成**：`RedissonSpringCacheManager` 替代默认 `ConcurrentMapCacheManager` → `RedissonCache` 包装 RMap 实现 → 支持 TTL、maxIdleTime 等 Redis 原生过期策略；**Spring Session**：`RedissonSessionRepository` 通过 `redisson-tomcat` 模块支持；**Spring Data Redis**：`redisson-spring-data` 模块适配 `RedisConnectionFactory` 接口 |
| R-7 | **RBucket/RAtomicLong/RSemaphore 基础数据结构** | RedissonBucket, RedissonAtomicLong, RedissonSemaphore | RBucket（`SET/GET` 键值对）、RAtomicLong（`INCRBY/GET` 原子计数器）、RSemaphore（`acquire/release` 分布式信号量）、RCountDownLatch（分布式倒计数锁）、RBitSet（位图）、RHyperLogLog（基数统计）——这些是 Redis 基本命令的 Java 封装 |

---

## 三、淘汰清单

| 子模块/功能 | 文件数 | 理由 |
|---|---|---|
| `api/` 801 个接口 | 801 | 每个数据结构接口都是一个独立文件——理解架构后无需逐个学习，用到查 API |
| `client/` 217 个协议文件 | 217 | Redis RESP 协议编解码实现，底层网络细节不深入 |
| `mapreduce/` | 若干 | 分布式 MapReduce，Spring 项目不用 |
| `liveobject/` | 若干 | 对象映射（类似 ORM for Redis），生产不常用 |
| `executor/` | 若干 | 分布式任务执行器，面试低频 |
| `remote/` | 若干 | RPC 远程服务调用，面试低频 |
| `reactive/` `rx/` | 若干 | 响应式流支持，独立话题 |
| `transaction/` | 若干 | Redis 事务（MULTI/EXEC），Spring `@Transactional` 不常用 |
| `jcache/` | 若干 | JCache (JSR107) 标准适配 |
| `redisson-hibernate/` `redisson-mybatis/` `redisson-tomcat/` `redisson-quarkus/` 等 | — | 框架特定集成 |
| `eviction/` `iterator/` | 若干 | 内部实现细节 |
| `cache/` | 若干 | 本地缓存细节 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 4 |
| 🟡 扩展域 | 3 |
| **总域** | **7** |
| 淘汰子模块/功能 | 12 个 |

---

## 五、与现有规划的交叉覆盖

| 交叉点 | Redisson 域 | Boot 域 | 关系 |
|---|---|---|---|
| spring-boot-starter-data-redis | R-1 连接管理 | B-10 Redis | Boot 讲自动装配，Redisson 讲连接池和数据结构实现 |
| 分布式锁 | R-2 RLock | —（Framework 不涉及） | 纯业务层功能 |
| @Cacheable | R-6 Spring Cache | B-12 缓存 | Boot 讲 `@EnableCaching`+`CacheManager` 自动装配，Redisson 讲 `RedissonSpringCacheManager` 实现 |

---

## 六、学习顺序建议

```
R-1 Redisson 主类+连接管理（理解 Netty 连接池+初始化链路）
  → R-4 命令执行流水线（理解异步执行+Lua 脚本执行）
    → R-2 RLock+Watchdog（分布式锁核心，最常用功能）
      → R-3 Codec 序列化（理解数据存取编解码）
        → R-5 RMap（本地缓存+MapWriter）
          → R-6 Spring Cache 集成（按需）
```

以上规划完成，共 **4🔴+3🟡=7 域**。考虑到 Redisson 1570+ 文件的核心规模，7 个域聚焦于 Spring 开发者最需要理解的部分，其余 800+ 数据结构 API 为"用到时查文档"。
