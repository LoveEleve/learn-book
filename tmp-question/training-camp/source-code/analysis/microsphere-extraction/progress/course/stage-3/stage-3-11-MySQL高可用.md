# stage-3 · 第 11 节：第九节："高可用" MySQL 数据库 — 知识点提取

> 课程：stage-3 三高架构 第 11 节（数据组 11/12）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/11. 第九节："高可用"MySQL 数据库.md`
> 提取时间：2026-08-12 | 权重：核心（MySQL 高可用三主题——主从复制/读写分离/MGR）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）

---

## 一、本节概览

- **技术域**：MySQL 主从复制（GTID/binlog）、读写分离（JDBC 客户端路由）、MySQL Group Replication（Paxos 分布式状态机）
- **维度**：`[分布式问题]`（高可用/读写分离）+ `[分布式理论]`（MGR/Paxos）+ `[工程问题]`（配置/部署）
- **核心命题**：**数据库高可用的三条路径**（docs 主要内容）——①主从复制（异步/GTID）②读写分离（JDBC 客户端）③MGR 升级（Paxos 一致性替代异步复制）；docs 的读写分离为**空节**，知识本体在发散 + my-xhs 实证
- **知识点数**：7 个
- **前置**：stage-2 02（Paxos——MGR 核心）、03 篇（MySQL 主从 3306/3307 实证）、MySQL binlog 概念

## 前置条件清单
读者需先掌握：
1. **Paxos 共识**（stage-2 02——MGR 的 GCS 核心是 Paxos 实现）
2. **MySQL binlog/GTID** 基本概念
3. **注册中心/服务调用**（07/08 篇——数据库高可用的调用侧配合）
未达前置者，先补：stage-2 02

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **机制用现状讲**：my-xhs 读写分离（ReadWriteRoutingDataSource 三策略 + 降级）与 canal（binlog 消费）为 docs 目标的现代落地
- **docs 场景 vs 现状**：docs 用 mysql:5.7/GTID 手工搭建；my-xhs 生产 compose 主从（3306/3307）；MGR 未采用

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 主从复制机制（异步复制/GTID/binlog ROW）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（docs 术语源/副本为 5.7 时代；现代 8.x 同构） | **置信度**：High
- **前置**：binlog 概念
- **来源**：docs §主从复制（概念 + Docker 操作全文）+ my-xhs 实证
- **需求**：掌握主从复制的**机制与搭建**——异步复制、GTID、binlog 格式选择
- **自主实现**：若我设计——Master 开 binlog（ROW 格式）+ 全局事务 ID（GTID）→ 复制用户授权 → Slave CHANGE MASTER 自动定位 → START SLAVE
- **参考实现**（docs 操作全文 + 架构师）：**概念（docs）**——数据从源复制到一个或多个副本；**默认异步**（副本无需永久连接）；**优点 4 项（docs）**——横向扩展（读分散到副本）/数据安全（副本可暂停备份）/分析（副本上跑分析不伤源）/远程数据分发；**Master 配置（docs）**——`server_id=1` + `log_bin=mysql-bin` + `binlog_format=ROW` + `gtid_mode=ON` + `enforce-gtid-consistency=true`；**Slave 配置（docs）**——`server_id=2` + `read_only=ON` + `--skip-log-bin`（不级联）；**关联（docs）**——`CHANGE MASTER TO ... MASTER_AUTO_POSITION=1`（**GTID 自动定位**——免手动 binlog 文件名/位置）+ `START SLAVE`；**验证（docs §测试）**——源端 INSERT（如 users id=3 admin）→ 副本 SELECT 校验（docs 示例 3 rows）；**复制用户**——`GRANT REPLICATION SLAVE`；**术语更新（docs 参考）**——master/slave → source/replica（MySQL 8.0.21（2020）官方术语更新，机制不变）；**my-xhs 实证**——生产 compose 主从 **3306 主/3307 从**（03 篇已证：production docker-compose.yml:143/190）+ config 注释"4主4从→1(3306)"（2026-08-08 简化）
- **对比取舍**：**GTID 自动定位 vs 传统文件名+位置**——自动/一致（MySQL 5.6+）vs 手工易错——现代默认 GTID；**ROW vs STATEMENT binlog**——行级精确（CDC 需要）vs 体积小——**数据同步/CDC 场景必须 ROW**
- **测试佐证**：docs §Docker 操作（master/slave cnf + CHANGE MASTER 全文）+ my-xhs 生产 compose（03 篇）

### KP-02 复制备份与数据安全（mysqldump vs 原始文件 + 只读备份）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §使用复制进行备份
- **需求**：掌握基于复制的**备份策略**——备份副本而非源（docs 明确：副本可暂停关闭不影响源）
- **自主实现**：若我设计——数据备份走副本（暂停复制 → 快照 → 恢复）；大库用原始文件 + binlog/relay log 重建
- **参考实现**（docs）：**三种备份方式**——①**mysqldump**（库不大时）②**原始数据文件**（大库：备份数据文件 + 二进制日志/中继日志——副本故障可重建）③**只读备份**（源或副本置只读 → 备份 → 恢复读写）；**核心价值（docs）**——复制副本可暂停/关闭而不影响源运行 → 生成"实时"数据的有效快照
- **对比取舍**：**mysqldump vs 原始文件**——简单 vs 大库效率/重建能力——按库大小选
- **测试佐证**：docs §使用复制进行备份

### KP-03 Binlog 消费（JDBC Binlog 客户端/CDC——衔接 Canal/事件）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01（ROW binlog）
- **来源**：docs §更多参考（mysql-binlog-connector-java）+ my-xhs canal 实证 + 架构师
- **需求**：掌握 **binlog 消费模式（CDC）**——应用/中间件作为 binlog 客户端（docs 关联 JDBC Binlog 支持库）
- **自主实现**：若我设计——binlog 客户端（如 Canal）伪装 Slave 订阅 binlog（ROW）→ 解析变更 → 推事件（缓存同步/搜索同步/事件驱动）
- **参考实现**（docs 链接 + my-xhs 实证 + 发散）：**docs 参考**——`mysql-binlog-connector-java`（JDBC Binlog 客户端库——Java 直接消费 binlog）；**my-xhs 实证（Canal 落地）**——`config/canal/conf/`（canal.properties + **3 个实例**：`inventory_instance`/`note_instance`/`product_instance`）+ `docker-compose.yml:454-455`（`my-xhs-canal-server:v1.1.7`）；**实例配置实证**——`inventory_instance/instance.properties:10`（`canal.instance.master.address = 127.0.0.1:3306`——**连 MySQL 主库 binlog**）、`:18`（`canal.instance.filter.regex = my_xhs_inventory\\.t_inventory`——**同步库存表**）；**模式（发散）**——Canal 伪装 Slave 拉 ROW binlog → 变更事件 → 下游（缓存失效/ES 同步/库存联动——衔接 14 节分布式事件、12 节数据存储）
- **对比取舍**：**CDC（binlog 消费）vs 应用双写**——数据一致（源真实变更）vs 应用侵入——**CDC 是数据同步的标准路径**（docs 的 JDBC Binlog 客户端到 Canal 的演进）
- **测试佐证**：docs §更多参考 + my-xhs `config/canal/conf/inventory_instance/instance.properties:10/18` + docker-compose.yml:454-455

### KP-04 读写分离（docs 空节发散 + my-xhs 三策略路由实证）【docs 主要内容②】
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01、Spring 事务
- **来源**：docs §读写分离（**空节：仅标题**）+ 架构师发散 + my-xhs ReadWriteRoutingDataSource 实证
- **需求**：掌握**客户端读写分离**——docs 主要内容第 2 条"基于 JDBC 实现客户端读写分离"（docs 空节，知识本体在发散 + my-xhs 完整落地）
- **自主实现**：若我设计——`AbstractRoutingDataSource` 包装主从两数据源 + 路由策略（注解/手动/SQL 分析）+ **读库故障降级写库**
- **参考实现**（my-xhs 实证 + 发散）：**my-xhs 完整落地**——`common/datasource/ReadWriteRoutingDataSource.java`（**extends `AbstractRoutingDataSource`**，注释实证**三策略优先级**：①`DataSourceContextHolder` 手动指定 ②`@Transactional(readOnly=true)` 自动路由 ③SQL 分析自动路由（**SELECT → SLAVE，其他 → MASTER**）；**读库不可用自动降级写库 + 定期探测恢复**——容错设计）+ `DataSourceContextHolder`/`DataSourceType` + `config/ReadWriteRoutingDataSourceConfig.java:25/69`（装配 routingDataSource）；**机制（发散）**——Spring `AbstractRoutingDataSource` 按 lookupKey 切数据源；**一致性挑战（发散）**——主从延迟（读旧数据）→ 读己之写/关键路径强制走主（策略①手动指定）——my-xhs 三策略正是此解
- **对比取舍**：**客户端路由（应用层）vs 中间件路由（Proxy）**——应用层灵活/无额外组件 vs 透明/集中（Proxy 层如 ShardingSphere——stage-2 26 已提取）；my-xhs 选应用层（AbstractRoutingDataSource）
- **测试佐证**：my-xhs `ReadWriteRoutingDataSource.java`（类注释三策略 + 降级字段实证）+ `ReadWriteRoutingDataSourceConfig.java:25/69`

### KP-05 MGR 分布式状态机复制（原子广播/写集认证/冲突检测）【docs 主要内容③】
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-2 02（Paxos）
- **来源**：docs §MySQL Group Replication（背景/组复制）
- **需求**：掌握 **MGR 的一致性机制**——docs 主要内容第 3 条：用 MGR 替代异步主从提高一致性
- **自主实现**：若我设计——组内服务器=分布式状态机：读写事务广播（原子/全序）→ 写集认证冲突检测 → 组一致决定提交/回滚
- **参考实现**（docs）：**背景（docs）**——容错 = 组件冗余；复制协调 = 分布式系统问题（网络分区/脑裂）→ **服务器就每次状态转换达成一致 = 分布式状态机**；**组复制协议（docs）**——复制组各服务器独立执行事务，**读写事务须组批准才提交**（非源单方面决定），只读事务免协调；**提交流程**——广播写入值 + 写集（更新行唯一标识）→ **原子广播**（全收或全不收、全序）→ **认证（certification）**：行级冲突检测（并发事务更新同行 → 冲突）→ **首次提交获胜**（先排序者提交，后者回滚）；**最终一致**（docs）——流量停止后组成员数据相同；外部化顺序可偏离全局顺序（无冲突时）；**单主/多主模式**——单主（仅一台接受更新）/多主（全部可更新，应用须绕过限制）；**一致性保证**——成员身份服务/视图一致/脑裂保护（无多数则阻塞）
- **对比取舍**：**MGR（强一致组共识）vs 异步主从（最终一致）**——一致性 vs 性能/复杂度（docs 主要内容③的"升级"动机）；**Paxos 实现（XCom）为 GCS 引擎**（stage-2 02 衔接）
- **测试佐证**：docs §背景/§组复制（原子广播/写集/认证/冲突全文）+ docs §参考资料（淘宝月报《Group Replication 内核解析》+ MySQL Docker 部署指南——MGR 深挖参考）

### KP-06 MGR 组成员与故障检测（视图/化身/怀疑/驱逐/法定人数）
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-05
- **来源**：docs §组复制细节（组成员关系/故障检测/容错）
- **需求**：掌握 **MGR 的成员管理与容错数字**——5s 怀疑/10s 传播驱逐/n=2f+1（docs 精确数字）
- **自主实现**：若我设计——成员视图共识 + 故障检测（怀疑 → 共识确认 → 驱逐）+ 法定人数（n=2f+1）
- **参考实现**（docs 精确细节）：**组成员关系**——组名 UUID、动态加入/离开（自动补状态/自动重配置触发视图变更）；**化身（incarnation，5.7.22+）**——服务器重加入唯一标识符（防"旧化身与新知化身"混淆导致 XCom 共识冲突——新化身被阻直到旧化身移除）；**故障检测**——成员间**点对点全连接图**（XCom 管理，TCP 双通道：发/收）；**5 秒**未收到消息 → 怀疑该成员（replication_group_members 状态 **UNREACHABLE**）；**10 秒** → 传播怀疑（仅"通知者"节点）；共识确认 → 驱逐；**容错（docs 公式）**——**n = 2f + 1**（容忍 f 故障需 2f+1 服务器；容忍 1 故障 → 3 台；2 台非自愿故障 → 阻塞——无多数无法决策）；**脑裂防护**——无多数在线则阻止动态配置变更（需管理员干预）
- **对比取舍**：**Paxos 法定人数 vs 异步复制无共识**——多数决策（容忍 n-1 台可继续服务）vs 单点源——MGR 的可用性模型 = 3 台起
- **测试佐证**：docs §组成员关系/§故障检测/§容错（5s/10s/n=2f+1 原文）

### KP-07 数据库高可用选型（主从 vs MGR vs 多活）【现状核对】
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01/05
- **来源**：docs §使用场景 + 架构师整合 + my-xhs 现状
- **需求**：掌握高可用方案的**选型矩阵**——docs 使用场景（弹性复制/高可用碎片/源副本替代）+ 现状对照
- **自主实现**：若我设计——按"一致性要求 × 写扩展需求"选：异步主从（读扩展/最终一致）→ 半同步 → MGR（组共识/强一致）→ 多活（区域）
- **参考实现**（docs 场景 + my-xhs 现状 + 发散）：**MGR 典型用例（docs）**——弹性复制（云数据库动态增减）/高可用碎片（每个分片映射复制组）/源副本替代（单源是争用点）/自主系统（协议内置自动化）；**客户端故障转移（docs 明确）**——MGR 不管连接重定向（MySQL Router/连接器/负载均衡器中间件处理）；**my-xhs 现状**——**异步主从**（3306/3307 实证）+ **读写分离**（KP-04 三策略）+ **无 MGR**（现状说明：读多写少场景主从+读写分离满足；MGR 需 3 节点 + 强一致诉求——当前业务无需）；**演进路径（发散）**——需要写高可用时：半同步 → MGR（3 节点）
- **对比取舍**：**选型结论**——读扩展 → 主从+读写分离（my-xhs 现状已满足）；写高可用/强一致 → MGR；区域级 → 多活（03 篇 zone）
- **测试佐证**：docs §使用场景 + my-xhs 生产 compose（3306/3307，03 篇）+ KP-04 实证

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 主从复制机制（GTID/binlog ROW） | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| 复制备份与数据安全 | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| Binlog 消费（CDC/Canal） | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |
| 读写分离（客户端路由） | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| MGR 分布式状态机复制 | 分布式理论 | 核心 | P1 | 🔴 | 有效 | High |
| MGR 组成员与故障检测 | 分布式理论 | 核心 | P1 | 🔴 | 有效 | High |
| 数据库高可用选型 | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：my-xhs（读写分离/canal/主从）+ JDK 不适用
- **关键源码**（本次实证）：
  - `common/datasource/ReadWriteRoutingDataSource.java`（extends AbstractRoutingDataSource——三策略注释实证 + slaveUnavailable 降级字段）+ `DataSourceContextHolder`/`DataSourceType` + `config/ReadWriteRoutingDataSourceConfig.java:25/69`
  - `config/canal/conf/inventory_instance/instance.properties:10`（master.address=127.0.0.1:3306）/`:18`（filter.regex=my_xhs_inventory.t_inventory）+ docker-compose.yml:454-455（canal-server v1.1.7）
  - 生产 compose 主从 3306/3307（03 篇实证）
- **诚实标注**：docs §读写分离为**空节**（仅标题）→ KP-04 以 my-xhs 完整落地实证补全（08 §2 最佳案例：docs 空节 + 实例即知识本体）；docs 部署为**作业标注**（§部署"作为作业独立完成~"）→ 选型对照处理；docs 用 mysql:5.7（8.x 同构机制，术语源/副本迁移）
- **关联标注**：stage-2 02（Paxos——MGR 核心）；stage-2 26（ShardingSphere——读写分离的中间件形态对照）；03 篇（MySQL 主从实证）；14 节（分布式事件——binlog/CDC 衔接）

---

## 五、本节小结（三层次视角）

**需求**：MySQL 高可用三路径——主从复制（GTID）、读写分离（JDBC 客户端）、MGR（Paxos 组共识）。

**自主实现核心**：若我设计——①主从：ROW binlog + GTID 自动定位（免手工位点）②读写分离：AbstractRoutingDataSource 三策略（注解/手动/SQL 分析）+ 读库降级 ③强一致写：MGR 3 节点（写集认证/冲突回滚）④CDC：Canal 伪装 Slave 拉 binlog 推事件。

**参考实现**：docs（主从操作全文 + MGR 机制精确数字）+ **my-xhs 实证**（ReadWriteRoutingDataSource 三策略降级、Canal 3 实例 binlog 消费、生产主从 3306/3307）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**数据库高可用与数据流动**"——异步复制（读扩展）→ 读写分离（应用层路由 + 降级）→ 组共识（写一致）；docs 空节（读写分离）由 my-xhs 完整落地补全——**B 模式（提取+现状核对）价值最大的一篇**。

**待验证汇总**：
- my-xhs 主从延迟监控与"读己之写"处理（三策略的手动指定使用面）
- Canal 3 实例的下游消费（缓存/ES/事件）——14 节展开
- MGR 若引入的评估（当前无强一致写诉求）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 目标（升级动作） | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| ① 主从复制搭建 | ✅ **已落地**：生产 compose 3306 主/3307 从（03 篇实证）+ config 注释"4主4从→1(3306)"（2026-08-08 简化） | 无（一主一从，docs 一主多从——从库数量按需扩） |
| ② 读写分离（JDBC 客户端） | ✅ **完整落地**：ReadWriteRoutingDataSource（AbstractRoutingDataSource + 三策略：手动/@Transactional(readOnly)/SQL 分析 + **读库故障自动降级写库 + 定期探测恢复**） | 无（docs 空节目标的现代完整实现） |
| ③ MGR 架构升级 | ❌ 未采用（异步主从现状） | 现状说明：读多写少场景主从+读写分离已满足；MGR 需 3 节点 + 强一致写诉求——**决策待定**（无写高可用诉求时不作为差距） |
| Binlog 消费（docs 参考：JDBC Binlog 客户端） | ✅ **已用 Canal 落地**：3 实例（inventory/note/product）+ instance.properties（3306 主库 + 表过滤实证） | 下游消费面（缓存/ES/事件）`[待验证]`——14 节核对 |
| 备份策略 | ⚠️ 未发现备份脚本/策略证据 | 差距：基于副本的备份（mysqldump/原始文件）未显式落地 `[待验证]` |

### 差距清单（数据层）

1. **P1**：备份策略显式化（docs 02 篇 KP-02：副本备份/大库原始文件方案）——当前未发现证据
2. **P2**：Canal 下游消费核对（3 实例同步的表 → 缓存/ES/事件去向）
3. **P3**：MGR 决策（无写高可用诉求 → 维持主从；如订单/支付强一致写场景评估）

**结论**：11 篇数据高可用落地度**高**——docs 主要内容 ①② 均已完整落地（读写分离含降级容错是亮点），③ MGR 属"决策待定"非差距；**最大缺口 = 备份策略无证据**。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 MySQL 官方文档转写（主从操作/MGR 精确数字）+ 读写分离空节；my-xhs 实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：MySQL 高可用的完整认知该讲什么

docs 覆盖主从/MGR。完整还该包含：

1. **"读写分离的核心难题是主从延迟"**（docs 空节 + 发散）：SELECT 走从库读到旧数据——**读己之写/强一致读必须路由主库**（my-xhs 策略①手动指定即此）；延迟监控（Seconds_Behind_Master）是必修
2. **降级容错是读写分离的生产必修**（发散）：读库故障若硬路由 → 读全面挂——**降级写库 + 恢复探测**（my-xhs slaveUnavailable 实证）——docs 空节未提，生产必须
3. **CDC 是"数据流动"的标准路径**（docs 参考 + 发散）：binlog 消费（Canal）→ 缓存/搜索/事件——**ROW 格式是前提**（docs 配置已选 ROW——正确）；14 节分布式事件与 12 节数据存储都依赖它
4. **MGR 的"一致性代价"**（docs + 发散）：强一致 = 每次提交组共识（Paxos 多数）——**写吞吐上限低于异步复制**；冲突回滚（写集认证）要求**热点行尽量同一服务器写**（docs 明确：冲突事务最好同源启动）——设计指引
5. **数据库高可用是"分层"的**（发散）：异步复制（读扩展）→ 半同步（写不丢）→ MGR（组共识）→ 区域多活（03 篇 zone）——**按一致性与可用性要求逐级升级**（docs 主要内容③正是此路径）
6. **客户端故障转移不在数据库内**（docs 明确）：MGR 不管连接重定向——**MySQL Router/连接器/网关层负责**（08 篇注册中心 + 19 节网关衔接）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 异步复制 vs 半同步 vs MGR | 性能 vs 一致性（docs 主要内容③主线） |
| ROW vs STATEMENT binlog | CDC 精确 vs 体积（数据同步必须 ROW） |
| GTID 自动定位 vs 手工位点 | 免错 vs 旧机制 |
| 应用层路由 vs 中间件（Proxy） | 灵活 vs 透明（my-xhs vs ShardingSphere） |
| 读走从库 vs 全走主 | 扩展 vs 延迟风险（降级+策略配合） |
| 单主 vs 多主 MGR | 简单一致 vs 全部可写（冲突成本） |
| 备份副本 vs 备份源 | 无影响 vs 停机 |

### 常见坑/反模式

1. **无降级的读写分离**：从库故障读全挂——降级写库必修（my-xhs 实证）
2. **读己之写读从库**：写后立刻读从库 → 旧数据——强一致读路由主（策略①）
3. **CDC 用非 ROW binlog**：Statement 格式解析歧义——ROW 是前提（docs 配置正确示范）
4. **MGR 少于 3 节点**：n=2f+1——2 节点无多数容错能力（docs 公式）
5. **热点行多主并发写**：写集认证冲突回滚——热点尽量单点写（docs 明确）
6. **备份打在源上**：影响生产——副本备份（docs 明确）
7. **忽略延迟监控**：主从延迟不监控 → 读扩展是纸面繁荣

### 生态位置

- **stage-3 教学主线**：数据组（11/12）——**11 数据库高可用（本篇）** → 12 数据存储（缓存/分片/存储引擎）；之后 13-17 事件/Reactive 组
- **前后篇衔接**：stage-2 02（Paxos——MGR 核心）/26（ShardingSphere——读写分离中间件形态）→ 本篇 → 12 节（数据存储深化）；03 篇（MySQL 主从实证）；14 节（分布式事件——binlog/CDC 衔接）
- **与源码提取的关系**：my-xhs 数据层（ReadWriteRoutingDataSource/Canal）为核心参考源；MGR 为 MySQL 内建（无本地源码，docs 机制 + 官方文档）

**架构师视角结论**：本篇以 **my-xhs 实证讲数据高可用**（读写分离三策略+降级、Canal CDC、主从 3306/3307）、docs 讲主从/MGR 机制（GTID/写集认证/Paxos）——知识本体是"**数据库高可用分层**"（异步复制→读写分离→组共识）与"**数据流动**"（binlog→CDC）；docs 空节（读写分离）由 my-xhs 完整落地补全——B 模式价值最大的一篇。
