# Arthas 源码学习范围规划

> **版本**: v4.3.2
> **仓库**: `/data/workspace/source-code/code/spring/arthas/`
> **规模**: core 477 + commands 239 = 899 个源文件
> **日期**: 2026-08-03

---

## 一、仓库概况

Arthas 是阿里巴巴开源的 Java 诊断工具，通过 **Java Agent + Instrumentation API** 实现运行时的字节码增强、JVM 信息采集和动态命令执行。核心机制：Attach 到目标 JVM → 加载 `arthas-agent.jar` → `ClassFileTransformer` 字节码增强 → WebSocket/Telnet 交互式命令 → `Instrumentation.retransformClasses()` 热替换。

**核心模块**：

| 模块 | 职责 | 状态 |
|---|---|---|
| `core/` | 核心引擎：Agent 加载、命令调度、字节码增强(ASM)、ClassLoader 隔离 | ✅ |
| `common/` | 公共工具：JVM 信息采集、OS 信息、日志 | ✅ |
| `client/` | 客户端：Telnet/HTTP/WebSocket 交互 | 淘汰 |
| `arthas-model/` | 命令模型定义 | ✅ |
| `arthas-vmtool/` | VM 工具（与 JDK 版本无关的 API） | 淘汰 |
| `arthas-mcp-server/` | MCP 协议 Server | 淘汰 |
| `arthas-spring-boot-starter/` | Spring Boot 集成 | 🟡 |
| `async-profiler/` | 异步性能采样（perf_events + JFR） | 淘汰 |
| `agent/` `arthas-agent-attach/` | Agent 打包/Attach 入口 | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| A-1 | **Agent 启动与 SpyAPI 注入** | ArthasBootstrap(1018行), InstrumentTransformer, Enhancer(760行) | **两阶段启动**：① `VirtualMachine.attach(pid)` → `loadAgent(arthas-agent.jar)` → `AgentLauncher.agentmain()` → `ArthasBootstrap`(单例) → 创建 `TransformerManager`；② **SpyAPI 注入**：`instrumentation.appendToBootstrapClassLoaderSearch(spyJar)` 注入 Bootstrap ClassLoader→**ClassLoader 增强**：`classLoaderInstrumentTransformer`(InstrumentTransformer) 修改 `ClassLoader.loadClass()` 字节码→`retransformClasses(ClassLoader.class)` 热替换→确保所有 ClassLoader 都能访问 SpyAPI（不常见于 target/lib 但需要 byteman 场景）；③ `SpyAPI.init()` → ShellServer 启动；**销毁**：`removeTransformer` + `SpyAPI.setNopSpy()` + `SpyAPI.destroy()` 完整清理
| A-2 | **命令体系** | EnhnacerCommand, BuiltinCommandPack, 52 个命令（7 类） | **7 类命令**：① **monitor200**(Watch/Trace/Stack/Monitor/TT/Thread/JVM/Memory/Dashboard/Heap/LGC/Profiler/VmTool)—监控诊断；② **klass100**(sc/sm/jad/dump/redefine/retransform/classloader)—类操作；③ **basic1000**(help/version/session/reset/stop)—基础；④ **logger**—动态修改日志级别；⑤ **view**(jfr/vmoption/sysenv/sysprop)—JVM 参数查看；⑥ **express**(ognl/groovy)—表达式执行；⑦ **hidden**(auth/jad/july/perfcounter)—内部命令；**管道**：`thread \| grep BLOCKED` `\|` 管道链式调用；**条件过滤**：OGNL `condition-express` 表达式（如 `params[0]>100`）
| A-3 | **字节码增强（ByteKit+SpyInterceptors）** | Enhancer(760行), SpyInterceptors(5种), ByteKit MethodProcessor, AdviceListener(93行) | **ByteKit 框架**：使用重定位 ASM(`com.alibaba.deps.org.objectweb.asm`—避免应用 ASM 版本冲突)+`MethodProcessor`(拦截器层)；**SpyInterceptors** 是 ByteKit ↔ SpyAPI 的桥梁——5 种拦截器：① `SpyInterceptor1`—`@AtEnter(inline=true)`→入口注入 `SpyAPI.atEnter()` ② `SpyInterceptor2`—`@AtExit(inline=true)`→出口注入 `SpyAPI.atExit()` ③ `SpyInterceptor3`—`@AtExceptionExit(inline=true)`→异常注入 `SpyAPI.atExceptionExit()` ④ `SpyLineInterceptor`—`@AtLine(lines={-1})`→**每行注入** `SpyAPI.atLine()`(LineCommand) ⑤ `SpyTraceInterceptor1`—`@AtInvoke(name="",inline=true)`→每个方法调用注入(解决 trace 下钻)；**`inline=true` 关键**：ByteKit 将 SpyAPI 调用**内联**到目标方法字节码中——不通过反射 invoke，保留原始调用栈；**@Binding 注解**：`@Binding.This/@Class/@MethodInfo/@Args/@Return/@Throwable/@Line/@ArgNames/@LocalVars` 绑定上下文；**增强流程**：`Enhancer.enhance()`→类名/方法名匹配(`SearchUtils`称)→`MethodProcessor.process()`(ByteKit 注入 interceptor)→`ClassWriter` 输出→`retransformClasses()`
| A-4 | **JVM 信息采集（thread/jvm/dashboard）** | ThreadCommand, JvmCommand, DashboardCommand | **ThreadCommand**：`ThreadMXBean.dumpAllThreads()+getThreadInfo()` → CPU 时间(ThreadCpuTime)→死锁检测(findDeadlockedThreads)→按 CPU%/状态排序→表格输出；**JvmCommand**：`RuntimeMXBean`(启动参数+类加载统计)+`MemoryMXBean`(堆使用)+`GarbageCollectorMXBean`(GC次数/耗时)+`OperatingSystemMXBean`(系统负载)；**DashboardCommand**：实时刷新 QPS/RT/SuccessRate(基于 Instrument 计数器或 Tomcat MBean) |

### 🟡 扩展域（2 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| A-5 | **watch/trace/stack 动态追踪** | EnhancerCommand, AdviceListener(93行—5回调), WatchCommand(228行), TraceCommand, StackCommand | **统一 EnhancerCommand 模板**：watch/trace/stack/monitor/tt 都继承 `EnhancerCommand`→调用 `Enhancer.enhance()` 注册 ClassFileTransformer→`retransformClasses()` 热替换→AdviceListener 回调收参；**AdviceListener 5回调**：① `before()`(入参/类名/方法名) ② `afterReturning()`(返回值) ③ `afterThrowing()`(异常) ④ `atLine()`(行号+局部变量—LineCommand专用) ⑤ `create()/destroy()`(生命周期)；**命令差异**：WatchCommands 显示入参/返回值/异常—支持 `-x 3` 深度展开 + OGNL `condition-express` 过滤；TraceCommands 显示方法调用树(耗时+占比+异常)——`TraceAdviceListener` 构建树形 Timeline；StackCommands 显示调用栈——`ThreadUtil.getThreadInfo()` 获取；**条件过滤**：OGNL 表达式（如 `params[0]>100`）在 `AdviceListener.before()` 中执行 `OgnlExpress` 判断 |
| A-6 | **Spring Boot Starter 集成** | ArthasConfiguration, ArthasProperties | `arthas-spring-boot-starter` 自动装配→`arthas.agent-id` 绑定 Spring 应用名→`arthas.tunnel-server` 配置远程 Tunnel Server→`arthas.session-timeout` 会话超时 |

---

## 三、淘汰清单

| 模块/功能 | 理由 |
|---|---|
| `client/` (Telnet/WS UI) | 交互终端——非诊断核心 |
| `arthas-vmtool/` | JDK 版本无关 VM API——工具层 |
| `arthas-mcp-server/` | MCP 协议——特定集成 |
| `async-profiler/` | perf_events 火焰图——独立工具 |
| `agent/` `arthas-agent-attach/` | 打包/Attach 入口——构建层面 |
| `arthas-demo-*` `labs/` | 示例/实验 |
| 200+ 个具体命令实现 | 每个命令独立实现——学透核心个例即可 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 4 |
| 🟡 扩展域 | 2 |
| **总域** | **6** |
| 淘汰模块 | 7+ 个 |

---

## 五、学习顺序建议

```
A-1 Agent 启动与 ClassLoader 隔离（理解 arthas 如何"寄生"到目标 JVM）
  → A-2 命令调度与执行（理解命令如何注册、解析、执行）
    → A-3 字节码增强 ASM（理解 watch/trace 如何"注入"代码）
      → A-4 JVM 信息采集（理解 thread/jvm/dashboard 数据来源）
        → A-5/A-6 按需深入
```

以上规划完成，共 **4🔴+2🟡=6 域**。
