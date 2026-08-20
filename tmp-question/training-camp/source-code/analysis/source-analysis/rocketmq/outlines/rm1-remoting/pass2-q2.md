# 闭环笔记 q2: 编解码链 — NettyDecoder/Encoder + 分帧

## 假设
长度字段分帧 (LengthFieldBasedFrameDecoder) + 帧内 decode/encode; 异常断连。

## 验证过程
- **Decoder** (NettyDecoder.java:34-48): `super(FRAME_MAX_LENGTH, 0, 4, 0, 4)` — **LengthFieldBasedFrameDecoder**: 最大 16MB (System.getProperty 默认 "16777216") / 长度字段偏移 0 / 长度 4 字节 / 无头剥离 / 无长度调整; 解码出整帧 ByteBuf → `RemotingCommand.decode(frame)` → 返回命令 + processTimer (耗时监控)
- **decode 路径** (RemotingCommand.decode): 读总长/头长 → markProtocolType 解析 serializeType → 按类型反序列化 header (RocketMQSerializable.rocketMQProtocolDecode / JSON) → body
- **Encoder** (NettyEncoder.java): 反向 → fastEncodeHeader (ByteBuf 直写)
- **异常处理** (NettyDecoder L54-60): 解码异常 → 日志 + **RemotingHelper.closeChannel** (断连 — 协议错误即断开, 对照 Redis R-28 "协议错误即断连")
- **分帧依赖**: 4 字节长度字段在帧头 — 无粘包/拆包问题 (Netty 框架处理)

## 代码类型
Implementation (传输编解码)

## 跨域关联
- Netty (阶段1): LengthFieldBasedFrameDecoder 参数语义
- RM-8/9 (消费): 拉取长轮询走同一编解码

## 结论
编解码 = LengthFieldBasedFrameDecoder (16MB 上限, 偏移 0 长度 4) + RemotingCommand.decode/encode 双序列化分派; 协议错误即断连。
源码位置: NettyDecoder.java:34-62; NettyEncoder.java; RemotingCommand.java decode/encode
