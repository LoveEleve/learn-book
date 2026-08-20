# 闭环笔记 q2: 记住面 — trackingRememberKeys 与只读命令

## 假设
只读命令执行后, 按命令键规格 (getKeys) 提取键 → 记入双层表; OPTIN/OPTOUT + CLIENT CACHING 做门控。

## 验证过程
- **调用点** (server.c:3710-3725, call() 尾): 条件 = `c->cmd->flags & CMD_READONLY` **且** proc 不是 evalRo/evalShaRo/fcallro — RO 脚本外层豁免 (内层逐命令再走 call(), 各自记住; 测试 L225-253 "Tracking only occurs for scripts when a command calls a read-only command")
- **双客户端身份分离** (L3716-3723): 跟踪标志取 `server.current_client` (外部触发客户端), 键取 `c` (实际执行命令的客户端 — 脚本/EXEC 内是内部客户端) — 注释 "original external client that triggered the command"
- **BCAST 排除** (L3721): current_client 是 BCAST 模式 → 不记表 (BCAST 零键级内存, redis.conf L864 佐证)
- **门控** (tracking.c:204-207): `(optin && !caching_given) || (optout && caching_given)` → return — OPTIN 需 CLIENT CACHING yes / OPTOUT 需无 CLIENT CACHING no
- **键提取** (L209-214): getKeysFromCommand (db.c:2434 → getKeysFromCommandWithSpecs db.c:2260, R-21 已讲 key specs); 零键 → return
- **PUBSUB 豁免** (L215-220): `CMD_PUBSUB` 命令跳过 — 注释 "Shard channels are treated as special keys ... These channels doesn't need to be tracked"
- **CACHING 一次性语义** (networking.c:2119-2123): 每条命令后清 `CLIENT_TRACKING_CACHING` — 条件 `!(c->flags & CLIENT_MULTI) && prevcmd != clientCommand` — 事务内保持到 EXEC (对照 ASKING 同款机制 L2116-2117)
- **CLIENT CACHING 命令面** (networking.c:3478-3507): 无 TRACKING → 报错; YES 仅 OPTIN 有效 / NO 仅 OPTOUT 有效; 其余 syntaxerr

## 代码类型
Implementation (读路径打点)

## 跨域关联
- R-21 (db): getKeysFromCommandWithSpecs (key specs 提取)
- R-20 (server): call() 命令链 / CMD_READONLY 标志 / current_client
- R-30 (Lua): RO 脚本豁免路径 (evalRoCommand/fcallroCommand)

## 结论
记住面 = call() 尾打点: 外层客户端身份 + 内层命令键规格; 四层门控 (READONLY/非RO脚本/非BCAST/optin-out×CACHING)。CACHING 为一次性标志 (下条命令即清, MULTI 内保活)。
源码位置: server.c:3710-3725; tracking.c:201-241; networking.c:2116-2123,3478-3507; db.c:2260,2434
