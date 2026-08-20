# 闭环笔记 q3: 差集处理与 6 分配算法

## 假设
再平衡 = 队列归属差集: 增 (新队列) / 删 (不再归属); 分配算法可插拔。

## 验证过程
- **updateProcessQueueTableInRebalance** (L311/364):
  - **删**: 不在 mqSet → removeQueueMap → **removeUnnecessaryMessageQueue** (drop ProcessQueue + 有序锁释放 unlock) → processQueueTable.remove + changed
  - **增**: putIfAbsent → **computePullFromWhere** (起点, q4) + setLocked (有序)
- **6 分配算法** (AbstractAllocateMessageQueueStrategy + 6 实现):
  - **AVG** (54): cidAll.indexOf(current) → 余数处理 (前 mod 个多 1) + startIndex 计算 — 主算法
  - ByCircle (50): 轮询圈分配 (cid 与 mq 交替)
  - ConsistentHash (102): 一致性哈希 (虚拟节点)
  - ByConfig (43): 配置指定
  - MachineRoom (75): 机房隔离 (同机房队列)
  - **Nearby** (129): 机房间就近 (5.x, 增强)
- **Abstract** (54): check 空集合/重复 cid 防护

## 代码类型
Algorithmic (分配算法)

## 跨域关联
- RM-8 (消费): processQueue 生命周期
- RM-5 (Broker): 有序锁服务端

## 结论
差集处理 = 删 (drop+unlock) / 增 (起点+锁); 6 算法可插拔 (AVG 主算法, 余数前 mod 个多 1)。
源码位置: RebalanceImpl.java:311-370; rebalance/ 6 文件 413 行
