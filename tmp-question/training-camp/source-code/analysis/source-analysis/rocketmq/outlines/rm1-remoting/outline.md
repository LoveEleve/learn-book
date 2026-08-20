# RM-1 remoting 协议层 — 私有协议与请求-响应分发

> 前置: [[Netty]] (阶段1: LengthFieldBasedFrameDecoder/线程模型) + [[RM-2 存储底层]] (协议消费者) | 引出: [[RM-7 生产]] [[RM-8 消费]] [[RM-11 路由]] [[RM-12 HA]] [[RM-13 Proxy]] | 对照: [[R-28-networking]] (RESP 协议)
> 🔴 A | 6 KP | [模式: 私有协议帧 + 双序列化 + 按 code 分派 + opaque 匹配]
> Pass 2 闭环: q1(帧格式) q2(编解码) q3(分发) q4(客户端调用) q5(服务端面) q6(扩展面)

**读者处境**: RocketMQ 各组件 (Producer/Broker/Namesrv) 怎么通信? 帧长什么样? 一个请求怎么找到处理它的线程? 同步/异步/单向怎么实现? 这篇拆 remoting 协议层: 4+4 帧头、双序列化、processorTable 分派、opaque 匹配。

### 1. 协议帧 — 4+4 头 + 双序列化

场景: 一条消息跨网络, 字节怎么排?
源码路径:
- **帧布局** (RemotingCommand.encode L395-409): `[4B 总长][4B 头长][header][body]`
- **markProtocolType** (L245-247): `(type.getCode() << 24) | (source & 0x00FFFFFF)` — **高 8 位序列化类型 + 低 24 位头长** (头长上限 16MB)
- **双序列化** (SerializeType): JSON=0 (默认) / **ROCKETMQ=1 二进制** (5.x RocketMQSerializable — 头字段序: code short → language byte → version short → opaque int → flag int → remark → map[FastCodesHeader 免反射+extFields short 键/int 值]); 系统属性 "rocketmq.remoting.serializeType" 可配; **suspended 仅 JSON 路径传输, 二进制头无此字段** (服务端内存标记, 功能不受影响)
- **字段集**: code/version/opaque/flag/remark/extFields (customHeader 反射展开)/body
- **flag 仅 2 位** (L50-51): RPC_TYPE (bit0 请求/响应) / RPC_ONEWAY (bit1 单向); **suspended 是独立字段** (长轮询挂起标记)
- **零拷贝** (fastEncodeHeader L450-473, 5.x): ByteBuf 直写 + FastCodesHeader (免反射)
关键设计 (q1): **类型信息藏进头长的高字节** = 零额外开销的协议协商; 双序列化让 5.x 可换二进制而协议不破坏。[模式: 私有协议帧]

### 2. 编解码 — 长度字段分帧

场景: TCP 粘包/拆包怎么处理?
源码路径:
- **NettyDecoder** (L34-48): `LengthFieldBasedFrameDecoder(16MB, 0, 4, 0, 4)` — 偏移 0 的 4 字节长度字段分帧; 帧内 RemotingCommand.decode (按 serializeType 反序列化)
- **FRAME_MAX_LENGTH**: 默认 16MB (系统属性 "com.rocketmq.remoting.frameMaxLength")
- **Encoder**: fastEncodeHeader 反向
- **协议错误即断连** (L54-60): 解码异常 → closeChannel (对照 Redis R-28 同策略)
关键设计 (q2): **Netty 长度字段框架复用** (阶段1 已学) — RocketMQ 只定制帧内解码; 16MB 上限防巨型帧攻击。[模式: 分帧解码]

### 3. 分发 — 按 code 分派线程池 + opaque 匹配

场景: 请求来了谁处理? 响应怎么找到等待者?
源码路径:
- **类型分派** (processMessageReceived L177): REQUEST → 请求处理 / RESPONSE → 响应处理
- **请求** (processRequestCommand L258): `processorTable.get(code)` → **Pair<processor, ExecutorService>** (每处理器独立线程池!) → 未匹配 → defaultRequestProcessorPair 兜底 → **rejectRequest() 限流** → RequestTask 提交线程池 (IO 线程零阻塞)
- **响应** (processResponseCommand L385): `opaque` → responseTable (ConcurrentMap) → 移除 → InvokeCallback 异步 / putResponse 唤醒同步等待者; 未匹配 → warn
- **writeResponse** (L207): opaque 回填 + markResponseType + 异步写 + metrics (rpcLatency)
关键设计 (q3): **code → 独立线程池 = 命令面的隔离** (拉取慢不影响心跳); opaque = 请求 ID 匹配 (异步协议的关键)。[模式: 请求-响应分发]

### 4. 客户端调用 — 三模式 + 长连接

场景: 同步/异步/单向怎么选? 超时怎么办?
源码路径:
- **invokeSync** (NettyRemotingClient:540): getAndCreateChannel (长连接缓存) → **剩余时间扣减** (取通道耗时计入超时) → Future 等待 → 成功更新 lastResponseTime
- **超时分级关闭** (L561-572): `left > MIN_CLOSE_TIMEOUT_MILLIS || left > timeoutMillis/4` 才关 channel — "avoid close the success channel if left timeout is small" (取通道慢时左超时小, 关掉可惜)
- **Async**: responseTable 预注册 + 回调; **Oneway 双端语义**: 客户端不创建 Future, 服务端 writeResponse 直接返回 (L225-228) — 双向无响应 (fire-and-forget)
- **长连接缓存** (channelTables): 复用 + 定时扫描; **namesrv 故障切换**: 地址列表更新 → shuffle 随机化 + chosen 失效关闭 (L499-530); 取通道轮询切换 (namesrvIndex 递增 + tryLock 防并发, L633-660)
- **中断拉取** (interruptPullRequests L593): 关 broker 连接时中断 PULL_MESSAGE(11)/LITE_PULL(361) — 5.x 长轮询唤醒
关键设计 (q4): **超时预算从取通道就开始扣** — 端到端超时语义; 分级关闭防误杀健康连接。[模式: 三模式调用]

### 5. 服务端面 — 160 请求码

场景: 服务端注册什么?
源码路径:
- **registerProcessor** (NettyRemotingServer:339): code → Pair<processor, executor> + default 兜底; **rejectRequest() 由处理器实现** (broker 侧基于负载/连接数拒绝 — 限流面, RM-5 交叉); **线程默认值** (NettyServerConfig): worker 8 / selector 3 / 连接空闲上限 120s
- **RequestCode 160 常量** (grep 穷举): PULL_MESSAGE=11 / HEART_BEAT=34 / LITE_PULL_MESSAGE=361 (5.x) / RAFT_BROKER_HEART_BEAT=1018 (HA)
- **心跳**: 客户端定期注册生产/消费信息
- **双实现**: NettyRemotingServer + proxy 内嵌 remoting (5.x rpc/)
关键设计 (q5): **160 请求码 = 协议命令面的注册表**; 5.x 新码 (361/1018) 向后兼容 (枚举增量)。[模式: 命令注册表]

### 6. 扩展面 — RPCHook + TLS + 5.x rpc 桥

场景: 认证/加密/proxy 怎么接入?
源码路径:
- **RPCHook** (doBeforeRequest/doAfterResponse): 认证/统计挂点 (5.x SignAuthentication 接入 — RM-13); **rpc/ 层 = remoting 高层调用抽象** (RpcClientImpl 包装 RemotingClient, BrokerOuterAPI 使用 — 非 gRPC; gRPC 在 proxy 模块 grpc/ 目录); **proxy 双协议翻译层** (RemotingProtocolHandler: 老客户端 remoting 经 proxy 转 grpc, RM-13 交叉)
- **TLS** (TlsSystemConfig): tls.enable/tls.server.mode/tls.config.file 系统属性 + TlsHelper
- **RPCHook** (doBeforeRequest/doAfterResponse): 认证/统计挂点 (5.x SignAuthentication 接入 — RM-13); **TLS** (TlsSystemConfig): tls.enable/tls.server.mode/tls.config.file 系统属性 + TlsHelper; **metrics**: rpcLatency (OTel) / ChannelEventListener (连接事件)
- **metrics**: rpcLatency (OTel) / ChannelEventListener (连接事件)
关键设计 (q6): **协议内核 + 扩展插槽** (hook/TLS/桥) — 认证与协议解耦。[模式: 扩展插槽]

### 负面空间 — remoting 刻意不做的事

- **不做协议协商**: serializeType 服务端全局配置, 非按连接协商 (对照 gRPC)
- **不做背压调度**: 线程池拒绝即报错, 无排队控制 (对照 Kafka)
- **不做多路复用升级**: 一连接一请求流 (opaque 足够, 无 HTTP/2 级多路复用)
- **不做压缩**: 帧无压缩标志 (大消息裸传)
- **不做消息追踪内建**: 靠 RPCHook 扩展

→ 引出: 生产者怎么用它发消息? → [[RM-7 Producer]]
