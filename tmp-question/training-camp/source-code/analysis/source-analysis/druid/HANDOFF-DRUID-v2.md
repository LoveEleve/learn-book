# Druid 源码分析 — 交接文档 (v2 详细版)

> **日期**: 2026-08-13 | 阶段3.2 数据与存储第二站
> **给新 AI**: 本文是 Druid 分析**唯一入口**。不要跳过任何节。上下文对话已超长, 本文独立承载全部必要信息。
> **你的任务**: 无未完成域 — 阶段3.2 全部 9 域交付完毕 (KP+大纲+questions+六层深审+全量五维度审查)。如需新任务: 按 §七 进入阶段3.3 MP。
> **⚠️ 方法论正式文档**: `talk-method/source-code-analysis/methodology/zh/` (01-08) — 本文内联为速查, 冲突时以正式文档为准。
> **⚠️ 规划权威**: `DRUID-PLAN.md` (9 域规划 v5) — 域清单/依赖图/排除清单/执行顺序全在此。
> **⚠️ 阶段3.3 规划**: `../mybatis-plus/MP-PLAN.md` (已就绪, 9 域待逐域推进)。

---

## §零 当前状态速查

### 全局进度

| 阶段 | 仓库 | 域数 | 篇数 | 状态 |
|:--:|---|:--:|:--:|:--:|
| 阶段1 | Netty + Tomcat | 18 | 36+19 | ✅ 100% (历史完成) |
| 阶段2 | Spring Framework + Boot | 64+29 | 64+30 | ✅ 100% (历史完成) |
| 阶段3.1 | HikariCP | 13 | 13 | ✅ 100% (历史完成) |
| **阶段3.2** | **Druid** | **9** | **9** | ✅ **100% (本次完成, 全量审查通过)** |
| 阶段3 后续 | MP(9)/MyBatis(5)/Redis(18)/Redisson(7)/ES(11) | 50 | — | ⏳ |

### Druid 9 域进度 (全部六层深审 + 全量五维度审查通过)

| 层 | 域 | 目录 | 类型 | 行数 | 锚点 | 闭环 | questions |
|:--:|---|------|:--:|:--:|:--:|:--:|:--:|
| 第1层 池核心 | D-1 连接池核心 (Lock+Condition 借还/init/shutdown) | d01-core-architecture | 🔴 | 67 | 18 | 8 | 17 |
| | D-5 维护体系 (shrink 四阶段/线程/removeAbandoned) | d05-maintenance | 🔴 | 68 | 19 | 6 | 16 |
| 第2层 拦截链 | D-2 Filter 拦截链 (递归链+proxy 对象模型) | d02-filter-chain | 🔴 | 68 | 15 | 7 | 17 |
| 第3层 监控安全 | D-3 StatFilter (模板钩子埋数+参数化+慢 SQL) | d03-statfilter | 🔴 | 65 | 16 | 7 | 16 |
| | D-4 WallFilter (前置检查+AST 规则) | d04-wallfilter | 🔴 | 61 | 10 | 5 | 16 |
| 第4层 支撑 | D-6 SQL Parser 体系 (四层架构概览) | d06-parser | 🟡 | 48 | 4 | 5 | 15 |
| | D-7 连接验证 (SPI 6 实现+三时机) | d07-validation | 🟡 | 49 | 11 | 6 | 17 |
| | D-8 PreparedStatementPool (LRU+自适应预取) | d08-pscache | 🟡 | 49 | 11 | 5 | 16 |
| | D-9 Boot3 Starter (装配链) | d09-boot3 | 🟡 | 49 | 10 | 5 | 16 |

**分类**: 5🔴 / 4🟡 (56% 🔴, 未犯 80% 反模式) — 与 DRUID-PLAN 一致

### 执行拓扑 (结尾桥链, 与 DRUID-PLAN §八 一致, 全量回归验证)

```
D-1(池核心) → D-2(Filter链) → D-7(验证) → D-5(维护) → D-8(PSCache) → D-6(Parser) → D-3(Stat) → D-4(Wall) → D-9(Boot3) → 阶段收尾
```

> 为什么 D-3 不在 D-2 后: **拓扑序** — D-5 shrink 依赖 D-7(validateConnection L3192), D-3/D-4 依赖 D-6(参数化/AST)。基线(issue 规划)是教学序, 本规划按依赖优先。

---

## §一 方法论 — 完整内联 (每域必走, 禁止跳过)

### 1.1 核心管线 (问题驱动!)

```
Pass 0 读代码前上下文(README/doc/git/测试地图)
  → Pass 1 扫轮廓(继承树/基本元素分解/≥5 真标记问题/读 2 测试)
  → Pass 2 闭环(每问题: 假设→grep 验证→结论; 每个闭环立即记录; ≥3 闭环)
  → Pass 3 大纲(只写闭环验证过的机制 — "为什么"必须能追溯到闭环)
  → 六层深审 → 修复 → 全量回归
```

**⚠️ 本会话最深刻的教训**: 第一轮把方法论理解成"格式规范"(填四要素模板), 9 域一次性"填"完 — 被用户多次质疑。正确做法是**问题驱动**: 大纲的"关键设计"必须来自"读源码产生疑问→假设→验证→结论"的闭环, 不是来自既有知识。

### 1.2 v5 大纲格式 — 每节四要素

```
### N. 机制名 — 一句话描述
场景: [真实场景]          ← 必须有"场景:" 前缀
源码路径: [File.java:行号 + 函数名]  ← 必须有"源码路径:" 前缀
关键设计: [为什么 + [模式: XXX]]     ← 必须有"关键设计:" 前缀
数据流: [代码级 trace]              ← 必须有"数据流:" 前缀
```

**严禁**: 裸行号 / 伪行号 / 文件总行数代行号 / 代码拼接 / 缺任一四要素。

**header 必含**: `前置: [[...]] | 复用: [[...]] | 对照: [[...]] | 引出: [[...]]` + `类型 | KP 数 | 模式` + **`Pass 2 闭环: qN(...) qN(...)` — 必须与 KP §05 闭环表完全同步** (本会话 8 次因漏同步被 REVIEW 抓出)。

### 1.3 密度标准

| 级别 | 行数/篇 |
|:--:|:--:|
| 🔴 Deep | 39-69 (超 69 需拆分) |
| 🟡 Working | 35-49 (超 49 需压缩或升级 🔴) |

### 1.4 六层深审 (每域完成必跑)

| 层 | 检查项 | 检测方法 |
|:--:|------|------|
| 1 | 源码行号 | find 定位文件 + sed 验证非空/非 `}` (含内联 Lxxx) |
| 2 | 内容密度 | wc -l 每篇 |
| 3 | 语义 | 逐行 Read, 前后矛盾/数据流跳步/**header 与 KP 闭环同步** |
| 4 | 技术声明 | grep 源码确认归属类 (防编造) |
| 5 | 算法正确性 | 数据流步骤/条件分支对照源码 (**条件写反**是本会话高频错误) |
| 6 | 机制归属 | 方法/字段的确切类 (不凭继承/主题推断) |

**模式A 铁律**: REVIEW 必须真找问题。零发现 = 格式扫描 = 不合格。本会话每域都抓到 1-3 处真实问题(含 3 处算法/编造级)。

### 1.5 复用 ≠ 省略 (五件事检查)

- 纯机制内核 → **一行引用**; 本层使用/组装/配置/生命周期/差异 → **必须展开**
- **五件事**: ①入口 ②配置 ③生命周期 ④差异 ⑤边界 — 全无增量才允许纯引用
- **底线: 拿不准宁可展开也不引用**
- **禁止正文引用未分析域** — 只允许结尾桥引出; 对未分析域(D-2 对 D-2 的环/D-3/D-4 对 D-6)用导航指针, 正文机制必须展开本层内容

### 1.6 产出物结构 (对齐 HikariCP 规划)

```
每域 = knowledge-planning/d{NN}-*.md (KP: §0.8 + 01提取 + 02-04聚合分类聚类 + P1P2P3 + §05闭环结论摘要)
     + outlines/d{NN}-*/01-*.md (大纲)
     + outlines/d{NN}-*/completeness-questions.md (≥3 身份 ≥5 问)
```

**闭环结论内化到 KP §05** (表: # / 机制 / 结论一句话 / 源码位置) — 不建独立 pass 文件。Pass 1/2 的过程文件已全部删除, 结论保留在 KP §05。

---

## §二 全部产出清单 (9 域 / 9 篇)

> 每域含核心机制 + 关键行号锚点 (写作/复习速查)

### 第1层 池核心 (D-1/D-5, 全 🔴)

| 域 | 核心机制 | 关键行号锚点 |
|:--|:--|:--|
| D-1 连接池核心 | 固定数组+ReentrantLock+Condition(notEmpty/empty); init 链路(校验→数组→三线程→initedLatch); 借出 createDirect CAS/maxWaitThreadCount 限流/onFatalError/pollLast await; 归还 rollback→reset 四态→putLast; 用户侧 useLocalSessionState/transactionInfo 事务跟踪; shutdown signalAll+异常联动 handleConnectionException→fatalError; 建连翻译 connectTimeout 按库 | DruidDataSource.java:659,772,1332,1366,1543,1565,1613,1620,1768,1894,1938,2029,2081,2194,2214,2218; DruidAbstractDataSource.java:243,299,1704,1745; DruidConnectionHolder.java:371; DruidPooledConnection.java:236,329,720,745,844 |
| D-5 维护体系 | shrink 四阶段(fatalError 增量→两级驱逐 checkCount/minEvictable/maxEvictable→keepAlive 收集→arraycopy 紧凑); 三线程+DestroyTask+CreateConnectionTask(固定间隔 500ms 重试非指数退避); removeAbandoned 超时强收+isRunning 防误杀+栈追踪; emptySignal 补连双路径 | DruidDataSource.java:2724,2846,2887,2925,3069,3084,3100,3121,3151,3183,3205,3252,3905; DruidPooledConnection.java:1239,1249 |

### 第2层 拦截链 (D-2 🔴)

| 域 | 核心机制 | 关键行号锚点 |
|:--|:--|:--|
| D-2 Filter 拦截链 | 五层类族(Filter 接口→Adapter 空实现→EventAdapter 模板→FilterChain 契约→ChainImpl 递归); pos/nextFilter 下推; 三类链尾(raw 裸执行/池操作/对象包装 — **返回 JDBC 对象包 Proxy, 标量直通**); 模板三路 catch(SQLException/RuntimeException/Error→ErrorAfter→重抛); proxy/jdbc 双层代理(ConnectionProxy=链载体, DruidPooledConnection=池语义); per-connection chain 所有权协议(借走置 null+异步新建); cloneChain 预留 API; 两种风格(模板钩子=观察者/覆写=决策者) | Filter.java:35; FilterChainImpl.java:37,67,246,3005,5067; FilterEventAdapter.java:30,178-197; DruidConnectionHolder.java:223,234; FilterManager.java:36,53,99; WallFilter.java:480; StatFilter.java:378 |

### 第3层 监控安全 (D-3/D-4, 全 🔴)

| 域 | 核心机制 | 关键行号锚点 |
|:--|:--|:--|
| D-3 StatFilter | 模板钩子族埋数(statementExecuteBefore/After/BatchBefore/After/ErrorAfter); JdbcSqlStat 懒创建挂载 StatementProxy; **updateCount 双分支(!firstResult && Execute→getUpdateCount / 否则批量数组)**; 慢 SQL(3000ms+JSON 参数快照 100 字符截断); 参数化聚合 mergeSql→key; 三级统计; **JdbcSqlStat 30+ 字段全 AtomicFieldUpdater**; **双直方图分离(addExecuteTime L702: 非 ExecuteQuery 且非 firstResultSet 并入执行+持有)** | StatFilter.java:378,412,429,468,499,536,562,677,708; JdbcSqlStat.java:33,40-142,127-134,702-707; JdbcDataSourceStat.java:38 |
| D-4 WallFilter | 前置守卫(override statement_execute, check 在 chain 前); Provider 按 dbType 装配(SPI WallProviderCreator+7 内置); 双路径(**ConcurrentLruCache 白名单 1024 + 参数化 SQL key** vs AST 硬检查 parse→visit→violations); WallConfig 规则(默认禁多语句/注释, hint/strictSyntax 允许); 黑名单回填 256; 违规以 SQLException 抛业务 | WallFilter.java:110,146,480,485,493-495; WallProvider.java:58,67,352,441,481,521,572,587; WallConfig.java:45,75,80,126 |

### 第4层 支撑 (D-6~D-9)

| 域 | 核心机制 | 关键行号锚点 |
|:--|:--|:--|
| D-6 SQL Parser | 四层架构(Lexer→Parser→AST→Visitor)+29 方言目录(每方言 Parser/Visitor/AST 子类)+SchemaRepository 元数据; 参数化流程(方言解析器→parseStatementList→ParameterizedVisitor→字面量替换 ?); 显式不做(不集成第三方/不深入 lexer) | Lexer.java:41; SQLStatementParser.java:118,5261; SchemaRepository.java:54; MySqlStatementParser.java:50; MySqlOutputVisitor.java:40; ParameterizedOutputVisitorUtils.java:83-185 |
| D-7 连接验证 | SPI 6 实现按 driver 类名匹配(三保险: 自动/用户覆盖/validationQuery 兜底); 双路径(checker vs execValidQuery); **裸连统计隔离**; 三时机(**testWhileIdle 默认 true!**); 空闲基准三来源取最大(lastActive/lastExec/lastKeep); **负 idle 时钟回拨防御**; onFatalError 恢复探针 | ValidConnectionChecker.java:21; JDBC4ValidConnectionChecker.java:25; ValidConnectionCheckerAdapter.java:40,47; DruidDataSource.java:1240,1385,1407-1419,1980; DruidAbstractDataSource.java:1434,1444,1449,1478; vendor/Oracle:31, OceanBase:26-34 |
| D-8 PSCache | per-connection LRU(LinkedHashMap accessOrder+removeEldestEntry, 默认 10); 归还三分支(干净 put/异常 remove/物理关); 全参键; Oracle 隐式缓存让位; **自适应行预取(fetchRowPeak 驱动: ≤1→2/>默认→默认/否则 peak+1)** | PreparedStatementPool.java:185,192; DruidAbstractDataSource.java:118; DruidPooledConnection.java:138,173,355; DruidPooledPreparedStatement.java:153,500-534,910; PreparedStatementHolder.java:73-77 |
| D-9 Boot3 | 条件注解组(Property matchIfMissing+Class+AutoConfigureBefore+@Import 4 配置); Wrapper 双前缀回退(username L38-39/password L41-42/url L44-45/driver L47-48)+autoAddFilters(@Autowired 汇入链); 8 Filter 条件注册; StatViewServlet/WebStatFilter/AOP(RegexpMethodPointcutAdvisor+DruidStatInterceptor+aop.auto=false 兜底) | DruidDataSourceAutoConfigure.java:44-53,63-68; DruidDataSourceWrapper.java:30-56; DruidStatViewServletConfiguration.java:29,33; DruidWebStatFilterConfiguration.java:30-39; DruidSpringAopConfiguration.java:30-47; DruidStatProperties.java:24-27 |

### completeness-questions

每域 3 身份(开发者/架构师/学生) / 15-17 问 — 见各域目录

---

## §三 质量标准 — 检查命令 (每域完成必跑)

```bash
# KP 检查 (P1P2P3+色+为什么 必须相等)
grep -cP '^\| P[123]' knowledge-planning/d{NN}-*.md
grep -P '^\| P[123]' knowledge-planning/d{NN}-*.md | grep -cP '🔴|🟡|🟢'   # = P条目数
grep -P '^\| P[123]' knowledge-planning/d{NN}-*.md | grep -c '为什么'       # = P条目数

# 大纲五项全等 + 密度 + 结尾桥 + header 闭环同步
for e in '^### ' '^数据流:' '^源码路径:' '^场景:' '^关键设计:'; do grep -c "$e" outlines/d{NN}-*/01-*.md; done  # 五者相等
wc -l outlines/d{NN}-*/01-*.md     # 🔴 39-69 / 🟡 35-49
tail -1 outlines/d{NN}-*/01-*.md | grep -c '→ 引出'  # =1
grep -c '^| q[0-9]' knowledge-planning/d{NN}-*.md   # header 的 "Pass 2 闭环" 必须与 KP §05 条数一致!
grep -c '视角' outlines/d{NN}-*/completeness-questions.md  # ≥3
grep -c '^| [0-9]' outlines/d{NN}-*/completeness-questions.md  # ≥5

# 锚点密度 (07 §维度2)
grep -c '\.java:' outlines/d{NN}-*/01-*.md   # 🔴 ≥8 / 🟡 ≥4
```

**行号验证** (关键, 含内联 Lxxx):
```bash
# find 定位文件 (注意同名类: DruidStatementConnection 在 pool/, 勿与 proxy 混淆)
# sed -n '{行号}p' 非空非 '}' — 必须在源码原文件 grep 行号, 禁止数 cat 输出!
# 方法体内部多行必须逐行核对 (条件分支/调用点)
```

---

## §四 缺陷谱系 — 本会话 REVIEW 全部发现

### 4.1 PLAN 阶段 (DRUID-PLAN v1→v5, 19 处)

| # | 域/范围 | 类型 | 错误 | 修复 | 教训 |
|:--:|:--|:--|:--|:--|:--|
| 1 | D-5 | 事实错误 | CreateConnectionTask "指数退避" | 固定间隔 500ms 重试 (**基线同误**) | 机制声明必须读方法体 |
| 2-6 | 全局 | 结构缺失 | 三信号置信度列/入口追踪表/环形依赖/依赖图/归属 | 补全 | 00 §3.5/§2/§4 产出格式逐列对照 |
| 7 | D-3 | 机制错误 | 埋数"statement_execute\* override" | 模板钩子族+JdbcSqlStat 挂载 | 拦截机制 grep 全部 override 签名 |
| 8 | D-2 | 预检未完成 | proxy/jdbc ≥500 行类未读 | 定量预检完成 | 00 §3 预检触发必读完关键类 |
| 9-15 | 多域 | 机制缺失/数字错误 | 入池三分支/置信度/实现数 4→6/shutdown/Filter 注册链 | 补全修正 | 生命周期逐分支核对; "N 个实现"穷举目录 |
| 16-19 | 全局 | 对照/产出物缺失 | 负面空间表/08 产出物映射/pool 72 实证/行号复核 | 补全 | 对比型域显式声明边界; 基线数字复核 |

### 4.2 域阶段 (六层深审, 19 处)

| # | 域 | 类型 | 错误 | 修复 | 教训 |
|:--:|:--|:--|:--|:--|:--|
| 1 | D-1 | 行号/归属 | getConnection 写 L2290(带参版), 实为 L1332 | 修正大纲+KP | 方法签名 grep 确认再锚定 |
| 2 | D-1 | 边界缺失 | putLast 失败路径(池满→close)未写 | 补入 §3 | 五件事的"边界"检查项 |
| 3 | D-2 | 一致性 | header 缺 q6/q7/q8 | 补全 | header 闭环列表与 KP §05 同步 |
| 4 | D-2 | 行号 | Before 钩子写 L181, 实为 L179 | 修正 | 方法体内逐行核对 |
| 5 | D-3 | 一致性 | header 缺 q6/q7 | 补全 | 同 #3 |
| 6 | D-3 | **算法** | updateCount 条件写反("Execute 且 firstResult"→实为 **!firstResult && Execute**) | 修正 | 条件分支逐字对照源码 |
| 7 | D-3 | **算法+行号** | 直方图条件写反("查询才记"→实为**非 ExecuteQuery 且非 firstResultSet**); addExecuteTime L166→L702 | 大纲+KP 双修正 | 同上 + 行号 grep |
| 8 | D-4 | 一致性 | header 缺 q5 | 补全 | 同 #3 |
| 9 | D-4 | 数字 | wall/spi/ 写 14 文件, 实为 **16**(漏数 WallVisitorBase/Utils), PLAN 同误 | 双修正 | 目录计数穷举 |
| 10 | D-4 | 行号 | WallFilter 异常传播 L497, 实为 catch L493/ErrorAfter L494/throw L495 | 大纲+KP 双修正 | 调用点行号 grep |
| 11 | D-5 | 一致性 | header 缺 q5/q6 | 补全 | 同 #3 |
| 12 | D-6 | 一致性 | header 缺 Pass 2 闭环行 | 补全 | 同 #3 |
| 13 | D-6 | 数字 | §2 标题残留"28 种", 正文已 29 | 修正 | 修正后全文件扫残留 |
| 14 | D-7 | 一致性 | header 缺 q4/q5/q6 | 补全 | 同 #3 |
| 15 | D-7 | **编造** | "三时机默认全 false" — 实为 **testWhileIdle 默认 true**(DEFAULT_WHILE_IDLE L76) | 修正 | 默认值必须 grep; 第一轮发现的错误重写时漏修 |
| 16 | D-7 | 行号格式 | MySql checker 无行号前缀 | 补 :44 | 行号格式统一 |
| 17 | D-8 | 一致性 | header 缺 q4/q5 | 补全 | 同 #3 |
| 18 | D-9 | 一致性 | header 缺 q5 | 补全 | 同 #3 |
| 19 | D-9 | 行号 | 前缀回退区间含糊(password 实为 L41-42) | 精确化 | 区间行号逐项核对 |

### 4.3 全量五维度审查 (07, 2 处)

| # | 维度 | 错误 | 修复 |
|:--:|:--|:--|:--|
| 1 | 维度2 锚点密度 | d06 仅 3 锚点 (🟡 标准 ≥4) | 补方言子类锚点(MySqlStatementParser:50/MySqlOutputVisitor:40) |
| 2 | 维度5 负面空间 | d06 无"不做"声明 (0) | 补 2 条显式不做(不集成第三方/不深入 lexer) |

**共性规律 (高频坑, 按频次)**:
1. **header 闭环列表与 KP §05 不同步** (8 次) — 每域改 KP 闭环后必须同步 header
2. **条件写反** (2 次算法级) — 条件分支必须逐字对照源码
3. **数字/目录穷举** (spi 14→16, 方言 28→29)
4. **默认值编造** (testWhileIdle) — 默认值必须 grep DEFAULT_*
5. **重写后残留旧错误** (D-7 默认值, D-6 28) — 修正后全文件扫残留
6. **行号必须 grep 原文件** (禁止数 cat 输出)

---

## §五 文件路径

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/
├── talk-method/source-code-analysis/methodology/zh/  ← 方法论正式文档 (01-08, 权威!)
└── source-analysis/
    ├── issue/源码分析执行计划.md                    ← 原始 337 域执行计划 (主线权威)
    ├── issue/Druid源码学习范围规划.md               ← Druid 基线 (9 域, 2026-08-03)
    ├── spring/  (已完成: Framework 64 + Boot 29)
    ├── hikaricp/  (已完成: 13 域, 含 HANDOFF-HIKARICP-v2.md)
    ├── druid/  ← 本会话 (阶段3.2)
    │   ├── HANDOFF-DRUID.md (v1 简版)
    │   ├── HANDOFF-DRUID-v2.md (本文, 权威)
    │   ├── DRUID-PLAN.md    ← 知识网络化规划 (9 域 v5: 域清单/追踪表/依赖图/排除/负面空间/缺陷谱系)
    │   ├── knowledge-planning/ (9 个 KP: d1-core ~ d9-boot3, 每 KP 含 §0.8+01提取+P1P2P3+§05闭环结论摘要)
    │   ├── outlines/  (9 目录 / 每域 2 文件: 01-*.md + completeness-questions.md — 对齐 HikariCP)
    │   └── harness/  (MiniDruidPool/MiniFilterChain 极简复现, 编译运行通过)
    ├── mybatis-plus/  ← 阶段3.3 (MP-PLAN.md 已就绪, 其余待建)
    └── ...

源码:
├── /data/workspace/source-code/code/spring/druid/   ← Druid (core 1614 文件, com.alibaba.druid)
│    pool/(DruidDataSource 3979/DruidAbstractDataSource 2388/Holder/PooledConnection/vendor 8)
│    filter/(Filter 1378/Adapter 2891/EventAdapter 541/Chain 1192/ChainImpl 5287/stat/wall)
│    sql/(parser 23/ast 422/visitor 54/dialect 713-29 目录/repository 11)  wall/(46)  stat/(24)  proxy/jdbc/(36)
├── /data/workspace/source-code/code/spring/mybatis-plus/   ← 阶段3.3 (403 文件, 模块: core/extension/annotation/generator/starter)
```

**重要**: ①pool/ 72 文件含子目录 ②MBean/StatService 服务层已排除(只读暴露, 对照 H-10 因热改 setter 才成域) ③Javassist 类比: Druid 无字节码代理, Filter 链+显式 Proxy 对象模型 ④同名类注意: DruidStatementConnection(pool/)/DataSourceProxyImpl(proxy/jdbc/)

---

## §六 知识网络 (Obsidian 双链)

```
← 复用/内核来源 (已分析):
   spring-jdbc (C-11) ──→ D-1, D-9 (DataSource/JDBC 内核)
   spring-tx (s29-s33) ──→ D-1 归还/事务边界 (未提交事务 rollback)
   spring-aop (s24-s28) ──→ D-2 (配置式拦截对照: CGLIB vs Filter 链), D-9 (AOP 切面)
   S-2 自动装配管线 ──→ D-9 (条件注解组复用)
   HikariCP H-1~H-13 ──→ D-1/D-5/D-7/D-8 (两极对照: ConcurrentBag vs Lock+Condition; HouseKeeper vs shrink; 验证; PSCache)

→ 引出/消费者:
   D-1~D-9 ──→ S-10 Boot DataSource (本阶段是 S-10 池化的深入 — Druid 侧闭环)
   D-2 Filter 链 ──→ 阶段3.4 MyBatis 插件机制 (仅引出)

📌 双链格式 (9/9 篇 header 已写): 前置/复用/对照/引出
📌 负面空间对照表 (DRUID-PLAN §七, 07 §维度5):
   Druid 不做: 极致精简/无锁并发/字节码代理/PSCache 之外; Hikari 不做: Filter 链/防火墙/解析器/内置监控
```

---

## §七 下一步行动 — 阶段3.3 MyBatis-Plus (9 域 MP-1~MP-9)

### 已就绪

`../mybatis-plus/MP-PLAN.md` — 域发现+规划已完成 (9 域 / 5🔴+4🟡 / 覆盖率 100% / 2 处执行计划数字修正: 注入方法 17→18 类/回调 5→7)

### 执行步骤 (每域)

1. 定位源码 (`/data/workspace/source-code/code/spring/mybatis-plus/`, 403 文件) → 2. **严格问题驱动**: Pass 0(README-zh/CHANGELOG/测试地图 bvt/) → Pass 1(继承树/元素分解/≥5 真问题) → Pass 2(闭环: 假设→验证→结论, 结论内化 KP §05) → 3. 写 KP → 4. 写大纲(四要素+header 闭环同步+密度) → 5. questions → 6. 六层深审(必须真找问题) → 7. 全量回归 → 8. 更新 MP HANDOFF

### MP 9 域 (MP-PLAN 已定)

| 域 | 核心主题 | 类型 |
|:--:|:--|:--:|
| MP-1 | SQL 自动注入 (DefaultSqlInjector→AbstractMethod 模板, 无主键降级) | 🔴 |
| MP-2 | 表元数据解析 (TableInfoHelper 注解→TableInfo, 两级缓存+Configuration 重初始化) | 🔴 |
| MP-3 | Lambda 条件构造器 (SerializedLambda 反序列化→SFunction 列名) | 🔴 |
| MP-4 | 插件体系 (MybatisPlusInterceptor→InnerInterceptor 7 回调) | 🔴 |
| MP-5 | 分页插件 (PaginationInnerInterceptor 物理分页改写) | 🔴 |
| 🟡 MP-6 | 乐观锁 (@Version CAS) | 🟡 |
| 🟡 MP-7 | 自动填充 (MetaObjectHandler) | 🟡 |
| 🟡 MP-8 | 逻辑删除 (@TableLogic DELETE→UPDATE) | 🟡 |
| 🟡 MP-9 | BaseMapper+IService (CRUD 接口/ServiceImpl 链式) | 🟡 |

**执行顺序** (拓扑): MP-2 → MP-1 → MP-9 → MP-3 → MP-4 → MP-5 → MP-6 → MP-7 → MP-8

**MyBatis 内核(阶段3.4)未分析** — MP 对 SqlSession/Executor/MappedStatement 用导航指针, 正文展开 MP 本层增量

### 阶段3 后续 (全部完成后)

- 阶段3.4 MyBatis(5) → 3.5 Redis(18) → 3.6 Redisson(7) → 3.7 ES(11) = 共 72 域
- 📌 Obsidian 知识图谱: 全部域大纲转双链 vault (全局待办, 同 HikariCP)

---

## §八 踩坑速查 (本会话全部教训)

| # | 坑 | 教训 |
|:--:|------|------|
| 1 | **批量操作** | 方法论是问题驱动认知流程, 不是文件格式流水线。一个域一个域: 读源码→真问题→闭环→落地→汇报→确认后再下一个。批量 = 被质疑 (本会话 4 次) |
| 2 | header 闭环不同步 | 每域改 KP §05 后必须同步大纲 header 的 "Pass 2 闭环" (8 次被抓) |
| 3 | 条件写反 | 条件分支逐字对照源码 (updateCount !firstResult/直方图 ExecuteQuery!=type) |
| 4 | 默认值编造 | 默认值必须 grep DEFAULT_* (testWhileIdle 默认 true 非 false) |
| 5 | 数字穷举 | 目录/文件数 ls 数全 (spi 16 非 14, 方言 29 非 28) |
| 6 | 重写残留 | 修正后全文件扫残留 (D-7 默认值/D-6 28 均为第一轮错误的重写漏网) |
| 7 | 行号数 cat 输出 | 必须 grep 原文件 (execValidQuery 段 5 处全错教训) |
| 8 | 锚点密度 | 07 §维度2: 🔴 ≥8 / 🟡 ≥4 (d06 曾 3 不达标) |
| 9 | 负面空间 | 对比型域必须显式声明"不做的事" (07 §维度5) |
| 10 | 前向引用 | 机制域正文禁引用未分析域; 未分析用导航指针; 总览域可导航式指针 |
| 11 | 结构对齐 | outlines/ 每域 2 文件; 闭环内化 KP §05; pass 独立文件已废弃 |
| 12 | 极简复现 | harness 验证核心控制流有价值(MiniDruidPool 抓到测试逻辑 bug; MiniFilterChain 实证 reset 必要性), 但勿占用主线时间 |
| 13 | 跨阶段 | 上一阶段未全部深审通过不得开新阶段 (Druid 未收尾就做 MP-PLAN 被质疑) |

---

## §九 质量追踪

| 指标 | 数据 |
|:--|:--:|
| Druid 完成 | 9 域/9 篇 全通过 (单域六层深审 + 全量五维度审查) |
| REVIEW 修复 | PLAN 阶段 19 处 + 域阶段 19 处 + 全量 2 处 = **40 处真实问题** |
| 全量回归 | 结构 9/9、KP 9/9 (P1P2P3+色+为什么 全等)、锚点 9/9 (🔴≥8/🟡≥4)、桥链 9/9 与执行序一致、行号全量有效 |
| 闭环 | 54 条 (9 域) 全部内化 KP §05, 每条含源码位置 |
| questions | 9/9 域 ≥3身份/15-17问 |
| 密度 | 🔴 61-68 / 🟡 48-49 (全在区间) |
| 结构对齐 | outlines/ 每域 2 文件 — 与 HikariCP 规划一致 |
| 算法级修复 | 3 处 (updateCount 条件/直方图条件/testWhileIdle 默认值) — 证明逐域深审必要性 |

**给新 AI 的第一句话**: 阶段3.2 已全部交付。若继续, 从 §七 MP 阶段开始 — 严格一个域一个域, 问题驱动, 每域六层深审必须真找问题。禁止批量写/批量修。
