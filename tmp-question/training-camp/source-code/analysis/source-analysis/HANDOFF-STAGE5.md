# 阶段5 RPC 与服务治理 — 总交接文档 (STAGE5 v1 超详细版)

> **日期**: 2026-08-17 | 阶段5.1~5.8 全收官, 5.9 待开工
> **给新 AI**: 本文是阶段5 的**唯一总入口** — 只读本文件 + 目标仓库的分域 HANDOFF 即可继续, 不必读其他文档。
> **权威**: 执行计划 `issue/源码分析执行计划.md` 阶段5 (9 仓库 76 域) + 方法论 `talk-method/source-code-analysis/methodology/zh/` (01-09, **09 对既有规划保持怀疑必读**)。
> **源码根**: `/data/workspace/source-code/code/spring/` (Spring 系) + `/data/workspace/source-code/code/` (其余, 各仓库 HANDOFF 内注明)

---

## §零 状态速查

### 阶段5 全景 (2026-08-17)

| 仓库 | 规划域数 | 实交域数 | 状态 | 规划 | 分域 HANDOFF | harness |
|:--|:--:|:--:|:--|:--|:--|:--:|
| Feign (5.1) | 5 | **6** (09 审计 +1) | ✅ **6/6 收官** | FEIGN-PLAN.md | HANDOFF-FEIGN.md | 20/20 |
| Dubbo (5.2) | 7 | **11** (09 重审 7→11) | ✅ **11/11 收官** (D-1~D-11, 四轮深审/域) | DUBBO-PLAN.md | HANDOFF-DUBBO.md | 12 个全过 |
| gRPC (5.3) | 6 | **8** (重审 6→8, +xDS/RLS) | ✅ **8/8 收官** (G-1~G-8, 全量回归 38/38) | GRPC-PLAN.md (v5 终版) | HANDOFF-GRPC.md | 38/38 |
| Spring Cloud Commons (5.4) | 13 | **13** | ✅ **13/13 收官** (SCC-1~13, 100+ 处 REVIEW 修正) | SCC-PLAN.md | HANDOFF-SPRING-CLOUD-COMMONS.md | 100/100 |
| Spring Cloud Gateway (5.5) | 9 | **9** (谓词 11→14 种, GW-9 代际过时修正) | ✅ **9/9 收官** | GATEWAY-PLAN.md | HANDOFF-GATEWAY.md | 31/31 |
| Spring Cloud OpenFeign (5.6) | 9 | **9** | ✅ **9/9 收官** | OPENFEIGN-PLAN.md | HANDOFF-OPENFEIGN.md | 45/45 |
| Spring Cloud Alibaba (5.7) | 10 | **10** (ALI-A1~A10) | ✅ **10/10 收官** | ALI-PLAN.md | HANDOFF-SPRING-CLOUD-ALIBABA.md | 83/83 |
| Nacos (5.8) | 7 | **25** (域发现 7→25, N-19 实证取消) | ✅ **25/25 收官** (六轮 REVIEW: 编造类名 2/伪锚点 22/文字锚 185/问数 11 域全修) | NACOS-PLAN.md | HANDOFF-NACOS.md | 113/113 |
| **Sentinel (5.9)** | 10 | — | 🚧 **另一 AI 进行中** (SENTINEL-PLAN.md 已建: 09 双基准审计 + 深度 REVIEW D1~D14 全量实证, 10 域 S-1~S-10 定稿, 尚未开工任何域) | SENTINEL-PLAN.md (已存在) | 待建 | — |

**阶段5 合计**: 8/9 仓库收官 + 5.9 另一 AI 进行中 · 92 域交付 (规划 76 域, 09 审计/域发现扩域后实交 92) · harness **443/443 断言全 PASS**

### ⚠️ 顺序与分工变更历史 (必读)

1. **分工**: Dubbo/gRPC/Gateway/OpenFeign 曾由其他 AI 并行 — 开工/接手前**必须确认无其他 AI 并行**, 避免撞车 (gRPC 曾因撞车作废产物重做)。
2. **5.8 Nacos 域数 7→25**: 对标 openjdk 48 域方法论全量域发现 (1640 主源/220K 行), 21 域清单 + 与执行计划 7 域差距分析, N-19 (备份面) 实证取消 → 25 域 26 篇大纲。
3. **5.9 Sentinel 已由另一 AI 接手** (SENTINEL-PLAN.md 于 2026-08-17 10:47 创建, 深度 REVIEW 已完): **本会话不碰 Sentinel** — 对照面写作时引用: Dubbo (D-9 Triple) / gRPC (G-1~G-3) / Alibaba (ALI-A3/A4/A5) / Nacos (NC-5)。

---

## §一 方法论与铁律 (权威: talk-method/.../methodology/zh/ 01-09)

### 核心管线 (每域必走)

```
Pass 0 读上下文(README/测试地图) → Pass 1 扫轮廓(≥5 真问题/读 2 测试)
→ Pass 2 闭环(假设→grep 验证→结论, 内化 KP §05) → Pass 3 大纲(四要素)
→ 六层深审(必须真找问题, 零发现=不合格) → 方案 A 强制: 时空溯源+harness
→ 全量回归 → 更新 HANDOFF → 用户确认后再下一个域
```

### 铁律 (用户明确, 违背会被批评)

1. **严格按规划, 不做多余选择** — 拓扑定了就逐项推进, 不要问"还是写 X?"
2. **一篇一篇写** — 不并行、不跳步、禁止批量生成 (用户曾批评批量交付质量)
3. **行号是"线索不是事实"** — 大纲/KP 行号写作时必须重 grep (每篇实测 2-6 处漂移)
4. **代码块贴真实源码** — 截取可, 编造不可 (深审缺陷 #14/#15)
5. **数字/事实必须验证** — 任何带数字的陈述回源码验证, 禁止"凭记忆"
6. **每篇写完整理后做深度 REVIEW** — 主动自查, 不等用户要求
7. **行号限制废弃为软参考** — 内容完整 > 行数 (2026-08-13 修正)
8. **09 对既有规划保持怀疑** — 域清单/数字/依赖方向/顺序四类怀疑对象, 每阶段开工前验证

### 深审缺陷档案 (issue/源码分析深审缺陷档案.md, 15 类)

- A 内容层: #1 事实错误 / #2 API 编造 / #3 文件名推断 / #4 跨项目转移 / #5 覆盖率 / #6 跨层不一致
- B 结构层: #7 文字锚 / #8 篇节标注 / #9 表格格式 / #10 数字自洽
- C 过程层: #11 范围规划不可信 / #12 待确认清零 / #13 一次一域 / #14 书稿代码块编造 / #15 大纲描述与源码漂移

---

## §二 执行计划修正清单 (9 项, 全部落文档)

| 修正 | 原文 (执行计划) | 实测 | 落点 |
|:--|:--|:--|:--|
| 5.1 Feign 域数 | 5 域 (F-1~F-5) | **6 域** (09 审计 +1) | FEIGN-PLAN.md |
| 5.2 Dubbo 域数 | 7 域 (DB-1~DB-7) | **11 域** (新增 triple/remoting/序列化/元数据, 覆盖率 157%) | DUBBO-PLAN.md |
| 5.3 gRPC 域数 | 6 域 (G-1~G-6) | **8 域** (重审新增 G-7 xDS 184 文件全仓库最大 + G-8 RLS) | GRPC-PLAN.md (v1→v5) |
| 5.5 Gateway 谓词 | 11 种 | **14 种** (漏 ReadBody/XForwardedRemoteAddr/CloudFoundryRouteService) | GATEWAY-PLAN.md |
| 5.5 Gateway GW-9 | CorsGatewayFilter | **代际过时** (无此类, 全局 CORS 配置) | GATEWAY-PLAN.md |
| 5.8 Nacos 域数 | 7 域 (NC-1~NC-7) | **25 域** (对标 48 域方法论全量域发现, N-19 实证取消) | NACOS-DOMAIN-DISCOVERY.md |
| 5.8 Nacos 长轮询归属 | NC-7 客户端侧 | **config 侧** (LongPollingService.java:264 内部类) | NACOS-PLAN.md 09 审计 |
| 5.8 Nacos 序列化 | 归属 common | **consistency/serialize** | NACOS-PLAN.md 09 审计 |
| 5.9 Sentinel 域序 | ST-1~ST-10 | 待开工时 09 审计验证 | 待建 |

---

## §三~§八 各仓库交付速查 (全量固化, 细节见分域 HANDOFF)

### §三 Feign 6/6 (5.1, OpenFeign 13.14-SNAPSHOT)

F-1 Builder → F-2 契约解析 → F-3 代理生成 → F-4 编码/解码 → F-5 拦截器 (+09 审计扩域)。4🔴+2🟡, 41 节, 161 问, harness 4 个 20/20, 时空溯源完成。

### §四 Dubbo 11/11 (5.2, 3.x)

D-1 SPI 微内核 (六轮深审, 10/10) → D-2 服务导出 (12/12) → D-3 服务引用 → D-4 RPC 调用 (大发现: 集群级 Filter 链) → D-5 注册中心 → D-6 负载均衡 (AdaptiveMetrics 公式) → D-7 集群容错 → D-8a 传输抽象+exchange (pipeline 五段) → D-8b HTTP 传输栈 (triple→http12 106 处 import) → D-9 Triple 协议 (gRPC 兼容) → D-10 序列化 → D-11 元数据/服务发现。每域四轮深审 + harness + 反写测试。

### §五 gRPC 8/8 (5.3, 1.83.1)

G-1 ProtoBuf/Stub → G-2 服务端 (ServerImpl 980) → G-3 客户端 (ManagedChannelImpl 2200) → G-6 流控重试 (RetriableStream 1618, 双模式互斥 L146-147) → G-5 命名解析 (DnsNameResolver 709 + UdsNameResolver) → G-4 负载均衡 (RoundRobin 在 util/MultiChild 414) → G-7 xDS (184 文件, 全仓库最大) → G-8 RLS (CachingRlsLbClient 1104)。全量回归 38/38, 收官全量审查 (07 五维度) 通过。

### §六 Spring Cloud Commons 13/13 (5.4, 4.3.2, 279 主源)

SCC-1 Bootstrap 双轨制 → SCC-2 @RefreshScope → SCC-8 RefreshEndpoint → SCC-9 配置加密 → SCC-13 NamedContextFactory → SCC-3 服务发现抽象 → SCC-4 服务注册抽象 → SCC-5 @LoadBalanced → SCC-6 Supplier 体系 → SCC-7 ReactorLB → SCC-11 BlockingLB 重试 → SCC-12 扩展策略 → SCC-10 断路器抽象。8 harness 100/100, 260 问。

### §七 Spring Cloud Gateway 9/9 (5.5, 4.3.2)

GW-1 路由定位 → GW-2 过滤器链 → GW-3 谓词 (14 种) → GW-4 负载均衡 → GW-5 Netty 转发 → GW-6 限流 → GW-7 熔断 → GW-8 路径重写 → GW-9 跨域 (全局 CORS)。9 harness 31/31。

### §八 Spring Cloud OpenFeign 9/9 (5.6, 4.3.2)

OF-1 @FeignClient → OF-2 契约 → OF-3 编码/解码 → OF-4 拦截器 → OF-5 负载均衡 → OF-6 熔断 → OF-7 压缩 → OF-8 配置 → OF-9 指标。9 harness 45/45。

---

## §九 阶段5.7/5.8 收官细节 (本会话)

### 5.7 Spring Cloud Alibaba 10/10 (2026-08-16 收官)

- 版本 2025.0.0.0 (pom.xml 实证), 源码 `/data/workspace/source-code/code/spring/spring-cloud-alibaba`
- **10 域**: ALI-A1 配置加载 → A2 配置刷新 → A3 服务注册/发现 → A4 LoadBalancer → A5 容错 → A6 三路限流 (AspectJ/SentinelWebFlux/RestTemplate) → A7 数据源 → A8 Gateway+断路器 → A9 Seata → A10 RocketMQ Binder
- 交付: ALI-PLAN + 10 outline + 10 KP + 5 harness **83/83** + 时空溯源 3
- 审查: 09 审计 + 六轮 REVIEW (淘汰清单反查修正 commons 定位、examples 136 文件验证面 EXAMPLES-VERIFICATION.md)
- 关键锚点: NacosPropertySourceLocator.java:47 / NacosRefreshHistory / NacosServiceRegistry (DiscoveryClient 抽象) / SentinelResourceAspect (Spring 侧)

### 5.8 Nacos 25/25 (2026-08-16 收官, 08-17 六轮 REVIEW)

- 版本 3.0.3 (pom.xml revision 实证), 源码 `/data/workspace/source-code/code/spring/nacos` (git 单提交 d14ae0ca)
- **域发现**: 对标 openjdk 48 域方法论 → 21 域清单 (NACOS-DOMAIN-DISCOVERY.md) + 执行计划 7 域差距分析 → **25 域 26 篇大纲** (N-07/N-08/N-15 拆多篇)
- 交付: NACOS-PLAN (09 审计 v1) + 25 outline + 25 KP + 25 深审 + 3 时空溯源 + 10 harness **113/113** (MiniNaming 18/MiniConfig 11/MiniRedo 18/MiniClientManager 9/MiniHealthCheck 11/MiniPush 7/MiniConfigStorage 11/MiniConfigRemote 10/MiniCluster 11/MiniNotify 7)
- 六轮 REVIEW 修正: 编造类名 2 (ClientOperationServiceImpl→接口:36+双实现 / ConfigChainRequestExtractor→ConfigQueryChainRequestExtractor:29) · 伪锚点 22 · 文字锚 185+2 · 问数 11 域补齐 20/20 · 区间锚点 2 (doSubscribe 486-528/doUnsubscribe 530-539) · extrnal 拼写加注
- 域清单修正: N-19 取消 (实证无独立备份面) · ClientLongPolling 在 config 侧 (LongPollingService.java:264 内部类) · 序列化在 consistency/serialize

---

## §十 阶段 5.9 Sentinel — 另一 AI 进行中 (本会话不碰)

**状态**: SENTINEL-PLAN.md 已由另一 AI 建成 (2026-08-17): 09 双基准审计 (执行计划 ST-1~ST-10 + issue S-1~S-10) + 二次 REVIEW + 深度 REVIEW 14 项 (D1~D14) — 10 域 S-1~S-10 定稿, 修正 6 处 + 新增面 4 处, 锚点实证完毕。
**本会话职责**: 不建 PLAN、不交付域、不并行 — 等待另一 AI 推进; 如需对照面 (Alibaba/Gateway/Dubbo/gRPC) 协作, 由对方发起。

**对照面 (写作时引用)**: Dubbo D-9 Triple / gRPC G-1~G-3 / Alibaba ALI-A3 (SphU.entry→FlowRule 链路) / A4 (DegradeRule/BlockExceptionHandler) / A5 (GatewayFlowRule) / Nacos NC-5 (健康检查对照)

**阶段 5 收官后**: 阶段 6 可观测与运维 (6 仓库 35 域: Micrometer/MicrometerTracing/SkyWalking/XXL-Job/Arthas/ShardingSphere)。注意: Arthas 目录已存在 (38 md, 阶段 6.5 可能已有人接触, 开工前确认)

---

## §十一 文件路径

| 东西 | 路径 |
|:--|:--|
| 执行计划 (权威) | `issue/源码分析执行计划.md` (阶段5: L441-564) |
| 方法论 | `talk-method/source-code-analysis/methodology/zh/` (01-09) |
| 深审缺陷档案 | `issue/源码分析深审缺陷档案.md` (15 类) |
| 阶段3 总交接 (参照) | `HANDOFF-STAGE3.md` |
| 分域 HANDOFF | 各仓库目录下 `HANDOFF-*.md` (见 §零 表) |
| 源码根 | `/data/workspace/source-code/code/spring/` (Spring 系) · `/data/workspace/source-code/code/` (其余) |

---

## §十二 完成检查单 (下次会话开始前)

- [ ] **5.9 Sentinel**: 另一 AI 进行中 (SENTINEL-PLAN.md 已建) — 本会话不并行, 等其收官后回填本表
- [ ] 每域交付后: 回填 §零 全景表 + 更新本文件
- [ ] 5.9 收官后 (另一 AI): 更新执行计划 (标注 5.9 ✅) + 本文件状态
- [ ] 阶段 5 全部收官后: 通知进入阶段 6 (6.1 Micrometer 先; 6.5 Arthas 目录已有 38 md, 开工前确认归属)