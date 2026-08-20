# 闭环笔记 q2: Snapshot 格式 — seal 三重校验 + 快照回退

## 假设
快照 = 文件头 + 树序列化 + CRC seal 段; 损坏快照可回退到更旧有效快照。

## 验证过程
- **文件头** (FileSnap:136-142): magic "ZKSN" (SNAP_MAGIC L53) + version 4B (VERSION=2 L50) + **dbid=-1 常量** (L51 — 与 txnlog 动态 dbid 不对称)
- **seal 段** (SnapStream:162-180): **writeLong(Adler32 校验和) + writeString("/")** — "/" 是结束标记; checkSealIntegrity 比对 → "CRC corruption"; **写三段**: 树 + zxidDigest + lastProcessedZxid (FileSnap:250-267, 每段独立 seal — 向后兼容: 老版本无 digest/lastProcessedZxid 段)
- **有效快照判定** (SnapStream:193-211,298-327): CHECKED 模式读尾部 5 字节 — **len=1 + '/'** — 快照至少 10 字节; GZIP/SNAPPY 看 magic
- **回退面** (FileSnap:73-126): **findNValidSnapshots(100)** 最多回退 100 个 (L77); 最新在前 (sortDataDir descending); deserialize 失败 → 试下一个 → 全失败 throw "Not able to find valid snapshots"
- **原子写** (SnapStream:132-154): **fsync=true → AtomicFileOutputStream (临时文件+rename)**, 否则直接 FileOutputStream — save(syncSnap) 由调用方决定
- **空快照文件删除** (FileTxnSnapLog:485-495): 写失败且文件为空 → delete (full disk 循环防护)
- **fuzzy 快照容错** (FileTxnSnapLog:445-453): 快照进行中后期事务混入 → restore 时 NONODE/NODEEXISTS 安全忽略

## 代码类型
Implementation (快照格式)

## 跨域关联
- Z-3: DataTree serialize/deserialize (DFS + "/" 结束标记) + serializeZxidDigest/lastProcessedZxid
- Z-4: snapCount 触发快照 (SyncRequestProcessor:146-151)
- Z-5: sessions 映射序列化 (dumpSessions 恢复面)

## 结论
快照 = [ZKSN|ver2|-1] + 树 + 三段独立 CRC seal ("/"), 最多回退 100 个; fsync 时原子替换。
源码位置: FileSnap.java:50-126,242-276; SnapStream.java:102-180,193-327; FileTxnSnapLog.java:445-453,474-504
