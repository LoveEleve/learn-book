# RM-7 Producer 发送 — Pass 1 探索笔记

> 域: RM-7 Producer 发送 | 🔴 A 方案 | 2026-08-14
> 源码: client/producer (1442) + impl/producer (1841+154) + latency/ (338) + selector/ (107) ≈ 4980 行 | RocketMQ 5.3.1

## 调用图

```
门面面 (DefaultMQProducer, 1442 行):
send 族 (sync/async/oneway) → defaultMQProducerImpl (DefaultMQProducerImpl, 1841 行)
配置: retryTimesWhenSendFailed=2 / retryAnotherBrokerWhenNotStoreOK=false / sendLatencyFaultEnable (默认 false?)

发送主链 (DefaultMQProducerImpl):
sendDefaultImpl (L733): tryToFindTopicPublishInfo (路由, L883) → 轮询/故障选择 mq →
  sendKernelImpl (L900: 构造发送请求 + 调 remoting invokeSync/Async/Oneway)
  重试循环: 换 broker 重发 (最多 retryTimesWhenSendFailed), timeout 逐次扣减
  sendCallBack/异步回调面; sendOneway (fire-and-forget)

路由与故障 (latency/):
MQFaultStrategy (181): 7 档延迟→不可用映射 (latencyMax {50..15000}ms → notAvailableDuration {0..30000}ms);
  updateFaultItem (isolation 固定 10000ms) → LatencyFaultToleranceImpl.updateFaultItem
  selectOneMessageQueue: 三级过滤 (availableFilter → reachableFilter → 兜底)
LatencyFaultToleranceImpl (FaultItem: currentLatency/startTimestamp/reachableFlag; isAvailable = now > startTimestamp)
TopicPublishInfo (154): sendWhichQueue (ThreadLocalIndex 递增轮询 % size) + filter 循环尝试 + resetIndex
selector/: SelectMessageQueueByHash/MachineRoom/Random (顺序消息/机房间/随机)
ProduceAccumulator (510): 5.x 批量累积发送 (Accumulate 面)
```

## 基本元素分解

1. **门面**: DefaultMQProducer (send 族 + 配置)
2. **发送主链**: sendDefaultImpl (路由→选择→kernel→重试)
3. **路由**: tryToFindTopicPublishInfo (namesrv 缓存)
4. **队列选择**: 轮询 + 故障过滤 (MQFaultStrategy)
5. **故障表**: LatencyFaultToleranceImpl (延迟→不可用时间)
6. **三模式**: sync/async/oneway (invokeSync/Async/Oneway — RM-1)
7. **扩展**: 队列选择器 (hash/机房间/随机) + 5.x ProduceAccumulator

## 标记问题 (20 问)

1. send 三模式入口差异?
2. sendDefaultImpl 重试逻辑? (次数/换 broker/超时扣减)
3. tryToFindTopicPublishInfo 路由缓存? (topic → TopicRouteData)
4. 轮询选择为什么 % size? (ThreadLocalIndex 线程独立)
5. MQFaultStrategy 7 档映射?
6. FaultItem 可用判定? (startTimestamp)
7. 三级过滤 (available/reachable/兜底)?
8. isolation 语义? (发送失败隔离 10s)
9. retryAnotherBrokerWhenNotStoreOK? (存储失败换 broker)
10. sendKernelImpl 请求构造? (SEND_MESSAGE 头)
11. 异步回调面? (sendCallBack + 线程池)
12. oneway 语义? (RM-1 双端无响应)
13. 顺序消息选择器? (SelectMessageQueueByHash)
14. 机房间选择器? (同机房优先)
15. ProduceAccumulator? (5.x 批量累积)
16. 发送超时? (timeout 参数/默认 3s?)
17. 消息重试属性? (重试次数/上次 broker)
18. 幂等发送? (msgId/uniqKey)
19. 发送后故障更新? (updateFaultItem 时机)
20. 测试面? (client 测试)

## 时空溯源 (代码内痕迹)

- 3.x: DefaultMQProducer 骨架 (三模式 + 轮询 + 重试 2 次)
- 4.x: MQFaultStrategy 延迟故障 (7 档映射 + 可用过滤); selector 家族 (hash/机房间)
- 5.x: **ProduceAccumulator** (批量累积); ServiceDetector (服务探测, startDetectorEnable); reachableFlag (可达性双维度)

## 大域拆分判断

4980 行 — 单篇 (🔴 A, 6 闭环)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "DefaultMQProducer/MQFaultStrategy/SYNC/ASYNC/ONEWAY" | 面存在: 门面 + 策略 + 三模式 | **接受** ✅ |
| 数字: retryTimesWhenSendFailed | 2 (DefaultMQProducer:127) | **补充** ✅ |
| 数字: 7 档映射 | latencyMax 7 档 {50..15000} → notAvailableDuration {0..30000} (MQFaultStrategy:29-30) | **补充** ✅ |
| 数字: isolation | 固定 10000ms (L174) | **补充** ✅ |
