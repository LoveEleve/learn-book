# SW-4C1 GraphQL query boundary — Pass 0 发现

> 模块: `oap-server/server-query-plugin/query-graphql-plugin`
> 日期: 2026-08-18

## 1. 域职责
`SW-4C1` 是 SkyWalking OAP 的 GraphQL 查询入口插件：
- 组装 GraphQL schema
- 注册 root query / mutation / 各业务 resolver
- 暴露 `/graphql` HTTP POST handler
- 对 GraphQL 查询施加复杂度限制与 telemetry 计数

它不是底层 query 算法引擎；实际业务查询能力大多仍在 `server-core` 的 query service 或其他模块 service 中。

## 2. 主链
`GraphQLQueryProvider.prepare()`
→ `SchemaParserBuilder.file(...).resolvers(...)` 装配 schema
→ `GraphQLQueryProvider.start()`
→ `HTTPHandlerRegister.addHandler(new GraphQLQueryHandler(...))`
→ `/graphql`
→ GraphQL resolver
→ `moduleManager.find(...).provider().getService(...)`

## 3. 当前结构特征
### 3.1 provider 是真正骨架
`GraphQLQueryProvider` 当前承担：
- config 初始化
- schema 文件注册
- resolver 实例创建
- complexity instrumentation 配置
- handler 注册

这意味着该模块的核心风险首先在装配边界，而不在某个 resolver 算法。

### 3.2 resolver 数量很多，但大多是薄封装
当前目录已看到大量 resolver，例如：
- `MetadataQuery/MetadataQueryV2`
- `TraceQuery/TraceQueryV2`
- `MetricsExpressionQuery`
- `TopologyQuery`
- `EventQuery`
- `HierarchyQuery`
- `Profile/Mutation`、`Pprof/Mutation`
- `ContinuousProfiling*`
- `EBPFProcessProfiling*`
- `LogQuery` / `LogTestQuery`
- `HealthQuery`

这些类大多通过 `moduleManager.find(...).provider().getService(...)` 延迟获取底层 service。

### 3.3 顶层 root resolver 很薄
- `Query` 只是提供 version 字段
- `Mutation` 同样只是 version 字段

真实查询语义主要在子 resolver 中。

### 3.4 handler 风险点
`GraphQLQueryHandler` 当前负责：
- `/graphql` POST endpoint
- `MaxQueryComplexityInstrumentation`
- histogram / error counter

因此 GraphQL 入口侧的主要边界是：
- schema 是否完整装配
- complexity 限制是否生效
- telemetry 是否只在异常时记错

## 4. 当前测试现实
完整模块回归通过，但当前明确看见的官方测试只有：
- `LogTestQueryTest`

这说明：
- 模块整体可编译、可启动
- 但自动测试密度相对很低
- 很多 resolver 很可能只靠集成路径间接覆盖

## 5. 首轮质疑点
- Q1: provider 的 schema 装配是否存在明显漏注册 / 版本分叉风险
- Q2: `GraphQLQueryHandler` 的 complexity instrumentation 与 telemetry 计数边界
- Q3: resolver 普遍 lazy-get service 的模式是否一致，是否有某些 resolver 跨模块取错 service
- Q4: `LogTestQuery` 这类 debug/tooling resolver 是否越界进入其他 analyzer 内部实现
- Q5: GraphQL 插件内部是否需要继续拆为“入口装配”和“resolver 族群”两个层次理解

## 6. 当前方法论结论
`SW-4C1` 当前最合理的下一步，不是先扫全部 resolver，而是：
1. 先把 `GraphQLQueryProvider + GraphQLQueryHandler` 骨架摸清
2. 再选 2~3 个代表性 resolver（如 `MetricsExpressionQuery`、`MetadataQueryV2`、`LogTestQuery`）做深审
3. 以 representative sampling 的方式判断模块级边界，而不是被 40+ resolver 机械淹没
