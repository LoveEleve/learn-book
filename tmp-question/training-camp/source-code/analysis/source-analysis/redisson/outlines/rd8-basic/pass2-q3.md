# 闭环笔记 q3: RAtomicLong 原子性 — INCRBY 的服务端原子

## 假设
RAtomicLong 的原子性来自 Redis 命令 (INCRBY/DECR) 而非客户端锁: 计数器操作在服务端单线程原子执行。compareAndSet 用 Lua 保证读-比-写原子。

## 验证过程
- RedissonAtomicLong (RedissonAtomicLong.java:43): extends RedissonExpirable
- addAndGet (L85-91): `writeAsync(name, StringCodec.INSTANCE, RedisCommands.INCRBY, name, delta)` — **INCRBY 服务端原子**
- decrementAndGet (L134): `writeAsync(name, StringCodec.INSTANCE, RedisCommands.DECR, name)` — DECR
- get (其他): GET
- compareAndSet (L120 附近): EVAL_LONG_SAFE — **Lua 保证"读-比-写"原子** (非 INCRBY 能表达的)
- 原子性来源: Redis 单线程执行命令 → 多客户端并发 INCRBY 无竞态
- StringCodec.INSTANCE: 数值用字符串编码 (Redis 数值存储为 string)

## 代码类型
Implementation (原子计数器) — 命令级原子性

## 跨域关联
- RD-4 (命令层) → writeAsync 消费
- Redis r24-string (INCRBY) → 服务端原子
- 面试点: "分布式计数器怎么保证原子?"

## 结论
RAtomicLong = 服务端原子计数器: INCRBY/DECR 靠 Redis 单线程原子性; compareAndSet 用 Lua 做"读-比-写"原子。数值存 string (StringCodec), 命令级保证并发安全。
源码位置: RedissonAtomicLong.java:85-134