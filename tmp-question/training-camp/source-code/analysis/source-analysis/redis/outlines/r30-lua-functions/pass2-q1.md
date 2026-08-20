# 闭环笔记 q1: EVAL 入口与脚本缓存 — SHA1 注册名 + dict + LRU

## 假设
脚本按 SHA1 缓存; 注册名 f_+40字符; dict 存 sha→body (EVALSHA 重写用); LRU 上限淘汰。

## 验证过程
- **sha1hex** (eval.c:98-117): SHA1_CTX (deps/sha1.c) → 20 字节 → 十六进制 40 字符
- **注册名** (evalGenericCommand L562-566): `funcname[0]='f'; funcname[1]='_'; memcpy(funcname+2, sha, 40)` — 43 字符含 NUL; registry 键
- **EVALSHA 快速失败** (L660-664): sha 长度 != 40 → 直接 noscripterr ("implementation without string length sanity check" 注释)
- **luaCreateFunction** (L429-492): sha1hex → dict 查重 (已存在 → 直接返回 sha) → **shebang 解析** (evalExtractShebangFlags L319: #!lua + flags=; **5 个可写标志** script.c:16-22; 无 shebang → EVAL_COMPAT_MODE) → luaL_loadbuffer 编译 (跳过 shebang 行但保留换行保行号 L456-457) → registry 注册 → dict 登记 (luaScript{body,flags,node}) + **lua_scripts_mem 记账** (L478) + incrRefCount(body)
- **双缓存目的** (L468-471 注释): "save a SHA1 -> Original script map ... so that we can replicate / write in the AOF all the EVALSHA commands as EVAL using the original script" — **EVALSHA 传播重写依赖 body 缓存**
- **LRU 淘汰** (L527-545): `LRU_LIST_LENGTH = 500` (L527); **仅 EVAL 参与淘汰, SCRIPT LOAD 不参与** (evalsha 参数, L530-531: "Script eviction only applies to EVAL, not SCRIPT LOAD"); 超限淘汰最老 + stat_evictedscripts++; 执行后 LRU 重排 (evalGenericCommand L610-615: unlink+link tail)
- **SCRIPT FLUSH** (scriptCommand L660+): FLUSH[ASYNC|SYNC] → scriptingReset (eval.c:282: scriptingRelease+scriptingInit) — lazyfree-lazy-user-flush 默认决定
- **SCRIPT EXISTS/LOAD**: dict 查找 / 只 LOAD 不执行

## 代码类型
Implementation (缓存 + 淘汰)

## 跨域关联
- R-9 (replication): EVALSHA→EVAL 传播重写 (从库/AOF 必须拿到 body)
- R-22 (expire): stat_evictedscripts 统计面

## 结论
缓存 = registry (f_+sha → 编译函数) + dict (sha → body, 传播重写用) + LRU 500 (仅 EVAL); shebang 解析在缓存创建时固化 flags; EVALSHA 依赖缓存 (无缓存 → NOSCRIPT 错误)。
源码位置: eval.c:98-117,319-360,429-492,527-545,545-621,660-700
