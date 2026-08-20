# RD-7 Spring 集成矩阵 — 知识规划 (knowledge-planning)

> 项目: Redisson 4.6.2-SNAPSHOT | 🟡 B / 3 篇 (无 harness) | redisson-spring 四模块
> 基线: REDISSON-PLAN RD-7 — 前置: **RD-1/5/2 (被集成) + s75/s77 (Boot 对照)** — 展开 starter 取代→Cache 包装→data 适配→transaction 模板→版本矩阵
> 双链: 前置 [[rd1-connection]] [[rd5-rmap]] [[rd2-rlock]] | 复用 [[s75-boot-redis]] [[s77-boot-cache]] [[s19-cacheable]] [[s29-tx-chain]] | 对照 [[s74-boot-datasource]] [[h13-datasource]] | 引出 [[rd8-basic]]

---

## §0.8

- 🟡 B，3篇 — starter 取代链(**RedissonClient→RedissonConnectionFactory→RedisTemplate; @ConditionalOnMissingBean 用户优先 + before=DataRedis 时序 V4:60,78-97**) → Cache 包装(**RedissonCache implements Cache 包装 RMapCache: get→map.get/put→fastPut(ttl)/evict→fastRemove; NullValue/null 语义 RedissonCache:41-159**) → getCache 双路(**config.ttl>0→RMapCache else RMap; configMap 按名配 RedissonSpringCacheManager:216-282**) → data 适配(**RedissonConnectionFactory implements RedisConnectionFactory+Reactive; RedissonConnection get/set/setNX 映射 Redisson 命令 data-26:49-126**) → Transaction 模板(**extends AbstractPlatformTransactionManager; doBegin/doCommit/doRollback 三模板方法接入 Redisson 事务 RedissonTransactionManager:37-105**) → 版本矩阵(**18 子模块 data-16~41 编译期隔离**) → Properties(**spring.redis.* 映射**)
- 设计模式: [模式: 自动装配取代+适配器家族+模板方法+版本隔离]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| RedissonAutoConfigurationV4.java:60,78-97 | starter 取代 | Client→ConnectionFactory→RedisTemplate | High |
| RedissonCache.java:41-159 | Cache 包装 | RMapCache 语义映射 | High |
| RedissonSpringCacheManager.java:216-282 | getCache 双路 | config 选 RMap/RMapCache | High |
| RedissonConnectionFactory.java:49-126 | data 适配 | RedisConnectionFactory 实现 | High |
| RedissonTransactionManager.java:37-105 | Transaction | 模板方法三实现 | High |
| redisson-spring-data/ 结构 | 版本矩阵 | 18 子模块编译期隔离 | High |

---

## 02-04 聚合+分类+聚类 (3篇)

**3篇理由**: 6 闭环 → 篇1 starter 取代 (q1/q4), 篇2 Spring Cache (q2/q3), 篇3 data+transaction (q5/q6)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | starter 取代链 | 🔴 | **为什么🔴**: 集成入口 |
| P1-2 | Cache 包装 | 🔴 | **为什么🔴**: @Cacheable 落地 |
| P1-3 | ConnectionFactory | 🟡 | 数据访问 |
| P1-4 | Transaction 模板 | 🟡 | 事务面 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **自动装配** (q1) | 🔴 | 入口 |
| B | **Cache 集成** (q2/q3) | 🔴 | 常用 |
| C | **data+transaction** (q4/q5/q6) | 🟡 | 数据面 |

---

## 05 闭环结论摘要 (Pass 2 内化)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | starter 取代 | Client→ConnectionFactory→RedisTemplate; Conditional+Before 双保险 | V4:60,78-97 |
| q2 | Cache 包装 | Spring Cache 接口包装 RMapCache (get/put(ttl)/evict) | RedissonCache:41-159 |
| q3 | getCache 双路 | config.ttl>0 → RMapCache else RMap | Manager:216-282 |
| q4 | data 适配 | RedisConnectionFactory 实现, 操作映射 Redisson 命令 | data-26:49-126 |
| q5 | Transaction | AbstractPlatformTransactionManager 三模板方法 | TM:37-105 |
| q6 | 版本矩阵 | 18 子模块编译期隔离 | 结构 |

→ 引出 RD-8: 基础数据结构消费命令层 — [[rd8-basic]]