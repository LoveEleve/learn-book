# Nacos 域发现 v1 — 全量源码树域划分 (对标 openjdk 48 域方法论)

> 2026-08-16 | v1 依据: 全量源码树扫描 (23 模块 1640 主源 ~220K 行) — 不再照抄 issue 规划的 7 粗粒度域
>
> 方法论: 仿 `openjdk-book/docs/openjdk/planning/00-domain-discovery-v3.md` — 逐模块包归属验证 + 巨域阈值 (>30K 行或 >100 文件拆多篇) + 每域文件/行数精确表

## 范围说明

**纳入**: 23 模块主源 (api/naming/core/config/common/client/client-basic/console/auth/consistency/address/persistence/sys/lock/cmdb/prometheus/server/bootstrap/istio/ai/k8s-sync/maintainer-client/mcp-registry-adaptor)

**排除**: console-ui (前端) / distribution (构建) / doc / example / style / test

## 全模块规模基线 (2026-08-16 实证)

| 模块 | 文件 | 行数 | 归属 |
|:--|:--:|:--:|:--|
| config | 243 | 36,743 | 巨域 (配置服务端) |
| naming | 271 | 31,008 | 巨域 (命名服务端) |
| common | 212 | 30,381 | 巨域 (公共底座) |
| core | 264 | 26,584 | 近巨域 (核心服务端) |
| api | 274 | 22,889 | 契约面 |
| client | 120 | 17,902 | 客户端 SDK |
| console | 79 | 8,355 | 控制台后端 |
| client-basic | 51 | 4,660 | 客户端基础 |
| istio | 40 | 4,423 | 排除 (istio 集成) |
| persistence | 36 | 3,320 | 持久化 (按需) |
| ai | 31 | 4,039 | 排除 (AI 特性) |
| auth | 28 | 1,895 | 认证插件 |
| maintainer-client | 26 | 5,378 | 排除 (运维) |
| sys | 25 | 3,414 | 系统运维 |
| consistency | 23 | 1,638 | 一致性契约 |
| lock | 20 | 1,372 | 分布式锁 |
| 其他 7 模块 | 41 | 4,587 | 并入/排除 |

---

## 一、域清单 (21 域: 9🔴 + 12🟡, 对标 openjdk 粒度)

### A. 客户端 SDK 面 (client 120 + client-basic 51 + api 274 部分)

| # | 域 | 源 | 规模 | 级别 | 状态 |
|:--:|:--|:--|:--:|:--:|:--:|
| N-01 | **NamingService 注册发现** | client/naming (门面+代理+缓存) | ~30 文件 | 🔴 | ✅ 已交付 |
| N-02 | **ConfigService 配置客户端** | client/config (门面+worker+cache) | ~25 文件 | 🔴 | ✅ 已交付 |
| N-03 | **gRPC 通信内核** | common/remote/client (22) + api/remote (35) | 57 文件 8,600 行 | 🔴 | ✅ 已交付 |
| N-04 | **Redo 重做机制** | client/redo + client/naming/remote/gprc/redo | 10 文件 | 🔴 | ✅ 已交付 |
| N-05 | **客户端容灾缓存** | client/naming/backups+cache + client/config Local* | 15 文件 | 🟡 | ✅ 已交付 |
| N-06 | **地址管理** | client-basic/address | 8 文件 702 行 | 🟡 | ✅ 已交付 (并入 N-05 交付, 本表独立成域) |
| N-07 | **API 契约面** | api/naming + api/config + api/common | 120 文件 10,000 行 | 🟡 | ⬜ 新增 (接口/pojo/事件/注解) |
| N-08 | **客户端基础工具** | client-basic 其余 + client/env + client/monitor + client/security | ~30 文件 | 🟡 | ⬜ 新增 |

### B. 服务端命名面 (naming 271 文件 31,008 行 — 巨域拆 6 域)

| # | 域 | 源 | 规模 | 级别 | 状态 |
|:--:|:--|:--|:--:|:--:|:--:|
| N-09 | **命名控制器面** | naming/controllers (含 v2/v3) | ~20 文件 | 🟡 | ⬜ 新增 (HTTP 入口) |
| N-10 | **客户端管理面** | naming/core/v2 (53) + naming/core | 60 文件 | 🔴 | ⬜ 新增 (ClientService/clientId 模型) |
| N-11 | **健康检查** | naming/healthcheck (含 heartbeat/v2/interceptor) | 35 文件 | 🔴 | ⬜ 新增 (BeatCheckTask/HealthCheckTaskV2) |
| N-12 | **推送面** | naming/push (含 v2) | 27 文件 | 🔴 | ⬜ 新增 (UDP+gRPC+SubscriberService) |
| N-13 | **一致性落点** | naming/consistency (ephemeral/persistent) | 10 文件 | 🟡 | ⬜ 新增 (Distro/JRaft 数据路由) |
| N-14 | **命名集群/监控** | naming/cluster + naming/monitor + naming/remote | 20 文件 | 🟡 | ⬜ 新增 |

### C. 服务端配置面 (config 243 文件 36,743 行 — 巨域拆 5 域)

| # | 域 | 源 | 规模 | 级别 | 状态 |
|:--:|:--|:--|:--:|:--:|:--:|
| N-15 | **配置存储面** | config/server/service (88) + model (53) | 141 文件 21,000 行 | 🔴 | ⬜ 新增 (ConfigInfoService/DB 存储) — 巨域内再拆 |
| N-16 | **配置控制器面** | config/server/controller (19) + aspect + paramcheck | 30 文件 | 🟡 | ⬜ 新增 |
| N-17 | **配置长轮询/通信** | config/server/remote (15) + CommunicationController | 20 文件 | 🔴 | ⬜ 新增 (3.x gRPC 监听) |
| N-18 | **配置审计/加密/监控** | config/server/monitor (8) + aspect + utils | 45 文件 | 🟡 | ✅ 已交付 (历史/监控/加密三面) |
| ~~N-19~~ | ~~配置快照/备份~~ | **实证: 无独立备份面** (仅 Constants 引用) | — | — | **取消** (capacity 并入 N-16 治理面) |

### D. 核心服务端面 (core 264 文件 26,584 行 + consistency 23 + auth 28 — 拆 4 域)

| # | 域 | 源 | 规模 | 级别 | 状态 |
|:--:|:--|:--|:--:|:--:|:--:|
| N-20 | **一致性协议 (Distro/JRaft)** | core/distributed (51) + consistency (23) | 74 文件 8,000 行 | 🔴 | ✅ 已交付 (NC-5) |
| N-21 | **集群管理面** | core/cluster + core/namespace + core/member | 35 文件 | 🔴 | ⬜ 新增 (ServerMemberManager/健康选举) |
| N-22 | **远程服务端面** | core/remote (38: grpc 28 + control 5 + tls 5) | 38 文件 | 🔴 | ⬜ 新增 (GrpcRequestAcceptor/连接管理) |
| N-23 | **认证/权限/安全** | auth (28) + core 相关 | 35 文件 | 🟡 | ⬜ 新增 (ProtocolAuthService 深化) |

### E. 公共底座面 (common 212 文件 30,381 行 — 巨域拆 3 域)

| # | 域 | 源 | 规模 | 级别 | 状态 |
|:--:|:--|:--|:--:|:--:|:--:|
| N-24 | **事件中心 NotifyCenter** | common/notify + common/event | 15 文件 | 🔴 | ⬜ 新增 (发布/订阅/慢事件) |
| N-25 | **HTTP 客户端/工具族** | common/http (18) + common/utils + common/executor | 50 文件 | 🟡 | ⬜ 新增 |
| N-26 | **SPI/序列化/缓存/任务** | common/spi + common/serialize + common/cache + common/task | 40 文件 | 🟡 | ⬜ 新增 |

---

## 二、域划分依据 (逐包归属验证, 代表性)

| 归属决策 | 证据 |
|:--|:--|
| naming 巨域拆 6 | naming 271 文件 31,008 行: controllers 20 / core+v2 60 / healthcheck 35 / push 27 / consistency 10 / cluster+monitor 20 — 各子域 >10 文件独立成域 |
| config 巨域拆 5 | config 243 文件 36,743 行: service+model 141 (超巨再拆) / controller 19 / remote 15 / monitor 8 |
| core 拆 4 | core 264 文件: distributed 51 / remote 38 / cluster 35 / 其他 |
| common 拆 3 | common 212 文件 30,381 行: remote/client 22 (已归 N-03) / notify / http 18 / utils / executor / spi / serialize |
| api 归契约面 | api 274 文件: naming 52 + config 60 + remote 35 + annotation + exception — 接口与 DTO 不拆多域 |

## 三、执行序 (拓扑)

**已交付 (N-01~N-06 + N-20)** → N-07 (契约面, 客户端各域依赖) → N-08 → N-10 (客户端管理, 服务端入口依赖) → N-09 → N-11 → N-12 → N-13 → N-14 → N-15 → N-16 → N-17 → N-18 → N-19 → N-21 → N-22 → N-23 → N-24 → N-25 → N-26

> 拓扑理由: 契约面先行 (接口是理解实现的钥匙) → 客户端剩余 → 命名服务端 (控制器→客户端管理→健康→推送→一致性→集群) → 配置服务端 (存储→控制器→长轮询→审计) → 核心 (集群→远程→安全) → 公共底座 (事件中心→工具→SPI)

## 四、与 issue 规划 7 域的关系

| issue 7 域 | 新域清单 | 差距 |
|:--|:--|:--|
| NC-1 NamingService | N-01 (不变) | — |
| NC-2 ConfigService | N-02 (不变) | — |
| NC-3 gRPC+Redo | N-03 + N-04 (拆分) | 原合并两机制 |
| NC-4 缓存+故障+地址 | N-05 + N-06 (拆分) | 原合并 |
| NC-5 一致性 | N-20 (不变) | — |
| NC-6 服务端核心 | **N-09~N-19 共 11 域** | 原 1 域吞 3 个巨域 |
| NC-7 安全+监控+限流/加密 | N-08/N-18/N-23/N-25/N-26 分散 | 原横切合并 |

**差距本质**: issue 规划把 1640 文件 220K 行压成 7 域 (平均 31K 行/域), openjdk 标准下每域 5-30 文件。**服务端面 (naming/config/core) 是最大盲区** — 原 NC-6 一个域吞了 778 文件 94K 行。

## 五、完成检查单

- [x] 全量 23 模块扫描 (文件/行数实证)
- [x] 逐包归属验证 (naming/config/core/common/api 分布表)
- [x] 域划分 21 域 (9🔴+12🟡) + 巨域拆分子域
- [x] 与 issue 7 域差距分析
- [x] **N-07~N-26 逐域交付 (2026-08-16)**: 20 个新域全部完成 (大纲 26 篇 + 20 问 + 深审 + KP)
- [x] **N-19 取消**: 实证无独立备份面 (capacity 并入 N-16)
- [x] **09 审计表述修正**: ClientLongPolling 存在于 config 侧 (LongPollingService:264) — N-17 深审实证
- [x] **harness 扩充**: 3 → 10 个 (新增 N-10/11/12/15/17/21/24), **113/113 断言全 PASS**
- [x] 🎉 **Nacos 全量收官**: 原 7 域 + 域发现 20 域 = 27 域交付 (26 篇大纲 + 10 harness 113/113)
