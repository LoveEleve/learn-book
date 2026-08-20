# 闭环笔记 q3: 失效面 — trackingInvalidateKey 与延迟发送

## 假设
键修改 → trackingInvalidateKey: BCAST 调度 + 逐客户端发消息; 当前执行中的客户端 → pending 延迟到响应后。

## 验证过程
- **入口** (db.c:621-624): signalModifiedKey = touchWatchedKey + trackingInvalidateKey(c,key,**1**) — bcast=1 (R-16/R-21 已讲双失效); c 可为 NULL (过期删除等非客户端上下文, tracking.c:342-344 注释)
- **BCAST 调度先行** (tracking.c:359-360): `bcast && raxSize(PrefixTable)>0` → trackingRememberKeyToBroadcast — 聚合到 bcastState.keys, 事件循环周期后统一发 (q4)
- **主表查键** (L362-363): raxFind 未命中 → return (没客户端缓存该键)
- **逐 ID 迭代** (L366-403): 三重过滤 — ① `target==NULL` (客户端已断连 — **惰性清理: disableTracking 只减计数不删表项**, L42-45 注释) ② `!CLIENT_TRACKING` (已关跟踪) ③ `CLIENT_TRACKING_BCAST` (切到 BCAST 后旧键条目不发 — L373-383 注释, 测试 "After switching from normal tracking to BCAST mode")
- **NOLOOP** (L385-391): `target->flags & CLIENT_TRACKING_NOLOOP && target == server.current_client` → 跳过 (自己改的键不发自己)
- **pending 延迟** (L393-401): `target == server.current_client && CLIENT_EXECUTING_COMMAND` → keyobj incrRefCount 入 `server.tracking_pending_keys` — 注释 "invalidation messages may be interleaved with command response and should after command response"
- **直接发送** (L400): sendTrackingMessage (q6)
- **表项回收** (L405-409): TrackingTableTotalItems -= raxSize(ids) + raxFree(ids) + raxRemove — **失效即删表项** (下次再读重建)
- **pending 冲刷** (L412-438): trackingHandlePendingKeyInvalidations — ① 空列表早退 ② **execution_nesting 门控** (L417): 嵌套执行 (EXEC/脚本) 内不发 → 不穿插事务响应 (测试 L449-464 "not interleaved with transaction response") ③ 逐项: current_client 存活才发 ④ NULL 项 = FLUSH 全失效信号 (L430-433, shared.null RESP 编码作 proto) ⑤ decrRefCount
- **消费点**: afterCommand (server.c:3803, 每条命令尾 — 注释 "reply to client before invalidating cache (makes more sense)" L3798-3799) + performEvictions 后 (server.c:4039-4043, 淘汰先于响应 — 测试 L466-495 "eviction keys should be before response") + beforeSleep 断言为空 (server.c:1719)
- **过期场景**: 惰性过期 (keyIsExpired→expireIfNeeded→deleteExpiredKeyAndPropagate→signalModifiedKey) + 主动过期 (activeExpireCycle) 都走 signalModifiedKey — 测试 L126-150 (active/lazy 两种过期通知)

## 代码类型
Implementation (失效传播 + 延迟队列)

## 跨域关联
- R-21 (db): signalModifiedKey / expireIfNeeded 惰性过期
- R-16 (multi): execution_nesting (EXEC 嵌套)
- R-22 (expire): 过期路径
- R-23 (evict): performEvictions 后强制冲刷
- R-28 (networking): afterCommand / CLIENT_EXECUTING_COMMAND

## 结论
失效 = 查表 → 三重过滤 (断连/关闭/BCAST 切换) + NOLOOP → 直发或 pending 延迟; 失效即删表项 (下次重建); pending 冲刷受 execution_nesting 门控, 保证消息在响应之后。
源码位置: db.c:621-624; tracking.c:353-438; server.c:3796-3809,4039-4043,1719
