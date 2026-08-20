# 闭环笔记 Q1 — xDS 控制面: 订阅模型 + ADS 流 + ACK/NACK

假设: XdsClientImpl 是控制面客户端 — 按资源类型 (CDS/EDS/LDS/RDS) 订阅, ADS 双向流上 ACK/NACK + version 协商, 断线退避重连。

验证过程:
- **订阅模型** (XdsClientImpl.java:251-280): `watchXdsResource(type, resourceName, watcher, executor)` — syncContext 内 (L252): resourceSubscribers (类型→名→ResourceSubscriber) + subscribedResourceTypeUrls (L98) → manageControlPlaneClient (L273, 按 authority 选/建控制面连接) → adjustResourceSubscription (L277)
- **资源类型** (XdsResourceType.java:41-77): typeUrl 抽象 (L70); **LDS/CDS 全量返回 vs RDS/EDS 增量** (L76-77 注释: "For LDS and CDS resources, the server must return all resources that the client has subscribed to in each request. For RDS and EDS, the server may only return...")
- **version 协商** (ControlPlaneClient.java:74-77): versions Map — "Last successfully applied version_info for each resource type. A version_info is used to update management server with client's most recent knowledge"
- **ACK/NACK** (L209-231): ACK — "Sending ACK for {0} update, nonce: {1}, current version: {2}" (L215); NACK — 带 errorDetail (L231)
- **断线重连**: retryBackoffPolicy/rpcRetryTimer (L89-90, G-6 退避); 断线 sendDiscoveryRequests 全量重发 (L291-306)
- 测试: ControlPlaneClientTest/GrpcXdsClientImplV3Test (V3 = xDS v3 协议)

代码类型: Implementation (控制面协议客户端)

结论: xDS 控制面 = **订阅-推送协议**: 客户端声明资源订阅 (watch), 服务端经 ADS 流推送 (全量/增量按类型), 客户端 ACK/NACK 确认 (nonce 对应), version_info 让服务端知道客户端状态; 断线按资源类型全量重发。**被放弃的方案: 客户端轮询控制面** — ADS 长连接推送 (服务端主动) 避免轮询延迟与负载; NACK 机制让错误显式反馈给控制面。 [跨域: G-3 syncContext; G-6 退避重连; G-8 RLS 是独立数据面协议 (对照)] [协议: xDS v3/ADS] (XdsClientImpl.java:251-280; ControlPlaneClient.java:74-77,191-231)
