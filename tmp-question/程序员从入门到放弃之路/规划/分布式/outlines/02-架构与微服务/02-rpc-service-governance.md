# RPC 与服务治理 — 远程调用为什么不像本地方法，超时重试又如何避免重复扣款

> Cluster A: 12 KPs | 依赖: 01-communication-foundation | 读者基线: HTTP、序列化、TCP/TLS、微服务基础
> 读者处境: 01 篇已经讲了通信底座；本篇把字节传输推进到服务间调用：注册发现、负载均衡、路由、超时、重试和幂等如何共同决定一次 RPC 的结果
> 打开新视角: RPC 的核心不是隐藏网络，而是**把网络的部分失败、延迟、重复和服务发现显式放进调用语义**

---

### 概念依赖链

```
01 communication-foundation → 本篇: RPC 与服务治理
  ├─ §1 IPC/RPC/Stub/Skeleton(远程调用边界)
  ├─ §2 注册发现(ZK/etcd/Nacos)
  ├─ §3 负载均衡(客户端/服务端/算法)
  ├─ §4 治理组件(路由/限流/熔断/配置/追踪)
  └─ §5 超时/重试/幂等(部分失败处理)
先讲: 远程边界 → 找服务 → 选节点 → 治理 → 可靠调用
后续依赖: 03-distributed-theory-architecture(CAP/微服务架构边界)
```

### 叙事顺序

1. 问题引入——订单服务调用库存服务，为什么“本地方法返回值”模型会在网络边界失效？
2. IPC/RPC——Stub、Skeleton、协议与部分失败
3. 注册发现——实例变化如何传播
4. 负载均衡——服务列表如何选择节点
5. 治理八件套——路由/限流/熔断/配置/追踪
6. 超时/重试/幂等——为什么库存会扣两次
7. 收束

### 1. IPC 到 RPC — “像本地调用”是一种危险幻觉

场景提示: `orderService.create()` 在本地失败就抛异常，远程 `OrderService` 超时却不知道服务端是否已经执行；差异在哪里？ [写作时展开]

关键设计: RPC 只是把序列化和网络封装成代理/骨架，并没有消除网络故障：

```[pseudocode]
client stub
  → serialize request
  → transport/network
  → server skeleton
  → deserialize
  → invoke service
  → serialize response
  → client deserialize

失败状态:
  请求没发出
  请求已发出但服务未执行
  服务已执行但响应丢失
  服务执行超时/部分失败
```

Why: 为什么 RPC 不能真正像本地调用？——**远程调用有不可预测延迟、连接失败、重复、服务端部分执行和协议演进问题**；接口代理只能隐藏语法，不能隐藏故障模型。gRPC/Protobuf、Dubbo/自定义协议的选择还要考虑跨语言、生态、HTTP/2、连接复用和运维工具。 [分布式理论: RPC 是网络边界，必须显式处理超时、幂等和重试]

比喻锚点: RPC 像给远方工厂打电话下单：电话接通不代表工厂完成生产，听不到回执也不代表订单没进系统。 [写作时展开]

### 2. 注册与发现 — 实例地址不是配置文件里的常量

场景提示: 服务从 10 个实例扩到 100 个，实例还会重启和迁移，调用方如何知道当前可用地址？ [写作时展开]

关键设计: 注册中心维护实例、元数据、健康和变更传播：

```[pseudocode]
provider startup
  → register address/port/metadata
  → heartbeat/lease

consumer
  → discover instance list
  → cache locally
  → watch/push/poll updates

health:
  active probe/heartbeat/lease expiry
  → remove or mark unhealthy
```

Why: 为什么不能只用 DNS 完成服务发现？——**DNS 缓存/TTL、健康语义、版本/权重/分组元数据和实例级变更传播不一定满足 RPC 治理需求**；但注册中心也会成为新的依赖和故障边界，客户端必须有缓存、退避和降级。ZK、etcd、Nacos 的一致性/可用性和服务模型不同，不能用简单 AP/CP 标签替代具体操作语义。 [分布式理论: 注册中心自身也需要故障模型、会话、租约和一致性设计]

比喻锚点: 注册中心像动态通讯录：不只记录号码，还记录谁在线、版本、权重和区域；通讯录故障时客户端要有缓存和应急策略。 [写作时展开]

### 3. 负载均衡 — 服务发现之后如何选节点

场景提示: 5 个实例响应速度不同，轮询为什么可能把慢节点压得更慢？ [写作时展开]

关键设计: 负载均衡可以放在服务端代理、客户端 SDK 或 RPC 框架内：

```[pseudocode]
服务端 LB:
  client → Nginx/HAProxy/LB → provider
  → 集中治理/对客户端透明

客户端 LB:
  consumer cache instance list
  → 本地选择节点
  → 少一跳, 但客户端承担发现/健康/算法复杂度

算法:
  round robin/random
  weighted
  least active/latency-aware
  consistent hash
```

Why: 为什么最小活跃数不是总能选出最快节点？——**活跃数是当前并发近似，不等于服务时间、请求大小、缓存命中和下游依赖**；一致性 Hash 减少路由变化造成的缓存抖动，但节点增删仍会迁移一部分 key。算法必须用真实流量、热点和实例健康数据验证。 [系统性能: 负载均衡算法会改变队列、尾延迟和缓存局部性]

比喻锚点: 负载均衡像分诊台：轮询按号码发单，最小活跃按当前候诊人数发单，一致性 Hash 则让同一客户尽量回同一个窗口。 [写作时展开]

### 4. 服务治理 — RPC 框架为什么不只负责“调用”

场景提示: 服务能发现、也能调用，但一个下游变慢就拖垮上游；还需要哪些治理能力？ [写作时展开]

关键设计: 治理组件围绕流量、故障和可见性组成：

```[pseudocode]
路由:
  版本/灰度/地域/租户/标签

限流:
  控制进入速率/并发/队列

熔断降级:
  失败率/超时达到阈值
  → 暂停调用/返回降级结果

超时/重试:
  约束等待预算
  → 只对安全操作重试

追踪/指标:
  request_id/trace_id
  → 连接调用链与延迟/错误

配置/健康:
  动态变更与实例摘除
```

Why: 为什么熔断、重试和限流要一起设计？——**只重试会放大故障，只熔断可能丢失可恢复请求，只限流不看优先级又可能保护错对象**；治理策略需要共享 deadline、错误分类、幂等和业务降级语义。框架组件名称会随 Spring Cloud/Dubbo 版本演进，不能把历史组件当作永久默认。 [分布式架构: 治理的核心是故障传播控制，而不是组件数量]

### 5. 超时、重试与幂等 — 为什么库存会扣两次

场景提示: RPC 超时 3 秒后自动重试，第一次其实已经扣库存成功；第二次请求为什么会再次扣减？ [写作时展开]

关键设计: 超时只是客户端没收到结果，重试必须建立在操作幂等和预算约束上：

```[pseudocode]
client request_id = unique
  → send(request_id, deadline)

timeout:
  先查询 request_id 状态?
  → retry only if operation safe
  → exponential backoff + jitter
  → max attempts / global deadline

server:
  UNIQUE(request_id) / idempotency record
  → first execution stores result
  → duplicate request returns same result/no-op

注意:
  timeout != server did not execute
```

Why: 为什么不能把所有 RPC 都“超时重试 3 次”？——**重试会制造惊群、放大下游负载、延长请求链，并对非幂等操作造成重复副作用**；GET/查询也可能触发计费或日志副作用，不能只按 HTTP method 判断。必须分类错误、共享 deadline、限制总预算，并让服务端实现幂等键/状态机。 [分布式理论: 02 网络模型和 09 可靠消息的幂等原则在 RPC 这里重合]

比喻锚点: 扣款超时像银行转账后没收到回执；正确做法是用订单号查询状态，而不是再无条件发起一笔转账。 [写作时展开]

### 6. 收束

RPC 调用闭环：

```[pseudocode]
调用
  → 注册发现实例
  → 负载均衡选节点
  → 路由/限流/熔断
  → deadline/timeout
  → 服务执行或部分执行
  → 幂等记录/重试/查询
  → trace/metrics 观测
```

**Aha Moment**: "RPC 框架真正解决的不是把远程调用写得像本地，而是**把服务发现、节点选择、故障传播、超时预算、幂等和观测组织成一套可控的远程调用协议**。"
**回答读者三问**: ①注册中心解决什么=动态实例与健康/元数据发现；②负载均衡为什么不只是轮询=要考虑健康、延迟、热点和局部性；③重试怎么安全=共享 deadline、错误分类、幂等键和去重状态。

---

### 核心悬念

**"RPC 有了重试、熔断和降级，但 20 个服务组成的调用链仍可能互相依赖、循环调用和整体雪崩；如何从服务边界设计架构？"**

→ 引出 03-distributed-theory-architecture — 微服务边界、CAP、拆分与架构权衡。