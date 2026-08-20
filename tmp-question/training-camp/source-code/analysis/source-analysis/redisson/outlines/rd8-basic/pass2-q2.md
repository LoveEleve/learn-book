# 闭环笔记 q2: RBucket 命令面 — String 的 GET/SET/GETEX

## 假设
RBucket 是 String 的 Java 封装: get→GET, set→SET, getAndSet→GETSET, getAndExpire→GETEX。GETEX (Redis 6.2) 支持获取+过期原子操作。

## 验证过程
- RedissonBucket (RedissonBucket.java:43): extends RedissonExpirable
- get (L141-147): `readAsync(name, codec, RedisCommands.GET, name)` — GET
- getAndSet (L107): `writeAsync(name, codec, RedisCommands.GETSET, name, encode(newValue))` — GETSET (原子交换)
- getAndExpire (L117-137): `writeAsync(name, codec, RedisCommands.GETEX, name, "PXAT", epochMilli)` (L117, 绝对时间) / `"PX", duration` (L127, 相对) / `"PERSIST"` (L137) — **GETEX 原子获取+过期**
- getAndDelete (L100): EVAL_OBJECT — Lua
- 封装模式: 每个方法 → commandExecutor.read/write/eval + RedisCommands.XXX
- GETEX 价值: 获取值同时设过期 — 一个 RTT 完成 (以前 GET+EXPIRE 两命令非原子)

## 代码类型
Implementation (命令封装) — String 基本命令的薄封装

## 跨域关联
- RD-4 (命令层) → commandExecutor 消费
- r24-string (Redis 服务端 string) → GET/SET 服务端面
- 面试点: "RBucket 和 RedisTemplate opsForValue 区别?"

## 结论
RBucket = String 薄封装: GET/SET/GETSET/GETEX 直接映射; GETEX 是"获取+过期"原子新命令 (省 RTT)。封装模式 = 方法→commandExecutor+RedisCommands 一一对应。
源码位置: RedissonBucket.java:100-147