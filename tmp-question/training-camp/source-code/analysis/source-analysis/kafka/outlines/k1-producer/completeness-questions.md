# K-1 Producer — 全视角提问验证 (completeness)

> 验证时机: 3 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生 (7 身份)

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 流水线 | send 内部几步? | ✅ 01-L2 (六步) |
| 2 | 开发者 | 流水线 | metadata 什么时候等? | ✅ 01-L2 (waitOnMetadata KafkaProducer.java:L990) |
| 3 | 开发者 | 流水线 | 异常怎么分流? | ✅ 01-L3 (三类) |
| 4 | 开发者 | 聚合 | 批怎么攒? 双路径? | ✅ 02-L2 (tryAppend/新批) |
| 5 | 开发者 | 内存 | 内存不够怎么办? | ✅ 02-L3 (阻塞/抛) |
| 6 | 开发者 | 内存 | free 怎么回收? | ✅ 02-L3 (poolableSize) |
| 7 | 开发者 | 分区 | 粘性分区怎么切换? | ✅ 02-L4 (stickyBatchSize) |
| 8 | 开发者 | Sender | Sender 每轮做什么? | ✅ 03-L2 (ready/drain/send/poll) |
| 9 | 架构师 | 设计 | 为什么异步? | ✅ 01 核心悬念 (不阻塞 IO) |
| 10 | 架构师 | 设计 | 为什么批聚合? | ✅ 02 核心悬念 (攒大批) |
| 11 | 架构师 | 设计 | 为什么内存有界? | ✅ 02-L3 (32MB 默认) |
| 12 | 架构师 | 设计 | acks 三态怎么选? | ✅ 03-L3 (0/1/all) |
| 13 | 架构师 | 幂等 | 幂等怎么保证? | ✅ 03-L4 (producerId+seq) |
| 14 | 性能工程师 | 批 | batch.size 多大合适? | ✅ 02-L3 (16KB 默认) |
| 15 | 性能工程师 | 内存 | buffer.memory 与吞吐? | ✅ 02-L3 (32MB 有界) |
| 16 | 性能工程师 | 延迟 | linger.ms 作用? | ⚠️ 02 未显式提 → 补一句 (linger 等待攒批) |
| 17 | SRE | 故障 | send 失败怎么办? | ✅ 01-L3 (FutureFailure/重试) |
| 18 | SRE | 超时 | maxBlockTimeMs 超了? | ✅ 01-L2 (waitOnMetadata 上限) |
| 19 | SRE | 关停 | close 时剩余批? | ✅ 03-L2 (关闭等待 Sender.java:L256-258) |
| 20 | 研究者 | 对照 | vs RocketMQ Producer? | ✅ 03-L4 (MQFaultStrategy) |
| 21 | 研究者 | 对照 | vs Redis 写入面? | ✅ 01 header (r24-string) |
| 22 | 研究者 | 对照 | vs Redisson 命令批? | ✅ 02 header (rd4-command) |
| 23 | 研究者 | 历史 | 异步化何时定型? | ✅ 时空溯源 (2014 Java 化) |
| 24 | 子系统开发者 | 衔接 | acks=all 服务端谁等? | ✅ 03-L3 (K-7 DelayedProduce) |
| 25 | 子系统开发者 | 衔接 | 幂等状态存哪? | ✅ 03-L4 (K-3 ProducerStateManager) |
| 26 | 子系统开发者 | 衔接 | 发送走哪层网络? | ✅ 03-L2 (client.poll, K-8) |
| 27 | 子系统开发者 | 衔接 | 事务与 K-11? | ✅ 03-L4 (TransactionManager 共享) |
| 28 | 学生 | 概念 | send 通俗解释? | ✅ 01-L1 (立即返回) |
| 29 | 学生 | 概念 | 批是什么? | ✅ 02-L1 (攒批引擎) |
| 30 | 学生 | 概念 | acks=all 是什么? | ✅ 03-L1 (全 ISR 确认) |

**统计**: ✅ 29 / ⚠️ 1 / ❌ 0 — ⚠️ 1 项"补一句"级
→ 回补 1 项: 02-L2 补 linger.ms 语义

## 回补清单

1. 02-L2: 补一句 "linger.ms: 批未满时最长等待 (默认 5ms ProducerConfig.java:397)"
