# SW-4C1 GraphQL query boundary — Outline

> 模块: `oap-server/server-query-plugin/query-graphql-plugin`
> 日期: 2026-08-18

## 1. 域定位
`SW-4C1` 是 SkyWalking OAP 的统一 GraphQL 查询入口插件：
- 负责 schema 组装
- 负责 `/graphql` HTTP endpoint
- 负责 resolver 到 core query services 的映射
- 负责 GraphQL query complexity 限制与 telemetry

它本身大多不是 query 业务实现层，而是 query 入口/装配/适配层。

## 2. 主链
`GraphQLQueryProvider.prepare()`
→ 注册 `.graphqls` 文件与 resolver
→ `schemaBuilder.build().makeExecutableSchema()`
→ `GraphQLQueryProvider.start()`
→ `HTTPHandlerRegister.addHandler(new GraphQLQueryHandler(...))`
→ `/graphql`
→ GraphQL resolver
→ `moduleManager.find(...).provider().getService(...)`

## 3. 模块内部层次
### 3.1 Provider 层
`GraphQLQueryProvider`：
- config 初始化
- schema 文件注册
- resolver 实例装配
- conditional feature（如 on-demand pod log）挂载
- handler 注册

### 3.2 Handler 层
`GraphQLQueryHandler`：
- 暴露 `/graphql` POST
- 使用 `MaxQueryComplexityInstrumentation`
- 记录 histogram / error counter

### 3.3 Resolver 层
- **Root resolver**：`Query`、`Mutation`，基本只暴露 version
- **Façade resolver**：如 `MetadataQueryV2`、`HierarchyQuery`、`TraceQuery*`、`TopologyQuery`，以 lazy-get service + `queryAsync` 为主
- **跨域逻辑 resolver**：如 `MetricsExpressionQuery`（直接跑 MQE），`LogTestQuery`（直接跑 LAL debug tool）

## 4. 代表性 resolver 结论
### 4.1 `MetadataQueryV2`
- 是典型 façade resolver
- 主要职责是参数适配与异步包装
- 例如 `findService` 会在 resolver 层构造 `ServiceID.buildId(serviceName, true)`

### 4.2 `MetricsExpressionQuery`
- 不只是 façade
- 在 resolver 层直接进行：lexer → parser → visitor → `ExpressionResult`
- 因此它与 `SW-4B MQE runtime` 强耦合，是 GraphQL 插件中最值得单独交叉引用的 resolver 之一

### 4.3 `LogTestQuery`
- 受配置开关保护
- 直接进入 `log-analyzer` 的 DSL dry-run 调试链
- 本质是调试工具型 resolver，而不是普通业务查询 resolver

## 5. Shared seam 结论
### 5.1 `AsyncQueryUtils`
- 所有 async GraphQL query 共享同一个 `ForkJoinPool`
- 统一把异常包装为 `RuntimeException` 后放进 `CompletableFuture`
- 这决定了 GraphQL 异步 resolver 的错误表现形式

### 5.2 `GraphQLQueryHandler`
- complexity 限制在 handler 侧统一施加
- telemetry 只在 `serve(...)` 抛异常时记 error count
- 成功请求只计 latency

### 5.3 `GraphQLQueryProvider`
- 是 schema completeness 的唯一骨架
- 一旦 `.graphqls` 文件或 resolver 漏注册，问题会体现在模块装配层，而不是某个 resolver 内部算法

## 6. 当前测试现实
原有官方测试只有：
- `LogTestQueryTest`

本轮新增：
- `AsyncQueryUtilsTest`
- `MetadataQueryV2Test`

新增测试覆盖：
- async query 成功与异常包装语义
- metadata resolver 的 service ID 构造委托
- TTL 查询委托行为

## 7. 当前结论
`SW-4C1` 当前已达到可交接收敛状态：
- 入口装配 / handler / resolver 三层边界清晰
- shared seam 已补测试
- 代表性 resolver 的模式已确认
- 暂未发现可复现源码缺陷

## 8. 剩余风险
- `MetricsExpressionQuery` 作为 GraphQL→MQE 直连点，后续若继续深挖可单独交叉审计
- schema completeness 仍主要依赖 provider 显式注册，未来改动时最容易出装配型回归
- 其他大量 façade resolver 测试覆盖仍稀疏，但从结构上看重复性很高
