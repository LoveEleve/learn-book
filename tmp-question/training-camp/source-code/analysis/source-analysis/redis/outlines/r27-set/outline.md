# R-27 集合命令 — 三编码与双向转换

> 前置: [[R-7-intset]] (整数编码) + [[R-19-listpack]] (小编码) + [[R-3-Dict]] (大集合) + [[R-21-db]] (键空间) | 引出: [[R-11-bitmap]] (位图集合) + [[R-13-geo]] (GEO 集合运算) | 对照: [[R-25-hash]] (单向升级 vs set 双向)
> 🟡 B | 6 KP | [模式: 三编码+双向转换+空集短路+最小集遍历]
> Pass 2 闭环: q1(三编码) q2(写入转换) q3(集合运算) q4(随机) q5(命令面) q6(编码取舍)

**读者处境**: SADD 一个非整数到 intset 会发生什么?为什么 SINTERSTORE 的结果可能是 intset?set 的 listpack 阈值为什么是 128 而 hash 是 512?SPOP COUNT -5 和 COUNT 5 差在哪?这篇拆集合命令: 三编码、双向转换、集合运算、随机采样。

### 1. 三编码 — intset/listpack/HT

场景: set 内存怎么随元素变化?
源码路径:
- setTypeCreate (t_set.c:25-36): **INT 且 ≤512 → intset** (L26-27) / **≤128 → listpack** (L28-29) / 大 → HT + dictExpand 预扩 (L33-35)
- setTypeMaybeConvert (L40-46): size_hint 超限预转换
- **阈值** (config.c:3216-3218): set-max-intset-entries=512 / **set-max-listpack-entries=128** (≠hash 512!) / set-max-listpack-value=64
- intsetMaxEntries (L49-54): **1<<30 上限** (intset 内部索引限制)
关键设计 (q1): **三编码四级**: intset 专精整数 (4/8B/元素), listpack 小字符串 (零指针), HT 大集合; listpack 阈值 128 因 set 元素无值密度高 (推断)。[模式: 三编码]
数据流: SADD 整数 → intset; SADD 小串 → listpack; 超限 → HT。

### 2. 写入与转换 — 双向链

场景: intset 遇非整数怎么升级?结果集怎么降级?
源码路径:
- setTypeAddAux (L104-208): 三编码分支 + **dict 预定位插入** (L124-128: FindPositionForInsert 一次哈希, 免 dictAddRaw 双查)
- **intset 非整数双路** (L169-202): 规模允许 → **listpack 中间态** (L181-194, lpShrinkToFit); 否则 → HT (L196-200)
- **降级** (maybeConvertToIntset L66-88): HT/listpack → intset (全整数且 ≤512) — **sinterstore 结果转回** (L1392)
- 超限升级: maybeConvertIntset (L57) → HT
关键设计 (q2): **双向转换是 set 特有**: intset→listpack→HT 渐进升级 + 全整数结果降回 intset; 与 hash/list 单向形成对比。[模式: 双向转换]
数据流: intset + 非整数 → 小: listpack / 大: HT; sinterstore 全整数 → 回 intset。

### 3. 集合运算 — 空集短路与最小集

场景: SINTER 大集合怎么算快?
源码路径:
- sinterGenericCommand (L1255-1417):
  - **空集短路** (L1275-1300): 任一空 → 结果空 (删 dstkey/空集回复)
  - **最小集策略** (L1300+, qsortCompareSetsByCardinality L1229): 按基数排序取最小遍历 — O(min × others)
  - STORE 版本 (L1370-1400): 全整数 → 降级 intset (L1392)
- sunionDiffGenericCommand (L1460-1630): 并集全加 / 差集首减余
- SINTERCARD (L1424): 只计数 + LIMIT
关键设计 (q3): **空集短路 + 最小集遍历** = 交集复杂度从 O(all) 降到 O(min×others)。[模式: 空集短路]
数据流: SINTER a b c → 空? → 最小集 → 成员检查 → 结果。

### 4. 随机命令 — COUNT 双语义

场景: SPOP 和 SRANDMEMBER 差在哪?
源码路径:
- setTypeRandomElement (L407) / setTypePopRandom (L430): intset 随机索引 / listpack 定位 / HT dictGetFairRandomKey (R-3 FAIR)
- spopWithCountCommand (L739-945): **COUNT 仅正数** (getPositiveLong L742); **count >= size → 返回全部+删键+重写 DEL/UNLINK** (L773-787); count < size → **双策略** (SPOP_MOVE_STRATEGY_MUL=5, L737): 大 set (remaining×5 > count) 直接随机抽取 (L809) / 小 set 拷贝后删
- **SRANDMEMBER 才是正负语义** (L1001-1016): **正 = 不重复** (uniq=1, 不足返回全部/报错) / **负 = 可重复** (uniq=0, 恰好 count 个)
- **弹空删键** (L900+): 维持"空集不存在" (R-26 不变量)
- srandmemberWithCountCommand (L998-1202): 同构不删
关键设计 (q4): 随机 = 编码分派 + **SPOP 正数-only + 全删短路 + MUL=5 双策略** vs **SRANDMEMBER 正负 (uniq)**。[模式: 随机采样]
数据流: SPOP key 3 → 采样 → 移除 → 空 → 删键。

### 5. 命令面 — SADD/SREM/SMOVE

场景: 增删移怎么保持不变量?
源码路径:
- saddCommand (L583): 创建 (size_hint 预判) → 预转换 (L596) → 批量 setTypeAdd → 返回新增数
- sremCommand (L608): 批量删 → **空集删键** (L620-624)
- smoveCommand (L636-690): **src==dst 短路** (L654) → 删+空 src 删键 (L665-669) → dst 创建/加入 + 双信号
- sismember/smismember/scard (L691-738): 三编码分派快查
关键设计 (q5): 命令面 = **创建 hint 预判 + 空集删键不变量** (与 R-26 list 同款)。[模式: 命令面]
数据流: SMOVE a b e → src 删 → 空删键 → dst 加 → 双 notify。

### 6. 编码取舍 — 为什么 set 支持降级

场景: 全 Redis 唯一能降级的容器?
源码路径:
- **对比矩阵**: hash/list 单向升级 (R-25/26) vs **set 双向** (maybeConvertToIntset L66-88)
- 降级动机: intset 4/8B/元素 vs dict 指针+entry — sinterstore 大结果集省内存 (推断)
- listpack 中间态: intset 遇非整数先试 listpack (渐进转换, 非一步 HT)
- 转换触发低频: 仅在"结果集"(sinterstore) 或"单元素越界"场景
关键设计 (q6): set 双向 = **"结果形态可塑"** 特例: 全整数结果享受 intset 密度; 渐进转换链避免一步到位开销。[模式: 渐进转换]
数据流: SINTERSTORE → 结果 → 全整数? → intset (省内存) / 混合 → HT。

### 负面空间 — 集合命令刻意不做的事

- **不做有序集合**: 成员无序 (SRANDMEMBER/SPOP 随机面, 有序需求用 ZSet R-6)
- **不做成员级 TTL**: set 元素无过期 (对照 HFE R-25)
- **不做集合引用计数**: SINTERSTORE 结果独立拷贝 (无共享)
- **不做流式集合运算**: 运算需全量 (无增量版本)
- **不做 SPOP 保序**: 随机移除无确定顺序

→ 引出: 位图怎么表达集合?→ [[R-11-bitmap]]
