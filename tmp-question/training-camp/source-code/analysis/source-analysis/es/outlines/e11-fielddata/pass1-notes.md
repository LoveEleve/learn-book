# E-11 FieldData — Pass 1 探索笔记 (扫轮廓)

> 🟡 B | 依赖: E-7 Mapping ✅ (docValues 字段决定) | 对照: [[rd6-localcachedmap]] (本地缓存) [[r23-evict]] (淘汰)
> 源码: server/src/main/java/org/elasticsearch/index/fielddata/ (103 文件 5940 行 + 3 子目录)
> 测试地图: server/src/test/.../index/fielddata/ 30+ 文件

## 继承树/调用图

```
IndexFieldData<FD> (接口, 255 行)
├── load(LeafReaderContext) (IndexFieldData.java:59) — 按段加载
├── loadDirect (IndexFieldData.java:64) / loadGlobal (L250) / loadGlobalDirect (L252) — global ordinals 面
└── 实现: plain/ (叶子加载器) + ordinals/ (global ordinals 构建)

GlobalOrdinalsBuilder (ordinals/, 核心):
    build (GlobalOrdinalsBuilder.java:39): OrdinalMap.build (L72) → memorySize 记账 (L73) → breaker.addWithoutBreaking (L74)
    → 断路器: GlobalOrdinalsBuilder 用 breakerService.getBreaker(FIELDDATA)

GlobalOrdinalsIndexFieldData (GlobalOrdinalsIndexFieldData.java:44): 全局序号包装 — loadGlobal (L83) / ramBytesUsed (L122)
```

## 基本元素分解 (原则二)

1. **IndexFieldData 加载契约** — load (IndexFieldData.java:59) 按段加载 docValues → LeafFieldData; loadGlobal (IndexFieldData.java:250) 跨段合并 global ordinals
2. **GlobalOrdinalsBuilder** — build (GlobalOrdinalsBuilder.java:39): OrdinalMap.build (L72) + 内存记账 (L73-74) — 跨段去重编号
3. **断路器** — HierarchyCircuitBreakerService: FIELDDATA_CIRCUIT_BREAKER_LIMIT_SETTING (L81-86) 默认 **40% heap** + overhead 1.03 (L88-92); 消费点 loadGlobalDirect (AbstractIndexOrdinalsFieldData.java:148)
4. **DocValues 读取面** — SortedBinaryDocValues/SortedNumericDocValues/SortedSetDocValues 抽象族 (E-7 决定字段面)
5. **排序比较器** — fieldcomparator/ (BytesRef/Long/Double/Float 4 源) — 排序走 docValues 不走倒排
6. **缓存** — IndexFieldDataCache (字段数据缓存) + FieldDataStats (统计)

## 标记问题 (≥5)

1. **Q1: load vs loadGlobal 的区别** — 段内序号 (per-segment ordinals) vs 全局序号 (global ordinals)? 何时需要 global?
2. **Q2: GlobalOrdinalsBuilder 构建流程** — OrdinalMap 怎么跨段合并? 内存怎么记账?
3. **Q3: 断路器怎么保护** — load 时 addEstimateBytesAndMaybeBreak? 超限抛 CircuitBreakingException?
4. **Q4: fielddata 历史 (ES 5.0)** — 为什么 5.0 把聚合/排序迁到 docValues? 旧 fielddata OOM 事故
5. **Q5: 排序怎么走 docValues** — fieldcomparator 4 源怎么用 SortedNumericDocValues?
6. **Q6: 缓存生命周期** — IndexFieldDataCache 什么时候失效? (refresh/clear?)
7. **Q7: 与本地缓存对照** — 客户端 RLocalCachedMap (一致性协议) vs 服务端 fielddata (纯容量问题) — 无一致性需求
8. **Q8: terms 聚合的加速** — GlobalOrdinals 供 terms agg (E-12 衔接)

## 已读测试 (2 个)

- `FieldDataTests.testSortableLongBitsToDoubles` (FieldDataTests.java:47): double↔long 位转换
- `FieldDataTests.testDoublesToSortableLongBits` (FieldDataTests.java:87)
- 断路器测试: CircuitBreakerTests (common/breaker)

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 2 个测试文件

## 跨域发现

- 来源: E-11 Pass 1 — GlobalOrdinalsBuilder.build 用 OrdinalMap (Lucene) — E-12 terms 聚合的 GlobalOrdinalsStringTermsAggregator 消费
- 发现: fielddata.breaker 与总断路器层次 (HierarchyCircuitBreakerService) — 内存保护面
- 已对照验证: GlobalOrdinalsBuilder.java:72,76 + HierarchyCircuitBreakerService.java:81-92
