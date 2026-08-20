# 闭环笔记 q1: 16 字节外壳 — 位域布局

## 假设
robj 是 16 字节的紧凑外壳: type:4 + encoding:4 + lru:24 + refcount:32 + ptr — 全部 Redis 值的统一头, 位域压缩到 64 位对齐。

## 验证过程
- server.h:903-911: `struct redisObject { unsigned type:4; unsigned encoding:4; unsigned lru:LRU_BITS; int refcount; void *ptr; };`
- LRU_BITS=24 (server.h:896) — lru 字段双用途: LRU 时钟 (24bit 分钟级) 或 LFU (8bit 频率 + 16bit 访问时间)
- 布局: 4+4+24 = 32bit (lru 与 type/encoding 共用 8 字节字) + refcount 4B + ptr 8B = **16 字节**
- 为什么位域: 每对象省 4 字节 (type/encoding 各自 1 字节 → 共享 1 字节) — 百万对象省 MB 级
- OBJ_STATIC_REFCOUNT (L901): INT_MAX-1 — 栈上对象标记 (不可 incr/decr)

## 代码类型
Interface (对象契约) — 布局设计

## 跨域关联
- R-20 (键空间全部值) → 载体
- R-33 (zmalloc) → 外壳分配
- R-23 (LRU/LFU 淘汰) → lru 字段消费者

## 结论
16B 外壳 = 类型/编码/lru/引用/指针五位一体: 位域压缩 (type+encoding 共享 1B), lru 24bit 双用途 (LRU/LFU)。这是"一切皆对象"的 Redis 根基 — 每个值都背着这 16 字节。
源码位置: server.h:896,901-911
