# 域 AR-2: Watch/Trace 与字节码增强 — 知识规划

> 源码路径: core/command/(BuiltinCommandPack+AnnotatedCommandImpl+Constants) + core/shell/(ShellLineHandler/JobControllerImpl/ProcessImpl/InternalCommandManager/command/impl + internal handlers 管道) + core/advisor/(Enhancer/SpyInterceptors/SpyImpl/AdviceListenerManager/AdviceListenerAdapter/Advice/ArthasMethod/AccessPoint/InvokeTraceable/AdviceWeaver/LineEnhanceOptions) + core/command/monitor200/(EnhancerCommand/Watch/Trace/Stack/Monitor/TimeTunnel/Line + 各 Listener) + core/command/model/(TraceTree/TraceNode/MethodNode/ThrowNode) + core/util/(SearchUtils+matcher 体系/StringUtils/ThreadLocalWatch/affect/EnhancerAffect) + arthas-model/(ObjectVO) + ByteKit 外部源码(source-code/code/spring/bytekit/src/: MethodProcessor/ClassLoaderAwareClassWriter/InliningAdapter/location/拦截器解析)
> 源码量: core ~35 文件 + ByteKit 130 文件(对照用)
> 提取日期: 2026-08-10(v2 深审补充: 机制 27→37)
> 前置域: AR-0(命令使用)/AR-1(SpyAPI 注入)

## 01 逐源提取

### 支撑小节 2a — 命令体系

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| BuiltinCommandPack.java:48-129 | **命令注册**: initCommands() 收集 47 无条件 + 2 条件(JFR/ClassLoaderMetaspace,JDK 含 JFR 时 :111-118)共 49 个命令类 → 读 `@Name` 注解过滤 disabledCommands → `Command.create(clazz)` | High |
| 命令类 @Name/@Summary(见 AR-0 KP) | **注解式命令声明**: 每个命令 = `@Name/@Summary/@Description/@Argument/@Option` 注解类,非继承多态注册 | High |
| ShellLineHandler.java:29-66 | **命令行分词**: `CliTokens.tokenize(line)`(:36)→ 首 token 特判 shell 内建(exit/logout/jobs/fg/bg/kill)→ 其余 `createJob` | High |
| JobControllerImpl.java:80-229 | **Job 模型**: createJob(:80)→ createProcess(:146,首 token 查 `commandManager.getCommand(name)` 未命中抛 "command not found")→ 管道段切分 → ProcessImpl | High |
| InternalCommandManager.java:36-47,123-135 | **命令查找**: 遍历 resolvers(**跳过 ShellInternalCommandResolver** :38),按 `command.name()` 匹配 | High |
| ProcessImpl.java:315-372 | **参数解析与执行**: `cli().parse(args2)`(:357,false 版本先查 --help :351)→ CommandProcessTask 异步执行(:370-371)→ `handler.handle(process)` | High |
| AnnotatedCommandImpl.java:73-86 | **注解注入**: `clazz.newInstance()` + `CLIConfigurator.inject(process.commandLine(), instance)`(:81)把解析结果注入 `@Option/@Argument` 字段 → `instance.process(process)` | High |
| InternalCommandManager.java:56/137 findLastPipe | **管道切分**: 按 `|` 分段,每段独立命令;输出链 stdoutHandlerChain(GrepHandler/WordCountHandler/TeeHandler)→ TermHandler | High |
| ExpressFactory.java:26-34 | **OGNL 工厂**: threadLocalExpress——ThreadLocal 弱引用复用 OgnlExpress 防 ClassLoader 泄漏 | High(AR-5 详述) |

### 支撑小节 2b — EnhancerCommand 模板

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| EnhancerCommand.java:39-292 | **抽象命令模板**: watch/trace/stack/monitor/tt/line/groovy 7 子类;`enhance()`(:193-292): `new Enhancer(listener, listener instanceof InvokeTraceable, skipJDKTrace, ...)`(:218)→ `enhancer.enhance(inst)`(:223);isTracing=true → 织入 invoke 追踪桩;getAdviceListener 由子类实现 | High |

### 主体 — 字节码增强引擎

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| Enhancer.java:639-705 enhance() | **增强发起**: `SearchUtils.searchClass/searchSubClass` 类名匹配 → `filter()`(:546,classloader/arthas 自身/unsafe/lambda 过滤)→ `TransformerManager.addTransformer(this)` → `inst.retransformClasses`(批量 or 逐个) | High |
| Enhancer.java:149-369 transform() | **JVM 回调织入**: `loadClass(SpyAPI)` 预检(:154)→ `AsmUtils.toClassNode`(:197,removeJSR)→ `DefaultInterceptorClassParser.parse(SpyInterceptor1/2/3.class)`(:207-209)→ 按 isTracing/skipJDKTrace 选 trace 拦截器(:220-230)→ GroupLocationFilter 防重复增强(:253-278)→ 逐方法 `MethodProcessor.process()`(:313-332)→ `AdviceListenerManager.registerAdviceListener`(:335-336)→ 类版本<49 提升(:346)→ `AsmUtils.toBytes(classNode, inClassLoader, classReader)`(:351,复用 ClassReader 常量池防 metaspace OOM)→ classBytesCache(reset 用) | High |
| Enhancer.java:95-99 静态块 | **SpyImpl 安装**: `SpyAPI.setSpy(new SpyImpl())`——Spy 实例安装唯一入口 | High |
| SpyInterceptors.java:20-125 | **10 个拦截器**: SpyInterceptor1(@AtEnter)/2(@AtExit)/3(@AtExceptionExit)/SpyLineInterceptor(@AtLine,lines={-1})/SpyTraceInterceptor1-3(@AtInvoke whenComplete=false/true + @AtInvokeException,excludes SpyAPI+8 box 类型)/SpyTraceExcludeJDKInterceptor1-3(excludes="java.**")——全部 inline=true | High |
| ByteKit DefaultInterceptorClassParser | **注解→字节码模板**: parse(Class) 解析 @AtXxx 注解 → InterceptorProcessor 列表;@Binding.This/@Args/@Return/@Throwable/@Line/@LocalVars 等绑定上下文 | High |
| ByteKit MethodProcessor/InliningAdapter | **织入执行**: MethodProcessor.process() 在方法内定位插入点(Location);InliningAdapter 将拦截器方法体**内联**到目标方法——生成直接静态调用 | High |
| ByteKit InstrumentTransformer | **transformer 包装**: InstrumentParseResult → ClassFileTransformer(AR-1 enhanceClassLoader 也用) | Medium |
| ClassWriter 常量池复用 | **防 metaspace OOM**: 传原始 ClassReader 复用常量池(#196 注释) | Medium |

### 主体 — 回调分发链与数据模型

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| SpyAPI.java:24-87 | **静态转发层**: volatile AbstractSpy + NOPSPY 兜底;7 个静态方法(atEnter/atExit/atExceptionExit/atBeforeInvoke/atAfterInvoke/atInvokeException/atLine)转发给 spyInstance | High |
| SpyImpl.java:28-202 | **分发实现**: `splitMethodInfo` 拆 `方法名\|描述`(:31)→ `AdviceListenerManager.queryAdviceListeners(classLoader, className, methodName, methodDesc)`(:35)→ skipAdviceListener 过滤已终止进程(:40,实现 :204-217)→ `listener.before(...)`(:43);atBeforeInvoke 用 `splitInvokeInfo` 拆 `owner\|方法\|描述\|行号`(:101-124)→ InvokeTraceable | High |
| AdviceListenerManager.java:101-193 | **分发索引**: `ConcurrentWeakKeyHashMap<ClassLoader, ClassLoaderAdviceListenerManager>`;key=类名+方法名+描述(trace 加 owner,line 加行号 :110-116);注册在 Enhancer.transform 期间(:335-336) | High |
| AdviceListenerAdapter.java:40-158 | **Spy→Listener 适配**: final 包装方法把无 loader 回调转成 `(loader, clazz, new ArthasMethod(...), target, args)`(:51);`isConditionMet(express, advice, cost)`(:132-135,OGNL 条件)+ `getExpressionResult`(:137-139);isLimitExceeded/abortProcess(:148/158)实现 -n 限次 | High |
| Advice.java:12-230 | **调用现场模型**: loader/clazz/method/target/params/returnObj/throwExp/lineNumber + 场景标志(AccessPoint 位: BEFORE(1)/AFTER_RETURNING(2)/AFTER_THROWING(4)/LINE(8),:147-150);工厂 newForBefore/AfterReturning/AfterThrowing/newForLine(:153/170/188/207);normalizeLocalVariables(:247-274) | High |
| ArthasMethod.java:26-164 | **反射桥**: ASM Type.getMethodType(methodDesc) 还原参数类型(:33)→ getDeclaredMethod/invoke(:155-164)——tt replay 用;懒加载避免增强代码里传 Method 对象 | High |
| ThreadLocalWatch.java:9-94 | **耗时计算**: 固定 4097 ring stack(ThreadLocal),start() push nanoTime, costInMillis() pop 求差——避免 ArthasClassLoader 泄漏 | High |

### 主体 — 各追踪命令

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| WatchCommand.java:22-228 + WatchAdviceListener.java | **watch**: `-x`(默认1)/`-n`(100)/`-b/-f/-e/-s/-M/-E`;listener: before 起计时(:41)→ afterReturning `newForAfterReturning`(:50)→ `watching`(:76): cost+isConditionMet→getExpressionResult→WatchModel→appendResult;超 -n abortProcess | High |
| TraceCommand.java:31-198 + TraceAdviceListener/AbstractTraceAdviceListener/TraceTree | **trace**: `-p` path→PathTraceAdviceListener(空子类,不插 invoke 桩);普通→TraceAdviceListener(InvokeTraceable: invokeBeforeTracing(:23)→tree.begin(isInvoking=true)/invokeAfterTracing(:30)→tree.end/invokeThrowTracing(:36)→end(true));AbstractTraceAdviceListener: before(:52)tree.begin(false)+deep++/afterReturning(:59)tree.end;**deep==0 最外层输出**(:88-101);TraceTree: findChild 按类+方法+行号合并调用点(:41),MethodNode 记 begin/endTimestamp+min/max/total/times | High |
| StackCommand.java:22-122 + StackAdviceListener | **stack**: afterReturning→`ThreadUtil.getThreadStackModel`(ThreadUtil.java:402)→`findTheSpyAPIDepth`(:381)裁剪 Spy 帧→StackModel(trace_id/rpc_id 来自 EagleEye 反射探测 :465) | High |
| TimeTunnelCommand.java:39-563 + TimeTunnelAdviceListener | **tt**: 全局 `LinkedHashMap<Integer,TimeFragment>`(:55);listener: `pushArgs` 存**512 ring stack**(:34-39,防方法内改参)→afterFinishing `new TimeFragment(advice, now, cost)`(:123);`processPlay`(:502): `ArthasMethod.invoke(target, params)`(:536)→TimeTunnelModel;`-s` OGNL 搜索(:409)/`-w` watch(:381) | High |
| View 层(WatchView/TraceView/StackView/TimeTunnelView) | **渲染**: TraceView.drawTree 树形 `---/+-/|`(:151-171),占比=子cost/父totalCost,最大耗时红高亮(:177-192);ThreadView 表格 | Medium |

| SearchUtils.java:28-67 + matcher/ 体系 | **类搜索与匹配器**: `searchClass(inst, matcher, limit)`(:28)/`searchSubClass`(:53-54,含子类)/`searchClassOnly`(:62-67);matcher 体系: `WildcardMatcher`(通配)/`RegexMatcher`(正则,-E 参数)/`GroupMatcher`(Or 组合,-p path)/`TrueMatcher`/`FalseMatcher`——匹配器模式 | High |
| InvokeTraceable.java | **追踪桩契约**: 3 方法 `invokeBeforeTracing/invokeAfterTracing/invokeThrowTracing`(classLoader+类+方法+desc+行号)——SpyImpl 强转调用,EnhancerCommand 用 instanceof 判定 isTracing | High |
| ObjectVO(arthas-model) | **对象展开载体**: `expand` 字段 + `array()` 工厂;`expandOrDefault()`(:32)——watch -x 的展开深度由它承载(注意: 在 arthas-model 模块,非 core) | High |
| EnhancerAffect.java:21-59 | **增强统计**: `cCnt/mCnt` 原子计数(:23-24)+ `overLimitMsg`(:37)——retransform 后 "Affect(class-cnt:1, method-cnt:1)" 的来源 | Medium |
| MonitorAdviceListener.java:67-190 | **monitor 统计命令**: `ConcurrentHashMap<Key, AtomicReference<MonitorData>>`(:72)按方法聚合调用次数/耗时/异常 → `MonitorTimer`(:186)定时输出;`isConditionMet`(:141)条件过滤——与 watch 同引擎不同输出 | Medium |
| LineCommandAdviceListener(atLine) | **line 行号命令**: atLine 回调(局部变量/行号)——AR-2 篇 2 的 SpyLineInterceptor 消费方 | Medium |
| StringUtils.java:970-976 | **编码协议**: `splitMethodInfo`(方法名|描述,:970)/`splitInvokeInfo`(owner|方法|描述|行号,:976)——增强织入拼串、SpyImpl 拆串,共享同一格式 | High |
| ByteKit ClassLoaderAwareClassWriter | **类型安全写出**: 重写 `getCommonSuperClass`(:34)——ASM 计算公共父类时用**目标 ClassLoader** 加载类,避免用错 CL 导致 NoClassDefFoundError | High |
| ByteKit MethodProcessor:117-125 | **插入点过滤管线**: `locationFilter = DefaultLocationFilter`(:117,可换 GroupLocationFilter);process 时逐 Location 过滤——防重复增强的挂载点 | High |
| AsmUtils(ByteKit) | **字节码工具**: toClassNode/toBytes/removeJSRInstructions——transform 的出入端口 | Medium |

*37 个知识点(v1 27 → 补充 10)*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

| KP | 出现文件 | 说明 |
|----|---------|------|
| 命令→增强→回调全链路 | EnhancerCommand, Enhancer, SpyInterceptors, SpyAPI, SpyImpl, AdviceListenerManager, AdviceListenerAdapter, 各 Listener | watch/trace/stack/monitor/tt 共用一条链路——模板方法模式的最大体现 |
| 字节码织入管线 | Enhancer, SpyInterceptors, ByteKit(DefaultInterceptorClassParser/MethodProcessor/InliningAdapter/ClassLoaderAwareClassWriter), AsmUtils | ByteKit 全流程——类型安全写出是织入质量的保证 |
| 类搜索与匹配 | SearchUtils, matcher/(Wildcard/Regex/Group/True), Enhancer, EnhancerCommand | 类名匹配→方法名匹配→增强,匹配器模式贯穿 |
| 追踪桩契约与消费 | InvokeTraceable, SpyImpl(:101-176), SpyTraceInterceptor1/2/3, TraceAdviceListener | 契约→织入→回调→树构建 全链 |
| 防重复增强 | Enhancer(:253-278), GroupLocationFilter, InvokeContainLocationFilter, AsmUtils.containsMethodInsnNode | 内联直调的特征扫描 |

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| 命令注册与查找 | BuiltinCommandPack, InternalCommandManager, JobControllerImpl |
| 注解注入 | AnnotatedCommandImpl, CLIConfigurator(外部), @Argument/@Option |
| Advice 现场模型 | Advice, AccessPoint, ArthasMethod |
| -n 限次与进程终止 | AdviceListenerAdapter, SpyImpl.skipAdviceListener |
| trace 树 | TraceAdviceListener, AbstractTraceAdviceListener, TraceTree, TraceEntity, TraceNode/MethodNode/ThrowNode |
| monitor/line 命令 | MonitorCommand, MonitorAdviceListener, LineCommand, LineCommandAdviceListener |
| 方法信息编码协议 | StringUtils.splitMethodInfo/splitInvokeInfo, SpyInterceptors(@Binding.MethodInfo/InvokeInfo), SpyImpl |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| ThreadLocalWatch ring stack | ThreadLocalWatch.java |
| tt 512 ring stack | TimeTunnelAdviceListener.java |
| 管道切分 | InternalCommandManager.findLastPipe |
| ClassWriter 常量池复用 | Enhancer.java:196 注释 + toBytes |

---

## 03 深度分类

### 🔴 Deep (教学核心——理解"watch 到底怎么工作"的每一环)

| KP | 为什么 |
|----|------|
| 注解式命令声明 + 执行链 | 面试"arthas 命令怎么实现的"必答;CLIConfigurator 注入是注解框架经典 |
| Enhancer 织入管线(ByteKit+10 拦截器) | 面试"watch 原理"核心;inline 内联 vs 反射 invoke 是关键 tradeoff |
| SpyImpl→AdviceListenerManager 分发 | 理解"增强代码怎么找到监听器"——ClassLoader 维度索引是难点 |
| Advice 场景模型 + AccessPoint 位标志 | 位运算四场景(before/return/throw/line)是设计亮点 |
| -n 限次/条件过滤的执行点 | 生产安全设计(AR-0 的操作规范在此有源码依据) |
| trace 树构建(findChild 合并/深层计数) | 面试"trace 的树怎么画出来" |
| ClassLoaderAwareClassWriter 类型安全写出 | 面试"织入时类加载器问题"——getCommonSuperClass 用目标 CL 是高级知识点 |
| 方法信息编码协议(splitMethodInfo) | 理解"织入与分发共享格式"——编码协议是增强系统的心跳 |
| SpyImpl 安装(静态块) | 为什么: core 一加载即装好,Spy 实例唯一入口 |
| 调用现场模型(Advice) | 为什么: 位标志场景语义是 watch 输出的核心 |
| 耗时计算(ThreadLocalWatch) | 为什么: ring 栈防嵌套+防泄漏 |
| 对象展开载体(ObjectVO) | 为什么: -x 深度的实现,在 arthas-model 模块 |
| 编码协议(splitMethodInfo/InvokeInfo) | 为什么: 织入与分发共享格式——增强系统的心跳 |
| watch 命令实现(watching 唯一出口) | 为什么: 四回调汇聚+条件+限次,命令实现范本 |
| 类搜索与匹配器(SearchUtils+matcher) | 为什么: 匹配器模式贯穿,命令只声明用什么匹配 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| 命令注册细节(49 命令/disabled 过滤) | AR-0 已覆盖使用;注册机制一条线 |
| tt replay(ArthasMethod 反射桥) | 概念重要,细节(ASM 描述符还原)可按需 |
| ThreadLocalWatch ring stack | 防泄漏设计值得讲,实现简单 |
| ClassWriter 常量池复用 | metaspace OOM 防护——讲解 why 即可 |
| 命令注册(49 命令) | 为什么: 47+2 条件注册 |
| 命令行分词(CliTokens) | 为什么: 词法切分+内建特判 |
| Job 模型 | 为什么: 一次回车=一个 Job,可管道 |
| 命令查找(InternalCommandManager) | 为什么: 跳过 ShellInternalCommandResolver |
| 注解注入(CLIConfigurator) | 为什么: 注解元数据驱动注入 |
| 管道切分(findLastPipe) | 为什么: 输出链 handler |
| 增强发起(enhance) | 为什么: 匹配→注册→retransform |
| 增强统计(EnhancerAffect) | 为什么: Affect(c/m-cnt) 输出来源 |
| transformer 包装(ByteKit InstrumentTransformer) | 为什么: AR-1 enhanceLoaders 同款 |
| 字节码工具(AsmUtils) | 为什么: toClassNode/toBytes 出入端口 |
| 抽象命令模板(EnhancerCommand) | 为什么: 7 子类的骨架——匹配/注册/retransform 固定 |
| 追踪桩契约(InvokeTraceable) | 为什么: 3 方法接口,isTracing 判定依据 |
| 参数解析与执行(ProcessImpl.run) | 为什么: cli().parse + 异步执行链 |
| 静态转发层(SpyAPI 7 方法) | 为什么: 薄转发+NOPSPY,增强代码的唯一依赖 |
| 插入点过滤管线(MethodProcessor.locationFilter) | 为什么: 防重复增强的挂载点 |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| 管道 handler 细节 | AR-0 已覆盖使用 |
| View 渲染细节 | 教学价值低 |
| PathTraceAdviceListener 差异 | trace -p 的边角 |
| JSR 移除/类版本提升 | ASM 兼容性细节 |

---

| OGNL 工厂(ExpressFactory) | 为什么: AR-5 详述,此处提存在 |
| monitor 统计命令 | 为什么: 与 watch 同引擎不同输出 |
| line 行号命令 | 为什么: atLine 消费方 |
| 渲染(View 层) | 为什么: 教学价值低 |## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 输入一条命令 → 命令系统把它变成一次增强 → 字节码被织入 → 业务执行时回调分发 → 数据回流成表格/树。

### 依赖图

```
01 命令注册与执行链                 ← 无前置
  └─ 02 字节码增强引擎              ← 依赖 01 (命令执行后进入 enhance)
       └─ 03 Spy 回调分发链          ← 依赖 02 (织入的调用点→分发)
            └─ 04 watch/trace/tt    ← 依赖 03 (监听器是分发链的终点)
```
### 教学顺序

01 命令体系(怎么执行)→ 02 字节码增强(怎么织入)→ 03 Spy 分发(怎么回调)→ 04 watch/trace/tt(怎么输出)
### 文章拆分 (4 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-command-system.md | 命令注册与执行链 | 注册 49 命令/@Name 注解/分词/Job/查找/CLIConfigurator 注入/管道(支撑小节 2a) |
| 2 | 02-bytekit-enhancer.md | 字节码增强引擎 | EnhancerCommand 模板(2b)+ Enhancer 流程 + ByteKit(含 **ClassLoaderAwareClassWriter**)+ 10 拦截器 + inline 防重复 + **SearchUtils/matcher 匹配** |
| 3 | 03-spy-dispatch.md | Spy 回调分发链 | SpyAPI→SpyImpl→AdviceListenerManager→AdviceListenerAdapter→Advice/ArthasMethod/ThreadLocalWatch |
| 4 | 04-watch-trace-tt.md | 追踪命令实现 | watch(WatchAdviceListener+OGNL 条件+**ObjectVO 展开**)+ trace(TraceTree)+ stack(栈帧裁剪)+ tt(replay)+ **monitor/line 命令** |

### 文章内机制顺序

```
01: @Name 注解 → BuiltinCommandPack 注册 → ShellLineHandler 分词 → JobController
    → InternalCommandManager 查找 → ProcessImpl.parse → CLIConfigurator.inject → process()
02: EnhancerCommand.enhance → Enhancer.enhance(匹配/过滤/注册/retransform)
    → transform(ByteKit 解析 10 拦截器 → 防重复 → 织入 → 输出字节码)
    → inline=true 与防重复增强的关系
03: 业务执行 → SpyAPI 静态转发 → SpyImpl 查 AdviceListenerManager
    → AdviceListenerAdapter 包装 → 具体 Listener(before/afterReturning/...)
    → Advice 场景模型 + ThreadLocalWatch 计时 + isConditionMet 条件 + -n 限次
04: watch(参数/监听器/watching 输出)→ trace(树构建/合并/渲染)
    → stack(栈帧裁剪)→ tt(ring 栈/重放)
```

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "输入 watch,3 秒后方法被'监视'了——发生了什么?" | 02: retransformClasses 热替换整条链 |
| "为什么增强代码保留原始调用栈?" | 02: inline 内联直调而非反射 |
| "业务方法怎么找到自己的监听器?" | 03: AdviceListenerManager 按 ClassLoader+类+方法索引 |
| "同一个方法被 watch 两次会重复织入吗?" | 02: GroupLocationFilter 扫描已存在的 SpyAPI 调用 |
| "trace 的树怎么知道方法调了哪些子方法?" | 03+04: invoke 追踪桩(InvokeTraceable)+ TraceTree.begin/end |
