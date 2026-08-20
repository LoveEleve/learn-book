# R-18 内存碎片 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **表述精确化** | 大纲 §5 "只升不降 (L1039-1047)" 例外表述不完整: 条件为 `cpu_pct > running OR configuration_changed` — 配置变更时**无条件采用新值 (允许下降)** (L1039-1043), 非仅"重新考虑" | 大纲 §5 补注 |
| 2 | **表述精确化** | 大纲 §4 "大键延迟不影响游标推进" **不准确**: L1199-1204 桶扫完后若 defrag_later 非空 → `defrag_later_item_in_progress=1; continue` — 延后任务**优先于主游标推进** (当前桶完成后先清积压再推 slot); 主扫描被延后扫描"插队" | 大纲 §4 修正 |
| 3 | **表述精确化** | 大纲 §3 "stream 三层 (s/rax/entry)" 过简: defragStream (L694-713) 实际递归 6 层 — s 结构 → s->rax (entry data) → s->cgroups (cg) → cg->consumers (consumer) → c->pel (nack) + cg->pel; PendingEntryContext 双引用同步 (L648-665: nack 搬移须 raxInsert 回 pel 并断言旧指针) | 大纲 §3 补注 |
| 4 | **机制洞察** | **expires 阶段仅计数 = 共享 sds 键的必然结果**: db->expires 键与 keys 表共享 (R-21 零拷贝), 键名引用已在 keys 阶段 defragKey L740-747 同步过; 故 expires 阶段只 scanCallbackCountScanned (L457-461) 统计 scanned, 不搬移 — 避免双搬 | 大纲 §5 补注 |
| 5 | 观察记录 | **pubsub 两表在每 db 迭代重复扫描** (L1175-1178 在 db 循环内, 表是全局): 冗余但无害 (表通常小, 多扫=多 misses); 简单性设计 (defrag_stages 数组统一处理) | 记录 (不修大纲) |
| 6 | 补充锚点 | defragScanCallback 统计粒度 (L826-833): key_hits/misses 按"键内是否有任何指针搬移" (hits 前后对比) — INFO 语义 | 大纲 §6 补注 |
| 7 | 行号验证 | 全函数 25 锚点 + 跨文件 10 处 grep 穷举全部命中 (defrag.c 30-1270 / dict.c 1217-1472 / dict.h 135-142,250 / kvstore.c 778 / zmalloc.c 199-213 / server.c 1066,1578-1586,5746,5852-5900 / config.c 2323-2337,2462-2466,3080,3155-3220 / module.c 13449,13553,13580 / jemalloc_internal_inlines_c.h 341-400) | 记录 |

## 07 五维度

### 维度1 功能正确性
- dictPauseRehashing/ResumeRehashing 配对 (dict.c:1398/1472) — 扫描期冻结 rehash 完整
- dictDefragBucket 三态 (storedKey/noValue/normal) 与 R-3 编码一致
- 键名搬移双表同步 (哈希+旧指针定位) 与 R-21 expires 共享键闭环
- zset dict+zsl 双结构同步 + score 引用 (zslDefrag 返回值)
- pubsub refcount 断言 = clients+1

### 维度2 性能
- je_get_defrag_hint 零成本过滤 (分配器内判定)
- 16/512/64 三条件时限 (两处同款)
- 大键延后防尖峰 + bookmark/static-last 廉价断点
- no_tcache 直接 arena 分配 (慢但确定性)

### 维度3 内存
- frag_pct 用小 bin 浪费占比 (大 bin 虚高规避)
- Lua arena 排除 (不可搬分配)
- defrag_later sdsdup 拷贝 (防键名搬移悬垂)

### 维度4 一致性
- 搬移=新分配-拷贝-释放, 旧指针全局替换
- fork 暂停 (COW 写页放大防护)
- digest 校验 (测试黄金标准)
- HFE 时间桶 ebDefragItem 引用同步

### 维度5 负面空间 (已写入大纲 7 条)
- 不搬大分配/不 rss 驱动/不重启替代/不 arena 调优/不实时保证/不跨 fork/不压缩

## 结论
R-18 全部锚点 ~40 处验证, 6 闭环完成, **表述精确化 3 + 机制洞察 1 + 观察 1**。怀疑审计 2/6 修正已入 pass1-notes。推断 3 处显式标注 (temporal-trace.md)。待二次 REVIEW 复核。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 前置 R-33/R-3/R-21/R-2/R-1 (序号均 < 30) 已讲 ✅; 引出 R-31 (未来 OK); 对照 R-22/R-23 ✅; 读者处境场景化 (RSS 高但 used_memory 不高/重启才能降/小对象碎片) ✅; 六结构元素齐备 ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 大纲 ~40 锚点 (file:line) — 🟡B 标准 ≥4 ⏫; 含 jemalloc 补丁文件锚点 ✅ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 前置声明无未来域 (R-31 仅引出) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 内存分配器 (mallocx/dallocx no_tcache) / 键空间 (keys+expires 双表) / 编码层 (listpack/intset/quicklist/skiplist) / 时间桶 (HFE ebDefragItem) / 复制 (fork COW 暂停) / 事件循环 (cron 双调用) / 模块 (三入口 API) — 七横切全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | 通过项 | 负面空间 7 条 ✅; 开篇场景化 ✅ (RSS 高/重启治/jemalloc 说了算) | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **反写测试发现** | 大纲 §2 缺 "dictScanDefrag 与 dictScan 游标算法完全一致" 的明确表述 (读者可能疑问为什么可以复用) — 补: 反向游标 + rehash 双表扩张桶是 R-3 同款, 差异仅在 defragfns 桶处理 | 大纲 §2 补注 |
| 14 | 通过项 | 其余 ~30 句机制描述逐句对源码一致 ✅ (hint 判定式/no_tcache 动机/expected_refcount/EMBSTR 偏移/双门槛 AND/INTERPOLATE+LIMIT/只升不降/timelimit/四阶段/128 迭代/bookmark 失败从头/static last/模块三入口/7 配置/INFO 8 统计/digest 校验) | 记录 |
| 15 | 通过项 | 数字穷举复核: 7 配置默认值 (10/100/1/25/1000/100MB/no) / 16-512-64 / 128 / 12.5% / 2.5ms@25%@100hz / 4 bookmark (quicklist MAX) 全部 grep 命中 ✅ | 记录 |

## 二次 REVIEW 汇总
07 五维度轮换 + 内容深度共 **1 处修复** (反写测试 #13), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

> 动机: 大纲每个锚点重新 grep, 对全部推断/计数反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | INTERPOLATE 边界 | frag<lower (运行中回落): INTERPOLATE 负外推 → LIMIT 钳到 min(1); L1039: cpu_pct(1) > running(65)? 否 → effort 保持 — **运行中不回降** 闭环 ✅ | 通过 |
| V2 | 测试断言 running ∈ [65,75] | INTERPOLATE(frag, 5, 100, 65, 75) + LIMIT — frag≥5 时值域 [65,75] ✅ (memefficiency.tcl L96) | 通过 |
| V3 | timelimit 数学 | 1M×running/hz/100: 25%@100hz = 2500us; 测试 L128-130: max_latency ≤30ms (≈2.5ms×多周期+大键余量) ✅ | 通过 |
| V4 | Pause/Resume 配对 | dict.c:1398/1472 — defrag 版完整配对 (对照 dictScan L815/833) ✅ | 通过 |
| V5 | hint 判定式等量变换 | 左 (nregs-free)×curslabs = "若全 slab 平均占用则该 slab 的占用"; 右 curregs+curregs/8 = 非满 slab 总占用+12.5% 权重; 不等式 = 该 slab 利用率 ≤ 加权平均 ✅ | 通过 |
| V6 | 双门槛 AND | L1021 return 条件 = OR (任一不足) → 继续 = 两者都超 ✅ | 通过 |
| V7 | 大键延后插队 | L1199-1204: cursor==0 → 列表非空 → continue 不推 slot — 延后优先 ✅ | 通过 |
| V8 | 跨 stage 游标 | 切 stage 前提 cursor==0 (L1199) → 新 stage 从 0 开始 — 不同 kvs 复用 0 游标安全 ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 16 | **覆盖缺口** | 大纲 §6 未提 **DEBUG mallctl / MEMORY 命令面**: 测试用 `debug mallctl arenas.page` (memefficiency.tcl L53) 判页大小; MEMORY DOCTOR 报告碎片但不治疗 — 排障命令面对运维读者有价值 | 大纲 §6 补注 |
| 17 | 精确化 | 大纲 §5 "expires (仅计数)" 未解释原因 — 补: 共享 sds 键已在 keys 阶段同步 (深审 #4 的落点) | 大纲 §5 修正 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **2 处** (覆盖缺口 1 / 精确化 1), 全部修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查四个存疑点 (大键延后跨槽/跨 db 切换守卫/kvstore slot 语义/LUT 搬移与 rehash 链表), 并做反写测试。

## 追查过程 (四个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 大键延后跨槽会丢键吗? (defragLater 不存 slot, defragLaterStep 用当前 slot 查) | 主循环 L1199-1204: 推进 slot 前提 = `listLength(db->defrag_later)==0`; 列表非空 → `defrag_later_item_in_progress=1; continue` **不推 slot** — 列表键恒属当前槽 (defragKey 登记时 slot=ctx->slot, L735) → defragLaterStep 同槽查找恒命中 | 通过 (无跨槽丢失) |
| T2 | db 切换与四阶段自洽? | L1118: 切 db 仅当 `!defrag_stage && !defrag_cursor && slot<0` (四 stage 全完成); L1120 先 defragLaterStep 清遗留再 ++current_db; stage 2/3 (pubsub) 在 db 迭代内完成 | 通过 |
| T3 | kvstoreDictFind 错误 slot 语义? | kvstore.c:808 直接索引 dicts[didx]; 错槽 → NULL → defragLaterItem(NULL) → cursor=0 摘除 — 但 T1 保证不会发生 | 通过 (防御性容错) |
| T4 | LUT 搬移后 kvstore 内部引用? | kvstoreDictLUTDefrag (kvstore.c:778-790) 尾部: `metadata->rehashing_node->value = *d` — **R-21 增量 rehash 链表的节点 value 同步为新 dict 指针** (L785-788) | 发现 1 (大纲 §2 缺此锚点) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 主循环守卫链 | cursor==0 → 列表空? → 推 slot/切 stage — 三条件顺序保证延后任务与主游标互斥推进 ✅ | 通过 |
| V2 | kvstoreDictScanDefrag 包装 | kvstore.c:764-770: kvstoreGetDict(didx) + dictScanDefrag; 空 dict → 返回 0 (立即推进) ✅ | 通过 |
| V3 | 复合字面量生命周期 | `&(defragPubSubCtx){...}` (L1175-1178) — C99 块作用域; defragScanCallback 同步执行不跨块 ✅ | 通过 |
| V4 | activeDefragList 续扫 | L339 `ln = newln` — 搬移后从新节点 next 续扫; activeDefragQuickListNode 同款 (*node_ref=newnode) ✅ | 通过 |
| V5 | scanLaterList 的 ob->ptr 更新 | L422 bookmark realloc 后 ob->ptr=ql; ob 指针由 defragLaterItem 每次 dictGetVal 重取 (L921) — 无悬垂 ✅ | 通过 |
| V6 | INT 编码语义 | activeDefragStringObEx L122-124: INT 不搬 (值在 ptr 无分配) + **其余未知编码 serverPanic** — 崩溃兜底 ✅ | 通过 (驱动发现 2) |
| V7 | expires 阶段 entry 不搬 | scanCallbackCountScanned (L457-461) 仅 ++scanned; 结构级由 LUT (L1159) 搬 — entry/桶级碎片不整理 ✅ | 通过 (观察) |
| V8 | 大键登记与槽绑定 | defragKey 内 defragLater 调用处 (L492 等) 的 ctx->slot 即当前扫描槽 — 与 T1 闭环 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 18 | **机制洞察/补充锚点** | 大纲 §2 的 kvstoreDictLUTDefrag 缺 **rehashing 链表同步**: LUT 搬移 dict 结构后, kvstore 增量 rehash 链表的节点 value 必须更新 (kvstore.c:785-788) — "搬移=引用全局同步"契约的又一实例 (连 kvstore 内部链表都算引用持有者); R-21 交叉 | 大纲 §2 补注 |
| 19 | **表述精确化** | 大纲 §1 "INT 编码跳过" 缺兜底: 未知字符串编码 → serverPanic (L122-124) — 崩溃保护 (编码矩阵完整性) | 大纲 §1 补注 |
| 20 | 观察记录 | expires 阶段 entry/桶级碎片不搬 (仅 LUT 结构级) — 设计取舍 (expires 表一般远小于 keys; 共享键引用已同步) | 记录 (不修大纲, 已在 §5 注"仅计数"基础上补充) |

## 反写测试 (只读大纲能否写文章)

- §1 搬移原语: hint 判定式/no_tcache/指针家族/INT panic (修复后) — 可写 ✅
- §2 渐进扫描: 游标同款/Pause-Resume 配对/三态桶/rehashing 链表同步 (修复后) — 可写 ✅
- §3 类型分派: 键名双表/robj HFE/全矩阵/stream 6 层/pubsub 断言 — 可写 ✅
- §4 大键延后: 阈值/三续扫技术/插队优先/跨槽安全 (T1 可作写作素材) — 可写 ✅
- §5 调度: 碎片率公式/双门槛/插值/只升不降/timelimit/四阶段/双调用点 — 可写 ✅
- §6 命令面: 7 配置/INFO/module 三入口/排障面 — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 四次 REVIEW 汇总

四存疑点全实证 (T1-T4); 推理验证 8 项全过 (V1-V8); **新发现 3 处全部修复** (机制洞察 1: rehashing 链表同步 / 表述精确化 1: panic 兜底 / 观察 1)。T1 (跨槽安全) 是本次最有价值的通过项 — 主循环守卫链 (L1199-1204) 设计精巧, 值得写作时展开。大纲经修复后反写测试全过。
