# 01. 5 分钟挂上采样器 — 构建与 Attach

> 🟢 使用域 | 覆盖: make 构建 / 三种 pid / start-stop
> 读者处境: 你已会用 Arthas 的 profiler 命令——现在直接操作它的"本体" asprof,5 分钟出第一张火焰图。

### 1. "make 一下就有" — 构建与 asprof

场景: 从源码构建这个 C++ 项目,得到命令行工具。

- 构建: `make`(README: How to build)——产物 `build/bin/asprof`;依赖 gcc 7.5+/静态 libstdc++ + JDK 11+
- 平台: Linux x64/arm64 官方支持(README: Supported platforms)
- [C++: Makefile 驱动;静态链接 libstdc++——asprof 要在各种 Linux 上独立运行,不依赖系统动态库]

关键设计: **静态 libstdc++ 是"自包含"的代价**: 采样器要 attach 到任意 JVM 进程,自身不能依赖目标机的动态库版本——静态链接牺牲体积换兼容(与 Arthas 把依赖重定位进 jar 是同一哲学,只是 C++ 版)。

### 2. "三种 pid 写法" — attach 方式

场景: 目标 JVM 在跑,怎么指定它?

- 数字 pid: `asprof start 8983`(GettingStarted:42)
- `jps` 自动: 系统只有一个 Java 进程时,`asprof start jps` 自动找(GettingStarted:48-49)
- 应用名: 按 `jps` 输出中的应用名,如 `asprof -d 30 Computey`(GettingStarted:53)
- 停止: `asprof stop 8983`(:43)

关键设计: **attach 由 asprof 自己完成**(自带 jattach 实现,src/jattach/)——不需要目标 JVM 安装任何东西,也不需要 JDK 的 tools.jar。对比 Arthas: Arthas 用 JDK 的 `VirtualMachine.attach`(AR-1),asprof 是**自研 attach 协议**(AP-1 源码域将深挖)。

### 3. "start 到 stop 的完整周期" — 基本流程

场景: 采样 30 秒,拿输出。

- 一键: `asprof -d 30 8983`(GettingStarted:55)——30 秒后自动停止,控制台输出调用树
- 分步: `start` → 跑业务 → `stop`(可加 `-o flamegraph -f out.html`)
- 状态: `status` 查看是否在采;`resume` 续采;`metrics` 看计数
- 与 Arthas 对照: asprof 的动作(8 个)是 Arthas profiler 动作(15 个)的**子集**——Arthas 多了 load/execute/dumpFlat 等包装(AR-6)

生产注意: 采样期间有轻微开销(默认 10ms 间隔);`-d` 限时是 CI 场景首选。

---

跨域桥: attach 协议细节 = AP-1(src/jattach/ + launcher);动作枚举 = AP-1(arguments.cpp:62-84);Arthas 侧对照 = Arthas AR-6 篇 1(15 动作)。

**OpenJDK 关联**: [域 36 Attach — outlines/36-attach/] — 自研 jattach 与 JDK Attach API 同协议不同实现(AP-1 篇 3 深挖)。
