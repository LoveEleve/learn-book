# 虚拟化 — CPU 虚拟化(VMX/EPT) + 内存虚拟化 + IOMMU/SR-IOV + virtio

> Cluster E: 6 KPs | 依赖: 16-容器原理 + 02-分页机制 | 读者基线: 理解页表遍历和 CPU 保护环概念

---

### 1. CPU 虚拟化 — VMX + VMCS + VM-Exit
  - VMX(VT-x) 基础: VMX root mode(hypervisor) / VMX non-root mode(guest) → VM-Entry(进入 guest) / VM-Exit(guest→host, 因中断/IO/EPT violation) → VMCS(虚拟机关联结构, host/guest 寄存器保存区) (arch/x86/kvm/vmx/vmx.c:4812)
  - VM-Entry: `VMLAUNCH`(首次) / `VMRESUME`(再次) → 从 VMCS 恢复 guest 寄存器 → 设置 VMCS → 进入 non-root mode
  - VM-Exit 原因: EPT violation(页面未映射) / IO 指令(in/out) / MSR 读写 / CPUID / 中断窗口 / HLT → VMCS exit reason 字段 → `handle_exit(exit_reason) → vmx_handle_exit` (arch/x86/kvm/vmx/vmx.c:5480)
  - EPT(Extended Page Tables): GPA→HPA 二级翻译 → guest 页表 GVA→GPA, EPT GPA→HPA → 每个 guest 有自己的 EPT 页表 → 影子页表(软件维护, 已淘汰) → EPT=硬件加速 (arch/x86/kvm/mmu/mmu.c:4401)

### 2. 内存虚拟化 — EPT + 过量分配 + KSM
  - EPT 遍历: guest 页表从 GVA 翻译到 GPA → KVM 截获 EPT violation 时查 EPT → 找不到则分配物理页 → 更新 EPT(GPA→HPA 映射) → 启用 `EPT_AD`(Accessed/Dirty) 标志位, 硬件自动维护 LRU (arch/x86/kvm/mmu/spte.h:116)
  - 内存过量分配(overcommit): 所有 VM 分配的内存总和 > 物理内存 → balloon driver(guest 释放未用内存给 host) → KSM 合并相同页(多 VM 运行相同 OS 时极有效) → THP 减少 EPT miss
  - `intel_iommu=on` 启用 IOMMU → DMA 重映射(设备认为写地址 IOVA 但 IOMMU 翻译到物理地址) → 中断重映射(设备中断路由到指定 vCPU)

### 3. SR-IOV — 一个物理网卡分多个虚拟功能
  - PF(物理功能) / VF(虚拟功能): SR-IOV → 一个物理 NIC 分 64 个 VF → 每个 VF 直接分配给一个 VM(VFIO passthrough) → 无需 hypervisor 中转 IO → 接近裸机性能
  - 设备直通: VFIO(Virtual Function I/O) → 绑定 VF 到 vfio-pci 驱动 → QEMU 通过 `-device vfio-pci,host=02:00.1` 分配 → Intel VT-d / AMD-Vi IOMMU 重映射 DMA (drivers/vfio/pci/vfio_pci_core.c:312)

### 4. virtio — 半虚拟化 I/O 框架
  - 原理: guest 写 virto queue(共享内存环形缓冲区) → kick hypervisor(通知) → host 处理 → inject 中断通知 guest → 无 trap-and-emulate 开销 (drivers/virtio/virtio_ring.c:125)
  - virtio 设备: virtio-blk(块设备) / virtio-net(网络) / virtio-scsi(SCSI) → `vhost` 内核态加速(避免用户态→内核态切换)
  - 对比: 全虚拟化(每次 IO 触发 VM-Exit, 慢) vs virtio 半虚拟化(queue + kick, 接近原生) vs SR-IOV 直通(zero 虚拟化, 接近裸机)

### 5. 收束
  - CPU 虚拟化 = VMX(执行模式) + VMCS(上下文) + VM-Exit(中断捕获) + EPT(二级页表翻译)
  - IO 虚拟化三层: 全虚拟化(VM-Exit 慢) → virtio(queue+kick, 接近原生) → SR-IOV(passthrough, 接近裸机)
  - EPT = 虚拟化内存的性能基础(硬件维护 GVA→GPA→HPA 双级翻译)

---

### 核心悬念
**"容器跑起来了，虚拟机也跑起来了 — 两个进程之间怎么通信？管道/共享内存/Binder IPC 在内核里怎么实现？"**

→ 引出 18-管道/共享内存/消息队列 + Binder IPC + Netlink
