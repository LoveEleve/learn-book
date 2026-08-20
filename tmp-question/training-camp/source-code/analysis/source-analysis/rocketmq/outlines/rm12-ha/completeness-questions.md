# RM-12 HA/DLedger+Controller — completeness-questions (全视角提问验证)

## 开发者视角

1. 主从怎么知道复制到哪了? (三条水位: masterPutWhere/push2SlaveMaxOffset/slaveAckOffset)
2. 从库主动连主还是主推? (从库 connectMaster 主动拉, 报 offset 后主推)
3. 同步断连怎么恢复? (5s 重试 + 从 request offset 续推; 20s 无响应 housekeeping)
4. 从库怎么升主? (AutoSwitch: epoch 截断 + confirmOffset 恢复; Controller: 选举通知)
5. fenced 是什么? (Controller 判定 broker 失联 → 拒绝读写, 防脑裂)
6. 同步复制等什么? (组提交等 inSyncReplicas 个从库 ack 到 slaveAckOffset)
7. DLedger 和主从复制差在哪? (Raft 多数派日志 vs 单主推送)
8. 心跳超时谁判? (namesrv 2min/5s + Controller 默认 2min/5s)

## 架构师视角

9. 为什么从库主动拉而不是主推? (从库控制节奏, 天然背压; 主挂不残留连接)
10. 256MB 落后阈值 (haMaxGapNotInSync) 的意义? (超差从库不算 in sync, 不参与组提交)
11. epoch 文件为什么必要? (数据合法区间裁决 — 谁在哪个区间写过, 防旧主复活污染)
12. Controller 为什么独立 Raft? (元数据集群自身高可用; broker 心跳进日志)
13. 选举策略为什么 maxOffset+priority? (数据最新者优先, 同则人工优先级)
14. fenced 与 namesrv 擦权的区别? (fenced=存储层拒读写; 擦权=路由层只读 — 双保险)
15. 对照 Kafka ISR/LeaderEpoch? (syncStateSet≈ISR; epoch≈LeaderEpoch; 无分区级 leader)
16. 异步复制的丢消息窗口? (主刷盘即 ack, 从未确认 → 主挂丢窗口内消息)

## 学生视角

17. 什么是主从复制? (主写从备份, 从库把主库数据拉过来)
18. 什么是选举? (多副本选一个当主, 其他人当从)
19. 什么是 fenced? (失联的节点被标记, 禁止继续服务)
20. 为什么要 3 副本? (Raft 多数派: 3 副本容忍 1 挂)
