# D-8a 传输抽象 + exchange — 门面分层与异步注册表

> 项目: Dubbo | 🟡 Deep / 1 篇 | Exchangers+Transporters+HeaderExchangeChannel+DefaultFuture+HeartbeatHandler+Dispatcher
> 基线: DUBBO-PLAN D-8a (网络层) — 前置: **D-1 (SPI) + D-4 (ExchangeClient 黑盒兑现)** — 展开 门面→异步→心跳→线程模型

---

## §0.8

- 🟡 Deep，1篇 — 门面(**Exchangers.bind/connect L33-49→getExchanger[header 默认]→HeaderExchanger→Transporters.bind→getTransporter[URL 自适应]; Transporter SPI: netty4/netty3/netty=PortUnification/mock**) → netty4(**NettyServer: boss/worker 双线程组 L80-105+IO_THREADS 参数+pipeline 五段 L181-190[SSL negotiation→decoder/encoder(NettyCodecAdapter)→IdleStateHandler→handler]; NettyClient: doConnect 重连 L158-184**) → 编解码(**Codec2 @SPI L26; NettyCodecAdapter L43-104[NEED_MORE_INPUT 半包等待]**) → 异步(**HeaderExchangeChannel.request L135-165: closed 检查→Request[twoWay]→DefaultFuture.newFuture→send; DefaultFuture extends CompletableFuture L51+FUTURES 表 L63[id→future]+received 回填 L196-209+超时 TimeoutCheckTask L311**) → 心跳(**HeartbeatHandler L33-100: HEARTBEAT_KEY 默认 60s+心跳成对[Request.isHeartbeat L144]+Timer 任务族[Heartbeat/Close/ReadWriteTimeout]**) → 线程(**Dispatcher @SPI(all) L28: 5 实现[all/direct/message/execution/connection]+WrappedChannelHandler 族**)
- 设计模式: [模式: 三层门面+异步注册表+装饰器链]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Exchangers.java:33-49 | 门面 | **Exchangers→HeaderExchanger→Transporters→Transporter SPI** — 三层门面每层可换 | High |
| NettyServer.java:181-190 | pipeline | **五段: SSL→decoder/encoder→IdleStateHandler→handler** — netty 集成细节 | High |
| NettyCodecAdapter.java:43-104 | 编解码 | **Codec2 接进 pipeline + NEED_MORE_INPUT 半包等待** — 粘包/拆包 | High |
| HeaderExchangeChannel.java:135-165 | request | **closed 检查→Request→DefaultFuture.newFuture→send** | High |
| DefaultFuture.java:51,63 | 异步 | **extends CompletableFuture + FUTURES 表 (request id→future)** — D-4 异步底座根源 | High |
| DefaultFuture.java:196-209 | 回填 | **received: FUTURES.remove(id)→complete** — 幂等处理 | High |
| HeartbeatHandler.java:33-100 | 心跳 | **HEARTBEAT_KEY 默认 60s + 心跳请求/响应成对** | High |
| Dispatcher.java:28 | 线程 | **@SPI(all 默认) 5 实现** — IO/业务线程分离 | High |
| Request.java:51,70 | id | **mId = newId(): AtomicLong INVOKE_ID 递增** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 传输层单机制 (门面+异步+心跳+线程) — 1篇按四段展开; 协议编解码 (DubboCodec) 属 D-9 面 (导航), http12 属 D-8b (导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 三层门面 + URL 自适应换框架 | 🔴 | **为什么🔴**: 分层可换 |
| P1-2 | id→future 注册表 (DefaultFuture) | 🔴 | **为什么🔴**: 异步核心 |
| P1-3 | 心跳保活 (成对+Timer 族) | 🔴 | **为什么🔴**: 长连接语义 |
| P1-4 | Dispatcher 线程模型 5 实现 | 🔴 | **为什么🔴**: 吞吐核心 |
| P2-1 | pipeline 五段 (SSL/编解码/空闲) | 🟡 | **为什么🟡**: netty 集成 |
| P2-2 | 半包处理 (NEED_MORE_INPUT) | 🟡 | **为什么🟡**: 粘包安全 |
| P3-1 | PortUnification 多协议端口 | 🟢 | **为什么🟢**: 端口节约 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **门面分层** | 🔴 | 主线 |
| B | **异步注册表** | 🔴 | 核心 |
| C | **心跳保活** | 🔴 | 可靠性 |
| D | **线程模型** | 🟡 | 性能面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 传输抽象 | Exchangers (exchange 门面)→Transporters (传输门面)→Transporter SPI (URL 自适应); 换框架只改 transporter 参数; pipeline 五段 (SSL→编解码→空闲→业务) | Exchangers.java:33-49; NettyServer.java:181-190 |
| q2 | exchange 异步 | **HeaderExchangeChannel.request → DefaultFuture (extends CompletableFuture) → FUTURES 表 (id→future)**; 响应回来 received 按 id 回填 — 网络层无状态靠 id 关联; 超时 TimeoutCheckTask isDone 安全检查 | HeaderExchangeChannel.java:135-165; DefaultFuture.java:51-209 |
| q3 | 心跳 | 心跳成对 (Request.isHeartbeat 标志) 验证双向连通; 默认 60s; Timer 任务族分工 (心跳/关闭/读写超时); 半开连接探测 | HeartbeatHandler.java:33-100 |
| q4 | 线程模型 | IO/业务线程分离 (IO 只收发); Dispatcher 5 模型 (all 默认/direct/message/execution/connection) 按场景选; 自动重连自愈 | Dispatcher.java:28; NettyClient.java:158-184 |

→ 引出 D-8b HTTP 传输栈 (http12); D-9 Triple 协议 (协议构建于 exchange 之上, 121 import)。
