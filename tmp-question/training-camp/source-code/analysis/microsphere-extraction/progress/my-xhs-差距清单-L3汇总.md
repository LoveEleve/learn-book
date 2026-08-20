# my-xhs 差距清单执行（L3 汇总版）

> **定位**：把 L3 总大纲 33 篇的"该用没用"判定汇总为**一份可执行的 my-xhs 优化清单**——与 stage-3 B2 模式的优化规划合并（本文件为 L3 侧汇总 + 执行状态表）。
> 依据：l3-outline/ 33 篇主题文件的 my-xhs 判定（每项附文章号 + 实证位置）
> 使用方式：按 P1→P2→P3 逐项执行；每项完成后更新状态列
> 关联：`my-xhs-优化规划.md`（stage-3 B2 模式——P1-1 灰度/P1-2 压测基线等——**本文件与它合并执行**）

---

## 一、执行总览

| 优先级 | 项数 | 定位 |
|:---:|:---:|------|
| P1 | 3 | 立即执行（差距 TOP + 整改项——代码实证确定；P1-B 已实现移除） |
| P2 | 9 | 短期（1-2 周）——明确收益可落地（+cgroup 指标） |
| P3 | ~10 | 演进项（触发条件驱动） |

---

## 二、P1 立即执行（4 项）

### P1-A 反射整改：RocketMQHealthIndicator 用官方 getProducer()（文章 28）
- **现状**：`RocketMQHealthIndicator.java:89-90` 反射读 `RocketMQTemplate.producer` 私有字段——**官方 `getProducer()` 就摆在那**（RocketMQTemplate.java:75 实证），RocketMQ 升级即静默失效
- **动作**：改 `getProducer()` 直接调用（一行）；反射失败显式报错（fail-fast 而非静默降级）
- **验证**：健康检查正常 + RocketMQ 升级后无反射依赖

### ~~P1-B 网关 Filter 链缓存~~ ✅ 已实现（2026-08-15 核实——非差距）
- **实证**：`my-xhs-gateway/handler/CachingFilteringWebHandler.java:36` ConcurrentHashMap + `:58` computeIfAbsent + `:71-73` @EventListener(RefreshRoutesResultEvent) 重建——**与源码 09 模式同构且规避 G8 坑**（继承重写而非反射读私有字段）
- **结论**：从差距清单移除；保留"三坑核对"为 review 项（G7 空链窗口/G9 dispose——实现已规避 G8）

### P1-C JFR 启用 + NMT 开启（文章 31——诊断前置）
- **现状**：my-xhs 未启用 JFR；`-XX:NativeMemoryTracking` 未开（"600M 之谜"这类排障需要）
- **动作**：start-all.sh 追加 `-XX:StartFlightRecording=defaultrecording=true`（低开销常驻）+ `-XX:NativeMemoryTracking=summary`（排障时再开 detail）+ jcmd 动态启用兜底事故现场
- **验证**：jcmd JFR.check 正常 + NMT summary 输出 committed 分布

### P1-D 命令级限流（文章 11——差距 TOP② 自主实现）
- **现状**：Sentinel 已用（Bulkhead/规则下发/网关限流）——**Redis/MyBatis 命令级限流是官方空白**
- **动作**：参照源码 10/12 的命令拦截面（RedisCommandInterceptor/ExecutorFilter）——拦截命令 → Sentinel 埋点（2.6/2.4 交叉）
- **验证**：命令级 QPS 超限被限流

---

## 三、P2 短期（8 项）

| # | 差距 | 文章 | 动作 | 实证位置 |
|---|------|:--:|------|---------|
| P2-A | 端点粒度路由（差距 TOP①） | 2.7 | 参照源码 09 we:// 端点路由——网关按端点粒度管理 | 2.7 章节 |
| P2-B | 跨区复制（差距 TOP④） | 2.8 | Kafka 逻辑复制评估（10 2.1——按域配置复制范围） | 2.8 章节 |
| P2-C | 动态权重 LB（指标驱动） | 2.3 | 参照 stage-1-12 动态权重——监控指标 → 权重调整 | 2.3 章节 |
| P2-D | 特性开关集中化 | 4.8 | 灰度/功能开关从"代码 if 散落"→配置 + 条件注解 + 排序（Features 门控模式）——2.8 灰度路由受益 | 4.8 章节 |
| P2-E | 事件拦截链（统一埋点/审计） | 4.5/2.10 | 参照 03 事件拦截链——所有事件先过拦截器（统一审计） | 4.5 章节 |
| P2-F | HTTP Interface 评估 | 1.7 | Spring 6 官方声明式 HTTP 客户端——网关/微服务间调用评估 | 1.7 章节 |
| P2-G | 线程 dump/JFR 常规演练沉淀 | 5.2 | 故障演练 SOP：jstack 多快照 + fastthread.io 分析 + JFR 时间线——沉淀为 runbook | 5.2 章节 |
| P2-H | 属性级热更新链路核对（P2-4 衔接） | 2.2 | 热生效链路待核——Binder 路径确认 |
| P2-I | 自定义系统/cgroup 指标 binder（2026-08-15 补——用户指出） | 2.9 | K8s 部署后容器内存指标缺失——参照源码 11 CGroupMemoryMetrics（/sys/fs/cgroup 直读 + @ConditionalOnResource 探测）——K8s 内存限制感知 | 2.2 章节 |

---

## 四、P3 演进项（触发条件驱动）

| # | 差距 | 文章 | 触发条件 |
|---|------|:--:|---------|
| P3-A | Seata AT（多库强一致） | 2.5 | 多库事务场景出现 |
| P3-B | 异地多活 | 3.4/2.8 | 跨地域部署诉求 |
| P3-C | Mesh 引入（P3-4 衔接） | 2.7 | 流量治理诉求 |
| P3-D | K8s 上真集群 | 1.7 | **模板已就绪**（k8s/ 8 文件——configmap/deployment/hpa/ingress/namespace/service——2026-08-15 核实）——上真集群 + 演练为演进项 |
| P3-E | 虚拟线程迁移（JDK21） | 5.2 | 高并发演进点（JEP 444） |
| P3-F | RSocket/自定义协议 | 5.3 | 背压场景触发（先解决可观测性） |
| P3-G | Native（GraalVM） | 5.2 | 启动/内存硬指标诉求（Metadata 成本评估） |
| P3-H | Bean 级重建热更新 | 2.2/4.5 | 多数据源复杂配置场景 |
| P3-I | RunListener 启动钩子 | 4.5 | 启动治理诉求 |
| P3-J | XXL-JOB 推广到全部模块 | 4.8 | **analytics 已引入**（XxlJobConfig + FollowCounterRepairJob——2026-08-15 核实）——推广到其他模块为演进项 |

---

## 五、判定为"不该用"的知识（执行时注意——不是差距）

> 这些判定**不构成差距**（官方/JDK 覆盖）——清单执行时不要误入：

| 知识 | 文章 | 不该用理由 |
|------|:--:|-----------|
| Eureka | 2.1 | 过时（Nacos 已用） |
| ZK 注册中心 | 2.1/3.2 | 无依赖——Nacos 覆盖 |
| SPI 自研 | 4.3 | SpringFactories 覆盖 |
| JDK 工具自研 | 4.7 | JDK 原生覆盖（List.of/ProcessHandle 等） |
| Native | 5.2 | Metadata 成本 |
| 双栈自研 | 4.8 | 官方 SCG 单栈 |
| JFR 之外的重型诊断 | 5.2 | Arthas 覆盖 |

---

## 六、执行纪律

1. **一次一项**：P1-A 开始，完成后交 review 再下一项（与 L3 写作同纪律）
2. **改前基线 + 改后对比**：所有优化必须配套测量（02 篇 6 步循环——P1-2 压测基线先行）
3. **每项验证**：`mvn clean test` 全量 + 业务验证（上真线才算自己的）
4. **实证优先**：执行前重新 grep 实证位置（代码可能已变——行号以执行时为准）
