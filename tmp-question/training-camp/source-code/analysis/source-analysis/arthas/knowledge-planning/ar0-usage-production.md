# 域 AR-0: Arthas 使用与生产排查实践 — 知识规划

> 性质: 🟢 使用实操域(非源码分析)——读者为 Arthas 新手,本域先"会用"再"看原理"
> 提取来源: as.sh 参数定义 + 各命令 `@Name/@Summary/@Description` 注解 + 官方 wiki(arthas.aliyun.com/doc/)
> 提取日期: 2026-08-10
> 产出: 使用手册式大纲 + 四排查场景,作为 AR-1~AR-6 大纲的"场景"素材

## 01 逐源提取 (命令定义源码 → 功能 + 场景)

> 来源: `core/src/main/java/com/taobao/arthas/core/command/{包}/{命令}.java` 的 `@Name/@Summary/@Description`,as.sh 参数表。行号即各文件注解行。

| Source (命令/脚本) | Inferred Usage (功能与场景) | Confidence |
|------------|-------------------|------------|
| as.sh: parse_arguments (L559-820) + attach_jvm (L823-901) | **安装与 attach**: `--target-ip/--telnet-port(3658)/--http-port(8563)/--session-timeout/--tunnel-server/--agent-id/--stat-url/--app-name/--username/--password/--disabled-commands/--attach-only/--select`;三种 attach(直接 pid、`pid@ip:port`、jps 交互);attach 后默认 telnet 连终端 | High |
| ThreadCommand.java `@Name("thread")` `@Summary("Display thread info, thread stack")` | **thread 线程查看**: 全部线程表格/`thread id` 单线程栈/`-n N` CPU 最高 N 个/`-b` 阻塞与死锁/`--state` 按状态过滤 | High |
| WatchCommand.java `@Name("watch")` "Display input/output parameter, return object, thrown exception" | **watch 方法观测**: 入参/返回值/异常;`-x` 展开深度、`-n` 次数、`-b/-f/-e/-s` 时机、`-E` 正则、条件表达式 | High |
| TraceCommand.java `@Name("trace")` "Trace the execution time of specified method invocation" | **trace 耗时追踪**: 方法调用树+每节点耗时/占比;`-n`、条件表达式、`-p` 路径过滤 | High |
| StackCommand.java `@Name("stack")` "Display the stack trace for the specified class and method" | **stack 调用栈**: 命中方法时打印当前线程栈,含 trace_id/rpc_id | High |
| TimeTunnelCommand.java `@Name("tt")` "Time Tunnel" | **tt 时间隧道**: 记录方法调用现场(参数/返回值/异常/耗时)→ `-p` 重放/`-i` 查看/`-s` 搜索/`-w` watch 表达式 | High |
| MonitorCommand.java:22 `@Name("monitor")` "Monitor method execution statistics" | **monitor 方法统计**: 按方法聚合调用次数/成功率/耗时,定时刷新——生产"这个方法正常吗"的持续观测 | High |
| LineCommand.java:32 `@Name("line")` | **line 行级追踪**: 方法执行到哪一行+局部变量——定位"走到哪一步停了" | Medium |
| DashboardCommand.java `@Name("dashboard")` "Overview of thread, memory, gc, vm, tomcat info" | **dashboard 面板**: 实时线程 CPU + 内存 + GC + runtime + tomcat;`-i` 刷新间隔/`-n` 次数 | High |
| JvmCommand.java `@Name("jvm")` "Display the target JVM information" | **jvm 信息**: 9 数据块(RUNTIME/CLASS-LOADING/COMPILATION/GC/MEMORY-MANAGERS/MEMORY/OS/THREAD/FILE-DESCRIPTOR) | High |
| MemoryCommand.java `@Name("memory")` "Display jvm memory info" | **memory 内存**: heap/nonheap 内存池 + BufferPool 明细 | High |
| HeapDumpCommand.java `@Name("heapdump")` | **heapdump 导出**: 导出堆转储文件(生产 OOM 分析) | High |
| SearchClassCommand.java `@Name("sc")` "Search all the classes loaded by JVM" | **sc 类搜索**: 已加载类搜索;`-d` 详情/`-f` 字段/`-e` 反编译 | High |
| SearchMethodCommand.java `@Name("sm")` "Search the method of classes loaded by JVM" | **sm 方法搜索**: 类的方法列表+签名 | High |
| JadCommand.java `@Name("jad")` "Decompile class" | **jad 反编译**: 线上类反编译为可读 Java 源码(看线上代码/确认生效版本) | High |
| RedefineCommand.java `@Name("redefine")` "Redefine classes. Instrumentation#redefineClasses" | **redefine 热更新**: 用 .class 文件热替换线上类(紧急修复);配合 memorycompiler 现场编译 | High |
| ClassLoaderCommand.java `@Name("classloader")` "Show classloader info" | **classloader 类加载器**: 类加载器树/统计;`-c` 指定 classloader hash 供其他命令用 | High |
| OgnlCommand.java `@Name("ognl")` "Execute ognl expression" | **ognl 表达式**: 直接执行表达式(读成员变量/调静态方法/调对象方法) | High |
| ProfilerCommand.java `@Name("profiler")` "Async Profiler" | **profiler 火焰图**: `start/stop` 采样 CPU/Allocation/Lock/Wall;输出 html/文本火焰图 | High |
| LoggerCommand.java `@Name("logger")` "Print logger info, and update the logger level" | **logger 日志级别**: 查/改 logback、log4j2 级别(线上动态调日志,无需重启) | High |
| ResetCommand.java `@Name("reset")` "Reset all the enhanced classes" | **reset 还原**: 撤销所有增强(watch/trace 等织入的字节码) | High |
| StopCommand.java `@Name("stop")` "Stop/Shutdown Arthas server and exit the console" | **stop 停止**: 关停 arthas server(注意: starter 默认禁用此命令) | High |
| Sysprop/Sysenv/VMOption (basic1000) | **参数查看/修改**: sysprop 系统属性、sysenv 环境变量、vmoption VM 参数——均可线上修改 | Medium |
| Constants.java: WIKI_HOME | 官方文档: `https://arthas.aliyun.com/doc/`——每命令详情页 | Medium |

*23 个知识点(命令功能,v2 补充 monitor/line)*

---

## 02 聚合 — 命令族分类

> AR-0 的聚合维度是"功能族"——按生产排查的目的分组,而非文件关联。

### P1 — 生产高频命令族 (对应 AR-1~AR-6 源码域)

| 命令族 | 成员命令 | 排查场景 | 对应源码域 |
|----|---------|---------|---------|
| 线程族 | thread(-n/-b/--state/id) | CPU 飙高、死锁、线程阻塞 | AR-3 |
| 追踪族 | watch/trace/stack/tt | 慢接口、参数异常、返回值不符、现场重放 | AR-2/AR-5 |
| 类族 | sc/sm/jad/classloader | 线上代码与本地不符、类加载异常、找不到方法 | AR-2 |
| JVM 族 | jvm/memory/dashboard/heapdump | OOM、GC 频繁、内存泄漏、整体健康 | AR-4 |
| 表达式族 | ognl + watch 条件表达式 | 直接读对象状态、条件过滤 | AR-5 |
| 火焰图族 | profiler | CPU 热点、分配热点、锁竞争 | AR-6 |

### P2 — 生产中频命令

| 命令 | 场景 |
|----|---------|
| redefine + memorycompiler | 紧急热更新(绕过发版) |
| logger | 线上临时调日志级别(排查时抓现场) |
| sysprop/sysenv/vmoption | 查看/临时修改运行参数 |

### P3 — 低频/支撑命令

| 命令 | 说明 |
|----|---------|
| help / version / session | 支撑 |
| reset | 撤销增强(维护卫生) |
| stop / auth / keymap | 生命周期与安全 |

---

## 03 深度分类 (生产价值视角)

> AR-0 不按源码深度分类(🔴Deep/🟡Working/🟢Surface 是源码分析维度),改为"生产价值 + 面试价值"分类。

### 🔴 必会 (生产刚需 + 面试常问)

| 命令 | 为什么 |
|------|------|
| thread -n / -b | CPU 排查第一步;面试"你线上怎么定位 CPU 高"的标准答案;对应 AR-3 源码 |
| watch / trace | 线上问题定位主力;面试"watch 和 trace 区别";对应 AR-2 源码 |
| jad | 看线上真实代码(发版对不上);面试"arthas 怎么反编译" |
| dashboard | 全局健康一屏;面试"arthas 看什么指标" |
| profiler | CPU 火焰图是高端排查手段;面试加分项;对应 AR-6 |
| ognl | 直接操作线上对象;对应 AR-5 |

### 🟡 常用 (生产偶用/特定场景)

| 命令 | 为什么 |
|------|------|
| jvm / memory / heapdump | 内存问题专项 |
| sc / sm / classloader | 类加载排查(双亲委派问题、版本冲突) |
| tt | 偶发问题现场重放(很强大但使用门槛高) |
| logger / sysprop / vmoption | 临时调整类 |
| stack | 慢方法定位调用来源 |

### 🟢 了解 (低频/支撑)

| 命令 | 为什么 |
|------|------|
| redefine / reset | 热更新高风险,生产慎用;了解机制即可 |
| help / version / session / stop / auth | 支撑性,会用就行 |

### 机制类知识点分类(23 知识点中的 10 个非命令机制)

| 机制 | 分类 | 为什么 |
| 三条 attach 路径 + 参数全家(as.sh) | 🔴 | 每次使用的前提;"怎么挂上 arthas"必答 |
| 隧道/agent-id/密码/disabled-commands | 🔴 | 生产安全配置;架构师视角必知 |
| 四排查场景(CPU高/死锁/OOM/慢接口) | 🔴 | AR-0 的核心产出;面试"你解决过什么问题" |
| stop/reset 清理 | 🟡 | 排查收尾纪律 |
| 组合拳(thread→jad→trace→profiler) | 🟡 | 排查方法论 |
| monitor 定时统计 | 🟡 | 持续观测场景 |
| line 行级定位 | 🟡 | 特定场景("走到哪一步") |
| 各命令 @Name/@Summary 来源 | 🟢 | 写文章时查注解即可 |
| WIKI_HOME 官方文档 | 🟢 | 工具性 |
| heapdump --live-only | 🟡 | OOM 专项 |

---## 04 聚类 — 教学顺序与大纲拆分

> 教学顺序 = 新手路径: 装得上 → 看得见 → 查得到 → 追得到 → 修得到。

### 依赖图

```
01 安装/attach                      ← 无前置(使用域起点)
  ├─ 02 线程排查                    ← 依赖 01 (attach 后才有命令)
  ├─ 03 类与字节码                  ← 依赖 01
  ├─ 04 动态追踪                    ← 依赖 01
  ├─ 05 JVM 与内存                  ← 依赖 01
  └─ 06 表达式+火焰图                ← 依赖 02-05 (组合拳收尾)
```
### 教学顺序

01 安装/attach → 02 线程 → 03 类 → 04 追踪 → 05 JVM/内存 → 06 表达式+火焰图(六篇并列命令族,01 前置,06 收尾组合拳)
### 文章拆分 (6 篇大纲)

| # | 大纲文件 | 主题 | 覆盖命令族 | 核心场景 |
|:--:|------|------|------|------|
| 1 | 01-install-attach.md | 安装/attach/连接/停止 | as.sh 参数全家 | 拿到一台生产机器,10 分钟内挂上 arthas |
| 2 | 02-thread-debug.md | 线程排查 | thread 全命令 | CPU 飙到 100%、线程全部 BLOCKED、死锁 |
| 3 | 03-class-bytecode.md | 类与字节码 | sc/sm/jad/classloader/redefine/reset | 线上代码和本地不一样、类加载异常 |
| 4 | 04-tracing.md | 动态追踪 | watch/trace/stack/tt | 慢接口、参数不对、偶发异常 |
| 5 | 05-jvm-memory.md | JVM 与内存 | jvm/memory/dashboard/heapdump/logger/sysprop | OOM、GC 频繁、全局健康 |
| 6 | 06-express-profiler.md | 表达式与火焰图 | ognl/profiler | 直接读线上对象状态、CPU 热点采样 |

### 每篇大纲的固定结构 (v5 标准)

```
# NN. 悬念式标题 — 场景化主标题
> 🟢 使用域 | 覆盖命令: xxx
> 读者处境: 一句话
### 1. "场景悬念" — 命令 + 参数
场景: 真实线上场景一句话
[命令用法 + 输出解读 + File.java 命令定义位置]
关键设计: [为什么这样设计/参数含义/坑]
[生产注意: ...]
```

### 与源码域的关系(产出物)

| AR-0 篇 | 供哪个源码域做"场景" |
|:--:|------|
| 02-thread-debug | AR-3(ThreadSampler 两次采样、findMostBlockingLock) |
| 04-tracing | AR-2(AdviceListener 回调、OGNL 条件)+ AR-5 |
| 05-jvm-memory | AR-4(Dashboard Timer、Tomcat 轮询) |
| 03-class-bytecode | AR-2(Enhancer 织入/还原) |
| 06-express-profiler | AR-5(OgnlExpress)+ AR-6(ProfilerCommand) |
| 01-install-attach | AR-1(两条 attach 路径、SpyAPI 注入) |
