# 闭环笔记 q5: 脏传播 — signalModifiedKey 与全库失效

## 假设
键修改统一入口 signalModifiedKey → touchWatchedKey; FLUSHDB/SWAPDB/SELECT 全量失效。

## 验证过程
- signalModifiedKey (db.c:620-623): **touchWatchedKey(db,key) + trackingInvalidateKey(c,key,1)** — 一条路径同时服务 WATCH 与客户端缓存 (R-17 未来域), 'c' 可为 NULL (无客户端上下文, L617-619 注释)
- 调用面 (grep 实证): db.c 内部 + 各命令写入路径 (setKey/dbAdd/dbDelete 等) — WATCH 失效覆盖面 = 所有修改键的命令
- touchAllWatchedKeysInDb (multi.c:407-450):
  - 触发点: **FLUSHDB/FLUSHALL** (db.c:637, signalFlushedDb) / **SWAPDB** (db.c:1721-1722 双向) / **SELECT** (db.c:1767, 切库空键空间) / 无盘复制结束
  - 语义 (L400-406 注释): FLUSHDB/SWAPDB/diskless 复制后全 db 失效; replaced_with 用于 SWAPDB 双库存在性判断
  - **expired 位三态处理** (L426-440): 已过期键删除无变化 → 清标志; 替换键仍过期 → 保持; 不存在键换成过期键 → 置 expired
  - **迭代中不能 unwatch** (L443-445 注释): 会 free 迭代器持有的下一节点 → **use-after-free** — 只置标志不摘除 (与 touchWatchedKey 单键路径不同!)
- isWatchedKeyExpired (L342-355): EXEC 前检查 — WATCH 时未过期但 EXEC 时已过期的键 → DIRTY_CAS (L350-351); 已经过期标志的跳过 (L350)

## 代码类型
Mechanism (失效传播)

## 跨域关联
- R-21: 键空间修改路径 (dbAdd/dbDelete/setKey)
- R-22: keyIsExpired (惰性过期)
- R-17 (未来): trackingInvalidateKey 双失效
- R-15 (未来): SWAPDB/SELECT 语义

## 结论
修改键 → signalModifiedKey 统一打点 → WATCH 失效; 批量场景 (FLUSH/SWAP) 全量失效但**不可迭代中退订 (UAF 防护)**; expired 位贯穿 WATCH/EXEC 生命周期。
源码位置: db.c:620-623,626-637,1721-1722,1767; multi.c:342-355,407-450
