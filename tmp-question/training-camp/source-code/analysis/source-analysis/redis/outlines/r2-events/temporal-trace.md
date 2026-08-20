# R-2 事件驱动+IO 多线程 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2006 (antirez) | ae 初版 — 头注释: "Originally I wrote this code for the Jim's event-loop (Jim is a Tcl interpreter)" — 从 Tcl 解释器移植 |
| 2009 | epoll 后端 (ae_epoll.c 版权 "2009-Present") — Linux 生产后端定型 |
| 演进 | aeResizeSetSize (动态扩容, 早期固定 maxclients); AE_BARRIER 引入 (beforeSleep fsync 组批回复); 单调时钟替换 gettimeofday (getMonotonicUs, NTP 安全) |
| 6.0 | **io threads 引入** (networking.c) — 扇出扇入范式; IO_THREADS_MAX_NUM=128 |
| 6.2+ | io-threads-do-reads 可选 (默认关, config.c:3051); threads_pending CACHE_LINE_SIZE 对齐 (防伪共享) |
| 演进 | ProcessingEventsWhileBlocked 保护 (#6988 修复: 阻塞中禁用读线程) |

## 痕迹证据

- ae.c:1-3 头注释: Jim 事件循环来源 — 图书馆化
- ae.c:29-30: "Include the best multiplexing layer supported by this system. The following should be ordered by performances, descending." — 后端选择原则
- ae.c:239-244: 时间事件无序链表的权衡注释 (有序插入/跳表 = 可选优化 "not needed by Redis so far")
- ae.c:402-407: AE_BARRIER 用途注释 (fsync 前不回复)
- networking.c:4244-4245: "the main thread will never touch our list before we drop the pending count to 0" — 扇出扇入互斥不变式
- networking.c:4388-4391: "fan-out -> fan-in paradigm" 注释 (两处: 写/读)
- networking.c:4491-4495: postponeClientRead 注释 (五条件)

## 推断标注

- "epoll 后端承载 Linux 生产环境" — 代码事实 (HAVE_EPOLL 分支首位), 生产占比是推断
- "io threads 默认 1 是因为单线程执行模型" — 配置默认 1 是事实 (config.c:3149), 动机 (避免线程开销) 由 stopThreadedIOIfNeeded 的惰性停推断
- "CACHE_LINE_SIZE 对齐是 6.2+ 的性能修复" — 对齐是代码事实 (L4217-4223), 引入版本是推断
