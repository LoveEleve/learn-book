# stage-4 · 第 16 节：第十三节：MySQL Server 多活架构实现 — 知识点提取

> 课程：stage-4 多活架构 第 16 节（数据面组 16-19 第一篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/16. 第十三节：MySQL Server 多活架构实现.md`
> 提取时间：2026-08-12 | 权重：核心（Binlog 订阅机制——异构同步的增量面；Source-Replica/MGR 交叉 stage-3-11）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**MySQL 主题（非 Eureka 文档）——机制 = binlog 订阅 + 跨区同步**

> **文档形态**：**短篇（51 行，java 块 0 个——awk 计数实证）**——主要内容 3 条：①回顾 Source-Replica/MGR（stage-3-11 交叉）②实战 Source-Replica + **跨区 Source-Source 同步**（docs:10-11——最终一致性）③**Binlog 订阅机制**（Canal/Maxwell/Connector——docs:12-47 主体）；**知识本体 = Binlog 订阅机制的完整认知**（docs 简介/历史/业务列表 + 发散）。

---

## 一、本节概览

- **技术域**：MySQL 多活（Source-Replica/Source-Source/MGR）、Binlog 订阅（Canal/Maxwell/Connector——异构同步）
- **维度**：`[分布式问题]`（多活/同步）+ `[工程问题]`（订阅机制/工具选型）+ `[分布式理论]`（最终一致）
- **核心命题**：**MySQL Server 多活的实现路径**——docs 三主题：①Source-Replica/MGR 回顾（stage-3-11 已提取——交叉）②跨区 Source-Source（最终一致性——docs:11）③**Binlog 订阅机制**（Canal 等——异构 Database 同步——MySQL→ES/Redis）；**知识本体 = "binlog 是 MySQL 多活的通用数据通道"**（复制/订阅/异构同步都基于它）
- **知识点数**：4 个
- **前置**：stage-3-11（MySQL 高可用——主从/MGR）、stage-2 01（CAP——最终一致）

## 前置条件清单
读者需先掌握：
1. **MySQL 主从复制/MGR**（stage-3-11——binlog/GTID/复制机制）
2. **最终一致性**（stage-2 01——BASE）
3. **binlog 概念**（stage-3-11——ROW 格式/CDC 前提）
未达前置者，先补：stage-3-11

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **交叉为主**：Source-Replica/MGR → stage-3-11（已提取——不重提）
- **发散为本**：Binlog 订阅机制（docs 简介 + 历史 + 业务列表）→ 完整认知发散（实现机制/选型/落地）
- **实例对照**：my-xhs Canal 已部署（docker-compose.yml:454-455 实证）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 MySQL 多活三形态回顾与跨区同步（Source-Replica/MGR/Source-Source）【docs 主要内容①②】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：stage-3-11（主从/MGR）
- **来源**：docs 主要内容①②（docs:2-3）+ §Source-Replica（docs:7-8）+ §Source-Source（docs:10-11）
- **需求**：**MySQL 多活三形态**——docs 明确：①回顾 **Source-Replica Replication**（第二期）+ **MySQL Group Replication**（第三期——docs:2——**stage-3-11 交叉不重提**）②实战 **Source-Replica 同步** + **跨区 Source-Source 同步**（docs:3）
- **自主实现**：若我设计——MySQL 多活三形态按"写面"区分：**Source-Replica**（单写多读——主从）+ **MGR**（**组复制——Paxos 强一致——默认 Single-Primary 单主、Multi-Primary 多主可选**——2026-08-12 精确化：初稿"组内多写"表述不准确）+ **Source-Source 跨区**（双写双向——**最终一致性**——docs:11）——**按 RPO/RTO 与一致性诉求选**（01 篇多活概念衔接）
- **参考实现**（docs 交叉 + 发散）：**Source-Replica/MGR（docs:8 交叉）**——"参考《第三期 第九节：高可用 MySQL 数据库》"（docs:8——**stage-3-11 已提取：主从 GTID/binlog + MGR Paxos——交叉不重提**）；**跨区 Source-Source（docs:10-11 照录）**——**"最终一致性的实现"**（docs:11——**跨区双向复制的最终一致模型——01 篇异地多活的 MySQL 面**——异步复制 + 冲突处理（01 篇 §7 跨区域规划衔接））
- **对比取舍**：**Source-Replica（单写——简单）vs MGR（组写——强一致）vs Source-Source（双写——最终一致）**——写面能力 vs 一致性/复杂度——**按区域拓扑选**（同城 MGR/跨区 Source-Source——01 篇规划）
- **机制/说明**：MySQL 多活三形态 = **"写面 × 一致性"的选型矩阵**——Source-Replica（1 写 N 读）、MGR（N 写强一致——多数派）、Source-Source（双向最终一致）——**跨区场景的默认是 Source-Source（最终一致——延迟容忍）**
- **测试佐证**：docs:2-3/7-11（照录）+ stage-3-11（交叉）+ 01 篇（多活规划衔接）

### KP-02 Binlog 订阅机制（Canal——增量解析/订阅/消费）【docs 主要内容③主体】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01、stage-3-11（binlog ROW）
- **来源**：docs §Alibaba Canal 框架（docs:14-30——简介/历史/业务列表/版本）+ 发散
- **需求**：**binlog 订阅机制**——docs 明确：Canal 基于 **MySQL 数据库增量日志解析**——提供**增量数据订阅和消费**（docs:18）；历史（docs:20——**杭州美国双机房跨机房同步——早期业务 trigger 获取增量——2010 起数据库日志解析**）；业务列表（docs:24-28——**5 类**：数据库镜像/实时备份/索引构建维护/业务 cache 刷新/带业务逻辑的增量数据处理）；支持版本（docs:29——**5.1.x~8.0.x**）
- **自主实现**：若我设计——binlog 订阅应用：**伪装 MySQL 从库**（binlog dump 协议——复用复制机制）→ 解析 binlog 事件（ROW 变更）→ 增量数据发布（订阅/消费）——**"订阅 binlog = 以从库身份读复制流"**（复制机制是现成通道）
- **参考实现**（docs 照录 + 发散 + my-xhs）：**Canal（docs:14-30 照录）**——增量日志解析 + 订阅消费（docs:18）；**历史动机（docs:20 照录）**——跨机房同步（业务 trigger → 日志解析——**trigger 方案 vs binlog 方案的演进**：trigger 侵入业务 vs binlog 无侵入）；**5 类业务（docs:24-28 照录）**——镜像/备份/索引/缓存刷新/带逻辑的增量处理（**异构同步的消费面**——MySQL→ES/Redis——docs:4 意图）；**版本（docs:29 照录）**——5.1.x~8.0.x；**`[无本地源码：Canal 为外部项目（github.com/alibaba/canal 链接——docs:15）]`**；**my-xhs 实证**——Canal 已部署（`docker-compose.yml:454-455`——my-xhs-canal-server:v1.1.7-squashed）
- **对比取舍**：**binlog 订阅（无侵入——解析日志）vs trigger/业务埋点（侵入——应用配合）**——解耦 vs 可控——**binlog 订阅是异构同步的标准通道**（docs 历史演进印证）
- **机制/说明**：binlog 订阅的本质 = **"复用主从复制通道做异构消费"**——Canal 伪装从库拉取 binlog（MySQL 复制协议）→ 解析为增量事件 → 供下游（ES/Redis/镜像）消费——**binlog 是 MySQL 的"通用变更事件流"**（复制/CDC/异构同步同一通道）；ROW 格式是前提（stage-3-11 交叉）
- **测试佐证**：docs:14-30（照录）+ my-xhs `docker-compose.yml:454-455`（实证）+ stage-3-11（binlog ROW 交叉）

### KP-03 Binlog 订阅生态（Maxwell/Connector——选型对比）【docs §Maxwell/§Connector】
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §Maxwell's Daemon（docs:31-41）+ §MySQL Binlog Connector for Java（docs:44-47）
- **需求**：**binlog 订阅的生态选型**——docs 列出：**Maxwell's Daemon**（docs:31——**What's it for：ETL/审计日志/cache 构建失效/search indexing/服务间通信——5 用途**——docs:37-41）+ **MySQL Binlog Connector for Java**（docs:44-47——**3 链接**：whitesock/open-replicator + shyiko/osheroff mysql-binlog-connector-java）
- **自主实现**：若我设计——binlog 订阅选型三档：**成熟框架**（Canal——Java 生态/国内主流）/**轻量守护**（Maxwell——单机/简单——ETL/审计）/**底层库**（Connector——自研订阅应用的基础）
- **参考实现**（docs 照录 + 发散）：**Maxwell（docs:37-41 照录——5 用途）**——ETL/审计日志/cache 构建与失效/search 索引/服务间通信（**`[无本地源码：外部项目 maxwells-daemon.io]`**——**JSON 输出的轻量 binlog 守护——对比 Canal 的完整框架**）；**Connector 三链接（docs:45-47 照录）**——open-replicator（whitesock）/mysql-binlog-connector-java（shyiko 与 osheroff——**fork 关系**——**底层 binlog 解析库——自研订阅的基础**）`[无本地源码：外部项目]`
- **对比取舍**：**Canal（框架——解析+订阅+消费一体）vs Maxwell（轻量——JSON 输出）vs Connector（底层库——自研）**——完整度 vs 轻量 vs 灵活——**按需求选：异构同步用 Canal、简单 ETL 用 Maxwell、深度定制用 Connector**
- **机制/说明**：binlog 订阅生态 = **"框架 → 守护 → 底层库"三档**——都基于同一机制（binlog 解析——KP-02）——**选型是"封装度 × 定制需求"的权衡**
- **测试佐证**：docs:31-47（照录）+ `[无本地源码]` 标注

### KP-04 现状核对（my-xhs：Canal 已部署 + 主从——异构同步面）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02/03
- **来源**：my-xhs 实证 + 架构师整合
- **需求**：以 binlog 订阅为尺——my-xhs 的数据同步现状
- **自主实现**：若我设计——核对：主从（有——3306/3307）/Canal（有——docker-compose）/异构消费（未核）
- **参考实现**（my-xhs 实证 + 交叉）：**主从 ✅**——MySQL 3306 主/3307 从（stage-3-11 实证——docker-compose.yml:112/158）；**Canal ✅ 已部署**——`docker-compose.yml:454-455`（my-xhs-canal-server:v1.1.7-squashed——**KP-02 实证——canal 组件就绪**）；**异构消费 `[待验证：Canal 下游消费链路——stage-3-11 P3-9 差距项（HANDOVER-session004）]`**；**跨区 Source-Source ❌**——单机房（`[现状：无跨区——01 篇规划衔接]`）
- **对比取舍**：**Canal 已部署（组件就绪）vs 消费链路未核（配置/下游未知）**——部署 vs 使用——**"部署了≠消费链路通"——P3-9 差距项**
- **测试佐证**：my-xhs `docker-compose.yml:454-455`（实证）+ stage-3-11（P3-9 交叉）+ `[待验证]` 标注

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| MySQL 多活三形态（Source-Source） | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| Binlog 订阅机制（Canal） | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| Binlog 订阅生态（Maxwell/Connector） | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| 现状核对（Canal 已部署） | 工程问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（docker-compose.yml:454-455——Canal 部署实证 + 3306/3307 主从——stage-3-11）；stage-3-11（主从/MGR 机制——交叉）
- **关键实证**（交叉引用）：stage-3-11（Source-Replica/MGR——docs:8 参考指向）；docker-compose.yml:454-455（Canal——**写入时 grep 实证**）
- **诚实标注**：docs 为**短篇（51 行，java 块 0 个 awk 实证）**——Canal/Maxwell/Connector 均为**外部项目链接** `[无本地源码：外部项目]`；**Source-Replica/MGR 交叉 stage-3-11 不重提**（docs:8 明示参考）；图 1 张（docs:30）`[跳过：图示佐证]`；Canal 机制发散（伪装从库/通用事件流——docs 未展开——`[发散]`）；docs:29 版本列表照录（5.1.x~8.0.x——5 个版本线）
- **关联标注**：stage-3-11（主从/MGR/binlog ROW）；01 篇（多活规划——跨区衔接）；stage-2 01（最终一致）；HANDOVER-session004（P3-9 Canal 下游消费差距）

---

## 五、本节小结（三层次视角）

**需求**：MySQL Server 多活——三形态回顾 + 跨区 Source-Source + Binlog 订阅（异构同步）。

**自主实现核心**：①**三形态选型矩阵**（Source-Replica 单写/MGR 组写/Source-Source 双写最终一致——按区域拓扑选）②**binlog 订阅 = 伪装从库复用复制通道**（通用变更事件流——复制/CDC/异构同步同一通道）③**生态三档选型**（Canal 框架/Maxwell 轻量/Connector 底层）。

**参考实现**：docs 照录（Canal 简介/历史/5 业务/Maxwell 5 用途/3 链接）+ stage-3-11 交叉（不重提）+ my-xhs 实证（Canal 已部署）+ 发散（机制/选型）。

**对比取舍**：知识本体是"**binlog 作为 MySQL 多活通用数据通道**"——trigger vs binlog（无侵入演进）、三形态（写面×一致性）、三档选型（封装度×定制）。

**待验证汇总**：
- my-xhs Canal 下游消费链路（`[待验证]`——P3-9 差距项延续）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| Source-Replica | ✅ 3306 主/3307 从（stage-3-11） | 无 |
| MGR | ❌ 未用（主从为主——stage-3-11） | 现状说明：强一致诉求未触发 |
| 跨区 Source-Source | ❌ 单机房 | 现状说明：跨区诉求未触发（01 篇规划） |
| Binlog 订阅（Canal） | ✅ 已部署（docker-compose.yml:454-455） | **P3-9：下游消费链路核对**（[待验证]） |
| 异构同步（MySQL→ES/Redis） | ❓ `[待验证]`（Canal 消费面未核） | P3：与 ES/Redis 联动评估 |

### 差距清单

1. **P3**：Canal 下游消费链路核对（P3-9 差距项延续——配置/消费端/联动）
2. **P3**：跨区 Source-Source（触发条件：跨机房诉求——01 篇规划衔接）

**结论**：16 篇——my-xhs **Canal 已部署 + 主从就绪**（binlog 订阅组件齐备）；异构消费链路待核（P3-9）；跨区同步未触发；无 P1/P2 差距。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为短篇（51 行）——Canal/Maxwell 简介照录；机制发散标注；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：MySQL Server 多活的完整认知该讲什么

docs 是短篇声明 + 工具简介。完整还该包含：

1. **"binlog 是 MySQL 的通用变更事件流"**（docs 主题③ + 发散）：复制（主从）、CDC（订阅）、异构同步（ES/Redis）——**全部基于 binlog**——"binlog 优先"是现代 MySQL 数据架构的底层共识（Change Data Capture 的通用通道）——docs 的 Canal/Maxwell 都是这条通道的消费端
2. **"伪装从库是订阅的标准技巧"**（发散）：Canal 等以**从库身份**走 binlog dump 协议——**复用 MySQL 原生复制机制**（不侵入主库/不额外开启通道）——理解"订阅 = 复制协议的应用"是机制核心
3. **"trigger vs binlog 的演进"**（docs:20 + 发散）：docs 历史（业务 trigger → 2010 日志解析）——**trigger 侵入业务（性能/维护负担）vs binlog 无侵入（日志级）**——**"数据变更的外置订阅"是演进方向**（与 14 篇事件驱动设计同思想——变更即事件）
4. **"跨区 Source-Source 的最终一致模型"**（docs:11 + 01 篇衔接）：双写双向（异步复制 + 冲突处理）——**异地多活的 MySQL 面**（01 篇 §7 规划：异地单元化——各单元写自己的分片——避免双向冲突）——**"最终一致"不是免责声明，是要设计冲突解决**
5. **"部署 ≠ 使用"**（my-xhs + 发散）：Canal 已部署（docker-compose）但**下游消费链路未核**（P3-9）——"看起来有了≠真的在用"——**组件就绪到链路贯通之间还有配置/消费端/联动的工作**（与 05 篇灰度"打标≠路由"同型教训）
6. **"异构同步的业务面"**（docs:24-28 + 发散）：镜像/备份/索引/cache/增量处理——**binlog 订阅是"读侧扩展"的万能通道**（写主库、读异构——CQRS 的 MySQL 实现——12 篇搜索 ES 同步衔接）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| Source-Replica vs MGR vs Source-Source | 单写简单 vs 组写强一致 vs 双写最终一致（按区域拓扑） |
| trigger vs binlog 订阅 | 侵入可控 vs 无侵入（演进方向 binlog） |
| Canal（框架） vs Maxwell（轻量） vs Connector（底层） | 完整度 vs 轻量 vs 定制 |
| 单机房（当前） vs 跨区 Source-Source | 简单 vs 最终一致复杂度（触发条件驱动） |

### 常见坑/反模式

1. **binlog 格式非 ROW**：订阅依赖 ROW 格式（stage-3-11）——STATEMENT 格式无法可靠解析
2. **订阅消费端不幂等**：binlog 至少一次投递——消费端幂等（stage-2 17 教训延续）
3. **部署 Canal 不配下游**：组件就绪 ≠ 链路贯通（my-xhs P3-9——"部署≠使用"）
4. **跨区双写无冲突处理**：Source-Source 的最终一致需要冲突设计（单元化/时间戳——01 篇规划）
5. **trigger 方案遗留**：业务 trigger 侵入——新项目用 binlog 订阅（docs 历史演进方向）
6. **单消费端单点**：binlog 订阅消费端要 HA（Canal 集群——订阅通道的可用性）

### 生态位置

- **stage-4 教学主线**：**数据面组（16-19 第一篇）**——15 网关 → **16 MySQL Server 多活（本篇：三形态 + binlog 订阅）** → 17 MySQL JDBC 多活 → 18-19 Redis 多活 → 22 动态 JDBC
- **前后篇衔接**：stage-3-11（主从/MGR/binlog ROW——本篇交叉）；01 篇（多活规划——跨区衔接）；stage-2 01（最终一致）；12 篇（搜索 ES——异构同步衔接）；17 篇（MySQL JDBC——docs 顺序）
- **与源码提取的关系**：Canal/Maxwell `[无本地源码：外部项目]`；my-xhs docker-compose 实证

**架构师视角结论**：本篇为 **短篇（51 行）提取**——三形态回顾（stage-3-11 交叉）+ 跨区 Source-Source（最终一致）+ **Binlog 订阅机制**（Canal 5 类业务/历史动机/版本 + Maxwell 5 用途 + Connector 3 链接）——知识本体是"**binlog 作为 MySQL 多活通用数据通道**"（伪装从库订阅/trigger→binlog 演进/三档选型）；my-xhs **Canal 已部署 + 主从就绪**（下游消费链路 P3-9 待核——"部署≠使用"教训）；**数据面组开篇，17 篇进入 MySQL JDBC 多活**。
