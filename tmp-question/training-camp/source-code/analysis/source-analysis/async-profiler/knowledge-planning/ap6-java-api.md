# 域 AP-6: Java API 与外部集成 — 知识规划

> 源码路径: src/api/one/profiler/(AsyncProfiler.java + Agent.java + AsyncProfilerMXBean.java + Counter.java + Events.java + Recording.java + Span.java 共 7 文件) + src/helper/one/profiler/(Instrument.java + JfrSync.java + LockTracer.java 共 3 文件 + .class) + src/converter/one/proto/(1 文件)
> 源码量: ~11 文件,核心 ~1000 行
> 提取日期: 2026-08-10(v1 深读提取)
> 前置域: AP-3 篇 4(JNI 桥 native 侧)/AP-5(JfrSync)/AP-2(Instrument/LockTracer 注入目标)

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| AsyncProfiler.java:19-29 | **单例**: `implements AsyncProfilerMXBean`(:19);`getInstance()`(:25)/`getInstance(String libPath)`(:29)——**加载即单例**,重复调用返回同一实例 | High |
| AsyncProfiler.java:29-60 | **库加载五级策略**: ①libPath 显式 `System.load` ②`getVersion()` 试探(**已预加载**(-agentpath)则跳过 :39-40)③`one.profiler.libraryPath` 系统属性 :43-44 ④**extractEmbeddedLib 内嵌库解压**(:49-50,临时文件加载后删除)⑤`System.loadLibrary("asyncProfiler")` 兜底(:54) | High |
| AsyncProfiler.java:162-228 | **execute 家族**: `getSamples` native(:162);`execute(command)`(:188)= `execute0`(:192)——**全部能力走字符串协议**;`dumpCollapsed/dumpTraces/dumpFlat`(:202-228)都是拼串(`"collapsed,samples"`/`"traces=N"`)— 与 AP-1 的逗号串协议闭环 | High |
| AsyncProfilerMXBean.java:19-33 | **JMX 管理接口**: `OBJECT_NAME = "one.profiler:type=AsyncProfiler"`(:20)+ start/resume/stop/getSamples/getVersion/execute/dumpXxx——**JMX 远程管理** | High |
| helper/Instrument.java:15-33 | **插桩目标**: `recordEntry()` native(:19)/`recordExit(startTimeNs, minLatency)`(:21,延迟阈值判断)/`recordExit0`——**BytecodeRewriter 注入的 Java 方法调用它**(AP-3 篇 2 latency 模式闭环) | High |
| helper/LockTracer.java:9-22 | **锁采样入口**: 注释 "Helper class to call JNI RegisterNatives in a **trusted context**"(:9);`setEntry0` native(:22)——bootstrap CL 注册 native 避免警告 | Medium |
| helper/JfrSync.java | **JFR 同步桥**: AP-5 篇 2 的 JfrSync(JDK JFR 录制同步)——native 反射调用的 Java 侧 | Medium |
| Recording.java:17-98 | **录制 API**: 用户级录制(开始/停止/快照);`jdk.jfr.internal.JVM` 反射(:78);`emitSpan` native(:98,Span 事件)——自定义时间区间事件 | Medium |
| Events.java/Counter.java/Agent.java/Span.java | **API 家族**: 事件常量/计数类型/Agent 生命周期/Span 时间区间 | Low |

*9 个知识点*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

无(API 层是薄壳)

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| execute 协议闭环 | AsyncProfiler.java(execute0), arguments.cpp(AP-1 解析), main.cpp(AP-1 拼串) |
| helper 与 native 的注入闭环 | Instrument.java, LockTracer.java, JfrSync.java + native 侧(AP-2/3/5) |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| MXBean | AsyncProfilerMXBean.java |
| Recording/Span | Recording.java |

---

## 03 深度分类

### 🔴 Deep (教学核心)

| KP | 为什么 |
|----|------|
| 库加载五级策略 | "API 怎么找到 .so"的完整答案;内嵌解压是亮点 |
| 单例(加载即实例化) | 库只加载一次;与 ArthasBootstrap 同模式(AR-1) |
| execute 字符串协议闭环 | **Arthas 集成点**: execute("start,event=cpu") 全链(AP-1 拼/AP-1 解析/此处入口) |
| Instrument helper(插桩目标) | BytecodeRewriter 注入的"收件人"——AP-3 篇 2 的闭环 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| MXBean JMX 管理 | 远程管理接口 |
| LockTracer trusted context | JNI 注册的细节 |
| Recording/Span | 用户级录制 API |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| JfrSync | AP-5 已述,此处是 Java 侧 |
| API 家族常量 | 用时可查 |

---

## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 外部怎么用这个采样器(API 层)→ 与前面 6 域怎么闭环。

### 依赖图

```
01 Java API(单例/加载/execute)          ← 无前置(对外入口)
  └─ 02 helper 与集成闭环                 ← 依赖 01 (helper 被 native 调)
```

### 教学顺序

```
01 AsyncProfiler(单例/五级加载/execute 家族/MXBean)
  → 02 helper 三件套(Instrument/LockTracer/JfrSync)+ 全链闭环(与 Arthas 集成)
```

### 文章拆分 (2 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-java-api.md | Java API 入口 | 单例/五级加载/execute 家族/MXBean |
| 2 | 02-helper-closure.md | helper 与全链闭环 | Instrument/LockTracer/JfrSync/Recording/与 Arthas 集成闭环 |

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "API 一行代码怎么就把 .so 找到的?" | 五级加载策略(01 篇) |
| "Arthas execute 的字符串最后到哪?" | execute0 → parse(01 篇,闭环) |
| "插桩注入的 recordEntry 是谁?" | Instrument helper(02 篇) |
