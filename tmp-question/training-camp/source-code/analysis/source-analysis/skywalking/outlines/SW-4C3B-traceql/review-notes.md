# SW-4C3B TraceQL compatibility — Review Notes

> 日期: 2026-08-18
> 轮次: 3 轮

## Review 1 — 能力与边界复核
确认：
- TraceQL 是 grammar + parameter extraction + datasource converter 的复合插件
- 同时存在 Zipkin 与 SkyWalking 两套 trace datasource
- parser grammar 与 lexer 当前能力并不完全对齐

## Review 2 — parser error seam 质疑
新增 malformed query 测试后实锤：
- `TraceQLQueryParser.parse(...)` 没有捕获 lexer/parser syntax error
- 非法 `{` 查询可能返回无 error 的成功结果
- ANTLR 只把错误打印到输出

## Review 3 — 修复与回归
修复：
- 添加 lexer/parser 共享 `BaseErrorListener`
- 解析后统一检查错误并抛出
- `extractParams(...)` 统一转 error result

验证：
```bash
./mvnw -pl oap-server/server-query-plugin/traceql-plugin -am -Dtest=TraceQLQueryParserErrorTest,TraceQLQueryParserTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/server-query-plugin/traceql-plugin -am test
```

结果：TraceQL **9/9 PASS**。

## 最终判断
- parser error 缺陷已修复
- 当前没有新的已知生产代码问题
- implicit token warning 已被识别为未启用能力边界，而非忽略

## 剩余风险
- OTLP AnyValue 非 string 类型的转换保真度
- 预留 TraceQL scope/operator 启用时的跨层同步
