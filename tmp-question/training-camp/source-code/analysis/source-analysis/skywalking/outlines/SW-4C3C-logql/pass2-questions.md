# SW-4C3C LogQL compatibility — Pass 2 问题收敛

> 模块: `server-query-plugin/logql-plugin`
> 日期: 2026-08-18
> 状态: Pass 2 已完成；保留一个未决协议前提风险

## Q1: labels / label-values 在缺省 start/end 时是否会 NPE
初版实现存在真实缺陷。

`LogQLApiHandler.labels(...)` 和 `labelValues(...)` 原本直接把可选的 `Long start/end` 拆箱并传入纳秒转毫秒逻辑；HTTP 请求缺省任一参数时会在 handler 内触发 NPE。

修复后：
- `end == null` 时使用当前时间的纳秒值
- `start == null` 时使用 `end - 24h`
- 两个 endpoint 都通过 `DurationUtils.timestamp2Duration(...)` 委托给 tag autocomplete service

当前测试：
- `LogQLApiHandlerTest.shouldReturnLabelNames`
- `LogQLApiHandlerTest.shouldUseDefaultDurationForLabelValues`

## Q2: labels / label-values 的 Mockito 测试是否真正验证了委托
初版测试没有通过，原因不是生产代码，而是 Mockito matcher 规则错误：
- `TagAutoCompleteQueryService.queryTagAutocompleteKeys` 的精确签名是 `(TagType, Duration)`
- raw `TagType.LOG` 与 `any(Duration.class)` 混用会触发 `InvalidUseOfMatchersException`

修复后统一使用：
- `eq(TagType.LOG)`
- `eq("service")`
- `any(Duration.class)`

定向回归确认两个测试均通过。

## Q3: LogQL parser 是否会阻止非法 query_range 查询
已确认。

`rangeQuery(...)` 为 lexer 与 parser 安装 `ParseErrorListener`，parser 触发 `ParseCancellationException` 时返回 HTTP 400，而不是继续调用 `LogQueryService`。

原有 `LogQLExprVisitorTest` 已覆盖：
- 非法前缀
- selector 后非法后缀
- 非法 filter

当前结果：原有 4 个 visitor 测试全部通过。

## Q4: blank/star label 是否保持 All 查询语义
已确认 visitor 行为：
- stream selector 中的 blank 或 `*` 值会被忽略
- 非 blank、非 `*` 值进入 label map
- line filter 分别进入 include/exclude keyword 列表

该结论来自既有 `LogQLExprVisitorTest`，本轮没有改变 visitor 生产代码。

## Q5: 纳秒时间是否正确转换为 OAP Duration
已确认 labels/label-values 路径：
- Loki 纳秒时间先除以 1,000,000
- 再通过 `DurationUtils.timestamp2Duration(...)` 创建 OAP Duration
- 缺省时间范围为当前时刻往前 24 小时

`query_range` 路径同样调用 `nano2Millis(start/end)`；新增成功路径测试已断言最终 `Duration` 的格式化 start/end 值，并验证了 `BACKWARD` 顺序和 tags/keywords 委托。

## Q6: query_range 的 start/end/direction 是否允许缺省
当前实现依赖调用方提供这些参数，尚未确认是否符合全部 Loki 兼容协议场景：
- `start` 或 `end` 为 null 时，`nano2Millis(...)` 会触发 NPE
- `direction` 为 null 时，`direction.getOrder()` 会触发 NPE
- `limit` 直接传入 `new Pagination(1, limit)`，其 null 行为尚未在本域专项验证

这目前记录为**未决协议前提风险**，不是本轮确认的生产缺陷。下一轮应先查 Armeria 参数绑定、SkyWalking API 约定和 Loki compatibility contract，再决定是补默认值、返回 400，还是维持必填约束。

## Q7: query_range 的业务错误和空结果映射是否正确
已读源码确认：
- `Logs.errorReason` 非空时返回 HTTP 400
- 无 error 时返回 HTTP 200、`resultType=streams`
- 日志按 service/instance/endpoint/traceId 组合分组
- 每条日志的 OAP 毫秒时间乘 1,000,000 后输出 Loki 纳秒字符串

但本轮没有 mock `LogQueryService` 的完整 handler 测试，因此以下行为仍需后续测试确认：
- errorReason body 内容
- 空日志列表的 response JSON
- 相同 stream key 的聚合
- `FORWARD`/`BACKWARD` 到 `Order.ASC`/`Order.DES`
- include/exclude keyword 及普通 tag 的完整委托参数

## Q8: 本轮是否发现新的生产缺陷
确认修复 1 个生产缺陷：缺省 `start/end` 下 labels/label-values NPE。

除 Q6 的协议前提风险和 Q7 的测试空洞外，本轮没有继续确认新的生产缺陷。

## Q9: 回归结果
定向命令：

```bash
./mvnw -pl oap-server/server-query-plugin/logql-plugin -am \
  -Dtest=LogQLApiHandlerTest,LogQLExprVisitorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：
- `LogQLApiHandlerTest`: 4/4 PASS
- `LogQLExprVisitorTest`: 4/4 PASS
- LogQL reactor: `BUILD SUCCESS`

完整模块回归：

```bash
./mvnw -pl oap-server/server-query-plugin/logql-plugin -am test
```

结果：
- `server-core`: 216/216 PASS
- `LogQLApiHandlerTest`: 4/4 PASS
- `LogQLExprVisitorTest`: 4/4 PASS
- LogQL reactor: `BUILD SUCCESS`
- 输出中存在既有 Netty Brotli native library warning 和异步 MAL 测试线程日志，但未导致 Surefire 失败，最终 Maven exit status 为成功。
