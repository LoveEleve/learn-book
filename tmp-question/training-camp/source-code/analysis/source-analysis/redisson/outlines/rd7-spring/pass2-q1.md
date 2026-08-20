# 闭环笔记 q1: starter 取代链 — RedissonConnectionFactory 替代 Lettuce/Jedis

## 假设
starter 自动装配用 RedissonConnectionFactory 替换 Boot 默认的 Lettuce/Jedis, 通过 @ConditionalOnMissingBean + @AutoConfiguration(before=DataRedisAutoConfiguration) 实现。

## 验证过程
- RedissonAutoConfigurationV4 (redisson-spring-boot-starter/.../RedissonAutoConfigurationV4.java):
  - L60: `@ConditionalOnClass({Redisson.class, RedisOperations.class, DataRedisAutoConfiguration.class})` — 依赖存在才激活
  - L95-97: `@Bean @ConditionalOnMissingBean(RedisConnectionFactory.class) RedissonConnectionFactory` — **用户没自定义时才替换**
  - L78-92: RedisTemplate/StringRedisTemplate bean (基于注入的 ConnectionFactory)
  - L114-116: `@Bean(destroyMethod="shutdown") RedissonClient` + @ConditionalOnMissingBean
  - L102-110: RedissonReactiveClient/RedissonRxClient (@Lazy)
- @AutoConfiguration(before=DataRedisAutoConfiguration): **在 Boot 的 DataRedis 之前装配** — 抢到 ConnectionFactory 位置
- 取代路径: RedissonClient → RedissonConnectionFactory → RedisTemplate/RedisTemplate 无感切换到 Redisson 连接
- 业务零改动: 注入 RedisTemplate 的地方不变, 底层连接已换

## 代码类型
Glue (自动装配) — 连接工厂的替换式集成

## 跨域关联
- RD-1 (RedissonClient) → starter 根 bean
- s75-boot-redis (Boot Redis 自动配置) → 被替换的默认
- 面试点: "starter 怎么让 RedisTemplate 用 Redisson?"

## 结论
starter 取代链 = RedissonClient → RedissonConnectionFactory → RedisTemplate; @ConditionalOnMissingBean (用户优先) + before=DataRedis (装配时序) 双保险。业务无感, 改依赖即换。
源码位置: RedissonAutoConfigurationV4.java:60,78-97,114-116