# S1-2 §2 getBean() 内部 — 从 BeanDefinition 到 Java 对象

> 依赖 §1 | 🔴 Deep | 2 KP | [模式: Template Method]

**读者处境**: `applicationContext.getBean("userService")` — 这行代码内部发生了什么？不是简单查 HashMap — `getBean()` 内部有三级缓存、循环依赖检测、parentFactory 回退、完整的生命周期回调。

### 1. getBean() 三层缓存 — singletonObjects/earlySingletonObjects/singletonFactories

场景: `A a = ctx.getBean(A.class)` — A 依赖 B — B 又依赖 A — 循环依赖。Spring 通过三级缓存解决。getBean 先查 singletonObjects(一级成品) — 命中→返回。未命中→检查是否在创建中→从 singletonFactories(三级工厂)调用 getObject()→存入 earlySingletonObjects(二级)→从二级返回提前暴露的引用。请求路径: `AbstractBeanFactory.getBean()`(L201 入口) → `doGetBean()` — 注意: 单参 `getBean(String)` 本身在 DefaultSingletonBeanRegistry.java:197 有共享实例快速路径 — 创建逻辑统一收敛到 doGetBean 模板。

源码路径:
- `AbstractBeanFactory.java:242` — **doGetBean(name, requiredType, args, typeCheckOnly)**: 模板方法入口，~100行
- `AbstractBeanFactory.java:246` — **transformedBeanName(name)**: 剥离 `&` 前缀(FactoryBean dereference) + alias 解析
- `DefaultSingletonBeanRegistry.java:85,94,88` — **singletonObjects(256)/earlySingletonObjects(16)/singletonFactories(16)** — 三级缓存
- `DefaultSingletonBeanRegistry.java:210` — **getSingleton(beanName, true)**: 三级查找 → singletonObjects→earlySingletonObjects→singletonFactories.getObject()
- `DefaultSingletonBeanRegistry.java:185` — **addSingletonFactory(beanName, singletonFactory)**: 放入三级缓存
- `DefaultSingletonBeanRegistry.java:160` — **addSingleton(beanName, singletonObject)**: 放入一级缓存，移除二/三级
- `AbstractBeanFactory.java:261` — **getObjectForBeanInstance**: 处理 FactoryBean — 检测 `&` 前缀决定返回 FactoryBean 自身或 getObject() 产物

关键设计: **Why 三级缓存而非二级？** 二级缓存存的是**已经确定的成品对象** — 若 A 提前放入二级, 则"是否代理 A"这个决策在放入那一刻就冻结了。但 AOP 需要**推迟决策**: A 先实例化 → B 需要 A 的引用 → 此刻才调用 `getEarlyBeanReference` → AOP 判断 A 是否需要代理 (AbstractAutoProxyCreator.getEarlyBeanReference:265-268 → wrapIfNecessary) → 需要则此时创建代理, B 拿到的是代理。三级缓存存 lambda (`singletonFactories`), 在暴露时刻 (L226 `singletonFactory.getObject()`) 才执行 getEarlyBeanReference — **代理决策从实例化推迟到首次暴露**。这是 Template Method: 三级缓存的查找骨架固定 (L210-231), 工厂行为由 getEarlyBeanReference 子类覆写。 [模式: Template Method — doGetBean 框架, 子步骤可覆写]

数据流: `getBean("a")`→`getSingleton("a")`→singletonObjects 无→`isSingletonCurrentlyInCreation("a")`=**false**(首次)→`beforeSingletonCreation("a")`标记正在创建→`singletonFactory = () -> createBean("a", mbd, args)`→`singletonObject = singletonFactory.getObject()`→`doCreateBean("a")`→`createBeanInstance("a")`(new A) →`addSingletonFactory("a", () -> getEarlyBeanReference("a", mbd, bean))`放入三级→`populateBean("a", mbd)`属性填充→发现 B →`getBean("b")`→`getSingleton("b")`→无→`createBean("b")`→`populateBean("b")`→B 依赖 A→`getBean("a")`→`getSingleton("a")`→singletonObjects 无→earlySingletonObjects 无→**singletonFactories 有**→`factory.getObject()`→`getEarlyBeanReference("a")`→earlyRef→放入 earlySingletonObjects→B 持有 A 引用→B 创建完成→`addSingleton("b", b)`→返回到 A→A 填充完成→`initializeBean("a")`→`addSingleton("a", a)`放入一级→`afterSingletonCreation("a")`→返回 A。

类型校验: `ctx.getBean(A.class)` 最后在 `adaptBeanInstance`(L409) 检查 `requiredType` — L411 `!requiredType.isInstance(bean)` 不匹配 → L415 抛 `BeanNotOfRequiredTypeException`。

### 2. doGetBean() 完整 8 步 + FactoryBean 处理

场景: `getBean("&userService")` — 如果 "userService" 是一个 FactoryBean(如 MyBatis Mapper)—`&` 前缀让 getBean 返回 FactoryBean 自身而非它创建的 Bean。

关键点: FactoryBean 的 `getObject()` 产物在 doGetBean 第 3 步 `getObjectForBeanInstance` 中生成 — 这是 BeanFactory 与 FactoryBean 的唯一耦合点。

源码路径:
- `AbstractBeanFactory.java:242-390` — **doGetBean()** 完整 8 步: 1.`transformedBeanName(name):246` 剥离 `&` 前缀和别名 2.`getSingleton(beanName):250` 查三级缓存 3.缓存命中→`getObjectForBeanInstance():261` 处理 FactoryBean 4.`isPrototypeCurrentlyInCreation():267` 检查循环 5.`getParentBeanFactory():272` 取父工厂 + `parentBeanFactory.getBean():276-288` 向上递归 6.`getMergedLocalBeanDefinition(beanName):302` 获取合并后的 RootBD 7.`mbd.getDependsOn():306` 检查 @DependsOn 8.按 scope 创建: singleton→`getSingleton()+createBean()` / prototype→`createBean()` / request→`Scope.get()`
- `AbstractBeanFactory.java:405` — **doGetBean 收尾**: `adaptBeanInstance(name, bean, requiredType)`(L409) 做 requiredType 类型校验 — 返回前统一出口

关键设计: **Why & 前缀 ≠ FactoryBean 的透明性？** Spring IoC 的核心契约是: `getBean("name")` 返回的必须是"可直接使用的 Bean"。如果 "userMapper" 是一个 FactoryBean(如 MyBatis MapperFactoryBean) — FactoryBean 自身是基础设施 — 用户要的是 `getObject()` 的产物(实际的 Mapper 接口代理)。所以 `getBean("userMapper")` 默认返回 Mapper 代理 — 透明。`&userMapper` 是**基础设施代码的 escape hatch** — 只有在需要 FactoryBean 自身时(如检查它是否是 FactoryBean/是否已初始化)才用 & 前缀。两者分离保证: 99% 的代码拿 Bean 不看 FactoryBean — 1% 的框架代码用 & 显式获取工厂。[模式: Proxy — FactoryBean 透明代理最终 Bean]

数据流: `getBean("userService")`→`transformedBeanName("userService")`("userService",无&)→`getSingleton("userService")`→null(首次)→scope=singleton→进入 singleton 创建→`beforeSingletonCreation("userService")`→`singletonFactory = () -> createBean("userService", mbd, args)`→`doCreateBean()`: 实例化(`createBeanInstance`/构造器注入)→`applyMergedBeanDefinitionPostProcessors`(处理 @Autowired/@Value)→`addSingletonFactory`(放入三级缓存)→`populateBean`(属性填充/依赖注入)→`initializeBean`(BeanPostProcessor前/afterPropertiesSet/initMethod/BeanPostProcessor后)→`addSingleton("userService", bean)`(放入一级/移除二三级)→返回。

→ 引出 S1-3 Bean 生命周期 — `doCreateBean()` 内部: 实例化→属性填充→初始化→销毁 — 完整 13 步声明周期 + BeanPostProcessor 干预点
