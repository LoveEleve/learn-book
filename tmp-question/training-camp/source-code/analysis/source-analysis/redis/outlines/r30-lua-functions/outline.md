# R-30 Lua 脚本 + Functions — 嵌入语言的原子执行 (缓存/沙箱/超时/引擎)

> 前置: [[R-20-server]] (call 命令链/CMD_NOSCRIPT/ACL) + [[R-16-multi]] (原子性对照/CLIENT_MULTI) + [[R-28-networking]] (RESP2/3 协议) + [[R-33-zmalloc]] (jemalloc arena) + [[R-21-db]] (键空间) | 引出: [[R-31-module]] (另一嵌入扩展面) | 对照: [[R-16-multi]] (事务 vs 脚本) + [[R-18-defrag]] (lua_arena 排除)
> 🟡 B | 6 KP | [模式: SHA1 缓存 + 沙箱 + 标志驱动验证 + 中断 + 引擎抽象]
> Pass 2 闭环: q1(缓存) q2(沙箱) q3(运行时) q4(超时) q5(Functions) q6(转换+命令面)

**读者处境**: EVAL 说"原子"为什么超时后别的命令又能执行? 脚本缓存为什么要存原始代码? 为什么 io 库进不来、os 库能进? 脚本怎么传播到从库? 这篇拆 Lua 脚本与 Functions: SHA1 缓存与 LRU、沙箱锁、shebang 标志、六重验证、busy 模式、引擎抽象。

### 1. 脚本缓存 — SHA1 注册名 + 原始代码双缓存

场景: EVALSHA 为什么依赖"之前 LOAD 过"? 传播时从库怎么知道代码?
源码路径:
- **sha1hex** (eval.c:98-117): 20 字节哈希 → 40 字符十六进制; 注册名 `"f_"+sha` (43 字符, L562-566) 作 Lua registry 键
- **双缓存** (luaCreateFunction L429-492): registry (sha→编译函数) + dict (sha→luaScript{body,flags,node})
- **body 缓存用途** (L468-471 注释): "replicate / write in the AOF all the EVALSHA commands as EVAL using the original script" — 注释保留 2.6-3.2 全量传播语义; **7.0 effects 时代**主要支撑: ① EVALSHA 可用性 (无缓存 → NOSCRIPT) ② 旧版从库/旧 AOF 兼容路径; 无缓存 EVALSHA → NOSCRIPT 错误 (L586-591)
- **shebang 解析** (L319-360): `#!lua flags=...` → **5 个可写标志** (script.c:16-22: no-writes/allow-oom/allow-stale/no-cluster/allow-cross-slot-keys); **无 shebang 时默认 EVAL_COMPAT_MODE** (script.h:64, 不可写); **编译时跳过 shebang 行保留换行** (L456-457 保行号)
- **LRU 淘汰** (L527-545): **LRU_LIST_LENGTH=500**; **无访问时间戳的 FIFO+执行即重排** (近似 LRU, 执行后 unlink+link tail L610-615); **仅 EVAL 淘汰, SCRIPT LOAD 不淘汰** (L530-531); 淘汰计数 **stat_evictedscripts** (L537, INFO evicted_scripts)
- **SCRIPT FLUSH/EXISTS/LOAD/KILL/DEBUG** (L660+): FLUSH → scriptingReset (release+init, L282-286)
关键设计 (q1): **缓存 = 执行句柄 + 传播素材双目的**; 500 上限防无限缓存 (脚本会随 EVAL 无限累积); EVAL 淘汰而 LOAD 不淘汰 = "显式管理的脚本不驱逐"。[模式: 内容寻址缓存]

### 2. 沙箱 — 装载面控制 + 全局表锁

场景: 为什么脚本不能读文件、不能开网络、不能改全局?
源码路径:
- **库装载** (script_lua.c:1218-1234): base/table/string/math/debug/**os** + cjson/struct/cmsgpack/bit (Redis 自带 C 库); **io 不加载**; package `#if 0` (L1231-1233 "for sandboxing concerns"); **os 库被 Redis 补丁精简为仅 os.clock** (deps/lua/src/loslib.c:240-243 sandbox_syslib "Only a subset is loaded currently" — 测试实证 os.execute/remove/rename 均报错 scripting.tcl:675-679)
- **全局保护** (eval.c:231-239): luaSetErrorMetatable (eval.c:1268-1278: 访问未定义全局 → 报错 "Script attempted to access nonexistent global variable") + **luaSetTableProtectionRecursively** (全局表递归只读)
- **白名单** (script_lua.c:96-107): 6 组 — libraries/redis_api (redis+__redis__err__handler)/lua_builtins 26 个 (含 loadstring/load)/not_documented (newproxy)/removed_after_init (**debug** — 错误处理器创建后置 nil); deny_list (dofile/loadfile/print)
- **白名单时机** (L92-95 注释): "only checked on start time, after that the global table is locked" — 启动期防未来意外全局, 运行期靠锁
- **确定性随机** (script_lua.c:1475-1486 + rand.c:1-6): math.random 替换为 **redisLrand48 (rand48 算法)** — 动机 = **跨系统一致性** ("not affected by specific libc random() implementations... same sequence in every arch"; libc rand() 同种子跨架构不保证同序列); **主从一致性由 effects 传播保证** (从库执行传播的命令流, 不重放脚本; 脚本内随机只影响主库本地命令决策); **非确定性命令确定性化**: SPOP 在脚本内被重写为 SREM (t_set.c:968 rewriteClientCommandVector) — 传播的命令流确定; rand48 静态状态进程级推进 (无每次播种)
关键设计 (q2): **双层防御 = 装载面 (不存在的库最安全) + 运行期锁 (白名单+递归只读)**; os 存在但受限 vs io 直接不存在 — 精细到函数的白名单; 确定性随机 = 传播一致性的根基 (对照 R-12 HLL 固定 seed 同哲学)。[模式: 沙箱]

### 3. 运行时 — 标志驱动验证 + effects 传播

场景: redis.call 里有什么命令出不来? 脚本怎么写进 AOF?
源码路径:
- **scriptPrepareForRun** (script.c:170-291): shebang 标志验证 (NO_CLUSTER×cluster / ALLOW_STALE×stale / NO_WRITES → 只读副本+磁盘错误+ro 命令+少从库四门禁 / ALLOW_OOM×maxmemory); **EVAL_COMPAT_MODE 兼容路径只查 stale** (L239-245)
- **run_ctx 装载** (L246-286): engine_client (Lua 专用 client) + original_client + **selectDb 对齐** (L255) + **MULTI 上下文传递** (L259-264) + SCRIPT_READ_ONLY/ALLOW_OOM/ALLOW_CROSS_SLOT 派生 + curr_run_ctx 全局 (KILL 用)
- **scriptCall 六重验证** (L575-640): arity / **CMD_NOSCRIPT** (L588-590, 可旁路) / stale / **ACL 复查** / 写允许 (只读 ctx 拒写) / **OOM** + 集群跨槽 (L601-608) → call(CMD_CALL_PROPAGATE_AOF|REPL)
- **effects 传播** (7.0 默认): 脚本内命令各自传播; **redis.replicate_commands 空操作** (eval.c:154-158 — 3.2 开关残留, 7.0 后 effects 不可关; 测试实证 "replicate_commands is the default on Redis Function" scripting.tcl:1396); **redis.set_repl() 仍有效** (script.c:536-544, 运行时改 run_ctx->repl_flags — REPL_ALL/AOF/REPLICA/NONE); 只读命令不传播 (测试 "MGET shouldn't be propagated")
- **luaCallFunction** (script_lua.c:1604-1660): 指令计数钩子 (超时) + KEYS/ARGV 注入 (临时解只读锁) + lua_pcall
关键设计 (q3): **标志 = 脚本对运行时的"合同声明"** (写/内存/集群/过期自述), 运行时按合同验证 — 对照 2.6 时代全量保守检查; effects 传播 = 从库逐命令重放 (可精确到只读命令不传播)。[模式: 声明式标志]

### 4. 超时与中断 — busy 模式与 SCRIPT KILL

场景: 死循环脚本怎么救? 为什么已写的脚本杀不掉?
源码路径:
- **配置** (config.c:3201): busy-reply-threshold (lua-time-limit) 默认 **5000ms**
- **钩子链**: LUA_MASKCOUNT **100000 指令**计数 (script_lua.c:1613-1615) → luaMaskCountHook (L1545) → scriptInterrupt
- **scriptInterrupt** (script.c:119-168): 超时 → **enterScriptTimedoutMode** + **protectClient** (L146-148, 防断连 UAF) + **processEventsWhileBlocked** (受限重入事件循环 — busy 模式)
- **busy 模式命令面** (server.c:4155-4175 + commands.def 穷举): 拒绝条件 = `isInsideYieldingLongCommand() && !(c->cmd->flags & CMD_ALLOW_BUSY)` → -BUSY (eval/function 区分文案 L4167-4169); **CMD_ALLOW_BUSY 12 命令**: auth/hello/quit/reset/replconf/shutdown/script-kill/function-kill/function-stats/multi/discard/watch/unwatch (13 含 container — 穷举实证); **事务命令豁免** (L4156-4160 注释, PR #7022: MULTI 等不拒 — 防 pipeline 事务半执行)
- **KILL 门槛** (script.c:329-360): **已写脚本不可 KILL** (SCRIPT_WRITE_DIRTY — 部分执行不可回滚, 对照 R-16) → 只能 SHUTDOWN NOSAVE
- **pcall 免疫** (script_lua.c:1551-1555): KILL 后钩子改 **LUA_MASKLINE** — "so the user will not be able to catch the error with pcall and invoke pcall again" (防脚本吞掉 KILL 错误继续跑)
关键设计 (q4): **超时 = 从"完全隔离"到"受限重入"的降级**: 未超时单线程不交错 (原子性), 超时后允许受限命令进入 (可用性优先); KILL 的写门槛 = 与事务一致的"部分执行不可撤销"哲学。[模式: 中断降级]

### 5. Functions — 命名库 + 原子加载 + 引擎抽象

场景: FUNCTION 和 EVAL 什么关系? LOAD 失败会怎样?
源码路径:
- **版本** (commands.def:11099-11101): FCALL/FCALLRO/FUNCTION **7.0.0**; 子命令: LOAD/REPLACE/DELETE/KILL/LIST/STATS/DUMP/RESTORE/FLUSH
- **数据面** (functions.c:163-235): functionsLibCtx = libraries dict (库名→functionLibInfo) + functions dict (函数名→functionInfo); curr_functions_lib_ctx 全局
- **引擎抽象** (L400-424): functionsRegisterEngine(name, engine{...}) — 设计上多引擎可插拔, 现仅 Lua (luaEngineInitEngine function_lua.c:414)
- **LOAD 原子性** (L322-398 libraryJoin): 临时 ctx 编译 → **库重名冲突** (replace 时旧库暂存待回滚) → **函数重名冲突** → 全链接 → 失败回滚 (old_libraries_list 回链) → 成功指针交换 (L770, O(1))
- **FCALL** (L609-658): scriptPrepareForRun + 引擎 call (luaEngineCall function_lua.c:143: registry 引用, keys/args 2 参数)
- **register_function** (function_lua.c:197-414): 位置/命名参数 + **函数级标志** (no-writes 等 — 比 EVAL 更细粒度)
- **编译期超时** (function_lua.c:64-81): luaEngineLoadHook LUA_MASKLINE — "FUNCTION LOAD timeout" (恶意长编译防护)
- **DUMP/RESTORE** (L679-711): 整个 lib_ctx RDB 序列化 (R-8 交叉)
关键设计 (q5): **Functions = 命名空间化 + 可持久化 + 原子替换的 EVAL**; 双 ctx 保证"要么全部生效要么原样" (FUNCTION LOAD 失败零副作用); 函数级标志比脚本级更精确。[模式: 原子库加载]

### 6. 类型转换与命令面 — RESP↔Lua 双向 + 四命令族

场景: 脚本里返回的 table 怎么变成协议? 命令面多大?
源码路径:
- **RESP→Lua** (script_lua.c:218-300): 14 类型转换器 (Int/Bulk/Null/Status/Error/Array/Map/Set/Bool/Double/BigNumber/Verbatim/Attribute — RESP3 全谱) + ReplyParser; **luaGC 周期 = LUA_GC_CYCLE_PERIOD 50 命令** (script_lua.c:1705)
- **Lua→RESP** (luaReplyToRedisReply L133): nil/number/string/boolean/table + **错误表 metatable** 语义 (带 err 字段的 table → error)
- **DB 隔离** (测试 "SELECT inside Lua should not affect the caller"): script_client 独立 DB 上下文
- **命令面四族**: EVAL/EVALSHA **2.6.0** (EVAL_RO 6.2 推断) + SCRIPT **2.6.0** (DEBUG/EXISTS/FLUSH/KILL/LOAD) + FCALL/FUNCTION **7.0.0**; 全 CMD_NOSCRIPT|SKIP_MONITOR|MAY_REPLICATE|STALE
- **调试器** (eval.c:754-957): CLIENT LUA DEBUG 断点/单步 (2.8 历史遗留)
- **内存记账** (L734-752): lua_scripts_mem + lua 运行时 + 开销; **测试面**: scripting 101 + functions 133 测试 (功能面最全)
关键设计 (q6): **转换器 = 协议全谱的双向镜像** (RESP3 新类型 (map/set/double) 与 Lua 表/数字的映射); 命令四族覆盖"执行/缓存管理/命名函数/库管理"四层。[模式: 协议镜像]

### 负面空间 — 脚本系统刻意不做的事

- **不做多引擎**: 引擎抽象存在但仅 Lua 实现 (FUNCTION 可扩展点未填)
- **不做 Lua 5.4 迁移**: 仍 Lua 5.1.5 (vendored) — 兼容性锁定的旧版本
- **不做完整沙箱**: 白名单是"启动期快照", 非运行期动态审计; os 库部分可用
- **不做写后撤销**: 已写脚本不可 KILL (只能 SHUTDOWN NOSAVE) — 无事务回滚等价物
- **不做脚本续跑/断点持久化**: 调试器会话进程级, 不跨重启
- **不做函数级 AOF 重写**: Functions 靠 DUMP/RESTORE + LOAD 命令传播, EVAL 靠 body 重写 — 两套机制不统一
- **不做脚本预编译缓存到磁盘**: 全部内存态 (重启需重新 LOAD)

→ 引出: 模块系统怎么扩展 Redis? → [[R-31-module]]
