# 闭环笔记 q4: ConnectionFactory 适配 — RedisTemplate 操作映射到 Redisson 命令

## 假设
RedissonConnectionFactory 实现 RedisConnectionFactory (同步+Reactive), RedissonConnection 把 Spring Data Redis 的 RedisTemplate 操作 (get/set/hSet 等) 映射到 Redisson 命令执行。

## 验证过程
- RedissonConnectionFactory (data-26/.../RedissonConnectionFactory.java:49-50): `implements RedisConnectionFactory, ReactiveRedisConnectionFactory, InitializingBean, DisposableBean`
  - getConnection (L118) → RedissonConnection
  - getClusterConnection (L126) / SentinelConnection
- RedissonConnection (RedissonConnection.java):
  - get (L516) / set (L535-540) / setNX (L568) / setEx (L575) / getRange (L646) / setBit (L663) — **Spring Data Redis 操作集**
  - execute(command, args) (L178-201): 反射式命令分发 (method → Redisson)
- 双向: 同步 (RedisConnection) + Reactive (ReactiveRedisConnectionFactory) — RedisTemplate 和 reactive 都能用
- 版本矩阵: 18 子模块 (data-16~41) 编译期隔离不同 Spring Data Redis API

## 代码类型
Interface (适配层) — Spring Data Redis API → Redisson 命令

## 跨域关联
- RD-1 (连接) → RedissonConnection 用 commandExecutor
- s75-boot-redis → RedisTemplate 的默认连接是 Lettuce/Jedis, 此处替换
- 面试点: "RedisTemplate 底层换了, 业务要改吗?"

## 结论
ConnectionFactory = Spring Data Redis 适配层: RedissonConnection 实现 get/set/hSet 等操作集, 映射到 Redisson 命令; 同步+Reactive 双实现; 18 版本子模块编译期隔离。业务 RedisTemplate 无感切换。
源码位置: RedissonConnectionFactory.java:49-126, RedissonConnection.java:516-663