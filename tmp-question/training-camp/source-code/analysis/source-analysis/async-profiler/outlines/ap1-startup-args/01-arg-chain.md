# 01. 一条 asprof 命令,参数走了几层? — 两层参数解析

> 🔴 Deep | 12 KP 中的 3 个(两层解析/PMU event/参数串拼接)
> 读者处境: 你敲 `asprof -e cpu -d 30 -f out.html 8983`——从命令行到引擎配置,参数经过了几层转换?答案: 两层,而且第二层解析器就是 Arthas 的 execute 协议。

### 1. "第一层: 命令行到逗号串" — main() 解析

场景: 用户友好的命令行参数,要变成 agent 认识的格式。

- `main()`(src/main/main.cpp:415): `Args args(argc, argv)` 循环解析(:417)
- 动作识别: start/resume/stop/dump/status/metrics/list/**collect**(:420-421,默认动作 :223)vs **jattach action** load/jcmd/threaddump/dumpheap/inspectheap(:424-426,置 `jattach_action=true`)
- 选项直解: `-d` 时长(:438-439)/`-f` 文件(:441-442)/`-o` 格式(:444-445)/`-e` 事件(:447-457)/`-i` 间隔(:461)/`-j` 栈深(:464)/`-t` 线程(:467)
- **参数串拼接**: 解析结果 `params << ",event=...,interval=..."`(:450-461)——**拼成 agent 认识的逗号参数串**
- `-h/-v`: 帮助/版本(:432-440)

关键设计: **asprof 是"翻译器"**: 命令行(`-e cpu`)→ 逗号串(`,event=cpu`)→(jattach 加载)→ agent 内再解析。**这个逗号串格式,就是 Arthas `execute("start,event=cpu")` 发的那条协议**(AR-6 篇 1)——Arthas 直接跳过了第一层,把字符串发给 native。

### 2. "第二层: 逗号串到 CASE" — arguments.cpp parse

场景: 逗号参数串进到 agent 里,怎么变成配置?

- `Arguments::parse(const char* args)`(src/arguments.cpp:41): `strtok(args_copy, ",")` 按逗号切分(:56)→ `strchr(arg, '=')` 取键值(:57-58)→ `SWITCH CASE`(:62-285)
- [C++: strtok 原地切分(修改缓冲区)——逗号分隔的 `action,key=value` 语法;SWITCH 是宏封装的多分支]
- CASE 表覆盖: 8 动作(:62-84)/输出格式(:87-141)/事件(:147-211)/采样参数(:238-285)
- 结果写入 `Arguments` 结构(_action/_event/_interval/_file...)

关键设计: **解析与执行分离**: parse() 只填结构,不启动任何引擎——`Arguments` 是"配置快照",谁消费谁负责后续(AP-2 的 Engine 从它取参数)。这保证了 execute/命令行/嵌入式 API(asprof_execute,asprof.cpp:27-40 内部也是 `args.parse(command)`)三条入口走同一解析器。

### 3. "PMU 事件的特殊通道" — 降权前的解析

场景: `-e cpu/umask=0x1,event=0xd3/` 这种硬件事件怎么进?

- **PMU 格式**: `cpu/umask=0x1,event=0xd3/`——main.cpp:441-443 把事件内逗号换成冒号(`event=0xd3:umask=0x1`),避免与参数分隔符冲突
- **tracepoint 解析**: `get_tracepoint_id("tracing", event)`(main.cpp:387/:452-453)——把 tracepoint 名字解析成 id
- **关键时序**: 注释与实现显示——**tracepoint 解析发生在降权之前**(root 才能读 /sys/kernel/debug/tracing),解析成 id 后 agent 无需 root
- [Linux: tracepoint 是内核静态插桩点(/sys/kernel/debug/tracing/events/);perf_event_open 可用 tracepoint id 作为事件源——解析一次,后续免权限]

关键设计: **root 权限最小化**: asprof 启动时短暂用 root 解析 tracepoint(或读内核接口),随后 **drop privileges**——agent 在普通权限下运行,安全边界清晰。这解释了"为什么 PMU 事件要预处理"。

### 4. 链路总览

```
asprof -e cpu -d 30 -f out.html 8983
  → main.cpp:415 动作/选项解析 → params=",event=cpu,duration=30,file=out.html"
  → run_jattach: jattach(pid, {"load", libpath, cmd})    [进入目标 JVM]
  → arguments.cpp parse(): strtok(",") → CASE → Arguments 结构
  → (AP-2) Engine 从 Arguments 取配置启动采样
```

关键设计: 这条链的本质是**"两套语法,一个协议"**: 命令行语法(asprof 友好)与逗号串语法(agent 内部)之间,靠 jattach 的 load 动作衔接;Arthas 用户敲的命令,直接落在第二段。

---

跨域桥: 逗号串协议 = Arthas AR-6 篇 1(executeArgs 拼串)与 AP-0 篇 4(协议对照);CASE 枚举表 = AP-0 篇 2(事件/参数);parse 产物 Arguments = 下一篇。

**OpenJDK 关联**: [域 40 Launcher — outlines/40-launcher/] — JVM 自身启动参数处理(libjli→JavaMain)与 asprof 参数链的对照。
