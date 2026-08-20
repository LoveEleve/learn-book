# D-8a 传输抽象 + exchange — Pass 2 闭环 Q1: 传输抽象面 (Exchangers + Transporter SPI)

> 核心: Exchangers 门面 + Transporter SPI + netty4 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 传输层怎么抽象? 门面怎么到具体实现? 换传输框架要改什么?**

## 机制链 (已实证)

```
Exchangers.bind/connect (exchange/Exchangers.java:33-49) — 门面 (静态, 线程安全):
└── getExchanger (SPI: header=HeaderExchanger 默认) → HeaderExchanger.bind/connect (L41/47)
    └── 1) Transporters.bind (传输层) 2) HeaderExchangeServer (exchange 包装, q2)

Transporter SPI (META-INF 注册表穷举):
├── netty4=NettyTransporter (主流)
├── netty3=NettyTransporter (旧版) / netty=NettyPortUnificationTransporter (多协议端口合一)
└── mockTransporter (测试面)
Transporters.bind → getTransporter(url).bind(url, handler) — URL 自适应 (D-1)

NettyTransporter (netty4 29 文件):
├── bind → NettyServer: ServerBootstrap + boss/worker 线程组 + NettyServerHandler
└── connect → NettyClient: Bootstrap + NettyClientHandler (重连)
```

## 关键设计 (why)

1. **三层门面**: Exchangers (exchange 门面) → Transporters (传输门面) → Transporter SPI — 每层可换
2. **URL 自适应**: transporter= 参数选实现 — 换传输框架不改业务代码 (netty3→netty4 平滑)
3. **多协议端口 (PortUnification)**: 一个端口识别多种协议 — 端口节约面
4. **exchange 与 transport 分离**: 协议语义 (request/response/心跳) 与网络实现 (连接/收发) 解耦

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Exchangers.bind/connect 门面 | exchange/Exchangers.java:33-49 |
| HeaderExchanger.bind/connect | exchange/support/header/HeaderExchanger.java:41-47 |
| Transporters.bind (URL 自适应) | transport/Transporters.java (D-1 已见) |
| Transporter SPI 注册表 | dubbo-remoting META-INF (netty4/netty3/netty/mock) |
| NettyServer/NettyClient | dubbo-remoting-netty4 transport/netty4/ |

## 负面空间 (Q1 面)

- 不做传输协议协商 (URL 静态指定)
- 不做自动选传输 (显式 transporter 参数)
- 不做连接池抽象 (连接管理在 exchange/协议面)
