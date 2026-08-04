# P06：RPC 与数据库中间件 — 详细规划

> Phase: P06 | 周期: 2周 | 书: 0本(MinerU) + 2本(需补)
> 依赖: P01（Netty，Dubbo 网络层基于 Netty） + P02（MQ 可做 RPC 异步解耦） + P03（ZK 可做 Dubbo 注册中心）
> 定位: 补齐中间件全景的最后两块——RPC通信 + 数据库分片

---

## 一、为什么这一站放在最后

P06 是"连接型"中间件：

- **Dubbo**：连接服务之间——它的服务发现(Nacos/ZK)、网络通信(Netty)、负载均衡、容错——每一项都依赖前面学过的知识
- **ShardingSphere**：连接应用与数据库之间——它的代理/编排模式、分片路由、读写分离——需要 MySQL 和分布式理论基础

**只有学完 P01-P05，才能理解 P06 各层的依赖。**

---

## 二、书单（均需补）

### 2.1 Dubbo（Tier 2 使用）

| 编号 | 推荐资源 | 定位 | 状态 |
|------|---------|------|------|
| Dubbo-01 | 《Apache Dubbo 微服务开发实战》GitChat | 使用层入门 | ❌ 需补 |
| Dubbo-02 | Apache Dubbo 3.x 源码（GitHub apache/dubbo） | 源码级理解 | ✅ 开源 |

**学习要点（结合 sca-lab 已有的服务治理基础）**：

| 模块 | 源码关键类 | 知识点 |
|------|-----------|--------|
| 服务导出 | `ServiceConfig.java`, `ServiceBuilder.java` | 服务如何注册到注册中心（与 Nacos 对接） |
| 服务引用 | `ReferenceConfig.java`, `ReferenceBuilder.java` | 消费者如何生成代理（ProxyFactory.getProxy） |
| RPC 协议 | `DubboProtocol.java`, `TripleProtocol.java` | Dubbo 协议 vs Triple 协议（gRPC 兼容） |
| 网络层 | `NettyServer.java`, `NettyClient.java`, `NettyTransporter.java` | **连接 P01 Netty**——Dubbo 如何封装 Netty |
| 注册中心 | `NacosRegistry.java`, `ZookeeperRegistry.java` | **连接 P03 ZK/Etcd**——服务注册发现 |
| 负载均衡 | `RandomLoadBalance.java`, `ConsistentHashLoadBalance.java` | 与 P02 RocketMQ queue 负载均衡对比 |
| 集群容错 | `FailoverClusterInvoker.java`, `FailfastClusterInvoker.java` | Failover/Failfast/Failsafe/Forking 策略 |
| 服务治理 | `RouterChain.java`, `ConditionRouter.java` | 路由规则（同 P02 消费者 Rebalance 对比） |
| 3.x 新特性 | 应用级服务发现、Triple 协议、柔性服务 | 接口级→应用级的演进（与 Nacos 2.x gRPC 演变对照） |

### 2.2 ShardingSphere（Tier 2 使用）

| 编号 | 推荐资源 | 定位 | 状态 |
|------|---------|------|------|
| Shard-01 | 《ShardingSphere核心原理精讲》(Peng Wei / 电子书) | 核心原理 | ❌ 需补 |
| Shard-02 | Apache ShardingSphere 5.x 源码（GitHub apache/shardingsphere） | 源码级理解 | ✅ 开源 |

**学习要点**：

| 模块 | 源码关键类 | 知识点 |
|------|-----------|--------|
| 分片引擎 | `ShardingRouter.java`, `ShardingStandardRoutingEngine.java` | **与 P02 RocketMQ 队列路由（MessageQueueSelector）对比** |
| 分片算法 | `InlineShardingAlgorithm.java`, `ModShardingAlgorithm.java` | 取模/Hash/范围分片 |
| 读写分离 | `ReadwriteSplittingDataSourceRouter.java` | 主库写/从库读路由 |
| 数据加密 | `EncryptAlgorithm.java` | 数据脱敏 |
| 影子库 | `ShadowAlgorithm.java` | 压测流量隔离 |
| 编排 | `RegistryCenter.java`（ZK/Nacos/Etcd对接） | **连接 P03**——配置中心驱动分片规则 |
| Proxy 模式 | `ShardingSphereProxy.java` | 代理模式（与 MySQL Proxy 对比） |
| JDBC 模式 | `ShardingSphereDriver.java` | 驱动模式（应用内分片） |
| SQL 解析 | `SQLParserEngine.java` (Antlr4) | SQL 解析→改写→路由→执行→归并 |

---

## 三、关键设计哲学

### Dubbo

| 设计决策 | 反事实假设 | 为什么选这个 |
|---------|-----------|-------------|
| **为什么 Dubbo 3.x 从接口级转向应用级服务发现？** | 保持接口级 | 接口级→注册中心存储 O(接口数×节点数)，应用级→O(节点数)——元数据膨胀问题 |
| **为什么 Triple 协议（gRPC 兼容）是新默认？** | 保持 Dubbo 协议 | gRPC 成为跨语言 RPC 标准——Triple 兼容 gRPC 使 Dubbo 不再是 Java 孤岛 |
| **为什么负载均衡在客户端而不是服务端？** | 服务端 LB | 去中心化——客户端持有服务列表，自己决定调用哪台（与 P05 Redis Cluster 客户端的 MOVED 重定向对比） |

### ShardingSphere

| 设计决策 | 反事实假设 | 为什么选这个 |
|---------|-----------|-------------|
| **为什么有 JDBC/Proxy 两种模式？** | 单模式 | JDBC 零额外组件(应用内)，Proxy 独立部署(语言无关)——覆盖不同场景 |
| **为什么分片键必须出现在大部分 SQL 中？** | 无限制 | 没有分片键 → 全库路由 → 性能退化到单库不如——这是"设计约束换取性能"的典型案例 |

---

## 四、与 sca-lab 的对接

P06 的两个中间件都在 sca-lab 中有对应的实践基础：

| sca-lab 模块 | 对接的 P06 知识点 |
|-------------|------------------|
| **my-xhs nacos 模块** | Dubbo 注册中心（Nacos/NamingService）——sca-lab 已手写 Nacos Client(C1)，注册机制已掌握 |
| **my-xhs counter 模块** | ShardingSphere 分表——counter 模块有按 user_id 分表的设计，与 ShardingSphere 分片策略对照学习 |
| **training-camp stage-1 服务治理** | Dubbo 服务治理（RPC/负载均衡/容错/可观测）——24节已覆盖 Dubbo + Spring Cloud 混合栈 |

**P06 是在 sca-lab "使用层" 基础上补足 "原理层"**——Dubbo 源码理解、ShardingSphere 架构理解。

---

## 五、生产陷阱

### Dubbo

| # | 陷阱 | 场景 | 修复 |
|---|------|------|------|
| 1 | **注册中心推送延迟 → 调用到已下线节点** | Provider 下线 → 注册中心通知 Consumer → Consumer 本地缓存未更新 | 开启 Consumer 端的重试机制 + 设置合理缓存刷新间隔 |
| 2 | **泛化调用参数类型不匹配** | `genericService.$invoke()` 传参类型错误 → 服务端反序列化失败 | 使用 `TypeUtils.isMatch()` 预检参数类型 |
| 3 | **线程池耗尽** | 所有 Dubbo 业务线程被慢请求占满 → 新请求排队超时 | 线程池隔离（不同服务或不同方法使用独立线程池） |

### ShardingSphere

| # | 陷阱 | 场景 | 修复 |
|---|------|------|------|
| 1 | **跨库 JOIN** | 分片键不在 JOIN 条件中 → 全库路由 + 内存 JOIN → OOM | 设计时避免跨库 JOIN，或使用广播表 |
| 2 | **分布式 ID 时钟回拨** | Snowflake 算法服务器时钟回拨 → ID 重复 | 使用 Leaf-segment（号段模式）替代时钟依赖 |
| 3 | **分片键选择错误 → 数据倾斜** | 用不均衡的字段做分片键（如 status=active占80%）→ 热点分片 | 选择分布均匀的字段（如 user_id hash） |

---

## 六、面试 Q&A

| 问题 | 面试官意图 | 回答主线 |
|------|----------|---------|
| "Dubbo 的一次 RPC 调用经历了哪些步骤？" | 考察全链路理解 | Proxy代理 → Cluster容错 → Directory路由 → LoadBalance负载均衡 → Filter链 → Protocol编码 → Transport(Netty)发送 → 服务端反向链路 |
| "Dubbo 和 Spring Cloud 的区别？" | 考察技术选型能力 | Dubbo是RPC框架(性能优先/二进制协议)，Spring Cloud是微服务生态(HTTP/REST/配置/网关全套)——sca-lab 把两者用 Spring Cloud Alibaba 整合 |
| "ShardingSphere 的 JDBC 模式和 Proxy 模式怎么选？" | 考察架构决策 | JDBC=应用内(零额外组件/Java限定/性能好)，Proxy=独立部署(语言无关/运维复杂/多一层网络)——Java 项目用 JDBC |
| "分片键怎么选？" | 考察分布式数据库设计 | 1)分布均匀(避免热点) 2)大部分查询能定位到单分片(避免全库路由) 3)稳定(不频繁变更) |

---

## 七、产出物（Phase C）

```
md/P06-Dubbo/（待补书后产出）
├── D-01-Dubbo服务导出与引用.md          # ServiceConfig → export → register → ProxyFactory
├── D-02-Dubbo RPC协议与Netty传输.md      # DubboProtocol → NettyServer → ExchangeHandler 链路
├── D-03-集群容错与负载均衡.md            # Cluster → Directory → Router → LoadBalance

md/P06-ShardingSphere/（待补书后产出）
├── D-01-分片引擎与SQL路由.md             # ShardingRouter → SQL改写 → 归并引擎
├── D-02-JDBC与Proxy模式架构对比.md       # 驱动模式 vs 代理模式
└── D-03-生产陷阱与面试Q&A.md             # 6个陷阱 + 4个面试题
```

## 八、学习检查清单

- [ ] 能手画 Dubbo RPC 调用的完整 10 步链路（从 Consumer Proxy 到 Provider Filter）
- [ ] 能解释 Dubbo 3.x 从接口级到应用级服务发现的动机（元数据膨胀问题）
- [ ] 能写出 ShardingSphere 一个跨分片查询的 SQL 改写过程（逻辑 SQL → 物理 SQL × N）
- [ ] 能对比 Dubbo 的 `FailoverCluster` 和 RocketMQ 的消息重试机制的差异（RPC层 vs 消息层重试）
