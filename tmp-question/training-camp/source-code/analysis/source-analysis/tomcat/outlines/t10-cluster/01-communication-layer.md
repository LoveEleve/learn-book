# T-10 §1 通信层 — GroupChannel 与组播成员管理

> 依赖 T-1 | 🟡B | 3 KP | 09 审计新增域

**读者处境**: 生产环境一台 Tomcat 扛不住 — 三台组成集群 — 第一个问题: 节点之间怎么互相发现? 新节点加入怎么被大家知道? 一个节点挂了, 其他节点怎么在几秒内察觉并把它的请求接走? Tomcat 的答案不是注册中心, 而是 **UDP 组播心跳**。

### 1. GroupChannel 门面 — 三组件可插拔组合

场景: 集群通信需要三件事: ① 成员发现 (谁在集群里) ② 消息发送 (发给谁) ③ 消息接收 (谁发给我)。这三件事的技术选型可以完全不同 — 组播做成员发现, TCP 做数据收发 — 所以不能写死成一个类。

源码路径: `GroupChannel.java:67` — `class GroupChannel extends ChannelInterceptorBase implements ManagedChannel, JmxChannel, GroupChannelMBean`。门面持有三大组件, 通过 setter 注入 (`setChannelReceiver` L528 / `setChannelSender` L533 / `setMembershipService` L538), 读取通过 getter (L513-523)。`messageReceived(ChannelMessage)` (L264) / `memberAdded(Member)` (L351) / `memberDisappeared(Member)` (L367) 三个事件入口向上分派。

关键设计: **组合 + 可插拔 — 为什么?** 成员发现用组播 (UDP, 广播式), 数据收发用 TCP (可靠流式) — 单一抽象无法同时优雅表达; setter 注入让部署者可以换实现 (比如生产环境不用组播而用 StaticMembershipService 静态配置)。ChannelInterceptorBase 继承意味着 GroupChannel 本身也是拦截器链的一环 — 整个通信栈是"拦截器包裹拦截器"的洋葱结构。 [模式: Facade + Strategy]

数据流: `SimpleTcpCluster` (T-10 §2) 构造 → `setMembershipService(new McastServiceImpl(...))` + `setChannelSender(new ReplicationTransmitter(...))` + `setChannelReceiver(...)` → GroupChannel 组合就绪 → 成员事件 (memberAdded) 经 GroupChannel 分派给监听器。

### 2. 组播心跳 — McastServiceImpl 双线程成员管理

场景: 节点 A 加入集群 — 它往组播地址发一个 "我是 A" 的 UDP 包 — 组播的特性: 组内所有节点都收到 — 于是所有节点瞬间知道 A 加入。A 之后每 5 秒发一次心跳 — 若 10 秒没收到, 大家判定 A 死亡。

源码路径: `McastServiceImpl.java:59-61` — `protected volatile boolean doRunSender/doRunReceiver` 双线程开关; L85 `sendFrequency`; L151-176 构造参数: `sendFrequency` (ping 间隔) + `expireTime` (成员过期时间); L222 `mcastSoTimeout = sendFrequency` — **接收超时与发送频率同步**; L268-269 `doRunSender = true; sender = new SenderThread(sendFrequency)` — 发送线程周期广播; L284 `memberwait = sendFrequency * 2` — **启动时等 2 个周期**让成员表建立。

关键设计: **组播 + 双线程 + 超时对齐 — 为什么?** ① 组播: 成员发现的复杂度是 O(1) 广播 — 无需注册中心, 无单点; ② 双线程: 发送线程周期性广播自己的存在, 接收线程监听别人 — 收发解耦; ③ 超时对齐: mcastSoTimeout 设为 sendFrequency — 接收线程的阻塞读恰好覆盖两个心跳间隔 — 一个心跳都不丢; expireTime 判定死亡。2 倍等待 (waitForMembers) 保证启动时成员表已收敛。 [模式: Heartbeat + Multicast]

数据流: 节点启动 → `McastServiceImpl.start()` → 发送线程每 sendFrequency 广播 Member 包 → 接收线程收包刷新成员表 → 节点死亡 → 连续过期 → `memberDisappeared` 事件 → GroupChannel 分派 → 应用层 (DeltaManager) 接管会话。

### 3. 拦截器链 — ChannelInterceptor 洋葱

场景: 组播能发现成员了, TCP 能传消息了 — 但通信栈需要横切能力: 心跳检测 (TcpFailureDetector)、大消息分片 (FragmentationInterceptor)、消息统计 — 这些不该写进 Channel 本体。

源码路径: `GroupChannel.java:159` `addInterceptor(ChannelInterceptor)` + L183 `heartbeat()` — 心跳沿拦截器链逐级下传; 继承 `ChannelInterceptorBase` — 每个拦截器默认透传 (不做事的基类), 子类只覆写关心的切面。

关键设计: **拦截器链 — 为什么不用继承?** 横切能力是正交的 (心跳/分片/统计互不相关) — 继承会组合爆炸 (2^n 子类); 链式拦截器每个切面一个类, 任意组合。与 Servlet Filter (T-3) 同构 — 但这是**双向链**: 消息出站 (send) 和入站 (messageReceived) 都要穿链, 拦截器在两侧都有机会。 [模式: Interceptor Chain — 与 T-3 Pipeline 对照]

数据流: `channel.heartbeat()` → 沿拦截器链 → TcpFailureDetector 检查连接健康 → 下行到 GroupChannel → 组播收发 → 入站消息 → messageReceived 沿链上溯 → 分派给监听器。

→ 引出 §2 会话复制 — 节点间通信通了 — 现在的问题是业务面: 用户 Session 在节点 A, 请求却到了节点 B — 会话数据怎么复制过去? 全量复制太贵 — 怎么只复制脏数据?