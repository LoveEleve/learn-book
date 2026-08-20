# 02. watch 是怎么"钻进"你的方法里的? — 字节码增强引擎

> 🔴 Deep | 37 KP 中的 8 个(EnhancerCommand + Enhancer + ByteKit + 10 拦截器)
> 读者处境: 上篇命令跑起来了——现在核心问题: 一个已经加载的类,怎么被"塞进"监视代码?

### 1. "模板方法" — EnhancerCommand 统一增强入口

场景: watch/trace/stack/monitor/tt 长得像、增强逻辑也像——怎么复用?

- `EnhancerCommand` 抽象类(monitor200/EnhancerCommand.java:39): 7 个子类(Watch/Trace/Stack/Monitor/TimeTunnel/Line/GroovyScript,各子类 `extends EnhancerCommand`)
- `enhance()`(:193-292): `new Enhancer(listener, listener instanceof InvokeTraceable, skipJDKTrace, ...)`(:218)→ `enhancer.enhance(inst, maxNumOfMatchedClass)`(:223)
- **`listener instanceof InvokeTraceable` = isTracing**: watch 不是(只要 before/after 回调),trace 是(还要方法内子调用的追踪桩)——一个 instanceof 决定织入哪套拦截器
- 子类只实现 4 件事: 类名匹配器/排除匹配器/方法名匹配器/`getAdviceListener(process)`(各命令自己的监听器工厂)

关键设计: [模式: 模板方法(EnhancerCommand 7 子类)+ 钩子回调(Enhancer=ClassFileTransformer)+ 管线(ByteKit 织入)] **模板方法模式**: 增强的"骨架"(匹配→注册→retransform)固定,可变的只有"匹配什么类/什么方法/回调给谁"。新增一个追踪命令 = 继承 + 实现 4 个方法 + 写一个 Listener——这就是为什么 49 个命令里追踪类只有 7 个是"增强命令"。

### 2. "找到并热替换" — Enhancer.enhance: 匹配/过滤/注册/retransform

场景: `watch com.example.Service doBiz` 只给了类名(支持通配),怎么找到真实类并让它"生效"?

- `enhance()`(Enhancer.java:639-705): `SearchUtils.searchClass/searchSubClass(inst, classNameMatcher)`(:641-643,基于 Instrumentation 的已加载类枚举;searchClass :28 + 子类搜索 :53-54)→ `filter()`(:546): 剔除 classloader 不匹配/arthas 自身类/unsafe 类(`!GlobalOptions.isUnsafe && loader==null` :560)/lambda 等
- 匹配器驱动: 命令的 `getClassNameMatcher()` 返回 `WildcardMatcher`/`RegexMatcher`(watch `-E`)/`GroupMatcher`(trace `-p`)——`EnhancerCommand` 只声明"用什么匹配",SearchUtils 执行
- 超限保护: 匹配类数 > `maxNumOfMatchedClass` → 报 "The number of matched classes is X, greater than the limit"(:653)
- 注册自身为 transformer: `TransformerManager.addTransformer(this, isTracing)`(:663)→ `inst.retransformClasses(classArray)`(:673-697,批量/逐个)
- [JVMTI: retransformClasses 对**已加载**类重新跑一遍 ClassFileTransformer 管线,拿到新字节码原地替换——已加载实例自动用新版本,正在执行的方法仍跑旧版本]
- 懒加载模式(`-l`): `addLazyTransformer`(:667-670)——类还没加载时预注册,将来类首次加载时就增强

关键设计: **Enhancer 本身就是 ClassFileTransformer**(Enhancer.java:73)——注册自己 + retransformClasses 触发"自己再被调用一次"。批/逐个的选择(`GlobalOptions.isBatchReTransform`)是性能 vs 失败隔离的取舍。

### 3. "字节码手术台" — transform(): ByteKit 织入 10 个拦截器

场景: JVM 回调 `transform()` 给了原始字节码——现在开始"手术"。

- 预检: `loadClass(SpyAPI)` 能加载才继续(Enhancer.java:154)——加载不到说明这个 ClassLoader 看不到 SpyAPI,增强会 NoClassDefFoundError(AR-1 §3 的 enhanceLoaders 就是治这个)
- 解析: `AsmUtils.toClassNode(classfileBuffer)`(:197)→ 读类结构
- **ByteKit 介入**: `new DefaultInterceptorClassParser()`(:203)→
  - `parse(SpyInterceptor1/2/3.class)`(:207-209)— watch 三件套(enter/exit/exception),**总是插入**
  - isTracing 时: `skipJDKTrace==false` → `parse(SpyTraceInterceptor1/2/3.class)`;`true` → `parse(SpyTraceExcludeJDKInterceptor1/2/3.class)`(:220-230)
  - 行号增强(line 命令): `parse(SpyLineInterceptor)` + `LineLocationMatcher` 指定行号(:211-218)
- **防重复增强**: `GroupLocationFilter`(:218)+ `InvokeContainLocationFilter`(:253-258 等)——扫描方法字节码,若已有对 `SpyAPI.atEnter/atBeforeInvoke/atLine` 的直接调用则跳过(**这就是"重复 watch 不会叠加"的机制**)
- 织入: 逐方法 `new MethodProcessor(classNode, methodNode, locationFilter)` + `interceptor.process(methodProcessor)`(:313-332)——ByteKit 计算插入点,`InliningAdapter` 把拦截器方法体内联进去
- 注册监听器: 每个织入的方法 `AdviceListenerManager.registerAdviceListener(inClassLoader, className, methodName, desc, listener)`(:335-336)——下一篇的分发索引
- 输出: 类版本 <49 提升(:346)→ `AsmUtils.toBytes(classNode, inClassLoader, classReader)`(:351,**复用原始 ClassReader 常量池**——classfileBuffer 的常量池可复用,避免 metaspace OOM(源码注释: 'keep origin class reader for bytecode optimizations, avoiding JVM metaspace OOM',Enhancer.java:195))→ `classBytesCache` 缓存(reset 还原用,:354)
- **类型安全写出**: ByteKit 用 `ClassLoaderAwareClassWriter`(bytekit/asm/ClassLoaderAwareClassWriter.java:12-36)——重写 `getCommonSuperClass`(:34): ASM 计算两个类型的公共父类时要加载类,默认用系统 CL 会 NoClassDefFoundError,这里**改用目标 ClassLoader** 加载——这是织入代码在"目标类加载器上下文"里正确计算类型的保证

关键设计: **transform 是"按需织入"**: 不是整类织入,是**方法级**——类匹配 → 方法名匹配 → 只改命中的方法;同一方法被多个命令 watch,靠防重复过滤器保证每个 SpyAPI 调用只插一次(多个 listener 挂在同一调用点上,分发时一次回调循环通知全部)。

### 4. "10 把手术刀" — SpyInterceptors 与 inline=true

场景: 织入的到底是什么代码?为什么能保留调用栈?

- 拦截器 = 静态方法 + 注解(SpyInterceptors.java:18): `@AtEnter(inline=true)`(SpyInterceptor1,:20-27)、`@AtExit`(SpyInterceptor2,:29-35)、`@AtExceptionExit`(SpyInterceptor3,:37-44)、`@AtLine(lines={-1})`(SpyLineInterceptor,:46-55)、`@AtInvoke`(SpyTraceInterceptor1/2/3,:57-100,whenComplete=false/true + `@AtInvokeException`)、`@AtInvoke(excludes="java.**")`(SpyTraceExcludeJDKInterceptor1/2/3,:102-125)
- `@Binding.This/@Class/@MethodInfo/@Args/@Return/@Throwable/@Line/@LocalVars` 声明要拿什么上下文——ByteKit 生成取参代码
- **inline=true 语义**: 拦截器方法体被**内联**进目标方法 → 生成对 `SpyAPI.atEnter(clazz, "方法名|描述", target, args)` 的**直接静态调用**——不是反射 invoke
- 证据: 防重复增强的 LocationFilter 扫描的就是 `MethodInsnNode(SpyAPI, "atEnter")`(Enhancer.java:253-258)——直接调用才能被扫描到
- excludes 的作用: trace 拦截器排除 `java.arthas.SpyAPI` 自身(防递归 trace 到 arthas 自己的调用)和 8 种 box 类型(Enhancer.java:305-308 `isBoxType` 跳过装箱代码)

关键设计: **inline 而非反射**: 反射 invoke 会在调用栈里插一帧 `Method.invoke`,污染业务栈(stack 命令要裁剪的就是这类);内联直调让业务调用栈**零污染**,同时省掉反射开销。代价: 织入的代码不能太大(InlineAdapter 有长度限制)——所以 SpyAPI 只做"转发到静态方法"这一件事,业务逻辑全在 SpyImpl 里(下一篇)。

---

跨域桥: SpyAPI 注入 Bootstrap = AR-1 篇 2;transform 预检 loadClass(SpyAPI) = AR-1 enhanceLoaders 的配套;classBytesCache + reset = AR-0 篇 3 的 reset 命令;SpyImpl/AdviceListenerManager = 下一篇。

---

**OpenJDK 关联**:  [OpenJDK 域 47 Instrumentation — outlines/47-instrumentation/] — ClassFileLoadHook 调用链(transform 的 JDK 侧);**另见** [OpenJDK 域 44 Class Verification — outlines/44-class-verification/] — 织入后的字节码必须通过验证(StackMapTable 重算)。
**另见** [OpenJDK 域 28 JVMTI — outlines/28-jvmti/] — transform 回调的 JVMTI 侧(ClassFileLoadHook);**另见** [OpenJDK 域 10 Metaspace — outlines/10-metaspace/] — 复用 ClassReader 常量池正是防 metaspace 膨胀的经典手法。

### 核心悬念

**"字节码织进去了——业务方法每次执行,谁接到 SpyAPI 的呼叫?"** — 不是广播,不是全局表: 是按"ClassLoader + 类名 + 方法名 + 描述符"四元组建的分发索引。同名类两个版本,回调各回各家。

> → [03-spy-dispatch.md](03-spy-dispatch.md)