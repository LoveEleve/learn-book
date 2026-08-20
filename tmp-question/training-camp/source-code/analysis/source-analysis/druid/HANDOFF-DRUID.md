# Druid 源码分析 — 交接文档 (v1)

> **日期**: 2026-08-12 | 阶段3.2 数据与存储第二站
> **给新 AI**: 本文是 Druid 分析**唯一入口**。不要跳过任何节。
> **你的任务**: 无未完成域 — 本阶段 9 域全部完成 (KP+大纲+questions+六层深审)。如需新任务: 按 §七 进入阶段3.3 MP。
> **⚠️ 方法论正式文档**: `talk-method/source-code-analysis/methodology/zh/` (01-08) — 冲突时以正式文档为准。
> **⚠️ 规划权威**: `DRUID-PLAN.md` (9 域规划 v5) — 域清单/依赖/排除/执行顺序全在此。

---

## §零 当前状态速查

### 全局进度

| 阶段 | 仓库 | 域数 | 篇数 | 状态 |
|:--:|---|:--:|:--:|:--:|
| 阶段1 | Netty + Tomcat | 18 | 36+19 | ✅ 100% (历史完成) |
| 阶段2 | Spring Framework + Boot | 64+29 | 64+30 | ✅ 100% (历史完成) |
| 阶段3.1 | HikariCP | 13 | 13 | ✅ 100% (历史完成) |
| **阶段3.2** | **Druid** | **9** | **9** | ✅ **100% (本次完成)** |
| 阶段3 后续 | MP(9)/MyBatis(5)/Redis(18)/Redisson(7)/ES(11) | 50 | — | ⏳ |

### Druid 9 域进度 (全部 REVIEW 过)

| 层 | 域 | 目录 | 类型 | 行数 |
|:--:|---|------|:--:|:--:|
| 第1层 池核心 | D-1 连接池核心 (Lock+Condition 借还/init/shutdown) | d01-core-architecture | 🔴 | 61 |
| | D-5 维护体系 (shrink 四阶段/线程/removeAbandoned) | d05-maintenance | 🔴 | 63 |
| 第2层 拦截链 | D-2 Filter 拦截链 (递归链+proxy 对象模型) | d02-filter-chain | 🔴 | 64 |
| 第3层 监控安全 | D-3 StatFilter (模板钩子埋数+参数化+慢 SQL) | d03-statfilter | 🔴 | 63 |
| | D-4 WallFilter (前置检查+AST 规则) | d04-wallfilter | 🔴 | 60 |
| 第4层 支撑 | D-6 SQL Parser 体系 (四层架构概览) | d06-parser | 🟡 | 47 |
| | D-7 连接验证 (SPI 6 实现+三时机) | d07-validation | 🟡 | 49 |
| | D-8 PreparedStatementPool (LRU) | d08-pscache | 🟡 | 49 |
| | D-9 Boot3 Starter (装配链) | d09-boot3 | 🟡 | 49 |

**分类**: 5🔴 / 4🟡 (56% 🔴, 未犯 80% 反模式) — 与 DRUID-PLAN 一致

### 执行拓扑 (实际结尾桥链, 已与 PLAN 对齐)

```
D-1(池核心) → D-2(Filter链) → D-7(验证) → D-5(维护) → D-8(PSCache) → D-6(Parser) → D-3(Stat) → D-4(Wall) → D-9(Boot3) → 阶段收尾
```

> ⚠️ 桥链修正记录: D-2 初始桥指向 D-3, 与执行序 (D-7 先于 D-3, shrink 依赖验证) 冲突 — 已改为 → D-7 (全量回归发现, 教训 #13 跨域一致性)

---

## §一 方法论 — 完整内联 (每域必走, 禁止跳过)

```
01 逐源提取 → 02 聚合(P1≥5/P2 2-4/P3 1) → 03 深度分类(🔴🟡🟢 per KP, 必须有"为什么")
  → 04 聚类(教学顺序+每个决定的原因) → 05 v5 大纲(AI 自写, 对照源码 grep 验证行号)
  → 06 completeness-questions(≥3 身份, ≥5 问) → 07 六层深审 → 08 修复 → 09 HANDOFF 更新
```

### 1.2 v5 大纲格式 — 每节四要素

```
### N. 机制名 — 一句话描述
场景: [真实场景]          ← 必须有"场景:" 前缀
源码路径: [File.java:行号 + 函数名]  ← 必须有"源码路径:" 前缀
关键设计: [为什么 + [模式: XXX]]     ← 必须有"关键设计:" 前缀
数据流: [代码级 trace]              ← 必须有"数据流:" 前缀
```

**严禁**: 裸行号 / 伪行号 / 文件总行数代行号 / 代码拼接 / 缺任一四要素

### 1.3 密度标准

| 级别 | 行数/篇 |
|:--:|:--:|
| 🔴 Deep | 39-69 (超 69 需拆分) |
| 🟡 Working | 35-49 (超 49 需压缩或升级 🔴) |

> ⚠️ D-9 曾 50 行超 🟡 上限 — 已压缩至 49 (合并 Wrapper 两行)

### 1.4 六层深审

| 层 | 检查项 | 检测方法 |
|:--:|------|------|
| 1 | 源码行号 | sed 验证非空/非 `}` (含内联 Lxxx!) — **必须在原文件 grep 行号, 禁止数 cat 输出** |
| 2 | 内容密度 | wc -l 每篇 |
| 3 | 语义 | 逐行 Read, 前后矛盾/数据流跳步 |
| 4 | 技术声明 | grep 源码确认归属类 (防编造) |
| 5 | 算法正确性 | 数据流步骤顺序对照源码 |
| 6 | 机制归属 | 方法/字段的确切类 (不凭继承/主题推断) |

### 1.5 复用 ≠ 省略 (五件事检查)

- 纯机制内核 → **一行引用**; 本层使用/组装/配置/生命周期/差异 → **必须展开**
- **五件事**: ①入口 ②配置 ③生命周期 ④差异 ⑤边界 — 全无增量才允许纯引用
- **底线: 拿不准宁可展开也不引用**
- **禁止正文引用未分析域** — 只允许结尾桥引出下一步; 总览域可用导航式指针, 但机制域正文的跨域引用必须指向**已分析**域 (D-1/D-5 借还对 D-2 用导航指针, D-3/D-4 对 D-6 用导航指针 — 环已化解)

---

## §二 全部产出清单 (9 域 / 9 篇)

> 每域含核心机制 + 关键行号锚点 (写作/复习速查)

### 第1层 池核心 (D-1/D-5, 全 🔴)

| 域 | 核心机制 | 关键行号锚点 |
|:--|:--|:--|
| D-1 连接池核心 | 固定数组+ReentrantLock+Condition(notEmpty/empty) 阻塞模型; init 链路; 借出 createDirect/maxWaitThreadCount/onFatalError/pollLast; 归还 rollback→reset 四态→putLast; shutdown+异常联动 | DruidDataSource.java:659,772,807,1332,1366,1543,1565,1613,1620,1768,1894,1938,2029,2081,2194,2214,2218; DruidAbstractDataSource.java:243,299; DruidConnectionHolder.java:371; DruidPooledConnection.java:236,329 |
| D-5 维护体系 | shrink 四阶段(fatalError 增量→驱逐 checkCount/min/maxEvictable→keepAlive→arraycopy 紧凑); 三线程+DestroyTask+CreateConnectionTask(固定间隔重试非指数退避); removeAbandoned 超时强收+栈追踪 | DruidDataSource.java:2724,2846,2887,2925,3069,3084,3100,3121,3151,3183,3252,3905 |
| D-2 Filter 拦截链 | 五层类族(Filter 接口→Adapter→EventAdapter 模板→FilterChain 契约→ChainImpl 递归); pos/nextFilter 下推; 三类链尾(raw/池操作/包装); proxy/jdbc 对象模型; per-connection chain 复用; 两种实现风格 | Filter.java:35; FilterChainImpl.java:37,469,3005,5067; FilterEventAdapter.java:30,178; DruidConnectionHolder.java:223,234; FilterManager.java:36,53,99; WallFilter.java:480; StatFilter.java:378 |

### 第3层 监控安全 (D-3/D-4, 全 🔴)

| 域 | 核心机制 | 关键行号锚点 |
|:--|:--|:--|
| D-3 StatFilter | 模板钩子族埋数(statementExecuteBefore/After/Batch/ErrorAfter); JdbcSqlStat 懒创建挂载 StatementProxy; 耗时/updateCount/并发计数; 慢 SQL(3000ms+JSON 参数快照); 参数化聚合 mergeSql→key; 三级统计 | StatFilter.java:378,412,429,468,499,562,677,708; JdbcSqlStat.java:33; JdbcDataSourceStat.java:38 |
| D-4 WallFilter | 前置守卫(override statement_execute, check 在 chain 前); Provider 按 dbType 装配(SPI+7 内置); 双路径(白名单 getWhiteSql 快路径 vs AST 硬检查 parse→visit→violations); WallConfig 规则; 黑/白名单回填 | WallFilter.java:110,146,480,485; WallProvider.java:441,468,481,521,572,587,625; WallConfig.java:45,75,80 |

### 第4层 支撑 (D-6~D-9)

| 域 | 核心机制 | 关键行号锚点 |
|:--|:--|:--|
| D-6 SQL Parser | 四层架构(Lexer→Parser→AST→Visitor)+29 方言目录+SchemaRepository 元数据; 消费者 D-3 参数化/D-4 检查 | SQLStatementParser.java:49,118; Lexer.java:41; SchemaRepository.java:54; SQLUtils.java |
| D-7 连接验证 | SPI 6 实现按 driver 类名匹配; 双路径(checker vs validationQuery); execValidQuery 裸连快路径; 三时机(testOnBorrow/WhileIdle/Return); onFatalError 复位 | ValidConnectionChecker.java:21; JDBC4ValidConnectionChecker.java:25; ValidConnectionCheckerAdapter.java:40,47; DruidDataSource.java:1240,1385,1421,1980; DruidAbstractDataSource.java:1434,1444,1478 |
| D-8 PSCache | per-connection LRU(LinkedHashMap accessOrder+removeEldestEntry, 默认 10); 借出命中/in-use 保护; 归还三分支(put/remove/物理关); 全参数键; Oracle 隐式缓存 | PreparedStatementPool.java:185,192; DruidAbstractDataSource.java:118; DruidPooledConnection.java:138,173,355; DruidPooledPreparedStatement.java:153,910 |
| D-9 Boot3 | 条件注解组(Property/Class/Before/Enable+@Import 4 配置); Wrapper 双前缀回退+autoAddFilters; 8 Filter 条件注册; StatViewServlet/WebStatFilter 按需 | DruidDataSourceAutoConfigure.java:44,47,63; DruidDataSourceWrapper.java:30,36,49,52; DruidFilterConfiguration.java; DruidStatViewServletConfiguration.java:29 |

### completeness-questions

每域 3 身份(开发者/架构师/学生) / 13-14 问 — 见各域目录

---

## §三 质量标准 — 检查命令 (每域完成必跑)

```bash
# KP 检查
grep -cP '^\| P[123]' knowledge-planning/d{NN}-*.md        # >0
grep -P '^\| P[123]' knowledge-planning/d{NN}-*.md | grep -cP '🔴|🟡|🟢'  # = P条目数
grep -P '^\| P[123]' knowledge-planning/d{NN}-*.md | grep -c '为什么'      # = P条目数

# 大纲五项全等 + 密度 + 结尾桥 + 跨层
for e in '^### ' '^数据流:' '^源码路径:' '^场景:' '^关键设计:'; do grep -c "$e" outlines/d{NN}-*/01-*.md; done  # 五者相等
wc -l outlines/d{NN}-*/01-*.md     # 🔴 39-69 / 🟡 35-49
tail -1 outlines/d{NN}-*/01-*.md | grep -c '→ 引出'  # =1
grep -c '视角' outlines/d{NN}-*/completeness-questions.md  # ≥3
grep -c '^| [0-9]' outlines/d{NN}-*/completeness-questions.md  # ≥5
```

**行号验证** (关键, 含内联 Lxxx): 必须在**源码原文件** grep 行号 (`grep -n '方法名' File.java`), **禁止数 cat/去头输出** — 本会话因此错 11 处 (见 §四 谱系 #5/#8/#9/#11)。

---

## §四 缺陷谱系 — 本会话 REVIEW 全部发现 (9 处修复)

| # | 域 | 类型 | 错误 | 修复 | 教训 |
|:--:|:--|:--|:--|:--|:--|
| 1 | D-1 | 行号/归属错误 | getConnection 写 L2290(带参版), 实为 L1332 | 修正大纲+KP | 方法签名 grep 确认再锚定 (基线亦未给行号) |
| 2 | D-2 | 行号错位 | FilterChainImpl 链尾写 L130/L133/L135(错 1 位) | 修正 L129/L132/L138/L140 | 方法体内部逐行核对 |
| 3 | D-2 | 行号错位 | FilterManager aliasMap 写 L35(空行) | 修正 L36/L38/L42 | 静态块行号 grep 精确 |
| 4 | D-7 | 行号概写 | execValidQuery 段 5 个行号全错(数 cat 输出) | 修正 L40/47/66/68/69 | **禁止数 cat 输出, 必须 grep 原文件** |
| 5 | D-8 | 行号错位 | closePoolableStatement put 写 L174 | 修正 L173/L186 | 同一 |
| 6 | D-4 | 行号错位 | addBlackSql/addWhiteSql 写 L560/L585 | 修正 L572/L587; whiteListHitCount L634 | 调用点行号 grep |
| 7 | D-6 | 数字错误 | 方言目录写 28 | 修正 29 | 目录计数 ls 数全 (数漏 1 个) |
| 8 | D-9 | 密度超限 | 大纲 50 行超 🟡 上限 49 | 压缩合并至 49 | 密度上限超了必须压缩不能升级 |
| 9 | 全局 | 桥链不一致 | D-2 桥 →D-3 与执行序(D-2→D-7)冲突 | 改 D-2 桥+header+KP →D-7 | 结尾桥链 vs 执行序全量回归 (三处一致) |

**共性规律**: ①行号必须 grep 原文件(禁止数输出) ②方法体内部逐行核对 ③数字/目录穷举 ④密度上限严格遵守 ⑤桥链与执行序全量对照

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
    │   ├── HANDOFF-DRUID.md (本文)
    │   ├── DRUID-PLAN.md    ← 知识网络化规划 (9 域 v5: 域清单/追踪表/依赖图/排除/负面空间/缺陷谱系)
    │   ├── knowledge-planning/ (9 个 KP: d1-core ~ d9-boot3, 每 KP 含 §0.8+01提取+P1P2P3+05闭环结论摘要)
    │   ├── outlines/  (9 目录 / 每域 2 文件: 01-*.md + completeness-questions.md — 对齐 HikariCP 规划)
    │   └── harness/  (MiniDruidPool/MiniFilterChain 极简复现, 跑通)
    └── ...

源码:
├── /data/workspace/source-code/code/spring/druid/   ← Druid (core 1614 文件, com.alibaba.druid)
└── (阶段3 后续: mybatis-plus/mybatis/redis/redisson/elasticsearch)
```

**重要**: ①Druid 结构: pool/(DruidDataSource/DruidAbstractDataSource/Holder/PooledConnection/vendor) + filter/(Filter/Adapter/EventAdapter/ChainImpl/stat/wall) + sql/(parser/ast/visitor/dialect/repository) + wall/ + stat/ + proxy/jdbc/ + support/(淘汰) ②pool/ 72 文件含子目录 ③MBean/StatService 服务层已排除(只读暴露, 对照 H-10 因热改 setter 才成域)

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

📌 双链格式 (每篇大纲 header 已写): 前置/复用/引出 — 9/9 篇全部就绪
```

---

## §七 下一步行动

- **本阶段完成**: 9/9 域全部交付 (KP+大纲+questions+六层深审+全量回归)
- **下一阶段**: 阶段3.3 MyBatis-Plus (9 域) — 参照本阶段流程: 先做域发现/规划 (同 DRUID-PLAN 做法), 再逐域走 v5 全管线
- **与 Boot 呼应**: S-10 Boot DataSource 已标注"深入在阶段3" — 本阶段闭环
- **📌 Obsidian 知识图谱**: 全部域大纲转双链 vault (全局待办, 同 HikariCP)

---

## §八 质量追踪

| 指标 | 数据 |
|:--|:--:|
| Druid 完成 | 9 域/9 篇 全通过 (问题驱动重写完成, D-3/D-4/D-9 已按闭环深度重写) |
| REVIEW 修复 | 9 处真实问题 (见 §四 缺陷谱系) + PLAN 阶段 19 处 (见 DRUID-PLAN §九) + 后续: testWhileIdle 默认 true 编造已修正、D-1 putLast 边界补充 |
| 全量回归 | 结构 9/9、KP 9/9 (P1P2P3+色+为什么 全等)、跨层 9/9 (5🔴/4🟡)、桥链 9/9 与执行序一致、行号 9/9 全有效 |
| 行号验证 | 9 域全量 find+sed 验证 (修复 13 处) |
| 密度 | 🔴 61-67 / 🟡 47-49 (全在区间) |
| 闭环深度 | 9 域 **54 条**闭环结论内化于 KP §05 (Pass 2 问题驱动, 假设→验证→结论) |
| 问题驱动补强 | 本轮逐域读源码补 15 个新闭环: D-2(Error 三路 catch/cloneChain 预留/链尾包装规则) D-7(空闲基准三来源/负 idle 防御/各库验证 SQL) D-5(running 跟踪/emptyWait) D-8(Oracle 自适应预取/峰值语义) D-6(参数化流程/双输出) D-3(计数原子化/双直方图) D-4(白名单 LRU 参数化 key) D-9(AOP 装配); 修正 D-4 白名单机制(非正则, 是 ConcurrentLruCache+参数化 key) |
| questions | 9/9 域 ≥3身份/15-17问 (补 21 问) |
| 结构对齐 | outlines/ 每域 2 文件 (01-*.md + completeness-questions.md) — 与 HikariCP 规划一致; 闭环证据在 KP §05 |
