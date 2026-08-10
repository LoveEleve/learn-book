# 容器原理 — 7 种 Namespace + Cgroups v1/v2

> Cluster E: 7 KPs | 依赖: 11-进程模型 + 02-分页机制 | 读者基线: 理解进程、文件系统和网络基础

---

### 1. 七种 Namespace — 进程的七层隔离屏障
  - UTS: 主机名/域名隔离(`hostname` 命令在容器内独立) → CLONE_NEWUTS → Unix Time-sharing System 遗留 (kernel/utsname.c:98)
  - PID: 进程树隔离 → 子 namespace 的 PID 1 → `/proc` 显示新视角(只显示本 ns 进程) → `pid_for_children`(子 ns 的 PID) → 容器内 `ps aux` 只看到自己 (kernel/pid_namespace.c:127)
  - NET: 网卡/路由/iptables 隔离 → veth pair 连接 ns 到 bridge(如 docker0) → 容器有独立 lo 网卡 → `ip netns` 管理 (net/core/net_namespace.c:365)
  - MNT: 挂载点隔离 → `pivot_root` 切换根文件系统 → overlay(容器镜像层, 写时拷贝) → `mount -t overlay` → 容器看到的文件系统隔离于宿主 (fs/namespace.c:2280)
  - IPC: System V IPC/POSIX 消息队列隔离 → `ipcs` 看不到宿主队列 → 容器内不能互相操作共享内存 (ipc/namespace.c:67)
  - USER: UID/GID 映射 → 非 root 用户在容器内可映射为 root → `/proc/PID/uid_map`(用户映射) → 安全关键(容器内 root≠宿主 root) (kernel/user_namespace.c:790)
  - CGROUP: cgroup namespace(v4.6+) → `/proc/self/cgroup` 显示路径仅当前 cgroup → 阻止容器看到宿主 cgroup 结构 (kernel/cgroup/namespace.c:12)
  - 系统调用: `clone(CLONE_NEW*)`(继承) / `unshare(CLONE_NEW*)`(分离) / `setns(fd)`(加入) → Docker 用 `clone + setns` 组合创建容器

### 2. Cgroups v1 — 多层级资源控制
  - 12 controller: cpu/memory/blkio/net_cls/net_prio/devices/freezer/hugetlb/pids/perf_event/rdma/cpuset → cgroupfs 层级挂载(每个 controller 可独立挂载) (include/linux/cgroup-defs.h:91)
  - memory cgroup: `memory.limit_in_bytes`(硬限制) / `memory.usage_in_bytes`(当前使用) / `memory.stat`(详细统计: cache/rss/swap) / `memory.oom_control`(OOM 行为) → 超限触发 OOM(杀进程或暂停) (mm/memcontrol.c:5138)
  - cpu cgroup: `cpu.shares`(权重, 空闲 CPU 按比例分配) / `cpu.cfs_quota_us`(配额, 每个周期最大微秒) / `cpu.cfs_period_us`(周期, 默认 100ms) → `docker run --cpus=2` → 设置 200000/100000 (kernel/sched/core.c:7456)
  - blkio: `blkio.throttle.read_bps_device`(读带宽限制) / `blkio.throttle.write_iops_device`(写 IOPS) → `docker run --device-read-bps=/dev/sda:1mb`

### 3. Cgroups v2 — 统一层级
  - 单一 `cgroup2` 文件系统 => 所有 controller 统一管理 → 一致性 → `cpu.max "$MAX $PERIOD"`(替代 v1 cpu.shares + cpu.cfs_quota) → `memory.max`(替代 memory.limit_in_bytes) (kernel/cgroup/cgroup.c:1756)
  - 改进: 无内务 controller(subtree_control 控制) / 压力通知(PSI) / `cgroup.stat`(nr_descendants) → v2 已被 Docker 和 K8s 推荐
  - 与 v1 对比: v1 CPU/cpuset 两个 hierarchy(冲突) → v2 统一(解决冲突) → v1 仅兼容保留

### 4. 容器启动流程 — Docker 作为 Namespace+Cgroup 编排器
  - Docker 启动: `docker run` → containerd → runc → 创建 Namespace(unshare CLONE_NEW*) → 挂载 overlay(容器层+镜像层) → pivot_root(换根) → 设置 Cgroup 限制 → 启动 entrypoint → 进程在隔离空间运行
  - Overlay 层: lowerdir(镜像基础层, 只读) + upperdir(容器可写层) + merged(合并视图) → 写时拷贝(修改只读层文件时拷贝到 upperdir) → 多个容器共享基础层

### 5. 收束
  - Namespace = 隔离视角(进程看不到宿主的/挂载/网络) / Cgroup = 限制资源(CPU/内存/IO 有上限)
  - 七种 Namespace 按 CLONE_NEW* 创建 → overlay 提供文件系统隔离 → Cgroup v2 提供统一资源控制
  - Docker = Namespace(隔离) + Cgroup(限额) + overlay(文件系统) + veth+bridge(网络) 的四合一组装

---

### 核心悬念
**"容器隔离了用户态 — 那虚拟机怎么隔离内核？CPU 虚拟化(VMX/VT-x)和 EPT 二级页表翻译是怎么做到的？"**

→ 引出 17-CPU 虚拟化(VMX/EPT) + 内存虚拟化 + IOMMU/SR-IOV + virtio
