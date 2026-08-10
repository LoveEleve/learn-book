# 子系统深度观测 — 内存 / 磁盘 I/O / 网络 / 调度器

> Cluster C: 6 KPs | 依赖: 01-methodology-foundation | 读者基线: 懂 USE 方法论 + 会 `vmstat` `iostat`

---

### 1. 内存观测 — 从应用分配器到物理压力
  - `vmstat 1`: free/buff/cache/si/so/bi/bo → si/so>0=触到swap(严重) → cache 突然下降=内存回收在挤压 (性能之巅 Ch7 §5)
  - `slabtop`: 内核 slab 分配器缓存排名 → dentry/inode 缓存消耗 → `echo 3 > /proc/sys/vm/drop_caches` 清理
  - `pmap -x PID` → 进程每一段的 Resident/Dirty/Shared → mmap/堆/栈/BSS/text 分别占多少
  - PSI 压力失速信息(内核5.2+): `cat /proc/pressure/memory` → avg10/avg60/avg300(内存压力失速时长百分比) → 传统指标缺失的"排队延时时间"
  - NUMA 感知: `numastat -p PID` → 本地 vs 远端分配比例 → 远端>20%就需调优
  - 内存泄漏检测: `valgrind --leak-check=full` + `memleak-bpfcc -p PID` (BCC工具) 跟踪 `kmalloc/kfree` 不配对

### 2. 磁盘 I/O — IOPS/带宽/使用率/延时
  - `iostat -xz 1`: %util+r/s+w/s+rMB/s+wMB/s+await+r_await+w_await → await>20ms 或 %util>80% 排查 (性能之巅 Ch9 §2-5)
  - `biolatency-bpfcc 10 1`: IO延时分布直方图 → P99读/写延时 → SSD写放大的P99延时可到秒级, 均值完全不可见 (性能之巅 Ch9 §2-5)
  - IOPS vs 带宽: 小IO看IOPS上限(SSD~100K, HDD~100) → 大IO看带宽上限(SSD~3GB/s, HDD~150MB/s) → `fio --rw=randread --bs=4k --iodepth=32`
  - 利用率谜题: 多Device的%util串行化 → 镜像盘%util会翻倍但实际只做一次 → `iostat -x` 的 `%util` 不是直接的使用率 (性能之巅 Ch9 §2)
  - 文件系统开销: 同步写(write+fsync)/日志回写 → `ext4`/`xfs` → `mount -o nobarrier` 风险高

### 3. 网络性能 — 连接队列/缓冲区/重传/调优
  - 连接队列: `ss -lnt` → Recv-Q(内核积压未accept的连接)/Send-Q → accept 队列满 `net.core.somaxconn` (性能之巅 Ch10 §2-5)
  - TCP 重传: `sar -n ETCP 1` → retrans/s>0 了 → `tcpretrans-bpfcc` 跟踪重传及原因(丢包/延迟/复制)
  - 缓冲区: `net.core.rmem_max/wmem_max` + `TCP autotuning` → 默认值512K/4MB → `iperf3` 测试
  - `ss -tiep`: tcp_info(cwnd/ssthresh/rtt/rttvar/pacing_rate/total_retrans) → 每个连接的健康状态
  - `ethtool -S eth0`: 网卡硬件offload(TSO/GRO/GSO)统计 → rx_missed_errors(网卡丢包)/rx_crc_errors(物理层错误)
  - 调优套件: BBR拥塞控制(`net.ipv4.tcp_congestion_control=bbr`) vs CUBIC → `iperf3 -C bbr` 3x 吞吐

### 4. 调度器调优
  - CFS: `perf sched latency` → 进程等待CPU时间(延迟最大/平均数/最大延迟PID) (性能之巅 Ch3 §1-4)
  - 上下文切换成本: `perf stat -e context-switches -p PID` → 每秒>1000 需分析 → `pidstat -w` 看自愿(等IO) vs 非自愿(时间片用完)
  - 中断均衡: `/proc/irq/N/smp_affinity` → `irqbalance` 自动分发 → 但高吞吐网卡应该手动绑定独占CPU核
  - 调度策略: `chrt -r -p 99 PID` 设置FIFO(实时)/`schedtool -B PID` 设置为batch(后台) → CPU隔离(`isolcpus`)

### 5. 收束
  - 内存观测四层: 物理分配(slabtop)→系统压力(PSI)→进程地址(pmap)→NUMA感知(numastat)
  - 磁盘IO别只看%util: IOPS/带宽/延时分布三平行 → P99延时才是用户体验
  - 网络60秒三板斧: `ss -lnt`(队列)+`sar -n ETCP`(重传)+`ethtool -S`(硬件丢包)

---

### 核心悬念
**"vmstat/iostat 是计数器聚合, 只能告诉你'某个子系统出问题了'— 但为什么出问题、谁出的问题、在哪条路径上出的问题？你需要跟踪。"**

→ 引出 06-Ftrace + BCC + bpftrace: 从计数器到代码路径跟踪
