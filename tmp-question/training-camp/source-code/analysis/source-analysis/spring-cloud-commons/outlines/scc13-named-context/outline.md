# SCC-13 NamedContextFactory — 每个客户端一个"小宇宙": 子上下文隔离的工厂

> 前置: [[SCC-1-Bootstrap]] (父子上下文对照) + Spring 上下文 (阶段2) | 引出: [[SCC-3-服务发现]] (DiscoveryClient 消费面) + [[SCC-6-LoadBalancer]] (LoadBalancerClientFactory 消费) + 阶段 5.6 OpenFeign (FeignClientFactory) | 对照: Spring 父子容器 + Feign 每客户端隔离
> 🔴 A | 方案 A (全深度) | 闭环: q1(双 Map 懒创建) q2(registerBeans 三段) q3(父环境复用) q4(多形态 getInstance)
> Pass 2 闭环: q1(getContext 双检) q2(default. 前缀) q3(buildContext propertySource) q4(ClientFactoryObjectProvider)

**读者处境**: Feign 怎么为每个 @FeignClient 隔离配置 (超时/编解码各不同)? LoadBalancer 怎么为每个服务配不同策略? "子上下文"和"父子容器"有什么区别? 为什么线程类加载器坏了也不崩?

### 1. 双 Map 结构 — contexts/configurations 的懒创建与双检锁

场景: 子上下文什么时候创建? 怎么保证并发安全?
源码路径:
- NamedContextFactory\<C extends Specification\> (named/NamedContextFactory.java:60-61): implements DisposableBean, ApplicationContextAware — 抽象工厂
- **contexts = ConcurrentHashMap\<String, GenericApplicationContext\>** (L69) + **configurations = ConcurrentHashMap\<String, C\>** (L71) — 双 Map: 已建上下文 + 已配配置
- **getContext 双检锁** (L119-126): `synchronized(this.contexts)` 内二次 containsKey 检查 — 并发下只建一次
- setConfigurations (L98-102): 遍历配置 put 到 configurations — 配置先于上下文注册
- destroy (L109-116): 遍历 contexts **逐个 close** (注释 "This can fail, but it never throws" — 关闭失败只 WARN)
关键设计 (q1): **"配置 Map + 上下文 Map"分离** — configurations 是"声明" (setConfigurations 注入), contexts 是"实例" (getContext 懒建); 双检锁保证每名字只一个子上下文; destroy 关闭全部 (DisposableBean 生命周期)。 [模式: 声明/实例分离 + 双检]

### 2. createContext — 三阶段构建与 AOT 分支

场景: 子上下文怎么从配置变成可用上下文?
源码路径:
- createContext (L130-140): ① buildContext(name) ② **AOT 分支**: applicationContextInitializers.get(name) 非空 → initialize + refresh 直接返回 (L133-137) ③ 否则 registerBeans + refresh (L139-140)
- buildContext (L161-192): ① **github issue 锚** (L162-163, netflix#3101/openfeign#475 — 类加载器问题) ② DefaultListableBeanFactory + **BeanFactory 的 BeanClassLoader 用 parent 的** (L169-173, 不依赖线程上下文 CL!) + **context 自身 setClassLoader 用工厂类的** (L183) ③ **AotDetector.useGeneratedArtifacts 分支** (L175-181: AOT → GenericApplicationContext / 否则 AnnotationConfigApplicationContext) ④ **propertySourceName 注入** (L185-187: `{propertyName: name}` MapPropertySource) ⑤ **setParent(parent)** (L189-191, 注释 "Uses Environment from parent as well as beans")
关键设计 (q2): **buildContext 是"父环境 + 子 Bean"的组装** — 属性源注入子标识 (propertyName=name 让子上下文知道自己是谁), setParent 复用父 Environment+Bean; **AOT 分支** 让 GraalVM 场景可用 GenericApplicationContext; 类加载器用父的 (issue 修复)。 [模式: 组装工厂 + AOT 分支]

### 3. registerBeans — "精确配置 + default 前缀 + 基础设施"三段注册

场景: 子上下文的 Bean 配置从哪来?
源码路径:
- registerBeans (L143-159): ① **name 精确匹配** (L147-150: configurations.get(name).getConfiguration() 逐个 register) ② **"default." 前缀配置** (L152-154: entry.getKey().startsWith("default.") — **默认配置注入所有子上下文**!) ③ **PropertyPlaceholderAutoConfiguration + defaultConfigType** (L158)
- **⚠ default. 前缀的生产者**: LoadBalancerClientConfigurationRegistrar 把 @LoadBalancerClients 默认配置注册为 `"default." + 类名` (loadbalancer/annotation/LoadBalancerClientConfigurationRegistrar.java:68/71); **AOT 初始化器排除 default. 配置** (LoadBalancerChildContextInitializer.java:84, `!startsWith("default.")` — 因 default 已注入每个子上下文)
- register 走 AnnotationConfigRegistry (L144-146, Assert 校验)
关键设计 (q3): **"default." 前缀是"全局默认"的约定** — 名字以 default. 开头的配置自动注入每个子上下文 (如 default. 的默认 LoadBalancer 策略); 精确匹配优先, default 兜底, 基础设施 (PropertyPlaceholder) 恒注册 — 三段式保证"每个子上下文都有完整配置"。 [模式: 前缀约定 + 三段注册]

### 4. getInstance 家族 — 多形态查找含祖先

场景: 从子上下文拿 Bean 有哪几种方式?
源码路径:
- getInstance(name, Class) (L199-208): getContext → **context.getBean(type) 不含祖先** (L203), NoSuchBeanDefinitionException → null — 只查子上下文快速失败
- **getInstance(name, ResolvableType)** (L225-234): **beanNamesForTypeIncludingAncestors** (L227 — **含祖先上下文查找**!) + isTypeMatch 过滤 → 单返回 — **含祖先的是 ResolvableType 变体, 非 Class 变体**
- getAnnotatedInstance (L236-249): beanNamesForAnnotationIncludingAncestors (L240) + **多 bean 抛 IllegalStateException** (L248 "Only one annotated bean")
- getInstances (L253-256): beansOfTypeIncludingAncestors 全量
- getLazyProvider (L210-211): **ClientFactoryObjectProvider 延迟包装** (ClientFactoryObjectProvider.java:35-43 — "resolve later" 占位)
关键设计 (q4): **IncludingAncestors 是父子可见性的实现** — 子上下文找不到时回父找 (Feign 的共享 Bean 经此可见); getAnnotatedInstance 单实例约束; lazy provider 把"解析"推迟到调用时 (解决循环依赖)。 [模式: 多形态查找 + 祖先回退]

### 5. Specification — 名字 + 配置类的声明契约

场景: 调用方怎么声明一个"客户端配置"?
源码路径:
- **Specification 接口** (L266-270): `getName()` (L268) + `getConfiguration()` (L270) — 声明契约
- 消费方: LoadBalancerClientSpecification (loadbalancer/annotation/ — SCC-6 面) / FeignClientSpecification (阶段 5.6 OpenFeign)
- setConfigurations 接收 List\<C\> (L98) — 批量注入
关键设计 (q1): **Specification 是"配置声明"的最小契约** — 名字 (标识) + 配置类 (Bean 来源); 各客户端框架实现自己的 Specification (LoadBalancer/Feign); 与 configurations Map 一一对应。 [模式: 声明接口]

### 6. 测试实证 — 子上下文隔离 + 类加载器健壮性

场景: 怎么证明"隔离"和"健壮"?
源码路径:
- testChildContexts (NamedContextFactoryTests.java:55-61): foo/bar 两上下文各自 getInstance — **互不污染** (L125-128) + getContextNames 断言 (L131)
- **testBadThreadContextClassLoader** (L63-77): ThrowingClassLoader 线程下创建子上下文 **不崩** — 验证 buildContext 用 parent CL (L170-173) 而非线程上下文 CL
- getAnnotatedInstance 三态: 单注解 (L79-92) / 无 (L94-105) / 多个抛 IllegalStateException (L108-117)
关键设计 (q2): **测试覆盖"坏 CL"场景** — 这是 github issue (netflix#3101/openfeign#475) 的回归测试; 子上下文隔离 + 祖先可见双语义都测到。 [模式: 回归测试]

## 代码类型
Architecture (上下文工厂) + Metaprogramming (子上下文组装)

## 负面空间 — NamedContextFactory 刻意不做的事

- **不做上下文缓存淘汰**: contexts 只增不删 (除 destroy 全清), 无 LRU
- **不做配置热更新**: setConfigurations 后不可改, 子上下文配置构建期固定 (对比 SCC-2 RefreshScope)
- **不做跨上下文共享状态**: 各子上下文独立, 共享靠 parent 或 default. 前缀
- **不做子上下文间通信**: 无事件桥接 (对比 Spring 父子容器事件向上传播)
- **不做懒配置校验**: Specification 配置类非法到 refresh 时才暴露
- **不做动态增删客户端**: 配置数量构建期固定

→ 引出: 子上下文隔离怎么被服务发现消费? → SCC-3 服务发现抽象
