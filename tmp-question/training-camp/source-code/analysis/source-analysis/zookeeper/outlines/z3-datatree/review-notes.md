# Z-3 DataTree — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | PLAN "NodeHashMap(ConcurrentHashMap)" — 实为 **NodeHashMapImpl 接口 + digest 钩子** (pre/postChange 增量计算 tree digest) 非纯 CHM | 大纲 §1 补注 |
| 2 | **补充锚点** | **根节点双 key**: `nodes.put("", root)` + `nodes.putWithoutDigest("/", root)` (L288-289) — createNode parentName 用 "" 查根, 快照序列化用 "/" — harness 实证 | 大纲 §1 补注 |
| 3 | **语义标注** | **父节点锁而非全局锁**: synchronized(parent) — 兄弟创建串行, 不同父并行 (L443,545) | 大纲 §2 补注 |
| 4 | **补充锚点** | **fuzzy snapshot 双保护**: ACL 先入缓存 (L446-457) + cversion/pzxid replay 单调 (L470-478,548-553) — 快照窗口一致性 | 大纲 §2 补注 |
| 5 | 行号验证 | 全函数 28 锚点 + 跨文件 6 处 grep (DataTree 95-259,410-625,627-757,845-960,1322-1388 / DataNode / NodeHashMapImpl / ZKDatabase) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 写校验链 (父存在/不重复/版本)
- cversion/pzxid 单调
- 分类维护

### 维度2 性能
- 扁平 O(1) 查找
- nodeDataSize 原子缓存
- digest 增量 (非全量)

### 维度3 内存
- ACL 引用计数缓存
- 分类集合 (ephemerals/containers/ttls)

### 维度4 一致性
- 父节点锁
- tree digest 校验
- fuzzy snapshot 保护

### 维度5 负面空间 (已写入大纲 6 条)
- 不真实树指针/不节点级锁/不路径压缩/不数据压缩/不子排序/不拆大节点

## 结论
Z-3 全部锚点 ~28 处验证, 8 闭环完成, **认知修正 1 + 补充锚点 2 + 语义标注 1**。harness 4/4 (自抓 2 缺陷: 根双 key/反序列化断言)。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 Z-2 ✅; 引出 Z-4 ✅; 对照 Redis/ES/RM-3 ✅; 读者处境场景化 ✅; 锚点 ~28 ✅; 负面空间 6 条 ✅; 横切 (并发/一致性/树结构/事务) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §1 未提 **digest 历史与校验触发**: digestLog (LinkedList 1024) + digestFromLoadedSnapshot + lastProcessedZxidDigest + DigestWatcher (L177-188) — 快照加载时校验面 | 大纲 §1 补注 |
| 8 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (扁平树/DataNode 字段/分类集合/父锁四步/cversion 保护/三 watch/quota/事务分发/快照重建/committedLog) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (digest 历史 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 父锁覆盖性 | createNode/deleteNode 锁父 — 兄弟互斥, 同路径并发创建串行 ✅ | 通过 |
| V2 | cversion 单调数学 | parentCVersion > 现有才更新 — replay 旧事务不回退 ✅ (harness C) | 通过 |
| V3 | pzxid 单调 | zxid > pzxid 才更新 — 防 CreateTxn 覆盖 DeleteTxn ✅ | 通过 |
| V4 | 分类不泄漏 | create 登记 (containers/ttls/ephemerals) ↔ delete 清理 — 对称 ✅ (harness D) | 通过 |
| V5 | 快照重建完整性 | 父链 + 分类 + ACL 引用三重建 — 加载后树等价 ✅ | 通过 |
| V6 | digest 增量正确性 | pre 减旧 + post 加新 — 单节点变更 O(1) ✅ | 通过 |
| V7 | ACL 先入缓存 | 快照模糊窗口内 create → ACL 必在缓存 — 无悬空引用 ✅ | 通过 |
| V8 | 事务幂等 | processTxn 按 zxid 单调重放 — 同一事务不重复应用 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **quota 面**: PathTrie 前缀索引 + updateQuotaStat + updateWriteStat (L500-518) — 配额计数是写路径一部分 | 大纲 §2 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (quota 面), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (multi 原子性/EphemeralType 特值/读写并发/StatPersisted 字段/PathTrie 结构/watch 顺序), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | multi 原子性? | processTxn multi → 子事务循环 (L1028, isSubTxn); **原子性靠 PrepRequestProcessor 预校验** (Z-4 交叉) — 树层无回滚 | 发现 10 (补锚) |
| T2 | EphemeralType 特值? | EphemeralType.get(owner) — CONTAINER/TTL 用高位置位编码 (对照普通 sessionId) — createContainer/createTTL 走特值 | 通过 (验证) |
| T3 | 读写并发? | 读 getNode 无锁 (CHM 读) + 写父锁 — 读不阻塞写 ✅ | 通过 (验证) |
| T4 | StatPersisted 字段? | czxid/mzxid/pzxid/cversion/version/ephemeralOwner/dataLength (DataNode:60) — STAT_OVERHEAD_BYTES=68B | 通过 (验证) |
| T5 | PathTrie 结构? | 前缀树 — quota 路径 addPath/deletePath (L505,594) — 仅 quota 用 | 通过 (验证) |
| T6 | watch 顺序? | create: dataWatches NodeCreated → childWatches NodeChildrenChanged (L519-521); delete: NodeDeleted ×2 + children (L621-624) — 先节点后父 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | multi 语义 | 预校验保证 → 树层循环应用 — 失败在预校验期拦截 ✅ | 通过 |
| V2 | 分类编码 | EphemeralType 特值不与 sessionId 冲突 — 分类判定无歧义 ✅ | 通过 |
| V3 | 读快照一致性 | 逐节点 synchronized(value) 遍历 (L244) — 弱一致可接受 ✅ | 通过 |
| V4 | 双 key 根 | "" (写路径) vs "/" (序列化) — 双 key 同对象 ✅ | 通过 |
| V5 | watch 触发顺序 | 先 data 后 child — 观察者看到一致性事件序 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **multi 原子性在 Prep 预校验** (树层无回滚): processTxn 子事务循环 isSubTxn (L1028) — 失败在事务生成期拦截 | 大纲 §3 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 树结构 (扁平/双 key/digest/分类) — 可写 ✅
- §2 写操作 (父锁四步/cversion 单调/quota) — 可写 ✅
- §3 事务应用 (分发/multi/TxnDigest) — 可写 ✅
- §4 快照面 (序列化/重建/committedLog) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (multi 预校验). 大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 追查读路径/会话清扫/quota/统计六存疑点)

> 动机: 对 outline 全部锚点重新 grep 并追查 DataTree 外围六个存疑点 (killSession 并发语义/quota 执行面/读路径 watch/统计指标/快照频率/digest 历史)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | killSession 并发语义? | **单线程注释** (L1122-1127): "only called from FinalRequestProcessor... in sequence" — **树应用单线程化** (ephemerals.remove 免锁); deleteNodes NoNodeException 忽略 (fuzzy 窗口容忍, L1166-1170) | 发现 11 (补锚) |
| T2 | quota 执行面? | **检查在 Prep** (PrepRequestProcessor:387,685 checkQuota) — 超限写请求预处理期拒绝; **树层只计数** (updateQuotaStat L375) | 发现 12 (补锚) |
| T3 | 读路径挂 watch? | getChildren (L722-744): **synchronized(n) 内 childWatches.addWatch** — 读请求同时注册 watch (一次性语义 Z-6) | 发现 13 (补锚) |
| T4 | 统计指标? | **updateReadStat/updateWriteStat → READ/WRITE_PER_NAMESPACE** (L1627-1644, 命名空间级 metrics) + nodeDataSize (树级缓存) | 通过 (验证) |
| T5 | 快照频率? | DEFAULT_SNAP_COUNT=**100000** (ZooKeeperServer:224, zookeeper.snapCount ≥2 校验 L1290-1294) — Sync 刷盘阈值 (Z-4/Z-9 交叉) | 通过 (验证) |
| T6 | digest 历史? | logZxidDigest (L1648+): lastProcessedZxidDigest + INTERVAL=128 记录进 digestLog (LIMIT=1024) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 单线程写模型 | Final 单线程应用 → 树写无并发 → 分类集合免锁 — 并发模型核心 ✅ | 通过 |
| V2 | quota 两段式 | Prep 检查 (拒绝) + 树计数 (统计) — 超限不落树 ✅ | 通过 |
| V3 | 读 watch 一致性 | synchronized(n) 内 addWatch — 读与 watch 注册原子 ✅ | 通过 |
| V4 | killSession 容忍 | NoNode 忽略 (fuzzy 已删) — 清扫不中断 ✅ | 通过 |
| V5 | digest 有界 | LIMIT=1024 截断 — 历史内存有界 ✅ | 通过 |
| V6 | snapCount 语义 | 100000 事务一刷 (Sync 侧) — 树无感知 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **树应用单线程化**: killSession 注释 (L1122-1127) — Final 顺序应用, 分类集合免锁; NoNode 容忍 | 大纲 §1 补注 |
| 12 | **补充锚点** | **quota 两段式**: 检查在 Prep (checkQuota 拒绝) / 树层只计数 (updateQuotaStat) | 大纲 §2 补注 |
| 13 | **补充锚点** | **读路径挂 watch**: getChildren 在 synchronized(n) 内 addWatch (Z-6 交叉) | 大纲 §2 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 树结构 (扁平/双 key/digest/单线程模型) — 可写 ✅
- §2 写操作 (父锁四步/quota 两段/读挂 watch) — 可写 ✅
- §3 事务应用 (分发/multi/TxnDigest) — 可写 ✅
- §4 快照面 (序列化/重建/committedLog) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 6 项全过 (V1-V6); **新发现 3 处全部修复** (单线程写模型/quota 两段式/读挂 watch)。核心认知: 树并发模型 = Final 单线程应用 (写免锁) + 读并发 (CHM); quota 是 Prep 检查树层计数的两段式。大纲经修复后反写测试全过。
