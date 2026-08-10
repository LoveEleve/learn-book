# 容器网络: Namespace + veth + bridge + kube-proxy + Pod/Service/Ingress + CNI

> Cluster F: 4 KPs | 依赖: 08-epoll, 10-netfilter-nat | 读者基线: Linux网络栈 + Netfilter/iptables

---

### 1. 网络Namespace — 容器网络隔离的基石
  - 创建: `clone(CLONE_NEWNET)` 或 `ip netns add` → 新Namespace有独立的路由表/iptables/网络设备/套接字 (net/core/net_namespace.c → setup_net)
  - 默认设备: 新ns只有lo(127.0.0.1 loopback) — 完全隔离, 无法与外界通信 (net/core/net_namespace.c → pernet_list)
  - 实现: 每个netns有独立的net结构, 包含所有网络数据结构(proc/sysfs的net目录) (include/net/net_namespace.h → struct net)
  - 查看: `ip netns list`, `ip netns exec <ns> ip addr` — 进入namespace执行命令 (iproute2工具)
  - Docker/K8s依赖: 每个容器的网络栈=独立的netns — 隔离是容器安全的物理基础

### 2. veth pair + bridge — 容器与宿主机的通信管道
  - veth pair: 虚拟以太网设备对 — 一端在容器ns, 一端在宿主机ns, 像管道两端 (drivers/net/veth.c → veth_newlink)
  - 工作原理: 发到veth一端的包→另一端收到→就像一根网线连接两个ns (drivers/net/veth.c → veth_xmit)
  - Bridge: 宿主机虚拟交换机 — 多个veth宿主机端接入同一bridge, 实现多容器二层互通 (net/bridge/br_if.c → br_add_if)
  - Docker bridge四场景: (1)同主机容器通信(veth+bridge二层), (2)容器访问外网(SNAT POST_ROUTING), (3)外部访问容器(DNAT PREROUTING端口映射), (4)跨主机通信(overlay/VXLAN)
  - 跨主机: macvlan(虚拟MAC, 物理网卡直接出)/ipvlan(共享MAC, IP路由)/flannel(VXLAN封装) (drivers/net/vxlan.c)

### 3. kube-proxy — iptables vs IPVS 模式
  - iptables模式: kube-proxy为每个Service生成DNAT规则 — ClusterIP→PodIP随机选 (pkg/proxy/iptables/proxier.go → syncProxyRules)
  - 问题: N个Service×M个Endpoint→O(N×M)条iptables规则, 大量Service时规则过多导致匹配延迟
  - IPVS模式: kube-proxy用IP Virtual Server — 内核L4负载均衡, O(1)查找 (net/netfilter/ipvs/ip_vs_core.c → ip_vs_in)
  - 三种调度: rr(轮询), lc(最少连接), sh(源哈希session affinity) — IPVS支持更丰富的负载均衡策略
  - IPVS优势: 规则不随Pod/Service增长, 内核数据结构(哈希表)更高效 — 生产推荐IPVS

### 4. Pod网络模型 — 同Node/跨Node/外部访问
  - 同Node Pod通信: 通过Node level bridge(cni0/docker0)二层转发 — 同一bridge下的veth设备直接通信 (net/bridge/)
  - 跨Node Pod通信: CNI插件负责 — flannel(VXLAN封装)→物理网络→对端解封, calico(BGP路由直连), weave(自研路由) (net/ipv4/ip_forward.c → ip_forward)
  - 外部访问Pod: NodePort(iptables DNAT NodeIP:NodePort→PodIP:targetPort) + LoadBalancer(外部LB→NodeIP:NodePort) (pkg/proxy/iptables/proxier.go)
  - Pod间共享网络: 同Pod内容器共享同一个network namespace(localhost通信) + 独立mount/pid namespace

### 5. Service + Ingress — L4与L7负载均衡
  - ClusterIP(虚拟IP): Service创建→kube-proxy/ipvs在Node上建立DNAT规则, 任何到ClusterIP的包转发到Endpoints (pkg/registry/core/service/ → ClusterIP)
  - 三种Service类型: ClusterIP(内部), NodePort(ClusterIP+NodePort, 每Node绑定端口), LoadBalancer(ClusterIP+NodePort+外部LB)
  - Ingress: L7路由规则(基于Host/Path)→Ingress Controller(Nginx/Envoy)→转发到Service ClusterIP (nginx ingress controller → 反向代理)
  - Ingress Controller: 监听Ingress资源变化→动态生成nginx配置→reload nginx — L7反向代理路径 (Nginx/Envoy/HAProxy)
  - Service无头(Headless): spec.clusterIP=None — 无ClusterIP仅DNS SRV记录, 用于StatefulSet直连

### 6. CNI规范 + flannel — 网络插件标准
  - CNI规范: JSON配置文件+可执行文件 — ADD(创建网络分配IP), DEL(删除网络回收IP), CHECK(验证) (containernetworking/cni → spec)
  - flannel原理: 每Node一个flannel.1(VXLAN设备)+etc存储子网分配→Pod IP→flannel.1封装→物理网络→对端解封 (flannel → backend/vxlan)
  - VXLAN: 二层over三层 — MAC帧封装在UDP包中(外层IP+UDP头+VXLAN头+内层以太帧), VXLAN VNI实现网络隔离 (drivers/net/vxlan.c → vxlan_xmit)
  - CNI链: containerd/kubelet→调用CNI插件(bridge创建+IPAM分配IP+端口映射)→配置Pod netns (参考containerd CRI)

### 7. 收束
  - 容器网络=网络Namespace(隔离)+veth(管道)+bridge(交换)+iptables/IPVS(NAT)+CNI(插件)五层技术栈的叠加
  - kube-proxy IPVS模式是生产标配: 内核O(1)负载均衡, 规则数不随Service膨胀
  - Ingress=HTTP层面的反向代理(类似Nginx), Service=TCP层面的DNAT(类似iptables NAT), CNI=网络层面的隧道(类似VXLAN)

---

### 核心悬念
**"网络全栈打通了: 从TCP三次握手→内核收发包→epoll→容器网络。那系统性能观测怎么做? CPU怎么被网络"偷"走的?"**

→ 引出 系统性能观测(内功修炼卷5 — OS性能)
