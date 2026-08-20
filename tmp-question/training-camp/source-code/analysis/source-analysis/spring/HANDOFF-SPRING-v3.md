# Spring 框架源码分析 — 交接文档 (v3)

> **日期**: 2026-08-11 (v3 重写 — 上一会话上下文已满)
> **给新 AI**: 本文档是继续 Spring 源码分析的唯一入口。不要跳过任何节。
> **你的任务**: 按照 §一 方法论，从 §七 的 W-5 开始，逐域推进 v5 全管线(KP → 大纲 → completeness-questions → 深审)。**禁止批量写/批量修**。

---

## §零 当前状态速查

### 全局进度

| 阶段 | 框架 | 层 | 域数 | 篇数 | 状态 |
|:--:|------|------|:--:|:--:|:--:|
| Stage 1 | Netty | — | 13章 | 36篇 | ✅ 100% |
| Stage 1 | Tomcat | — | 7域 | 19篇 | ✅ 100% |
| Stage 2 | Spring | spring-beans | 7 | 11 | ✅ 100% |
| Stage 2 | Spring | spring-context | 16 | 19 | ✅ 100% |
| Stage 3-4 | Spring | spring-aop+tx | 10 | 10 | ✅ 100% |
| Stage 5 | Spring | spring-jdbc | 3 | 3 | ✅ 100% |
| **Stage 6** | **Spring** | **spring-core** | **7** | **7** | ✅ (补缺完成) |
| **Stage 6** | **Spring** | **spring-web** | **7/?** | **7** | ✅ (补缺完成) |
| **Stage 6** | **Spring** | **spring-test + websocket** | **6/?** | **6** | ✅ (补缺完成) |
| **Stage 7** | **Spring Boot** | **自动装配** | **15/?** | **15** | 🔄 进行中 (BOOT-PLAN-v2) |
| **合计** | | | **80** | **86篇** | **~86%** |

### spring-web 进度

| W | 主题 | 目录 | 篇 | 状态 |
|:--|------|------|:--:|:--:|
| W-1 | DispatcherServlet doDispatch | s37-web-dispatcher | 1 | ✅ (深审过) |
| W-2 | @RequestMapping → HandlerMethod | s38-web-requestmapping | 1 | ✅ (深审过) |
| W-3 | RequestMappingHandlerAdapter | s39-web-adapter | 1 | ✅ (深审过) |
| W-4 | HandlerMethodArgumentResolver | s40-web-resolver | 1 | ✅ (深审过) |
| W-5 | HttpMessageConverter (JSON/XML) | s41-web-messageconverter | 1 | ✅ (深审过) |
| W-6 | ViewResolver | s42-web-viewresolver | 1 | ✅ (深审过) |
| W-7~ | (已暂停扩展 — 改为主线补缺口, 见 §七) | — | — | ⏳ |

### 重要历史 (2026-08-11 大深审)

- 全量六层深审 45 篇完成 (详细报告: `AUDIT-2026-08-11.md`), 修复 ~186 处缺陷
- 随后新增 W-3 (s39)、W-4 (s40) 两域, 各自六层深审通过
- 全量四要素检查 (场景/源码路径/关键设计/数据流 per 节) 修复 13 篇历史遗留
- s30-tx-failures 从 🟡 改为 🔴 Deep (实际 57 行内容深度匹配, KP 同步改)
- **最大教训**: 批量修复曾引入 17 处节=流破坏 — 永远逐篇深审

---

## §一 方法论 — 完整内联 (每域必走, 禁止跳过)

```
01 逐源提取(Agent 并行) → 01 聚合(P1≥5/P2 2-4/P3 1) → 02 深度分类(🔴🟡🟢 per KP, 必须有"为什么")
  → 03 聚类(教学顺序+每个决定的原因) → v5 大纲(AI 自写, 对照源码 grep 验证行号)
  → completeness-questions(≥3 身份, ≥5 问) → 六层深审 → 修复 → HANDOFF 更新
```

**禁止跳过任何步骤**。Agent 只做逐源提取表 — 大纲由 AI 对照源码自写(grep 验证行号)。

**⚠️ 方法论正式文档在 `talk-method/source-code-analysis/methodology/zh/` (01-08)** — 本 HANDOFF 内联为速查, 冲突时以正式文档为准。新增跨框架规则见 06 §2.5 **"复用 ≠ 省略 — 分层视角与五件事检查"**: 引用下层机制时, 机制内核一行引用, 但本层次的使用/组装/配置/生命周期/差异必须展开; 五件事检查 (①入口②配置③生命周期④差异⑤边界) 全无增量才允许纯引用; **拿不准宁可展开也不引用**。

### 1.2 v5 大纲格式 — 每节四要素

```
### N. 机制名 — 一句话描述
场景: [真实场景, 读者知道"为什么要看这个"]          ← 必须有"场景:" 前缀
源码路径: [FileName.java:行号 + 函数名]              ← 必须有"源码路径:" 前缀
关键设计: [为什么这样实现, 标注 [模式: XXX]]         ← 必须有"关键设计:" 前缀
数据流: [代码级 trace, 完整方法调用链]              ← 必须有"数据流:" 前缀
```

**严禁**: 裸行号 `(line 233)` / 伪行号 / 文件总行数代行号 / 代码拼接 / 技术清单式 bullet / 缺任一四要素

### 1.3 密度标准 (行数硬性约束)

| 级别 | 行数/篇 | 上限 |
|:--:|:--:|:--:|
| 🔴 Deep | 39-69 | 超 69 需拆分 |
| 🟡 Working | 35-49 | 超 49 需压缩或升级 🔴 |

**经验**: 大纲初稿常超上限 → 压缩"场景"为短句、合并源码路径 bullet、合并重复数据流段。压缩后立即 `wc -l` 复查。

### 1.4 六层深审 (每域宣称完成前必执行)

| 层 | 检查项 | 检测方法 |
|:--:|------|------|
| 1 | 源码行号 | `sed -n '行号p' File.java` — 验证非空/非 `}` (含内联 Lxxx 引用!) |
| 2 | 内容密度 | `wc -l` 每篇 — 低于/超过阈值→修正 |
| 3 | 语义 | 逐行 Read — 前后矛盾 / 编辑残留 / 数据流跳步 |
| 4 | 技术声明 | grep 源码确认归属类 (防编造: 类名/方法名/常量/异常消息) |
| 5 | 算法正确性 | 数据流步骤顺序逐条对照源码 |
| 6 | 机制归属 | 方法/字段的确切类 (不凭继承推断) |

### 1.5 全维度检查 — 四层必须全部覆盖

| 维度 | 检查项 | 检测 |
|:--|------|:--|
| KP层 | P1P2P3 + 🔴🟡🟢 per KP + 为什么列 | `grep -cP '^\| P[123]'` ≥1 且每行带色+为什么 |
| 大纲层 | 每节四要素全 + 结尾桥 | 节数 = 数据流数 = 源码路径数 = 场景数 = 关键设计数 |
| 管线层 | completeness-questions ≥3 身份 ≥5 问 | `grep -c '视角'` ≥3 |
| 跨层 | KP 域类型 = 大纲 subtitle 域类型 | 🔴/🟡 一致 |

### 1.6 行号防御 — 关键

- 行号引用前 **grep -n 实际声明行** (grep 不是 wc -l!)
- 注解/接口文件尤其危险 (行号常指向 `}`)
- **方法声明行 vs 调用点**: 引用时标注清楚 (`createXxx 声明在 L100, 调用在 L50`)
- 验证: `sed -n '行号p' File.java | grep -q '^\s*}' && echo WARN`

---

## §二 全部产出清单 (40 域 / 47 篇)

### spring-beans (7 域 / 11 篇 ✅)
| 域 | 主题 | 篇 | 状态 |
|:--|------|:--:|:--:|
| s1-beandefinition | BeanDefinition | 2 | ✅ |
| s2-beanfactory | BeanFactory | 2 | ✅ |
| s3-bean-lifecycle | 生命周期 | 2 | ✅ |
| s4-circular-dependency | 循环依赖 | 1 | ✅ |
| s5-di-injection | DI 注入 | 2 | ✅ |
| s6-bpp-panorama | BPP 全景 | 1 | ✅ |
| s7-factorybean | FactoryBean | 1 | ✅ |

### spring-context (16 域 / 19 篇 ✅)
| 域 | 主题 | 篇 |
|:--|------|:--:|
| s8-refresh | refresh() 12步 | 1 |
| s9-configuration | @Configuration 解析 | 3 |
| s10-event | 事件机制 | 2 |
| s11-messagesource | MessageSource | 1 |
| s12-parent | 父子容器 | 1 |
| s13-bfpp | BFPP 全景 | 1 |
| s14-scope | Bean 作用域 | 1 |
| s15-appcontext | AppContext 实现 | 1 |
| s16-conditional | @Conditional | 1 |
| s17-async | @Async | 1 |
| s18-scheduled | @Scheduled | 1 |
| s19-cacheable | @Cacheable | 1 |
| s20-lazy-primary-depends | @Lazy/@Primary/@DependsOn | 1 |
| s21-aot | AOT/Native Image | 1 |
| s22-stereotype | 元注解/@Validated/@DateTimeFormat | 1 |
| s23-import | @Import/@EnableXxx | 1 |

### spring-aop + tx (10 域 / 10 篇 ✅)
s24-aop-proxy / s25-advice-chain / s26-aspectj / s27-autoproxy / s28-pointcut / s29-tx-chain / s30-tx-failures(🔴) / s31-tx-propagation / s32-tx-exception / s33-tx-sync

### spring-jdbc (3 域 / 3 篇 ✅)
s34-jdbc-template / s35-jdbc-advanced / s36-jdbc-batch

### spring-web + websocket/messaging (12 域 / 12 篇 ✅)
s37-web-dispatcher / s38-web-requestmapping / s39-web-adapter / s40-web-resolver / s41-web-messageconverter / s42-web-viewresolver / s54-web-interceptor / s55-web-exception / s56-web-initbinder / s57-web-webflux

### spring-core + SpEL + context 补缺 (11 域 / 11 篇 ✅ 补缺口)
s43-core-resource / s44-core-conversion / s45-core-environment / s46-core-ordered / s47-core-annotation / s48-core-profile / s49-core-taskexecutor / s50-spel / s51-apprunner / s52-classpathindex / s53-jdbc-datasource / s54-web-interceptor / s55-web-exception / s56-web-initbinder

---

## §三 质量标准 — 检查命令 (每域完成必跑)

```bash
# KP 检查
grep -cP '^\| P[123]' knowledge-planning/{域}.md        # >0
grep -P '^\| P[123]' knowledge-planning/{域}.md | grep -cP '🔴|🟡|🟢'  # = P条目数
grep -P '^\| P[123]' knowledge-planning/{域}.md | grep -c '为什么'      # = P条目数

# 大纲检查 (五项全等!)
grep -c '^### ' outlines/{域}/0*.md          # 节数
grep -c '^数据流:' outlines/{域}/0*.md        # = 节数
grep -c '^源码路径:' outlines/{域}/0*.md      # = 节数
grep -c '^场景:' outlines/{域}/0*.md          # = 节数
grep -c '^关键设计:' outlines/{域}/0*.md      # = 节数

# 密度
wc -l outlines/{域}/0*.md   # 🔴 39-69 / 🟡 35-49

# 行号检查 (含内联 Lxxx!)
grep -oP '[A-Z][A-Za-z]+\.java:\d+' outlines/{域}/0*.md | while read r; do
  sed -n '{行号}p' {文件} | grep -qE '^\s*(\}|$)' && echo "WARN"
done

# 结尾桥 (在文末, 含"→ 引出"或"next")
tail -1 outlines/{域}/0*.md

# questions
grep -c '视角' outlines/{域}/completeness-questions.md  # ≥3
grep -c '^| [0-9]' outlines/{域}/completeness-questions.md  # ≥5

# 跨层
grep -oP '🔴 Deep|🟡 Working' knowledge-planning/{域}.md | head -1  # 与大纲 subtitle 一致
```

---

## §四 缺陷谱系 — 已发现的缺陷类型及防御 (26 种)

| # | 类型 | 示例 | 防御 |
|:--:|------|------|------|
| 1 | 文件总行数代行号 | `File.java:102` → L102 是 `}` | sed 验证 |
| 2 | 零行号引用 | 全文无 `.java:数字` | grep -c |
| 3 | 字段名编造 | 不存在的字段 | grep 字段名 |
| 4 | 方法名编造 | 不存在的类/方法 | grep 全库 |
| 5 | 算法顺序编造 | 优先级颠倒 | 逐步骤 grep |
| 6 | 术语误用 | 概念混淆 | 人工判断 |
| 7 | 机制归属编造 | 方法标错类 | grep 谁调用 |
| 8 | 缓存归属编造 | 字段归错父类 | grep 定义类 |
| 9 | KP 缺 P1P2P3 | 早期域全缺 | 逐域 grep |
| 10 | 大纲缺数据流 | 节无数据流段 | 节数=流数 |
| 11 | 大纲缺源码路径 | 节无路径段 | 节数=路径数 |
| 12 | 结尾桥缺失 | 文末无桥 | tail -1 |
| 13 | KP 类型 ≠ 大纲类型 | KP🔴 文章🟡 | 跨层对比 |
| 14 | 行号指向 `}` (注解文件) | 17次复发 | sed+grep |
| 15 | 🔴 密度不达标 | 32行标🔴 | wc -l |
| 16 | 跨层篇数不一致 | KP说3篇实际2篇 | ls 计数 |
| 17 | 行号数组衰减 | 尾篇引用变少 | 分布检查 |
| 18 | **批量修复引入结构破坏** | 17处节=流不匹配 | **永远逐篇** |
| 19 | **节缺场景/关键设计段** | 13篇历史遗留 | 五项计数全等 |
| 20 | **内联 Lxxx 行号错** | L962 实际 L964 | 全部内联也验证 |
| 21 | **方法声明行 vs 调用点混淆** | createXxx(L1005) 非调用点 | 标注声明/调用 |
| 22 | **超密度上限** | 🟡 50行 | wc -l 复查 |
| 23 | **编造转换器/依赖** | JAXB 在 Spring6 已移除 | grep 全库存在性 |
| 24 | **场景与数据流矛盾** | 场景URL无参但数据流有参 | 逐行对照 |
| 25 | **重载方法选错行号** | batchUpdate 选错重载 | 确认签名 |
| 26 | **find 同名类误报** | reactive/test 同名 | 精确包路径 |
| 27 | **KP 文本格式缺色** | `**P1 核心**` 无 🔴🟡🟢 (9个KP) | grep P标题带色数 |
| 28 | **KP 表格格式缺为什么列** | `| 级别|色|KP|篇 |` 无为什么 (7个KP) | grep P表带为什么数 |
| 29 | **交接文档虚假声称** | "40/40全达标"但实际16个KP不完整 | 每次更新 HANDOFF 前重跑 KP 检查 |

---

## §五 完整领域域映射 (40 域)

| 大纲目录 | KP 文件 | 主题 | 篇 |
|------|------|------|:--:|
| s1-beandefinition | s1-beandefinition.md | BeanDefinition | 2 |
| s2-beanfactory | s2-beanfactory.md | BeanFactory | 2 |
| s3-bean-lifecycle | s3-bean-lifecycle.md | 生命周期 | 2 |
| s4-circular-dependency | s4-circular-dependency.md | 循环依赖 | 1 |
| s5-di-injection | s5-di-injection.md | DI注入 | 2 |
| s6-bpp-panorama | s6-bpp-panorama.md | BPP全景 | 1 |
| s7-factorybean | s7-factorybean.md | FactoryBean | 1 |
| s8-refresh | s8-refresh.md | refresh() | 1 |
| s9-configuration | s2-2-configuration.md | @Configuration | 3 |
| s10-event | s3-event.md | 事件机制 | 2 |
| s11-messagesource | s4-messagesource.md | 国际化 | 1 |
| s12-parent | s5-parent.md | 父子容器 | 1 |
| s13-bfpp | s6-bfpp.md | BFPP全景 | 1 |
| s14-scope | s7-scope.md | Bean作用域 | 1 |
| s15-appcontext | s8-appcontext.md | AppContext | 1 |
| s16-conditional | s9-conditional.md | @Conditional | 1 |
| s17-async | s10-async.md | @Async | 1 |
| s18-scheduled | s11-scheduled.md | @Scheduled | 1 |
| s19-cacheable | s12-cacheable.md | @Cacheable | 1 |
| s20-lazy-primary-depends | s13-lazy-primary-depends.md | @Lazy/@Primary/@DependsOn | 1 |
| s21-aot | s14-aot.md | AOT/Native Image | 1 |
| s22-stereotype | s15-stereotype.md | 元注解/@Validated/@DateTimeFormat | 1 |
| s23-import | s16-import.md | @Import/@EnableXxx | 1 |
| s24-aop-proxy | a1-proxy.md | 代理机制 | 1 |
| s25-advice-chain | a2-advice.md | Advice链 | 1 |
| s26-aspectj | a3-aspectj.md | @AspectJ解析 | 1 |
| s27-autoproxy | a4-autoproxy.md | 自动代理 | 1 |
| s28-pointcut | a5-pointcut.md | Pointcut表达式 | 1 |
| s29-tx-chain | t1-transactional.md | @Transactional链路 | 1 |
| s30-tx-failures | t2-failures.md | 失效场景 (🔴) | 1 |
| s31-tx-propagation | t3-propagation.md | 传播行为 | 1 |
| s32-tx-exception | t4-exception.md | 异常翻译 | 1 |
| s33-tx-sync | t5-synchronization.md | TxSynchronization | 1 |
| s34-jdbc-template | j1-jdbctemplate.md | JdbcTemplate | 1 |
| s35-jdbc-advanced | j2-rowmapper.md | RowMapper/Extractor | 1 |
| s36-jdbc-batch | j3-batch.md | batchUpdate | 1 |
| s37-web-dispatcher | w1-dispatcherservlet.md | DispatcherServlet | 1 |
| s38-web-requestmapping | w2-requestmapping.md | @RequestMapping | 1 |
| s39-web-adapter | w3-requestmappinghandleradapter.md | RequestMappingHandlerAdapter | 1 |
| s40-web-resolver | w4-argumentresolver.md | HandlerMethodArgumentResolver | 1 |
| s41-web-messageconverter | w5-messageconverter.md | HttpMessageConverter | 1 |
| s42-web-viewresolver | w6-viewresolver.md | ViewResolver | 1 |
| s43-core-resource | c1-resource.md | Resource | 1 |
| s44-core-conversion | c2-conversion.md | 类型转换 | 1 |
| s45-core-environment | c3-environment.md | Environment | 1 |
| s46-core-ordered | c4-ordered.md | Ordered | 1 |
| s47-core-annotation | c5-annotation.md | 注解元数据 | 1 |
| s48-core-profile | c6-profile.md | Profile | 1 |
| s49-core-taskexecutor | c7-taskexecutor.md | TaskExecutor | 1 |
| s50-spel | c8-spel.md | SpEL | 1 |
| s51-apprunner | c9-apprunner.md | ApplicationRunner | 1 |
| s52-classpathindex | c10-classpathindex.md | ClassPathIndex | 1 |
| s53-jdbc-datasource | c11-datasource.md | DataSource | 1 |
| s54-web-interceptor | c12-interceptor.md | 拦截器 | 1 |
| s55-web-exception | c13-exception.md | 异常处理 | 1 |
| s56-web-initbinder | c14-initbinder.md | @InitBinder | 1 |
| s57-web-webflux | c15-webflux.md | WebFlux | 1 |
| s58-test-mockmvc | c16-mockmvc.md | MockMvc | 1 |
| s59-test-context | c17-testcontext.md | TestContext | 1 |
| s60-test-mockbean | c18-mockbean.md | @MockBean | 1 |
| s61-test-sql | c19-sql.md | @Sql | 1 |
| s62-web-websocket | c20-websocket.md | WebSocket | 1 |
| s63-web-messaging | c21-messaging.md | messaging | 1 |
| s64-context-validation | c22-validation.md | Bean Validation | 1 |
| s65-boot-application | b1-springbootapplication.md | @SpringBootApplication | 1 |
| s66-boot-autoconfiguration | b2-autoconfiguration.md | 自动装配加载 | 1 |
| s67-boot-condition | b3-condition.md | 条件注解 | 1 |
| s68-boot-springapplication | b4-springapplication.md | SpringApplication.run | 1 |
| s69-boot-configproperties | b5-configproperties.md | @ConfigurationProperties | 1 |
| s70-boot-starter | b6-starter.md | Starter 机制 | 1 |
| s71-boot-mvc | b7-mvc.md | MVC 自动装配 | 1 |
| s72-boot-webserver | b8-webserver.md | 嵌入式容器 | 1 |
| s73-boot-httpclient | b9-httpclient.md | HTTP客户端+消息转换 | 1 |
| s74-boot-datasource | b10-datasource.md | DataSource 自动装配 | 1 |
| s75-boot-redis | b11-redis.md | Redis 自动装配 | 1 |
| s76-boot-tx | b12-tx.md | 事务自动配置 | 1 |
| s77-boot-cache | b13-cache.md | 缓存自动配置 | 1 |
| s78-boot-taskexecutor | b14-taskexecutor.md | TaskExecutor 自动配置 | 1 |
| s79-boot-aot | b15-aot.md | AOT/Native Image | 1 |

---

## §六 文件路径

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/
├── issue/源码分析执行计划.md          ← 原始84域执行计划
├── netty/ tomcat/                     ← Stage 1 ✅
└── spring/
    ├── HANDOFF-SPRING-v3.md           ← ← 你正在读 (唯一入口)
    ├── AUDIT-2026-08-11.md            ← 全量深审报告 (~186缺陷)
    ├── knowledge-planning/            ← 40个KP文件
    └── outlines/                      ← 40目录/47篇
        └── s42-web-viewresolver/      ← 最新完成 (W-6)

Spring 源码: /data/workspace/source-code/code/spring/spring-framework/
├── spring-beans/   spring-context/    spring-aop/    spring-tx/
├── spring-jdbc/    spring-webmvc/     spring-web/    spring-core/
└── spring-webflux/ (同名类勿用! find 会误报)
```

**重要**: `find` 会匹配同名类 (reactive/test/integration 包) — 行号验证必须用精确路径或排除这些目录。

---

## §七 下一步行动 — W-6 ViewResolver (W-5 已完成)

### W-5 完成记录 (2026-08-11)

- s41-web-messageconverter: KP (6 条目 P1×3/P2×2/P3×1) + 01-messageconverter.md (🟡 49行, 3节) + completeness-questions (4视角/10问) — 六层深审通过
- 深审修复: RequestMappingHandlerAdapter initMessageConverters 声明 L616 (非614); MappingJackson2 super 调用 L72 (非75); getJsonEncoding 声明 L545 (非565); 3 处区间终点指向 `}` 已收窄; ByteArray readInternal 为 readNBytes/readAllBytes (6.x 已弃 copyToByteArray); Jackson 判定走 AbstractJackson2 L247→L253 而非模板 L134
- 核心链路: 接口四方法(canRead/canWrite/read/write) → AbstractHttpMessageConverter 模板(三抽象点+addDefaultHeaders) → 读链路(readWithMessageConverters: octet-stream 兜底→遍历 canRead) → 写链路(writeWithMessageConverters: Accept 协商→交集→最具体) → Jackson 集成(泛型 JavaType)

### W-5 深度 REVIEW 记录 (2026-08-11, 同会话)

**REVIEW 发现并修复 10 处** (含 2 处实质性错误):
1. **[实质] 默认转换器机制错**: AllEncompassingFormHttpMessageConverter 构造器按 classpath 条件 addPartConverter(Jackson/JAXB/Gson), 但 **仅服务 multipart part** — 原文"默认无 Jackson"不准确 → 改为"顶层 3 转换器, 顶层无 JSON 转换器, 纯 MVC @RequestBody JSON 报 415, Boot 自动装配才补顶层 Jackson" (KP/大纲/questions 三处同步)
2. **[实质] XML 覆盖缺失**: 域主题含 XML 但正文未提 → §1 补 xml/ 族(Jaxb2RootElement/MappingJackson2Xml 条件加载), KP 01 表加 XML 行
3. 数据流 "L295 选中" → L295 是 sortBySpecificity, 选中在 L297-305; 并验证 getMostSpecificMediaType 交集语义 (json×*+json→json)
4. KP typo "headres"→headers; 5. KP 补 L545/L247-253; 6. KP "~46行"→~49行; 7. KP P3-1/D "仅3个"→"顶层3个"; 8. questions 第6问表述; 9. questions 第2问补 configureMessageConverters; 10. 大纲 §3 场景补 @RestController
**REVIEW 验证通过**: Jackson 链不覆写 canRead(MediaType)(走模板 L147 includes 语义); FormHttpMessageConverter partConverters 仅 multipart; 外链 34 处+内联 Lxxx 35 处全部 sed 验证; 五项全等/密度49/桥/questions/跨层全过

### W-6 完成记录 (2026-08-11)

- s42-web-viewresolver: KP (6 条目 P1×3/P2×2/P3×1) + 01-viewresolver.md (🟡 49行, 3节) + completeness-questions (4视角/11问) — 六层深审通过
- 深审修复: canHandle 判断在 L469-470 (非471); RedirectView 默认状态码精确化 (redirectHttp10Compatible=true 默认→sendRedirect 302, 显式状态码或关 1.0 兼容→303 SEE_OTHER); 双缓存描述修正 (LRU 主缓存 viewCreationCache L80, 无锁读镜像 viewAccessCache L77); §3 数据流修正 — "userList.json" 扩展名尝试经 prefix/suffix 拼接会 checkResource 失败, JSON 视图实际来自 defaultViews
- 核心链路: DispatcherServlet.render(L1400) → resolveViewNameInternal(L1481) 责任链 → AbstractCachingViewResolver 双缓存(L172) → UrlBasedViewResolver.createView 三分支(L466: redirect:/forward:/loadView) → buildView(prefix+suffix) → InternalResourceViewResolver(JSP) → InternalResourceView forward/include → ContentNegotiatingViewResolver(Accept→候选→最匹配)
- 与 W-5 对照: 内容协商同源 (转换器链选转换器 vs 视图协商选 View); 结尾桥 → W-7 静态资源

### 主线复盘 (2026-08-11) — 补齐计划内缺口

**复盘结论**: 对照原始执行计划 (issue/源码分析执行计划.md), 已产出 42 域中约 33 域对应计划内, 6 个计划外补充域多为支撑/拆细 (s11/s15/s36/s39/s40), s42 为唯一完全计划外。**计划内未做 15 域** (本次起全部补齐, 按原计划底层→上层):

| 计划 | 域 | 状态 |
|:--|---|:--:|
| 0-1 | Resource | ✅ s43-core-resource (C-1) |
| 0-2 | 类型转换 (ConversionService/Converter/@Value) | ✅ s44-core-conversion (C-2) |
| 0-3 | Environment (PropertySource/@PropertySource) | ✅ s45-core-environment (C-3) |
| 0-4 | Ordered (@Order/PriorityOrdered/比较器) | ✅ s46-core-ordered (C-4) |
| 0-5 | 注解元数据 (MergedAnnotation/@AliasFor) | ✅ s47-core-annotation (C-5) |
| 0-6 | Profile (@Profile→ProfileCondition) | ✅ s48-core-profile (C-6) |
| 0-7 | TaskExecutor (Sync/ThreadPool/Concurrent) | ✅ s49-core-taskexecutor (C-7) |
| 3-1 | SpEL (Parser→Tokenizer→AST→求值) | ✅ s50-spel (C-8) |
| 2-D | ApplicationRunner (启动回调) | ✅ s51-apprunner (C-9) |
| 2-E | ClassPathIndex (类路径索引) | ✅ s52-classpathindex (C-10) |
| 6-2 | DataSource (DriverManager/Hikari/池化) | ✅ s53-jdbc-datasource (C-11) |
| 7-5 | 拦截器 (HandlerInterceptor 三方法+顺序) | ✅ s54-web-interceptor (C-12) |
| 7-6 | 异常处理 (@ControllerAdvice→HandlerExceptionResolver) | ✅ s55-web-exception (C-13) |
| 7-A | @InitBinder (WebDataBinder/@DateTimeFormat) | ✅ s56-web-initbinder (C-14) |
| 7-B | WebFlux (RouterFunction/HandlerFunction/WebClient) | ✅ s57-web-webflux (C-15) |
| 8-1 | MockMvc (perform→andExpect) | ✅ s58-test-mockmvc (C-16) |
| 8-2 | TestContext (@SpringBootTest/TestExecutionListener) | ✅ s59-test-context (C-17) |
| 8-A | @MockBean (Mockito 替换 Bean) | ✅ s60-test-mockbean (C-18) |
| 8-B | @Sql (测试数据初始化) | ✅ s61-test-sql (C-19) |
| 9-A | WebSocket (@EnableWebSocket/HandshakeInterceptor) | ✅ s62-web-websocket (C-20) |
| 9-B | messaging (@MessageMapping/STOMP) | ✅ s63-web-messaging (C-21) |

**教训**: 每开新域必须先回查原始执行计划确认域在主线; HANDOFF 的 W 列表只是笔记, 主线权威是 issue/源码分析执行计划.md。
### 执行步骤 (每域)
1. 定位源码路径 → 2. 建 outlines 目录 (s{nn}-{域}, 如 s44-core-conversion) → 3. grep 验证行号 → 4. 写 KP (P1P2P3+色+为什么) → 5. 写大纲 (四要素全+结尾桥+密度35-49) → 6. questions (≥3身份≥5问) → 7. 六层深审 → 8. 修复 → 9. 更新本 HANDOFF

### 后续计划
- ✅ 全部 21 个计划内缺域补齐 (C-1~C-21): core 7 + SpEL + context 2 + jdbc 1 + web 4 + test 4 + WebSocket/messaging 2
- ✅ C-22 Bean Validation 补入 (被低估域的深化, 替代计划 2-A 过薄覆盖)
- **下一步 Stage 7: Spring Boot 自动装配 (26域)** — 按 **`spring/BOOT-PLAN-v2.md` (知识网络化重构)** 推进 — 参考 `issue/源码分析执行计划.md`
- **📌 待办: Obsidian 知识图谱** — 把全部域大纲 (spring 65+ / tomcat / netty) 转成 Obsidian 双链 vault, 图谱视图可视化知识网络 (方法论 06 §6 已记录产出格式: 前置/复用/引出双链 + 分层标签着色) — 建议 Stage 7 收尾或作为独立任务执行

### S-1~S-4 深度 REVIEW 记录 (2026-08-11)

**REVIEW 结果**: 4 域结构全达标 (KP 6=6=6 / 五项全等 / 密度45-48 / 桥 / questions / 跨层一致)。
**REVIEW 修复 2 处**:
1. **S-2 exclude 行号缺失**: getExclusions(L144)/checkExcludedClasses+removeAll(L145-146) 补入大纲
2. **[违规] S-4 正文引用未分析域 (06 §2 禁止)**: S-16/S-17/S-8 引用改为描述现象本身或改用已分析的 t7 — 正文 forward reference 已清除
**REVIEW 验证通过**:
- "156 个候选" = imports 文件 156 行 ✓; S-3 L53 线程化 ✓; Group.process(L456) 内容准确 ✓
- S-4 复用≠省略: refresh(s8)/callRunners(C-9) 只标注衔接, 环境准备/容器创建增量充分展开 ✓
- 跨域引用: s67 的 s8 是已分析域无违规; 4 域外链全量回归无 WARN ✓

### S-5~S-6 深度 REVIEW 记录 (2026-08-11)

**REVIEW 结果**: 结构全达标 (KP 6=6=6 / 五项全等 / 密度46-47 / 桥 / questions / 跨层一致)。
**REVIEW 修复 2 处**:
1. **S-5 校验机制精确化**: 校验实际经 ValidationBindHandler(ConfigurationPropertiesBinder L97-101 绑定链)绑定中执行 — 补入大纲
2. **[违规] S-6 正文引用未分析域 (06 §2 禁止)**: S-7/S-8/S-9 域引用 (L25-27/L32) 改为只留自动装配类名(WebMvcAutoConfiguration 等), 不再标注未来域 — 仅保留结尾桥合法引出 S-7
**REVIEW 验证通过**:
- S-5 复用≠省略: C-2/C-3 机制复用正确, Binder 增量(bind/bindOrCreate/ConfigurationPropertyName/ValidationBindHandler)充分展开; 行号回归无 WARN
- S-6 防编造 (gradle 域): 大纲引用的全部依赖名(jakarta.annotation/spring-core/snakeyaml/spring-web/webmvc/jackson-databind/jackson-datatype-jdk8/jsr310/tomcat-embed-core)逐项与 build.gradle 核对真实; imports 含 Jackson/ServletWebServerFactory/WebMvc 自动装配确认
- S-6 无 .java 行号引用符合 gradle 域性质; S-5 无正文 forward reference

### S-7~S-8 深度 REVIEW 记录 (2026-08-11)

**REVIEW 结果**: 结构全达标 (KP 6=6=6 / 五项全等 / 密度45-46 / 桥 / questions / 跨层一致)。
**REVIEW 修复 1 处**:
1. **S-7 方法名精确化**: DelegatingWebMvcConfiguration 收集方法是 setConfigurers(L50)(非 getConfigurers) — 修正
2. **S-8 TomcatServletWebServerFactoryCustomizer 行号**: L89 实为 AprLifecycleListener(isAprAvailable 内), 属性应用在 customize L53 — 修正为 L38,53
**REVIEW 验证通过 (复用≠省略专项)**:
- **S-8 (示范域)**: t7 组装细节(getWebServer 全链/Starter/配置映射)10 处只引用不重复; 增量(工厂抽象/三容器条件选择/onRefresh 衔接/Jetty-Undertow 对照/切换机制)充分展开 — L35 显式标注"t7 只讲了 Tomcat, Jetty/Undertow 是本域增量"
- **S-7**: W-1~W-5 机制只标注不重复, 增量(装配核心/委托聚合/转换器装配/条件跳过)展开 — L28 显式声明"只讲 Bean 怎么被建"
- 跨域引用: S-7 的 S-8、S-8 的 S-9 均在结尾桥(合法引出下一步), 无正文 forward reference
- 两域行号回归无 WARN

### S-9 深度 REVIEW 记录 (2026-08-11)

**REVIEW 结果**: 结构全达标 (KP 6=6=6 / 五项全等 / 密度45 / 桥 / questions / 跨层一致)。
**REVIEW 修复 1 处 (防编造)**:
1. **HttpMessageConverters 默认集合来源精确化**: 原写"默认集合: ByteArray/String/Jackson 等" — 实际 L184 getDefaultConverters 取 **WebMvcConfigurationSupport.getMessageConverters + RestTemplate.getMessageConverters** 合并 — 修正
**REVIEW 验证通过**:
- Module 自动注册: JacksonAutoConfiguration L73 注释确认("auto-registration for all Module beans") + JsonComponentModule(L101) ✓
- RestClient.Builder prototype: L46 注释 + L93 @Scope(SCOPE_PROTOTYPE) ✓
- 跨域引用: S-10 仅在结尾桥(合法引出), 无正文 forward reference ✓
- 复用≠省略: W-5 机制 8 处只标注不重复, 增量(ObjectMapper 装配/转换器合并/RestClient Builder)展开 — L28 显式声明边界 ✓
- 行号回归无 WARN ✓

### S-10~S-13 深度 REVIEW 记录 (2026-08-11)

**REVIEW 结果**: 结构全达标 (KP 6=6=6 / 五项全等 / 密度44-46 / 桥 / questions / 跨层一致)。
**REVIEW 修复 2 处**:
1. **[违规] S-10 数据流正文引用 S-12 (未分析域)**: "注入 JdbcTemplate(S-12/阶段3 使用)" → 改为"事务自动配置与阶段3 使用"
2. **[编造] EmbeddedDatabaseCondition 归属错 (缺陷#7)**: 它是 DataSourceAutoConfiguration 的**静态内部类**, 非独立文件 — 原引用 "EmbeddedDatabaseCondition.java:133" 文件名不存在 → 修正为 DataSourceAutoConfiguration.java:133,142 (大纲+KP) — **内部类归属是防编造新盲区**
**REVIEW 验证通过 (复用≠省略专项)**:
- S-12 (s29-s33 复用最多): 机制只标注(L13/15/17/29/38), 增量(自动启用/管理器创建/Jdbc选择/定制器)展开 — L38/40 显式边界声明
- S-11 只讲接线边界: LettuceConnectionFactory 只到 Bean 创建层面, 协议/连接池/序列化明确标注"深入在阶段3"(6 处边界声明), 未越界
- 跨域引用: 除 S-10 一处已修, 其余 S-11 的 s29/s33、S-12 的 S-13、S-13 的 S-14 均在结尾桥(合法)
- 4 域行号回归: 除编造文件外全过; EmbeddedDatabaseConnection.get(L181) 存在 ✓

### S-14~S-15 深度 REVIEW 记录 (2026-08-11)

**REVIEW 结果**: 结构全达标 (KP 6=6=6 / 五项全等 / 密度44-45 / 桥 / questions / 跨层一致)。
**REVIEW 无修复** — 两域全部验证通过:
- 跨域引用: S-14 的 S-15、S-15 的 S-16 均在结尾桥(合法引出), 无正文 forward reference ✓
- **S-15 防编造 (内部类教训应用)**: AotProcessorHook 是 SpringApplicationAotProcessor 内部类(L103) — 引用正确指向宿主文件(上次 EmbeddedDatabaseCondition 教训已内化); L43 extends ContextAotProcessor ✓
- 复用≠省略: S-15(s21 复用 8 处: ContextAotProcessor/RuntimeHints 只标注, 增量=Boot 入口/钩子拦截/触发展开) + S-14(C-7 复用 10 处: 池机制只标注, 增量=默认执行器/Threading 双分支/builder 展开) — 均有显式边界声明
- @ConditionalOnThreading(L38, value L44) 存在 ✓; 行号回归无 WARN ✓

### C-22 深度 REVIEW 记录 (2026-08-11)

**REVIEW 修复 2 处实质错误** (防编造):
1. **@EnableMethodValidation 编造**: 该注解不存在 (缺陷#4) — 实际激活=注册 MethodValidationPostProcessor bean, Boot 的 ValidationAutoConfiguration(B-23) 自动注册 — 大纲+KP 已改, 残留清空
2. **方法名编造**: validateArguments/validateReturnValue 不存在 — 真实为 `invokeValidatorForArguments`(L166)/`invokeValidatorForReturnValue`(L178), 调真实方法用 `invocation.proceed()`(L172), 抛 ConstraintViolationException(L168/L180) — 大纲+KP 已改且行号全量 sed 验证
**REVIEW 验证通过**: determineValidationGroups(L152) 委托 validationAdapter 读 @Validated value 分组; ConstraintViolationException(L27 import/L168/L180 抛) 真实; 父类 AbstractBeanFactoryAwareAdvisingPostProcessor(L69); @RequestBody @Valid→C-14 binder validateIfApplicable(L286) 衔接一致; 结构全达标 (KP 6=6=6/五项全等/密度47/桥/questions/跨层)
- W 系列 (静态资源等) 仅在主线域全部完成后按需补充

### C-16~C-21 深度 REVIEW 记录 (2026-08-11)

**REVIEW 结果**: 6 域结构全达标 (KP 6=6=6 / 五项全等 / 密度45-47 / 桥 / questions / 跨层) + 外链全过 (C-18 用 boot 仓库) + 内联 Lxxx 抽查通过。
**REVIEW 修复 2 处实质错误**:
1. **[C-19] SqlConfig.transactionMode 语义错**: 原写"ISOLATED_TRANSACTION(独立)/INFERRED(默认)/DEFAULT(无事务)" — 实际: 枚举值是 `ISOLATED`(非 ISOLATED_TRANSACTION); `DEFAULT` 是默认且**解析为 INFERRED**(有事务管理器则现有事务内执行/随 @Transactional 回滚, 无管理器则直连数据源); INFERRED 不是独立的"默认" — 大纲+KP 已修正
2. **[C-20] doHandshake 归属**: DefaultHandshakeHandler:33 是类声明, doHandshake 实际在父类 AbstractHandshakeHandler:209 — 大纲补精确归属
**REVIEW 验证通过**: DependencyInjectionTestExecutionListener(L100/L111 注入 @Autowired via AutowireCapableBeanFactory); MergedContextConfiguration 键字段(locations L87/classes L89/profiles L93/loader L103); TestDispatcherServlet.service→super→doDispatch(W-1 衔接); MessageBrokerRegistry(enableSimpleBroker L82/setApplicationDestinationPrefixes L136/enableStompBrokerRelay L93); convertAndSend 在父类 AbstractMessageSendingTemplate:113

---

## §八 踩坑速查 (本会话全部教训)

| # | 坑 | 教训 |
|:--:|------|------|
| 1 | 行号→`}` 系统复发 | 每次 grep -n 声明行 |
| 2 | 节=流 不相等 | 五项计数 (节/流/路径/场景/关键设计) 全等 |
| 3 | KP 类型 ≠ 大纲类型 | 跨层 compare |
| 4 | HANDOFF 增量累积误差 | 每次更新后 find 计数验证 |
| 5 | 密度上限 (🟡≤49/🔴≤69) | 压缩或升级 🔴 (需 KP 同步!) |
| 6 | questions 缺第三身份 | 视角≥3 |
| 7 | 结尾桥遗漏 | tail -1 检查 |
| 8 | 批量修复引入结构破坏 (17处!) | **永远逐篇深审** |
| 9 | 内联 Lxxx 行号错 | 所有内联引用也 sed 验证 |
| 10 | 声明行 vs 调用点混淆 | 标注 (声明L100/调用L50) |
| 11 | 编造类/方法/常量 (26种谱系) | grep 全库存在性 |
| 12 | 旧版源码行号残留 | s37 doDispatch 曾引 5.x 行号 — 本仓库是 6.2.x |
| 13 | 场景与数据流矛盾 | 场景示例与数据流参数一致 |
| 14 | find 同名类误报 | 排除 reactive/test/integration |
| 15 | 单行替换不删行 | 压缩密度需真正删除段落/合并 bullet |

---

## §九 质量追踪

| 指标 | 数据 |
|:--|:--:|
| 全量深审 (2026-08-11) | 45/45 篇 + 38/38 KP 通过 |
| W-3/W-4/W-5/W-6 新增 | 各逐篇深审通过 (W-5 另有深度 REVIEW) |
| 四要素修复 | 13 篇历史遗留已补 (2026-08-11) |
| 当前全量回归 | 49 篇: 四要素/节=流=路径/密度/结尾桥/跨层 全过 |
| KP P1P2P3+色+为什么 | 40/40 全达标 |
| questions | 40/40 域 ≥3身份 |

### ⚠️ 2026-08-11 KP 格式统一记录

**发现**: 40 KP 曾有 2 种不完整格式:
- 9 个文本格式 KP (a5/j1/j2/j3/t2/t3/t4/t5/w2): `**P1 核心**` 有"为什么"但**缺 🔴🟡🟢 色标记**
- 7 个表格格式 KP (s1-s7): `| 级别 | 色 | KP | 篇 |` 有**色**但**缺"为什么"列**

**已修复**: 全部统一为 `| # | KP | 🔴🟡🟢 | 为什么 |` (表格) 或 `**P1 核心** 🔴: ... — **为什么**: ...` (文本) — 两者都要求"色+为什么"齐备。

**检查命令** (每域 KP 必跑):
```bash
# 表格格式: P条目数 = 带色数 = 带为什么数
grep -cP '^\| P[123]' knowledge-planning/{域}.md
grep -P '^\| P[123]' knowledge-planning/{域}.md | grep -cP '🔴|🟡|🟢'
grep -P '^\| P[123]' knowledge-planning/{域}.md | grep -c '为什么'
# 文本格式: P标题数 = 带色数 且 全文件有"为什么"
grep -cP '^\*\*P[123]' knowledge-planning/{域}.md
grep -P '^\*\*P[123]' knowledge-planning/{域}.md | grep -cP '🔴|🟡|🟢'
```

---

**给新 AI 的第一句话**: 从 §七 W-5 开始。每个域跑完全管线后再开下一个 — 不要批量写/批量修 (批量曾毁掉 17 处结构)。行号验证是必须的 — 不是可选的。写完每篇立即跑 §三 的检查命令。
