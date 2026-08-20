# T-10 集群通信与会话复制 — 知识规划 (09 审计新增域)

> 项目: Tomcat 10.1.x | 类型: Tomcat 自身设计 (集群扩展) | 🟡B 域 / 2 篇大纲
> 核心文件: GroupChannel 734 + McastServiceImpl 734 + AbstractReplicatedMap 1728 + DeltaManager 1358 + SimpleTcpCluster 762 | ~5300 行 (tribes 112 + ha 38 = 150 文件 31480 行)
> 基线: T-1 容器 — 集群是 StandardServer 之上的扩展; T-9 WebSocket — 长连接不可复制的对照
> 09 审计: tribes 112 文件 ≥50 定量预检 → 设计决策承载 (组播成员管理/心跳/消息复制) + ha 38 文件 → 新增域

---

## §0.8 域审核前置

### 1. 域过载检查
- 核心类: 5 个 (GroupChannel/McastServiceImpl/AbstractReplicatedMap/DeltaManager/SimpleTcpCluster)
- 5 ≤ 10 → 不触发拆分阈值 ✅

### 2. 淘汰清单
- tribes/group/interceptors/* (Interceptors 链: TcpFailureDetector/FragmentationInterceptor 等) → 归 §1 概述 (拦截器链是 Channel 的核心扩展面)
- tribes/transport/* (ReplicationTransmitter/ReceiverBase) → §1 概述 (收发传输)
- tribes/util/* (XByteBuffer 621) → 概述 (字节缓冲)
- ha/deploy (FarmWarDeployer 728) → 排除 (集群部署, 边缘)
- ha/ClusterManagerBase → 归 §2 (DeltaManager 基类)

### 3. 规范缺口
T-10 无 Servlet 规范对应 — Tomcat 自身集群扩展 (全集群/Delta 会话复制两种模式)

### 4. 禁止过度加域
- 会话存储 (StandardManager) 属 T-5 淘汰面 — 本域只讲复制的通信机制, 不展开会话存储细节 ✅
- 负载均衡器 (外部组件) → 对照提及

### 项目类型判定
| 维度 | 值 |
|---|---|
| 类型 | 集群扩展 (自身设计) |
| 面试频度 | 中 (会话复制/粘性会话/广播) |
| 生产 | 中 (高可用部署) |
| Hub | 低 (独立栈) |
| 方案 | 🟡 B |

---

## §一 逐源提取 (High 置信度, 源码实证)

| Source | Inferred Knowledge Point | Confidence |
|---|---|---|
| GroupChannel.java:67 | 实现 ManagedChannel + JmxChannel — 集群通信主通道, ChannelInterceptorBase 子类 (拦截器链起点) | High |
| GroupChannel.java:159-183 | addInterceptor() 链式添加 + heartbeat() — 心跳入口 (遍历拦截器链) | High |
| GroupChannel.java:264-367 | messageReceived()/memberAdded()/memberDisappeared() — 消息/成员事件分派 | High |
| GroupChannel.java:513-543 | getChannelReceiver/Sender/MembershipService + setter — 三大组件 (接收/发送/成员) 可插拔 | High |
| McastServiceImpl.java:59-85 | doRunSender/doRunReceiver + sendFrequency — 组播心跳双线程 (发送/接收) | High |
| McastServiceImpl.java:151-176 | sendFrequency (ping 间隔) / expireTime (成员过期) — 两个关键参数 | High |
| McastServiceImpl.java:222 | mcastSoTimeout = sendFrequency — 组播接收超时与发送频率同步 | High |
| McastServiceImpl.java:268-284 | SenderThread(sendFrequency) + memberwait = sendFrequency*2 — 发送线程 + 成员等待 2 周期 | High |
| DeltaManager.java:92-105 | 计数器族: EVT_SESSION_CREATED/DELTA/ACCESSED/EXPIRED — 会话事件类型枚举 | High |
| DeltaManager.java:55 | extends ClusterManagerBase — 集群会话管理器基类 | High |
| AbstractReplicatedMap.java:441-516 | replicate(key, complete) — isDirty()||complete||isAccessReplicate() 三条件决定复制 | High |
| AbstractReplicatedMap.java:120-212 | mapOwner — 主/备归属判定 | High |
| SimpleTcpCluster.java:66-129 | extends LifecycleMBeanBase + managers Map (context→ClusterManager) — 集群生命周期组件 | High |

---

## §二 深度分类 (🔴🟡🟢)

| 知识点 | 级别 | 理由 |
|:--|:--:|:--|
| 组播成员管理 (McastServiceImpl) | 🔴 | 集群核心 — 成员发现/心跳/过期, 双线程模型 |
| 拦截器链 (ChannelInterceptor) | 🔴 | Channel 的核心架构 — 所有横切 (保活/分片/流量控制) 都是拦截器 |
| 会话复制触发条件 (isDirty 三条件) | 🟡 | 复制性能核心 — 全量 vs 脏复制 |
| DeltaManager 事件协议 | 🟡 | 会话事件类型 + 计数器统计面 |
| 主/备归属 (mapOwner) | 🟡 | 复制拓扑判定 |
| 心跳参数 (sendFrequency/expireTime) | 🟡 | 集群稳定性配置 |

---

## §三 聚类 (机制边界 + 依赖图 + 教学顺序)

### 机制边界
1. 通信层 (tribes) — 成员发现/消息收发/拦截器链
2. 复制层 (tipis) — AbstractReplicatedMap 主备复制
3. 会话集成层 (ha) — DeltaManager 把复制挂到 StandardContext 会话

### 依赖图
```
McastServiceImpl (组播成员) ─┐
ChannelReceiver/Sender (收发) ├→ GroupChannel (Channel 门面) ← SimpleTcpCluster (Catalina 集成)
拦截器链 (心跳/分片) ─────────┘                              ↓
                                              AbstractReplicatedMap (tipis 复制) → DeltaManager (会话)
```

### 教学顺序 (2 篇)
1. **T-10 §1 通信层 (tribes)**: GroupChannel 门面 + 三组件 + 组播成员管理 (心跳/过期) + 拦截器链概述
2. **T-10 §2 会话复制 (tipis + ha)**: AbstractReplicatedMap 复制条件 → DeltaManager 事件协议 → SimpleTcpCluster 集成 → 粘性会话对照

---

## §四 大纲规划 (2 篇)

### T-10 §1: 通信层 — GroupChannel 与组播成员管理 (🟡B, 3 KP)
- 场景: 3 节点集群 — 新节点加入怎么被发现? 节点挂了谁先知道?
- 机制 1: GroupChannel 门面 (三组件可插拔) — 为什么组合? 发送/接收/成员分离
- 机制 2: 组播心跳 (McastServiceImpl 双线程) — 为什么组播? 成员发现 O(1) 广播
- 机制 3: 拦截器链 (heartbeat 遍历) — 为什么拦截器? 横切面可插拔
- 引出: 节点间能通信了 — 会话数据怎么复制?

### T-10 §2: 会话复制 — AbstractReplicatedMap + DeltaManager (🟡B, 2 KP)
- 场景: 用户 Session 在节点 A — 请求转到节点 B — B 没有这个 Session — 复制策略怎么选?
- 机制 1: 复制触发 (isDirty/complete/isAccessReplicate 三条件) — 为什么脏复制? 全量太贵
- 机制 2: DeltaManager 事件协议 (EVT_SESSION_* 族) — 为什么事件化? 增量传输
- 机制 3: SimpleTcpCluster 集成 (managers Map) + 粘性会话对照 — 为什么生产默认粘性? 复制成本
- 引出: 集群讲完 — 会话复制 vs WebSocket 长连接 (T-9) 的根本差异收束

---

## §五 跨域引用

| 域 | 关系 |
|:--|:--|
| T-1 容器 | SimpleTcpCluster 是 LifecycleMBeanBase, 生命周期随容器级联 |
| T-9 WebSocket | 长连接不可复制 → 粘性会话的对照 |
| T-5 (淘汰的 Session) | 会话存储的复制面在本域, 存储面不在 |
| ALI/Nacos | Nacos 的服务发现 (NC-2) vs 组播成员管理: 中心化 vs 去中心化对照 |

---

## §六 20 问 (A 5 / B 6 / C 5 / D 4)

### A. 机制理解 (5)
1. GroupChannel 为什么组合三大件 (Receiver/Sender/Membership) 而不是单类?
2. 组播成员发现的优势? 为什么不适合大集群?
3. 拦截器链能加什么? (心跳/分片/流量控制)
4. 会话复制为什么用事件而不是全量?
5. 主/备归属 (mapOwner) 决定什么?

### B. 源码实证 (6)
6. GroupChannel 实现的接口? (grep L67)
7. heartbeat() 做了什么? (grep L183)
8. McastServiceImpl 双线程字段? (grep L59-61)
9. sendFrequency/expireTime 语义? (grep L151-176)
10. replicate 的三条件? (grep AbstractReplicatedMap L441-453)
11. DeltaManager 事件类型有哪些? (grep EVT_)

### C. 推理深挖 (5)
12. 组播 UDP 丢包怎么办? 心跳的容错?
13. expireTime = 2×sendFrequency 的含义? 为什么 2 倍?
14. 脏复制 (isDirty) 的时序问题? 并发写怎么保证复制完整?
15. 会话复制失败 (节点宕机) 的恢复? 全量拉取?
16. 大集群为什么不用全组播? (广播风暴)

### D. 跨域扩展 (4)
17. 本域 vs Nacos 服务发现: 中心化 vs 去中心化?
18. 本域 vs T-9 WebSocket: 长连接为什么不能复制?
19. 本域 vs T-1 Lifecycle: 集群组件的生命周期怎么级联?
20. 本域 vs openjdk: 心跳机制 vs JVM Safepoint 同步的时序对照?

---

## §七 淘汰与排除

| 项 | 理由 |
|:--|:--|
| ha/deploy (FarmWarDeployer) | 集群部署边缘功能 |
| tribes/util (XByteBuffer) | 字节缓冲工具, 低承载 |
| 会话存储本身 (StandardManager) | T-5 淘汰面, 本域只讲复制通信 |