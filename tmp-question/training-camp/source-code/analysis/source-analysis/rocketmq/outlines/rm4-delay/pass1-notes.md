# RM-4 延迟消息 — Pass 1 探索笔记

> 域: RM-4 延迟消息 | 🟡 B 方案 | 2026-08-14
> 源码: broker/schedule/ScheduleMessageService.java (851) + DelayOffsetSerializeWrapper (45) | RocketMQ 5.3.1

## 调用图

```
写入面 (CommitLog.putMessage → MessageExtEncoder):
delayLevel 解析 → 原 topic 改 SCHEDULE_TOPIC_XXXX + queueId = delayLevel-1 (MessageExtEncoder L534)
  → 消息带 DELAY 属性 (REAL_TOPIC/REAL_QUEUEID) 落盘到延迟 topic

调度面 (ScheduleMessageService):
start (L134) → 每级 schedule(DeliverDelayedMessageTimerTask, FIRST_DELAY_TIME=1s)
DeliverDelayedMessageTimerTask (L365): run →
  读 offsetTable[level] → CQ 定位 → 逐条:
    deliverTimestamp (tagsCode 存到期时间) → 未到期 → correctDeliverTimestamp 校正 → 重排 (DELAY_FOR_A_SLEEP=10ms)
    到期 → messageTimeUp (重建 MessageExtBrokerInner: 还原 REAL_TOPIC/REAL_QUEUEID + 清 DELAY 属性)
      → putMessage 原 topic → 成功 updateOffset → 继续
    → 队列尾 → 重排 (DELAY_FOR_A_PERIOD=10s)

持久化面:
offsetTable (level → offset) → DelayOffsetSerializeWrapper (JSON) → flushDelayOffsetInterval=10s (config)
  每级一个延迟 topic 队列 (queueId = level-1); deliverPendingTable (5.x async)
```

## 基本元素分解

1. **延迟等级表**: 配置字符串 (18 级默认) → delayLevelTable (level → ms) + maxDelayLevel
2. **写路径改造**: 延迟消息 topic 重写 (SCHEDULE_TOPIC_XXXX + queueId)
3. **定时投递**: 每级一个 TimerTask (首次 1s 后启动)
4. **到期判定**: tagsCode 携带 deliverTimestamp
5. **消息还原**: messageTimeUp (REAL_TOPIC 属性还原)
6. **offset 推进**: updateOffset + 持久化 (10s JSON)
7. **校正**: correctDeliverTimestamp (时钟/异常补偿)
8. **5.x async**: enableAsyncDeliver + deliverPendingTable

## 标记问题 (20 问)

1. 18 级怎么配置? (字符串 + s/m/h/d 单位表)
2. maxDelayLevel 怎么定? (解析时取最大)
3. 延迟消息写哪? (SCHEDULE_TOPIC_XXXX + queueId=level-1)
4. 到期时间存哪? (tagsCode? DELAY 属性?)
5. 定时任务结构? (每级一个 TimerTask, 1s 首启)
6. 未到期怎么办? (重排定时)
7. correctDeliverTimestamp 干什么? (时间校正)
8. messageTimeUp 还原什么? (REAL_TOPIC/REAL_QUEUEID)
9. offset 推进/持久化? (updateOffset + JSON 10s)
10. 5.x async deliver? (deliverPendingTable/PutResultProcess)
11. 延迟消息的消费? (SCHEDULE_CONSUMER_GROUP 内部消费?)
12. 与事务消息交互? (PREPARED 面)
13. 重启恢复? (load/loadWhenSyncDelayOffset)
14. 大量延迟消息的内存? (每级一个线程?)
15. 延迟消息的 tag 过滤? (还原后原 tag)
16. 时钟回拨处理? (correctDeliverTimestamp)
17. 消息过期检查? (投递时再验)
18. enableAsyncDeliver 差异? (同步 vs 异步投递)
19. 指标面? (BrokerMetricsManager)
20. 测试面? (ScheduleMessageServiceTest)

## 时空溯源 (代码内痕迹)

- 3.x: 延迟消息初版 (SCHEDULE_TOPIC_XXXX + 每级 TimerTask) — 4.x 前定型
- 演进: flushDelayOffsetInterval 持久化; correctDeliverTimestamp 校正; 
- 5.x: enableAsyncDeliver (异步投递) + deliverPendingTable + BrokerMetricsManager 指标 + loadWhenSyncDelayOffset (同步复制恢复)

## 大域拆分判断

851 行单类 — 不拆 (🟡 B, 6 闭环)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "SCHEDULE_TOPIC_XXXX/延迟等级可配置 (ConcurrentSkipListMap 非硬编码 18)" | messageDelayLevel 默认字符串 "1s 5s ... 2h" 18 级 (MessageStoreConfig:224); delayLevelTable = ConcurrentSkipListMap (L74) | **接受** ✅ (可配置 18 级) |
| "5.x 位置: broker/schedule" | ScheduleMessageService 在 broker 模块 (非 store) | **接受** ✅ |
| 数字: flushDelayOffsetInterval | 10s (MessageStoreConfig, L224 后) | **补充** ✅ |
| 数字: FIRST_DELAY_TIME | 1s (L45); DELAY_FOR_A_SLEEP=10ms; DELAY_FOR_A_PERIOD=10s | **补充** ✅ |
