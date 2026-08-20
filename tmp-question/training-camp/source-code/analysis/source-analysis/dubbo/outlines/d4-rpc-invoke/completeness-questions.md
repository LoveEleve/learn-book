# D-4 RPC 调用 — completeness-questions (全视角提问验证)

## 开发者视角

1. 调用入口? (InvocationUtil.invoke 两拍)
2. Filter 链怎么构建? (buildInvokerChain + @Activate)
3. 消费端有哪些过滤器? (18 实例族)
4. 连接怎么选? (多连接轮询 index % size)
5. 超时怎么处理? (前置计算 + TIMEOUT_TERMINATE)
6. 单向调用? (isOneway send 不等结果)
7. 异步结果怎么拿? (recreate 三模式)
8. 异常怎么分类? (TIMEOUT/SERIALIZATION/NETWORK)

## 架构师视角

9. 为什么全异步底座? (CompletableFuture 链路, 线程不阻塞)
10. 为什么 recreate 两拍? (invoke 异步 + 同步化最后一拍)
11. 为什么 ProtocolFilterWrapper 在 cluster 模块? (3.x 归属变化)
12. 为什么 registry URL 透传? (注册中心引用不过滤器)
13. 为什么 CONSUMER group 分流? (消费/服务端链不同)
14. 为什么连接级轮询? (区别于 D-6 节点级负载均衡)
15. 为什么三模式一拍切换? (SYNC/ASYNC/FUTURE 共用一条异步链)
16. 为什么 FutureContext 兼容 2.6? (迁移面: Zipkin 老过滤器)

## 学生视角

17. 什么是 Filter? (调用链上的拦截器)
18. 什么是 AsyncRpcResult? (异步结果包装)
19. 什么是 InvokeMode? (同步/异步/未来三模式)
20. 什么是 RpcContext? (调用上下文)
