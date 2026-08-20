# Spring 框架源码分析 — 详细交接文档 (v2)

> ⚠️ **已过时 — 请读 HANDOFF-SPRING-v3.md (2026-08-11)**: v3 包含最新进度 (40域/47篇)、26 种缺陷谱系、W-5 已探路的源码信息、当前上下文全部要点。
> v2 保留仅作历史参考 (方法论/格式规范与 v3 相同)。
>
> **日期**: 2026-08-10
> **给新 AI**: 这个文档是你继续 Spring 源码分析的唯一入口。不要跳过任何节。
> **你的任务**: 按照 §一 方法论，从 §七 的下一步开始，逐域推进 v5 全管线(KP → 大纲 → completeness-questions → 深审)。

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
| **Stage 6** | **Spring** | **spring-web** | **4/?** | **4** | 🔄 **进行中** |
| **合计** | | | **38** | **100篇** | **45.2%** |

### 🔬 2026-08-11 全量六层深审完成 (重要!)

> **45/45 篇文章 + 38/38 KP 已全部逐篇通过六层深审** (非批量)。详细报告见 `AUDIT-2026-08-11.md`。
> 本轮发现并修复: ~186 处缺陷 (行号错位/编造/归属错/结构破坏/密度不足/KP 缺 P1P2P3/跨层不一致)。
> **教训: 批量修复引入结构破坏 (17处节=流不匹配) — 以后必须逐篇深审, 禁止批量。**

### spring-web 当前进度 (你从这里开始)

| W | 主题 | 篇 | 状态 |
|:--|------|:--:|:--:|
| W-1 | DispatcherServlet doDispatch | 1 | ✅ (已深审) |
| W-2 | @RequestMapping → HandlerMethod | 1 | ✅ (已深审) |
| W-3 | RequestMappingHandlerAdapter | 1 | ✅ (已深审) |
| W-4 | HandlerMethodArgumentResolver (@PathVariable/@RequestParam/@RequestBody) | 1 | ✅ (已深审) |
| W-5 | HttpMessageConverter (JSON/XML 序列化) | — | ⏳ next |
| W-6~W-8 | @Validated/ViewResolver/静态资源/... | — | ⏳ |

### ⚠️ 域对齐说明

原始执行计划在 `issue/源码分析执行计划.md`。本会话添加了4个补充域(S2-4 MessageSource/S2-6 BFPP/S2-7 Scope/S2-8 AppContext) — 它们不在原 spring-context 11🔴+5🟡 计划中，但从技术角度是合理补充。S2-9起对齐原计划。参考原执行计划继续推进。

---

## §一 方法论 — 完整内联

### 1.1 知识规划管线（每域必走）

```
01 逐源提取(Agent 并行) → 01 聚合(P1≥5/P2 2-4/P3 1) → 02 深度分类(🔴🟡🟢 per KP, 必须有"为什么")
  → 03 聚类(教学顺序+每个决定的原因) → v5 大纲(AI 自写, 对照源码 grep 验证行号)
  → completeness-questions(≥3 身份, ≥5 问) → 六层深审 → 修复 → HANDOFF 更新
```

**禁止跳过任何步骤**。Agent 只做逐源提取表 — 大纲由 AI 对照源码自写(T-1~T-7 119+锚点 0%错误 vs Agent 写 Ch12 18%错误)。

### 1.2 v5 大纲格式 — 每节四要素

```
### N. 机制名 — 一句话描述

场景: [真实场景, 读者知道"为什么要看这个"]

源码路径: FileName.java:行号 + 函数名。精确行号(grep 验证, 非凭记忆)

关键设计: [为什么这样实现] — 必须有"为什么"。标注 [模式: XXX]。

数据流: [代码级 trace, 完整方法调用链]
```

**严禁**: 裸行号 `(line 233)` / 伪行号 / 文件总行数代行号 / 代码拼接 / 技术清单式 bullet

### 1.3 密度标准

| 级别 | 行数/篇 | 机制/篇 |
|:--:|:--:|:--:|
| 🔴 Deep | 39-69 | 3-6 |
| 🟡 Working | 35-49 | 2-4 |

### 1.4 六层深审（每域宣称完成前必执行）

| 层 | 检查项 | 检测方法 |
|:--:|------|------|
| 1 | 源码行号 | `sed -n '行号p' File.java` — 验证非空/非 `}` |
| 2 | 内容密度 | `wc -l` 每篇 — 低于阈值→补全 |
| 3 | 语义 | 逐行 Read — 前后矛盾 / 编辑残留 |
| 4 | 技术声明 | grep 源码确认归属类 |
| 5 | 算法正确性 | 逐步骤对照源码顺序 |
| 6 | 机制归属 | 方法/字段的确切类(不凭继承推断) |

### 1.5 全维度检查 — 四层必须全部覆盖

| 维度 | 检查项 | 检测 |
|:--|------|:--|
| KP层 | P1P2P3 + 🔴🟡🟢 per KP + 为什么 | `grep -cP '^\| P[123]-\d'` ≥ 1 |
| 大纲层 | 每节四要素全 + 结尾桥 | `节数` = `数据流数` = `源码路径数` |
| 管线层 | completeness-questions ≥ 3身份 ≥ 5问 | `grep -c '视角'` ≥ 3 |
| 跨层 | KP 域类型 = 大纲 subtitle 域类型 | 🔴/🟡 一致 |

### 1.6 行号防御 — 注解/接口文件不要用 `wc -l`!

**17次复发模式**: AI 对小文件(注解/接口)习惯用 `wc -l` 总行数作为行号引用 → 指向 `}`。**每处行号引用前 grep 实际声明行号**:
```bash
grep -n 'public.*\\(class\\|@interface\\|interface\\|enum\\)' File.java
```
检测: `sed -n '行号p' File.java | grep -q '^[[:space:]]*}' && echo "WARN: 指向花括号!"`

---

## §二 全部产出清单

### spring-beans 层 (7/7 ✅)

| 域 | 主题 | 篇 | 深审 | 状态 |
|:--|------|:--:|:--:|:--:|
| S1-1 | BeanDefinition | 2 | 文件行号代行号修复 | ✅ |
| S1-2 | BeanFactory | 2 | 文件行号+补行号+术语 | ✅ |
| S1-3 | Bean 生命周期 | 2 | 术语+数据流+FactoryBean | ✅ |
| S1-4 | 循环依赖 | 1 | 字段名编造修复 | ✅ |
| S1-5 | DI 注入 | 2 | 算法顺序+方法名编造 | ✅ |
| S1-6 | BPP 全景 | 1 | 机制归属+方法名 | ✅ |
| S1-7 | FactoryBean | 1 | 缓存归属编造 | ✅ |

### spring-context 层 (16/16 ✅)

| S2-1 | refresh() 12步 | 1 | 4修复(KP/a§1/§3/行号) | ✅ |
| S2-2 | @Configuration 解析 | 3 | 首轮0 | ✅ |
| S2-3 | 事件机制 | 2 | 首轮0 | ✅ |
| S2-4 | MessageSource 国际化 | 1 | 首轮0 | ✅ |
| S2-5 | 父子容器 | 1 | 首轮0 | ✅ |
| S2-6 | BFPP 全景 | 1 | 首轮0 | ✅ |
| S2-7 | Bean 作用域 | 1 | 行号修正 | ✅ |
| S2-8 | AppContext 三大实现 | 1 | 首轮0 | ✅ |
| S2-9 | @Conditional | 1 | 首轮0 | ✅ |
| S2-10 | @Async | 1 | 行号修正 | ✅ |
| S2-11 | @Scheduled | 1 | 首轮0 | ✅ |
| S2-12 | @Cacheable | 1 | 4行号修正 | ✅ |
| S2-13 | @Lazy/@Primary/@DependsOn | 1 | 3行号修正 | ✅ |
| S2-14 | AOT/Native Image | 1 | 4行号修正 | ✅ |
| S2-15 | 3🟡域(元注解/@Validated/@DateTimeFormat) | 1 | 首轮0 (首次全正行号) | ✅ |
| S2-16 | @Import/@EnableXxx | 1 | 首轮0 | ✅ |

### spring-aop + spring-tx + spring-jdbc 层 (13/13 ✅)

| 域 | 主题 | 篇 | 状态 |
|:--|------|:--:|:--:|
| A-1 | 代理机制(JDK/CGLIB) | 1 | ✅ |
| A-2 | Advice 链(proceed递归) | 1 | ✅ |
| A-3 | @AspectJ 解析 | 1 | ✅ |
| A-4 | 自动代理(wrapIfNecessary) | 1 | ✅ |
| A-5 | Pointcut 表达式(9种primitive) | 1 | ✅ |
| T-1 | @Transactional 链路 | 1 | ✅ |
| T-2 | 8种失效场景 | 1 | ✅ |
| T-3 | 传播行为(7种) | 1 | ✅ |
| T-4 | DataAccessException异常翻译 | 1 | ✅ |
| T-5 | TransactionSynchronization回调 | 1 | ✅ |
| J-1 | JdbcTemplate | 1 | ✅ |
| J-2 | RowMapper/ResultSetExtractor/NamedParameterJdbcTemplate | 1 | ✅ |
| J-3 | batchUpdate/TransactionCallback | 1 | ✅ |

### spring-web 层 (4/? 🔄)

| 域 | 主题 | 篇 | 状态 |
|:--|------|:--:|:--:|
| W-1 | DispatcherServlet doDispatch | 1 | ✅ |
| W-2 | @RequestMapping → HandlerMethod | 1 | ✅ |
| W-3+ | HandlerAdapter/ArgumentResolver/ViewResolver... | — | ⏳ |

---

## §三 质量标准 — 你知道"通过"意味着什么

### 每域的质量要求

1. **KP 文件**: 四章完整(01提取 + 02-04聚合分类聚类) — P1/P2/P3 分级表 + 🔴🟡🟢 per KP + "为什么"原因
2. **大纲 每节**: 场景句 + 源码路径(File.java:行号) + 关键设计(Why) + 数据流(trace) + 结尾桥(→ 引出下一篇)
3. **completeness-questions**: ≥3 身份 ≥5 问
4. **行号验证**: 每处 `.java:数字` 对照源码 — 不能有 `}`/空行/文件不存在
5. **节=流**: 大纲的节数 = 数据流段落数 = 源码路径段落数

### 合格判断标准

```
# KP 检查
grep -cP '^\| P[123]-\d' knowledge-planning/{域}.md  # >0
grep -cP '🔴|🟡|🟢' knowledge-planning/{域}.md          # ≥2× P1P2P3 条目数

# 大纲检查
grep -c '^### ' outlines/{域}/0*.md           # 节数
grep -c '^数据流:' outlines/{域}/0*.md         # 必须=节数
grep -c '^源码路径:$' outlines/{域}/0*.md      # 必须=节数

# 行号检查
grep -oP '[A-Z][a-zA-Z]+\.java:\d+' outlines/{域}/0*.md | while read r; do
  sed -n '{行号}p' {文件} | grep -qE '^\s*(\}|$)' && echo "WARN"
done
```

---

## §四 缺陷谱系 — 17 种已知缺陷及防御

| # | 类型 | 示例 | 检测 |
|:--:|------|------|------|
| 1 | 文件总行数代行号 | `File.java:102` → L102 是 `}` | `sed -n '行号p'` 验证 |
| 2 | 零行号引用 | 全无 `.java:数字` | `grep -c '\.java:\d+'` |
| 3 | 字段名编造 | `earlyProxyReferences` → 实际 `earlyBeanReferences` | `grep` 字段名 |
| 4 | 方法名编造 | `autowireByName` → 实际 `autowireResource` | `grep` 方法名 |
| 5 | 算法顺序编造 | `@Primary→@Priority` → 实际 `@Primary→beanName→@Qualifier→@Priority` | 逐步骤 grep 源码 |
| 6 | 术语误用 | "最终一致性" → 引用语义 | 人工判断 |
| 7 | 机制归属编造 | `invokeInitMethods` 标为 BPP | grep 谁调用 |
| 8 | 缓存归属编造 | `factoryBeanObjectCache` 归错父类 | grep 字段定义类 |
| 9 | KP 缺 P1P2P3 (早期域) | S2-1 初始无 | 逐域 grep |
| 10 | 大纲缺数据流 | §2/§3 无数据流段 | `节数` vs `数据流` |
| 11 | 大纲缺源码路径 | §3 无源码路径段 | `节数` vs `源码路径` |
| 12 | 结尾桥缺失 | 文章末无 `→ 引出` | `tail -1` 检查 |
| 13 | KP 域类型 ≠ 大纲 subtitle | KP🔴 但大纲🟡 | 跨层 grep 对比 |
| 14 | 行号指向 `}` (注解文件) | 17次复发—Enable*/Cache*/Lazy/DependsOn 等 | `sed -n` + `grep -q '}'` |
| 15 | 域类型🔴→🟡不匹配密度 | 标注🔴但28行<39 | `wc -l` + §1.3阈值 |
| 16 | 跨层篇数不一致 | KP 说3篇实际2篇 | `ls outlines/{域}/*.md | wc -l` |
| 17 | 行号数组衰减至0 (文章末) | 尾篇行号引用从10→2→0 | 逐文章 grep 行号分布 |

---

## §五 完整领域域映射 — KP ↔ 大纲 ↔ questions

| 大纲目录 | KP 文件 | 主题 | 篇 | questions |
|------|------|------|:--:|:--:|
| s1-beandefinition | s1-beandefinition.md | BeanDefinition | 2 | ✅ |
| s2-beanfactory | s2-beanfactory.md | BeanFactory | 2 | ✅ |
| s3-bean-lifecycle | s3-bean-lifecycle.md | 生命周期 | 2 | ✅ |
| s4-circular-dependency | s4-circular-dependency.md | 循环依赖 | 1 | ✅ |
| s5-di-injection | s5-di-injection.md | DI注入 | 2 | ✅ |
| s6-bpp-panorama | s6-bpp-panorama.md | BPP全景 | 1 | ✅ |
| s7-factorybean | s7-factorybean.md | FactoryBean | 1 | ✅ |
| s8-refresh | s8-refresh.md | refresh() | 1 | ✅ |
| s9-configuration | s2-2-configuration.md | @Configuration | 3 | ✅ |
| s10-event | s3-event.md | 事件机制 | 2 | ✅ |
| s11-messagesource | s4-messagesource.md | 国际化 | 1 | ✅ |
| s12-parent | s5-parent.md | 父子容器 | 1 | ✅ |
| s13-bfpp | s6-bfpp.md | BFPP全景 | 1 | ✅ |
| s14-scope | s7-scope.md | Bean作用域 | 1 | ✅ |
| s15-appcontext | s8-appcontext.md | AppContext | 1 | ✅ |
| s16-conditional | s9-conditional.md | @Conditional | 1 | ✅ |
| s17-async | s10-async.md | @Async | 1 | ✅ |
| s18-scheduled | s11-scheduled.md | @Scheduled | 1 | ✅ |
| s19-cacheable | s12-cacheable.md | @Cacheable | 1 | ✅ |
| s20-lazy-primary-depends | s13-lazy-primary-depends.md | @Lazy/@Primary/@DependsOn | 1 | ✅ |
| s21-aot | s14-aot.md | AOT/Native Image | 1 | ✅ |
| s22-stereotype | s15-stereotype.md | 元注解/@Validated/@DateTimeFormat | 1 | ✅ |
| s23-import | s16-import.md | @Import/@EnableXxx | 1 | ✅ |
| s24-aop-proxy | a1-proxy.md | 代理机制 | 1 | ✅ |
| s25-advice-chain | a2-advice.md | Advice链 | 1 | ✅ |
| s26-aspectj | a3-aspectj.md | @AspectJ解析 | 1 | ✅ |
| s27-autoproxy | a4-autoproxy.md | 自动代理 | 1 | ✅ |
| s28-pointcut | a5-pointcut.md | Pointcut表达式 | 1 | ✅ |
| s29-tx-chain | t1-transactional.md | @Transactional链路 | 1 | ✅ |
| s30-tx-failures | t2-failures.md | 失效场景 | 1 | ✅ |
| s31-tx-propagation | t3-propagation.md | 传播行为 | 1 | ✅ |
| s32-tx-exception | t4-exception.md | 异常翻译 | 1 | ✅ |
| s33-tx-sync | t5-synchronization.md | TxSynchronization | 1 | ✅ |
| s34-jdbc-template | j1-jdbctemplate.md | JdbcTemplate | 1 | ✅ |
| s35-jdbc-advanced | j2-rowmapper.md | RowMapper/Extractor | 1 | ✅ |
| s36-jdbc-batch | j3-batch.md | batchUpdate | 1 | ✅ |
| s37-web-dispatcher | w1-dispatcherservlet.md | DispatcherServlet | 1 | ✅ |
| s38-web-requestmapping | w2-requestmapping.md | @RequestMapping | 1 | ✅ |
| s39-web-adapter | w3-requestmappinghandleradapter.md | RequestMappingHandlerAdapter | 1 | ✅ |
| s40-web-resolver | w4-argumentresolver.md | HandlerMethodArgumentResolver | 1 | ✅ |

---

## §六 文件路径

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/
├── issue/
│   └── 源码分析执行计划.md           ← 原始84域执行计划(11🔴+5🟡 spring-context 等)
├── netty/                            ← Stage 1 ✅
├── tomcat/                           ← Stage 1 ✅
└── spring/
    ├── HANDOFF-SPRING.md             ← 旧版交接文档(已过期—部分数据未更新)
    ├── HANDOFF-SPRING-v2.md          ← ← 你正在读的文档 ← ←
    ├── knowledge-planning/           ← 38个KP文件(每域四章)
    └── outlines/                     ← 38个目录/100篇v5大纲
        ├── s1-beandefinition/ ~      ← spring-beans 7域
        ├── s39-web-adapter/          ← spring-web 进行中 (W-3 已完)

Spring Framework 源码:
/data/workspace/source-code/code/spring/spring-framework/
├── spring-beans/     — BeanDefinition/BeanFactory/Scope
├── spring-context/   — ApplicationContext/@Configuration/Event/...
├── spring-aop/       — AopProxy/CglibAopProxy/Advisor/AspectJ
├── spring-tx/        — TransactionInterceptor/PlatformTransactionManager
├── spring-jdbc/      — JdbcTemplate/RowMapper
└── spring-webmvc/    — DispatcherServlet/HandlerMapping/HandlerAdapter
```

---

## §七 下一步行动

### 当前任务: spring-web 层
- ~~**W-3**: RequestMappingHandlerAdapter — HandlerMethod 如何被反射调用~~ ✅ 已完成 (s39-web-adapter)
- ~~**W-4**: HandlerMethodArgumentResolver — @PathVariable/@RequestParam/@RequestBody 参数绑定~~ ✅ 已完成 (s40-web-resolver)
- **W-5**: HttpMessageConverter — JSON/XML 序列化
- **W-6~**: ViewResolver/静态资源/异常处理...

### 执行步骤 (每域)
1. 定位源码路径 (源码在 spring-webmvc/)
2. 创建 outlines 目录 (下一编号 s39-web-*)
3. 验证关键方法行号 (`grep -n`)
4. 写 KP (聚合 P1P2P3 + 🔴🟡🟢 + 为什么)
5. 写大纲 (四要素全 + 结尾桥)
6. 写 completeness-questions (≥3身份 ≥5问)
7. 深审: 行号验证 + 四要素 grep + 节数=数据流数
8. 修复所有缺陷
9. 更新本交接文档

### 总体计划
- spring-web 剩余域 → spring-webmvc 完成
- Stage 7: Spring Boot 自动装配(24域)
- Stage 8-10: 剩余框架

---

## §八 踩坑速查 — 本会话教训

| # | 坑 | 教训 |
|:--:|------|------|
| 1 | **行号→`}` 系统复发** | 17次—注解/接口小文件—KP写作前务必 `grep -n 'public.*(class\|@interface)' File.java` |
| 2 | **节=流 不相等** | 每次写大纲检查: `节数` 必须 = `数据流段落数` = `源码路径段落数` |
| 3 | **KP 域类型 ≠ 大纲 subtitle** | 跨层 compare: `grep -oP '🔴 Deep\|🟡 Working'` KP vs 大纲 |
| 4 | **HANDOFF 增量累积误差** | 每次更新后 `find outlines -name '0*.md' | wc -l` 验证总篇数 |
| 5 | **大纲类型标记过度** | 如果28行<39→不要标🔴 Deep→标🟡 Working |
| 6 | **completeness-questions 缺第三身份** | `grep -c '视角'` 必须 ≥ 3 |
| 7 | **结尾桥遗漏** | `tail -1 outlines/{域}/*.md` 检查是否有 `→ 引出` |
| 8 | **Agent 提取 vs AI 自写** | Agent 只做逐源提取表—AI 对照源码自写大纲(grep 验证行号)—Agent 写大纲=18%行号错误 |

---

## §九 质量追踪

| 指标 | 数据 |
|:--|:--:|
| 方法论固化后 | S2-2 → W-2 = **连续30域零缺陷** |
| 行号→`}`复发 | 17次后打破(S2-15起全正) |
| 大纲四要素 | **22/22篇节=流** (spring-context through spring-jdbc) |
| 跨层类型一致 | 5/5层 type 一致 |
| KP P1P2P3 | 38/38 KP 全部包含 |
| Questions | 38/38 域≥3身份 |
| **2026-08-11 全量深审** | **45/45篇六层通过 + 38/38 KP 全达标** (详见 AUDIT-2026-08-11.md) |
| **2026-08-11 W-3** | **s39-web-adapter 新增篇: 逐篇六层深审通过 (49行/节=流=路径/行号全验)** |
| **2026-08-11 W-4** | **s40-web-resolver 新增篇: 逐篇六层深审通过 (49行/节=流=路径/行号全验)** |

---

**给新 AI 的第一句话**: 从 §七 的 W-3 开始。阅读 `issue/源码分析执行计划.md` 了解全局84域计划。每个域跑完全管线后再开下一个 — 不要批量写。行号验证是必须的 — 不是可选的。**2026-08-11 已对全部 45 篇执行逐篇六层深审并修复 — 任何新域完成后同样逐篇深审, 禁止批量修复 (批量曾引入17处结构破坏)。**
