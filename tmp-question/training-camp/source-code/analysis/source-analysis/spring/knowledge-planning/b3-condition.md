# S-3 条件注解 — @ConditionalOnClass/@OnBean/@OnProperty (SpringBootCondition 家族)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | SpringBootCondition(约150行)+OnClassCondition(约150行)+OnBeanCondition(约420行)+OnPropertyCondition(约230行)+ConditionalOnClass+ConditionalOnBean+ConditionalOnProperty
> 基线: BOOT-PLAN-v2 S-3 — 自动装配的筛选器; 前置: **C-6 ConditionEvaluator (同一引擎, 复用) + S-2 过滤链** — 展开 Boot 的三个 @ConditionalOnXxx 条件实现

---

## §0.8

- 🟡 Working，1篇 — 模板(SpringBootCondition extends Condition: matches final→getMatchOutcome 抽象) → OnClassCondition(元数据粗筛: 类存在性, 不加载类) → OnBeanCondition(容器查 Bean: getBeanNamesForType, REGISTER_BEAN 阶段) → OnPropertyCondition(属性值检查: prefix+name/havingValue/matchIfMissing) → 与 C-6 衔接(同一 ConditionEvaluator 引擎, Boot 条件是它的实现)
- 设计模式: [模式: 模板方法]—SpringBootCondition; [模式: 条件策略]—各 OnXxxCondition; [模式: 阶段感知]—ConfigurationCondition(REGISTER_BEAN)

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SpringBootCondition.java:39,44,115 | 模板 | **基类**: matches L44 final(统一入口/日志)→getMatchOutcome L115 抽象(子类实现判定) | High |
| ConditionalOnClass.java:66,78,84 | 注解 | **@ConditionalOnClass**: value(类)L78/name(类名)L84 — 类在 classpath 才匹配 | High |
| OnClassCondition.java:44,47 | 类条件 | **getOutcomes L47**: 按自动装配元数据批量判定类存在性, 多核线程化(L53) — 不加载类(性能) | High |
| OnBeanCondition.java:85,123 | Bean 条件 | **OnBeanCondition**: implements ConfigurationCondition — getMatchOutcome L123: getBeanNamesForType(L213) 按类型查容器 Bean — 有/无 Bean 决定 | High |
| ConditionalOnBean.java:68 | 注解 | **@ConditionalOnBean/@OnMissingBean**: @Conditional(OnBeanCondition.class) — 容器有/无该 Bean | High |
| OnPropertyCondition.java:51,54 | 属性条件 | **getMatchOutcome L54**: 读注解 prefix/name/havingValue(L132)/matchIfMissing(L133) → resolver.getProperty 检查(L160) | High |
| ConditionalOnProperty.java | 注解 | **@ConditionalOnProperty**: prefix+name(属性名)/havingValue(期望值)/matchIfMissing(缺省时是否匹配) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 基类+三条件+三注解约 1200 行 — 知识主线: "SpringBootCondition 模板 → 三类条件各自的判定逻辑". 1篇 (~46行) 按"模板→类→Bean→属性"展开; C-6 的 ConditionEvaluator 引擎已讲, 此处只展开 Boot 条件实现(复用≠省略: 引擎复用, 实现展开)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | SpringBootCondition 模板 (matches final→getMatchOutcome 抽象) | 🔴 | **为什么🔴**: 所有 @ConditionalOnXxx 的统一骨架 — 与 C-6 Condition 接口衔接 |
| P1-2 | OnClassCondition (元数据粗筛, 不加载类) | 🔴 | **为什么🔴**: 最常用条件 — 类存在性判定 + 批量/线程化性能设计 |
| P1-3 | OnBeanCondition (getBeanNamesForType 查容器 + REGISTER_BEAN 阶段) | 🔴 | **为什么🔴**: @ConditionalOnMissingBean 的核心 — 自动装配不覆盖用户 Bean 的关键 |
| P2-1 | OnPropertyCondition (prefix/name/havingValue/matchIfMissing) | 🟡 | **为什么🟡**: 配置开关型条件 — 属性语义 |
| P2-2 | 与 C-6 ConditionEvaluator 衔接 (同一引擎) | 🟡 | **为什么🟡**: Boot 条件=引擎的实现, 阶段语义复用 |
| P3-1 | 注解族 (OnXxx 注解都是 @Conditional(条件类)) | 🟢 | **为什么🟢**: 注解→条件类的映射关系 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **模板与引擎** (SpringBootCondition + C-6) | 🔴 | 统一骨架 |
| B | **类/Bean 条件** (OnClass/OnBean) | 🔴 | 最常用两个条件 |
| C | **属性条件与注解族** (OnProperty + OnXxx 注解) | 🟡 | 配置开关与声明 |

> **Cluster A (§1)**: SpringBootCondition 模板 + 与 C-6 ConditionEvaluator 的衔接(复用引擎)
> **Cluster B (§2)**: OnClassCondition(元数据粗筛) + OnBeanCondition(容器 Bean 查询 + 阶段)
> **Cluster C (§3)**: OnPropertyCondition(属性检查) + 注解族映射

→ 引出 S-4: SpringApplication.run — 条件评估在配置解析时发生, 而这一切由 SpringApplication.run 启动 — 展开 Boot 启动全流程(与 s8 refresh 衔接)

(End of file - total 61 lines)
