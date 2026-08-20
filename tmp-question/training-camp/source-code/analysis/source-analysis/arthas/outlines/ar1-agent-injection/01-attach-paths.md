# 01. 一条 attach 命令,目标 JVM 里发生了什么? — 两条 attach 路径

> 🔴 Deep | 31 KP 中的 2 个入口机制(外部 attach / 进程内自 attach)
> 读者处境: 你在终端敲了 `./as.sh 12345`,3 秒后 arthas 就寄生到了那个 JVM——这 3 秒里发生了什么?

### 1. "外来的和尚" — 外部 attach 全链 (as.sh 路径)

场景: as.sh 是一个**独立 JVM**(arthas-core.jar),它要向目标 JVM 借一个钩子。

- as.sh 拼参数执行 `java -jar arthas-core.jar -pid <pid> ...`(bin/as.sh:893-899)→ `Arthas.java attachAgent()`(core/Arthas.java:91)
- `VirtualMachine.attach(pid)`(Arthas.java:103/105)— attach 协议与目标 JVM 建立连接
- `virtualMachine.loadAgent(arthasAgentPath, "coreJar路径;agentArgs")`(Arthas.java:125)— **参数是 `;` 分隔的两段**: 前半是 core jar 路径,后半是要传给服务端的参数(AgentBootstrap.java:110-119 用 `args.indexOf(';')` 切分)
- 目标 JVM 内: `AgentBootstrap.agentmain()`(agent334/AgentBootstrap.java:67)→ `main()`(:90): 幂等检查 `SpyAPI.isInited()`(:94,已启动直接返回)→ 解析参数 → 定位 core jar(找不到回退到 agent jar 同目录 :121-138)
- [JVMTI: loadAgent 的本质是 JVMTI Agent 加载——JVM 在线程池里起一个 agentmain 线程调用 `Agent_OnLoad`/`Agent_OnAttach`,即 `AgentBootstrap.agentmain`]

关键设计: [模式: 门面+双实现汇聚——as.sh 外部 attach 与 Starter 自 attach 是同一入口(ArthasBootstrap 单例)的两个实现;ArthasClassloader/AttachArthasClassloader 是同一加载策略的两次编码] **attach 是"从外部 JVM 借钩子"**——`VirtualMachine.attach` 用的是 OS 信号(UNIX SIGQUIT 探测)+ 套接字协议(JVM 的 attach listener 线程),不是注入代码;真正"进入"目标 JVM 的是 loadAgent 的 **arthas-agent.jar**,而它只有 1 个使命: 找到 core jar 并用**自定义 ClassLoader** 加载。

### 2. "隔离的搬运工" — ArthasClassloader 双亲委派特例

场景: core 的代码(ArthasBootstrap 等)要跑在目标 JVM 里,但又不能污染应用。

- `loadOrDefineClassLoader`(AgentBootstrap.java:83-88): `new ArthasClassloader(new URL[]{arthasCoreJarFile.toURI().toURL()})`——只加载 arthas-core.jar
- **全局 volatile 持有**(AgentBootstrap.java:61): 重复 attach 时复用同一个 CL;`resetArthasClassLoader()`(:74-76)置空——stop 后允许重新加载
- **实现策略**(agent/ArthasClassloader.java:11-30): `extends URLClassLoader`,parent = System CL 的 parent(平台 CL);`loadClass` 重写——`sun.*`/`java.*` 走父加载器,**其余先 `findClass`(自己加载)再 parent(子加载器优先)**;`appendURL` 支持动态追加 jar
- 反射桥接(AgentBootstrap.java:176-191): `agentLoader.loadClass("com.taobao.arthas.core.server.ArthasBootstrap")` → `getMethod("getInstance", Instrumentation.class, String.class).invoke(...)` → `isBind()` 验证端口绑定成功
- 独立绑定线程 `arthas-binding-thread`(AgentBootstrap.java:146-162,守护线程+join): 防止在 attach 线程里跑业务导致内存泄漏(#195)

关键设计: ArthasClassloader 是**特殊的双亲委派**——它要能加载 core 里所有类,但又不能抢走应用的类。这是"隔离"与"可用"的平衡点:core 的类它自己加载,应用的类仍走双亲委派交给应用 CL,`java.*` 交给 Bootstrap。**关键对比**: 进程内 attach 用的 `AttachArthasClassloader`(arthas-agent-attach/AttachArthasClassloader.java:19-41)与它是**同一份策略代码**(parent=平台 CL、`sun.*`/`java.*` 走 parent、其余子优先)——两个入口两套类,设计完全相同: 保证"只有 core 的类被隔离加载,系统类永远不出问题"。[Java: 双亲委派破坏的经典场景——URLClassLoader 默认 parent 优先;这里改成 child-first,是为了让 core 的类(如 fastjson2、netty)不与应用版本冲突]

### 3. "自己的钩子自己借" — 进程内自 attach (Starter 路径)

场景: Spring Boot 应用启动时,arthas 已经"藏"在自己进程里——不需要外部 JVM。

- `ArthasAgent.init()`(arthas-agent-attach/ArthasAgent.java:77-134): `ByteBuddyAgent.install()`(:90)——**在进程内自 attach** 拿到 Instrumentation,全程无第二个 JVM
- arthas-home 解析(:94-105): 未指定时从 classpath 解压 `arthas-bin.zip` 到临时目录(createTempDir :98)——starter 场景用户不用装任何东西
- `AttachArthasClassloader`(:112-113)→ 反射 `ArthasBootstrap.getInstance(inst, Map)`(:120-122)— 与外部路径**同一单例**
- `slientInit=false`(默认)时失败直接抛异常**阻断应用启动**(ArthasAgent.java:128-133)
- 触发方: `ArthasConfiguration.arthasAgent` Bean(arthas-spring-boot-starter/ArthasConfiguration.java:66-69)——Spring 上下文启动时立即执行
- [Java: ByteBuddyAgent.install 自 attach——利用 `com.sun.tools.attach` 的 self-attach 技巧(JVM 允许 attach 自己),再走 loadAgent 同一条链路]

关键设计: 两条路径的差异本质是**"谁发起 attach"**: as.sh 是外部进程(要 target 的 attach 权限、端口连通);Starter 是**自己 attach 自己**(ByteBuddyAgent)——免去外部依赖,所以能做成"启动即寄生"。代价是: 应用进程内多了一个常驻 agent,这就是 starter 默认 `disabledCommands=stop`(防误 stop)的由来。

---

跨域桥: 两条路径的终点 `ArthasBootstrap` 单例 = 下一篇(构造 7 步+SpyAPI 注入);进程内 attach 的 Instrumentation 与外部 loadAgent 的是同一个东西(Instrumentation API);AR-0 篇 1 的 `--attach-only` = 本文 §1 的"只 attach 不连终端"。

---

**OpenJDK 关联**:  [OpenJDK 域 36 Attach — outlines/36-attach/] — VirtualMachine.attach/loadAgent 的 JDK 实现(Socket IPC + 信号);**另见** [OpenJDK 域 47 Instrumentation — outlines/47-instrumentation/] — agentmain 的 JPLISAgent 侧。

### 核心悬念

**"core 的代码凭什么跑在目标 JVM 里,还不污染它?"** — ArthasClassloader 只是第一层"隔离"。下一道更精妙: 被增强的业务方法要直接调 SpyAPI——而 SpyAPI 必须放在所有 ClassLoader 的共同祖先上。它是怎么进去的?

> → [02-bootstrap-init.md](02-bootstrap-init.md)