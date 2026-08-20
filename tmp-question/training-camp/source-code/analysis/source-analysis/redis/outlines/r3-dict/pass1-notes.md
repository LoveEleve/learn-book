# R-3 Dict — Pass 1 探索笔记

> 域: R-3 Dict (哈希表) | 🔴 方案 A | 2026-08-13
> 源码: src/dict.c (2056) + dict.h (264) | Redis 7.4.2
> 已读测试: dictTest (dict.c:1841+, REDIS_TEST): Add/Resize/AVOID/force ratio/shrink 断言族

## 继承树/调用图

```
struct dict (dict.h:94-111):
  dictType *type (函数指针族)
  dictEntry **ht_table[2]  ← 双表
  unsigned long ht_used[2]
  long rehashidx  (渐进迁移游标, -1 = 不在 rehash)
  pauserehash:15 / useStoredKeyApi:1 / ht_size_exp[2] (7.x 指数表) / pauseAutoResize:16
  metadata[]

dictType (dict.h:32-92):
  hashFunction/keyDup/valDup/keyCompare/keyDestructor/valDestructor/expandAllowed
  + no_value:1 / keys_are_odd:1 (7.x 单指针优化) / storedHashFunction/storedKeyCompare (存储键 API)
  + rehashingCompleted 回调 / onDictRelease / dictMetadataBytes

dictEntry (opaque, 联合值):
  key / next + v {val, s64, u64, double} — 整数值直接存 (免分配)
  entryIsKey/entryIsNoValue/entryHasValue 编码态 (7.x 单指针优化)

核心操作:
  dictAdd → dictAddRaw → dictFindPositionForInsert (L500-511)
  dictFind (L736-770): rehash 联动 (缓存友好 bucket rehash L747-757) → 双表查找
  dictExpand (L290) / dictShrink (L302) / _dictResize (L224)
  dictRehash (L385-414): n 步 × 每步一桶 + empty_visits=n*10 空桶上限
  rehashEntriesInBucketAtIndex (L312-359): 桶迁移; 扩=重算 hash / 缩=idx&掩码 (2 幂!)
  dictCheckRehashingCompleted (L362-374): ht[0] 空 → 释放 → ht[1] 提升 → rehashidx=-1
  _dictRehashStep (L448): 查找/添加时 1 步
  dictRehashMicroseconds (L426): serverCron 限时 rehash (R-20 连接)
  dictScan (L1369+): 反向二进制游标 (rev+1+rev), rehash 期间不丢 (重复/中途新增可漏)

触发阈值 (dict.c:1492-1550):
  _dictExpandIfNeeded → dictExpandIfNeeded: 空表→4 桶; 1:1 扩 (ENABLE) / AVOID 时 4:1
  dictShrinkIfNeeded: 初始 4 桶不缩; <1:8 缩 (ENABLE) / <1:32 (AVOID)
  全局三态: DICT_RESIZE_ENABLE/AVOID/FORBID (dict_can_resize, L41)
```

## 基本元素分解

1. **双表渐进**: ht[0]→ht[1] 逐桶迁移, rehashidx 游标, 迁移期间双表并存 (查找/添加/删除全兼容)
2. **触发阈值**: 1:1 扩 / 4:1 强制 / 1:8 缩 / 1:32 强制缩 — 与全局三态 (ENABLE/AVOID/FORBID) 联动
3. **2 幂表掩码**: 扩缩容 idx = h & mask — 缩容直接掩码复用 (不重算哈希)
4. **rehash 联动**: 查找路径顺带迁移 (缓存友好) + serverCron 限时迁移 (不阻塞)
5. **dictType 定制**: 函数指针族 (哈希/比较/复制/释放) — 键空间/过期表/命令表同构异制
6. **entry 联合值**: val/s64/u64/double — 整数直接存
7. **7.x 单指针优化**: no_value + keys_are_odd — set 字典的 key 指针直存
8. **dictScan 反向游标**: rehash 安全遍历

## 标记问题 (9 个)

1. 双表渐进 rehash 的状态机 — rehashidx 怎么推进?完成怎么检测?迁移期间读写怎么兼容双表?
2. 触发阈值体系 (1:1/4:1/1:8/1:32) — 为什么这些比例?can_resize 三态谁设置? (RDB 加载?)
3. dictFind 的 rehash 联动 — 为什么查找时触发 bucket 迁移? "缓存友好" vs rehashidx 步进?
4. 2 幂表掩码优化 — 为什么缩容不用重算哈希?什么时候不成立?
5. no_value/keys_are_odd 单指针优化 — 编码 (encodeMaskedPtr)? 谁用 (set)?
6. dictScan 反向游标 — rehash 期间为什么遍历不丢 (重复/中途新增可漏)?
7. dictType 定制族 — sds 键怎么配? 整数联合值怎么省分配?
8. SipHash + seed — 防碰撞攻击?
9. pauserehash/pauseAutoResize — 什么时候暂停 (iterator/加载)?

## 时空溯源 (代码内痕迹)

- dict.c 头部注释 (L22-40): 渐进 rehash 的历史演进说明 — 早期双桶扫描 vs 现代 bucket 迁移
- 7.x 重构: ht_size_exp (指数) 替代 size 字段; no_value/keys_are_odd 单指针 entry; storedKey API (L60-90 长注释); rehashingCompleted 回调
- DICT_HT_INITIAL_EXP=2 (初始 4 桶, 历史沿用)
- dictRehashMicroseconds: 早期 serverCron 的 1ms 限时迁移演进
