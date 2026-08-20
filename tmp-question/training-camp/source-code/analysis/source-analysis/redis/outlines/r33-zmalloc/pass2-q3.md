# 闭环笔记 q3: used_memory 原子统计 — 单线程 Redis 的例外

## 假设
Redis 主线程单线程执行命令, 但 used_memory 用原子操作 — 因为存在其他分配线程 (bio/io threads/fork 场景)。

## 验证过程
- zmalloc.c:70-73: `#define update_zmalloc_stat_alloc(n) atomicIncr(used_memory,(n))` + `static redisAtomic size_t used_memory`
- atomicvar.h:62-63/91-92: `redisAtomic` — 非 C11 平台为空宏, **C11 平台 = _Atomic** (编译期检测)
- 多线程分配证据:
  - bio.c:192-245: bio_job 分配用 zmalloc — **后台线程 (RDB 保存/关闭 fd/懒释放) 在分配**
  - networking.c:1499,1794: io_threads_op 检查 — IO 线程 (R-2) 解析阶段有并发
  - R-18 defrag (defrag.c) 在 activeDefragCycle 中分配 — 主线程, 但 doc 线程 (jemalloc bg thread: set_jemalloc_bg_thread) 也统计
- 非原子后果: bio 线程 free 与主线程 alloc 竞争 → used_memory 错乱 → INFO memory/evict 判断 (networking.c:2002 `zmalloc_used_memory() < server.maxmemory`) 全错

## 代码类型
Implementation (并发正确性)

## 跨域关联
- R-8 (bio.c 后台线程) → 原子性第一需求方
- R-2 (io threads) → 并发面
- R-23 (networking.c:2002 maxmemory 比较) → 统计精度依赖方

## 结论
"单线程 Redis" 的例外: used_memory 必须原子, 因为 bio 后台线程/io threads/defrag 与主线程并发分配释放。atomicvar.h 在非 C11 降级为空宏 (纯单线程平台) — 能力检测式兼容。
源码位置: zmalloc.c:70-73; atomicvar.h:62-92; bio.c:192-245
