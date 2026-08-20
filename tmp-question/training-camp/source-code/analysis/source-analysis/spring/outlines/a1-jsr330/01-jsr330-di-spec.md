# A-1 JSR-330 依赖注入规范 — `@Inject`/`@Named` 与 Spring `@Autowired` 的契约边界

> 依赖 S1-5 DI注入 | 🟡 Working | 2 KP | [模式: 适配器]

**读者处境**: 项目里有时写 `@Autowired`，有时写 `@Inject`——两者到底什么关系？JSR-330 是规范，Spring 是实现——规范层到底要求什么、Spring 又额外做了什么？

### 1. JSR-330 核心注解与 Spring 的对应关系

场景: `@Inject` / `@Named` / `@Qualifier`(javax) / `@Singleton`(javax) — 这些注解在 Spring 里都能用，但底层走的不是同一套处理器。

源码路径:
- `@Inject` → Spring 通过 `AutowiredAnnotationBeanPostProcessor` 处理（与 `@Autowired` 共用同一个 BPP）
- `@Named` → Spring 通过 `QualifierAnnotationAutowireCandidateResolver` 处理（等价于 `@Qualifier`）
- `@Singleton`(javax) → Spring 的默认 scope 就是 singleton，语义一致但不是同一套实现

关键设计: **Why Spring 能处理 JSR-330 注解？** Spring 的 `AutowiredAnnotationBeanPostProcessor` 不只扫描 `@Autowired`，还扫描 `@Inject`；`QualifierAnnotationAutowireCandidateResolver` 也同时识别 `@Named`。这样 Spring 在不修改自身核心架构的前提下，兼容了 JSR-330 规范。但这不是"Spring 实现了 JSR-330"，而是"Spring 在自己的注入体系里适配了 JSR-330 注解"。

### 2. JSR-330 与 Spring DI 的差异边界

场景: `@Inject` 没有 `required` 属性；`@Named` 没有 `@Qualifier` 的全部能力；`@Singleton` 不等于 Spring 的全套 singleton 管理。

关键差异:
- `@Inject` 必须注入，无 `required=false` 选项（需要 `Optional` / `@Nullable` 替代）
- `@Named` 只是名字限定，不能表达 `@Qualifier` 的全部元数据语义
- `@Singleton` 是 JSR-330 作用域声明，Spring 通过 `CommonAnnotationBeanPostProcessor` 处理，但不包含 Spring singleton 的三级缓存、提前暴露等扩展语义
- JSR-330 没有 `@Primary`、`@DependsOn`、`@Lazy` 等 Spring 特有控制信号

关键设计: **Why 差异不是"Spring 做多了"，而是"规范只覆盖最小契约"？** JSR-330 定义的是跨容器的最小注入契约——`@Inject` + `@Named` + `@Singleton`。Spring 在此基础上扩展了 `@Primary`、`@Lazy`、`@DependsOn`、`@Conditional` 等容器级控制信号。规范层是"所有兼容容器都该支持的底线"，Spring 的扩展是"Spring 容器额外提供的能力"。

数据流: `@Inject private UserService userService` → `AutowiredAnnotationBeanPostProcessor` 扫描到 `@Inject` → 与 `@Autowired` 共用同一套 `findAutowiringMetadata` → `resolveDependency` → `@Named("vip")` 走 `QualifierAnnotationAutowireCandidateResolver` → 最终注入。

→ 引出 A-2: JSR-250 通用注解规范（`@PostConstruct` / `@PreDestroy` / `@Resource`）。
