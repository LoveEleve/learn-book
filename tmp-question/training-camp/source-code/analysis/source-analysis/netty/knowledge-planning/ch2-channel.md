# Ch2 NIO Channel — 知识规划

> 来源: 7 源文件 | ~2580 行 | 学完第二章读者基线: ByteBuffer → Channel

---

## 01 提取 — 逐源映射

### Channel.java + NetworkChannel.java (接口)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Channel.java | **Channel 顶级接口**: close() + isOpen() — 所有 I/O 通道的基础 | High |
| NetworkChannel.java | **bind() + setOption/getOption**: 绑定地址 + Socket 选项 (SO_RCVBUF/SO_SNDBUF) | High |

### SocketChannel.java + ServerSocketChannel.java (抽象类)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| SocketChannel.java | **connect(address)**: 非阻塞连接 — 返回 false 表示需等待 | High |
| SocketChannel.java | **finishConnect()**: 完成非阻塞连接 | High |
| SocketChannel.java | **read(buf) / write(buf)**: 返回 int — 0 表示无数据/写满 | High |
| SocketChannel.java | **configureBlocking(boolean)**: 切换阻塞/非阻塞 | High |
| ServerSocketChannel.java | **bind(address)**: 绑定监听地址 | High |
| ServerSocketChannel.java | **accept()**: 返回新 SocketChannel — 阻塞直到有连接 | High |

### SocketChannelImpl.java (1129行 — TCP 客户端实现)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| SocketChannelImpl.java:76-79 | **readLock / writeLock**: 两个独立的 ReentrantLock — 读/写分离锁，不可互锁 | High |
| SocketChannelImpl.java:97-98 | **state 三态**: ST_CONNECTED(2) / ST_CLOSING(3) — synchronized stateLock | High |
| SocketChannelImpl.java:104-105 | **readerThread / writerThread**: 记录当前读写线程 — close 协调用 | High |
| SocketChannelImpl.java:336-367 | **read()**: beginRead → IOUtil.read(fd, buf, -1) → endRead — 阻塞模式下 while-retry on INTERRUPTED | High |
| SocketChannelImpl.java:448-476 | **write()**: beginWrite → IOUtil.write(fd, buf, -1) → endWrite | High |
| SocketChannelImpl.java:351-357 | **阻塞/非阻塞分叉**: blocking→while(retry INTERRUPTED), non-blocking→单次调用 | High |
| SocketChannelImpl.java:300-319 | **beginRead/endRead**: 阻塞模式下 Thread park/unpark — IOUtil.configureBlocking 底层非阻塞 fd | High |
| SocketChannelImpl.java:672 | **connect()**: 非阻塞 connect → fdConfigureBlocking(false) → ND.connect → ST_CONNECTED on success | High |
| SocketChannelImpl.java:757 | **finishConnect()**: ND.finishConnect — poll 是否连接完成 | High |
| SocketChannelImpl.java:248 | **kill()**: 强制关闭 — postClose + countDown closeLatch | High |

### ServerSocketChannelImpl.java (556行 — 服务端实现)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ServerSocketChannelImpl.java | **accept()**: 返回 new SocketChannelImpl → 调用 IOUtil.configureBlocking(newfd, true) — **新 socket 永远是阻塞模式** | High |
| ServerSocketChannelImpl.java | **bind()**: Net.bind(fd, isa) → state = ST_BOUND | High |
| ServerSocketChannelImpl.java | **close()**: 等待 readerThread 完成 (ST_CLOSING + stateLock)，不强制中断 | High |

### SocketAdaptor.java (439行 — NIO→BIO 适配)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| SocketAdaptor.java | **read() 阻塞转换**: readLock.lock → IOUtil.read(fd, buf, -1, nd) → while(retry INTERRUPTED) | High |
| SocketAdaptor.java | **getInputStream()**: 返回 ChannelInputStream — return 0 when no data | High |
| SocketAdaptor.java | **connect()**: 同步阻塞版本 — 先用非阻塞 connect, 然后 wait 直到完成 | Medium |

---

## 01 聚合 — 跨文件汇总

N=7, P1=≥3, P2=2, P3=1

### P1 — 共识 (≥3 文件, 6 KPs)

| Knowledge Point | 出现文件 |
|----------------|---------|
| read(buf) — 读取数据 | SocketChannel + SocketChannelImpl + SocketAdaptor |
| write(buf) — 写入数据 | SocketChannel + SocketChannelImpl + SocketAdaptor |
| connect(address) — 非阻塞连接 | SocketChannel + SocketChannelImpl + SocketAdaptor |
| configureBlocking(boolean) — 切换阻塞/非阻塞 | SocketChannel + SocketChannelImpl + ServerSocketChannel |
| close() / isOpen() — 生命周期 | Channel + SocketChannelImpl + ServerSocketChannelImpl |
| bind(address) — 绑定地址 | NetworkChannel + ServerSocketChannel + SocketChannelImpl |

### P2 — 局部重要 (2 文件, 5 KPs)

| Knowledge Point | 出现文件 |
|----------------|---------|
| accept() — 接受新连接 | ServerSocketChannel + ServerSocketChannelImpl |
| finishConnect() — 完成非阻塞连接 | SocketChannel + SocketChannelImpl |
| readLock/writeLock 分离 — 读写独立 | SocketChannelImpl + SocketAdaptor |
| readerThread/writerThread — close 协调 | SocketChannelImpl (impl detail, cross-section visible) |
| IOStatus 归一化 — 返回 -1/0/n | SocketChannelImpl + IOUtil (native) |

### P3 — 独立 (1 文件, 6 KPs)

| Knowledge Point | 出现文件 |
|----------------|---------|
| state 三态 (ST_CONNECTED/ST_CLOSING) | SocketChannelImpl |
| beginRead/endRead — 阻塞模式下 Thread park | SocketChannelImpl |
| beginWrite/endWrite | SocketChannelImpl |
| accept 返回的 SocketChannel 永远是阻塞模式 | ServerSocketChannelImpl |
| SocketAdaptor — NIO→BIO 适配桥 | SocketAdaptor |
| kill() — 强制关闭 + closeLatch | SocketChannelImpl |

---

## 02 深度分类

### 🔴 Deep (承载核心设计决策)

| KP | 为什么🔴 |
|----|---------|
| read() / write() — 非阻塞语义 (返回 0) | NIO 与 BIO 的根本区别 — read()=0 ≠ 无数据, write()=0 ≠ 缓冲区满。这是整个非阻塞 I/O 的入口 |
| configureBlocking(boolean) — 两种模式 | 整个 Netty Channel 设计的基础 — 理解阻塞/非阻塞差异才能理解 EventLoop 为什么必须是单线程 |
| connect() + finishConnect() — 非阻塞连接 | 非阻塞 connect 是 TCP 的经典陷阱 — 立即返回 false 但 TCP 握手仍在进行 |
| accept() 返回的 SocketChannel 永远是阻塞 | JDK 的隐藏行为 — ServerSocketChannelImpl:296 硬编码 configureBlocking(newfd, true) — 无论 ServerSocket 模式 |

### 🟡 Working (有设计决策, 非核心)

| KP | 01 Pri | 说明 |
|----|--------|------|
| readLock/writeLock 分离 | P2 | 读/写不互锁 — 与 EventLoop 单线程互补 |
| beginRead/endRead | P3 | 阻塞模式下的线程 park/unpark 机制 |
| state 三态 | P3 | Close 流程的核心 — stateLock 保护 |
| bind() | P1 | 地址绑定 — Server/Client 共有 |
| IOStatus 归一化 | P2 | native -1→Java 0 — 屏蔽 OS 差异 |

### 🟢 Surface (机制性了解即可)

| KP | 01 Pri | 放在哪 |
|----|--------|-------|
| SocketAdaptor NIO→BIO 适配 | P3 | 和 connect/read/write 一起 — BIO 用户的过渡桥 |
| kill() | P3 | Close 流程 — 资源释放 |
| readerThread/writerThread | P3 | Close 协作 — 等待线程完成 |
| Channel.close()/isOpen() | P1 | 和 connect/read 一起 — 生命周期 |

---

## 03 聚类

### Cluster A: 读与写 — SocketChannel 核心 (5 KPs)
  机制边界: read/write 的返回值语义 — 不涉及连接/绑定
  1. read(buf) — 非阻塞返回 0, 阻塞 while 重试 INTERRUPTED (SocketChannelImpl.java:336-367)
  2. write(buf) — 非阻塞写满返回 0, 阻塞写满阻塞 (SocketChannelImpl.java:448-476)
  3. readLock/writeLock 分离 — 读/写互不阻塞 (SocketChannelImpl.java:76-79)
  4. beginRead/endRead — 阻塞模式下的线程协调 (SocketChannelImpl.java:300-319)
  5. IOStatus.normalize — native -1 → Java 0 — 屏蔽 OS 差异

### Cluster B: 连接与接受 — TCP 生命周期 (4 KPs)
  机制边界: Channel 的出生和绑定 — 依赖 A
  1. connect() — 非阻塞连接, 返回 false 表示等待 (SocketChannelImpl.java:672)
  2. finishConnect() — poll 连接是否完成 (SocketChannelImpl.java:757)
  3. bind() — 绑定本地地址
  4. accept() — 返回 SocketChannel, **始终阻塞** (ServerSocketChannelImpl.java:296 configureBlocking(true))

### Cluster C: 阻塞对比 + 桥接 (4 KPs)
  机制边界: 阻塞 vs 非阻塞 + BIO 过渡 — 依赖 A+B
  1. configureBlocking(boolean) — 切换阻塞/非阻塞 (SocketChannel.java + SocketChannelImpl)
  2. 阻塞模式: while-retry on INTERRUPTED
  3. 非阻塞模式: 单次调用, 返回 0 需上层处理
  4. SocketAdaptor — NIO→BIO 适配桥 (SocketAdaptor.java — 439行)

### 教学顺序: A(读写) → B(连接) → C(阻塞对比)
  A 是数据操作 (依赖 Ch1 ByteBuffer), B 是 TCP 语义, C 是综合对比

---
