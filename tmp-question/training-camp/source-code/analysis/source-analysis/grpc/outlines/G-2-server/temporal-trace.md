# G-2 temporal-trace — 时空溯源 (🔴 域)

> 浅克隆无 git 历史, 无 CHANGELOG — 溯源用代码内 @since/@ExperimentalApi + 接口演进痕迹。

## 演进时间线

| 版本线索 | 证据 | 意义 |
|---|---|---|
| 1.0.0 前 | ServerImpl/ServerCallImpl/InternalHandlerRegistry 核心字段无 @since 注释 (内部实现类) | 内部核心 10 年稳定, 无版本标注本身说明历史久远 |
| 1.16.0+ | ServerCallExecutorSupplier (api, @since 待验证) | 动态执行器是后期扩展 (早期单 executor) |
| 1.46.0+ | ServerCall.setOnReadyThreshold (ServerCallImpl.java:183) | 背压阈值细化 (流控演进) |
| 1.60.0+ | JumpToApplicationThreadServerStreamListener/MethodLookup 重构 | 执行器切换 + setup 顺序保证 (serializing executor 架构) |
| 1.83.1 | NettyServerHandler GracefulShutdown 双 GOAWAY + keepAliveEnforcer | 连接生命周期演进成熟期 |

## 架构稳定性判断

**核心十年未变**: ServerImpl (注册表+生命周期) + ServerCallImpl (状态机) — gRPC 服务端骨架自 1.0 起稳定; **演进在边缘**: 执行器模型 (Supplier 动态切换)、连接管理 (双 GOAWAY/ping 限频)、背压 (onReadyThreshold)。

**关键演进痕迹 (代码内)**: 
- TODO "honor authority header" (InternalHandlerRegistry.java:52) — 注册表 authority 维度未实现, 真实语义缺口
- TODO "update fullMethodName to have the canonical path" (ServerImpl.java:662) — 方法名规范化未做
- KeepAliveEnforcer 的 ENHANCE_YOUR_CALM GOAWAY (NettyServerHandler.java:997) — HTTP/2 RFC 7540 规范行为 (5.5 节 ping 滥用)

## 写书建议

服务端域按"稳定骨架 + 演进边缘"呈现: 骨架 (注册表/状态机/链路) 讲 1.0 设计, 边缘 (双 GOAWAY/动态执行器) 标 @since 演进, TODO 注释可作"未完成的设计"讨论点。
