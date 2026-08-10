# 05-系统性能 — 完整性提问

> 5视角 × 5题 = 25题 | 用于检验 8 篇 per-article outlines 的知识覆盖完整性

---

## 1. 开发者视角 — 我写代码时怎么考虑性能？

**Q1-1**: 你写了一个微基准测试, 用 `gettimeofday()` 计时, 跑一次取结果。为什么这个结果不可信？
_(测量陷阱/编译器干扰/统计检查)_

**Q1-2**: 你的函数跑 `perf stat` 显示 `instructions:cycles` = 0.3。IPC 低于 0.5 说明什么问题？你应该看 TMA 的哪个 Bound？
_(IPC/CPI 解释 → Frontend/Backend Bound 诊断)_

**Q1-3**: 你的多线程程序在 4 核上加速 1.2x 而不是 4x — 你怀疑是伪共享还是锁竞争？用什么工具区分？
_(`perf c2c` vs `perf lock report` → 两个不同问题)_

**Q1-4**: `perf record -F 99 -g` 抓到的 on-CPU 火焰图上, `memcpy` 占了 40% 宽度。优化方向应该是什么？
_(分析 memcpy 的栈 → 是内存拷贝还是结构体赋值 → 改数据布局还是 SIMD)_

**Q1-5**: 你写了一个内层循环 `for(i=0;i<N;i++) a[i] += b[i];` — 编译器没向量化。怎么检查为什么？怎么手动修正？
_(`-fopt-info-vec` + `__restrict__` + alignment 保证 + 手动 SIMD intrinsics)_

---

## 2. 性能工程师视角 — 生产环境慢, 我第一步干什么？

**Q2-1**: 凌晨 2 点告警"API 延时 P99 增至 5 秒"。按 USE 方法论, 你前 60 秒的 9 个命令是什么？
_(60秒分析 9 命令: uptime→dmesg→vmstat→mpstat→pidstat→iostat→free→sar n+sar TCP→top)_

**Q2-2**: `vmstat 1` 的 `r` 列显示 0, 但 `await` 在 `iostat -xz` 显示 800ms。这说明什么？你下一步做什么？
_(CPU 不忙但磁盘在堵 → biolatency 看分布 → 可能是文件系统缓存miss 或 SSD 写放大)_

**Q2-3**: RED 方法论和 USE 方法论怎么配合？为什么"均值没意义"？
_(USE找哪台机器哪个资源 → RED找哪个服务哪个端点的延时 + 长尾/P99 masking the mean + 排队理论解释非线_

**Q2-4**: `perf sched latency` 显示进程 A 等了 200ms 才被调度, 但 `vmstat r=1` 只是 1 而已。矛盾在哪？
_(200ms 是单次最大值, r=1 是平均值 → 可能是中断风暴或实时进程抢占导致瞬时饥饿)_

**Q2-5**: 你的服务在 k8s 中被 throttle, `kubectl top pod` 显示 0.5 CPU 但应用很慢。为什么 0.5 不代表实际需求？
_(CFS quota + 请求周期 cfs_period_us → throttle 时段内的延时放大了排队效应 Little's Law)_

---

## 3. SRE/运维视角 — 上线、监控、报警怎么设计？

**Q3-1**: 你用 `sar -n ETCP 1` 看到 `retrans/s=12`。这是正常的还是有问题？怎么进一步诊断？
_(12/s 已经不正常 → `tcpretrans-bpfcc` 看重传原因(丢包/延时/复制) → `ethtool -S` 看网卡丢包)_

**Q3-2**: BBR 和 CUBIC 选哪个？在丢包率 0.2% 的洲际链路上, 你用什么 benchmark 验证？
_(BBR 不依赖丢包信号 → `iperf3 -C bbr` vs `iperf3 -C cubic` 对比 → BBR 可 3x 吞吐)_

**Q3-3**: `free -m` 显示 available 降到 200MB, 但 PSI `full avg10=0.0`。内存真的紧张吗？怎么评估？
_(available 小但无压力, 说明 cache/buff 可以回收, 应用工作集可容纳 → 看 PSI 压力指标才是真紧张)_

**Q3-4**: `ss -lnt` 显示 `Send-Q=128`(somaxconn=128)。连接被拒绝返回客户端 SYN reset。你调 `somaxconn` 值的原则是什么？
_(somaxconn = 2 × 并发瞬时burst → `net.core.somaxconn=4096` → 加上应用层 accept 速度检查)_

**Q3-5**: 你在 dashboard 上想区分 5 种延时(网络/应用队列/锁/磁盘IO/CPU计算)对总体延时的贡献。用什么工具/方法？
_(链路跟踪: tracepoint/net/accept→recv→disk→process→send → `bpftrace` 每阶段计时 → 区分贡献比例)_

---

## 4. 架构师视角 — 系统设计为什么这么选？

**Q4-1**: 为什么 Redis 选择单线程事件循环而不是多线程？单线程在高负载下 CPU 100% 是不是完蛋了？
_(IO密集单线程epoll=完美 → CPU密集(大key/cluster)需多实例 → 把CPU密集任务(持久化)offload 到另一个线程)_

**Q4-2**: Amdahl 定律说加速比有上限, 但 Gustafson 定律说可以线性扩展。为什么一个悲观一个乐观？生产环境信哪个？
_(Amdahl 固定了问题 → Gustafson 问题规模随算力增长 → 信 Gustafson 当负载可平行, 信 Amdahl 当强同步)_

**Q4-3**: 你有一个 Memory Bound 的任务在 `perf stat` 中 `LLC-misses=30%`。你选大页还是 SoA 重排？为什么可能需要两者？
_(大页减少 TLB miss → SoA 减少缓存行浪费 → 两者针对不同层次的 Memory Wall → 合用的效果不是和而是积)_

**Q4-4**: 你的系统每毫秒有 100K RPS 的 HTTP 请求 — 为什么不能每请求做 `perf record`？采样频率 99Hz 的统计意义是什么？
_(perf 采样每 10ms 一次 → 99Hz 避开与定时器/中断锁步 → 采样信号=所有线程上每10ms取一次指令指针 → 统计法则解释)_

**Q4-5**: 你选 Flink/Spark 做实时处理。USE 方法论在你选型中怎么用？
_(预测 work load 特征(CPU/IO/网络)→ 压测每个引擎 → 看哪个引擎 USE 指标更可控 → Roofline 评估是否超出硬件能力→选择_

---

## 5. 学生/初学者视角 — 我该从哪学起？

**Q5-1**: 我刚学完 Linux 基础, 想理解"计算延时"。为什么 CPU 访问 L1(4 cycles)、L3(40 cycles)、DRAM(200 cycles) 差这么多？
_(物理距离×光速≈延迟, 但主要因为硬件协议 → 读 DRAM 需要激活/读取/充电 → 这是第一步理解 Memory Wall 的门槛)_

**Q5-2**: 我用 `top` 看到 CPU 100%, 但老师说利用率会骗人 — 为什么？验证方法是什么？
_(100% != 在做有用功 → `perf stat` 看 instructions/cycles=IPC → IPC >1 是真计算, IPC <0.5 是在等内存/分支错 → 对比)_

**Q5-3**: `vmstat` 和 `iostat` 和 `sar` 有什么区别？我该先学哪个？
_(vmstat=系统整体(CPU+内存+粗略IO), iostat=磁盘, sar=历史全部(CPU+网络+磁盘+调度器) → 先学 vmstat 60秒分析)_

**Q5-4**: 我从没听过"火焰图" — 怎么从 `perf record` 生成火焰图？怎么读它？
_(`perf script | stackcollapse-perf.pl | flamegraph.pl > flame.svg` → 宽度=该函数栈占比, 颜色=热度, 从下到上=调用深度)_

**Q5-5**: 为什么系统性能分析需要同时学方法论(USE/RED)、工具(perf/ftrace/bpftrace)、源码(数据结构/循环/机器码)三个层次？
_(USE=找到问题在哪一层, 工具=定位到具体代码, 源码=真正的修复 → 三层一个都不能缺, 缺一层就是"知道问题但不能修"_
