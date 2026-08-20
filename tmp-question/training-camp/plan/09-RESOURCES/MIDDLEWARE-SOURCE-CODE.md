# 中间件源码阅读清单 — 技术专家全景版

> 目标：不只是用，而是能解释每一个设计决策的 WHY。
> 标注：⭐ 必读源码 | 📖 必懂原理 | 👁 了解对比

---

## 一、注册与配置中心

| 中间件 | 语言 | 深度 | 核心模块 |
|--------|------|------|---------|
| **Nacos** ⭐ | Java | 读源码 | Distro 协议 / JRaft / 长轮询 |
| **Zookeeper** 📖 | Java | 懂原理 | ZAB 协议 / Watcher / 临时节点 / Leader 选举 |
| **Etcd** 👁 | Go | 了解 | Raft 实现 / MVCC / Watch / Lease |

**为什么要看三个？**
- Nacos = 你的 my-xhs 在用，必须会
- Zookeeper = 经典分布式协调器，面试 100% 会问"ZK 怎么做选举/分布式锁"
- Etcd = K8s 的存储后端，云原生标配

**Nacos 必读**：
```
com.alibaba.nacos.naming.consistency.ephemeral.distro.DistroConsistencyServiceImpl  # AP 协议
com.alibaba.nacos.core.distributed.raft.JRaftServer                                 # CP 协议（内嵌 JRaft）
com.alibaba.nacos.config.server.service.LongPollingService                          # 配置长轮询
```

**Zookeeper 必懂（不需要读完源码，但原理要能画图）**：
- ZAB 协议：Leader 选举（epoch + zxid 比较）、广播（两阶段提交变种）、崩溃恢复
- Watcher 机制：一次性触发 + 客户端重注册
- 临时节点 + Session 机制：心跳检测 → Session 过期 → 删除临时节点

**对比：Nacos vs Zookeeper vs Etcd**

| | Nacos | Zookeeper | Etcd |
|---|-------|-----------|------|
| 一致性 | Distro(AP) / JRaft(CP) | ZAB(CP) | Raft(CP) |
| 健康检查 | 主动探测 + 被动上报 | Session 心跳 | Lease 租约 |
| 配置管理 | 原生支持 | 笨重（用 Node 存） | 不推荐 |
| 适用场景 | 微服务全功能 | 分布式协调 | K8s 后端 / 服务发现 |

---

## 二、消息队列（⭐ 双修必修）

| 中间件 | 语言 | 深度 | 核心模块 |
|--------|------|------|---------|
| **RocketMQ** ⭐ | Java | 读源码 | CommitLog / ConsumeQueue / 事务消息 / DLedger |
| **Kafka** ⭐ | Java/Scala | 读源码 | LogSegment / ISR / 零拷贝 / 幂等生产者 |
| **Pulsar** 👁 | Java | 了解 | 计算存储分离 / BookKeeper / 分层存储 |

**为什么要两个都读？**
- RocketMQ = 阿里系 + 事务消息 + 延时消息，电商场景优势明显
- Kafka = 大数据标配 + 日志场景无敌 + 面试必问"Kafka 为什么快"

**RocketMQ 必读**（电商视角）：
```
org.apache.rocketmq.store.CommitLog                         # 所有消息顺序写，一个 Broker 一个
org.apache.rocketmq.store.ConsumeQueue                      # 按 Topic-Queue 索引，存 offset+size+tag
org.apache.rocketmq.broker.transaction.queue.TransactionalMessageBridge  # Half Message
```

**Kafka 必读**（大数据视角）：
```
kafka.log.LogSegment                   # 分段日志（.log + .index + .timeindex）
kafka.server.ReplicaManager            # 副本管理（ISR 维护）
org.apache.kafka.clients.producer.KafkaProducer  # 生产者：accumulator + sender 线程模型
```

**对比：RocketMQ vs Kafka** — 这是面试高频题

| | RocketMQ | Kafka |
|---|---------|-------|
| 存储模型 | CommitLog + ConsumeQueue 两级 | Partition → LogSegment |
| 消息模型 | Queue（轻量） | Partition（重量，决定并行度） |
| 事务消息 | **原生支持** | Kafka 0.11+ 支持但用的人少 |
| 延时消息 | **18 级内置** | 需要外挂 |
| 顺序消息 | 同一 Queue 内顺序 | 同一 Partition 内顺序 |
| 适用场景 | 电商/金融/事务 | 日志/流处理/大数据 |

**Pulsar 只需了解**：
- 计算存储分离：Broker 无状态 + BookKeeper 存数据，扩容优雅
- 分层存储：热数据在 BookKeeper，冷数据下沉到 S3/HDFS

---

## 三、流量控制

| 中间件 | 语言 | 深度 | 核心模块 |
|--------|------|------|---------|
| **Sentinel** ⭐ | Java | 读源码 | ProcessorSlotChain / LeapArray 滑动窗口 |
| **Resilience4j** 👁 | Java | 对比用 | CircuitBreaker / Bulkhead / RateLimiter |
| **Hystrix** 👁 | Java | 历史对比 | 为什么被 Sentinel 替代？ |

**Sentinel 必读**（Spring Cloud Alibaba 标配）：
```
com.alibaba.csp.sentinel.CtSph.entryWithPriority()          # 入口
com.alibaba.csp.sentinel.slotchain.ProcessorSlotChain       # 8 个 Slot 责任链
com.alibaba.csp.sentinel.slots.statistic.base.LeapArray     # 滑动窗口 = 环形数组
```

**Sentinel vs Resilience4j vs Hystrix**

| | Sentinel | Resilience4j | Hystrix |
|---|---------|-------------|---------|
| 控制台 | **Dashboard 实时** | 无原生控制台 | Dashboard 只读 |
| 规则存储 | 内存/Nacos/ZK | 代码/配置文件 | 代码 |
| 限流粒度 | 单机/集群/QPS/线程/热点 | 基本限流 | 基本限流 |
| 状态 | 活跃维护 | 活跃维护 | **停止维护** |

---

## 四、分布式事务

| 中间件 | 语言 | 深度 | 核心模块 |
|--------|------|------|---------|
| **Seata** ⭐ | Java | 读源码 | DataSourceProxy / undo_log / GlobalLock |
| **RocketMQ 事务消息** ⭐ | Java | 读源码 | Half Message + 回查 |

**Seata AT 模式必读**：
```
io.seata.rm.datasource.DataSourceProxy                     # 代理数据源，拦截所有 SQL
io.seata.rm.datasource.undo.UndoLogManager                 # undo_log 管理
io.seata.tm.api.TransactionalTemplate                      # 全局事务编排
```

**对比：Seata AT vs TCC vs Saga vs RocketMQ 事务消息**

| 方案 | 一致性 | 性能 | 侵入性 | 适用 |
|------|--------|------|--------|------|
| Seata AT | 强一致(读未提交) | 降 20-30% | 零侵入 | 通用 |
| Seata TCC | 强一致 | 降 5-10% | 高侵入 | 性能敏感 |
| Seata Saga | 最终一致 | 高 | 中侵入 | 长事务 |
| RocketMQ 事务消息 | 最终一致 | 高 | 中侵入 | 异步解耦 |

---

## 五、RPC 框架

| 中间件 | 语言 | 深度 | 核心模块 |
|--------|------|------|---------|
| **Dubbo** ⭐ | Java | 读源码 | SPI 扩展 / 服务导出引入 / Triple 协议 |
| **gRPC** 📖 | 多语言 | 懂原理 | HTTP/2 / Protobuf / Stream |
| **Feign** 📖 | Java | 懂原理 | 动态代理 / Decoder/Encoder / LoadBalancer |

**Dubbo 必读**：
```
org.apache.dubbo.common.extension.ExtensionLoader          # SPI 扩展机制 ⭐
org.apache.dubbo.config.ServiceConfig.export()             # 服务导出：端口监听 + 注册
org.apache.dubbo.config.ReferenceConfig.get()              # 服务引入：订阅 + 代理
org.apache.dubbo.rpc.protocol.tri.TripleProtocol           # Dubbo 3 的 gRPC 兼容协议
```

**对比：Dubbo vs gRPC vs Feign**

| | Dubbo | gRPC | Feign |
|---|-------|------|-------|
| 协议 | 自定义/Triple(HTTP/2) | HTTP/2 | HTTP/1.1 |
| 序列化 | Hessian2/Protobuf/JSON | Protobuf | JSON/XML |
| 性能 | 高 | 高 | 中（HTTP/1.1 限制） |
| 服务治理 | 完善（注册/路由/限流） | 弱（靠 Service Mesh） | 弱（靠 Spring Cloud） |
| 跨语言 | Dubbo 3 Triple 兼容 gRPC | 原生多语言 | 仅 Java |

---

## 六、分布式缓存

| 中间件 | 语言 | 深度 | 核心模块 |
|--------|------|------|---------|
| **Redis** 📖⭐ | C | 懂原理（数据结构 + 持久化 + 集群） |
| **Redisson** ⭐ | Java | 读 Watchdog | `RedissonLock` / Lua 脚本 / Watchdog |

**Redis 必懂（C 源码不读，但必须会画图）**：
- 数据结构：SDS / ziplist / quicklist / skiplist / intset / dict
- 持久化：RDB（bgsave + Copy-On-Write）/ AOF（重写机制）/ 混合持久化
- 集群：16384 槽位 / MOVED vs ASK / Gossip 协议
- 线程模型：单线程 Reactor → 6.0+ 多线程 IO

**Redisson 必读**（Java 实现的分布式锁范本）：
```
org.redisson.RedissonLock.tryAcquireAsync()                # 可重入锁
scheduleExpirationRenewal()                                # Watchdog：30s 过期，每 10s 续期
Lua 释放脚本：if get(key) == threadId then del(key)       # 防误删
```

---

## 七、搜索引擎

| 中间件 | 语言 | 深度 | 核心概念 |
|--------|------|------|---------|
| **Elasticsearch** 📖 | Java | 懂原理 | 倒排索引 / 分片与副本 / 近实时搜索 |

**ES 必懂（源码不读，但概念必须清楚）**：
- 倒排索引：Term → PostingList → DocId（Roaring Bitmap 压缩）
- 写流程：写入 Memory Buffer → 每秒 Refresh 到 Segment → Translog 持久化 → Flush 到磁盘
- 搜索流程：Query Phase（协调节点 → 各分片，返回 DocId + Score）→ Fetch Phase（根据 DocId 拉数据）
- 分页：`from+size` 深分页问题 → `search_after` 替代方案

---

## 八、分布式存储与分库分表

| 中间件 | 语言 | 深度 | 核心模块 |
|--------|------|------|---------|
| **ShardingSphere-JDBC** ⭐ | Java | 读路由 | SQL 解析 → 路由 → 改写 → 归并 |
| **MySQL MGR** 📖 | C | 懂原理 | Group Replication / Paxos |

**ShardingSphere 必读**（分库分表的事实标准）：
```
org.apache.shardingsphere.infra.parser.ShardingSphereSQLParserEngine   # SQL 解析
org.apache.shardingsphere.sharding.route.engine.ShardingRouteEngine     # 路由：分片键 → 数据源+表名
org.apache.shardingsphere.sharding.rewrite.ShardingSQLRewriteEngine     # 改写：表名/主键改写
org.apache.shardingsphere.sharding.merge.DQLResultMerger                # 归并：排序/分页/聚合
```

---

## 九、可观测性

| 中间件 | 语言 | 深度 | 核心模块 |
|--------|------|------|---------|
| **OpenTelemetry** ⭐ | Java Agent | 懂架构 | 三大支柱统一（Trace + Metrics + Log） |
| **SkyWalking** 📖 | Java | 对比用 | Agent 架构 / Trace 数据结构 |
| **Prometheus** 📖 | Go | 懂原理 | Pull 模型 / TSDB / PromQL |
| **ELK** 📖 | Java | 懂原理 | Elasticsearch + Logstash + Kibana |

**OTel 架构必懂**：
```
App → OTel Agent(字节码增强) → OTLP(gRPC/HTTP) → Collector(接收/处理/导出)
    → Tempo(Trace) / Mimir(Metrics) / Loki(Log)    ← Grafana 统一查询
```

---

## 十、云原生与容器编排

| 中间件 | 语言 | 深度 | 核心概念 |
|--------|------|------|---------|
| **Kubernetes** 📖 | Go | 懂架构 | Pod/Deployment/Service/Ingress/HPA |
| **Istio** 👁 | Go | 了解 | Sidecar / VirtualService / DestinationRule |

**K8s 必懂（不是 Java 源码，但架构必须会画）**：
- Pod：最小调度单位，共享 Network/IPC 命名空间
- Service：ClusterIP/NodePort/LoadBalancer，kube-proxy iptables/IPVS
- HPA：CPU/Memory/QPS 三指标弹性伸缩
- 滚动更新：`maxSurge` + `maxUnavailable` 控制新旧 Pod 数量

---

## 阅读优先级矩阵

按投入产出比排序：

```
P0 必读源码（5 个）：
  Sentinel      — 一晚上读完，可读性最好
  RocketMQ      — 存储层是精华，3 天
  Dubbo         — SPI 是精髓，2 天
  Nacos         — AP/CP 对比，2 天
  Redisson      — Watchdog 机制，半天

P1 必读源码（3 个）：
  Seata         — AT 模式 DataSourceProxy，2 天
  ShardingSphere — SQL 路由链路，2 天
  Kafka         — LogSegment + 零拷贝，2 天

P2 必懂原理（5 个）：
  Redis         — 不读 C 源码，但数据结构/持久化/集群要能画图
  Zookeeper     — ZAB 协议 + Watcher 要能讲清楚
  ES            — 倒排索引 + 写流程 + 搜索流程要能画图
  Prometheus    — TSDB + Pull 模型 + PromQL
  K8s           — 核心概念（Pod → Deployment → Service → Ingress → HPA）
```

**一句话建议**：先把 P0 读完，这些是面试中"你读过哪些中间件源码"的标准答案。P1 选 1-2 个感兴趣的深入。P2 只要能讲清楚原理（画图 + 123 要点）就够了。