# E-12 闭环笔记 Q1-Q4: 生命周期/桶上限/global ordinals/延迟桶

## Q1: Aggregator 生命周期 — 递归组合模板

假设: AggregatorBase 用模板方法组合子聚合 — getLeafCollector 委托链 + postCollection 收尾。

验证过程:
- Read AggregatorBase.getLeafCollector (AggregatorBase.java:219-232): `preGetSubLeafCollectors` (AggregatorBase.java:220) → `sub = collectableSubAggregators.getLeafCollector(aggCtx)` (AggregatorBase.java:221) → 子类 `getLeafCollector(aggCtx, sub)` (AggregatorBase.java:222) — **模板方法: 父聚合包装子聚合 collector**
- collectableSubAggregators (AggregatorBase.java:46): 子聚合组合 (BucketCollector 包装)
- postCollection (AggregatorBase.java:96): 收集结束后逐级收尾 (桶排序/截断)
- 挂载 (AggregationPhase.preProcess (AggregationPhase.java:27-39)): 查询前创建聚合器树

代码类型: Implementation (组合模板)

结论: **Aggregator 是递归组合: 父聚合的 getLeafCollector 先取子聚合 collector (委托链), 再包装自己的逻辑 — 一次文档遍历逐层喂给整棵聚合树; postCollection 逐级收尾**。AggregatorBase.java:219-232 + AggregationPhase.java:27-39

跨域关联: E-2 QueryPhase (挂载点 L133) / Collector 组合模式

## Q2: 桶上限 — 65536 保护

假设: 桶数量硬限制防内存爆炸, 超限抛 TooManyBucketsException。

验证过程:
- Read MultiBucketConsumerService (MultiBucketConsumerService.java:32-38): `DEFAULT_MAX_BUCKETS = 65536` (L32) — 可配 (MAX_BUCKET_SETTING L33-35)
- TooManyBucketsException (MultiBucketConsumerService.java:55-60): extends AggregationExecutionException
- 消费: 桶聚合创建 bucket 时检查
- 语义: 高基数 terms 聚合最多 65536 桶 — 防"每文档一桶"内存爆炸

代码类型: Implementation (内存保护)

结论: **桶上限默认 65536 (可配 index.max_buckets): terms/date_histogram 等桶聚合超限抛 TooManyBucketsException 拒绝 — 防高基数聚合内存爆炸**。MultiBucketConsumerService.java:32-38,55-60

## Q3: terms 加速 — global ordinals 直接计数

假设: terms 聚合用 global ordinals (E-11) 按全局编号建桶, 避免字符串比较。

验证过程:
- Read GlobalOrdinalsStringTermsAggregator (GlobalOrdinalsStringTermsAggregator.java:55-63): 持有 collectionStrategy (GlobalOrdinalsStringTermsAggregator.java:63)
- collect (GlobalOrdinalsStringTermsAggregator.java:127-139): `singleValues.advanceExact(doc)` (GlobalOrdinalsStringTermsAggregator.java:133) → `ordValue()` (GlobalOrdinalsStringTermsAggregator.java:134) → `collectionStrategy.collectGlobalOrd(owningBucketOrd, doc, globalOrd, sub)` (GlobalOrdinalsStringTermsAggregator.java:135) — **直接按全局序号计数**
- 优化: acceptedGlobalOrdinals == ALWAYS_TRUE 时无过滤快路径 (GlobalOrdinalsStringTermsAggregator.java:121-135)
- 衔接: E-11 GlobalOrdinalsBuilder 构建 (跨段合并), 本类消费

代码类型: Algorithmic (编号加速)

结论: **terms 聚合 = global ordinals 直接计数: 每文档读全局序号 (E-11 构建), 按编号建桶 (哈希于 int 而非字符串) — 跨段聚合免字符串比较, 一次构建多次复用**。GlobalOrdinalsStringTermsAggregator.java:55-63,127-139

跨域关联: E-11 FieldData (global ordinals 构建方) — 两域闭环

## Q4: 深度优先 vs 广度优先 — 延迟桶

假设: 深度优先 (默认) 先收集所有候选桶再二次遍历选 top; 广度优先每段直接维护 top — 内存 vs 精度权衡。

验证过程:
- Read BestBucketsDeferringCollector (BestBucketsDeferringCollector.java:37-42): "collects all matches and then is able to replay a given subset of buckets" — **先全收集 + 选中子集重放**
- 流程: 第一遍收集全部候选 (selectedBuckets L60) → 排序取 top → 第二遍只重放 top 桶的子聚合
- 对照: 广度优先 (BreadthFirst) 每段直接维护 top, 不需二次遍历但每段都要完整子聚合
- 选择: SubAggCollectionMode (terms 聚合的 collection_mode 参数)

代码类型: Algorithmic (遍历策略)

结论: **深度优先 = 延迟桶 (BestBucketsDeferringCollector): 第一遍全收集 + 选 top + 第二遍只重放 top 桶子聚合 — 子聚合只跑在候选桶上省内存; 广度优先 = 每段直接维护 top (无二次遍历但每段完整) — 高基数子聚合场景选深度优先**。BestBucketsDeferringCollector.java:37-60
