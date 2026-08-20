# RD-7 Spring 集成矩阵 — 全视角提问验证 (completeness-questions)

> 域级验证 (redisson-spring 四模块 → 50 问 / 6 身份, 逐篇)
> 覆盖目标: 篇1 starter (q1/q4) | 篇2 Cache (q2/q3) | 篇3 data/tx (q4/q5/q6)

## 篇1 (starter 取代链)

### 开发者视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 1 | 加 starter 后 RedisTemplate 底层变谁? | ✅ 篇1-S1 ConnectionFactory |
| 2 | 用户自定义 ConnectionFactory 会怎样? | ✅ 篇1-S2 ConditionalOnMissingBean |
| 3 | RedissonClient bean 怎么销毁? | ✅ 篇1-S1 destroyMethod=shutdown |
| 4 | 只想用 client 不想换模板? | ⚠️ 篇1 未提条件关闭 |

### 架构师视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 5 | before=DataRedis 的原理? | ✅ 篇1-S3 时序 |
| 6 | 替换式集成 vs 覆盖默认 Bean? | ✅ 篇1-S3 |
| 7 | 为什么不强制替换? | ✅ 篇1 负面空间 |

### 性能工程师视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 8 | Lettuce 池 vs Redisson 池切换影响? | ⚠️ 篇1 未对比性能 |

### SRE/运维视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 9 | spring.redis.* 怎么映射? | ✅ 篇1 pass1 RedissonProperties |
| 10 | 排除 starter 的开关? | ⚠️ 篇1 负面空间提了未展开 |

### 研究者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 11 | vs s74 数据源替换同构? | ✅ 篇1 header 对照 |

### 学生视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 12 | 自动装配到底做啥? | ✅ 篇1-S1 |

## 篇2 (Cache 集成)

### 开发者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 13 | @Cacheable 的 Cache 谁实现? | ✅ 篇2-S1 RedissonCache |
| 14 | put 带 TTL 吗? | ✅ 篇2-S1 fastPut ttl |
| 15 | null 值怎么处理? | ✅ 篇2-S1 allowNullValues |
| 16 | 每个 Cache 能单独配 TTL? | ✅ 篇2-S2 configMap |
| 17 | RMap 和 RMapCache 怎么选? | ✅ 篇2-S2 ttl>0 |

### 架构师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 18 | Cache 适配器 vs 直接 CacheManager? | ✅ 篇2-S1 |
| 19 | 双载体设计合理性? | ✅ 篇2-S2 按需 |
| 20 | 多级缓存为什么不做? | ✅ 篇2 负面空间 |

### 性能工程师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 21 | fastPut vs put 性能? | ⚠️ 篇2 未提 fast 语义 |
| 22 | TTL 条目清理开销? | ⚠️ 篇2 引用 RD-5 |

### SRE/运维视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 23 | CacheConfig 从哪加载? | ⚠️ 篇2 未提 configLocation |
| 24 | 缓存指标 (addCachePut)? | ⚠️ 篇2 提指标未展开 |

### 研究者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 25 | vs Boot RedisCache (s77)? | ✅ 篇2 header 对照 |
| 26 | vs Caffeine Cache? | ⚠️ 未对照 |

### 学生视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 27 | @Cacheable 数据存哪了? | ✅ 篇2-S1 |

## 篇3 (data/tx)

### 开发者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 28 | RedisTemplate.set 怎么到 Redisson? | ✅ 篇3-S1 适配层 |
| 29 | Reactive 也能用? | ✅ 篇3-S1 双实现 |
| 30 | @Transactional 怎么管 Redis? | ✅ 篇3-S2 模板 |
| 31 | 传播行为继承谁? | ✅ 篇3-S2 AbstractPlatformTransactionManager |
| 32 | 版本矩阵怎么选? | ✅ 篇3-S3 按 SD Redis 版本 |

### 架构师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 33 | 编译期隔离 vs 运行时? | ✅ 篇3-S3 编译期 |
| 34 | 模板方法模式价值? | ✅ 篇3-S2 |
| 35 | 18 版本维护成本? | ✅ 篇3 负面空间 |

### 性能工程师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 36 | 命令反射分发开销? | ⚠️ 篇3 execute 提未量化 |
| 37 | 事务提交网络往返? | ⚠️ 篇3 未提 |

### SRE/运维视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 38 | 版本错配报什么错? | ✅ 篇3-S3 NoSuchMethodError |
| 39 | 事务回滚日志? | ⚠️ 篇3 未提 |

### 研究者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 40 | vs Lettuce 连接适配差异? | ✅ 篇3 header 对照 |
| 41 | vs Jedis? | ⚠️ 未对照 |

### 学生视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 42 | 四模块各管什么? | ✅ 篇3 收束 |

---

**覆盖统计**: ✅ 34 | ⚠️ 8 | ❌ 0

**⚠️ 深审回填项**:
1. Q23 (CacheConfig 来源): 篇2 补 "configLocation (XML/外部文件) 加载" — 需验证
2. Q24 (缓存指标): 篇2 补 addCachePut/Evictions 是 Micrometer 指标钩子
3. Q36 (反射分发): 篇3 execute(method,args) 提反射未量化 — 标 [无基准]
4. Q4/Q10: starter 条件关闭 (排除 starter) 篇1 负面空间已提