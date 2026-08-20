# 闭环笔记 q7: lazyfree — 阈值异步释放

## 假设
异步释放 = "工作量 (分配数) 阈值 + 换表法" 双路径; effort 按编码估算而非字节。

## 验证过程
- LAZYFREE_THRESHOLD = 64 (lazyfree.c:181, 注释: "few allocations... actually just slower")
- freeObjAsync (L184-196): lazyfreeGetFreeEffort > 64 **且 obj->refcount == 1** → bioCreateLazyFreeJob (异步 decrRefCount); 否则同步 decrRefCount
- lazyfreeGetFreeEffort (L129-174): 按编码估算 — quicklist → ql->len (节点数); dict → dictSize; skiplist → zsl->length; stream → rax 宏节点 + cgroups×PEL; module → moduleGetFreeEffort (0 → ULONG_MAX 强制异步); 字符串等 → 1
- 三个异步删除面:
  - 单键: dbGenericDelete async 分支 (db.c:394-397: freeObjAsync + 先置 NULL 防模块双删)
  - 全库: emptyDbAsync (lazyfree.c:201-215) — **换表法**: 新建空 keys/expires/hexpires 顶替 → 旧表交 lazyfreeFreeDatabase (L23-41: ebDestroy + kvstoreRelease×2 + **jemalloc purge + tcache flush**)
  - 其他: tracking rax / errors / lua_scripts / functions ctx / repl backlog (L219-274)
- 统计面: lazyfree_objects/lazyfreed_objects 原子计数 (L8-9, 13-17) — INFO 可见
- 消费线程: bio.c (BIO_WORKER_LAZY_FREE), R-33 分配计数实证过 atomicvar
- 三态删除入口回顾: dbSyncDelete (db.c:411) / dbAsyncDelete (L417) / dbDelete (L423, 配置驱动) / dbGenericDelete (L372)

## 代码类型
Mechanism (阈值异步 + 换表)

## 跨域关联
- R-33 (bio 线程分配/atomicvar) / R-20 (lazyfree 配置面) / R-3 (dict 释放) / R-25 (hash 摘除)

## 结论
阈值按"分配数"而非字节 (释放成本 ∝ 分配数, 与 allocator 行为一致); refcount==1 守卫防共享对象误异步。换表法让 FLUSHDB ASYNC 变 O(1) 主线程 + 后台释放。
源码位置: lazyfree.c:129-215; db.c:372-425
