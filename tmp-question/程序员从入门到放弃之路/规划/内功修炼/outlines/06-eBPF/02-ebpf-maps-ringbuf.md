# eBPF Maps + RingBuf + perf_event 数据管道 + 调用栈回溯

> Cluster A: 7 KPs | 依赖: 01-ebpf-architecture | 读者基线: 理解 eBPF 架构(验证器/JIT/加载流程)

---

### 1. Maps 全家桶 — 内核↔用户态唯一通道
  - BPF_MAP_TYPE_HASH: key-value 通用映射 → `bpf_map_lookup_elem` 查 / `bpf_map_update_elem` 写 → 用于计数/缓存/状态跟踪 → `bpftrace -e 'kprobe:vfs_read { @reads[pid]=count(); }'` (eBPF开发指南 Ch9 §1-3)
  - BPF_MAP_TYPE_ARRAY: 固定大小, 索引为 u32 → 预分配/pre-allocated → 无锁访问 → 适用于固定 slot 场景(per-CPU 数组) → `bpf_map_lookup_elem(&array, &index)` (eBPF开发指南 Ch9 §1-2)
  - BPF_MAP_TYPE_PERCPU_HASH/ARRAY: per-CPU 副本 → 无锁更新 → 读时 sum up → `bpftool map dump` 可见 per-CPU 值 → `@count = count()` 默认用 PerCPU Hash (BPF之巅 Ch2 §5)
  - BPF_MAP_TYPE_PERF_EVENT_ARRAY: 内核态 push 数据到 perf ring buffer → 用户态 `perf_event_poll` 读取 → stream 数据 pipe (eBPF开发指南 Ch9 §5)
  - Maps 操作: 创建(map_create)→查找(lookup)→更新(update)→删除(delete)→迭代(get_next_key)→固定(pin)→持久化(bpffs) (eBPF开发指南 Ch9 §3-4)

### 2. RingBuf — 新一代环形缓冲区
  - 对比 PerfBuffer: RingBuf 支持多生产者无锁写入(compare-and-exchange) → 无内存浪费(PerfBuffer 每 CPU 预分配 page) → 更少唤醒 (eBPF开发指南 Ch9 §6)
  - 写入: 内核态 `bpf_ringbuf_reserve()` 预留空间 → `bpf_ringbuf_submit()` 提交(或 `bpf_ringbuf_discard()` 丢弃) → 原子提交保证"要么全有要么全无" (BPF之巅 Ch2 §5)
  - 读取: 用户态 `ring_buffer__new()` → `ring_buffer__poll()` → callback per event → libbpf 封装, 不需要手写 epoll
  - 适用场景: 高频事件(Ftrace events 级别) → 持续输出 tracing 数据 → trace_pipe 替代 → 比 perf_event 更轻量

### 3. perf_event 数据管道 — 事件型数据通道
  - `BPF_MAP_TYPE_PERF_EVENT_ARRAY` 推送: `bpf_perf_event_output()` 内核态 → 用户态 `perf_event_open()` + mmap → 读环形缓冲区 (BPF之巅 Ch2 §4)
  - BCC 封装: `BPF_PERF_OUTPUT(events)` → `events.perf_submit()` → `b["events"].open_perf_buffer(print_event)` → `b.perf_buffer_poll()` (BPF之巅 Ch4 §3)
  - bpftrace: `printf()` 自动经 perf_event 管道 → 无需手动配置 → 适用快速原型 (BPF之巅 Ch5 §3)
  - 选择: RingBuf(新/libbpf/CO-RE) > PerfEvent(BCC 现状) > trace_pipe(最简单/只测少量数据)

### 4. 调用栈回溯 — 帧指针 + LBR + ORC + 火焰图
  - 帧指针(frame pointer): `rbp` 链 → `gcc -fno-omit-frame-pointer` → 每函数开头 push rbp → `bpftrace -e 'profile:hz:99 { @[kstack] = count(); }'` (BPF之巅 Ch2 §6)
  - LBR (Last Branch Record): Intel PMU 内置 → 记录最近 16-32 次分支 → 零开销获取调用路径 → 适用深层调用链 (BPF之巅 Ch2 §6)
  - ORC (Oops Rewind Capability): 配合 .eh_frame 替代帧指针 → 内核用 ORC 而非 DWARF → `/proc/kallsyms` 映射地址→符号 (BPF之巅 Ch2 §6)
  - 火焰图: `bpftrace` → `stackcount` → Brendan Gregg FlameGraph → svg 交互 → 宽度=CPU时间占比 (BPF之巅 Ch2 §6)
  - 回溯影响: 是否真的需要 frame pointer？ → `-fomit-frame-pointer` 少一个寄存器 = +1-3% 性能 → Cloudflare 等强制启用 → 权衡

### 5. 收束
  - Maps 是 eBPF 程序的"内存" — Hash/Array/PerCPU/PerfEventArray 覆盖 stateful 到 streaming 全场景
  - RingBuf vs PerfBuffer 选择: 新代码一律 RingBuf → PerfBuffer 只有 BCC 旧项目/兼容性
  - 调用栈回溯三种方案是补齐"分布式tracing → 具体哪行代码"的最后拼图

---

### 核心悬念
**"数据能出来了 — 但 eBPF 程序本身怎么写？一个 Hello World 级别的程序到底经历了什么？"**

→ 引出 03-libbpf + CO-RE + skel 开发流程 + 辅助函数 + Golang eBPF
