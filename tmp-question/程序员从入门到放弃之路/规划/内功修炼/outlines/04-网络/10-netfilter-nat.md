# Netfilter、iptables 与 NAT — 一个包如何被过滤、跟踪和改写

> Cluster D: 5 KPs | 依赖: 01-tcpip-model-arp | 读者基线: TCP/IP、路由、连接跟踪基础
> 读者处境: 09 篇讲完百万连接与容器网络的容量问题；本篇回答数据包经过 Linux 主机时，谁决定放行、丢弃、改目的地址，谁又记住这次改写让回包能正确返回
> 打开新视角: Netfilter 不是一张“iptables 规则表”，而是**协议栈钩子 + conntrack 状态 + NAT 映射 + 规则 target**组成的包处理框架

---

### 概念依赖链

```
01 分层/ARP + 09 连接容量 → 本篇: Netfilter/iptables/NAT
  ├─ §1 五个 hook(包在协议栈的决策位置)
  ├─ §2 表/链/优先级(规则如何挂到 hook)
  ├─ §3 match/target/conntrack(规则如何记状态)
  ├─ §4 SNAT/DNAT/NAPT(地址与端口改写)
  └─ §5 STUN/TURN/ICE(穿过 NAT 建立端到端路径)
先讲: hook → 规则框架 → 状态跟踪 → NAT 改写 → 穿透
后续依赖: 11-ssl-tls(TLS 在 NAT 之后保护应用内容)
```

### 叙事顺序

1. 问题引入——访问 Docker 端口或内网服务时，包为什么会被放行、转发、改写，还能让回包回到正确客户端？（**Aha: NAT 不是“改一下 IP”就结束，而是 conntrack 记录一条双向映射并让后续包沿同一状态处理**）
2. 五个 Netfilter hook——包在协议栈哪里被拦截
3. iptables 表与规则——match/target 如何进入内核
4. conntrack + SNAT/DNAT/NAPT——改写和回包为什么能对上
5. STUN/TURN/ICE——NAT 穿透为什么有时必须中继
6. 收束——从 hook 到端到端路径

### 1. Netfilter 五个 hook — 包在协议栈的五个决策点

场景提示: 入站包、转发包和本机发出的包，为什么不能用同一条过滤位置处理？ [写作时展开]

关键设计: IPv4 Netfilter 把包挂在路由前、本机入站、转发、本机出站和路由后五类 hook：

```[pseudocode]
入站:
  NF_INET_PRE_ROUTING
  → 路由判断
      本机 → NF_INET_LOCAL_IN → socket
      转发 → NF_INET_FORWARD → POST_ROUTING

本机发出:
  NF_INET_LOCAL_OUT
  → 路由/输出
  → NF_INET_POST_ROUTING
```

Why: 为什么 DNAT 常在 PREROUTING，而 SNAT 常在 POSTROUTING？——**DNAT 要在路由决定前改变目的地址，让路由把包送到新的内网目标；SNAT 通常要在路由选定出口后改变源地址，确保出口看到最终外部源地址**。这不是任意约定，而是地址改写与路由时序共同决定的。 [内核: `nf_hook_slow` 按 hook 优先级运行注册的处理函数]

比喻锚点: 五个 hook 像机场的五个安检/分流闸口：入境先检查，决定留在本楼还是转机，本机出发和离开机场也有各自检查点。 [写作时展开]

### 2. iptables 表、链与规则 — 用户态配置如何进入内核

场景提示: `iptables -A INPUT ...` 这条命令是直接修改内核网络代码，还是提交了一组匹配规则？ [写作时展开]

关键设计: iptables 用户态工具把 match/target 规则加载进 Netfilter 相关表；表和 hook 的实际优先级由实现与内核注册决定，不能简单把所有包都概括为固定“raw→mangle→nat→filter”单线顺序：

```[pseudocode]
rule = match conditions + target action

matches:
  source/destination / protocol / port / interface / conntrack state

targets:
  ACCEPT / DROP / REJECT / JUMP
  DNAT / SNAT / MARK / NOTRACK

典型职责:
  raw: 连接跟踪前的特殊处理/NOTRACK
  mangle: 标记或修改部分包属性
  nat: 新连接的地址/端口转换决策
  filter: INPUT/FORWARD/OUTPUT 过滤
```

Why: 为什么表要分开，而不是所有规则混成一张表？——**不同 target 的时机、状态依赖和副作用不同**：raw 可能决定是否进入 conntrack，mangle 可能影响策略路由，nat 需要连接状态，filter 才是主要放行/丢弃决策。表名不是“安全等级”，真正顺序要结合 hook、优先级和规则 target 判断。 [内核: `nf_register_net_hook` 把各模块注册到指定 hook 与优先级]

比喻锚点: 表像机场不同部门：证件预处理、行李标记、转机登记、最终放行各有职责；把所有部门混成一个柜台，顺序和副作用都会失控。 [写作时展开]

### 3. conntrack — NAT 与“已建立连接放行”背后的状态账本

场景提示: 为什么只在第一包决定 NAT，后续包还能自动沿同一映射返回？ [写作时展开]

关键设计: conntrack 为流维护双向连接状态，NAT 在首个相关包上建立转换信息，后续包依据连接状态执行映射：

```[pseudocode]
第一个包:
  nf_conntrack_in
  → 创建/查找 conn
  → NAT target 选择映射
  → 记录原始方向与应答方向的 tuple

后续包:
  → 根据 tuple 命中 conntrack
  → 复用已建立的 NAT 映射
  → 规则可按 ESTABLISHED/RELATED 放行

iptables:
  -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
```

Why: 为什么 NAT 不能只改当前包的 IP/端口？——**回包必须知道如何反向改写**：内网 `10.0.0.2:40000` 出去映射成公网地址/端口，服务端回包时 conntrack 才能把它翻译回原端点。conntrack 表满时新流可能无法建立，这属于状态资源耗尽，不是单条 filter 规则写错。 [内核: `nf_conntrack_in` 建立/查找连接状态，NAT 映射依赖双向 tuple]

比喻锚点: conntrack 像海关给一对往返行李贴同一个追踪编号——出境时改了标签，回境时靠编号知道应恢复成哪个原始地址。 [写作时展开]

### 4. SNAT、DNAT、NAPT — 地址改写发生在哪里

场景提示: 内网机器访问互联网和外部客户端访问内网服务，为什么分别需要 SNAT 与 DNAT？ [写作时展开]

关键设计: NAT 可以改源地址、目的地址，也可以同时改端口：

```[pseudocode]
SNAT:
  内网源地址 → 公网源地址/端口
  常见位置: POST_ROUTING
  用途: 多个内网客户端共享出口地址

DNAT:
  公网目的地址/端口 → 内部服务地址/端口
  常见位置: PREROUTING
  用途: 端口映射/反向代理前的转发

NAPT/PAT:
  地址 + 端口同时映射
  → 用不同公网端口区分多个内网流

转发路径:
  PREROUTING/DNAT → 路由 → FORWARD → POSTROUTING/SNAT
```

Why: 为什么 NAT 会让端到端连接变复杂？——**它改变了原始 tuple，并让一台设备承担状态中介角色**：入站连接需要端口映射、防火墙放行和回程路由同时正确；对称 NAT 还可能为不同目的地分配不同外部映射，使两个端点难以直接互相打洞。 [内核: `nf_nat_setup_info` 保存转换决策，协议专用 NAT 代码改写 TCP/UDP tuple]

比喻锚点: SNAT 像公司所有员工共用一个总机号码向外打电话，NAPT 用分机号区分每个人；DNAT 则像总机把外部来电转到内部具体分机。 [写作时展开]

### 5. STUN、TURN、ICE — NAT 穿透失败时谁来兜底

场景提示: 两台处于不同家庭路由器后的设备想建立 WebRTC 连接，为什么不能只互相发送本地 IP？ [写作时展开]

关键设计: ICE 收集候选路径，STUN 帮端点发现映射，TURN 在直连失败时提供中继：

```[pseudocode]
ICE candidates:
  host candidate: 本地接口地址
  server-reflexive candidate: STUN 观察到的公网映射
  relay candidate: TURN 分配的中继地址

连通性检查:
  对候选 pair 发送检查
  → 可直连? 选择直连路径
  → NAT/防火墙阻断? 回退 TURN 中继
```

Why: 为什么 STUN 不是万能穿透方案？——**STUN 只能告诉端点“从服务器看来你映射成什么地址”，不能强行让任意 NAT 接受来自陌生端点的入站包**：对称 NAT、端口限制、防火墙策略都可能导致打洞失败；TURN 用服务器中继提高成功率，代价是服务器带宽和额外 RTT。 [man 7 udp: UDP 无连接语义是打洞常见基础；穿透行为还取决于具体 NAT 实现]

比喻锚点: STUN 像让朋友告诉你“外界看到的公司前台号码”，TURN 像让双方都把货送到第三方仓库转交，成功率更高但仓库要付成本。 [写作时展开]

### 6. 收束

一个典型内网访问外部服务的路径：

```[pseudocode]
内网应用发包
  → LOCAL_OUT
  → 路由
  → POST_ROUTING/SNAT
  → 外部网络

外部访问端口映射
  → PRE_ROUTING/DNAT
  → 路由
  → FORWARD/filter
  → 内网服务

conntrack:
  为两个方向维护 tuple/NAT 状态
  → 回包沿反向映射返回
```

**Aha Moment**: "Netfilter 的核心不是四张表的记忆题，而是**在正确 hook 上做决策，并用 conntrack 把一条双向流的原始地址、转换地址和状态绑在一起**。NAT 真正改变的是连接寻址关系，不只是某个包头字段。"
**回答读者三问**: ①DNAT/SNAT 差在哪=分别改目的/源地址；②为什么只处理首包=conntrack 保存后续映射；③STUN 失败怎么办=ICE 回退 TURN 中继。

---

### 核心悬念

**"NAT 只改 IP/端口，TLS 却要验证主机身份和保护应用内容；HTTPS 的 TLS 握手发生在 NAT 前还是后，证书又如何证明你连的是谁？"**

→ 引出 11-ssl-tls — TLS 握手、证书链与加密——从网络地址转换进入端到端身份与机密性。