# Pass 2 闭环笔记 Q3: client 侧如何请求 token

## 验证过程

- `DefaultClusterTokenClient` 实现 `ClusterTokenClient`，内部持有 `ClusterTransportClient transportClient` (`DefaultClusterTokenClient.java:45-47`)。
- `start()` 创建 `NettyTransportClient(host, port)` 并启动 (`DefaultClusterTokenClient.java:80, 108-111`)。
- `requestToken(flowId, acquireCount, prioritized)` 构造 `ClusterRequest`，调 `sendTokenRequest(request)`，返回 `TokenResult`；失败时返回 `FAIL` (`DefaultClusterTokenClient.java:150-163`)。
- `sendTokenRequest`：通过 `transportClient.sendRequest(request)` 拿到 `ClusterResponse`，把 `response.getStatus()` 放进 `TokenResult`，并复制 `waitInMs`/`tokenId` 等字段 (`DefaultClusterTokenClient.java:206-220`)。
- `NettyTransportClient` 用 Netty `Bootstrap` 连接 server：`NioSocketChannel`、`PooledByteBufAllocator`、`TCP_NODELAY`，pipeline 里加 `TokenClientHandler` 与编解码器 (`NettyTransportClient.java:89-113`)。
- 请求-响应用 `TokenClientPromiseHolder` 按 requestId 关联 Future，异步等待响应 (`NettyTransportClient.java:30-31`)。

## 结论

client 侧请求 token 是“逻辑封装 + Netty 传输”两层：`DefaultClusterTokenClient` 把参数构造成请求并解析 `TokenResult`，`NettyTransportClient` 负责通过 Netty 连到 server 并异步收发，用 requestId 关联响应。