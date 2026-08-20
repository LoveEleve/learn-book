# 中间件全景清单 — 全部 38 个

> 按 10 大类排列。标注：⭐ 必读源码 | 📖 必懂原理 | 👁 了解即可

---

## 一、注册与配置中心（5 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 1 | **Nacos** | ⭐ | W14 | 服务注册发现 + 动态配置 | my-xhs 的注册配置中心，Spring Cloud Alibaba 标配 |
| 2 | **Zookeeper** | ⭐ | W13 | 分布式协调 + 分布式锁 + 选主 | 经典的 CP 系统，ZAB 协议面试必考，源码必读 |
| 3 | **Etcd** | 👁 | W37(多活) | 分布式 KV | K8s 的存储后端，云原生标配 |
| 4 | **Consul** | 👁 | W14(对比) | 服务发现 + KV | HashiCorp 出品，Nacos 的竞品对比 |
| 5 | **Eureka** | 👁 | W14(历史) | 服务注册 | Netflix 出品，已停止维护，只用于理解演进历史 |

**核心对比**：
```
Nacos AP(Distro) + CP(JRaft) → 微服务全功能
Zookeeper CP(ZAB) → 分布式协调，但配置管理笨重
Etcd CP(Raft) → K8s 后端，云原生场景
```

**Zookeeper 必读源码**：
```
org.apache.zookeeper.server.quorum.LeaderElection             # Leader 选举
org.apache.zookeeper.server.quorum.FastLeaderElection          # Fast Paxos 选举算法 ⭐
org.apache.zookeeper.server.quorum.QuorumPeer                  # 节点角色管理（LOOKING/LEADING/FOLLOWING）
org.apache.zookeeper.server.ZooKeeperServer                    # 核心服务器
org.apache.zookeeper.server.SessionTrackerImpl                 # Session 管理 + 心跳检测
org.apache.zookeeper.server.PrepRequestProcessor               # 请求预处理（事务日志写入）
org.apache.zookeeper.server.FinalRequestProcessor              # 请求最终处理（内存 DataTree 更新）
org.apache.zookeeper.server.DataTree                           # 数据树（ZK 的"内存数据库"）⭐
org.apache.zookeeper.server.watch.WatchManager                 # Watcher 注册与触发
org.apache.zookeeper.ZooKeeper                                 # 客户端 API
```

**阅读路径**：客户端 `create()` → `PrepRequestProcessor` → 事务日志 → `FinalRequestProcessor` → `DataTree` 更新 → Watcher 触发
**必理解**：ZAB 协议的两阶段提交变种（Leader 发出 Proposal → Follower 回复 ACK → 半数以上 ACK 后 Commit）。Watch 为什么是一次性的？

---

## 二、消息队列（3 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 6 | **RocketMQ** | ⭐ | W18 | 事务消息 + 延时消息 + 顺序消息 | my-xhs 的消息队列，电商场景最优 |
| 7 | **Kafka** | ⭐ | W17 | 高吞吐消息 + 日志 + 流处理 | 大数据标配，RocketMQ 的必对比对象 |
| 8 | **Pulsar** | 👁 | W17(对比) | 计算存储分离的消息队列 | 下一代 MQ 代表，理解架构创新 |

**核心对比**：
```
RocketMQ：CommitLog + ConsumeQueue，事务/延时原生支持
Kafka：Partition → LogSegment，零拷贝极致吞吐
Pulsar：Broker 无状态 + BookKeeper 存储，扩容更优雅
```

---

## 三、流量控制（3 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 9 | **Sentinel** | ⭐ | W15/W30 | 限流 + 熔断 + 降级 + 热点保护 | Spring Cloud Alibaba 标配 |
| 10 | **Resilience4j** | 👁 | W30(对比) | 断路器 + 限流 + 重试 | Spring 官方推荐，Sentinel 的对比对象 |
| 11 | **Hystrix** | 👁 | W30(历史) | 断路器(已停止维护) | 理解为什么被 Sentinel 替代 |

---

## 四、分布式事务（2 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 12 | **Seata** | ⭐ | W28 | AT / TCC / Saga 分布式事务 | Spring Cloud Alibaba 标配 |
| 13 | **RocketMQ 事务消息** | ⭐ | W18/W28 | Half Message + 回查 | 最终一致性的另一种实现 |

**核心对比**：
```
Seata AT：零侵入 + 全局锁，性能降 20-30%
Seata TCC：高侵入 + 高性能，降 5-10%
RocketMQ 事务消息：异步解耦 + 最终一致
```

---

## 五、RPC 框架（3 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 14 | **Dubbo** | ⭐ | W16 | 高性能 RPC + 服务治理 | Spring Cloud Alibaba 标配 |
| 15 | **gRPC** | 📖 | W16(对比) | HTTP/2 + Protobuf 跨语言 RPC | Dubbo Triple 兼容 gRPC |
| 16 | **Feign** | 📖 | W16 | 声明式 HTTP 客户端 | my-xhs 中与 Dubbo 同时使用 |

**核心对比**：
```
Dubbo：SPI 扩展 + 全链路服务治理
gRPC：HTTP/2 + Protobuf + 多语言
Feign：REST 风格 + 最简单 + 性能最弱
```

---

## 六、分布式缓存（2 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 17 | **Redis** | ⭐ | W12/W19/W25 | 缓存 + 分布式锁 + 持久化 + 集群 | 最核心的中间件之一 |
| 18 | **Redisson** | ⭐ | W15 | Redis Java 客户端(分布式锁) | my-xhs 的 @DistributedLock 底层实现 |

**Redis 核心源码**（C 语言，不逐行读，但关键结构必须看过）：
```
src/server.h        → redisServer / redisDb / redisObject 核心结构体定义
src/sds.h           → SDS（Simple Dynamic String）结构，二进制安全的字符串
src/ziplist.c       → 紧凑的连续内存列表，Redis 3.2 前 List/Hash/ZSet 的底层
src/quicklist.c     → Redis 3.2+ 的 List 底层：ziplist 的双向链表
src/skiplist.c      → 跳表，ZSet 的核心数据结构 ⭐
src/dict.c          → 哈希表，渐进式 rehash（每次只 rehash 一个 bucket）⭐
src/intset.c        → 整数集合，Set 在全整数时的编码
src/rdb.c           → RDB 持久化：bgsave fork → Copy-On-Write
src/aof.c           → AOF 持久化：追加写命令 → Rewrite 机制
src/replication.c   → 主从同步：全量 RDB + 增量 replication buffer
src/cluster.c       → 集群模式：16384 槽位 / Gossip / MOVED/ASK 重定向 ⭐
src/expire.c        → 过期策略：惰性删除 + 定期删除
src/evict.c         → 淘汰策略：LRU / LFU / TTL / Random
src/networking.c    → 网络 IO（6.0+ 多线程 IO）
```

**Redisson 必读源码**：
```
org.redisson.RedissonLock.tryAcquireAsync()         # 可重入锁 + Lua 加锁脚本
org.redisson.RedissonLock.scheduleExpirationRenewal() # Watchdog：30s 过期，每 10s 续期
Lua 释放脚本：if get(key) == threadId then del(key)  # 防误删
```

---

## 七、API 网关（1 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 19 | **Spring Cloud Gateway** | ⭐ | W14 | 路由 + 过滤 + 限流 + 鉴权 | my-xhs 的网关 |

**Gateway 必读源码**：
```
org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator
    └── getRoutes() → 从 Nacos/配置文件加载路由定义
org.springframework.cloud.gateway.route.Route
    └── 路由实体：id → predicates → filters → uri
org.springframework.cloud.gateway.handler.FilteringWebHandler
    └── handle() → 构建 GatewayFilterChain → 依次执行 Filter
org.springframework.cloud.gateway.filter.GatewayFilterChain
    └── DefaultGatewayFilterChain → 递归调用 filter()
org.springframework.cloud.gateway.filter.NettyRoutingFilter
    └── filter() → 用 Netty HttpClient 转发请求到下游 → 接收响应回写
org.springframework.cloud.gateway.filter.GlobalFilter
    └── 全局 Filter 接口，所有路由都执行
org.springframework.cloud.gateway.filter.GatewayFilter
    └── 路由级 Filter 接口，仅指定路由执行
```

**阅读路径**：请求进入 → `RoutePredicateHandlerMapping` 匹配路由 → `FilteringWebHandler` 构建链 → GlobalFilter 链 → `NettyRoutingFilter` 转发 → 响应回写
**必理解**：GlobalFilter(Order) vs GatewayFilter 的区别？Gateway 底层用的是 Netty Server 还是 Tomcat？

---

## 八、搜索引擎（1 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 20 | **Elasticsearch** | ⭐ | W19/W36 | 全文搜索 + 聚合分析 | my-xhs 的搜索服务 |

**Elasticsearch 必读源码**（只读核心流程，ES 源码量巨大）：
```
org.elasticsearch.index.mapper.MapperService            # Mapping 解析与字段定义
org.elasticsearch.index.engine.InternalEngine            # 引擎核心：index/delete/get
org.elasticsearch.index.shard.IndexShard                 # 分片操作入口
org.elasticsearch.index.IndexService                     # 索引服务（管理多个 Shard）
org.elasticsearch.action.search.SearchRequest            # 搜索请求
org.elasticsearch.action.search.TransportSearchAction    # 搜索两阶段调度 ⭐
org.elasticsearch.search.query.QueryPhase                # Query Phase: 匹配并排序，返回 DocId+Score
org.elasticsearch.search.fetch.FetchPhase                # Fetch Phase: 根据 DocId 拉完整文档
org.elasticsearch.index.query.QueryShardContext           # 查询上下文
```

**阅读路径**（搜索）：`SearchRequest` → `TransportSearchAction` → Query Phase(各 Shard) → 协调节点归并 → Fetch Phase(各 Shard) → 返回
**必理解**：倒排索引的 PostingList（Term → DocId 的 Roaring Bitmap 压缩）。`refresh_interval=1s` 为什么是近实时搜索？写流程为什么先写 Translog？

---

## 九、数据处理与同步（2 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 21 | **ShardingSphere** | ⭐ | W24 | 分库分表 + 读写分离 + 数据加密 | my-xhs 订单分库分表的基础 |
| 22 | **Canal** | 📖 | W19/W42 | MySQL Binlog 增量订阅 | my-xhs 的 Binlog → ES/Redis 同步 |

**ShardingSphere 必读路由链路**：SQL 解析 → 路由 → 改写 → 归并
**Canal 必懂整体流程**：`MysqlEventParser` → `CanalEventSink` → MQ/ES

---

## 十、可观测性（5 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 23 | **OpenTelemetry** | ⭐ | W41 | Traces + Metrics + Logs 统一 | CNCF 标准，Phase 6 替换 SkyWalking |
| 24 | **SkyWalking** | ⭐ | W29 | APM 全链路追踪 + 拓扑图 | my-xhs 当前方案，学习后迁移到 OTel |

**SkyWalking 必读源码**：
```
org.apache.skywalking.apm.agent.SkyWalkingAgent              # premain() Agent 入口
org.apache.skywalking.apm.agent.core.plugin.PluginBootstrap   # 插件加载
org.apache.skywalking.apm.agent.core.context.TracingContext   # Trace 上下文管理 ⭐
org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan  # Span 抽象（Entry/Local/Exit）
org.apache.skywalking.apm.agent.core.context.trace.TraceSegment           # 一次请求的 Trace 片段
org.apache.skywalking.apm.agent.core.context.trace.NoopSpan               # 空对象模式
org.apache.skywalking.apm.agent.core.context.ContextManager               # 上下文管理器
org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstMethodsInter  # 方法拦截增强
org.apache.skywalking.oap.server.analyzer.provider.trace.parser.SegmentParserService  # OAP 端 Trace 解析
```

**阅读路径**：`premain()` → 加载 Plugin → 字节码增强 → `ContextManager.getOrCreate()` → `EntrySpan` / `ExitSpan` → 发送到 OAP → 解析 → 拓扑图生成
**必理解**：SkyWalking 的数据模型：Trace(Segment(Span))。Entry/Exit/Local 三种 Span 分别代表什么？Agent 如何做到不侵入业务代码？
| 25 | **Prometheus** | 📖 | W41 | 指标采集与告警 | 云原生监控事实标准 |
| 26 | **ELK** | 📖 | W42 | 日志采集/存储/查询 | my-xhs 的日志平台 |
| 27 | **Grafana** | 📖 | W41 | 统一可视化 | Tempo(Trace) + Loki(Log) + Mimir(Metrics) |

**SkyWalking → OTel 迁移是 Phase 6 的核心改造任务。**

---

## 十一、分布式 ID 生成器（1 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 28 | **CosId** | 📖 | W34 | 号段模式 + 雪花算法 | my-xhs 的分布式 ID 方案 |

**必懂**：双 Buffer 设计（当前号段 + 预加载号段）

---

## 十二、云原生与 Service Mesh（2 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 29 | **Kubernetes** | 📖 | W38/W45 | 容器编排 + 自动扩缩容 | 生产部署标配 |
| 30 | **Istio** | 👁 | W39 | Service Mesh | 流量管理 + 安全 + 可观测 |

**K8s 必懂**：Pod → Deployment → Service → Ingress → HPA
**Istio 了解**：VirtualService + DestinationRule + Envoy Sidecar

---

## 十三、数据库与存储（2 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 31 | **MySQL** | ⭐ | W10/W23 | 关系型数据库 | 最核心的存储层 |
| 32 | **MyBatis** | ⭐ | W11 | ORM 框架 | Java 生态最常用的数据访问层 |

**MySQL**：索引(B+Tree) / MVCC(undo log + ReadView) / 锁(Record/Gap/Next-Key)
**MyBatis**：SqlSession → Executor → StatementHandler 执行链

---

## 十四、基础设施与工具（5 个）

| # | 中间件 | 深度 | 所属周 | 做什么 | 为什么在清单里 |
|---|--------|------|--------|--------|--------------|
| 33 | **Netty** | ⭐ | W6 | 网络通信框架 | Gateway/Dubbo/RocketMQ 的地基 |
| 34 | **GraalVM** | 👁 | W38 | Native Image 编译 | Java 21 的新部署方式 |
| 33 | **Docker** | ⭐ | W38 | 容器化 + 镜像 + docker-compose | my-xhs 的部署方式 |

**Docker 必懂核心**（Go 源码不读，但核心概念必须精通）：
```
Dockerfile 指令：FROM / RUN / COPY / ADD / CMD / ENTRYPOINT / ENV / ARG / VOLUME / EXPOSE
镜像构建：UnionFS 分层文件系统 → 写时复制 Copy-on-Write
容器隔离：Namespace（PID/NET/MNT/UTS/IPC/User）+ Cgroups（CPU/内存/IO 限制）
网络模式：bridge / host / overlay / macvlan / none
Docker Compose：services → networks → volumes 编排
Dockerfile 最佳实践：多阶段构建、最小基础镜像(docker:slim)、层合并 RUN、.dockerignore
```

**必理解的 3 个问题**：
1. 镜像分层原理：为什么基础镜像层已存在时 `docker build` 很快？
2. ENTRYPOINT vs CMD：ENTRYPOINT 定义主程序，CMD 定义默认参数
3. 多阶段构建：第一段编译（用 maven:3-jdk-21），第二段运行（用 eclipse-temurin:21-jre-alpine），减小最终镜像
| 32 | **XXL-Job** | ⭐ | W18 | 分布式定时任务调度 | my-xhs 的超时关单兜底 |

**XXL-Job 必读源码**：
```
com.xxl.job.admin.core.trigger.XxlJobTrigger                # 任务触发入口
com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum        # 路由策略（FIRST/ROUND/RANDOM/HASH/LFU/LRU）
com.xxl.job.admin.core.scheduler.XxlJobScheduler             # 调度器主循环
com.xxl.job.admin.core.thread.JobTriggerPoolHelper            # 快慢线程池隔离
com.xxl.job.admin.core.model.XxlJobGroup                      # 执行器组（AppName 关联）
com.xxl.job.admin.core.model.XxlJobInfo                       # 任务定义（Cron + 路由策略 + 阻塞策略）
com.xxl.job.admin.core.model.XxlJobLog                        # 任务执行日志
com.xxl.job.core.biz.ExecutorBiz                              # 执行器接口（beat/idleBeat/kill/log/run）
com.xxl.job.core.biz.impl.ExecutorBizImpl                     # 执行器实现
com.xxl.job.core.thread.JobThread                             # 任务执行线程
```

**阅读路径**：`XxlJobScheduler.start()` → 时间轮扫描 Cron → `XxlJobTrigger.trigger()` → 路由策略选执行器 → `ExecutorBiz.run()` → `JobThread` 执行 → 回调 Admin 记录日志
**必理解**：快慢线程池隔离设计（快池 10 线程处理常规任务，慢池 100 线程处理慢任务）。阻塞策略（单机串行/丢弃后续/覆盖之前）的实现？
| 37 | **ChaosBlade** | 👁 | W44 | 混沌工程故障注入 | Phase 6 的混沌实验工具 |
| 38 | **Testcontainers** | 📖 | W20 | 集成测试容器 | Phase 4 起的测试方案 |

---

## 按阅读优先级排序

```
P0 必读源码（15 个）— 面试会问、线上会崩、必须读懂源码：

  1. Sentinel        — W15, 一晚上，可读性最好
  2. RocketMQ        — W18, 3 天，存储层是精华
  3. Dubbo           — W16, 2 天，SPI 是精髓
  4. Nacos           — W14, 2 天，AP/CP 对比
  5. Netty           — W6,  3 天，一切网络通信的地基
  6. Zookeeper       — W13, 2 天，FastLeaderElection + DataTree + Watcher
  7. Redis(架构)     — W12, 3 天，数据结构(skiplist/dict) + 持久化 + Cluster
  8. Redisson        — W15, 半天，Watchdog + Lua 脚本
  9. Gateway         — W14, 1 天，RouteLocator → Filter 链 → NettyRoutingFilter
 10. Seata           — W28, 2 天，DataSourceProxy + undo_log
 11. ShardingSphere  — W24, 2 天，SQL 解析→路由→改写→归并
 12. Elasticsearch   — W19, 2 天，倒排索引 + 搜索两阶段
 13. SkyWalking      — W29, 2 天，Agent 字节码增强 + Segment/Span 模型
 14. Docker          — W38, 1 天，分层镜像 + 多阶段构建 + docker-compose
 15. XXL-Job         — W18, 1 天，调度中心 + 路由策略 + 阻塞策略

P1 必懂原理（7 个）— 不读完整源码，但原理要能画图讲清楚：

 16. Kafka           — LogSegment/ISR/零拷贝/幂等生产者
 17. MySQL           — 索引/MVCC/锁/Explain
 18. Prometheus      — TSDB/Pull/PromQL
 19. K8s             — Pod/Deploy/Service/HPA
 20. OTel            — Agent/Collector/OTLP
 21. MyBatis         — SqlSession→Executor→StatementHandler
 22. Feign           — 动态代理/Decoder/Encoder/LoadBalancer

P2 了解对比（16 个）— 知道是什么、什么时候用、和同类有什么区别：

 23. gRPC            — HTTP/2/Protobuf/Stream
 24. Canal           — Binlog→MQ→ES链路
 25. CosId           — 双Buffer号段模式
 26. ELK             — Filebeat→Logstash→ES→Kibana
 27. GraalVM         — Native Image AOT 编译
 28. Istio           — Sidecar/VirtualService/DestinationRule
 29. ChaosBlade      — 混沌工程故障注入
 30. Etcd            — Raft KV + K8s 后端
 31. Consul          — 服务发现 + KV
 32. Eureka          — 历史：被 Nacos 替代的路
 33. Hystrix         — 历史：被 Sentinel 替代的路
 34. Resilience4j    — Sentinel 的轻量替代
 35. Pulsar          — 下一代 MQ
 36. Grafana         — 统一可视化面板
 37. Testcontainers  — 集成测试容器
 38. Kafka对比       — 和 RocketMQ 的 5 维度对比
```

---

## 按学习周阅读顺序

```
W6:    Netty ⭐
W10:   MySQL ⭐
W11:   MyBatis ⭐
W12:   Redis ⭐
W13:   Zookeeper ⭐
W14:   Nacos ⭐ + Gateway ⭐ + Eureka 👁 + Consul 👁
W15:   Sentinel ⭐ + Redisson ⭐
W16:   Dubbo ⭐ + Feign 📖 + gRPC 📖
W17:   Kafka ⭐ + Pulsar 👁
W18:   RocketMQ ⭐ + XXL-Job ⭐
W19:   Elasticsearch ⭐ + Canal 📖
W20:   Testcontainers 📖
W24:   ShardingSphere ⭐
W28:   Seata ⭐
W29:   SkyWalking ⭐
W30:   Sentinel ⭐(复习) + Resilience4j 👁 + Hystrix 👁
W34:   CosId 📖
W37:   Etcd 👁
W38:   Docker ⭐ + GraalVM 👁
W39:   Istio 👁
W41:   OpenTelemetry ⭐ + Prometheus 📖 + Grafana 📖
W42:   ELK 📖
W43:   Canal 📖(复习)
W44:   ChaosBlade 👁
W45:   K8s 📖
```

## ⭐ 汇总

```
P0 必读源码：15 个

ZK Redis Gateway ES Docker XXL-Job — 6 个新升级的 ⭐
Sentinel RocketMQ Dubbo Nacos Netty Redisson Seata ShardingSphere SkyWalking — 9 个原有的 ⭐
```
