# E-12 Aggregations 篇 2/3 — 桶聚合: terms/composite/遍历策略

> 前置: [[E-12-aggregations-01]] [[E-11-fielddata-01]] (global ordinals) | 复用: — | 对照: [[r21-db]] (键空间遍历) | 引出: [[E-12-aggregations-03]]
> 🔴 A | 来源: GlobalOrdinalsStringTermsAggregator.java:55-63,127-139 + BestBucketsDeferringCollector.java:37-60 + CompositeAggregator.java:72-80,203-216,596-602
> 定位: Aggregations 卷中篇 — 回答"terms 怎么加速? 深度/广度优先差在哪?"

**读者处境**: 面试官问 "terms 聚合怎么算? 高基数会慢吗? 深度优先和广度优先是什么?" 你答 "global ordinals + collection_mode" — 但再问 "延迟桶是什么? composite 和 terms 区别?" 你卡住了。这篇是桶聚合的完整答案。

### 1. 问题引入 — 桶聚合的三类问题

场景: terms/date_histogram/composite 都是"分桶" — 怎么分得快? 桶太多怎么办? 分页怎么搞?
- 加速: global ordinals (E-11) 编号计数
- 保护: 深度优先延迟桶 (只重放 top)
- 分页: composite after_key
- 本篇问题: terms 加速 (Q3) / 遍历策略 (Q4) / composite (Q5)

### 2. terms 加速 — global ordinals 编号桶

场景: 高基数字段 terms 聚合怎么快?
- GlobalOrdinalsStringTermsAggregator (GlobalOrdinalsStringTermsAggregator.java:55-63): collectionStrategy (GlobalOrdinalsStringTermsAggregator.java:63)
- collect (GlobalOrdinalsStringTermsAggregator.java:127-139): advanceExact (L128) → ordValue (L131) → collectGlobalOrd (L132) — **直接按全局编号计数**
- 无过滤快路径 (GlobalOrdinalsStringTermsAggregator.java:121-135): acceptedGlobalOrdinals == ALWAYS_TRUE
- 衔接: E-11 GlobalOrdinalsBuilder 构建, 本类消费 — 两域闭环

### 3. 深度优先 vs 广度优先 — 延迟桶

场景: collection_mode 选哪个?
- 深度优先 (默认): BestBucketsDeferringCollector (BestBucketsDeferringCollector.java:37-42) — 第一遍全收集 → 选 top → 第二遍只重放 top 桶子聚合
- 广度优先: 每段直接维护 top — 无二次遍历但每段完整子聚合
- 权衡: 子聚合重 (嵌套深) 选深度优先省内存; 子聚合轻选广度优先省遍历
- 选择: terms 的 collection_mode 参数

### 4. composite — 多字段组合桶 + after_key

场景: 多字段分组怎么分页?
- CompositeAggregator (CompositeAggregator.java:72-80): rawAfterKey (CompositeAggregator.java:80) 分页游标
- collect (CompositeAggregator.java:596-602): queue.compareCurrent (CompositeAggregator.java:598) → 匹配收集 (CompositeAggregator.java:602)
- CompositeKey (CompositeKey.java:22-25): 多字段组合键
- 与 terms 区别: terms 单字段 top N; composite 多字段流式分页 (无 top N)

### 5. 收束 — 桶聚合的三种模型

- terms: 单字段 top N (global ordinals 加速)
- date_histogram: 时间桶
- composite: 多字段分页 (after_key)
- 共同保护: 桶上限 65536 (篇 1) + 遍历策略选择
- 引出: 篇 3 (pipeline/HLL 对照)

### 核心悬念
"高基数 terms 聚合为什么快?" — global ordinals 把字符串比较变成 int 编号计数: 每文档读序号 → 哈希编号 → 桶计数, 跨段聚合免字符串比较。

### 概念依赖链
Q3 global ordinals 加速 → Q4 遍历策略 → Q5 composite → (E-11 衔接)

### 源码锚点清单
- GlobalOrdinalsStringTermsAggregator.java:55-63 (类+strategy) / 97 (RemapGlobalOrds) / 121-135 (快路径) / 127-139 (collect)
- BestBucketsDeferringCollector.java:37-42 (类注释) / 60 (selectedBuckets)
- CompositeAggregator.java:72-80 (类+afterKey) / 203-216 (toCompositeKey/lastBucket) / 596-602 (collect+compareCurrent)
- CompositeKey.java:22-25 (组合键)
