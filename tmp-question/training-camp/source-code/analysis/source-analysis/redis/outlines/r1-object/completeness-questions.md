# R-1 redisObject — completeness-questions

## 开发者视角

1. robj 的 16 字节是什么?type/encoding 为什么各 4bit?
2. SET hello → 对象是 EMBSTR 还是 RAW?为什么?
3. SET count 5 → 这个 5 占多少内存?共享了吗?
4. OBJECT ENCODING 怎么查?什么值会看到?
5. 什么时候 int 编码会变成 raw?反过来会吗?
6. incrRefCount/decrRefCount 对共享对象做什么?
7. OBJECT REFCOUNT 返回什么?什么时候 >1?
8. 字符串 "12345" 和 "hello" 的编码路径有什么不同?

## 架构师视角

9. EMBSTR 的 44B 是"arena 数学" — 64B 桶怎么算出来的?为什么不再大?
10. INT 编码零分配 — 指针槽复用值的边界?LONG_MAX 之外呢?
11. maxmemory 下为什么禁用共享整数?LRU 与共享的矛盾?
12. tryObjectEncoding 的优化顺序 (INT→共享→EMBSTR) — 为什么这样排?
13. 引用计数三态 (1/特殊值) — 共享对象免计数的线程安全依据?
14. 共享池 10000 整数的选择?响应串共享的收益模型?
15. lru 24bit 双用途 (LRU/LFU) — 位分配的依据?
16. 解码按需 (INT 每次 ll2string) — 为什么不缓存?

## 学生视角

17. SET hello world 之后, 内存里到底有几块分配?
18. 16+3+44+1=64 — 每个数字代表什么?
19. GET count (INT 编码) 的完整流程: 解码→回复?
20. 共享对象和普通对象的 refcount 值有什么区别?
