# R-15b Cluster 协议 — Gossip 与故障转移

> 前置: [[R-15a-cluster]] (槽散列) + [[R-2-events]] (事件循环) + [[R-14-sentinel]] (判活对照) + [[R-9-replication]] (节点内复制) | 引出: [[R-17-client-caching]] | 对照: [[R-14-sentinel]] (哨兵投票 vs 集群 PFAIL/FAIL)
> 🔴 A (拆篇 2/2) | 6 KP | [模式: 集群总线 + Gossip 扩散 + 故障检测 + 配置纪元 + 无中心]
> Pass 2 闭环: q1(集群总线) q2(Gossip) q3(故障检测) q4(configEpoch) q5(从库提升) q6(状态机)

**读者处境**: 节点间怎么通信? 一个节点挂了大家怎么知道? 配置怎么一致? 这篇拆集群协议: 总线消息、Gossip 扩散、PFAIL/FAIL、configEpoch 裁决、从库提升。

### 1. 集群总线 — 独立端口

场景: 节点间的通信通道?
源码路径:
- **Cluster Bus**: 每个节点监听 **port+10000** 端口 (**CLUSTER_PORT_INCR, cluster_legacy.h:17**) — 与客户端端口分离
- 消息类型: PING/PONG/MEET/FAIL/PUBLISH/UPDATE/MIGRATE/ASKING/SHARD...
- **clusterAcceptHandler** (cluster_legacy.c:1232): 总线连接接受; clusterLink 双向
- 消息头: clusterMsg (sender/epoch/槽位位图 2048B=16384bit)
- clusterBeforeSleep (L1334 区域): 周期批处理
关键设计 (q1): **带外总线**: 协议数据不占客户端连接 — 独立拓扑 (全连接)。[模式: 独立总线]
数据流: 节点 → 总线 socket → PING/PONG 全互连。

### 2. Gossip — 节点状态扩散

场景: 一个节点的信息怎么传到全网?
源码路径:
- **clusterSendPing** (L4634+ clusterCron 内): 每周期发 PING — 携带 **gossip 条目** (随机节点子集 + 已知节点)
- **clusterProcessGossipSection** (L2088-2200): 收 PING/PONG → 逐条处理:
  - 未知节点 → 加入 (自动发现, L2160 区域)
  - 已知节点 → 更新 ping_sent/pong_received/失败报告
  - 失败报告: PFAIL 计数 (failure reports)
- gossip 消息格式: clusterMsgDataGossip (nodename/ip/port/flags)
- 握手: clusterStartHandshake (MEET) / clusterAcceptHandler
关键设计 (q2): **部分传播全收敛**: 每条消息带随机子集, 多轮后全网一致 — O(log n) 轮扩散。[模式: Gossip 扩散]
数据流: PING(带 gossip) → 对方处理 → PONG(带 gossip) → 收敛。

### 3. 故障检测 — PFAIL/FAIL

场景: 节点挂了怎么共识?
源码路径:
- **PFAIL (疑似)**: 单节点视角 — 超 node_timeout 无 PONG → PFAIL 标记
- **FAIL (确认)**: **markNodeAsFailingIfNeeded** (L1883-1900): nodeTimedOut + **failure reports ≥ size/2+1** (L1884, 自己若主 +1 票 L1891); 权威注释 L1875-1882: **"no majority... FAIL flag will be cleared"**; 无多数则 FAIL 被清除
- **clusterUpdateState** (L5044): 集群可用性 — 槽覆盖 + 半数主在线; **cluster_state=ok/fail**
- FAIL 消息: 广播 FAIL (非 gossip 逐跳)
- 对照 R-14: Sentinel 的 SDOWN/ODOWN vs 集群 PFAIL/FAIL — 同构判活 (注释印证)
关键设计 (q3): **两阶段判活**: PFAIL 本地 + FAIL 全网 (对照哨兵 SDOWN/ODOWN) — gossip 计数替代投票。[模式: 两级判活]
数据流: 超时 → PFAIL → gossip 扩散报告 → 多数 → FAIL。

### 4. configEpoch — 配置纪元

场景: 槽归属冲突怎么裁决?
源码路径:
- **configEpoch**: 每节点配置纪元 (槽位图版本) — 冲突时大者胜
- **clusterUpdateSlotsConfigWith** (L2321-2400): 收 UPDATE/槽位图 → configEpoch 比较 → 接受/拒绝
- **clusterHandleConfigEpochCollision** (L1765): 相同 epoch 冲突 → 递增解决
- **clusterBumpConfigEpochWithoutConsensus** (L2216 区域): 无共识提升 (7.0 新)
- 槽位图: clusterMsg 携带 2048B 位图 (16384bit)
关键设计 (q4): **大 epoch 胜**: 无中心配置 — 冲突靠版本号裁决 (对照 R-14 epoch)。[模式: 版本裁决]
数据流: 槽变更 → 广播 UPDATE/位图 → 各节点比 epoch → 收敛。

### 5. 从库提升 — 集群内故障转移

场景: 主挂了自己从库怎么上位?
源码路径:
- **clusterHandleSlaveFailover** (L4205): 前置条件 4 条 (L4224-4230: 从库/主 FAIL 或手动/非 nofailover/服务槽); **auth_timeout = MAX(NODE_TIMEOUT×2, 2000ms)** (L4219-4220) + **retry = timeout×2** (L4221) + 随机延迟
- **请求投票**: FAILOVER_AUTH_REQUEST 消息 → 主节点投票 (一 epoch 一票) → FAILOVER_AUTH_ACK
- **clusterBumpConfigEpoch** / failover_auth_count 统计 → 多数 → 提升 + SLAVEOF NO ONE
- 对照 R-14: 与哨兵选举同构 (epoch + 投票) — 集群内建 vs 外部哨兵
关键设计 (q5): **内建选举**: 无需外部哨兵 — 从库直接投票提升 (复制面 R-9 复用)。[模式: 从库提升]
数据流: 主 FAIL → 从库请求投票 → 多数 → 提升 + 广播。

### 6. 状态与安全 — cluster_state

场景: 集群什么时候拒绝服务?
源码路径:
- **clusterUpdateState** (L5044-5130): 三层判定:
  - **CLUSTER_WRITABLE_DELAY=2s** (L5042): 主节点重启后延迟才可写 (L5053-5061)
  - **cluster-require-full-coverage 可配置** (config.c:3069, 默认 1): 槽全覆盖检查 (L5063-5073)
  - **少数派分区保护** (L5095-5104): reachable_masters < size/2+1 → FAIL — **脑裂时少数派拒写**
- **cluster_state_ok/fail** → **CLUSTERDOWN** (cluster.c:1234) — 写拒绝
- **CLUSTERDOWN 读取豁免**: cluster-allow-reads-when-down 配置
- 槽覆盖检查: clusterCoveredSlots (L5044 区域)
- 部分故障: 槽缺失 → fail (无冗余主)
关键设计 (q6): **可用性 = 槽覆盖 + 多数存活**: 部分节点故障容忍 (有副本), 槽空档即 down。[模式: 可用性判定]
数据流: 节点状态变化 → 槽覆盖计算 → ok/fail → 命令允许/拒绝。

### 负面空间 — 集群协议刻意不做的事

- **不做强一致**: 最终一致 (gossip 收敛, 无同步提交)
- **不做跨节点事务**: 槽内多键才原子 (CROSSSLOT 拒)
- **不做无中心分片**: 槽手动分配 (16384 固定)
- **不做多活写**: 单主写 (从库只读+READONLY 例外)
- **不做消息压缩**: 全量位图 (2048B 固定)
- **不做脑裂防写**: 分区时无 quorum 写拒绝 (与哨兵同)

→ 引出: 客户端侧缓存怎么在集群下失效? → [[R-17-client-caching]]
