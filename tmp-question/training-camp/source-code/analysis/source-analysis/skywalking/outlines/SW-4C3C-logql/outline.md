# SW-4C3C LogQL compatibility — Outline

> 模块: `server-query-plugin/logql-plugin`
> 日期: 2026-08-18
> 状态: Pass 2/3 收敛中；核心 handler 修复已完成，query_range 仍有测试空洞

## 1. 域定位

`SW-4C3C` 提供 Loki/LogQL 风格的日志查询兼容层：
- LogQL ANTLR grammar/parser
- stream selector 与 line filter 解析
- Loki labels、label values、query_range HTTP API
- OAP `LogQueryService` / `TagAutoCompleteQueryService` 适配
- Loki response model

## 2. 主链

### 2.1 元数据 endpoint

`LogQLProvider`
→ `LogQLApiHandler.labels(...)` / `labelValues(...)`
→ `TagAutoCompleteQueryService`
→ OAP tag autocomplete DAO
→ `LabelsQueryRsp` / `LabelValuesQueryRsp`

### 2.2 日志查询 endpoint

`LogQLProvider`
→ `LogQLApiHandler.rangeQuery(...)`
→ `LogQLLexer` / `LogQLParser`
→ `LogQLExprVisitor`
→ `LogQLParseResult`
→ service/instance/endpoint/trace/tag 条件
→ `LogQueryService.queryLogs(...)`
→ stream 分组
→ Loki `LogRangeQueryRsp`

## 3. Parser / visitor 能力边界

### 3.1 当前支持
- `{label="value"}` stream selector
- `|="text"` contains filter
- `!=` not-contains filter
- 多个 line filters
- selector label 到 label map 的映射
- include/exclude content keyword 分离

### 3.2 All 查询语义

对 service、service instance、endpoint 等受支持的命名 label：
- blank 值被忽略
- `*` 值被忽略
- 有效非 blank、非 `*` 值进入条件

普通 stream selector label 会作为日志 tag 传给 OAP；内置 `LabelName` 字段不重复作为普通 tag。

### 3.3 非法语法

`rangeQuery(...)` 为 lexer/parser 安装 `ParseErrorListener`。语法错误触发 `ParseCancellationException` 后返回 HTTP 400，不继续调用日志查询服务。

## 4. 本轮确认并修复的真实缺陷

### 4.1 缺省时间参数导致 NPE

原实现中：
- `/loki/api/v1/labels` 的 `start/end` 为 null 时会在纳秒转毫秒路径拆箱
- `/loki/api/v1/label/{label_name}/values` 同样会失败

修复后：
- `end == null` 使用当前时间
- `start == null` 使用当前时间往前 24 小时
- 两个 endpoint 都构造 `Duration` 并委托到 tag service

生产文件：
`oap-server/server-query-plugin/logql-plugin/src/main/java/org/apache/skywalking/oap/query/logql/handler/LogQLApiHandler.java`

## 5. Handler 参数与响应语义

### 5.1 labels
- 读取可选纳秒 `start/end`
- 默认时间窗口为 24 小时
- 查询 `TagType.LOG` 的 autocomplete keys
- 返回 HTTP 200 的 Loki labels response

### 5.2 label values
- 读取 `label_name` 与可选纳秒 `start/end`
- 默认时间窗口为 24 小时
- 查询 `TagType.LOG` 的 autocomplete values
- 返回 HTTP 200 的 Loki label-values response

### 5.3 query_range
- 读取 `start/end/query/limit/direction`
- 解析 selector 与 line filters
- 构造 service、service instance、endpoint、trace 和普通 tag 条件
- 传递 include/exclude keywords
- `FORWARD` 映射 `Order.ASC`，`BACKWARD` 映射 `Order.DES`
- OAP 毫秒日志时间输出为 Loki 纳秒字符串
- `errorReason` 非空时返回 HTTP 400
- 正常结果按 stream key 分组并返回 `resultType=streams`

## 6. 测试覆盖

### 6.1 原有测试
`LogQLExprVisitorTest`：4 个测试，覆盖 selector、blank/star、contains/not-contains、多过滤器和非法 grammar。

### 6.2 本轮新增测试
`LogQLApiHandlerTest`：4 个测试：
- `shouldReturnLabelNames`
- `shouldUseDefaultDurationForLabelValues`
- `shouldDelegateRangeQueryWithParsedFiltersAndTimeRange`
- `shouldRejectMalformedRangeQueryWithoutCallingService`

测试使用 Mockito `eq(...)` + `any(...)`，验证两个 metadata endpoint 均委托到正确的 tag service。

## 7. 回归结果

定向命令：

```bash
./mvnw -pl oap-server/server-query-plugin/logql-plugin -am \
  -Dtest=LogQLApiHandlerTest,LogQLExprVisitorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

实测结果：
- `LogQLApiHandlerTest`: 4/4 PASS
- `LogQLExprVisitorTest`: 4/4 PASS
- LogQL reactor: `BUILD SUCCESS`

完整模块回归：

```bash
./mvnw -pl oap-server/server-query-plugin/logql-plugin -am test
```

实测结果：
- `server-core`: 216/216 PASS
- `LogQLApiHandlerTest`: 4/4 PASS
- `LogQLExprVisitorTest`: 4/4 PASS
- LogQL reactor: `BUILD SUCCESS`

构建输出包含既有 Netty Brotli native library warning 和异步 MAL 测试线程日志，但 Surefire 与 Maven 最终均成功。

## 8. 外域关系

- `SW-4C1`: GraphQL query boundary，不并入 LogQL
- `SW-4C2`: Zipkin query compatibility，底层 trace query，不并入日志查询
- `SW-4C3A`: PromQL compatibility，独立 DSL
- `SW-4C3B`: TraceQL compatibility，独立 DSL
- `SW-4C4`: Status/debug/ops query，后续独立审计
- `SW-5`: storage/query DAO 实现是本域的下游依赖，不在本域展开

## 9. 剩余风险与下一轮问题

1. `query_range` 的 `start/end/direction` 缺省行为尚未由协议和测试确认；当前源码直接使用，可能产生 NPE。
2. `query_range` 已有专属 handler mock 测试，覆盖 parser error、方向、时间和 tags/keywords 委托；errorReason、空结果、stream 分组、FORWARD 方向及 limit null 仍可继续扩展。
3. `limit == null` 的行为尚未确认。
4. 纳秒↔毫秒转换的边界、负值、溢出和精度损失尚未专项验证。
5. Loki response 对空字段（例如 null service instance/endpoint）和 stream key 拼接碰撞的行为尚未专项验证。

## 10. 当前结论

核心 LogQL parser/visitor、metadata handler 和 query_range 的关键委托/error seam 已完成修复并通过定向回归；本域仍保留 query_range 可选参数协议、errorReason/空结果/stream 分组等扩展覆盖风险，暂不宣称协议层完全收敛。
