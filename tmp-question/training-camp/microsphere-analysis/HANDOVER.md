# 交接文档 — microsphere 训练营源码分析

> 本文件是唯一权威的进度与约定文档。接手前请**完整阅读**本文（含 Review 标准与工作方法），再动任何文件。

---

## 〇、分析文档目录约定（新 AI 第一件事）

- **所有分析文章根目录**：`/data/workspace/source-code/book/MinerU/tmp-question/training-camp/microsphere-analysis/`
- **模块目录命名**：`NN-microsphere-xxx-analysis/`（如 `14-microsphere-druid-analysis/`）
- **每模块的标准交付物（三件套，参照 09-observability 的先例）**：
  - `README.md` ——模块级索引（源码信息 + 文章索引 + 核心结论 + 与其他模块的引用约定）
  - `NN-XX-简短英文标题.md` ——逐篇深度分析文章（核心命题开头 + 源码走读 + 问题清单 + 测试佐证）
  - **`NN-REQ-requirements-spec.md`** ——**完整需求规格文档**（已有实现的需求标注完成度和已知 bug + 发散列出当前没做但基于框架可做的能力 + 每项需求给出产出指标/事件/配置规格）
  > 需求文档的目标：**把它当成一个真正的生产级需求来做，可以写到简历上**——不只是"代码分析"，而是"这个项目解决了什么生产问题、还该解决什么、每项能力的完整规格是什么"
- **文章命名**：`NN-XX-简短英文标题.md`（如 `14-01-core-mechanism.md`）
- **权威总进度**：本文（HANDOVER.md）
- 相关文档：`master-integration-plan.md`（36 仓库价值评估视角）、`tech-weekly-knowledge-index.md`（知识索引）

---

## 一、任务背景

对 microsphere 开源生态（GitHub: microsphere-projects，作者 mercyblitz/小马哥）做**逐项目源码深度分析**，最终目标：**每个模块产出一份完整需求规格文档（参照 `09-REQ-requirements-spec.md` 标准），包含已有实现的需求（标注完成度和已知 bug）+ 发散待实现的需求（基于框架可做的能力）+ 每项需求的产出指标/事件/配置规格。** 在此基础上形成**知识地图 + Spring 源码系统学习路线**。

**交互原则**：所有关键结论必须源码验证（禁止凭记忆/推理）；每模块完成后立即双维度 review。**每个模块最终交付 README + N 篇分析文章 + 1 份 REQ 需求规格文档**。

**分析原则**：所有关键结论必须源码验证（禁止凭记忆/推理）；每模块完成后立即双维度 review。

---

## 二、当前状态总览（2026-07）

### 已完成深度分析（7 个模块，44 篇）

| 模块 | 文章数 | 一句话定位 | 关键结论 |
|------|--------|-----------|----------|
| **15-configuration** | 7 篇 | 统一注解模型对接 Nacos/Apollo/etcd/ZK | `@PropertySourceExtension` 元注解体系；`PropertySourcesChangedEvent` |
| **17-multiactive** | 9 篇 | 区域感知路由 | ZoneContext 单例；ZonePreferenceFilter 10 步决策树；配置中心改 zone 链路断 |
| **18-dynamic** | 14 篇 | 多数据源热替换 | 子上下文隔离；6 步管道；DynamicDataSource |
| **16-gateway** | 8 篇 | we:// 端点级路由 | **Zone 假设纠正**；实例变更刷新断链；G1-G15 缺口总表 |
| **13-mybatis** | 3 篇 | MyBatis Executor 拦截管道 | 三层拦截体系；官方 SPI 挂载；Sentinel 集成闭环 |
| **14-druid** | 2 篇 | JDBC 层 Statement 过滤器模板化 | 491 方法收敛为 2 回调；switch fallthrough bug（P2）；**同一 `BeanSource` 工具类在 13/14 两种用法产生两种结果**（14-02 核心发现） |
| **09-observability** | 3 篇 + 1 需求规格 + 1 发散 | Spring Boot Actuator 生产级补全 | **两个严重 bug**（`@EventListener` 时序错误→Kafka Appender 永不挂载 + `ConditionalOnBean(name=FQCN)`→Push Gateway 完全失效）；`AbstractMeterBinder` 模板方法跨 4 项目第 4 次复现；`0.0.1-SNAPSHOT` 从未发布 |

### REQ 需求文档进度（每个模块最终产出三件套：README + 分析文章 + REQ 需求规格）

| 模块 | 分析文章 | REQ 需求文档 | 备注 |
|------|:------:|:----------:|------|
| 09-observability | ✅ 4 篇 | ✅ `09-REQ-requirements-spec.md`（15 项需求，含发散） | **唯一完成三件套的模块——标准模板** |
| 13-mybatis | ✅ 3 篇 | ❌ 待产出 | 已有深度分析，需补 REQ 文档（发散：MyBatis 拦截可扩展到 ParameterHandler/ResultSetHandler 层级？与 Druid Filter 的协同治理策略？） |
| 14-druid | ✅ 2 篇 | ❌ 待产出 | 已有深度分析，需补 REQ 文档（发散：`CallableStatement` 支持？多数据源 Filter 复用策略？`WallFilter`/`StatFilter` 官方组件的一键装配？） |
| 15-configuration | ✅ 7 篇 | ❌ 待产出 | — |
| 16-gateway | ✅ 8 篇 | ❌ 待产出 | — |
| 17-multiactive | ✅ 9 篇 | ❌ 待产出 | — |
| 18-dynamic | ✅ 14 篇 | ❌ 待产出 | — |
| 06-nacos | ❌ 待重做 | ❌ 待产出 | 旧单体分析仅作参考素材 |
| 07-sentinel | ❌ 待重做 | ❌ 待产出 | 同上 |
| 08-redis | ❌ 待重做 | ❌ 待产出 | 同上 |

> **所有模块的最终目标**：补齐 REQ 需求规格文档——不只是"代码分析"，而是"这个项目解决了什么生产问题、还该解决什么、每项能力的完整规格是什么"——可以写到简历上。

### 下一批：按 09 标准产出完整需求文档（3 个）

> **工作目标变更（2026-08-02）**：这三个模块不再是"重写已有分析文章"——而是**像 09-observability 一样，产出完整的需求文档**。现有分析（单体 `.md` 文件）仅作为参考素材，不直接改写。每模块的最终交付物：(1) 模块 README 索引；(2) 逐篇深度分析文章；(3) **完整需求规格文档**（参照 `09-REQ-requirements-spec.md`，定义已有实现的需求 + 发散待实现的需求 + 产出指标/事件/配置规格）。目标是把每个模块当成一个**真正的生产级需求来做，可以写到简历上**。

| 模块 | 规模 | 现有参考素材 | 产出规格 |
|------|------|------------|---------|
| 06-nacos | 128 文件 + microsphere-etcd 对照 8 文件 | `06-microsphere-nacos-analysis.md`（16KB） | README + N 篇分析 + **REQ 需求文档** |
| 07-sentinel | 30+30 + microprofile fault-tolerance 对照 6 文件 | `07-microsphere-sentinel-analysis.md`（60KB） | README + N 篇分析 + **REQ 需求文档** |
| 08-redis | 86 文件，9 子模块 | `08-microsphere-redis-analysis.md`（15KB） | README + N 篇分析 + **REQ 需求文档** |

**每个模块的标准工作流程（参照 09-observability 的成功范式）**：
1. **构建 MCP 索引**（`index_repository`，探索优先用它）
2. **深度逐文件探索**——读全部源码（含测试），记录 bug/设计模式/跨模块引用 + `spring.factories`/`AutoConfiguration.imports` 等配置文件
3. **项目定位推导**——站在 Spring Boot 开发者视角，回答"这个项目解决了什么生产问题？"（用需求派生表，不从代码目录出发）
4. **按 09 的文章结构（`09-01/09-02/09-03/09-04`）展开逐篇分析，每篇都要有核心命题、源码走读、问题清单、测试佐证**
5. **输出完整需求规格文档**——已有实现标注完成度和已知 bug + 发散列出现有框架没做的能力 + 每项需求给出产出指标/事件/配置规格

> 现有分析文章是**参考素材**（已经做了一轮代码阅读和结论整理），不是**待重写的半成品**。新产出的质量标准和结构参照 09-observability，旧单体文件不直接改写，而是从中提取已验证的结论作为新文档的素材。

### 待重做（5 个）

01-confucius-commons / 02-microsphere-java / 03-microsphere-spring（主战场）/ 04-microsphere-spring-boot / 05-microsphere-spring-cloud

### 仅知识地图标注（不写分析）

10-dubbo（空壳）、11-resilience4j（已移除：Sentinel 覆盖核心，重试/超时可框架补）、12-tomcat（仓库空壳，但**已有 4 篇官方 Tomcat 源码分析**在 `12-microsphere-tomcat-analysis/`，基于 `/data/workspace/tomcat-10.1.34` + `/data/workspace/spring-boot-3.5.16`）、java-enterprise（CDI 原理教材）、microprofile（其余部分）、logging、jakarta、bom、build

---

## 三、关键结论索引（跨模块引用，写总结勿再写错）

### 各模块 bug/缺口（已源码验证）

| 模块 | 发现 |
|------|------|
| 17 | 配置中心改 zone 链路**断的**（无监听 PropertySourcesChangedEvent 调 setZone() 的组件） |
| 15 | Apollo 不走 `@PropertySourceExtension`；ZK 刷新未实现（`autoRefreshed` 静默无效）；etcd `kv.get().get()` 无限阻塞；Nacos `@NacosPorpertySource` 拼写错误 |
| 18 | 与 17 集成依赖 ZoneContextChangedEvent；子上下文事件父上下文监听器收不到 |
| 16 | **无 Zone 路由**（仅 2 处 TODO）；实例变更刷新断链（`ServiceInstancesChangedEvent` 无发布者 + 心跳被拦）；WebFlux 阻塞选实例（`toFuture().get()` 卡事件循环）；context-path 丢失（buildPath 死代码）；WebFlux refresh NPE（MVC 版有防御）；MVC static 缓存跨上下文；空过滤器数组吞请求；静默 200（NettyRoutingFilter 放行非 http scheme） |
| 13 | 反射读私有字段 ×2（CachingExecutor.delegate、SqlSessionFactoryBean.plugins）；拼写错误 `MyBatisConfigurationBeanDefintionRegistrar`；初始提交 NPE bug（`this.delegate = delegate`）；二级缓存命中时过滤器不执行；spring-cloud 空壳；`dataSource="*"` 多数据源需显式指定 |
| 14 | 双开关冗余；spring-cloud 自动配置空壳（features.yaml 有内容）；**switch fallthrough 导致 BPP 重复注册**（`AlibabaDruidRegistrar.java:67-79`，因下游 Bean 去重未暴露为运行时错误）；`@AlibabaDruidTest.filters()` 死代码 |
| 09 | **P1：`@EventListener(ApplicationPreparedEvent)` 时序错误**——Kafka Appender 永不挂载，启动期日志静默丢弃，KafkaClientMetrics 永不绑定（用 spring-framework v6.2.17 源码严格证明：`registerListeners()` 早期事件重放早于 `EventListenerMethodProcessor.afterSingletonsInstantiated()`）；**P2：`@ConditionalOnBean(name=FQCN)` 参数语义混淆**——`ConditionalOnEnabledPrometheusMetricsExport` 永不匹配，Prometheus Push Gateway 整条推送链路完全失效；`CGroupMemoryMetrics` 只支持 cgroup v1；`CompositeFilter` OR/XOR 未实现（`// TODO`）；`MicrometerJdbcEventListener` static 字段污染 + SQL 完整字符串当 tag（高基数反模式）；`NetworkStatisticsMetrics.parseStats` 无异常防护；`SentinelMetrics` 反射借用 Sentinel 私有字段 `FlowRuleManager.SCHEDULER`；`I18nLogger` 仅 trace 一个重载实现（`TODO : Finish`）；`MBeanMetrics` 零实现死代码 |

**14-02 的对照结论（未来分析其他 microsphere 项目时可直接引用）**：
- 13/14 共享同一个 `io.microsphere.spring.beans.BeanSource` 工具类，但装配代码实现路径不同：13-mybatis 直接调用 `BeanSource.registerBeans` 静态方法（枚举多态分派，零 bug）；14-druid 自己手写 for+switch（fallthrough bug 的根源）——**跨项目对照类分析要落到具体调用链验证，不能停留在"看起来像"的直觉判断**（14-02 review 时纠正过一次：`ComponentResolver.isComponentType` 实际是 `Objects.equals` 精确匹配，不是 `isAssignableFrom` 子类型判断，之前的表述夸大了差异）
- 与 13 的层级对照：ORM 层（13）必然先于 JDBC 层（14）拦截同一 SQL，是技术栈层级决定的物理事实非设计选择；可见信息不对称（13 层看到 MappedStatement 语义信息，14 层看到已绑定参数的最终 SQL 字符串）决定两层资源命名方式的本质差异
- **外部知识来源需标注**：`WallFilter`（SQL 防火墙）、`StatFilter`（慢查询统计）是 Druid **官方生态**能力，microsphere-alibaba-druid 项目未做任何封装引用，写作时曾错误地把这些当成"项目已验证能力"描述，review 时纠正为"官方生态能力，非本项目实现"

**13-03 的对照结论（14-02 写作时直接引用）**：
- 官方 Plugin vs microsphere = 手术刀 vs 流水线，**可共存**（都是官方 InterceptorChain 上的环），嵌套顺序由 `addInterceptor` 顺序决定
- Sentinel × MyBatis 集成闭环：实现 `ExecutorFilter` + 注册 bean 即入管道；资源名 = `MappedStatement.getId()`
- 与 18：子上下文天然隔离（各自 Factory）；防嵌套合并是"同一 Executor 被重复 plugin"的兜底
- **与 14 的层级对照已预埋**：MyBatis Executor 层 vs Druid JDBC 层，同一 SQL 先 Executor 管道后 Druid Filter——14-02 展开细节

### 编号约定（未来整体一致性 review 时统一）

- **16-08 有全局缺口表 G1-G15**（唯一全局编号）
- 各文章局部编号：16-03 P1-P5、16-05 风险 A-D、16-07 H1-H3、13-01 P1-P6、13-02 S1-S5、**14-01 P1-P4**——**局部编号不与 G 表冲突，但整体梳理时要映射**
- 引用其他模块文章用最新编号（18-08 不是 C-01）

### ApplicationPreparedEvent 时序（三重验证，勿写反）

**结论**：`ApplicationPreparedEvent` 在 `prepareContext` 阶段发布（refresh **之前**）。
证据：SpringApplication.java:307/308（`prepareContext()` 先于 `refreshContext()`）、`contextLoaded()` javadoc（"before it has been refreshed"）、`ApplicationPreparedEvent` javadoc（"fully prepared but not refreshed"）、javap 反编译 2.7.4 jar（`EventPublishingRunListener.contextLoaded()` → `new ApplicationPreparedEvent` → `multicastEvent`）。
**推论**：单 config 模式的 6 步管道在 refresh 前执行；两种模式的子上下文都**启动时创建**；真正延迟到首次请求的只有 HikariCP 建连。**禁止写"ApplicationPreparedEvent 在 refresh 之后"或"单 config 懒加载"**。

**09-01 新增的实践验证**：用 spring-framework v6.2.17 源码进一步证实了 `@EventListener(ApplicationPreparedEvent)` 在时序上永不可能触发——`registerListeners()`（refresh():625）的早期事件重放早于 `EventListenerMethodProcessor.afterSingletonsInstantiated()`（SmartInitializingSingleton，在 finishBeanFactoryInitialization():628 内触发）注册 `@EventListener` 方法级监听器。任何 `@EventListener` 注解监听 `ApplicationPreparedEvent` 的设计都是无效的（必须用 `spring.factories` 注册的原生 `ApplicationListener` 才能接收）。这一结论在后续 06-nacos/07-sentinel/08-redis 分析中可作为自动校验规则。

---

## 四、源码位置与环境（新 AI 必读）

### 源码仓库（全部在本地）

```
/data/workspace/java-training-camp/cloud-native-code/
├── share/      # 基础库与 Spring 扩展（02/03/04/07/11/12/13/14 等）
├── stage-4/    # Spring Cloud 扩展（05/08/15/16/17/18）
└── projects/   # 生态项目（06/09/10/etcd/microprofile/java-enterprise 等）
```

### 已拉取的框架源码（写分析时对照）

```
/data/tmp/opencode/spring-cloud-gateway-src/    # SCG 4.1.2（16 用）
/data/tmp/opencode/mybatis-3-src/               # MyBatis 3.5.16（13 用）
/data/tmp/opencode/springboot-src/              # Spring Boot 2.7.4（18 用，仅摘取部分类，非完整源码）
/data/tmp/opencode/druid-src/                   # Druid 1.2.28（14 用，git clone tag=1.2.28）
/data/workspace/tomcat-10.1.34/                 # Tomcat 官方源码（12 用）
/data/workspace/spring-boot-3.5.16/             # Spring Boot 3.5.16（12 用）
/data/workspace/source-code/code/spring/spring-framework/  # Spring Framework v6.2.17（09 P1 时序证明用，完整源码）
```

### MCP 索引（codebase-memory，探索优先用它）

已建：`microsphere-alibaba-druid`（全名 `data-workspace-java-training-camp-cloud-native-code-share-microsphere-alibaba-druid`，796 节点/1444 边，14-druid 分析已用完并完结）。
**新项目探索前先 `index_repository` 建索引**，再用 search_graph/get_code_snippet/trace_path 辅助。

### 拉取外部源码的方式（14-druid 的经验教训）

**禁止反编译 jar**——需要核对官方源码实现细节时（如 Druid 官方 `Filter`/`FilterAdapter`），直接 `git clone` 官方仓库到 `/data/tmp/opencode/` 下（14-druid 已拉取 `druid-src`，tag `1.2.28`），不要用 javap 反编译本地 m2 jar 反推源码逻辑——反编译只能看字节码效果，会增加不必要的中间推理步骤，且不便于后续复用（clone 的源码可以反复 grep/read，反编译产物是一次性的临时文件用后即删）。**16 的"javap 验证常量"仍然适用**（那是验证具体常量值/字节码行为，与"逆向理解源码逻辑"是不同的使用场景）。

### 环境

- Maven 本地仓库 `/data/.m2/repository`（settings.xml 在 /root/.m2，阿里云镜像；**离线构建会缺依赖，在线可拉**）
- 无 sources jar 时用 javap 反编译本地 m2 jar（16 已验证可行）

---

## 五、工作方法（16/13 沉淀的纪律，务必遵守）

1. **先探索后写作**：每个模块先深度探索（**逐文件过**、建 MCP 索引、拉框架源码、git 历史、测试当行为文档），**理解项目是什么之后**再规划文章；规划经用户确认后逐篇写
2. **交互节奏**：**一篇写完 → 深度 review（双维度）→ 用户确认 → 下一篇**。禁止一次性生成全部文章；禁止在用户未确认规划前动笔
3. **事实级 review**：行号/数字/提交归属用 git 取证（`git log --diff-filter=A` 查文件创建提交）、javap/字节码验证常量与行为、测试断言当行为文档——**禁止凭记忆写结论**（16 分析中两次纠正过"凭记忆"错误：NettyRoutingFilter 放行行为、RouteRefreshListener 拦截范围）
4. **内容级 review**：检查"为什么要有/作用是什么/适用边界/与生态关系"是否讲透，不抠行号；推断必须标注"推断"，外部事实（如 Nacos 默认 4096）标注来源
5. **完整阅读**：review 时一次性读完整个文档再列问题
6. **跨文档一致性**：同一结论在不同文档必须一致；引用用最新编号；**写完每篇更新模块 README 索引**
7. **写作风格**：每篇以"核心命题"开头；分层结构（定位→机制→集成→问题→小结）；关键结论带 `文件:行号` 引用；中文
8. **跨项目对照类文章的额外校验**（14-02 教训）：写"A 项目和 B 项目同构/不同"这类对照结论时，必须**逐条 grep 验证对照的另一侧源码**，不能只凭"两边看起来像"的直觉下判断——14-02 曾把 `ComponentResolver.isComponentType()`（精确类型 `Objects.equals`）误判为支持子类型判断，把两个精确匹配机制的差异夸大成"是否支持里氏替换"的差异；对照文章里出现"完全同构"这类绝对化措辞时要格外警惕，多数情况下宏观模式同构、具体检查粒度/参数数量并不完全一致
9. **外部生态能力 vs 本项目已验证能力要严格区分**（14-02 教训）：分析对象是某个 microsphere 项目时，如果提到该项目所依赖的官方生态（如 Druid 官方的 `WallFilter`/`StatFilter`）具备某种能力，必须标注"这是官方生态能力，不是本项目的实现"——不能因为熟悉某个官方生态的常见组件就默认分析对象也实现了它，必须先 grep 确认本项目源码里有没有对应引用

---

## 六、全部完成后（第三批）

- 整体一致性 review（编号统一、结论冲突、重复内容）
- 知识地图 + 学习路线规划（含 Spring/Spring Boot/Spring Cloud 源码系统学习路径——03/04 是主战场）
- `master-integration-plan.md`（36 仓库价值评估视角）与 `tech-weekly-knowledge-index.md`（知识索引）同步更新
