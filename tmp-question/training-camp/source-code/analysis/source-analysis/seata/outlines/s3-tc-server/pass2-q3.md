# 闭环笔记 q3: 消息分发 — onRequest + 限流装饰器

## 假设
TM/RM 请求统一入口 onRequest → 限流 → 按消息类型分派 doXxx → core。

## 验证过程
- **onRequest** (DefaultCoordinator:797-806): 非 AbstractTransactionRequestToTC → IllegalArgumentException; 否则 **setTCInboundHandler(this)** + **LimitRequestDecorator** (L804 — 限流装饰器包裹) → handle(context)
- **限流面** (server/limit/LimitRequestDecorator:25-39): **RateLimiterHandler.getInstance()** + setTransactionRequestLimitHandler — 请求级限流装饰 (可插拔 handler)
- **doXxx 8 方法** (L321-401): doGlobalBegin (core.begin, rpcContext 提供 applicationId/group) / doGlobalCommit / doGlobalRollback / doGlobalStatus / doGlobalReport / doBranchRegister (返回 branchId) / doBranchReport / doLockCheck (返回 lockable) — **全部 MDC.put(xid)** (L342-399)
- **getInstance** (L249-261): **SessionMode.RAFT → RaftCoordinator** (多态, S-8 交叉) — 存储模式驱动协调器选择
- **onResponse** (L809-813): 仅类型校验 (无业务)
- **TCC/SAGA 扩展**: AbstractCore 多态 (S-13) — doGlobalCommit/doGlobalRollback 转发 core (L270-293)

## 代码类型
Architecture (消息分发)

## 跨域关联
- S-1: core.begin/commit/rollback (消费面)
- S-8: RAFT → RaftCoordinator (存储模式面)
- S-13: AbstractCore 多态收束

## 结论
分发 = 统一入口 + 限流装饰器 + doXxx 分派 + MDC 上下文; RAFT 模式切换协调器实现。
源码位置: DefaultCoordinator.java:249-401,797-813; server/limit/LimitRequestDecorator.java:25-39
