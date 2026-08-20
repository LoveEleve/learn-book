# S2-9 @Conditional — PARSE_CONFIGURATION vs REGISTER_BEAN 两阶段评估

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 4文件/~439行
> 基线: S2-2 @Configuration — ConfigurationClassParser 中 shouldSkip 调用了 ConditionEvaluator

---

## §0.8

- 🟡 Working，1篇 — Condition 接口 → ConfigurationPhase 双阶段 → ConditionEvaluator → Spring Boot 自动装配条件基础
- 设计模式: [模式: 策略模式] — 每个 Condition 实现是独立策略; [模式: 模板方法] — shouldSkip 确定评估流程，子类 Condition 提供匹配逻辑
- Spring Boot @ConditionalOnClass/@ConditionalOnBean/@ConditionalOnMissingBean 都基于此

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Condition.java:59 | matches() | **核心接口**: matches(ConditionContext context, AnnotatedTypeMetadata metadata)→true(注册)/false(跳过) — context提供BeanFactory/Environment/ResourceLoader/ClassLoader | High |
| ConfigurationCondition.java:getConfigurationPhase() | 双阶段 | **PARSE_CONFIGURATION** vs **REGISTER_BEAN** — 前者在@Configuration类解析时判断(决定整个类是否跳过)，后者在@Bean注册时判断(决定单个@Bean/@ComponentScan是否跳过) | High |
| ConditionEvaluator.java:80-96 | shouldSkip() | **双阶段评估**: ①metadata非null→找@Conditional注解→获取Condition实现→②phase匹配(ConfigurationCondition.getConfigurationPhase())→③condition.matches(context, metadata)→false=skip→④回到调用方(Parser跳过类/Reader跳过@Bean) | High |
| ConditionEvaluator.java:70-71 | shouldSkip(metadata) | **单参重载**: 无phase参数→metadata instanceof AnnotationMetadata→PARSE_CONFIGURATION / else→REGISTER_BEAN — 根据metadata类型自动推断阶段 | High |

---

## 02-04 聚合+分类+聚类

### 聚合

**P1 核心 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | ConditionEvaluator.shouldSkip 双阶段评估 — PARSE_CONFIGURATION vs REGISTER_BEAN 的判定逻辑 | 🔴 | **为什么🔴**: @Conditional 的"开关"——ConfigurationPhase 决定Condition在哪个生命周期被评估——如果阶段不匹配，条件根本不执行——理解何时评估比理解条件内容更重要 |
| P1-2 | ConfigurationCondition vs Condition — 双阶段区分的必要性 | 🔴 | **为什么🔴**: 为什么需要两个阶段？@ConditionalOnClass在PARSE时判断(类存在→解析)，@ConditionalOnBean在REGISTER时判断(Bean注册→match) — 如果@ConditionalOnBean在PARSE阶段评估，相关Bean还没注册→永远返回false |

**P2 支持 (1):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | ConditionContext — BeanFactory/Environment/ResourceLoader/ClassLoader 四维度上下文 | 🟡 | **为什么🟡**: Condition评估需要运行时信息——BeanFactory查是否有Bean、Environment查属性、ClassLoader查类是否存在——context提供这四维 |

### 聚类 (1篇)

**1篇理由**: ~439行/4文件 — Condition接口(61行)+Conditional注解(70行)+ConditionEvaluator(240行)+ConditionContext(68行) — 概念简单(条件判断)但影响深远(Spring Boot自动装配)。1篇(~42行)覆盖接口→双阶段→Evaluator→Spring Boot示例。

**单篇结构**: §1 Condition 接口 + ConfigurationPhase 双阶段 → §2 ConditionEvaluator.shouldSkip 评估流程 → §3 两阶段分离的设计原因 + Spring Boot 自动装配示例
