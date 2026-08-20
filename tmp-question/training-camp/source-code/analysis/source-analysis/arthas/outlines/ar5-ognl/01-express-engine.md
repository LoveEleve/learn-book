# 01. 一个表达式引擎,为什么藏着 Arthas 最深的防泄漏设计 — 表达式引擎

> 🔴 Deep | 10 KP 中的 4 个(弱引用池/OgnlExpress/私有访问/类解析)
> 读者处境: `watch -e 'params[0] > 100'` 里的表达式由谁执行?执行它的对象由谁创建?为什么创建方式藏着 arthas 最经典的防泄漏设计?

### 1. "表达式对象不能直接存 ThreadLocal" — 弱引用池

场景: 每次方法调用都要判一次条件(高频!),表达式对象不能每次 new——复用又怕泄漏。怎么破?

- `ExpressFactory`(core/command/express/ExpressFactory.java:19-39): `ThreadLocal<WeakReference<Express>>`(:19)
- 源码注释原文(:11-17): **"这里不能直接在 ThreadLocalMap 里强引用 Express(它由 ArthasClassLoader 加载),否则 stop/detach 后会被业务线程持有,导致 ArthasClassLoader 无法被 GC 回收。用 WeakReference 打断强引用链: Thread → ThreadLocalMap → value(WeakReference) -X-> Express"**
- `threadLocalExpress(object)`(:23-31): 取 WeakReference → get 到 null(被 GC)则新建 → `reset().bind(object)` 返回——**复用 + 自动重建**
- 调用场景: `AdviceListenerAdapter.isConditionMet`(AR-2 篇 3 的条件过滤)——**每次回调都走这个池**
- [Java: ThreadLocalMap 的 key 是弱引用但 value 是强引用——如果 value 持有 ArthasClassLoader 加载的对象,类加载器永远不会被回收(经典的 ThreadLocal 内存泄漏模式)。用 WeakReference 包一层 value 打断链条]

关键设计: [模式: 对象池+弱引用(ThreadLocal<WeakReference>)+ 适配器(OgnlExpress 封装 OGNL)+ 缓存(ClassResolver 类名缓存)] **泄漏防御的层次**: 业务线程(长期存活)的 ThreadLocal 是"毒源",而表达式对象属于 ArthasClassLoader(可卸载)——两者一结合就是泄漏。WeakReference 让"线程还活着但 arthas 已卸载"时,表达式对象自然被回收,下次调用自动重建。**这是生产级工具的自我保护**: 诊断工具自身不能成为泄漏源。

### 2. "OGNL 的薄封装" — OgnlExpress

场景: OGNL 库直接用会怎样?Arthas 包了一层什么?

- `OgnlExpress`(OgnlExpress.java:18-47):
  - 静态初始化 `MEMBER_ACCESS = new DefaultMemberAccess(true)`(:17)+ `OBJECT_PROPERTY_ACCESSOR`(:19)
  - 构造: `OgnlRuntime.setPropertyAccessor(Object.class, OBJECT_PROPERTY_ACCESSOR)`(:26)+ `new OgnlContext(MEMBER_ACCESS, classResolver, null, null)`(:28)——**把"读属性/私有访问/类解析"三项策略一次性钉进 OGNL 上下文**
  - `get(express)` = `Ognl.getValue(express, context, bindObject)`(:34-38)——`bindObject` 就是 Advice(AR-2 篇 3 的调用现场)
  - `is(express)` = get 结果 `instanceof Boolean && (Boolean)ret`(:41-44)——**只认 Boolean true**
  - `bind(Object)`(:46-49)/`bind(name, value)`(:51-54)/`reset()`(:56-58)
- [OGNL: 上下文(OgnlContext)= 成员访问策略 + 类解析器 + 根对象。表达式 `params[0].name` 在根对象上取值——params 是 Advice 暴露的字段(AR-0 篇 6 的表达式环境)]

关键设计: **`is()` 的严格语义**: 条件表达式 `params[0] > 100` 必须返回 Boolean true 才放行——字符串 "false"/数字 0 都不过。这保证条件判断的确定性(watch 不会因为 "0" 或 null 误判)。

### 3. "私有成员随便读" — DefaultMemberAccess 与安全边界

场景: `ognl '@com.example.Config@INSTANCE.privateField'`——为什么能读 private?

- `DefaultMemberAccess(boolean allowAllAccess)`(DefaultMemberAccess.java:26-31): `allowPrivateAccess/allowProtectedAccess/allowPackageProtectedAccess` 三开关;`new DefaultMemberAccess(true)` 全开
- `canAccess` 恒返回 true——**不需要反射 setAccessible**,OGNL 直接放行
- `ArthasObjectPropertyAccessor`(ArthasObjectPropertyAccessor.java:13): 继承 OGNL ObjectPropertyAccessor——属性读取的扩展(容错 getter)

关键设计: **能力与边界的平衡**: 全放开是诊断的必需(线上对象私有状态往往才是问题根源);但"能读"不等于"随便用"——表达式里的**方法调用会真实执行**(AR-0 篇 6 强调的纪律)。安全边界分两层: ① 用户纪律层(只读不写);② **代码闸门**: `ArthasObjectPropertyAccessor.setPossibleProperty`(ArthasObjectPropertyAccessor.java:13-17)在 `GlobalOptions.strict` 模式下抛 `IllegalAccessError`——**写属性被源码级禁止**,这是"只读"的硬保证(设置 strict 选项即开启)。

### 4. "按 ClassLoader 解析类" — ClassLoaderClassResolver

场景: 同名类多版本共存,ognl 表达式里写 `@com.example.Service@x` 用的是哪个?

- `OgnlExpress(classResolver)` 构造 + `ClassLoaderClassResolver`(ClassLoaderClassResolver.java:12-28): `classLoader.loadClass(className)`(:28)
- 默认: `CustomClassResolver.customClassResolver`(OgnlExpress.java:24)——单例+`ConcurrentHashMap` 缓存(CustomClassResolver.java:15-17);`classForName`(:23-44)解析链: **TCCL 加载 → 失败 Class.forName → 类名无点号时试 `java.lang.` 前缀**(:37-39,如 `String`→`java.lang.String`)
- 指定: `unpooledExpress(classloader)`(ExpressFactory.java:33-39)——tt -w/ognl -c 场景,用目标 CL 解析类
- [OGNL: ClassResolver 决定 `@类名@` 语法中"类名"的解析——默认用当前线程 TCCL,arthas 改成显式 CL,多版本场景才能指哪打哪]

关键设计: **表达式与类加载器绑定**: watch 条件里的 `clazz`/方法上下文来自 Advice(运行时真实对象),不需要解析;但 `@类@静态成员` 语法需要**从字符串类名找到 Class 对象**——此时类加载器选择决定命中哪个版本(AR-0 篇 6 的 `-c <hash>` 在此落地)。

---

跨域桥: isConditionMet 的调用点 = AR-2 篇 3(分发链);表达式环境 = AR-2 篇 3 的 Advice 模型;`-c` 使用 = AR-0 篇 6;tt -s/-w = AR-2 篇 4。

---

**OpenJDK 关联**: 表达式引擎无直接对应的 JDK 域——可对照 [OpenJDK 域 44 Class Verification — outlines/44-class-verification/]: 理解 JVM 的访问控制(protected/private 语义)有助于理解 DefaultMemberAccess 放开了什么。

### 核心悬念

**"引擎就绪——它到底在哪一行代码被真正调用?"** — 不在命令里,在 AdviceListenerAdapter 的两个方法: isConditionMet(判)和 getExpressionResult(取)。而它们调用时,根对象是每次回调新建的 Advice。

> → [02-express-usage.md](02-express-usage.md)
