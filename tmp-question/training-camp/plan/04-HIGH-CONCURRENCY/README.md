# Phase 4：高并发改造实战（第 21-32 周）

## 目标
对 my-xhs 做全链路压测，从 100 QPS 优化到 10,000 QPS。每一周都是真实的压测→分析→优化→验证循环。

---

## Week 21：性能压测方法论与基线建立

### 压测环境搭建
- 工具：JMeter（GUI 编排场景） + Wrk（CLI 快速压测）
- 硬件：与生产环境一致的规格（CPU/内存/网络）
- 数据：预置 100 万用户 + 100 万商品 + 100 万订单

### 压测场景设计
| 场景 | 接口 | 并发 | 持续时间 | 目的 |
|------|------|------|---------|------|
| 场景 1 | GET /home/feed | 50→100→200→500 | 每档 5min | Feed 流基线 |
| 场景 2 | POST /order/create | 50→100→200 | 每档 5min | 下单基线 |
| 场景 3 | GET /product/detail/{id} | 500→1000→2000 | 每档 5min | 商品详情（读多写少） |
| 场景 4 | GET /search?q=xxx | 200→500→1000 | 每档 5min | 搜索基线 |
| 场景 5 | 混合压测（60%读+40%写） | 200→500→1000 | 30min | 全链路容量 |

### 指标采集
- **应用层**：QPS、RT（Avg/P50/P90/P99）、错误率
- **中间件层**：MySQL QPS/慢查询、Redis OPS/命中率、RocketMQ TPS
- **系统层**：CPU Usage、Memory Usage、Network IO、Disk IO

### 实践任务
1. 编写 5 个 JMeter 场景 + 2 个 Wrk 脚本
2. 全链路压测，建立性能基线
3. 输出《my-xhs V1 性能基线报告》（含 Top 5 瓶颈点）

### 输出
- 基线报告表格：
```
| 场景 | QPS | P99 RT | 错误率 | CPU | 瓶颈 |
|------|-----|--------|--------|-----|------|
| Feed | 180 | 520ms  | 0.1%   | 75% | home-service DB 查询慢 |
| 下单 | 45  | 890ms  | 0.5%   | 60% | order-service 锁等待 |
```

---

## Week 22：连接池与线程池调优

### 目标：QPS 提升 30-40%

### Tomcat 线程池调优
```yaml
# 调优前（默认）
server.tomcat.threads.max: 200
server.tomcat.accept-count: 100
server.tomcat.max-connections: 8192

# 调优后
server.tomcat.threads.max: 400       # 根据 CPU 核数 * 2 + 磁盘 IO 调整
server.tomcat.accept-count: 500      # 排队队列
server.tomcat.max-connections: 10000 # 最大连接数
server.tomcat.connection-timeout: 5000ms
```

### HikariCP 连接池调优
```yaml
# 调优前
spring.datasource.hikari.maximum-pool-size: 10
spring.datasource.hikari.minimum-idle: 10

# 调优后
spring.datasource.hikari.maximum-pool-size: 50  # 公式：((core_count * 2) + effective_spindle_count)
spring.datasource.hikari.minimum-idle: 10
spring.datasource.hikari.connection-timeout: 3000
spring.datasource.hikari.idle-timeout: 600000
spring.datasource.hikari.max-lifetime: 1800000
```

### Virtual Threads 试用（预告）
```yaml
# Tomcat 使用虚拟线程（Java 21+）
spring.threads.virtual.enabled: true
```

### 实践任务
1. 调优 Tomcat + HikariCP 参数，压测对比 Before/After
2. 调整 Virtual Threads 开关，压测对比（如果已升级 Java 21）
3. 输出优化报告

---

## Week 23-24：数据库优化 + 分库分表验证

### 慢 SQL 治理
对 my-xhs 全部 SQL 执行 `EXPLAIN`，识别问题：
- `type=ALL`（全表扫描）→ 加索引
- `type=index`（全索引扫描）→ 优化 WHERE 条件
- `Extra=Using filesort` → 优化 ORDER BY / 调整索引顺序
- `Extra=Using temporary` → 优化 GROUP BY / 避免临时表

### 分库分表验证
my-xhs 使用 ShardingSphere-JDBC 做分库分表：
```
订单表：4 库 × 4 表 = 16 逻辑表
分片键：user_id
库路由：user_id % 4
表路由：(user_id / 4) % 4
```

### 实践任务
1. 用 EXPLAIN 分析所有订单相关 SQL，记录 Before/After
2. 压测验证分库效果：单库 vs 4 库的 TPS 对比
3. 测试跨库关联查询的性能影响
4. 验证分片键选择是否合理（是否存在数据倾斜）

### 目标
- 慢 SQL 数量减少 80%
- 核心 SQL P99 降到 50ms 以下

---

## Week 25：缓存体系优化

### my-xhs 现有缓存走读
- **双 Redis 数据源**：业务 Redis（noeviction）+ 缓存 Redis（allkeys-lru）
- **Caffeine 本地缓存**：product-service 三级分类树
- **缓存一致性方案**：Canal 订阅 Binlog → MQ → 更新 Redis

### 缓存一致性边界 case 检验
| 场景 | 期望 | 当前是否满足 | 修复方案 |
|------|------|-------------|---------|
| 更新 DB 成功 + 更新 Redis 失败 | 缓存不过期，下次读到旧数据 | 检查 | 延迟双删 + MQ 重试 |
| 更新 DB 前 Redis 过期 | 读到旧数据后更新 | 检查 | 先更新 DB 再删缓存 |
| 并发更新同一 Key | 最终一致 | 检查 | 分布式锁保护 |
| 缓存雪崩 | 部分 Key 同时过期 | 检查 | TTL 打散 + 永不过期 + 异步更新 |

### 实践任务
1. 模拟 4 个边界 case，验证 my-xhs 的一致性方案
2. 压测对比：无缓存 vs 单 Redis vs Caffeine+Redis 的 P99
3. 优化：Caffeine 缓存大小调优

### 目标
- 缓存命中率 ≥ 90%
- 缓存一致性方案通过全部 4 个边界 case

---

## Week 26：库存系统专项压测

### 压测场景
| 场景 | 描述 | 并发 | 预期 QPS | 验证点 |
|------|------|------|---------|--------|
| 单 SKU 扣减 | 1000 用户抢 1 个热门商品 | 2000 | 10,000 | 分桶效果 + Lua 原子性 |
| 混合 SKU | 100 个 SKU 随机扣减 | 2000 | 15,000 | Pipeline 批量 |
| 热点扩容 | 单 SKU 从 2 桶扩到 8 桶 | 2000 | 10,000 | 扩容期间降级 |
| 超卖验证 | 库存 100，并发 500 | 500 | - | 零超卖 |

### 实践任务
1. 4 个场景专项压测，记录 P99/P999/错误率
2. 验证：扩容期间暂停预扣的时间窗口是否 ≤ 5 秒
3. 验证：超卖场景下库存余额始终 ≥ 0

### 目标
- 单 SKU 扣减 ≥ 10,000 QPS
- 超卖 = 0
- 扩容降级窗口 ≤ 5 秒

---

## Week 27：订单系统边界 case 验证

### 边界 case 矩阵
| # | 场景 | 验证点 |
|---|------|--------|
| 1 | 同一用户 1 秒内发 10 个下单请求 | 分布式锁只允许 1 个通过 |
| 2 | 同一 bizIdentifier 重放 3 次 | 幂等控制：1 次成功 + 2 次返回已有订单号 |
| 3 | 下单后 MQ 延迟消息未送达 | XXL-Job 兜底扫描在 T+1min 关闭超时订单 |
| 4 | 支付回调 vs 关单的竞态 | 乐观锁 WHERE status=WAIT_PAY 防覆盖 |
| 5 | 下单成功但库存扣减 MQ 消费失败 | MQ 重试 3 次 → 死信队列 → 人工处理 |

### 实践任务
1. 编写 5 个自动化测试覆盖上述边界 case
2. 全链路压测下单场景：从 45 QPS 优化到 ≥ 500 QPS
3. 检查订单状态机的全部状态流转路径是否都有超时/异常处理

---

## Week 28：分布式事务实战

### 三种方案压测对比
| 方案 | 原理 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|---------|
| Seata AT | 全局锁 + undo_log | 无侵入 | 性能损失 20-30% | 强一致需求 |
| Seata TCC | Try/Confirm/Cancel | 性能好 | 侵入性强 | 性能敏感 |
| RocketMQ 事务消息 | Half Message + 回查 | 异步解耦 | 最终一致 | 允许短暂不一致 |

### 实践任务
1. 用三种方案分别实现 my-xhs 的下单+扣库存场景
2. 压测对比 TPS/P99
3. 混沌测试：模拟 TC 宕机、RM 宕机、网络分区，验证各方案的恢复能力

---

## Week 29-30：可观测性升级 + 故障演练

### SkyWalking → OpenTelemetry 迁移
1. 引入 OTel Java Agent（`opentelemetry-javaagent.jar`）
2. 配置 OTLP Exporter → Grafana Tempo（Trace） + Mimir（Metrics） + Loki（Log）
3. 压测对比 SkyWalking vs OTel 的资源开销
4. 在 `OrderService.createOrder` 添加手动 Span

### Sentinel 故障演练
| 演练场景 | 方式 | 预期行为 |
|---------|------|---------|
| 限流触发 | 2000 QPS 打到阈值 1000 的接口 | 返回 429 + 降级兜底 |
| 熔断触发 | 下游全部返回 500 | 第 N 次开启熔断，返回降级数据 |
| 热点参数 | 同一商品打到 5000 QPS | 热点限流返回 429 |
| 慢调用 | P99 > 1s | 慢调用比例熔断 |
| 异常比例 | 异常 > 50% | 异常比例熔断 |

---

## Week 31-32：高并发交付 + 面试准备

### 第 31 周：完整压测报告

**Before（Week 21 基线）vs After（Week 31 优化后）**：
| 指标 | Before | After | 提升 |
|------|--------|-------|------|
| Feed QPS | 180 | 3,200 | +1680% |
| 下单 QPS | 45 | 680 | +1410% |
| 商品详情 QPS | 2,100 | 18,000 | +757% |
| 搜索 QPS | 320 | 4,500 | +1306% |
| P99 RT | 890ms | 85ms | -90% |
| CPU | 75% | 55% | -27% |
| 慢 SQL | 23 条 | 3 条 | -87% |

### 第 32 周：面试准备

**10 个核心面试主题**：
1. 你们的 QPS 从 100 到 10,000 是怎么做到的？
2. 库存分桶预扣减的设计原理和扩容策略？
3. 缓存一致性的 4 个方案对比？Canal 方案有什么风险？
4. 分布式锁怎么实现的？Watchdog 机制？
5. 下单如何保证幂等？bizIdentifier 的唯一性怎么保证？
6. 分库分表后跨库查询怎么处理？
7. 分布式事务选 Seata AT 还是 TCC？为什么？
8. 你们怎么发现性能瓶颈的？用了什么工具？
9. Virtual Threads 改造后性能提升多少？有什么坑？
10. 如果让你重新设计 my-xhs，你会改哪些？

## Phase 4 检验标准
- [ ] my-xhs 核心接口总 QPS ≥ 10,000
- [ ] 核心接口 P99 ≤ 100ms
- [ ] 库存超卖 = 0
- [ ] 缓存命中率 ≥ 90%
- [ ] 慢 SQL ≤ 5 条
- [ ] 完整压测报告 + 3 个真实优化 PR
