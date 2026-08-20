# SW-4C3A PromQL compatibility — Pass 0 发现

> 模块: `server-query-plugin/promql-plugin`
> 日期: 2026-08-18

## 1. 域职责
`SW-4C3A` 提供 Prometheus / PromQL 兼容接口：
- PromQL grammar + parser visitor
- Prometheus HTTP API handler
- 将 PromQL selector / range / aggregation 等语义映射到 OAP metrics/query service
- 组装 Prometheus 风格 response model

## 2. 主链
`PromQLProvider.start()`
→ `PromQLApiHandler`
→ HTTP API 参数解析（time/range/match/limit 等）
→ `PromQLLexer/Parser`
→ `PromQLExprQueryVisitor` / `PromQLMatchVisitor`
→ `MetricsQueryService` / `AggregationQueryService` / `RecordQueryService`
→ PromQL response model

## 3. 当前结构特征
### 3.1 Handler 比 parser 更关键
`PromQLApiHandler` 自身很大，包含：
- API endpoint 族
- RFC3339 时间解析
- limit/match/start/end 处理
- metrics / labels / label values / query / query_range 等逻辑入口
- parser/visitor 调用后的错误结果映射

因此 PromQL 不是单纯“grammar plugin”，而是 **DSL + HTTP compatibility API** 复合模块。

### 3.2 Visitor 负责语义错误转码
`PromQLExprQueryVisitor` 在多个场景下会返回：
- `ErrorType.BAD_DATA`
- `ErrorType.INTERNAL`

并把非法 label、缺失 layer、range/instant query 用错等情况转成错误结果，而不是直接抛异常。

### 3.3 与 GraphQL 复用
`PromQLApiHandler` 直接依赖 `MetadataQueryV2`，说明 Query-side DSL plugin 与 GraphQL query 层存在复用，而不是完全独立。

## 4. 当前测试现实
已见测试：
- `PromQLApiHandlerTest`：RFC3339 时间解析
- `PromQLExprQueryVisitorTest`：表达式/聚合/metrics 结果
- `PromQLMatchVisitorTest`

完整模块回归：**21/21 PASS**。

## 5. 首轮质疑点
- Q1: handler 对 RFC3339 / unix time / invalid time 输入的兼容边界
- Q2: range query 与 instant query 用错时的错误响应是否稳定
- Q3: `limit` 参数与 selector label 中的 `top_n` 是否有优先级/覆盖规则
- Q4: visitor 在 BAD_DATA 与 INTERNAL 间的划分是否一致
- Q5: handler 是否有未测的参数级错误路径

## 6. 当前方法论结论
PromQL 的下一步深审重点应先落在：
1. handler 参数与错误响应边界
2. visitor 的表达式错误分类
3. 再考虑 parser/grammar 细节
