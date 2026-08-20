# 闭环笔记 q6: ebuckets 时间桶 + HFE 两级注册

## 假设
ebuckets = 按到期时间分组的批量过期结构 (list→rax→segment 三级); HFE 全局表只挂"最早到期字段的 hash"。

## 验证过程
- 常量 (ebuckets.c:50-63): EB_KEY_SIZE=6 (PRECISION<8 → 6B bucketKey, rax 深度 ≤6); **EB_SEG_MAX_ITEMS=16 = EB_LIST_MAX_ITEMS**; EB_BUCKET_KEY_PRECISION=0 (ebuckets.h:142, "TBD: modify to 10")
- ExpireMeta (ebuckets.h:161-211): 48bit 到期时间 (Lo32+Hi16) + 位域 (lastInSegment/firstItemBucket/lastItemBucket/numItems:5/trash/userData:3) + next 指针 — 内嵌进 item
- **list/rax 判别** (ebuckets.c:146-165): eb 指针 LSB — ebIsList 用 &0x1; ebMarkAsList 置位; itemsAddrAreOdd=0 时指针恢复 &~1 (hashExpireBucketsType=0, hashFieldExpireBucketsType=1 — t_hash.c:115-129)
- list 路径 (ebAddToList L561-620): 空→头; 满 16 (numItems==16)→返回 1 触发转换; 按 TTL 升序插入 (头/中/尾)
- rax 路径 (ebAddToRax L1123 / ebConvertListToRax L528-548): bucketKey 大端 6B 作 rax 键; FirstSegHdr (head/totalItems/numSegs) + NextSegHdr (prevSeg/firstSeg)
- **segment 满则分裂** (ebTrySegSplit L286-330): 找最佳中分点 (同 TTL 不可劈 — 扩展段 ebSegAddExtended L201-233)
- ebAdd (L1424-1453): 时间戳→EB_BUCKET_KEY→list 或 rax; 断言 expireTime ≤ EB_EXPIRE_TIME_MAX (0x0000FFFFFFFFFFFF, ebuckets.h:148)
- ebExpire (L1464-1549): list: 从头删到 >now (ebListExpire L666); rax: 逐桶删到 bucketKey ≥ nowKey (L1508); ACT_UPDATE_EXP_ITEM → updateList 尾部统一重插; 全空 → raxFree 回 NULL (L1525-1528)
- ebExpireDryRun (L1561-1648): 只数不删 — 跨桶用 totalItems 累计 + 末桶逐项 (预算预判); ebGetNextTimeToExpire (L1663): 最小桶头 TTL
- **HFE 两级** (t_hash.c:110-130): db->hexpires 全局表 (hash 对象, TTL=最早字段) + 每 hash 内 hfe (字段级); 消费: hashTypeDbActiveExpire (t_hash.c:2080-2094: ebExpire(全局表) → 回调 hashTypeActiveExpire 处理字段/更新/摘除)
  - 注册点: db.c:1446 (RENAME) / db.c:1529 (MOVE) / db.c:1643 (COPY) / cluster.c:248 (迁移)
  - 摘除点: dbSetValue L282 / dbGenericDelete L382 (hash 类型旧值)
  - R-22 边界: activeExpireCycle → hashTypeDbActiveExpire (expire.c:159 先 ebIsEmpty 判空; HFE_DB_BASE_ACTIVE_EXPIRE_FIELDS_PER_SEC=10000, expire.c:98)

## 代码类型
Mechanism (时间分桶摊销)

## 跨域关联
- R-22 (主动过期消费) / R-25 (hash 字段过期) / R-3 (keys_are_odd 同族技巧: 指针位复用)

## 结论
ebuckets 的卖点: 删一个桶 = 删一批 (摊销 O(1)); segment 聚合避免 rax 每项一叶子 (~40B/项); list 小规模零开销 (ebCreate 返回 NULL)。PRECISION=0 显式 TBD — 主动过期精度待调, 惰性面不受影响 (ExpireMeta 精确到 msec)。HFE 全局表是"最早到期者代理", 让 R-22 一次 ebExpire 触达所有有到期字段的 hash。
源码位置: ebuckets.c:528-660,1424-1663; ebuckets.h:142-211; t_hash.c:110-130,2080-2094
