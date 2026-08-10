## Loop Note: Q2 — SelectStrategy 决策

**Hypothesis**: SelectStrategy 的三种策略构成一个任务/I/O 优先级决策树——有任务就跳过阻塞 I/O (CONTINUE)，无任务就阻塞等待 I/O (SELECT)，BUSY_WAIT 在 NIO 下退化为 SELECT。

**Verification**:
- `SelectStrategy.java:31,35,39` — 三种策略值: SELECT = -1, CONTINUE = -2, BUSY_WAIT = -3
- `DefaultSelectStrategy.java:29-31` — `calculateStrategy(selectSupplier, hasTasks)`:
  - hasTasks=true → `selectSupplier.get()` → try `selector.selectNow()` → if 0 return CONTINUE, else returnedKeys
  - hasTasks=false → `SelectStrategy.SELECT` → blocking select
- `NioIoHandler.java:423-468` — 使用方: switch on calculateStrategy result
  - CONTINUE → return 0 (跳过 I/O——有任务要跑)
  - BUSY_WAIT → fall through to SELECT (NIO 不支持 busy-wait)
  - SELECT → `select(context, wakenUp.getAndSet(false))` → blocking select + wakenUp race fix
- `NioIoHandler.java:69-73` — selectNowSupplier: IntSupplier that calls `selectNow()` → returns count of ready channels or 0

**Code type**: Algorithmic (scheduling decision)

**决策流**:
```
calculateStrategy(hasTasks):
  hasTasks == true
    → selectNow()           // 非阻塞轮询——快速检查
    → returns 0             // 无就绪通道 → CONTINUE: 跳过 I/O，去跑任务
    → returns N             // 有就绪通道 → N: 直接处理这些通道
  hasTasks == false
    → SELECT                // 无任务可跑 → 阻塞等待 I/O 事件
```

**设计意图**: 任务优先——当一个 EventLoop 有积压任务时，select 不应该阻塞。先执行任务、下次循环再检查 I/O。这样保证任务延迟不被长时间 select 阻塞。但当没有任务时，select 阻塞等待是合法的——因为没有别的事可做。

**Conclusion**: SelectStrategy 不是 I/O 与任务之间的"天平"——是任务优先的"检查哨"。default calculateStrategy 保证: 有任务→不阻塞 I/O，无任务→阻塞等待。BUSY_WAIT 留给 Epoll 等支持该模式的 I/O 实现。source: DefaultSelectStrategy.java:29-31, SelectStrategy.java:31-51, NioIoHandler.java:419-468
