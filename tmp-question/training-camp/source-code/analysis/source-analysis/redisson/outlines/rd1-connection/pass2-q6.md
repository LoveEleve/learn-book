# 闭环笔记 q6: ServiceManager 生命周期 — 中央服务工厂 + shutdown 顺序

## 假设
ServiceManager 是唯一持有 Netty EventLoopGroup/定时器/订阅器/空闲监控的中央服务; 构造按 TransportMode 分支建组; shutdown 有固定顺序。

## 验证过程
- ServiceManager.java:122-156 (全字段): ConnectionEventsHub (L122)/id (UUID)/EventLoopGroup group (L126)/resolverGroup/executor/cfg/HashedWheelTimer timer (L138)/IdleConnectionWatcher connectionWatcher (L140)/ElementsSubscribeService (L144)/NatMapper (L146)/QueueTransferService (L154)/LockRenewalScheduler renewalScheduler (L156, register 注入)
- 构造器分支 (TransportMode): EPOLL → EpollEventLoopGroup (nettyThreads=32 默认, DefaultThreadFactory "redisson-netty"), UDS 仅 single 模式 + EPOLL/KQUEUE 校验 (构造器 L5-13); KQUEUE 类似; NIO 默认
- newTimeout (L297): 委托 HashedWheelTimer — DNSMonitor/续期/清理全部走此
- shutdown 面: shutdownFutures (L404) / shutdownFuturesAsync (L416) — 连接未来集中管理 (responses 表 L152 + lastFutures L391: FastRemovalQueue<CompletableFuture<?>>)
- isShuttingDown: shutdownLatch (L142) — DNSMonitor 每轮检查 (DNSMonitor.java:79-81)
- MapResolver (L753, liveobject 包) — 只服务 liveobject
- register (L766): LockRenewalScheduler 注入点

## 代码类型
Implementation (服务容器) — Hub 角色

## 跨域关联
- Q1 (Redisson 构造器) → register 消费
- Q4 (DNSMonitor) → newTimeout + isShuttingDown
- RD-2 (LockRenewalScheduler) → register 注册
- RD-4 (命令执行) → responses/lastFutures 追踪
- Netty (EventLoopGroup) → 传输层宿主

## 结论
ServiceManager = 单例中央服务: EventLoopGroup (按 TransportMode 分支) + HashedWheelTimer (所有定时任务) + 订阅/空闲监控/映射服务 + 未来追踪 (shutdown 时统一完成); LockRenewalScheduler 等后期服务经 register() 注册。shutdown 由 shutdownLatch 广播。
源码位置: ServiceManager.java:122-156,297,404-416,766
