# RM-13 Proxy+安全 — Pass 1 探索笔记

> 域: RM-13 Proxy + 安全 | 🟡 B 方案 | 2026-08-14 (收官域)
> 源码: proxy/ 162 文件/20375 行 (grpc 36 + service 51 + remoting 29 + processor 20 + common 14 + config 5) + auth/ 5025 行 (authentication 23 + authorization 27 + migration) + acl/ AclSigner | RocketMQ 5.3.1

## 调用图

```
5.x 客户端 (gRPC) → :8081 GrpcMessagingApplication (v2 协议)
  → RequestPipeline: ContextInit → Authentication (签名) → Authorization (资源)
  → addExecutor 分流 (producer/consumer/clientManager/transaction 线程池)
  → MessagingProcessor 统一消息面
      → LOCAL: LocalMessageService 直接调 broker processor (同进程)
      → CLUSTER: ClusterMessageService 内嵌客户端远程转发

老 remoting 客户端 → :8080 RemotingProtocolServer (多协议协商, match 恒 true 兜底)
  → 请求码分发 (SEND/PULL/POP/HEART_BEAT/END_TRANSACTION → activity)
  → RemotingConverter 翻译 → MessagingProcessor 统一消息面

认证: authConfig 非空 → AuthenticationPipeline (AclSigner HmacSHA1 签名验证)
  → AuthorizationPipeline (资源权限) → username → AUTHORIZATION_AK header
```

## 基本元素分解

1. **拓扑双模式**: LOCAL (内嵌 broker) / CLUSTER (独立网关) + 三端口
2. **gRPC 层**: GrpcMessagingApplication + 管线 + 线程池分流
3. **双协议翻译**: ProtocolNegotiationHandler 协商 + RemotingProtocolHandler 装配 + 请求码分发
4. **统一消息面**: MessagingProcessor + Local/Cluster 双 MessageService
5. **认证一句话**: HmacSHA1 签名 + 授权资源表 + acl 迁移
6. **客户端面**: 重试 3/退避 2/长轮询 32

## 标记问题 (20 问)

1. LOCAL/CLUSTER 怎么选? (proxyMode 参数)
2. 三端口默认值? (8081/8080/5557)
3. gRPC 服务实现类? (GrpcMessagingApplication)
4. 管线执行序? (pipe 反转: ContextInit→Authn→Authz)
5. 多协议怎么协商? (首包 match)
6. remoting 兜底? (match 恒 true)
7. 请求码怎么分发? (activity 注册)
8. 翻译层在哪? (RemotingConverter + http2proxy)
9. 统一消息面方法集? (send/pop/ack/pull/offset/endTx)
10. LOCAL 直写实现? (调 broker processor)
11. CLUSTER 转发? (内嵌客户端)
12. 签名算法? (HmacSHA1)
13. 认证链 handler? (DefaultAuthenticationHandler)
14. 授权怎么判? (资源表 builder)
15. acl 迁移? (AuthMigrator)
16. gRPC 线程池? (16+2N/100000/130MB)
17. 客户端重试? (3/退避 2/长轮询 32)
18. proxy 有状态吗? (无 — 状态在 broker)
19. 与 RM-1 rpc/ 层关系? (proxy 内嵌翻译 + client rpc/ 桥)
20. 认证默认开吗? (authConfig 驱动, 默认关)

## 时空溯源 (代码内痕迹)

- 5.0: proxy 初版 (gRPC v2 协议 + remoting 兼容接入 + LOCAL/CLUSTER 双模式) — GrpcMessagingApplication/MessagingProcessor 架构
- 5.1+: auth 模块独立 (authentication/authorization 分治 + chain + Stateful/Stateless 策略)
- 5.x: AuthMigrator (acl→auth 迁移) + http2proxy 面 + POP 语义统一

## 大域拆分判断

RM-13 = proxy 全模块 (20375 行, 最大面) + auth 一句话 + acl 一句话; 单篇 🟡 B, 6 闭环; 安全面按 v3 定级"一句话" (面试低频+业务不接触), 不展开 auth 内部细节

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "gRPC 协议层 (GrpcServer/RpcServer — 5.x 客户端默认接入)" | GrpcMessagingApplication (v2 服务) + GrpcServerBuilder; 8081 | **接受** ✅ |
| "双协议翻译 (remoting↔grpc)" | ProtocolNegotiationHandler + RemotingProtocolHandler + RemotingConverter | **接受** ✅ |
| "proxy 请求转发链 (processor 管线)" | MessagingProcessor + RequestPipeline (ContextInit/Authn/Authz) | **接受+精确化**: 管线在 gRPC 入口, 非转发链 | ✅ |
| "双认证: SignAuthentication + ACL" | AclSigner HmacSHA1 + AuthorizationPipeline; **SignAuthentication 类不存在 — 实为 HmacSHA1 签名认证 + 资源授权两段** | **修正** ⚠ |
| 数字: 端口/线程/消息 | 8081/8080/5557 / 16+2N/100000 / 130MB/4MB | **补充** ✅ |
| 数字: 客户端重试 | attempts=3 / backoff=2 / longPollingBatch=32 / mqClientNum=6 | **补充** ✅ |
| "auth 7232 行" | main 5025 行 (含 test 7232) | **精确化** ✅ |
