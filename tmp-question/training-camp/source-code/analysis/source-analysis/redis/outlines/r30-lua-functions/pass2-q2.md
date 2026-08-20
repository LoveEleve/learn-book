# 闭环笔记 q2: Lua 沙箱 — 库装载面 + 全局保护 + 确定性随机

## 假设
io/package 不加载; os 加载但受限; 全局表递归只读; math.random 替换为确定性 PRNG。

## 验证过程
- **库装载** (script_lua.c:1218-1234 luaLoadLibraries): base/table/string/math/debug/**os** + cjson/struct/cmsgpack/bit (自定义 C 库); **io 不加载**; package 被 `#if 0` (L1231-1233 "Stuff that we don't load currently, for sandboxing concerns"); **os 库 = Redis 补丁精简版仅 os.clock** (deps/lua/src/loslib.c:240-243 sandbox_syslib — "Only a subset is loaded currently"; 测试实证 os.execute/remove/rename 报错 scripting.tcl:675-679)
- **全局保护** (eval.c:231-239): luaSetErrorMetatable (eval.c:1268-1278 — 访问不存在全局变量报错 "Script attempted to access nonexistent global variable") + **luaSetTableProtectionRecursively** (全局表递归只读 — 运行期改全局报错)
- **白名单** (script_lua.c:96-107): 6 组 — libraries_allow_list (string/math/table/struct/os...) / redis_api_allow_list (redis + __redis__err__handler) / lua_builtins_allow_list (26 个: xpcall/tostring/setmetatable/pcall/type/_G/pairs/loadstring... 含 **loadstring/load**) / not_documented (newproxy) / **removed_after_initialization (debug — 错误处理器创建后置 nil)**; deny_list (dofile/loadfile/print — 打印警告)
- **白名单时机** (L92-95 注释): "the allow list is only checked on start time, after that the global table is locked" — 启动期约束未来全局, 运行期靠只读锁
- **确定性随机** (L116-117, 测试 L71-72 "random numbers are random now" + "PRNG can be seeded correctly"): redis_math_random/randomseed 替换 math.random — **主从/AOF 重放一致性** (脚本传播到从库, 随机必须确定; redis.math.random 保留?)
- **luaSetErrorMetatable 的安全审计** (L1264-1290): luaProtectedTableError 检查参数数量/类型 — "malicious code trying to call luaProtectedTableError with wrong arguments" 防滥用日志
- **luaNewIndexAllowList** (L1291-1335): __newindex 白名单 (全局写入受限 — 允许列表?)

## 代码类型
Implementation (沙箱面)

## 跨域关联
- R-9 (replication): 确定性随机 = 主从一致前提 (对照 R-3 SipHash 随机化 / R-12 HLL 固定 seed — 同一哲学)

## 结论
沙箱 = "装载面控制 + 运行期锁": io/package 不加载, os 加载但白名单+递归只读受限, 全局表启动后锁死; math.random 替换确定性 PRNG (传播一致性); 错误信息注入源/行号 (__redis__err__handler)。
源码位置: script_lua.c:96-107,116-117,1218-1234,1264-1335,1628-1637; eval.c:1268-1278,231-239
