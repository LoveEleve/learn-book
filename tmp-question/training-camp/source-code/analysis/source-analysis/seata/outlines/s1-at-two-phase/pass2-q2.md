# 闭环笔记 q2: 全局事务对象 — begin/commit/rollback 生命周期

## 假设
DefaultGlobalTransaction 封装 xid/status/role; begin 绑定 RootContext; commit/rollback 带 TM 侧重试。

## 验证过程
- **对象构造** (DefaultGlobalTransaction:69-85): 默认 Launcher + UnKnown; getCurrent (GlobalTransactionContext:45-51) 构造 **Participant** (RootContext 有 xid 时); **reload 禁止 begin** (L73-80, IllegalStateException "Never BEGIN on a RELOADED GlobalTransaction")
- **begin** (L98-119): createTime=now (超时计算基准) → Launcher 才执行 → **assertXIDNull + RootContext 已有 xid → IllegalStateException** ("can't begin a new global transaction") → **transactionManager.begin(null, null, name, timeout)** → status=Begin → **RootContext.bind(xid)** (线程绑定)
- **commit** (L123-159): Participant 忽略 → **COMMIT_RETRY_COUNT 重试循环** (client.tm.commit.retry.count, L60-64,135-152) — 失败倒数重试, 0 时抛 → finally **suspend(true) 解绑** (xid 匹配时)
- **TM 协议** (DefaultTransactionManager.begin): GlobalBeginRequest (name+timeout) → **syncCall** → GlobalBeginResponse.xid; ResultCode.Failed → TmTransactionException(BeginFailed)
- **默认值**: DEFAULT_GLOBAL_TX_TIMEOUT=**60000ms** (L41) / DEFAULT_GLOBAL_TX_NAME="default" (L43)
- **XID 格式** (common/XID:55-62): **IP:PORT:transactionId** — TC 地址编码, 客户端据 xid 路由 TC (负载均衡面 XIDLoadBalance)

## 代码类型
Implementation (生命周期)

## 跨域关联
- S-3: TC 端 begin 处理 (DefaultCore.begin → GlobalSession)
- S-8: SessionHolder/GlobalSession 服务端面
- S-7: TM 重试 (client.tm.*.retry.count)

## 结论
生命周期 = begin (bind) → 业务 → commit/rollback (TM 重试 + 解绑); 角色决定行为; XID 编码 TC 地址。
源码位置: DefaultGlobalTransaction.java:41-159; DefaultTransactionManager.java; common/XID.java:55-62
