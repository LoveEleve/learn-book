# 闭环笔记 q3: 二分查找 + 首尾快速路径

## 假设
intsetSearch 是标准二分 (O(log n)) + 两个快速路径: 值 > 最大 → 返回尾部插入位; 值 < 最小 → 返回头部插入位 — 免二分。

## 验证过程
- intsetSearch (intset.c:117-156):
  - L122-135 快速路径: 空集 → pos=0; `value > _intsetGet(is,max)` → pos=length (尾部插入); `value < _intsetGet(is,0)` → pos=0 (头部插入)
  - L137-147 二分: `mid = ((unsigned int)min + (unsigned int)max) >> 1` (无符号防溢出); 比较调整 min/max
  - L149-155: 命中 → pos=mid 返回 1; 未命中 → pos=min (插入位)
- 收益: 有序数组的二分 O(log n) — 512 元素集 ≤9 次比较; 快速路径让"升级场景的后续插入"和"极值插入"免全二分
- 与 listpack 对照: listpack 线性扫描 O(n) (无索引), intset 二分 O(log n) — intset 的优势场景

## 代码类型
Algorithmic (二分) — 标准算法 + 微优化

## 跨域关联
- R-27 (SISMEMBER) → 查找面
- R-19 (listpack 线性) → 对照

## 结论
有序数组 + 二分: O(log n) 查找 + O(n) 插入 (memmove); 首尾快速路径 (极值) 免二分 — 小集合场景 (≤512) 的高效查询面。
源码位置: intset.c:117-156
