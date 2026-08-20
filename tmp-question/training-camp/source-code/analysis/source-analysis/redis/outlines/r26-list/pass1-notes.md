# R-26 t_list+blocked — Pass 1 探索笔记

> 域: R-26 t_list+blocked (t_list.c + blocked.c) | 🔴 A 方案 | 2026-08-13
> 源码: src/t_list.c (1364) + blocked.c (746) | Redis 7.4.2

## 调用图

```
写路径:
pushGenericCommand (t_list.c:464): lookupKeyWrite → 不存在 createListListpackObject + dbAdd (XX 时 0)
  → listTypeTryConversionAppend (L133) → listTypePush ×N (L144, quicklist API)
  → notify "lpush"/"rpush" + signalModifiedKey
  → signalKeyAsReady 触发点 (db.c:192 dbAddInternal 内, 阻塞键唤醒)

读/删路径:
popGenericCommand (L757): lookupKeyWriteOrReply → listTypePop (L173) / COUNT 范围版
  → listElementsRemoved (L736: 空 list 删键 + signalDeletedKeyAsReady)
mpopGenericCommand (L811): 多键逐个尝试 → 传播重写 [LR]POP COUNT
lmoveCommand (L908+): LMOVE/BLMOVE 复用

阻塞框架 (blocked.c):
BLPOP → blockForKeys (L359): 双向注册 (c->bstate.keys + db->blocking_keys)
  → CLIENT_BLOCKED + PENDING_COMMAND (L408) + blockClient (L409)
blockClient (L67): btype 设置 + blocked_clients_by_type[btype]++ + addClientToTimeoutTable
LPUSH 触发 → signalKeyAsReadyLogic (L447): 类型检查 (blocked_clients_by_type) 
  → ready_keys dict 防重 (L477-486) → server.ready_keys 列表 (L493)
beforeSleep → handleClientsBlockedOnKeys (L306): 列表消费 (防递归 L310-313 + 新列表交换 L329-330)
  → handleClientsBlockedOnKey (L553): 类型匹配 (L578-580) → unblockClientOnKey (L631)
    → releaseBlockedEntry (L507) → unblockClient (L164) → PENDING_COMMAND 重处理 (L648-668)
超时: unblockClientOnTimeout (L700) → replyToBlockedClientTimedOut (L211) + unblockClient
```

## 基本元素分解

1. **list 编码**: listpack (小) / quicklist (大, -2=8KB 节点); TryConversion 双向 (listpack→quicklist / quicklist 缩容→listpack?)
2. **push 语义**: LPUSH/RPUSH/LPUSHX/RPUSHX (XX 不存在返回 0)
3. **pop 语义**: 单元素/COUNT 范围 + 空键处理 (删键+就绪信号)
4. **LMOVE/BLMOVE**: 跨 list 移动 (原子 + 阻塞版)
5. **阻塞状态机**: CLIENT_BLOCKED/btype 10 种/timeout 表
6. **双向注册**: client→keys (bstate.keys) + db→clients (blocking_keys)
7. **就绪队列**: signalKeyAsReadyLogic (类型门槛 + dict 防重) → server.ready_keys
8. **消费**: handleClientsBlockedOnKeys (防递归/新列表/类型匹配/重处理)
9. **超时**: unblockClientOnTimeout (null 回复 + 清除)

## 标记问题 (10 个)

1. list 编码怎么选?listpack→quicklist 什么时候?
2. pushGenericCommand 怎么触发阻塞唤醒?
3. pop 的 COUNT 与空键语义?
4. LMOVE 与 BLMOVE 的复用?
5. blockClient 的统计与超时表?
6. blockForKeys 双向注册怎么维护?
7. signalKeyAsReadyLogic 的三个快速返回?
8. handleClientsBlockedOnKeys 防递归与公平性?
9. unblockClientOnKey 的重处理链?
10. 超时回复 (null vs 空数组)?

## 时空溯源 (代码内痕迹)

- 2009 (antirez): t_list.c 初版 — LPUSH/LPOP 家族 (版权 2009-Present); blocked.c 同期 (BLPOP)
- 2.8: 阻塞框架重构 (blockForKeys 双向注册); ready_keys 队列
- 3.2: quicklist 引入 (R-5)
- 6.0: LMOVE/BLMOVE (替换 RPOPLPUSH 语义); blocked 类型扩展 (WAIT/WAITAOF/MODULE/POSTPONE/SHUTDOWN/LAZYFREE)
- 7.x: list-max-listpack-size (旧 ziplist 别名); unblock_on_nokey (XREADGROUP)

## 大域拆分规划 (01 §大域)

8 闭环 → **2 篇**:
- 篇 1 (list 命令面): q1 (编码) + q2 (push) + q3 (pop) + q4 (范围/lmove)
- 篇 2 (阻塞框架): q5 (状态机) + q6 (双向注册) + q7 (就绪队列) + q8 (消费与超时)
