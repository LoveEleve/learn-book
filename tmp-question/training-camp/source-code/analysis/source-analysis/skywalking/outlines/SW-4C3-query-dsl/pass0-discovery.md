# SW-4C3 Query-side DSL plugins — Pass 0 发现

> 模块: `server-query-plugin/promql-plugin`、`traceql-plugin`、`logql-plugin`
> 日期: 2026-08-18
> 状态: **SW-4C3 内部拆分完成，三条 DSL 已完成首轮边界审计**

## 1. 域内再拆分
`SW-4C3` 不是单一 DSL，应继续按真实机制拆为：
- `SW-4C3A PromQL compatibility`
- `SW-4C3B TraceQL compatibility`
- `SW-4C3C LogQL compatibility`

三者均有 grammar/parser/handler，但输入协议、结果模型、底层查询转换完全不同。

## 2. 当前结构
### 2.1 PromQL
- ANTLR grammar + expression/match visitor
- `PromQLApiHandler` 提供 Prometheus API 兼容 endpoint
- response model 独立维护
- 依赖 `query-graphql-plugin` 的 `MetadataQueryV2`

### 2.2 TraceQL
- ANTLR grammar + TraceQL parser/visitor
- 同时兼容 Zipkin/OTLP/SkyWalking trace converter
- 自带 proto 生成
- 当前 parser 测试 8 个

### 2.3 LogQL
- ANTLR grammar + `LogQLExprVisitor`
- handler/provider + LogQL response model
- 当前 parser/visitor 测试 4 个

## 3. 首轮构建与测试
统一 reactor 回归通过：
- PromQL：**21/21 PASS**
- LogQL：**4/4 PASS**
- TraceQL：**8/8 PASS**

## 4. 重点发现
### 4.1 PromQL
重点是 API 参数/响应边界和 PromQL visitor，不是单纯 parser。

### 4.2 TraceQL
`TraceQLParser.g4` 引用了 `INTRINSIC`、`EVENT`、`LINK`、`RE`、`NRE`，但 lexer 中这些 token 当前被注释掉；ANTLR 因此给出 implicit token warnings。

这表示：
- grammar 预留了能力
- lexer 当前明确未支持这些语法
- 不能把 warning 直接当作编译失败，但必须记录为协议能力边界

### 4.3 LogQL
当前测试规模最小，首轮看到的失败日志来自故意的非法输入场景，测试仍然 PASS；需区分 parser error log 与测试失败。

## 5. 当前方法论结论
三条 DSL 不应强行合并成一份实现性大纲。当前阶段先完成：
1. 统一边界与能力盘点
2. 记录 TraceQL implicit-token 预留能力
3. 记录各 DSL 的测试密度差异
4. 后续按 4C3A/4C3B/4C3C 分别做针对性深审

## 6. 下一步建议
优先顺序：
1. PromQL API 参数/错误响应边界
2. TraceQL grammar reserved-token 与 converter 边界
3. LogQL parser/visitor error 与结果映射
