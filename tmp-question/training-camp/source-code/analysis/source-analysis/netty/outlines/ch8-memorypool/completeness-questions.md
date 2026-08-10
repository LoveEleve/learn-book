# Ch8 MemoryPool 全视角验证

## 开发者视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | handle 64 位编码 5 个字段怎么压缩? 为什么不用对象包装? | 5.2 §1 |
| 2 | Buddy 二叉树的 allocateRun 怎么找最小满足的兄弟节点? 完全满足 vs 部分满足怎么区分? | 5.2 §2 |
| 3 | Subpage 的 getNextAvail() 用位运算怎么找第一个空闲 bit? 满 bitmap 后怎么处理? | 5.3 §1 |
| 4 | AbstractPooledDerived.deallocate 为什么先 recycle 自己再 release parent? 反过来会有什么 bug? | 5.4 §3 |
| 5 | PoolThreadCache.allocate 命中 vs 未命中 — 分别走什么路径? 缓存 miss 的开销有多大? | 5.3 §4 |

## 性能工程师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | Arena 数量公式 `2*cores` — 为什么不是 `cores` 或 `4*cores`? #3888 解决的竞争是什么? | 5.1 §2 |
| 7 | MPSC 无锁队列的 trim — 8192 次分配触发, 什么条件下释放哪些条目? | 5.3 §5 |
| 8 | q050 是 sweet spot — 为什么分配从 q050 开始而不是 q000? | 5.1 §7 |

## 架构师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 双归还(arena.free + recyclerHandle.unguardedRecycle) — 为什么内存和对象要分离回收? | 5.4 §1 |
| 10 | AdaptivePooling vs PooledByteBufAllocator — 两个分配器并存, 怎么选择? | 5.4 §6 |
| 11 | SizeClasses 预计算 vs 运行时计算 — 为什么要把规格表写死? | 5.3 §6 |

## 学生/新人视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 12 | PooledHeapByteBuf 和 UnpooledHeapByteBuf 的 deallocate 有什么区别? | 5.4 §2 |
| 13 | 池化的 ByteBuf 不 release 会怎样? 有什么工具可以检测泄漏? | 5.4 §5 |

## 覆盖统计

| 角色 | 问题数 | 覆盖 |
|------|:--:|:--:|
| 开发者 | 5 | 5.2§1-2, 5.3§1,4, 5.4§3 |
| 性能工程师 | 3 | 5.1§2,7, 5.3§5 |
| 架构师 | 3 | 5.4§1,6, 5.3§6 |
| 学生 | 2 | 5.4§2,5 |
| **合计** | **13** | **100%** |
