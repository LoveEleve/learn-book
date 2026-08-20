# R-5 quicklist — 快表: 双向链表分页 + 压缩中间区

> 前置: [[R-19-listpack]] (PACKED 容器) + [[R-33-zmalloc]] | 引出: [[R-1-object]] | 对照: [[R-26-list]] (消费域)
> 🔴 A | 8 KP | [模式: 双向链表分页+双容器分流+访问感知压缩+延迟重压]
> Pass 2 闭环: q1(双容器) q2(fill) q3(压缩) q4(recompress) q5(分裂) q6(路由) q7(迭代) q8(演进)

**读者处境**: LPUSH 10 万个元素, 为什么内存紧凑又不慢?为什么大字符串会"单独住一个房间"?list-compress-depth 是什么?为什么两端的数据永远不压缩?这篇拆 quicklist: 双向链表分页、PLAIN/PACKED 双容器、fill 双语义、以及"中间区压缩 + 头尾永解压"的访问感知策略。

### 1. 结构 — 双向链表 + 每节点一个 listpack

场景: list 为什么不是一个大数组或普通链表?
源码路径:
- `quicklist.h:47-59` — quicklistNode: prev/next + entry + sz + **位域**: count:16 / encoding:2 (RAW/LZF) / container:2 (PLAIN/PACKED) / recompress / attempted / dont_compress / extra:9
- `quicklist.h:101-112` — quicklist: head/tail + count + len + fill:16 + compress:16 + bookmark_count:4 (64 位 40B)
关键设计: 分页链表 (q1 结构面): 普通链表每元素一节点 (内存碎); 大数组插入 O(n) 搬移 — **quicklist = 双向链表 + 每节点一个 listpack 打包**: 插入/删除在节点内 O(n) 但节点 ≤8KB; 节点间 O(1)。[模式: 分页双向链表]
数据流: LPUSH → 头部节点 listpack → 满则新节点。

### 2. 双容器 — 大元素"单独住"

场景: 1MB 的字符串进 list 会发生什么?为什么不开进 listpack?
源码路径:
- `quicklist.h:54` — container:2: PLAIN==1 (裸元素) / PACKED==2 (listpack)
- `quicklist.c:508-519` (isLargeElement) — `sz > 节点字节限制` → 大元素
- `quicklist.c:583-603` (PushHead) — `isLargeElement → __quicklistInsertPlainNode` — **大元素独立成 PLAIN 节点**
关键设计: 大小分流 (q1): 大元素进 listpack 会让节点内存爆炸 + 后续插入整体搬移; PLAIN 节点让大元素独立 (节点级 O(1) 操作)。[模式: 大小分流双容器]
数据流: LPUSH 1MB → isLargeElement → PLAIN 节点 (不进 listpack)。

### 3. fill — 节点上限的双语义

场景: list-max-listpack-size=-2 是什么意思?为什么是负数?
源码路径:
- `quicklist.c:472-482` (quicklistNodeLimit) — `fill >= 0 → 元素数; fill < 0 → 字节数`
- `quicklist.c:462-468` + `L49` — 负 fill 映射: `optimization_level = {4096, 8192, 16384, 32768, 65536}` — **-2 → 8KB (默认)**
- `config.c:3152` — list-max-listpack-size 默认 -2 (旧名 list-max-ziplist-size)
关键设计: 双度量 (q2): 正 fill = 每节点固定元素数 (分裂均匀, 大元素节点内存不均); 负 fill = 每节点固定字节 (内存均匀, 默认) — 内存敏感选择。[模式: 双语义配置]
数据流: fill=-2 → 每节点 ≤8KB → 超限分裂。

### 4. 压缩 — 冷中间区 + 收益阈值

场景: list-compress-depth=1 压谁?48 字节以下为什么不压?
源码路径:
- `quicklist.c:214-245` (__quicklistCompressNode) — 三条件: `sz >= MIN_COMPRESS_BYTES (48)` + `lzf_compress` 成功 + `lzf->sz + MIN_COMPRESS_IMPROVE (8) < node->sz` (**收益不足 8B 放弃**)
- `quicklist.c:307-345` (__quicklistCompress) — 长度 < compress×2 不压; **迭代跳过两端 compress 深度**
- `config.c:3174` — list-compress-depth 默认 0 (关闭)
关键设计: 访问感知压缩 (q3): LPUSH/LPOP/RPOP 高频操作两端 — 两端 compress 深度内永解压; 压缩只压冷中间区; 收益阈值防"压了白压"。[模式: 深度保护 + 收益阈值]
数据流: 节点变更 → __quicklistCompress → 深度外节点 → 压缩条件 → LZF。

### 5. recompress — 解压后延迟重压

场景: 访问压缩中的元素, 会反复压解吗?
源码路径:
- `quicklist.c:260-290` — DecompressNode + **recompress=1 标记**
- `quicklist.c:380-390` — 批量操作结束统一重压带标志节点
- `quicklist.c:311-312` — `assert(head/tail->recompress == 0)` — 头尾永不标记
关键设计: 延迟重压 (q4): 一次命令内多次访问同节点只解压一次, 用完标记, 批量后统一重压 — 压解频率大降; 代价是操作间隙内存峰值 (解压态)。[模式: 延迟重压]
数据流: LINDEX 压缩区 → 解压+标记 → 访问 → 命令结束重压。

### 6. 分裂与路由 — 复制裁剪 + 三路插入

场景: 中间插入时节点满了怎么办?LPUSH 有几种路径?
源码路径:
- `quicklist.c:971-1004` (_quicklistSplitNode) — **复制整包 → lpDeleteRange 双侧裁剪** (after? offset+1 : 0, extent -1=到结尾)
- `quicklist.c:583-603` (PushHead 三路) — PLAIN / lpPrepend (允许) / 新节点 (+相邻合并)
- `quicklist.c:521-534` — _quicklistNodeAllowInsert + **SIZE_SAFETY_LIMIT=8192** (L69, 单节点防爆)
关键设计: 极简分裂 + 三路路由 (q5/q6): 分裂 = 复制+裁剪 (复用 listpack 删除语义, 3×O(n) 换代码极简); 插入按 大元素/容量/满 三路分流, 删除后相邻合并防稀疏。[模式: 复制裁剪 + 三路分流]
数据流: LINSERT 中间 → 节点满 → 分裂 → 插入; LPUSH → 三路判定。

### 7. 迭代器 — 压缩透明的游走

场景: LINDEX 怎么穿过压缩节点?
源码路径:
- `quicklist.h:114+` (quicklistIter) — current 节点 + offset/zi (包内位置) + direction
- `quicklist.c:720+` (_quicklistNext) — **遇 LZF 自动解压** (recompress 标记)
- resetIterator (L142-148) — 结构变更后迭代器作废
关键设计: 透明迭代 (q7): 压缩对迭代无感 (需要时解压); 双向 direction 支撑反向; 变更即失效防悬垂。[模式: 自动解压迭代]
数据流: LRANGE 遍历 → 跨节点 → 遇压缩解压 → 返回元素。

### 8. 演进 — 从混合方案到快表

场景: 7.0 之前 list 怎么存的?为什么统一?
源码路径:
- 历史: 3.2 前 = 双 linkedlist (大) 或整体 ziplist (小) — **二选一, 无中间态**; 2014 Matt Stancliff 引入 quicklist (每节点 ziplist 打包)
- 7.x: 容器 ziplist → **listpack** (R-19)
- `quicklist.h:79-87` — bookmarks: 超大列表 (千节点) 的分段锚点 (按名定位, 柔性数组零默认开销, ≤16 个)
关键设计: 演进路径 (q8): linkedlist+ziplist 混合 → quicklist 分页 (2014) → listpack 容器 (7.x); **list 当前仅 quicklist 一种编码** (t_list.c:52); bookmarks 是超大列表的可选优化。[模式: 渐进式重构]
数据流: 旧 RDB 加载 → ziplist 容器 → 转 listpack。

### 负面空间 — quicklist 刻意不做的事

- **不做单节点无限增长**: fill/SIZE_SAFETY_LIMIT 双重上限 (防单节点内存爆炸)
- **不做全量压缩**: 只压中间区, 两端永解压 (访问面)
- **不做压缩保证**: 收益不足 (≤8B) 放弃压缩 (防无效 LZF)
- **不做 O(1) 随机访问**: LINDEX 是 O(n) 遍历 (跳表才是 O(log n), 对照 R-6)
- **不做 bookmarks 默认**: 超大列表专属 (≤16 个, 删除维护成本)

→ 引出: redisObject 是全部值的统一外壳 — 编码字段承载这一切 → [[R-1-object]]
