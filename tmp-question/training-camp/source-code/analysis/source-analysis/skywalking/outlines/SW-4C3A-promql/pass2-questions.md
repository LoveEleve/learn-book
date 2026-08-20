# SW-4C3A PromQL compatibility — Pass 2 问题收敛

> 模块: `server-query-plugin/promql-plugin`
> 日期: 2026-08-18

## Q1: Unix timestamp 的单位处理是否正确
初版存在真实兼容性缺陷：
- `formatTimestamp2Millis(...)` 对所有数字统一执行 `numericTimestamp * 1000`
- 秒级 timestamp 正确
- 毫秒级 timestamp 会被错误放大 1000 倍

实证：
- `"1698928776"` -> `1698928776000`（正确）
- `"1698928776000"` -> `1698928776000000`（错误）

修复：
- 对绝对值达到毫秒级量级的数字按毫秒原样保留
- 其他数字仍按秒乘 1000
- RFC3339 分支保持不变

## Q2: parser/visitor 错误是否统一为 response error
已确认：
- parser `ParseCancellationException` 在 handler 中映射为 `ResultStatus.ERROR + ErrorType.BAD_DATA`
- visitor 的非法 label、缺少 layer、range/instant 误用也返回 `BAD_DATA`
- IO 异常转为 `INTERNAL`

## Q3: PromQL 当前 API 兼容重点是什么
不是单纯 grammar，而是：
- `query/query_range`
- `labels/label values`
- metadata/buildinfo
- limit/match/start/end/time 参数
- Prometheus response model

## Q4: 当前测试覆盖变化
原有：
- handler RFC3339 parse
- visitor expression/aggregation
- match visitor

本轮新增：
- 秒级 Unix timestamp
- 毫秒级 Unix timestamp

完整 PromQL 回归：**23/23 PASS**。

## Q5: 当前是否还有已知缺陷
截至本轮，除已修复的秒/毫秒单位缺陷外，没有新的可复现生产代码问题。

剩余风险主要在：
- limit 与 matcher label 的交互
- 多个 API endpoint 的参数边界
- Prometheus 客户端对 response warning/error 字段的兼容细节
