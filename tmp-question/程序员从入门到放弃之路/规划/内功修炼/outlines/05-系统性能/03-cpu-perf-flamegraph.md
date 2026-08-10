# CPU 性能分析 — IPC/CPI + 缓存 + 分支预测 + perf 工具链 + 火焰图

> Cluster B: 5 KPs | 依赖: 02-benchmark-measurement | 读者基线: 会 `perf stat` 看 CPU cycles

---

### 1. IPC/CPI — 利用率陷阱
  - CPU 利用率 100% 但 IPC < 0.5 = CPU 在空转 stall → 利用率是假象, IPC 才是真指标 (性能之巅 Ch6 §2)
  - IPC > 1(超流水线每周期多条指令) → IPC < 1(流水线停顿 CPU 在等) → `perf stat -e cycles,instructions` (CPU性能分析 Ch4 §1-4)
  - 延时成因分层: Frontend Bound(ICache Miss/ITLB/解码器) vs Backend Bound(加载/存储/算术等) — `perf stat -e cpu/event=0x9c,umask=0x01/` (Intel)
  - %user-%sys 分化: %sys 高→内核路径多(无意义系统调用/context switch/中断风暴) → `perf record -e syscalls:sys_*` / `strace -c`

### 2. 缓存 miss — L1-L3 + TLB
  - L1d/32KB(4 cycles) → L2/256KB(12 cycles) → L3/共享LLC(40-80 cycles) → DRAM(200+ cycles) — 每级 miss 代价翻倍 (CPU性能分析 Ch3 §6-9)
  - `perf stat -e L1-dcache-load-misses,LLC-load-misses ./prog` — 测量 miss rate → LLC miss > 5% 是内存墙信号
  - 缓存行(64B): 相邻变量碰撞→伪共享→cache-line-bouncing(六章详解OS) → `perf c2c` 自动检测
  - TLB: L1 dTLB(64 entries) → L2 STLB(2048) → page walk(5级遍历) → `perf stat -e dtlb_load_misses.miss_causes_a_walk`
  - 优化口诀: "顺序访问→预取友好; 结构体紧凑→减少缓存行消耗; 对齐缓存行→避免跨行访问"

### 3. 分支预测错
  - 分支预测: 历史跳转记录 → 静态1bit/动态PHT/BHT → 预测失败(20 cycle 惩罚 flushing) (CPU性能分析 Ch4 §1-4)
  - `perf stat -e branch-misses,branches ./sort` → 在sort中 branch-misses > 0.5% 已算高
  - 消除分支: 用 `?:` 转为 `cmov`(条件移动/jmp表/SIMD) → 消除无法预测的分支(如随机数据排序 vs 已排序)
  - 排序案例: 已排序数组 `if(a[i] < 128)` 跑 2x 快于未排序 — 分支预测率 ~100% vs ~50%

### 4. perf 工具链 — stat/record/report/annotate/script
  - `perf stat -d -d -d` → 三级扩展事件(cycles→cache→main mem) → `perf list` 列出所有事件 (性能之巅 Ch6 §5)
  - `perf record -F 99 -g -p PID -- sleep 30` → 99Hz 采样防止锁步 → `perf report` 交互式调用树 (性能之巅 Ch13 §1-4)
  - `perf annotate --symbol=func_name` → 源码+汇编混合视图 → 每个指令的 cycles 占比 → 精确到指令行
  - `perf script` → 原始采样文本输出 → 可脚本后处理 → `FlameGraph/stackcollapse-perf.pl` 折叠 → 火焰图用
  - perf events: hardware PMC(LLC-loads)/software(cpu-clock)/tracepoint(syscalls:sys_enter_read)/kprobe/uprobe

### 5. 火焰图 — on-CPU / off-CPU / Differential
  - On-CPU: `perf record -F 99 -g -- sleep 30` → 宽度=某函数栈占比 → 找吃CPU最狠的函数 (性能之巅 Ch6 §6)
  - Off-CPU: 宽度=进程被阻塞(等IO/锁/网络)的时长 → `perf record -e sched:sched_switch -g -p PID` → 找等待在哪里
  - Differential: 两个时间点的火焰图 diff → 红(+)=新增开销 → 蓝(-)=减少 → 回归定位利器
  - 读火焰图的"形状": 平坦(plat)山→宽函数吃光CPU, 针峰→单重函数调用, 多色层→深调用栈

### 6. 收束
  - IPC 是 CPU 的"血压" — 利用率 100% + IPC < 0.5 = CPU 在发烧不是在工作
  - 缓存层级决定数据触达成本, 分支预测决定指令触达成本 — 两项加和就是 CPU"有效工作率"
  - flamegraph 三件套(on/off/diff)是性能分析的 CT/MRI/X 光 — 一个拓扑图定位三层

---

### 核心悬念
**"IPC 告诉你 CPU 闲置, 火焰图告诉你函数占比 — 但 CPU idle 的微观根因在哪？流水线哪一段在等？"**

→ 引出 04-TMA 自顶向下微架构分析 + 屋顶线模型
