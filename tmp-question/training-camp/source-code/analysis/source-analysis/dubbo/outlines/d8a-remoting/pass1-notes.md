# D-8a 传输抽象 + exchange — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 3.3.7-SNAPSHOT (dubbo-remoting-api 118 + netty4 29 文件)
> 09 域级审计: 执行计划 DB-2 "Netty 启动" 一笔带过 — 本域深潜, 已逐条 grep, 见文末审计表

## 入口展开 (Level-1~4, 已读源码)

### Level-1: Exchangers 门面 + Transporter SPI

```
Exchangers.bind/connect (exchange/Exchangers.java:33-49) — 门面:
└── getExchanger (SPI: header=HeaderExchanger 默认) → HeaderExchanger.bind/connect (L41/47)
    └── 1) Transporters.bind (传输层) 2) HeaderExchangeServer (exchange 包装)

Transporter SPI (META-INF 注册表):
├── netty4=NettyTransporter (主流)
├── netty3=NettyTransporter (旧) / netty=NettyPortUnificationTransporter (多协议端口)
└── mockTransporter (测试)
Transporters.bind (transport/Transporters.java, D-1 已见) → getTransporter(url).bind(url, handler)
```

### Level-2: HeaderExchangeChannel.request (D-4 黑盒兑现!)

```
HeaderExchangeChannel.request (exchange/support/header/HeaderExchangeChannel.java:135-165):
├── closed 检查 → RemotingException
├── Request 构建: version + twoWay=true + data (非 Request 参数包装)
├── **DefaultFuture.newFuture(channel, req, timeout, executor)** ← 异步核心!
│   └── DefaultFuture: request id → future 映射 (响应回来按 id 找)
└── channel.send(req) (→ NettyChannel → netty4)
```

### Level-3: 心跳 + 响应回填

```
HeartbeatHandler (exchange/support/header/HeartbeatHandler.java:33-100):
├── HEARTBEAT_KEY 参数 (默认 0 = 关?)
├── isHeartbeatRequest 判断 → HeartBeatRequest → 回 HeartBeatResponse
└── 收到心跳响应/心跳请求处理
HeaderExchangeHandler.received:
├── 响应 (Response) → DefaultFuture.received(response) → future.complete (异步回填)
└── 请求 (Request) → handleRequest → handler.reply (服务端面)
Timer 任务族: AbstractTimerTask + CloseTimerTask (+HeartbeatTimerTask/ReadWriteTimeoutTimerTask 兄弟)
```

### Level-4: netty4 实现 + 线程模型

```
NettyTransporter (netty4):
├── bind → NettyServer (ServerBootstrap: boss/worker 线程组)
└── connect → NettyClient (Bootstrap + 重连)

Dispatcher SPI (线程模型, 5 实现穷举):
├── all=AllDispatcher (默认: 所有事件进业务线程池)
├── direct=DirectDispatcher (IO 线程直接处理)
├── message=MessageOnlyDispatcher (仅消息)
├── execution=ExecutionDispatcher (请求/响应分流)
└── connection=ConnectionOrderedDispatcher (连接事件有序)
```

## 09 域级审计表 (执行计划 DB-2 "Netty 启动" 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "Netty 启动" (DB-2 一笔带过) | Exchangers.bind → Transporters.bind → NettyTransporter → NettyServer — 完整链存在 | 接受 (本域深潜兑现) |
| 执行计划未提: exchange 层 | ExchangeChannel/ExchangeServer/Exchanger + HeaderExchange* | 补锚 |
| 执行计划未提: DefaultFuture 异步 | DefaultFuture.newFuture (request id → future) | 补锚 |
| 执行计划未提: 心跳 | HeartbeatHandler + HEARTBEAT_KEY + timer 任务族 | 补锚 |
| 执行计划未提: 线程模型 | Dispatcher SPI 5 实现 (all 默认) | 补锚 |
| 执行计划未提: 多协议端口 | NettyPortUnificationTransporter (netty=) | 补锚 |

## 待展开 (下一层)

1. DefaultFuture 完整 (id 生成/future 表/超时)
2. HeaderExchangeHandler.received → DefaultFuture.received 回填
3. NettyServer/NettyClient 细节 (线程组/重连)
4. Dispatcher 默认值 + 线程池 (ExecutorRepository 关联)
5. Codec2 编解码抽象 (协议编解码入口)
