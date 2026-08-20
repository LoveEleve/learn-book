# R-23 内存淘汰 — Pass 1 探索笔记

> 域: R-23 内存淘汰 (evict.c) | 🟡 B 方案 | 2026-08-13
> 源码: src/evict.c (761) | Redis 7.4.2

## 调用图

```
processCommand (server.c:4037, maxmemory && !yielding 脚本):
  performEvictions() == EVICT_FAIL && is_denyoom_command (CMD_DENYOOM, L3958) → rejectCommand(oomerr) (L4049-4052)
  → pre_command_oom_state 保存 (L4050)

performEvictions (evict.c:520):
  1. isSafeToPerformEvictions (L463): yielding 脚本/loading/从库 ignore_maxmemory/PAUSE_ACTION_EVICT
  2. getMaxmemoryState (L379): zmalloc_used_memory - (AOF+repl 缓冲 overhead L318) vs maxmemory
  3. NO_EVICTION → EVICT_FAIL (L538)
  4. evictionTimeLimitUs (L479): tenacity ≤10 → 50us×tenacity (线性); <100 → 500×1.15^(t-10) (几何, 99→~2min); =100 → ULONG_MAX
  5. while (mem_freed < mem_tofree):
     a. 池路径 (LRU/LFU/TTL): 全 DB 采样填充池 (L568-600) → 从池尾 (最大 idle) 取键 (L604-630)
     b. random 路径 (L635-658): next_db 轮转 + FAIR 随机
     c. dbGenericDelete (lazyfree_lazy_eviction) (L677) → delta 实测 (L675-681)
     d. 每 16 键 (L692): flushSlavesOutputBuffers / lazyfree 时重查内存 / 时间上限 → startEvictionTimeProc (L717)
  6. EVICT_RUNNING 判定 (L726); cant_free: lazyfree 等待 (L735-742, ≤1000us 睡眠)
  7. 统计: stat_evictedkeys / stat_last_eviction_exceeded_time / latency eviction-cycle

evictionTimeProc (L437): aeCreateTimeEvent 循环 (startEvictionTimeProc L451) — 持续清直到 OK/FAIL

evictionPoolPopulate (L125):
  - FAIR 选槽 (kvstoreGetFairRandomDictIndex) + dictGetSomeKeys (samples=5)
  - idle 计算: LRU → estimateObjectIdleTime; LFU → 255-LFUDecrAndReturn; TTL → ULLONG_MAX-TTL (L152-168)
  - 有序插入池 (升序, 大 idle 靠右) (L173-205); cached sds 复用 (255B, L207-218)

LRU (L52-80): getLRUClock = mstime/1000 & (2^24-1); LRU_CLOCK 缓存 (hz≥1s 分辨率时用 server.lruclock); estimateObjectIdleTime 回绕处理
LFU (L265-308): 24bit = LDT 16bit (分钟) + LOG_C 8bit; LFUGetTimeInMinutes = unixtime/60 & 65535; LFULogIncr 概率对数递增 (p=1/(baseval×factor+1), 饱和 255); LFUDecrAndReturn 衰减 (每 lfu_decay_time 分钟 -1)
```

## 基本元素分解

1. **触发与状态**: maxmemory 检查链 (processCommand → performEvictions → getMaxmemoryState)
2. **三态返回**: EVICT_OK / EVICT_RUNNING / EVICT_FAIL → 命令拒绝 (denyoom)
3. **采样池**: EVPOOL_SIZE=16 全局池 + 每 DB maxmemory_samples=5 采样 + 有序插入 + cached sds 复用
4. **八策略矩阵**: 3 标志位 (LRU/LFU/ALLKEYS) × volatile/allkeys + TTL/RANDOM/NOEVICTION
5. **LRU 近似**: 24bit 时钟 + 1000ms 分辨率 + 回绕处理
6. **LFU**: 16bit 时间 + 8bit 对数计数 (概率递增 + 分钟衰减)
7. **执行控制**: tenacity 时间上限 / 16 键周期检查 / 从库缓冲 flush / lazyfree 等待
8. **传播**: notify "evicted" + propagateDeletion

## 标记问题 (9 个)

1. performEvictions 的三态怎么驱动命令拒绝与异步持续清?
2. 采样池怎么保证"跨 DB 全局最优"而非局部?
3. 池插入的复杂度与 cached sds 复用 (255B)?
4. 8 策略怎么用 3 个标志位分解?
5. LRU 24bit 回绕怎么算 idle?
6. LFU 对数计数的概率公式?衰减怎么处理冷门变化?
7. tenacity 时间上限怎么算 (线性/几何/无限制)?
8. volatile 策略为什么从 expires 表采样?TTL 策略为什么不需要值对象?
9. 淘汰键的传播/通知/从库缓冲联动?

## 时空溯源 (代码内痕迹)

- 2009 (antirez): maxmemory + LRU 近似采样 (版权 2009-Present)
- 2.8/3.0: LFU 引入 (24bit 复用 LRU 字段); volatile-ttl/random 策略
- 4.0: lazyfree 联动 (lazyfree_lazy_eviction, bio 线程)
- 7.0: **eviction tenacity** (maxmemory-eviction-tenacity, 几何级时间上限); performEvictions 重构 (EVICT_RUNNING + evictionTimeProc); kvstore 分片适配 (FAIR 选槽)
- 演进: 池从"局部 DB 池"→"跨 DB 全局池" (注释 L571: "We don't want to make local-db choices")

## 大域拆分判断

761 行单文件 — **不拆** (🟡 B, 6 闭环足够)。
