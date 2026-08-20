# S-23 Validation — ValidationAutoConfiguration (自动注册校验器 + MethodValidationPostProcessor)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | ValidationAutoConfiguration.java(84行)+PrimaryDefaultValidatorPostProcessor.java(90行)+FilteredMethodValidationPostProcessor.java(80行)+MessageInterpolatorFactory.java(90行, boot/validation)+LocalValidatorFactoryBean(spring-validation)
> 基线: BOOT-PLAN-v2 S-23 — 自动注册校验器; 前置: **C-22 Bean Validation(校验机制 AOP 织入/拦截/适配 — 复用)** — 展开 Boot 自动装配

---

## §0.8

- 🟡 Working，1篇 — 自动装配入口(ValidationAutoConfiguration: @AutoConfiguration + @ConditionalOnClass(ExecutableValidator) + @ConditionalOnResource(ValidationProvider SPI 文件) + @Import(PrimaryDefaultValidatorPostProcessor)) → defaultValidator 校验器(LocalValidatorFactoryBean 自动注册 + MessageInterpolatorFactory 消息插值 + customizers) → methodValidationPostProcessor(FilteredMethodValidationPostProcessor: Boot 差异 — MethodValidationExcludeFilter + spring.aop.proxy-target-class + spring.validation.method.adapt-constraint-violations)
- 设计模式: [模式: 条件装配]—@ConditionalOnClass/Resource; [模式: BPP 注册]—@Import PrimaryDefaultValidatorPostProcessor; [模式: 复用]—校验机制在 C-22

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ValidationAutoConfiguration.java:50,51,52 | 条件 | **@AutoConfiguration(L50)+@ConditionalOnClass(ExecutableValidator)(L51)+@ConditionalOnResource(ValidationProvider SPI)(L52)** — 有校验实现才装配 | High |
| ValidationAutoConfiguration.java:53,56,59 | defaultValidator | **@Import(PrimaryDefaultValidatorPostProcessor)(L53)+@Bean defaultValidator(L56/59)**: new LocalValidatorFactoryBean(L61)+MessageInterpolatorFactory(L64) | High |
| ValidationAutoConfiguration.java:69,71,73 | methodValidationPostProcessor | **@Bean(L69/71)**: FilteredMethodValidationPostProcessor(L73)+proxyTargetClass(L75)+adaptConstraintViolations(L78) | High |
| PrimaryDefaultValidatorPostProcessor.java:42,47,62 | primary | **class(L42)**: VALIDATOR_BEAN_NAME="defaultValidator"(L47); setPrimary(!hasPrimarySpringValidator())(L62) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: ValidationAutoConfiguration 是薄装配类(~84行) — 1篇 (~44行) 按"入口条件 → defaultValidator → methodValidationPostProcessor"展开; 校验机制(AOP 织入/拦截/适配)在 C-22 已讲, 本域只讲 Boot 怎么自动注册。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 自动装配条件 (@ConditionalOnClass + @ConditionalOnResource ValidationProvider) | 🔴 | **为什么🔴**: 何时装配校验 |
| P1-2 | defaultValidator 校验器 (LocalValidatorFactoryBean 自动注册) | 🔴 | **为什么🔴**: 默认校验器怎么来 |
| P1-3 | methodValidationPostProcessor (Filtered 版) | 🔴 | **为什么🔴**: 方法校验自动激活 |
| P2-1 | PrimaryDefaultValidatorPostProcessor (设为 primary) | 🟡 | **为什么🟡**: 默认校验器主选择 |
| P2-2 | 配置属性 (proxy-target-class / adapt-constraint-violations) | 🟡 | **为什么🟡**: 可调项 |
| P3-1 | 与 C-22 边界 (校验机制复用) | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **装配条件** | 🔴 | 何时装 |
| B | **校验器注册** | 🔴 | 默认校验器 |
| C | **方法校验+边界** | 🟡 | 自动激活 |

> **Cluster A (§1)**: ValidationAutoConfiguration 条件(@ConditionalOnClass/Resource) + @Import PrimaryDefaultValidatorPostProcessor
> **Cluster B (§2)**: defaultValidator(LocalValidatorFactoryBean + MessageInterpolatorFactory)
> **Cluster C (§3)**: methodValidationPostProcessor(Filtered 版差异) + 配置 + C-22 边界

→ 引出 S-24: Elasticsearch (降级) — Validation 之后: ElasticsearchRestClientAutoConfiguration 只讲接线(前置阶段3 ES)
