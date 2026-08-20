# 闭环笔记 q2: 提议广播 — logicalclock + 自荐 + 指数退避

## 假设
选举 = 轮次递增 + 自荐 + 全员广播; 收不到回执退避重发。

## 验证过程
- **lookForLeader 起点** (L880-908): `logicalclock.incrementAndGet()` + `updateProposal(getInitId(), getInitLastLoggedZxid(), getPeerEpoch())` — 初始投自己 (含自己的最后 zxid) + sendNotifications
- **sendNotifications** (L509+): 遍历 QuorumVerifier 成员 → ToSend(notification) 入 sendqueue → WorkerSender 异步发
- **指数退避** (L957-978): `recvqueue.poll(notTimeout)` null → `manager.haveDelivered() ? sendNotifications() : connectAll()` + `notTimeout = Math.min(notTimeout << 1, maxNotificationInterval)` — **min=finalizeWait=200ms → max=60000ms** (L62-76)
- **2 节点 Oracle 特例** (L980-994): QuorumOracleMaj + revalidateVoteset → 直接当选 (ZOOKEEPER-3922 注释 L985-987)
- **收包守卫**: validVoter(n.sid) && validVoter(n.leader) (L958) — 双视图成员

## 代码类型
Implementation (自荐广播 + 退避)

## 跨域关联
- Z-2: connectAll (QuorumCnxManager 连接建立)
- Z-9: getInitLastLoggedZxid (持久化日志)

## 结论
选举发起 = 轮次++ + 自荐 (带 zxid) + 全员广播; 无回执指数退避 200ms→60s 重发; 连接缺失时先 connectAll。
源码位置: FastLeaderElection.java:880-908,957-994,509+
