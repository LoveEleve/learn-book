# N-17 配置长轮询与 gRPC 通信 — HTTP 轮询遗留 + gRPC 监听主线

> 前置: [[NC-2-ConfigService]] (客户端面) + [[N-15-配置存储]] (缓存消费) | 对照: 1.x HTTP 长轮询 vs 3.x gRPC 监听
> 🔴 A | 方案 A (全深度) | 闭环: q1(长轮询) q2(gRPC handler 族) q3(变更通知)

**读者处境**: 配置变更怎么到客户端? HTTP 长轮询 (ClientLongPolling) 与 gRPC 监听 (ConfigChangeBatchListen) 双通道?

### 1. 长轮询 — LongPollingService 与 ClientLongPolling

场景: 1.x 遗留的 HTTP 长轮询怎么工作?
源码路径:
- **LongPollingService** (config/server/service/LongPollingService.java:63): **addLongPollingClient** (L172-216): ① **MD5Util.compareMd5 立即比对** (L181-184: 有变更直接响应 "instant") ② no-hang-up 头 (L187-193) ③ **AsyncContext.startAsync 异步挂起** (L198-200, setTimeout(0) 自控) ④ **checkLimit** (L202-208: 连接限制) ⑤ **timeout = max(10s, 请求超时 - 500ms)** (L210-212, "one response is returned 500 ms in advance to avoid client timeout") ⑥ **ConfigExecutor.executeLongPolling(ClientLongPolling)** (L213-214)
- **ClientLongPolling** (L264+): 订阅者任务 — run (L271): 挂起等待 → 变更唤醒
- **allSubs 队列** (L266): 全部订阅者
- **变更广播** (L78-91): 遍历 allSubs 唤醒
关键设计 (q1): **"提前 500ms 响应 + 自控超时"** — 响应比客户端超时早 500ms 防客户端误判; AsyncContext 挂起不占线程; 注释明言延迟意图 (L210)。 [模式: 提前响应]

### 2. gRPC handler 族 — remote 十五文件

场景: 3.x 配置 gRPC 请求怎么处理?
源码路径:
- **ConfigQueryRequestHandler** (remote/ConfigQueryRequestHandler.java:60): extends RequestHandler<ConfigQueryRequest, ConfigQueryResponse> — 查询
- **ConfigPublishRequestHandler** (remote/ConfigPublishRequestHandler.java:55): 发布
- **ConfigRemoveRequestHandler**: 删除
- **ConfigChangeBatchListenRequestHandler**: **批量监听** (3.x 主线 — 替代 HTTP 长轮询)
- **ConfigChangeListenContext** (remote/ConfigChangeListenContext.java:38): 监听上下文
- **RpcConfigChangeNotifier** (remote/RpcConfigChangeNotifier.java:51): 变更通知
- **ConfigClusterRpcClientProxy** (remote/ConfigClusterRpcClientProxy.java:33): 集群通信代理
- **ConfigConnectionEventListener**: 连接监听
- **模糊族** (ConfigFuzzyWatch* 5 文件): 模糊订阅 handler/notifier/sync
关键设计 (q2): **"handler 族 = 请求类型分派"** — 每个 gRPC 请求类型一个 handler; 批量监听是 3.x 主线 (与 NC-2 ConfigBatchListenRequest 对应)。 [模式: handler 族]

### 3. 变更通知 — RpcConfigChangeNotifier

场景: 配置变更怎么推给 gRPC 客户端?
源码路径:
- **RpcConfigChangeNotifier** (remote/RpcConfigChangeNotifier.java:51): 变更通知 — 按连接推送 ConfigChangeNotifyRequest
- 消费: NC-2 客户端 listenExecutebell 唤醒
- 与 N-12 命名推送平行的配置推送面
关键设计 (q3): **"变更通知 = 按连接推送"** — 服务端变更 → notifier → gRPC 推送; 与 N-12 的 clientId 遍历同构但按连接。 [模式: 连接推送]

### 4. 测试与行为锚

场景: 轮询/通信边界?
源码路径:
- 测试: LongPollingServiceTest / ConfigQueryRequestHandlerTest (config test)
- 日志锚: "{}|{}|{}|{}|{}|{}|{}" 客户端日志格式 (L184-193: instant/nohangup 标记)
- 注释锚: "one response is returned 500 ms in advance" (L210)
关键设计 (q1): **"日志格式 = 轮询可观测"** — instant/nohangup 区分响应路径。 [模式: 日志契约]
