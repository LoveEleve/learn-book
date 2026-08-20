# C-17 TestContext — 测试上下文框架 (TestContextManager → 缓存 → 监听器)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | TestContextManager(约500行)+TestExecutionListener(约330行)+ContextCache(约230行)+DefaultTestContext(约200行)+MergedContextConfiguration
> 基线: C-16 MockMvc — webAppContextSetup 背后是 TestContext 框架加载并缓存容器 — 本域展开测试上下文生命周期; 原始执行计划 8-2

---

## §0.8

- 🟡 Working，1篇 — 门面(TestContextManager: beforeTestClass→prepareTestInstance→beforeTestMethod→beforeTestExecution→afterTestMethod→afterTestClass) → 监听器分发(TestExecutionListener 各阶段回调: 依赖注入/事务/脏上下文) → 容器缓存(ContextCache: MergedContextConfiguration 作键, get/put, 跨测试类复用) → 加载链(DefaultTestContext.getApplicationContext→CacheAwareContextLoaderDelegate.loadContext)
- 设计模式: [模式: 门面]—TestContextManager 统一生命周期; [模式: 观察者]—TestExecutionListener 回调; [模式: 缓存]—ContextCache 复用容器

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| TestContextManager.java:91,251 | 门面 | **生命周期**: prepareTestInstance L251: updateState→遍历 TestExecutionListener.prepareTestInstance — 测试实例准备入口 | High |
| TestContextManager.java:220 | 阶段 | **beforeTestClass**: 遍历 listeners.beforeTestClass — 测试类级前置(如开启日志) | High |
| TestExecutionListener.java:126,173,198 | 监听器接口 | **回调点**: prepareTestInstance(L173)/beforeTestMethod(L198)/beforeTestExecution/afterTestMethod(L267) — 各阶段挂钩 | High |
| DefaultTestContext.java:47,129 | 上下文 | **getApplicationContext L129**: cacheAwareContextLoaderDelegate.loadContext(mergedConfig) — 从缓存取/懒加载容器 | High |
| ContextCache.java:99,107 | 缓存 | **get(mergedConfig) L99 / put(mergedConfig, ctx) L107**: key=MergedContextConfiguration(位置/配置类/profiles/加载器) — 相同配置复用同一容器 | High |
| MergedContextConfiguration.java | 配置键 | **合并配置**: 从 @ContextConfiguration/@SpringBootTest 解析出 locations/classes/activeProfiles/contextLoader — 决定缓存键与加载方式 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 门面+监听器+缓存约 1100 行 — 知识主线: "TestContextManager 驱动生命周期 → 监听器分发 → 容器缓存复用". 1篇 (~46行) 按"门面→缓存→监听器"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | TestContextManager 生命周期 (prepareTestInstance→before/after 各阶段) | 🔴 | **为什么🔴**: 测试框架的核心门面 — 所有测试行为的驱动时序 |
| P1-2 | ContextCache 容器缓存 (MergedContextConfiguration 键 + get/put) | 🔴 | **为什么🔴**: 测试为什么快 — 相同配置跨测试类共享同一容器, 避免重复启动 |
| P1-3 | TestExecutionListener 回调机制 (prepareTestInstance/beforeTestMethod) | 🔴 | **为什么🔴**: 扩展点 — 依赖注入/事务/脏上下文清理全靠监听器 |
| P2-1 | DefaultTestContext.getApplicationContext 加载链 (CacheAwareContextLoaderDelegate) | 🟡 | **为什么🟡**: 容器懒加载与缓存结合 |
| P2-2 | MergedContextConfiguration 配置键 (classes/profiles/loader) | 🟡 | **为什么🟡**: 缓存键的构成 — 什么变了会导致新容器 |
| P3-1 | 常用监听器 (DependencyInjection/Transactional/DirtiesContext) | 🟢 | **为什么🟢**: 开箱行为 — @Autowired 注入/事务回滚 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **生命周期门面** (TestContextManager 各阶段) | 🔴 | 测试执行时序 |
| B | **容器缓存** (ContextCache + MergedContextConfiguration) | 🔴 | 测试性能核心 |
| C | **监听器与加载** (TestExecutionListener + 常用实现) | 🟡 | 扩展点与注入 |

> **Cluster A (§1)**: TestContextManager 全生命周期 + TestExecutionListener 分发
> **Cluster B (§2)**: ContextCache(get/put) + MergedContextConfiguration 键 + DefaultTestContext 加载链
> **Cluster C (§3)**: 常用 TestExecutionListener(依赖注入/事务) + 与 MockMvc/webAppContextSetup 衔接

→ 引出 8-A: @MockBean — 测试里替换 Bean 依赖: 依赖注入监听器注入 @Autowired, 而 @MockBean 用 Mockito 替换目标 Bean

(End of file - total 61 lines)
