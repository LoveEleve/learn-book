# RocketMQ-11 重写规划

> 题目：delay level、专用 Topic 和定时扫描怎样把消息重新投递出来
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成“延迟消息从专用形态重新回到普通消息主链”的具体推进专题，而不是 ScheduleMessageService 的定时任务清单

## 1. 读者困惑

- 延迟消息既然已经被改写进 `SCHEDULE_TOPIC_XXXX`，到底是怎么重新回到原 topic/queue 的？
- `delayLevel` 为什么不仅是时间配置，还决定了专用队列映射？
- `ScheduleMessageService` 顺序扫描时在看什么、推进什么、更新什么？
- 为什么“到点重新投递”不能被理解成简单 timer callback？

## 2. 一句话顿悟

**RocketMQ 的延迟重投不是一个单点 timer 回调，而是一条按 delay level 和专用 queue 持续推进的后台主链：先解析延迟级别表，把每个级别映射到专用 queue，再由 `DeliverDelayedMessageTimerTask` 顺序读取、判断是否到点、恢复原消息语义、重新 put 回普通消息主链，并更新对应延迟队列 offset。**

## 3. 总图

```text
parseDelayLevel()
  → delayLevelTable + delayLevel2QueueId()
    → SCHEDULE_TOPIC_XXXX 的各级专用队列
      → DeliverDelayedMessageTimerTask(level, offset)
        → lookMessageByOffset()
          → messageTimeUp() 恢复原消息语义
            → putMessage() 重新投递
              → updateOffset(level, nextOffset)
```

## 4. 关键边界

- 本篇只讲延迟消息如何被重新投递，不回头重讲为什么要先改写成延迟专用形态。
- `delayLevel` 在本篇不只是“几秒几分钟”，还要被理解成“专用队列与扫描任务的分片键”。
- `ScheduleMessageService` 在本篇视为一条持续推进的后台主链，不写成一个孤立定时器对象。
- 不展开 EscapeBridge / async deliver 所有分支细节，只保留主链与关键边界。

## 5. 本轮重写主线

1. 用“消息到点以后，是谁、按什么顺序把它重新投回普通主链”开场。
2. 否定：delay level 只是时间配置、到点只是一个 timer callback、重投不需要维护 offset。
3. 先讲 delay level 与专用 queue 映射。
4. 再讲 DeliverDelayedMessageTimerTask 怎样扫描、判断、恢复消息。
5. 再讲重新 put 和 updateOffset 为什么是同一条推进链。
6. 收网时明确：延迟消息重投本质是“专用轨道上的顺序推进”，不是孤立定时唤醒事件。
