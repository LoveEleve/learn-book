# 闭环笔记 q2: 长轮询拉取 — pullKernelImpl + sysFlag

## 假设
Pull = ASYNC 请求 + broker 挂起 (长轮询); sysFlag 四 bit 控制。

## 验证过程
- **pullMessage** (L300-480):
  - offsetStore.readOffset (集群: 内存/broker; L453-458) → commitOffsetEnable
  - **sysFlag 四 bit** (PullSysFlag.buildSysFlag L475-479): commitOffset / **suspend=true** / subscription (postSubscriptionWhenPull) / classFilter
  - **pullKernelImpl** (ASYNC): 参数 = pullBatchSize / **BROKER_SUSPEND_MAX_TIME_MILLIS=15s** (broker 挂起长轮询) / **CONSUMER_TIMEOUT_MILLIS_WHEN_SUSPEND=30s** (客户端总超时)
- **回调** (pullCallback): 成功 → ProcessQueue.putMessage + submitConsumeRequest; **空 (PULL_NOT_FOUND) → suspend 重拉** (1s); 流控 → 50ms; 异常 → 3s
- **长轮询闭环**: 客户端挂起请求 → broker PullRequestHoldService (RM-5 交叉) 持有 15s → 新消息到达唤醒

## 代码类型
Algorithmic (长轮询)

## 跨域关联
- RM-1 (remoting): ASYNC invoke
- RM-5 (Broker): PullRequestHoldService 挂起
- RM-3 (存储): getMessage

## 结论
拉取 = ASYNC 长轮询 (broker 挂 15s/客户端等 30s); sysFlag 四 bit (offset 提交/挂起/订阅/类过滤); 空拉取 1s 重试。
源码位置: DefaultMQPushConsumerImpl.java:300-480; PullSysFlag
