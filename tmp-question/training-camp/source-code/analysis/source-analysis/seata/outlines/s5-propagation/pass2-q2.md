# 闭环笔记 q2: 挂起恢复 — suspend/resume + 上下文解绑

## 假设
挂起 = 线程上下文解绑 (xid 移出), 恢复 = 重新绑定; clean 语义区分挂起与结束。

## 验证过程
- **suspend** (DefaultGlobalTransaction:207-228): xid = RootContext.getXID() → **unbind()** → **clean ? null : new SuspendedResourcesHolder(xid)** — clean=true (事务结束) 不返回 holder; 注释 "In order to associate the following logs with XID, first get and then unbind" (L213)
- **resume** (L231-240): holder null → return; **RootContext.bind(xid)** — 恢复线程绑定
- **RootContext.bind** (RootContext:124-137): **空 xid → 转 unbind** (L127 注释); 否则 **MDC.put + CONTEXT_HOLDER.put**
- **RootContext.unbind** (L165-176): CONTEXT_HOLDER.remove + **MDC.remove**
- **SuspendedResourcesHolder** (44 行): 单 xid 字段 + null 检查 (L31-36) — 挂起资源的最小载体
- **结束解绑**: commit/rollback finally → **suspend(true)** (L154-156,197-199) — 事务结束即解绑 (xid 匹配才解)

## 代码类型
Implementation (上下文挂起)

## 跨域关联
- S-1: TransactionalTemplate 外层 finally resume (L147-152)
- S-9: 线程上下文 (Spring 集成依赖)
- S-13: MDC 日志关联 (xid 上下文)

## 结论
挂起 = unbind + holder; 恢复 = bind; clean 区分挂起/结束; MDC 同步维护。
源码位置: DefaultGlobalTransaction.java:207-240; RootContext.java:124-176; SuspendedResourcesHolder.java:31-36
