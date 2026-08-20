# Z-1 Leader 选举 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **表述精确化** | 执行计划 "logicalclock+recvset/outofelection+totalOrderPredicate" — 补全为**四要素轮次 (logicalclock) + 双集合 (recvset 当前轮裁决 / outofelection 历史学习) + 三要素全序 (epoch>zxid>sid) + weight 排除** | 大纲 §1-4 |
| 2 | **补充锚点** | **finalizeWait=200ms 稳定窗口**: 达成多数后仍 poll — 更高票放回重来 (L1050-1055) — 防瞬间多数抖动 | 大纲 §3 |
| 3 | **补充锚点** | **指数退避 200ms→60s 共 10 档** (harness 实证: 51200→60000 钳制) | 大纲 §2 |
| 4 | **语义标注** | **ZOOKEEPER-3922 双行为分离**: FOLLOWING/LEADING 分开处理, 2 节点 majority=2 无法达成 → QuorumOracleMaj 裁决 (L1073-1097 长注释) | 大纲 §4 |
| 5 | 行号验证 | 全函数 25 锚点 + 跨文件 6 处 grep (FastLeaderElection 112-749,849-908,957-1164 / QuorumPeer / QuorumCnxManager) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 三要素全序确定性
- 换轮清集
- 双视图多数 (reconfig)

### 维度2 性能
- 指数退避 (200ms→60s)
- 双队列异步 (Messenger 双线程)
- 稳定窗口 200ms

### 维度3 内存
- recvset/outofelection HashMap
- sendqueue/recvqueue

### 维度4 一致性
- logicalclock 轮次权威
- zxid 数据新鲜优先
- weight 加权

### 维度5 负面空间 (已写入大纲 7 条)
- 不 term 持久化/不随机超时/不 prevote/不租约/不绝对防脑裂/不自动成员变更

## 结论
Z-1 全部锚点 ~25 处验证, 8 闭环完成, **表述精确化 1 + 补充锚点 2 + 语义标注 1**。harness 4/4 (自抓 1 缺陷)。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置对照 (K-9/RM-12/jraft) ✅; 引出 Z-2 ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 7 条 ✅; 横切 (共识/并发/一致性/算法) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §2 未提 **haveDelivered()/connectAll() 分支**: 有已投递连接 → 重发通知; 无 → 先建连接 (L959-963) — 连接面与通知面耦合 | 大纲 §2 补注 |
| 8 | 通过项 | 其余 ~25 句机制描述逐句对源码一致 ✅ (四元组/28B-40B/轮次++/自荐/退避/换轮清集/稳定窗口/双集合/Oracle/observer) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (连接分支 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 三要素全序数学 | epoch>zxid>sid 严格偏序 → 任意两票可比 → 收敛 ✅ (harness A) | 通过 |
| V2 | 退避序列 | 200<<1 ×9 → 51200 → 60000 钳制, 共 10 档 ✅ (harness B) | 通过 |
| V3 | 换轮清集 | 新 epoch 票 → 旧轮 recvset 失效 → 清空重来 — 多数不可跨轮 ✅ (harness C) | 通过 |
| V4 | 稳定窗口收敛 | 窗口内更高票放回 → 下一轮次 majority 仍达成 → 最终当选 — 有界 ✅ (harness D) | 通过 |
| V5 | weight 语义 | weight==0 排除 + 加权多数 (QuorumVerifier.getWeight) — 异构集群多数按权重 ✅ | 通过 |
| V6 | 2 节点 Oracle | majority=2 永不达成 → Oracle 单向授权 — 2 节点可用性例外 ✅ | 通过 |
| V7 | 双集合隔离 | recvset 只收当前轮 LOOKING; outofelection 收历史轮 FOLLOWING/LEADING — 无交叉污染 ✅ | 通过 |
| V8 | 协议兼容 | 28B (无 peerEpoch) → 40B (peerEpoch+version) — 三代集群混合可行 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **QuorumOracleMaj 是接口可插拔** (QuorumOracleMaj 类 + revalidateVoteset) — 2 节点例外是扩展点非硬编码 | 大纲 §4 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (Oracle 可插拔), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (leaveInstance 动作/checkLeader/连接面隔离/QuorumVerifier 权重/回退重置/Oracle 授权方向), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | leaveInstance 动作? | L1064: leaveInstance(endVote) → 终止当前选举线程 + 更新 QuorumPeer 状态 — 后续 Z-2 lead/follow | 通过 (验证) |
| T2 | checkLeader 校验? | L1154 调用: recvset 中多数投同一 leader + leader 有效 — FOLLOWING 路径的多数验证 | 通过 (验证) |
| T3 | 连接面隔离? | QuorumCnxManager 独立线程 (WorkerReceiver pollRecvQueue 3000ms) — 选举与广播共用通道但隔离队列 | 通过 (验证) |
| T4 | QuorumVerifier 权重? | totalOrderPredicate 用 getWeight (L732); SyncedLearnerTracker.hasAllQuorums 加权多数 (L761-767) | 通过 (验证) |
| T5 | 退避重置? | 收到有效票后 notTimeout 未显式重置? — 看 L958-978: 仅 null 分支递增; 收到票分支走 switch — **需确认重置** ⚠ | 通过 (标注) |
| T6 | Oracle 授权方向? | L1093-1094: Oracle 拒绝 = 已存在 leader 有效 (授权新主失败即维持现状) — 单向 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 选举收敛性 | 全序 + 多数 + 稳定窗口 → 有限轮内收敛 (无活锁 — 退避递增防广播风暴) ✅ | 通过 |
| V2 | 数据安全 | zxid 高者优先 → 新主至少包含多数派最新数据 ✅ | 通过 |
| V3 | 换轮正确性 | 新轮票清旧轮 → 旧轮多数不污染新轮 ✅ | 通过 |
| V4 | Observer 无票权 | OBSERVING 忽略 → observer 不参与多数计算 ✅ | 通过 |
| V5 | reconfig 安全 | 双 QuorumVerifier 多数 → 新旧配置交集可达成 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **语义标注** | **退避重置语义**: notTimeout 仅在超时分支递增 (L957-978); 收到票分支不重置 — 退避是"连续无票"计数器 (连续超时次数), 非每次收票重置 — 与 harness B 的"重置"简化需区分 ⚠ | 大纲 §2 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 投票结构 (四元组/双队列/协议兼容) — 可写 ✅
- §2 提议广播 (轮次/自荐/退避/连接分支) — 可写 ✅
- §3 收票判定 (全序/多数/稳定窗口) — 可写 ✅
- §4 双集合 (recvset/outofelection/Oracle) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (退避重置语义 — 连续无票计数器). 大纲经修复后反写测试全过。
