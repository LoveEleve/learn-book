# 闭环笔记 q1: 三编码 — 创建与选择

## 假设
set 三编码: INTSET (整数+≤512) / LISTPACK (≤128/64B) / HT (大/混合); 创建按 size_hint 预判。

## 验证过程
- setTypeCreate (t_set.c:25-36):
  - **INT 且 ≤512** → createIntsetObject (L26-27)
  - **≤128** → createSetListpackObject (L28-29)
  - 否则 → createSetObject + **dictExpand 预扩** (L33-35)
- setTypeMaybeConvert (L40-46): size_hint 超限预转换 (listpack>128 / intset>512 → HT + dictExpand)
- 阈值 (config.c:3216-3218): set-max-intset-entries 默认 512 / **set-max-listpack-entries 默认 128** (≠hash 512!) / set-max-listpack-value 默认 64
- intsetMaxEntries (L49-54): **1<<30 上限** (intset 内部限制, L51 注释)
- setTypeConvert (L473) / setTypeConvertAndExpand (L481, cap+panic 参数)

## 代码类型
Mechanism (编码选择)

## 跨域关联
- R-7 (intset) / R-19 (listpack) / R-3 (dict+storedKey) / R-25 (hash 三态对照)

## 结论
set = **三编码四分支** (含 1<<30 intset 上限): intset 专精整数, listpack 兜小字符串, HT 大集合。**listpack 阈值 128 ≠ hash 512** — set 元素无值, 更省空间所以阈值不同 (推断: set 元素只有键, listpack 密度高)。
源码位置: t_set.c:25-54; config.c:3216-3218
