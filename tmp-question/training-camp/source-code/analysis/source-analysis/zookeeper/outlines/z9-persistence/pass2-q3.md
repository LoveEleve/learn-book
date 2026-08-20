# 闭环笔记 q3: restore 双路径 — 快照 + 事务重放 + 空库边界

## 假设
恢复 = 快照打底 + 日志重放到最新; 边界面 (无快照/空库/截断) 有明确语义。

## 验证过程
- **主路径** (FileTxnSnapLog:252-313): FileSnap.deserialize → **fastForwardFromEdits: txnLog.read(dt.lastProcessedZxid + 1)** (L330) — **严格从快照 zxid+1 重放** (fuzzy 快照语义: 快照可能已含到 X 的事务); 每事务 processTransaction + **compareDigest 校验** (L351)
- **目录布局**: dataDir = \<dir\>/version-2, snapDir = \<dir\>/version-2 (L112-117, VERSION=2); **交叉污染检查** (L180-204, ZOOKEEPER-2967): dataLogDir 有 snapshot / snapDir 有 log → 拒绝启动
- **无快照面** (L283-310): 无快照有日志 → **默认 throw "Something is broken!" (ZOOKEEPER-2325)**; **trustEmptySnapshot=true 时放行 (ZOOKEEPER-3056 3.4.x 升级逃生舱)**; 空库 → trustEmptyDB (initialize 文件 / zookeeper.db.autocreate) → **save 空快照 → return 0**
- **zxid 回退检测** (L344-347): hdr.zxid < highestZxid → ERROR log — 日志乱序面
- **digest 覆盖检查** (L272-279): 重放后 snapshotZxidDigest 未复位 → **"might lead to inconsistent state" 警告** (重放未覆盖快照 zxid)
- **truncate** (FileTxnLog:481-501): iterator 定位 → **setLength(pos) 截断到 < zxid (exclusive)** → 删除后续文件; 触发面 (LearnerHandler:842-846): learner 领先 → **TRUNC zxid = maxCommittedLog**
- **truncateLog 重开** (FileTxnSnapLog:513-535): close → truncate → 重建 txnLog/snapLog 实例 (引用共享面注释 L523-525)

## 代码类型
Architecture (恢复流程)

## 跨域关联
- Z-2: TRUNC/DIFF/SNAP 五分支同步 (LearnerHandler:773-895)
- Z-3: processTxn 重放 + digest
- Z-5: sessions 重建 (createSession/closeSession 重放)

## 结论
恢复 = 快照(≤100 回退) + zxid+1 重放 + digest 校验; 空库/无快照边界有逃生舱语义; truncate 为 exclusive 边界。
源码位置: FileTxnSnapLog.java:112-204,252-377,474-535; FileTxnLog.java:369-388,481-501; LearnerHandler.java:842-846
