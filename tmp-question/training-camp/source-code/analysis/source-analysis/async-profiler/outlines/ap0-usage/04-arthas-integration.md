# 04. 从 Arthas profiler 到 asprof — 衔接与生产场景

> 🟢 使用域 | 覆盖: execute 协议对应 / 四场景映射 / 容器注意
> 读者处境: 你会 Arthas 的 profiler——现在理解 asprof 是它的"真身",并完成两种工具的场景切换。

### 1. "同一条 execute 协议" — 命令级对照

场景: Arthas 里敲的 profiler 命令,底层就是 asprof 的 API。

- Arthas `profiler start --event alloc`(AR-6 篇 1)→ executeArgs 拼 `"start,event=alloc"` → `AsyncProfiler.execute("start,event=alloc")` → **native 侧解析的就是 arguments.cpp 的同一套 CASE 枚举**(arguments.cpp:62-211)
- 完整对照:
  - Arthas `--event cpu|alloc|lock|wall` = asprof `-e`(arguments.cpp:147-211)
  - Arthas `--duration` = asprof `-d`;`--file` = `-f`;`--format` = `-o`(arguments.cpp:238-285)
  - Arthas `--timeout`/`--loop` = asprof 同参数(arguments.cpp:164-173)
- 差异: Arthas 是"命令包装"(15 动作,md 后处理),asprof 是"原生"(8 动作,全格式)

关键设计: **字符串协议是唯一契约**: [C++: `execute("action,key=value,...")` 是 native 侧 strtok 按逗号切分 + strchr 按等号取值的解析(arguments.cpp:55-61)——Arthas 通过它驱动 native,不依赖任何 Java 侧对象]Arthas 通过 `execute("action,key=value,...")` 字符串驱动 native——这个协议的解析器就是 arguments.cpp(AP-1)。学 asprof = 学 Arthas profiler 的"另一半"。

### 2. "四类问题两套工具" — 场景映射

场景: 同一问题,Arthas 与 asprof 各是什么姿势?

| 问题 | Arthas 姿势 | asprof 姿势 |
|---|---|---|
| CPU 高 | `thread -n 3` → `profiler start --event cpu` | `asprof -e cpu -d 30 -f cpu.html` |
| 内存增长 | `memory`/`heapdump` + `profiler --event alloc` | `asprof -e alloc -d 30 -f alloc.html` |
| 锁竞争 | `thread -b` + `profiler --event lock` | `asprof -e lock` |
| 阻塞/慢 | `trace`/`watch` + `--event wall` | `asprof -e wall -i 5ms` |

关键设计: Arthas 强在**方法级插桩**(watch/trace 拿参数),asprof 强在**低成本全景**(采样零侵入可长跑)——**先 asprof 全景定位,再 Arthas 插桩下钻**是最佳组合(与 AR-0 篇 6 §3 的组合拳一致)。

### 3. "容器与安全" — 生产注意

场景: K8s 里采样、权限受限。

- **容器 CPU 配额**: perf_events 可能不可用(无 perf_event_open 权限)→ 换 `-e itimer`(软件定时器,docs/CpuSamplingEngines.md)——AP-2 的引擎选择在此有实际意义
- 容器内 attach: 目标 JVM 与 asprof 需**同一 PID namespace**(docker 内跑 asprof 或共享 pid ns)
- 安全: 采样器 attach 有 JVM 内代码执行能力——生产限制普通用户使用;`--memlimit` 防采样器自身 OOM

生产注意: 容器问题排查看 docs/ProfilingInContainer.md;itimer 精度低于 perf_events,但权限门槛低——先试 itimer,再升级。

---

跨域桥: 字符串协议解析 = AP-1(arguments.cpp);引擎选择 = AP-2(engine.cpp);Arthas 命令层 = Arthas AR-6 篇 1(executeArgs 拼串)。

**OpenJDK 关联**: [域 27 JNI — outlines/27-jni/] — execute 字符串协议穿越的 JNI 层。
