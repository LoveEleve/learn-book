# 13-03：与官方/18/07/14 的对照与生态定位

> **核心命题**：13-01 讲"怎么实现"、13-02 讲"怎么进 Spring"，本篇回答"为什么这么设计、在生态里站哪"：与官方 Plugin 的动态代理对照（两套机制的适用场景）、与 07-sentinel 的**跨项目集成完整闭环**（README 集成声明的实证）、与 18-dynamic 的 Mapper 扫描共存问题、与 14-druid 的"两个层级拦截"对照，最后给出生产评估。

---

## 一、与官方 Plugin 的对照（两套机制的适用场景）

### 1.1 机制对照

| 维度 | 官方 Plugin | microsphere ExecutorFilter |
|------|-------------|---------------------------|
| 拦截载体 | JDK 动态代理（`Plugin implements InvocationHandler`，Plugin.java:32） | 直接替换 Executor 对象（无代理） |
| 匹配粒度 | 方法级（`@Signature` 精确到方法签名） | 操作级（Executor 接口 10 类） |
| 拦截目标 | 4 类（Executor/ParameterHandler/ResultSetHandler/StatementHandler） | 仅 Executor |
| 干预能力 | `intercept()` 自管 proceed | Filter 责任链（短路/改参/改 boundSql） |
| 观察能力 | 需自己写（intercept 里做） | Interceptor 内建（before/after + result/failure） |
| 排序 | 无 | Prioritized |
| 异常 | 插件异常直接传播 | Interceptor 异常隔离（warn） |
| 扩展生态 | 官方插件遍地（分页/审计/加密） | 生态组件需实现 ExecutorFilter |

### 1.2 适用场景（选哪套）

| 场景 | 推荐 | 理由 |
|------|------|------|
| 精确拦截某个具体方法 | **官方** | 方法级匹配是官方独有的 |
| 需要 ParameterHandler/StatementHandler 层干预 | **官方** | microsphere 管道覆盖不到（13-01 2.1） |
| SQL 执行前后的横切（限流/监控/审计/降级） | **microsphere** | 操作分类 + 优先级 + 异常隔离，管道语义干净 |
| 多插件协作（先限流再监控） | **microsphere** | Prioritized 排序 + Interceptor 纯观察不干扰 |

**一句话**：官方是"手术刀"（精确但每刀都要自己管），microsphere 是"流水线"（标准化但只覆盖 Executor 层）——**两者不冲突，可共存**（官方插件继续用 `@Intercepts`，microsphere 的 `InterceptingExecutorInterceptor` 也是官方 Interceptor 链上的一环）。**但嵌套顺序有语义**：`interceptorChain.pluginAll` 按 `addInterceptor` 顺序执行——**先 add 的在外层先执行**（若 PageHelper 先注册：`InterceptingExecutor(PageHelperProxy(CachingExecutor(SimpleExecutor)))`——PageHelper 代理先于管道执行；反过来则管道先执行）。混合使用官方插件与 microsphere 管道时，**执行顺序由注册顺序决定**。

---

## 二、跨项目集成实证：Sentinel × MyBatis（完整闭环）

README 声称 "Integrate with other Microsphere projects like Sentinel"——`microsphere-alibaba-sentinel-mybatis` 插件是完整证据：

### 2.1 接入链路（四步，全部源码验证）

```
① 组件：SentinelMyBatisExecutorFilter（AbstractSentinelPlugin + ExecutorFilter）
     update/query → doInSentinel(ms, ...) → SentinelTemplate.call(resourceName, ...)
     资源名 = MappedStatement.getId()（Mapper 方法全限定名）→ 按方法粒度限流

② 注册：SentinelMyBatisAutoConfiguration
     @ConditionalOnSentinelAvailable + @ConditionalOnMyBatisAvailable + 类存在检查
     AutoConfigureAfter：SCA Sentinel / MyBatis SB / **MyBatis Plus** / microsphere MyBatis
     内嵌 @EnableMyBatisExtension Config 类 + @Bean SentinelMyBatisExecutorFilter

③ 发现：@EnableMyBatisExtension 三源扫描（13-02）→ 找到 ExecutorFilter bean

④ 装配：管道组装（13-01）→ SentinelMyBatisExecutorFilter 进入 ExecutorFilterChain
```

**闭环的关键**：Sentinel 侧**只需两件事**——实现 `ExecutorFilter` 接口 + 注册一个 bean。管道语义（优先级、异常隔离、生命周期）全部由 microsphere-mybatis 提供。这就是"生态统一接入点"的设计价值（13-01 第一节）。

### 2.2 与 07-sentinel 分析的边界

07 的浅文已覆盖 `SentinelTemplate`/`SentinelOperations`/`SentinelContext`（Sentinel API 的模板封装，07 后续重做时展开）；13-03 只讲 **Sentinel 如何借 ExecutorFilter 进入 MyBatis**——两篇互补不重复。

### 2.3 值得注意的细节

- `AutoConfigureAfter` 里出现 **MyBatis Plus**（`com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration`）——**作者显式考虑了 MyBatis-Plus 环境的装配顺序**（Plus 有自己的 SqlSessionFactory 链，但 Executor 拦截机制相同；是否真兼容需实测，13 未验证）
- 资源名用 `ms.getId()`——**Mapper 方法全限定名即 Sentinel 资源名**，限流规则按 Mapper 方法配置，语义自然

---

## 三、与 18-dynamic 的关系（Mapper 扫描与数据源共存）

18-02 已分析 MyBatis 与 MyBatis-Plus 不共存的四个问题（DataSource 竞争 / Factory 绑定冲突 / Configuration 冲突 / 事务冲突）与 18-dynamic 的**子上下文隔离**方案。13 在此之上补两点：

**1. 管道与多 SqlSessionFactory 的兼容（已证）**：18 的子上下文方案**天然隔离**——每个子上下文独立创建 `SqlSessionFactory`/`Configuration`/`DataSource`，各自的 `newExecutor` → `pluginAll` 得到独立管道，本就不叠加。`InterceptingExecutorInterceptor.plugin()` 的**防嵌套合并**（13-01 第四节）是针对"**同一 Executor 被重复 plugin**"的额外保险（如多 Configuration 共享 executor 的边界场景）——不是子上下文不叠加的原因，是兜底。

**2. `dataSource="*"` 的坑（13-02 S4）**：多数据源场景（18 的主题）用 `@EnableMyBatis` 必须显式指定 `dataSource` bean 名，否则错绑主数据源——18 的子上下文方案（每个子上下文独立 DataSource）天然规避了这个问题，但**混合使用**（子上下文里再配 `@EnableMyBatis`）时要小心。

**3. Mapper 扫描的关联**：18-05 的扫描机制（AutoConfigurationImportSelector 缓存优化）与 13 的 Mapper 扫描（官方 `MapperScannerConfigurer` 或 `@MapperScan`）是两回事——**18 管"自动配置类扫描"，13 管"Mapper 接口注册"**，都在类路径扫描层但目标不同。

---

## 四、与 14-druid 的层级对照（两个层级的拦截）

| 维度 | 13-microsphere-mybatis | 14-microsphere-alibaba-druid |
|------|------------------------|------------------------------|
| 拦截层级 | **MyBatis Executor 层**（ORM 之上） | **JDBC 层**（Druid `FilterAdapter`，`preparedStatement_execute` 等） |
| 载体 | `ExecutorFilter` 接口 + `ExecutorFilterChain` | Druid 官方 `FilterAdapter`（继承 + 模板方法 `execute(statement, callable)`） |
| 拦截点 | update/query/commit/rollback...（10 类） | preparedStatement/statement 的 execute/executeQuery/executeUpdate |
| 可见信息 | MappedStatement（含 SQL 语义）、参数对象 | StatementProxy（JDBC 语句）、SQL 字符串 |
| 适用 | ORM 层业务横切（限流按 Mapper 方法） | 连接池/JDBC 层横切（连接校验、慢 SQL 监控） |
| 关系 | **互补**：同一 SQL **先过 MyBatis Executor 管道（ORM 层），再进 Druid Filter（JDBC 层）**——顺序固定，两层各管各的 | 同左 |

**一句话**：Sentinel 的 SQL 限流挂在 Executor 层（按 Mapper 方法粒度），慢 SQL/连接监控挂在 JDBC 层（Druid）——**两个层级拦截同一 SQL，各管各的粒度**。18-dynamic 的 HikariCP 对比（18-06）与 14 的 Druid 是"连接池"话题，13 与 14 是"拦截层级"话题——不冲突。

---

## 五、适用场景与生产评估

### 5.1 什么时候该用

| 场景 | 判断 |
|------|------|
| 生态内（Sentinel 限流 SQL / 监控 SQL） | **推荐**——统一管道 + 优先级 + 异常隔离 |
| 需要 SQL 改写/分页/脱敏 | **可行但要想清楚**——Executor 层能改 boundSql（MetaObject），但复杂度自担（官方 PageHelper 生态更成熟） |
| 需要参数/结果集级精细控制 | **不推荐**——用官方 Interceptor |
| 纯 MyBatis 无 Spring | core 模块可用（零 Spring 依赖），但装配要手动 |

### 5.2 生产风险清单（结合 13-01/13-02 的 P/S 清单）

| 风险 | 级别 | 说明 |
|------|------|------|
| 反射读私有字段（CachingExecutor.delegate / SqlSessionFactoryBean.plugins） | 中 | MyBatis 升级脆弱（P1/S3） |
| 二级缓存命中不过滤器（P6） | 中 | 限流/审计可能漏掉缓存命中的查询——**设计时要知道** |
| 观察者异常隔离的双刃剑 | 低 | 监控插件 bug 不伤业务，但**监控失效也无声无息** |
| spring-cloud 模块空壳（S2） | 低 | 配置中心驱动管道开关的能力未实现 |
| 初始版 NPE bug 教训（P3） | 参考 | 项目迭代中已修复；**当前 0.2.14 版本质量需自行验证**（本项目测试覆盖尚可，60 个测试） |

### 5.3 项目活性

450 提交、2025-02 创建、**2026-07 仍在迭代**（条件加固/cloud 测试）——microsphere 生态里少数"活着"的项目（对比 gateway 的沉寂）。双分支（main：Spring Cloud 2022-2025 / 1.x：Hoxton-2021）——1.x 分支兼容老 Spring Cloud。

---

## 六、小结（引用要点）

- **与官方**：手术刀 vs 流水线——方法级精确 vs 操作级分类，**可共存**（都是官方 Interceptor 链上的环）。
- **与 07**：Sentinel × MyBatis 是"生态统一接入点"的完整闭环（实现 ExecutorFilter + 注册 bean 即入管道）；资源名 = Mapper 方法全限定名。
- **与 18**：子上下文天然隔离（各自 Factory）；防嵌套合并是"同一 Executor 被重复 plugin"的兜底；`dataSource="*"` 多数据源需显式指定；18 管自动配置扫描、13 管 Mapper 注册。
- **与 14**：Executor 层 vs JDBC 层，同一 SQL 两级拦截，粒度互补。
- **生产**：限流/监控场景推荐；缓存命中不过滤器（P6）和反射脆弱（P1/S3）是主要风险；项目活跃度生态内少见。
