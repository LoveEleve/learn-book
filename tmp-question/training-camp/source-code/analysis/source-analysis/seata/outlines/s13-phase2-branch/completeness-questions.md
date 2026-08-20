# S-13 Phase2 分支通知 — completeness-questions (全视角提问验证)

## 开发者视角

1. RM 处理器有几个? (5: AT/XA/TCC/Saga/SagaAnnotation)
2. 怎么注册? (SPI loadAll)
3. 怎么分发? (getRMHandler(branchType))
4. 接收端? (RmBranchCommit/RollbackProcessor)
5. 协议字段? (xid/branchId/resourceId/applicationData)
6. AT 提交? (AsyncWorker 异步)
7. AT 回滚? (undo 补偿)
8. 状态回传? (BranchStatus)

## 架构师视角

9. 为什么双面多态? (TC getCore / RM getRMHandler — 分支类型贯穿)
10. 为什么提交异步? (Phase2 非阻塞 — 最终一致)
11. 为什么回滚同步补偿? (undo 需完整执行)
12. 为什么状态回传? (TC 决策依据 — removeBranch/重试)
13. 为什么无 ACK 握手? (状态回传即确认 — 对照 RocketMQ)
14. 为什么 exceptionHandleTemplate? (双侧统一异常)
15. 为什么 SagaAnnotation 独立? (注解模式分离)
16. 对照 RocketMQ 事务消息? (两阶段确认对照)

## 学生视角

17. 什么是分支通知? (TC 通知 RM 执行提交/回滚)
18. 什么是处理器族? (按分支类型分派)
19. 什么是状态回传? (执行结果回报 TC)
20. 什么是收束? (13 域闭环)
