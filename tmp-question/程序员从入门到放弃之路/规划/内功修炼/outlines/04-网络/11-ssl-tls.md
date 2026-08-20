# TLS 握手、证书链与 SSH — 加密连接如何确认“你连的是谁”

> Cluster D: 5 KPs | 依赖: 01-tcpip-model-arp | 读者基线: TCP/IP、对称/非对称加密基础
> 读者处境: 10 篇讲了 NAT 改写 IP/端口；本篇回答 HTTPS 如何在网络地址变化之后，仍验证服务身份、协商会话密钥并保护应用数据
> 打开新视角: TLS 把三个问题分开解决——**密钥协商、身份认证、对称加密传输**；证书解决“公钥属于谁”，握手解决“这次连接用什么密钥”

---

### 概念依赖链

```
01 TCP/IP + 10 NAT → 本篇: TLS/证书/SNI/SSH
  ├─ §1 TLS 1.2(传统握手与密钥派生)
  ├─ §2 TLS 1.3(1-RTT/PFS/简化握手)
  ├─ §3 0-RTT(PSK恢复与重放边界)
  ├─ §4 X.509证书链(身份信任)
  └─ §5 SNI/ECH/SSH(虚拟主机与独立安全协议)
先讲: TLS 1.2 → TLS 1.3 → 0-RTT → 证书 → SNI/SSH
后续依赖: 12-http-evolution(HTTP/1.1、HTTP/2、HTTP/3如何摊薄握手成本)
```

### 叙事顺序

1. 问题引入——NAT 只改地址，TLS 如何确认“这个 IP 后面的服务真是 `example.com`”？（**Aha: TLS 不是单一加密算法，而是密钥协商、证书认证和对称传输的组合协议**）
2. TLS 1.2——从 ClientHello 到 Finished
3. TLS 1.3——KeyShare 前置、1-RTT 与前向保密
4. 0-RTT——更低延迟但允许重放的边界
5. 证书链——根 CA 如何成为信任锚
6. SNI/ECH 与 SSH——虚拟主机和另一套远程安全协议
7. 收束——握手、身份、加密与延迟

### 1. TLS 1.2 — 传统握手如何建立会话密钥

场景提示: TCP 已建立，但客户端还不能直接把 HTTP 明文发出去；TLS 1.2 需要交换什么信息？ [写作时展开]

关键设计: TLS 1.2 通过 ClientHello/ServerHello 协商参数，再完成密钥交换、证书验证和 Finished 校验（RFC 5246）：

```[pseudocode]
ClientHello
  → 版本/密码套件/ClientRandom/SNI

ServerHello + Certificate + ServerKeyExchange + ServerHelloDone
  → 选参数/发送证书链/提供密钥交换参数/ServerRandom

ClientKeyExchange + ChangeCipherSpec + Finished
  → 完成密钥交换
  → 切换到协商出的对称保护
  → Finished 验证握手 transcript

Server ChangeCipherSpec + Finished
  → 双方开始用会话密钥保护应用数据
```

Why: 为什么握手协商完之后还要用对称加密传输？——**非对称密码适合身份认证和密钥协商，但不适合持续加密大量应用数据**；握手完成后双方使用派生出的对称密钥，获得更低的 CPU 和带宽开销。TLS 1.2 的具体密钥交换可能是 RSA、DHE/ECDHE 等，不能把所有 TLS 1.2 都简化为“公钥加密 PreMasterSecret”。 [man 7 tls: TLS 库接口与协议版本/密码套件配置需要区分]

比喻锚点: TLS 握手像双方先在公开场合确认暗号和身份，谈妥一把临时房间钥匙；真正搬运大量货物时改用这把高效的对称钥匙。 [写作时展开]

### 2. TLS 1.3 — KeyShare 前置，把握手压到 1-RTT

场景提示: TLS 1.2 建连要多次往返，TLS 1.3 怎样让客户端更早开始密钥协商？ [写作时展开]

关键设计: TLS 1.3 把密钥交换 KeyShare 放进 ClientHello，服务端返回选择结果和加密后的后续握手消息（RFC 8446）：

```[pseudocode]
ClientHello + KeyShare
  → 客户端提前发送临时 DH/ECDH 公钥

ServerHello + KeyShare
  → 双方得到共享密钥
  → 后续 EncryptedExtensions/Certificate/Finished 受保护

Client Finished
  → 握手完成, 开始应用数据

TLS 1.3 的核心变化:
  默认使用临时密钥交换 → 前向保密
  删除静态 RSA 密钥交换/过时密码套件
  ChangeCipherSpec 不再承担 TLS 1.2 的核心切换语义
```

Why: 为什么 TLS 1.3 不是简单“删掉几个消息”就变快？——**它同时提前 KeyShare、减少往返、收紧密码套件并让握手后半段更早加密**；通常 TCP 1-RTT 加 TLS 1.3 1-RTT 才能发送首个应用数据，实际还受连接复用、TCP Fast Open、会话恢复和网络条件影响。 [内核: TLS 加密可在用户库或内核 kTLS 等不同层实现，握手协议与数据面实现不是一回事]

比喻锚点: TLS 1.2 像见面后才开始交换钥匙，TLS 1.3 像预约时就先带好可交换的临时钥匙，到现场只需确认并开始谈话。 [写作时展开]

### 3. TLS 1.3 0-RTT — 用更低延迟换取重放边界

场景提示: 客户端以前连过服务器，第二次连接能不能在 ClientHello 同时发送早期 HTTP 请求？ [写作时展开]

关键设计: 会话恢复通过 PSK 和 NewSessionTicket 提供 0-RTT early data，但 early data 的重放保护弱于握手完成后的应用数据：

```[pseudocode]
首次连接完成
  → Server NewSessionTicket
  → 客户端保存 PSK/恢复参数

再次连接
  → ClientHello + PSK + early data
  → 服务端验证恢复信息并决定是否接受 early data

风险边界:
  early data 可能被录制/重放
  → 不适合不可幂等的扣款/写操作
  → 服务端应按请求语义、nonce/票据和策略限制
```

Why: 为什么 0-RTT 不能被宣传成“免费降低一轮 RTT”？——**它牺牲了一部分重放安全语义，并且服务端可以拒绝或限制 early data**；适合幂等读取、静态资源和可安全重试的请求，不适合未经保护的状态变更。 [man 7 tls: 会话恢复/early data 的安全策略由 TLS 库与服务端配置共同决定]

比喻锚点: 0-RTT 像老客户凭上次发的取货券提前下单；速度很快，但同一张券/订单可能被别人复制提交，所以只能允许可重复的取货请求。 [写作时展开]

### 4. X.509 证书链 — 公钥为什么值得相信

场景提示: 客户端收到了服务端公钥，为什么不能直接相信？谁证明它属于目标域名？ [写作时展开]

关键设计: 证书把主体、域名、公钥、签发者、有效期和 CA 签名绑定在一起，客户端从叶证书向信任锚验证：

```[pseudocode]
服务端发送:
  leaf certificate + intermediate CA certificates

客户端验证:
  1) 域名/SAN 是否匹配目标主机
  2) 当前时间是否在有效期内
  3) 叶证书签名是否由中间 CA 验证
  4) 中间 CA 是否能链到系统/浏览器信任的 root CA
  5) key usage / basic constraints / 算法策略是否满足

root CA:
  通常作为本地信任库中的信任锚
  不需要再由网络上另一张证书证明
```

Why: 为什么“证书签名正确”仍不代表连接安全？——**签名只是链的一部分**：还要检查 SAN、有效期、用途、路径约束、算法策略和信任库；如果根 CA 被错误信任或私钥泄露，整条信任链都可能受影响。OCSP/CRL、OCSP stapling 与 CT 日志分别解决吊销查询、查询隐私/延迟和错误签发可发现性问题，不能互相替代。 [man 1 openssl: `s_client`/`verify` 可用于观察握手和验证链]

比喻锚点: 证书链像层层盖章的身份证明：商户证件由地方机构签发，地方机构由国家信任机构背书；但客户端仍要核对姓名、有效期和用途。 [写作时展开]

### 5. SNI、ECH 与 SSH — 虚拟主机和另一套安全协议

场景提示: 一个公网 IP 承载几百个 HTTPS 域名，服务端在看到加密 HTTP 之前怎样选择正确证书？ [写作时展开]

关键设计: SNI 把目标主机名放入 ClientHello，让服务端在握手早期选择虚拟主机证书；ECH 进一步尝试隐藏 ClientHello 中的敏感名称信息：

```[pseudocode]
ClientHello
  → SNI = example.com
  → 服务端按名称选择证书/配置
  → 继续 TLS 握手

ECH:
  通过外层 ClientHello + 加密 inner ClientHello
  → 尝试隐藏真实 SNI
  → 依赖客户端/服务端/部署环境支持

SSH:
  TCP
  → 版本协商
  → 密钥交换
  → 服务端主机密钥认证
  → 用户密码/公钥认证
  → 加密的会话/转发通道
```

Why: 为什么 SSH 不是“TLS 加一个登录页面”？——**两者都建立加密通道，但协议对象不同**：TLS 主要为应用协议提供认证和保密传输，SSH 自带主机认证、用户认证、会话、端口转发和通道复用。SNI 也不等于证书验证本身，它只是帮助服务端选择虚拟主机配置，客户端仍要验证证书中的名称。 [man 5 ssh_config / man 5 sshd_config: SSH 主机认证与用户认证由独立配置控制]

比喻锚点: TLS 像安全运输管道，SNI 是入口处的目的地标签；SSH 则像带门卫、用户登录、多个内部房间和隧道功能的一整栋安全办公楼。 [写作时展开]

### 6. 收束

回到 HTTPS 第一个字节如何安全到达：

```[pseudocode]
TCP handshake
  → TLS ClientHello/ServerHello
  → KeyShare/证书链/Finished
  → 双方派生对称会话密钥
  → HTTP 请求与响应进入加密记录

NAT:
  改写 IP/端口, 通常看不到 TLS 应用明文
TLS:
  在端点之间验证身份并保护应用内容
```

**Aha Moment**: "TLS 安全不是单一的‘加密开关’，而是三层协议承诺：**证书链回答‘我连的是谁’，密钥交换回答‘这次会话用什么秘密’，对称加密回答‘后续字节如何保密和验真’**。0-RTT、SNI、NAT 都是在这个主结构上增加的性能或部署边界。"
**回答读者三问**: ①TLS 1.3 为什么更快=KeyShare 前置并减少握手往返；②证书链验证什么=名称、时间、用途和信任路径；③0-RTT 为什么危险=early data 存在重放边界。

---

### 核心悬念

**"TLS 握手和 TCP 建连都要付往返延迟，HTTP/2 如何通过多路复用摊薄连接成本，HTTP/3/QUIC 又为何把传输层和 TLS 重新绑在一起？"**

→ 引出 12-http-evolution — HTTP/1.1、HTTP/2、HTTP/3/QUIC 演进——从安全握手进入应用协议与传输协议协同优化。