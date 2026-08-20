# C-6 Profile — 环境条件装配 (@Profile → 判定引擎 → 环境匹配)

> 依赖 C-5 注解元数据 | 🟡 Working | 6 KP | [模式: 条件策略 + 组合注解 + 惰性求值]

**读者处境**: `@Profile("dev")` 的 Bean 只在 dev 环境创建 — 谁判定？"当前环境"怎么定义？@Conditional 和 @Profile 什么关系？为什么 Spring Boot 的 @ConditionalOnXxx 也是一套机制？

### 1. @Profile 注解 + ConditionEvaluator 判定引擎

场景: `@Configuration @Profile("dev") class DevConfig` — 配置类解析时, Spring 需要决定"要不要加载这个类" — 入口是 ConfigurationClassParser 每解析一个类先问 ConditionEvaluator.shouldSkip。

源码路径:
- `Profile.java:110,111,116` — **@Profile**: `@Conditional(ProfileCondition.class)` + `String[] value()` — 组合注解: 只是"@Conditional+判定器"的语法糖
- `ConditionEvaluator.java:48,80` — **shouldSkip()**: L82-84 metadata 无 @Conditional→false(不跳过); L86-90 phase==null: 配置类候选→PARSE_CONFIGURATION 阶段, 否则 REGISTER_BEAN 阶段; L96-100 collectConditions→ConfigurationCondition 可声明阶段→(阶段匹配 && !matches)→跳过
- `ConditionEvaluator.java:119,121` — **条件实例化**: getConditionClasses 读 @Conditional.value(类名数组) → getCondition 反射实例化 Condition

关键设计: **Why 阶段(PARSE_CONFIGURATION/REGISTER_BEAN)分离？** 有些条件依赖"其他 Bean 是否已注册"(@ConditionalOnBean)— 配置类解析期(尚未注册)与 Bean 注册期语义不同 — ConfigurationCondition 让条件声明自己在哪个阶段生效, 避免误判。**Why 返回"shouldSkip"而非"是否匹配"？** 判定结果用于"跳过则丢弃配置" — 语义与决策点一致。[模式: 条件策略]

数据流: ConfigurationClassParser 解析 DevConfig → ConditionEvaluator.shouldSkip(DevConfig, PARSE_CONFIGURATION) → metadata.isAnnotated(@Conditional)→true → collectConditions: [ProfileCondition] → 无 ConfigurationCondition 阶段限制 → ProfileCondition.matches(env, metadata) → 结果决定 DevConfig 是否被处理。

### 2. ProfileCondition.matches — value 数组的"任一命中"语义

场景: `@Profile({"dev", "test"})` — 两个值是什么关系？只要 dev 或 test 任一激活就加载(OR), 不是 AND。

源码路径:
- `ProfileCondition.java:31,35` — **matches()**: L37 metadata.getAllAnnotationAttributes(Profile.class.getName()) → attrs.get("value") → L38-41 for 遍历: L39 environment.matchesProfiles(value) 任一 true→return true; 全不中→false; **无 @Profile→return true(不拦截)**
- `Environment.java:113` — **matchesProfiles(String...)**: 默认方法 → acceptsProfiles(Profiles) — 新 API 表达式: `"dev|prod"`(或) / `"!prod"`(非)

关键设计: **Why OR 而非 AND？** @Profile 的语义是"允许在哪些环境运行" — 多值=白名单并集; 若需"同时满足多环境"(几乎无场景)才用 AND。**Why 无 @Profile 默认 true？** Condition 只做"拦截": 没声明=不拦截 — 保证新类默认启用。[模式: 白名单并集]

数据流: @Profile({"dev","test"}) → matches → 遍历: matchesProfiles("dev") → env 激活 [dev] → true → 加载。@Profile("prod") 在 dev 环境 → matchesProfiles("prod") → false → 全部不中 → return false → shouldSkip=true → 配置类被跳过。

### 3. AbstractEnvironment.isProfileActive — "当前环境"的三层定义

场景: 激活的 profile 从哪来？spring.profiles.active 属性、程序 setActiveProfiles、没设置时用 defaultProfiles。

源码路径:
- `AbstractEnvironment.java:272` — **doGetActiveProfiles()**: activeProfiles 空→L279 doGetActiveProfilesProperty()(读属性 `spring.profiles.active`, 逗号分隔)→setActiveProfiles — **懒加载**: 首次访问才解析属性
- `AbstractEnvironment.java:77` — **ACTIVE_PROFILES_PROPERTY_NAME = "spring.profiles.active"** — 标准属性名, 启动器(如 Boot SpringApplication)也可主动读取并 setActiveProfiles
- `AbstractEnvironment.java:409` — **isProfileActive()**: validateProfile → doGetActiveProfiles().contains(profile) → 激活集空→defaultProfiles.contains(默认 "default" profile)
- `AbstractEnvironment.java:382` — **acceptsProfiles()**(老 API): `!` 前缀取反(L387) — 新代码走 Environment.matchesProfiles(Profiles)

关键设计: **Why active profiles 惰性解析？** 环境创建时属性源可能尚未就绪(Boot 装配顺序: 先建 Environment 再加载属性源) — 首次 getActiveProfiles 时才读属性, 保证"属性源齐了再求值"。**Why 空激活集回退 default？** "未显式激活任何环境"也是一种合法状态 — 默认全走 default profile, 与 @Profile("default") 的 Bean 兼容。[模式: 惰性求值]

数据流: @Profile("dev") 判定 → env.matchesProfiles("dev") → acceptsProfiles(Profiles) → profiles.matches(isProfileActive) → doGetActiveProfiles(): 空→读 spring.profiles.active 属性(启动参数 -Dspring.profiles.active=dev)→["dev"] → contains("dev")→true → Bean 加载。未设置任何激活 → 空→defaultProfiles=["default"] → @Profile("dev") false, @Profile("default") true。

→ 引出 0-7: TaskExecutor — 条件装配定了"哪些 Bean 在", 异步执行定"谁在什么线程跑" — 任务执行器家族(Sync/SimpleAsync/ThreadPool/Concurrent)。
