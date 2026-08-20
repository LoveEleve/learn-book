# 容器原理 — 7 种 Namespace + Cgroups v1/v2

> Cluster E: 7 KPs | 依赖: 11-进程模型 + 02-分页机制 | 读者基线: 理解进程、文件系统和网络基础
> 读者处境: 已读完 11 篇（进程模型）和 02 篇（分页）；本篇回答"Docker 把一个进程'关'进小隔间——怎么做到它看不到宿主、也用不完宿主资源？"
> 打开新视角: 隔离（Namespace）与限制（Cgroup）的本质区别、七种隔离维度、overlay 文件系统、v1→v2 的演进原因

---

### 概念依赖链

```
11-进程(克隆/共享) + 02-分页(地址空间) → 本篇: 容器隔离与限制
  ├─ §1 七种 Namespace(视角隔离 — 依赖 11 的 clone 机制)
  │    └─ §4 容器启动流程(组合 Namespace+overlay+Cgroup — 依赖 §1/§2/§3)
  ├─ §2 Cgroups v1(资源限制: 多层级 — 依赖 10 的 CPU 控制)
  │    └─ §3 Cgroups v2(统一层级 — 对照 §2 的缺陷)
先讲: 隔离(七种) → 限制(v1) → 演进(v2) → 组装(容器启动)
后续依赖: 17-虚拟化(容器 vs 虚拟机的边界)
```

### 叙事顺序

1. 问题引入——`docker run` 一个进程——它和宿主进程有什么"看不见的区别"？（**Aha: 容器不是"新东西"，是一个进程加上七层'眼罩'和四套'枷锁'**）
   - 过渡: 七层眼罩各遮什么？
2. 七种 Namespace——UTS/PID/NET/MNT/IPC/USER/CGROUP + 三个系统调用
   - 过渡: 看不见还不够——怎么限制"看得见"的资源？
3. Cgroups v1——12 controller；memory/cpu/blkio 三剑客
   - 过渡: v1 的多层级有什么问题？
4. Cgroups v2——统一层级；v1 冲突的解决
   - 过渡: 眼罩+枷锁都有了——Docker 怎么组装？
5. 容器启动流程——docker run → runc → namespace → overlay → pivot_root → cgroup
   - 过渡: 完整图景已齐——收束
6. 收束——Namespace=视角 / Cgroup=资源 / overlay=文件 / veth=网络

### 1. 七种 Namespace — 进程的七层隔离屏障

场景提示: 容器里的 `ps` 只看到自己的进程、`hostname` 是自己的名字——同一个内核，凭什么"视角不同"？ [写作时展开]

关键设计: Namespace 让**同一内核的不同进程看到不同的系统视图**——每种 CLONE_NEW* 隔离一个维度：

| Namespace | 隔离什么 | 容器内表现 |
|-----------|---------|-----------|
| UTS | 主机名/域名 | `hostname` 独立 |
| PID | 进程树 | 容器内 PID 1、`ps` 只见自己 |
| NET | 网卡/路由/iptables | 独立 lo、独立网络栈 |
| MNT | 挂载点 | 独立根文件系统 |
| IPC | System V IPC/消息队列 | `ipcs` 不见宿主队列 |
| USER | UID/GID 映射 | 容器内 root ≠ 宿主 root |
| CGROUP | cgroup 视图 | `/proc/self/cgroup` 只见自己 |

创建方式: `clone(CLONE_NEW*)`（继承+新建）/ `unshare(CLONE_NEW*)`（分离）/ `setns(fd)`（加入已有）——Docker 用 clone + setns 组合。 [内核: Namespace 复用 11 篇的 clone 标志位机制——CLONE_NEW* 是 clone 的扩展标志]

Why: 为什么叫"隔离视角"而非"隔离资源"？——Namespace 只改变"**看到什么**"（进程/网络/挂载的可见性），不限制"**用多少**"（那是 Cgroup 的事）。PID namespace 让容器进程"以为自己是 PID 1"（实际宿主 PID 3000+）——**视角欺骗是容器的最小单元**：进程以为自己独享系统，其实共享内核。

比喻锚点: Namespace=给每位员工发不同的"工作证+楼层图"——都在这栋楼（同一内核）上班，但 A 员工的地图只显示 3 层（PID 视角），B 只显示 5 层；楼层图不同（视角不同），楼是同一栋（共享内核）。 [写作时展开]

### 2. Cgroups v1 — 多层级资源控制

场景提示: `--memory=512m --cpus=2`——容器怎么被"限量"？超了会怎样？ [写作时展开]

关键设计: Cgroup（Control Group）限制资源用量——12 个 controller 各管一类资源，核心三剑客：

| controller | 关键文件 | 语义 |
|-----------|---------|------|
| memory | `memory.limit_in_bytes` / `usage_in_bytes` / `stat` / `oom_control` | 硬限制，超限触发 OOM（杀进程或暂停） |
| cpu | `cpu.shares`（权重）/ `cfs_quota_us`（配额）/ `cfs_period_us`（周期 100ms） | `--cpus=2` = 200000/100000（每 100ms 最多 200ms） [内核: cfs_quota 限制基于 10 篇的 CFS 调度——配额到期的进程被 throttle] |
| blkio | `blkio.throttle.read_bps_device` / `write_iops_device` | 磁盘带宽/IOPS 限流 |

Why: 为什么资源限制需要"cgroup 文件系统"？——一切皆文件：cgroup 的层级=文件系统目录树（写文件=设限制，读文件=看用量）；`echo 512M > memory.limit_in_bytes` 就是设限。**限制变成文件操作**——用户态无需新 API，`ls/echo/cat` 即可管理容器资源。

比喻锚点: Cgroup=食堂饭卡——每个容器一张卡（cgroup 节点），卡上写"每天 512g 饭、2 小时灶台"（limit 文件）；食堂（内核）按卡限量打饭，卡里余额（usage 文件）实时可查。 [写作时展开]

### 3. Cgroups v2 — 统一层级

场景提示: v1 的 CPU 和 cpuset 各自独立挂载——冲突了怎么办？ [写作时展开]

关键设计: v2 用**单一 `cgroup2` 文件系统**统一所有 controller：

| 项 | v1 | v2 |
|----|----|----|
| 层级 | 每 controller 独立挂载（可冲突） | 单一层级统一管理 |
| cpu 限制 | shares + cfs_quota 两个文件 | `cpu.max "$MAX $PERIOD"` 一个 |
| 内存限制 | memory.limit_in_bytes | `memory.max` |
| 子控制器 | 自动继承 | `subtree_control` 显式控制 |

改进: 无内务 controller（subtree_control 控制）/ 压力通知（PSI）/ `cgroup.stat`。**v1 仅兼容保留，v2 是 Docker/K8s 推荐**。

Why: 为什么 v1 的"多层级"是缺陷？——v1 每个 controller 独立挂载 = 同一进程可能属于 CPU 层级 A、cpuset 层级 B——**进程与层级的对应关系分裂**（A 说 2 核、B 说 4 核，谁算数？）。v2 单一层级让"一个进程一个 cgroup 节点，所有 controller 挂在一个节点上"——**一致性是资源控制的根基**（否则两个控制器对同一进程给出矛盾限制）。

比喻锚点: v1=一家公司两个互不通气的 HR 系统——一个系统记你工资 2 万、另一个记 4 万（CPU 限制冲突）；v2=合并成一套系统——所有部门（controller）看同一个员工档案（单一层级）。 [写作时展开]

### 4. 容器启动流程 — Docker 作为 Namespace+Cgroup 编排器

场景提示: `docker run nginx`——这一条命令背后，内核被"安排"了哪些事？ [写作时展开]

关键设计: Docker 启动链：

```[pseudocode]
docker run → containerd → runc
→ 创建 Namespace(unshare CLONE_NEW*) → 挂载 overlay(容器层+镜像层)
→ pivot_root(换根) → 设置 Cgroup 限制 → 启动 entrypoint
→ 进程在隔离空间运行
```

**Overlay 文件系统**: lowerdir（镜像基础层，只读）+ upperdir（容器可写层）+ merged（合并视图）→ **写时拷贝**（修改只读层文件时先复制到 upperdir）→ 多个容器共享基础层。

Why: 为什么 overlay 是容器的文件系统关键？——镜像层（lowerdir）只读共享（100 个容器共享同一基础层 = 磁盘只占一份）；容器修改时"拷贝-修改"（COW，与 03 篇内存 COW 同思想）——**共享只读层 + 私有可写层**让容器启动秒级（不用复制整个镜像）。pivot_root 换根让容器"以为" merged 视图就是全部文件系统。

比喻锚点: overlay=合租公寓的公共区+私人间——镜像层是公共客厅（所有人共享，只读）；容器是各自的私人间（upperdir 可写）；往公共区墙上钉钉子（改只读层文件）会先复制一块墙到自己房间再钉（COW）。 [写作时展开] [内核: overlay 的 COW 与 03 篇内存 COW 同思想——只读共享+写时复制]

### 5. 收束

回到 `docker run` 的完整图景：
- Namespace = 七层眼罩（视角隔离）
- Cgroup = 资源枷锁（限量使用）
- Overlay = 文件系统（共享只读+私有可写）
- veth+bridge = 网络（veth pair 连接容器到 bridge）

**Aha Moment**: "容器不是'新东西'——它就是一个普通进程，被戴上了七层眼罩（Namespace：看不见宿主）、四套枷锁（Cgroup：用不完宿主）、换了一副眼镜（overlay：以为 merged 是全部文件系统）。'容器'这个名字误导了我们：它从来不是虚拟机，只是一个'自以为独占'的进程。"
**回答读者三问**: ①容器 `ps` 为何只见自己=PID namespace；②`--cpus=2` 怎么限=cfs_quota/period；③镜像为何不占 100 份=overlay 共享只读层 COW。

---

### 核心悬念

**"容器隔离了用户态 — 那虚拟机怎么隔离内核？CPU 虚拟化(VMX/VT-x)和 EPT 二级页表翻译是怎么做到的？"**

→ 引出 17-CPU 虚拟化(VMX/EPT) + 内存虚拟化 + IOMMU/SR-IOV + virtio——容器是"假隔离"（共享内核），虚拟机是"真隔离"（独立内核）。
