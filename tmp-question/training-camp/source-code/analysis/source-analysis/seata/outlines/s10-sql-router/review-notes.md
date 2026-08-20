# S-10 SQL 路由 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "7种SQL类型" — 实证路由 **6 类型 + Multi + Plain** (ExecuteTemplate:100-168); SQLType **48 值** (0-44 + 101-103) — 表述精确化 | 大纲 §1/§3 |
| 2 | **补充锚点** | **INSERT_ON_DUPLICATE_UPDATE/UPDATE_JOIN 仅 3 方言** (MySQL/Mariadb/PolarDBX) — 其他 NotSupportYetException (L125-158) — 显式不支持优于错执行 | 大纲 §1 |
| 3 | **语义标注** | **守卫直通**: !requireGlobalLock && AT != branchType → 原 statement (L69-73) — 非 AT 零开销 (无识别无路由) | 大纲 §1 |
| 4 | **补充锚点** | **InsertExecutor SPI 方言加载**: EnhancedServiceLoader.load(InsertExecutor, dbType) (L101-106) — 方言可插拔 | 大纲 §2 |
| 5 | **补充锚点** | **sqlparser 三模块**: core/druid/antlr — SQLRecognizerFactory SPI (sqlParserType 配置切换) | 大纲 §3 |
| 6 | 行号验证 | 全函数 ~25 锚点 + 跨文件 grep (ExecuteTemplate 56-177 / MultiExecutor 45-88 / SQLVisitorFactory 28-47 / SQLType 23-218 / exec 方言目录) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 路由表 6+Multi+Plain
- 方言分支
- 异常包装

### 维度2 性能
- 守卫直通 (非 AT 零开销)
- 无解析缓存 (每 SQL 解析)

### 维度3 内存
- SQLRecognizer 列表
- 识别器 SPI 单例

### 维度4 一致性
- Plain 兜底 (未识别不拦截)
- NotSupportYet 显式
- SQLException 统一

### 维度5 负面空间 (已写入大纲 6 条)
- 不全部 SQLType 支持/不 SQL 改写/不解析缓存/不方言全支持/不 DDL 拦截/不 SQL 归一化

## 结论
S-10 锚点 ~25 处验证, 8 闭环完成, **数字穷举 1 + 语义标注 1 + 补充锚点 3**。🟡 B 无 harness。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-4/S-2 ✅; 对照 MyBatis/Druid ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 6 条 ✅; 横切 (路由/executor/识别/边界) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §1 未提 **MultiExecutor 的 sqlserver 变体**: SqlServerMultiUpdate/MultiDelete (MultiExecutor:81-88) — 批量语句的方言差异 | 大纲 §1 补注 |
| 9 | 通过项 | 其余 ~22 句机制描述逐句对源码一致 ✅ (路由/executor/识别/边界) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (Multi 方言变体 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 守卫正确 | 非 AT 直通 — 不误拦截 ✅ | 通过 |
| V2 | 方言可达 | Insert SPI 加载 — 新库可插 ✅ | 通过 |
| V3 | 不支持显式 | NotSupportYet — 不静默错执行 ✅ | 通过 |
| V4 | 兜底安全 | Plain — 未识别不拦截 ✅ | 通过 |
| V5 | 异常契约 | SQLException 统一 — 调用方简单 ✅ | 通过 |
| V6 | Multi 镜像 | 批量语句逐一镜像 ✅ | 通过 |
| V7 | GlobalLock 路由 | requireGlobalLock 走路由 — 锁场景 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **SELECT_FOR_UPDATE 的 SqlServer 变体**: SqlServerSelectForUpdateExecutor (L117-123) — 方言差异面 (S-2 行锁在 sqlserver 的差异) | 大纲 §1 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (SqlServer SelectForUpdate), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (SQLType 覆盖/识别器多值/Plain 场景/方言目录数/解析器切换)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | SQLType 覆盖? | 48 值 (0-44 + 101-103) — SELECT 族最多 (SELECT_FOR_UPDATE/LAST_INSERT_ID/WITHOUT_TABLE/FROM_UPDATE/FOUND_ROWS 等 6+ 变体) | 通过 (验证) |
| T2 | 多识别器场景? | 单语句多识别器 (SQL 解析器可返回多个) → MultiExecutor — 罕见但支持 | 通过 (验证) |
| T3 | Plain 场景? | 未识别 SQL (非 6 类型) → 原样 — 如 SELECT 普通查询也 Plain (无镜像) | 通过 (验证) |
| T4 | 方言目录数? | exec/ **10 方言目录** (mysql/oracle/pg/sqlserver/dm/kingbase/mariadb/oceanbase/oscar/polardbx) | 通过 (验证) |
| T5 | 解析器切换? | sqlParserType 配置 → SQLRecognizerFactory (druid/antlr) — 运行时选择 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | SELECT 族完备 | 6+ 变体 — 只读场景识别 ✅ | 通过 |
| V2 | 多识别安全 | Multi 逐一处理 ✅ | 通过 |
| V3 | Plain 零副作用 | 未识别原样 — 无镜像安全 ✅ | 通过 |
| V4 | 方言 10 覆盖 | 主流库齐备 ✅ | 通过 |
| V5 | 解析可换 | 双解析器 SPI ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **SELECT 普通查询也走 Plain** (T3): 非 SELECT_FOR_UPDATE 的 SELECT → Plain 原样 — 只读无镜像 (镜像只对 DML) | 大纲 §4 注 |

## 反写测试 (只读大纲能否写文章)

- §1 路由表 (守卫/6 分支/Multi 方言/异常) — 可写 ✅
- §2 executor 族 (基类链/10 方言/Insert SPI) — 可写 ✅
- §3 SQL 识别 (SPI 工厂/48 值/三模块) — 可写 ✅
- §4 边界面 (守卫/Plain 兜底/NotSupportYet) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (SELECT 也 Plain)。核心认知: **路由 6+Multi+Plain** (48 值仅消费 8 类型) + **2 类型仅 3 方言** (显式不支持) + **守卫直通** (非 AT 零开销) + **SELECT 只读无镜像**。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 调用链/Multi/工厂穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (SQLRecognizer 契约/StatementProxy 调用链/工厂委托/Multi 执行流/双解析器注册)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | SQLRecognizer 契约? | **3 方法**: getSQLType/getTableName/getOriginalSQL (L23-56) | 通过 (验证) |
| T2 | StatementProxy 调用链? | **executeQuery/executeUpdate/execute 全部经 ExecuteTemplate.execute(this, callback, sql)** (L63-77) — StatementCallback 模式, 路由入口完整 | 发现 12 (补锚) |
| T3 | 工厂委托? | SQLVisitorFactory.get → **SQL_RECOGNIZER_FACTORY.create(sql, dbType)** — 工厂方法模式 (双解析器由 LoadLevel 注册) | 通过 (验证) |
| T4 | Multi 执行流? | **按 tableName 分组 (groupingBy getTableName, L76)** → 每组按 SQLType 选 MultiUpdate/MultiDelete → **default → UnsupportedOperationException** (L97-99) — **Multi 仅支持 UPDATE/DELETE** | 发现 13 (补锚) |
| T5 | 双解析器注册? | **@LoadLevel(SQL_PARSER_TYPE_DRUID/ANTLR)** (druid/antlr 模块) — SPI 可切换实证 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 入口完整 | 三入口全经模板 — 无旁路 ✅ | 通过 |
| V2 | 分组正确 | 按表聚合 — 同表批量镜像 ✅ | 通过 |
| V3 | Multi 有界 | 仅 UPDATE/DELETE — 其他显式不支持 ✅ | 通过 |
| V4 | 工厂委托 | create 委托 — 解析器可换 ✅ | 通过 |
| V5 | SPI 注册 | LoadLevel 双实现 — 配置切换 ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **StatementProxy 三入口全经路由** (T2, L63-77): executeQuery/executeUpdate/execute — 无旁路 | 大纲 §1 注 |
| 13 | **语义标注** | **Multi 仅 UPDATE/DELETE** (T4, L97-99): 按表分组 + default UnsupportedOperationException — 批量镜像有界 | 大纲 §1 注 |

## 反写测试 (只读大纲能否写文章)

- §1 路由表 (守卫/6 分支/Multi 有界/三入口) — 可写 ✅
- §2 executor 族 (基类链/10 方言/Insert SPI) — 可写 ✅
- §3 SQL 识别 (工厂委托/48 值/双解析器) — 可写 ✅
- §4 边界面 (守卫/Plain 兜底/NotSupportYet) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 2 处全部修复** (三入口无旁路/Multi 仅两类型)。核心认知: **StatementProxy 三入口全经路由** (无旁路) + **Multi 按表分组仅 UPDATE/DELETE** (default 显式不支持) + **工厂委托 create** (双解析器 LoadLevel SPI)。大纲经修复后反写测试全过。
