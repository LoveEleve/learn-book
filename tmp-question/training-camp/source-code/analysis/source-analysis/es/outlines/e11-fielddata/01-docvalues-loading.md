# E-11 FieldData 篇 1/2 — docValues 读取面: 加载与 global ordinals

> 前置: [[E-7-mapping-01]] (docValues 字段面) | 复用: — | 对照: [[r23-evict]] (Redis 淘汰) | 引出: [[E-11-fielddata-02]] [[E-12-aggregations]] [[E-2-search]]
> 🟡 B | 来源: IndexFieldData.java:59,250-252 + GlobalOrdinalsBuilder.java:39-90 + LongValuesComparatorSource.java:43,75-86 + IndexFieldDataCache.java:18-35
> 定位: FieldData 卷开篇 — 回答"fielddata 怎么从 docValues 读取? global ordinals 什么时候需要?"

**读者处境**: 面试官问 "ES 聚合/排序的内存从哪来? global ordinals 是什么?" 你答 "docValues" — 但再问 "per-segment ordinals 和 global ordinals 差在哪? 什么时候用哪个?" 你卡住了。这篇是 docValues 读取面的完整答案。

### 1. 问题引入 — 只读数据的堆缓存

场景: docValues 在磁盘 (列式), 聚合/排序要频繁访问 — 怎么加速?
- 答案: fielddata = docValues 的堆缓存 (IndexFieldDataCache.java:18 "field data cache on the *index* level")
- 本篇问题: 怎么加载 (Q1) / 跨段怎么合并 (Q2) / 排序怎么用 (Q5) / 缓存怎么失效 (Q6) / 聚合怎么加速 (Q8)

### 2. 加载契约 — load vs loadGlobal

场景: 什么时候只需要段内数据, 什么时候要全局?
- 接口 (IndexFieldData.java:43-64): `load(LeafReaderContext)` (L59) 按段 + `loadDirect` (L64); `loadGlobal` (L250) / `loadGlobalDirect` (L252) 跨段
- 语义: per-segment ordinals (段内编号) vs global ordinals (跨段去重编号)
- 何时需要 global: terms 聚合跨段计数 (E-12 衔接); 排序/单段查询只需段内

### 3. GlobalOrdinalsBuilder — 跨段合并

场景: global ordinals 怎么构建?
- build (GlobalOrdinalsBuilder.java:39-90): 逐段 load (GlobalOrdinalsBuilder.java:51-53) → FilterTermsEnum 包装 (GlobalOrdinalsBuilder.java:59-71, **每 65536 次 next 查断路器** GlobalOrdinalsBuilder.java:65) → `OrdinalMap.build` (GlobalOrdinalsBuilder.java:72) → `ramBytesUsed` 记账 (GlobalOrdinalsBuilder.java:73) → `addWithoutBreaking` (GlobalOrdinalsBuilder.java:74)
- 返回 GlobalOrdinalsIndexFieldData (GlobalOrdinalsIndexFieldData.java:78-90)
- 关键设计: 长遍历期间的断路器检查点 (防构建过程 OOM)

### 4. 排序与缓存 — 消费面

场景: 排序怎么用 fielddata? 缓存怎么失效?
- 排序: LongValuesComparatorSource (LongValuesComparatorSource.java:40-86): converter (L43) + loadDocValues (L75-78) + getNumericDocValues (L86, missingValue 兜底) — 4 源 (BytesRef/Long/Double/Float)
- 缓存: IndexFieldDataCache clear() (IndexFieldDataCache.java:30) 全清 / clear(fieldName) (IndexFieldDataCache.java:35) 按字段 — 与 refresh 无关 (docValues 不变)
- terms 聚合: global ordinals 是主要消费者 (Q8, E-12 衔接)

### 5. 收束 — 读取面的定位

- load (段内) vs loadGlobal (跨段) — 粒度选择
- global ordinals = "聚合加速的堆内索引" (一次构建多次复用)
- 引出: 篇 2 (内存保护: 断路器 + 5.0 迁移史)

### 核心悬念
"per-segment 和 global ordinals 差在哪?" — 段内编号每个段从 0 开始, 跨段聚合会撞号; global ordinals 用 OrdinalMap 统一映射, 让聚合按全局编号直接计数。

### 概念依赖链
Q1 加载契约 → Q2 跨段合并 → Q5 排序 → Q6 缓存 → Q8 聚合衔接

### 源码锚点清单
- IndexFieldData.java:43-64 (接口) / 59 (load) / 250-252 (loadGlobal/loadGlobalDirect)
- GlobalOrdinalsBuilder.java:39-90 (build) / 51-53 (逐段 load) / 59-71 (FilterTermsEnum) / 65 (断路器检查点) / 72 (OrdinalMap.build) / 73 (ramBytesUsed) / 74 (addWithoutBreaking)
- GlobalOrdinalsIndexFieldData.java:44 (包装类) / 83 (loadGlobal) / 122 (ramBytesUsed)
- LongValuesComparatorSource.java:43 (converter) / 75-78 (loadDocValues) / 86 (getNumericDocValues)
- IndexFieldDataCache.java:18-35 (缓存契约) / 30 (clear) / 35 (clear by field)
