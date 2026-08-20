# N-22 远程服务端面 — 知识规划 (KP)

> 🔴 A | 模块: core/remote (41 文件) | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 双服务器 | GrpcSdkServer:46 / GrpcClusterServer:46 | SDK/集群隔离 |
| 2 | 基类 | BaseGrpcServer/BaseRpcServer | 通用 |
| 3 | 注册表 | RequestHandlerRegistry:45/57 | 类型路由 |
| 4 | 接收器 | GrpcRequestAcceptor:56/114 | 分发 |
| 5 | 双向流 | GrpcBiStreamRequestAcceptor | 流接收 |
| 6 | 连接管理 | ConnectionManager:57/102 | 注册+限制 |
| 7 | 连接模型 | Connection/ConnectionMeta | 元数据 |
| 8 | 事件监听 | ClientConnectionEventListenerRegistry | 连接事件 |
| 9 | 心跳事件 | RemotingHeartBeatEvent | 健康驱动 |
| 10 | 拦截器 | NacosGrpcServerInterceptor 族 | 横切 |

## 02 高频坑
1. SDK 与集群双服务器 (端口分离)
2. 请求经注册表路由 (非直接处理)
3. 连接注册带限制规则 (ControlManagerCenter)
4. 未知类型兜底
5. ContextRefreshedEvent 收集 handler
6. 过滤器/拦截器 ServiceLoader 加载

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 服务器 | SDK/集群/双向流/基类 |
| 路由 | 注册表/接收器/未知兜底 |
| 连接 | 注册/限制/心跳/事件 |
| 横切 | 过滤器/拦截器/协商器 |

## 04 跨域桥接
- ← NC-3: 客户端内核两端闭合
- ← N-17: 配置 handler 注册
- → 面试: "Nacos gRPC 服务端" — 双服务器 + 注册表 + 连接管理
