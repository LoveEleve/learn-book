# 闭环笔记 q6: OOM 链与 maxmemory 关系 — 软上限 vs 硬崩溃

## 假设
Redis 有两层内存防线: maxmemory (配置软上限, evict 淘汰兜底) 与 zmalloc OOM (硬崩溃); 两者通过 used_memory 关联。

## 验证过程
- OOM 链: zmalloc 失败 → zmalloc_oom_handler (zmalloc.c:82 默认) → main 替换为 redisOutOfMemoryHandler (server.c:6970) → serverLog+serverPanic (server.c:6712-6717) — **OOM = 进程崩溃**
- maxmemory 链 (软): evict.c:355-360 "Get the memory status from the point of view of the maxmemory directive" — checkForMemoryEviction (R-23); 触发面: processCommand 前检查 used_memory > maxmemory → freeMemoryIfNeeded
- 联动: networking.c:2002 `zmalloc_used_memory() < server.maxmemory` — 输出缓冲写回也参考 maxmemory
- **为什么还要硬崩溃**: maxmemory 淘汰只能救"可淘汰的数据" (volatile 键/可驱逐); 数据不可淘汰 + 内存耗尽 → 崩溃比数据损坏安全
- 边界: try 家族 (q2) 让特定路径在 OOM 前先降级 (RDB 加载/流批量), 但正常命令路径仍崩溃

## 代码类型
Glue (防线编排) + Implementation (handler 契约)

## 跨域关联
- R-23 (evict.c freeMemoryIfNeeded) → 软防线
- R-28 (networking.c:2002) → 输出缓冲与 maxmemory 联动
- R-8 (RDB 加载 try 降级) → 硬崩溃前的最后退路

## 结论
双层防线: maxmemory (软, 淘汰) → zmalloc OOM (硬, 崩溃)。OOM handler 单例可替换是"崩溃策略注入点" — main 装配 serverPanic。used_memory 是两层的共同度量。
源码位置: zmalloc.c:75-82; server.c:6712-6717,6970; evict.c:355
