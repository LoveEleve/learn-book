# R-6 skiplist+ZSet — 知识规划 (knowledge-planning)

> 项目: Redis 7.4.2 | 🔴 A / 1 篇 (+harness) | t_zset.c (4513)+server.h zskiplist (1341-1360)
> 基线: REDIS-PLAN R-6 — 前置: **R-3 (dict) + R-19 (listpack) + R-4 (sds)** — 展开 双结构→层级概率→span 排名→复合排序→插入算法→范围查询→命令面→转换

---

## §0.8

- 🔴 A，1篇 — 双结构(**zset{dict: member→score O(1) 存在性, zsl: 有序 O(log n) 范围} server.h:1357-1360; 写路径双写: zslInsert+dictAdd L1520+, zslDelete+dictDelete L1589+**) → 层级概率(**P=0.25 几何分布: threshold=P*RAND_MAX, 期望 1.33 层 L126-132; MAXLEVEL 32 = log_4(2^64) 理论上限 server.h:514-515; 与经典 P=0.5 取舍: 省指针换常数**) → span 排名(**每 level 存跨越节点数 server.h:1345-1348; zslGetRank 累加 span O(log n) L508-528; 插入 span 更新: rank 差公式 L171-183**) → 复合排序(**同分按 ele 字典序 (sdscmp): 全序契约 L147-150, ZRANGE/ZRANK 确定性**) → 插入算法(**zslInsert: update[]+rank[] 双数组, 自顶向下查找+span 维护 L137-192; 新层初始化 span=length; 删除 level 惰性收缩 L211-212**) → 范围查询(**zslIsInRange 首尾 O(1) 判空 L317-334; zslNthInRange 统一首/末/偏移 (n=0/-1/offset) — **7.x 替代 FirstInRange/LastInRange** L336-410; ZSKIPLIST_MAX_SEARCH 小偏移逐节点优化; 消费者 ZRANGE L3345/ZCOUNT L3443**) → 命令面(**zsetAdd 双编码透明: NX/XX/GT/LT/INCR + NaN 全局守卫 L1425-1530; score 变更先删后插**) → 转换(**zsetConvertAndExpand: dictExpand 预扩免 rehash + 逐元素双写 L1270-1300; 单向**) → 小规模编码(**listpack ≤128/64B (config.c:3219), 超阈值命令内转换**)
- 设计模式: [模式: 双结构互补+概率层级+span 距离索引+复合全序]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| server.h:1357-1360; t_zset.c:1520+,1589+ | 双结构 | dict O(1) 存在 + skiplist O(log n) 有序; 双写一致 | High |
| t_zset.c:126-132; server.h:514-515 | 层级 | P=0.25 期望 1.33 层; MAXLEVEL 32=2^64 | High |
| server.h:1345-1348; t_zset.c:171-183,508-528 | span | 距离索引; 排名 O(log n); rank 差维护 | High |
| t_zset.c:147-150 | 复合排序 | (score, ele) 全序; sdscmp 字典序 | High |
| t_zset.c:137-192,196-214 | 插入/删除 | update/rank 双数组; level 惰性收缩 | High |
| t_zset.c:317-410,3345-3451 | 范围 | IsInRange O(1) 判空; zslNthInRange 统一; 小偏移优化 | High |
| t_zset.c:1425-1530 | 命令面 | NX/XX/GT/LT/INCR; NaN 守卫; 双编码透明 | High |
| t_zset.c:1248,1270-1300; config.c:3219 | 转换 | dictExpand 预扩 (转换 L1292/创建 L1248); 单向; 128/64B 阈值 | High |

---

## 02-04 聚合+分类+聚类 (1篇+harness)

**1篇理由**: zset 是单机制闭环 (双结构→概率→span→算法→命令), 1篇 (~85行) 按"双结构→层级→span 排名→复合排序→插入→范围→命令→转换"展开; harness 验证 skiplist 核心 (插入/查找/span/排名/范围)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 双结构互补 (dict+skiplist) | 🔴 | **为什么🔴**: 架构核心 |
| P1-2 | span 排名 (O(log n)) | 🔴 | **为什么🔴**: 杀手功能 |
| P1-3 | zslInsert 算法 (update/rank) | 🔴 | **为什么🔴**: 核心算法 |
| P1-4 | 范围查询 (zslNthInRange) | 🔴 | **为什么🔴**: 招牌卖点 |
| P2-1 | 层级概率 P=0.25 | 🟡 | **为什么🟡**: 参数权衡 |
| P2-2 | 复合排序契约 | 🟡 | **为什么🟡**: 确定性 |
| P2-3 | zsetAdd 命令面 | 🟡 | **为什么🟡**: 语义面 |
| P3-1 | 转换与阈值 | 🟢 | **为什么🟢**: 策略面 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **双结构与 span** | 🔴 | 核心 |
| B | **算法与范围** | 🔴 | 机制 |
| C | **概率与契约** | 🟡 | 参数 |
| D | **命令与转换** | 🟡 | 工程 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 双结构 | dict O(1) 存在性 + skiplist O(log n) 有序 — 双写一致 (单线程无锁); 空间翻倍换双操作面 | server.h:1357-1360; t_zset.c:1425-1530,1589-1600 |
| q2 | 层级概率 | P=0.25 几何分布期望 1.33 层 (省指针); MAXLEVEL=32=log_4(2^64) 理论上限; 与 P=0.5 的取舍 | t_zset.c:126-132; server.h:514-515 |
| q3 | span 排名 | 每层存跨越节点数 → 排名=路径 span 累加 O(log n); 插入 rank 差 O(1)/层维护 | server.h:1345-1348; t_zset.c:171-183,508-545 |
| q4 | 复合排序 | (score, ele) 全序: 重复分值唯一可定位; sdscmp 字典序保证命令确定性 | t_zset.c:147-150 |
| q5 | 插入算法 | update/rank 双数组: 查找累计排名 + span 差更新; 新层 span=length; 删除 level 惰性收缩 | t_zset.c:137-192,196-214 |
| q6 | 范围查询 | IsInRange 首尾 O(1) 判空; zslNthInRange 统一首/末/偏移 (7.x 替代 First/Last); 小偏移逐节点优化 | t_zset.c:317-410,3345-3451 |
| q7 | 命令面 | zsetAdd 双编码透明 + NX/XX/GT/LT/INCR + NaN 全局守卫; score 变更先删后插 | t_zset.c:1425-1530 |
| q8 | 转换 | listpack→skiplist: dictExpand 预扩免 rehash (L1292) + 逐元素双写; 创建路径预扩 (L1248); 单向 (≤128/64B 阈值) | t_zset.c:1265-1300; config.c:3219 |

→ 引出 R-5: quicklist 是 list 的存储 (listpack 分页+LZF) → [[R-5-quicklist]]
