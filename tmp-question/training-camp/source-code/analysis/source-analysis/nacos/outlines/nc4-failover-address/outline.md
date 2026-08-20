# NC-4 本地缓存+故障转移+地址管理 — 客户端的三层兜底

> 前置: [[NC-1-Naming]] (ServiceInfoHolder 消费) + [[NC-2-Config]] (LocalConfigInfoProcessor 消费) | 引出: [[NC-5-一致性]] (服务端面) | 对照: 1.x ServerListManager 重构
> 🟡 B | 方案 B (重要域) | 闭环: q1(故障转移开关) q2(ServerListProvider 族) q3(磁盘缓存)

**读者处境**: 服务端挂了, 客户端怎么还"知道"服务列表? "failover 开关"谁在刷新? 服务器地址列表 (serverList) 怎么从配置/Endpoint 来?

### 1. 故障转移 — FailoverReactor 的 5 秒开关刷新

场景: failover 数据怎么进入/退出生效?
源码路径:
- **FailoverReactor** (naming/backups/FailoverReactor.java:52): serviceMap (L54) + failoverSwitchEnable (L56) + **SPI 加载 FailoverDataSource** (L72-77, 取第一个)
- **init 5 秒定时** (L87-89): `scheduleWithFixedDelay(FailoverSwitchRefresher, 0, 5000ms)`
- **FailoverSwitchRefresher.run 三态** (L91-153): ① switch null → 关闭 (L97-99) ② **enabled** → 加载 failoverData + **差异计算发布 InstancesChangeEvent** (L110-116) + 替换 serviceMap (L120-123) ③ **从开转关** → 对比主缓存发布差异 (L129-143) + clear (L145-147)
- **isFailoverSwitch(serviceName)** (L159-161): 开关 && serviceMap 含 && ipCount>0 — NC-1 发现三路的第一路数据源
- getService 兜底空 ServiceInfo (L163-172)
- Micrometer 指标: nacos_naming_client_failover_instances (L187-207)
关键设计 (q1): **"开关 + 5 秒轮询 = 运维可控的强制覆盖"** — failover 不是自动的, 是 FailoverDataSource (SPI) 提供开关与数据; 切换时发差异事件让监听器感知 (与正常推送同通道)。 [模式: 运维开关]

### 2. FailoverDataSource SPI — 数据源插槽

场景: failover 数据从哪来? 可扩展吗?
源码路径:
- **FailoverDataSource** (backups/FailoverDataSource.java:26 行): 接口 — getSwitch() + getFailoverData()
- **FailoverData** (L59): getData() 数据载体 + **NamingFailoverData** (L35): 命名服务专属
- **FailoverSwitch** (L38): enabled 开关
- 消费方: FailoverReactor SPI load (L72)
关键设计 (q2): **"SPI 插槽 = 用户可注入 failover 源"** — 默认无实现 (规划未提), 用户按需 SPI 实现; 与 NC-2 的 ConfigChangeParser SPI 同构。 [模式: SPI 插槽]

### 3. ServerListProvider 族 — 服务器地址的三实现

场景: 客户端怎么知道连哪个 Nacos 服务器?
源码路径:
- **AbstractServerListManager** (client-basic/address/AbstractServerListManager.java:41): 门面 — **start: SPI load ServerListProvider + order 降序排序 + match 首个命中** (L50-58) — 替代 1.x ServerListManager
- **EndpointServerListProvider** (EndpointServerListProvider.java:53): **30s 定时刷新** (L63 + L163-172: refreshServerListIfNeed) — Endpoint 地址解析
- **PropertiesListProvider** (L97 行): properties 配置静态列表
- **ServerListProvider** (L101): 接口 — getOrder/match/getServerList
- **ServerListChangeEvent** (L29): 变更事件 — NC-1 NamingGrpcClientProxy.onEvent 消费
关键设计 (q3): **"Provider 族 = 排序匹配的地址源"** — 按 order 降序逐个 match, 首个命中生效; Endpoint 定时刷新, Properties 静态; 变更发事件让连接层切换 (NC-3 onServerListChange)。 [模式: 提供者族]

### 4. 磁盘缓存 — DiskCache 的读写

场景: 实例缓存怎么落盘/读回?
源码路径:
- **DiskCache.write** (naming/cache/DiskCache.java:54-76): ServiceInfo → 文件 (ConcurrentDiskUtil.writeFileContent L73)
- **read** (L90-105): 目录文件解析 → Map<String, ServiceInfo>
- **parseServiceInfoFromCache** (L119-148): 逐行 JSON 解析
- 消费方: ServiceInfoHolder 差异事件后写 (NC-1 L161) + 启动加载 (NC-1 L66)
关键设计 (q4): **"磁盘 = 进程重启的缓存延续"** — 内存缓存 + 差异事件 + 磁盘落盘 + 启动加载四件套; 与配置面 snapshot (LocalConfigInfoProcessor) 平行。 [模式: 磁盘缓存]

### 5. 配置面容灾 — LocalConfigInfoProcessor 的 failover/snapshot

场景: 配置的本地容灾文件结构?
源码路径:
- **LocalConfigInfoProcessor** (config/impl/LocalConfigInfoProcessor.java:41 行): **getFailover** (L68) + **getSnapshot** (L85) + 文件路径结构 (L51-55: data/config-data/config-data-tenant 三级)
- 消费方: NC-2 getConfigInner 三路 (failover/server/snapshot)
关键设计 (q5): **"配置容灾 = 文件三路径"** — failover (用户维护) 与 snapshot (自动) 分目录; 三路语义在 NC-2 已深挖。 [模式: 文件容灾]

### 6. 测试与行为锚

场景: 容灾的边界行为?
源码路径:
- 测试: FailoverReactorTest / ServerListManagerTest (client/src/test)
- 日志锚: "FailoverDataSource type is {}" (L75) / "failover switch changed" (L102)
- 指标锚: nacos_naming_client_failover_instances (L193)
关键设计 (q1): **"指标锚 = 容灾可观测"** — failover 生效的实例数有 Micrometer 指标, 运维可监控。 [模式: 可观测]
