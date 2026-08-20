# Pass 2 闭环笔记 XJ-5: 失败重试与阻塞策略的两条链

## 初始假设
- XXL-Job 的失败重试就是 `executorFailRetryCount`，阻塞策略只是一个开关。

## 验证过程
- `ExecutorBizImpl.run` 在把 trigger 压入 `JobThread` 前，先处理 `ExecutorBlockStrategyEnum`：
  - `DISCARD_LATER`：若 `jobThread.isRunningOrHasQueue()`，直接返回失败，不入队
  - `COVER_EARLY`：若旧线程还在跑/有队列，记录 `removeOldReason`，旧线程作废，后续重建新线程
  - 其他（默认 `SERIAL_EXECUTION`）：排队等待 (`ExecutorBizImpl.java:120-149`)。
- 这是**执行前阻塞策略**，目的是控制同一个 job 的本地并发与排队，不是“失败重试”。
- 真正的执行结果回传由 `JobThread` → `TriggerCallbackThread.pushCallBack` 完成；`TriggerCallbackThread` 正常批量回调失败时，会把回调参数序列化写入 `callbacklog` 文件，后台 retry 线程定期读文件再重试 (`TriggerCallbackThread.java:35-121, 160-255`)。
- 所以至少有两条“失败处理”链：
  1. executor 入口的 block strategy（是否允许入队/替换旧线程）
  2. callback 回传失败后的文件持久化与重试

## 结论

XJ-5 不能写成单一的“重试机制”。正确边界是：阻塞策略发生在 trigger 入队前，决定是否排队/覆盖/丢弃；真正的回传重试则发生在 callback 阶段，由 `TriggerCallbackThread` 和 `callbacklog` 文件完成。