# RM-1 remoting 协议层 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | 协议定型: 4+4 帧格式 (总长+头长) + JSON 序列化 + opaque 匹配 + processorTable 分发 — 至今未变 (RemotingCommand 核心结构) |
| 4.x 中期 | TLS 面 (TlsSystemConfig 系统属性); ChannelEventListener; RPCHook 认证挂点 |
| **5.0 (2022)** | **ROCKETMQ 二进制序列化** (RocketMQSerializable — JSON 之外第二序列化); **rpc/ 层** (RpcClient gRPC 桥 — proxy 对接); metrics (rpcLatency, OTel); RemotingCodeDistributionHandler (分发优化) |
| 5.1+ | **fastEncodeHeader 零拷贝** (ByteBuf 直写) + FastCodesHeader 接口 (免反射头); LITE_PULL_MESSAGE=361 长轮询独立码; interruptPullRequests (关连接中断拉取) |

## 痕迹证据

- RemotingCommand.java:50-51: RPC_TYPE/RPC_ONEWAY 位定义 (初版即定)
- RemotingCommand.java:245-247: markProtocolType (类型<<24 | 头长)
- RemotingCommand.java:450-473: fastEncodeHeader (5.x 零拷贝注释风格)
- NettyRemotingAbstract.java:96: responseTable 注释 (opaque 匹配)
- NettyRemotingClient.java:561-572: 超时分级关闭注释 ("avoid close the success channel if left timeout is small")
- SerializeType.java: JSON/ROCKETMQ 双枚举
- rpc/ 目录: 5.x proxy 协议桥
- RequestCode.java: 160 常量 (LITE_PULL_MESSAGE=361, RAFT_BROKER_HEART_BEAT=1018 5.x 新增)

## 推断标注

- "3.x 协议定型" — RocketMQ 公知版本线, 仓库无 release note 实证 (标注)
- "fastEncodeHeader 5.1+" — 代码风格推断 (标注)
- "rpc/ 5.0" — 与 proxy 模块同代推断 (标注)
- 仓库为 RocketMQ 主仓库 (非 git 浅克隆) — 但未做 git 考古, 版本线为代码结构推断, 已逐条标注
