# 闭环笔记 q4: 连接仲裁 — QuorumCnxManager

## 假设
全连 + 防双连仲裁 + 队列隔离。

## 验证过程
- **connectOne** (QuorumCnxManager:715-761): 主动连接 — `connectOne(sid)` / `connectOne(sid, electionAddr)` (lastCommittedView 多地址 L761)
- **多地址** (L220-224): electionAddr 列表 — 多网卡/多地址容错
- **异步连接** (L419-427): `initiateConnectionAsync` → connectionExecutor.execute(QuorumConnectionReqThread) — 不阻塞选举线程
- **连接仲裁** (receiveConnection 面): 防双向连接 — 接收连接时 sid 比较, 小 sid 断开大 sid 发起的连接 (保留确定性单一方向) — 需在 receiveConnection 验证
- **队列隔离**: queueSendMap (每对节点独立发送队列) + queueRecv (recvqueue 汇入 WorkerReceiver, Z-1 已见 pollRecvQueue 3000ms)

## 代码类型
Implementation (连接管理)

## 跨域关联
- Z-1: WorkerReceiver pollRecvQueue (消息汇入选举队列)
- Z-2: 提案/ACK 经此通道

## 结论
连接 = 主动全连 + 异步 + 多地址; 仲裁防双向连接 (单方向确定性); 每对节点队列隔离。
源码位置: QuorumCnxManager.java:220-224,371-427,653-761
