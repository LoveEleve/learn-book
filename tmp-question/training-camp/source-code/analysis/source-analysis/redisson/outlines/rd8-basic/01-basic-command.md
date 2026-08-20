# RD-8 篇1 — basic-command: 基本数据结构的命令封装

> 前置: [[rd4-command]] (命令执行) + [[rd3-codec]] | 复用: [[r24-string]] (服务端 string) [[r11-bitmap]] (服务端 bitmap) | 对照: [[s75-boot-redis]] (RedisTemplate) | 引出: [[RD-8-篇2]] (统一等待)
> 🟡 B | 3 KP | [模式: 命令薄封装 + 服务端原子]
> Pass 2 闭环: q2(RBucket) q3(AtomicLong) q6(BitSet)

**读者处境**: Redisson 的 800+ 接口, 大部分是"数据结构本地化" — 但最简单的一批 (RBucket/RAtomicLong/RBitSet) 本质是**Redis 基本命令的直接封装**: get→GET, incrementAndGet→INCRBY, setBit→BITFIELD。它们简单但揭示了一个模式: 每个 Redisson 方法 = commandExecutor + RedisCommands 的映射。这篇拆基本结构的命令封装, 以及原子性从哪来 (服务端单线程)。

### 概念依赖链
q2(RBucket) ← q3(AtomicLong) ← q6(BitSet) — 先讲 String 封装, 再讲计数器原子, 最后讲位操作。

### 核心悬念
"Redisson 的原子性从哪来?get/increment/setBit 背后是哪些 Redis 命令?"

### 叙事顺序
1. 问题引入: 800 接口里的"简单派"
2. RBucket (q2) — GET/SET/GETSET/GETEX
3. RAtomicLong (q3) — INCRBY 服务端原子 + CAS Lua
4. RBitSet (q6) — BITFIELD 任意位宽
5. 收束: "命令封装模式 + 服务端原子"

### 1. RBucket — String 的命令封装

场景: get/set 背后是什么命令?
源码路径:
- `RedissonBucket extends RedissonExpirable` (RedissonBucket.java:43)
- get (RedissonBucket.java:141-147): `readAsync(name, codec, RedisCommands.GET, name)` — GET
- getAndSet (RedissonBucket.java:107): GETSET (原子交换)
- getAndExpire (RedissonBucket.java:117-137): **GETEX** — `PXAT` (绝对时间 L117) / `PX` (相对 L127) / `PERSIST` (L137) — 原子获取+过期
- getAndDelete (RedissonBucket.java:100): EVAL (Lua)
关键设计 (q2): 命令薄封装: 方法→commandExecutor+RedisCommands 一一对应; GETEX 是"获取+过期"新命令省 RTT。[模式: 命令封装]
数据流: bucket.get() → GET → Redis → 值。

### 2. RAtomicLong — 服务端原子计数器

场景: 分布式计数器怎么保证原子?
源码路径:
- `RedissonAtomicLong extends RedissonExpirable` (RedissonAtomicLong.java:43)
- addAndGet (RedissonAtomicLong.java:85-91): `writeAsync(name, StringCodec.INSTANCE, RedisCommands.INCRBY, name, delta)` — **INCRBY 服务端原子**
- decrementAndGet (RedissonAtomicLong.java:134): DECR
- compareAndSet (RedissonAtomicLong.java:120 附近): EVAL_LONG_SAFE — **Lua 读-比-写原子** (INCRBY 表达不了的 CAS)
- 原子来源: Redis 单线程执行命令 → 并发 INCRBY 无竞态
关键设计 (q3): 原子性 = 服务端单线程命令; CAS 用 Lua (读-比-写需脚本)。[模式: 命令级原子]
数据流: incrementAndGet → INCRBY → Redis 原子 +1 → 新值。

### 3. RBitSet — BITFIELD 任意位宽

场景: 位图操作怎么高效?
源码路径:
- `RedissonBitSet extends RedissonExpirable` (RedissonBitSet.java:39)
- getSigned/setSigned/incrementAndGetSigned + Unsigned 变体 (RedissonBitSet.java:47-77): 都转 bitFieldAsync
- bitFieldAsync (RedissonBitSet.java:85-94): `readAsync(name, LongCodec.INSTANCE, RedisCommands.BITFIELD_LONG)` — **BITFIELD**
- BITFIELD 语义: `GET u8 offset / SET i16 / INCRBY` — 任意位宽 (1-64bit) 原子
- vs SETBIT/GETBIT: 单 bit vs 任意位宽 (超集)
关键设计 (q6): BITFIELD = 位操作的超集: 任意位宽读写+原子自增, 一次命令一段位。[模式: 位宽操作]
数据流: setBit(i) → BITFIELD SET u1 → Redis。

### 负面空间 — 命令封装刻意不做的事

- **不做对象级封装**: RBucket/AtomicLong 是薄命令封装, 非高级语义 (对比 RMap/RLock)
- **不做本地缓存**: 基础结构无 near-cache (那是 RD-6)
- **不做事务聚合**: 单命令原子, 多操作需 batch (RD-4)
- **不封装全部 Redis 命令**: 800 接口是精选, 非穷尽

→ 引出: 信号量和闭锁怎么等待?→ [[RD-8-篇2]]