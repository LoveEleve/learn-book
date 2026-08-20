# 闭环笔记 q8: 全局注册与迁移 — trash 与 minExpire

## 假设
全局 HFE 注册 = 早到字段代理 (hash 对象挂 db->hexpires); 转换期 trash 标记处理中间态。

## 验证过程
- hashExpireBucketsType (t_hash.c:115-122): getExpireMeta=hashGetExpireMeta / **itemsAddrAreOdd=0** (hash 对象地址偶)
- hashFieldExpireBucketsType (L124-130): hfieldGetExpireMeta / **itemsAddrAreOdd=1** (mstr 奇数地址 — R-3 keys_are_odd 同族)
- **hashTypeAddToExpires** (L2040-2060): 全局注册 — LISTPACK_EX (lpt->key 引用 + ebAdd) / HT (dictExpireMetadata.key + ebAdd); 唯一入口 (R-21 已确认)
- **hashTypeRemoveFromExpires** (L1996-2013): 全局摘除 — 返回 minExpire (RENAME/MOVE/COPY 场景, R-21 db.c:1446)
- hashTypeGetMinExpire (L1952-1995): 私有 hfe 最小到期 (accurate 变体)
- hashTypeIsFieldsWithExpire (L2014-2039): 是否有 TTL 字段 (遍历)
- **trash 机制** (L1634, 转换): dictExpireMetadata.expireMeta.trash=1 — 转换期 hash 尚未注册全局 (ebAdd 后清 trash)
- hashTypeDbActiveExpire (L2073-2094): R-22 消费 — ebExpire(全局表) → onFieldExpire 回调 (L2940-2952: ACT_REMOVE/UPDATE/STOP)
- hashTypeExpire (L1853-1921): 本地 ebExpire 包装 (hash 私有 hfe)
- 空 hash 处理: hashTypeLength(o,0)==0 → dbDelete (L770-775, 惰性链)

## 代码类型
Mechanism (两级注册)

## 跨域关联
- R-21 (hexpires/ebuckets) / R-22 (activeExpireHashFieldCycle) / R-3 (mstr 奇数地址)

## 结论
两级 HFE = 全局代理 (hash 级, 早到字段) + 私有明细 (字段级)。itemsAddrAreOdd 双态 (0/1) 支撑 ebuckets 指针判别。trash 标记是转换期的"未注册"状态机。RENAME/MOVE/COPY 经 RemoveFromExpires 返回 minExpire 保续 (R-21 已交付)。
源码位置: t_hash.c:115-130,1853-2094,2940-2952
