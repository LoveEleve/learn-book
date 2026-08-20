# 域 AR-6: Profiler 火焰图 — 知识规划

> 源码路径: core/command/monitor200/ProfilerCommand.java(1107行) + one.profiler.AsyncProfiler(外部依赖 async-profiler 的 Java API)
> 源码量: 1 文件 1107 行(命令层)+ 外部 native 库(不深入)
> 提取日期: 2026-08-10(v2 深审补充: 机制 6→9)
> 前置域: AR-0 篇 6(profiler 使用)/AR-2(采样 vs 插桩的对比视角)
> 边界: 只学命令调用层;async-profiler 模块内部(native perf_events)淘汰

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| ProfilerCommand.java:48-73 | **命令声明**: `@Name("profiler")`;支持 start/stop/dumpFlat/dumpCollapsed/dumpTraces/execute/actions 等 action;参数 `--event`(cpu/alloc/lock/wall)/`--timeout`/`--loop`/`--duration`/`--file`/`--format`(flat[=N]/traces[=N]/collapsed/flamegraph/tree/jfr/md[=N])/`--threads`/`--include`/`--exclude`/`--jfrsync`(:372) | High |
| ProfilerCommand.java:551-585 profilerInstance() | **native 库加载**: `AsyncProfiler.getInstance(libPath)`(:580)——从 arthas 目录定位 libasyncProfiler;加载前**复制到临时文件**(:562-575,注释: 避免多次 attach 时 "Native Library already loaded in another classloader");仅 Linux/Mac(:583-588,其他 OS 报不支持) | High |
| ProfilerCommand.java:736-739 execute() | **命令执行封装**: `asyncProfiler.execute(arg)`(:739)——把 arthas 参数拼成 async-profiler 命令字符串("start,event=cpu,file=..." 形式,:786/800/831 executeArgs) | High |
| ProfilerCommand.java:747- process() | **动作分派**: actions(列出)/execute(原样透传)/start(自动生成文件 :768-782)/stop(--format 转换)/version=full(:834) | High |
| ProfilerCommand.java:105-110 + :372 jfrsync | **JFR 同步**: `--jfrsync` 与 JFR 同时录制;输出 jfr 格式可直接用 JMC 分析 | Medium |
| 输出解析: appendExecuteResult/createProfilerModel | **结果模型**: ProfilerModel 承接执行输出;md 格式为 LLM 友好输出(:60) | Medium |

| ProfilerCommand.java:595-604 ProfilerAction | **15 个动作枚举**: start/resume/stop/dump/status/meminfo/list/version/load/execute/dumpCollapsed/dumpFlat/dumpTraces/getSamples/actions——命令的第一个参数(action)映射此枚举 | High |
| ProfilerCommand.java:606-660 executeArgs() | **拼串逻辑**: `action,event=...,alloc=...,live,lock=...,jfrsync=...,file=...,format=...`(:606-660)——**md 格式是 Arthas 侧后处理,不透传 async-profiler**(:632-638 注释,避免识别失败/数据丢失) | High |
| ProfilerCommand.java:302-560 参数全景 | **35+ 参数**: action/actionArg + -i interval/-j jstackdepth/-f file/-o format/-e event/--alloc/--live/--lock/--jfrsync/--wall/-t threads/-F features/--signal/--clock/--norm/--sched/--cstack/-d duration/--loop/--timeout/--begin/--end/--title/--minwidth/--reverse/--total/--chunksize/--chunktime/--include/--exclude...——几乎全量透传 async-profiler 的能力 | Medium |

*9 个知识点(v1 6 → 补充 3)*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

无(本域单文件命令层)

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| 命令参数 → async-profiler 命令串 | ProfilerCommand.executeArgs(:606-660)+ AsyncProfiler.execute;35+ @Option 映射 |
| 动作分派 | ProfilerAction 枚举(15 动作), process() 各分支 |
| native 库加载与平台限制 | profilerInstance + OSUtils |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| 全部机制 | ProfilerCommand.java(单文件域) |

---

## 03 深度分类

### 🔴 Deep (教学核心)

| KP | 为什么 |
|----|------|
| 命令层 = async-profiler 的"参数翻译器" | "profiler 命令怎么工作的"——15 动作 + 35 参数拼串;md 后处理设计;面试"火焰图怎么采的"引到 native 边界 |
| native 库加载策略(临时文件复制) | 多次 attach 的坑(Native Library already loaded);理解 Java↔native 边界 |
| 采样 vs 插桩 | 与 AR-2 的插桩(ByteKit)对比——两种观测技术本质差异,面试高频 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| 参数全家(start/stop/dump/format) | AR-0 篇 6 已覆盖使用 |
| JFR 同步 | 了解存在即可 |
| 命令声明(@Name+参数) | 为什么: 1107 行命令层入口 |
| 结果模型(ProfilerModel) | 为什么: 输出承接 |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| 输出解析 | 实现细节 |

---

| 动作分派细节 | 为什么: 各分支 5-10 行 |## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 命令怎么把参数变成采样动作(命令层)→ 采样背后是什么(native 边界与原理概述)。

### 依赖图

```
01 Profiler 命令调用层              ← 无前置
  └─ 02 native 边界与原理           ← 依赖 01 (execute 触发 native 采样)
```
### 教学顺序

01 命令调用层(怎么翻译)→ 02 native 边界(怎么采样)
### 文章拆分 (2 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-profiler-command.md | 命令调用层 | ProfilerAction 15 动作/35+ 参数全景/executeArgs 拼串(含 md 后处理)/execute/结果模型 |
| 2 | 02-profiler-boundary.md | native 边界与原理 | 库加载(临时文件)/平台限制/采样 vs 插桩/火焰图读法 |

### 文章内机制顺序

```
01: ProfilerAction 15 动作 → 35+ 参数 → executeArgs 拼串 → AsyncProfiler.execute → 结果模型
02: profilerInstance 库加载(临时文件) → 平台限制 → 采样 vs 插桩 → 火焰图语义
```

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "arthas 的火焰图是它自己实现的吗?" | 不——是 async-profiler 的 Java API 调用(命令层 = 翻译器) |
| "为什么 profiler 不插桩却没有侵入?" | 采样(perf_events)——与 AR-2 的插桩对比 |
| "为什么反复 attach 不会报 Native Library already loaded?" | 加载前复制到临时文件 |
