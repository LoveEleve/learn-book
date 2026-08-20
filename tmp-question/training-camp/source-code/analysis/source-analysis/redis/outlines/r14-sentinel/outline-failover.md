# R-14b Sentinel 故障转移 — 选举与状态机

> 前置: [[R-14a-sentinel]] (ODOWN 触发) + [[R-9-replication]] (SLAVEOF 重定向) | 引出: [[R-15-cluster]] (对照: Raft 变体) | 对照: [[R-9-replication]] (复制面复用)
> 🔴 A (拆篇 2/2) | 6 KP | [模式: 领导者选举 + 状态机转移 + 并行重配 + 配置收敛]
> Pass 2 闭环: q1(选举) q2(选主) q3(状态机) q4(重配) q5(收敛) q6(保护)

**读者处境**: 多个哨兵同时想转移怎么办? 谁是新主? 其他从库怎么切过去? 这篇拆故障转移: 领导者选举、选主排序、7 状态机、并行 SLAVEOF、配置收敛、中止保护。

### 1. 领导者选举 — epoch 投票

场景: 多个哨兵都发现 ODOWN, 谁说了算?
源码路径:
- **sentinelStartFailover** (L4927-4938): `failover_epoch = ++sentinel.current_epoch` (L4932) + SRI_FAILOVER_IN_PROGRESS + failover_start_time 随机化 (L4936, SENTINEL_MAX_DESYNC 防同时)
- **sentinelGetLeader 胜者双条件** (L4773-4830): voters = 其他哨兵+1 (L4787); 数票 leader_epoch==current_epoch (L4790-4794); **绝对多数 voters/2+1 (L4808) + ≥master->quorum (L4811)** — 双门槛缺一不选 (L4809-4812); 自己投领先者或自己 (L4814-4823)
- **sentinelLeaderIncr** (L4762): runid → 票数
- 投票协议: sentinelVoteLeader / sentinelRequestLeaderVote — is-master-down-by-addr 携带 epoch, **每个 epoch 每哨兵只投一票**
- sentinelFailoverWaitStart (L5087-5118): **+elected-leader 才继续**; 非领导等 election_timeout (min(SENTINEL_ELECTION_TIMEOUT, failover_timeout)) 后中止 (L5098-5110)
关键设计 (q1): **epoch 递增 + 每 epoch 一票**: 单调 epoch 防旧票复用 — Raft 式任期 (对照 R-15)。[模式: 领导者选举]
数据流: ODOWN → ++epoch → 请求投票 → 票数>半数 → elected-leader。

### 2. 选主 — 优先级/偏移/runid

场景: 哪个从库配当新主?
源码路径:
- **sentinelSelectSlave** (L5041+): 候选过滤 (L4981-4994 注释): 非 SDOWN/ODOWN/断线 + ping 5×周期 + info 3×周期 + 断连 ≤ (s_down_since + down_after×10) + **slave_priority > 0**
- **compareSlavesForPromotion 排序** (L5013-5039): **priority 小 → repl_offset 大 → runid 字典序小** (L5018-5038)
- sentinelFailoverSelectSlave (L5120-5137): **+selected-slave** + SRI_PROMOTED
- max_master_down_time (L5048-5052): s_down_since + down_after×10 上限
关键设计 (q2): **三维排序**: 管理员优先级优先, 数据新者次之, runid 兜底 — 确定性选择 (所有哨兵算出同一结果)。[模式: 确定性选主]
数据流: 候选过滤 → qsort 三维排序 → 首个 = 新主。

### 3. 状态机 — 7 态转移

场景: 故障转移的完整生命周期?
源码路径:
- **状态枚举 7 值** (sentinel.c:89-95): NONE(0)/WAIT_START(1)/SELECT_SLAVE(2)/SEND_SLAVEOF_NOONE(3)/WAIT_PROMOTION(4)/RECONF_SLAVES(5)/UPDATE_CONFIG(6) — **⚠ DETECT_END 不是状态**, 是 RECONF_SLAVES 态的结束检测函数 (L5176, 全部 RECONF_DONE 或超时 → UPDATE_CONFIG)
- **sentinelFailoverStateMachine** (L5310): 按当前态分派处理函数 (L5087-5338)
- SEND_SLAVEOF_NOONE (L5139-5163): **SLAVEOF NO ONE 提升** (L5157, 异步, 靠 INFO 确认 L5153-5156)
- WAIT_PROMOTION (L5167-5174): 等 INFO 显示 master 角色 (L5165-5166 注释)
- 超时: 每态超 failover_timeout → sentinelAbortFailover (L5339)
关键设计 (q3): **状态机 = 分布式一致性协议**: 每态有进入条件/超时/中止 — 转移的每一步可验证。[模式: 状态机转移]
数据流: WAIT_START→选举→SELECT→SLAVEOF NO ONE→等提升→重配→结束。

### 4. 并行重配 — parallel_syncs

场景: 其他从库怎么切到新主?
源码路径:
- **sentinelFailoverReconfNextSlave** (L5239-5298): **每轮最多 parallel_syncs 个从库同时重配** (L5254, 默认 1) — 防同时全量同步压垮新主
- sentinelSendSlaveOf (L4859): **SLAVEOF <新主 ip> <port>** (L4859+)
- 从库状态: SRI_RECONF_SENT → SRI_RECONF_INPROG (INFO 显示连接新主) → SRI_RECONF_DONE (INFO 显示已同步)
- 超时强制 (L5263-5266 注释): 过长无进展视为完成, 后续哨兵自愈修正
- **哨兵观察主从关系变更**: sentinelRefreshInstanceInfo 中检测 role 变化 (L2490+)
关键设计 (q4): **并行度控制**: parallel_syncs 限流, 每从库重配可独立完成/超时 — 渐进收敛。[模式: 并行重配]
数据流: 剩余从库 → SLAVEOF 新主 → RECONF 状态机推进。

### 5. 配置收敛 — 切换与传播

场景: 转移完成后配置怎么收敛?
源码路径:
- **sentinelFailoverDetectEnd** (L5176-5235): 全部从库 RECONF_DONE (或超时强制) → **+failover-end** → UPDATE_CONFIG
- **sentinelFailoverSwitchToPromotedSlave** (L5299+): 主实例地址/runid 替换为新主 + **从库身份调整**
- 传播: hello 消息携带新 epoch/新主地址 → 其他哨兵同步收敛 (R-14a 节 5)
- 配置重写: sentinelFlushConfig (L2261) — 监控配置更新落盘
关键设计 (q5): **收敛靠 epoch 广播**: 新主信息经 hello 扩散, 各哨兵按 epoch 大小接受 — 最终一致。[模式: 配置收敛]
数据流: DETECT_END → UPDATE_CONFIG → 本地切换 + hello 广播 → 全体收敛。

### 6. 保护机制 — 中止/冷却/强制

场景: 失败/误判/手动干预怎么办?
源码路径:
- **sentinelAbortFailover** (L5339): 各态超时/无候选 → **-failover-abort-* 事件** + 清状态
- **冷却**: sentinelStartFailoverIfNeeded (L4958-4975): 上次转移后 **2×failover_timeout 内不重试** (L4959-4960)
- **SENTINEL FAILOVER 强制** (L3844 命令面): 无 ODOWN 也转移 (FORCE_FAILOVER 标志, L5098 豁免)
- 从库优先级 0: 永不选 (L4994 注释)
- master 变更检测: sentinelMasterLooksSane (L2481) — 配置合理性检查
关键设计 (q6): **多重保护**: 超时中止 + 2×冷却 + 强制通道 — 自动转移可控可介入。[模式: 保护机制]
数据流: 异常 → abort + 事件; 冷却 → 2×timeout 不重试; FAILOVER → 强制。

### 负面空间 — 故障转移刻意不做的事

- **不做数据补偿**: 转移后从库数据丢失不回补 (RDB 重同步)
- **不做多领导并行**: 每 epoch 唯一领导 (单主)
- **不做跨 epoch 投票复用**: 旧 epoch 票作废
- **不做哨兵间强同步**: 收敛靠最终一致 (hello 传播)
- **不做自动缩容**: 死哨兵配置残留
- **不做跨主转移**: 一次一个主 (主为根)

→ 引出: 16384 槽的分布式分片怎么实现? → [[R-15-cluster]]
