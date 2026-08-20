# RocketMQ-23 重写规划

> 题目：Consumer 没消息时为什么不立即返回——Pull 长轮询与挂起请求主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：补深 Broker 端“拉取请求为什么可以暂不返回”的运行时主链，把 `PullMessageProcessor`、`PullRequestHoldService`、`ManyPullRequest`、消息到达通知和超时唤醒串成闭环。

## 1. 读者困惑

- Consumer 拉取时没有消息，Broker 为什么不立即返回空结果？
- 一个请求挂起以后，Broker 把它放在哪里，消息来了谁负责唤醒？
- 新消息到达、过滤不匹配、请求超时分别怎样处理？
- 长轮询和短轮询到底差在哪，为什么不是简单 sleep 参数？
- `PullRequestHoldService` 与 `PullMessageProcessor` 为什么要互相回调？

## 2. 一句话顿悟

**RocketMQ 长轮询不是让 Broker 线程睡着等消息，而是把暂时无结果的 PullRequest 转移到按 `topic@queueId` 管理的挂起队列，消息到达时先按 offset/filter 唤醒，没匹配消息则继续挂起，超时才重新执行请求并返回。**

## 3. 五要素卡片

### 读者问题

没有消息时，Broker 如何把“暂时没有结果”的 Pull 请求变成一个可被消息到达事件或超时重新驱动的状态？

### 入口

- `PullMessageProcessor.processRequest()`：Pull 请求正常入口
- `PullRequestHoldService.suspendPullRequest()`：挂起请求
- `ManyPullRequest`：按 topic@queueId 聚合挂起请求
- `NotifyMessageArrivingListener.notifyMessageArriving()`：CommitLog 新消息到达后的通知桥
- `PullRequestHoldService.notifyMessageArriving()`：按 offset/filter/timeout 处理挂起请求
- `PullMessageProcessor.executeRequestWhenWakeup()`：重新执行被唤醒的 Pull 请求

### 状态核心

- `PullRequest` 的 pull offset、suspend timestamp、timeout、filter
- `pullRequestTable` 的 `topic@queueId` 分桶
- `ManyPullRequest` 的 clone-and-clear 批量转移
- `newestOffset > pullFromThisOffset`
- ConsumeQueue filter 与 CommitLog property filter
- 长轮询 5 秒检查周期 / 短轮询配置周期

### 失败路径

- 没消息立即返回：Consumer 需要高频空拉，形成无效请求风暴
- 消息到达但 offset 没超过请求起点：不能误唤醒
- 消息到达但 Tag/SQL 属性不匹配：请求应继续挂起，不应返回错误空结果
- 请求超过 timeout：重新执行请求，让客户端拿到最终空结果
- Broker 主切换：挂起请求需要被唤醒重新走当前主视图
- 通知执行失败：请求不能丢失，需依赖剩余挂起/超时路径兜底

### 连接点

- 前文 `RocketMQ-2`：Broker 宿主能力中只点到长轮询，本篇补其运行时闭环
- 前文 `RocketMQ-5/6/7`：ConsumeQueue、过滤和 Pull 主链是挂起后重新执行的前置
- 后续可继续补 Pop long polling，但本篇先聚焦经典 Pull 长轮询

## 4. 总图

```text
Consumer Pull
  → Broker 查询没有可返回消息
    → suspendPullRequest(topic@queueId)
      → ManyPullRequest 保存挂起请求
        → 新消息到达通知
          → offset 先判断是否有新增
            → filter 判断是否匹配
              → 匹配：executeRequestWhenWakeup()
              → 不匹配：重新放回挂起队列
        → timeout 到期：重新执行请求并返回空结果
```

## 5. 关键边界

- 本篇只讲经典 Pull 长轮询，不展开 PopLongPollingService、LitePull、消费组协调。
- 不把长轮询写成线程阻塞等待；请求进入的是 Broker 管理的挂起队列。
- 不把“有新消息”写成“当前请求一定有可见结果”，还要经过 offset 与过滤判断。
- 不把 timeout 写成异常；它是挂起请求的正常终止路径。

## 6. 失败方案推演

1. **无消息立即返回**：Consumer 会持续空拉，浪费网络与 Broker 线程。
2. **Broker 线程阻塞等待每个请求**：请求数一多就把线程资源耗尽，无法扩展。
3. **任意新消息都唤醒所有请求**：会制造无效重试，过滤不匹配请求仍然白跑。
4. **只按 offset 不做过滤判断**：Tag/SQL 订阅会收到不属于自己的唤醒结果。
5. **永不超时**：断开的客户端或永远不匹配的请求会永久占用挂起表。

## 7. 误解清单

- 长轮询不是让一个 Broker 工作线程 sleep。
- 新消息到达不等于所有挂起 Pull 都应立即返回。
- 过滤不匹配时，请求通常继续挂起，而不是当作消费失败。
- timeout 是正常收口，不是系统异常。
- `ManyPullRequest` 是挂起请求的批量管理结构，不是消息缓存。

## 8. 证据清单

- `broker/src/main/java/org/apache/rocketmq/broker/processor/PullMessageProcessor.java:287`
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:44`
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:68`
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:100`
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:118`
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/ManyPullRequest.java:25`
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/ManyPullRequest.java:33`
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/NotifyMessageArrivingListener.java:40`
- `broker/src/test/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldServiceTest.java:90`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇聚焦经典 Pull 长轮询；Pop long polling、ColdData、LMQ 子类只作为边界提示。
- 目标正文：7000~11000 字；核心拆解层覆盖挂起、分桶、通知唤醒、过滤重挂起、超时重新执行。

## 10. 本轮重写主线

1. 从“没有消息时为什么不立即返回”开场。
2. 否定立即空返回、线程阻塞、任意唤醒和无超时挂起。
3. 解释 PullMessageProcessor 怎样把无结果请求交给 PullRequestHoldService。
4. 解释 ManyPullRequest 如何按 topic@queueId 保存挂起请求。
5. 解释消息到达后 offset/filter/timeout 三段判断与重新挂起。
6. 收网：长轮询本质是把“暂时无结果”变成事件驱动的可重入请求状态。