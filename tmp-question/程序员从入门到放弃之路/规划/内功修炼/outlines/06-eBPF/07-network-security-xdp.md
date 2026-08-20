# eBPF 网络与安全 — 从连接观测、XDP 丢包到 LSM 策略

> Cluster C: 4 KPs | 依赖: 01-ebpf-architecture、04-bpftrace、05-tracing-sources | 读者基线: tcpdump/ss、TCP 生命周期、eBPF attach
> 读者处境: 前 01-06 篇已经覆盖 eBPF 架构、Map、开发、脚本、追踪源和 CPU/内存观测；本篇把同一技术栈推进到网络数据面和安全策略
> 打开新视角: eBPF 网络安全有三种距离——**socket/trace 观测、TC/XDP 数据面处理、LSM 安全决策**；越靠近数据入口，吞吐越高，但可见上下文和开发约束越多

---

### 概念依赖链

```
01 架构 + 04 bpftrace + 05 追踪源 + 06 CPU/内存 → 本篇: 网络/安全/XDP
  ├─ §1 tcpconnect/tcplife/tcpretrans(连接生命周期)
  ├─ §2 XDP/TC/sockmap(网络数据面)
  ├─ §3 opensnoop/execsnoop/capable/signals(安全观测)
  ├─ §4 LSM eBPF(安全策略)
  └─ §5 综合决策(观测→审计→预防)
先讲: 网络观测 → 早期数据面 → 安全观测 → LSM 策略 → 综合
后续依赖: 阶段7 系统编程(POSIX/信号/进程间通信)
```

### 叙事顺序

1. 问题引入——网络连接异常、DDoS、恶意进程和权限滥用能否用同一套 eBPF 思路观察？（**Aha: eBPF 的共同底座是事件/上下文/Map/策略，但 attach 位置决定可见信息、开销和风险**）
2. 网络观测——建立、存活、重传
3. XDP/TC/sockmap——数据面处理距离
4. 文件/命令/能力/信号安全观测
5. LSM eBPF——从记录到阻断
6. 综合选择与边界
7. 收束——阶段总结

### 1. 网络观测 — tcpconnect、tcplife、tcpretrans 组成时间线

场景提示: 服务偶发连接失败或重传升高，怎样知道是谁连了谁、连接活了多久、哪个阶段开始异常？ [写作时展开]

关键设计: 三类工具观察 TCP 生命周期不同阶段：

```[pseudocode]
tcpconnect:
  connect 入口/结果线索
  → 进程、目标地址、端口

tcplife:
  建立到关闭的生命周期
  → 持续时间、收发字节、状态等(具体字段随工具版本)

tcpretrans:
  重传事件与连接 tuple
  → 结合 RTT、SACK、拥塞和抓包判断原因

证据链:
  connect → established/life → retrans/close
```

Why: 为什么这些工具不能直接告诉你“物理链路丢包了”？——**重传可能来自拥塞、乱序、ACK 延迟、接收窗口、虚拟网络或中间设备**；工具把事件聚合出来，根因仍需和 `ss -ti`、网卡统计、抓包和应用指标交叉验证。 [内核: tcpconnect/tcplife/tcpretrans 依赖不同内核函数/tracepoint，目标版本要核对 attach 点]

比喻锚点: tcpconnect 是登记入住，tcplife 是住客账单，tcpretrans 是快递重送记录；三者拼成连接传记，但不等于完整网络尸检。 [写作时展开]

### 2. XDP、TC 与 sockmap — 越靠前越快，也越受约束

场景提示: DDoS 黑名单包如果已经走到 TCP/IP 栈才丢弃，CPU 可能早已被耗尽；能否在驱动收到包后立即处理？ [写作时展开]

关键设计: XDP、TC 和 socket 层位于不同网络处理阶段：

```[pseudocode]
RX queue / driver NAPI
  → XDP
      XDP_DROP: 尽早丢弃
      XDP_PASS: 进入正常网络栈
      XDP_TX: 从同一设备发回
      XDP_REDIRECT: 重定向到设备/CPU/map 等目标

TC ingress/egress
  → 网络栈更靠后
  → 可做分类、修改、重定向和策略

sockmap/sockhash:
  → socket 层重定向/复用特定数据路径
  → 具体旁路范围由程序类型、协议和内核实现决定
```

Why: 为什么不能把 XDP 宣传成“零 CPU、百万 pps 黑名单”和“绕过整个 TCP/IP 栈”？——**XDP 仍消耗 CPU/内存带宽，并受驱动、模式、报文解析、Map 查找和硬件能力限制**；sockmap 可以优化特定 socket 数据路径，但不是任意 TCP 流都自动绕过全部协议栈。TC 更晚，却能看到更多上下文、执行更复杂策略。 [内核: XDP 运行于驱动/NAPI 早期，TC 与 sockmap 的上下文和 helper 集合不同]

比喻锚点: XDP 是机场入口闸机，坏包还没进大厅就拒绝；TC 是安检区，能检查更多行李；socket 层是分配到具体登机口后再做重定向。 [写作时展开]

### 3. 安全观测 — 文件、命令、能力和信号

场景提示: 服务器出现可疑进程，怎样观察它打开了什么、执行了什么、申请了哪些能力、向谁发了信号？ [写作时展开]

关键设计: 安全观测把行为事件转换为带 PID/comm/路径/目标的审计线索：

```[pseudocode]
opensnoop:
  open/openat → 文件路径/flags/进程

execsnoop:
  execve → 新程序/父子关系/参数线索

capability:
  capable/security hooks → 权限能力请求

signals:
  signal_generate → 谁向谁发什么信号

输出策略:
  先过滤/聚合
  → 保留 request_id/进程身份/时间
  → 避免高频全量字符串输出
```

Why: 为什么 `bashreadline` 一类命令记录不能直接当成完整审计系统？——**它只覆盖特定 shell、readline 路径和权限范围，绕过 shell 的执行、脚本、远程命令或其他终端并不一定可见**；敏感命令可能包含凭据，采集本身要有授权、脱敏、访问控制和保留策略。eBPF 观测也不能替代正式审计和取证流程。 [内核: exec/open/signal/capability 事件位于不同 hook，字段、权限和覆盖范围不同]

比喻锚点: 安全观测像园区门禁、文件柜摄像头、命令审计和报警器的组合；每个传感器都有盲区，不能把一台摄像头当成全城监控。 [写作时展开]

### 4. LSM eBPF — 从观测与审计走向安全决策

场景提示: 发现某进程不该打开敏感文件，能否在内核安全检查点直接拒绝，而不只是事后记录？ [写作时展开]

关键设计: LSM BPF 程序可以挂到支持的 LSM hook，根据上下文返回允许/拒绝或配合审计：

```[pseudocode]
访问行为
  → LSM hook
  → BPF LSM 程序
      检查进程身份/cgroup/路径/凭据/策略 Map
      → allow
      → deny/错误码
      → audit/event 输出

策略生命周期:
  观测 → 验证规则
  审计 → 记录违反行为
  预防 → 在 hook 上阻断
```

Why: 为什么 LSM BPF 比 opensnoop 更接近“预防”，却不能简单替代 SELinux/AppArmor？——**它们的策略模型、持久化、审计、部署治理、组合顺序和工具生态不同**；错误的 BPF LSM 策略可能阻断系统关键操作，必须有最小权限、回滚、测试和故障恢复方案。 [内核: BPF LSM 能力依赖内核配置、LSM hook、程序权限和 verifier/安全策略]

比喻锚点: opensnoop 是门禁录像，LSM BPF 是门卫在刷卡时做决定；录像能回放，门卫却可能把合法员工也挡在门外。 [写作时展开]

### 5. 综合决策 — 观测、审计、预防三阶段

场景提示: 网络、安全和性能问题同时发生时，如何选择最靠前但风险可控的 eBPF hook？ [写作时展开]

关键设计: 用“问题语义—数据位置—动作风险”决定工具：

```[pseudocode]
只想知道发生了什么:
  tracepoint/kprobe/uprobe + Map/事件输出

需要高吞吐早丢包:
  XDP/TC, 先验证驱动/模式/报文解析和回滚

需要 socket 数据路径优化:
  sockmap/sockhash, 明确协议/程序类型边界

需要阻断安全行为:
  BPF LSM/现有安全框架组合
  → 测试、审计、最小权限、可回滚

部署原则:
  过滤/采样优先
  先观测再策略
  目标环境 benchmark 与故障演练
```

Why: 为什么越靠近数据入口不一定越适合所有策略？——**早期 hook 的上下文更少、错误影响更广，用户态诊断和复杂策略可能无法直接执行**；观测、优化、阻断是不同风险等级，不能只按性能排序。 [内核: 每种 program type 的上下文、helper 和返回语义定义了可做的事情]

### 6. 收束

完整 eBPF 网络安全闭环：

```[pseudocode]
网络行为
  → tcpconnect/tcplife/tcpretrans 观测
  → TC/XDP 处理数据面
  → opensnoop/execsnoop/capability/signal 审计
  → LSM BPF 做受控决策
  → Map/ringbuf 输出指标和事件
  → 与 ss/tcpdump/auditd/业务日志交叉验证
```

**Aha Moment**: "eBPF 从观测到安全不是单一工具升级，而是**从 socket/trace 事件，到 TC/XDP 数据面，再到 LSM 决策**的距离选择；越早处理越快，但上下文、风险和回滚要求越高。"
**回答读者三问**: ①网络重传怎么观测=连接生命周期加重传事件，并结合抓包；②XDP 适合什么=尽早丢弃/重定向，不能承诺零成本万能旁路；③观测能否直接阻断=需要 BPF LSM/策略 hook，并配合测试、审计和回滚。

---

### 核心悬念

**"eBPF 七篇已经完成：架构、Map、libbpf、bpftrace、追踪源、CPU/内存、网络/安全；下一阶段如何把这些能力放回 POSIX 系统编程，理解进程、信号、IPC 和 I/O 的完整语义？"**

→ 引出阶段 7 系统编程 — POSIX API、进程、信号、IPC、I/O 与并发。