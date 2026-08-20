# 容器与 Kubernetes 编排 — 从“换机器就跑不起来”到声明式自愈

> Cluster D: 12 KPs | 依赖: 09-service-mesh、07-microservices-design | 读者基线: Docker、微服务、Service Mesh、Linux namespace/cgroup
> 读者处境: 服务边界和网络治理已经建立；本篇回答如何把应用、配置、网络和副本放到可替换节点上，并由编排器持续恢复声明状态
> 打开新视角: 容器不是轻量 VM，Kubernetes 也不是“启动容器的脚本”；核心是**共享内核隔离 + 不可变镜像 + 声明式控制循环 + 服务发现**

---

### 概念依赖链

```
09 Service Mesh + 07 微服务设计 + 系统编程 namespace/cgroup → 本篇: 容器/Kubernetes
  ├─ §1 容器/VM/镜像(运行时边界)
  ├─ §2 Pod/Service/Deployment/Config(编排对象)
  ├─ §3 Rolling/Blue-Green/Canary(发布策略)
  ├─ §4 镜像构建/分层/Registry
  └─ §5 K8s/Swarm 与声明式控制
先讲: 运行时 → 抽象 → 发布 → 镜像 → 编排选型
后续依赖: 11-cloud-native-patterns(CI/CD/GitOps/弹性)
```

### 叙事顺序

1. 问题引入——服务在 101.132.1.34 上能跑，换节点后为什么失败？
2. 容器与 VM——隔离和启动成本
3. Pod/Service/Deployment——Kubernetes 抽象
4. 发布策略——Rolling/Blue-Green/Canary
5. 镜像与 Registry——不可变交付物
6. K8s/Swarm——声明式控制循环与复杂度
7. 收束

### 1. 容器与虚拟机 — 共享内核与完整 OS 的边界

场景提示: “在我机器上能跑”的服务如何把运行时、依赖和配置变成可搬运交付物？ [写作时展开]

关键设计: 容器通过 Linux namespace/cgroup 等机制隔离进程和资源，VM 则在虚拟硬件上运行完整 guest OS：

```[pseudocode]
Container:
  image filesystem layers
  + namespace(PID/NET/MNT/IPC/USER 等)
  + cgroup(CPU/memory/IO 等限制)
  + runtime
  → 共享宿主机内核

VM:
  guest kernel/userspace
  → hypervisor/virtual hardware
  → 更强 OS 隔离, 启动/资源成本通常更高

image:
  immutable-ish build artifact
  → registry → node pull → container start
```

Why: 为什么容器不能当成“更小的 VM”？——**容器与宿主机共享内核，内核漏洞、系统调用、权限和隔离配置决定边界**；容器也不自动保证无状态、可观测、数据持久化或网络安全。启动时间/内存不能用固定数字代表所有 runtime 和 workload。 [内核: namespace/cgroup 是隔离和资源控制机制，容器安全还依赖 seccomp/capability/LSM 等]

比喻锚点: VM 像每个租户有独立整栋房子和水电系统，容器像同一栋楼里的隔离套间；套间启动快，但共享楼宇基础设施。 [写作时展开]

### 2. Pod、Service、Deployment 与配置 — Kubernetes 的四个视角

场景提示: Pod 会重建、IP 会变化，客户端怎样仍然找到正确服务？ [写作时展开]

关键设计: Kubernetes 用不同对象表达进程共定位、稳定入口、期望副本和配置：

```[pseudocode]
Pod:
  最小调度单元
  → 一个 network namespace
  → 多容器共享网络/部分 volume

Deployment/ReplicaSet:
  声明期望副本与版本
  → controller 创建/替换 Pod
  → readiness/liveness 影响可用状态

Service:
  稳定 DNS/VIP
  → selector/EndpointSlice 找后端 Pod
  → 提供 L4 访问入口

ConfigMap/Secret:
  配置/敏感值与镜像分离
  → env/file/volume 等注入方式
```

Why: 为什么 Service 不只是一个“固定 IP”？——**它是稳定发现与后端选择抽象，Pod 生命周期由控制器管理，Endpoint 会动态变化**；ConfigMap/Secret 也不是自动安全保险箱，权限、加密、轮换和应用 reload 仍需设计。 [容器网络: Pod namespace/veth 与 Service 数据面共同组成访问路径]

比喻锚点: Pod 是一起出差的一组进程，Service 是不会随成员更换而改变的总机号码，Deployment 是负责补齐人员数量的调度员。 [写作时展开]

### 3. Rolling、Blue-Green、Canary — 发布策略是流量与回滚策略

场景提示: v2 版本有 bug，怎样让少量真实流量先验证，再快速回到 v1？ [写作时展开]

关键设计: 发布方式分别控制替换节奏、资源占用和流量切分：

```[pseudocode]
RollingUpdate:
  新 Pod Ready
  → 逐步减少旧 Pod/增加新 Pod
  → maxSurge/maxUnavailable 控制过程

Blue-Green:
  v1/v2 同时运行
  → Service/入口切 selector
  → 快速回滚
  → 需要额外资源与状态兼容

Canary:
  v2 先接小比例/特定用户
  → 观察错误率/P99/业务指标
  → 逐步放量或回滚
```

Why: 为什么 Pod Ready 不等于版本安全？——**进程活着不代表业务正确、依赖兼容、数据迁移安全和尾延迟正常**；灰度必须结合业务指标、数据库 schema 前后兼容、消息版本和回滚路径。 [分布式架构: Service Mesh 可做请求级切分，但不能替代发布验收]

### 4. 镜像构建 — 不可变交付物也需要供应链治理

场景提示: 一个 Java 镜像 1GB，另一个只有 100MB；多阶段构建和分层缓存解决了什么？ [写作时展开]

关键设计: 镜像构建要区分构建环境与运行环境，并让稳定依赖层尽量复用：

```[pseudocode]
多阶段:
  builder: JDK/Maven/编译工具
  → artifact
  runtime: 最小运行时 + artifact

缓存顺序:
  先复制依赖描述
  → 下载依赖
  → 再复制源码
  → 编译/打包

交付:
  build → scan/sign → registry
  → node pull → runtime start

安全:
  非 root、最小权限、漏洞扫描、secret 不烘进镜像
```

Why: 为什么镜像更小不等于更安全？——**基础镜像漏洞、运行时权限、依赖供应链、签名、secret 和可复现构建同样重要**；Alpine/Distroless/不同 libc 也可能带来兼容和调试成本。镜像层共享减少存储/拉取，但不改变运行时内存和应用本身的资源需求。 [云原生: 镜像是交付物，Registry/RBAC/扫描/签名构成供应链边界]

### 5. Kubernetes 与 Swarm — 声明式控制循环的复杂度

场景提示: Docker Compose 能启动几个服务，为什么 Kubernetes 要引入 API Server、Controller、Scheduler 和大量对象？ [写作时展开]

关键设计: Kubernetes 持续比较“期望状态”和“观测状态”，控制器执行修复动作：

```[pseudocode]
用户提交:
  desired replicas/image/service/config
  → API server 持久化对象

scheduler:
  → 选择节点

controller:
  → 创建/删除/重启/滚动替换 Pod

kubelet/runtime:
  → 在节点执行容器生命周期

reconcile loop:
  observed != desired
  → 继续修复直到收敛或报告错误
```

Why: 为什么 Kubernetes 更强也更复杂？——**对象、控制器、网络、存储、权限、升级和观测形成更大的控制平面**；Swarm 更简单但扩展生态和高级调度能力不同。不存在“Swarm 小于某规模、K8s 大于某规模”的固定门槛，团队平台能力和故障成本更重要。 [分布式架构: 声明式系统把运维动作转成控制循环，但控制面本身也是故障域]

### 6. 收束

容器编排闭环：

```[pseudocode]
代码/依赖
  → 镜像/Registry
  → Pod namespace/cgroup
  → Deployment 控制副本
  → Service 稳定发现
  → Rolling/Canary 流量与回滚
  → controller/kubelet 持续 reconcile
```

**Aha Moment**: "Kubernetes 解决的不是“启动容器”一个动作，而是**把不可变交付物、资源隔离、服务发现、版本发布和故障自愈组织成声明式控制循环**；能力越强，控制平面和运维复杂度也越高。"
**回答读者三问**: ①容器为何可搬运=镜像加运行时隔离；②Pod/Service/Deployment 分别做什么=共定位、稳定入口、期望副本；③K8s 为什么复杂=它持续管理网络、资源、版本和故障收敛。

---

### 核心悬念

**"编排解决了部署和自愈，但怎样把代码、配置、测试、灰度、回滚和弹性统一成可重复交付流水线？"**

→ 引出 11-cloud-native-patterns — CI/CD、GitOps、12-Factor、弹性伸缩与 Serverless。