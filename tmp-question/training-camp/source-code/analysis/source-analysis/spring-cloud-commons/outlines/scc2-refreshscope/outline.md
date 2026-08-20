# SCC-2 @RefreshScope 热刷新 — 配置变更的"推倒重来": 代理、双锁与双阶段刷新

> 前置: [[SCC-1-Bootstrap]] (配置拉取) + Spring AOP scoped proxy (阶段2) | 引出: [[SCC-8-RefreshEndpoint]] (刷新触发面) + [[SCC-9-配置加密]] (decrypt 重跑) | 对照: Spring Boot ContextRefreshedEvent + Apollo 自动刷新
> 🔴 A | 方案 A (全深度) | 闭环: q1(代理+双锁) q2(双阶段刷新) q3(新旧刷新器) q4(事件链)
> Pass 2 闭环: q1(LockedScopedProxyFactoryBean) q2(refresh/refreshAll) q3(ConfigData vs Legacy) q4(事件)

**读者处境**: 配置中心改了值, @RefreshScope Bean 怎么"自动"拿到新值? "销毁重建"是立即还是懒? 并发请求正在用旧 Bean, 刷新时会不会崩? @RefreshScope 和普通 scope (singleton/prototype) 的实现差异在哪?

### 1. 代理面 — LockedScopedProxyFactoryBean 的"方法级读锁"

场景: @RefreshScope Bean 为什么每次方法调用都过代理?
源码路径:
- GenericScope (502 行, scope/GenericScope.java:73): `extends SimpleThreadScope implements Scope, DisposableBean` — 刷新作用域核心
- **LockedScopedProxyFactoryBean** (L433-502): `extends ScopedProxyFactoryBean implements MethodInterceptor` — **代理 + 拦截器双角色**
- setBeanFactory (L447-452): getObject() 后 `advised.addAdvice(0, this)` — **把自己作为第一个 advice 注入**
- invoke (L460-490): ① equals/toString/hashCode/getTargetObject 直通 (L462-463) ② **getObject() 取代理** (L466, ScopedProxyFactoryBean 产物) ③ 无锁兜底 new ReentrantReadWriteLock (L468-471) ④ **readLock.lock()** (L474) ⑤ **getTargetSource().getTarget() 拿目标 Bean + 反射调用** (L480-481, 代理只做锁管理, 方法执行在目标上) ⑥ **catch UndeclaredThrowableException 抛原始异常** (L487, gh-349 修复) ⑦ finally unlock
- 无锁兜底: `new ReentrantReadWriteLock()` 防 NPE (L468-471)
关键设计 (q1): **"代理 + 读锁"是刷新安全的根本** — 每次方法调用拿读锁, destroy 拿写锁 → 刷新时读锁阻塞 (等旧 Bean 用完), 刷新后新 Bean 懒创建; gh-349 历史: 代理调目标抛 UndeclaredThrowableException 需还原原始异常。 [模式: 代理拦截 + 读写锁]

### 2. 双锁语义 — cache/locks 的读快写慢

场景: 为什么用 ReadWriteLock 而不是 synchronized 全部?
源码路径:
- **locks = ConcurrentMap\<String, ReadWriteLock\>** (L88); cache 是 ScopeCache (默认 StandardScopeCache)
- **get** (L173-182): `cache.put(name, new wrapper)` — **StandardScopeCache.put 是 putIfAbsent** (StandardScopeCache.java:20-25: 已有返回旧值不覆盖) → wrapper 稳定; + `locks.putIfAbsent(name, ReentrantReadWriteLock)` (L176) → value.getBean() — **get 本身不用 ReadWriteLock** (那是代理层 invoke 的锁); 错误累积 errors (L179-181)
- **BeanLifecycleWrapper.getBean** (L369-374): `synchronized(name)` **双检锁** — bean==null 才 objectFactory.getObject() (懒创建)
- **destroy()** (L127-142): `cache.clear()` → 逐个 wrapper `writeLock.lock()` → wrapper.destroy() → errors 收集+wrapIfNecessary (L141)
- destroy(String) 单 bean (L155-169): cache.remove + writeLock
关键设计 (q1): **双检 + 读写锁 = 并发读不阻塞, 刷新写全清** — getBean 双检保证懒创建线程安全; destroy 写锁保证"旧 Bean 正在用"时刷新等待; errors 累积后一次性 wrap 抛出。 [模式: 双检锁 + 读写锁]

### 3. RefreshScope — refresh/refreshAll 与事件发布

场景: 刷单个 Bean 和刷全部有什么区别?
源码路径:
- RefreshScope (scope/refresh/RefreshScope.java:69): `extends GenericScope` + **@ManagedResource (JMX 面)** (L68) + @ManagedOperation 两个 (L148/164)
- **refresh(Class)** (L140-147): 按类型找 bean 名 (含祖先上下文) → 委托 refresh(String) — 类型级入口
- **refresh(String)** (L150-164): ScopedProxyUtils.isScopedTarget 检查 → getTargetBeanName 转换 → super.destroy(name) → **RefreshScopeRefreshedEvent(name) 发布**
- **refreshAll()** (L166-171, @ManagedOperation): super.destroy() → **RefreshScopeRefreshedEvent() 全量发布**
- 事件面: RefreshScopeRefreshedEvent (L30-40) — 监听者通知
- 装配: RefreshAutoConfiguration (autoconfigure/RefreshAutoConfiguration.java:69-70): @ConditionalOnClass(RefreshScope) + REFRESH_SCOPE_ENABLED matchIfMissing=true
关键设计 (q2): **销毁是"立即清缓存 + 懒重建"** — destroy 只清缓存不重建; 下次方法调用 (代理读锁内) 才 getBean 重建; refresh(String) 单 bean 走 scoped target 名转换 (ScopedProxyUtils.getTargetBeanName — Boot 依赖类)。 [模式: 失效+懒重建]

### 4. ContextRefresher — 双阶段刷新: 环境变更 + Scope 清空

场景: /actuator/refresh 触发后, 到底做了什么?
源码路径:
- **ContextRefresher.refresh()** (refresh/ContextRefresher.java:92-96): **synchronized** + `refreshEnvironment()` + `scope.refreshAll()` — 双阶段
- **refreshEnvironment()** (L98-106): ① before = extract(环境) ② updateEnvironment() (抽象) ③ changes(before, after) 键集合 ④ **EnvironmentChangeEvent 发布** (L103-105) — 事件消费方: **ConfigurationPropertiesRebinder 重绑定 @ConfigurationProperties Bean** (context/properties/ConfigurationPropertiesRebinder.java, 与 RefreshScope 重建是互补机制: 属性类走重绑定, 业务 Bean 走重建)
- copyEnvironment (L110-119): **只复制默认源 + profiles** — DEFAULT_PROPERTY_SOURCES = **[commandLineArgs, defaultProperties]** (L54-57, 注释 "cli args 必须第一" 顺序敏感) + additionalPropertySourcesToRetain 可扩展 (L65/115-117); 防 merge 冲突 ("Don't use ConfigurableEnvironment.merge()")
- 继承者: ConfigDataContextRefresher (新) / LegacyContextRefresher (@Deprecated 旧)
关键设计 (q2): **"先环境后 Scope"的顺序是契约** — 先让 Environment 拿到新配置, 再清 Scope 缓存; EnvironmentChangeEvent 让监听者 (如 @ConfigurationProperties 重绑定) 先行动, RefreshScopeRefreshedEvent 后行动; copyEnvironment 防 merge 污染。 [模式: 双阶段模板]

### 5. 新旧刷新器 — ConfigDataContextRefresher vs LegacyContextRefresher

场景: 新项目和老项目的刷新机制差在哪?
源码路径:
- **ConfigDataContextRefresher** (refresh/ConfigDataContextRefresher.java:47): `extends ContextRefresher` — 新式
- **updateEnvironment** (L68-93): **重跑全部 EnvironmentPostProcessor** (SpringFactoriesLoader.loadFactoryNames L80 + Instantiator L83-88) — vcap/decrypt/ConfigData 全重放 (注释 L75-76)
- **LegacyContextRefresher** (L42, @Deprecated): 旧式 — gh-678 修复注释 (L69, web-application-type 属性)
- **⚠ 装配选择 (跨域关键)**: **@ConditionalOnBootstrapEnabled → LegacyContextRefresher** (RefreshAutoConfiguration.java:104-106) / **@ConditionalOnBootstrapDisabled → ConfigDataContextRefresher** (L112-114) — **新旧刷新器由 bootstrap 开关决定** (Legacy 依赖 bootstrap 上下文环境, ConfigData 走新 config 机制; 与 SCC-1 双轨制同源)
- ConfigDataContextRefresher.updateEnvironment 方法本身 @Deprecated (L52) — 新式也在演化
关键设计 (q3): **新式刷新 = 完整重放环境后处理链** — 不是"增量更新"而是"整个 Environment 重跑后处理"; 这保证 config.import (Boot 2.4+ 新机制) 在刷新时也生效; Legacy 只处理旧属性源; **bootstrap 启用的应用自动用 Legacy** (两机制配套: bootstrap 上下文 ↔ Legacy 刷新)。 [模式: 重放 vs 增量 + 装配联动]

### 6. 生命周期 — RefreshScopeLifecycle 与重启刷新

场景: 应用重启时 @RefreshScope Bean 怎么处理?
源码路径:
- RefreshScopeLifecycle (refresh/RefreshScopeLifecycle.java:33, @since 4.1.0): `implements Lifecycle` — 生命周期钩子
- start() (L48-52): synchronized(lifecycleMonitor) → "Refreshing context on restart" (L52) — 重启时刷新
- ContextRefresher 字段注入 (L19-20) — 复用刷新器
关键设计 (q4): **重启复用刷新逻辑** — Lifecycle.start 里触发 ContextRefresher; 4.1.0 新增 (对比 SCC-1 的 4.1 时代演进); 双锁/代理机制在重启路径同样生效。 [模式: 生命周期钩子]

## 代码类型
Architecture (作用域核心) + Concurrency (读写锁)

## 负面空间 — @RefreshScope 刻意不做的事

- **不自动感知配置变更**: 需要外部触发 (RefreshEndpoint/ContextRefresher.refresh 手动或配置中心回调) — 对比 Apollo 推送式
- **不做运行时字段级更新**: 销毁重建整个 Bean, 不保留旧实例字段 (对比 Spring Boot 的 @ConfigurationProperties 重绑定是另一机制)
- **不做跨上下文刷新**: 只刷当前上下文, 多上下文各自 refreshAll
- **不保证刷新原子性跨 Bean**: 逐个 wrapper 写锁, 无全局事务
- **不刷 singleton/prototype**: 只刷 refresh scope 的 Bean (作用域隔离)
- **不处理销毁回调异常恢复**: errors 累积抛首个, 无重试

→ 引出: /actuator/refresh 端点怎么触发这套机制? → SCC-8 RefreshEndpoint + 事件
