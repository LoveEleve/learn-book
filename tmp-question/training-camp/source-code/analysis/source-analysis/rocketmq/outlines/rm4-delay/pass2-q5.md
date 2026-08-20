# 闭环笔记 q5: 投递模式与 offset 持久化 — sync/async + JSON

## 假设
同步投递等待; 5.x 异步投递 (流控); offset 持久化 10s。

## 验证过程
- **syncDeliver** (L494-502): deliverMessage → get() 等待 → PUT_OK → **updateOffset(level, nextOffset)**; 失败返回 false (重排)
- **asyncDeliver** (L508-527, 5.x enableAsyncDeliver): deliverPendingTable[level] 队列 —
  - **流控**: currentPendingNum > **scheduleAsyncDeliverMaxPendingLimit** → false (重排, 告警)
  - **Blocked**: 队首 need2Blocked → false (阻塞等待)
  - 入队 PutResultProcess (异步结果跟踪) → 完成时 updateOffset (L565)
- **updateOffset** (L117-127): offsetTable[level] + **versionChangeCounter 每 DelayOffsetUpdateVersionStep 递增版本** (状态机版本, 5.x)
- **持久化** (persist L293-296): DelayOffsetSerializeWrapper (offsetTable → JSON) — **flushDelayOffsetInterval=10s** 定时 + 关闭时 (L194); load (L279-280) 恢复
- **loadWhenSyncDelayOffset** (L228-230): 同步复制模式 (DLedger) 恢复专用路径

## 代码类型
Implementation (投递与状态)

## 跨域关联
- RM-12 (HA): loadWhenSyncDelayOffset (DLedger)
- RM-2 (写链): putMessage 投递

## 结论
投递 = 同步等待 / 5.x 异步队列 (流控+Blocked); offset 状态 = 内存表 + 10s JSON 持久化 + 版本计数 (5.x 状态机)。
源码位置: ScheduleMessageService.java:117-127,194,279-296,494-527
