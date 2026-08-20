# SW-5A storage core abstraction + persistence timer — Pass 2 问题收敛

> 日期: 2026-08-18

## Q1: PersistenceTimer 的 prepare 异常是否会遗漏 worker round 收尾

结论：确认存在真实缺陷，已修复。

`PersistenceTimer.extractDataAndSave(...)` 原先在 `buildBatchRequests()` 成功返回后才调用 `worker.endOfRound()`。当 prepare 阶段抛出异常时，`CompletableFuture.allOf(...)` 直接异常完成，worker 的 round 收尾被跳过。

修复后，`worker.endOfRound()` 位于 prepare 阶段的 `finally` 中，因此无论 `buildBatchRequests()` 返回空列表、正常返回请求，还是抛出异常，都会完成 round 通知。`prepareLatencyTimer.close()` 也在内层 `finally` 中执行。

依据：
- `PersistenceWorker.endOfRound()` 的抽象契约明确说明它是每轮结束通知。
- `MetricsPersistentWorker.endOfRound()` 清理过期 session cache。
- `TopNWorker.endOfRound()` 当前为空实现，但仍需遵守统一 worker 生命周期。

回归测试：
- `PersistenceTimerEdgeCaseTest.shouldStillEndRoundWhenBuildBatchRequestsFails`
- 断言 prepare 异常、两个 worker 都调用 `endOfRound()`、`errorCounter.inc()` 与 `batchDAO.endOfFlush()` 均执行。

验证结果：
- 定向 reactor：`BUILD SUCCESS`
- `server-core` 完整 reactor：219 tests，0 failures，0 errors，`BUILD SUCCESS`

## Q2: prepare 异常时是否仍调用 batchDAO.endOfFlush()

当前实现仍在 `future.whenComplete(...)` 中统一调用 `batchDAO.endOfFlush()`，prepare 异常测试已确认该调用发生一次。该行为与统一 round flush 生命周期一致，暂未发现 backend-specific 反例。

## Q3: StorageModels listener 回放是否正确

结论：现有实现正确。

`addModelListener(...)` 先注册 listener，再遍历已有 `models` 回放。新增 harness 已确认：先 `add(...)` 再 `addModelListener(...)` 时会收到已有 model；后续再 `add(...)` 新 model 时同一个 listener 会继续收到通知。

## Q4: StorageModels 对非法注解组合与空值报错是否完整

结论：发现并修复 1 个真实缺陷。

`StorageModels.add(...)` 原先只在类上存在 `@BanyanDB.Trace.TraceIdColumn` 或 `@BanyanDB.Trace.IndexRule.List` 时，才进入 trace index rule 解析分支。若类只声明单个 `@BanyanDB.Trace.IndexRule`，即使定义非法（例如 `columns = {}`），也会被完全跳过，不会触发 `createTraceIndexRule(...)` 校验。

修复后，单个 `@BanyanDB.Trace.IndexRule` 也纳入同一分支，和 `List` 形式一样进行校验并写入 `traceIndexRules`。

回归测试：
- `StorageModelsAdvancedTest.shouldReplayExistingModelsToNewListener`
- `StorageModelsAdvancedTest.shouldRejectBlankTimestampColumn`
- `StorageModelsAdvancedTest.shouldRejectAdditionalEntityOnNonRecordModel`
- `StorageModelsAdvancedTest.shouldRejectMissingTraceIndexRuleColumns`

验证结果：
- 定向 reactor：`BUILD SUCCESS`
- `server-core` 完整 reactor：223 tests，0 failures，0 errors，`BUILD SUCCESS`

## Q5: PersistenceTimer.isStarted 的重复启动语义是否安全

结论：发现并修复 1 个真实缺陷。

`PersistenceTimer.start(...)` 原先虽然用 `isStarted` 避免重复注册 scheduled executor，但在进入判断前就无条件重建 `prepareExecutorService`。因此当 `start()` 被重复调用时，会静默替换并泄漏旧线程池，破坏 start 的幂等语义。

修复后，prepare executor 的创建与 scheduled executor 注册都放在同一个 `if (!isStarted)` 分支中。已启动后再次调用 `start()` 不会替换已有 executor。

回归测试：
- `PersistenceTimerStartSemanticsTest.shouldNotReplacePrepareExecutorWhenAlreadyStarted`

验证结果：
- 定向 reactor：`BUILD SUCCESS`
- `server-core` 完整 reactor 在本环境下执行到 `PersistenceTimerTest` 后因命令超时被截断，未观察到失败栈；此前两轮 `server-core` 全量回归均已 `BUILD SUCCESS`。

## Q6: 默认 builder factory 与模板契约是否保持最小可用

结论：当前默认实现符合设计，未发现真实缺陷。

`StorageBuilderFactory.Default`：
- `builderTemplate().getSuperClass()` 返回 `StorageBuilder` 全限定类名。
- `builderTemplate().getTemplatePath()` 返回 `metrics-builder`。
- `builderOf(...)` 原样返回 stream 静态声明的 `defaultBuilder`，不按 `dataType` 做额外替换。

回归测试：
- `StorageBuilderFactoryContractTest.shouldReturnDefaultBuilderAndTemplate`

验证结果：
- 定向 reactor：`BUILD SUCCESS`
- 未修改生产代码。
