# Pass 2 闭环笔记 XJ-7: 日志的三条边界

## 初始假设
- XXL-Job 的日志就是 `XxlJobFileAppender` 一个类负责写和清理。

## 验证过程
- executor 本地运行日志：`XxlJobFileAppender`
  - `initLogPath` 初始化 `logBasePath/gluesource`
  - `makeLogFileName(triggerDate, logId)` 生成 `yyyy-MM-dd/<logId>.log`
  - `appendLog` 追加写文件，`readLog` 支持分页读取 (`XxlJobFileAppender.java:22-143`)。
- executor 本地清理线程：`JobLogFileCleanThread`
  - 每天扫描一次 `logBasePath`
  - 仅清理日期目录，保留天数小于 3 天直接不启清理 (`JobLogFileCleanThread.java:17-79`)。
- callback 失败文件：`TriggerCallbackThread`
  - 写到 `callbacklog/xxl-job-callback-{x}.log`
  - 内容是序列化后的 `HandleCallbackParam` 列表，不是任务输出日志 (`TriggerCallbackThread.java:202-255`)。
- admin 侧日志/报表：`XxlJobLog`、`XxlJobLogGlue`、`XxlJobLogReport` + DAO + `JobLogReportHelper`
  - 这是数据库持久化与报表聚合，不是 executor 文件滚动。
- `ExecutorBizImpl.log(LogParam)` 通过 remoting 读取的是 executor 本地 `XxlJobFileAppender.readLog`，再包装成 `LogResult` 返回 (`ExecutorBizImpl.java:164-178`)。

## 结论

XXL-Job 的“日志”至少有三条边界：
1. executor 运行日志文件 (`XxlJobFileAppender`)
2. callback 失败重试文件 (`callbacklog`)
3. admin 数据库日志与报表 (`XxlJobLog*`)

XJ-7 必须把这三条线分开写，否则很容易把文件滚动、失败回传和报表聚合混成一类。