# 闭环笔记 q1: performEvictions — 触发链与三态

## 假设
淘汰触发 = 命令执行前内存检查 (processCommand), 三态返回驱动: 拒绝命令 / 持续清除 / 正常。

## 验证过程
- 触发点 (server.c:4036-4059): `if (server.maxmemory && !isInsideYieldingLongCommand())` (L4036) → performEvictions() (L4037)
  - EVICT_FAIL && is_denyoom_command → rejectCommand(shared.oomerr) (L4049-4052, rejectCommand L4050) — OOM 拒绝
  - 前置: trackingHandlePendingKeyInvalidations (L4043, 淘汰使旧缓存失效) + current_client 检查 (L4048)
  - pre_command_oom_state 保存 (L4059) — EXEC/模块/Lua 内共享判定
- isSafeToPerformEvictions (evict.c:463-476): yielding 脚本 / loading / 从库 repl_slave_ignore_maxmemory / PAUSE_ACTION_EVICT → 跳过
- getMaxmemoryState (L379-415): zmalloc_used_memory (R-33 原子统计) vs maxmemory
  - **overhead 剔除** (L396-398): freeMemoryGetNotCountedMemory (L318-353) = AOF buf + repl 超出 backlog 部分 — 防 DEL 反馈环 (注释 L310-317: 计数会"越删越需要删")
  - level = mem_used/maxmemory; tofree = mem_used - maxmemory
- NO_EVICTION (L538-541): 需释放但策略禁止 → EVICT_FAIL
- 三态 (L515-519): EVICT_OK (达标/不可执行) / EVICT_RUNNING (超限处理中) / EVICT_FAIL (超限无键可删)
- 异步续清 (L432-457): startEvictionTimeProc → aeCreateTimeEvent → evictionTimeProc 每事件循环轮 performEvictions 直到 OK/FAIL
- 统计 (L750-759): stat_last_eviction_exceeded_time 累计超限时长

## 代码类型
Glue (触发编排)

## 跨域关联
- R-33 (zmalloc_used_memory) / R-20 (processCommand) / R-21 (dbGenericDelete) / R-17 (tracking)

## 结论
淘汰 = 命令前置检查 + 三态驱动: FAIL 拒 DENYOOM 命令 (清理类放行), RUNNING 由 aeTimeProc 持续清, OK 放行。overhead 剔除是防反馈环的关键设计 (AOF/repl 缓冲不计入需释放量)。
源码位置: server.c:4037-4050; evict.c:379-457,515-541
