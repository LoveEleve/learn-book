# 13-01：核心机制——三层 Executor 拦截体系

> **核心命题**：MyBatis 官方拦截机制是 `@Intercepts` 注解 + `Plugin` 动态代理；microsphere 完全绕开它——`intercept()` 只打日志，真正的拦截发生在 `plugin()` 里**直接替换 Executor 对象**，换成自己实现的 `InterceptingExecutor`，再通过 `ExecutorFilterChain`（责任链）串起 `ExecutorFilter`（可干预）与 `ExecutorInterceptor`（纯观察）两层模型。本文逐层拆解：官方基线、三层解剖、挂载三步、演进史（含一个 git 取证的 NPE bug），以及 6 个问题。

---

## 一、项目定位（是什么、为什么、作用是什么）

**是什么**：microsphere-mybatis 是 MyBatis 的"SQL 执行过滤器管道"扩展框架——不碰官方动态代理，用责任链模型拦截 `Executor` 的全部操作，支持监控/修改/增强 SQL 执行，且是 Sentinel 等生态项目的接入点（`SentinelMyBatisExecutorFilter`，见 13-03）。

**为什么要有（从官方机制的四个缺陷推导）**：

| # | 官方 Interceptor 的缺陷 | microsphere 的答案 |
|---|------------------------|-------------------|
| 1 | **方法级匹配脆弱**：`@Signature` 精确到方法签名，MyBatis 升级改签名即抛 `PluginException`（官方 `Plugin.getSignatureMap`，Plugin.java:79-83：`getMethod` 找不到，NoSuchMethodException 被包装抛出） | **操作级分类**（10 类），与 `Executor` 接口同生共死 |
| 2 | **插件作者负担重**：`intercept()` 里自管 `proceed()`，每个调用都过一层 `InvocationHandler` | 责任链的放行/短路语义由 `ExecutorFilterChain` 保证，插件只实现操作回调 |
| 3 | **无优先级**：插件按配置顺序执行，跨插件协作（先限流再监控、监控包住限流）无法表达 | `Prioritized` 优先级排序 |
| 4 | **观察与干预不分**：一个 `Interceptor` 既要能改流程又要能纯观察，作者自选 | **双模型强制分工**：Filter 可干预 / Interceptor 纯观察（异常隔离） |

**作用是什么（业务价值，README Purpose 有据）**：
- **监控 SQL 执行**：日志、指标、可观测性（`LoggingExecutorFilter` 是内置样板）
- **横切关注点**：安全、限流、缓存注入（`SentinelMyBatisExecutorFilter` 是实证）
- **增强 SQL 执行**：参数改写、结果增强
- **生态统一接入点**：Sentinel/监控等生态组件**只需实现 `ExecutorFilter` 接口**即获得完整管道语义（顺序、异常隔离、生命周期）——不必各自写官方插件互相踩踏

**在训练营体系里的位置**：13 是"数据访问层"的横切能力底座——18-dynamic（动态数据源）的数据源切换、07-sentinel 的 SQL 限流、14-druid（JDBC 层 Filter）都可挂在 Executor 管道上；与 14 的对照是"**两个层级的拦截**"（MyBatis Executor 层 vs JDBC 层），13-03 展开。

**与官方 Plugin 的根本差异**：

| | 官方 Plugin | microsphere |
|---|---|---|
| 机制 | `@Intercepts` + `Plugin.wrap` 动态代理（InvocationHandler） | `plugin()` 里**直接替换 Executor**（无代理） |
| 粒度 | 按 `@Signature` 精确匹配方法 | 按 Executor 操作分类（10 类） |
| 模型 | 单一 Interceptor（`invoke()` 里自管 proceed） | **双模型**：Filter（责任链，可干预）+ Interceptor（观察者，纯监听） |
| 排序 | 无（按配置顺序） | `Prioritized` 优先级排序 |

**为什么绕开动态代理（一句）**：官方 `Plugin` 是 JDK 代理，**每个方法调用都过一次 InvocationHandler**，且 `intercept()` 里要自己管理 `proceed()`（错误处理、proceed 后继续拦截的语义都压在插件作者身上）；microsphere 用"一次替换 + 接口方法天然分类"——**拦截结构由 `Executor` 接口本身定义，插件作者只需实现"操作级别"的回调**，责任链的短路/放行语义由 `ExecutorFilterChain` 保证。代价是失去方法级精确匹配（官方 `@Signature` 能做到"只拦某个具体方法"）。

---

## 二、官方基线（mybatis-3.5.16 源码）

### 2.1 Executor 接口：15 个方法

`org.apache.ibatis.executor.Executor`（官方源码）：

```
update / query×2 / queryCursor / flushStatements / commit / rollback /
createCacheKey / isCached / clearLocalCache / deferLoad / getTransaction /
close / isClosed / setExecutorWrapper
```

**microsphere 的处理分类**：

| 类别 | 方法 | 说明 |
|------|------|------|
| 走链（10 个） | update / query×2 / queryCursor / commit / rollback / createCacheKey / deferLoad / getTransaction / close | 过滤器管道覆盖的全部操作 |
| 直通（4 个） | flushStatements / isCached / clearLocalCache / isClosed（InterceptingExecutor.java:114/137/165） | **"读缓存状态、刷新批处理"这类元操作不走管道**——有意的取舍（避免过滤器误伤内部机制） |
| 特殊 | setExecutorWrapper | 防嵌套处理（3.1） |

**只拦 Executor、不拦其他三类的取舍**：官方插件可拦截 4 类目标（`Plugins.TARGET_CLASSES`：Executor/ParameterHandler/ResultSetHandler/StatementHandler），microsphere 的 `plugin()` 对非 Executor **原样返回**——**只做"SQL 执行前后"的横切**。但要澄清 Executor 层的能力边界（避免误解）：
- **能做**：改 `parameter` 参数对象、改 `boundSql`（6 参版 `query` 把 `BoundSql` 直接传给过滤器——用 MetaObject 改 `sql` 字段即可实现分页/改写 SQL；**PageHelper 即拦截 `Executor.query` 这么做**）、改结果列表、短路（缓存/降级）
- **不能直接做**：ParameterHandler 的参数预编译、ResultSetHandler 的行映射、StatementHandler 的语句构建——**这三个层级的专属拦截点在管道外**（需官方 Interceptor 或自定义 Executor）

**职责聚焦的代价与分工**：管道语义简单（一个链），但 StatementHandler 等层级的精细控制要另走官方机制——**这是它与 Druid Filter 体系（14-druid，JDBC 层）的根本分工**：MyBatis Executor 层 vs JDBC 层，两个层级的拦截（13-03 展开）。

### 2.2 挂载点：Configuration.newExecutor → interceptorChain.pluginAll

```java
// Configuration.java:728-742（官方源码）
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    ...
    executor = new SimpleExecutor(this, transaction);   // 或 Batch/Reuse
    if (cacheEnabled) {
        executor = new CachingExecutor(executor);       // 二级缓存在外层
    }
    return (Executor) interceptorChain.pluginAll(executor);   // ← 挂载点
}
```

每次 `openSession` 都会执行 `newExecutor` → 遍历 `InterceptorChain` 里配置的所有 Interceptor，逐个 `interceptor.plugin(executor)`——**microsphere 的挂载就发生在这里**。

### 2.3 官方 Plugin：动态代理

官方 `Plugin implements InvocationHandler`（Plugin.java:32）：`wrap(target, interceptor)` 生成 JDK 代理，`invoke()` 里按 `@Intercepts` 声明的 `@Signature` 精确匹配方法，命中才走 `interceptor.intercept(new Invocation(target, method, args))`——**每个方法调用都过一遍代理层**。

---

## 三、三层解剖

### 3.1 InterceptingExecutor：包装器（每次调用新建链）

```java
public class InterceptingExecutor implements Executor {   // 实现官方接口，不是代理
    private final Executor delegate;
    private final Properties properties;
    private final ExecutorFilter[] executorFilters;

    public int update(MappedStatement ms, Object parameter) throws SQLException {
        ExecutorFilterChain chain = buildChain();   // 每次调用新建链
        return chain.update(ms, parameter);
    }
    ExecutorFilterChain buildChain() {
        return new ExecutorFilterChain(this.delegate, this.properties, this.executorFilters);
    }
    @Override
    public void setExecutorWrapper(Executor executor) {   // 防嵌套
        if (executor instanceof InterceptingExecutor) {
            return;
        }
        InterceptingExecutor interceptingExecutor = new InterceptingExecutor(executor, this.properties, this.executorFilters);
        delegate.setExecutorWrapper(interceptingExecutor);
    }
}
```

**设计要点**：
- **每次调用新建 `ExecutorFilterChain`**（filters 数组本身共享，只新建轻量 chain 对象）——"简单正确"路线（对比 16-gateway 的缓存路线，这里没有缓存必要性：过滤器数组不变，链对象创建开销可忽略）。
- `setExecutorWrapper` 防嵌套：再包装时若目标已是 `InterceptingExecutor` 直接忽略——**避免多层 InterceptingExecutor 叠加**（MyBatis 的 ResultSetHandler 惰性加载会调用 setExecutorWrapper，这是真实触发路径）。

### 3.2 ExecutorFilterChain：数组 + 游标（核心路由）

```java
public class ExecutorFilterChain {
    private final Executor executor;          // 链尾目标
    private final ExecutorFilter[] filters;
    private int position;

    protected <E extends Throwable, R> R process(ThrowableFunction<ExecutorFilter, R> filterFunction,
                                                 ThrowableFunction<Executor, R> executorFunction,
                                                 Function<Throwable, E> failureHandler) throws E {
        if (position < size) {
            result = filterFunction.apply(filters[position++]);   // 下一个过滤器
        } else {
            result = executorFunction.apply(this.executor);       // 链尾 → 官方 Executor
        }
    }
}
```

- **数组 + 游标**——与 16-gateway 的 `DefaultGatewayFilterChain` 完全同构（同一作者的惯用模式）。
- **四类入口**：`apply`（返回值，异常包 RuntimeException）/ `applySQL`（返回值，异常包 SQLException）/ `consume` / `consumeSQL`（void 版）——**SQLException 与 RuntimeException 的异常包装分离**，保证 SQL 异常语义不被吞掉。

### 3.3 双模型：ExecutorFilter（可干预）vs ExecutorInterceptor（纯观察）

| 维度 | `ExecutorFilter`（责任链） | `ExecutorInterceptor`（观察者） |
|------|---------------------------|-------------------------------|
| 父接口 | `extends Prioritized`（有优先级） | `extends Prioritized` |
| 能力 | **可改参数、可短路**（不调 chain 即短路） | **只能观察**：before/after 回调 + result/failure |
| 方法数 | 10 个 default 透传方法 | 18 个 before/after 回调 |
| 典型实现 | `LoggingExecutorFilter`（debug 日志） | `LoggingExecutorInterceptor` |
| 异常 | 抛给上游 | **catch + warn，不影响主链路** |

**为什么双模型（设计的精华）**：责任链适合"要干预的横切"（限流、改 SQL、鉴权），观察者适合"只要看的横切"（监控、日志、指标）——**观察者异常被隔离**（`InterceptorsExecutorFilterAdapter.iterate` 里 catch Throwable + warn），**一个监控插件的 bug 不会打挂业务 SQL**。这是生产友好的关键设计。

### 3.4 InterceptorsExecutorFilterAdapter：观察者 → 责任链的桥

```java
public class InterceptorsExecutorFilterAdapter implements ExecutorFilter {
    public InterceptorsExecutorFilterAdapter(ExecutorInterceptor[] executorInterceptors) {
        ...
        sort(this.executorInterceptors, PriorityComparator.INSTANCE);   // 构造时按优先级排序
    }
    @Override
    public int update(MappedStatement ms, Object parameter, ExecutorFilterChain chain) throws SQLException {
        InterceptorContext<Executor> context = buildContext(chain);
        beforeUpdate(context, ms, parameter);              // before 回调
        Integer result = null;
        SQLException failure = null;
        try {
            result = chain.update(ms, parameter);          // 继续链
        } catch (SQLException e) {
            failure = e;
            throw e;
        } finally {
            afterUpdate(context, ms, parameter, result, failure);   // after 回调（带 result/failure）
        }
        return result;
    }
    void iterate(Consumer<ExecutorInterceptor> consumer) {
        for (...) {
            try { consumer.accept(...); }
            catch (Throwable e) { logger.warn("Failed to execute ExecutorInterceptor[...]", e); }  // 异常隔离
        }
    }
}
```

**桥接语义**：把 N 个"纯观察者"适配成责任链里的**一个环节**——`ExecutorFilterChain` 只认 `ExecutorFilter`，观察者通过 Adapter 进入管道。`InterceptorContext`（target + properties 副本 + startTime + attributes）是观察者之间共享的上下文（**startTime 用于计时监控**）。

---

## 四、挂载机制：plugin() 三步（全部源码验证）

```java
// InterceptingExecutorInterceptor.plugin(Object target)
public Object plugin(Object target) {
    if (target instanceof Executor executor) {
        boolean isCachingExecutor = executor instanceof CachingExecutor;
        Executor delegate = executor;
        if (isCachingExecutor) {
            delegate = getDelegate(cachingExecutor);      // ① 剥：反射读 CachingExecutor.delegate
        }
        Properties newProperties = new Properties();
        ExecutorFilter[] executorFilters = this.executorFilters;
        if (delegate instanceof InterceptingExecutor previous) {   // ② 合并：防嵌套
            delegate = previous.getDelegate();
            newProperties.putAll(previous.getProperties());       //    合并 properties
            addAll(newExecutorFiltersList, previous.getExecutorFilters());  //    合并过滤器
            addAll(newExecutorFiltersList, executorFilters);
        }
        ...
        InterceptingExecutor interceptingExecutor = new InterceptingExecutor(delegate, newProperties, executorFilters);
        return isCachingExecutor ? new CachingExecutor(interceptingExecutor) : interceptingExecutor;  // ③ 重包
    }
    return target;   // 非 Executor 目标（ParameterHandler 等）原样返回——只拦截 Executor
}
```

**三步的含义**：
1. **剥**：默认 executor 是 `CachingExecutor`（二级缓存外层），剥出底层 delegate（`Executors.getDelegate` 反射读私有字段）
2. **合并**：若底层已是 `InterceptingExecutor`（多 SqlSessionFactory、多个 InterceptingExecutorInterceptor 实例），**剥到底 + 合并过滤器与 properties**——防嵌套叠加
3. **重包**：`new CachingExecutor(interceptingExecutor)`——**缓存语义保持在外层**（缓存命中不会经过过滤器链？不——CachingExecutor.query 先查二级缓存，未命中才调 delegate（InterceptingExecutor）→ 过滤器链。**所以二级缓存命中时过滤器不执行**，这是缓存与拦截的语义边界）

**挂载全景**：

```
openSession → Configuration.newExecutor
  → SimpleExecutor → CachingExecutor(SimpleExecutor)
  → interceptorChain.pluginAll
    → 官方插件们（动态代理包装）
    → InterceptingExecutorInterceptor.plugin
      → CachingExecutor( InterceptingExecutor( SimpleExecutor, [filters...] ) )
```

---

## 五、演进史（450 提交，git 取证）

| 时间 | 事件 | 意义 |
|------|------|------|
| 2025-02-15 | Initial commit | 项目诞生（比 gateway 晚一年） |
| 2025-02-16 | GA（1c88235） | 初始只有**观察者模型**（ExecutorInterceptor 直接内嵌 InterceptingExecutor）；**含真实 NPE bug**：`this.delegate = delegate`（构造参数 `executor` 从未使用，赋的是未初始化的 final 字段——运行必 NPE） |
| 2025-02-22 | Refactor（fcdbeee） | **创建 ExecutorFilter + ExecutorFilterChain**——责任链模型加入，Adapter 把观察者包成链环，**双模型成型** |
| 2025-02-23 | Refactor（067eea1） | **CachingExecutor 剥离重包**（初始是直接把 CachingExecutor 包进内层） |
| 2026-01-23 | Refactor（322c3ca） | executor 拦截重构 + 测试补全 |
| 2026-06-06 | f1284c3 | EnableMyBatisExtension（三源扫描）+ Registrar 重构 |
| 2026-07-11 | 条件加固 | **项目至今活跃**（对比 gateway 的沉寂） |

**演进叙事**：观察者模型 → 责任链模型（Filter 加入）→ 挂载细节打磨（CachingExecutor/防嵌套）→ Spring 集成成型。**双模型不是一次设计出来的，是演进叠加的**——这解释了为什么 Filter 和 Interceptor 的能力边界如此清晰（后加的 Filter 补上了"干预"能力，观察者保留"监听"职责）。

---

## 六、问题清单（已证）

| # | 问题 | 证据 |
|---|------|------|
| P1 | 反射读私有字段：`Executors.getDelegate` 读 `CachingExecutor.delegate` | `Executors.java`（`getFieldValue`） |
| P2 | 拼写错误：`MyBatisConfigurationBeanDefintionRegistrar`（Defintion 少 i） | 类名（13-02 详述） |
| P3 | 初始提交 NPE bug（`this.delegate = delegate`）——已修复，但暴露"初始版可能从未运行过" | git 1c88235 |
| P4 | 每次调用新建 chain（无缓存）——filters 数组共享、chain 轻量，开销可忽略 | `buildChain()` |
| P5 | `intercept()` 语义陷阱：实现官方 Interceptor 却**不拦截**（只 warn+proceed）——若有人误以为它走动态代理会困惑 | `InterceptingExecutorInterceptor.intercept` |
| P6 | 二级缓存命中时过滤器不执行（CachingExecutor 外层语义）——**源码验证**：`CachingExecutor.query`（官方 96-110 行）先 `tcm.getObject(cache, key)` 查二级缓存，**命中直接返回不调 delegate**（102-107 行），未命中才进过滤器链 | plugin() 重包逻辑 + `CachingExecutor.java:102-107` |

---

## 七、测试佐证

- `InterceptingExecutorInterceptorTest.testIntercept`：断言 `intercept()` 结果等于直接 `invocation.proceed()`——**"不拦截"语义有测试保护**（P5 是有意设计）。
- `InterceptingExecutorTest.testSetExecutorWrapper`：覆盖防嵌套逻辑（P1 关联）。
- `InterceptorsExecutorFilterAdapterTest` / `ThrowingErrorExecutorInterceptor`：异常隔离的测试证据（`ThrowingErrorExecutorInterceptor` 专门用来验证拦截器抛异常不影响主链路）。

---

## 八、小结（引用要点）

- **一句话**：microsphere-mybatis 用"实现官方 Executor 接口 + 官方 SPI 挂载 + 责任链/观察者双模型"替代官方动态代理，给 MyBatis Executor 装了过滤器管道。
- **三个关键设计**：Filter 可干预 vs Interceptor 纯观察（异常隔离）；CachingExecutor 剥出重包（缓存语义在外层，缓存命中不过滤器）；防嵌套合并（多 SqlSessionFactory 安全）。
- **一个语义边界**：二级缓存命中时过滤器不执行。
- **演进启示**：双模型是"观察者 → 责任链"演进叠加的结果，不是一次设计——初始版还有真实 NPE bug（git 取证）。
