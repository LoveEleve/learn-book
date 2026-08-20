# HANDOFF-SKYWALKING-STAGE2

> 项目: `spring/skywalking`
> 仓库: `/data/workspace/source-code/code/spring/skywalking`
> 日期: 2026-08-18
> 状态: **阶段性交接文档（非全仓最终收官）**
> 方法论: `09 怀疑审计 / Pass0→Pass1→Pass2→Pass3 / harness / 多轮 review 一次性收敛`

---

## 0. 这份文档是什么，不是什么

这份文档是 Stage 2 的**阶段性大域源码分析交接**，不是 SkyWalking 全仓最终总交付。

### Stage 2 在 Stage 1 基础上新增并完成的域：

| 域 | 名称 | 状态 |
|---|---|---|
| SW-3D | Event Analyzer | 从 Pass 0 收敛至可交接 |
| SW-3E | Hierarchy compiler | 完整收敛，并修复真实缺陷 |
| SW-3F | GenAI analyzer | 完整收敛 |
| SW-4 | Query / MQE / OAL boundary | 域内再拆分，部分子域完成 |
| SW-4A | OAL compiler/runtime | 完整收敛 |
| SW-4B | MQE grammar/runtime | 完整收敛，并修复真实缺陷 |
| SW-4C | Query interface / plugin boundary | 域内再拆分，4C1/4C2/4C3A/4C3B 已完成，4C3C 已收敛，4C4 已启动并进入 Pass2 |
| SW-4C1 | GraphQL query boundary | 完整收敛 |
| SW-4C2 | Zipkin query compatibility | 完整收敛，并修复真实缺陷 |
| SW-4C3A | PromQL compatibility | 完整收敛，并修复真实缺陷 |
| SW-4C3B | TraceQL compatibility | 完整收敛，并修复真实缺陷 |
| SW-4C3C | LogQL compatibility | 核心修复完成，定向与完整 reactor 回归通过；query_range 仍有协议/测试风险 |

### 尚未完成的域：

- `SW-4C4 Status/debug/ops query`（Pass0/Pass2 已启动；已修复 orphan parent trace 缺陷，tag/status handler 风险仍在）
- `SW-5 Storage / Persistence`（SW-5A 已完成核心抽象审计；SW-5B JDBC 已启动并修复 batch 异常吞没缺陷，其他 JDBC seam/backend 子域未完成）
- `SW-6 Cluster / Configuration`
- `SW-7 Transport / Export / Observability`
- `SW-8 UI / Distribution / Integration boundary`
- `HANDOFF-SKYWALKING.md` 全仓总交付

---

## 1. Stage 2 已修复的真实缺陷汇总

截至 LogQL 收敛阶段共发现并修复 **6 个**真实生产代码缺陷；本次 SW-4C4 又确认并修复 2 个真实生产代码缺陷；SW-5A 再确认并修复 3 个真实生产代码缺陷；SW-5B 再确认并修复 8 个真实生产代码缺陷，累计 **19 个**。LogQL、status-query-plugin、server-core、JDBC 与 library-client 的定向/完整回归均已通过。

| # | 模块 | 文件 | 缺陷 | 修复 |
|---|---|---|---|---|
| 1 | `hierarchy` | `HierarchyRuleScriptParser.java:265` | grammar 声明了 `>=`/`<=`，但 `ConditionVisitor` 缺少 `visitCondGte`/`visitCondLte`，AST condition 为 null，codegen 生成非法 `if ()` | 补齐两个 visitor 方法 |
| 2 | `mqe-rt` | `LROp.java:158`/`LROp.java:182` | `many2OneBinaryOp`/`one2ManyBinaryOp` 只检查 many-side `emptyValue`，single-side 为 empty 时仍继续计算 | 改为 single-side 或 many-side 任一 empty 即传播 empty |
| 3 | `zipkin-query-plugin` | `ZipkinQueryHandler.java:151` | `getTraceById` 使用 `isEmpty` 判断 traceId，空白字符串会进入 DAO 链路 | 改为 `isBlank` |
| 4 | `promql-plugin` | `PromQLApiHandler.java:609` | `formatTimestamp2Millis` 对所有数字统一乘 1000，毫秒 Unix timestamp 被错误放大 | 按数值位数区分秒/毫秒 |
| 5 | `traceql-plugin` | `TraceQLQueryParser.java:40` | `parse(...)` 未安装 lexer/parser error listener，非法查询仍返回成功结果 | 添加共享 error listener 并在有错误时抛异常 |
| 6 | `logql-plugin` | `LogQLApiHandler.java:94`/`LogQLApiHandler.java:110` | `labels`/`labelValues` 对可选 `start/end` 直接拆箱，null 会 NPE | 提供默认时间（当前时间回看 24h） |
| 7 | `status-query-plugin` | `DebuggingHTTPHandler.java:521` | debugging trace 中 child 的 parent 不在当前 span 集合时直接 NPE，整个 debug response 失败 | parent 存在时才建立 child 关系，orphan span 保留 |
| 8 | `status-query-plugin` | `DebuggingHTTPHandler.java:219-227`/`489-498` | comma-separated tag 使用无限制 `split("=")`，value 中的 `=` 被截断；非法 tag 可能数组越界 | 抽取 `parseTags`，按第一个 `=` 分割，非法格式抛 `IllegalArgumentException` |
| 9 | `server-core` | `PersistenceTimer.java:123-143` | prepare 阶段 `buildBatchRequests()` 抛异常时跳过 `worker.endOfRound()`，导致 worker round 生命周期未收尾 | 在 prepare `finally` 中无条件调用 `endOfRound()`，并确保 prepare timer 关闭 |
| 10 | `server-core` | `StorageModels.java:127-142` | 单个 `@BanyanDB.Trace.IndexRule` 不会进入 trace index rule 解析分支，非法定义被静默接受 | 将 `@BanyanDB.Trace.IndexRule.class` 纳入解析条件，与 `List` 形式统一校验 |
| 11 | `server-core` | `PersistenceTimer.java:94-107` | `start()` 在 `isStarted=true` 时仍会重建 `prepareExecutorService`，导致重复启动替换并泄漏旧线程池 | 将 prepare executor 初始化移入 `if (!isStarted)` 分支，使 start 对已启动实例保持幂等 |
| 12 | `storage-jdbc-hikaricp-plugin` | `JDBCBatchDAO.java:84-100` | SQL batch 执行异常只记录日志并返回成功 future，上游误判整批成功 | 独立 SQL group 继续执行，同时聚合异常并返回 failed future |
| 13 | `storage-jdbc-hikaricp-plugin` | `BatchSQLExecutor.java:44-48` | `maxBatchSqlSize` 为 0/负数时无明确校验，0 会绕过分批，负数产生间接异常 | 入口校验 `maxBatchSqlSize > 0`，非法值抛 `IllegalArgumentException` |
| 14 | `storage-jdbc-hikaricp-plugin` | `BatchSQLExecutor.java:87-97` | `executeBatch()` 返回长度与请求数不一致时可能越界或静默漏回调 | 显式校验结果长度，不匹配时抛出带上下文的 `SQLException` |
| 15 | `library-client` | `JDBCClient.java:69-82` | `setAutoCommit()` 失败时已取得连接未关闭，可能泄漏连接池资源 | 失败路径关闭连接，并合并 close 异常为 suppressed exception |
| 16 | `library-client` | `JDBCClient.java:58-63` | 未 connect 或重复 shutdown 时 `dataSource.close()` NPE | shutdown 对 null 安全，关闭后清空 dataSource |
| 17 | `storage-jdbc-hikaricp-plugin` | `JDBCBatchDAO.java:65-104` / `SQLExecutor.java:63-80` | 主表 SQL 与 additional SQL 被拆成不同 batch group 和不同 auto-commit 连接执行，可能出现半写入 | 按原始 request 边界执行，并在 `SQLExecutor.invokeInTransaction(...)` 中使用单连接事务提交/回滚 |
| 18 | `library-client` | `JDBCClient.java:52-60` | 重复 `connect()` 直接覆盖 `dataSource`，旧 `HikariDataSource` 未关闭，导致连接池泄漏 | 通过 `newDataSource()` 创建 replacement 后关闭 previous，并保留可测试工厂方法 |
| 19 | `storage-jdbc-hikaricp-plugin` | `TableMetaInfo.java:33-37` | 注册 JDBC 模型时直接裁剪传入 `Model` 的列集合，污染共享对象 | 向注册表保存裁剪后的 `Model` 副本，避免修改原始 `Model` |

---

## 2. Stage 2 新增测试汇总

本轮新增 **12 个测试文件**（其中 event-analyzer 目录下列出 2 个独立测试类）：

| 文件 | 模块 | 覆盖 |
|---|---|---|
| `EventAnalyzerServiceImplTest.java` | `event-analyzer` | 双无效时间回填、单边时间保留、factory 可见性 |
| `EventRecordAnalyzerListenerTest.java` | `event-analyzer` | source/naming/type/layer/timestamp/timeBucket 映射 |
| `HierarchyRuleScriptParserAdvancedTest.java` | `hierarchy` | `>=`、`<=`、`&&` visitor |
| `HierarchyRuleClassGeneratorAdvancedTest.java` | `hierarchy` | `>=` codegen、`&&` codegen |
| `LROpEdgeCaseTest.java` | `mqe-rt` | empty scalar + many 的 binary/compare 传播 |
| `GenAIMeterAnalyzerAdvancedTest.java` | `gen-ai-analyzer` | Zipkin 分支、legacy `gen_ai.system`、source 转换、NamingControl |
| `ZipkinQueryHandlerTest.java` | `zipkin-query-plugin` | config JSON、blank traceId、empty traceIds、duplicate traceIds |
| `PromQLApiHandlerTimestampTest.java` | `promql-plugin` | 秒级/毫秒级 Unix timestamp |
| `TraceQLQueryParserErrorTest.java` | `traceql-plugin` | malformed query error result |
| `AsyncQueryUtilsTest.java` | `query-graphql-plugin` | async 成功/异常包装 |
| `MetadataQueryV2Test.java` | `query-graphql-plugin` | service ID 构造、TTL 委托 |
| `LogQLApiHandlerTest.java` | `logql-plugin` | labels/label-values 默认时间、query_range 委托与 parser error |
| `DebuggingHTTPHandlerTest.java` | `status-query-plugin` | orphan parent trace、tag value 含等号 |
| `StatusQueryExceptionHandlerTest.java` | `status-query-plugin` | 400/500 异常映射 |
| `ServerStatusServiceTest.java` | `server-core` | 配置脱敏与 nested Properties |
| `PersistenceTimerEdgeCaseTest.java` | `server-core` | prepare 异常时 worker round 收尾、error counter 与 batch flush round 收尾 |
| `StorageModelsAdvancedTest.java` | `server-core` | listener 回放、blank timestamp、AdditionalEntity 非法组合、trace index rule 非法定义 |
| `PersistenceTimerStartSemanticsTest.java` | `server-core` | 已启动状态下重复 `start()` 不应替换 prepare executor |
| `StorageBuilderFactoryContractTest.java` | `server-core` | 默认 builder 模板父类、模板路径与 builder 选择契约 |
| `JDBCBatchDAOFailureTest.java` | `storage-jdbc-hikaricp-plugin` | JDBC batch 异常传播与 failed future |
| `BatchSQLExecutorBoundaryTest.java` | `storage-jdbc-hikaricp-plugin` | max batch size 非正数配置 |
| `BatchSQLExecutorResultBoundaryTest.java` | `storage-jdbc-hikaricp-plugin` | executeBatch 结果长度与请求长度不一致 |
| `JDBCBatchDAOTransactionTest.java` | `storage-jdbc-hikaricp-plugin` | 主 SQL/additional SQL 单连接事务回滚 |
| `JDBCClientLifecycleTest.java` | `library-client` | auto-commit 失败回收、shutdown 幂等、重复 connect 关闭旧池 |
| `TableMetaInfoMutationTest.java` | `storage-jdbc-hikaricp-plugin` | JDBC 注册表不应污染原始 Model 列集合 |

---

## 3. Stage 2 修改的生产源码文件

| 文件 | 修改点 |
|---|---|
| `HierarchyRuleScriptParser.java` | 补 `visitCondGte`/`visitCondLte` |
| `LROp.java` | 修复 empty scalar 传播 |
| `ZipkinQueryHandler.java` | `isEmpty` -> `isBlank` |
| `PromQLApiHandler.java` | 秒/毫秒时间戳区分 |
| `TraceQLQueryParser.java` | 添加 lexer/parser error listener |
| `LogQLApiHandler.java` | 可选时间参数默认值 |
| `DebuggingHTTPHandler.java` | orphan parent 时跳过无效 child 关系 |

另有 2 个 Stage 1 遗留修改：
- `TraceSegmentSampler.java`：`Math.abs(Integer.MIN_VALUE)` 修复
- `TraceSamplingPolicyWatcherTest.java`：对应测试

---

## 4. SW-4C3C LogQL 当前精确状态

### 已完成
- Pass 0 发现文档：`outlines/SW-4C3C-logql/pass0-discovery.md`
- parser/visitor 边界审查
- `LogQLApiHandler` 的 `labels`/`labelValues` NPE 缺陷已修复
- parser error 已确认正确（`ParseErrorListener` 抛 `ParseCancellationException`）
- visitor 的 blank/star 过滤语义已确认
- 纳秒时间参数转换已确认

### 当前剩余风险
- `query_range` 的 `start/end/direction` 缺省行为尚未从 Loki/SkyWalking 协议和 handler 测试双重确认；当前实现直接使用这些参数。
- `query_range` 已有 handler mock 测试，覆盖 parser error、方向、纳秒时间转换和 tags/keywords 委托；errorReason、空结果、stream 分组、FORWARD 方向和 limit null 仍是扩展风险。
- 这些是后续兼容性审计风险，不是当前已确认的生产缺陷。

### 当前回归
- `LogQLApiHandlerTest`: 4/4 PASS
- `LogQLExprVisitorTest`: 4/4 PASS
- `./mvnw -pl oap-server/server-query-plugin/logql-plugin -am test`: `BUILD SUCCESS`

---

## 5. 推荐后续主线

### 短期（建议下一个 AI 立即完成）

1. **SW-4C4 Status/debug/ops query 继续收敛**
   - 阅读 `outlines/SW-4C4-status-debug-ops/` 四份文档
   - 优先验证 tag parser、TTL/cluster/alarm handler 委托、ThreadLocal 异常清理
   - 评估重复 spanId 与缺省 service 参数协议

2. **SW-4 总文档收口**
   - 完成 SW-4C4 后写 `HANDOFF-SW-4.md`
   - 记录 SW-4A/4B/4C 三条主线的完整边界与交叉引用

### 中期（SW-5 之前建议完成）

3. **SW-4 总文档收口**
   - 在 `SW-4C4` 完成后写 `HANDOFF-SW-4.md`
   - 记录 SW-4A/4B/4C 三条主线的完整边界与交叉引用

### 长期

4. `SW-5 Storage / Persistence`
5. `SW-6 Cluster / Configuration`
6. `SW-7 Transport / Export / Observability`
7. `SW-8 UI / Distribution / Integration boundary`
8. `HANDOFF-SKYWALKING.md` 全仓总交付

---

## 6. 方法论经验（Stage 2 最容易重踩的坑）

### 6.1 大域必须先拆
SW-4 原名"Query / MQE / OAL boundary"，如果不拆成 4A/4B/4C，会被三个完全不同的机制链吞并。4C 又拆成 4C1/4C2/4C3/4C4。每层拆域都是正确的。

### 6.2 先怀疑 grammar 声明与 visitor 实现是否一致
SW-3E hierarchy 和 SW-4C3B TraceQL 都出现了"grammar 声明支持但 visitor/lexer 未实现"的问题。这是最容易被现有 happy-path 测试掩盖的缺陷类型。

### 6.3 DSL 插件必须同时审查 grammar、parser、visitor 和 API handler
PromQL、Zipkin、LogQL 的缺陷主要位于 handler 参数解析/默认值/错误响应；TraceQL 的缺陷位于 parser 错误监听。不能把 DSL 风险只归因于 handler。

### 6.4 空值传播是 MQE runtime 的关键风险
MQE 的 `LROp` 在 many↔one 路径遗漏 single-side empty，这种缺陷在正常 happy-path 测试中很难暴露，必须构造特定的 empty value 组合。

### 6.5 Mockito 测试风格要严格
SW-4C1 GraphQL 和 SW-4C3C LogQL 都出现了 Mockito matcher 使用错误（`UnnecessaryStubbingException`、`InvalidUseOfMatchersException`）。这不是代码缺陷，但会阻塞 CI。接手法时优先检查。

---

## 7. 关键构建命令

```bash
# 全仓编译检查
./mvnw -pl oap-server/server-core -am compile

# 单模块回归（示例）
./mvnw -pl oap-server/server-query-plugin/logql-plugin -am test

# 定向测试（示例）
./mvnw -pl oap-server/server-query-plugin/logql-plugin -am -Dtest=LogQLExprVisitorTest -Dsurefire.failIfNoSpecifiedTests=false test

# 从这里继续的推荐起点
./mvnw -pl oap-server/server-query-plugin/logql-plugin -am -Dtest=LogQLApiHandlerTest,LogQLExprVisitorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

---

## 8. 各子域 Pass 文档路径

所有子域的 Pass 文档都在以下绝对目录：
```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/skywalking/outlines/
```
每个子域下的文件以实际存在情况为准；不能假设每个子域都已经有 Pass 0/1/2/3 全部四类文件。当前 `SW-4C3C-logql` 已包含 `pass0-discovery.md`、`pass2-questions.md`、`outline.md`、`review-notes.md`，但本域仍保留 query_range 协议和 handler 测试风险。

具体子域：
- `SW-3D-event-analyzer/`
- `SW-3E-hierarchy/`
- `SW-3F-genai/`
- `SW-4-query-mqe-oal/`
- `SW-4A-oal-runtime/`
- `SW-4B-mqe-runtime/`
- `SW-4C-query-plugin/`
- `SW-4C1-graphql-query/`
- `SW-4C2-zipkin-query/`
- `SW-4C3-query-dsl/`
- `SW-4C3A-promql/`
- `SW-4C3B-traceql/`
- `SW-4C3C-logql/`
- `SW-4C4-status-debug-ops/`

已完成收敛的子域通常包含：
- `pass0-discovery.md` — 域职责、边界、首轮质疑
- `pass2-questions.md` — 问题收敛
- `outline.md` — 完整域大纲
- `review-notes.md` — 多轮 review 结论

注意：`SW-4C3C-logql/` 的三份后续文档已经补齐；下一个 AI 应先阅读这些文档，再决定是否继续专项补充 query_range handler 测试。

---

## 9. 当前 git diff 统计

以下是 `git diff --stat` 的已跟踪文件统计，不包含未跟踪的新测试文件和文档：

```
9 files changed, 90 insertions(+), 25 deletions(-)
```

当前工作树不是干净工作树，且保留了用户原有修改。下一个 AI 不得使用 `git reset --hard`、`git checkout --` 或清理未跟踪文件。提交前必须重新检查 `git status`，只提交明确属于本轮的文件。

修改的生产源码文件：
- `TraceSegmentSampler.java`
- `TraceSamplingPolicyWatcherTest.java`
- `event-analyzer/pom.xml`
- `HierarchyRuleScriptParser.java`
- `LROp.java`
- `LogQLApiHandler.java`
- `PromQLApiHandler.java`
- `TraceQLQueryParser.java`
- `ZipkinQueryHandler.java`

新增的测试文件：
- `event-analyzer/src/test/`（2 个文件）
- `HierarchyRuleClassGeneratorAdvancedTest.java`
- `HierarchyRuleScriptParserAdvancedTest.java`
- `LROpEdgeCaseTest.java`
- `GenAIMeterAnalyzerAdvancedTest.java`
- `ZipkinQueryHandlerTest.java`
- `PromQLApiHandlerTimestampTest.java`
- `TraceQLQueryParserErrorTest.java`
- `AsyncQueryUtilsTest.java`
- `MetadataQueryV2Test.java`
- `LogQLApiHandlerTest.java`

---

## 10. 本次深度 review 结论

已核对交接文档、LogQL handler、LogQL 测试、`TagAutoCompleteQueryService` 签名和实际 Maven 输出，确认以下结论：

- 文档原来的“5 个缺陷”是计数错误，已修正为 6 个。
- 文档原来的“7 个测试文件”是计数错误，已修正为 12 个测试文件。
- LogQL 历史失败点曾位于 `LogQLApiHandlerTest.java:55` 的 stub，根因是 Mockito 混用了 raw `TagType.LOG` 和 matcher；现已改用 `eq(...)`，测试通过。
- 当前 LogQL 定向结果：`LogQLApiHandlerTest` 4/4、`LogQLExprVisitorTest` 4/4，BUILD SUCCESS。
- 当前 LogQL 完整 reactor 结果：`server-core` 216/216、`LogQLApiHandlerTest` 4/4、`LogQLExprVisitorTest` 4/4，LogQL 8/8，BUILD SUCCESS。
- `SW-4C3C-logql` 的 Pass2、outline、review-notes 已补齐；query_range 协议与 handler 测试风险仍明确记录。
- “全部已完成”只适用于已列明的已收敛子域；SW-4C4 已启动但尚未完全收敛，全仓仍未完成。
