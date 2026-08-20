# 闭环笔记 q1: 双表渐进 rehash 状态机

## 假设
rehash 时 ht[0] 和 ht[1] 并存, rehashidx 是逐桶迁移游标; 迁移期间所有操作 (查找/添加/删除) 双表兼容; 完成时释放旧表并切换。

## 验证过程
- dict.h:96-102: `dictEntry **ht_table[2]; unsigned long ht_used[2]; long rehashidx;` + `#define dictIsRehashing(d) ((d)->rehashidx != -1)`
- dictRehash (dict.c:385-414): `while(n-- && d->ht_used[0] != 0)` — 每步迁一个桶; 空桶扫描上限 `empty_visits = n*10` (注释 L376-384: "it will visit at max N*10 empty buckets in total, otherwise the amount of work it does would be unbound and the function may block for a long time")
- rehashEntriesInBucketAtIndex (L312-359): 整桶链迁移 + `ht_used[0]--; ht_used[1]++` 计数
- dictCheckRehashingCompleted (L362-374): `ht_used[0]==0` → zfree(ht_table[0]) → **ht[1] 提升为 ht[0]** (指针交换) → _dictReset(1) → rehashidx=-1 → rehashingCompleted 回调
- 双表兼容: dictFind 双表循环 (L762-769); dictAddRaw 定位 (L500+) 按 rehash 状态选表
- 三触发面: _dictRehashStep (操作联动 1 步, L448) / dictRehashMicroseconds (serverCron 限时, L426) / dictRehash(d,100) 批量

## 代码类型
Algorithmic (状态机 + 增量迁移)

## 跨域关联
- R-20 (serverCron → dictRehashMicroseconds) → 限时迁移
- R-21 (db 键空间 dict) → 主消费方
- R-33 (zfree 旧表) → 分配面

## 结论
渐进 rehash = 双表 + 逐桶游标: 迁移被摊到每次操作 (1 步) + 周期任务 (限时 100 步/ms), 空桶扫描有 n*10 上限防长时间阻塞; 完成时旧表释放、新表提升、游标复位。迁移期间读写全兼容 — 无锁单线程下"无感知"迁移。
源码位置: dict.h:96-102,170; dict.c:312-414
