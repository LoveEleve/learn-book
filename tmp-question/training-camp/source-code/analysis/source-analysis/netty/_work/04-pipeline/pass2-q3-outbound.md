## Loop Note: Q3 — Outbound 传播

**Hypothesis**: write/flush 沿 prev 反向遍历——从当前 handler 回到 HeadContext。HeadContext 是出站终点，call unsafe.write(msg, promise)。

**Verification** (`AbstractChannelHandlerContext.java`):
- `write()` (`line 780-804`): `findContextOutbound(flush ? MASK_WRITE|MASK_FLUSH : MASK_WRITE)` — 写+刷合并优化
- `flush()` (`line 742-771`): `findContextOutbound(MASK_FLUSH)` — 独立的 flush 传播
- `findContextOutbound(int mask)` (`line 936-942`): `do ctx = ctx.prev while skipContext(ctx, executor, mask, MASK_ONLY_OUTBOUND)` — 向后走到第一个匹配 handler
- Type dispatch (`line 746-760`): 四种类型分发: HeadContext→ChannelDuplexHandler→ChannelOutboundHandlerAdapter→ChannelOutboundHandler
- `HeadContext.write()` (`DefaultChannelPipeline.java:1385-1386`): `unsafe.write(msg, promise)` — 终止点
- `HeadContext.flush()` (`line 1390-1392`): `unsafe.flush()` — 终止点
- `ensurePromiseUseCorrectExecutor()` (`line 788`): 如果 promise 绑定到不同的 EventLoop → 确保通知在正确线程上

**Code type**: Algorithmic (reverse event propagation)

**设计权衡**: 出站和入站使用同一条 skipContext 方法但反向遍历。write+flush 合并: `MASK_WRITE | MASK_FLUSH` 一次找到实现两者的 handler，减少两次查找。Type dispatch 对称——和 inbound 使用完全相同的结构，只改变 handler 类型。

**Conclusion**: Outbound = prev 反向遍历 + write/flush 合并掩码 + HeadContext 终止。入站/出站共用 skipContext 方法链——传播算法完全相同，方向相反。source: AbstractChannelHandlerContext.java:742-804,936-942
