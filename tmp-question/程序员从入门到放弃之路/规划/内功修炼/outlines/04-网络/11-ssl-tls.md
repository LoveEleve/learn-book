# SSL/TLS握手 + 证书链 + SNI + SSH

> Cluster D: 5 KPs | 依赖: 01-协议基础 | 读者基线: TCP/IP + 对称/非对称加密基础

---

### 1. TLS 1.2 握手 — 2-RTT的完整流程
  - ClientHello: 支持的TLS版本/密码套件/随机数ClientRandom/SNI (RFC 5246 §7.4.1.2)
  - ServerHello+Certificate+ServerKeyExchange+ServerHelloDone: 选密码套件/发证书链/KeyExchange参数/ServerRandom (RFC 5246 §7.4.1.2)
  - ClientKeyExchange+ChangeCipherSpec+Finished: 客户端生成PreMasterSecret→用服务端公钥加密/切换到对称加密/发Finished验证 (RFC 5246 §7.4.7)
  - ChangeCipherSpec+Finished: 服务端也切换对称加密/发Finished验证 — 此后所有通信使用对称密钥加密 (RFC 5246 §7.4.9)
  - 密钥推导: MasterSecret=PRF(PreMasterSecret+ClientRandom+ServerRandom)→symmetric keys/IV/MAC keys (RFC 5246 §8.1)
  - 2-RTT: ClientHello→ServerHello→ClientFinish→ServerFinish — 建连+加密=TCP握手(1RTT)+TLS握手(2RTT)=3RTT

### 2. TLS 1.3 — 1-RTT握手的简化
  - 减去的内容: 去掉RSA密钥交换(无前向保密), 去掉静态DH, 去掉过时对称算法(RC4/3DES), 去掉ChangeCipherSpec (RFC 8446 §1.2)
  - ClientHello: 带上KeyShare(客户端DH公钥) — 提前开始密钥交换 (RFC 8446 §4.2.8)
  - ServerHello+EncryptedExtensions+Certificate+Finished: 服务端选DH参数→发证书→发Finished — 仅1-RTT (RFC 8446 §4.4)
  - 1-RTT: 总连接时间=TCP握手(1RTT)+TLS握手(1RTT)=2RTT — 比TLS 1.2少1RTT
  - 前向保密(PFS): TLS 1.3强制DHE/ECDHE — 即使服务器私钥泄露, 历史会话也无法解密

### 3. 0-RTT — TLS 1.3 PSK恢复
  - PSK(Pre-Shared Key): 首次连接后服务端发NewSessionTicket→客户端保存→再次连接时ClientHello带PSK (RFC 8446 §4.2.11)
  - 0-RTT数据: 客户端在ClientHello后立即发应用数据(用PSK派生的密钥加密) — 首次包就带数据 (RFC 8446 §4.2.10)
  - 重放攻击风险: 0-RTT数据可被攻击者录制重放 → 服务端需做幂等保护(不应在0-RTT阶段做写操作) (RFC 8446 §8)
  - 适用: 只读API/静态资源/CDN预取 — 写操作应延迟到握手完成后

### 4. 证书链 — 信任体系的核心
  - 证书内容: Subject(域名)/Public Key/Issuer(签发者)/有效期/签名(CA用其私钥签名) (X.509 v3, RFC 5280)
  - 证书链验证: 叶证书→中间CA→根CA(预装于操作系统/浏览器信任库) (RFC 5280 §6)
  - 根CA信任锚: 系统预装(如/etc/ssl/certs/), 自签名但被OS/浏览器信任 — 整个Web PKI的信任基础
  - 吊销(OCCSP/CRL): 证书过期/泄露前吊销, OCSP Stapling(服务端定期获取OCSP响应并附在握手中)减少客户端查询延迟 (RFC 6066 §8)
  - 证书透明(CT): 所有证书公开记录到CT日志 — 防CA错误签发(如赛门铁克事件) (RFC 6962)

### 5. SNI + SSH — TLS扩展与远程登录协议
  - SNI(Server Name Indication): ClientHello中携带域名→服务端根据域名选对应证书(虚拟主机场景) — 没有SNI则一个IP只能一个证书 (RFC 6066 §3)
  - SNI隐私: TLS 1.3支持Encrypted SNI(ESNI/ECH), 加密SNI防中间人获知访问域名 (RFC 8744)
  - SSH握手: TCP连接→协议版本协商→密钥交换(DH/ECDH)→服务端认证(主机密钥)→客户端认证(密码/公钥)→加密通道 (RFC 4253)
  - SSH vs TLS: SSH在TCP之上自建加密通道(非TLS), 设计目标为远程登录/隧道, TLS目标为Web安全

### 6. 收束
  - TLS 1.3从2-RTT→1-RTT是协议简化(去掉ChangeCipherSpec+RSA)+提前KeyShare的组合结果
  - 证书链验证是Web安全的根基: 根CA被攻破→整个PKI崩塌, CT日志是防滥用的新防线
  - SNI解决了HTTPS多站点同IP场景, SSH是不同于TLS的独立安全协议体系

---

### 核心悬念
**"TLS握手3RTT+TCP握手1RTT=4RTT返回第一个字节, HTTP协议是怎么优化这个延迟的?"**

→ 引出 HTTP/1.1→HTTP/2→HTTP/3 QUIC 演进(12-http-evolution)
