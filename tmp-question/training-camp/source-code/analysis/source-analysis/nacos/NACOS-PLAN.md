# Nacos — 知识网络化规划 (NC-1~NC-7, 09 怀疑审计后 v1)

> **日期**: 2026-08-16 | **依据**: issue/源码分析执行计划.md 阶段5.8 (7 域, 1.x 语义过时) + issue/Nacos源码学习范围规划.md (3.0.3 基准, 7 域逐包扫描) + 09 对既有规划保持怀疑
> **源码**: `/data/workspace/source-code/code/spring/nacos` (**3.0.3**, pom.xml revision 实证; git 单提交 d14ae0ca "Develop release3.0.3"; 核心模块: client 120 + client-basic 51 + api 274 + config 243 + core 264 + consistency 23 = **975 主源** — 与规划逐包扫描完全一致)
> **定位**: 阶段 5.8 — RPC 与服务治理第七环 **注册中心/配置中心内核** (Spring Cloud Alibaba 5.7 的底层客户端)
> **知识网络**: 承接 ALI-A1/A2 (NacosConfigService 消费方) + ALI-A3 (NamingService 消费方) + ALI-A5 (subscribe/心跳) 的内核面; 对照 SofaJRaft 4.6 (JRaftProtocol 已学) + ZK 4.3/Curator 4.5 (注册中心语义对照)
> **分工确认**: 5.8 无人占用, 本会话开工 ✅

---

## 〇、09 怀疑审计表 (Nacos, 2026-08-16) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| 版本 3.0.3 | pom.xml revision | **3.0.3** 实证 + git d14ae0ca | **接受** ✅ |
| 文件数 120/51/274/243/264/23 | find 实测 | 六模块**逐数字一致** (975 主源) | **接受** ✅ |
| 域编号 NC-1~NC-7 | 双文档对照 | **执行计划 5.8 部分是 1.x 语义** (BeatReactor HTTP 心跳/LocalSnapshot/NacosServiceRegistryV2 **不存在于 3.0.3**); 但 **ClientLongPolling 存在于 config 服务端** (LongPollingService.java:264 内部类, N-17 深审实证) — 执行计划 NC-7 方向对, "30s 超时"精确化为 max(10s, 请求值-500ms) | **裁决** ⚠ 以详细规划为准, 编号 NC-1~NC-7 重定义; 09 审计"ClientLongPolling 不存在"表述修正 |
| 执行计划 "心跳 (BeatReactor)" | find 穷举 | 3.x 已用 **gRPC 长连接 + NamingGrpcClientProxy** 替代 (1.x→3.x 变化表实证) | **过时修正** ✅ |
| ConfigRpcTransportClient | find | **存在但为 ClientWorker 内部类** (ClientWorker.java:639) — 非独立文件 | **路径修正** ✅ |
| AuthManager/PermissionManager/UserManager (NC-7) | auth 模块扫描 | 3.x auth 重构为 **ProtocolAuthService 体系** (AbstractProtocolAuthService/GrpcProtocolAuthService/HttpProtocolAuthService, 28 文件) — 规划类名过时 | **修正** ⚠ |
| ClientMetrics (NC-7) | find | 实为 **ClientMetricsController** (config/server/controller/) — 类名编造 | **修正** ⚠ |
| MetricsMonitor | find | **3 处**: client/monitor + config/server/monitor + core/monitor — 规划只提 1 处 | **扩充** ✅ |
| 核心类穷举 (44+ 客户端面) | find 逐个 | NC-1~NC-3 全部存在; NC-4~NC-7 除上述 4 修正外全存在 | **接受+修正** ✅ |
| 淘汰清单 | find 实测 | console 79/istio 40/ai 31/k8s-sync 4/address 8/cmdb 9/lock 20/maintainer-client 26/mcp 6/persistence 36/prometheus 7/sys 25 — 与规划一致 (k8s-sync 规划 4 实际 4) | **接受** ✅ |

> 注: 按 09 铁律, 每个断言开工时独立 grep 复验, 数字/类名/行号一律以 3.0.3 源码为准。

---

## 一、入口点与主线 (待 Pass 0 确认)

`NacosNamingService.registerInstance/getAllInstances/subscribe → NamingClientProxyDelegate → NamingGrpcClientProxy → RpcClient (gRPC 长连接) → NamingGrpcRedoService (断线重做)` (注册发现主线) / `NacosConfigService.getConfig/addListener → ClientWorker → ConfigRpcTransportClient (内部类) → CacheData/ConfigChangeHandler` (配置主线) / `ConsistencyProtocol → APProtocol (Distro) / CPProtocol (SOFA-JRaft)` (一致性主线)

## 二、域清单 (7 域: 3🔴 + 4🟡, 详细规划分级)

| # | 域 | 模块 | 核心主题 | 方案 |
|:--:|---|---|---|---|
| 🔴 NC-1 | **NamingService 注册发现** | client/naming + core | NacosNamingService 多重载 / NamingClientProxyDelegate 双代理 / gRPC 订阅 / ServiceInfoHolder 差异计算 / ProtectMode 保护 | 🔴 A |
| 🔴 NC-2 | **ConfigService 配置客户端** | client/config + api/config | getConfig 链路 / ClientWorker+ConfigRpcTransportClient 内部类 / CacheData / CAS 发布 / 注解族 | 🔴 A |
| 🔴 NC-3 | **gRPC 通信 + Redo 重做** | client/naming/remote + client-basic/remote + api/remote | RpcClient 工厂 / GrpcConnection / NamingGrpcRedoService 三 RedoData / 请求协议族 | 🔴 A |
| 🟡 NC-4 | **本地缓存+故障转移+地址管理** | client/naming/cache+backups + client-basic/address | DiskCache / FailoverReactor 体系 / ServerListProvider 三实现 | 🟡 B |
| 🟡 NC-5 | **一致性协议 Distro+JRaft** | consistency + core/distributed | APProtocol Distro / CPProtocol SOFA-JRaft / 选型 | 🟡 B |
| 🟡 NC-6 | **服务端核心** | naming + config/server + core/cluster | 服务端架构 / InstanceController / 健康检查 / push | 🟡 B |
| 🟡 NC-7 | **安全+监控+限流/加密** | auth + client/security + client/config/impl | ProtocolAuthService 体系 / MetricsMonitor×3 / Limiter / 加密 | 🟡 B |

## 三、执行顺序 (拓扑: 客户端 SDK → 通信内核 → 服务端)

**NC-1 → NC-2 → NC-3 → NC-4 → NC-5 → NC-6 → NC-7**

> 拓扑理由: 客户端两主线 (注册发现 NC-1 → 配置 NC-2) → 通信内核 NC-3 (两主线共用 gRPC/Redo 底座) → 容灾缓存 NC-4 (客户端兜底) → 服务端一致性 NC-5 (协议面) → 服务端整体 NC-6 → 横切面 NC-7 (安全/监控收束)。

## 四、知识网络图

```
← 承接: ALI-A1/A2 (NacosConfigService 消费方实证) + ALI-A3 (NamingService 消费方实证) + ALI-A5 (subscribe/心跳 内核面)
→ 引出: Sentinel 5.9 (下一阶段)
对照: SofaJRaft 4.6 (JRaftProtocol 内核已学, 本阶段 CP 面复用) + ZK 4.3/Curator 4.5 (注册中心语义对照) + 执行计划 5.8 1.x 语义已过时
```

## 五、完成检查单

- [x] 模块扫描 ↔ 域覆盖矩阵 (7 域, 六核心模块 975 主源实证; 淘汰 14 项清单验证)
- [x] 09 审计: 版本实证 / 双文档编号裁决 (1.x 过时) / 核心类穷举 (4 修正) / MetricsMonitor×3 扩充
- [x] **NC-1 ✅ 2026-08-16** (方案 A: 大纲 8 节/20+ 锚点/20 问 + 深审 6 项 5 修正 + 时空溯源 (1.x→3.x 演进) + **harness 18/18 能力路由实证 (自抓路由语义缺陷)** + KP; 核心: 重载漏斗/双代理能力路由/redo 前置订阅/差异事件缓存/发现三路/本地注册门控)
- [x] **NC-2 ✅ 2026-08-16** (方案 A: 大纲 7 节/20+ 锚点/20 问 + 深审 6 项 6 修正 + 时空溯源 (HTTP 轮询→gRPC 唤醒) + **harness 11/11 三路容灾实证 (自抓 null 不落 snapshot 语义)** + KP; 核心: 三路读取/CAS 发布/COW 缓存/信号量唤醒/通知五步/SPI 解析器)
- [x] **NC-3 ✅ 2026-08-16** (方案 A: 大纲 7 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + 时空溯源 (短连接→长连接自愈) + **harness 18/18 四态机实证** + KP; 核心: RpcClient 双队列/状态机/RedoData 四态/双 Map 仓库/定时补发/泛型基类; **09 审计路径修正 (内核在 common 非 client-basic)**)
- [x] **NC-4 ✅ 2026-08-16** (方案 B: 大纲 6 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + KP; 核心: FailoverReactor 5 秒开关三态/SPI 数据源/ServerListProvider 族/磁盘缓存/配置容灾文件)
- [x] **NC-5 ✅ 2026-08-16** (方案 B: 大纲 6 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + KP; 核心: ConsistencyProtocol 统一契约/Distro 组件注册面+任务链/SOFA-JRaft 集成面/状态机闭包/读写分派)
- [x] **NC-6 ✅ 2026-08-16** (方案 B: 大纲 6 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + KP; 核心: 三层入口链/HealthCheckReactor 去重调度/UDP+gRPC 双推送/ephemeral↔persistent 双存储路由/配置服务端)
- [x] **NC-7 ✅ 2026-08-16** (方案 B: 大纲 6 节/20+ 锚点/20 问 + 深审 6 项 4 修正 + KP; 核心: ProtocolAuthService 族 (09 审计修正)/SecurityProxy 上下文注入/Limiter 5 QPS/加密键容灾/监控三处)
- [x] **二轮 REVIEW (2026-08-16)**: 锚点抽查 7 项全精确 + 1 漂移修正 (Limiter L26→L47-49) + 文字锚 22 处清零 + harness 47/47 回归
- [x] 🎉 **阶段 5.8 Nacos 7/7 全量收官 (2026-08-16)**: 3 harness 47/47 断言全 PASS · 140 问 (7×20) · 交付 NACOS-PLAN + 7 outline + 7 KP + 3 时空溯源 + 3 harness
