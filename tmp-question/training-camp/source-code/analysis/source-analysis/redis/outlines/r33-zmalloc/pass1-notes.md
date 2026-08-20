# R-33 zmalloc — Pass 1 探索笔记

> 域: R-33 zmalloc (内存分配层) | 🔴 方案 A | 2026-08-13
> 源码: src/zmalloc.c (1056) + zmalloc.h (160) | Redis 7.4.2
> 已读测试: zmalloc_test (zmalloc.c:1010-1056, REDIS_TEST 内嵌)

## 继承树/调用图

```
分配器多路选择 (zmalloc.h:29-79):
  USE_TCMALLOC → tc_malloc + tc_malloc_size (HAVE_MALLOC_SIZE=1)
  USE_JEMALLOC → je_malloc + je_malloc_usable_size (HAVE_MALLOC_SIZE=1) [生产默认]
  __APPLE__    → malloc_size (HAVE_MALLOC_SIZE=1)
  否则 libc     → malloc_usable_size (glibc/FreeBSD...) 或 NO_MALLOC_SIZE

宏重映射 (zmalloc.c:54-68): malloc→je_malloc, free→je_free 等 (编译期覆盖)

API 面 (zmalloc.h:91-127):
  基础: zmalloc/zcalloc/zrealloc/zfree + zstrdup
  try 家族: ztrymalloc/ztrycalloc/ztryrealloc (OOM→NULL 不抛)
  usable 家族: *_usable (返回 jemalloc 实际可用大小)
  with_flags 家族 (jemalloc only): mallocx/rallocx/dallocx flags
  no_tcache (HAVE_DEFRAG): 绕过 tcache 直接 arena (defrag R-18 用)
  统计: zmalloc_used_memory / zmalloc_get_rss / zmalloc_get_allocator_info
        / get_private_dirty (smaps) / get_memory_size
  OOM: zmalloc_set_oom_handler / zmalloc_default_oom

装配 (server.c):
  main → zmalloc_set_oom_handler(redisOutOfMemoryHandler) L6970 → serverPanic L6715
  serverCron → stat_peak_memory (zmalloc_used_memory) L1215
  INFO memory → zmalloc_get_allocator_info L1228 / arena L1236
```

## 基本元素分解

1. **统一入口**: 所有内存分配走 zmalloc 族 — used_memory 原子统计 (redisAtomic, 多线程安全)
2. **PREFIX_SIZE 双路径**: HAVE_MALLOC_SIZE → 0 前缀 (malloc_usable_size 实时查询); 否则 8 字节前缀存大小 (L37-45)
3. **OOM 分层**: zmalloc (OOM→handler→serverPanic 崩溃) vs ztrymalloc (OOM→NULL, 调用方处理)
4. **usable size 优化**: jemalloc 实际可用 > 请求大小 — *_usable 家族返回, 消费方利用剩余空间
5. **编译期分配器替换**: 宏覆盖 malloc/free — 运行时零开销切换 tcmalloc/jemalloc/libc
6. **统计面**: rss (/proc/self/stat 多平台) / allocator_info (mallctl: allocated/active/resident/retained/muzzy) / private_dirty (smaps 解析)
7. **编译器交互**: extend_to_usable (gcc alloc_size 骗 _FORTIFY_SOURCE) + malloc/noinline 属性

## 标记问题 (8 个)

1. PREFIX_SIZE 双路径为什么存在? HAVE_MALLOC_SIZE 时为什么能省前缀? (malloc_usable_size 语义)
2. try 家族 vs 非 try 家族 — 哪些路径用 try? (maxmemory 检查/evict 联动)
3. 单线程 Redis 为什么 used_memory 要原子? (io threads/bio 多线程分配)
4. usable size 谁消费? 怎么用? (SDS 扩容/networking 缓冲 — 跨域)
5. extend_to_usable 的编译器骗术 — _FORTIFY_SOURCE SIGABRT 问题?
6. OOM handler 链: 默认 handler → serverPanic; maxmemory (软上限, evict) vs OOM (硬崩溃) 的关系
7. zmalloc_get_rss 多平台实现 + private_dirty (fork COW 评估 — R-8 持久化依赖)
8. jemalloc 特有面: with_flags (mallocx) / no_tcache (MALLOCX_TCACHE_NONE, R-18 defrag 依赖) / allocator_info (INFO memory)

## 测试要点 (已读 1 个)

- zmalloc_test (L1010-1056): 初始 0 → 分配 123 → realloc 456 → calloc → free → 0 字节分配 → 结束 0 — used_memory 记账正确性 (差值断言)
- 无集成测试文件 (tests/unit 无 zmalloc 专项)

## 时空溯源 (仓库单提交快照, 无 git 历史)

- 版权 2009-Present (antirez 2009 初版, Redis 最早组件之一)
- 演进痕迹 (代码内): PREFIX_SIZE 保留旧路径 (无 malloc_usable_size 平台兜底) → try 家族 (7.x maxmemory 重构引入) → with_flags/no_tcache (6.x defrag 引入, R-18) → extend_to_usable (_FORTIFY_SOURCE 修复, gcc-12 时代) → muzzy 统计 (7.0, RELEASENOTES #12996)
