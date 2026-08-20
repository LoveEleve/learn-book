# R-6 skiplist+ZSet — 有序集合: dict 问"有没有", 跳表答"第几个"

> 前置: [[R-3-Dict]] (成员表) + [[R-19-listpack]] (小规模编码) + [[R-4-SDS]] (ele) | 引出: [[R-5-quicklist]] | 对照: [[R-7-intset]] (编码家族)
> 🔴 A | 8 KP | [模式: 双结构互补+概率层级+span 距离索引+复合全序]
> Pass 2 闭环: q1(双结构) q2(层级) q3(span) q4(排序) q5(插入) q6(范围) q7(命令) q8(转换)

**读者处境**: ZADD 之后 ZRANK 为什么 O(log n) 就知道排名?ZRANGEBYSCORE 怎么跳过范围外的元素?为什么不用红黑树?同分的两个元素谁在前?这篇拆 zset: dict 与跳表的双结构、P=0.25 的概率层级、span 距离索引 — 以及"跳表为什么是 Redis 的选择"。

### 1. 双结构 — dict 管存在, 跳表管顺序

场景: 一个 zset 为什么是两个数据结构?
源码路径:
- `server.h:1357-1360` — `typedef struct zset { dict *dict; zskiplist *zsl; } zset;`
- 分工: **dict (member→score)**: ZSCORE/ZREM O(1) 成员定位; **zsl**: ZRANGE/ZRANK/ZCOUNT 有序面
- `t_zset.c:1425-1530` (zsetAdd) — 双写: zslInsert + dictAdd; `t_zset.c:1589-1600` (zsetDel) — zslDelete + dictDelete
关键设计: 互补双结构 (q1): 单一 dict 无序 (排序 O(n log n)); 单一跳表成员存在 O(log n) 但 dict 更快 + 双结构允许各取所长; **单线程下双写天然一致**。[模式: 双结构互补]
数据流: ZADD → dict 查重 → zslInsert (有序) + dictAdd (索引) → 双写完成。

### 2. 层级概率 — P=0.25 与 32 层上限

场景: 跳表的高度怎么定?为什么 32 层封顶?
源码路径:
- `t_zset.c:126-132` (zslRandomLevel) — `threshold = ZSKIPLIST_P*RAND_MAX; while (random() < threshold) level++;` — **P=0.25 几何分布**
- `server.h:514-515` — ZSKIPLIST_MAXLEVEL 32 / ZSKIPLIST_P 0.25
- 期望层数 = 1/(1-P) ≈ 1.33/节点; 最高层与元素数: log_{1/P}(n) — **2^64 元素需 32 层** (注释 "Should be enough for 2^64 elements")
关键设计: 概率层级 (q2): 每节点随机高度 (25% 升层) — 期望 1.33 层/节点 (vs 经典 P=0.5 的 2 层): **省一半指针, 查找常数略大** — Redis 内存敏感的选择。[模式: 几何分布层级]
数据流: 插入 → zslRandomLevel → 节点按 level 分配层数组 → 链接。

### 3. span — 跳表的"距离索引"

场景: ZRANK 怎么 O(log n) 知道排名?每个 level 那列数字是什么?
源码路径:
- `server.h:1345-1348` — `zskiplistLevel { forward; unsigned long span; }` — **span = 该层跳跃跨越的节点数**
- `t_zset.c:508-528` (zslGetRank) — 从 header 顶层走, `rank += span` — 路径上 span 累加 = 排名
- `t_zset.c:171-183` (插入维护) — 新节点 span = 前驱原 span - (rank[0]-rank[i]); 前驱 span = (rank[0]-rank[i])+1
关键设计: 距离索引 (q3): span 让"排名"成为跳表路径的副产品 — O(log n) ZRANK (无 span 时代 O(n)); 插入用 rank 差 O(1)/层维护。[模式: 跨度累加索引]
数据流: ZRANK member → header 顶层走 → span 累加 → 排名; ZRANGE BYRANK → 排名反查元素。

### 4. 复合排序 — 同分谁在前

场景: 两个 member 同分, ZRANGE 谁先?
源码路径:
- `t_zset.c:147-150` (zslInsert 比较) — `forward->score < score || (score == score && sdscmp(ele) < 0)` — **(score, ele) 复合比较**
- `t_zset.c:230-233` (zslDelete) / `t_zset.c:508-528` (zslGetRank) — 同构复合
关键设计: 全序契约 (q4): score 可重复 → 必须次级键; sdscmp 字典序提供二进制安全全序 — **任何状态下排序唯一确定**, ZRANGE/ZRANK 结果可预测。[模式: 复合全序]
数据流: 同分比较 → sdscmp(ele) → 字典序决定先后。

### 5. 插入算法 — update/rank 双数组

场景: 插入怎么同时找到位置和维护排名跨度?
源码路径:
- `t_zset.c:137-192` (zslInsert):
  - L144-156: 自顶向下查找, `rank[i] = rank[i+1] 继承 + span 累计` — **每层记录到插入位的排名**
  - L161-169: 新层初始化 (update=header, span=length)
  - L170-178: 自底向上链接 + span 差更新 (q3 公式)
  - L181-183: 未涉及层 span++
- `t_zset.c:196-214` (zslDeleteNode) — span 调整 + **level 惰性收缩** (顶层空才减, L211-212)
关键设计: 一次遍历双信息 (q5): update[] (链接前驱) + rank[] (跨度差) 在一次自顶向下遍历中同时获得 — 插入 O(log n) + 链接 O(level); 删除后层级只收缩到"顶层为空" (惰性)。[模式: 双数组遍历]
数据流: 插入 → 查找累计 rank → 随机 level → 链接 + span 更新 → backward/tail 维护。

### 6. 范围查询 — 跳过范围外

场景: ZRANGEBYSCORE 怎么直接跳到范围起点?
源码路径:
- `t_zset.c:317-334` (zslIsInRange) — 首尾判空 O(1): tail ≥ min 且 first ≤ max
- `t_zset.c:336-410` (zslNthInRange) — 从 header 顶层 while 跳过 `!GteMin` 区域 (记 edge_rank) → 小偏移逐节点 (**ZSKIPLIST_MAX_SEARCH=10**, server.h:516) 或 **zslGetElementByRankFromNode 排名差定位**
- 消费者: ZRANGE BYSCORE offset (L3345), ZCOUNT 首末 (L3443/3451)
关键设计: 范围定位 (q6): 跳表直接"跳进"范围 (O(log n)), 无需扫描范围外元素; **7.x 用 zslNthInRange 统一首/末/偏移三态** (替代 FirstInRange/LastInRange)。[模式: 范围跳跃定位]
数据流: ZRANGEBYSCORE → IsInRange 判空 → 顶层跳过范围外 → 定位边界 → 遍历输出。

### 7. 命令面 — ZADD 的选项宇宙

场景: NX/XX/GT/LT/INCR 怎么在一个函数里处理?
源码路径:
- `t_zset.c:1425+` (zsetAdd) — 选项解析 (L1427-1435) + **NaN 全局守卫** (L1435-1436: "NaN as input is an error regardless of all the other parameters")
- listpack 路径: zzlFind → 选项判定 → score 变更先删后插 (L1473-1475) → 超阈值转换 (L1480)
- skiplist 路径: dictFind → 选项 → zslDelete+zslInsert+dictAdd
关键设计: 编码透明命令核 (q7): 选项语义在两编码路径完全一致 — 命令面对编码无感知; NaN 是最高优先级错误。[模式: 统一语义 + 双路径路由]
数据流: ZADD NX GT → 编码判定 → 路径选择 → 选项判定 → 插入/更新。

### 8. 转换与阈值 — listpack 小时代

场景: zset 什么时候从紧凑编码变成跳表?
源码路径:
- `config.c:3219` — zset-max-listpack-entries=128 / zset-max-listpack-value=64B
- `t_zset.c:1270-1300` (zsetConvertAndExpand) — **dictExpand 预扩** (L1292, cap=元素数, 转换零 rehash) → lpSeek 遍历 → 逐元素 zslInsert+dictAdd 双写; 创建路径同样预扩 (zsetTypeCreate L1248 size_hint)
- 触发: zsetAdd 超阈值命令内转换 (L1480); **单向** (skiplist 不缩回)
关键设计: 阈值转换 (q8): ≤128 元素用 listpack (紧凑线性), 超阈值一次转双结构; dictExpand 预扩让转换 O(n) 无迁移开销。[模式: 阈值转换 + 预扩]
数据流: ZADD 超 128 → dictExpand(cap) → 逐元素双写 → 释放 listpack → SKIPLIST 编码。

### 负面空间 — zset 刻意不做的事

- **不用红黑树/AVL**: 实现复杂 (旋转), 范围遍历需中序; skiplist 简单 + 天然有序遍历 (工程选择)
- **不做 skiplist 降级**: 转换单向 (listpack→skiplist 不回头)
- **不做原地更新**: score 变更 = 先删后插 (跳表位置可能全变)
- **不做 P=0.5**: 内存敏感选 P=0.25 (省指针)
- **不做多 key 排序结构**: 跨 key 排序是 ZUNION/ZINTERSTORE 的临时结构

→ 引出: list 的存储是 quicklist — listpack 分页 + LZF 压缩 → [[R-5-quicklist]]
