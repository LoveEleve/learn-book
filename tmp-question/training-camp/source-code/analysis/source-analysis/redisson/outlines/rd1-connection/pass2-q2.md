# 闭环笔记 q2: lazyConnect 单飞 — CAS latch + 重入防护 + 失败重试

## 假设
lazyConnect 用 AtomicReference<CompletableFuture<Void>> 作 latch: 首个线程 CAS(null→newFuture) 获得连接所有权, 其余线程 join() 等待; 失败后可重试; 持有线程重入必须返回避免自死锁。

## 验证过程
- MasterSlaveConnectionManager.java:190-227 (lazyConnect 全文):
  - L191-193: `isInitialized()` 快速路径 (已初始化直接返回)
  - L197-199: **重入防护**: `Thread.currentThread() == connectingThread` → return (注释: "Re-entry by the connecting thread itself: return rather than join() the latch it holds, which would self-deadlock")
  - L201-213: `lazyConnectLatch.compareAndSet(null, newFuture)` — 成功=获得所有权; 失败→读 currentFuture:
    - `isCompletedExceptionally()` → CAS 替换 newFuture (L204-208) **失败可重试**
    - 否则 `join()` 等待 (L210)
  - L217-226: 所有权线程标记 connectingThread → connect() → complete/completeExceptionally → finally 清空标记
- 测试对应: MasterSlaveConnectionManagerTest:179 (testLazyConnectReentryFromConnectingThreadDoesNotDeadlock), L144 (testLazyConnectRetriesAfterFailedInitialization)
- 死锁场景: connect() 同步路径内部若再触发 lazyConnect (如 getEntry 时), 同一线程 join 自己持有的 latch → 死锁; connectingThread 检查截断此路径

## 代码类型
Implementation (并发协议) — 高价值: CAS + 线程身份双重防护

## 跨域关联
- Q3 (connect 重试循环) → lazyConnect 的失败由内部 connect 重试吸收
- RD-4 (命令执行首次触发 lazy) → lazyConnect 是懒模式的统一入口

## 结论
lazyConnect = 单飞锁协议: CAS latch 保证单线程连接 (其余 join), isCompletedExceptionally 检测失败 → 新线程可重试, connectingThread 身份检查防自死锁。三测试对应 (重试/重入/无死锁)。
源码位置: MasterSlaveConnectionManager.java:190-227 + 测试 L144,179
