# 性能方法论基础 — USE + RED + 60秒分析 + Amdahl + 排队

> Cluster A: 6 KPs | 依赖: 无 | 读者基线: Linux 基础命令 + 知道"吞吐量/延时"

---

### 1. USE 方法论 — per-resource 三个问题
  - Utilization(使用率): 资源忙的时间占比 → `mpstat %usr/%sys/%idle`, `iostat %util`, `vmstat bi/bo` (性能之巅 Ch2 §3)
  - Saturation(饱和度): 排队等资源的任务数 → `vmstat r/b 列`, `/proc/net/sockstat`, `sar -q runq-sz` (性能之巅 Ch2 §4)
  - Errors(错误数): 资源已经出的错 → `dmesg`, `netstat -s | grep -i err`, `cat /sys/devices/system/edac/mc/mc*/ce_count` (性能之巅 Ch2 §5)
  - USE 检查清单: 每一资源(CPU/内存/磁盘/网络/互连)都要问三问 → 先 U→S→E 排除法定位瓶颈资源

### 2. RED 方法论 — per-service 三个指标
  - Rate: 每秒请求数 → `nginx access.log`, `kubectl top pod`, prometheus `http_requests_total` (性能之巅 Ch2 §3-5)
  - Errors: 失败请求率 → 5xx 响应对全量请求比, gRPC error codes, 熔断器状态
  - Duration: 请求延时分布(均值无用) → P50/P95/P99/分桶直方图 → `wrk2 --latency`, Prometheus Histogram
  - USE vs RED 分工: USE 找哪台机器/哪个资源 → RED 找哪个服务/哪个端点的延时高了

### 3. Linux 60秒分析 — 标准开机检查 9 命令
  - `uptime` 负载均值(1/5/15分钟) → `dmesg -T | tail` 内核错误 → `vmstat 1` 系统整体(CPU/内存/IO) (性能之巅 Ch1 §5)
  - `mpstat -P ALL 1` CPU均衡 → `pidstat 1` 进程消耗 → `iostat -xz 1` 磁盘负载+IOPS+await
  - `free -m` 内存+swap → `sar -n DEV 1` 网络包/吞吐 → `sar -n TCP,ETCP 1` 连接/重传 → `top` 最终overview
  - 执行顺序固定 → 每命令输出量可控 → `60s-checklist.sh` 一次性脚本 → 面试/oncall第一反应

### 4. Amdahl 定律 + 排队理论
  - Amdahl: $S_{\max} = \frac{1}{(1-P)} $ → P=并行占比 → 并行50%上限仅2x → 决定"优化上限在哪" (性能之巅 Ch2 §6)
  - Gustafson定律对比: Amdahl 问"加速上限(固定问题)?" — 悲观主义; Gustafson 问"同样时间能处理多少(扩展问题)?" — 乐观主义 (B1 Ch2 §6)
  - 通用扩展定律: 通信/同步开销随 N 增长 → 总有反弯点 → `perf stat -e LLC-load-misses,cache-misses` 当并行瓶颈
  - Little's Law: $L = \lambda W$ → 系统中平均请求数 = 到达率 × 平均延时 → 容量规划基础 (性能之巅 Ch2 §6)
  - M/M/1 排队: 使用率 50%→2x延时, 90%→10x, 99%→100x → 延时非线性 → 队列就是延时放大

### 5. 工作负载特征归纳 + 可视化
  - 特征归纳: 谁产生的负载(who)→为什么(why)→什么构成(what I/O/CPU/网络比例)→怎么变(how随时间) (性能之巅 Ch2 §3)
  - 向下钻取(drill-down): 从 USE 找到的瓶颈资源往下逐层(系统→子系统→组件→模块→函数)定位
  - 线图: 时间趋势(CPU利用率变化) → 热图: 多维度(时间×延时计数, 一眼看出P99峰值) (性能之巅 Ch2 §9-10)
  - 散点图: 两维度关系(concurrency vs latency) → 火焰图: 调用栈采样堆叠(CPU/内存/off-CPU 三个维度)

### 6. 从工具到可观测平台 — 日志 + 指标 + 仪表板
  - 日志设计: 时间戳/RPC ID/结构化格式/文件大小管理 — 离线性能分析的数据源 (B2 Ch8 §1-2)
  - 聚合指标: 直方图/时间线/P50-P99/更新间隔/事务采样 — RED 的量化基础 (B2 Ch9 §1-2)
  - 仪表板: 主仪表板/单实例板/服务板/健康检查板 — 持续观测 vs 一次性诊断 (B2 Ch10)
  - 关系: 这是 B2 的独有贡献 — 将性能分析从"跑 perf"扩展到系统性可观测工程

### 7. 收束
  - USE(找资源) + RED(找服务) 覆盖"哪层哪机器哪资源"的全栈定位
  - 60秒分析是上机第一步, Amdahl 定上限, Little's Law 定容量, 排队理论解释"非线性的延时惩罚"
  - 可视化不是装饰 — 线图/热图/火焰图是性能领域的三把手术刀, 分别看趋势/分布/栈

---

### 核心悬念
**"U/S/E 三问找到了瓶颈资源, 然后呢？你怎么相信自己测量的数字是真的？"**

→ 引出 02-基准测试方法论 + 统计检查 + 测量陷阱
