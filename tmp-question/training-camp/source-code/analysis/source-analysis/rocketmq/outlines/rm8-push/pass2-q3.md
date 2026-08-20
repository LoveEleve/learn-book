# 闭环笔记 q3: ProcessQueue — 拉取与消费的缓冲

## 假设
TreeMap 消息缓存; 消费推进; 流控 (缓存上限)。

## 验证过程
- **结构** (ProcessQueue L46-53): msgTreeMap (TreeMap<offset, MessageExt> — **按 offset 有序**) + consumingMsgOrderlyTreeMap (有序消费子集)
- **写入** (putMessage L129): 拉取结果入树; msgCount 计数
- **消费取** (takeMessages L306): 按 batchSize 取连续段
- **推进** (commit L264 / removeMessage L187): 消费成功 → 移除 + **committed offset 推进** (msgAccCount)
- **流控** (getMaxSpan L170): 首尾 offset 跨度 (consumeConcurrentlyMaxSpan=**2000** 默认) — 超限 → 拉取延迟 (q1 的 50ms)
- **缓存阈值** (DefaultMQPushConsumer): pullThresholdForQueue=**1000 条** / pullThresholdSizeForQueue=**100 MiB** — 单队列缓存上限 (L182-217)
- **有序锁** (MessageQueueLock): 有序消费时队列级互斥 (RM-8 有序面)

## 代码类型
Implementation (消息缓冲)

## 跨域关联
- RM-9 (Rebalance): 队列分配 → ProcessQueue 绑定
- RM-3 (存储): 消息结构

## 结论
缓冲 = TreeMap 有序缓存 + 消费推进 + 双阈值流控 (span 2000/条数 1000/字节 100MiB); 拉取-消费解耦。
源码位置: ProcessQueue.java:46-306; DefaultMQPushConsumer.java:176-217
