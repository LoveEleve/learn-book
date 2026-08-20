# C-8 SpEL — 表达式引擎 (词法 → 语法 AST → 求值/编译)

> 依赖 C-7 TaskExecutor | 🟡 Working | 6 KP | [模式: 解释器 + 递归下降 + JIT]

**读者处境**: @Cacheable(key="#user.id")、@EventListener(condition="#event.success")、@Value("#{config.url}") — 这些字符串表达式怎么变成值？"#user.id" 里的 # 是什么？为什么表达式要"解析"和"求值"两步？

### 1. 解析三阶段 — Tokenizer 词法 → eatExpression 语法 → SpelExpression

场景: 表达式字符串 "#user.id + 1" — 引擎要把它变成可执行的 AST: ①切成 Token(标识符/运算符/数字) ②按文法组装成节点树 ③包装成 Expression 对象。

源码路径:
- `ExpressionParser.java:28,42` — **入口**: parseExpression(String) → Expression — 一次解析、多次求值(Expression 可并发 getValue)
- `SpelExpressionParser.java:34,62` — **委托**: doParseExpression → InternalSpelExpressionParser.doParseExpression
- `InternalSpelExpressionParser.java:128` — **三阶段编排**: L135-136 Tokenizer 创建+process() → tokenStream → L140 eatExpression()(递归下降语法分析, 产出 SpelNodeImpl AST) → L145 new SpelExpression(expressionString, ast, configuration)
- `Tokenizer.java:37,88` — **词法**: process() — 逐个字符读标识符/数字/运算符/字符串 → Token 列表(带 startPos) — 非法字符在此抛 SpelParseException

关键设计: **Why 分"词法+语法"两阶段？** 词法只关心"字符→Token"(无上下文), 语法关心"Token 序列→树结构" — 分离让文法清晰(运算符优先级/嵌套在 eatExpression 处理, 字符识别在 Tokenizer); 递归下降(eatExpression→eatLogicalOr→eatAnd→…)逐层处理优先级。[模式: 递归下降解析]

数据流: `parser.parseExpression("#user.id + 1")` → Tokenizer.process: [#, user, ., id, +, 1] → eatExpression: 二元运算节点(+, 左=属性访问(变量user, 属性id), 右=常量1) → SpelExpression("...", ast) → 返回。

### 2. SpelExpression.getValue — 解释执行 + 热路径编译

场景: getValue() 每次调用怎么算出结果？为什么框架里表达式第一次慢、后面快？

源码路径:
- `SpelExpression.java:50,121` — **getValue()**: L122-131 compiledAst(编译产物)存在→直接执行字节码; 编译执行抛异常→MIXED 模式清空 compiledAst 回退解释 / IMMEDIATE 模式抛 SpelEvaluationException; L142 解释: new ExpressionState(context, configuration) → ast.getValue(expressionState)(AST 节点递归求值) → L143 checkCompile(统计调用次数, 超阈值触发编译)
- `SpelCompiler.java:102,271` — **JIT 编译**: compile(SpelNodeImpl)→生成字节码类(ClassLoader 加载); SpelCompilerMode: IMMEDIATE(SpelExpression 创建即编译)/MIXED(解释若干次后编译)

关键设计: **Why 要编译而非一直解释？** 表达式在 @Cacheable key/安全校验里高频执行 — AST 解释有节点分派开销; 编译成字节码消除解释开销(类似 JIT)。**Why MIXED 模式失败要回退？** 不是所有表达式可编译(动态类加载等) — 回退解释保证功能正确, 编译只是优化。[模式: 解释器 + 两级执行]

数据流: 第一次 getValue → compiledAst=null → ExpressionState → ast.getValue(节点树递归: 变量节点取 root 的 #user → 属性节点反射 getter id → 加法节点) → checkCompile: 次数达到阈值且 MIXED → SpelCompiler.compile(ast) → 后续 getValue 直接跑字节码。

### 3. StandardEvaluationContext — 表达式在哪求值

场景: "#user.id" 里的 user 从哪来？"@config.url" 的 @ 怎么解析到 Bean？表达式能访问什么、不能访问什么由"上下文"决定。

源码路径:
- `StandardEvaluationContext.java:80,138` — **上下文**: setRootObject(root, typeDescriptor)(根对象 — 未加 # 的属性从根取) / setVariable(name, value)(变量 — `#user` 是变量) / setBeanResolver(实现 `@beanName` 引用)
- 框架接入: `@Value("#{config.url}")` — bean 引用通过 BeanExpressionContext/BeanFactoryResolver; `@Cacheable(key="#user.id")` — key 表达式在 CacheOperationExpressionEvaluator 用 MethodBasedEvaluationContext(方法参数注册为变量)求值; `@EventListener(condition=...)` — 事件对象注册为变量
- `TemplateAwareExpressionParser`(基类) — 支持 `#{...}` 与文本混合(模板模式)

关键设计: **Why 求值需要显式上下文？** 表达式求值必须限定"作用域" — 安全(不暴露容器全部)、可测(注入假数据)、隔离(不同调用点不同变量)。框架各处用子类上下文注入各自变量: Cache 的 #参数、Event 的 #事件对象 — 统一接口, 场景化上下文。[模式: 上下文对象]

数据流: @Cacheable(key="#user.id") → CacheOperationExpressionEvaluator → MethodBasedEvaluationContext: rootObject=方法目标, 变量 user=方法参数 → parser.parseExpression("#user.id") → getValue(context) → 变量节点查 context.getVariable("user") → 属性 id → key="1001" → 缓存命中判定。@Value("#{config.url}") → BeanFactoryResolver → 容器查 config bean → url 属性。

→ 引出 2-D: ApplicationRunner — 表达式/容器都就绪后, 启动回调(@EventListener(ApplicationReadyEvent) 同族) — 应用启动生命周期的最后一步。
