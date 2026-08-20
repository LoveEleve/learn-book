# 闭环笔记 q6: LitePull — 手动拉取消费

## 假设
LitePull = poll 语义 (对照 Kafka); 与 Push 共享 Rebalance 但用户控制节奏。

## 验证过程
- **DefaultLitePullConsumerImpl** (1315): **subscribe/assign 互斥** (SUBSCRIPTION_CONFLICT 异常, L105) — 订阅制 vs 手动分配
- **poll**: 用户拉取节奏 (对照 Push 自动); **AssignedMessageQueue** (L136, 分配结果管理) + **messageQueueLock** (L156)
- **RebalanceLitePullImpl** (180): 共享 RebalanceImpl 核心 (差集/算法) — 无自动拉取
- **API 面**: assign/seek/resume/pause (Kafka consumer 风格) — 手动偏移控制
- **5.x**: POP 模式也支持 LitePull? (标注 — pop 协议与 LitePull 的边界)

## 代码类型
Interface (手动消费)

## 跨域关联
- RM-8 (Push): 对比
- RM-10 (顺序): 有序 LitePull

## 结论
LitePull = poll 手动消费 (assign/seek 控制); 与 Push 共享 Rebalance 核心; subscribe/assign 互斥。
源码位置: DefaultLitePullConsumerImpl.java:93-219; RebalanceLitePullImpl.java
