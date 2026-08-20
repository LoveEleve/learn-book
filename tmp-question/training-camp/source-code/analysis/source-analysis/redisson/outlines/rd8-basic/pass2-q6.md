# 闭环笔记 q6: RBitSet BITFIELD — 任意位宽读写

## 假设
RBitSet 用 BITFIELD (Redis 6.2) 而非 SETBIT/GETBIT: 支持任意位宽 (1-64bit) 的有符号/无符号读写 + 原子自增, 比单个 bit 操作强大得多。

## 验证过程
- RedissonBitSet (RedissonBitSet.java:39): extends RedissonExpirable
- getSigned/setSigned/incrementAndGetSigned + Unsigned 变体 (L47-77): 都转 `bitFieldAsync(args)`
- bitFieldAsync (L85-94): `readAsync(name, LongCodec.INSTANCE, RedisCommands.BITFIELD_LONG, ...)` / writeAsync — **BITFIELD 命令**
- BITFIELD 语义: `BITFIELD key GET u8 offset / SET i16 offset val / INCRBY` — 任意位宽 (u8/u16/i32/i64, i16...) 原子操作
- vs SETBIT/GETBIT: 单 bit vs 任意位宽 (BITFIELD 是超集)
- LongCodec: 位值用 long 编码
- 应用: 布隆/统计/状态位 (RD-0 提到 BloomFilter)

## 代码类型
Implementation (位操作封装) — BITFIELD 任意位宽

## 跨域关联
- RD-4 (命令层) → readAsync/writeAsync
- Redis r11-bitmap (服务端 bitmap) → BITFIELD 服务端
- 面试点: "位图存统计怎么高效?"

## 结论
RBitSet = BITFIELD 封装: 任意位宽 (1-64bit) 有/无符号读写 + 原子自增, 是 SETBIT/GETBIT 的超集。一次命令处理一段位, 适合布隆/统计/标志位。LongCodec 承载位值。
源码位置: RedissonBitSet.java:47-94