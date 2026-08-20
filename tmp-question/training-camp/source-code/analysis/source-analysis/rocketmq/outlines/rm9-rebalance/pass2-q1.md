# 闭环笔记 q1: 再平衡调度 — 20s/1s 自适应

## 假设
周期自适应: 平衡 20s, 不平衡 1s 快速收敛。

## 验证过程
- **RebalanceService** (ServiceThread): waitInterval=**20s** (系统属性 rocketmq.client.rebalance.waitInterval) + minInterval=**1s**
- **run 循环** (L22-40): waitForRunning → 距上次 ≥ minInterval? → doRebalance → **balanced ? 20s : 1s** (不平衡快速重试) + lastRebalanceTimestamp 更新
- **触发面**: 周期轮询 + **rebalanceImmediately** (MQClientInstance — RM-8 OFFSET_ILLEGAL 修复时调用) + 心跳 broker 变更广播 (RM-5 交叉)
- **客户端入口**: MQClientInstance.doRebalance → 各 MQConsumerInner

## 代码类型
Implementation (周期调度)

## 跨域关联
- RM-5 (Broker): 心跳/变更广播
- RM-8 (消费): 拉取驱动

## 结论
再平衡 = 20s 周期 + 不平衡时 1s 快速收敛; 可被事件立即触发; 全局在 MQClientInstance 协调。
源码位置: RebalanceService.java:22-40
