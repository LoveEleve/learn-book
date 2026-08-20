# RocketMQ-1 重写规划

> 题目：一条消息怎样从 Producer 走到 Consumer —— RocketMQ 主链总图
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把 RocketMQ 的第一篇写成“没看懂 RocketMQ 的读者先建立主链地图”的总图篇，而不是一上来就掉进 Producer、Broker、CommitLog 某个局部实现细节

## 1. 读者困惑

- RocketMQ 的一条消息到底是不是“Producer 发给 Broker，Consumer 再取走”这么简单？
- NameServer、Broker、CommitLog、ConsumeQueue、Consumer 各自到底在补什么缺口？
- 为什么 Broker 不能被理解成简单转发站？
- 为什么 CommitLog 和 ConsumeQueue 不是两份重复数据？
- 为什么这篇必须先立主链总图，后面才能拆 Producer/存储/消费/事务/HA？

## 2. 一句话顿悟

**RocketMQ 的消息主链不是两跳转发，而是“Producer 发现路由 -> Broker 接收请求 -> CommitLog 先落统一顺序日志 -> ConsumeQueue 再建立消费索引 -> Consumer 按队列拉取推进”的分层管道；每一层都在解决上层不能直接跳过的现实问题。**

## 3. 总图

```text
Producer
  → NameServer 找路由
    → Broker 请求处理器
      → CommitLog 顺序落盘
        → Reput / ConsumeQueue 建消费索引
          → Consumer 拉取 / Rebalance / ProcessQueue
            → 业务消费
```

## 4. 关键边界

- 本篇是 RocketMQ 开篇总图篇，不深入任何单层实现细节。
- 不展开 Broker 初始化内部细节，不展开 CommitLog 刷盘参数、IndexFile 文件布局、长轮询实现、事务消息、DLedger/Controller 细节。
- 重点回答“为什么主链必须有这些层”，而不是“每层源码怎么写”。
- NameServer 在本篇只讲“路由发现角色”，不展开其内部数据结构与注册流程细节。
- Consumer 在本篇只讲“为什么不能直接读 CommitLog”，不提前吞掉 Rebalance / Push / Pull 的全部实现。

## 5. 本轮重写主线

1. 用“消息发出去后，中间到底发生了什么”开场。
2. 否定几个失败直觉：Producer 直接发给 Consumer、Broker 只是转发站、CommitLog/ConsumeQueue 是重复存储、NameServer 像强一致注册中心。
3. 先按主链顺序解释每一层存在的理由。
4. 每一层只讲“职责”和“为什么不能省”，不抢后续专题实现细节。
5. 结尾明确后续篇目拆分：Broker 宿主、Producer+路由、存储、消费、变体专题、一致性/事务。
