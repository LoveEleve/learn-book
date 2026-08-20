# H-8 泄漏检测 — ProxyLeakTask / leakDetectionThreshold

> 依赖 H-12 代理 + H-3/H-4 生命周期 (复用) | 🟡 Working | 6 KP | [模式: 延迟任务 + 空对象 + 生命周期钩子]

**读者处境**: 应用借了连接忘了 close() 会怎样?Hikari 怎么发现泄漏?为什么默认不检测?

### 1. 泄漏检测入口 — schedule / cancel + NO_LEAK

场景: 借出连接时怎么安排泄漏检测?怎么取消?

源码路径:
- `ProxyLeakTaskFactory.java:38,40` — **工厂**: `schedule(poolEntry)`(L38) → `(leakDetectionThreshold == 0) ? ProxyLeakTask.NO_LEAK : scheduleNewTask(poolEntry)`(L40) — 阈值 0 时不检测(返回空对象)
- `ProxyLeakTask.java:68,70` — **调度**: `schedule(...)`(L68) → `executorService.schedule(this, leakDetectionThreshold, MILLISECONDS)`(L70) — 延迟阈值后执行
- 衔接: **borrow 时 schedule**(H-3 L179 `leakTaskFactory.schedule(poolEntry)`), **close 时 cancel**(H-4 L246 `leakTask.cancel()`)

关键设计: **Why borrow schedule / close cancel？** 借出连接时安排"若阈值后未还就报告"的延迟任务; 连接正常关闭时取消 — 定时器与借用生命周期绑定, 不额外全局扫描。**Why NO_LEAK 空对象？** 未配置 leakDetectionThreshold(默认 0)时用空实现, 避免为每次 borrow 建任务的开销 — 零成本关闭检测。[模式: 延迟任务 + 空对象]

数据流: borrow(H-3) → leakTaskFactory.schedule(poolEntry)(L38) → 阈值0?→ NO_LEAK; 否则 scheduleNewTask → executor.schedule(this, 阈值)(L70) → close(H-4) → leakTask.cancel()(L88)。

### 2. run() — 泄漏报告

场景: 连接在阈值内没归还, 泄漏任务触发做什么?

源码路径:
- `ProxyLeakTask.java:75` — **run()**: 泄漏触发
- `ProxyLeakTask.java:77,85` — **报告**: `isLeaked = true`(L77) + `LOGGER.warn("Connection leak detection triggered for ... stack trace follows", ...)`(L85) — 记录借出点栈追踪
- `ProxyLeakTask.java:88` — **cancel**: 若已泄漏且随后归还, log "unleaked"(L91) — 提示连接之后还了

关键设计: **Why 只报告不自动关闭？** 泄漏检测的职责是**暴露问题**(借出点栈追踪帮开发者定位谁忘 close), 不自动 kill 连接 — 自动关可能中断正在使用的合法长事务; 记录栈让开发者找到泄漏源头。**Why 记录借出点栈？** run() 触发时栈里含"借出连接的那行代码" — 正是排查泄漏需要的信息。[模式: 诊断报告]

数据流: 阈值后连接未还 → run()(L75) → isLeaked=true(L77) → WARN + 借出点栈追踪(L85)。之后若归还 → cancel 里 log "unleaked"(L91)。

### 3. 配置与边界 — leakDetectionThreshold

场景: 怎么开启泄漏检测?阈值语义?

源码路径:
- `HikariConfig.java:66,221,228` — **配置**: `leakDetectionThreshold`(L66, **默认 0=不检测**), getter(L221)/setter(L228)
- 阈值语义: 单位毫秒 — 连接借用超过该时长未归还即视为泄漏
- 边界: 检测机制挂靠在代理(ProxyLeakTask 由 ProxyConnection 持有), 与 H-12 代理族紧密衔接

关键设计: **Why 默认 0？** 泄漏检测有开销(每个借用建任务)— 默认关闭, 用户按需配置(如 10000ms)排查泄漏; 这是"默认零开销, 按需开启"的权衡。**Why 与 H-12 边界？** 泄漏任务由代理连接持有/触发, 机制在 H-8, 代理结构在 H-12 — 边界清晰。[模式: 按需开启 + 复用边界]

数据流: 配 leakDetectionThreshold=10000(L66) → borrow 时 schedule(L70) → 10000ms 未还 → run() 报告泄漏栈。默认 0 → NO_LEAK, 无任务零开销。

→ 引出 H-9: 指标监控 — 泄漏之后: MetricsTracker/PoolStats/Micrometer 的池指标(前置 H-1)。
