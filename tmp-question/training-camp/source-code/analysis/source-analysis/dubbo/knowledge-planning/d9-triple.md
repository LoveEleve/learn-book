# D-9 Triple 协议 — HTTP/2 应用协议与 gRPC 互通

> 项目: Dubbo | 🟡 Deep / 1 篇 | TripleProtocol(233)+TripleInvoker+ThreadlessExecutor+TripleHttp2Protocol+GrpcHttp2Protocol+PbUnpack
> 基线: DUBBO-PLAN D-9 (协议层, 228 文件) — 前置: **D-8a/D-8b (双基座: remoting 121 + http12 106 import)** — 展开 装配→调用模式→传输流控→兼容面

---

## §0.8

- 🟡 Deep，1篇 — 装配(**TripleProtocol export L106-156[exporterMap+pathResolver.register L135 gRPC 路径+REST_ENABLED→mappingRegistry L138+bindServerPort+optimizeSerialization]; refer L193-204[streamExecutor→PortUnificationExchanger.connect[Http3Exchanger 可选]→TripleInvoker]**) → 调用模式(**TripleInvoker.doInvoke L144-200: isSync→ThreadlessExecutor[common/threadpool L37-56: 任务排队+waitAndDrain 调用线程自执行=零额外线程]/streamExecutor; 三模式 invokeUnary L305/invokeServerStream/invokeBiOrClientStream; DeadlineFuture extends CompletableFuture L38[timeoutListeners L46+HashedWheelTimer 30ms L50]**) → 传输流控(**transport/: H2TransportListener+TripleHttp2Local/RemoteFlowController+TripleGoAwayHandler+GracefulShutdown+TripleCommandOutBoundHandler; TriplePingPongHandler L28 HTTP/2 PING; TripleHttp2Protocol L81 WINDOW_UPDATE 接管 L221**)) → 兼容面(**GrpcHttp2Protocol extends TripleHttp2Protocol L22[标记类]; gRPC 内置服务 service/ 5 文件[TriHealthImpl 健康检查+ReflectionV1AlphaService 反射+SchemaDescriptorRegistry]; 压缩 compressor/ 7 文件[Gzip/Bzip2/Identity]; protobuf PbUnpack L25-41[SingleProtobufUtils writeTo/parseFrom+GLOBAL_REGISTRY L96-132]+PackableMethodFactory 族; h12 服务端 40 文件[AbstractServerTransportListener+BiStreamServerCallListener+CompressibleEncoder]; RestProtocol extends TripleProtocol L21**)
- 设计模式: [模式: HTTP/2 应用协议+三模式调用+零线程同步+双协议面]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| TripleProtocol.java:106-156 | export | **exporterMap+pathResolver.register+REST_ENABLED mappingRegistry+bindServerPort** | High |
| TripleProtocol.java:193-204 | refer | **PortUnificationExchanger.connect (HTTP/2, D-8b 基座) → TripleInvoker** | High |
| TripleInvoker.java:172 | 同步 | **isSync ? ThreadlessExecutor : streamExecutor** — 同步零线程 | High |
| ThreadlessExecutor.java:37-56 | 机制 | **任务排队 + waitAndDrain 调用线程自执行 (注释 L37-38)** | High |
| TripleInvoker.java:180-186 | 三模式 | **invokeUnary / invokeServerStream / invokeBiOrClientStream** | High |
| DeadlineFuture.java:38-53 | deadline | **extends CompletableFuture + timeoutListeners + HashedWheelTimer 30ms** | High |
| TripleHttp2Protocol.java:221 | 流控 | **WINDOW_UPDATE 接管 (防 netty 自动发送)** — 自定义流控 | High |
| TriplePingPongHandler.java:28 | PING | **HTTP/2 PING + pingAckTimeout** | High |
| SingleProtobufUtils.java:96-132 | protobuf | **writeTo/parseFrom + GLOBAL_REGISTRY 动态消息** | High |
| GrpcHttp2Protocol.java:22 | gRPC | **extends TripleHttp2Protocol {} 标记类** — 配置隔离 | High |
| service/ | 内置服务 | **TriHealthImpl 健康检查 + ReflectionV1AlphaService 反射** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: Triple 单机制 (装配+调用+传输+兼容) — 1篇按四段展开; 序列化在 D-10 (导航), 与 gRPC (G-1~G-3) 对照。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 三模式调用 (unary/服务端流/双向流) | 🔴 | **为什么🔴**: 调用模型 |
| P1-2 | ThreadlessExecutor 零线程同步 | 🔴 | **为什么🔴**: 吞吐核心 |
| P1-3 | pathResolver gRPC 路径寻址 | 🔴 | **为什么🔴**: 生态互通 |
| P1-4 | WINDOW_UPDATE 流控接管 | 🔴 | **为什么🔴**: 精确控速 |
| P2-1 | DeadlineFuture deadline 机制 | 🟡 | **为什么🟡**: gRPC deadline |
| P2-2 | gRPC 内置服务 (健康检查/反射) | 🟡 | **为什么🟡**: 生态治理 |
| P2-3 | REST_ENABLED 双协议面 | 🟡 | **为什么🟡**: 多协议 |
| P3-1 | 压缩面 (Gzip/Bzip2 协商) | 🟢 | **为什么🟢**: 压缩细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **协议装配** | 🔴 | 主线 |
| B | **调用模式** | 🔴 | 核心 |
| C | **传输流控** | 🔴 | 可靠性 |
| D | **gRPC/protobuf/REST 面** | 🟡 | 兼容面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 装配面 | export: pathResolver gRPC 风格路径注册 + REST_ENABLED mappingRegistry; refer: PortUnificationExchanger.connect (复用 D-8b 双栈) — 双端对称 (都走 optimizeSerialization+线程池) | TripleProtocol.java:106-204 |
| q2 | 调用模式 | **三模式一协议 (gRPC 调用模型): unary/服务端流/双向流**; 同步用 ThreadlessExecutor (回调由调用线程 waitAndDrain 执行 — 零额外线程); 异步 streamExecutor; DeadlineFuture deadline 超时回调 | TripleInvoker.java:144-200; ThreadlessExecutor.java:37-56; DeadlineFuture.java:38-53 |
| q3 | 传输流控 | **WINDOW_UPDATE 接管 (防 netty 自动发) + Local/Remote FlowController 精确控速**; GOAWAY 优雅停机; HTTP/2 层 PING 保活; 出站命令与 D-8b 写队列同构 | TripleHttp2Protocol.java:221; TriplePingPongHandler.java:28 |
| q4 | 兼容面 | **gRPC 互通三件: 路径 (pathResolver) + 帧/状态兼容 + PING; 内置健康检查/反射服务**; protobuf 序列化 (SingleProtobufUtils); REST 变体 (RestProtocol) 多协议一端口 | GrpcHttp2Protocol.java:22; service/; PbUnpack.java:25-41 |

→ 引出 D-10 序列化: PbUnpack/PackableMethod — protobuf 序列化深潜; 对照 gRPC (G-1~G-3)。
