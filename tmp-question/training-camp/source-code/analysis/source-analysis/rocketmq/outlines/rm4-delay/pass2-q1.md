# 闭环笔记 q1: 延迟等级表 — 可配置 18 级

## 假设
延迟等级 = 配置字符串解析; 非硬编码。

## 验证过程
- **配置** (MessageStoreConfig:224): `messageDelayLevel = "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h"` — **18 级字符串可配置** (执行计划"18 级"接受但非硬编码)
- **解析** (parseDelayLevel L299-325): 空格分割 → 单位表 (s/m/h/d → ms) → **level = i+1** → delayLevelTable (ConcurrentSkipListMap, level → ms) + maxDelayLevel 跟踪
- **越界钳制** (CommitLog.doAppend L541-544): 客户端 DELAY_TIME_LEVEL > maxDelayLevel → **钳制到 max** (防超配延迟)
- **等级↔队列**: delayLevel2QueueId = level-1 (L102) / queueId2DelayLevel = queueId+1 (L98)
- **enableAsyncDeliver**: 配置时每级预建 deliverPendingTable 队列 (L320-322)

## 代码类型
Implementation (配置解析)

## 跨域关联
- RM-3 (分发): doAppend 的 tagsCode 计算消费此表
- RM-5 (Broker): 配置面

## 结论
延迟等级 = 配置字符串 (默认 18 级) → 等级表; 越界钳制; 等级↔延迟队列一一对应。
源码位置: MessageStoreConfig.java:224; ScheduleMessageService.java:74,98-102,299-325
