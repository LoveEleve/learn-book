# 闭环笔记 q3: 投递主循环 — TimerTask + 到期判定 + 重排

## 假设
每级一个定时任务; 扫 CQ → 到期投递 → 未到期重排。

## 验证过程
- **启动** (start L134-153): 每级 schedule(DeliverDelayedMessageTimerTask, **FIRST_DELAY_TIME=1s**) — 18 个定时任务 (每级一个)
- **执行** (executeOnTimeUp L401-485): CQ 迭代 (iterateFrom offset) → 逐条:
  - **tagsCode 无效 (Ext 丢失)** → 重算 (pickupStoreTimestamp + computeDeliverTimestamp, L434-440, "[BUG]" 日志)
  - `deliverTimestamp = correctDeliverTimestamp(now, tagsCode)` (L443)
  - **countdown = deliverTimestamp - now > 0 → 未到期**: scheduleNextTimerTask(currOffset, **DELAY_FOR_A_WHILE=100ms**) + updateOffset (L447-452) — 100ms 后重查
  - 到期 → lookMessageByOffset → messageTimeUp (q4) → sync/asyncDeliver (q5)
  - **投递失败 → 重排 100ms** (L477-481)
  - **RMQ_SYS_TRANS_HALF_TOPIC 校验** (L460-464): 延迟消息真实 topic 是事务半消息 → "[BUG] discard"
- **队尾** → scheduleNextTimerTask(nextOffset, **DELAY_FOR_A_PERIOD=10s**) (L484) — 10s 后下一轮
- **调度器**: deliverExecutorService (每级任务提交)

## 代码类型
Algorithmic (定时扫描)

## 跨域关联
- RM-3 (CQ): iterateFrom 迭代
- RM-10 (事务): TRANS_HALF 校验

## 结论
投递 = 每级独立 TimerTask: 未到期 100ms 重查 (offset 续扫) / 到期投递 / 队尾 10s 重排; 三级延迟 (1s 首启/100ms 等待/10s 周期)。
源码位置: ScheduleMessageService.java:134-153,365-485
