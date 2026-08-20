# R-16 事务 — Pass 1 探索笔记

> 域: R-16 事务 (multi.c) | 🟡 B 方案 | 2026-08-13
> 源码: src/multi.c (482) + server.c 接入点 | Redis 7.4.2

## 调用图

```
命令队列面:
multiState (server.h:1005-1016): commands/count/cmd_flags/cmd_inv_flags/argv_len_sums/alloc_count
multiCmd (server.h:998-1003): argv/argv_len/argc/cmd
initClientMultiState (multi.c:14-21) / freeClientMultiState (L24-36, 逐命令释放 argv)
queueMultiCommand (L39-75): DIRTY 提前返回 → 预分配 2 → 倍增 → 复制 argv (客户端 argv 置 NULL)

接入点 (server.c):
processCommand (L4193-4201): CLIENT_MULTI 且非 exec/discard/multi/watch/quit/reset → 入队+QUEUED
CMD_NO_MULTI 拒绝 (L3979-3981): psync/save/shutdown/sync 四命令禁入事务
rejectCommand 系列 (L3757-3785): flagTransaction (DIRTY_EXEC) + 被拒是 EXEC → execCommandAbort
signalModifiedKey (db.c:620-623): touchWatchedKey + trackingInvalidateKey (WATCH+缓存双失效)
signalFlushedDb (db.c:626-637) / SWAPDB (L1721-1722) / SELECT (L1767): touchAllWatchedKeysInDb

EXEC 执行面:
execCommand (multi.c:127-235): 过期检查 → DIRTY 分支 (nullarray vs EXECABORT) → DENY_BLOCKING
  → unwatchAllKeys → in_exec → 逐命令恢复 mstate + ACL 复查 (L184-207) + call (AOF 客户端 CMD_CALL_NONE L209-212)
  → mstate 回写 (命令可改 argv L218-221) → 恢复 → discardTransaction
execCommandAbort (L115-125): discard + EXECABORT 错误 + monitor 反馈

WATCH 面:
watchedKey (L253-259): listNode 嵌入 + key/db/client/expired:1 — 双链表单向嵌入式节点
watchForKey (L279-310): 客户端列表 + db->watched_keys dict 双向注册
unwatchAllKeys (L314-338): O(1) 摘除 (嵌入式 node) + 空列表删键
touchWatchedKey (L359-398): dict 查键 → redis_member2struct → DIRTY_CAS + 立即 unwatch (省内存)
touchAllWatchedKeysInDb (L407-450): FLUSHDB/SWAPDB 全量失效 (不可迭代中 unwatch — UAF 注释 L443-445)
isWatchedKeyExpired (L342-355): WATCH 时已过期键不算 (expired 标志)

命令面:
multiCommand (L91-99) / discardCommand (L101-108) / watchCommand (L452-467, MULTI 内拒 + DIRTY 早退)
unwatchCommand (L469-473) / multiStateMemOverhead (L475-482)
```

## 基本元素分解

1. 队列: multiState/multiCmd + 预分配 2 + 倍增 + DIRTY 冻结
2. 入队错误: rejectCommand → flagTransaction (DIRTY_EXEC) + CMD_NO_MULTI 4 命令
3. EXEC: 双失败路径 (nullarray vs EXECABORT) + ACL 复查 + DENY_BLOCKING + in_exec
4. WATCH: 嵌入式 watchedKey 双向注册 + touchWatchedKey DIRTY_CAS + expired 位
5. 全库失效: FLUSHDB/SWAPDB/SELECT → touchAllWatchedKeysInDb
6. 命令面: MULTI/DISCARD/WATCH/UNWATCH + 内存统计

## 标记问题 (20 问)

1. multiState 各字段语义 (cmd_flags/cmd_inv_flags)?
2. queueMultiCommand 预分配 2 的假设?
3. DIRTY_EXEC 后入队为什么直接返回?
4. rejectCommand 为什么对 EXEC 特判 (execCommandAbort)?
5. EXEC 的双失败路径 (nullarray vs EXECABORT) 区别?
6. CMD_NO_MULTI 哪 4 个命令?
7. EXEC 中 ACL 复查 (为什么入队时也查)?
8. CLIENT_ID_AOF 的 CMD_CALL_NONE 特例?
9. watchedKey 的嵌入式 listNode 设计 (O(1) 摘除)?
10. touchWatchedKey 为什么立即 unwatchAllKeys?
11. touchWatchedKey 的 expired 键特殊处理?
12. isWatchedKeyExpired 的语义 (WATCH 时已过期)?
13. touchAllWatchedKeysInDb 为什么不能迭代中 unwatch (UAF)?
14. signalModifiedKey 双失效 (WATCH+tracking)?
15. EXEC 中 DENY_BLOCKING 的意义?
16. mstate 回写 (命令改 argv) 的意义?
17. execCommandAbort 的 monitor 反馈?
18. watching_clients 统计?
19. multiStateMemOverhead 构成?
20. WATCH inside MULTI 为什么禁止?

## 时空溯源 (代码内痕迹)

- 1.2.0: MULTI/EXEC (commands.def 实证, 版权 2009-Present)
- 2.0.0: DISCARD
- 2.2.0: WATCH/UNWATCH (CAS 语义)
- 演进: watchedKey 从"客户端独立列表节点"改为**嵌入式 listNode + redis_member2struct** (multi.c:246-252 注释 — "avoid the need for listSearchKey and dictFind"); isWatchedKeyExpired (expired 位) — 7.x HFE 时代引入 (WATCH 已过期键语义)

## 大域拆分判断

482 行单文件 — **不拆** (🟡 B, 6 闭环足够)
