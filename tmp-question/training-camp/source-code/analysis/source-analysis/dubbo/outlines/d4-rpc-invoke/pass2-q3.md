# D-4 RPC 调用 — Pass 2 闭环 Q3: 协议发送 (DubboInvoker.doInvoke)

> 核心: DubboInvoker.doInvoke (protocol/dubbo/DubboInvoker.java:89-161) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Filter 链末端的协议 invoker 怎么把调用发出去? 连接怎么选? 超时/单向怎么处理?**

## 机制链 (已实证)

```
DubboInvoker.doInvoke(invocation)               DubboInvoker.java:89-161
├── 附件: PATH_KEY (服务路径) + VERSION_KEY (协议版本)
├── clientsProvider.getClients() → ExchangeClient 列表 (多连接!)
│   └── 单连接 → 直接用; 多连接 → index.getAndIncrement() % size 轮询  ← 连接级负载
├── isOneway = RpcUtils.isOneway(url, invocation) (oneway=true 单向)
├── timeout = RpcUtils.calculateTimeout(url, invocation, methodName, DEFAULT_TIMEOUT)
│   └── timeout <= 0 → TIMEOUT_TERMINATE 直接返回超时异常 (无时间可等)
├── invocation 附件: TIMEOUT_KEY
├── Request 构建: payload (PAYLOAD 参数) + version + data=RpcInvocation
├── 单向分支: request.setTwoWay(false) → currentClient.send(request, isSent) → 空结果 (newDefaultAsyncResult)
└── 双向分支 (twoWay=true):
    ├── executor = getCallbackExecutor(url, inv)  ← ExecutorRepository + ThreadPool SPI (fixed/cached/limited/eager 4 实现)
    ├── CompletableFuture<AppResponse> = currentClient.request(request, timeout, executor)
    │   └── .thenApply(AppResponse::cast)  ← ExchangeClient.request (remoting 面, D-8 深潜)
    ├── FutureContext.setCompatibleFuture (2.6 兼容, Zipkin TraceFilter 等)
    └── AsyncRpcResult(appResponseFuture, inv) + setExecutor → 返回 (异步!)

timeout 倒计时 (RpcUtils.calculateTimeout L280-288):
├── TIME_COUNTDOWN 附件 → getTimeout (方法级参数)
└── ENABLE_TIMEOUT_COUNTDOWN → 超时倒计时传递远端 (服务端也知剩余时间)

异常翻译:
├── TimeoutException → RpcException(TIMEOUT_EXCEPTION, "Invoke remote method timeout...")
└── RemotingException → RpcException(SERIALIZATION_EXCEPTION 或 NETWORK_EXCEPTION, 按 cause 分类)
```

## 关键设计 (why)

1. **连接轮询**: clientsProvider 多连接 (共享/独占), 请求级轮询分散 — 连接级负载均衡 (区别于 D-6 节点级)
2. **isOneway 单向**: 通知类调用 (不关心结果) 直接 send 不等待 — 性能面
3. **timeout 前置计算**: 剩余时间不足直接终止 (TIMEOUT_TERMINATE) — 避免白等
4. **全异步 + 最后同步**: request 返回 CompletableFuture → AsyncRpcResult — 与 q1 的 recreate 两拍呼应, 3.x 全链路异步
5. **异常分级**: Serialization vs Network 分类 (cause 判断) — 可诊断性

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| doInvoke 全流程 | DubboInvoker.java:89-161 |
| 连接轮询 (index % size) | 同上 L98-101 |
| isOneway 分支 | 同上 L110-118 |
| timeout 计算 + TIMEOUT_TERMINATE | 同上 L105-109, 119-125 |
| Request 构建 + twoWay | 同上 L127-144 |
| CompletableFuture + AsyncRpcResult | 同上 L143-151 |
| 异常翻译 (TIMEOUT/SERIALIZATION/NETWORK) | 同上 L153-161 |

## 负面空间 (Q3 面)

- 不做请求重试 (重试在 D-7 ClusterInvoker, 本层一次发送)
- 不做连接池管理 (连接生命周期在 remoting, D-8)
- 不做序列化细节 (序列化在 D-10, 本层 Request 组装)
