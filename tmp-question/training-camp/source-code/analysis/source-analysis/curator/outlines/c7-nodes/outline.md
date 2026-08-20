# C-7 持久节点与组成员 — 让临时节点跨会话活下来

> 前置: [[C-1-CuratorFramework]] (guaranteed 删除) + [[C-5-缓存与监听]] (CuratorCache) + [[Z-5-Session]] (临时节点语义) | 引出: [[C-8-服务发现]] (实例注册的本质) | 对照: ZK 官方 PersistentNode 无此配方
> 🟡 B | 3 KP | [模式: 三触发重建 + TTL 心跳]
> Pass 2 闭环: q1(重建) q2(TTL) q3(组成员)

**读者处境**: 注册中心的临时节点, 会话一断就没了。怎么让节点在进程活着期间"永远在"? 断开又重连, 节点什么时候回来?

### 1. PersistentNode — 三重触发的跨会话重建

场景: 服务进程活着, 节点就必须在注册中心存在
源码路径:
- **三重触发** (PersistentNode.java:86-113, 130-137):
  ① **watcher**: checkExists 挂 watch (PersistentNode.java:470-482) — NodeDeleted → createNode (L86-87); NodeDataChanged → 重挂 (L88-90)
  ② **连接状态**: RECONNECTED + active → createNode (L130-137)
  ③ **checkExists 回调**: NONODE → createNode (L95-113)
- **createNode** (PersistentNode.java:397-445): 已有路径复用 / sequential 剥后缀 / *SEQUENTIAL 降级; 后台创建带回调 (L208-217)
- 回调分派 (PersistentNode.java:239-273): NODEEXISTS 他人抢先 → setData 同步 (L264-265); NOAUTH → authFailure; NONODE → parentCreationFailure
- **initialCreateLatch + waitForInitialCreate** (PersistentNode.java:302-307): 调用方拿到"已注册"确定信号
- **close 防孤儿** (PersistentNode.java:313-336): close 后迟到回调 guaranteed 删残留 (L222-237)
关键设计 (q1): **三种触发覆盖三种"节点消失"原因** — 被别人删 (watcher) / 自己断线重建 (连接事件) / 竞态兜底 (回调 NONODE); "close 后防孤儿"解决"关闭和后台创建赛跑"的经典竞态。 [模式: 事件驱动重建]

### 2. PersistentTtlNode — CONTAINER 父 + TTL 子心跳

场景: 进程崩溃后, "临时"语义怎么用 TTL 表达?
源码路径:
- 父节点 CONTAINER + deleteNode 覆写 NOP (PersistentTtlNode.java:162-167); **touchTask 每 ttlMs/2 setData 子节点** (L178-199, scheduleAtFixedRate L201); NoNode → create().orSetData().withTtl (L183-190)
- 崩溃 → 停止 touch → 子节点 TTL 到期被 ZK 删除 → CONTAINER 父因无子回收 (类注释 L38-57)
- 关键: keep-alive 只写子节点, **父节点零 watch 事件** (类注释 L51-53 "does not generate watch triggers on the parent node")
关键设计 (q2): **父子分层隔离心跳与数据** — 直接给父节点设 TTL 需周期 setData 且产生 watch 噪音; 子节点 touch 让"存活心跳"与"被观察的数据节点"解耦。 [模式: 心跳节拍]

### 3. GroupMember — 缓存之上的成员视图

场景: 集群成员列表, 谁在线一目了然
源码路径:
- 成员节点 = PersistentNode(EPHEMERAL) (GroupMember.java:68-71); 视图 = **CuratorCache.bridgeBuilder** (GroupMember.java:68) — 跨配方复用 (C-5)
- **getCurrentMembers** (GroupMember.java:116-132): 缓存过滤直接子节点; **自己强制入列** (L128-130 — 刚创建/重建期间缓存可能没有自己)
关键设计 (q3): **"自己永远在成员列表"** 是缓存一致性与事实的调和 — EPHEMERAL 节点存在但缓存事件未达时, 不丢失自己。 [模式: 视图调和]

## 代码类型
Architecture (分布式原语)

## 负面空间 — Curator 持久节点刻意不做的事

- **不做数据版本 CAS**: setData 盲写覆盖
- **不做分区期间的保真**: 网络分区时节点可能短暂缺失, 无本地代理 (对照 Eureka 本地缓存)
- **不做多实例协调**: 同名节点靠 NODEEXISTS 抢占, 无选主
- **不做心跳频率自适应**: TTL 心跳固定节拍, 无动态调整

→ 引出: 服务发现配方怎么组合注册+发现+缓存? → C-8 服务发现
