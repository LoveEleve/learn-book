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
- **参考实现**：Sentinel `DegradeRule` + `DegradeSlot` + `CircuitBreaker`（熔断降级）；Resilience4j CircuitBreaker 用 sliding window（docs 提到"核心技术 - sliding window"）
- **对比取舍**：**核心都是 sliding window 统计 + 状态机(关/开/半开)**——思想一致
- **待验证**：Sentinel 熔断状态机可用源码验证

### KP-03 容错模式：限流（RateLimiter / flow）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **需求**：控制单位时间请求量，防流量突发打垮服务
- **自主实现**：计数器/滑动窗口/令牌桶/漏桶限流算法
- **参考实现**：Sentinel `FlowRule` + `flow` slot（QPS 限流）；Resilience4j RateLimiter
- **对比取舍**：Sentinel 的流控更丰富（QPS/线程数、并发控制）
- **待验证**：Sentinel 限流算法（计数器/滑动窗口）可用源码验证

### KP-04 容错模式：隔离（Bulkhead）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：KP-01
- **需求**：隔离故障，避免一个下游拖垮所有
- **自主实现**：线程池隔离/信号量隔离（舱壁模式）
- **参考实现**：Resilience4j Bulkhead；Sentinel 支持信号量隔离
- **对比取舍**：隔离粒度——线程池/信号量，防止级联故障
- **待验证**：Sentinel 隔离具体实现

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
| 熔断/降级 | 分布式 | 核心 | P1 | 🔴 | High |
| 限流 | 分布式 | 核心 | P1 | 🔴 | High |
| 隔离 | 分布式 | 支撑 | P2 | 🟡 | Medium |
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
1. 四种容错模式：熔断/限流/隔离/降级
2. 熔断：滑动窗口统计 + 状态机(关/开/半开)
3. 限流：QPS 控制
4. 整合 Web：Filter/Interceptor/AOP 接入请求链

**参考实现**：**Sentinel（主流 + 有源码）**——`code/spring/sentinel` 的 flow/degrade/circuitbreaker + microsphere-alibaba-sentinel 的 spring/web 多层整合。Resilience4j 不做对照（国内非主流，且与 Sentinel 思想一致）。

**对比取舍**：docs 标题是 Resilience4j，但按方法论 08 §1.3.1，参考实现选主流 Sentinel。**容错模式思想两者一致**，选型看生态。

**待验证汇总**：
- Sentinel 熔断状态机（可用源码验证）
- Sentinel 限流算法（计数器/滑动窗口）
- Sentinel 隔离实现
