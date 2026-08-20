# R-22 过期机制 — 主动回收与命令面

> 前置: [[R-21-db]] (惰性过期 expireIfNeeded db.c:1974 + kvstoreScan/hexpires) + [[R-20-server]] (databasesCron/beforeSleep 调用点) | 引出: [[R-23-evict]] (淘汰) + [[R-9-replication]] (传播) | 对照: [[R-21-db]] (惰性 vs 主动双面)
> 🟡 B | 6 KP | [模式: 预算自适应+采样驱动+角色记账+命令规范化]
> Pass 2 闭环: q1(SLOW/FAST 双循环) q2(采样驱动) q3(HFE 配额) q4(从库记账) q5(命令族) q6(查询面)

**读者处境**: 设置了 TTL 的键, 没人访问它, 什么时候被删?为什么主从时钟不用同步?可写从库自己生成的临时键过期了谁来管?EXPIRE 和 PEXPIREAT 为什么最终都变成同一条命令?这篇拆主动过期: SLOW/FAST 双循环预算、抽样驱动、HFE 配额、可写从库记账、EXPIRE 命令族。

### 1. activeExpireCycle — SLOW/FAST 双循环预算

场景: 过期键没人访问, 谁来清?
源码路径:
- 调用点: **SLOW → databasesCron** (server.c:1059, iAmMaster 分支, 每 hz tick) / **FAST → beforeSleep** (server.c:1687-1690, 每事件循环轮; 同样 iAmMaster + active_expire_enabled 双条件)
- 基线常量 (expire.c:92-96): KEYS_PER_LOOP=20 / FAST_DURATION=1000us / SLOW_TIME_PERC=25% / ACCEPTABLE_STALE=10%
- **effort 缩放** (L191-200): active_expire_effort 1-10 (config.c:3177) → 四参数全缩放 (20→+5/档, 1000us→+250, 25%→+2, 10%→-1)
- **FAST 拒跑两条件** (L218-231): 上次未超时且过期比例低 → return; 距上次 <2×1000us → return
- 时间预算 (L247-252): SLOW = `25%×1000000/hz/100` (hz=10 → 25ms); FAST = 1000us
- **超时传递** (L240-241): 上次 timelimit_exit → 本次 dbs_per_call = 全 DB 扫 (默认 CRON_DBS_PER_CALL=16, server.h:104)
- 每 16 迭代查时间 (L383-390); PAUSE_ACTION_EXPIRE 全局暂停 (L216)
关键设计 (q1): 双循环 = **低频高预算 + 高频低预算** 互补; FAST 只在"上次超时或有积压"时启动 — 正常情况零开销。[模式: 预算制自适应]
数据流: cron tick → SLOW (25ms 上限) → 超时标记 → FAST 补漏 (1ms) → 全清 → FAST 休眠。

### 2. 采样驱动 — 20 键 × 20 桶 + 持久游标

场景: 一次循环清多少?怎么保证所有 DB 雨露均沾?
源码路径:
- 每 DB 每轮: **num = min(expires 大小, 20)** (L313-314) + **桶上限 num×20** (L326) — 键与桶双限
- **expires_cursor 持久游标** (L332, server.h:980): kvstoreScan(db->expires, cursor) — 跨调用续扫, 每 DB 独立
- 游标回 0 = 该 DB 扫完 (L333-335)
- **repeat 判定** (L348): 过期比例 >10% (effort 可调) 才重扫同一 DB; 否则下一 DB — 不做无谓重复
- **填充率门槛** (isExpiryDictValidForSamplingCb L127-137): 桶填充 <1% 跳过 (等缩容, 防稀疏扫描浪费)
- avg_ttl 统计 (L353-382): `新 = 采样均值 + (旧 − 采样均值)×pow(0.98, n)` — 指数滑动, pow 常数表 (L24, 16 项) 替代循环
- stale_perc (L399-407): 5%/95% 滑动平均 — **FAST 的触发信号**
关键设计 (q2): 采样 = **预算内尽可能多清 + 比例反馈** (过期率高于 10% 就继续, 否则让位); 统计双滑动平均反馈到调度。[模式: 采样驱动反馈]
数据流: 游标 → 扫 20 键 → 统计过期比例 → repeat? → 下 DB / 下轮。

### 3. HFE 主动过期 — 10000 字段/秒配额

场景: hash 字段级过期怎么批量清?
源码路径:
- 独立循环 activeExpireHashFieldCycle (L144-185): 每 DB 先 HFE 后键过期 (L288, "HFE DS is optimized for active expiration")
- **maxToExpire = 10000/hz** (L166, hz=10 → 1000 字段/次)
- **序列放大** (L170-174): 累积清不完 >100 万 (L154) → 配额 ×1-32 (积压越多越激进)
- 判定 (L177-184): hashTypeDbActiveExpire (t_hash.c:2073) 返回达配额 → 积压标记; 否则清零进下一 DB
- 空表快速轮转 (L159-163)
关键设计 (q3): HFE 独立配额 + **积压反馈放大** — 与键过期同一时间预算内交错, 但互不挤占配额。[模式: 配额自适应]
数据流: hexpires 非空 → 1000 字段/次 → 清不完 → ×N 放大 → 清完 → 下一 DB。

### 4. expireSlaveKeys — 可写从库的记账回收

场景: 从库自产的临时键过期了, master 不知道, 谁删?
源码路径:
- 记账表 (L445): **slaveKeysWithExpire** — 键 → uint64 dbid 位图 (键 sdsdup 副本 L532)
- 记账时机 (rememberSlaveKeyWithExpire L511-539): setExpire 触发 (db.c:1860-1862) — 仅 masterhost + 可写
- **位图上限 63 DB** (L524) — 显式折衷 (注释 "trivial fix")
- 回收 (expireSlaveKeys L449-507): cron 从库分支 (server.c:1061) → 随机键 → 位图逐 DB 查过期 → 全清即删记账项
- **停止三条件** (L500-505): 连续 3 个不可过期 / 64 循环且 >1ms / 表空
- FLUSHALL 清表 (flushSlaveKeysWithExpireList L555, db.c:542 调用) — 防误删同名新键
关键设计 (q4): 角色特例 = **"master 不知道的键"独立记账回收** (3.2 前泄漏, 注释 L430); 位图压缩多 DB 跟踪到 1 个 dict entry。[模式: 角色边界记账]
数据流: 从库自产键+TTL → 记账表 (位图) → cron 抽查 → 过期删 + 清记账。

### 5. EXPIRE 命令族 — 传播归一三格式

场景: 四条 EXPIRE 命令怎么变成三种归一格式?
源码路径:
- 统一入口 expireGenericCommand (L635): basetime + unit 二参数归一 (L753-770 四命令)
- 溢出守卫 (L651-663): SECONDS ×1000 前溢出预检 (`> LLONG_MAX/1000`) + basetime 加法溢出检查
- **NX/XX/GT/LT** (L671-713): 条件设置 — NX 无 TTL 才设 / XX 有才设 / GT 更大才设 (无 TTL 视为无限 → 必失败) / LT 更小才设 (无 TTL 视为无限 → 通过)
- **checkAlreadyExpired** (L562-570, L715): `when <= now && !loading && !masterhost` → **直接删 + 重写为 DEL 或 UNLINK** (L718-728, 依 lazyfree_lazy_expire 配置) + notify "del"
- 正常路径 (L730-748): setExpire (R-21) → **重写为 PEXPIREAT 毫秒时间戳** (L734-736) + 参数归一 (L739-743) + notify "expire"
- 互斥: NX×XX/GT/LT, GT×LT 不兼容 (L609-617)
关键设计 (q5): 命令规范化 = **传播面只认 PEXPIREAT (未过期) / DEL|UNLINK (已过期) 三种** — 主从无需时钟同步 (绝对时间戳), 已过期在源端就变 DEL。[模式: 传播归一]
数据流: EXPIRE key 60 → 溢出检查 → 条件检查 → 已过期? → DEL 重写 / setExpire + PEXPIREAT 重写。

### 6. TTL/PERSIST/TOUCH — 查询面

场景: TTL 的三值语义是什么?TOUCH 返回什么, 和 GET 有什么不同?
源码路径:
- ttlGenericCommand (L773-794): **-2 不存在 / -1 无 TTL / 剩余毫秒** (L777-792); 秒级四舍五入 `(ttl+500)/1000` (L792)
- EXPIRETIME/PEXPIRETIME (L807-814): 返回到期时间戳本身 (绝对)
- 时间源统一 commandTimeSnapshot (server.c:221-232): 命令期冻结 (脚本内恒定, #1525)
- persistCommand (L817-830): lookupKeyWrite (过期先被删) → removeExpire (db.c:1838)
- touchCommand (L833-838): 纯 LRU 更新访问 (返回触摸计数) — db.c:104-106 特判豁免 CLIENT_NO_TOUCH
关键设计 (q6): 三值语义 = 协议面契约; TOUCH 是**"查询即 LRU"的显式面** (配合淘汰策略)。[模式: 语义契约]
数据流: TTL key → 存在? → 有 TTL? → 剩余 = 到期戳 − 冻结时间 → 四舍五入。

### 负面空间 — 过期机制刻意不做的事

- **不做实时时钟同步**: 传播用绝对时间戳 (PEXPIREAT), 不依赖主从时钟一致
- **不做精确到期扫描**: 主动过期是抽样 + 预算制 (20 键/轮), 到期删除不保证即时 (惰性面保证正确性)
- **不做过期键永久驻留**: 最坏情况 (键永不访问) 由主动过期兜底, 但延迟不确定
- **不做从库主动删主库键**: 从库只等 DEL (除可写自产键记账)
- **不做 DB>63 的从库记账**: 位图上限显式折衷

→ 引出: 内存不够时谁先被淘汰?→ [[R-23-evict]]
