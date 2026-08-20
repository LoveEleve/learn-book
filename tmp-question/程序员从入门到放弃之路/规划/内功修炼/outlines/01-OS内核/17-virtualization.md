# 虚拟化 — CPU 虚拟化(VMX/EPT) + 内存虚拟化 + IOMMU/SR-IOV + virtio

> Cluster E: 6 KPs | 依赖: 16-容器原理 + 02-分页机制 | 读者基线: 理解页表遍历和 CPU 保护环概念
> 读者处境: 已读完 16 篇，知道容器"假隔离"（共享内核）；本篇回答"虚拟机怎么做到'真隔离'（独立内核）——CPU 肯让一个'客机'跑在自己上面？"
> 打开新视角: VMX 的双模式、VM-Exit 的捕获机制、EPT 二级翻译、IO 虚拟化的三层递进（全虚拟→virtio→SR-IOV）

---

### 概念依赖链

```
16-容器(用户态隔离) + 02-分页(页表) → 本篇: 虚拟机(内核级隔离)
  ├─ §1 CPU 虚拟化(VMX/VMCS/VM-Exit — 依赖 02 页表概念)
  │    ├─ §2 内存虚拟化(EPT 二级翻译 — 依赖 §1 + 02 页表)
  │    │    └─ §4 virtio(半虚拟化 IO — 依赖 §1 的 VM-Exit 代价)
  │    └─ §3 SR-IOV(设备直通 — 对照 §4 的折中)
先讲: CPU 模式(VMX) → 内存翻译(EPT) → 设备直通(SR-IOV) → 半虚拟化(virtio)
后续依赖: 18-IPC(虚拟机之间通信)
```

### 叙事顺序

1. 问题引入——虚拟机里跑一个操作系统——它怎么"骗过"CPU 以为自己独占硬件？（**Aha: 虚拟机的本质是 CPU 新增了两种模式——'真内核'和'客内核'**）
   - 过渡: 客机怎么进入/退出？——VM-Entry/VM-Exit
2. CPU 虚拟化——VMX root/non-root；VMCS 上下文；VM-Exit 原因
   - 过渡: 客机的物理内存谁管？——EPT
3. 内存虚拟化——GVA→GPA→HPA 二级翻译；过量分配/balloon/KSM
   - 过渡: 设备访问呢？——三层方案
4. SR-IOV——PF/VF 直通，接近裸机
   - 过渡: 直通快但贵——折中方案？
5. virtio——共享队列 + kick 通知，接近原生
   - 过渡: 三种方案怎么选？——收束
6. 收束——CPU/内存/IO 三层虚拟化全景

### 1. CPU 虚拟化 — VMX + VMCS + VM-Exit

场景提示: VMware 里装 Windows——Windows 内核"以为"自己独占 CPU，实际跑在 Linux 之上的"客机模式"——CPU 怎么分得清？ [写作时展开]

关键设计: x86 的 VMX（VT-x）新增**两种 CPU 模式**：

```[pseudocode]
VMX root mode(hypervisor/KVM 所在) — 真特权
VMX non-root mode(guest 内核) — "看起来特权" 实际受限
VM-Entry: VMLAUNCH(首次)/VMRESUME(再次) — 进入 non-root
VM-Exit: 因中断/IO/EPT violation 退出 — 回 root mode 处理
VMCS: 虚拟机关联结构 — host/guest 寄存器保存区(切换现场)
```

VM-Exit 原因: EPT violation（页面未映射）/IO 指令（in/out）/MSR 读写/CPUID/中断窗口/HLT → VMCS exit reason 字段 → `handle_exit(exit_reason) → vmx_handle_exit`。

Why: 为什么 CPU 愿意"骗"客机？——**关键洞察：敏感指令在 non-root 模式会触发 VM-Exit**（而非悄悄执行或报错）。客机执行特权指令（改 CR3/开中断）→ 自动退出到 hypervisor → hypervisor 模拟后返回——**硬件把"捕获"变成了机制**，hypervisor 只需写"捕获后怎么办"的处理逻辑。没有 VMX 时靠软件模拟（慢且复杂），VMX 让虚拟化成为 CPU 原生能力。 [x86: VMX 是 x86 的硬件虚拟化扩展(VT-x)——AMD 对应 SVM/AMD-V]

比喻锚点: VMX=剧场双舞台——"前台"（non-root）给演员（guest）演出，"后台"（root）给导演（hypervisor）控场；演员一碰"观众席"（敏感指令）就被请到后台（VM-Exit）——观众（硬件）负责"请人"，导演只负责"处理"。 [写作时展开]

### 2. 内存虚拟化 — EPT 二级翻译

场景提示: 客机内核管理"自己的物理内存"（GPA）——但真正的物理内存（HPA）是 hypervisor 管的——两级地址怎么衔接？ [写作时展开]

关键设计: **EPT（Extended Page Tables）**——硬件二级翻译：

```[pseudocode]
guest 页表: GVA(客虚拟) → GPA(客物理)
EPT 页表:   GPA(客物理) → HPA(真物理)
硬件自动遍历两级 → 每 guest 一份 EPT
(影子页表=软件维护, 已淘汰; EPT=硬件加速)
```

- **EPT 遍历**: KVM 截获 EPT violation → 查 EPT → 找不到则分配物理页 → 更新 EPT（GPA→HPA）→ 启用 `EPT_AD`（Accessed/Dirty）标志，硬件自动维护
- **过量分配**: 所有 VM 分配总和 > 物理内存 → **balloon driver**（guest 释放未用内存给 host）→ **KSM** 合并相同页（多 VM 同 OS 极有效）→ **THP** 减少 EPT miss

Why: 为什么需要"两级页表"而非"让客机直接用 HPA"？——如果客机知道真物理地址，就能**访问其他 VM 的内存**（安全崩溃）；GPA 层让每个 VM"以为自己独占内存"，EPT 层由 hypervisor 独家控制映射——**两级的本质是权限分离**：客机管自己的虚拟视图（GPA），hypervisor 管真实分配（HPA），中间由硬件 EPT 翻译并强制隔离。 [内核: EPT 与 02 篇四级页表同构——硬件遍历, 但多一层 GPA→HPA]

比喻锚点: EPT=酒店房间号与真地址——客人（guest）只知道"房间 301"（GPA），前台（hypervisor）才知道"301 对应 8 楼第 3 间"（HPA）；客人永远拿不到真实门牌（隔离），前台换房间（分配）客人无感（GPA 不变）。 [写作时展开]

### 3. SR-IOV — 一个物理网卡分多个虚拟功能

场景提示: 云主机网络要"接近裸机性能"——虚拟化网络的中转开销怎么消灭？ [写作时展开]

关键设计: SR-IOV 让**一个物理网卡直接分成多个虚拟功能**：

```[pseudocode]
PF(物理功能): 物理网卡本体
VF(虚拟功能): 硬件划分的独立网卡(一个 PF 可分 64 个 VF)
每个 VF 直接分配给一个 VM(VFIO passthrough) → 无需 hypervisor 中转 IO → 接近裸机
```

- **设备直通**: VFIO（Virtual Function I/O）→ 绑定 VF 到 vfio-pci 驱动 → QEMU `-device vfio-pci,host=02:00.1` → Intel VT-d/AMD-Vi IOMMU 重映射 DMA

Why: 为什么直通能"接近裸机"？——普通虚拟化 IO 每次都要 VM-Exit 让 hypervisor 中转（慢）；VF 直通让 VM 的驱动**直接操作硬件队列**（DMA 直连），hypervisor 只在配置时介入（运行时零参与）——**"虚拟化"在硬件层完成（网卡自己分出 VF），软件层完全绕开**。代价：VF 数量有限（64）、无热迁移（硬件绑定）。

比喻锚点: SR-IOV=把一辆大巴分成独立出租车位——大巴（PF）上有 64 个固定出租车位（VF），每位乘客（VM）直接开走自己的车（直通），不再需要调度员（hypervisor）安排拼车；代价是车位固定，不能临时换车（无热迁移）。 [写作时展开]

### 4. virtio — 半虚拟化 I/O 框架

场景提示: 直通快但 VF 有限——普通虚拟磁盘/网卡有没有"快且灵活"的方案？ [写作时展开]

关键设计: virtio 用**共享内存环形队列**替代 trap-and-emulate：

```[pseudocode]
guest 写 virtio queue(共享内存环形缓冲区) → kick hypervisor(通知)
→ host 处理 → inject 中断通知 guest → 完成
(无 trap-and-emulate 开销)
```

- **virtio 设备**: virtio-blk（块）/virtio-net（网络）/virtio-scsi（SCSI）→ `vhost` 内核态加速（避免用户态→内核态切换）

IO 三层对比:

| 方案 | 机制 | 性能 |
|------|------|------|
| 全虚拟化 | 每次 IO VM-Exit + 模拟 | 慢 |
| virtio 半虚拟化 | 共享队列 + kick | 接近原生 |
| SR-IOV 直通 | 硬件 VF 直连 | 接近裸机 |

Why: 为什么 virtio 比全虚拟化快？——全虚拟化"**假装**"给客机一个真实设备（每次 IO 都模拟，慢）；virtio"**坦白**"告诉客机"你是虚拟机，用这个共享队列"——客机驱动按队列协议写数据（快），hypervisor 按队列取数据。**半虚拟化的哲学：与其伪装，不如协商**——客机知道自己在虚拟化环境，用高效协议配合，省掉全部模拟开销。

比喻锚点: virtio=内线电话——全虚拟化是"假装有真电话，每次通话都让总机（hypervisor）转接+录音"（模拟）；virtio 是"直接给你分机号，你拨分机对方就接"（共享队列）——不假装、不转接，谈好协议直接通话。 [写作时展开]

### 5. 收束

回到"虚拟机怎么骗过 CPU"：
- CPU = VMX 双模式 + VMCS 上下文 + VM-Exit 捕获
- 内存 = EPT 二级翻译（GVA→GPA→HPA）+ 过量分配三件套
- IO = 全虚拟（慢）→ virtio（快）→ SR-IOV（最快）三层

**Aha Moment**: "虚拟化的本质是'让客机以为独占，但每一步都被记录'——CPU 捕获（VM-Exit）、内存翻译（EPT）、设备协商（virtio）三件事都建立在同一个前提：**硬件配合演戏**。容器是'共享内核装独享'，虚拟机是'独立内核演独享'——一个骗进程，一个骗整个 OS。"
**回答读者三问**: ①VM 为何不崩=VM-Exit 捕获敏感指令；②VM 内存怎么隔离=EPT 二级翻译；③云主机网络为何快=virtio 队列/SR-IOV 直通。

---

### 核心悬念

**"容器跑起来了，虚拟机也跑起来了 — 两个进程之间怎么通信？管道/共享内存/Binder IPC 在内核里怎么实现？"**

→ 引出 18-管道/共享内存/消息队列 + Binder IPC + Netlink——隔离与独立讲完了，接下来是"怎么互相通信"。
