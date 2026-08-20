# 闭环笔记 q3: 脚本运行时 — scriptRunCtx + scriptCall 六重验证 + 传播

## 假设
7.0 统一运行时: scriptPrepareForRun 验证标志, scriptCall 走完整命令链, 脚本内命令各自传播 (effects)。

## 验证过程
- **scriptPrepareForRun** (script.c:170-291):
  - **shebang 标志验证** (非 EVAL_COMPAT_MODE): NO_CLUSTER×cluster / ALLOW_STALE×stale 副本 / NO_WRITES 未设 → 只读副本+磁盘错误+ro 命令+少从库 (checkGoodReplicasStatus) 四重写门禁 (L183-228) / ALLOW_OOM×maxmemory (L230-238, NO_WRITES 隐含 ALLOW_OOM)
  - **EVAL_COMPAT_MODE 兼容路径** (L239-245): 只查 stale (masterdownerr) — 旧语义 (2.6 时代)
  - run_ctx 装载 (L246-257): engine_client (Lua 专用 client) + original_client + slot; **selectDb 对齐调用者 DB** (L255-256) + resp=2 默认 + MULTI 上下文传递 (L259-264, CLIENT_MULTI 标志)
  - 标志派生 (L266-281): SCRIPT_READ_ONLY (ro 或 NO_WRITES) / SCRIPT_ALLOW_OOM / SCRIPT_ALLOW_CROSS_SLOT
  - `curr_run_ctx = run_ctx` (L286) — 全局当前运行上下文 (KILL 用)
- **scriptCall** (L575-640, redis.call 核心):
  - c->user = 调用者 user (ACL 上下文) + moduleCallCommandFilters (L579-580)
  - 六重验证 (L582-608): ① arity ② **CMD_NOSCRIPT** (L588-590, server.script_disable_deny_script 可旁路) ③ stale ④ **ACL 复查** (scriptVerifyACL L372) ⑤ 写允许 (scriptVerifyWriteCommandAllow L386 — 只读 ctx 拒绝写) ⑥ **OOM** (scriptVerifyOOM L438 — 无 ALLOW_OOM 时 CMD_DENYOOM)
  - **集群跨槽** (scriptVerifyClusterState L462 + ALLOW_CROSS_SLOT)
  - **CMD_WRITE → SCRIPT_WRITE_DIRTY** (L597-600) — 记录脚本写过数据 (KILL 决策用)
  - call_flags = CMD_CALL_PROPAGATE_AOF|REPL (L601-607) → call(c)
- **传播语义** (7.0 effects 复述): 脚本内每条命令经 scriptCall → call() 各自传播 (AOF+复制); **redis.replicate_commands 已成空操作** (eval.c:154-158 只返回 true — 3.2 时代的 effects 开关, 7.0 后 effects 为默认且不可关); 测试 L75 "MGET: mget shouldn't be propagated in Lua" — 只读命令不传播
- **递归检测** (script_lua.c:877-904): luaRedisGenericCommand 的 static inuse — debug 钩子恶意递归调用防护
- **luaCallFunction** (script_lua.c:1604-1660): registry 存 run_ctx (钩子可用) → **LUA_MASKCOUNT 100000 指令计数钩子** (超时检查, L1613-1615) → KEYS/ARGV 注入 (EVAL 模式为全局, 临时解只读锁 L1619-1632) → lua_pcall (EVAL: 0 参数 1 返回; FCALL: 2 参数) → 返回转换

## 代码类型
Implementation (运行时管线)

## 跨域关联
- R-16 (multi): CLIENT_MULTI 上下文传递 + 脚本与事务交互 (对照)
- R-20 (server): call() 命令链 / CMD_NOSCRIPT / ACL
- R-23 (evict): OOM 门禁 (pre_command_oom_state)
- R-15 (cluster): 跨槽验证

## 结论
运行时 = 前置标志验证 (shebang) + 六重命令验证 (redis.call) + effects 传播 (命令各自 call, replicate_commands 空操作); run_ctx 全局承载 KILL 上下文; 指令计数钩子驱动超时。
源码位置: script.c:170-291,575-640; eval.c:154-158,259-264; script_lua.c:877-904,1604-1660
