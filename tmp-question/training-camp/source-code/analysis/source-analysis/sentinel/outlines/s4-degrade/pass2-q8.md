# Pass 2 闭环笔记 Q8: whenTerminate 回调与熔断统计时序

## 验证过程

- `AbstractCircuitBreaker.fromOpenToHalfOpen(context)` 在探测请求进入 HALF_OPEN 时，给当前 entry 注册 `whenTerminate` 回调 (`AbstractCircuitBreaker.java:95-109`)。
- 这个回调不是做正常统计，而是兜底：如果探测请求最终被后续规则标记成 block，则把 HALF_OPEN 回退为 OPEN，并通知观察者。
- 正常统计入口仍然在 degrade slot 的 exit 侧：
  - `DegradeSlot.exit` / `DefaultCircuitBreakerSlot.exit`
  - 当前 entry 没有 blockError 时，遍历 `circuitBreaker.onRequestComplete(context)` (`DegradeSlot.java:62-80`, `DefaultCircuitBreakerSlot.java:72-96`)
- 所以时序是：
  1. HALF_OPEN 探测放过请求
  2. 若后续规则 block，whenTerminate 先把状态回退 OPEN
  3. 若请求真正完成且未 block，exit 侧才调用 `onRequestComplete`
  4. 具体 breaker 根据 error/rt 决定 HALF_OPEN → CLOSED 或 HALF_OPEN → OPEN

## 结论
`whenTerminate` 不是熔断器的主统计入口，而是 HALF_OPEN 探测的安全钩子；真正的异常/慢调用统计仍在 degrade slot 的 exit 侧 `onRequestComplete(context)` 完成。