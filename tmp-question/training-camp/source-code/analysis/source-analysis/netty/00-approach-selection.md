# Netty 方案选择

> 框架: Netty 4.2.15.Final
> 方法: 方法论/04 — 决策树 (🔴→A / 🟡+设计决策→B)
> 域数: 4🔴 + 7🟡 = 11 域
> 产出日期: 2026-08-06

---

## 决策树

```
🔴 → A (全量深挖: Pass 0-3 + 演化追溯 + 极简复现)
🟡 + 有设计决策 → B (标准: Pass 0-2)
🟡 + 简单机制 → C (轻量: Pass 0-1)
🟡 + 胶水/封装 → D (扫描: Pass 1)
```

---

## 逐域方案

### A 方案 (🔴 × 4)

| # | 域 | 层级 | Passes | 演化追溯 | 极简复现 | 理由 |
|:--:|------|:--:|------|:--:|:--:|------|
| 1 | **ByteBuf** | 🔴 | 0→1→2→3 | ✅ | ✅ | 引用计数+零拷贝=Netty定义特征 |
| 2 | **EventLoop** | 🔴 | 0→1→2→3 | ✅ | ✅ | 单线程事件循环=Netty定义特征 |
| 3 | **Pipeline+Handler** | 🔴 | 0→1→2→3 | ✅ | ✅ | 责任链=Netty定义特征 |
| 4 | **内存池化** | 🔴 | 0→1→2→3 | ✅ | ✅ | Buddy分配=Netty性能核心 |

### B 方案 (🟡 × 7)

| # | 域 | 层级 | Passes | 理由 |
|:--:|------|:--:|------|------|
| 5 | **Promise/Future** | 🟡 | 0→1→2 | 异步结果+监听器通知，有设计决策 |
| 6 | **Bootstrap** | 🟡 | 0→1→2 | Fluent API+生命周期(含ChannelPool)，有设计决策 |
| 7 | **Codec 框架** | 🟡 | 0→1→2 | ByteToMessageDecoder+4拆包器(含SSL子话题)，有设计决策 |
| 8 | **HTTP Codec** | 🟡 | 0→1→2 | HTTP/1.1分块/聚合/WebSocket，有协议级设计决策 |
| 9 | **HTTP/2 Codec** | 🟡 | 0→1→2 | 二进制帧/流多路复用/HPACK/流控，有独立设计决策 |
| 10 | **Epoll 原生传输** | 🟡 | 0→1→2 | JNI直调epoll vs NIO Selector，有设计决策 |
| 11 | **HashedWheelTimer** | 🟡 | 0→1→2 | 时间轮算法，有算法级设计决策 |

---

## Hub 升级检查

无域达到 ≥10 依赖阈值的 Hub 升级条件。

| 域 | 依赖方 | 升级? |
|---|---|---|
| ByteBuf | 内存池化、Pipeline+Handler (2) | 否 |
| EventLoop | Promise/Future、Pipeline、Bootstrap、Epoll (4) | 否 |
| Pipeline+Handler | Bootstrap、Codec、HTTP、HTTP/2 (4) | 否 |

---

## 执行顺序 (拓扑)

```
1.  ByteBuf           A → Pass 0-3 + trace + harness
2.  EventLoop         A → Pass 0-3 + trace + harness
3.  Promise/Future    B → Pass 0-2
4.  Pipeline+Handler  A → Pass 0-3 + trace + harness
5.  内存池化           A → Pass 0-3 + trace + harness
6.  Bootstrap         B → Pass 0-2
7.  Codec 框架         B → Pass 0-2
8.  HTTP Codec        B → Pass 0-2
9.  HTTP/2 Codec      B → Pass 0-2
10. Epoll 原生传输     B → Pass 0-2
11. HashedWheelTimer  B → Pass 0-2
```

---

## 前置依赖验证

所有域的前置依赖已按拓扑排序就位：

```
ByteBuf          → 无前置 ✅
EventLoop        → 无前置 ✅
Promise/Future   → EventLoop ✅
Pipeline+Handler → EventLoop, ByteBuf ✅
内存池化          → ByteBuf ✅
Bootstrap        → EventLoop, Pipeline ✅
Codec 框架        → Pipeline ✅
HTTP Codec       → Codec ✅
HTTP/2 Codec     → Codec ✅
Epoll            → EventLoop ✅
HashedWheelTimer → 独立 ✅
```

---

## 下一步

读 methodology/01 → 从 ByteBuf 开始执行 Pass 0-3 源码分析。
