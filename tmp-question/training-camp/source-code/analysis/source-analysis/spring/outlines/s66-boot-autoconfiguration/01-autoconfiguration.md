# S-2 自动装配加载机制 — AutoConfigurationImportSelector (Boot 的灵魂)

> 依赖 s23 @Import + S-1 | 🔴 Deep | 6 KP | [模式: DeferredImportSelector + 责任链 + 元数据驱动]

**读者处境**: `@EnableAutoConfiguration` 之后发生了什么, 让引入 spring-boot-starter-web 就自动配好 DispatcherServlet?156 个自动装配类怎么被加载、过滤、排序的?

### 1. DeferredImportSelector 时机 + imports 文件读取

场景: 自动装配必须**晚于用户配置** — 否则 @ConditionalOnMissingBean(用户已配则跳过)无法判断。Spring 的 DeferredImportSelector 提供这个时机: 所有 @Configuration 解析完后统一处理。

源码路径:
- `AutoConfigurationImportSelector.java:77,153` — **DeferredImportSelector**: L153 getImportGroup → AutoConfigurationGroup(L423, Group 实现) — 在用户 @Configuration 全部解析后的 deferred 阶段, Group.process(L456) 统一处理
- `AutoConfigurationImportSelector.java:195` — **getCandidateConfigurations L195**: `ImportCandidates.load(org.springframework.boot.autoconfigure.AutoConfiguration.imports, classLoader)` → getCandidates — 读取各 jar 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件, 合并全部候选类名
- 文件实例: spring-boot-autoconfigure 自带一个 imports (含 WebMvcAutoConfiguration 等 156+ 候选)

关键设计: **Why DeferredImportSelector 而非普通 ImportSelector？** 时机: 普通 @Import 在解析早期执行 — 此时用户配置类还没注册, @ConditionalOnMissingBean(自动装配类依赖它)判断会误判"用户没配→自动装配生效"然后与用户配置冲突; Deferred 阶段用户 Bean 已注册, 条件评估才准确。**Why imports 文件而非 spring.factories？** 2.7 起 imports 文件更快(直接读列表, 无工厂名解析)且无需加载类。[模式: 延迟导入 + 清单文件]

数据流: @EnableAutoConfiguration → @Import(AutoConfigurationImportSelector) → 解析: getImportGroup→AutoConfigurationGroup → (用户配置解析完) → Group.process: getAutoConfigurationEntry → getCandidateConfigurations: ImportCandidates.load(imports) → [WebMvcAutoConfiguration, DataSourceAutoConfiguration, ...156个] → 继续过滤(§2)。

### 2. 两级过滤 — 元数据粗筛 + 条件评估精筛

场景: 156 个候选不能全注册 — 条件(类在不在 classpath/Bean 在不在/属性配没配)逐个评估太慢 — 分两级: 元数据粗筛(不加载类) + 完整条件精筛。

源码路径:
- `AutoConfigurationImportSelector.java:137,147` — **主流程**: getAutoConfigurationEntry L137: L142 getCandidateConfigurations → L144 getExclusions(exclude 读取) → L145-146 checkExcludedClasses+removeAll → L147 `getConfigurationClassFilter().filter(configurations)` → L148 fireAutoConfigurationImportEvents
- `AutoConfigurationImportSelector.java:270,274` — **过滤链**: L271 SpringFactoriesLoader 加载 AutoConfigurationImportFilter 实现(OnClassCondition/OnBeanCondition/OnPropertyCondition) → L274-278 组装 ConfigurationClassFilter
- `OnClassCondition.java:44,47` — **粗筛**: getOutcomes L47 — 只读 `spring-autoconfigure-metadata.properties`(每个自动装配类的 @ConditionalOnClass 元数据), 按类存在性批量判定, 多核线程化(L53) — **不加载类**
- `AutoConfigurationImportEvent` — **精筛**: 粗筛幸存者发布事件 → 条件评估器(AutoConfigurationImportSelector 内部)按完整条件(含 Bean 条件, 需访问容器)评估 → 不匹配移除

关键设计: **Why 两级过滤？** 性能: 元数据文件免加载类即可按"类在不在"粗筛(156→几十); 正确性: Bean 条件(@ConditionalOnMissingBean)必须等容器里用户 Bean 注册完才能评 — 精筛阶段做。粗筛用"自动装配元数据"而非加载类, 是 Boot 启动性能的关键优化。[模式: 两级过滤(粗筛+精筛)]

数据流: 156 候选 → ConfigurationClassFilter.filter: OnClassCondition.getOutcomes: 读元数据 → 按 @ConditionalOnClass 粗筛(如 DataSourceAutoConfiguration 需要 DataSource 类) → 幸存者 → fireAutoConfigurationImportEvents(发布 AutoConfigurationImportEvent) → 条件评估器精筛(含 OnBeanCondition: 用户已配 DataSource? 有则跳过) → 最终列表。exclude(X) → L144 getExclusions 读→L146 removeAll 从候选移除。

### 3. 排序与注册

场景: 幸存者也有依赖顺序(如 DataSourceAutoConfiguration 先于 JdbcTemplate 自动装配) — 怎么排序?最后怎么变成 Bean?

源码路径:
- `AutoConfigurationImportSelector.java:491,503` — **sortAutoConfigurations L491/503**: 用自动装配元数据, 按声明的 @AutoConfigureBefore/After(依赖)与 @AutoConfigureOrder(数值)拓扑排序
- 声明侧: 自动装配类标 `@AutoConfiguration(after = DataSourceAutoConfiguration.class)` 或 @AutoConfigureAfter/@AutoConfigureOrder
- 注册: 排序后的类名列表返回 → 由 s9 的 ConfigurationClassParser 作为 @Configuration 导入注册 → 进入正常配置类处理(条件注解 S-3 评估/Bean 方法注册)

关键设计: **Why 声明式排序？** 自动装配跨 jar 分布, 依赖关系必须由作者声明(after=xxx)而非框架硬编码; 拓扑排序保证"前置自动装配先注册"; 元数据驱动让排序不加载类。[模式: 声明式依赖 + 拓扑排序]

数据流: 幸存者列表 → sortAutoConfigurations: DataSourceAutoConfiguration(after=DataSourceTransactionManagerAutoConfiguration?) → 按 @AutoConfiguration(after=)/@AutoConfigureOrder 拓扑排序 → 有序类名列表 → ConfigurationClassParser 逐个当 @Configuration 解析 → BeanDefinition 注册 → 条件注解(S-3)在解析中评估 → 最终 Bean 就绪。

→ 引出 S-3: 条件注解 — 过滤链的 OnClassCondition/OnBeanCondition/OnPropertyCondition 就是 @ConditionalOnClass/@ConditionalOnBean/@ConditionalOnProperty 的实现 — 展开三个条件的评估细节。
