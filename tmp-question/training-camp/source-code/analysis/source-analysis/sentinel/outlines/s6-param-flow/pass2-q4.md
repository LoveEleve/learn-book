# Pass 2 闭环笔记 Q4: CacheMap 的容量与淘汰

## 验证过程

- `CacheMap` 是参数值计数的通用接口，提供 containsKey/get/remove/put/putIfAbsent/size/clear/keySet(ascending)。
- `ConcurrentLinkedHashMapWrapper` 是它的实现，底层用 Google 的 `ConcurrentLinkedHashMap`，`maximumWeightedCapacity` 控制容量上限，`Weighers.singleton()` 表示每个 entry 权重为 1 (`ConcurrentLinkedHashMapWrapper.java:33-49`)。
- 参数值计数都用 `putIfAbsent` + `AtomicInteger/AtomicLong` 累加，容量满时 `ConcurrentLinkedHashMap` 按 LRU 自动淘汰。
- 热点参数统计的容量是刻意设限的（例如 `ParameterMetric` 里 `BASE_PARAM_MAX_CAPACITY=4000`、`THREAD_COUNT_MAX_CAPACITY=4000`），避免海量参数值撑爆内存。

## 结论

热点参数统计用带容量上限的 LRU map，而不是无界 HashMap。这是热点限流在“海量参数值”场景下能安全运行的关键：超出的冷门参数值会被自动淘汰。