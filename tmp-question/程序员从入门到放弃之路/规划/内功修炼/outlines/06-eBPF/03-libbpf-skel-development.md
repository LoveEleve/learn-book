# libbpf、CO-RE 与 skeleton — 从 eBPF 对象文件到可维护程序

> Cluster B: 5 KPs | 依赖: 01-ebpf-architecture、02-ebpf-maps-ringbuf | 读者基线: eBPF 加载链、Map/事件管道、C 语言
> 读者处境: 01-02 篇已经讲完 verifier、Map、RingBuf；本篇回答：怎样把 BPF C 程序编译、加载、绑定 Map、attach 到 hook，并让用户态应用可靠消费事件
> 打开新视角: 工业化 eBPF 开发不是手写 syscall，而是**对象文件 + BTF/CO-RE + libbpf 生命周期 + 生成 skeleton + 用户态事件循环**

---

### 概念依赖链

```
01 架构 + 02 Maps/RingBuf → 本篇: libbpf/CO-RE/skeleton 开发
  ├─ §1 编译/BTF/CO-RE/skeleton(构建产物)
  ├─ §2 open/load/attach/destroy(生命周期)
  ├─ §3 BPF syscall/helper(底层模型)
  ├─ §4 Go ebpf-go/bpf2go(语言绑定)
  └─ §5 BCC/bpftrace/libbpf 选择(开发方式)
先讲: 编译 → 生成 skeleton → 加载/绑定 → helper → Go/框架选择
后续依赖: 04-bpftrace-programming(快速脚本化探针)
```

### 叙事顺序

1. 问题引入——同一个 tracing 程序怎样从 BPF C 文件变成一个可部署的用户态工具？（**Aha: skeleton 不是魔法，它只是把对象文件中的 program/map/link 生命周期生成成可检查的用户态 API**）
2. clang/BTF/CO-RE/skeleton——先生成可加载对象
3. libbpf 生命周期——open/load/attach/consume/destroy
4. `bpf()` 与 helper——理解底层边界
5. Go 生态——ebpf-go/bpf2go/libbpf wrapper
6. BCC/bpftrace/libbpf 选择
7. 收束——构建、部署、运行三条链

### 1. clang + BTF + CO-RE + skeleton — 从源代码到对象文件

场景提示: BPF C 程序怎样变成内核能验证、用户态能操作的 `.bpf.o`？ [写作时展开]

关键设计: 典型 libbpf/CO-RE 工作流把 BPF 程序、类型信息、Map 和 attach section 一起放进 ELF 对象：

```[pseudocode]
编译 BPF 对象:
  clang -target bpf -g -O2 -c prog.bpf.c -o prog.bpf.o

类型:
  bpftool btf dump file /sys/kernel/btf/vmlinux format c
  → 生成/准备 vmlinux.h

CO-RE:
  bpf_core_read(&dst, sizeof(dst), &src->field)
  → 记录字段重定位信息
  → libbpf 加载时结合目标内核 BTF 调整

skeleton:
  bpftool gen skeleton prog.bpf.o > prog.skel.h
  → 生成 open/load/attach/link/map 访问封装
```

Why: 为什么 CO-RE 仍然需要目标内核 BTF 和功能检查？——**它解决的是类型布局/字段偏移的重定位，不是所有 helper、hook、配置和行为差异**；缺少 BTF、目标字段不存在或程序依赖新 helper 时，仍可能加载失败。`vmlinux.h` 也不应与普通内核头文件随意混用，类型定义要按 libbpf/编译约定组织。 [内核: `/sys/kernel/btf/vmlinux` 是运行内核 BTF 的一个来源，BTF 与 verifier/CO-RE 在加载阶段共同发挥作用]

比喻锚点: `.bpf.o` 像带有可重定位施工图的预制件，skeleton 是操作手册；到不同内核现场，libbpf 根据当地地图调整接口位置。 [写作时展开]

### 2. skeleton 生命周期 — open、load、attach、consume、destroy

场景提示: skeleton 生成后，用户态程序究竟在哪一步触发 verifier，在哪一步真正挂上 hook？ [写作时展开]

关键设计: libbpf 把 BPF 对象生命周期分成可检查的阶段：

```[pseudocode]
obj = prog_bpf__open()
  → 解析 ELF、发现 programs/maps/links

prog_bpf__load(obj)
  → 创建 maps
  → CO-RE 重定位
  → BPF_PROG_LOAD
  → verifier + JIT/解释执行

prog_bpf__attach(obj)
  → 按 section/程序类型 attach 到 hook
  → 返回 link 对象

运行:
  ring_buffer__poll / perf_buffer__poll
  → 消费用户态事件

prog_bpf__destroy(obj)
  → 释放 link/map/program fd 引用
```

Why: 为什么 open 和 load 必须分开？——**open 适合检查/修改对象配置，load 才会触发内核验证和资源创建**；attach 又是独立阶段，失败时可明确区分对象解析、verifier、权限、hook 和事件消费问题。生产程序还要处理信号、pin 生命周期、内核能力探测和优雅卸载。 [内核: `BPF_PROG_LOAD` 与 attach 操作是不同控制面步骤，libbpf 只提供更安全的封装]

比喻锚点: open 是拆箱验货，load 是安检入场，attach 是把设备接上生产线，poll 是运行值班；每一步失败原因不同。 [写作时展开]

### 3. `bpf()` 与 helper — 了解封装下面的底层协议

场景提示: libbpf 一行 `bpf_program__attach_tracepoint` 背后，内核到底收到了哪些操作？ [写作时展开]

关键设计: 用户态最终通过 `bpf()` 命令创建/加载对象，BPF 程序通过受限 helper 调用内核功能：

```[pseudocode]
bpf(BPF_MAP_CREATE, attr)
  → type/key_size/value_size/max_entries
  → map fd

bpf(BPF_PROG_LOAD, attr)
  → instruction/license/log/attach约束
  → program fd

attach:
  tracepoint / kprobe / cgroup / XDP / TC
  → 不同 attach API、link 或 netlink/setsockopt 路径

BPF helper:
  map_lookup/update/delete
  get_current_pid_tgid / ktime_get_ns
  probe_read / ringbuf_output 等
  → helper id + prototype + program type 权限
```

Why: 为什么 helper 不能当作稳定的普通 C 库 API？——**helper 集合、参数类型、上下文权限和返回错误由内核版本/程序类型决定**；`bpftool feature probe` 只能告诉你当前环境能力，不能让程序自动拥有不存在的 helper。libbpf 的价值在于封装 fd、link、重定位和错误处理，不是抹掉内核边界。 [内核: verifier 根据 helper prototype 与程序类型检查参数和可达性]

比喻锚点: helper 像经过许可的服务窗口；用户程序不能直接伸手进内核仓库，只能按窗口规定的参数办理业务。 [写作时展开]

### 4. Go eBPF 开发 — 生成类型安全的用户态加载器

场景提示: 不想用 C 写完整用户态 loader，Go 程序如何加载 `.bpf.o`、绑定 Map 并消费 RingBuf？ [写作时展开]

关键设计: Go 生态通常使用 cilium/ebpf 与 bpf2go 生成对象访问代码；libbpfgo 则是另一条 CGO wrapper 路线：

```[pseudocode]
//go:generate bpf2go -type event bpf prog.bpf.c

生成:
  Go embed/加载 .bpf.o
  → CollectionSpec
  → LoadAndAssign(&objs, nil)
  → attach link
  → ringbuf.NewReader(objs.Events)
  → 读取 event struct

选择:
  cilium/ebpf: Go 原生 loader/类型绑定, 不等于零依赖
  bpf2go: 生成 Go 对象/struct/加载代码
  libbpfgo: CGO wrapper, 适合已有 libbpf 集成
```

Why: 为什么“纯 Go”不等于部署一定没有依赖？——**仍需要目标内核 BPF 能力、BTF/CO-RE 条件、编译产物、权限和运行时系统支持**；代码生成也必须保证 BPF 事件结构与 Go 结构的布局/字节序一致。新项目的选择取决于团队语言、CO-RE 需求、生态和 CGO 约束，不存在普遍唯一的“业界标准”。 [内核: Go loader 最终仍调用 BPF syscall，并受 verifier、map、link 与内核能力限制]

比喻锚点: bpf2go 像把施工图自动翻译成 Go 操作手册；减少手写错误，但现场仍需要对应的设备和许可证。 [写作时展开]

### 5. BCC、bpftrace 与 libbpf — 按生命周期选择工具

场景提示: 同一个问题，什么时候用一行 bpftrace，什么时候写 BCC 工具，什么时候做 libbpf/CO-RE 项目？ [写作时展开]

关键设计: 工具选择取决于实验速度、部署稳定性、类型兼容和维护成本：

```[pseudocode]
bpftrace:
  一行探针/聚合/临时排障
  → 快速验证假设

BCC:
  Python + BPF 程序 + 现成工具
  → 原型与运维工具
  → 运行时编译/依赖成本需评估

libbpf + CO-RE + skeleton:
  构建/部署/维护长期工具
  → 对象文件、BTF 重定位、link 生命周期更明确

eunomia/其他分发框架:
  进一步封装配置/分发
  → 能力和生态需按项目验证
```

Why: 为什么不能简单写成“生产一定 libbpf，学习一定 bpftrace”？——**长期工具也可能需要 bpftrace 原型，生产环境也可能受内核、发行版、团队技能和兼容性约束**；选择应由探针稳定性、事件吞吐、部署控制和故障回滚决定。 [内核: 工具层不同，但最终都受 BPF verifier、attach 类型和运行时能力限制]

比喻锚点: bpftrace 是随身螺丝刀，BCC 是带说明书的工具箱，libbpf 是可维护生产线；小修、快速诊断和长期设施不应使用同一种工具标准。 [写作时展开]

### 6. 收束

一个可维护 eBPF 项目的闭环：

```[pseudocode]
BPF C + vmlinux.h
  → clang -target bpf 生成 .bpf.o
  → libbpf 解析对象/CO-RE 重定位
  → skeleton open/load/attach
  → verifier/JIT 运行
  → Map/RingBuf 输出
  → Go/C 用户态消费
  → signal/error/link 生命周期管理
```

**Aha Moment**: "skeleton 的价值不是生成几行样板代码，而是把**对象、Map、程序、link、CO-RE 和用户态生命周期**统一成一套可检查的工程接口；它减少手写 syscall，却没有消除内核能力和部署约束。"
**回答读者三问**: ①libbpf 开发流程是什么=编译对象、open/load/attach、消费、销毁；②CO-RE 保证什么=类型布局适配，不保证所有内核能力；③Go 能否绕开 C=可以用 Go loader/代码生成，但仍受 BPF 内核和对象兼容条件限制。

---

### 核心悬念

**"libbpf 适合长期项目，bpftrace 适合快速验证；如果只想用一行脚本观察系统调用、函数参数和聚合结果，bpftrace 的探针、过滤器、动作和 Map 语法怎么组织？"**

→ 引出 04-bpftrace-programming — bpftrace 探针、过滤、动作、变量和映射。