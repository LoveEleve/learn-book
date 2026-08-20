# C-1 ApplicationContext 完整生命周期 — 从创建到关闭的全链路

> 依赖 S2-1 refresh + S2-8 AppContext | 🟡 Working | 3 KP | [模式: 模板方法]

**读者处境**: `refresh()` 讲的是启动，但 `close()` 呢？Bean 销毁顺序是什么？`SmartLifecycle` 在什么时候调用？容器关闭时哪些资源会被释放？

### 1. ApplicationContext 生命周期的完整阶段

场景: 从 `new ApplicationContext()` 到 `close()`，容器经历：创建 → 初始化 → refresh → 运行 → close → 销毁。

关键阶段:
1. 构造：创建 BeanFactory
2. `refresh()`：12 步启动链
3. 运行态：处理请求、创建 Bean
4. `close()`：触发销毁链

### 2. `close()` 时的 Bean 销毁顺序

场景: 容器关闭时，`DisposableBean`、`@PreDestroy`、`destroy-method` 的执行顺序是什么？

源码路径:
- `AbstractApplicationContext.close()` → `doClose()` → `destroyBeans()` → `beanFactory.destroySingletons()`
- `DefaultListableBeanFactory.destroySingletons()` → 遍历 `disposableBeans` → 按依赖关系逆序销毁

关键设计: **Why 销毁顺序是依赖关系的逆序？** 如果 A 依赖 B，A 持有 B 的引用。销毁时先销毁 A（释放对 B 的引用），再销毁 B。否则 B 先销毁，A 仍持有 B 的引用，可能导致资源泄漏或 NPE。

### 3. `SmartLifecycle` — 容器级生命周期回调

场景: 某些 Bean 需要在容器启动完成后自动启动（如消息监听器），在容器关闭前优雅停止。

源码路径:
- `SmartLifecycle` 接口：`start()` / `stop()` / `isRunning()` / `isAutoStartup()`
- `AbstractApplicationContext.finishRefresh()` → `start()` 调用所有 `SmartLifecycle` Bean
- `AbstractApplicationContext.doClose()` → `stop()` 调用所有 `SmartLifecycle` Bean

关键设计: **Why `SmartLifecycle` 而不是 `@PostConstruct`？** `@PostConstruct` 在 Bean 初始化时调用，此时其他 Bean 可能还没创建完。`SmartLifecycle.start()` 在所有单例创建完后调用，此时容器已完全就绪，适合启动需要依赖其他 Bean 的后台任务。

→ 引出 C-2: BeanFactory 销毁与回收（`DisposableBeanAdapter`、销毁链、资源释放）。
