## Loop Note: Q2 — Inbound 传播 + 掩码跳过

**Hypothesis**: fireChannelRead 不是简单调用 next handler——通过 findContextInbound + skipContext 跳过未实现此方法的 handler。掩码由 ChannelHandlerMask 编译期预计算。

**Verification** (`AbstractChannelHandlerContext.java`):
- `fireChannelRead()` (`line 341-369`): `findContextInbound(MASK_CHANNEL_READ)` → `next.invokeHandler()` → 三种类型分发 (HeadContext/DuplexHandler/InboundHandler) → or passthrough (`next.fireChannelRead(m)`)
- `findContextInbound(int mask)` (`line 927-933`): `do ctx = ctx.next while skipContext(ctx, executor, mask, MASK_ONLY_INBOUND)` — 向前走到第一个实现此方法的 handler
- `skipContext()` (`line 945-954`): 两层判断:
  1. `(executionMask & (MASK_ONLY_INBOUND | mask)) == 0` — handler 完全不实现任何 Inbound 事件 → skip
  2. `(executor() == currentExecutor && (executionMask & mask) == 0)` — 同一 executor AND handler 不实现此具体事件 → skip (不同 executor 不可跳，需 offload 保证顺序)
- `invokeHandler()` 检查 handlerState == ADD_COMPLETE
- Type dispatch (`line 347-358`): DON'T CHANGE — JDK-8180450 JIT 优化 bug，必须分类型调用以触发内联
- `ChannelHandlerMask.java:38-55` — 17 个位: MASK_CHANNEL_READ=1<<5, MASK_WRITE=1<<15, MASK_FLUSH=1<<16

**Code type**: Algorithmic (mask-based event routing)

**设计权衡**: 如果每个 fireChannelRead 都遍历全部 handler → O(N²)。掩码跳过 = 从当前节点出发一次 O(1) 跳转到下一个匹配 handler。ChannelHandlerMask + skipContext 保证传播路径最优——只调用真正实现此方法的 handler。不同 executor 不可跳过（即使 handler 没实现此方法也需 fire 以保证线程切换）。

**Conclusion**: Inbound = next 遍历 + 掩码跳过 + 类型分发。skipContext 的两层判断是关键——同一 executor 跳过，不同 executor 不可跳（需要 fire 去 offload 线程）。source: AbstractChannelHandlerContext.java:341-369,927-954, ChannelHandlerMask.java:38-55
