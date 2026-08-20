# SW-4C4 Status/debug/ops query — Pass 2 问题收敛

> 模块: `oap-server/server-query-plugin/status-query-plugin`
> 日期: 2026-08-18
> 状态: Pass 2 已完成首轮；已修复 2 个真实生产缺陷，保留参数与状态边界风险

## Q1: debugging trace 的 parent span 缺失时是否会失败
初版存在真实缺陷。

`DebuggingHTTPHandler.transformTrace(...)` 原本对每个非 root span 执行：

```java
DebuggingSpanRsp parentSpan = spanMap.get(span.getParentSpanId());
parentSpan.getChildSpans().add(...);
```

当输入 trace 来自不完整、跨来源或被裁剪的 span 集合时，parent ID 可能不存在于当前 `spanMap`，此时 `parentSpan` 为 null，debug endpoint 直接抛 `NullPointerException`。

最小 harness：
- 构造只有 span 1、parentId=99 的 trace
- 反射调用 `transformTrace`
- 初版在 `DebuggingHTTPHandler.java:521` 失败

修复：
- 只有 parent span 存在时才建立 child 关系
- orphan span 仍被转换并保留在 span map 中
- 没有 span 0 的输入返回 `rootSpan == null`，但不再抛异常

新增测试：
- `status-query-plugin/src/test/java/org/apache/skywalking/oap/query/debug/DebuggingHTTPHandlerTest.java`
- `status-query-plugin/src/test/java/org/apache/skywalking/oap/query/debug/StatusQueryExceptionHandlerTest.java`
- `server-core/src/test/java/org/apache/skywalking/oap/server/core/status/ServerStatusServiceTest.java`

## Q2: DebuggingTraceContext 的 ThreadLocal 是否会残留
已读源码确认：
- MQE resolver 在 finally 中 `TRACE_CONTEXT.remove()`
- DebuggingHTTPHandler 的两个 Zipkin endpoint 在 finally 中 `stopTrace()` 并 remove
- trace/basic/topology/log query 的清理主要依赖下游 resolver/query service

本轮没有继续确认 ThreadLocal 泄漏。后续应补异常路径测试，特别是 `queryBasicTraces`、topology 和 log query 的 `CompletableFuture.join()` 异常。

## Q3: transformTrace 是否还存在其他不完整 trace 风险
已确认并记录：
- orphan parent 已修复
- `Collectors.toMap(DebuggingSpan::getSpanId, ...)` 对重复 spanId 仍可能抛 `IllegalStateException`
- `spanMap.get(0)` 为空是允许的响应状态，但调用方必须接受 `rootSpan == null`
- parent-child 关系只建立一层 map lookup，不会递归失败

重复 spanId 是否可能由真实 storage/query 数据产生，尚未用实际数据或 harness 确认，因此暂列风险。

## Q4: comma-separated tag 解析是否安全
初版存在真实缺陷。

原实现对 tag 使用 `t.split(Const.EQUAL)` 后读取 `[0]`、`[1]`：
- value 含 `=` 时后续 value 被丢弃
- 缺少 `=` 时可能触发数组越界
- 同一逻辑存在于 `queryBasicTraces` 和 `queryLogs`

新增 handler harness 通过 `queryLogs(tags=token=left=right)` 实锤了 value 截断。

修复：
- 抽取共享 `parseTags(...)`
- 使用 `split(Const.EQUAL, 2)`，只按第一个等号分隔
- 缺少等号或空 key 时抛 `IllegalArgumentException`，交由 status exception handler 映射为 400

当前 `DebuggingHTTPHandlerTest` 已验证 value `left=right` 被完整传给 `LogQueryService`。

## Q5: 配置脱敏是否安全
`ServerStatusService.dumpBootingConfigurations(...)` 已读源码：
- keyword matching 对 key 大小写不敏感
- 嵌套 `Properties` 也执行 mask
- 命中 keyword 的 value 输出 `******`
- 未 booted 或空配置返回空列表

已新增 `ServerStatusServiceTest` 覆盖：空配置、单 provider、普通 key、大小写不敏感 keyword、嵌套 Properties。定向 `server-core` reactor 通过，脱敏路径未发现新的生产缺陷。

## Q6: status handler 的异常映射
`StatusQueryExceptionHandler` 已确认：
- `IllegalArgumentException` -> 400
- 其他异常 -> 500
- body 使用 exception message 或异常类名

仍需验证 Armeria annotation handler 是否覆盖所有 handler 异常，以及 JSON/text response 的 content type 是否符合调用方约定。

## Q7: 回归结果
定向命令：

```bash
./mvnw -pl oap-server/server-query-plugin/status-query-plugin -am \
  -Dtest=DebuggingHTTPHandlerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：
- `DebuggingHTTPHandlerTest`: 2/2 PASS
- `StatusQueryExceptionHandlerTest`: 2/2 PASS
- status-query-plugin 定向 reactor: `BUILD SUCCESS`
- `ServerStatusServiceTest`: 2/2 PASS
- server-core 定向 reactor: `BUILD SUCCESS`

完整模块回归：

```bash
./mvnw -pl oap-server/server-query-plugin/status-query-plugin -am test
```

结果：
- `server-alarm-plugin`: 53 tests, 0 failures, 0 errors, 1 skipped
- `DebuggingHTTPHandlerTest`: 2/2 PASS
- `StatusQueryExceptionHandlerTest`: 2/2 PASS
- status-query-plugin reactor: `BUILD SUCCESS`
