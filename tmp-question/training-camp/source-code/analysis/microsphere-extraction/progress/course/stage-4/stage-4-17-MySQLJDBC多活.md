# stage-4 · 第 17 节：第十四节：MySQL JDBC 多活架构设计与实现 — 知识点提取

> 课程：stage-4 多活架构 第 17 节（数据面组 16-19 第二篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/17. 第十四节：MySQL JDBC 多活架构设计与实现.md`
> 提取时间：2026-08-12 | 权重：核心（JDBC 层多活——Multi-Host 模式 + AZ Locator 整合；与 stage-2-25 交叉）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**MySQL JDBC 主题（非 Eureka 文档）——机制 = JDBC Multi-Host + 区域路由**

> **文档形态**：JDBC 多活设计文档（103 行，java 块 2 个——awk 计数实证）——主要内容 3 条：①**负载均衡策略**整合 AZ Locator（Multi-Host 同区域优先）②**Replication API** 整合（故障转移 Source→Replica/Source + 读写分离）③**Failover API** 整合（故障恢复/自动复联）；**与 stage-2-25（Connector/J 高可用连接）交叉**——failOverReadOnly 枚举已在 stage-2-25 提取，本篇增量 = **语义详述 + JDBC 层多活设计**。

---

## 一、本节概览

- **技术域**：MySQL JDBC Multi-Host（SINGLE/LOADBALANCE/FAILOVER/REPLICATION 四模式）、failOverReadOnly 语义、JDBC 层区域路由（AZ Locator 整合）、代理实现（静态/动态）
- **维度**：`[分布式问题]`（故障转移/读写分离/区域路由）+ `[工程问题]`（连接模式/代理设计）+ `[规范]`（JDBC URL）
- **核心命题**：**JDBC 层的多活能力**——docs 三主线：①Multi-Host 连接模式（驱动内建的高可用/负载均衡/复制连接）②AZ Locator 整合（JDBC 层区域路由——**适合模式 = LOADBALANCE**）③主动/被动切换设计（优雅关闭 vs 立即关闭）；**知识本体 = "JDBC 连接层的多活机制"**（驱动级——应用无感知）
- **知识点数**：5 个
- **前置**：stage-2-25（Connector/J 高可用——交叉）、07 篇（AZ Locator）、stage-3-11（读写分离落地）

## 前置条件清单
读者需先掌握：
1. **Connector/J 高可用连接**（stage-2-25 KP-03——Load Balanced/failOverReadOnly 枚举）
2. **AZ Locator 抽象**（07 篇——区域路由层）
3. **读写分离**（stage-3-11——落地形态）
未达前置者，先补：stage-2-25 / 07 篇

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **交叉为主**：failOverReadOnly 枚举（stage-2-25）——本篇补语义详述
- **发散为本**：JDBC 层多活设计（URL 分析/Zone 绑定/切换设计——docs 设计节）
- **实例对照**：my-xhs ReadWriteRoutingDataSource（stage-3-11 实证）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 MySQL JDBC Multi-Host 连接模式（SINGLE/LOADBALANCE/FAILOVER/REPLICATION 四模式）【docs §连接类型】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-2-25（Connector/J）
- **来源**：docs §JDBC Driver 连接类型（docs:6-50）
- **需求**：**MySQL JDBC 的连接模式族**——docs 明确：`ConnectionUrl.Type` API（docs:8）+ 四模式：**SINGLE_CONNECTION**（单主机——URL 实现 SingleConnectionUrl/Connection 实现 ConnectionImpl——docs:11-14）/ **LOADBALANCE_CONNECTION**（负载均衡——docs:17）/ **FAILOVER_CONNECTION**（容灾——FailoverConnectionUrl——docs:18-19）/ **REPLICATION_CONNECTION**（副本——docs:50）
- **自主实现**：若我设计——Multi-Host 连接模式 = **驱动级多主机管理**：连接 URL 声明多主机 → 驱动按模式语义管理（负载均衡/容灾/复制）
- **参考实现**（docs 照录 + stage-2-25 交叉 + 本地验证）：**四模式（docs:11-50 照录）**——SINGLE（单机——基础）/LOADBALANCE（多机负载均衡）/FAILOVER（多机容灾——**URL 格式：`jdbc:mysql://[primary],[secondary1],[secondary2]...`——docs:22-23**）/REPLICATION（读写分离——副本）；**`[无本地源码：mysql-connector-j 未在本地 code/spring——类名（SingleConnectionUrl/FailoverConnectionUrl/FailoverConnectionProxy/ConnectionImpl）按 docs 照录]`**；**stage-2-25 交叉**——Connector/J 高可用连接已提取（Load Balanced 模式/failOverReadOnly 等 PropertyKey——stage-2-25 KP-03——**本篇为模式族全景补充**）
- **对比取舍**：**驱动级 Multi-Host（应用无感知）vs 应用层路由（数据源代理）**——透明 vs 可控——**驱动级是"零改造"方案（JDBC URL 即配置）**
- **机制/说明**：Multi-Host 四模式 = **JDBC 驱动的多活内置能力**——URL 声明主机列表 + 模式决定语义（LB 轮询/FAILOVER 顺序/REPLICATION 读写）——**"URL 即拓扑声明"**
- **测试佐证**：docs:6-50（照录）+ stage-2-25（交叉）+ `[无本地源码]` 标注

### KP-02 FAILOVER 容灾模式（动态代理 + failOverReadOnly 语义 + 恢复参数）【docs §容灾连接模式】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §FAILOVER_CONNECTION（docs:18-49）
- **需求**：**FAILOVER 模式的容灾语义**——docs 明确：**Connection 实现 = JdbcConnection 的动态代理**（docs:26——`createProxyInstance`——**FailoverConnectionProxy + Proxy.newProxyInstance**——docs:27-32）；**failOverReadOnly 参数**（docs:36-44——**true/false 两分支的读写模式语义**）；**恢复 Primary 参数**（docs:46-49——**secondsBeforeRetrySource/queriesBeforeRetrySource**）
- **自主实现**：若我设计——FAILOVER 连接 = **代理层管理多主机连接**：主连接 + 备连接池 + 故障探测/切换/恢复（重试参数控制恢复节奏）
- **参考实现**（docs 照录 + stage-2-25 交叉）：**动态代理（docs:26-32 照录）**——`FailoverConnectionProxy` + JDK 动态代理（docs:30-31——**代理拦截 Connection 接口方法**）；**failOverReadOnly 语义（docs:36-44 照录——两分支）**——**true**：初始 Primary 读写、**故障转移后 Secondary 强制只读（无论 setReadOnly 调整）**、恢复 Primary 后读写继续（docs:37-40）；**false**：故障转移后 Secondary 只读、**可主动 setReadOnly(false) 切回读写**（docs:41-44）——**"故障期间写保护"vs"允许人工切写"的取舍**；**恢复参数（docs:46-49 照录）**——`secondsBeforeRetrySource`（时间重试）/`queriesBeforeRetrySource`（查询数重试）——**Primary 恢复探测节奏**；**stage-2-25 交叉**——failOverReadOnly 枚举已提取（stage-2-25 :108——**本篇补 true/false 语义详述**）
- **对比取舍**：**failOverReadOnly=true（故障期强制只读——安全）vs false（可人工切写——灵活）**——写保护 vs 运维弹性——**生产默认 true（防故障期误写 Replica）**
- **机制/说明**：FAILOVER 容灾 = **"代理连接 + 主备切换 + 只读保护"**——动态代理拦截（透明切换）+ failOverReadOnly（故障期写保护）+ 重试参数（自动恢复）——**docs 主要内容的"故障恢复/自动复联"即此**（docs:4 意图）
- **测试佐证**：docs:18-49（照录）+ stage-2-25（交叉）+ `[无本地源码]`

### KP-03 JDBC 层多活设计（URL 分析 → Zone 绑定 + AZ Locator 扩展 + 切换设计）【docs §实现多活架构】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：07 篇（AZ Locator）、KP-01/02
- **来源**：docs §基于 MySQL JDBC 驱动 API 实现多活（docs:57-73）+ §基于 JDBC 通用 API（docs:75-91）
- **需求**：**JDBC 层区域多活的设计**——docs 明确：**URL 分析**（拿 Multi-Host 信息——Primary/Secondary——docs:59-61 + **与 Zone/Region 映射（稳定确定）**——docs:63）+ **三模式考虑**（FAILOVER 顺序执行/LOADBALANCE 内建自定义规则/REPLICATION——docs:66-68）+ **适合 AZ Locator 扩展的模式 = LOADBALANCE_CONNECTION**（docs:70）+ **AZ Locator 扩展对象归纳**（**负载均衡扩展：集合中选择其一/服务路由扩展：集合中选择其子集**——docs:72-73）；**切换设计**（docs:82-91——**主动切换**（配置/API 调整目标——docs:84）+ **被动切换**（异常/后台重建——docs:86）+ **Zone/Region 与 Connection 绑定**（docs:89）+ **主动→延迟关闭（优雅）/被动→立即关闭**（docs:90-91））
- **自主实现**：若我设计——JDBC 层区域多活：**①URL 主机列表 ↔ Zone 映射**（每主机标 Zone）②**区域路由**（当前 Zone → 同区域主机优先——ZonePreference 机制）③**切换策略**（主动：配置/API 切目标——优雅关闭旧连接；被动：故障触发——立即关闭重建）
- **参考实现**（docs 照录 + 发散 + 07 篇衔接）：**URL 分析（docs:58-63）**——Multi-Host 信息 + Zone/Region 映射（**"稳定（确定）"——docs:63——区域拓扑是静态映射**）；**模式选型（docs:66-70）**——FAILOVER（顺序执行）/LOADBALANCE（内建/自定义规则）/REPLICATION——**docs 结论：LOADBALANCE 模式适合 AZ Locator 扩展**（docs:70——区域偏好作为"选择其一"规则）；**AZ Locator 扩展两对象（docs:72-73 照录）**——**负载均衡扩展（集合选择其一——选实例）vs 服务路由扩展（集合选择子集——过滤）**——**07 篇 ZonePreference 语义的双形态**（选一=规则、子集=过滤）；**切换设计（docs:82-91 照录）**——主动（优雅关闭——docs:90）/被动（立即关闭——docs:91——**故障已发生的果断性**）；**07 篇衔接**——区域路由机制（ZonePreference Filter/Supplier）在 JDBC 层的挂载（主机选择面）
- **对比取舍**：**主动切换（可控——优雅）vs 被动切换（故障——立即）**——优雅 vs 果断——**关闭策略与切换类型绑定**（docs 设计核心）；**LOADBALANCE 模式（区域路由挂载点）vs 其他模式**——docs 明确选 LB（规则可扩展）
- **机制/说明**：JDBC 层多活 = **"URL 拓扑声明 + Zone 映射 + 区域路由规则 + 切换策略"**——**区域信息与连接绑定**（docs:89——连接携带 Zone 语义）——与 08 篇 attachZone（元信息写面）呼应（JDBC 是读面）；**AZ Locator 扩展两对象** = 07 篇 ZonePreference 的"选一（LB 规则）与选子集（过滤）"双语义
- **测试佐证**：docs:57-91（照录）+ 07 篇（区域机制交叉）

### KP-04 代理实现方式（静态 Druid/P6Spy vs 动态 Spring AOP）【docs §JDBC 通用 API】
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：无
- **来源**：docs §基于 JDBC 通用 API 实现多活（docs:75-80）
- **需求**：**JDBC 多活的代理实现两路**——docs 明确：**静态代理**（Druid/P6Spy——操作 Connection——docs:78）+ **动态代理**（Spring AOP——操作 DataSource——docs:80）
- **自主实现**：若我设计——JDBC 拦截两路：**静态**（包装 Connection——每连接一层）vs **动态**（代理 DataSource——创建层拦截）
- **参考实现**（docs 照录 + 发散）：**静态代理（docs:78 照录）**——Druid/P6Spy 风格（**Connection 包装**——连接级拦截）；**动态代理（docs:80 照录）**——Spring AOP（**DataSource 代理**——创建连接时拦截）；**选型（发散）**——静态（细粒度——连接操作全拦截——Druid 生态主流）/动态（粗粒度——创建层——AOP 便捷）
- **对比取舍**：**静态（Connection 级——细）vs 动态（DataSource 级——粗）**——粒度 vs 侵入——**Druid（静态——my-xhs 连接池生态？）vs Spring AOP（动态——框架内建）**
- **机制/说明**：JDBC 多活的织入层 = **Connection（静态）或 DataSource（动态）**——区域路由/切换逻辑的挂载点选择（粒度 × 侵入权衡）
- **测试佐证**：docs:75-80（照录）+ 发散标注

### KP-05 现状核对（my-xhs：ReadWriteRoutingDataSource——JDBC 层读写分离落地）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~04
- **来源**：my-xhs 实证（stage-3-11 交叉）+ 架构师整合
- **需求**：以 JDBC 多活为尺——my-xhs 数据访问层现状
- **自主实现**：若我设计——核对：读写分离（有——ReadWriteRoutingDataSource）/区域路由（JDBC 层——无）/Multi-Host 驱动模式（无）
- **参考实现**（my-xhs 实证 + 交叉）：**读写分离 ✅ 应用层实现**——`ReadWriteRoutingDataSource` + `ReadWriteRoutingDataSourceConfig`（stage-3-11 实证——session004 源码索引：**Spring AbstractRoutingDataSource 路由模式（determineCurrentLookupKey 按上下文选数据源）——2026-08-12 修正：初稿误称 docs:80 的"动态代理 DataSource"——路由模式 ≠ AOP 动态代理，两者是 DataSource 层的不同机制**）；**Multi-Host 驱动模式 ❌ 未用**（`[现状：应用层路由（DataSource 代理）而非驱动级 Multi-Host——JDBC URL 单主机]`）；**区域路由 JDBC 层 ❌**（`[现状：区域路由在 LoadBalancer 面（11 篇）——JDBC 层未挂]`——触发条件：多区域数据访问）
- **对比取舍**：**应用层路由（ReadWriteRoutingDataSource——可控）vs 驱动级 Multi-Host（透明）**——my-xhs 选应用层（读写分离语义自定义——SQL 级路由——stage-3-11 三策略）
- **测试佐证**：my-xhs `ReadWriteRoutingDataSource`（stage-3-11/session004 实证）+ 交叉标注

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| Multi-Host 四模式 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| FAILOVER 容灾（failOverReadOnly 语义） | 分布式问题 | 核心 | P1 | 🟡 | 有效 | High |
| JDBC 层多活设计（Zone 绑定/切换） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 代理实现（静态/动态） | 工程问题 | 支撑 | P2 | 🟢 | 有效 | High |
| 现状核对（ReadWriteRoutingDataSource） | 工程问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（ReadWriteRoutingDataSource——stage-3-11 实证）；stage-2-25（Connector/J 高可用——failOverReadOnly 枚举交叉）；**mysql-connector-j `[无本地源码：未在本地 code/spring——类名按 docs 照录]`**
- **关键实证**（交叉引用）：stage-2-25 KP-03（Load Balanced/failOverReadOnly——docs 17 的枚举面已提取——本篇补语义详述）；my-xhs `ReadWriteRoutingDataSource`（stage-3-11——session004 源码索引）
- **诚实标注**：docs 为 **JDBC 多活设计文档（103 行，java 块 2 个 awk 实证）**——类名 `[无本地源码：mysql-connector-j]` 照录；**LOADBALANCE/REPLICATION 空节标题（docs:17/50——docs 未展开——机制发散）**；图 1 张（docs:53——Multi-Host+多区域——异地/同城灾备区分 docs:55 照录）`[跳过：图示佐证]`；**failOverReadOnly 语义为 docs 详述（stage-2-25 只有枚举名——本篇增量）**；**`[跳过：docs:93-103 JDBC URL 基础（Protocol 主 jdbc:/子 mysql,oracle + Authority user-info@host:port——host 允许多值）——URL 规范为基础知识（stage-1 已覆盖——非多活机制，穷尽性标注）]`**
- **关联标注**：stage-2-25（Connector/J）；07 篇（AZ Locator——区域路由）；stage-3-11（读写分离）；16 篇（binlog——数据面组衔接）

---

## 五、本节小结（三层次视角）

**需求**：JDBC 层的多活——Multi-Host 模式（驱动级）+ AZ Locator 整合（区域路由）+ 切换设计。

**自主实现核心**：①**URL 即拓扑声明**（Multi-Host 主机列表 + Zone 映射——静态）②**区域路由挂载 = LOADBALANCE 模式**（AZ Locator 两对象：选一（规则）/选子集（过滤））③**切换策略与关闭绑定**（主动→优雅/被动→立即）④**织入层选择**（Connection 静态 vs DataSource 动态）。

**参考实现**：docs 照录（四模式/failOverReadOnly 两分支语义/恢复参数/切换设计）+ stage-2-25 交叉（枚举已提取——本篇补语义）+ my-xhs 实证（ReadWriteRoutingDataSource——应用层路由）+ `[无本地源码：mysql-connector-j]`。

**对比取舍**：知识本体是"**JDBC 连接层的多活机制**"——驱动级（透明）vs 应用层（可控——my-xhs 选应用层）、failOverReadOnly（安全 vs 灵活）、主动/被动切换（优雅 vs 果断）。

**待验证汇总**：
- 无（本篇类名均标 [无本地源码]；机制交叉已验证）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| Multi-Host 驱动模式 | ❌ 未用（JDBC URL 单主机） | 现状说明：应用层路由替代（可控） |
| 读写分离 | ✅ `ReadWriteRoutingDataSource`（stage-3-11——三策略——AbstractRoutingDataSource 路由模式） | 无（应用层实现——路由模式非 AOP 代理——2026-08-12 修正） |
| 故障转移/自动复联 | ⚠️ 主从哨兵/降级（stage-3-11） | 现状说明：驱动级 FAILOVER 未用 |
| 区域路由（JDBC 层） | ❌ 未挂 | 现状说明：区域路由在 LB 面（11 篇）——JDBC 层触发条件未到 |

### 差距清单

1. **P3**：JDBC 层区域路由（触发条件：多区域数据访问诉求——Multi-Host + Zone 映射）
2. **P3**：驱动级 FAILOVER 评估（应用层路由 vs 驱动级——触发条件：主从切换自动化诉求）

**结论**：17 篇——my-xhs **应用层路由（ReadWriteRoutingDataSource）替代驱动级 Multi-Host**（读写分离已落地——可控优先）；驱动级模式/区域路由为演进项（触发条件驱动）；无 P1/P2 差距。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 JDBC 多活设计文档（103 行）——模式/参数照录 + 设计节照录；机制发散标注；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：MySQL JDBC 多活的完整认知该讲什么

docs 是 JDBC 多活设计文档。完整还该包含：

1. **"JDBC 驱动是'零改造'的多活层"**（docs + 发散）：Multi-Host 模式（LB/Failover/Replication）——**JDBC URL 声明拓扑，驱动管理切换**——应用无感知（对比应用层路由——my-xhs ReadWriteRoutingDataSource——可控但改造）——**"驱动级 vs 应用级"是数据访问多活的第一决策**
2. **"failOverReadOnly 的写保护哲学"**（docs:36-44 + 发散）：故障期 Secondary 强制只读（true）——**"故障期间宁可只读不可误写"**——与 02 篇自我保护（AP 哲学）同思想（故障期保护数据）——**切换安全性的细节设计**
3. **"区域路由的 JDBC 挂载 = LOADBALANCE 模式"**（docs:70 + 发散）：docs 明确选 LB 模式（规则可扩展——AZ Locator 两对象：选一/选子集）——**"区域偏好作为 LB 规则"**（同区域主机优先）——**与 11 篇 LoadBalancer 的 ZonePreference 同机制、不同层**（服务实例层 vs 数据库主机层）
4. **"切换策略 × 关闭语义"**（docs:88-91 + 发散）：主动（配置/API——优雅关闭——在途事务完成）vs 被动（故障——立即关闭——果断止损）——**"关闭策略是切换类型的结果"**——优雅 vs 果断的二元设计（运维可控 vs 故障自动）
5. **"静态 vs 动态代理"**（docs:75-80 + 发散）：Connection 包装（Druid/P6Spy——细粒度）vs DataSource 代理（Spring AOP——创建层）——**"织入层的粒度决定能力"**（连接级能拦全部操作、数据源级只在创建时）
6. **"my-xhs 应用层路由的合理性"**（发散）：ReadWriteRoutingDataSource（SQL 级路由——读写分离三策略——stage-3-11）——**应用层路由能力更细（按 SQL/事务路由）vs 驱动级（按连接）**——my-xhs 选应用层是"能力优先"决策——驱动级 Multi-Host 作为"零改造"备选

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 驱动级 Multi-Host vs 应用层路由 | 零改造透明 vs 细粒度可控（my-xhs 选应用层） |
| failOverReadOnly true vs false | 故障期写保护 vs 可人工切写（生产默认 true） |
| LOADBALANCE 模式（区域挂载） vs 其他 | 规则可扩展 vs 固定语义（docs 明确选 LB） |
| 主动切换（优雅） vs 被动切换（立即） | 可控优雅 vs 果断止损 |
| 静态代理 vs 动态代理 | 连接级细粒度 vs 创建层便捷 |

### 常见坑/反模式

1. **故障期误写 Replica**：failOverReadOnly=false 且未主动 setReadOnly——故障转移后写进副本（数据不一致——docs:41-44 语义）
2. **恢复参数缺失**：secondsBeforeRetrySource/queriesBeforeRetrySource 未配——Primary 恢复后连接不复联（docs:46-49）
3. **URL 拓扑与 Zone 映射不维护**：主机列表变更后 Zone 映射过期——区域路由错乱（docs:63"稳定确定"的维护义务）
4. **切换关闭策略错配**：被动切换用优雅关闭——故障连接拖住在途请求（docs:90-91 二元设计）
5. **驱动级与应用级混用**：既用驱动 Multi-Host 又用应用层路由——双路由冲突（选一层）

### 生态位置

- **stage-4 教学主线**：**数据面组（16-19 第二篇）**——16 MySQL Server 多活 → **17 MySQL JDBC 多活（本篇：连接层）** → 18-19 Redis 多活 → 22 动态 JDBC
- **前后篇衔接**：stage-2-25（Connector/J 高可用——枚举交叉）；07 篇（AZ Locator——区域路由）；stage-3-11（读写分离落地）；16 篇（binlog——数据面组衔接）；22 篇（动态 JDBC——docs 顺序）
- **与源码提取的关系**：mysql-connector-j `[无本地源码]`；my-xhs ReadWriteRoutingDataSource 实证

**架构师视角结论**：本篇为 **JDBC 多活设计文档（103 行，java 块 2 个）**——Multi-Host 四模式（SINGLE/LB/Failover/Replication——驱动级多活）+ **FAILOVER 容灾语义**（动态代理 + failOverReadOnly 两分支 + 恢复参数——stage-2-25 枚举的语义详述）+ **JDBC 层多活设计**（URL→Zone 映射 + LOADBALANCE 挂载 + 主动/被动切换 + 优雅/立即关闭）——知识本体是"**JDBC 连接层的多活机制**"（驱动级透明 vs 应用层可控）；my-xhs **ReadWriteRoutingDataSource（应用层路由）已落地**（驱动级为演进备选——触发条件驱动）。
