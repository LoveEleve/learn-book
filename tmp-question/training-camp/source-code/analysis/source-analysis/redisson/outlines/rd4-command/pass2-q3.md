# 闭环笔记 q3: 超时三定时器 — 连接/写/响应 的分层判定

## 假设
RedisExecutor 用三个定时器覆盖执行的三阶段: 连接获取超时 (池满) / 写入超时 (EventLoop 堵) / 响应超时 (服务端无响应)。告警文案各自带专属排障建议。

## 验证过程
- **scheduleConnectionTimeout** (RedisExecutor.java:232-252): retryInterval>0 && attempts>0 时跳过 (交给重试计时); 否则 newTimeout(responseTimeout):
  - L240: connectionFuture.completeExceptionally(CancellationException)
  - L241-245: `RedisTimeoutException "Unable to acquire connection! Increase connection pool size or timeout... after attempt of attempts"`
- **scheduleWriteTimeout** (L254-276): L261-262: writeFuture.cancel(false) → L264-270 文案 **"Command still hasn't been written into connection! Check CPU usage of the JVM. Check that there are no blocking invocations in async/reactive/rx listeners or subscribeOnElements method. Check connection... for TCP packet drops. Try to increase nettyThreads setting. Netty pending tasks: N"** — 排障: 主线程堵/Netty 事件循环卡顿
- **scheduleResponseTimeout**: 写成功后才排 (checkWriteFuture L406 调用) — 服务端响应未回
- 三定时共用 responseTimeout (BaseConfig timeout=3000ms), 但语义阶段不同
- countPendingTasks (L409-418): 遍历所有事件循环累计 pendingTasks — 写超时文案的量化指标

## 代码类型
Implementation (定时分层) — 运营友好: 每失败带可行动建议

## 跨域关联
- RD-1 (连接池) → 池满超时文案指向 pool size
- r28 (RESP) → 响应超时对服务端处理耗时
- 面试点: "Redis 客户端超时有哪些?超时了怎么办"

## 结论
三定时器 = 连接/写/响应 三阶段超时, 同一 responseTimeout 复用; 文案差异是运营设计: 池满→加池, 写堵→查 Netty, 响应→查服务端。写超时的 countPendingTasks 量化 EventLoop 积压。
源码位置: RedisExecutor.java:232-252,254-276,409-418