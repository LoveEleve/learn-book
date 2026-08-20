# 闭环笔记 Q3 — newCall 三路径: 就绪直通 / 优化内联 / 缓冲排队

假设: channel.newCall 在配置选择器 (ConfigSelector) 未就绪时有三级路径: 就绪直通、内联优化 (inprocess)、PendingCall 缓冲; shutdown 时返回立即失败的 call。

验证过程:
- **第一路径 (就绪)**: `configSelector.get() != INITIAL_PENDING_SELECTOR → newClientCall` (ManagedChannelImpl.java:858-860) — 解析完成后直通
- **第二路径 (内联优化)**: 未就绪 → `syncContext.execute(exitIdleMode)` (L864-868, 懒启动触发解析) → 再查 → 就绪 → newClientCall (L870-874) — 注释: "optimization for the case (typically with InProcessTransport) when name resolution result is immediately available" — 同步解析器立即可用
- **第三路径 (shutdown)**: `return new ClientCall { onClose(SHUTDOWN_STATUS) }` (L878-895) — **立即失败 call**, 不缓冲
- **第四路径 (缓冲)**: `PendingCall` (DelayedClientCall 子类, L897-919) — 快照 context/method/callOptions → syncContext 内加入 pendingCalls (LinkedHashSet) (L902-911); **放行**: updateConfigSelector (L925-933) 遍历 reprocess 逐个 (排队顺序) — 与 G-2 的 serializing executor 排队思想一致
- 配置: InternalConfigSelector (L831, AtomicReference 初始 INITIAL_PENDING_SELECTOR)

代码类型: Glue (调用路由)

结论: newCall 是**条件路由**: 就绪直通零开销; 未就绪先触发懒启动 (exitIdleMode) 并借 syncContext 内联重试; 还不行就 PendingCall 缓冲 (含 Context 快照, 放行时在目标线程恢复); shutdown 则立即失败 (SHUTDOWN_STATUS)。**被放弃的方案: 一律阻塞等待解析** — 阻塞调用线程; 缓冲+异步放行让调用方无感等待。PendingCall 保存 Context 快照是"调用语义冻结" — 放行后仍按发起时的上下文执行。 [跨域: G-1 的 STUB_TYPE_OPTION 经 CallOptions 传播] [并发: AtomicReference 无锁读] (ManagedChannelImpl.java:858-933)
