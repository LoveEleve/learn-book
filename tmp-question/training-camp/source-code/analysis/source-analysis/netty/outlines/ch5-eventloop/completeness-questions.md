# Ch5 EventLoop 全视角验证

> 从多角色视角提问, 验证每问都能在 outline 中找到答案

---

## 开发者视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | EventLoop 为什么同时继承 EventLoopGroup? 这和 Channel 的 register 有什么关系? | 2.1 §1-2 |
| 2 | run() 主循环的 IO 和 Task 交替执行 — maxTaskProcessingQuantum 是如何防止 task 吃光 CPU 的? | 2.1 §4 |
| 3 | tailTasks 和普通 task 有什么区别? executeAfterEventLoopIteration 的设计意图是什么? | 2.1 §6 |
| 4 | MPSC queue 注释说 "never calls takeTask()" — 为什么不阻塞? 这和 wakeup 机制有什么关系? | 2.1 §5 |
| 5 | SelectStrategy 的三个状态各触发什么行为? DefaultSelectStrategy 怎么在 "有任务" 时避免阻塞? | 2.2 §1-2 |
| 6 | delete() 永远返回 false — 不会造成内存泄漏吗? | 2.2 §4 |
| 7 | rebuildSelector0 把旧 Selector 的 Key 迁移到新 Selector — 迁移过程中如果新事件来了怎么处理? | 2.3 §4 |
| 8 | ThreadPerChannelEventLoop 的 activeChildren/idleChildren 双池 — 和通常的线程池有什么不同? | 2.4 §6 |

## 性能工程师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | SelectedSelectionKeySet 从 JDK HashSet 换成数组 — 在 10K 连接场景下 GC 压力减少多少? 为什么? | 2.2 §3-4 |
| 10 | power-of-two Chooser 的 idx&(count-1) 比取模快多少? 为什么用 count-1 而不是取反? | 2.4 §2 |
| 11 | wakeup CAS (compareAndSet→selector.wakeup) 在什么条件下才真正触发 syscall? | 2.3 §5 |
| 12 | DEFAULT_EVENT_LOOP_THREADS = CPU*2 — 为什么不是 CPU*1 或 CPU*4? 这个数怎么推导的? | 2.4 §1 |
| 13 | processSelectedKeysOptimized 在 needsToSelectAgain 时 reset(i+1)+i=-1 — 为什么是用剩余索引而不是全部? | 2.2 §7 |

## SRE/运维视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 14 | Netty 应用 CPU 100% 但 GC 日志正常 — 怎么判断是不是 epoll bug? 日志里找什么? | 2.3 §2-4 |
| 15 | SELECTOR_AUTO_REBUILD_THRESHOLD=512 被触发了 — rebuild 后业务会受影响吗? 会有连接断开吗? | 2.3 §4 |
| 16 | 怎么调高或调低 SELECTOR_AUTO_REBUILD_THRESHOLD? 设为 0 会怎样（禁用自动检测，需手动 rebuildSelectors）? | 2.3 §3 |
| 17 | MAX_PENDING_TASKS 超限后会怎样? 有默认的拒绝策略吗? 可以自定义吗? | 2.1 §5 |
| 18 | ManualIoEventLoop 和 DefaultEventLoop — 在生产环境各用在什么场景? | 2.4 §4-5 |

## 架构师视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 19 | NioEventLoop 为什么变成 @Deprecated 薄壳? 新架构 IoEventLoop+IoHandler 相比旧架构有什么优势? | 2.4 §7 |
| 20 | EventLoop extends EventLoopGroup — 为什么不分成两个独立的类型? 这种自包含设计有什么代价? | 2.1 §1 |
| 21 | IoHandlerContext 的 canBlock/delayNanos/reportActiveIoTime 三个回调 — 设计意图是什么? 为什么不直接在 EventLoop 里硬编码? | 2.1 §3 |
| 22 | IoRegistrationWrapper 的 cancel 中 decrementAndGet — ManualIoEventLoop 却没有这个包装器, 为什么? | 2.1 §7, 2.4 §5 |

## 学生/新人视角

| # | 问题 | 答案位置 |
|:--:|------|------|
| 23 | EventLoop 和线程池有什么区别? 为什么 Netty 不用传统的 ThreadPoolExecutor? | 2.1 §1-2, §4 |
| 24 | select() 是阻塞操作 — 那 EventLoop 线程在等待 IO 的时候怎么同时处理任务? | 2.2 §1-2 |
| 25 | 为什么 DefaultEventLoop 不需要 Selector? 什么场景用 DefaultEventLoop 而不是 NioEventLoop? | 2.4 §4 |

---

## 覆盖统计

| 角色 | 问题数 | 覆盖 |
|------|:--:|:--:|
| 开发者 | 8 | 2.1§1-2,§4-7, 2.2§1-4,7, 2.3§4, 2.4§6 |
| 性能工程师 | 5 | 2.2§3-4,7, 2.3§5, 2.4§1-2 |
| SRE/运维 | 5 | 2.1§5, 2.3§2-5, 2.4§4-5 |
| 架构师 | 4 | 2.1§1,3,7, 2.4§5,7 |
| 学生/新人 | 3 | 2.1§1-2,§4, 2.2§1-2, 2.4§4 |
| **合计** | **25** | **100%** |

## 缺口

无缺口 — 25 问全部能在 4 篇 outline 中找到答案。
