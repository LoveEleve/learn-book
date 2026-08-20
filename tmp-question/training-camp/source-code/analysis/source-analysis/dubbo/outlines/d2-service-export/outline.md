# D-2 服务导出 — export 链与 Netty 启动

> 前置: [[D-1-SPI微内核]] (自适应+Wrapper 消费) | 引出: [[D-3-服务引用]] (对称面) + [[D-4-RPC调用]] + [[D-5-注册中心]] | 对照: ZK NettyServer (阶段4.3)
> 🔴 A | 8 KP | [模式: 6 层导出链 + scope 三态 + server 缓存]
> Pass 2 闭环: q1(export 链) q2(scope 三态) q3(DubboProtocol) q4(invoker 元数据)

**读者处境**: 配置好 ServiceConfig 后, 服务怎么从 Bean 变成可被调用的? 这篇拆 ServiceConfig (1202) + DubboProtocol (661)。

### 1. export 链 — 6 层调用

场景: 导出怎么编排?
源码路径:
- **export()** (L326): synchronized + 标志 + **delay 延迟导出**
- **doExportUrls** (L582): **loadRegistries** + 多协议遍历
- **doExportUrlsFor1Protocol** (L628): buildAttributes → buildUrl → exportUrl
- **doExportUrl** (L978-993): **proxyFactory.getInvoker → DelegateProviderMetaDataInvoker → protocolSPI.export** → exporters 收集 (registerType 分组)
- **registerType 修正**: REGISTER_KEY=false → MANUAL; NEVER/MANUAL → URL 加 REGISTER_KEY=false
关键设计 (q1): **6 层链 + invoker 生成 + 自适应 Protocol**。[模式: 导出链]

### 2. scope 三态 + 本地导出

场景: 导出到哪?
源码路径:
- **scope 三态** (L650-690): **NONE 不导出 / LOCAL 仅本地 / REMOTE 仅远程** / 默认双导出
- **exportLocal** (L1010-1020): **injvm (LOCAL_PROTOCOL + port 0)** + EXPORTER_LISTENER
- **exportRemote** (L730-770): registry 遍历 + **EXT_PROTOCOL 附加协议** (IS_PU_SERVER/IS_EXTRA 标记, L660-685) + **MetadataUtils.publishServiceDefinition** (元数据发布)
- **DYNAMIC_KEY 继承** + isOnlyInJvm
关键设计 (q2): **本地始终可导 + 远程按 registry**。[模式: scope 三态]

### 3. DubboProtocol — Exporter + serverMap

场景: 服务端怎么启动?
源码路径:
- **export** (DubboProtocol:346-375): **DubboExporter (exporterMap 索引)** + openServer + **optimizeSerialization** (L373); ⚠ **回调服务导出面** (IS_CALLBACK_SERVICE + STUB_EVENT, L351-360 — 消费者反向导出双工)
- **openServer** (L377-405): **serverMap 按地址缓存 + 双检锁** → createServer; 已存在 → **server.reset (override)**
- **createServer** (L407-430): URL 补参数 (**READONLYEVENT/HEARTBEAT 默认/CODEC=DubboCodec**) → **Exchangers.bind — Netty 启动**
- **checkDestroyed** + 同地址多服务共享 server
关键设计 (q3): **exporterMap 索引 + server 缓存 + reset override**。[模式: server 管理]

### 4. invoker 与元数据 — proxyFactory + Delegate

场景: 导出什么?
源码路径:
- **proxyFactory.getInvoker** (L988): ref → Invoker (JavassistProxyFactory 默认)
- **DelegateProviderMetaDataInvoker**: 元数据委托包装
- **protocolSPI** (L188): **Protocol 自适应** — URL protocol 参数选实现 — **D-1 消费面**
- **Wrapper 织入**: ProtocolListenerWrapper/FilterWrapper — export 织入监听/过滤
关键设计 (q4): **invoker = 代理 + 元数据 + 自适应协议 (Wrapper 织入)**。[模式: invoker 面]

## 代码类型
Architecture (服务导出面)

## 负面空间 — 服务导出刻意不做的事

- **不做热发布**: export 后即固定 (override 靠 reset/动态配置)
- **不做按需懒加载**: 启动即导出 (delay 可配但默认立即)
- **不做多租户隔离**: 单服务单注册 (无租户维度)
- **不做服务降级**: 导出失败即启动失败 (无降级路径)
- **不做端口复用协商**: 同地址共享 server — 端口冲突即失败
- **不做导出回滚**: exporters 收集后无事务回滚 (部分失败留残)

→ 引出: 服务引用 (对称面) → [[D-3-服务引用]]
