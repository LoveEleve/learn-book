## Loop Note: Q4 — ChannelFuture isVoid 零分配

**Hypothesis**: VoidChannelPromise 是 Netty 的零分配优化——每次 write() 不创建新的 DefaultChannelPromise（64字节+），而是返回一个永不完成的预分配 Promise。用 isVoid() 标记区分真实 Promise 和虚空 Promise。

**Verification**:
- `VoidChannelPromise` (`VoidChannelPromise.java:27`) — `final class extends AbstractFuture<Void> implements ChannelPromise`
- `isVoid()` (`line 226`) — `return true` — 标记此 promise 是虚空的
- `isDone()` (`line 124-126`) — `return false` — 故意永不完成，没有状态转换
- `isSuccess()` (`line 129-131`) — `return false`
- `setSuccess()` (`line 172-174`) — no-op, `return this` — 不存储结果
- `tryFailure(cause)` (`line 177-180`) — `fireException0(cause)` → 直接传播异常到 Channel Pipeline 的 exceptionCaught
- `addListener()` — 注册 listener 但永不触发 (promise 永远不完成)
- `DefaultChannelPipeline.voidPromise` (`DefaultChannelPipeline.java:68,94`) — 每条 Pipeline 一个实例
- `AbstractChannel.voidPromise()` (`AbstractChannel.java:281-282`) — 委托给 `pipeline.voidPromise()`

**Code type**: Implementation (allocation optimization)

**设计权衡**: write() 每次返回 ChannelPromise → 如果每次创建新对象 → 频繁 GC 压力。VoidChannelPromise = 永不完成、不走 listener、不存 result、直接传播失败。高频写路径上省掉 `new DefaultChannelPromise()`。write() 通过 `AbstractChannel.newPromise()` vs `voidPromise()` 区分——有 flush 监听的复杂路径使用真实的 promise。

**结论**: VoidChannelPromise = 零分配的承诺。isVoid() 标记让 Channel 判断这是虚空承诺还是需要通知的真实 Promise。failure 绕过 promise 直接 fireChannelExceptionCaught。source: VoidChannelPromise.java:27,124-131,172-226, DefaultChannelPipeline.java:68
