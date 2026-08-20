# D-9 Triple 协议 — completeness-questions (全视角提问验证)

## 开发者视角

1. export/refer 怎么装配? (pathResolver + PortUnification)
2. 三种调用模式? (unary/服务端流/双向流)
3. 同步怎么不占线程? (ThreadlessExecutor)
4. 流控? (Local/Remote FlowController)
5. GOAWAY? (优雅停机)
6. PING? (HTTP/2 保活)
7. protobuf 怎么处理? (PbUnpack)
8. REST 怎么共存? (RestProtocol)

## 架构师视角

9. 为什么 HTTP/2 单连接多路复用?
10. 为什么 gRPC 路径寻址? (生态互通)
11. 为什么 ThreadlessExecutor? (吞吐)
12. 为什么 WINDOW_UPDATE 接管? (精确流控)
13. 为什么三模式一协议? (调用模型统一)
14. 为什么 REST 变体? (复用 RPC 基建)
15. 为什么多协议一端口? (部署简化)
16. 为什么 PackableMethod 抽象? (序列化可换)

## 学生视角

17. 什么是 HTTP/2 流? (多路复用)
18. 什么是 unary? (一元调用)
19. 什么是 gRPC? (HTTP/2 RPC 框架)
20. 什么是 protobuf? (二进制序列化)
