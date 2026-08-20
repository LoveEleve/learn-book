# D-8a 传输抽象 + exchange — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 骨架: Exchangers/Transporters 双门面 + Transporter SPI (netty3/mina/grizzly) + exchange 层 (HeaderExchange/DefaultFuture) + Dispatcher 线程模型 + HeartbeatHandler |
| 2.6.x | DefaultFuture 异步化 (CompletableFuture 化前身); HeartbeatTimerTask 族 |
| 2.7.x | **DefaultFuture extends CompletableFuture** (全链路异步改造, D-4 呼应); netty4 成为主流 |
| 3.x | **NettyPortUnificationTransporter (多协议端口)** + netty3 退役; AbstractTimerTask 统一 |
| 3.3.x | dubbo-remoting-api 118 + netty4 29 稳定 |

## 痕迹证据

- Exchangers.java:33-49: bind/connect 门面 (2.x 锚)
- HeaderExchangeChannel.java:135-165: request → DefaultFuture.newFuture (2.x 锚)
- DefaultFuture.java:51: **extends CompletableFuture<Object>** (2.7+ 锚 — 全链路异步根源)
- DefaultFuture.java:63: FUTURES ConcurrentHashMap (2.x 锚)
- HeartbeatHandler.java:33-100 + Constants.java:156-157: DEFAULT_HEARTBEAT = 60*1000 (2.x 锚)
- Dispatcher.java:28: @SPI(AllDispatcher.NAME) (2.x 锚)
- NettyPortUnificationTransporter: 3.x 多协议端口 (3.x 锚, 类名实证)
- NettyClient.java:158-184: doConnect 重连 (2.x 锚)

## 推断标注

- "2.6 异步前身" — Dubbo 公知版本线 (标注)
- "2.7 CompletableFuture 化" — DefaultFuture 继承关系实证 (实证)
- "3.x PortUnification" — 类名/SPI 注册实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 以注释锚 + 类结构为主 (降级说明)

## 对照线 (已交付/待交付)

- Netty (N-2 EventLoop/N-3 Pipeline): IO 线程模型 vs Dubbo Dispatcher — 线程对照
- ZK (4.3): NettyServer 服务端 vs Dubbo NettyServer — 服务端对照
- gRPC (G-2): 服务端线程模型 vs Dubbo — 框架对照
