# stage-2 · 第 27 节：分布式缓存设计 — 知识点提取

> 课程：stage-2 模式设计与实现 第 27 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/27. 第二十七节：分布式缓存设计.md`
> 提取时间：2026-08-11 | 权重：核心（分布式缓存主线，源码级）

---

## 一、本节概览

- **技术域**：缓存基础理论 + 客户端缓存(HTTP) + 服务端缓存(JSR-107/Spring Cache) + 分布式缓存(Redis/Redisson 主流)
- **维度**：`[性能优化]`（缓存/吞吐/命中率）+ `[工程问题]`（缓存抽象/多级缓存，源码级）+ `[分布式问题]`（分布式缓存/锁）
- **核心命题**：理解缓存设计——基础理论(吞吐/命中率/淘汰)、客户端缓存(HTTP)、服务端缓存(抽象层)、分布式缓存(**Redis/Redisson 主流**)
- **知识点数**：13 个
- **前置**：HTTP 协议、Redis 基础、Spring AOP、缓存概念

## 前置条件清单
读者需先掌握：
1. **HTTP 缓存头**（ETag/Last-Modified）
2. **Redis 基础**（数据结构/主从）
3. **Spring AOP**（第 14 节）
4. **缓存概念**（LRU/TTL）
未达前置者，先补：HTTP 缓存 + Redis 基础

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节，方案 2：主流优先）：
- **JSR-107/Spring Cache 简提（🟡 工作层）**：缓存**抽象层**规范(CacheManager/Cache/@Cacheable)，理解抽象但非知识本体
- **Redis/Redisson 主流定位（🔴 深层，点到为止）**：**分布式缓存知识本体**——Redis 集中式/集群 + Redisson(锁/缓存) 生产主流；本篇只作**主流定位 + 关键类指引**，**Redis/Redisson 源码深挖后续单独规划**（源码提取主体，见进度 source/ 未开始）；本地 `code/spring/redis` + `code/spring/redisson` 完整源码供后续深挖

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 缓存基础理论（吞吐量/命中率/淘汰）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：缓存概念
- **来源**：docs §缓存基础理论
- **需求**：理解缓存核心指标与淘汰策略
- **自主实现**：若我设计——吞吐(QPS/TPS)、命中率、淘汰策略(LRU/TTL)
- **参考实现**（docs）：**吞吐量**——QPS(Queries Per Second)/TPS(Transactions Per Second)，单机 1W-10W，公司服务 2W；**命中率**——缓存命中次数/总请求数，一般达 50%；**淘汰策略**——**LRU**(Java WeakHashMap 被动淘汰，GC 决定)/**基于时间**(TTL/Delayed)
- **对比取舍**：**吞吐/命中率/淘汰**——缓存三要素；LRU(被动) vs TTL(主动)
- **测试佐证**：JDK `WeakHashMap` + Redis 淘汰策略

### KP-02 客户端缓存设计（HTTP 协议）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：HTTP
- **来源**：docs §客户端缓存设计
- **需求**：理解 HTTP 客户端缓存（ETag/Last-Modified）
- **自主实现**：若我设计——响应头 ETag/Last-Modified + 请求头 If-None-Match/If-Modified-Since
- **参考实现**（docs + 架构师）：HTTP 客户端缓存——**DefaultServlet**(Tomcat 静态资源) 写 `ETag`(如 46ae-5fb57efcaf0cd-gzip)/`Last-Modified`；下次请求——ETag 作为 `If-None-Match`、Last-Modified 作为 `If-Modified-Since`；**匹配返回 304**(读取客户端缓存)
- **对比取舍**：**304 协商缓存**——服务端校验未变返回 304，客户端用缓存；减少传输
- **测试佐证**：HTTP 缓存头机制 + docs DefaultServlet 示例

### KP-03 Servlet 基础（Context/Dispatcher 类型）
- **维度**：`[规范]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[有效]`（Servlet） | **置信度**：High
- **前置**：Servlet
- **来源**：docs §Servlet 基础
- **需求**：理解 ServletContext 与 Dispatcher 类型
- **自主实现**：无（背景）
- **参考实现**（docs）：**ServletContext**——Web 应用上下文，Context Path=应用根路径(如 /manager)；**Dispatcher 类型**——FORWARD(转发)/INCLUDE(合并)/REQUEST(原始)/ERROR(错误)/ASYNC(Servlet 3.0+ 异步)
- **对比取舍**：**五种 Dispatcher**——请求分发类型
- **测试佐证**：Servlet 规范 + Tomcat 源码

### KP-04 服务端缓存抽象（JSR-107/Java Cache 简提）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]`（JSR-107 规范） | **置信度**：High
- **前置**：缓存
- **来源**：docs §基于 Java Cache（JSR-107）
- **需求**：理解 Java 缓存抽象规范（非知识本体，抽象层）
- **自主实现**：若我设计——CacheManager/Cache/Configuration 抽象
- **参考实现**（docs + 架构师）：**JSR-107(Java Cache)**——依赖 Java Interceptor(AOP) 注解拦截；**CacheManager**(缓存管理器)/**Configuration**(配置，getKeyType/getValueType/isStoreByValue)/**CompleteConfiguration**(完整配置)/**Cache**(isReadThrough 读击穿/isWriteThrough 写击穿/isStatisticsEnabled 统计/isManagementEnabled JMX/getExpiryPolicyFactory 淘汰策略工厂)
- **对比取舍**：**抽象规范**——理解缓存抽象(CacheManager/Cache)，但生产用 Redis 等实现，JSR-107 只是接口
- **测试佐证**：Spring Cache(整合 JSR-107 的抽象) 源码

### KP-05 Spring Cache 抽象（简提）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：Spring AOP
- **来源**：docs §基于 Spring Cache + 源码验证
- **需求**：理解 Spring Cache 抽象（缓存门面）
- **自主实现**：若我设计——CacheManager/Cache 接口 + @Cacheable + CacheInterceptor
- **参考实现**（源码 spring-context）：`CacheManager`(32/getCache 43)、`Cache`(接口)、`@Cacheable`(注解，key/condition SpEL)、`CacheInterceptor`(46，MethodInterceptor 拦截)；RedissonSpringCacheManager(45，Redis 实现) 整合
- **对比取舍**：**Spring Cache 抽象**——统一缓存门面，可换 Redis/本地实现；生产常用 Redis 实现
- **测试佐证**：源码 `spring-context/.../cache/CacheManager.java`(32/43)+`CacheInterceptor.java`(46)+`redisson-spring-cache/.../RedissonSpringCacheManager.java`(45/216)

### KP-06 Java Interceptor（JSR-308，拦截器注解体系）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：注解、AOP
- **来源**：docs §Java Interceptor（JSR-308）
- **需求**：理解拦截器注解体系（@InterceptorBinding，JSR-107 缓存拦截的实现基础）
- **自主实现**：若我设计——@InterceptorBinding 标注注解 + 拦截器实现类
- **参考实现**（docs）：**JSR-308 拦截注解**——**@Interceptor 约定**：通常注解需标注/元标注 `@InterceptorBinding`(Java Common Annotations 打破：@PostConstruct/@PreDestroy)；**标注 @InterceptorBinding**——`@Logging`(@InterceptorBinding+@Inherited+name @Nonbinding)；**元标注 @InterceptorBinding**——`@Monitored` 标注 @DataAccess(元标注 @InterceptorBinding)
- **对比取舍**：**拦截器注解体系**——@InterceptorBinding 绑定拦截行为；JSR-107 缓存拦截(AOP) 的基础
- **测试佐证**：docs @Logging/@Monitored/@DataAccess 源码

### KP-07 多级缓存实现（CacheLoader/CacheWriter + Composite）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §多级缓存实现 + 架构师发散
- **需求**：实现多级缓存（本地+分布式+DB）
- **自主实现**：若我设计——CacheLoader(降级存储) + CacheWriter + 组合 Cache
- **参考实现**（docs + 架构师）：**基于 CacheLoader/CacheWriter**——内存 Cache + CacheLoader 用 **FallbackStorage**(Redis/MySQL/File System 降级存储链)；**基于组合 Cache(Composite)**——多级缓存组合；**同步多级缓存**——基于缓存条目事件监听
- **对比取舍**：**多级缓存**——本地(快)→Redis(分布)→DB(持久)；CacheLoader 降级
- **测试佐证**：架构师 + Spring Cache Loader 概念

### KP-08 缓存事件/淘汰/击穿/处理
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §缓存时间/淘汰/击穿/处理（骨架节）+ 架构师发散
- **需求**：理解缓存事件/淘汰策略/击穿处理
- **自主实现**：若我设计——事件监听 + 淘汰策略 + 击穿防护
- **参考实现**（docs 骨架 + 架构师）：**Cache Events**(缓存事件)/**Cache Expiry Policies**(淘汰策略：LRU/TTL/访问频率)/**Cache Through**(缓存击穿——热 key 失效并发穿透，用互斥锁/逻辑过期)/**Cache Processing**(缓存处理)
- **对比取舍**：**击穿防护**——互斥重建/逻辑过期；缓存三高问题(击穿/穿透/雪崩)
- **测试佐证**：架构师 + Redis 缓存三高

### KP-09 Redis 分布式缓存（集中式主流，深挖）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（Redis 当前主流） | **置信度**：High
- **前置**：Redis
- **来源**：架构师发散 + redis 源码验证（docs 仅多级缓存 FallbackStorage 提及；**Redis 是分布式缓存主流，深挖**）
- **需求**：理解 Redis 集中式分布式缓存
- **自主实现**：若我设计——Redis 集中式(单节点高吞吐) 缓存
- **参考实现**（redis 源码 + 架构师）：**Redis 集中式缓存**——生产主流；本地 `code/spring/redis`(完整源码) 可验证；**集中式 vs 副本式**——集中式(单点高性能，需主从高可用)；副本式(多副本，一致性与可用性权衡，呼应第 1 节 CAP)
- **对比取舍**：**集中式 vs 副本式**——集中简单高性能(主从备)；副本式读扩展(一致性复杂)
- **测试佐证**：`code/spring/redis` 源码 + 第 1 节 CAP

### KP-10 Redis Cluster（集群缓存，深挖）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-08
- **来源**：架构师发散 + redis 源码验证
- **需求**：理解 Redis Cluster（分片 + 高可用）
- **自主实现**：若我设计——16384 槽位分片 + 主从 + Gossip 集群
- **参考实现**（redis 源码）：**Redis Cluster**——**16384 槽位**(key 哈希分片)、主从复制(高可用)、Gossip 协议(节点通信，呼应第 23 节)；`cluster.c` 源码(CLUSTER SLOTS 837)
- **对比取舍**：**Cluster vs 单节点**——分片扩展容量；槽位路由(客户端/代理)
- **测试佐证**：redis 源码 `src/cluster.c`(837)

### KP-11 Redisson 分布式锁（深挖）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（Redisson 当前主流） | **置信度**：High
- **前置**：Redis、分布式锁
- **来源**：架构师发散 + redisson 源码验证（docs 未覆盖，深挖主流）
- **需求**：用 Redisson 实现分布式锁
- **自主实现**：若我设计——Redis 分布式锁 + 看门狗续期
- **参考实现**（redisson 源码）：`RedissonLock`——`tryAcquire`(105，Redis 加锁)/`latch.tryAcquire`(131，等待释放)；`RedissonRedLock`(红锁，多节点)；`RLock`(接口)；**看门狗**(internalLockLeaseTime 61，自动续期防死锁)
- **对比取舍**：**Redisson 锁**——看门狗续期 + 红锁；对比 ZK 锁(第 12 节) 与 Redis 单锁(可能丢锁)
- **测试佐证**：源码 `redisson/redisson/src/main/java/org/redisson/RedissonLock.java`(61/105/131)+`RedissonRedLock.java`

### KP-12 Redisson 缓存（Spring Cache 的 Redis 实现，深挖）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-05、Redisson
- **来源**：架构师发散 + redisson 源码验证
- **需求**：用 Redisson 实现 Spring Cache（Redis 后端）
- **自主实现**：若我设计——RedissonSpringCacheManager 实现 CacheManager
- **参考实现**（redisson 源码）：`RedissonSpringCacheManager implements CacheManager`(45)——`getCache`(216)/`getCacheNames`(286)；`RedissonCache`(Spring Cache 的 Redis 实现)——把 @Cacheable 落到 Redis
- **对比取舍**：**Spring Cache + Redis**——Redisson 作为 Spring Cache 后端，@Cacheable 注解透明落 Redis
- **测试佐证**：源码 `redisson-spring/redisson-spring-cache/.../RedissonSpringCacheManager.java`(45/216)+`RedissonCache.java`

### KP-13 分布式缓存设计要点（总结 + 就业/简历）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~11
- **来源**：架构师整合 + docs §就业方面
- **需求**：总结分布式缓存设计要点
- **自主实现**：若我设计——抽象层(Spring Cache) + 主流实现(Redis/Redisson) + 多级缓存
- **参考实现**（架构师整合）：设计要点——①**缓存抽象**(JSR-107/Spring Cache，简提) ②**主流实现 Redis**(集中式/Cluster) ③**Redisson**(锁/缓存) ④**多级缓存**(本地→Redis→DB) ⑤**三高防护**(击穿/穿透/雪崩)；docs §就业方面(简历优化/项目亮点)——面试工程
- **对比取舍**：**抽象 + 主流实现**——Spring Cache 抽象 + Redis/Redisson 主流；简历/面试结合
- **测试佐证**：整合本篇 KP

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 缓存基础理论 | 性能优化 | 核心 | P1 | 🔴 | 时间无关 | High |
| 客户端缓存(HTTP) | 性能优化 | 核心 | P1 | 🟡 | 时间无关 | High |
| Servlet 基础 | 规范 | 支撑 | P3 | 🟢 | 有效 | High |
| JSR-107 抽象(简提) | 规范 | 核心 | P1 | 🟡 | 有效 | High |
| Spring Cache 抽象(简提) | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| Java Interceptor(JSR-308) | 规范 | 核心 | P1 | 🟡 | 时间无关 | High |
| 多级缓存 | 性能优化 | 核心 | P1 | 🟡 | 时间无关 | High |
| 缓存事件/淘汰/击穿 | 性能优化 | 支撑 | P2 | 🟡 | 时间无关 | High |
| Redis 集中式缓存(主流定位) | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| Redis Cluster(主流定位) | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| Redisson 分布式锁(主流定位) | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| Redisson 缓存(主流定位) | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| 设计要点 + 就业 | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/redis`(完整 Redis 源码，cluster.c)、`code/spring/redisson`(RedissonLock/RedissonRedLock/RLock/RedissonSpringCacheManager/RedissonCache)、`code/spring/spring-framework`(spring-context Cache 包)
- **关键源码类**（本次实证）：`CacheManager`(32/43)、`CacheInterceptor`(46)、`RedissonSpringCacheManager`(45/216)、`RedissonLock`(61/105/131)、redis `cluster.c`(837)
- **诚实标注**：JSR-107 无独立源码(Spring 整合)；Redis 集中式/集群为深挖主流(08 参考实现按主流性)
- **关联标注**：microsphere-redis `[待验证]`；衔接第 1 节 CAP、第 12 节 ZK 锁、第 28 节缓存实战

---

## 五、本节小结（三层次视角）

**需求**：设计分布式缓存——理论(吞吐/命中率/淘汰)、客户端(HTTP)、服务端(抽象层)、分布式(Redis/Redisson 主流)。

**自主实现核心**：若我设计——
1. 缓存三要素：吞吐/命中率/淘汰(LRU/TTL)
2. HTTP 客户端缓存(ETag/304)
3. 抽象层：JSR-107/Spring Cache(简提，CacheManager/@Cacheable)
4. **Redis 主流(深挖)**：集中式 + Cluster(16384 槽位)
5. **Redisson(深挖)**：分布式锁(看门狗/红锁) + Spring Cache 后端
6. 多级缓存(本地→Redis→DB) + 三高防护

**参考实现**：redis/redisson/spring-context 源码(深挖验证) + docs(JSR-107/Spring Cache 简提)。

**对比取舍**：知识本体是"**分布式缓存设计**"。核心洞察：**抽象层(JSR-107/Spring Cache) + 主流实现(Redis/Redisson)**——按主流性选(08)，Redis 是分布式缓存本体，JSR-107 是抽象参考。

**待验证汇总**：
- microsphere-redis 具体场景
- JSR-107 实现(无本地独立源码)

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为缓存骨架(JSR-107/Spring Cache 主线) + redis/redisson 源码深挖(主流)；方案 2：抽象简提、主流深挖。

### 完整认知：分布式缓存设计在真实架构中完整该讲什么

docs 覆盖了 JSR-107/Spring Cache/多级缓存。作为架构师，这个主题完整还该包含：

1. **Redis 是分布式缓存绝对主流**：集中式(单节点高性能) + Redis Cluster(16384 槽位分片) + 主从高可用——生产事实标准（docs 只把它当 FallbackStorage，这是 docs 局限，需补全）
2. **Redisson 是 Redis 的 Java 高阶客户端**：分布式锁(看门狗续期防死锁/红锁多节点)、Spring Cache 后端(@Cacheable 落 Redis)——生产标配
3. **缓存抽象层的价值**：JSR-107/Spring Cache 是"抽象规范"，理解 CacheManager/Cache 便于换实现——但知识本体是 Redis
4. **多级缓存范式**：本地(Guava/Caffeine)→Redis→DB——CacheLoader 降级链；需一致性处理
5. **缓存三高**：击穿(热 key 失效并发)/穿透(不存在 key 打 DB)/雪崩(批量失效)——防护是面试/生产高频
6. **缓存一致性**：写 DB 后删缓存/双写——最终一致(呼应第 1 节 BASE)
7. **集中式 vs 副本式**：Redis 集中式(主从) vs 副本式缓存——CAP 权衡(第 1 节)

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 集中式 vs 副本式 | 单点高性能；多副本一致性复杂 |
| Redis vs JSR-107/Spring Cache | 主流实现；抽象规范 |
| 单机 vs Cluster | 简单；16384 槽位分片扩展 |
| Redisson 锁 vs ZK 锁 | 快但可能丢锁；强一致(第 12 节) |
| 多级缓存 | 本地快；一致性复杂 |

### 常见坑/反模式

1. **缓存三高未防护**：击穿/穿透/雪崩——互斥重建/布隆过滤/随机过期
2. **缓存与 DB 不一致**：双写无策略——删缓存/最终一致
3. **Redis 单点**：无主从高可用——主从 + 哨兵/Cluster
4. **锁不续期**：Redisson 看门狗/锁超时——防死锁
5. **大 key/热 key**：Redis 大 key 阻塞/热 key 单点——拆分/本地缓存

### 生态位置

- **性能优化维度**：分布式缓存是**性能优化核心**——承接第 1 节 CAP、第 12 节锁，为第 28 节缓存实战铺垫
- **衔接**：CAP(第 1 节) → ZK 锁(第 12 节) → 缓存设计(本篇) → 缓存实战(第 28 节)
- **与源码提取的关系**：redis/redisson/spring-context 是核心源码

**架构师视角结论**：本篇不只是背 JSR-107，而是"**理解分布式缓存的主流实践**"——JSR-107/Spring Cache 是抽象规范(简提)，**Redis 集中式/Cluster + Redisson(锁/缓存) 是知识本体(深挖)**；多级缓存 + 三高防护是生产核心；这是性能优化的关键基础设施。
