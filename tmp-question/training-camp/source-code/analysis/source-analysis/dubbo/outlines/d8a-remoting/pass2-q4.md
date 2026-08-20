# D-8a 传输抽象 + exchange — Pass 2 闭环 Q4: 线程模型 (Dispatcher SPI)

> 核心: Dispatcher SPI 5 实现 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: IO 线程与业务线程怎么分? 5 种线程模型差异? 重连怎么处理?**

## 机制链 (已实证)

```
Dispatcher SPI (@SPI(AllDispatcher.NAME) 默认 all, Dispatcher.java:28):
├── all=AllDispatcher — 所有事件 (连接/消息/异常) 进业务线程池
├── direct=DirectDispatcher — IO 线程直接处理 (不切线程)
├── message=MessageOnlyDispatcher — 仅消息事件进线程池 (连接事件 IO 线程)
├── execution=ExecutionDispatcher — 请求/响应/心跳分流 (消息处理与连接分离)
└── connection=ConnectionOrderedDispatcher — 连接事件有序 (同连接串行)

实现 = WrappedChannelHandler 族 (包装 ChannelHandler, 装饰器):
├── AllChannelHandler → dispatch 到 ExecutorRepository 线程池
└── 线程池: Dispatcher 参数 (dispatch=all) → URL 驱动 (D-1)

NettyClient 重连 (netty4/NettyClient.java):
├── doConnect (L158): 连接失败/断开 → 重连 (L167/175)
└── 重连逻辑: 定时重试 (连接恢复自愈)
```

## 关键设计 (why)

1. **IO/业务线程分离**: Netty IO 线程只做收发, 业务逻辑进线程池 — IO 线程不阻塞 (吞吐核心)
2. **5 模型按场景选**: all (默认, 简单) / direct (低延迟小流量) / execution (大流量精细) — dispatch 参数切换
3. **装饰器链**: WrappedChannelHandler 包装 — 线程模型与 ChannelHandler 解耦
4. **自动重连**: 客户端断线定时重试 — 网络抖动自愈 (配合 D-5 FailbackRegistry 语义)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| @SPI 默认 all | Dispatcher.java:28 |
| SPI 注册表 5 实现 | dubbo-remoting-api META-INF org.apache.dubbo.remoting.Dispatcher |
| AllChannelHandler (WrappedChannelHandler) | dispatcher/all/AllChannelHandler.java |
| ExecutionDispatcher | dispatcher/execution/ExecutionDispatcher.java:26 |
| NettyClient.doConnect 重连 | netty4/NettyClient.java:158-184 |

## 负面空间 (Q4 面)

- 不动态换线程模型 (dispatch 静态参数)
- 不 IO 线程处理业务 (默认 all 切线程池; direct 例外显式选)
- 不做背压 (线程池满 → 拒绝/等待策略, 无原生背压)
