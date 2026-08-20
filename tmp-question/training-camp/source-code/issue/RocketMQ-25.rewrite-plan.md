# RocketMQ-25 重写规划

> 题目：Broker 为什么不是“一堆 Processor 堆在一起”——请求码分发与宿主入口主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：补深 `BrokerController.registerProcessor()` 这条宿主入口链，解释 RocketMQ 为什么把 SEND / PULL / HEARTBEAT / END_TRANSACTION / ADMIN 等请求按 RequestCode 分发到不同 Processor 与线程池，而不是让 Broker 成为一个无边界的大 switch 杂糅层。

## 1. 读者困惑

- Broker 收到各种请求以后，到底是谁决定交给哪个 Processor？
- `SendMessageProcessor`、`PullMessageProcessor`、`ClientManageProcessor`、`ConsumerManageProcessor`、`EndTransactionProcessor` 为什么要分开？
- 请求处理为什么还要绑定不同线程池，而不是统一丢进一个 executor？
- default processor 的存在意味着什么，为什么管理命令会落到 `AdminBrokerProcessor`？
- 请求码分发这层为什么值得单独成篇，而不是启动过程里的一个实现细节？

## 2. 一句话顿悟

**RocketMQ 的 Broker 不是“一个大 switch 里塞满所有逻辑”，而是先在 `registerProcessor()` 里把不同 RequestCode 显式分配给不同 Processor 与线程池，让发送、拉取、心跳、事务和管理命令各自挂到合适的宿主入口上；这层分发结构，决定了后面所有主链到底从哪里进入 Broker 世界。**

## 3. 五要素卡片

### 读者问题

为什么 `BrokerController` 要花这么大篇幅注册 Processor，而不是做一个统一入口再在内部随便分支？

### 入口

- `BrokerController.registerProcessor()`：注册所有 RequestCode → Processor 映射
- `SendMessageProcessor.processRequest()`：发送入口
- `PullMessageProcessor.processRequest()`：拉取入口
- `ClientManageProcessor.processRequest()`：心跳/注销/客户端配置入口
- `ConsumerManageProcessor.processRequest()`：offset / group 视图入口
- `EndTransactionProcessor.processRequest()`：事务二阶段入口
- `AdminBrokerProcessor.processRequest()`：默认管理入口

### 状态核心

- RequestCode → NettyRequestProcessor 的映射关系
- `remotingServer` / `fastRemotingServer` 双注册
- send / pull / heartbeat / admin / ack 等专用 executor
- hook 注册（send/consume hook）
- default processor 与显式 processor 的边界

### 失败路径

- 所有请求共用一个大入口：发送、拉取、管理命令互相污染语义和线程资源
- 所有请求共用一个线程池：慢查询或管理操作会拖垮高频消息主链
- 默认处理器吞掉过多语义：关键主链入口失去显式边界
- 唤醒长轮询时绕过 `PullMessageProcessor`：普通 pull 和唤醒 pull 形成两套逻辑
- 事务二阶段不单独挂入口：Broker 无法清晰守住 half fact 的收束边界

### 连接点

- 前文 `RocketMQ-2`：Broker 宿主能力篇只说“Processor 平面已装配”，本篇补“它们怎样形成请求入口图”
- 前文 `RocketMQ-23`：长轮询最终仍回到 `PullMessageProcessor.executeRequestWhenWakeup()`
- 前文 `RocketMQ-20/21`：事务回查与二阶段都依赖独立 Processor 边界
- 后续若补 admin/consumer offset/heartbeat 深篇，本篇可作为统一入口导航

## 4. 总图

```text
Remoting request(RequestCode)
  → BrokerController.registerProcessor() 预先绑定入口
    → SEND_*        → SendMessageProcessor + sendMessageExecutor
    → PULL_*        → PullMessageProcessor + pull/litePull executor
    → HEART_BEAT    → ClientManageProcessor + heartbeatExecutor
    → OFFSET/GROUP  → ConsumerManageProcessor + consumerManageExecutor
    → END_TRANSACTION → EndTransactionProcessor + endTransactionExecutor
    → 其他管理命令   → AdminBrokerProcessor(default) + adminBrokerExecutor
```

## 5. 关键边界

- 本篇只讲请求码分发与宿主入口，不重新展开各 Processor 内部完整算法。
- 不把 Processor 分层写成“代码组织习惯”；它是 Broker 运行时隔离的入口结构。
- 不把 default processor 误写成“什么都能处理”的主入口；关键消息链入口是显式注册的。
- 不细抠 Netty pipeline，只聚焦 RocketMQ 自己的 RequestCode → Processor 平面。

## 6. 失败方案推演

1. **所有请求进一个大 Processor 再 switch**：最直觉，但高频消息路径与低频管理命令会快速耦合成大泥球。
2. **所有请求共用一个线程池**：慢 admin / query / rebalance 会拖累 send/pull 主链时延。
3. **所有请求都走 default processor**：关键主链边界消失，读者和运行时都很难看出入口职责。
4. **长轮询唤醒时直接拼响应，不回 Processor**：会让同一类 Pull 请求出现两套不一致语义。

## 7. 误解清单

- Processor 分层不只是代码整洁，它决定运行时入口隔离。
- default processor 不是 Broker 的主入口，而是兜底管理入口。
- 发送和拉取分属不同 Processor，不只是因为方法多，而是因为主链职责不同。
- 不同 executor 不是线程池微优化，而是主链资源隔离。
- 长轮询唤醒最终仍要回到 `PullMessageProcessor`，不是旁路。 

## 8. 证据清单

- `broker/src/main/java/org/apache/rocketmq/broker/BrokerController.java:1070`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/SendMessageProcessor.java:86`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/PullMessageProcessor.java:287`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/ClientManageProcessor.java:57`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/ConsumerManageProcessor.java:58`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:57`
- `broker/src/main/java/org/apache/rocketmq/broker/processor/AdminBrokerProcessor.java:248`
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:147`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇聚焦 Broker 请求入口分发，不深入 Netty remoting 层和每个 Processor 的全部业务细节。
- 目标正文：7000~11000 字；核心拆解层覆盖 RequestCode 分发、线程池隔离、default processor、跨篇入口导航。

## 10. 本轮重写主线

1. 从“Broker 为什么不是一个大 switch”开场。
2. 否定统一入口、统一线程池、default processor 吞一切三种朴素方案。
3. 解释 `registerProcessor()` 如何把关键主链请求码显式挂到不同 Processor。
4. 解释为什么发送/拉取/心跳/事务/管理命令要配不同 executor。
5. 解释长轮询和事务二阶段为什么都要回到显式 Processor 入口。
6. 收网：Broker 宿主真正稳定，不在“功能很多”，而在入口边界与资源边界先被钉死。