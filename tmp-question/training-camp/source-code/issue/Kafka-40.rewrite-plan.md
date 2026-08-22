# Kafka-40 重写规划

> 题目：分区到底怎么分给成员——Range、RoundRobin、Sticky、Uniform 分配算法主链
> 状态：补充篇，覆盖 zsxq【原理分析系列第十四篇】消费者分区分配策略。按 Kafka v4 展开 Classic（客户端）与 Modern（服务端）两种 Assignor 场景。

## 1. 读者困惑
- Range 为什么容易不均？
- RoundRobin 怎么解决不均，但为什么不粘性？
- Sticky 怎么保证稳定？
- CooperativeSticky 与 Sticky 区别？
- Modern 协议下分配在哪儿做？

## 2. 一句话顿悟
**Classic 协议由 leader 在客户端用 AbstractPartitionAssignor 算分配；Modern 协议由 coordinator 用服务端 assignor（如 UniformAssignor）算。Range 按 topic 均分但不均，RoundRobin 拉平均但不粘，Sticky 求粘性与均衡，CooperativeSticky 支持增量，Uniform 服务端按同质/异质订阅选子算法。**

## 3. 失败方案推演
- Range 在大数 topic 时少数成员过载
- RoundRobin 每次 rebalance 大变
- Sticky 不支持增量 rebalance

## 4. 误解清单
- "assignment 一定在客户端"：Modern 在服务端
- "RoundRobin 一定均衡"：只是比 Range 好，仍可能不均
- "Sticky 一定最均衡"：以粘性和均衡折中
- "CooperativeSticky 只有集群分配"：主要用于增量 rebalance

## 5. 证据清单
- clients AbstractPartitionAssignor.java:44
- group-coordinator assignor UniformAssignor.java:53 / RangeAssignor.java:85

## 6. 版本边界与字数预算
- 基线 v4.x；目标 5000~8000 字。