# 01. profiler start 之后,命令层做了什么? — 参数翻译器

> 🔴 Deep | 9 KP 中的 3 个(命令层核心)
> 读者处境: 你敲 `profiler start --event alloc`,火焰图 30 秒后出来了——中间这 30 秒,arthas 的代码在干什么?答案是: 它只是个"翻译器"。

### 1. "不是实现者,是翻译器" — ProfilerCommand 的本质

场景: arthas 的 profiler 命令 1107 行,但火焰图引擎一个字都没写——它调的是 async-profiler。

- `@Name("profiler")`(monitor200/ProfilerCommand.java:48);命令动作: start/stop/dumpFlat/dumpCollapsed/dumpTraces/execute/actions(:67-70)
- 核心执行: `execute(asyncProfiler, arg)`(ProfilerCommand.java:736-739)= **`asyncProfiler.execute(arg)`**(:739)——把 arthas 参数翻译成 async-profiler 的命令字符串
- **15 个动作**(ProfilerAction 枚举,ProfilerCommand.java:595-604): start/resume/stop/dump/status/meminfo/list/version/load/execute/dumpCollapsed/dumpFlat/dumpTraces/getSamples/actions——`profiler <action>` 的第一个参数映射此枚举
- **35+ 参数全景**(@Option,:302-560): -i interval/-j jstackdepth/-f file/-o format/-e event/--alloc/--live/--lock/--jfrsync/--wall/-t threads/-F features/--signal/--clock/--norm/--sched/--cstack/-d duration/--loop/--timeout/--begin/--end/--title/--minwidth/--reverse/--total/--chunksize/--chunktime/--include/--exclude...——几乎全量透传 async-profiler 能力
- 示例拼串(`executeArgs`,:606-660): `start,event=alloc,file=/tmp/x.html` 形式——start/stop/dump 全走这一个入口;`jfrsync` 参数自动把 format 设为 jfr(:625-627)
- `AsyncProfiler`(`one.profiler.AsyncProfiler`)— async-profiler 项目自带的 Java API,一个 native 方法的薄封装
- 结果: `ProfilerModel` 承接输出(`appendExecuteResult`/`createProfilerModel`),md 格式是 LLM 友好的 Markdown 报告(:60)

关键设计: [模式: 适配器/翻译器(arthas 命令 → async-profiler action 串)+ 门面(对外部系统薄封装)] **委托而非实现**——[JNI: AsyncProfiler 是 async-profiler 的 Java API,内部是 native 方法——命令层到引擎只有一层 JNI 调用,火焰图引擎本身在 .so 里]——arthas 只负责把用户友好的命令翻译成 async-profiler 的 `action,key=value` 串,不重复造轮子。这也是淘汰清单里"async-profiler 模块内部不学"的源码依据: **命令层在 arthas,引擎在 native 库**。

### 2. "自动善后" — start 的文件生成与 stop 的格式转换

场景: `profiler start --timeout 300s` 没给文件——30 秒后文件哪来的?

- start 时: 指定了 `--file` 则记录 `fileSpecifiedAtStart`(:768-773);有 `--timeout` 没 `--file` 则**自动生成输出文件**(:774-782)
- stop 时: 按 `--format` 转换(flamegraph/html/tree/jfr/md...)
- `--duration` 模式: start 后**延时调度 stop**(:798+)
- [bash/脚本视角: 这套"自动文件+延时停止"让 profiler 可以在 CI 里 `profiler start --timeout 30s` 后不管,定时自动出图]

关键设计: **命令组合的便利性**: start 与 stop 是两次独立执行,但 arthas 用"start 时记住状态(file/timeout)"打通了会话——第二次 attach 进同一 JVM,`profiler stop` 能接上之前的采样(状态在命令实例字段)。

### 3. 动作分派全景

```
profiler actions            → 列出支持的动作(ProfilerAction.actions,:750-756)
profiler execute 'xxx'      → 原样透传 execute(:764)
profiler start [--event/--timeout/--duration/--loop/--file] → executeArgs 拼 start(:768)
profiler stop [--format/--file/--threads/--include/--exclude] → executeArgs 拼 stop(:800)
profiler dumpXxx            → 转储(:831)
profiler version            → execute("version=full")(:834)
```

关键设计: **薄命令层**: 每个动作 5-10 行,全是"参数校验 + 拼串 + execute + 包装模型"——没有任何采样逻辑。这让 profiler 命令成为"async-profiler 的一个友好 shell"。**md 是例外**: `--format md` 的 Markdown 报告是 Arthas 侧后处理(拼串时**不透传** file/format 给 async-profiler,executeArgs :632-638 注释: 避免识别失败/输出到文件导致数据丢失)——"翻译器"也有自己的增值输出。

---

跨域桥: 采样 vs 插桩的对比(AR-2 的 ByteKit 插桩 vs 本域采样)= 下一篇;--event/--format 使用 = AR-0 篇 6;命令执行链(注解/参数注入)= AR-2 篇 1。

---

**OpenJDK 关联**:  [OpenJDK 域 32 JFR — outlines/32-jfr/] — 同为采样型工具,JFR 是 JDK 内置实现,async-profiler 是外部实现——两种集成方式的对比。
**另见** [OpenJDK 域 27 JNI — outlines/27-jni/] — AsyncProfiler 是 JNI 桥,一次 native 调用进入 .so。

### 核心悬念

**"翻译器就位——native 那边到底怎么采样?和插桩有什么本质区别?"** — 插桩回答"这次调用花多久",采样回答"CPU 时间整体花在哪"。两种世界观,决定了 arthas 里两种工具的分工。

> → [02-profiler-boundary.md](02-profiler-boundary.md)