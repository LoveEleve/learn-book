# stage-2 · 第 16 节：Java 分布式事务整合 — 知识点提取

> 课程：stage-2 模式设计与实现 第 16 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/16. 第十六节：Java 分布式事务整合.md`
> 提取时间：2026-08-11 | 权重：核心（Spring 事务整合主线）

---

## 一、本节概览

- **技术域**：Spring Transaction 事务抽象整合（定义/AOP/管理器/JTA 整合/JDBC 事务模式）
- **维度**：`[工程问题]`（Spring 事务抽象/AOP）+ `[规范]`（JDBC/JTA 整合）+ `[分布式问题]`（分布式事务，弱）
- **核心命题**：理解 Spring 事务抽象如何整合 JDBC/JTA——TransactionDefinition、TransactionInterceptor、PlatformTransactionManager、JTA 整合
- **知识点数**：12 个
- **前置**：第 13/14 节 JDBC/Spring 事务、第 15 节 JTA、Spring AOP

## 前置条件清单
读者需先掌握：
1. **JDBC 本地事务**（第 13 节）
2. **Spring 事务**（第 14 节：TransactionInterceptor/传播）
3. **JTA/XA**（第 15 节：UserTransaction/XAResource）
4. **Spring AOP**（Pointcut/Advisor/Advice）
未达前置者，先补：第 13/14/15 节 + Spring AOP 基础

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：Spring 事务抽象/AOP/管理器直接对照 spring-framework 源码讲
- **工程化弱**：JTA 整合/XML 配置补基础
- **必做**：对照本地 `code/spring/spring-framework`(spring-tx) 源码验证（非只看 docs，08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Spring Transaction 事务抽象实现模式（行为定义）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring 事务、AOP
- **来源**：docs §Spring Transaction 事务抽象实现模式
- **需求**：定义 Spring 事务行为（边界/隔离/传播/回滚）
- **自主实现**：若我设计——定义事务边界(AOP 方法级)、隔离级别、传播行为、回滚策略
- **参考实现**（docs）：**定义事务边界**——Spring AOP 仅支持方法级代理，事务边界在某个 Bean 方法执行周期内；`ABean.method1()→BBean.method2()→...`(方法调用链)；**隔离级别**——JDBC Connection 常量；**传播行为**——来自 EJB 规范；**回滚策略**——默认拦截所有异常(`java.lang.Throwable`)
- **对比取舍**：**AOP 方法级边界**——事务边界=Bean 方法执行周期；传播继承 EJB 规范
- **测试佐证**：spring-tx 源码 + 第 14 节

### KP-02 Spring 事务隔离级别（TransactionDefinition 常量）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、JDBC 隔离
- **来源**：docs §定义 Transaction 隔离级别 + 源码验证
- **需求**：定义事务隔离级别常量
- **自主实现**：若我设计——映射 JDBC Connection 隔离级别
- **参考实现**（docs + 源码）：`TransactionDefinition` 隔离常量——`ISOLATION_DEFAULT(-1)`(默认，用底层存储)/`ISOLATION_READ_UNCOMMITTED(1)`/`ISOLATION_READ_COMMITTED(2)`/`ISOLATION_REPEATABLE_READ(4)`/`ISOLATION_SERIALIZABLE(8)`——与 `java.sql.Connection.TRANSACTION_*` 一致
- **对比取舍**：**映射 JDBC 隔离**——TransactionDefinition 隔离常量与 Connection 对齐
- **测试佐证**：源码 `TransactionDefinition.java`(ISOLATION 常量)

### KP-03 Spring 事务传播行为（Propagation 常量）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、第 14 节传播
- **来源**：docs §定义 Transaction 传播行为 + 源码验证
- **需求**：定义事务传播行为常量
- **自主实现**：若我设计——映射 EJB 传播属性
- **参考实现**（docs + 源码）：`TransactionDefinition` 传播常量——`PROPAGATION_REQUIRED(0)`(默认，有则复用无则新建)/`SUPPORTS(1)`/`MANDATORY(2)`/`REQUIRES_NEW(3)`(新事务挂起当前)/`NOT_SUPPORTED(4)`(非事务挂起)/`NEVER(5)`/`NESTED(6)`(嵌套 Savepoint，仅 JDBC 3.0)；来自 EJB 规范(CMT)
- **对比取舍**：**传播继承 EJB**——七种传播与 EJB 对齐；NESTED 是 Spring 新增(EJB 无)
- **测试佐证**：源码 `TransactionDefinition.java`(REQUIRED 52/REQUIRES_NEW 96/NOT_SUPPORTED 111/NESTED 132)

### KP-04 回滚策略（rollbackOn）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §定义回滚策略 + 源码验证
- **需求**：为应用提供回滚策略，提高弹性
- **自主实现**：若我设计——按异常类型决定是否回滚
- **参考实现**（docs + 源码）：`TransactionAttribute.rollbackOn(Throwable ex)`——判断是否回滚；默认拦截所有异常(`java.lang.Throwable`)；`TransactionAttribute` 继承 `TransactionDefinition`
- **对比取舍**：**异常驱动回滚**——rollbackOn 按异常类型决策；默认全拦截(可配置 rollbackFor)
- **测试佐证**：源码 `interceptor/TransactionAttribute.java`(rollbackOn)

### KP-05 Spring 事务实现策略（AOP 拦截）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring AOP
- **来源**：docs §Spring 事务实现策略 + 源码验证
- **需求**：用 AOP 拦截业务方法实现事务
- **自主实现**：若我设计——业务方法作为 Pointcut，前置/后置/围绕处理事务
- **参考实现**（docs）：业务方法属于 **Spring AOP Pointcut**(拦截条件)，必须来自被代理 Bean；**Pointcut+Advice=PointcutAdvisor**；Advice 拦截动作——**前置 before/后置 after(返回后/异常后)/围绕 round**；Spring 事务——前置准备事务、业务执行、后置 commit/rollback/生命周期回调
- **对比取舍**：**AOP 织入事务**——前置(开事务)/后置(提交或回滚)；Pointcut 匹配需代理 Bean 方法
- **测试佐证**：spring-aop + spring-tx 源码

### KP-06 Spring Transaction 定义（TransactionDefinition / DefaultTransactionDefinition）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02/03
- **来源**：docs §Spring Transaction 定义 + 源码验证
- **需求**：定义事务的核心接口与默认实现
- **自主实现**：若我设计——TransactionDefinition 接口 + DefaultTransactionDefinition 默认
- **参考实现**（docs + 源码）：`TransactionDefinition`(核心 API)、`DefaultTransactionDefinition`(默认实现)；核心方法——`getPropagationBehavior()`(默认 REQUIRED)/`getIsolationLevel()`(默认 ISOLATION_DEFAULT)
- **对比取舍**：**定义接口**——TransactionDefinition 描述事务属性(传播/隔离/超时/只读)
- **测试佐证**：源码 `TransactionDefinition.java`/`DefaultTransactionDefinition.java`

### KP-07 TransactionAttribute（传统 API + 属性来源）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-06
- **来源**：docs §TransactionAttribute + §属性来源 + 源码验证
- **需求**：定义事务属性与来源
- **自主实现**：若我设计——TransactionAttribute 继承 TransactionDefinition + TransactionAttributeSource 提供属性
- **参考实现**（docs + 源码）：`TransactionAttribute` 继承 `TransactionDefinition`，`DefaultTransactionAttribute` 默认；核心——`getQualifier()`(指定 TransactionManager Bean，对应 `@Transactional#value/transactionManager` 或 `<tx:advice transaction-manager>`)、`rollbackOn()`；**TransactionAttributeSource**(属性来源)——`NameMatchTransactionAttributeSource`(XML)/`AnnotationTransactionAttributeSource`(注解)/`CompositeTransactionAttributeSource`(组合)
- **对比取舍**：**属性来源可插拔**——XML/注解/组合三种来源
- **测试佐证**：源码 `interceptor/TransactionAttribute.java`/`AnnotationTransactionAttributeSource.java`/`CompositeTransactionAttributeSource.java`

### KP-08 Spring Transaction AOP 组件（TransactionInterceptor/Pointcut）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-05、AOP
- **来源**：docs §Spring Transaction AOP 组件 + 源码验证
- **需求**：拦截器 + 切点匹配事务方法
- **自主实现**：若我设计——TransactionInterceptor 拦截 + TransactionAttributeSourcePointcut 匹配
- **参考实现**（docs + 源码）：`TransactionInterceptor`(仅拦截 Spring Bean 方法，不做判断)；`TransactionAttributeSourcePointcut.matches`(判断——`tas.getTransactionAttribute(method, targetClass) != null`)；模板父类 `TransactionAspectSupport`；拦截接口 `MethodInterceptor`；`invoke`(367→调用 `invokeWithinTransaction` 342 行)
- **对比取舍**：**拦截器 + 切点分离**——Interceptor 拦截执行，Pointcut 判断是否事务方法
- **测试佐证**：源码 `interceptor/TransactionInterceptor.java`/`TransactionAttributeSourcePointcut.java`/`TransactionAspectSupport.java`(invokeWithinTransaction 342)

### KP-09 invokeWithinTransaction 核心流程
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-08
- **来源**：docs §invokeWithinTransaction + 源码验证
- **需求**：实现事务拦截的核心流程
- **自主实现**：若我设计——createTransactionIfNecessary → proceed → commit/rollback
- **参考实现**（docs + 源码）：`TransactionAspectSupport.invokeWithinTransaction`(342 行)——
  - `getTransactionAttributeSource().getTransactionAttribute`(取事务属性，null 则非事务)
  - `determineTransactionManager`(确定事务管理器)
  - `createTransactionIfNecessary`(374，开事务)
  - `invocation.proceedWithInvocation()`(执行)
  - 异常 `completeTransactionAfterThrowing`(384，回滚)
  - 正常 `commitTransactionAfterReturning`(416，提交)
- **对比取舍**：**前置开事务/异常回滚/正常提交**——事务拦截核心三部曲
- **测试佐证**：源码 `TransactionAspectSupport.java`(invokeWithinTransaction 342/createTransactionIfNecessary 623/completeTransactionAfterThrowing 708/commitTransactionAfterReturning 693)

### KP-10 createTransactionIfNecessary（TransactionInfo/ThreadLocal）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-09
- **来源**：docs §createTransactionIfNecessary + 源码验证
- **需求**：创建事务信息并绑定线程
- **自主实现**：若我设计——TransactionAttribute→PlatformTransactionManager→TransactionStatus→TransactionInfo→ThreadLocal
- **参考实现**（docs + 源码）：`createTransactionIfNecessary`(623 行)核心逻辑——①通过 TransactionAttribute 经 PlatformTransactionManager 转成 TransactionStatus ②用 TransactionInfo 包装(含 PlatformTransactionManager/TransactionStatus/joinpointIdentification) ③绑定到当前线程 **ThreadLocal**(`transactionInfoHolder.set(this)`，480-486 行)
- **对比取舍**：**ThreadLocal 绑定**——TransactionInfo 绑定当前线程，支持嵌套(保存 oldTransactionInfo)
- **测试佐证**：源码 `TransactionAspectSupport.java`(createTransactionIfNecessary 623/bindToThread 480)

### KP-11 Spring Transaction 管理器（AbstractPlatformTransactionManager 模板）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-09
- **来源**：docs §Spring Transaction 管理器 + 源码验证
- **需求**：定义事务管理器抽象（模板方法）
- **自主实现**：若我设计——AbstractPlatformTransactionManager 模板 + 子类实现 doBegin/doCommit
- **参考实现**（docs + 源码）：`AbstractPlatformTransactionManager`(模板)——`getTransaction`(final 373 行，TransactionInterceptor 调用)；**模板方法**——`doBegin`(abstract 1160，事务开始)/`doCommit`(abstract 1254，事务结束)；调用链——getTransaction→startTransaction→doBegin；commit→processCommit→doCommit
- **对比取舍**：**模板方法模式**——AbstractPlatformTransactionManager 定义骨架，子类(DataSource/Jta)实现 doBegin/doCommit
- **测试佐证**：源码 `support/AbstractPlatformTransactionManager.java`(getTransaction 373/doBegin 1160/doCommit 1254)

### KP-12 Spring 与 JTA 整合（JtaTransactionManager）+ JDBC 事务模式
- **维度**：`[工程问题]`+`[规范]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[过时→Seata/现代]`（JTA 相关） | **置信度**：High
- **前置**：KP-11、JTA(第 15 节)
- **来源**：docs §Spring Transaction 与 JTA 整合 + §JDBC 事务实现模式 + 源码验证
- **需求**：Spring 整合 JTA 实现分布式事务；理解 JDBC 事务模式
- **自主实现**：若我设计——JtaTransactionManager 实现 PlatformTransactionManager + UserTransaction JNDI
- **参考实现**（docs + 源码）：
  - **JtaTransactionManager**：实现 `TransactionManager`/`PlatformTransactionManager`，继承 `AbstractPlatformTransactionManager`；**UserTransaction JNDI**——`DEFAULT_USER_TRANSACTION_NAME="java:comp/UserTransaction"`(570 行)，`afterPropertiesSet`→`findUserTransaction`(JNDI 查找)；`doBegin`(`userTransaction.begin()` 646 行)、`doCommit`(676 行)
  - **JDBC 事务模式**：本地事务(setAutoCommit(false)→DML→commit/rollback)；XA/全局事务(XAResource start→DML→end→prepare→commit)；JTA 事务(UserTransaction begin→DML→commit)
  - **Atomikos 实现**：UserTransaction.begin 初始化 TM；AtomikosDataSourceBean 代理 DataSource 生成 Connection 代理；Connection.enlist 时关联 XAResource.start；commit 时 XAResource.end→prepare→commit
- **对比取舍**：**JTA 整合**——JtaTransactionManager 用 UserTransaction(JNDI) 管理分布式事务；JTA 相关 `[过时→Seata/现代]`(第 15 节)
- **测试佐证**：源码 `jta/JtaTransactionManager.java`(DEFAULT_USER_TRANSACTION_NAME 570/doJtaBegin 646/doCommit 676) + 第 15 节 JTA

> **docs 空节标注（穷尽性）**：docs §Spring Transaction 与 DataSource 整合（686 行）、§JTA 与 ShardingSphere 整合（744 行）仅标题无正文——DataSource 整合内容已在 KP-12 JDBC 事务模式覆盖；ShardingSphere 整合为分库分表场景的 JTA 分布式事务应用，见架构师补全生态位置。标注空节符合 08 §2。

> **管理器历史时间线**（docs §Spring Transaction 管理器 513 行）：`JtaTransactionManager`(2003-03-24 最早) → `AbstractPlatformTransactionManager`(2003-03-28 抽象) → `DataSourceTransactionManager`(2003-05-02)——JTA 管理器先于 DataSource 管理器，帮助抽象为 AbstractPlatformTransactionManager 模板（体现"先有分布式后抽象本地"的演进）。

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 事务抽象实现模式 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 事务隔离级别 | 规范 | 核心 | P1 | 🟡 | 时间无关 | High |
| 事务传播行为 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 回滚策略 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 事务实现策略(AOP) | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| TransactionDefinition | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| TransactionAttribute+来源 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| AOP 组件(Interceptor/Pointcut) | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| invokeWithinTransaction | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| createTransactionIfNecessary | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| AbstractPlatformTransactionManager | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| JTA 整合 + JDBC 模式 | 工程+规范 | 核心 | P1 | 🔴 | 过时→Seata/现代 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/spring-framework`(spring-tx)——TransactionDefinition/TransactionAttribute/TransactionAttributeSourcePointcut/TransactionAspectSupport/AbstractPlatformTransactionManager/JtaTransactionManager 全部验证
- **关键源码类**（本次实证）：`TransactionDefinition`(REQUIRED 52/REQUIRES_NEW 96/NOT_SUPPORTED 111/NESTED 132)、`TransactionAspectSupport.invokeWithinTransaction`(342)/createTransactionIfNecessary(623)/completeTransactionAfterThrowing(708)/commitTransactionAfterReturning(693)、`AbstractPlatformTransactionManager.getTransaction`(final 373)/doBegin(abstract 1160)/doCommit(abstract 1254)、`JtaTransactionManager`(DEFAULT_USER_TRANSACTION_NAME 570)
- **关联标注**：microsphere-spring 用 Spring 事务抽象 `[待验证]`；衔接第 14 节 Spring 事务、第 15 节 JTA、第 17 节起分布式事务方案

---

## 五、本节小结（三层次视角）

**需求**：Spring 事务抽象整合 JDBC/JTA——TransactionDefinition、TransactionInterceptor、PlatformTransactionManager、JTA 整合。

**自主实现核心**：若我设计——
1. TransactionDefinition 定义隔离/传播/回滚
2. AOP 拦截(Pointcut+Advice) 实现声明式事务
3. TransactionInterceptor → invokeWithinTransaction(开事务/执行/提交回滚)
4. createTransactionIfNecessary 绑定 ThreadLocal
5. AbstractPlatformTransactionManager 模板(doBegin/doCommit)
6. JtaTransactionManager 整合 JTA(UserTransaction JNDI)

**参考实现**：Spring 源码（`spring-tx` 完整验证）+ docs。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**Spring 事务抽象整合**"。核心洞察：**TransactionDefinition(隔离/传播)、AOP 声明式事务、TransactionInterceptor 拦截、AbstractPlatformTransactionManager 模板、JTA 整合**。为分布式事务方案(第 17-20 节)铺垫。

**待验证汇总**：
- microsphere-spring 用 Spring 事务的具体场景
- ShardingSphere 与 JTA 整合(docs 末段仅标题)

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Spring 事务抽象 + 源码片段 + 本地源码验证；补全聚焦"Spring 事务抽象的设计价值"。

### 完整认知：Spring 事务抽象在真实架构中完整该讲什么

docs 覆盖了定义/AOP/管理器/JTA 整合。作为架构师，这个主题完整还该包含：

1. **Spring 事务抽象是统一事务门面**：PlatformTransactionManager 抽象 JDBC/JTA/分布式，应用只需声明——屏蔽底层事务差异
2. **模板方法模式**：AbstractPlatformTransactionManager 定义骨架(getTransaction/commit)，子类(DataSource/Jta)只实现 doBegin/doCommit——扩展性设计
3. **AOP 声明式事务**：TransactionInterceptor + Pointcut 织入事务，业务无侵入——第 14 节的深入实现
4. **ThreadLocal 事务上下文**：TransactionInfo 绑定线程，支持嵌套——线程关联事务的核心
5. **JTA 整合的分布式能力**：JtaTransactionManager 用 UserTransaction(JNDI) 支持分布式事务——多数据源强一致
6. **与分布式事务演进**：Spring 本地事务(DataSource) → JTA(分布式) → Seata/事件驱动(现代)——事务边界选型
7. **ShardingSphere 整合**：docs 末段仅标题，ShardingSphere 可整合 JTA 做分布式事务(分库分表场景)

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 声明式(AOP) vs 编程式 | 无侵入；灵活 |
| TransactionDefinition | 隔离/传播/回滚统一描述 |
| 模板方法 vs 各实现 | 骨架统一；子类只实现关键 |
| 本地 vs JTA | 本地单库；JTA 分布式(重) |
| JTA vs Seata | JTA 强一致重；Seata 灵活 |

### 常见坑/反模式

1. **@Transactional 自调用失效**：同类内部调用不走代理(第 14 节)
2. **事务边界过长**：事务跨长操作，锁持有久——事务粒度控制
3. **忽略传播级别**：嵌套方法事务行为不符预期——按需选传播
4. **JTA 事务管理器未配置**：JtaTransactionManager 需 UserTransaction(容器提供)
5. **回滚策略不当**：检查异常默认不回滚——需 rollbackFor

### 生态位置

- **工程问题维度**：Spring 事务抽象是**声明式事务核心**——承接第 14 节、第 15 节 JTA，为分布式事务方案(第 17-20 节)铺垫
- **衔接**：Spring 事务(第 14 节) → JTA(第 15 节) → 事务整合(本篇) → 可靠事件/TCC/Seata(第 17-20 节)
- **与源码提取的关系**：spring-tx 是核心源码

**架构师视角结论**：本篇不只是背 Spring 事务 API，而是"**理解 Spring 事务抽象如何统一整合本地/JTA 事务**"——TransactionDefinition 定义、AOP 声明式拦截、AbstractPlatformTransactionManager 模板、JtaTransactionManager 分布式整合；这是 Java 声明式事务的完整实现，也是理解后续分布式事务方案的基石。
