# D-9 Triple 协议 — Pass 2 闭环 Q4: gRPC 兼容 + protobuf + REST 面

> 核心: GrpcHttp2Protocol + PbUnpack + RestProtocol | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: triple 怎么兼容 gRPC? protobuf 怎么处理? REST 怎么共存?**

## 机制链 (已实证)

```
gRPC 兼容面:
├── **GrpcHttp2Protocol extends TripleHttp2Protocol** (GrpcHttp2Protocol.java:22) — gRPC 协议变体
├── pathResolver: gRPC 风格路径 (/package.Service/Method) — grpc 客户端可直接调用!
└── **TriplePingPongHandler** (q3) + 帧兼容 (grpc trailer/status)

protobuf 面 (序列化, D-10 关联):
├── **PbUnpack** (PbUnpack.java:25-41): SingleProtobufUtils.deserialize — protobuf 反序列化
├── PbArrayPacker / PackableMethodFactory / DefaultPackableMethodFactory — 方法打包抽象
├── ReflectionPackableMethod — 反射式方法打包
└── DescriptorUtils — protobuf Descriptor (gRPC 反射/服务发现)

REST 面 (REST_ENABLED, D-8b 基座):
├── **RestProtocol extends TripleProtocol** (RestProtocol.java:21) — REST 协议变体
├── rest/ 子包: mappingRegistry (D-8b Mapping 注解消费)
└── 同一端口: RPC (triple) + REST (json) 双协议
```

## 关键设计 (why)

1. **gRPC 兼容 = 生态互通**: 路径/帧/状态兼容 — gRPC 客户端调 dubbo 服务 (与阶段 5.3 gRPC 对照)
2. **protobuf 打包抽象**: PackableMethod 族 — 序列化可换 (protobuf 默认, D-10 深入)
3. **REST 变体**: RestProtocol extends TripleProtocol — 复用 RPC 基建只换消息语义
4. **多协议一端口**: RPC + gRPC + REST 共端口 — 部署简化 (D-8b 双栈 + 多协议面)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| GrpcHttp2Protocol | GrpcHttp2Protocol.java:22 |
| PbUnpack (protobuf 反序列化) | PbUnpack.java:25-41 |
| PackableMethodFactory 族 | DefaultPackableMethodFactory.java |
| RestProtocol | RestProtocol.java:21 |
| 路径注册 (pathResolver) | TripleProtocol.java:135 |

## 负面空间 (Q4 面)

- 不做 gRPC 全特性 (gRPC 反射/健康检查部分支持)
- 不做 protobuf 动态 schema (编译期生成)
- 不做 REST 版本化 (路径级版本)
