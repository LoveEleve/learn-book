# 14-02：与 13/18 的对照、测试基建与生产评估

> **核心命题**：同一条 SQL 在 microsphere 生态里要经过两级拦截——先进 MyBatis Executor 管道（13，ORM 层），再进 Druid Filter（14，JDBC 层），粒度互补、顺序固定。装配侧的"三源扫描"机制（`BeanSource` 枚举）是 13/14 两个项目共享的同一个工具类，但 13 项目正确复用了官方封装好的静态方法，14 项目却自己手写了一遍、还写出了 bug（14-01 的 P2）——这是理解"同一作者的惯用模式为什么会在不同项目里表现不一致"的关键案例。本文展开层级对照、装配机制对照、测试基建对照、与 HikariCP 的连接池对照，最后给出生产评估。

---

## 一、与 13-mybatis 的层级对照：两个层级的拦截

13-03 第四节已经预埋了"两个层级的拦截"表格（13/14 的层级、载体、拦截点、可见信息、适用场景对照），这里不重复整张表，只展开三个 14-01 未讲透的细节。

### 1.1 "先 Executor 后 Filter"的顺序为什么固定

同一条 SQL 从 Mapper 方法调用到落地执行，物理顺序是：

```
Mapper 方法调用
  → MyBatis Executor.update/query（13 的拦截点：ExecutorFilterChain）
    → JDBC PreparedStatement.execute（14 的拦截点：AbstractStatementFilter.execute）
      → 真正发到数据库的 SQL
```

这个顺序不是设计选择，是**技术栈层级决定的物理事实**：MyBatis 是 ORM 框架，它的 `Executor` 在应用逻辑和 JDBC API 之间；Druid 是连接池，它包装的是 JDBC `Statement` 对象本身。ORM 层必然先拿到调用，再往下委托到 JDBC 层——**顺序不可能反过来**。这意味着 13 的 `ExecutorFilter`（比如 Sentinel 限流）如果要短路（不执行 SQL），Druid Filter 根本不会被触发；反过来，Druid Filter 看到的每一条 SQL 都已经是 MyBatis Executor 管道放行过的。

### 1.2 可见信息的不对称：语义信息在上层，执行信息在下层

13 的 `ExecutorFilter` 拿到的是 `MappedStatement`（含 SQL id、参数类型、结果映射配置）+ 参数对象（Java 对象，未转 SQL 字面量）；14 的 `AbstractStatementFilter` 拿到的是 `StatementProxy.getLastExecuteSql()`——**已经是拼好占位符的最终 SQL 字符串**（`PreparedStatement` 的参数已经绑定，通过 `getLastExecuteSql()` 能拿到实际执行的 SQL 文本，而不是带 `?` 占位符的模板）。

这个不对称直接决定了两层过滤器的"资源名"生成方式完全不同：13-01（3.4 节以 13-01 记法）Sentinel 集成用 `MappedStatement.getId()`（Mapper 接口全限定名 + 方法名，比如 `com.example.UserMapper.selectById`）作资源名——**语义清晰但与实际执行的 SQL 无关**；14 的 `buildResourceName` 是解析 SQL AST 拼出 `"SELECT users"` 这种**基于实际执行语句的资源名**——同一个 Mapper 方法如果动态生成不同的 SQL（比如 `<if>` 标签导致不同分支），13 层看到的资源名不变（还是那个方法名），14 层看到的资源名可能不同（取决于最终 SQL 命中了哪个分支）。这是"按业务语义限流"（13）与"按实际 SQL 模式监控"（14）两种不同粒度的根本原因。

### 1.3 两层的"防重复挂载"逻辑不同源，但目标一致

13-01 的 `plugin()` 三步里有"合并防嵌套"——多个 `SqlSessionFactory` 或多次 `plugin()` 调用时，剥到底 + 合并过滤器数组，避免同一个 `InterceptingExecutor` 被多层嵌套包装。14 的对应逻辑是"跳过 `@AutoLoad` filter"（14-01 第四节）——避免三源扫描注册的 Filter 类和官方 SPI 自动加载的 Filter 类产生两个实例。**两者的触发场景不同**（13 是"同一个 Executor 被多次包装"，14 是"同一个 Filter 类被两条独立路径都注册"），**但设计目标相同**：防止同一个横切逻辑被套用两次、产生重复的性能开销或语义混乱。这是"防重复"这个通用工程问题，在两个不同层级找到了两种不同的具体解法。

---

## 二、装配机制对照：同一个 `BeanSource` 工具类，两种用法

14-01 分析过 `AlibabaDruidRegistrar.registerFilterBeans`（`AlibabaDruidRegistrar.java:67-79`）的 switch fallthrough bug。现在把镜头转到 13-mybatis 侧看同一个机制怎么被正确使用的。

### 2.1 13-mybatis 的 `MyBatisExtensionBeanDefinitionRegistrar`：直接复用静态工具方法

```java
// MyBatisExtensionBeanDefinitionRegistrar.java:78-87
protected void registerBeanDefinitions(...) {
    if (attributes.getBoolean("interceptExecutor")) {
        BeanSource[] sources = (BeanSource[]) attributes.get("sources");
        registerExecutorFilters(sources);
        registerExecutorInterceptors(sources);
        registerInterceptingExecutorInterceptorIfRequired(registry);   // ← 收尾动作在循环外，只执行一次
    }
}

// MyBatisExtensionBeanDefinitionRegistrar.java:117-120
private void registerBeansFromSources(Class<?> beanType, BeanSource[] sources) {
    Map<Class<?>, String> beanTypesAndNames = registerBeans(this.beanFactory, sources, beanType);
    // ↑ 调用 BeanSource.registerBeans(beanFactory, sources, beanType) 3 参数静态方法（BeanSource.java:301-303）
}
```

`BeanSource.registerBeans(ConfigurableListableBeanFactory, BeanSource[], Class<?>...)`（`BeanSource.java:301-303`）内部转发给 4 参数版本（`BeanSource.java:382-393`）：

```java
public static Map<Class<?>, String> registerBeans(..., BeanSource[] beanSources, Class<?>... beanTypes) {
    Map<Class<?>, String> beanTypesAndNames = newHashMap(length);
    for (BeanSource beanSource : beanSources) {
        beanTypesAndNames.putAll(beanSource.registerBeans(beanFactory, registry, beanTypes));
        //                        ↑ 多态分派——每个 BeanSource 枚举常量各自实现 registerBeans
    }
    return unmodifiableMap(beanTypesAndNames);
}
```

**关键设计**：`BeanSource` 是枚举，`BEAN_FACTORY`/`SPRING_FACTORIES`/`JAVA_SERVICE_PROVIDER` 三个常量各自覆写 `getBeanTypes()`（`BeanSource.java:63-110`），`registerBeans` 遍历数组时对每个常量调用同一个方法名——**这是策略模式 + 枚举多态，不需要 switch**。13-mybatis 项目里的 `MyBatisExtensionBeanDefinitionRegistrar` 完全没有写任何 `if`/`switch` 判断 `BeanSource` 的具体类型，直接把 `sources` 数组转发给 `BeanSource.registerBeans` 静态方法，让枚举自己的多态方法处理分支逻辑。

### 2.2 14-druid 的 `AlibabaDruidRegistrar`：绕开工具方法，手写了一个 switch

对比 14-01 已经展开的代码：

```java
// AlibabaDruidRegistrar.java:67-79
private void registerFilterBeans(BeanDefinitionRegistry registry, Class<? extends Filter>[] filterClasses, BeanSource[] sources) {
    for (BeanSource source : sources) {
        switch (source) {
            case SPRING_FACTORIES:
                registerFiltersBySpringFactories(registry, filterClasses);
            case JAVA_SERVICE_PROVIDER:
                registerFiltersByJavaServiceProvider(registry, filterClasses);
            case BEAN_FACTORY:
            default:
                registerDruidDataSourceBeanPostProcessor(registry, filterClasses);
        }
    }
}
```

`AlibabaDruidRegistrar` **没有调用** `BeanSource.registerBeans` 静态方法——而是自己写了一个 `for` 循环 + `switch`，对 `BeanSource` 枚举值做手动分支判断。这个 switch 恰好就是 14-01 P2 bug 的根源：**手写的分支逻辑忘了写 `break`，而枚举多态分派根本不存在"忘记某个分支"的可能——因为多态天生就是"各管各的"，没有共享的穿透风险**。

**同一个作者，同一个 `BeanSource` 工具类，两种用法，两种结果**：13-mybatis 走"复用现成静态方法"路线，零 bug；14-druid 走"自己实现分支逻辑"路线，产生了一个 fallthrough bug。这不是能力问题（`BeanSource.registerBeans` 本身就是这个作者写的），而是**同一个人在不同项目、不同时间点的实现选择不同**——这类"同源工具类被不同调用方以不同方式误用/正确使用"的现象，在系统性代码 review 时值得作为一类检查项：**新代码引入自定义分支逻辑前，先看是否已有现成的多态/策略模式方法可以直接调用**。

### 2.3 为什么 14-druid 没有直接复用（推断，非源码可证）

`AlibabaDruidRegistrar` 除了要注册 Filter Bean，还要在 `BEAN_FACTORY` 场景额外注册 `DruidDataSourceBeanPostProcessor`（13-mybatis 的对应逻辑 `registerInterceptingExecutorInterceptorIfRequired` 是在**三源扫描全部完成之后**统一调用一次，不在循环内部）。**推断**：`AlibabaDruidRegistrar` 的作者想要"每个 source 处理完自己的注册后，还要做一个共同的收尾动作"，选择把收尾动作也塞进 switch 的 `default` 分支，试图用 fallthrough 让每次循环都执行收尾——这解释了为什么会写成"看起来像故意 fallthrough"的结构，但因为忘记控制"只执行一次"的边界，变成了重复调用的 bug。13-mybatis 的正确写法是把收尾动作**完全挪到循环外面**（`registerBeanDefinitions` 方法体里，三源注册完成之后再调用一次），这才是清晰的做法。

---

## 三、测试基建对照：同构骨架，两种实现规模

### 3.1 接口签名完全一致

```java
// AlibabaDruidTestExtension.java:85-86
class AlibabaDruidTestExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor,
        AfterEachCallback, ParameterResolver {

// MyBatisTestExtension.java:54-55
public class MyBatisTestExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback,
        TestInstancePostProcessor, ParameterResolver {
```

两个 `TestExtension` 实现的 JUnit5 扩展点接口**完全一致**（只是声明顺序不同）——`BeforeAllCallback`（类级初始化）、`AfterAllCallback`（类级清理）、`AfterEachCallback`（方法级清理）、`TestInstancePostProcessor`（字段注入）、`ParameterResolver`（参数注入）。这是同一个作者对"JUnit5 测试运行时"这类需求的标准骨架——需要类级 + 方法级两种粒度的资源生命周期管理，加上字段/参数两种注入方式，5 个接口正好覆盖全部场景。

### 3.2 组件发现机制不同：手工枚举 vs SPI resolver

**13-mybatis 走 SPI + 多态**：

```java
// MyBatisTestExtension.java:57
private static final List<ComponentResolver> componentResolvers = loadServicesList(ComponentResolver.class);
```

`ComponentResolver` 是一个接口，具体的 `Configuration`/`Connection`/`DataSource`/`Environment`/`Executor`/`MapperComponent`/`Properties`/`SqlSessionFactory`/`SqlSession`/`Transaction` 10 种 resolver 各自实现 `META-INF/services` 注册，`MyBatisTestExtension` 只需要遍历 `componentResolvers` 列表找到能处理当前字段/参数类型的那个。**注意一个容易高估的细节**：`ComponentResolver.isComponentType()`（`ComponentResolver.java:105-107`）用的是 `Objects.equals(getComponentType(), requestedComponentType)`——**精确类型相等判断，不是 `isAssignableFrom` 子类型判断**，所以 `ConfigurationResolver` 并不能处理 `Configuration` 的任意子类型字段（比如自定义 `MyConfiguration extends Configuration`），必须字段声明的类型正好是 `Configuration.class` 本身才会命中。真正的扩展性优势在另一个维度：**新增一种全新的可注入组件类型**（比如要支持注入某个业务专属对象），只需要新增一个 resolver 实现类 + SPI 注册，`MyBatisTestExtension` 本身不用改一行代码——这是"开放扩展点"（SPI 发现）而不是"支持里氏替换"（子类型兼容）。

**14-druid 走硬编码类型判断**：

```java
// AlibabaDruidTestExtension.java:305-307
static boolean isCandidateType(Class<?> type) {
    return candidateTypes.contains(type);
}
```

`candidateTypes` 是一个静态字段，通过 `findAllClasses(DruidDataSource.class, AlibabaDruidTestExtension::isAlibabaDruidClass)` 在类加载时扫描出来（`AlibabaDruidTestExtension.java:92`）——本质上是"扫描出所有跟 Druid 相关的类，判断字段/参数类型是否在这个集合里"。在"是否支持子类型注入"这个维度上，`isCandidateType` 和 `ComponentResolver.isComponentType` 其实**没有本质区别**（都是精确类型集合匹配，见上一段的修正）。真正的差异在"新增一种可注入类型要不要改现有代码"：14-druid 若要支持注入一个新的 Druid 相关类型，只需要它被 `findAllClasses` 扫描进 `candidateTypes` 静态集合即可（**这个扫描本身是自动的**，不需要手动维护列表）；但 14-druid **不支持像 13-mybatis 那样为完全不相关的新类型（不在 `com.alibaba.druid.*` 包名下、也不是 `DataSource` 子类）注册专属的注入逻辑**——因为 14-druid 没有 SPI resolver 这层可扩展的抽象，`isAlibabaDruidClass` 的判断逻辑是硬编码在 `AlibabaDruidTestExtension` 内部的（`AlibabaDruidTestExtension.java:338-343`），要支持一种"判断方式"不同的新类型就必须改这个方法本身。

**规模差异是这个设计差异的直接后果**：`MyBatisTestExtension.java` 全文 **142 行**，`AlibabaDruidTestExtension.java` 全文 **488 行**——3.4 倍的体量差。14-druid 版本要自己实现 class/method 双级 `Store`（`CLASS_NAMESPACE`/`METHOD_NAMESPACE`）+ `buildKey`（拼字符串键）+ `get`/`store` 存取逻辑（`AlibabaDruidTestExtension.java:88-488` 大部分篇幅），而这些"缓存 + 复用已构建实例"的逻辑，本质上是因为 `DruidDataSource` 构建成本较高（要走 `configFromProperties` + `init()` 建立真实连接池），需要在同一个测试类的多个方法间复用；13-mybatis 侧没有这个问题是因为 `SqlSessionFactory` 的构建被封装进了 `AbstractMyBatisTest.init()`（每个测试方法独立构建，13-02 提到过"每个测试方法独立数据环境，隔离性好，代价是慢"）——**14-druid 选择了"缓存复用换性能"，13-mybatis 选择了"每次重建换隔离性"，是两种不同的测试隔离策略取舍**，不是谁更好，是场景不同：Druid 连接池初始化开销比 MyBatis SqlSessionFactory 构建开销更值得缓存复用。

### 3.3 14-druid 特有的测试桩三件套

`AutoLoadFilter`（标 `@AutoLoad`）/ `LoadFilter`（普通类，走 `META-INF/services`）/ `TestStatementFilter`（走 `spring.factories`）——三个测试桩精确对应三源扫描的三条路径，`EnableAlibabaDruidTest` 同时启用 `BEAN_FACTORY`/`SPRING_FACTORIES`/`JAVA_SERVICE_PROVIDER` 验证三源都能找到对应 Filter。这套测试桩设计是清晰的（一条路径一个桩），**但如第二节分析，没有断言"BPP 只应注册一次"，因此 P2 bug 未被捕获**——测试桩的"广度"（覆盖三条路径）没有配上"深度"（验证注册次数的正确性）。

---

## 四、与 18-06 的连接池对照：HikariCP vs Druid

18-06 详细拆解了 HikariCP 的 `ConcurrentBag`（无锁并发容器，`ThreadLocal` 缓存 + `SynchronousQueue` 交接）和三种连接池的对比表。这里只做 14 独有的补充。

| 维度 | HikariCP（18-06 详解） | Druid（本系列 13/14 涉及） |
|------|------------------------|---------------------------|
| 核心优势 | 性能（无锁 `ConcurrentBag`） | **可扩展的可观测性能力**（`Filter` 体系原生支持 SQL 监控；microsphere 项目本身只落地了 `LoggingStatementFilter` 一种，但 `AbstractStatementFilter` 模板可以低成本扩展出慢查询统计等能力） |
| 连接验证 | JDBC 4.0 `isValid()`（推荐），`connectionTestQuery`（已废弃） | `validationQuery` 属性 + `AbstractStatementFilter` 里对 validationSQL 的短路判断（14-01 3.4 节） |
| 扩展机制 | 无内置扩展点体系（要监控需要额外集成 Micrometer 等） | `Filter` 接口体系原生支持——491 个方法覆盖 JDBC 全生命周期，`FilterChain` 责任链天然支持多 Filter 叠加（`getProxyFilters().addAll`，14-01 4.3 节已证） |
| actuator 集成 | Spring Boot 内置 `HikariDataSource` 的 `DataSourcePoolMetadataProvider`（官方直接支持） | 需要 microsphere-alibaba-druid 补一个 `DruidDataSourcePoolMetadata`（14-01 5.2 节，因为官方 Druid 不像 HikariCP 那样有 Spring Boot 内置的 metadata provider） |
| 适用场景取舍 | 高并发、追求极致性能、不需要 SQL 级可观测性 | 需要 SQL 执行监控类横切能力，能接受略低于 HikariCP 的连接获取性能 |

> **来源标注**：Druid 官方生态本身还有 `WallFilter`（SQL 防火墙/黑白名单）、`StatFilter`（慢查询统计）等成熟组件——但这些是 **Druid 官方**（`com.alibaba.druid` 包）自带的能力，`microsphere-alibaba-druid` 项目**没有对它们做任何封装或引用**，本系列分析范围内只验证过 `LoggingStatementFilter` 这一个内置样板。上表和下文提到的"审计/防火墙/慢查询"等能力，指的是"`AbstractStatementFilter` 模板理论上可以低成本扩展出这类 Filter"，不是"microsphere-alibaba-druid 项目已经实现了这些能力"——两者不要混淆。

**关键结论**：这不是"哪个连接池更好"的问题，是**性能优先 vs 可观测性优先**的场景选择——18-dynamic 默认用 HikariCP（`DEFAULT_DATASOURCE_TYPE_NAME = HikariDataSource`，18-06 已验证），是因为动态数据源场景本身对连接创建/销毁性能敏感（子上下文频繁重建）；而需要 SQL 治理能力（比如结合 Sentinel 做 JDBC 层限流，或者需要官方 Druid 生态的 `WallFilter` 之类能力）的场景，Druid 的 `Filter` 生态本身是**目前 HikariCP 生态里没有对应能力**的差异化优势——但这个优势是"Druid 官方生态"的优势，microsphere-alibaba-druid 项目在其中扮演的角色是"提供了模板化的 Spring 装配方式"，不是"自己实现了审计/防火墙"。两者可以在同一套技术栈里根据子系统需求分别选型，不是互斥关系。

---

## 五、作者惯用模式：三个项目的共性与差异（13/14/16 归纳）

综合 13（MyBatis）、14（Druid）、16（Gateway，仅引用其"JUnit5 测试即行为文档"理念，16 本身不涉及三源扫描）三个项目，同一作者的惯用模式可以归纳为：

| 模式 | 13-mybatis | 14-druid | 说明 |
|------|-----------|----------|------|
| before/after 回调模板 | `InterceptorsExecutorFilterAdapter`（13-01 3.4） | `AbstractStatementFilter.execute`（14-01 3.3） | 结构同构（try/beforeCall/afterCall/finally），异常捕获范围与归一化逻辑因异常契约不同而不同（14-01 已析） |
| 三源扫描（`BeanSource`） | 直接调用 `BeanSource.registerBeans` 静态方法（本文 2.1） | 自己手写 switch 分支（本文 2.2，含 bug） | **同一工具类，两种用法** |
| JUnit5 测试基建 | `MyBatisTestExtension` + SPI resolver（本文 3.2） | `AlibabaDruidTestExtension` + 硬编码类型集合（本文 3.2） | 骨架接口相同，组件发现机制不同（SPI 多态 vs 类型集合），规模差 3.4 倍 |
| 双开关体系 | `@ConditionalOnMyBatisEnabled` + `@ConditionalOnMyBatisAvailable`（13-02 4.1） | `@ConditionalOnAlibabaDruidEnabled` + `@ConditionalOnAlibabaDruidAvailable`（14-01 5.1） | 命名规则、组合方式（`@ConditionalOnXxxEnabled` 元注解 + `@ConditionalOnClass`）同构；但 `ConditionalOnMyBatisAvailable` 检查 2 个 classpath 类（`SqlSession`+`SqlSessionFactory`），`ConditionalOnAlibabaDruidAvailable` 只检查 1 个（`DruidDataSource`）——**顶层模式同构，具体检查粒度不同**（依赖数量本身反映的是 MyBatis API 面比 Druid 更分散） |

**归纳价值**：这套对照说明——"同一作者的架构偏好"（回调模板、双开关、JUnit5 扩展骨架）在跨项目场景下高度一致、几乎是复制粘贴级别的同构；但**具体到某个机制的实现细节**（三源扫描到底调用现成方法还是手写分支、测试基建到底走 SPI 还是硬编码），会因为项目的实现时间点、复杂度、维护精力分配不同而产生差异——**这类差异正是 bug 潜伏的地方**：宏观架构的一致性掩盖不了微观实现的参差，14-druid 的 P2 bug 正是这种"细节层面的实现漂移"的具体案例。

---

## 六、生产评估

### 6.1 适用场景

- **适合**：需要在连接池层面接入 SQL 执行监控（日志/审计类横切逻辑）的场景；已经在用 Druid 作为连接池（而非仅用它的监控页面）；需要用统一的 `beforeExecute`/`afterExecute` 模型快速接入监控/日志类横切逻辑，不想直接面对 Druid 官方 491 个方法的 `Filter` 接口。若需要更完整的 SQL 治理能力（`WallFilter` 防火墙、`StatFilter` 统计），那些是 Druid 官方生态自带的组件，与 `AbstractStatementFilter` 可以并存但不是本项目提供的能力（第四节"来源标注"已析）。
- **不适合**：纯粹追求连接池性能极限（HikariCP 更优，18-06 已证）；业务大量使用存储过程（`AbstractStatementFilter` 不覆盖 `CallableStatement`，14-01 3.2 节已析，过滤器完全看不到存储过程调用）；需要 Spring Cloud Actuator `/features` 之外更丰富的 Spring Cloud 集成（spring-cloud 模块自动配置层面是空壳，14-01 5.3 节）。

### 6.2 风险清单（汇总 14-01 + 本文新增）

| 来源 | 风险 | 严重度 |
|------|------|--------|
| 14-01 P1 | 双开关冗余，`enabled` 字段未被实际读取 | 低（不影响功能，只是维护混淆） |
| 14-01 P2 | switch fallthrough 导致 BPP 重复注册，因下游去重未暴露 | 中（换个不做去重的场景就会出问题，本文第二节已析根因） |
| 14-01 P3 | spring-cloud 自动配置空壳 | 低（真正内容在 features.yaml，本文未发现新证据推翻此结论） |
| 14-01 P4 | 日志占位符数量不匹配 | 极低（纯格式瑕疵） |
| 本文新增 | 测试桩广度够、深度不够（未断言注册次数） | 中（这是 P2 未被发现的直接原因） |
| 能力边界 | 不覆盖 `CallableStatement` | 场景相关（存储过程重度用户需注意） |

### 6.3 项目活性

git 386 提交，2025-02-14 创建，2026-07 仍活跃（最新版本 0.2.19，与本系列写作时间同期）——项目处于持续维护状态，与 13-mybatis（450 提交，13-01 已析）、16-gateway（16-02 演进史）相比，14-druid 相对年轻但迭代节奏稳定。P2 这类"手写分支逻辑" bug 出现在 2026-05-30 的提交（`81b8043b`），距离最新版本约 1-2 个月未被发现/修复——说明这个 fallthrough 分支在实际使用中触发概率低（因为下游去重掩盖了后果），建议后续版本用第二节展示的 `BeanSource.registerBeans` 静态方法替换手写 switch，同时补一个"BPP 只应注册一次"的断言测试。

---

## 七、小结（引用要点）

- **两个层级的拦截，顺序由技术栈决定**：ORM 层（13）必然先于 JDBC 层（14），不是设计选择而是物理事实；可见信息不对称（语义信息在上、执行信息在下）决定了两层资源命名方式的本质差异。
- **同一个 `BeanSource` 工具类，两种用法，两种结果**：13-mybatis 复用静态方法（零 bug），14-druid 手写 switch（fallthrough bug）——这是本文最重要的发现，把 14-01 的 P2 bug 放进了"同源工具类误用"的更大背景下理解。
- **测试基建骨架同构、实现规模相差 3.4 倍**：接口签名完全一致，但组件发现机制（SPI 多态 vs 硬编码类型集合）不同，直接导致代码量差异——这个差异背后是"缓存复用换性能" vs "每次重建换隔离性"的测试策略取舍。
- **HikariCP vs Druid 不是优劣关系，是场景选择**：性能优先选 HikariCP，可观测性/治理能力优先选 Druid，18-dynamic 默认用 HikariCP 是场景决定，不代表 Druid 更差。
- **宏观架构一致、微观实现漂移**：同一作者的顶层设计模式（回调模板、双开关、JUnit5 骨架）高度同构，但具体机制的实现细节会因项目、时间点不同产生差异——这类细节漂移正是 bug 的潜伏地带。
