# 源码阅读清单 —— 从 P0(必读) 到 P3(了解)

> 阅读方法：不要逐行通读。每个模块先理解设计思想（看文档/博客/架构图），再带着问题看源码，最后自己画一张图。

---

## P0：必读 —— 这 6 个不看，地基不牢

### 1. AQS 框架 (Phase 1, Week 3)
```
java.util.concurrent.locks.AbstractQueuedSynchronizer
```
- **核心方法**：`acquire()` / `release()` / `acquireShared()` / `releaseShared()`
- **CLH 变种队列**：`Node` 内部类，前驱/后继指针，状态位
- **条件队列**：`ConditionObject`，单向链表
- **为什么重要**：ReentrantLock / Semaphore / CountDownLatch / ReentrantReadWriteLock 全部基于它
- **阅读技巧**：先理解 `acquire(1)` 这 5 行代码的每一步，再推广到共享模式

### 2. ThreadPoolExecutor (Phase 1, Week 4)
```
java.util.concurrent.ThreadPoolExecutor
```
- **核心方法**：`execute(Runnable)` → `addWorker()` → `runWorker()` → `getTask()`
- **ctl 位运算**：`AtomicInteger ctl`，高 3 位存运行状态，低 29 位存线程数
- **Worker 内部类**：继承 AQS，实现 Runnable
- **为什么重要**：面试高频 + 实际线上 OOM 的根源
- **阅读技巧**：从一个 `execute()` 调用开始，画时序图追踪

### 3. HashMap + ConcurrentHashMap (Phase 1, Week 5)
```
java.util.HashMap.putVal() / resize() / treeifyBin()
java.util.concurrent.ConcurrentHashMap.putVal() / transfer() / addCount()
```
- **HashMap**：hash 扰动函数、取模技巧 `(n-1)&hash`、扩容时的 `(e.hash & oldCap)` 高位判断
- **ConcurrentHashMap**：sizeCtl 字段的多重语义（初始化/-扩容-/阈值）、多线程协作扩容 `transfer()`
- **为什么重要**：最常用的数据结构，面试必问

### 4. Spring Bean 生命周期 (Phase 2, Week 7-8)
```
org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory
```
- **核心方法**：`doCreateBean()` — Bean 创建的 13 步核心流程
- **refresh() 13 步**：`AbstractApplicationContext.refresh()`
- **关键节点**：`populateBean()` (属性填充) / `initializeBean()` (初始化回调)
- **为什么重要**：理解了它，Spring 的 @Autowired / @PostConstruct / BeanPostProcessor 全通了

### 5. AOP 代理链 (Phase 2, Week 7-8)
```
org.springframework.aop.framework.JdkDynamicAopProxy.invoke()
org.springframework.aop.framework.CglibAopProxy.DynamicAdvisedInterceptor.intercept()
org.springframework.aop.framework.ReflectiveMethodInvocation.proceed()
```
- **核心**：`ReflectiveMethodInvocation.proceed()` — 责任链模式，递归调用
- **为什么重要**：理解 AOP 的切面执行顺序、事务代理的本质

### 6. MySQL Explain 执行计划 (Phase 2, Week 10)
```
不是 Java 源码，是 SQL 优化必会
EXPLAIN SELECT ... 的输出解读：
- type: 从 const → eq_ref → ref → range → index → ALL
- key: 命中的索引
- Extra: Using filesort / Using temporary / Using index
```
- **为什么重要**：慢 SQL 优化的起点

---

## P1：重要 —— 这 6 个打通中间件原理

### 7. MyBatis 执行链 (Phase 2, Week 11)
```
org.apache.ibatis.session.defaults.DefaultSqlSession.selectList()
org.apache.ibatis.executor.BaseExecutor.query()
org.apache.ibatis.plugin.InterceptorChain.pluginAll()
```
- **插件链**：`InterceptorChain` 如何层层代理 Executor → StatementHandler → ResultSetHandler
- **缓存**：`PerpetualCache` (一级) / `TransactionalCacheManager` (二级)

### 8. Netty ChannelPipeline (Phase 1, Week 6)
```
io.netty.channel.DefaultChannelPipeline
io.netty.channel.nio.NioEventLoop
io.netty.buffer.PooledByteBufAllocator
```
- **责任链**：`ChannelHandlerContext` 双向链表，支持动态增删
- **EventLoop**：单线程事件循环模型
- **为什么重要**：Gateway / RPC 底层都是它

### 9. Sentinel 责任链 (Phase 3/4, Week 15/30)
```
com.alibaba.csp.sentinel.slots.DefaultSlotChainBuilder
com.alibaba.csp.sentinel.slots.block.flow.FlowSlot
com.alibaba.csp.sentinel.slots.block.degrade.DegradeSlot
```
- **核心**：`ProcessorSlotChain`，每个 Slot 处理一个维度的流量控制
- **滑动窗口**：`LeapArray`，环形数组 + 时间窗口

### 10. Nacos 服务注册 (Phase 3, Week 14)
```
com.alibaba.nacos.naming.consistency.ephemeral.distro.DistroConsistencyServiceImpl
com.alibaba.nacos.core.distributed.raft.JRaftServer
```
- **AP 模式**：Distro 协议 — 临时实例，健康检查 + 数据同步
- **CP 模式**：JRaft — 持久化服务，Leader 选举 + 日志复制

### 11. RocketMQ 事务消息 (Phase 3, Week 18)
```
org.apache.rocketmq.broker.transaction.queue.TransactionalMessageBridge
org.apache.rocketmq.broker.transaction.AbstractTransactionalMessageCheckListener
```
- **半消息**：如何存储不可见的 Half Message
- **回查机制**：15 级递增延时（10s → 30s → 1m → 2m → ... → 2h）

### 12. Seata AT 模式 (Phase 4, Week 28)
```
io.seata.rm.datasource.DataSourceProxy
io.seata.rm.datasource.undo.UndoLogManager
io.seata.tm.api.TransactionalTemplate
```
- **全局锁**：`SELECT FOR UPDATE` 实现写隔离
- **undo_log**：前镜像 + 后镜像，事务回滚时反向执行
- **为什么重要**：理解分布式事务的侵入方案

---

## P2：进阶 —— my-xhs 核心模块源码 (Phase 3)

> 以下所有路径前缀为 `/data/workspace/my-xhs/`

### 13. 网关 Filter 链 (Week 14)
```
my-xhs-gateway/src/main/java/com/myxhs/gateway/filter/
├── TrafficColoringFilter.java       # 6 个 Header 染色（最核心）
├── GatewayAuthFilter.java           # JWT 鉴权 + 黑名单
└── RateLimitFilter.java             # Sentinel 限流
```
- **阅读重点**：Filter 链的 Order 排序逻辑、染色标记如何在 Feign/MQ 中透传

### 14. AOP 三剑客源码 (Week 15)
```
my-xhs-common/src/main/java/com/myxhs/common/
├── aspect/
│   ├── RateLimitAspect.java         # Order=10, Redis Lua 滑动窗口
│   ├── DistributedLockAspect.java   # Order=50, Redisson 三种锁
│   └── IdempotentAspect.java        # Order=100, SET NX + SpEL
├── trace/
│   ├── TraceContext.java            # 6 个染色字段
│   └── TraceContextHolder.java      # ThreadLocal 持有
```
- **阅读重点**：三个 AOP 的降级策略（Redis 不可用时怎么办）

### 15. 库存分桶预扣减 (Week 17)
```
my-xhs-inventory/src/main/java/com/myxhs/inventory/
├── service/InventoryService.java        # 核心扣减（~700 行）
├── service/InventoryBucketService.java  # 分桶管理 + 动态扩容
└── resources/lua/
    ├── prededuct.lua                    # 预扣减原子脚本
    ├── confirm.lua                      # 确认扣减
    └── release.lua                      # 释放库存
```
- **阅读重点**：Lua 脚本的原子性保障、扩容算法（2→4→8→16）、扩容期间请求降级

### 16. 订单 5 步保障 + Event Sourcing (Week 18)
```
my-xhs-order/src/main/java/com/myxhs/order/
├── service/OrderService.java            # createOrder() 下单
├── service/OrderTransactionService.java # 独立事务
├── state/OrderStateMachine.java         # 状态机
└── model/OrderEvent.java                # 事件实体
```
- **阅读重点**：bizIdentifier 幂等设计、乐观锁 WHERE status=?、Event Sourcing 如何与主事务协同

### 17. 全链路追踪 (Week 15)
```
my-xhs-common/src/main/java/com/myxhs/common/trace/
├── FeignTraceInterceptor.java           # Feign Header 透传
├── MqTraceHelper.java                   # RocketMQ 消息头透传
└── TraceIdConfig.java                   # MDC 自动注入
```
- **阅读重点**：6 个染色标记如何在 HTTP → Feign → MQ 链路上不丢失

### 18. Zone 多区域 (Week 37)
```
my-xhs-common/src/main/java/com/myxhs/common/zone/
├── ZonePreferenceFilter.java            # 10 步决策算法
└── ZoneLocator.java                     # Zone 自动感知
```
- **阅读重点**：同 Zone 优先 + 跨 Zone 降级的策略实现

---

## P3：了解 —— 有余力可以看

### 19. ShardingSphere SQL 解析
```
org.apache.shardingsphere.infra.parser.ShardingSphereSQLParserEngine
org.apache.shardingsphere.sharding.route.engine.ShardingRouteEngineFactory
```
- SQL 解析 → 路由 → 改写 → 归并的完整链路

### 20. Canal Binlog 解析
```
com.alibaba.otter.canal.parse.inbound.mysql.MysqlEventParser
```
- MySQL Binlog 格式（ROW / STATEMENT / MIXED）
- Event 解析 → 过滤 → 投递到 MQ

### 21. Redisson 分布式锁
```
org.redisson.RedissonLock
org.redisson.RedissonReadWriteLock
```
- Watchdog 自动续期机制（默认 30s，每 10s 续期一次）
- Lua 脚本安全释放

### 22. CosId 号段模式
```
me.ahoo.cosid.segment.IdSegmentDistributor
```
- 双 Buffer 设计（当前号段 + 预加载号段）
- 号段耗尽时的同步等待策略

### 23. GraalVM Native Image (Phase 5, Week 38 可选)
```
com.oracle.svm.core.SubstrateUtil
```
- Closed World Assumption（假设所有类在编译时已知）
- Spring AOT hint 机制

### 24. OpenTelemetry Java Agent (Phase 6, Week 41)
```
io.opentelemetry.javaagent.OpenTelemetryAgent
io.opentelemetry.sdk.trace.SdkTracerProvider
```
- 自动插桩的字节码增强
- OTLP 协议数据结构

---

## 阅读顺序建议

```
Week 1-6:    P0 #1-3 (AQS → ThreadPoolExecutor → HashMap/ConcurrentHashMap)
Week 7-8:    P0 #4-5 (Spring Bean 生命周期 → AOP 代理链)
Week 9:      Spring Boot 自动装配链（在 P0 #4 基础上扩展）
Week 10-12:  P0 #6 (MySQL Explain) + P1 #7 (MyBatis)
Week 13:     读论文（Raft），不需要源码
Week 14-20:  P2 #13-18（my-xhs 源码全览）
Week 15:     穿插 P1 #9 (Sentinel)
Week 21-32:  优化阶段，回读 P0 #2 (ThreadPoolExecutor) + P1 #12 (Seata)
Week 33-40:  P2 #18 (Zone) + P3 #22 (CosId) + P3 #23 (GraalVM)
Week 41-46:  P3 #24 (OTel)
```

---

## 一行总结

| 优先级 | 数量 | 核心价值 |
|--------|------|---------|
| P0 必读 | 6 个 | 面试会考、线上会崩、同事会问——不看不行 |
| P1 重要 | 6 个 | 理解中间件原理，出了问题能定位 |
| P2 my-xhs | 6 个 | 你的面试项目作品，必须能讲清楚每一行 WHY |
| P3 了解 | 6 个 | 加分项，有余力再看不迟 |
