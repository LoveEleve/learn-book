# stage-4 · 第 22 节：第十九节：动态 JDBC 组件多活架构 — 知识点提取

> 课程：stage-4 多活架构 第 22 节（动态 JDBC 面——多活组收官）
> 来源 docs：`/data/workspace/java-training-camp/stage-4/docs/22. 第十九节：动态 JDBC 组件多活架构.md`
> 提取时间：2026-08-12 | 权重：核心（动态 JDBC——配置驱动 + 子上下文隔离 + 动态换源；**多活收官篇**）
> 案例载体：my-xhs（决策 B）+ 现状核对（决策 B2）——**microsphere 框架文档（非 Eureka 文档）——机制 = 动态 JDBC 模块化 + 区域化数据源**

> **文档形态**：**最长篇（554 行，代码块 13 个——awk 计数实证）**——主要内容 3 条：①动态 Spring 应用上下文 + AZ Locator——**MySQL DataSource 同区域优先/动态路由/多活**（docs:2）②插件机制——**单应用 MyBatis/MyBatis-Plus/JPA 并存 + 独立事务（本地/分布式）+ 分库分表（ShardingSphere）**（docs:3）③**Actuator 指标/健康检查**（docs:4）；正文 = 配置示例（3 个）+ 核心 API 体系（6 模块/3 SPI 族/子上下文/DynamicDataSource）。

---

## 一、本节概览

- **技术域**：动态 JDBC（配置驱动模块化/子上下文隔离/动态换源/区域化数据源）、Spring SPI 族（校验/后置处理/属性合成）
- **维度**：`[工程问题]`（配置驱动/装配）+ `[分布式问题]`（区域化数据源/多活）+ `[分布式理论]`（框架并存的事务隔离）
- **核心命题**：**动态 JDBC 组件体系**——docs 三主线：①配置驱动（JSON/Properties——模块化 + 区域化数据源 ha-datasource）②子上下文隔离（**打破 MyBatis/MyBatis-Plus 互斥**——docs:131）③动态换源（DynamicDataSource——运行时换 DataSource）；**知识本体 = "动态 JDBC 的模块化与动态化机制"**
- **知识点数**：4 个
- **前置**：17 篇（JDBC 多活）、stage-3-12（MyBatis/ShardingSphere）、07 篇（AZ Locator）

## 前置条件清单
读者需先掌握：
1. **JDBC 多活**（17 篇——Multi-Host/DataSource 层）
2. **MyBatis/ShardingSphere**（stage-3-12——my-xhs 落地）
3. **AZ Locator**（07 篇——区域化数据源衔接）
4. **Spring 装配**（04 篇——ConfigurationProperties/自动装配）
未达前置者，先补：17 篇 / stage-3-12

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **配置要点提取**：13 个代码块（配置示例 + 类源码）——配置示例提取关键（区域化/模块依赖），类源码提取机制
- **发散为本**："动态"的本质（子上下文隔离/动态换源）——机制发散
- **实例对照**：my-xhs ShardingSphere 订单 + MyBatis-Plus（stage-3-12 实证）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 动态 JDBC 需求与定位（需求 5 条/交付 2 条——多活+动态+框架并存）【docs §项目背景】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：无
- **来源**：docs §项目背景（docs:8-27）
- **需求**：**动态 JDBC 的定位**——docs 明确：需求清单 5 条（**升级 ShardingSphere 5.x/兼容 MyBatis+MyBatis-Plus/支持数据库事务/动态变化能力/多活架构能力**——docs:14-18）；交付功能 2 条（**JDBC 功能模块化（MyBatis/MyBatis-Plus/Transaction/多 DataSource/多活）+ Spring Boot 高度集成（配置驱动）**——docs:21-22）
- **自主实现**：若我设计——动态 JDBC = **"配置驱动的 JDBC 功能组合框架"**：模块化（各功能独立配置）+ 动态化（运行时可变）+ 多活（区域化数据源）
- **参考实现**（docs 照录 + 本地验证）：**需求 5 条（docs:14-18 照录）**——ShardingSphere 5.x/MyBatis 兼容/事务/动态/多活；**交付 2 条（docs:21-22 照录）**——模块化 + Boot 配置驱动；**项目（docs:10/25 照录）**——microsphere-multiactive（多活基础——07-19 篇已提取）+ **microsphere-dynamic**（`[本地存在：microsphere-dynamic-jdbc-spring-boot 模块——内容待 source/ 核对]`）；**子项目（docs:27）**——Dynamic JDBC Spring Boot Starter
- **对比取舍**：**配置驱动（模块组合——声明式）vs 代码装配（显式）**——组合灵活 vs 可控——**docs 选配置驱动（Boot 高度集成——docs:22）**
- **机制/说明**：动态 JDBC 定位 = **"JDBC 能力的配置化组合"**——MyBatis/ShardingSphere/事务/多数据源/多活——**全部配置声明**（docs:21-22 交付）——**"一个应用按配置组合 JDBC 能力"**
- **测试佐证**：docs:8-27（照录）+ `[本地存在待核对]`

### KP-02 配置驱动模块化（ha-datasource 区域化 + 模块依赖拓扑 + 互斥共存）【docs §配置驱动】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-01、07 篇（AZ Locator）
- **来源**：docs §功能特性配置驱动（docs:31-171——3 个配置示例）
- **需求**：**配置驱动的模块化设计**——docs 明确：**简单配置**（JSON——datasource（HikariDataSource——**docs:41 `com.mysql.jdbc.Driver` 旧驱动 `[过时→com.mysql.cj.jdbc.Driver——stage-3-03 已提]`**）+ sharding-sphere（config-resource）+ mybatis（base-packages）——docs:35-53）；**复杂配置 1**（Properties——**ha-datasource：defaultZone + test-zone——区域化数据源**（docs:68-80）+ transaction + sharding-sphere + mybatis/mybatis-plus）；**复杂配置 2**（JSON——**ha-datasource defaultZone 带池参数**（maxLifetime/connectionTimeout/validationTimeout/idleTimeout/maxPoolSize——docs:143-147）+ **ha-datasource 支持（DataSource Key = 区域 Zone——多活——docs:170）+ DataSource 支持 Primary 指定**（docs:171））；**模块依赖拓扑**（mybatis < sharding-sphere < datasource——docs:58/128；mybatis-plus < sharding-sphere < datasource——docs:129）；**互斥模块共存**（docs:131——**打破 mybatis 与 mybatis-plus 不能同时用**）
- **自主实现**：若我设计——配置模块化：**模块声明**（JSON/Properties）+ **依赖拓扑**（datasource 底座 → sharding-sphere 增强 → mybatis 消费）+ **区域化**（ha-datasource——Zone Key）
- **参考实现**（docs 配置要点提取 + 07 篇衔接）：**ha-datasource 区域化（docs:170 照录）**——**DataSource Key = 区域（Zone）——多活支持**（docs:170——**07 篇 AZ Locator 的配置面**——区域偏好数据源选择）；**Primary 指定（docs:171）**——多数据源主备声明；**模块依赖（docs:58/128-129 照录）**——**mybatis < sharding-sphere < datasource**（**模块拓扑——底层模块先装配**）；**互斥共存（docs:131 照录）**——MyBatis 与 MyBatis-Plus 同应用（docs:131——**子上下文隔离的动机——KP-03 机制**）；**池参数（docs:143-147 照录）**——区域数据源可配连接池（maxLifetime 等）
- **对比取舍**：**模块化配置（声明组合）vs 单框架集成**——组合灵活 vs 简单——**docs 的模块化解决"多框架并存"**（MyBatis/Plus/Sharding/事务——按需组合）
- **机制/说明**：配置驱动模块化 = **"JDBC 能力的模块声明 + 依赖拓扑 + 区域化"**——ha-datasource（Zone Key——多活数据源）+ 模块依赖（装配顺序）+ 互斥共存（子上下文——KP-03）
- **测试佐证**：docs:31-171（配置要点提取——非全文照录）+ 07 篇（区域衔接）+ `[过时→驱动名]` 标注

### KP-03 动态 JDBC 核心机制（6 模块常量 + 3 SPI 族 + 子上下文隔离 + DynamicDataSource 动态换源）【docs §核心 API】
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-02、04 篇（Spring 装配）
- **来源**：docs §核心 API（docs:174-539——模块常量/SPI 族/子上下文/DynamicDataSource）
- **需求**：**动态 JDBC 的核心机制**——docs 明确：**6 模块常量**（datasource/ha-datasource/transaction/sharding-sphere/mybatis/mybatis-plus——docs:180-190）+ **3 SPI 族**（ConfigValidator 校验器——docs:331-354/ConfigPostProcessor 后置处理器——docs:356-367/ConfigConfigurationPropertiesSynthesizer 属性合成器——docs:369-404——**各 5 个模块实现（DataSource + Transaction/ShardingSphere/Mybatis/MybatisPlus 四模块——2026-08-12 修正：初稿"6 实现"凭印象——实为 5）**——**合成 Spring Boot @ConfigurationProperties**（transaction.properties.defaultTimeout → spring.transaction.defaultTimeout——docs:393）——**设计思想：尽可能复用 Spring Boot 组件**（docs:406））+ **子应用上下文**（DynamicJdbcChildContext——**继承 AnnotationConfigApplicationContext——模块独立运行互不影响——互斥模块可共存**（docs:426-428）+ Parent Context 可依赖查找 Child Bean（@Autowired @Mapper——docs:491））+ **DynamicDataSource 动态换源**（**与传统 AbstractRoutingDataSource 不同**（docs:494）——afterPropertiesSet → initializeDataSource（**动态创建 ChildContext → 获取 DataSource → 交换 delegate + 关闭旧 Context**——docs:496-535））
- **自主实现**：若我设计——动态 JDBC 核心机制三件：**①模块配置体系**（Config 对象 + 校验/后置处理/属性合成三 SPI——复用 Spring Boot 装配）②**子上下文隔离**（每配置一个 ChildContext——互斥模块独立运行）③**动态换源**（运行时创建新 ChildContext → 换 DataSource delegate → 关闭旧的——**"动态"的本质**）
- **参考实现**（docs 机制照录 + 发散 + 本地验证）：**模块常量（docs:180-190 照录）**——6 模块（新增模块先加常量——docs:192）；**Config 通用属性（docs:223-227 照录）**——name（唯一）/configurations（自定义 Configuration 类）/properties（多层 Map）；**3 SPI 族（docs:331-404 照录）**——Validator（配置校验）/PostProcessor（配置后处理）/Synthesizer（**属性合成——复用 Spring Boot @ConfigurationProperties——docs:393——"尽可能复用 Spring Boot 组件"——docs:406——**04 篇 ConfigurationProperties 机制的应用**）；**子上下文（docs:425-491 照录）**——DynamicJdbcChildContext（AnnotationConfigApplicationContext 子类——docs:426——**模块独立上下文——互斥共存的基础**）+ Parent 查找 Child Bean（docs:491）；**DynamicDataSource（docs:493-535 照录）**——**与传统 AbstractRoutingDataSource 不同**（docs:494——**动态换源而非静态路由**——对比 17 篇 my-xhs ReadWriteRoutingDataSource）+ initializeDataSource（**创建 ChildContext（mergeParentEnvironment → refresh → 获取 DataSource → synchronized 交换 delegate → 关闭旧 Context**——docs:508-532）；**激活（docs:538-539）**——@EnableDynamicJdbcAutoConfiguration；**本地验证**——microsphere-dynamic 本地存在（模块结构——类名待 source/ 核对 `[本地存在待核对]`）
- **对比取舍**：**子上下文隔离（互斥共存——每模块独立容器）vs 单上下文强制共存**——隔离 vs 冲突——**docs 用子上下文（docs:409-410——"模块可以在独立的 Spring 应用上下文中运行"）**；**动态换源（DynamicDataSource——运行时换）vs 静态路由（AbstractRoutingDataSource——查找键切换）**——动态 vs 静态——**docs 选动态（创建/交换/关闭——docs 494 明示不同）**
- **机制/说明**：动态 JDBC 的核心 = **"子上下文隔离 + 动态换源"**——**每配置一个 ChildContext**（模块独立——互斥可共存）+ **DynamicDataSource 运行时换 delegate**（动态变化能力——docs:17 需求）——**"动态" = 运行时可替换**（创建新上下文 → 换源 → 关旧）——与 04 篇 refresh scope（重建 Bean）同思想
- **测试佐证**：docs:174-539（机制照录）+ 04 篇（ConfigurationProperties 交叉）+ 17 篇（AbstractRoutingDataSource 对比）+ `[本地存在待核对]`

### KP-04 现状核对（my-xhs：ShardingSphere 订单 + MyBatis-Plus——动态 JDBC 对应面）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01~03
- **来源**：my-xhs 实证（stage-3-12 交叉）+ 架构师整合
- **需求**：以动态 JDBC 为尺——my-xhs 的 JDBC 组合面
- **自主实现**：若我设计——核对：MyBatis-Plus（有）/ShardingSphere（有——订单）/动态（无）/区域化数据源（无）
- **参考实现**（my-xhs 实证 + 交叉）：**MyBatis-Plus ✅**（stage-3-12——全量采用——JPA 无）；**ShardingSphere ✅ 订单面**——`my-xhs-order/.../config/ShardingSphereDataSourceConfig.java`（session004 源码索引实证——**订单分片**）；**动态 JDBC ❌ 未用**（`[现状：静态配置——非动态 JDBC 框架——各框架独立配置]`——触发条件：多框架动态组合诉求）；**区域化数据源 ❌**（`[现状：17 篇 ReadWriteRoutingDataSource（读写分离）——非 Zone Key 数据源]`——07 篇 AZ 衔接）；**Actuator 指标/健康检查（docs:4 意图）✅**（Boot 内建——stage-3 03 实证——prometheus 采集）；**专题突破（docs:541-554）`[跳过：事务性内容（求职/面试建议）]`**
- **对比取舍**：**静态配置（my-xhs——各框架独立）vs 动态 JDBC（组合框架——配置驱动）**——简单 vs 动态组合——**my-xhs 场景（MyBatis-Plus + 订单 Sharding——够用）**
- **测试佐证**：my-xhs `ShardingSphereDataSourceConfig.java`（实证）+ stage-3-12（交叉）+ `[跳过]` 标注

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 动态 JDBC 需求与定位 | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| 配置驱动模块化（ha-datasource） | 工程问题 | 核心 | P1 | 🟡 | 有效 | High |
| 核心机制（子上下文/动态换源） | 工程问题 | 核心 | P1 | 🔴 | 有效 | High |
| 现状核对（ShardingSphere+MyBatis-Plus） | 工程问题 | 支撑 | P2 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：microsphere-dynamic（`[本地存在：microsphere-dynamic-jdbc-spring-boot 模块——类名待 source/ 核对（按新方式不深挖）]`）；my-xhs（ShardingSphereDataSourceConfig——session004 索引实证）；stage-3-12（MyBatis/ShardingSphere 交叉）
- **关键实证**（交叉引用）：stage-3-12（MyBatis-Plus/ShardingSphere——my-xhs 落地）；17 篇（AbstractRoutingDataSource 对比——DynamicDataSource 差异）；04 篇（ConfigurationProperties——Synthesizer 复用）
- **诚实标注**：docs 为**最长篇（554 行，代码块 13 个 awk 实证）**——配置示例 3 个**按要点提取（非全文照录）**；类源码块（Config 内部类/SPI/Registrar/DynamicDataSource）**机制提取**；**docs:41 `com.mysql.jdbc.Driver` 旧驱动 `[过时→com.mysql.cj.jdbc.Driver]`**；**docs:541-554 专题突破 `[跳过：事务性内容]`**；**docs:369-406 Synthesizer 的"尽可能复用 Spring Boot 组件"为设计思想（docs:406 照录）**；`[本地存在待核对：microsphere-dynamic 类名——source/ 提取时]`
- **关联标注**：17 篇（JDBC 多活）；stage-3-12（MyBatis/Sharding）；07 篇（AZ Locator——区域化数据源）；04 篇（Spring 装配）；01 篇（多活收官）

---

## 五、本节小结（三层次视角）

**需求**：动态 JDBC 组件体系——配置驱动组合 JDBC 能力（多活 + 动态 + 框架并存）。

**自主实现核心**：①**模块化配置**（JSON/Properties 声明——ha-datasource 区域化（Zone Key）+ 模块依赖拓扑）②**子上下文隔离**（每配置一个 ChildContext——互斥模块共存）③**动态换源**（DynamicDataSource——创建/交换/关闭——"动态"本质）④**3 SPI 族**（校验/后置/属性合成——复用 Spring Boot 装配）。

**参考实现**：docs 机制照录（配置要点 + 类机制——非全文）+ my-xhs 实证（ShardingSphere 订单 + MyBatis-Plus）+ 17/04 篇交叉。

**对比取舍**：知识本体是"**动态 JDBC 的模块化与动态化机制**"——子上下文隔离（互斥共存）vs 单上下文、动态换源（运行时替换）vs 静态路由（AbstractRoutingDataSource）、配置驱动（声明组合）vs 代码装配。

**待验证汇总**：
- microsphere-dynamic 类名（`[本地存在待核对]`——source/ 提取时）
- my-xhs 动态 JDBC 采用评估（`[决策待定]`——多框架动态组合诉求）

---

## 六、现状核对（my-xhs 落地核对与差距清单）

### 现状核对表

| docs 主题 | my-xhs 现状（实证） | 差距/行动项 |
|---|---|---|
| MyBatis-Plus | ✅ 全量（stage-3-12） | 无 |
| ShardingSphere | ✅ 订单分片（`ShardingSphereDataSourceConfig`） | 无（分片规则细节 P3——stage-3-12 差距） |
| 动态 JDBC 框架 | ❌ 未用（静态配置） | 现状说明：多框架动态组合诉求未触发 |
| 区域化数据源（ha-datasource） | ❌ 无（ReadWriteRoutingDataSource 读写分离——非 Zone Key） | 现状说明：区域数据源触发条件未到（07 篇衔接） |
| Actuator 指标/健康检查 | ✅ Boot 内建（stage-3 03——prometheus） | 无 |

### 差距清单

1. **P3**：动态 JDBC 采用评估（触发条件：多框架动态组合/配置化诉求）
2. **P3**：区域化数据源（触发条件：多区域数据访问——17 篇同）

**结论**：22 篇——my-xhs **MyBatis-Plus + 订单 ShardingSphere 已落地**（静态配置——够用）；动态 JDBC 框架/区域化数据源为演进项（触发条件驱动）；**stage-4 多活组全部完成（01-19+22——除 20/21 缺失）**。

---

## 七、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为最长篇（554 行）——配置要点/类机制照录；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：动态 JDBC 的完整认知该讲什么

docs 是框架设计文档。完整还该包含：

1. **"子上下文隔离 = 互斥共存的容器解"**（docs:409-410 + 发散）：MyBatis 与 MyBatis-Plus 单上下文互斥（docs:131）——**子上下文（每配置一个容器）让互斥模块各占其域**——与 04 篇 NamedContextFactory（每客户端子上下文）**同机制**（Spring 生态的隔离范式）
2. **"动态换源 vs 静态路由"**（docs:493-535 + 发散）：AbstractRoutingDataSource（查找键静态切换——17 篇 my-xhs 用）vs **DynamicDataSource（运行时创建新上下文换源——"动态"）**——**"动态"的成本**（创建/刷新/交换/关闭——docs:508-532——比静态路由重）——**按变化频度选**（低频静态/高频或配置化动态）
3. **"3 SPI 族的装配哲学"**（docs:331-406 + 发散）：校验（Validator）→ 后置处理（PostProcessor）→ 属性合成（Synthesizer——**复用 Spring Boot @ConfigurationProperties**——docs:393/406）——**"复用框架组件而非自建"是 Microsphere 的设计原则**（docs:406 明示）——**与 08 篇"学机制不照搬"呼应的框架级实践**
4. **"区域化数据源 = 多活的数据访问面"**（docs:170 + 07 篇衔接）：**DataSource Key = Zone**（docs:170）——**区域偏好选择数据源**（同区域数据源优先——07 篇机制的数据层挂载）——**17 篇（Multi-Host）与 22 篇（Zone Key）是 JDBC 多活的两形态**（驱动级 vs 框架级）
5. **"配置驱动的可维护性"**（docs:21-22 + 发散）：全部能力配置声明（JSON/Properties）——**"配置即架构"**（组合关系可读可版本化）——**与 12 篇"声明即负载均衡"同思想**（声明式优于代码式）
6. **"my-xhs 的够用场景"**（发散）：MyBatis-Plus + 订单 Sharding——**静态配置够用**（无多框架动态组合诉求）——动态 JDBC 的价值在"复杂组合 + 动态变化"场景（触发条件驱动）

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 子上下文隔离 vs 单上下文 | 互斥共存 vs 简单（docs 选隔离） |
| 动态换源 vs 静态路由 | 运行时替换 vs 查找键切换（按变化频度） |
| 配置驱动 vs 代码装配 | 声明组合 vs 显式可控 |
| 复用 Spring Boot 组件 vs 自建 | 生态复用 vs 定制（docs:406 明示复用） |
| ha-datasource（Zone Key） vs 静态多数据源 | 区域偏好 vs 固定（多活诉求） |

### 常见坑/反模式

1. **动态换源当静态用**：DynamicDataSource 的创建/交换/关闭有成本——低频场景用静态路由（17 篇）
2. **子上下文 Bean 访问**：Parent 查 Child Bean 需 @Autowired 委托（docs:491）——直接注入会失败
3. **模块依赖顺序错**：mybatis < sharding-sphere < datasource（docs:58）——装配顺序错则模块冲突
4. **旧驱动名**：com.mysql.jdbc.Driver（docs:41）→ com.mysql.cj.jdbc.Driver（过时标注）
5. **配置模块名冲突**：name 唯一性（docs:225）——重名配置覆盖/冲突
6. **互斥误以为解决**：子上下文隔离是容器级——框架内部自身的兼容性仍要验证

### 生态位置

- **stage-4 教学主线**：**多活组收官**——01-19（概念/Eureka/通用化/负载均衡/REST/Dubbo/网关/数据面）+ **22 动态 JDBC（本篇收官）**——**stage-4 全部完成**（20/21 缺失——docs 编号跳空）
- **前后篇衔接**：17 篇（JDBC 多活——AbstractRoutingDataSource 对比）；stage-3-12（MyBatis/Sharding）；07 篇（AZ Locator——区域化数据源）；04 篇（Spring 装配——Synthesizer）；01 篇（多活概念——收官呼应）
- **与源码提取的关系**：microsphere-dynamic `[本地存在待核对]`——source/ 提取时以本地仓库为准

**架构师视角结论**：本篇为 **最长篇（554 行，代码块 13 个）**——动态 JDBC 组件体系：**配置驱动**（JSON/Properties 模块化——**ha-datasource 区域化（Zone Key——多活数据源）** + 模块依赖拓扑 + 互斥共存）+ **核心机制**（6 模块常量 + 3 SPI 族（校验/后置/属性合成——复用 Spring Boot）+ **子上下文隔离**（互斥共存）+ **DynamicDataSource 动态换源**（创建/交换/关闭——"动态"本质））——知识本体是"**动态 JDBC 的模块化与动态化机制**"；my-xhs **MyBatis-Plus + 订单 Sharding 静态落地**（动态框架为演进项）；**stage-4 多活组全部完成（01-19+22）——L1 提取收官，下一步 L2 聚合/结营更新**。
