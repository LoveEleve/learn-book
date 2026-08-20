# SW-4C3C LogQL compatibility — Pass 0 发现

> 模块: `server-query-plugin/logql-plugin`
> 日期: 2026-08-18

## 1. 域职责
`SW-4C3C` 提供 Loki/LogQL 风格的日志查询兼容层：
- LogQL grammar/parser
- stream selector 与 line filter 提取
- Loki labels/label values/query_range HTTP API
- OAP `LogQueryService` / `TagAutoCompleteQueryService` 适配
- Loki response model

## 2. 主链
`LogQLProvider`
→ `LogQLApiHandler`
→ `LogQLLexer/Parser`
→ `LogQLExprVisitor`
→ labelMap + content include/exclude keywords
→ `LogQueryService`
→ Loki response JSON

## 3. 当前能力边界
grammar 当前支持：
- `{label="value"}` stream selector
- `|="text"` contains
- `!=` not contains
- 多个 line filters

visitor 语义：
- blank 或 `*` 的 service_instance/endpoint 等值被忽略，表示 All 查询
- stream selector label 进入 labelMap
- line filters 分成 include/exclude keyword 列表

## 4. Handler endpoint
- `/loki/api/v1/labels`
- `/loki/api/v1/label/{label_name}/values`
- `/loki/api/v1/query_range`

handler 将纳秒时间参数转毫秒，再构造 OAP `Duration`。

## 5. 当前测试现实
- 原有 LogQL 测试：**4/4 PASS**
- 覆盖 stream selector、blank/star 忽略、contains/not contains、多行 filter、非法 grammar
- 当前没有专属 handler 测试

## 6. 首轮质疑点
- Q1: parser error 是否正确抛出并阻止 query service 调用
- Q2: blank/star label 的 All 语义是否只作用于预期字段
- Q3: 纳秒到毫秒转换与 Loki response 时间输出是否对称
- Q4: query_range 的 limit/direction/空结果/errorReason 映射
- Q5: handler 的 start/end 是否依赖必填协议参数

## 7. 当前结论
首轮没有发现明确生产缺陷；主要风险在 handler 层测试空洞，而不是 visitor 已覆盖的基础 selector/filter 语义。
