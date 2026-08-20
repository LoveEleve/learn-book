# C-6 Profile — 环境条件装配 (@Profile → ConditionEvaluator → matchesProfiles)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | Profile(118行)+ProfileCondition(48行)+ConditionEvaluator(240行)+AbstractEnvironment(601行)
> 基线: C-5 注解元数据 — @Profile 注解读取走 MergedAnnotation 体系 — 本域展开条件装配的第一课: 注解得 annotation 了, 判定谁做、环境怎么匹配; 原始执行计划 0-6

---

## §0.8

- 🟡 Working，1篇 — 声明(@Profile(value=[...]) 是 @Conditional(ProfileCondition.class) 的组合注解) → 判定器(ConditionEvaluator.shouldSkip: 阶段选择→collectConditions→condition.matches) → 匹配规则(ProfileCondition: 遍历 value→environment.matchesProfiles) → 环境匹配(AbstractEnvironment: isProfileActive→activeProfiles.contains || 空时 defaultProfiles; doGetActiveProfiles 懒加载 spring.profiles.active 属性)
- 设计模式: [模式: 策略/条件]—Condition 接口统一判定; [模式: 组合注解]—@Profile=@Conditional+Condition; [模式: 惰性求值]—active profiles 首次访问才解析属性

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Profile.java:110,111,116 | @Profile 注解 | **组合注解**: `@Conditional(ProfileCondition.class)` + `String[] value()` — 本质是"条件注解的语法糖" | High |
| ProfileCondition.java:31,35 | matches() | **匹配规则**: metadata.getAllAnnotationAttributes(Profile) → 遍历 value 数组 → context.getEnvironment().matchesProfiles(value) → 任一命中即 true; 无 @Profile → true(不拦截) | High |
| ConditionEvaluator.java:48,80 | shouldSkip() | **判定入口**: L82-84 无 @Conditional → false; L86-90 phase==null→配置候选类用 PARSE_CONFIGURATION / 否则 REGISTER_BEAN; L96-100 collectConditions→ConfigurationCondition 的阶段过滤→!matches→true(跳过) | High |
| ConditionEvaluator.java:119,121 | collectConditions/getCondition | **条件实例化**: 读 @Conditional 的 value(类名数组) → 反射 newInstance | High |
| Environment.java:113 | matchesProfiles() | **默认方法**: matchesProfiles(String...) → acceptsProfiles(Profiles) — 新 API 支持 `!prod` 否定与 `A|B` 或语义 | High |
| AbstractEnvironment.java:382,409 | acceptsProfiles/isProfileActive | **环境匹配**: isProfileActive: validateProfile → doGetActiveProfiles().contains(profile) || (activeProfiles 空→defaultProfiles.contains) — `!` 前缀取反(L387) | High |
| AbstractEnvironment.java:272,77 | doGetActiveProfiles | **懒加载**: activeProfiles 空→读 ACTIVE_PROFILES_PROPERTY_NAME("spring.profiles.active") 属性(L77, 逗号分隔) → setActiveProfiles — 首次访问才解析 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 注解+条件+求值器+环境共 1000 行 — 链路单线: "@Profile → shouldSkip → matches → isProfileActive". 1篇 (~45行) 按"声明→判定→匹配"展开; 若分 2 篇则注解语义与求值器割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | ConditionEvaluator.shouldSkip 全流程 (无 @Conditional→false / 阶段选择 / 条件遍历) | 🔴 | **为什么🔴**: 所有 @Conditional(@Profile/@ConditionalOnXxx) 的统一判定引擎 — Boot 条件注解也挂在这套机制上 |
| P1-2 | ProfileCondition 匹配规则 (value 数组任一命中→true) | 🔴 | **为什么🔴**: @Profile 的 or 语义 — value 是"任一激活即可"而非"全部激活" |
| P1-3 | AbstractEnvironment 环境匹配 (isProfileActive: 显式集合→spring.profiles.active 懒加载→default 兜底) | 🔴 | **为什么🔴**: "当前环境是什么"的定义 — 激活 profile 的三层来源与顺序 |
| P2-1 | @Profile 组合注解设计 (@Conditional+Condition) | 🟡 | **为什么🟡**: 注解组合的范例 — 与 C-5 @AliasFor 衔接 |
| P2-2 | 阶段语义 (PARSE_CONFIGURATION vs REGISTER_BEAN) | 🟡 | **为什么🟡**: 条件判定时机 — 配置类解析期 vs Bean 注册期 (条件类可声明阶段) |
| P3-1 | matchesProfiles 新 API (Profiles 表达式: !否定/A|B) | 🟢 | **为什么🟢**: 现代写法 — 老 acceptsProfiles(String...) 已废弃 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **判定引擎** (ConditionEvaluator + 阶段) | 🔴 | 条件装配的骨架 — 阶段与遍历规则 |
| B | **匹配规则** (ProfileCondition + 环境 isProfileActive) | 🔴 | @Profile 的语义与"激活"定义 |
| C | **声明与 API** (@Profile 组合 + matchesProfiles 新语法) | 🟡 | 使用侧语法 |

> **Cluster A (§1)**: @Profile 注解(组合注解) + ConditionEvaluator.shouldSkip(阶段/遍历)
> **Cluster B (§2)**: ProfileCondition.matches + AbstractEnvironment.isProfileActive/matchesProfiles
> **Cluster C (§3)**: 激活来源链(显式 setActiveProfiles→spring.profiles.active 属性→defaultProfiles) + 使用场景(dev/prod 配置切换)

→ 引出 0-7: TaskExecutor — 条件装配解决了"哪个 Bean 存在", 异步执行解决"谁跑在什么线程" — 任务执行器家族(Sync/SimpleAsync/ThreadPool/Concurrent)

(End of file - total 61 lines)
