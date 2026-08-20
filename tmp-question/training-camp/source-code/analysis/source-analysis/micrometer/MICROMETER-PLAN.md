# Micrometer 源码分析计划 (阶段 6.1)

> **版本**: 开发版 1.18.x (gradle.properties compatibleVersion=1.17.0)
> **仓库**: /data/workspace/source-code/code/spring/micrometer
> **日期**: 2026-08-17 | **方法论**: 09 怀疑审计 → 域规划 → 三层循环
> **状态**: 规划 v1 (09 审计完成, 域规划完成)

## 一、仓库全貌 (09 怀疑审计)

### 1.1 模块结构
| 模块 | 主源码文件数 | 定位 |
|---|---|---|
| micrometer-core | 366 (instrument 335 + aop 8 + annotation 5 + ipc 5 + util 13) | 核心: Meter 模型/Registry/Filter/Binder/AOP |
| micrometer-observation | 41 | 观测模型: Observation/Registry/Handler/Context/transport |
| micrometer-commons | 36 | 支撑: KeyValue/KeyValues/annotation 工具 |
| implementations/ | 24 registry 模块 (prometheus/statsd/otlp/jmx/influx...) | 各后端 Registry (边界引用) |

### 1.2 关键文件行数 (反向扫描 ≥300 行, 40 文件全归类)
| 文件 | 行数 | 定位 |
|---|---|---|
| observation/Observation.java | 1569 | MI-6 核心 (观测模型) |
| instrument/MeterRegistry.java | 1337 | MI-1 核心 (注册表/过滤/分发) |
| binder/db/MetricsDSLContext.java | 800 | MI-5 扩展 (jOOQ DSL) |
| binder/jvm/ExecutorServiceMetrics.java | 548 | MI-5 扩展 (线程池) |
| instrument/Meter.java | 524 | MI-1 支撑 (Meter 模型) |
| instrument/LongTaskTimer.java | 518 | MI-2 扩展 (长任务计时) |
| distribution/DistributionStatisticConfig.java | 498 | MI-3 支撑 (直方图配置) |
| binder/db/PostgreSQLDatabaseMetrics.java | 492 | MI-5 扩展 (PG 指标) |
| config/MeterFilter.java | 481 | MI-7 核心 (过滤器) |
| instrument/Timer.java | 478 | MI-2 核心 (计时器) |
| binder/kafka/KafkaConsumerMetrics.java | 440 | MI-5 扩展 (Kafka 指标) |
| commons/KeyValues.java | 436 | MI-8 支撑 (标签) |
| binder/jvm/JvmGcMetrics.java | 429 | MI-5 核心 (GC 指标) |
| instrument/DistributionSummary.java | 421 | MI-3 核心 |
| instrument/Tags.java | 404 | MI-8 支撑 (标签) |
| binder/tomcat/TomcatMetrics.java | 401 | MI-5 扩展 |
| config/validate/Validated.java | 393 | MI-7 扩展 (配置校验) |
| aop/TimedAspect.java | 378 | MI-4 核心 (切面) |
| binder/kafka/KafkaMetrics.java | 377 | MI-5 扩展 (Kafka) |
| ipc/http/HttpSender.java | 370 | E1 推送型 HTTP 发送 |
| observation/ObservationHandler.java | 336 | MI-6 核心 (处理器链) |
| binder/okhttp3/OkHttpMetricsEventListener.java | 334 | MI-5 扩展 (OkHttp) |
| instrument/search/Search.java | 333 | MI-1 扩展 (按名查询) |
| observation/SimpleObservation.java | 330 | MI-6 核心 (实现) |
| instrument/Metrics.java | 323 | E5 静态门面 |
| instrument/internal/DefaultLongTaskTimer.java | 323 | MI-2 扩展 (长任务实现) |
| binder/cache/HazelcastIMapAdapter.java | 323 | MI-5 扩展 (缓存) |
| instrument/logging/LoggingMeterRegistry.java | 321 | MI-1 扩展 (日志型 Registry) |
| instrument/search/RequiredSearch.java | 312 | MI-1 扩展 (按名查询) |
| aop/CountedAspect.java | 312 | MI-4 核心 (切面) |
| binder/jpa/HibernateMetrics.java | 310 | MI-5 扩展 (JPA) |
| instrument/HighCardinalityTagsDetector.java | 308 | MI-1 扩展 (高基数检测) |
| instrument/AbstractTimer.java | 307 | MI-2 支撑 (Timer 基类) |

## 二、09 怀疑审计结论

### 2.1 执行计划断言 vs 源码实证
| 执行计划断言 | 实证 | 结论 |
|---|---|---|
| MI-1 SimpleMeterRegistry→CompositeMeterRegistry→PrometheusMeterRegistry | simple/ 4 文件 + composite/ 13 文件 ✅; Prometheus 在 implementations/micrometer-registry-prometheus (独立模块) | ✅ 成立 (Prometheus 边界引用) |
| MI-2 Counter/Gauge/FunctionCounter/Timer→MeterFilter | 顶级类全部存在 ✅; Timer 家族含 LongTaskTimer(518)/FunctionTimer/TimeGauge/MultiGauge | ✅ 成立 (家族可扩展) |
| MI-3 Histogram→SLO→PauseDetector | distribution/ 22 文件 ✅ (TimeWindow* 直方图家族 + pause/) | ✅ 成立 |
| MI-4 TimedAspect→CountedAspect→MeterHandler | aop/ 8 文件 ✅; 但" MeterHandler"应修正为 **MeterTagAnnotationHandler** (MeterTag 注解体系 4 文件) | ⚠️ 名称修正 |
| MI-5 JvmMetrics/JvmGcMetrics/JvmThreadMetrics→Binders | binder/ 175 文件 ✅ (jvm/ + cache/db/grpc/http/kafka/netty4/tomcat 等 18 子包) | ✅ 成立 |
| MI-6 ObservationRegistry→ObservationHandler→Context | **主战场在独立模块 micrometer-observation (41 文件)**; core 内 observation/ 4 文件 (DefaultMeterObservationHandler) 是桥接 | ⚠️ 主战场修正 (独立模块) |
| MI-7 deny/accept/configure→最小/最大期望值/重命名/百分比 | config/ 8 文件 ✅ (MeterFilter 481 行, accept L786-788 在 MeterRegistry 消费) | ✅ 成立 |

### 2.2 域内扩展点 (09 审计新增, 类比 Gateway 8 处)
| # | 扩展 | 内容 |
|---|---|---|
| E1 | push/ + step/ 机制 | PushMeterRegistry (push/ 3 文件) + StepMeterRegistry (step/ 13 文件) + ipc/http/HttpSender(370) — 推送型/步长聚合 Registry 家族 + HTTP 发送 |
| E2 | Timer 家族扩 | LongTaskTimer (518)/FunctionTimer/TimeGauge/MultiGauge — 执行计划只列了 3 个 |
| E3 | TimeWindow* 滑动窗口 | TimeWindowSum/Max/FixedBoundaryHistogram/PercentileHistogram — 桶/百分位计算机制 |
| E4 | MeterTag 注解体系 | MeterTagAnnotationHandler/MeterTagSupport (aop/ 4 文件) — @Timed/@Counted 的 tag 解析 |
| E5 | Metrics 静态门面 | Metrics.java (globalRegistry CompositeMeterRegistry) — 全局注册入口 |
| E6 | Observation 独立模块全量 | observation 41 文件 (含 aop/ObservedAspect + transport/Propagator) — 执行计划只列 3 概念 |
| E7 | NamingConvention + 配置校验 | NamingConvention.java + MeterRegistryConfigValidator (config/validate/ 6 文件) |
| E8 | commons 支撑层 | KeyValue/KeyValues (436)/annotation 工具 — 跨域复用 |
| E9 | search/ 查询 API | Search (333)/RequiredSearch (312) — 按名称/标签查询 Meter 集合 |
| E10 | LoggingMeterRegistry | logging/ 3 文件 — 日志输出型 Registry (调试验证) |
| E11 | HighCardinalityTagsDetector | (308) — 高基数标签自动检测 (同名列超阈值) |

### 2.3 排除与边界
- **registry 后端模块** (implementations/ 24 个) + **core 内 dropwizard/ 11 文件** (DropwizardMeterRegistry 旧适配, 同属后端, 归 MI-1 边界引用不展开): 只引用 PrometheusMeterRegistry 的 push 机制交叉验证 (MI-1 E1), 不探索源码
- **util/internal/logging** (core 13 + commons 13 = 26 文件): 日志适配, 排除 (Slf4J/Jdk 双实现)
- **benchmarks/docs/samples/concurrency-tests**: 非核心源码, 排除
- **binder 各子包** (cache/db/grpc/http/kafka/netty4/tomcat 等): 归 MI-5 域内, 精选 jvm 家族深度 + 其余归类

## 三、域规划 (7 域 + 8 扩展, 修订执行计划)

### 3.1 域清单
| # | 域 | 核心主题 | 拓扑 |
|---|---|---|---|
| MI-1 | MeterRegistry | MeterRegistry(1337)→注册/过滤/分发; SimpleMeterRegistry; CompositeMeterRegistry(13) + **E1 push/step 家族** + **E5 Metrics 门面** + **E9 search/ 查询 API (Search 333/RequiredSearch 312)** + **E10 LoggingMeterRegistry(321)** + **E11 HighCardinalityTagsDetector(308)** | 1 (根) |
| MI-7 | MeterFilter | MeterFilter(481) deny/accept/configure→最小/最大期望/重命名/百分比 + **E7 NamingConvention+校验** | 2 (被 registry 消费, 紧随) |
| MI-2 | Counter/Gauge/Timer 家族 | Counter/Gauge/FunctionCounter/Timer + **E2 LongTaskTimer(518)/FunctionTimer/TimeGauge/MultiGauge** | 3 |
| MI-3 | DistributionSummary | DistributionSummary(421) Histogram→SLO→PauseDetector + **E3 TimeWindow* 家族** | 4 |
| MI-4 | @Timed/@Counted | annotation/ 5 + aop/ 8: TimedAspect(378)/CountedAspect + **E4 MeterTag 体系** + **E6 联动 ObservedAspect** | 5 |
| MI-6 | Observation | **独立模块 micrometer-observation (41 文件)**: Observation(1569)/Registry/Handler/Convention/Context + aop/ObservedAspect + transport/Propagator + **core 桥接 DefaultMeterObservationHandler** | 6 |
| MI-5 | MeterBinder | binder/ 175: MeterBinder 接口 + **jvm 家族深** (JvmGcMetrics 429/JvmThreadMetrics/ExecutorServiceMetrics 548) + 22 子包归类 + **E8 commons 支撑** | 7 (最高层消费) |

### 3.2 拓扑理由 (前向引用纪律)
- MI-1 根: 所有 Meter 从 Registry 创建, MeterFilter 在注册时消费 (accept L786-788)
- MI-7 紧随: filter 是 registry 内部机制, 无反向依赖
- MI-2→MI-3: 同一 meter 家族, 依赖 registry
- MI-4: 注解切面消费 meter 家族 (TimedAspect→Timer)
- MI-6: observation 独立模块, 仅 core 桥接 4 文件 (DefaultMeterObservationHandler→MeterRegistry) — 前向依赖 MI-1
- MI-5: binder 消费所有 meter 类型 — 拓扑最后, 无前向引用

### 3.3 交叉引用
- **MI-5 grpc 子包** → gRPC 阶段 (census 集成对比)
- **MI-6 Observation** → Spring Cloud Commons Observation (SCC-7/10/13 网关已实证) — ObservationHandler 桥接
- **MI-4 @Timed** → Spring Boot Actuator (阶段 6 外部参照)

## 四、harness 规划 (费曼验证)
| harness | 验证点 | 断言方向 |
|---|---|---|
| MiniMI-1 | SimpleMeterRegistry 注册/查询/过滤消费; CompositeMeterRegistry 转发; Metrics 门面 | 5 |
| MiniMI-7 | MeterFilter deny/accept/configure: 重命名/最小期望/百分比 | 3 |
| MiniMI-2 | Counter 单调递增/Gauge 瞬时/Timer 直方图/LongTaskTimer | 4 |
| MiniMI-3 | DistributionSummary SLO 直方图桶/百分位 TimeWindow | 3 |
| MiniMI-4 | @Timed/@Counted AOP 拦截 → 实际 meter 产生; MeterTag 动态 tag | 3 |
| MiniMI-6 | ObservationRegistry→Handler 链: start/stop 事件; Context 传递 | 4 |
| MiniMI-5 | JvmGcMetrics/JvmThreadMetrics bind 到 registry 后 meter 集合验证 | 3 |
| 合计 | | 25 |

## 五、执行计划 (三层循环)
| 域 | Pass 0 发现 | Pass 1 细节 | Pass 2 问题 | Pass 3 收敛 | 审查轮次 |
|---|---|---|---|---|---|
| MI-1 | 计划 | ①注册②过滤③分发④composite 转发⑤push/step | 默认 6 问 | 全闭环 | ≥2 |
| MI-7 | 计划 | ①四种 reply ②configure 变换③最小/最大期望 | 同上 | 全闭环 | ≥2 |
| MI-2 | 计划 | ①Counter ②Gauge ③Timer 直方图 ④LongTask ⑤MultiGauge | 同上 | 全闭环 | ≥2 |
| MI-3 | 计划 | ①直方图桶 ②SLO ③百分位 ④TimeWindow 滑动 | 同上 | 全闭环 | ≥2 |
| MI-4 | 计划 | ①TimedAspect ②CountedAspect ③MeterTag ④ObservedAspect | 同上 | 全闭环 | ≥2 |
| MI-6 | 计划 | ①Observation 状态机 ②Registry ③Handler 链 ④Context ⑤transport | 同上 | 全闭环 | ≥3 |
| MI-5 | 计划 | ①MeterBinder ②JvmGc ③JvmThread ④ExecutorService ⑤22 子包归类 | 同上 | 全闭环 | ≥2 |

## 六、检查单
- [x] 09 怀疑审计: 执行计划断言全实证 (7 域 2 修正: MI-4 名称/MI-6 主战场)
- [x] 反向扫描: ≥300 行 40 文件全归类 (无遗漏, 第二轮 review 补齐 22 个)
- [x] 深审修正 (2026-08-17): 排除计数 4→26 / step 12→13 / push 2→3 / dropwizard 边界声明 / binder 22→18 子包
- [x] 域内扩展: 8 处 (E1-E8)
- [x] 排除: registry 后端/日志适配/非核心目录
- [x] 拓扑: 前向引用纪律遵守 (MI-6 桥接仅 4 文件)
- [x] MI-1 三层循环完成 (25/25) (Pass0 发现 6 机制 / Pass1 细节 Q1-Q6 / Pass2 问题 P1-P6 闭环 / Pass3 收敛)
- [x] MI-1 三层循环完成 (25/25) (Pass0 发现 6 机制 / Pass1 细节 Q1-Q6 / Pass2 问题 P1-P6 闭环 / Pass3 收敛)
- [x] MI-7 三层循环完成 (30/30, 打脸3次) (Pass0 15 工厂+校验链 / Pass1 Validator 链 / Pass2 P1-P6 / Pass3 收敛)
- [x] MI-2 三层循环完成 (20/20, 打脸2次)
- [x] MI-3 三层循环完成 (20/20, review 3 轮)
- [x] MI-4 三层循环完成 (22/22, review 3 轮; 重复 @Timed 风险)
- [x] MI-5 三层循环完成 (16/16, review 2 轮; JVM 主干深读)
- [x] MI-6 三层循环完成 (20/20, review 2 轮; Observation 核心状态机)
- [x] MiniMI1 harness 25/25 (MI-1, R3+R6 补强后)
- [x] MiniMI7 harness 30/30 (MI-7, 打脸3次修正)
- [x] MiniMI2 harness 20/20 (MI-2, 打脸2次)
- [x] MiniMI3 harness 20/20
- [x] MiniMI4 harness 22/22
- [x] MiniMI5 harness 16/16
- [x] MiniMI6 harness 20/20
- [ ] HANDOFF-MICROMETER.md (待执行)