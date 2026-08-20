# MI-5 MeterBinder / JVM Binder 域 — outline 收敛版

> 首批深读: `MeterBinder` / `JvmGcMetrics` / `JvmThreadMetrics` / `ExecutorServiceMetrics` / `JvmMemoryMetrics` / `JvmHeapPressureMetrics` / `ClassLoaderMetrics` / `JvmCompilationMetrics`
> harness: `MiniMI5` **16/16 PASS** | 日期: 2026-08-17

## 一、域定位
- `MeterBinder` 只有一个入口：`bindTo(MeterRegistry)`
- binder 域是 **状态源 → meter 注册** 的消费层
- MI-5 位于拓扑末端，只消费 MI-1~MI-4 的 meter 家族与 registry 能力

## 二、分型
- Pull 型：`JvmThreadMetrics` / `JvmMemoryMetrics` / `ClassLoaderMetrics` / `JvmCompilationMetrics`
- Event 型：`JvmGcMetrics` / `JvmHeapPressureMetrics`
- Wrapper 型：`ExecutorServiceMetrics`

## 三、JvmGcMetrics
- 前置探测:
  - management extensions 是否存在
  - GC notifications 是否可用
  - 当前 GC 是否 generational
- 绑定后注册:
  - `jvm.gc.max.data.size`
  - `jvm.gc.live.data.size`
  - `jvm.gc.memory.allocated`
  - `jvm.gc.memory.promoted`（仅 generational）
  - `jvm.gc.cpu.time`（仅 JDK/实现支持时）
- 监听 GC notification:
  - concurrent phase → `jvm.gc.concurrent.phase.time`
  - pause phase → `jvm.gc.pause`
  - tags: `gc` / `action` / `cause` + extra tags
- 同时更新 allocated / promoted / live/max data size
- `close()` 必须清理 listener

## 四、JvmHeapPressureMetrics
- 构造阶段就 `monitor()`，立即挂 GC listener
- `bindTo()` 只负责把已有内部状态暴露为 gauge
- 输出:
  - `jvm.memory.usage.after.gc{area=heap,pool=long-lived}`
  - `jvm.gc.overhead`
- `jvm.gc.overhead = gcPauseSum.poll() / min(elapsed, lookback)`
- concurrent GC phase 不计入 overhead
- `close()` 清理 listener

## 五、ExecutorServiceMetrics
- 既能做 pool 状态监控，也能包装 executor 记录任务耗时
- `monitor(...)` 三路线:
  - `Executor` → `TimedExecutor`
  - `ExecutorService` → `TimedExecutorService`
  - `ScheduledExecutorService` → `TimedScheduledExecutorService`
- direct `Executor` 计时器名是:
  - `executor.execution`
  - `executor.idle`
- `ExecutorService`/`ThreadPoolExecutor` 另注册状态 gauges，例如:
  - `executor.queued`
  - `executor.pool.size` / `executor.pool.core` 等（测试覆盖）
- `metricPrefix` 通过 `sanitizePrefix()` 规范化：空白→空串，非空自动补 `.`
- `registeredMeterIds` 由 wrapper 持有，用于 shutdown 时清理已注册 metrics
- 对 JDK 私有 executor 解包可能受模块反射限制影响（测试覆盖 `Executors` 私有类场景）

## 六、JvmThreadMetrics
- 固定 meters:
  - `jvm.threads.peak`
  - `jvm.threads.daemon`
  - `jvm.threads.live`
  - `jvm.threads.started`
- 若 `getAllThreadIds()` 可用，则按每个 `Thread.State` 注册状态 gauge
- 若底层 VM 不支持（如 SubstrateVM），捕获 `Error` 并跳过状态 gauges
- 1.16.0 起引入 conventions；extraTags 与 convention 不一定自动叠加，文档已明确

## 七、JvmMemoryMetrics / ClassLoaderMetrics / JvmCompilationMetrics
- `JvmMemoryMetrics`:
  - buffer pool: count / memory.used / total.capacity
  - memory pool: used / committed / max
- `ClassLoaderMetrics`:
  - current loaded class count → Gauge
  - total loaded / unloaded → FunctionCounter
- `JvmCompilationMetrics`:
  - 仅在 `CompilationMXBean` 存在且支持时注册 `jvm.compilation.time{compiler=*}`

## 八、测试收敛要点
- `JvmGcMetricsTest`: close/GC metrics availability/cpu time/pause-cycle collectors/size metrics not zero
- `ExecutorServiceMetricsTest`: direct executor、thread pool、scheduled pool、shutdown cleanup、queued submissions、fork join delayed tasks、private class reflective access
- `JvmThreadMetricsTest`: extra tags、state counting、OTel conventions
- `JvmMemoryMetricsTest` / `ClassLoaderMetricsTest` / `JvmCompilationMetricsTest`: 基础注册 + extra tags / conventions

## 九、关键结论
- binder 域最重要的不是单个 meter，而是**生命周期与可移植性**：JMX listener cleanup、模块反射、不同 JVM 实现能力探测
- `ExecutorServiceMetrics` 是 MI-5 的复杂度峰值：同时处理状态 metrics、计时 wrapper、metricPrefix、shutdown cleanup
- `JvmGcMetrics` 与 `JvmHeapPressureMetrics` 都是 listener 型 binder，`close()` 是语义一部分，不是可选细节