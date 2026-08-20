# 闭环笔记 q3: HFE 主动过期 — 配额与序列放大

## 假设
HFE 字段过期走独立循环, 配额 = 10000 字段/秒均摊到 hz, 清不完则序列放大 (×32 封顶)。

## 验证过程
- activeExpireHashFieldCycle (expire.c:144-185):
  - 静态态: currentDb (跨调用) + activeExpirySequence (L146-152)
  - 空表推进: ebIsEmpty(db->hexpires) → sequence=0, currentDb 轮转 (L159-163)
  - **maxToExpire = HFE_DB_BASE_ACTIVE_EXPIRE_FIELDS_PER_SEC / server.hz** (L166) = 10000/hz (hz=10 → 1000 字段/次)
  - **序列放大** (L170-174): activeExpirySequence > EXPIRED_FIELDS_TH=1000000 (L154) 且 SLOW → `factor = sequence/1000000; maxToExpire *= factor<32 ? factor : 32` (L173-174)
  - 判定 (L177-184): `hashTypeDbActiveExpire(db, maxToExpire) == maxToExpire` → 达配额没清完 → sequence += maxToExpire; 否则清完 → sequence=0 + 下一 DB
- 消费链: hashTypeDbActiveExpire (t_hash.c:2073-2094) → ebExpire(db->hexpires, hashExpireBucketsType) → 回调 hashTypeActiveExpire: 删字段/更新次早字段/空 hash 摘除
- 交错执行 (expire.c:288): 每 DB 先 HFE 后键过期 — 注释 "HFE DS is optimized for active expiration"
- 从库: 只读从库不执行 (databasesCron iAmMaster 分支, server.c:1059); HFE 与键过期同面

## 代码类型
Mechanism (配额自适应)

## 跨域关联
- R-21 (ebExpire/hexpires/hashExpireBucketsType) / R-25 (hashTypeDbActiveExpire)

## 结论
HFE 主动过期 = 独立配额循环: 1000 字段/次 (hz=10) 为基线, 连续清不完 (累积 >100 万) 则按倍数放大至多 32× — 预算随积压自适应。与键过期交错但独立游标 (currentDb)。
源码位置: expire.c:98,144-185; t_hash.c:2073-2094
