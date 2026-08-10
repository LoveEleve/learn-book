# libbpf + CO-RE + skel 开发流程 + 辅助函数 + Golang eBPF

> Cluster B: 5 KPs | 依赖: 01 架构 + 02 Maps/RingBuf | 读者基线: 理解 eBPF 架构 + 会 C 语言

---

### 1. libbpf + CO-RE + skeleton — 从字节码到可运行程序
  - 开发流程: `clang -target bpf -g -O2 -c prog.bpf.c -o prog.bpf.o` → `bpftool gen skeleton prog.bpf.o > prog.skel.h` → C 程序 include skel → `prog_bpf__open()` → `prog_bpf__load()` → `prog_bpf__attach()` → poll (eBPF开发指南 Ch4 §6, 深入理解eBPF Ch3 §1-2)
  - skeleton: 自动生成 struct/maps/progs/links → 类型安全的 open/load/attach/destroy → 编译时错误 → 替代手写 `bpf(BPF_PROG_LOAD)` (eBPF开发指南 Ch4 §6)
  - CO-RE: `bpf_core_read(&dst, sizeof(dst), &src->field)` → BTF 在加载时重定位偏移 → 内核版本无关 (深入理解eBPF Ch3 §1-2)
  - 关键文件: `vmlinux.h` → `bpftool btf dump file /sys/kernel/btf/vmlinux format c > vmlinux.h` → 替换所有内核头文件 (eBPF开发指南 Ch8 §3)

### 2. eBPF 系统调用 — BPF_PROG_LOAD + BPF_MAP_CREATE + attach
  - `BPF_PROG_LOAD`: 加载验证过的 BPF 字节码 → 返回 fd → `syscall(__NR_bpf, BPF_PROG_LOAD, &attr, sizeof(attr))` (深入理解eBPF Ch2 §3)
  - `BPF_MAP_CREATE`: 创建 Map → 指定 type/key_size/value_size/max_entries → 返回 fd → 可 pin 到 bpffs (深入理解eBPF Ch2 §4)
  - Attach 方式: tracepoint → `perf_event_open()` + `PERF_EVENT_IOC_SET_BPF` / kprobe → `perf_event_open()` + ioctl / cgroup → `BPF_PROG_ATTACH` / XDP → `setsockopt()` 或 netlink (深入理解eBPF Ch2 §5)
  - libbpf 封装: 不再手写 syscall → `bpf_object__open_file()` / `bpf_program__attach_tracepoint()` 一行 attach

### 3. 辅助函数 (Helper Functions) — 设计/实现/bpf_map_lookup_elem
  - 辅助函数本质: 内核内置的稳定 API → 每个 helper 有 id → `bpf_map_lookup_elem` = helper 1, `bpf_map_update_elem` = helper 2 (深入理解eBPF Ch2 §4)
  - 设计机制: 内核 `struct bpf_verifier_ops` 注册 helper 的 prototype(arg types/ret type) → 验证器校验参数类型 (深入理解eBPF Ch2 §4)
  - 核心 helper 分类: Map 操作(lookup/update/delete/get_next_key) / 追踪辅助(probe_read/get_current_pid_tgid) / 网络辅助(skb_store_bytes/redirect) / 平台辅助(ktime_get_ns/get_prandom_u32) (eBPF开发指南 Ch9 §3)
  - 最新 helper 列表: `bpftool feature probe` → 当前内核支持的 helper id + names + 验证器版本

### 4. Golang eBPF 开发 — libbpfgo / cilium ebpf-go / bpf2go
  - Cilium ebpf-go: 纯 Go 实现 eBPF 加载 → `ebpf.CollectionSpec.LoadAndAssign()` → 不需要 CGO → maps/programs 类型安全 → 业界标准 (eBPF开发指南 Ch7 §2)
  - bpf2go: Go generate → `//go:generate go run github.com/cilium/ebpf/cmd/bpf2go -type event bpf prog.bpf.c` → 自动生成 Go struct + load/attach 代码 (eBPF开发指南 Ch7 §3)
  - libbpfgo: Go wrapper over libbpf C → CGO 依赖 → Aqua Security 维护 → 适合已有 libbpf 项目迁移 (eBPF开发指南 Ch7 §1)
  - 对比: ebpf-go(纯 Go 零 CGO) > bpf2go(ebpf-go+代码生成) > libbpfgo(C libbpf wrapper) → 新项目首选 ebpf-go + bpf2go

### 5. 开发框架对比 — BCC / bpftrace / eunomia-bpf / Coolbpf
  - BCC: Python/C 混合 → `BPF(text=bpf_program_code)` → 运行时编译(BCC 内嵌 clang/llvm) → 适合原型/工具 → 部署重 (深入理解eBPF Ch3 §3-4)
  - bpftrace: awk for eBPF → 一行命令: `bpftrace -e 'tracepoint:syscalls:sys_enter_openat { printf("%s\n", str(args->filename)); }'` → 零编译零加载 (BPF之巅 Ch5 §1)
  - eunomia-bpf: 用 JSON 描述 BPF 程序 → 分发字节码 + 配置文件 → 零依赖部署 (深入理解eBPF Ch3 §5)
  - 选择指南: 学习/调试→bpftrace, 生产力工具→BCC, 生产部署→libbpf/CO-RE, 新 Go 项目→ebpf-go+bpf2go

### 6. 收束
  - libbpf + CO-RE + skeleton = eBPF 工业级开发的"标准写法" — 告别 BCC 的运行时编译和内核头文件编译依赖
  - Golang eBPF 生态成熟(ebpf-go+bpf2go) — 不再需要 C 语言作为应用层入口
  - 辅助函数 100+ 个 → 记住 "Map 操作 + probe_read + get_current" 就能覆盖 80% 场景

---

### 核心悬念
**"libbpf 生产环境很好 — 但我想先快速看到效果, 不写 C 代码, 不编译。有没有一行搞定？"**

→ 引出 04-bpftrace 编程 — 探针格式/过滤器/动作/控制流/变量/映射表
