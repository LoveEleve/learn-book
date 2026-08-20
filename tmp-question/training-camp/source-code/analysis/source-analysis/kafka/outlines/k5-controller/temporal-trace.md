# K-5 Controller — 时空溯源 (2026-08-15)

> 🔴 A 域强制 | 方法: git log --diff-filter=A 找引入 commit + git log -1 日期实证

## 断代链

| 时间 | commit | 里程碑 | 演进 |
|---|---|---|---|
| 2012-09-18 | KAFKA-499 | **KafkaController.scala 初始** | 控制器重构 — ZK 时代分区/副本状态机 |
| 2021-02-19 | KAFKA-12276 | **QuorumController 诞生** | KRaft 控制器代码 — 写事件队列 + Raft 复制 (与 BrokerHeartbeatManager 同期) |
| 2021-07-01 | KAFKA-13019 | **MetadataImage** | 元数据快照 — ClusterImage/TopicsImage/ConfigurationsImage |
| 2023-2024 | KIP-848 同期 | **KRaft 全面接管** | ZK 模式废弃 (Kafka 4.0 仅 KRaft) — KafkaController.scala 退役 |

## 关键观察

1. **2012 ZK 控制器 → 2021 KRaft 控制器**: 9 年 ZK 时代 (KafkaController) → QuorumController 重写 — 控制器架构的第二次诞生
2. **2021 同期三件套**: QuorumController + BrokerHeartbeatManager (KAFKA-12276) + MetadataImage (KAFKA-13019, 5 个月后) — KRaft 核心一次成型
3. **2024 全面接管**: 4.0 仅 KRaft (ZK 移除) — 与 K-6 KIP-848 (2022-2024) 同期完成"去 ZK"大迁移
4. 对照时间线: 存储层 2012 定型 (K-3) / 复制层 2017 (K-4) / 客户端 2014-2023 (K-1/K-2) / **协调层 2021 重写 (K-5) + 2022-2024 (K-6)** — 元数据面是最后完成现代化的一层

## 对照锚点

- K-6 GroupMetadataManager (2022-2024): 与 QuorumController 同为"记录状态机"模式
- E-10 ES ClusterState: ES master 协调 (2018 Zen2 重写) vs Kafka QuorumController (2021) — 两家同期现代化协调层
- K-9 KRaft (raft/): QuorumController 的复制底座 (交付后回补)
