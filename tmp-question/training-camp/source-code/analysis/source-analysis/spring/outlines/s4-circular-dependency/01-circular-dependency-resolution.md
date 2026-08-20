# S1-4 循环依赖 — 三级缓存 + getEarlyBeanReference 的精确协作

> 依赖 S1-2 / S1-3 | 🔴 Deep | 3 KP | [模式: Object Pool + Interceptor]

**读者处境**: S1-2 getBean 里看到了三级缓存、S1-3 看到了 getEarlyBeanReference — 但循环依赖时两者怎么配合？为什么构造器注入的循环依赖 Spring 解决不了，setter 注入的却可以？

### 1. 为什么构造器注入的循环依赖无法解决？

场景: `@Autowired public A(B b)` 和 `@Autowired public B(A a)` — Spring 启动报错 `BeanCurrentlyInCreationException: Requested bean is currently in creation: Is there an unresolvable circular reference?`

源码路径:
- `DefaultSingletonBeanRegistry.java:85,94,88` — **三级缓存字段**: singletonObjects(L85)/earlySingletonObjects(L94)/singletonFactories(L88)
- `DefaultSingletonBeanRegistry.java:210` — **getSingleton(beanName, allowEarlyReference)**: L211 查 singletonObjects→L212 `isSingletonCurrentlyInCreation`→L213 查 earlySingletonObjects→L215 allowEarlyReference 判断(L216 是 singletonLock.tryLock — 非创建线程时直接返回 null)→L226 `singletonFactories.get()`→L230 `singletonFactories.remove()`→L231 移入 earlySingletonObjects

关键设计: **Why 构造器循环不可解？** `createBeanInstance("a")` 内部 `autowireConstructor()` 解析构造器参数 → 发现需要 B → `getBean("b")` → `createBeanInstance("b")` 解析构造器需要 A → `getBean("a")` → 此时 A 还在构造器执行中，**尚未执行到 addSingletonFactory**(L600 — 在 createBeanInstance 之后)。三级缓存中没有 A 的 factory — `getSingleton("a", true)` → 三缓存全 miss → 抛 `BeanCurrentlyInCreationException`。**addSingletonFactory 在 createBeanInstance 之后 — 构造器注入在 createBeanInstance 内部递归 — 来不及放入三级缓存。**

数据流: `getBean("a")`→`doCreateBean("a")`→`createBeanInstance("a")`→`autowireConstructor`→需要 B→`getBean("b")`→`doCreateBean("b")`→`createBeanInstance("b")`→需要 A→`getBean("a")`→`getSingleton("a", true)`→singletonObjects null→isSingletonCurrentlyInCreation("a")=true→earlySingletonObjects null→singletonFactories null(**A 的 addSingletonFactory 还没执行!** )→抛异常。**A 需要 B 在构造器完成前 — B 需要 A 也在构造器完成前 — 互相等待 — 永远不能绕过构造器。**

### 2. setter 注入 — 三级缓存的精确时序

场景: `@Autowired private B b` (在 A 中) 和 `@Autowired private A a` (在 B 中) — 启动正常。

源码路径:
- `AbstractAutowireCapableBeanFactory.java:560` — **doCreateBean()**: `createBeanInstance(可完成)→addSingletonFactory(L600)→populateBean(此处触发循环依赖)`
- `AbstractAutowireCapableBeanFactory.java:600` — **addSingletonFactory**: `singletonFactories.put(beanName, () -> getEarlyBeanReference(beanName, mbd, bean))` — **在 populateBean 之前**放入三级缓存
- `DefaultSingletonBeanRegistry.java:183` — addSingletonFactory: 方法声明在 L183，L185 执行 `singletonFactories.put`，L186 移除对应的 earlySingletonObjects(保证 factory 优先于已存半成品)
- `AbstractAutowireCapableBeanFactory.java:975` — **getEarlyBeanReference()**: 遍历 SmartInstantiationAwareBeanPostProcessor → `getEarlyBeanReference(bean, beanName)` → 返回原始 Bean 或early proxy

关键设计: **Why setter 可解？** A 的构造器不需要 B — `createBeanInstance("a")` 成功返回 → 此时 A 已实例化(无参构造) → `addSingletonFactory("a", () -> getEarlyBeanReference("a", ...))` 放入三级缓存 → **populateBean("a")** 发现需要 B → `getBean("b")` → B 的 createBeanInstance 成功(B 的构造器可能也不需要A) → B 的 addSingletonFactory 放入三级 → B 的 populateBean 发现需要 A → `getBean("a")` → **此时 A 的 factory 已在三级缓存中** → `getSingleton("a", true)` → singletonFactories.get("a") → getEarlyBeanReference → 返回 earlyA → B 拿到 A 引用 → B 完成 → `addSingleton("b", b)` → 返回 A → A 的 populateBean 拿到完整 B → A 完成 → `addSingleton("a", a)`。**addSingletonFactory 在 populateBean 之前 = 循环依赖的窗口。**

数据流: `getBean("a")`→`doCreateBean("a")`(1)createBeanInstance("a")→new A ✅ (2)applyMergedBeanDefinitionPostProcessors (3)`addSingletonFactory("a", () -> getEarlyBeanReference("a", ...))` ⬅️ 关键: A的factory放入三级 (4)`populateBean("a")`→需要B→`getBean("b")`→`doCreateBean("b")`(1)createBeanInstance("b")→new B ✅ (2)applyMergedBeanDefinitionPostProcessors (3)`addSingletonFactory("b")` (4)`populateBean("b")`→需要A→`getBean("a")`→`getSingleton("a",true)`→singletonObjects null→isSingletonCurrentlyInCreation("a")=true→singletonFactories.get("a")→`getEarlyBeanReference("a", mbd, bean)`→SmartInstantiationAwareBPP遍历→返回 earlyA→存入earlySingletonObjects→B拿到earlyA(引向同一个A对象)→B的populateBean完成→B的initializeBean→`addSingleton("b",b)`(B放入一级/移出二三级)→返回B→A的populateBean拿到B→A的属性填充完成→A的initializeBean→BPP After(@PostConstruct等)→AOP代理生成→`addSingleton("a", proxyA)`(最终A放入一级)→返回A。

### 3. getEarlyBeanReference 的作用 — 非代理 vs 需要代理

场景: A 需要 AOP 代理(@Transactional) — B 循环依赖 A。B 在 populateBean 时拿到的是什么？A 的原始对象还是 AOP 代理？答案在 getEarlyBeanReference。

源码路径: `AbstractAutoProxyCreator.java:265-268` — **getEarlyBeanReference()**: L267 将原始 bean 存入 `earlyBeanReferences` Map(L140: `ConcurrentHashMap(16)`)，L268 `return wrapIfNecessary(bean, beanName, cacheKey)` — 需要 AOP 时直接返回代理。之后的 `postProcessAfterInitialization`(L315-319) 判断 `earlyBeanReferences.remove(cacheKey) != bean`: remove 返回 **null**(未提前创建代理)→`wrapIfNecessary` 现在创建 / remove 返回 **==bean**(early proxy 已创建)→跳过、直接 return bean

关键设计: **Why early proxy + 最终代理可能不同？** getEarlyBeanReference 时 Bean 的属性还没完全填充 — 代理只能基于当前的"半成品" Bean 生成。initializeBean 的 AfterInitialization 会再次调用 `wrapIfNecessary` — 但 `AbstractAutoProxyCreator.postProcessAfterInitialization()`(L315) 先执行 `earlyBeanReferences.remove(cacheKey) != bean`(L318): **remove 返回 null(未提前创建代理)→ 条件成立 → 此时调用 `wrapIfNecessary` 创建代理**；**remove 返回 == bean(说明 getEarlyBeanReference 阶段已经创建了代理，Map 中存的就是这个原始 bean 引用)→ 条件不成立 → 直接返回当前 bean(已是代理，不需再次包装)**。early proxy 和最终代理是同一个代理对象 — 但代理内部持有的是原始 Bean 引用(非代理的)—当原始 Bean 的属性被 populateBean 填充时 — 代理自动看到更新后的值。这就是 Java 引用的威力。

数据流: `B 的 populateBean 需要 A`→`getSingleton("a", true)`→三级缓存命中→`singletonFactories.get("a")`→factory 内执行 `getEarlyBeanReference("a", mbd, bean)`(AbstractAutowireCapableBeanFactory.java:975)→mbd 非 synthetic 且容器有 InstantiationAwareBPP→遍历 smartInstantiationAware 缓存→`AbstractAutoProxyCreator.getEarlyBeanReference(bean, "a")`(AbstractAutoProxyCreator.java:265)→L267 `earlyBeanReferences.put(cacheKey, bean)` 记录原始引用→L268 `wrapIfNecessary(bean, "a", cacheKey)`→`getAdvicesAndAdvisorsForBean` 判断 A 需要 @Transactional 代理→`createProxy`→返回代理 A→存入 earlySingletonObjects→B 注入代理 A。之后 A 的 initializeBean→`postProcessAfterInitialization`(L315)→L318 `earlyBeanReferences.remove(cacheKey)` 返回==bean→条件不成立→直接返回代理 A→**不二次包装**。

→ 引出 S1-5 DI注入三机制 — 循环依赖解决了"引用问题"，但 @Autowired 怎么知道注入哪个 Bean？@Qualifier 怎么区分同类型多实例？@Resource 和 @Autowired 的区别在哪？
