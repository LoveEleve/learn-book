# 06-eBPF — 全视角完备性提问 (5 身份 × 4-5 题 = 22 题)

> 验证方法: 每个身份问 Why/What 非 How, 每题必须能用大纲中 1-3 篇的内容答出
> 书: B1 BPF之巅(20KPs) + B2 eBPF与可观测性(13KPs) + B3 eBPF开发指南(13KPs) = 46KPs / 7篇大纲

---

## 1. 开发者视角 (Developer) — 4 题

Q1-1: BTF+CO-RE 说"一次编译, 随处运行" — 那 CO-RE 在加载时到底重定位了什么？不是 JIT 才编译成机器码吗, 重定位和 JIT 的时间顺序是什么？
   → 覆盖: 01 (BTF/CO-RE) + 03 (libbpf 加载流程)

Q1-2: bpftrace 写了 `kretprobe:vfs_read { @bytes = hist(retval); }` — 这个 @bytes 映射表在内核态还是用户态？print 时数据什么时候从内核搬到用户态的？
   → 覆盖: 02 (Maps/ringbuf perf_event 管道) + 04 (bpftrace 映射表 @)

Q1-3: bpf2go 生成的 Go 文件, 怎么知道哪一个 struct 对应哪一个 map？ebpf-go 的 `LoadAndAssign` 到底怎么找到我的 map 引用的？
   → 覆盖: 03 (Golang eBPF: ebpf-go + bpf2go) + 02 (Map 创建和命名)

Q1-4: 我想在 uprobe:malloc 里记录分配的调用栈(`kstack`), 这个 `kstack` 在内核态读取 — 但 malloc 在用户态。kstack 说的是内核调用栈还是用户态调用栈？能同时拿到吗？
   → 覆盖: 02 (调用栈回溯: kstack/ustack/帧指针/LBR/ORC) + 05 (uprobe 原理)

---

## 2. 性能工程师视角 (Performance) — 5 题

Q2-1: 一个服务延时 P99 突然从 50ms 跳到 800ms, 你已知是用 USE 找到了 CPU 使用率 85%。第一步该跑什么 bpftrace 命令区分"CPU bound"还是"I/O bound"而不是直接看 syscall 表？
   → 覆盖: 06 (cpudist 区分 CPU/I/O bound) + 01 (60秒方法论→关联 05域)

Q2-2: profile 火焰图顶部是 `tcp_sendmsg` 占了 40% CPU — 这是"CPU 在等网络"还是"网络快但 CPU 在做拷贝"？怎么用 runqlat 验证假设？
   → 覆盖: 06 (profile + runqlat 组合 + offcputime) + 07 (网络 tcpconnect/tcplife)

Q2-3: cachestat 显示 BUFMISS 90% — 说明页缓存命中率为 10%。这个时候加内存有用吗？memleak 显示无泄漏但 vmstat 的 si/so 很高 — 加内存 vs 优化访问模式怎么决策？
   → 覆盖: 06 (cachestat/memleak/vmscan) + 05域 (Amdahl/Gustafson/Little's Law → 关联)

Q2-4: tcpretrans 显示重传率 15% — 但 `netstat -s` 没看到丢包计数上升(drop=0)。重传和丢包的关系是什么？tcpretrans 计数除了丢包还有什么原因？
   → 覆盖: 07 (tcpretrans 类型: Lost/SackAll/LostRetransmit)

Q2-5: 你对一个进程做 `profile:99hz` 5 分钟后, 火焰图最高的函数是 `unix_stream_read_actor` 2% — 这意味着什么？为什么 2% 就是最高但仍然可能是瓶颈？
   → 覆盖: 01 (采样理论) + 06 (offcputime 找阻塞) + 火焰图解释

---

## 3. SRE 视角 (SRE) — 4 题

Q3-1: 你对生产容器跑 `bpftrace -e 'kprobe:tcp_v4_connect { printf("%d -> %d\n", pid, kstack()); }'` — 这对用户请求有性能影响吗？kprobe 和 tracepoint 的开销分别是多少, 什么场景下不能用 kprobe？
   → 覆盖: 05 (追踪源选择: 开销对比 kprobe ~100ns vs tracepoint ~50ns)

Q3-2: 凌晨 3 点被叫起来看 OOM kill — 看到 killed pid=X comm=java。你要排因, 先跑 oomkill 再跑 memleak 还是先 cachestat 再 faults？顺序是什么？
   → 覆盖: 06 (oomkill/memleak/faults/cachestat 四步诊断法)

Q3-3: bashreadline 能实时看用户的 bash 命令 — 那这个 uprobe 挂载点 `/bin/bash:readline` 在 bash 更新后会失效吗？怎么在 CI 中自动验证 u probe 的有效性？
   → 覆盖: 05 (uprobe 选择指南 + USDT vs uprobe 稳定性) + 07 (bashreadline)

Q3-4: 一个 Kubernetes Pod 被 NetworkPolicy deny 了 — 你能用 eBPF 在哪个 hook 点看到被拒绝的包吗？TC hook 能看到但 XDP 能吗？区别？
   → 覆盖: 07 (XDP vs TC hook 层差异) + 07 (Cilium NetworkPolicy 实现)

---

## 4. 架构师视角 (Architect) — 5 题

Q4-1: eBPF 验证器保证 BPF 程序不会 crash — 但 BPF 程序能不能死循环把 CPU 打满？验证器做了什么来防止这个？
   → 覆盖: 01 (验证器: DAG 无环 + 最大指令数 + 无后跳)

Q4-2: BTF+CO-RE 解决了"不同内核版本"的兼容 — 那如果目标机器不支持 BTF(老内核), eBPF 还有办法跑吗？退化路径是什么？
   → 覆盖: 01 (BTF 退化至旧 BPF 方式: per-kernel 编译或 BCC 运行时编译)

Q4-3: ebpf-go(纯 Go)和 libbpf(C)的 trade-off 是什么？如果以后要支持 arm64/risc-v/loongarch 多平台, 哪个方案更合适？
   → 覆盖: 03 (libbpfgo vs ebpf-go vs bpf2go 对比)

Q4-4: XDP 在网卡驱动层执行(DDoS 防御) — 但万一你的 XDP 程序有 bug 把正常流量也 DROP 了, 怎么回滚？XDP 有热更新吗？
   → 覆盖: 07 (XDP 返回码/attach 方式/热更新 `bpftool net detach`)

Q4-5: 七个模块的依赖顺序是 A→B→C — 如果一个新来的只关心"生产网络排障"不需要写任何 eBPF 程序, 最少读哪几篇就能用 bpftrace 处理网络问题？
   → 覆盖: 全 7 篇(最小路径: 01→04→07) — 验证大纲结构的可拆分性

---

## 5. 学生视角 (Student) — 4 题

S5-1: eBPF 程序加载时验证器检查了"不允许无限循环" — 那我在 bpftrace 里写 `i:1 { if (1) { @count = count(); }}` 这种"每一次调用都计数"是无限循环吗？
   → 覆盖: 01 (验证器 + 指令限制) + 04 (bpftrace 触发式非循环)

S5-2: BCC 的 `BPF(text='...')` 和 libbpf 的 skeleton 生成 — 它们都是"把 C 变成能跑的 eBPF"但过程完全不同。区别在哪一步？
   → 覆盖: 03 (BCC 运行时编译 vs libbpf 预编译 + CO-RE 重定位)

S5-3: bpftrace 的 `@count = count()` 和 Cilium/eBPF 写的 `bpf_map_update_elem()` 是同一个 Map 概念吗？bpftrace 隐藏了多少手写代码？
   → 覆盖: 02 (Maps: 创建/类型/Hash/Array/per-CPU) + 04 (bpftrace @ 映射表魔法)

S5-4: 整个 eBPF 域你最震惊的两个事实是什么？至少一个与"为什么不用内核模块"不同。
   → 覆盖: 全 7 篇 — 验证教学叙事(场景→源码路径→关键设计→数据流)是否能产生"aha moment"
