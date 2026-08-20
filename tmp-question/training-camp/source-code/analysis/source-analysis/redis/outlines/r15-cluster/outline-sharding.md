# R-15a Cluster 分片 — 槽散列与客户端路由

> 前置: [[R-21-db]] (kvstore 分片) + [[R-3-dict]] (哈希) + [[R-28-networking]] (客户端) | 引出: [[R-15b-cluster]] (集群协议) | 对照: [[R-13-geo]] (散列对照) + [[R-14-sentinel]] (判活)
> 🔴 A (拆篇 1/2) | 6 KP | [模式: 槽散列 + 哈希标签 + 重定向路由 + 迁移状态机]
> Pass 2 闭环: q1(槽散列) q2(哈希标签) q3(重定向) q4(槽迁移) q5(阻塞重定向) q6(命令面)

**读者处境**: 16384 槽怎么算? {tag} 是什么? MOVED 和 ASK 什么区别? 这篇拆分片: CRC16 槽散列、哈希标签、七种重定向、槽迁移状态机。

### 1. 槽散列 — CRC16 低 14 位

场景: 一个键落在哪个节点?
源码路径:
- **16384 槽** (cluster.h:8-10): CLUSTER_SLOT_MASK_BITS=14 → CLUSTER_SLOTS=1<<14=16384 → 掩码 0x3FFF (低 14 位)
- **keyHashSlot** (cluster.h:43-62): **crc16(key) & 0x3FFF** (L50) — 权威注释 L37-42 "least significant 14 bits of the crc16"
- **crc16 算法** (crc16.c:82-88): 标准 CRC-16/XMODEM (多项式 0x1021, 查表) — harness 实证 "123456789"→0x31C3
- 已知值对照 (harness): "foo"→槽 12182 / "bar"→槽 5061
- 槽位表: clusterState.slots[16384] → 节点 (getNodeBySlot)
关键设计 (q1): **低 14 位取模**: 65536 种 CRC 值映射 16384 槽 — 均匀分布 (CRC 特性)。[模式: 槽散列]
数据流: key → crc16 → &0x3FFF → 槽 → 节点。

### 2. 哈希标签 — {tag}

场景: 多个键怎么强制同槽 (事务/管道)?
源码路径:
- **{tag} 规则** (cluster.h:43-62): ① 无 { → 全键哈希 (L50) ② 有 { 无 } 或空 {} → 全键 (L57) ③ 有 → **只哈希 {} 之间** (L61)
- 用途: 多键命令 (MGET/MSET/Lua) 要求同槽 — 注释 L40-42 "force certain keys to be in the same node"
- 多 { 场景: 取第一个 { 到第一个 } (harness 实证 "x{a}y{b}z" → 哈希 "a")
关键设计 (q2): **显式共位**: 业务控制的散列域 — 事务/管道/脚本的跨键前提。[模式: 哈希标签]
数据流: key → 找 { → 找 } → tag 哈希 / 全键哈希。

### 3. 重定向 — 七种 CLUSTER_REDIR

场景: 键不在本节点怎么办?
源码路径:
- **七种重定向** (cluster.h:16-23): NONE(可服务)/CROSS_SLOT(-CROSSSLOT)/UNSTABLE(-TRYAGAIN)/ASK/ MOVED/DOWN_STATE(-CLUSTERDOWN)/DOWN_UNBOUND(-CLUSTERDOWN 槽无主)
- **clusterRedirectClient** (cluster.c:1179-1205): **MOVED/ASK 带槽号+节点地址** (L1193-1201) — 客户端据此重发
- 判定入口: getNodeByQuery (processCommand 调用) → 槽归属 + 迁移状态
- TLS 端口选择 (L1196-1197); preferred endpoint (L1201)
- **MOVED vs ASK 语义**: MOVED = 槽已迁移完成 (永久重定向, 客户端更新路由); ASK = 迁移中 (一次性, ASKING 后访问) — harness 实证
关键设计 (q3): **协议级路由**: 服务端不发数据发地址 — 客户端智能 (redis-cli -c 自动跟随)。[模式: 重定向路由]
数据流: 命令 → 槽归属 → 非本节点 → MOVED/ASK 槽 地址。

### 4. 槽迁移 — migrating/importing

场景: 重新分片 (resharding) 怎么进行?
源码路径:
- **migrating_slots_to[16384] / importing_slots_from[16384]** (cluster_legacy.c:617-619, server.h): 槽迁移双向标记
- **CLUSTER SETSLOT <slot> MIGRATING <node>** (L617): 源节点标记导出
- **CLUSTER SETSLOT <slot> IMPORTING <node>** (L619): 目标节点标记导入
- **CLUSTER SETSLOT <slot> NODE <node>** (L618 区域): 迁移完成 → 槽归属更新 (clusterAddSlot/DelSlot)
- 迁移期间: 键数据搬移 (MIGRATE 命令逐键) + 源节点对缺失键返回 **ASK 重定向**
- clusterUpdateSlotsConfigWith (L2321): 槽配置传播 (configEpoch 裁决)
关键设计 (q4): **双标记 + 归属切换**: MIGRATING/IMPORTING 期间双向服务, 完成后 NODE 交接。[模式: 迁移状态机]
数据流: SETSLOT MIGRATING → 逐键 MIGRATE → SETSLOT NODE → 广播。

### 5. 阻塞重定向 — 客户端救出

场景: BLPOP 阻塞中槽被迁走怎么办?
源码路径:
- **clusterRedirectBlockedClientIfNeeded** (cluster.c:1218-1278): 阻塞客户端 (LIST/ZSET/STREAM/MODULE) 的槽已不归本节点 → **发送 MOVED/DOWN 并解阻** (L1262-1273)
- 集群 down 时直接 CLUSTERDOWN (L1233-1236, 注释: 写会解阻但不会来)
- **READONLY 豁免** (L1252-1257): 只读客户端访问从库副本槽 → 允许
- 对照 R-26: 阻塞框架 + 集群迁移的交叉 (防永久阻塞)
关键设计 (q5): **迁移 ≠ 永久阻塞**: 槽走后阻塞客户端立即被重定向 — 集群语义下的阻塞安全。[模式: 阻塞救出]
数据流: 阻塞中 → 槽迁移 → 解阻 + MOVED。

### 6. 命令面 — CLUSTER 子命令

场景: 运维怎么管理集群?
源码路径:
- **clusterCommand** (cluster.c:816): 主命令分派 — MYID/MYSHARDID/SLOTS/SHARDS/INFO/NODES/COUNTKEYSINSLOT/GETKEYSINSLOT/ADDSLOTS/DELSLOTS/SETSLOT/FAILOVER...
- CLUSTER SLOTS (L1371): 槽→节点映射 (redis-cli 路由依据)
- CLUSTER SHARDS (cluster_legacy.c:5711): 7.0 新版 (shard 概念: 主+从组)
- CLUSTER SETSLOT: MIGRATING/IMPORTING/NODE/STABLE
- CLUSTER FAILOVER: 从库主动提升 (R-14 对照)
关键设计 (q6): **管理即协议**: 槽操作全走 CLUSTER 命令 — 路由表是集群的分布式状态。[模式: 命令面]
数据流: CLUSTER SETSLOT → 状态变更 → gossip 广播 → 全集群一致。

### 负面空间 — 分片刻意不做的事

- **不做自动重分片**: 槽迁移需手动/工具驱动 (redis-cli --cluster reshard)
- **不做跨槽多键**: 非 tag 多键命令拒 (CROSSSLOT)
- **不做槽级复制**: 复制按节点 (主从整节点)
- **不做热迁移**: 迁移期间 ASK/TRYAGAIN (多键不稳)
- **不做 tag 嵌套解析**: 取第一个 {} 对

→ 引出: 槽信息怎么在节点间传播? → [[R-15b-cluster]]
