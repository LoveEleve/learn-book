# Pass 1 探索笔记: RD-4 命令执行流水线

> 方案 A (🔴) | 源码: `/data/workspace/source-code/code/spring/redisson` (4.6.2-SNAPSHOT)
> 大域: command/ 12 文件 4027 行 (CommandAsyncService 1256 + RedisExecutor 928 + CommandBatchService 828) → 拆 3 篇

## Pass 0 上下文吸收

- command/ 12 文件: CommandAsyncService(1256)/RedisExecutor(928)/CommandBatchService(828)/RedisQueuedBatchExecutor(259)/RedisCommonBatchExecutor(201)/CommandAsyncExecutor接口(184)/同级小类
- 依赖: RD-3 (codec 显式传参) + RD-1 (连接池借还) + Redis r28 (RESP)
- 09 审计已知: retryAttempts=4, retryDelay=EqualJitter 1-2s, timeout=3000ms (BaseConfig), command 层是消费点
- 时空: CHANGELOG DelayStrategy 引入 (4.0) — 重试从静态 retryInterval 演进为可插拔延迟策略

## 继承树/调用图

```
结构方法 (getMap/put/set) → CommandAsyncService (1256 行)
  ├── async(readOnly, NodeSource, codec, command, params, ignoreRedirect, noRetry)  L690-733
  │    ├── serviceManager.resp3(command)         ⎠ RESP2/3 命令适配
  │    ├── SORT_RO 降级探测 (L694-714)           ⎠ 能力探测失败回退
  │    ├── Client-Side Caching 失效钩子 (L717-725) ⎠ 写命令→evict (RD-6)
  │    └── new RedisExecutor(...).execute()      ⎠ 单命令执行核心
  ├── getNodeSource(key) → calcSlot(key) → NodeSource(slot)  L395-405
  ├── evalWriteAsync/evalReadAsync (L471+)       ⎠ Lua 预加载 loadScript L498
  ├── readRandom/readRoundRobin/writeAll/executeAll L264-391 ⎠ 全 node 路由矩阵
  └── CommandBatchService (828)                   ⎠ 批处理

RedisExecutor (928 行)   ← 单命令执行: getConnection → sendCommand → 响应/重试/超时
  ├── execute() L122-230: 尝试循环入口
  ├── scheduleRetryTimeout L278-375 (三定时器+递归重试)
  ├── scheduleConnectionTimeout L232-252 (池满"Increase pool size")
  ├── scheduleWriteTimeout L254-276 (写堵"Check CPU/nettyThreads")
  ├── scheduleResponseTimeout (响应超时)
  └── checkWriteFuture L387-407 / handleError / tryComplete

NodeSource (connection/NodeSource:28-51)  ← slot/addr/Redirect(MOVED/ASK/REDIRECT)/entry
```

## 基本元素分解

1. **async() 汇聚点** — read/write/eval 全收敛; resp3 适配 + SORT_RO 降级 + 缓存失效钩子 (CommandAsyncService:690-733)
2. **getNodeSource** — calcSlot → NodeSource(slot) (L395-405); 集群分片寻址的入口
3. **RedisExecutor 执行核心** — 借用连接 → 发送 → 超时三定时器 → 重试递归 (928 行)
4. **重试协议** — scheduleRetryTimeout: attempt<attempts → 递归 execute(); 阻塞命令特殊路径 (L278-375)
5. **命令适配** — resp3() RESP2/3 版本转换 + SORT_RO 探测降级 (L692-714)
6. **Client-Side Caching 失效** — 写完成 → evictClientSideCaching? (L717-725, RD-6 衔接)
7. **Lua 预加载** — loadScript SCRIPT_LOAD 按 client 分读写 (L498-504)
8. **全 node 路由** — readRandom/readRoundRobin/writeAllAsync/executeAllAsync (L264-391)

## 标记问题 (8 个)

1. **Q1 异步执行管线**: RedisExecutor.execute() 的完整时序 — getConnection → sendCommand → 响应 → 归还? 各节点如何串成 CompletableFuture 链?
2. **Q2 重试协议边界**: scheduleRetryTimeout 在什么时点递增 attempt?连接失败/写失败/响应超时分别怎么重试?为什么 eval 续期用 noRetry(evicted)?
3. **Q3 超时三定时器**: connectionTimeout (池满) / writeTimeout (写堵) / responseTimeout (无响应) — 各自触发条件与告警文案差异?
4. **Q4 resp3 命令适配**: getServiceManager().resp3(command) 做了什么?RESP2/3 下命令面差异 (SORT_RO 等)?
5. **Q5 能力探测降级**: SORT_RO 失败 → 降级 SORT 的机制?探测缓存 (SORT_RO_SUPPORTED AtomicBoolean)?
6. **Q6 缓存失效钩子**: async() 写命令 → evictClientSideCaching 的触发条件 (hasCachingInstances)?与 RD-6 的衔接?
7. **Q7 Lua 执行链路**: evalWriteAsync → evalAsync → loadScript (SCRIPT_LOAD 预加载) 全流程?useScriptCache SHA 缓存?
8. **Q8 批处理**: CommandBatchService 的 accumulate → 一次性发送?与单命令 RedisExecutor 的关系?

## 已读测试 (2 个)

- RedissonBatchTest (顶层): 批处理验证
- CommandHandlersTest (顶层): 命令处理器
- (另浏览 RedisClientTest)

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 8 项全部有源码位置
- [x] 8 个标记问题有源码位置
- [x] 已读测试 (RedissonBatchTest/CommandHandlersTest)
- [x] 时空溯源 (CHANGELOG DelayStrategy 演进)