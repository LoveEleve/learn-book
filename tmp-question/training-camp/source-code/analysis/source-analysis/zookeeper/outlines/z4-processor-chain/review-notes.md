# Z-4 Processor 链 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **表述精确化** | 执行计划 "Commit(读写分离)" — 补全: **per-session 队列 + 会话内串行跨会话并行** (CommitProcessor:251-255) — ZK 并发模型核心 | 大纲 §4 |
| 2 | **补充锚点** | **ToBeAppliedRequestProcessor 中间层** (Leader.java:1117): leader 侧"已提交待应用"清单 — Final 必须同步应用 (Z-2 交叉) | 大纲 §1 |
| 3 | **补充锚点** | **outstandingChanges 先行**: getRecordForPath 未提交变更优先 (L165-190) — 读-改-写一致性 | 大纲 §2 |
| 4 | **语义标注** | **multi 原子性在 Prep**: getPendingChanges (ZOOKEEPER-1624 父记录注释 L228-235) + rollbackPendingChanges — 树层无回滚 (Z-3 交叉确认) | 大纲 §2 |
| 5 | 行号验证 | 全函数 30 锚点 + 跨文件 8 处 grep (Prep 137-251,315-674 / Sync 85-232 / Commit 93-349 / Final 146-218,594 / ZooKeeperServer setupRequestProcessors) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 校验链 (session/ACL/quota/路径)
- 提交匹配 (sessionId+cxid)
- throttled 处理

### 维度2 性能
- 读直通 (不等广播)
- per-session 并行
- 批量刷盘 + 随机快照

### 维度3 内存
- outstandingChanges 暂存
- per-session pendingRequests

### 维度4 一致性
- 会话内串行
- 提交顺序匹配
- waitForEmptyPool 边界

### 维度5 负面空间 (已写入大纲 6 条)
- 不全局单线程/不写直通/不请求合并/不读副本路由/不无日志/不事务缓存

## 结论
Z-4 全部锚点 ~30 处验证, 8 闭环完成, **表述精确化 1 + 补充锚点 2 + 语义标注 1**。harness 4/4。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 Z-3/Z-2 ✅; 引出 Z-5 ✅; 对照 RM-5/Redis/ES ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (并发/一致性/管线/事务) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §4 未提 **节流面**: decInProcess (Final L170) + requestFinished + **isThrottled → THROTTLEDOP** (L207-209) — 全局节流计数与 per-session 并行交互 | 大纲 §4 补注 |
| 8 | 通过项 | 其余 ~28 句机制描述逐句对源码一致 ✅ (五节点链/毒丸/校验链/顺序节点/closeSession 清扫/digest 进事务/批量刷盘/读旁路/读直通/提交匹配/应用响应) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (节流面 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 读-改-写一致性 | outstanding 先行 → 同路径后续请求看到未提交变更 ✅ (harness B) | 通过 |
| V2 | 会话内保序 | per-session Deque FIFO — 同会话读写串行 ✅ | 通过 |
| V3 | 提交匹配 | sessionId+cxid 精确匹配 → 本地写 vs 远端写无混淆 ✅ (harness C) | 通过 |
| V4 | 读写边界 | waitForEmptyPool drain 读 → 写提交前读全部完成 ✅ | 通过 |
| V5 | snapCount 数学 | logCount > snapCount/2 + randRoll (randRoll ∈ [0, snapCount/2)) → 快照窗口 [snapCount/2, snapCount] ✅ (harness D) | 通过 |
| V6 | multi 原子性 | Prep 预校验全部子事务 → 失败回滚 outstandingChanges → 树无半应用 ✅ | 通过 |
| V7 | 读旁路正确性 | Sync toFlush 空 → 直通 — 读无需日志 ✅ | 通过 |
| V8 | 毒丸停止 | requestOfDeath 遍历各级队列 → 链关闭有序 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **follower 链变体**: FollowerZooKeeperServer/ObserverZooKeeperServer 重装配 (follower 读面/observer 转发) — 链按角色差异化 | 大纲 §1 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (角色变体链), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (pRequest 主流程/错误事务/check 版本/批量读写交互/全局节流/角色变体), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | pRequest 主流程? | L157: pRequest(request) — checkSession + pRequest2Txn; 失败 → ErrorTxn (异常捕获生成错误事务) | 通过 (验证) |
| T2 | 错误事务语义? | Final: hdr type==error → 抛 ErrorTxn 错误码 (L177-191); 本地异常 (request.getException) 优先 | 通过 (验证) |
| T3 | check OpCode? | L616-626: 版本检查事务 (CheckVersionTxn) — multi 的 getData+version 组合用 | 发现 10 (补锚) |
| T4 | 批量读写交互? | maxReadBatchSize<0 时 pending+committed 同时非空 → 提前切提交 (L272-279 注释) — 防读 starve 写 | 通过 (验证) |
| T5 | 全局节流? | decInProcess (L170) + requestFinished — ZooKeeperServer 全局 inProcess 计数 (submitRequest 侧) | 通过 (验证) |
| T6 | 角色变体? | FollowerZooKeeperServer: Prep→Commit→Final (无 Sync — 写经 leader); Observer 同 — 链按角色 | 发现 11 (补锚) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 错误事务闭环 | Prep 失败 → ErrorTxn → Final 抛错 → 客户端收到错误码 ✅ | 通过 |
| V2 | check 语义 | multi 内版本断言 — 预校验原子性补充 ✅ | 通过 |
| V3 | 读写公平 | 读批控 + pending 切换 — 写不被读饿死 ✅ | 通过 |
| V4 | follower 无 Sync | follower 写 → 转发 leader (Z-2 REQUEST) → leader 链 Sync — 单点落盘 ✅ | 通过 |
| V5 | 节流计数闭环 | submitRequest++ → Final decInProcess — 满则拒新请求 ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **check OpCode**: CheckVersionTxn (L616-626) — multi 的版本断言原语 | 大纲 §2 补注 |
| 11 | **补充锚点** | **角色变体链**: follower/observer 无 Sync (写转发 leader) — Sync 仅 leader 落盘 | 大纲 §1 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 链装配 (五节点/角色变体/毒丸) — 可写 ✅
- §2 Prep (校验链/outstanding/multi/check/closeSession) — 可写 ✅
- §3 Sync (批量/snapCount/读旁路) — 可写 ✅
- §4 Commit+Final (读写分离/匹配/节流/应用响应) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 2 处全部修复** (check OpCode/角色变体链)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 追查入口节流/角色链/变更清理六存疑点)

> 动机: 对 outline 全部锚点重新 grep 并追查链外围六个存疑点 (follower/observer 链真实结构/outstandingChanges 清理时机/全局节流/SessionMoved/committedLog 入口)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | follower 链真实结构? | FollowerZooKeeperServer:67-73: **Final → Commit → FollowerRequestProcessor** (firstProcessor) + **Sync → SendAckRequestProcessor 旁路** (L73: syncProcessor 落盘后给 leader 发 ACK, L83) — **follower 有 Sync, 但链是旁路 ACK 非进 Commit** | 发现 12 (认知修正) |
| T2 | observer 链? | ObserverZooKeeperServer:88-100: Final → Commit → ObserverRequestProcessor + **注释 "Observer should write to disk"** (L100) — observer 也写盘 (防向 leader 要过旧 txn → SNAP 风暴) | 发现 13 (补锚) |
| T3 | outstandingChanges 清理? | **ZooKeeperServer.processTxn** (L1871-1881): synchronized(outstandingChanges) 内 **processTxnInDB (树应用) + while peek.zxid <= 当前 → remove + ForPath 清理** — 清理在应用时 | 发现 14 (补锚) |
| T4 | 全局节流? | **RequestThrottler** (L145-192): maxRequests=0 默认关 (L84) / dropStale 关连接 (L99) / **shouldThrottleOp → setIsThrottled** (Final THROTTLEDOP 上游) / submitRequestNow 直通 | 通过 (验证) |
| T5 | SessionMoved? | submitRequestNow catch MissingSessionException (L1276-1279) — 会话迁移丢请求 | 通过 (验证) |
| T6 | committedLog 入口? | processTxn 尾部 **addCommittedProposal (quorum 请求)** (L1883) — Z-2 DIFF 数据源确认 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | follower ACK 链闭环 | PROPOSAL 收到 → Sync 落盘 → SendAck → leader processAck → 提交 — 刷盘即 ack 语义 ✅ | 通过 |
| V2 | observer 写盘防 SNAP | 落盘 → 可 DIFF 补 → 免全量快照 ✅ | 通过 |
| V3 | 变更清理时机 | 应用时清理 (zxid 单调) — 暂存窗口 = 提案到提交 ✅ | 通过 |
| V4 | 节流默认关 | maxRequests=0 → 无限流 (性能优先) ✅ | 通过 |
| V5 | throttled 闭环 | 节流器标记 → Final THROTTLEDOP → 客户端重试 ✅ | 通过 |
| V6 | committedLog 幂等 | addCommittedProposal 应用后追加 — DIFF 数据新鲜 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **认知修正** | outline §1 "follower/observer 无 Sync" **错误** — follower 链 = Final→Commit→FollowerRequestProcessor + **Sync→SendAck 旁路** (落盘 ACK leader); 写请求经 FollowerRequestProcessor 转发 | 大纲 §1 修正 |
| 13 | **补充锚点** | **observer 也写盘** (ObserverZooKeeperServer:100 注释) — 防过旧请求 → SNAP 风暴 | 大纲 §1 补注 |
| 14 | **补充锚点** | **outstandingChanges 清理在 ZooKeeperServer.processTxn** (L1871-1881): 应用时同步清理 + addCommittedProposal (committedLog) | 大纲 §2 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 链装配 (五节点/角色链: follower Sync→SendAck 旁路/observer 写盘) — 可写 ✅
- §2 Prep (校验链/outstanding/清理时机) — 可写 ✅
- §3 Sync (批量/snapCount/读旁路/follower ACK 语义) — 可写 ✅
- §4 Commit+Final (读写分离/匹配/节流链) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 6 项全过 (V1-V6); **新发现 3 处全部修复** (follower Sync→SendAck 旁路认知修正/observer 写盘/变更清理时机)。核心认知: 角色链差异 — leader (Sync→Commit) vs follower (Sync→SendAck 旁路 + 写转发) vs observer (同 follower + ObserverRequestProcessor); 变更清理在应用时。大纲经修复后反写测试全过。
