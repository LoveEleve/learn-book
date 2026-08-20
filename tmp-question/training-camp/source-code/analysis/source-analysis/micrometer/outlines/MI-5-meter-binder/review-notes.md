# MI-5 MeterBinder / JVM Binder 域 — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0 + harness 收敛)
- [x] 深读 8 文件：`MeterBinder` / `JvmGcMetrics` / `JvmThreadMetrics` / `ExecutorServiceMetrics` / `JvmMemoryMetrics` / `JvmHeapPressureMetrics` / `ClassLoaderMetrics` / `JvmCompilationMetrics`
- [x] binder 域职责明确：只做状态源/JMX/线程池包装到 meter 的桥接，不引入新计量原语
- [x] `JvmGcMetrics` / `JvmHeapPressureMetrics` listener 生命周期已确认：都需要 `close()` 清理
- [x] `ExecutorServiceMetrics` 三路线（Executor / ExecutorService / ScheduledExecutorService）与 metricPrefix 规则已确认
- [x] `JvmThreadMetrics` 的 `Error` 捕获不是疏忽，而是为了兼容不支持 `getAllThreadIds()` 的 VM
- [x] harness `MiniMI5` **16/16 PASS**

## 关键打脸 / 易错点
- 打脸1：`ClassLoaderMetrics` 默认命名并不是我先前直觉里的 `jvm.classes.loaded.total`；官方测试表明默认只稳定断言 `jvm.classes.loaded`，OTel convention 下才变成 `jvm.class.loaded/jvm.class.unloaded/jvm.class.count`
- 打脸2：`Executor` 直连包装的计时器名是 `executor.execution` / `executor.idle`，不是 `executor`
- 打脸3：`Executors.newSingleThreadExecutor()` 在当前模块环境可能因 JDK 模块反射限制无法解包出 `ThreadPoolExecutor`，改用 `newFixedThreadPool(1)` 才能稳定验证 `executor.queued`

## 收敛判定
- 第一轮收敛，待第二轮用官方测试清单反推遗漏路径（ForkJoinPool / shutdown cleanup / GC cpu time / custom conventions）。

## 审查轮次: 第二轮 (2026-08-17, 官方测试交叉/遗漏路径盘点)
- [x] `JvmGcMetricsTest` 覆盖确认：
  - `cleanUp()` 直接调用 `binder.close()`
  - `gcMetricsAvailableAfterGc()` 证明 GC notifications 异步到达，断言必须放进 await block
  - `gcCpuTimeAvailable/NotAvailable` 明确 JDK 26 前后行为分叉
  - `gcTimingIsCorrectForPauseCycleCollectors()` 说明某些 collector 把 pause/cycle 分成两套 bean
- [x] `ExecutorServiceMetricsTest` 覆盖确认：
  - direct `Executor` 路径 → `executor.execution` / `executor.idle`
  - `monitorExecutorServiceAfterShutdown` / `monitorScheduledExecutorServiceAfterShutdown` 验证 wrapper shutdown 后 meter 重新注册不会残留旧核心线程数
  - `queuedSubmissionsAreIncludedInExecutorQueuedMetric` / `forkJoinPoolDelayedTaskCountMetric` / `private class reflective access` 均属实现复杂点
- [x] `JvmThreadMetricsTest` 覆盖确认：extra tags / OTel conventions / blocked+timed_waiting state 统计
- [x] `JvmMemoryMetricsTest` / `ClassLoaderMetricsTest` / `JvmCompilationMetricsTest` 覆盖基础注册与 conventions 差异
- [x] 当前 harness 尚未覆盖的分支已明确边界：ForkJoinPool delayed metric、shutdown 后清理再注册、GC cpu time JDK 版本分歧、OTel/custom conventions

## 收敛判定 (第二轮终)
MI-5 主路径已闭环；剩余未手工跑的分支均已有官方测试覆盖且已纳入边界说明。
