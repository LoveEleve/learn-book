# D-4 RPC 调用 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (dubbo-rpc-api + dubbo-cluster/filter + dubbo-rpc-dubbo)
> 09 域级审计: 执行计划 DB-4 断言 "InvokerInvocationHandler→Filter 链→ProtocolFilterWrapper→NettyClient" — 已逐条 grep, 见文末审计表

## 入口展开 (Level-1~3, 已读源码)

### Level-1: InvocationUtil.invoke (proxy/InvocationUtil.java:39-106) ← D-3 桥接点

```
InvocationUtil.invoke(invoker, rpcInvocation)
├── RpcContext.storeServiceContext (上下文保存)
├── setTargetServiceUniqueName(serviceKey) + setConsumerUrl (消费 URL 绑定)
├── Profiler 简单性能追踪 (超时告警: 执行时间 vs timeout 对比)
└── return invoker.invoke(rpcInvocation).recreate()   ← 核心: 同步化 (recreate 取回结果)
    └── invoker = ClusterInvoker (D-7 容错黑盒)
```

### Level-2: Filter 链构建 — ProtocolFilterWrapper (dubbo-cluster! 3.x 归属)

```
ProtocolFilterWrapper.refer (cluster/filter/ProtocolFilterWrapper.java:66-73)
├── registry URL → 直接透传 (注册中心引用不走消费 Filter)
└── 非 registry → builder.buildInvokerChain(protocol.refer(url), REFERENCE_FILTER_KEY, CONSUMER)
    └── DefaultFilterChainBuilder.buildInvokerChain (L43-77):
        ├── getActivateExtension(url, key, group=CONSUMER)   ← D-1 激活面消费! (@Activate 筛选)
        ├── 多 ModuleModel → 过滤排序去重 (sortingAndDeduplication)
        └── 倒序包装: for (i = size-1 → 0) last = CopyOfFilterChainNode(original, next, filter)
            + CallbackRegistrationInvoker 收尾 → 责任链逐层包
```

### Level-3: DubboInvoker.doInvoke (protocol/dubbo/DubboInvoker.java:89-161) — 协议发送核心

```
DubboInvoker.doInvoke(invocation)
├── 附件: PATH_KEY + VERSION_KEY
├── clientsProvider.getClients() → 多连接轮询: index.getAndIncrement() % size  ← 连接选择
├── isOneway 判断 → request.setTwoWay(false); client.send(request) → 空结果返回 (单向)
├── timeout = RpcUtils.calculateTimeout (<=0 → TIMEOUT_TERMINATE 直接终止)
├── Request 构建: payload + version + data=RpcInvocation
└── twoWay=true:
    ├── executor = getCallbackExecutor
    ├── CompletableFuture<AppResponse> = currentClient.request(request, timeout, executor)
    ├── FutureContext 兼容 2.6 (setCompatibleFuture)
    └── AsyncRpcResult(appResponseFuture, inv)  ← 异步核心
异常翻译: TimeoutException → RpcException(TIMEOUT) / RemotingException → SERIALIZATION/NETWORK
```

## 09 域级审计表 (执行计划 DB-4 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "InvokerInvocationHandler" | proxy/InvokerInvocationHandler.java:34-100 (D-3 已实证: Object 拦截 + RpcInvocation 构建) | 接受 (D-3 已覆盖入口) |
| "Filter 链" | DefaultFilterChainBuilder.buildInvokerChain L43-77 (getActivateExtension + 倒序责任链) | 接受 (**3.x 归属 dubbo-cluster**! 非 rpc-api) |
| "ProtocolFilterWrapper" | cluster/filter/ProtocolFilterWrapper.java:40-73 (export L52-59 / refer L66-73, registry URL 透传) | 接受 (**执行计划未提: registry URL 透传面 + CONSUMER group 参数**) |
| "NettyClient" | DubboInvoker → currentClient.request → ExchangeClient (remoting 面, D-8 深潜) | 接受 (D-8 深入; D-4 到 ExchangeClient 黑盒) |
| 执行计划未提: 异步核心 | AsyncRpcResult + CompletableFuture + recreate 同步化 | 补锚 |
| 执行计划未提: 多连接轮询 | clientsProvider.getClients() + index % size | 补锚 |
| 执行计划未提: isOneway 单向 | RpcUtils.isOneway → send 不等待 | 补锚 |
| 执行计划未提: 超时计算 | RpcUtils.calculateTimeout + TIMEOUT_TERMINATE | 补锚 |

## 展开完成度 (三次 REVIEW 后)

1. ~~ExchangeClient 族~~ ✅ (深审: SharedClientsProvider/ExclusiveClientsProvider + ReferenceCount/LazyConnect, DubboProtocol:462-481)
2. ~~AsyncRpcResult.recreate~~ ✅ (q4: InvokeMode 三模式 + FUTURE 自动检测)
3. ~~RpcContext~~ ✅ (q1 + 三次 REVIEW: 3.x 三件套 getServiceContext/getClientAttachment/getServerAttachment L204/170/179)
4. ~~消费端 Filter 实例~~ ✅ (q2: 18 个 internal 注册表穷举)
5. ~~服务端面~~ ✅ (三次 REVIEW T1: HeaderExchangeHandler.received → handleRequest → DubboProtocol.reply 属 D-8 服务端面, 本域消费端聚焦边界成立)
6. ~~集群级 Filter 链~~ ✅ (三次 REVIEW: buildClusterInvokerChain + AbstractCluster L92 join 织入 — 双链层次)
