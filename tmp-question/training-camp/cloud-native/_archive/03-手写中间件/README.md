# 03 手写中间件（Week 5-7 · 6课时）

## 学完能干什么
理解 Spring Cloud 到底帮你做了什么——因为你亲手写了一个完整的 RPC 框架。

## ⭐ 核心项目：rpc-project（训练营最强代码）

`stage-2/src/middleware-projects/rpc-project/` — 52 个 Java 源文件，Netty + SOFAJRaft + Protobuf 完整实现。

**完整调用链路**：
```
ServiceConsumer.echo("Hello")
  → ServiceInvocationHandler.invoke()   // JDK 动态代理拦截
    → createRequest()                    // 构建 InvocationRequest
    → selectServiceProviderInstance()    // 负载均衡选择实例
    → rpcClient.connect(instance)        // 建立 Netty Channel
    → channel.writeAndFlush(request)     // MessageEncoder 编码发送
    → ExchangeFuture.create()            // Promise 模式注册到等待Map
    → exchangeFuture.get()               // 阻塞等待

=== 网络传输（自定义协议：[4字节长度][序列化数据]）===

  Server: MessageDecoder → InvocationRequestHandler.channelRead0()
    → serviceContext.getService(serviceName)
    → MethodUtils.invokeMethod(反射调用)
    → ctx.writeAndFlush(response)       // 返回 InvocationResponse

  Client: MessageDecoder → InvocationResponseHandler.channelRead0()
    → ExchangeFuture.removeExchangeFuture(requestId)
    → promise.setSuccess(result)         // 唤醒阻塞线程
```

### 分层解析

| 层 | 关键类 | 核心方法 |
|----|-------|---------|
| Transport | InvocationRequestHandler, InvocationResponseHandler | channelRead0 反射调用/回调唤醒 |
| Codec | MessageEncoder, MessageDecoder | 4字节长度头 + 序列化体，半包处理 |
| Client | RpcClient, ServiceInvocationHandler, ExchangeFuture | connect/exchangeFuture.get Promise 模式 |
| Serializer | Serializer(SPI), DefaultSerializer | Java 原生序列化（可替换为 Hessian/Kryo） |
| LoadBalancer | RandomServiceInstanceSelector, RoundRobinServiceInstanceSelector | SPI 加载，ThreadLocalRandom/AtomicInteger |
| Server | RpcServer | Netty ServerBootstrap + ShutdownHook |

### JRaft 注册中心（最核心子模块）

**ServiceDiscoveryServer 启动流程**：
1. 创建 Raft 数据目录 → RaftRpcServer（Netty/gRPC）
2. 注册 3 个处理器：Registration / GetServiceInstances / HeartBeat
3. 初始化 ServiceDiscoveryStateMachine（FSM）
4. RaftGroupService.start() 启动 Raft 节点
5. 启动 ServiceInstanceBeatThread（每 5s 检查心跳）

**心跳机制**：客户端每 5s 发 HeartBeat（不写 Raft 日志），30s 无心跳自动摘除

**三种服务发现实现**：

| 实现 | 完成度 | 适用 |
|------|:----:|------|
| FileSystemServiceDiscovery | 100% | 单机测试 |
| ZookeeperServiceDiscovery | 骨架（未实现） | - |
| JRaftServiceDiscovery | 100% | 生产可用 |

### 发现的 7 个 Bug（理解即学到）

1. **RoundRobin 取模 Bug**：`(counter.getAndIncrement()-1)%size`，首次 index=-1
2. **Channel 无复用**：每次 invoke 新建连接，高并发性能差
3. **ExchangeFuture EventLoop 不明确**：DefaultEventLoop 与 Promise 回调无关
4. **ServiceInstanceBeatThread 无退出机制**：while(true) 无限循环
5. **Protobuf 重复类**：生成的 ServiceDiscoveryOuter 提交到了 src
6. **RpcServer 用 logback 的 ContextUtil 获取 hostname**
7. **RpcClient 无 connection close**

## 📖 其他中间件

### 分布式配置中心
⚠️ `distributed-config-project` 仅 POM 骨架，无 Java 代码。设计思想在 stage-2/docs/24-25 中。

### 分布式缓存
`distributed-cache-project/` — MyQueuedSynchronizer（自定义 AQS）、DistributedSessionFilter（Filter 包装 HttpServletRequest）、RedisDistributedHttpSession

**动手**：
```bash
cd stage-2/src/middleware-projects && mvn compile -pl rpc-project
# 跑 demo: ServiceProvider → ServiceConsumer
```
