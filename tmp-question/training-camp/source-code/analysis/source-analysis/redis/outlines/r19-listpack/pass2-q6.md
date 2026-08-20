# 闭环笔记 q6: 批量接口 — 单次 realloc 构建 hash

## 假设
lpBatchInsert/lpBatchAppend 一次调用插入多元素: 单次 realloc + 单次 memmove (逐元素插入是 N 次 realloc) — 小 hash 构建场景的关键优化。

## 验证过程
- lpBatchInsert (listpack.c:993+): `addedlen` 累计所有编码长度 (L1008); 局部数组 tmp[3] 栈上, 超 3 元素才堆分配 (L1009-1015); 单次 realloc + memmove (L1040+)
- lpBatchAppend (L1055+): 追加批量
- 消费场景 (t_hash.c): hash 从 listpack 转 dict 时/构建时批量写入? 看调用 — t_hash 的 hashTypeSet 逐元素 lpAppend? 批量主要用于加载/转换 (hashTypeConvert)
- 与逐元素对比: N 次 lpInsert = N 次 realloc + N 次 memmove (O(N²) 复制量); 批量 = 1 次 realloc + 1 次 memmove (O(N))
- 编码准备: 每个元素独立 lpEncodeGetType (整数嗅探) + backlen — 批量时全部预编码后统一搬移

## 代码类型
Algorithmic (批量摊还) — 构建优化

## 跨域关联
- R-25 (t_hash 转换/复制) → 主消费
- R-10 (t_stream 消息构建) → 批量追加
- R-33 (zmalloc 堆分配当超栈缓冲) → 分配面

## 结论
批量接口 = 摊还构建: 预编码 N 元素 → 单次 realloc+memmove — 小集合构建从 O(N²) 复制降到 O(N)。栈上 3 元素缓冲避免小批量堆分配。
源码位置: listpack.c:993-1080
