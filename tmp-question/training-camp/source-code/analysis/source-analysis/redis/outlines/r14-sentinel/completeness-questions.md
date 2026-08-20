# R-14 Sentinel — completeness-questions + 时空溯源

## R-14a 监控 — 开发者视角

1. sentinelRedisInstance 的三角结构?
2. 命令链接和 pubsub 链接的区别?
3. PING/INFO/PUBLISH 各为什么?
4. INFO 频率怎么动态调整?
5. SDOWN 的三条件?
6. ODOWN 的 quorum 怎么数?
7. hello 消息内容?
8. TILT 模式什么时候进入?

## R-14a 监控 — 架构师视角

9. SDOWN vs ODOWN 的设计意图?
10. "weak quorum" 注释的含义?
11. hello 自动发现的收敛性?
12. 双链接分离的容错价值?
13. TILT 保护的动机 (时钟/阻塞)?
14. 10s→1s 的频率缩放?
15. 配置自写的持久化策略?
16. 为什么哨兵间无强一致?

## R-14a 监控 — 学生视角

17. 主挂了一个哨兵看到什么 (SDOWN)?
18. 什么条件触发 ODOWN (quorum)?
19. 新哨兵怎么发现其他哨兵?
20. 哨兵进程被卡 3 秒会怎样 (TILT)?

## R-14b 故障转移 — 开发者视角

1. failover_epoch 怎么递增?
2. 每 epoch 投票规则?
3. 选主的三维排序?
4. 7 状态机的顺序?
5. SLAVEOF NO ONE 是什么?
6. parallel_syncs 控制什么?
7. 重配从库的状态推进 (SENT→INPROG→DONE)?
8. abort 的触发条件?

## R-14b 故障转移 — 架构师视角

9. 领导者选举与 Raft 的异同 (对照 R-15)?
10. 确定性选主的意义 (所有哨兵同结果)?
11. 状态机每态超时/中止的设计?
12. parallel_syncs 限流的动机?
13. 2×failover_timeout 冷却?
14. hello 传播的最终一致收敛?
15. 强制 FAILOVER 的用途?
16. failover_start_time 随机化 (MAX_DESYNC)?

## R-14b 故障转移 — 学生视角

17. 5 哨兵 quorum=3, 主挂了几票才 ODOWN?
18. 选主时 priority 相同看什么?
19. 转移期间新主收到写命令?
20. 转移失败 (无好从库) 会怎样?

# 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 2012 (2.4?) | Sentinel 雏形 (sentinel.c 版权 2011-Present; 早期监控哨兵) |
| 2.8 | **Sentinel 2 正式发布**: 主/从/哨兵三角 + SDOWN/ODOWN + 自动故障转移 + 配置自写 |
| 3.0 | Sentinel 3: 哨兵间 hello 自动发现 + 配置传播 (epoch 收敛) |
| 3.2 | 选主改进 (priority/offset/runid 三维) + parallel_syncs |
| 4.0 | SLAVEOF 兼容 REPLICAOF; 转移状态机完善 |
| 6.2 | SENTINEL CONFIG SET/GET; master-reboot-down-after-period (重启保护) |
| 7.x | 稳定期: 无重大重构 (模块化 vs Cluster 对照) |

## 痕迹证据

- L5418-5436: TILT 模式完整注释 (进入条件/影响/恢复)
- L4584-4589: ODOWN "weak quorum" 注释 (权威语义)
- L4981-4994: 选主候选过滤注释 (5 条件 + 排序键)
- L4927-4938: failover_start_time 随机化 (MAX_DESYNC) 注释
- L5101-5104: election_timeout = min(ELECTION_TIMEOUT, failover_timeout)
- L5165-5166: 提升确认靠 INFO 角色注释
- L5263-5266: 重配超时强制注释 (哨兵自愈)

## 推断标注

- "2.4/2.8/3.0 版本" — 版本推断 (Sentinel 演进史; 仓库浅克隆无法 git 验证)
- "SENTINEL_MAX_DESYNC / tilt_trigger / tilt_period 具体值" — 需 grep 确认常量
- "默认 quorum 1 / down_after_period 30s" — 配置默认值推断 (需 grep config)
