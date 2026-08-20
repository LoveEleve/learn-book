# Pass 1 探索笔记: RD-7 Spring 集成矩阵

> 方案 B (🟡) | 源码: `/data/workspace/source-code/code/spring/redisson` (4.6.2-SNAPSHOT)
> 域规模: redisson-spring 四模块 (starter 5 + cache 11 + data 18 版本子模块 + transaction 6) → 3 篇

## Pass 0 上下文吸收

- 四模块: boot-starter (自动装配) / spring-cache (CacheManager) / spring-data (ConnectionFactory) / spring-transaction (事务管理器)
- **spring-data 18 版本子模块** (data-16 ~ data-41, 对应 Spring Data Redis 版本), 每版 14-56 文件
- 取代关系: starter 用 RedissonConnectionFactory 替代 Lettuce/Jedis
- 09 审计: V4:31-33 @AutoConfiguration(before=DataRedisAutoConfiguration)

## 继承树/调用图

```
redisson-spring (4 模块)
├── boot-starter (5 文件)                    RedissonAutoConfiguration (V4 4.x)
│    ├── @ConditionalOnClass(Redisson+RedisOperations+DataRedis) L60
│    ├── RedissonClient bean (L114-116, destroyMethod=shutdown, @ConditionalOnMissingBean)
│    ├── RedissonConnectionFactory bean (L96-97, @ConditionalOnMissingBean) — 替代 Lettuce/Jedis
│    ├── RedisTemplate/StringRedisTemplate bean (L78-92)
│    └── RedissonReactiveClient/RedissonRxClient (L102-110)
│    └── RedissonProperties (spring.redis.* 映射)
├── spring-cache (11 文件)
│    ├── RedissonSpringCacheManager implements CacheManager, ResourceLoaderAware, InitializingBean (L45)
│    │    ├── configMap (Cache 名 → CacheConfig) L59
│    │    ├── getCache(name) → createMap (RMap) / createMapCache (RMapCache) L216-235
│    │    └── getMapCache (L278-282: redisson.getMapCache name/codec)
│    └── RedissonCache implements Cache (L41) — 包装 RMapCache: get→map.get; put/evict/clear
│    └── CacheConfig (ttl/maxIdleTime/maxSize L44-46)
│    └── NullValue (缓存 null 语义)
├── spring-data (18 版本子模块)
│    └── RedissonConnectionFactory implements RedisConnectionFactory+Reactive (data-26:49-50)
│         ├── getConnection → RedisConnection
│         ├── getClusterConnection / SentinelConnection
│         └── RedissonConnection (命令映射: RedisTemplate 操作 → Redisson 命令)
│    └── RedissonSubscription (PubSub 适配)
└── spring-transaction (6 文件)
     └── RedissonTransactionManager extends AbstractPlatformTransactionManager (L37)
          ├── doBegin (L73) / doCommit (L92) / doRollback (L102) — 模板方法
          └── RedissonTransactionObject (TransactionHolder)
```

## 基本元素分解

1. **starter 自动装配** (RedissonAutoConfigurationV4) — 4 beans: Client/ConnectionFactory/RedisTemplate/Reactive
2. **RedissonConnectionFactory** — RedisConnectionFactory 实现 (同步+Reactive), 取代 Lettuce/Jedis
3. **RedissonSpringCacheManager** — CacheManager 实现, configMap 按名配 TTL
4. **RedissonCache** — Spring Cache 接口包装 RMapCache (get/put/evict/clear)
5. **CacheConfig** — ttl/maxIdleTime/maxSize
6. **RedissonTransactionManager** — AbstractPlatformTransactionManager 模板方法
7. **spring-data 版本矩阵** — 18 子模块适配不同 Spring Data Redis
8. **RedissonProperties** — spring.redis.* 配置映射

## 标记问题 (6 个)

1. **Q1 取代链**: starter 怎么用 RedissonConnectionFactory 替换 Lettuce/Jedis?@ConditionalOnMissingBean 语义?Before=DataRedis 时序?
2. **Q2 Cache 包装**: RedissonCache 包装 RMapCache 的语义映射 (Spring Cache get/put/evict → RMap 操作)?NullValue 怎么处理?
3. **Q3 getCache 双路**: createMap (RMap) vs createMapCache (RMapCache) 怎么选?CacheConfig 的 ttl/maxSize 决定?
4. **Q4 ConnectionFactory 适配**: RedissonConnection 怎么把 Spring Data Redis 的 RedisTemplate 操作映射到 Redisson 命令?
5. **Q5 Transaction 模板**: RedissonTransactionManager 的 doBegin/doCommit/doRollback 怎么用 Redisson 事务?
6. **Q6 版本矩阵**: 18 个 spring-data 子模块的适配策略 (编译期版本隔离)?

## 已读测试 (2 个)

- RedissonAutoConfigurationTest (starter 测试): 自动装配验证
- RedissonSpringCacheTest (cache 测试): CacheManager 验证

## 完成检查

- [x] 继承树/调用图已画出 (四模块)
- [x] 基本元素分解 8 项有源码位置
- [x] 6 个标记问题有源码位置
- [x] 已读测试 (RedissonAutoConfigurationTest/RedissonSpringCacheTest)
- [x] 方案 B: 无 harness/时空溯源 (符合 🟡 B)