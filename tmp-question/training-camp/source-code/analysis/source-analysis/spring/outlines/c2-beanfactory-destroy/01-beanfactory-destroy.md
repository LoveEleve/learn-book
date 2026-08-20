# C-2 BeanFactory 销毁与回收 — `DisposableBeanAdapter`、销毁链与资源释放

> 依赖 S1-3 Bean 生命周期 + C-1 ApplicationContext 生命周期 | 🟡 Working | 2 KP | [模式: 模板方法 + 责任链]

**读者处境**: `@PreDestroy`、`DisposableBean.destroy()`、`destroy-method` 三种销毁方式的执行顺序是什么？`DisposableBeanAdapter` 是什么？销毁失败会怎样？

### 1. 销毁链的三种来源与执行顺序

场景: 一个 Bean 同时有 `@PreDestroy`、`DisposableBean.destroy()` 和自定义 `destroyMethod`——容器关闭时它们按什么顺序执行？

源码路径:
- `DisposableBeanAdapter.java` — 统一包装销毁逻辑
- 执行顺序：`DestructionAwareBeanPostProcessor.postProcessBeforeDestruction`（含 `@PreDestroy`）→ `DisposableBean.destroy()` → 自定义 `destroyMethod`

关键设计: **Why 三种销毁方式都收进一个 Adapter？** 每个 Bean 的销毁逻辑来源不同，但容器关闭时需要统一管理执行顺序和异常处理。`DisposableBeanAdapter` 把这些来源包装成统一的 `Runnable`，容器遍历 `disposableBeans` 时只需调用 `run()`。

### 2. 销毁顺序与依赖关系

场景: A 依赖 B，关闭时先销毁 A 还是 B？

源码路径:
- `DefaultListableBeanFactory.destroySingletons()` → 遍历 `disposableBeans`（按依赖关系逆序）
- `registerDisposableBeanIfNecessary()` 在 Bean 创建时就注册销毁回调

关键设计: **Why 按依赖关系逆序销毁？** 依赖方持有被依赖方的引用——先销毁依赖方释放引用，再销毁被依赖方。否则被依赖方先销毁，依赖方仍持有其引用，可能导致资源泄漏。

### 3. 销毁失败的处理

场景: 某个 Bean 的 `@PreDestroy` 抛异常，后续 Bean 的销毁还会继续吗？

源码路径:
- `DefaultListableBeanFactory.destroySingletons()` → 每个 Bean 的销毁独立 try/catch
- 某个 Bean 销毁失败不影响其他 Bean 的销毁
- 最后统一收集并报告所有销毁异常

关键设计: **Why 销毁异常不阻断后续销毁？** 容器关闭时，所有 Bean 都需要有机会释放资源。如果一个 Bean 的销毁异常阻断了后续 Bean 的销毁，会导致资源泄漏。Spring 选择"尽量多销毁，最后统一报告异常"的策略。

→ 引出 D-1: Spring 运行时诊断（循环依赖诊断、事务失效诊断、AOP 代理诊断、条件注解诊断、启动慢诊断）。
