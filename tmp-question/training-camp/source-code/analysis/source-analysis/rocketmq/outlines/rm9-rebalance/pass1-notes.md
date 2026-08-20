# RM-9 Rebalance+offset+LitePull — Pass 1 探索笔记

> 域: RM-9 消费再平衡 + 进度 + LitePull | 🔴 A 方案 | 2026-08-14
> 源码: RebalanceImpl (853) + RebalanceService (64) + RebalancePushImpl (291) + 6 分配算法 (413) + OffsetStore (RM-8 已讲) + DefaultLitePullConsumerImpl (1315) | RocketMQ 5.3.1

## 调用图

```
调度面 (RebalanceService, 64):
waitInterval=20s 默认 + minInterval=1s; **balanced ? 20s : 1s** (不平衡快速重试)
  → MQClientInstance.doRebalance → 各 consumer doRebalance

再平衡面 (RebalanceImpl, 853):
doRebalance (L237): 逐 topic —
  双模式: clientRebalance (本地分配算法) vs **broker 分配** (5.x queryAssignment:
    QUERY_ASSIGNMENT_TIMEOUT=3s / TIMEOUT_CHECK_TIMES=3 / 策略名上报)
  → rebalanceByTopic (本地) / getRebalanceResultFromBroker (broker 结果)
  → updateProcessQueueTableInRebalance (L311/364): 差集处理 —
    不再归属 → removeUnnecessaryMessageQueue (drop+unlock) → remove
    新归属 → putIfAbsent + computePullFromWhere (起点) + setLocked (有序锁)
  → messageQueueChanged (通知监听器 RM-8 消费面)
有序锁: lockAll/unlock (LOCK_BATCH_MQ — 有序消费队列锁定, broker 侧)

分配算法 (rebalance/ 6 个, 413 行):
AVG (均分: 余数前 mod 个多 1, startIndex 计算) / ByCircle (轮询圈) /
  ConsistentHash (一致性哈希 1024 虚拟节点?) / ByConfig (配置) / ByMachineRoom / Nearby (机房间就近, 129)

进度面 (RM-8 已讲 OffsetStore 双实现): 本域聚焦 computePullFromWhere (首次消费起点:
  CONSUME_FROM_LAST_OFFSET 等 4 模式) + updateAndFreezeOffset (OFFSET_ILLEGAL 冻结, RM-8 交叉)

LitePull 面 (DefaultLitePullConsumerImpl, 1315):
手动拉取消费 (poll 语义, 对照 Kafka): 用户控制拉取节奏; RebalanceLitePullImpl (180)
```

## 基本元素分解

1. **再平衡调度**: 20s/1s 自适应 (不平衡快速收敛)
2. **双模式分配**: 客户端算法 vs broker 分配 (5.x)
3. **差集处理**: 增删队列 (drop/unlock/新起点)
4. **6 分配算法**: AVG 主算法 + 5 变体
5. **消费起点**: computePullFromWhere (4 模式)
6. **有序锁**: LOCK_BATCH_MQ
7. **LitePull**: 手动拉取 (poll)

## 标记问题 (20 问)

1. 再平衡周期怎么自适应? (balanced ? 20s : 1s)
2. 客户端 vs broker 分配的切换? (queryAssignment)
3. 差集怎么处理? (remove/putIfAbsent)
4. removeUnnecessaryMessageQueue 干什么? (drop + unlock)
5. computePullFromWhere 起点模式? (4 种)
6. AVG 算法的分配数学? (余数处理)
7. ByCircle 与 AVG 差异?
8. ConsistentHash 虚拟节点? (1024?)
9. 有序消费的队列锁? (LOCK_BATCH_MQ)
10. messageQueueChanged 通知? (监听器)
11. 再平衡中的消费中断? (drop 后未消费消息?)
12. 5.x broker 分配的动机? (中心化)
13. TIMEOUT_CHECK_TIMES=3 的重试?
14. processQueueTable 的并发安全? (ConcurrentHashMap)
15. LitePull 的 poll 语义?
16. LitePull 与 Push 的 Rebalance 差异? (RebalanceLitePullImpl)
17. 广播模式的再平衡? (无 rebalance?)
18. 新消费者加入的触发? (心跳 broker → 广播 → 再平衡)
19. 测试面? (6 算法测试已见 5345 行)
20. 与 RM-8 的边界? (RebalancePushImpl 桥接)

## 时空溯源 (代码内痕迹)

- 3.x: RebalanceService + AVG 算法 + 有序锁 (LOCK_BATCH_MQ)
- 4.x: ConsistentHash/MachineRoom/Nearby 算法族; computePullFromWhere 4 模式
- 5.x: **broker 分配模式** (queryAssignment + topicClientRebalance/topicBrokerRebalance 双表); RebalanceLitePullImpl; 自适应 1s 快速重试

## 大域拆分判断

RM-9 覆盖 Rebalance + 进度面 + LitePull; 单篇 (🔴 A, 6 闭环); OffsetStore 核心在 RM-8 已讲 (本域聚焦起点/冻结)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "RebalanceImpl 三实现/AllocateMessageQueueStrategy/OffsetStore/LitePull" | RebalanceImpl + Push/LitePull/Pull 三实现 + 6 算法 + store 双实现 + LitePullImpl | **接受** ✅ |
| 数字: 周期 | waitInterval=20s / minInterval=1s (系统属性) | **补充** ✅ |
| 数字: 分配算法 | 6 个 (AVG/ByCircle/ConsistentHash/ByConfig/MachineRoom/Nearby) | **补充** ✅ |
| 数字: broker 分配超时 | QUERY_ASSIGNMENT_TIMEOUT=3s / TIMEOUT_CHECK_TIMES=3 | **补充** ✅ |
