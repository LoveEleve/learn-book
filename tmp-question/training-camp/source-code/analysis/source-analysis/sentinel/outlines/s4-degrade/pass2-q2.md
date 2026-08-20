# Pass 2 闭环笔记 Q2: AbstractCircuitBreaker 的状态机

## 验证过程

- 初始状态为 `CLOSED`，`tryPass` 直接返回 true (`AbstractCircuitBreaker.java:42-61`)。
- `CLOSED → OPEN` 使用 `AtomicReference.compareAndSet`，成功后设置 `nextRetryTimestamp = now + recoveryTimeoutMs` 并通知观察者 (`AbstractCircuitBreaker.java:76-87`)。
- `OPEN` 状态只有在恢复时间到达后才允许尝试 `OPEN → HALF_OPEN`；CAS 成功者成为唯一探测请求，其他请求仍返回 false (`AbstractCircuitBreaker.java:48-57, 89-112`)。
- 转入 HALF_OPEN 时，断路器给当前 entry 注册 `whenTerminate` 回调：如果后续槽又把该 entry 标成 block，则把 HALF_OPEN 回退为 OPEN。这是为了覆盖“探测请求通过熔断槽、但被后续规则挡住”的情况 (`AbstractCircuitBreaker.java:95-109`)。
- 探测完成后由具体实现调用：
  - `fromHalfOpenToClose()`：CAS 成功，重置统计，通知 CLOSED
  - `fromHalfOpenToOpen(snapshot)`：CAS 成功，重设恢复时间，通知 OPEN (`AbstractCircuitBreaker.java:114-132`)

## 结论
状态机不是定时线程驱动，而是请求访问时惰性推进：CLOSED 放行，OPEN 等待恢复时间，唯一 CAS 获胜者进入 HALF_OPEN 探测，探测结果再决定 CLOSED 或 OPEN。