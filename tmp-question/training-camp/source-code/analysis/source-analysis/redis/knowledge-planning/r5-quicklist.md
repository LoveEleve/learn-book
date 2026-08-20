# R-5 quicklist — 知识规划 (knowledge-planning)

> 项目: Redis 7.4.2 | 🔴 A / 1 篇 (+harness) | quicklist.c (3334)+quicklist.h (215)
> 基线: REDIS-PLAN R-5 — 前置: **R-19 (listpack 容器) + R-33** — 展开 双容器→fill 双语义→压缩→recompress→分裂→路由→迭代→演进

---

## §0.8

- 🔴 A，1篇 — 结构(**quicklistNode: prev/next+entry+sz+位域 count:16/encoding:2/container:2/recompress/attempted/dont/extra:9 quicklist.h:47-59; quicklist: head/tail+count+len+fill:16+compress:16+bookmark_count:4 L101-112**) → 双容器(**PACKED=listpack / PLAIN=大元素裸节点: isLargeElement (sz>fill 字节限) → __quicklistInsertPlainNode L508-519,571-603**) → fill 双语义(**正=元素数上限 (fill==0→1) / 负=字节上限 (optimization_level={4096,8192,16384,32768,65536} 2^k 映射, 默认 -2=8KB) L462-482; config.c:3152**) → 压缩(**三条件: 两端 compress 深度外 + ≥MIN_COMPRESS_BYTES 48 + 收益 ≥MIN_COMPRESS_IMPROVE 8 (lzf_compress 失败也放弃) L214-252; __quicklistCompress 迭代跳过两端 L307-345; 默认 compress=0 config.c:3174**) → recompress(**读时解压+标志, 批量后统一重压 L260-290,380-390; 头尾永不压缩/重压 L311-312**) → 分裂(**复制整包+lpDeleteRange 双侧裁剪 (after? offset+1:-1 语义) L971-1004**) → 路由(**PushHead 三路: PLAIN/prepend/新节点+相邻合并 L521-603; SIZE_SAFETY_LIMIT 单节点防爆**) → 迭代(**quicklistIter 自动解压+双向 direction+resetIterator 失效 L720+**) → 演进(**2014 Matt Stancliff 替代 linkedlist+ziplist 混合; 7.x 容器 ziplist→listpack; bookmarks 超大列表分段锚点 (L79-87, 柔性数组零默认开销, ≤16); list 仅 quicklist 编码 (t_list.c:52)**)
- 设计模式: [模式: 双向链表分页+双容器分流+访问感知压缩+延迟重压]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| quicklist.h:47-59 | 节点位域 | count:16/encoding:2/container:2/recompress 等 | High |
| quicklist.c:508-519,571-603 | 双容器 | PLAIN 大元素独立; isLargeElement 判定 | High |
| quicklist.c:462-482; config.c:3152 | fill | 正=元素数/负=字节 (2^k 表); 默认 -2=8KB | High |
| quicklist.c:214-252,307-345 | 压缩 | 三条件; 两端深度; 默认关 | High |
| quicklist.c:260-290,380-390 | recompress | 延迟重压; 头尾守卫 | High |
| quicklist.c:971-1004 | 分裂 | 复制+双侧裁剪 | High |
| quicklist.c:521-603,1010+ | 路由 | 三路; 合并; 安全上限 | High |
| quicklist.h:70-112 | 演进/bookmarks | listpack 容器; 分段锚点 | High |

---

## 02-04 聚合+分类+聚类 (1篇+harness)

**1篇理由**: quicklist 是单机制闭环 (容器→限制→压缩→分裂→路由), 1篇 (~80行) 按"结构→双容器→fill→压缩→recompress→分裂→路由→演进"展开; harness 验证三路路由/分裂/fill 语义。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 双容器 (PLAIN/PACKED) | 🔴 | **为什么🔴**: 核心设计 |
| P1-2 | fill 双语义 | 🔴 | **为什么🔴**: 节点策略 |
| P1-3 | 压缩三条件 + 深度 | 🔴 | **为什么🔴**: 内存优化 |
| P1-4 | 分裂 (复制+裁剪) | 🔴 | **为什么🔴**: 核心算法 |
| P2-1 | recompress 延迟重压 | 🟡 | **为什么🟡**: 访问优化 |
| P2-2 | 三路路由 + 合并 | 🟡 | **为什么🟡**: 边界处理 |
| P2-3 | 迭代器 | 🟡 | **为什么🟡**: 访问面 |
| P3-1 | 演进与 bookmarks | 🟢 | **为什么🟢**: 历史/可选 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **容器与限制** | 🔴 | 核心 |
| B | **压缩与分裂** | 🔴 | 算法 |
| C | **访问与路由** | 🟡 | 边界 |
| D | **演进** | 🟢 | 历史 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 双容器 | 大元素 (超 fill) 单独 PLAIN 节点免 listpack 膨胀; 小元素 PACKED 打包 | quicklist.c:508-519,571-603 |
| q2 | fill | 正=元素数/负=字节 (2^k 表 {4K..64K}); 默认 -2=8KB — 内存均匀优先 | quicklist.c:462-482; config.c:3152 |
| q3 | 压缩 | 三条件: 深度外 + ≥48B + 收益 ≥8B; 默认关; 头尾永不压 | quicklist.c:214-252,307-345 |
| q4 | recompress | 读时解压+标记, 批量后重压; 一次操作多次访问免重复压解 | quicklist.c:260-290,380-390 |
| q5 | 分裂 | 复制整包 + lpDeleteRange 双侧裁剪 (复用 listpack 删除) — 3×O(n) 换极简 | quicklist.c:971-1004 |
| q6 | 路由 | 三路: PLAIN/就地/新节点 (+相邻合并); SIZE_SAFETY_LIMIT 防单节点爆 | quicklist.c:521-603 |
| q7 | 迭代 | 自动解压 + 双向 + 变更失效; 压缩对迭代透明 | quicklist.h:114+; quicklist.c:720+ |
| q8 | 演进 | 2014 quicklist 替代 linkedlist+ziplist; 7.x 容器→listpack; bookmarks 超大列表锚点 (零默认开销) | quicklist.h:70-112 |

→ 引出 R-1: redisObject 是全部值的统一外壳 → [[R-1-object]]
