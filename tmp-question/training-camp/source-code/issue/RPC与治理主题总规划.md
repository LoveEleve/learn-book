# RPC 与治理主题总规划

> 主题范围：`Feign → Dubbo → gRPC → Spring Cloud Commons / OpenFeign / Gateway / Alibaba → Nacos → Sentinel`  
> 目标：不按仓库平铺，而是按机制轴心重组，形成一条可写成“完整卷”的源码分析路线。  
> 参考依据：`源码分析执行计划.md` 第 5 阶段 + 各仓库已完成的单仓范围规划 + `源码范围规划复盘方法论.md`。

---

## 一、为什么这一组不能按仓库顺序直接写

如果按仓库顺序平铺：

- Feign
- Dubbo
- gRPC
- Spring Cloud Commons
- Spring Cloud OpenFeign
- Spring Cloud Gateway
- Spring Cloud Alibaba
- Nacos
- Sentinel

会很快出现三个问题：

### 1. 同一机制被拆散在多个仓库里

例如：
- **声明式代理** 同时出现在 Feign、OpenFeign、Dubbo proxy、gRPC Stub
- **服务发现 / 注册** 同时出现在 Commons、Nacos、Dubbo Registry、Cloud Alibaba
- **限流 / 熔断 / 降级** 同时出现在 Sentinel、Spring Cloud CircuitBreaker、Gateway Filter、Dubbo Cluster
- **HTTP/2 / RPC 传输** 同时出现在 gRPC、Dubbo Triple、Netty HTTP/2

如果按仓库写，读者会在多个卷里反复看见同类问题，却一直缺一个横向总图。

### 2. 集成层和内核层会顺序错乱

Spring Cloud Commons / OpenFeign / Alibaba 这些仓库很多时候不是“重新发明机制”，而是在：
- 复用底层中间件
- 做 Spring 装配
- 做注解桥接
- 做自动配置

如果先写这些集成层，再写底层仓库，正文会被大量“这个接口来自别处”“这个主线在另一个仓库”打断。

### 3. 最后形成的不是“完整卷”，而是“很多仓库的串讲”

这正是 `源码范围规划复盘方法论.md` 里强调的第 10 个风险：
- 只做主干闭环，不做完整卷补层

RPC 与治理这组如果只按仓库顺序写，最后很容易得到：
- 每个仓库单看没问题
- 但读者仍然回答不了“整个 Spring 生态里 RPC 和治理的通用模型是什么”

因此，这一组必须先按**机制轴心**重组，再决定仓库内的落笔顺序。

---

## 二、这组主题真正的机制轴心

按读者真实困惑与上层复用度，RPC 与治理应重组为 8 条主轴。

### 轴心 1：调用入口与代理抽象

回答的问题：
- 一个本地 Java 调用是怎么变成远程调用的？
- 为什么 Feign 用接口代理，Dubbo 用 Invoker，gRPC 用 Stub？
- 这些代理在调用前到底需要准备哪些元信息？

覆盖仓库：
- Feign
- Spring Cloud OpenFeign
- Dubbo
- gRPC-Java

关键词：
- `Feign.Builder / ReflectiveFeign / MethodHandler`
- `FeignClientFactoryBean / SpringMvcContract`
- `Invoker / ProxyFactory / Invocation`
- `ManagedChannel / ClientCall / Stub / ClientCalls`

### 轴心 2：传输与协议主线

回答的问题：
- 远程调用最终落在 HTTP/1.1、Dubbo2、Triple、gRPC、HTTP/2 的哪条链上？
- 为什么有的框架自己维护 transport handler，有的框架复用 Netty HTTP/2 API 层？
- 流式调用和单次调用在 transport 上的差异在哪里？

覆盖仓库：
- Dubbo
- gRPC-Java
- Netty（已完成，可直接引用）

关键词：
- `TripleProtocol / TripleInvoker / TripleClientCall / TripleWriteQueue`
- `GrpcHttp2ConnectionHandler / NettyServerHandler / NettyClientHandler`
- `Http2FrameCodec / Http2MultiplexHandler / ConnectionHandler`

### 轴心 3：服务发现、注册与命名

回答的问题：
- 服务怎么注册？消费者怎么发现实例？
- 为什么 Spring Cloud 有 `DiscoveryClient / ServiceRegistry` 抽象？
- Nacos、Dubbo Registry、LoadBalancer 之间怎样衔接？

覆盖仓库：
- Spring Cloud Commons
- Nacos
- Dubbo
- Spring Cloud Alibaba

关键词：
- `DiscoveryClient / ServiceRegistry / AbstractAutoServiceRegistration`
- `NacosNamingService / NacosServiceRegistry / NacosDiscoveryClient`
- `RegistryProtocol / RegistryDirectory`
- `NacosLoadBalancer / ServiceInstanceListSupplier`

### 轴心 4：实例选择、负载均衡与集群容错

回答的问题：
- 同一个服务有多个实例时，谁来选？
- 为什么 Dubbo 的 Failover/LoadBalance 和 Spring Cloud LoadBalancer 的职责看起来相近但层次不同？
- 什么时候是“挑一个实例”，什么时候是“选择后再重试换实例”？

覆盖仓库：
- Dubbo
- Spring Cloud Commons
- Spring Cloud Alibaba
- gRPC-Java（NameResolver + LoadBalancer）

关键词：
- `LoadBalance / FailoverClusterInvoker / StickyInvoker`
- `ReactorLoadBalancer / BlockingLoadBalancerClient`
- `NacosLoadBalancer`
- `NameResolver / LoadBalancer / SubchannelPicker`

### 轴心 5：元数据、请求上下文与跨层透传

回答的问题：
- 请求头、Context、XID、Tracing 信息是怎么跨进程透传的？
- gRPC Context、Feign interceptor、Seata XID、Dubbo RpcContext 在机制上有什么共同点？

覆盖仓库：
- Feign
- Dubbo
- gRPC-Java
- Spring Cloud Alibaba

关键词：
- `RequestInterceptor`
- `RpcContext`
- `Context / Metadata`
- `SeataFeignRequestInterceptor / SeataRestTemplateInterceptor / SeataHandlerInterceptor`

### 轴心 6：配置中心、动态刷新与命名上下文

回答的问题：
- 配置是怎么加载进应用上下文的？
- 为什么 `@RefreshScope`、Nacos Config、Feign 子上下文、Gateway 动态路由看起来都在做“局部重建”？
- 哪些是配置中心语义，哪些是 Spring 引导/上下文语义？

覆盖仓库：
- Spring Cloud Commons
- Spring Cloud OpenFeign
- Spring Cloud Gateway
- Spring Cloud Alibaba
- Nacos

关键词：
- `PropertySourceLocator / Bootstrap / ContextRefresher / RefreshScope`
- `FeignClientFactory / NamedContextFactory`
- `GatewayControllerEndpoint / RefreshRoutesEvent`
- `NacosPropertySourceLocator / NacosConfigRefreshEventListener`

### 轴心 7：限流、熔断、降级与治理规则

回答的问题：
- Sentinel、Gateway、Feign、Dubbo、Spring Cloud CircuitBreaker 这些“治理动作”分别挂在哪一层？
- 限流规则、断路器规则、网关规则和数据源推送是怎样落到运行时链上的？

覆盖仓库：
- Sentinel
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Spring Cloud Alibaba
- Dubbo（过滤器、集群容错）

关键词：
- `ProcessorSlot chain / FlowSlot / DegradeSlot / ParamFlowSlot`
- `SentinelGatewayFilter / SentinelFeign / SentinelProtectInterceptor`
- `FeignCircuitBreakerTargeter / InvocationHandler`
- `Gateway Filter / RequestRateLimiter`
- `Dubbo Filter / Cluster fault tolerance`

### 轴心 8：完整集成层与生态桥接

回答的问题：
- Spring Cloud Commons / OpenFeign / Alibaba / Gateway 到底是在“创造新机制”，还是在“装配已存在机制”？
- 哪些类是桥接器，哪些类才是机制内核？

覆盖仓库：
- Spring Cloud Commons
- Spring Cloud OpenFeign
- Spring Cloud Gateway
- Spring Cloud Alibaba

关键词：
- `NamedContextFactory`
- `FeignClientFactoryBean`
- `ReactiveLoadBalancerClientFilter`
- `NacosPropertySourceLocator / Sentinel* / Seata* / RocketMQ binder`

---

## 三、按“完整卷”思路重排后的写作阶段

### 阶段 A：RPC 运行时内核

先把“调用是怎么发生的”讲清楚。

建议顺序：
1. gRPC-Java：`Channel + Stub + Stream`
2. Dubbo：`ExtensionLoader + Export/Refer + Invoker + Cluster`
3. Feign：`Builder + Contract + Client + Decoder`
4. 对照篇：三种调用抽象（Stub / Invoker / MethodHandler）

目标：先回答“远程调用到底怎么出发”。

### 阶段 B：服务发现与实例选择

建议顺序：
1. Spring Cloud Commons：`DiscoveryClient / ServiceRegistry / LoadBalancer 抽象`
2. Nacos：`NamingService + ConfigService + gRPC/Redo`
3. Dubbo Registry / Directory / Cluster
4. gRPC-Java：`NameResolver + LoadBalancer`
5. 对照篇：实例列表、选择器、重试与容错

目标：回答“调用发出去之前怎么找到人、怎么选人”。

### 阶段 C：Spring 集成桥接层

建议顺序：
1. Spring Cloud OpenFeign
2. Spring Cloud Gateway
3. Spring Cloud Alibaba（Nacos/Sentinel/Seata/RocketMQ binder 集成）
4. 对照篇：`NamedContextFactory + @RefreshScope + 动态路由/客户端子上下文`

目标：回答“Spring 生态是如何把底层机制装进注解和自动配置里的”。

### 阶段 D：治理与规则引擎

建议顺序：
1. Sentinel 核心：`Slot chain + Context/Entry + Flow/Degrade`
2. Sentinel 适配器与数据源
3. Gateway 限流 / 熔断 / 过滤器链
4. Feign CircuitBreaker
5. Dubbo 过滤器 / 集群容错桥接
6. 对照篇：限流/熔断/降级在不同层级的落点

目标：回答“治理动作挂在哪，规则怎么进来，运行时怎么执行”。

---

## 四、这一组不应直接开写的内容

根据《源码范围规划复盘方法论》，以下内容不能在现在直接写成正文，必须等主题总规划稳定后再开：

1. **Cloud*4 的 41 域全部按仓库顺序平推**
   - 会把大量“集成层”写成“内核层”，顺序错乱。

2. **Nacos / Sentinel 直接从注解或 starter 写起**
   - 会跳过客户端、协议、规则执行主线。

3. **先写 Gateway / OpenFeign，再写 Feign / Commons / Nacos**
   - 会不断前向引用未落地机制。

4. **先写业务用法型正文**
   - 比如“如何用 Feign”“如何用 Sentinel”这类内容，不应先于机制闭环正文。

---

## 五、第一批正文建议顺序

为了最大化复用已经写好的 Netty 卷成果，第一批建议这样开：

1. **gRPC-Java：Channel + Stub + Stream + Interceptor**
   - 理由：它和 Netty HTTP/2 主线连接最直接，且能最快建立“RPC 调用运行时”的最低心智图。

2. **Dubbo：ExtensionLoader + Export/Refer + Invoker + Cluster**
   - 理由：Dubbo 把“服务导出 / 引用 / 注册 / 集群容错”拉得最完整，能建立和 gRPC 完全不同的 RPC 心智图。

3. **对照篇：gRPC vs Dubbo 的 RPC 运行时主线**
   - 理由：防止两条主线各写各的，读者脑中仍无总图。

4. **Spring Cloud Commons：DiscoveryClient / ServiceRegistry / LoadBalancer / NamedContextFactory**
   - 理由：它是 OpenFeign、Gateway、Alibaba 集成层的共同地基。

5. **Nacos：NamingService + ConfigService + gRPC/Redo + AP/CP**
   - 理由：服务发现与配置中心这条线要尽早建立，不然后面 Alibaba 集成层会悬空。

6. **Sentinel：SlotChain + Context/Entry + Flow/Degrade + 适配器**
   - 理由：治理动作的独立内核要先落地，后面 Gateway/OpenFeign/Alibaba 才有落点。

7. **Spring Cloud OpenFeign**
8. **Spring Cloud Gateway**
9. **Spring Cloud Alibaba**

---

## 六、当前建议

当前最合理的下一步，不是立刻写这 76 域中的任意一篇正文，而是：

- 先承认这是一组**主题卷**，不是仓库串讲
- 再以 **gRPC-Java** 作为第一篇正文入口
- 因为它：
  1. 直接复用已经完成的 Netty HTTP/2 卷成果
  2. 机制主线短、清楚、闭环快
  3. 非常适合作为 Dubbo / Feign / Gateway 的对照基准

**因此，RPC 与治理主题的第一篇建议从 gRPC-Java 开始。**