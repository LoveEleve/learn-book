# 03. 不带 JDK 的 attach 与跨进程权限桥 — 自研 jattach 与 fdtransfer

> 🔴 Deep | 12 KP 中的 4 个(jattach/run_jattach/fdtransfer/权限桥)
> 读者处境: asprof 不需要 JDK 就能 attach 任意 JVM——它自己实现了 jattach。更妙的是: 目标应用没权限 map perf buffer,却能用 perf 采样。

### 1. "不依赖 JDK 的 attach" — 自研 jattach

场景: Arthas 用 `VirtualMachine.attach`(JDK API),asprof 呢?

- `run_jattach`(main.cpp:365-373): `argv[] = {"load", libpath, ..., cmd}` → `jattach(pid, 4, argv, 0)`(:373)——**一切动作最终都是 jattach 的 load action**
- `jattach` 是 `extern "C"` 声明的自研实现(src/jattach/,:115)——包含 psutil(进程查找)与 attach 协议
- 对照: Arthas 的 attach 走 JDK `VirtualMachine.attach`(AR-1 篇 1)——asprof 是**从零实现 attach 协议**(信号+SOCKET,与 OpenJDK 域 36 Attach 的 JDK 实现同协议不同代码)
- [Linux: JVM attach 协议——SIGQUIT 探测 + /tmp 下 unix socket 握手 + loadAgent 指令;asprof 的 jattach 实现了这套协议的客户端侧]

关键设计: **自包含的代价与收益**: 不依赖 JDK 的 tools/attach API,意味着 asprof 可以在任何 JVM 上 attach(甚至 OpenJ9),且构建产物只有一个二进制——这正是"静态 libstdc++"(AP-0 篇 1)的延续: **能自己实现就不依赖外部**。

### 2. "fd 传递: 权限的桥" — fdtransfer

场景: 目标应用是非 root 启动,没有权限 `perf_event_open` 或 map perf buffer——perf 采样怎么工作?

- `run_fdtransfer(pid, fdtransfer)`(main.cpp:605)——start/resume 时启动 fdtransfer 服务
- `fdtransferServer_linux.cpp:134` 注释直说: "**Map the perf buffer here (mapping perf fds may require privileges, and fdtransfer has them while the target application does not)**"
- 机制: asprof(有权限)创建并 map 好 perf buffer → 通过 **fd 传递**(unix socket + SCM_RIGHTS 发送文件描述符)把 fd 交给目标应用 → 目标应用直接采样,无需权限
- [Linux: SCM_RIGHTS 是 unix socket 的"文件描述符传递"机制——接收方获得同一文件的内核引用;perf buffer 的 map 是一次性的,map 后读写不需要特权]

关键设计: **权限最小化的极致**: 不降权(AP-1 篇 1 的 tracepoint 时序),而是**把有权限的结果传递出去**——asprof 进程短暂持有权限完成初始化,目标应用无权限运行。这是采样器在容器/安全环境跑起来的核心设计。

### 3. "三件事串起来" — attach 完整流程

```
asprof -d 30 8983
  → main.cpp 解析参数,拼逗号串
  → run_jattach: jattach(8983, load, agent.so, "duration=30,...")
  → 目标 JVM 加载 agent,arguments.cpp parse 逗号串
  → main.cpp:605 run_fdtransfer(8983) —— 有权限时开 fdtransfer
  → (AP-2) agent 用 fdtransfer 传来的 fd 采样,或自行 perf_event_open
```

关键设计: attach(进 JVM)+ fdtransfer(进权限)是两个独立通道——attach 是"控制通道"(agent 加载、命令),fdtransfer 是"数据通道"(perf fd);权限只花在初始化,运行期目标进程零特权。

---

跨域桥: attach 协议细节 = OpenJDK 域 36 Attach(JDK 实现对照);Arthas attach = AR-1 篇 1(VirtualMachine vs 自研);fd 传递的消费方 = AP-2(perfEvents_linux.cpp 用 fd);权限时序 = 上一篇(PMU/tracepoint 降权解析)。

**OpenJDK 关联**: [域 36 Attach — outlines/36-attach/] — attach 协议(Socket IPC+信号)的 JDK 侧实现对照。
