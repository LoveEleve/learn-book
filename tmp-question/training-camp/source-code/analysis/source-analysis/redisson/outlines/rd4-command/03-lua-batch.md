# RD-4 篇3 — lua-batch: Lua 脚本自愈与批处理分组

> 前置: [[RD-4-篇2]] (EVALSHA_RO 降级) | 复用: [[rd1-connection]] (entry 定位) | 对照: [[r20-server]] (服务端命令处理 vs 客户端 batch 编排) | 引出: [[rd2-rlock]] (锁 Lua 消费) + [[rd6-localcachedmap]] (缓存失效钩子)
> 🔴 A | 3 KP | [模式: 脚本缓存自愈 + 分组批处理 + 失效钩子]
> Pass 2 闭环: q6(缓存失效钩子) q7(Lua 执行) q8(批处理)

**读者处境**: RLock.tryLock 的 Lua 脚本每次传?还是缓存在服务端?服务器重启脚本丢了怎么办?一次批量写 100 个 key, 是发 100 条命令还是一个 pipeline?为什么"批≠事务"?这篇拆 Lua 的 EVALSHA 缓存 + NOSCRIPT 自愈, 批处理的按节点分组, 以及写命令触发本地缓存失效的钩子。

### 概念依赖链
q7(Lua 执行) ← q8(批处理) ← q6(缓存钩子) — 先讲复杂脚本怎么高效执行 (EVALSHA), 再讲多命令怎么聚合 (分组批), 最后讲写完成怎么联动缓存。

### 核心悬念
"Lua 脚本怎么做到'一次加载处处执行'+服务器重启自动恢复？批量 100 命令为什么快过 100 次单发？"

### 叙事顺序
1. 问题引入: tryLock 的 Lua 脚本每次都传?
2. Lua EVALSHA (q7) — map→calcSHA→EVALSHA; 双自愈: EVALSHA_RO 降级 / NOSCRIPT auto reload
3. 批处理 (q8) — CommandBatchService 按 NodeSource 分组 + skipResult 快路径
4. 缓存失效钩子 (q6) — 写完成→evictClientSideCaching
5. 收束: "缓存自愈 + 分组编排 + 联动失效" 三件套

### 1. Lua EVALSHA 缓存 — 脚本不重传 + NOSCRIPT 自愈

场景: 脚本怎么避免每次传输?服务器重启后?
源码路径:
- `evalAsync` (CommandAsyncService.java:578-659):
  - L581 `mappedScript = map(script)`; L583 `isEvalCacheActive() && EVAL` → 缓存路径:
  - L590 `calcSHA(mappedScript)` → sha1 (SHA1 寻址)
  - L592-595 cmd = EVALSHA_RO (readOnly+支持) 或 EVALSHA
  - L597-601 args = [sha1, keys.size, keys..., params...]; L603-607 RedisExecutor 执行
  - **双错误自愈** (CommandAsyncService.java:609-649):
    - L611-614 `ERR unknown command` → EVAL_SHA_RO_SUPPORTED=false → 递归降级
    - L615-638 **NOSCRIPT** → `loadScript(executor.getRedisClient(), script)` (SCRIPT_LOAD 预加载到该 client) → 绑定 NodeSource → 重发 EVALSHA
    - L639-643 其他 → 直接失败
  - 非缓存路径 (CommandAsyncService.java:653-658): 直接 EVAL 脚本文本
- `loadScript` (CommandAsyncService.java:498-504): SCRIPT_LOAD 按 client: entry 的 master → writeAsync; 否则 readAsync — **脚本加载对节点**
- useScriptCache 默认 true (Config:95)
关键设计 (q7): EVALSHA = 脚本 SHA 寻址省带宽; NOSCRIPT 自愈 = 服务器重启/新节点自动 reload + 重发 (Cluster 扩容/切换后可用)。[模式: 缓存+自愈]
数据流: tryLock → evalAsync → EVALSHA sha1 → NOSCRIPT? loadScript → 重发 EVALSHA → 成功。

### 2. 批处理 — 按节点分组 + 快路径

场景: 批量 100 条写, 怎么快过 100 次单发?
源码路径:
- `CommandBatchService extends CommandAsyncService` (CommandBatchService.java:53) — **批是单命令的超集**
- executeAsync (CommandBatchService.java:273-327):
  - L274 防重复 "Batch already executed!"
  - L284-286 Redis 队列模式
  - commands = `Map<NodeSource, Entry>` (CommandBatchService.java:289) — **命令按 NodeSource 分组** (Cluster 下按槽位次节点)
  - L290-310 **skipResult 快路径**: options.skipResult + syncSlaves==0 → void 模式不收集响应
  - L311+: 完整路径收集 responses (BatchResult)
  - L314-318 失败: 全部命令 tryFailure(ex) — 批量原子失败
- `BatchPromise extends CompletableFuture` (BatchPromise.java:25) — 延迟完成
- **边界**: batch = pipeline (性能优化) ≠ transaction (原子性); 事务需 BatchOptions/TransactionOptions (对照 Redis R-16 MULTI/EXEC)
关键设计 (q8): 批 = 按节点分组的 pipeline 编排, skipResult 免响应收集, 失败全灭。[模式: 分组批处理]
数据流: RBatch.create → 100 put → executeAsync → 按 slot 分组 → 每节点 pipeline → 收集。

### 3. 缓存失效钩子 — 写完成即失效本地缓存

场景: RLocalCachedMap 写入后, 本地缓存怎么保证不脏?
源码路径:
- `async()` (CommandAsyncService.java:717-727):
  ```
  if (!readOnlyMode && getServiceManager().hasCachingInstances()) {
      Arrays.stream(params).filter(r -> r instanceof String).findFirst()
          .ifPresent(name -> mainPromise.thenAccept(r -> getServiceManager().evictClientSideCaching(name)));
  }
  ```
- 条件: 写命令 + 有缓存实例 → 取第一个 String 参数 (key) → **命令成功后 evict**
- hasCachingInstances (ServiceManager:774 `Set<RedissonClientSideCaching>`) — 全局注册缓存实例
- 就近失效: 本实例写 → 本地立失效; 跨实例靠订阅通道 (RD-6 完整面)
关键设计 (q6): 一致性 = 写成功回调挂失效, 不阻塞命令; 本地缓存"指令即失效"捷径。[模式: 完成回调失效]
数据流: RLocalCachedMap put → 写命令成功 → evictClientSideCaching(key) → 本地缓存同步。

### 负面空间 — Lua/批/钩子刻意不做的事

- **不做脚本自动过期管理**: EVALSHA 缓存依赖服务端 SCRIPT FLUSH 时机, 客户端不主动清理
- **不做批的断点续传**: 失败整批重发 (tryFailure 全灭后由用户重试)
- **不做批内事务隔离**: 批无 MULTI 包裹, 非原子
- **不做跨节点脚本批**: 分组保证单节点内脚本一致, 跨槽位脚本需用户设计
- **不给缓存失效等 ack**: evict 即发, 无确认 (乐观)

→ 引出: 锁在这个 Lua 链路上怎么 wait/release?→ [[rd2-rlock]] ; 本地缓存失效的订阅侧 → [[rd6-localcachedmap]]