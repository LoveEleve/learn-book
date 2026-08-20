# D-4 RPC 调用 — Pass 2 闭环 Q4: 异步结果与同步化 (AsyncRpcResult)

> 核心: AsyncRpcResult (dubbo-rpc-api) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 异步结果怎么回到调用方? InvokeMode 三态 (SYNC/ASYNC/FUTURE) 语义差异? 异常怎么统一?**

## 机制链 (已实证)

```
invoker.invoke(...).recreate()                 InvocationUtil.java:65
└── AsyncRpcResult.recreate (AsyncRpcResult.java:237-247)
    ├── InvokeMode.FUTURE → RpcContext.getClientAttachment().getFuture()   ← 返回 Future 本身
    ├── InvokeMode.ASYNC  → createDefaultValue(invocation).recreate()      ← 返回默认值 (不阻塞)
    └── InvokeMode.SYNC (默认) → getAppResponse().recreate()               ← 同步等待真实结果
        └── AppResponse: 阻塞拿 CompletableFuture 结果 (value/exception 提取)

InvokeMode 从哪来? (invocation 上)
├── 由调用方配置 (async=true → ASYNC; 返回值 Future → FUTURE 自动检测?)
└── 默认 SYNC (同步调用)

结果载体:
├── AppResponse (dubbo-rpc-api): value + exception + attachments — 统一结果对象
└── CompletableFuture<AppResponse>: 异步链 (request → thenApply cast → AsyncRpcResult 持有)

异常翻译 (q3 已有): 传输层 Timeout/Remoting → RpcException (TIMEOUT/SERIALIZATION/NETWORK)
业务异常: 保留在 AppResponse.exception 内, recreate 时抛出 (getAppResponse().recreate 内部)
```

## 关键设计 (why)

1. **三模式一拍切换**: 同一个 AsyncRpcResult, recreate 时按 InvokeMode 决定返回什么 — 同步/异步/未来三种调用风格共用一条异步链
2. **全链路异步底座**: 发送 (q3) 与接收 (本面) 都是 CompletableFuture — 线程不阻塞, 3.x 性能核心
3. **AppResponse 统一载体**: value/exception/attachments 一体 — 传输层/业务层共享
4. **兼容 2.6 FutureContext**: 老过滤器 (Zipkin) 仍可拿 Future — 迁移面

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| recreate 三模式 | AsyncRpcResult.java:237-247 |
| getAppResponse (同步等待路径) | AsyncRpcResult.java:211-235 (get 族) |
| 创建: new AsyncRpcResult(future, inv) | DubboInvoker.java:150 |
| 单向空结果 | DubboInvoker.java:116-118 (newDefaultAsyncResult) |
| FutureContext 2.6 兼容 | DubboInvoker.java:145-148 |

## 负面空间 (Q4 面)

- 不做取消传播 (AsyncRpcResult 无原生 cancel → invocation 链)
- 不做背压 (结果全量缓冲, 无流式回压 — 流式在 D-9 Triple)
- 不做超时中断 (超时后依赖 provider 端继续执行, 消费端只等不到)
