# RM-1 remoting 协议层 — Pass 1 探索笔记

> 域: RM-1 remoting 协议层 | 🔴 A 方案 | 2026-08-14
> 源码: remoting 模块 (28217 行) 核心: protocol/ + netty/ | RocketMQ 5.3.1

## 调用图

```
协议面 (protocol/):
RemotingCommand (642 行): code/version/language/opaque/flag/remark/extFields/customHeader/body
  帧格式: [4B 总长][4B 头长=markProtocolType(3B 头长+1B serializeType)][header][body] (L403-409)
  双序列化: JSON (默认, RemotingSerializable) / ROCKETMQ 二进制 (RocketMQSerializable — 5.x 新增)
  双编码: encode (ByteBuffer) / fastEncodeHeader (ByteBuf 零拷贝, 5.x; FastCodesHeader 接口)
  flag 位: markOnewayRPC / markResponseType / markSuspended (长轮询)
RequestCode/RemotingCommandType/RemotingSysResponseCode: 请求码/类型/系统响应码枚举

传输面 (netty/):
NettyDecoder (62 行): LengthFieldBasedFrameDecoder (FRAME_MAX_LENGTH=16MB, 偏移 0 长度 4) → RemotingCommand.decode
NettyEncoder (50 行): encode → fastEncodeHeader
NettyRemotingAbstract (731 行) 分发核心:
  processMessageReceived → REQUEST → processRequestCommand / RESPONSE → processResponseCommand
  processorTable (code → Pair<NettyRequestProcessor, ExecutorService>) + defaultRequestProcessorPair
  → RequestTask 提交线程池 (按 code 分线程池!) + rejectRequest 拒绝面
  processResponseCommand: opaque → responseTable 匹配 → 回调/putResponse/release
  RPCHook: doBeforeRequest/doAfterResponse (认证挂点)
  writeResponse: opaque 回填 + markResponseType + metrics (rpcLatency)
NettyRemotingClient (1230 行): invokeSync/Async/Oneway + getChannel (长连接缓存) + 扫描清理
NettyRemotingServer (817 行): 监听/处理器注册/EventExecutorGroup
RequestTask/ResponseFuture: 任务包装 + 异步响应 (Future 信号量)
```

## 基本元素分解

1. **RemotingCommand**: 协议命令 (请求/响应统一结构) + 帧编解码 + flag 位
2. **双序列化**: JSON 默认 vs ROCKETMQ 二进制 (5.x) + fastEncodeHeader 零拷贝
3. **分发**: processorTable 按 code 分派线程池 + opaque 匹配响应
4. **三调用模式**: invokeSync/Async/Oneway
5. **长连接管理**: 客户端 channel 缓存 + 服务端监听
6. **扩展面**: RPCHook (认证) / metrics / TLS / rpc (proxy 协议)

## 标记问题 (20 问)

1. 帧格式各字段位宽? (4+4+header+body; serializeType 在头长的低 1 字节?)
2. markProtocolType 怎么编码? (高 24 位头长 + 低 8 位类型?)
3. 双序列化选择时机? (serializeTypeCurrentRPC 来源)
4. fastEncodeHeader 与 encode 差异? (ByteBuf 零拷贝 + FastCodesHeader)
5. flag 位哪些? (oneway/response/suspended?)
6. opaque 的作用? (请求 ID 匹配)
7. processorTable 分派? (code → processor+executor; default 兜底)
8. 线程池模型? (每个 processor 独立线程池?)
9. rejectRequest 拒绝面? (限流?)
10. invokeSync 超时处理? (timeout → 关闭 channel)
11. 长连接 channel 缓存? (channelTable + 扫描)
12. responseTable 清理? (定时扫描超时)
13. RPCHook 挂点? (doBeforeRequest/doAfterResponse — 认证)
14. 长轮询 suspended 标志? (broker 拉取挂起)
15. FRAME_MAX_LENGTH? (16MB 默认)
16. TLS 面? (TlsHelper/TlsSystemConfig)
17. 5.x proxy 协议? (rpc/ 目录 + ProxyProtocolTest)
18. 心跳? (心跳请求码/定时)
19. 序列化版本兼容? (serializeVersionCurrent)
20. 与 Netty (阶段1) 的复用与定制? (LengthFieldBasedFrameDecoder 等)

## 时空溯源 (代码内痕迹)

- RemotingCommand 版权/注释: RocketMQ 初版 (3.x) 协议即定型 (4+4 帧格式)
- 5.x 演进: RocketMQSerializable 二进制序列化 (ROCKETMQ 类型, JSON 之外) + fastEncodeHeader (零拷贝) + FastCodesHeader + metrics (rpcLatency) + rpc/ (proxy gRPC 对接)
- TLS: TlsSystemConfig (4.x 后期) + TlsHelper
- RemotingCodeDistributionHandler (104 行): 5.x 处理器分发优化

## 大域拆分判断

28217 行大模块 — 核心面 protocol+netty ~5000 行主体; 🔴 A 单篇 (6 闭环)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "RemotingCommand/NettyDecoder-Encoder/帧格式/RemotingClient-Server" | 全部存在; 帧格式实证 [4B 长][4B 头长(含 serializeType)][header][body] | **接受** ✅ |
| "面试常考" | 帧格式/opaque/双序列化/线程池分派 — 常考点 | **接受** ✅ |
| 数字: FRAME_MAX_LENGTH | NettyDecoder: System.getProperty 默认 "16777216" = 16MB | **补充** ✅ |
| 数字: serializeType 位宽 | markProtocolType: 高 24 位头长 + 低 8 位类型 (需验证 L245-250) | **待验证 → q1** |
| "remoting 是 store/broker/client 全依赖" | pom 实证 (broker→remoting, store→remoting, client→remoting) | **接受** ✅ |
