# SCC-2 @RefreshScope 热刷新 — 时空溯源 (代码注释锚, git shallow)

> git shallow (单提交) 无法考古; 溯源以 gh-* 编号 + @since 注释 + @Deprecated 为锚。

## 演进链 (代码痕迹实证)

| 阶段 | 事件 | 证据 |
|:--:|:--|:--|
| 远古 | **GenericScope 诞生** (Spring Cloud 1.x): SimpleThreadScope 扩展 + 双检懒创建 + 代理 | GenericScope.java:73 (extends SimpleThreadScope) |
| gh-349 | **UndeclaredThrowableException 还原** — 代理调目标抛异常需还原原始异常 | GenericScope.java:485-487 ("see gh-349") |
| 2.x | **LockedScopedProxyFactoryBean 引入** — 代理 + MethodInterceptor 双角色 + 方法级读锁 | GenericScope.java:433-502 |
| gh-678 | **LegacyContextRefresher web-application-type 修复** — REACTIVE/SERVLET 场景失败 | LegacyContextRefresher.java:69 ("gh-678") |
| 3.1 | GenericScope 稳定标记 | GenericScope.java:70 (@since 3.1) |
| 4.1.0 | **RefreshScopeLifecycle 新增** — 重启刷新钩子 | RefreshScopeLifecycle.java:31 (@since 4.1.0) |
| 4.x | **双刷新器并存**: ConfigDataContextRefresher (新, 重跑 EPP) vs LegacyContextRefresher (@Deprecated) | ConfigDataContextRefresher.java:47/52 + LegacyContextRefresher.java:42 |

## 版本相关性结论

- **双锁 (ReadWriteLock) 是长期稳定核心** — 从 LockedScopedProxyFactoryBean 引入至今未变 (刷新安全根本)
- **gh-349 是代理异常还原的历史修复** — 面试讲"代理调目标抛异常"时引用有据
- **4.x 刷新器分代**: ConfigData (重跑全部 EnvironmentPostProcessor, 支持 config.import) vs Legacy (旧属性源) — 与 SCC-1 的 bootstrap 双轨制同源演进
- **RefreshScopeLifecycle 4.1.0 新增** — 说明刷新面仍在演化 (重启刷新新需求)
