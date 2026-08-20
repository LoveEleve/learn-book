# 闭环笔记 q4: 队列选择与路由 — TopicPublishInfo + tryToFind

## 假设
路由缓存 (topic→TopicRouteData); 队列轮询 (ThreadLocalIndex)。

## 验证过程
- **路由** (tryToFindTopicPublishInfo L883-900): topic → **topicRouteData 缓存** (producerTable) → 未命中 → **fetchTopicRouteDataFromNameServer (RM-11 交叉)** → TopicPublishInfo (messageQueueList + sendWhichQueue); 失败 fallback (旧路由)
- **轮询** (TopicPublishInfo.selectOneMessageQueue L75-110): `Math.abs(sendWhichQueue.incrementAndGet() % size)` — **线程独立递增** (ThreadLocalIndex) 负载均衡; filter 时循环尝试 (最多 size 次)
- **resetIndex** (L112): 重试时重置索引 (L766-768 — 从 0 重新轮询, 配合 lastBroker 排除)
- **MessageQueue**: topic@brokerName@queueId 三元组

## 代码类型
Implementation (路由与负载均衡)

## 跨域关联
- RM-11 (Namesrv): 路由发现
- RM-9 (Rebalance): 队列选择对照

## 结论
路由 = namesrv 发现缓存; 队列选择 = 线程独立轮询 (递增取模) + 故障过滤; 重试 resetIndex 防重复打同一队列。
源码位置: DefaultMQProducerImpl.java:883-900; TopicPublishInfo.java:75-112
