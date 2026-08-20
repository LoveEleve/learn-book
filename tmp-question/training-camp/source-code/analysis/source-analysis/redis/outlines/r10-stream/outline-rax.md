# R-10a rax — 压缩路径基数树

> 前置: [[R-3-dict]] (对照) + [[R-19-listpack]] (stream 叶子) | 引出: [[R-10b-stream]] (主消费端) | 对照: [[R-3-dict]] (哈希 vs 前缀树)
> 🔴 A (拆篇 1/2) | 6 KP | [模式: 压缩路径 + 位置索引 + 分裂插入 + 前缀键 + 迭代器]
> Pass 2 闭环: q1(节点结构) q2(LowWalk) q3(ALGO 1) q4(ALGO 2) q5(查找) q6(迭代器)

**读者处境**: Stream 的百万消息 ID 怎么 O(log n) 查? rax 和字典树有什么区别? 压缩节点怎么分裂? 这篇拆 rax: 压缩路径、位置索引、ALGO 1/2 插入、前缀键、迭代器。

### 1. 节点结构 — 压缩 vs 非压缩

场景: 前缀树怎么省内存?
源码路径:
- **raxNode 位域** (rax.h:77-82): **4B 头 (uint32: iskey:1/isnull:1/iscompr:1/size:29)** + 边字符区 + 子指针区 + aux data; **权威注释: 键可在任意层级** (rax.h:99-103 "can represent a key with associated data in the radix tree at any level (not just terminal nodes)")
- **压缩节点** (raxCompressNode L374-405): 多条边字符合一, **单个子指针** (L397-400); 非压缩: 每字符一子
- 布局宏 (rax.c:129-154): raxPadding (对齐) / raxNodeLastChildPtr / raxNodeCurrentLength — 紧凑内存布局
- **raxNew** (L175-192): 空根 (非压缩 size=0) + numele/numnodes
- 栈: raxStack (L66-119) — 静态数组起步, 动态扩容 (父节点链)
关键设计 (q1): **边在节点上, 键在边上**: 公共前缀压缩为一条边 — 路径压缩比 O(n×len) 降到 O(分支数)。[模式: 压缩路径]
数据流: "foo"+"foobar" → 根边 'f' + 压缩 "oo" + "bar" 分裂。

### 2. raxLowWalk — 沿树下行

场景: 查找/插入怎么定位?
源码路径:
- raxLowWalk (rax.c:436-477): 从根沿边下行 — 压缩节点逐字符匹配 (L446-450) / 非压缩**位置匹配** (L451-460, edge[j]==c → children[j])
- **j 每次循环后重置 0** (L464-471) — 只有停在中间 break 才保留 splitpos
- 返回: stopnode + parentlink (替换槽) + splitpos (压缩节点内停止位置)
- 复杂度: O(匹配字符数 + 分支数), 压缩节点一步跳过多字符
关键设计 (q2): **parentlink + splitpos 双返回值**: 插入/删除需要"父中槽位"来替换节点 — 单趟定位。[模式: 单趟下行]
数据流: key → 逐节点匹配 → 停 (匹配完/不匹配/字符耗尽)。

### 3. 插入 ALGO 1 — 压缩节点分裂

场景: 在压缩节点中间插入怎么办?
源码路径:
- raxGenericInsert (L486-878): i==len 键尾 → 设键 (L502-527); 否则压缩节点处理
- **ALGO 1** (L596-780, i<len): **j==0 (3a)**: splitnode 替换原节点 + **iskey 继承** (L709-719); **j>0 (3b)**: trimmed [0..j) iskey 继承 + splitnode (边=分裂字符) (L719-740)
- **postfix** (L741-757): 原节点 [j+1..size) 或原子子 (postfixlen==0), splitnode->child[0] = postfix
- 新键余下: splitnode 加第二边 (L833-841 raxAddChild) + 压缩/单字符 (L819-830)
- **5 情形注释** (L529-595): "ANNIBALE" 系列分裂图解 (权威教学素材)
关键设计 (q3): **分裂三件套**: trimmed(前缀) + splitnode(分支) + postfix(后缀) — 原键归属 trimmed, 新键挂 splitnode 第二子。[模式: 分裂插入]
数据流: "foozap" vs 压缩 "bar" → splitnode 'b' + "ar"/"ap" 双子。

### 4. 插入 ALGO 2 — 前缀键与键保留

场景: 插入的键是已有键的前缀?
源码路径:
- **ALGO 2** (L759-806, i==len && j>0): 停在压缩节点内部但键已到尾
- **postfix = 原节点 [j..size) + iskey=1 + 新键数据** (L787-792); **trimmed = [0..j) + iskey 继承原节点** (L798-806); postfix->child = 原原子子
- **键可在压缩节点上**: raxCompressNode 保留 iskey (L384-394); raxFind 允许 splitpos==0 的压缩节点 (L899)
- 最终语义: 插入 "ANNI" 后树 = "ANNI"→"BALE"(新键)→[] (原键) — harness 实证 (真实 rax 编译运行验证)
关键设计 (q4): **键不独占叶子**: 压缩节点/中间节点都可承载键 — "ANNI" 数据挂在 postfix "BALE" 上。[模式: 前缀键]
数据流: 树 "ANNIBALE" + 插入 "ANNI" → trimmed "ANNI" + postfix "BALE"(新键) + 原子子(原键)。

### 5. 查找与删除 — raxFind/raxRemove

场景: 查找和删除的完整语义?
源码路径:
- **raxFind** (L895-904): `i != len || (iscompr && splitpos != 0) || !iskey → 0` — **完全匹配 (含压缩 splitpos==0) 且 iskey**
- raxRemove (L1000-1238): 删除键 + **节点收缩** (单子非键节点合并回压缩) — 与插入的逆操作
- **删除后压缩** (L1170+): 单子链合并 (raxCompressNode 逆)
- raxRemoveChild (L927+): 子指针摘除
关键设计 (q5): **查找 = 完全匹配 + iskey**: 停在任何节点 (压缩/非压缩) 只要完全匹配且是键 — 前缀键天然支持。[模式: 精确查找]
数据流: key → LowWalk → 完全匹配? → iskey? → 数据。

### 6. 迭代器 — raxSeek/raxNext

场景: 怎么按序遍历/范围查询?
源码路径:
- raxStart (L1264) / raxSeek (L1517-1553): 定位 — **>=/></=<== 比较 + ^ (首) + $ (末)** (L1543-1548); raxSeekGreatest (L1421)
- **raxNext/raxPrev** (L1681-1790): 中序推进 — 压缩节点展开为逐字符路径 (L1779-1784 "Same prefix: longer wins")
- 迭代器状态: RAX_ITER_JUST_SEEKED/EOF/SAFE (rax.h:149-153)
- **RAX_ITER_SAFE** (L1681+): 迭代中允许修改 (删除安全)
- raxStop (L1792): 释放迭代器
关键设计 (q6): **中序 = 字典序**: 叶子展开 + 前缀优先 — Stream 范围查询 (XREAD) 的基础。[模式: 有序迭代]
数据流: raxSeek(>=) → 逐键 Next → EOF。

### 负面空间 — rax 刻意不做的事

- **不做平衡**: 无旋转/无高度保证 (前缀树深度 = 键长)
- **不做字节级压缩率保证**: 压缩仅限单子链, 不压缩分支字符
- **不做并发安全**: 单线程 (Redis 语义)
- **不做值删除优化**: 删除仅收缩单子链
- **不做内存池**: 每节点独立分配

→ 引出: Stream 怎么用 rax 存百万消息? → [[R-10b-stream]]
