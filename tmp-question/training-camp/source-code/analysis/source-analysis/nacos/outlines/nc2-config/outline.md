# NC-2 ConfigService 配置客户端 — 三路读取、CAS 发布与监听通知链

> 前置: [[ALI-A1-NacosPropertySourceLocator]] (getConfig 消费方) + [[ALI-A2-配置刷新]] (addListener 消费方) | 引出: [[NC-3-gRPC-Redo]] (通信底座) + [[NC-4-容灾缓存]] (snapshot 面) | 对照: SCC-8 RefreshEvent 链
> 🔴 A | 方案 A (全深度) | 闭环: q1(三路读取) q2(监听链) q3(CAS 发布) q4(变更解析)

**读者处境**: ALI-A1 的 locator 调 `configService.getConfig(dataId, group, timeout)` — 这一行背后的三路容灾、加密过滤链、监听通知链怎么工作?配置变更时 listener.receiveConfigInfo 之外, AbstractConfigChangeListener 的 ConfigChangeEvent 哪来的?

### 1. 三路读取 — getConfigInner 的 failover → server → snapshot

场景: getConfig 的返回内容从哪来? 优先级?
源码路径:
- **getConfigInner** (config/NacosConfigService.java:206-261): ① **failover 优先** (L215-231): LocalConfigInfoProcessor.getFailover — 注释明言 "not created by client program automatically, but is maintained by user" (L216-219, 应急重启/服务端宕机场景) + getEncryptDataKeyFailover (L225) ② **server 主路** (L233-240): worker.getServerConfig + NO_RIGHT 异常原样抛 (L242-244) ③ **snapshot 兜底** (L249-261): getSnapshot + getEncryptDataKeySnapshot
- 每路都过 **configFilterChainManager.doFilter** (L228/237/258 — 加密/解密链)
- **publishConfigInner** (L273-290): ParamUtils.checkParam + ConfigRequest + **doFilter 出站** (L284, 加密) + worker.publishConfig (casMd5 可空 — 普通/CAS 双模式)
- **getServerStatus** (L293-299): worker.isHealthServer → UP/DOWN
关键设计 (q1): **"三路容灾 = 用户维护 > 服务端 > 客户端快照"** — failover 文件是"用户手动维护的强制覆盖" (应急场景), 服务端是主路, 快照是自动兜底; 每路都过加密过滤链保证内容一致。 [模式: 三路容灾]

### 2. 监听注册 — addListener → addTenantListeners → CacheData 双检

场景: addListener 的监听器挂在哪?
源码路径:
- **addListener** (NacosConfigService.java:121-123): → **worker.addTenantListeners** (ClientWorker.java:195-210)
- **addCacheDataIfAbsent 双检** (ClientWorker.java:381-402): getCache → 无则 new CacheData + 双检 (L391-394) → putCache (L481: AtomicReference copy-on-write)
- 带内容注册 **addTenantListenersWithContent** (L225-242): getConfigAndSignListener 用 — 先查后注册 (原子语义)
- **cacheMap AtomicReference<Map>** (L134) — 无锁读 + COW 写
- notifyListenConfig (L210): 通知 agent 唤醒监听
关键设计 (q2): **"COW Map + 双检 = 无锁并发"** — cacheMap 用 AtomicReference 指向不可变 Map, 写时复制; addCacheDataIfAbsent 双检防重复 CacheData。 [模式: COW 缓存]

### 3. 传输内核 — ConfigRpcTransportClient 内部类与信号量

场景: 配置长轮询的客户端怎么驱动?
源码路径:
- **ConfigRpcTransportClient 是 ClientWorker 内部类** (ClientWorker.java:639) — 09 审计修正: 非独立文件
- **listenExecutebell 信号量** (L644-646): ArrayBlockingQueue(1) — 监听唤醒机制
- **ALL_SYNC_INTERNAL = 3 分钟** (L650): 全量同步检查周期 — "3 minutes to check all listen cache keys"
- getConnectionType = GRPC (L660) — 配置通道也是 gRPC
- shutdown 对称 (L666-701): RpcClientFactory 全量清理 (uuid 前缀匹配) + cacheMap 标记 consistentWithServer=false + subscriber 注销
关键设计 (q3): **"信号量 = 唤醒式长轮询"** — 监听不轮询, 靠 listenExecutebell 信号量被服务端变更唤醒; 3 分钟全量同步兜底 — 与 1.x HTTP 长轮询 (执行计划过时描述) 的架构差异。 [模式: 唤醒式监听]

### 4. 通知链 — CacheData 的任务执行与变更解析

场景: 服务端推送变更后, 监听器回调链长什么样?
源码路径:
- **CacheData.checkListenerMd5** (CacheData.java:342): md5 比对 → 有变化才通知
- **通知任务** (L430-500): ① **AbstractSharedListener.fillContext** (L438-441, 填充 dataId/group 上下文) ② **ClassLoader 切换** (L444-448 + L503: 应用 ClassLoader, 多应用部署 SPI 隔离) ③ 过滤链 doFilter (L454-457) ④ **receiveConfigInfo** (L460) ⑤ **AbstractConfigChangeListener → ConfigChangeHandler.parseChangeData → ConfigChangeEvent** (L462-468 — 变更项解析 + lastContent 更新) ⑥ 日志锚: notify-ok/notify-error
- **用户 Executor 异步** (L494-501): listener.getExecutor() 非空 → 用户线程池执行 (async=true); 否则内部线程
- 阻塞监控: notifyWarnTimeout 超时告警 (L451-453)
关键设计 (q4): **"通知 = 过滤 + 回调 + 变更解析三明治"** — 内容先过加密过滤链再回调; AbstractConfigChangeListener 额外获得"变更明细" (ConfigChangeItem 集合), 而普通 Listener 只有新内容 — 两种监听器两种粒度。 [模式: 双粒度回调]

### 5. 变更解析 — ConfigChangeHandler 的 SPI 解析器链

场景: ConfigChangeEvent 的变更项怎么算出来的?
源码路径:
- **ConfigChangeHandler 单例** (ConfigChangeHandler.java:46-56): Holder 懒加载 + **NacosServiceLoader SPI 加载 ConfigChangeParser** (L51) + **内置 PropertiesChangeParser/YmlChangeParser** (L52-53)
- **parseChangeData** (L70+): 新旧内容按 type 解析 → 变更项集合
- 消费方: ALI-A2 的 NacosPropertiesKeyListener 等 (key 级监听)
关键设计 (q5): **"解析器 = SPI 可扩展"** — properties/yml 内置, 用户可注册自定义 ConfigChangeParser (SPI 文件) — 与 SCC-1 PropertySourceLocator 的插槽哲学同构。 [模式: SPI 解析链]

### 6. 模糊监听 — fuzzyWatch 与 ConfigFuzzyWatchGroupKeyHolder

场景: 不知道 dataId 怎么监听?
源码路径:
- **fuzzyWatch 族** (NacosConfigService.java:126-172): dataIdPattern/groupNamePattern + FuzzyWatchEventWatcher → worker.addTenantFuzzyWatcher → ConfigFuzzyWatchContext
- ConfigFuzzyWatchGroupKeyHolder (config/impl/ConfigFuzzyWatchGroupKeyHolder.java:72, 545 行) — 模式匹配注册表
- cancelFuzzyWatch 对称 (L156-172)
关键设计 (q6): **"模糊监听 = 模式注册表"** — 按正则/通配监听一批 dataId, 变更批量通知; 3.x 新能力 (ALI-A2 未覆盖面)。 [模式: 模式监听]

### 7. 测试与行为锚

场景: 配置读写的边界行为?
源码路径:
- 测试: NacosConfigServiceTest / ClientWorkerTest / ConfigChangeHandlerTest (client/src/test)
- 注释锚: failover "is maintained by user" (L216-219) / "3 minutes to check all listen cache keys" (L648-649)
- issue 锚: #7039 (L110: getConfigAndSignListener 解密内容修复)
- 日志锚: [notify-context]/[notify-ok]/[notify-error] (L439/464/478)
关键设计 (q1): **"注释/issue/日志三锚"** — 语义注释 + 修复溯源 + 可观测日志齐全。 [模式: 行为锚]
