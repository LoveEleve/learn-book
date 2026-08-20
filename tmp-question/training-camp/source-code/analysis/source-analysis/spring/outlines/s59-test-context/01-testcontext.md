# C-17 TestContext — 测试上下文框架 (门面 → 缓存 → 监听器)

> 依赖 C-16 MockMvc | 🟡 Working | 6 KP | [模式: 门面 + 观察者 + 缓存]

**读者处境**: @SpringBootTest 测试类能直接 @Autowired — 容器是谁加载的？为什么多个测试类共享同一个容器(启动一次)？@Transactional 测试怎么自动回滚？答案在 TestContext 框架。

### 1. TestContextManager — 测试生命周期门面

场景: 每个测试类/方法执行前后, 框架要统一做: 加载容器、注入依赖、开事务、清理 — 这些由 TestContextManager 按固定时序驱动, 交给各 TestExecutionListener 执行。

源码路径:
- `TestContextManager.java:91,251` — **门面**: prepareTestInstance L251: getTestContext().updateState(testInstance) → 遍历 getTestExecutionListeners().prepareTestInstance(getTestContext()) — 测试实例准备入口
- `TestContextManager.java:220` — **beforeTestClass**: 遍历 listeners.beforeTestClass — 测试类级前置(如注册日志/准备夹具)
- 全生命周期: beforeTestClass → **prepareTestInstance**(实例创建后) → beforeTestMethod(@Before 前) → beforeTestExecution(@Before 后) → 测试方法 → afterTestMethod → afterTestClass

关键设计: **Why 门面 + 监听器分发？** 生命周期固定(门面), 具体行为可插拔(监听器) — 新增能力(如并行、日志)只需加一个 TestExecutionListener, 不改框架; 这与 Spring 处处"模板+扩展"一致。[模式: 门面 + 观察者]

数据流: JUnit 执行测试 → 调 TestContextManager.beforeTestClass(220) → 每测试实例 prepareTestInstance(251): 遍历[DependencyInjectionTestExecutionListener(注入 @Autowired), TransactionalTestExecutionListener(开事务)...] → beforeTestMethod → 执行测试方法 → afterTestMethod(回滚事务) → afterTestClass。

### 2. ContextCache — 容器复用缓存

场景: 100 个测试类都用 @SpringBootTest(相同配置) — 若每个类都启动一次容器, 启动 100 次慢死。ContextCache 按配置键缓存容器, 相同配置共用。

源码路径:
- `ContextCache.java:99,107` — **缓存**: `get(MergedContextConfiguration key)`(L99, 命中返回) / `put(key, context)`(L107, 放入) — key 是合并配置
- `MergedContextConfiguration.java` — **缓存键**: 合并的配置(locations/classes/activeProfiles/contextLoader/父上下文) — **任一不同→不同键→新容器**
- `DefaultTestContext.java:47,129` — **加载链**: getApplicationContext L129 → cacheAwareContextLoaderDelegate.loadContext(mergedConfig) → 缓存优先, miss 才由 ContextLoader 建容器

关键设计: **Why 用"合并配置"作缓存键？** 容器由配置决定 — 相同(配置类+profiles+加载器)必然产生等价容器, 可安全复用; 配置一变(如加 @ActiveProfiles)键变, 自动建新容器 — 缓存正确性由键的完整性保证。[模式: 缓存 — 键驱动]

数据流: 测试 A getApplicationContext → loadContext(configA) → cache.get(configA) miss → ContextLoader 加载容器 → cache.put(configA, ctx) → 测试 B(相同 config) → cache.get 命中 → 直接复用, 不重启。测试 C(不同 profile)→键不同→新容器。

### 3. TestExecutionListener 常用实现 + 与 MockMvc 衔接

场景: @Autowired 注入、@Transactional 回滚、@DirtiesContext 销毁 — 都是内置监听器干的。

源码路径:
- `TestExecutionListener.java:126,173,198` — **接口**: prepareTestInstance(L173)/beforeTestMethod(L198)/beforeTestExecution/afterTestMethod(L267) — 各阶段回调
- 常用实现(spring-test/context/support 或 boot): **DependencyInjectionTestExecutionListener**(prepareTestInstance 阶段用 TestContext 的 beanFactory 注入 @Autowired)/ **TransactionalTestExecutionListener**(beforeTestMethod 开事务, afterTestMethod 回滚)/ **DirtiesContextTestExecutionListener**(@DirtiesContext 清理缓存)
- 与 MockMvc 衔接: C-16 的 webAppContextSetup(ctx) 中的 ctx 就是 TestContext 加载缓存的容器 — 测试类 @Autowired MockMvc 也是依赖注入监听器注入

关键设计: **Why 注入/事务都做成监听器？** 都是"测试生命周期侧行为", 解耦于业务 — 不想用事务回滚可禁用该监听器; 组合可配(用 @TestExecutionListeners 指定)。[模式: 策略组合]

数据流: @SpringBootTest 测试 → prepareTestInstance → DependencyInjectionTestExecutionListener: 遍历 @Autowired 字段→从缓存容器 getBean 注入 → beforeTestMethod → TransactionalTestExecutionListener: 若 @Transactional→开事务 → 方法内 DB 操作 → afterTestMethod→回滚(测试数据不落库) → @DirtiesContext 类→afterTestClass 清缓存容器。

→ 引出 8-A: @MockBean — 依赖注入监听器注入真实 Bean, 而 @MockBean 用 Mockito 生成 mock 替换目标 Bean 供注入。
