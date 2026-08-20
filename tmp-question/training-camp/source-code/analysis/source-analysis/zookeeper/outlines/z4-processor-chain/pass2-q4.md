# 闭环笔记 q4: Commit 读写分离 + Final 应用

## 假设
读直通 + 写等 commit + per-session 保序; Final 应用+响应一体。

## 验证过程
- **CommitProcessor** (quorum/CommitProcessor, 644): **双队列** queuedRequests (本地) / committedRequests (leader 提交回包, L93-114)
- **读直通** (L251-260): `needCommit(request) || pendingRequests.containsKey(sessionId)` false → 直接 sendToNextProcessor — **读不等广播**
- **写暂存** (L251-255): **pendingRequests per-session Deque** — **会话内串行, 跨会话并行**
- **提交匹配** (L325-349): committedRequests 头 vs queuedWriteRequests — **sessionId+cxid 匹配** (本地写) → sessionQueue.poll → sendToNextProcessor
- **waitForEmptyPool** (L294): 处理提交前 drain 读 — 读写顺序边界
- **maxReadBatchSize/maxCommitBatchSize**: 批控 (读不 starve 写, L246-248)
- **FinalRequestProcessor** (680): **applyRequest → zks.processTxn (树应用 Z-3)** (L158) + **响应面**: 错误 hdr → ErrorTxn (L177-191) / **throttled → THROTTLEDOP** (L207-209) / **cnxn.sendResponse(hdr, rsp)** (L594) + decInProcess (节流计数 L170)
- **ping 响应**: PING_XID + lastZxid (L218)

## 代码类型
Implementation (读写分离 + 应用响应)

## 跨域关联
- Z-3: processTxn (树应用)
- Z-2: committedRequests 数据源 (leader COMMIT)
- Z-7: cnxn (客户端连接面)

## 结论
Commit = 读直通 + 写 per-session 等 commit + 匹配放行 + 读 drain 边界; Final = 应用 + 响应 + 节流。会话级串行是 ZK 并发模型核心。
源码位置: CommitProcessor.java:93-114,251-349; FinalRequestProcessor.java:146-218,594
