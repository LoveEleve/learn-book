# 闭环笔记 q2: 重试协议 — scheduleRetryTimeout 的递归与边界

## 假设
重试不是 for 循环而是"定时器驱动递归": scheduleRetryTimeout 每次到期决定是否 attempt++ 再调 execute()。连接失败/写失败/响应超时三种情况重试策略不同。

## 验证过程
- RedisExecutor.execute() 每次尝试全新执行 (L122): 失败 → 上层调 execute() 再来一轮
- scheduleRetryTimeout (L278-375):
  - L279: `retryInterval==0 || attempts==0 → return` (不重试)
  - L283-303 **blocking 命令特殊路径**: 连接未得 + 是 blocking 命令 → attempt<attempts → attempt++ 重排 (L290-293); 到顶 → RedisTimeoutException (L295-302)
  - L305-340 普通路径: connectionFuture.completeExceptionally → 若失败 / 连接已得但写未完成 → attempt==attempts 则超时 (L314-328), 否则 attempt++ (L330) 重排
  - L359-371 **递归 execute()**: `attemptPromise.completeExceptionally(CancellationException); attempt++; execute()` — 新轮次完整重跑
- 触发分类:
  - **连接失败** → 可重试 (递归 execute)
  - **写失败** (checkWriteFuture L387-407) → `WriteRedisConnectionException` whyComplete (L394) → 可重试
  - **响应超时** (scheduleResponseTimeout) → 超时异常 (响应没来, 不重试 — 命令已在服务端执行可能成功)
- **为什么续期用 noRetry**: evalWriteNoRetryAsync (L489-492) noRetry=true → RedisExecutor 内 RetryAttempts 归零? (锁续期脚本幂等, 不应放大重试)

## 代码类型
Implementation (重试协议) — 高价值: 有界重试 + 类型化失败分路

## 跨域关联
- RD-1 (connect 重试) → 两层重试 (connect 级 5 次 vs command 级 4 次)
- BaseConfig (retryAttempts=4, EqualJitter) → 消费点
- RD-2 (锁续期) → evalWriteNoRetry 消费

## 结论
命令重试 = 定时器驱动递归: 连接/写失败可重试 (attempt++ 重跑 execute), 响应超时不重试 (服务端可能已执行)。noRetry 用于续期等幂等场景防放大。两级重试对比: connect 级 5 次 (建链) vs command 级 4 次 (命令)。
源码位置: RedisExecutor.java:122,278-375,387-407; CommandAsyncService.java:489-492