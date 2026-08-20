# 闭环笔记 q1: TxnLog 文件格式 — CRC+len+payload+0x42 记录链

## 假设
事务日志 = 文件头 + 定长记录链 + 预分配填充; CRC 保护记录完整性。

## 验证过程
- **FileHeader** (Javadoc L60-96): magic "ZKLG" 4B (TXNLOG_MAGIC L102) + version 4B (VERSION=2 L104) + dbid 8B — 文件创建时写入 (append L297-299)
- **记录格式**: **[CRC 8B Adler32][len 4B][TxnHeader+Record(+digest) payload][0x42 'B' 结束标记]** (L315-319 + Util.writeTxnBytes L205-208)
- **CRC 覆盖实证**: `crc.update(buf)` 只覆盖 payload (L316-317), 先写 CRC 后写 len 与 0x42 — **Javadoc L77-78 "calculated across payload -- Txnlen, TxnHeader, Record and 0x42" 与实现不符** (表述精确化发现)
- **预分配**: FilePadding preAllocSize 默认 **65536KB=64MB** (FilePadding:30) — position+4096 ≥ fileSize 才补 pad (L101-115); 尾部 ZeroPad 读到即 EOF (Util.readTxnBytes L157-173: 空 buffer = EOF)
- **幂等守卫**: append 中 zxid ≤ lastZxidSeen → **只 warn 不拒** (L281-289) — 重放面
- **读侧**: FileTxnIterator.next (L784-824): readLong CRC → readTxnBytes (len+payload+校验 0x42) → CRC 比对 → deserializeTxn; **EOFException → 跳下一文件 (尾部残缺容忍)**; **CRC_ERROR IOException → 致命 (中部损坏)**

## 代码类型
Implementation (文件格式)

## 跨域关联
- Z-4: Sync 批量 toFlush → append/commit (SyncRequestProcessor:85-92,160-215)
- Z-3: payload = marshallTxnEntry (hdr+txn+digest, Util:185-196)
- Z-2: LearnerHandler txnlog 同步面 + truncate

## 结论
TxnLog = [ZKLG|ver2|dbid] + CRC(len(payload)0x42) 链 + 64MB 预分配; CRC 实覆盖 payload 而非 Javadoc 所述全字段; 尾部残缺容忍/中部损坏致命。
源码位置: FileTxnLog.java:102-147,275-327,394-443,715-824; Util.java:157-173,205-208; FilePadding.java:30,101-115
