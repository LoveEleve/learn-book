# 闭环笔记 q6: 命令面与内存 — MULTI/DISCARD/WATCH/UNWATCH

## 假设
四命令 + 版本演化 + 内存统计; WATCH 在 MULTI 内禁止; DISCARD 无 MULTI 报错。

## 验证过程
- 命令版本 (commands.def): MULTI/EXEC **1.2.0** / DISCARD **2.0.0** / WATCH/UNWATCH **2.2.0**; 全部 CMD_NOSCRIPT|CMD_LOADING|CMD_STALE|CMD_FAST|CMD_ALLOW_BUSY; EXEC 额外 CMD_SKIP_SLOWLOG (不记慢日志)
- multiCommand (multi.c:91-99): 嵌套 MULTI 拒 (L92-94); 置 CLIENT_MULTI + OK
- discardCommand (L101-108): 无 MULTI → "DISCARD without MULTI"; discardTransaction (L77-82: 释放 mstate + 清三标志 + unwatchAllKeys)
- watchCommand (L452-467): MULTI 内拒 "WATCH inside MULTI is not allowed" (L455-458); DIRTY_CAS 早退回 OK (L460-463)
- unwatchCommand (L469-473): unwatchAllKeys + 清 DIRTY_CAS + OK
- **multiStateMemOverhead** (L475-482): argv_len_sums + watched_keys 数 × (listNode+watchedKey) + alloc_count × multiCmd — 注释 L477 "watched keys themselves aren't managed per-client" (键对象本身不算客户端开销)
- watching_clients 统计 (server.h:1995): watchForKey 首键++ / unwatchAllKeys--
- shared.execaborterr (server.c:1894): "-EXECABORT Transaction discarded because of: " 前缀

## 代码类型
Command (家族) + 资源管理

## 跨域关联
- R-20: 命令标志/CMD_ALLOW_BUSY (busy 期间可执行)
- R-1: 共享对象 (execaborterr)
- R-30 (未来): CMD_NOSCRIPT (脚本内禁事务命令)

## 结论
命令面简洁 (四命令 + 队列); 内存面 argv 零拷贝转移 + 统计三构成; 版本演化 1.2→2.2 (EXEC→DISCARD→WATCH)。
源码位置: multi.c:77-108,452-482; server.c:1894; commands.def
