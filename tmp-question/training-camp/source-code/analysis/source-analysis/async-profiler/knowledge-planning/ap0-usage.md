# 域 AP-0: async-profiler 使用与火焰图 — 知识规划

> 性质: 🟢 使用实操域(非源码分析)——读者已会 Arthas profiler(AR-6),本域建立 async-profiler 本体的"手感"
> 提取来源: README.md + docs/(GettingStarted/ProfilerOptions/OutputFormats/FlamegraphInterpretation) + src/arguments.cpp(动作/格式/事件枚举)
> 提取日期: 2026-08-10
> 产出: 使用手册式大纲 + 与 Arthas 衔接点,作为 AP-1~AP-6 的场景素材

## 01 逐源提取 (命令定义 → 功能与场景)

| Source (asprof/文档) | Inferred Usage (功能与场景) | Confidence |
|------------|-------------------|------------|
| README: How to build + GettingStarted:29-42 | **构建与 attach**: `make` → `build/bin/asprof`;`asprof start <pid>`(:42)/`asprof stop <pid>`(:43)——**attach 由 asprof 自身完成**(自带 jattach,无 JDK 依赖) | High |
| GettingStarted:48-53 | **pid 三种写法**: 数字 pid / `jps` 自动查找(单进程时)/ 应用名(如 `Computey`) | High |
| GettingStarted:55-59 | **一键采样**: `asprof -d 30 8983`——30 秒 CPU 采样后控制台输出调用树;`-f /tmp/flamegraph.html`(:102)输出文件 | High |
| arguments.cpp:62-84 | **8 个动作**: start/resume/stop/dump/status/metrics/list/version——与 Arthas profiler 的 15 动作对应(子集) | High |
| arguments.cpp:87-141 | **输出格式**: collapsed/flamegraph/tree/jfr/jfropts/jfrsync/traces/flat/otlp/samples——按需转换 | High |
| arguments.cpp:147-211 | **事件类型**: event=cpu(默认)/alloc/tlab/nativemem/nofree/trace/lock/nativelock/wall/proc/all——与 Arthas `--event` 同源 | High |
| arguments.cpp:238-285 | **采样参数**: interval(默认 10ms)/jstackdepth/signal/features/file/log/loglevel/quiet + timeout/loop/memlimit(:164-176) | High |
| docs/CpuSamplingEngines.md | **CPU 采样引擎**: perf_events(硬件计数器)/itimer(软件定时器)——不同容器环境的取舍 | Medium |
| docs/FlamegraphInterpretation.md | **火焰图读法**: x 轴=采样占比/y 轴=栈深/颜色=类型——与 Arthas AR-0 篇 6 同一读法 | Medium |
| docs/OutputFormats.md + ConverterUsage.md | **输出转换**: jfr 采集 → `jfr2flame`/converter 转 HTML——离线分析流程 | Medium |
| docs/ProfilingModes.md | **采样模式**: CPU/Allocation/Lock/Wall——各模式适用场景 | Medium |

*12 个知识点(命令功能)*

---

## 02 聚合 — 功能族分类

### P1 — 核心流程族

| 命令族 | 成员 | 场景 |
|----|---------|---------|
| attach 族 | asprof start/stop <pid>、jps/应用名、resume | 开始/暂停/恢复采样 |
| 事件族 | -e cpu/alloc/lock/wall + -d/-f/-i | 按问题类型选事件 |
| 输出族 | flamegraph/collapsed/tree/jfr/otlp | 按消费方选格式 |

### P2 — 进阶功能

| 功能 | 场景 |
|----|---------|
| -i interval / --signal | 调采样率与信号(容器限制) |
| --timeout/--loop | CI 自动化(定时自动停) |
| jfr + converter | 采集后离线转 HTML(服务器无浏览器) |
| --include/--exclude 过滤 | 聚焦业务代码 |

### P3 — 支撑

| 功能 | 说明 |
|----|---------|
| status/metrics/list/version | 查看状态 |
| --log/--loglevel | 调试 |
| tlab/nativemem/nofree | 高级内存事件 |

---

## 03 深度分类 (生产价值视角)

### 🔴 必会 (生产刚需 + 与 Arthas 衔接)

| 功能 | 为什么 |
|------|------|
| asprof start/stop + -d/-f | 采样主流程;Arthas profiler 的参数同源 |
| -e cpu/alloc/lock/wall | 四事件对应四类问题(AR-6 已讲概念,此处实操) |
| 火焰图读法 | 排查输出解读;与 AR-0 篇 6 完全一致 |
| jfr + converter | 生产离线分析流程 |

### 🟡 常用 (特定场景)

| 功能 | 为什么 |
|------|------|
| -i interval 调采样率 | 高负载调整 |
| --timeout/--loop | CI 集成 |
| --include/--exclude | 聚焦热点 |
| perf_events vs itimer 引擎选择 | 容器/VM 环境 |

### 🟢 了解 (支撑)

| 功能 | 为什么 |
|------|------|
| status/metrics/list | 状态查询 |
| tlab/nativemem 高级事件 | 专项 |
| resume/partial dump | 高级流程 |

---

## 04 聚类 — 教学顺序与大纲拆分

> 教学主线: 装得上 → 采得到 → 看得懂 → 接得上(Arthas)。

### 文章拆分 (4 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 |
|:--:|------|------|------|
| 1 | 01-build-attach.md | 构建与 attach | make/三种 pid/start-stop/resume/status |
| 2 | 02-events-options.md | 事件与参数 | -e 四事件/-d/-f/-i/--timeout/--loop/过滤 |
| 3 | 03-output-flamegraph.md | 输出与火焰图读法 | 8 种格式/jfr+converter/读法/颜色语义 |
| 4 | 04-arthas-integration.md | 与 Arthas 衔接与生产场景 | execute 协议对应/四排查场景映射/容器注意 |

### 与源码域的关系

| AP-0 篇 | 供哪个源码域做"场景" |
|:--:|------|
| 01-build-attach | AP-1(launcher/jattach/attach 协议) |
| 02-events-options | AP-2(引擎选择/事件解析) |
| 03-output-flamegraph | AP-5(flameGraph/flightRecorder/converter) |
| 04-arthas-integration | AP-6(Java API)+ 全部域(总衔接) |
