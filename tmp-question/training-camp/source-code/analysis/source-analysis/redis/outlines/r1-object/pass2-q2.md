# 闭环笔记 q2: EMBSTR 44B — 64 字节 arena 的数学

## 假设
EMBSTR 让 robj 与 sds 同 chunk 分配 (一次 malloc, 零二次分配 + 缓存友好); 44B 上限是"塞进 jemalloc 64B arena"的精确计算。

## 验证过程
- createEmbeddedStringObject (object.c:71-93): `zmalloc(sizeof(robj)+sizeof(struct sdshdr8)+len+1)` — **robj+sds头+buf 一次分配**; o->ptr = sh+1 (sds buf)
- 上限注释 (L99-100): "The current limit of 44 is chosen so that the biggest string object we allocate as EMBSTR will still fit into the 64 byte arena of jemalloc"
- 数学验证: robj 16B + sdshdr8 3B + 44B 内容 + 1B NUL = **64B** — 恰好 jemalloc 64B 桶
- 收益: 一次分配 (省 malloc 调用) + **对象与数据同缓存行** (缓存友好 — 访问 robj 即带出内容)
- 代价: EMBSTR 不可变 (无 avail 空间, sds 无预分配 — 追加要转 RAW)
- 触发: createStringObject (L102-107) ≤44 → EMBSTR; tryObjectEncoding RAW→EMBSTR (小字符串)
- SDS_NOINIT 支持 (L84-85): 免清零

## 代码类型
Algorithmic (分配优化) — 缓存友好设计

## 跨域关联
- R-4 (sds 头) / R-33 (zmalloc/jemalloc arena) → 基础
- R-6 (zset 小规模) → 同哲学 (紧凑分配)
- 生产面: 大量小字符串的缓存命中

## 结论
EMBSTR = 同 chunk 分配: 16+3+44+1=64B 恰好 jemalloc 64B 桶 — 一次 malloc + 同缓存行。44 是"arena 数学"的精确解; 不可变 (无预分配) 是代价 (追加即升级 RAW)。
源码位置: object.c:71-107
