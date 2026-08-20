# 闭环笔记 q3: 命令注册与分派 — proxy 模式 + 动态键位置

## 假设
模块命令 = 双 dict 注册 + proxy 包装; proc 统一分派器; 键位置可静态或动态。

## 验证过程
- **注册** (RM_CreateCommand L1253-1276): onload 限制 → strflags → commandFlagsFromString (写/只读/管理/ACL 分类等) → 命令名验证 (isCommandNameValid) + **冲突检查** (lookupCommandByCString) → **双 dict 注册** (commands + orig_commands, L1272-1273) → **ACL ID 分配** (ACLGetCommandID, R-32 交叉)
- **proxy 包装** (moduleCreateCommandProxy L1286-1301): RedisModuleCommand{module/func/rediscmd} — rediscmd->proc = **RedisModuleCommandDispatcher** (L1301)
- **分派器** (RedisModuleCommandDispatcher L910-935): moduleCreateContext (REDISMODULE_CTX_COMMAND) + ctx.client = c + **cp->func(ctx, argv, argc)** — 模块函数签名 (ctx, argv, argc); 收尾: 模块 take ownership 的 argv (refcount>1) → trimStringObjectIfNeeded
- **键位置**: 静态 firstkey/lastkey/keystep (注册参数) → key_specs; **动态 "getkeys-api" 标志** → moduleGetCommandKeysViaAPI (L937): REDISMODULE_CTX_KEYS_POS_REQUEST 上下文调用模块命令 → RM_IsKeysPositionRequest 感知 → RM_KeyAtPos 上报 (R-20 key specs 交叉)
- **子命令**: RM_CreateSubcommand (L1377) — 容器命令
- **arity**: cmdfunc ? -1 : -2 (L1268) — 无函数为容器

## 代码类型
Implementation (命令扩展)

## 跨域关联
- R-20 (server): 命令表双 dict / key specs
- R-32 (ACL): 模块命令纳入位图 (R-32 三次 REVIEW 已接)
- R-28 (networking): argv 生命周期

## 结论
模块命令 = proxy 模式: redisCommand.proc 统一指向 Dispatcher, Dispatcher 查 module_cmd 调模块函数; 双 dict 注册 + ACL ID + 动态键位置 API — 模块命令与原生命令完全同构。
源码位置: module.c:910-935,1253-1301,1377; redismodule.h
