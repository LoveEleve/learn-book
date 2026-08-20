# E-11 闭环笔记 Q5-Q8: 排序比较器/缓存/对照/聚合衔接

## Q5: 排序怎么走 docValues

假设: fieldcomparator 从 docValues 读值排序, 不走倒排 — 排序是"列式读取"面。

验证过程:
- Read LongValuesComparatorSource (LongValuesComparatorSource.java:40-86): `Function<SortedNumericDocValues, SortedNumericDocValues> converter` (L43) + `loadDocValues` (LongValuesComparatorSource.java:75-78, 从 LeafReaderContext 取 SortedNumericDocValues) + `getNumericDocValues` (LongValuesComparatorSource.java:86, 含 missingValue 兜底)
- 4 源: BytesRef/Long/Double/Float (fieldcomparator/ 4 文件)
- 设计: 排序需要"每文档一个值" → SortedNumericDocValues 列式读取 (磁盘顺序) 优于倒排随机访问

代码类型: Implementation (列式读取)

结论: **排序走 docValues 列式面: comparator source 从 LeafReader 取 SortedNumericDocValues, 转换器 + missingValue 兜底后逐文档比较 — 磁盘顺序读优于倒排随机访问**。LongValuesComparatorSource.java:43,75-86

## Q6: 缓存生命周期

假设: IndexFieldDataCache 按索引缓存字段数据, clear 时全清。

验证过程:
- Read IndexFieldDataCache (IndexFieldDataCache.java:18-35): "A simple field data cache abstraction on the *index* level" — clear() (IndexFieldDataCache.java:30) 全清 / clear(fieldName) (L35) 按字段清
- 失效场景: 索引 close/删除/字段更新 → clear
- 缓存对象: LeafFieldData (段级) + GlobalOrdinals (跨段)

代码类型: Interface (缓存契约)

结论: **fielddata 缓存是索引级: 段级 LeafFieldData + 跨段 global ordinals; 失效 = clear() 全清或按字段清 — 与 refresh 无关 (docValues 不变, 缓存有效)**。IndexFieldDataCache.java:30-35

## Q7: 与客户端本地缓存对照 — 无一致性问题的缓存

假设: 服务端 fielddata 无一致性需求 (docValues 只读不可变), 客户端 RLocalCachedMap 需要一致性协议 — 两类缓存的本质差异。

验证过程:
- 服务端: fielddata 缓存的是**不可变磁盘数据** (docValues) — 无需失效协议, 只有容量问题 (断路器)
- 客户端: RLocalCachedMap 缓存的是**可变分布式数据** — 需要失效广播 (INVALIDATE/UPDATE, 对照 [[rd6-localcachedmap]])
- 对照结论: "服务端缓存管内存, 客户端缓存管一致" — 两类问题的解决方向完全不同

代码类型: 对照分析

结论: **fielddata = 只读磁盘数据的堆缓存 (无一致性, 断路器管容量); RLocalCachedMap = 可变分布式数据的本地副本 (失效协议管一致性) — 缓存维度: 服务端看容量, 客户端看一致**。对照锚点: [[rd6-localcachedmap]] + IndexFieldDataCache.java:18

## Q8: terms 聚合的加速 — global ordinals 消费方

假设: terms 聚合跨段计数需要 global ordinals (去重编号 + 全局词频), GlobalOrdinalsBuilder 是前置构建。

验证过程:
- GlobalOrdinalsStringTermsAggregator (E-12 文件, 974 行): 消费 GlobalOrdinalsIndexFieldData
- 加速原理: global ordinals 让跨段 terms 聚合直接按全局编号聚合, 无需逐段合并字符串
- 内存: global ordinals 构建时断路器保护 (Q2/Q3)
- 懒加载: 首次 terms 聚合触发 loadGlobal, 缓存后续复用 (Q6)

代码类型: 衔接分析

结论: **terms 聚合 = global ordinals 的主要消费者: 跨段去重编号 + 全局计数, 一次构建 (断路器保护) 多次复用 (缓存) — global ordinals 是"聚合加速的堆内索引"**。衔接锚点: GlobalOrdinalsBuilder.java:72 → E-12 GlobalOrdinalsStringTermsAggregator

跨域关联: E-12 Aggregations (消费方) / E-2 Search (排序面)
