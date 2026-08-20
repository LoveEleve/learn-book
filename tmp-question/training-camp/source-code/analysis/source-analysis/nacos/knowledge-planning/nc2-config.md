# NC-2 ConfigService 配置客户端 — 知识规划 (KP)

> 🔴 A | 模块: client/config (310+1408+649 行核心) | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 三路读取 | NacosConfigService:206-261 | failover(用户) > server(主) > snapshot(异常兜底) |
| 2 | 出站发布 | NacosConfigService:273-290 | ConfigRequest + 过滤链 + casMd5 可选 |
| 3 | 监听注册 | ClientWorker:195-210 | addTenantListeners + notifyListenConfig |
| 4 | COW 缓存 | ClientWorker:134/381-402 | AtomicReference + 双检 |
| 5 | 信号量内核 | ClientWorker:639-650 | 内部类 + listenExecutebell + 3 分钟全量 |
| 6 | 通知五步 | CacheData:430-500 | fillContext/ClassLoader/过滤/回调/变更解析 |
| 7 | 双粒度回调 | CacheData:462-468 | Listener vs AbstractConfigChangeListener |
| 8 | 解析器链 | ConfigChangeHandler:46-73 | SPI 加载 + Properties/Yml 内置 + type 分派 |
| 9 | 模糊监听 | NacosConfigService:126-172 | fuzzyWatch 模式注册表 |
| 10 | 健康 | NacosConfigService:293-299 | worker.isHealthServer → UP/DOWN |

## 02 高频坑

1. server 返回 null(无配置)不落 snapshot — 只有异常才兜底
2. ConfigRpcTransportClient 是 ClientWorker 内部类, 非独立文件
3. 通知链先切 ClassLoader 再回调 (多应用部署 SPI 隔离)
4. 变更明细只给 AbstractConfigChangeListener (需 parseChangeData)
5. listenExecutebell 是 ArrayBlockingQueue(1) 信号量, 唤醒式非轮询
6. 3 分钟全量同步兜底
7. failover 文件是用户手动维护, 不是客户端自动

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 容灾 | failover/server/snapshot 三路 / NO_RIGHT 不吞 / 加密键三路对应 |
| 并发 | AtomicReference COW / 双检 / 信号量唤醒 |
| 通知 | md5 门控 / ClassLoader 切换 / 双粒度 / 用户 executor 异步 |
| 解析 | SPI ConfigChangeParser / type 分派 / 差异比较 |
| 传输 | GRPC 通道 / 3 分钟兜底 / shutdown 对称 |
| 过滤 | ConfigFilterChainManager 出入站 / LocalEncryptedDataKeyProcessor |

## 04 跨域桥接

- ← ALI-A1/A2: getConfig/addListener 消费方实证 (三路容灾 + 监听链内核)
- → NC-3: ConfigRpcTransportClient 的 gRPC/Redo 底座
- → NC-4: LocalConfigInfoProcessor snapshot/failover 面
- → 面试: "Nacos 配置客户端怎么工作" — 三路 + 唤醒监听 + 变更解析
