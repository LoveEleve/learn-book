# 03. 一图看懂 CPU 去哪了 — 输出格式与火焰图读法

> 🟢 使用域 | 覆盖: 8 种输出格式 / jfr+converter / 读法
> 读者处境: 采样完了,拿到一堆格式——每个格式给谁看?火焰图到底怎么读?

### 1. "八种格式各归其位" — 输出家族

场景: 输出给不同消费方。

- `flamegraph`(默认): 交互式 HTML 火焰图——给人看
- `collapsed`/`traces`/`flat`: 文本栈列表——给脚本/聚合工具
- `tree`: 调用树文本——终端直接看(GettingStarted:55 的一键输出就是它)
- `jfr`: JFR 格式——**给 JMC/Arthas/jfr2flame 消费**(与 JDK 自带 JFR 格式兼容)
- `otlp`: OpenTelemetry 协议——上报可观测平台
- 语法: `asprof stop -o jfr -f out.jfr 8983`;或一键 `-d 30 -f out.jfr`
- [JFR: 二进制事件流格式——async-profiler **自研写入**(AP-5 flightRecorder.cpp,1558 行),不依赖 JDK 的 jdk.jfr 模块]

关键设计: **格式即消费方**: 人看→flamegraph,机器看→collapsed,生态看→jfr/otlp——同一份采样数据,输出时按需转换。与 Arthas 对照: Arthas `--format html|jfr|md`(AR-6 篇 1)透传的正是这些格式,`md` 是 Arthas 自己的增值(LLM 友好)。

### 2. "服务器上没有浏览器" — jfr + converter 离线分析

场景: 生产服务器没浏览器,采样后拿回本地分析。

- 服务器: `asprof -d 60 -f dump.jfr 8983`(采集 jfr)
- 本地: `java -cp converter.jar jfr2flame dump.jfr dump.html`(docs/ConverterUsage.md)或 JMC 打开
- converter(Java,src/converter/ 51 文件)支持: JFR→flamegraph/collapsed/tree/热力图
- [Java: converter 是独立 Java 程序,解析 JFR 事件流(AP-5 的 Java 侧)——服务器只跑 C++ 采样器,本地跑 Java 转换器,职责分离]

关键设计: **采集与渲染分离**: C++ 侧(快、无依赖)采集,Java 侧(rich 生态)渲染——这也是 async-profiler 的部署形态: 目标机只放 .so,转换工具放分析机。

### 3. "火焰图三问" — 读法

场景: HTML 火焰图打开,一片彩色——怎么看?

- **x 轴** = 采样次数占比(块越宽 = 占 CPU 越多,**不是时间线**);**y 轴** = 栈深(下→上: 调用者→被调用者)
- 第一眼: 顶部最宽的块 = 热点函数;顺栈往下 = 热点调用链
- 颜色: 黄=Java,绿=JIT 编译,native 灰/红——一眼区分"自己的代码 vs 库"
- 交互: 点击块下钻、搜索函数、`[inlined]` 帧 = JIT 内联(火焰图独有视角)
- 与 Arthas 完全同读法(AR-0 篇 6 §2)

关键设计: 火焰图是**采样的可视化**(伯努利频度估计,AR-6 篇 2)——宽块≠总耗时,是"被采样命中的比例";`[inlined]` 帧暴露 JIT 内联——这是 jstack/thread dump 永远看不到的视角。

生产注意: 火焰图顶部如果是库代码(GC/编译线程),方向转内存/JIT;`--exclude` 去噪声后重采对比更清晰。

---

跨域桥: flamegraph 生成 = AP-5(flameGraph.cpp);JFR 自研写入 = AP-5(flightRecorder.cpp + jfrMetadata.cpp);converter = AP-5(Java 侧);Arthas md 格式 = Arthas AR-6 篇 1(后处理设计)。

**OpenJDK 关联**: [域 32 JFR — outlines/32-jfr/] — jfr 输出格式与 JDK JFR 的兼容性;JMC 是第一读者。
