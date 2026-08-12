# stage-4 · 第 19 节：第十六节：Redis Server 多活架构 — 知识点提取

> 课程：stage-4 多活架构 第 19 节（数据面组 16-19 收官）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/19. 第十六节：Redis Server 多活架构.md`
> 提取时间：2026-08-12 | 权重：核心（Redis Server 高可用——Sentinel 部署/Cluster/多区域复制；my-xhs 主从哨兵实证）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**Redis 主题（非 Eureka 文档）——机制 = Sentinel 高可用 + Cluster 分片 + 多区域复制**

> **文档形态**：**最长篇（538 行，代码块 8 个——awk 计数实证）**——主要内容 4 条：①Redis Sentinel 高可用部署（**正文主体——docs:38-531**）②Redis Cluster Data Sharding（**自学链接 docs:534**）③集群多区域数据复制（**意图——正文无**）④**SCG 作为 Redis Proxy** + AZ Locator（**TODO docs:537-538**）；**Sentinel 配置全文（docs:148-517）为配置文档主体——配置要点提取而非全文照录**。

---

## 一、本节概览

- **技术域**：Redis Sentinel（分布式系统/特性/部署前提/实战配置）、Redis Cluster（分片——自学）、多区域复制（意图）、SCG Redis Proxy（TODO）
- **维度**：`[分布式问题]`（高可用/故障转移）+ `[工程问题]`（部署/配置）+ `[分布式理论]`（CAP/一致性）
- **核心命题**：**Redis Server 的高可用与多区域形态**——docs 四主线：①Sentinel（**防单点的分布式故障转移系统**）②Cluster（分片——链接）③多区域复制（意图）④SCG 作为 Proxy（TODO——故障转移/读写分离/AZ Locator）；**知识本体 = Sentinel 高可用机制 + Redis 多活形态全景**
- **知识点数**：6 个
- **前置**：stage-2-28（Redis 实战）、01 篇（多活规划）、stage-2-01（CAP）

## 前置条件清单
读者需先掌握：
1. **Redis 基础**（stage-2-27/28——缓存/实战）
2. **CAP/一致性**（stage-2-01——P 必选）
3. **多活规划**（01 篇——多区域衔接）
4. **SCG**（14 篇——Proxy 整合基础）
未达前置者，先补：stage-2-28 / stage-2-01

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **配置要点提取**：538 行配置全文 → 关键配置项（monitor quorum/auth/超时/并行同步/故障超时）——非全文照录
- **发散为本**：Cluster/多区域复制/SCG Proxy（docs 链接/意图/TODO——发散补全）
- **实例对照**：my-xhs 主从哨兵 3 实例（docker-compose 实证）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 有状态服务一致性（CAP P 必选 + 1 Source N Replicas）【docs §有状态服务/§CAP/§1 Source N Replicas】
- **维度**：`[分布式理论]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：stage-2-01（CAP）
- **来源**：docs §有状态服务数据一致性保障（docs:17-35）+ §OSS V.S. Enterprise（docs:11-14）
- **需求**：**有状态服务的一致性与高可用框架**——docs 明确：DB/ES/Redis 多存储媒介 Cluster（docs:18）；**CAP 模型（P 必选——C/A 二选一偏好**——docs:21-22）；**1 Source N Replicas 架构**（数据复制模型——**强一致协议 Raft/ZAB + 弱一致性 Gossip/Broadcast/Replication**——docs:28-29；高可用：Cluster 高可用 + Source 高可用（**选主算法**——docs:33-35））
- **自主实现**：若我设计——有状态服务高可用框架：**复制模型**（强一致 Raft/ZAB vs 弱一致 Gossip——按一致性诉求选）+ **高可用两面**（Cluster 级 + Source 级（选主））
- **参考实现**（docs 照录 + stage-2-01 交叉）：**CAP（docs:21-22 照录）**——P 必选 + C/A 偏好（**stage-2-01 已深挖——交叉不重提**）；**复制协议谱（docs:28-29 照录）**——强一致（Raft/ZAB——stage-2 03/04 已提取）/弱一致（Gossip/Broadcast/Replication——**Redis 主从复制即弱一致面**）；**高可用两面（docs:33-35 照录）**——Cluster 高可用 + Source 高可用（**选主算法——本篇 Sentinel 即 Redis 的选主实现**）；**OSS vs Enterprise（docs:11-14 照录）**——开源→企业付费（MySQL/ES/Redis/Oracle JDK——**Broker 或存储更容易赚钱**——docs:12——商业生态背景）+ Alibaba Open Message/OpenSergo（docs:14）
- **对比取舍**：**强一致（Raft/ZAB——可用性低）vs 弱一致（Gossip/Replication——可用性高）**——一致性 vs 可用性——**Redis 主从复制选弱一致（异步——吞吐）**（docs:28 谱系定位）
- **机制/说明**：有状态服务高可用 = **"复制模型 × 高可用面"的矩阵**——复制（强/弱协议）+ 高可用（Cluster/Source 选主）——**本篇 Sentinel = Redis 的 Source 高可用实现（选主算法）**
- **测试佐证**：docs:11-35（照录）+ stage-2-01/03/04（交叉）

### KP-02 Redis Sentinel 高可用架构（分布式系统 2 优点 + 特性 4 条 + 部署前提 5 条）【docs §Sentinel 高可用架构】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01、stage-2-28（Redis）
- **来源**：docs §Redis Sentinel 高可用架构（docs:38-62）
- **需求**：**Sentinel 的高可用机制**——docs 明确：**Sentinel 是分布式系统**（docs:39——多进程协作）——**2 优点**（①**多个 Sentinel 一致认为 master 不可用才故障检测——降低误报**（docs:41）②**部分 Sentinel 工作即可——防 SPOF**（docs:42——"拥有单点故障的故障转移系统没什么乐趣"））；**特性 4 条**（监控/通知/自动故障转移/配置提供者——docs:48-51）；**部署前提 5 条**（至少 3 实例/独立故障（不同可用区）/异步复制不保证已确认写入（docs:58）/Client 需 Sentinel 支持/必须测试（docs:56-60））
- **自主实现**：若我设计——Sentinel 高可用核心：**多数派一致性判定**（防误报——多 Sentinel 一致才判定）+ **自身无单点**（多实例协作）+ **配置提供者**（客户端查主地址——故障转移后报告新地址）
- **参考实现**（docs 照录 + 发散）：**分布式设计（docs:39-44 照录）**——多 Sentinel 协作（docs:39）+ 2 优点（docs:41-42——**一致判定防误报/部分工作防 SPOF**）；**特性 4 条（docs:48-51 照录）**——监控/通知/自动故障转移/配置提供者（**客户端服务发现的权威来源——docs:51——故障转移后报告新地址**）；**部署前提 5 条（docs:56-60 照录）**——3 实例/独立故障（**不同可用区**——docs:57——区域语义）/异步复制不保证已确认写入（docs:58——**RPO 非零**）/Client 支持/测试（docs:60——**"凌晨 3 点 master 停止工作时才发现的错误配置"**——docs 原文）；**链接（docs:62）**——redis.io/docs/management/sentinel `[无本地源码：外部文档]`
- **对比取舍**：**多数派一致判定（防误报）vs 单 Sentinel 判定（快但误报）**——准确性 vs 简单——**docs 选多数派（2 优点首条）**；**哨兵自身无单点 vs 哨兵单点**——防 SPOF 是设计红线（docs:42）
- **机制/说明**：Sentinel 高可用 = **"故障转移系统的自身高可用"**——多数派（防误报）+ 无单点（自身可靠）+ 配置提供（客户端寻址）——**故障转移（failover）+ 配置发现（discovery）双职责**（docs:50-51）
- **测试佐证**：docs:38-62（照录）+ `[无本地源码：外部文档]`

### KP-03 Sentinel 实战（Docker 1 主 2 从 + 配置要点 + 3 Sentinel 启动）【docs §实战】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §Redis Sentinel 实战（docs:65-531——Docker 部署 + 配置全文 + 启动）
- **需求**：**Sentinel 的工程部署**——docs 完整实战：**1 Master 2 Replicas**（Docker——requirepass + slaveof + masterauth——docs:72-112）+ **验证**（info Replication——role:master/connected_Replicas:2/两 slave 在线——docs:122-136）+ **sentinel.conf 配置**（docs:148-517——**关键配置项**）+ **启动 3 Sentinel**（docs:522-530）
- **自主实现**：若我设计——Sentinel 部署：**1 主 2 从（复制面）+ 3 哨兵（判定面）**——配置要点：monitor（quorum 判定）/auth（认证）/down-after（主观下线）/parallel-syncs（并行同步）/failover-timeout（故障超时）
- **参考实现**（docs 配置要点提取——非全文照录）：**部署拓扑（docs:69-112 照录要点）**——Master（6300——requirepass 123456——`config set masterauth`——docs:72-82）/Replica 1/2（6301/6302——`slaveof 192.168.0.114 6300` + masterauth——docs:86-112）；**验证（docs:122-136 照录要点）**——`info Replication`：role:master/connected_Replicas:2/slave0+slave1 state:online/master_replid/repl_backlog（**复制状态字段**——docs:124-136）；**sentinel.conf 关键配置（docs:148-517 提取——配置要点非全文）**——`protected-mode no`（docs:154）；**`[跳过：docs:283-338 ACL/auth-user 等安全配置细节——ACL 为 Redis 6.2+ 安全面（机制要点已覆盖 auth-pass——穷尽性标注）]`**/`port 26379`（docs:158）/`sentinel monitor mymaster 127.0.0.1 6379 2`（**quorum=2——O_DOWN 判定——docs:240——"quorum 只是 O_DOWN 条件，failover 需 Sentinel 多数派选举"——docs:228-230**）/`auth-pass`（docs:242-249）/`down-after-milliseconds 30000`（**S_DOWN 主观下线阈值——docs:273-281**）/`parallel-syncs 1`（docs:350-356——**故障转移期间副本并行同步数**）/`failover-timeout 180000`（docs:358-381——**故障转移超时多用途**）/`deny-scripts-reconfig yes`（docs:454——安全——**防客户端改脚本触发故障转移**）/`resolve-hostnames`（docs:483-499——DNS 支持）；**实战配置（docs:511-515）**——`SENTINEL MONITOR redis-master 192.168.0.114 6300 2` + auth-pass + daemonize；**启动（docs:522-530 照录要点）**——3 个 Sentinel Docker 容器（26379/26380/26381——**配置文件挂载**）+ `redis-sentinel /etc/redis/sentinel.conf`
- **对比取舍**：**配置要点提取（机制学习）vs 全文照录（操作手册）**——**538 行配置文档按配置要点提取**（08 SOP：知识本体是机制——monitor quorum/auth/超时/同步参数）
- **机制/说明**：Sentinel 配置机制核心 = **quorum（O_DOWN 判定）+ 多数派（failover 选举）双层判定**（docs:228-230——quorum 只是主观确认，failover 需多数派——**与 02 篇自我保护/多数派同思想**）；**parallel-syncs/failover-timeout** 是故障转移的节奏控制
- **测试佐证**：docs:65-531（配置要点提取——非全文）+ my-xhs（主从哨兵实证——KP-06）

### KP-04 Redis Cluster 与多区域复制（自学链接 + 意图发散）【docs §Cluster/主要内容③】
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：Medium
- **前置**：KP-01、stage-2-27（Redis Cluster 概念——16384 槽位）
- **来源**：docs §Redis Cluster 架构（docs:533-534——**自学链接**）+ 主要内容③（docs:5——多区域数据复制意图）
- **需求**：**Redis Cluster 与多区域复制**——docs 明确：Cluster 架构**自学**（docs:534——redis.io/docs/management/scaling 链接 `[无本地源码：外部文档]`）；**集群及 Cluster 集群多区域数据复制**（docs:5——意图——正文无）
- **自主实现**：若我设计——Redis 多活形态：**Cluster（分片——数据分布）+ 多区域复制（跨区同步——Cluster 间复制/命令转发）**
- **参考实现**（docs 链接/意图 + stage-2-27 交叉 + 发散）：**Cluster（docs:534 链接）**——stage-2-27 已提取（**16384 槽位/主从/Gossip——交叉不重提**）；**多区域复制（docs:5 意图——发散）**——Redis 跨区方案：**①Cluster 间复制**（跨区主从——异步）②**客户端侧命令转发**（18 篇——写入事件化管道——docs 18 的 Replicator 即此）③**Proxy 层**（docs 19 的 SCG Proxy——KP-05）——**三形态对照**
- **对比取舍**：**Cluster（单区域分片——扩展性）vs 多区域复制（跨区——容灾）**——不同目的（扩展 vs 容灾）——**两者可组合（Cluster 分片 + 跨区复制）**
- **机制/说明**：Redis 多区域形态 = **"分片（Cluster）叠复复制（跨区）"**——单区域扩展（槽位）+ 跨区容灾（复制/转发）——**docs 的 18/19 篇合起来是 Redis 多活全景**（客户端侧命令管道 + 服务端侧 Sentinel/Cluster）
- **测试佐证**：docs:533-534（链接照录）+ stage-2-27（Cluster 交叉）+ 发散标注

### KP-05 SCG 作为 Redis Proxy（TODO——故障转移/读写分离/AZ Locator 整合意图）【docs §SCG Redis Proxy】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：14 篇（SCG）、07 篇（AZ Locator）
- **来源**：docs 主要内容④（docs:6）+ §SCG 作为 Redis Proxy（docs:537-538——**TODO 后面部分统一实现**）
- **需求**：**SCG 作为 Redis Proxy**——docs 明确：基于 SCG 作为 Redis Proxy，整合 AZ Locator——**故障转移 + 读写分离**（docs:6）；**TODO 后面部分统一实现**（docs:537-538）
- **自主实现**：若我设计——Redis Proxy 模式：**网关层代理 Redis 流量**（统一入口——认证/路由/读写分离/区域偏好）——SCG 承担 Redis 协议代理（对比业务应用直连 Redis）
- **参考实现**（docs 意图照录 + 发散 + 18 篇衔接）：**docs 意图（docs:6 照录）**——SCG Redis Proxy + AZ Locator（故障转移/读写分离）；**TODO（docs:537-538 照录）**——`[空节标注：docs 未实现——后面部分统一实现]`；**机制发散——Proxy 模式的 Redis 多活**——①**统一入口**（Redis 流量过网关——认证/治理集中）②**故障转移**（Proxy 感知主从切换——Sentinel 配置提供者——KP-02）③**读写分离**（Proxy 按命令路由——读副本/写主）④**区域偏好**（AZ Locator——07 篇——同区域 Redis 优先）；**18 篇衔接**——客户端侧事件化（写入拦截）vs 本篇 Proxy 侧（流量代理）——**两形态的 Redis 多活入口**
- **对比取舍**：**Proxy 模式（统一入口——治理集中）vs 客户端直连（简单——分散）**——集中治理 vs 零跳数——**Proxy 适合多活治理诉求（区域/故障集中处理）**
- **机制/说明**：SCG Redis Proxy = **"Redis 流量的网关化"**——故障转移（哨兵感知）+ 读写分离（命令路由）+ 区域偏好（AZ Locator）——**docs 的 Redis 多活总入口设计**（TODO 未实现——机制意图照录）；**`[批判性标注：SCG 原生为 HTTP 网关（WebFlux——14 篇）——作为 Redis Proxy 需 RESP 协议适配（自定义扩展——docs 意图的工程可行性依赖协议适配层——非开箱即用）]`**
- **测试佐证**：docs:6/537-538（意图照录）+ 14/07 篇（交叉）+ `[空节标注]`

### KP-06 现状核对（my-xhs：Redis 主从哨兵 3 实例实证）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02/03
- **来源**：my-xhs 实证 + 架构师整合
- **需求**：以 Sentinel 为尺——my-xhs Redis 高可用现状
- **自主实现**：若我设计——核对：主从（有）/哨兵（有）/Cluster（无）/Proxy（无）
- **参考实现**（my-xhs 实证 + 交叉）：**主从 ✅**——redis + redis-slave（docker-compose.yml:211/239——stage-3-01 实证）；**哨兵 ⚠️ `[待验证：哨兵实例数——docker-compose.yml:270 仅 1 个 redis-sentinel 服务定义（docs 前提'至少 3 实例'——教学单哨兵的可用性差距）]`**——+ 注释 `SENTINEL RESET mymaster`（docker-compose.yml:102——**哨兵运维命令实证**）+ start-all.sh "Redis Sentinel 模式"（03 篇提）；**Cluster ❌**（`[现状：主从哨兵——非 Cluster 分片——数据量未触发]`——stage-2-27 衔接）；**SCG Redis Proxy ❌**（`[现状：docs TODO 未实现——my-xhs 无 Redis Proxy]`——触发条件：Redis 治理集中诉求）
- **对比取舍**：**主从哨兵（当前——高可用足够）vs Cluster（分片——规模触发）**——高可用 vs 扩展——**my-xhs 哨兵形态（与 docs 实战同构）**
- **测试佐证**：my-xhs docker-compose.yml:211/239/270/:102（实证）+ stage-3-01（交叉）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 有状态服务一致性（CAP/复制协议） | 分布式理论 | 支撑 | P2 | 🟡 | 时间无关 | High |
| Sentinel 高可用架构（2 优点/4 特性/5 前提） | 分布式问题 | 核心 | P1 | 🟡 | 有效 | High |
| Sentinel 实战（配置要点） | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| Redis Cluster 与多区域复制 | 分布式问题 | 支撑 | P2 | 🟡 | 有效 | Medium |
| SCG 作为 Redis Proxy（TODO 意图） | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | Medium |
| 现状核对（主从哨兵实证） | 工程问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（docker-compose.yml:211/239/270/102——主从哨兵实证）；stage-2-27（Redis Cluster——16384 槽位交叉）；redis 源码（`code/spring/redis`——stage-2-27 已用）
- **关键实证**（grep——写入时验证）：my-xhs docker-compose.yml（redis/redis-slave/redis-sentinel 三服务 + SENTINEL RESET 注释）；docs 链接 `[无本地源码：外部文档 redis.io]`
- **诚实标注**：docs 为**最长篇（538 行，代码块 8 个 awk 实证）**——**Sentinel 配置全文（docs:148-517）按配置要点提取（非全文照录）**；**docs:534 Redis Cluster 自学链接 `[无本地源码：外部文档——stage-2-27 交叉]`**；**docs:537-538 SCG Redis Proxy TODO `[空节标注]`**；**主要内容③多区域复制为意图（正文无——发散）**；OSS vs Enterprise（docs:11-14）为商业背景照录；docs:28 复制协议谱（Raft/ZAB/Gossip/Broadcast/Replication）照录（stage-2 03/04 交叉）
- **关联标注**：stage-2-27/28（Redis）；stage-2-01/03/04（CAP/共识）；01 篇（多活规划）；14 篇（SCG）；07 篇（AZ Locator）；18 篇（客户端侧管道——两形态对照）

---

## 五、本节小结（三层次视角）

**需求**：Redis Server 的高可用与多区域形态——Sentinel/Cluster/多区域复制/SCG Proxy。

**自主实现核心**：①**Sentinel 双层判定**（quorum O_DOWN + 多数派 failover 选举——防误报/防 SPOF）②**配置要点**（monitor/auth/超时/并行同步/故障超时——机制参数）③**Redis 多活全景**（服务端 Sentinel/Cluster + 客户端命令管道（18 篇）+ Proxy 入口（本篇意图））。

**参考实现**：docs 照录（Sentinel 特性/前提/配置要点——非全文）+ stage-2-27 交叉（Cluster）+ my-xhs 实证（主从哨兵）+ 发散（Cluster/Proxy/多区域）。

**对比取舍**：知识本体是"**Redis Server 高可用与多区域机制**"——Sentinel（故障转移系统自身高可用）vs Cluster（分片扩展）vs 多区域（跨区容灾）vs Proxy（集中入口）；配置要点提取（机制）vs 全文照录（操作手册）。

**待验证汇总**：
- my-xhs 哨兵实例数精确核对（`[待验证：docker-compose 哨兵 1 个——docs 前提 3 实例的差距]`）
- SCG Redis Proxy（`[docs TODO——未实现]`）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| 主从复制 | ✅ redis + redis-slave（docker-compose.yml:211/239） | 无 |
| Sentinel 高可用 | ⚠️ redis-sentinel（docker-compose.yml:270）+ SENTINEL RESET（:102）——**实例数 `[待验证]`** | P3：哨兵实例数核对（docs 前提 3 实例——my-xhs 可能 1 个） |
| Redis Cluster | ❌ 未用（主从哨兵） | 现状说明：数据规模未触发（stage-2-27） |
| 多区域复制 | ❌ 单机房 | 现状说明：跨区诉求未触发（01 篇规划） |
| SCG Redis Proxy | ❌ docs TODO 未实现 | 现状说明：Proxy 治理诉求未触发 |

### 差距清单

1. **P3**：哨兵实例数核对（`[待验证]`——docs 前提"至少 3 实例"——my-xhs 单哨兵的可用性差距）
2. **P3**：Cluster 分片（触发条件：数据规模/槽位诉求——stage-2-27）
3. **P3**：SCG Redis Proxy（docs TODO——治理集中诉求）

**结论**：19 篇——my-xhs **主从 + 哨兵已部署**（docker-compose 实证——哨兵实例数待核）；Cluster/多区域/Proxy 为演进项（触发条件驱动）；数据面组（16-19）收官。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为最长篇（538 行）——Sentinel 特性/前提/配置要点照录；Cluster/Proxy/多区域发散；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：Redis Server 多活的完整认知该讲什么

docs 是 Sentinel 实战 + 多活意图文档。完整还该包含：

1. **"故障转移系统也要自身高可用"**（docs:39-44 + 发散）：Sentinel 的多数派一致（防误报）+ 无单点（docs:42"单点故障的故障转移系统没什么乐趣"）——**"高可用系统的自举"**（02 篇自我保护同思想——监控者也要被多数派保护）
2. **"quorum 与多数派的双层判定"**（docs:228-230 + 发散）：quorum（O_DOWN 主观确认——2/3）+ failover 选举（多数派）——**"确认与执行的两级门槛"**（误报防护 + 合法性保障）——与 Raft 选举/02 篇自我保护同思想
3. **"Redis 多活的完整形态"**（docs 4 条 + 发散）：**服务端**（Sentinel 高可用/Cluster 分片）+ **客户端**（18 篇命令管道）+ **Proxy**（本篇 SCG——集中入口）——**三层的 Redis 多活全景**（docs 18/19 两篇合起来才完整）
4. **"配置文档的提取方法论"**（docs:148-517 + 发散）：538 行 sentinel.conf 全文——**提取配置要点（monitor/auth/超时/同步参数）而非照录**——**"配置是操作手册，机制才是知识"**（08 SOP——若实现时再查官方文档）
5. **"哨兵实例数的工程现实"**（my-xhs + 发散）：docs 前提"至少 3 实例"（docs:56）——my-xhs 可能单哨兵（`[待验证]`）——**"教学部署 vs 生产前提的差距"**（3 哨兵是防误报/防 SPOF 的最低要求）
6. **"SCG Redis Proxy 的定位"**（docs:6 + 发散）：Proxy = **Redis 流量的集中治理入口**（故障转移/读写分离/区域偏好——docs:6）——**"网关模式延伸到数据层"**（与 14 篇 SCG 多活衔接——网关从 HTTP 到 Redis 协议）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 多数派一致判定 vs 单 Sentinel | 防误报 vs 快（docs 选多数派） |
| quorum（O_DOWN） vs 多数派（failover） | 确认门槛 vs 执行合法性（双层） |
| Sentinel（高可用） vs Cluster（分片） vs Proxy（治理） | 可用性 vs 扩展 vs 集中（不同目的可组合） |
| 配置要点提取 vs 全文照录 | 机制 vs 操作手册（08 SOP 选机制） |
| 3 哨兵（生产前提） vs 单哨兵（教学） | 防误报防 SPOF vs 简单（my-xhs 待核） |

### 常见坑/反模式

1. **单哨兵**：docs:42 明示"单点故障的故障转移系统没什么乐趣"——单哨兵 = 哨兵自身 SPOF（my-xhs 待核）
2. **quorum 当 failover 门槛**：quorum 只确认 O_DOWN——failover 需多数派（docs:228-230——两层分开）
3. **异步复制当零丢失**：docs:58——异步复制不保证已确认写入（RPO 非零——故障窗口）
4. **配置不测试**：docs:60——"凌晨 3 点才发现的错误配置"（HA 必须演练）
5. **配置全文照录**：538 行照录 = 操作手册不是知识（提取配置要点）
6. **哨兵与客户端不配套**：docs:59——客户端需 Sentinel 支持（选型时验证）

### 生态位置

- **stage-4 教学主线**：**数据面组（16-19 收官）**——16 MySQL Server → 17 MySQL JDBC → 18 Redis Client → **19 Redis Server（本篇：Sentinel/Cluster/Proxy——数据面组收官）** → 22 动态 JDBC
- **前后篇衔接**：stage-2-27/28（Redis）；stage-2-01/03/04（CAP/共识）；01 篇（多活规划）；14/07 篇（SCG/AZ Locator）；18 篇（客户端侧管道——两形态对照）；22 篇（动态 JDBC——docs 顺序）
- **与源码提取的关系**：redis 源码（stage-2-27 已用——code/spring/redis）；docs 链接 `[无本地源码：外部文档]`

**架构师视角结论**：本篇为 **最长篇（538 行，代码块 8 个）**——**Sentinel 高可用**（分布式系统 2 优点/特性 4 条/前提 5 条/实战配置要点——双层判定 quorum+多数派）+ **Cluster**（自学链接——stage-2-27 交叉）+ **多区域复制**（意图发散）+ **SCG Redis Proxy**（TODO——故障转移/读写分离/AZ Locator 意图）——知识本体是"**Redis Server 高可用与多区域机制**"（故障转移系统自身高可用/配置要点提取/Redis 多活三层全景）；my-xhs **主从哨兵已部署**（哨兵实例数待核——docs 3 实例前提）；**数据面组（16-19）收官**。
