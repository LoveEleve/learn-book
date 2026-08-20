# A-1 代理机制 — JDK 动态代理 vs CGLIB 双实现

> 项目: Spring Framework 6.x | 🔴 Deep / 1 篇 | 5文件/~1590行
> 基线: S2-10 @Async / S2-12 @Cacheable — 两者都用 AOP 代理 — 代理怎么创建的？JDK还是CGLIB？

---

## §0.8

- 🟡 Working，1篇 — AopProxy双实现 + DefaultAopProxyFactory决策
- 设计模式: [模式: 代理模式]—JDK Proxy(接口代理)/CGLIB(子类代理); [模式: 工厂模式]—DefaultAopProxyFactory根据AdvisedSupport配置创建对应AopProxy

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AopProxy.java:64行 | getProxy() | AopProxy接口—两个方法: getProxy()返回代理对象 / getProxy(classLoader)指定ClassLoader | High |
| DefaultAopProxyFactory.java:60 | createAopProxy() | **决策**: ①config.isProxyTargetClass()=true→CGLIB ②config.isOptimize()=true→CGLIB ③target有接口→JDK Proxy ④无接口→CGLIB — 四条件决策 | High |
| JdkDynamicAopProxy.java:115-124 | getProxy() | **JDK实现**: Proxy.newProxyInstance(classLoader, interfaces, this(InvocationHandler))—invoke(L167)→getInterceptorsAndDynamicInterceptionAdvice→ReflectiveMethodInvocation.proceed→advice chain | High |
| CglibAopProxy.java:87-162 | getProxy() | **CGLIB实现**: Enhancer.create(superClass, interfaces, Callback)—DynamicAdvisedInterceptor(Callback)—intercept→ReflectiveMethodInvocation.proceed→advice chain | High |
| ProxyFactory.java:169行 | getProxy() | **统一入口**: extends AdvisedSupport→getProxy(DefaultAopProxyFactory.createAopProxy(this))→AopProxy.getProxy | High |

---

## 02-04 聚合+分类+聚类

### 聚合 — P1 核心 (2) + P2 支持 (1)

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | DefaultAopProxyFactory.createAopProxy 四条件决策 — proxyTargetClass/optimize/hasInterface/noInterface | 🔴 | **为什么🔴**: JDK vs CGLIB 的决策点—proxyTargetClass=true强制CGLIB(如@EnableAsync)—hasInterface且无强制→JDK—理解每个AOP注解(@Transactional/@Async/@Cacheable)用哪种代理都从这开始 |
| P1-2 | JdkDynamicAopProxy.invoke + CglibAopProxy.DynamicAdvisedInterceptor 双链调用 | 🔴 | **为什么🔴**: 代理的核心—请求到代理→invoke/intercept→获取advice chain→ReflectiveMethodInvocation.proceed→逐个调用advice→最终method.invoke(target) |

| P2-1 | ProxyFactory/AdvisedSupport 配置链 — advisor/advice/target/interfaces/proxyTargetClass | 🟡 | **为什么🟡**: 代理的配置层—ProxyFactory是AOP代理的统一入口—所有BPP(Async/Cache/Transactional)都通过它创建代理—但决策逻辑在DefaultAopProxyFactory |

### 聚类 (1篇)

**1篇理由**: ~1590行/5文件 — 决策工厂(78行)+JDK实现(360行)+CGLIB实现(919行)+ProxyFactory(169行)—核心是createAopProxy四条件+双getProxy。1篇(~45行)覆盖决策→双实现→调用链。

**单篇结构**: §1 DefaultAopProxyFactory四条件决策(JDK vs CGLIB) → §2 JdkDynamicAopProxy.getProxy+invoke + CglibAopProxy.getProxy+DynamicAdvisedInterceptor → §3 ProxyFactory统一入口
