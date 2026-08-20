# SW-5A storage core abstraction + persistence timer — Outline

## 1. 范围

- `StorageModule`
- `StorageDAO`
- `StorageBuilderFactory`
- `StorageModels`
- `PersistenceTimer`
- `IBatchDAO` / `StorageData` / `Model` 共同契约

## 2. 已确认决策

### 2.1 PersistenceTimer round 生命周期

`PersistenceTimer` 必须保证每个已调度 worker 在每轮 prepare 结束时收到一次 `endOfRound()` 通知。该通知不能只依赖 `buildBatchRequests()` 成功返回，因为 worker 可能需要在 prepare 失败后清理或推进内部状态。

实现位置：`oap-server/server-core/src/main/java/org/apache/skywalking/oap/server/core/storage/PersistenceTimer.java`

行为：

- `buildBatchRequests()` 正常返回：调用 `endOfRound()`，空请求跳过 flush。
- `buildBatchRequests()` 返回空列表：调用 `endOfRound()`，不调用 `flush()`。
- `buildBatchRequests()` 抛异常：调用 `endOfRound()`，该 worker future 异常完成，统一记录 error，并执行 `batchDAO.endOfFlush()`。
- `endOfRound()` 完成后关闭 prepare latency timer。

## 3. 回归覆盖

`PersistenceTimerEdgeCaseTest` 覆盖 prepare 异常路径的：

- worker round 收尾
- error counter
- batch DAO flush round 收尾
- 测试线程与单例 worker 列表清理

## 4. 已确认问题

### 4.1 StorageModels trace index rule 校验缺口

`StorageModels.add(...)` 原先遗漏了仅声明单个 `@BanyanDB.Trace.IndexRule` 的分支入口，导致非法定义不会进入 `createTraceIndexRule(...)` 校验，静默通过建模。

实现位置：`oap-server/server-core/src/main/java/org/apache/skywalking/oap/server/core/storage/model/StorageModels.java`

修复：将 `@BanyanDB.Trace.IndexRule.class` 纳入 trace index rule 解析条件。

## 5. 已确认问题

### 5.1 PersistenceTimer start 非幂等

`PersistenceTimer.start(...)` 原先在检查 `isStarted` 之前就重建 `prepareExecutorService`。重复调用 `start()` 时虽然不会重复注册 scheduled executor，但会替换并泄漏旧 prepare 线程池。

实现位置：`oap-server/server-core/src/main/java/org/apache/skywalking/oap/server/core/storage/PersistenceTimer.java`

修复：把 prepare executor 初始化移动到 `if (!isStarted)` 分支内，使 `start()` 对已启动实例保持幂等。

## 6. 待审计问题

- 默认 builder factory 与插件覆盖 builder 的选择契约。
- `StorageModels` 其他 backend-specific annotation 组合是否仍有遗漏分支。
- `PersistenceTimer` 并发首次启动是否还存在竞争窗口。

## 7. 当前状态

PersistenceTimer 异常 round 收尾、StorageModels trace index rule 漏校验、PersistenceTimer start 非幂等这 3 个 SW-5A 抽象层缺陷都已修复。定向回归全部通过；`server-core` 全量回归多轮通过过，但本轮最后一次完整执行因命令超时被截断，未看到失败栈。SW-5A 仍未整体收敛，下一步优先审计首次并发启动竞争窗口或默认 builder factory 契约。
