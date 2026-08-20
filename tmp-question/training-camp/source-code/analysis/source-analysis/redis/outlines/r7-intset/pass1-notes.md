# R-7 intset — Pass 1 探索笔记

> 域: R-7 intset (整数集合) | 🟡 方案 B | 2026-08-13
> 源码: src/intset.c (560) + intset.h (57) | Redis 7.4.2
> 已读测试: intsetTest (intset.c:407-559): 编码边界断言/升级 16→32/16→64/32→64/有序一致性/stress

## 继承树/调用图

```
struct intset (intset.h:35-39):
  uint32_t encoding  (INTSET_ENC_INT16=2/32=4/64=8, 即元素字节宽)
  uint32_t length
  int8_t contents[]  (柔性数组, 有序排列)

核心操作:
  intsetNew (L98): 空集, encoding=INT16
  intsetAdd (L206-233): 值编码 > 当前 → intsetUpgradeAndAdd (升级插入)
                         否则 intsetSearch 二分 → intsetResize → intsetMoveTail → 插入
  intsetUpgradeAndAdd (L159-182): 升级 = 从后往前搬 (prepend 留头/尾空位) + 新值只可能在前 (负数) 或后 (正数)
  intsetRemove (L236-253): 二分 → MoveTail 覆盖 → resize 缩
  intsetSearch (L117-156): 二分 O(log n) + 首尾快速判断 (值>max 插尾/值<min 插头)
  intsetMoveTail (L184-203): memmove 按编码宽度
  访问: intsetFind (编码检查+二分) / Get (按位取, 字节序转换) / Random / Max / Min
  intsetValidateIntegrity (L302-343): 头部/大小一致/encoding 合法; deep: 无重复且严格递增

字节序: 头部与内容统一 intrev32ifbe/memrevXXifbe — 小端存储 (大端平台读写转换)
```

## 基本元素分解

1. **编码分级**: 值域决定元素宽度 (2/4/8B) — 小值集 2B/元素
2. **有序数组 + 二分**: O(log n) 查找, O(n) 插入 (memmove)
3. **升级机制**: 值域超界 → 整体扩宽 + 从后往前搬 + 新值头/尾插入
4. **升而不降**: 删除不缩编码 (设计决策)
5. **字节序统一**: 小端存储, 跨平台转换
6. **完整性校验**: deep 模式查重/有序

## 标记问题 (8 个)

1. 升级只发生在"值域超界" — 新值为什么只能在前 (负数) 或后 (正数)? (L163 prepend 判断)
2. 升级从后往前搬 — 为什么不能从前往后? (L169-173 注释)
3. 编码选择阈值 (INT16±32767/INT32±2^31) — 与 listpack 编码族的对照?
4. 二分查找的首尾快速路径 (L128-134) — 收益?
5. 字节序统一小端 — 为什么? (网络/文件交换?)
6. 删除不降级 (升而不降) — 设计决策? 为什么不做降级?
7. 完整性校验 (deep: 严格递增无重复) — 防御场景?
8. 消费: t_set 何时用 intset (阈值)? intset vs listpack 对照?

## 时空溯源 (代码内痕迹)

- 2009-2012 Pieter Noordhuis (版权头) — Redis 早期组件
- 自初版基本未变 (560 行稳定); 新增: intsetValidateIntegrity (RDB 加载防御, 7.x)
- 对照: listpack 的整数编码 (7BIT/13BIT...) 是更细的粒度; intset 是固定 2/4/8 三档
