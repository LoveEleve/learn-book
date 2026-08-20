# MI-5 MeterBinder / JVM Binder 域 — Pass 0 发现

> 首批深读: `MeterBinder`(31) / `JvmGcMetrics`(429) / `JvmThreadMetrics`(124) / `ExecutorServiceMetrics`(548) / `JvmMemoryMetrics`(126) / `JvmHeapPressureMetrics`(158) / `ClassLoaderMetrics`(89) / `JvmCompilationMetrics`(59)
> 日期: 2026-08-17

## 1. 域职责
- `MeterBinder` 只有一个入口：`bindTo(MeterRegistry)`
- binder 域本质是 **把外部状态源/MXBean/线程池/JMX 通知流** 转换为已存在的 meter 家族 (`Gauge/Counter/FunctionCounter/Timer/LongTaskTimer`)
- MI-5 位于拓扑末端：只消费 MI-1~MI-4 的能力，不反向影响底层计量语义

## 2. JVM binder 主分型
### Pull 型（按读取时采样）
- `JvmThreadMetrics`: `Gauge + FunctionCounter`
- `JvmMemoryMetrics`: `Gauge`
- `ClassLoaderMetrics`: `Gauge + FunctionCounter`
- `JvmCompilationMetrics`: `FunctionCounter`

### Push / Event 型（监听通知流并累积）
- `JvmGcMetrics`: `GC notification` → `Timer + Counter + Gauge`
- `JvmHeapPressureMetrics`: `GC notification` → `Gauge + TimeWindowSum`

### Hybrid / Wrapper 型
- `ExecutorServiceMetrics`: 
  - `bindTo` 监控线程池状态
  - `monitor(...)` 返回 `TimedExecutor/TimedExecutorService/TimedScheduledExecutorService` 包装器，附带任务耗时计量

## 3. JvmGcMetrics 核心
- 前置能力探测:
  - `managementExtensionsPresent`
  - `garbageCollectorNotificationsAvailable`
  - `isGenerationalGc`
- 构造时扫描 MemoryPool:
  - `allocationPoolName`
  - `longLivedPoolNames`
- `bindTo(...)` 后注册:
  - `jvm.gc.max.data.size` (Gauge)
  - `jvm.gc.live.data.size` (Gauge)
  - `jvm.gc.memory.allocated` (Counter)
  - `jvm.gc.memory.promoted` (Counter, only generational GC)
  - `jvm.gc.cpu.time` (FunctionCounter, 仅当 `MemoryMXBean.getTotalGcCpuTime()` 存在且 >=0)
- `GcMetricsNotificationListener.handleNotification(...)`:
  - concurrent phase → `jvm.gc.concurrent.phase.time`
  - else → `jvm.gc.pause`
  - tags: `gc/action/cause` + extra tags
  - 同时统计 allocated / promoted / liveDataSize / maxDataSize
- `close()` 必须清理 `NotificationEmitter` listener（类注释明确要求）

## 4. JvmThreadMetrics 核心
- 固定 meters:
  - `jvm.threads.peak`
  - `jvm.threads.daemon`
  - `jvm.threads.live`
  - `jvm.threads.started`
- 若 `ThreadMXBean.getAllThreadIds()` 可用，则按 `Thread.State.values()` 逐个注册 state gauge
- 若不支持（如 SubstrateVM）会捕获 `Error` 并静默跳过 state gauges
- 1.16.0 引入 conventions；extraTags 与 convention tags 并非总自动合并，文档已明确

## 5. ExecutorServiceMetrics 核心
- `monitor(...)` 重载很多，本质分三类:
  - `Executor` → 若不是 `ExecutorService`，仅包装 `TimedExecutor`
  - `ExecutorService` → `bindTo` + 返回 `TimedExecutorService`
  - `ScheduledExecutorService` → `bindTo` + 返回 `TimedScheduledExecutorService`
- `metricPrefix` 通过 `sanitizePrefix()` 规范化（空串 or 自动补 `.`）
- binder 内部维护 `registeredMeterIds`，供包装器 shutdown 时清理 meters
- 支持 `ThreadPoolExecutor` / `ForkJoinPool` / 某些 JDK 私有 executor（需要 `--add-opens`）
- 类级文档明确：**状态监控** 与 **任务耗时** 是两条线；耗时必须通过 wrapper 获得

## 6. JvmMemoryMetrics 核心
- BufferPool 三个 gauge:
  - `jvm.buffer.count`
  - `jvm.buffer.memory.used`
  - `jvm.buffer.total.capacity`
- MemoryPool 三个 gauge:
  - used / committed / max
- conventions 只覆盖部分 meter；未覆盖部分继续拼 `extraTags`
- 值获取统一委托 `JvmMemory.getUsageValue(...)`

## 7. JvmHeapPressureMetrics 核心
- 构造即 `monitor()`：直接注册 GC notification listener（不是 bindTo 时才建立）
- `lookback` + `testEvery` 形成 `TimeWindowSum gcPauseSum`
- 输出:
  - `jvm.memory.usage.after.gc{area=heap,pool=long-lived}`
  - `jvm.gc.overhead` = `gcPauseSum.poll() / min(elapsed, lookback)`，范围意图 `[0..1]`
- concurrent GC cause 不计入 overhead
- 需要 `close()` 清理 listener

## 8. 其他 JVM binder
- `ClassLoaderMetrics`:
  - `jvm.classes.loaded` (Gauge current)
  - `jvm.classes.unloaded` (FunctionCounter)
  - `jvm.classes.loaded.total` (FunctionCounter)
- `JvmCompilationMetrics`:
  - 仅当 `CompilationMXBean` 存在且支持 compilation time 时注册 `jvm.compilation.time{compiler=*}`

## 9. 待 Pass 1 验证 (Q)
- Q1: `JvmGcMetrics.close()` / `JvmHeapPressureMetrics.close()` 的 listener 清理是否有官方测试覆盖
- Q2: `ExecutorServiceMetrics.bindTo(...)` 对 `ThreadPoolExecutor/ForkJoinPool` 实际注册哪些 meter；`registeredMeterIds` 清理链是否闭环
- Q3: `JvmHeapPressureMetrics` 构造即 monitor（先挂 listener）而 bindTo 才暴露 gauges，这个生命周期是否存在空监听窗口/泄漏风险
- Q4: `JvmThreadMetrics` 捕获 `Error` 而不是 `Exception` 的动机和官方测试覆盖
- Q5: `JvmGcMetrics` 的 generational/promotedBytes / allocationPoolName 识别逻辑与不同 GC 名称映射
- Q6: `metricPrefix` + `name` tag 在 `ExecutorServiceMetrics.monitor(...)` 包装路径上的一致性