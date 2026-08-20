# D-4 RPC 调用 — Pass 2 闭环 Q1: 调用入口与上下文

> 入口: InvocationUtil.invoke (D-3 桥接点) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 用户调用代理方法后, 请求怎么进入 invoker? RpcContext 上下文怎么传递? 同步/异步/未来三种模式怎么选?**

## 机制链 (已实证)

```
InvocationUtil.invoke(invoker, rpcInvocation)     proxy/InvocationUtil.java:39-106
├── RpcContext.storeServiceContext (上下文快照保存, finally restoreServiceContext 恢复)
├── setTargetServiceUniqueName(serviceKey) + setConsumerUrl (消费 URL 绑定到上下文)
├── Profiler 简单性能追踪 (启用时: 执行耗时 vs timeout 对比 → 超时告警日志)
└── return invoker.invoke(rpcInvocation).recreate()   ← 核心两拍:
    ├── invoker.invoke → 进入调用链 (ClusterInvoker → Filter 链 → 协议 invoker, q2/q3)
    └── .recreate() → 同步化取回结果 (q4: InvokeMode 三模式)

invoker 是什么? (D-3 收尾已实证)
├── 代理 handler 持有的是 ClusterInvoker (D-7 容错黑盒, AbstractClusterInvoker.doInvoke L448)
└── ClusterInvoker 内部 → Filter 链 (q2) → DubboInvoker (q3)
```

## 关键设计 (why)

1. **storeServiceContext/restoreServiceContext**: 调用上下文快照+恢复 — 嵌套调用 (A 调 B 再调 C) 不串扰
2. **recreate 两拍结构**: 调用本身全异步 (invoke 返回 AsyncRpcResult), 同步化只在最后一拍 (recreate) — 这是 Dubbo 3.x 全链路异步的底座
3. **Profiler 追踪可选**: 简单 profiler 默认关闭, 开启时记录调用耗时与 timeout 对比 — 可观测面
4. **InvokeMode 三态** (q4 深入): SYNC (默认同步) / ASYNC (异步不阻塞) / FUTURE (返回 Future)

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| invoke 入口 + 上下文保存/恢复 | proxy/InvocationUtil.java:39-106 |
| setConsumerUrl (消费 URL 绑定) | 同上 L47 |
| invoker.invoke().recreate() 两拍 | 同上 L65/L106 |
| AbstractClusterInvoker.doInvoke (容错黑盒) | dubbo-cluster support/AbstractClusterInvoker.java:448-449 |

## 负面空间 (Q1 面)

- 不做调用级同步 (invoke 返回异步结果, 同步化在 recreate)
- 不做跨线程上下文自动传递 (需显式 RpcContext 操作)
- 不做 Profiler 默认开启 (默认关闭, 避免性能开销)
