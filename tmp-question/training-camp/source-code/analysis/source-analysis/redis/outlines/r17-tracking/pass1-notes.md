# R-17 客户端缓存 — Pass 1 探索笔记

> 域: R-17 客户端缓存 (tracking.c) | 🟡 B 方案 | 2026-08-14
> 源码: src/tracking.c (648) + networking.c (命令面) + server.c/db.c (接入点) | Redis 7.4.2

## 调用图

```
命令面 (networking.c clientCommand L3032+):
CLIENT TRACKING (L3354-3477): 解析 REDIRECT/BCAST/PREFIX/OPTIN/OPTOUT/NOLOOP → 兼容性检查 (L3413-3466)
  → enableTracking (tracking.c:164) / disableTracking (tracking.c:46)
CLIENT CACHING (L3478-3507): 仅 OPTIN/OPTOUT 模式有效; 置 CLIENT_TRACKING_CACHING
CLIENT GETREDIR (L3508-3514): 有 tracking → 重定向 id; 否则 -1
CLIENT TRACKINGINFO (L3515-3575): flags/redirect/prefixes 三字段
CACHING 一次性: processCommand 尾清理 (networking.c:2122-2123, 非 MULTI 且 prevcmd≠clientCommand)

记住面 (call() 后, server.c:3710-3725):
CMD_READONLY 且非 evalRo/evalShaRo/fcallro → trackingRememberKeys(server.current_client, c)
  → 双层 rax: TrackingTable[key → ids rax] (tracking.c:201-241)

失效面 (db.c):
signalModifiedKey (db.c:621-624) → trackingInvalidateKey(c,key,1)  ← 所有键修改 (R-21 已讲)
signalFlushedDb (db.c:640) → trackingInvalidateKeysOnFlush(async)
flushdbCommand ASYNC (db.c:1799) / swapdb (db.c:1799) → trackingInvalidateKeysOnFlush(1)
trackingInvalidateKey (tracking.c:353-410): BCAST 调度 + 逐 id 查找 + NOLOOP 过滤
  → 当前客户端执行中 → tracking_pending_keys 延迟 (L396-398) / 否则 sendTrackingMessage (L400)

延迟发送:
afterCommand (server.c:3796-3809): postExecutionUnitOperations → trackingHandlePendingKeyInvalidations
  (execution_nesting==0 门控 — 事务/脚本内不穿插)
performEvictions 后 (server.c:4039-4043) 强制 flush (淘汰先于响应)
beforeSleep (server.c:1716-1724): 断言 pending 空 + trackingBroadcastInvalidationMessages (BCAST 批量)

限额:
processCommand (server.c:4064) + serverCron (server.c:1504) → trackingLimitUsedSlots (tracking.c:496-533)
  effort=100×(timeout_counter+1) 递增; raxRandomWalk 随机驱逐

生命周期:
createClient (networking.c:112-211): id=atomicGetIncr(next_client_id)
freeClient (networking.c:1515-1516) → disableTracking (惰性 — 只减计数, ID 残留在表里)
clearClientConnectionState (networking.c:1536, RESET)

INFO (server.c): tracking_clients (L5648) / tracking_total_keys+items+prefixes (L5905-5907)
配置: tracking-table-max-keys 默认 1000000 (config.c:3225)
```

## 基本元素分解

1. **双层 rax 表**: TrackingTable (key → client-ID rax, 惰性创建 tracking.c:174-178) — 键级精确跟踪
2. **PrefixTable + bcastState**: BCAST 前缀表 (keys/clients 双 rax, tracking.c:31-38) — 广播模式零键级内存
3. **7 个跟踪标志位**: CLIENT_TRACKING(31)~NOLOOP(37) (server.h:361-370) + CLIENT_PUSHING(46)
4. **记住面**: trackingRememberKeys — 只读命令后按 getKeysFromCommand 记键 (R-20 命令链)
5. **失效面**: trackingInvalidateKey + tracking_pending_keys 延迟队列 (响应后发送, 不穿插)
6. **FLUSH 语义**: trackingInvalidateKeysOnFlush — 发 RESP NULL (全失效) + 表重建 (async 走 bio)
7. **BCAST 面**: trackingRememberKeyToBroadcast (事件循环聚合) → beforeSleep 批量 per-prefix 发送
8. **限额面**: trackingLimitUsedSlots — 独立于 maxmemory 的 tracking-table-max-keys 随机驱逐
9. **重定向面**: client_tracking_redirection → lookupClientByID; broken-redir push (tracking-redir-broken)
10. **协议面**: RESP3 push [2,"invalidate",keys] / RESP2 仅 pubsub 重定向通道 __redis__:invalidate

## 标记问题 (20 问)

1. 双层 rax 为什么选 rax 不选 dict? (key→ids 嵌套)
2. 客户端 ID 在表里怎么存? (8B uint64 作 rax key)
3. CLIENT TRACKING 选项兼容性矩阵? (BCAST×OPTIN/OPTOUT、OPTIN×OPTOUT、BCAST 切换)
4. OPTIN/OPTOUT 与 CLIENT CACHING 的一次性语义? (标志何时清?)
5. 只读命令怎么判定? (CMD_READONLY + RO 脚本豁免)
6. 为什么脚本的 RO 变体 (EVAL_RO) 不走外层 tracking?
7. NOLOOP 怎么判定"自己改的键"? (target == server.current_client)
8. pending 延迟发送的动机? (响应不穿插 — 协议层约束)
9. execution_nesting 门控的消费场景? (事务 EXEC 批量响应)
10. FLUSH 为什么发 NULL 而不是逐键? (防消息洪泛)
11. FLUSHALL ASYNC 怎么异步回收表? (bio 阈值)
12. tracking-table-max-keys 驱逐的 effort 递增逻辑? (100→200→300)
13. 驱逐为什么在 processCommand 和 serverCron 两处?
14. BCAST 前缀怎么匹配? (memcmp 前缀扫描 PrefixTable)
15. 前缀冲突检查? (checkPrefixCollisionsOrReply O(n²))
16. BCAST 每前缀一条消息的聚合? (beforeSleep per-prefix 批量)
17. RESP2 客户端怎么收到失效? (仅重定向到 pubsub)
18. tracking-redir-broken 什么时候发? (重定向目标消失)
19. CLIENT_PUSHING 的作用? (穿透 CLIENT REPLY OFF/SKIP)
20. 断连时表里残留的 ID 怎么清? (惰性 — 查找时 target==NULL 跳过)

## 时空溯源 (代码内痕迹)

- tracking.c 版权 2019-Present — 客户端缓存 6.0 时代引入 (2020-05)
- commands.def 实证: CLIENT TRACKING **6.0.0** / GETREDIR 6.0.0 / CACHING 6.0.0 / TRACKINGINFO **6.2.0**
- 演进痕迹 (代码注释): NOLOOP 模式; tracking_pending_keys 延迟发送 (响应不穿插 — 测试 "Tracking invalidation message is not interleaved with transaction response"); trackingLimitUsedSlots effort 递增 (evict 测试); 回归 #11715 (MULTI + 限额驱逐崩溃 — 测试 L746-782); CLIENT_TRACKING_BROKEN_REDIR (测试 L255-277)
- 7.0+ kvstore 时代与 tracking 无耦合 (表独立于 db 键空间 — 注释 "Caching keys are not specific for each DB")

## 大域拆分判断

648 行单文件 — **不拆** (🟡 B, 6 闭环足够)

## 域级怀疑审计 (HANDOFF §四 R-17 详案 断言复查)

| HANDOFF 断言 | 验证 | 结论 |
|:--|:--|:--|
| "源码: tracking.c + **blocked.c 部分** + networking.c 部分" | grep blocked.c tracking → 仅 latency_tracking_enabled (L93, 延迟监控非本域) | **修正: blocked.c 与本域无代码耦合** (BLMOVE 场景走 pending 机制, 无直接接入) |
| "RESP2: **重连失效**" | sendTrackingMessage (L282-300): RESP2 无重定向 → 静默丢弃; **不存在"重连失效"服务器机制** | **修正: RESP2 必须 REDIRECT 到 pubsub 客户端, 否则失效消息被静默丢弃** (连接级断连即 disableTracking, freeClient L1515) |
| "内存上限 **maxmemory 淘汰**" | trackingLimitUsedSlots: 独立 `tracking-table-max-keys` 默认 1M (config.c:3225), 与 maxmemory 无耦合; BCAST 零内存 | **修正: 独立限额 + 随机驱逐, 非 maxmemory 参与**; redis.conf L864 "BCAST 模式无服务器内存" |
| "重连恢复 (**id 与失效重放**)" | 服务器无重放机制; 表只存 client id (断连后残留惰性跳过 L378) | **修正: 不存在失效重放; 重连后客户端需自行全量失效 (客户端职责)** |
| "多客户端共享" | 表值=ID 集合 (raxTryInsert 去重 L237) + 测试 "Different clients can redirect to the same connection" | **接受** ✅ |
| "集群节点内失效" | cluster.c/cluster_legacy.c 零 tracking 代码; 表全局独立于槽 | **接受** ✅ (失效仅本节点; MOVED 不触发缓存失效 — 客户端职责, 标注) |

> 审计结论: HANDOFF 详案 4/6 断言不精确 (blocked.c 耦合/RESP2 语义/限额机制/重放机制), 全部以源码修正。这正是 09 方法论要求的"先怀疑"。
