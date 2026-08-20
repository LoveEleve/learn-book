# 闭环笔记 q1: 升级机制 — 值域超界时的"头尾特权"

## 假设
升级只发生在"新值超出当前编码值域" — 此时新值必然大于所有现有值 (正) 或小于所有 (负) → 只能插头/尾; 升级搬移从后往前防覆盖。

## 验证过程
- intsetAdd (intset.c:214-216): `if (valenc > is->encoding) return intsetUpgradeAndAdd(is,value)` — 注释 L211-213: "this value should be either appended (if > 0) or prepended (if < 0), because it lies outside the range of existing values"
- intsetUpgradeAndAdd (L159-182):
  - L163: `prepend = value < 0 ? 1 : 0` — 负数 → 头插; 正数 → 尾插
  - L166-167: 先改 encoding + resize (+1 元素)
  - L172-173: `while(length--) _intsetSet(is, length+prepend, _intsetGetEncoded(is,length,curenc))` — **从后往前搬** (旧值读 curenc, 写 newenc 偏移): 最后一个元素搬到 (len-1+prepend), 逐步往前 — 不会覆盖未读的旧值
  - L176-179: 新值写头 (prepend) 或尾
  - L180: length+1
- 为什么从后往前: 新宽度下每个元素占更多字节, 从前往后搬会覆盖后面的旧值 (旧值还没读); 从后往前先搬最后元素, 前面未搬区域不动 ✅
- 升级不涉及二分: 头/尾直接判定 — O(n) 搬移 + O(1) 定位

## 代码类型
Algorithmic (升级搬移) — 核心设计

## 跨域关联
- R-27 (t_set) → 消费方
- R-19 (listpack 编码族) → 对照: intset 三档粗粒度 vs listpack 六档细粒度

## 结论
升级 = "值域超界的特权插入": 新值必在极值 (头/尾), 免二分; 搬移从后往前 (新宽度下防覆盖); 一次性 O(n) 换后续所有元素的新宽度。升格是单调的 (值域只会变大)。
源码位置: intset.c:159-182,206-216
