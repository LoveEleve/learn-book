# stage-2 · 第 18 节：TCC(Try-Confirm-Cancel) 分布式事务原理和实现 — 知识点提取

> 课程：stage-2 模式设计与实现 第 18 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/18. 第十八节：TCC(Try-Confirm-Cancel) 分布式事务原理和实现.md`
> 提取时间：2026-08-11 | 权重：核心（分布式事务方案，TCC 主线）

---

## 一、本节概览

- **技术域**：TCC（Try-Confirm-Cancel）分布式事务（概念/实现方案/通用 AOP 模式/Hmily 组件）
- **维度**：`[分布式问题]`（分布式事务/补偿）+ `[工程问题]`（AOP 拦截/事务处理器，源码级）
- **核心命题**：理解 TCC 方案——Try 预留/Confirm 确认/Cancel 取消，用业务补偿实现分布式事务
- **知识点数**：10 个
- **前置**：第 15 节 JTA、第 17 节可靠事件、第 14/16 节 Spring 事务/AOP

## 前置条件清单
读者需先掌握：
1. **2PC/XA**（第 15 节：TCC 是 2PC 的补偿变体）
2. **可靠事件/本地消息表**（第 17 节）
3. **Spring AOP**（TCC 实现依赖 AOP 拦截）
4. **分布式事务概念**
未达前置者，先补：第 15/17 节 + Spring AOP

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：TCC 概念/AOP 模式对照 seata TCC 源码讲
- **工程化弱**：TCC 实现方案/Hmily 组件补基础
- **必做**：对照 `code/spring/seata`(TCC 模块) 源码验证（docs 的 Hmily 无本地源码，用 seata 验证，08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 TCC 概念（Try-Confirm-Cancel）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：2PC、分布式事务
- **来源**：docs §简介·TCC 概念
- **需求**：用业务补偿(Confirm/Cancel)实现分布式事务，避免 2PC 资源锁
- **自主实现**：若我设计——Try 预留资源、Confirm 确认、Cancel 取消
- **参考实现**（docs）：**TCC**=Try、Confirm、Cancel 缩写，由 **Pat Helland 2007**《Life beyond Distributed Transactions》论文提出（原命名 Tentative-Confirmation-Cancellation）；正式用 Try-Confirm-Cancel 的是 **Atomikos**(注册 TCC 商标)；国内阿里程立 2008 引入
- **对比取舍**：**TCC 是 2PC 的补偿变体**——Try(预留)/Confirm(提交)/Cancel(回滚) 用业务逻辑实现，避免 2PC 锁资源
- **测试佐证**：seata TCC 源码(工业实现)

### KP-02 TCC 实现方案（Atomikos/TX LCN/TCC-transaction/Hmily/Seata）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]`（Seata/Hmily 当前主流） | **置信度**：High
- **前置**：KP-01
- **来源**：docs §TCC 实现
- **需求**：了解 TCC 各实现方案
- **自主实现**：若我设计——比较各 TCC 框架选型
- **参考实现**（docs）：TCC 实现方案——**Atomikos ExtremeTransactions**(商业，提供 TCC 实现但收费)、**TX LCN**(LCN Lock Control Notify，基于 Java 代理协调技术的分布式事务系统)、**TCC Transaction**(开源，微服务 TCC)、**Dromara Hmily**(高性能零侵入，TCC/TAC/XA)、**Seata**(开源主流)
- **对比取舍**：**选型**——Hmily/Seata 开源主流；Atomikos 商业收费；Seata 有本地源码验证
- **docs 作者观点（引用）**：docs 提到 ByteTCC/Hmily/TCC-transaction 等开源实现**"笔者都不推荐使用"**（作者保留态度，未展开理由）——选型时需结合团队技术栈与维护成本自行评估
- **测试佐证**：`code/spring/seata`(TCC 模块) 源码

### KP-03 TCC 通用实现模式（AOP 拦截）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring AOP
- **来源**：docs §TCC 实现模式·通用模式 + seata 源码验证
- **需求**：用 AOP 实现 TCC(拦截目标方法)
- **自主实现**：若我设计——注解绑定 confirm/cancel 方法，AOP 拦截执行
- **参考实现**（docs + seata 源码）：**基本思路 AOP**——拦截目标业务方法，用注解绑定 confirm/cancel；**Pointcut**(TCC 注解)/**JoinPoint**(拦截方法)/**Around** 模式——**Before Advice**(TCC 事务管理)/**Execution**(interceptedMethod.invoke)/**After Advice**(afterReturning→confirmMethod、afterThrowing→cancelMethod)；seata `TccActionInterceptorHandler extends AbstractProxyInvocationHandler`(48 行，拦截 try 方法)
- **对比取舍**：**AOP Around 拦截**——Before 开事务、正常→confirm、异常→cancel；seata TccActionInterceptorHandler 实现
- **测试佐证**：seata `tcc/.../interceptor/TccActionInterceptorHandler.java`(class 48)

### KP-04 Hmily 核心组件（自动装配/Aspect/拦截器）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（Hmily） | **置信度**：High
- **前置**：KP-02
- **来源**：docs §Hmily 核心组件
- **需求**：理解 Hmily TCC 的组件架构
- **自主实现**：若我设计——自动装配 + Aspect + 全局拦截器 + 事务处理器
- **参考实现**（docs）：Hmily 核心组件——`HmilyAutoConfiguration`(自动装配)、`AbstractHmilyTransactionAspect`(AOP Aspect)、`HmilyGlobalInterceptor`(全局拦截器)；链：`AbstractHmilyTransactionAspect → HmilyGlobalInterceptor → HmilyTransactionHandlerRegistry → HmilyTransactionHandler`；`HmilyTransactionContext`(事务上下文)、`HmilyTransactionHandlerRegistry`(处理器注册中心)
- **对比取舍**：**组件链**——Aspect→Interceptor→Registry→Handler 分层，按角色分发
- **测试佐证**：docs 组件 + seata TCC 对照

### KP-05 Hmily 事务处理器与角色（Handler/RoleEnum）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §事务处理器/角色枚举
- **需求**：理解 Hmily 按角色分发事务处理器
- **自主实现**：若我设计——按 HmilyRoleEnum 获取具体 Handler
- **参考实现**（docs）：**HmilyTransactionHandler** TCC 实现——`StarterHmilyTccTransactionHandler`(发起者，开启事务+状态存 HmilyRepositoryStorage)/`LocalHmilyTccTransactionHandler`(本地直接执行)/`ConsumeHmilyTccTransactionHandler`(消费端直接执行)/`ParticipantHmilyTccTransactionHandler`(参与方)；**HmilyRoleEnum**——START(发起者)/CONSUMER(消费者)/PARTICIPANT(参与者)/LOCAL(本地)/INLINE(内嵌 RPC)/SPRING_CLOUD；角色来自事务上下文 `HmilyTransactionContext.role`
- **对比取舍**：**角色驱动处理器**——按角色(发起/参与/消费)选 Handler
- **测试佐证**：docs 组件（Hmily 无本地源码，标注）

### KP-06 Hmily 事务执行器（preTry/preTryParticipant）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-05
- **来源**：docs §事务执行器 + 源码片段
- **需求**：理解 Try 阶段的执行流程
- **自主实现**：若我设计——preTry 创建事务/参与者，preTryParticipant 缓存参与方
- **参考实现**（docs + 源码片段）：
  - **preTry**(发起者 Try 预操作)：`createHmilyTransaction`(创建事务)→`HmilyRepositoryStorage.createHmilyTransaction`(存储)→`buildHmilyParticipant`(构建参与方,Role=START)→`registerParticipant`→存 ThreadLocal(`HmilyTransactionHolder`)、设 `HmilyTransactionContext`(Action=TRYING/Role=START/TransType=TCC)
  - **preTryParticipant**(参与者 Try 预操作)：`buildHmilyParticipant`(Role=PARTICIPANT)→`cacheHmilyParticipant`→`createHmilyParticipant`→`set Role=PARTICIPANT`
- **对比取舍**：**Try 预操作**——发起者创建全局事务，参与者缓存参与方；通过 HmilyTransactionContext 传递(远程)
- **测试佐证**：docs preTry/preTryParticipant 源码片段

### KP-07 Hmily 事务日志存储（HmilyRepositoryStorage/Repository）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-06
- **来源**：docs §事务日志存储
- **需求**：持久化事务状态
- **自主实现**：若我设计——HmilyRepositoryStorage 操作事务对象 + Disruptor 事件存储
- **参考实现**（docs）：`HmilyRepositoryStorage`(操作 HmilyTransaction，通过 **Disruptor** 发送事件，监听执行存储)；`HmilyRepository`(底层存储)；**MySQL 实现** `MysqlRepository`；表——`hmily_transaction_global`(全局)/`hmily_transaction_participant`(参与方)/`hmily_participant_undo`(UNDO)
- **对比取舍**：**Disruptor 异步存储**——事务状态持久化用 Disruptor 事件驱动；三表记录全局/参与方/UNDO
- **测试佐证**：docs 组件（Hmily 无本地源码）

### KP-08 Hmily 基本实现思路（角色分发 + confirm/cancel）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03/05
- **来源**：docs §基本实现思路
- **需求**：理解 Hmily TCC 完整实现思路
- **自主实现**：若我设计——AOP 拦截 + 按角色处理 + 成功 confirm/失败 cancel
- **参考实现**（docs）：基本思路——①Spring AOP 拦截数据相关业务方法 ②按角色选事务处理器 ③发起者创建事务上下文(作为日志存储/方法元信息传递：本地/远程 Feign/Dubbo/gRPC) ④上游参与者成功→confirm、失败→cancel ⑤参与者接受事务上下文(远程上下文获取)→更新+记录状态(三表)→本地成功 confirm/失败 cancel
- **对比取舍**：**事务上下文传播**——发起者→参与方传递上下文，按结果 confirm/cancel
- **测试佐证**：docs 思路 + seata TCC 对照

### KP-09 TCC 核心流程（Try→Confirm/Cancel）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01/03
- **来源**：架构师发散 + seata 源码验证
- **需求**：理解 TCC 两阶段(业务补偿)流程
- **自主实现**：若我设计——Try 预留、Confirm 提交、Cancel 补偿
- **参考实现**（docs + seata 源码）：**TCC 两阶段**——**Try**(预留/检查业务资源)、**Confirm**(提交，所有 Try 成功则执行)、**Cancel**(取消/补偿，任一 Try 失败则执行)；seata 实现——`TccActionInterceptorHandler`(拦截 try)、`TCCResourceManager`/`RMHandlerTCC`(分支事务)、`TccCore`(TCC 核心)
- **对比取舍**：**业务补偿 vs 2PC 锁**——TCC 用业务 Try/Confirm/Cancel 替代 2PC 的 XA 锁，无资源锁阻塞；代价是需写三方法(侵入)
- **测试佐证**：seata `tcc/.../interceptor/TccActionInterceptorHandler.java`/`TCCResourceManager.java`/`RMHandlerTCC.java`/`server/.../tcc/TccCore.java`

### KP-10 TCC vs 其他方案（对比 + 补充大纲衔接）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-09、第 15/17 节
- **来源**：架构师发散
- **需求**：对比 TCC 与 2PC/本地消息表/Saga
- **自主实现**：若我设计——按业务选方案
- **参考实现**（架构师）：方案对比——
  - **TCC**：业务补偿、强一致(最终)、侵入性(三方法)、无锁
  - **2PC/XA**：强一致、锁资源、阻塞（第 15 节）
  - **本地消息表/可靠事件**：最终一致、无侵入、需幂等（第 17 节）
  - **Saga**：补偿、长事务（Seata 第 19/20 节）
- **对比取舍**：**选型**——TCC 适合需业务补偿的强一致场景；最终一致场景用消息表/Saga；现代主流"避免强一致"(补充大纲)
- **测试佐证**：衔接第 15/17/19/20 节 + 补充大纲

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| TCC 概念 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| TCC 实现方案 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| 通用实现模式(AOP) | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Hmily 核心组件 | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| Hmily Handler/Role | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Hmily 事务执行器 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Hmily 事务存储 | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| Hmily 实现思路 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| TCC 核心流程 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| TCC vs 其他方案 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/seata`(tcc 模块)——`TccActionInterceptorHandler`(48)、`TCCResource`/`TCCResourceManager`/`RMHandlerTCC`、`server/.../tcc/TccCore`
- **诚实标注**：docs 的 Hmily 组件无本地源码，用 docs 组件说明 + seata TCC 对照验证
- **关联标注**：microsphere 用 TCC/Seata 分布式事务 `[待验证]`；衔接第 15 节 JTA、第 17 节可靠事件、第 19/20 节 Seata

---

## 五、本节小结（三层次视角）

**需求**：用 TCC（Try/Confirm/Cancel 业务补偿）实现分布式事务，避免 2PC 资源锁。

**自主实现核心**：若我设计——
1. TCC 概念：Try 预留/Confirm 确认/Cancel 取消
2. AOP 拦截：注解绑定 confirm/cancel，Around 拦截
3. 按角色分发处理器(发起/参与/消费)
4. 事务上下文传播(远程 Feign/Dubbo/gRPC)
5. 成功 confirm/失败 cancel + 事务日志存储

**参考实现**：seata TCC 源码(验证) + docs(Hmily 组件说明)。

**对比取舍**：知识本体是"**TCC 分布式事务方案**"。核心洞察：**Try/Confirm/Cancel 业务补偿、AOP 拦截、角色分发、事务上下文传播**。TCC 强一致但侵入性(三方法)。

**待验证汇总**：
- microsphere 用 TCC/Seata 的具体场景
- Hmily 源码(无本地，docs 组件说明)

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 TCC 概念/方案/Hmily 组件 + seata 源码验证；补全聚焦"TCC 的工程价值与选型"。

### 完整认知：TCC 在真实架构中完整该讲什么

docs 覆盖了概念/方案/AOP/Hmily。作为架构师，这个主题完整还该包含：

1. **TCC 是 2PC 的业务补偿变体**：Try 预留、Confirm/Cancel 业务补偿，避免 2PC 资源锁阻塞——但需业务写三方法(侵入性)
2. **Try 的"预留"语义**：Try 需预留资源(冻结/预占)，Confirm 真正提交，Cancel 释放——资源预留是 TCC 核心难点
3. **事务上下文传播**：跨服务(Feign/Dubbo/gRPC)传递 TCC 上下文，是分布式协调关键
4. **Hmily vs Seata**：Hmily 专注柔性事务(TCC/TAC)；Seata 支持 AT/TCC/SAGA/XA 多模式——Seata 更全面(本地有源码)
5. **TCC vs 其他方案选型**：TCC(强一致,侵入) vs 消息表(最终一致,无侵入) vs Saga(补偿,长事务)——按业务
6. **现代实践定位**：TCC 适合需业务补偿的强一致场景；但"避免强一致"的 Outbox/事件驱动是更主流取向(补充大纲)
7. **TCC 的幂等/悬挂/空回滚**：Try/Confirm/Cancel 需幂等，处理悬挂(超时)与空回滚——工程难点

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| TCC vs 2PC | TCC 业务补偿无锁；2PC 锁资源阻塞 |
| TCC vs 消息表 | TCC 强一致侵入；消息表最终一致无侵入 |
| Hmily vs Seata | Hmily 柔性专注；Seata 多模式全面 |
| 侵入性 | TCC 三方法侵入 vs AT 无侵入 |
| Confirm/Cancel 幂等 | 重复调用需幂等 |

### 常见坑/反模式

1. **Try 不预留资源**：Try 未真正预留，Confirm/Cancel 无法保证
2. **Confirm/Cancel 不幂等**：重复调用导致数据错——需幂等
3. **悬挂/空回滚处理**：超时/不确定需处理(悬挂、空回滚)
4. **上下文不传递**：跨服务未传 TCC 上下文，协调失败
5. **强一致误用 TCC**：最终一致场景可用消息表(更简单)

### 生态位置

- **分布式问题维度**：TCC 是**业务补偿型分布式事务**——承接 2PC(第 15 节)、可靠事件(第 17 节)，为 Seata(第 19/20 节)铺垫
- **衔接**：2PC(15) → 可靠事件(17) → TCC(本篇) → Seata(19/20) → 补充大纲(Outbox/事件驱动)
- **与源码提取的关系**：seata tcc 模块是核心源码

**架构师视角结论**：本篇不只是背 Try/Confirm/Cancel，而是"**理解业务补偿型分布式事务**"——TCC 用业务预留/确认/取消替代 2PC 锁、AOP 拦截 + 角色分发 + 上下文传播实现跨服务协调；强一致但侵入性强，现代主流是"避免强一致"的 Outbox/事件驱动。
