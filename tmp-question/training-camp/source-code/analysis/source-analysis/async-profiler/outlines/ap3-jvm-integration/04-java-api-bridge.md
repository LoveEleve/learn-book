# 04. Java API 的 native 侧 — execute0 与 RegisterNatives

> 🟡 Working | 13 KP 中的 2 个(JNI 注册/getSamples)
> 读者处境: Arthas 调的 `AsyncProfiler.execute("start,event=cpu")`——这条 JNI 桥的 native 侧长什么样?

### 1. "execute 协议落地" — execute0/execute1

场景: Java 字符串命令怎么进 C++ 的 Arguments::parse?

- `Java_one_profiler_AsyncProfiler_execute0`(javaApi.cpp:56): JNI 入口——jstring → C 字符串 → `Arguments::parse`(AP-1 篇 1 的第二层解析)→ 执行
- `execute1`(:97): 返回二进制(JFR 数据);注释——**"execute1 calls should not specify an output file argument"**(:108,输出走返回值)
- `getSamples`(:128): 返回已采样计数(配合 VMThread::nativeThreadId :137 按线程统计)
- 注册: `RegisterNatives`(javaApi.cpp:179-216)——**不用 -Djava.library.path 的 System.loadLibrary 命名约定,显式注册**

关键设计: **显式注册 vs 命名约定 + shaded 兼容**: [JNI: RegisterNatives 把 Java 方法名/签名与 C 函数指针绑定——避免 `Java_包名_类名_方法名` 长符号名的脆弱性,且不用全部导出 JNIEXPORT]JNI 默认按 `Java_包名_类名_方法名` 符号约定查找;这里用 `RegisterNatives` 表(:179-188)——**规避名字过长/混淆问题,且无需 JNIEXPORT 导出全部**。更妙的是注册时机(javaApi.cpp:196-216 注释): **通过栈回溯找真实的 AsyncProfiler 类**——因为类可能被 shaded(改名/移包),`System.load` 栈帧的下一个帧就是调用方,`GetMethodDeclaringClass` 拿到实际类再注册——**shaded 兼容是 RegisterNatives 路线的隐藏收益**。asprof.cpp 的 C API 是另一条嵌入式路径(AP-1)。

### 2. "两条 API,一个内核" — execute 与 asprof_execute

场景: Java API(Arthas 用)与 C API(嵌入式用)什么关系?

- Java 侧: `AsyncProfiler.execute()` → execute0 → `Arguments::parse` + `Profiler::runInternal`(AP-1 篇 1 的 asprof_execute 同款: asprof.cpp:27-40)
- 对比: **javaApi.cpp = JNI 桥**,asprof.cpp = **C 导出**——两条通道最终都汇到 `Profiler::instance()` 单例
- Arthas 的 `ProfilerCommand`(AR-6 篇 1)调的就是 execute0——**本项目 AP-1~AP-3 学的所有机制,最终都被这条 execute 串起来**

关键设计: **字符串协议是统一入口**: 无论 JNI/命令行/C API,都走 `execute("action,key=value")` → parse → 引擎——三入口一内核。这就是为什么 Arthas 只要会拼字符串就能驱动全部能力(AR-6 的 executeArgs)。

---

跨域桥: 协议解析 = AP-1(arguments.cpp);引擎执行 = AP-2(profiler.cpp);Java API 完整面 = AP-6(AsyncProfiler.java 7 文件);Arthas 消费方 = AR-6 篇 1(ProfilerCommand)。

**OpenJDK 关联**: [域 27 JNI — outlines/27-jni/] — RegisterNatives 语义与 JNI 方法绑定。
