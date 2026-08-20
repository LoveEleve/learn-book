# S-3 条件注解 — @ConditionalOnClass/@OnBean/@OnProperty

> 依赖 C-6 ConditionEvaluator + S-2 | 🟡 Working | 6 KP | [模式: 模板方法 + 条件策略 + 阶段感知]

**读者处境**: `@ConditionalOnClass(DataSource.class)` 决定 DataSource 自动装配是否生效 — 条件怎么评估?类/Bean/属性三种条件各怎么判?和 Spring 的 @Conditional 什么关系?

### 1. SpringBootCondition 模板 + 与 C-6 引擎衔接

场景: 所有 @ConditionalOnXxx 的判定都走一个模板: 统一入口(matches)→子类实现(getMatchOutcome)。引擎是 C-6 的 ConditionEvaluator — Boot 只是它的实现。

源码路径:
- `SpringBootCondition.java:39,44,115` — **模板**: extends Condition — matches L44 `final`(统一入口: 调 getMatchOutcome + 记录日志)→ getMatchOutcome L115 抽象(子类实现具体判定)
- 引擎衔接: C-6 的 ConditionEvaluator.shouldSkip 遍历 @Conditional 条件类调用 matches — SpringBootCondition 是 Condition 的实现, 天然被引擎驱动
- 注解→条件映射: `@ConditionalOnXxx` 都带 @Conditional(OnXxxCondition.class) — 声明式挂接

关键设计: **Why matches 设 final？** 条件评估的"统一流程"(入口/日志/异常处理)固定, 子类只写"判定逻辑"(getMatchOutcome)— 与 C-6 ProfileCondition 同一模式; 复用 C-6 引擎(shouldSkip/阶段), Boot 条件=引擎的新实现 — **机制复用, 实现展开**(06 §2.5)。[模式: 模板方法]

数据流: 配置类解析 → C-6 ConditionEvaluator.shouldSkip(metadata, phase) → 遍历 @Conditional 条件 → SpringBootCondition.matches(L44) → getMatchOutcome(context, metadata) → ConditionOutcome(match/不match+原因) → 引擎决定跳过与否。

### 2. OnClassCondition / OnBeanCondition — 类与 Bean 条件

场景: `@ConditionalOnClass(DataSource.class)`(classpath 有 DataSource 才生效) vs `@ConditionalOnMissingBean(DataSource.class)`(容器没配才生效) — 判定对象完全不同。

源码路径:
- `OnClassCondition.java:44,47` — **类条件**: getOutcomes L47 — 按**自动装配元数据**(spring-autoconfigure-metadata.properties)批量判定类存在性 — 多核时线程化(L53) — **不加载类**(性能)
- `ConditionalOnClass.java:66,78,84` — **注解**: value(Class[] L78)/name(String[] L84) — 任一存在即匹配
- `OnBeanCondition.java:85,123` — **Bean 条件**: implements ConfigurationCondition(REGISTER_BEAN 阶段) — getMatchOutcome L123 → L213 getBeanNamesForType(按类型查容器已有 Bean) → 有/无决定匹配
- `ConditionalOnBean.java:68` — **注解**: @ConditionalOnBean/@OnMissingBean → @Conditional(OnBeanCondition.class)

关键设计: **Why Bean 条件在 REGISTER_BEAN 阶段？** 判断"用户是否已配 Bean"必须等用户 Bean 注册完成 — ConfigurationCondition 声明阶段保证评估时机(C-6 已讲阶段语义); **Why 类条件不加载类？** 读元数据文件判"类在不在"比 Class.forName 快 — 自动装配批量筛选的性能关键。[模式: 元数据驱动 + 阶段感知]

数据流: @ConditionalOnClass(DataSource.class) → OnClassCondition.getOutcomes: 查元数据 → DataSource 在 classpath → match。@ConditionalOnMissingBean(DataSource) → OnBeanCondition.getMatchOutcome(REGISTER_BEAN 阶段): getBeanNamesForType(DataSource) → 用户已配 dataSource bean → 不匹配 → 自动装配跳过(不覆盖用户配置)。

### 3. OnPropertyCondition — 属性开关

场景: `@ConditionalOnProperty(name="spring.redis.enabled", havingValue="true")` — 配置属性控制自动装配开关。

源码路径:
- `OnPropertyCondition.java:51,54` — **getMatchOutcome L54**: 解析注解属性 → prefix/name(属性路径) → resolver.getProperty(key) 检查(L160)
- `OnPropertyCondition.java:132,133` — **语义**: havingValue(L132, 期望值 — 未指定时"非 false 即匹配")/matchIfMissing(L133, 属性缺失时是否匹配)
- `ConditionalOnProperty.java` — **注解**: prefix+name/value(多属性)/havingValue/matchIfMissing

关键设计: **Why havingValue/matchIfMissing 灵活？** 开关三种形态: ①属性存在即开(不写 havingValue, 非 false 即匹配) ②必须等于某值(havingValue="true") ③属性缺失时的默认(matchIfMissing=true 让默认开启) — 满足"默认开/默认关"两种需求。[模式: 属性断言]

数据流: @ConditionalOnProperty(prefix="spring.redis", name="enabled", havingValue="true", matchIfMissing=false) → getMatchOutcome → resolver.getProperty("spring.redis.enabled") → 未配置 → matchIfMissing=false → 不匹配 → Redis 自动装配跳过。配置 enabled=true → 匹配 → 生效。

→ 引出 S-4: SpringApplication.run — 条件评估发生在配置解析, 而配置解析由 run() 启动 — 展开 Boot 启动全流程(与 s8 refresh 衔接)。
