# SW-4C3A PromQL compatibility — Review Notes

> 日期: 2026-08-18
> 轮次: 3 轮

## Review 1 — 结构复核
确认：
- PromQL 是 DSL + HTTP compatibility API 的复合模块
- `PromQLApiHandler` 是主要边界，而不是只看 grammar
- `PromQLExprQueryVisitor` 负责语义求值和错误分类

## Review 2 — 时间协议质疑
新增秒/毫秒 timestamp 测试后实锤：
- 毫秒 Unix timestamp 被错误乘以 1000
- 这是 Prometheus API 兼容性缺陷

## Review 3 — 修复与回归
修复：
- `formatTimestamp2Millis(...)` 区分秒级与毫秒级数值

验证：
```bash
./mvnw -pl oap-server/server-query-plugin/promql-plugin -am -Dtest=PromQLApiHandlerTimestampTest,PromQLApiHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/server-query-plugin/promql-plugin -am test
```

结果：通过，PromQL 完整回归 **23/23 PASS**。

## 最终判断
- Unix timestamp 单位缺陷已修复
- 当前没有新的已知生产代码问题
- 文档、源码、测试已一致

## 剩余风险
- limit/selector 组合和其他 API 参数边界还可继续 targeted probing
- PromQL 与 GraphQL/MQE 的跨域 response 语义留待后续交叉审计
