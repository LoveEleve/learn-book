# SkyWalking 源码学习范围规划

> **版本**: v10.4.0
> **仓库**: `/data/workspace/source-code/code/spring/skywalking/`
> **规模**: oap-server 2134 个源文件，8+ 子模块
> **日期**: 2026-08-03

---

## 一节、仓库概况

Apache SkyWalking 是分布式 APM（应用性能监控）系统，采用 **Agent 采集 → OAP Server 分析 → UI 展示** 架构。OAP Server 是核心——接收 Agent 上报的 Trace Segment 数据，进行流式分析（Service/Instance/Endpoint 指标聚合、拓扑构建、告警），存储到 ES/MySQL/InfluxDB 等后端。

**子模块清单**（核心 oap-server）：

| 子模块 | 职责 | 状态 |
|---|---|---|
| `server-receiver-plugin/` | 数据接收层：gRPC/HTTP/Kafka/Fluentd/SkyWalking 协议 | ✅ |
| `server-core/` | 核心引擎：配置、模块管理、启动生命周期 | ✅ |
| `analyzer/` | 分析引擎：Trace 聚合、指标计算、拓扑构建 | ✅ |
| `server-storage-plugin/` | 存储适配：ES/MySQL/InfluxDB/PostgreSQL/BanyanDB | 淘汰 |
| `server-alarm-plugin/` | 告警引擎：规则匹配→通知 | 🟡 |
| `ai-pipeline/` | AI 辅助管道（HTTP URI 模式学习等） | 淘汰 |
| `server-cluster-plugin/` | 集群协调（ZooKeeper/K8s/Consul/Nacos） | 淘汰 |
| `exporter/` | 指标导出（Prometheus/OTLP） | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（3 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| S-1 | **OAP 启动与模块化体系** | OAPServerBootstrap, ModuleManager, ModuleDefine, ModuleProvider | **SPI 模块加载**：基于 `ModuleDefine`/`ModuleProvider` 的服务加载器——`ModuleManager.init()` 扫描 `META-INF/services` 加载所有模块→`ModuleDefine.prepare()` → `ModuleProvider.start()` 启动服务；**核心模块**：`CoreModule`（配置+启动）、`SharingServerModule`（gRPC 服务器）、`ConfigurationModule`（动态配置）、`ClusterModule`（集群协调）、`StorageModule`（存储引擎） |
| S-2 | **Trace 接收与分析管道** | TraceSegmentReportServiceHandler, TraceAnalyzer, SegmentParserListenerManager, AnalysisListener(5种) | **gRPC 接收**：`TraceSegmentReportServiceHandler.collect()` 接收 Agent 上报的 Protobuf `SegmentObject`；**Listener 模式分析**：`TraceAnalyzer.doAnalysis(segmentObject)` → `createSpanListeners()` 创建 5 种 Listener→**按 Span 类型分发**：① `notifySegmentListener`(整个 Segment 生命周期) ② spanId==0 → `notifyFirstListener`(第一个Span=入口识别) ③ `SpanType.Entry` → `EntryAnalysisListener`(服务端接收耗时) ④ `SpanType.Exit` → `ExitAnalysisListener`(客户端调用耗时+远程依赖) ⑤ `SpanType.Local` → `LocalAnalysisListener`(本地方法耗时)→最后 `notifyListenerToBuild()` 所有 Listener 构建 Metrics 送 `MetricsStreamProcessor`；**多接收器**：server-receiver-plugin 含 12+ 种（trace/OTEL/Envoy/eBPF/Browser/AWS Firehose 等） |
| S-3 | **指标聚合与存储流水线** | MetricsStreamProcessor, MetricsAggregateWorker, MetricsPersistentWorker, MetricsTransWorker | **三层降采样**：`create()` 构建 metrics 流水线——`MetricsAggregateWorker` 分钟级聚合→`MetricsTransWorker` 分钟级转换→**`minutePersistentWorker`**(持久化分钟级)→**`hourPersistentWorker`**(小时级降采样)→**`dayPersistentWorker`**(天级降采样)；**OAL 脚本驱动**：`oal-rt/` 子模块执行 `oal-grammar/` 定义的 OAL 脚本→声明式定义指标聚合规则（如 `service_resp_time = from(Service.latency).sum()`）；**拓扑**：`ServiceTopologyBuilder` 每 5 分钟从 ServiceRelation 指标构建拓扑图——`EndpointTopologyBuilder`/`ProcessTopologyBuilder` 构建端点和进程级拓扑 |

### 🟡 扩展域（3 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| S-4 | **告警引擎** | AlarmModule, AlarmRulesWatcher, AlarmCallback | **规则定义**：`alarm-settings.yml` 定义指标阈值规则（如 `service_resp_time > 1000`）→`AlarmRulesWatcher` 监听规则变更；**通知**：`AlarmCallback` SPI——Webhook/WeChat/DingTalk/Feishu 等通知渠道 |
| S-5 | **GraphQL 查询 API** | query-graphql-plugin(155文件), MetadataQuery, MetricsQuery, TraceQuery, LogQuery, TopologyQuery | **6 种查询插件**：`query-graphql-plugin`(GraphQL——UI 主查询接口)、`logql-plugin`(LogQL 日志查询)、`promql-plugin`(PromQL——Prometheus 风格指标查询)、`traceql-plugin`(TraceQL 链路查询)、`zipkin-query-plugin`(Zipkin JSON API 兼容)、`status-query-plugin`(OAP 自身状态查询)；**数据入口**：UI → GraphQL Query → 查询对应 Worker 的持久化数据（ES/MySQL/BanyanDB）→ 聚合返回；`MQE 引擎`：`mqe-rt/`(21文件)提供 Metrics Query Expression 语法——类似 SQL 的指标查询 DSL |
| S-6 | **Server-Module 框架** | ModuleManager(98行), ModuleDefine, ModuleProvider, Service | **SPI 微内核**：`library-module/` 提供整个 OAP 的插件化架构——每个 `ModuleDefine` 定义模块（Core/SharingServer/Storage/Query/etc.），每个 `ModuleProvider` 提供实现（如 Storage 有 ES/MySQL/BanyanDB Provider）；**生命周期**：`prepare()`→`start()`→`notifyAfterCompleted()`；ApplicationConfiguration YAML 驱动模块加载；`BootingLog` 表格化启动日志 |

---

## 三、淘汰清单

| 子模块/功能 | 理由 |
|---|---|
| `server-storage-plugin/` 多种存储 | ES/MySQL/InfluxDB 等存储适配——基础设施，非分析核心 |
| `server-cluster-plugin/` | 24 | ZK/K8s/Nacos 集群协调——运维配置 |
| `server-telemetry/` | 24 | OAP 自身指标暴露（Prometheus SOFAMosn） |
| `server-fetcher-plugin/` | 27 | 外部指标拉取（Prometheus metrics adapter） |
| `exporter/` | 7 | 指标导出到外部（Prometheus/OTLP） |
| `server-health-checker/` | 5 | 健康检查 |
| `server-configuration/` | 34 | 动态配置同步（Apollo/Nacos/etc.） |
| `ai-pipeline/` | 8 | AI 辅助管道 |
| `server-tools/` `server-testing/` | 33/19 | 工具/测试 |
| `library-*` 存储客户端/工具库 | ~200 | ES client/BanyanDB client/K8s support/pprof parser 等——基础设施 |
| `analyzer/event-analyzer/gen-ai-analyzer/log-analyzer/meter-analyzer/` | 若干 | 事件/GenAI/日志/Meter 分析——特定场景 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 3 |
| 🟡 扩展域 | 3 |
| **总域** | **6** |
| 淘汰子模块/功能 | 12+ 个 |

---

## 五、学习顺序建议

```
S-1 OAP 启动与模块化（理解 SPI 服务加载器）
  → S-2 Trace 接收与分析管道（理解数据如何进入→分析）
    → S-3 指标聚合与拓扑（理解指标如何计算）
      → S-4 告警引擎（按需）
```

以上规划完成，共 **3🔴+2🟡=5 域**。聚焦 OAP Server 的 3 个核心链路（接收→分析→聚合），存储/集群/AI 等为基础设施。
