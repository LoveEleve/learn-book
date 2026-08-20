# S-6 TransactionHook — 7 生命周期钩子

> 前置: [[S-1-AT两阶段]] (触发面实证) + [[S-5-事务传播]] (同线程模型) | 引出: [[S-9-Spring集成]] | 对照: Spring TransactionSynchronization (同步器)
> 🔴 A | 8 KP | [模式: 观察者 + ThreadLocal 管理器]
> Pass 2 闭环: q1(钩子接口) q2(管理器) q3(触发编排) q4(跨模式复用)

**读者处境**: 怎么在全局事务各个阶段插入自定义逻辑? 钩子怎么注册/触发/清理? 这篇拆 TransactionHook (7 钩子) + TransactionHookManager (ThreadLocal 栈) + 触发编排。

### 1. 钩子接口 — 7 方法定义

场景: 有哪些观察点?
源码路径:
- **接口 7 方法** (TransactionHook:19-55): **beforeBegin / afterBegin / beforeCommit / afterCommit / beforeRollback / afterRollback / afterCompletion** — 执行计划漏列 afterCommit/afterRollback (总数 7 对)
- **分组结构**: begin 周期 2 + commit 周期 2 + rollback 周期 2 + **afterCompletion 1 (收尾)** — 3×2+1
- **afterCompletion**: finally 触发 — **成功/失败都触发**; **仅 Launcher** (S-1)
- **实现**: 业务自定义 (无内置默认) — 观察者模式
关键设计 (q1): **3 周期×2 + 收尾 1**; 全路径收尾钩子。[模式: 观察者]

### 2. 钩子管理器 — ThreadLocal 栈 + 只读视图

场景: 钩子存哪?
源码路径:
- **存储** (TransactionHookManager:32-33): **ThreadLocal\<List\<TransactionHook\>\>** — 线程隔离
- **getHooks** (L35-47): 空 → emptyList; 非空 → **unmodifiableList 只读视图** (触发方不能改列表; ⚠ **每次新建包装非缓存** — 对照 TccHookManager CACHED volatile 缓存, harness 自抓)
- **registerHook** (L49-61): **null → NullPointerException**; lazy ArrayList; 追加 (注册序触发)
- **clear** (L63-66): LOCAL_HOOKS.remove() — **remove 防 ThreadLocal 泄漏**
- **生命周期**: 业务前注册 → 事务周期触发 → **Launcher cleanUp 清除** (S-1); ⚠ **无内置调用者** (spring 模块无 registerHook — 纯 SPI 风格, 业务自定义注册)
关键设计 (q2): **ThreadLocal 隔离 + 只读视图 + remove 清理**。[模式: 线程局部管理器]

### 3. 触发编排 — 7 trigger 的时机与守卫

场景: 钩子什么时候触发?
源码路径:
- **触发点 7 处** (TransactionalTemplate:223-315,321-391): beginTransaction (before→begin→after) / commitTransaction (before→commit→after) / rollbackTransaction (before→rollback→after) / finally afterCompletion
- **守卫差异**: begin/commit/rollback trigger 由外层方法 Launcher 守卫; **afterCompletion 显式仅 Launcher** (L381-391); 注释 "Of course, the hooks will still be triggered" (L124)
- **异常语义** (L322-328): 每钩子 **catch(Exception) → LOGGER.error → 继续下一个** — 钩子失败不破坏主流程
- **cleanUp** (L393-402): **Launcher 才 clear** — Participant 共享外层钩子不清; ⚠ **finally 顺序**: resumeGlobalLockConfig → afterCompletion → cleanUp (L141-146) — 钩子执行时锁配置已恢复
关键设计 (q3): **模板 7 点嵌入 + Launcher 守卫 + 异常不中断**。[模式: 模板嵌入]

### 4. 跨模式复用 — Saga/TCC 对比

场景: 钩子机制通用吗?
源码路径:
- **Saga 复用**: **Saga 模板完整 7 钩子** (L130-203: beforeBegin/afterBegin/beforeRollback/afterRollback/beforeCommit/afterCommit/afterCompletion) — 与 AT 同构 (模式无关强化); ⚠ **守卫粒度不同**: Saga 每 trigger 内判 Launcher vs AT 外层方法守卫 (同语义两实现)
- **TCC 独立体系** (TccHookManager): **CopyOnWriteArrayList 全局列表 + 缓存不可变视图** (L33-35) + **TccHook 6+ 方法** (before/after × prepare/commit/rollback, TccHook:3-28) — 与 ThreadLocal 对比:
  | 维度 | TransactionHookManager | TccHookManager |
  |:--|:--|:--|
  | 存储 | ThreadLocal (事务级) | 全局列表 (应用级) |
  | 清空 | clear (事务结束) | 无 (常驻) |
- **用途**: TransactionHook = 生命周期观察; TccHook = 门面拦截 (prepare/commit/rollback 前后)
关键设计 (q4): **钩子机制模式无关** (AT/Saga); TCC 独立全局体系。[模式: 复用对比]

## 代码类型
Architecture (钩子机制)

## 负面空间 — TransactionHook 刻意不做的事

- **不做过滤器链**: 无链式上下文传递 — 观察者广播 (无返回值/无中断)
- **不做异步钩子**: 同步触发 — 钩子耗时直接加在事务路径
- **不做钩子重试**: 异常即吞 (log) — 不重试
- **不做跨线程钩子**: ThreadLocal — 子线程需显式传递
- **不做优先级**: 注册序触发 — 无排序机制
- **不做事件总线**: 仅事务生命周期 7 点 — 非通用事件 (对照 Spring ApplicationEvent)

→ 引出: Spring 集成注入 → [[S-9-Spring集成]]
