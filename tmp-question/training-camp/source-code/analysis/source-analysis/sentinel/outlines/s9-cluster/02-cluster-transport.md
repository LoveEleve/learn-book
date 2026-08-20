# client 与 server 通信

> S-9 中篇。本文讲集群 client 与 server 之间的通信：请求如何封装、Netty 如何传输、异步响应如何关联。

## 悬念

`ClusterStateManager` 只负责切换模式，真正的 token 请求要靠 client 发到 server。一个请求发出后，client 怎么拿到 server 的响应？靠轮询吗？

不，靠 Netty 的异步通道 + 请求 ID 关联。

## 一、DefaultClusterTokenClient:逻辑封装

`DefaultClusterTokenClient` 实现 `ClusterTokenClient`，内部持有 `ClusterTransportClient transportClient`。

`requestToken` 是入口：

```java
public TokenResult requestToken(Long flowId, int acquireCount, boolean prioritized) {
    // 构造 ClusterRequest,调 sendTokenRequest
    TokenResult result = sendTokenRequest(request);
    return result;
}
```

`sendTokenRequest` 是核心：

```java
private TokenResult sendTokenRequest(ClusterRequest request) throws Exception {
    if (transportClient == null) {
        return new TokenResult(TokenResultStatus.FAIL);
    }
    ClusterResponse response = transportClient.sendRequest(request);
    TokenResult result = new TokenResult(response.getStatus());
    // 复制 waitInMs / tokenId 等字段
    return result;
}
```

它把参数构造成 `ClusterRequest`，通过 transportClient 发出，拿到 `ClusterResponse` 后转成 `TokenResult`。如果 transportClient 为 null（未连接），返回 `FAIL`。

## 二、NettyTransportClient:传输层

真正的网络传输由 `NettyTransportClient` 完成。它用 Netty 的 `Bootstrap` 连接 server：

```java
Bootstrap b = new Bootstrap();
b.group(eventLoopGroup)
    .channel(NioSocketChannel.class)
    .option(ChannelOption.TCP_NODELAY, true)
    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ClusterClientConfigManager.getConnectTimeout())
    .handler(new ChannelInitializer<SocketChannel>() {
        public void initChannel(SocketChannel ch) {
            clientHandler = new TokenClientHandler(currentState, disconnectCallback);
            ChannelPipeline pipeline = ch.pipeline();
            // 加 LengthField 编解码 + TokenClientHandler
        }
    });
```

几个关键点：

- `NioSocketChannel`：非阻塞客户端
- `TCP_NODELAY`：禁用 Nagle 算法，降低延迟
- `PooledByteBufAllocator`：池化缓冲，减少分配
- `CONNECT_TIMEOUT_MILLIS`：连接超时来自配置
- `TokenClientHandler`：处理收到的响应

## 三、请求-响应异步关联

这是通信层最巧妙的部分。`NettyTransportClient.sendRequest` 发出请求后不会同步等待，而是通过 `TokenClientPromiseHolder` 关联：

```java
ChannelPromise promise = ...
TokenClientPromiseHolder.put(requestId, promise);
// 异步等待 promise 完成
```

请求发出时，用 `requestId` 作为 key 把 `ChannelPromise` 存进 `TokenClientPromiseHolder`。Netty 收到 server 响应后，`TokenClientHandler` 解析出 `requestId`，从 holder 取出对应的 promise 并完成它。

发送线程在 promise 上等待，收到响应后被唤醒拿到结果。这样：

- 发送线程不阻塞在 IO 上
- 多请求并发时靠 requestId 区分各自的响应
- 一个连接的多个请求可以并发，不必排队

## 四、DefaultEmbeddedTokenServer 与 NettyTransportServer

server 侧由 `DefaultEmbeddedTokenServer` 实现 `EmbeddedClusterTokenServer`，内部委托 `NettyTransportServer`：

```java
public void start() throws Exception {
    server.start();
}
public void stop() throws Exception {
    server.stop();
}
```

`NettyTransportServer` 是服务端，监听端口，接受 client 连接，处理 token 请求。它是 Netty 的服务端版：`ServerBootstrap`、`NioServerSocketChannel`、处理连接的 handler。

## 五、server 端 token 请求处理

server 收到 client 的 token 请求后，交给 `TokenService` 处理。`TokenServiceProvider` 用 `SpiLoader.loadFirstInstanceOrDefault()` 解析全局 token 服务（`DefaultTokenService`）。

处理流程（下篇详述）：

- 普通流控 → `ClusterFlowChecker`
- 参数流控 → 参数 checker
- 并发流控 → `ConcurrentClusterFlowChecker`

server 算完结果后，把 `TokenResult` 编码回 client。

## 悬念回收

集群 client-server 通信是：

1. `DefaultClusterTokenClient` 封装逻辑，构造请求、解析结果
2. `NettyTransportClient` 用 Netty Bootstrap 连接 server
3. 请求发出时用 `requestId` 关联 promise 存进 holder
4. Netty 收到响应后按 requestId 完成对应 promise，唤醒发送线程
5. server 侧 `NettyTransportServer` 监听，`TokenService` 处理请求

整个通信是异步的：client 不轮询，靠请求 ID 关联响应。

## 锚点

- `DefaultClusterTokenClient.java:45-47`
- `DefaultClusterTokenClient.java:150-163`
- `DefaultClusterTokenClient.java:206-220`
- `NettyTransportClient.java:61-113`
- `NettyTransportClient.java:222-235`
- `DefaultEmbeddedTokenServer.java:37-43`
