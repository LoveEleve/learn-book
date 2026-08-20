# R-22 过期机制 — Pass 1 探索笔记

> 域: R-22 过期机制 (expire.c) | 🟡 B 方案 | 2026-08-13
> 源码: src/expire.c (838) | Redis 7.4.2

## 调用图

```
主动面:
databasesCron (server.c:1059, iAmMaster 分支) → activeExpireCycle(SLOW)
beforeSleep (server.c:1689)                        → activeExpireCycle(FAST)
  activeExpireCycle (L187):
    - 常量: KEYS_PER_LOOP=20 (L92) / FAST_DURATION=1000us (L93) / SLOW_TIME_PERC=25% (L94) / ACCEPTABLE_STALE=10% (L95)
    - effort 缩放 (L191-200): effort=active_expire_effort-1 (0..9, 配置 1..10 config.c:3177)
    - 全局态: current_db / timelimit_exit / last_fast_cycle (L204-206)
    - FAST 前置 (L218-231): 上次超时 或 stale>acceptable; 2×duration 防重
    - timelimit: SLOW = 25%×1000000/hz/100 (L247); FAST = 1000us (L252)
    - 每 DB: activeExpireHashFieldCycle (L288, HFE 先) → do-while:
        num=min(expires 大小, keys_per_loop) (L313-314)
        max_buckets = num*20 (L326)
        kvstoreScan(expires, expires_cursor, ...) (L332) — 游标跨调用持久
        repeat = expired*100/sampled > acceptable_stale (L348)
        avg_ttl 更新 (L353-382, pow(0.98) 表加速)
        每 16 迭代查时间 (L383-390)
    - stale_perc 运行平均 5%/95% (L399-407)
  activeExpireHashFieldCycle (L144):
    - maxToExpire = 10000/hz (L166); 累积 >100 万 (L154) → ×1-32 (L170-174)
    - hashTypeDbActiveExpire (t_hash.c:2073) → ebExpire(db->hexpires)

从库面:
databasesCron (server.c:1061, 从库分支) → expireSlaveKeys (L449)
  - slaveKeysWithExpire dict (L445): 键 → dbid 位图 (uint64, >63 不跟踪 L524)
  - 循环: 随机键 → 位图逐 DB → activeExpireCycleTryExpire (L37) → 位图更新/删
  - 停止: noexpire>3 (L503) / 64 循环且 >1ms (L504) / 表空
  - rememberSlaveKeyWithExpire (L511) — setExpire 调 (db.c:1860-1862)

惰性面 (R-21 已交付):
lookupKey → expireIfNeeded (db.c:1974) → deleteExpiredKeyAndPropagate (db.c:1877)

命令面:
expireGenericCommand (L635): 单位/溢出 (L651-663) → NX/XX/GT/LT (L671-713) → checkAlreadyExpired (L562, L715)
  → 已过期: dbGenericDelete + 重写 DEL/UNLINK (L718-728)
  → 未过期: setExpire + 重写 PEXPIREAT (L730-748) + notify "expire"
ttlGenericCommand (L773): -2 不存在 / -1 无 TTL / 剩余 (L786-793, 四舍五入)
persistCommand (L817) / touchCommand (L833)

时间源:
commandTimeSnapshot (server.c:221): cmd_time_snapshot — 命令期间时间冻结 (脚本一致, #1525)
```

## 基本元素分解

1. **SLOW/FAST 双循环**: 25% CPU 预算 (slow) vs 1000us 高频快速 (fast) — 触发条件差异
2. **采样驱动**: kvstoreScan(expires) 游标持久 + num×20 桶上限 + repeat 判定 (stale 比例)
3. **effort 自适应**: 1-10 配置缩放全部基线参数
4. **统计面**: avg_ttl (pow 0.98 指数滑动) + stat_expired_stale_perc (5/95 滑动)
5. **HFE 主动面**: 10000 字段/秒配额 + 序列放大 (×32 封顶) + DB 轮转
6. **从库记账**: slaveKeysWithExpire 位图 — 可写从库自产键的过期回收
7. **命令面**: EXPIRE 族 (NX/XX/GT/LT) / TTL 族 / PERSIST / 已过期直接 DEL 重写
8. **时间一致**: commandTimeSnapshot 命令期冻结 (脚本/传播一致)

## 标记问题 (8 个)

1. SLOW/FAST 的触发与预算差异?FAST 为什么可能完全拒绝运行?
2. expires_cursor 怎么跨调用持续?采样上限 (20 键/20×桶)?
3. repeat 判定为什么用"过期比例"而非"还有过期键"?
4. avg_ttl 的 pow(0.98) 表加速怎么推的?为什么 2%/98% 权重?
5. HFE 配额 10000/秒怎么算?序列放大逻辑?
6. 可写从库为什么需要独立记账 (位图)?主库键为什么不记?
7. EXPIRE 已过期为什么重写为 DEL?为什么 PEXPIREAT 统一传播格式?
8. TTL 四舍五入 (ttl+500)/1000 与 -1/-2 语义?

## 时空溯源 (代码内痕迹)

- 2009 (antirez): expire.c 初版 — activeExpireCycle 骨架 (版权 2009-Present)
- 3.2: writable slave 过期记账 (注释 L430: "implemented in 3.2" 前的泄漏问题)
- 4.0: effort 配置引入 (active_expire_effort) — 注释 "The configured expire effort"
- 5.x: expires_cursor 每 DB 持久游标 (server.h:980)
- 7.x: HFE 字段级过期 (activeExpireHashFieldCycle, 2024) — "HFE DS is optimized for active expiration"
- 7.x: kvstore 分片后扫描走 kvstoreScan (R-21 连接)
- 演进: avg_ttl 从循环 (L366-368 注释的旧代码) → pow(0.98) 常数表 (几何级数求和公式)

## 大域拆分判断

838 行单文件 — **不拆** (🟡 B, 6 闭环足够, 对比 r7-intset 560 行单篇)。
