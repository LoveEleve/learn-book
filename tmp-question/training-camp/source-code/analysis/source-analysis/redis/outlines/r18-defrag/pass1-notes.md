# R-18 内存碎片 — Pass 1 探索笔记

> 域: R-18 内存碎片 (defrag.c) | 🟡 B 方案 | 2026-08-14
> 源码: src/defrag.c (1270) + zmalloc.c (no_tcache) + dict.c (ScanDefrag) + server.c (cron) | Redis 7.4.2

## 调用图

```
调度面 (server.c):
serverCron (L1066): activeExpireCycle 后 → activeDefragCycle() ← 常规每 tick
whileBlockedCron (L1586): 阻塞期间循环调用 (补齐 CPU 预算 — 注释 L1578-1583: 25% CPU 需 2.5ms)
activeDefragCycle (defrag.c:1053-1252):
  未启用 → 清运行态; fork 子进程 → return (COW 保护 L1089)
  run_with_period(1000) → computeDefragCycles (每秒决策) + configuration_changed 立即决策
  timelimit = 1M×running/hz/100 (CPU 百分比 → 微秒)
  四阶段: db->keys / db->expires / pubsub_channels / pubsubshard_channels (L1172-1179)
  每 16 迭代/512 重分配/64 键 检时限 (L1224-1227); defragOtherGlobals 须同周期完成 (L1222-1223)
  完成后 running=0 + 重新 compute (L1144-1147)

搬移原语 (defrag.c):
activeDefragAlloc (L39-55): je_get_defrag_hint(ptr)? → zmalloc_usable_size → zmalloc_no_tcache(size) → memcpy → zfree_no_tcache → hits++
  ← jemalloc 补丁 API (L30-32 注释 "added to jemalloc in order to help us understand which pointers are worthwhile moving")
zmalloc_no_tcache/zfree_no_tcache (zmalloc.c:199-213): mallocx/dallocx MALLOCX_TCACHE_NONE (绕过 thread cache — 避免拿回刚释放的同一块)

指针家族 helper: activeDefragSds (偏移保留 L62-71) / activeDefragHfield (L78-87) / activeDefragStringObEx (expected_refcount + EMBSTR 偏移 L96-127) / activeDefragLuaScript / dictDefragTables (结构+双表 L165-183) / zslDefrag (update[] 数组 L209-242)

渐进扫描面 (dict.c):
dictScanDefrag (dict.c:1385): dictScan 变体 + defragfns (defragAlloc/defragKey/defragVal, dict.h:142)
kvstoreDictScanDefrag (kvstore.c): 跨 slot 版
activeDefragSdsDict (L295-310): val_type 5 档 (NO_VAL/SDS/STROB/VOID_PTR/LUA_SCRIPT)
activeDefragHfieldDict (L313-324): hfield 键 + ebDefragItem (HFE 交叉 L278-279)

类型分派面:
defragKey (L729-822): 键名 (含 expires 表 + hashTypeUpdateKeyRef) → robj (HFE: ebDefragItem L755-758) → 按 type×encoding 分派 (listpack/quicklist/intset/HT/skiplist/stream/module)
defragPubsubScanCallback (L870-902): 共享频道名 refcount=clients+1 断言 + 双端引用更新
defragOtherGlobals (L906-915): eval scripts / module globals / pubsub 两表 LUT

大键延后面:
defragLater (L386-389): sdsdup key → db->defrag_later 列表 (阈值 active_defrag_max_scan_fields)
defragLaterStep (L950-1010): 静态续扫状态 + 每键 16 迭代/512 重分配/64 字段检时限
scanLaterList (L392-433): quicklistBookmark "_AD" 续扫 (128 迭代/时限)
scanLaterStreamListpacks (L567-616): static last[16] + raxSeek(">") 续扫
scanLaterSet/Zset/Hash (L446-484): dictScanDefrag 游标续扫

碎片率面:
getAllocatorFragmentation (L841-867): frag_pct = frag_smallbins_bytes/allocated×100 (小 bin 浪费占比 — 注释: 大 bin 多时 rss 比例虚高 L854-857); Lua arena 排除 (L845-852); rss_pct 仅日志
computeDefragCycles (L1016-1048): 双门槛 (pct≥lower AND bytes≥ignore) → INTERPOLATE 线性插值 + LIMIT 钳制 → 只升不降 (L1039-1047, configuration_changed 例外)

配置 (config.c): activedefrag (L3080, 默认 0, isValidActiveDefrag 非 jemalloc 拒) + active-defrag-* 6 项 (L3155-3220)
统计 (INFO): active_defrag_running (server.c:5746) / hits/misses/key_hits/key_misses/scanned + total_active_defrag_time (5852-5853)
模块: moduleDefragValue / moduleLateDefrag (module.c, 模块键可自实现搬移回调)
```

## 基本元素分解

1. **搬移原语**: je_get_defrag_hint 判定 + no_tcache 搬移 (新分配-拷贝-释放, 指针必须替换)
2. **指针家族**: sds/hfield/robj/dict/zsl 各自偏移保留与引用更新
3. **渐进扫描**: dictScanDefrag (R-3 SCAN 变体) + defragfns 三回调
4. **类型分派**: defragKey 按 type×encoding 全谱 (listpack 整块搬/intset 整块/HT 内部扫描/skiplist 双结构)
5. **大键延后**: defrag_later 列表 + scanLater 族 (bookmark/静态 last 续扫)
6. **碎片率判定**: small-bins 占比 + Lua arena 排除
7. **调度**: computeDefragCycles 插值 + activeDefragCycle 四阶段 + 时限检查
8. **COW 交互**: fork 暂停 + whileBlockedCron 补齐
9. **命令/配置面**: 7 配置 + INFO 6 统计 + module API

## 标记问题 (20 问)

1. je_get_defrag_hint 怎么判定"值得搬"? (jemalloc 补丁, 页/arena 级别)
2. 为什么必须 no_tcache 分配? (MALLOCX_TCACHE_NONE 防拿回同一块)
3. EMBSTR 怎么搬? (同 chunk 内偏移重算)
4. expected_refcount 参数的意义? (共享对象不搬 — refcount 检查)
5. dictScanDefrag 与 dictScan 的区别? (桶内指针就地替换)
6. defragfns 三回调 (Alloc/Key/Val) 的分工?
7. defragKey 的键名搬移为什么还要同步 expires 表? (共享 sds 指针)
8. HFE 交叉: ebDefragItem 为什么不能直接搬? (时间桶引用)
9. pubsub 频道名搬移的 refcount 断言? (clients+1)
10. 大键延后的阈值是什么? (max_scan_fields=1000)
11. scanLaterList 的 bookmark 机制? (quicklistBookmark "_AD")
12. stream 的 static last 续扫为什么安全? (单线程)
13. frag_pct 为什么用 small-bins 不用 rss? (大 bin 虚高注释)
14. INTERPOLATE 插值 + LIMIT 钳制的边界?
15. 为什么"只升不降"? (扫描中途降 effort 无意义)
16. 双门槛 AND 语义? (pct 和 bytes 都要超)
17. fork 子进程为什么必须暂停? (COW 页)
18. whileBlockedCron 为什么多轮调用? (阻塞期间 CPU 预算补齐)
19. 四阶段顺序的意义? (keys 主战场 → expires 计数 → pubsub)
20. 搬移后指针替换失败的后果? (double-free/UAF — 测试 digest 校验)

## 时空溯源 (代码内痕迹)

- defrag.c 版权 2020-Present (L8) — 但 redis.conf L2242 "implemented by Oran Agra for **Redis 4.0**" — 4.0 引入 (2018), 2020 版权=文件重组/开源许可变更 (RSAL 2020)
- jemalloc 补丁: je_get_defrag_hint (L30-32) — 4.0 同期
- 演进痕迹: HFE 集成 (ebDefragItem, hashTypeUpdateKeyRef — 7.4 时代); kvstoreDictLUTDefrag (7.x kvstore 适配); listpackEX (7.4 HFE); defragStage 数组 (7.x 多阶段); whileBlockedCron 循环 (阻塞期间)
- moduleDefragValue (module API) — 6.0 模块 defrag 支持
- 配置: active-defrag-max-scan-fields 6.0 引入 (大键延后) — 推断, 需标注

## 大域拆分判断

1270 行单文件 — **不拆** (🟡 B, 6 闭环足够)

## 域级怀疑审计 (HANDOFF §四 R-18 简案 断言复查)

| HANDOFF 断言 | 验证 | 结论 |
|:--|:--|:--|
| "active-defrag-* 配置: ignore-bytes/**ignore-fragmentation**/cycle-min/max" | config.c:3155-3220 grep: 无 ignore-fragmentation; 实为 6 项: ignore-bytes/threshold-lower/threshold-upper/cycle-min/cycle-max/max-scan-fields + activedefrag 开关 (L3080) | **修正: 无 ignore-fragmentation; 7 配置** (数字穷举) |
| "**jemalloc arena 遍历** (mallctl)" | 逐指针判定用 **je_get_defrag_hint** (补丁 API, L30-32); mallctl 仅碎片率统计 (zmalloc_get_allocator_info) + Lua arena 排除 (L845-852) | **修正: 判定=hint 补丁 API, mallctl 仅统计面** |
| "逐对象搬移 (dict 重哈希式渐进)" | dictScanDefrag (dict.c:1385) + kvstoreDictScanDefrag — R-3 SCAN 变体 | **接受** ✅ |
| "碎片率判定 (jemalloc stats)" | 精确化: frag_pct = frag_smallbins_bytes/allocated (小 bin 浪费占比, 非 rss — L854-857 注释) | **接受+精确化** |
| "COW 交互 (fork 子进程时暂停)" | L1089 hasActiveChildProcess → return | **接受** ✅ |
| "大对象 vs 小对象策略" | 阈值 = active_defrag_max_scan_fields (1000 字段) → defragLater 列表 | **接受+精确化** (阈值名与语义) |
