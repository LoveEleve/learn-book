# C-1-2 SmartLifecycle + 事件发布 + close 收口 — 容器后半段生命周期

> 依赖 C-1-1 + S2-3 事件机制 | 🟡 Working | 2 KP | [模式: 生命周期回调 + 收尾阶段]

**读者处境**: `refresh()` 完成之后容器什么时候真正对外宣布“活了”？`close()` 时哪些组件会优先停止？`SmartLifecycle`、`ContextRefreshedEvent`、`ContextClosedEvent` 到底怎么接在一起？

### 1. `finishRefresh()`：刷新完成、发布 `ContextRefreshedEvent`

场景: 容器所有单例 Bean 都创建完，`finishRefresh()` 负责：
- 清理资源缓存
- 初始化生命周期处理器
- 调用 `LifecycleProcessor.onRefresh()`
- 发布 `ContextRefreshedEvent`

源码路径:
- `AbstractApplicationContext.finishRefresh()`
- `LifecycleProcessor.onRefresh()`
- `publishEvent(new ContextRefreshedEvent(this))`

关键设计: **Why 事件在这里发布？** 因为此时定义世界、实例世界和基础设施世界都已经收口，监听者才能安全观察“容器已经 fully ready”这一事实。

### 2. `SmartLifecycle`：自动启动与分 phase 停止

场景: 某些 Bean（如消息监听器、调度器）在容器刷新完成后要自动启动，容器关闭前要按 phase 有序停止。

源码路径:
- `SmartLifecycle` 接口：`isAutoStartup()` / `getPhase()` / `start()` / `stop()`
- `DefaultLifecycleProcessor`：按 phase 分组启动和停止

关键设计: **Why `SmartLifecycle` 比 `@PostConstruct` 更适合长生命周期后台组件？** `@PostConstruct` 发生在单个 Bean 初始化链里，太早；`SmartLifecycle.onRefresh` 发生在整个容器 ready 之后，更适合需要依赖其它 Bean 已全部就绪的组件。

### 3. `doClose()`：发布 `ContextClosedEvent`、停止 `LifecycleProcessor`、销毁 Bean

场景: 调用 `context.close()` 后，Spring 按顺序：
- 发布 `ContextClosedEvent`
- `lifecycleProcessor.onClose()`
- `destroyBeans()`
- `closeBeanFactory()`

源码路径:
- `AbstractApplicationContext.doClose()`
- `DefaultLifecycleProcessor.onClose()`
- `destroyBeans()`

关键设计: **Why close 不是直接 destroyBeans？** 容器先发布关闭事件，让外部观察者有机会先做业务级收尾；再停掉 lifecycle 组件；最后销毁 Bean。这样“外部收尾 → 组件停止 → Bean 销毁”形成清晰顺序。

→ 引出 C-2: BeanFactory 销毁与 `DisposableBeanAdapter`。
