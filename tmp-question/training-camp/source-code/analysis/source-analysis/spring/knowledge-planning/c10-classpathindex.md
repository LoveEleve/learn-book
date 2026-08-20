# C-10 ClassPathIndex — 组件索引 (@Indexed → spring.components → 扫描加速)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | Indexed(91行)+CandidateComponentsIndexLoader(121行)+CandidateComponentsIndex(114行)+ClassPathScanningCandidateComponentProvider(593行)+ClassPathBeanDefinitionScanner(393行)
> 基线: C-9 结尾桥 — 启动还能更快吗？组件扫描的类路径全扫描 → 索引直接定位; 原始执行计划 2-E

---

## §0.8

- 🟡 Working，1篇 — 诞生(@Indexed 标记注解 + spring-indexer 编译期处理器→生成 META-INF/spring.components, 格式: 全限定类名=stereotype 列表) → 加载(CandidateComponentsIndexLoader: cache.computeIfAbsent + classLoader.getResources 合并多 jar 索引) → 查询(CandidateComponentsIndex.getCandidateTypes(basePackage, stereotype)) → 扫描分派(findCandidateComponents: 有索引→addCandidateComponentsFromIndex 精准定位 / 无→scanCandidateComponents 全扫描兜底)
- 设计模式: [模式: 缓存]—loadIndex 按 ClassLoader 缓存; [模式: 索引查询]—编译期建索引避免运行期扫描; [模式: 降级]—无索引回退全扫描

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Indexed.java:90 | 注解 | **@Indexed**: 标记注解 — 标注后 spring-indexer 编译期处理器生成索引 — @Component 等已有 @Indexed 元注解 | High |
| CandidateComponentsIndexLoader.java:42,48 | 加载器 | **位置与缓存**: COMPONENTS_RESOURCE_LOCATION="META-INF/spring.components"(L48); loadIndex L84/L89: cache.computeIfAbsent(ClassLoader) 按类加载器缓存 | High |
| CandidateComponentsIndexLoader.java:93 | doLoadIndex() | **合并**: classLoader.getResources(多 jar 各自 spring.components) → Properties 列表 → 合并成单索引(totalCount>0 才非 null) | High |
| CandidateComponentsIndex.java:49,81 | 查询 | **getCandidateTypes(basePackage, stereotype)**: 从索引 Map 查"某包+某 stereotype"下的候选类集合 | High |
| ClassPathScanningCandidateComponentProvider.java:301,345 | 扫描入口 | **分派**: L301 构造时 loadIndex; findCandidateComponents L345: componentsIndex!=null 且 indexSupportsIncludeFilters→L347 addCandidateComponentsFromIndex(索引路径) / 否则 L350 scanCandidateComponents(全扫描兜底) | High |
| ClassPathScanningCandidateComponentProvider.java:408 | 索引路径 | **addCandidateComponentsFromIndex**: 对 includeFilters 提取 stereotype → index.getCandidateTypes → 逐个 getMetadataReader+isCandidateComponent 生成 BeanDefinition — 无需遍历类路径 | High |
| ClassPathScanningCandidateComponentProvider.java:451 | 全扫描 | **scanCandidateComponents**: resourcePatternResolver 扫描 classpath*: 全部 .class + ASM 读取注解 — 慢, 无索引时的兜底 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 注解+加载器+索引+扫描器共 1300 行 — 知识单线: "@Indexed→spring.components→加载合并→按类型查询→(无则全扫描)". 1篇 (~44行) 按"诞生→加载→分派"展开; 若分 2 篇则索引加载与扫描分派割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | findCandidateComponents 分派 (有索引→索引查询 / 无→全扫描) | 🔴 | **为什么🔴**: 启动加速的核心决策点 — 有索引免全扫描, 无索引兜底保证正确 |
| P1-2 | CandidateComponentsIndexLoader 加载与缓存 (getResources 合并 + ClassLoader 缓存) | 🔴 | **为什么🔴**: 索引怎么从多 jar 汇聚 — 启动期一次加载、按类加载器缓存 |
| P1-3 | addCandidateComponentsFromIndex (stereotype 提取→getCandidateTypes→BeanDefinition) | 🔴 | **为什么🔴**: 索引查询到 BeanDefinition 的完整路径 — 免扫描的关键 |
| P2-1 | @Indexed + spring-indexer 生成 spring.components (编译期) | 🟡 | **为什么🟡**: 索引从哪来 — 编译期生成 vs 运行期扫描的时间换空间 |
| P2-2 | CandidateComponentsIndex.getCandidateTypes (包+stereotype 双维度) | 🟡 | **为什么🟡**: 索引查询语义 — 精确到(包, 类型) |
| P3-1 | scanCandidateComponents 全扫描兜底 | 🟢 | **为什么🟢**: 无索引时的正确性保证 — 理解为何默认仍可用 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **索引生成与加载** (@Indexed + Loader) | 🔴 | 索引从哪来、怎么载入内存 |
| B | **扫描分派** (findCandidateComponents 双路径) | 🔴 | 有/无索引的决策 — 加速核心 |
| C | **查询与兜底** (getCandidateTypes + 全扫描) | 🟡 | 索引查询语义与降级 |

> **Cluster A (§1)**: @Indexed 注解 + spring-indexer 生成 spring.components + CandidateComponentsIndexLoader 加载合并
> **Cluster B (§2)**: findCandidateComponents 分派 + addCandidateComponentsFromIndex(索引路径)
> **Cluster C (§3)**: getCandidateTypes 查询语义 + scanCandidateComponents 全扫描兜底 + 何时用索引(大项目/启动慢)

→ 引出 6-2: DataSource — 组件索引解决"类在哪", 数据访问解决"连接在哪" — 从容器层进入 JDBC 数据源层

(End of file - total 61 lines)
