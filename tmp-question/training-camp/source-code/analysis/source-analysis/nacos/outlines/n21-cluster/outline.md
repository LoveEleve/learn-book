# N-21 集群管理面 — 成员管理、Lookup 族与健康

> 前置: [[NC-5-一致性]] (集群成员消费) + [[N-14-命名集群]] | 对照: 1.x 单节点概念 → 3.x 成员模型
> 🔴 A | 方案 A (全深度) | 闭环: q1(成员管理) q2(Lookup 族) q3(变更事件)

**读者处境**: Nacos 集群的节点列表谁维护? 节点发现 (Lookup) 的三种方式? 成员变更怎么通知各组件?

### 1. 成员管理 — ServerMemberManager

场景: 集群节点怎么管理?
源码路径:
- **ServerMemberManager** (cluster/ServerMemberManager.java:92): implements NacosMemberManager — **ConcurrentSkipListMap serverList** (L110, 有序成员表) + **memberReportTs** (L137, 成员上报时间) + **UnhealthyMemberInfoReportTask** (L149, 不健康节点上报)
- 关键 API (注释 L76-86): **getSelf() 本地节点** (L169 自注册) / **getServerList() 集群字典** / getMemberAddressInfos() 健康成员地址 / **memberChange() 最终变更** / isUnHealth() 健康判定
- 自注册: serverList.put(self.getAddress(), self) (L169)
关键设计 (q1): **"成员表 = 集群事实源"** — ConcurrentSkipListMap 有序; getSelf 本地/其余远端; 不健康节点上报任务保成员健康。 [模式: 成员事实源]

### 2. Lookup 族 — 节点发现三方式

场景: 集群节点从哪发现?
源码路径:
- **MemberLookup** (lookup/MemberLookup.java:30): 接口 — **start()** (L37) + **afterLookup(Collection)** (L58, 发现后回调)
- **LookupFactory** (lookup/LookupFactory.java:35): **createLookUp** (L50-64) — **类型判定** (L107-130: FILE_CONFIG → FileConfigMemberLookup / ADDRESS_SERVER → AddressServerMemberLookup / 否则)
- **StandaloneMemberLookup** (lookup/StandaloneMemberLookup.java:30): 单机
- **FileConfigMemberLookup** (lookup/FileConfigMemberLookup.java:38): 配置文件
- **AddressServerMemberLookup** (lookup/AddressServerMemberLookup.java:45): 地址服务器 (1.x ServerListManager 对应)
- **AbstractMemberLookup** (lookup/AbstractMemberLookup.java:32): 基类
关键设计 (q2): **"Lookup 族 = 节点来源可插拔"** — 单机/文件/地址服务器三实现 + 工厂按配置判定; 与 N-04 ServerListProvider 的客户端对应。 [模式: Lookup 族]

### 3. 变更事件 — MembersChangeEvent 与监听器

场景: 成员变更怎么通知?
源码路径:
- **MembersChangeEvent** (cluster/MembersChangeEvent.java:36): 变更事件
- **MemberChangeListener** (cluster/MemberChangeListener.java:28): 监听器接口
- **MemberReportHandler** (remote/MemberReportHandler.java:45): 成员上报处理
- **ClusterRpcClientProxy** (remote/ClusterRpcClientProxy.java:60): 集群 gRPC 代理 (成员上报请求)
- 消费: NC-5 Distro memberManager (L46)
关键设计 (q3): **"变更事件 = 集群感知统一通道"** — 成员变化发事件, 各组件 (Distro/健康) 订阅; 上报经 gRPC 代理。 [模式: 变更事件]

### 4. 健康面 — ModuleHealthChecker 族

场景: 模块健康怎么检查?
源码路径:
- **AbstractModuleHealthChecker** (health/AbstractModuleHealthChecker.java:24): 模块健康检查基类
- **ModuleHealthCheckerHolder** (health/ModuleHealthCheckerHolder.java:29): 检查器持有
- **ReadinessResult** (health/ReadinessResult.java:24): 就绪结果
关键设计 (q4): **"模块健康 = 就绪语义"** — 各模块自检, 结果聚合 (N-14 ReadinessCheckService 消费)。 [模式: 模块自检]

### 5. 测试与行为锚

场景: 集群边界?
源码路径:
- 测试: ServerMemberManagerTest / LookupFactoryTest (core test)
- 日志锚: member 变更日志
关键设计 (q1): **"成员表注释 = API 文档"** (L76-86)。 [模式: 注释即文档]
