# 闭环笔记 q5: 完整性校验 — RDB 加载的畸形数据防御

## 假设
intsetValidateIntegrity 校验: 头部可读/encoding 合法/大小一致 (count×宽度 == blob)/非空; deep 模式逐元素验证严格递增无重复。

## 验证过程
- intsetValidateIntegrity (intset.c:302-343):
  - L305-306: `size < sizeof(*is)` → 0 (头部可读)
  - L308-319: encoding 必须是 2/4/8 (非法 → 0)
  - L321-324: `sizeof(*is) + count*record_size != size` → 0 (**大小必须精确匹配** — 防伪造 length 字段)
  - L326-328: count==0 → 0 (空集不合法? 注意: 这是校验的严格性 — RDB 里的 intset 不应为空)
  - L330-331: 非 deep → 通过 (只查结构)
  - L333-340: deep: 逐元素 `cur <= prev → 0` (**严格递增, 无重复**)
- 防御场景: RDB 加载 (损坏/伪造) + DEBUG RELOAD; 越界防御 (length 超大 → size 检查拦截)
- 对照: listpack 的 lpValidateIntegrity (同族防御, R-19 q7)

## 代码类型
Algorithmic (完整性校验) — 防御性

## 跨域关联
- R-8 (RDB 加载) → 主场景
- R-19 (lpValidateIntegrity) → 同族
- 安全面: 畸形数据防御

## 结论
双级校验: 浅层 (头部/encoding/大小一致) + deep (严格递增无重复) — 防 RDB/DEBUG 加载的畸形 intset 引发越界读或逻辑错误。count==0 判非法是"加载路径不产生空 intset"的约定。
源码位置: intset.c:302-343
