# SW-4C3B TraceQL compatibility — Outline

> 模块: `server-query-plugin/traceql-plugin`
> 日期: 2026-08-18

## 1. 域定位
`SW-4C3B` 提供 TraceQL/Tempo 风格 query compatibility：
- TraceQL grammar/parser
- query parameter extraction
- Zipkin/SkyWalking trace query handler
- Zipkin/OTLP/SkyWalking trace converter
- Tempo response model

## 2. 主链
`TraceQLApiHandler`
→ `TraceQLQueryParser.extractParams(...)`
→ `TraceQLQueryVisitor`
→ `TraceQLQueryParams`
→ datasource-specific query service
→ `ZipkinOTLPConverter` / `SkyWalkingOTLPConverter`
→ Tempo/OTLP response

## 3. Parser 边界
### 3.1 已支持
- scoped/unscoped attributes
- `&&` / `||` / `!`
- `=` / `!=` / `<` / `<=` / `>` / `>=`
- string/number/duration/bool/nil literal
- duration conversion to microseconds

### 3.2 未启用/预留
parser grammar 中存在但 lexer 当前注释掉：
- `INTRINSIC`
- `EVENT`
- `LINK`
- `RE`
- `NRE`

因此 ANTLR 生成时会有 implicit token warnings；这是当前协议能力边界。

## 4. 本轮确认并修复的真实缺陷
`TraceQLQueryParser.parse(...)` 初版没有安装 syntax error listener，也不检查 lexer/parser syntax errors。

后果：
- malformed query 只输出 ANTLR 错误
- `extractParams(...)` 仍可能返回无 error 的结果

修复后：
- lexer/parser 共享错误收集 listener
- 任意 syntax error 都转成 `IllegalArgumentException`
- `extractParams(...)` 统一返回 `TraceQLParseResult.error(...)`

## 5. Trace 数据源边界
- Zipkin handler 走 Zipkin query service/converter
- SkyWalking handler 走 SkyWalking query service/converter
- 最终 OTLP response 中 traceId/spanId 编码按 `TraceType` 区分

## 6. 测试覆盖
原有：TraceQL parser **8 个**。

本轮新增：
- `TraceQLQueryParserErrorTest`

新增覆盖：
- malformed query 必须进入 error result

完整回归：TraceQL **9/9 PASS**。

## 7. 外域关系
- `SW-4C3B`：TraceQL compatibility
- `SW-4C2`：Zipkin query compatibility 是底层数据源之一
- `SW-4C1`：GraphQL 不直接并入
- `SW-7`：trace receiver/ingress 不在本域

## 8. 当前结论
`SW-4C3B` 当前达到可交接收敛状态：
- parser/visitor/handler/converter 边界清晰
- parser error seam 已修复
- 预留 token 能力边界已明确记录
- 完整回归通过

## 9. 剩余风险
- implicit token 预留能力未来启用时需要同步 lexer/parser/visitor/converter
- converter 对多类型 OTLP AnyValue 的完整保真度仍值得后续专项审计
