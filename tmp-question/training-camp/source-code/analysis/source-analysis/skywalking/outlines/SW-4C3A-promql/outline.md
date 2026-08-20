# SW-4C3A PromQL compatibility — Outline

> 模块: `server-query-plugin/promql-plugin`
> 日期: 2026-08-18

## 1. 域定位
`SW-4C3A` 是 Prometheus/PromQL 兼容插件：
- grammar/parser
- PromQL expression visitor
- Prometheus HTTP API handler
- OAP metrics/query service 适配
- Prometheus response model

## 2. 主链
`PromQLProvider`
→ `PromQLApiHandler`
→ time/range/limit/match 参数解析
→ `PromQLParser`
→ `PromQLExprQueryVisitor` / `PromQLMatchVisitor`
→ core query services
→ Prometheus response JSON

## 3. 核心边界
### 3.1 Handler
负责：
- API 路由
- 时间格式转换
- limit/matcher 处理
- parser/visitor 错误结果映射
- response 序列化

### 3.2 Visitor
负责：
- scalar/vector/matrix 表达式求值
- binary/compare/aggregation
- label selector
- `BAD_DATA` / `INTERNAL` 语义分类

## 4. 本轮确认并修复的真实缺陷
`PromQLApiHandler.formatTimestamp2Millis(...)` 原本把所有数字都当成秒：
- 秒级输入正确
- 毫秒级输入被再次乘 1000

修复后：
- 秒级数字转换为毫秒
- 毫秒级数字原样保留
- RFC3339 输入继续按 `OffsetDateTime` 转换

## 5. 测试覆盖
原有 PromQL 测试：
- RFC3339 时间
- expression visitor 17 个
- match visitor 3 个
- handler 基础测试

本轮新增：
- `PromQLApiHandlerTimestampTest`

覆盖：
- 秒级 Unix timestamp
- 毫秒级 Unix timestamp

完整 PromQL 回归：**23/23 PASS**。

## 6. 外域关系
- `SW-4B`：MQE runtime 是 PromQL/GraphQL 之外的另一条表达式执行链
- `SW-4C1`：GraphQL 侧提供 `MetadataQueryV2` 等可复用 query façade
- `SW-5`：底层 metrics/query DAO 不在本域展开

## 7. 当前结论
`SW-4C3A` 当前达到可交接收敛状态：
- handler/visitor/parser 边界清晰
- Unix timestamp 单位缺陷已修复
- 时间与表达式回归已覆盖
- 暂无新的已知源码问题

## 8. 剩余风险
- limit 与 selector label 交互
- 多 endpoint 参数校验的一致性
- response warning/error 字段的外部客户端兼容性
