# R-2 事件驱动+IO 多线程 — completeness-questions

## 开发者视角

1. aeCreateFileEvent 注册一个 fd 要传什么?mask 怎么合并?
2. aeProcessEvents 的三种等待模式分别是什么?
3. 为什么读事件总在写事件之前触发?
4. AE_BARRIER 是什么?什么时候用?
5. 时间事件怎么注册?周期事件怎么重排?
6. io threads 最多几个?默认几个?
7. 写扇出扇入的完整时序?
8. postponeClientRead 的五个条件是什么?

## 架构师视角

9. fd 直接索引数组 vs 哈希表 — 为什么 O(1) 且无冲突?
10. 无序链表定时器 vs 红黑树/跳表 — 权衡是什么 (注释怎么说的)?
11. refcount 和 maxId 双重防重入 — 各防什么场景?
12. 编译期特化后端 vs 函数指针表 — 为什么选前者?
13. 互斥锁当启停信号的设计 — 自旋和锁怎么配合?
14. 从库客户端为什么必须主线程写?
15. 惰性停 (pending < num×2) 的依据?
16. "IO 可并行, 执行仍单线程" — 这个不变式怎么保证?

## 学生视角

17. serverCron 注册后, 一次完整的事件循环迭代里它怎么被调用?
18. epoll 的 EPOLLERR/HUP 为什么映射成读写双触发?
19. 一次写扇出扇入: 从 pending_write 到装回写处理器?
20. 读线程化时, 客户端数据什么时候真正被解析?
21. aeMain 和 aeProcessEvents 的关系?
