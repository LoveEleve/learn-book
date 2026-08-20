# 02. SpyAPI 凭什么全世界都能访问? — 构造七步与 Bootstrap 注入

> 🔴 Deep | 31 KP 中的 3 个核心机制(initSpy / enhanceClassLoader / 参数链)
> 读者处境: 你已经知道 arthas 寄生进来了——现在看它进门后做的第一件事: 把 SpyAPI 放进"所有人都够得着"的地方。

### 1. "单例进门" — ArthasBootstrap.getInstance 与构造 7 步

场景: 两个入口(loadAgent/自 attach)都反射调用了 `getInstance`,重复 attach 怎么办?

- 单例: `synchronized static getInstance(Instrumentation, Map)`(ArthasBootstrap.java:918-923)— 静态 volatile 字段,重复调用直接返回已有实例;构造失败不缓存
- String 版本入口(:897-908): FeatureCodec 把 `key=value;key2=value2` 字符串解析为 Map,**每个 key 自动加 `arthas.` 前缀**——这就是配置文档里"arthas.xxx"的由来
- 构造器 7 步(ArthasBootstrap.java:149-196):
  1. `initFastjson`(:198)— 忽略 getter 错误(#1661)
  2. **`initSpy()`(:209-232)** ← 本篇核心
  3. `initArthasEnvironment`(:264-295)— 配置装配: 优先级 `命令行 > System Env > System Properties > arthas.properties`;`BinderUtils.inject` 绑定到 `Configure`(prefix="arthas")
  4. `initLogger` — 日志落到 `$HOME/logs/arthas/arthas.log`
  5. **`enhanceClassLoader()`(:234-262)** ← §3
  6. `initBeans` — ResultViewResolver/HistoryManager
  7. 末尾: 创建 `TransformerManager`(:194)+ 注册 shutdown hook(:195)

关键设计: [模式: 单例(ArthasBootstrap)+ 属性源责任链(ArthasEnvironment 多级优先级)+ 空对象(NOPSPY 兜底)] 单例保证了"寄生一次";**构造器即全部初始化**——没有懒加载,失败即销毁(异常会触发 destroy,ArthasBootstrap.java:513-517)。7 步顺序有依赖: SpyAPI 必须在日志/Server 之前(增强代码随时可能回调它),环境必须在 bind 之前(端口/隧道都要配置)。

### 2. "放进门后最安全的位置" — initSpy: 注入 Bootstrap ClassLoader

场景: watch 增强的代码在业务类里直接调用 `java.arthas.SpyAPI.atEnter(...)`——业务类由各种 ClassLoader 加载,它们怎么都"认识" SpyAPI?

- `initSpy()`(ArthasBootstrap.java:209-232): 先试 `parent.loadClass("java.arthas.SpyAPI")`(parent = System CL 的 parent = Platform CL;若能加载说明已注入,跳过)
- 否则: `instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(spyJarFile))`(:227)— **把 arthas-spy.jar 追加进 Bootstrap ClassLoader 的搜索路径**
- spyJar 位置: 与 arthas-core.jar 同目录(ARTHAS_SPY_JAR,ArthasBootstrap.java:105)
- [Java: appendToBootstrapClassLoaderSearch 不改类路径顺序(找不到才 fallback),但**全局可见**——任何 ClassLoader 委派链最终都能到达 Bootstrap,这是"增强代码能访问 SpyAPI"的唯一解释]
- SpyAPI 本体(spy/src/main/java/java/arthas/SpyAPI.java:24-27): 包名 `java.arthas`——**伪装成 JDK 包**,既避免被应用类加载器遮蔽,也避免与业务类冲突;`volatile AbstractSpy spyInstance`(:25)+ `NOPSPY` 兜底(:24),`INITED` 标志(:27)

关键设计: **为什么不把 SpyAPI 也塞进 ArthasClassloader?** 因为增强的代码在**业务类**里——它们的 ClassLoader 是应用自己的,根本看不见 ArthasClassloader。SpyAPI 必须放在"所有 ClassLoader 的共同祖先"上:Bootstrap。而增强代码只是"调一个静态方法",真正的逻辑在 SpyImpl(Arthas 侧),SpyAPI 只是薄转发层——**这是"注入点极小化"**: 目标 JVM 里只有 spy.jar 这一小坨 arthas 代码常驻,其余全在 ArthasClassloader 里可卸载。

### 3. "还有更刁钻的场景" — enhanceClassLoader 与 ClassLoader_Instrument 模板

场景: 某些**自定义 ClassLoader 不委派给父加载器**(不走标准双亲委派),增强后的业务类调用 SpyAPI 会 NoClassDefFoundError。

- `enhanceClassLoader()`(ArthasBootstrap.java:234-262): **默认不执行**(`configure.getEnhanceLoaders() == null` 直接 return,即需配 `arthas.enhanceLoaders=java.lang.ClassLoader,...`)
- 用 ByteKit 增强 `ClassLoader.loadClass` 本身: 读 `ClassLoader_Instrument.class` 模板字节码(:245-246)→ `InstrumentConfig` + `SimpleClassMatcher(loaders)`(:248-249)→ `new InstrumentTransformer`(:253)→ `addTransformer(classLoaderInstrumentTransformer, true)`(:254)→ 单类时直接 `retransformClasses(ClassLoader.class)`(:258)
- 模板逻辑(ClassLoader_Instrument.java:13-23): 若 `name.startsWith("java.arthas.")` → 改用 `ClassLoader.getSystemClassLoader().getParent()`(Platform CL)加载;否则 `InstrumentApi.invokeOrigin()` 调回原始 loadClass
- [ASM: @Instrument 模板类编译后,ByteKit 把模板方法体织入目标类(ClassLoader.loadClass)——模板即"织入的补丁代码",运行时读字节码而非编译依赖]
- 场景出处: issue #1596——"增强 ClassLoader#loadClass,解决一些ClassLoader加载不到SpyAPI的问题"(ArthasBootstrap.java:243-244 注释)

关键设计: 这层防线回答"**万一有 ClassLoader 不看 Bootstrap 怎么办**"——直接改 `java.lang.ClassLoader.loadClass`,把 `java.arthas.*` 的加载强制导向 Platform CL。它是"兜底"不是"常规": 双亲委派没坏的时候用不到,所以默认关闭、按需开启。

### 4. 参数链总览: 命令行字符串 → Map → Configure

场景: 你传的 `--session-timeout 3600` 最终怎么变成 ArthasBootstrap 里的一个字段?

- as.sh 拼参(`-session-timeout 3600` 等,as.sh:838-891)→ Arthas.java:91 `attachAgent` 解析成 Configure 对象 → `loadAgent` 传**字符串**(Arthas.java:125)
- AgentBootstrap 按 `;` 切出 agentArgs → 反射传给 `getInstance(inst, args 字符串)`(AgentBootstrap.java:183)
- `getInstance(String)`: FeatureCodec → Map + 加 `arthas.` 前缀(ArthasBootstrap.java:902-908)→ `getInstance(inst, Map)`
- 构造器: `initArthasEnvironment`(ArthasBootstrap.java:264-295)— Map 存进 `ArthasEnvironment`(多级 PropertySource)→ `BinderUtils.inject` 绑定到 `Configure`(prefix "arthas")——`sessionTimeout`、`tunnelServer`、`agentId` 等字段落地

关键设计: 链路绕了一大圈是因为**attach 参数只能传字符串**(loadAgent 的 API 限制),而 core 内部要用类型化配置——所以做了"字符串 → Map → 绑定"两次转换;`arthas.` 前缀在此过程中加上,统一了 as.sh 参数与 properties 配置的命名空间。

### 5. "配置是怎么变成字段的" — Configure/FeatureCodec/BinderUtils/ArthasEnvironment

场景: `--session-timeout 3600` 到 `Configure.sessionTimeout` 字段,中间三个工具类各干一件事。

- `Configure`(config/Configure.java:22-90): `@Config(prefix="arthas")` 标注;**18 个字段**——ip/telnetPort/httpPort/javaPid/tunnelServer/agentId/username/password/outputPath/**enhanceLoaders**/appName/statUrl/sessionTimeout/disabledCommands/commandLocations/**localConnectionNonAuth**/**mcpEndpoint/mcpProtocol**;注释要求**字段不能有默认值**(防配置混乱)
- `FeatureCodec`(config/FeatureCodec.java:18-100): `DEFAULT_COMMANDLINE_CODEC(';','=')`——`key=value;key2=value2` 的解析/编码器;getInstance(String) 用它把 attach 字符串转 Map
- `BinderUtils.inject`(config/BinderUtils.java:13-81): 反射遍历字段 → 按字段名从 Environment 取属性 → 注入;支持**嵌套对象递归**(Configure 里有复杂字段时 :81 递归)
- `ArthasEnvironment`(env/ 19 文件): 自研 PropertySource 栈(仿 Spring)——args/env/properties 多个属性源,`addFirst/addLast` 控制优先级;`arthas.config.overrideAll` 可反转 arthas.properties 的优先级(#986)

关键设计: **"无默认值"的纪律**: Configure 字段全空,由 BinderUtils 从多级属性源注入——默认值(如 session-timeout)由**消费方**(ShellServerOptions)兜底,而不是配置类里写死。这让"命令行 > Env > Properties"的优先级在任何组合下都一致。

---

跨域桥: SpyAPI 的调用侧 = AR-2(SpyInterceptors 织入 `SpyAPI.atEnter` 调用、Enhancer.transform 里 `loadClass(SpyAPI)` 预检 :154);TransformerManager 注册 = AR-2(Enhancer 增强流程);ClassLoader_Instrument 的 ByteKit 机制 = AR-2 ByteKit 框架。

---

**OpenJDK 关联**:  [OpenJDK 域 07 ClassFile & ClassLoader — outlines/07-classfile-classloader/] — 双亲委派/ClassLoader 搜索顺序是 SpyAPI 注入的理论基础;**另见** [OpenJDK 域 47 Instrumentation — outlines/47-instrumentation/] — appendToBootstrapClassLoaderSearch 的 JDK 实现。
**另见** [OpenJDK 域 28 JVMTI — outlines/28-jvmti/] — enhanceClassLoader 的 ByteKit InstrumentTransformer 最终走 JVMTI 回调。

### 核心悬念

**"SpyAPI 放进 Bootstrap 之后,增强的代码就安全了吗?"** — 大部分情况是。但万一有个不走双亲委派的 ClassLoader,连 Bootstrap 都不看呢?arthas 为此留了一条默认关闭的后路——它甚至要修改 java.lang.ClassLoader 自己的字节码。

> → [AR-2 篇 2](../ar2-watch-trace/02-bytekit-enhancer.md)