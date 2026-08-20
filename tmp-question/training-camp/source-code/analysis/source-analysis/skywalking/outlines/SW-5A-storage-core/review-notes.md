# SW-5A storage core abstraction + persistence timer — Review Notes

## Review 1

- 复核 `PersistenceTimer.extractDataAndSave(...)` 的 prepare 异常路径。
- 发现原实现只在 `buildBatchRequests()` 成功后调用 `endOfRound()`。
- 用真实异步 harness 复现：prepare worker 抛异常时 `endOfRound()` 未调用。
- 结论：真实生产代码缺陷，不是测试桩问题。

## Review 2

- 将 `worker.endOfRound()` 放入 prepare `finally`，并保证 prepare timer 在内层 `finally` 中关闭。
- 检查 `PersistenceWorker` 抽象契约：`endOfRound()` 明确是每轮结束通知。
- 检查 worker 实现：`MetricsPersistentWorker` 清理过期 session cache；`TopNWorker` 当前为空实现。
- 结论：异常、空列表、正常列表都应统一调用 round 收尾。

## Review 3

- 检查异常传播：worker prepare 异常仍使 `CompletableFuture.allOf(...)` 异常完成，并由统一 `whenComplete(...)` 记录 error、关闭 all timer、调用 `batchDAO.endOfFlush()`。
- 检查测试隔离：测试关闭 executor，并移除两个单例 worker，避免污染其他测试。
- 检查 telemetry harness：为 histogram timer 提供可关闭 mock，避免测试收尾 NPE。
- 结论：未发现新的生产行为回归。

## Review 4

- 定向命令：`./mvnw -pl oap-server/server-core -am -Dtest=PersistenceTimerEdgeCaseTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`。
- 完整命令：`./mvnw -pl oap-server/server-core -am test`
- 结果：219 tests，0 failures，0 errors，`BUILD SUCCESS`。
- 构建阶段 checkstyle audit 通过；`git diff --check` 通过。

## Review 5

- 新增 `StorageModelsAdvancedTest` 覆盖 listener 回放、blank timestamp column、AdditionalEntity on non-record、单个 trace index rule 非法定义。
- listener 回放、blank timestamp、AdditionalEntity 报错都符合预期。
- 发现真实缺陷：仅声明单个 `@BanyanDB.Trace.IndexRule` 时不会进入校验分支，非法定义被静默接受。
- 以一行生产修复把 `@BanyanDB.Trace.IndexRule.class` 纳入解析条件。

## Review 6

- 定向命令：`./mvnw -pl oap-server/server-core -am -Dtest=StorageModelsAdvancedTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`。
- 完整命令：`./mvnw -pl oap-server/server-core -am test`
- 结果：223 tests，0 failures，0 errors，`BUILD SUCCESS`。
- 本轮新增 1 个生产修复文件、1 个测试文件，`git diff --check` 通过。

## Review 7

- 新增 `PersistenceTimerStartSemanticsTest` 验证 `isStarted=true` 后再次调用 `start()` 不应替换已有 `prepareExecutorService`。
- 测试首次稳定失败，证明 `start()` 在已启动状态下仍会重建线程池。
- 生产修复为把 prepare executor 初始化移动进 `if (!isStarted)` 分支，使 start 保持幂等。

## Review 8

- 定向命令：`./mvnw -pl oap-server/server-core -am -Dtest=PersistenceTimerStartSemanticsTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`。
- 本轮完整 `server-core` 回归在本环境下两次都执行到 `PersistenceTimerTest` 之后因 shell 超时被截断；未观察到失败栈。
- `git diff --check` 通过。

## Review 9

- 首版 builder harness 误用了 `ModuleDefineHolder` 与 `Stream.processor` 类型，无法作为有效证据，已收缩并替换为直接契约测试。
- `StorageBuilderFactoryContractTest` 只验证公开默认实现：模板父类、模板路径、默认 builder 原样返回。
- 定向命令：`./mvnw -pl oap-server/server-core -am -Dtest=StorageBuilderFactoryContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`BUILD SUCCESS`，未发现生产缺陷，未修改生产代码。

## 未决项

- 默认 builder factory 的插件覆盖路径仍缺少仓内自定义实现样本，暂不推断更多行为。
- `StorageModels` 其他 backend-specific annotation 组合。
- `SW-5A` 是否继续补充抽象层边界，或转入 `SW-5B JDBC`。
