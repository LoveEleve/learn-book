# 闭环笔记 q6: 类型转换与命令面 — RESP↔Lua 双向 + SCRIPT/FUNCTION 家族

## 假设
RESP2/RESP3 全类型 ↔ Lua 转换; 命令面 = EVAL 族 + SCRIPT 族 + FCALL 族 + FUNCTION 族。

## 验证过程
- **Lua → RESP** (luaReplyToRedisReply, script_lua.c:133): Lua 类型 → 协议 (nil→null array?/number→integer/bool/string/table→array/带 metatable 错误表→error; 测试 L76-105 逐类型)
- **RESP → Lua** (redisProtocolToLuaType_*, L218-300+): 14 类型转换器 — Int/BulkString/NullBulkString/NullArray/Status/Error/Array/Map/Set/Null/Bool/Double/BigNumber/VerbatimString/Attribute (RESP3 全谱) + **批量解析器** (struct ReplyParser, 零拷贝? 需写作时确认)
- **转换一致性** (测试 L141-199): Redis integer→Lua number→Redis integer 往返 / status/error/nil 语义 / SELECT 隔离 ("Is the Lua client using the currently selected DB?" L26 — 脚本内 SELECT 不影响调用者, script_client 独立 DB)
- **命令家族** (commands.def 实证):
  - EVAL/EVALSHA **2.6.0** (L11095-11096); EVAL_RO/EVALSHA_RO 6.2.0 (推断)
  - SCRIPT **2.6.0** (L11102) — DEBUG/EXISTS/FLUSH/KILL/LOAD
  - FCALL/FCALLRO/FUNCTION **7.0.0** (L11099-11101)
  - 全组 CMD_NOSCRIPT|SKIP_MONITOR|MAY_REPLICATE|NO_MANDATORY_KEYS|STALE
- **调试面** (eval.c:754-957, ldb*): CLIENT LUA DEBUG — 断点/单步/日志 (2.8+ 历史遗留, 独立会话进程 fork)
- **内存记账** (eval.c:734-752): evalMemory = lua_scripts_mem + lua 运行时 (luaMemory) + 开销; INFO memory 面
- **stat_evictedscripts**: 缓存淘汰统计 (q1)
- **测试面**: scripting.tcl **101 测试** + functions.tcl **133 测试** (功能面最全测试域之一) — 含 EVAL_RO 写拒绝 / 全局保护读写 / PRNG 种子 / argv 重写传播 (L74-77: "scripts rewriting client->argv") / 大 JSON (2GB) / bitop 数值

## 代码类型
Interface (协议转换 + 命令面)

## 跨域关联
- R-28 (networking): RESP2/RESP3 协议面 (类型转换复用解析器)
- R-20 (server): 命令表注册/MAY_REPLICATE
- R-8 (persistence): AOF 重写脚本传播 (effects 命令流)

## 结论
转换面 = 14 类型双向转换器 (RESP3 全谱) + 错误表语义; 命令面四族 (EVAL 2.6 / SCRIPT 2.6 / FCALL+FUNCTION 7.0); 调试器独立会话; 内存记账三构成。
源码位置: script_lua.c:118-133,212-300; eval.c:734-752,754-957; commands.def:11095-11102
