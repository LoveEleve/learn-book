# RD-7 篇1 — starter: 自动装配取代 Lettuce/Jedis

> 前置: [[rd1-connection]] (RedissonClient) + [[s75-boot-redis]] (Boot Redis 自动配置) | 复用: [[s77-boot-cache]] (Cache 装配) | 对照: [[s74-boot-datasource]] (数据源替换同构) | 引出: [[RD-7-篇2]] (Cache 集成) + [[RD-7-篇3]] (data/transaction)
> 🟡 B | 2 KP | [模式: 条件装配 + 用户优先 + 时序控制]
> Pass 2 闭环: q1(starter 取代) q4(data 适配 前置)

**读者处境**: 你的 Spring Boot 项目里 `RedisTemplate` 是谁给的?默认是 Lettuce 连接 — 但加一个 redisson-spring-boot-starter 依赖后, RedisTemplate 的底层连接就变成 Redisson 了, 业务代码一行不改。怎么做到的?这篇拆自动装配的取代链: RedissonClient → RedissonConnectionFactory → RedisTemplate, 以及 @ConditionalOnMissingBean (用户优先) + before=DataRedis (时序) 的双保险。

### 概念依赖链
q1(starter 取代) ← q4(data 前置) — 先讲装配链怎么换连接工厂, 再提 RedissonConnectionFactory 是 data 模块产物。

### 核心悬念
"加一个依赖, RedisTemplate 底层连接就换了 —— 自动装配怎么做到'无感取代'？"

### 叙事顺序
1. 问题引入: starter 依赖的魔法
2. 取代链 (q1) — Client → ConnectionFactory → RedisTemplate
3. 条件装配 (q1) — @ConditionalOnMissingBean 用户优先
4. 时序 (q1) — before=DataRedisAutoConfiguration
5. 收束: "替换式集成"

### 1. 取代链 — Client → Factory → Template

场景: 加 starter 后连接怎么换?
源码路径:
- `RedissonAutoConfigurationV4` (RedissonAutoConfigurationV4.java:60,78-116):
  - L60: `@ConditionalOnClass({Redisson, RedisOperations, DataRedisAutoConfiguration})` — 依赖齐全才激活
  - L114-116: `@Bean(destroyMethod="shutdown") RedissonClient` + @ConditionalOnMissingBean — 根对象
  - L95-97: `@Bean @ConditionalOnMissingBean(RedisConnectionFactory.class) RedissonConnectionFactory(redisson)` — **替换 Lettuce/Jedis**
  - L78-92: RedisTemplate/StringRedisTemplate bean (注入 ConnectionFactory)
- 取代路径: RedissonClient → RedissonConnectionFactory → RedisTemplate
关键设计 (q1): 连接工厂替换 = 在 Boot 默认 (Lettuce/Jedis) 之前声明自己的 ConnectionFactory, RedisTemplate 自动用它。[模式: 替换式装配]
数据流: RedissonClient bean → RedissonConnectionFactory → RedisTemplate → 业务注入。

### 2. 条件装配 — 用户优先

场景: 用户自己配了连接工厂怎么办?
源码路径:
- `@ConditionalOnMissingBean(RedisConnectionFactory.class)` (RedissonAutoConfigurationV4.java:95): **用户已自定义 → 不替换**
- @ConditionalOnMissingBean(RedissonClient.class) (RedissonAutoConfigurationV4.java:115): 用户自定义 Client → 不覆盖
- 原则: starter 是默认值, 用户显式配置优先
- @ConditionalOnMissingBean(name="redisTemplate") / StringRedisTemplate (RedissonAutoConfigurationV4.java:79,87): 模板同理
关键设计 (q1): 条件装配 = 默认兜底非强制: 用户自定义的 Bean 永远优先。[模式: 用户优先]
数据流: 装配时查已有 Bean → 有则跳过 → 无则建默认。

### 3. 时序 — before=DataRedis

场景: 为什么能抢在 Boot 默认前面?
源码路径:
- `@AutoConfiguration(before = DataRedisAutoConfiguration.class)` (V4:31-33): **在 Boot 的 Redis 自动配置之前装配**
- 时序保证: Redisson 的 ConnectionFactory 先注册 → Boot 默认 (Lettuce/Jedis) 看到已存在 → 跳过
- 对比: 无 before → Boot 默认先注册 → Redisson 的 @ConditionalOnMissingBean 失效 (发现 Lettuce 已建)
关键设计 (q1): before 时序 = 抢先注册让默认失效; 与 @ConditionalOnMissingBean 配合双保险。[模式: 装配时序]
数据流: AutoConfiguration 排序 → Redisson 先 → Boot 默认 skipped。

### 负面空间 — starter 刻意不做的事

- **不强制替换**: @ConditionalOnMissingBean 用户优先, 可关 (排除 starter)
- **不覆盖已有模板**: redisTemplate 已存在则跳过
- **不做 Bean 覆盖战争**: 靠条件+时序, 非覆盖默认 Bean
- **不处理 Lettuce 专用配置**: Lettuce 的池/SSL 配置不迁移 (用户需改 Redisson 配置)
- **不提供 XML 配置**: 纯注解装配 (4.0.0 砍 Spring XML)

→ 引出: @Cacheable 背后的 CacheManager 怎么用 Redisson?→ [[RD-7-篇2]]