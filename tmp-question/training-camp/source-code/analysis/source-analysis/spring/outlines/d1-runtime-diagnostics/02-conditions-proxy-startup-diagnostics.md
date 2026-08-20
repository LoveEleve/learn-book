# D-1-2 条件注解 / 代理 / 启动慢 诊断 — Spring 运行时问题的第二类定位路径

> 依赖 D-1-1 + S2-9 @Conditional + A-4 自动代理 | 🟡 Working | 2 KP | [模式: 实践分析]

**读者处境**: 某个 Bean 为什么没注册？某个 Service 为什么没被代理？启动为什么慢？这些问题表面不同，但最终都要回到条件装配、自动代理和 Bean 创建主线去定位。

### 1. 条件注解诊断：为什么 Bean 没有注册

场景: `@ConditionalOnProperty` 没生效、`@ConditionalOnBean` 条件不满足。

诊断入口:
- `ConditionEvaluationReport`
- `/actuator/conditions`（Boot）
- DEBUG 日志：`org.springframework.boot.autoconfigure.condition`

关键点:
- 区分 PARSE_CONFIGURATION vs REGISTER_BEAN
- 检查类路径、属性值、Bean 是否已注册

### 2. AOP 代理诊断：为什么 `@Transactional` / `@Async` / `@Cacheable` 不生效

诊断入口:
- `AopUtils.isAopProxy(bean)` / `AopUtils.isCglibProxy(bean)`
- `bean.getClass()` 看是否是 `$$EnhancerBySpringCGLIB` 或 `$Proxy`
- 检查 `AbstractAutoProxyCreator.wrapIfNecessary` 路径

关键点:
- 自调用绕过代理
- final/private 方法不代理
- @Aspect 自身不会被代理

### 3. 启动慢诊断：Bean 创建耗时、重型初始化、懒加载策略

诊断入口:
- Bean 创建日志
- 观察 `preInstantiateSingletons()` 的热点 Bean
- 识别 `@PostConstruct` / `afterPropertiesSet` 内部的重型逻辑

关键点:
- `@Lazy` 是否可降低启动峰值
- AOT / classpath index / 组件扫描范围是否可优化
- `SmartInitializingSingleton` 是否承担了耗时工作

→ 引出 D-2: 启动加速与运行时性能优化。
