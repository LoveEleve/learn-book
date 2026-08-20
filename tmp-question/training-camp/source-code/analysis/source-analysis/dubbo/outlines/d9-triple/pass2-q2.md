# D-9 Triple 协议 — Pass 2 闭环 Q2: 调用模式面 (unary/流式 + ThreadlessExecutor)

> 核心: TripleInvoker.doInvoke 三模式 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 三种调用模式 (一元/服务端流/双向流) 怎么实现? 同步调用怎么不占线程?**

## 机制链 (已实证)

```
TripleInvoker.doInvoke (L144-200):
├── CompletableFuture<AppResponse> 创建 → 连接不可用 → completeExceptionally
├── **isSync(methodDescriptor, invocation) ? new ThreadlessExecutor() : streamExecutor** (L172)
│   ← 同步: ThreadlessExecutor (调用线程自执行!); 异步: streamExecutor
├── 三模式分支:
│   ├── **invokeUnary(methodDescriptor, invocation, call, executor)** — 一元 (请求-响应)
│   ├── **invokeServerStream(...)** — 服务端流 (ServerStreamObserver, D-8b 呼应)
│   └── **invokeBiOrClientStream(...)** — 双向流/客户端流
└── 返回 AsyncRpcResult (D-4 面一致)

ThreadlessExecutor (dubbo-common/threadpool/ThreadlessExecutor.java:37-56):
├── 注释 (L37-38): "Tasks are stored in a blocking queue and will only be executed when a
│   thread calls waitAndDrain()" — 任务排队, waitAndDrain 时由**调用线程自己执行**
└── waitAndDrain(deadline) (L56) — 同步等待 + 执行回调任务 (零额外线程!)

⚠ DeadlineFuture (DeadlineFuture.java:38-53) — gRPC deadline 机制:
├── extends CompletableFuture<AppResponse> (L38)
├── timeout + **timeoutListeners 回调列表** (L46)
└── **HashedWheelTimer 30ms tick** (L50) — 超时回调触发 (timeoutListeners)
```

## 关键设计 (why)

1. **三模式一协议**: 一元/服务端流/双向流共用 HTTP/2 流 — gRPC 兼容的调用模型
2. **ThreadlessExecutor 零线程同步**: 同步调用不占 IO/业务线程 — 回调任务由调用线程 drain 执行 (吞吐关键!)
3. **D-4 异步底座一致**: 返回 AsyncRpcResult — recreate 同步化统一
4. **流式 = HTTP/2 天然**: ServerStream/BiStream 直接映射 h2 流 (D-8b 流控/写队列配合)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| doInvoke 三模式分支 | TripleInvoker.java:144-200 |
| ThreadlessExecutor vs streamExecutor | TripleInvoker.java:172 |
| ThreadlessExecutor 注释 | dubbo-common threadpool/ThreadlessExecutor.java:37-56 |
| waitAndDrain | ThreadlessExecutor.java:56 |

## 负面空间 (Q2 面)

- 不做客户端流取消恢复 (取消即断)
- 不做消息级背压协商 (固定流控窗口, D-8b)
- 不做 unary 分包聚合 (h2 帧已组合, CompositeInputStream)
