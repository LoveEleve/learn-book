# 01. 10 分钟挂上生产机器 — 安装与 Attach

> 🟢 使用域 | 覆盖: as.sh 全家 + help/version/stop
> 读者处境: 你刚拿到一台线上 JVM 的访问权,CPU 已经 100%,怎么在 10 分钟内开始排查?

### 1. "as.sh 一行搞定" — 三条 attach 路径

场景: 目标 pid 已知,一键 attach 并进入交互终端。

- 直接 pid: `./as.sh <pid>`(as.sh:559-820 `parse_arguments` 解析参数)
- 指定端口: `./as.sh <pid>@<ip>:<telnetPort>:<httpPort>`
- 不确定 pid: 无参数运行 → jps 列出 JVM 进程交互选择(as.sh:751-819)
- 只挂不连: `./as.sh --attach-only <pid>`(CI/脚本场景)
- 默认端口: telnet 3658 / http 8563(as.sh:100-105)
- 命令定义: `help`(basic1000/HelpCommand.java:28 `@Name("help")`)

关键设计: attach 是**外部 JVM 发起,目标 JVM 内执行**——[JVMTI: loadAgent 本质是 JVMTI Agent 加载,目标 JVM 的 attach listener 线程在收到 attach 请求后调用 Agent_OnAttach(即 agentmain),全程不修改目标进程内存,只注册回调]——`as.sh` 执行 `java -jar arthas-core.jar -pid X`(as.sh:893-899),core 里 `VirtualMachine.attach(pid)` + `loadAgent(arthas-agent.jar)`(core/Arthas.java:103/125)。attach 成功后 as.sh 再 telnet 连终端——**attach 和连接是两个独立步骤**,`--attach-only` 就是只做前者。

生产注意: ① 无 `--attach-only` 时 as.sh 会阻塞在 telnet 终端——脚本调用要加;② 端口被占会报错,`port_pid_check`(as.sh:940-971)帮你检测;③ attach 需要与目标进程**同一用户**(sanity_check as.sh:903-938)。

### 2. "远程机器怎么办" — 隧道与安全参数

场景: 目标 JVM 在 K8s 里/内网,本机无法直连。

- `--tunnel-server http://tunnel.aliyun.com` + `--agent-id <id>`: 走公共/私有隧道(as.sh:838-891 拼参)
- `--username/--password`: 连接认证
- 0.0.0.0 监听时 Arthas 强制生成随机密码(ArthasBootstrap.java:415-426,源码域 AR-1 细节)
- `--disabled-commands stop`: 禁用危险命令(starter 默认禁用 stop)

关键设计: 隧道模式 = arthas 在目标 JVM 内**主动外连** tunnel-server 注册(agent-id 是注册标识),本机再从 tunnel-server 拉会话——绕过了"目标机不暴露端口"的网络限制。

生产注意: 公共隧道有数据出境风险,公司自建 tunnel-server 更稳;agent-id 要唯一且可辨识(常与应用名绑定)。

### 3. "用完要干净" — 停止与还原

场景: 排查结束,恢复现场。

- `stop`: 关停 arthas server 并退出(basic1000/StopCommand.java:18 `@Name("stop")`)
- `reset`: 撤销所有字节码增强(watch/trace 织入的 SpyAPI 调用,basic1000/ResetCommand.java:25)
- 退出终端: `exit`/`logout` 只断连接,不停止 server

关键设计: 停 server 与退终端是两件事——`exit` 只是断开 telnet,arthas 仍寄生在目标 JVM(守护线程);`stop` 才走完整销毁链(removeTransformer + SpyAPI.setNopSpy,ArthasBootstrap.java:944-945,AR-1 源码)。

生产注意: 排查完必须 `stop`(否则性能探针常驻);`reset` 在 watch/trace 场景必做。

---

跨域桥: 本篇的 attach 机制 = AR-1 全部源码内容(两条 attach 路径、SpyAPI 注入);端口/密码/隧道 = AR-1 bind() 的配置消费。

---

**OpenJDK 关联**:  [OpenJDK 域 36 Attach — outlines/36-attach/] — attach 的底层是 JDK Attach API(Socket IPC + attach listener 线程);**另见** [OpenJDK 域 47 Instrumentation — outlines/47-instrumentation/] — loadAgent 的落地是 JPLISAgent。
**另见** [OpenJDK 域 40 Launcher — outlines/40-launcher/] — java -jar arthas-core.jar 的启动链(libjli→main)。

### 核心悬念

**"attach 之后,JVM 里到底多了什么?"** — as.sh 只是发令枪,真正进门的是 AgentBootstrap 和 ArthasClassloader。但这里有个更刁钻的问题: 增强后的代码要调 SpyAPI,它凭什么全世界都找得到?答案不在 as.sh——在下一篇的单例构造器里。

> → [AR-1 篇 2](../ar1-agent-injection/02-bootstrap-init.md)
