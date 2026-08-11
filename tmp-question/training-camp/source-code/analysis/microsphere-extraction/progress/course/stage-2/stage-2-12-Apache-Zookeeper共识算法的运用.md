# stage-2 · 第 12 节：Apache Zookeeper 共识算法的运用 — 知识点提取

> 课程：stage-2 模式设计与实现 第 12 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/12. 第十二节：Apache Zookeeper 共识算法的运用.md`
> 提取时间：2026-08-11 | 权重：核心（ZooKeeper 共识运用主线，源码级）

---

## 一、本节概览

- **技术域**：ZooKeeper 共识运用（服务注册发现 Curator X Discovery + 分布式锁 Curator Recipes）
- **维度**：`[分布式问题]`（服务发现/分布式锁）+ `[工程问题]`（Curator API/策略，源码级）
- **核心命题**：理解 ZK 共识在实际应用——服务注册发现（ServiceDiscovery/Provider）与分布式锁（InterProcessMutex）
- **知识点数**：11 个
- **前置**：第 9 节 znode/节点类型、第 11 节共识、第 3 节 Raft 锁相关概念

## 前置条件清单
读者需先掌握：
1. **ZooKeeper 数据模型**（第 9 节：znode/临时/顺序/容器节点）
2. **ZooKeeper 共识**（第 11 节：Leader 选举/一致性）
3. **服务注册发现**（基本概念）
4. **分布式锁**（基本概念）
未达前置者，先补：第 9/11 节 + 服务发现/分布式锁基础

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：Curator 服务发现/锁直接对照源码讲
- **工程化弱**：Curator 框架/第三方库补基础
- **必做**：对照本地 `code/spring/curator` 源码验证（非只看 docs，08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 使用场景（服务注册发现 / 分布式锁 / 配置管理）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]`（ZK/Curator 当前主流） | **置信度**：High
- **前置**：无
- **来源**：docs §使用场景 + §作业
- **需求**：用 ZK 实现服务发现/分布式锁/配置管理
- **自主实现**：若我设计——基于 znode 临时/顺序节点特性实现
- **参考实现**（docs）：**服务注册与发现**——Apache Dubbo、Spring Cloud Zookeeper Service Discovery、Apache Curator X Discovery；**分布式锁**——利用 ZK 强一致性 + Curator Recipes；**配置管理**——利用 ZK 数据发布/订阅实时推送(作业)
- **对比取舍**：**ZK 作为协调底座**——服务发现/锁/配置都建立在 znode + 一致性之上
- **关联 microsphere**：microsphere 用 ZK 服务发现/锁 `[待验证]`

### KP-02 服务注册与发现客户端（节点同义词/基本模式）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：服务发现
- **来源**：docs §服务注册与发现客户端
- **需求**：理解服务发现的基本操作模式
- **自主实现**：若我设计——注册/注销/查询/更新/监听五类操作
- **参考实现**（docs）：分布式节点同义词——**Service Instance**(服务实例)/**Node**(节点)/**Peer**(对点)/**Endpoint**(端点)；基本模式——**服务注册**(注册对象=服务实例/名称/集群)、**服务注销**(实例)、**服务查询**(名称或实例列表)、**服务更新**(实例元数据/状态/心跳)、**服务变更监听**(基于事件，Plus)
- **对比取舍**：**五类操作**——注册/注销/查询/更新/监听是服务发现的完整生命周期
- **测试佐证**：源码 `curator-x-discovery`(ServiceDiscovery)

### KP-03 Curator X Discovery 核心 API（ServiceProvider/ProviderStrategy/InstanceProvider/ServiceInstance）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：服务发现、负载均衡
- **来源**：docs §Curator X Discovery 核心 API + 源码验证
- **需求**：用 Curator 抽象服务发现与实例选择
- **自主实现**：若我设计——ServiceProvider(门面)+ProviderStrategy(选择策略)+InstanceProvider(实例源)+ServiceInstance(模型)
- **参考实现**（docs + 源码）：
  - **ServiceProvider**：ProviderStrategy 与 InstanceProvider 的服务门面；`serviceProviderBuilder()` 构建；**缓存复用**(Curator 2.x 需缓存，NamespaceWatcher 无法移除，防内存耗尽)
  - **ProviderStrategy**：选择实例策略(类似负载均衡)——`RoundRobin`(轮询)/`Random`(随机)/`Sticky`(粘性)
  - **InstanceProvider**：提供服务实例列表 `getInstances()`
  - **ServiceInstance**：服务实例模型(name/id/address/port/sslPort/payload/registrationTimeUTC/serviceType/uriSpec/enabled)
- **对比取舍**：**门面 + 策略**——ServiceProvider 门面封装实例来源+选择策略；三种 ProviderStrategy 对应负载均衡策略
- **测试佐证**：源码 `curator-x-discovery`(ServiceProvider/ProviderStrategy/InstanceProvider/ServiceInstance)+`strategies/RoundRobinStrategy`

### KP-04 ProviderStrategy 选择策略（RoundRobin/Random/Sticky）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03、负载均衡
- **来源**：docs §ProviderStrategy + 源码验证
- **需求**：从实例列表选一个（负载均衡）
- **自主实现**：若我设计——轮询/随机/粘性三种策略
- **参考实现**（docs + 源码）：`ProviderStrategy.getInstance(InstanceProvider)` 返回一个实例；三种实现——
  - **RoundRobin**(轮询)：`RoundRobinStrategy`，`AtomicInteger index` 递增轮询(源码 32 行)
  - **Random**(随机)：`RandomStrategy`，**优化用 ThreadLocalRandom 替换 Random**(docs 优化点)
  - **Sticky**(粘性)：`StickyStrategy`，始终选同一个
- **对比取舍**：**三策略对应负载均衡**——轮询/随机/粘性，ThreadLocalRandom 优化并发
- **测试佐证**：源码 `strategies/RoundRobinStrategy.java`(AtomicInteger index 32)/`RandomStrategy`/`StickyStrategy`

### KP-05 ServiceDiscovery 接口（门面）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03
- **来源**：docs §ServiceDiscovery 接口 + 源码验证
- **需求**：定义服务注册/注销/更新/查询门面
- **自主实现**：若我设计——registerService/updateService/unregisterService/queryForInstances
- **参考实现**（docs + 源码）：`ServiceDiscovery<T> extends Closeable` 门面接口——
  - `registerService`(注册)、`updateService`(更新)、`unregisterService`(注销)
  - `serviceCacheBuilder`(缓存构建)、`queryForNames`(查询所有服务名)、`queryForInstances`(查询实例)、`queryForInstance`(按 id 查实例)
- **对比取舍**：**门面统一操作**——注册/更新/注销/查询集中在一个接口
- **测试佐证**：源码 `curator-x-discovery/.../ServiceDiscovery.java`

### KP-06 ServiceDiscoveryImpl（实现类 + 服务注册）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-05、第 9 节节点类型
- **来源**：docs §ServiceDiscoveryImpl + 源码验证
- **需求**：实现服务注册（映射到 znode）
- **自主实现**：若我设计——序列化 ServiceInstance → 按 serviceType 选 CreateMode 建 znode
- **参考实现**（docs + 源码）：`ServiceDiscoveryImpl` 依赖——`CuratorFramework`(client)/`basePath`(基准路径)/`serializer`/`thisInstance`/`payloadClass`/`watchInstances`；`internalRegisterService`(206 行)——序列化 → 按 serviceType 选模式：
  - `DYNAMIC` → `CreateMode.EPHEMERAL`(217 行)
  - `DYNAMIC_SEQUENTIAL` → `EPHEMERAL_SEQUENTIAL`(220 行)
  - 默认 → `PERSISTENT`(223 行)
  - `creatingParentContainersIfNeeded()` + NodeExists 时 delete 重建(触发 watcher)
- **对比取舍**：**serviceType 映射节点类型**——动态实例用临时节点(会话失效自动下线)，实现故障检测
- **测试佐证**：源码 `curator-x-discovery/.../ServiceDiscoveryImpl.java`(internalRegisterService 206/EPHEMERAL 217/EPHEMERAL_SEQUENTIAL 220/PERSISTENT 223)

### KP-07 InstanceSerializer（JsonInstanceSerializer）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：序列化
- **来源**：docs §InstanceSerializer + 源码验证
- **需求**：序列化/反序列化 ServiceInstance
- **自主实现**：若我设计——JSON 序列化器
- **参考实现**（docs + 源码）：`InstanceSerializer` 接口，内建 `JsonInstanceSerializer`(默认)——基于 **Jackson** 序列化/反序列化 ServiceInstance
- **对比取舍**：**JSON 序列化**——Jackson 实现，简单通用
- **测试佐证**：源码 `curator-x-discovery/.../JsonInstanceSerializer.java`

### KP-08 服务注册与发现服务端（Curator X Discovery Server）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02、服务端
- **来源**：docs §服务注册与发现服务端
- **需求**：提供服务发现服务端（可 GUI）
- **自主实现**：若我设计——基于 Curator X Discovery Server 提供服务端能力
- **参考实现**（docs）：服务端基于 **Apache Curator X Discovery Server**；延伸——**Spring Cloud Zookeeper Service Discovery Server** 可基于 Curator X Discovery Server 实现，进一步实现 GUI
- **对比取舍**：**客户端/服务端对称**——客户端(ServiceDiscovery 注册/查询) + 服务端(Server 承载，可 GUI)
- **测试佐证**：Curator X Discovery Server 生态

### KP-09 分布式锁（InterProcessMutex / LockInternals）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：分布式锁、第 9 节临时/顺序节点
- **来源**：docs §分布式锁 + 源码验证
- **需求**：用 ZK 实现强一致分布式锁
- **自主实现**：若我设计——临时有序节点 + 最小序号持锁 + 前驱监听
- **参考实现**（docs + 源码）：`InterProcessLock`(接口)/`InterProcessMutex`(重入实现)/`LockInternals`(锁内部)；`LockInternals.internalLockLoop`(223 行)——
  - `createsTheLock` 创建**临时有序节点**(EPHEMERAL_SEQUENTIAL)
  - `driver.getsTheLock` 判断是否最小编号(获得锁)
  - 未获锁则 `getData().usingWatcher(watcher)` 监听前驱节点，`wait()` 等待
  - 前驱删除(锁释放)唤醒；超时 `doDelete` 删除自己节点
- **对比取舍**：**最小序号 + 前驱监听**——按序号排序，监听前驱实现公平锁；临时节点会话失效自动释放
- **测试佐证**：源码 `curator-recipes/.../locks/LockInternals.java`(internalLockLoop 223/getsTheLock 234/usingWatcher 244)

### KP-10 锁路径与获取/释放条件
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-08
- **来源**：docs §锁路径/条件 + 源码验证
- **需求**：理解锁节点路径与获取/释放逻辑
- **自主实现**：若我设计——/locks-base/服务名/临时有序节点
- **参考实现**（docs）：锁路径 `/locks-base-path/lock-biz-1/临时有序节点`；**获取锁条件**——按最小节点序号获取；**释放锁条件**——Lock 操作结束删除节点，或 Client 超时临时节点删除；例：children=["1","2","3"]，ourIndex=0<maxLeases=1 → 持锁；ourIndex=1 → 监听前驱"1"等待
- **对比取舍**：**公平锁**——按序号排队，监听前驱，实现公平互斥
- **测试佐证**：docs 序号示例 + 源码 LockInternals

### KP-11 锁优化点 + 第三方整合
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-08
- **来源**：docs §优化点 + §第三方整合
- **需求**：理解锁实现优化与 Curator 生态
- **自主实现**：若我设计——监听器加状态/类型/路径判断，避免无效释放
- **参考实现**（docs + 源码）：`LockInternals.watcher` **优化建议**——增加 ZK 状态、节点类型和路径判断(瑕疵是所有状态都释放，`client.postSafeNotify` 通知)；Curator 为 Netflix 开源框架(curator.apache.org)；Spring Cloud ZK Service Discovery 基于 Curator X Discovery
- **对比取舍**：**watcher 优化**——减少无效唤醒；Curator 是 ZK 客户端事实标准
- **测试佐证**：源码 `LockInternals.java`(watcher)

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 使用场景 | 分布式问题 | 核心 | P1 | 🟡 | 有效 | High |
| 服务发现客户端(基本模式) | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| Curator X Discovery 核心 API | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| ProviderStrategy 策略 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| ServiceDiscovery 接口 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| ServiceDiscoveryImpl | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| InstanceSerializer | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |
| 服务端(Curator X Discovery Server) | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |
| 分布式锁(InterProcessMutex) | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 锁路径/获取释放条件 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 锁优化 + 第三方整合 | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/curator`（curator-x-discovery/curator-recipes）+ `code/spring/zookeeper`（第 9/11 节 CreateMode/znode）
- **关键源码类**（本次实证）：`curator-x-discovery`(ServiceDiscoveryImpl.internalRegisterService 206/EPHEMERAL 217/EPHEMERAL_SEQUENTIAL 220/PERSISTENT 223、ServiceProvider/ProviderStrategy/ServiceInstance、strategies/RoundRobinStrategy AtomicInteger 32)、`curator-recipes/.../locks`(InterProcessMutex/LockInternals.internalLockLoop 223/getsTheLock 234/usingWatcher 244)
- **关联标注**：microsphere 用 ZK/Curator 服务发现/锁 `[待验证]`；衔接第 9 节节点类型、第 11 节共识

---

## 五、本节小结（三层次视角）

**需求**：用 ZK 共识实现服务注册发现与分布式锁。

**自主实现核心**：若我设计——
1. 服务发现：ServiceDiscovery(门面) + ServiceProvider(选择) + ProviderStrategy(策略) + ServiceInstance(模型)
2. 服务注册映射 znode：DYNAMIC→临时节点(会话失效自动下线)
3. 分布式锁：临时有序节点 + 最小序号持锁 + 前驱监听
4. 锁释放：删除节点/会话超时；watcher 优化

**参考实现**：Curator 源码（`code/spring/curator` 完整验证）+ docs。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**ZooKeeper 共识的运用（服务发现 + 分布式锁）**"。核心洞察：**服务发现(Curator X Discovery 门面+策略)、分布式锁(临时有序节点+前驱监听)**。为事务组(第 13-20 节)铺垫。

**待验证汇总**：
- microsphere 用 ZK/Curator 的具体场景
- Curator ServiceCache 细节

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 Curator 运用 + 源码片段 + 本地源码验证；补全聚焦"ZK 共识运用的工程价值"。

### 完整认知：ZK 共识运用在真实架构中完整该讲什么

docs 覆盖了服务发现 + 分布式锁。作为架构师，这个主题完整还该包含：

1. **临时节点 = 服务发现的故障检测**：服务实例用 EPHEMERAL 节点注册，会话失效自动下线——无需人工清理，是服务发现高可用的基础
2. **ServiceProvider 缓存的重要性**：Curator 2.x 必须缓存 ServiceProvider(否则 NamespaceWatcher 内存泄漏)——生产踩坑点
3. **ProviderStrategy = 负载均衡**：轮询/随机/粘性选择实例——服务发现与负载均衡的衔接
4. **分布式锁 = 临时有序节点 + 前驱监听**：最小序号持锁保证公平；临时节点会话超时自动释放防死锁——这是 ZK 锁相比 Redis 锁的强一致优势
5. **ZK 锁 vs Redis 锁**：ZK 强一致(无主从切换丢锁)、Redis 快但可能丢锁——按一致性需求选
6. **服务发现生态对比**：Eureka(AP)/ZK-Curator(CP)/Nacos(AP/CP)/Consul(AP)——CAP 定位选型
7. **配置管理的发布订阅**：ZK 数据变更 + watch 实现实时配置推送——ZK 三大运用(发现/锁/配置)的第三块

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| ZK(CP) vs Eureka(AP) | 强一致 vs 高可用——服务发现按需选 |
| ZK 锁 vs Redis 锁 | ZK 强一致防丢锁；Redis 快但可能丢 |
| 临时 vs 持久节点 | 临时会话失效自动清理；持久需手动 |
| RoundRobin vs Random/Sticky | 轮询均衡；随机分散；粘性亲和 |
| 公平锁 vs 非公平 | 前驱监听公平；简单非公平快 |

### 常见坑/反模式

1. **ServiceProvider 不缓存**：每次新建 → 内存泄漏(Curator 2.x)
2. **用持久节点注册服务**：服务下线不清理——用临时节点
3. **锁不处理会话超时**：临时节点自动释放是保障——勿依赖手动解锁
4. **watcher 无条件释放**：锁 watcher 所有状态都释放——应加状态/类型/路径判断
5. **忽略 CAP 选型**：强一致需求用 AP 服务发现——丢数据

### 生态位置

- **分布式问题维度**：ZK 共识运用是 **ZooKeeper 应用的收尾**——承接第 9/11 节数据模型/共识，为事务组(第 13-20 节)铺垫
- **衔接**：数据模型(第 9 节) → 共识(第 11 节) → 运用(本篇，服务发现/锁) → 事务组(第 13 节起)
- **与源码提取的关系**：curator-x-discovery/curator-recipes 是核心源码

**架构师视角结论**：本篇不只是背 Curator API，而是"**理解 ZK 共识如何落地为服务发现与分布式锁**"——临时节点故障检测、ProviderStrategy 负载均衡、临时有序节点+前驱监听公平锁；这是 ZK 作为协调服务在生产的核心应用，也是 stage-2 分布式问题维度的基石。
