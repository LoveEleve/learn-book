# 资源地图

## 必读书单（按阶段）

| 阶段 | 书名 | 版本 | 重点章节 | 阅读方式 |
|------|------|------|---------|---------|
| Phase 1 | 《深入理解 Java 虚拟机》周志明 | 第 3 版 | 第 2/3/4/5/11 章 | 精读 2 遍，对照源码 |
| Phase 1 | 《Java 并发编程的艺术》方腾飞 | 第 1 版 | 第 2/3/4/5/6/7 章 | 精读 + AQS 源码对照 |
| Phase 2 | 《Spring 技术内幕》计文柯 | 第 2 版 | 第 2/3/6/8 章 | 对照 Spring Framework 源码 |
| Phase 2 | 《高性能 MySQL》 | 第 4 版 | 第 1-7/11 章 | 重点读索引和优化 |
| Phase 2 | 《Redis 设计与实现》黄健宏 | - | 第 2/3/4/7/9/10/17/18 章 | 通读 + 源码验证 |
| Phase 3 | 《数据密集型应用系统设计》(DDIA) | - | 第 5/6/7/8/9/10/11 章 | 精读，分布式圣经 |
| Phase 3 | 《Raft 论文》 | - | Section 5 + Figure 2 | 逐段精读 + 代码实现 |
| Phase 4 | 《企业集成模式》 | - | 按需查阅 | 参考书 |
| Phase 5 | 《系统设计面试》(Alex Xu) | - | 第 1-12 章 | 每章做一道完整设计 |
| Phase 5 | 《Kubernetes 权威指南》龚正 | 第 5 版 | 第 1/2/3/4/8/10 章 | 重点读核心概念 |
| Phase 6 | 《SRE：Google 运维解密》 | - | 第 1/2/3/6/11/12/13 章 | 理念学习 |
| Phase 7 | 《重构》Martin Fowler | 第 2 版 | 第 1/3/4/6/7 章 | 查缺补漏 |

---

## 必读论文（14 篇）

| # | 论文 | 年份 | 关联周 | 阅读时间 |
|---|------|------|--------|---------|
| 1 | GFS - The Google File System | 2003 | W13 | 2h |
| 2 | MapReduce | 2004 | W13 | 2h |
| 3 | Bigtable | 2006 | W13 | 2h |
| 4 | Dynamo | 2007 | W13 | 2h |
| 5 | The Chubby Lock Service | 2006 | W13 | 1h |
| 6 | Paxos Made Simple | 2001 | W13 | 2h |
| 7 | Raft | 2014 | W13 | 3h（核心，实现） |
| 8 | ZAB | 2011 | W13 | 1h |
| 9 | Spanner | 2012 | W35 | 2h |
| 10 | Kafka: a Distributed Messaging System | 2011 | W17 | 1h |
| 11 | Dapper | 2010 | W29 | 1h |
| 12 | Amazon Aurora | 2017 | W37 | 2h |
| 13 | FoundationDB | 2021 | W37 | 1h |
| 14 | Deep Dive into ZGC | 2020 | W2 | 2h |

### 论文获取渠道
- Google Scholar 直接搜索论文标题
- 论文阅读顺序：Abstract → Introduction → Key Design → Evaluation → Conclusion
- 不需要完全读懂，理解核心思想和 trade-off 即可

---

## 必看源码（17 个项目）

| 优先级 | 项目 | 版本 | 重点模块 | 关联周 |
|--------|------|------|---------|--------|
| P0 | OpenJDK | 21 | `java.util.HashMap` / `ConcurrentHashMap` / `ArrayList` | W5 |
| P0 | OpenJDK | 21 | `java.util.concurrent.locks.AQS` / `ReentrantLock` / `ThreadPoolExecutor` | W3-W4 |
| P0 | Spring Framework | 6.1.x | `beans/factory` / `aop` / `transaction` | W7-W8 |
| P0 | MyBatis | 3.5.x | `executor` / `plugin` / `cache` | W11 |
| P1 | Netty | 4.1.x | `channel` / `buffer` / `handler` | W6 |
| P1 | Sentinel | 1.8.x | `core` / `slot` 责任链 | W30 |
| P1 | Nacos | 2.3.x | `naming` / `config` / `core` Raft | W14 |
| P1 | RocketMQ | 5.1.x | `store` / `broker/transaction` / `dledger` | W18 |
| P1 | Seata | 1.8.x | `rm` / `tm` / `tc` | W28 |
| P2 | ShardingSphere | 5.5.x | `sharding` / `readwrite-splitting` | W24 |
| P2 | Canal | 1.1.x | `parse` / `sink` | W19/W42 |
| P2 | Redisson | 3.27.x | `lock` / `RedissonLock` | W15 |
| P2 | CosId | 2.6.x | `segment` / `snowflake` | W34 |
| P2 | OpenTelemetry Java | 1.x | `agent` / `sdk` / `api` | W41 |
| P3 | Apache Dubbo | 3.2.x | `registry` / `protocol` / `filter` | W16 |
| P3 | GraalVM | 21.x | `native-image` | W38 |
| P3 | ChaosBlade | 1.x | `exec-jvm` / `exec-os` | W44 |

---

## 中间件版本清单

| 中间件 | my-xhs 当前 | 推荐 | 说明 |
|--------|------------|------|------|
| Java | 17 | **21 LTS** | Virtual Threads + Pattern Matching |
| Spring Boot | 3.2.5 | 3.3+ | Jakarta EE + AOT |
| Nacos | 2.3.0 | 2.3.x | 稳定 |
| Sentinel | 1.8.8 | 1.8.x | 稳定 |
| RocketMQ | 5.1.4 | 5.1.x | 稳定 |
| Seata | - | 1.8.x | 建议升级 |
| ShardingSphere | 5.5.1 | 5.5.x | 稳定 |
| Redisson | 3.27.0 | 3.27.x | 稳定 |
| Elasticsearch | 8.12.2 | 8.12.x | 稳定 |
| MySQL | 8.0 | 8.0 | 8.4 LTS 可选 |
| Canal | 1.1.x | 1.1.x | 稳定 |
| SkyWalking | 9.7.0 | 9.x（探索后迁移到 OTel） | OTel 以后替代 |
| OpenTelemetry | - | **1.40+** | 新增，替代 SkyWalking |
| CosId | 2.6.8 | 2.6.x | 稳定 |

---

## 必会工具

### JVM 诊断工具（10+）
| 工具 | 用途 | 掌握程度 | 关联周 |
|------|------|---------|--------|
| jps | 列出 Java 进程 | 熟练 | W1 |
| jmap | 堆内存 dump + histogram | 熟练 | W1 |
| jstack | 线程 dump 分析 | 熟练 | W1 |
| jstat | GC 统计 | 熟练 | W2 |
| jcmd | NMT 分析 | 掌握 | W1 |
| MAT | 堆 dump 分析 | 掌握 | W2 |
| Arthas | 在线诊断 | 掌握 | W21 |
| Async Profiler | 火焰图 | 掌握 | W21 |
| JFR + JMC | 飞行记录 | 了解 | W2 |
| JMH | 微基准测试 | 熟练 | W4 |

### 压测工具（6）
| 工具 | 用途 | 关联周 |
|------|------|--------|
| JMeter | GUI 压测场景编排 | W21 |
| Wrk | CLI 高并发压测 | W21 |
| Vegeta | 恒压/阶梯压测 | W21 |
| ab | HTTP 简单压测 | W21 |

### 效率工具
| 工具 | 用途 |
|------|------|
| Docker Desktop / Orbstack | 本地容器环境 |
| Postman / Insomnia | API 测试 |
| DataGrip | 数据库管理 |
| RedisInsight | Redis 可视化管理 |
| Kafka Tool / RocketMQ Console | MQ 可视化管理 |
| Kibana / Grafana | 日志/监控可视化 |
| PlantUML / Mermaid | 架构图绘制 |
| VSCode + Copilot | AI 辅助编码 |

### IDE 插件（IntelliJ IDEA）
- **MyBatisX**：Mapper ↔ XML 跳转
- **Alibaba Java Coding Guidelines**：代码规范检查
- **SonarLint**：静态代码分析
- **Maven Helper**：依赖冲突分析
- **RestfulTool**：REST API 导航
- **Statistic**：代码统计
- **String Manipulation**：字符串处理
- **Grep Console**：日志高亮

---

## 推荐技术博客与社区

### 中文（10+）
1. **美团技术博客** — 分布式/高并发实战
2. **字节跳动技术博客** — 大厂架构实践
3. **阿里中间件团队博客** — Nacos/Sentinel/RocketMQ/Seata 源码解读
4. **有赞技术博客** — 电商架构实践
5. **滴滴技术博客** — 稳定性/混沌工程
6. **Ricky 的技术小站** — Java 并发深度文章
7. **JavaGuide**（Snailclimb） — Java 体系总结
8. **Doocs 技术社区** — 源码阅读笔记
9. **程序猿 DD**（SpringForAll） — Spring 技术栈
10. **MacroZheng** — 电商项目实战
11. **江南一点雨** — Spring Boot 教程
12. **廖雪峰的官方网站** — Java 教程
13. **PDai 技术博客** — Java 全栈知识体系

### 英文（5+）
1. **Baeldung** — Java/Spring 教程大全
2. **Reflectoring** — Spring 架构设计模式
3. **Martin Fowler's Blog** — 架构大师
4. **InfoQ** — 技术前沿
5. **Reddit r/java** — Java 社区讨论

---

## 每周学习日志模板

```markdown
# Week X 学习日志

## 本周目标
- [ ] 目标 1
- [ ] 目标 2
- [ ] 目标 3

## 周一（日期）
### 理论
- 做了什么：
- 理解了什么：
- 不理解的点：

### 实践
- 做了什么任务：
- 代码仓库链接：
- 遇到了什么问题：

## 周二（日期）
...

## 周末总结
### 本周达成
- [x] ...
- [ ] ...（未达成，原因：）

### 踩坑记录
1. 坑：...
   根因：...
   解决：...

### 下周计划
1. ...
2. ...

### 本周 GitHub 提交
| 日期 | 仓库 | Commit | 内容 |
|------|------|--------|------|
| Mon | my-xhs | abc123 | 优化xxx |
```

---

## FAQ

**Q: 跟不上计划怎么办？**
A: 检查原因——时间不够（减少理论，增加实践）还是难度过高（先跳过看下一遍再来）。不要追求 100% 掌握，60% 理解 + 40% 实践 > 100% 理解不实践。

**Q: 可以跳过某个 Phase 吗？**
A: Phase 1（JVM 内核）和 Phase 2（Spring 内功）是地基，不能跳过。其他 Phase 可以根据实际情况调整节奏。

**Q: 需要先看完整本书再实践吗？**
A: 不要。读一章，写一段代码，输出一篇笔记。三环循环才是最高效的学习方式。
