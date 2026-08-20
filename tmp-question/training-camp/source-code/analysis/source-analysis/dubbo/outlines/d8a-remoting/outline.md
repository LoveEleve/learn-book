# D-8a 传输抽象 + exchange — 门面→异步→心跳→线程模型

> 前置: [[D-1-SPI微内核]] [[D-4-RPC调用]] | 引出: [[D-8b-HTTP传输栈]] [[D-9-Triple协议]] | 对照: Netty (N-1~N-12) + ZK NettyServer
> 🟡 A | 8 KP | [模式: 分层抽象 + 异步注册表 + 心跳]
> Pass 2 闭环: q1(传输抽象) q2(exchange 异步) q3(心跳回填) q4(线程模型)

**读者处境**: "Netty 启动" 背后是什么? request 怎么变成异步 future? 长连接怎么保活? 这篇拆 Exchangers + Transporter SPI + HeaderExchange + DefaultFuture + Heartbeat。

### 1. 传输抽象面 — Exchangers + Transporter SPI

场景: 传输层怎么抽象? 换框架要改什么?
源码路径:
- **Exchangers.bind/connect** (Exchangers.java:33-49) → getExchanger (SPI: header 默认) → HeaderExchanger → **Transporters.bind** → **getTransporter** (URL 自适应)
- **Transporter SPI 注册表**: netty4=NettyTransporter (主流) / netty3 (旧) / **netty=NettyPortUnificationTransporter** (多协议端口合一) / mock
- **NettyTransporter** → NettyServer (**boss/worker 双 EventLoopGroup** L80-105 + IO_THREADS_KEY 参数 L160; **pipeline 五段** L181-190: negotiation(SSL) → decoder/encoder(NettyCodecAdapter) → **server-idle-handler(IdleStateHandler closeTimeout)** → handler) / NettyClient (Bootstrap + 重连)
- ⚠ **编解码面 (Codec2)**: Codec2 @SPI(FRAMEWORK) (L26); **NettyCodecAdapter** (netty4: L43-49) 把 Codec2 接进 netty pipeline — **NEED_MORE_INPUT 半包等待** (L104, 粘包/拆包处理) — 具体协议编解码 (DubboCodec) 属 D-9 面
关键设计 (q1): **三层门面 (Exchangers→Transporters→SPI) + URL 自适应换框架 + 多协议端口**。[模式: 抽象面]

### 2. exchange 异步面 — HeaderExchangeChannel + DefaultFuture (D-4 黑盒兑现)

场景: request 怎么变成 future? 响应怎么回填?
源码路径:
- **HeaderExchangeChannel.request** (L135-165): closed 检查 → Request 构建 (twoWay) → **DefaultFuture.newFuture** → channel.send
- **DefaultFuture extends CompletableFuture** (DefaultFuture.java:51) ← D-4 异步底座根源! **FUTURES 表** (L63: request id → future) + id 关联 (L97-101) + **超时任务** (L107-109: TIME_OUT_TIMER.newTimeout; ⚠ **TimeoutCheckTask** L311+: getFuture → isDone 检查 → executor 执行超时逻辑)
- **HeaderExchangeServer.close(timeout)** (L107-126): 优雅关闭 (服务端 exchange 包装)
- ⚠ **Request id 生成**: mId = newId() (Request.java:51) — **AtomicLong INVOKE_ID 递增** (L34,67,70); 心跳标志 isHeartbeat (L144-145: mEvent && HEARTBEAT_EVENT==mData)
- **received 回填** (L196-209): FUTURES.remove(response.getId()) → complete — HeaderExchangeHandler.received 分流 (Response→回填 / Request→reply)
关键设计 (q2): **id→future 注册表 (无状态网络层关联) + CompletableFuture 无缝衔接 D-4**。[模式: 异步面]

### 3. 心跳面 — HeartbeatHandler + Timer 任务族

场景: 长连接怎么保活?
源码路径:
- **HeartbeatHandler** (L33-100): HEARTBEAT_KEY (⚠ 默认 60s Constants:157) → 心跳请求/响应成对 (**Request.isHeartbeat 标志** L144-145) + 无数据超时探测
- **Timer 任务族**: AbstractTimerTask + **HeartbeatTimerTask** (定时发) / **CloseTimerTask** (空闲关) / ReadWriteTimeoutTimerTask
- 响应路径: **HeaderExchangeHandler.received 分流** (L196-230: Request→handleRequest / Response→handleResponse / String→异常) → Response → DefaultFuture.received
关键设计 (q3): **心跳探测半开连接 + 任务族分工 (保活/清理/超时) + 装饰器链**。[模式: 保活面]

### 4. 线程模型面 — Dispatcher SPI

场景: IO 线程与业务线程怎么分?
源码路径:
- **Dispatcher @SPI(all 默认)** (Dispatcher.java:28): all (全进线程池) / direct (IO 直处理) / message (仅消息) / execution (分流) / connection (连接有序)
- 实现 = **WrappedChannelHandler 族** (装饰器) → ExecutorRepository 线程池
- **NettyClient.doConnect** (L158-184): 断线自动重连
关键设计 (q4): **IO/业务线程分离 (吞吐) + 5 模型按场景选 + 自动重连自愈**。[模式: 线程面]

## 代码类型
Architecture (分层抽象) + Concurrency (异步/线程模型)

## 负面空间 (D-8a, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不传输协议协商 | URL 静态指定 (q1) |
| 不响应乱序重排 | id 关联天然无序安全 (q2) |
| 不 future 复用 | 请求即建, 完成即除 (q2) |
| 不自适应心跳间隔 | 固定 heartbeat 参数 (q3) |
| 不动态换线程模型 | dispatch 静态参数 (q4) |
| 不背压 | 线程池满拒绝/等待, 无原生背压 (q4) |

## 结尾桥 OUTBOUND

- → [[D-8b-HTTP传输栈]]: http12 (HTTP/1.1+H/2) — 传输层 HTTP 面
- → [[D-9-Triple协议]]: 协议构建于 exchange 之上 (triple→remoting 121 import)
- → 对照: Netty (N-2 EventLoop: IO 线程模型 vs Dispatcher) / ZK NettyServer
