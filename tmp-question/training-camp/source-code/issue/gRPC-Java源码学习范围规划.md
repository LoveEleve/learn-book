# gRPC-Java 源码学习范围规划

> **版本**: v1.83.1
> **仓库**: `/data/workspace/source-code/code/spring/grpc-java/`
> **规模**: 989 个源文件，20+ 子模块
> **日期**: 2026-08-03

---

## 一、仓库概况

gRPC-Java 是 Google 开源的 HTTP/2 + Protobuf RPC 框架的 Java 实现。核心机制：**Protobuf 编译器生成 Stub → Channel 抽象传输层 → ClientInterceptor/ServerInterceptor 拦截链 → Netty HTTP/2 传输 → 四种调用模式（Unary/ServerStream/ClientStream/BiStream）**。

**核心模块**：

| 模块 | 职责 | 状态 |
|---|---|---|
| `core/` | 核心引擎：Channel/Server/Transport/Stream/Interceptor | ✅ |
| `stub/` | Stub 生成：阻塞 Stub、Future Stub、Async Stub | ✅ |
| `api/` | API 接口定义（GeneratedMessage 等） | 淘汰 |
| `protobuf/` | Protobuf 编解码 | ✅ |
| `netty/` | Netty HTTP/2 Transport | ✅ |
| `services/` | 服务定义：HealthCheck/Reflection/Channelz | 淘汰 |
| `context/` | gRPC Context 传递（类似 ThreadLocal 跨线程） | ✅ |
| `compiler/` | Gradle 插件（protoc 代码生成） | 淘汰 |
| `auth/` `alts/` `authz/` `binder/` `census/` `xds/` | 安全/RPC绑定/监控/xDS | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| G-1 | **Channel + Stub 调用链** | ManagedChannel, ClientCall, AbstractStub, ClientCalls | **Stub 三层**：`BlockingStub`(同步阻塞—`future.get()`)、`FutureStub`(ListenableFuture—可组合监听)、`AsyncStub`(StreamObserver 回调)；**调用链**：`stub.sayHello(request)` → `ClientCalls.blockingUnaryCall(channel, method, options, request)` → `channel.newCall(method, options)` → `ClientCallImpl` → `ClientTransport.newStream()` → Netty HTTP/2 帧编码发送；**Channel 抽象**：`ManagedChannel` 管理连接池 + NameResolver 服务发现 + LoadBalancer 负载均衡——类似 Dubbo 的 Cluster+Registry+Transport 三合一 |
| G-2 | **Server 服务端** | ServerImpl, ServerCall, ServerCalls, ServerTransportListener | **服务端启动**：`ServerBuilder.forPort(port).addService(serviceImpl).build().start()` → `ServerImpl` 初始化 Netty Server → `ServerTransportListener` 接收 HTTP/2 Stream → `ServerCallImpl` 包装请求/响应 → `ServerCalls` 分发到业务 `@Override` 方法；**生命周期**：`start()` → `awaitTermination()` → `shutdown()` → `shutdownNow()` |
| G-3 | **Interceptor 拦截链** | ClientInterceptor, ServerInterceptor, ForwardingClientCall | **ClientInterceptor**：`interceptCall(MethodDescriptor, CallOptions, Channel, next)` → 返回 `ClientCall` 包装器→可在发请求前修改 Metadata(headers)、发请求后拦截 Response/错误/完成事件；**ServerInterceptor**：`interceptCall(ServerCall, Metadata, next)` → 返回 `ServerCall.Listener` 包装器——可在收到请求前验证 auth、处理后写 headers；**典型用途**：认证 Token 注入、超时控制、Deadline 传播、Metrics 收集、Tracing 注入/提取 |
| G-4 | **Stream 流式调用** | ClientCall, ServerCall, StreamObserver | **四种模式**：① `Unary`(单请求单响应—默认) ② `ServerStreaming`(Client→1req→Server→Nres→onCompleted) ③ `ClientStreaming`(Client→Nreq→Server→1res) ④ `BidiStreaming`(双向流—Client↔Server→Nreq↔Nres)；**流控**：`request(int numMessages)` 请求更多消息——`StreamObserver.onReady()` 回调触发；**半关闭**：`clientCall.halfClose()` 通知 Server 不再发送更多请求 |

### 🟡 扩展域（2 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| G-5 | **gRPC Context 与 Deadline** | Context, Contexts, Deadline | **Context**：gRPC 的跨线程上下文传递（类似 ThreadLocal 但可跨线程/回调传播）——`Context.current().withValue(key, val).run(runnable)`；`Contexts.interceptCall()` 注入到 Interceptor 链；**Deadline**：`Context.current().withDeadlineAfter(duration, scheduler)` → 超时自动取消——服务端检测 `Context.current().isCancelled()` |
| G-6 | **NameResolver + LoadBalancer** | NameResolver(1040行), LoadBalancer(1626行), PickFirstLoadBalancer(202行), RoundRobinLoadBalancer | **服务发现**：`NameResolver` 将逻辑服务名（`dns:///myservice:8080`）解析为物理地址——`ResolutionResult` 返回 `List<EquivalentAddressGroup>` + `Attributes` + `ConfigOrError`；**Listener**：`NameResolver.Listener.onResult()` 回调通知地址变更；**负载均衡**：`LoadBalancer` 抽象 `Subchannel`(单个物理连接)+`SubchannelPicker`(选择器)——`PickFirstLoadBalancer`(选第一个 ready Subchannel 202行)、`RoundRobinLoadBalancer`(轮询)；`Helper.createSubchannel()` 创建 Subchannel→Transport 建立 TCP/HTTP2 连接→`Subchannel.requestConnection()`→`TRANSIENT_FAILURE/CONNECTING/READY/IDLE` 四态 |

---

## 三、淘汰清单

| 模块 | 理由 |
|---|---|
| `api/` | Protobuf 生成代码——编译产物 |
| `compiler/` | protoc 编译器 Gradle 插件 |
| `services/` (Health/Reflection/Channelz) | 辅助服务 |
| `auth/` `alts/` `authz/` | 认证/安全——Production 配用但非核心 |
| `binder/` `android/` | Android 特定 |
| `census/` | OpenCensus 指标集成（已淘汰用 Micrometer） |
| `xds/` | Envoy xDS 控制面 |
| `protobuf-lite/` `protobuf/` | 序列化细节 |
| `testing/` `benchmarks/` `examples/` | 测试/基准/示例 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 4 |
| 🟡 扩展域 | 2 |
| **总域** | **6** |

---

## 五、学习顺序建议

```
G-1 Channel+Stub 调用链（理解从 stub.method() 到 HTTP/2 帧的全链路）
  → G-2 Server 服务端（理解如何接收并分发到业务方法）
    → G-3 Interceptor 拦截链（理解中间件模式）
      → G-4 Stream 流式调用（理解四种 RPC 模式）
        → G-5/G-6 按需深入
```

以上规划完成，共 **4🔴+2🟡=6 域**。
