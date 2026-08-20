# S2-5 父子容器 — getBean 的 parent 委托链

> 依赖 S2-4 MessageSource | 🟡 Working | 3 KP | [模式: 责任链 + 组合模式]

**读者处境**: S2-4 中 getMessageFromParent 调用了 parent.getMessage() — 但 parent 是什么？谁设置的？为什么 S2-2 中父子容器被反复提到？这篇回答"父子关系怎么建立"和"Bean 怎么从 parent 查找"。

### 1. setParent — 建立父子关系的"三线同步"

场景: Spring Boot `SpringApplication.run()` → `context.setParent(environmentContext)` 把环境上下文设为 Web 容器上下文的父亲 — 三个子系统(BeanFactory/Environment/MessageSource)的 parent 最终一致 — 但**不是** setParent 一个方法内同步: Environment 在 setParent 内 merge — BeanFactory 的 parent 在 GenericApplicationContext.setParent 覆写/AbstractRefreshableApplicationContext.createBeanFactory 建立 — MessageSource 的 parent 在 refresh() 的 initMessageSource 建立。

源码路径:
- `AbstractApplicationContext.java:538-543` — **setParent(ApplicationContext)**: ①`this.parent = parent`(L539) ②若 parent 非 null → 仅同步 Environment: `parent.getEnvironment()` instanceof ConfigurableEnvironment → `getEnvironment().merge(parentEnvironment)`(L540-543) — **无任何 EnvironmentAware/ResourceLoaderAware 注入** — ③本方法内**不**同步 BeanFactory/MessageSource 的 parent(见下两条)
- `GenericApplicationContext.java:165-167` — **setParent 覆写**: `super.setParent(parent)` 之后调 `this.beanFactory.setParentBeanFactory(getInternalParentBeanFactory())`(L167) — BeanFactory parent 的同步点之一; AbstractRefreshableApplicationContext 则在 `createBeanFactory()`(L197-198) 创建 DefaultListableBeanFactory 时传入 — MessageSource 的 parent 由 refresh() 中 `initMessageSource()`(L817-829) 的 `hms.setParentMessageSource(getInternalParentMessageSource())`(L826) 建立
- `AbstractApplicationContext.java:1517-1519` — **getInternalParentBeanFactory()**: parent 是 ConfigurableApplicationContext → `cac.getBeanFactory()`(L1518-1519, 直接取内部 BeanFactory, 跳过重复 getParent()) — 非 ConfigurableApplicationContext → `getParent()` 自身作为 BeanFactory
- `AbstractBeanFactory.java:191-192` — **AbstractBeanFactory(parentBeanFactory)**: 构造函数设置 `this.parentBeanFactory = parentBeanFactory` — Bean 查找的 parent 链在这个字段建立 — getBean 时不是查getParent()而是查parentBeanFactory

关键设计: **Why 三线同步而非单线？** BeanFactory.getBean/getMessage/Environment 是三个独立的子系统 — 如果只设置一个 parent，其他两个的查找方向可能不一致。三处同步点分布在 AbstractApplicationContext 与其子类中(setParent merge / setParent 覆写 / initMessageSource) — 但全部以 `getInternalParentBeanFactory()` 为统一入口 — 最终保证: parent 链的语义在所有子系统中统一[模式: 组合模式]

数据流: context.setParent(parentContext) → L539 this.parent = parent → L540-543 parent.getEnvironment() instanceof ConfigurableEnvironment → getEnvironment().merge(parentEnvironment) → (GenericApplicationContext) setParent 覆写 L167: beanFactory.setParentBeanFactory(getInternalParentBeanFactory()) → L1518 parent instanceof ConfigurableApplicationContext → true → cac.getBeanFactory() → 返回 parent 的 DefaultListableBeanFactory → beanFactory.parentBeanFactory = parent 的 BeanFactory → refresh() 时 initMessageSource: L826 hms.setParentMessageSource(getInternalParentMessageSource()) → 三线同步完成

### 2. getBean parent 委托 — containsBean vs containsLocalBean

场景: 父容器有一个 `DataSource` bean — 子容器调用 `containsBean("dataSource")` 返回 true(会查父容器) — `containsLocalBean("dataSource")` 返回 false(只看自己) — `getBean("dataSource")` 从父容器获取 DataSource 实例。这三者的差异是父子容器最容易混淆的点。

源码路径:
- `AbstractBeanFactory.java:272-288` — **doGetBean() 的 parent 委托**: ①本地 `transformedBeanName(name)`(L246) → `parentBeanFactory != null && !containsBeanDefinition(beanName)`(L272-273, 本地没定义) ②parent 路径: parent 是 AbstractBeanFactory → `return abf.doGetBean(nameToLookup, requiredType, args, typeCheckOnly)`(L277, 直调内部方法 — requiredType/args 原样传入, 转换后的原始名 nameToLookup 传入, parent 内部仍会再执行 transformedBeanName/getSingleton/getObjectForBeanInstance) / 不是 → 按 args/requiredType 分派 `parentBeanFactory.getBean(...)`(L281-288, 走标准路径)
- `AbstractBeanFactory.java:431-438` — **containsBean(name)**: containsSingleton/containsBeanDefinition → 都没有? → `parentBeanFactory != null && parent.containsBean(name)` → 递归查 parent
- `AbstractBeanFactory.java:809-812` — **containsLocalBean(name)**: `(containsSingleton(beanName) || containsBeanDefinition(beanName))` 且对工厂引用做 `isFactoryDereference/isFactoryBean` 校验(L811-812) — **不查 parent** — 这才是"本地"的含义(接口声明见 HierarchicalBeanFactory.java:51)

关键设计: **Why getBean parent 委托优化 AbstractBeanFactory→直调 doGetBean？** parent 是 AbstractBeanFactory 时省的只是 parent 侧 getBean 的包装帧 — 直接进入 doGetBean 骨架。transformedBeanName(L246)、getSingleton、getObjectForBeanInstance、类型检查在 parent 的 doGetBean 内照常执行 — requiredType 原样传入(L277), 并不跳过。这是调用栈的微观优化 — 父子容器链在大型应用中可能5-6层深 — 每层省1层包装帧。

数据流: childContext.getBean("dataSource") → AbstractBeanFactory.getBean(L201-202)→doGetBean(L242) → L246 transformedBeanName → L250 getSingleton("dataSource") → singletonObjects.get() → null(本地没) → L273 parentBeanFactory != null && !containsBeanDefinition("dataSource") → L276 parentBeanFactory instanceof AbstractBeanFactory → true → L277 return abf.doGetBean("dataSource", requiredType, null, false) → parent 的 doGetBean 内 getSingleton→找到 DataSource→getObjectForBeanInstance→返回 → 结果沿 L277 直接返回给调用方(child 侧不再经过 getObjectForBeanInstance)

### 3. 性能优化 + Spring Boot 父子容器模式

场景: Spring Boot 启动 — `SpringApplication` 创建一个 `bootstrap` ApplicationContext 作为 parent → 创建 Web-Server ApplicationContext 作为 child — parent 持有共享组件(DataSource, 配置) → child 持有 Web 组件(Controller, Interceptor)。getBean 沿 parent 链查找 — getInternalParentBeanFactory 是"快线"(ConfigurableApplicationContext→直接取BeanFactory → 跳过getParent()中间层)。

源码路径:
- `AbstractApplicationContext.java:1517-1519` — **getInternalParentBeanFactory**: parent instanceof ConfigurableApplicationContext → 直接 `cac.getBeanFactory()` — 跳过 `getParent()` 方法(可能被覆写)
- `AbstractBeanFactory.java:276-279` — **parent 是 AbstractBeanFactory → 直调 doGetBean**: 省 parent 侧 getBean 包装帧 — transformedBeanName/getObjectForBeanInstance/类型检查在 parent doGetBean 内照常执行 — 1层调用栈节省
- Spring Boot `SpringApplication.prepareContext()`: 设置 parent ApplicationContext + 启动 child → controller 在 child 中 → getBean("dataSource") → child 找不到 → parent → 获取共享 DataSource

关键设计: **两处微观优化: ApplicationContext 层(getInternalParentBeanFactory, L1517-1520, 跳过 getParent() 中间层) + BeanFactory 层(直调 doGetBean, L277, 省包装帧) → 各省1层调用栈。** 父子容器这类"高频调用"路径上的微观优化 — 因为大型 Spring Boot 应用的 Bean 依赖图可能经过 3+ 层父子容器。

数据流: SpringApplication.run()→createApplicationContext→context.setParent(environmentContext)→L539 this.parent = environmentContext→(GenericApplicationContext覆写)L167 beanFactory.setParentBeanFactory(getInternalParentBeanFactory())→L1518 environmentContext instanceof ConfigurableApplicationContext→true→cac.getBeanFactory()→返回environment的DefaultListableBeanFactory→beanFactory.parentBeanFactory = envBeanFactory → childContext.getBean("dataSource")→AbstractBeanFactory.getBean→doGetBean→L250 getSingleton→null(本地没)→L273 parentBeanFactory != null && !containsBeanDefinition→L276 parentBeanFactory instanceof AbstractBeanFactory→true→L277 abf.doGetBean("dataSource")→parent.getSingleton→找到DataSource→返回child→WebServer继续启动

→ spring-context 第五域完成。父子容器关系 + Bean 委托链 + 三线同步 + paths 优化 — 4个维度。引出 S2-6: BeanFactoryPostProcessor 全景 — S1-8 spring-beans 层中 config 在等待这个域。
