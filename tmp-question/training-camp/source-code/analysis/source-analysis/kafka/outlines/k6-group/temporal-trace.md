# K-6 Consumer Group — 时空溯源 (2026-08-15)

> 🔴 A 域强制 | 方法: git log --diff-filter=A 找引入 commit + git log -1 日期实证

## 断代链

| 时间 | commit | 里程碑 | 演进 |
|---|---|---|---|
| 2015-10-21 | KAFKA-2464 | **GroupCoordinator.scala 初始** | 与 K-2 ConsumerCoordinator 同期 — 客户端/服务端 rebalance 协议同日诞生 |
| 2017-04-26 | KAFKA-5059 | **事务协调** | GroupCoordinator 扩展事务能力 (与 K-1 TransactionManager 同期) |
| 2022-11-09 | KAFKA-14363 | **新 group-coordinator 模块** | KIP-848 启动 — 独立模块 (Java) 取代 core/scala 旧实现 |
| 2023-07-27 | KAFKA-14499 | **OffsetMetadataManager** | KIP-848 OffsetCommit API 落地 |
| 2023-12-21 | KAFKA-16040 | **Generic→Classic 更名** | 旧协议组命名稳定 (与 K-2 ClassicKafkaConsumer 2024-07 更名呼应) |
| 2024-06-27 | KAFKA-16822 | **ModernGroup 抽象化** | 消费者组→抽象组 (Share Group 共享基础设施) |

## 关键观察

1. **2015 是组协议诞生点**: 客户端 (K-2) 与服务端 (K-6) 同日引入 — rebalance 协议从第一天就是两端设计
2. **2022-2024 KIP-848 是第二次架构**: 旧 Scala 实现 → 新 Java 模块 (增量 rebalance + 服务端分配) — 与 K-2 AsyncKafkaConsumer 2023 同期 (客户端服务端同步迁移)
3. **更名序列**: Generic→Classic (2023-12) 与 Legacy→Classic (K-2, 2024-07) — 旧协议命名在两端先后稳定
4. 对照 K-1/K-2: Producer 2014→2017→2019; Consumer 2014→2015→2023; Group 2015→2017→2022 — **组协调层是三者中最晚二迁的** (协议复杂度最高)

## 对照锚点

- K-2 Consumer: ConsumerCoordinator 2015 同期 — 两端协议同日诞生
- K-5 Controller: KRaft 元数据记录 vs GroupMetadataManager 记录 — 同构 (用日志存状态)
- r29-pubsub: Redis 订阅无协调器 vs Kafka 组协调器 — 协调能力分水岭
