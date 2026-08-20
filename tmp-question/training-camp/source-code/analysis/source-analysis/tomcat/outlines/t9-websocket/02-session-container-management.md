# T-9 §2 会话与容器管理 — WsSession 生命周期 + 后台任务

> 依赖 T-9 §1 + T-1 | 🟡B | 3 KP | 09 审计新增域

**读者处境**: T-9 §1 讲完帧解析 — 现在的问题是连接的管理: WebSocket 连接是**长连接** — 可能挂几小时。谁记住每个连接的状态? 谁扫超时? 谁发心跳? 一个实例 10 万连接 — 周期任务不能每连接一个线程。

### 1. WsSession 生命周期 — 打开/关闭状态与双语义 close

场景: 服务端 `session.close()` 主动关, 客户端断网 (TCP 没发 FIN) 被动关 — 两种情况都要走到"连接已关闭" — 但语义不同: 正常关闭走 RFC 6455 关闭握手 (close 帧+回包), 异常断连只能本地判定。

源码路径: `WsSession.java:537-553`。`close()` (L537) 无参 → `close(new CloseReason(NORMAL_CLOSURE, ""))` (L538) → 带参 `close(CloseReason)` (L543) → 内部 `doClose(closeReason, closeReason)` (L544)。注释 (L549-553) 明示: **规范 §2.1.5 — 本地端点收到 1006 (异常关闭) 时必须有内部 close 路径** — 区分"发给远端的 reason"和"本地记录的 reason"两个参数。会话状态字段 (L107-119): messageHandler 三个 (text/binary/pong) + 缓冲大小 + **maxIdleTimeout (L115, 默认 0 = 不超时)** + lastActiveRead/Write (L116-117) — 空闲检测的数据源。

关键设计: **双参数 close — 为什么区分远端/本地语义?** 1006 (ABNORMAL_CLOSURE) 不是通过 close 帧传递的 — 它是**本地生成的**状态 (TCP 断/超时/协议错)。正常关闭: 发 close 帧给远端, 等回 close 帧, 才标记关闭; 异常: 远端永远不知道 — 本地必须自我完成状态迁移。`doClose` 的第二个参数就是"本地自我认知" — 确保两端语义一致时状态一致, 不一致时本地仍能收敛。 [模式: Dual-Semantics State Transition]

数据流: `WsSession.close(CloseReason)` → `doClose(reason, reason)` → 发 close 帧 → 等远端 close 帧 → `sessionCloseTimeoutExpiry` (L119) 记录等待超时 → 完成状态迁移 → 通知 `onClose` 回调。

### 2. WsWebSocketContainer 双角色 — Container + BackgroundProcess

场景: 10 万连接挂了一天 — 其中 5000 个客户端断了网但 TCP 没拆 — 谁发现? 靠每个连接一个线程轮询? 不行 — 10 万线程会压垮服务器。

源码路径: `WsWebSocketContainer.java:79` — `public class WsWebSocketContainer implements WebSocketContainer, BackgroundProcess`。`backgroundProcess()` (L1062-1074): 注释 "This method gets called once a second" — `backgroundProcessCount++` → 每 `processPeriod` (默认 10, getProcessPeriod L1085-1088) 次清零 → **遍历 sessions.keySet() 逐个 `checkExpiration()` + `checkCloseTimeout()`**。

关键设计: **双接口 — 为什么周期任务在容器里?** BackgroundProcess 是 Tomcat 的通用后台任务接口 (StandardServer.utilityExecutor 周期调度) — 容器实现它 = 后台任务复用全局调度, 不新建线程。扫描是**批量遍历**而非每连接一线程 — 10 万连接一次遍历 cost 低 (纯内存检查), 周期 10 秒可接受。checkExpiration 读 lastActiveRead/Write 判空闲 (maxIdleTimeout), checkCloseTimeout 判关闭握手的等待是否超时 — 两个检查都是 O(1) 字段比较。 [模式: Background Batch Scanning]

数据流: `StandardServer.utilityExecutor` (T-1) 每秒调 `backgroundProcess()` → 计数到 10 → 遍历 sessions → checkExpiration (空闲超时) / checkCloseTimeout (关闭超时) → 超时的连接主动 close(1006 本地语义)。

### 3. 发送面概述 — WsRemoteEndpointImplBase 的写锁与队列

场景: 两个线程同时对同一连接 `sendMessage()` — 一个写大文本帧, 一个写 ping — 字节流如果交错写入, 对端解析必错。发送端怎么串行化?

源码路径: `WsRemoteEndpointImplBase.java` (1288 行)。发送面设计: **写锁串行化** — 同步发送 (blocking) 持锁写完整个帧再释放; 异步发送 (Future) 入队待发送。锁粒度是"连接级" — 同一连接的所有发送互斥, 不同连接并行不受影响。

关键设计: **写锁 — 为什么是锁而不是队列?** 帧是原子的协议单元 — 半帧发送出去对端无法解析。同步发送: 锁内写完整帧, 简单直接 (但长帧会阻塞其他发送者); 异步: 队列化避免阻塞 — 权衡点在帧大小 vs 并发度。WebSocket 帧通常小 (≤125 控制帧), 同步锁的成本可接受 — 这是"协议原子性"对并发模型的决定性影响。 [模式: Per-Connection Write Lock]

数据流: `session.getBasicRemote().sendText(msg)` (同步) / `getAsyncRemote().sendText(msg)` (异步) → WsRemoteEndpointImplBase → 写锁 → 帧编码 (T-9 §1 逆过程) → NIO 通道写 (T-4) → 写后更新 lastActiveWrite。

→ 引出 T-10 集群 — 单节点 WebSocket 会话管理完了 — 但生产是集群: 会话复制 (T-10 DeltaManager) 能复制 WebSocket 长连接吗? 不能 — 长连接是节点私有的 — 集群怎么解决粘性会话和故障转移? T-10 的 tribes 通信层怎么承载节点间消息?