# stage-1 · 第 8 节：Web 服务容错性 — 知识点提取

> 课程：stage-1 服务治理 第 8 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/08. 第八节：基于 Resilience4j 实现 Web 服务容错性.md`
> 提取时间：2026-08-09 | 权重：核心（容错是服务治理核心）
> **参考实现说明**：docs 标题为 Resilience4j，但按方法论 08 §1.3.1，国内容错主流是 **Sentinel**（`code/spring/sentinel` 有源码），故参考实现以 Sentinel 为主，不照搬标题。

---

## 一、本节概览

- **技术域**：Web 服务容错（熔断/限流/隔离/降级）整合到 Servlet/WebMVC/WebFlux
- **维度**：`[分布式问题]`（容错）+ `[工程问题]`（框架整合）
- **核心命题**：如何实现 Web 服务容错（熔断/限流/隔离），并整合到 Web 框架
- **知识点数**：6 个
- **前置**：熔断/限流概念、Servlet/WebMVC/WebFlux、函数式编程

## 前置条件清单
读者需先掌握：
1. **容错概念**（熔断/限流/隔离/降级）
2. **Servlet / Spring WebMVC / WebFlux**
3. **函数式接口**（Java 8）
4. **Sentinel 基本概念**（规则/资源）
未达前置者，先补：Sentinel 官方入门 + 熔断/限流原理

## 掌握度
目标读者：**本人（读源码多，Spring/并发熟悉）** — 已确认
讲解策略：容错机制 + Sentinel 整合直接讲（你熟悉）；补 Sentinel 规则/资源概念

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Web 服务容错需求与容错模式
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **需求**：Web 服务如何应对下游故障/流量突发，保证自身可用性
- **自主实现**：若设计容错，核心四种模式——**熔断(降级)** / **限流** / **隔离(舱壁)** / **重试**
- **参考实现**：Sentinel 提供 flow(限流)/degrade(熔断降级)/authority(黑白名单)/system(系统保护)；Resilience4j 提供 CircuitBreaker/Bulkhead/RateLimiter
- **对比取舍**：**两者容错模式思想一致**（熔断/限流/隔离）——都是这几种模式；选型看生态主流
- **测试佐证**：`code/spring/sentinel/sentinel-core/.../slots/block/` 有 flow/degrade/circuitbreaker

### KP-02 容错模式：熔断/降级（CircuitBreaker）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **需求**：下游故障时，快速失败而非拖垮自己
- **自主实现**：滑动窗口统计失败率 → 超阈值熔断 → 后续请求直接降级/快速失败 → 半开试探恢复
- **参考实现**：Sentinel `DegradeRule` + `DegradeSlot` + `CircuitBreaker`；Resilience4j CircuitBreaker 用 sliding window（docs 提到"核心技术 - sliding window"）
- **对比取舍**：**核心都是滑动窗口统计 + 状态机(CLOSED/OPEN/HALF_OPEN)**——思想一致
- **测试佐证**（已源码验证）：`code/spring/sentinel` 的 `circuitbreaker/AbstractCircuitBreaker` 含 `enum State{CLOSED,OPEN,HALF_OPEN}` + `fromClosedToOpen()`/`fromOpenToHalfOpen()`/`fromHalfOpenToOpen()`（compareAndSet 状态转换）

### KP-03 容错模式：限流（RateLimiter / flow）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **需求**：控制单位时间请求量，防流量突发打垮服务
- **自主实现**：计数器/滑动窗口/令牌桶/漏桶限流算法
- **参考实现**：Sentinel `FlowRule` + `flow` slot（QPS 限流）；Resilience4j RateLimiter
- **对比取舍**：Sentinel 的流控更丰富（QPS/线程数、并发控制、多种 Controller）
- **测试佐证**（已源码验证）：`code/spring/sentinel` 的 `flow/controller/` 有 DefaultController（默认计数）/ThrottlingController（排队）/WarmUpController（预热）等流控策略

### KP-04 容错模式：隔离（Bulkhead / 舱壁）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：KP-01
- **需求**：隔离故障，避免一个下游拖垮所有
- **自主实现**：线程池隔离/信号量隔离（舱壁模式）
- **参考实现**：Sentinel 支持信号量/线程数隔离（流控维度含线程数）；Resilience4j Bulkhead（非主流，仅概念）
- **对比取舍**：隔离粒度——线程池/信号量，防止级联故障
- **待验证**：Sentinel 隔离具体实现（流控的线程数模式）

### KP-04b 容错模式：重试（Retry）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：KP-01
- **需求**：瞬时故障（网络抖动）时重试，提高成功率
- **自主实现**：对可重试的失败(超时/瞬时错误)按退避策略重试
- **参考实现**：Spring Retry / Resilience4j Retry；Sentinel 不主打重试（重试常由调用方/重试框架做）
- **对比取舍**：重试与熔断配合——重试针对瞬时，熔断针对持续故障；重试需谨慎(避免雪崩)

### KP-05 容错整合到 Web 框架（Sentinel Servlet/WebMVC）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Servlet/WebMVC、Sentinel
- **需求**：把容错能力整合到 Web 请求处理链（Servlet/WebMVC/WebFlux）
- **自主实现**：在 Web 请求入口接入容错拦截——基于 Filter/Interceptor/Aspect
- **参考实现**：microsphere-alibaba-sentinel 把 Sentinel 接入 spring/web（`spring/web` 包）、mybatis、redis、druid 等；Sentinel 官方 Web Servlet Filter / WebMVC 拦截器
- **对比取舍**：整合方式——Servlet Filter（全局）/ WebMVC Interceptor（Web）/ AOP（方法级）
- **测试佐证**：microsphere-alibaba-sentinel 有 `spring/web`、`mybatis`、`redis`、`druid` 包 = 多层整合实证

### KP-06 Sentinel 规则/资源核心概念
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Sentinel 概念
- **需求**：理解 Sentinel 的规则与资源模型
- **自主实现**：定义资源(入口) → 绑定规则(限流/熔断) → 动态生效
- **参考实现**：Sentinel `RuleManager` + `AbstractRule`（FlowRule/DegradeRule/AuthorityRule 等）；microsphere 封装 `common/constants` 等
- **对比取舍**：规则(rule) + 资源(entry) + 槽(slot) 是 Sentinel 核心模型
- **待验证**：可用 sentinel-core 源码验证 RuleManager/slot 链

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| 容错需求与模式 | 分布式 | 核心 | P1 | 🔴 | High |
| 熔断/降级 | 分布式 | 核心 | P1 | 🔴 | High（已源码验证状态机） |
| 限流 | 分布式 | 核心 | P1 | 🔴 | High（已源码验证 Controller） |
| 隔离 | 分布式 | 支撑 | P2 | 🟡 | Medium |
| 重试 | 分布式 | 支撑 | P3 | 🟢 | Medium |
| Web 整合 | 工程 | 核心 | P1 | 🟡 | High |
| 规则/资源模型 | 工程 | 核心 | P1 | 🟡 | High |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **Sentinel 整合**：microsphere-alibaba-sentinel 的 spring/web、mybatis、redis、druid 包 = 多层整合实证
- **Sentinel 源码**：`code/spring/sentinel` 有 flow/degrade/circuitbreaker
- **Resilience4j**：microsphere-resilience4j 存在，但按主流性不深挖（国内用 Sentinel 为主）

---

## 五、本节小结（三层次视角）

**需求**：实现 Web 服务容错（熔断/限流/隔离/降级），整合到 Web 框架。

**自主实现核心**：若我设计——
1. 四种容错模式：熔断/限流/隔离/重试
2. 熔断：滑动窗口统计 + 状态机(CLOSED/OPEN/HALF_OPEN)
3. 限流：QPS 控制 + 多种 Controller 策略
4. 整合 Web：Filter/Interceptor/AOP 接入请求链

**参考实现**：**Sentinel（主流 + 有源码）**——`code/spring/sentinel` 的熔断状态机(CLOSED/OPEN/HALF_OPEN)与限流 Controller(Default/Throttling/WarmUp)已源码验证 + microsphere-alibaba-sentinel 的 spring/web 多层整合。Resilience4j 不做对照（国内非主流，且与 Sentinel 思想一致）。

**对比取舍**：docs 标题是 Resilience4j，但按方法论 08 §1.3.1，参考实现选主流 Sentinel。**容错模式思想两者一致**，选型看生态。

**待验证汇总**：
- Sentinel 隔离具体实现（流控的线程数模式）

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：本节大部分为架构师发散(docs 仅 23 行标题)；与 docs/前篇重复处已交叉引用。docs 内容极少，本节尤需发散。

### 完整认知：Web 服务容错在真实架构中完整该讲什么

docs 只有"Resilience4j 基础 + 整合"标题（23 行）。作为架构师，这个主题完整还该包含：

1. **完整容错体系**：不只熔断/限流，而是**熔断/降级/限流/隔离/重试/超时/热点防护/系统自适应保护**的组合——Sentinel 的 slots 链（flow/degrade/authority/system/hot-param 等）
2. **雪崩防护**：容错的核心目标是**防止级联故障（雪崩）**——下游故障 → 熔断 → 快速失败 → 上游不被拖垮；这是容错的架构级意义
3. **熔断状态机细节**：CLOSED→OPEN→HALF_OPEN 转换（已源码验证 KP-02）、滑动窗口统计、熔断恢复策略（探测/冷却时间）
4. **降级策略**：熔断后的降级（返回默认值/缓存/兜底逻辑），与第 3 节统一响应(业务码)衔接
5. **限流算法全谱**：计数器/滑动窗口/令牌桶/漏桶，Sentinel 的多种 Controller（已源码验证 KP-03），以及热点参数限流
6. **规则治理**：规则配置(静态 vs 动态/Nacos 推送，衔接第 10 节配置动态变更)、规则持久化、规则生效链路
7. **与负载均衡/网关配合**：容错在客户端(第 4 节)、网关(第 19 节)、服务端的落地位置

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 熔断 vs 限流 | 熔断针对"下游故障"(自我保护)；限流针对"自身流量"(防突发)——两者都要 |
| 熔断粒度 | 按资源/接口/下游服务粒度熔断——粒度细(精准)但规则多，粗(简单)但误伤 |
| 隔离：线程池 vs 信号量 | 线程池隔离彻底(独立线程)但资源占用高；信号量轻量但共享线程(见 KP-04) |
| 重试 vs 熔断 | 重试针对瞬时故障(提高成功率)；熔断针对持续故障(快速失败)——配合用，重试要退避+熔断门(见 KP-04b) |
| 规则静态 vs 动态 | 静态简单但改规则要重启；动态(配置中心推送)灵活(第 10 节) |
| 限流算法 | 计数器简单但毛刺；滑动窗口平滑；令牌桶允许突发；漏桶匀速——按场景选 |

### 常见坑/反模式

1. **熔断阈值拍脑袋**：失败率阈值设太敏感导致误熔断，或太迟钝失效——要结合压测/历史数据
2. **无降级兜底**：只熔断不降级，熔断后直接 500——应配降级(默认值/缓存)
3. **重试放大雪崩**：无退避/无熔断门地重试，下游故障时重试风暴拖垮一切(见 KP-04b + 第 4 节客户端)
4. **限流误伤正常流量**：阈值不当、热点误判，把正常用户限了——要精细规则 + 动态调整
5. **规则不持久化**：规则存在内存，重启丢失——要持久化到配置中心(第 10 节)
6. **忽略系统保护**：只做接口限流，没做系统级自适应保护(CPU/负载过高全局限流)——Sentinel 的 system 规则
7. **容错不覆盖全链路**：只在网关做容错，客户端/服务端没做——要全链路(客户端+网关+服务端)

### 生态位置

- **服务治理核心**：容错是"三高"(高可用)的关键，与负载均衡(第 11/12 节)、网关(第 19 节)、可观测(第 13 节)协同
- **Sentinel(国内主流)**：`code/spring/sentinel` 有源码验证；microsphere-alibaba-sentinel 接入 mybatis/redis/druid/web(第 9 节整合)
- **与第 4 节(客户端)、第 7 节(容器限流)、第 9 节(整合第三方)**衔接
- **雪崩防护**是容错的架构级价值——连接负载均衡/网关形成完整保护链

**架构师视角结论**：本篇不只是"用 Sentinel 的熔断/限流 API"，而是"**设计完整的容错保护体系防雪崩**"——熔断/限流/隔离/降级/重试/系统保护组合，全链路落地（客户端+网关+服务端），是"三高"架构的核心。
