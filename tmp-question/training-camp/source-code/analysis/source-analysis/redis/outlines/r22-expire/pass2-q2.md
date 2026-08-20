# 闭环笔记 q2: 采样驱动 — 游标/上限/repeat/统计

## 假设
主动过期 = 抽样扫描 expires 表: 每 DB 20 键/次, 桶上限 20×, 过期比例决定是否重复。

## 验证过程
- num 上限 (L313-314): `if (num > config_keys_per_loop) num = config_keys_per_loop;` — 每轮最多 20 键
- 桶上限 (L326): `long max_buckets = num*20;` — 稀疏表最多扫 400 桶
- 主循环 (L331-338): `while (data.sampled < num && checked_buckets < max_buckets)` — kvstoreScan(expires, expires_cursor, -1, expireScanCallback, isExpiryDictValidForSamplingCb, &data) (L332)
  - **expires_cursor 持久游标** (server.h:980): db->expires_cursor 跨 activeExpireCycle 调用保存 — 下次从停处继续
  - 游标回 0 → db_done (L333-335)
- skip 回调 (L127-137): **桶填充率 <1% 跳过采样** (`buckets > DICT_HT_INITIAL_SIZE && numkeys*100/buckets < 1`) — 稀疏表扫描不划算, 等 rehash 缩容
- **repeat 判定** (L348): `repeat = db_done ? 0 : (data.sampled == 0 || (data.expired*100/data.sampled) > config_cycle_acceptable_stale)` — 过期比例 >10% (effort 可调) 才继续同一 DB
- expireScanCallback (L110-125): 逐键 activeExpireCycleTryExpire (L37: now>ttl → deleteExpiredKeyAndPropagate + enter/exitExecutionUnit) + ttl_sum 收集 (未过期键)
- avg_ttl 更新 (L353-382): 每 16 迭代或退出时 — 指数滑动: `db->avg_ttl = avg_ttl + (db->avg_ttl - avg_ttl)*avg_ttl_factor[...]` — **注意代码变量遮蔽**: 右值第一个 avg_ttl 是局部采样均值, (db->avg_ttl - avg_ttl) 是旧值减新均值 — 即 `新 = 均值 + (旧 − 均值)×pow(0.98, n)`; pow(0.98, n) 常数表 (L24), 旧代码注释 (L366-368: 循环 49/50+1/50) → 几何级数求和闭式
- stale_perc (L399-407): `current_perc*0.05 + old*0.95` — 5%/95% 运行平均 (FAST 触发依据)
- 统计: stat_expiredkeys (activeExpireCycleTryExpire 内 db.c 递增) / stat_expire_cycle_time_used / latency "expire-cycle"

## 代码类型
Mechanism (抽样驱动 + 统计反馈)

## 跨域关联
- R-21 (kvstoreScan/expires_cursor/deleteExpiredKeyAndPropagate) / R-20 (databasesCron)

## 结论
主动过期 = "20 键/20×桶" 抽样 + 持久游标 + 过期比例反馈 (repeat) + 填充率门槛 (skip)。统计双滑动平均: avg_ttl (98% 指数, 表加速) 与 stale_perc (95% 平滑) — 前者是 INFO 指标, 后者是 FAST 触发信号。
源码位置: expire.c:22-24,37-50,110-137,297-407
