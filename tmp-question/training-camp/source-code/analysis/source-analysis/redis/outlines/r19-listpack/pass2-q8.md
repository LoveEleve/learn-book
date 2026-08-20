# 闭环笔记 q8: 消费场景 — hash/zset/stream 的阈值转换

## 假设
hash/zset 小规模用 listpack (紧凑), 超阈值转 dict; stream 用 listpack 存消息 — 阈值配置 hash-max-listpack-entries=512/values=64B, zset=128/64B。

## 验证过程
- 配置 (config.c:3215-3223): `hash-max-listpack-entries`=512 (别名 hash-max-ziplist-entries, 兼容旧名) / `hash-max-listpack-value`=64B / `zset-max-listpack-entries`=128 / `zset-max-listpack-value`=64B
- hash 转换 (t_hash.c:605,893,932): `new_fields > server.hash_max_listpack_entries` → hashTypeConvert (listpack→dict); 值超 64B → 同样转换 (t_hash.c:487 注释)
- hash 的 listpack 布局: field/value 成对连续 (t_hash.c:887-888 lpAppend field, lpAppend value) — **2N 个 entry**
- zset: ele + score 成对 (t_zset.c:1101-1108: ele 字符串 + score 整数/字符串) — score 常编码为整数 (q3 嗅探)
- stream: 消息字段用 listpack (t_stream.c) — stream 结构里 listpack 是核心 (R-10 详述)
- 转换触发: 插入时检查长度 (每次写 O(1) 判断); 转换后不再回退 (单向)
- 为什么 512/128: 线性查找 O(N) 在小 N 可接受, 紧凑内存收益大; 超阈值查找变慢 → 转 dict O(1)

## 代码类型
Glue (编码选择策略) + Interface (配置面)

## 跨域关联
- R-25 (t_hash) / R-6 (t_zset) / R-10 (t_stream) → 消费域 (后续详述)
- R-3 (dict) → 转换目标
- 配置面: MODIFIABLE_CONFIG (CONFIG SET 动态)

## 结论
listpack 是"小规模默认编码": hash ≤512 字段/值 ≤64B, zset ≤128 — 紧凑省内存 (每元素 2-11B); 超阈值单向转 dict (O(1) 查找)。阈值可 CONFIG SET 动态调 (生产调优面)。
源码位置: config.c:3215-3223; t_hash.c:487,605,893,932; t_zset.c:1101-1108
