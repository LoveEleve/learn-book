# SW-4C3B TraceQL compatibility — Pass 2 问题收敛

> 模块: `server-query-plugin/traceql-plugin`
> 日期: 2026-08-18

## Q1: TraceQL parser 是否真正报告语法错误
初版不正确。

`TraceQLQueryParser.parse(...)` 原本：
- 创建 lexer/parser
- 直接调用 `parser.query()`
- 不移除默认 listener
- 不收集 lexer/parser syntax error
- 不检查 syntax error count

后果：
- 非法查询（如 `{`）只在 stderr/log 中出现 ANTLR 错误
- `extractParams(...)` 仍可能继续 visitor 并返回成功结果

修复：
- 增加共享 `BaseErrorListener`
- 同时挂到 lexer 与 parser
- 解析后有错误则抛 `IllegalArgumentException`
- 由 `extractParams(...)` 统一包装为 `TraceQLParseResult.error(...)`

## Q2: TraceQL lexer/parser 是否完全支持 grammar 声明的能力
不完全支持。

parser grammar 声明了：
- `INTRINSIC`
- `EVENT`
- `LINK`
- `RE`
- `NRE`

但 lexer 中这些 token 当前被注释，ANTLR 生成阶段会给 implicit token warning。

结论：
- 这些是预留/未启用能力
- 当前不应宣称支持 intrinsic/event/link scope 或 regex operator
- warning 是能力边界信号，不是当前构建失败

## Q3: 当前 visitor 的参数映射
已确认：
- `.service.name` / `resource.service.name` -> serviceName
- `resource.instance` -> serviceInstance
- `name` / `span.name` -> spanName
- `http.status_code` -> httpStatusCode
- 其他 scoped attributes 去掉 scope 前缀后进入 tags
- duration 支持 us/µs/ms/s/m/h，并转换为微秒

## Q4: 三种 trace 数据源的转换边界
TraceQL handler 分为：
- Zipkin datasource handler
- SkyWalking datasource handler

二者最终都转换为 Tempo/OTLP response，但 traceId/spanId 编码规则不同，由 `OTLPConverter.TraceType` 区分。

## Q5: 当前是否还有已知生产代码缺陷
截至本轮，除 parser error listener 缺失外，没有新的可复现生产代码问题。

## Q6: 回归
```bash
./mvnw -pl oap-server/server-query-plugin/traceql-plugin -am -Dtest=TraceQLQueryParserErrorTest,TraceQLQueryParserTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/server-query-plugin/traceql-plugin -am test
```

结果：TraceQL **9/9 PASS**。
