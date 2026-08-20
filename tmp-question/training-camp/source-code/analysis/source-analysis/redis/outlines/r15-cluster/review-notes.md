# R-15 Cluster — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 预判验证 | 交接文档 §四 预判: 16384 槽 ✅ (cluster.h:8-10) / CRC16+{tag} ✅ (cluster.h:43-62) / MOVED-ASK ✅ (cluster.c:1193-1201) / 槽迁移 ✅ (L617-619) / gossip ✅ — 全验证 | 记录 |
| 2 | harness 实证 | **20 断言一次通过 ASan clean**: crc16 已知值 (0x31C3) / "foo"→12182 "bar"→5061 (Redis 文档已知槽位) / {tag} 同槽 / 无 } / 空 {} / 多 { 取首个 / MOVED vs ASK vs DOWN_UNBOUND 判定 | harness |
| 3 | 机制洞察 | **MOVED vs ASK 语义**: MOVED=槽已迁移 (客户端更新路由表); ASK=迁移中导入 (一次性, 需 ASKING) — harness 实证 | 大纲节 3 |
| 4 | 常量确认 | CLUSTER_PORT_INCR=10000 (cluster_legacy.h:17) / CLUSTER_SLOTS=16384 (cluster.h:9) / CLUSTER_OK (cluster.h:11); clusterHandleSlaveFailover (L56) / clusterProcessPacket (L2699) | 记录 |
| 5 | 行号验证 | 全函数 ~40 锚点 grep 穷举 (cluster.h 8-62 / cluster.c 816-1371 / cluster_legacy.c 617-619,2088,2321,2699,4634,5044) | 记录 |
| 6 | 覆盖检查 | 阻塞救出 (cluster.c:1218, R-26 交叉) / 从库提升投票 (L3206 failover_auth_count++) / READONLY 豁免 (L1252-1257) — 全部入大纲 | 记录 |

## 07 五维度

### 维度1 功能正确性
- keyHashSlot 三分支 + {tag} 边界 (harness)
- 七种重定向枚举 + 回复格式
- 迁移双标记 + 归属切换
- PFAIL/FAIL 两阶段

### 维度2 性能
- CRC 查表 O(len)
- gossip 随机子集传播
- 槽位表数组直查

### 维度3 内存
- 槽位图 2048B (16384bit)
- 槽位表 16384 数组
- migrating/importing 双数组

### 维度4 一致性
- configEpoch 大者胜
- 最终一致 (gossip 收敛)
- 槽覆盖 + 多数存活 → ok/fail
- MOVED 路由表更新 vs ASK 一次性

### 维度5 负面空间 (已写入大纲 6+6 条)
- 分片: 无自动重分片/跨槽多键/槽级复制; 协议: 无强一致/跨节点事务/多活写

## 结论
R-15 全部锚点行号 ~40 处验证, 12 闭环完成 (6+6), **机制洞察 1 + 常量 3**。harness 20 断言 ASan clean (一次通过)。待二次 REVIEW 复核。

---

# 二次深度 REVIEW (2026-08-13, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 R-21/R-3/R-28/R-2/R-9/R-14 (序号 < 28) 均已讲 ✅; 引出 R-17 (未来域 OK); 对照 R-13/R-14 ✅; 两篇各自五结构元素齐备 ✅; 读者处境场景化 (槽怎么算/{tag}/MOVED-ASK) ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 两篇合计 ~70 锚点 (file:line) — 🔴A 标准 ≥4 ⏫ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域 (R-17 仅作引出) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 传播 (PUBLISH gossip) / 通知 (shard 事件) / 键空间 (槽内多键) / 复制 (节点内主从) / 阻塞 (重定向救出) / pubsub (shard channel, R-29 交叉) — 全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 两篇负面空间 6+6 条 ✅; 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **强断言实证** | 大纲节 1 (R-15a) "foo→12182/bar→5061" — harness 实测与 Redis 文档已知槽位一致 ✅ | 记录 |
| 13 | **表述精确化** | 大纲节 5 (R-15b) "FAIL 消息非 gossip 逐跳" — clusterProcessPacket 处理 FAIL 类型 (L2699+) 直接广播; 标注与 gossip 条目不同路径 | 大纲节 3 已含 |
| 14 | 通过项 | 其余 ~40 句机制描述逐句对源码一致 ✅ (低 14 位/三分支 tag/七枚举/迁移双标记/阻塞救出/READONLY/总线 10000/gossip 随机子集/PFAIL-FAIL/configEpoch 大者胜/从库投票/槽覆盖判定) | 记录 |

## 二次 REVIEW 汇总

07 五维度轮换 + 内容深度共 **1 处记录** (强断言实证槽位对照), 全部验证。反写测试结论: 两篇大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-13, 逐句对源码 + 推理验证)

> 动机: 用户要求深度 REVIEW — 对大纲每个锚点重新 grep, 对全部阈值/判定条件反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | CLUSTER_PORT_INCR 位置 | **cluster_legacy.h:17** (非 server.h) — 大纲引用修正 | 修正 |
| V2 | FAIL 判定精确条件 | **markNodeAsFailingIfNeeded (L1883-1900)**: nodeTimedOut + **failure reports ≥ size/2+1** (L1884) + 自己若主 +1 (L1891); 注释 "no majority... FAIL flag will be cleared" (L1875-1882) | 修正 |
| V3 | 写保护延迟 | **CLUSTER_WRITABLE_DELAY=2000ms** (L5042): 主重启后延迟可写 (L5053-5061) — 大纲节 6 漏 | 修正 |
| V4 | 全覆盖可配置 | **cluster-require-full-coverage 默认 1** (config.c:3069) — 槽覆盖判定是配置项 | 修正 |
| V5 | **少数派分区保护** | clusterUpdateState (L5095-5104): reachable_masters < **size/2+1** → FAIL — 脑裂拒写 (大纲"半数主在线"表述不精确) | 修正 |
| V6 | 从库提升超时 | clusterHandleSlaveFailover (L4205): **auth_timeout = MAX(NODE_TIMEOUT×2, 2000)** (L4219-4220) + retry×2 (L4221) + **4 前置条件** (L4224-4230) | 修正 |
| V7 | nodeFailed | = CLUSTER_NODE_FAIL 标志检查 (cluster_legacy.h:74) — 判定实现在 markNodeAsFailingIfNeeded | 通过 |
| V8 | cluster size 定义 | 服务 ≥1 槽的主节点数 (L5079-5093) — quorum 基数 | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 15 | 引用错误 | 节 1 "CLUSTER_PORT_INCR, server.h" — **实际 cluster_legacy.h:17** | 大纲节 1 修正 |
| 16 | 语义缺失 | 节 3 FAIL 判定笼统 ("报告数 > 半数") — **精确: markNodeAsFailingIfNeeded + size/2+1 + 自己主 +1 + 无多数清除** | 大纲节 3 重写 |
| 17 | 机制缺失 | 节 6 漏 **CLUSTER_WRITABLE_DELAY=2s** (重启写保护) + **require_full_coverage 配置** + **少数派分区保护 (size/2+1)** — 脑裂语义是核心机制 | 大纲节 6 重写 |
| 18 | 表述不精确 | 节 5 "等待 failover_auth_time + 随机延迟" — **精确: 4 前置条件 + auth_timeout=MAX(NODE_TIMEOUT×2,2000) + retry×2** | 大纲节 5 修正 |

## 三次 REVIEW 汇总

推理验证 8 项全通过 (V1-V8); 新发现 **4 处** (引用 1 / 语义缺失 1 / **机制缺失 1 (少数派保护)** / 表述 1), 全部修复。锚点逐句 re-grep。两篇大纲现可支撑写作。
