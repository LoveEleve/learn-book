# HANDOFF — Nacos 源码分析交接文档 (7/7 全量收官)

> **日期**: 2026-08-16 | **版本**: 3.0.3 (pom.xml revision 实证) | 模块: 六核心 client 120 + client-basic 51 + api 274 + config 243 + core 264 + consistency 23 = **975 主源**
> **给新 AI**: 本文是 Nacos 阶段的**唯一入口**。**25 域全量交付** (原 issue 7 域 + 域发现 18 域; 25 outline 目录 + 26 篇大纲 + 20 问 + 六层深审 + 时空溯源 🔴 + **10 个 harness 113/113** + REVIEW)。
> **源码**: `/data/workspace/source-code/code/spring/nacos` (git 单提交 d14ae0ca "Develop release3.0.3")
> **规划**: NACOS-PLAN.md (09 审计 v1 + 二轮 REVIEW)
> **分工确认**: Nacos 无人占用, 本会话开工 7/7 收官 ✅

---

## §零 状态速查 (2026-08-16, 7/7 收官)

| 域 | 模块 | 级别 | 方案 | 大纲节 | questions | harness | 时空溯源 | REVIEW 修正 |
|:--:|:--|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| NC-1 NamingService 注册发现 | client/naming | 🔴 | A | 8 | 20 | **18/18** | ✅ 1.x→3.x | 深审 5 |
| NC-2 ConfigService 配置客户端 | client/config | 🔴 | A | 7 | 20 | **11/11** | ✅ 轮询→唤醒 | 深审 6 |
| NC-3 gRPC 通信+Redo | common/remote + client/redo | 🔴 | A | 7 | 20 | **18/18** | ✅ 短连→长连自愈 | 深审 4 |
| NC-4 缓存+故障转移+地址 | client/backups+cache+address | 🟡 | B | 6 | 20 | — | — | 深审 4 |
| NC-5 一致性 Distro+JRaft | consistency + core/distributed | 🟡 | B | 6 | 20 | — | — | 深审 4 |
| NC-6 服务端核心 | naming + config/server | 🟡 | B | 6 | 20 | — | — | 深审 4 |
| NC-7 安全+监控+限流/加密 | auth + client/security | 🟡 | B | 6 | 20 | — | — | 深审 4 |

**统计**: **25 域全交付** (原 7 + 域发现 18, N-19 实证取消) · **10 个 harness 113/113 断言全 PASS** · 500 问 (25×20) · 26 篇大纲 (巨域拆多篇) · 深审修正 60+ 处 · 六轮 REVIEW (编造类名 2/伪锚点 22/文字锚 185/问数 11 域全修)

**执行序**: NC-1 → NC-2 → NC-3 → NC-4 → NC-5 → NC-6 → NC-7 (客户端两主线 → 通信内核 → 容灾 → 服务端)

---

## §一 7 域核心知识速查 (全量固化)

### NC-1 NamingService 注册发现 (🔴, client/naming)

**核心机制**: 门面漏斗 + 双代理路由 + 订阅缓存
- **重载漏斗** (NacosNamingService.java:140-175): 6 重载收敛到 registerInstance(s,g,i)
- **双代理能力路由** (Delegate:197-203): **`ephemeral || grpcSupported → gRPC`** — 持久实例在服务端支持时也走 gRPC, 仅老服务端回退 HTTP (harness 自抓语义修正)
- **订阅三连** (NamingGrpcClientProxy:392-410): cacheRedo → doSubscribe → registered — **redo 前置**
- **推送四步** (ServiceInfoHolder:124-164): 空推送忽略 → put → 差异 → 事件+落盘
- **发现三路** (NacosNamingService:351-376): failover > 订阅缓存 > 直查; selectInstances 过滤 (healthy/enabled/weight)
- **订阅门控** (L528-539): 无监听器才 unsubscribe
- **harness MiniNaming 18/18**: 能力路由/diff 四态/空推送/门控

### NC-2 ConfigService 配置客户端 (🔴, client/config)

**核心机制**: 三路读取 + 唤醒监听 + 双粒度回调
- **三路容灾** (NacosConfigService:206-261): **failover(用户) > server(主) > snapshot(仅异常兜底 — 服务端返回 null 不落 snapshot, harness 自抓)**; 每路过加密过滤链
- **CAS 发布** (L273-290): publishConfigCas (casMd5 乐观锁)
- **COW 缓存** (ClientWorker:134/381-402): AtomicReference + 双检
- **信号量内核** (ClientWorker:639-650): **ConfigRpcTransportClient 是内部类** + listenExecutebell + 3 分钟全量同步
- **通知五步** (CacheData:430-500): fillContext → ClassLoader 切换 → 过滤 → 回调 → (变更监听器) parseChangeData + ConfigChangeEvent
- **SPI 解析器** (ConfigChangeHandler:46-73): ConfigChangeParser SPI + Properties/Yml 内置
- **harness MiniConfig 11/11**: 三路/filter 链/COW/md5 门控/变更解析

### NC-3 gRPC 通信+Redo (🔴, common/remote + client/redo)

**核心机制**: 长连接内核 + 断线自愈
- **09 审计路径修正**: RpcClient 内核在 **common/remote/client** (4368 行), 规划声称的 client-basic/remote 只有 HttpClientManager
- **双队列** (RpcClient:78-86/242-310): 事件队列 + 重连信号量; keepAlive 超时 → healthCheck → UNHEALTHY → 重连
- **RedoData 四态机** (RedoData.java:97-113): 三布尔 (expectedRegistered/registered/unregistering) → NONE/REGISTER/UNREGISTER/REMOVE — **注释语义表 (L100-110) 即规格**
- **双 Map 仓库** (NamingGrpcRedoService:60-68): instances + subscribes 隔离; onDisConnect 全标记 (L100-113)
- **定时补发** (RedoScheduledTask:27-66): connected 门控 + switch 分派
- **泛型基类** (AbstractRedoService:42): config 面复用铺路 (TODO 注释 L48)
- **harness MiniRedo 18/18**: 四态机/断线标记/门控/移除条件

### NC-4 本地缓存+故障转移+地址管理 (🟡, client/backups+cache+address)

**核心机制**: 运维开关容灾 + Provider 地址族
- **FailoverReactor 5 秒开关** (L87-89): FailoverSwitchRefresher 三态 (null→关 / 开→加载+差异事件 / 关→回主缓存)
- **SPI 数据源** (L72-77): FailoverDataSource 默认无实现 (用户注入)
- **地址 Provider 族** (AbstractServerListManager:41-58): order 降序 + match 首个; Endpoint 30s 定时 / Properties 静态
- **磁盘缓存** (DiskCache:54-105): ConcurrentDiskUtil 写 + 目录解析读
- **配置容灾** (LocalConfigInfoProcessor:68/85): failover(用户)/snapshot(自动) 文件三级路径

### NC-5 一致性 Distro+SOFA-JRaft (🟡, consistency+core/distributed)

**核心机制**: AP/CP 双轨
- **统一契约** (ConsistencyProtocol:40): init/读写/onRequest; APProtocol/CPProtocol 分叉
- **Distro AP** (DistroProtocol:44-88): 组件 Holder (resourceType 注册) + Load/Verify 任务链 + onReceive 分派
- **SOFA-JRaft 集成非自研** (JRaftProtocol:93-122 + JRaftServer:105): init → start; commit → applyOperation; 架构图在类注释 (L60-90)
- **读写分派**: NacosWrite (提交) / NacosRead (状态机直读)

### NC-6 服务端核心 (🟡, naming+config/server)

**核心机制**: 三层入口 + 双推送 + 双存储
- **三层链** (InstanceController:90/127 → InstanceOperatorClientImpl:73 → ClientServiceImpl:57): 入口/业务/客户端管理; 3.x clientId 模型
- **健康调度** (HealthCheckReactor:36-77): futureMap 去重 + BeatCheckTask 5s + v2 任务
- **双推送** (UdpPushService:49 遗留 + push/v2 gRPC): 订阅服务本地/聚合双实现
- **双存储路由** (consistency/ephemeral→Distro / persistent→JRaft): AP/CP 落点实证
- **TcpSuperSenseProcessor 已不存在** (执行计划 1.x 语义过时)

### NC-7 安全+监控+限流/加密 (🟡, auth+client/security)

**核心机制**: 协议认证 + 回调限流 + 键容灾
- **ProtocolAuthService 族** (auth/ProtocolAuthService.java:33): 09 审计修正 (AuthManager 等 1.x 类不存在); gRPC/HTTP 双实现 + ResourceParser 族
- **客户端代理** (SecurityProxy:45/78/95): login + LoginIdentityContext 注入 header
- **回调限流** (Limiter:33/47-49): Guava RateLimiter 缓存 (1000/1min) + 默认 5 QPS + limitTime 可配
- **加密键容灾** (LocalEncryptedDataKeyProcessor:38/60/79): extends LocalConfigInfoProcessor — failover/snapshot 双路
- **监控三处** (09 审计扩充): client/config/core 分层 + RAFT 指标族

---

## §二 方法论执行报告

### 09 怀疑审计修正汇总 (NACOS-PLAN)

| 修正 | 证据 | 落点 |
|:--|:--|:--|
| 版本 3.0.3 | pom.xml revision + git d14ae0ca | ✅ |
| 文件数 975 | 六模块逐数字一致 | ✅ |
| **双文档编号裁决** | 执行计划 5.8 是 1.x 语义 (BeatReactor/LocalSnapshot/ClientLongPolling/NacosServiceRegistryV2 全不存在) | 以详细规划为准 |
| **gRPC 内核路径** | RpcClient 等全在 **common/remote/client** (4368 行), 非 client-basic/remote (仅 HttpClientManager) | NC-3 |
| ConfigRpcTransportClient | 是 **ClientWorker 内部类** (L639) 非独立文件 | NC-2 |
| AuthManager 等 3 类 | 3.x 重构为 ProtocolAuthService 体系 | NC-7 |
| ClientMetrics | 实为 ClientMetricsController | NC-7 |
| MetricsMonitor | **3 处** (client/config/core) 非 1 处 | NC-7 |
| TcpSuperSenseProcessor | 3.x 不存在 — healthcheck 重构 | NC-6 |

### 各域深审发现 (代表性)

| 域 | 最重大发现 |
|:--|:--|
| NC-1 | 能力路由非"ephemeral=grpc/persistent=http" — 持久实例在服务端支持时也走 gRPC (harness 自抓) |
| NC-2 | **server 返回 null 不落 snapshot** — 仅异常 (NacosException) 才兜底 (harness 自抓) |
| NC-3 | RedoData 三布尔四组合语义表; onDisConnect 标记而非 onConnected 清空 |
| NC-4 | FailoverDataSource 是 SPI 默认无实现; 5 秒开关轮询 |
| NC-5 | JRaft 是 SOFA-JRaft 集成非自研; 架构图在类注释 |
| NC-6 | 双推送 (UDP 遗留+gRPC); ephemeral/persistent 双存储路由 |
| NC-7 | Limiter 默认 5 QPS; 加密键独立容灾 |

### harness 自抓缺陷汇总 (3 个, 每域 1-2)

- NC-1: 能力路由语义 (ephemeral||ability 短路)
- NC-2: 服务端 null 不落 snapshot / 解析器差异比较
- NC-3: 移除条件 (expectedRegistered) 两段实证

---

## §三 高频坑汇总 (跨域 21 条)

### 注册发现 (NC-1/NC-3)
1. 持久实例也走 gRPC — 能力路由不是"持久=HTTP"
2. subscribe 先 cacheRedo 后发送
3. 空推送被忽略 (pushEmptyProtection 可控)
4. 无监听器才真正 unsubscribe
5. doDiff 过期数据静默忽略
6. RedoData 三布尔四组合 — registered+unregistering → UNREGISTER 非 REMOVE
7. remove 只在 !expectedRegistered 时执行
8. 断线时 redo 任务跳过 (connected 门控)

### 配置 (NC-2)
9. server 返回 null 不落 snapshot — 只有异常才兜底
10. ConfigRpcTransportClient 是内部类非独立文件
11. 通知链先切 ClassLoader 再回调
12. 变更明细只给 AbstractConfigChangeListener
13. 3 分钟全量同步兜底
14. failover 文件是用户手动维护

### 容灾/服务端 (NC-4/NC-5/NC-6)
15. FailoverDataSource 默认无实现 — 用户 SPI 注入
16. failover 开关 5 秒才刷新
17. JRaft 是集成非自研
18. ephemeral→Distro / persistent→JRaft
19. TcpSuperSenseProcessor 已不存在 (1.x)
20. UDP 推送是遗留, 主线 gRPC

### 横切 (NC-7)
21. Limiter 默认 5 QPS / authEnabled 主开关

---

## §四 文件路径

```
analysis/source-analysis/nacos/
├── NACOS-PLAN.md                    ← 09 审计 v1 + 二轮 REVIEW (权威规划)
├── NACOS-DOMAIN-DISCOVERY.md        ← 域发现 v1 (对标 openjdk 48 域方法论, 21 域清单)
├── HANDOFF-NACOS.md                 ← 本文 (27 域收官唯一入口)
├── outlines/ (27 域, 26 篇大纲)
│   ├── nc1-naming/ ... nc7-auth-monitor/   (原 issue 7 域)
│   ├── n07-api-contract/ (3 篇)  n08-client-foundation/ (2 篇)
│   ├── n09-naming-controllers/  n10-client-management/
│   ├── n11-healthcheck/  n12-push/  n13-consistency-apply/
│   ├── n14-naming-cluster-monitor/
│   ├── n15-config-storage/ (3 篇)  n16-config-controllers/
│   ├── n17-config-remote/  n18-config-observability/
│   ├── n21-cluster/  n22-remote-server/  n23-auth/
│   ├── n24-notifycenter/  n25-http-utils/  n26-common-base/
│   └── (每域: 大纲 + completeness-questions.md (20 问) + review-notes.md (+temporal-trace.md 🔴))
├── knowledge-planning/ (25 个 KP 文件)
├── harness/ (10 个 🔴 域, 113/113)
│   ├── nc1-naming/MiniNaming.java (18/18)   nc2-config/MiniConfig.java (11/11)
│   ├── nc3-grpc-redo/MiniRedo.java (18/18)  n10-client-management/MiniClientManager.java (9/9)
│   ├── n11-healthcheck/MiniHealthCheck.java (11/11)  n12-push/MiniPush.java (7/7)
│   ├── n15-config-storage/MiniConfigStorage.java (11/11)  n17-config-remote/MiniConfigRemote.java (10/10)
│   ├── n21-cluster/MiniCluster.java (11/11)  n24-notifycenter/MiniNotify.java (7/7)
源码: /data/workspace/source-code/code/spring/nacos/ (3.0.3)
上级: ../HANDOFF-STAGE5.md (阶段 5 唯一总入口) — 5.8 已标注 ✅ 25/25 收官
后续: 阶段 5.9 Sentinel (10 域) — 需确认无其他 AI
```

---

## §五 完成检查单

- [x] 7/7 域全量交付 (2026-08-16): §零 状态表 + §一 7 域速查 + §三 21 坑
- [x] 每域: 大纲 + 20 问 + 六层深审 + 时空溯源 🔴 (3 个) + harness 🔴 (3 个 47/47) + REVIEW
- [x] 09 审计: 版本实证 + 双文档编号裁决 (1.x 过时) + 核心类穷举 (4 修正) + 路径大修正 (common/remote)
- [x] 二轮 REVIEW: 锚点全量复验 (文件+行号 0 缺失) + 文字锚清零 (22 处) + Limiter 漂移修正 (L26→L47-49)
- [x] 下一步: 阶段 5.9 Sentinel (空闲, 需先确认无其他 AI)

---

## §六 交接 REVIEW (2026-08-16, 文档发布前)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点抽查 | 7 项全精确 (onEvent L139 / registerService L174 / Distro onReceive / Failover 5s L88 / 三路 L220 / RedoData 语义表 L104-113 / onDisConnect L100) | 通过 ✅ |
| 2 | 锚点漂移 | Limiter "qps 5" 原写 L26 → 实证 **L47-49** | **已修正** NC-7 三文件 |
| 3 | 目录整洁 | 0 .class 残留; harness 3 个全编译回归 47/47 | 通过 ✅ |
| 4 | 结构完整性 | 7 outline / 7 KP / 3 harness / 3 时空溯源与文档一致 | 通过 ✅ |
| 5 | 文字锚 | 22 处 `Xxx.java` 无行号 → 全部补行号 (grep 实证) | 清零 ✅ |
| 6 | **六轮 REVIEW (深度查漏, 用户质疑驱动)** | ① **编造类名 2 处修正**: `ClientOperationServiceImpl` (实为接口 ClientOperationService:36 + Ephemeral/Persistent 双实现) / `ConfigChainRequestExtractor` (实为 ConfigQueryChainRequestExtractor:29) ② **伪锚点 22 处清零** (文件总行数误写为行号锚, 内容均为 `}`) ③ **文字锚 185 处批量补类声明行号** ④ **11 个域 20 问不完整** (8 域 13 问 + 3 域 18 问) → 全量重排 20/20 ⑤ 区间锚点修正 (NC-1 doSubscribe 486-528/doUnsubscribe 530-539) ⑥ extrnal 拼写加注 ⑦ 前置/引出引用全有效 ⑧ harness 113/113 回归 | 通过 ✅ |

> 注: 接手后运行 harness 需先 javac 编译 (源码在 harness/*/Mini*.java, class 不保留)
