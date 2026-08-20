# SCC-7 ReactorLoadBalancer 策略 — 时空溯源 (代码注释锚, git shallow)

> git shallow (单提交) 无法考古; 溯源以 ocelli 源码引用 + @since 注释为锚。

## 演进链 (代码痕迹实证)

| 阶段 | 事件 | 证据 |
|:--:|:--|:--|
| 2.2.0 | **ReactorLoadBalancer 接口 + RoundRobin 诞生** — 响应式策略核心 | ReactorLoadBalancer.java:31 + RoundRobinLoadBalancer.java:43 |
| 2.2.7 | **RandomLoadBalancer 添加** — 随机策略 | RandomLoadBalancer.java:39 (@since 2.2.7) |
| 演进 | **ocelli 灵感引用**: 轮询实现源自 Netflix ocelli | RoundRobinLoadBalancer.java:80-81 注释 (ocelli-core 链接) |
| 3.x | **SingletonSupplier 惰性解析** — ObjectProvider 延迟获取 Supplier | RoundRobinLoadBalancer.java:64-66 |
| 当前 | **Noop 兜底**: provider.getIfAvailable(NoopSupplier::new) — 无 Supplier 不 NPE | L65 |

## 版本相关性结论

- **响应式策略自 2.2.0 定型** — ReactorLoadBalancer + RoundRobin 一次建立, 之后只加策略 (Random 2.2.7)
- **ocelli 是算法源头** — 轮询的 & MAX_VALUE 循环直接承袭 Netflix ocelli (注释链接)
- **惰性解析是 3.x 演进** — SingletonSupplier 解决"Supplier 创建晚于 LoadBalancer"的时序
- **Noop 兜底贯穿** — 无 Supplier/无实例都有兜底 (EmptyResponse/NoopSupplier), 防 NPE 设计
