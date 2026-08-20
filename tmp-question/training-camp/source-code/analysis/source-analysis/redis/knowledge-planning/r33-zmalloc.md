# R-33 zmalloc — 知识规划 (knowledge-planning)

> 项目: Redis 7.4.2 | 🔴 A / 1 篇 (+harness) | zmalloc.c (1056)+zmalloc.h (160)+atomicvar.h+server.c 装配点
> 基线: REDIS-PLAN R-33 — 前置: **无 (零依赖, 拓扑第 1)** — 展开 分配器多路→PREFIX 双路径→OOM 策略→usable 优化→统计面→jemalloc 绑定

---

## §0.8

- 🔴 A，1篇 — 分配器多路(**zmalloc.h:29-79: tcmalloc/jemalloc/macOS/libc 编译期选择; zmalloc.c:54-68 宏重映射 malloc→je_malloc**) → 双路径记账(**PREFIX_SIZE: HAVE_MALLOC_SIZE→0 前缀 (usable_size 实时查询, 记账=分配器实际大小) / 否则 8 字节前缀存大小 (记账=请求大小) L37-45,93-111**) → OOM 分层(**zmalloc 失败→zmalloc_oom_handler→main 装配 redisOutOfMemoryHandler→serverPanic (server.c:6970,6712-6717); ztry* 失败→NULL (rdb.c:389 加载/ t_stream:3411 / module) — 软 (maxmemory evict) 硬 (OOM 崩溃) 双层**) → usable 优化(**zmalloc_usable 返回 jemalloc 实际大小; SDS sdsnew 初始 alloc=usable 免费膨胀 sds.c:90-105; extend_to_usable 骗编译器 alloc_size 解 _FORTIFY_SOURCE SIGABRT zmalloc.h:110-124**) → 原子统计(**used_memory redisAtomic (C11=_Atomic), bio.c:192 后台线程分配实证**) → 统计面(**zmalloc_get_rss /proc/self/stat 多平台+fallback used_memory; private_dirty smaps 解析→childinfo.c:70 fork CoW 报告 (R-8); jemalloc mallctl allocator_info (INFO memory) L640-1004**) → jemalloc 绑定(**with_flags (mallocx) L149-213 / no_tcache MALLOCX_TCACHE_NONE (R-18 defrag 专用, HAVE_DEFRAG=USE_JEMALLOC+FRAG_HINT) / arena 级 (Lua VM lua_arena) / jemalloc_purge/bg_thread**)
- 设计模式: [模式: 分配器抽象+双路径记账+错误策略分层+编译器交互 hack]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| zmalloc.h:29-79 | 分配器多路 | 编译期 tcmalloc/jemalloc/macOS/libc 选择; HAVE_MALLOC_SIZE 能力检测 | High |
| zmalloc.c:37-45,93-111 | 双路径 | PREFIX_SIZE: usable→0 前缀/记账实际大小; 无→8B 前缀存请求大小 | High |
| zmalloc.c:75-82,124-134; server.c:6970,6712-6717 | OOM | zmalloc 崩溃 vs ztry 降级; handler 可替换; maxmemory 软上限对照 | High |
| zmalloc.c:138-147; sds.c:90-105 | usable | 分配器剩余空间利用: SDS 初始 alloc=usable; 免费预分配 | High |
| zmalloc.h:125-146; zmalloc.c:84-89 | 编译器 | extend_to_usable: alloc_size 属性传导, 解 _FORTIFY_SOURCE SIGABRT | High |
| zmalloc.c:70-73; atomicvar.h:62-92; bio.c:192 | 原子性 | used_memory 原子 (C11 _Atomic); 后台线程分配实证 | High |
| zmalloc.c:496-646,700+; childinfo.c:60-80 | 统计面 | RSS 多平台/fallback; smaps private_dirty→fork CoW 报告; mallctl INFO | High |
| zmalloc.c:149-213,640-1004; zmalloc.h:76-78 | jemalloc 绑定 | with_flags/no_tcache (defrag)/arena 级 (Lua)/FRAG_HINT 定制 | High |

---

## 02-04 聚合+分类+聚类 (1篇+harness)

**1篇理由**: zmalloc 是单机制闭环 (抽象→记账→错误→优化→统计→绑定), 1篇 (~70行) 按"多路抽象→双路径记账→OOM 分层→usable 优化→原子统计→统计面→jemalloc 绑定"展开; harness 验证记账双路径与 usable 语义。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 双路径记账 (PREFIX_SIZE/usable 语义) | 🔴 | **为什么🔴**: 核心机制 |
| P1-2 | OOM 分层 (zmalloc vs ztry + handler 链) | 🔴 | **为什么🔴**: 错误策略 |
| P1-3 | usable 优化 + extend_to_usable (SDS 联动) | 🔴 | **为什么🔴**: 性能核心 |
| P2-1 | used_memory 原子统计 (多线程例外) | 🟡 | **为什么🟡**: 并发正确性 |
| P2-2 | 统计面 (RSS/smaps/mallctl + CoW 报告) | 🟡 | **为什么🟡**: 可观测性 |
| P3-1 | jemalloc 绑定 (with_flags/no_tcache/arena) | 🟢 | **为什么🟢**: 深度绑定面 (defrag/Lua 连接) |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **记账与错误** | 🔴 | 核心机制 |
| B | **usable 优化** | 🔴 | 性能 |
| C | **统计与绑定** | 🟡 | 可观测/扩展 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 双路径记账 | HAVE_MALLOC_SIZE → 0 前缀 (usable_size 实时查询, 记账=分配器实际大小); 无 → 8B 前缀存请求大小 — 记账语义不同 (harness 实证: 10B 请求 usable 记 16/前缀记 18) | zmalloc.h:29-45; zmalloc.c:93-111 |
| q2 | OOM 分层 | 默认 zmalloc 失败→serverPanic 崩溃 (handler 可替换, main 装配 redisOutOfMemoryHandler); ztry 家族=降级开关 (RDB 加载/流批量/module); maxmemory 软上限 vs OOM 硬崩溃双层 | zmalloc.c:124-134; server.c:6970,6712-6717; rdb.c:389 |
| q3 | 原子统计 | used_memory 必须原子 (C11 _Atomic), 因为 bio 后台线程 (bio.c:192)/io threads 并发分配 — "单线程 Redis" 的例外 | zmalloc.c:70-73; atomicvar.h:62-92 |
| q4 | usable 优化 | zmalloc_usable 返回分配器实际大小 (size class 膨胀), SDS sdsnew 初始 alloc=usable — 免费预分配, 后续追加免 realloc | zmalloc.c:138-147; sds.c:90-105 |
| q5 | 编译器 hack | extend_to_usable = alloc_size 属性传导空洞, 解 _FORTIFY_SOURCE 下使用 malloc_usable_size 剩余空间的 SIGABRT (systemd PR#25688/gcc bug 96503) | zmalloc.h:125-146; zmalloc.c:84-89 |
| q6 | 防线编排 | 软: maxmemory evict (淘汰) → 硬: OOM serverPanic (崩溃) — used_memory 共同度量; try 家族是崩溃前最后降级 | evict.c:355; networking.c:2002 |
| q7 | 统计面 | RSS (/proc 慢速) vs RedisEstimateRSS (快估); private_dirty (smaps)→fork CoW 报告 (childinfo.c:70, 节流); mallctl allocator_info→INFO memory | zmalloc.c:496-646; childinfo.c:60-80 |
| q8 | jemalloc 绑定 | with_flags (mallocx) / no_tcache (MALLOCX_TCACHE_NONE, defrag 专用, HAVE_DEFRAG=jemalloc 定制 FRAG_HINT) / arena 级 (Lua VM) | zmalloc.c:149-213; zmalloc.h:76-78 |

→ 引出 R-4: SDS 的 usable 消费 + 预分配策略建立在 zmalloc 之上 → [[R-4-SDS]]
