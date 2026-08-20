# R-16 事务 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | 交接文档 §四 预判 "EXEC 传播为 MULTI...EXEC 包裹" — **不精确**! 实测 execCommand 逐命令 call(c,CMD_CALL_FULL) **每条命令独立传播** (无包裹); **MULTI...EXEC 包裹只在 also_propagate 批量场景** (server.c:3416-3427, numops>1 时, 脚本/模块/分裂传播); CMD_TOUCHES_ARBITRARY_KEYS 例外 (L3404-3410); dbid=-1 不传 SELECT (L3413) | 大纲节 3 补注 + pass2-q3 修正 |
| 2 | 机制洞察 | **argv 所有权转移**: queueMultiCommand 入队后 c->argv=NULL — 零拷贝 + 免 double-free 风险 (multi.c:71-74) | 大纲节 1 |
| 3 | 机制洞察 | **嵌入式 watchedKey**: listNode 嵌入 + node.value 指回客户端列表 — 双列表 O(1) 增删 (multi.c:246-276) | 大纲节 4 |
| 4 | 机制洞察 | **迭代中不可退订**: touchAllWatchedKeysInDb 只置 DIRTY_CAS 不 unwatch (UAF 注释 L443-445) vs touchWatchedKey 单键路径立即 unwatch (L393) — 两种失效路径的取舍差异 | 大纲节 5 |
| 5 | 边界验证 | CMD_NO_MULTI grep 实证 4 命令 (psync/save/shutdown/sync); CLIENT_ID_AOF → CMD_CALL_NONE (L209-212); 断言无阻塞 (L214); DIRTY 冻结入队 (L46-47) | 大纲节 2/3 |
| 6 | 行号验证 | 全函数 22 锚点 + 跨文件 8 处 grep 穷举全部命中 (multi.c 14-482 / server.h 998-1016,1995 / server.c 1894,3416-3427,3757-3785,3979-3981,4193-4201 / db.c 620-637,1721-1722,1767; commands.def) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 双失败路径逐行验证 (nullarray vs EXECABORT)
- mstate 回写 (命令改 argv) 一致性
- expired 位三态处理 (删除无变化/保持/置位)
- WATCH 同键去重 (同 db+同 key)

### 维度2 性能
- argv 零拷贝转移
- 嵌入式节点 O(1) 增删
- touch 后立即退订 (省后续内存)
- 预分配 2 + 倍增

### 维度3 内存
- multiStateMemOverhead 三构成
- watching_clients 统计
- argv_len_sums 记账
- 空列表即删 dict 键

### 维度4 一致性
- 单线程顺序执行原子性
- signalModifiedKey 双失效 (WATCH+tracking)
- EXEC 逐命令传播 (主从顺序一致)
- DENY_BLOCKING 防原子性破坏

### 维度5 负面空间 (已写入大纲 6 条)
- 不做回滚/隔离/嵌套/MVCC/中断恢复/多 DB WATCH

## 结论
R-16 全部锚点行号 ~30 处验证, 6 闭环完成, **认知修正 1 (传播包裹语义) + 机制洞察 3**。推断 3 处显式标注。待二次 REVIEW 复核。

---

# 二次深度 REVIEW (2026-08-13, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 R-20/R-21/R-22/R-1 (序号 < 23) 均已讲 ✅; 引出 R-8/R-17/R-30 (未来域 OK); 对照 R-29 ✅; 五结构元素齐备 ✅; 读者处境场景化 (原子性哪来/不存在的键/EXEC 错误/BLPOP) ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 大纲 ~40 锚点 (file:line) — 🟡B 标准 ≥4 ⏫ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域 (R-8/R-17/R-30 仅作引出) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 传播 (逐条 vs also_propagate 包裹) / 通知 (signalModifiedKey) / 编码 (argv 引用计数) / 键空间 (watched_keys dict) / 过期 (expired 位) / ACL (复查) / 阻塞 (DENY_BLOCKING) — 全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 负面空间 6 条 ✅; 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **强断言实证** | 大纲节 4 "touchWatchedKey 立即 unwatchAllKeys" — 验证: L393 置 DIRTY 后立即调用; 但**同列表后续节点迭代** — unwatch 会 free 当前 wk 之前的节点? 实测 listRewind 迭代中 unwatchAllKeys(c) 会释放整个 c->watched_keys 列表 — **迭代器悬垂?** 检查: touchWatchedKey 中 listRewind(clients) 迭代的是 **db 侧客户端列表**, unwatchAllKeys 释放的是 **c->watched_keys (客户端侧)**, 并 listUnlinkNode(clients, node) 摘除当前节点 — 摘除的正是当前迭代节点但 list 迭代器有 next 指针已缓存? 实测 listNext 基于 node->next 且当前节点已 unlink → 安全 (listUnlinkNode 后 next 仍可访问? 需确认 adlist 语义) — **标注: 依赖 listUnlinkNode 不释放节点内存 (wk 由 unwatchAllKeys 内 listDelNode+zfree)** — 关键: unwatchAllKeys 对当前正在迭代的 clients 列表做了 listUnlinkNode (不 free 节点, 仅摘除) — 迭代器持有 node 指针继续 next — 安全 ✅ | 记录 (adlist 迭代安全性依赖, 不展开) |
| 13 | 补充锚点 | 大纲节 3 补 also_propagate 包裹 (server.c:3416-3427) + CMD_TOUCHES_ARBITRARY_KEYS 例外 + dbid=-1 — 认知修正落地 | 大纲节 3 已含 (修正 #1), 记录 |
| 14 | 通过项 | 其余 ~35 句机制描述逐句对源码一致 ✅ (预分配 2/DIRTY 冻结/cmd_flags 双累计/CMD_NO_MULTI 4 命令/ACL 复查/AOF 特例/mstate 回写/嵌入式节点/双引用/expired 位/signalModifiedKey 双失效/UAF 防护/内存三构成) | 记录 |

## 二次 REVIEW 汇总

07 五维度轮换 + 内容深度共 **1 处记录** (强断言实证 adlist 迭代安全性, 通过), 认知修正 1 已落地。反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-13, 逐句对源码 + 推理验证)

> 动机: 用户要求深度 REVIEW — 对大纲每个锚点重新 grep, 对全部推断/计数反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | cmd_inv_flags 语义 | `cmd_inv_flags |= ~cmd_flags` (multi.c:66) — 本次命令缺 X (~X 含 X 位) → 置位; "全部命令都有 X" ⟺ flags 有 X 且 inv 无 X — 数学反推 ✅ | 通过 |
| V2 | CLIENT_ID_AOF | server.h:1105 = **UINT64_MAX** (保留 ID) — AOF 加载期客户端身份 ✅ | 通过 |
| V3 | 两种 EXECABORT | shared.execaborterr = "previous errors" 固定文案 (server.c:1894-1895) vs execCommandAbort = "because of: %s" 动态 (L119) — **两个不同错误来源** ✅ | 通过 |
| V4 | CMD_TOUCHES_ARBITRARY_KEYS | grep 实证仅 **2 命令: randomkey/scan** ✅ | 通过 |
| V5 | in_exec 消费端 | 5 处: aof.c:964/2510, module.c:3916/7787, rdb.c:4006, networking.c:4133 (client_pause_in_transaction) — 真实消费面 ✅ | 通过 |
| V6 | watching_clients | 仅 INFO 统计 (server.c:5650) + 初始化 (2695) ✅ | 通过 |
| V7 | 测试面 | multi.tcl 实证: "EXEC fail on lazy expired WATCHed key" (L134) / "WATCH stale keys not fail" (L151) / "Delete stale keys not fail" (L166) / "WATCH consider touched expired" (L362) — expired 位行为有测试覆盖 ✅ | 通过 |
| V8 | 双失败语义 | DIRTY_CAS → nullarray (nil 非错误) vs DIRTY_EXEC → EXECABORT — 测试 multi.tcl L134 循环等过期验证 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 15 | **边界语义实证** | touchWatchedKey expired 分支 (L375-387): WATCH 时已过期键 **被 SET 重建 → break 不标记 DIRTY** — 与 "WATCH 不存在键 + SET → DIRTY" 行为不同; 仅删除路径有测试覆盖 (multi.tcl L166), 重建路径无测试 — **语义存疑标注** | 大纲节 4 补注 |
| 16 | 表述精确化 | 节 1 cmd_inv_flags "~flags OR, 全命令属性" 含糊 → 精确: 任一命令缺某标志则置位, "全部有 X" ⟺ flags 有 X 且 inv 无 X | 大纲节 1 修正 |
| 17 | 补充锚点 | 节 3 补: 两种 EXECABORT 来源 (固定/动态文案) + CMD_TOUCHES_ARBITRARY_KEYS 2 命令 + CLIENT_ID_AOF=UINT64_MAX (server.h:1105) + in_exec 5 消费端 | 大纲节 3 修正 |

## 三次 REVIEW 汇总

推理验证 8 项全通过 (V1-V8); 新发现 **3 处** (边界语义实证 1 / 表述精确化 1 / 补充锚点 1), 全部修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。
