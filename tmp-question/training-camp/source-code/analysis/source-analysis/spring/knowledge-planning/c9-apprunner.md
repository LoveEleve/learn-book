# C-9 ApplicationRunner — 启动回调 (SpringApplication.run → callRunners)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | ApplicationRunner(42行)+CommandLineRunner(45行)+Runner(28行)+SpringApplication(1847行)+ApplicationArguments(74行)+DefaultApplicationArguments(91行)
> 基线: C-8 SpEL 结尾桥 — 容器就绪后的最后一步 — 启动生命周期: refresh 完成→callRunners 执行 Runner bean; 原始执行计划 2-D

---

## §0.8

- 🟡 Working，1篇 — 接口(Runner 标记接口 + ApplicationRunner.run(ApplicationArguments) / CommandLineRunner.run(String[])) → 触发点(SpringApplication.run: listeners.starting→prepareContext→refreshContext→L325 callRunners) → 收集与排序(callRunners: getBeanNamesForType(Runner)→AnnotationAwareOrderComparator 排序, C-4 衔接) → 分派(callRunner: instanceof 双分派) → 参数封装(ApplicationArguments: 源参数/选项/非选项) → 与其他回调对照(执行顺序表)
- 设计模式: [模式: 模板方法]—SpringApplication.run 固定流程, Runner 是流程末尾的扩展钩子; [模式: 标记接口+多态分派]—Runner 下两个子接口按类型分派

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ApplicationRunner.java:33,40 | 接口 | **ApplicationRunner**: extends Runner — run(ApplicationArguments args) — 结构化参数 | High |
| CommandLineRunner.java:36,43 | 接口 | **CommandLineRunner**: run(String... args) — 原始字符串数组 — 老 API | High |
| Runner.java:26 | 标记接口 | **统一标记**: Runner 空接口 — 两个 Runner 共用收集逻辑, 分派靠 instanceof | High |
| SpringApplication.java:301,310,317,318,325 | run() 流程 | **触发点**: L310 listeners.starting → L317 prepareContext → L318 refreshContext(容器完整启动) → L325 callRunners(context, args) | High |
| SpringApplication.java:763 | callRunners() | **收集+排序**: getBeanNamesForType(Runner.class) → IdentityHashMap → getOrderComparator(AnnotationAwareOrderComparator + FactoryAwareOrderSourceProvider) → sorted→逐个 callRunner — @Order/@Ordered 生效(C-4) | High |
| SpringApplication.java:782,793 | callRunner() | **分派**: instanceof ApplicationRunner→run(args) / CommandLineRunner→run(args.getSourceArgs()); callRunner 包 IllegalStateException | High |
| DefaultApplicationArguments.java:47,63 | 参数封装 | **ApplicationArguments**: getSourceArgs(原始数组)/getOptionValues(name)(--key=value)/getNonOptionArgs(非选项) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+触发+收集+分派+参数约 2100 行 — 知识单线: "run 流程尾部 → 收集 Runner bean → 排序 → 分派执行". 1篇 (~44行) 按"接口→触发→收集分派→对照"展开; 若分 2 篇则触发点与收集逻辑割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | SpringApplication.run 流程与 callRunners 触发点 (L301-325) | 🔴 | **为什么🔴**: 启动生命周期地图 — Runner 在"容器完整就绪后"执行, 时序是理解回调顺序的基础 |
| P1-2 | callRunners 收集+排序+分派 (getBeanNamesForType→AnnotationAwareOrderComparator→instanceof) | 🔴 | **为什么🔴**: "扩展点怎么被框架执行"的范本 — 与 C-4 排序、C-6 条件装配衔接 |
| P1-3 | ApplicationRunner vs CommandLineRunner (参数形态差异 + Runner 标记接口) | 🔴 | **为什么🔴**: 两个接口的选择 — 结构化参数 vs 原始数组 — instanceof 双分派设计 |
| P2-1 | ApplicationArguments 参数封装 (getOptionValues/getNonOptionArgs) | 🟡 | **为什么🟡**: 命令行参数的标准解析 — --key=value 语义 |
| P2-2 | 启动回调全家对照 (@PostConstruct→InitializingBean→SmartInitializingSingleton→ApplicationReadyEvent→Runner) | 🟡 | **为什么🟡**: "启动时做 X 用哪个回调"的决策表 |
| P3-1 | callRunner 异常包装 (IllegalStateException) | 🟢 | **为什么🟢**: 启动失败语义 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **run 流程与触发** (SpringApplication.run + callRunners 位置) | 🔴 | 启动生命周期的主干 — Runner 的时序 |
| B | **收集分派** (getBeanNamesForType→排序→instanceof) | 🔴 | 扩展点执行机制 — 通用模式 |
| C | **接口与对照** (双接口+参数封装+回调全家) | 🟡 | 使用侧选型 |

> **Cluster A (§1)**: Runner 双接口 + SpringApplication.run 流程(L301-325)
> **Cluster B (§2)**: callRunners(收集/排序/分派) + ApplicationArguments
> **Cluster C (§3)**: 回调全家对照表 + 使用建议(数据预热/启动检查)

→ 引出 2-E: ClassPathIndex — Boot 启动加速: 组件扫描读 META-INF/spring.components 索引, 免去类路径全扫描 — 启动性能的最后一个优化点

(End of file - total 61 lines)
