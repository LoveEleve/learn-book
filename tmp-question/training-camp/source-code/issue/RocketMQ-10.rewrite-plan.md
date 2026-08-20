# RocketMQ-10 重写规划

> 题目：延迟消息为什么不是 Broker 睡一会再发
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成“延迟消息为什么必须先改写主链落点，再由定时服务重投递，而不是简单 sleep 后发送”的专题，而不是 delay level 配置说明文

## 1. 读者困惑

- 为什么 RocketMQ 的延迟消息不能简单理解成“Broker 收到消息后睡一会再投递”？
- delay level、专用 topic/queue 和定时扫描分别在补哪一层缺口？
- 延迟消息为什么不是脱离普通消息主链的独立系统，而是建立在普通存储主链上的再投递机制？
- 为什么消息需要先以特殊形态落盘，而不是到时间点时现算现发？

## 2. 一句话顿悟

**RocketMQ 的延迟消息不是在 Broker 里把线程 sleep 到时间点，而是先把消息改写到 `SCHEDULE_TOPIC_XXXX` 的专用队列里落入普通存储主链，再由 `ScheduleMessageService` 按 delay level 和队列映射定时扫描、恢复原 topic/queue、重新投递；它本质上是“先以延迟形态落盘，再转正投递”的两段式主链。**

## 3. 总图

```text
Producer 发送延迟消息
  → Broker 改写 topic/queue 为延迟专用形态
    → 先按普通消息主链写入 CommitLog / ConsumeQueue
      → ScheduleMessageService 按 delay level 扫描专用队列
        → 到点后恢复原 topic/queue
          → 重新投递为普通消息
```

## 4. 关键边界

- 本篇只讲延迟消息为什么必须“先存后投递”，不展开事务消息和顺序消息。
- 不把 delay level 写成简单配置项列表，而是解释它为什么对应专用队列和扫描调度。
- `ScheduleMessageService` 在本篇视为“延迟形态转正”的后台推进服务，不写成定时任务 API 说明文。
- 延迟消息不是独立存储系统，它仍然建立在普通 Broker 存储主链之上。

## 5. 本轮重写主线

1. 用“为什么不能 sleep 一会再发”开场。
2. 否定：延迟消息是 Broker 定时器直接缓存消息、到点现发；delay level 只是时间配置；延迟消息不经过普通 CommitLog。
3. 先讲延迟消息必须先落成特殊形态消息。
4. 再讲 `SCHEDULE_TOPIC_XXXX` / delayLevel2QueueId 为什么存在。
5. 再讲 `ScheduleMessageService` 如何按专用队列扫描并恢复原 topic/queue。
6. 收网时明确：延迟消息不是独立系统，而是普通消息主链上的“延迟形态 -> 普通形态”再投递专题。
