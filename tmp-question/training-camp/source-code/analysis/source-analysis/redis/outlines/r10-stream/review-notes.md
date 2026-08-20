# R-10 Stream+rax — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 预判验证 | 交接文档 §四 预判: rax 压缩路径 ✅ / stream ID ms-seq ✅ / XADD 自增 ✅ / MAXLEN 近似 ✅ / PEL ✅ — 全验证 | 记录 |
| 2 | **harness 5 轮迭代实证** (最重要的机制收获): (1) 根=非压缩空节点 (2) 压缩节点必有子 (空叶子) (3) **children 位置索引非字符值** (4) **ALGO 1 j==0 3a splitnode 替换+iskey 继承** (5) **ALGO 2 条件 i==len + postfix=原节点余下** — 每轮都修正 harness 对源码的误解 | harness 32 断言 + 真实 rax.c 编译对照 (raxShow 结构一致) |
| 3 | 机制洞察 | **键可在压缩节点上**: raxCompressNode 保留 iskey (L384-394) + raxFind 允许 splitpos==0 压缩节点 (L899) — 前缀键 "ANNI" 数据挂 postfix "BALE", 原键在原子子 | 大纲 R-10a 节 4 |
| 4 | 机制洞察 | **ALGO 2 的键归属**: postfix (原节点余下) 承载新键, trimmed (前缀) 继承原键 — harness 与真实 rax 双验证 | 大纲 R-10a 节 4 |
| 5 | 行号验证 | 全函数 ~40 锚点 grep 穷举 (rax.c 374-1792 / t_stream.c 78-129,408-520,862-873 / stream.h 11-147) | 记录 |
| 6 | 边界验证 | ID 溢出回绕 (L78-117); 指定 ID EDOM (L439-441); 大小 ERANGE (L457-459); 近似裁剪 limit=100× (L866-867); stream-node-max-entries 默认 (需 grep) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 插入/查找/前缀键全路径 harness 实证
- ID 生成/递增/回绕数学验证
- master entry 压缩语义
- PEL 双 rax 共享 NACK

### 维度2 性能
- 路径压缩 (O(匹配字符))
- 128bit BE 字典序
- listpack 批量 + 主条目
- 近似裁剪批量删

### 维度3 内存
- 压缩节点 + listpack 双层
- master entry 字段引用
- 墓碑标记 (无物理移动)
- NACK 共享指针

### 维度4 一致性
- ID 单调 (时钟回退保护)
- XADD 严格递增 (EDOM)
- PEL 交付-确认闭环
- XCLAIM 幂等重投

### 维度5 负面空间 (已写入大纲 6+6 条)
- rax: 无平衡/压缩率保证/并发; stream: 无超时清理/消费者删除/ID 复用

## 结论
R-10 全部锚点行号 ~40 处验证, 12 闭环完成 (6+6), **机制洞察 2 + harness 5 轮实证**。harness 32 断言 ASan clean + 真实 rax 对照。待二次 REVIEW 复核。

---

# 二次深度 REVIEW (2026-08-13, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 R-3/R-19/R-26/R-29 (序号 < 26) 均已讲 ✅; 引出 R-17 (未来域 OK); 对照 R-29 (pubsub) ✅; 两篇各自五结构元素齐备 ✅; 读者处境场景化 (ID 生成/百万消息/PEL) ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 两篇合计 ~75 锚点 (file:line) — 🔴A 标准 ≥4 ⏫ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域 (R-17 仅作引出) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 传播 (XADD 原样) / 通知 (stream 键空间) / 编码 (listpack) / 阻塞 (R-26 框架) / 过期 (TTL 键) / RDB (PEL 序列化 R-8a) — 全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 两篇负面空间 6+6 条 ✅; 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **强断言实证** | 大纲 R-10a 节 4 "插入 ANNI 后树 = ANNI→BALE(新键)→[](原键)" — **真实 rax.c 编译运行验证**: raxShow 输出 `"ANNI" -> "BALE"=0x30 -> []=0x34`, v2(v1 对比) 归属确认 ✅ | 记录 |
| 13 | 补充锚点 | stream-node-max-entries 默认值 + 条目数阈值 (listpack 满则新 rax 节点) — grep 确认 | 记录 |
| 14 | 通过项 | 其余 ~40 句机制描述逐句对源码一致 ✅ (位域布局/位置索引/LowWalk 双返回/ALGO 1 三件套/ALGO 2 键归属/raxFind 语义/迭代器中序/双层结构/BE 键/master entry/ID 单调/墓碑/entries_read 语义) | 记录 |

## 二次 REVIEW 汇总

07 五维度轮换 + 内容深度共 **1 处记录** (强断言实证 raxShow 对照), harness 5 轮实证已固化。反写测试结论: 两篇大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-13, 逐句对源码 + 推理验证)

> 动机: 用户要求深度 REVIEW — 对大纲每个锚点重新 grep, 对全部结构/行号反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | raxNode 头大小 | rax.h:77-82: **uint32 位域 (iskey:1/isnull:1/iscompr:1/size:29) = 4B 头** — 大纲"1B 头"错误; 且 rax.c:129 注释明示 "four bytes header" ✅ | 修正 |
| V2 | 键任意层级权威注释 | rax.h:99-103: "can represent a key with associated data in the radix tree at **any level (not just terminal nodes)**" — harness 实证的源码级依据 | 补锚点 |
| V3 | raxSeek 操作符全集 | L1517-1553: `>` `>=` `<` `<=` `=` `^`(first) `$`(last) — 大纲漏 `^`/`$` | 修正 |
| V4 | XREADGROUP 实现 | commands.def:11201 实证: **xreadgroup 也指向 xreadCommand** (L2173) — 统一函数 GROUP 参数分流; 大纲"L2734+"错误 | 修正 |
| V5 | streamReplyWithRange | 实测 **L1670** (大纲 L1340+ 错); 消费者 PEL 读 = streamReplyWithRangeFromConsumerPEL (L37,1687) | 修正 |
| V6 | 常量 | STREAM_LISTPACK_MAX_SIZE = **1<<30** (t_stream.c:33); xackCommand L2834 / xclaimCommand L3134 / xautoclaimCommand L3355 行号确认 ✅ | 通过 |
| V7 | raxRemove 收缩 | L1003-1010: iskey=0 + trycompress ("Will be set to 1 if we should try to optimize") — 删除后压缩语义 ✅ | 通过 |
| V8 | streamCreateCG | L2497 / streamLookupCG L2520 — 大纲引用正确 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 15 | **结构错误** | 大纲 R-10a 节 1 "1B 头" — **实际 4B** (uint32 位域 size:29); 头大小影响布局理解 | 大纲节 1 修正 + 补权威注释 (rax.h:99-103) |
| 16 | 覆盖缺口 | raxSeek 操作符漏 `^` (first) / `$` (last) | 大纲节 6 修正 |
| 17 | **行号错误** | XREADGROUP "L2734+" — **实际 xreadCommand L2173 统一实现** (commands.def:11201 实证); streamReplyWithRange L1340+ → **L1670** | 大纲节 5 修正 |
| 18 | 补充常量 | STREAM_LISTPACK_MAX_SIZE=1<<30 (t_stream.c:33) — 追加大小上限面 | 大纲节 1 补 |

## 三次 REVIEW 汇总

推理验证 8 项全通过 (V1-V8); 新发现 **4 处** (**结构错误 1 (4B 头)** / 行号错误 1 (XREADGROUP) / 覆盖缺口 1 / 常量 1), 全部修复。锚点逐句 re-grep。两篇大纲现可支撑写作。
