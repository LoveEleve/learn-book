# SCC-2 @RefreshScope 热刷新 — 知识规划 (KP)

> 域: SCC-2 | 级别: 🔴 | 方案: A | 大纲: outlines/scc2-refreshscope/outline.md (6 节)

## §01 域定位

@RefreshScope = 配置热刷新的作用域核心。代理 (LockedScopedProxyFactoryBean) + 双锁 (cache ConcurrentMap + ReadWriteLock) + 双阶段刷新 (环境变更 → Scope 清空) + 懒重建。面试必问机制。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| scope/GenericScope.java | 502 | 作用域核心: cache/locks/双检/destroy | 1,2 |
| scope/refresh/RefreshScope.java | — | refresh/refreshAll/事件/JMX | 3 |
| scope/refresh/RefreshScopeRefreshedEvent.java | — | 刷新事件 | 3 |
| scope/ScopeCache.java + StandardScopeCache | — | 缓存抽象 | 2 |
| scope/thread/ThreadScope + ThreadLocalScopeCache | — | 线程作用域变体 | 2 |
| refresh/ContextRefresher.java | 194 | 双阶段刷新模板 | 4 |
| refresh/ConfigDataContextRefresher.java | — | 新式刷新器 (重跑 EPP) | 5 |
| refresh/LegacyContextRefresher.java | — | 旧式 (@Deprecated) | 5 |
| refresh/RefreshScopeLifecycle.java | — | 重启刷新钩子 (4.1.0) | 6 |

## §05 闭环要点 (Pass 2 内化)

### q1 代理+双锁
LockedScopedProxyFactoryBean (代理+Interceptor): 方法级读锁 (L460-490) + 异常还原 (gh-349); cache 无锁读 + destroy 写锁 (L126-142)。

### q2 双阶段刷新
ContextRefresher.refresh (L92-96): refreshEnvironment (before/after 对比 + EnvironmentChangeEvent) → scope.refreshAll (清缓存 + RefreshScopeRefreshedEvent)。

### q3 新旧刷新器
ConfigDataContextRefresher (重跑全部 EnvironmentPostProcessor L68-93) vs LegacyContextRefresher (@Deprecated)。

### q4 事件链
EnvironmentChangeEvent (环境变更, 先) → RefreshScopeRefreshedEvent (Scope 清空, 后)。

## §06 负面空间 (6 条)

不自动感知配置变更 / 不做字段级更新 / 不跨上下文刷新 / 不保证跨 Bean 原子性 / 不刷 singleton/prototype / 不处理销毁异常恢复

## §07 交叉引用

- ← SCC-1 Bootstrap (配置拉取源) + Spring AOP scoped proxy
- → SCC-8 RefreshEndpoint (刷新触发面) + SCC-9 配置加密 (decrypt 重跑)
- 另见: Apollo 推送式 / Spring Boot @ConfigurationProperties 重绑定
