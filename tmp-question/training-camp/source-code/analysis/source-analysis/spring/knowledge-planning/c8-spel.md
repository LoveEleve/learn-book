# C-8 SpEL — 表达式引擎 (词法 → 语法 AST → 求值/编译)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | ExpressionParser(59行)+SpelExpressionParser(66行)+InternalSpelExpressionParser(1075行)+Tokenizer(589行)+SpelExpression(590行)+StandardEvaluationContext(604行)+SpelCompiler(327行)+Expression(278行)
> 基线: C-7 结尾桥 — @Scheduled/@Cacheable 的 #{...} 表达式 — 本域展开表达式引擎三阶段: 解析→AST→求值; 原始执行计划 3-1

---

## §0.8

- 🟡 Working，1篇 — 入口(ExpressionParser.parseExpression → SpelExpressionParser → InternalSpelExpressionParser) → 词法(Tokenizer.process: 字符串→Token 流) → 语法(eatExpression 递归下降→SpelNodeImpl AST→SpelExpression) → 求值(getValue: 解释执行 ast.getValue + checkCompile 编译升级) → 编译(SpelCompiler: 字节码, MIXED/immediate 模式) → 上下文(StandardEvaluationContext: rootObject/变量/BeanResolver)
- 设计模式: [模式: 解释器]—AST 节点树求值; [模式: 递归下降]—eatExpression 语法分析; [模式: JIT]—热表达式编译为字节码

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ExpressionParser.java:28,42 | 接口 | **解析入口**: parseExpression(String) → Expression 对象 — 一次解析多次求值(Expression 线程安全) | High |
| SpelExpressionParser.java:34,62 | 委托 | **解析器**: doParseExpression → InternalSpelExpressionParser.doParseExpression | High |
| InternalSpelExpressionParser.java:128 | 三阶段编排 | **doParseExpression**: L135-136 Tokenizer 创建+process(词法)→tokenStream → L140 eatExpression(语法, 递归下降→AST) → L145 new SpelExpression(expressionString, ast, configuration) | High |
| Tokenizer.java:37,88 | 词法 | **Token 流**: process() — 读标识符/数字/运算符/字符串字面量 → Token 列表(带位置) — 词法错误在此抛 SpelParseException | High |
| SpelExpression.java:50,121 | 求值 | **getValue()**: L122-131 编译版(compiledAst)优先, MIXED 模式失败→回退解释; L142 解释: new ExpressionState(context, config) → ast.getValue(expressionState) → L143 checkCompile(热表达式升级编译) | High |
| StandardEvaluationContext.java:80,138 | 求值上下文 | **EvaluationContext**: setRootObject(根对象, 属性访问起点) / setVariable(变量 #x) / setBeanResolver(@bean 引用) | High |
| SpelCompiler.java:102,271 | 编译 | **JIT**: compile(SpelNodeImpl)→字节码类; getCompiler(ClassLoader); 触发: SpelCompilerMode.IMMEDIATE(立即)/MIXED(解释 N 次后编译) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 解析器+AST+求值器+编译器约 3600 行 — 但知识主线清晰: "字符串→Token→AST→求值→(可选)编译". 1篇 (~46行) 按"入口→解析→求值→上下文"展开; 若分 2 篇则 AST 构建与求值割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 三阶段解析 (Tokenizer 词法→eatExpression 递归下降→SpelExpression AST) | 🔴 | **为什么🔴**: 表达式引擎的核心 — "字符串→可执行结构"的完整编译原理缩影 |
| P1-2 | SpelExpression.getValue (编译优先→MIXED 回退→解释执行→checkCompile) | 🔴 | **为什么🔴**: 求值与性能升级策略 — "解释→热路径编译"是表达式引擎的经典设计 |
| P1-3 | StandardEvaluationContext (rootObject/变量/BeanResolver) | 🔴 | **为什么🔴**: "表达式在什么环境里求值" — @Cacheable key 引用参数(#)、@EventListener 条件、@Value @bean 引用全在这 |
| P2-1 | SpelCompiler 字节码编译 (IMMEDIATE/MIXED 模式) | 🟡 | **为什么🟡**: 性能进阶 — 什么时候编译、失败怎么回退 |
| P2-2 | ExpressionParser/Expression 接口 (一次解析多次求值) | 🟡 | **为什么🟡**: 使用侧抽象 — 线程安全与缓存语义 |
| P3-1 | 模板表达式 (#{...} 与文本混合, TemplateAwareExpressionParser) | 🟢 | **为什么🟢**: @Value("#{a}#{b}") 的混合语法基础 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **解析三阶段** (入口→Tokenizer→eatExpression→AST) | 🔴 | 字符串到结构 — 编译原理核心 |
| B | **求值与编译** (getValue 双路径 + SpelCompiler) | 🔴 | 执行与性能 — 解释/JIT 策略 |
| C | **上下文与使用** (EvaluationContext + @Value/@Cacheable 场景) | 🔴 | "在哪求值"与框架接入点 |

> **Cluster A (§1)**: parseExpression 入口 + Tokenizer 词法 + eatExpression 语法(AST)
> **Cluster B (§2)**: SpelExpression.getValue(编译/解释双路径) + checkCompile + SpelCompiler(IMMEDIATE/MIXED)
> **Cluster C (§3)**: StandardEvaluationContext(rootObject/变量/@bean) + 框架使用(@Value/#{...}/@Cacheable key/@EventListener condition)

→ 引出 2-D: ApplicationRunner — SpEL 求值需要 Bean 上下文(BeanResolver), 容器启动回调让 Bean 就绪后执行 — 启动生命周期的最后一个钩子

(End of file - total 61 lines)
