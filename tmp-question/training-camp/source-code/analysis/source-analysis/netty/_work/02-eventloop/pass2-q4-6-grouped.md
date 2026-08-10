## Loop Note: Q4 — SelectedSelectionKeySet 优化

**Hypothesis**: JDK Selector 的 `selectedKeys()` 返回 HashSet，每次 select 后需要 O(N × size) 迭代所有注册的 key 来检查就绪状态。SelectedSelectionKeySet 用数组 + size 计数器替代，迭代只遍历选中 key，O(selectedCount)。

**Verification** (`NioIoHandler.java:143-200, 525-540`):
- `SelectedSelectionKeySet` — 替换 `selector.selectedKeys()` 返回的 HashSet
- `openSelector()` (`line 143`) — 通过反射将 `selectorImpl.selectedKeys` + `selectorImpl.publicSelectedKeys` 两个字段替换为 `SelectedSelectionKeySet`
- JDK 默认: `selector.selectedKeys()` → 遍历所有注册 key (`HashSet<SelectionKey>`)
- Netty 优化: `processSelectedKeysOptimized()` (`line 512`) → 遍历 `selectedKeys.flip()` → 只遍历实际选中的 key (O(N) where N = selected)
- 数组容器自带 `flip()` — 标记 size 作为迭代上限

**Code type**: Implementation (performance hack via reflection)

**Conclusion**: 核心优化不是"用什么数据结构"——而是"迭代多少元素"。一个 Selector 可能注册了 200 个 Channel，但一次 select 只有 3 个收到数据。HashSet 迭代 200 个检查 readyOps，数组只迭代 3 个。source: NioIoHandler.java:143-200,512-540

---

## Loop Note: Q5 — Wakeup Race Condition

**Hypothesis**: wakenUp AtomicBoolean 的 CAS + selector.wakeup() 之间有时序竞争——wakenUp.set(false) 和 selector.select() 之间如果发生 wakeup，wakenUp 被设为 true 但 selector 已经在阻塞，下次 CAS 失败绕过 wakeup。代码插入立即执行的 wakeup 检查破坏 race。

**Verification** (`NioIoHandler.java:107-111, 430-466, 658-666`):
- `wakenUp = new AtomicBoolean()` (`line 111`)
- Race BAD 场景: `wakenUp.set(false)` + `selector.select(timeout)` 之间 → wakeup() → `wakenUp.compareAndSet(false, true)` = true → selector.select() 阻塞 -> 预期的 wakeup 不生效
- Netty 解决: `selector.select(timeout)` 后检查 `if (wakenUp.get()) { selector.wakeup(); }` (`line 464-466`) — 如果 select 期间 wakenUp 被设为 true，立即再次 wakeup
- 代价: 解决 BAD case 时也对 OK case（次 wakeup 已生效）做一次多余的 wakeup——wakeup 是幂等的，多一次无害

**Code type**: Implementation (concurrency correctness)

**Conclusion**: 这是 CAS→select→CAS 间 race 的标准解决方案——select 后立即 double check。类似于并发编程中的 double-checked locking 模式。source: NioIoHandler.java:430-466,658-666

---

## Loop Note: Q6 — EventLoopGroup 分配策略

**Hypothesis**: NioEventLoopGroup.next() 通过 EventExecutorChooser 将新 Channel 分配到下一个 EventLoop——默认 Power-of-Two chooser (O(1) 位运算)。

**Verification**:
- `MultithreadEventLoopGroup.java:77-78` — `next() → (EventLoop) super.next()` → delegate to `MultithreadEventExecutorGroup`
- `MultithreadEventExecutorGroup.java:43` — `EventExecutorChooserFactory.EventExecutorChooser chooser`
- `DefaultEventExecutorChooserFactory` → 两个内部类:
  - `PowerOfTwoEventExecutorChooser` — executors.length 是 2 的幂 → `idx.getAndIncrement() & (length - 1)` (位运算, O(1))
  - `GenericEventExecutorChooser` — 通用 → `abs(idx.getAndIncrement() % length)`
- `SingleThreadIoEventLoop.register(IoHandle)` (`line 234-243`) — inEventLoop → registerForIo0, else → execute(() → registerForIo0)
- `registerForIo0()` (`line 250-261`) → `ioHandler.register(handle)` → IoRegistration → numRegistrations++

**Code type**: Glue (distribution strategy)

**Conclusion**: Chooser 策略从 int 自增 + 取模简化为位运算 (power-of-2) 优化——在 Netty 每次 accept 新连接都要走这里，位运算是关键性能点。与 Arena 数量对齐 (cores×2) 配合，新 Channel 被分配到 EventLoop N 时，其 ByteBuf 也大概率被 Arena N 处理——零跨 Arena 锁竞争。source: MultithreadEventLoopGroup.java:77-78, DefaultEventExecutorChooserFactory
