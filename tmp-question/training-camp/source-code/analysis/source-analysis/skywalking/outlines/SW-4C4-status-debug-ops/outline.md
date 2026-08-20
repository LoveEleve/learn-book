# SW-4C4 Status/debug/ops query — Outline

> 模块: `oap-server/server-query-plugin/status-query-plugin`
> 日期: 2026-08-18
> 状态: Pass 2/3 收敛中；已修复 debugging trace orphan parent 缺陷

## 1. 域定位

`SW-4C4` 是 OAP 的 status、debugging 和 operational query HTTP compatibility 层，实际实现位于：

```text
oap-server/server-query-plugin/status-query-plugin/
```

它聚合四类能力：
- booting configuration dump
- TTL、cluster、alarm status
- debugging MQE/trace/Zipkin/topology/log query
- debugging trace context 与响应树转换

## 2. Provider 与注册

`StatusQueryProvider`：
- `prepare()` 注册 `AlarmStatusQueryService`
- `start()` 向 `HTTPHandlerRegister` 注册四个 handler
- 只声明 `CoreModule` 为 required module

注册 handler：
- `DebuggingHTTPHandler`
- `TTLConfigQueryHandler`
- `ClusterStatusQueryHandler`
- `AlarmStatusQueryHandler`

## 3. Endpoint 边界

### Status
- `/status/config/ttl`
- `/status/cluster/nodes`
- `/status/alarm/rules`
- `/status/alarm/{ruleId}`
- `/status/alarm/{ruleId}/{entityName}`

### Debugging configuration
- `/debugging/config/dump`

### Debugging query
- `/debugging/query/mqe`
- `/debugging/query/trace/queryBasicTraces`
- `/debugging/query/trace/queryTrace`
- `/debugging/query/zipkin/api/v2/traces`
- `/debugging/query/zipkin/api/v2/trace`
- `/debugging/query/topology/getGlobalTopology`
- `/debugging/query/topology/getServicesTopology`
- `/debugging/query/topology/getServiceInstanceTopology`
- `/debugging/query/topology/getEndpointDependencies`
- `/debugging/query/topology/getProcessTopology`
- `/debugging/query/log/queryLogs`

## 4. 主链

```text
HTTPHandlerRegister
  -> status/debug handler
     -> core status service / GraphQL resolver / Zipkin handler
        -> storage, alarm, cluster, or MQE implementation
     -> JSON/YAML/text response
```

Debugging query 在下游通过 `DebuggingTraceContext` 收集执行 span；`DebuggingHTTPHandler.transformTrace(...)` 将 span map 转换为 parent-child response tree。

## 5. 本轮确认并修复的真实缺陷

### 5.1 orphan parent span 导致 debug response NPE

原逻辑假定每个 child 的 parent 都存在于当前 trace 集合。对于 orphan parent：
- `spanMap.get(parentId)` 返回 null
- 直接调用 `parentSpan.getChildSpans()`
- debug response 失败

修复后：
- parent 存在时建立 child 关系
- parent 不存在时跳过关系连接
- 当前 span 仍被转换
- 无 root span 的响应允许 `rootSpan == null`

生产文件：

```text
oap-server/server-query-plugin/status-query-plugin/src/main/java/org/apache/skywalking/oap/query/debug/DebuggingHTTPHandler.java
```

### 5.2 tag value 含等号被截断

原实现对 `queryBasicTraces` 与 `queryLogs` 的 comma-separated tags 使用无限制 `split("=")`，例如 `token=left=right` 会丢失第二个等号后的内容。

修复后抽取 `parseTags(...)`：
- 只按第一个等号分隔
- 保留完整 value
- 缺少等号或空 key 抛 `IllegalArgumentException`
- 由 `StatusQueryExceptionHandler` 统一返回 HTTP 400

## 6. 当前已验证能力

### Debugging trace transform
- 正常 root/child 逻辑仍沿用 span map
- orphan parent 不再抛 NPE
- orphan span 不会被静默删除

### 异常映射
`StatusQueryExceptionHandler`：
- `IllegalArgumentException` -> HTTP 400
- 其他异常 -> HTTP 500

### 配置脱敏源码语义
`ServerStatusService.dumpBootingConfigurations(...)`：
- key 命中 masking keyword 时 value 替换为 `******`
- 大小写不敏感
- 嵌套 `Properties` 也处理
- 空配置返回空列表

配置脱敏已由 `ServerStatusServiceTest` 覆盖空配置、provider key、大小写 keyword 和嵌套 Properties；定向 server-core 回归通过。

## 7. 测试覆盖

本轮新增：
- `DebuggingHTTPHandlerTest`
  - orphan parent 不抛异常
  - orphan span 输入的 root response 为空而不是异常
  - tag value 含 `=` 时完整传给 `LogQueryService`
- `StatusQueryExceptionHandlerTest`
  - `IllegalArgumentException` -> 400
  - unexpected exception -> 500
- `ServerStatusServiceTest`
  - 空配置
  - provider key
  - 大小写 keyword mask
  - nested Properties mask

当前测试结果：

```text
DebuggingHTTPHandlerTest: 2/2 PASS
StatusQueryExceptionHandlerTest: 2/2 PASS
ServerStatusServiceTest: 2/2 PASS
status-query-plugin reactor: BUILD SUCCESS
```

## 8. 剩余风险

1. `transformTrace` 对重复 spanId 使用 `Collectors.toMap`，重复 key 可能失败。
2. 其他 debug query 的异常路径是否清理 ThreadLocal 尚未专项验证。
3. alarm/cluster/TTL handler 的 null、远端异常、空节点和 response content type 尚无专属测试。
4. `Step.valueOf`、`TraceState.valueOf`、`QueryOrder.valueOf`、`Order.valueOf` 等非法枚举输入需验证统一 400 映射。
5. `serviceInstance`/`endpoint` 在缺省 service 时调用 `orElseThrow()`，需要确认协议是否保证 service 必填。
6. 缺少 `=`、空 value、空 tag segment 的协议语义虽已转为 `IllegalArgumentException`，但仍需更细粒度 handler 测试确认 HTTP 400 response body。

## 9. 外域关系

- `SW-4C1`：GraphQL resolver 被 DebuggingHTTPHandler 复用，但本域聚焦 debug HTTP 参数和响应边界。
- `SW-4C2`：Debugging Zipkin endpoint 复用 ZipkinQueryHandler。
- `SW-4C3A/B/C`：PromQL、TraceQL、LogQL 不属于本域。
- `SW-5`：storage DAO 是下游实现，不在本域展开。
- `SW-6`：cluster/config provider 影响 status 数据，但本域只审 query boundary。

## 10. 当前结论

SW-4C4 已完成 Pass0 和首轮 Pass2：确认并修复两个真实生产缺陷，建立 debugging trace、异常映射和配置脱敏测试，并通过定向与完整 reactor。当前仍不能标记为完全收敛，下一步优先验证异常清理和 status handler null/remote-error 边界。
