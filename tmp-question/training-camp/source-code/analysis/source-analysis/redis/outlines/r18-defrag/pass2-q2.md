# 闭环笔记 q2: 渐进扫描 — dictScanDefrag 与桶内指针替换

## 假设
dictScanDefrag = R-3 dictScan 反向游标变体 + 桶内就地指针替换 (defragfns 三回调)。

## 验证过程
- **签名** (dict.h:142,250): dictDefragFunctions{defragAlloc/defragKey/defragVal} + dictScanDefrag(d, v, fn, defragfns, privdata)
- **游标语义** (dict.c:1385-1470): 与 dictScan 同款 — 非 rehash: 单表 `v & m0` 桶 + `v |= ~m0; rev; v++; rev` 反向增量 (L1405-1414); rehash: 小表桶 + `do { 大表扩张桶 } while (v & (m0^m1))` 双表迭代 (L1416-1463)
- **dictPauseRehashing** (L1394): 扫描期间冻结 rehash — "needed in case the scan callback tries to do dictFind or alike" (键名搬移后要按哈希找 expires 表)
- **dictDefragBucket** (L1217-1252) — R-3 单指针 entry 编码三态:
  - **entryIsKey** (storedKey, R-3): key 即 entry → defragKey 返回值替换 bucketref (L1232-1235)
  - **entryIsNoValue**: 解包 encodeMaskedPtr → defragalloc 搬 → 重编码 ENTRY_PTR_NO_VALUE (L1236-1241)
  - **entryIsNormal**: defragalloc(de) 搬 entry + key/val 就地替换 (L1242-1249)
  - 沿 `dictGetNextRef` 链式推进 (L1251) — 注意 bucketref 指向 next 指针的引用, 链可原地改写
- **消费方**:
  - activeDefragSdsDict (defrag.c:295-310): val_type 5 档 (NO_VAL/SDS/STROB/VOID_PTR/LUA_SCRIPT) 选 defragVal
  - activeDefragHfieldDict (L313-324): defragKey=NULL (hfield 由 callback 搬 — 需 ebucket 引用更新, q3)
  - scanLaterSet/Zset/Hash (L446-484): 大键续扫复用
- **kvstore 面**: kvstoreDictScanDefrag 跨 slot 版 + kvstoreDictLUTDefrag (kvstore.c:778 — LUT 数组里每个 dict 结构/表搬移, 四阶段用)
- **callback 职责**: defragScanCallback (L825-833) 统计 key_hits/key_misses/scanned — "key 级"统计 (hits 前后对比)

## 代码类型
Algorithmic (反向游标扫描 + 桶内替换)

## 跨域关联
- R-3 (dict): 反向游标/单指针 entry/rehash 双表 — 本域全部复用
- R-21 (db): kvstore 四阶段 (keys/expires)

## 结论
dictScanDefrag = dictScan 的"可搬移"变体: 同游标算法 + dictPauseRehashing + dictDefragBucket 三态就地替换; defragfns 三回调 (Alloc=entry 本身/Key=storedKey/Val=值指针) 参数化; kvstore 面 LUT 搬移 + 跨 slot 扫描。
源码位置: dict.c:1217-1252,1385-1470; dict.h:135-142,250; defrag.c:295-324,446-484,825-833; kvstore.c:778
