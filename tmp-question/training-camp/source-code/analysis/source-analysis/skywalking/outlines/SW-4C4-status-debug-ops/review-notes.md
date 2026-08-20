# SW-4C4 Status/debug/ops query — Review Notes

> 模块: `oap-server/server-query-plugin/status-query-plugin`
> 日期: 2026-08-18
> Review 状态: 已完成 4 轮；已修复 2 个真实生产缺陷，剩余风险明确记录

## Review 1：边界—源码一致性

### 检查范围
- `StatusQueryProvider`
- `DebuggingHTTPHandler`
- `TTLConfigQueryHandler`
- `ClusterStatusQueryHandler`
- `AlarmStatusQueryHandler`
- `StatusQueryExceptionHandler`
- `ServerStatusService`
- `DebuggingTraceContext`

### 结果
- 实际模块确认是 `status-query-plugin`，不是此前假设的不存在目录。
- Provider 确实注册四个 handler，所有 endpoint 已按源码列出。
- DebuggingHTTPHandler 直接复用 GraphQL resolver/Zipkin handler，边界文档已明确。
- `DebuggingTraceContext` 的主要下游异步 resolver 有 finally 清理，但所有 query path 仍需进一步异常测试。
- `ServerStatusService` 配置脱敏支持嵌套 Properties，当前文档没有把源码阅读误写成测试通过。

### 结论
通过。Pass0 范围与实际目录/入口一致。

## Review 2：缺陷—harness—修复一致性

### 原始失败
新增 `DebuggingHTTPHandlerTest` 构造 orphan span：
- spanId = 1
- parentSpanId = 99
- 当前 trace 没有 parent 99

生产初版在：
`DebuggingHTTPHandler.java:521`

抛出：
`NullPointerException: Cannot invoke DebuggingSpanRsp.getChildSpans() because parentSpan is null`

### 修复
`transformTrace(...)` 现在仅当 `parentSpan != null` 时建立 child 关系；orphan span 继续保留在转换 map 中。

### 回归
```bash
./mvnw -pl oap-server/server-query-plugin/status-query-plugin -am \
  -Dtest=DebuggingHTTPHandlerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：
- `DebuggingHTTPHandlerTest`: 1/1 PASS
- status-query-plugin reactor: `BUILD SUCCESS`

### 结论
通过。harness 证明了真实生产缺陷，修复后测试通过。

## 已确认的剩余高风险

### Tag parser
已确认并修复 value 含等号的截断；`DebuggingHTTPHandlerTest` 通过 service 参数捕获验证。缺少等号、空 value 和空 tag segment 的协议语义仍可继续扩展。

### Transform trace
重复 spanId 可能触发 `Collectors.toMap` duplicate key；尚未确认真实输入是否允许。

### Status handlers
alarm/cluster/TTL 仍缺少专属测试，尤其是远端异常、null payload、空节点和 response content type；异常统一映射本身已由 `StatusQueryExceptionHandlerTest` 覆盖。

### Configuration dump
`ServerStatusServiceTest` 已验证空配置、provider key、普通值、大小写不敏感 keyword 和 nested Properties 脱敏；定向 server-core reactor 通过。

## Review 3：新增测试与回归事实一致性

- `ServerStatusServiceTest`: 2/2 PASS，覆盖空配置、provider key、大小写 keyword、nested Properties。
- `StatusQueryExceptionHandlerTest`: 2/2 PASS，覆盖 400/500 映射。
- `DebuggingHTTPHandlerTest`: 2/2 PASS，覆盖 orphan parent 和 tag value 含等号。
- 完整 status-query-plugin reactor：`BUILD SUCCESS`；server-alarm-plugin 53 tests、0 failures、0 errors、1 skipped。
- status-query-plugin 新增测试总计 4/4：DebuggingHTTPHandlerTest 2/2、StatusQueryExceptionHandlerTest 2/2。
- 当前剩余风险仍只包括 tag parser、重复 spanId、其他 status handler 专属委托和 ThreadLocal 异常路径；没有把它们误标为已验证。

### 下一轮建议

1. 为 tag parser 建立成功/非法值 harness；若确证协议允许 `=`，修复为 limit 2。
2. 为 TTL/cluster/alarm handler 添加最小 service 委托测试。
3. 为 debugging query 的异常路径验证 ThreadLocal 清理。
4. 当前第 5 轮 review 已完成；后续每次新增代码或测试后重新执行至少 2 轮文档/源码/测试一致性 review。
