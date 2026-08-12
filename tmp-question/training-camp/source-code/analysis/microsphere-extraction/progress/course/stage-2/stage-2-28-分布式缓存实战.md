# stage-2 · 第 28 节：分布式缓存实战 — 知识点提取

> 课程：stage-2 模式设计与实现 第 28 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/28. 第二十八节：分布式缓存实战.md`
> 提取时间：2026-08-11 | 权重：核心（分布式缓存实战主线，源码级，stage-2 docs 收官）

---

## 一、本节概览

- **技术域**：分布式缓存实战（限流/分布式锁/分布式 Session/幂等性服务）
- **维度**：`[分布式问题]`（限流/锁/会话/幂等）+ `[工程问题]`（J.U.C/Redisson/Session，源码级）
- **核心命题**：理解分布式缓存实战四大场景——限流(RateLimiter)、分布式锁(Redisson)、分布式 Session、幂等性服务
- **知识点数**：10 个
- **前置**：第 27 节缓存设计、J.U.C(AQS)、Redis/Redisson、Servlet Session

## 前置条件清单
读者需先掌握：
1. **分布式缓存**（第 27 节：Redis/Redisson）
2. **J.U.C**（AQS：Lock/Semaphore）
3. **Redis 数据结构**（Hash/zset）
4. **Servlet Session**（HttpSession）
未达前置者，先补：第 27 节 + J.U.C + Redis 基础

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：Redisson 锁/限流对照源码讲
- **工程化弱**：Session 分布式/幂等补基础
- **必做**：对照 `code/spring/redisson` 源码验证（08 教训）；Redis/Redisson 深挖后续单独规划（第 27 节约定）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 预备知识（J.U.C 并发框架 + AQS）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Java 并发
- **来源**：docs §预备知识
- **需求**：理解 J.U.C 并发框架（分布式化的基础）
- **自主实现**：若我设计——AQS(AbstractQueuedSynchronizer) 核心组件
- **参考实现**（docs + 架构师）：**J.U.C** 核心组件——**AQS**(AbstractQueuedSynchronizer)——Lock/Semaphore 的同步基础；**基于 Redis 实现 AQS 组件**——把 J.U.C 的 Lock/Semaphore 分布式化(Redisson 实现)
- **对比取舍**：**AQS 分布式化**——J.U.C 本地并发 → Redisson 分布式锁/信号量
- **测试佐证**：JDK J.U.C + redisson 源码

### KP-02 限流场景（Bulkhead/RateLimiter/TimeLimiter/CircuitBreaker）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：限流
- **来源**：docs §限流场景 + 架构师发散
- **需求**：理解四种限流类型
- **自主实现**：若我设计——Bulkhead(并发数)/RateLimiter(频率)/TimeLimiter(时长)/CircuitBreaker(失败)
- **参考实现**（docs + 架构师）：四类限流——**Bulkhead**(控制并发数量)/**RateLimiter**(控制并发频率，Redisson 实现见 KP-03)/**TimeLimiter**(控制并发操作时长)/**CircuitBreaker**(控制并发失败操作，熔断，衔接 stage-1 第 8 节容错)；**docs 空节标注（08 §2）**——docs 对 Bulkhead(23 行)/TimeLimiter(30 行)/CircuitBreaker(32 行) 仅标题无正文，仅 RateLimiter 有 Redisson 参考链接——架构师发散补全四类定义，RateLimiter 深挖见 KP-03
- **对比取舍**：**四种限流维度**——数量/频率/时长/失败
- **测试佐证**：docs 分类 + 第 8 节容错 + redisson RateLimiter 源码(KP-03)

### KP-03 Redisson RateLimiter（分布式限流，源码验证）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（Redisson） | **置信度**：High
- **前置**：KP-02、Redis
- **来源**：docs §RateLimiter + redisson 源码验证
- **需求**：用 Redisson 实现分布式限流
- **自主实现**：若我设计——RedissonRateLimiter(trySetRate + tryAcquire)
- **参考实现**（redisson 源码）：`RedissonRateLimiter extends RedissonExpirable implements RRateLimiter`(41)——`tryAcquire()`(64)/`tryAcquire(long permits)`(74)/`tryAcquireAsync`(79，Lua 脚本实现原子性)
- **对比取舍**：**Lua 原子限流**——Redisson RateLimiter 用 Lua 脚本保证原子性(令牌桶)
- **测试佐证**：源码 `redisson/redisson/src/main/java/org/redisson/RedissonRateLimiter.java`(41/64/74/79)

### KP-04 Redisson 分布式锁家族（RLock/互斥/公平/读写/自旋）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：分布式锁
- **来源**：docs §分布式锁 + redisson 源码验证
- **需求**：理解 Redisson 锁家族
- **自主实现**：若我设计——RLock 接口 + 多种锁实现
- **参考实现**（docs + redisson 源码）：**RLock 接口**——`extends Lock, RLockAsync`(28，集成 J.U.C Lock)；**RedissonLock**(互斥锁，支持重进入，未支持 Condition)；**RedissonFairLock**(公平锁，基于 **Redis zset** 排队)；**RedissonReadWriteLock**(读写锁，支持重进入，未支持 Condition，锁升级？)；**RedissonSpinLock**(自旋锁)
- **对比取舍**：**锁家族**——互斥/公平(zset)/读写/自旋；RLock 集成 J.U.C Lock
- **测试佐证**：源码 `redisson/.../api/RLock.java`(28)+`RedissonLock.java`+`RedissonFairLock.java`+`RedissonReadWriteLock.java`+`RedissonSpinLock.java`

### KP-05 Redisson 信号量（Semaphore/PermitExpirableSemaphore）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：信号量
- **来源**：docs §信号量 + redisson 源码验证
- **需求**：用 Redisson 实现分布式信号量
- **自主实现**：若我设计——RedissonSemaphore(Redis Key 存 Permit，acquire -1/release +1)
- **参考实现**（docs + redisson 源码）：**RedissonSemaphore**——基于 Redis Key 保存 Permit 状态，`acquire -1`/`release +1`；**RedissonPermitExpirableSemaphore**(可过期信号量，while(true) 轮询)
- **对比取舍**：**分布式信号量**——Permit 状态存 Redis；可过期版本支持过期
- **测试佐证**：源码 `RedissonSemaphore.java`+`RedissonPermitExpirableSemaphore.java`

### KP-06 分布式 Session（中心化 vs 去中心化）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Session
- **来源**：docs §分布式 Session
- **需求**：理解分布式 Session 两种实现
- **自主实现**：若我设计——中心化(有状态服务) vs 去中心化(Gossip 广播)
- **参考实现**（docs + 架构师）：**中心化实现**——基于有状态服务(类似 Redisson/Zookeeper/MySQL)，服务集群同步数据(共识算法/副本算法)；**去中心化实现**——各服务器存储相同状态，类似 Gossip 语义(广播)
- **对比取舍**：**中心化 vs 去中心化**——集中(Redis 等)；Gossip 广播(各节点一致，呼应第 23 节 Gossip)
- **测试佐证**：架构师 + 第 23 节 Gossip

### KP-07 Spring Session 分布式 Session（Redis Hash）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（Spring Session） | **置信度**：High
- **前置**：Servlet/Spring
- **来源**：docs §Spring Session
- **需求**：用 Spring Session + Redis 实现分布式 Session
- **自主实现**：若我设计——Filter 包装 HttpServletRequest → HttpServletRequestWrapper + Redis Hash 存 Session
- **参考实现**（docs + 架构师）：**Spring Session**——基于 Servlet API 搭配 **Redis Hash** 实现；**Filter 包装** HttpServletRequest → `HttpServletRequestWrapper`；**多级缓存**——HttpSession 容器实现(本地) + HttpSession Session 服务(远程)
- **对比取舍**：**Redis Hash 存 Session**——Filter 透明替换 HttpSession；多级缓存(本地+远程)
- **测试佐证**：docs 架构 + `[待验证]` spring-session 无本地源码

### KP-08 幂等性服务（HttpSession/Spring Session + Token）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：幂等、Token
- **来源**：docs §幂等性服务 + 架构师发散
- **需求**：基于 Session 实现服务幂等性
- **自主实现**：若我设计——Token 机制(首次生成/二次拦截)
- **参考实现**（docs + 架构师）：**基于 HttpSession/Spring Session 实现**；**AOP + Session**——Client 首次请求服务器生成 **Token** 返回；业务完成 Token 存一段时间(有效期)；二次请求传 Token 被**拦截失败**；**Token 透传**——HTTP 基于请求头，RPC 基于 RPC 元数据
- **对比取舍**：**Token 幂等**——防重复提交；Token 透传(HTTP 头/RPC 元数据)
- **测试佐证**：docs Token 机制 + 架构师

### KP-09 幂等性设计要点（Token + 去重）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-08
- **来源**：架构师发散
- **需求**：总结幂等性设计
- **自主实现**：若我设计——Token 预生成 + 消费去重
- **参考实现**（架构师）：幂等性设计——①**Token 模式**(首次生成、二次拦截) ②**唯一键去重**(业务唯一 ID/分布式锁) ③**状态机**(处理状态)；衔接第 17 节消费幂等
- **对比取舍**：**三种幂等方案**——Token/唯一键/状态机；与第 17 节消息消费幂等呼应
- **测试佐证**：架构师 + 第 17 节

### KP-10 分布式缓存实战要点（总结）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~09
- **来源**：架构师整合
- **需求**：总结分布式缓存实战场景
- **自主实现**：若我设计——限流/锁/会话/幂等四大场景
- **参考实现**（架构师整合）：实战四场景——①**限流**(RateLimiter 令牌桶 Lua) ②**分布式锁**(Redisson 家族) ③**分布式 Session**(Redis Hash) ④**幂等**(Token)——都基于 Redis/Redisson
- **对比取舍**：**Redis 实战基石**——四大场景建立在 Redis/Redisson 之上；深挖后续单独规划
- **测试佐证**：整合本篇 KP

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| J.U.C + AQS 预备 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 限流场景(四种) | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Redisson RateLimiter | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| Redisson 锁家族 | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| Redisson 信号量 | 分布式问题 | 核心 | P1 | 🟡 | 有效 | High |
| 分布式 Session(中心化/去中心) | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Spring Session(Redis Hash) | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| 幂等性服务(Token) | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 幂等设计要点 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 实战要点(总结) | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/redisson`(RLock/RedissonLock/FairLock/ReadWriteLock/SpinLock/Semaphore/PermitExpirableSemaphore/RateLimiter)——锁家族 + 限流全源码
- **关键源码类**（本次实证）：`RLock`(28，extends Lock)、`RedissonRateLimiter`(41/64/74/79)、`RedissonSemaphore`/`RedissonPermitExpirableSemaphore`
- **诚实标注**：spring-session 无本地独立源码；Redis/Redisson 深挖后续单独规划(第 27 节约定)
- **关联标注**：microsphere-redis `[待验证]`；衔接第 27 节缓存设计、第 8 节容错(熔断)、第 17 节幂等

---

## 五、本节小结（三层次视角）

**需求**：分布式缓存实战四场景——限流/分布式锁/分布式 Session/幂等性服务。

**自主实现核心**：若我设计——
1. 限流：RateLimiter(Lua 令牌桶)/Bulkhead/TimeLimiter/CircuitBreaker
2. 分布式锁：Redisson 家族(互斥/公平 zset/读写/自旋/信号量)
3. 分布式 Session：Redis Hash + Filter 包装
4. 幂等：Token 机制 + 唯一键去重

**参考实现**：redisson 源码(锁家族/RateLimiter 验证) + docs。

**对比取舍**：知识本体是"**分布式缓存实战**"。核心洞察：**限流/锁/会话/幂等四场景基于 Redis/Redisson**。**stage-2 docs 全部收官**。

**待验证汇总**：
- microsphere-redis 具体场景
- spring-session(无本地源码)

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为实战场景 + redisson 源码验证；补全聚焦"缓存实战的工程价值"。

### 完整认知：分布式缓存实战在真实架构中完整该讲什么

docs 覆盖了限流/锁/会话/幂等。作为架构师，这个主题完整还该包含：

1. **Redis/Redisson 是分布式实战基石**：限流/锁/会话/幂等四大场景都建立在 Redis/Redisson 上——第 27 节的实战落地
2. **限流四维度**：Bulkhead(并发数)/RateLimiter(频率)/TimeLimiter(时长)/CircuitBreaker(失败)——Resilience4j/Sentinel 分类(呼应 stage-1 第 8 节容错)
3. **Redisson 锁家族选型**：互斥(默认)/公平(zset 排队)/读写/自旋/信号量——按业务场景选
4. **分布式 Session**：中心化(Redis) vs 去中心化(Gossip)——会话一致性权衡(呼应第 23 节)
5. **幂等性**：Token 模式 + 唯一键 + 状态机——防重复提交/重复处理(呼应第 17 节消息幂等)
6. **AQS 分布式化**：J.U.C AQS → Redisson 分布式锁/信号量——本地并发到分布式的演进
7. **Spring Session**：Redis Hash 透明替换 HttpSession——会话外置化

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 限流类型 | 数量/频率/时长/失败 |
| 锁家族选型 | 互斥/公平/读写/自旋 |
| 中心化 vs 去中心化 Session | Redis 集中；Gossip 广播 |
| Token 幂等 | 防重复；Token 生命周期管理 |
| 本地 vs 分布式 Session | 单机简单；分布式可扩展 |

### 常见坑/反模式

1. **RateLimiter 非原子**：限流不用 Lua——并发超限
2. **锁不续期**：Redisson 看门狗/超时——防死锁
3. **Session 粘性**：本地 Session 集群失效——用 Spring Session Redis
4. **Token 无有效期**：幂等 Token 永不过期——设 TTL
5. **熔断与限流混淆**：CircuitBreaker(失败) vs RateLimiter(频率)——区分

### 生态位置

- **分布式问题维度**：分布式缓存实战是**stage-2 docs 收官**——承接第 27 节缓存设计、第 8 节容错、第 17 节幂等
- **衔接**：缓存设计(第 27 节) → 实战(本篇) → **stage-3 三高架构**(下一期)
- **与源码提取的关系**：redisson 锁家族/RateLimiter 是核心源码(深挖后续规划)

**架构师视角结论**：本篇不只是背四场景，而是"**理解 Redis/Redisson 的实战应用**"——限流(RateLimiter Lua)、分布式锁(Redisson 家族)、分布式 Session(Redis Hash)、幂等(Token)；这是第 27 节缓存设计的实战落地，也是 **stage-2 全部 28 篇 docs 的收官**。
