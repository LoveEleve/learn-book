# 闭环笔记 q1: 顺序发送 — 选择器绑定队列

## 假设
顺序消息 = 业务键 hash 选队列, 同键同队列; 发送失败不换队列。

## 验证过程
- **MessageQueueSelector**: select(mqs, msg, arg) — 接口仅 1 方法 (client/producer/MessageQueueSelector.java:23-25)
- **SelectMessageQueueByHash** (L28-31): `value = arg.hashCode() % mqs.size()`, `value<0 → Math.abs(value)` — 同键恒同队列 (队列集不变时); **注意 |result|<size 故 Math.abs 无 MIN_VALUE 溢出风险**
- **SelectMessageQueueByRandom** (L33): ThreadLocalRandom 随机 — 非顺序用途
- **SelectMessageQueueByMachineRoom** (L29-31): **select() 直接 return null — 桩实现** ⚠ (consumeridcs 字段无人用)
- **sendSelectImpl** (DefaultMQProducerImpl:1314-1353): 选择器一次 → **单次 sendKernelImpl** — SYNC 无重试循环 (对照 sendDefaultImpl 1+2 次); 选队列耗时计入超时预算 (L1340-1343)
- **ASYNC 重试同 broker**: sendKernelImpl ASYNC 传 topicPublishInfo=null (L1345) → MQClientAPIImpl.onExceptionImpl (L778-782): `topicPublishInfo==null → retryBrokerName=brokerName` + request 复用 (同 queueId) → **同 broker 同队列重发**

## 代码类型
Implementation (队列选择)

## 跨域关联
- RM-7 (发送): sendKernelImpl/超时预算/双配置重试 — 本域对照无重试
- RM-9 (再平衡): 队列集变化 → hash 取模下标变化 → 同键可能换队列 (顺序在 rebalance 后不保 — 标注)

## 结论
顺序发送 = 选择器确定队列 (hash 取模) + 单次发送 (SYNC 不重试; ASYNC 重试同 broker 同队列)。MachineRoom 选择器为桩。
源码位置: SelectMessageQueueByHash.java:28-31; DefaultMQProducerImpl.java:1314-1353; MQClientAPIImpl.java:778-782
