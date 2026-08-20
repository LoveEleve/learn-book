# 02. 插桩的"收件人"与全链闭环 — helper 三件套

> 🟡 Working | 9 KP 中的 4 个(Instrument/LockTracer/JfrSync/Recording)
> 读者处境: native 侧织入/注入的代码,最终落在哪?——三个 helper 类就是"native 的 Java 收件箱",也是整个项目与 Arthas 的闭环点。

### 1. "插桩代码调的就是它" — Instrument helper

场景: BytecodeRewriter 在方法进入处注入的代码长什么样?

- `one.profiler.Instrument`(helper/Instrument.java:13): `recordEntry()` native(:19)——方法进入时调;`recordExit(startTimeNs, minLatency)`(:21)——**延迟阈值判断**(`System.nanoTime() - startTimeNs >= minLatency` 才记录,高频短方法自动跳过);`recordExit0`(:33,latency=0 的快路径)
- 注入端: AP-3 篇 2 的 BytecodeRewriter `rewriteCodeForLatency` 生成 `Instrument.recordEntry/recordExit` 调用——**改写器与 helper 是"织入+收件"的一对**
- [JVM: 注入的字节码是普通静态调用(Java 方法)——helper 类必须被目标应用可访问(bootstrap CL 或应用 CL)]

关键设计: **"注入调用 + Java 收件"的最小闭环**: async-profiler 的插桩不注入 native 指令,而是注入对 Java helper 的静态调用——native 实现藏在 RegisterNatives 里。这让注入代码"看起来就是普通 Java",可调试、可替换。

### 2. "trusted context 注册" — LockTracer helper

场景: 为什么锁采样要一个 helper 类?

- `LockTracer`(helper/LockTracer.java:9): 注释——"**Helper class to call JNI RegisterNatives in a trusted context**";`setEntry0` native(:22)
- 目的: native 侧(AP-2 篇 4 的 lockTracer)要注册 native 方法——**在 bootstrap CL 下注册避免 JNI 警告**(:17 注释,"belonging to the bootstrap class loader for RegisterNatives not to emit a warning")
- [JNI: RegisterNatives 在非引导类加载器注册会发警告;helper 类挂在 bootstrap 下 = "可信上下文"]

关键设计: **native 注册的"身份"问题**: 谁调用 RegisterNatives 决定注册的类加载器归属——helper 类是"引导加载器下的注册代理",保证 JNI 表干净。

### 3. "JDK JFR 的桥" — JfrSync 与 Recording/Span

场景: `--jfrsync` 要操作 JDK 的 JFR,Span 事件要自定义录制。

- `JfrSync`(helper/JfrSync.java): AP-5 篇 2 已述的同步桥——native 反射调用的 Java 侧(JDK JFR 的 start/stop)
- `Recording`(api/Recording.java:17): 用户级录制——`jdk.jfr.internal.JVM` 反射(:78,时钟源对齐);`emitSpan` native(:98)——**Span 事件**(自定义时间区间,如"数据库调用耗时"写入录制)
- `Span.java`: 时间区间事件 API——业务埋点级集成

关键设计: **两级录制 API**: 系统级(execute 采样)+ 用户级(Recording/Span 业务事件)——Span 让业务代码往 JFR 里写自定义区间,与采样事件同时间线。这是"框架采样 + 业务埋点"的融合。

### 4. 全链闭环(与 Arthas 的合拢)

```
Arthas ProfilerCommand (AR-6 篇 1)
  → AsyncProfiler.execute("start,event=cpu")     ← 本项目 AP-6
    → execute0 → arguments.cpp parse              ← AP-1
      → Engine 启动(perf/itimer)                  ← AP-2
        → 信号 → recordSample → 栈行走 → 符号      ← AP-2/3/4
          → flameGraph/JFR/otlp 输出              ← AP-5
```

关键设计: **一条命令,七个域**: Arthas 拼的字符串,走完 async-profiler 的全部机制——AP-0~AP-6 的每一篇都在这个闭环里找到位置。这就是"学完本项目 = 彻底搞懂 Arthas profiler 的底层"的意义。

---

跨域桥: 插桩注入 = AP-3 篇 2(BytecodeRewriter);锁采样 = AP-2 篇 4;JfrSync = AP-5 篇 2;协议 = AP-1 篇 1;Arthas = AR-6 篇 1。

**OpenJDK 关联**: [域 27 JNI — outlines/27-jni/] — RegisterNatives trusted context 与 JNI 注册规则。
