# 闭环笔记 q2: 接收端与协议 — RmBranchCommitProcessor

## 假设
TC 分支通知经 Netty 到 RM 处理器; 协议携带分支身份。

## 验证过程
- **接收端** (RmBranchCommitProcessor:42-63): 构造注入 handler → process → **handleBranchCommit: handler.onRequest(BranchCommitRequest, null)** → BranchCommitResponse 回传
- **RmBranchRollbackProcessor**: 对称 (回滚通知)
- **协议字段** (BranchCommitRequest): **xid / branchId / resourceId / applicationData** — 分支身份完整
- **响应回传**: BranchStatus (PhaseTwo_*) 回 TC — TC 侧 switch 处理 (S-1:329-363)
- **MDC**: 处理器设置 xid/branchId
- **异步通知**: TC 侧发送异步 (sendAsyncRequest — S-3 广播面)

## 代码类型
Implementation (接收端)

## 跨域关联
- S-1: 状态回传消费 (PhaseTwo_Committed → removeBranch)
- S-3: Netty 发送面

## 结论
接收 = RmBranchCommit/RollbackProcessor → handler.onRequest → 协议 4 字段 → BranchStatus 回传。
源码位置: RmBranchCommitProcessor.java:42-63; BranchCommitRequest.java
