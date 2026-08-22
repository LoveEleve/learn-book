# Kafka-38 重写规划

> 题目：一条消息的可靠之旅——acks=all、ISR、Purgatory、事务 marker 的共同边界
> 状态：计划 D 篇（跨域总结）；作为全系列收束篇，串联 acks=all / ISR / Purgatory / 事务 marker / read_committed。

## 1. 读者困惑

- acks=all、ISR、Purgatory、事务 marker 是怎么串起来的？
- 一条消息从 Producer 到 Consumer 可见，经历了哪些可靠性层？
- 事务与幂等在整条链里各自承担什么？

## 2. 一句话顿悟

**一条消息从 Producer 发出到 Consumer 可见，经过幂等校验（seq/epoch）、acks=all 等 ISR 确认、Purgatory 等待满足、以及事务 marker 扇出与 read_committed 过滤，形成一条可靠链路。**

## 3. 失败方案推演

- 缺 ISR 确认 → acks=all 失真
- 缺 Purgatory 等待 → producer 过早收到成功
- 缺 marker 扇出 → 事务不可见或 abort 数据可见

## 4. 误解清单

- "可靠性就是 acks=all"：还有幂等、事务、ISR 维护
- "read_committed 只看 COMMIT marker"：还要 LSO + txnindex
- "ISR 是静态的"：需要 leader 与 controller 持续维护

## 5. 证据清单

- 交叉引用各篇：Kafka-10/9/33/35。
- 本篇为总结篇，主要依赖前序证据。

## 6. 版本边界与字数预算

- 基线 v4.x；目标 4000~7000 字。