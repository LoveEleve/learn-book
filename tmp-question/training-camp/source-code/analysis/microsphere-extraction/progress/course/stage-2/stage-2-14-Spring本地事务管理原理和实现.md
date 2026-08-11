# stage-2 · 第 14 节：Spring 本地事务管理原理和实现 — 知识点提取

> 课程：stage-2 模式设计与实现 第 14 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/14. 第十四节：Spring 本地事务管理原理和实现.md`
> 提取时间：2026-08-11 | 权重：核心（Spring 事务主线）

---

## 一、本节概览

- **技术域**：Spring 本地事务管理（Java Beans/EJB 背景 + Spring AOP + TransactionInterceptor + 传播机制）
- **维度**：`[规范]`（Java Beans/EJB 背景）+ `[工程问题]`（Spring 事务/AOP/传播）+ `[分布式问题]`（事务，弱）
- **核心命题**：理解 Spring 事务管理——Java Beans/EJB 背景、Spring AOP、TransactionInterceptor、事务传播机制
- **知识点数**：9 个
- **前置**：第 13 节 JDBC 本地事务、Spring AOP、事务概念

## 前置条件清单
读者需先掌握：
1. **JDBC 本地事务**（第 13 节：ACID/Connection 事务）
2. **Spring AOP**（MethodInterceptor/代理）
3. **事务传播概念**
4. **Java Beans/EJB 背景**（了解）
未达前置者，先补：第 13 节 + Spring AOP 基础

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：Spring 事务/AOP/传播直接对照 spring-framework 源码讲
- **工程化弱**：Java Beans/EJB 背景、XML 配置补基础
- **必做**：对照本地 `code/spring/spring-framework`(spring-tx) 源码验证（非只看 docs，08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Java Beans 自省（BeanInfo/PropertyDescriptor）
- **维度**：`[规范]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Java Reflection
- **来源**：docs §Java Beans 自省
- **需求**：运行时获取 Bean 元信息
- **自主实现**：若我设计——基于 Reflection + Reference 的元信息 API
- **参考实现**（docs）：Java Beans 元信息 API 基于 **Java Reflection + Reference** 综合运用——`BeanInfo`(Bean 元信息)、`PropertyDescriptor`(属性元信息)、`MethodDescriptor`(方法元信息)
- **对比取舍**：**反射自省**——运行时发现 Bean 结构，是 IoC/框架的基础
- **测试佐证**：JDK `java.beans` 包

### KP-02 Java Beans 容器管理（BeanContext）
- **维度**：`[规范]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：IoC 概念
- **来源**：docs §Java Beans 容器管理
- **需求**：Bean 容器管理（类似 IoC）
- **自主实现**：若我设计——BeanContext 类似 IoC 容器（无 DI）
- **参考实现**（docs）：`java.beans.beancontext.BeanContext`——类似 Java Beans IoC 容器（**没有提供依赖注入**）
- **对比取舍**：**BeanContext vs Spring IoC**——BeanContext 无 DI，Spring IoC 完整
- **测试佐证**：JDK `java.beans.beancontext` 包

### KP-03 Java Beans 钝化（.ser 序列化）
- **维度**：`[规范]` | **权重**：`[边缘]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：序列化
- **来源**：docs §Java Beans 钝化
- **需求**：Bean 状态持久化
- **自主实现**：若我设计——序列化到 .ser
- **参考实现**（docs）：Java Beans 钝化，媒体类型 **.ser**(序列化文件)
- **对比取舍**：**.ser 序列化**——Java 原生序列化持久化 Bean 状态
- **测试佐证**：Java 序列化机制

### KP-04 EJB 规范（背景——提取被 Spring 继承的模式）
- **维度**：`[规范]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[过时→Spring/CDI]`（EJB 过时；提取底层模式） | **置信度**：Medium
- **前置**：EJB 概念（背景）
- **来源**：docs §EJB 规范
- **需求**：**理解 EJB 承载的"时间无关模式"**——这些模式被 Spring 轻量继承，而非记住 EJB 本身（04 SOP：过时工具→提炼模式）
- **自主实现**：若我设计——提炼"容器管理事务、事务传播、组件生命周期管理"三大模式，看 Spring 如何轻量落地
- **参考实现**（docs 背景 + 架构师模式提炼）：
  - **模式一：容器管理事务(CMT)**——EJB 由容器声明式管理事务 → Spring 用 AOP 声明式事务(TransactionInterceptor)轻量继承（本篇 KP-06）
  - **模式二：事务传播机制**——EJB 定义事务传播 → Spring `Propagation` 七种传播级别继承（本篇 KP-07）
  - **模式三：组件生命周期/状态管理**——EJB 会话 Bean(有状态 Statefull/无状态 Stateless)、实体 Bean(Entity)、消息驱动 Bean 由容器管理生命周期 → Spring IoC/容器管理 Bean 生命周期轻量继承
- **对比取舍**：**EJB 是重量级过时规范**——`[过时→Spring/CDI]`；**关键是提炼模式**：CMT→Spring AOP、事务传播→Propagation、组件管理→Spring IoC。EJB 只是历史例证，非知识本体（08 反模式纠正）
- **测试佐证**：Spring 事务/AOP/IoC 源码（本篇 KP-05/06/07）

### KP-05 Spring 事务管理 + Spring AOP 架构
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring AOP
- **来源**：docs §Spring 事务管理 + §Spring AOP 架构
- **需求**：用 AOP 实现声明式事务
- **自主实现**：若我设计——用 AOP 拦截方法，动态管理事务
- **参考实现**（docs）：Spring 事务管理基于 **Spring AOP 架构**；通过 AOP 拦截目标方法，在方法执行前后管理事务
- **对比取舍**：**声明式事务( AOP)**——切面织入事务逻辑，业务无侵入；对比编程式(TransactionTemplate)
- **测试佐证**：spring-tx + spring-aop 源码

### KP-06 Spring 事务拦截器（TransactionInterceptor）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring AOP、MethodInterceptor
- **来源**：docs §Spring 事务实现原理·拦截器 + 源码验证
- **需求**：用拦截器拦截方法执行事务
- **自主实现**：若我设计——TransactionInterceptor 实现 MethodInterceptor，before/proceed/after
- **参考实现**（docs + 源码）：`TransactionInterceptor` 实现 **MethodInterceptor**(55 行)，拦截方法，显式执行 `MethodInvocation#proceed()`——存在**方法执行前后拦截**：`before` → `proceed` → `after`；核心方法 `invoke(MethodInvocation)`(112 行)，内部 `createTransactionIfNecessary`(开事务)/`completeTransactionAfterThrowing`(异常回滚)/`cleanupTransactionInfo`(清理)
- **对比取舍**：**前后拦截**——proceed 前开事务、后提交/回滚；是声明式事务的核心
- **测试佐证**：源码 `interceptor/TransactionInterceptor.java`(class 55/invoke 112)

### KP-07 事务传播机制（Propagation 枚举）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：事务传播
- **来源**：docs §Spring 事务传播机制 + 源码验证
- **需求**：定义事务的传播行为
- **自主实现**：若我设计——按传播级别决定新开/复用/挂起事务
- **参考实现**（docs + 源码）：`Propagation` 枚举（`TransactionDefinition.PROPAGATION_*`）——
  - `REQUIRED`(37 行，默认，有则复用无则新建)
  - `SUPPORTS`(50 行，有则用无则非事务)
  - `MANDATORY`(56 行，必须有否则异常)
  - `REQUIRES_NEW`(68 行，总是新开挂起当前)
  - `NOT_SUPPORTED`(80 行，非事务挂起当前)
  - `NEVER`(86 行，必须无否则异常)
  - `NESTED`(97 行，嵌套 Savepoint)
- **对比取舍**：**七种传播级别**——REQUIRED 默认、REQUIRES_NEW 隔离、NESTED 部分回滚；继承 EJB 传播思想
- **测试佐证**：源码 `annotation/Propagation.java`(REQUIRED 37/SUPPORTS 50/MANDATORY 56/REQUIRES_NEW 68/NOT_SUPPORTED 80/NEVER 86/NESTED 97)

### KP-08 NOT_SUPPORTED 传播（挂起同步）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-07
- **来源**：docs §NOT_SUPPORTED 级别 + 源码验证
- **需求**：理解 NOT_SUPPORTED 的挂起逻辑
- **自主实现**：若我设计——挂起当前事务同步，方法在非事务下执行
- **参考实现**（docs + 源码）：NOT_SUPPORTED **before 操作** `doSuspendSynchronization()`(701 行)——`getSynchronizations`(获取同步)、逐个 `synchronization.suspend()`(挂起)、`clearSynchronization`(清理)；`TransactionSynchronizationManager` 管理同步
- **对比取舍**：**挂起同步**——NOT_SUPPORTED 挂起当前事务同步，方法在无事务下执行
- **测试佐证**：源码 `support/AbstractPlatformTransactionManager.java`(doSuspendSynchronization 701/getSynchronizations 703/clearSynchronization 707)+`TransactionSynchronizationManager.java`

### KP-09 Spring 事务配置（XML Schema + @Reference 示例）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[有效]`（Spring 当前） | **置信度**：Medium
- **前置**：Spring 配置
- **来源**：docs §NOT_SUPPORTED 末段（XML Schema + @Reference）
- **需求**：配置 Spring 事务
- **自主实现**：若我设计——XML/注解声明事务
- **参考实现**（docs）：XML Schema 配置事务；`@Reference private UserService userService`(示例)；Spring 事务可用注解 `@Transactional` + XML `<tx:advice>`
- **对比取舍**：**声明式配置**——注解/XML 声明事务边界；docs 骨架节，架构师发散补全
- **测试佐证**：spring-tx 配置 + `@Transactional`

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Java Beans 自省 | 规范 | 支撑 | P2 | 🟡 | 时间无关 | High |
| Java Beans 容器(BeanContext) | 规范 | 支撑 | P3 | 🟢 | 时间无关 | High |
| Java Beans 钝化 | 规范 | 边缘 | P3 | 🟢 | 时间无关 | Medium |
| EJB 规范 | 规范 | 支撑 | P2 | 🟡 | 过时→Spring/CDI | Medium |
| Spring 事务 + AOP | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| TransactionInterceptor | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 事务传播机制(Propagation) | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| NOT_SUPPORTED(挂起) | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| Spring 事务配置 | 工程问题 | 支撑 | P3 | 🟢 | 有效 | Medium |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/spring-framework`(spring-tx)——TransactionInterceptor/Propagation/AbstractPlatformTransactionManager/TransactionSynchronizationManager
- **关键源码类**（本次实证）：`interceptor/TransactionInterceptor`(class 55/invoke 112)、`annotation/Propagation`(REQUIRED 37/.../NESTED 97)、`support/AbstractPlatformTransactionManager.doSuspendSynchronization`(701)
- **关联标注**：microsphere-spring 用 Spring 事务抽象 `[待验证]`；衔接第 13 节 JDBC、第 15 节 JTA

---

## 五、本节小结（三层次视角）

**需求**：用 Spring AOP 实现声明式本地事务管理。

**自主实现核心**：若我设计——
1. Java Beans/EJB 背景(过时)
2. Spring AOP 拦截方法
3. TransactionInterceptor(before/proceed/after) 管理事务
4. 传播机制：REQUIRED/REQUIRES_NEW/NOT_SUPPORTED/NESTED 等七种
5. NOT_SUPPORTED 挂起同步

**参考实现**：Spring 源码（`spring-tx` 完整验证）+ docs。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**Spring 本地事务管理**"。核心洞察：**Spring AOP + TransactionInterceptor 前后拦截、七种传播机制、NOT_SUPPORTED 挂起**。为第 15 节 JTA/XA 铺垫。

**待验证汇总**：
- microsphere-spring 用 Spring 事务的具体场景
- EJB 事务传播细节(背景，过时)

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为骨架(Java Beans/EJB 背景 + Spring 事务要点) + 源码验证；docs 内容较少，补全重要（08 §2）。

### 完整认知：Spring 事务在真实架构中完整该讲什么

docs 覆盖了 Java Beans/EJB 背景 + Spring 事务要点。作为架构师，这个主题完整还该包含：

1. **Spring 事务是 EJB 的轻量替代**：EJB 重量级容器管理事务被 Spring 声明式事务(轻量/POJO)取代——理解这段演进才懂 Spring 事务的定位
2. **AOP 声明式事务是核心**：TransactionInterceptor 拦截方法(before/proceed/after)，业务无侵入——声明式 vs 编程式(TransactionTemplate)权衡
3. **传播机制的实际场景**：REQUIRED(默认,嵌套方法共用)、REQUIRES_NEW(独立事务,如日志)、NESTED(部分回滚,Savepoint)、NOT_SUPPORTED(挂起)——生产选型关键
4. **事务边界与 AOP 自调用陷阱**：同类内部调用不走代理、@Transactional 失效——生产高频坑
5. **本地事务 vs 分布式事务**：Spring 本地事务(单数据源)；多数据源需 JTA/分布式(第 15 节起)——事务边界架构
6. **TransactionSynchronizationManager**：管理事务同步(资源绑定/同步回调)，是事务与资源(连接)绑定的核心
7. **Java Beans 自省 → Spring IoC**：Java Beans 元信息 API 是 IoC/框架的基础，Spring 深入运用

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 声明式(AOP) vs 编程式 | 声明式无侵入；编程式灵活 |
| REQUIRED vs REQUIRES_NEW | 复用事务；独立新事务 |
| NESTED vs REQUIRES_NEW | 部分回滚(Savepoint)；完全独立 |
| 传播机制 | 按业务嵌套/挂起/隔离需求选 |
| EJB vs Spring 事务 | EJB 重量级；Spring 轻量 |

### 常见坑/反模式

1. **@Transactional 自调用失效**：同类方法内部调用不走代理——需事务边界分离或自注入
2. **传播级别误用**：REQUIRES_NEW 滥用导致事务过多；NOT_SUPPORTED 挂起当前——按需选
3. **运行时异常才回滚**：默认只回滚 RuntimeException/Error——检查异常需 rollbackFor
4. **忽略事务边界**：事务跨长操作，锁持有久——事务粒度控制
5. **多数据源混用本地事务**：单数据源本地事务无法跨库——需分布式(第 15 节)

### 生态位置

- **工程问题维度**：Spring 事务是**声明式事务核心**——承接第 13 节 JDBC、为第 15 节 JTA、microsphere-spring 铺垫
- **衔接**：JDBC 本地事务(第 13 节) → Spring 事务(本篇) → JTA/XA(第 15 节) → 分布式事务(第 16-20 节)
- **与源码提取的关系**：spring-tx(TransactionInterceptor/AbstractPlatformTransactionManager) 是核心源码

**架构师视角结论**：本篇不只是背传播级别，而是"**理解 Spring 声明式事务的机制**"——AOP + TransactionInterceptor 前后拦截、七种传播机制、NOT_SUPPORTED 挂起同步；这是 Java 声明式事务的主流实现，也是理解分布式事务(多数据源)的起点。
