# R-15 Cluster — completeness-questions + 时空溯源

## R-15a 分片 — 开发者视角

1. 16384 槽怎么算出来的 (14bit)?
2. keyHashSlot 的三个分支?
3. {tag} 什么时候不生效?
4. MOVED 和 ASK 的区别?
5. CLUSTER REDIR 七种是什么?
6. 槽迁移的双标记 (migrating/importing)?
7. 阻塞客户端槽迁走怎么办?
8. CLUSTER SLOTS 返回什么?

## R-15a 分片 — 架构师视角

9. CRC16 低 14 位为什么均匀?
10. {tag} 对事务/管道的意义?
11. 协议级重定向 vs 代理 (Redis Cluster 无代理)?
12. 迁移期间 ASK vs TRYAGAIN?
13. 阻塞救出的必要性 (R-26 交叉)?
14. 槽位表 (16384 数组) 的空间?
15. READONLY 豁免的语义?
16. 重分片的操作流程 (redis-cli --cluster reshard)?

## R-15a 分片 — 学生视角

17. "foo" 的槽是多少?
18. "foo{bar}baz" 和 "{bar}" 同槽吗?
19. MGET a b (不同槽) 会怎样?
20. SETSLOT MIGRATING 后键还在本节点吗?

## R-15b 协议 — 开发者视角

1. 集群总线端口 (port+10000)?
2. gossip 消息内容?
3. PFAIL 和 FAIL 的区别?
4. configEpoch 怎么裁决冲突?
5. 从库提升的投票流程?
6. cluster_state 怎么判定?
7. FAIL 消息是 gossip 还是广播?
8. 槽位图 (2048B) 怎么传?

## R-15b 协议 — 架构师视角

9. 带外总线的设计价值?
10. Gossip 的收敛复杂度 (O(log n) 轮)?
11. PFAIL/FAIL 与哨兵 SDOWN/ODOWN 的对照?
12. configEpoch 与 R-14 epoch 的异同?
13. 内建选举 vs 外部哨兵的取舍?
14. 最终一致的可用性边界 (槽覆盖)?
15. cluster_state fail 的写拒绝?
16. 无中心配置的传播路径?

## R-15b 协议 — 学生视角

17. 5 节点集群, 挂 1 个主 (无副本) 会怎样?
18. 主挂了从库多久上位?
19. 集群分区 (脑裂) 时写入?
20. gossip 一条消息能带几个节点?

# 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 2011 (3.0 计划) | Redis Cluster 设计 (antirez 设计文档; cluster.h 版权) |
| 3.0 | **Redis Cluster 发布**: 16384 槽/CRC16/MOVED/gossip/PFAIL-FAIL/configEpoch |
| 3.2 | 槽迁移完善 (MIGRATE/ASK); CLUSTER SETSLOT |
| 4.0 | cluster-allow-reads-when-down; 总线改进 (TLS 支持) |
| 5.0 | cluster replica 提升优化 (failover_auth_time) |
| 6.0 | CLUSTER SHARDS 雏形; 集群客户端缓存 (R-17 交叉) |
| 7.0 | **cluster_legacy.c 拆分** (cluster.c 新框架 + legacy 保留); CLUSTER SHARDS/MYSHARDID; bumpConfigEpoch 无共识 |

## 痕迹证据

- cluster.h:37-42: 槽散列设计注释 ("least significant 14 bits of the crc16")
- cluster.h:40-42: {tag} 用途注释 ("force certain keys to be in the same node")
- cluster_legacy.c:617-619: SETSLOT MIGRATING/IMPORTING 标记
- cluster.c:1193-1201: MOVED/ASK 回复格式 (槽+地址)
- cluster_legacy.c:4634+: clusterCron 全节点周期处理 (PFAIL 统计)
- cluster.h:16-23: 七种 CLUSTER_REDIR 枚举

## 推断标注

- "Gossip O(log n) 轮收敛" — 理论推断 (随机子集传播)
- "3.0/3.2/4.0/6.0/7.0 版本" — 版本推断 (Redis 版本史; 浅克隆无法 git 验证)
- "PFAIL 报告数 > 半数主" — 需 grep 确认精确条件
- "总线 port+10000" — 常量名需 grep (CLUSTER_PORT_INCR)
