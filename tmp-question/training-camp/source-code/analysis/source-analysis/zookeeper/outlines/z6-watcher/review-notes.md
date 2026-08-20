# Z-6 Watcher — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | 09 审计已修正 WatchManager 双实现 — 深审确认 **Optimized 核心 = 位图压缩** (watcherBitIdMap BitMap + pathWatches BitHashSet, L61-64) 非简单 CHM | 大纲 §1 |
| 2 | **补充锚点** | **RWLock 语义**: addWatch 读锁 / removeWatcher 写锁 (L79-80,154-155 注释) — 防 dead watch 竞态 | 大纲 §1 |
| 3 | **补充锚点** | **懒清理**: deadWatchers → triggerWatch 时批量删 (L200-201 注释) — 避免 remove 锁竞争 | 大纲 §1 |
| 4 | **语义标注** | **watchers 集合去重**: 同 watcher 多路径只触发一次 (L142 HashSet) + suppress 跳过已处理 (L185-187) | 大纲 §3 |
| 5 | 行号验证 | 全函数 25 锚点 + 跨文件 6 处 grep (WatchManager 46-199 / WatchManagerOptimized 57-213 / WatcherMode 23-46 / ServerCnxn 261-265 / WatchManagerFactory) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 双向一致性
- 一次性语义
- 模式差异

### 维度2 性能
- 位图 contains O(1)
- RWLock 读写分离
- 懒清理换锁竞争

### 维度3 内存
- HashSet(4) 懒创建
- 位图压缩 watcher 集合

### 维度4 一致性
- 双向索引同步
- dead watcher 守卫
- suppress 去重

### 维度5 负面空间 (已写入大纲 6 条)
- 不持久事件历史/不超时/不跨节点同步/不 watch 级 ACL/不递归默认/不排序保证

## 结论
Z-6 全部锚点 ~25 处验证, 8 闭环完成, **认知修正 1 + 补充锚点 2 + 语义标注 1**。harness 4/4 (自抓 1 缺陷)。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 Z-3/Z-7 ✅; 引出 Z-7 ✅; 对照 Redis/ES ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 6 条 ✅; 横切 (并发/一致性/索引/事件) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §3 未提 **PathParentIterator 具体语义**: 路径自身 + 逐级父路径 (递归 watch 的迭代器) — PERSISTENT_RECURSIVE 依赖此 | 大纲 §3 补注 |
| 8 | 通过项 | 其余 ~23 句机制描述逐句对源码一致 ✅ (双向索引/位图/RWLock/懒清理/触发移除/模式枚举/统计/工厂) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (PathParentIterator #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 双向一致性 | addWatch 双写 / removeWatcher 双清 — 无悬挂引用 ✅ (harness A/C) | 通过 |
| V2 | 一次性语义 | STANDARD 触发即移除 — 下次需重注册 ✅ (harness B) | 通过 |
| V3 | 持久语义 | PERSISTENT 保持可重复触发 ✅ (harness B) | 通过 |
| V4 | 位图幂等 | 重复 add 同 watcher 同 bit ✅ (harness D) | 通过 |
| V5 | 死 watcher 守卫 | cnxn 关闭忽略 add — 无死引用污染 ✅ | 通过 |
| V6 | 递归正确性 | 父路径迭代 — 子变化触发父 watch ✅ | 通过 |
| V7 | suppress 语义 | 已处理 watcher 跳过 — deleteNode 双触发去重 ✅ | 通过 |
| V8 | 懒清理收敛 | deadWatchers 触发时批量删 — 内存最终回收 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **WatchStats.NONE 判定**: removeMode 后空 → 移除路径 (L162-167) — 触发移除的精确条件 | 大纲 §3 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (WatchStats.NONE), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (工厂配置/事件投递链/removeWatcher API/统计/递归计数/数据-子 watch 分离), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 工厂配置? | WatchManagerFactory: watchManagerClassName / isWatchManagerOptimized 系统属性 — DataTree 构造调用 | 通过 (验证) |
| T2 | 事件投递链? | ServerCnxn.process → queueEvent → 客户端 (NIOServerCnxn) — Z-7 交叉 | 通过 (验证) |
| T3 | removeWatcher API? | 客户端 removeWatch 请求 → 服务端 removeWatcher(path, watcher) — 4.0+ 协议面 | 发现 10 (补锚) |
| T4 | 统计面? | getWatchesSummary/getWatchesByPath/getWatchesBySession (L308+) — admin 查询 | 发现 11 (补锚) |
| T5 | 递归计数? | recursiveWatchQty (L103-105,127-131) — PERSISTENT_RECURSIVE 统计 | 通过 (验证) |
| T6 | 数据-子 watch 分离? | DataTree dataWatches/childWatches 双管理器 (Z-3) — NodeCreated vs NodeChildrenChanged 分投 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 触发-投递闭环 | triggerWatch → process → queueEvent → 客户端 — 端到端 ✅ | 通过 |
| V2 | removeWatch 对称 | 客户端显式移除 vs 触发移除 — 双路径一致 ✅ | 通过 |
| V3 | 统计一致 | getWatches* 从双索引导出 — 无额外状态 ✅ | 通过 |
| V4 | 分离正确性 | data 与 child 独立触发 — 事件类型精确 ✅ | 通过 |
| V5 | 死 watcher 终回收 | add 守卫 + 懒清理 — 无泄漏 ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **removeWatch 客户端 API**: removeWatcher(path, watcher) — 显式移除协议面 (4.0+) | 大纲 §2 补注 |
| 11 | **补充锚点** | **统计查询**: getWatchesSummary/getWatchesByPath/getWatchesBySession (L308+) — admin 监控面 | 大纲 §1 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 双实现 (位图/RWLock/懒清理/统计) — 可写 ✅
- §2 双向注册 (双向索引/removeWatch API) — 可写 ✅
- §3 触发链 (PathParentIterator/NONE/suppress) — 可写 ✅
- §4 WatcherMode (三模式/清理/工厂) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 2 处全部修复** (removeWatch API/统计查询)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 追查 session 分桶断言/迭代器/投递链六存疑点)

> 动机: 对 outline 全部锚点重新 grep 并追查六个存疑点 (执行计划 "sessionWatches 分桶" 断言/PathParentIterator 双模式/事件投递 ACL/位图结构/工厂配置/统计面)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 执行计划 "WatchManagerOptimized 按 session 分桶 (sessionWatches)"? | **grep 实证: WatchManagerOptimized 无 sessionWatches** — 实际 = **pathWatches (按路径) + watcherBitIdMap (按 watcher 位图)**; 会话关闭清理靠 **deadWatchers 懒清理** (ServerCnxn.close → addDeadWatcher → triggerWatch 批量删), **非 session 分桶** — 执行计划断言错误 (09 审计漏网!) | 发现 12 (认知修正) |
| T2 | PathParentIterator 双模式? | **forAll (maxLevel=MAX, 递归) vs forPathOnly (maxLevel=0)** (L39-51); getPathParentIterator 按配置选 (WatchManager:370-374); next() 逐级上溯 + atParentPath()=level>0 (L79-100) | 发现 13 (补锚) |
| T3 | 事件投递 ACL? | NIOServerCnxn.process (L710-736): **checkACL (READ 权限过滤!)** — NoAuth → 丢弃; NOTIFICATION_XID + sendResponse ("notification") + WATCH_BYTES 指标 — **投递时 ACL 过滤** (大纲负面空间"不 watch 级 ACL"需精确化) | 发现 14 (补锚) |
| T4 | 位图结构? | util/BitHashSet (BitMap + 位集合) — watcher 位 id + 路径位集合 | 通过 (验证) |
| T5 | 工厂配置? | WatchManagerFactory (watchManagerClassName/isWatchManagerOptimized 系统属性) — DataTree 构造 | 通过 (验证) |
| T6 | 统计面? | getWatchesSummary/getWatchesByPath/getWatchesBySession (L308+) + DataTree.getWatchCount | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 会话清理闭环 | ServerCnxn.close → isDeadWatcher 标记 → addDeadWatcher → triggerWatch 批量删 — 无 session 分桶也正确 ✅ | 通过 |
| V2 | 迭代正确性 | forAll 逐级父路径 — PERSISTENT_RECURSIVE 全覆盖 ✅ | 通过 |
| V3 | ACL 过滤安全 | 投递时 READ 校验 — 无权限不泄漏事件 ✅ | 通过 |
| V4 | 位图一致性 | watcherBitIdMap 与 pathWatches 同步 — 无悬挂位 ✅ | 通过 |
| V5 | 工厂可插拔 | 配置切换实现 — 标准/优化无侵入 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **认知修正** | 执行计划 "WatchManagerOptimized 按 session 分桶 (sessionWatches)" **不存在** — 位图按路径+watcher; 会话清理走 deadWatchers 懒清理 (09 审计漏网断言) | 大纲 §1 修正 + PLAN 审计表补录 |
| 13 | **补充锚点** | PathParentIterator 双模式: forAll (递归) / forPathOnly (仅路径, 配置开关 L370-374) | 大纲 §3 补注 |
| 14 | **补充锚点** | **投递时 ACL 过滤**: NIOServerCnxn.process → checkACL (READ) — NoAuth 丢弃 (负面空间精确化) | 大纲 §3 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 双实现 (位图/RWLock/懒清理/统计) — 可写 ✅
- §2 双向注册 (双向索引/removeWatch API) — 可写 ✅
- §3 触发链 (迭代器双模式/ACL 过滤/suppress) — 可写 ✅
- §4 WatcherMode (三模式/清理/工厂) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (sessionWatches 断言错误/迭代器双模式/投递 ACL 过滤)。核心认知: Optimized 无 session 分桶 (执行计划错误 — 09 审计补录); 投递有 ACL 过滤。大纲经修复后反写测试全过。
