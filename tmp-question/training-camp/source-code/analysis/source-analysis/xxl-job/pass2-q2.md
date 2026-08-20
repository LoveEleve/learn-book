# Pass 2 闭环笔记 XJ-2: Executor 执行主链

## 初始假设
- Executor 侧主线只有 `XxlJobSpringExecutor -> JobThread`，其余都是外围协议细节。

## 验证过程
- `XxlJobSpringExecutor`/`XxlJobExecutor` 负责：初始化 admin 地址、handler 仓库、嵌入式 HTTP server、注册线程、销毁线程，是 executor 的总入口。
- 远程协议入口在 `ExecutorBiz` / `ExecutorBizImpl`：
  - `run(TriggerParam)`：根据 glueType、blockStrategy、已有 `JobThread` 状态决定复用/替换线程，并把 `TriggerParam` 压入 `JobThread` 队列 (`ExecutorBizImpl.java:90-156`)。
  - `kill(KillParam)`：移除并终止 `JobThread` (`ExecutorBizImpl.java:159-169`)。
  - `log(LogParam)`：通过 `XxlJobFileAppender.readLog` 读取本地日志 (`ExecutorBizImpl.java:171-178`)。
- `JobThread` 是真正的执行容器：从队列消费 trigger、调用 handler、记录执行结果、把 `HandleCallbackParam` 推入 `TriggerCallbackThread` (`JobThread.java:207-234`)。
- `TriggerCallbackThread` 负责把执行结果异步回传 Admin：正常批量回调失败则落盘到 `callbacklog`，后台 retry 线程再读文件重试 (`TriggerCallbackThread.java:35-98, 160-255`)。

## 结论
Executor 主链是 **XxlJobExecutor -> ExecutorBizImpl -> JobThread -> TriggerCallbackThread**。`JobThread` 只是执行核心，不代表整个 executor 生命周期；协议入口、阻塞策略、日志读取和 callback 回传都必须放进 XJ-2。