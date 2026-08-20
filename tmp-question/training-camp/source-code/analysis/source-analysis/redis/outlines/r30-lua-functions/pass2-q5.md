# 闭环笔记 q5: Functions 系统 — lib_ctx 双缓冲 + FCALL + 引擎抽象

## 假设
Functions = 7.0 引擎抽象: 命名库 (库→函数), 原子加载 (双 ctx), FCALL 执行, RDB 序列化。

## 验证过程
- **版本与面** (commands.def:11099-11101): FCALL/FCALLRO/FUNCTION **7.0.0**; FUNCTION 子命令: LOAD/REPLACE/DELETE/KILL/LIST/STATS/DUMP/RESTORE/FLUSH
- **数据结构** (functions.c:163-235): functionsLibCtx (libraries dict: 库名→functionLibInfo + functions dict: 函数名→functionInfo); `curr_functions_lib_ctx` 全局 (L1118)
- **引擎抽象** (L400-424): functionsRegisterEngine(name, engine{create/call/free...}) — Lua 引擎注册 (function_lua.c:414 luaEngineInitEngine); **engine 可扩展** (设计上支持多语言, 现仅 lua)
- **FUNCTION LOAD 原子性** (functionLoadCommand L1029 + libraryJoin L322-398):
  - 临时 ctx 编译 (functionsLibCtxCreate L211) → 成功 → libraryJoin(当前, 临时, replace):
    - **库重名冲突** (L337-341: !replace → "Library %s already exists"; replace → 旧库卸载入 old_libraries_list 待回滚)
    - **函数重名冲突** (L354-360: 新库的函数与现有冲突 → goto done 回滚)
    - 无冲突 → 全部链接 + 临时 ctx 清空 (L363-375)
    - **失败回滚** (L380-389: old_libraries_list 回链 — 已卸载的旧库恢复)
  - 成功 → functionsLibCtxSwapWithCurrent (L770, 指针交换 — O(1))
- **FCALL** (fcallCommandGeneric L609-658): scriptPrepareForRun (fcallro → ro=1) → engine call (luaEngineCall function_lua.c:143-181: registry 引用 + 2 参数 keys/args)
- **redis.register_function** (function_lua.c:197-414): 位置参数/命名参数 (--description/--flags 等) 解析 + 标志读取 (luaRegisterFunctionReadFlags L221) — 函数级标志 (no-writes 等)
- **FUNCTION DUMP/RESTORE** (L679-711): RDB 序列化整个 lib_ctx (FUNCTION FLUSH/LOAD 的持久化面 — RDB 类型)
- **FUNCTION LIST/STATS** (L425-576): 元数据 (库名/函数/描述/标志) + 运行统计 (fcalls 等)
- **内存记账** (engineFunctionMemoryOverhead/luaEngineGetUsedMemoy L149-169): 每函数/引擎可查询
- **超时** (function_lua.c:64-81): luaEngineLoadHook — **编译期超时** (LUA_MASKLINE + load_ctx 时限, "FUNCTION LOAD timeout") — 恶意长编译防护

## 代码类型
Implementation (插件式引擎 + 原子加载)

## 跨域关联
- R-8 (persistence): FUNCTION DUMP/RESTORE RDB 序列化 (functions 段)
- R-9 (replication): 函数传播 (FUNCTION LOAD 命令传播 vs EVAL body 重写 — 对照 q1)
- R-18 (defrag): evalScriptsDict defrag (R-18 §3 已见)

## 结论
Functions = 命名库系统 + 引擎抽象 (现仅 Lua) + 双 ctx 原子加载 (冲突检测+回滚) + 函数级标志 + RDB 序列化; 与 EVAL 的差异 = 按名调用/库管理/持久化面, 执行内核共用 scriptRunCtx。
源码位置: functions.c:163-235,322-424,498-658,679-711,1029-1118; function_lua.c:64-181,197-414
