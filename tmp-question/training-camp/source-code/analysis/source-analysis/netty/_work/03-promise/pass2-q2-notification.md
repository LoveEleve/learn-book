## Loop Note: Q2 — notifyListeners 通知机制

**Hypothesis**: notifyListeners 不只是遍历 listener 列表——有两层防御：(1) 单 listener 优化避免创建数组，(2) 栈深度保护防止递归爆栈。

**Verification**:
- `DefaultPromise.java:72-73` — `listener` (第一个监听器) + `listeners` (DefaultFutureListeners，≥2个时使用) — 单监听器优化
- `DefaultPromise.java:498-520` — `notifyListeners()`: inEventLoop → 检查 stackDepth < MAX_LISTENER_STACK_DEPTH(8) → 直接调用 notifyListenersNow；超过阈值或不在 EventLoop → `safeExecute(executor, notifyListenersNow)`
- `MAX_LISTENER_STACK_DEPTH = 8` (`line 52-53`) — 可通过 `io.netty.defaultPromise.maxListenerStackDepth` 配置
- `DefaultPromise.java:552-593` — `notifyListenersNow()`: synchronized copy of listeners → notifyListeners0
- `setValue0(objResult)` (`line 646-655`): CAS null→result → `checkNotifyWaiters()` → `notifyListeners()` (如果有 listener) + `notifyAll()` (如果有 await 等待者)
- `addListener()`: synchronized addListener0 → if already done → `notifyListeners()` (immediate notify)

**Code type**: Implementation

**设计权衡**:
| 机制 | 做了什么 | 为什么 |
|------|------|------|
| Stack overflow protection | stackDepth ≥ 8 → submit to executor | listener 可能再 addListener → 递归链 → StackOverflowError |
| Single listener opt | 第一个 listener 存字段，≥2 升级为 DefaultFutureListeners 数组 | 大多数 Promise 只有 0-1 个 listener |
| Immediate notify | addListener 时如果已完成 → 立即通知 | 先完成再 add → listener 不应错过通知 |

**Conclusion**: notifyListeners = 不是简单的 for 循环。三层防护：栈深度保护、单监听器优化、立即通知。CAS 完成后通知所有等待者和监听器。source: DefaultPromise.java:47-53,498-520,552-593,638-666
