# 闭环笔记 q2: 双模式分配 — 客户端算法 vs broker 分配

## 假设
5.x 支持 broker 中心化分配; 客户端模式为传统。

## 验证过程
- **doRebalance** (L237-320): 逐 topic — `if (!clientRebalance(topic) && tryQueryAssignment(topic))`:
  - **broker 分配** (tryQueryAssignment L265-300): topicBrokerRebalance 表标记 → **queryAssignment (上报策略名, QUERY_ASSIGNMENT_TIMEOUT=3s, TIMEOUT_CHECK_TIMES=3 重试)** → getRebalanceResultFromBroker
  - **客户端分配** (rebalanceByTopic): 本地算法 (q3)
- **双表**: topicClientRebalance / topicBrokerRebalance (ConcurrentHashMap) — 模式判定缓存
- **5.x 动机**: broker 中心化分配 (配合 Controller/服务端管理)

## 代码类型
Implementation (双模式)

## 跨域关联
- RM-14 (Controller): broker 分配的服务端支持
- RM-5 (Broker): QUERY_ASSIGNMENT 请求码

## 结论
再平衡 = 客户端本地算法 (默认) 或 broker 中心化分配 (5.x, 3s×3 重试); 双表缓存模式。
源码位置: RebalanceImpl.java:237-320; QUERY_ASSIGNMENT_TIMEOUT
