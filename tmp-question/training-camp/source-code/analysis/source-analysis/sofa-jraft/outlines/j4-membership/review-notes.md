# J-4 review-notes — 六层深审记录 (2026-08-15)

## 审法: 探索代理锚点 + 本审抽查 + 纯逻辑 harness

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "CLI 纯 RPC" — CliServiceImpl.java:131-161 addPeer; getLeader L442-494 | 通过 ✅ |
| 2 | 事实 | §2 "isStable = oldConf.isEmpty()" — ConfigurationEntry.java:77-79 | 通过 ✅ |
| 3 | 事实 | §2 "listPeers 并集" — L85-89 | 通过 ✅ |
| 4 | 事实 | §2 "get(index) 二分" — ConfigurationManager.java:88-109 | 通过 ✅ |
| 5 | 事实 | §3 "追平期限 = electionTimeoutMs" — NodeImpl.java:408-411 dueTime | 通过 ✅ |
| 6 | 事实 | §3 "ETIMEDOUT 近期有 RPC 重新追平" — L2309-2318 | 通过 ✅ |
| 7 | 事实 | §4 "四阶段 + 两条日志" — L506-533 (JOINT L509-514/STABLE L516-518/ELEADERREMOVED L520-526) | 通过 ✅ |
| 8 | 事实 | §4 "双配置 quorum appendPendingTask" — L2484 | 通过 ✅ |
| 9 | 事实 | §5 "并发 EBUSY / 相等成功" — L2516-2527 | 通过 ✅ |
| 10 | 事实 | §6 "reset(ECATCHUP) + 无回滚日志" — L448, L451-474 | 通过 ✅ |
| 11 | 事实 | §6 "flush 新 leader 重写" — L490-504 | 通过 ✅ |
| 12 | 事实 | §7 "变更中拒绝转移" — L3323-3340 | 通过 ✅ |
| 13 | 事实 | §7 "findTheNextCandidate lastLogId 最大" — L3347 | 通过 ✅ |
| 14 | 事实 | §7 "learner 添加不追平/提升才追平" — L419-429 + CliServiceImpl L355-362 | 通过 ✅ |
| 15 | 事实 | §5 "变更中让位 → confCtx.reset → EPERM" — L1333, L469-471 | 通过 ✅ |
| 16 | **事实修正** | 任务提示 "ConfigurationChangeContext" **不存在** — 实为 NodeImpl 私有内部类 ConfigurationCtx (L332-538) | ✅ 大纲/KP 均用 ConfigurationCtx, 无错误引用 |
| 17 | **事实修正** | 任务提示 "isConfChangeAllowed" **不存在** — 职责由双配置判定+防护+让位承担 | ✅ 大纲明确"无此方法" (KP M5) |
| 18 | 结构 | 负面空间 "不做自动故障摘除" 与手动 removePeer 自洽 | 通过 ✅ |

**结论**: 18 项核对 0 修正 (2 项外部假设被源码推翻, 大纲/KP 均已按实际实现撰写)。harness 验证四阶段/双 quorum/回滚/让位。

## harness 设计 (MiniMembership — 纯逻辑)

- A. isStable 语义: oldConf 空 = stable
- B. 四阶段: CATCHING_UP → JOINT → STABLE 顺序推进
- C. 双 quorum: 新老配置各自多数才提交配置日志
- D. 追平失败回滚: 新 peer 未追平 → ECATCHUP, 无配置日志落盘
- E. ELEADERREMOVED: 合并完成后被移除的 leader 让位
