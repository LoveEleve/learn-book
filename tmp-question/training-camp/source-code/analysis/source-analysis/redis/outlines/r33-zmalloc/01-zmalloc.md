# R-33 zmalloc — Redis 的内存账本: 统一分配入口 + used_memory 精确记账

> 前置: 根域 (零依赖) | 复用: [[atomicvar]] (C11 原子) | 引出: [[R-4-SDS]] (usable 消费) | 对照: [[R-23-evict]] (软上限) + [[R-8-persistence]] (CoW 报告) + [[R-18-defrag]] (no_tcache)
> 🔴 A | 8 KP | [模式: 分配器抽象+双路径记账+错误策略分层+编译器交互 hack]
> Pass 2 闭环: q1(双路径) q2(OOM) q3(原子) q4(usable) q5(编译器) q6(防线) q7(统计) q8(jemalloc)

**读者处境**: `INFO memory` 里的 used_memory 是谁记的?为什么 Redis 把 malloc/free 全部换成自己的 zmalloc?OOM 时 Redis 直接崩溃而不是返回 null — 为什么?为什么分配 10 字节的请求, SDS 的 alloc 字段却是 16?这篇拆 Redis 的内存账本: 统一入口、精确记账、崩溃策略、以及"利用分配器剩余空间"的编译器 hack。

### 1. 统一入口 — 为什么要包一层 malloc

场景: 直接 malloc 不好吗?zmalloc 包一层有什么收益?
源码路径:
- `zmalloc.h:29-79` — 分配器多路: USE_TCMALLOC/USE_JEMALLOC/__APPLE__/libc 编译期选择 — **生产默认 jemalloc**
- `zmalloc.c:54-68` — 宏重映射: `malloc→je_malloc, free→je_free` — 全部代码的 malloc 调用于编译期换血
- `zmalloc.c:70-73` — **used_memory 记账**: `atomicIncr(used_memory, n)` — 每次分配/释放同步账本
- API 面: 基础 (zmalloc/zcalloc/zrealloc/zfree/zstrdup) + try + usable + with_flags + no_tcache 五族 (zmalloc.h:91-127)
- **安全面**: `zcalloc_num` 乘法溢出检查 (zmalloc.c:249-259: `num > SIZE_MAX/size` → OOM handler) — calloc 语义的 (num×size) 防 wrap
关键设计: 统一入口 = 记账点 (q1/q3): 一切内存行为可统计 (INFO memory), 一切分配策略可替换 (tcmalloc/jemalloc 零成本切换), 崩溃策略可注入 (OOM handler); 入口集中也让溢出防护只写一处。[模式: 分配器抽象 + 记账点]
数据流: 任何组件分配 → zmalloc 族 → 底层分配器 + used_memory 原子更新。

### 2. 双路径记账 — 8 字节前缀 vs malloc_usable_size

场景: 指针前面那 8 个字节是什么?为什么 jemalloc 下不需要?
源码路径:
- `zmalloc.c:37-45` — PREFIX_SIZE: **HAVE_MALLOC_SIZE → 0** (分配器能报实际大小) / 否则 sizeof(size_t)=8
- `zmalloc.c:93-111` — usable 路径: `size = zmalloc_size(ptr)` 实时查询 → 记账=**分配器实际大小** (≥请求); 前缀路径: `*((size_t*)ptr)=size` 存请求大小 → 返回 ptr+8
- **语义差异 (harness 实证)**: 请求 10B — usable 路径记账 16 (桶大小, 分配器视角), 前缀路径记账 18 (请求 10+前缀 8, 不含真实对齐) — **两路径记账语义不同, 无绝对高低** (usable=分配器真实开销, 前缀=请求者视角)
- usable 路径的收益: 省 8B/次前缀 + 记账精确到分配器真实开销 + 为 usable 优化 (q4) 铺路
关键设计: 双路径权衡 (q1): 能力检测 (HAVE_MALLOC_SIZE) 决定记账视角 — jemalloc 下 used_memory = 分配器真实开销 (含 size class 对齐), 前缀路径 = 请求大小+前缀 (低估真实占用)。[模式: 能力检测 + 双路径]
数据流: zmalloc(n) → 有 usable_size? → 分配+查询实际大小记账 : 前缀存大小记账。

### 3. OOM 策略 — 崩溃还是返回 null

场景: 内存不够时 Redis 为什么不返回 null 让业务处理?
源码路径:
- `zmalloc.c:124-128` — zmalloc: 失败 → `zmalloc_oom_handler(size)` (默认 serverPanic 崩溃)
- `zmalloc.c:131-134` — ztry* 家族: 失败 → NULL, 调用方自决
- 装配链: `server.c:6970` zmalloc_set_oom_handler(redisOutOfMemoryHandler) → `server.c:6712-6717` serverLog+serverPanic
- try 家族消费方: rdb.c:389 (RDB 加载大对象), t_stream.c:3411 (XDEL 数组), module.c — 大块一次性分配
关键设计: 错误策略分层 (q2/q6): 正常路径分配失败 = 状态已破坏, **崩溃 (可观测) 比继续执行安全**; try 面 = "一次分配失败可放弃操作"的路径; 与 maxmemory (软上限, evict 淘汰) 构成双层防线。[模式: 崩溃式 OOM + 降级开关]
数据流: 分配失败 → 正常路径: OOM handler → serverPanic; try 路径: NULL → 调用方降级。

### 4. usable 优化 — 分配器的免费膨胀

场景: 为什么 SDS 请求 10 字节, alloc 字段却是 16?
源码路径:
- `zmalloc.c:138-147` — zmalloc_usable: 分配后把**实际可用大小** (jemalloc size class) 通过出参返回
- `sds.c:90-105` — sdsnew: `s_trymalloc_usable(hdrlen+initlen+1, &usable)` → `alloc = usable-hdrlen-1` — **SDS 初始容量 = 分配器实际大小** (免费膨胀), 后续追加不触发 realloc
- `zmalloc.h:125-146` + `zmalloc.c:84-89` — **extend_to_usable**: 假函数只传指针, 靠 alloc_size 属性告诉编译器"指针大小已扩展" — 解 _FORTIFY_SOURCE 下用 usable 剩余空间的 SIGABRT (systemd PR#25688/gcc bug 96503)
关键设计: 剩余空间利用 (q4/q5): 分配器按桶分配 (16B 桶), 请求 10B 白得 6B — SDS 直接记账为可用, 省一次 realloc; 但这是"C 语义红线" (malloc_usable_size 非设计用途), 需要编译器 hack 背书。[模式: 桶膨胀利用 + alloc_size 传导]
数据流: 请求 n → jemalloc 桶 m≥n → usable=m → SDS alloc=m → 追加到 m 免 realloc。

### 5. 原子统计与统计面 — INFO memory 的账本

场景: 单线程 Redis 为什么 used_memory 要原子?RSS 怎么读?
源码路径:
- `zmalloc.c:70-73` + `atomicvar.h:62-92` — redisAtomic: C11 → `_Atomic`, 非 C11 → 空宏
- **多线程证据**: `bio.c:192-245` — 后台线程 (RDB 保存/懒释放) 用 zmalloc 分配 job — 非主线程也在记账 → 原子必需
- `zmalloc.c:496-646` — RSS 多平台: /proc/self/stat 解析 (get_proc_stat_ll) / macOS kvm / **fallback: used_memory** (碎片率恒 1); 注释明说"not designed to be fast" — 快速面用 RedisEstimateRSS
- `childinfo.c:60-80` — **private_dirty (smaps 解析) → fork CoW 报告**: "Fork CoW: current/peak/average MB" — R-8 持久化子进程的内存开销评估 (带节流)
- `server.c:2637` — zmalloc_get_memory_size (sysconf 物理内存) → system_memory_size → INFO memory total_system_memory
- jemalloc mallctl: allocator_info (allocated/active/resident/retained/muzzy) → INFO memory (L640-1004); mallctl 不可用时的兜底: allocator_allocated = zmalloc_used (server.c:1249-1250)
关键设计: 统计分层 (q3/q7): 原子账本 (快, 每次分配) / RSS (慢, /proc) / smaps (更慢, CoW 专用, 节流) / mallctl (分配器内部视角) / sysconf (物理总量) — 每层服务不同消费方, 性能与精度取舍显式。[模式: 分层度量]
数据流: 分配 → 原子账本; INFO memory → mallctl + RSS + 账本三源合并。

### 6. jemalloc 深度绑定 — flags / no_tcache / arena

场景: defrag 为什么要求"Redis 定制版 jemalloc"?Lua VM 内存怎么单独核算?
源码路径:
- `zmalloc.c:149-213` — with_flags 族: mallocx/rallocx/dallocx (arena/tcache/零化 flags); realloc 语义三态 (size=0→free / ptr=NULL→malloc)
- `zmalloc.c:196-213` — **no_tcache 族**: `MALLOCX_TCACHE_NONE` — 绕过线程缓存直达 arena — 注释明说 "Used for online defragmentation" (R-18 唯一消费方)
- `zmalloc.h:76-78` — **HAVE_DEFRAG = USE_JEMALLOC && JEMALLOC_FRAG_HINT** — 需要 Redis 定制版 jemalloc (per-allocation 碎片提示)
- arena 级: `allocator_info_by_arena` (L680+) — Lua VM 独立 arena 核算 (RELEASENOTES #13133)
关键设计: 深度绑定 (q8): 通用抽象之外, Redis 对 jemalloc 有专属绑定面 (flags/tcache/arena) — 生产内存模型以 jemalloc 为第一公民; defrag 能力反噬编译依赖 (定制 jemalloc)。[模式: 第一公民绑定]
数据流: defrag 移动对象 → zmalloc_no_tcache (绕过 tcache) → 不干扰主线程缓存命中。

### 负面空间 — zmalloc 刻意不做的事

- **不做垃圾回收**: 只记账不回收, 内存释放全靠 zfree 对称调用 (对照 JVM GC)
- **不做内存池**: 不预分配池化复用 (分配直接走系统分配器) — jemalloc 内部已有 tcache/arena
- **不做对象大小分级**: 不按对象类型分级管理 (记账面纯字节)
- **不做内存压缩**: 压缩/去重是上层数据结构的事 (SDS/quicklist), zmalloc 不管
- **不兜底所有 OOM**: try 家族只覆盖特定路径, 其余崩溃 (对照: 某些系统返回 null 重试)

→ 引出: SDS 是 zmalloc 的第一个大消费者 — 预分配策略 + usable 利用 → [[R-4-SDS]]
