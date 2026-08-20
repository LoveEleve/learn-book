# 域 AP-1: 启动与参数解析 — 知识规划

> 源码路径: src/main/main.cpp(610行) + src/arguments.cpp/h(602行) + src/asprof.cpp + src/jattach/ + src/fdtransfer.h + src/main/fdtransferServer_linux.cpp + src/launcher/
> 源码量: ~10 文件,核心 ~1800 行
> 提取日期: 2026-08-10
> 前置域: AP-0(使用——本域解释 asprof 命令背后的机制)

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| main.cpp:415-470 main() | **两层参数解析(第一层)**: 命令行 `-d/-f/-o/-e/-i/-j/-t/-s` 直接解析(:423-470);动作识别 start/resume/stop/dump/status/metrics/list/collect(:423-426)vs **jattach action** load/jcmd/threaddump/dumpheap/inspectheap(:424-426,置 jattach_action=true);`-h/-v` 帮助与版本(:432-440) | High |
| main.cpp:470-560(推断) | **参数串拼接**: 解析结果拼成 `",event=xxx,interval=xxx"` 逗号参数串(params <<)——**与 Arthas execute 协议同源**(Arthas AR-6 拼的就是这个) | High |
| main.cpp:436-456 | **PMU event 特殊处理**: `cpu/umask=0x1,event=0xd3/` 格式(冒号替换 :441-443)+ **tracepoint id 解析**(`get_tracepoint_id("tracing", event)` :445-452)——**在降权前解析**(注释: 解析 tracepoint 需要 root,先解析再 drop privileges) | High |
| main.cpp:365-373 run_jattach | **jattach 调用**: `argv[] = {"load", libpath, ..., cmd}` → `jattach(pid, 4, argv, 0)`(:373)——asprof 的一切动作最终都是 **jattach 的 load action**(把 agent 库和参数串加载进目标 JVM) | High |
| arguments.cpp:41-60 parse() | **两层参数解析(第二层)**: `strtok(args_copy, ",")` 按逗号切分 → `strchr(arg, '=')` 取键值 → SWITCH CASE(:62-285)——**native 侧的 execute 协议解析器** | High |
| arguments.cpp:62-285 CASE 表 | **动作/格式/事件/选项全枚举**: 8 动作(:62-84)/8+ 格式(:87-141)/事件(:147-211)/参数(:238-285)——AP-0 已列表,此处是解析实现 | High |
| arguments.h:30-96 | **7 个枚举**: Action/Counter/Style/CStack/Clock/Output/JfrOption/EventCategory——CStack 注释"更新枚举须同步 FlightRecorder"(:57) | High |
| arguments.h:150-216 | **Arguments 结构**: 全部字段(_action/_event/_timeout/_loop/_interval/_alloc/_lock/_wall/_jstackdepth/_signal/_file/_log/_include/_exclude/_threads/_features/_output/_file_num...)— 解析结果的结构化承载 | High |
| arguments.cpp:529-563 | **单位与超时解析**: `parseUnits`(:529,支持 `10ms`/`1s`/`500us` 单位后缀)/`parseTimeout`(:553,支持相对/绝对时间) | Medium |
| fdtransfer.h + fdtransferServer_linux.cpp:134 | **fd 传递(权限桥)**: "mapping perf fds may require privileges, and **fdtransfer has them while the target application does not**"(:134)——**asprof 采样进程把已映射的 perf fd 传给目标应用**,绕过目标应用的权限限制 | High |
| jattach/(psutil.h + 实现) | **自研 attach**: 不依赖 JDK——psutil 找进程 + attach 协议(与 Arthas 的 VirtualMachine.attach 对照) | High |
| asprof.cpp:20-40 | **C API 导出(嵌入式接口)**: `asprof_init/asprof_execute(asprof_writer_t output_callback)/asprof_error_str`——DLLEXPORT 的 C 接口,`asprof_execute` 内部 `args.parse(command)` → `Profiler::instance()->runInternal(args, out)`;供其他工具(C/C++/JNI)嵌入式集成,不走命令行 | High |

*12 个知识点*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

| KP | 出现文件 | 说明 |
|----|---------|------|
| 参数全链路(命令行→参数串→parse→Arguments) | main.cpp, arguments.cpp, arguments.h, run_jattach, jattach | 三层转换,最终进 Arguments 结构 |
| attach 与加载 | main.cpp(run_jattach), jattach/, fdtransfer*, launcher/ | attach → load agent → fd 传递 |

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| 枚举体系 | arguments.h(7 枚举), arguments.cpp(CASE) |
| PMU/tracepoint 特殊处理 | main.cpp, get_tracepoint_id |
| 单位/超时解析 | arguments.cpp(parseUnits/parseTimeout) |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| fdtransfer 权限桥 | fdtransfer.h + fdtransferServer_linux.cpp |
| jattach 自研 | jattach/ |

---

## 03 深度分类

### 🔴 Deep (教学重点)

| KP | 为什么 |
|----|------|
| 两层参数解析(命令行 → 逗号串 → CASE) | "asprof 参数怎么变成引擎配置"的完整答案;**与 Arthas execute 协议同源**——衔接 AR-6 |
| jattach 自研 attach | "asprof 为什么不需要 JDK"——vs Arthas VirtualMachine.attach 的对照 |
| fdtransfer 权限桥 | 采样器设计的高级点: perf fd 跨进程传递绕过权限——生产容器场景的关键 |
| PMU event/tracepoint 解析 | 降权前解析的时序设计——root 权限的最小化使用 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| Arguments 结构与 7 枚举 | 数据承载,用时可查 |
| 单位/超时解析 | 解析细节 |
| run_jattach 的 argv 拼装 | load action 的约定 |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| launcher/ 细节 | 构建产物相关 |
| C API 导出(asprof_execute) | 嵌入式集成接口——非命令行入口 |
| 参数串拼接(params <<) | 第一层解析的产物,execute 协议的中介 |
| CASE 解析表 | parse 的实现主体——动作/格式/事件全覆盖 |
| parse() 解析器(execute 协议落地) | 三条入口(命令行/execute/JNI)共享的唯一解析器 |

---

## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 一条 asprof 命令从回车到引擎启动——参数怎么走、agent 怎么进、权限怎么解决。

### 依赖图

```
01 两层参数解析                   ← 无前置
  ├─ 02 Arguments 结构与枚举      ← 依赖 01 (parse 的产物)
  └─ 03 jattach 与 fdtransfer     ← 依赖 01 (run_jattach 是终点) + 02 (参数串)
```

### 教学顺序

```
01 参数链路(命令行 → 逗号串 → CASE 解析)
  → 02 参数承载(Arguments/枚举/单位解析)
    → 03 attach 与权限(自研 jattach + fdtransfer 权限桥)
```

### 文章拆分 (3 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-arg-chain.md | 两层参数解析 | main() 解析/PMU event/参数串拼接/parse() CASE |
| 2 | 02-arguments-struct.md | 参数承载与枚举 | 7 枚举/Arguments 字段/parseUnits/parseTimeout |
| 3 | 03-attach-fdtransfer.md | attach 与权限桥 | 自研 jattach/run_jattach/fdtransfer 权限传递/与 Arthas attach 对照 |

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "Arthas 的 execute('start,event=cpu') 和 asprof 命令是什么关系?" | 同一协议的两端(01 篇) |
| "asprof 为什么不带 JDK 也能 attach?" | 自研 jattach(03 篇) |
| "为什么目标应用没权限 map perf buffer,却能用 perf 采样?" | fdtransfer 权限桥(03 篇) |
