# Pass 2 闭环笔记 Q10: EventObserverRegistry 的观察时序

## 验证过程

- `EventObserverRegistry` 是按名字保存观察者的 registry，`getStateChangeObservers()` 返回新的 List 快照 (`EventObserverRegistry.java:31-63`)。
- `AbstractCircuitBreaker` 的主状态转换只在 CAS 成功后调用 `notifyObservers`：
  - CLOSED → OPEN：先 CAS，再设置恢复时间，再通知
  - OPEN → HALF_OPEN：先 CAS，再通知
  - HALF_OPEN → OPEN/CLOSED：先 CAS，再通知 (`AbstractCircuitBreaker.java:76-132`)
- 因此主转换路径不会通知 CAS 失败的伪事件。但 HALF_OPEN 探测 entry 注册的兜底回调在 `AbstractCircuitBreaker.java:114-118` 中没有检查 `compareAndSet` 返回值，却无条件通知 HALF_OPEN → OPEN；这是一个边界不一致，不能笼统断言所有路径都过滤 CAS 失败。
- `snapshotValue` 只在触发 OPEN 时携带异常比例/异常数/慢调用比例；进入 HALF_OPEN 或 CLOSED 时传 null (`CircuitBreakerStateChangeObserver.java:24-31`)。

## 结论

观察者主要是状态机转换后的事件流，不是转换前的拦截器；主转换方法会过滤 CAS 失败，但 HALF_OPEN 兜底回调存在未检查 CAS 结果的特殊路径，可能广播一个并未真正完成的 HALF_OPEN → OPEN 事件。
