# Phase 7：技术影响力与输出（第 47-52 周）

## 目标
从"技术能手"到"技术专家"——建立行业影响力。

---

## Week 47：开源贡献

### 项目选择
| 项目 | 语言 | 适合初学者？ | 与 my-xhs 关联 |
|------|------|-------------|---------------|
| Sentinel | Java | ✅ good first issue 多 | 限流熔断 |
| Seata | Java | ⚠️ 分布式事务复杂 | 分布式事务 |
| Nacos | Java | ✅ 社区活跃 | 注册/配置中心 |
| OpenTelemetry Java | Java | ✅ 文档完善 | 可观测性 |

### 标准 PR 流程
1. Fork 项目 → Clone 到本地
2. 读 `CONTRIBUTING.md`（编码规范/测试要求/PR 模板）
3. 找 `good first issue` 标签
4. 创建分支 `fix/xxx` → 编码 → 测试 → Commit
5. Push → 创建 PR → 等 Review → 修改 → Merge
6. 庆祝 🎉

### PR 升级路径（5 个 PR）
| PR # | 类型 | 难度 | 预计耗时 | 举例 |
|------|------|------|---------|------|
| PR 1 | Typo/Doc Fix | ⭐ | 2h | 修复 README 中的变量名拼写错误 |
| PR 2 | 单测补充 | ⭐⭐ | 4h | 为某个工具类补充边界 case 测试 |
| PR 3 | Bug Fix | ⭐⭐⭐ | 8h | 修复空指针/并发问题 |
| PR 4 | 小 Feature | ⭐⭐⭐⭐ | 16h | 新增一个配置项/参数 |
| PR 5 | 性能优化 | ⭐⭐⭐⭐⭐ | 24h | 用 JMH 证明优化有效 |

### 实践任务
- 目标：5 个 PR 被合并

---

## Week 48：技术写作

### 5 篇深度博客大纲

**文章 1：《从 SkyWalking 到 OpenTelemetry：my-xhs 可观测性升级之路》**
- Part 1：为什么换？SkyWalking 的 3 个痛点
- Part 2：OTel 架构（Agent + Collector + Exporter）
- Part 3：Grafana LGTM 栈搭建实录
- Part 4：Before/After 对比（资源开销、链路完整性、查询体验）

**文章 2：《Java 21 Virtual Threads 实战：从线程池到虚拟线程的跃升》**
- Part 1：传统线程池的 3 大瓶颈（内存、切换开销、数量限制）
- Part 2：Virtual Threads 原理（Continuation、Carrier Thread）
- Part 3：my-xhs 改造实录（Tomcat/Async/Feign 全切换）
- Part 4：10,000 并发压测对比（平台线程 OOM vs 虚拟线程 P99 1.2s）

**文章 3：《库存分桶预扣减：从原理到 10,000 QPS》**
- Part 1：问题定义（为什么单 Key 是瓶颈）
- Part 2：分桶算法（取模路由 + Lua 原子 + 动态扩容）
- Part 3：三级保证（Redis → MQ → 对账）
- Part 4：压测数据 + 边界处理 + 扩容窗口

**文章 4：《AOP 三剑客：@RateLimit + @DistributedLock + @Idempotent》**
- Part 1：为什么是三个注解而不是一个？
- Part 2：执行顺序（10→50→100）的设计哲学
- Part 3：每个注解的实现细节（Lua 脚本 / Watchdog / SpEL）
- Part 4：降级策略 + Redis 宕机时怎么办

**文章 5：《迁移清单：Java 17 → 21 升级的 20 个坑》**
- 18 个 pom.xml 的修改清单
- javax → jakarta 迁移脚本（含漏网之鱼检查）
- Spring Boot 3.3 的 5 个 Breaking Changes
- Virtual Threads 的 3 个已知限制

### 发布策略
- 平台：掘金（首发）+ CSDN + 个人博客 + 公众号
- 时机：每周一篇，周一早上 8:00 发布
- 推广：转 LinkedIn + Reddit r/java + V2EX

### 每篇标准结构
```
背景（为什么写）→ 方案对比（有哪些选择）→ 原理分析（深入代码）→ 实践步骤（跟着做）→ 踩坑记录（我遇到的）→ 性能数据（Before/After）→ 总结
```

---

## Week 49：技术演讲

### 演讲大纲（30 分钟）
《my-xhs 从 100 到 10,000 QPS 的演进之路》

**Part 1：项目起点（5min）**
- my-xhs 是什么？一个 15 微服务的电商项目
- 最开始的性能基线：45 QPS 下单 / 180 QPS Feed

**Part 2：优化之路（15min）**
- 第 1 层：连接池/线程池 → +30%
- 第 2 层：SQL 优化 + 分库分表 → +200%
- 第 3 层：多级缓存 + 库存分桶 → +500%
- 第 4 层：Virtual Threads + 异步化 → +∞（不再 OOM）

**Part 3：关键决策的 WHY（5min）**
- 为什么库存用分桶而不是悲观锁？
- 为什么从 SkyWalking 换到 OTel？
- 为什么 Java 21 而不是 17？

**Part 4：踩坑与教训（3min）**
- 缓存一致性边界 case × 3
- 分库跨库关联查询的性能陷阱

**Part 5：展望（2min）**

### PPT 制作
- 25 页（每页 1-3 个要点，不用满篇文字）
- 工具：Keynote / reveal.js / Slidev
- 架构图、压测截图、代码片段（关键行高亮）

### Live Demo（演讲中）
- Docker Compose 一键启动 my-xhs
- JMeter 实时压测，Grafana 大屏展示 QPS/RT/错误率

---

## Week 50：面试突击

### 系统设计（50 题分类）
| 类别 | 题目数 | 示例 |
|------|--------|------|
| 短链/URL | 5 | TinyURL、Pastebin |
| 社交 | 8 | Twitter Timeline、Instagram、微信朋友圈 |
| 电商 | 8 | 秒杀、购物车、订单、支付 |
| 消息/通讯 | 5 | 微信/钉钉消息、WhatsApp |
| 搜索/推荐 | 5 | 搜索引擎、Feed 推荐 |
| 存储 | 8 | 分布式文件系统、KV 存储、时序数据库 |
| 基础组件 | 6 | 限流器、ID 生成器、分布式锁、配置中心 |
| 视频/直播 | 5 | YouTube、直播弹幕 |

### Java 深度题（精选 30 题）
1. HashMap 1.7 vs 1.8 的头插/尾插和扩容区别？
2. ConcurrentHashMap 如何实现线程安全的扩容？
3. synchronized 锁膨胀的完整过程（无锁→偏向→轻量→重量）？
4. AQS 的 CLH 队列和 Condition 队列的区别？
5. ThreadPoolExecutor 的 ctl 变量设计（为什么用位运算）？
6. Virtual Threads 为什么不适用于 CPU 密集型？
7. CMS vs G1 vs ZGC 的选型依据？
8. Spring Bean 的 13 个生命周期节点？
9. @Transactional 失效的 8 种场景？
10. MyBatis 一二级缓存的失效场景？

### my-xhs 项目深挖（10 问）
- "为什么库存分桶用 userId 取模而不是 random？"
- "分布式锁的 Watchdog 为什么是 30s？"
- "AOP 三剑客的 Order(10/50/100) 为什么是这样？"

### 行为面试（STAR 法则）
- 最具挑战的项目（Situation → Task → Action → Result）
- 与同事的技术分歧如何解决
- 如何推动一个技术决策落地

---

## Week 51：模拟面试 + 复盘

### 3 轮模拟面试
| 轮次 | 形式 | 时长 | 重点 |
|------|------|------|------|
| 第 1 轮 | 系统设计 | 45min | 1 道设计题（TinyURL 或秒杀） |
| 第 2 轮 | 项目深挖 | 45min | my-xhs 库存/订单/升级的 WHY |
| 第 3 轮 | 综合（算法 + 基础 + BQ） | 60min | LeetCode Medium + JVM/并发 + STAR |

### 自评表
| 能力维度 | 评分(1-10) | 证据 |
|---------|-----------|------|
| JVM 调优 | | GC 日志分析经验 |
| 并发编程 | | MiniAQS + Virtual Threads 实战 |
| 分布式设计 | | my-xhs 库存分桶 + 分布式事务 |
| 性能优化 | | 100→10,000 QPS 压测报告 |
| 架构设计 | | 多活架构设计 + 50 题系统设计 |
| 技术写作 | | 5 篇深度博客（掘金阅读量） |
| 开源贡献 | | 5 个 PR 被合并 |

### 10 个核心知识体系图
1. JVM 内存 + GC 全景图
2. Java 并发体系（从 AQS 到 Virtual Threads）
3. Spring 架构全景图
4. MySQL 索引 + MVCC + 锁全景图
5. Redis 数据结构 + 持久化 + 集群全景图
6. 分布式系统理论（CAP/BASE/一致性）
7. 微服务架构全景图
8. 消息队列全景图（Kafka vs RocketMQ）
9. 可观测性全景图（OTel + Prometheus + Grafana）
10. my-xhs 架构全景图

---

## Week 52：收尾与展望

### my-xhs 开源发布

#### README.md 清单
```markdown
# my-xhs（你重命名的版本）
## 简介
## 架构概览（一张大图）
## 技术栈
## 快速开始（Docker Compose 一键启动）
## 核心亮点（库存分桶 / AOP 三剑客 / 流量染色 / 多活）
## API 文档（Swagger 链接）
## 压测报告
## 架构设计文档
## 如何贡献
## License
```

#### 目录结构规范
```
my-xhs/
├── README.md
├── ARCHITECTURE.md    # 架构设计文档
├── BENCHMARK.md       # 压测报告
├── UPGRADE.md         # Java 17→21 升级指南
├── CHAOS.md           # 混沌工程报告
├── docker-compose.yml
├── k8s/
├── docs/
│   ├── api/           # API 文档
│   ├── ops/           # 运维手册
│   └── design/        # 设计决策记录
└── services/          # 各微服务
```

### 《Java 技术专家成长手册》
作为整个 52 周的方法论沉淀：
1. 学习金字塔（被动学习 10% → 主动学习 90%）
2. 项目驱动的力量（为什么 my-xhs 贯穿全程）
3. 输出是最好的输入（每周一篇博客的效果）
4. 资源清单（书/论文/源码/工具/社区）

### 下一阶段目标
- 技术方向：深入 AI/LLM（Spring AI）/ 分布式数据库 / 多语言运行时（WASM）
- 管理方向：技术团队 TL / 架构组 Leader
- 创业方向：技术合伙 / 独立开发者

---

## Phase 7 检验标准
- [ ] 5 个开源 PR 被合并
- [ ] 5 篇深度博客发布（每篇 3000+ 字）
- [ ] 30 分钟技术演讲录制完成
- [ ] 50 道系统设计题准备好答案框架
- [ ] 3 轮模拟面试完成
- [ ] 10 张核心知识体系图完成
- [ ] my-xhs 开源版发布
- [ ] 《Java 技术专家成长手册》完成
