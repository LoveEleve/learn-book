# R-30 Lua+Functions — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.6.0 (2012) | EVAL/EVALSHA/SCRIPT (commands.def:11095-11102 实证) — 脚本系统初版: Lua 5.1 (deps/lua/src/lua.h:19 实证), KEYS/ARGV, redis.call/pcall, 全量传播 (脚本整体广播 EVAL) |
| 2.8 | **脚本调试器** (ldb*, eval.c:754+ 历史代码) — CLIENT LUA DEBUG; 只读从库脚本限制演进 |
| 3.2 | **effects 传播**: redis.replicate_commands (eval.c:154 现为空操作 — 7.0 前曾控制"命令级传播"); 脚本超时后可 KILL 演进 |
| 5.0 | 更多确定性保证 (math.random 替换 — 推断, 需标注) |
| **7.0 (2022)** | **script.c 统一引擎重构**: scriptRunCtx/scriptPrepareForRun/scriptCall/scriptInterrupt 提取 (eval.c 拆分); **shebang 标志** (#!lua flags= — SCRIPT_FLAG_* 7 个); **Functions 系统** (FCALL/FUNCTION 7.0.0) + lib_ctx 双缓冲 + 引擎抽象; **replicate_commands 变空操作** (effects 默认); 传播统一为命令级 |
| 7.x | **脚本缓存 LRU 淘汰** (LRU_LIST_LENGTH=500, eval.c:527 + lua_scripts_lru_list); FUNCTION LOAD 编译超时 (luaEngineLoadHook); 函数级标志 (register_function) |

## 痕迹证据

- commands.def: EVAL/EVALSHA/SCRIPT 2.6.0 / FCALL/FUNCTION 7.0.0 — 权威版本实证
- deps/lua/src/lua.h:19-21: Lua 5.1.5 (vendored) — 沙箱基础版本
- eval.c:468-471: 双缓存目的注释 (EVALSHA 重写为 EVAL)
- eval.c:154-158: replicate_commands 空操作 (残留 API — effects 时代证据)
- script.c:119-168: scriptInterrupt 超时链注释 (Slow script detected)
- script.c:146-148: protectClient 注释 (client may disconnect)
- script_lua.c:92-95: 白名单时机注释 (only checked on start time... global table is locked)
- script_lua.c:1231-1233: package #if 0 沙箱注释
- script_lua.c:1551-1555: KILL 后 MASKLINE 注释 (pcall 免疫)
- eval.c:527-531: LRU 淘汰注释 (only applies to EVAL, not SCRIPT LOAD)
- function_lua.c:64-81: LOAD 超时钩子 ("FUNCTION LOAD timeout")
- redis.conf:1567-1586: lua-time-limit 文档 (busy 模式可执行命令面)

## 推断标注

- "EVAL_RO/EVALSHA_RO 6.2.0" — 推断 (commands.def History NULL; RO 变体与 6.2 只读命令族同代)
- "math.random 替换 5.0" — 推断 (确定性保证演进, 无 release note 实证)
- "调试器 2.8" — ldb 与 CLIENT 命令同代推断 (标注)
- 仓库浅克隆无法 git 考古 — 版本线依赖 commands.def/注释/残留 API, 已逐条标注实证级别
