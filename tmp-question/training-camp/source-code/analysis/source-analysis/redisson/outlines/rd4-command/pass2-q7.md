# 闭环笔记 q7: Lua 执行链路 — EVALSHA 缓存 + NOSCRIPT 自愈

## 假设
useScriptCache 开启时 EVAL → SCRIPT_LOAD 预加载 + EVALSHA 执行; NOSCRIPT 错误 (脚本丢失) 自动 reload 重发。这是 Lua 脚本的高效+容错执行。

## 验证过程
- evalAsync (CommandAsyncService.java:578-659):
  - L581 `mappedScript = map(script)` — 名称映射
  - L583 `isEvalCacheActive() && EVAL` → 缓存路径:
    - L590 `calcSHA(mappedScript)` → sha1
    - L592-595 cmd = EVALSHA_RO (readOnly+支持) or EVALSHA; trunc 截断命令
    - L597-601 args = [sha1, keys.size, keys..., params...]
    - L603-607 new RedisExecutor(...).execute()
  - **双错误自愈** (L609-649):
    - L611-614 `ERR unknown command` → EVAL_SHA_RO_SUPPORTED=false → 递归 evalAsync 降级
    - L615-638 **NOSCRIPT** → loadScript (SCRIPT_LOAD 预加载到 executor.getRedisClient()) → 成功后 new NodeSource 绑定到该 client → 重发 EVALSHA
    - L639-643 其他错误 → 直接 fail
- loadScript (L498-504): SCRIPT_LOAD 按 client 目标: entry 的 master 用 writeAsync, 其他用 readAsync — **脚本加载到正确节点**
- 非缓存路径 (L653-658): 直接 EVAL 原始脚本 (脚本每次传)
- 设计: 
  - **useScriptCache = 减少脚本传输** (EVAL→EVALSHA)
  - **NOSCRIPT 自愈 = 服务器重启/新节点后脚本丢失自动重载** — Redis 集群扩容/故障切换后保证

## 代码类型
Implementation (脚本缓存协议) — 高价值: 缓存 + 自愈容错

## 跨域关联
- RD-2 (锁 Lua) → RLock 的 tryLock 脚本走此链路
- RD-5 (RMap) → 复杂 eval 操作
- useScriptCache 默认 true (Config:95) — 全脚本默认缓存

## 结论
Lua 执行 = 缓存路径 (SCRIPT_LOAD + EVALSHA, sha1 寻址) + 双自愈 (EVALSHA_RO 不支持降级 / NOSCRIPT 自动 reload)。设计心法: 缓存省带宽, 自愈抗漂移 (脚本在节点间的一致性由 loadScript 到对节点保证)。
源码位置: CommandAsyncService.java:578-659,498-504