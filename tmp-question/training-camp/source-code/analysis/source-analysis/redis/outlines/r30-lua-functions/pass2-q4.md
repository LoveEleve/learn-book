# 闭环笔记 q4: 超时与中断 — busy 模式 + SCRIPT KILL

## 假设
lua-time-limit 超时后进入 busy 模式 (重入事件循环), 未写脚本可 KILL; 写脚本只能 SHUTDOWN。

## 验证过程
- **配置** (config.c:3201): `busy-reply-threshold` (别名 lua-time-limit), 默认 5000ms, MODIFIABLE
- **钩子链**: luaCallFunction 设 LUA_MASKCOUNT 100000 指令计数钩子 (script_lua.c:1613-1615) → luaMaskCountHook (L1545-1560) → scriptInterrupt
- **scriptInterrupt** (script.c:119-168):
  - 已超时 → 直接 processEventsWhileBlocked + 返回 KILL/CONTINUE (L125-130)
  - 未超时 → elapsed < busy_reply_threshold → CONTINUE (L132-135)
  - **超时瞬间** (L136-152): 日志 ("Slow script detected ... SCRIPT KILL/FUNCTION KILL") → **enterScriptTimedoutMode** (L37-46: 置 SCRIPT_TIMEDOUT + 记录 run 上下文) → **protectClient** (L146-148, 注释: "the client may disconnect and could no longer be here when the EVAL command will return") → **processEventsWhileBlocked** (重入事件循环 — busy 模式)
- **busy 模式可执行什么**: 正常命令被拒 (-BUSY), 仅允许 SCRIPT KILL/FUNCTION KILL/SHUTDOWN NOSAVE 等少数 (redis.conf L1574-1580: "SCRIPT KILL, FUNCTION KILL, SHUTDOWN NOSAVE and possibly some other commands")
- **KILL 语义** (script.c:329-360 scriptKill): **只有未写脚本可 KILL** (SCRIPT_WRITE_DIRTY 检查 — "script already issued a write command, cannot be killed" → 只能 SHUTDOWN NOSAVE 保护一致性; 对照 R-16 事务不可部分回滚)
- **luaMaskCountHook 的 KILL 落地** (script_lua.c:1545-1560): scriptInterrupt 返回 SCRIPT_KILL → **钩子改 LUA_MASKLINE** ("so the user will not be able to catch the error with pcall and invoke pcall again which will prevent the script from ever been killed" L1551-1555) → luaPushError("Script killed by user with SCRIPT KILL...") → luaError
- **whileBlockedCron 交叉** (server.c:1560+): busy 脚本期间 cron 由 processEventsWhileBlocked 驱动 (对照 R-18 阻塞期间补齐)
- **FUNCTION KILL** (functions.c:593-608 functionKillCommand): scriptKill(c, 0) — 引擎无关
- **测试** (scripting.tcl): "Script can't run more than configured time limit" (L32) / SCRIPT KILL 场景

## 代码类型
Algorithmic (中断 + 受限重入)

## 跨域关联
- R-2 (events): processEventsWhileBlocked / whileBlockedCron
- R-16 (multi): 写后不可回滚哲学 (KILL 限制)
- R-8 (persistence): SHUTDOWN NOSAVE 兜底

## 结论
超时 = 指令计数钩子 → busy_reply_threshold → 受限重入事件循环 (processEventsWhileBlocked) → 未写脚本可 SCRIPT KILL (改 MASKLINE 防 pcall 免疫), 已写只能 SHUTDOWN NOSAVE; protectClient 防断连后 UAF。
源码位置: script.c:28-46,102-168,329-360; script_lua.c:1545-1560,1613-1615; config.c:3201; functions.c:593-608
