# 用户发来POST请求, 你的服务器怎么收到这4KB数据的? — 网络/NIO/通信协议全景

> Cluster A: 15 KPs | 依赖: 无 | 读者基线: 写过Spring Boot CRUD, 了解HTTP基本概念

---

### 1. 客户端发了请求, 服务器究竟如何"看到"这些字节?
  用户浏览器点下"下单"按钮, POST一个JSON到你的Java服务 — 中间经过了多少层? 每层做了什么?
  - B1 Ch1 §1: TCP/IP五层模型 — 物理层(比特流)→链路层(MAC帧)→网络层(IP路由)→传输层(TCP分段)→应用层(HTTP/JSON) (B2 Ch5 §6)
  - TCP三次握手: 客户端SYN→服务器SYN-ACK→客户端ACK, 2-RTT建立连接 — 为什么是三次不是两次? 防止历史SYN被误建连接 (B2 Ch5 §6.3) [工程: 三次握手是网络2将军问题在TCP层的工程化求解 — 不可靠信道上确认收到确认]
  - TLS握手: TCP之上再四次握手(RSA)/两次(Ephemeral DH), 协商对称密钥 — HTTPS=HTTP over TLS, 端到端加密 (B2 Ch5 §4.8)
  - 关键设计: 为什么TCP层和TLS层都需要握手? — TCP保证可靠传输, TLS保证传输内容安全, 两层正交不可合并

### 2. 字节到了进程门口 — 线程阻塞等数据还是异步回调?
  传统Servlet一个请求一个线程, 1000并发开1000线程 — 线程切换和栈内存足够要命
  - B1 Ch1 §2.2: 阻塞I/O vs 非阻塞I/O — 阻塞模式下read()调用会挂起线程直到数据到达; NIO通过Selector实现单线程管理多连接
  - B2 Ch4 §3.3: select/poll/epoll — select(遍历FD_SET O(n))/poll(动态数组 O(n))/epoll(红黑树+事件驱动 O(1)) (B2 Ch4 §3.3)
  - epoll三件套: epoll_create(创建红黑树), epoll_ctl(注册FD), epoll_wait(阻塞等就绪事件) — LT(电平触发, 未读完下次再通知) vs ET(边沿触发, 必须读干净否则丢事件) (B2 Ch4 §3.3)
  - 关键设计: epoll用红黑树+就绪链表 — 注册时O(log n), wait时O(1)取就绪事件, 比select O(n)遍历所有FD高效

### 3. Reactor模式 — 怎么让单线程处理1万连接?
  你看到Netty的EventLoop是一个线程配一个Selector — 这背后是Reactor模式
  - B1 Ch1 §2.3: Reactor三变种 — 单Reactor单线程(Redis 6.0前), 单Reactor多线程(Reactor分发→Worker池处理), 主从Reactor(主接收连接+从处理读写, Netty Boss+Worker)
  - B2 Ch4 §3.4: 1+N+M模型 — 1 Acceptor线程+N个I/O线程(epoll)+M个Worker线程(业务处理), Netty的BossGroup(1)+WorkerGroup(N)+业务线程池(M)
  - B1 Ch1 §2.1: ByteBuffer — position(当前读/写位置)/limit(上界)/capacity(容量)/flip(读转写)/compact(压缩), 关键坑: 读完要flip否则position不对
  - 关键设计: 主从Reactor — Boss接收连接→Worker注册读写, 连接和I/O职责分离避免接收连接时I/O被阻塞

### 4. 对象序列化 — Java对象怎么变成网络字节流?
  RPC调用时User{name:"张三",age:25}怎么通过网络传输到另一个服务?
  - B1 Ch1 §4: JDK Serializable(Java Native, 版本兼容差+码流大) vs Protobuf(Google, 二进制+IDL+跨语言) vs Hessian(轻量二进制, Dubbo默认) vs JSON(可读性好但体积大)
  - Protobuf编码: varint(变长整数, 小数字1字节大数字10字节)+length-delimited+zigzag编码 — String长度前缀避免粘包 (B4 Ch2 §7.3)
  - gRPC: HTTP/2+Protobuf — 多路复用+双向流+头部压缩HPACK, 比HTTP/1.1 JSON REST快3-10x (B4 Ch2 §7.8)
  - 关键设计: 为什么Dubbo默认Hessian而gRPC用Protobuf? — Hessian兼容Java类型体系, Protobuf强调Schema+多语言, 场景不同

### 5. HTTP演进 — 为什么HTTP/2比HTTP/1.1快?
  单体应用切微服务后, 每个HTTP请求的额外开销被放大N倍
  - B2 Ch5 §1-2: HTTP/1.0短连接→HTTP/1.1 Keep-Alive连接复用+Chunked传输 — Head-of-Line Blocking: 一个响应慢阻塞后续
  - B2 Ch5 §3: HTTP/2二进制分帧 — 一条TCP连接上多Stream独立传输, 消除队头阻塞(应用层) + HPACK头部压缩
  - QUIC(HTTP/3): UDP+0-RTT+TLS 1.3内置, 连接迁移(IP切换不断连) — 解决TCP层面的队头阻塞 (B2 Ch5 §7)
  - 关键设计: HTTP/2的Stream优先级+流量控制 — 浏览器优先渲染CSS而非图片; 队头阻塞从HTTP层下移到TCP层(丢包时仍影响所有Stream)

### 6. 认证与状态 — Cookie/Session/Token/JWT怎么选?
  分布式微服务下, 用户登录状态如何在10个服务间共享?
  - B1 Ch1 §5.2-5.4: Cookie(客户端存储凭证，如登录token)， Session(服务端存储状态，通过Cookie传递SessionId)， 分布式Session三方案: 粘滞Session(负载均衡固定转发)→复制Session(Tomcat DeltaManager)→集中存储(Redis统一存)
  - JWT: Header.Payload.Signature三段式, 无状态+自包含 — RS256非对称签名, 服务间无需共享密钥 (B1 Ch1 §5.4)
  - Token认证流程: 客户端→认证中心→JWT→后续请求带Authorization:Bearer→网关验证签名+过期→不存Session (B1 Ch1 §5.4)
  - 关键设计: Session vs JWT — Session(服务端存状态, 可即时撤销) vs JWT(无状态+好扩展, 但未过期Token无法撤销需要黑名单), 微服务场景偏向JWT

### 7. 收束 — 回到POST请求的旅程
  - 一次HTTP POST经历了: TCP/TLS握手→epoll等待→Reactor分发→ByteBuffer解码→Protobuf反序列化→JWT验签→业务处理→响应反转
  - 每一层都是为下一层服务: TCP为可靠, TLS为安全, epoll为高效, Reactor为扩展, Protobuf为跨语言
  - 理解通信栈是理解一切分布式系统的基础 — RPC/消息队列/Service Mesh都在此之上构建

---

### 核心悬念
**"RPC说让远程调用像本地一样, 但网络丢包+超时+序列化开销让它根本不像本地——RPC到底在和什么博弈?"**

→ 引出 RPC与服务治理: 从IPC到服务注册发现再到负载均衡 (02-rpc-service-governance)
