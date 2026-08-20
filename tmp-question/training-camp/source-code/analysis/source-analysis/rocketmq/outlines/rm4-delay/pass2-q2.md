# 闭环笔记 q2: 写路径 — 延迟消息的 topic 重写与到期标记

## 假设
延迟消息落 SCHEDULE_TOPIC_XXXX; 到期时间存 tagsCode。

## 验证过程
- **写前改道** (MessageExtEncoder/发送面): 客户端设 DELAY_TIME_LEVEL 属性 → broker 写前把 topic 改为 SCHEDULE_TOPIC_XXXX + queueId = level-1 (真实 topic/queue 存属性 REAL_TOPIC/REAL_QUEUEID)
- **分发期标记** (CommitLog.doAppend L536-552): 读属性 DELAY_TIME_LEVEL → **topic 是 SCHEDULE 且 level 有效 → tagsCode = computeDeliverTimestamp(level, storeTimestamp) = level 毫秒 + 落盘时间** (L548) — **到期绝对时间存 CQ 的 tagsCode 槽** (8B)
- **computeDeliverTimestamp** (ScheduleMessageService:125-130): 查表; **表缺失 → storeTimestamp+1000** (兜底 1s)
- **钳制**: delayLevel > maxDelayLevel → max (L541-544)
- **投递前**: tagsCode 作为 deliverTimestamp 读出 (L443)

## 代码类型
Implementation (写路径改造)

## 跨域关联
- RM-3 (CQ): tagsCode 槽复用 (到期时间 vs tag 哈希 — 二义性由 topic 区分)
- RM-2 (写链): putMessage 改道

## 结论
延迟消息 = topic 重写 (SCHEDULE_TOPIC_XXXX + level 队列) + tagsCode 存到期绝对时间; 与普通消息的 tag 哈希共用 8B 槽 (topic 区分语义)。
源码位置: CommitLog.java:536-552; ScheduleMessageService.java:125-130
