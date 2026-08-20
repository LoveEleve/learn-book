# 闭环笔记 q8: AsyncSemaphore permit 池容量 — 连接池精确性协议

## 假设
连接池容量用 AsyncSemaphore(poolMaxSize) 的 permit 控制; 初始化/借用/归还必须精确释放 permit, 否则池容量漂移 (泄漏或超额) — 4.6.1 曾因 tryRun 竞态修复。

## 验证过程
- ConnectionsHolder.java:44-59: allConnections (ConcurrentLinkedQueue) + freeConnections (ConcurrentLinkedDeque) + `freeConnectionsCounter = new AsyncSemaphore(poolMaxSize, serviceManager.getGroup())`
- acquireConnection (L224): freeConnections 有 → poll; 空 → 等 permit + 新建连接 (回调 connectionCallback)
- initConnections(minimumIdleSize) (L141): 预建 N 连接, **每个创建消费一个 permit, 失败必须归还**
- releaseConnection (L263): 归还连接 + permit
- 4.6.1 竞态 (CHANGELOG: "Fixed - AsyncSemaphore.tryRun() over-increment on cancelled waiters"): 取消的等待者重复 release → 计数高于 poolMaxSize → 空闲驱逐失效池永不排空
- 测试 (ConnectionsHolderTest:56-100): `testFailedInitConnectionReleasesPermitExactlyOnce` — 失败的 init 消费一个 permit 必须恰好归还一次, 断言 `counter.getCounter() == poolMaxSize`; `testSuccessfulInitReleasesEachPermitExactlyOnce` 成功路径同样精确
- AsyncSemaphore.java:53 (acquire) / 121 (release) — 基于 Netty EventLoop 的异步信号量

## 代码类型
Implementation (并发协议) — 高价值: permit 精确性是池正确性根基

## 跨域关联
- Q5 (池路由) → ConnectionsHolder 是池实体
- RD-4 (RedisExecutor 借还连接) → acquire/release 消费方
- CHANGELOG 时空: 4.6.1 竞态修复 = 该协议的真实事故史

## 结论
池容量 = AsyncSemaphore permit 协议: init 每建一连接消费一 permit, 失败精确归还; 借用 poll free 队列, 空则等 permit; 归还回队+释放。4.6.1 竞态教训: tryRun 取消等待者 over-increment 会永久抬高计数 → 池退化。测试断言 counter==poolMaxSize 是协议不变式。
源码位置: ConnectionsHolder.java:44-59,141,224,263 + ConnectionsHolderTest:56-100

## 跨域发现

来源: RD-1 第二遍, 闭环笔记 q8
发现: HikariCP h02-concurrentbag (ConcurrentBag: 无锁 CAS + 线程本地表 + 交接队列) 与 Redisson ConnectionsHolder (双队列 + AsyncSemaphore) 解决**同一问题** (连接池借还) 的两种架构: Hikari 追求无锁极致低延迟 (借出不用信号量, 用 threadlocal 顺手取), Redisson 追求异步化 (AtomicSemaphore 挂 EventLoop)。对比点 = 面试高频: "连接池快在哪?"
已对照验证: hikaricp/outlines/h02-concurrentbag/01-concurrentbag.md header [模式:无锁并发+线程亲和+交接]; ConnectionsHolder.java:44-59
传播操作: 第一级 — RD-1 篇2 (连接池) 大纲加"对照 h02" 显式对比段 (06 §2.5 80/20 差异展开)
