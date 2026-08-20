# A-2 JSR-250 通用注解规范 — `@PostConstruct`/`@PreDestroy`/`@Resource` 的规范要求与 Spring 实现

> 依赖 S1-5 DI注入 + A-1 JSR-330 | 🟡 Working | 2 KP | [模式: 适配器]

**读者处境**: `@PostConstruct` 和 `@PreDestroy` 是 Java EE 注解，为什么在 Spring 里也能用？`@Resource` 和 `@Autowired` 有什么区别？这些注解是规范要求还是 Spring 扩展？

### 1. `@PostConstruct` / `@PreDestroy` — 规范要求的生命周期回调

场景: `@PostConstruct public void init()` — 在依赖注入完成后、Bean 正式可用前执行。`@PreDestroy public void cleanup()` — 在 Bean 销毁前执行。

源码路径:
- `CommonAnnotationBeanPostProcessor.java`（spring-context）— 处理 `@PostConstruct` / `@PreDestroy` / `@Resource`
- `InitDestroyAnnotationBeanPostProcessor.java` — 基类，处理生命周期注解的扫描和回调执行
- 在 `initializeBean` 的 `applyBeanPostProcessorsBeforeInitialization` 阶段调用 `@PostConstruct`
- 在 `destroyBean` 阶段调用 `@PreDestroy`

关键设计: **Why `@PostConstruct` 和 `@PreDestroy` 通过 `CommonAnnotationBeanPostProcessor` 处理？** 这些是 JSR-250 注解，Spring 通过 `CommonAnnotationBeanPostProcessor`（继承 `InitDestroyAnnotationBeanPostProcessor`）兼容。它和 Spring 自己的 `InitializingBean.afterPropertiesSet()` / `DisposableBean.destroy()` 最终进入同一条生命周期链，但来源不同——前者是规范契约，后者是 Spring 接口契约。

### 2. `@Resource` — JSR-250 的按名称注入

场景: `@Resource private UserService userService` — 先按字段名找 Bean，找不到再按类型找。

源码路径:
- `CommonAnnotationBeanPostProcessor.java` 处理 `@Resource` 注解
- `@Resource` 的注入逻辑：先 byName → 再 byType（与 `@Autowired` 的 byType → byName 相反）

关键设计: **Why `@Resource` 和 `@Autowired` 顺序相反？** `@Resource` 来自 JSR-250（Java EE）——设计为"组件名引用"（JNDI 传统），先按名字查找。`@Autowired` 是 Spring 自己的——设计为"类型驱动"（IoC 容器），先按类型查找。两者在 Spring 里通过不同的 BPP 处理：`CommonAnnotationBeanPostProcessor` 处理 `@Resource`，`AutowiredAnnotationBeanPostProcessor` 处理 `@Autowired`。

### 3. JSR-250 与 Spring 的生命周期契约边界

规范要求:
- `@PostConstruct`：依赖注入完成后、Bean 正式可用前调用
- `@PreDestroy`：容器关闭时、Bean 销毁前调用

Spring 额外提供:
- `InitializingBean.afterPropertiesSet()`：Spring 接口，语义等价于 `@PostConstruct`
- `DisposableBean.destroy()`：Spring 接口，语义等价于 `@PreDestroy`
- `init-method` / `destroy-method`：XML / `@Bean` 配置方式

关键设计: **Why 三种方式并存？** `@PostConstruct` / `@PreDestroy` 是规范契约，跨容器兼容；`InitializingBean` / `DisposableBean` 是 Spring 接口，强类型但侵入性强；`init-method` / `destroy-method` 是配置方式，无侵入但松散。Spring 统一处理这三者，但执行顺序有明确先后。

→ 引出 A-3: Servlet 规范与 Spring MVC 的契约边界（Servlet 生命周期、`HttpServlet.service()`、`Filter`）。
