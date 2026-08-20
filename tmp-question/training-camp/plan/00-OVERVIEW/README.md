# 从 Java 基础到技术专家 — 52 周训练计划总览

## 前置条件与技术栈基准

| 项 | 值 |
|---|-----|
| 起点 | 能用 Spring Boot 做 CRUD 业务开发 |
| 目标 | 全能技术专家（P7+ 架构师能力） |
| 时长 | 52 周（1 年），每天 4-6 小时 |
| Java | **21 LTS**（2023-09 发布，支持至 2031） |
| Spring Boot | **3.3+**（Jakarta EE 9，javax → jakarta） |
| Spring Cloud | 2023.0.x |
| Spring Cloud Alibaba | 2023.x |
| 实战项目 | **my-xhs**（/data/workspace/my-xhs，15 微服务电商） |

## 为什么这样设计？

### 为什么是 Java 21 而不是 8/11/17？
- Virtual Threads 改变了并发编程范式
- Pattern Matching / Record / Sealed Classes 提高代码质量
- 已发布近 3 年，生态成熟，大厂已落地

### 为什么基于 my-xhs 改造而不是从零造？
- my-xhs 已经是 15 微服务、51K 行 Java 的生产级代码
- 阅读高质量代码本身就是学习
- 面试时说"我在一个生产级项目上做了真实优化"比"我写了个 demo"有说服力

### 为什么 01-JAVA-CORE 从 JVM 开始而不是 Spring？
- Spring 是术，JVM 是道。理解 JVM 之后再看 Spring 源码，就像开了天眼
- 面试中 JVM 题的权重大于 Spring 使用题

## 52 周路线图

```
Phase 1 [W1-W6]   Java 内核：JVM内存 → GC → AQS/并发 → Virtual Threads → 集合 → Netty
Phase 2 [W7-W12]  Spring 内功：Spring源码 → Boot 3.3 → MySQL内核 → MyBatis → Redis源码
Phase 3 [W13-W20] my-xhs 深读：15 微服务逐类逐方法阅读 + 架构全景图
Phase 4 [W21-W32] 高并发改造：全链路压测 → 瓶颈定位 → 逐层优化到 10K QPS
Phase 5 [W33-W40] 架构升级：系统设计50题 + Java21升级 + Virtual Threads + K8s
Phase 6 [W41-W46] 云原生运维：OTel + 混沌工程 + 安全加固 + 成本优化
Phase 7 [W47-W52] 影响力输出：5PR + 5博客 + 演讲 + 面试100题
```

## my-xhs 版本演进

```
V1 [W13-W20] 学习阶段  — 读懂代码 + 画出架构图
V2 [W21-W40] 改造阶段  — 性能优化 + Java21升级 + Virtual Threads
V3 [W41-W46] 生产级阶段 — OTel + K8s + 多活 + 混沌验证
开源版[W47-W52]         — 完整文档 + 技术博客 + PR
```

## Phase 索引

| Phase | 目录 | 周 | 核心主题 |
|-------|------|-----|---------|
| 1 | [01-JAVA-CORE](../01-JAVA-CORE/README.md) | W1-W6 | JVM/GC/AQS/线程池/集合/Netty |
| 2 | [02-SPRING-STACK](../02-SPRING-STACK/README.md) | W7-W12 | Spring源码/MySQL/Redis |
| 3 | [03-MYXHS-DEEP-READ](../03-MYXHS-DEEP-READ/README.md) | W13-W20 | my-xhs 源码全景阅读 |
| 4 | [04-HIGH-CONCURRENCY](../04-HIGH-CONCURRENCY/README.md) | W21-W32 | 压测优化到 10K QPS |
| 5 | [05-ARCHITECTURE](../05-ARCHITECTURE/README.md) | W33-W40 | 系统设计 + Java21 升级 |
| 6 | [06-CLOUD-NATIVE](../06-CLOUD-NATIVE/README.md) | W41-W46 | OTel + 混沌 + K8s |
| 7 | [07-IMPACT](../07-IMPACT/README.md) | W47-W52 | 开源 + 写作 + 面试 |
| - | [08-CHECKLIST](../08-CHECKLIST/README.md) | 全阶段 | 检验标准清单 |
| - | [09-RESOURCES](../09-RESOURCES/README.md) | 全阶段 | 书单/论文/源码/工具 |

## 每周节奏模板

| 时段 | 内容 | 时间 |
|------|------|------|
| 周一-周三 | 理论学习 + 源码阅读 | 每晚 2h |
| 周四-周五 | 动手实践 + 代码编写 | 每晚 2h |
| 周六 | 项目实战（my-xhs 相关） | 全天 8h |
| 周日 | 技术写作 + 复盘总结 | 4h |
| 每日必做 | LeetCode 1 题 + 英文技术阅读 30min | 碎片时间 |

## 训练方法论

1. **理论 → 源码 → 手写**：每个知识点先理解原理，再读真实源码，最后自己实现一遍
2. **可追溯**：每条学习路径标注到具体的文件名、类名、方法名
3. **度量驱动**：每个优化有 Before/After 数字对比
4. **生产标准**：不以"能跑通"为终点，以"生产可用"为标准

## 危险信号（出现以下情况立即调整）

- 连续 3 天没有写代码
- 只说"看懂了"但画不出流程图
- 对着问题不能解释 WHY（为什么选这个方案而不是那个）
- 项目代码没有 commit（超过 1 周）

## 核心交付物

- [ ] X-SHOP（my-xhs 改造版）从 100 QPS 到 10,000 QPS 的完整项目
- [ ] 10+ 篇深度技术博客
- [ ] 5+ 个开源 PR
- [ ] 1 个 30 分钟技术演讲
- [ ] 完整架构设计文档
- [ ] 系统设计题库 50+ 题 + 答案
