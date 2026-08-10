# Ch5 epoll bug 与 Selector 重建 — 三层防护的故障恢复

> Cluster D: 10 KPs | 依赖 §5.2 Selector 优化 | §5.2 → §5.3

### 1. epoll 假唤醒 — NIO 最著名的生产级 bug

场景: Linux 上 `epoll_wait(fd, events, maxEvents, timeout)` 被内核唤醒——但 `epoll_wait` 返回 0(没有就绪事件)。JVM 底层 `EPollSelectorImpl.epoll_wait()` 把这个 0 传给了上层 `select()`——select 返回 0 但 selectedKeys 为空。EventLoop 以为"没有 IO 就绪"→循环继续→下一个 loop 又 select→又返回 0→selectedKeys 仍空→死循环空转→CPU 100%。

源码路径: JDK bug JDK-6427854——epoll_wait 的虚假唤醒是 Linux 2.6 内核的已知 bug, 在特定条件下(Linux 定时器精度+epoll 内部 race)返回 0。Detectable by: `selector.select(timout) == 0` 且 `selectedKeys.isEmpty()` 且 `!hasTasks()`。三次同样的现象→loop 空转。

关键设计: 假唤醒了为什么 CPU 100%? 因为事件循环中没有阻塞点——select 返回 0→skip IO processing→check hasTasks(empty)→confirmShutdown(not shutting)→next loop→select→0→repeat。没有任务、没有 IO——纯粹的自旋。Linux 上的严重性远高于 macOS(kqueue)和 Windows(select), 因为 epoll 是生产环境的标配多路复用器。

### 2. 第一层 — wakeup CAS race fix

场景: EventLoop 线程正在 `selector.select()` 阻塞——外部线程进来了一个新 Channel 需要注册→调 `selector.wakeup()`→select 返回。但 wakeup 后, EventLoop 还没处理新 Channel——外部线程又调了一次 `register()`→又 wakeup。第二次 wakeup 可能在前一轮 select 返回后、下一轮 select 之前的窗口内——被丢失。

源码路径: `NioIoHandler.java:464-466` — `wakenUp race fix`: `select()` 返回后, `if (wakenUp.get()) { selector.wakeup(); }`——select 返回后再次检查并主动 wakeup——防止窗口期唤醒信号丢失。`NioIoHandler.java:615-619` — wakeup CAS: `wakenUp.compareAndSet(false, true)` 成功→`selector.wakeup()`——只有 CAS 成功才调昂贵的 wakeup syscall, 其他线程的冗余 wakeup 被过滤。

关键设计: wakenUp 是 `AtomicBoolean`——select 内部: CAS `wakenUp.set(false)` → `selector.select()` → 返回后检查 `wakenUp.get()`。如果外部线程在 CAS(false)和 select()进内核之间的窗口调了 wakeup——`wakenUp` 仍然是 true——select 返回后 `if(wakenUp.get()) { selector.wakeup() }` 补偿一次。这个修复处理的不是假唤醒(假唤醒是内核层面的 Bug), 而是 wakeup 的——CAS 防护唤醒信号丢失。

数据流: Thread A: `select()` → `wakenUp.set(false)` → [窗口] ← Thread B: `wakenUp.compareAndSet(false,true)`→`selector.wakeup()` → [window ends] → `selector.select()`(被唤醒) → `if(wakenUp.get())` → true! → `selector.wakeup()`(补偿) → 确保唤醒不丢失。

### 3. 第二层 — selectCnt 空轮询检测

场景: epoll 假唤醒连续发生——上文描述的 EventLoop 空转。怎么区分"没有 IO 就绪(正常)"和"epoll 假唤醒(bug)"? 单次 select=0 不能判断——可能是真的没有数据。但连续 512 次 select 都返回 0——一定是 bug。

源码路径: `NioIoHandler.java:66-88` — `SELECTOR_AUTO_REBUILD_THRESHOLD = 512`, `io.netty.selectorAutoRebuildThreshold` 系统属性。`NioIoHandler.java:697-703` — 检测逻辑: `selector.selectNow()` 返回 0 且没有 timeout→`selectCnt++`(计数+1); 返回 >0 或超时→`selectCnt=0`(重置计数——有正常的 IO 或超时, 说明 Selector 工作正常)。`selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD` → `selectRebuildSelector()` → `rebuildSelector0()` → `selector.selectNow()` → selectCnt 重置为 1。若阈值 < 3 → 设为 0 禁用检测(NioIoHandler.java:66-88)。

关键设计: 阈值 512 是怎么选的? 如果太小(如 10), 正常的内存抖动也可能触发重建——重建 Selector 有迁移成本。如果太大(如 10000), CPU 已经在自旋上浪费了太长时间。512 是一个经验值——在 100ms tick 下, 512×100ms≈51 秒的假唤醒后才触发重建——足以区分"暂时没有数据"和"Selector 真的坏了"。

数据流: `selectNow()==0 && !hasTasks() && !hasScheduledTasks()` → `selectCnt++` → `selectCnt>=512` → `selectRebuildSelector()` → `openSelector()` 新 Selector → 遍历 oldSelector.keys() 重新注册 → 原子替换 `this.selector` → 关闭旧 Selector → `selectCnt=1`。

### 4. 第三层 — rebuildSelector0 重建+迁移全部注册

场景: selectCnt 达到 512——确认 epoll bug——需要创建一个新 Selector, 把旧 Selector 上所有的 Channel 注册迁移到新 Selector。但迁移期间——旧 Selector 上可能还有未就绪的事件(它们还没被 epoll 假唤醒), 也可能有新的 Channel 注册(它们应该进新 Selector)。

源码路径: `NioIoHandler.java:255-302` — `rebuildSelector0()`: `openSelector()` 创建新 Selector → 遍历 `oldSelector.keys()`(所有已注册但未取消的 key)——对每个 SelectionKey: 读取 `key.attachment()` + `key.interestOps()` → `channel.register(newSelector, ops, attachment)` 重新注册 → 原子替换 `this.selector`/`this.unwrappedSelector`(SelectorTuple 双引用)→ 关闭旧 Selector。`NioEventLoopGroup.java:173-177` — `rebuildSelectors()` 批量入口: 遍历 children → 每个 NioEventLoop.rebuildSelector()。

关键设计: 重建期间会发生两件事: 1) 旧 Selector 上可能有未触发的事件——它们在旧 Selector.close() 时被丢弃。但在下一轮 select 中, 这些事件会被新 Selector 捕获(因为 Channel 已经被重新注册, 且内核缓冲区中有数据→epoll 会立即返回)。2) 新 Channel 的 register——是在新 Selector(原子替换后)上操作的。原子替换确保"迁移完成后"的注册看向新 Selector, "迁移完成前"的注册看向旧 Selector。

数据流: `rebuildSelector0()` → `openSelector()` → 遍历 `oldSelector.keys()` → `oldKey.interestOps=OP_READ` → `channel.register(newSelector, OP_READ, newKey)` → `this.selector = newSelector`(原子) → `oldSelector.close()` → 所有旧 key 失效 → 新 key 生效。

### 核心悬念

**"epoll bug 的三层防护——wakeup CAS(第一层)、selectCnt 累计(第二层)、重建迁移(第三层)——让 EventLoop 在生产环境 7×24 运行。但 §5.4 的多线程模型——MultithreadEventLoopGroup 用 nThreads=CPU*2 个 EventLoop 管理 N 个 Channel——每个 EventLoop 独立维护自己的 Selector——如果其中一个 EventLoop 触发了 rebuild, 其他 EventLoop 会感知到吗？多线程下 Channel 的分配怎么保证负载均衡？"**

→ 引出 §5.4 多线程与特殊模型 — PowerOfTwoEventExecutorChooser 用 idx&(count-1) 分配 Channel, 但 NioEventLoop 的退化(核心逻辑移到 NioIoHandler)让架构更清晰: EventLoop=调度, IoHandler=IO。
