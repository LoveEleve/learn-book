# SW-4C4 Status/debug/ops query — Pass 0 发现

> 模块: `oap-server/server-query-plugin/status-query-plugin`
> 核心依赖: `server-core`、`query-graphql-plugin`、`zipkin-query-plugin`、`server-alarm-plugin`
> 日期: 2026-08-18

## 1. 域定位

`SW-4C4` 是 OAP 的 status/debug/ops query compatibility 层，实际模块为：

```text
oap-server/server-query-plugin/status-query-plugin/
```

它不是单一的 status endpoint，而是四条运维查询链：

1. server booting configuration dump
2. cluster / TTL / alarm status
3. debugging query（MQE、trace、Zipkin、topology、log）
4. debugging trace instrumentation and response transformation

## 2. 模块注册链

```text
StatusQueryModule
  -> StatusQueryProvider
     -> prepare(): AlarmStatusQueryService
     -> start(): HTTPHandlerRegister
        -> DebuggingHTTPHandler
        -> TTLConfigQueryHandler
        -> ClusterStatusQueryHandler
        -> AlarmStatusQueryHandler
```

Provider 只声明依赖 `CoreModule`，所有 handler 都以 GET 注册。

## 3. Endpoint 清单

### 3.1 配置与 status

- `/status/config/ttl`
- `/status/cluster/nodes`
- `/status/alarm/rules`
- `/status/alarm/{ruleId}`
- `/status/alarm/{ruleId}/{entityName}`
- `/debugging/config/dump`

### 3.2 Debugging MQE

- `/debugging/query/mqe`

### 3.3 Debugging trace / Zipkin

- `/debugging/query/trace/queryBasicTraces`
- `/debugging/query/trace/queryTrace`
- `/debugging/query/zipkin/api/v2/traces`
- `/debugging/query/zipkin/api/v2/trace`

### 3.4 Debugging topology

- `/debugging/query/topology/getGlobalTopology`
- `/debugging/query/topology/getServicesTopology`
- `/debugging/query/topology/getServiceInstanceTopology`
- `/debugging/query/topology/getEndpointDependencies`
- `/debugging/query/topology/getProcessTopology`

### 3.5 Debugging logs

- `/debugging/query/log/queryLogs`

## 4. 主链

```text
HTTPHandlerRegister
  -> StatusQueryProvider handlers
     -> DebuggingHTTPHandler / status handler
        -> GraphQL resolver/query service or status service
           -> server-core query service
              -> storage/cluster/alarm implementation
        -> YAML/JSON response
```

`DebuggingHTTPHandler` 还会创建 `DebuggingTraceContext`，把 query service 的内部执行 trace 转换为 debugging response。

## 5. 当前源码能力

### 5.1 配置 dump

`ServerStatusService.dumpBootingConfigurations(...)`：
- 读取 booting module configurations
- 展开 provider/property
- 对配置 key 命中 masking keyword 的值输出 `******`
- 支持嵌套 `Properties`
- 空或未 booted 配置返回空 `ConfigList`

### 5.2 Debugging 参数转换

`DebuggingHTTPHandler` 负责：
- service/service-instance/endpoint/process ID 构造
- service layer 到 normal/non-normal 映射
- duration 的 start/end/step/coldStage 组装
- trace/log/topology/MQE 查询条件组装
- comma-separated tags、keywords、exclude keywords 解析
- response 中 debugging trace 的 parent-child 结构转换

### 5.3 异常映射

`StatusQueryExceptionHandler` 统一处理 handler 异常：
- `IllegalArgumentException` -> HTTP 400
- 其他异常 -> HTTP 500
- 使用 exception message 作为 text body

## 6. 首轮质疑点

### Q1: Debugging tag parser 是否正确处理值中包含 `=` 的 tag

当前代码使用 `t.split(Const.EQUAL)` 后直接取 `[0]`、`[1]`。需要确认：
- value 中含 `=` 是否被截断
- 无 `=` 时是否触发数组越界
- 空 key/value 是否应该拒绝或保留

该模式同时存在于：
- `/debugging/query/trace/queryBasicTraces` 的 `tags`
- `/debugging/query/topology/...` 仅 services 使用 comma split
- `/debugging/query/log/queryLogs` 的 `tags`

### Q2: service instance / endpoint 参数在 service 缺省时是否会错误抛异常

当前 `queryBasicTraces` 和 `queryLogs` 使用 `serviceId.orElseThrow()` 构造 instance/endpoint ID。需要确认 HTTP API 是否保证 service 必填；若不是，当前行为可能是 500 或被映射为 400，且错误信息不够明确。

### Q3: trace context 是否始终清理

Zipkin 两个 debug endpoint 使用 `try/finally` 清理 `DebuggingTraceContext`；其他 query path 依赖下游 query service 自身。需要检查：
- `DebuggingTraceContext` 的 ThreadLocal 清理策略
- `transformTrace` 对 null/重复 spanId/缺失 parent 的处理
- 异常时是否残留 ThreadLocal

### Q4: 配置脱敏是否会泄漏 secret

需要验证：
- 默认 keyword 是否大小写不敏感
- keyword 空项行为
- 嵌套 Properties 是否完全脱敏
- provider/module 名称和 property key 是否可能包含 secret value
- `dumpConfigurations` 是否同时支持 text/json 序列化且不泄漏原始值

### Q5: status handler 的响应和异常语义是否一致

需要验证：
- alarm service 返回 null 或抛异常时的状态码
- cluster nodes 空列表是否返回稳定 JSON
- TTL 返回 null 时的响应行为
- status endpoint 是否应强制 GET 且无认证边界（认证不在本模块声明）

### Q6: query 参数枚举/时间格式错误是否统一返回 400

高风险调用包括：
- `Step.valueOf(step)`
- `TraceState.valueOf(traceState)`
- `QueryOrder.valueOf(queryOrder)`
- `Order.valueOf(queryOrder)`
- `Layer.nameOf(serviceLayer)`
- malformed start/end/step

需要确认 `IllegalArgumentException` 是否都被 Armeria annotation exception handler 捕获。

### Q7: comma-separated 参数是否安全

需要验证：
- `services` 空字符串
- `tags` 空字符串
- tag 中多余 `=`
- keyword 列表空项
- comma、equal 常量是否与实际协议一致

## 7. 测试现实

Pass0 发现 `status-query-plugin` 当前没有 `src/test` Java 测试目录，至少没有被现有文件盘点发现的专属测试。

因此本域的首要任务不是扩大 endpoint 数量，而是先为高风险纯转换/边界建立 harness：
- `ServerStatusService` 配置脱敏
- `DebuggingHTTPHandler` tag/ID/enum 参数边界
- `StatusQueryExceptionHandler` 状态码映射
- status handler 的 service 委托

## 8. 外域关系

- `SW-4C1`：GraphQL resolver 是 DebuggingHTTPHandler 的下游适配来源，但本域聚焦 debug HTTP compatibility。
- `SW-4C2`：Zipkin query handler 被 debug Zipkin endpoint 复用。
- `SW-4C3*`：PromQL/TraceQL/LogQL 不属于本域。
- `SW-5`：storage DAO 是 status/debug 查询的下游，不在本 Pass 展开。
- `SW-6`：cluster/config provider 影响 status 结果，但本域只审 query boundary。

## 9. Pass0 结论

本域边界已确认，且存在明显测试空洞。首轮暂不宣称生产缺陷；下一步优先审计 Q1/Q2/Q3/Q4，并以可观察的测试失败证明问题，而不是仅凭代码气味下结论。
