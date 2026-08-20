# R-23 内存淘汰 — maxmemory 八策略与近似选择

> 前置: [[R-21-db]] (dbGenericDelete/lookupKey LRU 写面) + [[R-1-object]] (lru:24bit 双用途) + [[R-22-expire]] (volatile 采样源) + [[R-33-zmalloc]] (used_memory) | 引出: [[R-18-defrag]] (碎片) + [[R-9-replication]] (传播) | 对照: [[R-22-expire]] (过期 vs 淘汰 — 生命周期两种回收)
> 🟡 B | 6 KP | [模式: 三态驱动+近似最优+位域策略+预算执行]
> Pass 2 闭环: q1(触发与三态) q2(采样池) q3(LRU 近似) q4(LFU) q5(策略矩阵) q6(执行控制)

**读者处境**: maxmemory 超了会发生什么?为什么"刚访问过的键"还是可能被删?为什么说 Redis 的 LRU 是"近似"的?LFU 的 8 位计数器怎么模拟上百万次访问?volatile 和 allkeys 策略的本质区别是什么?这篇拆内存淘汰: 触发链与三态、跨 DB 采样池、LRU/LFU 近似、八策略矩阵、预算执行。

### 1. 触发链与三态 — 谁在什么时候淘汰

场景: 内存超限, 命令还能执行吗?
源码路径:
- 触发点 (server.c:4036-4059): processCommand 前置 — `maxmemory && !yielding 脚本` (L4036) → performEvictions() (L4037)
  - **EVICT_FAIL && CMD_DENYOOM 标志命令 → rejectCommand(oomerr)** (L4049-4052, rejectCommand L4050; is_denyoom_command 定义 L3958); 无 DENYOOM 标志放行 (DEL/EXPIRE 等清理类可执行)
  - pre_command_oom_state 保存 (L4059) — EXEC/模块/Lua 内共享判定
  - 前置: trackingHandlePendingKeyInvalidations (L4043, 淘汰使旧缓存失效) + current_client 检查 (L4048)
- **三态** (evict.c:515-519): EVICT_OK (达标/不可执行) / EVICT_RUNNING (处理中) / EVICT_FAIL (超限无键可删)
- isSafeToPerformEvictions (L463-476): yielding 脚本 / loading / 从库 ignore_maxmemory / PAUSE_ACTION_EVICT → 跳过
- **overhead 剔除** (L318-353, L396-398): AOF buf + repl 超出 backlog 部分不计入需释放量 — 防 DEL 反馈环 (注释: 越删 DEL 越大 → 越需要删)
- 异步续清 (L432-457): startEvictionTimeProc → aeTimeProc 每轮续跑直到 OK/FAIL
关键设计 (q1): 三态驱动 = **FAIL 拒写 / RUNNING 后台清 / OK 放行**; overhead 剔除是防反馈环关键。[模式: 三态驱动]
数据流: 命令 → 内存检查 → 超限? → 清 → FAIL+DENYOOM 命令 → oomerr。

### 2. 跨 DB 采样池 — 16 槽近似全局最优

场景: 怎么在所有 DB 里找到"最该删"的键?
源码路径:
- 池 (evict.c:33-43): EVPOOL_SIZE=16 全局单例 + 每项 (idle/key/cached/dbid/slot)
- **每 DB 采样** (L568-600): 全 DB 循环 — ALLKEYS → keys / 否则 expires (L577-581); FAIR 选槽 (L129, R-21 BIT) + dictGetSomeKeys (L130, maxmemory_samples=5)
- 采样上限 (L589-598): 每 DB 非空槽数循环, 满 5 键或 DB 键 < 5×10 提前退
- **idle 三打分** (L152-168): LRU → 空闲毫秒; LFU → `255-计数` (反频率); TTL → `ULLONG_MAX-TTL` (越早到期越大)
- **有序插入** (L173-205): 升序 memmove 插入; 比最差还差 → 跳过; cached sds 255B 复用 (L207-218, 免每键分配)
- 选键 (L604-630): 从池尾 (最大 idle) 回退, **幽灵键** (已删) 跳过 — 池不随删除更新
- **跨 DB 全局池** (注释 L571: "We don't want to make local-db choices") — 淘汰质量 ∝ 池全局性
关键设计 (q2): 近似最优 = **全 DB 采样 (5 键/DB) 进共享池 (16 槽)**, 每次淘汰取池中最大 idle — 常数内存的全局近似。[模式: 近似最优]
数据流: 全 DB 采样 → 池有序插入 → 取池尾 → 幽灵跳过 → 命中淘汰。

### 3. LRU 近似 — 24bit 时钟与回绕

场景: 为什么 Redis 的 LRU 不是精确 LRU?
源码路径:
- 常量 (server.h:896-898): LRU_BITS=24 / LRU_CLOCK_MAX=2^24-1 / RESOLUTION=1000ms — 满量程 ~194 天
- getLRUClock (evict.c:52-54): `(mstime()/1000) & 0xFFFFFF`
- **时钟缓存** (L60-68): hz≥1 → server.lruclock (serverCron 更新); 否则实时 — 免系统调用
- **回绕处理** (L72-80): `lruclock >= o->lru` 直接减; 否则 `lruclock + (MAX - o->lru)` — 符号判断
- 写面: lookupKey 命中 `val->lru = LRU_CLOCK()` (db.c:111, R-21 已交付)
- 近似本质 (注释 L82-100): 采样 N=5 → 池 M=16 — 常数内存, 非全局排序
关键设计 (q3): 24bit×1000ms 降精度时钟 + 采样池 = **常数内存近似 LRU** (精确 LRU 需要全局链表, 内存与时间不可行)。[模式: 降精度近似]
数据流: 访问 → lru=时钟 → 淘汰时 时钟−lru → idle → 池排序。

### 4. LFU — 8bit 对数计数 + 分钟衰减

场景: 8 位计数器怎么区分高频低频?
源码路径:
- 布局 (evict.c:230-260): `16bit LDT (分钟) + 8bit LOG_C` — 复用 lru:24bit 字段
- **对数概率递增** LFULogIncr (L281-289): `p = 1/(baseval×factor + 1)`, 计数越高 +1 概率越低 (指数饱和); 255 封顶; LFU_INIT_VAL=5 起跳 (新键免立即淘汰)
- **分钟衰减** LFUDecrAndReturn (L301-308): 每 lfu_decay_time 分钟 (默认 1) 减 1 — 惰性 (检查候选时才算)
- 时间: LFUGetTimeInMinutes (L265-267): `unixtime/60 & 65535` (16bit, 回绕 ~45 天)
- 池集成 (L162): idle = 255-counter — 反频率入池 (最低频先删)
- 写面: updateLFU (db.c:42-46): Decr + LogIncr → `lru = (分钟<<8)|counter`
- 参数: lfu-log-factor=10 / lfu-decay-time=1 (config.c:3159-3160)
关键设计 (q4): 对数计数 = **模拟 2 的幂计数器** (高频键指数级难增); 惰性衰减适应访问模式变化 (热键变冷)。[模式: 对数概率计数]
数据流: 访问 → 概率 +1 → 候选检查 → 衰减 → 反序入池。

### 5. 八策略矩阵 — 位域分派

场景: maxmemory-policy 的 8 个值怎么变成 3 种执行路径?
源码路径:
- 位域 (server.h:556-569): 高 8 位策略 id + 低 3 位行为标志 — FLAG_LRU (1<<0) / FLAG_LFU (1<<1) / FLAG_ALLKEYS (1<<2)
- 8 策略: volatile-lru/lfu/ttl/random + allkeys-lru/lfu/random + noeviction
- **来源选择** (evict.c:577-581, 609-613): `ALLKEYS → db->keys; 否则 db->expires` — volatile 族天然只碰有 TTL 的键
- **三条路径** (L538,564-566,635-637): 池排序 (LRU/LFU/TTL) / 直接随机 (RANDOM) / 拒绝 (NOEVICTION)
- TTL idle 特例 (L163-165): `ULLONG_MAX - TTL` — 无需值对象 (L143)
- 联动 (server.h:559-560): LRU/LFU 策略 → 禁共享整数 (NO_SHARED_INTEGERS, R-1 已交付) — 私有 LRU 需要
关键设计 (q5): 策略 = **行为标志 (怎么打分) + 来源标志 (从哪采样) + 特例路径** 的三维分解。[模式: 位域策略]
数据流: policy → 标志分解 → 采样源 (keys/expires) → 打分 (LRU/LFU/TTL) → 池/随机 → 淘汰。

### 6. 执行控制 — tenacity 预算与联动

场景: 一次要清 10GB, 会卡死吗?
源码路径:
- **tenacity 时间上限** (evict.c:479-494): ≤10 → 50us×tenacity (线性); <100 → `500×1.15^(t-10)` (几何, 99→~2min); =100 → 无限; 默认 10 (500us)
- **delta 实测法** (L674-681): 删除前后 zmalloc_used_memory 差 — 不估算
- **每 16 键周期** (L692-720): 从库缓冲 flush (L697, 防 DEL 积压) / lazyfree 重查内存 (L706-710, 后台可能已达标) / 超时 → startEvictionTimeProc (L715-718)
- 淘汰面 (L677-688): dbGenericDelete(lazyfree_lazy_eviction) → stat_evictedkeys (L682) → notify "evicted" → propagateDeletion
- 统计面 (L750-759): stat_last_eviction_exceeded_time 累计超限时长 (RUNNING/FAIL 起表, OK 归零累计)
- EVICT_FAIL 兜底 (L728-745): 等 lazyfree 后台 ≤1000us 轮询
关键设计 (q6): 预算执行 = **时间上限 + 周期三检查 + 异步续清** — 单命令不因大清理卡死。[模式: 预算执行]
数据流: 清键 → 16 键检查 → 超时 → 后台续清 → 下一命令。

### 负面空间 — 内存淘汰刻意不做的事

- **不做全局精确 LRU**: 采样+池近似 (常数内存), 精确 LRU 需要全局链表
- **不做逐键评估**: 只在候选池内决策 (每次最多 16×5 采样)
- **不做淘汰预算动态调优**: tenacity 是配置静态值, 无自适应反馈
- **不做大对象优先**: 淘汰只看 idle/频率, 不看对象大小 (注释: 只关心键空间内存)
- **不做淘汰排序保证**: volatile 键也可能被 allkeys 淘汰 (allkeys 忽略 TTL)

→ 引出: 淘汰后内存碎片怎么整理?→ [[R-18-defrag]]
