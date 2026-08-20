# 闭环笔记 q5: 碎片率判定与调度 — computeDefragCycles + activeDefragCycle

## 假设
碎片率 = jemalloc small-bins 浪费占比 (非 rss); 启动双门槛; effort 线性插值; cron 每 tick 渐进执行 + fork 暂停。

## 验证过程
- **碎片率** (defrag.c:841-867 getAllocatorFragmentation):
  - `zmalloc_get_allocator_info(1, &allocated, &active, &resident, ..., &frag_smallbins_bytes)` (zmalloc.c, jemalloc mallctl 面)
  - **Lua arena 排除** (L845-852): server.lua_arena != UINT_MAX → 减掉 lua 的 resident/active/allocated/frag — Lua 分配不可搬 (不受 defrag 控制)
  - **公式** (L858): `frag_pct = frag_smallbins_bytes / allocated × 100` — 注释 L854-857: 用**可搬的小 bin 浪费**相对总分配, 而非 rss 比例 — "if most of the memory usage is large bins, we may show high percentage, despite the fact it's not a lot of memory"
  - rss_pct/rss_bytes 仅日志 (L859-865)
- **启动门槛** (L1020-1023): `frag_pct < lower || frag_bytes < ignore_bytes → return` — **双门槛 AND 语义** (两个都要超; 任一不足不启动; 对照 redis.conf L2272-2276 "Minimum amount" + "Minimum percentage")
- **effort 插值** (L1027-1034): INTERPOLATE(frag_pct, lower, upper, cycle_min, cycle_max) + LIMIT(min,max) — 线性映射 [lower,upper] → [1%,25%]; 超出范围钳制
- **只升不降** (L1039-1047): 扫描中途 cpu_pct 只增不减 (frag 下降不降低当前 effort — "should not lower the aggressiveness when fragmentation drops"); **configuration_changed 例外** (CONFIG SET 后重新考虑, config.c:2462-2466 updateDefragConfiguration 置位)
- **cycle 主循环** (L1053-1252 activeDefragCycle):
  - 静态续扫态 (slot/current_db/defrag_stage/cursor/db — L1054-1060)
  - 禁用清理 (L1069-1087); **fork 暂停** (L1089-1090: "Defragging memory while there's a fork will just do damage" — COW 页搬移会放大复制)
  - 每秒 computeDefragCycles (run_with_period(1000), L1094-1096) + configuration_changed 立即决策 (L1100-1103)
  - **timelimit** (L1109-1112): `1M × running / hz / 100` — CPU 百分比 × 周期时间 (25%@100hz = 2.5ms; 对照 activeExpireCycle 同款注释 L1108)
  - **四阶段** (L1172-1179): db->keys (真搬移) → db->expires (仅计数 — 键与 keys 表共享, 值无意义) → pubsub_channels → pubsubshard_channels (LUT + 频道)
  - **时限三条件** (L1224-1227): 16 迭代 / 512 指针重分配 / 64 键 — 与 defragLaterStep (L990-992) 同款
  - **defragOtherGlobals 须同周期完成** (L1222-1223 注释): 最后一个 db 后不中断 — 全局搬移是单次操作
  - 完成后 (L1137-1148): running=0 + 立即 compute (frag 仍高则马上重启)
- **双调用点** (server.c): serverCron L1066 (常规每 tick) + **whileBlockedCron L1586** (阻塞命令期间循环多轮 — 注释 L1578-1583: "if activeDefragCycle needs to utilize 25% cpu, it will utilize 2.5ms, so we need to call it multiple times"); 阻塞恢复时 whileBlockedCron 补足 cron 周期
- **统计**: stat_active_defrag_hits/misses/key_hits/key_misses/scanned (INFO server.c:5746+5852-5853) + latency "active-defrag-cycle" (L1242)

## 代码类型
Algorithmic (插值调度 + 时限预算)

## 跨域关联
- R-2 (events): serverCron/whileBlockedCron 双调用 + latency monitor
- R-22 (expire): activeExpireCycle 同款 timelimit 模式
- R-30 (Lua): lua_arena 排除

## 结论
碎片率 = small-bins 浪费/总分配 (Lua 排除); 双门槛 AND 启动; effort 线性插值+钳制+只升不降; cron 每 tick 渐进 + 16/512/64 三条件时限; fork 暂停; whileBlockedCron 补齐阻塞期预算。
源码位置: defrag.c:841-867,1016-1048,1053-1252; server.c:1066,1578-1586; config.c:2462-2466
