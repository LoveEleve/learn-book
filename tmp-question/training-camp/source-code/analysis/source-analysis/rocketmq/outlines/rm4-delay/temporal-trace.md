# RM-4 延迟消息 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | 延迟消息初版: SCHEDULE_TOPIC_XXXX + 每级 TimerTask (FIRST_DELAY_TIME 1s) + tagsCode 存到期时间 + messageTimeUp 还原 — 核心机制至今未变 |
| 4.x | flushDelayOffsetInterval 持久化 (10s JSON); correctDeliverTimestamp 校正 (时钟异常兜底) |
| **5.0** | **enableScheduleAsyncDeliver** (异步投递: deliverPendingTable + 流控 2000 + Blocked 语义); BrokerMetricsManager 指标 (LABEL_MESSAGE_TYPE); versionChangeCounter 状态机版本 (DelayOffsetUpdateVersionStep) |
| 5.x | loadWhenSyncDelayOffset (DLedger/同步复制恢复专用); TIMER_DELAY_SEC/TIMER_DELIVER_MS 属性清理 (Timer 消息交叉) |

## 痕迹证据

- ScheduleMessageService.java:43-49: 五个延迟常量 (1s/100ms/10s/5s/10ms)
- ScheduleMessageService.java:74: delayLevelTable (ConcurrentSkipListMap — 4.x 起可配置演进痕迹)
- ScheduleMessageService.java:125-130: computeDeliverTimestamp (表缺失兜底 +1000)
- ScheduleMessageService.java:434-440: Ext 丢失 tagsCode 重算 ("[BUG]" 日志)
- ScheduleMessageService.java:508-527: async 流控/Blocked 注释 (5.x)
- MessageStoreConfig.java:224: 18 级默认字符串 (可配置)
- CommitLog.java:536-552: 分发期到期标记 (topic 区分语义)

## 推断标注

- "3.x 初版" — RocketMQ 公知版本线 (标注)
- "4.x 持久化/校正" — 特性年代推断 (标注)
- "5.0 async/指标/版本" — 与 5.x 架构同代推断 (标注)
- 未做 git 考古, 版本线为代码结构推断, 已逐条标注
