# N-07-03 API 契约面 — Remote 请求/响应协议族 (协议契约篇)

> 前置: [[NC-3-gRPC-Redo]] (传输层) | 对照: 请求协议是 gRPC 序列化的契约
> 🟡 B | 方案 B (重要域) | 闭环: q1(请求族) q2(响应族) q3(回调面)

**读者处境**: gRPC 通道上传的请求对象怎么定义? 请求/响应怎么配对? 回调与 future 怎么组织?

### 1. 请求族 — Request 层级

场景: 请求协议怎么分层?
源码路径:
- **Request** (api/remote/request/Request.java:29): 抽象基类 — implements Payload (序列化契约)
- **InternalRequest** (request/InternalRequest.java:28): 内部请求基类
- **ServerRequest** (request/ServerRequest.java:26): 服务端请求 (ClientDetectionRequest/ConnectResetRequest 等)
- 具体族: ConnectionSetupRequest (L28 连接建立) / HealthCheckRequest / PushAckRequest (推送确认) / ClientDetectionRequest (客户端探测)
- **RequestMeta** (request/RequestMeta.java:33): 请求元数据 (连接信息)
- **Payload** (api/remote/Payload.java:22): 序列化接口 — gRPC proto 映射
关键设计 (q1): **"请求层级 = 内部/服务端/业务分型"** — 连接面 (Setup/Detection/HealthCheck) 与业务面 (Naming/Config 在各自 remote 包) 分型; 全部 implements Payload 保证可序列化。 [模式: 请求分型]

### 2. 响应族与回调面 — Response + Callback + Future

场景: 响应与异步回调怎么组织?
源码路径:
- **Requester** (api/remote/Requester.java:29): 请求者接口
- **RequestFuture** (RequestFuture.java:27) / **DefaultRequestFuture** (DefaultRequestFuture.java:31): 异步 future
- **RequestCallBack** (RequestCallBack.java:29) / **AbstractRequestCallBack**: 回调接口
- **PushCallBack** (PushCallBack.java:25) / **AbstractPushCallBack**: 推送回调
- **RpcScheduledExecutor** (RpcScheduledExecutor.java:29): 调度线程
- **RemoteConstants** (RemoteConstants.java:25): 常量 (LABEL_SOURCE/LABEL_MODULE 等 — NC-3 构造 labels 消费)
关键设计 (q2): **"异步面 = Future + Callback 双通道"** — 请求响应异步化 (Future), 推送有独立回调; 常量面支撑连接标签。 [模式: 异步契约]

### 3. Naming/Config 业务协议 — 各域 remote 包

场景: 业务请求在契约层怎么组织?
源码路径:
- **api/naming/remote/request** (12): SubscribeServiceRequest/RegisterInstanceRequest/InstanceRequest 等 (NC-1 doSubscribe 消费)
- **api/naming/remote/response** (9): SubscribeServiceResponse 等
- **api/config/remote/request** (12): ConfigQueryRequest/ConfigPublishRequest/ConfigBatchListenRequest 等 (NC-2 消费)
- **api/config/remote/response** (10): ConfigQueryResponse 等
- **api/remote/ability** (11): 能力协商请求 (ServerAbilities/ClientAbilities)
关键设计 (q3): **"业务协议 = 按域分包"** — naming/config 各自请求/响应子包; 能力协商 (ability) 支撑 NC-1 的 isAbilitySupportedByServer 路由。 [模式: 域分包协议]

### 4. 测试与行为锚

场景: 协议边界?
源码路径:
- api/grpc/auto (7): proto 自动生成面
- 测试: api/remote test
关键设计 (q1): **"proto 自动生成 = 契约唯一源"** — gRPC proto 生成与手写 Request 并存, Payload 桥接。 [模式: 双源契约]
