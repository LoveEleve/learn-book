# SW-5A storage core abstraction + persistence timer — Pass 0 发现

> 模块: `server-core storage abstraction`
> 关联实现族: `server-storage-plugin/*`
> 日期: 2026-08-18

## 1. 域定位

`SW-5A` 只聚焦存储抽象与统一批处理调度，不进入具体 backend：
- `StorageModule`
- `StorageDAO`
- `StorageBuilderFactory`
- `StorageModels`
- `PersistenceTimer`
- 与其直接相关的 `IBatchDAO` / `StorageData` / `Model` 共同契约

这是所有 JDBC / ES / BanyanDB 实现的共同上游。

## 2. 主链

### 写入链

```text
worker data
  -> PersistenceWorker.buildBatchRequests()
  -> PersistenceTimer.extractDataAndSave()
  -> IBatchDAO.flush(...)
  -> backend batch write
```

### 模型链

```text
annotated stream/entity class
  -> StorageModels.add(...)
  -> retrieval annotations
  -> SQL / BanyanDB / ES model extension
  -> Model list / listeners
```

### builder 链

```text
StorageBuilderFactory
  -> builderTemplate()
  -> builderOf(dataType, defaultBuilder)
  -> runtime builder selection
```

## 3. 当前源码能力

### 3.1 StorageModule
- 统一暴露 `StorageBuilderFactory`、`StorageDAO`、`IBatchDAO`、`IHistoryDeleteDAO`
- 暴露大量 query/profiling/management DAO 接口
- 也是 query service 获取 backend DAO 的统一入口

### 3.2 StorageDAO
- 作为 DAO factory
- 提供 metrics / record / none-stream / management 四类写入 DAO 构造

### 3.3 StorageBuilderFactory
- 默认实现直接返回静态 builder
- 支持存储插件覆盖 builder superclass 和模板路径
- 支持按 `StorageData` 类型选择实际 builder

### 3.4 StorageModels
- 读取 `@Storage` / `@Column` / SQLDatabase / ElasticSearch / BanyanDB 注解
- 生成 `Model` 与 `ModelColumn`
- 维护 listener 回调
- 组合 backend-specific model extension

### 3.5 PersistenceTimer
- 从 `TopNStreamProcessor` 与 `MetricsStreamProcessor` 收集 workers
- prepare 阶段并发执行 `buildBatchRequests()` 与 `endOfRound()`
- `endOfRound()` 在 prepare 异常路径也必须执行，保证每轮生命周期收尾
- empty request list 直接跳过 flush
- 统一在 `future.whenComplete(...)` 中调用 `batchDAO.endOfFlush()`、关闭 metrics timer、记录 error counter
- 通过 `isStarted` 控制定时调度只注册一次

## 4. 当前测试现实

### 已有测试
- `PersistenceTimerTest`
  - 只覆盖 happy-path：多个 mock worker -> `flush()` 汇总请求数
- `StorageModelsTest`
  - 覆盖基础列、storageOnly、SQL composite index

### 未覆盖的高风险 seam
- `PersistenceTimer` empty batch / partial failure / exception path
- `PersistenceTimer.isStarted` 与重复 `start()` 语义
- `batchDAO.endOfFlush()` 是否在 prepare 异常、flush 异常时仍执行
- `StorageModels.addModelListener(...)` 对已有模型的立即回放
- `StorageModels` 对非法注解组合、缺少列、blank timestamp/traceId/spanId 的报错
- `StorageBuilderFactory.Default` 以外的 builder 选择契约

## 5. 首轮质疑点

### Q1: PersistenceTimer 的 `whenComplete` 是否会吞掉 prepare/flush 异常，导致 worker round 状态与 error counter 不一致
### Q2: `batchDAO.endOfFlush()` 是否在 `CompletableFuture.allOf(...)` 异常时仍稳定调用一次
### Q3: `isStarted` 是否在并发或重复 start 下产生双重调度风险
### Q4: `StorageModels.addModelListener(...)` 是否对已有模型与后续新增模型都正确回放
### Q5: `StorageModels` 对 annotation 空值 / 组合错误的报错是否完整且 backend-specific extension 一致
### Q6: 默认 builder factory 是否在插件未覆盖时保持最小可用契约

## 6. Pass0 结论

SW-5A 是一个很好的“抽象层真实缺陷”挖掘入口：
- `PersistenceTimer` 有明显的异常与生命周期 seam
- `StorageModels` 有复杂注解拼装逻辑但测试仍偏 happy-path

下一步优先：
1. 深审 `PersistenceTimer` 的 empty/failure path
2. 为 `StorageModels` listener 与非法注解组合补 harness
3. 仅在这些统一契约收敛后，再进入 JDBC/ES/BanyanDB 子域
