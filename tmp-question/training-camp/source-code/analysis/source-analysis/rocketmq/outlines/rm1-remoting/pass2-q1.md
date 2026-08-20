# 闭环笔记 q1: 协议帧格式 — 4+4 头 + 双序列化

## 假设
帧 = [4B 总长][4B 头长+序列化类型][header][body]; markProtocolType 高 24 位头长。

## 验证过程
- **帧布局** (RemotingCommand.encode L395-409): `ByteBuffer.allocate(4 + length)` → putInt(length) → putInt(markProtocolType(headerData.length, serializeType)) → header → body
- **markProtocolType** (L245-247): `(type.getCode() << 24) | (source & 0x00FFFFFF)` — **高 8 位 serializeType + 低 24 位头长** (头长上限 16MB; 修正 pass1 猜测"高 24 位")
- **SerializeType** (SerializeType.java:20-31): JSON=0 (默认, serializeTypeConfigInThisServer) / ROCKETMQ=1 (二进制); 可由系统属性 "rocketmq.remoting.serializeType" 配置 (L77)
- **双序列化** (headerEncode L415-422): ROCKETMQ → RocketMQSerializable.rocketMQProtocolEncode (二进制, 5.x) / 默认 → RemotingSerializable.encode (JSON + extFields)
- **字段集**: code (请求码) / version (语言版本) / opaque (请求 ID) / flag (位标志) / remark / extFields (customHeader 反射展开, makeCustomHeaderToNet L424-444) / body
- **flag 位** (L50-51): RPC_TYPE=bit0 (请求/响应) / RPC_ONEWAY=bit1 (单向); **suspended 是独立 boolean 字段** (isSuspended L592, @JSONField) 非 flag — 长轮询挂起标记
- **零拷贝路径** (fastEncodeHeader L450-473): ByteBuf 直写 (writeLong 占位 → setInt 回填), 5.x + FastCodesHeader 接口 (免反射头)

## 代码类型
Interface (协议编解码)

## 跨域关联
- Netty (阶段1): LengthFieldBasedFrameDecoder 复用 (偏移 0, 长度字段 4)
- RM-12 (HA): 复制同步走同一协议

## 结论
帧 = [4B 总长][1B 序列化类型+3B 头长][header][body]; 双序列化 (JSON 默认/ROCKETMQ 二进制 5.x); flag 仅 2 位 (type/oneway), suspended 独立字段; 5.x 零拷贝 fastEncodeHeader。
源码位置: RemotingCommand.java:245-247,395-473,505-540; SerializeType.java:20-31
