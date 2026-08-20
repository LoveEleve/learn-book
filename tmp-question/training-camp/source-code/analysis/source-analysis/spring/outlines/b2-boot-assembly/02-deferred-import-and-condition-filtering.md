# B-2-2 DeferredImportSelector + 条件过滤 — Boot 自动装配为什么要延迟到更晚处理

> 依赖 B-2-1 + S2-9 @Conditional | 🟡 Working | 2 KP | [模式: 延迟收敛 + 条件裁决]

**读者处境**: `AutoConfigurationImportSelector` 为什么不是普通 `ImportSelector`，而是 `DeferredImportSelector`？为什么 Boot 自动装配要等其他配置类解析完以后才做条件过滤？

### 1. `AutoConfigurationImportSelector` 作为 `DeferredImportSelector`

场景: `@EnableAutoConfiguration` 不应过早导入 100+ 自动配置类，否则 `@ConditionalOnBean` / `@ConditionalOnMissingBean` 的判断还没有足够上下文。

源码路径:
- `AutoConfigurationImportSelector` 实现 `DeferredImportSelector`
- `ConfigurationClassParser.DeferredImportSelectorHandler` 在所有普通配置类解析后再统一处理
- `selectImports(AnnotationMetadata)` 读取 `AutoConfiguration.imports`

关键设计: **Why 不能用普通 ImportSelector？** 普通 `ImportSelector` 在当前配置类解析时立即生效，但 Boot 自动装配要先看到“用户自己已经注册了什么”，才能决定哪些自动配置需要让位，所以必须晚一点。

### 2. 条件过滤：`@ConditionalOnClass` / `@ConditionalOnBean` / `@ConditionalOnMissingBean`

场景: 读取到 100+ 自动配置类后，Boot 并不会全注册，而是逐个过条件链：
- 类路径满足吗
- 用户自己已经有这个 Bean 吗
- 某个属性是否开启

源码路径:
- `ConditionEvaluator`（Framework）
- Boot 的 `OnClassCondition` / `OnBeanCondition` / `OnPropertyCondition`
- `ConditionEvaluationReport` 记录哪些配置被跳过、为什么跳过

关键设计: **Why 条件过滤是 Boot 自动装配的核心而不是附属能力？** 自动装配不是“帮你多注册 Bean”，而是“在当前应用已有配置的前提下，缺什么补什么”。条件系统正是这套行为的裁决器。

→ 引出 C-1-1: `ApplicationContext` 生命周期总图。
