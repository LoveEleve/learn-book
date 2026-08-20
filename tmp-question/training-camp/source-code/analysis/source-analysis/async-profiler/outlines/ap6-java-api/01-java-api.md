# 01. 一行 getInstance,怎么把 .so 变活? — Java API 入口

> 🔴 Deep | 9 KP 中的 3 个(单例/五级加载/execute 家族)
> 读者处境: Arthas 里 `AsyncProfiler.getInstance().execute("start,event=cpu")`——这个单例怎么加载 native?execute 家族怎么把全部能力压进一个字符串?

### 1. "加载即单例" — getInstance

场景: 库加载一次,实例全局唯一。

- `AsyncProfiler implements AsyncProfilerMXBean`(AsyncProfiler.java:19);`getInstance()`(:25)→ `getInstance(String libPath)`(:29,`synchronized`)
- 静态字段 `instance`——**首次调用触发 native 加载,之后复用**
- 与 Arthas 对照: ArthasBootstrap 也是单例(AR-1 篇 2)——"寄生工具单例化"是共同模式

关键设计: **加载与使用耦合**: 单例构造里完成 native 加载——`getInstance()` 失败 = 库没加载成功,后续全不可用。加载失败的原因通过异常暴露(UnsatisfiedLinkError)。

### 2. "五级降级找库" — 加载策略

场景: 库可能在哪?显式路径/已预加载/系统属性/内嵌/classpath——按序尝试。

- `getInstance(String libPath)`(AsyncProfiler.java:29-60)五级:
  1. libPath 显式 → `System.load(libPath)`(:36)
  2. **预加载试探**: `profiler.getVersion()`——**-agentpath 已注入时无需再 load**(:39-40,注释直说)
  3. `one.profiler.libraryPath` 系统属性(:43-44)
  4. **extractEmbeddedLib()**: 从 jar 里解出内嵌 .so 到临时文件 → `System.load` → **finally 删除**(:49-51)——**用完即删**,不留垃圾
  5. `System.loadLibrary("asyncProfiler")`(:54,classpath/系统库路径兜底)
- [JNI: System.load(绝对路径)vs loadLibrary(搜索路径)——两者都是"把 .so 载入进程"的唯一通道]

关键设计: **"能少一个依赖就少一个"**: 内嵌解压(4)让"一个 jar 全包"成为可能(与 AP-1 的静态链接、AP-5 的 INCBIN 同哲学: 自包含);预加载试探(2)避免重复加载的 UnsatisfiedLinkError(Arthas 的 native 库临时文件复制也是这个问题的解法,AR-6 篇 2)。

### 3. "一个字符串驱动一切" — execute 家族

场景: start/stop/dump/collapsed/traces——全是字符串命令。

- `execute(String command)`(AsyncProfiler.java:188)→ `execute0(command)`(:192,native)
- `dumpCollapsed(Counter)`(:202)= `execute0("collapsed," + counter)`;`dumpTraces(n)`(:217)= `"traces=N"`——**拼串即 API**
- `getSamples()`(:162)是少数非字符串 native 方法
- 与 AP-1 闭环: Arthas `executeArgs` 拼的串(main.cpp)→ 这里 execute0 → arguments.cpp parse——**一条协议,三层代码,两个项目**(AP-1 篇 1/AP-3 篇 4 的闭环在此合拢)

关键设计: **API 是协议的投影**: Java API 没有"参数对象",只有"字符串拼接 + execute"——所有选项的权威定义在 native 的 arguments.cpp(AP-1),Java 侧只是拼串器。这解释了为什么 Arthas 能无缝驱动: 它直接拼 native 认识的串,不需要理解 Java API。

---

跨域桥: 协议解析 = AP-1(arguments.cpp);JNI 桥 = AP-3 篇 4(execute0/RegisterNatives);Arthas 消费方 = AR-6 篇 1(ProfilerCommand);自包含哲学 = AP-1 篇 1/AP-5 篇 1。

**OpenJDK 关联**: [域 27 JNI — outlines/27-jni/] — System.load/loadLibrary 的 native 加载语义。
