# 闭环笔记 q1: 链装配 — 五节点 + Request 封装

## 假设
生产者-消费者链; 每级独立线程 + 队列。

## 验证过程
- **链构造** (ZooKeeperServer.setupRequestProcessors): PrepRequestProcessor → SyncRequestProcessor → CommitProcessor → ToBeAppliedRequestProcessor → FinalRequestProcessor — 单向 next 引用
- **ToBeAppliedRequestProcessor** (Leader.java:1117-1131): leader 侧"已提交待应用"清单维护 — 注释: next 必须 Final 且同步应用 (Z-2 交叉)
- **Request 封装** (Request.java 561 行): sessionId/cxid/zxid/type/authInfo/cnxn/owner + requestOfDeath 毒丸
- **每级线程**: Prep run (L137-163): submittedRequests.take 循环 + **PREP_PROCESSOR_QUEUE_TIME 指标** (L143-144) + requestOfDeath 退出 (L152-154)
- **follower/observer 变体**: ZooKeeperServer 子类 (FollowerZooKeeperServer/ObserverZooKeeperServer) 重装配 (follower 无 Sync? — 待验证)

## 代码类型
Architecture (责任链 + 队列)

## 跨域关联
- Z-2: ToBeApplied (leader 提交待应用)
- Z-3: Final → processTxn

## 结论
链 = Prep→Sync→Commit→ToBeApplied→Final 五节点, 每级独立线程+队列; Request 封装全上下文; 毒丸停止。
源码位置: ZooKeeperServer.setupRequestProcessors; PrepRequestProcessor.java:137-163; Leader.java:1117-1131
