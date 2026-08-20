# eBPF 核心架构 — 验证器、JIT、Maps、BTF 与程序类型如何协作

> Cluster A: 7 KPs | 依赖: 无 | 读者基线: 知道内核模块概念、会使用 `dmesg`/`/proc`
> 读者处境: 阶段 6 开篇——前面已经用 perf/ftrace/BCC 观测过内核；本篇回答这些工具背后的共同基础：为什么 eBPF 能在不加载传统内核模块的情况下，把程序安全地放到内核 hook 上运行
> 打开新视角: eBPF 不是“内核里的脚本”，而是一条受约束的执行链：**用户态加载 → verifier 证明安全 → JIT/解释执行 → hook 触发 → Maps/ringbuf 传递状态**

---

### 概念依赖链

```
无前置(阶段6开篇) → 本篇: eBPF 架构与程序类型
  ├─ §1 为什么需要受约束的内核可编程性
  ├─ §2 bpf() / verifier / JIT / attach / Maps
  ├─ §3 BTF + CO-RE(跨内核类型适配)
  ├─ §4 eBPF 指令集(寄存器/指令/反汇编)
  └─ §5 XDP/TC/tracing/socket/cgroup 程序类型
先讲: 动机 → 加载执行链 → 类型兼容 → 指令模型 → hook 分类
后续依赖: 02-ebpf-maps-ringbuf(内核数据如何传回用户态)
```

### 叙事顺序

1. 问题引入——想观测或改变内核行为时，为什么不每次都写一个内核模块？（**Aha: eBPF 的核心不是“任意代码进内核”，而是把程序限制在 verifier 能证明安全的执行模型里**）
2. 加载执行链——`bpf()`、verifier、JIT、attach
3. Maps 与 bpffs——程序和状态如何存在
4. BTF/CO-RE——为什么同一份对象文件能适配不同内核类型
5. 指令集——寄存器、分支和内存访问边界
6. 程序类型——不同 hook 决定不同能力
7. 收束——安全、性能、动态性的真实边界

### 1. 为什么不直接写内核模块 — 需要受约束的可编程性

场景提示: 线上想临时统计一次系统调用或丢包路径，写内核模块、编译、加载和回滚的风险是什么？ [写作时展开]

关键设计: 传统模块能力强但权限和故障半径大；eBPF 用受限程序、验证和特定 hook 降低风险：

```[pseudocode]
传统内核模块:
  直接运行在内核权限
  → 能力大、接口广
  → bug/ABI/生命周期错误可能影响整机

eBPF:
  用户态准备受限字节码
  → verifier 检查
  → attach 到允许的 hook
  → 事件触发时执行
  → 通过 map/ringbuf 与用户态交换数据
```

Why: 为什么 eBPF 不能承诺“绝对不会让内核出问题”？——**verifier 约束的是可证明的程序安全属性，内核 bug、helper 实现、资源耗尽、驱动和用户态逻辑错误仍可能造成问题**；它降低风险，不是数学意义的零风险。相比模块，eBPF 的能力边界、生命周期和加载权限更明确。 [内核: `bpf(BPF_PROG_LOAD)`、verifier 和 hook 类型共同决定程序能做什么]

比喻锚点: 内核模块像允许工程师直接进入发动机舱改线，eBPF 像在经过安全检查的接口上安装受限控制器；可插拔更快，但不能任意改动所有零件。 [写作时展开]

### 2. `bpf()` → verifier → JIT → attach — 程序如何进入内核

场景提示: bpftrace 一行脚本最终怎样变成内核能执行的程序？ [写作时展开]

关键设计: 加载过程包含程序验证、类型/权限检查、可选 JIT 和 hook 绑定：

```[pseudocode]
用户态工具/libbpf/bpftrace
  → 编译为 eBPF 指令 + map/attach 描述
  → bpf(BPF_PROG_LOAD)
  → verifier:
      检查控制流可达性/循环规则
      追踪寄存器与指针类型
      检查栈/内存边界
      检查 helper 与程序类型权限
  → 解释执行或 JIT 为目标架构机器码
  → attach 到 kprobe/tracepoint/TC/XDP/cgroup 等 hook
```

Why: 为什么 verifier 要追踪寄存器状态和指针类型，而不是只检查“代码看起来短”？——**内核指针、map value、packet data 和用户指针的访问权限不同，必须证明每次读取都有边界和正确类型**；程序复杂度、循环支持、指令上限和资源限制会随内核版本变化，不能固定写成“永远 4096 或永远 1M”。 JIT 提升执行效率，但不改变 verifier 的安全约束。 [内核: verifier 先于 JIT，目标架构 JIT 和配置决定最终执行路径]

比喻锚点: verifier 像编译前的安全审查员，逐步证明每次走廊访问都带权限和边界；JIT 只是审查通过后把路线翻译成当地高速公路。 [写作时展开]

### 3. Maps、bpffs 与生命周期 — 程序之外还要保存状态

场景提示: eBPF 程序每次事件触发都很短，计数、配置和事件数据放在哪里？ [写作时展开]

关键设计: Maps 是内核中的键值/数组/队列状态对象，用户态和多个 BPF 程序可以按权限访问；pinning 把对象生命周期从某个进程延长：

```[pseudocode]
常见 map:
  hash / array / per-CPU map
  ring buffer / perf event buffer
  → 存计数、配置、聚合结果或事件

程序执行:
  event → bpf_map_lookup/update
  → 更新 map 或提交事件

生命周期:
  bpf fd 被进程持有 → 对象保持引用
  bpffs pin
  → 通过文件系统路径保留引用
  → 其他进程可按路径打开/共享
```

Why: 为什么 eBPF 程序和 Map 要分成两个对象？——**程序是执行逻辑，Map 是可共享状态**：同一份程序可以关联多个 map，不同程序也可以共享策略、计数和事件通道。Map 并非“无限共享内存”，大小、并发、per-CPU 复制和用户态消费速度都会带来内存与一致性成本。 [内核: bpffs pin 是保持 BPF 对象引用的一种方式，不等同于所有系统都必须挂载同一路径]

比喻锚点: BPF 程序像流水线工人，Map 像共享账本；工人执行很快，但账本容量、并发写入和读取速度仍要规划。 [写作时展开]

### 4. BTF + CO-RE — 跨内核类型适配，而不是无条件“一次编译到处跑”

场景提示: 不同内核版本的 `task_struct` 字段偏移可能变化，eBPF 程序如何避免为每个内核手工重编译？ [写作时展开]

关键设计: BTF 描述类型/字段信息，CO-RE 重定位在加载时结合目标内核 BTF 调整访问：

```[pseudocode]
BTF:
  类型、字段、大小、枚举等调试/类型信息
  可能来自 /sys/kernel/btf/vmlinux 或对象文件

CO-RE 编译:
  编译器/libbpf 记录字段访问重定位信息

加载目标内核:
  读取目标 BTF
  → 重定位字段偏移/类型差异
  → bpf_core_read 等兼容访问
  → verifier 检查最终程序
```

Why: 为什么 CO-RE 不是“任何内核、任何程序都无需适配”？——**目标内核需要 BTF/必要特性，程序还可能依赖特定字段、helper、hook 和语义**；CO-RE 解决类型布局差异，不解决所有功能版本、配置和行为差异。BTF Hub/发行版 BTF 包可以补充缺失信息，但不是内核能力的自动替代。 [内核: `/sys/kernel/btf/vmlinux`、BTF 类型和 libbpf 重定位共同构成 CO-RE 条件]

比喻锚点: BTF 是不同城市的建筑地图，CO-RE 是根据当地地图调整“去三楼某房间”的路线；如果当地根本没有这栋楼，地图重定位也无法创造它。 [写作时展开]

### 5. eBPF 指令集与程序类型 — hook 决定能力边界

场景提示: 同样是 eBPF 字节码，为什么 XDP 能决定丢包，tracing 程序却不能随意修改网络包？ [写作时展开]

关键设计: eBPF 指令集提供受限寄存器/内存/分支模型，程序类型则决定可访问上下文、helper 与返回语义：

```[pseudocode]
寄存器:
  R0 返回值
  R1-R5 参数/临时值
  R6-R9 callee-saved
  R10 只读栈帧指针

指令:
  ALU32/ALU64 / load-store / branch / call / exit
  → 实际编码与扩展由内核架构实现定义

程序类型:
  XDP: 驱动早期, PASS/DROP/TX/REDIRECT 等动作
  TC: ingress/egress 流量控制与修改
  tracing: kprobe/tracepoint/perf 等观测
  socket/cgroup: socket 过滤、策略与 cgroup 级网络钩子
```

Why: 为什么程序类型必须限制 helper 和上下文？——**XDP 的 packet data 指针、tracepoint 的只读上下文、cgroup socket 的策略上下文具有不同生命周期和安全边界**；同一 helper 在不同程序类型可能不可用。sockmap/sockhash 可以优化 socket 数据路径，但不能简单说“绕过整个 TCP/IP 栈”。 [内核: attach 类型、context 和 helper 白名单是 verifier 判断的一部分]

比喻锚点: eBPF 指令集是统一语言，程序类型像不同工作许可证：网卡入口许可证能做快速分流，追踪许可证能观察调用，但不能因为会说同一种语言就互相拥有全部权限。 [写作时展开]

### 6. 收束

eBPF 架构闭环：

```[pseudocode]
用户态编译/加载
  → bpf() + verifier
  → JIT/解释执行
  → attach 到特定 hook
  → 通过 map/ringbuf 保存状态/传事件
  → BTF/CO-RE 适配类型差异
  → 程序类型决定能力与上下文边界
```

**Aha Moment**: "eBPF 的工业化能力不是单独来自 verifier、JIT 或 CO-RE，而是**安全执行模型 + 共享状态对象 + 类型重定位 + hook 能力边界**共同组成的系统。"
**回答读者三问**: ①为什么比内核模块更适合动态观测=受约束加载、可验证和按 hook 插入；②CO-RE 解决什么=类型布局迁移，不是所有版本差异；③程序类型为什么重要=它决定上下文、helper 和返回语义。

---

### 核心悬念

**"程序已经安全地挂进内核，Map、ringbuf、perf event 如何把高频数据高效送回用户态？不同数据结构又该如何选择？"**

→ 引出 02-ebpf-maps-ringbuf — eBPF Maps、ringbuf 与 perf event 数据管道。