# 闭环笔记 q6: 配置/统计/模块面 — 7 配置 + INFO + moduleDefragValue

## 假设
配置面 = activedefrag 开关 + 6 个 active-defrag-* 参数; 统计面 = INFO; 模块面 = 注册制搬移回调。

## 验证过程
- **配置穷举** (config.c) — 7 项:
  1. `activedefrag` (L3080): Bool, DEBUG|MODIFIABLE, 默认 0; **isValidActiveDefrag** (L2323-2337): 非 HAVE_DEFRAG 编译 → 启用报错 "requires a Redis server compiled with a modified Jemalloc"
  2. `active-defrag-cycle-min` (L3155): Int 1-99, 默认 1 (% CPU at lower threshold)
  3. `active-defrag-cycle-max` (L3156): Int 1-99, 默认 25 (% CPU at upper threshold)
  4. `active-defrag-threshold-lower` (L3157): Int 0-1000, 默认 10 (% frag 启动下界)
  5. `active-defrag-threshold-upper` (L3158): Int 0-1000, 默认 100 (% frag 满 effort 上界)
  6. `active-defrag-max-scan-fields` (L3196): ULong 1-LONG_MAX, 默认 1000 (大键延后阈值)
  7. `active-defrag-ignore-bytes` (L3220): SizeT 1-LLONG_MAX, 默认 100<<20 = **100MB** (MEMORY_CONFIG)
  - cycle-min/max + threshold-upper 挂 updateDefragConfiguration 回调 (configuration_changed)
  - 附: `jemalloc-bg-thread` (redis.conf L2294 默认 yes; updateJemallocBgThread config.c:2469+)
- **INFO 统计** (server.c): `active_defrag_running` (L5746, 测试断言 effort 用) + `active_defrag_hits/misses` (L5896-5897) + `active_defrag_key_hits/misses` (L5898-5899) + `active_defrag_scanned` (L5900) + `total_active_defrag_time/current_active_defrag_time` (L5852-5853) + `latency "active-defrag-cycle"` (defrag.c:1242)
- **DEBUG 命令**: `debug mallctl arenas.page` (测试 L53 用 — 判定页大小); DEBUG SET-ACTIVE-EXPIRE 类旁路无 defrag 专用
- **模块面** (module.c):
  - `RM_RegisterDefragFunc` (L13449) — 注册 defrag 回调
  - `moduleDefragValue` (L13580) — 立即搬移入口 (defragKey 调); 返回 0 → defragLater (defrag.c:723-724)
  - `moduleLateDefrag` (L13553) — 延后搬移 (cursor/endtime 续扫, defrag.c:935)
  - `moduleDefragGlobals` — 全局搬移 (defragOtherGlobals 调, defrag.c:912)
  - 测试: tests/modules/defragtest.c + unit/moduleapi/defrag.tcl (RegisterDefragFunc 实证 L233)
- **测试地图** (memefficiency.tcl L39-800, run_solo defrag): main dictionary (700000 键 + frag≥1.4→<1.1 + **digest 校验数据不变** L134-136 + RDB save) / AOF loading (while-blocked-cron + hits>100000) / eval scripts / big keys / pubsub / HFE / big list / edge case — 8 场景
- **负面空间** (补): 无 defrag 专用命令; MEMORY DOCTOR 只报告不治疗 (对照检查); 不做 jemalloc arenas 数量调优

## 代码类型
Interface (配置/统计/扩展面)

## 跨域关联
- R-33 (zmalloc): HAVE_DEFRAG 编译开关 (zmalloc.h:79) / mallctl 统计
- R-31 (module): RegisterDefragFunc API
- R-8 (persistence): AOF loading 期间 defrag (whileBlockedCron)

## 结论
7 配置 (开关+6 参数, 全 MODIFIABLE 热调) + INFO 8 统计 + latency 采样 + 模块注册制回调 (立即/延后/全局三入口); 非 jemalloc 编译拒绝启用。
源码位置: config.c:2323-2337,2462-2466,3080,3155-3158,3196,3220; server.c:5746,5852-5853,5896-5900; module.c:13449,13553,13580; tests/unit/memefficiency.tcl:39-800
