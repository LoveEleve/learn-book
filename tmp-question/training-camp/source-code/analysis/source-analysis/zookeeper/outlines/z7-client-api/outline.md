# Z-7 Client API — 客户端门面与双线程协议

> 前置: [[Z-5-Session]] (会话协商) + [[Z-6-Watcher]] (watch 注册) + [[Z-4-Processor链]] (服务端面) | 引出: [[Z-8-Recipes]] | 对照: Jedis/Redisson (Redis 客户端)
> 🟡 B | 8 KP | [模式: 门面 + 双线程 + XID 匹配]
> Pass 2 闭环: q1(门面) q2(双线程) q3(连接会话) q4(响应回调)

**读者处境**: 客户端 API 怎么组织? 请求怎么匹配响应? 断线重连怎么恢复? 这篇拆 ZooKeeper (门面) + ClientCnxn (SendThread/EventThread 双线程 + 队列 + XID 匹配)。

### 1. 门面 — 同步/异步双 API + WatchRegistration

场景: API 怎么组织?
源码路径:
- **ZooKeeper** (3118): 同步 (getData 阻塞) / **异步 (getData 带 AsyncCallback — cb 回调)** 双面; 每 API 同步版调异步版 + wait (内部)
- **WatchRegistration 族** (ZooKeeper:265-362): **Exists/Data/Child/AddWatch 四种注册** — getWatches(rc) 按错误码决定注册哪些路径 (成功才注册)
- **ZooKeeperState**: CONNECTING/CONNECTED/CLOSED/AUTH_FAILED 状态机 (客户端侧)
- **构造**: connectString + sessionTimeout + watcher → ClientCnxn 创建 + SendThread/EventThread 启动
关键设计 (q1): **异步为内核, 同步为薄封装**; watch 注册绑定响应 (失败不注册)。[模式: 门面]

### 2. 双线程 — SendThread + EventThread + 三队列

场景: 网络与事件怎么分离?
源码路径:
- **SendThread** (ClientCnxn:969+): 连接管理 + 收发主循环 — **doTransport (socket) + ping 节奏** (L1260-1274: timeToNextPing = readTimeout/2 - idleSend, MAX_SEND_PING_INTERVAL=10s 双条件)
- **EventThread** (L469+): **事件 + 回调统一队列** (waitingEvents) — 串行处理 (Watcher.process + AsyncCallback); **queueEventOfDeath** (L1235: AUTH_FAILED 后终止 — 生命周期收尾)
- **三队列**: outgoingQueue (LinkedBlockingDeque 发送) + pendingQueue (ArrayDeque 等待响应) + eventThread 事件队列 (L148-153)
- **XID 预定义** (L120-127): **PING_XID=-2 / AUTHPACKET_XID=-4 / SET_WATCHES_XID=-8 / NOTIFICATION_XID=-1** (服务端) — 特殊包不匹配; **XID 惰性分配** (五次 REVIEW): 发送时 setXid (L334-336 注释 + NIO:115) — **ping/auth 不分配不入 pendingQueue** (L114-116)
关键设计 (q2): **网络线程 (Send) 与业务线程 (Event) 分离**; 三队列解耦。[模式: 双线程]

### 3. 连接与会话 — primeConnection + 超时面

场景: 连接建立与恢复?
源码路径:
- **primeConnection** (L1005-1095): **ConnectRequest (0, lastZxid, sessionTimeout, sessId, sessionPasswd, readOnly)** — **seenRwServerBefore ? sessionId : 0** (重连恢复 vs 新会话); **watch 重注册** (setWatches/setWatches2 — 五类 watch 分批 SET_WATCHES_MAX_LENGTH, DISABLE_AUTO_WATCH_RESET 配置); auth 重放 (AUTHPACKET_XID); **outgoingQueue.addFirst 倒序** (连接 → auth → watch)
- **startConnect** (L1132-1167): 重连 sleep 1000 随机 + CONNECTING + SASL + connect; **hostProvider.next 多地址轮询** (L1197 — 单点故障换址); **chroot 前缀** (prependChroot L1097-1112 — 多租户隔离)
- **超时面** (L1244-1258): **会话超时 = expirationTimeout - idleRecv → SessionTimeoutException** / 连接超时 = connectTimeout → ConnectionTimeoutException
- **readConnectResult** (ClientCnxnSocket:131-150): 服务端回 ConnectResponse → **onConnected (sessionId/passwd/timeout 更新)**
- **read-only 模式**: CONNECTEDREADONLY → 找 RW server (pingRwTimeout 指数 100→60000)
关键设计 (q3): **重连 = 会话恢复 + watch 重注册双动作**; 超时双面 (会话/连接)。[模式: 重连恢复]

### 4. 响应与回调 — XID 匹配 + finishPacket

场景: 响应怎么回配请求?
源码路径:
- **readResponse** (ClientCnxnSocket): replyHeader.xid → **pendingQueue 匹配** (FIFO — 服务端按序响应) + **Xid out of order 强校验** (L945-949) → finishPacket; **queuePacket 闭包** (L334-360): 关闭后入队 → conLoss 立即结算; closeSession 标记 closing
- **finishPacket** (ClientCnxn:725-761): **watchRegistration.register (成功才注册)** + **watchDeregistration → WatchRemoved 事件** (L730-750) + **cb null → 同步 notifyAll (同步 API) / cb → eventThread.queuePacket (异步回调)** (L752-760)
- **conLossPacket** (L782-797): 连接丢失 → 错误码按状态 (AUTHFAILED/SESSIONEXPIRED/CONNECTIONLOSS) — 挂起包全部结算
- **queueEvent** (L763-771): err SESSIONEXPIRED/CONNECTIONLOSS → Disconnected 状态事件
- **EventThread 处理** (L533+): Watcher.process + AsyncCallback.processResult 串行

## 代码类型
Architecture (门面 + 协议线程)

## 负面空间 — 客户端刻意不做的事

- **不做多路复用协议扩展**: 单连接串行请求 (XID 递增 — 无 pipelining 语义面)
- **不做自动重试业务操作**: 重连恢复会话但请求不重放 (连接丢失错误交业务)
- **不做本地缓存**: 每次请求都到服务端 (对照 Redis 客户端缓存)
- **不做 watch 持久化**: 断线重连 setWatches 重注册 (服务端已触发的不补)
- **不做协议级压缩**: 明文请求 (对照 gRPC 压缩)

→ 引出: 客户端模式怎么复用? → [[Z-8-Recipes]]
