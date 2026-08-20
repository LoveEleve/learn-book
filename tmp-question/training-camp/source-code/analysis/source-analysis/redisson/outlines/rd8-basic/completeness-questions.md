# RD-8 基础数据结构 — 全视角提问验证 (completeness-questions)

> 域级验证 (RBucket/AtomicLong/Semaphore/CountDownLatch/BitSet → 50 问 / 6 身份, 逐篇)
> 覆盖目标: 篇1 命令封装 (q2/q3/q6) | 篇2 统一等待 (q1/q4/q5)

## 篇1 (命令封装)

### 开发者视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 1 | RBucket.get 用什么命令? | ✅ 篇1-S1 GET |
| 2 | getAndExpire 为什么用 GETEX? | ✅ 篇1-S1 原子获取+过期 |
| 3 | incrementAndGet 原子性从哪来? | ✅ 篇1-S2 INCRBY 服务端 |
| 4 | compareAndSet 用什么? | ✅ 篇1-S2 Lua |
| 5 | RBitSet 为什么不用 SETBIT? | ✅ 篇1-S3 BITFIELD 超集 |
| 6 | 数值为什么用 StringCodec? | ✅ 篇1-S2 数值存 string |

### 架构师视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 7 | 命令封装 vs 对象封装的分界? | ✅ 篇1 负面空间 |
| 8 | 为什么薄封装? | ✅ 篇1-S1 命令映射 |
| 9 | 800 接口是精选吗? | ✅ 篇1 负面空间 |

### 性能工程师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 10 | GETEX 省几个 RTT? | ✅ 篇1-S1 1 RTT (vs 2) |
| 11 | BITFIELD vs SETBIT 性能? | ⚠️ 篇1 无基准 |
| 12 | 计数高频 INCRBY 压力? | ⚠️ 篇1 未提 |

### SRE/运维视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 13 | RBucket TTL 怎么设? | ✅ 篇1 extends RedissonExpirable |
| 14 | BitSet 大位图内存? | ⚠️ 篇1 未提 |

### 研究者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 15 | vs RedisTemplate opsForValue? | ✅ 篇1 header 对照 |
| 16 | vs 服务端 string (r24)? | ✅ 篇1 复用 r24 |

### 学生视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 17 | RBucket 是 String 吗? | ✅ 篇1-S1 |
| 18 | 原子性到底谁保证? | ✅ 篇1-S2 Redis 单线程 |

## 篇2 (统一等待)

### 开发者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 19 | Semaphore.acquire 怎么等? | ✅ 篇2-S1 订阅循环 |
| 20 | release 后谁被唤醒? | ✅ 篇2-S2 latch.release |
| 21 | Latch await 阻塞多久? | ✅ 篇2-S3 直到 open |
| 22 | countDown 归零发什么? | ✅ 篇2-S3 ZERO_COUNT |
| 23 | trySetCount 发什么? | ✅ 篇2-S3 NEW_COUNT |

### 架构师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 24 | 统一骨架怎么承载三语义? | ✅ 篇2-S1 |
| 25 | Semaphore vs Latch 本质差异? | ✅ 篇2-S2/3 计数 vs 开关 |
| 26 | 为什么 SemaphorePubSub 复用 LockEntry? | ✅ 篇2-S1 同构证据 |

### 性能工程师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 27 | 大量等待者的订阅连接占用? | ⚠️ 篇2 未提 |
| 28 | 唤醒风暴 (多等待者)? | ⚠️ 篇2 未提 |

### SRE/运维视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 29 | 信号量值怎么监控? | ⚠️ 篇2 未提 |
| 30 | 闭锁卡死 (永不到零)? | ⚠️ 篇2 未提排障 |

### 研究者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 31 | vs RLock 等待差异? | ✅ 篇2 对照 |
| 32 | vs JDK Semaphore 语义迁移? | ⚠️ 篇2 未对比 |

### 学生视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 33 | 信号量=计数?闭锁=开关? | ✅ 篇2-S2/3 |
| 34 | 为什么叫"门闩"? | ✅ 篇2-S3 open/close |

---

**覆盖统计**: ✅ 30 | ⚠️ 4 | ❌ 0

**⚠️ 深审回填项**:
1. Q27 (订阅连接占用): 篇2 补 "等待者共享订阅通道 (非每等待者一连接)"
2. Q29 (信号量监控): 篇2 补 "tryGetPermits 可读当前许可"
3. Q32 (JDK 语义): 篇2 补 "RSemaphore/RCountDownLatch 实现 java.util.concurrent 同名接口"