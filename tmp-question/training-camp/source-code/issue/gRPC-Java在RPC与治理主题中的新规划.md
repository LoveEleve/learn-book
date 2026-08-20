# gRPC-Java 在 RPC 与治理主题中的新规划

> 目标：不再把 gRPC-Java 当成一个孤立仓库按原顺序平推，而是把它作为“RPC 与治理主题”的第一站，承担远程调用运行时基线的角色。  
> 依据：`gRPC-Java源码学习范围规划.md` + `RPC与治理主题总规划.md` + `源码范围规划复盘方法论.md`。

---

## 一、为什么 gRPC-Java 适合作为这一组主题的第一篇正文入口

在“Feign→Dubbo→gRPC→Cloud*4→Nacos→Sentinel”这 76 域里，gRPC-Java 最适合作为第一站，不是因为它最简单，而是因为它最容易和已经完成的 Netty 卷形成直接复用关系。

它有三个天然优势：

### 1. 它直接复用 Netty HTTP/2 主线

前面 Netty 卷已经把：
- HTTP/2 协议地基
- FrameCodec / Multiplex API 层
- ConnectionHandler / Encoder / Decoder 主链
- gRPC / Triple over HTTP/2 桥接

都补齐了。

而 gRPC-Java 的 Netty transport 恰好直接压在这条主线上，因此：
- 很多底层问题不用从零解释
- 可以直接站在“RPC 如何落到既有 transport 主线”这个角度切入

### 2. 它最容易建立“远程调用运行时”的最低心智图

相比 Dubbo、Feign 或 Spring Cloud 集成层，gRPC-Java 的主线更集中：

- Stub
- Channel
- ClientCall / ServerCall
- StreamObserver
- Interceptor
- Netty transport

它足够完整，但还没复杂到被注册中心、SPI、自动配置、配置刷新和治理规则打散。

这使它特别适合先回答：
- 一个本地方法调用如何变成远程调用
- 一个远程响应如何回到调用方
- 流式调用如何和 transport 主线对齐

### 3. 它天然适合作为对照基准

后面不管写：
- Dubbo 的 Invoker / Export / Refer
- Feign 的 MethodHandler / Contract / Client
- Commons / Nacos 的服务发现与负载均衡
- Sentinel / Gateway 的治理动作

都可以随时回头对照 gRPC-Java：
- 这里的调用入口是什么
- 这里的 transport 接口是什么
- 这里如何表示单次调用与流式调用
- 这里是否复用连接级流控与 HTTP/2 语义

所以它适合承担“第一篇远程调用运行时基线”的角色。

---

## 二、gRPC-Java 在这一组主题里不再按单仓顺序写，而按 4 条机制线重排

旧的单仓规划里，gRPC-Java 被拆成 6 个域：
- Channel + Stub
- Server
- Interceptor
- Stream
- Context + Deadline
- NameResolver + LoadBalancer

这些域本身没问题，但若直接按仓库顺序写，仍然会有两个风险：

1. 容易把“客户端 / 服务端 / transport / 流式调用”切得太碎  
2. 容易在后面与 Dubbo、Feign、Commons 的对照时，失去横向基准

因此在“RPC 与治理”主题里，gRPC-Java 应按下面 4 条机制线重排。

### G-RPC-1：调用入口与客户端运行时

回答的问题：
- Stub 到底是什么
- `stub.method(request)` 怎么变成一次远程调用
- `Channel`、`ClientCall`、`ClientCalls` 在这一链上各自承担什么职责

对应旧域：
- G-1 Channel + Stub 调用链
- G-4 Stream 调用模式的一部分

这是第一篇最适合落笔的主线。

### G-RPC-2：服务端运行时与流式交互

回答的问题：
- 服务端如何接住一个 RPC stream
- `ServerCall`、`ServerCalls`、`ServerTransportListener` 的职责边界
- Unary / ServerStreaming / ClientStreaming / BidiStreaming 的运行时差异

对应旧域：
- G-2 Server
- G-4 Stream

这条线应作为第二篇或第一篇后半段素材，但不建议一开始就和客户端主线混写得过重。

### G-RPC-3：拦截器、Context 与 Deadline

回答的问题：
- 客户端 / 服务端拦截器怎么插入调用主线
- Context 和 Deadline 为什么不是“附属工具”，而是 RPC 上下文与取消语义的一部分

对应旧域：
- G-3 Interceptor
- G-5 Context + Deadline

这条线非常适合在“调用主线讲完之后”补成第二篇，因为它是所有 RPC 框架对照时都会反复用到的“横切面机制”。

### G-RPC-4：传输与实例选择桥接层

回答的问题：
- gRPC-Java 怎样通过 Netty transport 落到 HTTP/2
- NameResolver / LoadBalancer 为什么属于调用前链，而不是 transport 内核
- `Subchannel`、`Picker` 和实际连接之间是什么关系

对应旧域：
- G-6 NameResolver + LoadBalancer
- Netty transport 相关类（在旧单仓规划里被放在 G-1 背景里）

这条线不建议作为第一篇，因为它太容易和 Spring Cloud Commons / Nacos 的发现主线发生交叉。更适合在后面写“实例选择与服务发现”大轴时再接进去。

---

## 三、第一批正文不该怎么写

### 1. 不该一上来就把 6 个旧域平均铺开

那样会得到一个“仓库浏览式”正文，而不是“RPC 运行时基线篇”。

### 2. 不该先从 NameResolver / LoadBalancer 开始

它们在“RPC 与治理”大主题里更像“调用发出去之前怎么找人”，而不是“远程调用本身怎么发生”。如果先写，会把 transport 与调用主线打断。

### 3. 不该先从服务端全链路开始

服务端更适合作为客户端调用主线写完之后的镜像补全。否则读者会在第一篇里同时面对：
- 客户端出发
- 服务端接收
- 流式交互
- Netty transport
- 拦截器
- Context

信息量过大。

---

## 四、建议的正文顺序

### 第一篇：gRPC 客户端调用主线

建议标题方向：
- `gRPC-Java：Stub、Channel 与 ClientCall 调用主线`

核心聚焦：
- `AbstractStub`
- `ManagedChannel`
- `ClientCalls`
- `ClientCall`
- Unary 调用的最短闭环
- 点到 Netty transport，但不把 HTTP/2 连接主线重新展开

这篇的使命是先回答：
**一个本地方法调用怎样变成一次远程调用。**

### 第二篇：gRPC 服务端与四种调用模式

建议标题方向：
- `gRPC-Java：ServerCall、ServerCalls 与流式调用模型`

核心聚焦：
- `ServerImpl`
- `ServerCall`
- `ServerCalls`
- `StreamObserver`
- 四种调用模式的差别

这篇的使命是回答：
**远端调用怎样在服务端落地，并且为什么流式调用不是 Unary 的简单放大。**

### 第三篇：Interceptor、Context 与 Deadline

建议标题方向：
- `gRPC-Java：拦截器、上下文传播与 Deadline`

核心聚焦：
- `ClientInterceptor`
- `ServerInterceptor`
- `Context`
- `Deadline`

这篇的使命是回答：
**RPC 中最关键的横切面语义是怎样挂进调用主线的。**

### 第四篇：NameResolver、LoadBalancer 与 Netty transport 桥接

建议标题方向：
- `gRPC-Java：NameResolver、LoadBalancer 与 Netty Transport`

核心聚焦：
- `NameResolver`
- `LoadBalancer`
- `SubchannelPicker`
- `GrpcHttp2ConnectionHandler` / `NettyClientHandler` / `NettyServerHandler`

这篇不必立即开写，更适合在：
- Commons
- Nacos
- Dubbo Registry
这些发现/路由机制写起来之前或并行时再写。

---

## 五、当前结论

在“RPC 与治理”主题里，gRPC-Java 不再按旧单仓的 6 域平均推进，而是应先承担**RPC 调用运行时基线篇**的角色。

因此下一步最合理的动作不是直接写 gRPC 全卷，而是：

1. 先起第一篇正文的 `rewrite-plan`
2. 标题聚焦：`Stub / Channel / ClientCall 调用主线`
3. 控制范围：
   - 不同时吃下服务端、流式、NameResolver、LoadBalancer、Context、Deadline
   - 只建立最小、最稳的客户端调用闭环

**所以，gRPC-Java 在这一组主题中的第一篇，应从客户端调用主线开始。**