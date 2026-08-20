# RM-13 Proxy+安全 — gRPC 协议层与双协议翻译

> 前置: [[RM-1-协议]] (remoting 协议/请求码/双实现) + [[RM-5-Broker]] (认证管线) + [[RM-11-路由]] (proxy 时代路由弱化) | 收官域 | 对照: Kafka 无代理直连 vs gRPC 网关
> 🟡 B | 6 KP | [模式: 多协议网关 + 统一消息面 + 认证链]
> Pass 2 闭环: q1(拓扑双模式) q2(gRPC 层) q3(双协议翻译) q4(统一消息面) q5(认证一句话) q6(客户端面)

**读者处境**: 5.x 客户端怎么连的? 老 remoting 客户端还能用吗? 签名认证怎么做的? 这篇拆 proxy: 多协议网关 (gRPC+remoting 同端口协商) + 统一消息面 + 双认证链。

### 1. 拓扑与双模式 — LOCAL 内嵌 / CLUSTER 独立

场景: proxy 怎么部署?
源码路径:
- **ProxyMode 枚举** (LOCAL/CLUSTER, ProxyStartup:180-213): **CLUSTER** → DefaultMessagingProcessor.createForClusterMode (独立 proxy, 远程转发 broker); **LOCAL** → **内嵌 BrokerController** (BrokerStartup.createBrokerController) + createForLocalMode (同进程直写)
- **三端口**: grpcServerPort=**8081** (5.x 客户端) / remotingListenPort=**8080** (老 remoting 客户端) / metricsPromExporterPort=**5557**; **会话/上下文过期**: channelExpiredInSeconds=**60** / contextExpiredInSeconds=**30**; **LOCAL 生命周期耦合**: 内嵌 broker 启动失败 → proxy 启动失败 (同进程)
- **ProxyConfig** (1528 行): grpc 线程池 16+2×PROCESSOR_NUMBER / 队列 **100000** / **grpcMaxInboundMessageSize=130MB** / maxMessageSize=4MB / rocketmqMQClientNum=6 (cluster 模式内嵌客户端数) / channelExpiredInSeconds=60 / contextExpiredInSeconds=30
关键设计 (q1): **LOCAL = 部署便捷 (proxy+broker 一体), CLUSTER = 网关独立 (水平扩展)**; 双协议同进程共存。[模式: 双模式部署]

### 2. gRPC 协议层 — 服务面 + 管线

场景: gRPC 请求怎么进来?
源码路径:
- **GrpcMessagingApplication** (464 行): MessagingServiceGrpc.MessagingServiceImplBase — **v2 协议服务实现** (sendMessage/receiveMessage/ack/pop/query); **addExecutor 按类型分流** (producer/consumer/clientManager/transaction 独立线程池, 拒绝策略 flowLimit TOO_MANY_REQUESTS)
- **RequestPipeline 管线** (GrpcMessagingApplication.create L145-158): **pipe 语义 = 最后声明的先生执行** (RequestPipeline:28-33) → 执行序 **ContextInit → Authentication → Authorization** (authConfig 非空才挂双认证)
- **GrpcServerBuilder/ProxyAndTlsProtocolNegotiator**: gRPC 与 TLS 协商 (tlsTestModeEnable 默认 true)
关键设计 (q2): **gRPC 服务面 + 函数式管线** (认证/授权/上下文初始化); 线程池按请求类型隔离 + 满则流控。[模式: gRPC 服务]

### 3. 双协议翻译 — 多协议协商 + remoting 接入

场景: 老 remoting 客户端怎么进 proxy?
源码路径:
- **协商位置 (5 次 REVIEW 修正)**: 协商在 **8080 remoting server** (MultiProtocolRemotingServer, **enableRemotingLocalProxyGrpc** 配置: HandshakeHandler→IdleState→ProtocolNegotiationHandler); **'PRI ' magic (0x50524920) 判定 HTTP/2** → **Http2ProxyFrontendHandler 本地转发到 LOCAL_HOST:8081** ("LocalProxyGrpc" 单端口暴露方案; ⚠ PRI 4 字节前缀碰撞风险代码自认 L49-50); 否则 **RemotingProtocolHandler match 恒真兜底** (复用 remoting 完整管线)
- **gRPC 8081 独立性**: 独立 io.grpc **NettyServerBuilder** (GrpcServerBuilder:49-53, 自带 boss/worker loop) + **ProxyAndTlsProtocolNegotiator** (grpc 侧 TLS; 8080 侧 TLS 走 MultiProtocolTlsHelper HandshakeHandler — 双端口各自 TLS)
- **RemotingProtocolServer** (382 行): **请求码分发注册** (registerProcessor): SEND_MESSAGE/SEND_MESSAGE_V2/SEND_BATCH → sendMessageActivity; PULL/LITE_PULL/**POP** → pullMessageActivity; HEART_BEAT/UNREGISTER/CHECK_CLIENT → clientManagerActivity; END_TRANSACTION → transactionActivity; 各 activity 独立线程池 (L191-217+)
- **翻译面**: **RemotingConverter** (remoting/common): RemotingCommand ↔ gRPC 模型转换; http2proxy 目录 = 转发代理 (Frontend/Backend/HAProxyMessageForwarder) 非翻译
关键设计 (q3): **端口职责**: 8081 = gRPC 独立服务; 8080 = remoting + (配置开启时) HTTP/2 本地代理转发; 老 remoting 请求在 proxy 内**翻译成语义操作再走统一消息面** — 非透明转发。[模式: 协议协商]

### 4. 统一消息面 — MessagingProcessor + 本地直写/远程转发

场景: 两种协议怎么共用逻辑?
源码路径:
- **MessagingProcessor** (337) / **DefaultMessagingProcessor** (364): 统一语义面 — sendMessage/popMessage/ackMessage/pullMessage/updateConsumerOffset/endTransaction/queryMessage (L158-230); gRPC 与 remoting 双入口都收敛于此
- **LocalMessageService** (484): 内嵌 broker → **直接调 broker processor** (brokerController.getSendMessageProcessor().processRequest, L115) — 同进程免网络; 仅包一层 RemotingCommand 转换
- **ClusterMessageService**: 独立模式 → 经内嵌 RocketMQ 客户端 (rocketmqMQClientNum=6) 远程发 broker; **CLUSTER 双段认证**: 客户端→proxy 签名认证 + **proxy→broker 内嵌 SessionCredentials** (createForClusterMode L126 解析)
- **MessageQueueSelector** (proxy/service/route, 314): proxy 侧队列选择 (gRPC 客户端无路由概念 — proxy 代选)
关键设计 (q4): **双协议 → 统一消息面 → 本地直写或远程转发**; proxy 承担客户端路由职责 (gRPC 客户端只发地址)。[模式: 语义统一]

### 5. 认证一句话 — SignAuthentication + ACL 迁移

场景: 5.x 怎么认证?
源码路径:
- **管线执行序**: ContextInit → **AuthenticationPipeline** (认证: 签名验证) → **AuthorizationPipeline** (授权: 资源权限检查) (GrpcMessagingApplication:153-155; 声明序反转)
- **AuthenticationPipeline** (grpc/pipeline): 调 auth 模块 AuthenticationEvaluator → 上下文 → **username 写入 AUTHORIZATION_AK header** (L74-76) — **认证结果透传后续面** (认证与授权/业务解耦)
- **auth 模块** (5025 行, 一句话): authentication/ + authorization/ 分治 — **DefaultAuthenticationHandler 链**: `AclSigner.calSignature(content, password)` 比对签名 (L64-66); **签名算法 = HmacSHA1** (acl/common/AclSigner:30 DEFAULT_ALGORITHM); Stateful/Stateless 双策略; 元数据 provider (Local) + **AuthMigrator (acl→auth 迁移, 229 行)**
- **AuthorizationPipeline**: DefaultAuthorizationContextBuilder → 权限判定失败拒
- **broker 侧**: AuthorizationPipeline/AuthenticationPipeline 同构挂载 (RM-5 已见) — **proxy 与 broker 双面同链**
关键设计 (q5): **签名认证 (HmacSHA1, 无状态) + 授权 (资源表, 有状态) 两段式**; acl 旧体系经 AuthMigrator 平滑迁移。[模式: 认证链]

### 6. 客户端接入面 — 5.x gRPC 客户端

- **5.x 客户端 (grpc 面)**: client-proxy 直连 8081; retry: grpcClientProducerMaxAttempts=**3** + backoffMultiplier=**2** (指数退避); 长轮询批量 grpcClientConsumerLongPollingBatchSize=**32**
- **POP 语义统一**: 老 remoting 走 POP 码 (LITE_PULL/POP) 也能经 proxy — RM-8 POP 面在 proxy 收敛
- **无状态网关**: proxy 不存消费状态 (在 broker) — 可水平扩展; 客户端会话 (ProducerManager) 在 broker 侧 (RM-5)

### 负面空间 — proxy 刻意不做的事

- **不做透明代理**: 老 remoting 客户端经 proxy 是**翻译** (协议转换) 非字节转发 — 需客户端版本兼容
- **不做 proxy 集群状态**: 无状态网关, 但 LOCAL 模式 proxy 挂 = broker 也挂 (同进程)
- **不做 gRPC 深度流控**: 130MB 入站上限粗粒度 (对照 remoting 16MB)
- **不做端到端加密默认**: 客户端→broker 明文; TLS 需显式配置 (tlsTestModeEnable 默认 true 仅测试)
- **不做认证默认强制**: authConfig 为 null 时管线跳过认证 (配置驱动)
- **不替代 namesrv**: 路由仍在 namesrv (proxy 只是接入面, RM-11 弱化交叉)

→ 收官: 13 域全景 — 协议→存储→生产→消费→路由→HA→网关
