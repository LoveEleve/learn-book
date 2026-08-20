# 闭环笔记 q1: 投票结构 — Notification/ToSend/Vote

## 假设
投票消息四元组 + 双队列 + 协议版本兼容。

## 验证过程
- **Notification** (FastLeaderElection:112-145): `leader / zxid / electionEpoch / state / sid / peerEpoch / qv` — CURRENTVERSION=0x2 (L117); zxid = 提议者最后日志 zxid (数据新鲜度)
- **ToSend** (L154-202): `mType` 四类 — **crequest (连接请求)/challenge/notification/ack**; 字段同 Notification + configData (reconfig 配置广播)
- **Vote**: (leader, zxid, electionEpoch, peerEpoch) — getVote (L830-831) 构造本地票
- **双队列**: sendqueue + recvqueue (LinkedBlockingQueue) — Messenger 双线程 (WorkerSender 消费 sendqueue / WorkerReceiver 填 recvqueue, L204-232)
- **协议兼容** (L245-257): capacity==28 → ZK-107 前无 peerEpoch; ==40 → 含 peerEpoch+version; 新协议再变 — 向后兼容三代

## 代码类型
Data Structure (投票消息 + 队列)

## 跨域关联
- Z-2 (广播): QuorumCnxManager 是消息通道 (recvqueue 上游)
- Z-9 (持久化): zxid 来自最后日志 (getInitLastLoggedZxid)

## 结论
投票四元组 (leader/zxid/electionEpoch/peerEpoch) + state 标记状态 + 双队列线程模型; 28B/40B 三代协议兼容。
源码位置: FastLeaderElection.java:112-202,204-257,830-831
