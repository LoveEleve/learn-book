# D-9 Triple 协议 — Pass 2 闭环 Q3: 传输与流控面 (transport/ + PING + GOAWAY)

> 核心: protocol/tri/transport/ + TriplePingPongHandler | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: HTTP/2 传输事件怎么处理? 连接保活/优雅停机/流控?**

## 机制链 (已实证)

```
transport/ 面 (protocol/tri/transport/):
├── H2TransportListener + AbstractH2TransportListener — HTTP/2 传输事件 (onMetadata/onData, D-8b 呼应)
├── **TripleHttp2LocalFlowController / TripleHttp2RemoteFlowController** — 本地/远端流控
├── **TripleGoAwayHandler** — HTTP/2 GOAWAY 帧处理 (连接关闭协商)
├── **TripleCommandOutBoundHandler** — 出站命令 (D-8b 写命令队列配合)
└── **GracefulShutdown** — 优雅停机 (在途请求处理完再关)

保活与协议面:
├── **TriplePingPongHandler** (TriplePingPongHandler.java:28): ChannelDuplexHandler
│   — HTTP/2 PING 帧处理 + pingAckTimeout (L30-35)
└── TripleHttp2Protocol extends AbstractWireProtocol (L81):
    ├── frameLogger 配置 (L116)
    └── **WINDOW_UPDATE 控制** (L221: 防止 netty 自动发送 — 自定义流控接管)
```

## 关键设计 (why)

1. **自定义流控**: 接管 WINDOW_UPDATE (L221) — 配合 Local/Remote FlowController 精确控速 (呼应 D-8b request(count))
2. **GOAWAY 优雅停机**: 连接关闭前协商在途请求 — 无损重启
3. **PING 保活**: HTTP/2 层 PING (非应用层心跳) — 连接健康探测
4. **命令出站**: TripleCommandOutBoundHandler — 写路径命令化 (与 D-8b HttpWriteQueue 同构)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 流控控制器 (Local/Remote) | transport/TripleHttp2*FlowController.java |
| TripleGoAwayHandler | transport/TripleGoAwayHandler.java |
| GracefulShutdown | transport/GracefulShutdown.java |
| PING 处理 | TriplePingPongHandler.java:28-35 |
| WINDOW_UPDATE 接管 | TripleHttp2Protocol.java:221 |

## 负面空间 (Q3 面)

- 不做连接级负载均衡 (HTTP/2 连接复用, 节点选择在 D-6)
- 不做 PING 主动策略 (仅响应/超时处理)
- 不做多连接合并 (单 h2 连接多路复用)
