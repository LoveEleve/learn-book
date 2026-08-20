# R-23 内存淘汰 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | maxmemory + LRU 近似采样 (版权 "2009-Present"); 早期策略少 (volatile-lru/allkeys-lru/volatile-ttl/noeviction) |
| 3.0 | **LFU 引入** — 24bit lru 字段双用途 (16bit 分钟 + 8bit 对数计数); 注释 L227-260 完整设计文档 |
| 4.0 | lazyfree 联动: lazyfree_lazy_eviction (dbGenericDelete 异步面) + EVICT_FAIL 时等 bio 线程 (L735-742) |
| 7.0 | **tenacity 重构**: maxmemory-eviction-tenacity (0-100, 线性→几何→无限三级时间上限, L479-494); performEvictions 三态 (EVICT_OK/RUNNING/FAIL) + evictionTimeProc 异步续清 (L432-457) |
| 7.0 | **kvstore 分片适配**: evictionPoolPopulate 用 kvstoreGetFairRandomDictIndex (L129, R-21 BIT) + dictGetSomeKeys |
| 演进 | 采样池从"局部 DB 池"→"跨 DB 全局池" (注释 L571: "We don't want to make local-db choices") |

## 痕迹证据

- L22-32 头注释: 池设计文档 (升序 idle 排列/LFU 反频率/幽灵键)
- L82-100: LRU 近似算法注释 (N=5 采样 M=16 池) — "runs in constant memory"
- L230-260: LFU 24bit 拆分设计文档 (对数计数动机/衰减动机/INIT_VAL)
- L310-317: overhead 剔除动机注释 (DEL 反馈环) — "massive eviction loop, even all keys are evicted"
- L571: "We don't want to make local-db choices" — 全局池演进证据
- L207-210: cached sds 注释 ("according to the profiler, not my fantasy") — 优化痕迹
- L493: tenacity=100 → ULONG_MAX (无限制) — 三级设计的极端档

## 推断标注

- "LFU 对数计数 ~1M 次访问到 100" — 注释 (L254-257 提到 chance 依赖 factor), 具体数值 ~10 万次/100 计数是社区经验值, 代码无精确断言 — 大纲已用定性表述
- "geometric progression 99 → ~2min" — 代码注释 (L489) 给出, 复算: 500×1.15^89 ≈ 500×6.4e5 ≈ 3.2e8us ≈ 5.3min — **注释的 ~2min 与复算不一致** (1.15^89 = e^(89×ln1.15) = e^(89×0.1398) = e^12.44 ≈ 2.5e5; 500×2.5e5 = 1.25e8us = 125s ≈ 2min ✅ 复算正确, 我之前心算错) — 记录: 1.15^89≈2.5e5, 125s≈2min ✅
- "194 天回绕" — 24bit×1000ms = 16777215s ≈ 194.2 天 (代码事实推导)
- "LFU 16bit 回绕 ~45 天" — 65535 分钟 = 45.5 天 (代码事实推导)
