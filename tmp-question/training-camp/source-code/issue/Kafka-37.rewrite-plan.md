# Kafka-37 重写规划

> 题目：Server 侧怎么记住你的 fetch session——FetchSession、CachedPartition 与 epoch 校验
> 状态：K-5 FetchSession 域第 2 篇；与 Kafka-5 客户端篇对照，聚焦 server 侧 FetchSession / CachedPartition。

## 1. 读者困惑

- server 怎么按 sessionId 缓存分区？
- CachedPartition 保存什么？
- Full / Incremental 在 server 侧怎么区分？
- epoch 不匹配时返回什么错误？
- session 超时怎么处理？

## 2. 一句话顿悟

**Server 用 FetchSession 按 sessionId 缓存 CachedPartition 集合，增量请求通过 toForget / toReplace 修改缓存，epoch 校验确保视图一致，不匹配或 session 超时都要求客户端回退 Full Fetch 重新注册。**

## 3. 失败方案推演

- 每次 fetch 重新带全量分区地址 → 无缓存收益
- 不做 epoch 校验 → 视图不一致检查缺失
- 增量不带 toForget → 已弃分区残留

## 4. 误解清单

- "CachedPartition 存消息数据"：它保存请求参数
- "Full 和 Incremental 是两套协议"：同一 session 两阶段
- "会话一定不会被清理"：超时会被移除

## 5. 证据清单

- `core/src/main/scala/kafka/server/FetchSession.scala`：FetchSession / CachedPartition。
- `core/src/main/scala/kafka/server/FetchSession.scala`：epoch 校验。

## 6. 版本边界与字数预算

- 基线 v4.x；目标 5000~8000 字。