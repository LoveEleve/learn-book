# 闭环笔记 q3: 布隆过滤 — 两级机制 + 参数数学

## 假设
CQ 位图粗筛 (布隆) + CommitLog 精筛 (表达式); 参数 f/n → k/m。

## 验证过程
- **参数** (BloomFilter L70-86): f (误判率 1-99%) + n (预期元素) → **k = ceil(log(0.5, f))** (哈希次数) → **m = n×log2(1/f)×log2(e)** (位数组, 8 对齐) — 数学注释 (p=e^(-kn/m), f=(1-p)^k)
- **双哈希** (calcBitPositions L92-108): murmur3_128 → hash1+hash2 → **k 个位置 = hash1 + i×hash2** (Kirsch-Mitzenmacher "Less Hashing" 论文)
- **两级过滤** (ExpressionMessageFilter):
  - **isMatchedByConsumeQueue** (L60-110): TAG → codeSet; SQL92 → **Ext 位图命中** (cqExtUnit.getFilterBitMap + **isMsgInLive 时间窗** + bitNum 校验) — 粗筛 (位图可能误判)
  - **isMatchedByCommitLog** (L112+): SQL92 → **compiledExpression.evaluate 精筛** (消息属性)
  - **粗筛通过但精筛拒绝** = 布隆误判被精筛纠正
- **位图失效兜底**: filterBitMap null/位宽不符 → return true (走精筛)

## 代码类型
Algorithmic (概率过滤)

## 跨域关联
- RM-3 (CQ Ext): filterBitMap 存储
- RM-5 (Broker): ConsumerFilterManager 持有 BloomFilter

## 结论
布隆两级: CQ 位图 (O(1) 粗筛, 误判可忍) + CommitLog 表达式 (精筛兜底); 参数数学精确 (f→k, n→m); 双哈希加速。
源码位置: BloomFilter.java:70-108; ExpressionMessageFilter.java:60-130
