# D-8a 传输抽象 + exchange — Pass 2 闭环 Q2: exchange 层 (HeaderExchangeChannel + DefaultFuture)

> 核心: HeaderExchangeChannel.request + DefaultFuture (D-4 黑盒兑现!) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: request 怎么变成异步 future? 响应回来怎么回填? 超时怎么处理?**

## 机制链 (已实证)

```
HeaderExchangeChannel.request (support/header/HeaderExchangeChannel.java:135-165):
├── closed 检查 → RemotingException
├── Request 构建: version + twoWay=true + data (参数包装)
├── **DefaultFuture.newFuture(channel, req, timeout, executor)** ← 异步核心
└── channel.send(req) (→ NettyChannel → netty4 写出)

DefaultFuture (exchange/support/DefaultFuture.java):
├── **extends CompletableFuture<Object>** (L51) ← D-4 异步底座的根源!
├── FUTURES = ConcurrentHashMap<Long, DefaultFuture> (L63) — request id → future 表
├── id = request.getId() (L97); FUTURES.put(id, this) + CHANNELS.put (L100-101)
├── newId: 递增 id 生成 (请求唯一标识)
└── 超时: timeoutCheck (L107) + Timeout 任务 (future.timeoutCheckTask)

响应回填 (DefaultFuture.received L196-209):
└── FUTURES.remove(response.getId()) → future 完成 (取消超时任务)
    ← HeaderExchangeHandler.received → DefaultFuture.received (响应路径)
```

## 关键设计 (why)

1. **Request id → future 映射**: 网络层无状态 (字节流), 靠 id 关联请求与响应 — 异步核心数据结构
2. **DefaultFuture extends CompletableFuture**: 与 D-4 的 CompletableFuture 链无缝衔接 (thenApply/recreate)
3. **超时任务**: 请求时注册 Timeout — 响应不来超时自动完成 (异常)
4. **received 幂等**: FUTURES.remove — 响应只处理一次 (重复响应忽略)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| HeaderExchangeChannel.request 完整 | HeaderExchangeChannel.java:135-165 |
| DefaultFuture extends CompletableFuture | DefaultFuture.java:51 |
| FUTURES 表 + id 关联 | DefaultFuture.java:63,97-101 |
| received 回填 | DefaultFuture.java:196-209 |
| HeaderExchangeHandler.received (响应路径) | HeaderExchangeHandler.java:196+ |

## 负面空间 (Q2 面)

- 不做响应乱序重排 (id 关联天然无序安全)
- 不做 future 复用 (请求即建, 完成即除)
- 不阻塞 send (写失败抛 RemotingException)
