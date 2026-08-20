# R-31 模块系统 — Redis 的插件扩展层 (361 API + 类型系统 + 钩子面)

> 前置: [[R-20-server]] (命令表/call 链) + [[R-8-persistence]] (RDB MODULE_2/AOF) + [[R-18-defrag]] (defrag 三入口) + [[R-32-acl]] (模块命令入位图) + [[R-26-list]] (阻塞框架) + [[R-29-pubsub]] (keyspace 通知) | 引出: 阶段3.6 Redisson (客户端) | 对照: [[R-30-lua]] (另一嵌入扩展面)
> 🟡 B | 6 KP | [模式: 插件扩展层 + 版本化契约 + proxy 分派 + 钩子面]
> Pass 2 闭环: q1(API 面) q2(类型系统) q3(命令注册) q4(执行面) q5(事件钩子) q6(加载边界)

**读者处境**: Redis 是 C 写的, 怎么让第三方加数据类型? 模块键怎么进 RDB? 后台线程怎么碰主线程的数据? 这篇拆模块系统: 361 个 API、64 位类型 ID、版本化契约、proxy 分派、钩子面。

### 1. API 面 — 361 个直接链接函数

场景: 模块怎么"调用 Redis"?
源码路径:
- **361 个 RM_ 函数** (module.c grep 穷举) — 模块 .so 直接链接 extern 符号; 契约头 redismodule.h
- **版本兼容**: RedisModule_Init(APIVER_1) + **typemethods 版本字段** (L6938-6969: v1→v5 增量字段 — rdb/aux/free_effort/unlink/copy/defrag/mem_usage2)
- **ctx 交互载体** (moduleCreateContext/moduleFreeContext L804): 每次命令/回调创建释放
- **AutoMemory**: 模块内存所有权辅助
关键设计 (q1): **直接链接 + 版本字段 = C 扩展的标准契约**; 361 API 的代价 = 兼容负担 (每字段版本化)。[模式: 插件 API]

### 2. 类型系统 — 64 位 ID + 全序列化契约

场景: 模块类型怎么让 RDB/复制/淘汰/碎片全部认识?
源码路径:
- **类型 ID** (moduleTypeEncodeId L6654): **9 字符名×6bit (64 符号表) + 10bit encver = 64 位**
- **方法表 v1-v5** (L6941-6990): rdb_load/save (R-8) / aof_rewrite / mem_usage (INFO) / digest (DEBUG DIGEST) / free / v2 aux / v3 free_effort+unlink+copy+defrag (R-21/R-18) / v4 二代 / v5 aux_save2 — **Redis 每个子系统都有模块回调**
- **RDB MODULE_2**: 类型 ID + 模块 rdb_save 数据 (R-8 交叉)
关键设计 (q2): **类型 = 64 位 ID + 方法表** — 一个结构让模块类型融入全部子系统; 版本字段保兼容。[模式: 扩展类型]

### 3. 命令注册 — proxy 分派

场景: 模块命令怎么进命令表?
源码路径:
- **注册** (RM_CreateCommand L1253): onload 限制 + strflags 解析 + 冲突检查 + **双 dict 注册** (commands+orig_commands) + **ACL ID** (R-32)
- **proxy** (L1286-1301): RedisModuleCommand{module/func/rediscmd} — rediscmd->proc = **RedisModuleCommandDispatcher** (L910): 建 ctx + cp->func(ctx, argv, argc) — 统一分派器
- **键位置**: 静态 (firstkey/lastkey/keystep) / **动态 getkeys-api** (moduleGetCommandKeysViaAPI — KEYS_POS_REQUEST 上下文调模块函数)
- **子命令**: RM_CreateSubcommand (L1377)
关键设计 (q3): **proxy = 原生命令的同构接入**; 模块命令与内建命令在命令表/ACL/键提取上完全等价。[模式: 代理分派]

### 4. 执行面 — RM_Call + 阻塞 + 线程

场景: 模块怎么执行命令? 怎么并发?
源码路径:
- **RM_Call** (L6304): fmt 参数 → **临时客户端** (moduleAllocTempClient) + DENY_BLOCKING + DB 对齐 + resp 选项 + **RUN_AS_USER** (ACL) → call() 全链 — 与正常命令同构; **传播显式化: RM_Replicate (L3560)/RM_ReplicateVerbatim (L3605) — 模块命令默认不传播, 需显式调用 (对照原生命令自动传播)**
- **阻塞 API**: RM_BlockClient / OnKeys (R-26 框架) + moduleHandleBlockedClients (L8311)
- **线程面** (L8522-8587): **RM_ThreadSafeContextLock/Unlock** — 后台线程经全局锁进主线程临界区; 线程唤醒管道 (module_pipe)
- **busy 面**: busy_module_yield_flags (R-30 交叉 — 模块长命令进 busy)
关键设计 (q4): **临时客户端 = 把模块调用复用到完整命令链**; 全局锁 + 管道 = 单线程核心的受控并发窗口。[模式: 受控并发]

### 5. 事件与钩子 — 订阅总线 + 命令过滤

场景: 模块怎么感知 Redis 内部事件?
源码路径:
- **事件订阅** (RM_SubscribeToServerEvent L11663) → moduleFireServerEvent (L11761): REDISMODULE_EVENT_* 45 处枚举 (加载/持久化/复制/键空间/ACL/集群)
- **命令过滤** (RM_RegisterCommandFilter): moduleCallCommandFilters (L6367) — **processCommand 前改写链** (拦截/插参/换参)
- **keyspace 通知** (R-29 交叉): moduleNotifyKeyspaceEvent (L8800) — **enterExecutionUnit 技巧**: 通知回调内传播命令须与触发命令同执行单元 (MULTI 包裹, "ugly hack" 注释 L8804-8812, execution_nesting 交叉 R-30); **模块 API 写键走 signalModifiedKey** (L2487,4119) — WATCH (R-16) 与 tracking 失效 (R-17) 覆盖模块键修改 / **ACL API** (R-32) / **INFO 注入** (RM_RegisterInfoFunc) / **模块配置** (RM_Register*Config L12869+)
关键设计 (q5): **钩子面 = 行为扩展** — 模块不只是数据结构, 还能改命令流/听事件/管配置。[模式: 事件总线]

### 6. 加载面与核心边界 — dlopen + 8 大钩子

场景: 模块什么时候加载? 挂了会怎样?
源码路径:
- **初始化** (moduleInitModulesSystem L11999): 全局结构 + 线程管道 + Timers rax
- **加载** (moduleLoadFromQueue L12109): config "loadmodule" 队列 → dlopen → RedisModule_Init → onload; **失败 exit(1)** — 信任扩展
- **与核心边界 8 钩子**: 命令过滤 (processCommand 前) / 事件 (cron+loading) / keyspace / defrag (R-18) / 阻塞 (R-2) / PostExecUnitJobs (R-30) / 线程管道 / INFO
- **依赖图**: usedby/using (模块间依赖)
关键设计 (q6): **加载 = 信任扩展 (失败即亡)**; 每个钩子点都已在前面 32 域见过 — 模块系统是 Redis 各域的旁路扩展层。[模式: 插件加载]

### 负面空间 — 模块系统刻意不做的事

- **不做沙箱**: 模块是信任代码 (C 语言全权限) — 对照 R-30 Lua 沙箱
- **不做热加载管理**: MODULE LOAD 命令可运行期手动加载, 但无配置变更自动加载/优雅卸载 (依赖图约束)
- **不做多语言**: 官方引擎仅 C (社区有第三方语言绑定)
- **不做隔离**: 模块崩溃 = 进程崩溃 (无独立进程)
- **不做版本强制**: 兼容靠自觉 (APIVER 软检查)
- **协议透明**: 模块命令对客户端零感知 (普通协议调用) — RediSearch/RedisJSON 等生态即此模式的产物

→ 引出: 客户端怎么用这些特性? → 阶段3.6 [[REDISSON-PLAN]]
