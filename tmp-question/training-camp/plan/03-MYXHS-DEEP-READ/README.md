# Phase 3：my-xhs 源码深度阅读（第 13-20 周）

## 目标
深度阅读 my-xhs 全部 15 个微服务源码，理解每个设计决策背后的 WHY。画出完整架构图。

**阅读原则**：不只看怎么写的，问自己"为什么这么设计"。
**输出原则**：每读完一个模块，必须画出该模块的架构图/时序图/状态图。

---

## Week 13：分布式理论基础

> 本周不读 my-xhs，先建立理论基础。

### 理论知识
- **CAP**：P（分区容错）必须选，CP vs AP 的选择与后果
- **BASE**：Basically Available（基本可用）+ Soft State（软状态）+ Eventually Consistent（最终一致）
- **Raft**：Leader 选举（Term、RequestVote）、日志复制（AppendEntries）、安全性（选举限制+提交限制）

### 实践任务
1. 读论文《In Search of an Understandable Consensus Algorithm》Section 5（Raft 基础），精读 Figure 2
2. 用 Java 实现 Raft 状态机（`MiniRaft.java`，~500 行）：参考 Figure 2 的 Rules for Servers
3. 写 5 个 JUnit 测试验证：Leader 选举、日志复制、网络分区恢复

### 输出
- 博客：《Raft 论文精读：从 Figure 2 到 Java 实现》

---

## Week 14：my-xhs 网关 + 注册发现

### 精读目标
理解一个外部请求从进入网关到路由到下游服务的完整链路。

### 必读文件清单（按阅读顺序）

```
my-xhs-gateway/src/main/java/com/myxhs/gateway/
├── GatewayApplication.java                          // 启动类
├── config/
│   ├── RouteConfig.java                             // 15 条路由规则定义
│   └── SentinelConfig.java                          // Sentinel 网关限流
├── filter/
│   ├── TrafficColoringFilter.java                   // 流量染色：6 个 Header 注入
│   ├── HmacAuthFilter.java                          // HMAC 签名校验
│   ├── RateLimitFilter.java                         // 按服务 QPS 限流
│   ├── GatewayAuthFilter.java                       // JWT 鉴权 + Token 黑名单
│   └── RetryAndCircuitBreakerFilter.java            // 重试 + 熔断
```

### 阅读要点

**TrafficColoringFilter**（最核心的 Filter）：
- 染色标记：TraceId / UserId / GrayTag / ApiVersion / ABGroup / PressureTest
- 实现逻辑：从请求头解析 → 存入 ThreadLocal → 透传到下游
- 思考：为什么用 6 个 Header？灰度发布需要哪些标记？压测流量如何隔离？

**GatewayAuthFilter**：
- JWT 解析流程：提取 Bearer Token → 解析 Claims → 验证过期
- Token 黑名单：Redis SET 存储已注销 Token，TTL = Token 剩余有效时间
- 思考：为什么需要黑名单？只用过期时间不行吗？

**Filter 链 Order 分析**：
```
TrafficColoring(-5) → HmacAuth(-3) → RateLimit(-2) → GatewayAuth(0) → RetryAndCircuitBreaker(10)
```
- 染色最早（需要在整个链路可用）
- 鉴权在限流之后（先拒绝恶意流量）
- 思考：为什么 HMAC 在 RateLimit 之前？

### 实践任务
1. 画出网关 Filter 链的完整时序图（标注每个 Filter 的 Order、职责、输入输出）
2. 追踪一个带 JWT Token 的请求，用日志验证经过的 Filter 顺序
3. 修改 RateLimitFilter 的 user-service 限流阈值从 200 → 500，重启验证

### 输出
- 架构图：《my-xhs 网关 Filter 链全景图》

---

## Week 15：my-xhs-common 核心基础设施

### 精读目标
理解 my-xhs 最核心的 AOP 三剑客（@RateLimit / @DistributedLock / @Idempotent）实现原理。

### 必读文件清单

```
my-xhs-common/src/main/java/com/myxhs/common/
├── annotation/
│   ├── RateLimit.java                               // 限流注解
│   ├── DistributedLock.java                         // 分布式锁注解
│   └── Idempotent.java                              // 幂等注解
├── aspect/
│   ├── RateLimitAspect.java                         // 限流 AOP（Order=10）
│   ├── DistributedLockAspect.java                   // 分布式锁 AOP（Order=50）
│   └── IdempotentAspect.java                        // 幂等 AOP（Order=100）
├── trace/
│   ├── TraceContext.java                            // 追踪上下文（6 个字段）
│   ├── TraceContextHolder.java                      // ThreadLocal 持有
│   ├── FeignTraceInterceptor.java                   // Feign 拦截器透传
│   └── MqTraceHelper.java                           // RocketMQ 消息头透传
└── chaos/
    ├── ChaosInterceptor.java                        // 混沌工程故障注入
    └── ChaosProperties.java                         // 混沌配置
```

### 阅读要点

**RateLimitAspect**（Order=10，第一道防线）：
- 限流算法：Redis Lua 脚本实现滑动窗口
- Lua 脚本：`redis.call('zadd', key, now, now)` → `redis.call('zremrangebyscore', key, 0, now-window)` → `redis.call('zcard', key)`
- 降级策略：Redis 不可用时放行（`@RateLimit(fallback = "pass")`）
- 思考：为什么用 Redis Lua 而不用 Sentinel？什么场景各适合？

**DistributedLockAspect**（Order=50，第二道防线）：
- 锁类型：互斥锁/读写锁/公平锁
- Watchdog 自动续期：Redisson 默认 30s 锁，每 10s 续期
- Lua 安全释放：`if redis.call('get', key) == token then return redis.call('del', key)`
- 思考：为什么 Watchdog 是 30s 而不是 10s 或 60s？

**IdempotentAspect**（Order=100，第三道防线）：
- 实现：Redis SET NX + SpEL 表达式计算 Key
- 特殊处理：BizException 时自动删除幂等标记（允许业务异常重试）
- 思考：System Exception 为什么不需要删除标记？

### 实践任务
1. 画出 AOP 三剑客的执行层次图（含降级路径）
2. 追踪 my-xhs 的 `@RateLimit` 在 content-service 的使用位置（搜注解使用处）
3. 写一个测试：模拟 Redis 宕机，验证 @RateLimit 降级放行

### 输出
- 博客：《my-xhs 的 AOP 三剑客：三个注解防并发的完整设计》

---

## Week 16：RPC 与消息队列

### 精读目标
理解 my-xhs 中服务间调用（Feign）和异步消息（RocketMQ）的完整实现。

### 必读文件

```
my-xhs-home/  → Feign 调用方：调用 6 个下游服务
my-xhs-order/ → RocketMQ 生产者：订单创建/延时关单
my-xhs-inventory/ → RocketMQ 消费者：异步扣库存
```

### 阅读要点

**Feign 调用链**：
- my-xhs-home 如何聚合 6 个下游服务
- `FeignTraceInterceptor` 如何透传 TraceContext 的 6 个 Header
- `CompletableFuture` 异步编排 6 个调用

**RocketMQ 事务消息（订单关单）**：
1. 发送 Half Message → Broker 存储但不可见
2. 执行本地事务（创建订单 + 本地消息表） → commit/rollback
3. Broker 回查（15 级递增延时）→ 检查本地事务状态
4. Consumer 消费（库存扣减/积分发放/通知推送）

### 实践任务
1. 追踪一个下单请求：Gateway → Order → RocketMQ → Inventory，画出消息时序图
2. 模拟 Half Message 超时未 commit，观察 Broker 回查机制

---

## Week 17：my-xhs-inventory 库存模块（核心亮点）

### 精读目标
深入理解分桶预扣减的完整实现——这是整个 my-xhs 最亮眼的设计。

### 必读文件

```
my-xhs-inventory/src/main/java/com/myxhs/inventory/
├── controller/InventoryController.java              // 库存 API
├── service/
│   ├── InventoryService.java                        // 核心扣减逻辑
│   └── InventoryBucketService.java                  // 分桶管理
├── dao/InventoryDao.java                            // 数据访问
└── resources/
    └── lua/
        ├── prededuct.lua                            // 预扣减 Lua
        ├── confirm.lua                              // 确认扣减 Lua
        └── release.lua                              // 释放库存 Lua
```

### 阅读要点

**分桶预扣减算法（三级保证）**：

L1 - Redis 预扣（毫秒级）：
```
钥匙 = {skuId}:bucket:{bucketIndex}
Lua 原子操作：检查库存 → 扣减 → 写流水 → 返回结果
热点分配：userId % bucketCount → 固定桶，分散单 Key 压力
```

L2 - MQ 异步扣 DB（秒级）：
```
预扣成功 → 发送 ConfirmMessage → Consumer 消费 → UPDATE stock SET stock = stock - quantity WHERE sku_id = ? AND stock >= quantity
```

L3 - 定时对账（分钟级）：
```
定时任务：SUM Redis 各桶余额 → 对比 DB → 不一致则修复
```

**动态扩容**：
- 热点检测：单桶 QPS > 阈值（如 5000）
- 扩容算法：2桶 → 4桶 → 8桶 → 16桶（2 的幂次，方便取模）
- 扩容期间：暂停预扣，请求改走 MQ（5 秒内恢复）
- 迁移策略：`bucketIndex = userId % newBucketCount`，部分 user 归属到旧桶

### 实践任务
1. 画出库存分桶预扣减的完整时序图（包含三级保证 + 动态扩容）
2. 分析扩容期间暂停预扣的时间窗口：MQ 重试能否保证不丢请求？
3. 写一个单元测试验证分桶扩容前后 `userId` 路由的迁移正确性

### 输出
- 深度博客：《库存分桶预扣减：从原理到 10,000 QPS 的完整设计方案》
- 架构图：《my-xhs 库存三级保证架构图》

---

## Week 18：my-xhs-order + my-xhs-payment

### 精读目标
理解订单的 5 步保障机制和 Event Sourcing 事件表设计。

### 必读文件

```
my-xhs-order/src/main/java/com/myxhs/order/
├── service/
│   ├── OrderService.java                            // 核心下单逻辑
│   └── OrderTransactionService.java                 // 独立事务管理
├── model/
│   ├── Order.java                                   // 订单实体
│   └── OrderEvent.java                              // 事件实体
└── state/
    └── OrderStateMachine.java                       // 状态机

my-xhs-payment/src/main/java/com/myxhs/payment/
├── service/PaymentService.java                      // 支付/退款/回调/对账
```

### 阅读要点

**5 步保障机制**：
1. **幂等下单**：bizIdentifier + Redis SET NX（24h 过期）
2. **分布式锁**：同用户 10s 内只能下 1 单（`@DistributedLock(key = "order:user:#{userId}")`）
3. **本地事务**：订单表 + 事件表 + 消息表，`@Transactional` 原子写入
4. **超时关单**：RocketMQ 延时消息（Level 16 = 30min）+ XXL-Job 兜底扫描
5. **状态机**：乐观锁 `UPDATE ... SET status = ? WHERE id = ? AND status = ?`

**Event Sourcing 事件表**：
- `t_order_event`：记录订单的每一次状态变更
- 事件类型：CREATED / PAID / SHIPPED / CONFIRMED / CANCELED / REFUNDED / TIMEOUT
- 用途：对账/审计/补偿/回放

**支付策略模式**：
- `PaymentStrategy` 接口：pay/refund/callback/reconcile
- Mock 实现（本地模拟）+ Remote 实现（真实支付）
- 工厂模式：`PaymentStrategyFactory.get(paymentMethod)`

### 实践任务
1. 画出订单状态机的完整状态流转图（7 种状态 + 触发条件）
2. 模拟并发下单：2 个请求同时用同一 userId，验证分布式锁
3. 画 Event Sourcing 的事件流回放图

### 输出
- 博客：《订单五重保障：my-xhs 如何做到不丢单、不重单》

---

## Week 19：my-xhs-search + my-xhs-home

### 精读目标
理解搜索服务（ES）和 BFF 聚合层（Home Service）的实现。

### 必读文件

```
my-xhs-search/src/main/java/com/myxhs/search/
├── service/
│   ├── SearchService.java                           // ES 搜索
│   ├── SuggestService.java                          // 搜索建议
│   └── HotSearchService.java                        // 热搜排行榜
└── sync/
    └── CanalSyncService.java                        // Canal 数据同步

my-xhs-home/src/main/java/com/myxhs/home/
├── service/
│   ├── FeedService.java                             // Feed 流聚合
│   ├── NoteDetailService.java                       // 笔记详情聚合
│   └── HomeAggregationService.java                  // 首页聚合
```

### 阅读要点

**ES 搜索流程**：
- 倒排索引 + TF-IDF/BM25 相关性评分
- Search After 深分页（避免 from+size 的 10000 限制）
- Completion Suggester：前缀匹配搜索建议
- 高亮 + 同义词 + 拼音搜索

**Canal 数据同步**：
```
MySQL Binlog →  Canal Server →  RocketMQ  →  ES Consumer  →  ES Index
                                        →  Redis Consumer →  Redis Cache
```

**Feed 流推拉结合**：
- 推模式：大 V 发布 → 写入粉丝收件箱（Redis List）
- 拉模式：普通用户发布 → 粉丝拉取时实时查询
- 混合阈值：粉丝数 > 10,000 用推，否则用拉

**BFF 双层并行聚合**：
```java
CompletableFuture<NoteDTO> noteFuture = CompletableFuture.supplyAsync(() -> contentService.getNote(noteId));
CompletableFuture<UserDTO> userFuture = noteFuture.thenCompose(note -> userService.getUser(note.getUserId()));
CompletableFuture<CounterDTO> counterFuture = noteFuture.thenCompose(note -> counterService.getCounts(noteId));
// 聚合结果
CompletableFuture.allOf(userFuture, counterFuture).join();
```

### 实践任务
1. 画出 Search After 深分页的 ES 请求流程
2. 画出 Canal 同步的完整数据流图（Binlog → Canal → MQ → ES/Redis）
3. 分析 BFF 聚合的超时策略：如果下游 3 个服务中有 1 个超时，当前逻辑如何处理？

---

## Week 20：Phase 3 交付周

### 核心交付物

#### 1. my-xhs 完整架构图
必须包含：
- 15 个微服务的拓扑关系图
- 每个微服务的核心职责和关键类
- 数据流向（读路径 vs 写路径）
- 中间件依赖关系（Nacos/Gateway/Redis/RocketMQ/ES/Canal）

#### 2. 核心数据流图 × 3
- **下单全链路**：Gateway → Order → RocketMQ → Inventory/Payment/Coupon
- **Feed 流加载**：Home → Content/Counter/Analytics → 并行聚合
- **搜索全链路**：Search→ES + Canal→Binlog→MQ→ES 增量同步

#### 3. 深度博客：《my-xhs 架构全景分析》

建议大纲（~5000 字）：
1. 项目背景：为什么要设计这样一个系统
2. 核心架构决策：为什么选 Alibaba Cloud 全家桶
3. 最亮眼的 3 个设计：
   - 库存分桶预扣减
   - AOP 三剑客（@RateLimit + @DistributedLock + @Idempotent）
   - 全链路流量染色
4. 可以改进的 3 个点：
   - SkyWalking → OpenTelemetry
   - Java 17 → 21 + Virtual Threads
   - 缺少 Service Mesh
5. 架构演进方向

---

## Phase 3 检验标准

- [ ] 能画出 my-xhs 全部 15 个微服务的架构拓扑图
- [ ] 能追踪一个下单请求从 Gateway → Order → MQ → Inventory 的完整路径
- [ ] 能解释库存分桶预扣减的三级保证和动态扩容算法
- [ ] 能解释 AOP 三剑客的 Order 排序原因（10→50→100）
- [ ] 能解释 Feign + MQ 如何透传 TraceContext
- [ ] 能画出 Canal 数据同步的完整链路
