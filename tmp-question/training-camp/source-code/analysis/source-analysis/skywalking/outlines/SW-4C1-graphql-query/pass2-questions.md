# SW-4C1 GraphQL query boundary — Pass 2 问题收敛

> 模块: `oap-server/server-query-plugin/query-graphql-plugin`
> 日期: 2026-08-18

## Q1: GraphQL 插件真正的核心在哪里
核心不在单个 resolver 算法，而在 3 个共享 seam：
1. `GraphQLQueryProvider` 的 schema 装配
2. `GraphQLQueryHandler` 的 `/graphql` 入口、复杂度限制与 telemetry
3. `AsyncQueryUtils.queryAsync(...)` 的统一异步执行与异常包装

这三者定义了模块级边界；大量 resolver 只是对底层 query service 的薄封装。

## Q2: GraphQL root query/mutation 是否承载业务逻辑
没有。`Query` 与 `Mutation` 当前只暴露 version 字段。

结论：真正的业务查询都在子 resolver 中，root resolver 只是 schema 根节点。

## Q3: 代表性 resolver 的模式是否一致
已抽样确认：
- `MetadataQueryV2`：lazy 获取 `MetadataQueryService`/`TTLStatusQuery`，大部分方法直接 `queryAsync(() -> service.method(...))`
- `MetricsExpressionQuery`：在 resolver 层自带 grammar parse + visitor 执行，是 GraphQL 插件中少数真正内含逻辑的 resolver
- `LogTestQuery`：受 `enableLogTestTool` 开关控制，会直接进入 `log-analyzer` 的 DSL dry-run 调试链

结论：
- 大多数 resolver 是 service façade
- 少数 resolver（如 MetricsExpression/LogTest）跨到下层 DSL/analyzer 体系，需要单独看边界

## Q4: `AsyncQueryUtils` 如何处理异常
`queryAsync(...)` 用共享 `ForkJoinPool` 包装 `Callable`：
- 正常返回 -> `CompletableFuture` 正常完成
- 任意异常 -> 先记录 error log，再包成 `RuntimeException` 抛出

结论：
- GraphQL 异步 resolver 的异常会统一被包成 `CompletionException(RuntimeException(cause))`
- 这是当前统一语义，而不是单个 resolver 各自处理

## Q5: `MetadataQueryV2` 的边界是否清晰
已测试确认：
- `findService(serviceName)` 会在 resolver 层构造 `ServiceID.buildId(serviceName, true)` 再下沉到底层 service
- `getRecordsTTL/getMetricsTTL` 只是 `TTLStatusQuery` 的薄委托

结论：它是典型 façade resolver，没有隐藏复杂转换逻辑。

## Q6: `LogTestQuery` 是否越界
是一个有意越界的 debug/tooling resolver：
- 直接拿 `LogAnalyzerModuleProvider`
- 直接构造 `DSL.of(...)`
- dry-run 执行 LAL 并把 log/metrics 结果回填到 GraphQL response

结论：这是设计上的调试工具边界，不应误归类为普通 metadata/log 查询 resolver。

## Q7: 当前是否发现真实源码缺陷
截至本轮，没有发现可复现的生产代码缺陷。

本轮补测主要验证了：
- shared async seam
- metadata resolver 委托行为

都与当前实现一致。
