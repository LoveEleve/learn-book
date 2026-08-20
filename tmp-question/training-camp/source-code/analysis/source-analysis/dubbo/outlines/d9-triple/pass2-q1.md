# D-9 Triple 协议 — Pass 2 闭环 Q1: 协议装配面 (TripleProtocol + pathResolver)

> 核心: TripleProtocol (L62-233) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Triple 协议怎么装配? 服务路径怎么注册? 连接怎么建?**

## 机制链 (已实证)

```
TripleProtocol (protocol/tri/TripleProtocol.java, 233 行) @SPI("tri"):
├── export (L106-156, D-2 面): exporterMap.put + **pathResolver.register(invoker)** (L135)
│   ├── REST_ENABLED (H2_SETTINGS_REST_ENABLED) → mappingRegistry.register (L138)
│   ├── ExecutorRepository.createExecutorIfAbsent (服务端流线程池)
│   ├── bindServerPort (L166-191) — 端口绑定
│   └── optimizeSerialization (D-2 深审锚)
├── refer (L193-204): optimizeSerialization → streamExecutor →
│   **Http3Exchanger.isEnabled(url) ? Http3.connect : PortUnificationExchanger.connect(url, DefaultPuHandler)**
│   ← HTTP/2 连接 (D-8b 基座!) → TripleInvoker
└── pathResolver (PathResolver SPI, L64/79): frameworkModel.getDefaultExtension
    — gRPC 风格路径 (/package.Service/Method) 注册/注销 (L135/116) + destroy (L227)
```

## 关键设计 (why)

1. **HTTP/2 单连接多路复用**: PortUnificationExchanger 复用 D-8b 传输栈 — 协议层只管应用语义
2. **pathResolver = 服务寻址**: gRPC 风格路径注册表 — 兼容 gRPC 生态 (grpc-client 可调 dubbo)
3. **REST_ENABLED 双协议**: 同一协议实例同时暴露 RPC + REST (mappingRegistry) — 端口/连接复用
4. **双端对称**: export (server) / refer (client) 都走 optimizeSerialization + 线程池 — 与 D-2/D-3 面一致

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| export (exporterMap + pathResolver + REST) | TripleProtocol.java:106-156 |
| refer (PortUnificationExchanger.connect) | TripleProtocol.java:193-204 |
| pathResolver 获取 | TripleProtocol.java:64,79 |
| bindServerPort | TripleProtocol.java:166-191 |
| REST_ENABLED 双协议 | TripleProtocol.java:73,92,119,138 |

## 负面空间 (Q1 面)

- 不做多连接自动扩缩 (HTTP/2 多路复用, 单连接够)
- 不做协议协商降级 (h2 必须, 无 h1 降级)
- 不做路径通配注册 (精确路径)
