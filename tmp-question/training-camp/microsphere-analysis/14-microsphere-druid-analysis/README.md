# 14 - microsphere-alibaba-druid 深度分析

## 源码信息

- **本地路径**: `/data/workspace/java-training-camp/cloud-native-code/share/microsphere-alibaba-druid`
- **GitHub**: https://github.com/microsphere-projects/microsphere-alibaba-druid
- **规模**: 19 个主 Java 文件 + 18 个测试，8 个 Maven 模块（core / spring / spring-boot / spring-cloud / test / spring-test / parent / dependencies）
- **依赖**: druid 1.2.28
- **git**: 386 提交，2025-02-14 创建，2026-07 活跃（0.2.19）

## 探索结论（全部源码验证）

- **是什么**：Druid 连接池的"Statement 过滤器模板化"扩展——把 Druid 官方 `Filter` 接口的 491 个方法（javap/源码双重验证）精确收敛为 `beforeExecute`/`afterExecute` 两个回调 + `execute(statement, callable)` 模板（`AbstractStatementFilter extends FilterAdapter`），子类只需覆写两个方法；覆盖 `PreparedStatement`/`Statement`，不覆盖 `CallableStatement`（存储过程调用不受管）
- **装配**：`@EnableAlibabaDruid`（filterClasses + BeanSource 三源）→ Registrar 注册 Filter beans + `DruidDataSourceBeanPostProcessor`（排序后塞进每个 `DruidDataSource.getProxyFilters()`）→ spring-boot 自动配置（BPP bean + actuator 池指标 `DruidDataSourcePoolMetadata`）
- **与 13 的同构与差异**：execute 模板的 try/catch/finally + result/failure **骨架结构**与 `InterceptorsExecutorFilterAdapter`（13-01）同构，但捕获范围（`Throwable` vs `SQLException`）和异常归一化逻辑（`wrap()` 类型判断 vs 原样 `throw`）不同——同一设计思想在不同异常契约下的适配，不是"完全同构"；@EnableXxx + Registrar + 三源扫描、JUnit5 测试基建（Extension + Runtime 注解）是作者惯用模式的第三个实例
- **同一 `BeanSource` 工具类，两种用法（14-02 核心发现）**：13-mybatis 的 `MyBatisExtensionBeanDefinitionRegistrar` 直接复用 `BeanSource.registerBeans` 静态方法（枚举多态分派，零 bug）；14-druid 的 `AlibabaDruidRegistrar` 自己手写了一个 for + switch（fallthrough bug 的根源）——同一作者、同一工具类，两种实现路径导致两种结果
- **问题**：双开关冗余（Properties.enabled vs 条件注解）、spring-cloud 空壳（自动配置层面，真正内容在 features.yaml）、**switch fallthrough 导致 BPP 重复注册**（`AlibabaDruidRegistrar.registerFilterBeans` 第 67-79 行，`SPRING_FACTORIES`/`JAVA_SERVICE_PROVIDER` 分支缺 `break`，穿透到 `default` 重复调用 `registerDruidDataSourceBeanPostProcessor`；`@EnableAlibabaDruid` 默认 `sources={BEAN_FACTORY, SPRING_FACTORIES}` 即触发；提交 `81b8043b` 2026-05-30 引入，测试未覆盖此断言，因下游 Bean 去重才未暴露为运行时错误）、**`@AlibabaDruidTest.filters()` 死代码**（`AlibabaDruidTest.java:117` 声明了该属性，javadoc 说明用于 `DruidDataSource.setFilters(String)`，但 `AlibabaDruidTestExtension.buildDruidDataSource(AlibabaDruidTest)` 第 219-228 行只读取 `propertiesResource()`/`properties()`，从未读取 `filters()` 或调用 `setFilters`；全仓库无任何测试使用该属性）、`LoggingStatementFilter.beforeExecute` 日志占位符数量不匹配（纯格式瑕疵）
- **能力边界澄清（写作中纠偏）**：`WallFilter`（SQL 防火墙）、`StatFilter`（慢查询统计）是 **Druid 官方生态**自带能力，`microsphere-alibaba-druid` 项目**没有对它们做任何封装或引用**——项目本身只落地了 `LoggingStatementFilter` 一个内置样板，写作时需明确区分"Druid 官方生态能力"与"microsphere 项目已验证能力"，避免混淆
- **对照素材**：与 13 的"Executor 层 vs JDBC 层"（顺序由技术栈层级决定，非设计选择）、与 18-06 的"HikariCP vs Druid 连接池"（性能优先 vs 可观测性扩展性优先的场景选择，非优劣关系）

## 文章规划（2 篇，已全部完成 + 深度 review）

### 14-01 核心机制与 Spring 装配 ✅
文件：`14-01-core-mechanism.md`
> 核心命题：Druid Filter 接口 491 个方法如何被精确收敛成两个回调；三源扫描 + BPP 如何把过滤器装进连接池
- 官方基线：Druid `Filter`（491 方法，8 前缀分组）/`FilterAdapter`（默认透传）/`AutoLoad`（SPI 自动加载语义）机制（1.2.28 官方源码验证）
- `AbstractStatementFilter` 解剖：init（DataSource + validationSQL 缓存）、**13 个** final 执行方法收敛（7 个方法名、13 个具体重载签名）、`execute` 模板（buildResourceName → before → call → after，`wrap()` 类型判断而非无差别包装）、resourceName 可覆写化演进（含一次真实 bug-fix：`getFrom()`→`getTableSource()`）
- `LoggingStatementFilter`（内置样板，含日志占位符瑕疵 P4）
- `@EnableAlibabaDruid` + Registrar 三源装配（含 switch fallthrough bug P2）+ `DruidDataSourceBeanPostProcessor`（getSortedBeans + sort + getProxyFilters().addAll）
- 自动配置：`AlibabaDruidProperties`（双开关冗余 P1）、`DruidDataSourcePoolMetadata`（actuator 池指标）、两级条件注解、spring-cloud 空壳（P3）
- 4 个问题（P1-P4）+ 测试佐证 + 小结

### 14-02 与 13/18 的对照、测试基建与生产评估 ✅
文件：`14-02-comparison-and-testing.md`
> 核心命题：同一 SQL 的两级拦截（MyBatis Executor 层 vs Druid JDBC 层）怎么分工；同一个 `BeanSource` 工具类为什么在 13/14 两个项目里产生了不同结果
- **与 13 的层级对照**：顺序为何固定（技术栈层级决定的物理事实）、可见信息不对称（语义信息在上层/执行信息在下层，决定两层资源命名方式差异）、防重复挂载逻辑不同源但目标一致
- **装配机制对照（核心发现）**：13-mybatis 直接调用 `BeanSource.registerBeans` 静态方法（枚举多态，零 bug）vs 14-druid 自己手写 switch（fallthrough bug 根源）——把 14-01 的 P2 bug 放进"同源工具类两种用法"的更大背景下解释
- **测试基建对照**：接口签名完全一致（5 个 JUnit5 扩展点），组件发现机制不同（SPI resolver 多态 vs 硬编码类型集合，两者在"子类型支持"上无本质区别），规模差 3.4 倍（142 行 vs 488 行，"缓存复用换性能" vs "每次重建换隔离性"的策略取舍）
- **与 18-06 的对照**：HikariCP vs Druid（连接池定位差异、actuator 指标暴露方式；SQL 防火墙/慢查询统计等能力已标注为 Druid 官方生态而非本项目实现）
- **作者惯用模式总结**：before/after 回调、三源扫描、JUnit5 测试基建、双开关体系——13/14 两个项目的宏观架构高度同构、微观实现细节存在漂移（bug 潜伏地带）
- 测试基建：`AbstractAlibabaDruidTest`（H2）、`AlibabaDruidTestExtension`（类/方法级 DataSource 生命周期 + get-or-build）、`@AlibabaDruidTest`/`@AlibabaDruidRuntime`、测试桩（AutoLoadFilter/LoadFilter/TestStatementFilter）、**`@AlibabaDruidTest.filters()` 死代码问题**
- 生产评估：适用场景、风险清单（汇总 14-01 P1-P4 + 测试桩广度够深度不够）、项目活性

## 与 13 的引用约定

- 层级对照结论已在 13-03 第四节预埋（"两个层级的拦截"），14-02 已展开细节
- 作者惯用模式清单（回调/三源/测试基建/双开关）以 14 为第三个实例归纳，14-02 已完成对照
- **写作中的关键发现**：13 与 14 共享 `BeanSource` 这同一个工具类，但装配代码的实现路径不同（13 复用静态方法，14 手写分支逻辑）——这是"宏观架构一致、微观实现漂移"的典型案例，对未来分析其他 microsphere 项目（07-sentinel 等）时有参考价值：跨项目对照要落到具体调用链，不能停留在"看起来像"的直觉判断
