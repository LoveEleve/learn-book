# C-19 @Sql — 测试数据初始化 (注解 → 监听器 → ScriptUtils 执行)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | Sql(约300行)+SqlScriptsTestExecutionListener(约200行)+ScriptUtils(约700行)+SqlConfig(约250行)
> 基线: C-18 @MockBean — 替换 Bean 后要准备数据: @Sql 在测试前后执行 SQL; 原始执行计划 8-B

---

## §0.8

- 🟡 Working，1篇 — 声明(@Sql: scripts 脚本/statements 内联 SQL/phase 执行阶段/mergeMode 合并) → 监听器(SqlScriptsTestExecutionListener: beforeTestMethod→executeSqlScripts BEFORE, afterTestMethod→AFTER) → 执行(ScriptUtils.executeSqlScript: 解析注释/分隔符→statement 逐个执行) → 事务模式(SqlConfig.transactionMode: 无事务/Spring 事务/独立事务)
- 设计模式: [模式: 观察者]—TestExecutionListener 挂测试生命周期; [模式: 模板]—ScriptUtils 固定脚本解析执行; [模式: 声明式]—@Sql 注解声明测试数据

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Sql.java:88,141,155 | 注解 | **@Sql**: scripts(脚本文件 L141)/statements(内联 SQL L155) — 可标类或方法 | High |
| Sql.java:159 | phase | **执行阶段**: 默认 ExecutionPhase.BEFORE_TEST_METHOD — 可选 AFTER_TEST_METHOD/BEFORE/AFTER_TEST_CLASS | High |
| SqlScriptsTestExecutionListener.java:118,174 | 监听器 | **beforeTestMethod L174**: executeSqlScripts(testContext, BEFORE_TEST_METHOD) — 方法前执行; afterTestMethod L183 对称 | High |
| SqlScriptsTestExecutionListener.java | executeSqlScripts | **执行入口**: 收集 @Sql(类+方法合并, 依 mergeMode)→DataSource/事务→ScriptUtils.executeSqlScript | High |
| ScriptUtils.java:55,125 | 脚本执行 | **executeSqlScript L125**: 读脚本→解析注释/语句分隔符→连接上逐个 Statement 执行 | High |
| SqlConfig.java | 配置 | **transactionMode**: DEFAULT(=INFERRED 默认)/INFERRED(在现有 @Transactional 事务内, 无管理器则直连)/ISOLATED(独立事务) — 控制数据回滚 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 注解+监听器+脚本执行器约 1500 行 — 知识单线: "@Sql 声明 → 监听器按 phase 触发 → ScriptUtils 执行". 1篇 (~45行) 按"声明→监听器→执行"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | SqlScriptsTestExecutionListener (beforeTestMethod/afterTestMethod 触发执行) | 🔴 | **为什么🔴**: @Sql 的执行时机 — 挂 TestContext 监听器(C-17 机制) |
| P1-2 | @Sql 声明模型 (scripts/statements/phase/mergeMode) | 🔴 | **为什么🔴**: 怎么声明测试数据 — 脚本 vs 内联、类 vs 方法、合并规则 |
| P1-3 | ScriptUtils.executeSqlScript (脚本解析+statement 执行) | 🔴 | **为什么🔴**: 脚本执行的底层 — 注释/分隔符/错误处理 |
| P2-1 | SqlConfig.transactionMode (DEFAULT/INFERRED/ISOLATED) | 🟡 | **为什么🟡**: 数据回滚语义 — 与 @Transactional 测试配合 |
| P2-2 | ExecutionPhase (BEFORE/AFTER × METHOD/CLASS) | 🟡 | **为什么🟡**: 执行阶段选型 — 建表(BEFORE_CLASS)/清理(AFTER_METHOD) |
| P3-1 | mergeMode (SqlMergeMode: 合并类+方法脚本) | 🟢 | **为什么🟢**: 类级+方法级 @Sql 的叠加规则 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **声明模型** (@Sql scripts/statements/phase/merge) | 🔴 | 怎么声明测试数据 |
| B | **执行机制** (监听器 + ScriptUtils) | 🔴 | 何时/如何执行 |
| C | **事务与配置** (SqlConfig) | 🟡 | 数据回滚语义 |

> **Cluster A (§1)**: @Sql 注解(scripts/statements/phase/mergeMode)
> **Cluster B (§2)**: SqlScriptsTestExecutionListener(before/afterTestMethod) + executeSqlScripts
> **Cluster C (§3)**: ScriptUtils 脚本解析执行 + SqlConfig.transactionMode + 使用场景

→ 引出 9-A: WebSocket — spring-test 层收尾, 回到 Web: @EnableWebSocket/WebSocketHandler/HandshakeInterceptor 双向通信

(End of file - total 61 lines)
