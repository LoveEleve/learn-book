# T-10 §2 会话复制 — AbstractReplicatedMap 与 DeltaManager

> 依赖 T-10 §1 + T-1 | 🟡B | 3 KP | 09 审计新增域

**读者处境**: T-10 §1 讲完节点间通信 — 现在业务面: 集群模式下 Session 存在节点 A, 负载均衡把下一个请求转到节点 B — B 没有这个 Session, 用户被登出。两种解法: ① 粘性会话 (负载均衡把同一用户固定到一个节点 — 但节点挂了就丢会话) ② 会话复制 (每个节点都有一份 — 节点挂了请求还能接走)。Tomcat 的 DeltaManager 实现 ② — 怎么复制才不把网络打爆?

### 1. 复制触发 — isDirty 脏复制三条件

场景: 一个用户点按钮 → Session 里 setAttribute 一次 → 如果整个 Session 序列化广播给所有节点 — 一次点击 1KB, 每秒 10 万点击 = 100MB/s 网络流量 — 集群直接被打爆。复制必须**只传变化的部分**。

源码路径: `AbstractReplicatedMap.java:441-460`。`replicate(Object key, boolean complete)`: 前置条件链 — `entry == null` return → `!entry.isSerializable()` return → **`entry.isPrimary() && backupNodes.length > 0`** (只有主节点才复制, 且有备份节点才复制); 然后三条件: `repl = complete || isDirty || isAccess` — `isDirty()` (值真的变了) / `isAccessReplicate()` (访问也要同步 — 会话最后访问时间) / `complete` (强制全量)。L514-516 `replicate(boolean complete)` 周期线程版本 — 遍历全部 key 逐个复制。

关键设计: **脏复制 — 为什么?** 全量复制的时间/带宽成本 O(会话数×会话大小), 脏复制成本 O(变化数) — 差一个数量级。三条件分别对应三种复制必要性: 值变了 (isDirty) / 元数据变了 (isAccess — 会话超时计时依赖最后访问时间) / 显式全量 (complete — 节点加入时的初始同步)。前置条件 isPrimary 保证复制方向唯一 (备节点不重复发) — 避免复制风暴。 [模式: Dirty Replication]

数据流: `Session.setAttribute()` → StandardSession 标记 isDirty → DeltaManager 检测 (request 结束/周期) → `replicate(key, complete=false)` → isPrimary 校验 → 序列化变化部分 → tribes 通道发备份节点。

### 2. 事件协议 — DeltaManager 的 EVT_ 族

场景: 复制的粒度不是"整个 Session"而是"Session 生命周期里的每个事件": 创建 (新会话在 A 产生, B/C 也要有)、更新 (属性变化)、访问 (刷新超时)、过期 (销毁)。每种事件一个消息类型 — 接收方按类型走不同的处理分支。

源码路径: `DeltaManager.java:92-105` — 计数器族: `counterReceive_EVT_SESSION_CREATED / EVT_ALL_SESSION_DATA / EVT_SESSION_DELTA / EVT_SESSION_ACCESSED / EVT_SESSION_EXPIRED / EVT_CHANGE_SESSION_ID / EVT_ALL_SESSION_TRANSFERCOMPLETE` 与 send 侧对应。类头 L55 `extends ClusterManagerBase`。

关键设计: **事件化协议 + 双向计数器 — 为什么?** 事件化 = 增量语义 — 每个事件携带最小必要数据 (SESSION_DELTA 只带变化属性); 双向计数器 (receive/send 各一套) 是**运维观测面** — JMX 暴露收发量, 集群不均衡时一眼看出哪个节点在丢事件。EVT_GET_ALL_SESSIONS / EVT_ALL_SESSION_DATA 是**全量同步协议**: 新节点加入时主动拉全量 — 这补上了脏复制在"初始状态"上的缺口: 脏复制只能传变化, 全量同步负责初始一致。 [模式: Event Protocol + Full Sync Bootstrap]

数据流: 会话创建 → DeltaManager 发 EVT_SESSION_CREATED (含 Session 数据) → 其他节点接收 → 本地创建 Session → 属性变化 → EVT_SESSION_DELTA → 接收方更新 → 会话过期 → EVT_SESSION_EXPIRED → 接收方销毁 → 新节点加入 → 发 EVT_GET_ALL_SESSIONS → 对方回 EVT_ALL_SESSION_DATA (全量) → EVT_ALL_SESSION_TRANSFERCOMPLETE 结束。

### 3. SimpleTcpCluster 集成 + 粘性会话对照

场景: 复制机制讲完了 — 它怎么挂到 Catalina 上? 每个 Context 的 Manager 换成集群版 — 集群组件本身是生命周期组件 — 随 Server 启动/停止。

源码路径: `SimpleTcpCluster.java:66` — `extends LifecycleMBeanBase` (生命周期组件, 随容器级联 start/stop); L129 `Map<String, ClusterManager> managers` — **context 名 → 集群 Manager** 的关联表 (每个 Context 一个 DeltaManager)。L77-83 事件常量: BEFORE_MANAGERREGISTER_EVENT 等 — 集群生命周期事件。

关键设计: **生命周期组件 + 逐 Context 挂 Manager — 为什么?** 集群是 Server 级组件 (每个 Server 一个 SimpleTcpCluster), 但会话复制是 Context 级 (每个 Web 应用一个 Manager) — 中间用 managers Map 关联 — 应用可以独立开关集群复制 (Manager 元素配置)。LifecycleMBeanBase 让集群随容器生命周期自动启停 — 无需应用代码介入。

**对照 — 为什么生产集群默认用粘性会话而不是复制?** 复制不是免费的: 每次属性变化都要序列化+广播 — 高频写会话的应用复制成本极高; 粘性会话把流量钉在一个节点 — 零复制成本, 代价是节点宕机丢会话。**Tomcat 提供了选择权, 但最佳实践是粘性 + 会话存储外置 (Redis/DB)** — 复制的价值在于"无外部依赖的纯 Java 高可用"。

数据流: `server.xml <Cluster className="org.apache.catalina.ha.tcp.SimpleTcpCluster">` → Server 启动 → SimpleTcpCluster.start() → 创建 GroupChannel (T-10 §1) → 每个 Context 注册 → 替换 StandardManager 为 DeltaManager → 会话生命周期事件 → 复制协议 (本域 §1-2)。

→ 收束: Tomcat 十域收官 — T-1 容器生命周期 → T-2 连接器 → T-3 Pipeline → T-4 线程模型 → T-5 Mapper → T-6 类加载 → T-7 Spring Boot 集成 → T-8 HTTP 解析 → T-9 WebSocket → T-10 集群 — 从单请求处理到多节点高可用, 完整的 Tomcat 纵深。