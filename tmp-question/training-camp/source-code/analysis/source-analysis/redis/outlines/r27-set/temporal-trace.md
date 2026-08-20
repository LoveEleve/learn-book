# R-27 集合命令 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | t_set.c 初版 — SADD/SREM 家族 (版权 "2009-Present"); intset+HT 双编码 |
| 2.8+ | SPOP/SRANDMEMBER COUNT 扩展; 集合运算优化 (sinter 最小集 qsort) |
| 6.0+ | SINTERCARD (基数版); SMISMEMBER |
| 7.x | **listpack 编码加入** (set-max-listpack-entries=128, config.c:3217); **HT→intset 降级** (maybeConvertToIntset L66-88, sinterstore L1392); dictFindPositionForInsert 预定位插入; intset 1<<30 上限 |

## 痕迹证据

- L31-32: "We may oversize the set by using the hint if the hint is not accurate, but we will assume this is acceptable to maximize performance" — size_hint 预扩注释
- L51: "limit to 1G entries due to intset internals" — 1<<30 上限注释
- L146-147: "TODO: Create and use lpFindInteger; don't go via string" — intset→listpack 转换的 TODO (整数直插优化)
- L186-187: "In the 'safe to add' check above we assumed all elements in the intset are of size maxelelen. This is an upper bound." — lpSafeToAdd 上界假设
- L198-199: "The set *was* an intset and this value is not integer encodable, so dictAdd should always work" — 转换正确性注释
- L1392: maybeConvertToIntset 调用点 (sinterstore 结果) — 降级设计实证

## 推断标注

- "listpack 阈值 128 ≠ hash 512 因 set 元素无值密度高" — 128/512 数值是事实, 动机是推断 (无注释)
- "双向转换动机 = intset 内存密度" — maybeConvertToIntset 存在是事实, 动机由 intset 4/8B/元素特性推断
- "sinterstore 降级是低频场景" — 转换调用点 (仅结果集) 是事实, "低频"是推断
