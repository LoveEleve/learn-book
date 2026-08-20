# 全阶段检验标准清单

## Phase 1: Java 内核（第 1-6 周）

### 硬技能
- [ ] 能手绘 JVM 内存结构图（堆的 Young/Old/MetaSpace）
- [ ] 能用 jmap/jstack/jstat/jcmd 分析运行中的 Spring Boot 应用
- [ ] 能分析 GC 日志，判断 GC 是否正常
- [ ] 能根据业务场景推荐 GC 选型（CMS vs G1 vs ZGC）
- [ ] 能手绘 AQS 的 CLH 队列 acquire 流程
- [ ] 能解释 synchronized 锁膨胀的完整过程
- [ ] 能手写 ThreadPoolExecutor 的核心 execute 方法
- [ ] 能解释 Virtual Threads 的适用场景和局限
- [ ] 能手绘 HashMap 1.8 的 putVal 完整流程（含红黑树化）
- [ ] 能解释 ConcurrentHashMap 扩容时多线程如何协作
- [ ] 能手写 O(1) 的 LRU Cache
- [ ] 能手绘 Netty ChannelPipeline 职责链结构
- [ ] 能用 Netty 完成一个简单的 RPC 通信

### 软技能
- [ ] 能在 5 分钟内画出 JVM 内存结构图（白板）
- [ ] 能给别人讲清楚 Virtual Threads 和传统线程的区别

### 交付物
- [ ] MiniAQS + MiniReentrantLock（~450 行）
- [ ] MiniThreadPool（~350 行）
- [ ] LRUCache × 2 版本（~250 行）
- [ ] NioHttpServer + Netty RPC（~600 行）
- [ ] 5 篇博客

---

## Phase 2: Spring 内功（第 7-12 周）

### 硬技能
- [ ] 能手绘 Spring Bean 完整的 13 个生命周期节点
- [ ] 能手写 Mini-Spring 的 IOC + AOP + 事务管理
- [ ] 能列举 @Transactional 失效的 8 种场景并解释原因
- [ ] 能解释 Spring Boot 3.x 的 javax → jakarta 迁移影响
- [ ] 能写一个自定义 Starter
- [ ] 能手绘 MVCC 的 ReadView 判断逻辑
- [ ] 能用 EXPLAIN 分析任意 SQL 的执行计划
- [ ] 能解释最左前缀原则并设计联合索引
- [ ] 能解释 MyBatis 插件链的代理模式实现
- [ ] 能手绘 Redis skip list 的插入过程
- [ ] 能解释 Redis Cluster 的 16384 槽位分配逻辑
- [ ] 能手写 Mini-Redis 的核心数据结构

### 软技能
- [ ] 能画出 SQL 从客户端到磁盘的完整执行路径
- [ ] 能解释 Redis Sentinel 的故障转移过程（主观下线→客观下线→选举→切换）

### 交付物
- [ ] Mini-Spring（~2000 行）
- [ ] 自定义 Starter（~200 行）
- [ ] MyBatis 插件 × 2（~200 行）
- [ ] Mini-Redis（~500 行）
- [ ] 4 篇博客 + 2 份速查表

---

## Phase 3: my-xhs 源码深度阅读（第 13-20 周）

### 硬技能
- [ ] 能画出 my-xhs 全部 15 个微服务的架构拓扑图
- [ ] 能追踪下单请求从 Gateway → Order → MQ → Inventory 的完整路径
- [ ] 能解释库存分桶预扣减的三级保证和动态扩容算法
- [ ] 能解释 AOP 三剑客的 Order 排序原因（10:限流 → 50:锁 → 100:幂等）
- [ ] 能解释 Feign + MQ 如何透传 TraceContext 的 6 个标记
- [ ] 能画出 Canal 数据同步的完整链路（Binlog → Canal → MQ → ES/Redis）
- [ ] 能解释订单 5 步保障机制每步的设计原因
- [ ] 能对比 Event Sourcing 和传统 CRUD 的优缺点

### 软技能
- [ ] 能写一篇 5000 字的 my-xhs 架构全景分析

### 交付物
- [ ] my-xhs 完整架构图（15 微服务拓扑）
- [ ] 核心数据流图 × 3（下单/Feed/搜索）
- [ ] 3 篇深度博客
- [ ] Raft 状态机实现（~500 行）

---

## Phase 4: 高并发改造（第 21-32 周）

### 硬技能
- [ ] 能用 JMeter/Wrk 搭建完整压测体系
- [ ] 能调优 Tomcat/HikariCP/Async 线程池参数
- [ ] 能用 EXPLAIN 分析并优化慢 SQL
- [ ] 能验证 ShardingSphere 分库分表效果
- [ ] 能检验缓存一致性的 4 个边界 case
- [ ] 能对库存系统做专项压测并验证分桶效果
- [ ] 能验证分布式事务各方案的恢复能力
- [ ] 能完成 SkyWalking → OTel 迁移

### 量化目标
- [ ] 核心接口总 QPS ≥ 10,000
- [ ] P99 ≤ 100ms
- [ ] 库存超卖 = 0
- [ ] 缓存命中率 ≥ 90%
- [ ] 慢 SQL ≤ 5 条

### 交付物
- [ ] 完整压测报告（Before/After 10 项指标对比）
- [ ] 3 个真实优化 PR
- [ ] 10 个核心面试题答案

---

## Phase 5: 架构升级（第 33-40 周）

### 硬技能
- [ ] 能在一小时内完成一个系统设计题的完整方案
- [ ] 能独立完成容量估算（QPS/存储/带宽）
- [ ] 能对比 CosId 号段模式 vs Snowflake 的优劣
- [ ] 能手写 Redis Lua 滑动窗口限流器
- [ ] my-xhs 成功升级到 Java 21 + Spring Boot 3.3
- [ ] Virtual Threads 在 10,000 并发下 P99 ≤ 1.5s
- [ ] GraalVM Native Image 成功编译 gateway 服务
- [ ] my-xhs 接入 Istio 并实现灰度发布
- [ ] 能画出多活架构的完整部署拓扑图

### 交付物
- [ ] 《my-xhs V2 架构设计文档》
- [ ] Java 21 升级 PR（18 个文件）
- [ ] Virtual Threads 压测对比报告
- [ ] GraalVM Native Image 试点报告
- [ ] 系统设计题库 50+ 题
- [ ] 多活架构设计方案
- [ ] Istio 灰度发布配置

---

## Phase 6: 云原生运维（第 41-46 周）

### 硬技能
- [ ] SkyWalking → OTel 迁移完成，5 个 Grafana Dashboard 可用
- [ ] 15 个微服务全部 JSON 结构化日志 + MDC TraceId
- [ ] 能搭建 ELK Pipeline（Filebeat→Logstash→ES→Kibana）
- [ ] 安全渗透测试 5 个场景全部通过
- [ ] 10 个混沌实验全部执行，有 RTO/RPO 记录
- [ ] JVM + 容器资源精准化，月度成本降低 ≥ 40%
- [ ] K8s 部署 + HPA + 灰度发布可用

### 交付物
- [ ] 生产级运维手册（10 章）
- [ ] Docker Compose + K8s 双模部署
- [ ] 混沌工程实验报告
- [ ] 安全测试报告
- [ ] 成本优化报告

---

## Phase 7: 技术影响力（第 47-52 周）

### 硬技能
- [ ] 5 个开源 PR 被合并
- [ ] 5 篇深度博客发布（每篇 3000+ 字）
- [ ] 30 分钟技术演讲录制完成
- [ ] 50 道系统设计题准备好答案框架
- [ ] 3 轮模拟面试完成

### 交付物
- [ ] 10 张核心知识体系图
- [ ] my-xhs 开源版（README + 架构文档 + 部署指南）
- [ ] 《Java 技术专家成长手册》

---

## 季度里程碑检查点

### Q1 检查点（第 13 周）
- [ ] 能手写 MiniAQS + MiniThreadPool + LRUCache + Netty RPC
- [ ] 能手写 Mini-Spring + Mini-Redis
- [ ] 能用 EXPLAIN 分析慢 SQL
- [ ] my-xhs 环境搭建完成（Docker Compose 一键启动）

### Q2 检查点（第 26 周）
- [ ] 能画出 my-xhs 完整架构图
- [ ] 性能基线建立 + 第一轮优化完成
- [ ] 库存压测通过（10,000 QPS + 零超卖）
- [ ] 缓存一致性 4 个边界 case 通过

### Q3 检查点（第 39 周）
- [ ] Java 21 升级完成
- [ ] Virtual Threads 改造完成 + 压测对比
- [ ] 系统设计 50 题掌握
- [ ] Istio 灰度发布可用

### Q4 检查点（第 52 周）
- [ ] my-xhs V3 完整交付（K8s + 多活 + OTel）
- [ ] 5 PR + 5 博客 + 1 演讲
- [ ] 模拟面试通过
- [ ] my-xhs 开源 + 《成长手册》完成

---

## 自评规则

| 完成率 | 评价 | 行动 |
|--------|------|------|
| > 90% | 优秀 | 正常推进 |
| 70-90% | 良好 | 回顾薄弱项，下周补上 |
| 50-70% | 警告 | 减少本周新内容，先补完上周 |
| < 50% | 危险 | 复盘：时间不够还是难度过高？调整计划 |
