# T-9 WebSocket 协议实现 — 知识规划 (09 审计新增域)

> 项目: Tomcat 10.1.x | 类型: 规范实现 (JSR-356 / RFC 6455) | 🟡B 域 / 2 篇大纲
> 核心文件: WsFrameBase 982 + WsWebSocketContainer 1112 + WsSession 1091 + WsRemoteEndpointImplBase 1288 + AsyncChannelWrapperSecure 556 | ~5000 行 (tomcat/websocket 77 文件 13267 行)
> 基线: T-8 HTTP 解析 — 握手升级请求由 MimeHeaders 解析; T-9 回答 "握手成功后字节流如何切换成帧协议"
> 09 审计: websocket 77 文件 ≥50 定量预检 → 设计决策承载 (帧协议错误处理/掩码/心跳/容器管理) → 新增域

---

## §0.8 域审核前置

### 1. 域过载检查
- 核心类: 4 个 (WsFrameBase/WsWebSocketContainer/WsSession/WsRemoteEndpointImplBase)
- 4 ≤ 10 → 不触发拆分阈值 ✅

### 2. 淘汰清单
- WsRemoteEndpointImplBase 1288 行 (发送端) → 归 T-9 §2 概述 (发送缓冲/写锁), 不独立成域
- AsyncChannelWrapperSecure 556 (TLS 通道包装) → 归 §2 概述 (通道抽象)
- pojo/ 子包 (PojoMethodMapping 663) → 概述 (注解端点映射)
- Authenticator 族 (WebSocket 认证) → 边缘, 概述

### 3. 规范缺口
T-9 需要映射的规范:
- RFC 6455 (WebSocket 协议) → 帧格式/掩码/关闭握手/心跳
- JSR-356 (Java WebSocket API) → Endpoint/Session/Container 接口
- RFC 8441 (WebSocket over HTTP/2) → 概述 (10.1 支持面)

### 4. 禁止过度加域
- 帧字节解析的原子判断 (UTF-8 校验/掩码) 复用 T-8 HttpParser 分类基础设施 → 不重复
- 传输层 (NIO 通道) 属 T-4 → 不展开

### 项目类型判定
| 维度 | 值 |
|---|---|
| 类型 | 规范实现 (JSR-356 + RFC 6455) |
| 面试频度 | 中 (帧格式/掩码/关闭码) |
| 生产 | 中 (实时推送场景) |
| Hub | 低 (独立栈, 消费 T-8) |
| 方案 | 🟡 B (Pass 0-2, Pass 3 可选) |

---

## §一 逐源提取 (High 置信度, 源码实证)

| Source | Inferred Knowledge Point | Confidence |
|---|---|---|
| WsFrameBase.java:111 | processInputBuffer() — 帧读取主循环, 粘包/半包处理 (读不满 return false 等下次) | High |
| WsFrameBase.java:141-160 | processInitialHeader() — 首 2 字节: fin (b&0x80) + rsv (b&0x70>>>4) + opCode (b&0x0F); rsv 校验 (transformation.validateRsv) | High |
| WsFrameBase.java:155-165 | 控制帧 (PING/PONG/CLOSE) 严格校验: fin 必须 1 (controlFragmented) + opCode 白名单 (invalidOpCode) | High |
| WsFrameBase.java:226 | processRemainingHeader() — 长度扩展: 126→2 字节, 127→8 字节; 客户端掩码位解析 | High |
| WsFrameBase.java:276-311 | processData()/processDataControl() — 数据帧与控制帧分派; 控制帧载荷 ≤125 字节 (controlBuffer 125 分配 L55-56) | High |
| WsFrameBase.java:400-493 | processDataText()/processDataBinary() — 文本 (UTF-8 校验) / 二进制分派; 分片 (continuation) 状态机 | High |
| WsWebSocketContainer.java:79 | 实现 WebSocketContainer + BackgroundProcess — 容器 + 后台周期任务双角色 | High |
| WsWebSocketContainer.java:120-131 | connectToServer (客户端): Object pojo / Class annotated 重载 → connectToServerRecursive | High |
| WsWebSocketContainer.java:1062-1064 | backgroundProcess() — 周期任务: 会话超时扫描/心跳 | High |
| WsSession.java:537-553 | close() 双入口 (closeReason 参数化) — 本地/远端 1006 语义区分 (doClose 内部) | High |
| WsRemoteEndpointImplBase 1288 行 | 发送端: 写锁 + 队列化发送 (同步/异步双面) | High |

---

## §二 深度分类 (🔴🟡🟢)

| 知识点 | 级别 | 理由 |
|:--|:--:|:--|
| 帧头解析 + 协议错误处理 | 🔴 | T-9 核心 — 位运算取字段 + 违规即 WsIOException (CloseCodes.PROTOCOL_ERROR) |
| 半包/粘包状态机 (readAvail 循环) | 🔴 | 网络 I/O 设计核心 — 数据不足 return false, 缓冲等下次 |
| 分片 (continuation) 状态机 | 🟡 | RFC 6455 分片语义实现 |
| 控制帧 125 字节上限 | 🟡 | RFC 限制 + 预分配缓冲 |
| 容器管理 (BackgroundProcess) | 🟡 | 会话超时/心跳周期任务 |
| close 双语义 (1006 本地/远端) | 🟡 | 异常关闭与正常关闭区分 |

---

## §三 聚类 (机制边界 + 依赖图 + 教学顺序)

### 机制边界
1. 帧协议层 (WsFrameBase) — 字节 → 帧: 头解析/载荷/分片/控制帧
2. 会话与容器层 (WsSession/WsWebSocketContainer) — 生命周期/连接管理/后台任务
3. 发送面 (WsRemoteEndpointImplBase) — 写锁/队列/异步

### 依赖图
```
WsFrameBase (帧解析) ← WsSession (会话状态) ← WsWebSocketContainer (容器)
        ↑ 消费                                          ↓
  HttpParser/MimeHeaders (T-8, 握手)         BackgroundProcess (周期任务)
```

### 教学顺序 (2 篇)
1. **T-9 §1 帧协议解析**: WsFrameBase 头解析 (位运算/控制帧校验/长度扩展) → 半包状态机 → 分片 → 协议错误处理
2. **T-9 §2 会话/容器管理**: WsSession 生命周期 → WsWebSocketContainer 连接管理 → 心跳/超时 → 发送面概述

---

## §四 大纲规划 (2 篇)

### T-9 §1: 帧协议解析 (🟡B, 3 KP)
- 场景: 一条 4KB 的消息可能拆成 3 个帧到达 — 首帧只有 2 字节时解析器怎么办? 
- 机制 1: 帧头位运算 (fin/rsv/opCode 一字节三字段) — 为什么位运算? 单字节多语义
- 机制 2: 控制帧严格校验 (fin=1/白名单/125 上限) — 为什么严格? 协议状态机安全
- 机制 3: 半包/粘包状态机 (processInputBuffer 循环 + return false) — 为什么返回 false? 缓冲不足等下次
- 引出: 帧解析出来了, 会话 (连接状态/关闭语义) 怎么管理?

### T-9 §2: 会话与容器管理 (🟡B, 2 KP)
- 场景: 10 万个 WebSocket 连接挂着 — 谁扫描超时? 谁发心跳? 
- 机制 1: WsSession 生命周期 + close 双语义 (本地/远端 1006) — 为什么区分? 异常关闭 vs 正常
- 机制 2: WsWebSocketContainer 双角色 (Container + BackgroundProcess) — 为什么周期任务在容器? 统一管理
- 机制 3: 发送面概述 (WsRemoteEndpointImplBase 写锁/队列) — 为什么写锁? 多线程并发发送
- 引出: 单体 WebSocket 讲完 — 多节点怎么办? 集群 (T-10) 的会话复制怎么做?

---

## §五 跨域引用

| 域 | 关系 |
|:--|:--|
| T-8 HTTP 解析 | 握手请求 (Upgrade: websocket) 由 MimeHeaders/HttpParser 解析 |
| T-4 线程模型 | NIO 通道提供帧的字节流 |
| T-10 集群 | 集群会话复制 vs WebSocket 长连接 (不可复制 — 需要粘性会话) |
| ALI/Spring | spring-websocket (SockJS) 与 Tomcat 原生 JSR-356 的对照 |

---

## §六 20 问 (A 5 / B 6 / C 5 / D 4)

### A. 机制理解 (5)
1. fin/rsv/opCode 为什么压在一字节里? 位运算的语义?
2. 控制帧为什么强制 fin=1 + 125 字节上限? 不校验会怎样?
3. 半包处理为什么 return false 而不是阻塞等待?
4. 掩码位为什么客户端必须置 1 (服务端帧不掩码)?
5. close 握手为什么区分本地/远端 1006?

### B. 源码实证 (6)
6. processInitialHeader 的位运算取字段? (grep WsFrameBase L141-160)
7. 控制帧校验的 3 个条件? (grep WsFrameBase L155-165)
8. 长度扩展的 126/127 分支? (grep WsFrameBase.processRemainingHeader)
9. controlBuffer 分配大小? (grep WsFrameBase L55-56)
10. WsWebSocketContainer 实现的接口? (grep WsWebSocketContainer L79)
11. connectToServer 的重载形态? (grep WsWebSocketContainer L120-131)

### C. 推理深挖 (5)
12. 粘包时数据不足 return false — 谁负责再读? 调用方如何循环?
13. 分片 (continuation) 状态机怎么跨帧记忆? 中断怎么办?
14. 为什么 ping/pong 要后台周期发? 心跳的语义?
15. 写锁 vs 并发发送: 同步发送和异步发送怎么共存?
16. WsSession 与 HTTP Session 的生命周期差异?

### D. 跨域扩展 (4)
17. 本域 vs T-8: 帧协议与 HTTP 报文的解析范式差异?
18. 本域 vs T-4: NIO 通道如何承载帧流?
19. 本域 vs T-10: WebSocket 长连接能集群复制吗?
20. 本域 vs spring-websocket: JSR-356 原生 vs SockJS 的取舍?

---

## §七 淘汰与排除

| 项 | 理由 |
|:--|:--|
| WsRemoteEndpointImplBase 独立成域 | 发送面是会话的附属面, §2 概述 |
| pojo/ 注解映射 | JSR-356 注解规范面, 低设计决策 |
| AsyncChannelWrapper 族 | T-4 通道抽象的 WebSocket 特化 |