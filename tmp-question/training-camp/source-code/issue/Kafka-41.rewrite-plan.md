# Kafka-41 重写规划

> 题目：路由怎么知道该找谁——MetadataCache 元数据缓存与路由主链
> 状态：补充篇，覆盖 zsxq【服务端_Broker_源码分析系列第三十一篇】Broker 异步更新元数据缓存。按 KRaft v4 展开 MetadataCache 的缓存结构、更新路径与路由用途。

## 1. 读者困惑
- MetadataCache 缓存什么？
- 怎么从 MetadataImage 更新到局部 cache？
- 生产/消费路由怎么用？

## 2. 一句话顿悟
**MetadataCache 是本地的元数据“读视图”，由 MetadataLoader 的 publish 事件驱动更新；生产/消费请求通过它查分区 leader，决定把请求发给哪个 broker。**

## 3. 失败路径
- 缓存未更新就服务 → 请求发错 leader
- 缓存过大 → 内存拥挤

## 4. 误解清单
- "MetadataCache 缓存所有元数据"：只缓存路由相关
- "每次请求都问 controller"：本地缓存避免

## 5. 证据清单
- metadata MetaDataCache.java
- core MetadataCache.scala

## 6. 版本边界与字数预算
- 基线 v4.x；目标 5000~8000 字。