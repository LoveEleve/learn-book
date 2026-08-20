# E-2 Search 篇 2/3 — 打分与获取: BM25 与 Fetch

> 前置: [[E-2-search-01]] (分片查询) | 复用: — | 对照: [[r24-string]] (Redis 字符串) | 引出: [[E-2-search-03]] [[E-11-fielddata]]
> 🔴 A | 来源: SimilarityProviders.java:255-262 + LegacyBM25Similarity.java:35-69 + FetchPhase.java:59,110-115,268
> 定位: Search 卷中篇 — 回答"相关性怎么算? _source 怎么取?"

**读者处境**: 面试官问 "ES 怎么算相关度? BM25 公式?" 你答 "k1=1.2, b=0.75" — 但再问 "k1 和 b 分别管什么? LegacyBM25 和标准 BM25 差在哪? Fetch 阶段怎么取 _source?" 你卡住了。这篇是打分与获取的完整答案。

### 1. 问题引入 — 排序的两半

场景: 搜索结果"相关度"从哪来? 文档内容怎么取?
- Query 阶段: BM25 打分 → topDocs (doc_id + score)
- Fetch 阶段: 按 doc_id 取 _source/stored_fields
- 本篇问题: BM25 参数 (Q3) + Fetch 按需加载 (Q4)

### 2. BM25 — 词频饱和 + 长度归一化

场景: 为什么"出现次数多"不等于"更相关"?
- 参数 (SimilarityProviders.java:255-262): `k1 = settings.getAsFloat("k1", 1.2f)` (SimilarityProviders.java:258) / `b = settings.getAsFloat("b", 0.75f)` (SimilarityProviders.java:259)
- 语义 (LegacyBM25Similarity javadoc L51-53): **k1 控制词频饱和 (saturation)** — 出现 10 次不是 1 次的 10 倍相关; **b 控制长度归一化** — 短文档匹配更稀有
- Legacy 差异 (SimilarityProviders.java:67): `boost * (1 + k1)` — 兼容旧打分 (boost 乘进 k1+1)
- @Deprecated (SimilarityProviders.java:33): 新代码直接用 Lucene BM25Similarity

### 3. Fetch — 按需加载

场景: Fetch 阶段怎么取文档内容?
- StoredFieldsSpec.build(processors) (FetchPhase.java:110) — **各子阶段声明所需字段** → merge sourceLoader (SimilarityProviders.java:111) → StoredFieldLoader.fromSpec (SimilarityProviders.java:113)
- requiresSource (SimilarityProviders.java:115): 是否需 _source
- 逐 hit: hitContext (SimilarityProviders.java:268) 携带 leafStoredFieldLoader.storedFields + source
- 设计: 子阶段 (highlight/vectors/...) 声明需求, 合并后一次性加载 — 最小 IO

### 4. 收束 — 打分与获取的配合

- Query 给 score (BM25 参数化), Fetch 给内容 (按需加载)
- BM25 参数可调 (k1/b), 影响相关性; Fetch 只读需要的
- 引出: 篇 3 (分布式面: 协调节点怎么合并各分片 topDocs)

### 核心悬念
"为什么一个词出现 100 次的文档不比出现 5 次的文档相关 20 倍?" — BM25 的词频饱和 (k1): 收益递减; b=0.75 让长文档的词频被惩罚 (更可能是偶然出现)。

### 概念依赖链
Q3 BM25 → Q4 Fetch → (篇 3 分布式)

### 源码锚点清单
- SimilarityProviders.java:255-262 (createBM25Similarity) / 258 (k1=1.2) / 259 (b=0.75)
- LegacyBM25Similarity.java:35 (类) / 51-53 (参数 javadoc) / 67 (boost*(1+k1)) / 33 (@Deprecated)
- FetchPhase.java:59 (execute) / 110-115 (StoredFieldsSpec/StoredFieldLoader) / 268 (hitContext)
