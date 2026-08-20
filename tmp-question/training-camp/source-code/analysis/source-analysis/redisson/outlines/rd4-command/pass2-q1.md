# 闭环笔记 q1: 异步执行管线 — async() 汇聚 → RedisExecutor 链

## 假设
命令执行汇聚于 CommandAsyncService.async(), 它做命令适配 (resp3+SORT_RO) + 缓存钩子, 然后交给 new RedisExecutor().execute() 完成借连接→发送→响应→归还全链。

## 验证过程
- `async(boolean readOnlyMode, NodeSource, Codec, command, params, ignoreRedirect, noRetry)` (CommandAsyncService.java:690-731) — 全 read/write/eval 汇聚点:
  - L692: `cmnd = getServiceManager().resp3(command)` — RESP2/3 命令版本适配
  - L694-714: SORT_RO 降级探测 (readOnly + SORT + SORT_RO_SUPPORTED): 支持 → SORT_RO; `ERR unknown command` → 置 false + 递归 async 降级 SORT (L705-708). **AtomicBoolean SORT_RO_SUPPORTED (L688) = 能力缓存**
  - L717-725: Client-Side Caching 失效钩子: !readOnly + hasCachingInstances → 取 String param → 完成时 evictClientSideCaching(name)
  - L728-731: `new RedisExecutor<>(...).execute()`
- RedisExecutor.execute() 管线 (RedisExecutor.java:122-230):
  - L128-130 addFuture 追踪 / L132 shutdown 检查
  - L139 getCodec / L142 getConnection(attemptPromise)
  - L143-167 mainPromiseListener 取消处理 (blocking 命令 forceFastReconnectAsync L162)
  - L177 retryInterval = retryStrategy.calcDelay(attempt)
  - L179-181 scheduleRetryTimeout + scheduleConnectionTimeout
  - L183-213 connectionFuture 完成 → sendCommand → scheduleWriteTimeout → writeFuture listener
  - L215-224 attemptPromise 完成 → releaseConnection + checkAttemptPromise
- 数据流: async() 适配 → RedisExecutor 借连接 → sendCommand → 响应 → releaseConnection (归还池)

## 代码类型
Glue (执行装配) — 命令层从"结构 API"到"网络发送"的桥

## 跨域关联
- RD-3 (codec) → getCodec + codec 进 CommandData
- RD-1 (连接池) → getConnection/releaseConnection
- RD-6 (缓存失效) → evictClientSideCaching 钩子
- r28 (RESP) → resp3 适配服务端双协议

## 结论
命令执行 = 三层装配: async() 适配 (resp3/SORT_RO/缓存钩子) → RedisExecutor 连接链路 (借→发→响→还) → CompletableFuture 回调和。单命令执行是后续重试/批处理的最小单元。
源码位置: CommandAsyncService.java:690-731, RedisExecutor.java:122-230