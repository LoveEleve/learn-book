# 容器网络 — Namespace、veth、Service 与 CNI 如何把 Pod 接到网络

> Cluster F: 4 KPs | 依赖: 08-epoll-reactor、10-netfilter-nat | 读者基线: Linux 网络栈、Netfilter、NAT、事件循环
> 读者处境: 13 篇已经走完 DNS/CDN/WebSocket；本篇回答容器里的进程怎样拥有独立网络栈、Pod 怎样互通、Service 怎样找到后端、CNI 怎样把这些组件装配起来
> 打开新视角: 容器网络不是一种单独协议，而是**namespace 隔离 + veth 连接 + bridge/路由转发 + NAT/负载均衡 + CNI 编排**的组合

---

### 概念依赖链

```
08 epoll + 10 Netfilter/NAT → 本篇: 容器网络
  ├─ §1 network namespace(隔离网络栈)
  ├─ §2 veth/bridge(连接容器与宿主机)
  ├─ §3 kube-proxy(Service数据面)
  ├─ §4 Pod/Node/外部访问(同节点/跨节点)
  ├─ §5 Service/Ingress(L4/L7)
  └─ §6 CNI/VXLAN(插件装配与跨节点封装)
先讲: 隔离 → 接线 → Service 转发 → Pod 路径 → L7 → CNI
后续依赖: 系统性能观测(网络 CPU/软中断/队列如何测量)
```

### 叙事顺序

1. 问题引入——容器里的进程为什么看到一块独立网卡，却能访问宿主机和其他 Pod？（**Aha: 容器网络是把多个“独立小网络栈”用虚拟设备和规则重新接成一张逻辑网**）
2. network namespace——先隔离路由表、设备和 socket
3. veth pair + bridge——再把隔离栈接到宿主机
4. kube-proxy——ClusterIP 如何转发到 Pod
5. Pod/Service/Ingress——同节点、跨节点和 L7 路径
6. CNI/VXLAN——谁负责创建和配置这些网络组件
7. 收束——从进程到 Pod IP 到 Service 的完整链路

### 1. network namespace — 容器拥有独立网络世界

场景提示: `ip addr` 在宿主机和容器里显示不同设备，容器里的 `127.0.0.1` 为什么只指向本容器网络栈？ [写作时展开]

关键设计: network namespace 隔离网络设备、路由表、邻居表、iptables/netfilter 状态和 socket 命名空间：

```[pseudocode]
clone/unshare(CLONE_NEWNET) 或 ip netns add
  → 创建新的 struct net
  → 新 namespace 初始只有 loopback
  → 独立配置 eth/veth、地址、路由、规则

ip netns exec <ns> ip addr/route
  → 在目标 namespace 中观察和操作网络栈
```

Why: 为什么容器网络首先需要 namespace，而不是只给进程分配一个不同 IP？——**IP 地址不是完整隔离边界**：没有独立 netns，进程仍可能看到同一套设备、路由、iptables 和 socket。namespace 先建立网络视图隔离，后面的 veth、bridge 和 CNI 才有连接对象。 [内核: `struct net` 保存每个 network namespace 的网络状态；loopback 也必须在目标 namespace 内启用]

比喻锚点: network namespace 像给每个租户一套独立小区道路和门禁；小区内部先自洽，之后再由网关决定如何接入城市道路。 [写作时展开]

### 2. veth pair + bridge — 把容器接入宿主机

场景提示: 容器 namespace 与宿主机 namespace 彼此隔离，数据包怎样跨过这道边界？ [写作时展开]

关键设计: veth 是一对互相连接的虚拟以太设备，通常一端放入 Pod netns，另一端接入宿主机 bridge：

```[pseudocode]
veth pair = (veth-host, eth0-pod)
  veth-host 放宿主机
  eth0-pod 放 Pod netns

Pod 发包:
  eth0-pod → veth-host → bridge/cni0/docker0
  → 同桥其他 veth 或宿主机路由

外部访问:
  可能继续经过宿主机路由、Netfilter/NAT、物理 NIC
```

Why: 为什么需要 bridge，veth 不能直接和所有 Pod 两两连接吗？——**bridge 提供二层交换和可扩展的接入点**：每增加一个 Pod，只需把宿主端 veth 接入 bridge，而不必建立全连接网线；跨节点则要加路由或 overlay。Docker bridge、Kubernetes CNI bridge 的具体规则和地址分配由实现配置决定，不能把所有容器网络都简化成同一种拓扑。 [内核: veth 把两个 namespace 连接成虚拟链路，Linux bridge 用 FDB 做二层转发]

比喻锚点: veth 是穿过小区围墙的双向网线，bridge 是小区交换机；每个容器插一根线，就能接入同一二层网络。 [写作时展开]

### 3. kube-proxy — Service 虚拟 IP 如何找到后端 Pod

场景提示: 客户端访问一个稳定的 ClusterIP，但后端 Pod 会扩缩容、重建、换 IP；谁负责把虚拟地址映射到真实后端？ [写作时展开]

关键设计: kube-proxy 监听 Service/EndpointSlice，并把服务数据面规则或内核负载均衡状态同步到节点：

```[pseudocode]
客户端 → Service ClusterIP:Port
  → 节点数据面规则
      iptables/nftables: DNAT 到后端 PodIP:targetPort
      或 IPVS/其他实现: 选择后端并转发
  → Pod

控制面变化:
  Service/EndpointSlice 更新
  → kube-proxy 同步节点规则/虚拟服务
```

Why: 为什么不能把 Service 当成一个真实监听 socket？——**ClusterIP 是虚拟服务抽象，后端集合会动态变化**：节点数据面需要把稳定入口和后端发现解耦。iptables 模式规则匹配和同步成本会随规则规模增长；IPVS 也不是所有新 Kubernetes 部署的默认或唯一推荐方案，内核能力、版本和 kube-proxy 模式都需核实。 [内核: Netfilter DNAT/conntrack 是 Service 数据面的基础机制之一]

比喻锚点: ClusterIP 是总机号码，kube-proxy 是分机调度台；来电拨总机不需要知道今天哪几个 Pod 正在值班。 [写作时展开]

### 4. Pod、Node 与外部访问 — 三段路径不能混为一谈

场景提示: 同一个 Pod 内的两个容器为什么能用 localhost 通信，而两个 Node 上的 Pod 却要经过 CNI/路由/隧道？ [写作时展开]

关键设计: Pod 网络模型、节点内通信、跨节点通信和外部暴露是不同路径：

```[pseudocode]
同一 Pod:
  多个容器共享一个 network namespace
  → localhost 直接通信

同一 Node 的不同 Pod:
  Pod eth0 → veth → bridge/路由
  → 目标 Pod veth

跨 Node:
  路由直连或 overlay/VXLAN
  → 物理网络传输
  → 对端解封/路由到目标 Pod

外部访问:
  NodePort/LoadBalancer/Ingress
  → 节点或边缘数据面
  → Service/Pod
```

Why: 为什么“Pod 能互通”不是天然成立的？——**CNI 必须为 Pod 分配地址、安装路由、配置 veth/bridge，跨节点还要决定采用路由、VXLAN、Geneve 或其他方案**；NetworkPolicy、云安全组和 NAT 还会在路径上增加过滤/改写。不同 CNI 的数据面不能用一张图完全概括。 [内核: 跨节点 IP forwarding、Netfilter 和虚拟设备共同参与；具体插件决定封装/路由细节]

比喻锚点: 同 Pod 是同一间办公室的内线电话，同 Node Pod 是同园区交换机，跨 Node Pod 是跨城专线，Ingress 则是面向公众的总服务台。 [写作时展开]

### 5. Service + Ingress — L4 虚拟服务与 L7 路由

场景提示: Service 能按端口转发，Ingress 却能按 Host/Path 分流；这两个对象分别处在哪一层？ [写作时展开]

关键设计: Service 主要提供 L4 级稳定入口和后端选择，Ingress 依赖 Controller 解析 HTTP/TLS 并执行 L7 路由：

```[pseudocode]
ClusterIP Service
  → 虚拟 IP/端口
  → kube-proxy/CNI 数据面选择 Endpoint
  → TCP/UDP 转发

Ingress
  → Host/Path/TLS 等规则
  → Ingress Controller(Nginx/Envoy 等)
  → 解析 HTTP
  → 转发到 Service/Endpoint

Headless Service
  clusterIP=None
  → 不提供虚拟 ClusterIP
  → DNS 返回 Pod/Endpoint 地址
```

Why: 为什么 Ingress 不能简单替代 Service？——**Ingress 需要理解 HTTP/TLS，Service 不依赖具体应用协议**：Service 适合稳定的 L4 发现与转发，Ingress 适合多个 HTTP 服务共享入口、按域名/路径路由。Ingress Controller 的配置更新、TLS 终止、超时和重试都可能改变端到端语义。 [内核: Service 的 L4 转发可落到 Netfilter/IPVS；Ingress Controller 是用户态 L7 代理]

比喻锚点: Service 是大楼前台的固定总机，Ingress 是能读懂部门、路径和业务类型的分诊台。 [写作时展开]

### 6. CNI 与 VXLAN — 谁负责把网络装配出来

场景提示: Pod 创建时，veth、IP、路由和跨节点隧道不是凭空出现的；kubelet/container runtime 通过什么接口完成配置？ [写作时展开]

关键设计: CNI 通过标准化插件调用把容器生命周期与网络配置连接起来；具体 IPAM、bridge、路由和 overlay 由插件组合完成：

```[pseudocode]
Pod sandbox 创建
  → runtime/kubelet 调用 CNI ADD
  → plugin 创建/移动 veth
  → IPAM 分配 Pod IP
  → 配置接口/路由/bridge/规则
  → 返回网络结果

Pod 删除
  → CNI DEL
  → 删除设备/路由/地址
  → 回收 IPAM 状态

VXLAN 示例:
  inner Ethernet frame
  → VXLAN header(VNI)
  → outer UDP/IP header
  → 物理网络
  → 对端解封 → inner frame 继续转发
```

Why: 为什么 CNI 是插件规范而不是一个固定的网络实现？——**不同环境需要不同数据面取舍**：简单 bridge 适合单节点，路由/BGP 适合可达性，VXLAN/Geneve 适合跨节点隔离，IPAM 还决定地址生命周期。CNI ADD/DEL 是装配接口，不保证所有插件都提供同样的策略、加密和故障恢复能力。 [内核: VXLAN 把内层以太帧封装在外层 UDP/IP；namespace/veth/bridge 是插件调用的内核对象]

比喻锚点: CNI 像集装箱入港时的装配工单：分配箱号（IPAM）、接电接水（接口/路由）、接入码头（bridge），跨港运输时再套上外层集装箱（VXLAN）。 [写作时展开]

### 7. 收束

容器网络是一条叠加链：

```[pseudocode]
进程
  → network namespace(隔离)
  → veth(跨 namespace 管道)
  → bridge/route(节点内转发)
  → Service/kube-proxy(Netfilter/IPVS 等数据面)
  → CNI 路由/overlay(跨节点)
  → Node/NIC/外部网络
```

**Aha Moment**: "容器网络不是‘每个 Pod 一张虚拟网卡’这么简单，而是**隔离、接线、寻址、转发、负载均衡和编排**多层叠加；Service 是稳定入口，Pod IP 是可变化后端，CNI 负责把这些内核对象真正装起来。"
**回答读者三问**: ①容器为什么隔离=network namespace；②Pod 怎么接到宿主机=veth + bridge/route；③Service 怎么找到 Pod=控制面同步后由节点数据面转发。

---

### 核心悬念

**"网络全栈从 TCP、epoll 到 Pod、Service 都打通了；性能观测如何回答‘CPU 被网络哪里吃掉了’——软中断、qdisc、conntrack、TLS、应用线程分别怎样测量？"**

→ 引出系统性能观测(内功修炼卷 5) — CPU/内存/网络性能观测与瓶颈定位。