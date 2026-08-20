# Apache SkyWalking 源码分析计划 (阶段 6.3)

> 仓库: `/data/workspace/source-code/code/spring/skywalking`
> HEAD: `b272da3 Prepare for release 10.4.0`
> 日期: 2026-08-17
> 冲突审计: 未发现 SkyWalking 的既有分析计划/HANDOFF/outline 产物；`Arthas` 为其他框架已有交接，明确不触碰
> 方法论: 09 怀疑审计 / Pass0→Pass1→Pass2→Pass3 / harness / 每域多轮 review 一次性收敛

## 0. 仓库规模与边界

SkyWalking 不是单模块库，而是：
- `apm-protocol/`：agent ↔ OAP 协议模型（17 Java 文件，另有 proto）
- `oap-server/`：核心后端（当前约 2344 Java 文件，含 server-core、analyzer、query、storage、cluster/config 等大量插件）
- `apm-webapp/` / `skywalking-ui/`：UI/前端边界
- `apm-dist/`：发行装配
- `test/`：跨模块测试与运行场景
- `docs/`：契约与配置文档

首阶段只把 **Java 后端核心 + 协议** 作为源码分析主战场；UI/发行装配先做归类，不把前端源码与 OAP 机制混为一谈。

## 1. 初步域拓扑（候选 8 域，Pass 0 后必须重新审计）

| 域 | 名称 | 主战场 | 依赖 |
|---|---|---|---|
| SW-1 | Agent Protocol / Data Model | `apm-protocol/` + proto | 协议基础 |
| SW-2 | OAP Server Bootstrap / Module SPI | `server-starter` / `server-core` / module manager / provider | SW-1 |
| SW-3 | Analysis Pipeline | `analyzer/`、segment/topology/alarm/metadata analyzers | SW-1/2 |
| SW-4 | Query / MQE / OAL | `query`、`mqe-*`、`oal-*`、expression/runtime | SW-2/3 |
| SW-5 | Storage / Persistence | `storage`、`storage-*`、record/query DAO、TTL/metadata | SW-2/4 |
| SW-6 | Cluster / Configuration / Coordination | `server-cluster-plugin/*`、`server-configuration/*`、coordination | SW-2/5 |
| SW-7 | Transport / Export / Observability | gRPC/HTTP ingress、exporter、health、metrics、telemetry | SW-2/5 |
| SW-8 | UI / Distribution / Integration Boundary | `apm-webapp`、`skywalking-ui`、`apm-dist`、test runtime | 汇总域，默认不深读前端实现 |

> 重要：8 域只是初始候选，不是最终承诺。Pass 0 必须按真实调用/依赖拓扑重新拆分，防止“大域吞并多个机制”或“插件重复归属”。

## 2. 计划执行顺序

1. SW-1 Agent Protocol / Data Model
2. SW-2 OAP Bootstrap / SPI
3. SW-3 Analysis Pipeline
4. SW-4 Query / MQE / OAL
5. SW-5 Storage / Persistence
6. SW-6 Cluster / Configuration
7. SW-7 Transport / Export / Observability
8. SW-8 UI/Distribution/Integration 边界汇总

子域数字穷举（当前生产 Java）：
- SW-3A Trace/Agent analyzer: `analyzer/agent-analyzer` **60**
- SW-3B Meter/MAL analyzer: `analyzer/meter-analyzer` **54**
- SW-3C Log/LAL analyzer: `analyzer/log-analyzer` **52**
- SW-3D Event analyzer: `analyzer/event-analyzer` **8**
- SW-3E Hierarchy compiler: `analyzer/hierarchy` **7**
- SW-3F GenAI analyzer: `analyzer/gen-ai-analyzer` **10**
- `server-core/analysis/manual` 不重复归入 analyzer 子域；它是结果模型/dispatcher 消费侧，按具体输出域交叉引用

> analyzer 六子域合计 **191**，与 Pass 0 全量数字一致。

## 3. 执行顺序（按依赖）
1. SW-1 Agent Command/Protocol boundary
2. SW-2 OAP Bootstrap/SPI
3. SW-3A Trace/Agent analyzer
4. SW-3B Meter/MAL analyzer
5. SW-3C Log/LAL analyzer
6. SW-3D Event analyzer
7. SW-3E Hierarchy compiler
8. SW-3F GenAI analyzer
9. SW-4 Query/MQE/OAL boundary
10. SW-5 Storage/Persistence

## 4. 冲突与边界铁律
- 只操作 `/data/workspace/source-code/code/spring/skywalking` 与对应 `analysis/source-analysis/skywalking/`
- 不触碰 `Arthas` 既有文档
- 不假设 8 域最终正确；每个 Pass 0 都要数字穷举、包归属、调用链复核
- UI 不与 OAP 后端机制混写
- 插件机制必须区分 API/SPI、实现、装配和运行时发现

## 5. MT/SW 方法论交付物
- 每域：`pass0-discovery.md` / `pass2-questions.md` / `outline.md` / `review-notes.md` / `harness/`
- 每次写完文档后：锚点、数字、因果链、前向引用、负面空间、官方测试交叉、多轮 review 一次性完成
- 全阶段：`HANDOFF-SKYWALKING.md`

## 6. 当前状态

- [x] 冲突审计：无已有 SkyWalking 分析产物
- [x] 仓库结构/规模首轮盘点
- [x] 初始 8 域候选计划
- [ ] SW-1 Pass 0
- [ ] SW-2~SW-8
- [ ] HANDOFF-SKYWALKING

## 6. 首个域 SW-1 Pass 0 入口

- `apm-protocol/` 全部 Java/proto 文件数字穷举
- agent data model / segment / span / metrics / logs / topology / metadata 消息模型归类
- 验证 proto 生成物与 Java API 的边界
- 先不读 OAP 存储/分析实现，遵守前向引用纪律
