# R-30 Lua+Functions — Pass 1 探索笔记

> 域: R-30 Lua 脚本+Functions | 🟡 B 方案 | 2026-08-14
> 源码: src/eval.c (1749) + script.c (640) + script_lua.c (1712) + functions.c (1121) + function_lua.c (497) = 5936 行 | Redis 7.4.2

## 调用图

```
命令面 (eval.c):
EVAL/EVALSHA (evalCommand/evalShaCommand L622-659, 2.6.0) → evalGenericCommand (L545-621)
  → funcname "f_"+40字符SHA (L562-566) → registry 查找 (L577) → 未定义 → luaCreateFunction (L429-492)
    → sha1hex (L98) + shebang 解析 (evalExtractShebangFlags L319) + luaL_loadbuffer 编译 + dict 注册 + LRU (luaScriptsLRUAdd)
  → scriptPrepareForRun (script.c:170) → luaCallFunction → scriptResetRun → LRU 重排 (L610-615)
EVAL_RO/EVALSHA_RO: evalRoCommand (L632) / evalShaRoCommand (L656) — 只读版 (processCommand 拒绝写)
SCRIPT (scriptCommand L660): DEBUG/EXISTS/FLUSH[ASYNC|SYNC]/KILL/LOAD
FCALL/FCALLRO (functions.c:609-658, 7.0.0) → scriptPrepareForRun + engine call
FUNCTION (functions.c): LOAD/REPLACE/CREATE/DELETE/KILL/DUMP/RESTORE/LIST/STATS/FLUSH

引擎运行时 (script.c, 7.0 统一):
scriptPrepareForRun (L170-291): shebang 标志验证 (NO_CLUSTER/ALLOW_STALE/NO_WRITES/ALLOW_OOM) +
  EVAL_COMPAT_MODE 向后兼容路径; run_ctx 装载 (engine_client/original_client/slot)
scriptCall (L575-640): redis.call → lookupCommand → 六重验证 (arity/NOSCRIPT/stale/ACL/写允许/OOM) +
  集群状态 → call(CMD_CALL_PROPAGATE_AOF|REPL)
scriptInterrupt (L119-168): elapsed ≥ busy_reply_threshold (lua-time-limit 默认 5000, config.c:3201)
  → SCRIPT_TIMEDOUT → enterScriptTimedoutMode + protectClient + processEventsWhileBlocked → KILL/CONTINUE
scriptFlagsToCmdFlags (L151-168): shebang 标志 → 命令标志映射 (NO_WRITES 隐含 ALLOW_OOM)
scriptIsRunning/IsEval/GetClient/GetCaller (L314-329); scriptKill (L329)
luaEnvInit (L77-96): Lua 分配器钩子 (luaAlloc L47 — jemalloc arena 绑定, R-33/R-18 lua_arena)

沙箱与类型转换 (script_lua.c):
luaLoadLibraries (L1218-1234): base/table/string/math/debug/os + cjson/struct/cmsgpack/bit; io 不加载; package #if 0
全局保护: luaSetErrorMetatable + luaSetTableProtectionRecursively (eval.c:231-239) + allow_lists 6 组 (L96-107)
  libraries_allow_list/redis_api_allow_list/lua_builtins_allow_list(26)/not_documented/removed_after_init; deny_list (dofile/loadfile/print)
确定性随机: redis_math_random/redis_math_randomseed (L116-117) — 替换 math.random (主从一致)
类型转换: redisProtocolToLuaType_* 14 函数 (L218-300+, RESP2/RESP3 全类型) + luaReplyToRedisReply
redis.call/pcall: luaRedisGenericCommand → scriptCall

Functions 引擎 (function_lua.c):
luaEngineCreate (L84): FUNCTION LOAD 编译 (load 钩子超时 L64-81 — LUA_MASKLINE)
luaEngineCall (L143-181): registry 引用执行
luaRegisterFunction (L389): redis.register_function API

Functions 管理 (functions.c):
functionsLibCtx* (L163-235): 双 ctx (临时+当前) — FUNCTION LOAD 原子性
libraryJoin (L322-398): 冲突检测 (库/函数重名) + 回滚 (old_libraries_list) + 链接
functionLoadCommand (L1029); functionListCommand (L498); functionDump/Restore (L679/711, RDB 序列化)
engine 注册: functionsRegisterEngine (L400); luaEngineInitEngine (function_lua.c:414)
```

## 基本元素分解

1. **EVAL 入口**: sha1 注册名 + registry 函数 + 缓存 (dict+LRU)
2. **沙箱**: 库装载面 (os 有白名单/io 无/package 无) + 全局表递归只读 + 确定性随机
3. **脚本标志 (shebang)**: 7 标志 → 命令标志映射 + 运行时验证 (scriptPrepareForRun)
4. **脚本运行时**: scriptRunCtx + scriptCall 六重验证 + 超时中断 (busy 模式)
5. **类型转换**: RESP↔Lua 双向 14 类型
6. **Functions**: lib_ctx 双缓冲原子加载 + FCALL + 引擎抽象
7. **传播**: 脚本内命令各自传播 (effects) vs EVAL 整体

## 标记问题 (20 问)

1. sha1hex 怎么实现? (SHA1 40 字符)
2. lua_scripts dict + LRU 列表的缓存淘汰? (脚本缓存上限?)
3. shebang 解析细节? (#!lua + flags=; EVAL_COMPAT_MODE)
4. scriptPrepareForRun 的验证矩阵? (7 标志 × 场景)
5. redis.call 的命令验证链? (scriptCall 六重)
6. 超时后 busy 模式怎么工作? (processEventsWhileBlocked + SCRIPT KILL)
7. 沙箱怎么锁全局表? (递归只读 + 白名单)
8. os 库允许什么? (allow_lists 细节)
9. 确定性随机怎么实现? (替换 math.random)
10. RESP 类型 ↔ Lua 转换面? (14 函数)
11. redis.pcall vs redis.call 的错误语义?
12. 脚本内 SELECT/DB 隔离? (script_client selectDb)
13. 脚本传播? (effects 复述 vs EVAL 整体)
14. FUNCTION LOAD 原子性? (双 ctx + 回滚)
15. FUNCTION DUMP/RESTORE? (RDB 序列化)
16. luaEngineLoadHook 超时? (编译期超时)
17. SCRIPT FLUSH/EVAL 缓存清理? (scriptingReset)
18. Lua 内存记账? (lua_scripts_mem + evalMemory)
19. 脚本与事务交互? (CLIENT_MULTI 传递)
20. 集群跨槽? (ALLOW_CROSS_SLOT + scriptVerifyClusterState)

## 时空溯源 (代码内痕迹)

- EVAL/EVALSHA/SCRIPT **2.6.0** (commands.def:11095-11102 实证) — 2012 Redis 2.6 发布
- FCALL/FUNCTION **7.0.0** (commands.def:11099-11101) — 2022
- 版权: eval.c/script.c/script_lua.c/functions.c/function_lua.c 均 2020-Present (RSAL 时代重组)
- 演进痕迹: redis.replicate_commands (eval.c:154, 3.2 时代的 effects 传播开关 — 现为兼容空操作? 需验证); shebang 标志 (7.0); script.c 统一引擎 (7.0 重构 — engine/script 分离); luaEngineLoadHook 编译超时 (7.x); lua_scripts_lru_list (7.x 缓存淘汰); CLIENT_LUA_DEBUG/ldb 调试器 (2.8+ 历史残留)

## 大域拆分判断

5936 行 5 文件 — 大域! 闭环 6 个 — **不拆** (🟡 B 单篇大纲, 但节内覆盖两个引擎面 EVAL+Functions)

## 域级怀疑审计 (HANDOFF §四 R-30 简案 断言复查)

| HANDOFF 断言 | 验证 | 结论 |
|:--|:--|:--|
| "**EVAL 原子性** (单线程内不中断)" | scriptInterrupt (script.c:119-168): 超时后 **processEventsWhileBlocked 重入事件循环** (busy 模式) + SCRIPT KILL 可杀; 未超时确实不中断 | **修正: 单线程不交错执行, 但超时进入 busy 模式后可执行受限命令 (PING/SCRIPT KILL 等)+可被杀**; "完全不中断"是 3.2 前旧语义 |
| "**脚本缓存** (SHA1 摘要, EVALSHA)" | sha1hex (eval.c:98) + funcname "f_"+40 (L562-566) + lua_scripts dict + **LRU 淘汰列表** (luaScriptsLRUAdd/LRU 重排 L610-615) | **接受+补充**: SHA1 40 字符; 缓存带 LRU 淘汰 (7.x) |
| "**Lua 沙箱 (无 io/os 库, redis.call 全访问)**" | script_lua.c:1218-1234: **io 不加载 ✓, 但 os 加载** (L1224); package #if 0; 全局保护 = 递归只读 + allow_lists 6 组 + deny_list | **修正: os 存在但白名单受限 (luaSetTableProtectionRecursively + allow_lists), io 才是不加载** |
| "**7.0 脚本重写**: script.c 统一管理" | scriptRunCtx/scriptPrepareForRun/scriptCall/scriptInterrupt/scriptFlagsToCmdFlags — 引擎/脚本分离 | **接受** ✅ |
| "**Functions (7.0)**: FUNCTION CREATE 库管理, 与 EVAL 的传播差异" | FCALL/FUNCTION 7.0.0; lib_ctx 双缓冲; 引擎抽象 | **接受** ✅ (传播差异需闭环验证) |
| "超时处理 (lua-time-limit, SCRIPT KILL)" | busy_reply_threshold 默认 5000 (config.c:3201) + scriptInterrupt + SCRIPT KILL/FUNCTION KILL | **接受** ✅ |
