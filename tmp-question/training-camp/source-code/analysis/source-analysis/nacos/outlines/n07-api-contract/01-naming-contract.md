# N-07-01 API 契约面 — NamingService 接口族与数据模型 (Naming 契约篇)

> 前置: [[NC-1-NamingService]] (实现方) | 引出: N-07-02 (Config 契约) + N-07-03 (Remote 协议) | 对照: 接口是实现的"说明书"
> 🟡 B | 方案 B (重要域) | 闭环: q1(接口族) q2(pojo 模型) q3(监听器族)

**读者处境**: NacosNamingService 实现的那 20+ 方法, 接口本身怎么定义? Instance/ServiceInfo 的数据结构? 监听器事件族怎么分层?

### 1. 接口族 — NamingService 与 NamingMaintainService

场景: 命名服务对外契约?
源码路径:
- **NamingService** (api/naming/NamingService.java:36): 核心接口 — **registerInstance 6 重载** (L46-100) + batchRegisterInstance/batchDeregisterInstance (L122) + getAllInstances 多载 + selectInstances 多载 + **subscribe/unsubscribe 多载** (NamingSelector 变体)
- **NamingMaintainService** (NamingMaintainService.java:34): 维护面 — createService/updateService/查询健康检查配置等
- 工厂: NamingFactory (L), NamingMaintainFactory — 客户端创建入口
- NamingResponseCode/CommonParams: 常量面
关键设计 (q1): **"接口即契约 = 重载收敛的源头"** — NC-1 的"重载漏斗"正是接口 6 重载的实现端收敛; 接口定义默认值语义 (DEFAULT_GROUP)。 [模式: 接口契约]

### 2. 数据模型 — pojo 家族

场景: 实例/服务/列表的数据结构?
源码路径:
- **Instance** (api/naming/pojo/Instance.java:40): ip/port/weight/enabled/healthy/ephemeral/clusterName/metadata — **toInetAddr()** (NC-1 InstancesDiffer 的 key)
- **ServiceInfo** (pojo/ServiceInfo.java:36): hosts + lastRefTime + jsonFromServer — **缓存/差异计算的核心载体**
- **ListView** (pojo/ListView.java:26): 服务列表分页视图
- **Service/Cluster** (pojo/): 维护面模型
- pojo/maintainer (10): 维护面 DTO
- pojo/healthcheck (6): 健康检查配置模型
- **PreservedMetadataKeys** (PreservedMetadataKeys.java:25): 保留元数据键 (heart-beat 等)
关键设计 (q2): **"pojo = 传输与缓存共用"** — Instance/ServiceInfo 既是 gRPC 载荷又是客户端缓存与差异计算的对象; 字段即协议。 [模式: 双用模型]

### 3. 监听器族 — Event/EventListener 分层

场景: 事件怎么组织?
源码路径:
- **Event** (listener/Event.java:24) / **EventListener** (listener/EventListener.java:24): 基础接口
- **AbstractEventListener** (listener/AbstractEventListener.java:27): 泛型模板
- **NamingEvent** (listener/NamingEvent.java:28): 命名变更事件 (ALI-A5 NacosWatch 消费)
- **FuzzyWatch 族** (FuzzyWatchEventWatcher/FuzzyWatchChangeEvent/AbstractFuzzyWatchEventWatcher): 模糊订阅事件 (NC-2 fuzzyWatch 面)
- **NamingSelector** (selector/NamingSelector.java:26): 3.x 订阅选择器 (NC-1 subscribe 变体)
关键设计 (q3): **"事件分层 = 精确/模糊双通道"** — 精确订阅 (NamingEvent) 与模糊订阅 (FuzzyWatchChangeEvent) 各自事件类型; 选择器 (NamingSelector) 是 3.x 订阅增强。 [模式: 事件分层]

### 4. 测试与行为锚

场景: 契约的边界?
源码路径:
- 测试: api 模块 test (NamingService 接口测试)
- 常量锚: CommonParams/PreservedMetadataKeys — 契约常量集中
关键设计 (q1): **"常量面 = 契约的稳定面"** — 跨模块共享常量集中定义, 避免魔法值漂移。 [模式: 常量契约]
