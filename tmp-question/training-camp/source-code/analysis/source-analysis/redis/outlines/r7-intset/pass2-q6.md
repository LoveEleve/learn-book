# 闭环笔记 q6: 消费场景 — set 的 intset↔dict 转换

## 假设
set 元素"可解析为 long long"且规模 ≤512 (set-max-intset-entries) 时用 intset; 超阈值或含非整数 → dict; 转换单向。

## 验证过程
- 配置 (config.c:3216): `set-max-intset-entries` = 512 (MODIFIABLE)
- setTypeAdd (t_set.c:26): `isSdsRepresentableAsLongLong(value) == C_OK && size_hint <= server.set_max_intset_entries` → 走 intset 路径; t_set.c:42: intset 已超阈值 → 转 dict
- intset 路径 (t_set.c:69-81,109,164): intsetNew/intsetAdd (值 llval); 非整数或超限 → dict (hashtable 编码)
- 双向判定: 值类型 (整数性) + 规模 (512) — 任一不满足 → dict
- 转换触发时机: 每次 SADD 检查 (O(1)); intset→dict 单向 (dict 不缩回)
- 对照: hash 的 listpack→dict (512/64B 双条件) — 同哲学: 小规模紧凑编码 + 阈值转换

## 代码类型
Glue (编码策略) + Interface (配置)

## 跨域关联
- R-27 (t_set) → 消费域 (后续详述)
- R-19 (listpack→dict) → 同哲学对照
- R-3 (dict 目标) → 转换终点

## 结论
set 的双条件入 intset: 整数性 + ≤512 (CONFIG SET 可调); 超限单向转 dict。与 hash 的 listpack 策略同构 — "小规模紧凑、超阈值哈希"是 Redis 编码层统一哲学。
源码位置: config.c:3216; t_set.c:26,42,69-81
