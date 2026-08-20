# 闭环笔记 q6: 执行控制 — 时间上限与联动

## 假设
淘汰循环有硬时间上限 (tenacity 换算), 每 16 键检查: 从库缓冲 flush / lazyfree 重查 / 时间退出。

## 验证过程
- evictionTimeLimitUs (evict.c:479-494):
  - tenacity ≤10: `50us × tenacity` (线性 0-500us)
  - 10 < tenacity < 100: `500 × 1.15^(tenacity-10)` (几何, 99 → ~2 分钟)
  - =100: ULONG_MAX (无限)
  - 配置: maxmemory-eviction-tenacity 0-100 默认 10 (config.c:3164)
- 主循环 (L556): `while (mem_freed < mem_tofree)`
  - **delta 实测法** (L674-681): 删除前后 zmalloc_used_memory 差值 — 而非估算
- 每 16 键周期 (L692-720):
  1. 有从库 → flushSlavesOutputBuffers (L697, 防 DEL 积压饿死从库)
  2. lazyfree 时 → getMaxmemoryState 重查 (L706-710, 后台线程可能已达标)
  3. **超时** → startEvictionTimeProc (L715-718) 异步续清 + break
- 淘汰执行 (L677): dbGenericDelete(lazyfree_lazy_eviction) → stat_evictedkeys (L682) → signalModifiedKey (L683) → notify "evicted" (L684-685) → propagateDeletion (L686) → postExecutionUnitOperations (L688)
- EVICT_FAIL 兜底 (L728-745): 无键可删时等 lazyfree 后台 (bioPendingJobsOfType, ≤1000us 睡眠轮询)
- 结果判定 (L726): isEvictionProcRunning → EVICT_RUNNING

## 代码类型
Mechanism (预算 + 联动)

## 跨域关联
- R-20 (aeTimeEvent/processCommand) / R-21 (dbGenericDelete) / R-33 (zmalloc_used_memory) / R-9 (从库输出缓冲)

## 结论
执行控制 = 实测 delta + 16 键周期三检查 + tenacity 时间上限 (线性→几何→无限三级)。超时转 aeTimeProc 异步续清 — 单次命令不卡死。EVICT_FAIL 时对 lazyfree 后台做有限等待。
源码位置: evict.c:479-494,556-726,728-745
