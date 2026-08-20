# Pass 2 闭环笔记 Q8: 三套日志体系分工 — RecordLog / EagleEye / EagleEyeLogUtil

## 初始假设
- Sentinel 只有一套日志(RecordLog 封装的 JUL)。
- 实际: **两套引擎 + 一个桥接**,分工明确。

## 验证过程
- **引擎 1 — log/ 包 (14 文件, RecordLog 业务日志)**: `RecordLog.java:31-39` — LoggerSpiProvider 先找用户自定义 Logger(SPI),无则默认 JUL(JavaLoggingAdapter)。用于启动/警告/异常等**运行期事件**。
- **引擎 2 — eagleeye/ 包 (15 文件, 独立统计日志引擎)**: EagleEye + StatLogger/StatEntry/TokenBucket/StatLogController/EagleEyeRollingFileAppender/daemon — 自研**批量统计 + 滚动文件**框架(非 JUL)。
- **桥接 — slots/logger/EagleEyeLogUtil**: `EagleEyeLogUtil.java:32` `EagleEye.statLoggerBuilder("sentinel-block-log")` 建静态 StatLogger;`log()` 写入。LogSlot 只依赖这个桥接,不直接碰 EagleEye 细节(LogSlot.java:40)。
- 分工结论: RecordLog = 事件日志(可替换);EagleEye = 高频统计日志(block 事件,批量落盘)。

## 代码类型
- Glue(日志引擎 + 桥接)

## 跨域关联
- S-3~S-6: 所有 block 事件日志面(eagleeye "sentinel-block-log")
- S-5 统计: StatLogger/StatEntry 与指标统计的相似性(都是高吞吐写入)

## 结论
三套分工: log/(RecordLog, SPI 可替换默认 JUL)承载事件日志;eagleeye/(自研滚动+批量统计引擎)承载高频 block 日志;EagleEyeLogUtil 是 LogSlot→EagleEye 的静态桥接("sentinel-block-log")(RecordLog.java:31-39 + EagleEyeLogUtil.java:32 + LogSlot.java:40)。