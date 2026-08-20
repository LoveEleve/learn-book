# 03. 线上代码和本地不一样 — 类与字节码操作

> 🟢 使用域 | 覆盖: sc/sm/jad/classloader/redefine/reset
> 读者处境: 线上行为和你本地代码对不上——版本没发?代码被动态修改?类被哪个 ClassLoader 加载?

### 1. "先找到类" — sc / sm / classloader

场景: 报错信息里的类行为异常,先确认它真的存在、是谁加载的。

- `sc org.example.Service`: 搜索已加载类(klass100/SearchClassCommand.java:37 `@Name("sc")` "Search all the classes loaded by JVM")
- `sc -d org.example.Service`: 类详情(ClassLoader 位置/注解/接口/继承链)
- `sm org.example.Service`: 类的方法清单+签名(klass100/SearchMethodCommand.java:42)
- `classloader`: 类加载器树+每个 CL 的类数量统计(klass100/ClassLoaderCommand.java:46 `@Name("classloader")`);`-c <hash>` 可把 hash 传给其他命令指定用哪个加载器

关键设计: `sc` 只看**已加载**的类——类没被用到就不存在(懒加载);`-d` 里的 `classLoaderHash` 是个金矿:同名类多版本共存时,你能看出业务代码到底由哪个 CL 加载(典型的 jar 冲突/双亲委派问题)。

生产注意: 多版本 jar 冲突排查流程: `sc -d` 看 CL hash → `classloader -c <hash>` 看 CL 树 → `jad --source-only <类名> -c <hash>` 反编译指定版本。

### 2. "看线上真实代码" — jad

场景: 本地代码和线上行为不符,直接反编译线上类看真相。

- `jad org.example.Service`: 反编译(klass100/JadCommand.java:40 `@Name("jad")` "Decompile class")
- `jad --source-only org.example.Service`: 只要源码
- `jad org.example.Service.Method`: 只反编译单个方法
- 配合 `sc -d` 的 classLoaderHash 指定版本: `jad -c <hash> --source-only ...`

关键设计: jad 反编译的是**当前生效的字节码**——包括运行期被增强/被 agent 改过的代码。线上"代码和我本地不一样"的三种真相它都能揭穿:版本没发、字节码被改过、条件分支走了你没想到的路径。

生产注意: jad 结果用于定位"线上到底在跑什么",不要拿它当代码评审工具(反编译丢失注释/泛型擦除)。

### 3. "紧急修复不上线" — redefine + reset(高风险)

场景: 线上一个空指针要马上修,等发版来不及。

- 现场编译: `mc /tmp/Fix.java -d /tmp`(memorycompiler,klass100/MemoryCompilerCommand.java:38 `@Name("mc")`)
- 热替换: `redefine /tmp/Fix.class`(klass100/RedefineCommand.java:38 `@Name("redefine")`,底层 `Instrumentation.redefineClasses`)
- 撤销: `reset` 还原所有增强(basic1000/ResetCommand.java:25)
- 注意: redefine **只能改方法体,不能改类签名**(加字段/加方法/改继承会报错)

关键设计: redefine 走 `Instrumentation.redefineClasses(ClassDefinition...)`——[JVMTI: RetransformClasses 与 RedefineClasses 的区别——redefine 直接用新字节码替换且不经过 ClassFileTransformer,retransform 才重新跑 transformer 管线;两者都禁止改类签名(结构变化)]——JVM 保留旧方法版本给正在执行的线程,新调用走新字节码;所以 watch/trace 的增强会先被还原再重织。

生产注意: 热更新是最后手段——改签名会直接抛 UnsupportedOperationException;redefine 后建议立即 `watch` 验证;排查类问题顺序: `sc -d` → `jad` → 确认原因,redefine 只在确认是代码 bug 且必须立即止血时用。

---

跨域桥: jad/redefine/reset 的字节码来源与还原 = AR-2 Enhancer 的 classBytesCache 与 ByteKit 织入;classLoaderHash = AR-1 ClassLoader 隔离。

---

**OpenJDK 关联**:  [OpenJDK 域 47 Instrumentation — outlines/47-instrumentation/] — redefine/retransform 的 JDK 侧实现;**另见** [OpenJDK 域 44 Class Verification — outlines/44-class-verification/] — 替换后的字节码要过验证器。
**另见** [OpenJDK 域 28 JVMTI — outlines/28-jvmti/] — redefine/retransform 的 JVMTI 侧(RedefineClasses/ClassFileLoadHook)。

### 核心悬念

**"jad 反编译的字节码从哪来?redefine 为什么不能改签名?"** — 反编译与热更新共享同一套字节码管线: 一个读,一个写。而"不能改签名"的限制,藏在 Instrumentation.redefineClasses 的 JVMTI 语义里。

> → [AR-2 篇 2](../ar2-watch-trace/02-bytekit-enhancer.md)
