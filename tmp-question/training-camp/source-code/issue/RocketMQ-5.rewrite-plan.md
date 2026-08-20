# RocketMQ-5 重写规划

> 题目：ConsumeQueue 为什么不是“另一份日志”，而是消费索引桥
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成“统一顺序真相层（CommitLog）为什么仍然不足以直接支撑消费推进，因此 RocketMQ 必须再建立一层按 topic/queue 组织的消费索引桥”的主链专题

## 1. 读者困惑

- CommitLog 已经把消息统一顺序写下来了，为什么 Consumer 还不能直接读它？
- ConsumeQueue 为什么不是“另一份消息日志”，而是另一层角色完全不同的结构？
- `ReputMessageService`、`putMessagePositionInfo`、`ConsumeQueue` 在主链里分别补哪一层缺口？
- 为什么后面的 Pull/ProcessQueue/消费推进都必须先依赖 ConsumeQueue？

## 2. 一句话顿悟

**CommitLog 解决的是“消息如何成为系统统一事实”，ConsumeQueue 解决的是“Consumer 怎样按 topic/queue 和 offset 去推进这份事实”；RocketMQ 不是重复存一份消息，而是把统一顺序真相翻译成消费侧可按队列定位的索引桥。**

## 3. 总图

```text
CommitLog 已有统一顺序日志
  → ReputMessageService 顺序扫描新增日志
    → putMessagePositionInfo(...)
      → ConsumeQueue 为 (topic, queueId) 建 offset 索引
        → Consumer / Pull 侧按队列和位点推进
```

## 4. 关键边界

- 本篇只讲 ConsumeQueue 为什么必须存在以及它如何作为索引桥出现，不展开 Pull/Push/Rebalance 全部消费细节。
- 不把 ConsumeQueue 写成“第二份消息正文存储”；它存的是消费定位信息，不是重新承载消息真相。
- `ReputMessageService` 在本篇只讲“它怎样把 CommitLog 新增事实翻译到消费索引层”，不展开其所有后台线程实现枝节。
- IndexFile 只作为“另一类索引消费者”点到，不抢主题。

## 5. 本轮重写主线

1. 用“既然 CommitLog 已经有了，为什么还不能直接消费”开场。
2. 否定：Consumer 直接扫 CommitLog 就够、ConsumeQueue 是重复存储、Broker 收到消息时就能顺手完成消费视图。
3. 先讲 CommitLog 是统一真相层，但不是消费推进层。
4. 再讲 ReputMessageService 怎样把新增日志翻译成队列索引。
5. 再讲 ConsumeQueue 为什么把“全局顺序事实”变成“按 topic/queue/offset 推进”的消费桥。
6. 收网时明确：下一篇再进入 Pull/ProcessQueue，回答消费者怎样真正沿着这座索引桥往前走。
